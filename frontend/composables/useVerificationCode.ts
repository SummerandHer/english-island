export type VerificationScene = 'register' | 'reset_password'

export function useVerificationCode(scene: VerificationScene) {
  const { request } = useApi()
  const message = useAppMessage()

  const sending = ref(false)
  const cooldown = ref(0)
  let timer: ReturnType<typeof setInterval> | null = null

  function startCooldown(seconds: number) {
    cooldown.value = seconds
    if (timer) clearInterval(timer)
    timer = setInterval(() => {
      cooldown.value -= 1
      if (cooldown.value <= 0 && timer) {
        clearInterval(timer)
        timer = null
      }
    }, 1000)
  }

  onUnmounted(() => {
    if (timer) clearInterval(timer)
  })

  async function sendCode(email: string) {
    const trimmed = email.trim()
    if (!trimmed) {
      message.warning('请先输入 QQ 邮箱')
      return
    }
    if (!/^[a-zA-Z0-9._%+-]+@qq\.com$/i.test(trimmed)) {
      message.warning('仅支持 @qq.com 邮箱')
      return
    }
    if (cooldown.value > 0) return

    sending.value = true
    try {
      const data = await request<{ cooldownSeconds: number }>('/api/v1/auth/send-code', {
        method: 'POST',
        body: { email: trimmed, scene }
      })
      message.success('验证码已发送，请查收 QQ 邮箱')
      startCooldown(Math.max(data.cooldownSeconds || 60, 60))
    } catch (e: unknown) {
      message.error(e instanceof Error ? e.message : '发送失败')
    } finally {
      sending.value = false
    }
  }

  return { sending, cooldown, sendCode }
}
