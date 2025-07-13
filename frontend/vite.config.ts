import { defineConfig } from "vite";
import react from "@vitejs/plugin-react-swc";
import path from "path";
import { componentTagger } from "lovable-tagger";

// https://vitejs.dev/config/
export default defineConfig(({ mode }) => ({
  server: {
    host: "0.0.0.0",
    port: 5173,
    proxy: {
      '/api': 'http://localhost:8082',
      '/search': 'http://localhost:8082',
    },
    fs: { strict: false },
    historyApiFallback: true,
  },
  plugins: [
    react(),
    mode === 'development' && componentTagger(),
  ].filter(Boolean),
  resolve: {
    alias: {
      "@": path.resolve(__dirname, "./src"),
    },
  },
  optimizeDeps: {
    include: ['pdfjs-dist/build/pdf.worker.js'], // ✅ Include worker
  },
  build: {
    rollupOptions: {
      output: {
        manualChunks: undefined, // ✅ prevent code splitting issues for dynamic import
      },
    },
  },
}));
