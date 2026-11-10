<script setup>
/**
 * 拍卖行（服务端驱动版）
 * 每 10 分钟一场、提前 1 分钟通知在线用户、每场 5 件按品级由低到高、
 * 起拍 100 灵石、加价 10/50/100/500、出价后 5 秒倒计时落槌归最高出价者。
 *
 * 所有状态（倒计时 / 当前价 / 最高出价人 / 场次）都来自 /api/auction/status，
 * 前端每 2 秒轮询一次，保证所有在线用户看到的拍场完全一致。
 */
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { auctionApi, AUCTION_RULES } from '../api/auction'
import { findGrade } from '../constants/codex'
import { findElement } from '../constants/wuxing'
import navBg from '../assets/nav-bg-2.jpg'

const emit = defineEmits(['to-home'])

const s = ref(null) // 服务端状态快照
const error = ref('')
const flashMsg = ref('')
const selected = ref(null) // 拍品属性弹层
const detailLoading = ref(false)
const bidding = ref(false)
const historyList = ref([]) // 往场成交
const watched = ref(false) // 当前拍品是否已关注
const myWatches = ref([]) // 我关注的拍品
const myBids = ref([]) // 我的竞拍记录
let pollTimer = null
let historyTimer = null
let mineTimer = null
let flashTimer = null

const rules = computed(() => s.value?.rules || AUCTION_RULES)
const items = computed(() => s.value?.items || [])
const current = computed(() => s.value?.current || null)
const phase = computed(() => s.value?.phase || 'waiting')
const logs = computed(() => s.value?.logs || [])
const loggedIn = computed(() => !!s.value?.loggedIn)
const index = computed(() => s.value?.index || 0)
const canBid = computed(() => phase.value === 'bidding' && loggedIn.value && !bidding.value)

const phaseText = computed(() => {
  if (phase.value === 'bidding') return '竞 价 中'
  if (phase.value === 'gap') return '一 槌 定 音'
  return '候 场 中'
})
const lotNo = (i) => String(i + 1).padStart(2, '0')
const gradeLabel = (it) => it?.gradeLabel || findGrade(it?.grade).label
const gradeColor = (it) => findGrade(it?.grade).color
const elementInfo = (it) => findElement(it?.element)

function flash(msg) {
  flashMsg.value = msg
  clearTimeout(flashTimer)
  flashTimer = setTimeout(() => { flashMsg.value = '' }, 2400)
}

async function refresh() {
  try {
    s.value = await auctionApi.getStatus()
    error.value = ''
  } catch (e) {
    error.value = e.message || '连接拍卖行失败'
  }
}

async function bid(inc) {
  if (!canBid.value) return
  bidding.value = true
  try {
    const r = await auctionApi.bid(inc)
    flash(`${r.bidder} 加价 ${r.increment}，当前 ${r.price} 灵石`)
    await refresh()
  } catch (e) {
    flash(e.message || '出价未成，请重试')
  } finally {
    bidding.value = false
  }
}

async function openDetail(it) {
  selected.value = { ...it }
  detailLoading.value = true
  try {
    const d = await auctionApi.getItem(it.id)
    selected.value = { ...it, ...d }
  } catch (e) {
    flash(e.message || '属性加载失败')
  } finally {
    detailLoading.value = false
  }
}

async function loadHistory() {
  try {
    historyList.value = await auctionApi.history(20)
  } catch (e) { /* 往场记录失败不影响竞价 */ }
}

/** 我的关注与我的竞拍（未登录时静默跳过） */
async function loadMine() {
  try {
    const [w, b] = await Promise.all([
      auctionApi.getWatches().catch(() => []),
      auctionApi.getMyBids().catch(() => [])
    ])
    myWatches.value = w || []
    myBids.value = b || []
  } catch (e) { /* 个人信息失败不影响竞价 */ }
}

async function doWatch() {
  if (!current.value || !loggedIn.value) return
  try {
    const r = await auctionApi.watch(current.value.title)
    watched.value = true
    flash(r.already ? '你早已关注过此物' : '已关注，此物再上拍时提醒你')
  } catch (e) {
    flash(e.message || '关注失败')
  }
}

// 换拍品时重置关注状态
watch(() => current.value?.id, () => { watched.value = false })

