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
