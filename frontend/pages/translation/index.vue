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

    <section class="island-card mb-8 p-6">
      <h2 class="mb-4 font-bold">模拟练习</h2>
      <p v-if="questionsLoading" class="text-sm text-gray-500">加载题目中…</p>
      <p v-else-if="!auth.isLoggedIn" class="mb-3 text-sm text-gray-500">
        可浏览题目列表；
        <NuxtLink to="/login" class="text-[var(--island-primary)] hover:underline">登录</NuxtLink>
        后开始 AI 批改。
      </p>
      <p v-else-if="!questions.length" class="text-sm text-gray-500">暂无练习题。</p>
      <div v-else class="space-y-3">
        <NuxtLink
          v-for="q in questions"
          :key="q.id"
          :to="`/translation/practice/${q.id}`"
          class="block rounded-lg border border-gray-100 p-4 transition hover:border-green-200 hover:shadow-sm"
        >
          <div class="mb-2 flex flex-wrap items-center gap-2">
            <NTag size="small" type="info">{{ questionDirectionLabel(q.direction) }}</NTag>
            <NTag v-if="q.mock" size="small">模拟题</NTag>
            <NTag v-if="q.vip" type="warning" size="small">VIP</NTag>
          </div>
          <p class="line-clamp-2 text-sm text-gray-700">{{ questionPromptPreview(q) }}</p>
          <p class="mt-2 text-xs text-[var(--island-primary)]">开始练习 →</p>
        </NuxtLink>
      </div>
    </section>

    <TranslationHistoryList />
  </div>
</template>

<script setup lang="ts">
import type { TranslationQuestionSummary } from '~/types/api'
import { questionDirectionLabel, questionPromptPreview } from '~/utils/translationQuestion'

interface Chapter { id: number; title: string; slug: string; summary?: string; vip: boolean }

const auth = useAuthStore()
const { request } = useApi()
const message = useAppMessage()
const chapters = ref<Chapter[]>([])
const questions = ref<TranslationQuestionSummary[]>([])
const questionsLoading = ref(false)

onMounted(async () => {
  auth.hydrate()
  chapters.value = await request<Chapter[]>('/api/v1/translation/chapters')
  await loadQuestions()
})

async function loadQuestions() {
  questionsLoading.value = true
  try {
    questions.value = await request<TranslationQuestionSummary[]>('/api/v1/translation/questions')
  } catch (e: unknown) {
    message.error(e instanceof Error ? e.message : '加载题目失败')
  } finally {
    questionsLoading.value = false
  }
}
</script>
