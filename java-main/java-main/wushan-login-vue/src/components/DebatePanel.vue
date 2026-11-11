<script setup>
/**
 * 辩场：同一辩题下正反两方各自陈词，票数即点赞数。
 * 嵌在首页「论道」区下方，与普通论道语录并存（辩论语录按 topic 分组）。
 */
import { ref, onMounted } from 'vue'
import { debateApi } from '../api/debate'
import { findElement } from '../constants/wuxing'

const debates = ref([])
const showNew = ref(false)
const newTopic = ref('')
const newQuote = ref('')
const openTopic = ref('')
const replySide = ref(1)
const replyQuote = ref('')
const busy = ref(false)
const msg = ref('')

async function load() {
  try {
    debates.value = await debateApi.list()
  } catch (e) { /* 辩场拉取失败不阻塞论道区 */ }
}

function flash(m) {
  msg.value = m
  setTimeout(() => { msg.value = '' }, 2200)
}

async function publish() {
  if (!newTopic.value.trim()) return flash('请先拟一个辩题')
  if (!newQuote.value.trim()) return flash('正方总要立个论')
  busy.value = true
  try {
    await debateApi.publish(newTopic.value.trim(), newQuote.value.trim())
    flash('辩题已开，等待反方应战')
    newTopic.value = ''
    newQuote.value = ''
    showNew.value = false
    await load()
  } catch (e) {
    flash(e.message || '开辩失败')
  } finally {
    busy.value = false
  }
}

/** 点赞即投票：一人对一条陈词只能赞一次，正反票数多者胜 */
async function doLike(say) {
  if (say.liked) return flash('此言你已赞过')
  try {
    const r = await debateApi.like(say.id)
    flash('已投一票 · 当前' + (r.winnerLabel || ''))
    await load()
  } catch (e) {
    flash(e.message || '点赞失败')
  }
}

function toggle(topic) {
  if (openTopic.value === topic) {
    openTopic.value = ''
    return
  }
  openTopic.value = topic
  replySide.value = 1
  replyQuote.value = ''
}

async function reply(d) {
  if (!replyQuote.value.trim()) return flash('陈词不可为空')
  busy.value = true
  try {
    await debateApi.reply(d.topic, replySide.value, replyQuote.value.trim())
    flash('已记入辩场')
    replyQuote.value = ''
    openTopic.value = ''
    await load()
  } catch (e) {
    flash(e.message || '陈词失败')
  } finally {
    busy.value = false
  }
}

/** 正方票数占比（用于对比条） */
function ratio(d) {
  const total = (d.leftZan || 0) + (d.rightZan || 0)
  if (!total) return 50
  return Math.round(((d.leftZan || 0) / total) * 100)
}

onMounted(load)
</script>

<template>
  <div class="debate">
    <div class="db-head">
      <span class="db-title">辩 场 · 正 反 两 方</span>
      <span class="db-sub">同 题 各 陈 其 词 · 赞 即 为 票 · 谁 赞 多 谁 赢</span>
      <button class="db-new" @click="showNew = !showNew">{{ showNew ? '收 起' : '开 一 辩 题' }}</button>
    </div>

    <div v-if="showNew" class="db-form">
      <input v-model="newTopic" class="inp" placeholder="辩题，如：刚柔孰先？" maxlength="30" />
      <textarea v-model="newQuote" class="inp ta" placeholder="正方立论…" maxlength="200" rows="2"></textarea>
      <button class="db-btn" :disabled="busy" @click="publish">开 辩</button>
    </div>

    <div v-for="d in debates" :key="d.topic" class="db-card">
      <div class="db-topic">
        {{ d.topic }}
        <span class="db-winner" :class="d.winner">{{ d.winnerLabel }}</span>
      </div>
      <div class="db-vote">
        <span class="v-l" :style="{ width: ratio(d) + '%' }"></span>
        <span class="v-num">正 {{ d.leftZan }} : {{ d.rightZan }} 反</span>
      </div>
      <div class="db-sides">
        <div class="db-side left">
          <div class="side-head"><span class="side-tag">正 方</span><small>{{ d.leftZan }} 票</small></div>
          <div v-for="say in (d.left || []).slice(0, 3)" :key="say.id" class="say">
            <span class="say-name" :style="{ color: findElement(say.element).color }">{{ say.name }}</span>
            <p class="say-text">{{ say.quote }}</p>
            <button class="like-btn" :class="{ liked: say.liked }" @click="doLike(say)">
              {{ say.liked ? '已 赞' : '赞' }} · {{ say.zan }}
            </button>
          </div>
          <div v-if="!d.left || !d.left.length" class="say say-empty">尚 无 人 立 论</div>
        </div>

        <div class="db-mid">对<br />阵</div>

        <div class="db-side right">
          <div class="side-head"><span class="side-tag">反 方</span><small>{{ d.rightZan }} 票</small></div>
          <div v-for="say in (d.right || []).slice(0, 3)" :key="say.id" class="say">
            <span class="say-name" :style="{ color: findElement(say.element).color }">{{ say.name }}</span>
            <p class="say-text">{{ say.quote }}</p>
            <button class="like-btn" :class="{ liked: say.liked }" @click="doLike(say)">
              {{ say.liked ? '已 赞' : '赞' }} · {{ say.zan }}
            </button>
          </div>
          <div v-if="!d.right || !d.right.length" class="say say-empty">尚 无 人 应 战</div>
        </div>
      </div>

      <div class="db-reply">
        <button class="db-link" @click="toggle(d.topic)">
          {{ openTopic === d.topic ? '收 起' : '应 战 / 陈 词' }}
        </button>
        <div v-if="openTopic === d.topic" class="db-reply-form">
          <select v-model="replySide" class="sel">
            <option :value="0">立 正 方</option>
            <option :value="1">立 反 方</option>
          </select>
          <input v-model="replyQuote" class="inp" placeholder="陈词…" maxlength="200" />
          <button class="db-btn" :disabled="busy" @click="reply(d)">陈 词</button>
        </div>
      </div>
    </div>

    <div v-if="!debates.length" class="db-empty">尚 无 辩 题 · 开 一 场 罢</div>
    <div v-if="msg" class="db-msg">{{ msg }}</div>
  </div>
