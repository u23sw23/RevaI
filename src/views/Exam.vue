<template>
  <div class="exam-page">
    <div class="exam-header">
      <div class="exam-info">
        <span class="question-count">共{{ totalQuestions }}题 / 第一题</span>
      </div>
      <div class="score-box">
        分数
      </div>
    </div>
    
    <div class="question-section">
      <div class="question-box">
        <h3>当前题目</h3>
      </div>
    </div>
    
    <div class="answer-section">
      <div class="answer-box">
        <h3>作答区域</h3>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const totalQuestions = ref(10)
</script>

<style scoped>
.exam-page {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.exam-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background-color: #fff;
  padding: 16px 24px;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
}

.exam-info {
  font-size: 16px;
  color: #333;
}

.question-count {
  font-weight: 500;
}

.score-box {
  background-color: #f5f5f5;
  padding: 8px 16px;
  border-radius: 4px;
  font-size: 14px;
  font-weight: 500;
}

.question-section {
  background-color: #fff;
  padding: 24px;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
}

.question-box {
  min-height: 200px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 2px dashed #ddd;
  border-radius: 4px;
}

.question-box h3 {
  font-size: 18px;
  color: #666;
  font-weight: 500;
}

.answer-section {
  background-color: #fff;
  padding: 24px;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
}

.answer-box {
  min-height: 300px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 2px dashed #ddd;
  border-radius: 4px;
}

.answer-box h3 {
  font-size: 18px;
  color: #666;
  font-weight: 500;
}
</style>
