<template>
  <div v-if="video" class="space-y-6">
    <div class="flex items-center gap-2">
      <NuxtLink to="/admin/videos" class="text-sm text-[var(--island-primary)]">← 列表</NuxtLink>
      <h1 class="text-xl font-bold">编辑视频 #{{ video.id }}</h1>
    </div>

    <section class="rounded-xl border bg-white p-4 space-y-3">
      <h2 class="font-medium">基本信息</h2>
      <NForm :model="meta" label-placement="left" label-width="100">
        <NFormItem label="标题">
          <NInput v-model:value="meta.title" />
        </NFormItem>
        <NFormItem label="封面">
          <NInput v-model:value="meta.coverUrl" />
          <input type="file" accept="image/*" class="mt-2 text-sm" @change="onCoverUpload" />
        </NFormItem>
        <NFormItem label="系列">
          <NSelect v-model:value="meta.seriesId" :options="seriesOptions" clearable />
        </NFormItem>
        <NFormItem label="简介">
          <NInput v-model:value="meta.description" type="textarea" :rows="2" />
        </NFormItem>
        <NFormItem label="主题标签">
          <NSelect
            v-model:value="meta.tagIds"
            multiple
            filterable
            :options="tagOptions"
            placeholder="可多选主题"
          />
        </NFormItem>
        <NFormItem label="难度">
          <NSelect v-model:value="meta.difficulty" :options="difficultyOptions" />
        </NFormItem>
        <NFormItem label="VIP">
          <NSwitch v-model:value="meta.isVip" />
        </NFormItem>
        <NFormItem label="上架">
          <NSwitch :value="meta.status === 1" @update:value="(v: boolean) => (meta.status = v ? 1 : 0)" />
        </NFormItem>
      </NForm>
      <NButton type="primary" :loading="savingMeta" @click="saveMeta">保存基本信息</NButton>
    </section>

    <section class="rounded-xl border bg-white p-4 space-y-3">
      <div class="flex items-center justify-between">
        <h2 class="font-medium">句轴（{{ sentences.length }} 句）</h2>
        <NButton :loading="zhLoading" @click="generateZh">AI 生成中文草稿</NButton>
      </div>
      <div class="space-y-3 max-h-[480px] overflow-auto">
        <div
          v-for="(s, idx) in sentences"
          :key="idx"
          class="grid gap-2 rounded-lg border p-3 md:grid-cols-[72px_1fr_1fr]"
        >
          <span class="text-xs text-gray-500">#{{ s.seq }}</span>
          <NInput v-model:value="s.textEn" type="textarea" :rows="2" />
          <NInput v-model:value="s.textZh" type="textarea" :rows="2" />
        </div>
      </div>
      <NButton type="primary" :loading="savingSentences" @click="saveSentences">保存句轴</NButton>
    </section>
  </div>
</template>

<script setup lang="ts">
import type { AdminVideoDetail, SentenceDraft, VideoSeriesItem, VideoTag } from '~/types/api'

definePageMeta({ layout: 'admin', middleware: 'admin', ssr: false })

const route = useRoute()
const message = useAppMessage()
const { request, uploadForm } = useApi()

const video = ref<AdminVideoDetail | null>(null)
const sentences = ref<SentenceDraft[]>([])
const savingMeta = ref(false)
const savingSentences = ref(false)
const zhLoading = ref(false)

const meta = reactive({
  title: '',
  coverUrl: '',
  description: '',
  seriesId: null as number | null,
  difficulty: 'medium',
  isVip: false,
  status: 1,
  tagIds: [] as number[]
})

const seriesOptions = ref<{ label: string; value: number }[]>([])
const tagOptions = ref<{ label: string; value: number }[]>([])
const difficultyOptions = [
  { label: '简单', value: 'easy' },
  { label: '中等', value: 'medium' },
  { label: '困难', value: 'hard' }
]

onMounted(async () => {
  const [detail, series, tags] = await Promise.all([
    request<AdminVideoDetail>(`/api/v1/admin/videos/${route.params.id}`),
    request<VideoSeriesItem[]>('/api/v1/admin/video-series'),
    request<VideoTag[]>('/api/v1/admin/video-tags')
  ])
  video.value = detail
  seriesOptions.value = series.map((s) => ({ label: s.title, value: s.id }))
  tagOptions.value = tags.map((t) => ({ label: t.name, value: t.id }))
  meta.title = detail.title
  meta.coverUrl = detail.coverUrl ?? ''
  meta.description = detail.description ?? ''
  meta.seriesId = detail.seriesId ?? null
  meta.difficulty = detail.difficulty
  meta.isVip = detail.vip
  meta.status = detail.status
  meta.tagIds = (detail.tags ?? []).map((t) => t.id)
  sentences.value = detail.sentences.map((s) => ({
    seq: s.seq,
    startMs: s.startMs,
    endMs: s.endMs,
    textEn: s.textEn,
    textZh: s.textZh ?? ''
  }))
})

async function saveMeta() {
  savingMeta.value = true
  try {
    await request(`/api/v1/admin/videos/${route.params.id}`, {
      method: 'PUT',
      body: {
        title: meta.title,
        coverUrl: meta.coverUrl,
        description: meta.description,
        seriesId: meta.seriesId,
        difficulty: meta.difficulty,
        isVip: meta.isVip ? 1 : 0,
        status: meta.status,
        tagIds: meta.tagIds
      }
    })
    message.success('已保存')
  } catch (e: unknown) {
    message.error(e instanceof Error ? e.message : '保存失败')
  } finally {
    savingMeta.value = false
  }
}

async function saveSentences() {
  savingSentences.value = true
  try {
    await request(`/api/v1/admin/videos/${route.params.id}/sentences`, {
      method: 'PUT',
      body: { sentences: sentences.value }
    })
    message.success('句轴已保存')
  } catch (e: unknown) {
    message.error(e instanceof Error ? e.message : '保存失败')
  } finally {
    savingSentences.value = false
  }
}

async function generateZh() {
  zhLoading.value = true
  try {
    const drafts = await request<SentenceDraft[]>(
      `/api/v1/admin/videos/${route.params.id}/zh-draft`,
      { method: 'POST' }
    )
    sentences.value = drafts
    message.success('中文草稿已生成')
  } catch (e: unknown) {
    message.error(e instanceof Error ? e.message : '生成失败')
  } finally {
    zhLoading.value = false
  }
}

async function onCoverUpload(e: Event) {
  const f = (e.target as HTMLInputElement).files?.[0]
  if (!f) return
  const fd = new FormData()
  fd.append('file', f)
  try {
    const res = await uploadForm<{ url: string }>('/api/v1/admin/files/cover', fd)
    meta.coverUrl = res.url
    message.success('封面上传成功')
  } catch (err: unknown) {
    message.error(err instanceof Error ? err.message : '上传失败')
  }
}
</script>
