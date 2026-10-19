/**
 * ============================================================
 *  藏经阁接口层 —— 功法 / 心得 / 修炼资源 / 灵根 / 秘境
 * ============================================================
 *  约定（与 auth.js 一致）：
 *    1. 所有接口均返回 Promise，成功 resolve 业务数据，失败 reject(new Error(中文文案))
 *    2. 统一响应体：{ code: 0, message: 'ok', data: {...} }，code !== 0 视为失败
 *    3. 未接后端时默认走 mock（本地常量过滤），前端流程完整可跑
 *
 *  切换真实后端：
 *    .env 中设置
 *      VITE_API_BASE_URL = https://your-api.com/api
 *      VITE_USE_MOCK     = false
 *
 *  后端需提供的接口清单（路径可自行调整，改这里即可）：
 *    GET  /codex/items          条目检索 ?element=&category=&grade=&q= → { total, items }
 *    GET  /codex/spirit-roots   灵根列表 ?element=&grade= → { total, items }
 *    GET  /codex/realms         秘境列表 ?element=&grade= → { total, items }
 *    GET  /codex/grades         品阶字典（前端已内置，后端可覆盖）
 *    GET  /codex/categories     分类字典（前端已内置，后端可覆盖）
 * ============================================================
 */
import { BASE_URL, USE_MOCK, tokenStore } from './auth'
import { CODEX_ITEMS, SPIRIT_ROOTS, REALMS } from '../constants/codex'

const TIMEOUT = Number(import.meta.env.VITE_API_TIMEOUT || 12000)

/* ------------------------------ 基础请求 ------------------------------ */
async function request(path, { method = 'GET', data, query } = {}) {
  let url = BASE_URL + path
  if (query) {
    const qs = new URLSearchParams(
      Object.entries(query).filter(([, v]) => v !== undefined && v !== null && v !== '')
    ).toString()
    if (qs) url += (url.includes('?') ? '&' : '?') + qs
  }

  const ctrl = new AbortController()
  const timer = setTimeout(() => ctrl.abort(), TIMEOUT)

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
    if (e.name === 'AbortError') throw new Error('山门路远，请求超时，请重试。')
    throw e instanceof Error ? e : new Error('网络异常，请稍后重试。')
  } finally {
    clearTimeout(timer)
  }
}

/* ------------------------------- mock ------------------------------- */
const sleep = (ms) => new Promise((r) => setTimeout(r, ms))

function filterItems(list, { element, grade, q } = {}) {
  return list.filter(
    (it) =>
      (!element || element === 'all' || it.element === element) &&
      (!grade || grade === 'all' || it.grade === grade) &&
      (!q || it.title.includes(q) || it.name.includes(q) || (it.desc || '').includes(q))
  )
}

/* ------------------------------ 对外接口 ------------------------------ */
export const codexApi = {
  /**
   * 条目检索（功法 / 心得 / 修炼资源）
   * @param {{element?:string, category?:string, grade?:string, q?:string}} filters
   * @returns {Promise<{total:number, items:Array}>}
   */
  async getItems(filters = {}) {
    if (USE_MOCK) {
      await sleep(240)
      let list = CODEX_ITEMS
      if (filters.category && filters.category !== 'all') {
        list = list.filter((it) => it.category === filters.category)
      }
      const items = filterItems(list, filters)
      return { total: items.length, items }
    }
    return request('/codex/items', { query: filters })
  },

  /**
   * 灵根列表
   * @param {{element?:string, grade?:string}} filters
   */
  async getSpiritRoots(filters = {}) {
    if (USE_MOCK) {
      await sleep(200)
      const items = filterItems(SPIRIT_ROOTS, filters)
      return { total: items.length, items }
    }
    return request('/codex/spirit-roots', { query: filters })
  },

  /**
   * 秘境列表
   * @param {{element?:string, grade?:string}} filters
   */
  async getRealms(filters = {}) {
    if (USE_MOCK) {
      await sleep(200)
      const items = filterItems(REALMS, filters)
      return { total: items.length, items }
    }
    return request('/codex/realms', { query: filters })
  }
}

export default codexApi
