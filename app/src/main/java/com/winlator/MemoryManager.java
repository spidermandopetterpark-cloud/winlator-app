package com.winlator;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Debug;
import android.os.Handler;
import android.os.Looper;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Locale;

public class MemoryManager {

    public enum Profile {
        LOW_MEMORY,
        BALANCED,
        PERFORMANCE,
        HIGH_MEMORY
    }

    public interface MemoryListener {
        void onMemoryUpdate(MemoryInfo info);
    }

    public static class MemoryInfo {

        public long totalRam;
        public long availableRam;
        public long usedRam;

        public long processRam;

        public long swapTotal;
        public long swapFree;
        public long swapUsed;

        public long virtualRam;

        public Profile profile;

        public MemoryInfo() {
            profile = Profile.PERFORMANCE;
        }

        public int ramPercent() {
            if (totalRam <= 0) return 0;

            return (int) Math.min(
                    100,
                    (usedRam * 100L) / totalRam
            );
        }

        public int swapPercent() {
            if (swapTotal <= 0) return 0;

            return (int) Math.min(
                    100,
                    (swapUsed * 100L) / swapTotal
            );
        }

        public String getRamText() {
            return formatGB(usedRam)
                    + " / "
                    + formatGB(totalRam);
        }

        public String getSwapText() {
            return formatGB(swapUsed)
                    + " / "
                    + formatGB(swapTotal);
        }

        public String getVirtualRamText() {
            return formatGB(virtualRam);
        }
    }

    private final Context context;

    private final Handler handler =
            new Handler(Looper.getMainLooper());

    private MemoryListener listener;

    private boolean running = false;

    /*
     * Valor configurável pelo usuário.
     *
     * Isso representa o orçamento de memória virtual
     * utilizado pelo Winlator/container.
     */
    private long virtualRam = 4L * 1024L * 1024L * 1024L;

    /*
     * Perfil padrão.
     */
    private Profile profile = Profile.PERFORMANCE;

    /*
     * Capacidade esperada/configurada de SWAP.
     *
     * O valor real do sistema sempre terá prioridade
     * quando /proc/meminfo informar um valor válido.
     */
    private long configuredSwap =
            8L * 1024L * 1024L * 1024L;

    private final Runnable monitorRunnable =
            new Runnable() {

                @Override
                public void run() {

                    if (!running) {
                        return;
                    }

                    MemoryInfo info =
                            readMemoryInfo();

                    if (listener != null) {
                        listener.onMemoryUpdate(info);
                    }

                    handler.postDelayed(
                            this,
                            1000
                    );
                }
            };

    public MemoryManager(Context context) {

        this.context =
                context.getApplicationContext();
    }

    /*
     * ============================================================
     * INICIALIZAÇÃO
     * ============================================================
     */

    public boolean initialize() {

        try {

            readMemoryInfo();

            return true;

        } catch (Throwable e) {

            return false;
        }
    }

    /*
     * ============================================================
     * MONITOR
     * ============================================================
     */

    public void startMonitoring(
            MemoryListener listener) {

        this.listener = listener;

        if (running) {
            return;
        }

        running = true;

        handler.removeCallbacks(
                monitorRunnable
        );

        handler.post(
                monitorRunnable
        );
    }

    public void stopMonitoring() {

        running = false;

        handler.removeCallbacks(
                monitorRunnable
        );

        listener = null;
    }

    public boolean isMonitoring() {
        return running;
    }

    /*
     * ============================================================
     * MEMORY INFO
     * ============================================================
     */

    public MemoryInfo readMemoryInfo() {

        MemoryInfo info =
                new MemoryInfo();

        ActivityManager activityManager =
                (ActivityManager)
                        context.getSystemService(
                                Context.ACTIVITY_SERVICE
                        );

        if (activityManager != null) {

            ActivityManager.MemoryInfo androidMemory =
                    new ActivityManager.MemoryInfo();

            activityManager.getMemoryInfo(
                    androidMemory
            );

            info.totalRam =
                    androidMemory.totalMem;

            info.availableRam =
                    androidMemory.availMem;

            info.usedRam =
                    Math.max(
                            0,
                            info.totalRam
                                    - info.availableRam
                    );
        }

        /*
         * ========================================================
         * PROCESS RSS
         * ========================================================
         */

        Debug.MemoryInfo processMemory =
                new Debug.MemoryInfo();

        Debug.getMemoryInfo(
                processMemory
        );

        info.processRam =
                processMemory.getTotalPss()
                        * 1024L;

        /*
         * ========================================================
         * SWAP
         * ========================================================
         */

        long[] swap =
                readSwapInfo();

        info.swapTotal = swap[0];
        info.swapFree = swap[1];

        info.swapUsed =
                Math.max(
                        0,
                        info.swapTotal
                                - info.swapFree
                );

        /*
         * ========================================================
         * SWAP FALLBACK
         * ========================================================
         *
         * Se o kernel não disponibilizar SwapTotal,
         * utilizamos o valor configurado como capacidade
         * de referência da interface.
         */

        if (info.swapTotal <= 0) {

            info.swapTotal =
                    configuredSwap;

            info.swapFree =
                    configuredSwap;

            info.swapUsed = 0;
        }

        /*
         * ========================================================
         * VIRTUAL RAM
         * ========================================================
         */

        info.virtualRam =
                virtualRam;

        info.profile =
                profile;

        return info;
    }

