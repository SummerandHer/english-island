<template>
  <NuxtLink :to="`/video/${video.id}`" class="vcard group">
    <div class="vcard__thumb">
      <img
        v-if="video.coverUrl"
        :src="coverSrc"
        :alt="video.title"
        loading="lazy"
        class="vcard__img"
      />
      <div v-else class="vcard__placeholder">
        <span class="vcard__play">Play</span>
      </div>

      <div class="vcard__scrim" />

      <span v-if="video.learned" class="vcard__badge vcard__badge--done">已学完</span>
      <span v-if="video.vip" class="vcard__badge vcard__badge--vip">VIP</span>
      <span v-if="video.favorited" class="vcard__fav" aria-label="已收藏">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
          <path d="M12 20.5s-6.5-4.1-9-7.8C1.2 10.2 2.1 7 5 6.2c1.8-.5 3.5.3 4.5 1.7C10.5 6.5 12.2 5.7 14 6.2c2.9.8 3.8 4 2 6.5-2.5 3.7-4 7.8-4 7.8z" />
        </svg>
      </span>

      <div class="vcard__thumb-meta">
        <span class="vcard__level" :title="difficultyLabel">{{ difficultyLabel }}</span>
        <span v-if="video.durationSec" class="vcard__dur">{{ formatDuration(video.durationSec) }}</span>
      </div>
    </div>

    <div class="vcard__body">
      <h3 class="vcard__title">{{ video.title }}</h3>
      <p v-if="video.description" class="vcard__desc">{{ video.description }}</p>

      <div v-if="video.tags?.length" class="vcard__tags">
        <span v-for="tag in video.tags" :key="tag.id || tag.slug" class="vcard__tag">
          {{ tag.name }}
        </span>
      </div>

      <div class="vcard__stats">
        <span>{{ video.sentenceCount }} 句</span>
        <span v-if="video.vocabCount">词汇 {{ video.vocabCount }}</span>
        <span v-if="video.createdAt" class="vcard__date">{{ formatDate(video.createdAt) }}</span>
      </div>
    </div>
  </NuxtLink>
</template>

<script setup lang="ts">
import type { VideoSummary } from '~/types/api'

const props = defineProps<{ video: VideoSummary }>()
const { apiBase } = useApi()

const coverSrc = computed(() => {
  const url = props.video.coverUrl
  if (!url) return ''
  return url.startsWith('http') ? url : `${apiBase}${url}`
})

const difficultyLabel = computed(() => {
  const map: Record<string, string> = { easy: '入门', medium: '进阶', hard: '挑战' }
  return map[props.video.difficulty] || props.video.difficulty
})

function formatDuration(sec: number) {
  const m = Math.floor(sec / 60)
  const s = sec % 60
  return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
}

function formatDate(iso: string) {
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return ''
  return `${d.getFullYear()}.${d.getMonth() + 1}.${d.getDate()}`
}
</script>

<style scoped>
.vcard {
  --card-ink: #1f2a24;
  --card-muted: #6b7a6f;
  --card-line: rgba(47, 67, 51, 0.08);
  display: block;
  overflow: hidden;
  border: 1px solid var(--card-line);
  border-radius: 18px;
  background: #fff;
  text-decoration: none;
  color: inherit;
  box-shadow: 0 1px 0 rgba(47, 67, 51, 0.03);
  animation: vcard-enter 0.5s ease both;
  animation-delay: var(--enter-delay, 0ms);
  transition:
    transform 0.28s ease,
    box-shadow 0.28s ease,
    border-color 0.28s ease;
}

.vcard:hover {
  transform: translateY(-3px);
  border-color: rgba(59, 83, 62, 0.16);
  box-shadow: 0 14px 32px rgba(31, 42, 36, 0.08);
}

.vcard__thumb {
  position: relative;
  aspect-ratio: 16 / 10;
  overflow: hidden;
  background: linear-gradient(145deg, #e8eee4, #d5ddd3);
}

.vcard__img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.55s ease;
}

.vcard:hover .vcard__img {
  transform: scale(1.04);
}

.vcard__placeholder {
  display: flex;
  height: 100%;
  width: 100%;
  align-items: center;
  justify-content: center;
}

.vcard__play {
  font-family: var(--font-display);
  font-size: 0.78rem;
  font-weight: 600;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: rgba(47, 67, 51, 0.45);
}

.vcard__scrim {
  position: absolute;
  inset: auto 0 0;
  height: 48%;
  background: linear-gradient(to top, rgba(18, 28, 22, 0.55), transparent);
  pointer-events: none;
}

.vcard__badge {
  position: absolute;
  top: 0.65rem;
  left: 0.65rem;
  z-index: 1;
  border-radius: 6px;
  padding: 0.18rem 0.45rem;
  font-size: 0.65rem;
  font-weight: 600;
  letter-spacing: 0.02em;
  color: #fff;
}

.vcard__badge--done {
  background: rgba(59, 83, 62, 0.92);
}

.vcard__badge--vip {
  left: auto;
  right: 0.65rem;
  background: rgba(140, 110, 55, 0.92);
}

.vcard__fav {
  position: absolute;
  top: 0.65rem;
  right: 0.65rem;
  z-index: 1;
  display: inline-flex;
  color: rgba(255, 255, 255, 0.95);
  filter: drop-shadow(0 1px 3px rgba(0, 0, 0, 0.28));
}

.vcard__badge--vip ~ .vcard__fav {
  top: 2.15rem;
}

.vcard__thumb-meta {
  position: absolute;
  inset: auto 0 0;
  z-index: 1;
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  padding: 0.65rem 0.7rem;
}

.vcard__level {
  font-size: 0.68rem;
  font-weight: 500;
  color: rgba(255, 255, 255, 0.88);
}

.vcard__dur {
  border-radius: 5px;
  background: rgba(255, 255, 255, 0.92);
  padding: 0.12rem 0.4rem;
  font-size: 0.65rem;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
  color: var(--card-ink);
}

.vcard__body {
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
  padding: 0.95rem 1rem 1.05rem;
}

.vcard__title {
  margin: 0;
  font-family: var(--font-display);
  font-size: 0.95rem;
  font-weight: 700;
  line-height: 1.4;
  color: var(--card-ink);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  transition: color 0.2s ease;
}

.vcard:hover .vcard__title {
  color: #3b533e;
}

.vcard__desc {
  margin: 0;
  font-size: 0.78rem;
  line-height: 1.55;
  color: var(--card-muted);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.vcard__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 0.3rem;
  padding-top: 0.15rem;
}

.vcard__tag {
  border-radius: 5px;
  background: #f1f4ee;
  padding: 0.15rem 0.45rem;
  font-size: 0.65rem;
  color: #5a6b60;
}

.vcard__stats {
  display: flex;
  flex-wrap: wrap;
  gap: 0.65rem;
  padding-top: 0.2rem;
  font-size: 0.7rem;
  color: #93a096;
}

.vcard__date {
  margin-left: auto;
  font-variant-numeric: tabular-nums;
}

@keyframes vcard-enter {
  from {
    opacity: 0;
    transform: translateY(12px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@media (prefers-reduced-motion: reduce) {
  .vcard,
  .vcard__img {
    animation: none;
    transition: none;
  }
}
</style>
