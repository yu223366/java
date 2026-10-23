<script setup>
// 宝库（原藏经阁）：十种元素 × 分类（功法/心得/修炼资源/灵根/秘境）× 品阶（凡黄玄地天圣）
// 左侧元素栏 + 顶部分类页签 + 品阶筛选，风格延续登录页水墨五行。
// 数据接口见 ../api/codex.js —— 前端 mock 可跑，后端接上即用。
import { ref, computed, watch, onMounted } from 'vue'
import { codexApi } from '../api/codex'
import { ELEMENTS, findElement } from '../constants/wuxing'
import { GRADES, findGrade, CATEGORIES } from '../constants/codex'

const emit = defineEmits(['to-home'])

/* ------------------------------ 筛选状态 ------------------------------ */
const activeElement = ref('all') // all | 元素 key
const activeTab = ref('gongfa') // gongfa | xinde | ziyuan | linggen | mijing
const activeGrade = ref('all') // all | 品阶 key
const loading = ref(false)
const items = ref([])
const total = ref(0)

const tabs = [
  ...CATEGORIES,
  { key: 'linggen', label: '灵根', en: 'spirit root' },
  { key: 'mijing', label: '秘境', en: 'secret realm' }
]

const activeElementInfo = computed(() =>
  activeElement.value === 'all' ? null : findElement(activeElement.value)
)