onMounted(() => {
  refresh()
  loadHistory()
  loadMine()
  pollTimer = setInterval(refresh, 2000)
  historyTimer = setInterval(loadHistory, 15000)
  mineTimer = setInterval(loadMine, 20000)
})
onUnmounted(() => {
  clearInterval(pollTimer)
  clearInterval(historyTimer)
  clearInterval(mineTimer)
  clearTimeout(flashTimer)
})
</script>

<template>
  <div class="auction">
    <div class="bg-photo" :style="{ backgroundImage: `url(${navBg})` }"></div>

    <!-- ============ 页头 ============ -->
    <header class="au-head">
      <button class="back-btn" @click="emit('to-home')">‹ 回首页</button>
      <div class="head-brand">
        <div class="seal">拍<br />卖</div>
        <div class="brand-text">
          <span class="name">拍卖行</span>
          <small>每 十 分 钟 一 场 · 五 秒 落 槌</small>
        </div>
      </div>
      <div class="head-stat">
        第 {{ s?.roundNo || 1 }} 场 ·
        <span class="online">在线 {{ s?.onlineCount || 0 }} 人</span> ·
        <span :class="{ 'not-login': !loggedIn }">{{ loggedIn ? (s?.me || '山友') : '未入境' }}</span>
      </div>
    </header>

    <!-- ============ 开拍通知（面板内） ============ -->
    <Transition name="fade">
      <div v-if="s?.notify" class="round-notice">{{ s?.noticeText }}</div>
    </Transition>

    <!-- ============ 主体三栏 ============ -->
    <div class="au-body">
      <!-- 左：本场拍品 -->
      <aside class="rail">
        <div class="rail-title">本 场 拍 品 <small>{{ items.length }} 件</small></div>
        <div
          v-for="(it, i) in items"
          :key="it.id"
          class="lot"
          :class="{ 'is-on': i === index && phase !== 'waiting', 'is-done': i < index && phase !== 'waiting' }"
          @click="openDetail(it)"
        >
          <span class="lot-no">{{ lotNo(i) }}</span>
          <span class="lot-name">{{ it.title }}</span>
          <span class="lot-grade" :style="{ color: gradeColor(it) }">{{ gradeLabel(it) }}</span>
          <span class="lot-state" v-if="i < index && phase !== 'waiting'">已毕</span>
          <span class="lot-state live" v-else-if="i === index && phase === 'bidding'">在拍</span>
          <span class="lot-state" v-else-if="i === index && phase === 'gap'">落槌</span>
        </div>
        <p class="rail-tip">点 击 拍 品 查 看 属 性</p>
        <p class="rail-tip" v-if="!items.length">候 场 中 · 开 拍 后 显 示</p>
      </aside>

      <!-- 中：当前拍品 -->
      <section class="stage">
        <div class="stage-top">
          <span class="stage-tag">{{ phaseText }}</span>
          <span class="stage-count" v-if="items.length">第 {{ index + 1 }} / {{ items.length }} 件</span>
          <span class="stage-count" v-if="phase === 'waiting'">
            下 一 场 {{ s?.secondsToNextRound || 0 }} 秒 后 开 拍
          </span>
        </div>

        <template v-if="current && phase !== 'waiting'">
          <div class="lot-hero">
            <div class="hero-badges">
              <span class="el-chip" :style="{ color: elementInfo(current).color, borderColor: elementInfo(current).color }">
                {{ elementInfo(current).label }}行
              </span>
              <span class="grade-badge" :style="{ color: gradeColor(current), borderColor: gradeColor(current) }">
                {{ gradeLabel(current) }}
              </span>
              <span class="cat-chip">{{ current.categoryName }}</span>
            </div>
            <h2 class="hero-title">{{ current.title }}</h2>
            <p class="hero-desc">{{ current.desc }}</p>
            <button class="watch-btn" :disabled="!loggedIn || watched" @click="doWatch">
              {{ watched ? '已 关 注 此 物' : '关 注 此 物 · 捡 漏 提 醒' }}
            </button>
          </div>

          <div class="price-row">
            <div class="price-box">
              <small>当 前 价</small>
              <strong>{{ s?.price || rules.startPrice }}</strong>
              <span>灵石</span>
            </div>
            <div class="leader-box">
              <small>最 高 出 价</small>
              <strong>{{ s?.leader || '尚 无 人 出 价' }}</strong>
            </div>
            <div class="clock-box" :class="{ urgent: (s?.countdown || 0) <= 2 && phase === 'bidding' }">
              <small>落 槌 倒 计 时</small>
              <strong>{{ s?.countdown || 0 }}</strong>
              <span>秒</span>
            </div>
          </div>

          <div class="bid-area" v-if="phase === 'bidding'">
            <div class="bid-label">加 价</div>
            <button
              v-for="inc in rules.increments"
              :key="inc"
              class="bid-btn"
              :disabled="!canBid"
              @click="bid(inc)"
            >+{{ inc }}</button>
          </div>
          <div class="stage-msg" v-else-if="phase === 'gap'">
            一 槌 定 音 · 稍 候 上 拍 下 一 件
          </div>
          <div class="login-tip" v-if="!loggedIn">请 先 入 境（登 录）后 方 可 参 与 竞 价</div>
        </template>

        <div class="waiting" v-else>
          <p class="waiting-title">山 门 未 启</p>
          <p>下 一 场 拍 卖 将 在 {{ s?.secondsToNextRound || 0 }} 秒 后 开 拍</p>
          <p class="waiting-sub">开 拍 前 一 分 钟 会 通 知 所 有 在 线 山 友</p>
        </div>

        <div class="result-line" v-if="s?.lastResult">{{ s.lastResult }}</div>

        <div class="rules">
          起拍 {{ rules.startPrice }} 灵石 ·
          加价 {{ rules.increments.join(' / ') }} ·
          每次出价重置 {{ rules.countdownSeconds }} 秒 ·
          每 {{ Math.round(rules.roundIntervalSeconds / 60) }} 分钟一场 ·
          拍品取未拍卖的功法 / 法宝 / 丹药，按品级由低到高
        </div>
      </section>

      <!-- 右：出价流水 -->
      <aside class="log">
        <div class="rail-title">拍 场 实 录</div>
        <div class="log-list">
          <div v-for="(l, i) in logs" :key="i" class="log-item">{{ l.text }}</div>
          <div v-if="!logs.length" class="log-empty">本 场 尚 无 出 价</div>
        </div>

        <!-- 往场成交 -->
        <div class="rail-title" style="margin-top: 18px">往 场 成 交</div>
        <div class="history-list">
          <div v-for="(h, i) in historyList" :key="i" class="history-item">
            <span class="h-title">{{ h.itemTitle }}</span>
            <span class="h-price">{{ h.finalPrice }} 灵石</span>
            <span class="h-winner">{{ h.winnerName }}</span>
          </div>
          <div v-if="!historyList.length" class="log-empty">尚 无 往 场</div>
        </div>

        <!-- 我的关注 -->
        <div class="rail-title" style="margin-top: 18px">我 的 关 注</div>
        <div class="history-list">
          <div v-for="(w, i) in myWatches" :key="i" class="history-item">
            <span class="h-title">{{ w.itemTitle }}</span>
            <span class="h-winner">再上拍时提醒</span>
          </div>
          <div v-if="!myWatches.length" class="log-empty">尚 未 关 注 拍 品</div>
        </div>

        <!-- 我的竞拍 -->
        <div class="rail-title" style="margin-top: 18px">我 的 竞 拍</div>
        <div class="history-list">
          <div v-for="(b, i) in myBids" :key="i" class="history-item" :class="{ 'is-win': b.type === 'win' }">
            <span class="h-title">{{ b.itemTitle }}</span>
            <span class="h-price">{{ b.price }} 灵石</span>
            <span class="h-winner">{{ b.type === 'win' ? '已 拍 得' : '出 价' }}</span>
          </div>
          <div v-if="!myBids.length" class="log-empty">尚 未 出 价</div>
        </div>
      </aside>
    </div>

    <Transition name="fade">
      <div v-if="flashMsg" class="notice">{{ flashMsg }}</div>
    </Transition>
    <div v-if="error" class="err-line">{{ error }}</div>

    <!-- ============ 拍品属性弹层 ============ -->
    <div v-if="selected" class="modal" @click.self="selected = null">
      <div class="modal-card">
        <button class="modal-close" @click="selected = null">×</button>
        <div class="modal-head">
          <span class="grade-badge" :style="{ color: gradeColor(selected), borderColor: gradeColor(selected) }">
            {{ gradeLabel(selected) }}
          </span>
          <h3>{{ selected.title }}</h3>
        </div>
        <p class="modal-desc">{{ selected.desc }}</p>
        <dl class="modal-attrs">
          <div><dt>本 命</dt><dd :style="{ color: elementInfo(selected).color }">{{ elementInfo(selected).label }}行</dd></div>
          <div><dt>品 阶</dt><dd>{{ gradeLabel(selected) }}</dd></div>
          <div><dt>门 类</dt><dd>{{ selected.categoryName }}</dd></div>
          <div><dt>起 拍</dt><dd>{{ selected.startPrice || rules.startPrice }} 灵石</dd></div>
        </dl>
        <p class="modal-foot">{{ detailLoading ? '正 在 取 回 属 性…' : `拍 卖 状 态：${selected.auction === 1 ? '已 拍 出' : '未 拍 卖'}` }}</p>
      </div>
    </div>
  </div>
