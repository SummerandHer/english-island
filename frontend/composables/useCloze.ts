import type { ClozeDensity, ClozeToken, VideoSentence } from '~/types/api'

/** 功能词：默认不挖 */
const STOP_WORDS = new Set(
  [
    'a', 'an', 'the', 'to', 'of', 'in', 'on', 'at', 'for', 'from', 'by', 'with',
    'and', 'or', 'but', 'as', 'if', 'is', 'are', 'was', 'were', 'be', 'been',
    'been', 'am', 'do', 'does', 'did', 'have', 'has', 'had', 'will', 'would',
    'can', 'could', 'should', 'shall', 'may', 'might', 'must', 'not', 'no',
    'it', 'its', 'this', 'that', 'these', 'those', 'i', 'you', 'he', 'she',
    'we', 'they', 'me', 'him', 'her', 'us', 'them', 'my', 'your', 'his', 'our',
    'their', 'so', 'than', 'then', 'there', 'here', 'up', 'out', 'about', 'into',
    'over', 'after', 'before', 'between', 'through', 'during', 'without', 'within'
  ].map((w) => w.toLowerCase())
)

interface WordSpan {
  /** 含标点的原文片段，如 "house," */
  raw: string
  /** 用于比对的词干字母，如 "house" */
  core: string
  start: number
  end: number
}

function extractWordSpans(text: string): WordSpan[] {
  const spans: WordSpan[] = []
  const re = /[A-Za-z]+(?:'[A-Za-z]+)?/g
  let m: RegExpExecArray | null
  while ((m = re.exec(text)) != null) {
    spans.push({
      raw: m[0],
      core: m[0].replace(/'s$/i, '').toLowerCase(),
      start: m.index,
      end: m.index + m[0].length
    })
  }
  return spans
}

function isDiggable(core: string): boolean {
  if (core.length < 4) return false
  if (STOP_WORDS.has(core)) return false
  return true
}

function pickBlankIndices(candidates: number[], density: ClozeDensity, seed: number): number[] {
  if (candidates.length === 0) return []
  if (density === 'one') {
    // 优先较长词：按 core 长度在 candidates 中选（由调用方已排序则取第一个随机偏移）
    const idx = Math.abs(seed) % candidates.length
    return [candidates[idx]]
  }
  // 多空：约 35%，上限 5
  const target = Math.min(5, Math.max(2, Math.round(candidates.length * 0.35)))
  const shuffled = [...candidates]
  // 简单可复现洗牌
  for (let i = shuffled.length - 1; i > 0; i--) {
    const j = Math.abs((seed * (i + 3)) ^ (i * 2654435761)) % (i + 1)
    ;[shuffled[i], shuffled[j]] = [shuffled[j], shuffled[i]]
  }
  return shuffled.slice(0, target).sort((a, b) => a - b)
}

/**
 * 将一句英文按挖空规则拆成 token 序列。
 * seed 变化可「换一批空」。
 */
export function buildClozeTokens(
  textEn: string,
  density: ClozeDensity,
  sentenceKey: string | number,
  seed = 0
): ClozeToken[] {
  const text = textEn?.trim() || ''
  if (!text) return [{ kind: 'text', text: '' }]

  const spans = extractWordSpans(text)
  const diggable = spans
    .map((s, i) => ({ i, len: s.core.length, ok: isDiggable(s.core) }))
    .filter((x) => x.ok)
    .sort((a, b) => b.len - a.len)
    .map((x) => x.i)

  const numericSeed = seed + hashKey(String(sentenceKey))
  // one：在最长候选里偏向更长词
  let candidates = diggable
  if (density === 'one' && diggable.length > 0) {
    const top = diggable.filter((i) => spans[i].core.length >= spans[diggable[0]].core.length - 1)
    candidates = top.length ? top : diggable
  }

  const blankSet = new Set(pickBlankIndices(candidates, density, numericSeed))
  if (blankSet.size === 0 && spans.length > 0) {
    // 兜底：挖最长词
    const longest = spans.reduce((bi, s, i) => (s.core.length > spans[bi].core.length ? i : bi), 0)
    blankSet.add(longest)
  }

  const tokens: ClozeToken[] = []
  let cursor = 0
  spans.forEach((span, i) => {
    if (span.start > cursor) {
      tokens.push({ kind: 'text', text: text.slice(cursor, span.start) })
    }
    if (blankSet.has(i)) {
      tokens.push({
        kind: 'blank',
        id: `${sentenceKey}-${i}`,
        answer: span.raw,
        display: '_'.repeat(Math.min(12, Math.max(4, span.raw.length)))
      })
    } else {
      tokens.push({ kind: 'text', text: span.raw })
    }
    cursor = span.end
  })
  if (cursor < text.length) {
    tokens.push({ kind: 'text', text: text.slice(cursor) })
  }
  return tokens
}

export function normalizeAnswer(input: string): string {
  return input
    .trim()
    .toLowerCase()
    .replace(/[.,!?;:'"]+$/g, '')
}

export function checkClozeAnswer(userInput: string, answer: string): boolean {
  return normalizeAnswer(userInput) === normalizeAnswer(answer)
}

function hashKey(s: string): number {
  let h = 0
  for (let i = 0; i < s.length; i++) h = (h * 31 + s.charCodeAt(i)) | 0
  return Math.abs(h)
}

export function useCloze(
  sentences: Ref<VideoSentence[]>,
  density: Ref<ClozeDensity>,
  seed: Ref<number>
) {
  const answers = ref<Record<string, string>>({})
  const results = ref<Record<string, boolean | null>>({})

  const clozeMap = computed(() => {
    const map = new Map<number, ClozeToken[]>()
    sentences.value.forEach((s) => {
      map.set(s.id, buildClozeTokens(s.textEn, density.value, s.id, seed.value))
    })
    return map
  })

  function getTokens(sentenceId: number): ClozeToken[] {
    return clozeMap.value.get(sentenceId) ?? [{ kind: 'text', text: '' }]
  }

  function setAnswer(blankId: string, value: string) {
    answers.value[blankId] = value
    const token = findBlank(blankId)
    if (token && value.trim()) {
      results.value[blankId] = checkClozeAnswer(value, token.answer)
    } else {
      results.value[blankId] = null
    }
  }

  function findBlank(blankId: string): Extract<ClozeToken, { kind: 'blank' }> | null {
    for (const tokens of clozeMap.value.values()) {
      for (const t of tokens) {
        if (t.kind === 'blank' && t.id === blankId) return t
      }
    }
    return null
  }

  function reveal(blankId: string) {
    const token = findBlank(blankId)
    if (!token) return
    answers.value[blankId] = token.answer
    results.value[blankId] = true
  }

  function resetAnswers() {
    answers.value = {}
    results.value = {}
  }

  watch([density, seed], () => resetAnswers())

  return {
    clozeMap,
    answers,
    results,
    getTokens,
    setAnswer,
    reveal,
    resetAnswers,
    reshuffle: () => {
      seed.value += 1
    }
  }
}
