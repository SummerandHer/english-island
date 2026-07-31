import type { VideoSummary } from './video'

export interface TranslationGradingError {
  span: string
  suggestion: string
  reason: string
}

export interface TranslationQuestionSummary {
  id: number
  promptZh?: string
  promptEn?: string
  direction: string
  mock: boolean
  vip: boolean
}

export type TranslationQuestionDetail = TranslationQuestionSummary

export interface TranslationSubmissionResult {
  submissionId: number
  score: number
  cetScore: number
  band: string
  bandLabel: string
  bandDescription: string
  overallComment: string
  errors: TranslationGradingError[]
  referenceHint: string
  aiModel: string
}

export interface TranslationChapterDetail {
  id: number
  title: string
  slug: string
  summary?: string
  contentHtml: string
  vip: boolean
  recommendedVideo?: VideoSummary | null
}

export interface TranslationSubmissionSummary {
  submissionId: number
  questionId: number
  promptPreview: string
  direction?: string
  userAnswer: string
  score: number
  cetScore: number
  band: string
  bandLabel: string
  bandDescription: string
  overallComment: string
  errors: TranslationGradingError[]
  errorCount: number
  referenceHint: string
  aiModel: string
  createdAt: string
}

/** GET /api/v1/learn/summary — aggregate learning dashboard (P2-2) */
