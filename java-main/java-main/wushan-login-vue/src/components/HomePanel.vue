<script setup>
// 首页：登录成功后进入。风格延续登录页 —— 水墨 / 楷体 / 印章 / 五行换色 / 星火粒子。
// 导航四项内容：「观山」= 五行卡片 / 相生环 / 四时图卷 / 论道榜 + 大搜索（分级分类筛选）；
//   「藏经」→ 跳转藏经阁（App 层切视图）；「论道」= 论道堂（登台发表 + 服务端点赞 + 分页）；
//   「市集」= 雾山市集（分类/五行/品阶筛选 + 灵石购买 + 分页），数据首次切入时懒加载。
// 山门公告跑马灯 / 墨韵页脚 / 回顶按钮为全栏共用。
// 所有数据接口见 ../api/home.js —— 前端 mock 可跑，后端接上即用。
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { homeApi } from '../api/home'
import { ELEMENTS, findElement } from '../constants/wuxing'

const props = defineProps({
  username: { type: String, default: '' }
})
const emit = defineEmits(['select', 'logout', 'nav'])

/* ------------------------------ 导航栏 ------------------------------ */
const navItems = ref([
  { key: 'guanshan', label: '观山', en: 'scenery' },
  { key: 'cangjing', label: '藏经', en: 'codex' },
  { key: 'lundao', label: '论道', en: 'discourse' },
  { key: 'shiji', label: '市集', en: 'market' }
])
const activeNav = ref('guanshan')
// 各导航内容的懒加载标记：首次切入时才拉数据
const navLoaded = ref({ guanshan: true })

function switchNav(item) {
  activeNav.value = item.key
  // 藏经 → 藏经阁（App 层切视图）；观山/论道/市集在首页内切换内容
  if (item.key === 'cangjing') {
    emit('nav', item)
    return
  }
  ensureNavData(item.key)
}

/** 首次切入某导航时拉取对应数据（论道榜/市集） */
function ensureNavData(key) {
  if (navLoaded.value[key]) return
  navLoaded.value = { ...navLoaded.value, [key]: true }
  if (key === 'lundao') loadDq(true)
  if (key === 'shiji') loadMarket(true)
}

/* ------------------------------ 五行换色 ------------------------------ */
const activeElement = ref('huo')
function pickElement(el) {
  activeElement.value = el.key
  emit('select', { color: el.color, huo: el.huo })
}
const activeEl = computed(() => findElement(activeElement.value))

/* ------------------------------ 山门公告（跑马灯） ------------------------------ */
const announcements = ref([])
const marqueeText = computed(() =>
  announcements.value.map((a) => a.text).join('　◆　') + (announcements.value.length ? '　◆　' : '')
)

/* ------------------------------ 时辰问候 ------------------------------ */
const DIZHI = ['子', '丑', '寅', '卯', '辰', '巳', '午', '未', '申', '酉', '戌', '亥']
const YI_LIST = ['宜观山', '宜读经', '宜论道', '宜煮茶', '宜习武', '宜围炉']
const now = new Date()
const shichen = DIZHI[Math.floor(((now.getHours() + 1) % 24) / 2)]
const yi = YI_LIST[now.getHours() % YI_LIST.length]
const greeting = computed(() => `此刻山外 ${shichen} 时 · ${yi}`)

/* ------------------------------ 搜索栏（分级分类筛选 + 分页） ------------------------------ */
const keyword = ref('')
const searching = ref(false)
const searched = ref(false)
const results = ref([])
const hotKeywords = ref([])

// 分级分类筛选状态：分类树 + 品阶字典由后端提供（mock 模式下走 home.js 内置同口径数据）
const categories = ref([])   // [{ key, label, children: [{ key, label }] }]
const grades = ref([])       // [{ key, label, rank, color }]
const filterCategory = ref('all')
const filterGrade = ref('all')
const gradeAbove = ref(false) // 品阶「及以上」分级筛选开关
const total = ref(0)
const loadingMore = ref(false)
const PAGE_SIZE = 10

/** 组装当前筛选条件（category 传一级分类时后端自动下钻子类） */
function currentFilters(page) {
  const f = { page, size: PAGE_SIZE }
  if (filterCategory.value && filterCategory.value !== 'all') f.category = filterCategory.value
  if (filterGrade.value && filterGrade.value !== 'all') {
    if (gradeAbove.value) f.gradeMin = filterGrade.value
    else f.grade = filterGrade.value
  }
  return f
}

async function fetchSearch(kw, page) {
  const res = await homeApi.search(kw, currentFilters(page))
  const items = res.items || []
  if (page === 1) results.value = items
  else results.value = results.value.concat(items)
  total.value = res.total ?? items.length
  searched.value = true
}

async function doSearch(kw) {
  const q = String(kw ?? keyword.value).trim()
  // 无关键词且未选任何筛选时不搜；选了分类/品阶则允许空关键词浏览全类
  if (!q && filterCategory.value === 'all' && filterGrade.value === 'all') {
    keyword.value = ''
    return
  }
  keyword.value = q
  searching.value = true
  try {
    await fetchSearch(q, 1)
  } catch (e) {
    results.value = []
    total.value = 0
    searched.value = true
  } finally {
    searching.value = false
  }
}

async function loadMore() {
  if (loadingMore.value || !searched.value) return
  loadingMore.value = true
  try {
    await fetchSearch(keyword.value, Math.floor(results.value.length / PAGE_SIZE) + 1)
  } catch (e) {
    /* 加载失败保持现状 */
  } finally {
    loadingMore.value = false
  }
}

/** 已有结果时切换筛选即时重搜；尚未搜过则只记录选择 */
function rerunIfSearched() {
  if (searched.value) doSearch(keyword.value)
}
function pickCategory(k) {
  filterCategory.value = k
  rerunIfSearched()
}
function pickGrade(k) {
  filterGrade.value = k
  rerunIfSearched()
}
function toggleAbove() {
  gradeAbove.value = !gradeAbove.value
  rerunIfSearched()
}
function gradeColor(key) {
  const g = grades.value.find((x) => x.key === key)
  return g ? g.color : null
}

/* ------------------------------ 五行相生环 ------------------------------ */
// 相生次序：木生火 → 火生土 → 土生金 → 金生水 → 水生木（顺时针排布）
const ring = (() => {
  const order = ['mu', 'huo', 'tu', 'jin', 'shui']
  const cx = 190
  const cy = 190
  const R = 122
  const node = 34 // 节点半径
  const pts = order.map((k, i) => {
    const a = ((i * 72 - 90) * Math.PI) / 180
    return { k, x: cx + R * Math.cos(a), y: cy + R * Math.sin(a) }
  })
  const edges = order.map((k, i) => {
    const p1 = pts[i]
    const p2 = pts[(i + 1) % 5]
    const dx = p2.x - p1.x
    const dy = p2.y - p1.y
    const len = Math.hypot(dx, dy)
    const ux = dx / len
    const uy = dy / len
    return {
      key: k + '-' + order[(i + 1) % 5],
      x1: p1.x + ux * (node + 4),
      y1: p1.y + uy * (node + 4),
      x2: p2.x - ux * (node + 14),
      y2: p2.y - uy * (node + 14),
      lx: (p1.x + p2.x) / 2,
      ly: (p1.y + p2.y) / 2,
      text: findElement(k).label + '生' + findElement(order[(i + 1) % 5]).label
    }
  })
  return { order, pts, edges, cx, cy }
})()

/* ------------------------------ 山中四时 ------------------------------ */
const scenes = ref([])
function enterScene(s) {
  // 入景即搜：把图卷标题带入搜索（后续接入分页时可改为路由跳转）
  doSearch(s.title)
}

/* ------------------------------ 山友论道榜 ------------------------------ */
const discourse = ref([])
const liked = ref({})
function toggleZan(item) {
  liked.value = { ...liked.value, [item.id]: !liked.value[item.id] }
  item.zan += liked.value[item.id] ? 1 : -1
}

