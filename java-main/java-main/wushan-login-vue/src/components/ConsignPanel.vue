<script setup>
/**
 * 宝库 · 山友寄售：别人的物品可在此用灵石购买，或以物换物。
 * 挂单从背包扣物，撤销退回；灵石购买会转给卖家，以物换物双方背包同时变动。
 */
import { ref, computed, onMounted } from 'vue'
import { treasureApi } from '../api/treasure'
import { worldApi } from '../api/world'
import ItemDetailModal from './ItemDetailModal.vue'

const list = ref([])
const mine = ref([])
const bag = ref([])
const tab = ref('all')
const showPublish = ref(false)
const showBarter = ref(null)
const offerItemId = ref('')
const form = ref({ itemId: '', count: 1, priceSpirit: '', barterItemId: '', barterTitle: '', mode: 0 })
const busy = ref(false)
const msg = ref('')
const detailItem = ref(null)

function flash(m) {
  msg.value = m
  setTimeout(() => { msg.value = '' }, 3200)
}

async function load() {
  try {
    const [a, b, g] = await Promise.all([
      treasureApi.getConsigns().catch(() => []),
      treasureApi.getMyConsigns().catch(() => []),
      worldApi.getBag().catch(() => ({ items: [] }))
    ])
    list.value = a || []
    mine.value = b || []
    bag.value = (g.items || []).filter((x) => x.count > 0)
  } catch (e) { flash(e.message || '寄售区读取失败') }
}

async function publish() {
  if (!form.value.itemId) return flash('请先选一件背包里的物品')
  busy.value = true
  try {
    const r = await treasureApi.publish({
      itemId: Number(form.value.itemId),
      count: Number(form.value.count) || 1,
      priceSpirit: form.value.priceSpirit === '' ? null : Number(form.value.priceSpirit),
      barterItemId: form.value.barterItemId === '' ? null : Number(form.value.barterItemId),
      barterTitle: form.value.barterTitle,
      mode: Number(form.value.mode)
    })
    flash(`已挂出「${r.itemTitle}」`)
    showPublish.value = false
    form.value = { itemId: '', count: 1, priceSpirit: '', barterItemId: '', barterTitle: '', mode: 0 }
    await load()
  } catch (e) { flash(e.message || '挂单失败') }
  finally { busy.value = false }
}

async function buy(c) {
  if (busy.value) return
  busy.value = true
  try {
    const r = await treasureApi.buy(c.id)
    flash(`以 ${r.spent} 灵石购得「${r.itemTitle}」×${r.count}，已入背包`)
    await load()
  } catch (e) { flash(e.message || '交易未成') }
  finally { busy.value = false }
}

function openBarter(c) {
  showBarter.value = c
  offerItemId.value = c.barterItemId ? String(c.barterItemId) : ''
}

async function barter() {
  if (!offerItemId.value) return flash('请选一件自己的物品作交换')
  busy.value = true
  try {
    const r = await treasureApi.barter(showBarter.value.id, Number(offerItemId.value))
    flash(`以「${r.offerTitle}」换得「${r.itemTitle}」`)
    showBarter.value = null
    await load()
  } catch (e) { flash(e.message || '交换未成') }
  finally { busy.value = false }
}

async function cancel(c) {
  if (busy.value) return
  busy.value = true
  try {
    await treasureApi.cancel(c.id)
    flash('挂单已撤，物品退回背包')
    await load()
  } catch (e) { flash(e.message || '撤销失败') }
  finally { busy.value = false }
}

function showDetail(c) {
  detailItem.value = {
    title: c.itemTitle,
    detail: c.detail,
    acquire: c.acquire,
    effect: c.effect,
    gradeLabel: c.gradeLabel,
    gradePrice: c.gradePrice,
    categoryLabel: '山友寄售'
  }
}

const shown = computed(() => tab.value === 'mine' ? mine.value : list.value)
onMounted(load)
</script>

