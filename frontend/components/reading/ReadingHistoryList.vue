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
    <p v-else-if="!history.length" class="text-sm text-gray-500">暂无提交记录，完成一篇模拟练习后将显示在这里。</p>

    <div v-else class="space-y-3">
      <div
        v-for="item in history"
        :key="item.submissionId"
        class="rounded-lg border border-gray-100 p-4 transition hover:border-green-200"
      >
        <button type="button" class="w-full text-left" @click="toggle(item.submissionId)">
          <div class="flex flex-wrap items-center gap-2">
            <span
              class="font-semibold"
              :class="scoreClass(item.correctCount, item.totalQuestions)"
            >
              {{ item.correctCount }}/{{ item.totalQuestions }}
            </span>
            <span class="text-xs text-gray-400">{{ formatTime(item.createdAt) }}</span>
          </div>
          <p class="mt-1 line-clamp-1 text-sm text-gray-600">{{ item.passageTitle }}</p>
        </button>

        <div v-if="expandedId === item.submissionId" class="mt-3 border-t border-gray-100 pt-3">
          <NuxtLink
            :to="practiceLink(item)"
            class="text-sm text-[var(--island-primary)] hover:underline"
          >
            再练此篇 →
          </NuxtLink>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import type { ReadingSubmissionSummary } from '~/types/api'

withDefaults(defineProps<{
  title?: string
}>(), {
  title: '阅读练习记录'
})

const auth = useAuthStore()
const { request } = useApi()
const message = useAppMessage()
const history = ref<ReadingSubmissionSummary[]>([])
const loading = ref(false)
const expandedId = ref<number | null>(null)

function scoreClass(correct: number, total: number) {
  if (total <= 0) return 'text-gray-600'
  const ratio = correct / total
  if (ratio >= 0.75) return 'text-green-600'
  if (ratio >= 0.5) return 'text-amber-600'
  return 'text-red-600'
}

function formatTime(iso: string) {
  try {
    return new Date(iso).toLocaleString('zh-CN', { month: 'numeric', day: 'numeric', hour: '2-digit', minute: '2-digit' })
  } catch {
    return iso
  }
}

function practiceLink(item: ReadingSubmissionSummary) {
  const chapter = item.chapterSlug ? `?chapter=${item.chapterSlug}` : ''
  return `/reading/practice/${item.passageId}${chapter}`
}

async function loadHistory() {
  if (!auth.isLoggedIn) return
  loading.value = true
  try {
    history.value = await request<ReadingSubmissionSummary[]>('/api/v1/reading/submissions/mine?limit=20')
  } catch (e: unknown) {
    message.error(e instanceof Error ? e.message : '加载失败')
  } finally {
    loading.value = false
  }
}

function toggle(id: number) {
  expandedId.value = expandedId.value === id ? null : id
}

watch(() => auth.isLoggedIn, (loggedIn) => {
  if (loggedIn) loadHistory()
  else {
    history.value = []
    expandedId.value = null
  }
}, { immediate: true })

defineExpose({ refresh: loadHistory })
</script>
