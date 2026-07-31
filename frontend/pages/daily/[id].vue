<template>
  <div>
    <!-- 普通阅读布局（非沉浸） -->
    <div v-show="!focusMode" class="reader">
      <div v-if="loading" class="state">加载中…</div>
      <div v-else-if="error" class="state err">{{ error }}</div>
      <template v-else-if="article">
        <div class="toolbar-top">
          <NuxtLink to="/daily" class="back">← 日报岛</NuxtLink>
          <button type="button" class="focus-btn" @click="enterFocus">
            沉浸阅读
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

        <div
          v-if="article.coverUrl"
          class="cover-wrap"
          :class="`cover-wrap--${coverShape}`"
          :style="coverWrapStyle"
        >
          <img
            :src="article.coverUrl"
            :alt="article.title"
            class="cover"
            decoding="async"
            fetchpriority="high"
            @load="onCoverLoad"
          />
        </div>

        <div class="layout">
          <div class="main">
            <div class="paper">
              <p class="hint">点单词查释义 · 点句子在句下看译文 · 侧栏可开全文翻译</p>
              <DailyReadingText
                ref="readerRef"
                :content="article.contentEn"
                :annotations="article.annotations"
                :sentences="article.sentences || []"
                :cet-vocab="article.cetVocab || []"
                :hard-vocab="article.hardVocab || []"
                :article-id="article.id"
                :article-title="article.title"
                :show-full-zh="showFullZh"
                @annotate="onAnnotate"
              />
            </div>

            <section v-if="article.cetVocab?.length" class="panel">
              <h2>高频词汇</h2>
              <ul class="vocab">
                <li v-for="(v, i) in article.cetVocab" :key="`c-${i}`">
                  <strong>{{ v.word }}</strong>
                  <span v-if="v.pos" class="pos">{{ v.pos }}</span>
                  <span>{{ v.zh }}</span>
                </li>
              </ul>
            </section>

            <section v-if="article.hardVocab?.length" class="panel">
              <h2>难词拆分</h2>
              <ul class="vocab">
                <li v-for="(v, i) in article.hardVocab" :key="`h-${i}`">
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

          <aside class="side">
            <div class="side-sticky">
              <section class="side-card side-card--tools">
                <h3>阅读辅助</h3>
                <button
                  type="button"
                  class="tool-btn"
                  :class="{ on: showFullZh }"
                  :disabled="!hasSentenceZh"
                  @click="toggleFullZh"
                >
                  {{ showFullZh ? '隐藏全文翻译' : '全文翻译' }}
                </button>
                <p class="tool-hint">
                  {{
                    hasSentenceZh
                      ? (showFullZh ? '已在每段英文下显示中文' : '开启后，每段下方显示中文译文')
                      : '本篇暂无逐句译文，请管理员重新 AI 增强'
                  }}
                </p>
              </section>

              <section class="side-card">
                <h3>可借用句式</h3>
                <div v-if="!article.structures?.length" class="muted">暂无</div>
                <article v-for="(s, i) in article.structures" :key="i" class="struct">
                  <p class="struct-en">{{ s.en }}</p>
                  <p v-if="s.zh" class="struct-zh">{{ s.zh }}</p>
                  <p v-if="s.hint" class="struct-hint">{{ s.hint }}</p>
                </article>
              </section>
            </div>

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
          </aside>
        </div>

        <!-- 左侧悬浮：我的标注（不占右侧学习区） -->
        <div class="ann-float">
          <button
            type="button"
            class="ann-float-btn"
            :class="{ open: annPanelOpen }"
            @click="annPanelOpen = !annPanelOpen"
          >
            <span class="ann-float-label">我的标注</span>
            <span class="ann-count">{{ article.annotations?.length || 0 }}</span>
          </button>
          <div v-if="annPanelOpen" class="ann-float-panel">
            <div class="ann-head">
              <h3>我的标注</h3>
              <button type="button" class="ann-dock-close" @click="annPanelOpen = false">关闭</button>
            </div>
            <p v-if="!article.annotations?.length" class="muted ann-empty">
              划选正文即可标注，点条目可跳回原文
            </p>
            <ul v-else class="ann-list">
              <li v-for="a in article.annotations" :key="a.id">
                <span :class="['dot', `dot--${a.color}`]" />
                <button
                  type="button"
                  class="ann-text"
                  :title="'定位到正文'"
                  @click="jumpToAnn(a.startOffset); annPanelOpen = false"
                >
                  {{ a.selectedText }}
                </button>
                <button type="button" class="ann-del" @click="removeAnn(a.id)">删除</button>
              </li>
            </ul>
          </div>
        </div>

        <!-- 窄屏：右侧补全文翻译入口（桌面已在侧栏） -->
        <div class="zh-float">
          <button
            type="button"
            class="zh-float-btn"
            :class="{ on: showFullZh }"
            :disabled="!hasSentenceZh"
            @click="toggleFullZh"
          >
            {{ showFullZh ? '隐译' : '全文译' }}
          </button>
        </div>
      </template>
    </div>

    <!-- 沉浸模式：盖住侧边栏/顶栏，整屏只剩文章 -->
    <Teleport to="body">
      <div
        v-if="focusMode && article"
        class="immerse"
        role="dialog"
        aria-modal="true"
        aria-label="沉浸阅读"
      >
        <header class="immerse-top">
          <div class="immerse-brand">
            <span class="immerse-kicker">沉浸阅读</span>
            <span class="immerse-topic">{{ article.topicLabel }} · {{ article.difficulty.toUpperCase() }}</span>
          </div>
          <button type="button" class="immerse-exit" @click="exitFocus">
            退出沉浸
            <kbd>Esc</kbd>
          </button>
        </header>

        <div class="immerse-body">
          <div class="immerse-grid">
            <div class="immerse-main">
              <h1 class="immerse-title">{{ article.title }}</h1>
              <p class="immerse-hint">点词查义 · 点句在句下看译 · 侧栏可开全文翻译</p>
              <DailyReadingText
                ref="immerseReaderRef"
                :content="article.contentEn"
                :annotations="article.annotations"
                :sentences="article.sentences || []"
                :cet-vocab="article.cetVocab || []"
                :hard-vocab="article.hardVocab || []"
                :article-id="article.id"
                :article-title="article.title"
                :show-full-zh="showFullZh"
                @annotate="onAnnotate"
              />

              <section v-if="article.cetVocab?.length" class="immerse-vocab">
                <h2>高频词汇</h2>
                <ul>
                  <li v-for="(v, i) in article.cetVocab" :key="`iv-c-${i}`">
                    <strong>{{ v.word }}</strong>
                    <span v-if="v.pos" class="pos">{{ v.pos }}</span>
                    <span class="zh">{{ v.zh }}</span>
                  </li>
                </ul>
              </section>

              <section v-if="article.hardVocab?.length" class="immerse-vocab immerse-vocab--hard">
                <h2>难词拆分</h2>
                <ul>
                  <li v-for="(v, i) in article.hardVocab" :key="`iv-h-${i}`">
                    <strong>{{ v.word }}</strong>
                    <span v-if="v.pos" class="pos">{{ v.pos }}</span>
                    <span class="zh">{{ v.zh }}</span>
                    <em v-if="v.note">{{ v.note }}</em>
                  </li>
                </ul>
              </section>
            </div>

            <aside class="immerse-side">
              <div class="immerse-side-sticky">
                <div class="immerse-side-card">
                  <h2>阅读辅助</h2>
                  <button
                    type="button"
                    class="tool-btn"
                    :class="{ on: showFullZh }"
                    :disabled="!hasSentenceZh"
                    @click="toggleFullZh"
                  >
                    {{ showFullZh ? '隐藏全文翻译' : '全文翻译' }}
                  </button>
                  <p class="tool-hint">
                    {{ showFullZh ? '每段下已显示中文' : '点句看译 · 或开启全文翻译' }}
                  </p>
                </div>

                <div class="immerse-side-card">
                  <h2>可借用句式</h2>
                  <p v-if="!article.structures?.length" class="immerse-empty">暂无句式卡片</p>
                  <article
                    v-for="(s, i) in article.structures"
                    :key="`iv-s-${i}`"
                    class="immerse-struct"
                  >
                    <p class="en">{{ s.en }}</p>
                    <p v-if="s.zh" class="zh">{{ s.zh }}</p>
                    <p v-if="s.hint" class="hint-line">{{ s.hint }}</p>
                  </article>
                </div>
              </div>
            </aside>
          </div>
        </div>

        <!-- 沉浸模式同样用左下标注浮层 -->
        <div class="ann-float ann-float--immerse">
          <button
            type="button"
            class="ann-float-btn"
            :class="{ open: annPanelOpen }"
            @click="annPanelOpen = !annPanelOpen"
          >
            <span class="ann-float-label">我的标注</span>
            <span class="ann-count">{{ article.annotations?.length || 0 }}</span>
          </button>
          <div v-if="annPanelOpen" class="ann-float-panel">
            <div class="ann-head">
              <h3>我的标注</h3>
              <button type="button" class="ann-dock-close" @click="annPanelOpen = false">关闭</button>
            </div>
            <p v-if="!article.annotations?.length" class="muted ann-empty">划选正文即可标注</p>
            <ul v-else class="ann-list">
              <li v-for="a in article.annotations" :key="`iv-a-${a.id}`">
                <span :class="['dot', `dot--${a.color}`]" />
                <button type="button" class="ann-text" @click="jumpToAnn(a.startOffset); annPanelOpen = false">
                  {{ a.selectedText }}
                </button>
                <button type="button" class="ann-del" @click="removeAnn(a.id)">删除</button>
              </li>
            </ul>
          </div>
        </div>

        <footer class="immerse-foot">
          <span class="immerse-timer">
            {{ article.checkedIn ? '已打卡' : `${Math.floor(readSeconds / 60)}:${String(readSeconds % 60).padStart(2, '0')}` }}
          </span>
          <button
            type="button"
            class="immerse-checkin"
            :disabled="article.checkedIn || checking || !canCheckin"
            @click="doCheckin"
          >
            {{ article.checkedIn ? '已完成' : canCheckin ? '完成今日阅读' : `再读 ${90 - readSeconds}s` }}
          </button>
        </footer>
      </div>
    </Teleport>
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
const annPanelOpen = ref(false)
const showFullZh = ref(false)
const readerRef = ref<{ scrollToAnnotation: (n: number) => void } | null>(null)
const immerseReaderRef = ref<{ scrollToAnnotation: (n: number) => void } | null>(null)
/** 封面宽高比：按原图自适应，并夹在舒适范围内 */
const coverRatio = ref(16 / 9)
const coverShape = ref<'wide' | 'standard' | 'tall'>('standard')
let timer: ReturnType<typeof setInterval> | null = null

