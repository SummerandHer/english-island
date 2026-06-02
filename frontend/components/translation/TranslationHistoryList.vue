<template>
  <section class="island-card p-6">
    <div class="mb-4 flex items-center justify-between">
      <h2 class="font-bold">{{ title }}</h2>
      <NButton v-if="auth.isLoggedIn" text type="primary" :loading="loading" @click="loadHistory">
        刷新
      </NButton>
    </div>

    <p v-if="!auth.isLoggedIn" class="text-sm text-gray-500">登录后可查看练习记录。</p>
    <p v-else-if="loading && !history.length" class="text-sm text-gray-500">加载中…</p>
    <p v-else-if="!history.length" class="text-sm text-gray-500">暂无提交记录。</p>

    <div v-else class="space-y-3">
      <div
        v-for="item in history"
        :key="item.submissionId"
        class="rounded-lg border border-gray-100 p-4 transition hover:border-green-200"
      >
        <button type="button" class="w-full text-left" @click="toggleHistory(item.submissionId)">
          <div class="flex flex-wrap items-center gap-2">
            <span class="font-semibold text-[var(--island-primary)]">{{ item.cetScore }}/15</span>
            <NTag :type="bandTagType(item.band)" size="small">{{ item.band }} · {{ item.bandLabel }}</NTag>
            <span class="text-xs text-gray-400">{{ formatSubmissionTime(item.createdAt) }}</span>
            <span v-if="item.errorCount" class="text-xs text-red-500">{{ item.errorCount }} 处扣分</span>
          </div>
          <p class="mt-1 line-clamp-1 text-sm text-gray-600">{{ item.promptPreview }}</p>
        </button>

        <div v-if="expandedId === item.submissionId" class="mt-3 border-t border-gray-100 pt-3 text-sm">
          <div class="mb-3 flex flex-wrap gap-2">
            <NuxtLink
              :to="`/translation/practice/${item.questionId}`"
              class="text-sm text-[var(--island-primary)] hover:underline"
            >
              再做此题 →
            </NuxtLink>
          </div>
          <p class="mb-2 text-gray-700">{{ item.overallComment }}</p>
          <p
            class="mb-3 rounded bg-gray-50 p-2 leading-relaxed text-gray-800"
            v-html="highlightAnswerErrors(item.userAnswer, item.errors)"
          />
          <ul v-if="item.errors?.length" class="space-y-2">
            <li v-for="(err, i) in item.errors" :key="i" class="rounded bg-red-50/50 p-2 text-xs">
              <strong>{{ err.span }}</strong> → {{ err.suggestion }}
              <span class="text-gray-500">（{{ err.reason }}）</span>
            </li>
          </ul>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import type { TranslationSubmissionSummary } from '~/types/api'
import { bandTagType, formatSubmissionTime, highlightAnswerErrors } from '~/utils/translationGrading'

withDefaults(defineProps<{
  title?: string
}>(), {
  title: '我的练习'
})

const auth = useAuthStore()
const { request } = useApi()
const message = useAppMessage()
const history = ref<TranslationSubmissionSummary[]>([])
const loading = ref(false)
const expandedId = ref<number | null>(null)

async function loadHistory() {
  if (!auth.isLoggedIn) return
  loading.value = true
  try {
    history.value = await request<TranslationSubmissionSummary[]>('/api/v1/translation/submissions/mine?limit=10')
  } catch (e: unknown) {
    message.error(e instanceof Error ? e.message : '加载练习记录失败')
  } finally {
    loading.value = false
  }
}

function toggleHistory(id: number) {
  expandedId.value = expandedId.value === id ? null : id
}

watch(() => auth.isLoggedIn, (loggedIn) => {
  if (loggedIn) {
    loadHistory()
  } else {
    history.value = []
    expandedId.value = null
  }
}, { immediate: true })

defineExpose({ refresh: loadHistory })
</script>

<style scoped>
:deep(.translation-error-mark) {
  background: #fef2f2;
  color: #b91c1c;
  padding: 0 2px;
  border-radius: 2px;
}
</style>
