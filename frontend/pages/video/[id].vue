<template>
  <div v-if="loadError" class="island-card p-8">
    <VipUpgradeBanner
      v-if="isVipError"
      title="VIP 专属视频"
      message="该视频为 VIP 专享。升级后可观看（支付功能即将上线）。"
      back-to="/video"
      @dismiss="navigateTo('/video')"
    />
    <template v-else>
      <p class="text-lg font-medium text-gray-700">{{ loadError }}</p>
      <NButton class="mt-4" @click="navigateTo('/video')">返回列表</NButton>
    </template>
  </div>

  <div v-else-if="video" class="video-detail">
    <div class="video-detail__body">
      <!-- 左侧 -->
      <section class="video-detail__main">
        <VideoStudyHeader
          :title="video.title"
          :favorited="video.favorited"
          @favorite="onFavorite"
        />

        <div ref="playerWrap" class="video-detail__player">
          <iframe
            v-if="video.embedBvid"
            :src="embedUrl"
            class="h-full w-full"
            allowfullscreen
            frameborder="0"
          />
          <video
            v-else-if="videoSrc"
            ref="videoEl"
            :src="videoSrc"
            class="h-full w-full object-contain"
            playsinline
            @timeupdate="onTimeUpdate"
            @play="isPlaying = true"
            @pause="isPlaying = false"
          />
          <div v-else class="flex h-full items-center justify-center text-sm text-gray-400">
            暂无可用播放源
          </div>
        </div>

        <VideoControls
          v-model:auto-follow="autoFollow"
          v-model:single-pause="singlePause"
          :playback-rate="playbackRate"
          :playing="isPlaying"
          @prev="onPrev"
          @next="onNext"
          @toggle-play="onTogglePlay"
          @fullscreen="onFullscreen"
          @rate="onRate"
        />

        <VideoCurrentSentence
          :sentence="currentSentence"
          :index="currentIndex"
          :total="video.sentences.length"
          :mode="subtitleMode"
        />
      </section>

      <!-- 右侧：语言 Tab + 句列表 -->
      <aside class="video-detail__aside">
        <VideoSentenceList
          v-model:mode="subtitleMode"
          :sentences="video.sentences"
          :current-index="currentIndex"
          @select="onSelectSentence"
          @repeat="onRepeatSentence"
        />
      </aside>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { VideoDetail } from '~/types/api'
import type { SubtitleMode } from '~/types/api'

definePageMeta({ layout: 'video' })

const route = useRoute()
const auth = useAuthStore()
const { request, apiBase } = useApi()

const video = ref<VideoDetail | null>(null)
const loadError = ref('')
const isVipError = ref(false)
const playerWrap = ref<HTMLElement>()
const videoEl = ref<HTMLVideoElement>()
const playbackRate = ref(1)
const isPlaying = ref(false)
const autoFollow = ref(true)
const singlePause = ref(false)
const subtitleMode = ref<SubtitleMode>('bilingual')
let timer: ReturnType<typeof setInterval> | null = null
let simulatedMs = 0
let lastSentenceIndex = -1

const sentencesRef = computed(() => video.value?.sentences ?? [])
const sync = useVideoSync(sentencesRef)
const { currentIndex, currentSentence, syncByTime, goPrev, goNext, selectIndex } = sync

const useNativePlayer = computed(() => !video.value?.embedBvid && !!video.value?.playUrl)

const videoSrc = computed(() => {
  const url = video.value?.playUrl
  if (!url) return ''
  return url.startsWith('http') ? url : `${apiBase}${url}`
})

const embedUrl = computed(() =>
  video.value?.embedBvid
    ? `https://player.bilibili.com/player.html?bvid=${video.value.embedBvid}&page=1&high_quality=1&danmaku=0`
    : ''
)

