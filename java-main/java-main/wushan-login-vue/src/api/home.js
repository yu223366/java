/**
 * ============================================================
 *  首页接口层 —— 预留后端，前端可直接对接
 * ============================================================
 *  约定（与 auth.js 一致）：
 *    1. 所有接口均返回 Promise，成功 resolve 业务数据，失败 reject(new Error(中文文案))
 *    2. 统一响应体：{ code: 0, message: 'ok', data: {...} }，code !== 0 视为失败
 *    3. 未接后端时默认走 mock（本地假数据），前端流程完整可跑
 *
 *  切换真实后端：
 *    .env 中设置
 *      VITE_API_BASE_URL = https://your-api.com/api
 *      VITE_USE_MOCK     = false
 *
 *  后端需提供的接口清单（路径可自行调整，改这里即可）：
 *    GET  /home/user/info            当前登录用户信息 { username, element, nickname }
 *    GET  /home/nav                  导航栏菜单 [{ key, label, en }]
 *    GET  /home/hot-keywords         热门搜索 [{ keyword, element }]
 *    GET  /home/announcements        山门公告 [{ id, text }]
 *    GET  /home/scenes               山中四时图卷 [{ key, title, en, season, desc, tint }]
 *    GET  /home/discourse            山友论道榜 [{ id, name, element, quote, zan }]
 *    GET  /home/search/suggest       搜索联想 ?q=xxx
 *    POST /home/search               搜索 { keyword, element?, grade?, gradeMin?, category?, page?, size? } → { total, page, size, items:[...] }
 *    GET  /home/categories           分类树（分级）[{ key, label, en, children }]
 *    GET  /home/grades               品阶字典（分级）[{ key, label, rank, color }]
 *    GET  /home/discourse/list       论道分页列表 ?page=&size= → { total, items }
 *    POST /home/discourse/like       论道点赞 { id } → { zan }
 *    POST /home/discourse/publish    论道发表 { quote }（Bearer）→ 新语料
 *    GET  /home/market/items         市集物品 ?keyword=&element=&grade=&category=&page=&size=
 *    POST /home/market/buy           市集购买 { id, count }（Bearer）→ 最新物品信息
 *    POST /auth/logout               退出登录（清除 token / 会话）
 *
 *  分级分类搜索说明（后端已实现）：
 *    keyword   关键词（模糊匹配条目名 / 简介）
 *    element   五行筛选：jin/mu/shui/huo/tu/feng/lei/dian/taiji/yinyang
 *    category  分类筛选，支持分级下钻：传一级分类（如 gongfa 功法）
 *              自动包含其全部二级子类（心法/身法/术法/阵法）
 *    grade     品阶精确筛选：fan/huang/xuan/di/tian/sheng
 *    gradeMin  品阶分级筛选：传 di 即「地品及以上」（地/天/圣）
 *    page/size 分页，默认 1 / 20
 * ============================================================
 */
import { BASE_URL, USE_MOCK, tokenStore } from './auth'
import * as treasury from '../utils/treasuryStore'

const TIMEOUT = Number(import.meta.env.VITE_API_TIMEOUT || 12000)

/* ------------------------------ 基础请求 ------------------------------ */
async function request(path, { method = 'GET', data, query, signal } = {}) {
  let url = BASE_URL + path
  if (query) {
    const qs = new URLSearchParams(
      Object.entries(query).filter(([, v]) => v !== undefined && v !== null && v !== '')
    ).toString()
    if (qs) url += (url.includes('?') ? '&' : '?') + qs
  }

  const ctrl = new AbortController()
  const timer = setTimeout(() => ctrl.abort(), TIMEOUT)
  // 外部传入的 signal：用于联想请求的竞态取消（新输入作废上一次请求）
  let onOuterAbort = null
  if (signal) {
    if (signal.aborted) {
      clearTimeout(timer)
      throw new Error('请求已取消')
    }
    onOuterAbort = () => ctrl.abort()
    signal.addEventListener('abort', onOuterAbort)
  }

  try {
    const res = await fetch(url, {
      method,
      headers: {
        'Content-Type': 'application/json',
        ...(tokenStore.get() ? { Authorization: `Bearer ${tokenStore.get()}` } : {})
      },
      body: method === 'GET' || method === 'HEAD' ? undefined : JSON.stringify(data || {}),
      signal: ctrl.signal,
      credentials: 'include'
    })
    const body = await res.json().catch(() => ({}))
    if (!res.ok) throw new Error(body.message || `请求失败（${res.status}）`)
    if (body && typeof body.code !== 'undefined' && body.code !== 0 && body.code !== 200) {
      throw new Error(body.message || '服务端返回异常')
    }
    return body.data !== undefined ? body.data : body
  } catch (e) {
    if (e.name === 'AbortError') {
      // 外部取消（联想竞态）不上抛业务文案，交由调用方静默处理
      if (signal && signal.aborted) throw e
      throw new Error('山门路远，请求超时，请重试。')
    }
    throw e instanceof Error ? e : new Error('网络异常，请稍后重试。')
  } finally {
    clearTimeout(timer)
    if (signal && onOuterAbort) signal.removeEventListener('abort', onOuterAbort)
  }
}

