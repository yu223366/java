import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// 雾山五行登录页 · Vite 配置
export default defineConfig({
  plugins: [vue()],
  server: {
    open: true,
    port: 5173
  }
})