onMounted(async () => {
  auth.hydrate()
  if (window.matchMedia('(min-width: 768px)').matches) {
    document.documentElement.classList.add('video-page-lock')
  }
  try {
    video.value = await request<VideoDetail>(`/api/v1/videos/${route.params.id}`)
    if (!useNativePlayer.value) {
      startSimulatedSync()
    }
  } catch (e: unknown) {
    const msg = e instanceof Error ? e.message : '加载失败'
    loadError.value = msg
    isVipError.value = msg.includes('VIP')
  }
})

onUnmounted(() => {
  document.documentElement.classList.remove('video-page-lock')
  stopSimulatedSync()
})

function startSimulatedSync() {
  stopSimulatedSync()
  timer = setInterval(() => {
    if (!video.value?.sentences.length) return
    simulatedMs += 500 * playbackRate.value
    const last = video.value.sentences[video.value.sentences.length - 1]
    if (simulatedMs > last.endMs + 2000) simulatedMs = 0
    syncByTime(simulatedMs)
    checkSinglePause(simulatedMs)
  }, 500)
}

function stopSimulatedSync() {
  if (timer) {
    clearInterval(timer)
    timer = null
  }
}

function onTimeUpdate() {
  if (!videoEl.value) return
  const ms = Math.round(videoEl.value.currentTime * 1000)
  if (autoFollow.value) {
    syncByTime(ms)
  }
  checkSinglePause(ms)
}

function checkSinglePause(ms: number) {
  if (!singlePause.value || !video.value?.sentences.length) return
  const idx = currentIndex.value
  const sentence = video.value.sentences[idx]
  if (!sentence) return
  if (idx !== lastSentenceIndex) {
    lastSentenceIndex = idx
    return
  }
  if (ms >= sentence.endMs - 80) {
    if (useNativePlayer.value && videoEl.value && !videoEl.value.paused) {
      videoEl.value.pause()
    }
  }
}

function seekTo(ms: number | null, autoplay = false) {
  if (ms == null) return
  if (useNativePlayer.value && videoEl.value) {
    videoEl.value.currentTime = ms / 1000
    syncByTime(ms)
    if (autoplay) void videoEl.value.play()
    return
  }
  simulatedMs = ms
  syncByTime(ms)
}

function onPrev() { seekTo(goPrev()) }
function onNext() { seekTo(goNext()) }
function onSelectSentence(index: number) { seekTo(selectIndex(index), true) }
function onRepeatSentence(index: number) {
  const ms = selectIndex(index)
  seekTo(ms, true)
}

function onTogglePlay() {
  if (useNativePlayer.value && videoEl.value) {
    if (videoEl.value.paused) {
      void videoEl.value.play()
    } else {
      videoEl.value.pause()
    }
  }
}

function onRate(rate: number) {
  playbackRate.value = rate
  if (videoEl.value) {
    videoEl.value.playbackRate = rate
  }
}

function onFullscreen() {
  const target = useNativePlayer.value ? videoEl.value : playerWrap.value
  target?.requestFullscreen?.()
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

<style>
@media (min-width: 768px) {
  html.video-page-lock,
  html.video-page-lock body {
    overflow: hidden;
    height: 100%;
  }
}
</style>

<style scoped>
.video-detail__body {
  @apply grid grid-cols-1 gap-3;
}

.video-detail__main {
  @apply flex flex-col gap-2;
}

.video-detail__player {
  @apply aspect-video w-full overflow-hidden rounded-xl bg-black shadow-sm;
}

.video-detail__aside {
  @apply min-h-[280px];
}

@media (min-width: 768px) {
  .video-detail {
    @apply h-full overflow-hidden;
  }

  .video-detail__body {
    @apply h-full gap-4 overflow-hidden;
    grid-template-columns: minmax(0, 1.08fr) minmax(0, 0.92fr);
  }

  .video-detail__main {
    @apply grid h-full min-h-0 gap-2 overflow-hidden;
    grid-template-rows: auto minmax(0, 1fr) auto minmax(96px, 22%);
  }

  .video-detail__player {
    aspect-ratio: unset;
    @apply min-h-0 rounded-xl;
  }

  .video-detail__aside {
    @apply flex min-h-0 flex-col overflow-hidden;
  }
}
</style>
