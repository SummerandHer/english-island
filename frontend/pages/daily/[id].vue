<template>
  <div class="reader" :class="{ 'reader--focus': focusMode }">
    <div v-if="loading" class="state">加载中…</div>
    <div v-else-if="error" class="state err">{{ error }}</div>
    <template v-else-if="article">
      <div class="cover-wrap">
        <img
          v-if="article.coverUrl"
          :src="article.coverUrl"
          :alt="article.title"
          class="cover"
        />
        <div class="cover-fade" />
      </div>

      <div class="toolbar-top">
        <NuxtLink v-if="!focusMode" to="/daily" class="back">← 日报岛</NuxtLink>
        <button type="button" class="focus-btn" @click="toggleFocus">
          {{ focusMode ? '退出全屏' : '全屏阅读' }}
        </button>
      </div>

      <header class="head">
        <div class="tags">
          <span class="tag">{{ article.topicLabel }}</span>
          <span class="tag soft">{{ article.difficulty.toUpperCase() }}</span>
          <span class="tag soft">{{ article.wordCount || '—' }} 词</span>
        </div>
        <h1>{{ article.title }}</h1>
        <p v-if="article.summaryZh" class="summary">{{ article.summaryZh }}</p>
        <p class="meta">
          <span v-if="article.sourceAuthor">作者 {{ article.sourceAuthor }}</span>
          <span v-if="article.sourcePublishedAt"> · {{ article.sourcePublishedAt }}</span>
          <span v-if="article.sourcePlace"> · {{ article.sourcePlace }}</span>
          <span> · 排期 {{ article.publishDate }}</span>
        </p>
      </header>

      <div class="layout">
        <div class="main">
          <p class="hint">划选单词或句子即可标注（苔绿 / 琥珀 / 天空色）</p>
          <DailyAnnotatableText
            :content="article.contentEn"
            :annotations="article.annotations"
            @annotate="onAnnotate"
          />

          <section v-if="article.cetVocab?.length" class="panel">
            <h2>高频词汇</h2>
            <ul class="vocab">
              <li v-for="(v, i) in article.cetVocab" :key="i">
                <strong>{{ v.word }}</strong>
                <span v-if="v.pos" class="pos">{{ v.pos }}</span>
                <span>{{ v.zh }}</span>
              </li>
            </ul>
          </section>

          <section v-if="article.hardVocab?.length" class="panel">
            <h2>难词拆分</h2>
            <ul class="vocab">
              <li v-for="(v, i) in article.hardVocab" :key="i">
                <strong>{{ v.word }}</strong>
                <span v-if="v.pos" class="pos">{{ v.pos }}</span>
                <span>{{ v.zh }}</span>
                <em v-if="v.note">{{ v.note }}</em>
              </li>
            </ul>
          </section>

          <div class="checkin-bar">
            <p>
              {{ article.checkedIn ? '今日已打卡，继续保持。' : `阅读约 ${Math.max(0, Math.floor(readSeconds / 60))} 分 ${readSeconds % 60} 秒` }}
            </p>
            <button
              type="button"
              class="checkin-btn"
              :disabled="article.checkedIn || checking || !canCheckin"
              @click="doCheckin"
            >
              {{ article.checkedIn ? '已完成' : canCheckin ? '完成今日阅读' : `再读 ${90 - readSeconds}s` }}
            </button>
            <p v-if="checkinMsg" class="checkin-msg">{{ checkinMsg }}</p>
          </div>

          <p class="bridge">
            <NuxtLink to="/translation">摘一句去翻译岛练手 →</NuxtLink>
          </p>
        </div>

        <aside v-if="!focusMode" class="side">
          <section class="side-card">
            <h3>可借用句式</h3>
            <div v-if="!article.structures?.length" class="muted">暂无</div>
            <article v-for="(s, i) in article.structures" :key="i" class="struct">
              <p class="struct-en">{{ s.en }}</p>
              <p v-if="s.zh" class="struct-zh">{{ s.zh }}</p>
              <p v-if="s.hint" class="struct-hint">{{ s.hint }}</p>
            </article>
          </section>

          <section class="side-card">
            <h3>相关文章</h3>
            <ul v-if="article.related?.length" class="related">
              <li v-for="r in article.related" :key="r.id!">
                <NuxtLink :to="`/daily/${r.id}`">{{ r.title }}</NuxtLink>
                <span>{{ r.publishDate }}</span>
              </li>
            </ul>
            <p v-else class="muted">同主题暂无更多文章</p>
          </section>

          <section v-if="article.annotations?.length" class="side-card">
            <h3>我的标注</h3>
            <ul class="ann-list">
              <li v-for="a in article.annotations" :key="a.id">
                <span :class="['dot', `dot--${a.color}`]" />
                <span class="ann-text">{{ a.selectedText }}</span>
                <button type="button" class="ann-del" @click="removeAnn(a.id)">删除</button>
              </li>
            </ul>
          </section>
        </aside>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import type { DailyArticleDetail } from '~/types/api'

