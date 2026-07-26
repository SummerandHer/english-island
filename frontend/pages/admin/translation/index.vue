<template>
  <div class="space-y-4">
    <div class="flex items-center justify-between">
      <h1 class="text-xl font-bold">翻译题管理</h1>
      <NuxtLink to="/admin/translation/new">
        <NButton type="primary">新建题目</NButton>
      </NuxtLink>
    </div>
    <NDataTable :columns="columns" :data="rows" :loading="loading" />
  </div>
</template>

<script setup lang="ts">
import type { DataTableColumns } from 'naive-ui'
import { NButton, NTag } from 'naive-ui'

definePageMeta({ layout: 'admin', middleware: 'admin', ssr: false })

interface Row {
  id: number
  promptZh: string
  difficulty: string
  vip: boolean
  mock: boolean
  status: number
  sortOrder: number
}

const { request } = useApi()
const router = useRouter()
const rows = ref<Row[]>([])
const loading = ref(false)

const columns: DataTableColumns<Row> = [
  { title: 'ID', key: 'id', width: 60 },
  { title: '题干摘要', key: 'promptZh', ellipsis: { tooltip: true } },
  { title: '难度', key: 'difficulty', width: 70 },
  {
    title: '标签',
    key: 'tags',
    width: 120,
    render: (r) =>
      h('div', { class: 'flex gap-1' }, [
        r.mock ? h(NTag, { size: 'small' }, () => '模拟') : null,
        r.vip ? h(NTag, { size: 'small', type: 'warning' }, () => 'VIP') : null
      ])
  },
  {
    title: '状态',
    key: 'status',
    width: 80,
    render: (r) => (r.status === 1 ? '上架' : '下架')
  },
  {
    title: '操作',
    key: 'act',
    width: 140,
    render: (r) =>
      h('div', { class: 'flex gap-2' }, [
        h(
          'button',
          { class: 'text-sm text-[var(--island-primary)]', onClick: () => router.push(`/admin/translation/${r.id}`) },
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
    rows.value = await request<Row[]>('/api/v1/admin/content/translation/questions')
  } finally {
    loading.value = false
  }
}

async function toggleStatus(row: Row) {
  const next = row.status === 1 ? 0 : 1
  await request(`/api/v1/admin/content/translation/questions/${row.id}/status?status=${next}`, {
    method: 'PATCH'
  })
  await load()
}
</script>
