/**
 * ============================================================
 *  执事（后台管理）接口层 —— 预留后端，前端可直接对接
 * ============================================================
 *  约定（与 auth.js / codex.js 一致）：
 *    1. 所有接口均返回 Promise，成功 resolve 业务数据，失败 reject(new Error(中文文案))
 *    2. 统一响应体：code=0 视为成功，否则视为失败
 *    3. 未接后端时默认走 mock（本地 localStorage 会话 + 宝库本地数据源），前端流程完整可跑
 *
 *  切换真实后端：
 *    .env 中设置
 *      VITE_API_BASE_URL = https://your-api.com/api
 *      VITE_USE_MOCK     = false
 *
 *  后端需提供的接口清单（路径可自行调整，改这里即可）：
 *    POST   /admin/login                  执事登录，参数 用户名+暗语，返回 令牌+名号
 *    POST   /admin/logout                 退出执事（Bearer）
 *    GET    /admin/treasury/:kind         宝库列表，参数 element/grade/q，返回 总数+条目
 *    POST   /admin/treasury/:kind         新增条目
 *    PUT    /admin/treasury/:kind/:id     修改条目
 *    DELETE /admin/treasury/:kind/:id     删除条目
 *    POST   /admin/treasury/:kind/reset   重置该集合回种子数据
 *  其中 kind 取值：items（条目） / roots（灵根） / realms（秘境）
 *
 *  山友管理（用户系统）接口清单（真实后端）：
 *    GET    /admin/users                      列表，参数 q/role/status/includeDeleted/page/size
 *    GET    /admin/users/:id                  详情
 *    POST   /admin/users/:id/ban              封禁
 *    POST   /admin/users/:id/unban            解封
 *    POST   /admin/users/:id/reset-password   重置暗语，参数 password 可省（默认 123456）
 *    POST   /admin/users/:id/adjust           调整资源，参数 spirit/exp/level 增减量
 *    POST   /admin/users/:id/role             升降执事，参数 role（0=执事 1=山友）
 *    DELETE /admin/users/:id                  注销山籍（软注销）
 *    POST   /admin/users/:id/restore          恢复山籍
 * ============================================================
 */
import { BASE_URL, USE_MOCK, tokenStore } from './auth'
import * as treasury from '../utils/treasuryStore'
import { userStore } from '../utils/userStore'

const TIMEOUT = Number(import.meta.env.VITE_API_TIMEOUT || 12000)

