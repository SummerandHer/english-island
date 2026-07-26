<template>
  <div class="lg:grid lg:grid-cols-2 lg:gap-6 lg:items-start">
    <div class="space-y-4 lg:sticky lg:top-20">
      <div class="flex flex-wrap items-center gap-2">
        <NTag size="small" type="info">{{ passage.difficulty.toUpperCase() }}</NTag>
        <NTag v-if="passage.mock" size="small">模拟题</NTag>
        <NTag v-if="passage.wordCount" size="small" quaternary>约 {{ passage.wordCount }} 词</NTag>
        <span class="text-xs text-gray-400">点击单词可查义并加入生词本</span>
      </div>

      <section class="rounded-lg border border-gray-100 bg-gray-50/80 p-4">
        <h2 class="mb-3 text-sm font-medium text-gray-600">阅读材料</h2>
        <SelectablePassageText
          :content="passage.contentEn"
          :passage-id="passage.id"
          :passage-title="passage.title"
        />
      </section>

      <LongSentencePanel v-if="passage.longSentences?.length" :sentences="passage.longSentences" />
    </div>

    <div class="mt-6 space-y-6 lg:mt-0">
      <section
        v-for="(q, idx) in passage.questions"
        :key="q.id"
        class="rounded-lg border border-gray-100 p-4"
      >
        <p class="mb-3 font-medium text-gray-800">
          {{ idx + 1 }}. {{ q.stem }}
        </p>
        <NRadioGroup
          :value="answers[q.id] ?? null"
          @update:value="(v: string) => setAnswer(q.id, v)"
        >
          <NSpace vertical>
            <NRadio
              v-for="opt in q.options"
              :key="opt.label"
              :value="opt.label"
              :class="resultOptionClass(q.id, opt.label)"
            >
              <span class="font-medium">{{ opt.label }}.</span> {{ opt.content }}
            </NRadio>
          </NSpace>
        </NRadioGroup>
        <p
          v-if="resultFor(q.id)"
          class="mt-3 rounded-lg p-3 text-sm"
          :class="resultFor(q.id)?.correct ? 'bg-green-50 text-green-900' : 'bg-red-50 text-red-900'"
        >
          <span class="font-medium">{{ resultFor(q.id)?.correct ? '正确' : '错误' }}</span>
          <span v-if="!resultFor(q.id)?.correct && resultFor(q.id)?.correctLabel">
            · 正确答案 {{ resultFor(q.id)?.correctLabel }}
          </span>
          <span v-if="resultFor(q.id)?.explanation" class="mt-1 block text-gray-600">
            {{ resultFor(q.id)?.explanation }}
          </span>
        </p>
      </section>

      <div class="flex flex-wrap items-center gap-3">
        <NButton
          type="primary"
          :loading="submitting"
          :disabled="!canSubmit"
          @click="submit"
        >
          {{ auth.isLoggedIn ? '提交答案' : '请先登录后提交' }}
        </NButton>
        <span v-if="result" class="text-sm text-gray-600">
          得分 {{ result.correctCount }} / {{ result.totalQuestions }}
        </span>
      </div>

      <div
        v-if="result && chapterSlug"
        class="rounded-lg border border-green-100 bg-green-50/40 p-4 text-sm"
      >
        <p class="text-gray-700">有错题？回到技巧章复习解题方法。</p>
        <NuxtLink :to="`/reading/${chapterSlug}`" class="mt-2 inline-block text-[var(--island-primary)] hover:underline">
          复习本章技巧 →
        </NuxtLink>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { ReadingPassagePractice, ReadingSubmitResult } from '~/types/api'

const props = defineProps<{
  passage: ReadingPassagePractice
  chapterSlug?: string
  timedMode?: boolean
}>()

const emit = defineEmits<{
  submitted: [result: ReadingSubmitResult]
}>()

const auth = useAuthStore()
const { request } = useApi()
const message = useAppMessage()

const answers = ref<Record<number, string>>({})
const submitting = ref(false)
const result = ref<ReadingSubmitResult | null>(null)

const allAnswered = computed(() =>
  props.passage.questions.every((q) => !!answers.value[q.id])
)

const canSubmit = computed(
  () => auth.isLoggedIn && allAnswered.value && !submitting.value
)

function setAnswer(questionId: number, label: string) {
  if (result.value) {
    result.value = null
  }
  answers.value = { ...answers.value, [questionId]: label }
}

function resultFor(questionId: number) {
  return result.value?.questions.find((q) => q.questionId === questionId) ?? null
}

function resultOptionClass(questionId: number, label: string) {
  const r = resultFor(questionId)
  if (!r) {
    return ''
  }
  if (label === r.correctLabel) {
    return 'text-green-700'
  }
  if (label === r.userLabel && !r.correct) {
    return 'text-red-700'
  }
  return ''
}

async function submit() {
  if (!auth.isLoggedIn) {
    navigateTo('/login')
    return
  }
  if (!allAnswered.value) {
    return
  }

  submitting.value = true
  try {
    const payload = {
      answers: props.passage.questions.map((q) => ({
        questionId: q.id,
        label: answers.value[q.id]
      }))
    }
    result.value = await request<ReadingSubmitResult>(
      `/api/v1/reading/passages/${props.passage.id}/submit`,
      { method: 'POST', body: payload }
    )
    emit('submitted', result.value)
    if (result.value.correctCount < result.value.totalQuestions) {
      message.info(`得分 ${result.value.correctCount}/${result.value.totalQuestions}，可查看解析后重练`)
    } else {
      message.success('全部正确！')
    }
  } catch (e: unknown) {
    message.error(e instanceof Error ? e.message : '提交失败')
  } finally {
    submitting.value = false
  }
}

defineExpose({
  submit,
  tryAutoSubmit: async () => {
    if (canSubmit.value) {
      await submit()
      return true
    }
    return false
  }
})
</script>
