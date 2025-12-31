<template>
  <div class="layout">
    
    <TopHeader
      @search="handleHeaderSearch"
      @auth-success="handleAuthSuccess"
    />

    <div class="layout-body">
      
      <Sidebar />

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

const searchKeyword = ref('')

const currentUser = ref(null)

const handleHeaderSearch = (keyword) => {
  searchKeyword.value = keyword || ''
}

const handleAuthSuccess = (user) => {
  currentUser.value = user
}

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
  background: linear-gradient(to bottom, #F8FBFF 0%, #FFFFFF 100%);
  min-height: 100%;
}
</style>