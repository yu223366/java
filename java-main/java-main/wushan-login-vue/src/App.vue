<script setup>
// 根组件：负责整体布局，并把「五行主题色」状态提升到此处，
// 同时分发给背景粒子（emberColor）与面板（accent）；
// 并在 登录 / 注册 / 首页 / 宝库 / 后台登录 / 后台管理 六个视图间切换。
import { ref, computed, onMounted } from 'vue'
import EmberBackground from './components/EmberBackground.vue'
import FogMountains from './components/FogMountains.vue'
import LoginPanel from './components/LoginPanel.vue'
import RegisterPanel from './components/RegisterPanel.vue'
import HomePanel from './components/HomePanel.vue'
import CodexPanel from './components/CodexPanel.vue'
import AuctionPanel from './components/AuctionPanel.vue'
import AdminLoginPanel from './components/AdminLoginPanel.vue'
import AdminPanel from './components/AdminPanel.vue'
import { adminApi } from './api/admin'
import { tokenStore, sessionStore, USE_MOCK } from './api/auth'
import { homeApi } from './api/home'
import bgYinyang from './assets/bg-yinyang.jpg'
import AuctionNotice from './components/AuctionNotice.vue'
import GamePanel from './components/GamePanel.vue'
import WorldPanel from './components/WorldPanel.vue'
import SectPanel from './components/SectPanel.vue'
import ObserverPanel from './components/ObserverPanel.vue'

const accent = ref('#e8412b')
const accent2 = ref('#ff7a18')
const emberColor = ref('#e8412b')
const bg = ref(null)
// login | register | home | codex | admin-login | admin
const view = ref('login')
const username = ref('') // 登录成功的名号，带入首页
const role = ref(1) // 当前用户权限：0=执事（可见后台入口） 1=山友
const adminName = ref('') // 后台登录成功的执事名号，带入后台

// 五行被点击：更新主题色与火星颜色（火元素使用默认红，其余使用所选色）
function onSelect({ color, huo }) {
  accent.value = color
  emberColor.value = huo ? '#e8412b' : color
}

// 成功瞬间（登录 / 立契）：在面板中心位置引爆一团火星
function onBurst(p) {
  if (bg.value) bg.value.burst(p.x, p.y, p.n, p.spread)
}

// 登录成功：记录名号与权限并跳转首页
function onEnterHome(name, r) {
  username.value = name
  role.value = Number(r) === 0 ? 0 : 1
  view.value = 'home'
}

// 首页「出山」：回到登录页（令牌与本地会话一并清掉，刷新后不会自动复登）
function onLogout() {
  username.value = ''
  role.value = 1
  sessionStore.clear()
  tokenStore.clear()
  view.value = 'login'
}

// 首页导航：宝库 → 宝库页；拍卖行 → 拍卖行页；其余项暂为占位（后续接分页）
// cangjing 为改名前的旧 key，库里若还是旧种子数据也能正常跳转
function onNav(item) {
  if (!item) return
  if (item.key === 'baoku' || item.key === 'cangjing') view.value = 'codex'
  if (item.key === 'paimai') view.value = 'auction'
  if (item.key === 'xiuxing') view.value = 'game'
  if (item.key === 'mijing') view.value = 'explore'
  if (item.key === 'liandan') view.value = 'alchemy'
  if (item.key === 'lianqi') view.value = 'lianqi'
  if (item.key === 'zhenfa') view.value = 'zhenfa'
  if (item.key === 'duijue') view.value = 'duel'
  if (item.key === 'chuanshu') view.value = 'letter'
  if (item.key === 'zongmen') view.value = 'sect'
  if (item.key === 'guanshantai') view.value = 'observer'
}

// 首页导航栏「后台」：仅执事（role=0）可见可入；已持执事令牌就直接回后台，否则去登录页
function onGoAdminLogin() {
  if (role.value !== 0) return // 山友无权入内（按钮本就不渲染，此为兜底）
  if (adminApi.isLogin()) {
    adminName.value = adminApi.currentName()
    view.value = 'admin'
    return
  }
  view.value = 'admin-login'
}

// 后台登录成功：进入后台管理系统
function onEnterAdmin(name) {
  adminName.value = name || ''
  view.value = 'admin'
}

// 后台退出：清名号，回首页
function onAdminLogout() {
  adminName.value = ''
  view.value = 'home'
}

