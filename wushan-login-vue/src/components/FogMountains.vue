<script setup>
// 雾气山峦 —— 还原原 login-wushan.html 中的两层 SVG 山形
</script>

<template>
  <svg class="fog" viewBox="0 0 1440 300" preserveAspectRatio="none">
    <path
      fill="#0c1412"
      opacity="0.95"
      d="M0,230 C200,150 340,210 480,170 C640,125 780,215 940,180 C1110,142 1260,225 1440,190 L1440,300 L0,300 Z"
    />
    <path
      fill="#08100e"
      opacity="0.98"
      d="M0,270 C220,220 380,265 540,240 C720,210 880,285 1060,250 C1230,218 1340,275 1440,255 L1440,300 L0,300 Z"
    />
  </svg>
</template>

<style scoped>
.fog {
  position: fixed;
  left: 0;
  bottom: 0;
  width: 100%;
  z-index: 1;
  pointer-events: none;
}
</style>
