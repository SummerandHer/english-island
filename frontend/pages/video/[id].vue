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

  <div v-else-if="video" class="video-detail" :class="{ 'video-detail--hide-player': videoHidden }">
    <div class="video-detail__body">
      <section class="video-detail__main">
        <VideoStudyHeader
          :title="video.title"
          :favorited="video.favorited"
          @favorite="onFavorite"
        />

        <div v-if="isEmbed && studyMode !== 'intensive'" class="embed-tip">
          当前为 B 站 Embed，句级重听/挖空体验有限；后续将全部改为自托管视频以获得完整体验。
        </div>

        <div v-show="!videoHidden" ref="playerWrap" class="video-detail__player">
          <iframe
            v-if="video.embedBvid"
            :src="embedUrl"
            class="video-detail__media"
            allowfullscreen
            frameborder="0"
          />
          <video
            v-else-if="videoSrc"
            ref="videoEl"
            :src="videoSrc"
            class="video-detail__media"
            playsinline
            @timeupdate="onTimeUpdate"
            @play="isPlaying = true"
            @pause="isPlaying = false"
            @ended="isPlaying = false"
          />
          <div v-else class="video-detail__empty">
            暂无可用播放源
          </div>
        </div>

        <VideoPlayerToolbar
          :playback-rate="playbackRate"
          :playing="isPlaying"
          :video-hidden="videoHidden"
          :sentence-loop="sentenceLoop"
          :single-pause="singlePause"
          @prev="onPrev"
          @next="onNext"
          @toggle-play="onTogglePlay"
          @fullscreen="onFullscreen"
          @rate="onRate"
          @update:video-hidden="videoHidden = $event"
          @update:sentence-loop="sentenceLoop = $event"
          @update:single-pause="singlePause = $event"
        />

        <VideoCurrentSentence
          :sentence="currentSentence"
          :index="currentIndex"
          :total="video.sentences.length"
          :mode="subtitleMode"
          :study-mode="studyMode"
          :revealed="currentBlindRevealed"
          :tokens="currentClozeTokens"
          :answers="clozeAnswers"
          :results="clozeResults"
          @reveal="revealCurrentBlind"
          @blank-input="onBlankInput"
        />
      </section>

      <aside class="video-detail__aside">
        <VideoSentenceList
          v-model:mode="subtitleMode"
          v-model:study-mode="studyMode"
          v-model:cloze-density="clozeDensity"
          :sentences="video.sentences"
          :current-index="currentIndex"
          :get-tokens="getClozeTokens"
          :answers="clozeAnswers"
          :results="clozeResults"
          :revealed="blindRevealed"
          @select="onSelectSentence"
          @repeat="onRepeatSentence"
          @reshuffle="reshuffleCloze()"
          @blank-input="onBlankInput"
          @reveal="revealBlindById"
        />
      </aside>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { ClozeDensity, StudyMode, SubtitleMode, VideoDetail } from '~/types/api'
import VideoPlayerToolbar from '~/components/video/VideoPlayerToolbar.vue'

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
const videoHidden = ref(false)
const sentenceLoop = ref(false)
const subtitleMode = ref<SubtitleMode>('bilingual')
const studyMode = ref<StudyMode>('intensive')
const clozeDensity = ref<ClozeDensity>('one')
const clozeSeed = ref(0)
const blindRevealed = ref<Record<number, boolean>>({})

let timer: ReturnType<typeof setInterval> | null = null
let simulatedMs = 0
let lastSentenceIndex = -1
let loopingHold = false

const sentencesRef = computed(() => video.value?.sentences ?? [])
const sync = useVideoSync(sentencesRef)
const { currentIndex, currentSentence, syncByTime, goPrev, goNext, selectIndex } = sync

const cloze = useCloze(sentencesRef, clozeDensity, clozeSeed)
const clozeAnswers = cloze.answers
const clozeResults = cloze.results
const getClozeTokens = cloze.getTokens
const reshuffleCloze = cloze.reshuffle
const setClozeAnswer = cloze.setAnswer

const isEmbed = computed(() => !!video.value?.embedBvid)
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

const currentBlindRevealed = computed(() => {
  const s = currentSentence.value
  if (!s) return false
  return !!blindRevealed.value[s.id]
})

