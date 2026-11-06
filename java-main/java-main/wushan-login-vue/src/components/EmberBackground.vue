<script setup>
// 雾 + 火墨 粒子背景（Canvas）
//
// 性能改造要点（旧版卡顿的根因）：
//   1. 旧版每帧对「每一颗」粒子调用 createRadialGradient + arc + fill，
//      200 颗粒子 × 60 帧 ≈ 每秒 1.2 万次渐变对象创建，主线程直接被拖死。
//      现在改为：光晕 / 雾气各预渲染成一张离屏贴图（sprite），每帧只做 drawImage。
//   2. 旧版 mousemove 无节流、无上限地 push 新粒子，鼠标一划粒子数暴涨。
//      现在：定长对象池（MAX_EMBERS）轮转复用 + 鼠标 90ms 节流。
//   3. 旧版用 splice 删除粒子，高频触发 GC 抖动。现在：alive 标记，零数组增删。
//   4. 旧版在后台标签页仍在跑。现在：document.hidden 时跳过全部绘制。
//   5. 首页 / 宝库 / 后台内容多，通过 intensity 自动降到 30fps 并降低环境火星密度。
import { ref, onMounted, onBeforeUnmount } from 'vue'

const props = defineProps({
  emberColor: { type: String, default: '#e8412b' },
  // 1 = 登录 / 注册页（满帧满密度）；< 1 = 首页 / 宝库 / 后台（降帧降密度）
  intensity: { type: Number, default: 1 }
})

const cv = ref(null)

let ctx
let W = 0
let H = 0
let dpr = 1
let raf = 0
let last = 0
let resizeTimer = null
let moveTs = 0

/* -------------------- 火星对象池（定长，轮转复用） -------------------- */
const MAX_EMBERS = 160
const POOL = []
let cursor = 0
for (let i = 0; i < MAX_EMBERS; i++) {
  POOL.push({ alive: false, x: 0, y: 0, vx: 0, vy: 0, life: 1, r: 1, color: '#e8412b' })
}

/* -------------------- 雾团 -------------------- */
let mist = []

/* -------------------- 离屏贴图缓存 -------------------- */
const spriteCache = new Map()

function hexA(hex, a) {
  const s = String(hex || '').trim()
  if (s.charAt(0) !== '#' || (s.length !== 7 && s.length !== 4)) {
    return `rgba(232, 65, 43, ${a})`
  }
  let n = s.length === 4
    ? parseInt(s.charAt(1) + s.charAt(1) + s.charAt(2) + s.charAt(2) + s.charAt(3) + s.charAt(3), 16)
    : parseInt(s.slice(1), 16)
  return `rgba(${(n >> 16) & 255}, ${(n >> 8) & 255}, ${n & 255}, ${a})`
}

/** 火星光晕贴图：中心亮、边缘透明，按主题色缓存 */
function glowSprite(color) {
  const key = 'g|' + color
  const hit = spriteCache.get(key)
  if (hit) return hit
  const S = 64
  const c = document.createElement('canvas')
  c.width = S
  c.height = S
  const g = c.getContext('2d')
  const grd = g.createRadialGradient(S / 2, S / 2, 0, S / 2, S / 2, S / 2)
  grd.addColorStop(0, hexA(color, 1))
  grd.addColorStop(0.28, hexA(color, 0.45))
  grd.addColorStop(1, hexA(color, 0))
  g.fillStyle = grd
  g.fillRect(0, 0, S, S)
  spriteCache.set(key, c)
  return c
}

/** 雾团贴图：只做一次，所有雾团共用缩放绘制 */
function mistSprite() {
  const key = 'm'
  const hit = spriteCache.get(key)
  if (hit) return hit
  const S = 256
  const c = document.createElement('canvas')
  c.width = S
  c.height = S
  const g = c.getContext('2d')
  const grd = g.createRadialGradient(S / 2, S / 2, 0, S / 2, S / 2, S / 2)
  grd.addColorStop(0, 'rgba(120,140,135,1)')
  grd.addColorStop(0.55, 'rgba(120,140,135,0.35)')
  grd.addColorStop(1, 'rgba(120,140,135,0)')
  g.fillStyle = grd
  g.fillRect(0, 0, S, S)
  spriteCache.set(key, c)
  return c
}

