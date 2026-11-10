<script setup>
/**
 * 全局开拍通知：挂在 App.vue 上，无论用户当前在哪个页面都能收到。
 * 每 5 秒轮询 /api/auction/status —— 同时起到「在线心跳」作用，
 * 服务端据此把该用户计入在线列表，开拍前 1 分钟向所有在线用户下发通知。
 */
import { ref, onMounted, onUnmounted } from 'vue'
import { auctionApi } from '../api/auction'

const visible = ref(false)
const text = ref('')
let timer = null
let es = null
let lastKey = ''

/** 显示一条通知（相同内容不重复弹） */
function show(t) {
  if (!t || t === lastKey) return
  lastKey = t
  text.value = t
  visible.value = true
}

/** 订阅 SSE：开拍、落槌、通知实时推送；失败则退回轮询 */
function connect() {
  if (typeof EventSource === 'undefined') return
  try {
    es = new EventSource('/api/auction/stream')
    es.addEventListener('notify', (e) => show(e.data))
    es.addEventListener('round-start', (e) => show(e.data))
    es.addEventListener('hammer', (e) => show(e.data))
    es.onerror = () => {
      if (es) {
        es.close()
        es = null
      }
    }
  } catch (e) {
    es = null
  }
}

async function poll() {
  try {
    const s = await auctionApi.getStatus()
    if (s && s.notify && s.noticeText) {
      show(s.noticeText)
    }
  } catch (e) {
    // 通知轮询失败不影响页面，静默跳过
  }
}

onMounted(() => {
  poll()
  connect()
  timer = setInterval(poll, 20000)
})
onUnmounted(() => {
  clearInterval(timer)
  if (es) es.close()
})
</script>

<template>
  <Transition name="notice-slide">
    <div v-if="visible" class="auction-notice" @click="visible = false">
      <span class="tag">拍 卖 行</span>
      <span class="msg">{{ text }}</span>
      <span class="close">×</span>
    </div>
  </Transition>
</template>

<style scoped>
.auction-notice {
  position: fixed;
  top: 18px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 30;
  display: flex;
  align-items: center;
  gap: 14px;
  max-width: min(680px, 92vw);
  padding: 12px 20px;
  color: var(--paper);
  font-family: "Kaiti SC", "STKaiti", "KaiTi", "楷体", serif;
  font-size: 15px;
  letter-spacing: 2px;
  background: linear-gradient(120deg, rgba(120, 33, 21, 0.96), rgba(30, 20, 18, 0.96));
  border: 1px solid #c8543c;
  box-shadow: 0 10px 34px rgba(0, 0, 0, 0.55), 0 0 22px rgba(232, 65, 43, 0.28);
  cursor: pointer;
}
.tag {
  flex: none;
  padding: 4px 9px;
  font-size: 12px;
  letter-spacing: 3px;
  color: #f6ece0;
  background: #a3311f;
  border: 1px solid #c8543c;
}
.msg { line-height: 1.6; }
.close { flex: none; margin-left: auto; font-size: 20px; color: var(--muted); }
.notice-slide-enter-active, .notice-slide-leave-active { transition: opacity 0.3s, transform 0.3s; }
.notice-slide-enter-from, .notice-slide-leave-to { opacity: 0; transform: translate(-50%, -16px); }
</style>
