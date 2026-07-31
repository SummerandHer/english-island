<template>
  <div class="reader-text">
    <div ref="rootEl" class="body" @mouseup="onMouseUp">
      <template v-for="(para, pi) in paragraphBlocks" :key="pi">
        <span v-if="para.prefix" class="para-prefix">{{ para.prefix }}</span>
        <div
          class="para"
          :data-para="pi"
        >
          <div class="para-en">
            <template v-for="(seg, si) in para.segs" :key="`${pi}-${si}`">
              <span v-if="seg.type === 'gap'" class="gap">{{ seg.text }}</span>
              <template v-else>
                <span
                  class="sentence"
                  :class="{ active: activeSentenceIndex === seg.index }"
                  @click.stop="onSentenceClick(seg.index!, $event)"
                >
                  <template v-for="(tok, ti) in seg.tokens" :key="ti">
                    <mark
                      v-if="tok.markColor"
                      :class="['hl', `hl--${tok.markColor}`]"
                      :data-ann-start="tok.annStart"
                    >{{ tok.text }}</mark>
                    <span
                      v-else-if="tok.word"
                      class="word"
                      @click.stop="onWordClick(tok.text, seg.index!)"
                    >{{ tok.text }}</span>
                    <span v-else>{{ tok.text }}</span>
                  </template>
                </span>
                <!-- 点句译文：紧挨该句下方 -->
                <div
                  v-if="!showFullZh && inlineZh?.sentenceIndex === seg.index"
                  class="inline-zh"
                  :class="{ flash: inlineZh.flash }"
                >
                  <div class="inline-zh-head">
                    <span>本句译文</span>
                    <button type="button" class="zh-close" @click.stop="clearInlineZh">关闭</button>
                  </div>
                  <p class="inline-zh-text">{{ inlineZh.zh || '暂无译文，请管理员重新 AI 增强。' }}</p>
                  <p v-if="inlineZh.hint" class="inline-zh-hint">{{ inlineZh.hint }}</p>
                </div>
              </template>
            </template>
          </div>

          <!-- 全文翻译：每段英文下方显示该段中文 -->
          <div v-if="showFullZh" class="para-zh">
            <p v-if="para.zh">{{ para.zh }}</p>
            <p v-else class="para-zh-empty">本段暂无译文</p>
          </div>
        </div>
      </template>
    </div>

    <div
      v-if="pending"
      class="toolbar"
      :style="{ top: `${pending.y}px`, left: `${pending.x}px` }"
      @mousedown.prevent
    >
      <p class="toolbar-preview">{{ pending.text.slice(0, 42) }}{{ pending.text.length > 42 ? '…' : '' }}</p>
      <div class="toolbar-actions">
        <button type="button" class="act act-primary" @click="doTranslateSelection">翻译</button>
        <div class="swatches">
          <button
            v-for="c in colors"
            :key="c"
            type="button"
            class="swatch"
            :class="`swatch--${c}`"
            :title="'标注 ' + c"
            @click="confirmAnnotate(c)"
          />
        </div>
        <button type="button" class="act ghost" @click="pending = null">取消</button>
      </div>
    </div>

    <WordLookupPopover
      v-model:show="lookupShow"
      :word="lookupWord"
      :passage-id="articleId"
      :passage-title="articleTitle"
      source-type="daily"
      :fallback-zh="lookupFallbackZh"
      :fallback-hint="lookupFallbackHint"
      @added="emit('word-added')"
      @use-sentence="showSentenceForWord"
    />
  </div>
</template>

<script setup lang="ts">
import type { DailyAnnotation, DailySentenceItem, DailyVocabItem } from '~/types/api'

const props = defineProps<{
  content: string
  annotations: DailyAnnotation[]
  sentences: DailySentenceItem[]
  cetVocab: DailyVocabItem[]
  hardVocab: DailyVocabItem[]
  articleId?: number
  articleTitle?: string
  /** 全文翻译：每段下显示中文 */
  showFullZh?: boolean
}>()

