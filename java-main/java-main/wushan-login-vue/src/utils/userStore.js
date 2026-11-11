/**
 * ============================================================
 *  山友本地数据源 —— mock 模式下可持久化的「伪后台用户系统」
 * ============================================================
 *  api/admin.js 在 USE_MOCK 时改从本模块取数，覆盖：
 *    列表 / 详情 / 封禁 / 解封 / 重置暗语 / 调整资源 / 升降执事 / 注销 / 恢复
 *  接真实后端后本模块仅 mock 使用，无需替换调用方。
 * ============================================================
 */
const STORE_KEY = 'wushan_admin_users_v1'

/* 内存兜底（隐私模式下本次会话内仍可用） */
let memStore = null

function seed() {
  return [
    {
      id: 1, username: 'admin', element: 'huo', elementLabel: '火',
      role: 0, status: 1, deleted: 0, spirit: 9999,
      practiceLevel: 9, exp: 80, sectName: '', createTime: '2026-01-01T08:00:00'
    },
    {
      id: 2, username: '雾山甲', element: 'huo', elementLabel: '火',
      role: 1, status: 1, deleted: 0, spirit: 320,
      practiceLevel: 3, exp: 40, sectName: '五行宗', createTime: '2026-02-02T09:00:00'
    },
    {
      id: 3, username: '雾山乙', element: 'shui', elementLabel: '水',
      role: 1, status: 1, deleted: 0, spirit: 150,
      practiceLevel: 2, exp: 10, sectName: '玄冰阁', createTime: '2026-02-03T10:00:00'
    },
    {
      id: 4, username: '散修阿三', element: 'mu', elementLabel: '木',
      role: 1, status: 0, deleted: 0, spirit: 20,
      practiceLevel: 1, exp: 0, sectName: '', createTime: '2026-03-04T11:00:00'
    },
    {
      id: 5, username: '已退隐者', element: 'tu', elementLabel: '土',
      role: 1, status: 0, deleted: 1, spirit: 0,
      practiceLevel: 1, exp: 0, sectName: '', createTime: '2026-01-10T12:00:00'
    }
  ]
}

function load() {
  if (memStore) return memStore
  if (typeof localStorage !== 'undefined') {
    try {
      const raw = localStorage.getItem(STORE_KEY)
      if (raw) {
        const data = JSON.parse(raw)
        if (Array.isArray(data)) {
          memStore = data
          return memStore
        }
      }
    } catch (e) {
      /* 脏数据走回播种 */
    }
  }
  memStore = seed()
  persist()
  return memStore
}

function persist() {
  if (typeof localStorage !== 'undefined' && memStore) {
    try {
      localStorage.setItem(STORE_KEY, JSON.stringify(memStore))
    } catch (e) {
      /* 隐私模式忽略 */
    }
  }
}

function roleLabel(role) {
  return role === 0 ? '执事' : '山友'
}
function statusLabelOf(u) {
  if (u.deleted === 1) return '已注销'
  return u.status === 0 ? '封禁' : '正常'
}
function listVo(u) {
  return {
    id: u.id,
    username: u.username,
    element: u.element,
    elementLabel: u.elementLabel,
    role: u.role,
    roleLabel: roleLabel(u.role),
    status: u.status,
    statusLabel: statusLabelOf(u),
    deleted: u.deleted || 0,
    spirit: u.spirit || 0,
    practiceLevel: u.practiceLevel || 1,
    exp: u.exp || 0,
    sectName: u.sectName || '',
    createTime: u.createTime || ''
  }
}
function detailVo(u) {
  const m = listVo(u)
  m.practice = {
    level: u.practiceLevel || 1,
    exp: u.exp || 0,
    totalCount: u.practiceTotalCount || 0,
    totalSpirit: u.practiceTotalSpirit || 0,
    lastTime: u.practiceLastTime || ''
  }
  const inSect = u.sectName && u.sectName.length > 0
  m.sect = {
    sectId: inSect ? (u.sectId || 1) : null,
    sectName: inSect ? u.sectName : '',
    role: u.sectRole != null ? u.sectRole : 0,
    roleLabel: inSect
      ? (u.sectRole === 2 ? '宗主' : u.sectRole === 1 ? '长老' : '弟子')
      : '散修',
    contribution: u.contribution || 0
  }
  m.stats = {
    consignActive: u.consignActive || 0,
    consignTotal: u.consignTotal || 0,
    practiceTotalCount: u.practiceTotalCount || 0
  }
  return m
}
function nextId() {
  const data = load()
  return data.reduce((mx, u) => Math.max(mx, u.id || 0), 0) + 1
}

