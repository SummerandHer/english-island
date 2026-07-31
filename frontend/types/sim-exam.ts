export interface SimExamHub {
  shortCount: number
  longCount: number
  complianceNote: string
}

export interface SimPassageCard {
  id: number
  title: string
  examLevel: string
  sectionType: string
  wordCount?: number | null
  vocabCount?: number | null
  recommendedMinutes?: number | null
  questionCount: number
}

export interface SimOptionView {
  questionId: number
  label: string
  content: string
}

export interface SimQuestionPractice {
  id: number
  questionType: string
  stem: string
  sortOrder?: number
  options: SimOptionView[]
}

export interface SimPracticeDetail {
  id: number
  title: string
  examLevel: string
  sectionType: string
  contentEn: string
  wordCount?: number | null
  vocabCount?: number | null
  recommendedMinutes?: number | null
  paragraphs?: Array<{ label: string; text: string }>
  questions: SimQuestionPractice[]
}

export interface SimQuestionResult {
  questionId: number
  stem: string
  skillTag?: string | null
  userLabel?: string | null
  correctLabel?: string | null
  correct: boolean
  locateEn?: string | null
  locateZh?: string | null
  explainCorrect?: string | null
  explainDistractors?: Record<string, string>
  explainTip?: string | null
  options: SimOptionView[]
}

export interface SimSubmitResult {
  passageId: number
  title: string
  correctCount: number
  totalQuestions: number
  elapsedSeconds: number
  recommendedMinutes?: number | null
  contentEn: string
  contentZh?: string | null
  vocab: Array<Record<string, string>>
  results: SimQuestionResult[]
}

export interface SimSubmissionSummary {
  submissionId: number
  passageId: number
  passageTitle: string
  correctCount: number
  totalQuestions: number
  elapsedSeconds: number
  createdAt?: string | null
}

export interface AdminSimSourceSummary {
  id: number
  examLevel: string
  sectionType: string
  title: string
  sourceMeta?: string | null
  status: string
  createdAt?: string | null
}

export interface AdminSimSourceDetail {
  id: number
  examLevel: string
  sectionType: string
  title: string
  passageEn: string
  questionsJson: string
  /** 便于编辑的纯文本题目 */
  questionsText?: string
  /** 便于编辑的答案，如 46.D 47.A */
  answersText?: string
  sourceMeta?: string | null
  licenseNote: string
  officialExplains?: string | null
  status: string
}

export interface AdminSimPassageSummary {
  id: number
  title: string
  examLevel: string
  sectionType: string
  status: string
  aiStatus: string
  wordCount?: number | null
  vocabCount?: number | null
  recommendedMinutes?: number | null
  derivedFromSourceId?: number | null
}

export interface SimGenerateResult {
  passageId: number
  aiStatus: string
  aiError?: string | null
  status: string
  warnings: string[]
  similarityScore?: number | null
}