/* ------------------------------- mock ------------------------------- */
const sleep = (ms) => new Promise((r) => setTimeout(r, ms))

const mockNav = [
  { key: 'guanshan', label: '观山', en: 'scenery' },
  { key: 'baoku', label: '宝库', en: 'treasury' },
  { key: 'lundao', label: '论道', en: 'discourse' },
  { key: 'shiji', label: '市集', en: 'market' }
]

const mockHotKeywords = [
  { keyword: '五行阵法', element: 'shui' },
  { keyword: '雾山秘录', element: 'huo' },
  { keyword: '淬火之术', element: 'huo' },
  { keyword: '山涧灵草', element: 'mu' },
  { keyword: '金石锻造', element: 'jin' },
  { keyword: '厚土寻脉', element: 'tu' },
  { keyword: '御风身法', element: 'feng' },
  { keyword: '紫雷诀', element: 'lei' },
  { keyword: '掣电心法', element: 'dian' },
  { keyword: '太极图谱', element: 'taiji' },
  { keyword: '阴阳双鱼', element: 'yinyang' }
]

// 山门公告（跑马灯轮播）
const mockAnnouncements = [
  { id: 1, text: '今日卯时东岭雾散，观山道上可见云海，宜出行。' },
  { id: 2, text: '宝库新录《五行总纲》批注三卷，山友可借阅。' },
  { id: 3, text: '论道台今晚戌时开讲：火行「心明」之辨。' },
  { id: 4, text: '市集三日内以物易物，免山税。' }
]

// 山中四时图卷
const mockScenes = [
  { key: 'spring', title: '春山新霁', en: 'spring', season: '春', desc: '草木生发，山色初醒，涧水初涨。', tint: '#4caf6e' },
  { key: 'summer', title: '夏山听雨', en: 'summer', season: '夏', desc: '骤雨过岭，雾气蒸腾，雷走云低。', tint: '#3a8fd0' },
  { key: 'autumn', title: '秋山落叶', en: 'autumn', season: '秋', desc: '金风肃杀，层林尽染，雁字南归。', tint: '#e8c372' },
  { key: 'winter', title: '冬山覆雪', en: 'winter', season: '冬', desc: '千山素裹，炉火围坐，星火可亲。', tint: '#ece4d2' }
]

// 山友论道榜
const mockDiscourse = [
  { id: 1, name: '焚舟子', element: 'huo', quote: '火不焚舟，舟自渡；心不明镜，镜自昏。', zan: 128 },
  { id: 2, name: '淬金生', element: 'jin', quote: '金生于土而锋于砺，人成于事而明于过。', zan: 96 },
  { id: 3, name: '溪山客', element: 'shui', quote: '水行不争先，争的是滔滔不绝。', zan: 87 },
  { id: 4, name: '扶桑叟', element: 'mu', quote: '草木一秋，尚且向光而立。', zan: 64 },
  { id: 5, name: '厚土翁', element: 'tu', quote: '土不言语，载万物而行。', zan: 52 },
  { id: 6, name: '听风散人', element: 'feng', quote: '风过千山，不留一物，故能过千山。', zan: 47 },
  { id: 7, name: '掌雷真人', element: 'lei', quote: '雷起于沉默，决于一线。', zan: 39 },
  { id: 8, name: '掣电行者', element: 'dian', quote: '电光一瞬，照见本心。', zan: 33 },
  { id: 9, name: '圆转师太', element: 'taiji', quote: '至刚易折，至柔无形，圆转者久。', zan: 28 },
  { id: 10, name: '负阴君', element: 'yinyang', quote: '孤阳不生，独阴不长，相济者昌。', zan: 21 }
]