const emit = defineEmits<{
  annotate: [{ startOffset: number; endOffset: number; selectedText: string; color: string }]
  'word-added': []
}>()

const colors = ['moss', 'amber', 'sky'] as const
const rootEl = ref<HTMLElement | null>(null)
const activeSentenceIndex = ref<number | null>(null)

const pending = ref<{
  startOffset: number
  endOffset: number
  text: string
  x: number
  y: number
} | null>(null)

const inlineZh = ref<{
  sentenceIndex: number
  zh: string
  hint?: string
  flash?: boolean
} | null>(null)

const lookupShow = ref(false)
const lookupWord = ref('')
const lookupFallbackZh = ref('')
const lookupFallbackHint = ref('')
const lookupSentenceIndex = ref<number | null>(null)

interface Tok {
  text: string
  word: boolean
  markColor?: string
  annStart?: number
}

interface RenderSeg {
  type: 'gap' | 'sentence'
  text?: string
  index?: number
  en?: string
  zh?: string
  start?: number
  end?: number
  tokens?: Tok[]
}

interface ParaBlock {
  start: number
  end: number
  /** 上一段到本段之间的换行，需进 DOM 以保持 offset 对齐 */
  prefix: string
  segs: RenderSeg[]
  zh: string
}

const articleVocabMap = computed(() => {
  const map = new Map<string, DailyVocabItem>()
  for (const v of [...(props.cetVocab || []), ...(props.hardVocab || [])]) {
    if (!v?.word) continue
    map.set(v.word.toLowerCase(), v)
  }
  return map
})

const normalizedSentences = computed(() => {
  const content = props.content || ''
  if (props.sentences?.length) {
    return [...props.sentences]
      .map((s) => ({
        en: s.en || content.slice(Number(s.startOffset), Number(s.endOffset)),
        zh: s.zh || '',
        startOffset: Number(s.startOffset ?? 0),
        endOffset: Number(s.endOffset ?? 0)
      }))
      .sort((a, b) => a.startOffset - b.startOffset)
  }
  return [{ en: content, zh: '', startOffset: 0, endOffset: content.length }]
})

/** 按换行切段（空行会被跳过），保留绝对 offset */
function paragraphSpans(content: string): { start: number; end: number }[] {
  const out: { start: number; end: number }[] = []
  const n = content.length
  let i = 0
  while (i < n) {
    while (i < n && content[i] === '\n') i++
    if (i >= n) break
    const start = i
    while (i < n && content[i] !== '\n') i++
    if (i > start) out.push({ start, end: i })
  }
  if (!out.length && content) {
    out.push({ start: 0, end: content.length })
  }
  return out
}

