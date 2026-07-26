<template>
  <div class="max-w-2xl space-y-4">
    <NuxtLink to="/admin/translation" class="text-sm text-[var(--island-primary)]">← 题目列表</NuxtLink>
    <h1 class="text-xl font-bold">{{ isNew ? '新建翻译题' : '编辑翻译题' }}</h1>
    <NForm v-if="!loading" label-placement="top">
      <NFormItem label="所属章节">
        <NSelect v-model:value="form.chapterId" :options="chapterOptions" clearable placeholder="可选" />
      </NFormItem>
      <NFormItem label="中文题干" required>
        <NInput v-model:value="form.promptZh" type="textarea" :rows="4" />
      </NFormItem>
      <NFormItem label="参考答案" required>
        <NInput v-model:value="form.referenceAnswer" type="textarea" :rows="4" />
      </NFormItem>
      <NFormItem label="难度">
        <NSelect v-model:value="form.difficulty" :options="difficultyOptions" />
      </NFormItem>
      <NFormItem label="排序（越小越靠前）">
        <NInputNumber v-model:value="form.sortOrder" class="w-full" />
      </NFormItem>
      <NFormItem label="VIP 题">
        <NSwitch v-model:value="form.vip" />
      </NFormItem>
      <NFormItem label="上架">
        <NSwitch v-model:value="form.published" />
      </NFormItem>
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

const form = ref({
  chapterId: null as number | null,
  promptZh: '',
  referenceAnswer: '',
  difficulty: 'cet4',
  sortOrder: 0,
  vip: false,
  published: true
})

const chapterOptions = ref<{ label: string; value: number }[]>([])
const difficultyOptions = [
  { label: '四级', value: 'cet4' },
  { label: '六级', value: 'cet6' }
]

onMounted(async () => {
  const chapters = await request<Array<{ id: number; title: string }>>(
    '/api/v1/admin/content/translation/chapters'
  )
  chapterOptions.value = chapters.map((c) => ({ label: c.title, value: c.id }))
  if (!isNew.value) {
    const d = await request<{
      chapterId: number
      promptZh: string
      referenceAnswer: string
      difficulty: string
      sortOrder: number
      vip: boolean
      status: number
    }>(`/api/v1/admin/content/translation/questions/${route.params.id}`)
    form.value = {
      chapterId: d.chapterId,
      promptZh: d.promptZh,
      referenceAnswer: d.referenceAnswer,
      difficulty: d.difficulty,
      sortOrder: d.sortOrder,
      vip: d.vip,
      published: d.status === 1
    }
    loading.value = false
  }
})

function payload() {
  return {
    chapterId: form.value.chapterId,
    promptZh: form.value.promptZh,
    referenceAnswer: form.value.referenceAnswer,
    difficulty: form.value.difficulty,
    sortOrder: form.value.sortOrder,
    isVip: form.value.vip ? 1 : 0,
    isMock: 1,
    status: form.value.published ? 1 : 0
  }
}

async function save() {
  saving.value = true
  try {
    if (isNew.value) {
      const res = await request<{ id: number }>('/api/v1/admin/content/translation/questions', {
        method: 'POST',
        body: payload()
      })
      router.replace(`/admin/translation/${res.id}`)
    } else {
      await request(`/api/v1/admin/content/translation/questions/${route.params.id}`, {
        method: 'PUT',
        body: payload()
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
