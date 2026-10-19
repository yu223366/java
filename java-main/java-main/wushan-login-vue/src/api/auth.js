/**
 * ============================================================
 *  认证接口层 —— 预留后端，前端可直接对接
 * ============================================================
 *  约定：
 *    1. 所有接口均返回 Promise，成功 resolve 业务数据，失败 reject(new Error(中文文案))
 *    2. 统一响应体：{ code: 0, message: 'ok', data: {...} }，code !== 0 视为失败
 *    3. 未接后端时默认走 mock（本地假数据），前端流程完整可跑
 *
 *  切换真实后端：
 *    .env 中设置
 *      VITE_API_BASE_URL = https://your-api.com/api
 *      VITE_USE_MOCK     = false
 *    Vite 会自动代理 / 直连（见 vite.config.js 的 server.proxy）
 *
 *  后端需提供的接口清单（路径可自行调整，改这里即可）：
 *    POST /auth/captcha              生成图形验证码 { captchaId, image(base64 或 url) }
 *    POST /auth/captcha/verify       校验图形验证码 { captchaId, captchaCode }
 *    GET  /auth/username/available   名号可用性 ?username=xxx
 *    POST /auth/register             注册 { username, password, element, captchaId, captchaCode }
 *    POST /auth/login                登录 { username, password }
 * ============================================================
 */

export const BASE_URL = (import.meta.env.VITE_API_BASE_URL || '/api').replace(/\/+$/, '')
export const USE_MOCK = import.meta.env.VITE_USE_MOCK !== 'false'
const TIMEOUT = Number(import.meta.env.VITE_API_TIMEOUT || 12000)

/* ------------------------- token 存取（预留） ------------------------- */
const TOKEN_KEY = 'wushan_token'
export const tokenStore = {
  get: () => (typeof localStorage === 'undefined' ? '' : localStorage.getItem(TOKEN_KEY) || ''),
  set: (t) => typeof localStorage !== 'undefined' && localStorage.setItem(TOKEN_KEY, t),
  clear: () => typeof localStorage !== 'undefined' && localStorage.removeItem(TOKEN_KEY)
}

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
const MEM_NAMES = ['wushan', 'admin', 'huoyu', '后山', 'test01'] // 模拟已被占用的名号

const mock = {
  async checkUsername(username) {
    await sleep(260)
    return { available: !MEM_NAMES.includes(String(username || '').trim().toLowerCase()) }
  },
  async register(payload) {
    await sleep(760)
    return { userId: 'u_' + Math.random().toString(36).slice(2, 10), ...payload, token: 'mock-token-' + Date.now() }
  },
  async login({ username }) {
    await sleep(700)
    return { token: 'mock-token-' + Date.now(), username }
  }
}

/* ------------------------------ 对外接口 ------------------------------ */
export const authApi = {
  /** 生成图形验证码。后端模式下返回 { captchaId, image }；mock 由前端本地绘制 */
  getCaptcha() {
    if (USE_MOCK) {
      return Promise.resolve({ captchaId: 'local-' + Date.now().toString(36), image: '', local: true })
    }
    return request('/auth/captcha', { method: 'POST' })
  },

  /** 校验图形验证码（后端模式下必调；mock 下仅占位） */
  verifyCaptcha({ captchaId, captchaCode }) {
    if (USE_MOCK) return Promise.resolve({ ok: true })
    return request('/auth/captcha/verify', { method: 'POST', data: { captchaId, captchaCode } })
  },

  /** 名号是否可用 */
  checkUsername(username) {
    if (USE_MOCK) return mock.checkUsername(username)
    return request('/auth/username/available', { query: { username } })
  },

  /** 注册（提交前端上已做完整校验，此处为二次校验） */
  register(payload) {
    if (USE_MOCK) return mock.register(payload)
    return request('/auth/register', { method: 'POST', data: payload })
  },

  /** 登录（登录页预留，替换 LoginPanel 中的假延时即可） */
  login(payload) {
    if (USE_MOCK) return mock.login(payload)
    return request('/auth/login', { method: 'POST', data: payload })
  }
}

export default authApi
