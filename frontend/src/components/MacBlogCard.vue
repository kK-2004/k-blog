<template>
  <div class="mb-8 group transition-all duration-300">
    <!-- 卡片容器 -->
    <div class="bg-white/80 dark:bg-[#1e1e1e]/80 backdrop-blur-xl rounded-xl shadow-lg border border-white/20 dark:border-white/10 overflow-hidden hover:shadow-2xl transition-all">
      
      <!-- 标题栏 (Traffic Lights) -->
      <div class="h-8 bg-gradient-to-b from-gray-100 to-gray-200 dark:from-[#3a3a3a] dark:to-[#2b2b2b] border-b border-gray-300 dark:border-black flex items-center px-4 justify-between">
        <div class="flex gap-2">
          <div class="w-3 h-3 rounded-full bg-[#FF5F56] shadow-sm border border-black/10"></div>
          <div class="w-3 h-3 rounded-full bg-[#FFBD2E] shadow-sm border border-black/10"></div>
          <div class="w-3 h-3 rounded-full bg-[#27C93F] shadow-sm border border-black/10"></div>
        </div>
        <div class="text-[11px] font-semibold text-gray-500 dark:text-gray-400 flex items-center gap-1 opacity-80 font-sans tracking-wide">
          <i class="ph ph-terminal-window text-lg"></i>
          <span>{{ post.title }}</span>
        </div>
        <div class="w-10"></div>
      </div>

      <!-- 内容区 -->
      <div class="p-6 relative">
        <!-- 用户信息 -->
        <div class="flex items-center gap-3 mb-4">
          <img :src="post.avatar" alt="avatar" class="w-11 h-11 rounded-full shadow-md border-2 border-white dark:border-gray-600" />
          <div>
            <div class="text-sm font-bold text-gray-800 dark:text-gray-100">{{ post.author }}</div>
            <div class="text-xs text-gray-400 font-mono">{{ post.time }}</div>
          </div>
        </div>

        <!-- 信息工具栏 (统计 + AI按钮) -->
        <div class="mt-4 mb-4">
          <div class="flex items-center justify-between bg-gray-50/80 dark:bg-white/5 rounded-lg p-2 px-3 border border-gray-100 dark:border-white/5 mb-3 backdrop-blur-sm">
            <!-- 左侧：统计信息 -->
            <div class="flex items-center gap-4 text-xs text-gray-500 dark:text-gray-400 font-mono">
              <div class="flex items-center gap-1.5" title="预估字数">
                <i class="ph ph-text-t text-base text-gray-400"></i>
                <span>{{ readStats.count }}</span>
              </div>
              <div class="flex items-center gap-1.5" title="预估阅读时间">
                <i class="ph ph-hourglass text-base text-gray-400"></i>
                <span>{{ readStats.time }}</span>
              </div>
            </div>

            <!-- 右侧：AI 摘要按钮 -->
            <button
                v-if="!summary && !isGenerating"
                @click="generateAiSummary"
                class="text-xs flex items-center gap-1.5 px-2.5 py-1 rounded-md bg-white dark:bg-white/10 border border-gray-200 dark:border-white/10 text-gray-600 dark:text-gray-300 hover:text-blue-600 dark:hover:text-blue-400 hover:border-blue-200 dark:hover:border-blue-500/50 transition-all shadow-sm active:scale-95 group/btn"
            >
              <i class="ph ph-sparkle text-purple-500 group-hover/btn:scale-110 transition-transform"></i>
              <span>AI 摘要</span>
            </button>
          </div>

          <!-- AI 摘要内容 (生成中/成功/失败) -->
          <div v-if="isGenerating || summary || summaryError" class="relative group/ai animate-[fadeIn_0.3s_ease-out]">
            <div class="absolute -inset-0.5 bg-gradient-to-r from-pink-500 via-purple-500 to-blue-500 rounded-lg opacity-20 blur group-hover/ai:opacity-30 transition duration-1000"></div>
            <div class="relative bg-gray-50/90 dark:bg-[#252527]/90 backdrop-blur-md p-3 rounded-lg border border-white/50 dark:border-white/10 text-xs text-gray-600 dark:text-gray-300 leading-relaxed shadow-sm">
              <div v-if="isGenerating" class="flex items-center gap-2 text-gray-500">
                <i class="ph ph-circle-notch animate-spin text-blue-500"></i>
                <span class="animate-pulse">AI 正在分析文章内容...</span>
              </div>
              <div v-else-if="summaryError" class="flex gap-2 text-red-500">
                <i class="ph ph-warning-circle shrink-0 mt-0.5"></i>
                <div>{{ summaryError }}</div>
              </div>
              <div v-else class="flex gap-2">
                <i class="ph ph-sparkle-fill text-purple-500 shrink-0 mt-0.5"></i>
                <div>{{ summary }}</div>
              </div>
            </div>
          </div>
        </div>

        <!-- 正文 -->
        <div class="relative">
          <div 
            ref="contentRef"
            class="text-[#1d1d1f] dark:text-[#f5f5f7] text-[15px] leading-relaxed font-sans whitespace-pre-wrap tracking-wide transition-all duration-300"
            :class="{ 'overflow-hidden': !isExpanded }"
            :style="{ maxHeight: (!isExpanded && showExpandBtn) ? MAX_HEIGHT + 'px' : 'none' }"
          >
            {{ post.content }}
          </div>
          
          <!-- 遮罩 -->
          <div v-if="!isExpanded && showExpandBtn" class="absolute bottom-0 left-0 w-full h-16 bg-gradient-to-t from-white/90 dark:from-[#1e1e1e]/90 to-transparent pointer-events-none"></div>
        </div>

        <!-- 展开按钮 -->
        <div v-if="showExpandBtn" class="mt-4 flex justify-center">
          <button 
            @click="isExpanded = !isExpanded"
            class="px-4 py-1 rounded-md text-xs font-medium bg-gray-100 dark:bg-gray-700 text-gray-600 dark:text-gray-200 border border-gray-200 dark:border-gray-600 hover:bg-gray-200 dark:hover:bg-gray-600 transition-all shadow-sm active:scale-95"
          >
            {{ isExpanded ? '收起全文' : '展开阅读' }}
          </button>
        </div>

        <!-- 评论区域 (包含热门评论和新评论) -->
        <div class="mt-5 transition-all duration-500 ease-in-out" :class="{ 'hidden': !isExpanded && showExpandBtn, 'block': isExpanded || !showExpandBtn }">
          <!-- 评论列表 -->
          <div class="space-y-3 mb-4">
            <div v-for="(comment, index) in comments" :key="index" class="bg-[#1c1c1e] dark:bg-black/40 rounded-lg p-3.5 font-mono text-[13px] relative overflow-hidden shadow-inner border border-white/10 animate-[fadeIn_0.3s_ease-out]">
              <div class="flex items-start gap-2">
                <span v-if="comment.isHot" class="text-[#32d74b] select-none" title="热门评论">➜</span>
                <span v-else class="text-gray-500 select-none">#</span>
                <!-- 评论头像：昵称首字符 + 纯色背景 (macOS 风格) -->
                <div
                  class="w-7 h-7 rounded-full flex items-center justify-center flex-shrink-0 text-[12px] font-bold text-white shadow-sm border border-white/10 select-none mt-0.5"
                  :style="{ backgroundColor: getAvatarColor(comment.user) }"
                  :title="comment.user"
                >
                  {{ getInitial(comment.user) }}
                </div>

                <div class="text-gray-300 min-w-0">
                  <span :class="comment.isHot ? 'text-[#0a84ff]' : 'text-gray-400'" class="font-bold">@{{ comment.isHot ? 'hot_comment' : 'comment' }}</span>: {{ comment.text }}
                  <div class="mt-1 text-[11px] text-gray-500 flex items-center gap-2">
                    <span class="truncate">// user: {{ comment.user }}</span>
                    <span v-if="comment.isHot" class="bg-red-500/20 text-red-400 px-1 rounded text-[9px]">HOT</span>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- 评论输入框 (锚点) - MacOS iMessage 风格 -->
          <div ref="commentInputRef" class="mt-6 pt-0">
            <div class="flex items-end gap-2 bg-[#f2f2f7] dark:bg-[#1c1c1e] p-2 rounded-[24px] border border-gray-100 dark:border-white/5 transition-colors">
              
              <!-- 动态头像预览：昵称首字符 + 纯色背景 (macOS 风格) -->
              <div
                class="w-8 h-8 rounded-full flex-shrink-0 mb-1 ml-1 overflow-hidden shadow-sm border border-white dark:border-white/10 flex items-center justify-center text-[13px] font-bold text-white select-none"
                :style="{ backgroundColor: getAvatarColor(draftUser || '匿名用户') }"
                :title="draftUser || '匿名用户'"
              >
                {{ getInitial(draftUser) }}
              </div>

              <div class="flex-1 flex flex-col min-w-0 relative">
                <!-- 昵称输入 (Subtle) -->
                <input 
                  v-model="draftUser" 
                  placeholder="Nickname (Optional)" 
                  class="bg-transparent text-[10px] text-gray-500 dark:text-gray-400 px-3 pt-0 pb-1 outline-none font-medium tracking-wide placeholder-gray-400/70"
                />
                
                <!-- 文本输入 (iMessage style) -->
                <textarea 
                  v-model="draftText" 
                  placeholder="iMessage" 
                  class="w-full bg-white dark:bg-[#2c2c2e] border border-black/5 dark:border-black/20 rounded-[18px] px-4 py-2 text-[14px] focus:outline-none focus:ring-2 focus:ring-[#007AFF]/20 transition-all resize-none h-9 focus:h-24 leading-5 text-gray-800 dark:text-gray-100 placeholder-gray-400 no-scrollbar shadow-sm"
                  style="min-height: 36px;"
                ></textarea>
              </div>

              <!-- 发送按钮 (Blue Circle Arrow) -->
              <button 
                @click="submitComment" 
                :disabled="!draftText.trim()"
                class="mb-1 mr-1 w-8 h-8 rounded-full bg-[#007AFF] hover:bg-[#0062cc] disabled:bg-gray-300 dark:disabled:bg-gray-600 disabled:cursor-not-allowed text-white flex items-center justify-center transition-all shadow-md active:scale-95 group"
              >
                <i class="ph ph-arrow-up font-bold text-base group-hover:-translate-y-0.5 transition-transform"></i>
              </button>
            </div>
            <div class="text-center mt-2">
               <p class="text-[10px] text-gray-400 dark:text-gray-600 font-sans tracking-tight">Delivered securely</p>
            </div>
          </div>
        </div>
      </div>

      <!-- 底部 Action Bar -->
      <div class="bg-gray-50/50 dark:bg-white/5 px-6 py-3 border-t border-gray-100 dark:border-white/10 flex justify-between items-center backdrop-blur-sm">
        <div class="flex gap-4">
          <button @click="handleLike" class="flex items-center gap-1.5 text-gray-400 hover:text-red-500 transition-colors group">
            <i class="ph ph-heart text-lg group-hover:scale-110 transition-transform" :class="{'text-red-500 fill-current': localLikes > post.likes}"></i>
            <span class="text-xs font-medium">{{ localLikes }}</span>
          </button>
          <button @click="handleCommentClick" class="flex items-center gap-1.5 text-gray-400 hover:text-blue-500 transition-colors group">
            <i class="ph ph-chat-circle text-lg group-hover:scale-110 transition-transform"></i>
            <span class="text-xs font-medium">评论</span>
          </button>
        </div>
        <button class="text-gray-400 hover:text-gray-700 dark:hover:text-gray-300 transition-colors">
          <i class="ph ph-share-network text-lg"></i>
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick, watch, computed } from 'vue';
import { generatePostSummary } from '../api/aiSummary';

