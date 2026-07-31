<template>
  <div class="max-w-3xl space-y-6">
    <NuxtLink to="/admin/sim-exam/sources" class="text-sm text-[var(--island-primary)]">← 底稿列表</NuxtLink>
    <h1 class="text-xl font-bold">{{ isNew ? '录入真题底稿' : '编辑底稿' }}</h1>
    <p class="text-sm text-gray-500">
      仅录入本人持有真题。题目可直接从别处复制粘贴，无需手写 JSON。用户永不看见底稿。
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
      <NFormItem label="试卷说明">
        <NInput v-model:value="form.sourceMeta" placeholder="如 2024年6月 CET4 仔细阅读 Passage 1" />
      </NFormItem>
      <NFormItem label="英文正文" required>
        <NInput v-model:value="form.passageEn" type="textarea" :rows="10" />
      </NFormItem>

      <NFormItem label="全部题目（直接粘贴）" required>
        <NInput
          v-model:value="form.questionsText"
          type="textarea"
          :rows="14"
          :placeholder="questionsPlaceholder"
        />
        <p class="mt-1 text-xs text-gray-400">
          支持格式：题号 + 题干，下一行 A) B) C) D) 选项。可从真题 PDF/网页整段复制。
        </p>
      </NFormItem>

      <NFormItem label="答案（可选，强烈建议填）">
        <NInput
          v-model:value="form.answersText"
          type="textarea"
          :rows="2"
          placeholder="46.D 47.A 48.D 49.C 50.B   或按题序：D A D C B"
        />
        <p class="mt-1 text-xs text-gray-400">
          粘贴的题目通常不含正确答案；填上答案有助于 AI 对齐考点生成仿真卷。
        </p>
      </NFormItem>

      <NFormItem label="可选官方解析摘要">
        <NInput v-model:value="form.officialExplains" type="textarea" :rows="3" />
      </NFormItem>
      <NFormItem label="license_note">
        <NInput v-model:value="form.licenseNote" type="textarea" :rows="2" />
      </NFormItem>

      <div class="flex flex-wrap gap-3">
        <NButton type="primary" :loading="saving" @click="save">保存底稿</NButton>
        <NButton v-if="!isNew" :loading="generating" @click="generate">生成仿真卷</NButton>
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

const questionsPlaceholder = `46. What is the classic grocery store dilemma?

A) Whether or not one should eat organic food.
B) Whether or not one can stretch their wallet far.
C) Organic food costs more but it certainly tastes better.
D) One wants the best food but their budget is limited.
47. What do we learn about organic food from science?

A) Whether it is any better remains uncertain.
B) Whether it is healthier is under consideration.
C) Whether it is more nutritious than conventional food is arguable.
D) Whether it is going to replace conventional food is still unclear.`

const form = reactive({
  examLevel: 'cet4',
  sectionType: 'short_careful',
  title: '',
  passageEn: '',
  questionsText: '',
  answersText: '',
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
  await loadDetail(sourceId.value)
})

async function loadDetail(id: number) {
  const d = await request<AdminSimSourceDetail>(`/api/v1/admin/content/sim-exam/sources/${id}`)
  form.examLevel = d.examLevel
  form.sectionType = d.sectionType
  form.title = d.title || ''
  form.passageEn = d.passageEn || ''
  form.questionsText = d.questionsText || d.questionsJson || ''
  form.answersText = d.answersText || ''
  form.sourceMeta = d.sourceMeta || ''
  form.officialExplains = d.officialExplains || ''
  form.licenseNote = d.licenseNote || form.licenseNote
}

async function save() {
  if (!form.questionsText.trim()) {
    message.error('请粘贴题目文本')
    return
  }
  if (!form.passageEn.trim()) {
    message.error('请填写英文正文')
    return
  }
  saving.value = true
  try {
    const body = {
      examLevel: form.examLevel,
      sectionType: form.sectionType,
      title: form.title,
      passageEn: form.passageEn,
      questionsText: form.questionsText,
      answersText: form.answersText,
      sourceMeta: form.sourceMeta,
      officialExplains: form.officialExplains,
      licenseNote: form.licenseNote
    }
    if (isNew.value) {
      const res = await request<{ id: number }>('/api/v1/admin/content/sim-exam/sources', {
        method: 'POST',
        body
      })
      message.success('已保存入库')
      await router.replace(`/admin/sim-exam/sources/${res.id}`)
      await loadDetail(res.id)
    } else {
      await request(`/api/v1/admin/content/sim-exam/sources/${sourceId.value}`, {
        method: 'PUT',
        body
      })
      message.success('已保存入库')
      if (sourceId.value) await loadDetail(sourceId.value)
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
