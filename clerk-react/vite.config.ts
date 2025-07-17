import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      // Proxy all /api requests to the Spring Boot backend
      '/api': {
        target: 'http://localhost:8080',  // <— adjust to your backend port
        changeOrigin: true,
        secure: false,
        rewrite: (path) => path,          // keep the /api prefix
      },
    },
  },
});
