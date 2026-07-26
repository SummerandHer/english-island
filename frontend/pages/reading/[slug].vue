<template>
  <article v-if="chapter" class="space-y-6">
    <NuxtLink to="/reading" class="inline-block text-sm text-[var(--island-primary)]">← 返回列表</NuxtLink>
    <div class="island-card p-6">
      <div class="mb-4 flex flex-wrap items-center justify-between gap-2">
        <h1 class="text-2xl font-bold">{{ chapter.title }}</h1>
        <NTag v-if="chapter.finished" type="success" size="small">已学完</NTag>
      </div>
      <div class="prose max-w-none" v-html="chapter.contentHtml" />
      <div v-if="auth.isLoggedIn" class="mt-6 border-t border-gray-100 pt-4">
        <NCheckbox :checked="chapter.finished" @update:checked="onFinishedChange">
          标记本章技巧已学完
        </NCheckbox>
      </div>
    </div>

    <section v-if="chapter.passages?.length" class="island-card p-6">
      <h2 class="mb-3 text-lg font-semibold">本章模拟练习</h2>
      <p class="mb-4 text-sm text-gray-500">学完技巧后完成下列篇章练习（自编模拟题）。</p>
      <div class="space-y-3">
        <NuxtLink
          v-for="p in chapter.passages"
          :key="p.id"
          :to="`/reading/practice/${p.id}?chapter=${chapter.slug}`"
          class="flex items-center justify-between rounded-lg border border-gray-100 p-4 transition hover:border-[var(--island-primary)] hover:bg-green-50/30"
        >
          <div>
            <h3 class="font-medium">{{ p.title }}</h3>
            <p class="text-sm text-gray-500">
              {{ p.questionCount }} 道选择题
              <span v-if="p.wordCount"> · 约 {{ p.wordCount }} 词</span>
              <span v-if="lastScores[p.id]" class="text-[var(--island-primary)]">
                · 上次 {{ lastScores[p.id].correctCount }}/{{ lastScores[p.id].totalQuestions }}
              </span>
            </p>
          </div>
          <NTag size="small" type="info">去练习</NTag>
        </NuxtLink>
      </div>
    </section>

    <ChapterRecommendedVideo :video="chapter.recommendedVideo" />
  </article>

  <div v-else-if="loading" class="island-card p-6 text-sm text-gray-500">加载中…</div>

  <div v-else-if="errorMessage" class="island-card p-6">
    <VipUpgradeBanner v-if="isVipError" back-to="/reading" @dismiss="navigateTo('/reading')" />
    <template v-else>
      <p class="mb-4 text-gray-700">{{ errorMessage }}</p>
      <NButton quaternary @click="loadChapter">重试</NButton>
    </template>
  </div>
</template>

<script setup lang="ts">
import type { ReadingChapterDetail, ReadingPassageLastScore } from '~/types/api'

const route = useRoute()
const auth = useAuthStore()
const slug = computed(() => String(route.params.slug))
const { request } = useApi()

const chapter = ref<ReadingChapterDetail | null>(null)
const loading = ref(true)
const errorMessage = ref('')
const isVipError = ref(false)
const lastScores = ref<Record<number, ReadingPassageLastScore>>({})

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
    chapter.value = await request<ReadingChapterDetail>(
      `/api/v1/reading/chapters/${slug.value}`
    )
    await loadLastScores()
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

async function loadLastScores() {
  if (!auth.isLoggedIn || !chapter.value?.passages?.length) {
    lastScores.value = {}
    return
  }
  const ids = chapter.value.passages.map((p) => p.id)
  try {
    const params = ids.map((id) => `passageIds=${id}`).join('&')
    const raw = await request<Record<string, ReadingPassageLastScore>>(
      `/api/v1/reading/submissions/last-scores?${params}`
    )
    lastScores.value = Object.fromEntries(
      Object.entries(raw).map(([k, v]) => [Number(k), v])
    )
  } catch {
    lastScores.value = {}
  }
}

async function onFinishedChange(checked: boolean) {
  if (!chapter.value) {
    return
  }
  try {
    await request(`/api/v1/reading/chapters/${slug.value}/progress`, {
      method: 'POST',
      body: { finished: checked }
    })
    chapter.value = { ...chapter.value, finished: checked }
  } catch (e: unknown) {
    window.alert(e instanceof Error ? e.message : '保存失败')
  }
}
</script>
