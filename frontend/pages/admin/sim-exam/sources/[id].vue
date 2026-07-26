<template>
  <div class="max-w-3xl space-y-6">
    <NuxtLink to="/admin/sim-exam/sources" class="text-sm text-[var(--island-primary)]">← 底稿列表</NuxtLink>
    <h1 class="text-xl font-bold">{{ isNew ? '录入真题底稿' : '编辑底稿' }}</h1>
    <p class="text-sm text-gray-500">
      仅录入本人持有真题。用户永不看见底稿。生成失败只能重试，不可手工改写冒充仿真卷。
    </p>

    <NForm label-placement="top">
      <div class="grid gap-4 md:grid-cols-2">
        <NFormItem label="级别" required>
          <NSelect v-model:value="form.examLevel" :options="levelOptions" />
        </NFormItem>
        <NFormItem label="题型" required>
          <NSelect v-model:value="form.sectionType" :options="sectionOptions" />
        </NFormItem>
      </div>
      <NFormItem label="标题" required>
        <NInput v-model:value="form.title" placeholder="含考试年月，仅 Admin 可见" />
      </NFormItem>
      <NFormItem label="试卷说明" >
        <NInput v-model:value="form.sourceMeta" placeholder="如 2024年6月 CET4 仔细阅读 Passage 1" />
      </NFormItem>
      <NFormItem label="英文正文" required>
        <NInput v-model:value="form.passageEn" type="textarea" :rows="10" />
      </NFormItem>
      <NFormItem label="原题 JSON（题干/选项/答案）" required>
        <NInput
          v-model:value="form.questionsJson"
          type="textarea"
          :rows="8"
          placeholder='[{"stem":"...","options":[{"label":"A","content":"...","correct":true}]}]'
        />
      </NFormItem>
      <NFormItem label="可选官方解析摘要">
        <NInput v-model:value="form.officialExplains" type="textarea" :rows="3" />
      </NFormItem>
      <NFormItem label="license_note">
        <NInput v-model:value="form.licenseNote" type="textarea" :rows="2" />
      </NFormItem>

      <div class="flex flex-wrap gap-3">
        <NButton type="primary" :loading="saving" @click="save">保存底稿</NButton>
        <NButton
          v-if="!isNew"
          :loading="generating"
          @click="generate"
        >
          生成仿真卷
        </NButton>
      </div>
      <p v-if="genMsg" class="text-sm" :class="genOk ? 'text-green-700' : 'text-red-600'">{{ genMsg }}</p>
    </NForm>
  </div>
</template>

<script setup lang="ts">
import type { AdminSimSourceDetail, SimGenerateResult } from '~/types/api'

definePageMeta({ layout: 'admin', middleware: 'admin', ssr: false })

const route = useRoute()
const router = useRouter()
const { request } = useApi()
const message = useAppMessage()

const isNew = computed(() => route.params.id === 'new' || route.path.endsWith('/new'))
const sourceId = computed(() => (isNew.value ? null : Number(route.params.id)))

const levelOptions = [
  { label: 'CET4', value: 'cet4' },
  { label: 'CET6', value: 'cet6' }
]
const sectionOptions = [
  { label: '短篇仔细阅读', value: 'short_careful' },
  { label: '长篇信息匹配', value: 'long_match' }
]

const form = reactive({
  examLevel: 'cet4',
  sectionType: 'short_careful',
  title: '',
  passageEn: '',
  questionsJson: '[]',
  sourceMeta: '',
  officialExplains: '',
  licenseNote: '本人持有之四六级试卷录入，仅作站内仿真题仿写底稿，不对用户展示原文/原题。'
})

const saving = ref(false)
const generating = ref(false)
const genMsg = ref('')
const genOk = ref(false)

onMounted(async () => {
  if (isNew.value || !sourceId.value) return
  const d = await request<AdminSimSourceDetail>(`/api/v1/admin/content/sim-exam/sources/${sourceId.value}`)
  form.examLevel = d.examLevel
  form.sectionType = d.sectionType
  form.title = d.title
  form.passageEn = d.passageEn
  form.questionsJson = d.questionsJson
  form.sourceMeta = d.sourceMeta || ''
  form.officialExplains = d.officialExplains || ''
  form.licenseNote = d.licenseNote
})

async function save() {
  saving.value = true
  try {
    if (isNew.value) {
      const res = await request<{ id: number }>('/api/v1/admin/content/sim-exam/sources', {
        method: 'POST',
        body: { ...form }
      })
      message.success('已创建')
      await router.replace(`/admin/sim-exam/sources/${res.id}`)
    } else {
      await request(`/api/v1/admin/content/sim-exam/sources/${sourceId.value}`, {
        method: 'PUT',
        body: { ...form }
      })
      message.success('已保存')
    }
  } catch (e: unknown) {
    message.error(e instanceof Error ? e.message : '保存失败')
  } finally {
    saving.value = false
  }
}

async function generate() {
  if (!sourceId.value) return
  generating.value = true
  genMsg.value = ''
  try {
    const res = await request<SimGenerateResult>(
      `/api/v1/admin/content/sim-exam/sources/${sourceId.value}/generate`,
      { method: 'POST' }
    )
    genOk.value = res.aiStatus === 'ok' || res.aiStatus === 'needs_review'
    genMsg.value = genOk.value
      ? `生成完成（${res.aiStatus}），仿真卷 #${res.passageId}${res.warnings?.length ? '；' + res.warnings.join('；') : ''}`
      : res.aiError || '生成失败'
    if (genOk.value) {
      message.success('可去仿真卷列表发布')
    }
  } catch (e: unknown) {
    genOk.value = false
    genMsg.value = e instanceof Error ? e.message : '生成失败'
  } finally {
    generating.value = false
  }
}
</script>
