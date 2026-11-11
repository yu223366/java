<script setup>
/**
 * 修行洞府：测灵根 · 每日签到 · 每日一题 · 灵石钱包
 * 三件事都直接补给玩家资源：灵石（签到 / 答题）、身份（灵根）、每日回访理由（一题）。
 */
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { gameApi } from '../api/game'
import { findElement } from '../constants/wuxing'
import { findGrade, SPIRIT_ROOTS } from '../constants/codex'
import ShareCard from './ShareCard.vue'
import navBg from '../assets/nav-bg-3.jpg'

const emit = defineEmits(['to-home'])

const tab = ref('spirit')
const showShare = ref(false)
const wallet = ref({ balance: 0, logs: [] })
const err = ref('')
const msg = ref('')

/* 测灵根 */
const quiz = ref(null)
const qi = ref(0)
const picks = ref([])
const mySpirit = ref(null)
const spiritResult = ref(null)
const submitting = ref(false)

/* 每日签到 */
const sign = ref(null)
const signing = ref(false)

/* 每日一题 */
const daily = ref(null)
const dailyPick = ref(-1)
const dailyDone = ref(null)
const answering = ref(false)

const TABS = [
  { key: 'spirit', label: '测灵根', en: 'spirit root' },
  { key: 'signin', label: '每日签到', en: 'sign in' },
  { key: 'daily', label: '每旬一题', en: '10 min quiz' },
  { key: 'practice', label: '练功', en: 'practice' }
]

/* 练功：每 30 秒一次；每旬一题：10 分钟一轮 */
const practice = ref(null)
const practiceCd = ref(0)
const slotCd = ref(0)
let tickTimer = null
const SIGN_REWARD = [10, 20, 30, 40, 50, 60, 100]
const question = computed(() => (quiz.value?.questions || [])[qi.value] || null)
const picked = computed(() => (picks.value[qi.value] === undefined ? -1 : picks.value[qi.value]))
const elementInfo = computed(() => findElement(spiritResult.value?.element || mySpirit.value?.element))
const currentSpirit = computed(() => (mySpirit.value?.hasProfile ? mySpirit.value : spiritResult.value))

function flash(m) {
  msg.value = m
  setTimeout(() => { msg.value = '' }, 2400)
}

async function loadWallet() {
  try {
    wallet.value = await gameApi.getWallet()
  } catch (e) { /* 钱包失败不阻塞其他玩法 */ }
}

async function loadSpirit() {
  try {
    mySpirit.value = await gameApi.getMySpirit()
    if (!quiz.value) quiz.value = await gameApi.getSpiritQuiz()
  } catch (e) {
    err.value = e.message || '灵根谱读取失败'
  }
}

function startQuiz() {
  // 首测免费，重测要 100 灵石；余额不足就别让山友白答八题
  if (mySpirit.value?.hasProfile && (wallet.value.balance || 0) < (mySpirit.value.retestCost || 100)) {
    return flash('灵石不足，重测需 ' + (mySpirit.value.retestCost || 100) + ' 灵石')
  }
  picks.value = []
  qi.value = 0
  spiritResult.value = null
  err.value = ''
}

function choose(i) {
  picks.value[qi.value] = i
}

async function nextQuestion() {
  if (picked.value < 0) {
    flash('请先选择一个答案')
    return
  }
  if (qi.value < (quiz.value?.questions?.length || 0) - 1) {
    qi.value += 1
    return
  }
  submitting.value = true
  try {
    spiritResult.value = await gameApi.submitSpirit(picks.value)
    mySpirit.value = { hasProfile: true, ...spiritResult.value }
    flash('灵根鉴定完成')
    await Promise.all([loadSpirit(), loadWallet()])
  } catch (e) {
    err.value = e.message || '鉴定失败'
  } finally {
    submitting.value = false
  }
}

