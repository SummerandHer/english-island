<template>
  <article v-if="chapter" class="island-card p-6">
    <NuxtLink to="/reading" class="mb-4 inline-block text-sm text-[var(--island-primary)]">← 返回列表</NuxtLink>
    <h1 class="mb-4 text-2xl font-bold">{{ chapter.title }}</h1>
    <div class="prose max-w-none" v-html="chapter.contentHtml" />
  </article>
</template>

<script setup lang="ts">
interface ChapterDetail {
  id: number
  title: string
  slug: string
  contentHtml: string
}

const route = useRoute()
const { request } = useApi()
const chapter = ref<ChapterDetail | null>(null)

onMounted(async () => {
  try {
    chapter.value = await request<ChapterDetail>(`/api/v1/reading/chapters/${route.params.slug}`)
  } catch (e: unknown) {
    alert(e instanceof Error ? e.message : '加载失败')
  }
})
</script>
