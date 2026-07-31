<template>
  <div class="current-sentence">
    <div class="current-sentence__meta">
      <span>{{ index + 1 }} / {{ total }}</span>
      <span v-if="studyMode === 'blind' && !revealed" class="current-sentence__tag">盲听中</span>
      <span v-else-if="studyMode === 'cloze'" class="current-sentence__tag">挖空练习</span>
    </div>

    <template v-if="!sentence">
      <p class="current-sentence__empty">暂无字幕</p>
    </template>

    <template v-else-if="studyMode === 'blind' && !revealed">
      <div class="blind-box">
        <p>先听本句，再揭晓字幕</p>
        <button type="button" class="reveal-btn" @click="$emit('reveal')">显示本句</button>
      </div>
    </template>

    <template v-else-if="studyMode === 'cloze'">
      <p class="line-en cloze-line">
        <template v-for="(tok, ti) in tokens" :key="ti">
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
            :size="Math.min(16, Math.max(4, tok.answer.length + 1))"
            @input="$emit('blank-input', tok.id, ($event.target as HTMLInputElement).value)"
            @keydown.enter.prevent="$emit('blank-input', tok.id, ($event.target as HTMLInputElement).value)"
          />
        </template>
      </p>
      <p v-if="sentence.textZh" class="line-zh">{{ sentence.textZh }}</p>
    </template>

    <template v-else>
      <p v-if="showEn" class="line-en">{{ sentence.textEn }}</p>
      <p
        v-if="showZh"
        class="line-zh"
        :class="{ 'line-zh--empty': !sentence.textZh }"
      >
        {{ sentence.textZh || '（暂无中文）' }}
      </p>
    </template>
  </div>
</template>

<script setup lang="ts">
import type { ClozeToken, StudyMode, SubtitleMode, VideoSentence } from '~/types/api'

const props = defineProps<{
  sentence?: VideoSentence
  index: number
  total: number
  mode?: SubtitleMode
  studyMode?: StudyMode
  revealed?: boolean
  tokens?: ClozeToken[]
  answers?: Record<string, string>
  results?: Record<string, boolean | null>
}>()

defineEmits<{
  reveal: []
  'blank-input': [blankId: string, value: string]
}>()

const studyMode = computed(() => props.studyMode ?? 'intensive')
const mode = computed(() => props.mode ?? 'bilingual')
const tokens = computed(() => props.tokens ?? [])
const answers = computed(() => props.answers ?? {})
const results = computed(() => props.results ?? {})

const showEn = computed(() => mode.value !== 'zh')
const showZh = computed(() => mode.value !== 'en')
</script>

<style scoped>
.current-sentence {
  display: flex;
  min-height: 0;
  flex-direction: column;
  justify-content: center;
  border: 1px solid rgba(47, 67, 51, 0.08);
  border-radius: 16px;
  background: #fff;
  padding: 0.9rem 1.1rem 1.05rem;
  text-align: center;
}

.current-sentence__meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 0.55rem;
  font-size: 0.75rem;
  color: #8a968c;
}

.current-sentence__tag {
  color: #a3aea6;
}

.current-sentence__empty {
  margin: 0;
  font-size: 0.9rem;
  color: #a3aea6;
}

.line-en {
  margin: 0;
  font-family: Georgia, 'Times New Roman', 'Noto Serif SC', serif;
  font-size: clamp(1.15rem, 1.8vw, 1.45rem);
  font-weight: 600;
  line-height: 1.55;
  letter-spacing: 0.01em;
  color: #1f2a24;
}

.line-zh {
  margin: 0.55rem 0 0;
  font-size: clamp(0.92rem, 1.2vw, 1.05rem);
  line-height: 1.6;
  color: #5f6f64;
}

.line-zh--empty {
  font-style: italic;
  color: #b4beb6;
}

.blind-box {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.75rem;
  padding: 1rem 0;
  color: #8a968c;
  font-size: 0.9rem;
}

.reveal-btn {
  border: 1px solid rgba(59, 83, 62, 0.22);
  border-radius: 999px;
  background: #f3f7f1;
  padding: 0.4rem 1rem;
  font-size: 0.85rem;
  color: #3b533e;
  cursor: pointer;
}

.reveal-btn:hover {
  background: #e7efe4;
}

.cloze-line {
  line-height: 2;
}

.cloze-input {
  display: inline-block;
  min-width: 3.5rem;
  margin: 0 0.15rem;
  border: 1px dashed rgba(59, 83, 62, 0.28);
  border-radius: 6px;
  background: #f7faf5;
  padding: 0.1rem 0.4rem;
  text-align: center;
  font-family: inherit;
  font-size: 1rem;
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
</style>
