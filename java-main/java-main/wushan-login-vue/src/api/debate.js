/**
 * 辩场接口层 —— 同一辩题下分正反两方。
 * 后端接口（DebateController，前缀 /api/debate）：
 *   GET  /debate/list     辩场列表（按辩题分组）
 *   POST /debate/publish   发起辩论 { topic, quote }
 *   POST /debate/reply     应战 { topic, side, quote }
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

export const debateApi = {
  list() {
    return request('/debate/list')
  },
  publish(topic, quote) {
    return request('/debate/publish', { method: 'POST', data: { topic, quote } })
  },
  reply(topic, side, quote) {
    return request('/debate/reply', { method: 'POST', data: { topic, side, quote } })
  },
  /** 给一条陈词点赞（一人一票，票多者胜） */
  like(id) {
    return request('/debate/like', { method: 'POST', data: { id } })
  }
}

export default debateApi
