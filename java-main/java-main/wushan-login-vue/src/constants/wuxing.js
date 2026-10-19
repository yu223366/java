// 五行常量 —— 登录页 / 注册页 / 首页共用
// 色值为本命色，huo 为 true 时表示该元素使用默认「火」主题（火星粒子保持红色）
// 前五者为正五行（金木水火土），后五者为衍生元素（风雷电 / 太极 / 阴阳），
// 相生环仅展示正五行的生克次序，衍生元素仅用于换色与本命展示。
export const ELEMENTS = [
  { key: 'jin', label: '金', en: 'metal', color: '#e8c372', huo: false, trait: '锋锐 · 决断' },
  { key: 'mu', label: '木', en: 'wood', color: '#4caf6e', huo: false, trait: '生发 · 仁厚' },
  { key: 'shui', label: '水', en: 'water', color: '#3a8fd0', huo: false, trait: '灵动 · 智略' },
  { key: 'huo', label: '火', en: 'fire', color: '#e8412b', huo: true, trait: '炽烈 · 心明' },
  { key: 'tu', label: '土', en: 'earth', color: '#c8923a', huo: false, trait: '厚重 · 承载' },
  /* ---- 衍生元素（新增） ---- */
  { key: 'feng', label: '风', en: 'wind', color: '#7fd8cf', huo: false, trait: '疾速 · 无形' },
  { key: 'lei', label: '雷', en: 'thunder', color: '#9d7bf0', huo: false, trait: '震赫 · 果决' },
  { key: 'dian', label: '电', en: 'lightning', color: '#f2e34d', huo: false, trait: '迅烈 · 明澈' },
  { key: 'taiji', label: '太极', en: 'taiji', color: '#d8d2c2', huo: false, trait: '圆融 · 万化之枢' },
  { key: 'yinyang', label: '阴阳', en: 'yinyang', color: '#98a4b0', huo: false, trait: '相济 · 消长之衡' }
]

export const DEFAULT_ELEMENT = 'huo'

export function findElement(key) {
  return ELEMENTS.find((e) => e.key === key) || ELEMENTS[3]
}
