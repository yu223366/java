<script setup>
/**
 * 求购榜：山友挂出想收之物并标出最高愿付灵石，持有者可上门。
 * 嵌在宝库页顶部，和市集「卖」形成对照。
 */
import { ref, computed, onMounted } from 'vue'
import { wantedApi } from '../api/wanted'
import { sessionStore } from '../api/auth'

const list = ref([])
const showNew = ref(false)
const form = ref({ title: '', maxPrice: '', note: '' })
const busy = ref(false)
const msg = ref('')

const myName = computed(() => {
  try {
    const s = sessionStore.get()
    return (s && (s.username || s.name)) || ''
  } catch (e) {
    return ''
  }
})

async function load() {
  try {
    list.value = await wantedApi.list()
  } catch (e) { /* 求购榜失败不影响宝库 */ }
}

function flash(m) {
  msg.value = m
  setTimeout(() => { msg.value = '' }, 2200)
}

async function publish() {
  if (!form.value.title.trim()) return flash('请写明求购之物')
  busy.value = true
  try {
    await wantedApi.publish(
      form.value.title.trim(),
      Number(form.value.maxPrice) || 0,
      form.value.note.trim()
    )
    flash('求购已挂出')
    form.value = { title: '', maxPrice: '', note: '' }
    showNew.value = false
    await load()
  } catch (e) {
    flash(e.message || '挂单失败')
  } finally {
    busy.value = false
  }
}

async function close(w) {
  busy.value = true
  try {
    await wantedApi.close(w.id)
    flash('已撤销求购')
    await load()
  } catch (e) {
    flash(e.message || '撤销失败')
  } finally {
    busy.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="wanted">
    <div class="wt-head">
      <span class="wt-title">求 购 榜</span>
      <span class="wt-sub">山 友 悬 货 求 购 · 最 高 愿 付 见 价</span>
      <button class="wt-new" @click="showNew = !showNew">{{ showNew ? '收 起' : '挂 求 购' }}</button>
    </div>

    <div v-if="showNew" class="wt-form">
      <input v-model="form.title" class="inp" placeholder="想求购之物，如《焚天七绝焰》" maxlength="60" />
      <input v-model="form.maxPrice" class="inp num" type="number" min="0" placeholder="最高愿付灵石" />
      <input v-model="form.note" class="inp" placeholder="备注（可选）" maxlength="120" />
      <button class="wt-btn" :disabled="busy" @click="publish">挂 上</button>
    </div>

    <div class="wt-list">
      <div v-for="w in list" :key="w.id" class="wt-item">
        <span class="wt-user">{{ w.username }}</span>
        <span class="wt-title2">{{ w.title }}</span>
        <span class="wt-price">最高 {{ w.maxPrice }} 灵石</span>
        <span class="wt-note" v-if="w.note">{{ w.note }}</span>
        <button v-if="myName && w.username === myName" class="wt-close" :disabled="busy" @click="close(w)">撤 销</button>
      </div>
      <div v-if="!list.length" class="wt-empty">尚 无 人 求 购 · 你 可 挂 一 物</div>
    </div>

    <div v-if="msg" class="wt-msg">{{ msg }}</div>
  </div>
</template>

<style scoped>
.wanted { margin-bottom: 16px; padding: 14px 16px; border: 1px solid var(--line); background: rgba(12, 17, 16, 0.5); }
.wt-head { display: flex; align-items: baseline; gap: 12px; flex-wrap: wrap; margin-bottom: 12px; }
.wt-title { font-size: 14px; letter-spacing: 5px; color: #e8c372; }
.wt-sub { font-size: 11px; letter-spacing: 1px; color: var(--muted); }
.wt-new { margin-left: auto; font: inherit; font-size: 12px; letter-spacing: 2px; padding: 6px 14px; color: var(--paper); background: rgba(236, 228, 210, 0.08); border: 1px solid var(--line); cursor: pointer; }
.wt-new:hover { background: rgba(236, 228, 210, 0.18); }
.wt-form { display: flex; gap: 10px; margin-bottom: 12px; flex-wrap: wrap; }
.inp { font: inherit; font-size: 13px; color: var(--paper); background: rgba(236, 228, 210, 0.06); border: 1px solid var(--line); padding: 8px 11px; min-width: 160px; }
.inp.num { width: 130px; min-width: 0; }
.wt-btn { font: inherit; font-size: 13px; letter-spacing: 2px; padding: 8px 18px; color: #f6ece0; background: linear-gradient(180deg, rgba(163, 49, 31, 0.92), rgba(120, 33, 21, 0.92)); border: 1px solid #c8543c; cursor: pointer; }
.wt-btn:disabled { opacity: 0.5; cursor: not-allowed; }
.wt-list { display: flex; flex-direction: column; gap: 7px; }
.wt-item { display: flex; align-items: center; gap: 12px; padding: 8px 11px; border-left: 2px solid rgba(232, 195, 114, 0.5); background: rgba(236, 228, 210, 0.04); font-size: 13px; letter-spacing: 1px; }
.wt-user { color: rgba(236, 228, 210, 0.75); font-size: 12px; }
.wt-title2 { color: #e8c372; }
.wt-price { color: rgba(236, 228, 210, 0.9); }
.wt-note { flex: 1; font-size: 11px; color: var(--muted); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.wt-close { margin-left: auto; font: inherit; font-size: 11px; letter-spacing: 1px; padding: 3px 10px; color: var(--muted); background: none; border: 1px solid var(--line); cursor: pointer; }
.wt-close:hover { color: var(--paper); }
.wt-empty { font-size: 12px; letter-spacing: 2px; color: var(--muted); text-align: center; padding: 14px 0; }
.wt-msg { margin-top: 8px; font-size: 12px; letter-spacing: 2px; color: #e8c372; text-align: center; }
</style>