const focusMode = computed(() => String(route.query.focus || '') === '1')
const canCheckin = computed(() => readSeconds.value >= 90)
const hasSentenceZh = computed(() =>
  (article.value?.sentences || []).some((s) => !!(s.zh && s.zh.trim()))
)

function toggleFullZh() {
  if (!hasSentenceZh.value) return
  showFullZh.value = !showFullZh.value
}

function jumpToAnn(startOffset: number) {
  const target = focusMode.value ? immerseReaderRef.value : readerRef.value
  target?.scrollToAnnotation(startOffset)
}

const coverWrapStyle = computed(() => ({
  aspectRatio: String(coverRatio.value)
}))

function resetCoverLayout() {
  coverRatio.value = 16 / 9
  coverShape.value = 'standard'
}

function onCoverLoad(e: Event) {
  const img = e.target as HTMLImageElement
  const w = img.naturalWidth
  const h = img.naturalHeight
  if (!w || !h) return
  const raw = w / h
  // 过宽 → 横幅；方一点 → 标准；偏竖 → 稍高，避免裁成一条细带
  if (raw >= 1.9) {
    coverShape.value = 'wide'
    coverRatio.value = Math.min(raw, 2.2)
  } else if (raw <= 1.25) {
    coverShape.value = 'tall'
    coverRatio.value = Math.max(raw, 1.2)
  } else {
    coverShape.value = 'standard'
    coverRatio.value = Math.min(Math.max(raw, 1.4), 1.85)
  }
}

