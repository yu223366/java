/**
 * 图形验证码工具 —— 本地生成（mock / 离线模式）使用
 * 若后端已提供验证码接口（返回 base64 图片），CaptchaBox 会直接渲染远程图，本文件不参与。
 */

// 排除易混字符：0 O o 1 I l 2 Z 8 B
const POOL = 'ACDEFGHJKLMNPQRSTUVWXY34679'
const INK = ['#e8c372', '#4caf6e', '#3a8fd0', '#e8412b', '#c8923a']

/** 随机符文字串 */
export function randomText(len = 4) {
  let s = ''
  for (let i = 0; i < len; i++) s += POOL[Math.floor(Math.random() * POOL.length)]
  return s
}

/**
 * 在 canvas 上绘制符印
 * @param {HTMLCanvasElement} canvas
 * @param {string} text
 */
export function drawCaptcha(canvas, text, opts = {}) {
  if (!canvas) return
  const w = opts.width || 128
  const h = opts.height || 46
  const dpr = window.devicePixelRatio || 1
  canvas.width = w * dpr
  canvas.height = h * dpr
  canvas.style.width = w + 'px'
  canvas.style.height = h + 'px'

  const ctx = canvas.getContext('2d')
  ctx.setTransform(dpr, 0, 0, dpr, 0, 0)
  ctx.clearRect(0, 0, w, h)

  // 底：淡淡宣纸
  ctx.fillStyle = 'rgba(236,228,210,0.05)'
  ctx.fillRect(0, 0, w, h)
  ctx.strokeStyle = 'rgba(236,228,210,0.18)'
  ctx.lineWidth = 1
  ctx.strokeRect(0.5, 0.5, w - 1, h - 1)

  // 干扰：三道游丝
  for (let i = 0; i < 3; i++) {
    ctx.beginPath()
    ctx.strokeStyle = `rgba(236,228,210,${0.06 + Math.random() * 0.1})`
    ctx.lineWidth = 1
    ctx.moveTo(0, Math.random() * h)
    ctx.bezierCurveTo(w * 0.3, Math.random() * h, w * 0.6, Math.random() * h, w, Math.random() * h)
    ctx.stroke()
  }
  // 干扰：墨点
  for (let i = 0; i < 24; i++) {
    ctx.fillStyle = `rgba(236,228,210,${0.05 + Math.random() * 0.12})`
    ctx.beginPath()
    ctx.arc(Math.random() * w, Math.random() * h, Math.random() * 1.4 + 0.4, 0, 7)
    ctx.fill()
  }

  // 字符：逐字随机旋转 / 抖动，字色取五行色
  const gap = w / (text.length + 0.6)
  ctx.textAlign = 'center'
  ctx.textBaseline = 'middle'
  for (let i = 0; i < text.length; i++) {
    const ch = text[i]
    const size = 20 + Math.random() * 6
    ctx.save()
    ctx.translate(gap * (i + 0.8) + (Math.random() - 0.5) * 5, h / 2 + (Math.random() - 0.5) * 8)
    ctx.rotate((Math.random() - 0.5) * 0.55)
    ctx.font = `700 ${size}px "Kaiti SC","STKaiti","KaiTi",serif`
    ctx.fillStyle = INK[Math.floor(Math.random() * INK.length)]
    ctx.globalAlpha = 0.92
    ctx.fillText(ch, 0, 0)
    ctx.restore()
  }
  ctx.globalAlpha = 1
}
