<template>
  <div class="flex flex-wrap justify-center gap-2 rounded-xl bg-white p-3 shadow-sm">
    <button
      v-for="ctrl in controls"
      :key="ctrl.key"
      type="button"
      class="ctrl-btn"
      @click="onClick(ctrl.key)"
    >
      <span class="ctrl-icon">{{ ctrl.icon }}</span>
      <span class="ctrl-label">{{ ctrl.label }}</span>
    </button>
  </div>
</template>

<script setup lang="ts">
const props = defineProps<{ playbackRate: number; favorited: boolean }>()
const emit = defineEmits(['prev', 'next', 'toggle-play', 'fullscreen', 'rate', 'favorite'])

const rates = [0.75, 1, 1.25, 1.5, 2]

const controls = computed(() => [
  { key: 'rate', icon: `${props.playbackRate}x`, label: '倍速' },
  { key: 'fs', icon: '⛶', label: '全屏' },
  { key: 'prev', icon: '←', label: '上一句' },
  { key: 'pause', icon: '⏸', label: '暂停' },
  { key: 'next', icon: '→', label: '下一句' },
  { key: 'fav', icon: props.favorited ? '❤' : '♡', label: '收藏' }
])

function onClick(key: string) {
  if (key === 'rate') {
    const idx = rates.indexOf(props.playbackRate)
    const next = rates[(idx + 1) % rates.length]
    emit('rate', next)
  } else if (key === 'fs') emit('fullscreen')
  else if (key === 'prev') emit('prev')
  else if (key === 'pause') emit('toggle-play')
  else if (key === 'next') emit('next')
  else if (key === 'fav') emit('favorite')
}
</script>

<style scoped>
.ctrl-btn {
  @apply flex min-w-[56px] flex-col items-center gap-1 rounded-lg px-2 py-2 text-gray-600 transition hover:bg-green-50 hover:text-[var(--island-primary)];
}
.ctrl-icon {
  @apply flex h-10 w-10 items-center justify-center rounded-full bg-gray-50 text-sm font-medium;
}
.ctrl-label {
  @apply text-xs;
}
</style>
