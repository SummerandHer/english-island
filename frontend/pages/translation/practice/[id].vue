<template>
  <div>
    <NuxtLink to="/translation" class="mb-4 inline-block text-sm text-[var(--island-primary)]">
      ← 返回翻译技巧
    </NuxtLink>

    <div v-if="loading" class="island-card p-6 text-sm text-gray-500">加载题目中…</div>

    <div v-else-if="errorMessage" class="island-card p-6">
      <VipUpgradeBanner v-if="isVipError" back-to="/translation" @dismiss="navigateTo('/translation')" />
      <template v-else>
        <p class="mb-4 text-gray-700">{{ errorMessage }}</p>
        <NButton v-if="!auth.isLoggedIn" type="primary" @click="navigateTo('/login')">去登录</NButton>
        <NButton v-else quaternary @click="loadQuestion">重试</NButton>
      </template>
    </div>

    <template v-else-if="question">
      <header class="mb-4 flex flex-wrap items-start justify-between gap-3">
        <div>
          <h1 class="text-xl font-bold">模拟练习 · AI 批改</h1>
          <p class="mt-1 text-sm text-gray-500">题目 #{{ question.id }} · 翻译限时约 30 分钟</p>
        </div>
        <div class="flex flex-wrap items-center gap-2">
          <NButton
            size="small"
            :type="timedMode ? 'warning' : 'default'"
            :disabled="!!timeExpired"
            @click="toggleTimedMode"
          >
            {{ timedMode ? '退出限时' : '限时 30 分钟' }}
          </NButton>
          <span
            v-if="timedMode"
            class="rounded-lg px-3 py-1 text-sm font-medium"
            :class="timer.isLow || timeExpired ? 'bg-red-100 text-red-700' : 'bg-amber-100 text-amber-800'"
          >
            ⏱ {{ timeExpired ? '0:00' : timer.formatted }}
          </span>
        </div>
      </header>

      <section class="island-card mb-8 p-6">
        <TranslationPracticePanel
          :question="question"
          :timed-mode="timedMode"
          :time-expired="timeExpired"
          @submitted="onSubmitted"
        />
      </section>

      <TranslationHistoryList ref="historyRef" />
    </template>
  </div>
</template>

<script setup lang="ts">
import { TRANSLATION_EXAM_SECONDS, useExamTimer } from '~/composables/useExamTimer'
import type { TranslationQuestionDetail, TranslationSubmissionResult } from '~/types/api'

const route = useRoute()
const auth = useAuthStore()
const { request } = useApi()
const message = useAppMessage()

const questionId = computed(() => Number(route.params.id))
const question = ref<TranslationQuestionDetail | null>(null)
const loading = ref(true)
const errorMessage = ref('')
const isVipError = ref(false)
const historyRef = ref<{ refresh: () => Promise<void> } | null>(null)
const timedMode = ref(false)
const timeExpired = ref(false)

const timer = useExamTimer(TRANSLATION_EXAM_SECONDS, () => {
  timeExpired.value = true
  message.warning('时间到，请提交译文进行批改')
})

onMounted(async () => {
  auth.hydrate()
  await loadQuestion()
})

onUnmounted(() => {
  timer.stop()
})

watch(() => auth.isLoggedIn, () => {
  if (!question.value) {
    loadQuestion()
  }
})

function toggleTimedMode() {
  if (timedMode.value) {
    timedMode.value = false
    timeExpired.value = false
    timer.stop()
    return
  }
  timedMode.value = true
  timeExpired.value = false
  timer.start()
}

async function loadQuestion() {
  if (!Number.isFinite(questionId.value) || questionId.value <= 0) {
    errorMessage.value = '题目 ID 无效'
    loading.value = false
    return
  }

  if (!auth.isLoggedIn) {
    errorMessage.value = '请先登录后再练习本题。'
    loading.value = false
    return
  }

  loading.value = true
  errorMessage.value = ''
  isVipError.value = false
  question.value = null

  try {
    question.value = await request<TranslationQuestionDetail>(`/api/v1/translation/questions/${questionId.value}`)
  } catch (e: unknown) {
    const msg = e instanceof Error ? e.message : '加载失败'
    if (msg.includes('VIP')) {
      errorMessage.value = '该题为 VIP 专属练习，请升级后使用。'
      isVipError.value = true
    } else if (msg.includes('不存在')) {
      errorMessage.value = '题目不存在或已下线。'
    } else {
      errorMessage.value = msg
    }
  } finally {
    loading.value = false
  }
}

function onSubmitted(_result: TranslationSubmissionResult) {
  timer.stop()
  timedMode.value = false
  timeExpired.value = false
  historyRef.value?.refresh()
}
</script>