definePageMeta({ ssr: false })

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const { request } = useApi()

const article = ref<DailyArticleDetail | null>(null)
const loading = ref(true)
const error = ref('')
const checking = ref(false)
const checkinMsg = ref('')
const readSeconds = ref(0)
let timer: ReturnType<typeof setInterval> | null = null

const focusMode = computed(() => String(route.query.focus || '') === '1')
const canCheckin = computed(() => readSeconds.value >= 90)

watch(
  focusMode,
  (v) => {
    setPageLayout(v ? 'focus' : 'default')
  },
  { immediate: true }
)

async function load() {
  loading.value = true
  error.value = ''
  try {
    const id = Number(route.params.id)
    article.value = await request<DailyArticleDetail>(`/api/v1/daily/articles/${id}`)
  } catch (e) {
    error.value = e instanceof Error ? e.message : '加载失败'
    article.value = null
  } finally {
    loading.value = false
  }
}

function toggleFocus() {
  const q = { ...route.query }
  if (focusMode.value) {
    delete q.focus
  } else {
    q.focus = '1'
  }
  router.replace({ query: q })
}

async function onAnnotate(payload: {
  startOffset: number
  endOffset: number
  selectedText: string
  color: string
}) {
  if (!auth.isLoggedIn) {
    checkinMsg.value = '登录后即可保存标注'
    navigateTo('/login')
    return
  }
  if (!article.value) return
  try {
    const created = await request<DailyArticleDetail['annotations'][number]>(
      `/api/v1/daily/articles/${article.value.id}/annotations`,
      { method: 'POST', body: payload }
    )
    article.value.annotations = [...article.value.annotations, created].sort(
      (a, b) => a.startOffset - b.startOffset
    )
  } catch (e) {
    checkinMsg.value = e instanceof Error ? e.message : '标注失败'
  }
}

async function removeAnn(id: number) {
  if (!article.value || !auth.isLoggedIn) return
  try {
    await request(`/api/v1/daily/articles/${article.value.id}/annotations/${id}`, {
      method: 'DELETE'
    })
    article.value.annotations = article.value.annotations.filter((a) => a.id !== id)
  } catch (e) {
    checkinMsg.value = e instanceof Error ? e.message : '删除失败'
  }
}

async function doCheckin() {
  if (!article.value || !auth.isLoggedIn) {
    navigateTo('/login')
    return
  }
  if (!canCheckin.value) return
  checking.value = true
  checkinMsg.value = ''
  try {
    await request(`/api/v1/daily/articles/${article.value.id}/checkin`, {
      method: 'POST',
      body: { readSeconds: readSeconds.value }
    })
    article.value.checkedIn = true
    checkinMsg.value = '打卡成功，明天见（或下一个发布日）'
  } catch (e) {
    checkinMsg.value = e instanceof Error ? e.message : '打卡失败'
  } finally {
    checking.value = false
  }
}

onMounted(() => {
  auth.hydrate()
  load()
  timer = setInterval(() => {
    if (!article.value?.checkedIn) {
      readSeconds.value += 1
    }
  }, 1000)
})

onBeforeUnmount(() => {
  if (timer) clearInterval(timer)
  setPageLayout('default')
})

watch(() => route.params.id, () => {
  readSeconds.value = 0
  checkinMsg.value = ''
  load()
})
</script>

<style scoped>
.reader {
  max-width: 1100px;
  margin: 0 auto;
  padding-bottom: 3rem;
}

.reader--focus {
  max-width: 720px;
  padding: 1.25rem 1.25rem 3rem;
}

