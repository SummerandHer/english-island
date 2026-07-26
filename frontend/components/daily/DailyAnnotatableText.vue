<template>
  <div class="annotator">
    <div
      ref="rootEl"
      class="body"
      @mouseup="onMouseUp"
    >
      <template v-for="(seg, i) in segments" :key="i">
        <mark
          v-if="seg.color"
          :class="['hl', `hl--${seg.color}`]"
          :data-ann-id="seg.annotationId"
          :title="seg.note || '标注'"
        >{{ seg.text }}</mark>
        <span v-else>{{ seg.text }}</span>
      </template>
    </div>

    <div
      v-if="pending"
      class="toolbar"
      :style="{ top: `${pending.y}px`, left: `${pending.x}px` }"
    >
      <p class="toolbar-preview">{{ pending.text.slice(0, 48) }}{{ pending.text.length > 48 ? '…' : '' }}</p>
      <div class="swatches">
        <button
          v-for="c in colors"
          :key="c"
          type="button"
          class="swatch"
          :class="`swatch--${c}`"
          :aria-label="c"
          @click="confirm(c)"
        />
      </div>
      <button type="button" class="cancel" @click="pending = null">取消</button>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { DailyAnnotation } from '~/types/api'

const props = defineProps<{
  content: string
  annotations: DailyAnnotation[]
}>()

const emit = defineEmits<{
  annotate: [{ startOffset: number; endOffset: number; selectedText: string; color: string }]
}>()

const colors = ['moss', 'amber', 'sky'] as const
const rootEl = ref<HTMLElement | null>(null)

const pending = ref<{
  startOffset: number
  endOffset: number
  text: string
  x: number
  y: number
} | null>(null)

interface Segment {
  text: string
  color?: string
  annotationId?: number
  note?: string | null
}

const segments = computed(() => {
  const text = props.content || ''
  const anns = [...props.annotations]
    .filter((a) => a.startOffset >= 0 && a.endOffset > a.startOffset && a.endOffset <= text.length)
    .sort((a, b) => a.startOffset - b.startOffset)

  const out: Segment[] = []
  let cursor = 0
  for (const a of anns) {
    if (a.startOffset < cursor) continue
    if (a.startOffset > cursor) {
      out.push({ text: text.slice(cursor, a.startOffset) })
    }
    out.push({
      text: text.slice(a.startOffset, a.endOffset),
      color: a.color || 'moss',
      annotationId: a.id,
      note: a.note
    })
    cursor = a.endOffset
  }
  if (cursor < text.length) {
    out.push({ text: text.slice(cursor) })
  }
  if (!out.length) {
    out.push({ text })
  }
  return out
})

function offsetInRoot(node: Node, offset: number): number {
  const root = rootEl.value
  if (!root) return 0
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
  if (!sel || sel.isCollapsed || !rootEl.value) {
    return
  }
  if (!rootEl.value.contains(sel.anchorNode) || !rootEl.value.contains(sel.focusNode)) {
    return
  }
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
    x: Math.min(Math.max(e.clientX - rect.left, 8), rect.width - 160),
    y: Math.max(e.clientY - rect.top - 8, 8)
  }
}

function confirm(color: string) {
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
</script>

<style scoped>
.annotator {
  position: relative;
}

.body {
  font-family: Georgia, 'Noto Serif SC', 'Songti SC', serif;
  font-size: 1.08rem;
  line-height: 1.95;
  color: var(--island-text);
  white-space: pre-wrap;
  user-select: text;
  cursor: text;
}

.hl {
  border-radius: 3px;
  padding: 0 0.05em;
  box-decoration-break: clone;
}

.hl--moss {
  background: rgba(140, 168, 120, 0.38);
}
.hl--amber {
  background: rgba(220, 180, 110, 0.4);
}
.hl--sky {
  background: rgba(140, 180, 200, 0.35);
}

.toolbar {
  position: absolute;
  z-index: 20;
  min-width: 9.5rem;
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
  max-width: 12rem;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.swatches {
  display: flex;
  gap: 0.4rem;
  margin-bottom: 0.35rem;
}

.swatch {
  width: 1.35rem;
  height: 1.35rem;
  border: 2px solid rgba(45, 71, 57, 0.12);
  border-radius: 50%;
  cursor: pointer;
}
.swatch--moss { background: rgba(140, 168, 120, 0.85); }
.swatch--amber { background: rgba(220, 180, 110, 0.9); }
.swatch--sky { background: rgba(140, 180, 200, 0.85); }

.cancel {
  border: none;
  background: transparent;
  color: var(--island-muted);
  font-size: 0.72rem;
  cursor: pointer;
  padding: 0;
}
</style>
