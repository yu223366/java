/**
 * 求购挂单接口层。
 * 后端接口（WantedController，前缀 /api/wanted）：
 *   GET  /wanted/list    求购榜
 *   POST /wanted/publish  挂出求购 { title, maxPrice, note }
 *   POST /wanted/close    撤销求购 ?id=
 *   GET  /wanted/match?title=  查与某物相关的求购
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

export const wantedApi = {
  list() {
    return request('/wanted/list')
  },
  publish(title, maxPrice, note) {
    return request('/wanted/publish', { method: 'POST', data: { title, maxPrice, note } })
  },
  close(id) {
    return request('/wanted/close', { method: 'POST', query: { id } })
  },
  match(title) {
    return request('/wanted/match', { query: { title } })
  }
}

export default wantedApi
