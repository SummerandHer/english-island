<template>
  <div class="video-island">
    <header class="video-island__hero">
      <p class="video-island__eyebrow">Bilingual Island</p>
      <h1>丰富视频，精准筛选</h1>
      <p class="video-island__lead">
        按主题挑选内容，逐句精听 · 盲听 · 挖空，把听力练成习惯
      </p>
    </header>

    <div class="video-island__layout">
      <VideoIslandSidebar
        :overview="overview"
        :filter="filter"
        :tag-slug="tagSlug"
        @select-filter="onFilter"
        @select-tag="onTag"
        @clear="onClear"
      />

      <section class="video-island__main">
        <div class="video-island__toolbar">
          <div>
            <h2>精选视频</h2>
            <p class="video-island__count">共 {{ total }} 部作品</p>
          </div>
        </div>

        <div v-if="loading" class="video-island__state">
          <NSpin size="medium" />
        </div>

        <template v-else-if="videos.length">
          <div class="video-island__grid">
            <VideoCard
              v-for="(v, i) in videos"
              :key="v.id"
              :video="v"
              :style="{ '--enter-delay': `${Math.min(i, 8) * 40}ms` }"
            />
          </div>
          <div v-if="pageCount > 1" class="video-island__pager">
            <NPagination
              v-model:page="page"
              :page-count="pageCount"
              :page-size="pageSize"
              @update:page="loadVideos"
            />
          </div>
        </template>

        <div v-else class="video-island__empty">
          <p>暂无符合条件的视频</p>
          <button type="button" class="video-island__reset" @click="onClear">清除筛选</button>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { PageResult, VideoListFilter, VideoOverview, VideoSummary } from '~/types/api'

const PAGE_SIZE = 12

const auth = useAuthStore()
const { request } = useApi()

const videos = ref<VideoSummary[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(PAGE_SIZE)
const loading = ref(true)
const filter = ref<VideoListFilter>('all')
const tagSlug = ref<string | null>(null)

const overview = ref<VideoOverview>({
  totalCount: 0,
  learnedCount: 0,
  unlearnedCount: 0,
  favoritedCount: 0,
  historyCount: 0,
  tags: []
})

const pageCount = computed(() => Math.max(1, Math.ceil(total.value / pageSize.value)))

onMounted(async () => {
  auth.hydrate()
  await Promise.all([loadOverview(), loadVideos()])
})

async function loadOverview() {
  overview.value = await request<VideoOverview>('/api/v1/videos/overview')
}

async function loadVideos() {
  loading.value = true
  try {
    const q = new URLSearchParams({
      page: String(page.value),
      size: String(pageSize.value),
      filter: filter.value
    })
    if (tagSlug.value) q.set('tag', tagSlug.value)
    const data = await request<PageResult<VideoSummary>>(`/api/v1/videos?${q}`)
    videos.value = data.items.map((v) => ({
      ...v,
      tags: normalizeTags(v.tags)
    }))
    total.value = data.total
    pageSize.value = data.size
  } finally {
    loading.value = false
  }
}

function normalizeTags(tags: VideoSummary['tags']): VideoSummary['tags'] {
  if (!tags?.length) return []
  return tags.map((t, i) => {
    if (typeof t === 'string') {
      return { id: i, name: t, slug: t }
    }
    return t
  })
}

function onFilter(f: VideoListFilter) {
  if ((f === 'learned' || f === 'favorited' || f === 'history') && !auth.isLoggedIn) {
    navigateTo('/login')
    return
  }
  filter.value = f
  page.value = 1
  loadVideos()
}

function onTag(slug: string | null) {
  tagSlug.value = slug
  page.value = 1
  loadVideos()
}

function onClear() {
  filter.value = 'all'
  tagSlug.value = null
  page.value = 1
  loadVideos()
}
</script>

<style scoped>
.video-island {
  --vi-ink: #1f2a24;
  --vi-muted: #6b7a6f;
  --vi-line: rgba(47, 67, 51, 0.08);
  --vi-accent: #3b533e;
  --vi-accent-soft: rgba(209, 224, 201, 0.55);
  --vi-surface: #ffffff;
  max-width: 1180px;
  margin: 0 auto;
  padding: 0.25rem 0 3rem;
}

.video-island__hero {
  margin-bottom: 1.75rem;
  padding-bottom: 1.35rem;
  border-bottom: 1px solid var(--vi-line);
  animation: vi-rise 0.55s ease both;
}

.video-island__eyebrow {
  margin: 0 0 0.45rem;
  font-family: var(--font-display);
  font-size: 0.72rem;
  font-weight: 600;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  color: var(--vi-muted);
}

.video-island__hero h1 {
  margin: 0;
  font-family: var(--font-display);
  font-size: clamp(1.75rem, 3.2vw, 2.55rem);
  font-weight: 700;
  letter-spacing: -0.02em;
  line-height: 1.15;
  color: var(--vi-ink);
}

.video-island__lead {
  margin: 0.7rem 0 0;
  max-width: 34rem;
  font-size: 0.95rem;
  line-height: 1.65;
  color: var(--vi-muted);
}

.video-island__layout {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

@media (min-width: 768px) {
  .video-island__layout {
    flex-direction: row;
    align-items: flex-start;
    gap: 1.75rem;
  }
}

@media (min-width: 1024px) {
  .video-island__layout {
    gap: 2.25rem;
  }
}

.video-island__main {
  min-width: 0;
  flex: 1;
  animation: vi-rise 0.6s ease 0.08s both;
}

.video-island__toolbar {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  margin-bottom: 1.1rem;
}

.video-island__toolbar h2 {
  margin: 0;
  font-family: var(--font-display);
  font-size: 1.05rem;
  font-weight: 700;
  color: var(--vi-ink);
}

.video-island__count {
  margin: 0.2rem 0 0;
  font-size: 0.8rem;
  color: var(--vi-muted);
}

.video-island__grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 1.1rem;
}

@media (min-width: 640px) {
  .video-island__grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 1.2rem;
  }
}

@media (min-width: 1100px) {
  .video-island__grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 1.35rem;
  }
}

.video-island__pager {
  display: flex;
  justify-content: center;
  margin-top: 2rem;
}

.video-island__state,
.video-island__empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 0.75rem;
  min-height: 280px;
  border: 1px dashed var(--vi-line);
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.55);
  color: var(--vi-muted);
  font-size: 0.9rem;
}

.video-island__reset {
  border: 1px solid var(--vi-line);
  border-radius: 10px;
  background: #fff;
  padding: 0.4rem 0.9rem;
  font-size: 0.8rem;
  color: var(--vi-accent);
  cursor: pointer;
  transition: background 0.2s ease, border-color 0.2s ease;
}

.video-island__reset:hover {
  border-color: rgba(59, 83, 62, 0.25);
  background: var(--vi-accent-soft);
}

@keyframes vi-rise {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
