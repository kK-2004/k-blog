import { ref } from 'vue';
import kkAvatar from '../server/kk-avatar.png';

export const usePostsStore = () => {
  const posts = ref([
    {
      id: 1,
      author: "极客小王",
      title: "Vue3_Composition_API.js",
      avatar: kkAvatar,
      time: "2小时前",
      content: "今天把React项目重构成了Vue 3，Composition API (setup语法糖) 真的太香了！逻辑复用比Mixins好用一万倍，代码组织起来像写原生JS一样流畅。大家觉得 Vue 3 和 React Hooks 哪个更顺手？",
      hotComment: null,
      views: 1204,
      likes: 85
    },
    {
      id: 2,
      author: "UI设计师-阿卡",
      title: "MacOS_Design_System.fig",
      avatar: kkAvatar,
      time: "4小时前",
      content: "拟物化设计的回归？最近MacOS的设计语言在扁平中加入了很多光影细节，比如这个窗口的毛玻璃效果(Backdrop Blur)和阴影深度。前端实现这种效果主要靠 backdrop-filter: blur(20px) 和 box-shadow。分享一张我复刻的UI稿。",
      hotComment: {
        user: "前端切图仔",
        text: "CSS写这个磨砂玻璃效果最怕性能问题，特别是滚动的时候。"
      },
      views: 892,
      likes: 124
    },
    {
      id: 3,
      author: "架构师老李",
      title: "Frontend_Architecture.md",
      avatar: kkAvatar,
      time: "昨天",
      content: "谈谈前端工程化：\n\n随着项目复杂度增加，单一文件的组件已经无法满足需求。我们需要关注：\n1. 模块拆分：将UI组件、业务逻辑(Composables)、工具函数分离。\n2. 状态管理：小项目用 ref/reactive，大项目上 Pinia。\n3. 类型安全：TypeScript 是必选项，它能从编译阶段规避80%的低级错误。\n\nVue 3 对 TS 的支持比 Vue 2 有了质的飞跃。建议大家新项目直接上 TS + Vue 3 Setup。",
      hotComment: {
        user: "学习中的小白",
        text: "大佬，Pinia和Vuex的区别主要在哪？感觉Pinia写法更像Hooks。"
      }, 
      views: 2341,
      likes: 456
    }
  ]);

  const addPost = (post: any) => {
    posts.value.push(post);
  };

  const updatePost = (id: number, updatedPost: any) => {
    const index = posts.value.findIndex(post => post.id === id);
    if (index !== -1) {
      posts.value[index] = { ...posts.value[index], ...updatedPost };
    }
  };

  const deletePost = (id: number) => {
    const index = posts.value.findIndex(post => post.id === id);
    if (index !== -1) {
      posts.value.splice(index, 1);
    }
  };

  return {
    posts,
    addPost,
    updatePost,
    deletePost
  };
};