</template>

<style scoped>
.auction {
  position: relative;
  height: 100vh;
  overflow: hidden;
  color: var(--paper);
  font-family: "Kaiti SC", "STKaiti", "KaiTi", "楷体", "Songti SC", "SimSun", serif;
}
.bg-photo {
  position: fixed;
  inset: 0;
  z-index: 0;
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
}
.bg-photo::after {
  content: '';
  position: absolute;
  inset: 0;
  background:
    radial-gradient(120% 90% at 50% 40%, rgba(10, 10, 9, 0) 35%, rgba(10, 10, 9, 0.62) 100%),
    linear-gradient(180deg, rgba(10, 10, 9, 0.7) 0%, rgba(10, 10, 9, 0.55) 50%, rgba(10, 10, 9, 0.82) 100%);
}

/* 页头 */
.au-head {
  position: relative;
  z-index: 2;
  height: 92px;
  display: flex;
  align-items: center;
  gap: 22px;
  padding: 0 30px;
  border-bottom: 1px solid var(--line);
  background: rgba(12, 16, 15, 0.42);
}
.back-btn {
  flex: none;
  padding: 8px 16px;
  color: var(--paper);
  font: inherit;
  font-size: 15px;
  letter-spacing: 2px;
  background: rgba(236, 228, 210, 0.06);
  border: 1px solid var(--line);
  border-radius: 2px;
  cursor: pointer;
}
.back-btn:hover { background: rgba(236, 228, 210, 0.14); }
.head-brand { display: flex; align-items: center; gap: 14px; }
.seal {
  width: 44px; height: 44px;
  display: grid; place-content: center;
  font-size: 15px; line-height: 1.05; text-align: center;
  color: #f6ece0; background: #a3311f; border: 1px solid #c8543c;
  box-shadow: 0 0 14px rgba(163, 49, 31, 0.45);
}
.brand-text { display: flex; flex-direction: column; gap: 3px; }
.brand-text .name { font-size: 26px; letter-spacing: 8px; }
.brand-text small { font-size: 11px; letter-spacing: 3px; color: var(--muted); }
.head-stat { margin-left: auto; font-size: 14px; letter-spacing: 2px; color: #e8c372; }
.online { color: var(--paper); }
.not-login { color: var(--muted); }

/* 开拍通知 */
.round-notice {
  position: relative; z-index: 3;
  margin: 10px 22px 0; padding: 9px 18px;
  font-size: 14px; letter-spacing: 2px;
  color: #f6ece0; background: rgba(163, 49, 31, 0.9);
  border: 1px solid #c8543c;
}

/* 三栏 */
.au-body {
  position: relative; z-index: 2;
  height: calc(100vh - 92px);
  display: grid;
  grid-template-columns: 232px minmax(0, 1fr) 258px;
  gap: 16px;
  padding: 18px 22px 34px;
}
.rail, .log, .stage {
  min-height: 0;
  background: rgba(12, 17, 16, 0.58);
  border: 1px solid var(--line);
  backdrop-filter: blur(4px);
}
.rail, .log { padding: 16px 12px; overflow-y: auto; }
.rail-title {
  padding-bottom: 12px; margin-bottom: 10px;
  font-size: 14px; letter-spacing: 4px; color: #e8c372;
  border-bottom: 1px solid var(--line);
}
.rail-title small { float: right; font-size: 11px; letter-spacing: 1px; color: var(--muted); }
.lot {
  display: grid; grid-template-columns: 26px 1fr auto; align-items: center; gap: 6px;
  padding: 10px 8px; margin-bottom: 6px;
  border: 1px solid transparent; border-left: 2px solid rgba(236, 228, 210, 0.16);
  cursor: pointer;
}
.lot:hover { background: rgba(236, 228, 210, 0.07); }
.lot.is-on { background: rgba(232, 65, 43, 0.12); border-left-color: #e8412b; }
.lot.is-done { opacity: 0.42; }
.lot-no { font-size: 12px; color: var(--muted); }
.lot-name { font-size: 15px; letter-spacing: 1px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.lot-grade { font-size: 11px; }
.lot-state { grid-column: 3; font-size: 10px; color: var(--muted); }
.lot-state.live { color: #e8412b; }
.rail-tip { margin-top: 14px; font-size: 11px; letter-spacing: 2px; color: var(--muted); text-align: center; }

/* 中栏 */
.stage { display: flex; flex-direction: column; padding: 20px 26px; overflow-y: auto; }
.stage-top { display: flex; align-items: center; justify-content: space-between; font-size: 12px; letter-spacing: 3px; color: var(--muted); }
.stage-tag { color: #e8412b; }
.lot-hero { padding: 26px 0 20px; border-bottom: 1px solid var(--line); }
.hero-badges { display: flex; gap: 10px; margin-bottom: 14px; }
.el-chip, .grade-badge, .cat-chip {
  display: inline-block; padding: 3px 10px;
  font-size: 12px; letter-spacing: 2px; border: 1px solid; border-radius: 2px;
}
.cat-chip { border-color: var(--line); color: var(--muted); }
.hero-title { font-size: 38px; font-weight: normal; letter-spacing: 8px; margin-bottom: 12px; }
.hero-desc { font-size: 15px; line-height: 1.9; letter-spacing: 2px; color: rgba(236, 228, 210, 0.78); }

.price-row { display: grid; grid-template-columns: repeat(3, 1fr); gap: 14px; padding: 22px 0; }
.price-row > div {
  display: flex; flex-direction: column; align-items: center; gap: 6px;
  padding: 14px 8px; border: 1px solid var(--line); background: rgba(236, 228, 210, 0.04);
}
.price-row small { font-size: 11px; letter-spacing: 3px; color: var(--muted); }
.price-row strong { font-size: 30px; font-weight: normal; letter-spacing: 2px; }
.price-row span { font-size: 11px; color: var(--muted); }
.price-box strong { color: #e8c372; font-size: 40px; }
.leader-box strong { font-size: 19px; letter-spacing: 3px; }
.clock-box strong { color: #e8412b; }
.clock-box.urgent { border-color: rgba(232, 65, 43, 0.7); box-shadow: 0 0 16px rgba(232, 65, 43, 0.28); }

.bid-area { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; padding: 6px 0 20px; }
.bid-label { font-size: 13px; letter-spacing: 4px; color: var(--muted); margin-right: 6px; }
.bid-btn {
  min-width: 92px; padding: 11px 18px;
  font: inherit; font-size: 17px; letter-spacing: 2px;
  color: var(--paper);
  background: linear-gradient(180deg, rgba(163, 49, 31, 0.9), rgba(120, 33, 21, 0.9));
  border: 1px solid #c8543c; border-radius: 2px; cursor: pointer;
}
.bid-btn:hover:not(:disabled) { transform: translateY(-2px); filter: brightness(1.2); }
.bid-btn:disabled { opacity: 0.35; cursor: not-allowed; }
.stage-msg { padding: 12px 0 22px; font-size: 16px; letter-spacing: 4px; color: #e8c372; }
.login-tip { padding-bottom: 16px; font-size: 13px; letter-spacing: 2px; color: #e8c372; }
.waiting { flex: 1; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 14px; text-align: center; font-size: 16px; letter-spacing: 4px; color: rgba(236, 228, 210, 0.8); }
.waiting-title { font-size: 34px; letter-spacing: 12px; color: #e8c372; }
.waiting-sub { font-size: 12px; letter-spacing: 2px; color: var(--muted); }
.result-line { padding: 10px 14px; margin-bottom: 12px; font-size: 14px; letter-spacing: 2px; color: #f0c9b8; border-left: 2px solid #e8412b; background: rgba(232, 65, 43, 0.08); }
.rules { margin-top: auto; padding-top: 14px; border-top: 1px solid var(--line); font-size: 11px; line-height: 1.8; letter-spacing: 1px; color: var(--muted); }

/* 右栏 */
.log-list { display: flex; flex-direction: column; gap: 8px; }
.log-item {
  padding: 9px 10px; font-size: 12px; line-height: 1.6; letter-spacing: 1px;
  color: rgba(236, 228, 210, 0.82);
  border-left: 2px solid #e8c372; background: rgba(236, 228, 210, 0.04);
}
.log-empty { font-size: 12px; letter-spacing: 2px; color: var(--muted); text-align: center; padding: 20px 0; }
.history-list { display: flex; flex-direction: column; gap: 7px; }
.history-item {
  display: flex; flex-direction: column; gap: 3px;
  padding: 8px 10px; font-size: 12px; letter-spacing: 1px;
  border-left: 2px solid rgba(232, 195, 114, 0.5); background: rgba(236, 228, 210, 0.04);
}
.h-title { color: rgba(236, 228, 210, 0.9); }
.h-price { color: #e8c372; }
.h-winner { color: var(--muted); }
.history-item.is-win { border-left-color: #e8c372; }
.history-item.is-win .h-title { color: #e8c372; }
.watch-btn {
  margin-top: 16px; padding: 9px 20px;
  font: inherit; font-size: 13px; letter-spacing: 3px; cursor: pointer;
  color: var(--paper); background: rgba(236, 228, 210, 0.07); border: 1px solid var(--line); border-radius: 2px;
}
.watch-btn:hover:not(:disabled) { background: rgba(236, 228, 210, 0.16); }
.watch-btn:disabled { opacity: 0.5; cursor: not-allowed; }

/* 其他 */
.notice {
  position: fixed; left: 50%; bottom: 60px; transform: translateX(-50%); z-index: 9;
  padding: 10px 26px; font-size: 14px; letter-spacing: 2px;
  color: var(--paper); background: rgba(163, 49, 31, 0.92); border: 1px solid #c8543c;
}
.err-line {
  position: fixed; left: 50%; bottom: 24px; transform: translateX(-50%); z-index: 9;
  font-size: 12px; letter-spacing: 1px; color: #f0a89a;
}
.fade-enter-active, .fade-leave-active { transition: opacity 0.25s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }

.modal { position: fixed; inset: 0; z-index: 10; display: grid; place-items: center; background: rgba(6, 9, 8, 0.72); }
.modal-card {
  position: relative; width: min(460px, 88vw); padding: 30px 34px 26px;
  background: linear-gradient(160deg, #1c2421, #101614);
  border: 1px solid rgba(236, 228, 210, 0.32); box-shadow: 0 20px 60px rgba(0, 0, 0, 0.6);
}
.modal-close { position: absolute; top: 10px; right: 14px; font-size: 26px; color: var(--muted); background: none; border: none; cursor: pointer; }
.modal-head { display: flex; align-items: center; gap: 14px; margin-bottom: 18px; }
.modal-head h3 { font-size: 26px; font-weight: normal; letter-spacing: 5px; }
.modal-desc { font-size: 14px; line-height: 2; letter-spacing: 1px; color: rgba(236, 228, 210, 0.8); margin-bottom: 22px; }
.modal-attrs { display: grid; grid-template-columns: 1fr 1fr; gap: 14px; }
.modal-attrs > div { padding: 10px 12px; border: 1px solid var(--line); background: rgba(236, 228, 210, 0.04); }
.modal-attrs dt { font-size: 11px; letter-spacing: 3px; color: var(--muted); margin-bottom: 6px; }
.modal-attrs dd { font-size: 16px; letter-spacing: 2px; }
.modal-foot { margin-top: 18px; font-size: 12px; letter-spacing: 2px; color: var(--muted); }

@media (max-width: 1080px) {
  .au-body { grid-template-columns: 180px minmax(0, 1fr); }
  .log { display: none; }
  .hero-title { font-size: 32px; }
}
@media (max-width: 720px) {
  .au-body { grid-template-columns: 1fr; overflow-y: auto; }
  .rail { display: none; }
  .price-row { grid-template-columns: 1fr; }
}
</style>