/* ------------------------------ 论道堂（导航「论道」：分页列表 + 服务端点赞 + 登台发表） ------------------------------ */
const DQ_SIZE = 8
const dq = ref([])            // 论道列表（按赞数降序）
const dqTotal = ref(0)
const dqLoading = ref(false)
const dqLiking = ref({})      // 点赞中的条目 id
const myQuote = ref('')
const publishing = ref(false)
const dqMsg = ref('')
let dqMsgTimer = null

function dqRank(i) {
  return i + 1 // 列表本身按赞数降序分页拼接，序号即全局名次
}

async function loadDq(reset) {
  if (dqLoading.value) return
  dqLoading.value = true
  try {
    const page = reset ? 1 : Math.floor(dq.value.length / DQ_SIZE) + 1
    const res = await homeApi.getDiscourseList({ page, size: DQ_SIZE })
    const items = res.items || []
    if (page === 1) dq.value = items
    else dq.value = dq.value.concat(items)
    dqTotal.value = res.total ?? items.length
  } catch (e) {
    if (reset) dq.value = []
  } finally {
    dqLoading.value = false
  }
}

/** 服务端点赞：后端自增并返回最新 zan（与观山页的本地乐观切换不同） */
async function dqLike(item) {
  if (dqLiking.value[item.id] || liked.value[item.id]) return
  dqLiking.value = { ...dqLiking.value, [item.id]: true }
  try {
    const r = await homeApi.likeDiscourse(item.id)
    if (r && typeof r.zan === 'number') item.zan = r.zan
    liked.value = { ...liked.value, [item.id]: true }
  } finally {
    dqLiking.value = { ...dqLiking.value, [item.id]: false }
  }
}

/** 登台发表（需登录，后端从 token 取名号与五行） */
async function publishQuote() {
  const q = myQuote.value.trim()
  if (!q || publishing.value) return
  publishing.value = true
  dqMsg.value = ''
  try {
    await homeApi.publishDiscourse(q)
    myQuote.value = ''
    dqMsg.value = '一言既出，驷马难追 —— 已登论道台。'
    await loadDq(true)
  } catch (e) {
    dqMsg.value = e && e.message ? e.message : '登台未成，稍后再试。'
  } finally {
    publishing.value = false
    clearTimeout(dqMsgTimer)
    dqMsgTimer = setTimeout(() => { dqMsg.value = '' }, 4000)
  }
}

/* ------------------------------ 市集（导航「市集」：筛选 + 分页 + 灵石购买） ------------------------------ */
const MK_SIZE = 12
const market = ref([])
const mkTotal = ref(0)
const mkLoading = ref(false)
const buying = ref({})       // 购买中的条目 id
const mkKeyword = ref('')
const mkElement = ref('all')
const mkGrade = ref('all')
const mkCategory = ref('all')
const mkMsg = ref('')
let mkMsgTimer = null

// 市集分类取「修炼资源」下的二级子类（灵药/炼材/丹药/法器），来自分类树
const mkCategories = computed(() => {
  const z = categories.value.find((c) => c.key === 'ziyuan')
  return (z && z.children) || []
})

async function fetchMarket(page) {
  const res = await homeApi.getMarketItems({
    keyword: mkKeyword.value.trim(),
    element: mkElement.value,
    grade: mkGrade.value,
    category: mkCategory.value,
    page,
    size: MK_SIZE
  })
  const items = res.items || []
  if (page === 1) market.value = items
  else market.value = market.value.concat(items)
  mkTotal.value = res.total ?? items.length
}

async function loadMarket(reset) {
  if (mkLoading.value) return
  mkLoading.value = true
  try {
    await fetchMarket(reset ? 1 : Math.floor(market.value.length / MK_SIZE) + 1)
  } catch (e) {
    if (reset) market.value = []
  } finally {
    mkLoading.value = false
  }
}

function rerunMarket() {
  if (navLoaded.value.shiji) loadMarket(true)
}
function pickMkCategory(k) { mkCategory.value = k; rerunMarket() }
function pickMkElement(k) { mkElement.value = k; rerunMarket() }
function pickMkGrade(k) { mkGrade.value = k; rerunMarket() }
function searchMarket() { rerunMarket() }

/** 购得一件：后端校验库存并扣减（防超卖），成功后刷新卡片 */
async function buyItem(m) {
  if (buying.value[m.id] || !m.stock) return
  buying.value = { ...buying.value, [m.id]: true }
  mkMsg.value = ''
  try {
    const r = await homeApi.buyMarketItem(m.id, 1)
    Object.assign(m, r)
    mkMsg.value = `已购得「${m.title}」，灵石 ${m.price} 枚成交。`
  } catch (e) {
    mkMsg.value = e && e.message ? e.message : '交易未成，稍后再试。'
    if (/售罄|库存不足|抢空/.test(mkMsg.value)) loadMarket(true)
  } finally {
    buying.value = { ...buying.value, [m.id]: false }
    clearTimeout(mkMsgTimer)
    mkMsgTimer = setTimeout(() => { mkMsg.value = '' }, 4000)
  }
}

/* ------------------------------ 用户信息 / 退出 ------------------------------ */
const userText = ref('')
const loggingOut = ref(false)

async function doLogout() {
  loggingOut.value = true
  try {
    // 预留接口：后端模式下会清除服务端会话，同时清掉本地 token
    await homeApi.logout()
    emit('logout')
  } finally {
    loggingOut.value = false
  }
}

/* ------------------------------ 滚动：回顶 + 渐显 ------------------------------ */
const homeEl = ref(null)
const showTop = ref(false)

