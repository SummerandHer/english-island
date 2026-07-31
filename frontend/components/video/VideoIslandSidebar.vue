<template>
  <aside class="video-aside">
    <div class="stat-grid">
      <button
        type="button"
        class="stat-card"
        :class="{ 'stat-card--active': filter === 'all' }"
        @click="emit('select-filter', 'all')"
      >
        <span class="stat-card__label">当前期数</span>
        <span class="stat-card__value">{{ overview.totalCount }}</span>
        <span class="stat-card__sub">持续更新</span>
      </button>

      <button
        type="button"
        class="stat-card"
        :class="{ 'stat-card--active': filter === 'learned' }"
        @click="emit('select-filter', 'learned')"
      >
        <span class="stat-card__label">已学习</span>
        <span class="stat-card__value">{{ overview.learnedCount }}</span>
        <span class="stat-card__sub">我的打卡</span>
      </button>

      <button
        type="button"
        class="stat-card"
        :class="{ 'stat-card--active': filter === 'favorited' }"
        @click="emit('select-filter', 'favorited')"
      >
        <span class="stat-card__label">已收藏</span>
        <span class="stat-card__value">{{ overview.favoritedCount }}</span>
        <span class="stat-card__sub">收藏夹</span>
      </button>

      <button
        type="button"
        class="stat-card"
        :class="{ 'stat-card--active': filter === 'history' }"
        @click="emit('select-filter', 'history')"
      >
        <span class="stat-card__label">观看历史</span>
        <span class="stat-card__value">{{ overview.historyCount }}</span>
        <span class="stat-card__sub">最近学过</span>
      </button>
    </div>

    <div class="filter-block">
      <div class="filter-block__head">
        <h3>筛选条件</h3>
        <button type="button" class="clear-btn" @click="emit('clear')">清除</button>
      </div>

      <p class="filter-label">主题</p>
      <div class="tag-cloud">
        <button
          type="button"
          class="tag-pill"
          :class="{ 'tag-pill--active': !tagSlug }"
          @click="emit('select-tag', null)"
        >
          全部
        </button>
        <button
          v-for="t in overview.tags"
          :key="t.id"
          type="button"
          class="tag-pill"
          :class="{ 'tag-pill--active': tagSlug === t.slug }"
          @click="emit('select-tag', t.slug)"
        >
          {{ t.name }}
        </button>
      </div>
    </div>
  </aside>
</template>

<script setup lang="ts">
import type { VideoListFilter, VideoOverview } from '~/types/api'

defineProps<{
  overview: VideoOverview
  filter: VideoListFilter
  tagSlug: string | null
}>()

const emit = defineEmits<{
  'select-filter': [filter: VideoListFilter]
  'select-tag': [slug: string | null]
  clear: []
}>()
</script>

<style scoped>
.video-aside {
  --aside-ink: #1f2a24;
  --aside-muted: #6b7a6f;
  --aside-line: rgba(47, 67, 51, 0.09);
  --aside-soft: rgba(232, 238, 228, 0.65);
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

@media (min-width: 768px) {
  .video-aside {
    position: sticky;
    top: 1rem;
    width: 220px;
    flex-shrink: 0;
  }
}

@media (min-width: 1024px) {
  .video-aside {
    width: 236px;
  }
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.65rem;
}

.stat-card {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 0.15rem;
  padding: 0.9rem 0.85rem;
  border: 1px solid var(--aside-line);
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.72);
  text-align: left;
  cursor: pointer;
  transition:
    border-color 0.2s ease,
    background 0.2s ease,
    transform 0.2s ease;
}

.stat-card:hover {
  border-color: rgba(59, 83, 62, 0.22);
  background: #fff;
  transform: translateY(-1px);
}

.stat-card--active {
  border-color: transparent;
  background: linear-gradient(160deg, #eef4ea 0%, #f7faf5 100%);
  box-shadow: inset 0 0 0 1px rgba(59, 83, 62, 0.18);
}

.stat-card__label {
  font-size: 0.7rem;
  letter-spacing: 0.02em;
  color: var(--aside-muted);
}

.stat-card__value {
  font-family: var(--font-display);
  font-size: 1.55rem;
  font-weight: 700;
  line-height: 1.1;
  letter-spacing: -0.03em;
  color: var(--aside-ink);
}

.stat-card__sub {
  font-size: 0.68rem;
  color: #93a096;
}

.filter-block {
  padding: 1rem 1rem 1.1rem;
  border: 1px solid var(--aside-line);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.78);
}

.filter-block__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 0.85rem;
}

.filter-block__head h3 {
  margin: 0;
  font-family: var(--font-display);
  font-size: 0.88rem;
  font-weight: 700;
  color: var(--aside-ink);
}

.clear-btn {
  border: none;
  background: transparent;
  padding: 0;
  font-size: 0.72rem;
  color: var(--aside-muted);
  cursor: pointer;
}

.clear-btn:hover {
  color: var(--aside-ink);
}

.filter-label {
  margin: 0 0 0.55rem;
  font-size: 0.7rem;
  letter-spacing: 0.04em;
  color: var(--aside-muted);
}

.tag-cloud {
  display: flex;
  flex-wrap: wrap;
  gap: 0.4rem;
}

.tag-pill {
  border: 1px solid var(--aside-line);
  border-radius: 8px;
  background: #fff;
  padding: 0.32rem 0.65rem;
  font-size: 0.74rem;
  color: #4a5a50;
  cursor: pointer;
  transition:
    background 0.18s ease,
    border-color 0.18s ease,
    color 0.18s ease;
}

.tag-pill:hover {
  border-color: rgba(59, 83, 62, 0.28);
  background: var(--aside-soft);
}

.tag-pill--active {
  border-color: #3b533e;
  background: #3b533e;
  color: #f5f8f3;
}
</style>