const currentClozeTokens = computed(() => {
  const s = currentSentence.value
  if (!s) return []
  return getClozeTokens(s.id)
})

watch(studyMode, (m) => {
  if (m === 'blind') {
    blindRevealed.value = {}
    singlePause.value = true
  }
})

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
    handleBoundary(simulatedMs)
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
  handleBoundary(ms)
}

function handleBoundary(ms: number) {
  if (!video.value?.sentences.length) return
  const idx = currentIndex.value
  const sentence = video.value.sentences[idx]
  if (!sentence) return

  if (sentenceLoop.value && ms >= sentence.endMs - 80) {
    if (loopingHold) return
    loopingHold = true
    seekTo(sentence.startMs, true)
    window.setTimeout(() => {
      loopingHold = false
    }, 200)
    return
  }

  if (idx !== lastSentenceIndex) {
    lastSentenceIndex = idx
    loopingHold = false
    return
  }

  if (ms < sentence.endMs - 80) return

  if (singlePause.value && useNativePlayer.value && videoEl.value && !videoEl.value.paused) {
    videoEl.value.pause()
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
  seekTo(selectIndex(index), true)
}

function revealCurrentBlind() {
  const s = currentSentence.value
  if (!s) return
  revealBlindById(s.id)
}

function revealBlindById(sentenceId: number) {
  blindRevealed.value = { ...blindRevealed.value, [sentenceId]: true }
}

function onBlankInput(blankId: string, value: string) {
  setClozeAnswer(blankId, value)
}

function onTogglePlay() {
  if (useNativePlayer.value && videoEl.value) {
    if (videoEl.value.paused) {
      void videoEl.value.play()
    } else {
      videoEl.value.pause()
    }
    return
  }
  isPlaying.value = !isPlaying.value
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
  display: grid;
  grid-template-columns: 1fr;
  gap: 0.85rem;
}

.video-detail__main {
  display: flex;
  flex-direction: column;
  gap: 0.65rem;
}

/* 固定 16:9，contain 保证不变形；余白用柔和底色而不是死黑 */
.video-detail__player {
  position: relative;
  width: 100%;
  max-width: 100%;
  aspect-ratio: 16 / 9;
  overflow: hidden;
  border-radius: 14px;
  background: linear-gradient(160deg, #2a3530, #1c2420);
  box-shadow: 0 8px 28px rgba(31, 42, 36, 0.08);
}

.video-detail__media {
  display: block;
  width: 100%;
  height: 100%;
  border: 0;
  object-fit: contain;
  object-position: center;
  background: transparent;
}

.video-detail__empty {
  display: flex;
  height: 100%;
  align-items: center;
  justify-content: center;
  font-size: 0.875rem;
  color: rgba(255, 255, 255, 0.45);
}

.video-detail__aside {
  display: flex;
  min-height: 320px;
  flex-direction: column;
}

.embed-tip {
  border-radius: 10px;
  border: 1px solid rgba(180, 140, 60, 0.2);
  background: #fbf7ee;
  padding: 0.55rem 0.75rem;
  font-size: 0.75rem;
  color: #8a6b2e;
}

.video-detail--hide-player .video-detail__player {
  display: none;
}

@media (min-width: 768px) {
  .video-detail {
    height: 100%;
    overflow: hidden;
  }

  .video-detail__body {
    height: 100%;
    gap: 1rem;
    overflow: hidden;
    grid-template-columns: minmax(0, 1.12fr) minmax(0, 0.88fr);
  }

  .video-detail__main {
    display: flex;
    height: 100%;
    min-height: 0;
    flex-direction: column;
    gap: 0.65rem;
    overflow: hidden;
  }

  .video-detail__player {
    flex: 0 0 auto;
    width: min(100%, calc(34vh * 16 / 9), 640px);
    margin-inline: auto;
  }

  .video-detail__main > :last-child {
    flex: 1 1 auto;
    min-height: 110px;
    overflow: auto;
  }

  .video-detail__aside {
    min-height: 0;
    overflow: hidden;
  }
}

@media (min-width: 1100px) {
  .video-detail__body {
    grid-template-columns: minmax(0, 1.18fr) minmax(320px, 0.82fr);
  }

  .video-detail__player {
    width: min(100%, calc(38vh * 16 / 9), 720px);
  }
}
</style>