/* ------------------------------ 拉取数据 ------------------------------ */
async function load() {
  loading.value = true
  try {
    const filters = {
      element: activeElement.value,
      grade: activeGrade.value
    }
    // 预留接口：后端模式下走真实检索（mock 下本地过滤常量）
    if (activeTab.value === 'linggen') {
      const r = await codexApi.getSpiritRoots(filters)
      items.value = r.items
      total.value = r.total
    } else if (activeTab.value === 'mijing') {
      const r = await codexApi.getRealms(filters)
      items.value = r.items
      total.value = r.total
    } else {
      const r = await codexApi.getItems({ ...filters, category: activeTab.value })
      items.value = r.items
      total.value = r.total
    }
  } catch (e) {
    items.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

watch([activeElement, activeTab, activeGrade], load)
onMounted(load)

function resetGrade() {
  activeGrade.value = 'all'
}
</script>

<template>
  <div class="codex">
    <!-- ============ 页头 ============ -->
    <header class="codex-head">
      <button class="back-btn" @click="emit('to-home')">‹ 回首页</button>
      <div class="head-brand">
        <div class="seal">宝<br />库</div>
        <div class="brand-text">
          <span class="name">宝库</span>
          <small>功 法 · 心 得 · 资 源 · 灵 根 · 秘 境</small>
        </div>
      </div>
      <div class="head-stat">{{ loading ? '翻 检 中…' : `共 藏 · ${total} 条` }}</div>
    </header>

    <!-- ============ 主体 ============ -->
    <div class="codex-body">
      <!-- 左侧元素栏 -->
      <aside class="rail">
        <div class="rail-title">元 素</div>
        <div
          class="rail-item all-item"
          :class="{ 'is-on': activeElement === 'all' }"
          @click="activeElement = 'all'; resetGrade()"
        >全<small>all</small></div>
        <div
          v-for="el in ELEMENTS"
          :key="el.key"
          class="rail-item"
          :class="[el.key, { 'is-on': activeElement === el.key }]"
          :title="el.trait"
          @click="activeElement = el.key"
        >{{ el.label }}<small>{{ el.en }}</small></div>
      </aside>

      <!-- 右侧内容区 -->
      <section class="main">
        <!-- 分类页签 -->
        <div class="tabs">
          <div
            v-for="t in tabs"
            :key="t.key"
            class="tab"
            :class="{ 'is-on': activeTab === t.key }"
            @click="activeTab = t.key"
          >
            {{ t.label }}<small>{{ t.en }}</small>
          </div>
        </div>

        <!-- 品阶筛选 -->
        <div class="grade-bar">
          <span class="grade-label">品 阶：</span>
          <span
            class="grade-chip all-chip"
            :class="{ 'is-on': activeGrade === 'all' }"
            @click="activeGrade = 'all'"
          >全部</span>
          <span
            v-for="g in GRADES"
            :key="g.key"
            class="grade-chip"
            :class="{ 'is-on': activeGrade === g.key }"
            :style="{ color: g.color }"
            @click="activeGrade = g.key"
          >{{ g.label }}</span>
        </div>

        <!-- 当前筛选提示 -->
        <div class="filter-tip" v-if="activeElementInfo">
          本 命 · {{ activeElementInfo.label }} · {{ activeElementInfo.trait }}
        </div>

        <!-- 条目列表 -->
        <div class="grid" v-if="items.length">
          <!-- 灵根卡片 -->
          <template v-if="activeTab === 'linggen'">
            <div v-for="it in items" :key="it.id" class="item root-item">
              <span class="grade-badge" :style="{ color: findGrade(it.grade).color, borderColor: findGrade(it.grade).color }">{{ findGrade(it.grade).label }}</span>
              <div class="item-title">
                <span class="el-chip" :class="'t-' + it.element">{{ findElement(it.element).label }}</span>
                {{ it.name }}
              </div>
              <p class="item-desc">{{ it.desc }}</p>
              <div class="item-foot">
                <span class="el-name" :class="'t-' + it.element">{{ findElement(it.element).label }}行灵根</span>
              </div>
            </div>
          </template>

          <!-- 秘境卡片 -->
          <template v-else-if="activeTab === 'mijing'">
            <div v-for="it in items" :key="it.id" class="item realm-item">
              <span class="grade-badge" :style="{ color: findGrade(it.grade).color, borderColor: findGrade(it.grade).color }">{{ findGrade(it.grade).label }}</span>
              <div class="item-title">
                <span class="el-chip" :class="'t-' + it.element">{{ findElement(it.element).label }}</span>
                {{ it.name }}
              </div>
              <p class="item-desc">{{ it.desc }}</p>
              <div class="item-foot">
                <span class="layers">层数 · {{ it.layers }}</span>
              </div>
            </div>
          </template>

          <!-- 功法 / 心得 / 资源卡片 -->
          <template v-else>
            <div v-for="it in items" :key="it.id" class="item">
              <span class="grade-badge" :style="{ color: findGrade(it.grade).color, borderColor: findGrade(it.grade).color }">{{ findGrade(it.grade).label }}</span>
              <div class="item-title">
                <span class="el-chip" :class="'t-' + it.element">{{ findElement(it.element).label }}</span>
                {{ it.title }}
              </div>
              <p class="item-desc">{{ it.desc }}</p>
              <div class="item-foot">
                <span class="el-name" :class="'t-' + it.element">{{ findElement(it.element).label }}行</span>
              </div>
            </div>
          </template>
        </div>

        <!-- 空态 / 加载中 -->
        <div class="empty" v-else-if="!loading">库中无此藏，换个元素或品阶再翻。</div>
      </section>
    </div>
  </div>
</template>

<style scoped>
/* ============================== 页头 ============================== */
.codex {
  position: relative;
  z-index: 2;
  height: 100vh;
  overflow-y: auto;
  scroll-behavior: smooth;
}
.codex-head {
  position: sticky;
  top: 0;
  z-index: 5;
  display: flex;
  align-items: center;
  gap: 24px;
  padding: 14px 28px;
  background: linear-gradient(180deg, rgba(14, 13, 12, 0.92), rgba(14, 13, 12, 0.6));
  border-bottom: 1px solid var(--line);
  backdrop-filter: blur(6px);
}
.back-btn {
  padding: 8px 16px;
  font-family: inherit;
  font-size: 14px;
  letter-spacing: 2px;
  color: var(--paper);
  background: rgba(236, 228, 210, 0.05);
  border: 1px solid var(--line);
  border-radius: 6px;
  cursor: pointer;
  transition: 0.2s;
}
.back-btn:hover {
  border-color: var(--accent);
  color: var(--accent);
}
.head-brand {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
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
  letter-spacing: 3px;
  color: var(--muted);
  margin-top: 4px;
}
.head-stat {
  font-size: 13px;
  letter-spacing: 3px;
  color: var(--muted);
}

/* ============================== 主体 ============================== */
.codex-body {
  display: flex;
  gap: 0;
  max-width: 1080px;
  margin: 0 auto;
  padding: 28px 24px 48px;
  align-items: flex-start;
}

/* ---------- 左侧元素栏 ---------- */
.rail {
  position: sticky;
  top: 92px;
  width: 92px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.rail-title {
  font-size: 12px;
  letter-spacing: 4px;
  color: var(--muted);
  margin-bottom: 6px;
  padding-left: 4px;
}
.rail-item {
  padding: 8px 0;
  text-align: center;
  border-radius: 8px;
  cursor: pointer;
  font-size: 16px;
  letter-spacing: 2px;
  border: 1px solid rgba(236, 228, 210, 0.14);
  background: rgba(18, 18, 16, 0.6);
  color: var(--paper);
  transition: 0.2s;
}
.rail-item small {
  display: block;
  font-size: 9px;
  opacity: 0.55;
  letter-spacing: 1px;
  margin-top: 2px;
}
.rail-item:hover {
  transform: translateX(3px);
}
.rail-item.is-on {
  box-shadow: 0 0 0 2px currentColor, 0 6px 16px rgba(0, 0, 0, 0.4);
}
.rail-item.jin { color: #e8c372; }
.rail-item.mu { color: #4caf6e; }
.rail-item.shui { color: #3a8fd0; }
.rail-item.huo { color: #e8412b; }
.rail-item.tu { color: #c8923a; }
.rail-item.feng { color: #7fd8cf; }
.rail-item.lei { color: #9d7bf0; }
.rail-item.dian { color: #f2e34d; }
.rail-item.taiji { color: #d8d2c2; }
.rail-item.yinyang { color: #98a4b0; }
.all-item {
  color: var(--paper);
}

/* ---------- 右侧内容 ---------- */
.main {
  flex: 1;
  min-width: 0;
  padding-left: 28px;
}

.tabs {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
.tab {
  padding: 9px 24px;
  border-radius: 8px;
  cursor: pointer;
  font-size: 16px;
  letter-spacing: 4px;
  color: var(--paper);
  border: 1px solid rgba(236, 228, 210, 0.14);
  background: rgba(18, 18, 16, 0.6);
  transition: 0.2s;
  text-align: center;
}
.tab small {
  display: block;
  font-size: 9px;
  opacity: 0.5;
  letter-spacing: 1px;
  margin-top: 2px;
}
.tab:hover {
  transform: translateY(-2px);
}
.tab.is-on {
  border-color: var(--accent);
  background: rgba(236, 228, 210, 0.05);
  box-shadow: 0 0 14px rgba(232, 65, 43, 0.25);
}

.grade-bar {
  margin-top: 18px;
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.grade-label {
  font-size: 12px;
  letter-spacing: 3px;
  color: var(--muted);
}
.grade-chip {
  font-size: 13px;
  letter-spacing: 2px;
  padding: 4px 14px;
  border-radius: 20px;
  border: 1px solid var(--line);
  cursor: pointer;
  transition: 0.2s;
  background: rgba(236, 228, 210, 0.04);
  color: var(--paper);
}
.grade-chip:hover {
  transform: translateY(-2px);
  border-color: currentColor;
}
.grade-chip.is-on {
  box-shadow: 0 0 0 2px currentColor, 0 4px 12px rgba(0, 0, 0, 0.35);
}
.all-chip {
  color: var(--paper);
}

.filter-tip {
  margin-top: 18px;
  font-size: 13px;
  letter-spacing: 4px;
  color: var(--muted);
  border-left: 3px solid var(--accent);
  padding-left: 12px;
}

/* ---------- 卡片网格 ---------- */
.grid {
  margin-top: 18px;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(272px, 1fr));
  gap: 14px;
}
.item {
  position: relative;
  padding: 18px 18px 14px;
  border-radius: 8px;
  border: 1px solid rgba(236, 228, 210, 0.14);
  background: linear-gradient(180deg, rgba(18, 18, 16, 0.72), rgba(10, 10, 9, 0.8));
  box-shadow: 0 16px 40px rgba(0, 0, 0, 0.4);
  transition: 0.22s;
}
.item:hover {
  transform: translateY(-4px);
  border-color: var(--line);
}
.grade-badge {
  position: absolute;
  top: 12px;
  right: 12px;
  font-size: 11px;
  letter-spacing: 2px;
  padding: 2px 10px;
  border-radius: 4px;
  border: 1px solid currentColor;
  background: rgba(10, 10, 9, 0.7);
}
.item-title {
  font-size: 18px;
  letter-spacing: 2px;
  color: var(--paper);
  display: flex;
  align-items: center;
  gap: 8px;
  padding-right: 64px;
  min-height: 30px;
}
.el-chip {
  flex-shrink: 0;
  width: 24px;
  height: 24px;
  display: grid;
  place-items: center;
  border-radius: 6px;
  font-size: 13px;
  border: 1px solid currentColor;
}
.el-chip.t-jin { color: #e8c372; }
.el-chip.t-mu { color: #4caf6e; }
.el-chip.t-shui { color: #3a8fd0; }
.el-chip.t-huo { color: #e8412b; }
.el-chip.t-tu { color: #c8923a; }
.el-chip.t-feng { color: #7fd8cf; }
.el-chip.t-lei { color: #9d7bf0; }
.el-chip.t-dian { color: #f2e34d; }
.el-chip.t-taiji { color: #d8d2c2; }
.el-chip.t-yinyang { color: #98a4b0; }
.item-desc {
  margin-top: 10px;
  font-size: 13px;
  letter-spacing: 1px;
  color: var(--muted);
  line-height: 1.7;
}
.item-foot {
  margin-top: 12px;
  padding-top: 10px;
  border-top: 1px dashed rgba(236, 228, 210, 0.12);
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  letter-spacing: 2px;
}
.el-name.t-jin { color: #e8c372; }
.el-name.t-mu { color: #4caf6e; }
.el-name.t-shui { color: #3a8fd0; }
.el-name.t-huo { color: #e8412b; }
.el-name.t-tu { color: #c8923a; }
.el-name.t-feng { color: #7fd8cf; }
.el-name.t-lei { color: #9d7bf0; }
.el-name.t-dian { color: #f2e34d; }
.el-name.t-taiji { color: #d8d2c2; }
.el-name.t-yinyang { color: #98a4b0; }
.layers {
  color: var(--muted);
}

.empty {
  margin-top: 40px;
  text-align: center;
  font-size: 14px;
  letter-spacing: 3px;
  color: var(--muted);
}

/* ---------- 窄屏适配 ---------- */
@media (max-width: 760px) {
  .codex-body {
    flex-direction: column;
    padding: 16px 14px 40px;
  }
  .rail {
    position: static;
    width: 100%;
    flex-direction: row;
    flex-wrap: wrap;
    justify-content: center;
  }
  .rail-title {
    width: 100%;
    text-align: center;
    margin-bottom: 2px;
  }
  .rail-item {
    width: 52px;
    font-size: 14px;
  }
  .main {
    padding-left: 0;
    padding-top: 16px;
  }
  .codex-head {
    flex-wrap: wrap;
    gap: 10px;
    padding: 12px 14px;
  }
}
</style>