function setFocusQuery(on: boolean) {
  const q = { ...route.query }
  if (on) q.focus = '1'
  else delete q.focus
  return router.replace({ query: q })
}

async function enterFocus() {
  await setFocusQuery(true)
  nextTick(() => {
    try {
      document.documentElement.requestFullscreen?.()
    } catch {
      /* 浏览器可能拒绝，覆盖层已足够 */
    }
  })
}

async function exitFocus() {
  if (document.fullscreenElement) {
    try {
      await document.exitFullscreen()
    } catch {
      /* ignore */
    }
  }
  await setFocusQuery(false)
}

function onKeydown(e: KeyboardEvent) {
  if (e.key === 'Escape' && focusMode.value) {
    e.preventDefault()
    exitFocus()
  }
}

function onFullscreenChange() {
  // 用户按系统 Esc 退出浏览器全屏时，同步退出沉浸
  if (!document.fullscreenElement && focusMode.value) {
    setFocusQuery(false)
  }
}

watch(focusMode, (on) => {
  if (!import.meta.client) return
  document.body.style.overflow = on ? 'hidden' : ''
})

async function load() {
  loading.value = true
  error.value = ''
  resetCoverLayout()
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

async function onAnnotate(payload: {
  startOffset: number
  endOffset: number
  selectedText: string
  color: string
}) {
  if (!auth.isLoggedIn) {
    checkinMsg.value = '登录后即可保存标注'
    await exitFocus()
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
    await exitFocus()
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
  window.addEventListener('keydown', onKeydown)
  document.addEventListener('fullscreenchange', onFullscreenChange)
  if (focusMode.value) {
    document.body.style.overflow = 'hidden'
  }
})

onBeforeUnmount(() => {
  if (timer) clearInterval(timer)
  window.removeEventListener('keydown', onKeydown)
  document.removeEventListener('fullscreenchange', onFullscreenChange)
  document.body.style.overflow = ''
  if (document.fullscreenElement) {
    document.exitFullscreen?.().catch(() => {})
  }
})

watch(() => route.params.id, () => {
  readSeconds.value = 0
  checkinMsg.value = ''
  showFullZh.value = false
  annPanelOpen.value = false
  load()
})
</script>

<style scoped>
.reader {
  max-width: 1120px;
  margin: 0 auto;
  padding-bottom: 4.5rem;
}

.state {
  padding: 2rem;
  color: var(--island-muted);
}
.err { color: #b45353; }

.cover-wrap {
  position: relative;
  width: 100%;
  margin: 0.35rem 0 1.5rem;
  border-radius: 20px;
  overflow: hidden;
  background: #e8ebe4;
  box-shadow: 0 10px 28px rgba(45, 71, 57, 0.06);
}

.cover-wrap--wide {
  max-height: min(38vh, 360px);
}

.cover-wrap--standard {
  max-height: min(44vh, 400px);
}

.cover-wrap--tall {
  max-height: min(48vh, 460px);
}

.cover {
  position: absolute;
  inset: 0;
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: center;
}

.toolbar-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.85rem;
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
  padding: 0.4rem 0.95rem;
  font-size: 0.85rem;
  font-weight: 600;
  color: var(--island-forest);
  cursor: pointer;
}

.head h1 {
  margin: 0.45rem 0 0.35rem;
  font-family: var(--font-display);
  font-size: clamp(1.55rem, 3vw, 2.1rem);
  line-height: 1.25;
  color: var(--island-forest-deep);
}

.summary {
  margin: 0.45rem 0 0.15rem;
  color: var(--island-muted);
  line-height: 1.6;
  font-size: 0.95rem;
}

.meta {
  margin: 0.55rem 0 0;
  font-size: 0.78rem;
  color: var(--island-muted);
}

.layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 300px;
  gap: 1.35rem;
  margin-top: 0.25rem;
  align-items: start;
}