// 背景粒子强度：登录 / 注册页满量，进入内容较多的页面后降帧降密度
const emberIntensity = computed(() => (view.value === 'login' || view.value === 'register' ? 1 : 0.45))

// 会话恢复：刷新 / 重开页面后，若本地还留着令牌就自动回到首页。
// 修复「首页一刷新就跳登录页」：
//   原逻辑把 view='home' 放在 await getUserInfo 之后，且 catch 里无条件清空令牌/会话。
//   真实后端模式下只要后端没启动 / 网络抖动 / 校验超时，请求一失败就被踢回登录页；
//   而且因为会话已被清掉，之后再也恢复不了，只能重新登录。
// 新策略：
//   1) 本地有「令牌 + 名号」就先用本地会话直接回首页 —— 刷新不再依赖后端是否在线；
//   2) 再后台异步校验一次，role 用服务端返回值校正（mock 下仍用本地值）；
//   3) 只有后端明确判定令牌无效/未登录/查无此山籍才清会话回登录页；
//      网络错误、超时、后端 5xx 一律保留本地登录态，避免误踢。
onMounted(async () => {
  const s = sessionStore.get()
  const token = tokenStore.get()
  if (!s || !s.username || !token) return

  // 先用本地会话恢复首页：不 await、不依赖后端，刷新立即可用
  username.value = s.username
  role.value = Number(s.role) === 0 ? 0 : 1
  view.value = 'home'

  // 后台异步校正权限：后端在线时以服务端 role 为准；失败则保留本地值
  try {
    const u = await homeApi.getUserInfo(s.username)
    username.value = (u && u.username) || s.username
    if (!USE_MOCK && u && typeof u.role !== 'undefined') {
      role.value = Number(u.role) === 0 ? 0 : 1
    }
  } catch (e) {
    const msg = (e && e.message) || ''
    // 仅「明确鉴权失败」才清会话；网络异常 / 超时 / 后端未启动一律保留登录态
    if (/令牌无效|签名不符|未登录|查无此山籍|请重新入境|重新登录|401|403/.test(msg)) {
      tokenStore.clear()
      sessionStore.clear()
      username.value = ''
      role.value = 1
      view.value = 'login'
    }
  }
})

const hint = computed(() =>
  view.value === 'login'
    ? 'VUE 重构 · 点五行可换主题色 · 任意非空名号暗语即可入山'
    : view.value === 'register'
      ? '立 契 · 本命既定，五行同辉 · 名号与暗语须经校验'
      : view.value === 'codex'
        ? '宝 库 · 功法心得资源灵根秘境 · 按元素与品阶翻检'
        : view.value === 'auction'
          ? '拍 卖 行 · 每 十 分 钟 一 场 · 出 价 五 秒 落 槌'
          : view.value === 'game'
            ? '修 行 · 测 灵 根 · 签 到 · 每 日 一 题'
            : view.value === 'admin-login'
          ? '后 台 · 执 事 入 内 · 此 处 只 通 执 事'
          : view.value === 'admin'
            ? '后 台 · 宝 库 数 据 管 理 · 改 动 即 时 生 效'
            : '雾 山 · 境 内 · 点五行可换主题色'
)
</script>

