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
  /** vocabulary | dict */
  lookupSource?: string
  matchedWord?: string
  queryWord?: string
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

