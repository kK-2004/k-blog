<template>
  <div class="bg-white/80 dark:bg-[#1e1e1e]/80 backdrop-blur-2xl rounded-xl shadow-2xl border border-white/20 dark:border-white/10 overflow-hidden min-h-[500px]">
    <div class="h-12 bg-gray-100/50 dark:bg-[#333]/50 border-b border-gray-200 dark:border-gray-700 flex items-center px-6 justify-between">
      <h2 class="text-sm font-semibold text-gray-700 dark:text-gray-200 flex items-center gap-2">
        <i class="ph ph-squares-four text-lg"></i> KK Blog Admin
      </h2>
      <div class="flex gap-2 items-center">
        <div class="w-2 h-2 rounded-full bg-green-500 animate-pulse"></div>
        <span class="text-[10px] text-gray-500 uppercase tracking-widest">System Ready</span>
      </div>
    </div>
    
    <div class="p-0 overflow-x-auto">
      <table class="w-full text-left border-collapse">
        <thead>
          <tr class="border-b border-gray-100 dark:border-white/10 text-xs text-gray-400 font-medium uppercase tracking-wider">
            <th class="p-4 pl-6">ID</th>
            <th class="p-4">文章标题</th>
            <th class="p-4 text-center">状态</th>
            <th class="p-4 text-right">数据</th>
            <th class="p-4 text-center pr-6">操作</th>
          </tr>
        </thead>
        <tbody class="divide-y divide-gray-50 dark:divide-white/5">
          <tr v-for="post in posts" :key="post.id" class="hover:bg-blue-50/50 dark:hover:bg-blue-900/10 transition-colors group">
            <td class="p-4 pl-6 text-gray-400 text-xs font-mono">#{{ String(post.id).padStart(3, '0') }}</td>
            <td class="p-4">
              <div class="font-medium text-gray-800 dark:text-gray-200 text-sm">{{ post.title }}</div>
            </td>
            <td class="p-4 text-center">
              <span class="inline-flex items-center px-2 py-0.5 rounded text-[10px] font-medium bg-green-100 text-green-800 dark:bg-green-900/40 dark:text-green-400 border border-green-200 dark:border-green-800">
                Published
              </span>
            </td>
            <td class="p-4 text-right">
              <div class="text-xs text-gray-500 dark:text-gray-400 font-mono">
                <div>{{ post.views }} 阅读</div>
              </div>
            </td>
            <td class="p-4 pr-6 text-center">
              <div class="flex items-center justify-center gap-2 opacity-0 group-hover:opacity-100 transition-opacity">
                <button class="p-1.5 text-blue-500 hover:bg-blue-100 rounded dark:hover:bg-blue-900/30"><i class="ph ph-pencil-simple"></i></button>
                <button class="p-1.5 text-red-500 hover:bg-red-100 rounded dark:hover:bg-red-900/30"><i class="ph ph-trash"></i></button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup>
defineProps({
  posts: {
    type: Array,
    required: true
  }
});
</script>