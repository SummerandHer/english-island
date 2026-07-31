<template>
  <div class="space-y-6">
    <header>
      <h1 class="text-2xl font-bold text-gray-900">仿真题岛</h1>
      <p class="mt-2 max-w-2xl text-sm leading-relaxed text-gray-600">
        {{ hub?.complianceNote || '按四六级考点生成的高仿阅读练习。提交前为考试模式；提交后可看解析、中译与点词。' }}
      </p>
    </header>

    <div class="grid gap-4 md:grid-cols-2">
      <NuxtLink
        to="/islands/exam/short"
        class="island-card block p-6 transition hover:border-[var(--island-primary)]"
      >
        <h2 class="text-lg font-semibold">短篇阅读</h2>
        <p class="mt-2 text-sm text-gray-500">仔细阅读 · 选择题</p>
        <p class="mt-4 text-2xl font-bold text-[var(--island-primary)]">{{ hub?.shortCount ?? '—' }}</p>
        <p class="text-xs text-gray-400">套可练</p>
      </NuxtLink>
      <NuxtLink
        to="/islands/exam/long"
        class="island-card block p-6 transition hover:border-[var(--island-primary)]"
      >
        <h2 class="text-lg font-semibold">长篇阅读</h2>
        <p class="mt-2 text-sm text-gray-500">信息匹配 · 完整段落点选</p>
        <p class="mt-4 text-2xl font-bold text-[var(--island-primary)]">{{ hub?.longCount ?? 0 }}</p>
        <p class="text-xs text-gray-400">套可练</p>
      </NuxtLink>
    </div>

    <section v-if="auth.isLoggedIn" class="island-card p-4">
      <h3 class="mb-3 text-sm font-semibold text-gray-700">我的最近练习</h3>
      <div v-if="subsLoading" class="text-sm text-gray-400">加载中…</div>
      <ul v-else-if="subs.length" class="space-y-2 text-sm">
        <li v-for="s in subs" :key="s.submissionId" class="flex flex-wrap items-center justify-between gap-2">
          <NuxtLink
            :to="`/islands/exam/practice/${s.passageId}?submission=${s.submissionId}`"
            class="text-[var(--island-primary)]"
          >
            {{ s.passageTitle }}
          </NuxtLink>
          <span class="text-gray-500">{{ s.correctCount }}/{{ s.totalQuestions }} · {{ s.elapsedSeconds }}s</span>
        </li>
      </ul>
      <p v-else class="text-sm text-gray-400">暂无记录，去做一套短篇吧。</p>
    </section>
  </div>
</template>

<script setup lang="ts">
import type { SimExamHub, SimSubmissionSummary } from '~/types/api'

const { request } = useApi()
const auth = useAuthStore()

const hub = ref<SimExamHub | null>(null)
const subs = ref<SimSubmissionSummary[]>([])
const subsLoading = ref(false)

onMounted(async () => {
  auth.hydrate()
  try {
    hub.value = await request<SimExamHub>('/api/v1/sim-exam/hub')
  } catch {
    hub.value = null
  }
  if (auth.isLoggedIn) {
    subsLoading.value = true
    try {
      subs.value = await request<SimSubmissionSummary[]>('/api/v1/sim-exam/submissions/mine?limit=5')
    } catch {
      subs.value = []
    } finally {
      subsLoading.value = false
    }
  }
})
</script>
