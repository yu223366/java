<script setup>
/**
 * 物品详情弹层：宝库 / 市集 / 宗门宝库 / 寄售共用。
 * 只负责展示，数据由父组件传入（字段见 props）。
 */
import { computed } from 'vue'
import { findElement } from '../constants/wuxing'

const props = defineProps({
  item: { type: Object, default: null }
})
const emit = defineEmits(['close'])

const it = computed(() => props.item || null)
const el = computed(() => findElement(it.value?.element))
const full = computed(() => it.value?.detail || it.value?.desc || '此物尚无详录。')
</script>

<template>
  <Transition name="fade">
    <div v-if="it" class="mask" @click.self="emit('close')">
      <div class="box">
        <button class="x" @click="emit('close')">×</button>
        <div class="seal">物<br />录</div>
        <div class="head">
          <span class="el" :style="{ color: el.color }">{{ el.label }}行</span>
          <h2>{{ it.title }}</h2>
          <div class="tags">
            <span v-if="it.gradeLabel" class="tag grade">{{ it.gradeLabel }}</span>
            <span v-if="it.categoryLabel" class="tag">{{ it.categoryLabel }}</span>
            <span v-if="it.shopLabel" class="tag">{{ it.shopLabel }}</span>
            <span v-if="it.gradePrice" class="tag price">基准价 {{ it.gradePrice }} 灵石</span>
          </div>
        </div>

        <div class="body">
          <section>
            <h3>详 细 介 绍</h3>
            <p class="detail">{{ full }}</p>
          </section>
          <section v-if="it.acquire || it.itemAcquire">
            <h3>怎 么 获 取</h3>
            <p class="line">{{ it.acquire || it.itemAcquire }}</p>
            <p v-if="it.qualityRule" class="sub">{{ it.qualityRule }}</p>
          </section>
          <section v-if="it.effect">
            <h3>用 途 与 功 效</h3>
            <p class="line">{{ it.effect }}</p>
          </section>
          <section v-if="it.shopTip">
            <h3>炼 制 提 示</h3>
            <p class="line">{{ it.shopTip }}</p>
          </section>
        </div>
      </div>
    </div>
  </Transition>
</template>

<style scoped>
.mask { position: fixed; inset: 0; z-index: 90; display: grid; place-items: center; background: rgba(6, 9, 8, 0.72); }
.box {
  position: relative; width: min(560px, 92vw); max-height: 84vh; overflow-y: auto;
  padding: 26px 30px 30px; color: var(--paper);
  background: linear-gradient(160deg, rgba(26, 30, 27, 0.98), rgba(14, 18, 17, 0.98));
  border: 1px solid var(--line); box-shadow: 0 20px 60px rgba(0, 0, 0, 0.6);
  font-family: "Kaiti SC", "STKaiti", "KaiTi", "楷体", serif;
}
.x { position: absolute; right: 14px; top: 10px; font-size: 26px; line-height: 1; color: var(--muted); background: none; border: none; cursor: pointer; }
.x:hover { color: var(--accent); }
.seal {
  position: absolute; right: 28px; top: 26px; width: 38px; height: 38px; display: grid; place-content: center;
  font-size: 13px; line-height: 1.05; text-align: center; color: #f6ece0; background: #a3311f;
  border: 1px solid #c8543c; opacity: 0.85;
}
.head { padding-right: 96px; border-bottom: 1px solid var(--line); padding-bottom: 14px; }
.el { font-size: 12px; letter-spacing: 3px; }
.head h2 { margin: 6px 0 10px; font-size: 27px; letter-spacing: 5px; font-weight: normal; }
.tags { display: flex; gap: 8px; flex-wrap: wrap; }
.tag { font-size: 11px; letter-spacing: 1px; padding: 3px 9px; border: 1px solid var(--line); color: var(--muted); }
.tag.grade { color: #e8c372; border-color: rgba(232, 195, 114, 0.5); }
.tag.price { color: #e8c372; }
.body section { margin-top: 20px; }
.body h3 { margin: 0 0 8px; font-size: 13px; letter-spacing: 5px; color: #e8c372; font-weight: normal; }
.detail, .line { margin: 0; font-size: 14px; line-height: 2; letter-spacing: 1px; color: rgba(236, 228, 210, 0.88); }
.sub { margin: 8px 0 0; font-size: 12px; color: var(--muted); letter-spacing: 1px; }
.fade-enter-active, .fade-leave-active { transition: opacity 0.18s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
</style>
