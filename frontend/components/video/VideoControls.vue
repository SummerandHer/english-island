<template>
  <div class="video-toolbar island-card flex flex-wrap items-center gap-2 px-3 py-2 md:px-4">
    <button
      type="button"
      class="toolbar-pill"
      :class="{ 'toolbar-pill--active': autoFollow }"
      @click="autoFollow = !autoFollow"
    >
      自动跟随 {{ autoFollow ? 'ON' : 'OFF' }}
    </button>

    <button type="button" class="toolbar-btn" @click="emit('prev')">
      <span class="toolbar-btn__icon">←</span>
      上一句
    </button>

    <button type="button" class="toolbar-btn" @click="emit('next')">
      <span class="toolbar-btn__icon">→</span>
      下一句
    </button>

    <button
      type="button"
      class="toolbar-pill"
      :class="{ 'toolbar-pill--active': singlePause }"
      @click="singlePause = !singlePause"
    >
      单句暂停 {{ singlePause ? 'ON' : 'OFF' }}
    </button>

    <div class="ml-auto flex items-center gap-2">
      <button type="button" class="toolbar-btn toolbar-btn--icon" @click="emit('toggle-play')">
        <span class="toolbar-btn__icon">{{ playing ? '⏸' : '▶' }}</span>
      </button>
      <button type="button" class="toolbar-rate" @click="cycleRate">
        倍速 {{ playbackRate }}x
      </button>
      <button type="button" class="toolbar-btn toolbar-btn--icon" title="全屏" @click="emit('fullscreen')">
        <span class="toolbar-btn__icon">⛶</span>
      </button>
    </div>
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

const autoFollow = defineModel<boolean>('autoFollow', { default: true })
const singlePause = defineModel<boolean>('singlePause', { default: false })

const rates = [0.75, 1, 1.25, 1.5, 2]

function cycleRate() {
  const idx = rates.indexOf(props.playbackRate)
  emit('rate', rates[(idx + 1) % rates.length])
}
</script>

<style scoped>
.toolbar-pill {
  @apply rounded-full border border-gray-200 bg-white px-3 py-1.5 text-xs text-gray-600 transition;
}

.toolbar-pill:hover {
  border-color: #18a0584d;
}

.toolbar-pill--active {
  border-color: #18a05866;
  background-color: var(--island-primary-light);
  color: var(--island-primary);
}

.toolbar-btn {
  @apply inline-flex items-center gap-1 rounded-lg px-2.5 py-1.5 text-xs text-gray-600 transition;
}

.toolbar-btn:hover {
  @apply bg-gray-50;
  color: var(--island-primary);
}

.toolbar-btn--icon {
  @apply px-2;
}

.toolbar-btn__icon {
  @apply text-sm;
}

.toolbar-rate {
  @apply rounded-lg border border-gray-200 px-3 py-1.5 text-xs text-gray-600 transition;
}

.toolbar-rate:hover {
  border-color: #18a0584d;
  color: var(--island-primary);
}
</style>