const props = defineProps({
  post: {
    type: Object,
    required: true
  }
});

const isExpanded = ref(false);
const showExpandBtn = ref(false);
const contentRef = ref(null);
const commentInputRef = ref(null); // 用于滚动的锚点
const MAX_HEIGHT = 130;

// 互动状态
const localLikes = ref(props.post.likes);
const comments = ref([]);
const draftUser = ref('');
const draftText = ref('');
const summary = ref('');
const summaryError = ref('');
const isGenerating = ref(false);

const MACOS_SOLID_COLORS = [
  '#0A84FF', // blue
  '#30D158', // green
  '#FF9F0A', // orange
  '#FF453A', // red
  '#BF5AF2', // purple
  '#64D2FF', // cyan
  '#FFD60A', // yellow
  '#AC8E68', // brown
];

const hashString = (value) => {
  let hash = 0;
  for (const ch of String(value)) {
    const cp = ch.codePointAt(0) ?? 0;
    hash = ((hash << 5) - hash + cp) | 0;
  }
  return Math.abs(hash);
};

const getAvatarColor = (name) => {
  const seed = String(name || '匿名用户').trim() || '匿名用户';
  return MACOS_SOLID_COLORS[hashString(seed) % MACOS_SOLID_COLORS.length];
};

const getInitial = (name) => {
  const trimmed = String(name || '').trim();
  if (!trimmed) return '匿';
  const first = Array.from(trimmed)[0] || '匿';
  return first.toUpperCase();
};

