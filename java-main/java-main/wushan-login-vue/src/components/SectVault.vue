<script setup>
/**
 * 宗门宝库：宝库归宗门管，本宗成员用贡献点兑换库中藏品。
 * 成员也可把背包里的物品存入宗库，换取贡献点并补充库存。
 */
import { ref, computed, onMounted } from 'vue'
import { treasureApi } from '../api/treasure'
import { worldApi } from '../api/world'
import { findElement } from '../constants/wuxing'
import ItemDetailModal from './ItemDetailModal.vue'

const vault = ref({ hasSect: false, stocks: [], myContribution: 0, sectName: '' })
const bag = ref([])
const showDeposit = ref(false)
const form = ref({ itemId: '', count: 1, contributionPrice: '' })
const busy = ref(false)
const msg = ref('')
const detailItem = ref(null)

function flash(m) {
  msg.value = m
  setTimeout(() => { msg.value = '' }, 3200)
}

async function load() {
  try {
    vault.value = await treasureApi.getVault()
  } catch (e) { flash(e.message || '宗库读取失败') }
}

async function loadBag() {
  try {
    const r = await worldApi.getBag()
    bag.value = (r.items || []).filter((b) => b.count > 0)
  } catch (e) { /* 背包失败不阻塞 */ }
}

async function deposit() {
  if (!form.value.itemId) return flash('请先选一件背包里的物品')
  busy.value = true
  try {
    const r = await treasureApi.deposit(
      Number(form.value.itemId),
      Number(form.value.count) || 1,
      form.value.contributionPrice === '' ? null : Number(form.value.contributionPrice)
    )
    flash(`已存入「${r.itemTitle}」×${r.count}，得 ${r.rewardContribution} 贡献点`)
    showDeposit.value = false
    form.value = { itemId: '', count: 1, contributionPrice: '' }
    await Promise.all([load(), loadBag()])
  } catch (e) { flash(e.message || '存入失败') }
  finally { busy.value = false }
}

async function redeem(s, count = 1) {
  if (busy.value) return
  busy.value = true
  try {
    const r = await treasureApi.redeem(s.id, count)
    flash(`兑换「${r.itemTitle}」×${r.count}，耗 ${r.costContribution} 贡献点，已入背包`)
    await Promise.all([load(), loadBag()])
  } catch (e) { flash(e.message || '兑换失败') }
  finally { busy.value = false }
}

function showDetail(s) {
  detailItem.value = {
    title: s.title,
    element: '',
    detail: s.detail,
    acquire: s.acquire,
    effect: s.effect,
    gradeLabel: s.gradeLabel,
    gradePrice: s.gradePrice,
    categoryLabel: '宗门宝库'
  }
}

const stocks = computed(() => vault.value.stocks || [])
onMounted(() => { load(); loadBag() })
</script>

