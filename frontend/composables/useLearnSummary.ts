import type {
  LearnSummary,
  ReadingChapterDetail,
  ReadingSubmissionSummary,
  TranslationQuestionSummary,
  TranslationSubmissionSummary,
  VocabCheckinStats,
  VocabStats,
  VocabTodayPlan
} from '~/types/api'
import { questionPromptPreview } from '~/utils/translationQuestion'

const LOW_READING_RATIO = 0.6
const LOW_TRANSLATION_CET = 9

/** Backend GET /learn/summary raw shape (P2-2) */
interface LearnSummaryApi {
  vocabulary: {
    masteredCount: number
    reviewTotal: number
    notebookCount: number
    todayDone: number
    dailyLimit: number
    todayRemaining: number
    streakDays: number
    examLevel: string
  }
  checkin: VocabCheckinStats
  reading: {
    latest: ReadingSubmissionSummary | null
    weekCount: number
  }
  translation: {
    latest: {
      submissionId: number
      questionId: number
      promptPreview: string
      direction?: string
      score: number
      cetScore: number
      band: string
      bandLabel: string
      createdAt: string
    } | null
    weekCount: number
  }
  suggestedActions: Array<{ type: string; label: string; path: string }>
}

function mapAggregateToLearnSummary(api: LearnSummaryApi): LearnSummary {
  const v = api.vocabulary
  const lastReading = api.reading.latest
  const lastTranslation = api.translation.latest

  const readingNever = !lastReading
  const readingLow =
    lastReading != null &&
    isLowReadingScore(lastReading.correctCount, lastReading.totalQuestions)

  const translationNever = !lastTranslation
  const translationLow =
    lastTranslation != null && lastTranslation.cetScore < LOW_TRANSLATION_CET

  const dueCount = v.todayRemaining

  const nextReading = lastReading
    ? {
        passageId: lastReading.passageId,
        passageTitle: lastReading.passageTitle,
        chapterSlug: lastReading.chapterSlug,
        href: `/reading/practice/${lastReading.passageId}${
          lastReading.chapterSlug ? `?chapter=${lastReading.chapterSlug}` : ''
        }`,
        action: readingAction(lastReading.correctCount, lastReading.totalQuestions),
        correctCount: lastReading.correctCount,
        totalQuestions: lastReading.totalQuestions,
        neverDone: false,
        lowScore: readingLow
      }
    : api.suggestedActions.some((a) => a.type === 'reading')
      ? {
          passageId: 0,
          passageTitle: api.suggestedActions.find((a) => a.type === 'reading')!.label,
          href: '/reading',
          action: '开始' as const,
          neverDone: true,
          lowScore: false
        }
      : null

  const nextTranslation = lastTranslation
    ? {
        questionId: lastTranslation.questionId,
        promptPreview: lastTranslation.promptPreview,
        href: `/translation/practice/${lastTranslation.questionId}`,
        action: translationAction(lastTranslation.cetScore),
        lastCetScore: lastTranslation.cetScore,
        neverDone: false,
        lowScore: translationLow
      }
    : api.suggestedActions.some((a) => a.type === 'translation')
      ? {
          questionId: 0,
          promptPreview: api.suggestedActions.find((a) => a.type === 'translation')!.label,
          href: '/translation',
          action: '开始' as const,
          neverDone: true,
          lowScore: false
        }
      : null

  const suggestions: LearnSummary['suggestions'] = api.suggestedActions.map((a) => ({
    type: a.type as LearnSummary['suggestions'][0]['type'],
    label: a.label,
    href: a.path,
    action: (a.type === 'vocab' && dueCount > 0 ? '继续' : '开始') as '继续' | '开始'
  }))

  if (v.notebookCount > 0 && !suggestions.some((s) => s.type === 'notebook')) {
    suggestions.push({
      type: 'notebook',
      label: `生词本 ${v.notebookCount} 词`,
      href: '/vocabulary?tab=notebook',
      action: '继续'
    })
  }

  return {
    streakDays: api.checkin.streakDays ?? v.streakDays,
    vocab: {
      dueCount,
      todayDone: v.todayDone,
      dailyLimit: v.dailyLimit,
      masteredCount: v.masteredCount,
      notebookCount: v.notebookCount,
      todayRemaining: v.todayRemaining
    },
    lastReading: lastReading
      ? {
          passageId: lastReading.passageId,
          passageTitle: lastReading.passageTitle,
          chapterSlug: lastReading.chapterSlug,
          correctCount: lastReading.correctCount,
          totalQuestions: lastReading.totalQuestions,
          createdAt: lastReading.createdAt
        }
      : null,
    lastTranslation: lastTranslation
      ? {
          questionId: lastTranslation.questionId,
          promptPreview: lastTranslation.promptPreview,
          cetScore: lastTranslation.cetScore,
          score: lastTranslation.score,
          createdAt: lastTranslation.createdAt
        }
      : null,
    nextVocab: {
      href: '/vocabulary',
      label:
        dueCount > 0
          ? `待复习 ${dueCount} 词`
          : v.todayDone >= v.dailyLimit
            ? `今日已完成 · 掌握 ${v.masteredCount} 词`
            : `今日 ${v.todayDone}/${v.dailyLimit}`,
      action: dueCount > 0 || v.todayDone > 0 ? '继续' : '开始',
      dueCount
    },
    nextReading,
    nextTranslation,
    suggestions
  }
}

