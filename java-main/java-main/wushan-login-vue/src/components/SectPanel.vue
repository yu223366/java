<script setup>
/**
 * 宗门：山友自发结阵。一人只属一宗，宗主不可自离，捐献灵石换贡献。
 */
import { ref, computed, onMounted } from 'vue'
import { sectApi } from '../api/sect'
import { ELEMENTS, findElement } from '../constants/wuxing'
import navBg from '../assets/nav-bg-1.jpg'

const emit = defineEmits(['to-home'])

const mine = ref(null)
const sects = ref([])
const newName = ref('')
const newElement = ref('huo')
const msg = ref('')
const busy = ref(false)
let timer = null

const hasSect = computed(() => mine.value?.hasSect)
const mySect = computed(() => mine.value?.sect || null)

function flash(m) {
  msg.value = m
  setTimeout(() => { msg.value = '' }, 2400)
}

async function load() {
  try {
    const [m, l] = await Promise.all([sectApi.mine(), sectApi.list()])
    mine.value = m
    sects.value = l
  } catch (e) {
    flash(e.message || '宗门信息读取失败')
  }
}

async function create() {
  if (!newName.value.trim()) return flash('请先取一个宗门名')
  busy.value = true
  try {
    const r = await sectApi.create(newName.value.trim(), newElement.value)
    flash(`「${r.name}」已立`)
    newName.value = ''
    await load()
  } catch (e) {
    flash(e.message || '立宗失败')
  } finally {
    busy.value = false
  }
}

async function join(s) {
  busy.value = true
  try {
    await sectApi.join(s.id)
    flash(`已入「${s.name}」`)
    await load()
  } catch (e) {
    flash(e.message || '投山门失败')
  } finally {
    busy.value = false
  }
}

async function quit() {
  busy.value = true
  try {
    await sectApi.quit()
    flash('已离山')
    await load()
  } catch (e) {
    flash(e.message || '离山失败')
  } finally {
    busy.value = false
  }
}

async function donate(amount) {
  busy.value = true
  try {
    const r = await sectApi.donate(amount)
    flash(`捐献 ${amount} 灵石，贡献 ${r.contribution}`)
    await load()
  } catch (e) {
    flash(e.message || '捐献失败')
  } finally {
    busy.value = false
  }
}

/** 宗主传位给某位成员 */
async function transfer(m) {
  busy.value = true
  try {
    const r = await sectApi.transfer(m.userId)
    flash(`宗主之位已传于 ${r.newLeader}`)
    await load()
  } catch (e) {
    flash(e.message || '传位失败')
  } finally {
    busy.value = false
  }
}

