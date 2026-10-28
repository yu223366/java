/**
 * ============================================================
 *  宝库本地数据源 —— mock 模式下可持久化的「伪后端」
 * ============================================================
 *  背景：
 *    api/codex.js 与 api/home.js 在 USE_MOCK 时改从本模块取数，
 *    后台管理系统（AdminPanel）的增删改会立刻反映到前台宝库与首页搜索。
 *
 *  约定：
 *    1. 数据以 localStorage 持久化（key = STORE_KEY），首次访问以
 *       constants/codex.js 中的种子数据（CODEX_ITEMS / SPIRIT_ROOTS / REALMS）初始化
 *    2. 三种集合：items（条目：功法/心得/资源，字段 title）、
 *       roots（灵根，字段 name）、realms（秘境，字段 name + layers）
 *    3. 所有读取返回浅拷贝数组，调用方改动不影响库内数据；
 *       写操作后内部立即落盘，并触发同窗口的重新读取
 *    4. 隐私模式 / 无 localStorage 环境下静默降级为内存存储（本次会话有效）
 *
 *  接真实后端后：本模块仅 mock 使用，无需替换调用方。
 * ============================================================
 */
import { CODEX_ITEMS, SPIRIT_ROOTS, REALMS } from '../constants/codex'

const STORE_KEY = 'wushan_treasury_v1'

/* 三种集合的种子数据 */
const SEEDS = {
  items: CODEX_ITEMS,
  roots: SPIRIT_ROOTS,
  realms: REALMS
}

export const KINDS = ['items', 'roots', 'realms']

/* 内存兜底（隐私模式下本次会话内仍可用） */
let memStore = null

function seed() {
  const base = {}
  KINDS.forEach((k) => {
    base[k] = SEEDS[k].map((it) => ({ ...it }))
  })
  return base
}

function load() {
  if (memStore) return memStore
  if (typeof localStorage !== 'undefined') {
    try {
      const raw = localStorage.getItem(STORE_KEY)
      if (raw) {
        const data = JSON.parse(raw)
        // 结构校验：三种集合都必须是数组，否则视为脏数据回播种
        if (KINDS.every((k) => Array.isArray(data[k]))) {
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
  if (typeof localStorage === 'undefined' || !memStore) return
  try {
    localStorage.setItem(STORE_KEY, JSON.stringify(memStore))
  } catch (e) {
    /* 忽略：隐私模式 / 存储已满 */
  }
}

function assertKind(kind) {
  if (KINDS.indexOf(kind) === -1) throw new Error(`未知宝库集合：${kind}`)
}

function genId(kind, item) {
  const head = String(kind).slice(0, 4)
  const el = item && item.element ? String(item.element) : 'wu'
  return `${head}-${el}-${Date.now().toString(36)}${Math.random().toString(36).slice(2, 5)}`
}

/* ------------------------------ 读取 ------------------------------ */

/** 取某集合全部数据（浅拷贝数组，元素为浅拷贝对象） */
export function getList(kind) {
  assertKind(kind)
  return load()[kind].map((it) => ({ ...it }))
}

/** 按 id 取单条；不存在返回 null */
export function getItem(kind, id) {
  assertKind(kind)
  const hit = load()[kind].find((it) => String(it.id) === String(id))
  return hit ? { ...hit } : null
}

/* ------------------------------ 写入 ------------------------------ */

/** 新增一条，自动补 id；返回写入后的完整对象 */
export function addItem(kind, data) {
  assertKind(kind)
  const item = { ...data, id: (data && data.id) || genId(kind, data) }
  load()[kind].push(item)
  persist()
  return { ...item }
}

/** 按 id 合并更新（浅合并）；返回更新后的对象，不存在返回 null */
export function updateItem(kind, id, patch) {
  assertKind(kind)
  const list = load()[kind]
  const idx = list.findIndex((it) => String(it.id) === String(id))
  if (idx === -1) return null
  list[idx] = { ...list[idx], ...patch, id: list[idx].id } // id 不可被覆盖
  persist()
  return { ...list[idx] }
}

/** 按 id 删除；返回是否删除成功 */
export function removeItem(kind, id) {
  assertKind(kind)
  const list = load()[kind]
  const idx = list.findIndex((it) => String(it.id) === String(id))
  if (idx === -1) return false
  list.splice(idx, 1)
  persist()
  return true
}

/** 某集合重置回种子数据；返回重置后的列表 */
export function resetKind(kind) {
  assertKind(kind)
  load()[kind] = SEEDS[kind].map((it) => ({ ...it }))
  persist()
  return getList(kind)
}

/* ------------------------------ 展示辅助 ------------------------------ */

/** 条目显示名：条目用 title，灵根 / 秘境用 name */
export function titleOf(item) {
  return (item && (item.title || item.name)) || ''
}
