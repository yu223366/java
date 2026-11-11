/**
 * 雾山市集接口层 —— 摊位分类 / 物品详情 / 按品质用灵石购买
 * 后端：MarketController（/api/market），购得之物直接进背包。
 */
import { BASE_URL, tokenStore } from './auth'

const TIMEOUT = Number(import.meta.env.VITE_API_TIMEOUT || 12000)

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

export const marketApi = {
  /** 摊位字典：丹药材料铺 / 炼器材料铺 / 阵法材料铺 / 药铺 / 法器铺 / 杂货 */
  getShops() {
    return request('/market/shops')
  },
  /** 物品列表，可传 shop / element / grade / keyword / category / page / size */
  getItems(filters = {}) {
    return request('/market/items', { query: filters })
  },
  /** 单件全文详情 */
  getDetail(id) {
    return request('/market/item/' + id)
  },
  /** 购买：扣灵石、扣库存、物品入背包 */
  buy(id, count = 1) {
    return request('/market/buy', { method: 'POST', data: { id, count } })
  }
}

export default marketApi