/* ------------------------- 执事会话存取 ------------------------- */
const ADMIN_KEY = 'wushan_admin_session'
const adminStore = {
  get() {
    if (typeof localStorage === 'undefined') return null
    try {
      const raw = localStorage.getItem(ADMIN_KEY)
      return raw ? JSON.parse(raw) : null
    } catch (e) {
      return null
    }
  },
  set(s) {
    if (typeof localStorage === 'undefined') return
    try {
      localStorage.setItem(ADMIN_KEY, JSON.stringify({ name: s.name || '', token: s.token || '', loginAt: Date.now() }))
    } catch (e) {
      /* 忽略：隐私模式 */
    }
  },
  clear() {
    if (typeof localStorage !== 'undefined') localStorage.removeItem(ADMIN_KEY)
  }
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

function filterList(list, { element, grade, q } = {}) {
  const kw = q ? String(q).trim() : ''
  return list.filter(
    (it) =>
      (!element || element === 'all' || it.element === element) &&
      (!grade || grade === 'all' || it.grade === grade) &&
      (!kw ||
        String(it.title || '').includes(kw) ||
        String(it.name || '').includes(kw) ||
        String(it.desc || '').includes(kw))
  )
}

/* ------------------------------ 对外接口 ------------------------------ */
export const adminApi = {
  /** 是否已持执事令牌（mock / 真实模式通用） */
  isLogin() {
    if (USE_MOCK) {
      const s = adminStore.get()
      return !!(s && s.name && s.token)
    }
    return !!(tokenStore.get() && adminStore.get() && adminStore.get().name)
  },

  /** 当前执事名号；未登录返回空串 */
  currentName() {
    const s = adminStore.get()
    return (s && s.name) || ''
  },

  /**
   * 执事登录
   * mock 约定：名号须为 admin，暗语非空即入（仅为前端演示，无任何真实鉴权）
   * 真实模式：后端校验并返回 { token, name }
   */
  async login({ username, password }) {
    const name = String(username || '').trim()
    if (!name || !String(password || '').trim()) {
      return Promise.reject(new Error('执事名号与暗语，皆不可空。'))
    }
    if (USE_MOCK) {
      await sleep(650)
      if (name.toLowerCase() !== 'admin') {
        return Promise.reject(new Error('此处只通执事，山友请回前山。'))
      }
      const s = { name, token: 'mock-admin-' + Date.now() }
      adminStore.set(s)
      tokenStore.set(s.token)
      return { ...s }
    }
    const data = await request('/admin/login', { method: 'POST', data: { username: name, password } })
    const s = { name: data.name || data.username || name, token: data.token || '' }
    adminStore.set(s)
    if (s.token) tokenStore.set(s.token)
    return s
  },

  /** 退出执事：清本地会话与令牌（真实模式会通知后端作废 token） */
  async logout() {
    if (!USE_MOCK && tokenStore.get()) {
      try {
        await request('/admin/logout', { method: 'POST' })
      } catch (e) {
        /* 后端不可达也照常清理本地会话 */
      }
    } else {
      await sleep(240)
    }
    adminStore.clear()
    tokenStore.clear()
    return { ok: true }
  },

  /**
   * 宝库列表（后台管理用，含筛选）
   * @param {string} kind items | roots | realms
   * @param {object} filters element / grade / q
   */
  async list(kind, filters = {}) {
    if (USE_MOCK) {
      await sleep(220)
      const items = filterList(treasury.getList(kind), filters)
      return { total: items.length, items }
    }
    return request(`/admin/treasury/${kind}`, { query: filters })
  },

  /** 新增条目；返回写入后的完整对象 */
  async create(kind, data) {
    if (USE_MOCK) {
      await sleep(320)
      return treasury.addItem(kind, data)
    }
    return request(`/admin/treasury/${kind}`, { method: 'POST', data })
  },

  /** 修改条目；返回更新后的对象 */
  async update(kind, id, data) {
    if (USE_MOCK) {
      await sleep(320)
      const it = treasury.updateItem(kind, id, data)
      if (!it) return Promise.reject(new Error('条目已不在库中，可能已被移除。'))
      return it
    }
    return request(`/admin/treasury/${kind}/${id}`, { method: 'PUT', data })
  },

  /** 删除条目 */
  async remove(kind, id) {
    if (USE_MOCK) {
      await sleep(320)
      if (!treasury.removeItem(kind, id)) return Promise.reject(new Error('条目已不在库中。'))
      return { ok: true }
    }
    return request(`/admin/treasury/${kind}/${id}`, { method: 'DELETE' })
  },

  /** 重置某集合回种子数据（谨慎使用） */
  async reset(kind) {
    if (USE_MOCK) {
      await sleep(420)
      const items = treasury.resetKind(kind)
      return { total: items.length, items }
    }
    return request(`/admin/treasury/${kind}/reset`, { method: 'POST' })
  },

  /* ------------------------------ 山友管理（用户系统） ------------------------------ */

  /**
   * 山友列表
   * @param {object} filters q / role / status / includeDeleted / page / size
   */
  async listUsers(filters = {}) {
    if (USE_MOCK) {
      await sleep(220)
      return userStore.list(filters)
    }
    return request('/admin/users', { query: filters })
  },

  /** 山友详情 */
  async userDetail(id) {
    if (USE_MOCK) {
      await sleep(220)
      return userStore.detail(id)
    }
    return request(`/admin/users/${id}`)
  },

  /** 封禁 */
  async banUser(id) {
    if (USE_MOCK) {
      await sleep(260)
      return userStore.ban(id)
    }
    return request(`/admin/users/${id}/ban`, { method: 'POST' })
  },

  /** 解封 */
  async unbanUser(id) {
    if (USE_MOCK) {
      await sleep(260)
      return userStore.unban(id)
    }
    return request(`/admin/users/${id}/unban`, { method: 'POST' })
  },

  /** 重置暗语；password 缺省 123456 */
  async resetPassword(id, password) {
    if (USE_MOCK) {
      await sleep(260)
      return userStore.resetPassword(id, password)
    }
    return request(`/admin/users/${id}/reset-password`, { method: 'POST', data: { password } })
  },

  /** 调整资源：spirit / exp / level 增减量 */
  async adjustResource(id, payload) {
    if (USE_MOCK) {
      await sleep(260)
      return userStore.adjust(id, payload)
    }
    return request(`/admin/users/${id}/adjust`, { method: 'POST', data: payload })
  },

  /** 升降执事：role 0=执事 1=山友 */
  async setRole(id, role) {
    if (USE_MOCK) {
      await sleep(260)
      return userStore.setRole(id, role)
    }
    return request(`/admin/users/${id}/role`, { method: 'POST', data: { role } })
  },

  /** 注销山籍（软注销） */
  async deleteUser(id) {
    if (USE_MOCK) {
      await sleep(260)
      return userStore.remove(id)
    }
    return request(`/admin/users/${id}`, { method: 'DELETE' })
  },

  /** 恢复山籍 */
  async restoreUser(id) {
    if (USE_MOCK) {
      await sleep(260)
      return userStore.restore(id)
    }
    return request(`/admin/users/${id}/restore`, { method: 'POST' })
  }
}

export default adminApi
