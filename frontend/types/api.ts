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
