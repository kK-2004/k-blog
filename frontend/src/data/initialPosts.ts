export type HotComment = {
  id?: number
  user: string
  text: string
  likes?: number
  replies?: { id: number; user: string; text: string; likes?: number }[]
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

export const INITIAL_POSTS: Post[] = [
  {
    id: 1,
    author: 'GeekWang',
    title: 'Vue3_Composition_API.js',
    time: '2小时前',
    content: `# Vue 3 体验

今天把React项目重构成了Vue 3，\`Composition API\` 真的太香了！

\`\`\`javascript
const app = createApp({
  setup() {
    return { count: ref(0) }
  }
})
\`\`\`

逻辑复用比Mixins好用一万倍。组合式 API 让我们可以更加优雅地组织代码，特别是在处理复杂逻辑时。相比于 Vue 2 的 Options API，这种方式让相关联的逻辑（如数据获取、筛选、分页）能够聚合在一起，而不是分散在 data、methods 和 mounted 中。
今天把React项目重构成了Vue 3，Composition API 真的太香了！

const app = createApp({
  setup() {
    return { count: ref(0) }
  }
})
逻辑复用比Mixins好用一万倍。组合式 API 让我们可以更加优雅地组织代码，特别是在处理复杂逻辑时。相比于 Vue 2 的 Options API，这种方式让相关联的逻辑（如数据获取、筛选、分页）能够聚合在一起，而不是分散在 data、methods 和 mounted 中。
今天把React项目重构成了Vue 3，Composition API 真的太香了！

const app = createApp({
  setup() {
    return { count: ref(0) }
  }
})
逻辑复用比Mixins好用一万倍。组合式 API 让我们可以更加优雅地组织代码，特别是在处理复杂逻辑时。相比于 Vue 2 的 Options API，这种方式让相关联的逻辑（如数据获取、筛选、分页）能够聚合在一起，而不是分散在 data、methods 和 mounted 中。
`,
    hotComment: null,
    views: 1204,
    likes: 85,
    comments: 12,
    pinned: false,
  },
  {
    id: 2,
    author: 'DesignerAka',
    title: 'MacOS_Design.fig',
    time: '4小时前',
    content: `### 拟物化回归

最近MacOS的设计语言在扁平中加入了很多光影细节，比如这个窗口的毛玻璃效果(Backdrop Blur)。

> 设计不是为了好看，而是为了好用。

前端实现主要靠 \`backdrop-filter: blur(20px)\`。这种设计风格被称为“新拟态”或“玻璃拟态”，它通过半透明的材质和微妙的阴影来构建层级感，让界面看起来更加通透和现代。`,
    hotComment: {
      user: 'CSSMaster',
      text: '性能是个大问题，特别是列表滚动时。',
    },
    views: 892,
    likes: 124,
    comments: 8,
    pinned: false,
  },
  {
    id: 3,
    author: 'DevOps',
    title: 'Docker_K8s.sh',
    time: '昨天',
    content: `### 容器化部署

Docker 已经成为现代开发的标配。

1. **一致性**: 环境一致，拒绝“在我机器上能跑”。
2. **隔离性**: 资源隔离，安全可靠。

\`\`\`bash
docker run -d -p 80:80 nginx
\`\`\`

随着微服务架构的普及，Kubernetes (K8s) 作为容器编排的事实标准，更是运维人员必须掌握的技能。它不仅能管理容器的生命周期，还能处理自动扩缩容、负载均衡等复杂场景。`,
    hotComment: {
      user: 'OpsGuy',
      text: 'K8s 才是终极目标！',
    },
    views: 562,
    likes: 42,
    comments: 5,
    pinned: false,
  },
]
