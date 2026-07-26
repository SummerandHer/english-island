<template>
  <section v-if="sentences.length" class="rounded-lg border border-blue-100 bg-blue-50/40 p-4">
    <h3 class="mb-3 text-sm font-medium text-blue-900">长难句点选讲解</h3>
    <div class="space-y-2">
      <div
        v-for="(s, idx) in sentences"
        :key="idx"
        class="rounded-lg border border-white bg-white/80 p-3"
      >
        <button
          type="button"
          class="w-full text-left text-sm leading-relaxed text-gray-800 hover:text-[var(--island-primary)]"
          @click="toggle(idx)"
        >
          {{ idx + 1 }}. {{ truncate(s.en) }}
        </button>
        <div v-if="expanded === idx" class="mt-3 space-y-2 border-t border-gray-100 pt-3 text-sm">
          <p class="text-gray-700">{{ s.en }}</p>
          <p class="text-gray-600">{{ s.zh }}</p>
          <p v-if="s.hint" class="rounded bg-amber-50 p-2 text-xs text-amber-900">
            <strong>结构提示：</strong>{{ s.hint }}
          </p>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import type { ReadingLongSentence } from '~/types/api'

defineProps<{
  sentences: ReadingLongSentence[]
}>()

const expanded = ref<number | null>(null)

function toggle(idx: number) {
  expanded.value = expanded.value === idx ? null : idx
}

function truncate(text: string) {
  return text.length > 88 ? `${text.slice(0, 88)}…` : text
}
</script>
