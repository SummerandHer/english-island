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
