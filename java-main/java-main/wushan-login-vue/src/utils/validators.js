/**
 * 表单校验规则集（名号 / 暗语 / 图形符印）
 * 所有函数统一返回「错误文案」或 null，便于直接赋给 UI 的错误位。
 * 校验文案沿用山门口径，便于与登录页风格统一。
 */

// 名号：2~16 位，汉字 / 字母 / 数字 / 下划线
export const USERNAME_RE = /^[\u4e00-\u9fa5A-Za-z0-9_]{2,16}$/

/** 名号校验 */
export function validateUsername(v) {
  const s = (v || '').trim()
  if (!s) return '名号不可空，请先落款。'
  if (s.length < 2) return '名号至少二字，方可记入山籍。'
  if (s.length > 16) return '名号最多十六字，请精简。'
  if (!USERNAME_RE.test(s)) return '名号仅可用汉字、字母、数字与下划线。'
  if (/^\d+$/.test(s)) return '名号不可全为数字，请掺以文字。'
  return null
}

/** 暗语规则清单（用于 UI 实时打勾） */
export const PASSWORD_RULES = [
  { key: 'len', text: '八至二十位', test: (p) => p.length >= 8 && p.length <= 20 },
  { key: 'letter', text: '含字母', test: (p) => /[A-Za-z]/.test(p) },
  { key: 'digit', text: '含数字', test: (p) => /\d/.test(p) },
  { key: 'clean', text: '不含空格与汉字', test: (p) => p.length > 0 && !/[\s\u4e00-\u9fa5]/.test(p) }
]

/** 单条规则是否通过 */
export function rulePass(rule, pw) {
  try {
    return !!rule.test(pw || '')
  } catch {
    return false
  }
}

/** 暗语强度：0~4 分，附文案与颜色 */
export function passwordScore(pw) {
  const p = pw || ''
  if (!p) return { score: 0, label: '未设', color: 'rgba(236,228,210,0.25)' }
  let score = 0
  if (p.length >= 8) score++
  if (p.length >= 12) score++
  if (/[A-Za-z]/.test(p) && /\d/.test(p)) score++
  if (/[^A-Za-z0-9]/.test(p)) score++
  if (/[\s\u4e00-\u9fa5]/.test(p)) score = Math.min(score, 1)

  const table = [
    { label: '弱水', color: '#c0392b' },
    { label: '薄弱', color: '#d97b26' },
    { label: '尚可', color: '#c8a23a' },
    { label: '稳固', color: '#4caf6e' },
    { label: '金汤', color: '#3a8fd0' }
  ]
  const idx = Math.max(0, Math.min(4, score))
  return { score: idx, ...table[idx] }
}

/** 暗语校验（传入名号以阻止暗语与名号雷同） */
export function validatePassword(pw, username = '') {
  const p = pw || ''
  if (!p) return '暗语不可空。'
  if (p.length < 8) return '暗语至少八位，方御外邪。'
  if (p.length > 20) return '暗语最多二十位。'
  if (/[\s\u4e00-\u9fa5]/.test(p)) return '暗语不可含空格或汉字。'
  if (!/[A-Za-z]/.test(p) || !/\d/.test(p)) return '暗语需兼含字母与数字。'
  const u = (username || '').trim()
  if (u && u.length >= 2 && p.toLowerCase() === u.toLowerCase()) return '暗语不可与名号相同。'
  if (u && u.length >= 3 && p.toLowerCase().includes(u.toLowerCase())) return '暗语不可包含名号。'
  return null
}

/** 确认暗语 */
export function validateConfirm(pw, confirm) {
  if (!confirm) return '请再录一次暗语。'
  if (pw !== confirm) return '两次暗语不一，请重录。'
  return null
}

/** 图形符印（图形验证码） */
export function validateCaptchaCode(code, len = 4) {
  const c = (code || '').trim()
  if (!c) return '符印未填。'
  if (c.length !== len) return `符印为 ${len} 位。`
  return null
}