// 模拟搜索库：按关键字模糊匹配
// 说明：mock 模式下改为实时取本地宝库数据源，后台管理系统的增删改会立刻反映到首页搜索
const mockLibrarySeed = [
  { id: 1, title: '雾山秘录 · 卷一', element: 'huo', desc: '五山武星之火卷，载炽烈心明之法。' },
  { id: 2, title: '金石锻造录', element: 'jin', desc: '锋锐决断，金行兵刃锻造全解。' },
  { id: 3, title: '山涧灵草图鉴', element: 'mu', desc: '生发仁厚，草木图谱与采撷时令。' },
  { id: 4, title: '五行阵法纲要', element: 'shui', desc: '灵动智略，水行布阵之纲要。' },
  { id: 5, title: '厚土寻脉记', element: 'tu', desc: '厚重承载，寻地脉走向之实录。' },
  { id: 6, title: '淬火之术十二式', element: 'huo', desc: '以火淬锋，十二式口诀与图解。' },
  { id: 7, title: '雾山五行总纲', element: 'huo', desc: '五山武星总纲，入境者必读。' },
  { id: 8, title: '御风身法 · 行无踪', element: 'feng', desc: '疾速无形，踏叶不惊的轻功总诀。' },
  { id: 9, title: '紫雷诀', element: 'lei', desc: '震赫果决，引九霄雷气入体的法门。' },
  { id: 10, title: '掣电心法', element: 'dian', desc: '迅烈明澈，以电悟道的观想法门。' },
  { id: 11, title: '太极图谱', element: 'taiji', desc: '圆融万化，阴阳鱼推手与套路图解。' },
  { id: 12, title: '阴阳双鱼石拓本', element: 'yinyang', desc: '相济消长，山中古碑的双鱼拓片。' }
]

/** 首页搜索库：优先取后台维护的宝库条目，回落种子数据 */
function mockLibrary() {
  try {
    const list = treasury.getList('items')
    if (list && list.length) return list
  } catch (e) {
    /* 数据源异常时回落种子 */
  }
  return mockLibrarySeed
}

// 分类树（与后端 wushan_category 种子数据同口径，mock 模式用）
const mockCategories = [
  {
    key: 'gongfa', label: '功法', en: 'technique',
    children: [
      { key: 'xinfa', label: '心法', en: 'mind', children: [] },
      { key: 'shenfa', label: '身法', en: 'movement', children: [] },
      { key: 'shufa', label: '术法', en: 'skill', children: [] },
      { key: 'zhenfa', label: '阵法', en: 'formation', children: [] }
    ]
  },
  { key: 'xinde', label: '心得', en: 'insight', children: [] },
  {
    key: 'ziyuan', label: '修炼资源', en: 'resource',
    children: [
      { key: 'lingyao', label: '灵药', en: 'herb', children: [] },
      { key: 'liancai', label: '炼材', en: 'material', children: [] },
      { key: 'danyao', label: '丹药', en: 'pill', children: [] },
      { key: 'faqi', label: '法器', en: 'artifact', children: [] }
    ]
  }
]

// 品阶（与后端 wushan_grade 种子数据同口径，mock 模式用）
const mockGrades = [
  { key: 'fan', label: '凡品', rank: 1, color: '#9aa08c' },
  { key: 'huang', label: '黄品', rank: 2, color: '#e8c372' },
  { key: 'xuan', label: '玄品', rank: 3, color: '#3a8fd0' },
  { key: 'di', label: '地品', rank: 4, color: '#9d7bf0' },
  { key: 'tian', label: '天品', rank: 5, color: '#e8412b' },
  { key: 'sheng', label: '圣品', rank: 6, color: '#f2e34d' }
]

