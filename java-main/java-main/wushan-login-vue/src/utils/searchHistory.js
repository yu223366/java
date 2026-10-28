/**
 * ============================================================
 *  搜索历史工具 —— 按名号分桶存于 localStorage
 * ============================================================
 *  约定：
 *    1. 最多保留 MAX 条，最新在前，重复关键词去重后置顶
 *    2. 所有函数均返回操作后的最新历史数组（供直接赋值给 ref）
 *    3. 隐私模式 / 无 localStorage 环境下静默降级为内存数组
 * ============================================================
 */

const STORE_KEY = 'wushan_search_history'
const MAX = 10

/* 内存兜底（隐私模式下本次会话内仍可用） */
let memFallback = {}

function readAll() {
  if (typeof localStorage === 'undefined') return { ...memFallback }
  try {
    const raw = localStorage.getItem(STORE_KEY)
    return raw ? JSON.parse(raw) : {}
  } catch (e) {
    return { ...memFallback }
  }
}

function writeAll(data) {
  memFallback = { ...data }
  if (typeof localStorage === 'undefined') return
  try {
    localStorage.setItem(STORE_KEY, JSON.stringify(data))
  } catch (e) {
    /* 忽略：隐私模式 */
  }
}

function bucket(username) {
  return String(username || '').trim() || '__anon__'
}

/** 读取某名号的搜索历史（最新在前，最多 MAX 条） */
export function getHistory(username = '') {
  const all = readAll()
  const list = all[bucket(username)]
  return Array.isArray(list) ? list.slice(0, MAX) : []
}

/** 追加一条历史：去重置顶，超出 MAX 截断；返回最新历史 */
export function addHistory(keyword, username = '') {
  const kw = String(keyword || '').trim()
  if (!kw) return getHistory(username)
  const all = readAll()
  const b = bucket(username)
  const old = Array.isArray(all[b]) ? all[b] : []
  all[b] = [kw, ...old.filter((k) => k !== kw)].slice(0, MAX)
  writeAll(all)
  return all[b].slice()
}

/** 移除某一条；返回最新历史 */
export function removeHistory(keyword, username = '') {
  const all = readAll()
  const b = bucket(username)
  const old = Array.isArray(all[b]) ? all[b] : []
  all[b] = old.filter((k) => k !== keyword)
  writeAll(all)
  return all[b].slice()
}

/** 清空某名号的全部历史；返回空数组 */
export function clearHistory(username = '') {
  const all = readAll()
  all[bucket(username)] = []
  writeAll(all)
  return []
}
