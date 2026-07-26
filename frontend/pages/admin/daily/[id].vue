<template>
  <div class="max-w-3xl space-y-6">
    <NuxtLink to="/admin/daily" class="text-sm text-[var(--island-primary)]">← 日报列表</NuxtLink>
    <h1 class="text-xl font-bold">{{ isNew ? '新建日报' : '编辑日报' }}</h1>
    <p class="text-sm text-gray-500">
      只需填写标题、正文与出处信息，点「一键 AI 增强」生成主题、难度、摘要、词汇与句式。
    </p>

    <NForm v-if="!loading" label-placement="top">
      <section class="rounded-xl border border-gray-100 bg-white p-4 space-y-1">
        <h2 class="mb-2 text-sm font-semibold text-gray-700">基础信息</h2>
        <NFormItem label="标题" required>
          <NInput v-model:value="form.title" placeholder="Why Slow Learning Still Wins" />
        </NFormItem>
        <NFormItem label="英文正文（自编改写）" required>
          <NInput v-model:value="form.contentEn" type="textarea" :rows="12" placeholder="粘贴改写后的英文正文…" />
          <p class="mt-1 text-xs" :class="wordCountHintClass">{{ wordCountHint }}</p>
        </NFormItem>
        <div class="grid gap-4 md:grid-cols-3">
          <NFormItem label="作者" required>
            <NInput v-model:value="form.sourceAuthor" placeholder="Island Editorial" />
          </NFormItem>
          <NFormItem label="地点 / 媒体语境" required>
            <NInput v-model:value="form.sourcePlace" placeholder="Campus education desk" />
          </NFormItem>
          <NFormItem label="文章出处时间" required>
            <NInput v-model:value="form.sourcePublishedAt" type="date" />
          </NFormItem>
        </div>
        <NFormItem label="封面图 URL" required>
          <NInput v-model:value="form.coverUrl" placeholder="/home/hero-banner.png" />
        </NFormItem>
        <div class="grid gap-4 md:grid-cols-2">
          <NFormItem label="排期日（仅周一 / 三 / 五）" required>
            <NInput v-model:value="form.publishDate" type="date" />
            <p v-if="publishDayWarn" class="mt-1 text-xs text-amber-600">{{ publishDayWarn }}</p>
          </NFormItem>
          <NFormItem label="状态">
            <NSelect v-model:value="form.status" :options="statusOptions" />
          </NFormItem>
        </div>
      </section>

      <div class="flex flex-wrap items-center gap-3">
        <NButton type="primary" :loading="parsing" @click="runAiEnrich">一键 AI 增强</NButton>
        <span
          v-if="gate"
          class="rounded-full px-2.5 py-0.5 text-xs font-medium"
          :class="gateBadgeClass"
        >{{ gateLabel }}</span>
        <span v-if="parseMsg" class="text-sm text-gray-500">{{ parseMsg }}</span>
      </div>

      <section
        v-if="hasEnrichment"
        class="rounded-xl border border-emerald-100 bg-emerald-50/40 p-4 space-y-4"
      >
        <div class="flex flex-wrap items-center gap-2">
          <h2 class="text-sm font-semibold text-gray-800">AI 产出预览</h2>
          <span v-if="form.aiVersion" class="text-xs text-gray-400">{{ form.aiVersion }}</span>
        </div>
        <ul v-if="warnings.length" class="list-disc space-y-1 pl-5 text-xs text-amber-700">
          <li v-for="(w, i) in warnings" :key="i">{{ w }}</li>
        </ul>
        <p v-if="gateError" class="text-sm text-red-600">{{ gateError }}</p>

        <div class="grid gap-3 md:grid-cols-3 text-sm">
          <div>
            <p class="text-xs text-gray-500">主题</p>
            <p class="font-medium">{{ topicLabel[form.topic] || form.topic || '—' }}</p>
          </div>
          <div>
            <p class="text-xs text-gray-500">难度</p>
            <p class="font-medium">{{ form.difficulty?.toUpperCase() || '—' }}</p>
          </div>
          <div>
            <p class="text-xs text-gray-500">词数</p>
            <p class="font-medium">{{ form.wordCount ?? '—' }}</p>
          </div>
        </div>
        <div v-if="form.summaryZh">
          <p class="text-xs text-gray-500">中文摘要</p>
          <p class="text-sm leading-relaxed">{{ form.summaryZh }}</p>
        </div>
        <div v-if="coverHint">
          <p class="text-xs text-gray-500">封面提示（可选参考）</p>
          <p class="text-xs text-gray-600">{{ coverHint }}</p>
        </div>

        <div v-if="cetPreview.length" class="space-y-1">
          <p class="text-xs font-medium text-gray-600">高频词</p>
          <ul class="flex flex-wrap gap-2">
            <li
              v-for="(v, i) in cetPreview"
              :key="'c' + i"
              class="rounded-md bg-white px-2 py-1 text-xs shadow-sm"
            >
              <strong>{{ v.word }}</strong>
              <span v-if="v.pos" class="text-gray-400"> {{ v.pos }}</span>
              <span class="text-gray-600"> {{ v.zh }}</span>
              <span v-if="v.inGlossary === false" class="text-amber-500"> ·未入库</span>
            </li>
          </ul>
        </div>
        <div v-if="hardPreview.length" class="space-y-1">
          <p class="text-xs font-medium text-gray-600">难词</p>
          <ul class="flex flex-wrap gap-2">
            <li
              v-for="(v, i) in hardPreview"
              :key="'h' + i"
              class="rounded-md bg-white px-2 py-1 text-xs shadow-sm"
            >
              <strong>{{ v.word }}</strong>
              <span class="text-gray-600"> {{ v.zh }}</span>
              <em v-if="v.note" class="text-gray-400">（{{ v.note }}）</em>
            </li>
          </ul>
        </div>
        <div v-if="structPreview.length" class="space-y-2">
          <p class="text-xs font-medium text-gray-600">可借用句式</p>
          <article
            v-for="(s, i) in structPreview"
            :key="'s' + i"
            class="rounded-md bg-white p-2 text-xs shadow-sm"
          >
            <p class="font-medium text-gray-800">{{ s.en }}</p>
            <p v-if="s.zh" class="text-gray-600">{{ s.zh }}</p>
            <p v-if="s.hint" class="text-emerald-700">{{ s.hint }}</p>
          </article>
        </div>
      </section>

      <NCollapse>
        <NCollapseItem title="高级：手动覆盖 / 校对 JSON" name="advanced">
          <NFormItem label="Slug（可空，自动生成）">
            <NInput v-model:value="form.slug" placeholder="why-slow-learning-still-wins" />
          </NFormItem>
          <div class="grid gap-4 md:grid-cols-2">
            <NFormItem label="主题">
              <NSelect v-model:value="form.topic" :options="topicOptions" clearable />
            </NFormItem>
            <NFormItem label="难度">
              <NSelect v-model:value="form.difficulty" :options="difficultyOptions" clearable />
            </NFormItem>
          </div>
          <NFormItem label="中文摘要">
            <NInput v-model:value="form.summaryZh" type="textarea" :rows="2" />
          </NFormItem>
          <NFormItem label="高频词 JSON">
            <NInput v-model:value="form.cetVocabJson" type="textarea" :rows="3" />
          </NFormItem>
          <NFormItem label="难词 JSON">
            <NInput v-model:value="form.hardVocabJson" type="textarea" :rows="3" />
          </NFormItem>
          <NFormItem label="句式结构 JSON">
            <NInput v-model:value="form.structuresJson" type="textarea" :rows="3" />
          </NFormItem>
        </NCollapseItem>
      </NCollapse>

      <div class="flex flex-wrap gap-3">
        <NButton type="primary" :loading="saving" @click="save">保存</NButton>
        <NButton
          v-if="!isNew && form.status === 'draft' && hasEnrichment"
          :loading="saving"
          @click="saveAs('ready')"
        >
          标为待发
        </NButton>
        <NButton
          v-if="!isNew && form.status !== 'published' && hasEnrichment"
          type="success"
          :loading="saving"
          @click="saveAs('published')"
        >
          发布
        </NButton>
      </div>
      <p v-if="saveMsg" class="mt-2 text-sm text-gray-500">{{ saveMsg }}</p>
    </NForm>
  </div>