<template>
  <div class="app-root" :style="{ '--accent': accent, '--accent2': accent2 }">
    <!-- 阴阳图背景：仅登录 / 注册页显示，进入首页后淡出 -->
    <Transition name="bg-fade">
      <div v-if="view === 'login' || view === 'register'" class="bg-photo" :style="{ backgroundImage: `url(${bgYinyang})` }"></div>
    </Transition>
    <EmberBackground ref="bg" :ember-color="emberColor" :intensity="emberIntensity" />
    <FogMountains />
    <div class="fogband"></div>

    <div class="wrap" :class="{ 'wrap-home': view === 'home' || view === 'codex' || view === 'auction' || view === 'admin' }">
      <Transition name="swap" mode="out-in">
        <CodexPanel
          v-if="view === 'codex'"
          key="codex"
          @to-home="view = 'home'"
        />
        <AuctionPanel
          v-else-if="view === 'auction'"
          key="auction"
          @to-home="view = 'home'"
        />
        <GamePanel
          v-else-if="view === 'game'"
          key="game"
          @to-home="view = 'home'"
        />
        <WorldPanel
          v-else-if="['explore', 'alchemy', 'lianqi', 'zhenfa', 'duel', 'letter'].includes(view)"
          :key="view"
          :tab="view"
          @to-home="view = 'home'"
        />
        <SectPanel
          v-else-if="view === 'sect'"
          key="sect"
          @to-home="view = 'home'"
        />
        <ObserverPanel
          v-else-if="view === 'observer'"
          key="observer"
          @to-home="view = 'home'"
        />
        <HomePanel
          v-else-if="view === 'home'"
          key="home"
          :username="username"
          :role="role"
          @select="onSelect"
          @logout="onLogout"
          @nav="onNav"
          @to-admin="onGoAdminLogin"
        />
        <AdminPanel
          v-else-if="view === 'admin'"
          key="admin"
          :username="adminName"
          @logout="onAdminLogout"
          @to-home="view = 'home'"
        />
        <AdminLoginPanel
          v-else-if="view === 'admin-login'"
          key="admin-login"
          @to-admin="onEnterAdmin"
          @to-home="view = 'home'"
          @burst="onBurst"
        />
        <RegisterPanel
          v-else-if="view === 'register'"
          key="register"
          @select="onSelect"
          @burst="onBurst"
          @to-login="view = 'login'"
        />
        <LoginPanel
          v-else
          key="login"
          @select="onSelect"
          @burst="onBurst"
          @to-register="view = 'register'"
          @to-home="onEnterHome"
        />
      </Transition>
    </div>

    <!-- 后台管理页自带状态栏，不再叠一层底部提示 -->
    <div class="hint" v-if="view !== 'admin'">{{ hint }}</div>

    <!-- 全局开拍通知：在线用户无论在哪个页面都能收到（登录 / 注册页不打扰） -->
    <AuctionNotice v-if="view !== 'login' && view !== 'register'" />
  </div>
</template>

<style scoped>
.app-root {
  position: relative;
  min-height: 100%;
}
/* 阴阳图背景层：垫在粒子与远山之下（z-index 0），压暗保证面板可读 */
.bg-photo {
  position: fixed;
  inset: 0;
  z-index: 0;
  background-size: cover;
  background-position: center 30%;
  background-repeat: no-repeat;
  /* 注意：不要加 transform: translateZ(0) / will-change ——
     与全屏 canvas 叠成多个超大图层会让浏览器光栅预算吃紧，内容被延迟绘制 */
}
.bg-photo::after {
  content: '';
  position: absolute;
  inset: 0;
  background:
    radial-gradient(120% 90% at 50% 40%, rgba(10, 10, 9, 0) 40%, rgba(10, 10, 9, 0.55) 100%),
    linear-gradient(180deg, rgba(10, 10, 9, 0.45) 0%, rgba(10, 10, 9, 0.35) 45%, rgba(10, 10, 9, 0.62) 100%);
}
/* 背景图淡入淡出 */
.bg-fade-enter-active,
.bg-fade-leave-active {
  transition: opacity 0.6s ease;
}
.bg-fade-enter-from,
.bg-fade-leave-to {
  opacity: 0;
}
.wrap {
  position: relative;
  z-index: 2;
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}
/* 首页占满整屏，导航栏自带布局 */
.wrap-home {
  display: block;
  padding: 0;
}
.fogband {
  position: fixed;
  left: 0;
  bottom: 0;
  width: 100%;
  height: 42%;
  z-index: 1;
  pointer-events: none;
  background: linear-gradient(
    180deg,
    rgba(20, 30, 28, 0) 0%,
    rgba(30, 45, 42, 0.55) 70%,
    rgba(40, 60, 55, 0.8) 100%
  );
}
.hint {
  position: fixed;
  bottom: 16px;
  width: 100%;
  text-align: center;
  z-index: 2;
  color: var(--muted);
  font-size: 12px;
  letter-spacing: 2px;
}

/* 视图切换动效
   性能：只保留 opacity。
   之前带 transform: translateY + scale(0.98)，等于让 HomePanel / CodexPanel 这种
   几千个节点的整页组件在 0.32s 内做变换动画，很容易掉帧；
   而且 Vue 的 <Transition> 依赖 transitionend 收尾，动画被打断时元素会卡在
   opacity:0 的 enter-from 状态 —— 表现就是"页面空着，要动一下鼠标才出来"。 */
.swap-enter-active,
.swap-leave-active {
  transition: opacity 0.16s ease;
}
.swap-enter-from,
.swap-leave-to {
  opacity: 0;
}
</style>
