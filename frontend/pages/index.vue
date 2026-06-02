<template>
  <div>
    <section class="hero-banner island-card">
      <div class="hero-content">
        <p class="hero-eyebrow">ISLAND · 四六级岛</p>
        <h1 class="hero-title">系统化备考，轻松过四六级</h1>
        <p class="hero-desc">阅读技巧 · 翻译练习 · 双语精听 · 学习社区，一站式大学生备考平台</p>
        <div class="hero-actions">
          <NButton type="primary" size="large" @click="navigateTo('/reading')">开始学习</NButton>
          <NButton v-if="!auth.isLoggedIn" size="large" quaternary @click="navigateTo('/register')">
            免费注册
          </NButton>
        </div>
      </div>
      <div class="hero-stats hidden lg:grid">
        <div v-for="stat in stats" :key="stat.label" class="stat-item">
          <strong>{{ stat.value }}</strong>
          <span>{{ stat.label }}</span>
        </div>
      </div>
    </section>

    <section v-if="auth.isLoggedIn" class="island-card mb-8 mt-8 p-6">
      <h2 class="section-title mb-1">今日学习</h2>
      <p class="section-desc mb-4">建议按顺序完成下列任务（约 30 分钟）</p>
      <div v-if="todayLoading" class="text-sm text-gray-500">加载今日任务…</div>
      <div v-else class="grid gap-3 sm:grid-cols-3">
        <NuxtLink
          to="/vocabulary"
          class="rounded-lg border border-gray-100 p-4 transition hover:border-[var(--island-primary)] hover:bg-green-50/30"
        >
          <p class="text-xs text-gray-500">每日词汇</p>
          <p class="mt-1 font-medium">{{ todayVocabLabel }}</p>
        </NuxtLink>
        <NuxtLink
          v-if="todayReadingLink"
          :to="todayReadingLink"
          class="rounded-lg border border-gray-100 p-4 transition hover:border-[var(--island-primary)] hover:bg-green-50/30"
        >
          <p class="text-xs text-gray-500">阅读模拟</p>
          <p class="mt-1 font-medium line-clamp-2">{{ todayReadingTitle }}</p>
        </NuxtLink>
        <NuxtLink
          v-if="todayTranslationId"
          :to="`/translation/practice/${todayTranslationId}`"
          class="rounded-lg border border-gray-100 p-4 transition hover:border-[var(--island-primary)] hover:bg-green-50/30"
        >
          <p class="text-xs text-gray-500">翻译练习</p>
          <p class="mt-1 font-medium line-clamp-2">{{ todayTranslationPreview }}</p>
        </NuxtLink>
      </div>
    </section>

    <section class="mb-2 mt-8">
      <h2 class="section-title">学习模块</h2>
      <p class="section-desc">按模块循序渐进，覆盖四六级核心能力</p>
    </section>

    <div class="module-grid">
      <NuxtLink v-for="m in modules" :key="m.to" :to="m.to" class="module-card island-card">
        <div class="module-icon" :class="m.tone">{{ m.icon }}</div>
        <div class="module-body">
          <h3>{{ m.title }}</h3>
          <p>{{ m.desc }}</p>
        </div>
        <span class="module-arrow">→</span>
      </NuxtLink>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { ReadingChapterDetail, TranslationQuestionSummary, VocabTodayPlan } from '~/types/api'
import { questionPromptPreview } from '~/utils/translationQuestion'

const auth = useAuthStore()
const { request } = useApi()

const todayLoading = ref(false)
const todayVocabLabel = ref('今日 20 词')
const todayReadingLink = ref<string | null>(null)
const todayReadingTitle = ref('阅读模拟练习')
const todayTranslationId = ref<number | null>(null)
const todayTranslationPreview = ref('翻译模拟题')

onMounted(async () => {
  auth.hydrate()
  if (auth.isLoggedIn) {
    await loadTodayTasks()
  }
})

watch(() => auth.isLoggedIn, async (loggedIn) => {
  if (loggedIn) {
    await loadTodayTasks()
  }
})

