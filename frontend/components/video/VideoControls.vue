<template>
  <div class="vctl" role="toolbar" aria-label="视频学习控制">
    <!-- 倍速：一并保留 -->
    <button type="button" class="vctl__item" title="倍速" @click="cycleRate">
      <span class="vctl__glyph vctl__glyph--text">{{ rateLabel }}</span>
      <span class="vctl__label">倍速</span>
    </button>

    <!-- 红框：隐藏视频 -->
    <button
      type="button"
      class="vctl__item"
      :class="{ 'vctl__item--on': videoHidden }"
      title="隐藏视频"
      @click="videoHidden = !videoHidden"
    >
      <span class="vctl__glyph" aria-hidden="true">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7">
          <path d="M3 3l18 18" />
          <path d="M10.6 6.3A10.4 10.4 0 0 1 12 5c6.5 0 10 7 10 7a17.4 17.4 0 0 1-3.2 3.9" />
          <path d="M6.1 6.1C3.7 7.8 2 12 2 12s3.5 7 10 7a10.3 10.3 0 0 0 4.2-.9" />
          <path d="M9.9 9.9a3 3 0 0 0 4.2 4.2" />
        </svg>
      </span>
      <span class="vctl__label">隐藏视频</span>
    </button>

    <!-- 红框：全屏 -->
    <button type="button" class="vctl__item" title="全屏" @click="emit('fullscreen')">
      <span class="vctl__glyph" aria-hidden="true">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7">
          <path d="M8 3H3v5M16 3h5v5M8 21H3v-5M16 21h5v-5" stroke-linecap="round" stroke-linejoin="round" />
        </svg>
      </span>
      <span class="vctl__label">全屏</span>
    </button>

    <!-- 红框：上一句 / 暂停 / 下一句 -->
    <button type="button" class="vctl__item vctl__item--core" title="上一句" @click="emit('prev')">
      <span class="vctl__core" aria-hidden="true">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="currentColor">
          <path d="M15.5 5.5 8 12l7.5 6.5V5.5Z" />
        </svg>
      </span>
      <span class="vctl__label">上一句</span>
    </button>

    <button
      type="button"
      class="vctl__item vctl__item--core"
      :title="playing ? '暂停' : '播放'"
      @click="emit('toggle-play')"
    >
      <span class="vctl__core vctl__core--lg" aria-hidden="true">
        <svg v-if="playing" width="14" height="14" viewBox="0 0 24 24" fill="currentColor">
          <rect x="6" y="5" width="4" height="14" rx="1" />
          <rect x="14" y="5" width="4" height="14" rx="1" />
        </svg>
        <svg v-else width="14" height="14" viewBox="0 0 24 24" fill="currentColor">
          <path d="M8 5.5v13l11-6.5L8 5.5Z" />
        </svg>
      </span>
      <span class="vctl__label">{{ playing ? '暂停' : '播放' }}</span>
    </button>

    <button type="button" class="vctl__item vctl__item--core" title="下一句" @click="emit('next')">
      <span class="vctl__core" aria-hidden="true">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="currentColor">
          <path d="M8.5 5.5 16 12l-7.5 6.5V5.5Z" />
        </svg>
      </span>
      <span class="vctl__label">下一句</span>
    </button>

    <!-- 红框：单句循环 -->
    <button
      type="button"
      class="vctl__item"
      :class="{ 'vctl__item--on': sentenceLoop }"
      title="单句循环"
      @click="sentenceLoop = !sentenceLoop"
    >
      <span class="vctl__glyph" aria-hidden="true">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7">
          <path d="M17 1l4 4-4 4" stroke-linecap="round" stroke-linejoin="round" />
          <path d="M3 11V9a4 4 0 0 1 4-4h14" stroke-linecap="round" />
          <path d="M7 23l-4-4 4-4" stroke-linecap="round" stroke-linejoin="round" />
          <path d="M21 13v2a4 4 0 0 1-4 4H3" stroke-linecap="round" />
          <text x="12" y="14.5" text-anchor="middle" font-size="7" fill="currentColor" stroke="none" font-weight="700">1</text>
        </svg>
      </span>
      <span class="vctl__label">单句循环</span>
    </button>

    <!-- 红框：单句暂停 -->
    <button
      type="button"
      class="vctl__item"
      :class="{ 'vctl__item--on': singlePause }"
      title="单句暂停"
      @click="singlePause = !singlePause"
    >
      <span class="vctl__glyph" aria-hidden="true">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7">
          <path d="M8 5.5v13l9-6.5-9-6.5Z" fill="currentColor" stroke="none" />
          <path d="M19 6v12" stroke-linecap="round" />
        </svg>
      </span>
      <span class="vctl__label">单句暂停</span>
    </button>
  </div>
</template>

<script setup lang="ts">
const props = defineProps<{
  playbackRate: number
  playing?: boolean
}>()

const emit = defineEmits<{
  prev: []
  next: []
  'toggle-play': []
  fullscreen: []
  rate: [number]
}>()

const videoHidden = defineModel<boolean>('videoHidden', { default: false })
const singlePause = defineModel<boolean>('singlePause', { default: false })
const sentenceLoop = defineModel<boolean>('sentenceLoop', { default: false })
const autoFollow = defineModel<boolean>('autoFollow', { default: true })
void autoFollow

const rates = [0.75, 1, 1.25, 1.5, 2]
const rateLabel = computed(() => `${props.playbackRate}x`)

function cycleRate() {
  const idx = rates.indexOf(props.playbackRate)
  emit('rate', rates[(idx + 1) % rates.length])
}
</script>

<style scoped>
.vctl {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
  justify-content: center;
  gap: 0.15rem 0.35rem;
  padding: 0.6rem 0.55rem 0.5rem;
  border: 1px solid rgba(47, 67, 51, 0.08);
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.95);
}

.vctl__item {
  display: flex;
  min-width: 3.6rem;
  flex-direction: column;
  align-items: center;
  gap: 0.25rem;
  border: none;
  background: transparent;
  padding: 0.28rem 0.2rem;
  color: #4a574e;
  cursor: pointer;
  transition: color 0.18s ease;
}

.vctl__item:hover {
  color: #1f2a24;
}

.vctl__item--on {
  color: #2f4333;
}

.vctl__glyph {
  display: inline-flex;
  height: 1.6rem;
  width: 1.6rem;
  align-items: center;
  justify-content: center;
}

.vctl__glyph--text {
  font-family: var(--font-display);
  font-size: 0.84rem;
  font-weight: 700;
  letter-spacing: -0.02em;
}

.vctl__label {
  font-size: 0.64rem;
  line-height: 1.15;
  color: #7d8a80;
  white-space: nowrap;
}

.vctl__item--on .vctl__label,
.vctl__item:hover .vctl__label {
  color: inherit;
}

.vctl__core {
  display: inline-flex;
  height: 1.75rem;
  width: 1.75rem;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  background: #222;
  color: #fff;
}

.vctl__core--lg {
  height: 1.95rem;
  width: 1.95rem;
}

.vctl__item--core .vctl__label {
  color: #6b7a6f;
}

@media (min-width: 768px) {
  .vctl {
    gap: 0.2rem 0.5rem;
    padding: 0.7rem 1rem 0.55rem;
  }

  .vctl__item {
    min-width: 3.9rem;
  }

  .vctl__label {
    font-size: 0.7rem;
  }
}
</style>