.paper {
  background: #ffffff;
  border-radius: 18px;
  padding: 1.15rem 1.25rem 1.4rem;
  border: 1px solid rgba(45, 71, 57, 0.06);
  box-shadow: 0 8px 24px rgba(45, 71, 57, 0.04);
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
  background: #e8efe4;
  color: var(--island-forest-deep);
}

.tag.soft {
  background: #fff;
  border: 1px solid var(--island-line);
  color: var(--island-muted);
  font-weight: 500;
}

@media (max-width: 960px) {
  .layout { grid-template-columns: 1fr; }
  .side { display: none; }
}

.hint {
  margin: 0 0 0.75rem;
  font-size: 0.8rem;
  color: var(--island-muted);
}

.panel {
  margin-top: 1.75rem;
  padding: 1rem 1.1rem;
  border-radius: 16px;
  background: #fff;
  border: 1px solid rgba(45, 71, 57, 0.06);
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
  margin-top: 1.25rem;
  padding: 1rem 1.1rem;
  border-radius: 16px;
  background: #fff;
  border: 1px solid rgba(45, 71, 57, 0.08);
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
  background: #ffffff;
  border: 1px solid rgba(59, 83, 62, 0.08);
  margin-bottom: 0.85rem;
}

.ann-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.5rem;
  margin-bottom: 0.75rem;
}