async function loadTodayTasks() {
  todayLoading.value = true
  try {
    const [vocab, reading, questions] = await Promise.all([
      request<VocabTodayPlan>('/api/v1/vocabulary/today'),
      request<ReadingChapterDetail>('/api/v1/reading/chapters/skim-questions-first'),
      request<TranslationQuestionSummary[]>('/api/v1/translation/questions')
    ])
    todayVocabLabel.value =
      vocab.dueCount > 0 ? `待复习 ${vocab.dueCount} 词` : '今日词汇已完成'
    const passage = reading.passages?.[0]
    if (passage) {
      todayReadingLink.value = `/reading/practice/${passage.id}?chapter=skim-questions-first`
      todayReadingTitle.value = passage.title
    }
    const q = questions.find((item) => !item.vip) ?? questions[0]
    if (q) {
      todayTranslationId.value = q.id
      todayTranslationPreview.value = questionPromptPreview(q)
    }
  } catch {
    // 首页任务非关键路径，失败时保留默认文案
  } finally {
    todayLoading.value = false
  }
}

const stats = [
  { value: '4+', label: '核心模块' },
  { value: 'AI', label: '翻译批改' },
  { value: '逐句', label: '双语精听' }
]

const modules = [
  { to: '/reading', icon: '📖', title: '阅读技巧', desc: '章节式方法论 + 模拟阅读练习', tone: 'tone-green' },
  { to: '/vocabulary', icon: '📝', title: '每日词汇', desc: '今日 20 词，认识/模糊/不认识复习', tone: 'tone-teal' },
  { to: '/translation', icon: '✍️', title: '翻译技巧', desc: '技巧学习 + 模拟练习 + AI 批改', tone: 'tone-blue' },
  { to: '/video', icon: '🎬', title: '双语视频', desc: '逐句同步精听，提升听力与口语', tone: 'tone-purple' },
  { to: '/exam-guide', icon: '📋', title: '备考导航', desc: '官方链接 + 站内学习路径', tone: 'tone-orange' },
  { to: '/feed', icon: '🏝️', title: '全平台', desc: '学习心得交流，与同路人一起进步', tone: 'tone-orange' }
]
</script>

<style scoped>
.hero-banner {
  @apply relative overflow-hidden p-8 lg:flex lg:items-center lg:justify-between lg:p-10;
  background: linear-gradient(135deg, #ffffff 0%, #f0faf4 50%, #eef5ff 100%);
}

.hero-banner::before {
  content: '';
  @apply pointer-events-none absolute -right-16 -top-16 h-48 w-48 rounded-full bg-[#18a058]/5;
}

.hero-eyebrow {
  @apply mb-3 text-sm font-medium text-[var(--island-primary)];
}

.hero-title {
  @apply text-2xl font-bold text-gray-800 lg:text-3xl;
}

.hero-desc {
  @apply mt-3 max-w-xl text-sm leading-relaxed text-gray-500 lg:text-base;
}

.hero-actions {
  @apply mt-6 flex flex-wrap gap-3;
}

.hero-stats {
  @apply shrink-0 grid-cols-1 gap-4;
}

.stat-item {
  @apply rounded-xl border border-white/80 bg-white/70 px-5 py-4 text-center shadow-sm;
}

.stat-item strong {
  @apply block text-xl font-bold text-[var(--island-primary)];
}

.stat-item span {
  @apply mt-1 block text-xs text-gray-500;
}

.section-title {
  @apply text-lg font-semibold text-gray-800;
}

.section-desc {
  @apply mt-1 text-sm text-gray-500;
}

.module-grid {
  @apply mt-4 grid gap-4 sm:grid-cols-2;
}

.module-card {
  @apply flex items-start gap-4 p-5 transition hover:-translate-y-0.5 hover:shadow-md;
}

.module-icon {
  @apply flex h-12 w-12 shrink-0 items-center justify-center rounded-xl text-xl;
}

.tone-green {
  @apply bg-[#e8f7ef] text-green-700;
}

.tone-teal {
  @apply bg-[#e6f7f5] text-teal-700;
}

.tone-blue {
  @apply bg-[#eef4ff] text-blue-700;
}

.tone-purple {
  @apply bg-[#f3eeff] text-purple-700;
}

.tone-orange {
  @apply bg-[#fff4ea] text-orange-700;
}

.module-body h3 {
  @apply font-semibold text-gray-800;
}

.module-card:hover .module-body h3 {
  @apply text-[var(--island-primary)];
}

.module-body p {
  @apply mt-1 text-sm leading-relaxed text-gray-500;
}

.module-arrow {
  @apply ml-auto self-center text-gray-300 transition;
}

.module-card:hover .module-arrow {
  @apply text-[var(--island-primary)];
}
</style>
