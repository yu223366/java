/**
 * ============================================================
 *  山门世界玩法接口层 —— 背包 / 秘境 / 炼丹 / 对决 / 传书
 * ============================================================
 *  后端接口（WorldController，前缀 /api/world）：
 *    GET  /world/bag              背包 + 灵石余额
 *    GET  /world/realms           秘境列表
 *    POST /world/explore/start    进入秘境 ?realmId=
 *    POST /world/explore/choose   选择事件选项 ?choiceIdx=
 *    GET  /world/explore/logs     探索记录
 *    GET  /world/recipes          配方列表
 *    POST /world/craft            炼制 ?recipeId=
 *    POST /world/duel/start       发起对决
 *    POST /world/duel/answer      应答 ?duelId=&choiceIdx=
 *    GET  /world/duel/records      对决记录
 *    GET  /world/inbox            收件箱
 *    POST /world/send             传书 ?toName=&content=
 *    POST /world/read             已读 ?messageId=
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

export const worldApi = {
  getBag() {
    return request('/world/bag')
  },
  getRealms() {
    return request('/world/realms')
  },
  exploreStart(realmId) {
    return request('/world/explore/start', { method: 'POST', query: { realmId } })
  },
  exploreChoose(choiceIdx) {
    return request('/world/explore/choose', { method: 'POST', query: { choiceIdx } })
  },
  getExploreLogs() {
    return request('/world/explore/logs')
  },
  /** type: danyao 炼丹 / lianqi 炼器 / zhenfa 阵法；不传取全部 */
  getRecipes(type) {
    return request('/world/recipes', { query: { type } })
  },
  craft(recipeId) {
    return request('/world/craft', { method: 'POST', query: { recipeId } })
  },
  duelStart() {
    return request('/world/duel/start', { method: 'POST' })
  },
  duelAnswer(duelId, choiceIdx) {
    return request('/world/duel/answer', { method: 'POST', query: { duelId, choiceIdx } })
  },
  getDuelRecords() {
    return request('/world/duel/records')
  },
  getInbox() {
    return request('/world/inbox')
  },
  send(toName, content) {
    return request('/world/send', { method: 'POST', query: { toName, content } })
  },
  read(messageId) {
    return request('/world/read', { method: 'POST', query: { messageId } })
  },
  /** 一键全部已读 */
  readAll() {
    return request('/world/readAll', { method: 'POST' })
  }
}

export default worldApi