</template>

<style scoped>
.debate { margin-top: 18px; padding-top: 16px; border-top: 1px solid var(--line); }
.db-head { display: flex; align-items: baseline; gap: 12px; margin-bottom: 14px; flex-wrap: wrap; }
.db-title { font-size: 15px; letter-spacing: 5px; color: #e8c372; }
.db-sub { font-size: 11px; letter-spacing: 1px; color: var(--muted); }
.db-new { margin-left: auto; font: inherit; font-size: 12px; letter-spacing: 2px; padding: 6px 14px; color: var(--paper); background: rgba(236, 228, 210, 0.08); border: 1px solid var(--line); cursor: pointer; }
.db-new:hover { background: rgba(236, 228, 210, 0.18); }

.db-form { display: flex; gap: 10px; margin-bottom: 16px; flex-wrap: wrap; }
.inp {
  font: inherit; font-size: 14px; color: var(--paper);
  background: rgba(236, 228, 210, 0.06); border: 1px solid var(--line); padding: 9px 12px; min-width: 180px;
}
.ta { flex: 1; min-width: 260px; resize: vertical; }
.sel { font: inherit; font-size: 13px; color: var(--paper); background: rgba(236, 228, 210, 0.06); border: 1px solid var(--line); padding: 9px 10px; }
.sel option { background: #16201d; }
.db-btn { font: inherit; font-size: 13px; letter-spacing: 3px; padding: 9px 18px; color: #f6ece0; background: linear-gradient(180deg, rgba(163, 49, 31, 0.92), rgba(120, 33, 21, 0.92)); border: 1px solid #c8543c; cursor: pointer; }
.db-btn:disabled { opacity: 0.5; cursor: not-allowed; }

.db-card { padding: 16px 18px; margin-bottom: 14px; border: 1px solid var(--line); background: rgba(12, 17, 16, 0.5); }
.db-topic { display: flex; align-items: center; gap: 12px; font-size: 19px; letter-spacing: 4px; margin-bottom: 10px; }
.db-winner { margin-left: auto; font-size: 12px; letter-spacing: 2px; padding: 3px 10px; border: 1px solid var(--line); color: var(--muted); }
.db-winner.left { color: #f0c9b8; border-color: #c8543c; background: rgba(163, 49, 31, 0.18); }
.db-winner.right { color: #cfe6fa; border-color: #3a8fd0; background: rgba(58, 143, 208, 0.16); }
.db-winner.draw { color: #e8c372; border-color: rgba(232, 195, 114, 0.5); }
.side-head { display: flex; align-items: baseline; gap: 8px; }
.side-head small { font-size: 11px; color: var(--muted); }
.like-btn {
  margin-top: 8px; font: inherit; font-size: 11px; letter-spacing: 1px; padding: 3px 10px;
  color: #e8c372; background: rgba(232, 195, 114, 0.08); border: 1px solid rgba(232, 195, 114, 0.35); cursor: pointer;
}
.like-btn:hover { background: rgba(232, 195, 114, 0.2); }
.like-btn.liked { color: var(--muted); border-color: var(--line); background: none; cursor: default; }
.say { margin-bottom: 8px; padding-bottom: 8px; border-bottom: 1px dashed rgba(236, 228, 210, 0.1); }
.db-vote { position: relative; height: 8px; margin-bottom: 14px; background: rgba(60, 120, 160, 0.25); }
.db-vote .v-l { display: block; height: 100%; background: linear-gradient(90deg, #8a2f1e, #e8412b); }
.v-num { position: absolute; right: 0; top: -18px; font-size: 11px; color: var(--muted); }
.db-sides { display: grid; grid-template-columns: 1fr 46px 1fr; gap: 10px; align-items: stretch; }
.db-side { padding: 10px 12px; border: 1px solid var(--line); background: rgba(236, 228, 210, 0.03); }
.db-side.left { border-left: 2px solid #e8412b; }
.db-side.right { border-left: 2px solid #3a8fd0; }
.side-head { margin-bottom: 8px; }
.side-tag { font-size: 12px; letter-spacing: 3px; color: #e8c372; }
.db-side.right .side-tag { color: #85b7eb; }
.say-name { font-size: 13px; letter-spacing: 2px; }
.say-text { margin: 6px 0 0; font-size: 13px; line-height: 1.8; letter-spacing: 1px; color: rgba(236, 228, 210, 0.82); }
.say-empty { font-size: 12px; letter-spacing: 2px; color: var(--muted); }
.db-mid { display: grid; place-items: center; font-size: 14px; letter-spacing: 4px; color: var(--muted); }
.db-reply { margin-top: 12px; }
.db-link { font: inherit; font-size: 12px; letter-spacing: 2px; color: #e8c372; background: none; border: none; cursor: pointer; padding: 0; }
.db-reply-form { display: flex; gap: 10px; margin-top: 10px; flex-wrap: wrap; }
.db-empty { font-size: 13px; letter-spacing: 2px; color: var(--muted); text-align: center; padding: 20px 0; }
.db-msg { margin-top: 10px; font-size: 12px; letter-spacing: 2px; color: #e8c372; text-align: center; }
</style>
