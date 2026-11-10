<script setup>
/**
 * 分享战报卡：用 canvas 手绘一张水墨风个人战报图，可直接下载分享。
 * 数据来自 /game/* 接口（灵根、灵石、签到）。
 */
import { ref, onMounted } from 'vue'
import { gameApi } from '../api/game'
import { findElement } from '../constants/wuxing'

const canvas = ref(null)
const drawing = ref(false)
const ready = ref(false)

async function load() {
  drawing.value = true
  try {
    const [wallet, spirit, sign] = await Promise.all([
      gameApi.getWallet(),
      gameApi.getMySpirit().catch(() => null),
      gameApi.getSignInStatus().catch(() => null)
    ])
    draw(wallet, spirit, sign)
    ready.value = true
  } catch (e) {
    drawing.value = false
  }
}

function draw(wallet, spirit, sign) {
  const cv = canvas.value
  if (!cv) return
  const W = 640
  const H = 880
  cv.width = W
  cv.height = H
  const ctx = cv.getContext('2d')

  // 底：宣纸
  ctx.fillStyle = '#ece4d2'
  ctx.fillRect(0, 0, W, H)
  // 墨色渐层（用深浅矩形模拟，不依赖渐变 API 以保证导出稳定）
  ctx.fillStyle = 'rgba(14,13,12,0.06)'
  for (let i = 0; i < 40; i++) {
    ctx.fillRect(0, (i * H) / 40, W, 2)
  }
  // 外框
  ctx.strokeStyle = 'rgba(14,13,12,0.55)'
  ctx.lineWidth = 2
  ctx.strokeRect(28, 28, W - 56, H - 56)
  ctx.lineWidth = 1
  ctx.strokeRect(38, 38, W - 76, H - 76)

  const el = spirit?.hasProfile ? findElement(spirit.element) : findElement('huo')
  const elColor = el?.color || '#e8412b'

  // 顶部印章
  ctx.fillStyle = '#a3311f'
  ctx.fillRect(W - 150, 66, 76, 76)
  ctx.fillStyle = '#f6ece0'
  ctx.font = '30px "Kaiti SC", "STKaiti", "楷体", serif'
  ctx.textAlign = 'center'
  ctx.fillText('雾', W - 112, 100)
  ctx.fillText('山', W - 112, 134)

  // 标题
  ctx.textAlign = 'left'
  ctx.fillStyle = '#0e0d0c'
  ctx.font = '46px "Kaiti SC", "STKaiti", "楷体", serif'
  ctx.fillText('山 门 战 报', 76, 118)

  ctx.fillStyle = 'rgba(14,13,12,0.6)'
  ctx.font = '20px "Kaiti SC", "STKaiti", "楷体", serif'
  ctx.fillText('雾山五行 · 一人一签', 78, 152)

  // 分隔线
  ctx.strokeStyle = 'rgba(14,13,12,0.35)'
  ctx.beginPath()
  ctx.moveTo(76, 180)
  ctx.lineTo(W - 76, 180)
  ctx.stroke()

  const rows = [
    ['名 号', spirit?.me || '无名山友', '#0e0d0c'],
    ['本 命', spirit?.hasProfile ? `${el.label}行 · ${spirit.rootName}` : '尚未测定', elColor],
    ['品 阶', spirit?.hasProfile ? `${spirit.gradeLabel}（${spirit.rareLabel}）` : '—', '#0e0d0c'],
    ['灵 石', `${wallet.balance || 0} 枚`, '#a3311f'],
    ['连 签', sign ? `${sign.continuousDays || 0} 天` : '—', '#0e0d0c'],
    ['累计签到', sign ? `${sign.totalDays || 0} 天` : '—', '#0e0d0c']
  ]
  let y = 236
  for (const [k, v, color] of rows) {
    ctx.fillStyle = 'rgba(14,13,12,0.55)'
    ctx.font = '22px "Kaiti SC", "STKaiti", "楷体", serif'
    ctx.fillText(k, 78, y)
    ctx.fillStyle = color
    ctx.font = '30px "Kaiti SC", "STKaiti", "楷体", serif'
    ctx.fillText(String(v), 210, y)
    ctx.strokeStyle = 'rgba(14,13,12,0.12)'
    ctx.beginPath()
    ctx.moveTo(76, y + 16)
    ctx.lineTo(W - 76, y + 16)
    ctx.stroke()
    y += 78
  }

  // 底部题词
  ctx.fillStyle = 'rgba(14,13,12,0.45)'
  ctx.font = '20px "Kaiti SC", "STKaiti", "楷体", serif'
  ctx.textAlign = 'center'
  ctx.fillText('山门路远 · 修行不止', W / 2, H - 96)
  ctx.fillStyle = 'rgba(14,13,12,0.25)'
  ctx.font = '16px "Kaiti SC", "STKaiti", "楷体", serif'
  ctx.fillText('雾山五行 · 生成于山门之内', W / 2, H - 62)
  drawing.value = false
}

function download() {
  const cv = canvas.value
  if (!cv) return
  const a = document.createElement('a')
  a.href = cv.toDataURL('image/png')
  a.download = '雾山五行-战报.png'
  a.click()
}

onMounted(load)
</script>

<template>
  <div class="share-card">
    <canvas ref="canvas" class="cv"></canvas>
    <div class="ops">
      <button class="mini-btn" :disabled="drawing" @click="load">重 新 生 成</button>
      <button class="main-btn" :disabled="!ready" @click="download">下 载 战 报</button>
    </div>
  </div>
</template>

<style scoped>
.share-card { display: flex; flex-direction: column; align-items: center; gap: 16px; }
.cv {
  width: 100%;
  max-width: 320px;
  height: auto;
  border: 1px solid var(--line);
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.35);
}
.ops { display: flex; gap: 12px; }
.mini-btn, .main-btn {
  font: inherit; font-size: 14px; letter-spacing: 3px; cursor: pointer;
  padding: 9px 20px; color: var(--paper);
  background: rgba(236, 228, 210, 0.08); border: 1px solid var(--line); border-radius: 2px;
}
.main-btn { color: #f6ece0; background: linear-gradient(180deg, rgba(163, 49, 31, 0.92), rgba(120, 33, 21, 0.92)); border-color: #c8543c; }
.mini-btn:disabled, .main-btn:disabled { opacity: 0.4; cursor: not-allowed; }
</style>
