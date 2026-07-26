export function useExamTimer(durationSec: number, onTimeout: () => void) {
  const enabled = ref(false)
  const remainingSec = ref(durationSec)
  let timer: ReturnType<typeof setInterval> | null = null

  const formatted = computed(() => {
    const m = Math.floor(remainingSec.value / 60)
    const s = remainingSec.value % 60
    return `${m}:${String(s).padStart(2, '0')}`
  })

  const isLow = computed(() => enabled.value && remainingSec.value <= 120)
  const expired = computed(() => enabled.value && remainingSec.value <= 0)

  function start() {
    stopInterval()
    enabled.value = true
    remainingSec.value = durationSec
    timer = setInterval(() => {
      if (remainingSec.value <= 0) {
        stopInterval()
        onTimeout()
        return
      }
      remainingSec.value -= 1
    }, 1000)
  }

  function stopInterval() {
    if (timer) {
      clearInterval(timer)
      timer = null
    }
  }

  function stop() {
    stopInterval()
    enabled.value = false
  }

  onUnmounted(stop)

  return { enabled, remainingSec, formatted, isLow, expired, start, stop }
}

export const READING_EXAM_SECONDS = 18 * 60
export const TRANSLATION_EXAM_SECONDS = 30 * 60