// 市集物品（与后端 wushan_market_item 种子数据同口径，mock 模式用）
const mockMarket = [
  { id: 1, title: '千年灵芝', desc: '固本培元第一灵药，重伤亦可续命。', element: 'mu', grade: 'tian', gradeLabel: '天品', categoryLabel: '灵药', price: 800, stock: 2, seller: '拾露童子', sold: 3 },
  { id: 2, title: '固元参', desc: '常见的培元参类，价平效稳。', element: 'mu', grade: 'huang', gradeLabel: '黄品', categoryLabel: '灵药', price: 30, stock: 50, seller: '雾山山货铺', sold: 21 },
  { id: 3, title: '朱果', desc: '赤红果实，服之火行亲和大增。', element: 'huo', grade: 'xuan', gradeLabel: '玄品', categoryLabel: '灵药', price: 120, stock: 12, seller: '燃灯叟', sold: 7 },
  { id: 4, title: '庚金精', desc: '万年金气凝成的精粒，锻造上品兵刃之基。', element: 'jin', grade: 'tian', gradeLabel: '天品', categoryLabel: '炼材', price: 900, stock: 1, seller: '藏锋居士', sold: 4 },
  { id: 5, title: '培元丹', desc: '固本培元的常用丹药，性温无碍。', element: 'mu', grade: 'huang', gradeLabel: '黄品', categoryLabel: '丹药', price: 40, stock: 30, seller: '雾山药庐', sold: 18 },
  { id: 6, title: '青锋剑', desc: '三尺青锋，削铁如泥，剑修常备。', element: 'jin', grade: 'xuan', gradeLabel: '玄品', categoryLabel: '法器', price: 200, stock: 5, seller: '藏锋居士', sold: 3 },
  { id: 7, title: '赤焰葫芦', desc: '内蕴一缕地心火，可放可收。', element: 'huo', grade: 'di', gradeLabel: '地品', categoryLabel: '法器', price: 480, stock: 3, seller: '焚舟子', sold: 1 },
  { id: 8, title: '避风铜铃', desc: '风起铃动，先人一步知险。', element: 'feng', grade: 'huang', gradeLabel: '黄品', categoryLabel: '法器', price: 35, stock: 25, seller: '听风散人', sold: 10 }
]