function onScroll() {
  showTop.value = (homeEl.value && homeEl.value.scrollTop > 360) || window.scrollY > 360
}
function toTop() {
  if (homeEl.value) homeEl.value.scrollTo({ top: 0, behavior: 'smooth' })
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

let io = null

onMounted(async () => {
  // 拉取各模块数据（接口失败不阻塞页面）
  homeApi.getNav().then((l) => { if (l && l.length) navItems.value = l }).catch(() => {})
  homeApi.getHotKeywords().then((l) => { if (l) hotKeywords.value = l }).catch(() => {})
  homeApi.getAnnouncements().then((l) => { if (l && l.length) announcements.value = l }).catch(() => {})
  homeApi.getScenes().then((l) => { if (l && l.length) scenes.value = l }).catch(() => {})
  homeApi.getDiscourse().then((l) => { if (l) discourse.value = l }).catch(() => {})
  // 分类树 / 品阶字典（分级分类筛选用，接口失败不阻塞页面）
  homeApi.getCategories().then((l) => { if (l && l.length) categories.value = l }).catch(() => {})
  homeApi.getGrades().then((l) => { if (l && l.length) grades.value = l }).catch(() => {})
  homeApi.getUserInfo(props.username)
    .then((u) => { userText.value = u.username || props.username || '雾山行者' })
    .catch(() => { userText.value = props.username || '雾山行者' })

  // 滚动渐显
  if (typeof IntersectionObserver !== 'undefined') {
    io = new IntersectionObserver(
      (es) => es.forEach((e) => {
        if (e.isIntersecting) {
          e.target.classList.add('in')
          io.unobserve(e.target)
        }
      }),
      { threshold: 0.12, root: homeEl.value }
    )
    document.querySelectorAll('.reveal').forEach((el) => io.observe(el))
  }
})

onBeforeUnmount(() => {
  if (io) io.disconnect()
})
</script>

<template>
  <div class="home" ref="homeEl" @scroll.passive="onScroll">
    <!-- ============ 顶部导航栏 ============ -->
    <header class="navbar">
      <div class="brand">
        <div class="seal">雾<br />山</div>
        <div class="brand-text">
          <span class="name">雾山</span>
          <small>五 行 · 境 内</small>
        </div>
      </div>

      <nav class="nav">
        <div
          v-for="item in navItems"
          :key="item.key"
          class="nav-item"
          :class="{ 'is-on': activeNav === item.key }"
          @click="switchNav(item)"
        >
          {{ item.label }}<small>{{ item.en }}</small>
        </div>
      </nav>

      <div class="nav-right">
        <!-- 迷你五行换色器 -->
        <div class="wx-mini">
          <span
            v-for="el in ELEMENTS"
            :key="el.key"
            class="wx-dot"
            :class="[el.key, { 'is-on': activeElement === el.key }]"
            :title="el.label + ' · ' + el.trait"
            @click="pickElement(el)"
          >{{ el.label }}</span>
        </div>

        <span class="user">{{ userText }}</span>

        <button class="out-btn" :disabled="loggingOut" @click="doLogout">
          {{ loggingOut ? '出 山…' : '出 山' }}
        </button>
      </div>
    </header>

    <!-- ============ 山门公告（跑马灯） ============ -->
    <div class="announce" v-if="announcements.length">
      <span class="announce-tag">山 门 公 告</span>
      <div class="announce-track">
        <div class="announce-move">
          <span>{{ marqueeText }}</span>
          <span>{{ marqueeText }}</span>
        </div>
      </div>
    </div>

    <!-- ============ 主体内容 ============ -->
    <main class="body">
      <!-- 搜索区 -->
      <section class="hero">
        <svg class="brush" viewBox="0 0 150 46">
          <path d="M6,38 C40,8 90,6 144,14 C120,22 70,30 30,40 C20,42 12,42 6,38 Z" fill="var(--accent)" />
        </svg>

        <div class="greeting">{{ greeting }}</div>
        <h1 class="hero-title">雾山寻物</h1>
        <p class="hero-sub">五行归位 · 万象可寻</p>

        <div class="search-bar">
          <input
            v-model="keyword"
            placeholder="入山搜物…阵法、秘录、灵草，皆可寻也"
            autocomplete="off"
            @keydown.enter="doSearch()"
          />
          <button class="search-btn" :disabled="searching" @click="doSearch()">
            {{ searching ? '寻…' : '寻' }}
          </button>
        </div>

        <div class="hot" v-if="hotKeywords.length">
          <span class="hot-label">山 中 热 搜：</span>
          <a
            v-for="h in hotKeywords"
            :key="h.keyword"
            href="#"
            class="hot-tag"
            :class="'t-' + h.element"
            @click.prevent="doSearch(h.keyword)"
          >{{ h.keyword }}</a>
        </div>

        <!-- 分级分类筛选：一级分类含二级子类（虚线为二级），品阶支持「及以上」分级 -->
        <div class="filters" v-if="categories.length || grades.length">
          <div class="f-row" v-if="categories.length">
            <span class="f-label">分 类</span>
            <span class="f-chip" :class="{ on: filterCategory === 'all' }" @click="pickCategory('all')">全部</span>
            <template v-for="c in categories" :key="c.key">
              <span class="f-chip" :class="{ on: filterCategory === c.key }" @click="pickCategory(c.key)">{{ c.label }}</span>
              <span
                v-for="s in c.children"
                :key="s.key"
                class="f-chip sub"
                :class="{ on: filterCategory === s.key }"
                @click="pickCategory(s.key)"
              >{{ s.label }}</span>
            </template>
          </div>
          <div class="f-row" v-if="grades.length">
            <span class="f-label">品 阶</span>
            <span class="f-chip" :class="{ on: filterGrade === 'all' }" @click="pickGrade('all')">不限</span>
            <span
              v-for="g in grades"
              :key="g.key"
              class="f-chip"
              :class="{ on: filterGrade === g.key }"
              :style="filterGrade === g.key ? { color: g.color, borderColor: g.color, boxShadow: '0 0 10px ' + g.color + '40' } : null"
              @click="pickGrade(g.key)"
            >{{ g.label }}</span>
            <span class="f-above" :class="{ on: gradeAbove }" @click="toggleAbove">及以上</span>
          </div>
        </div>
      </section>

      <!-- 五行卡片（观山） -->
      <section class="cards reveal" v-show="activeNav === 'guanshan'">
        <div
          v-for="el in ELEMENTS"
          :key="el.key"
          class="card"
          :class="[el.key, { 'is-on': activeElement === el.key }]"
          @click="pickElement(el)"
        >
          <div class="card-glyph">{{ el.label }}</div>
          <div class="card-name">{{ el.label }}<small>{{ el.en }}</small></div>
          <div class="card-trait">{{ el.trait }}</div>
        </div>
      </section>

      <!-- 搜索结果 -->
      <section class="results reveal" v-if="searched">
        <div class="results-head">
          {{ keyword ? `「${keyword}」之 所 寻` : '山 中 寻 物' }} · 共 {{ total }} 条
        </div>
        <div v-if="results.length" class="results-list">
          <div v-for="r in results" :key="r.id" class="result-item">
            <span class="r-tag" :class="'t-' + r.element">{{ findElement(r.element).label }}</span>
            <span class="r-title">{{ r.title }}</span>
            <span class="r-meta" v-if="r.categoryLabel || r.gradeLabel">
              <span class="r-cat" v-if="r.categoryLabel">{{ r.categoryLabel }}</span>
              <span
                class="r-grade"
                v-if="r.gradeLabel"
                :style="gradeColor(r.grade) ? { color: gradeColor(r.grade), borderColor: gradeColor(r.grade) } : null"
              >{{ r.gradeLabel }}</span>
            </span>
            <span class="r-desc">{{ r.desc }}</span>
          </div>
          <!-- 分页加载更多 -->
          <div class="more-wrap" v-if="results.length < total">
            <button class="more-btn" :disabled="loadingMore" @click="loadMore">
              {{ loadingMore ? '寻 …' : '再 寻 十 条' }}
            </button>
          </div>
        </div>
        <div v-else class="results-empty">山中无此物，换个说法再寻。</div>
      </section>

      <!-- 五行相生环（观山） -->
      <section class="block reveal" v-show="activeNav === 'guanshan'">
        <h2 class="block-title">五 行 相 生</h2>
        <p class="block-sub">木生火 · 火生土 · 土生金 · 金生水 · 水生木，周而复始</p>
        <div class="ring-wrap">
          <svg class="ring" viewBox="0 0 380 380">
            <defs>
              <marker id="arrow" viewBox="0 0 10 10" refX="8" refY="5" markerWidth="7" markerHeight="7" orient="auto-start-reverse">
                <path d="M0,0 L10,5 L0,10 z" fill="var(--accent)" opacity="0.75" />
              </marker>
            </defs>
            <!-- 外圈虚线（周天） -->
            <circle :cx="ring.cx" :cy="ring.cy" r="158" fill="none" stroke="var(--line)" stroke-width="1" stroke-dasharray="3 7" />
            <circle :cx="ring.cx" :cy="ring.cy" r="86" fill="none" stroke="var(--line)" stroke-width="1" stroke-dasharray="2 6" />
            <!-- 相生箭头 -->
            <line
              v-for="e in ring.edges"
              :key="e.key"
              :x1="e.x1" :y1="e.y1" :x2="e.x2" :y2="e.y2"
              stroke="var(--accent)"
              stroke-width="1.6"
              opacity="0.65"
              marker-end="url(#arrow)"
            />
          </svg>
          <!-- 环上五个节点 -->
          <div
            v-for="(p, i) in ring.pts"
            :key="p.k"
            class="ring-node"
            :class="[p.k, { 'is-on': activeElement === p.k }]"
            :style="{ left: p.x + 'px', top: p.y + 'px' }"
            @click="pickElement(findElement(p.k))"
          >
            {{ findElement(p.k).label }}
          </div>
          <!-- 相生关系标签 -->
          <span
            v-for="e in ring.edges"
            :key="'l' + e.key"
            class="ring-label"
            :style="{ left: e.lx + 'px', top: e.ly + 'px' }"
          >{{ e.text }}</span>
          <!-- 环心：本命 -->
          <div class="ring-core" :class="'t-' + activeElement">
            <span class="core-glyph">{{ activeEl.label }}</span>
            <span class="core-trait">{{ activeEl.trait }}</span>
            <span class="core-hint">本 命</span>
          </div>
        </div>
      </section>

      <!-- 山中四时图卷（观山） -->
      <section class="block reveal" v-if="scenes.length" v-show="activeNav === 'guanshan'">
        <h2 class="block-title">山 中 四 时</h2>
        <p class="block-sub">春夏秋冬 · 山色各异 · 入景即寻</p>
        <div class="scenes">
          <div v-for="s in scenes" :key="s.key" class="scene" :style="{ '--tint': s.tint }">
            <svg class="scene-art" viewBox="0 0 260 90" preserveAspectRatio="none">
              <path :fill="s.tint" opacity="0.28" d="M0,70 C40,34 70,52 100,40 C130,26 150,56 180,44 C210,32 240,60 260,48 L260,90 L0,90 Z" />
              <path :fill="s.tint" opacity="0.5" d="M0,80 C50,54 90,70 130,62 C170,54 200,74 260,64 L260,90 L0,90 Z" />
            </svg>
            <div class="scene-season">{{ s.season }}</div>
            <div class="scene-title">{{ s.title }}<small>{{ s.en }}</small></div>
            <div class="scene-desc">{{ s.desc }}</div>
            <button class="scene-btn" @click="enterScene(s)">入 景</button>
          </div>
        </div>
      </section>

      <!-- 山友论道榜（观山 · 精选十席） -->
      <section class="block reveal" v-if="discourse.length" v-show="activeNav === 'guanshan'">
        <h2 class="block-title">山 友 论 道</h2>
        <p class="block-sub">论道台 · 山友之言 · 以赞为证</p>
        <div class="discourse">
          <div v-for="(d, i) in discourse" :key="d.id" class="ds-item">
            <div class="ds-rank" :class="{ top: i < 3 }">{{ i + 1 }}</div>
            <div class="ds-avatar" :class="'t-' + d.element">{{ findElement(d.element).label }}</div>
            <div class="ds-main">
              <div class="ds-name">{{ d.name }}<span class="ds-tag" :class="'t-' + d.element">{{ findElement(d.element).label }}行</span></div>
              <div class="ds-quote">「{{ d.quote }}」</div>
            </div>
            <button class="ds-zan" :class="{ liked: liked[d.id] }" @click="toggleZan(d)">
              {{ liked[d.id] ? '已 服' : '叹 服' }} · {{ d.zan }}
            </button>
          </div>
        </div>
      </section>

      <!-- ============ 论道堂（导航「论道」：登台发表 + 服务端点赞 + 分页） ============ -->
      <section class="block lundao-panel" v-show="activeNav === 'lundao'">
        <h2 class="block-title">论 道 台</h2>
        <p class="block-sub">山友之言 · 以赞为证 · 登台一语</p>

        <!-- 登台发表 -->
        <div class="dq-publish">
          <textarea
            v-model="myQuote"
            rows="2"
            maxlength="200"
            placeholder="登台一语，说与山中人听……（至多 200 字）"
          ></textarea>
          <button class="dq-btn" :disabled="publishing || !myQuote.trim()" @click="publishQuote">
            {{ publishing ? '落 笔…' : '登 台' }}
          </button>
        </div>
        <p class="dq-msg" v-if="dqMsg">{{ dqMsg }}</p>

        <!-- 论道列表 -->
        <div class="dq-list" v-if="dq.length">
          <div v-for="(d, i) in dq" :key="d.id" class="ds-item">
            <div class="ds-rank" :class="{ top: dqRank(i) <= 3 }">{{ dqRank(i) }}</div>
            <div class="ds-avatar" :class="'t-' + d.element">{{ findElement(d.element).label }}</div>
            <div class="ds-main">
              <div class="ds-name">{{ d.name }}<span class="ds-tag" :class="'t-' + d.element">{{ findElement(d.element).label }}行</span></div>
              <div class="ds-quote">「{{ d.quote }}」</div>
            </div>
            <button
              class="ds-zan"
              :class="{ liked: liked[d.id] }"
              :disabled="dqLiking[d.id]"
              @click="dqLike(d)"
            >
              {{ dqLiking[d.id] ? '赞…' : liked[d.id] ? '已 服' : '叹 服' }} · {{ d.zan }}
            </button>
          </div>
        </div>
        <div class="results-empty" v-else-if="!dqLoading">论道台尚无人语，不如你先登台。</div>

        <!-- 分页再观 -->
        <div class="more-wrap" v-if="dq.length < dqTotal">
          <button class="more-btn" :disabled="dqLoading" @click="loadDq(false)">
            {{ dqLoading ? '观 …' : '再 观 八 席' }}
          </button>
        </div>
      </section>

      <!-- ============ 市集（导航「市集」：筛选 + 灵石购买 + 分页） ============ -->
      <section class="block market-panel" v-show="activeNav === 'shiji'">
        <h2 class="block-title">雾 山 市 集</h2>
        <p class="block-sub">以灵石易物 · 童叟无欺 · 售罄即止</p>

        <!-- 市集筛选 -->
        <div class="filters mk-filters">
          <div class="f-row" v-if="mkCategories.length">
            <span class="f-label">分 类</span>
            <span class="f-chip" :class="{ on: mkCategory === 'all' }" @click="pickMkCategory('all')">全部</span>
            <span
              v-for="c in mkCategories"
              :key="c.key"
              class="f-chip"
              :class="{ on: mkCategory === c.key }"
              @click="pickMkCategory(c.key)"
            >{{ c.label }}</span>
          </div>
          <div class="f-row">
            <span class="f-label">五 行</span>
            <span class="f-chip" :class="{ on: mkElement === 'all' }" @click="pickMkElement('all')">不限</span>
            <span
              v-for="el in ELEMENTS"
              :key="el.key"
              class="f-chip"
              :class="{ on: mkElement === el.key }"
              @click="pickMkElement(el.key)"
            >{{ el.label }}</span>
          </div>
          <div class="f-row" v-if="grades.length">
            <span class="f-label">品 阶</span>
            <span class="f-chip" :class="{ on: mkGrade === 'all' }" @click="pickMkGrade('all')">不限</span>
            <span
              v-for="g in grades"
              :key="g.key"
              class="f-chip"
              :class="{ on: mkGrade === g.key }"
              :style="mkGrade === g.key ? { color: g.color, borderColor: g.color, boxShadow: '0 0 10px ' + g.color + '40' } : null"
              @click="pickMkGrade(g.key)"
            >{{ g.label }}</span>
          </div>
          <div class="f-row">
            <span class="f-label">寻 物</span>
            <input
              v-model="mkKeyword"
              class="mk-search"
              placeholder="市集中寻物…回车即查"
              @keydown.enter="searchMarket()"
            />
          </div>
        </div>

        <p class="dq-msg" v-if="mkMsg">{{ mkMsg }}</p>

        <!-- 物品卡片 -->
        <div class="mk-grid" v-if="market.length">
          <div v-for="m in market" :key="m.id" class="mk-card" :class="{ gone: !m.stock }">
            <div class="mk-head">
              <span class="r-tag" :class="'t-' + m.element">{{ findElement(m.element).label }}</span>
              <span
                class="r-grade"
                v-if="m.gradeLabel"
                :style="gradeColor(m.grade) ? { color: gradeColor(m.grade), borderColor: gradeColor(m.grade) } : null"
              >{{ m.gradeLabel }}</span>
              <span class="r-cat" v-if="m.categoryLabel">{{ m.categoryLabel }}</span>
            </div>
            <div class="mk-title">{{ m.title }}</div>
            <div class="mk-desc">{{ m.desc }}</div>
            <div class="mk-foot">
              <span class="mk-price">{{ m.price }}<small>灵石</small></span>
              <span class="mk-stock">{{ m.stock ? `余 ${m.stock} 件` : '已售罄' }}</span>
              <button class="mk-buy" :disabled="!m.stock || buying[m.id]" @click="buyItem(m)">
                {{ buying[m.id] ? '成交…' : m.stock ? '购 得' : '无 货' }}
              </button>
            </div>
            <div class="mk-seller">摊主 · {{ m.seller }}<template v-if="m.sold"> · 已售 {{ m.sold }}</template></div>
          </div>
        </div>
        <div class="results-empty" v-else-if="!mkLoading">市集暂无此物，改日再来。</div>

        <!-- 分页再看 -->
        <div class="more-wrap" v-if="market.length < mkTotal">
          <button class="more-btn" :disabled="mkLoading" @click="loadMarket(false)">
            {{ mkLoading ? '逛 …' : '再 看 一 批' }}
          </button>
        </div>
      </section>

      <!-- 墨韵页脚 -->
      <footer class="footer reveal">
        <div class="footer-seal">雾<br />山</div>
        <div class="footer-line"></div>
        <p class="footer-text">雾 山 五 行 · 境 内 一 隅</p>
        <p class="footer-sub">楷体水墨 · 五山武星 · 山中一日 · 世上千年</p>
      </footer>
    </main>

    <!-- 回顶按钮 -->
    <Transition name="fade">
      <button class="to-top" v-if="showTop" title="回山脚" @click="toTop">顶</button>
    </Transition>
  </div>
</template>

<style scoped>
/* ============================== 顶部导航栏 ============================== */
.navbar {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 10;
  display: flex;
  align-items: center;
  gap: 28px;
  padding: 14px 28px;
  background: linear-gradient(180deg, rgba(14, 13, 12, 0.92), rgba(14, 13, 12, 0.6));
  border-bottom: 1px solid var(--line);
  backdrop-filter: blur(6px);
}
.brand {
  display: flex;
  align-items: center;
  gap: 12px;
}
.seal {
  width: 42px;
  height: 42px;
  border-radius: 6px;
  background: var(--accent);
  color: #fff8ee;
  display: grid;
  place-items: center;
  font-size: 15px;
  line-height: 1.1;
  text-align: center;
  border: 2px solid #9c2418;
  transform: rotate(-4deg);
  box-shadow: inset 0 0 0 2px rgba(255, 248, 238, 0.22), 0 3px 8px rgba(0, 0, 0, 0.4);
}
.brand-text .name {
  font-size: 24px;
  letter-spacing: 6px;
  color: var(--accent);
  text-shadow: 0 0 14px rgba(232, 65, 43, 0.45);
  font-weight: 700;
  line-height: 1;
}
.brand-text small {
  display: block;
  font-size: 11px;
  letter-spacing: 4px;
  color: var(--muted);
  margin-top: 4px;
}

.nav {
  display: flex;
  gap: 6px;
  flex: 1;
  justify-content: center;
}
.nav-item {
  padding: 8px 22px;
  border-radius: 8px;
  cursor: pointer;
  font-size: 17px;
  letter-spacing: 4px;
  color: var(--paper);
  border: 1px solid transparent;
  transition: 0.2s;
  text-align: center;
}
.nav-item small {
  display: block;
  font-size: 10px;
  opacity: 0.55;
  letter-spacing: 1px;
  margin-top: 2px;
}
.nav-item:hover {
  transform: translateY(-2px);
  background: rgba(236, 228, 210, 0.05);
}
.nav-item.is-on {
  border-color: var(--accent);
  background: rgba(236, 228, 210, 0.05);
  box-shadow: 0 0 14px rgba(232, 65, 43, 0.25);
}

.nav-right {
  display: flex;
  align-items: center;
  gap: 16px;
}
/* 迷你五行换色器（与登录页 .wx 同色） */
.wx-mini {
  display: flex;
  gap: 6px;
}
.wx-dot {
  width: 30px;
  height: 30px;
  display: grid;
  place-items: center;
  border-radius: 7px;
  font-size: 14px;
  cursor: pointer;
  border: 1px solid rgba(236, 228, 210, 0.18);
  background: rgba(236, 228, 210, 0.04);
  transition: 0.2s;
}
.wx-dot:hover {
  transform: translateY(-2px);
}
.wx-dot.jin { color: #e8c372; }
.wx-dot.mu  { color: #4caf6e; }
.wx-dot.shui{ color: #3a8fd0; }
.wx-dot.huo { color: #e8412b; }
.wx-dot.tu  { color: #c8923a; }
.wx-dot.feng { color: #7fd8cf; }
.wx-dot.lei { color: #9d7bf0; }
.wx-dot.dian { color: #f2e34d; }
.wx-dot.taiji { color: #d8d2c2; }
.wx-dot.yinyang { color: #98a4b0; }
.wx-dot.is-on {
  box-shadow: 0 0 0 2px currentColor, 0 4px 12px rgba(0, 0, 0, 0.4);
}

.user {
  font-size: 15px;
  letter-spacing: 2px;
  color: var(--paper);
}
.out-btn {
  padding: 8px 18px;
  font-family: inherit;
  font-size: 15px;
  letter-spacing: 4px;
  color: var(--paper);
  background: rgba(236, 228, 210, 0.05);
  border: 1px solid var(--line);
  border-radius: 6px;
  cursor: pointer;
  transition: 0.2s;
}
.out-btn:hover {
  border-color: var(--accent);
  color: var(--accent);
  box-shadow: 0 0 12px rgba(232, 65, 43, 0.3);
}

/* ============================== 山门公告 ============================== */
.announce {
  position: fixed;
  top: 71px;
  left: 0;
  right: 0;
  z-index: 9;
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 7px 28px;
  background: rgba(10, 10, 9, 0.72);
  border-bottom: 1px solid rgba(236, 228, 210, 0.08);
  backdrop-filter: blur(4px);
  overflow: hidden;
}
.announce-tag {
  flex-shrink: 0;
  font-size: 12px;
  letter-spacing: 3px;
  color: #fff8ee;
  background: var(--accent);
  border: 1px solid #9c2418;
  padding: 3px 10px;
  border-radius: 4px;
  transform: rotate(-1deg);
}
.announce-track {
  flex: 1;
  overflow: hidden;
  white-space: nowrap;
  mask-image: linear-gradient(90deg, transparent, #000 6%, #000 94%, transparent);
  -webkit-mask-image: linear-gradient(90deg, transparent, #000 6%, #000 94%, transparent);
}
.announce-move {
  display: inline-flex;
  animation: marquee 36s linear infinite;
}
.announce-move span {
  font-size: 13px;
  letter-spacing: 2px;
  color: var(--muted);
  padding-right: 30px;
}
@keyframes marquee {
  from { transform: translateX(0); }
  to { transform: translateX(-50%); }
}

/* ============================== 主体 ============================== */
.home {
  position: relative;
  z-index: 2;
  height: 100vh;
  overflow-y: auto;
  scroll-behavior: smooth;
}
.body {
  min-height: 100%;
  padding: 168px 24px 40px;
  max-width: 980px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  align-items: center;
}

/* ---------- 搜索区 ---------- */
.hero {
  position: relative;
  text-align: center;
  width: 100%;
}
.brush {
  position: absolute;
  top: -30px;
  left: 50%;
  margin-left: -160px;
  width: 150px;
  height: 46px;
  opacity: 0.9;
  filter: drop-shadow(0 4px 10px rgba(232, 65, 43, 0.5));
  pointer-events: none;
}
.greeting {
  font-size: 13px;
  letter-spacing: 6px;
  color: var(--muted);
  margin-bottom: 14px;
}
.hero-title {
  font-size: 56px;
  letter-spacing: 14px;
  color: var(--paper);
  text-shadow: 0 0 20px rgba(232, 65, 43, 0.35), 0 2px 2px rgba(0, 0, 0, 0.5);
  font-weight: 700;
}
.hero-sub {
  margin-top: 10px;
  font-size: 14px;
  letter-spacing: 8px;
  color: var(--muted);
}

.search-bar {
  margin: 34px auto 0;
  max-width: 620px;
  display: flex;
  gap: 12px;
  align-items: stretch;
}
.search-bar input {
  flex: 1;
  padding: 14px 20px;
  font-family: inherit;
  font-size: 17px;
  letter-spacing: 2px;
  color: var(--paper);
  background: rgba(18, 18, 16, 0.72);
  border: 1px solid var(--line);
  border-radius: 6px;
  outline: none;
  transition: 0.25s;
}
.search-bar input::placeholder {
  color: rgba(236, 228, 210, 0.3);
}
.search-bar input:focus {
  border-color: var(--accent);
  box-shadow: 0 0 16px rgba(232, 65, 43, 0.25), inset 0 0 30px rgba(232, 65, 43, 0.05);
}
.search-btn {
  padding: 14px 30px;
  font-family: inherit;
  font-size: 19px;
  letter-spacing: 4px;
  color: #fff8ee;
  background: var(--accent);
  border: 2px solid #9c2418;
  border-radius: 6px;
  cursor: pointer;
  transition: 0.18s;
  box-shadow: inset 0 0 0 2px rgba(255, 248, 238, 0.2), 0 5px 16px rgba(232, 65, 43, 0.45);
}
.search-btn:hover {
  transform: scale(1.04);
  filter: brightness(1.08);
}
.search-btn:disabled {
  opacity: 0.85;
  cursor: default;
}

.hot {
  margin-top: 20px;
  font-size: 13px;
  letter-spacing: 1px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  flex-wrap: wrap;
}
.hot-label {
  color: var(--muted);
  letter-spacing: 3px;
}
.hot-tag {
  color: var(--paper);
  text-decoration: none;
  padding: 4px 14px;
  border-radius: 20px;
  border: 1px solid var(--line);
  background: rgba(236, 228, 210, 0.04);
  transition: 0.2s;
}
.hot-tag:hover {
  transform: translateY(-2px);
  border-color: currentColor;
}
.hot-tag.t-jin  { color: #e8c372; }
.hot-tag.t-mu   { color: #4caf6e; }
.hot-tag.t-shui { color: #3a8fd0; }
.hot-tag.t-huo  { color: #e8412b; }
.hot-tag.t-tu   { color: #c8923a; }
.hot-tag.t-feng { color: #7fd8cf; }
.hot-tag.t-lei { color: #9d7bf0; }
.hot-tag.t-dian { color: #f2e34d; }
.hot-tag.t-taiji { color: #d8d2c2; }
.hot-tag.t-yinyang { color: #98a4b0; }

/* ---------- 分级分类筛选 ---------- */
.filters {
  margin-top: 20px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  align-items: center;
  font-size: 13px;
}
.f-row {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-wrap: wrap;
  gap: 8px;
}
.f-label {
  color: var(--muted);
  letter-spacing: 3px;
  margin-right: 2px;
}
.f-chip {
  padding: 4px 14px;
  border-radius: 20px;
  border: 1px solid var(--line);
  background: rgba(236, 228, 210, 0.04);
  color: var(--paper);
  cursor: pointer;
  letter-spacing: 1px;
  transition: 0.2s;
  user-select: none;
}
.f-chip:hover {
  transform: translateY(-2px);
  border-color: currentColor;
}
.f-chip.on {
  border-color: var(--accent);
  color: var(--accent);
  background: rgba(232, 65, 43, 0.08);
  box-shadow: 0 0 10px rgba(232, 65, 43, 0.25);
}
.f-chip.sub {
  padding: 3px 10px;
  font-size: 12px;
  border-style: dashed;
  opacity: 0.88;
}
.f-above {
  padding: 3px 12px;
  font-size: 12px;
  letter-spacing: 2px;
  border-radius: 20px;
  border: 1px dashed var(--line);
  color: var(--muted);
  cursor: pointer;
  transition: 0.2s;
  user-select: none;
}
.f-above:hover {
  transform: translateY(-2px);
}
.f-above.on {
  color: #f2e34d;
  border-color: #f2e34d;
  box-shadow: 0 0 10px rgba(242, 227, 77, 0.2);
}

/* ---------- 五行卡片 ---------- */
.cards {
  margin-top: 52px;
  display: flex;
  gap: 14px;
  flex-wrap: wrap;
  justify-content: center;
  width: 100%;
}
.card {
  width: 150px;
  padding: 20px 12px 16px;
  border-radius: 8px;
  text-align: center;
  cursor: pointer;
  border: 1px solid rgba(236, 228, 210, 0.16);
  background: linear-gradient(180deg, rgba(18, 18, 16, 0.72), rgba(10, 10, 9, 0.78));
  box-shadow: 0 18px 50px rgba(0, 0, 0, 0.45);
  transition: 0.22s;
  color: var(--paper);
}
.card:hover {
  transform: translateY(-5px);
}
.card.is-on {
  box-shadow: 0 0 0 2px currentColor, 0 18px 50px rgba(0, 0, 0, 0.5);
}
.card-glyph {
  font-size: 40px;
  line-height: 1;
  margin-bottom: 10px;
}
.card-name {
  font-size: 16px;
  letter-spacing: 3px;
}
.card-name small {
  font-size: 10px;
  opacity: 0.6;
  letter-spacing: 1px;
  margin-left: 4px;
}
.card-trait {
  margin-top: 8px;
  font-size: 12px;
  letter-spacing: 2px;
  color: var(--muted);
}
.card.jin  { color: #e8c372; }
.card.mu   { color: #4caf6e; }
.card.shui { color: #3a8fd0; }
.card.huo  { color: #e8412b; }
.card.tu   { color: #c8923a; }
.card.feng { color: #7fd8cf; }
.card.lei { color: #9d7bf0; }
.card.dian { color: #f2e34d; }
.card.taiji { color: #d8d2c2; }
.card.yinyang { color: #98a4b0; }

/* ---------- 搜索结果 ---------- */
.results {
  margin-top: 48px;
  width: 100%;
  max-width: 680px;
}
.results-head {
  font-size: 15px;
  letter-spacing: 4px;
  color: var(--muted);
  border-bottom: 1px solid var(--line);
  padding-bottom: 12px;
  text-align: center;
}
.results-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-top: 14px;
}
.result-item {
  display: flex;
  align-items: baseline;
  gap: 12px;
  padding: 14px 18px;
  border-radius: 8px;
  border: 1px solid rgba(236, 228, 210, 0.12);
  background: rgba(18, 18, 16, 0.6);
  transition: 0.2s;
}
.result-item:hover {
  border-color: var(--line);
  transform: translateX(4px);
}
.r-tag {
  flex-shrink: 0;
  width: 26px;
  height: 26px;
  display: grid;
  place-items: center;
  border-radius: 6px;
  font-size: 14px;
  border: 1px solid currentColor;
  transform: translateY(4px);
}
.r-tag.t-jin  { color: #e8c372; }
.r-tag.t-mu   { color: #4caf6e; }
.r-tag.t-shui { color: #3a8fd0; }
.r-tag.t-huo  { color: #e8412b; }
.r-tag.t-tu   { color: #c8923a; }
.r-tag.t-feng { color: #7fd8cf; }
.r-tag.t-lei { color: #9d7bf0; }
.r-tag.t-dian { color: #f2e34d; }
.r-tag.t-taiji { color: #d8d2c2; }
.r-tag.t-yinyang { color: #98a4b0; }
/* 结果条目的分类 / 品阶标签 */
.r-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-shrink: 0;
}
.r-cat,
.r-grade {
  font-size: 11px;
  letter-spacing: 1px;
  padding: 2px 8px;
  border-radius: 4px;
  border: 1px solid var(--line);
  color: var(--muted);
  white-space: nowrap;
}
.r-title {
  font-size: 17px;
  letter-spacing: 2px;
  color: var(--paper);
  white-space: nowrap;
}
.r-desc {
  font-size: 13px;
  letter-spacing: 1px;
  color: var(--muted);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.results-empty {
  margin-top: 18px;
  text-align: center;
  font-size: 14px;
  letter-spacing: 3px;
  color: var(--muted);
}
/* 分页加载更多 */
.more-wrap {
  margin-top: 16px;
  display: flex;
  justify-content: center;
}
.more-btn {
  padding: 8px 26px;
  border-radius: 20px;
  border: 1px solid var(--line);
  background: rgba(236, 228, 210, 0.04);
  color: var(--paper);
  font-size: 13px;
  letter-spacing: 3px;
  cursor: pointer;
  transition: 0.2s;
}
.more-btn:hover {
  border-color: var(--accent);
  color: var(--accent);
  transform: translateY(-2px);
}
.more-btn:disabled {
  opacity: 0.6;
  cursor: default;
  transform: none;
}

/* ---------- 区块通用标题 ---------- */
.block {
  margin-top: 76px;
  width: 100%;
  text-align: center;
}
.block-title {
  font-size: 30px;
  letter-spacing: 12px;
  color: var(--paper);
  font-weight: 700;
  text-shadow: 0 0 14px rgba(232, 65, 43, 0.25);
}
.block-sub {
  margin-top: 10px;
  font-size: 13px;
  letter-spacing: 4px;
  color: var(--muted);
}

/* ---------- 五行相生环 ---------- */
.ring-wrap {
  position: relative;
  width: 380px;
  height: 380px;
  margin: 30px auto 0;
}
.ring {
  width: 100%;
  height: 100%;
}
.ring-node {
  position: absolute;
  width: 68px;
  height: 68px;
  margin: -34px 0 0 -34px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  font-size: 26px;
  cursor: pointer;
  border: 1px solid rgba(236, 228, 210, 0.25);
  background: linear-gradient(180deg, rgba(18, 18, 16, 0.92), rgba(10, 10, 9, 0.96));
  transition: 0.22s;
  box-shadow: 0 10px 26px rgba(0, 0, 0, 0.5);
}
.ring-node:hover {
  transform: scale(1.1);
}
.ring-node.is-on {
  box-shadow: 0 0 0 2px currentColor, 0 0 26px rgba(0, 0, 0, 0.55);
  transform: scale(1.12);
}
.ring-node.jin  { color: #e8c372; }
.ring-node.mu   { color: #4caf6e; }
.ring-node.shui { color: #3a8fd0; }
.ring-node.huo  { color: #e8412b; }
.ring-node.tu   { color: #c8923a; }
.ring-label {
  position: absolute;
  margin: -9px 0 0 -22px;
  width: 44px;
  text-align: center;
  font-size: 10px;
  letter-spacing: 1px;
  color: var(--muted);
  pointer-events: none;
}
.ring-core {
  position: absolute;
  left: 50%;
  top: 50%;
  transform: translate(-50%, -50%);
  width: 128px;
  height: 128px;
  border-radius: 50%;
  border: 1px dashed var(--line);
  background: radial-gradient(circle at 50% 40%, rgba(18, 18, 16, 0.96), rgba(10, 10, 9, 0.9));
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
}
.core-glyph {
  font-size: 44px;
  line-height: 1;
  text-shadow: 0 0 16px currentColor;
}
.ring-core.t-jin  { color: #e8c372; }
.ring-core.t-mu   { color: #4caf6e; }
.ring-core.t-shui { color: #3a8fd0; }
.ring-core.t-huo  { color: #e8412b; }
.ring-core.t-tu   { color: #c8923a; }
.ring-core.t-feng { color: #7fd8cf; }
.ring-core.t-lei { color: #9d7bf0; }
.ring-core.t-dian { color: #f2e34d; }
.ring-core.t-taiji { color: #d8d2c2; }
.ring-core.t-yinyang { color: #98a4b0; }
.core-trait {
  font-size: 11px;
  letter-spacing: 3px;
  color: var(--paper);
}
.core-hint {
  font-size: 10px;
  letter-spacing: 4px;
  color: var(--muted);
}

/* ---------- 山中四时 ---------- */
.scenes {
  margin-top: 30px;
  display: flex;
  gap: 14px;
  flex-wrap: wrap;
  justify-content: center;
}
.scene {
  position: relative;
  width: 210px;
  padding: 18px 16px 16px;
  border-radius: 8px;
  overflow: hidden;
  text-align: center;
  cursor: default;
  border: 1px solid rgba(236, 228, 210, 0.16);
  background: linear-gradient(180deg, rgba(18, 18, 16, 0.72), rgba(10, 10, 9, 0.8));
  box-shadow: 0 18px 50px rgba(0, 0, 0, 0.45);
  transition: 0.24s;
}
.scene:hover {
  transform: translateY(-6px);
  border-color: var(--tint);
  box-shadow: 0 18px 50px rgba(0, 0, 0, 0.5), 0 0 20px color-mix(in srgb, var(--tint) 25%, transparent);
}
.scene-art {
  width: 100%;
  height: 90px;
  display: block;
  border-radius: 6px;
  background: linear-gradient(180deg, rgba(236, 228, 210, 0.03), rgba(0, 0, 0, 0.25));
}
.scene-season {
  position: absolute;
  top: 26px;
  right: 22px;
  font-size: 34px;
  color: var(--tint);
  text-shadow: 0 0 14px rgba(0, 0, 0, 0.6);
  transform: rotate(4deg);
}
.scene-title {
  margin-top: 14px;
  font-size: 19px;
  letter-spacing: 4px;
  color: var(--paper);
}
.scene-title small {
  font-size: 10px;
  opacity: 0.55;
  letter-spacing: 2px;
  margin-left: 6px;
}
.scene-desc {
  margin-top: 8px;
  font-size: 12px;
  letter-spacing: 1px;
  color: var(--muted);
  min-height: 34px;
}
.scene-btn {
  margin-top: 10px;
  padding: 7px 24px;
  font-family: inherit;
  font-size: 14px;
  letter-spacing: 6px;
  color: var(--paper);
  background: transparent;
  border: 1px solid var(--line);
  border-radius: 6px;
  cursor: pointer;
  transition: 0.2s;
}
.scene-btn:hover {
  border-color: var(--tint);
  color: var(--tint);
  box-shadow: 0 0 12px color-mix(in srgb, var(--tint) 30%, transparent);
}

/* ---------- 山友论道 ---------- */
.discourse {
  margin-top: 30px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  max-width: 680px;
  width: 100%;
  margin-left: auto;
  margin-right: auto;
}
.ds-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 18px;
  border-radius: 8px;
  border: 1px solid rgba(236, 228, 210, 0.12);
  background: rgba(18, 18, 16, 0.6);
  transition: 0.2s;
}
.ds-item:hover {
  border-color: var(--line);
  transform: translateX(4px);
}
.ds-rank {
  flex-shrink: 0;
  width: 30px;
  font-size: 20px;
  text-align: center;
  color: var(--muted);
  font-weight: 700;
}
.ds-rank.top {
  color: var(--accent);
  text-shadow: 0 0 10px rgba(232, 65, 43, 0.4);
}
.ds-avatar {
  flex-shrink: 0;
  width: 44px;
  height: 44px;
  border-radius: 6px;
  display: grid;
  place-items: center;
  font-size: 20px;
  border: 1px solid currentColor;
  transform: rotate(-3deg);
  background: rgba(236, 228, 210, 0.04);
}
.ds-avatar.t-jin  { color: #e8c372; }
.ds-avatar.t-mu   { color: #4caf6e; }
.ds-avatar.t-shui { color: #3a8fd0; }
.ds-avatar.t-huo  { color: #e8412b; }
.ds-avatar.t-tu   { color: #c8923a; }
.ds-avatar.t-feng { color: #7fd8cf; }
.ds-avatar.t-lei { color: #9d7bf0; }
.ds-avatar.t-dian { color: #f2e34d; }
.ds-avatar.t-taiji { color: #d8d2c2; }
.ds-avatar.t-yinyang { color: #98a4b0; }
.ds-main {
  flex: 1;
  min-width: 0;
}
.ds-name {
  font-size: 15px;
  letter-spacing: 2px;
  color: var(--paper);
  display: flex;
  align-items: center;
  gap: 8px;
}
.ds-tag {
  font-size: 11px;
  padding: 1px 8px;
  border-radius: 12px;
  border: 1px solid currentColor;
}
.ds-tag.t-jin  { color: #e8c372; }
.ds-tag.t-mu   { color: #4caf6e; }
.ds-tag.t-shui { color: #3a8fd0; }
.ds-tag.t-huo  { color: #e8412b; }
.ds-tag.t-tu   { color: #c8923a; }
.ds-tag.t-feng { color: #7fd8cf; }
.ds-tag.t-lei { color: #9d7bf0; }
.ds-tag.t-dian { color: #f2e34d; }
.ds-tag.t-taiji { color: #d8d2c2; }
.ds-tag.t-yinyang { color: #98a4b0; }
.ds-quote {
  margin-top: 6px;
  font-size: 13px;
  letter-spacing: 1px;
  color: var(--muted);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.ds-zan {
  flex-shrink: 0;
  padding: 7px 14px;
  font-family: inherit;
  font-size: 13px;
  letter-spacing: 2px;
  color: var(--muted);
  background: transparent;
  border: 1px solid var(--line);
  border-radius: 6px;
  cursor: pointer;
  transition: 0.2s;
}
.ds-zan:hover {
  border-color: var(--accent);
  color: var(--accent);
}
.ds-zan.liked {
  border-color: var(--accent);
  color: var(--accent);
  background: rgba(232, 65, 43, 0.08);
  box-shadow: 0 0 10px rgba(232, 65, 43, 0.25);
}

/* ---------- 页脚 ---------- */
.footer {
  margin-top: 90px;
  padding-bottom: 30px;
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 14px;
}
.footer-seal {
  width: 46px;
  height: 46px;
  border-radius: 6px;
  background: var(--accent);
  color: #fff8ee;
  display: grid;
  place-items: center;
  font-size: 17px;
  line-height: 1.1;
  text-align: center;
  border: 2px solid #9c2418;
  transform: rotate(-4deg);
  box-shadow: inset 0 0 0 2px rgba(255, 248, 238, 0.22), 0 3px 8px rgba(0, 0, 0, 0.4);
  opacity: 0.85;
}
.footer-line {
  width: 240px;
  height: 1px;
  background: linear-gradient(90deg, transparent, var(--line), transparent);
}
.footer-text {
  font-size: 14px;
  letter-spacing: 6px;
  color: var(--muted);
}
.footer-sub {
  font-size: 12px;
  letter-spacing: 3px;
  color: rgba(236, 228, 210, 0.35);
}

/* ---------- 回顶按钮 ---------- */
.to-top {
  position: fixed;
  right: 26px;
  bottom: 30px;
  z-index: 12;
  width: 46px;
  height: 46px;
  border-radius: 50%;
  font-family: inherit;
  font-size: 17px;
  letter-spacing: 0;
  color: var(--paper);
  background: rgba(18, 18, 16, 0.85);
  border: 1px solid var(--line);
  cursor: pointer;
  transition: 0.2s;
  backdrop-filter: blur(4px);
}
.to-top:hover {
  border-color: var(--accent);
  color: var(--accent);
  box-shadow: 0 0 14px rgba(232, 65, 43, 0.35);
  transform: translateY(-3px);
}

/* ---------- 滚动渐显 ---------- */
.reveal {
  opacity: 0;
  transform: translateY(26px);
  transition: opacity 0.7s ease, transform 0.7s ease;
}
.reveal.in {
  opacity: 1;
  transform: none;
}
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.25s ease, transform 0.25s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
  transform: translateY(8px);
}

/* ---------- 窄屏适配 ---------- */
@media (max-width: 860px) {
  .navbar {
    flex-wrap: wrap;
    gap: 12px;
    padding: 12px 16px;
  }
  .nav {
    order: 3;
    width: 100%;
    justify-content: center;
  }
  .nav-right {
    margin-left: auto;
  }
  .wx-mini {
    display: none;
  }
  .hero-title {
    font-size: 42px;
    letter-spacing: 8px;
  }
  .r-desc,
  .ds-quote {
    white-space: normal;
  }
  .announce {
    top: 63px;
  }
  .body {
    padding-top: 150px;
  }
  .ring-wrap {
    transform: scale(0.88);
  }
}

/* ============================== 论道堂（导航「论道」） ============================== */
.dq-publish {
  display: flex;
  gap: 12px;
  align-items: stretch;
  max-width: 760px;
  margin: 26px auto 10px;
}
.dq-publish textarea {
  flex: 1;
  resize: none;
  padding: 12px 16px;
  border-radius: 10px;
  border: 1px solid var(--line);
  background: rgba(18, 18, 16, 0.6);
  color: var(--paper);
  font-family: var(--font-kai, inherit);
  font-size: 14px;
  letter-spacing: 1px;
  line-height: 1.7;
  outline: none;
  transition: 0.2s;
}
.dq-publish textarea:focus {
  border-color: var(--accent);
  box-shadow: 0 0 12px rgba(232, 65, 43, 0.18);
}
.dq-btn {
  padding: 0 26px;
  border-radius: 10px;
  border: 1px solid var(--accent);
  background: transparent;
  color: var(--accent);
  font-size: 15px;
  letter-spacing: 4px;
  cursor: pointer;
  transition: 0.2s;
  white-space: nowrap;
}
.dq-btn:hover:not(:disabled) {
  background: var(--accent);
  color: #14100c;
}
.dq-btn:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}
.dq-msg {
  max-width: 760px;
  margin: 0 auto 14px;
  text-align: center;
  font-size: 13px;
  letter-spacing: 2px;
  color: var(--accent);
  opacity: 0.9;
}
.dq-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  max-width: 900px;
  margin: 0 auto;
}

/* ============================== 市集（导航「市集」） ============================== */
.mk-filters {
  margin-top: 26px;
}
.mk-search {
  padding: 6px 16px;
  width: 260px;
  border-radius: 20px;
  border: 1px solid var(--line);
  background: rgba(18, 18, 16, 0.6);
  color: var(--paper);
  font-size: 13px;
  letter-spacing: 1px;
  outline: none;
  transition: 0.2s;
}
.mk-search:focus {
  border-color: var(--accent);
}
.mk-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
  gap: 18px;
  margin-top: 26px;
}
.mk-card {
  position: relative;
  padding: 18px 20px 14px;
  border-radius: 12px;
  border: 1px solid rgba(236, 228, 210, 0.12);
  background: rgba(18, 18, 16, 0.62);
  transition: 0.25s;
  overflow: hidden;
}
.mk-card:hover {
  transform: translateY(-4px);
  border-color: rgba(236, 228, 210, 0.3);
  box-shadow: 0 10px 28px rgba(0, 0, 0, 0.45);
}
.mk-card.gone {
  opacity: 0.55;
  filter: grayscale(0.5);
}
.mk-card::before {
  content: '易';
  position: absolute;
  right: -8px;
  top: -18px;
  font-size: 84px;
  color: rgba(236, 228, 210, 0.05);
  font-family: var(--font-kai, inherit);
}
.mk-head {
  display: flex;
  gap: 8px;
  align-items: center;
}
.mk-title {
  margin-top: 10px;
  font-size: 19px;
  letter-spacing: 2px;
  color: var(--paper);
}
.mk-desc {
  margin-top: 8px;
  font-size: 12.5px;
  line-height: 1.7;
  color: var(--muted);
  letter-spacing: 0.5px;
  min-height: 42px;
}
.mk-foot {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 12px;
  padding-top: 10px;
  border-top: 1px dashed rgba(236, 228, 210, 0.14);
}
.mk-price {
  font-size: 20px;
  color: #e8c372;
  letter-spacing: 1px;
}
.mk-price small {
  font-size: 12px;
  margin-left: 4px;
  color: var(--muted);
}
.mk-stock {
  font-size: 12px;
  color: var(--muted);
  letter-spacing: 1px;
  flex: 1;
}
.mk-buy {
  padding: 6px 18px;
  border-radius: 18px;
  border: 1px solid var(--accent);
  background: transparent;
  color: var(--accent);
  font-size: 13px;
  letter-spacing: 3px;
  cursor: pointer;
  transition: 0.2s;
  white-space: nowrap;
}
.mk-buy:hover:not(:disabled) {
  background: var(--accent);
  color: #14100c;
}
.mk-buy:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}
.mk-seller {
  margin-top: 8px;
  font-size: 11.5px;
  color: var(--muted);
  letter-spacing: 1px;
}
@media (max-width: 760px) {
  .mk-grid {
    grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
  }
  .dq-publish {
    flex-direction: column;
  }
  .dq-btn {
    padding: 10px 26px;
  }
}
</style>
