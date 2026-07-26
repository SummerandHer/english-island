<template>
  <NModal v-model:show="visible" preset="card" :title="word || '查词'" class="max-w-md">
    <div v-if="loading" class="py-4 text-sm text-gray-500">查询中…</div>
    <div v-else-if="error" class="py-4 text-sm text-gray-600">{{ error }}</div>
    <div v-else-if="detail">
      <div class="mb-2 flex items-center gap-2">
        <span class="text-xl font-bold">{{ detail.summary.word }}</span>
        <NButton quaternary size="small" @click="speakWord(detail.summary.word)">🔊</NButton>
        <NTag size="small">{{ detail.summary.examLevel?.toUpperCase() }}</NTag>
      </div>
      <p v-if="detail.summary.phonetic" class="mb-2 text-sm text-gray-500">{{ detail.summary.phonetic }}</p>
      <p class="text-gray-800">{{ detail.meaningZh }}</p>
      <p v-if="detail.exampleEn" class="mt-3 text-sm text-gray-600">{{ detail.exampleEn }}</p>
      <div class="mt-4 flex gap-2">
        <NButton v-if="auth.isLoggedIn" type="primary" :loading="adding" @click="addToNotebook">
          加入生词本
        </NButton>
        <NButton v-else @click="navigateTo('/login')">登录后收藏</NButton>
      </div>
    </div>
  </NModal>
</template>

<script setup lang="ts">
import type { VocabDetail } from '~/types/api'
import { speakWord } from '~/utils/speech'

const props = defineProps<{
  word: string
  passageId?: number
  passageTitle?: string
  sourceType?: string
}>()

const emit = defineEmits<{
  added: []
}>()

const visible = defineModel<boolean>('show', { default: false })

const auth = useAuthStore()
const { request } = useApi()
const message = useAppMessage()

const loading = ref(false)
const adding = ref(false)
const error = ref('')
const detail = ref<VocabDetail | null>(null)

watch([visible, () => props.word], async ([show, word]) => {
  if (!show || !word) {
    detail.value = null
    error.value = ''
    return
  }
  loading.value = true
  error.value = ''
  try {
    detail.value = await request<VocabDetail>(`/api/v1/vocabulary/lookup?word=${encodeURIComponent(word)}`)
  } catch (e: unknown) {
    detail.value = null
    error.value = e instanceof Error ? e.message : '词库中未找到该词'
  } finally {
    loading.value = false
  }
})

async function addToNotebook() {
  if (!detail.value) return
  adding.value = true
  try {
    await request('/api/v1/vocabulary/notebook', {
      method: 'POST',
      body: {
        vocabularyId: detail.value.summary.id,
        sourceType: props.sourceType || (props.passageId ? 'reading' : 'manual'),
        sourceId: props.passageId,
        note: props.passageTitle ? `来自《${props.passageTitle}》` : undefined
      }
    })
    message.success('已加入生词本')
    emit('added')
    visible.value = false
  } catch (e: unknown) {
    message.error(e instanceof Error ? e.message : '收藏失败')
  } finally {
    adding.value = false
  }
}
</script>