/** 解散宗门 */
async function disband() {
  busy.value = true
  try {
    const r = await sectApi.disband()
    flash(`「${r.name}」已散伙`)
    await load()
  } catch (e) {
    flash(e.message || '解散失败')
  } finally {
    busy.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="sect">
    <div class="bg-photo" :style="{ backgroundImage: `url(${navBg})` }"></div>

    <header class="sc-head">
      <button class="back-btn" @click="emit('to-home')">‹ 回首页</button>
      <div class="head-brand">
        <div class="seal">宗<br />门</div>
        <div class="brand-text">
          <span class="name">宗门</span>
          <small>同 气 连 枝 · 共 守 一 方</small>
        </div>
      </div>
      <div class="head-stat">灵 石 · {{ mine?.balance || 0 }}</div>
    </header>

    <div class="sc-body">
      <!-- 已入宗 -->
      <template v-if="hasSect">
        <div class="sect-hero">
          <div class="hero-name">
            <span class="hero-el" :style="{ color: findElement(mySect?.element).color }">
              {{ findElement(mySect?.element).label }}
            </span>
            {{ mySect?.name }}
          </div>
          <div class="hero-meta">
            <span>宗 主 · {{ mySect?.leaderName }}</span>
            <span>我 为 {{ mine?.myRoleName }}</span>
            <span>我 的 贡 献 · {{ mine?.myContribution || 0 }}</span>
            <span>全 宗 贡 献 · {{ mine?.totalContribution || 0 }}</span>
          </div>
          <p class="hero-notice">宗 门 告 示 · {{ mySect?.notice }}</p>
          <div class="hero-ops">
            <button class="main-btn" :disabled="busy" @click="donate(50)">献 50 灵 石</button>
            <button class="main-btn" :disabled="busy" @click="donate(200)">献 200 灵 石</button>
            <button class="mini-btn" :disabled="busy || mine?.myRole === 2" @click="quit">离 山</button>
            <button v-if="mine?.myRole === 2" class="mini-btn danger" :disabled="busy" @click="disband">解 散 宗 门</button>
          </div>
          <div class="sect-goal">
            <div class="goal-title">宗 门 目 标 <small>日 贡 献 满 500 者，可领宗门令牌</small></div>
            <div class="goal-bar">
              <i :style="{ width: Math.min(100, ((mine?.myContribution || 0) / 500) * 100) + '%' }"></i>
            </div>
            <div class="goal-text">本 日 贡 献 {{ mine?.myContribution || 0 }} / 500</div>
          </div>
        </div>

        <h3 class="sec-title">宗 门 列 座</h3>
        <div class="member-list">
          <div v-for="(m, i) in mine?.members || []" :key="i" class="member">
            <span class="m-rank">{{ i + 1 }}</span>
            <span class="m-name">{{ m.username }}</span>
            <span class="m-role" :class="m.role">{{ m.roleName }}</span>
            <span class="m-con">贡献 {{ m.contribution }}</span>
            <button
              v-if="mine?.myRole === 2 && m.userId"
              class="m-transfer"
              :disabled="busy"
              @click="transfer(m)"
            >传 位</button>
          </div>
          <div v-if="!(mine?.members || []).length" class="log-empty">尚 无 同 门</div>
        </div>
      </template>

      <!-- 未入宗 -->
      <template v-else>
        <div class="sect-hero">
          <div class="hero-name">开 山 立 宗</div>
          <p class="hero-notice">你尚未拜入任何宗门，可自立门户，或投靠已有山门。</p>
          <div class="create-row">
            <input v-model="newName" class="txt" placeholder="宗门名（12 字内）" maxlength="12" />
            <div class="el-picker">
              <button
                v-for="e in ELEMENTS.slice(0, 5)"
                :key="e.key"
                class="el-btn"
                :class="{ 'is-on': newElement === e.key }"
                :style="{ color: e.color, borderColor: newElement === e.key ? e.color : 'var(--line)' }"
                @click="newElement = e.key"
              >{{ e.label }}</button>
            </div>
            <button class="main-btn" :disabled="busy" @click="create">立 宗</button>
          </div>
        </div>

        <h3 class="sec-title">山 门 列 表</h3>
        <div class="sect-list">
          <div v-for="s in sects" :key="s.id" class="sect-item">
            <span class="s-el" :style="{ color: findElement(s.element).color }">{{ findElement(s.element).label }}</span>
            <span class="s-name">{{ s.name }}</span>
            <span class="s-leader">宗主 {{ s.leaderName }}</span>
            <span class="s-count">{{ s.memberCount }} 人</span>
            <button class="mini-btn" :disabled="busy" @click="join(s)">投 山 门</button>
          </div>
          <div v-if="!sects.length" class="log-empty">尚 无 宗 门 · 你可自 立 一 处</div>
        </div>
      </template>
    </div>

    <Transition name="fade">
      <div v-if="msg" class="notice">{{ msg }}</div>
    </Transition>
  </div>
</template>

<style scoped>
.sect { position: relative; height: 100vh; overflow: hidden; color: var(--paper); font-family: "Kaiti SC", "STKaiti", "KaiTi", "楷体", serif; }
.bg-photo { position: fixed; inset: 0; z-index: 0; background-size: cover; background-position: center; }
.bg-photo::after {
  content: ''; position: absolute; inset: 0;
  background: radial-gradient(120% 90% at 50% 40%, rgba(10, 10, 9, 0) 35%, rgba(10, 10, 9, 0.62) 100%),
              linear-gradient(180deg, rgba(10, 10, 9, 0.72) 0%, rgba(10, 10, 9, 0.55) 50%, rgba(10, 10, 9, 0.84) 100%);
}
.sc-head {
  position: relative; z-index: 2; height: 92px; display: flex; align-items: center; gap: 22px; padding: 0 30px;
  border-bottom: 1px solid var(--line); background: rgba(12, 16, 15, 0.42);
}
.back-btn { flex: none; padding: 8px 16px; color: var(--paper); font: inherit; font-size: 15px; letter-spacing: 2px; background: rgba(236, 228, 210, 0.06); border: 1px solid var(--line); border-radius: 2px; cursor: pointer; }
.back-btn:hover { background: rgba(236, 228, 210, 0.14); }
.head-brand { display: flex; align-items: center; gap: 14px; }
.seal { width: 44px; height: 44px; display: grid; place-content: center; font-size: 15px; line-height: 1.05; text-align: center; color: #f6ece0; background: #a3311f; border: 1px solid #c8543c; }
.brand-text { display: flex; flex-direction: column; gap: 3px; }
.brand-text .name { font-size: 26px; letter-spacing: 8px; }
.brand-text small { font-size: 11px; letter-spacing: 3px; color: var(--muted); }
.head-stat { margin-left: auto; font-size: 15px; letter-spacing: 3px; color: #e8c372; }

.sc-body { position: relative; z-index: 2; height: calc(100vh - 92px); overflow-y: auto; padding: 26px 30px 36px; }
.sect-hero { padding: 22px 24px; margin-bottom: 26px; border: 1px solid var(--line); background: rgba(12, 17, 16, 0.58); }
.hero-name { font-size: 30px; letter-spacing: 8px; margin-bottom: 12px; }
.hero-el { font-size: 30px; margin-right: 10px; }
.hero-meta { display: flex; flex-wrap: wrap; gap: 20px; font-size: 13px; letter-spacing: 2px; color: var(--muted); margin-bottom: 14px; }
.hero-notice { font-size: 14px; letter-spacing: 2px; color: rgba(236, 228, 210, 0.8); margin-bottom: 18px; }
.hero-ops { display: flex; gap: 12px; flex-wrap: wrap; align-items: center; }
.sect-goal { margin-top: 20px; padding-top: 16px; border-top: 1px solid var(--line); }
.goal-title { font-size: 13px; letter-spacing: 4px; color: #e8c372; margin-bottom: 10px; }
.goal-title small { font-size: 11px; color: var(--muted); letter-spacing: 1px; margin-left: 10px; }
.goal-bar { height: 10px; background: rgba(236, 228, 210, 0.08); }
.goal-bar i { display: block; height: 100%; background: linear-gradient(90deg, #8a2f1e, #e8412b); transition: width 0.4s; }
.goal-text { margin-top: 8px; font-size: 12px; letter-spacing: 2px; color: var(--muted); }
.create-row { display: flex; gap: 12px; flex-wrap: wrap; align-items: center; }
.txt { font: inherit; font-size: 15px; color: var(--paper); background: rgba(236, 228, 210, 0.06); border: 1px solid var(--line); padding: 10px 14px; width: 220px; }
.el-picker { display: flex; gap: 6px; }
.el-btn { width: 40px; height: 40px; font: inherit; font-size: 16px; background: rgba(236, 228, 210, 0.05); border: 1px solid var(--line); cursor: pointer; }
.el-btn.is-on { background: rgba(232, 65, 43, 0.18); }

.sec-title { font-size: 15px; font-weight: normal; letter-spacing: 5px; color: #e8c372; margin-bottom: 16px; }
.sect-list, .member-list { display: flex; flex-direction: column; gap: 10px; }
.sect-item, .member { display: flex; align-items: center; gap: 16px; padding: 13px 18px; border: 1px solid var(--line); background: rgba(12, 17, 16, 0.5); font-size: 14px; letter-spacing: 2px; }
.s-el { font-size: 20px; }
.s-name { font-size: 18px; }
.s-leader { color: var(--muted); font-size: 13px; }
.s-count { margin-left: auto; color: var(--muted); font-size: 13px; }
.m-rank { width: 26px; color: var(--muted); }
.m-name { flex: 1; }
.m-role { font-size: 12px; padding: 2px 8px; border: 1px solid var(--line); color: var(--muted); }
.m-role.role2 { border-color: #e8c372; color: #e8c372; }
.m-role.role1 { border-color: #639922; color: #c0dd97; }
.m-con { color: var(--muted); font-size: 13px; }
.m-transfer {
  font: inherit; font-size: 11px; letter-spacing: 1px; padding: 3px 10px;
  color: #e8c372; background: none; border: 1px solid rgba(232, 195, 114, 0.4); cursor: pointer;
}
.m-transfer:hover:not(:disabled) { background: rgba(232, 195, 114, 0.16); }
.m-transfer:disabled { opacity: 0.4; cursor: not-allowed; }
.mini-btn.danger { color: #f0a89a; border-color: rgba(232, 75, 74, 0.5); }
.mini-btn.danger:hover:not(:disabled) { background: rgba(232, 75, 74, 0.18); }
.log-empty { font-size: 12px; letter-spacing: 2px; color: var(--muted); text-align: center; padding: 20px 0; }

.main-btn { font: inherit; font-size: 15px; letter-spacing: 3px; cursor: pointer; padding: 10px 24px; color: #f6ece0; background: linear-gradient(180deg, rgba(163, 49, 31, 0.92), rgba(120, 33, 21, 0.92)); border: 1px solid #c8543c; border-radius: 2px; }
.mini-btn { font: inherit; font-size: 14px; letter-spacing: 3px; cursor: pointer; padding: 9px 18px; color: var(--paper); background: rgba(236, 228, 210, 0.08); border: 1px solid var(--line); border-radius: 2px; }
.main-btn:disabled, .mini-btn:disabled { opacity: 0.4; cursor: not-allowed; }
.notice { position: fixed; left: 50%; bottom: 60px; transform: translateX(-50%); z-index: 9; padding: 10px 26px; font-size: 14px; letter-spacing: 2px; color: var(--paper); background: rgba(163, 49, 31, 0.92); border: 1px solid #c8543c; }
.fade-enter-active, .fade-leave-active { transition: opacity 0.25s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
</style>
