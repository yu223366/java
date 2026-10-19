<script setup>
/**
 * 注册面板 · 立契
 * 风格：延续「雾山五行」水墨暗调面板
 * 交互：单页制（本命 · 名号 · 暗语 · 图形符印 同屏一次填完）
 * 校验：名号（含后端查重）、暗语强度与一致、图形符印、盟约
 * 接口：全部走 src/api/auth.js（预留后端，默认 mock 可离线跑通）
 */
import { ref, reactive, computed, onMounted } from 'vue'
import WuxingPicker from './WuxingPicker.vue'
import CaptchaBox from './CaptchaBox.vue'
import { authApi } from '../api/auth'
import { DEFAULT_ELEMENT, findElement } from '../constants/wuxing'
import {
  PASSWORD_RULES,
  rulePass,
  passwordScore,
  validateUsername,
  validatePassword,
  validateConfirm
} from '../utils/validators'

const emit = defineEmits(['select', 'burst', 'to-login'])

/* ------------------------------ 状态 ------------------------------ */
const element = ref(DEFAULT_ELEMENT)
const card = ref(null)

const form = reactive({
  username: '',
  captchaCode: '',
  password: '',
  confirm: '',
  agree: false
})
const errors = reactive({
  username: '',
  captcha: '',
  password: '',
  confirm: '',
  agree: '',
  other: ''
})
const captchaId = ref('')
const captchaRef = ref(null)

// 后端模式下图形符印由服务端生成图片（base64）；mock 下走本地 Canvas
const USE_MOCK = import.meta.env.VITE_USE_MOCK !== 'false'
const captchaImage = ref('')

/** 拉取后端图形符印（仅后端模式有效） */
function loadCaptcha() {
  if (USE_MOCK) return
  authApi
    .getCaptcha()
    .then((r) => {
      captchaImage.value = (r && r.image) || ''
      captchaId.value = (r && r.captchaId) || ''
    })
    .catch(() => {
      captchaImage.value = ''
    })
}

const nameState = ref('idle') // idle | checking | ok | bad
const submitting = ref(false)
const done = ref(false)

const currentElement = computed(() => findElement(element.value))
const strength = computed(() => passwordScore(form.password))

/* ------------------------------ 交互 ------------------------------ */
function pickElement({ color, huo, key }) {
  element.value = key
  clearErr()
  emit('select', { color, huo })
}

// 图形验证码 id 回传（后端模式下用于二次校验）
function setCaptchaId(v) {
  captchaId.value = v
}

