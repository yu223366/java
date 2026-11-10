/**
 * ============================================================
 *  宗门 + 观山台接口层
 * ============================================================
 *  后端接口：
 *    GET  /sect/list     宗门列表
 *    GET  /sect/mine      我所在的宗门
 *    POST /sect/create    立宗 ?name=&element=
 *    POST /sect/join      投山门 ?sectId=
 *    POST /sect/quit      离山
 *    POST /sect/donate    捐献 ?amount=
 *    GET  /observer/overview  观山台总览
 * ============================================================
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

export const sectApi = {
  list() {
    return request('/sect/list')
  },
  mine() {
    return request('/sect/mine')
  },
  create(name, element) {
    return request('/sect/create', { method: 'POST', query: { name, element } })
  },
  join(sectId) {
    return request('/sect/join', { method: 'POST', query: { sectId } })
  },
  quit() {
    return request('/sect/quit', { method: 'POST' })
  },
  donate(amount) {
    return request('/sect/donate', { method: 'POST', query: { amount } })
  },
  /** 宗主传位 */
  transfer(targetUserId) {
    return request('/sect/transfer', { method: 'POST', query: { targetUserId } })
  },
  /** 解散宗门 */
  disband() {
    return request('/sect/disband', { method: 'POST' })
  }
}

export const observerApi = {
  overview() {
    return request('/observer/overview')
  },
  /** 导航栏徽标：各入口实时状态 */
  badges() {
    return request('/observer/badges')
  }
}

export default sectApi
