<template>
  <div>
    <div class="mb-6 flex flex-wrap items-end justify-between gap-3">
      <div>
        <h1 class="text-xl font-bold text-gray-800">双语视频</h1>
        <p class="mt-1 text-sm text-gray-500">精选 Embed 视频，句级双语精听</p>
      </div>
      <p v-if="total > 0" class="text-sm text-gray-400">共 {{ total }} 个视频</p>
    </div>

    <div v-if="loading" class="flex min-h-[320px] items-center justify-center">
      <NSpin size="medium" />
    </div>

    <template v-else-if="videos.length">
      <div class="video-layout">
        <div v-if="featured" class="video-layout__featured">
          <VideoCard :video="featured" featured />
        </div>
        <div class="video-layout__grid">
          <VideoCard v-for="v in gridVideos" :key="v.id" :video="v" />
        </div>
      </div>

      <div v-if="pageCount > 1" class="mt-8 flex justify-center">
        <NPagination
          v-model:page="page"
          :page-count="pageCount"
          :page-size="pageSize"
          @update:page="loadVideos"
        />
      </div>
    </template>

    <div v-else class="island-card flex min-h-[240px] items-center justify-center text-gray-400">
      暂无视频
    </div>
  </div>
</template>

<script setup lang="ts">
import type { PageResult, VideoSummary } from '~/types/api'

const PAGE_SIZE = 7

const auth = useAuthStore()
const { request } = useApi()

const videos = ref<VideoSummary[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(PAGE_SIZE)
const loading = ref(true)

const featured = computed(() => videos.value[0] ?? null)
const gridVideos = computed(() => videos.value.slice(1))
const pageCount = computed(() => Math.max(1, Math.ceil(total.value / pageSize.value)))

onMounted(async () => {
  auth.hydrate()
  await loadVideos()
})

async function loadVideos() {
  loading.value = true
  try {
    const data = await request<PageResult<VideoSummary>>(
      `/api/v1/videos?page=${page.value}&size=${pageSize.value}`
    )
    videos.value = data.items
    total.value = data.total
    pageSize.value = data.size
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.video-layout {
  @apply flex flex-col gap-4 lg:flex-row;
}

.video-layout__featured {
  @apply w-full shrink-0 lg:w-[38%];
}

.video-layout__grid {
  @apply grid flex-1 grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3;
}
</style>