</template>

<script setup lang="ts">
import type { AdminDailyArticleDetail, DailyAiEnrichmentResult, DailyStructureItem, DailyVocabItem } from '~/types/api'

definePageMeta({ layout: 'admin', middleware: 'admin', ssr: false })

const route = useRoute()
const router = useRouter()
const { request } = useApi()

const isNew = computed(() => route.params.id === 'new')
const loading = ref(!isNew.value)
const saving = ref(false)
const parsing = ref(false)
const parseMsg = ref('')
const saveMsg = ref('')
const coverHint = ref('')
const gate = ref<string | null>(null)
const gateError = ref('')
const warnings = ref<string[]>([])
const cetPreview = ref<DailyVocabItem[]>([])
const hardPreview = ref<DailyVocabItem[]>([])
const structPreview = ref<DailyStructureItem[]>([])

function nextPublishDay(): string {
  const d = new Date()
  for (let i = 0; i < 8; i++) {
    const t = new Date(d)
    t.setDate(d.getDate() + i)
    const day = t.getDay()
    if (day === 1 || day === 3 || day === 5) {
      return t.toISOString().slice(0, 10)
    }
  }
  return d.toISOString().slice(0, 10)
}

const form = ref({
  title: '',
  slug: '',
  topic: '' as string,
  difficulty: '' as string,
  contentEn: '',
  coverUrl: '/home/hero-banner.png',
  summaryZh: '',
  publishDate: nextPublishDay(),
  sourcePublishedAt: '' as string,
  sourceAuthor: 'Island Editorial',
  sourcePlace: '',
  cetVocabJson: '[]',
  hardVocabJson: '[]',
  structuresJson: '[]',
  wordCount: null as number | null,
  status: 'draft',
  aiStatus: 'idle',
  aiError: '' as string,
  aiVersion: '' as string
})

