<template>
  <div v-if="video" class="space-y-4">
    <div class="flex items-center gap-2">
      <NuxtLink to="/video" class="text-sm text-[var(--island-primary)]">← 返回</NuxtLink>
      <h1 class="text-lg font-bold">{{ video.title }}</h1>
    </div>
    <div class="grid gap-4 lg:grid-cols-2">
      <div class="space-y-3">
        <div ref="playerWrap" class="aspect-video overflow-hidden rounded-xl bg-black">
          <iframe
            v-if="video.embedBvid"
            :src="embedUrl"
            class="h-full w-full"
            allowfullscreen
            frameborder="0"
          />
        </div>
        <VideoControls
          :playback-rate="playbackRate"
          :favorited="video.favorited"
          @prev="onPrev"
          @next="onNext"
          @toggle-play="onTogglePlay"
          @fullscreen="onFullscreen"
          @rate="onRate"
          @favorite="onFavorite"
        />
        <VideoCurrentSentence
          :sentence="sync.currentSentence"
          :index="sync.currentIndex"
          :total="video.sentences.length"
        />
      </div>
      <VideoSentenceList
        :sentences="video.sentences"
        :current-index="sync.currentIndex"
        @select="onSelectSentence"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import type { VideoDetail } from '~/types/api'

const route = useRoute()
const auth = useAuthStore()
const { request } = useApi()

const video = ref<VideoDetail | null>(null)
const playerWrap = ref<HTMLElement>()
const playbackRate = ref(1)
let timer: ReturnType<typeof setInterval> | null = null
let simulatedMs = 0

const sentencesRef = computed(() => video.value?.sentences ?? [])
const sync = useVideoSync(sentencesRef)
const { syncByTime, goPrev, goNext, selectIndex } = sync

const embedUrl = computed(() =>
  video.value?.embedBvid
    ? `https://player.bilibili.com/player.html?bvid=${video.value.embedBvid}&page=1&high_quality=1&danmaku=0`
    : ''
)

onMounted(async () => {
  auth.hydrate()
  video.value = await request<VideoDetail>(`/api/v1/videos/${route.params.id}`)
  startSimulatedSync()
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})

function startSimulatedSync() {
  if (timer) clearInterval(timer)
  timer = setInterval(() => {
    if (!video.value?.sentences.length) return
    simulatedMs += 500 * playbackRate.value
    const last = video.value.sentences[video.value.sentences.length - 1]
    if (simulatedMs > last.endMs + 2000) simulatedMs = 0
    syncByTime(simulatedMs)
  }, 500)
}

function seekTo(ms: number | null) {
  if (ms == null) return
  simulatedMs = ms
  syncByTime(ms)
}

function onPrev() { seekTo(goPrev()) }
function onNext() { seekTo(goNext()) }
function onSelectSentence(index: number) { seekTo(selectIndex(index)) }
function onTogglePlay() { /* B站 iframe 播放控制受限，MVP 用模拟轴 */ }
function onRate(rate: number) { playbackRate.value = rate }
function onFullscreen() {
  playerWrap.value?.requestFullscreen?.()
}

async function onFavorite() {
  if (!auth.isLoggedIn) {
    navigateTo('/login')
    return
  }
  await request(`/api/v1/videos/${route.params.id}/favorite`, { method: 'POST' })
  if (video.value) video.value.favorited = !video.value.favorited
}
</script>
