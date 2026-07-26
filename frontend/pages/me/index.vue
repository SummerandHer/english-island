<template>

  <div>

    <h1 class="mb-2 text-xl font-bold">我的</h1>

    <p class="mb-6 text-sm text-gray-500">学习记录与收藏</p>

    <div v-if="auth.isAdmin" class="island-card mb-6 flex flex-wrap items-center justify-between gap-3 p-4">
      <div>
        <p class="text-sm font-medium text-gray-800">管理后台</p>
        <p class="text-xs text-gray-500">录入日报、阅读/翻译题、视频等</p>
      </div>
      <NuxtLink
        to="/admin/daily"
        class="rounded-full bg-[var(--island-forest)] px-4 py-2 text-sm font-semibold text-white no-underline"
      >
        打开日报管理
      </NuxtLink>
    </div>

    <div v-if="auth.isLoggedIn" class="mb-6">

      <div v-if="summaryLoading" class="island-card p-4 text-sm text-gray-500">加载学习概览…</div>

      <div v-else-if="summary" class="island-card space-y-4 p-4">

        <div class="flex flex-wrap items-center justify-between gap-2">

          <div>

            <p class="text-sm font-medium text-gray-800">连续打卡 {{ summary.streakDays }} 天</p>

            <p class="text-xs text-gray-500">完成今日复习后自动打卡</p>

          </div>

          <NuxtLink to="/vocabulary" class="text-sm text-[var(--island-primary)]">去词汇 →</NuxtLink>

        </div>



        <div>

          <div class="mb-1 flex justify-between text-xs text-gray-500">

            <span>今日词汇</span>

            <span>{{ summary.vocab.todayDone }}/{{ summary.vocab.dailyLimit }}</span>

          </div>

          <NProgress

            type="line"

            :percentage="vocabProgressPercent"

            :show-indicator="false"

            color="#18a058"

            rail-color="#e8f7ef"

          />

          <p v-if="summary.vocab.dueCount > 0" class="mt-1 text-xs text-amber-700">

            待复习 {{ summary.vocab.dueCount }} 词

          </p>

        </div>



        <div class="grid gap-3 sm:grid-cols-2">

          <div

            v-if="summary.lastReading"

            class="rounded-lg border border-gray-100 bg-gray-50/80 px-3 py-2 text-sm"

          >

            <p class="text-xs text-gray-500">最近阅读</p>

            <p class="mt-0.5 font-medium line-clamp-1">{{ summary.lastReading.passageTitle }}</p>

            <p class="text-[var(--island-primary)]">

              {{ summary.lastReading.correctCount }}/{{ summary.lastReading.totalQuestions }}

            </p>

          </div>

          <div

            v-if="summary.lastTranslation"

            class="rounded-lg border border-gray-100 bg-gray-50/80 px-3 py-2 text-sm"

          >

            <p class="text-xs text-gray-500">最近翻译</p>

            <p class="mt-0.5 font-medium line-clamp-1">{{ summary.lastTranslation.promptPreview }}</p>

            <p class="text-[var(--island-primary)]">{{ summary.lastTranslation.cetScore }} / 15 分</p>

          </div>

        </div>



        <div v-if="summary.suggestions.length" class="border-t border-gray-100 pt-3">

          <p class="mb-2 text-xs font-medium text-gray-500">建议下一步</p>

          <div class="flex flex-wrap gap-2">

            <NuxtLink

              v-for="(s, i) in summary.suggestions.slice(0, 3)"

              :key="i"

              :to="s.href"

              class="rounded-full border border-[var(--island-primary)]/30 bg-green-50/50 px-3 py-1 text-xs text-[var(--island-primary)] transition hover:bg-green-50"

            >

              {{ s.action }} · {{ s.label }}

            </NuxtLink>

          </div>

        </div>

      </div>

    </div>



    <div class="space-y-3">

      <NuxtLink

        to="/vocabulary?tab=notebook"

        class="island-card flex items-center justify-between p-4 transition hover:shadow-md"

      >

        <div>

          <h3 class="font-medium">生词本</h3>

          <p class="text-sm text-gray-500">阅读点词与手动收藏</p>

        </div>

        <span v-if="notebookCount != null" class="text-sm text-[var(--island-primary)]">

          {{ notebookCount }} 词

        </span>

      </NuxtLink>



      <NuxtLink

        to="/reading"

        class="island-card flex items-center justify-between p-4 transition hover:shadow-md"

      >

        <div>

          <h3 class="font-medium">阅读练习记录</h3>

          <p class="text-sm text-gray-500">模拟题得分与错题</p>

        </div>

        <span class="text-gray-300">→</span>

      </NuxtLink>



      <NuxtLink

        to="/translation"

        class="island-card flex items-center justify-between p-4 transition hover:shadow-md"

      >

        <div>

          <h3 class="font-medium">翻译练习记录</h3>

          <p class="text-sm text-gray-500">AI 批改历史</p>

        </div>

        <span class="text-gray-300">→</span>

      </NuxtLink>



      <NuxtLink

        to="/vocabulary"

        class="island-card flex items-center justify-between p-4 transition hover:shadow-md"

      >

        <div>

          <h3 class="font-medium">今日词汇</h3>

          <p v-if="vocabLine" class="text-sm text-gray-500">{{ vocabLine }}</p>

          <p v-else class="text-sm text-gray-500">登录后查看进度</p>

        </div>

        <span class="text-gray-300">→</span>

      </NuxtLink>

    </div>

  </div>

</template>



<script setup lang="ts">

const auth = useAuthStore()

const { summary, loading: summaryLoading, load } = useLearnSummary()



const vocabProgressPercent = computed(() => {

  const v = summary.value?.vocab

  if (!v || v.dailyLimit <= 0) return 0

  return Math.min(100, Math.round((v.todayDone / v.dailyLimit) * 100))

})



const notebookCount = computed(() => summary.value?.vocab.notebookCount ?? null)



const vocabLine = computed(() => {

  const v = summary.value?.vocab

  if (!v) return ''

  return `今日 ${v.todayDone}/${v.dailyLimit} · 已掌握 ${v.masteredCount}`

})



onMounted(async () => {

  auth.hydrate()

  if (auth.isLoggedIn) {

    await load()

  }

})



watch(

  () => auth.isLoggedIn,

  async (loggedIn) => {

    if (loggedIn) {

      await load()

    } else {

      summary.value = null

    }

  }

)

</script>

