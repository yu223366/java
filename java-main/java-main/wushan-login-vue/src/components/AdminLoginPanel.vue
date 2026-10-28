<script setup>
// 后台登录面板：执事（管理员）专用入口
// mock 约定：名号 admin + 任意非空暗语即可入内；接后端后走 adminApi 真实鉴权
import { ref } from 'vue'
import { adminApi } from '../api/admin'

const emit = defineEmits(['to-admin', 'to-home', 'burst'])

const user = ref('')
const pass = ref('')
const err = ref('')
const loading = ref(false)
const loggedIn = ref(false)
const card = ref(null)

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
    err.value = '执事名号与暗语，皆不可空。'
    shake()
    return
  }
  err.value = ''
  loading.value = true
  try {
    const s = await adminApi.login({ username: user.value.trim(), password: pass.value })
    // 成功瞬间引爆火星，与登录页仪式感保持一致
    const r = card.value.getBoundingClientRect()
    emit('burst', { x: r.left + r.width / 2, y: r.top + r.height / 2, n: 80, spread: 4 })
    setTimeout(() => {
      loading.value = false
      loggedIn.value = true
      // 落印动画展示约 1.2s 后进入后台
      setTimeout(() => emit('to-admin', s.name || user.value.trim()), 1200)
    }, 700)
  } catch (e) {
    loading.value = false
    err.value = e instanceof Error ? e.message : '山门未应，请稍后重试。'
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
    <div v-if="!loggedIn">
      <div class="head">
        <div class="title">执事<small>后 台 · 入 内</small></div>
        <div class="seal">执<br />事</div>
      </div>

      <p class="tip">此处只通执事。宝库数据，改动即时生效。</p>

      <label>执事名号</label>
      <div class="field">
        <input v-model="user" placeholder="执事落款…" autocomplete="off" @keydown.enter="submit" />
        <span class="line"></span>
      </div>

      <label>暗 语</label>
      <div class="field">
        <input v-model="pass" type="password" placeholder="••••••" @keydown.enter="submit" />
        <span class="line"></span>
      </div>

      <button class="seal-btn" :disabled="loading" @click="submit">
        {{ loading ? '验 印…' : '入 内' }}
      </button>
      <div class="err">{{ err }}</div>
      <div class="foot">
        非执事？<a href="#" @click.prevent="emit('to-home')">回 前 山</a>
      </div>
    </div>

    <!-- 成功视图 -->
    <div class="success" v-else>
      <div class="big">印 已 验</div>
      <div class="mark">执<br />事</div>
      <p class="sub">执事 {{ user.trim() }}，宝库已开。</p>
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
.tip {
  margin: 20px 0 4px;
  font-size: 13px;
  letter-spacing: 2px;
  color: var(--muted);
}
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
  border: none;
  outline: none;
}
.line {
  display: block;
  height: 1px;
  background: var(--line);
  position: relative;
}
.line::after {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  height: 100%;
  width: 100%;
  background: var(--accent);
  transform: scaleX(0);
  transform-origin: left;
  transition: transform 0.3s ease;
}
.field:focus-within .line::after {
  transform: scaleX(1);
}
.seal-btn {
  width: 100%;
  margin-top: 28px;
  padding: 13px;
  font-size: 17px;
  letter-spacing: 10px;
  font-family: inherit;
  color: #fff8ee;
  background: var(--accent);
  border: none;
  border-radius: 4px;
  cursor: pointer;
  position: relative;
  box-shadow: 0 8px 24px rgba(232, 65, 43, 0.35);
  transition: transform 0.15s ease, box-shadow 0.15s ease, filter 0.15s ease;
}
.seal-btn:hover {
  filter: brightness(1.1);
  transform: translateY(-2px);
  box-shadow: 0 12px 30px rgba(232, 65, 43, 0.45);
}
.seal-btn:active {
  transform: translateY(0) scale(0.99);
}
.seal-btn:disabled {
  opacity: 0.65;
  cursor: not-allowed;
  transform: none;
}
.err {
  min-height: 20px;
  margin-top: 12px;
  text-align: center;
  font-size: 13px;
  letter-spacing: 1px;
  color: var(--accent);
}
.foot {
  margin-top: 14px;
  text-align: center;
  font-size: 13px;
  letter-spacing: 2px;
  color: var(--muted);
}
.foot a {
  color: var(--accent);
  text-decoration: none;
  letter-spacing: 3px;
}
.success {
  text-align: center;
  padding: 30px 0 20px;
}
.success .big {
  font-size: 40px;
  letter-spacing: 12px;
  color: var(--accent);
  text-shadow: 0 0 22px rgba(232, 65, 43, 0.6);
}
.success .mark {
  width: 84px;
  height: 84px;
  margin: 26px auto;
  border-radius: 8px;
  background: var(--accent);
  color: #fff8ee;
  display: grid;
  place-items: center;
  font-size: 38px;
  border: 3px solid #9c2418;
  transform: rotate(-5deg);
  box-shadow: inset 0 0 0 3px rgba(255, 248, 238, 0.25), 0 6px 20px rgba(0, 0, 0, 0.5);
  line-height: 1.05;
  animation: stamp 0.5s cubic-bezier(0.2, 1.6, 0.4, 1);
}
@keyframes stamp {
  0% {
    transform: rotate(-5deg) scale(2.2);
    opacity: 0;
  }
  100% {
    transform: rotate(-5deg) scale(1);
    opacity: 1;
  }
}
.success .sub {
  margin-top: 8px;
  font-size: 14px;
  letter-spacing: 3px;
  color: var(--muted);
}
</style>
