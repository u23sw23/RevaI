<template>
  <div class="home-page">
    <div class="chat-section">
      <div class="chat-header">
        <input 
          type="text" 
          placeholder="Ask anything......(普通AI聊天)" 
          class="chat-input"
        />
        <button class="look-btn">LOOK</button>
      </div>
      <button class="add-file-btn">add file +</button>
    </div>
    
    <div class="hot-repos-section">
      <h3>热门库展示在此处 (状态为public)</h3>
      <p class="hot-repos-text">some hot repositories show here......</p>
    </div>
  </div>
</template>

<script setup>
</script>

<style scoped>
.home-page {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.chat-section {
  background-color: #fff;
  padding: 24px;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
}

.chat-header {
  display: flex;
  gap: 12px;
  margin-bottom: 12px;
}

.chat-input {
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

.hot-repos-section {
  background-color: #fff;
  padding: 24px;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
  min-height: 400px;
}

.hot-repos-section h3 {
  margin-bottom: 16px;
  color: #333;
  font-size: 16px;
  font-weight: 500;
}

.hot-repos-text {
  color: #999;
  font-size: 14px;
}
</style>