<template>
  <div class="consign">
    <div class="cg-head">
      <div>
        <span class="cg-title">山 友 寄 售</span>
        <span class="cg-sub">别 人 的 物 品 · 灵 石 或 以 物 换 物</span>
      </div>
      <div class="cg-tabs">
        <span :class="{ on: tab === 'all' }" @click="tab = 'all'">寄 售 区</span>
        <span :class="{ on: tab === 'mine' }" @click="tab = 'mine'">我 的 挂 单</span>
      </div>
      <button class="cg-btn" @click="showPublish = !showPublish">{{ showPublish ? '收 起' : '挂 出 一 物' }}</button>
    </div>

    <div v-if="showPublish" class="cg-form">
      <select v-model="form.itemId" class="inp">
        <option value="">选一件背包里的物品</option>
        <option v-for="b in bag" :key="b.itemId" :value="b.itemId">{{ b.title }}（余 {{ b.count }}）</option>
      </select>
      <input v-model.number="form.count" class="inp num" type="number" min="1" max="99" placeholder="数量" />
      <input v-model="form.priceSpirit" class="inp num" type="number" min="0" placeholder="灵石价（留空按品质）" />
      <select v-model.number="form.mode" class="inp">
        <option :value="0">只 卖 灵 石</option>
        <option :value="1">只 以 物 换 物</option>
        <option :value="2">两 者 皆 可</option>
      </select>
      <input v-model="form.barterTitle" class="inp" placeholder="想换何物（如：雷击木心）" />
      <select v-model="form.barterItemId" class="inp">
        <option value="">指定想换的物品（可选）</option>
        <option v-for="b in bag" :key="b.itemId" :value="b.itemId">{{ b.title }}</option>
      </select>
      <button class="cg-btn" :disabled="busy" @click="publish">挂 出</button>
    </div>

    <div v-if="shown.length" class="cg-grid">
      <div v-for="c in shown" :key="c.id" class="cg-card" :class="{ mine: c.mine }">
        <div class="c-head">
          <span class="c-title">{{ c.itemTitle }} <small>×{{ c.count }}</small></span>
          <span class="c-grade">{{ c.gradeLabel }}</span>
        </div>
        <p class="c-desc">{{ c.detail || c.acquire }}</p>
        <div class="c-info">
          寄售人 · {{ c.sellerName }}
          <template v-if="c.priceSpirit"> · 灵石 {{ c.priceSpirit }}</template>
          <template v-if="c.barterTitle"> · 想换「{{ c.barterTitle }}」</template>
          <template v-if="c.status !== 0"> · {{ c.status === 1 ? '已成交' : c.status === 2 ? '已撤销' : '已取回' }}</template>
        </div>
        <div class="c-foot" v-if="!c.mine && c.status === 0">
          <button class="c-link" @click="showDetail(c)">详 情</button>
          <button v-if="c.mode !== 1" class="cg-btn small" :disabled="busy" @click="buy(c)">灵 石 购 得</button>
          <button v-if="c.mode !== 0" class="cg-btn small alt" :disabled="busy" @click="openBarter(c)">以 物 换 物</button>
        </div>
        <div class="c-foot" v-else-if="c.mine && c.status === 0">
          <button class="cg-btn small alt" :disabled="busy" @click="cancel(c)">撤 销 挂 单</button>
        </div>
      </div>
    </div>
    <p v-else class="cg-empty">{{ tab === 'mine' ? '你还没有挂单' : '寄售区暂无在架之物' }}</p>

    <!-- 以物换物：选一件自己的物品 -->
    <div v-if="showBarter" class="barter-mask" @click.self="showBarter = null">
      <div class="barter-box">
        <button class="x" @click="showBarter = null">×</button>
        <h3>以 物 换 物</h3>
        <p>拿出一件自己背包里的物品，换「{{ showBarter.itemTitle }}」。</p>
        <p v-if="showBarter.barterTitle" class="want">对方想换：{{ showBarter.barterTitle }}</p>
        <select v-model="offerItemId" class="inp wide">
          <option value="">选一件自己的物品</option>
          <option v-for="b in bag" :key="b.itemId" :value="b.itemId">{{ b.title }}（余 {{ b.count }}）</option>
        </select>
        <button class="cg-btn" :disabled="busy" @click="barter">交 换</button>
      </div>
    </div>

    <p v-if="msg" class="cg-msg">{{ msg }}</p>
    <ItemDetailModal :item="detailItem" @close="detailItem = null" />
  </div>
