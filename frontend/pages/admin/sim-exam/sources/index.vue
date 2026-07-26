<template>
  <div class="space-y-4">
    <div class="flex items-center justify-between">
      <div>
        <NuxtLink to="/admin/sim-exam" class="text-sm text-[var(--island-primary)]">← 仿真卷列表</NuxtLink>
        <h1 class="mt-2 text-xl font-bold">真题底稿</h1>
      </div>
      <NuxtLink to="/admin/sim-exam/sources/new">
        <NButton type="primary">新建底稿</NButton>
      </NuxtLink>
    </div>
    <NDataTable :columns="columns" :data="rows" :loading="loading" />
  </div>
</template>

<script setup lang="ts">
import type { DataTableColumns } from 'naive-ui'
import type { AdminSimSourceSummary } from '~/types/api'

definePageMeta({ layout: 'admin', middleware: 'admin', ssr: false })

const { request } = useApi()
const router = useRouter()
const rows = ref<AdminSimSourceSummary[]>([])
const loading = ref(false)

const columns: DataTableColumns<AdminSimSourceSummary> = [
  { title: 'ID', key: 'id', width: 60 },
  { title: '标题', key: 'title', ellipsis: { tooltip: true } },
  { title: '级别', key: 'examLevel', width: 70 },
  { title: '题型', key: 'sectionType', width: 120 },
  { title: '来源说明', key: 'sourceMeta', ellipsis: { tooltip: true } },
  {
    title: '操作',
    key: 'act',
    width: 100,
    render: (r) =>
      h(
        'button',
        {
          class: 'text-sm text-[var(--island-primary)]',
          onClick: () => router.push(`/admin/sim-exam/sources/${r.id}`)
        },
        '打开'
      )
  }
]

onMounted(async () => {
  loading.value = true
  try {
    rows.value = await request<AdminSimSourceSummary[]>('/api/v1/admin/content/sim-exam/sources')
  } finally {
    loading.value = false
  }
})
</script>
