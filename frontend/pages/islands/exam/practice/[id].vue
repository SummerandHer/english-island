<template>
  <div>
    <NuxtLink
      :to="backTo"
      class="mb-4 inline-block text-sm text-[var(--island-primary)]"
    >
      &larr; {{ isLong ? '长篇列表' : '短篇列表' }}
    </NuxtLink>

    <div v-if="loading" class="island-card p-6 text-sm text-gray-500">加载中…</div>
    <div v-else-if="errorMessage" class="island-card p-6">
      <p class="mb-4 text-gray-700">{{ errorMessage }}</p>
      <NButton quaternary @click="load">重试</NButton>
    </div>

    <template v-else-if="passage">
      <header class="mb-4 flex flex-wrap items-start justify-between gap-3">
        <div>
          <h1 class="text-xl font-bold">{{ passage.title }}</h1>
          <p class="mt-1 text-sm text-gray-500">
            {{ passage.examLevel?.toUpperCase() }} ·
            {{ isLong ? '仿真长篇匹配' : '仿真短篇' }} ·
            {{ passage.questions.length }} 题
            <span v-if="passage.recommendedMinutes"> · 推荐 {{ passage.recommendedMinutes }} 分钟</span>
          </p>
        </div>
        <div v-if="!result" class="rounded-lg bg-amber-50 px-3 py-1.5 text-sm text-amber-800">
          用时 {{ formatElapsed(elapsed) }}
          <span v-if="passage.recommendedMinutes" class="text-amber-600">
            / {{ passage.recommendedMinutes }}:00
          </span>
        </div>
      </header>

      <p class="mb-4 text-xs text-gray-400">
        考试模式：提交前不可点词、不提供全文翻译。本站为仿真题，非历年原题。
        <span v-if="isLong"> 点击段落标号作答；同一段落可对应多题。</span>
      </p>

      <div v-if="!result && isLong" class="lg:grid lg:grid-cols-2 lg:gap-6 lg:items-start">
        <section class="island-card max-h-[70vh] space-y-3 overflow-y-auto p-4 lg:sticky lg:top-20">
          <h2 class="text-sm font-medium text-gray-600">阅读段落（点击标号作答当前题）</h2>
          <article
            v-for="para in paragraphs"
            :id="`para-${para.label}`"
            :key="para.label"
            class="rounded-lg border p-3 transition"
            :class="paraClass(para.label)"
          >
            <button
              type="button"
              class="mb-2 inline-flex h-8 w-8 items-center justify-center rounded-md text-sm font-bold"
              :class="
                answers[activeQuestionId!] === para.label
                  ? 'bg-[var(--island-primary)] text-white'
                  : 'bg-gray-100 text-gray-700 hover:bg-green-100'
              "
              @click="pickParagraph(para.label)"
            >
              {{ para.label }}
            </button>
            <p class="whitespace-pre-wrap text-sm leading-relaxed text-gray-800">{{ para.text }}</p>
          </article>
        </section>

        <div class="mt-6 space-y-4 lg:mt-0">
          <div class="flex flex-wrap gap-2">
            <button
              v-for="(q, idx) in passage.questions"
              :key="q.id"
              type="button"
              class="h-8 min-w-8 rounded px-2 text-xs font-medium"
              :class="qChipClass(q.id, idx)"
              @click="activeIdx = idx"
            >
              {{ idx + 1 }}{{ answers[q.id] ? `·${answers[q.id]}` : '' }}
            </button>
          </div>

          <section v-if="activeQuestion" class="island-card p-4">
            <p class="mb-3 text-xs text-gray-400">
              当前第 {{ activeIdx + 1 }} / {{ passage.questions.length }} 题
            </p>
            <p class="mb-4 font-medium text-gray-800">{{ activeQuestion.stem }}</p>
            <div class="flex flex-wrap gap-2">
              <button
                v-for="opt in activeQuestion.options"
                :key="opt.label"
                type="button"
                class="h-9 min-w-9 rounded-md border text-sm font-semibold"
                :class="
                  answers[activeQuestion.id] === opt.label
                    ? 'border-[var(--island-primary)] bg-green-50 text-[var(--island-primary)]'
                    : 'border-gray-200 text-gray-700 hover:border-green-300'
                "
                @click="pickParagraph(opt.label)"
              >
                {{ opt.label }}
              </button>
            </div>
            <div class="mt-4 flex gap-2">
              <NButton size="small" :disabled="activeIdx <= 0" @click="activeIdx--">上一题</NButton>
              <NButton
                size="small"
                :disabled="activeIdx >= passage.questions.length - 1"
                @click="activeIdx++"
              >
                下一题
              </NButton>
            </div>
          </section>

          <NButton type="primary" :loading="submitting" :disabled="!canSubmit" @click="submit">
            {{ auth.isLoggedIn ? '提交答案' : '请先登录后提交' }}
          </NButton>
        </div>
      </div>

      <div v-else-if="!result" class="lg:grid lg:grid-cols-2 lg:gap-6 lg:items-start">
        <section class="island-card p-4 lg:sticky lg:top-20">
          <h2 class="mb-3 text-sm font-medium text-gray-600">阅读材料</h2>
          <SelectablePassageText
            :content="passage.contentEn"
            :passage-id="passage.id"
            :passage-title="passage.title"
            source-type="sim_exam"
            disabled
          />
        </section>

        <div class="mt-6 space-y-4 lg:mt-0">
          <div class="flex flex-wrap gap-2">
            <button
              v-for="(q, idx) in passage.questions"
              :key="q.id"
              type="button"
              class="h-8 w-8 rounded text-xs font-medium"
              :class="answers[q.id] ? 'bg-[var(--island-primary)] text-white' : 'bg-gray-100 text-gray-600'"
              @click="scrollToQ(idx)"
            >
              {{ idx + 1 }}
            </button>
          </div>

          <section
            v-for="(q, idx) in passage.questions"
            :id="`sq-${idx}`"
            :key="q.id"
            class="island-card p-4"
          >
            <p class="mb-3 font-medium text-gray-800">{{ idx + 1 }}. {{ q.stem }}</p>
            <NRadioGroup
              :value="answers[q.id] ?? null"
              @update:value="(v: string) => (answers[q.id] = v)"
            >
              <NSpace vertical>
                <NRadio v-for="opt in q.options" :key="opt.label" :value="opt.label">
                  <span class="font-medium">{{ opt.label }}.</span> {{ opt.content }}
                </NRadio>
              </NSpace>
            </NRadioGroup>
          </section>

          <NButton type="primary" :loading="submitting" :disabled="!canSubmit" @click="submit">
            {{ auth.isLoggedIn ? '提交答案' : '请先登录后提交' }}
          </NButton>
        </div>
      </div>

      <div v-else class="space-y-6">
        <section class="island-card p-4">
          <p class="text-lg font-semibold">
            得分 {{ result.correctCount }} / {{ result.totalQuestions }}
          </p>
          <p class="mt-1 text-sm text-gray-500">
            用时 {{ formatElapsed(result.elapsedSeconds) }}
            <span v-if="result.recommendedMinutes">
              · 推荐 {{ result.recommendedMinutes }} 分钟
              <span
                v-if="result.elapsedSeconds > result.recommendedMinutes * 60"
                class="text-amber-600"
              >
                （超时）
              </span>
            </span>
          </p>
        </section>

        <section class="island-card p-4">
          <div class="mb-3 flex flex-wrap items-center gap-2">
            <h2 class="text-sm font-medium text-gray-600">原文复盘</h2>
            <NRadioGroup v-model:value="viewMode" size="small">
              <NRadioButton value="en">英文</NRadioButton>
              <NRadioButton value="both">英中对照</NRadioButton>
              <NRadioButton value="zh">中文</NRadioButton>
            </NRadioGroup>
          </div>
          <template v-if="isLong && reviewParagraphs.length">
            <article
              v-for="para in reviewParagraphs"
              :key="para.label"
              class="mb-3 rounded-lg border p-3"
              :class="highlightLabels.has(para.label) ? 'border-green-300 bg-green-50/50' : 'border-gray-100'"
            >
              <span class="mb-1 inline-block rounded bg-gray-800 px-2 py-0.5 text-xs font-bold text-white">
                {{ para.label }}
              </span>
              <SelectablePassageText
                v-if="viewMode !== 'zh'"
                :content="para.text"
                :passage-id="result.passageId"
                :passage-title="result.title"
                source-type="sim_exam"
              />
              <p
                v-if="viewMode !== 'en' && zhParagraphMap[para.label]"
                class="mt-2 whitespace-pre-wrap text-sm text-gray-700"
              >
                {{ zhParagraphMap[para.label] }}
              </p>
            </article>
          </template>
          <template v-else>
            <SelectablePassageText
              v-if="viewMode !== 'zh'"
              :content="result.contentEn"
              :passage-id="result.passageId"
              :passage-title="result.title"
              source-type="sim_exam"
            />
            <p
              v-if="viewMode !== 'en' && result.contentZh"
              class="mt-4 whitespace-pre-wrap text-sm leading-relaxed text-gray-700"
              :class="viewMode === 'both' ? 'border-t border-gray-100 pt-4' : ''"
            >
              {{ result.contentZh }}
            </p>
          </template>
        </section>

        <section v-if="result.vocab?.length" class="island-card p-4">
          <h2 class="mb-2 text-sm font-medium text-gray-600">本篇精练词</h2>
          <ul class="flex flex-wrap gap-2">
            <li
              v-for="(v, i) in result.vocab"
              :key="i"
              class="rounded-md bg-gray-50 px-2 py-1 text-xs"
            >
              <strong>{{ v.word }}</strong>
              <span class="text-gray-500"> {{ v.zh }}</span>
            </li>
          </ul>
        </section>

        <section
          v-for="(r, idx) in result.results"
          :key="r.questionId"
          class="island-card p-4"
          :class="r.correct ? 'border-green-100' : 'border-red-100'"
        >
          <p class="font-medium">
            {{ idx + 1 }}. {{ r.stem }}
            <NTag size="small" :type="r.correct ? 'success' : 'error'" class="ml-2">
              {{ r.correct ? '正确' : '错误' }}
            </NTag>
            <NTag v-if="r.skillTag" size="small" class="ml-1">{{ skillLabel(r.skillTag) }}</NTag>
          </p>
          <p class="mt-2 text-sm text-gray-600">
            你的答案 {{ r.userLabel || '—' }}
            <span v-if="!r.correct"> · 正解 {{ r.correctLabel }}</span>
          </p>
          <div class="mt-3 space-y-2 rounded-lg bg-gray-50 p-3 text-sm text-gray-700">
            <p v-if="r.locateEn">
              <span class="font-medium text-gray-500">【定位】</span>{{ r.locateEn }}
              <span v-if="r.locateZh" class="block text-gray-500">{{ r.locateZh }}</span>
            </p>
            <p v-if="r.explainCorrect">
              <span class="font-medium text-gray-500">【正解】</span>{{ r.explainCorrect }}
            </p>
            <div v-if="r.explainDistractors && Object.keys(r.explainDistractors).length">
              <span class="font-medium text-gray-500">【干扰】</span>
              <ul class="mt-1 list-inside list-disc text-gray-600">
                <li v-for="(txt, lab) in r.explainDistractors" :key="lab">{{ lab }}：{{ txt }}</li>
              </ul>
            </div>
            <p v-if="r.explainTip">
              <span class="font-medium text-gray-500">【技巧】</span>{{ r.explainTip }}
            </p>
          </div>
        </section>

        <NButton @click="resetPractice">再练一次</NButton>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import type { SimPracticeDetail, SimSubmitResult } from '~/types/api'

