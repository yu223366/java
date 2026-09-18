<script setup>
// 雾 + 火墨 粒子背景（Canvas）
// 还原原 login-wushan.html 的 <script> 粒子逻辑，封装为独立组件，
// 通过 emberColor prop 接收主题色，并对外暴露 burst() 供登录时引爆。
import { ref, onMounted, onBeforeUnmount } from 'vue'

const props = defineProps({
  emberColor: { type: String, default: '#e8412b' }
})

const cv = ref(null)
let ctx
let W = 0
let H = 0
let embers = []
let mist = []
let raf = 0

function hexA(hex, a) {
  const n = parseInt(hex.slice(1), 16)
  return `rgba(${(n >> 16) & 255}, ${(n >> 8) & 255}, ${n & 255}, ${a})`
}

function resize() {
  W = cv.value.width = innerWidth
  H = cv.value.height = innerHeight
  mist = []
  for (let i = 0; i < 6; i++) {
    mist.push({
      x: Math.random() * W,
      y: Math.random() * H * 0.6 + 40,
      r: Math.random() * 220 + 160,
      vx: (Math.random() - 0.5) * 0.25,
      a: Math.random() * 0.05 + 0.03
    })
  }
}

function spawnEmber(x, y, n, spread) {
  for (let i = 0; i < n; i++) {
    embers.push({
      x,
      y,
      vx: (Math.random() - 0.5) * spread,
      vy: -Math.random() * 1.6 - 0.4,
      life: 1,
      r: Math.random() * 2.4 + 1.2,
      hue: Math.random() < 0.5 ? props.emberColor : '#ff7a18'
    })
  }
}

function draw() {
  ctx.clearRect(0, 0, W, H)

  // 雾气
  ctx.globalCompositeOperation = 'source-over'
  for (const m of mist) {
    m.x += m.vx
    if (m.x < -m.r) m.x = W + m.r
    if (m.x > W + m.r) m.x = -m.r
    const g = ctx.createRadialGradient(m.x, m.y, 0, m.x, m.y, m.r)
    g.addColorStop(0, `rgba(120,140,135,${m.a})`)
    g.addColorStop(1, 'rgba(120,140,135,0)')
    ctx.fillStyle = g
    ctx.beginPath()
    ctx.arc(m.x, m.y, m.r, 0, 7)
    ctx.fill()
  }

  // 火星（叠加发光）
  ctx.globalCompositeOperation = 'lighter'
  for (let i = embers.length - 1; i >= 0; i--) {
    const p = embers[i]
    p.x += p.vx
    p.y += p.vy
    p.vy += 0.01
    p.life -= 0.012
    p.r *= 0.992
    if (p.life <= 0) {
      embers.splice(i, 1)
      continue
    }
    const g = ctx.createRadialGradient(p.x, p.y, 0, p.x, p.y, p.r * 4)
    g.addColorStop(0, hexA(p.hue, p.life))
    g.addColorStop(1, hexA(p.hue, 0))
    ctx.fillStyle = g
    ctx.beginPath()
    ctx.arc(p.x, p.y, p.r * 4, 0, 7)
    ctx.fill()
  }

  ctx.globalCompositeOperation = 'source-over'
  if (embers.length < 200 && Math.random() < 0.5) {
    spawnEmber(Math.random() * W, H * 0.5 + Math.random() * H * 0.4, 1, 1.2)
  }
  raf = requestAnimationFrame(draw)
}

function onMove(e) {
  if (Math.random() < 0.4) spawnEmber(e.clientX, e.clientY, 1, 0.8)
}
function onClick(e) {
  spawnEmber(e.clientX, e.clientY, 40, 3)
}

// 登录成功时调用：在 (x, y) 处爆发 n 颗火星
function burst(x, y, n, spread) {
  spawnEmber(x, y, n, spread)
}

defineExpose({ burst })

onMounted(() => {
  ctx = cv.value.getContext('2d')
  resize()
  addEventListener('resize', resize)
  addEventListener('mousemove', onMove)
  addEventListener('click', onClick)
  draw()
})

onBeforeUnmount(() => {
  cancelAnimationFrame(raf)
  removeEventListener('resize', resize)
  removeEventListener('mousemove', onMove)
  removeEventListener('click', onClick)
})
</script>

<template>
  <canvas ref="cv" class="bg-canvas"></canvas>
</template>

<style scoped>
.bg-canvas {
  position: fixed;
  inset: 0;
  z-index: 1;
}
</style>
