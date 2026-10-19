<script setup>
/**
 * 图形验证码（符印）
 *  - 本地模式（默认 / VITE_USE_MOCK !== 'false'）：Canvas 本地绘制 + 前端比对
 *  - 后端模式：传入 imageSrc（后端返回的图片地址或 base64），比对交由后端完成
 *    使用：<CaptchaBox ref="cap" v-model="form.captchaCode" :image-src="capImage" />
 */
import { ref, watch, onMounted } from 'vue'
import { randomText, drawCaptcha } from '../utils/captcha'
import { validateCaptchaCode } from '../utils/validators'

const props = defineProps({
  modelValue: { type: String, default: '' },
  captchaId: { type: String, default: '' },
  length: { type: Number, default: 4 },
  imageSrc: { type: String, default: '' }, // 后端模式：图形验证码图片
  accent: { type: String, default: '#e8412b' },
  placeholder: { type: String, default: '符 印' }
})
const emit = defineEmits(['update:modelValue', 'update:captchaId', 'refresh'])

const canvas = ref(null)
const answer = ref('')
const uid = ref('')
const wrong = ref(false)

/** 换一张符印 */
function refresh(silent = false) {
  wrong.value = false
  if (props.imageSrc) {
    // 后端模式：交由父组件重新拉取图片与真实 captchaId，自身不生成伪 id
    emit('refresh')
  } else {
    answer.value = randomText(props.length)
    uid.value = 'cap_' + Date.now().toString(36) + Math.random().toString(36).slice(2, 8)
    emit('update:captchaId', uid.value)
    if (!silent && canvas.value) drawCaptcha(canvas.value, answer.value, { accent: props.accent })
  }
  if (!silent) {
    emit('update:modelValue', '')
  }
  emit('refresh')
}

/**
 * 前端校验
 * @returns {{ ok: boolean, msg?: string, captchaId: string, captchaCode: string }}
 */
function validate() {
  const code = (props.modelValue || '').trim()
  const base = { captchaId: uid.value, captchaCode: code }

  const emptyMsg = validateCaptchaCode(code, props.length)
  if (emptyMsg) {
    return { ok: false, msg: emptyMsg, ...base }
  }
  // 后端模式：交由服务端比对，前端只做非空与长度校验
  if (props.imageSrc) return { ok: true, captchaId: props.captchaId, captchaCode: code }

  if (code.toUpperCase() !== answer.value.toUpperCase()) {
    refresh()
    wrong.value = true
    return { ok: false, msg: '符印不合，已换新符，请重填。', ...base }
  }
  return { ok: true, ...base }
}

/** 只读快照，供父组件提交时取 id */
function snapshot() {
  return { captchaId: uid.value, captchaCode: (props.modelValue || '').trim() }
}

defineExpose({ refresh, validate, snapshot })

watch(
  () => props.modelValue,
  (v) => {
    if (v) wrong.value = false
  }
)

onMounted(() => {
  if (props.imageSrc) {
    uid.value = props.captchaId || 'cap_remote'
  } else {
    refresh(true)
  }
})
</script>

<template>
  <div class="cap-box" :class="{ wrong }">
    <div class="cap-field">
      <input
        class="cap-input"
        :value="modelValue"
        :maxlength="length"
        :placeholder="placeholder"
        autocomplete="off"
        spellcheck="false"
        @input="emit('update:modelValue', $event.target.value.toUpperCase())"
      />
      <span class="line"></span>
    </div>
    <div class="cap-img" title="点击换一张" @click="refresh()">
      <img v-if="imageSrc" :src="imageSrc" alt="captcha" />
      <canvas v-else ref="canvas"></canvas>
    </div>
  </div>
</template>

<style scoped>
.cap-box {
  display: flex;
  gap: 12px;
  align-items: flex-start;
}
.cap-field {
  position: relative;
  flex: 1;
}
.cap-input {
  width: 100%;
  padding: 10px 4px;
  font-size: 16px;
  font-family: inherit;
  color: var(--paper);
  background: transparent;
  border: 0;
  border-bottom: 1.5px solid var(--line);
  outline: none;
  letter-spacing: 6px;
  text-transform: uppercase;
}
.cap-input::placeholder {
  color: rgba(236, 228, 210, 0.3);
  letter-spacing: 2px;
}
.cap-box.wrong .cap-input {
  border-bottom-color: #e8412b;
}
.line {
  position: absolute;
  left: 0;
  bottom: 0;
  height: 2px;
  width: 0;
  background: linear-gradient(90deg, var(--accent), var(--accent2));
  transition: width 0.4s ease;
  box-shadow: 0 0 10px var(--accent);
}
.cap-input:focus ~ .line {
  width: 100%;
}
.cap-img {
  cursor: pointer;
  border-radius: 4px;
  overflow: hidden;
  display: grid;
  place-items: center;
  transition: filter 0.2s;
  line-height: 0;
}
.cap-img:hover {
  filter: brightness(1.2);
}
.cap-img img {
  display: block;
  width: 128px;
  height: 46px;
  object-fit: cover;
}
</style>
