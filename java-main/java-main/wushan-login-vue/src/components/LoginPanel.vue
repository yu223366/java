<script setup>
// 登录面板：五行换色 / 名号暗语输入 / 校验 / 登录成功「落印」动画
import { ref } from 'vue'
import { authApi, tokenStore } from '../api/auth'
import { ELEMENTS } from '../constants/wuxing'

const emit = defineEmits(['select', 'burst', 'to-register', 'to-home'])

const elements = ELEMENTS

const active = ref('huo')
const user = ref('')
const pass = ref('')
const rem = ref(false)
const err = ref('')
const loading = ref(false)
const loggedIn = ref(false)
const card = ref(null)

function pick(el) {
  active.value = el.key
  emit('select', { color: el.color, huo: el.huo })
}

function shake() {
  if (!card.value) return
  card.value.animate(
    [
      { transform: 'translateX(0)' },
      { transform: 'translateX(-8px)' },
      { transform: 'translateX(7px)' },
      { transform: 'translateX(-5px)' },
      { transform: 'translateX(0)' }
    ],
    { duration: 420 }
  )
}

async function submit() {
  if (!user.value.trim() || !pass.value.trim()) {
    err.value = '名号与暗语，皆不可空。'
    shake()
    return
  }
  err.value = ''
  loading.value = true
  const r = card.value.getBoundingClientRect()
  emit('burst', { x: r.left + r.width / 2, y: r.top + r.height / 2, n: 80, spread: 4 })
  try {
    // 预留接口：接后端时直接走真实登录（mock 模式下仍为本地模拟）
    const res = await authApi.login({ username: user.value.trim(), password: pass.value })
    if (res && res.token && rem.value) tokenStore.set(res.token)
    setTimeout(() => {
      loading.value = false
      loggedIn.value = true
      // 落印动画展示约 1.4s 后，携名号进入首页
      setTimeout(() => emit('to-home', user.value.trim()), 1400)
    }, 850)
  } catch (e) {
    loading.value = false
    err.value = (e && e.message) || '入境未成，请核对名号暗语。'
    shake()
  }
}
</script>

<template>
  <div class="panel" ref="card">
    <svg class="brush" viewBox="0 0 150 46">
      <path d="M6,38 C40,8 90,6 144,14 C120,22 70,30 30,40 C20,42 12,42 6,38 Z" fill="var(--accent)" />
    </svg>

    <!-- 登录视图 -->
    <div id="formView" v-if="!loggedIn">
      <div class="head">
        <div class="title">雾山<small>五 行 · 入 境</small></div>
        <div class="seal">雾<br />山</div>
      </div>

      <div class="wuxing">
        <div
          v-for="el in elements"
          :key="el.key"
          class="wx"
          :class="[el.key, { 'is-on': active === el.key }]"
          @click="pick(el)"
        >
          {{ el.label }}<small>{{ el.en }}</small>
        </div>
      </div>

      <label>名 号</label>
      <div class="field">
        <input v-model="user" placeholder="入山落款…" autocomplete="off" @keydown.enter="submit" />
        <span class="line"></span>
      </div>

      <label>暗 语</label>
      <div class="field">
        <input v-model="pass" type="password" placeholder="••••••" @keydown.enter="submit" />
        <span class="line"></span>
      </div>

      <div class="row">
        <label class="rem"><input type="checkbox" v-model="rem" /> 铭记于山</label>
        <a href="#" @click.prevent>忘却暗语？</a>
      </div>

      <button class="seal-btn" :disabled="loading" @click="submit">
        {{ loading ? '落 印…' : '入 山' }}
      </button>
      <div class="err">{{ err }}</div>
      <div class="foot">
        尚无山籍？<a href="#" @click.prevent="emit('to-register')">立 契</a>
      </div>
    </div>

    <!-- 成功视图 -->
    <div class="success" v-else>
      <div class="big">已 入 山</div>
      <div class="mark">入<br />山</div>
      <p class="sub">五行归一，{{ user.trim() }} 已至此。</p>
    </div>
  </div>
</template>

<style scoped>
.panel {
  width: 390px;
  max-width: 100%;
  padding: 42px 36px 36px;
  border-radius: 4px;
  position: relative;
  background: linear-gradient(180deg, rgba(18, 18, 16, 0.72), rgba(10, 10, 9, 0.78));
  border: 1px solid rgba(236, 228, 210, 0.16);
  box-shadow: 0 28px 80px rgba(0, 0, 0, 0.55), inset 0 0 60px rgba(232, 65, 43, 0.06);
  backdrop-filter: blur(3px);
}
.brush {
  position: absolute;
  top: -26px;
  left: -14px;
  width: 150px;
  height: 46px;
  z-index: 0;
  opacity: 0.9;
  filter: drop-shadow(0 4px 10px rgba(232, 65, 43, 0.5));
}
.head {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
}
.title {
  font-size: 50px;
  letter-spacing: 8px;
  line-height: 1;
  color: var(--accent);
  text-shadow: 0 0 18px rgba(232, 65, 43, 0.55), 0 2px 2px rgba(0, 0, 0, 0.5);
  font-weight: 700;
}
.title small {
  display: block;
  font-size: 14px;
  letter-spacing: 6px;
  color: var(--muted);
  margin-top: 10px;
  font-weight: 400;
}
.seal {
  width: 50px;
  height: 50px;
  border-radius: 6px;
  background: var(--accent);
  color: #fff8ee;
  display: grid;
  place-items: center;
  font-size: 24px;
  border: 2px solid #9c2418;
  transform: rotate(-4deg);
  box-shadow: inset 0 0 0 2px rgba(255, 248, 238, 0.22), 0 3px 8px rgba(0, 0, 0, 0.4);
  text-align: center;
  line-height: 1.05;
  padding: 3px;
}