    /*
     * ============================================================
     * SWAP
     * ============================================================
     */

    private long[] readSwapInfo() {

        long swapTotal = 0;
        long swapFree = 0;

        BufferedReader reader = null;

        try {

            reader =
                    new BufferedReader(
                            new FileReader(
                                    "/proc/meminfo"
                            )
                    );

            String line;

            while ((line = reader.readLine())
                    != null) {

                if (line.startsWith("SwapTotal:")) {

                    swapTotal =
                            parseKB(line);
                }

                else if (line.startsWith("SwapFree:")) {

                    swapFree =
                            parseKB(line);
                }
            }

        } catch (IOException ignored) {

        } finally {

            if (reader != null) {

                try {
                    reader.close();
                } catch (IOException ignored) {
                }
            }
        }

        return new long[]{
                swapTotal,
                swapFree
        };
    }

    private long parseKB(String line) {

        try {

            String[] parts =
                    line.trim()
                            .split("\\s+");

            if (parts.length >= 2) {

                return Long.parseLong(
                        parts[1]
                ) * 1024L;
            }

        } catch (Exception ignored) {
        }

        return 0;
    }

    /*
     * ============================================================
     * VIRTUAL RAM
     * ============================================================
     */

    public void setVirtualRamMB(long megabytes) {

        if (megabytes < 512) {
            megabytes = 512;
        }

        virtualRam =
                megabytes
                        * 1024L
                        * 1024L;
    }

    public long getVirtualRam() {
        return virtualRam;
    }

    public long getVirtualRamMB() {

        return virtualRam
                / (1024L * 1024L);
    }

    /*
     * ============================================================
     * SWAP CONFIG
     * ============================================================
     */

    public void setConfiguredSwapGB(
            long gigabytes) {

        if (gigabytes < 0) {
            gigabytes = 0;
        }

        configuredSwap =
                gigabytes
                        * 1024L
                        * 1024L
                        * 1024L;
    }

    public long getConfiguredSwapGB() {

        return configuredSwap
                / (1024L * 1024L * 1024L);
    }

    /*
     * ============================================================
     * PROFILE
     * ============================================================
     */

    public void setProfile(
            Profile profile) {

        if (profile == null) {
            profile =
                    Profile.BALANCED;
        }

        this.profile =
                profile;

        applyProfile();
    }

    public Profile getProfile() {
        return profile;
    }

    private void applyProfile() {

        switch (profile) {

            case LOW_MEMORY:

                /*
                 * Perfil para aparelhos com pouca RAM.
                 */

                break;

            case BALANCED:

                /*
                 * Equilíbrio entre desempenho e memória.
                 */

                break;

            case PERFORMANCE:

                /*
                 * Prioriza desempenho.
                 */

                break;

            case HIGH_MEMORY:

                /*
                 * Permite orçamento de memória maior
                 * para containers pesados.
                 */

                break;
        }
    }

    /*
     * ============================================================
     * MÉTODOS RÁPIDOS
     * ============================================================
     */

    public long getRamSize() {

        return readMemoryInfo()
                .totalRam;
    }

    public long getAvailableMemory() {

        return readMemoryInfo()
                .availableRam;
    }

    public long getUsedMemory() {

        return readMemoryInfo()
                .usedRam;
    }

    public long getProcessMemory() {

        return readMemoryInfo()
                .processRam;
    }

    public long getSwapSize() {

        return readMemoryInfo()
                .swapTotal;
    }

    public long getSwapFree() {

        return readMemoryInfo()
                .swapFree;
    }

    public long getSwapUsed() {

        return readMemoryInfo()
                .swapUsed;
    }

    /*
     * ============================================================
     * FORMAT
     * ============================================================
     */

    public static String formatGB(
            long bytes) {

        double gb =
                bytes
                        / (1024.0
                        * 1024.0
                        * 1024.0);

        return String.format(
                Locale.US,
                "%.1f GB",
                gb
        );
    }

    public static String formatMB(
            long bytes) {

        double mb =
                bytes
                        / (1024.0
                        * 1024.0);

        return String.format(
                Locale.US,
                "%.0f MB",
                mb
        );
    }
}
