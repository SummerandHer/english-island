<template>
  <div>
    <h1 class="mb-2 text-xl font-bold">每日词汇</h1>
    <p class="mb-4 text-sm text-gray-500">今日 20 词 · 认识 / 模糊 / 不认识</p>

    <template v-if="auth.isLoggedIn">
      <VocabBookSelector
        v-model="examLevel"
        :saving="settingsSaving"
        @change="saveExamLevel"
      />
      <VocabCheckinCard :stats="checkinStats" />
      <VocabProgressBar :stats="stats" />
    </template>

    <NTabs v-model:value="activeTab" type="line" animated>
      <NTabPane name="review" tab="今日复习">
        <VocabReviewTab ref="reviewRef" @refreshed="onReviewRefreshed" />
      </NTabPane>
      <NTabPane name="notebook" :tab="notebookTabLabel">
        <VocabNotebookTab ref="notebookRef" />
      </NTabPane>
      <NTabPane name="search" tab="查词">
        <VocabSearchTab />
      </NTabPane>
    </NTabs>
  </div>
</template>

<script setup lang="ts">
import type { VocabCheckinStats, VocabStats } from '~/types/api'

const route = useRoute()
const auth = useAuthStore()
const { request } = useApi()
const message = useAppMessage()

const activeTab = ref('review')
const stats = ref<VocabStats | null>(null)
const checkinStats = ref<VocabCheckinStats | null>(null)
const examLevel = ref('cet4')
const settingsSaving = ref(false)
const reviewRef = ref<{ reload: () => Promise<void> } | null>(null)
const notebookRef = ref<{ reload: () => Promise<void> } | null>(null)

const notebookTabLabel = computed(() => {
  const n = stats.value?.notebookCount ?? 0
  return n > 0 ? `生词本 (${n})` : '生词本'
})

async function loadStats() {
  if (!auth.isLoggedIn) {
    stats.value = null
    checkinStats.value = null
    return
  }
  try {
    const [s, c, settings] = await Promise.all([
      request<VocabStats>('/api/v1/vocabulary/stats'),
      request<VocabCheckinStats>('/api/v1/vocabulary/checkin'),
      request<{ examLevel: string }>('/api/v1/vocabulary/settings')
    ])
    stats.value = s
    checkinStats.value = c
    examLevel.value = settings.examLevel || s.examLevel || 'cet4'
  } catch {
    stats.value = null
    checkinStats.value = null
  }
}

async function saveExamLevel(level: string) {
  settingsSaving.value = true
  try {
    await request('/api/v1/vocabulary/settings', {
      method: 'PUT',
      body: { examLevel: level }
    })
    message.success(level === 'cet6' ? '已切换至六级词书' : '已切换至四级词书')
    await reviewRef.value?.reload()
    await loadStats()
  } catch (e: unknown) {
    message.error(e instanceof Error ? e.message : '切换失败')
    await loadStats()
  } finally {
    settingsSaving.value = false
  }
}

async function onReviewRefreshed() {
  await loadStats()
}

onMounted(async () => {
  auth.hydrate()
  const tab = route.query.tab as string | undefined
  if (tab === 'notebook' || tab === 'search' || tab === 'review') {
    activeTab.value = tab
  }
  await loadStats()
})

watch(() => auth.isLoggedIn, () => loadStats())

watch(activeTab, async (tab) => {
  if (tab === 'notebook') {
    await notebookRef.value?.reload()
    await loadStats()
  }
})
</script>
