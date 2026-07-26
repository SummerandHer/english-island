<template>
  <div>
    <NuxtLink
      v-if="backLink"
      :to="backLink"
      class="mb-4 inline-block text-sm text-[var(--island-primary)]"
    >
      ← 返回章节
    </NuxtLink>
    <NuxtLink v-else to="/reading" class="mb-4 inline-block text-sm text-[var(--island-primary)]">
      ← 返回阅读技巧
    </NuxtLink>

    <div v-if="loading" class="island-card p-6 text-sm text-gray-500">加载练习中…</div>

    <div v-else-if="errorMessage" class="island-card p-6">
      <p class="mb-4 text-gray-700">{{ errorMessage }}</p>
      <NButton quaternary @click="loadPassage">重试</NButton>
    </div>

    <template v-else-if="passage">
      <header class="mb-4 flex flex-wrap items-start justify-between gap-3">
        <div>
          <h1 class="text-xl font-bold">{{ passage.title }}</h1>
          <p class="mt-1 text-sm text-gray-500">阅读模拟 · 共 {{ passage.questions.length }} 题</p>
        </div>
        <div class="flex flex-wrap items-center gap-2">
          <NButton
            size="small"
            :type="timedMode ? 'warning' : 'default'"
            @click="toggleTimedMode"
          >
            {{ timedMode ? '退出限时' : '限时 18 分钟' }}
          </NButton>
          <span
            v-if="timedMode"
            class="rounded-lg px-3 py-1 text-sm font-medium"
            :class="timer.isLow ? 'bg-red-100 text-red-700' : 'bg-amber-100 text-amber-800'"
          >
            ⏱ {{ timer.formatted }}
          </span>
        </div>
      </header>

      <section class="island-card p-6">
        <ReadingPracticePanel
          ref="panelRef"
          :passage="passage"
          :chapter-slug="(route.query.chapter as string) || undefined"
          :timed-mode="timedMode"
          @submitted="onSubmitted"
        />
      </section>
    </template>
  </div>
</template>

<script setup lang="ts">
import type { ReadingPassagePractice, ReadingSubmitResult } from '~/types/api'

const route = useRoute()
const { request } = useApi()
const message = useAppMessage()

const passageId = computed(() => Number(route.params.passageId))
const passage = ref<ReadingPassagePractice | null>(null)
const loading = ref(true)
const errorMessage = ref('')
const backLink = ref<string | null>(null)
const timedMode = ref(false)
const panelRef = ref<{ tryAutoSubmit: () => Promise<boolean> } | null>(null)

const timer = useReadingTimer(async () => {
  message.warning('时间到，正在自动提交…')
  const submitted = await panelRef.value?.tryAutoSubmit()
  if (!submitted) {
    message.info('尚有题目未作答，请尽快完成')
    timer.start()
  }
})

onMounted(() => {
  const chapterSlug = route.query.chapter as string | undefined
  if (chapterSlug) {
    backLink.value = `/reading/${chapterSlug}`
  }
  loadPassage()
})

onUnmounted(() => {
  timer.stop()
})

function toggleTimedMode() {
  if (timedMode.value) {
    timedMode.value = false
    timer.stop()
    return
  }
  timedMode.value = true
  timer.start()
}

async function loadPassage() {
  if (!Number.isFinite(passageId.value) || passageId.value <= 0) {
    errorMessage.value = '篇章 ID 无效'
    loading.value = false
    return
  }

  loading.value = true
  errorMessage.value = ''
  passage.value = null

  try {
    passage.value = await request<ReadingPassagePractice>(
      `/api/v1/reading/passages/${passageId.value}`
    )
  } catch (e: unknown) {
    errorMessage.value = e instanceof Error ? e.message : '加载失败'
  } finally {
    loading.value = false
  }
}

function onSubmitted(_result: ReadingSubmitResult) {
  timer.stop()
  timedMode.value = false
}
</script>