const topicLabel: Record<string, string> = {
  education: '教育学习',
  technology: '科技创新',
  environment: '环境可持续',
  society_culture: '社会文化',
  economy_business: '经济商业',
  health: '健康医学',
  psychology: '心理认知',
  science: '自然科学'
}

const topicOptions = Object.entries(topicLabel).map(([value, label]) => ({ label, value }))

const difficultyOptions = [
  { label: '四级', value: 'cet4' },
  { label: '六级', value: 'cet6' }
]

const statusOptions = [
  { label: '草稿', value: 'draft' },
  { label: '待发', value: 'ready' },
  { label: '已发布', value: 'published' }
]

const liveWordCount = computed(() => {
  const t = form.value.contentEn.trim()
  if (!t) return 0
  return t.split(/\s+/).length
})

const wordCountHint = computed(() => {
  const n = liveWordCount.value
  if (n === 0) return '词数：0（建议 280+；上限 1000，适配四六级长篇）'
  if (n < 200) return `词数：${n} · 过短，至少 200 词才能 AI 增强`
  if (n < 280) return `词数：${n} · 略低于 CET 建议下限`
  if (n <= 350) return `词数：${n} · 适合 CET4 短篇`
  if (n <= 550) return `词数：${n} · 适合 CET6 / 中篇`
  if (n <= 900) return `词数：${n} · 长篇精读，篇幅合适`
  if (n <= 1000) return `词数：${n} · 接近上限 1000`
  return `词数：${n} · 过长，请压到 1000 以内`
})

const wordCountHintClass = computed(() => {
  const n = liveWordCount.value
  if (n === 0) return 'text-gray-400'
  if (n < 200 || n > 1000) return 'text-red-600'
  if (n < 280 || n > 900) return 'text-amber-600'
  return 'text-emerald-700'
})

const publishDayWarn = computed(() => {
  if (!form.value.publishDate) return ''
  const day = new Date(form.value.publishDate + 'T12:00:00').getDay()
  if (day !== 1 && day !== 3 && day !== 5) {
    return '排期日须为周一、周三或周五'
  }
  return ''
})

const hasEnrichment = computed(() => {
  return !!(
    form.value.summaryZh ||
    (form.value.cetVocabJson && form.value.cetVocabJson !== '[]') ||
    gate.value
  )
})

const gateLabel = computed(() => {
  if (gate.value === 'ok') return '质量门禁：通过'
  if (gate.value === 'needs_review') return '质量门禁：需复核'
  if (gate.value === 'failed') return '质量门禁：失败'
  return ''
})

const gateBadgeClass = computed(() => {
  if (gate.value === 'ok') return 'bg-emerald-100 text-emerald-800'
  if (gate.value === 'needs_review') return 'bg-amber-100 text-amber-800'
  if (gate.value === 'failed') return 'bg-red-100 text-red-800'
  return 'bg-gray-100 text-gray-600'
})

function parseJsonArray<T>(raw: string | null | undefined): T[] {
  if (!raw) return []
  try {
    const v = JSON.parse(raw)
    return Array.isArray(v) ? v : []
  } catch {
    return []
  }
}

function syncPreviewsFromForm() {
  cetPreview.value = parseJsonArray(form.value.cetVocabJson)
  hardPreview.value = parseJsonArray(form.value.hardVocabJson)
  structPreview.value = parseJsonArray(form.value.structuresJson)
}

onMounted(async () => {
  if (isNew.value) {
    loading.value = false
    return
  }
  try {
    const data = await request<AdminDailyArticleDetail>(
      `/api/v1/admin/content/daily/articles/${route.params.id}`
    )
    form.value = {
      title: data.title,
      slug: data.slug,
      topic: data.topic || '',
      difficulty: data.difficulty || '',
      contentEn: data.contentEn,
      coverUrl: data.coverUrl || '',
      summaryZh: data.summaryZh || '',
      publishDate: data.publishDate,
      sourcePublishedAt: data.sourcePublishedAt || '',
      sourceAuthor: data.sourceAuthor || '',
      sourcePlace: data.sourcePlace || '',
      cetVocabJson: data.cetVocabJson || '[]',
      hardVocabJson: data.hardVocabJson || '[]',
      structuresJson: data.structuresJson || '[]',
      wordCount: data.wordCount ?? null,
      status: data.status,
      aiStatus: data.aiStatus || 'idle',
      aiError: data.aiError || '',
      aiVersion: data.aiVersion || ''
    }
    gate.value = data.aiStatus === 'ok' || data.aiStatus === 'needs_review' || data.aiStatus === 'failed'
      ? data.aiStatus
      : null
    gateError.value = data.aiError || ''
    syncPreviewsFromForm()
  } finally {
    loading.value = false
  }
})

