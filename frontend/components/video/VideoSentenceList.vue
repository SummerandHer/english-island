<template>
  <div class="sentence-panel flex min-h-0 flex-1 flex-col overflow-hidden">
    <!-- 练习模式 -->
    <div class="panel-toolbar">
      <div class="mode-row">
        <button
          v-for="m in studyModes"
          :key="m.value"
          type="button"
          class="mode-tab"
          :class="{
            'mode-tab--active': studyMode === m.value,
            'mode-tab--disabled': m.disabled
          }"
          :disabled="m.disabled"
          :title="m.disabled ? '即将上线' : undefined"
          @click="!m.disabled && (studyMode = m.value)"
        >
          {{ m.label }}
        </button>
      </div>

      <!-- 挖空强度 -->
      <div v-if="studyMode === 'cloze'" class="density-row">
        <button
          type="button"
          class="density-btn"
          :class="{ 'density-btn--active': clozeDensity === 'one' }"
          @click="clozeDensity = 'one'"
        >
          每句 1 空
        </button>
        <button
          type="button"
          class="density-btn"
          :class="{ 'density-btn--active': clozeDensity === 'many' }"
          @click="clozeDensity = 'many'"
        >
          每句多空
        </button>
        <button type="button" class="density-btn density-btn--ghost" @click="$emit('reshuffle')">
          换一批空
        </button>
      </div>

      <!-- 语言显示（精听） -->
      <div v-if="studyMode === 'intensive'" class="lang-row">
        <button
          v-for="l in langModes"
          :key="l.value"
          type="button"
          class="lang-tab"
          :class="{ 'lang-tab--active': mode === l.value }"
          @click="mode = l.value"
        >
          {{ l.label }}
        </button>
      </div>
      <p v-else-if="studyMode === 'blind'" class="hint-text">
        字幕已隐藏，听完可点「显示本句」揭晓
      </p>
      <p v-else class="hint-text">听写关键词填入空格，回车或失焦自动判题</p>
    </div>

    <!-- 句列表 -->
    <div class="sentence-scroll">
      <div
        v-for="(s, i) in sentences"
        :key="s.id"
        :ref="(el) => setRowRef(i, el)"
        class="sentence-row"
        :class="{ 'sentence-row--active': i === currentIndex }"
        @click="$emit('select', i)"
      >
        <div class="sentence-row__meta">
          <span>{{ s.seq }}</span>
          <span>{{ formatTime(s.startMs) }} - {{ formatTime(s.endMs) }}</span>
          <button
            type="button"
            class="repeat-btn"
            title="重听本句"
            @click.stop="$emit('repeat', i)"
          >
            ↻
          </button>
        </div>

        <!-- 精听 -->
        <template v-if="studyMode === 'intensive'">
          <p v-if="mode !== 'zh'" class="sentence-en">{{ s.textEn }}</p>
          <p v-if="mode !== 'en' && s.textZh" class="sentence-zh">{{ s.textZh }}</p>
          <p v-else-if="mode !== 'en' && !s.textZh" class="sentence-zh sentence-zh--empty">（暂无中文）</p>
        </template>

        <!-- 盲听 -->
        <template v-else-if="studyMode === 'blind'">
          <template v-if="revealed[s.id]">
            <p class="sentence-en">{{ s.textEn }}</p>
            <p v-if="s.textZh" class="sentence-zh">{{ s.textZh }}</p>
          </template>
          <div v-else class="blind-mask">
            <span class="blind-mask__text">内容已隐藏</span>
            <button type="button" class="reveal-btn" @click.stop="$emit('reveal', s.id)">
              显示本句
            </button>
          </div>
        </template>

        <!-- 挖空 -->
        <template v-else>
          <p class="sentence-en sentence-en--cloze">
            <template v-for="(tok, ti) in getTokens(s.id)" :key="ti">
              <span v-if="tok.kind === 'text'">{{ tok.text }}</span>
              <input
                v-else
                class="cloze-input"
                :class="{
                  'cloze-input--ok': results[tok.id] === true,
                  'cloze-input--bad': results[tok.id] === false
                }"
                :value="answers[tok.id] ?? ''"
                :placeholder="tok.display"
                :size="Math.min(14, Math.max(4, tok.answer.length + 1))"
                @click.stop
                @input="onBlankInput(tok.id, ($event.target as HTMLInputElement).value)"
                @keydown.enter.prevent="onBlankInput(tok.id, ($event.target as HTMLInputElement).value)"
              />
            </template>
          </p>
          <p v-if="s.textZh" class="sentence-zh">{{ s.textZh }}</p>
        </template>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { ClozeDensity, ClozeToken, StudyMode, SubtitleMode, VideoSentence } from '~/types/api'

