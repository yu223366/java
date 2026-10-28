/**
 * ============================================================
 *  关键词高亮工具 —— 转义 + <mark> 包裹，供 v-html 安全渲染
 * ============================================================
 *  约定：
 *    1. 输入原文先做 HTML 转义，杜绝注入
 *    2. 关键词为空（空白串）时只返回转义后的原文
 *    3. 命中片段统一包成 <mark class="hl">，配色由调用方 CSS 决定
 * ============================================================
 */

/** HTML 特殊字符转义 */
export function escapeHtml(text) {
  return String(text == null ? '' : text)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}

/** 把关键词中的正则元字符转义，避免用户输入破坏正则 */
function escapeRegExp(s) {
  return s.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
}

/**
 * 高亮命中关键词
 * @param {string} text    原文
 * @param {string} keyword 关键词（大小写不敏感）
 * @returns {string} 转义并包裹 <mark class="hl"> 后的 HTML
 */
export function highlight(text, keyword) {
  const raw = String(text == null ? '' : text)
  const kw = String(keyword || '').trim()
  const safe = escapeHtml(raw)
  if (!kw) return safe
  try {
    const re = new RegExp(escapeRegExp(escapeHtml(kw)), 'gi')
    return safe.replace(re, (m) => `<mark class="hl">${m}</mark>`)
  } catch (e) {
    /* 正则异常时兜底：仅转义不高亮 */
    return safe
  }
}
