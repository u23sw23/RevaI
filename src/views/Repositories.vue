<template>
  <div class="repositories-page">
    <h2 class="page-title">create new repository+...</h2>
    
    <div class="existing-repos">
      <div class="repo-card" v-for="repo in repositories" :key="repo.id" @click="goToRepository(repo.id)">
        {{ repo.name }}
      </div>
    </div>
    
    <div class="create-section">
      <div class="create-header">
        <input 
          type="text" 
          placeholder="Ask AI to create a responsity......" 
          class="create-input"
        />
        <button class="look-btn">LOOK</button>
      </div>
      <p class="create-hint">在这里传文件给AI, AI根据内容生成库</p>
      <button class="add-file-btn">add file ...</button>
      <input 
        type="text" 
        placeholder="res nam..." 
        class="repo-name-input"
      />
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

const repositories = ref([
  { id: 'res1', name: 'res1' },
  { id: 'res2', name: 'res2' },
  { id: 'res3', name: 'res3' }
])

const goToRepository = (id) => {
  router.push(`/repositories/${id}`)
}
</script>

<style scoped>
.repositories-page {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.page-title {
  font-size: 20px;
  font-weight: 600;
  color: #333;
  margin-bottom: 8px;
}

.existing-repos {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 16px;
}

.repo-card {
  background-color: #fff;
  padding: 24px;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
  cursor: pointer;
  text-align: center;
  font-size: 16px;
  transition: all 0.2s;
  border: 2px solid transparent;
}

.repo-card:hover {
  border-color: #1976d2;
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
}

.create-section {
  background-color: #fff;
  padding: 24px;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
}

.create-header {
  display: flex;
  gap: 12px;
  margin-bottom: 8px;
}

.create-input {
  flex: 1;
  padding: 12px 16px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
}

.look-btn {
  padding: 12px 24px;
  background-color: #ff4081;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
}

.look-btn:hover {
  background-color: #e91e63;
}

.create-hint {
  font-size: 12px;
  color: #666;
  margin-bottom: 12px;
}

.add-file-btn {
  padding: 8px 16px;
  background-color: #f5f5f5;
  border: 1px solid #ddd;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  margin-right: 12px;
}

.add-file-btn:hover {
  background-color: #e0e0e0;
}

.repo-name-input {
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
  width: 200px;
}
</style>
