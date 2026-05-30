import type { Ref } from 'vue'
import type { VideoSentence } from '~/types/api'

export function useVideoSync(sentences: Ref<VideoSentence[]>) {
  const currentIndex = ref(0)
  const isPlaying = ref(false)
  const playbackRate = ref(1)
  const currentTimeMs = ref(0)

  const currentSentence = computed(() => sentences.value[currentIndex.value])

  function syncByTime(ms: number) {
    currentTimeMs.value = ms
    const idx = sentences.value.findIndex(s => ms >= s.startMs && ms < s.endMs)
    if (idx >= 0) currentIndex.value = idx
  }

  function goPrev() {
    if (currentIndex.value > 0) {
      currentIndex.value--
      return sentences.value[currentIndex.value].startMs
    }
    return null
  }

  function goNext() {
    if (currentIndex.value < sentences.value.length - 1) {
      currentIndex.value++
      return sentences.value[currentIndex.value].startMs
    }
    return null
  }

  function selectIndex(index: number) {
    if (index >= 0 && index < sentences.value.length) {
      currentIndex.value = index
      return sentences.value[index].startMs
    }
    return null
  }

  return {
    currentIndex,
    currentSentence,
    isPlaying,
    playbackRate,
    currentTimeMs,
    syncByTime,
    goPrev,
    goNext,
    selectIndex
  }
}
