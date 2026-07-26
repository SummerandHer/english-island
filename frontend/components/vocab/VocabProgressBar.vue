<template>
  <div v-if="stats" class="mb-4 rounded-lg border border-green-100 bg-green-50/40 px-4 py-3">
    <div class="mb-2 flex items-center justify-between text-sm">
      <span class="text-gray-600">今日复习</span>
      <span class="font-medium text-[var(--island-primary)]">
        {{ stats.todayDone }} / {{ stats.dailyLimit }}
      </span>
    </div>
    <NProgress
      type="line"
      :percentage="todayPercent"
      :show-indicator="false"
      color="#18a058"
      rail-color="#e8f7ef"
    />
        <p class="mt-2 text-xs text-gray-500">
      已掌握 {{ stats.masteredCount }} 词 · 生词本 {{ stats.notebookCount }} 词
      <span v-if="stats.streakDays != null"> · 连续 {{ stats.streakDays }} 天</span>
      <span v-if="stats.todayRemaining > 0"> · 待复习 {{ stats.todayRemaining }}</span>
    </p>
  </div>
</template>

<script setup lang="ts">
import type { VocabStats } from '~/types/api'

const props = defineProps<{
  stats: VocabStats | null
}>()

const todayPercent = computed(() => {
  if (!props.stats || props.stats.dailyLimit <= 0) return 0
  return Math.min(100, Math.round((props.stats.todayDone / props.stats.dailyLimit) * 100))
})
</script>