/* ------------------------------ 对外（mock）接口 ------------------------------ */
export const userStore = {
  list({ q, role, status, includeDeleted, page, size } = {}) {
    let arr = load().slice()
    const kw = q ? String(q).trim() : ''
    if (kw) arr = arr.filter((u) => u.username.includes(kw))
    if (role === '0' || role === 'admin') arr = arr.filter((u) => u.role === 0)
    else if (role === '1' || role === 'user') arr = arr.filter((u) => u.role === 1)
    if (!includeDeleted) arr = arr.filter((u) => (u.deleted || 0) === 0)
    if (status === 'banned') arr = arr.filter((u) => u.status === 0)
    else if (status === 'normal') arr = arr.filter((u) => u.status === 1 && (u.deleted || 0) === 0)
    else if (status === 'deleted') arr = arr.filter((u) => (u.deleted || 0) === 1)
    arr.sort((a, b) => b.id - a.id)
    const total = arr.length
    const pg = Math.max(1, page || 1)
    const sz = Math.min(100, Math.max(1, size || 12))
    const items = arr.slice((pg - 1) * sz, pg * sz).map(listVo)
    return { total, page: pg, size: sz, items }
  },

  detail(id) {
    const u = load().find((x) => x.id === id)
    if (!u) throw new Error('查无此山籍。')
    return detailVo(u)
  },

  ban(id) {
    const u = load().find((x) => x.id === id)
    if (!u) throw new Error('查无此山籍。')
    u.status = 0
    persist()
    return { ok: true, id, status: 0, statusLabel: '封禁' }
  },

  unban(id) {
    const u = load().find((x) => x.id === id)
    if (!u) throw new Error('查无此山籍。')
    if (u.deleted === 1) throw new Error('该山籍已注销，请先恢复再解封。')
    u.status = 1
    persist()
    return { ok: true, id, status: 1, statusLabel: '正常' }
  },

  resetPassword(id, password) {
    const u = load().find((x) => x.id === id)
    if (!u) throw new Error('查无此山籍。')
    const np = password && password.length ? password : '123456'
    u.password = np
    persist()
    return { ok: true, id, password: np }
  },

  adjust(id, { spirit, exp, level } = {}) {
    const u = load().find((x) => x.id === id)
    if (!u) throw new Error('查无此山籍。')
    if (spirit != null) u.spirit = Math.max(0, (u.spirit || 0) + Number(spirit))
    if (exp != null) u.exp = Math.max(0, (u.exp || 0) + Number(exp))
    if (level != null) u.practiceLevel = Math.max(1, (u.practiceLevel || 1) + Number(level))
    persist()
    return {
      ok: true, id,
      spirit: u.spirit,
      practiceLevel: u.practiceLevel,
      exp: u.exp
    }
  },

  setRole(id, role) {
    const u = load().find((x) => x.id === id)
    if (!u) throw new Error('查无此山籍。')
    if (role === 0 && u.role === 0 && !load().some((x) => x.role === 0 && x.id !== id)) {
      throw new Error('不可罢免最后一位执事。')
    }
    u.role = role
    persist()
    return { ok: true, id, role, roleLabel: roleLabel(role) }
  },

  remove(id) {
    const u = load().find((x) => x.id === id)
    if (!u) throw new Error('查无此山籍。')
    if (u.role === 0) throw new Error('执事不可注销，请先降为山友。')
    u.deleted = 1
    u.status = 0
    persist()
    return { ok: true, id, deleted: 1, status: 0 }
  },

  restore(id) {
    const u = load().find((x) => x.id === id)
    if (!u) throw new Error('查无此山籍。')
    if (u.deleted !== 1) throw new Error('该山籍并未注销。')
    u.deleted = 0
    u.status = 1
    persist()
    return { ok: true, id, deleted: 0, status: 1 }
  },

  _nextId: nextId
}

export default userStore
