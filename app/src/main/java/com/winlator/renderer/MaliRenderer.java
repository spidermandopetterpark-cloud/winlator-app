package com.winlator.renderer;

import android.content.Context;

public final class MaliRenderer {

    public enum Backend {
        VKD3D_VULKAN,
        OPENGL,
        UNSUPPORTED
    }

    private MaliRenderer() {}

    public static boolean isMali() {
        return GPUDetector.isMali();
    }

    public static boolean hasVulkan(Context context) {
        return VulkanRenderer.isMaliVulkanSupported(context);
    }

    public static Backend selectBackend(Context context) {

        if (!isMali()) {
            return Backend.UNSUPPORTED;
        }

        /*
         * D3D12:
         *
         * D3D12
         *   ↓
         * VKD3D-Proton
         *   ↓
         * Vulkan
         *   ↓
         * Mali
         */
        if (hasVulkan(context)) {
            return Backend.VKD3D_VULKAN;
        }

        /*
         * OpenGL fica apenas como fallback
         * para caminhos que não sejam D3D12.
         */
        return Backend.OPENGL;
    }

    public static boolean canTryD3D12(Context context) {
        return selectBackend(context)
                == Backend.VKD3D_VULKAN;
    }

    public static String getBackendName(Context context) {

        switch (selectBackend(context)) {

            case VKD3D_VULKAN:
                return "VKD3D-Proton + Vulkan + Mali";

            case OPENGL:
                return "OpenGL ES + Mali";

            default:
                return "Unsupported";
        }
    }
}
