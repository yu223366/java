<script setup>
// 本命元素选择器 —— 点选换主题色（登录页 / 注册页共用）
// 含正五行（金木水火土）与衍生元素（风雷电 · 太极 · 阴阳），共十种
import { ELEMENTS } from '../constants/wuxing'

const props = defineProps({
  modelValue: { type: String, default: 'huo' },
  size: { type: String, default: 'md' } // md | lg
})
const emit = defineEmits(['update:modelValue', 'select'])

function pick(el) {
  emit('update:modelValue', el.key)
  emit('select', { color: el.color, huo: el.huo, key: el.key })
}
</script>

<template>
  <div class="wuxing" :class="size">
    <div
      v-for="el in ELEMENTS"
      :key="el.key"
      class="wx"
      :class="[el.key, { 'is-on': modelValue === el.key }]"
      :title="el.trait"
      @click="pick(el)"
    >
      {{ el.label }}<small>{{ el.en }}</small>
    </div>
  </div>
</template>

<style scoped>
.wuxing {
  display: flex;
  gap: 8px;
  justify-content: center;
  flex-wrap: wrap; /* 十种元素自动换行（每行五个） */
}
.wx {
  padding: 6px 0;
  width: 50px;
  text-align: center;
  border-radius: 8px;
  cursor: pointer;
  font-size: 17px;
  letter-spacing: 2px;
  border: 1px solid rgba(236, 228, 210, 0.18);
  background: rgba(236, 228, 210, 0.04);
  transition: 0.2s;
  color: var(--paper);
  user-select: none;
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
.wx.jin { color: #e8c372; }
.wx.mu { color: #4caf6e; }
.wx.shui { color: #3a8fd0; }
.wx.huo { color: #e8412b; }
.wx.tu { color: #c8923a; }
/* 衍生元素 */
.wx.feng { color: #7fd8cf; }
.wx.lei { color: #9d7bf0; }
.wx.dian { color: #f2e34d; }
.wx.taiji { color: #d8d2c2; }
.wx.yinyang { color: #98a4b0; }
.wuxing.lg .wx {
  width: 56px;
  font-size: 19px;
  padding: 8px 0;
}
</style>
