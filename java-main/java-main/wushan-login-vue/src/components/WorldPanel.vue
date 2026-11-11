<script setup>
/**
 * 山门世界玩法面板：秘境探索 / 炼丹炼器 / 五行问对决 / 传书私信
 * 右侧常驻背包与灵石余额 —— 秘境掉的材料、炼丹的产物都在这里流转。
 */
import { ref, computed, onMounted, watch } from 'vue'
import { worldApi } from '../api/world'
import { findElement } from '../constants/wuxing'
import navBg from '../assets/nav-bg-4.jpg'

const props = defineProps({ tab: { type: String, default: 'explore' } })
const emit = defineEmits(['to-home'])

const TITLES = {
  explore: { seal: '秘\n境', name: '秘境', sub: '一 探 幽 窟 · 材 料 尽 归' },
  alchemy: { seal: '炼\n丹', name: '炼丹', sub: '文 火 武 火 · 尽 在 一 炉' },
  lianqi: { seal: '炼\n器', name: '炼器', sub: '金 铁 入 炉 · 器 成 待 主' },
  zhenfa: { seal: '阵\n法', name: '阵法', sub: '阵 眼 方 位 · 一 笔 定 乾 坤' },
  duel: { seal: '对\n决', name: '五行问', sub: '五 题 定 胜 负' },
  letter: { seal: '传\n书', name: '传书', sub: '一 纸 心 事 · 越 山 河' }
}

/* 背包 / 灵石 */
const bag = ref({ items: [], balance: 0 })
const bagItems = computed(() => bag.value.items || [])

/* 秘境 */
const realms = ref([])
const node = ref(null)
const exploreStep = ref(0)
const exploreTotal = 3
const explorePath = ref([])
const exploreRewards = ref([])
const exploreDone = ref(false)
const exploring = ref(false)
const fortune = ref(null)

/* 炼丹 / 炼器 / 阵法 */
const recipes = ref([])
const crafting = ref(false)
const craftResult = ref(null)
const CRAFT_TYPES = { alchemy: 'danyao', lianqi: 'lianqi', zhenfa: 'zhenfa' }

/* 对决 */
const duel = ref(null)
const duelIndex = ref(0)
const duelResult = ref(null)
const duelRecords = ref([])
const dueling = ref(false)

/* 传书 */
const inbox = ref({ messages: [], unread: 0, targets: [] })
const toName = ref('')
const content = ref('')
const sending = ref(false)

/* 秘境图鉴 / 炼丹图鉴 */
const exploreLogs = ref([])
const exploredNames = computed(() => new Set((exploreLogs.value || []).map((l) => l.realmName)))
const bagHas = (itemId) => bagItems.value.some((b) => b.itemId === itemId && b.count > 0)

const err = ref('')
const msg = ref('')
let msgTimer = null

function flash(m) {
  msg.value = m
  clearTimeout(msgTimer)
  msgTimer = setTimeout(() => { msg.value = '' }, 2400)
}

async function loadBag() {
  try {
    bag.value = await worldApi.getBag()
  } catch (e) { /* 背包失败不阻塞 */ }
}

async function loadRealms() {
  try {
    realms.value = await worldApi.getRealms()
  } catch (e) { err.value = e.message }
}

async function enterRealm(realm) {
  if (exploring.value) return
  exploring.value = true
  err.value = ''
  try {
    const r = await worldApi.exploreStart(realm.id)
    node.value = r.node
    exploreStep.value = r.step
    exploreTotal.value = r.total
    explorePath.value = []
    exploreRewards.value = []
    exploreDone.value = false
    fortune.value = null
  } catch (e) {
    err.value = e.message || '入秘境失败'
  } finally {
    exploring.value = false
  }
}

async function choose(i) {
  if (exploring.value || !node.value) return
  exploring.value = true
  try {
    const r = await worldApi.exploreChoose(i)
    explorePath.value = r.path || explorePath.value
    if (r.fortune) {
      fortune.value = r.fortune
      flash('奇 遇 · ' + r.fortune.name + '！')
    }
    if (r.done) {
      exploreDone.value = true
      exploreRewards.value = r.rewards || []
      node.value = null
      if (!r.fortune) flash('秘境归来，所获已入背包')
      await loadBag()
    } else {
      node.value = r.node
      exploreStep.value = r.step
    }
  } catch (e) {
    err.value = e.message || '探索中断'
  } finally {
    exploring.value = false
  }
}