</template>

<style scoped>
.consign { margin: 18px 0 6px; padding: 16px 18px; border: 1px solid var(--line); background: rgba(12, 17, 16, 0.5); }
.cg-head { display: flex; align-items: center; gap: 14px; flex-wrap: wrap; margin-bottom: 14px; }
.cg-title { font-size: 16px; letter-spacing: 5px; color: #85b7eb; }
.cg-sub { font-size: 11px; letter-spacing: 1px; color: var(--muted); margin-left: 10px; }
.cg-tabs { display: flex; gap: 14px; margin-left: auto; }
.cg-tabs span { font-size: 12px; letter-spacing: 2px; color: var(--muted); cursor: pointer; padding-bottom: 3px; }
.cg-tabs span.on { color: #e8c372; border-bottom: 1px solid #e8c372; }
.cg-btn {
  font: inherit; font-size: 12px; letter-spacing: 2px; padding: 7px 15px; cursor: pointer;
  color: #f6ece0; background: linear-gradient(180deg, rgba(163, 49, 31, 0.9), rgba(120, 33, 21, 0.9)); border: 1px solid #c8543c;
}
.cg-btn:disabled { opacity: 0.45; cursor: not-allowed; }
.cg-btn.small { padding: 5px 11px; font-size: 11px; }
.cg-btn.alt { background: rgba(58, 143, 208, 0.22); border-color: #3a8fd0; color: #cfe6fa; }
.cg-form { display: flex; gap: 9px; flex-wrap: wrap; margin-bottom: 16px; }
.inp { font: inherit; font-size: 13px; color: var(--paper); background: rgba(236, 228, 210, 0.06); border: 1px solid var(--line); padding: 7px 10px; min-width: 160px; }
.inp option { background: #16201d; }
.inp.num { min-width: 80px; width: 120px; }
.inp.wide { width: 100%; margin: 12px 0; }
.cg-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 12px; }
.cg-card { padding: 12px 14px; border: 1px solid var(--line); background: rgba(236, 228, 210, 0.03); }
.cg-card.mine { border-left: 2px solid #85b7eb; }
.c-head { display: flex; align-items: baseline; gap: 8px; }
.c-title { font-size: 16px; letter-spacing: 2px; }
.c-title small { font-size: 11px; color: var(--muted); }
.c-grade { margin-left: auto; font-size: 11px; color: #e8c372; }
.c-desc { margin: 8px 0; font-size: 12px; line-height: 1.8; color: var(--muted); display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
.c-info { font-size: 11px; letter-spacing: 1px; color: rgba(236, 228, 210, 0.6); margin-bottom: 10px; }
.c-foot { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.c-link { margin-right: auto; font: inherit; font-size: 11px; letter-spacing: 2px; color: #85b7eb; background: none; border: none; cursor: pointer; padding: 0; }
.cg-empty, .cg-msg { font-size: 12px; letter-spacing: 2px; color: var(--muted); text-align: center; padding: 14px 0; }
.cg-msg { color: #e8c372; }
.barter-mask { position: fixed; inset: 0; z-index: 91; display: grid; place-items: center; background: rgba(6, 9, 8, 0.75); }
.barter-box { position: relative; width: min(420px, 92vw); padding: 26px; color: var(--paper); background: #141a18; border: 1px solid var(--line); font-family: "Kaiti SC", "STKaiti", "KaiTi", "楷体", serif; }
.barter-box .x { position: absolute; right: 12px; top: 8px; font-size: 24px; color: var(--muted); background: none; border: none; cursor: pointer; }
.barter-box h3 { margin: 0 0 12px; font-size: 18px; letter-spacing: 5px; font-weight: normal; color: #e8c372; }
.barter-box p { font-size: 13px; line-height: 1.9; color: var(--muted); }
.barter-box .want { color: #85b7eb; }
</style>
