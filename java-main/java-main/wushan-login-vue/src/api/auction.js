/**
 * ============================================================
 *  拍卖行接口层 —— 对接 Spring Boot /api/auction/*
 * ============================================================
 *  约定与 auth.js / codex.js 一致：统一响应体 { code: 0, message, data }，
 *  失败 reject(new Error(中文文案))。
 *
 *  后端接口（AuctionController）：
 *    GET  /auction/status   当前场次全量状态；每次调用同时刷新在线心跳
 *    POST /auction/bid      出价 { increment: 10|50|100|500 }，需 Bearer token
 *    GET  /auction/item?id= 拍品属性
 *    GET  /auction/logs     出价流水
 *
 *  注意：拍卖必须由服务端统一驱动（倒计时、落槌、在线判定），
 *  因此本层不做本地 mock，一律走真实后端。
 * ============================================================
 */
import { BASE_URL, tokenStore } from './auth'

const TIMEOUT = Number(import.meta.env.VITE_API_TIMEOUT || 12000)

/** 前端兜底规则（后端 status.rules 优先） */
export const AUCTION_RULES = {
  batchSize: 5,
  startPrice: 100,
  increments: [10, 50, 100, 500],
  countdownSeconds: 5,
  itemGapSeconds: 3,
  roundIntervalSeconds: 600,
  notifyBeforeSeconds: 60
}

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

export const auctionApi = {
  /** 当前场次状态（前端每 2 秒轮询一次，服务端借此判定在线） */
  getStatus() {
    return request('/auction/status')
  },

  /** 出价：身份由令牌决定，前端只传加价档 */
  bid(increment) {
    return request('/auction/bid', { method: 'POST', data: { increment } })
  },

  /** 拍品属性 */
  getItem(id) {
    return request('/auction/item', { query: { id } })
  },

  /** 出价流水 */
  getLogs(roundNo) {
    return request('/auction/logs', { query: { roundNo } })
  },

  /** 往场成交记录（回放） */
  history(limit) {
    return request('/auction/history', { query: { limit } })
  },

  /** 关注拍品，捡漏提醒 */
  watch(itemTitle) {
    return request('/auction/watch', { method: 'POST', query: { itemTitle } })
  },

  /** 我关注的拍品 */
  getWatches() {
    return request('/auction/watches')
  },

  /** 我的竞拍记录（拍到的 + 出价过的） */
  getMyBids() {
    return request('/auction/my')
  }
}

export default auctionApi