const props = defineProps<{
  sentences: VideoSentence[]
  currentIndex: number
  getTokens: (sentenceId: number) => ClozeToken[]
  answers: Record<string, string>
  results: Record<string, boolean | null>
  /** 盲听已揭晓的句子 id */
  revealed: Record<number, boolean>
}>()

const emit = defineEmits<{
  select: [index: number]
  repeat: [index: number]
  reshuffle: []
  'blank-input': [blankId: string, value: string]
  reveal: [sentenceId: number]
}>()

const mode = defineModel<SubtitleMode>('mode', { default: 'bilingual' })
const studyMode = defineModel<StudyMode>('studyMode', { default: 'intensive' })
const clozeDensity = defineModel<ClozeDensity>('clozeDensity', { default: 'one' })

const rowRefs = ref<(Element | null)[]>([])

const studyModes: Array<{ value: StudyMode | 'shadow' | 'dictation'; label: string; disabled?: boolean }> = [
  { value: 'intensive', label: '精听' },
  { value: 'blind', label: '盲听' },
  { value: 'shadow', label: '跟读', disabled: true },
  { value: 'dictation', label: '听写', disabled: true },
  { value: 'cloze', label: '挖空' }
]

const langModes: Array<{ value: SubtitleMode; label: string }> = [
  { value: 'bilingual', label: '双语' },
  { value: 'en', label: '英' },
  { value: 'zh', label: '中' }
]

watch(
  () => props.currentIndex,
  async () => {
    await nextTick()
    const el = rowRefs.value[props.currentIndex]
    if (el && 'scrollIntoView' in el) {
      ;(el as HTMLElement).scrollIntoView({ block: 'nearest', behavior: 'smooth' })
    }
  }
)

function setRowRef(i: number, el: Element | null) {
  rowRefs.value[i] = el
}

function onBlankInput(blankId: string, value: string) {
  emit('blank-input', blankId, value)
}

function formatTime(ms: number) {
  const sec = Math.floor(ms / 1000)
  const m = Math.floor(sec / 60)
  const s = sec % 60
  return `${m}:${String(s).padStart(2, '0')}`
}
</script>

<style scoped>
.sentence-panel {
  border: 1px solid rgba(47, 67, 51, 0.08);
  border-radius: 16px;
  background: #fff;
}

.panel-toolbar {
  flex-shrink: 0;
  padding: 0.75rem 0.9rem;
  border-bottom: 1px solid rgba(47, 67, 51, 0.08);
  background: rgba(250, 251, 249, 0.9);
}

.mode-row,
.lang-row,
.density-row {
  display: flex;
  flex-wrap: wrap;
  gap: 0.4rem;
}

.mode-row + .lang-row,
.mode-row + .density-row,
.mode-row + .hint-text,
.density-row {
  margin-top: 0.55rem;
}

.mode-tab {
  border: 1px solid transparent;
  border-radius: 8px;
  background: transparent;
  padding: 0.35rem 0.7rem;
  font-size: 0.8rem;
  color: #6b7a6f;
  cursor: pointer;
  transition: background 0.15s ease, color 0.15s ease, border-color 0.15s ease;
}

.mode-tab:hover:not(:disabled) {
  background: #f1f4ee;
  color: #3b533e;
}

.mode-tab--active {
  border-color: rgba(59, 83, 62, 0.2);
  background: #eef4ea;
  color: #2f4333;
  font-weight: 600;
}