function tokenizeWithMarks(text: string, baseOffset: number): Tok[] {
  const parts = text.split(/(\b[a-zA-Z'-]+\b)/g).filter(Boolean)
  const out: Tok[] = []
  let cursor = 0
  for (const part of parts) {
    const absStart = baseOffset + cursor
    const absEnd = absStart + part.length
    const isWord = /^[a-zA-Z'-]+$/.test(part) && part.length > 1
    const mark = findMark(absStart, absEnd)
    out.push({
      text: part,
      word: isWord,
      markColor: mark?.color,
      annStart: mark?.startOffset
    })
    cursor += part.length
  }
  return out
}

function findMark(start: number, end: number) {
  return (props.annotations || []).find((a) => a.startOffset <= start && a.endOffset >= end)
}

const paragraphBlocks = computed((): ParaBlock[] => {
  const content = props.content || ''
  const sentences = normalizedSentences.value
  const spans = paragraphSpans(content)

  return spans.map((span, idx) => {
    const prefix = idx === 0 ? '' : content.slice(spans[idx - 1].end, span.start)
    const segs: RenderSeg[] = []
    let cursor = span.start
    const inPara = sentences
      .map((s, index) => ({ ...s, index }))
      .filter((s) => s.endOffset > span.start && s.startOffset < span.end)

    for (const s of inPara) {
      const start = Math.max(s.startOffset, span.start)
      const end = Math.min(s.endOffset, span.end)
      if (start > cursor) {
        segs.push({ type: 'gap', text: content.slice(cursor, start) })
      }
      if (end > start) {
        const slice = content.slice(start, end)
        segs.push({
          type: 'sentence',
          index: s.index,
          en: slice,
          zh: s.zh,
          start,
          end,
          tokens: tokenizeWithMarks(slice, start)
        })
      }
      cursor = Math.max(cursor, end)
    }
    if (cursor < span.end) {
      segs.push({ type: 'gap', text: content.slice(cursor, span.end) })
    }

    const zh = inPara
      .map((s) => (s.zh || '').trim())
      .filter(Boolean)
      .join(' ')

    return { start: span.start, end: span.end, prefix, segs, zh }
  })
})

function clearInlineZh() {
  inlineZh.value = null
  activeSentenceIndex.value = null
}

function openInlineZh(sentenceIndex: number, opts?: { hint?: string; flash?: boolean }) {
  const sent = normalizedSentences.value[sentenceIndex]
  if (!sent) return
  activeSentenceIndex.value = sentenceIndex
  inlineZh.value = {
    sentenceIndex,
    zh: sent.zh || '',
    hint: opts?.hint || (sent.zh ? undefined : '本篇尚未生成逐句译文'),
    flash: opts?.flash
  }
  nextTick(() => {
    const el = rootEl.value?.querySelector('.inline-zh') as HTMLElement | null
    el?.scrollIntoView({ behavior: 'smooth', block: 'nearest' })
  })
}

function scrollToAnnotation(startOffset: number) {
  const el = rootEl.value?.querySelector(`[data-ann-start="${startOffset}"]`) as HTMLElement | null
  if (!el) return
  el.scrollIntoView({ behavior: 'smooth', block: 'center' })
  el.classList.add('hl-flash')
  window.setTimeout(() => el.classList.remove('hl-flash'), 1200)
}

defineExpose({ scrollToAnnotation })

function onWordClick(word: string, sentenceIndex: number) {
  pending.value = null
  lookupWord.value = word
  lookupSentenceIndex.value = sentenceIndex
  const hit = articleVocabMap.value.get(word.toLowerCase())
    || articleVocabMap.value.get(word.toLowerCase().replace(/s$/, ''))
  lookupFallbackZh.value = hit?.zh || ''
  lookupFallbackHint.value = hit?.note
    ? hit.note
    : hit
      ? '来自本篇词表'
      : '词库未收录时，可点「看本句译文」'
  lookupShow.value = true
}

function showSentenceForWord() {
  const idx = lookupSentenceIndex.value
  if (idx == null) return
  lookupShow.value = false
  if (props.showFullZh) {
    activeSentenceIndex.value = idx
    return
  }
  openInlineZh(idx, { flash: true })
}

function onSentenceClick(si: number, e: MouseEvent) {
  const sel = window.getSelection()
  if (sel && !sel.isCollapsed) return
  const target = e.target as HTMLElement
  if (target.classList.contains('word')) return
  pending.value = null
  if (props.showFullZh) {
    // 全文模式下点句：只高亮该句，段下已有译文
    activeSentenceIndex.value = activeSentenceIndex.value === si ? null : si
    return
  }
  if (inlineZh.value?.sentenceIndex === si) {
    clearInlineZh()
    return
  }
  openInlineZh(si)
}

function offsetInRoot(node: Node, offset: number): number {
  const root = rootEl.value
  if (!root) return 0
  // 只统计英文正文节点，跳过译文块，避免 offset 漂移
  const walker = document.createTreeWalker(root, NodeFilter.SHOW_TEXT, {
    acceptNode(n) {
      const p = (n as Text).parentElement
      if (!p) return NodeFilter.FILTER_REJECT
      if (p.closest('.inline-zh, .para-zh, .toolbar')) return NodeFilter.FILTER_REJECT
      return NodeFilter.FILTER_ACCEPT
    }
  })
  let count = 0
  let current: Node | null = walker.nextNode()
  while (current) {
    if (current === node) {
      return count + offset
    }
    count += current.textContent?.length || 0
    current = walker.nextNode()
  }
  // fallback: Range 方式（可能含译文，尽量不用）
  const range = document.createRange()
  range.selectNodeContents(root)
  try {
    range.setEnd(node, offset)
  } catch {
    return 0
  }
  return range.toString().length
}

function onMouseUp(e: MouseEvent) {
  const sel = window.getSelection()
  if (!sel || sel.isCollapsed || !rootEl.value) return
  if (!rootEl.value.contains(sel.anchorNode) || !rootEl.value.contains(sel.focusNode)) return
  if ((e.target as HTMLElement).closest?.('.inline-zh, .para-zh')) return
  const a = offsetInRoot(sel.anchorNode!, sel.anchorOffset)
  const b = offsetInRoot(sel.focusNode!, sel.focusOffset)
  const start = Math.min(a, b)
  const end = Math.max(a, b)
  if (end <= start) return
  const selectedText = props.content.slice(start, end)
  if (!selectedText.trim()) return
  const rect = rootEl.value.getBoundingClientRect()
  pending.value = {
    startOffset: start,
    endOffset: end,
    text: selectedText,
    x: Math.min(Math.max(e.clientX - rect.left, 8), rect.width - 180),
    y: Math.max(e.clientY - rect.top - 8, 8)
  }
}

function confirmAnnotate(color: string) {
  if (!pending.value) return
  emit('annotate', {
    startOffset: pending.value.startOffset,
    endOffset: pending.value.endOffset,
    selectedText: pending.value.text,
    color
  })
  pending.value = null
  window.getSelection()?.removeAllRanges()
}

function doTranslateSelection() {
  if (!pending.value) return
  const text = pending.value.text.trim()
  const start = pending.value.startOffset
  const end = pending.value.endOffset
  pending.value = null
  window.getSelection()?.removeAllRanges()

  const exactIdx = normalizedSentences.value.findIndex(
    (s) => (s.startOffset === start && s.endOffset === end) || s.en.trim() === text
  )
  if (exactIdx >= 0) {
    openInlineZh(exactIdx)
    return
  }
  const coverIdx = normalizedSentences.value.findIndex(
    (s) => s.startOffset <= start && s.endOffset >= end
  )
  if (coverIdx >= 0) {
    const hit = articleVocabMap.value.get(text.toLowerCase())
    if (hit?.zh && text.split(/\s+/).length <= 3) {
      activeSentenceIndex.value = coverIdx
      inlineZh.value = {
        sentenceIndex: coverIdx,
        zh: hit.zh,
        hint: hit.note || '来自本篇词表（划选）'
      }
      return
    }
    openInlineZh(coverIdx, { hint: '已显示所在句子的译文' })
    return
  }
  openInlineZh(0, {
    hint: '未找到对应译文。可点所在句子查看，或让管理员重新 AI 增强。'
  })
}

watch(
  () => props.showFullZh,
  (on) => {
    if (on) clearInlineZh()
  }
)
</script>

<style scoped>
.reader-text {
  position: relative;
}

.body {
  font-family: Georgia, 'Noto Serif SC', 'Songti SC', serif;
  font-size: 1.08rem;
  line-height: 1.95;
  color: var(--island-text);
}

.para {
  margin: 0 0 0.75rem;
}

.para-prefix {
  white-space: pre;
  font-size: 0;
  line-height: 0;
}

.para:last-child {
  margin-bottom: 0;
}

.para-en {
  white-space: pre-wrap;
}

.sentence {
  border-radius: 6px;
  padding: 0.05em 0.12em;
  cursor: pointer;
  transition: background 0.15s ease;
  box-decoration-break: clone;
}

.sentence:hover {
  background: rgba(45, 71, 57, 0.04);
}

.sentence.active {
  background: rgba(45, 110, 180, 0.1);
}

.word {
  cursor: pointer;
  border-radius: 3px;
  padding: 0 0.05em;
  transition: background 0.12s ease;
}

.word:hover {
  background: rgba(45, 110, 180, 0.12);
}

.hl {
  border-radius: 2px;
  padding: 0.05em 0.08em;
  box-decoration-break: clone;
  color: #1a2a1e;
  box-shadow: inset 0 -2px 0 rgba(0, 0, 0, 0.12);
}
.hl--moss { background: #c8e06a; }
.hl--amber { background: #f5c84a; }
.hl--sky { background: #7ec4ea; }

.hl-flash {
  outline: 2px solid #2f6fed;
  outline-offset: 2px;
}

.inline-zh {
  display: block;
  margin: 0.45rem 0 0.75rem;
  padding: 0.7rem 0.85rem;
  border-radius: 12px;
  background: #f3f7fb;
  border: 1px solid rgba(45, 110, 180, 0.16);
  white-space: normal;
}

.inline-zh.flash {
  animation: zh-flash 0.9s ease;
}

@keyframes zh-flash {
  0%, 100% { box-shadow: none; }
  40% { box-shadow: 0 0 0 3px rgba(47, 111, 237, 0.25); }
}

.inline-zh-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.35rem;
  font-size: 0.72rem;
  font-weight: 700;
  color: #2f6a9a;
}

.inline-zh-text {
  margin: 0;
  font-family: var(--font-body);
  font-size: 0.95rem;
  line-height: 1.7;
  color: var(--island-text);
}

.inline-zh-hint {
  margin: 0.35rem 0 0;
  font-size: 0.72rem;
  color: var(--island-muted);
}

.para-zh {
  margin-top: 0.55rem;
  padding: 0.65rem 0.8rem;
  border-radius: 12px;
  background: #f5f6f4;
  border-left: 3px solid #8fa88a;
  white-space: normal;
}

.para-zh p {
  margin: 0;
  font-family: var(--font-body);
  font-size: 0.92rem;
  line-height: 1.75;
  color: #3a4a3e;
}

.para-zh-empty {
  color: var(--island-muted) !important;
  font-size: 0.82rem !important;
}

.zh-close {
  border: none;
  background: transparent;
  color: var(--island-muted);
  font-size: 0.72rem;
  cursor: pointer;
}

.toolbar {
  position: absolute;
  z-index: 30;
  min-width: 11rem;
  padding: 0.55rem 0.65rem;
  border-radius: 12px;
  background: #fff;
  box-shadow: 0 8px 28px rgba(45, 71, 57, 0.14);
  border: 1px solid var(--island-line);
  transform: translateY(-100%);
}

.toolbar-preview {
  margin: 0 0 0.4rem;
  font-size: 0.72rem;
  color: var(--island-muted);
  max-width: 14rem;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.toolbar-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.4rem;
}

.act {
  border: 1px solid var(--island-line);
  background: #fff;
  border-radius: 999px;
  padding: 0.25rem 0.65rem;
  font-size: 0.75rem;
  cursor: pointer;
  color: var(--island-forest);
}

.act-primary {
  background: var(--island-forest);
  border-color: transparent;
  color: #fff;
  font-weight: 600;
}

.act.ghost {
  border: none;
  color: var(--island-muted);
}

.swatches {
  display: flex;
  gap: 0.3rem;
}

.swatch {
  width: 1.15rem;
  height: 1.15rem;
  border: 2px solid rgba(45, 71, 57, 0.12);
  border-radius: 50%;
  cursor: pointer;
}
.swatch--moss { background: #c8e06a; }
.swatch--amber { background: #f5c84a; }
.swatch--sky { background: #7ec4ea; }
</style>
