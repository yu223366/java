/**
 * ============================================================
 *  山门玩法接口层 —— 测灵根 / 每日签到 / 每日一题 / 灵石钱包
 * ============================================================
 *  约定与 auth.js 一致：统一响应体 { code: 0, message, data }，
 *  失败 reject(new Error(中文文案))。
 *
 *  后端接口（GameController）：
 *    GET  /game/spirit/quiz     测灵根题目
 *    POST /game/spirit/answer   提交测评 { answers: [0,2,...] }
 *    GET  /game/spirit/me       我的灵根
 *    GET  /game/signin/status   签到状态
 *    POST /game/signin/checkin  每日签到
 *    GET  /game/daily/question  今日一题
 *    POST /game/daily/answer    提交答案 { answerIdx }
 *    GET  /game/wallet          灵石余额与流水
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

export const gameApi = {
  getSpiritQuiz() {
    return request('/game/spirit/quiz')
  },
  submitSpirit(answers) {
    return request('/game/spirit/answer', { method: 'POST', data: { answers } })
  },
  getMySpirit() {
    return request('/game/spirit/me')
  },
  getSignInStatus() {
    return request('/game/signin/status')
  },
  checkIn() {
    return request('/game/signin/checkin', { method: 'POST' })
  },
  getDailyQuestion() {
    return request('/game/daily/question')
  },
  answerDaily(answerIdx) {
    return request('/game/daily/answer', { method: 'POST', data: { answerIdx } })
  },
  getWallet() {
    return request('/game/wallet')
  },
  /** 练功状态（重数 / 修为 / 30 秒冷却） */
  getPracticeStatus() {
    return request('/game/practice/status')
  },
  /** 练功一次 */
  practiceRun() {
    return request('/game/practice/run', { method: 'POST' })
  }
}

export default gameApi
