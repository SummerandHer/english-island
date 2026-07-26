<template>
  <section class="island-card p-6">
    <h2 class="mb-3 text-sm font-medium text-gray-600">查词（前缀搜索）</h2>
    <NInput v-model:value="searchQ" placeholder="输入单词开头…" clearable @update:value="onSearch" />
    <ul v-if="searchResults.length" class="mt-3 space-y-2">
      <li
        v-for="item in searchResults"
        :key="item.id"
        class="flex cursor-pointer items-center justify-between rounded-lg border border-gray-100 px-3 py-2 text-sm hover:bg-green-50/40"
        @click="openItem(item.id)"
      >
        <span class="font-medium">{{ item.word }}</span>
        <span class="truncate pl-2 text-gray-500">{{ item.meaningBrief }}</span>
      </li>
    </ul>

    <div v-if="detail" class="mt-4 rounded-lg border border-green-100 bg-green-50/30 p-4">
      <div class="mb-2 flex items-center gap-2">
        <span class="text-xl font-bold">{{ detail.summary.word }}</span>
        <NButton quaternary size="small" @click="speakWord(detail.summary.word)">🔊</NButton>
      </div>
      <p class="text-sm text-gray-600">{{ detail.meaningZh }}</p>
      <p v-if="detail.exampleEn" class="mt-2 text-sm text-gray-700">{{ detail.exampleEn }}</p>
      <NButton v-if="auth.isLoggedIn" class="mt-3" size="small" @click="addNotebook">加入生词本</NButton>
    </div>
  </section>
</template>

<script setup lang="ts">
import type { VocabDetail, VocabListItem } from '~/types/api'
import { speakWord } from '~/utils/speech'

const auth = useAuthStore()
const { request } = useApi()
const message = useAppMessage()

const searchQ = ref('')
const searchResults = ref<VocabListItem[]>([])
const detail = ref<VocabDetail | null>(null)
let searchTimer: ReturnType<typeof setTimeout> | null = null

function onSearch() {
  if (searchTimer) clearTimeout(searchTimer)
  searchTimer = setTimeout(async () => {
    const q = searchQ.value.trim()
    detail.value = null
    if (q.length < 1) {
      searchResults.value = []
      return
    }
    try {
      searchResults.value = await request<VocabListItem[]>(`/api/v1/vocabulary/search?q=${encodeURIComponent(q)}`)
    } catch {
      searchResults.value = []
    }
  }, 200)
}

async function openItem(id: number) {
  try {
    detail.value = await request<VocabDetail>(`/api/v1/vocabulary/${id}`)
  } catch (e: unknown) {
    message.error(e instanceof Error ? e.message : '加载失败')
  }
}

async function addNotebook() {
  if (!detail.value) return
  try {
    await request('/api/v1/vocabulary/notebook', {
      method: 'POST',
      body: { vocabularyId: detail.value.summary.id }
    })
    message.success('已加入生词本')
  } catch (e: unknown) {
    message.error(e instanceof Error ? e.message : '操作失败')
  }
}
</script>