/* -------------------- 尺寸 / 雾团初始化 -------------------- */
function applySize() {
  const c = cv.value
  if (!c) return
  W = window.innerWidth
  H = window.innerHeight
  // 限制到 1.25 倍：全屏画布的显存和填充成本与像素数成正比，
  // 火星是柔光贴图，降采样肉眼几乎看不出，但滚动时的合成压力明显下降
  dpr = Math.min(window.devicePixelRatio || 1, 1.25)
  c.width = Math.round(W * dpr)
  c.height = Math.round(H * dpr)
  c.style.width = W + 'px'
  c.style.height = H + 'px'
  ctx.setTransform(dpr, 0, 0, dpr, 0, 0)

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

function onResize() {
  clearTimeout(resizeTimer)
  resizeTimer = setTimeout(applySize, 120)
}

/* -------------------- 粒子生成 -------------------- */
/** 取一个空槽位；池满时轮转覆盖最旧的一颗（保证上限，绝不无限增长） */
function takeSlot() {
  for (let k = 0; k < MAX_EMBERS; k++) {
    const idx = (cursor + k) % MAX_EMBERS
    if (!POOL[idx].alive) {
      cursor = (idx + 1) % MAX_EMBERS
      return POOL[idx]
    }
  }
  const p = POOL[cursor]
  cursor = (cursor + 1) % MAX_EMBERS
  return p
}

function spawnEmber(x, y, n, spread) {
  for (let i = 0; i < n; i++) {
    const p = takeSlot()
    p.alive = true
    p.x = x
    p.y = y
    p.vx = (Math.random() - 0.5) * spread
    p.vy = -Math.random() * 1.6 - 0.4
    p.life = 1
    p.r = Math.random() * 2.4 + 1.2
    p.color = Math.random() < 0.5 ? props.emberColor : '#ff7a18'
  }
}

/* -------------------- 主循环 -------------------- */
function draw(ts) {
  raf = requestAnimationFrame(draw)
  // 后台标签页 / 窗口不可见：不做任何绘制
  if (typeof document !== 'undefined' && document.hidden) {
    last = 0
    return
  }

  const full = props.intensity >= 1
  const interval = full ? 1000 / 60 : 1000 / 30
  if (!last) last = ts
  const elapsed = ts - last
  if (elapsed < interval - 1) return
  last = ts

  // 以 60fps 为基准的帧倍率，保证不同帧率下速度一致
  const dt = Math.min(elapsed, 50) / 16.667

  ctx.clearRect(0, 0, W, H)

  // ---- 雾气：贴图缩放绘制 ----
  ctx.globalCompositeOperation = 'source-over'
  const ms = mistSprite()
  for (let i = 0; i < mist.length; i++) {
    const m = mist[i]
    m.x += m.vx * dt
    if (m.x < -m.r) m.x = W + m.r
    else if (m.x > W + m.r) m.x = -m.r
    ctx.globalAlpha = m.a
    ctx.drawImage(ms, m.x - m.r, m.y - m.r, m.r * 2, m.r * 2)
  }

  // ---- 火星：叠加发光 ----
  ctx.globalCompositeOperation = 'lighter'
  for (let i = 0; i < MAX_EMBERS; i++) {
    const p = POOL[i]
    if (!p.alive) continue
    p.x += p.vx * dt
    p.y += p.vy * dt
    p.vy += 0.01 * dt
    p.life -= 0.012 * dt
    p.r *= 0.992
    if (p.life <= 0) {
      p.alive = false
      continue
    }
    const d = p.r * 8
    ctx.globalAlpha = p.life > 1 ? 1 : p.life
    ctx.drawImage(glowSprite(p.color), p.x - d * 0.5, p.y - d * 0.5, d, d)
  }

  ctx.globalAlpha = 1
  ctx.globalCompositeOperation = 'source-over'

  // ---- 环境火星（自下而上缓缓升起）----
  if (Math.random() < 0.5 * props.intensity * dt) {
    spawnEmber(Math.random() * W, H * 0.5 + Math.random() * H * 0.4, 1, 1.2)
  }
}

/* -------------------- 交互 -------------------- */
function onMove(e) {
  if (props.intensity < 0.4) return
  // 节流：鼠标狂划也不会疯狂生成粒子（旧版每个 mousemove 都判定，粒子数会失控）
  const now = performance.now()
  if (now - moveTs < 90) return
  moveTs = now
  if (Math.random() < 0.4) spawnEmber(e.clientX, e.clientY, 1, 0.8)
}

function onClick(e) {
  spawnEmber(e.clientX, e.clientY, 28, 3)
}

/** 登录成功时调用：在 (x, y) 处爆发 n 颗火星 */
function burst(x, y, n, spread) {
  spawnEmber(x, y, Math.min(n || 0, 90), spread)
}

defineExpose({ burst })

onMounted(() => {
  const c = cv.value
  if (!c) return
  ctx = c.getContext('2d', { alpha: true })
  applySize()
  window.addEventListener('resize', onResize)
  window.addEventListener('mousemove', onMove, { passive: true })
  window.addEventListener('click', onClick, { passive: true })
  raf = requestAnimationFrame(draw)
})

onBeforeUnmount(() => {
  if (raf) cancelAnimationFrame(raf)
  raf = 0
  clearTimeout(resizeTimer)
  window.removeEventListener('resize', onResize)
  window.removeEventListener('mousemove', onMove)
  window.removeEventListener('click', onClick)
  spriteCache.clear()
  POOL.forEach((p) => { p.alive = false })
})
</script>

<template>
  <canvas ref="cv" class="bg-canvas" aria-hidden="true"></canvas>
</template>

<style scoped>
.bg-canvas {
  position: fixed;
  inset: 0;
  z-index: 1;
  /* 纯装饰层：不参与命中测试 */
  pointer-events: none;
  /* canvas 本身就是独立合成层，无需再 translateZ(0) 提升 */
}
</style>