<template>
  <div class="vault">
    <div class="v-head">
      <div>
        <span class="v-title">宗 门 宝 库</span>
        <span class="v-sub">一 宗 一 库 · 以 贡 献 点 兑 换</span>
      </div>
      <div class="v-stat" v-if="vault.hasSect">
        <span>{{ vault.sectName }}</span>
        <b>贡献 {{ vault.myContribution || 0 }}</b>
      </div>
      <button class="v-btn" :disabled="!vault.hasSect" @click="showDeposit = !showDeposit">
        {{ showDeposit ? '收 起' : '存 入 宗 库' }}
      </button>
    </div>

    <p v-if="!vault.hasSect" class="v-empty">{{ vault.tip || '尚无山门' }}</p>

    <div v-if="showDeposit && vault.hasSect" class="v-form">
      <select v-model="form.itemId" class="inp">
        <option value="">选一件背包里的物品</option>
        <option v-for="b in bag" :key="b.itemId" :value="b.itemId">
          {{ b.title }}（余 {{ b.count }}）
        </option>
      </select>
      <input v-model.number="form.count" class="inp num" type="number" min="1" max="99" placeholder="数量" />
      <input v-model="form.contributionPrice" class="inp num" type="number" min="1" placeholder="兑换价（留空自动）" />
      <button class="v-btn" :disabled="busy" @click="deposit">存 入</button>
    </div>

    <div v-if="vault.hasSect && stocks.length" class="v-grid">
      <div v-for="s in stocks" :key="s.id" class="v-card" :class="{ off: !s.count }">
        <div class="c-head">
          <span class="c-title">{{ s.title }}</span>
          <span class="c-grade">{{ s.gradeLabel }}</span>
        </div>
        <p class="c-desc">{{ s.detail || s.acquire }}</p>
        <div class="c-foot">
          <span class="c-price">{{ s.contributionPrice }} <small>贡献点</small></span>
          <span class="c-stock">库 存 {{ s.count }}</span>
          <button class="c-link" @click="showDetail(s)">详 情</button>
          <button class="v-btn small" :disabled="!s.canRedeem || !s.count || busy" @click="redeem(s, 1)">
            {{ s.canRedeem ? '兑 换' : '贡 献 不 足' }}
          </button>
        </div>
      </div>
    </div>
    <p v-else-if="vault.hasSect" class="v-empty">库中暂无藏品，宗主可先存入一批。</p>

    <p v-if="msg" class="v-msg">{{ msg }}</p>
    <ItemDetailModal :item="detailItem" @close="detailItem = null" />
  </div>
</template>

<style scoped>
.vault { margin: 18px 0 6px; padding: 16px 18px; border: 1px solid var(--line); background: rgba(12, 17, 16, 0.5); }
.v-head { display: flex; align-items: center; gap: 14px; flex-wrap: wrap; margin-bottom: 14px; }
.v-title { font-size: 16px; letter-spacing: 5px; color: #e8c372; }
.v-sub { font-size: 11px; letter-spacing: 1px; color: var(--muted); margin-left: 10px; }
.v-stat { margin-left: auto; display: flex; gap: 14px; align-items: baseline; font-size: 12px; color: var(--muted); }
.v-stat b { color: #e8c372; font-size: 14px; font-weight: normal; }
.v-btn {
  font: inherit; font-size: 12px; letter-spacing: 2px; padding: 7px 15px; cursor: pointer;
  color: #f6ece0; background: linear-gradient(180deg, rgba(163, 49, 31, 0.9), rgba(120, 33, 21, 0.9));
  border: 1px solid #c8543c;
}
.v-btn:disabled { opacity: 0.45; cursor: not-allowed; }
.v-btn.small { padding: 5px 12px; font-size: 11px; }
.v-form { display: flex; gap: 10px; flex-wrap: wrap; margin-bottom: 16px; }
.inp { font: inherit; font-size: 13px; color: var(--paper); background: rgba(236, 228, 210, 0.06); border: 1px solid var(--line); padding: 7px 10px; min-width: 170px; }
.inp option { background: #16201d; }
.inp.num { min-width: 90px; width: 120px; }
.v-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(250px, 1fr)); gap: 12px; }
.v-card { padding: 12px 14px; border: 1px solid var(--line); background: rgba(236, 228, 210, 0.03); }
.v-card.off { opacity: 0.55; }
.c-head { display: flex; align-items: baseline; gap: 8px; }
.c-title { font-size: 16px; letter-spacing: 2px; }
.c-grade { margin-left: auto; font-size: 11px; color: #e8c372; }
.c-desc { margin: 8px 0; font-size: 12px; line-height: 1.8; color: var(--muted); display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
.c-foot { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.c-price { font-size: 15px; color: #e8c372; }
.c-price small { font-size: 10px; color: var(--muted); }
.c-stock { font-size: 11px; color: var(--muted); }
.c-link { margin-left: auto; font: inherit; font-size: 11px; letter-spacing: 2px; color: #85b7eb; background: none; border: none; cursor: pointer; padding: 0; }
.v-empty, .v-msg { font-size: 12px; letter-spacing: 2px; color: var(--muted); text-align: center; padding: 12px 0; }
.v-msg { color: #e8c372; }
</style>
