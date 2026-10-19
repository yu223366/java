<script setup>
/**
 * 六爻密印输入框 —— 六格分离式验证码输入
 * 支持：自动跳格、退格回退、方向键移动、整串粘贴、输入即完成回调
 */
import { ref, watch, nextTick } from 'vue'

const props = defineProps({
  modelValue: { type: String, default: '' },
  length: { type: Number, default: 6 },
  disabled: { type: Boolean, default: false }
})
const emit = defineEmits(['update:modelValue', 'complete'])

const list = ref(Array.from({ length: props.length }, () => ''))
const cells = ref([])

function syncFromProps(v) {
  const arr = String(v || '').replace(/\D/g, '').split('').slice(0, props.length)
  while (arr.length < props.length) arr.push('')
  if (arr.join('') !== list.value.join('')) list.value = arr
}

watch(() => props.modelValue, syncFromProps, { immediate: true })
watch(() => props.length, () => {
  list.value = Array.from({ length: props.length }, () => '')
  syncFromProps(props.modelValue)
})

function commit() {
  const v = list.value.join('')
  emit('update:modelValue', v)
  if (list.value.every((c) => c !== '')) emit('complete', v)
}

function focusAt(i) {
  const el = cells.value[i]
  if (el) el.focus()
}

function onInput(i, e) {
  const raw = String(e.target.value || '').replace(/\D/g, '')
  if (!raw) {
    list.value[i] = ''
    commit()
    return
  }
  if (raw.length > 1) {
    // 粘贴整串
    for (let k = 0; k < raw.length && i + k < props.length; k++) list.value[i + k] = raw[k]
    commit()
    nextTick(() => focusAt(Math.min(i + raw.length, props.length - 1)))
    return
  }
  list.value[i] = raw
  commit()
  if (i < props.length - 1) nextTick(() => focusAt(i + 1))
}

function onKeydown(i, e) {
  if (e.key === 'Backspace') {
    if (!list.value[i] && i > 0) {
      list.value[i - 1] = ''
      commit()
      nextTick(() => focusAt(i - 1))
    }
    return
  }
  if (e.key === 'ArrowLeft' && i > 0) focusAt(i - 1)
  if (e.key === 'ArrowRight' && i < props.length - 1) focusAt(i + 1)
  if (e.key === 'Enter') commit()
}

function onPaste(e) {
  const text = (e.clipboardData || window.clipboardData).getData('text')
  const raw = String(text || '').replace(/\D/g, '')
  if (!raw) return
  e.preventDefault()
  for (let k = 0; k < props.length; k++) list.value[k] = raw[k] || ''
  commit()
  nextTick(() => focusAt(Math.min(raw.length, props.length - 1)))
}

defineExpose({ focusFirst: () => focusAt(0) })
</script>

<template>
  <div class="otp" :class="{ disabled }" @paste="onPaste">
    <input
      v-for="(c, i) in list"
      :key="i"
      :ref="(el) => (cells[i] = el)"
      class="otp-cell"
      :value="c"
      inputmode="numeric"
      autocomplete="one-time-code"
      :disabled="disabled"
      @input="onInput(i, $event)"
      @keydown="onKeydown(i, $event)"
      @focus="$event.target.select()"
    />
  </div>
</template>

<style scoped>
.otp {
  display: flex;
  gap: 8px;
  justify-content: space-between;
}
.otp-cell {
  flex: 1;
  width: 100%;
  min-width: 0;
  height: 48px;
  text-align: center;
  font-size: 20px;
  font-family: inherit;
  color: var(--paper);
  background: rgba(236, 228, 210, 0.04);
  border: 1px solid rgba(236, 228, 210, 0.2);
  border-radius: 6px;
  outline: none;
  transition: 0.2s;
  caret-color: var(--accent);
}
.otp-cell:focus {
  border-color: var(--accent);
  box-shadow: 0 0 0 1px var(--accent), 0 0 14px rgba(232, 65, 43, 0.28);
  background: rgba(236, 228, 210, 0.07);
}
.otp.disabled {
  opacity: 0.55;
}
.otp-cell:disabled {
  cursor: not-allowed;
}
</style>
