import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// 雾山五行登录页 · Vite 配置
export default defineConfig({
  plugins: [vue()],
  server: {
    open: true,
    port: 5173,
    // 监听全部网卡，局域网内可通过 http://本机当前IP:5173 访问（IP 随网络变化，用 ipconfig 查）
    host: '0.0.0.0',
    // 开发期把 /api 代理到认证后端（wushan-auth-server，默认 8080），
    // 这样前端保持 BASE_URL=/api 同源调用，无需 CORS。
    // 代理请求由 vite 服务端发起，固定转发到本机回环即可，不要写死局域网 IP。
    proxy: {
      '/api': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true
      }
    }
  }
})
