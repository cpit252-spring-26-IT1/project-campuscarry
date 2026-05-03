import { defineConfig, loadEnv } from "vite";
import react from "@vitejs/plugin-react";

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, ".", "");
  const devProxyTarget = env.VITE_DEV_PROXY_TARGET || "http://localhost:8000";

  return {
    plugins: [react()],
    build: {
      rollupOptions: {
        output: {
          manualChunks(id) {
            if (!id.includes("node_modules")) {
              return undefined;
            }

            if (id.includes("react-dom") || id.includes("/react/")) {
              return "react-vendor";
            }

            if (id.includes("react-router") || id.includes("@remix-run")) {
              return "router-vendor";
            }

            if (
              id.includes("@heroui") ||
              id.includes("@react-aria") ||
              id.includes("@react-stately") ||
              id.includes("framer-motion") ||
              id.includes("motion")
            ) {
              return "ui-vendor";
            }

            if (id.includes("firebase")) {
              return "firebase-vendor";
            }

            if (id.includes("react-toastify")) {
              return "toast-vendor";
            }

            if (id.includes("react-icons")) {
              return "icons-vendor";
            }

            return "vendor";
          },
        },
      },
    },
    server: {
      port: 3005,
      proxy: {
        "/api": {
          target: devProxyTarget,
          changeOrigin: true,
        },
      },
    },
  };
});
