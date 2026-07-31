<template>
  <div class="toolbar">
    <button type="button" class="tb-btn" @click="cycleRate">
      <b>{{ rateLabel }}</b>
      <span>倍速</span>
    </button>

    <button type="button" class="tb-btn" :class="{ on: videoHidden }" @click="emit('update:videoHidden', !videoHidden)">
      <b>{{ videoHidden ? '显示' : '隐藏' }}</b>
      <span>隐藏视频</span>
    </button>

    <button type="button" class="tb-btn" @click="emit('fullscreen')">
      <b>⛶</b>
      <span>全屏</span>
    </button>

    <button type="button" class="tb-btn core" @click="emit('prev')">
      <i class="dot">‹</i>
      <span>上一句</span>
    </button>

    <button type="button" class="tb-btn core" @click="emit('toggle-play')">
      <i class="dot lg">{{ playing ? 'Ⅱ' : '▶' }}</i>
      <span>{{ playing ? '暂停' : '播放' }}</span>
    </button>

    <button type="button" class="tb-btn core" @click="emit('next')">
      <i class="dot">›</i>
      <span>下一句</span>
    </button>

    <button type="button" class="tb-btn" :class="{ on: sentenceLoop }" @click="emit('update:sentenceLoop', !sentenceLoop)">
      <b>1↻</b>
      <span>单句循环</span>
    </button>

    <button type="button" class="tb-btn" :class="{ on: singlePause }" @click="emit('update:singlePause', !singlePause)">
      <b>▷Ⅱ</b>
      <span>单句暂停</span>
    </button>
  </div>
</template>

<script setup lang="ts">
const props = defineProps<{
  playbackRate: number
  playing?: boolean
  videoHidden?: boolean
  sentenceLoop?: boolean
  singlePause?: boolean
}>()

const emit = defineEmits<{
  prev: []
  next: []
  'toggle-play': []
  fullscreen: []
  rate: [number]
  'update:videoHidden': [boolean]
  'update:sentenceLoop': [boolean]
  'update:singlePause': [boolean]
}>()

const rates = [0.75, 1, 1.25, 1.5, 2]
const rateLabel = computed(() => `${props.playbackRate}x`)

function cycleRate() {
  const idx = rates.indexOf(props.playbackRate)
  emit('rate', rates[(idx + 1) % rates.length])
}
</script>

<style scoped>
.toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: center;
  gap: 0.35rem 0.55rem;
  min-height: 4.25rem;
  padding: 0.55rem 0.75rem;
  border: 1px solid rgba(47, 67, 51, 0.12);
  border-radius: 14px;
  background: #ffffff;
  box-shadow: 0 1px 0 rgba(47, 67, 51, 0.04);
}

.tb-btn {
  display: inline-flex;
  min-width: 3.6rem;
  flex-direction: column;
  align-items: center;
  gap: 0.2rem;
  border: none;
  background: transparent;
  padding: 0.2rem 0.35rem;
  color: #2f4333;
  cursor: pointer;
}

.tb-btn b {
  font-family: var(--font-display);
  font-size: 0.9rem;
  font-weight: 700;
  line-height: 1.4;
}

.tb-btn span {
  font-size: 0.7rem;
  color: #6b7a6f;
}

.tb-btn.on b,
.tb-btn.on span {
  color: #1f2a24;
}

.tb-btn.core .dot {
  display: inline-flex;
  width: 1.85rem;
  height: 1.85rem;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  background: #222;
  color: #fff;
  font-style: normal;
  font-size: 0.95rem;
  line-height: 1;
}

.tb-btn.core .dot.lg {
  width: 2.05rem;
  height: 2.05rem;
}

.tb-btn:hover span {
  color: #2f4333;
}
</style>
