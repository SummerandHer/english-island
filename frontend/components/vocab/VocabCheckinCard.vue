<template>
  <div v-if="stats" class="mb-4 rounded-lg border border-amber-100 bg-amber-50/40 px-4 py-3">
    <div class="flex items-center justify-between">
      <div>
        <p class="text-sm font-medium text-gray-800">连续打卡 {{ stats.streakDays }} 天</p>
        <p class="text-xs text-gray-500">完成今日复习队列后自动打卡</p>
      </div>
    </div>
    <div class="mt-3 flex items-end justify-between gap-1">
      <div
        v-for="day in stats.week"
        :key="day.date"
        class="flex flex-1 flex-col items-center gap-1"
      >
        <div
          class="h-8 w-full max-w-[2rem] rounded-md transition"
          :class="day.checked ? 'bg-[var(--island-primary)]' : 'bg-gray-200'"
        />
        <span class="text-[10px] text-gray-500">{{ weekLabel(day.date) }}</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { VocabCheckinStats } from '~/types/api'

defineProps<{
  stats: VocabCheckinStats | null
}>()

function weekLabel(isoDate: string) {
  try {
    const d = new Date(isoDate)
    return `${d.getMonth() + 1}/${d.getDate()}`
  } catch {
    return isoDate
  }
}
</script>
