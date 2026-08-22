package com.winlator.core;

import android.util.Log;

public final class MemoryManager {

    private static final String TAG = "MemoryManager";

    public enum Mode {
        PHYSICAL_ONLY,
        RAM_PLUS_SWAP
    }

    private static Mode mode = Mode.PHYSICAL_ONLY;

    static {
        try {
            System.loadLibrary("winlator");
        } catch (UnsatisfiedLinkError e) {
            Log.w(TAG, "Biblioteca winlator não carregada", e);
        }
    }

    private MemoryManager() {
    }

    public static void setMode(Mode newMode) {
        if (newMode == null) {
            mode = Mode.PHYSICAL_ONLY;
            nativeSetEnabled(false);
            return;
        }

        mode = newMode;

        nativeSetEnabled(
                mode == Mode.RAM_PLUS_SWAP
        );
    }

    public static Mode getMode() {
        return mode;
    }

    public static boolean isRamPlusSwapEnabled() {
        return mode == Mode.RAM_PLUS_SWAP;
    }

    public static MemoryInfo getMemoryInfo() {
        return new MemoryInfo(
                nativeGetPhysicalRam(),
                nativeGetAvailableRam(),
                nativeGetSwapTotal(),
                nativeGetSwapFree()
        );
    }

    public static long getReportedTotalMemory() {
        MemoryInfo info = getMemoryInfo();

        if (mode == Mode.RAM_PLUS_SWAP) {
            return info.physicalRam + info.swapTotal;
        }

        return info.physicalRam;
    }

    public static long getReportedAvailableMemory() {
        MemoryInfo info = getMemoryInfo();

        if (mode == Mode.RAM_PLUS_SWAP) {
            return info.availableRam + info.swapFree;
        }

        return info.availableRam;
    }

    public static void applyToWine() {
        boolean enabled =
                mode == Mode.RAM_PLUS_SWAP;

        nativeSetEnabled(enabled);

        if (enabled) {
            nativeApply();
        }
    }

    public static final class MemoryInfo {

        public final long physicalRam;
        public final long availableRam;
        public final long swapTotal;
        public final long swapFree;

        public MemoryInfo(
                long physicalRam,
                long availableRam,
                long swapTotal,
                long swapFree
        ) {
            this.physicalRam = physicalRam;
            this.availableRam = availableRam;
            this.swapTotal = swapTotal;
            this.swapFree = swapFree;
        }

        public long getCombinedTotal() {
            return physicalRam + swapTotal;
        }

        public long getCombinedAvailable() {
            return availableRam + swapFree;
        }

        public String toString() {
            return "RAM=" + physicalRam +
                    " available=" + availableRam +
                    " swap=" + swapTotal +
                    " swapFree=" + swapFree;
        }
    }

    private static native long nativeGetPhysicalRam();

    private static native long nativeGetAvailableRam();

    private static native long nativeGetSwapTotal();

    private static native long nativeGetSwapFree();

    private static native void nativeSetEnabled(
            boolean enabled
    );

    private static native boolean nativeApply();
}