.ann-head h3,
.ann-head h2 {
  margin: 0;
  font-size: 0.9rem;
}

.ann-count {
  min-width: 1.4rem;
  height: 1.4rem;
  padding: 0 0.4rem;
  border-radius: 999px;
  background: #1f2a22;
  color: #fff;
  font-size: 0.72rem;
  font-weight: 700;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.ann-empty {
  margin: 0;
  line-height: 1.5;
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
  box-shadow: 0 0 0 1px rgba(0, 0, 0, 0.12);
}
.dot--moss { background: #9fc23a; }
.dot--amber { background: #e0a820; }
.dot--sky { background: #3a9fd0; }

.ann-text {
  line-height: 1.4;
  word-break: break-word;
  text-align: left;
  border: none;
  background: transparent;
  padding: 0;
  color: var(--island-text);
  cursor: pointer;
  font: inherit;
}

.ann-text:hover {
  color: #1d5bb8;
  text-decoration: underline;
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

.side-sticky {
  position: sticky;
  top: 4.2rem;
  z-index: 8;
  margin-bottom: 0.85rem;
}

.side-card--tools h3 {
  margin: 0 0 0.65rem;
  font-size: 0.9rem;
}

.tool-btn {
  width: 100%;
  border: 1px solid rgba(45, 71, 57, 0.14);
  border-radius: 12px;
  background: #fff;
  color: var(--island-forest-deep);
  font-weight: 700;
  font-size: 0.88rem;
  padding: 0.65rem 0.85rem;
  cursor: pointer;
  transition: background 0.15s ease, color 0.15s ease, border-color 0.15s ease;
}

.tool-btn:hover:not(:disabled) {
  border-color: var(--island-forest);
}

.tool-btn.on {
  background: var(--island-forest);
  border-color: transparent;
  color: #fff;
}

.tool-btn:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.tool-hint {
  margin: 0.5rem 0 0;
  font-size: 0.72rem;
  line-height: 1.45;
  color: var(--island-muted);
}

/* 左下：标注浮层（桌面+移动统一） */
.ann-float {
  position: fixed;
  left: 1rem;
  bottom: 1.25rem;
  z-index: 50;
}

.ann-float--immerse {
  z-index: 220;
}

.ann-float-btn {
  display: inline-flex;
  align-items: center;
  gap: 0.45rem;
  border: 1px solid rgba(45, 71, 57, 0.14);
  border-radius: 999px;
  background: #fff;
  color: var(--island-forest-deep);
  font-weight: 700;
  font-size: 0.85rem;
  padding: 0.65rem 0.95rem;
  box-shadow: 0 10px 28px rgba(20, 30, 22, 0.16);
  cursor: pointer;
}

.ann-float-btn.open {
  background: #1f2a22;
  border-color: transparent;
  color: #fff;
}

.ann-float-btn.open .ann-count {
  background: #fff;
  color: #1f2a22;
}

.ann-float-label {
  line-height: 1;
}

.ann-float-panel {
  position: absolute;
  left: 0;
  bottom: calc(100% + 0.55rem);
  width: min(86vw, 320px);
  max-height: min(52vh, 420px);
  overflow: auto;
  padding: 0.9rem 1rem;
  border-radius: 16px;
  background: #fff;
  border: 1px solid rgba(45, 71, 57, 0.1);
  box-shadow: 0 16px 40px rgba(20, 30, 22, 0.18);
}

.ann-dock-close {
  border: none;
  background: transparent;
  color: var(--island-muted);
  font-size: 0.78rem;
  cursor: pointer;
}

/* 窄屏右侧：全文译（宽屏侧栏已有） */
.zh-float {
  display: none;
}

@media (max-width: 960px) {
  .zh-float {
    display: block;
    position: fixed;
    right: 1rem;
    bottom: 1.25rem;
    z-index: 50;
  }
}

.zh-float-btn {
  border: 1px solid rgba(45, 71, 57, 0.12);
  border-radius: 999px;
  background: #fff;
  color: var(--island-forest-deep);
  font-weight: 700;
  font-size: 0.82rem;
  padding: 0.7rem 0.95rem;
  box-shadow: 0 10px 28px rgba(20, 30, 22, 0.18);
  cursor: pointer;
}

.zh-float-btn.on {
  background: var(--island-forest);
  color: #fff;
  border-color: transparent;
}

.zh-float-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* —— 沉浸层（盖住全局侧边栏） —— */
.immerse {
  position: fixed;
  inset: 0;
  z-index: 200;
  display: flex;
  flex-direction: column;
  background: #f7f8f6;
  color: var(--island-text);
}

.immerse-top {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  padding: 0.85rem 1.25rem;
  border-bottom: 1px solid rgba(59, 83, 62, 0.08);
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(8px);
}

.immerse-brand {
  display: flex;
  flex-direction: column;
  gap: 0.15rem;
  min-width: 0;
}

.immerse-kicker {
  font-size: 0.72rem;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--island-forest);
}

.immerse-topic {
  font-size: 0.8rem;
  color: var(--island-muted);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.immerse-exit {
  display: inline-flex;
  align-items: center;
  gap: 0.45rem;
  border: 1px solid var(--island-line);
  background: #fff;
  border-radius: 999px;
  padding: 0.45rem 0.9rem;
  font-size: 0.85rem;
  font-weight: 600;
  color: var(--island-forest-deep);
  cursor: pointer;
  flex-shrink: 0;
}

.immerse-exit kbd {
  font-size: 0.68rem;
  font-family: inherit;
  padding: 0.1rem 0.35rem;
  border-radius: 4px;
  background: #eef1ec;
  color: var(--island-muted);
}

.immerse-body {
  flex: 1;
  overflow: auto;
  width: 100%;
  padding: 1.35rem clamp(1.25rem, 3vw, 2.5rem) 6.5rem;
}

.immerse-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(260px, 340px);
  gap: 1.75rem 2.25rem;
  max-width: 1360px;
  width: 100%;
  margin: 0 auto;
  align-items: start;
}

@media (min-width: 1280px) {
  .immerse-grid {
    grid-template-columns: minmax(0, 1fr) 340px;
    gap: 2rem 2.75rem;
  }
}

@media (max-width: 960px) {
  .immerse-grid {
    grid-template-columns: 1fr;
    gap: 1.35rem;
  }

  .immerse-side {
    order: 2;
    position: static;
  }
}

.immerse-main {
  min-width: 0;
  padding: 1.15rem 1.3rem 1.4rem;
  border-radius: 18px;
  background: #ffffff;
  border: 1px solid rgba(45, 71, 57, 0.06);
  box-shadow: 0 8px 24px rgba(45, 71, 57, 0.04);
}

.immerse-main :deep(.body) {
  font-size: clamp(1.08rem, 1.15vw, 1.22rem);
  line-height: 2.05;
  letter-spacing: 0.01em;
}

.immerse-title {
  margin: 0 0 0.55rem;
  font-family: var(--font-display);
  font-size: clamp(1.65rem, 2.8vw, 2.2rem);
  line-height: 1.28;
  color: var(--island-forest-deep);
  text-wrap: balance;
}

.immerse-hint {
  margin: 0 0 1.5rem;
  font-size: 0.8rem;
  color: var(--island-muted);
}

.immerse-vocab {
  margin-top: 2.25rem;
  padding-top: 1.35rem;
  border-top: 1px solid rgba(59, 83, 62, 0.12);
}

.immerse-vocab h2,
.immerse-side-card h2 {
  margin: 0 0 0.9rem;
  font-size: 0.95rem;
  font-weight: 700;
  color: var(--island-forest-deep);
  letter-spacing: 0.02em;
}

.immerse-vocab ul {
  list-style: none;
  margin: 0;
  padding: 0;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 0.6rem 0.85rem;
}

.immerse-vocab li {
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  gap: 0.3rem 0.45rem;
  padding: 0.65rem 0.8rem;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.78);
  border: 1px solid rgba(59, 83, 62, 0.07);
  font-size: 0.9rem;
  line-height: 1.4;
}

.immerse-vocab--hard li {
  background: rgba(255, 252, 245, 0.92);
}

.immerse-vocab .pos {
  font-size: 0.72rem;
  color: var(--island-muted);
}

.immerse-vocab .zh {
  color: var(--island-text);
}

.immerse-vocab em {
  width: 100%;
  font-style: normal;
  font-size: 0.75rem;
  color: var(--island-muted);
}

.immerse-side {
  position: sticky;
  top: 0.65rem;
  max-height: calc(100vh - 5.5rem);
  overflow: auto;
}

.immerse-side-sticky {
  display: grid;
  gap: 0;
}

.immerse-side-card {
  padding: 1.15rem 1.2rem 1.25rem;
  border-radius: 18px;
  background: #ffffff;
  border: 1px solid rgba(59, 83, 62, 0.08);
  box-shadow: 0 10px 28px rgba(45, 71, 57, 0.05);
  margin-bottom: 0.85rem;
}

.immerse-side-card .tool-btn {
  width: 100%;
  border: 1px solid rgba(45, 71, 57, 0.14);
  border-radius: 12px;
  background: #fff;
  color: var(--island-forest-deep);
  font-weight: 700;
  font-size: 0.88rem;
  padding: 0.65rem 0.85rem;
  cursor: pointer;
}

.immerse-side-card .tool-btn.on {
  background: var(--island-forest);
  border-color: transparent;
  color: #fff;
}

.immerse-side-card .tool-btn:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.immerse-side-card .tool-hint {
  margin: 0.5rem 0 0;
  font-size: 0.72rem;
  line-height: 1.45;
  color: var(--island-muted);
}

.immerse-empty {
  margin: 0;
  font-size: 0.82rem;
  color: var(--island-muted);
}

.immerse-struct {
  padding: 0.9rem 0;
  border-bottom: 1px dashed rgba(59, 83, 62, 0.14);
}

.immerse-struct:first-of-type {
  padding-top: 0.1rem;
}

.immerse-struct:last-child {
  border-bottom: none;
  padding-bottom: 0;
}

.immerse-struct .en {
  margin: 0;
  font-family: Georgia, 'Noto Serif SC', serif;
  font-size: 0.9rem;
  line-height: 1.6;
  color: var(--island-text);
}

.immerse-struct .zh {
  margin: 0.45rem 0 0;
  font-size: 0.82rem;
  line-height: 1.5;
  color: var(--island-muted);
}

.immerse-struct .hint-line {
  margin: 0.4rem 0 0;
  display: inline-block;
  font-size: 0.72rem;
  font-weight: 600;
  color: var(--island-forest);
  background: var(--island-sage-soft);
  border-radius: 6px;
  padding: 0.18rem 0.5rem;
}

.immerse-foot {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 1rem;
  padding: 0.9rem 1.25rem calc(0.9rem + env(safe-area-inset-bottom));
  background: linear-gradient(transparent, rgba(247, 248, 246, 0.97) 32%);
  pointer-events: none;
}

.immerse-foot > * {
  pointer-events: auto;
}

.immerse-timer {
  font-variant-numeric: tabular-nums;
  font-size: 0.9rem;
  font-weight: 600;
  color: var(--island-muted);
  min-width: 3.5rem;
}

.immerse-checkin {
  border: none;
  border-radius: 999px;
  background: var(--island-forest);
  color: #fff;
  font-weight: 600;
  padding: 0.65rem 1.35rem;
  font-size: 0.9rem;
  cursor: pointer;
  box-shadow: 0 8px 24px rgba(45, 71, 57, 0.18);
}

.immerse-checkin:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  box-shadow: none;
}

@media (prefers-reduced-motion: no-preference) {
  .immerse {
    animation: immerse-in 0.28s ease-out;
  }
}

@keyframes immerse-in {
  from {
    opacity: 0;
    transform: translateY(8px);
  }
  to {
    opacity: 1;
    transform: none;
  }
}
</style>
