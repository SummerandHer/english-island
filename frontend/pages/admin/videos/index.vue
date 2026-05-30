<template>
  <div class="space-y-4">
    <div class="flex items-center justify-between">
      <h1 class="text-xl font-bold">双语视频管理</h1>
      <NuxtLink to="/admin/videos/new">
        <NButton type="primary">上传新视频</NButton>
      </NuxtLink>
    </div>

    <NSelect
      v-model:value="statusFilter"
      :options="statusOptions"
      placeholder="状态筛选"
      clearable
      class="max-w-xs"
      @update:value="load"
    />

    <NDataTable :columns="columns" :data="videos" :loading="loading" :bordered="false" />

    <div class="flex justify-end">
      <NPagination v-model:page="page" :page-count="pageCount" @update:page="load" />
    </div>
  </div>
</template>

<script setup lang="ts">
import type { DataTableColumns } from 'naive-ui'
import type { AdminVideoSummary } from '~/types/api'

definePageMeta({ layout: 'admin', middleware: 'admin', ssr: false })

const { request } = useApi()
const router = useRouter()

const videos = ref<AdminVideoSummary[]>([])
const loading = ref(false)
const page = ref(1)
const pageSize = 20
const total = ref(0)
const statusFilter = ref<number | null>(null)

const pageCount = computed(() => Math.max(1, Math.ceil(total.value / pageSize)))

const statusOptions = [
  { label: '已上架', value: 1 },
  { label: '已下架', value: 0 }
]

const columns: DataTableColumns<AdminVideoSummary> = [
  { title: 'ID', key: 'id', width: 70 },
  { title: '标题', key: 'title', ellipsis: { tooltip: true } },
  {
    title: '状态',
    key: 'status',
    width: 80,
    render: (row) => (row.status === 1 ? '上架' : '下架')
  },
  { title: '句数', key: 'sentenceCount', width: 70 },
  {
    title: '操作',
    key: 'actions',
    width: 100,
    render: (row) =>
      h(
        'button',
        {
          class: 'text-[var(--island-primary)] text-sm',
          onClick: () => router.push(`/admin/videos/${row.id}`)
        },
        '编辑'
      )
  }
]

onMounted(load)

async function load() {
  loading.value = true
  try {
    const q = new URLSearchParams({
      page: String(page.value),
      size: String(pageSize)
    })
    if (statusFilter.value != null) q.set('status', String(statusFilter.value))
    const res = await request<{ items: AdminVideoSummary[]; total: number }>(
      `/api/v1/admin/videos?${q}`
    )
    videos.value = res.items
    total.value = res.total
  } finally {
    loading.value = false
  }
}
</script>