async function loadRecipes() {
  try {
    recipes.value = await worldApi.getRecipes(CRAFT_TYPES[props.tab] || null)
  } catch (e) { err.value = e.message }
}

async function craft(r) {
  if (crafting.value) return
  crafting.value = true
  craftResult.value = null
  try {
    const res = await worldApi.craft(r.id)
    craftResult.value = res
    flash(res.success ? `炼成「${res.outputTitle}」×${res.outputCount}` : '炉毁了 · ' + res.message)
    await Promise.all([loadRecipes(), loadBag()])
  } catch (e) {
    flash(e.message || '炼制失败')
  } finally {
    crafting.value = false
  }
}

async function loadDuel() {
  try {
    duelRecords.value = await worldApi.getDuelRecords()
  } catch (e) { /* 记录失败不阻塞 */ }
}

async function startDuel() {
  if (dueling.value) return
  dueling.value = true
  err.value = ''
  duelResult.value = null
  try {
    const r = await worldApi.duelStart()
    duel.value = r
    duelIndex.value = 0
  } catch (e) {
    err.value = e.message || '无人应战'
  } finally {
    dueling.value = false
  }
}

async function answerDuel(i) {
  if (dueling.value || !duel.value) return
  dueling.value = true
  try {
    const r = await worldApi.duelAnswer(duel.value.duelId, i)
    if (r.finished) {
      duelResult.value = r
      duel.value = null
      await Promise.all([loadDuel(), loadBag()])
    } else {
      duelIndex.value += 1
    }
  } catch (e) {
    flash(e.message || '应答失败')
  } finally {
    dueling.value = false
  }
}

async function loadInbox() {
  try {
    inbox.value = await worldApi.getInbox()
  } catch (e) { err.value = e.message }
}

async function sendLetter() {
  if (sending.value) return
  sending.value = true
  try {
    const r = await worldApi.send(toName.value, content.value)
    flash(`已寄予 ${r.toName}`)
    content.value = ''
    await loadInbox()
  } catch (e) {
    flash(e.message || '传书失败')
  } finally {
    sending.value = false
  }
}

async function readMsg(m) {
  if (m.isRead) return
  try {
    await worldApi.read(m.id)
    m.isRead = 1
    inbox.value.unread = Math.max(0, (inbox.value.unread || 1) - 1)
  } catch (e) { /* 忽略 */ }
}

/** 一键全部已读 */
async function readAll() {
  try {
    await worldApi.readAll()
    ;(inbox.value.messages || []).forEach((m) => { m.isRead = 1 })
    inbox.value.unread = 0
    flash('已全部标记为已读')
  } catch (e) {
    flash(e.message || '操作失败')
  }
}

/** 按寄信人分组的会话 */
const conversations = computed(() => {
  const map = new Map()
  for (const m of inbox.value.messages || []) {
    if (!map.has(m.fromName)) {
      map.set(m.fromName, { name: m.fromName, last: m, unread: 0, count: 0 })
    }
    const g = map.get(m.fromName)
    g.count += 1
    if (!m.isRead) g.unread += 1
    if (m.id > g.last.id) g.last = m
  }
  return Array.from(map.values())
})

async function loadForTab() {
  err.value = ''
  if (props.tab === 'explore') { loadRealms(); loadBag(); loadExploreLogs() }
  if (['alchemy', 'lianqi', 'zhenfa'].includes(props.tab)) { loadRecipes(); loadBag() }
  if (props.tab === 'duel') { loadDuel(); loadBag() }
  if (props.tab === 'letter') { loadInbox() }
}

/** 我的探索记录（用于秘境图鉴标记） */
async function loadExploreLogs() {
  try {
    exploreLogs.value = await worldApi.getExploreLogs()
  } catch (e) { /* 图鉴标记失败不影响探索 */ }
}

onMounted(loadForTab)
watch(() => props.tab, loadForTab)
</script>

