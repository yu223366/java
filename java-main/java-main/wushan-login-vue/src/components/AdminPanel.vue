<script setup>
// 后台管理面板：执事的宝库数据管理台
// 条目 / 灵根 / 秘境 三库切换，支持检索、新增、修改、删除、重置；
// mock 模式下改动写入 localStorage（treasuryStore），前台宝库与首页搜索即时可见。
import { ref, reactive, computed, onMounted } from 'vue'
import { adminApi } from '../api/admin'
import { ELEMENTS, findElement } from '../constants/wuxing'
import { GRADES, CATEGORIES, findGrade } from '../constants/codex'

const props = defineProps({
  username: { type: String, default: '' }
})
const emit = defineEmits(['logout', 'to-home'])

/* ------------------------------ 字典选项 ------------------------------ */
const elOptions = ELEMENTS.map((e) => ({ value: e.key, label: e.label }))
const gradeOptions = GRADES.map((g) => ({ value: g.key, label: g.label }))
const catOptions = CATEGORIES.map((c) => ({ value: c.key, label: c.label }))

/* ------------------------------ 三库元配置 ------------------------------ */
const KIND_META = {
  items: {
    label: '条目',
    nameKey: 'title',
    nameLabel: '名号',
    namePh: '如：焚天七绝焰',
    fields: [
      { key: 'title', label: '名号', type: 'text', required: true, ph: '如：焚天七绝焰' },
      { key: 'category', label: '分类', type: 'select', options: catOptions },
      { key: 'element', label: '五行', type: 'select', options: elOptions },
      { key: 'grade', label: '品阶', type: 'select', options: gradeOptions },
      { key: 'desc', label: '简介', type: 'textarea', ph: '一行简述…' }
    ]
  },
  roots: {
    label: '灵根',
    nameKey: 'name',
    nameLabel: '名称',
    namePh: '如：庚金灵根',
    fields: [
      { key: 'name', label: '名称', type: 'text', required: true, ph: '如：庚金灵根' },
      { key: 'element', label: '五行', type: 'select', options: elOptions },
      { key: 'grade', label: '品阶', type: 'select', options: gradeOptions },
      { key: 'desc', label: '简介', type: 'textarea', ph: '一行简述…' }
    ]
  },
  realms: {
    label: '秘境',
    nameKey: 'name',
    nameLabel: '名称',
    namePh: '如：焚火秘境',
    fields: [
      { key: 'name', label: '名称', type: 'text', required: true, ph: '如：焚火秘境' },
      { key: 'layers', label: '层数', type: 'text', ph: '如：九层' },
      { key: 'element', label: '五行', type: 'select', options: elOptions },
      { key: 'grade', label: '品阶', type: 'select', options: gradeOptions },
      { key: 'desc', label: '简介', type: 'textarea', ph: '一行简述…' }
    ]
  }
}
const KINDS = Object.keys(KIND_META) // items | roots | realms

/* 表格列定义：type = text | element | grade | category */
const COLUMNS = [
  { key: '_name', label: '名号', type: 'name' },
  { key: 'category', label: '分类', type: 'category' },
  { key: 'layers', label: '层数', type: 'text' },
  { key: 'element', label: '五行', type: 'element' },
  { key: 'grade', label: '品阶', type: 'grade' },
  { key: 'desc', label: '简介', type: 'text' }
]

/* ------------------------------ 状态 ------------------------------ */
const kind = ref('items')
const list = ref([])
const total = ref(0)
const loading = ref(false)
const err = ref('')
const notice = ref('') // 操作成功提示
const filterElement = ref('all')
const filterQ = ref('')

/* 编辑弹窗：null=关闭，'new'=新增，否则为被编辑对象的 id */
const editing = ref(null)
const form = reactive({})
const saving = ref(false)
const formErr = ref('')

/* 删除确认 */
const removingId = ref('')

let noticeTimer = null
function toast(msg) {
  notice.value = msg
  clearTimeout(noticeTimer)
  noticeTimer = setTimeout(() => (notice.value = ''), 2200)
}