async function loadSign() {
  try {
    sign.value = await gameApi.getSignInStatus()
  } catch (e) { err.value = e.message || '签到状态读取失败' }
}

async function doSign() {
  signing.value = true
  try {
    const r = await gameApi.checkIn()
    flash(`签到成功，得 ${r.reward} 灵石`)
    await Promise.all([loadSign(), loadWallet()])
  } catch (e) {
    flash(e.message || '签到失败')
  } finally {
    signing.value = false
  }
}

async function loadDaily() {
  try {
    daily.value = await gameApi.getDailyQuestion()
    slotCd.value = daily.value?.nextSlotSeconds || 600
    dailyDone.value = null
    dailyPick.value = -1
  } catch (e) {
    err.value = e.message || '本轮题目读取失败'
  }
}

async function answerDaily(idx) {
  if (dailyPick.value >= 0 || daily.value?.answered) return
  dailyPick.value = idx
  answering.value = true
  try {
    dailyDone.value = await gameApi.answerDaily(idx)
    flash(dailyDone.value.correct
      ? `答对了，得 ${dailyDone.value.reward} 灵石 · 下一轮 ${formatCd(dailyDone.value.nextSlotSeconds || slotCd.value)} 后开启`
      : `答错了，下一轮 ${formatCd(dailyDone.value.nextSlotSeconds || slotCd.value)} 后再来`)
    // 保留本轮对错反馈，只切换已答状态；倒计时交给本地 tick
    if (daily.value) {
      daily.value.answered = true
      daily.value.correct = dailyDone.value.correct
    }
    slotCd.value = dailyDone.value.nextSlotSeconds || slotCd.value
    await loadWallet()
  } catch (e) {
    flash(e.message || '提交失败')
    dailyPick.value = -1
  } finally {
    answering.value = false
  }
}

/* ------------------------------ 练功 ------------------------------ */
async function loadPractice() {
  try {
    practice.value = await gameApi.getPracticeStatus()
    practiceCd.value = practice.value?.cooldownSeconds || 0
  } catch (e) { /* 练功状态失败不阻塞 */ }
}

async function doPractice() {
  if (practiceCd.value > 0) return
  try {
    const r = await gameApi.practiceRun()
    flash(r.levelUp
      ? `突破！入第 ${r.level} 重，得 ${r.gain} 灵石`
      : `练功一遍，得 ${r.gain} 灵石 · 修为 +10`)
    await Promise.all([loadPractice(), loadWallet()])
  } catch (e) { flash(e.message || '练功未成') }
}

function formatCd(sec) {
  const s = Math.max(0, Number(sec) || 0)
  return `${String(Math.floor(s / 60)).padStart(2, '0')}:${String(s % 60).padStart(2, '0')}`
}

const practiceExpPct = computed(() => Math.min(100, practice.value?.exp || 0))

function switchTab(k) {
  tab.value = k
  err.value = ''
  if (k === 'signin') loadSign()
  if (k === 'daily') loadDaily()
  if (k === 'practice') loadPractice()
}

onMounted(() => {
  loadWallet()
  loadSpirit()
  loadSign()
  loadDaily()
  loadPractice()
  // 每秒推进两个冷却；到点时自动拉一轮新题
  tickTimer = setInterval(() => {
    if (practiceCd.value > 0) practiceCd.value -= 1
    if (slotCd.value > 0) {
      slotCd.value -= 1
      if (slotCd.value === 0) loadDaily()
    }
  }, 1000)
})
onBeforeUnmount(() => { clearInterval(tickTimer) })
</script>

