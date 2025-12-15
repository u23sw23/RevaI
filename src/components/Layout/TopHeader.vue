<template>
  <header class="top-header">
    <div class="header-left">
      <input 
        type="text" 
        placeholder="search..." 
        class="search-input"
      />
    </div>
    <div class="header-center">
      <select class="repo-select">
        <option>所有库...</option>
      </select>
    </div>
    <div class="header-right">
      <div class="avatar" @click="goToProfile">头像</div>
    </div>
  </header>
</template>

<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()

const goToProfile = () => {
  router.push('/profile')
}
</script>

<style scoped>
.top-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 24px;
  background-color: #fff;
  border-bottom: 1px solid #e0e0e0;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
}

.header-left {
  flex: 1;
  max-width: 400px;
}

.search-input {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
}

.header-center {
  flex: 1;
  display: flex;
  justify-content: center;
}

.repo-select {
  padding: 8px 16px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
  background-color: #fff;
  cursor: pointer;
}

.header-right {
  flex: 1;
  display: flex;
  justify-content: flex-end;
}

.avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background-color: #e0e0e0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s;
}

.avatar:hover {
  background-color: #1976d2;
  color: white;
}
</style>
