<template>
  <div class="space-y-6">
    <h1 class="text-xl font-bold">上传并解析视频</h1>

    <section class="rounded-xl border bg-white p-4 space-y-3">
      <h2 class="font-medium">1. 上传视频（ASR 自动提取英文字幕与时间轴）</h2>
      <input ref="fileInput" type="file" accept="video/*" class="text-sm" @change="onFileChange" />
      <div class="flex items-center gap-3">
        <NCheckbox v-model:checked="generateZh">同时生成 AI 中文草稿</NCheckbox>
        <NButton type="primary" :loading="parsing" :disabled="!selectedFile" @click="parseVideo">
          {{ parsing ? '解析中，请勿关闭页面…' : '开始解析' }}
        </NButton>
      </div>
      <div v-if="parseLogs.length" class="max-h-48 overflow-auto rounded bg-gray-900 p-3 font-mono text-xs text-green-400">
        <div v-for="(line, i) in parseLogs" :key="i">{{ line }}</div>
      </div>
    </section>

    <section v-if="sentences.length" class="rounded-xl border bg-white p-4 space-y-3">
      <h2 class="font-medium">2. 预览 / 编辑句轴（共 {{ sentences.length }} 句）</h2>
      <div class="space-y-3 max-h-[420px] overflow-auto">
        <div
          v-for="(s, idx) in sentences"
          :key="idx"
          class="grid gap-2 rounded-lg border p-3 md:grid-cols-[80px_1fr_1fr]"
        >
          <div class="text-xs text-gray-500">
            #{{ s.seq }}<br />
            {{ formatMs(s.startMs) }} - {{ formatMs(s.endMs) }}
          </div>
          <NInput v-model:value="s.textEn" type="textarea" :rows="2" placeholder="英文" />
          <NInput v-model:value="s.textZh" type="textarea" :rows="2" placeholder="中文（可留空）" />
        </div>
      </div>
    </section>

    <section v-if="sentences.length" class="rounded-xl border bg-white p-4 space-y-4">
      <h2 class="font-medium">3. 填写信息并发布</h2>
      <NForm :model="meta" label-placement="left" label-width="100">
        <NFormItem label="标题" required>
          <NInput v-model:value="meta.title" placeholder="视频标题" />
        </NFormItem>
        <NFormItem label="封面 URL" required>
          <div class="flex w-full flex-col gap-2 sm:flex-row">
            <NInput v-model:value="meta.coverUrl" placeholder="上传后自动填入或粘贴 URL" />
            <input ref="coverInput" type="file" accept="image/*" class="text-sm" @change="onCoverUpload" />
          </div>
        </NFormItem>
        <NFormItem label="系列">
          <NSelect v-model:value="meta.seriesId" :options="seriesOptions" clearable placeholder="选择系列" />
        </NFormItem>
        <NFormItem label="简介">
          <NInput v-model:value="meta.description" type="textarea" :rows="2" />
        </NFormItem>
        <NFormItem label="难度">
          <NSelect v-model:value="meta.difficulty" :options="difficultyOptions" />
        </NFormItem>
        <NFormItem label="VIP 专属">
          <NSwitch v-model:value="meta.isVip" />
        </NFormItem>
        <NFormItem label="立即上架">
          <NSwitch v-model:value="meta.publish" />
        </NFormItem>
      </NForm>
      <NButton type="primary" size="large" :loading="publishing" @click="publish">
        确认发布
      </NButton>
    </section>
  </div>
</template>

<script setup lang="ts">
import type { ParseVideoResult, SentenceDraft, VideoSeriesItem } from '~/types/api'

definePageMeta({ layout: 'admin', middleware: 'admin', ssr: false })

const message = useAppMessage()
const { request, uploadForm } = useApi()

const fileInput = ref<HTMLInputElement | null>(null)
const coverInput = ref<HTMLInputElement | null>(null)
const selectedFile = ref<File | null>(null)
const parsing = ref(false)
const publishing = ref(false)
const generateZh = ref(true)
const parseLogs = ref<string[]>([])

const videoObjectKey = ref('')
const playUrl = ref('')

const sentences = ref<SentenceDraft[]>([])

const meta = reactive({
  title: '',
  coverUrl: '',
  description: '',
  seriesId: null as number | null,
  difficulty: 'medium',
  isVip: false,
  publish: true
})

const seriesOptions = ref<{ label: string; value: number }[]>([])
const difficultyOptions = [
  { label: '简单', value: 'easy' },
  { label: '中等', value: 'medium' },
  { label: '困难', value: 'hard' }
]

onMounted(async () => {
  const list = await request<VideoSeriesItem[]>('/api/v1/admin/video-series')
  seriesOptions.value = list.map((s) => ({ label: s.title, value: s.id }))
})

function onFileChange(e: Event) {
  const f = (e.target as HTMLInputElement).files?.[0]
  selectedFile.value = f ?? null
}

async function parseVideo() {
  if (!selectedFile.value) return
  parsing.value = true
  parseLogs.value = ['[前端] 开始上传并等待服务端解析…']
  try {
    const fd = new FormData()
    fd.append('file', selectedFile.value)
    const res = await uploadForm<ParseVideoResult>(
      `/api/v1/admin/videos/parse?generateZh=${generateZh.value}`,
      fd
    )
    videoObjectKey.value = res.videoObjectKey
    playUrl.value = res.playUrl
    sentences.value = res.sentences.map((s) => ({ ...s }))
    parseLogs.value = [...parseLogs.value, ...res.logs, '[前端] 解析完成，请检查句轴后发布']
    if (!meta.title) {
      meta.title = selectedFile.value.name.replace(/\.[^.]+$/, '')
    }
    message.success(`解析完成，共 ${res.sentences.length} 句`)
  } catch (e: unknown) {
    message.error(e instanceof Error ? e.message : '解析失败')
  } finally {
    parsing.value = false
  }
}

async function onCoverUpload(e: Event) {
  const f = (e.target as HTMLInputElement).files?.[0]
  if (!f) return
  const fd = new FormData()
  fd.append('file', f)
  try {
    const res = await uploadForm<{ url: string }>('/api/v1/admin/files/cover', fd, 120_000)
    meta.coverUrl = res.url
    message.success('封面上传成功')
  } catch (err: unknown) {
    message.error(err instanceof Error ? err.message : '封面上传失败')
  }
}

async function publish() {
  if (!meta.title.trim()) {
    message.warning('请填写标题')
    return
  }
  if (!meta.coverUrl.trim()) {
    message.warning('请上传或填写封面')
    return
  }
  if (!videoObjectKey.value) {
    message.warning('请先完成视频解析')
    return
  }
  publishing.value = true
  try {
    const res = await request<{ id: number }>('/api/v1/admin/videos', {
      method: 'POST',
      body: {
        title: meta.title,
        coverUrl: meta.coverUrl,
        videoObjectKey: videoObjectKey.value,
        playUrl: playUrl.value,
        description: meta.description,
        seriesId: meta.seriesId,
        difficulty: meta.difficulty,
        isVip: meta.isVip ? 1 : 0,
        status: meta.publish ? 1 : 0,
        sentences: sentences.value
      }
    })
    message.success('发布成功')
    await navigateTo(`/admin/videos/${res.id}`)
  } catch (e: unknown) {
    message.error(e instanceof Error ? e.message : '发布失败')
  } finally {
    publishing.value = false
  }
}

function formatMs(ms: number) {
  const s = Math.floor(ms / 1000)
  const m = Math.floor(s / 60)
  const sec = s % 60
  return `${m}:${String(sec).padStart(2, '0')}`
}
</script>
