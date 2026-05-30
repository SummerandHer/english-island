<template>
  <div class="island-card p-6 text-center">
    <div class="mb-3 flex items-center justify-between text-sm text-gray-400">
      <span>{{ index + 1 }} / {{ total }}</span>
    </div>
    <p v-if="sentence" class="text-xl font-semibold leading-relaxed text-gray-900">{{ sentence.textEn }}</p>
    <p v-if="sentence" class="mt-3 text-base text-gray-600">{{ sentence.textZh }}</p>
  </div>
</template>

<script setup lang="ts">
import type { VideoSentence } from '~/types/api'

defineProps<{
  sentence?: VideoSentence
  index: number
  total: number
}>()
</script>
