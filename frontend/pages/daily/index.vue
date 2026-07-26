<template>
  <div class="daily-hub">
    <header class="hero">
      <div>
        <p class="eyebrow">Daily Island · 周一 / 三 / 五</p>
        <h1>日报岛</h1>
        <p class="lead">读完一篇外刊风格短文，积累句式与词汇，把备考变成温柔的习惯。</p>
      </div>
      <div class="streak">
        <span class="streak-num">{{ hub?.streakDays ?? 0 }}</span>
        <span class="streak-label">连续打卡<br />（发布日）</span>
      </div>
    </header>

    <div v-if="hub?.todayArticle?.id" class="today-cta">
      <div>
        <p class="cta-kicker">今日精读</p>
        <p class="cta-title">{{ hub.todayArticle.title }}</p>
        <p class="cta-meta">{{ hub.todayArticle.topicLabel }} · {{ hub.todayArticle.weekdayLabel }}</p>
      </div>
      <NuxtLink :to="`/daily/${hub.todayArticle.id}`" class="cta-btn">
        {{ hub.todayArticle.checkedIn ? '再读一遍' : '开启今日日报' }}
      </NuxtLink>
    </div>

    <section class="week">
      <h2>本周日程</h2>
      <div class="week-grid">
        <article
          v-for="slot in hub?.weekSlots || []"
          :key="slot.publishDate"
          class="slot"
          :class="{ locked: !slot.unlocked, done: slot.checkedIn }"
        >
          <p class="slot-day">{{ slot.weekdayLabel }}</p>
          <p class="slot-date">{{ slot.publishDate }}</p>
          <template v-if="slot.id && slot.unlocked">
            <NuxtLink :to="`/daily/${slot.id}`" class="slot-title">{{ slot.title }}</NuxtLink>
            <p class="slot-meta">{{ slot.topicLabel }} · {{ slot.difficulty?.toUpperCase() }}</p>
            <span v-if="slot.checkedIn" class="badge">已打卡</span>
          </template>
          <template v-else-if="!slot.unlocked">
            <p class="slot-title muted">尚未开放</p>
            <p class="slot-meta">到点再来登岛</p>
          </template>
          <template v-else>
            <p class="slot-title muted">暂无排期</p>
          </template>
        </article>
      </div>
    </section>

    <section class="filters">
      <button
        type="button"
        class="chip"
        :class="{ active: !topic }"
        @click="topic = ''"
      >全部主题</button>
      <button
        v-for="t in hub?.topics || []"
        :key="t.slug"
        type="button"
        class="chip"
        :class="{ active: topic === t.slug }"
        @click="topic = t.slug"
      >{{ t.label }}</button>
    </section>

    <section class="archive">
      <h2>往期日报</h2>
      <div v-if="pending" class="muted">加载中…</div>
      <div v-else-if="!filteredArchive.length" class="muted">这一主题暂时还没有文章。</div>
      <ul v-else class="list">
        <li v-for="item in filteredArchive" :key="item.id!">
          <NuxtLink :to="`/daily/${item.id}`" class="row">
            <img
              v-if="item.coverUrl"
              :src="item.coverUrl"
              alt=""
              class="thumb"
              width="72"
              height="56"
            />
            <div class="row-body">
              <p class="row-title">{{ item.title }}</p>
              <p class="row-meta">
                {{ item.weekdayLabel }} · {{ item.publishDate }} · {{ item.topicLabel }}
                <span v-if="item.checkedIn"> · 已读</span>
              </p>
            </div>
          </NuxtLink>
        </li>
      </ul>
    </section>
  </div>
</template>

<script setup lang="ts">
import type { DailyHubPayload } from '~/types/api'

definePageMeta({ ssr: false })

const { request } = useApi()
const topic = ref('')
const hub = ref<DailyHubPayload | null>(null)
const pending = ref(true)
const error = ref('')

const filteredArchive = computed(() => hub.value?.archive || [])

async function load() {
  pending.value = true
  error.value = ''
  try {
    const q = topic.value ? `?topic=${encodeURIComponent(topic.value)}` : ''
    hub.value = await request<DailyHubPayload>(`/api/v1/daily/hub${q}`)
  } catch (e) {
    error.value = e instanceof Error ? e.message : '加载失败'
  } finally {
    pending.value = false
  }
}

watch(topic, () => load())
onMounted(load)
</script>

<style scoped>
.daily-hub {
  max-width: 960px;
  margin: 0 auto;
  padding-bottom: 2rem;
}

.hero {
  display: flex;
  justify-content: space-between;
  gap: 1.5rem;
  align-items: flex-end;
  padding: 0.5rem 0 1.25rem;
}

.eyebrow {
  margin: 0;
  font-size: 0.78rem;
  letter-spacing: 0.06em;
  color: var(--island-muted);
  text-transform: uppercase;
}