<template>
  <div class="game">
    <div class="bg-photo" :style="{ backgroundImage: `url(${navBg})` }"></div>

    <header class="gm-head">
      <button class="back-btn" @click="emit('to-home')">‹ 回首页</button>
      <div class="head-brand">
        <div class="seal">修<br />行</div>
        <div class="brand-text">
          <span class="name">修行洞府</span>
          <small>测 灵 根 · 签 到 · 每 旬 一 题 · 练 功</small>
        </div>
      </div>
      <div class="head-stat">灵 石 · {{ wallet.balance || 0 }}</div>
      <button class="mini-btn" @click="showShare = true">分 享 战 报</button>
    </header>

    <div class="gm-tabs">
      <div
        v-for="t in TABS"
        :key="t.key"
        class="tab"
        :class="{ 'is-on': tab === t.key }"
        @click="switchTab(t.key)"
      >{{ t.label }}<small>{{ t.en }}</small></div>
    </div>

    <div class="gm-body">
      <section class="gm-main">
        <!-- ============ 测灵根 ============ -->
        <template v-if="tab === 'spirit'">
          <div v-if="currentSpirit" class="root-card">
            <div class="root-ring" :style="{ borderColor: elementInfo.color, color: elementInfo.color }">
              <span class="root-el">{{ elementInfo.label }}</span>
              <span class="root-rare">{{ currentSpirit.rareLabel }}</span>
            </div>
            <div class="root-info">
              <h2>{{ currentSpirit.rootName }}</h2>
              <p class="root-grade">{{ currentSpirit.gradeLabel }} · 契合 {{ currentSpirit.score }} 分</p>
              <p class="root-desc">{{ currentSpirit.summary }}</p>
              <div class="retest-row">
                <button class="mini-btn" :disabled="currentSpirit && currentSpirit.canRetest === false" @click="startQuiz">
                  重 新 测 评 · {{ currentSpirit?.retestCost || 100 }} 灵 石
                </button>
                <small v-if="currentSpirit">
                  今 日 已 测 {{ currentSpirit.retestCountToday || 0 }} / {{ currentSpirit.retestLimit || 5 }} 次
                </small>
              </div>
            </div>
          </div>

          <div v-else-if="quiz" class="quiz">
            <div class="quiz-progress">
              <span class="q-index">第 {{ qi + 1 }} / {{ quiz.questions.length }} 题</span>
              <span class="q-bar"><i :style="{ width: ((qi + 1) / quiz.questions.length * 100) + '%' }"></i></span>
            </div>
            <h3 class="q-title">{{ question?.title }}</h3>
            <div class="q-options">
              <button
                v-for="(o, i) in question?.options || []"
                :key="i"
                class="q-option"
                :class="{ 'is-on': picked === i }"
                @click="choose(i)"
              >
                <span class="q-mark">{{ ['甲', '乙', '丙', '丁'][i] }}</span>{{ o }}
              </button>
            </div>
            <div class="quiz-foot">
              <button class="mini-btn" :disabled="qi === 0" @click="qi -= 1">上 一 题</button>
              <button class="main-btn" :disabled="submitting" @click="nextQuestion">
                {{ qi < quiz.questions.length - 1 ? '下 一 题' : '揭 晓 灵 根' }}
              </button>
            </div>
          </div>

          <!-- 灵根图鉴 -->
          <div class="root-codex">
            <div class="codex-title">灵 根 图 鉴 <small>已录 {{ SPIRIT_ROOTS.length }} 种 · 点亮即为己有</small></div>
            <div class="codex-grid">
              <div
                v-for="r in SPIRIT_ROOTS"
                :key="r.id"
                class="codex-item"
                :class="{ 'is-mine': currentSpirit?.rootName === r.name }"
              >
                <span class="cx-el" :style="{ color: findElement(r.element).color }">{{ findElement(r.element).label }}</span>
                <span class="cx-name">{{ r.name }}</span>
                <span class="cx-grade" :style="{ color: findGrade(r.grade).color }">{{ findGrade(r.grade).label }}</span>
                <p class="cx-desc">{{ r.desc }}</p>
                <span v-if="currentSpirit?.rootName === r.name" class="cx-mine">我 的 灵 根</span>
              </div>
            </div>
          </div>
        </template>

        <!-- ============ 每日签到 ============ -->
        <template v-else-if="tab === 'signin'">
          <div class="sign-card">
            <div class="sign-head">
              <div>
                <small>连 续 签 到</small>
                <strong>{{ sign?.continuousDays || 0 }} 天</strong>
              </div>
              <div class="sign-total">
                <small>累 计</small>
                <strong>{{ sign?.totalDays || 0 }} 天</strong>
              </div>
            </div>
            <div class="sign-grid">
              <div
                v-for="(r, i) in SIGN_REWARD"
                :key="i"
                class="sign-cell"
                :class="{ 'is-on': (sign?.continuousDays || 0) > i, 'is-next': (sign?.continuousDays || 0) === i && !sign?.signed }"
              >
                <span class="d">第 {{ i + 1 }} 日</span>
                <span class="r">{{ r }}</span>
              </div>
            </div>
            <button class="main-btn wide" :disabled="sign?.signed || signing" @click="doSign">
              {{ sign?.signed ? '今 日 已 签 到' : '灵 泉 签 到 · 得 ' + (sign?.nextReward || 10) + ' 灵 石' }}
            </button>
          </div>
        </template>

        <!-- ============ 每旬一题（10 分钟一轮） ============ -->
        <template v-else-if="tab === 'daily'">
          <div v-if="daily" class="daily-card">
            <div class="daily-head">
              <span class="daily-date">{{ daily.slotKey || daily.date }} · 十 分 钟 一 轮</span>
              <span class="daily-reward">答对得 {{ daily.reward }} 灵石</span>
            </div>
            <div class="cd-bar"><i :style="{ width: (100 - Math.min(100, slotCd / 6)) + '%' }"></i></div>
            <p class="cd-text">距下一轮 {{ formatCd(slotCd) }}</p>
            <p class="daily-q">{{ daily.question }}</p>
            <div class="q-options">
              <button
                v-for="(o, i) in daily.options"
                :key="i"
                class="q-option"
                :class="{
                  'is-on': dailyPick === i,
                  'is-right': dailyDone && dailyDone.answerIdx === i,
                  'is-wrong': dailyDone && dailyPick === i && !dailyDone.correct
                }"
                :disabled="daily?.answered || answering"
                @click="answerDaily(i)"
              >
                <span class="q-mark">{{ ['甲', '乙', '丙', '丁'][i] }}</span>{{ o }}
              </button>
            </div>
            <p v-if="daily?.answered" class="daily-done">
              {{ daily.correct ? '本轮答对，' + daily.reward + ' 灵石已入账。' : '本轮已答。' }}
              十分钟后新题自动开启。
            </p>
          </div>
        </template>

        <!-- ============ 练功（每 30 秒一次） ============ -->
        <template v-else>
          <div class="practice-card">
            <div class="pc-head">
              <div>
                <small>练 功 重 数</small>
                <strong>第 {{ practice?.level || 1 }} 重</strong>
              </div>
              <div class="pc-total">
                <small>累 计</small>
                <strong>{{ practice?.totalCount || 0 }} 遍</strong>
              </div>
            </div>
            <div class="pc-ring" :class="{ ready: practiceCd <= 0 }">
              <span class="pc-cd">{{ practiceCd > 0 ? formatCd(practiceCd) : '可 练' }}</span>
              <span class="pc-sub">{{ practiceCd > 0 ? '气 息 未 平' : '一 息 一 遍' }}</span>
            </div>
            <div class="pc-exp">
              <span>修 为 {{ practice?.exp || 0 }} / 100</span>
              <div class="pc-bar"><i :style="{ width: practiceExpPct + '%' }"></i></div>
              <small>满 100 突破一重，另赏 20 灵石</small>
            </div>
            <p class="pc-tip">每 30 秒可练一遍，一遍得 {{ practice?.rewardSpirit || 3 }} 灵石、修为 +10。
              所得灵石可去市集按品质买丹药 / 炼器 / 阵法材料。</p>
            <button class="main-btn wide" :disabled="practiceCd > 0" @click="doPractice">
              {{ practiceCd > 0 ? '调 息 中 · ' + formatCd(practiceCd) : '运 功 一 遍' }}
            </button>
          </div>
        </template>

        <p v-if="err" class="err-line">{{ err }}</p>
      </section>

      <!-- 灵石流水 -->
      <aside class="gm-side">
        <div class="rail-title">灵 石 流 水</div>
        <div class="log-list">
          <div v-for="(l, i) in wallet.logs || []" :key="i" class="log-item" :class="{ 'is-in': l.amount > 0 }">
            <span class="lg-reason">{{ l.reason }}</span>
            <span class="lg-amount">{{ l.amount > 0 ? '+' : '' }}{{ l.amount }}</span>
          </div>
          <div v-if="!(wallet.logs || []).length" class="log-empty">尚 无 流 水</div>
        </div>
      </aside>
    </div>

    <Transition name="fade">
      <div v-if="msg" class="notice">{{ msg }}</div>
    </Transition>

    <!-- 分享战报 -->
    <div v-if="showShare" class="share-modal" @click.self="showShare = false">
      <div class="share-box">
        <button class="share-close" @click="showShare = false">×</button>
        <ShareCard />
      </div>
    </div>
  </div>
