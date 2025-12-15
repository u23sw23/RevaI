<template>
  <div class="repository-detail-page">
    <h2 class="page-title">repository name: {{ repositoryName }}...</h2>
    
    <div class="repository-content">
      <div class="cards-section">
        <div class="cards-grid">
          <div class="card-item" v-for="card in cards" :key="card.id">
            {{ card.name }}
          </div>
        </div>
      </div>
      
      <div class="edit-section">
        <textarea 
          class="edit-textarea"
          placeholder="修改库属性如: 名称/可见性/标签"
        ></textarea>
      </div>
    </div>
    
    <div class="update-section">
      <div class="update-header">
        <input 
          type="text" 
          placeholder="Ask AI to update a responsity......" 
          class="update-input"
        />
        <button class="look-btn">LOOK</button>
      </div>
      <p class="update-hint">在这里传文件给AI, AI根据内容在该库下添加卡片</p>
      <button class="add-file-btn">add file ...</button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'

const props = defineProps({
  id: {
    type: String,
    required: true
  }
})

const repositoryName = computed(() => props.id || 'res1')

const cards = ref([
  { id: 1, name: 'card1' },
  { id: 2, name: 'card2' },
  { id: 3, name: 'card3' },
  { id: 4, name: 'card4' },
  { id: 5, name: 'card5' },
  { id: 6, name: 'card6' }
])
</script>

<style scoped>
.repository-detail-page {
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

.repository-content {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 24px;
}

.cards-section {
  background-color: #fff;
  padding: 24px;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
}

.cards-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.card-item {
  background-color: #f5f5f5;
  padding: 24px;
  border-radius: 8px;
  text-align: center;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
  border: 2px solid transparent;
}

.card-item:hover {
  border-color: #1976d2;
  background-color: #e3f2fd;
}

.edit-section {
  background-color: #fff;
  padding: 24px;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
}

.edit-textarea {
  width: 100%;
  min-height: 200px;
  padding: 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
  resize: vertical;
  font-family: inherit;
}

.update-section {
  background-color: #fff;
  padding: 24px;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
}

.update-header {
  display: flex;
  gap: 12px;
  margin-bottom: 8px;
}

.update-input {
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

.update-hint {
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
}

.add-file-btn:hover {
  background-color: #e0e0e0;
}
</style>
