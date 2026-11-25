import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// 端口 5173，接口与图片均代理到本通道后端 8083
export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    strictPort: true,
    proxy: {
      '/api': { target: 'http://127.0.0.1:8083', changeOrigin: true },
      '/uploads': { target: 'http://127.0.0.1:8083', changeOrigin: true }
    }
  },
  build: { outDir: 'dist', chunkSizeWarningLimit: 1500 }
})