</template>

<style scoped>
.game {
  position: relative;
  height: 100vh;
  overflow: hidden;
  color: var(--paper);
  font-family: "Kaiti SC", "STKaiti", "KaiTi", "楷体", "Songti SC", "SimSun", serif;
}
.bg-photo {
  position: fixed; inset: 0; z-index: 0;
  background-size: cover; background-position: center; background-repeat: no-repeat;
}
.bg-photo::after {
  content: ''; position: absolute; inset: 0;
  background:
    radial-gradient(120% 90% at 50% 40%, rgba(10, 10, 9, 0) 35%, rgba(10, 10, 9, 0.62) 100%),
    linear-gradient(180deg, rgba(10, 10, 9, 0.72) 0%, rgba(10, 10, 9, 0.55) 50%, rgba(10, 10, 9, 0.84) 100%);
}
.gm-head {
  position: relative; z-index: 2; height: 92px;
  display: flex; align-items: center; gap: 22px; padding: 0 30px;
  border-bottom: 1px solid var(--line); background: rgba(12, 16, 15, 0.42);
}
.back-btn {
  flex: none; padding: 8px 16px; color: var(--paper);
  font: inherit; font-size: 15px; letter-spacing: 2px;
  background: rgba(236, 228, 210, 0.06); border: 1px solid var(--line); border-radius: 2px; cursor: pointer;
}
.back-btn:hover { background: rgba(236, 228, 210, 0.14); }
.head-brand { display: flex; align-items: center; gap: 14px; }
.seal {
  width: 44px; height: 44px; display: grid; place-content: center;
  font-size: 15px; line-height: 1.05; text-align: center;
  color: #f6ece0; background: #a3311f; border: 1px solid #c8543c;
  box-shadow: 0 0 14px rgba(163, 49, 31, 0.45);
}
.brand-text { display: flex; flex-direction: column; gap: 3px; }
.brand-text .name { font-size: 26px; letter-spacing: 8px; }
.brand-text small { font-size: 11px; letter-spacing: 3px; color: var(--muted); }
.head-stat { margin-left: auto; font-size: 15px; letter-spacing: 3px; color: #e8c372; }

.gm-tabs {
  position: relative; z-index: 2;
  display: flex; gap: 10px; padding: 14px 26px 0;
}
.tab {
  padding: 9px 22px; font-size: 15px; letter-spacing: 3px;
  color: var(--muted); background: rgba(12, 17, 16, 0.55);
  border: 1px solid var(--line); border-bottom: none; cursor: pointer;
  display: flex; align-items: baseline; gap: 8px;
}
.tab small { font-size: 10px; letter-spacing: 1px; opacity: 0.7; }
.tab.is-on { color: #e8c372; background: rgba(12, 17, 16, 0.78); border-color: rgba(232, 195, 114, 0.4); }

.gm-body {
  position: relative; z-index: 2;
  height: calc(100vh - 150px);
  display: grid; grid-template-columns: minmax(0, 1fr) 262px; gap: 16px;
  padding: 14px 26px 30px;
}
.gm-main, .gm-side {
  min-height: 0; padding: 24px 28px;
  background: rgba(12, 17, 16, 0.58); border: 1px solid var(--line); backdrop-filter: blur(4px);
}
.gm-main { overflow-y: auto; }
.gm-side { overflow-y: auto; padding: 16px 14px; }

/* 灵根结果 */
.root-card { display: flex; gap: 26px; align-items: center; padding: 10px 0 24px; }
.root-ring {
  flex: none; width: 148px; height: 148px; border-radius: 50%;
  display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 6px;
  border: 1px solid; background: rgba(236, 228, 210, 0.05);
}
.root-el { font-size: 52px; letter-spacing: 0; }
.root-rare { font-size: 13px; letter-spacing: 5px; }
.root-info h2 { font-size: 32px; font-weight: normal; letter-spacing: 8px; margin-bottom: 8px; }
.root-grade { font-size: 13px; letter-spacing: 3px; color: #e8c372; margin-bottom: 12px; }
.root-desc { font-size: 14px; line-height: 2; letter-spacing: 1px; color: rgba(236, 228, 210, 0.8); margin-bottom: 16px; }

/* 答题 */
.quiz-progress { display: flex; align-items: center; gap: 14px; margin-bottom: 22px; }
.q-index { font-size: 12px; letter-spacing: 3px; color: var(--muted); }
.q-bar { flex: 1; height: 2px; background: rgba(236, 228, 210, 0.14); }
.q-bar i { display: block; height: 100%; background: #e8412b; transition: width 0.25s; }
.q-title { font-size: 26px; font-weight: normal; letter-spacing: 5px; margin-bottom: 24px; line-height: 1.6; }
.q-options { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
.q-option {
  display: flex; align-items: center; gap: 12px; padding: 15px 18px;
  font: inherit; font-size: 16px; letter-spacing: 2px; text-align: left;
  color: var(--paper); background: rgba(236, 228, 210, 0.05);
  border: 1px solid var(--line); cursor: pointer; transition: all 0.18s;
}
.q-option:hover:not(:disabled) { background: rgba(236, 228, 210, 0.12); }
.q-option:disabled { cursor: not-allowed; opacity: 0.7; }
.q-option.is-on { background: rgba(232, 65, 43, 0.18); border-color: #e8412b; }
.q-option.is-right { background: rgba(99, 153, 34, 0.22); border-color: #639922; }
.q-option.is-wrong { background: rgba(232, 75, 74, 0.2); border-color: #e24b4a; }
.q-mark {
  flex: none; width: 26px; height: 26px; display: grid; place-items: center;
  font-size: 13px; border: 1px solid var(--line); border-radius: 50%;
}
.quiz-foot { display: flex; gap: 14px; margin-top: 26px; }
/* 灵根图鉴 */
.root-codex { margin-top: 30px; padding-top: 20px; border-top: 1px solid var(--line); }
.codex-title { font-size: 13px; letter-spacing: 5px; color: #e8c372; margin-bottom: 14px; }
.codex-title small { font-size: 11px; color: var(--muted); letter-spacing: 1px; margin-left: 10px; }
.codex-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(190px, 1fr)); gap: 12px; }
.codex-item {
  position: relative; padding: 12px 14px;
  border: 1px solid var(--line); background: rgba(236, 228, 210, 0.03);
}
.codex-item.is-mine { border-color: #e8c372; background: rgba(232, 195, 114, 0.1); }
.cx-el { font-size: 18px; margin-right: 8px; }
.cx-name { font-size: 15px; letter-spacing: 2px; }
.cx-grade { float: right; font-size: 11px; }
.cx-desc { margin: 8px 0 0; font-size: 11px; line-height: 1.7; color: var(--muted); }
.cx-mine {
  display: inline-block; margin-top: 8px; padding: 2px 8px;
  font-size: 10px; letter-spacing: 2px; color: #f6ece0; background: #a3311f; border: 1px solid #c8543c;
}

/* 按钮 */
.mini-btn, .main-btn {
  font: inherit; font-size: 14px; letter-spacing: 3px; cursor: pointer;
  padding: 9px 20px; color: var(--paper);
  background: rgba(236, 228, 210, 0.08); border: 1px solid var(--line); border-radius: 2px;
}
.mini-btn:hover:not(:disabled), .main-btn:hover:not(:disabled) { background: rgba(236, 228, 210, 0.18); }
.main-btn {
  color: #f6ece0; background: linear-gradient(180deg, rgba(163, 49, 31, 0.92), rgba(120, 33, 21, 0.92));
  border-color: #c8543c;
}
.main-btn.wide { width: 100%; padding: 14px; font-size: 16px; }
.main-btn:disabled, .mini-btn:disabled { opacity: 0.4; cursor: not-allowed; }

/* 签到 */
.sign-head { display: flex; gap: 40px; align-items: baseline; margin-bottom: 22px; }
.sign-head small { display: block; font-size: 11px; letter-spacing: 3px; color: var(--muted); margin-bottom: 6px; }
.sign-head strong { font-size: 34px; font-weight: normal; letter-spacing: 3px; color: #e8c372; }
.sign-total strong { color: var(--paper); font-size: 24px; }
.sign-grid { display: grid; grid-template-columns: repeat(7, 1fr); gap: 10px; margin-bottom: 26px; }
.sign-cell {
  padding: 14px 6px; text-align: center;
  border: 1px solid var(--line); background: rgba(236, 228, 210, 0.04);
}
.sign-cell .d { display: block; font-size: 11px; letter-spacing: 1px; color: var(--muted); margin-bottom: 8px; }
.sign-cell .r { font-size: 17px; color: rgba(236, 228, 210, 0.6); }
.sign-cell.is-on { border-color: #639922; background: rgba(99, 153, 34, 0.16); }
.sign-cell.is-on .r { color: #c0dd97; }
.sign-cell.is-next { border-color: #e8c372; }

/* 每日一题 */
.daily-head { display: flex; justify-content: space-between; align-items: baseline; margin-bottom: 18px; }
.daily-date { font-size: 13px; letter-spacing: 3px; color: var(--muted); }
.daily-reward { font-size: 13px; letter-spacing: 2px; color: #e8c372; }
.daily-q { font-size: 19px; line-height: 2; letter-spacing: 2px; margin-bottom: 24px; white-space: pre-line; }
.daily-done { margin-top: 18px; font-size: 13px; letter-spacing: 2px; color: #e8c372; }

/* ---------- 重测 / 倒计时 / 练功 ---------- */
.retest-row { display: flex; align-items: center; gap: 12px; margin-top: 12px; flex-wrap: wrap; }
.retest-row small { font-size: 11px; letter-spacing: 1px; color: var(--muted); }
.cd-bar { height: 4px; margin-top: 14px; background: rgba(236, 228, 210, 0.1); }
.cd-bar i { display: block; height: 100%; background: linear-gradient(90deg, #8a2f1e, #e8c372); transition: width 1s linear; }
.cd-text { margin: 6px 0 0; font-size: 11px; letter-spacing: 2px; color: #e8c372; text-align: right; }
.practice-card {
  padding: 26px 30px; border: 1px solid var(--line); background: rgba(12, 17, 16, 0.55);
}
.pc-head { display: flex; align-items: baseline; gap: 30px; border-bottom: 1px solid var(--line); padding-bottom: 14px; }
.pc-head small, .pc-total small { display: block; font-size: 11px; letter-spacing: 3px; color: var(--muted); }
.pc-head strong { font-size: 24px; letter-spacing: 3px; font-weight: normal; color: #e8c372; }
.pc-total { margin-left: auto; }
.pc-total strong { font-size: 18px; }
.pc-ring {
  width: 168px; height: 168px; margin: 26px auto; border-radius: 50%;
  display: grid; place-content: center; gap: 6px; text-align: center;
  border: 2px solid rgba(232, 195, 114, 0.28); background: radial-gradient(circle, rgba(232, 195, 114, 0.08), transparent 70%);
}
.pc-ring.ready { border-color: #e8c372; box-shadow: 0 0 28px rgba(232, 195, 114, 0.2); }
.pc-cd { font-size: 30px; letter-spacing: 3px; color: #e8c372; }
.pc-sub { font-size: 11px; letter-spacing: 3px; color: var(--muted); }
.pc-exp { margin-bottom: 18px; font-size: 12px; letter-spacing: 2px; color: var(--muted); }
.pc-bar { height: 6px; margin: 8px 0; background: rgba(236, 228, 210, 0.1); }
.pc-bar i { display: block; height: 100%; background: linear-gradient(90deg, #3a8fd0, #97c459); transition: width 0.3s; }
.pc-exp small { font-size: 11px; }
.pc-tip { font-size: 13px; line-height: 2; letter-spacing: 1px; color: rgba(236, 228, 210, 0.78); margin: 0 0 18px; }

/* 流水 */
.rail-title {
  padding-bottom: 12px; margin-bottom: 10px;
  font-size: 14px; letter-spacing: 4px; color: #e8c372; border-bottom: 1px solid var(--line);
}
.log-list { display: flex; flex-direction: column; gap: 8px; }
.log-item {
  display: flex; justify-content: space-between; gap: 10px;
  padding: 9px 10px; font-size: 12px; letter-spacing: 1px;
  border-left: 2px solid rgba(232, 75, 74, 0.6); background: rgba(236, 228, 210, 0.04);
}
.log-item.is-in { border-left-color: #639922; }
.lg-reason { color: rgba(236, 228, 210, 0.8); }
.lg-amount { color: #f0a89a; }
.log-item.is-in .lg-amount { color: #c0dd97; }
.log-empty { font-size: 12px; letter-spacing: 2px; color: var(--muted); text-align: center; padding: 20px 0; }

.notice {
  position: fixed; left: 50%; bottom: 60px; transform: translateX(-50%); z-index: 9;
  padding: 10px 26px; font-size: 14px; letter-spacing: 2px;
  color: var(--paper); background: rgba(163, 49, 31, 0.92); border: 1px solid #c8543c;
}
.err-line { margin-top: 18px; font-size: 13px; letter-spacing: 1px; color: #f0a89a; }
.share-modal {
  position: fixed; inset: 0; z-index: 20; display: grid; place-items: center;
  background: rgba(6, 9, 8, 0.75);
}
.share-box {
  position: relative; padding: 24px 26px;
  background: linear-gradient(160deg, #1c2421, #101614);
  border: 1px solid rgba(236, 228, 210, 0.3);
}
.share-close {
  position: absolute; top: 6px; right: 12px; font-size: 24px;
  color: var(--muted); background: none; border: none; cursor: pointer;
}
.share-close:hover { color: var(--paper); }
.fade-enter-active, .fade-leave-active { transition: opacity 0.25s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }

@media (max-width: 1000px) {
  .gm-body { grid-template-columns: 1fr; }
  .gm-side { display: none; }
  .q-options { grid-template-columns: 1fr; }
}
</style>