.state {
  padding: 2rem;
  color: var(--island-muted);
}
.err { color: #b45353; }

.cover-wrap {
  position: relative;
  margin: 0 -0.25rem 1rem;
  border-radius: 22px;
  overflow: hidden;
  aspect-ratio: 21 / 8;
  background: linear-gradient(135deg, #d1e0c9, #e8eee4 50%, #c2d6be);
}

.reader--focus .cover-wrap {
  margin: 0 0 1rem;
  aspect-ratio: 16 / 7;
}

.cover {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.cover-fade {
  position: absolute;
  inset: auto 0 0;
  height: 45%;
  background: linear-gradient(transparent, rgba(232, 238, 228, 0.85));
  pointer-events: none;
}

.toolbar-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.75rem;
}

.back {
  color: var(--island-muted);
  text-decoration: none;
  font-size: 0.88rem;
}

.focus-btn {
  border: 1px solid var(--island-line);
  background: #fff;
  border-radius: 999px;
  padding: 0.35rem 0.85rem;
  font-size: 0.82rem;
  color: var(--island-forest);
  cursor: pointer;
}

.head h1 {
  margin: 0.4rem 0;
  font-family: var(--font-display);
  font-size: clamp(1.55rem, 3vw, 2.1rem);
  line-height: 1.25;
  color: var(--island-forest-deep);
}

.tags {
  display: flex;
  flex-wrap: wrap;
  gap: 0.4rem;
}

.tag {
  font-size: 0.72rem;
  font-weight: 600;
  padding: 0.2rem 0.55rem;
  border-radius: 999px;
  background: var(--island-sage);
  color: var(--island-forest-deep);
}

.tag.soft {
  background: #fff;
  border: 1px solid var(--island-line);
  color: var(--island-muted);
  font-weight: 500;
}

.summary {
  margin: 0.35rem 0;
  color: var(--island-muted);
  line-height: 1.55;
  font-size: 0.95rem;
}

.meta {
  margin: 0.5rem 0 0;
  font-size: 0.78rem;
  color: var(--island-muted);
}

.layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 280px;
  gap: 1.5rem;
  margin-top: 1.25rem;
}

.reader--focus .layout {
  grid-template-columns: 1fr;
}

@media (max-width: 900px) {
  .layout { grid-template-columns: 1fr; }
}

.hint {
  margin: 0 0 0.75rem;
  font-size: 0.8rem;
  color: var(--island-muted);
}

.panel {
  margin-top: 1.75rem;
  padding-top: 1rem;
  border-top: 1px solid var(--island-line);
}

.panel h2 {
  margin: 0 0 0.65rem;
  font-size: 0.95rem;
}

.vocab {
  list-style: none;
  margin: 0;
  padding: 0;
  display: grid;
  gap: 0.4rem;
}

.vocab li {
  display: flex;
  flex-wrap: wrap;
  gap: 0.35rem 0.55rem;
  align-items: baseline;
  font-size: 0.88rem;
}

.vocab .pos {
  color: var(--island-muted);
  font-size: 0.75rem;
}

.vocab em {
  font-style: normal;
  color: var(--island-muted);
  font-size: 0.78rem;
}

.checkin-bar {
  margin-top: 1.75rem;
  padding: 1rem 1.1rem;
  border-radius: 16px;
  background: var(--island-sage-soft);
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.75rem 1rem;
}

.checkin-bar p { margin: 0; font-size: 0.88rem; }

.checkin-btn {
  border: none;
  border-radius: 999px;
  background: var(--island-forest);
  color: #fff;
  font-weight: 600;
  padding: 0.55rem 1.1rem;
  cursor: pointer;
}

.checkin-btn:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.checkin-msg {
  width: 100%;
  font-size: 0.8rem !important;
  color: var(--island-forest-deep);
}

.bridge {
  margin: 1rem 0 0;
  font-size: 0.88rem;
}

.bridge a {
  color: var(--island-primary);
  text-decoration: none;
}

.side-card {
  padding: 1rem;
  border-radius: 16px;
  background: var(--island-card);
  border: 1px solid rgba(59, 83, 62, 0.07);
  margin-bottom: 0.85rem;
}

.side-card h3 {
  margin: 0 0 0.75rem;
  font-size: 0.9rem;
}

.struct {
  margin-bottom: 0.85rem;
  padding-bottom: 0.75rem;
  border-bottom: 1px dashed var(--island-line);
}

.struct:last-child {
  border-bottom: none;
  margin-bottom: 0;
  padding-bottom: 0;
}

.struct-en {
  margin: 0;
  font-size: 0.82rem;
  line-height: 1.5;
  font-family: Georgia, serif;
}

.struct-zh {
  margin: 0.3rem 0 0;
  font-size: 0.78rem;
  color: var(--island-muted);
}

.struct-hint {
  margin: 0.25rem 0 0;
  font-size: 0.72rem;
  color: var(--island-forest);
}

.related {
  list-style: none;
  margin: 0;
  padding: 0;
}

.related li {
  display: flex;
  flex-direction: column;
  gap: 0.15rem;
  margin-bottom: 0.65rem;
  font-size: 0.85rem;
}

.related a {
  color: var(--island-text);
  text-decoration: none;
  font-weight: 600;
  line-height: 1.35;
}

.related span {
  font-size: 0.72rem;
  color: var(--island-muted);
}

.ann-list {
  list-style: none;
  margin: 0;
  padding: 0;
}

.ann-list li {
  display: grid;
  grid-template-columns: 10px 1fr auto;
  gap: 0.4rem;
  align-items: start;
  margin-bottom: 0.55rem;
  font-size: 0.78rem;
}

.dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  margin-top: 0.3rem;
}
.dot--moss { background: rgb(140, 168, 120); }
.dot--amber { background: rgb(220, 180, 110); }
.dot--sky { background: rgb(140, 180, 200); }

.ann-text {
  line-height: 1.4;
  word-break: break-word;
}

.ann-del {
  border: none;
  background: transparent;
  color: var(--island-muted);
  font-size: 0.7rem;
  cursor: pointer;
  padding: 0;
}

.muted { color: var(--island-muted); font-size: 0.82rem; }
</style>
