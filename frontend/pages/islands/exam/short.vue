<template>
  <div class="space-y-4">
    <NuxtLink to="/islands/exam" class="text-sm text-[var(--island-primary)]">← 仿真题岛</NuxtLink>
    <header class="flex flex-wrap items-end justify-between gap-3">
      <div>
        <h1 class="text-xl font-bold">短篇阅读</h1>
        <p class="mt-1 text-sm text-gray-500">仔细阅读选择题 · 考试模式提交前不可点词</p>
      </div>
      <NSelect
        v-model:value="examLevel"
        :options="levelOptions"
        clearable
        placeholder="级别"
        style="width: 120px"
        @update:value="load"
      />
    </header>

    <p class="text-xs text-gray-400">
      本站练习为高仿仿真题，非历年原题；原题请以中国教育考试网为准。
    </p>

    <div v-if="loading" class="island-card p-6 text-sm text-gray-500">加载中…</div>
    <div v-else-if="!rows.length" class="island-card p-6 text-sm text-gray-500">暂无短篇，请稍后再来。</div>
    <div v-else class="grid gap-3">
      <NuxtLink
        v-for="p in rows"
        :key="p.id"
        :to="`/islands/exam/practice/${p.id}`"
        class="island-card block p-4 transition hover:border-[var(--island-primary)]"
      >
        <div class="flex flex-wrap items-start justify-between gap-2">
          <h2 class="font-semibold text-gray-900">{{ p.title }}</h2>
          <span class="rounded bg-green-50 px-2 py-0.5 text-xs text-[var(--island-primary)]">
            {{ p.examLevel?.toUpperCase() }}
          </span>
        </div>
        <div class="mt-2 flex flex-wrap gap-3 text-xs text-gray-500">
          <span v-if="p.wordCount">约 {{ p.wordCount }} 词</span>
          <span v-if="p.vocabCount">精练词 {{ p.vocabCount }}</span>
          <span v-if="p.recommendedMinutes">推荐 {{ p.recommendedMinutes }} 分钟</span>
          <span>{{ p.questionCount }} 题</span>
        </div>
      </NuxtLink>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { SimPassageCard } from '~/types/api'

const { request } = useApi()
const rows = ref<SimPassageCard[]>([])
const loading = ref(true)
const examLevel = ref<string | null>(null)

const levelOptions = [
  { label: 'CET4', value: 'cet4' },
  { label: 'CET6', value: 'cet6' }
]

onMounted(load)

async function load() {
  loading.value = true
  try {
    const q = new URLSearchParams({ sectionType: 'short_careful' })
    if (examLevel.value) q.set('examLevel', examLevel.value)
    rows.value = await request<SimPassageCard[]>(`/api/v1/sim-exam/passages?${q}`)
  } catch {
    rows.value = []
  } finally {
    loading.value = false
  }
}
</script>