const route = useRoute()
const { request } = useApi()
const auth = useAuthStore()
const message = useAppMessage()

const passageId = computed(() => Number(route.params.id))
const passage = ref<SimPracticeDetail | null>(null)
const loading = ref(true)
const errorMessage = ref('')
const answers = ref<Record<number, string>>({})
const submitting = ref(false)
const result = ref<SimSubmitResult | null>(null)
const elapsed = ref(0)
const viewMode = ref<'en' | 'both' | 'zh'>('both')
const activeIdx = ref(0)
let timer: ReturnType<typeof setInterval> | null = null

const isLong = computed(
  () =>
    passage.value?.sectionType === 'long_match' ||
    result.value?.contentEn?.includes('[A]') === true
)
const backTo = computed(() => (isLong.value ? '/islands/exam/long' : '/islands/exam/short'))

const paragraphs = computed(() => {
  if (passage.value?.paragraphs?.length) return passage.value.paragraphs
  return parseParagraphs(passage.value?.contentEn || '')
})

const activeQuestion = computed(() => passage.value?.questions[activeIdx.value] || null)
const activeQuestionId = computed(() => activeQuestion.value?.id)

const canSubmit = computed(() => {
  if (!auth.isLoggedIn || !passage.value) return false
  return passage.value.questions.every((q) => answers.value[q.id])
})

