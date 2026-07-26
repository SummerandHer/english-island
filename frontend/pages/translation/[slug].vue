<template>
  <div v-if="chapter" class="space-y-6">
    <article class="island-card p-6">
      <NuxtLink to="/translation" class="mb-4 inline-block text-sm text-[var(--island-primary)]">← 返回</NuxtLink>
      <h1 class="mb-4 text-2xl font-bold">{{ chapter.title }}</h1>
      <div class="prose max-w-none" v-html="chapter.contentHtml" />
    </article>

    <ChapterRecommendedVideo :video="chapter.recommendedVideo" />
  </div>

  <div v-else-if="loading" class="island-card p-6 text-sm text-gray-500">加载中…</div>

  <div v-else-if="errorMessage" class="island-card p-6">
    <VipUpgradeBanner v-if="isVipError" back-to="/translation" @dismiss="navigateTo('/translation')" />
    <template v-else>
      <p class="mb-4 text-gray-700">{{ errorMessage }}</p>
      <NButton quaternary @click="loadChapter">重试</NButton>
    </template>
  </div>
</template>

<script setup lang="ts">
import type { TranslationChapterDetail } from '~/types/api'

const route = useRoute()
const auth = useAuthStore()
const { request } = useApi()

const chapter = ref<TranslationChapterDetail | null>(null)
const loading = ref(true)
const errorMessage = ref('')
const isVipError = ref(false)

onMounted(async () => {
  auth.hydrate()
  await loadChapter()
})

watch(() => auth.isLoggedIn, () => {
  if (!chapter.value && errorMessage.value.includes('VIP')) {
    loadChapter()
  }
})

async function loadChapter() {
  loading.value = true
  errorMessage.value = ''
  isVipError.value = false
  chapter.value = null

  try {
    chapter.value = await request<TranslationChapterDetail>(
      `/api/v1/translation/chapters/${route.params.slug}`
    )
  } catch (e: unknown) {
    const msg = e instanceof Error ? e.message : '加载失败'
    if (msg.includes('VIP')) {
      errorMessage.value = '该章节为 VIP 高级技巧，请升级后阅读。'
      isVipError.value = true
    } else {
      errorMessage.value = msg
    }
  } finally {
    loading.value = false
  }
}
</script>
