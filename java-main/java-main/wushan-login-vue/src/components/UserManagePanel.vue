<script setup>
// 山友管理（用户系统）面板：执事对山友的检索、详情、封禁/解封、重置暗语、
// 调整资源（灵石/修为/境界）、升降执事、注销/恢复。mock 与真实后端双模式。
import { ref, reactive, computed, onMounted } from 'vue'
import { adminApi } from '../api/admin'
import { findElement } from '../constants/wuxing'

const props = defineProps({
  username: { type: String, default: '' }
})

/* ------------------------------ 筛选 ------------------------------ */
const q = ref('')
const roleFilter = ref('all') // all | admin | user
const statusFilter = ref('all') // all | normal | banned | deleted
const includeDeleted = ref(false)

/* ------------------------------ 列表 ------------------------------ */
const list = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(12)
const loading = ref(false)
const err = ref('')
const notice = ref('')
let noticeTimer = null
function toast(msg) {
  notice.value = msg
  clearTimeout(noticeTimer)
  noticeTimer = setTimeout(() => (notice.value = ''), 2200)
}

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / size.value)))

async function loadUsers() {
  loading.value = true
  err.value = ''
  try {
    const r = await adminApi.listUsers({
      q: q.value.trim(),
      role: roleFilter.value === 'all' ? '' : roleFilter.value,
      status: statusFilter.value === 'all' ? '' : statusFilter.value,
      includeDeleted: includeDeleted.value,
      page: page.value,
      size: size.value
    })
    list.value = r.items || []
    total.value = r.total != null ? r.total : list.value.length
    page.value = r.page || page.value
  } catch (e) {
    err.value = e instanceof Error ? e.message : '山门未应，请稍后重试。'
    list.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function doSearch() {
  page.value = 1
  loadUsers()
}
function changePage(d) {
  const np = page.value + d
  if (np < 1 || np > totalPages.value) return
  page.value = np
  loadUsers()
}

/* ------------------------------ 详情抽屉 ------------------------------ */
const detail = ref(null)
const detailLoading = ref(false)
async function openDetail(user) {
  detailLoading.value = true
  try {
    detail.value = await adminApi.userDetail(user.id)
  } catch (e) {
    toast(e instanceof Error ? e.message : '无法读取详情。')
  } finally {
    detailLoading.value = false
  }
}
function closeDetail() {
  detail.value = null
}

/* ------------------------------ 操作弹窗 ------------------------------ */
// type: confirm(封禁/解封/升降/注销/恢复) | reset(重置暗语) | adjust(调整资源)
const dialog = ref(null)
const dForm = reactive({ password: '123456', spirit: 0, exp: 0, level: 0 })
const dErr = ref('')
const dSaving = ref(false)

const DIALOG_META = {
  ban: { title: '封禁山友', confirm: '封 禁', desc: '封禁后该山友将无法登录山门。' },
  unban: { title: '解封山友', confirm: '解 封', desc: '解封后恢复其登录与修行资格。' },
  roleUp: { title: '提任执事', confirm: '提 任', desc: '提任后该山友可入执事后台，请谨慎授予。' },
  roleDown: { title: '降为山友', confirm: '降 职', desc: '降职后其将失去执事后台权限。' },
  delete: { title: '注销山籍', confirm: '注 销', desc: '软注销：数据保留可恢复，该山友将无法登录。' },
  restore: { title: '恢复山籍', confirm: '恢 复', desc: '恢复后该山友可重新登录山门。' },
  reset: { title: '重置暗语', confirm: '重 置', desc: '将该山友暗语重置为下方值（默认 123456）。' },
  adjust: { title: '调整资源', confirm: '落 印', desc: '灵石 / 修为 / 境界 填增减量，正加负减，留 0 不改。' }
}

function openDialog(type, user) {
  dialog.value = { type, user }
  dErr.value = ''
  dForm.password = '123456'
  dForm.spirit = 0
  dForm.exp = 0
  dForm.level = 0
}
function closeDialog() {
  dialog.value = null
}

// 行内快捷按钮：根据当前状态决定走哪个 type
function toggleBan(user) {
  openDialog(user.status === 0 ? 'unban' : 'ban', user)
}
function toggleRole(user) {
  openDialog(user.role === 0 ? 'roleDown' : 'roleUp', user)
}
function toggleDeleted(user) {
  openDialog(user.deleted === 1 ? 'restore' : 'delete', user)
}
function isSelf(user) {
  return props.username && user.username === props.username
}

async function doDialogConfirm() {
  if (!dialog.value) return
  dErr.value = ''
  dSaving.value = true
  const { type, user } = dialog.value
  try {
    if (type === 'ban') {
      await adminApi.banUser(user.id)
      toast(`已封禁「${user.username}」。`)
    } else if (type === 'unban') {
      await adminApi.unbanUser(user.id)
      toast(`已解封「${user.username}」。`)
    } else if (type === 'roleUp') {
      await adminApi.setRole(user.id, 0)
      toast(`已提任「${user.username}」为执事。`)
    } else if (type === 'roleDown') {
      await adminApi.setRole(user.id, 1)
      toast(`已将「${user.username}」降为山友。`)
    } else if (type === 'delete') {
      await adminApi.deleteUser(user.id)
      toast(`已注销「${user.username}」山籍。`)
    } else if (type === 'restore') {
      await adminApi.restoreUser(user.id)
      toast(`已恢复「${user.username}」山籍。`)
    } else if (type === 'reset') {
      await adminApi.resetPassword(user.id, dForm.password.trim() || '123456')
      toast(`已重置「${user.username}」暗语。`)
    } else if (type === 'adjust') {
      const payload = {}
      if (dForm.spirit) payload.spirit = Number(dForm.spirit)
      if (dForm.exp) payload.exp = Number(dForm.exp)
      if (dForm.level) payload.level = Number(dForm.level)
      await adminApi.adjustResource(user.id, payload)
      toast(`已调整「${user.username}」资源。`)
    }
    closeDialog()
    await loadUsers()
    if (detail.value && detail.value.id === user.id) {
      detail.value = await adminApi.userDetail(user.id)
    }
  } catch (e) {
    dErr.value = e instanceof Error ? e.message : '操作未成，请重试。'
  } finally {
    dSaving.value = false
  }
}

/* ------------------------------ 展示辅助 ------------------------------ */
function elColor(el) {
  return (findElement(el) || {}).color || '#888'
}
function elLabel(el) {
  return (findElement(el) || {}).label || el || '—'
}
function statusBadge(u) {
  if (u.deleted === 1) return { text: '已注销', cls: 'b-grey' }
  return u.status === 0 ? { text: '封禁', cls: 'b-red' } : { text: '正常', cls: 'b-green' }
}
function roleBadge(role) {
  return role === 0 ? { text: '执事', cls: 'b-gold' } : { text: '山友', cls: 'b-blue' }
}

onMounted(loadUsers)
</script>

<template>
  <div class="ump-root">
    <!-- 筛选行 -->
    <div class="filters">
      <select v-model="roleFilter" @change="doSearch">
        <option value="all">身份不限</option>
        <option value="admin">执事</option>
        <option value="user">山友</option>
      </select>
      <select v-model="statusFilter" @change="doSearch">
        <option value="all">状态不限</option>
        <option value="normal">正常</option>
        <option value="banned">封禁</option>
        <option value="deleted">已注销</option>
      </select>
      <div class="qbox">
        <input v-model="q" placeholder="检索名号…" @keydown.enter="doSearch" />
      </div>
      <button class="ghost-btn" @click="doSearch">检 索</button>
      <label class="ck">
        <input type="checkbox" v-model="includeDeleted" @change="doSearch" /> 含已注销
      </label>
      <span class="total">共 {{ total }} 位 · 第 {{ page }}/{{ totalPages }} 页</span>
      <span class="pg">
        <button class="mini" :disabled="page <= 1" @click="changePage(-1)">上一页</button>
        <button class="mini" :disabled="page >= totalPages" @click="changePage(1)">下一页</button>
      </span>
    </div>

    <!-- 数据表 -->
    <main class="table-wrap">
      <div v-if="loading" class="state">点 名 中 …</div>
      <div v-else-if="err" class="state err-text">{{ err }}</div>
      <div v-else-if="!list.length" class="state">山门暂无此辈。</div>
      <table v-else>
        <thead>
          <tr>
            <th>名号</th>
            <th>五行</th>
            <th>身份</th>
            <th>状态</th>
            <th>灵石</th>
            <th>境界</th>
            <th>本重修为</th>
            <th>所属宗门</th>
            <th>加入时间</th>
            <th class="op-col">操 作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="u in list" :key="u.id">
            <td class="cell-name">
              <a class="link" @click="openDetail(u)">{{ u.username }}</a>
            </td>
            <td>
              <i class="dot" :style="{ background: elColor(u.element) }"></i>{{ elLabel(u.element) }}
            </td>
            <td><span class="badge" :class="roleBadge(u.role).cls">{{ roleBadge(u.role).text }}</span></td>
            <td><span class="badge" :class="statusBadge(u).cls">{{ statusBadge(u).text }}</span></td>
            <td>{{ u.spirit }}</td>
            <td>第 {{ u.practiceLevel }} 重</td>
            <td>{{ u.exp }}</td>
            <td>{{ u.sectName || '—' }}</td>
            <td class="muted">{{ (u.createTime || '').slice(0, 10) }}</td>
            <td class="op-col">
              <button class="mini" @click="openDetail(u)">详情</button>
              <button class="mini" :disabled="isSelf(u)" :title="isSelf(u) ? '不可操作自身' : ''" @click="toggleBan(u)">
                {{ u.status === 0 ? '解封' : '封禁' }}
              </button>
              <button class="mini" @click="openDialog('reset', u)">暗语</button>
              <button class="mini" @click="openDialog('adjust', u)">调整</button>
              <button class="mini" :disabled="isSelf(u)" :title="isSelf(u) ? '不可操作自身' : ''" @click="toggleRole(u)">
                {{ u.role === 0 ? '降职' : '提任' }}
              </button>
              <button class="mini danger" :disabled="isSelf(u)" :title="isSelf(u) ? '不可操作自身' : ''" @click="toggleDeleted(u)">
                {{ u.deleted === 1 ? '恢复' : '注销' }}
              </button>
            </td>
          </tr>
        </tbody>
      </table>
    </main>

    <!-- 成功提示 -->
    <Transition name="toast">
      <div v-if="notice" class="toast">{{ notice }}</div>
    </Transition>

    <!-- 详情抽屉 -->
    <div v-if="detail" class="mask" @click.self="closeDetail">
      <div class="drawer">
        <div class="dialog-head">
          <div class="dialog-title">山籍详情</div>
          <button class="mini" @click="closeDetail">✕</button>
        </div>
        <div v-if="detailLoading" class="state">观 相 中 …</div>
        <div v-else class="detail-body">
          <section class="blk">
            <h4>基础</h4>
            <div class="kv"><span>名号</span><b>{{ detail.username }}</b></div>
            <div class="kv"><span>五行</span><b><i class="dot" :style="{ background: elColor(detail.element) }"></i>{{ elLabel(detail.element) }}</b></div>
            <div class="kv"><span>身份</span><b>{{ detail.roleLabel }}</b></div>
            <div class="kv"><span>状态</span><b>{{ detail.statusLabel }}</b></div>
            <div class="kv"><span>灵石</span><b>{{ detail.spirit }}</b></div>
            <div class="kv"><span>加入</span><b class="muted">{{ (detail.createTime || '').slice(0, 19).replace('T', ' ') }}</b></div>
          </section>
          <section class="blk">
            <h4>修为</h4>
            <div class="kv"><span>境界</span><b>第 {{ detail.practice.level }} 重</b></div>
            <div class="kv"><span>本重修为</span><b>{{ detail.practice.exp }}</b></div>
            <div class="kv"><span>累计练功</span><b>{{ detail.practice.totalCount }} 次</b></div>
            <div class="kv"><span>累计灵石</span><b>{{ detail.practice.totalSpirit }}</b></div>
            <div class="kv"><span>上次练功</span><b class="muted">{{ (detail.practice.lastTime || '').slice(0, 19).replace('T', ' ') || '—' }}</b></div>
          </section>
          <section class="blk">
            <h4>宗门</h4>
            <div class="kv"><span>宗门</span><b>{{ detail.sect.sectName || '散修' }}</b></div>
            <div class="kv"><span>身份</span><b>{{ detail.sect.roleLabel }}</b></div>
            <div class="kv"><span>贡献点</span><b>{{ detail.sect.contribution }}</b></div>
          </section>
          <section class="blk">
            <h4>经营</h4>
            <div class="kv"><span>在售寄售</span><b>{{ detail.stats.consignActive }}</b></div>
            <div class="kv"><span>累计寄售</span><b>{{ detail.stats.consignTotal }}</b></div>
          </section>
          <div class="drawer-actions">
            <button class="mini" @click="toggleBan(detail)">{{ detail.status === 0 ? '解封' : '封禁' }}</button>
            <button class="mini" @click="openDialog('reset', detail)">重置暗语</button>
            <button class="mini" @click="openDialog('adjust', detail)">调整资源</button>
            <button class="mini" @click="toggleRole(detail)">{{ detail.role === 0 ? '降职' : '提任' }}</button>
            <button class="mini danger" @click="toggleDeleted(detail)">{{ detail.deleted === 1 ? '恢复' : '注销' }}</button>
          </div>
        </div>
      </div>
    </div>

    <!-- 操作弹窗 -->
    <div v-if="dialog" class="mask" @click.self="closeDialog">
      <div class="dialog">
        <div class="dialog-head">
          <div class="dialog-title">{{ (DIALOG_META[dialog.type] || {}).title }}</div>
          <button class="mini" @click="closeDialog">✕</button>
        </div>
        <p class="d-desc">{{ (DIALOG_META[dialog.type] || {}).desc }}</p>
        <div class="d-target">山友：<b>{{ dialog.user.username }}</b></div>

        <label v-if="dialog.type === 'reset'" class="form-item">
          <span class="form-label">新暗语（4~32 位）</span>
          <input v-model="dForm.password" type="text" placeholder="默认 123456" />
        </label>
        <template v-if="dialog.type === 'adjust'">
          <label class="form-item">
            <span class="form-label">灵石增减（正加负减）</span>
            <input v-model.number="dForm.spirit" type="number" />
          </label>
          <label class="form-item">
            <span class="form-label">本重修为增减</span>
            <input v-model.number="dForm.exp" type="number" />
          </label>
          <label class="form-item">
            <span class="form-label">境界重数增减（不低于 1）</span>
            <input v-model.number="dForm.level" type="number" />
          </label>
        </template>

        <div class="dialog-err">{{ dErr }}</div>
        <div class="dialog-actions">
          <button class="ghost-btn" @click="closeDialog">作 罢</button>
          <button class="accent-btn" :disabled="dSaving" @click="doDialogConfirm">
            {{ dSaving ? '落 印…' : (DIALOG_META[dialog.type] || {}).confirm }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.ump-root {
  color: var(--paper);
}
/* ------------------------------ 筛选 ------------------------------ */
.filters {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  margin-top: 4px;
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
  width: 200px;
}
.ck {
  font-size: 13px;
  letter-spacing: 1px;
  color: var(--muted);
  cursor: pointer;
}
.total {
  margin-left: auto;
  font-size: 12px;
  letter-spacing: 2px;
  color: var(--muted);
}
.pg {
  display: flex;
  gap: 6px;
}
/* ------------------------------ 表格 ------------------------------ */
.table-wrap {
  margin-top: 16px;
  border: 1px solid var(--line);
  border-radius: 6px;
  background: linear-gradient(180deg, rgba(18, 18, 16, 0.8), rgba(10, 10, 9, 0.84));
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
tbody tr:hover {
  background: rgba(236, 228, 210, 0.04);
}
.cell-name {
  font-size: 15px;
  color: var(--paper);
}
.link {
  cursor: pointer;
  border-bottom: 1px dashed var(--line);
}
.link:hover {
  color: var(--accent);
  border-color: var(--accent);
}
.dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  margin-right: 6px;
  vertical-align: middle;
}
.muted {
  color: var(--muted);
}
.op-col {
  text-align: right;
}
th.op-col {
  text-align: right;
}
/* ------------------------------ 徽标 ------------------------------ */
.badge {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 10px;
  font-size: 12px;
  letter-spacing: 1px;
  border: 1px solid currentColor;
}
.b-green { color: #7fd6a0; }
.b-red { color: #ff8d7a; }
.b-grey { color: #9a948a; }
.b-gold { color: #e8c372; }
.b-blue { color: #8fb8e8; }
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
}
.accent-btn:hover {
  filter: brightness(1.1);
}
.accent-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
.mini {
  padding: 4px 10px;
  font-size: 12px;
  letter-spacing: 1px;
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
.mini:hover:not(:disabled) {
  border-color: var(--accent);
  color: var(--accent);
}
.mini:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}
.mini.danger {
  color: #ff8d7a;
}
.mini.danger:hover:not(:disabled) {
  border-color: #e8412b;
  color: #e8412b;
}
/* ------------------------------ 状态 ------------------------------ */
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
  z-index: 40;
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
/* ------------------------------ 弹窗 / 抽屉 ------------------------------ */
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
  width: 420px;
  max-width: 100%;
  max-height: 86vh;
  overflow-y: auto;
  padding: 26px 28px 24px;
  border-radius: 6px;
  background: linear-gradient(180deg, rgba(24, 22, 19, 0.97), rgba(14, 13, 11, 0.97));
  border: 1px solid rgba(236, 228, 210, 0.18);
  box-shadow: 0 30px 80px rgba(0, 0, 0, 0.6);
}
.drawer {
  width: 520px;
  max-width: 100%;
  max-height: 88vh;
  overflow-y: auto;
  padding: 24px 26px;
  border-radius: 8px;
  background: linear-gradient(180deg, rgba(24, 22, 19, 0.98), rgba(14, 13, 11, 0.98));
  border: 1px solid rgba(236, 228, 210, 0.18);
  box-shadow: 0 30px 80px rgba(0, 0, 0, 0.6);
}
.dialog-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
}
.dialog-title {
  font-size: 20px;
  letter-spacing: 6px;
  color: var(--accent);
  text-shadow: 0 0 14px rgba(232, 65, 43, 0.5);
}
.d-desc {
  font-size: 13px;
  letter-spacing: 1px;
  color: var(--muted);
  margin: 0 0 12px;
  line-height: 1.7;
}
.d-target {
  font-size: 14px;
  letter-spacing: 2px;
  margin-bottom: 14px;
}
/* ------------------------------ 详情块 ------------------------------ */
.detail-body {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px 22px;
}
.blk {
  border: 1px solid rgba(236, 228, 210, 0.12);
  border-radius: 6px;
  padding: 12px 14px;
  background: rgba(236, 228, 210, 0.03);
}
.blk h4 {
  margin: 0 0 8px;
  font-size: 13px;
  letter-spacing: 4px;
  color: var(--accent);
  font-weight: 400;
}
.kv {
  display: flex;
  justify-content: space-between;
  font-size: 14px;
  padding: 3px 0;
  letter-spacing: 1px;
}
.kv span {
  color: var(--muted);
}
.kv b {
  font-weight: 500;
  color: var(--paper);
}
.drawer-actions {
  grid-column: 1 / -1;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 6px;
  border-top: 1px solid var(--line);
  padding-top: 14px;
}
/* ------------------------------ 表单 ------------------------------ */
.form-item {
  display: block;
  margin-bottom: 14px;
}
.form-label {
  display: block;
  font-size: 12px;
  letter-spacing: 2px;
  color: var(--muted);
  margin-bottom: 6px;
}
.form-item input {
  width: 100%;
  font-family: inherit;
  font-size: 14px;
  color: var(--paper);
  background: rgba(236, 228, 210, 0.05);
  border: 1px solid var(--line);
  border-radius: 4px;
  padding: 9px 10px;
  outline: none;
}
.form-item input:focus {
  border-color: var(--accent);
}
.form-item input[type='number'] {
  -moz-appearance: textfield;
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
