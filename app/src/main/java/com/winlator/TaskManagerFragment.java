package com.winlator;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.Locale;

public class TaskManagerFragment extends Fragment {

    /*
     * ============================================================
     * MEMORY MANAGER
     * ============================================================
     */

    private MemoryManager memoryManager;

    /*
     * ============================================================
     * UI
     * ============================================================
     */

    private TextView textRam;
    private TextView textWinlator;
    private TextView textWine;
    private TextView textBox64;
    private TextView textOther;
    private TextView textSwap;
    private TextView textVirtualMemory;
    private TextView textProfile;

    private ProgressBar progressRam;
    private ProgressBar progressSwap;

    /*
     * ============================================================
     * LIFECYCLE
     * ============================================================
     */

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        return inflater.inflate(
                R.layout.fragment_task_manager,
                container,
                false
        );
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState) {

        super.onViewCreated(
                view,
                savedInstanceState
        );

        /*
         * ========================================================
         * FIND VIEWS
         * ========================================================
         */

        textRam =
                view.findViewById(
                        R.id.text_memory_ram
                );

        textWinlator =
                view.findViewById(
                        R.id.text_memory_winlator
                );

        textWine =
                view.findViewById(
                        R.id.text_memory_wine
                );

        textBox64 =
                view.findViewById(
                        R.id.text_memory_box64
                );

        textOther =
                view.findViewById(
                        R.id.text_memory_other
                );

        textSwap =
                view.findViewById(
                        R.id.text_memory_swap
                );

        textVirtualMemory =
                view.findViewById(
                        R.id.text_memory_virtual
                );

        textProfile =
                view.findViewById(
                        R.id.text_memory_profile
                );

        progressRam =
                view.findViewById(
                        R.id.progress_memory_ram
                );

        progressSwap =
                view.findViewById(
                        R.id.progress_memory_swap
                );

        /*
         * ========================================================
         * PROGRESS BARS
         * ========================================================
         */

        if (progressRam != null) {
            progressRam.setMax(100);
        }

        if (progressSwap != null) {
            progressSwap.setMax(100);
        }

        /*
         * ========================================================
         * MEMORY MANAGER
         * ========================================================
         */

        initializeMemoryManager();
    }

    /*
     * ============================================================
     * INITIALIZE
     * ============================================================
     */

    private void initializeMemoryManager() {

        if (getContext() == null) {
            return;
        }

        memoryManager =
                new MemoryManager(
                        requireContext()
                );

        /*
         * Configuração padrão:
         *
         * SWAP: 8 GB
         * Memória virtual: 4 GB
         * Perfil: PERFORMANCE
         */

        memoryManager.setConfiguredSwapGB(8);

        memoryManager.setVirtualRamMB(4096);

        memoryManager.setProfile(
                MemoryManager.Profile.PERFORMANCE
        );

        /*
         * Inicializa.
         */

        memoryManager.initialize();

        /*
         * Começa monitoramento.
         */

        memoryManager.startMonitoring(
                info -> {

                    if (!isAdded()) {
                        return;
                    }

                    updateMemoryUI(info);
                }
        );
    }

    /*
     * ============================================================
     * UPDATE UI
     * ============================================================
     */

    private void updateMemoryUI(
            MemoryManager.MemoryInfo info) {

        if (info == null) {
            return;
        }

        /*
         * ========================================================
         * RAM
         * ========================================================
         */

        if (textRam != null) {

            textRam.setText(
                    String.format(
                            Locale.US,
                            "RAM física       %s",
                            info.getRamText()
                    )
            );
        }

        if (progressRam != null) {

            progressRam.setProgress(
                    info.ramPercent()
            );
        }

        /*
         * ========================================================
         * WINLATOR
         * ========================================================
         */

        long winlator =
                info.processRam;

        if (textWinlator != null) {

            textWinlator.setText(
                    String.format(
                            Locale.US,
                            "Winlator          %s",
                            MemoryManager.formatMB(
                                    winlator
                            )
                    )
            );
        }

        /*
         * ========================================================
         * WINE
         * ========================================================
         *
         * Ainda não temos separação JNI dos processos Wine.
         * Portanto, temporariamente mostramos N/D.
         *
         * Depois:
         *
         * WineMemory.cpp
         *       ↓
         * JNI
         *       ↓
         * MemoryManager
         */

        if (textWine != null) {

            textWine.setText(
                    "Wine              N/D"
            );
        }

        /*
         * ========================================================
         * BOX64
         * ========================================================
         */

        if (textBox64 != null) {

            textBox64.setText(
                    "Box64             N/D"
            );
        }

        /*
         * ========================================================
         * OTHER
         * ========================================================
         */

        long other =
                Math.max(
                        0,
                        info.processRam
                );

        if (textOther != null) {

            textOther.setText(
                    String.format(
                            Locale.US,
                            "Outros            %s",
                            MemoryManager.formatMB(
                                    other
                            )
                    )
            );
        }

        /*
         * ========================================================
         * SWAP
         * ========================================================
         */

        if (textSwap != null) {

            textSwap.setText(
                    String.format(
                            Locale.US,
                            "SWAP              %s",
                            info.getSwapText()
                    )
            );
        }

        if (progressSwap != null) {

            progressSwap.setProgress(
                    info.swapPercent()
            );
        }

        /*
         * ========================================================
         * VIRTUAL MEMORY
         * ========================================================
         */

        if (textVirtualMemory != null) {

            textVirtualMemory.setText(
                    String.format(
                            Locale.US,
                            "Memória virtual  %s",
                            info.getVirtualRamText()
                    )
            );
        }

        /*
         * ========================================================
         * PROFILE
         * ========================================================
         */

        if (textProfile != null) {

            String profile =
                    info.profile.name();

            textProfile.setText(
                    String.format(
                            Locale.US,
                            "Perfil            %s",
                            profile
                    )
            );
        }
    }

    /*
     * ============================================================
     * GET MEMORY MANAGER
     * ============================================================
     */

    @Nullable
    public MemoryManager getMemoryManager() {

        return memoryManager;
    }

    /*
     * ============================================================
     * CHANGE PROFILE
     * ============================================================
     */

    public void setMemoryProfile(
            MemoryManager.Profile profile) {

        if (memoryManager == null) {
            return;
        }

        memoryManager.setProfile(
                profile
        );
    }

    /*
     * ============================================================
     * CHANGE VIRTUAL RAM
     * ============================================================
     */

    public void setVirtualRamMB(
            long megabytes) {

        if (memoryManager == null) {
            return;
        }

        memoryManager.setVirtualRamMB(
                megabytes
        );
    }

    /*
     * ============================================================
     * CHANGE SWAP CONFIGURATION
     * ============================================================
     */

    public void setSwapGB(
            long gigabytes) {

        if (memoryManager == null) {
            return;
        }

        memoryManager.setConfiguredSwapGB(
                gigabytes
        );
    }

    /*
     * ============================================================
     * REFRESH
     * ============================================================
     */

    public void refreshMemory() {

        if (memoryManager == null) {
            return;
        }

        MemoryManager.MemoryInfo info =
                memoryManager.readMemoryInfo();

        updateMemoryUI(info);
    }

    /*
     * ============================================================
     * LIFECYCLE START
     * ============================================================
     */

    @Override
    public void onStart() {

        super.onStart();

        if (memoryManager != null
                && !memoryManager.isMonitoring()) {

            memoryManager.startMonitoring(
                    info -> {

                        if (isAdded()) {
                            updateMemoryUI(info);
                        }
                    }
            );
        }
    }

    /*
     * ============================================================
     * LIFECYCLE STOP
     * ============================================================
     */

    @Override
    public void onStop() {

        if (memoryManager != null) {

            memoryManager.stopMonitoring();
        }

        super.onStop();
    }

    /*
     * ============================================================
     * DESTROY
     * ============================================================
     */

    @Override
    public void onDestroyView() {

        if (memoryManager != null) {

            memoryManager.stopMonitoring();
        }

        textRam = null;
        textWinlator = null;
        textWine = null;
        textBox64 = null;
        textOther = null;
        textSwap = null;
        textVirtualMemory = null;
        textProfile = null;

        progressRam = null;
        progressSwap = null;

        super.onDestroyView();
    }

    @Override
    public void onDestroy() {

        if (memoryManager != null) {

            memoryManager.stopMonitoring();

            memoryManager = null;
        }

        super.onDestroy();
    }
}
