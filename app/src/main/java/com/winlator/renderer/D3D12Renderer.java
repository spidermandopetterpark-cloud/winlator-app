package com.winlator.renderer;

import android.content.Context;

import java.util.HashMap;
import java.util.Map;

public final class D3D12Renderer {

    private D3D12Renderer() {}

    public static final class Configuration {

        public boolean enabled;
        public boolean mali;
        public boolean vulkan;

        public String backend;

        public final Map<String, String> environment =
                new HashMap<>();
    }

    public static Configuration create(Context context) {

        Configuration config = new Configuration();

        config.mali = GPUDetector.isMali();

        config.vulkan =
                VulkanRenderer.isSupported(context);

        if (config.mali && config.vulkan) {

            config.enabled = true;

            config.backend =
                    "VKD3D-Proton + Vulkan + Mali";

            /*
             * VKD3D-Proton
             */
            config.environment.put(
                    "VKD3D_CONFIG",
                    ""
            );

            /*
             * Seleciona o primeiro dispositivo Vulkan.
             */
            config.environment.put(
                    "VKD3D_VULKAN_DEVICE",
                    "0"
            );

            /*
             * Wine DLL overrides.
             *
             * IMPORTANTE:
             * as DLLs precisam existir no prefixo.
             */
            config.environment.put(
                    "WINEDLLOVERRIDES",
                    "d3d12=n,b;" +
                    "d3d12core=n,b;" +
                    "dxgi=n,b"
            );

        } else {

            config.enabled = false;

            config.backend =
                    "D3D12 indisponível";
        }

        return config;
    }

    public static boolean isSupported(Context context) {
        return create(context).enabled;
    }

    public static String getBackend(Context context) {
        return create(context).backend;
    }

    public static Map<String, String> getEnvironment(
            Context context
    ) {
        return create(context).environment;
    }
}