function readingAction(
  correct?: number,
  total?: number
): '继续' | '开始' {
  if (correct == null || total == null || total <= 0) return '开始'
  return '继续'
}

function translationAction(cetScore?: number): '继续' | '开始' {
  if (cetScore == null) return '开始'
  return '继续'
}

function isLowReadingScore(correct: number, total: number) {
  return total > 0 && correct / total < LOW_READING_RATIO
}

function buildSummaryFromParts(
  stats: VocabStats,
  today: VocabTodayPlan,
  checkin: VocabCheckinStats | null,
  lastReading: ReadingSubmissionSummary | null,
  lastTranslation: TranslationSubmissionSummary | null,
  readingChapter: ReadingChapterDetail | null,
  translationQuestions: TranslationQuestionSummary[]
): LearnSummary {
  const streakDays = checkin?.streakDays ?? stats.streakDays ?? 0
  const vocabDue = today.dueCount > 0

  const passage = readingChapter?.passages?.[0]
  const lastReadOnPassage =
    lastReading && passage && lastReading.passageId === passage.id
      ? lastReading
      : lastReading

  const readingNever = !lastReading
  const readingLow =
    lastReading != null &&
    isLowReadingScore(lastReading.correctCount, lastReading.totalQuestions)

  const freeQ =
    translationQuestions.find((q) => !q.vip) ?? translationQuestions[0] ?? null
  const translationNever = !lastTranslation
  const translationLow =
    lastTranslation != null && lastTranslation.cetScore < LOW_TRANSLATION_CET

  const nextReading =
    passage != null
      ? {
          passageId: passage.id,
          passageTitle: passage.title,
          chapterSlug: readingChapter?.slug,
          href: `/reading/practice/${passage.id}?chapter=${readingChapter?.slug ?? ''}`,
          action: readingAction(
            lastReadOnPassage?.correctCount,
            lastReadOnPassage?.totalQuestions
          ) as '继续' | '开始',
          correctCount: lastReadOnPassage?.correctCount,
          totalQuestions: lastReadOnPassage?.totalQuestions,
          neverDone: readingNever,
          lowScore: readingLow
        }
      : null

  const nextTranslation = freeQ
    ? {
        questionId: freeQ.id,
        promptPreview: questionPromptPreview(freeQ),
        href: `/translation/practice/${freeQ.id}`,
        action: translationAction(lastTranslation?.cetScore) as '继续' | '开始',
        lastCetScore: lastTranslation?.cetScore,
        neverDone: translationNever,
        lowScore: translationLow
      }
    : null

  const suggestions: LearnSummary['suggestions'] = []
  if (vocabDue) {
    suggestions.push({
      type: 'vocab',
      label: `待复习 ${today.dueCount} 词`,
      href: '/vocabulary',
      action: '继续'
    })
  } else if (stats.todayDone < stats.dailyLimit) {
    suggestions.push({
      type: 'vocab',
      label: `今日词汇 ${stats.todayDone}/${stats.dailyLimit}`,
      href: '/vocabulary',
      action: stats.todayDone > 0 ? '继续' : '开始'
    })
  }
  if (nextReading && (readingNever || readingLow)) {
    suggestions.push({
      type: 'reading',
      label: nextReading.passageTitle,
      href: nextReading.href,
      action: nextReading.action
    })
  }
  if (nextTranslation && (translationNever || translationLow)) {
    suggestions.push({
      type: 'translation',
      label: nextTranslation.promptPreview,
      href: nextTranslation.href,
      action: nextTranslation.action
    })
  }
  if (stats.notebookCount > 0 && suggestions.length < 3) {
    suggestions.push({
      type: 'notebook',
      label: `生词本 ${stats.notebookCount} 词`,
      href: '/vocabulary?tab=notebook',
      action: '继续'
    })
  }

  return {
    streakDays,
    vocab: {
      dueCount: today.dueCount,
      todayDone: stats.todayDone,
      dailyLimit: stats.dailyLimit,
      masteredCount: stats.masteredCount,
      notebookCount: stats.notebookCount,
      todayRemaining: stats.todayRemaining
    },
    lastReading: lastReading
      ? {
          passageId: lastReading.passageId,
          passageTitle: lastReading.passageTitle,
          chapterSlug: lastReading.chapterSlug,
          correctCount: lastReading.correctCount,
          totalQuestions: lastReading.totalQuestions,
          createdAt: lastReading.createdAt
        }
      : null,
    lastTranslation: lastTranslation
      ? {
          questionId: lastTranslation.questionId,
          promptPreview: lastTranslation.promptPreview,
          cetScore: lastTranslation.cetScore,
          score: lastTranslation.score,
          createdAt: lastTranslation.createdAt
        }
      : null,
    nextVocab: {
      href: '/vocabulary',
      label:
        today.dueCount > 0
          ? `待复习 ${today.dueCount} 词`
          : stats.todayDone >= stats.dailyLimit
            ? `今日已完成 · 掌握 ${stats.masteredCount} 词`
            : `今日 ${stats.todayDone}/${stats.dailyLimit}`,
      action: (today.dueCount > 0 || stats.todayDone > 0 ? '继续' : '开始') as '继续' | '开始',
      dueCount: today.dueCount
    },
    nextReading,
    nextTranslation,
    suggestions
  }
}

