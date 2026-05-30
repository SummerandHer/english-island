<template>
  <article v-if="chapter" class="island-card p-6">
    <NuxtLink to="/translation" class="mb-4 inline-block text-sm text-[var(--island-primary)]">← 返回</NuxtLink>
    <h1 class="mb-4 text-2xl font-bold">{{ chapter.title }}</h1>
    <div class="prose max-w-none" v-html="chapter.contentHtml" />
  </article>
</template>

<script setup lang="ts">
const route = useRoute()
const { request } = useApi()
const chapter = ref<{ title: string; contentHtml: string } | null>(null)

onMounted(async () => {
  chapter.value = await request(`/api/v1/translation/chapters/${route.params.slug}`)
})
</script>
