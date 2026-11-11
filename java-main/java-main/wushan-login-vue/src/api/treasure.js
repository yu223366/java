/**
 * 宗门宝库 + 宝库寄售接口层
 * 后端：TreasureController（/api/treasure）
 *  · 宝库归宗门管：贡献点兑换 / 存入宗库
 *  · 别人的物品：灵石购买 / 以物换物
 */
import { BASE_URL, tokenStore } from './auth'

const TIMEOUT = Number(import.meta.env.VITE_API_TIMEOUT || 12000)

async function request(path, { method = 'GET', data } = {}) {
  const ctrl = new AbortController()
  const timer = setTimeout(() => ctrl.abort(), TIMEOUT)
  try {
    const res = await fetch(BASE_URL + path, {
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

export const treasureApi = {
  /* ---- 宗门宝库 ---- */
  getVault() {
    return request('/treasure/vault')
  },
  deposit(itemId, count = 1, contributionPrice = null) {
    return request('/treasure/deposit', { method: 'POST', data: { itemId, count, contributionPrice } })
  },
  redeem(stockId, count = 1) {
    return request('/treasure/redeem', { method: 'POST', data: { stockId, count } })
  },
  /* ---- 山友寄售 ---- */
  getConsigns() {
    return request('/treasure/consigns')
  },
  getMyConsigns() {
    return request('/treasure/consigns/mine')
  },
  publish(payload) {
    return request('/treasure/consigns/publish', { method: 'POST', data: payload })
  },
  buy(consignId) {
    return request('/treasure/consigns/buy', { method: 'POST', data: { consignId } })
  },
  barter(consignId, offerItemId) {
    return request('/treasure/consigns/barter', { method: 'POST', data: { consignId, offerItemId } })
  },
  cancel(consignId) {
    return request('/treasure/consigns/cancel', { method: 'POST', data: { consignId } })
  }
}

export default treasureApi
