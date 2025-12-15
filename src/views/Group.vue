<template>
  <div class="group-page">
    <div class="groups-display">
      <p class="groups-desc">这里展示该用户所属的所有groups 可输入邀请码加入某个小组</p>
    </div>
    
    <div class="group-actions">
      <button class="action-btn add-group-btn">add group...</button>
      <button class="action-btn join-group-btn">join group...</button>
    </div>
  </div>
</template>

<script setup>
</script>

<style scoped>
.group-page {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.groups-display {
  background-color: #fff;
  padding: 48px;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
  min-height: 400px;
  display: flex;
  align-items: center;
  justify-content: center;
  text-align: center;
}

.groups-desc {
  font-size: 14px;
  color: #666;
  line-height: 1.6;
}

.group-actions {
  display: flex;
  gap: 16px;
}

.action-btn {
  flex: 1;
  padding: 12px 24px;
  border: 1px solid #ddd;
  border-radius: 4px;
  background-color: #fff;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.2s;
}

.action-btn:hover {
  background-color: #f5f5f5;
  border-color: #1976d2;
  color: #1976d2;
}

.add-group-btn {
  background-color: #1976d2;
  color: white;
  border-color: #1976d2;
}

.add-group-btn:hover {
  background-color: #1565c0;
  color: white;
}
</style>