<template>
  <div class="world">
    <div class="bg-photo" :style="{ backgroundImage: `url(${navBg})` }"></div>

    <header class="wd-head">
      <button class="back-btn" @click="emit('to-home')">‹ 回首页</button>
      <div class="head-brand">
        <div class="seal">{{ TITLES[tab].seal }}</div>
        <div class="brand-text">
          <span class="name">{{ TITLES[tab].name }}</span>
          <small>{{ TITLES[tab].sub }}</small>
        </div>
      </div>
      <div class="head-stat">灵 石 · {{ bag.balance || 0 }}</div>
    </header>

    <div class="wd-body">
      <section class="wd-main">
        <!-- ============ 秘境 ============ -->
        <template v-if="tab === 'explore'">
          <div v-if="node" class="node-card">
            <div class="node-head">
              <span class="node-title">{{ node.title }}</span>
              <span class="node-step">第 {{ exploreStep }} / {{ exploreTotal }} 段</span>
            </div>
            <p class="node-desc">{{ node.desc }}</p>
            <div class="choices">
              <button
                v-for="(c, i) in node.choices"
                :key="i"
                class="choice-btn"
                :disabled="exploring"
                @click="choose(i)"
              >
                <span class="c-text">{{ c.text }}</span>
                <span class="c-hint">{{ c.hint }}</span>
              </button>
            </div>
          </div>

          <div v-else-if="exploreDone" class="node-card">
            <div class="node-head"><span class="node-title">归 来</span></div>
            <!-- 奇遇：偶尔撞见，额外灵石与稀有材料一并计入所得 -->
            <div v-if="fortune" class="fortune-card">
              <span class="ft-tag">奇 遇</span>
              <strong>{{ fortune.name }}</strong>
              <p>{{ fortune.desc }}</p>
              <span v-if="fortune.spirit" class="ft-gain">+{{ fortune.spirit }} 灵石</span>
              <span v-if="fortune.itemTitle" class="ft-gain">{{ fortune.itemTitle }} ×{{ fortune.itemCount }}</span>
            </div>
            <p class="node-desc">此行所得：</p>
            <div class="reward-list">
              <div v-for="(r, i) in exploreRewards" :key="i" class="reward">
                {{ r.title }} × {{ r.count }}
              </div>
              <div v-if="!exploreRewards.length" class="reward">空手而归</div>
            </div>
            <div class="path-list">
              <span v-for="(p, i) in explorePath" :key="i" class="path-item">{{ p }}</span>
            </div>
            <button class="main-btn" @click="node = null; exploreDone = false; fortune = null">再 探 一 处</button>
          </div>

          <div v-else>
            <h3 class="sec-title">可 入 之 境</h3>
            <div class="realm-grid">
              <div
                v-for="r in realms"
                :key="r.id"
                class="realm"
                @click="enterRealm(r)"
              >
                <span class="realm-el" :style="{ color: findElement(r.element).color }">{{ findElement(r.element).label }}</span>
                <span class="realm-name">{{ r.name }}</span>
                <span class="realm-grade" :style="{ color: findElement(r.element).color }">{{ r.gradeLabel }}</span>
                <span class="realm-seen" v-if="exploredNames.has(r.name)">已 探</span>
                <p class="realm-desc">{{ r.summary }}</p>
                <span class="realm-cost">入 境 · {{ r.cost }} 灵 石</span>
              </div>
            </div>
          </div>
        </template>

        <!-- ============ 炼丹 / 炼器 / 阵法（均有失败概率） ============ -->
        <template v-else-if="['alchemy', 'lianqi', 'zhenfa'].includes(tab)">
          <h3 class="sec-title">
            {{ tab === 'lianqi' ? '器 谱' : tab === 'zhenfa' ? '阵 图 谱' : '丹 方 谱' }}
            <small>火 候 有 失 · 成 败 有 数</small>
          </h3>
          <p class="craft-tip">材料可在首页「市集」的丹药 / 炼器 / 阵法材料铺按品质用灵石购买，购得即入背包。</p>

          <div v-if="craftResult" class="craft-result" :class="{ fail: !craftResult.success }">
            <span class="cr-tag">{{ craftResult.success ? '功 成' : '炉 毁' }}</span>
            <span>{{ craftResult.message }}</span>
            <span v-if="!craftResult.success" class="cr-refund">退回 {{ craftResult.refund }} 灵石</span>
          </div>

          <div class="recipe-list">
            <div v-for="r in recipes" :key="r.id" class="recipe">
              <div class="recipe-head">
                <span class="recipe-name">{{ r.name }}</span>
                <span class="recipe-tag">{{ r.craftLabel }}</span>
                <span class="recipe-rate" :class="{ low: r.successRate < 60 }">成 率 {{ r.successRate }}%</span>
                <span class="recipe-out">出 {{ r.outputTitle }} ×{{ r.outputCount }}</span>
                <span class="recipe-made" v-if="bagHas(r.outputItemId)">已 炼</span>
              </div>
              <p class="recipe-desc">{{ r.description }}</p>
              <div class="mat-row">
                <span
                  v-for="m in r.materials"
                  :key="m.itemId"
                  class="mat-chip"
                  :class="{ 'is-lack': m.have < m.need }"
                >{{ m.title }} {{ m.have }}/{{ m.need }}</span>
                <span class="mat-chip cost">耗 {{ r.costSpirit }} 灵石</span>
              </div>
              <button class="main-btn" :disabled="!r.enough || crafting" @click="craft(r)">
                {{ r.enough ? (tab === 'zhenfa' ? '开 坛 布 阵' : tab === 'lianqi' ? '开 炉 炼 器' : '开 炉 炼 丹') : '材 料 不 足' }}
              </button>
            </div>
          </div>
        </template>

        <!-- ============ 对决 ============ -->
        <template v-else-if="tab === 'duel'">
          <div v-if="duelResult" class="node-card">
            <div class="node-head"><span class="node-title">对 决 已 终</span></div>
            <p class="result-line">
              {{ duelResult.result === 'win' ? '你 胜' : duelResult.result === 'lose' ? '你 负' : '平 手' }}
              · {{ duelResult.score }} 比 {{ duelResult.opScore }}
            </p>
            <p class="node-desc">所得 {{ duelResult.reward }} 灵石</p>
            <button class="main-btn" @click="duelResult = null">再 战 一 场</button>
          </div>

          <div v-else-if="duel" class="node-card">
            <div class="node-head">
              <span class="node-title">对 手 · {{ duel.opponentName }}</span>
              <span class="node-step">第 {{ duelIndex + 1 }} / {{ duel.total }} 题</span>
            </div>
            <p class="node-desc">{{ duel.questions[duelIndex]?.title }}</p>
            <div class="choices">
              <button
                v-for="(o, i) in duel.questions[duelIndex]?.options || []"
                :key="i"
                class="choice-btn"
                :disabled="dueling"
                @click="answerDuel(i)"
              >{{ o }}</button>
            </div>
          </div>

          <div v-else>
            <div class="node-card">
              <div class="node-head"><span class="node-title">五 行 问</span></div>
              <p class="node-desc">随机匹配一位在线山友，五题定胜负。答对多者胜，参与即得灵石。</p>
              <button class="main-btn" :disabled="dueling" @click="startDuel">求 一 战</button>
            </div>

            <!-- 五行相生相克 -->
            <h3 class="sec-title">五 行 相 剋 相 生</h3>
            <svg class="wuxing-chart" viewBox="0 0 320 290">
              <polyline points="64.9,119.1 160,50 255.1,119.1 218.8,230.9 101.2,230.9 64.9,119.1"
                fill="none" stroke="#639922" stroke-width="1.4" stroke-dasharray="5 4" />
              <polyline points="64.9,119.1 255.1,119.1 101.2,230.9 160,50 218.8,230.9 64.9,119.1"
                fill="none" stroke="#e8412b" stroke-width="1.4" />
              <circle cx="160" cy="50" r="23" fill="#3a1d16" stroke="#e8412b" stroke-width="1" />
              <text x="160" y="57" text-anchor="middle" font-size="17" fill="#f0c9b8">火</text>
              <circle cx="255.1" cy="119.1" r="23" fill="#3a2c16" stroke="#c8923a" stroke-width="1" />
              <text x="255.1" y="126" text-anchor="middle" font-size="17" fill="#e8c372">土</text>
              <circle cx="218.8" cy="230.9" r="23" fill="#33291a" stroke="#e8c372" stroke-width="1" />
              <text x="218.8" y="238" text-anchor="middle" font-size="17" fill="#e8c372">金</text>
              <circle cx="101.2" cy="230.9" r="23" fill="#16232e" stroke="#3a8fd0" stroke-width="1" />
              <text x="101.2" y="238" text-anchor="middle" font-size="17" fill="#85b7eb">水</text>
              <circle cx="64.9" cy="119.1" r="23" fill="#16281c" stroke="#4caf6e" stroke-width="1" />
              <text x="64.9" y="126" text-anchor="middle" font-size="17" fill="#97c459">木</text>
              <text x="160" y="278" text-anchor="middle" font-size="11" fill="#888780">实线为相克 · 虚线为相生</text>
            </svg>

            <h3 class="sec-title">往 场 对 决</h3>
            <div class="record-list">
              <div v-for="d in duelRecords" :key="d.id" class="record">
                <span class="rec-name">对 {{ d.opponent }}</span>
                <span class="rec-score">{{ d.myScore }} : {{ d.opScore }}</span>
                <span class="rec-result" :class="d.result">
                  {{ d.result === 'win' ? '胜' : d.result === 'lose' ? '负' : '平' }}
                </span>
              </div>
              <div v-if="!duelRecords.length" class="log-empty">尚 无 对 决 记 录</div>
            </div>
          </div>
        </template>

        <!-- ============ 传书 ============ -->
        <template v-else>
          <div class="letter-compose">
            <h3 class="sec-title">修 书 一 封</h3>
            <div class="compose-row">
              <select v-model="toName" class="sel">
                <option value="">收 信 人</option>
                <option v-for="t in inbox.targets" :key="t.username" :value="t.username">
                  {{ t.username }}
                </option>
              </select>
              <input v-model="content" class="txt" placeholder="写点什么…" maxlength="200" />
              <button class="main-btn" :disabled="sending || !toName || !content" @click="sendLetter">寄 出</button>
            </div>
          </div>
          <h3 class="sec-title">
            收 件 箱 <small v-if="inbox.unread">（{{ inbox.unread }} 封未读）</small>
            <button v-if="inbox.unread" class="mini-btn" @click="readAll">全 部 已 读</button>
          </h3>
          <!-- 会话列表 -->
          <div class="conv-row">
            <div
              v-for="c in conversations"
              :key="c.name"
              class="conv"
              :class="{ 'is-unread': c.unread > 0 }"
              @click="readMsg(c.last)"
            >
              <span class="c-name">{{ c.name }}</span>
              <span class="c-last">{{ c.last.content }}</span>
              <span class="c-count" v-if="c.unread">{{ c.unread }}</span>
            </div>
            <div v-if="!conversations.length" class="log-empty">尚 无 会 话</div>
          </div>
          <div class="letter-list">
            <div
              v-for="m in inbox.messages"
              :key="m.id"
              class="letter"
              :class="{ 'is-unread': !m.isRead }"
              @click="readMsg(m)"
            >
              <div class="letter-head">
                <span class="from">{{ m.fromName }}</span>
                <span class="time">{{ m.time }}</span>
              </div>
              <p class="letter-body">{{ m.content }}</p>
            </div>
            <div v-if="!inbox.messages.length" class="log-empty">收 件 箱 空 空 如 也</div>
          </div>
        </template>

        <p v-if="err" class="err-line">{{ err }}</p>
      </section>

      <!-- 背包 -->
      <aside class="wd-side">
        <div class="rail-title">随 身 包 囊 <small>{{ bagItems.length }} 种</small></div>
        <div class="bag-list">
          <div v-for="b in bagItems" :key="b.itemId" class="bag-item">
            <span class="bag-name">{{ b.title }}</span>
            <span class="bag-count">×{{ b.count }}</span>
            <span class="bag-grade" :style="{ color: findElement(b.element).color }">{{ b.gradeLabel }}</span>
          </div>
          <div v-if="!bagItems.length" class="log-empty">囊 中 空 空</div>
        </div>
      </aside>
    </div>

    <Transition name="fade">
      <div v-if="msg" class="notice">{{ msg }}</div>
    </Transition>
  </div>
