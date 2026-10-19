import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// 雾山五行登录页 · Vite 配置
export default defineConfig({
  plugins: [vue()],
  server: {
    open: true,
    port: 5173,
    // 开发期把 /api 代理到认证后端（wushan-auth-server，默认 8080），
    // 这样前端保持 BASE_URL=/api 同源调用，无需 CORS。
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})