.mode-tab--disabled {
  cursor: not-allowed;
  color: #c5cdc7;
}

.lang-tab,
.density-btn {
  border: 1px solid rgba(47, 67, 51, 0.1);
  border-radius: 999px;
  background: #fff;
  padding: 0.25rem 0.7rem;
  font-size: 0.75rem;
  color: #5f6f64;
  cursor: pointer;
}

.lang-tab--active,
.density-btn--active {
  border-color: #3b533e;
  background: #3b533e;
  color: #f5f8f3;
}

.density-btn--ghost {
  border-style: dashed;
  color: #8a968c;
}

.hint-text {
  margin: 0.55rem 0 0;
  font-size: 0.75rem;
  color: #8a968c;
}

.sentence-scroll {
  min-height: 0;
  flex: 1;
  overflow-y: auto;
  padding: 0.65rem 0.55rem 0.9rem;
}

.sentence-row {
  margin-bottom: 0.35rem;
  border-radius: 12px;
  padding: 0.85rem 0.9rem;
  text-align: left;
  cursor: pointer;
  transition: background 0.15s ease, box-shadow 0.15s ease;
}

.sentence-row:hover {
  background: #f5f7f3;
}

.sentence-row--active {
  background: #fff;
  box-shadow: inset 0 0 0 1px rgba(59, 83, 62, 0.16);
}

.sentence-row__meta {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-bottom: 0.4rem;
  font-size: 0.72rem;
  color: #93a096;
}

.repeat-btn {
  margin-left: auto;
  border: none;
  border-radius: 6px;
  background: transparent;
  padding: 0.1rem 0.4rem;
  font-size: 0.9rem;
  color: #93a096;
  cursor: pointer;
}

.repeat-btn:hover {
  background: #eef4ea;
  color: #3b533e;
}

.sentence-en {
  margin: 0;
  font-size: 1.02rem;
  font-weight: 600;
  line-height: 1.65;
  color: #243029;
}

.sentence-en--cloze {
  line-height: 2;
}

.sentence-zh {
  margin: 0.35rem 0 0;
  font-size: 0.92rem;
  line-height: 1.6;
  color: #627268;
}

.sentence-zh--empty {
  font-style: italic;
  color: #b4beb6;
}

.blind-mask {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.5rem;
  border-radius: 10px;
  background: #f3f5f1;
  padding: 0.85rem 0.9rem;
}

.blind-mask__text {
  font-size: 0.85rem;
  color: #8a968c;
}

.reveal-btn {
  flex-shrink: 0;
  border: 1px solid rgba(59, 83, 62, 0.2);
  border-radius: 999px;
  background: #fff;
  padding: 0.3rem 0.7rem;
  font-size: 0.75rem;
  color: #3b533e;
  cursor: pointer;
}

.reveal-btn:hover {
  background: #eef4ea;
}

.cloze-input {
  display: inline-block;
  min-width: 3rem;
  margin: 0 0.12rem;
  border: 1px dashed rgba(59, 83, 62, 0.28);
  border-radius: 6px;
  background: #f7faf5;
  padding: 0.1rem 0.35rem;
  text-align: center;
  font-size: 0.95rem;
  color: #1f2a24;
  outline: none;
}

.cloze-input:focus {
  border-style: solid;
  border-color: #3b533e;
  background: #fff;
}

.cloze-input--ok {
  border-color: #6f9b72;
  background: #eef6ee;
  color: #2f5a38;
}

.cloze-input--bad {
  border-color: #c98989;
  background: #faf2f2;
  color: #8a4444;
}

@media (min-width: 768px) {
  .sentence-en {
    font-size: 1.08rem;
  }

  .sentence-zh {
    font-size: 0.98rem;
  }

  .sentence-panel {
    max-height: none;
  }
}

@media (min-width: 1100px) {
  .sentence-en {
    font-size: 1.14rem;
  }

  .sentence-zh {
    font-size: 1.02rem;
  }
}
</style>