const readStats = computed(() => {
  const text = String(props.post?.content || '');
  const count = text.replace(/\s/g, '').length;
  const minutes = Math.max(1, Math.ceil(count / 300));
  return { count, time: `${minutes} min` };
});

const generateAiSummary = async () => {
  if (isGenerating.value) return;
  summaryError.value = '';
  isGenerating.value = true;

  try {
    summary.value = await generatePostSummary({
      postId: props.post?.id,
      title: props.post?.title,
      content: props.post?.content
    });
  } catch (err) {
    summary.value = '';
    summaryError.value = err instanceof Error ? err.message : 'AI 摘要生成失败，请稍后重试';
  } finally {
    isGenerating.value = false;
  }
};

// 初始化评论列表
if (props.post.hotComment) {
  comments.value.push({ ...props.post.hotComment, isHot: true });
}

// 核心逻辑：检测高度
const checkHeight = () => {
  if (contentRef.value) {
    if (contentRef.value.scrollHeight > MAX_HEIGHT) {
      showExpandBtn.value = true;
    } else {
      showExpandBtn.value = false;
      isExpanded.value = true;
    }
  }
};

// 处理点赞 (无需登录)
const handleLike = () => {
  localLikes.value++;
};

// 处理评论按钮点击：展开 + 滚动
const handleCommentClick = () => {
  // 1. 如果是折叠状态，先展开
  if (!isExpanded.value) {
    isExpanded.value = true;
  }
  
  // 2. 等待 DOM 更新（展开动画或渲染）后，滚动到评论区
  nextTick(() => {
    if (commentInputRef.value) {
      commentInputRef.value.scrollIntoView({ behavior: 'smooth', block: 'center' });
      // 可选：聚焦输入框
      // commentInputRef.value.querySelector('textarea')?.focus(); 
    }
  });
};

// 提交评论
const submitComment = () => {
  if (!draftText.value.trim()) return;

  // 添加新评论到列表头部
  comments.value.push({
    user: draftUser.value.trim() || '匿名用户', // 默认为匿名用户
    text: draftText.value,
    isHot: false
  });

  // 清空输入
  draftText.value = '';
  // draftUser.value = ''; // 昵称可以保留方便继续评论
};

onMounted(() => {
  nextTick(() => {
    checkHeight();
  });
});

watch(() => props.post.content, () => {
  nextTick(checkHeight);
});
</script>
