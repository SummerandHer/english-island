<template>
  <div class="max-w-3xl space-y-6">
    <NuxtLink to="/admin/reading" class="text-sm text-[var(--island-primary)]">← 篇章列表</NuxtLink>
    <h1 class="text-xl font-bold">{{ isNew ? '新建阅读篇章' : '编辑阅读篇章' }}</h1>

    <NForm v-if="!loading" label-placement="top">
      <NFormItem label="所属章节">
        <NSelect v-model:value="form.chapterId" :options="chapterOptions" clearable />
      </NFormItem>
      <NFormItem label="标题" required>
        <NInput v-model:value="form.title" />
      </NFormItem>
      <NFormItem label="英文正文" required>
        <NInput v-model:value="form.contentEn" type="textarea" :rows="10" />
      </NFormItem>
      <NFormItem label="难度">
        <NSelect v-model:value="form.difficulty" :options="difficultyOptions" />
      </NFormItem>
      <NFormItem label="排序">
        <NInputNumber v-model:value="form.sortOrder" class="w-full" />
      </NFormItem>
      <NFormItem label="上架">
        <NSwitch v-model:value="form.published" />
      </NFormItem>

      <h2 class="font-semibold">选择题（每题 4 选项，勾选正确答案）</h2>
      <div v-for="(q, qi) in form.questions" :key="qi" class="rounded-lg border border-gray-100 p-4">
        <p class="mb-2 text-sm text-gray-500">第 {{ qi + 1 }} 题</p>
        <NInput v-model:value="q.stem" type="textarea" :rows="2" placeholder="题干" class="mb-2" />
        <NInput v-model:value="q.explanation" placeholder="解析（可选）" class="mb-2" />
        <div v-for="opt in q.options" :key="opt.label" class="mb-2 flex items-center gap-2">
          <span class="w-6 font-medium">{{ opt.label }}</span>
          <NInput v-model:value="opt.content" class="flex-1" placeholder="选项内容" />
          <NCheckbox v-model:checked="opt.correct">正确</NCheckbox>
        </div>
      </div>

      <NButton type="primary" :loading="saving" @click="save">保存</NButton>
    </NForm>
  </div>
</template>

<script setup lang="ts">
definePageMeta({ layout: 'admin', middleware: 'admin', ssr: false })

const route = useRoute()
const router = useRouter()
const { request } = useApi()

const isNew = computed(() => route.params.id === 'new')
const loading = ref(!isNew.value)
const saving = ref(false)

function emptyQuestion() {
  return {
    stem: '',
    explanation: '',
    sortOrder: 0,
    options: ['A', 'B', 'C', 'D'].map((label) => ({ label, content: '', correct: label === 'A' }))
  }
}

const form = ref({
  chapterId: null as number | null,
  title: '',
  contentEn: '',
  difficulty: 'cet4',
  sortOrder: 0,
  published: true,
  questions: [emptyQuestion(), emptyQuestion(), emptyQuestion(), emptyQuestion()]
})

const chapterOptions = ref<{ label: string; value: number }[]>([])
const difficultyOptions = [
  { label: '四级', value: 'cet4' },
  { label: '六级', value: 'cet6' }
]

onMounted(async () => {
  const chapters = await request<Array<{ id: number; title: string }>>(
    '/api/v1/admin/content/reading/chapters'
  )
  chapterOptions.value = chapters.map((c) => ({ label: c.title, value: c.id }))
  if (!isNew.value) {
    const d = await request<{
      chapterId: number
      title: string
      contentEn: string
      difficulty: string
      sortOrder: number
      status: number
      questions: Array<{
        stem: string
        explanation: string
        sortOrder: number
        options: Array<{ label: string; content: string; correct: boolean }>
      }>
    }>(`/api/v1/admin/content/reading/passages/${route.params.id}`)
    form.value = {
      chapterId: d.chapterId,
      title: d.title,
      contentEn: d.contentEn,
      difficulty: d.difficulty,
      sortOrder: d.sortOrder,
      published: d.status === 1,
      questions: d.questions.map((q, i) => ({
        stem: q.stem,
        explanation: q.explanation || '',
        sortOrder: q.sortOrder ?? i,
        options: q.options.map((o) => ({
          label: o.label,
          content: o.content,
          correct: o.correct
        }))
      }))
    }
    loading.value = false
  }
})

function buildPayload() {
  return {
    chapterId: form.value.chapterId,
    title: form.value.title,
    contentEn: form.value.contentEn,
    difficulty: form.value.difficulty,
    sortOrder: form.value.sortOrder,
    isMock: 1,
    status: form.value.published ? 1 : 0,
    questions: form.value.questions.map((q, i) => ({
      stem: q.stem,
      explanation: q.explanation,
      sortOrder: i + 1,
      options: q.options.map((o) => ({
        label: o.label,
        content: o.content,
        isCorrect: o.correct ? 1 : 0
      }))
    }))
  }
}

async function save() {
  saving.value = true
  try {
    const body = buildPayload()
    if (isNew.value) {
      const res = await request<{ id: number }>('/api/v1/admin/content/reading/passages', {
        method: 'POST',
        body
      })
      router.replace(`/admin/reading/${res.id}`)
    } else {
      await request(`/api/v1/admin/content/reading/passages/${route.params.id}`, {
        method: 'PUT',
        body
      })
    }
    window.alert('已保存')
  } catch (e: unknown) {
    window.alert(e instanceof Error ? e.message : '保存失败')
  } finally {
    saving.value = false
  }
}
</script>
