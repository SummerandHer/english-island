<template>
  <div class="space-y-4">
    <div class="flex flex-wrap items-center gap-2">
      <NTag size="small" type="info">{{ questionDirectionLabel(question.direction) }}</NTag>
      <NTag v-if="question.mock" size="small">模拟题</NTag>
      <NTag v-if="question.vip" type="warning" size="small">VIP</NTag>
    </div>

    <p class="rounded-lg bg-green-50 p-4 text-gray-800">{{ questionPromptText(question) }}</p>

    <NInput
      v-model:value="answer"
      type="textarea"
      :rows="6"
      :placeholder="questionAnswerPlaceholder(question.direction)"
    />

    <NButton type="primary" :loading="grading" :disabled="!auth.isLoggedIn" @click="submit">
      {{ auth.isLoggedIn ? '提交批改' : '请先登录' }}
    </NButton>

    <div v-if="result" class="grading-result rounded-lg border border-green-100 bg-white p-4">
      <div class="mb-3 flex flex-wrap items-center gap-2">
        <span class="text-2xl font-bold text-[var(--island-primary)]">{{ result.cetScore }}</span>
        <span class="text-sm text-gray-500">/ 15 分（四六级）</span>
        <NTag :type="bandTagType(result.band)" size="small">
          {{ result.band }} 分档 · {{ result.bandLabel }}
        </NTag>
        <NTag v-if="result.aiModel && result.aiModel !== 'fallback'" type="success" size="small">
          AI 批改
        </NTag>
        <NTag v-else type="default" size="small">占位评分</NTag>
      </div>
      <p class="mb-2 text-xs text-gray-500">
        综合得分 {{ result.score }}/100 · {{ result.bandDescription }}
      </p>
      <p class="text-gray-700">{{ result.overallComment }}</p>

      <div v-if="answer" class="mt-4">
        <p class="mb-1 text-sm font-medium text-gray-600">你的译文（问题片段已高亮）</p>
        <p
          class="rounded-lg bg-gray-50 p-3 text-sm leading-relaxed text-gray-800"
          v-html="highlightedAnswer"
        />
      </div>

      <p v-if="result.referenceHint" class="mt-3 rounded bg-amber-50 p-3 text-sm text-amber-900">
        <strong>参考提示：</strong>{{ result.referenceHint }}
      </p>

      <div v-if="result.errors?.length" class="mt-4">
        <p class="mb-2 text-sm font-medium text-gray-600">主要扣分点（{{ result.errors.length }}）</p>
        <ul class="space-y-2 text-sm">
          <li
            v-for="(err, i) in result.errors"
            :key="i"
            class="rounded-lg border border-red-100 bg-red-50/60 p-3"
          >
            <p>
              <span class="font-medium text-red-800">{{ err.span || '—' }}</span>
              <span class="text-gray-400"> → </span>
              <span class="text-green-800">{{ err.suggestion }}</span>
            </p>
            <p class="mt-1 text-xs text-gray-500">{{ err.reason }}</p>
          </li>
        </ul>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { TranslationQuestionDetail, TranslationSubmissionResult } from '~/types/api'
import { bandTagType, highlightAnswerErrors } from '~/utils/translationGrading'
import {
  questionAnswerPlaceholder,
  questionDirectionLabel,
  questionPromptText
} from '~/utils/translationQuestion'

const props = defineProps<{
  question: TranslationQuestionDetail
}>()

const emit = defineEmits<{
  submitted: [result: TranslationSubmissionResult]
}>()

const auth = useAuthStore()
const { request } = useApi()
const message = useAppMessage()
const answer = ref('')
const grading = ref(false)
const result = ref<TranslationSubmissionResult | null>(null)

const highlightedAnswer = computed(() =>
  result.value ? highlightAnswerErrors(answer.value, result.value.errors) : ''
)

watch(() => props.question.id, () => {
  answer.value = ''
  result.value = null
})

async function submit() {
  grading.value = true
  try {
    result.value = await request<TranslationSubmissionResult>('/api/v1/translation/submissions', {
      method: 'POST',
      body: { questionId: props.question.id, userAnswer: answer.value }
    })
    emit('submitted', result.value)
  } catch (e: unknown) {
    message.error(e instanceof Error ? e.message : '批改失败')
  } finally {
    grading.value = false
  }
}
</script>

<style scoped>
:deep(.translation-error-mark) {
  background: #fef2f2;
  color: #b91c1c;
  padding: 0 2px;
  border-radius: 2px;
}
</style>
