<template>
  <div class="space-y-4">
    <div class="flex items-center justify-between">
      <h1 class="text-xl font-bold">日报岛管理</h1>
      <NuxtLink to="/admin/daily/new">
        <NButton type="primary">新建日报</NButton>
      </NuxtLink>
    </div>
    <div class="flex gap-2">
      <NSelect
        v-model:value="statusFilter"
        :options="statusOptions"
        clearable
        placeholder="状态筛选"
        style="width: 160px"
        @update:value="load"
      />
    </div>
    <NDataTable :columns="columns" :data="rows" :loading="loading" />
  </div>
</template>

<script setup lang="ts">
import type { DataTableColumns } from 'naive-ui'
import type { AdminDailyArticleSummary } from '~/types/api'

definePageMeta({ layout: 'admin', middleware: 'admin', ssr: false })

const { request } = useApi()
const router = useRouter()
const rows = ref<AdminDailyArticleSummary[]>([])
const loading = ref(false)
const statusFilter = ref<string | null>(null)

const statusOptions = [
  { label: '草稿', value: 'draft' },
  { label: '待发', value: 'ready' },
  { label: '已发布', value: 'published' }
]

const topicLabel: Record<string, string> = {
  education: '教育',
  technology: '科技',
  environment: '环境',
  society_culture: '社会文化',
  economy_business: '经济',
  health: '健康',
  psychology: '心理',
  science: '科学'
}

const columns: DataTableColumns<AdminDailyArticleSummary> = [
  { title: 'ID', key: 'id', width: 60 },
  { title: '标题', key: 'title', ellipsis: { tooltip: true } },
  {
    title: '主题',
    key: 'topic',
    width: 90,
    render: (r) => topicLabel[r.topic] || r.topic
  },
  { title: '难度', key: 'difficulty', width: 70 },
  { title: '排期', key: 'publishDate', width: 110 },
  { title: '状态', key: 'status', width: 90 },
  { title: '词数', key: 'wordCount', width: 70 },
  {
    title: '操作',
    key: 'act',
    width: 160,
    render: (r) =>
      h('div', { class: 'flex gap-2' }, [
        h(
          'button',
          {
            class: 'text-sm text-[var(--island-primary)]',
            onClick: () => router.push(`/admin/daily/${r.id}`)
          },
          '编辑'
        ),
        h(
          'button',
          { class: 'text-sm text-gray-500', onClick: () => cycleStatus(r) },
          r.status === 'published' ? '下架为草稿' : '发布'
        )
      ])
  }
]

onMounted(load)

async function load() {
  loading.value = true
  try {
    const q = statusFilter.value ? `?status=${statusFilter.value}` : ''
    rows.value = await request<AdminDailyArticleSummary[]>(`/api/v1/admin/content/daily/articles${q}`)
  } finally {
    loading.value = false
  }
}

async function cycleStatus(row: AdminDailyArticleSummary) {
  const next = row.status === 'published' ? 'draft' : 'published'
  await request(`/api/v1/admin/content/daily/articles/${row.id}/status?status=${next}`, {
    method: 'PATCH'
  })
  await load()
}
</script>
