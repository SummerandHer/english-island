<template>
  <div class="max-w-3xl space-y-4">
    <NuxtLink to="/admin/sim-exam" class="text-sm text-[var(--island-primary)]">← 仿真卷列表</NuxtLink>
    <div v-if="loading" class="text-sm text-gray-500">加载中…</div>
    <template v-else-if="detail">
      <h1 class="text-xl font-bold">{{ detail.title }}</h1>
      <div class="flex flex-wrap gap-2 text-xs text-gray-500">
        <span>{{ detail.examLevel }}</span>
        <span>{{ detail.sectionType }}</span>
        <span>status={{ detail.status }}</span>
        <span>ai={{ detail.aiStatus }}</span>
        <span v-if="detail.similarityScore != null">sim={{ detail.similarityScore }}</span>
      </div>
      <p v-if="detail.aiError" class="text-sm text-amber-700">{{ detail.aiError }}</p>
      <NButton
        v-if="detail.status !== 'published'"
        type="primary"
        :loading="publishing"
        @click="publish"
      >
        发布
      </NButton>
      <section class="island-card p-4">
        <h2 class="mb-2 text-sm font-medium">英文正文</h2>
        <p class="whitespace-pre-wrap text-sm">{{ detail.contentEn }}</p>
      </section>
      <section v-if="detail.contentZh" class="island-card p-4">
        <h2 class="mb-2 text-sm font-medium">中文翻译</h2>
        <p class="whitespace-pre-wrap text-sm text-gray-700">{{ detail.contentZh }}</p>
      </section>
      <section
        v-for="(q, idx) in detail.questions"
        :key="q.id"
        class="island-card p-4 text-sm"
      >
        <p class="font-medium">{{ idx + 1 }}. {{ q.stem }}</p>
        <p class="mt-1 text-xs text-gray-400">{{ q.skillTag }}</p>
        <ul class="mt-2 space-y-1">
          <li v-for="o in q.options" :key="o.label">{{ o.label }}. {{ o.content }}</li>
        </ul>
        <p v-if="q.locateEn" class="mt-2 text-gray-600">定位：{{ q.locateEn }}</p>
        <p v-if="q.explainCorrect" class="text-gray-600">正解：{{ q.explainCorrect }}</p>
      </section>
    </template>
  </div>
</template>

<script setup lang="ts">
definePageMeta({ layout: 'admin', middleware: 'admin', ssr: false })

const route = useRoute()
const { request } = useApi()
const message = useAppMessage()

const loading = ref(true)
const publishing = ref(false)
const detail = ref<{
  id: number
  title: string
  examLevel: string
  sectionType: string
  contentEn: string
  contentZh?: string | null
  status: string
  aiStatus: string
  aiError?: string | null
  similarityScore?: number | null
  questions: Array<{
    id: number
    stem: string
    skillTag?: string
    locateEn?: string
    explainCorrect?: string
    options: Array<{ label: string; content: string }>
  }>
} | null>(null)

onMounted(async () => {
  try {
    detail.value = await request(`/api/v1/admin/content/sim-exam/passages/${route.params.id}`)
  } catch (e: unknown) {
    message.error(e instanceof Error ? e.message : '加载失败')
  } finally {
    loading.value = false
  }
})

async function publish() {
  publishing.value = true
  try {
    await request(`/api/v1/admin/content/sim-exam/passages/${route.params.id}/publish`, {
      method: 'POST'
    })
    message.success('已发布')
    detail.value = await request(`/api/v1/admin/content/sim-exam/passages/${route.params.id}`)
  } catch (e: unknown) {
    message.error(e instanceof Error ? e.message : '发布失败')
  } finally {
    publishing.value = false
  }
}
</script>