.wuxing {
  display: flex;
  gap: 8px;
  margin: 22px 0 6px;
  justify-content: center;
  flex-wrap: wrap; /* 十种元素自动换行 */
}
.wx {
  padding: 6px 0;
  width: 52px;
  text-align: center;
  border-radius: 8px;
  cursor: pointer;
  font-size: 17px;
  letter-spacing: 2px;
  border: 1px solid rgba(236, 228, 210, 0.18);
  background: rgba(236, 228, 210, 0.04);
  transition: 0.2s;
  color: var(--paper);
}
.wx:hover {
  transform: translateY(-3px);
}
.wx.is-on {
  box-shadow: 0 0 0 2px currentColor, 0 6px 18px rgba(0, 0, 0, 0.4);
}
.wx small {
  display: block;
  font-size: 10px;
  opacity: 0.6;
  letter-spacing: 1px;
}
.wx.jin {
  color: #e8c372;
}
.wx.mu {
  color: #4caf6e;
}
.wx.shui {
  color: #3a8fd0;
}
.wx.huo {
  color: #e8412b;
}
.wx.tu {
  color: #c8923a;
}
/* 衍生元素 */
.wx.feng { color: #7fd8cf; }
.wx.lei { color: #9d7bf0; }
.wx.dian { color: #f2e34d; }
.wx.taiji { color: #d8d2c2; }
.wx.yinyang { color: #98a4b0; }

label {
  font-size: 13px;
  letter-spacing: 3px;
  color: var(--muted);
  display: block;
  margin: 20px 0 8px;
}
.field {
  position: relative;
}
input {
  width: 100%;
  padding: 10px 4px;
  font-size: 16px;
  font-family: inherit;
  color: var(--paper);
  background: transparent;
  border: 0;
  border-bottom: 1.5px solid var(--line);
  outline: none;
}
input::placeholder {
  color: rgba(236, 228, 210, 0.3);
}
.field .line {
  position: absolute;
  left: 0;
  bottom: 0;
  height: 2px;
  width: 0;
  background: linear-gradient(90deg, var(--accent), var(--accent2));
  transition: width 0.4s ease;
  box-shadow: 0 0 10px var(--accent);
}
input:focus ~ .line {
  width: 100%;
}

.row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 22px 0 24px;
  font-size: 13px;
  color: var(--muted);
  letter-spacing: 1px;
}
.row .rem {
  margin: 0;
  display: flex;
  align-items: center;
  gap: 7px;
  cursor: pointer;
}
.row a {
  color: var(--accent2);
  text-decoration: none;
}

button.seal-btn {
  width: 100%;
  padding: 14px;
  border: 2px solid #9c2418;
  cursor: pointer;
  font-family: inherit;
  font-size: 20px;
  letter-spacing: 10px;
  color: #fff8ee;
  background: var(--accent);
  border-radius: 6px;
  transform: rotate(-1.5deg);
  transition: 0.18s;
  position: relative;
  box-shadow: inset 0 0 0 2px rgba(255, 248, 238, 0.2), 0 5px 16px rgba(232, 65, 43, 0.45);
}
button.seal-btn:hover {
  transform: rotate(0) scale(1.02);
  filter: brightness(1.08);
}
button.seal-btn:disabled {
  opacity: 0.85;
  cursor: default;
}

.err {
  color: var(--accent2);
  font-size: 13px;
  min-height: 16px;
  margin-top: 12px;
  text-align: center;
  letter-spacing: 1px;
}
.foot {
  margin-top: 14px;
  text-align: center;
  font-size: 13px;
  letter-spacing: 1px;
  color: var(--muted);
}
.foot a {
  color: var(--accent2);
  text-decoration: none;
  margin-left: 4px;
}

.success {
  text-align: center;
  padding-top: 8px;
}
.success .big {
  font-size: 38px;
  letter-spacing: 12px;
  color: var(--accent);
  text-shadow: 0 0 16px rgba(232, 65, 43, 0.5);
}
.success .mark {
  width: 92px;
  height: 92px;
  margin: 18px auto;
  border-radius: 9px;
  background: var(--accent);
  color: #fff8ee;
  display: grid;
  place-items: center;
  font-size: 30px;
  line-height: 1.1;
  text-align: center;
  border: 3px solid #9c2418;
  transform: rotate(-5deg) scale(0);
  animation: stamp 0.5s 0.1s forwards cubic-bezier(0.2, 1.4, 0.4, 1);
  box-shadow: inset 0 0 0 3px rgba(255, 248, 238, 0.22);
}
@keyframes stamp {
  to {
    transform: rotate(-5deg) scale(1);
  }
}
.success .sub {
  color: var(--muted);
  letter-spacing: 3px;
  margin-top: 0;
}
</style>
