export interface LearnSummaryLastReading {
  passageId: number
  passageTitle: string
  chapterSlug?: string
  correctCount: number
  totalQuestions: number
  createdAt: string
}

export interface LearnSummaryLastTranslation {
  questionId: number
  promptPreview: string
  cetScore: number
  score: number
  createdAt: string
}

export interface LearnSummaryNextTask {
  href: string
  label: string
  action: '继续' | '开始'
}

export interface LearnSummaryNextReading extends LearnSummaryNextTask {
  passageId: number
  passageTitle: string
  chapterSlug?: string
  correctCount?: number
  totalQuestions?: number
  neverDone?: boolean
  lowScore?: boolean
}

export interface LearnSummaryNextTranslation extends LearnSummaryNextTask {
  questionId: number
  promptPreview: string
  lastCetScore?: number
  neverDone?: boolean
  lowScore?: boolean
}

export interface LearnSummarySuggestion {
  type: 'vocab' | 'reading' | 'translation' | 'notebook'
  label: string
  href: string
  action: '继续' | '开始'
}

export interface LearnSummary {
  streakDays: number
  vocab: {
    dueCount: number
    todayDone: number
    dailyLimit: number
    masteredCount: number
    notebookCount: number
    todayRemaining: number
  }
  lastReading: LearnSummaryLastReading | null
  lastTranslation: LearnSummaryLastTranslation | null
  nextVocab: LearnSummaryNextTask & { dueCount: number }
  nextReading: LearnSummaryNextReading | null
  nextTranslation: LearnSummaryNextTranslation | null
  suggestions: LearnSummarySuggestion[]
}

