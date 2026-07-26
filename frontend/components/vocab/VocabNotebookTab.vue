<template>
  <div>
    <div v-if="!auth.isLoggedIn" class="island-card p-6">
      <p class="mb-4">请先登录查看生词本。</p>
      <NButton type="primary" @click="navigateTo('/login')">去登录</NButton>
    </div>

    <div v-else-if="loading" class="island-card p-6 text-sm text-gray-500">加载中…</div>

    <template v-else>
      <div v-if="items.length" class="mb-4 flex flex-wrap gap-2">
        <NTag
          v-for="f in filters"
          :key="f.value"
          :type="activeFilter === f.value ? 'success' : 'default'"
          :bordered="false"
          class="cursor-pointer"
          @click="activeFilter = f.value"
        >
          {{ f.label }}
        </NTag>
      </div>

      <div v-if="filteredItems.length" class="mb-3">
        <NButton
          type="primary"
          size="small"
          :loading="addingReview"
          @click="addAllToReview"
        >
          将全部生词加入今日复习
        </NButton>
      </div>

      <ul v-if="filteredItems.length" class="space-y-2">
        <li
          v-for="item in filteredItems"
          :key="item.vocab.id"
          class="island-card flex items-start justify-between gap-3 p-4"
        >
          <div class="min-w-0 flex-1">
            <div class="flex flex-wrap items-center gap-2">
              <span class="font-medium">{{ item.vocab.word }}</span>
              <NButton quaternary size="tiny" @click="speakWord(item.vocab.word)">🔊</NButton>
              <NTag size="small">{{ item.vocab.examLevel?.toUpperCase() }}</NTag>
            </div>
            <p v-if="item.vocab.phonetic" class="text-sm text-gray-500">{{ item.vocab.phonetic }}</p>
            <p class="mt-1 text-sm text-gray-600">{{ item.vocab.meaningBrief }}</p>
            <p v-if="sourceLabel(item)" class="mt-1 text-xs text-gray-400">{{ sourceLabel(item) }}</p>
          </div>
          <NButton size="tiny" quaternary :loading="addingId === item.vocab.id" @click="addOneToReview(item.vocab.id)">
            复习
          </NButton>
        </li>
      </ul>

      <div v-else class="island-card p-6 text-sm text-gray-500">
        生词本为空。可在「今日复习」或阅读练习中点词加入。
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import type { VocabNotebookItem } from '~/types/api'
import { speakWord } from '~/utils/speech'

const auth = useAuthStore()
const { request } = useApi()
const message = useAppMessage()

const items = ref<VocabNotebookItem[]>([])
const loading = ref(false)
const addingReview = ref(false)
const addingId = ref<number | null>(null)
const activeFilter = ref('all')

const filters = [
  { label: '全部', value: 'all' },
  { label: '来自阅读', value: 'reading' },
  { label: '来自仿真题', value: 'sim_exam' },
  { label: '手动添加', value: 'manual' }
]

const filteredItems = computed(() => {
  if (activeFilter.value === 'all') return items.value
  return items.value.filter((i) => (i.sourceType || 'manual') === activeFilter.value)
})

function sourceLabel(item: VocabNotebookItem) {
  if (item.sourceType === 'reading') return '来自阅读练习'
  if (item.sourceType === 'sim_exam') return '来自仿真题'
  if (item.note) return item.note
  return ''
}

async function addOneToReview(vocabularyId: number) {
  addingId.value = vocabularyId
  try {
    await request('/api/v1/vocabulary/notebook/add-to-review', {
      method: 'POST',
      body: { vocabularyIds: [vocabularyId] }
    })
    message.success('已加入今日复习')
  } catch (e: unknown) {
    message.error(e instanceof Error ? e.message : '操作失败')
  } finally {
    addingId.value = null
  }
}

async function addAllToReview() {
  if (!filteredItems.value.length) return
  addingReview.value = true
  try {
    const res = await request<{ added: number }>('/api/v1/vocabulary/notebook/add-to-review', {
      method: 'POST',
      body: { vocabularyIds: filteredItems.value.map((i) => i.vocab.id) }
    })
    message.success(`已加入 ${res.added} 个词到今日复习`)
  } catch (e: unknown) {
    message.error(e instanceof Error ? e.message : '操作失败')
  } finally {
    addingReview.value = false
  }
}

async function load() {
  if (!auth.isLoggedIn) return
  loading.value = true
  try {
    items.value = await request<VocabNotebookItem[]>('/api/v1/vocabulary/notebook/mine')
  } catch (e: unknown) {
    message.error(e instanceof Error ? e.message : '加载失败')
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  auth.hydrate()
  await load()
})

watch(() => auth.isLoggedIn, (loggedIn) => {
  if (loggedIn) load()
  else items.value = []
})

defineExpose({ reload: load })
</script>
