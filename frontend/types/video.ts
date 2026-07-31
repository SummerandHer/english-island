export interface VideoTag {
  id: number
  name: string
  slug: string
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
  tags: VideoTag[]
  sentenceCount: number
  vocabCount: number
  vip: boolean
  favorited: boolean
  learned?: boolean
  createdAt?: string
}

export type SubtitleMode = 'bilingual' | 'en' | 'zh'

/** 精听时的语言显示 — 保留旧别名兼容 */
export type StudyMode = 'intensive' | 'blind' | 'cloze'

/** 挖空强度（仅 cloze） */
export type ClozeDensity = 'one' | 'many'

export type ClozeToken =
  | { kind: 'text'; text: string }
  | { kind: 'blank'; id: string; answer: string; display: string }

export interface VideoOverview {
  totalCount: number
  learnedCount: number
  unlearnedCount: number
  favoritedCount: number
  historyCount: number
  tags: VideoTag[]
}

export type VideoListFilter = 'all' | 'learned' | 'unlearned' | 'favorited' | 'history'

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
  tags?: VideoTag[]
  sentences: VideoSentence[]
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
  tags?: VideoTag[]
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

