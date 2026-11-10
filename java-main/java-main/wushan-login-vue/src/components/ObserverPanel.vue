<script setup>
/**
 * 观山台：全站数据总览大屏。
 * 纯 CSS 绘制图表，不引入任何图表库，保证零依赖。
 */
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { observerApi } from '../api/sect'
import { findElement } from '../constants/wuxing'
import { findGrade } from '../constants/codex'
import navBg from '../assets/nav-bg-2.jpg'

const emit = defineEmits(['to-home'])

const data = ref(null)
let timer = null

const cards = computed(() => {
  const d = data.value || {}
  return [
    { label: '当 前 在 线', value: d.online ?? 0, unit: '人' },
    { label: '山 友 总 数', value: d.userTotal ?? 0, unit: '人' },
    { label: '今 日 签 到', value: d.signInToday ?? 0, unit: '人' },
    { label: '今 日 成 交', value: d.auctionAmount ?? 0, unit: '灵石' }
  ]
})

const maxHot = computed(() => {
  const list = data.value?.hotSearch || []
  return list.length ? Math.max(...list.map((h) => h.count)) : 1
})

const elementDist = computed(() => data.value?.elementDist || [])
const gradeDist = computed(() => data.value?.gradeDist || [])
const signTrend = computed(() => data.value?.signTrend || [])
const maxElement = computed(() => Math.max(...elementDist.value.map((e) => e.count), 1))
const maxGrade = computed(() => Math.max(...gradeDist.value.map((g) => g.count), 1))
const maxSign = computed(() => Math.max(...signTrend.value.map((s) => s.count), 1))

/** 成交价走势：算成 SVG 折线点串 */
const priceTrend = computed(() => data.value?.priceTrend || [])
const pricePolyline = computed(() => {
  const list = priceTrend.value
  if (!list.length) return ''
  const max = Math.max(...list.map((p) => p.price || 0), 1)
  const min = Math.min(...list.map((p) => p.price || 0))
  const span = Math.max(1, max - min)
  const step = list.length > 1 ? 260 / (list.length - 1) : 0
  return list
    .map((p, i) => `${(20 + i * step).toFixed(1)},${(100 - (((p.price || 0) - min) / span) * 78).toFixed(1)}`)
    .join(' ')
})
const priceDots = computed(() => {
  const list = priceTrend.value
  if (!list.length) return []
  const max = Math.max(...list.map((p) => p.price || 0), 1)
  const min = Math.min(...list.map((p) => p.price || 0))
  const span = Math.max(1, max - min)
  const step = list.length > 1 ? 260 / (list.length - 1) : 0
  return list.map((p, i) => ({
    x: 20 + i * step,
    y: 100 - (((p.price || 0) - min) / span) * 78,
    title: p.title,
    price: p.price,
    roundNo: p.roundNo
  }))
})

async function load() {
  try {
    data.value = await observerApi.overview()
  } catch (e) {
    /* 大屏失败静默重试 */
  }
}

onMounted(() => {
  load()
  timer = setInterval(load, 20000)
})
onUnmounted(() => clearInterval(timer))
</script>