/* ------------------------------ 数据读写 ------------------------------ */
async function loadList() {
  loading.value = true
  err.value = ''
  try {
    const r = await adminApi.list(kind.value, {
      element: filterElement.value,
      q: filterQ.value.trim()
    })
    list.value = r.items || []
    total.value = r.total != null ? r.total : list.value.length
  } catch (e) {
    err.value = e instanceof Error ? e.message : '库房未应，请稍后重试。'
    list.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function switchKind(k) {
  if (kind.value === k || loading.value) return
  kind.value = k
  filterElement.value = 'all'
  filterQ.value = ''
  removingId.value = ''
  loadList()
}

function search() {
  loadList()
}

/* ------------------------------ 新增 / 修改 ------------------------------ */
function openNew() {
  const meta = KIND_META[kind.value]
  Object.keys(form).forEach((k) => delete form[k])
  meta.fields.forEach((f) => {
    form[f.key] = f.type === 'select' ? (f.options[0] ? f.options[0].value : '') : ''
  })
  editing.value = 'new'
  formErr.value = ''
}

function openEdit(item) {
  const meta = KIND_META[kind.value]
  Object.keys(form).forEach((k) => delete form[k])
  meta.fields.forEach((f) => {
    form[f.key] = item[f.key] != null ? item[f.key] : ''
  })
  editing.value = item.id
  formErr.value = ''
}

function closeEdit() {
  editing.value = null
}

async function save() {
  const meta = KIND_META[kind.value]
  // 校验必填（名号 / 名称）
  for (const f of meta.fields) {
    if (f.required && !String(form[f.key] || '').trim()) {
      formErr.value = `${f.label}不可空。`
      return
    }
  }
  formErr.value = ''
  saving.value = true
  try {
    const payload = {}
    meta.fields.forEach((f) => {
      payload[f.key] = typeof form[f.key] === 'string' ? form[f.key].trim() : form[f.key]
    })
    if (editing.value === 'new') {
      await adminApi.create(kind.value, payload)
      toast(`新${meta.label}已入库。`)
    } else {
      await adminApi.update(kind.value, editing.value, payload)
      toast(`${meta.label}已修订。`)
    }
    editing.value = null
    await loadList()
  } catch (e) {
    formErr.value = e instanceof Error ? e.message : '库房未应，请稍后重试。'
  } finally {
    saving.value = false
  }
}

/* ------------------------------ 删除 / 重置 ------------------------------ */
async function doRemove(item) {
  const meta = KIND_META[kind.value]
  try {
    await adminApi.remove(kind.value, item.id)
    toast(`${meta.label}「${item[meta.nameKey]}」已出库。`)
    removingId.value = ''
    await loadList()
  } catch (e) {
    err.value = e instanceof Error ? e.message : '库房未应，请稍后重试。'
    removingId.value = ''
  }
}

async function doReset() {
  const meta = KIND_META[kind.value]
  try {
    const r = await adminApi.reset(kind.value)
    toast(`${meta.label}已重置回种子数据。`)
    list.value = r.items || []
    total.value = r.total != null ? r.total : list.value.length
  } catch (e) {
    err.value = e instanceof Error ? e.message : '库房未应，请稍后重试。'
  }
}

async function logout() {
  await adminApi.logout()
  emit('logout')
}

/* ------------------------------ 展示辅助 ------------------------------ */
function nameOf(item) {
  const meta = KIND_META[kind.value]
  return item[meta.nameKey] || ''
}

function catLabel(key) {
  const c = CATEGORIES.find((x) => x.key === key)
  return c ? c.label : key || '—'
}

function colVisible(col) {
  // 分类列仅条目库；层数列仅秘境库
  if (col.key === 'category') return kind.value === 'items'
  if (col.key === 'layers') return kind.value === 'realms'
  return true
}

const visibleColumns = computed(() => COLUMNS.filter(colVisible))

onMounted(loadList)
</script>

<template>
  <div class="admin-root">
    <!-- 顶栏 -->
    <header class="topbar">
      <div class="brand">
        <span class="seal">执<br />事</span>
        <div class="brand-text">
          <div class="brand-title">宝库执事</div>
          <div class="brand-sub">{{ username }} · 改动即时生效</div>
        </div>
      </div>
      <div class="topbar-actions">
        <button class="ghost-btn" @click="emit('to-home')">返回山门</button>
        <button class="ghost-btn danger" @click="logout">退出执事</button>
      </div>
    </header>

    <!-- 库切换 -->
    <nav class="tabs">
      <button
        v-for="k in KINDS"
        :key="k"
        class="tab"
        :class="{ 'is-on': kind === k }"
        @click="switchKind(k)"
      >
        {{ KIND_META[k].label }}
      </button>
      <span class="tab-side">
        <button class="ghost-btn warn" @click="doReset">重置本库</button>
        <button class="accent-btn" @click="openNew">＋ 新录{{ KIND_META[kind].label }}</button>
      </span>
    </nav>

    <!-- 筛选行 -->
    <div class="filters">
      <select v-model="filterElement" @change="search">
        <option value="all">五行不限</option>
        <option v-for="o in elOptions" :key="o.value" :value="o.value">{{ o.label }}</option>
      </select>
      <div class="qbox">
        <input
          v-model="filterQ"
          :placeholder="`检索${KIND_META[kind].label}名号或简介…`"
          @keydown.enter="search"
        />
      </div>
      <button class="ghost-btn" @click="search">检 索</button>
      <span class="total">共 {{ total }} 条</span>
    </div>

    <!-- 数据表 -->
    <main class="table-wrap">
      <div v-if="loading" class="state">开 库 中 …</div>
      <div v-else-if="err" class="state err-text">{{ err }}</div>
      <div v-else-if="!list.length" class="state">库中暂无此物。</div>
      <table v-else>
        <thead>
          <tr>
            <th v-for="col in visibleColumns" :key="col.key">{{ col.label }}</th>
            <th class="op-col">操 作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in list" :key="item.id">
            <td v-for="col in visibleColumns" :key="col.key" :class="`col-${col.type}`">
              <span v-if="col.type === 'name'" class="cell-name">{{ nameOf(item) }}</span>
              <span v-else-if="col.type === 'element'">
                <i class="dot" :style="{ background: (findElement(item.element) || {}).color || '#888' }"></i>
                {{ (findElement(item.element) || {}).label || item.element || '—' }}
              </span>
              <span v-else-if="col.type === 'grade'" :style="{ color: (findGrade(item.grade) || {}).color }">
                {{ (findGrade(item.grade) || {}).label || item.grade || '—' }}
              </span>
              <span v-else-if="col.type === 'category'">{{ catLabel(item.category) }}</span>
              <span v-else>{{ item[col.key] || '—' }}</span>
            </td>
            <td class="op-col">
              <template v-if="removingId === item.id">
                <button class="mini danger" @click="doRemove(item)">确认出库</button>
                <button class="mini" @click="removingId = ''">留</button>
              </template>
              <template v-else>
                <button class="mini" @click="openEdit(item)">修 订</button>
                <button class="mini danger" @click="removingId = item.id">出 库</button>
              </template>
            </td>
          </tr>
        </tbody>
      </table>
    </main>

    <!-- 成功提示 -->
    <Transition name="toast">
      <div v-if="notice" class="toast">{{ notice }}</div>
    </Transition>

    <!-- 新增 / 修订弹窗 -->
    <div v-if="editing" class="mask" @click.self="closeEdit">
      <div class="dialog">
        <div class="dialog-head">
          <div class="dialog-title">
            {{ editing === 'new' ? '新录' : '修订' }}{{ KIND_META[kind].label }}
          </div>
          <button class="mini" @click="closeEdit">✕</button>
        </div>

        <label v-for="f in KIND_META[kind].fields" :key="f.key" class="form-item">
          <span class="form-label">{{ f.label }}<em v-if="f.required"> *</em></span>
          <select v-if="f.type === 'select'" v-model="form[f.key]">
            <option v-for="o in f.options" :key="o.value" :value="o.value">{{ o.label }}</option>
          </select>
          <textarea v-else-if="f.type === 'textarea'" v-model="form[f.key]" rows="3" :placeholder="f.ph || ''"></textarea>
          <input v-else v-model="form[f.key]" type="text" :placeholder="f.ph || ''" />
        </label>

        <div class="dialog-err">{{ formErr }}</div>
        <div class="dialog-actions">
          <button class="ghost-btn" @click="closeEdit">作 罢</button>
          <button class="accent-btn" :disabled="saving" @click="save">
            {{ saving ? '落 印…' : '落 印' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.admin-root {
  position: relative;
  z-index: 2;
  min-height: 100vh;
  padding: 22px 28px 40px;
  color: var(--paper);
}
/* ------------------------------ 顶栏 ------------------------------ */
.topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
}
.brand {
  display: flex;
  align-items: center;
  gap: 14px;
}
.seal {
  width: 46px;
  height: 46px;
  border-radius: 6px;
  background: var(--accent);
  color: #fff8ee;
  display: grid;
  place-items: center;
  font-size: 22px;
  border: 2px solid #9c2418;
  transform: rotate(-4deg);
  box-shadow: inset 0 0 0 2px rgba(255, 248, 238, 0.22), 0 3px 8px rgba(0, 0, 0, 0.4);
  line-height: 1.05;
  padding: 3px;
  flex: none;
}
.brand-title {
  font-size: 26px;
  letter-spacing: 8px;
  color: var(--accent);
  text-shadow: 0 0 16px rgba(232, 65, 43, 0.5);
  font-weight: 700;
}
.brand-sub {
  margin-top: 4px;
  font-size: 12px;
  letter-spacing: 3px;
  color: var(--muted);
}
.topbar-actions {
  display: flex;
  gap: 10px;
}
/* ------------------------------ 按钮 ------------------------------ */
.ghost-btn {
  padding: 8px 16px;
  font-size: 13px;
  letter-spacing: 3px;
  font-family: inherit;
  color: var(--paper);
  background: rgba(236, 228, 210, 0.06);
  border: 1px solid var(--line);
  border-radius: 4px;
  cursor: pointer;
  transition: 0.2s;
}
.ghost-btn:hover {
  border-color: var(--accent);
  color: var(--accent);
}
.ghost-btn.danger:hover {
  border-color: #e8412b;
  color: #e8412b;
}
.ghost-btn.warn:hover {
  border-color: #e8c372;
  color: #e8c372;
}
.accent-btn {
  padding: 8px 18px;
  font-size: 13px;
  letter-spacing: 3px;
  font-family: inherit;
  color: #fff8ee;
  background: var(--accent);
  border: none;
  border-radius: 4px;
  cursor: pointer;
  box-shadow: 0 6px 18px rgba(232, 65, 43, 0.3);
  transition: 0.2s;
}
.accent-btn:hover {
  filter: brightness(1.1);
  transform: translateY(-1px);
}
.accent-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none;
}
.mini {
  padding: 4px 10px;
  font-size: 12px;
  letter-spacing: 2px;
  font-family: inherit;
  color: var(--paper);
  background: transparent;
  border: 1px solid var(--line);
  border-radius: 3px;
  cursor: pointer;
  transition: 0.2s;
}
.mini + .mini {
  margin-left: 6px;
}
.mini:hover {
  border-color: var(--accent);
  color: var(--accent);
}
.mini.danger {
  color: #ff8d7a;
}
.mini.danger:hover {
  border-color: #e8412b;
  color: #e8412b;
}
/* ------------------------------ 页签 ------------------------------ */
.tabs {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 22px 0 16px;
  border-bottom: 1px solid var(--line);
  padding-bottom: 12px;
  flex-wrap: wrap;
}
.tab {
  padding: 8px 22px;
  font-size: 15px;
  letter-spacing: 4px;
  font-family: inherit;
  color: var(--muted);
  background: transparent;
  border: 1px solid transparent;
  border-radius: 4px;
  cursor: pointer;
  transition: 0.2s;
}
.tab:hover {
  color: var(--paper);
}
.tab.is-on {
  color: var(--accent);
  border-color: var(--accent);
  text-shadow: 0 0 12px rgba(232, 65, 43, 0.5);
}
.tab-side {
  margin-left: auto;
  display: flex;
  gap: 10px;
}
/* ------------------------------ 筛选 ------------------------------ */
.filters {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}
.filters select,
.filters input {
  font-family: inherit;
  font-size: 14px;
  color: var(--paper);
  background: rgba(236, 228, 210, 0.05);
  border: 1px solid var(--line);
  border-radius: 4px;
  padding: 8px 10px;
  outline: none;
}
.filters select:focus,
.filters input:focus {
  border-color: var(--accent);
}
.filters select option {
  background: #14120f;
  color: var(--paper);
}
.qbox input {
  width: 240px;
}
.total {
  margin-left: auto;
  font-size: 12px;
  letter-spacing: 2px;
  color: var(--muted);
}
/* ------------------------------ 表格 ------------------------------ */
.table-wrap {
  margin-top: 16px;
  border: 1px solid var(--line);
  border-radius: 6px;
  background: linear-gradient(180deg, rgba(18, 18, 16, 0.66), rgba(10, 10, 9, 0.72));
  backdrop-filter: blur(3px);
  overflow-x: auto;
  min-height: 220px;
}
table {
  width: 100%;
  border-collapse: collapse;
  font-size: 14px;
}
th,
td {
  text-align: left;
  padding: 11px 14px;
  border-bottom: 1px solid rgba(236, 228, 210, 0.1);
  letter-spacing: 1px;
  white-space: nowrap;
}
th {
  color: var(--muted);
  font-size: 12px;
  letter-spacing: 3px;
  font-weight: 400;
  background: rgba(236, 228, 210, 0.04);
}
td.col-text:last-of-type,
td .cell-desc {
  white-space: normal;
}
tbody tr:hover {
  background: rgba(236, 228, 210, 0.04);
}
.cell-name {
  color: var(--paper);
  font-size: 15px;
}
.dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  margin-right: 6px;
  vertical-align: middle;
}
.op-col {
  text-align: right;
}
th.op-col {
  text-align: right;
}
.state {
  padding: 60px 0;
  text-align: center;
  letter-spacing: 6px;
  color: var(--muted);
  font-size: 15px;
}
.err-text {
  color: var(--accent);
}
/* ------------------------------ 提示浮层 ------------------------------ */
.toast {
  position: fixed;
  top: 22px;
  left: 50%;
  transform: translateX(-50%);
  padding: 10px 26px;
  background: rgba(18, 18, 16, 0.92);
  border: 1px solid var(--accent);
  border-radius: 4px;
  color: var(--paper);
  font-size: 14px;
  letter-spacing: 3px;
  z-index: 20;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.5);
}
.toast-enter-active,
.toast-leave-active {
  transition: opacity 0.25s ease, transform 0.25s ease;
}
.toast-enter-from,
.toast-leave-to {
  opacity: 0;
  transform: translateX(-50%) translateY(-10px);
}
/* ------------------------------ 弹窗 ------------------------------ */
.mask {
  position: fixed;
  inset: 0;
  z-index: 30;
  background: rgba(8, 8, 7, 0.66);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}
