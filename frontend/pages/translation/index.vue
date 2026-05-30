<template>
  <div>
    <h1 class="mb-4 text-xl font-bold">翻译技巧</h1>
    <div class="mb-8 space-y-3">
      <NuxtLink
        v-for="c in chapters"
        :key="c.id"
        :to="`/translation/${c.slug}`"
        class="island-card block p-4 transition hover:shadow-md"
      >
        <div class="flex items-center justify-between">
          <h3 class="font-medium">{{ c.title }}</h3>
          <NTag v-if="c.vip" type="warning" size="small">VIP</NTag>
        </div>
        <p class="text-sm text-gray-500">{{ c.summary }}</p>
      </NuxtLink>
    </div>
    <section class="island-card p-6">
      <h2 class="mb-4 font-bold">模拟练习 · AI 批改</h2>
      <div v-if="question" class="space-y-4">
        <p class="rounded-lg bg-green-50 p-4 text-gray-800">{{ question.promptZh }}</p>
        <NInput v-model:value="answer" type="textarea" :rows="4" placeholder="在此输入英文译文..." />
        <NButton type="primary" :loading="grading" :disabled="!auth.isLoggedIn" @click="submit">
          {{ auth.isLoggedIn ? '提交批改' : '请先登录' }}
        </NButton>
        <div v-if="result" class="rounded-lg border border-green-100 bg-white p-4">
          <p class="mb-2 text-lg font-semibold text-[var(--island-primary)]">得分：{{ result.score }}</p>
          <p class="text-gray-700">{{ result.overallComment }}</p>
          <ul v-if="result.errors?.length" class="mt-3 space-y-2 text-sm">
            <li v-for="(err, i) in result.errors" :key="i" class="rounded bg-gray-50 p-2">
              <strong>{{ err.span }}</strong> → {{ err.suggestion }}
              <span class="text-gray-500">（{{ err.reason }}）</span>
            </li>
          </ul>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
interface Chapter { id: number; title: string; slug: string; summary?: string; vip: boolean }
interface Question { id: number; promptZh?: string }
interface GradingResult {
  submissionId: number
  score: number
  overallComment: string
  errors: { span: string; suggestion: string; reason: string }[]
  referenceHint: string
}

const auth = useAuthStore()
const { request } = useApi()
const chapters = ref<Chapter[]>([])
const question = ref<Question | null>(null)
const answer = ref('')
const grading = ref(false)
const result = ref<GradingResult | null>(null)

onMounted(async () => {
  auth.hydrate()
  chapters.value = await request<Chapter[]>('/api/v1/translation/chapters')
  const qs = await request<Question[]>('/api/v1/translation/questions')
  question.value = qs[0] || null
})

async function submit() {
  if (!question.value) return
  grading.value = true
  try {
    result.value = await request<GradingResult>('/api/v1/translation/submissions', {
      method: 'POST',
      body: { questionId: question.value.id, userAnswer: answer.value }
    })
  } catch (e: unknown) {
    alert(e instanceof Error ? e.message : '批改失败')
  } finally {
    grading.value = false
  }
}
</script>
