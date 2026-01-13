<script setup lang="ts">
import { ref } from 'vue'
import ProfileSidebar from '@/components/mac/ProfileSidebar.vue'
import ProfileOverview from '@/components/mac/ProfileOverview.vue'
import ProfileEditor from '@/components/mac/ProfileEditor.vue'

const currentTab = ref('overview')

const handleTabChange = (tab: string) => {
  currentTab.value = tab
}

const handleQuickAction = (action: string) => {
  currentTab.value = action
}
</script>

<template>
  <div class="flex gap-6">
    <!-- 侧边栏 -->
    <div class="w-56 flex-shrink-0">
      <ProfileSidebar :currentTab="currentTab" @tab-change="handleTabChange" />
    </div>

    <!-- 内容区域 -->
    <div class="flex-1 min-w-0">
      <!-- 概览页 -->
      <ProfileOverview
        v-if="currentTab === 'overview'"
        @edit-avatar="handleQuickAction('avatar')"
        @edit-basic="handleQuickAction('basic')"
        @edit-contact="handleQuickAction('contact')"
        @edit-social="handleQuickAction('social')"
      />

      <!-- 编辑页 -->
      <ProfileEditor v-else :currentTab="currentTab" />
    </div>
  </div>
</template>