.dialog {
  width: 440px;
  max-width: 100%;
  max-height: 86vh;
  overflow-y: auto;
  padding: 26px 28px 24px;
  border-radius: 6px;
  background: linear-gradient(180deg, rgba(24, 22, 19, 0.97), rgba(14, 13, 11, 0.97));
  border: 1px solid rgba(236, 228, 210, 0.18);
  box-shadow: 0 30px 80px rgba(0, 0, 0, 0.6);
}
.dialog-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 18px;
}
.dialog-title {
  font-size: 20px;
  letter-spacing: 6px;
  color: var(--accent);
  text-shadow: 0 0 14px rgba(232, 65, 43, 0.5);
}
.form-item {
  display: block;
  margin-bottom: 14px;
}
.form-label {
  display: block;
  font-size: 12px;
  letter-spacing: 3px;
  color: var(--muted);
  margin-bottom: 6px;
}
.form-label em {
  color: var(--accent);
  font-style: normal;
}
.form-item input,
.form-item select,
.form-item textarea {
  width: 100%;
  font-family: inherit;
  font-size: 14px;
  color: var(--paper);
  background: rgba(236, 228, 210, 0.05);
  border: 1px solid var(--line);
  border-radius: 4px;
  padding: 9px 10px;
  outline: none;
  resize: vertical;
}
.form-item input:focus,
.form-item select:focus,
.form-item textarea:focus {
  border-color: var(--accent);
}
.form-item select option {
  background: #14120f;
  color: var(--paper);
}
.dialog-err {
  min-height: 18px;
  font-size: 13px;
  letter-spacing: 1px;
  color: var(--accent);
  margin: 4px 0 10px;
}
.dialog-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>
