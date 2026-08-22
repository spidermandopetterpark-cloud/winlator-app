package com.winlator.renderer;

import android.opengl.GLES20;

public final class GPUDetector {

    public enum GPU {
        MALI,
        ADRENO,
        POWERVR,
        XCLIPSE,
        UNKNOWN
    }

    private GPUDetector() {}

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    public static String getRenderer() {
        return safe(GLES20.glGetString(GLES20.GL_RENDERER));
    }

    public static String getVendor() {
        return safe(GLES20.glGetString(GLES20.GL_VENDOR));
    }

    public static String getVersion() {
        return safe(GLES20.glGetString(GLES20.GL_VERSION));
    }

    public static GPU detect() {
        String value = (
                getRenderer() + " " +
                getVendor()
        ).toLowerCase();

        if (value.contains("mali") ||
            value.contains("arm")) {
            return GPU.MALI;
        }

        if (value.contains("adreno") ||
            value.contains("qualcomm")) {
            return GPU.ADRENO;
        }

        if (value.contains("powervr") ||
            value.contains("imagination")) {
            return GPU.POWERVR;
        }

        if (value.contains("xclipse")) {
            return GPU.XCLIPSE;
        }

        return GPU.UNKNOWN;
    }

    public static boolean isMali() {
        return detect() == GPU.MALI;
    }

    public static boolean isAdreno() {
        return detect() == GPU.ADRENO;
    }

    public static String getGPUName() {
        return getRenderer();
    }
}
