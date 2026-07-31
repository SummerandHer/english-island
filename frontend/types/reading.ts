import type { VideoSummary } from './video'

export interface ReadingPassageSummary {
  id: number
  chapterId: number
  title: string
  difficulty: string
  mock: boolean
  wordCount?: number
  questionCount: number
}

export interface ReadingChapterDetail {
  id: number
  title: string
  slug: string
  summary?: string
  contentHtml: string
  vip: boolean
  finished?: boolean
  passages: ReadingPassageSummary[]
  recommendedVideo?: VideoSummary | null
}

export interface ReadingQuestionOption {
  label: string
  content: string
}

export interface ReadingQuestionItem {
  id: number
  questionType: string
  stem: string
  sortOrder: number
  options: ReadingQuestionOption[]
}

export interface ReadingLongSentence {
  en: string
  zh: string
  hint?: string
}

export interface ReadingPassagePractice {
  id: number
  chapterId: number
  title: string
  contentEn: string
  longSentences?: ReadingLongSentence[]
  wordCount?: number
  difficulty: string
  mock: boolean
  questions: ReadingQuestionItem[]
}

export interface ReadingQuestionResult {
  questionId: number
  stem: string
  userLabel: string
  correctLabel: string
  correct: boolean
  explanation?: string
}

export interface ReadingSubmitResult {
  passageId: number
  passageTitle: string
  correctCount: number
  totalQuestions: number
  answeredCount: number
  questions: ReadingQuestionResult[]
}

export interface ReadingSubmissionSummary {
  submissionId: number
  passageId: number
  passageTitle: string
  chapterSlug?: string
  correctCount: number
  totalQuestions: number
  createdAt: string
}

export interface ReadingPassageLastScore {
  correctCount: number
  totalQuestions: number
  createdAt: string
}