<template>
  <div class="obs">
    <div class="bg-photo" :style="{ backgroundImage: `url(${navBg})` }"></div>

    <header class="ob-head">
      <button class="back-btn" @click="emit('to-home')">‹ 回首页</button>
      <div class="head-brand">
        <div class="seal">观<br />山</div>
        <div class="brand-text">
          <span class="name">观山台</span>
          <small>全 山 门 动 态 · 五 秒 一 更</small>
        </div>
      </div>
      <button class="mini-btn" @click="load">刷 新</button>
    </header>

    <div class="ob-body">
      <!-- 指标卡 -->
      <div class="cards">
        <div v-for="c in cards" :key="c.label" class="card">
          <span class="c-label">{{ c.label }}</span>
          <strong class="c-value">{{ c.value }}</strong>
          <span class="c-unit">{{ c.unit }}</span>
        </div>
      </div>

      <div class="grid">
        <!-- 热门搜索 -->
        <section class="panel">
          <div class="p-title">热 门 搜 索</div>
          <div class="hot-list">
            <div v-for="(h, i) in data?.hotSearch || []" :key="i" class="hot">
              <span class="h-rank">{{ i + 1 }}</span>
              <span class="h-word">{{ h.keyword }}</span>
              <span class="h-bar"><i :style="{ width: (h.count / maxHot * 100) + '%' }"></i></span>
              <span class="h-count">{{ h.count }}</span>
            </div>
            <div v-if="!(data?.hotSearch || []).length" class="empty">尚 无 搜 索</div>
          </div>
        </section>

        <!-- 热议榜 -->
        <section class="panel">
          <div class="p-title">山 友 论 道</div>
          <div class="talk-list">
            <div v-for="(t, i) in data?.hotDiscourse || []" :key="i" class="talk">
              <span class="t-name">{{ t.name }}</span>
              <p class="t-quote">{{ t.quote }}</p>
              <span class="t-zan">赞 {{ t.zan }}</span>
            </div>
            <div v-if="!(data?.hotDiscourse || []).length" class="empty">论 道 台 尚 无 高 论</div>
          </div>
        </section>

        <!-- 大事记 -->
        <section class="panel">
          <div class="p-title">山 门 大 事 记</div>
          <div class="event-list">
            <div v-for="(e, i) in data?.events || []" :key="i" class="event">
              <span class="e-dot"></span>
              <span class="e-text">{{ e.text }}</span>
            </div>
            <div v-if="!(data?.events || []).length" class="empty">山 门 安 静</div>
          </div>
        </section>
      </div>

      <div class="foot-line">
        今日拍卖 {{ data?.auctionCount || 0 }} 件 · 成交 {{ data?.auctionSold || 0 }} 件 ·
        秘境探索累计 {{ data?.exploreTotal || 0 }} 次
      </div>

      <!-- 第二排：五行 / 品阶 / 成交走势 / 签到趋势 -->
      <div class="grid2">
        <section class="panel">
          <div class="p-title">五 行 藏 珍</div>
          <div class="bar-list">
            <div v-for="e in elementDist" :key="e.element" class="bar-row">
              <span class="b-name" :style="{ color: findElement(e.element).color }">{{ findElement(e.element).label }}</span>
              <span class="b-bar"><i :style="{ width: (e.count / maxElement * 100) + '%', background: findElement(e.element).color }"></i></span>
              <span class="b-count">{{ e.count }}</span>
            </div>
          </div>
        </section>

        <section class="panel">
          <div class="p-title">品 阶 分 布</div>
          <div class="bar-list">
            <div v-for="g in gradeDist" :key="g.grade" class="bar-row">
              <span class="b-name" :style="{ color: findGrade(g.grade).color }">{{ findGrade(g.grade).label }}</span>
              <span class="b-bar"><i :style="{ width: (g.count / maxGrade * 100) + '%', background: findGrade(g.grade).color }"></i></span>
              <span class="b-count">{{ g.count }}</span>
            </div>
          </div>
        </section>

        <section class="panel">
          <div class="p-title">近 场 成 交 价</div>
          <svg v-if="pricePolyline" class="trend" viewBox="0 0 300 120" preserveAspectRatio="none">
            <polyline :points="pricePolyline" fill="none" stroke="#e8412b" stroke-width="1.6" />
            <circle v-for="(d, i) in priceDots" :key="i" :cx="d.x" :cy="d.y" r="3" fill="#e8c372">
              <title>{{ d.title }} · {{ d.price }} 灵石</title>
            </circle>
          </svg>
          <div v-else class="empty">尚 无 成 交 记 录</div>
          <div class="trend-legend">
            <span v-for="d in priceDots.slice(-3)" :key="d.roundNo">第{{ d.roundNo }}场 {{ d.price }}</span>
          </div>
        </section>

        <section class="panel">
          <div class="p-title">近 七 日 签 到</div>
          <div class="sign-cols">
            <div v-for="s in signTrend" :key="s.date" class="sign-col">
              <span class="s-count">{{ s.count }}</span>
              <span class="s-bar"><i :style="{ height: (s.count / maxSign * 100) + '%' }"></i></span>
              <span class="s-date">{{ s.date.slice(5) }}</span>
            </div>
          </div>
        </section>
      </div>
    </div>
  </div>
</template>