async function runAiEnrich() {
  if (!form.value.title.trim()) {
    parseMsg.value = '请先填写标题'
    return
  }
  if (!form.value.contentEn.trim()) {
    parseMsg.value = '请先粘贴正文'
    return
  }
  parsing.value = true
  parseMsg.value = ''
  gateError.value = ''
  warnings.value = []
  try {
    const data = await request<DailyAiEnrichmentResult>('/api/v1/admin/content/daily/ai-enrich', {
      method: 'POST',
      body: {
        title: form.value.title,
        contentEn: form.value.contentEn
      }
    })
    gate.value = data.gate
    warnings.value = data.warnings || []
    gateError.value = data.error || ''
    form.value.aiVersion = data.aiVersion || ''
    form.value.wordCount = data.wordCount ?? liveWordCount.value

    if (data.gate === 'failed') {
      parseMsg.value = '增强失败，未覆盖已有结果'
      return
    }

    if (typeof data.summaryZh === 'string') form.value.summaryZh = data.summaryZh
    if (typeof data.topic === 'string' && data.topic) form.value.topic = data.topic
    if (typeof data.difficulty === 'string' && data.difficulty) form.value.difficulty = data.difficulty
    if (typeof data.cetVocabJson === 'string') form.value.cetVocabJson = data.cetVocabJson
    if (typeof data.hardVocabJson === 'string') form.value.hardVocabJson = data.hardVocabJson
    if (typeof data.structuresJson === 'string') form.value.structuresJson = data.structuresJson
    if (typeof data.coverHint === 'string') coverHint.value = data.coverHint
    if (typeof data.slugSuggestion === 'string' && data.slugSuggestion && !form.value.slug) {
      form.value.slug = data.slugSuggestion
    }
    cetPreview.value = data.cetVocab || parseJsonArray(data.cetVocabJson)
    hardPreview.value = data.hardVocab || parseJsonArray(data.hardVocabJson)
    structPreview.value = data.structures || parseJsonArray(data.structuresJson)
    parseMsg.value =
      data.gate === 'ok' ? '增强完成，可保存或发布' : '增强完成，请核对警告项后再发布'
  } catch (e) {
    gate.value = 'failed'
    parseMsg.value = e instanceof Error ? e.message : '增强失败'
  } finally {
    parsing.value = false
  }
}

function buildBody(statusOverride?: string) {
  return {
    title: form.value.title,
    slug: form.value.slug || undefined,
    topic: form.value.topic || undefined,
    difficulty: form.value.difficulty || undefined,
    contentEn: form.value.contentEn,
    coverUrl: form.value.coverUrl || undefined,
    summaryZh: form.value.summaryZh || undefined,
    publishDate: form.value.publishDate,
    sourcePublishedAt: form.value.sourcePublishedAt || undefined,
    sourceAuthor: form.value.sourceAuthor || undefined,
    sourcePlace: form.value.sourcePlace || undefined,
    cetVocabJson: form.value.cetVocabJson || undefined,
    hardVocabJson: form.value.hardVocabJson || undefined,
    structuresJson: form.value.structuresJson || undefined,
    wordCount: form.value.wordCount ?? liveWordCount.value,
    status: statusOverride || form.value.status
  }
}

async function save() {
  await persist(form.value.status)
}

async function saveAs(status: string) {
  form.value.status = status
  await persist(status)
}

async function persist(status: string) {
  if (publishDayWarn.value) {
    saveMsg.value = publishDayWarn.value
    return
  }
  saving.value = true
  saveMsg.value = ''
  try {
    const body = buildBody(status)
    if (isNew.value) {
      const res = await request<{ id: number }>('/api/v1/admin/content/daily/articles', {
        method: 'POST',
        body
      })
      saveMsg.value = '已创建'
      await router.replace(`/admin/daily/${res.id}`)
    } else {
      await request(`/api/v1/admin/content/daily/articles/${route.params.id}`, {
        method: 'PUT',
        body
      })
      saveMsg.value = status === 'published' ? '已发布' : status === 'ready' ? '已标为待发' : '已保存'
    }
  } catch (e) {
    saveMsg.value = e instanceof Error ? e.message : '保存失败'
  } finally {
    saving.value = false
  }
}
</script>
