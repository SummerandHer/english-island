export interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

export interface UserProfile {
  id: number
  email: string
  nickname: string
  avatarUrl?: string
  vip: boolean
  vipExpireAt?: string
  role?: string
  admin?: boolean
}

export interface AuthData {
  token: string
  user: UserProfile
}

export interface VideoSentence {
  id: number
  seq: number
  startMs: number
  endMs: number
  textEn: string
  textZh: string
}

export interface VideoSummary {
  id: number
  title: string
  description?: string
  coverUrl?: string
  durationSec?: number
  difficulty: string
  tags: string[]
  sentenceCount: number
  vocabCount: number
  vip: boolean
  favorited: boolean
  createdAt?: string
}

export type SubtitleMode = 'bilingual' | 'en' | 'zh'

export interface VideoDetail {
  id: number
  title: string
  description?: string
  coverUrl?: string
  storageType: string
  provider: string
  sourceUrl: string
  embedBvid?: string
  playUrl?: string
  durationSec?: number
  vocabCount?: number
  sentenceCount?: number
  vip: boolean
  favorited: boolean
  sentences: VideoSentence[]
}

export interface PostItem {
  id: number
  userId: number
  authorNickname: string
  authorAvatar?: string
  content: string
  images: string[]
  likeCount: number
  createdAt: string
}

export interface PageResult<T> {
  items: T[]
  total: number
  page: number
  size: number
}

export interface SentenceDraft {
  seq: number
  startMs: number
  endMs: number
  textEn: string
  textZh: string
}

export interface ParseVideoResult {
  videoObjectKey: string
  playUrl: string
  fileId: number
  durationMs?: number
  durationSec?: number
  sentences: SentenceDraft[]
  logs: string[]
}

export interface AdminVideoSummary {
  id: number
  title: string
  coverUrl?: string
  storageType: string
  status: number
  durationSec?: number
  vip: boolean
  sentenceCount: number
  createdAt: string
}

export interface AdminVideoDetail {
  id: number
  seriesId?: number
  title: string
  description?: string
  coverUrl?: string
  storageType: string
  provider: string
  sourceUrl: string
  embedBvid?: string
  playUrl?: string
  durationSec?: number
  difficulty: string
  vip: boolean
  sortOrder: number
  status: number
  createdAt: string
  sentences: Array<{
    id: number
    seq: number
    startMs: number
    endMs: number
    textEn: string
    textZh: string
  }>
}

export interface VideoSeriesItem {
  id: number
  title: string
  description?: string
  coverUrl?: string
  sortOrder: number
  status: number
}

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
  passages: ReadingPassageSummary[]
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

export interface ReadingPassagePractice {
  id: number
  chapterId: number
  title: string
  contentEn: string
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

export interface VocabListItem {
  id: number
  word: string
  phonetic?: string
  partOfSpeech?: string
  meaningBrief: string
  examLevel: string
  freqRank?: number
}

export interface VocabPhrase {
  en: string
  zh: string
}

export interface VocabDetail {
  summary: VocabListItem
  meaningZh: string
  exampleEn?: string
  exampleZh?: string
  collocation?: string
  phrases: VocabPhrase[]
}

export interface VocabTodayItem {
  vocab: VocabListItem
  familiarity: number
}

export interface VocabTodayPlan {
  dueCount: number
  dailyLimit: number
  items: VocabTodayItem[]
}

export interface ReadingSubmitResult {
  passageId: number
  passageTitle: string
  correctCount: number
  totalQuestions: number
  answeredCount: number
  questions: ReadingQuestionResult[]
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