h1 {
  margin: 0.25rem 0;
  font-family: var(--font-display);
  font-size: clamp(1.8rem, 3vw, 2.4rem);
  font-weight: 700;
  color: var(--island-forest-deep);
}

.lead {
  margin: 0;
  max-width: 28rem;
  color: var(--island-muted);
  font-size: 0.95rem;
  line-height: 1.6;
}

.streak {
  display: flex;
  align-items: baseline;
  gap: 0.5rem;
  padding: 0.85rem 1.1rem;
  border-radius: 18px;
  background: linear-gradient(145deg, #f7faf4, #e4efe0);
  border: 1px solid rgba(59, 83, 62, 0.08);
}

.streak-num {
  font-family: var(--font-display);
  font-size: 2.2rem;
  font-weight: 700;
  color: var(--island-forest);
  line-height: 1;
}

.streak-label {
  font-size: 0.75rem;
  color: var(--island-muted);
  line-height: 1.3;
}

.today-cta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  margin-bottom: 1.5rem;
  padding: 1.1rem 1.25rem;
  border-radius: 20px;
  background:
    linear-gradient(120deg, rgba(209, 224, 201, 0.55), rgba(248, 249, 244, 0.9)),
    var(--island-card);
  border: 1px solid rgba(59, 83, 62, 0.08);
}

.cta-kicker {
  margin: 0;
  font-size: 0.75rem;
  color: var(--island-muted);
}

.cta-title {
  margin: 0.2rem 0;
  font-size: 1.15rem;
  font-weight: 700;
}

.cta-meta {
  margin: 0;
  font-size: 0.82rem;
  color: var(--island-muted);
}

.cta-btn {
  display: inline-flex;
  align-items: center;
  padding: 0.65rem 1.2rem;
  border-radius: 999px;
  background: var(--island-forest);
  color: #fff;
  font-weight: 600;
  font-size: 0.9rem;
  text-decoration: none;
}

.week h2,
.archive h2 {
  margin: 0 0 0.75rem;
  font-size: 1.05rem;
  font-weight: 700;
}

.week-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 0.75rem;
  margin-bottom: 1.5rem;
}

@media (max-width: 720px) {
  .week-grid { grid-template-columns: 1fr; }
  .hero { flex-direction: column; align-items: flex-start; }
}

.slot {
  position: relative;
  padding: 1rem;
  border-radius: 16px;
  background: var(--island-card);
  border: 1px solid rgba(59, 83, 62, 0.07);
  min-height: 8.5rem;
}

.slot.locked {
  opacity: 0.72;
  background: var(--island-surface);
}

.slot-day {
  margin: 0;
  font-size: 0.78rem;
  font-weight: 700;
  color: var(--island-forest);
}

.slot-date {
  margin: 0.15rem 0 0.55rem;
  font-size: 0.72rem;
  color: var(--island-muted);
}

.slot-title {
  display: block;
  margin: 0;
  font-size: 0.95rem;
  font-weight: 600;
  color: var(--island-text);
  text-decoration: none;
  line-height: 1.4;
}

.slot-title.muted { color: var(--island-muted); font-weight: 500; }

.slot-meta {
  margin: 0.35rem 0 0;
  font-size: 0.75rem;
  color: var(--island-muted);
}

.badge {
  position: absolute;
  top: 0.75rem;
  right: 0.75rem;
  font-size: 0.68rem;
  padding: 0.15rem 0.45rem;
  border-radius: 999px;
  background: rgba(140, 168, 120, 0.35);
  color: var(--island-forest-deep);
}

.filters {
  display: flex;
  flex-wrap: wrap;
  gap: 0.45rem;
  margin-bottom: 1.25rem;
}

.chip {
  border: 1px solid var(--island-line);
  background: #fff;
  color: var(--island-muted);
  border-radius: 999px;
  padding: 0.35rem 0.75rem;
  font-size: 0.8rem;
  cursor: pointer;
}

.chip.active {
  background: var(--island-sage-soft);
  border-color: transparent;
  color: var(--island-forest-deep);
  font-weight: 600;
}

.list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 0.55rem;
}

.row {
  display: flex;
  gap: 0.85rem;
  align-items: center;
  padding: 0.7rem 0.85rem;
  border-radius: 14px;
  background: var(--island-card);
  text-decoration: none;
  color: inherit;
  border: 1px solid rgba(59, 83, 62, 0.06);
}

.thumb {
  width: 72px;
  height: 56px;
  object-fit: cover;
  border-radius: 10px;
  flex-shrink: 0;
}

.row-title {
  margin: 0;
  font-weight: 600;
  font-size: 0.95rem;
}

.row-meta {
  margin: 0.2rem 0 0;
  font-size: 0.75rem;
  color: var(--island-muted);
}

.muted { color: var(--island-muted); font-size: 0.9rem; }
</style>
