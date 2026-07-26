<template>
  <p class="selectable-passage whitespace-pre-wrap text-sm leading-relaxed text-gray-800">
    <span
      v-for="(token, idx) in tokens"
      :key="idx"
      :class="token.word ? (disabled ? 'word-token word-token-disabled' : 'word-token') : ''"
      @click="token.word ? onWordClick(token.text) : undefined"
    >{{ token.text }}</span>
  </p>

  <WordLookupPopover
    v-model:show="popoverShow"
    :word="selectedWord"
    :passage-id="passageId"
    :passage-title="passageTitle"
    :source-type="sourceType"
    @added="emit('word-added')"
  />
</template>

<script setup lang="ts">
const props = defineProps<{
  content: string
  passageId?: number
  passageTitle?: string
  sourceType?: string
  /** 考试模式：禁止点词 */
  disabled?: boolean
}>()

const emit = defineEmits<{
  'word-added': []
}>()

const popoverShow = ref(false)
const selectedWord = ref('')

interface Token {
  text: string
  word: boolean
}

const tokens = computed(() => {
  const parts = props.content.split(/(\b[a-zA-Z'-]+\b)/g)
  return parts.filter(Boolean).map((text) => ({
    text,
    word: /^[a-zA-Z'-]+$/.test(text) && text.length > 1
  }))
})

function onWordClick(word: string) {
  if (props.disabled) return
  selectedWord.value = word
  popoverShow.value = true
}
</script>

<style scoped>
.word-token {
  @apply cursor-pointer rounded px-0.5 transition hover:bg-green-100 hover:text-green-800;
}
.word-token-disabled {
  @apply cursor-default;
}
</style>
