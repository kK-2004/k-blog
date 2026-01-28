export type PostCommentReply = {
  id: number
  user: string
  text: string
  likes: number
  createdAt: number
  toUser: string | null
}

export type PostComment = {
  id: number
  user: string
  text: string
  likes: number
  createdAt: number
  hot: boolean
  replies: PostCommentReply[]
}

export type Post = {
  id: number
  author: string
  title: string
  createdAt: number | null
  updatedAt: number | null
  content: string
  views: number
  likes: number
  comments: number
  pinned: boolean
}

export type HotPost = {
  id: number
  title: string
  lastViewTime: number
}

export type MenuItem = {
  id: string
  label: string
  icon: string
  requiresAuth: boolean
  visible: boolean
  order: number
}

export type QuickAction = {
  id: string
  title: string
  description: string | null
  icon: string
  targetType: 'internal' | 'external'
  target: string
  visible: boolean
  order: number
}

export type AdminMe = {
  id: number
  username: string
  avatarUrl: string | null
  gender: string | null
  age: number | null
  email: string | null
  phone: string | null
  qq: string | null
  wechat: string | null
  github: string | null
  gitee: string | null
  visibility: {
    gender: 'public' | 'private'
    age: 'public' | 'private'
    email: 'public' | 'private'
    phone: 'public' | 'private'
    qq: 'public' | 'private'
    wechat: 'public' | 'private'
    github: 'public' | 'private'
    gitee: 'public' | 'private'
  } | null
  createdAt: string
  updatedAt: string
  lastLoginAt: string | null
  lastLoginIp: string | null
  lastLoginLocation: string | null
}

export type LastLoginInfo = {
  lastLoginAt: string | null
  lastLoginLocation: string | null
}
