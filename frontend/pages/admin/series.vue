<template>
  <div class="space-y-4">
    <h1 class="text-xl font-bold">视频系列</h1>

    <section class="rounded-xl border bg-white p-4 space-y-3">
      <h2 class="font-medium">新建系列</h2>
      <NInput v-model:value="form.title" placeholder="系列名称" />
      <NInput v-model:value="form.description" type="textarea" placeholder="简介" />
      <NButton type="primary" :loading="creating" @click="create">创建</NButton>
    </section>

    <NDataTable :columns="columns" :data="series" :bordered="false" />
  </div>
</template>

<script setup lang="ts">
import type { DataTableColumns } from 'naive-ui'
import type { VideoSeriesItem } from '~/types/api'

definePageMeta({ layout: 'admin', middleware: 'admin', ssr: false })

const { request } = useApi()
const message = useAppMessage()

const series = ref<VideoSeriesItem[]>([])
const creating = ref(false)
const form = reactive({ title: '', description: '' })

const columns: DataTableColumns<VideoSeriesItem> = [
  { title: 'ID', key: 'id', width: 60 },
  { title: '名称', key: 'title' },
  { title: '排序', key: 'sortOrder', width: 80 }
]

onMounted(load)

async function load() {
  series.value = await request<VideoSeriesItem[]>('/api/v1/admin/video-series')
}

async function create() {
  if (!form.title.trim()) {
    message.warning('请填写系列名称')
    return
  }
  creating.value = true
  try {
    await request('/api/v1/admin/video-series', {
      method: 'POST',
      body: { title: form.title, description: form.description, sortOrder: 0, status: 1 }
    })
    form.title = ''
    form.description = ''
    message.success('已创建')
    await load()
  } catch (e: unknown) {
    message.error(e instanceof Error ? e.message : '创建失败')
  } finally {
    creating.value = false
  }
}
</script>
