<template>
  <div>
    <div v-if="!auth.isLoggedIn" class="island-card p-6">
      <p class="mb-4 text-gray-700">登录后开始今日复习队列。</p>
      <NButton type="primary" @click="navigateTo('/login')">去登录</NButton>
    </div>

    <template v-else>
      <div v-if="loading" class="island-card p-6 text-sm text-gray-500">加载中…</div>

      <div v-else-if="loadError" class="island-card p-6">
        <p class="mb-4 text-gray-700">{{ loadError }}</p>
        <NButton quaternary @click="loadToday">重试</NButton>
      </div>

      <div v-else-if="!plan || plan.items.length === 0" class="island-card p-6">
        <p class="text-gray-700">今日暂无待复习词汇，明天再来看看吧 🎉</p>
      </div>

      <template v-else-if="current">
        <div class="mb-4 flex items-center justify-between text-sm text-gray-500">
          <span>进度 {{ currentIndex + 1 }} / {{ plan?.items.length }}</span>
          <span>待复习 {{ plan?.dueCount }} 词</span>
        </div>

        <div class="island-card p-6">
          <div class="mb-4 flex flex-wrap items-center gap-2">
            <span class="text-2xl font-bold">{{ current.vocab.word }}</span>
            <NButton quaternary size="small" title="发音" @click="speakWord(current.vocab.word)">🔊</NButton>
            <NTag size="small">{{ current.vocab.examLevel?.toUpperCase() }}</NTag>
            <NTag v-if="current.vocab.freqRank" size="small" quaternary>#{{ current.vocab.freqRank }}</NTag>
          </div>
          <p v-if="current.vocab.phonetic" class="mb-2 text-sm text-gray-500">{{ current.vocab.phonetic }}</p>
          <p v-if="current.vocab.partOfSpeech" class="mb-4 text-xs text-gray-400">{{ current.vocab.partOfSpeech }}</p>

          <div
            v-if="!revealed"
            class="flex min-h-[120px] cursor-pointer items-center justify-center rounded-lg border border-dashed border-gray-200 bg-gray-50 text-sm text-gray-500"
            @click="revealed = true"
          >
            点击显示释义
          </div>
          <div v-else class="space-y-3">
            <p class="rounded-lg bg-green-50 p-4 text-lg text-gray-800">
              {{ detail?.meaningZh || current.vocab.meaningBrief }}
            </p>
            <p v-if="detail?.exampleEn" class="rounded-lg bg-gray-50 p-3 text-sm leading-relaxed text-gray-700">
              <span class="block text-gray-800">{{ detail.exampleEn }}</span>
              <span v-if="detail.exampleZh" class="mt-1 block text-gray-500">{{ detail.exampleZh }}</span>
            </p>
            <ul v-if="detail?.phrases?.length" class="text-xs text-gray-500">
              <li v-for="(p, i) in detail.phrases.slice(0, 2)" :key="i">{{ p.en }} — {{ p.zh }}</li>
            </ul>
          </div>

          <div v-if="revealed" class="mt-6 flex flex-wrap gap-3">
            <NButton type="primary" @click="submitReview('know')">认识</NButton>
            <NButton @click="submitReview('vague')">模糊</NButton>
            <NButton type="error" ghost @click="submitReview('unknown')">不认识</NButton>
          </div>
          <p v-if="lastReviewHint" class="mt-4 text-sm text-gray-500">{{ lastReviewHint }}</p>
        </div>

        <div class="mt-4 flex gap-3">
          <NButton quaternary :disabled="currentIndex <= 0" @click="prevCard">上一词</NButton>
          <NButton quaternary @click="addNotebook">加入生词本</NButton>
        </div>
      </template>
    </template>
  </div>
</template>

<script setup lang="ts">
import type { VocabDetail, VocabTodayPlan } from '~/types/api'
import { speakWord } from '~/utils/speech'

const emit = defineEmits<{
  refreshed: []
}>()

const auth = useAuthStore()
const { request } = useApi()
const message = useAppMessage()

const plan = ref<VocabTodayPlan | null>(null)
const loading = ref(false)
const loadError = ref('')
const currentIndex = ref(0)
const revealed = ref(false)
const detail = ref<VocabDetail | null>(null)
const lastReviewHint = ref('')

const current = computed(() => plan.value?.items[currentIndex.value] ?? null)

async function loadToday() {
  loading.value = true
  loadError.value = ''
  try {
    plan.value = await request<VocabTodayPlan>('/api/v1/vocabulary/today')
    currentIndex.value = 0
    revealed.value = false
    detail.value = null
  } catch (e: unknown) {
    plan.value = null
    loadError.value = e instanceof Error ? e.message : '加载失败'
    message.error(loadError.value)
  } finally {
    loading.value = false
  }
}

async function loadDetail(id: number) {
  try {
    detail.value = await request<VocabDetail>(`/api/v1/vocabulary/${id}`)
  } catch {
    detail.value = null
  }
}

watch(current, (c) => {
  revealed.value = false
  detail.value = null
  lastReviewHint.value = ''
  if (c?.vocab.id) {
    loadDetail(c.vocab.id)
  }
})

interface ReviewResult {
  vocabularyId: number
  result: string
  familiarity: number
  nextReviewAt: string
  daysUntilNext: number
}

async function submitReview(result: 'know' | 'vague' | 'unknown') {
  if (!current.value) return
  try {
    const res = await request<ReviewResult>('/api/v1/vocabulary/review', {
      method: 'POST',
      body: { vocabularyId: current.value.vocab.id, result }
    })
    const days = res.daysUntilNext ?? 0
    lastReviewHint.value =
      days <= 0 ? '下次复习：今天稍后会再次出现' : `下次复习：约 ${days} 天后`
    if (currentIndex.value < (plan.value?.items.length ?? 1) - 1) {
      currentIndex.value += 1
    } else {
      await loadToday()
    }
    revealed.value = false
    emit('refreshed')
  } catch (e: unknown) {
    message.error(e instanceof Error ? e.message : '提交失败')
  }
}

function prevCard() {
  if (currentIndex.value > 0) {
    currentIndex.value -= 1
  }
}

async function addNotebook() {
  if (!current.value) return
  try {
    await request('/api/v1/vocabulary/notebook', {
      method: 'POST',
      body: { vocabularyId: current.value.vocab.id }
    })
    message.success('已加入生词本')
    emit('refreshed')
  } catch (e: unknown) {
    message.error(e instanceof Error ? e.message : '操作失败')
  }
}

onMounted(async () => {
  auth.hydrate()
  if (auth.isLoggedIn) {
    await loadToday()
  }
})

watch(() => auth.isLoggedIn, async (loggedIn) => {
  if (loggedIn) {
    await loadToday()
  }
})

defineExpose({ reload: loadToday })
</script>
