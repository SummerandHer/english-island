<template>
  <div class="space-y-4">
    <div class="flex items-center justify-between">
      <h1 class="text-xl font-bold">阅读模拟题管理</h1>
      <NuxtLink to="/admin/reading/new">
        <NButton type="primary">新建篇章</NButton>
      </NuxtLink>
    </div>
    <NDataTable :columns="columns" :data="rows" :loading="loading" />
  </div>
</template>

<script setup lang="ts">
import type { DataTableColumns } from 'naive-ui'

definePageMeta({ layout: 'admin', middleware: 'admin', ssr: false })

interface Row {
  id: number
  title: string
  difficulty: string
  questionCount: number
  wordCount: number
  status: number
}

const { request } = useApi()
const router = useRouter()
const rows = ref<Row[]>([])
const loading = ref(false)

const columns: DataTableColumns<Row> = [
  { title: 'ID', key: 'id', width: 60 },
  { title: '标题', key: 'title', ellipsis: { tooltip: true } },
  { title: '难度', key: 'difficulty', width: 70 },
  { title: '题数', key: 'questionCount', width: 60 },
  { title: '词数', key: 'wordCount', width: 70 },
  {
    title: '操作',
    key: 'act',
    width: 140,
    render: (r) =>
      h('div', { class: 'flex gap-2' }, [
        h(
          'button',
          { class: 'text-sm text-[var(--island-primary)]', onClick: () => router.push(`/admin/reading/${r.id}`) },
          '编辑'
        ),
        h(
          'button',
          { class: 'text-sm text-gray-500', onClick: () => toggleStatus(r) },
          r.status === 1 ? '下架' : '上架'
        )
      ])
  }
]

onMounted(load)

async function load() {
  loading.value = true
  try {
    rows.value = await request<Row[]>('/api/v1/admin/content/reading/passages')
  } finally {
    loading.value = false
  }
}

async function toggleStatus(row: Row) {
  const next = row.status === 1 ? 0 : 1
  await request(`/api/v1/admin/content/reading/passages/${row.id}/status?status=${next}`, {
    method: 'PATCH'
  })
  await load()
}
</script>
