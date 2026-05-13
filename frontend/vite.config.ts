import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// DEV : sans variable VITE_API_BASE_URL, les appels /api/* sont proxifiés vers le backend (évite CORS en local).
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8090',
        changeOrigin: true,
      },
    },
  },
});
