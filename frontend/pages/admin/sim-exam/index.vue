<template>
  <div class="space-y-6">
    <div class="flex flex-wrap items-center justify-between gap-3">
      <h1 class="text-xl font-bold">仿真题岛管理</h1>
      <div class="flex gap-2">
        <NuxtLink to="/admin/sim-exam/sources">
          <NButton>真题底稿</NButton>
        </NuxtLink>
        <NuxtLink to="/admin/sim-exam/sources/new">
          <NButton type="primary">录入底稿</NButton>
        </NuxtLink>
      </div>
    </div>

    <p class="text-sm text-gray-500">
      用户只看见已发布仿真卷。底稿仅 Admin 可见。AI 失败时只能重试，不可手工冒充发布。
    </p>

    <NDataTable :columns="columns" :data="rows" :loading="loading" />
  </div>
</template>

<script setup lang="ts">
import type { DataTableColumns } from 'naive-ui'
import type { AdminSimPassageSummary } from '~/types/api'

definePageMeta({ layout: 'admin', middleware: 'admin', ssr: false })

const { request } = useApi()
const router = useRouter()
const message = useAppMessage()
const rows = ref<AdminSimPassageSummary[]>([])
const loading = ref(false)

const columns: DataTableColumns<AdminSimPassageSummary> = [
  { title: 'ID', key: 'id', width: 60 },
  { title: '标题', key: 'title', ellipsis: { tooltip: true } },
  { title: '级别', key: 'examLevel', width: 70 },
  { title: '题型', key: 'sectionType', width: 120 },
  { title: '状态', key: 'status', width: 90 },
  { title: 'AI', key: 'aiStatus', width: 100 },
  { title: '词数', key: 'wordCount', width: 70 },
  {
    title: '操作',
    key: 'act',
    width: 180,
    render: (r) =>
      h('div', { class: 'flex gap-2' }, [
        h(
          'button',
          {
            class: 'text-sm text-[var(--island-primary)]',
            onClick: () => router.push(`/admin/sim-exam/passages/${r.id}`)
          },
          '详情'
        ),
        r.status !== 'published'
          ? h(
              'button',
              {
                class: 'text-sm text-amber-700',
                onClick: () => publish(r.id)
              },
              '发布'
            )
          : null
      ])
  }
]

onMounted(load)

async function load() {
  loading.value = true
  try {
    rows.value = await request<AdminSimPassageSummary[]>('/api/v1/admin/content/sim-exam/passages')
  } catch (e: unknown) {
    message.error(e instanceof Error ? e.message : '加载失败')
  } finally {
    loading.value = false
  }
}

async function publish(id: number) {
  try {
    await request(`/api/v1/admin/content/sim-exam/passages/${id}/publish`, { method: 'POST' })
    message.success('已发布')
    load()
  } catch (e: unknown) {
    message.error(e instanceof Error ? e.message : '发布失败')
  }
}
</script>