const highlightLabels = computed(() => {
  const set = new Set<string>()
  result.value?.results?.forEach((r) => {
    if (r.correctLabel) set.add(r.correctLabel)
  })
  return set
})

const reviewParagraphs = computed(() => parseParagraphs(result.value?.contentEn || ''))
const zhParagraphMap = computed(() => {
  const map: Record<string, string> = {}
  parseParagraphs(result.value?.contentZh || '').forEach((p) => {
    map[p.label] = p.text
  })
  return map
})

onMounted(() => {
  auth.hydrate()
  load()
})

onUnmounted(() => stopTimer())

function parseParagraphs(content: string) {
  const out: Array<{ label: string; text: string }> = []
  const re = /\[([A-Z])\]\s*([\s\S]*?)(?=\[[A-Z]\]|$)/g
  let m: RegExpExecArray | null
  while ((m = re.exec(content))) {
    const text = m[2].trim()
    if (text) out.push({ label: m[1], text })
  }
  return out
}

async function load() {
  if (!Number.isFinite(passageId.value) || passageId.value <= 0) {
    errorMessage.value = '篇章 ID 无效'
    loading.value = false
    return
  }
  loading.value = true
  errorMessage.value = ''
  result.value = null
  answers.value = {}
  activeIdx.value = 0
  try {
    passage.value = await request<SimPracticeDetail>(`/api/v1/sim-exam/passages/${passageId.value}`)
    startTimer()
  } catch (e: unknown) {
    errorMessage.value = e instanceof Error ? e.message : '加载失败'
    passage.value = null
  } finally {
    loading.value = false
  }
}

