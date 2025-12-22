<template>
  <div class="layout">
    <!-- 顶部：搜索 / 仓库选择 / 头像 / 登录注册 -->
    <TopHeader
      @search="handleHeaderSearch"
      @auth-success="handleAuthSuccess"
    />

    <div class="layout-body">
      <!-- 左侧：导航栏 -->
      <Sidebar />

      <!-- 右侧：根据路由展示不同页面，并把搜索关键词传给页面 -->
      <main class="main-content">
        <router-view v-slot="{ Component}">
          <component
            :is="Component"
            :search-keyword="searchKeyword"
            :user="currentUser"
          />
        </router-view>
      </main>
    </div>

    <!-- 全局学习小精灵（固定悬浮在页面上） -->
    <AiSprite
      :user-name="currentUser?.name || ''"
      :last-login-at="currentUser?.lastLoginAt || ''"
      :today-study-minutes="0"
      :today-wrong-count="0"
    />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import TopHeader from './TopHeader.vue'
import Sidebar from './Sidebar.vue'
import AiSprite from '../../views/AiSprite.vue'

// 从 TopHeader 接收到的搜索关键词
const searchKeyword = ref('')

// 当前登录用户信息（从登录/注册成功或本地存储还原）
const currentUser = ref(null)

// 头部搜索回调：更新当前搜索关键词
const handleHeaderSearch = (keyword) => {
  searchKeyword.value = keyword || ''
}

// 顶部登录/注册成功后回调：更新当前用户信息
const handleAuthSuccess = (user) => {
  currentUser.value = user
}

// 初始化：尝试从本地存储还原用户信息
onMounted(() => {
  const savedUser = localStorage.getItem('auth_user')
  if (savedUser) {
    try {
      currentUser.value = JSON.parse(savedUser)
    } catch (e) {
      currentUser.value = null
    }
  }
})
</script>

<style scoped>
.layout {
  display: flex;
  flex-direction: column;
  height: 100vh;
  width: 100%;
}

.layout-body {
  display: flex;
  flex: 1;
  overflow: hidden;
}

.main-content {
  flex: 1;
  padding: 24px;
  overflow-y: auto;
  background-color: #f5f5f5;
}
</style>