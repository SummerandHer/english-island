<template>
  <div>
    <h1 class="mb-4 text-xl font-bold">双语视频</h1>
    <div class="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
      <NuxtLink
        v-for="v in videos"
        :key="v.id"
        :to="`/video/${v.id}`"
        class="island-card overflow-hidden transition hover:shadow-md"
      >
        <div class="flex aspect-video items-center justify-center bg-gray-100 text-4xl">🎬</div>
        <div class="p-4">
          <h3 class="line-clamp-2 font-medium">{{ v.title }}</h3>
          <p class="mt-1 text-xs text-gray-400">{{ formatDuration(v.durationSec) }}</p>
        </div>
      </NuxtLink>
    </div>
  </div>
</template>

<script setup lang="ts">
interface VideoSummary {
  id: number
  title: string
  durationSec?: number
}

const { request } = useApi()
const videos = ref<VideoSummary[]>([])

onMounted(async () => {
  videos.value = await request<VideoSummary[]>('/api/v1/videos')
})

function formatDuration(sec?: number) {
  if (!sec) return ''
  const m = Math.floor(sec / 60)
  const s = sec % 60
  return `${m}:${String(s).padStart(2, '0')}`
}
</script>
