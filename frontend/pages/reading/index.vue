<template>
  <div>
    <h1 class="mb-4 text-xl font-bold">阅读技巧</h1>
    <p class="mb-4 text-sm text-gray-500">
      刷题请优先前往
      <NuxtLink to="/islands/exam" class="text-[var(--island-primary)]">仿真题岛</NuxtLink>
      （考点级高仿练习）；本页保留技巧章节与历史轻模拟。
    </p>
    <div class="space-y-3">
      <NuxtLink
        v-for="c in chapters"
        :key="c.id"
        :to="`/reading/${c.slug}`"
        class="island-card flex items-center justify-between p-4 transition hover:shadow-md"
      >
        <div>
          <h3 class="font-medium">{{ c.title }}</h3>
          <p class="text-sm text-gray-500">{{ c.summary }}</p>
        </div>
        <NTag v-if="c.vip" type="warning" size="small">VIP</NTag>
      </NuxtLink>
    </div>

    <div class="mt-8">
      <ReadingHistoryList />
    </div>
  </div>
</template>

<script setup lang="ts">
interface Chapter { id: number; title: string; slug: string; summary?: string; vip: boolean }

const { request } = useApi()
const chapters = ref<Chapter[]>([])

onMounted(async () => {
  chapters.value = await request<Chapter[]>('/api/v1/reading/chapters')
})
</script>
