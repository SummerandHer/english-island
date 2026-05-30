<template>
  <div class="island-card max-h-[520px] overflow-y-auto p-2">
    <button
      v-for="(s, i) in sentences"
      :key="s.id"
      type="button"
      class="sentence-row w-full rounded-lg p-3 text-left transition"
      :class="i === currentIndex ? 'bg-gray-100' : 'hover:bg-gray-50'"
      @click="$emit('select', i)"
    >
      <div class="mb-1 text-xs text-gray-400">
        {{ s.seq }} · {{ formatTime(s.startMs) }} - {{ formatTime(s.endMs) }}
      </div>
      <p class="font-medium text-gray-800">{{ s.textEn }}</p>
      <p class="mt-1 text-sm text-gray-500">{{ s.textZh }}</p>
    </button>
  </div>
</template>

<script setup lang="ts">
import type { VideoSentence } from '~/types/api'

defineProps<{ sentences: VideoSentence[]; currentIndex: number }>()
defineEmits<{ select: [index: number] }>()

function formatTime(ms: number) {
  const sec = Math.floor(ms / 1000)
  const m = Math.floor(sec / 60)
  const s = sec % 60
  return `${m}:${String(s).padStart(2, '0')}`
}
</script>
