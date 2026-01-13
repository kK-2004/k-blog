export type HotCommentReply = {
  id: number
  user: string
  text: string
  likes: number
}

export type HotComment = {
  id?: number
  user: string
  text: string
  likes?: number
  replies?: HotCommentReply[]
}

export type Post = {
  id: number
  author: string
  title: string
  time: string
  content: string
  hotComment: HotComment | null
  views: number
  likes: number
  comments: number
  pinned: boolean
}

export type MenuItem = {
  id: string
  label: string
  icon: string
  requiresAuth: boolean
  visible: boolean
  order: number
}

export type AdminMe = {
  id: number
  username: string
  createdAt: string
  updatedAt: string
  lastLoginAt: string | null
  lastLoginIp: string | null
  lastLoginLocation: string | null
}