async function buildFallbackSummary(
  request: <T>(path: string, options?: Parameters<typeof $fetch>[1]) => Promise<T>
): Promise<LearnSummary> {
  const [stats, today, checkinResult, readingSubs, translationSubs, readingChapter, questions] =
    await Promise.all([
      request<VocabStats>('/api/v1/vocabulary/stats'),
      request<VocabTodayPlan>('/api/v1/vocabulary/today'),
      request<VocabCheckinStats>('/api/v1/vocabulary/checkin').catch(() => null),
      request<ReadingSubmissionSummary[]>('/api/v1/reading/submissions/mine?limit=1').catch(
        () => [] as ReadingSubmissionSummary[]
      ),
      request<TranslationSubmissionSummary[]>(
        '/api/v1/translation/submissions/mine?limit=1'
      ).catch(() => [] as TranslationSubmissionSummary[]),
      request<ReadingChapterDetail>('/api/v1/reading/chapters/skim-questions-first').catch(
        () => null
      ),
      request<TranslationQuestionSummary[]>('/api/v1/translation/questions').catch(() => [])
    ])

  return buildSummaryFromParts(
    stats,
    today,
    checkinResult,
    readingSubs[0] ?? null,
    translationSubs[0] ?? null,
    readingChapter,
    questions
  )
}

export function useLearnSummary() {
  const { request } = useApi()
  const summary = ref<LearnSummary | null>(null)
  const loading = ref(false)
  const fromAggregateApi = ref(false)

  async function load() {
    loading.value = true
    fromAggregateApi.value = false
    try {
      const raw = await request<LearnSummaryApi>('/api/v1/learn/summary')
      summary.value = mapAggregateToLearnSummary(raw)
      fromAggregateApi.value = true
    } catch {
      try {
        summary.value = await buildFallbackSummary(request)
      } catch {
        summary.value = null
      }
    } finally {
      loading.value = false
    }
  }

  /** Higher = more urgent for homepage task ordering */
  function taskPriority(type: 'vocab' | 'notebook' | 'reading' | 'translation'): number {
    const s = summary.value
    if (!s) return 0
    if (type === 'vocab') {
      if (s.vocab.dueCount > 0) return 100
      if (s.vocab.todayDone < s.vocab.dailyLimit) return 80
      return 20
    }
    if (type === 'notebook') {
      return s.vocab.notebookCount > 0 ? 40 : 10
    }
    if (type === 'reading') {
      const r = s.nextReading
      if (!r) return 0
      if (r.neverDone) return 70
      if (r.lowScore) return 60
      return 30
    }
    if (type === 'translation') {
      const t = s.nextTranslation
      if (!t) return 0
      if (t.neverDone) return 50
      if (t.lowScore) return 45
      return 25
    }
    return 0
  }

  return { summary, loading, fromAggregateApi, load, taskPriority }
}
