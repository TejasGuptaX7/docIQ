import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react-swc';
import path from 'path';
import { componentTagger } from 'lovable-tagger';

export default defineConfig(({ mode }) => ({
  server: {
    host: '0.0.0.0',
    port: 5173,
    proxy: {
      // ⬅️  All /api/** calls are forwarded to Spring Boot
      '/api': {
        target: 'http://localhost:8082',
        changeOrigin: true,
      },
    },
    fs: { strict: false },
  },

  plugins: [
    react(),
    mode === 'development' && componentTagger(),
  ].filter(Boolean),

  resolve: {
    alias: { '@': path.resolve(__dirname, './src') },
  },

  optimizeDeps: {
    include: ['pdfjs-dist/build/pdf.worker.js'],
  },

  build: {
    rollupOptions: {
      output: { manualChunks: undefined }, // avoid dynamic-import worker issues
    },
  },
}));
