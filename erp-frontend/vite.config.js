import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [vue()],
  define: {
    global: 'window'
  },
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
  server: {
    host: '0.0.0.0', // Listen on all IPs
    proxy: {
      '/api': {
        target: process.env.VITE_BACKEND_TARGET || 'http://localhost:8080',
        changeOrigin: true,
        secure: false
      },
      '/ws-chat': {
        target: process.env.VITE_BACKEND_TARGET || 'http://localhost:8080',
        ws: true,
        changeOrigin: true,
        secure: false
      }
    }
  }
})
