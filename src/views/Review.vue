<template>
  <div class="review-page">
    <div class="review-grid">
      <div class="review-box learn-box">
        <h3 class="box-title">learn</h3>
        <p class="box-desc">推送未学习过的卡片</p>
      </div>
      
      <div class="review-box review-box-active">
        <h3 class="box-title">review</h3>
        <p class="box-desc">推送学习过的卡片, 遵循遗忘曲线</p>
      </div>
    </div>
  </div>
</template>

<script setup>
</script>

<style scoped>
.review-page {
  width: 100%;
}

.review-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
}

.review-box {
  background-color: #fff;
  padding: 48px;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
  min-height: 400px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  border: 2px solid transparent;
  transition: all 0.2s;
}

.review-box:hover {
  border-color: #1976d2;
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
}

.review-box-active {
  border-color: #1976d2;
  background-color: #e3f2fd;
}

.box-title {
  font-size: 24px;
  font-weight: 600;
  color: #333;
  margin-bottom: 16px;
}

.box-desc {
  font-size: 14px;
  color: #666;
  line-height: 1.6;
}
</style>

