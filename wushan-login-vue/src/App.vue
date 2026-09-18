<script setup>
// 根组件：负责整体布局，并把「五行主题色」状态提升到此处，
// 同时分发给背景粒子（emberColor）与登录面板（accent）。
import { ref } from 'vue'
import EmberBackground from './components/EmberBackground.vue'
import FogMountains from './components/FogMountains.vue'
import LoginPanel from './components/LoginPanel.vue'

const accent = ref('#e8412b')
const accent2 = ref('#ff7a18')
const emberColor = ref('#e8412b')
const bg = ref(null)

// 五行被点击：更新主题色与火星颜色（火元素使用默认红，其余使用所选色）
function onSelect({ color, huo }) {
  accent.value = color
  emberColor.value = huo ? '#e8412b' : color
}

// 登录成功瞬间：在面板中心位置引爆一团火星
function onBurst(p) {
  if (bg.value) bg.value.burst(p.x, p.y, p.n, p.spread)
}
</script>

<template>
  <div class="app-root" :style="{ '--accent': accent, '--accent2': accent2 }">
    <EmberBackground ref="bg" :ember-color="emberColor" />
    <FogMountains />
    <div class="fogband"></div>

    <div class="wrap">
      <LoginPanel @select="onSelect" @burst="onBurst" />
    </div>

    <div class="hint">VUE 重构 · 点五行可换主题色 · 任意非空名号暗语即可入山</div>
  </div>
</template>

<style scoped>
.app-root {
  position: relative;
  min-height: 100%;
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
</style>
