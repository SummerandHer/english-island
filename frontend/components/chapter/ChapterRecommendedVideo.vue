<template>
  <section v-if="video" class="island-card p-6">
    <h2 class="mb-1 text-lg font-semibold">推荐双语视频</h2>
    <p class="mb-4 text-sm text-gray-500">学完本章技巧后，用真实语料巩固听力与表达。</p>
    <VideoCard :video="video" />
  </section>
</template>

<script setup lang="ts">
import type { VideoSummary } from '~/types/api'

defineProps<{
  video: VideoSummary | null | undefined
}>()
</script>