<style scoped>
.obs { position: relative; height: 100vh; overflow: hidden; color: var(--paper); font-family: "Kaiti SC", "STKaiti", "KaiTi", "楷体", serif; }
.bg-photo { position: fixed; inset: 0; z-index: 0; background-size: cover; background-position: center; }
.bg-photo::after {
  content: ''; position: absolute; inset: 0;
  background: radial-gradient(120% 90% at 50% 40%, rgba(10, 10, 9, 0) 30%, rgba(10, 10, 9, 0.68) 100%),
              linear-gradient(180deg, rgba(10, 10, 9, 0.75) 0%, rgba(10, 10, 9, 0.6) 50%, rgba(10, 10, 9, 0.88) 100%);
}
.ob-head {
  position: relative; z-index: 2; height: 92px; display: flex; align-items: center; gap: 22px; padding: 0 30px;
  border-bottom: 1px solid var(--line); background: rgba(12, 16, 15, 0.45);
}
.back-btn { flex: none; padding: 8px 16px; color: var(--paper); font: inherit; font-size: 15px; letter-spacing: 2px; background: rgba(236, 228, 210, 0.06); border: 1px solid var(--line); border-radius: 2px; cursor: pointer; }
.head-brand { display: flex; align-items: center; gap: 14px; }
.seal { width: 44px; height: 44px; display: grid; place-content: center; font-size: 15px; line-height: 1.05; text-align: center; color: #f6ece0; background: #a3311f; border: 1px solid #c8543c; }
.brand-text { display: flex; flex-direction: column; gap: 3px; }
.brand-text .name { font-size: 26px; letter-spacing: 8px; }
.brand-text small { font-size: 11px; letter-spacing: 3px; color: var(--muted); }
.mini-btn { margin-left: auto; font: inherit; font-size: 14px; letter-spacing: 3px; padding: 9px 18px; color: var(--paper); background: rgba(236, 228, 210, 0.08); border: 1px solid var(--line); border-radius: 2px; cursor: pointer; }

.ob-body { position: relative; z-index: 2; height: calc(100vh - 92px); overflow-y: auto; padding: 20px 26px 30px; }
.cards { display: grid; grid-template-columns: repeat(4, 1fr); gap: 14px; margin-bottom: 18px; }
.card {
  display: flex; flex-direction: column; align-items: center; gap: 6px; padding: 18px 10px;
  border: 1px solid var(--line); background: rgba(12, 17, 16, 0.6);
}
.c-label { font-size: 12px; letter-spacing: 4px; color: var(--muted); }
.c-value { font-size: 38px; font-weight: normal; letter-spacing: 2px; color: #e8c372; }
.c-unit { font-size: 11px; color: var(--muted); }

.grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 14px; }
.panel { padding: 16px 18px; border: 1px solid var(--line); background: rgba(12, 17, 16, 0.58); min-height: 320px; }
.p-title { font-size: 14px; letter-spacing: 5px; color: #e8c372; padding-bottom: 12px; margin-bottom: 12px; border-bottom: 1px solid var(--line); }

.hot-list, .talk-list, .event-list { display: flex; flex-direction: column; gap: 9px; }
.hot { display: flex; align-items: center; gap: 9px; font-size: 13px; letter-spacing: 1px; }
.h-rank { width: 20px; color: var(--muted); }
.h-word { width: 84px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.h-bar { flex: 1; height: 8px; background: rgba(236, 228, 210, 0.08); }
.h-bar i { display: block; height: 100%; background: linear-gradient(90deg, #8a2f1e, #e8412b); }
.h-count { width: 32px; text-align: right; color: var(--muted); font-size: 12px; }

.talk { padding: 10px 12px; border-left: 2px solid #e8c372; background: rgba(236, 228, 210, 0.04); }
.t-name { font-size: 13px; letter-spacing: 2px; }
.t-quote { font-size: 12px; line-height: 1.7; color: rgba(236, 228, 210, 0.75); margin: 6px 0; }
.t-zan { font-size: 11px; color: var(--muted); }

.event { display: flex; gap: 9px; font-size: 12px; line-height: 1.8; color: rgba(236, 228, 210, 0.82); }
.e-dot { flex: none; width: 6px; height: 6px; margin-top: 9px; border-radius: 50%; background: #e8412b; }
.empty { font-size: 12px; letter-spacing: 2px; color: var(--muted); text-align: center; padding: 24px 0; }
.foot-line { margin-top: 16px; font-size: 12px; letter-spacing: 2px; color: var(--muted); text-align: center; }
.grid2 { display: grid; grid-template-columns: repeat(4, 1fr); gap: 14px; margin-top: 14px; }
.bar-list { display: flex; flex-direction: column; gap: 7px; }
.bar-row { display: flex; align-items: center; gap: 8px; font-size: 12px; letter-spacing: 1px; }
.b-name { width: 34px; }
.b-bar { flex: 1; height: 9px; background: rgba(236, 228, 210, 0.07); }
.b-bar i { display: block; height: 100%; transition: width 0.4s; }
.b-count { width: 30px; text-align: right; color: var(--muted); }
.trend { width: 100%; height: 110px; }
.trend-legend { display: flex; gap: 10px; justify-content: center; margin-top: 6px; font-size: 11px; color: var(--muted); }
.sign-cols { display: flex; align-items: flex-end; gap: 10px; height: 118px; }
.sign-col { flex: 1; display: flex; flex-direction: column; align-items: center; gap: 4px; height: 100%; justify-content: flex-end; }
.s-count { font-size: 11px; color: #e8c372; }
.s-bar { width: 100%; flex: 1; display: flex; align-items: flex-end; background: rgba(236, 228, 210, 0.05); }
.s-bar i { display: block; width: 100%; background: linear-gradient(180deg, #e8412b, #8a2f1e); min-height: 2px; transition: height 0.4s; }
.s-date { font-size: 10px; color: var(--muted); }

@media (max-width: 1100px) {
  .cards { grid-template-columns: repeat(2, 1fr); }
  .grid { grid-template-columns: 1fr; }
}
</style>