function clearErr(...keys) {
  const ks = keys.length ? keys : Object.keys(errors)
  ks.forEach((k) => (errors[k] = ''))
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

/** 名号失焦：本地校验 + 后端查重（预留接口） */
async function checkName() {
  nameState.value = 'idle'
  const msg = validateUsername(form.username)
  errors.username = msg || ''
  if (msg) return false

  nameState.value = 'checking'
  try {
    const r = await authApi.checkUsername(form.username.trim())
    if (r && r.available === false) {
      errors.username = '此名号已有人立契，请另择一名。'
      nameState.value = 'bad'
      return false
    }
    nameState.value = 'ok'
    return true
  } catch (e) {
    // 查重接口异常不阻断流程，最终由服务端在校验
    nameState.value = 'idle'
    return true
  }
}

/** 提交：立契（名号 / 暗语 / 符印 同页一次校验） */
async function submit() {
  clearErr()
  if (submitting.value) return

  // 一、名号（本地规则 + 后端查重）
  const nameOk = await checkName()
  // 二、暗语与一致性
  const pwMsg = validatePassword(form.password, form.username)
  const cfMsg = validateConfirm(form.password, form.confirm)
  const agMsg = form.agree ? '' : '需先允诺山规，方可立契。'
  errors.password = pwMsg || ''
  errors.confirm = cfMsg || ''
  errors.agree = agMsg

  // 三、图形符印（后端模式下由服务端比对，此处只校验非空与位数）
  const cap = captchaRef.value ? captchaRef.value.validate() : { ok: true }
  if (!cap.ok) errors.captcha = cap.msg

  if (!nameOk || pwMsg || cfMsg || agMsg || !cap.ok) {
    shake()
    return
  }

  submitting.value = true
  // 成功瞬间：面板中心引爆火星（与登录页同一套动效）
  const rect = card.value.getBoundingClientRect()
  emit('burst', { x: rect.left + rect.width / 2, y: rect.top + rect.height / 2, n: 90, spread: 4 })

  try {
    // 预留：后端二次校验图形验证码
    await authApi.verifyCaptcha({ captchaId: cap.captchaId || captchaId.value, captchaCode: cap.captchaCode })

    // 预留：注册接口
    const res = await authApi.register({
      username: form.username.trim(),
      password: form.password,
      element: element.value,
      elementLabel: currentElement.value.label,
      captchaId: cap.captchaId || captchaId.value,
      captchaCode: cap.captchaCode,
      agreed: form.agree,
      agreedAt: new Date().toISOString()
    })
    if (res?.token) {
      // 需要保存登录态时打开（tokenStore 已在 api/auth.js 中预留）
      // tokenStore.set(res.token)
    }
    setTimeout(() => {
      submitting.value = false
      done.value = true
    }, 700)
  } catch (e) {
    submitting.value = false
    errors.other = e.message || '立契未成，请稍后重试。'
    if (!USE_MOCK) loadCaptcha() // 符印已作废，换一张
    shake()
  }
}

onMounted(() => {
  if (!USE_MOCK) loadCaptcha()
})
</script>

<template>
  <div class="panel" ref="card">
    <svg class="brush" viewBox="0 0 150 46">
      <path d="M6,38 C40,8 90,6 144,14 C120,22 70,30 30,40 C20,42 12,42 6,38 Z" fill="var(--accent)" />
    </svg>

    <!-- 注册表单 -->
    <div v-if="!done">
      <div class="head">
        <div class="title">立契<small>五 行 · 入 籍</small></div>
        <div class="seal">立<br />契</div>
      </div>

      <!-- 表单：本命 · 名号 · 暗语 · 符印 同屏 -->
      <div class="stage">
        <label>本 命</label>
        <WuxingPicker v-model="element" size="lg" @select="pickElement" />
        <div class="trait">{{ currentElement.label }} · {{ currentElement.trait }}</div>

        <label>名 号</label>
        <div class="field">
          <input
            v-model="form.username"
            placeholder="二至十六字，汉字 / 字母 / 数字"
            autocomplete="off"
            @input="nameState = 'idle'"
            @blur="checkName"
            @keydown.enter="submit"
          />
          <span class="line"></span>
          <span class="flag" :class="nameState">
            {{ nameState === 'checking' ? '查籍中…' : nameState === 'ok' ? '山籍可用' : nameState === 'bad' ? '已被占用' : '' }}
          </span>
        </div>
        <div class="tip">二至十六字，汉字 / 字母 / 数字；失焦即查重。</div>

        <label>暗 语</label>
        <div class="field">
          <input
            v-model="form.password"
            type="password"
            placeholder="八至二十位，字母 + 数字"
            autocomplete="new-password"
            @input="clearErr('password')"
          />
          <span class="line"></span>
        </div>
        <div class="meter">
          <i
            v-for="n in 5"
            :key="n"
            :style="{ background: n <= strength.score ? strength.color : 'rgba(236,228,210,0.12)' }"
          ></i>
          <em :style="{ color: strength.color }">{{ strength.label }}</em>
        </div>
        <ul class="rules">
          <li v-for="r in PASSWORD_RULES" :key="r.key" :class="{ pass: rulePass(r, form.password) }">
            {{ rulePass(r, form.password) ? '✓' : '○' }} {{ r.text }}
          </li>
        </ul>

        <label>重 录 暗 语</label>
        <div class="field">
          <input
            v-model="form.confirm"
            type="password"
            placeholder="再录一次，以验其准"
            autocomplete="new-password"
            @input="clearErr('confirm')"
            @keydown.enter="submit"
          />
          <span class="line"></span>
          <span class="flag" :class="form.confirm && !validateConfirm(form.password, form.confirm) ? 'ok' : ''">
            {{ form.confirm && !validateConfirm(form.password, form.confirm) ? '一致' : '' }}
          </span>
        </div>

        <label>符 印</label>
        <CaptchaBox
          ref="captchaRef"
          v-model="form.captchaCode"
          :accent="currentElement.color"
          :image-src="captchaImage"
          :captcha-id="captchaId"
          @update:captcha-id="setCaptchaId"
          @refresh="loadCaptcha"
        />
        <div class="tip">不识符印？点右侧图案可另换一张。</div>

        <label class="agree">
          <input type="checkbox" v-model="form.agree" @change="clearErr('agree')" />
          <span>允诺山规：一契一名，暗语自守。</span>
        </label>
      </div>

      <!-- 错误汇总 -->
      <div class="err">{{ errors.username || errors.captcha || errors.password || errors.confirm || errors.agree || errors.other }}</div>

      <!-- 操作 -->
      <div class="actions">
        <button class="seal-btn" :disabled="submitting" @click="submit">
          {{ submitting ? '落 印…' : '立 契' }}
        </button>
      </div>

      <div class="foot">
        已有山籍？<a href="#" @click.prevent="emit('to-login')">入 境</a>
      </div>
    </div>

    <!-- 成功视图 -->
    <div class="success" v-else>
      <div class="big">立 契 已 成</div>
      <div class="mark">入<br />籍</div>
      <p class="sub">
        {{ currentElement.label }}命既立，{{ form.username.trim() }} 已入山籍。
      </p>
      <button class="seal-btn" @click="emit('to-login')">前 往 入 境</button>
    </div>
  </div>
</template>

<style scoped>
.panel {
  width: 420px;
  max-width: 100%;
  max-height: calc(100vh - 72px);
  overflow-y: auto;
  padding: 40px 34px 30px;
  border-radius: 4px;
  position: relative;
  background: linear-gradient(180deg, rgba(18, 18, 16, 0.72), rgba(10, 10, 9, 0.8));
  border: 1px solid rgba(236, 228, 210, 0.16);
  box-shadow: 0 28px 80px rgba(0, 0, 0, 0.55), inset 0 0 60px rgba(232, 65, 43, 0.06);
  backdrop-filter: blur(3px);
}
.panel::-webkit-scrollbar {
  width: 6px;
}
.panel::-webkit-scrollbar-thumb {
  background: rgba(236, 228, 210, 0.18);
  border-radius: 3px;
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
  font-size: 46px;
  letter-spacing: 8px;
  line-height: 1;
  color: var(--accent);
  text-shadow: 0 0 18px rgba(232, 65, 43, 0.55), 0 2px 2px rgba(0, 0, 0, 0.5);
  font-weight: 700;
}
.title small {
  display: block;
  font-size: 13px;
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
  font-size: 22px;
  border: 2px solid #9c2418;
  transform: rotate(-4deg);
  box-shadow: inset 0 0 0 2px rgba(255, 248, 238, 0.22), 0 3px 8px rgba(0, 0, 0, 0.4);
  text-align: center;
  line-height: 1.05;
  padding: 3px;
}

label {
  font-size: 13px;
  letter-spacing: 3px;
  color: var(--muted);
  display: block;
  margin: 18px 0 8px;
}
.trait {
  text-align: center;
  font-size: 12px;
  letter-spacing: 3px;
  color: var(--muted);
  margin-top: 10px;
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
  font-size: 14px;
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
.flag {
  position: absolute;
  right: 2px;
  bottom: 12px;
  font-size: 12px;
  letter-spacing: 1px;
  color: var(--muted);
}
.flag.ok {
  color: #4caf6e;
}
.flag.bad {
  color: #e8412b;
}
.tip {
  font-size: 12px;
  letter-spacing: 1px;
  color: rgba(236, 228, 210, 0.42);
  margin-top: 8px;
}

/* 密码强度 */
.meter {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 12px;
}
.meter i {
  flex: 1;
  height: 4px;
  border-radius: 2px;
  transition: background 0.3s;
}
.meter em {
  font-style: normal;
  font-size: 12px;
  letter-spacing: 2px;
  min-width: 36px;
  text-align: right;
}
.rules {
  list-style: none;
  display: flex;
  flex-wrap: wrap;
  gap: 6px 14px;
  margin-top: 10px;
  font-size: 12px;
  letter-spacing: 1px;
  color: rgba(236, 228, 210, 0.4);
}
.rules li.pass {
  color: #4caf6e;
}

label.agree {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 22px 0 4px;
  letter-spacing: 1px;
  cursor: pointer;
  font-size: 13px;
}
label.agree input {
  flex: none;
  width: 15px;
  height: 15px;
  padding: 0;
  margin: 0;
  border: 0;
  background: transparent;
  accent-color: var(--accent);
  cursor: pointer;
}

.actions {
  display: flex;
  gap: 12px;
  margin-top: 22px;
}
button.seal-btn {
  flex: 1;
  padding: 13px;
  border: 2px solid #9c2418;
  cursor: pointer;
  font-family: inherit;
  font-size: 19px;
  letter-spacing: 8px;
  color: #fff8ee;
  background: var(--accent);
  border-radius: 6px;
  transform: rotate(-1.2deg);
  transition: 0.18s;
  position: relative;
  box-shadow: inset 0 0 0 2px rgba(255, 248, 238, 0.2), 0 5px 16px rgba(232, 65, 43, 0.45);
}
button.seal-btn:hover:not(:disabled) {
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
  margin-top: 14px;
  text-align: center;
  letter-spacing: 1px;
}
.foot {
  margin-top: 18px;
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
  font-size: 32px;
  letter-spacing: 8px;
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
  letter-spacing: 2px;
  margin: 0 0 20px;
  font-size: 13px;
}
</style>