function startTimer() {
  stopTimer()
  elapsed.value = 0
  timer = setInterval(() => {
    elapsed.value += 1
  }, 1000)
}

function stopTimer() {
  if (timer) {
    clearInterval(timer)
    timer = null
  }
}

function formatElapsed(sec: number) {
  const m = Math.floor(sec / 60)
  const s = sec % 60
  return `${m}:${String(s).padStart(2, '0')}`
}

function scrollToQ(idx: number) {
  document.getElementById(`sq-${idx}`)?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

function pickParagraph(label: string) {
  if (!activeQuestion.value) return
  answers.value[activeQuestion.value.id] = label
}

function paraClass(label: string) {
  const used = Object.values(answers.value).includes(label)
  const active = answers.value[activeQuestionId.value!] === label
  if (active) return 'border-[var(--island-primary)] bg-green-50/40'
  if (used) return 'border-green-200'
  return 'border-gray-100'
}

function qChipClass(id: number, idx: number) {
  if (idx === activeIdx.value) return 'bg-[var(--island-primary)] text-white'
  if (answers.value[id]) return 'bg-green-100 text-green-800'
  return 'bg-gray-100 text-gray-600'
}

function skillLabel(tag: string) {
  const map: Record<string, string> = {
    main_idea: '主旨',
    detail: '细节',
    inference: '推理',
    vocab_in_context: '词义',
    attitude: '态度',
    structure: '结构'
  }
  return map[tag] || tag
}

async function submit() {
  if (!passage.value || !canSubmit.value) return
  submitting.value = true
  stopTimer()
  try {
    result.value = await request<SimSubmitResult>(
      `/api/v1/sim-exam/passages/${passage.value.id}/submit`,
      {
        method: 'POST',
        body: {
          elapsedSeconds: elapsed.value,
          answers: passage.value.questions.map((q) => ({
            questionId: q.id,
            label: answers.value[q.id]
          }))
        }
      }
    )
    message.success('已提交')
  } catch (e: unknown) {
    message.error(e instanceof Error ? e.message : '提交失败')
    startTimer()
  } finally {
    submitting.value = false
  }
}

function resetPractice() {
  result.value = null
  answers.value = {}
  activeIdx.value = 0
  startTimer()
}
</script>
