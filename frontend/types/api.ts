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

export interface TranslationChapterDetail {
  id: number
  title: string
  slug: string
  summary?: string
  contentHtml: string
  vip: boolean
  recommendedVideo?: VideoSummary | null
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
  examLevel?: string
  items: VocabTodayItem[]
}

export interface VocabCheckinStats {
  streakDays: number
  week: Array<{ date: string; checked: boolean }>
}

export interface VocabSettings {
  examLevel: string
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

export interface VocabStats {
  masteredCount: number
  reviewTotal: number
  notebookCount: number
  todayDone: number
  dailyLimit: number
  todayRemaining: number
  streakDays?: number
  examLevel?: string
}

export interface VocabNotebookItem {
  vocab: VocabListItem
  sourceType?: string
  sourceId?: number
  note?: string
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

export interface DailyTopic {
  slug: string
  label: string
}

export interface DailyArticleSummary {
  id: number | null
  title: string | null
  slug: string | null
  topic: string | null
  topicLabel: string | null
  difficulty: string | null
  coverUrl: string | null
  publishDate: string
  wordCount: number | null
  checkedIn: boolean
  unlocked: boolean
  weekdayLabel: string
}

export interface DailyAnnotation {
  id: number
  startOffset: number
  endOffset: number
  selectedText: string
  color: string
  note?: string | null
}

export interface DailyVocabItem {
  word: string
  zh?: string
  pos?: string
  note?: string
  inGlossary?: boolean
  vocabularyId?: number
}

export interface DailyStructureItem {
  en: string
  zh?: string
  hint?: string
}

export interface DailySentenceItem {
  en: string
  zh?: string
  startOffset: number
  endOffset: number
}

export interface DailyArticleDetail {
  id: number
  title: string
  slug: string
  topic: string
  topicLabel: string
  difficulty: string
  contentEn: string
  contentZh?: string | null
  coverUrl?: string | null
  summaryZh?: string | null
  publishDate: string
  sourcePublishedAt?: string | null
  sourceAuthor?: string | null
  sourcePlace?: string | null
  wordCount?: number | null
  cetVocab: DailyVocabItem[]
  hardVocab: DailyVocabItem[]
  structures: DailyStructureItem[]
  sentences: DailySentenceItem[]
  checkedIn: boolean
  annotations: DailyAnnotation[]
  related: DailyArticleSummary[]
}

export interface DailyHubPayload {
  weekSlots: DailyArticleSummary[]
  archive: DailyArticleSummary[]
  streakDays: number
  todayArticle: DailyArticleSummary | null
  topics: DailyTopic[]
}

export interface AdminDailyArticleSummary {
  id: number
  title: string
  slug: string
  topic: string
  difficulty: string
  publishDate: string
  status: string
  wordCount?: number | null
  coverUrl?: string | null
}

export interface AdminDailyArticleDetail {
  id: number
  title: string
  slug: string
  topic: string
  difficulty: string
  contentEn: string
  contentZh?: string | null
  coverUrl?: string | null
  coverAssetId?: number | null
  summaryZh?: string | null
  publishDate: string
  sourceId?: number | null
  sourcePublishedAt?: string | null
  sourceAuthor?: string | null
  sourcePlace?: string | null
  cetVocabJson?: string | null
  hardVocabJson?: string | null
  structuresJson?: string | null
  sentencesJson?: string | null
  wordCount?: number | null
  status: string
  aiStatus?: string | null
  aiError?: string | null
  aiVersion?: string | null
}

export interface DailyAiEnrichmentResult {
  gate: 'ok' | 'needs_review' | 'failed' | string
  warnings: string[]
  error?: string | null
  aiVersion?: string | null
  summaryZh?: string | null
  topic?: string | null
  difficulty?: string | null
  slugSuggestion?: string | null
  wordCount?: number | null
  coverHint?: string | null
  cetVocabJson?: string | null
  hardVocabJson?: string | null
  structuresJson?: string | null
  contentZh?: string | null
  sentencesJson?: string | null
  aiRawJson?: string | null
  cetVocab?: DailyVocabItem[]
  hardVocab?: DailyVocabItem[]
  structures?: DailyStructureItem[]
  sentences?: DailySentenceItem[]
}

/** 仿真题岛 */
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