</template>

<style scoped>
.world {
  position: relative; height: 100vh; overflow: hidden;
  color: var(--paper);
  font-family: "Kaiti SC", "STKaiti", "KaiTi", "楷体", "Songti SC", "SimSun", serif;
}
.bg-photo { position: fixed; inset: 0; z-index: 0; background-size: cover; background-position: center; }
.bg-photo::after {
  content: ''; position: absolute; inset: 0;
  background:
    radial-gradient(120% 90% at 50% 40%, rgba(10, 10, 9, 0) 35%, rgba(10, 10, 9, 0.62) 100%),
    linear-gradient(180deg, rgba(10, 10, 9, 0.72) 0%, rgba(10, 10, 9, 0.55) 50%, rgba(10, 10, 9, 0.84) 100%);
}
.wd-head {
  position: relative; z-index: 2; height: 92px;
  display: flex; align-items: center; gap: 22px; padding: 0 30px;
  border-bottom: 1px solid var(--line); background: rgba(12, 16, 15, 0.42);
}
.back-btn {
  flex: none; padding: 8px 16px; color: var(--paper); font: inherit; font-size: 15px; letter-spacing: 2px;
  background: rgba(236, 228, 210, 0.06); border: 1px solid var(--line); border-radius: 2px; cursor: pointer;
}
.back-btn:hover { background: rgba(236, 228, 210, 0.14); }
.head-brand { display: flex; align-items: center; gap: 14px; }
.seal {
  width: 44px; height: 44px; display: grid; place-content: center;
  font-size: 15px; line-height: 1.05; text-align: center; white-space: pre-line;
  color: #f6ece0; background: #a3311f; border: 1px solid #c8543c;
  box-shadow: 0 0 14px rgba(163, 49, 31, 0.45);
}
.brand-text { display: flex; flex-direction: column; gap: 3px; }
.brand-text .name { font-size: 26px; letter-spacing: 8px; }
.brand-text small { font-size: 11px; letter-spacing: 3px; color: var(--muted); }
.head-stat { margin-left: auto; font-size: 15px; letter-spacing: 3px; color: #e8c372; }

.wd-body {
  position: relative; z-index: 2; height: calc(100vh - 92px);
  display: grid; grid-template-columns: minmax(0, 1fr) 262px; gap: 16px;
  padding: 18px 26px 30px;
}
.wd-main, .wd-side {
  min-height: 0; padding: 22px 26px;
  background: rgba(12, 17, 16, 0.58); border: 1px solid var(--line); backdrop-filter: blur(4px);
}
.wd-main { overflow-y: auto; }
.wd-side { overflow-y: auto; padding: 16px 14px; }

.sec-title { font-size: 15px; font-weight: normal; letter-spacing: 5px; color: #e8c372; margin-bottom: 16px; }
.sec-title small { font-size: 12px; color: var(--muted); letter-spacing: 2px; }

/* 秘境 */
.realm-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(200px, 1fr)); gap: 14px; }
.realm {
  padding: 16px 18px; cursor: pointer;
  border: 1px solid var(--line); background: rgba(236, 228, 210, 0.04);
  transition: all 0.18s;
}
.realm:hover { background: rgba(232, 65, 43, 0.12); border-color: rgba(232, 65, 43, 0.5); }
.realm-el { font-size: 22px; margin-right: 8px; }
.realm-name { font-size: 19px; letter-spacing: 3px; }
.realm-grade { float: right; font-size: 12px; }
.realm-desc { margin: 10px 0 12px; font-size: 12px; line-height: 1.8; color: var(--muted); }
.realm-cost { font-size: 12px; letter-spacing: 2px; color: #e8c372; }
.realm-seen {
  float: right; padding: 1px 7px; font-size: 10px; letter-spacing: 1px;
  color: #c0dd97; border: 1px solid #639922; background: rgba(99, 153, 34, 0.16);
}
.recipe-made {
  padding: 1px 7px; font-size: 10px; letter-spacing: 1px;
  color: #c0dd97; border: 1px solid #639922; background: rgba(99, 153, 34, 0.16);
}
.wuxing-chart { display: block; width: 100%; max-width: 360px; margin: 0 auto 24px; }

.node-card { padding: 8px 0; }
.node-head { display: flex; justify-content: space-between; align-items: baseline; margin-bottom: 14px; }
.node-title { font-size: 26px; letter-spacing: 6px; }
.node-step { font-size: 12px; letter-spacing: 2px; color: var(--muted); }
.node-desc { font-size: 15px; line-height: 2; letter-spacing: 2px; margin-bottom: 20px; }
.choices { display: flex; flex-direction: column; gap: 12px; }
.choice-btn {
  display: flex; justify-content: space-between; align-items: center; gap: 14px;
  padding: 15px 20px; font: inherit; font-size: 16px; letter-spacing: 2px; text-align: left;
  color: var(--paper); background: rgba(236, 228, 210, 0.05);
  border: 1px solid var(--line); cursor: pointer; transition: all 0.18s;
}
.choice-btn:hover:not(:disabled) { background: rgba(232, 65, 43, 0.16); border-color: #e8412b; }
.choice-btn:disabled { opacity: 0.5; cursor: not-allowed; }
.c-hint { font-size: 12px; color: var(--muted); letter-spacing: 1px; }
.reward-list { display: flex; flex-wrap: wrap; gap: 10px; margin-bottom: 18px; }
.reward { padding: 8px 14px; font-size: 14px; letter-spacing: 2px; border: 1px solid #639922; background: rgba(99, 153, 34, 0.16); }
.path-list { display: flex; flex-wrap: wrap; gap: 8px; margin-bottom: 20px; }
.path-item { font-size: 12px; color: var(--muted); padding: 4px 10px; border: 1px solid var(--line); }
.result-line { font-size: 24px; letter-spacing: 6px; color: #e8c372; margin-bottom: 10px; }

/* 炼丹 */
.recipe-list { display: flex; flex-direction: column; gap: 14px; }

/* ---------- 奇遇 / 炼制成败 ---------- */
.fortune-card {
  position: relative; margin: 0 0 18px; padding: 14px 18px;
  border: 1px solid rgba(232, 195, 114, 0.55);
  background: linear-gradient(120deg, rgba(232, 195, 114, 0.12), rgba(163, 49, 31, 0.08));
}
.ft-tag { position: absolute; right: 14px; top: 12px; font-size: 11px; letter-spacing: 3px; color: #e8c372; }
.fortune-card strong { font-size: 19px; letter-spacing: 4px; color: #e8c372; font-weight: normal; }
.fortune-card p { margin: 8px 0; font-size: 13px; line-height: 1.9; color: rgba(236, 228, 210, 0.85); }
.ft-gain { display: inline-block; margin-right: 10px; font-size: 12px; letter-spacing: 1px; color: #97c459; }
.craft-tip { margin: -6px 0 16px; font-size: 12px; line-height: 1.9; color: var(--muted); letter-spacing: 1px; }
.craft-result {
  display: flex; align-items: center; gap: 12px; flex-wrap: wrap;
  padding: 12px 16px; margin-bottom: 16px; font-size: 13px; line-height: 1.8;
  border: 1px solid rgba(151, 196, 89, 0.5); background: rgba(151, 196, 89, 0.09); color: #d6e8bd;
}
.craft-result.fail { border-color: rgba(232, 65, 43, 0.5); background: rgba(232, 65, 43, 0.09); color: #f0c9b8; }
.cr-tag { font-size: 12px; letter-spacing: 3px; color: #e8c372; }
.cr-refund { margin-left: auto; font-size: 12px; color: #e8c372; }
.recipe-rate { font-size: 11px; letter-spacing: 1px; color: #97c459; }
.recipe-rate.low { color: #e8412b; }
.recipe { padding: 16px 18px; border: 1px solid var(--line); background: rgba(236, 228, 210, 0.04); }
.recipe-head { display: flex; align-items: baseline; gap: 12px; }
.recipe-name { font-size: 20px; letter-spacing: 4px; }
.recipe-tag { font-size: 12px; padding: 2px 8px; border: 1px solid #c8543c; color: #f0c9b8; }
.recipe-out { margin-left: auto; font-size: 13px; color: #e8c372; letter-spacing: 2px; }
.recipe-desc { margin: 10px 0 12px; font-size: 12px; line-height: 1.8; color: var(--muted); }
.mat-row { display: flex; flex-wrap: wrap; gap: 8px; margin-bottom: 14px; }
.mat-chip { font-size: 12px; letter-spacing: 1px; padding: 4px 10px; border: 1px solid var(--line); color: rgba(236, 228, 210, 0.8); }
.mat-chip.is-lack { border-color: #e24b4a; color: #f0a89a; }
.mat-chip.cost { color: #e8c372; }

/* 对决 */
.record-list { display: flex; flex-direction: column; gap: 8px; }
.record { display: flex; align-items: center; gap: 14px; padding: 9px 12px; border: 1px solid var(--line); font-size: 13px; letter-spacing: 2px; }
.rec-name { flex: 1; }
.rec-score { color: var(--muted); }
.rec-result { font-size: 15px; }
.rec-result.win { color: #c0dd97; }
.rec-result.lose { color: #f0a89a; }
.rec-result.draw { color: #e8c372; }

/* 传书 */
.compose-row { display: flex; gap: 10px; margin-bottom: 22px; }
.sel, .txt {
  font: inherit; font-size: 14px; letter-spacing: 1px; color: var(--paper);
  background: rgba(236, 228, 210, 0.06); border: 1px solid var(--line); padding: 10px 12px;
}
.sel { flex: none; width: 150px; }
.sel option { background: #16201d; color: var(--paper); }
.txt { flex: 1; }
.letter-list { display: flex; flex-direction: column; gap: 10px; }
.conv-row { display: flex; gap: 10px; flex-wrap: wrap; margin-bottom: 16px; }
.conv {
  display: flex; align-items: center; gap: 10px; padding: 9px 14px; cursor: pointer;
  border: 1px solid var(--line); background: rgba(236, 228, 210, 0.04); font-size: 13px; letter-spacing: 1px;
}
.conv.is-unread { border-color: #e8412b; background: rgba(232, 65, 43, 0.1); }
.c-name { font-size: 14px; }
.c-last { max-width: 220px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; color: var(--muted); font-size: 12px; }
.c-count { min-width: 18px; height: 18px; display: grid; place-items: center; font-size: 11px; background: #a3311f; border-radius: 9px; }
.letter { padding: 13px 16px; border: 1px solid var(--line); background: rgba(236, 228, 210, 0.03); cursor: pointer; }
.letter.is-unread { border-left: 2px solid #e8412b; background: rgba(232, 65, 43, 0.07); }
.letter-head { display: flex; justify-content: space-between; margin-bottom: 8px; }
.from { font-size: 14px; letter-spacing: 2px; }
.time { font-size: 11px; color: var(--muted); }
.letter-body { font-size: 13px; line-height: 1.9; letter-spacing: 1px; }

/* 背包 */
.rail-title { padding-bottom: 12px; margin-bottom: 10px; font-size: 14px; letter-spacing: 4px; color: #e8c372; border-bottom: 1px solid var(--line); }
.rail-title small { float: right; font-size: 11px; color: var(--muted); }
.bag-list { display: flex; flex-direction: column; gap: 7px; }
.bag-item { display: flex; align-items: baseline; gap: 10px; padding: 8px 10px; border-left: 2px solid rgba(236, 228, 210, 0.2); background: rgba(236, 228, 210, 0.04); font-size: 13px; letter-spacing: 1px; }
.bag-name { flex: 1; }
.bag-count { color: #e8c372; }
.bag-grade { font-size: 11px; }
.log-empty { font-size: 12px; letter-spacing: 2px; color: var(--muted); text-align: center; padding: 20px 0; }

.main-btn {
  font: inherit; font-size: 15px; letter-spacing: 4px; cursor: pointer;
  padding: 11px 26px; color: #f6ece0;
  background: linear-gradient(180deg, rgba(163, 49, 31, 0.92), rgba(120, 33, 21, 0.92));
  border: 1px solid #c8543c; border-radius: 2px;
}
.main-btn:hover:not(:disabled) { filter: brightness(1.15); }
.main-btn:disabled { opacity: 0.4; cursor: not-allowed; }

.notice {
  position: fixed; left: 50%; bottom: 60px; transform: translateX(-50%); z-index: 9;
  padding: 10px 26px; font-size: 14px; letter-spacing: 2px;
  color: var(--paper); background: rgba(163, 49, 31, 0.92); border: 1px solid #c8543c;
}
.err-line { margin-top: 16px; font-size: 13px; color: #f0a89a; letter-spacing: 1px; }
.fade-enter-active, .fade-leave-active { transition: opacity 0.25s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }

@media (max-width: 1000px) {
  .wd-body { grid-template-columns: 1fr; }
  .wd-side { display: none; }
}
</style>