/* ------------------------------ 对外接口 ------------------------------ */
export const homeApi = {
  /** 当前登录用户信息（后端模式下返回；mock 由前端本地拼装） */
  getUserInfo(username) {
    if (USE_MOCK) {
      return Promise.resolve({
        username: username || '雾山行者',
        element: 'huo',
        nickname: username || '雾山行者'
      })
    }
    return request('/home/user/info')
  },

  /** 导航栏菜单 */
  getNav() {
    if (USE_MOCK) return Promise.resolve(mockNav)
    return request('/home/nav')
  },

  /** 热门搜索关键词 */
  getHotKeywords() {
    if (USE_MOCK) return Promise.resolve(mockHotKeywords)
    return request('/home/hot-keywords')
  },

  /** 山门公告（跑马灯轮播） */
  getAnnouncements() {
    if (USE_MOCK) return Promise.resolve(mockAnnouncements)
    return request('/home/announcements')
  },

  /** 山中四时图卷 */
  getScenes() {
    if (USE_MOCK) return Promise.resolve(mockScenes)
    return request('/home/scenes')
  },

  /** 山友论道榜 */
  getDiscourse() {
    if (USE_MOCK) return Promise.resolve(mockDiscourse)
    return request('/home/discourse')
  },

  /**
   * 搜索联想（输入时调用）
   * @param {string} q 已输入关键词
   * @param {{signal?:AbortSignal}} [opts] 可选 AbortSignal，用于取消上一次联想请求
   * @returns {Promise<string[]>} 建议词列表（最多 8 条）
   */
  async suggest(q, opts = {}) {
    const kw = String(q || '').trim()
    if (USE_MOCK) {
      if (!kw) return []
      await sleep(140)
      const lower = kw.toLowerCase()
      const hits = mockLibrary().filter(
        (i) =>
          i.title.includes(kw) ||
          i.desc.includes(kw) ||
          i.title.toLowerCase().includes(lower)
      )
      // 去重：同一标题只出现一次，标题优先于简介
      const titles = []
      hits.forEach((i) => { if (titles.indexOf(i.title) === -1) titles.push(i.title) })
      return titles.slice(0, 8)
    }
    return request('/home/search/suggest', { query: { q: kw }, signal: opts.signal })
  },

  /**
   * 搜索（分级分类组合筛选）
   * @param {string} keyword                    关键词
   * @param {{element?:string, category?:string, grade?:string, gradeMin?:string,
   *          page?:number, size?:number}} [filters] 可选筛选项（后端模式生效）
   *   - element   五行筛选（all = 不限）
   *   - category  分类筛选，传一级分类自动下钻其子类（all = 不限）
   *   - grade     品阶精确筛选
   *   - gradeMin  品阶分级筛选（该品及以上，如 di = 地品及以上）
   * @returns {Promise<{total:number, page:number, size:number, items:Array}>}
   */
  async search(keyword, filters = {}) {
    const kw = String(keyword || '').trim()
    if (USE_MOCK) {
      await sleep(500)
      let list = mockLibrary()
      // mock 模式下也尽力支持 element / category 基础过滤，便于离线演示
      if (filters.element && filters.element !== 'all') {
        list = list.filter((i) => i.element === filters.element)
      }
      if (filters.category && filters.category !== 'all') {
        list = list.filter((i) => i.category === filters.category)
      }
      const items = kw
        ? list.filter(
            (i) =>
              String(i.title || '').includes(kw) || String(i.desc || '').includes(kw)
          )
        : list
      return { total: items.length, page: 1, size: items.length, items }
    }
    return request('/home/search', {
      method: 'POST',
      data: {
        keyword: kw,
        element: filters.element,
        category: filters.category,
        grade: filters.grade,
        gradeMin: filters.gradeMin,
        page: filters.page,
        size: filters.size
      }
    })
  },

  /** 分类树（分级：一级含二级，如 功法 → 心法/身法/术法/阵法） */
  getCategories() {
    if (USE_MOCK) return Promise.resolve(mockCategories)
    return request('/home/categories')
  },

  /** 品阶字典（分级：凡→黄→玄→地→天→圣，rank 1~6） */
  getGrades() {
    if (USE_MOCK) return Promise.resolve(mockGrades)
    return request('/home/grades')
  },

  /* ------------------------------ 论道堂（导航「论道」） ------------------------------ */

  /**
   * 论道分页列表（按点赞数降序）
   * @returns {Promise<{total:number, items:Array<{id,name,element,quote,zan}>}>}
   */
  getDiscourseList({ page = 1, size = 8 } = {}) {
    if (USE_MOCK) {
      const sorted = [...mockDiscourse].sort((a, b) => b.zan - a.zan)
      const start = (page - 1) * size
      return Promise.resolve({
        total: sorted.length,
        page,
        size,
        items: sorted.slice(start, start + size)
      })
    }
    return request('/home/discourse/list', { query: { page, size } })
  },

  /** 论道点赞（后端自增并返回最新 zan） */
  likeDiscourse(id) {
    if (USE_MOCK) {
      const d = mockDiscourse.find((x) => x.id === id)
      if (d) d.zan += 1
      return Promise.resolve(d ? { ...d } : null)
    }
    return request('/home/discourse/like', { method: 'POST', data: { id } })
  },

  /** 发表论道语录（需登录，后端取 token 中的名号与五行） */
  publishDiscourse(quote) {
    if (USE_MOCK) {
      const d = { id: Date.now(), name: '雾山行者', element: 'huo', quote, zan: 0 }
      return Promise.resolve(d)
    }
    return request('/home/discourse/publish', { method: 'POST', data: { quote } })
  },

  /* ------------------------------ 市集（导航「市集」） ------------------------------ */

  /**
   * 市集物品列表（关键词 + 五行 + 品阶 + 分类组合筛选，分页）
   * @param {{keyword?:string, element?:string, grade?:string, category?:string,
   *          page?:number, size?:number}} filters
   * @returns {Promise<{total:number, items:Array}>}
   */
  getMarketItems(filters = {}) {
    if (USE_MOCK) {
      let list = mockMarket
      if (filters.keyword) list = list.filter((i) => i.title.includes(filters.keyword) || i.desc.includes(filters.keyword))
      if (filters.element && filters.element !== 'all') list = list.filter((i) => i.element === filters.element)
      if (filters.grade && filters.grade !== 'all') list = list.filter((i) => i.grade === filters.grade)
      const page = filters.page || 1
      const size = filters.size || 12
      return Promise.resolve({ total: list.length, page, size, items: list.slice((page - 1) * size, page * size) })
    }
    return request('/home/market/items', {
      query: {
        keyword: filters.keyword,
        element: filters.element,
        grade: filters.grade,
        category: filters.category,
        page: filters.page,
        size: filters.size
      }
    })
  },

  /** 购买市集物品（后端校验库存并扣减；mock 模式仅本地扣） */
  buyMarketItem(id, count = 1) {
    if (USE_MOCK) {
      const m = mockMarket.find((x) => x.id === id)
      if (!m || m.stock < count) return Promise.reject(new Error('库存不足'))
      m.stock -= count
      m.sold += count
      return Promise.resolve({ ...m })
    }
    return request('/home/market/buy', { method: 'POST', data: { id, count } })
  },

  /** 退出登录 */
  async logout() {
    if (USE_MOCK) {
      await sleep(300)
      tokenStore.clear()
      return { ok: true }
    }
    const r = await request('/auth/logout', { method: 'POST' })
    tokenStore.clear()
    return r
  }
}

export default homeApi
