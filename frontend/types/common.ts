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
