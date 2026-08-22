package com.winlator.renderer;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

public final class VulkanRenderer {

    private VulkanRenderer() {}

    public static boolean isSupported(Context context) {
        if (context == null) {
            return false;
        }

        PackageManager pm = context.getPackageManager();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return pm.hasSystemFeature(
                    PackageManager.FEATURE_VULKAN_HARDWARE_LEVEL
            );
        }

        return false;
    }

    public static boolean isMaliVulkanSupported(Context context) {
        return GPUDetector.isMali()
                && isSupported(context);
    }

    public static String getBackend(Context context) {
        if (isMaliVulkanSupported(context)) {
            return "VULKAN_MALI";
        }

        if (isSupported(context)) {
            return "VULKAN";
        }

        return "UNSUPPORTED";
    }
}
