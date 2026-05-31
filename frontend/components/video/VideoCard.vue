<template>
  <NuxtLink
    :to="`/video/${video.id}`"
    class="video-card island-card group block overflow-hidden transition hover:-translate-y-0.5 hover:shadow-md"
    :class="featured ? 'video-card--featured' : ''"
  >
    <div class="video-card__thumb" :class="featured ? 'video-card__thumb--featured' : ''">
      <img
        v-if="video.coverUrl"
        :src="coverSrc"
        :alt="video.title"
        loading="lazy"
        class="h-full w-full object-cover"
      />
      <div v-else class="video-card__placeholder">
        <span class="video-card__play">▶</span>
      </div>
      <span v-if="video.durationSec" class="video-card__duration">
        {{ formatDuration(video.durationSec) }}
      </span>
      <span v-if="video.vip" class="video-card__vip">VIP</span>
    </div>

    <div class="video-card__body" :class="featured ? 'p-5' : 'p-3'">
      <div v-if="video.tags.length" class="video-card__tags">
        <span v-for="tag in video.tags" :key="tag" class="video-card__tag">{{ tag }}</span>
      </div>

      <h3 class="video-card__title" :class="featured ? 'text-lg' : 'text-sm'">
        {{ video.title }}
      </h3>

      <p
        v-if="video.description"
        class="video-card__desc"
        :class="featured ? 'line-clamp-3' : 'line-clamp-2'"
      >
        {{ video.description }}
      </p>

      <div class="video-card__stats">
        <span>词汇量 {{ video.vocabCount }}</span>
        <span>{{ video.sentenceCount }} 条字幕</span>
        <span v-if="video.createdAt" class="video-card__date">{{ formatDate(video.createdAt) }}</span>
      </div>
    </div>
  </NuxtLink>
</template>

<script setup lang="ts">
import type { VideoSummary } from '~/types/api'

const props = defineProps<{
  video: VideoSummary
  featured?: boolean
}>()

const { apiBase } = useApi()

const coverSrc = computed(() => {
  const url = props.video.coverUrl
  if (!url) return ''
  return url.startsWith('http') ? url : `${apiBase}${url}`
})

function formatDuration(sec: number) {
  const m = Math.floor(sec / 60)
  const s = sec % 60
  return `${m}:${String(s).padStart(2, '0')}`
}

function formatDate(iso: string) {
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return ''
  return `${d.getFullYear()}-${d.getMonth() + 1}-${d.getDate()}`
}
</script>

<style scoped>
.video-card__thumb {
  @apply relative aspect-video overflow-hidden bg-gradient-to-br from-green-50 to-gray-100;
}

.video-card__thumb--featured {
  @apply aspect-[16/10] lg:min-h-[280px];
}

.video-card__placeholder {
  @apply flex h-full w-full items-center justify-center;
}

.video-card__play {
  @apply flex h-12 w-12 items-center justify-center rounded-full bg-white/80 text-lg text-gray-600 shadow-sm;
}

.video-card__duration {
  @apply absolute bottom-2 left-2 rounded bg-black/70 px-1.5 py-0.5 text-xs text-white;
}

.video-card__vip {
  @apply absolute right-2 top-2 rounded bg-amber-500 px-2 py-0.5 text-xs font-medium text-white;
}

.video-card__tags {
  @apply mb-2 flex flex-wrap gap-1.5;
}

.video-card__tag {
  @apply rounded-full bg-gray-100 px-2 py-0.5 text-xs text-gray-600;
}

.video-card__title {
  @apply line-clamp-2 font-semibold text-gray-800 group-hover:text-[var(--island-primary)];
}

.video-card__desc {
  @apply mt-1.5 text-xs leading-relaxed text-gray-500;
}

.video-card__stats {
  @apply mt-2 flex flex-wrap items-center gap-x-3 gap-y-1 text-xs text-gray-400;
}

.video-card__date {
  @apply ml-auto;
}

.video-card--featured .video-card__stats {
  @apply mt-3 text-sm;
}
</style>
