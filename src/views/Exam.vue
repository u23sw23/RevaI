<template>
  <div class="exam-page">
    <!-- Exam list (similar to repositories cards) -->
    <div v-if="!selectedExam" class="paper-list">
      <h2 class="page-title">Select exam / note</h2>
      <p class="page-subtitle">Each note has one exam. Choose an exam to start answering.</p>

      <div class="papers-grid">
        <div
          v-for="paper in papers"
          :key="paper.id"
          class="paper-card"
          @click="selectPaper(paper)"
        >
          <div class="paper-title">{{ paper.title }}</div>
          <div class="paper-desc">{{ paper.desc }}</div>
          <div class="paper-meta">
            <span>Questions: {{ paper.total }}</span>
            <span>Last updated: {{ paper.updatedAt }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- Exam detail -->
    <div v-else class="exam-detail">
      <div class="exam-header">
        <div class="exam-info">
          <button class="back-btn" @click="backToList">Back to exam list</button>
          <div class="exam-title">{{ selectedExam.title }}</div>
          <div class="question-count">
            Total {{ totalQuestions }} questions / Question {{ currentIndex + 1 }}
          </div>
        </div>
        <div class="score-box">
          Current score: {{ score }} pts
        </div>
      </div>
      
      <div class="question-section">
        <div class="question-box">
          <div class="question-header">
            <div class="question-type">{{ currentQuestion.type }}</div>
            <div class="question-points">{{ currentQuestion.points }} pts</div>
          </div>
          <div class="question-body">
            {{ currentQuestion.title }}
          </div>
          <ul v-if="currentQuestion.options" class="options-list">
            <li
              v-for="option in currentQuestion.options"
              :key="option.value"
              :class="['option-item', { active: userAnswer === option.value }]"
              @click="selectAnswer(option.value)"
            >
              <span class="option-label">{{ option.label }}</span>
              <span class="option-text">{{ option.text }}</span>
            </li>
          </ul>
        </div>
      </div>
      
      <div class="answer-section">
        <div class="answer-box">
          <h3>Answer area</h3>
          <textarea
            v-if="currentQuestion.type === 'open question'"
            v-model="subjectiveAnswer"
            class="answer-textarea"
            placeholder="Type your answer here..."
          />
          <div class="answer-actions">
            <button class="nav-btn" :disabled="currentIndex === 0" @click="prevQuestion">Previous</button>
            <button class="nav-btn" :disabled="currentIndex === totalQuestions - 1" @click="nextQuestion">Next</button>
            <button class="submit-btn" @click="submitExam">Submit exam</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { subjects } from '../data/notes'

// Build exam list dynamically from note data
const papers = computed(() =>
  subjects.flatMap(subject =>
    subject.notes.map(note => ({
      id: note.id,
      title: note.title,
      desc: subject.name,
      total: 5, // default number of questions per exam (can be adjusted)
      updatedAt: note.updatedAt,
      content: note.content
    }))
  )
)

const selectedExam = ref(null)
const currentIndex = ref(0)
const score = ref(0)
const userAnswer = ref(null)
const subjectiveAnswer = ref('')

// Questions generated from selected note content
const questions = ref([])

const totalQuestions = computed(() => questions.value.length)
const currentQuestion = computed(() => questions.value[currentIndex.value] || {})

const selectPaper = (paper) => {
  selectedExam.value = paper
  generateQuestionsFromContent(paper.content)
  currentIndex.value = 0
  userAnswer.value = null
  subjectiveAnswer.value = ''
}

const backToList = () => {
  selectedExam.value = null
  currentIndex.value = 0
  userAnswer.value = null
  subjectiveAnswer.value = ''
}

const selectAnswer = (val) => {
  if (currentQuestion.value.type === 'single choice') {
    userAnswer.value = val
  }
}

const nextQuestion = () => {
  if (currentIndex.value < totalQuestions.value - 1) {
    currentIndex.value += 1
    userAnswer.value = null
    subjectiveAnswer.value = ''
  }
}

const prevQuestion = () => {
  if (currentIndex.value > 0) {
    currentIndex.value -= 1
    userAnswer.value = null
    subjectiveAnswer.value = ''
  }
}

const submitExam = () => {
  // TODO: submit answers to backend and calculate score
  alert('Exam submitted!')
}

// Very simple generator that creates questions based on note content.
// In a real app this would call your backend / AI service.
const generateQuestionsFromContent = (content) => {
  if (!content) {
    questions.value = []
    return
  }

  const lines = content
    .split(/\n+/)
    .map(l => l.trim())
    .filter(Boolean)

  const openQuestions = lines.slice(0, 3).map((line, idx) => ({
    id: idx + 2,
    title: `Explain the following key point: ${line}`,
    type: 'open question',
    points: 5
  }))

  questions.value = [
    {
      id: 1,
      title: 'Summarize the main ideas of this note in your own words.',
      type: 'open question',
      points: 10
    },
    ...openQuestions
  ]
}
</script>

<style scoped>
.exam-page {
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

.page-subtitle {
  color: #666;
  margin-bottom: 16px;
  font-size: 14px;
}

.papers-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 16px;
}

.paper-card {
  background-color: #fff;
  padding: 20px;
  border-radius: 12px;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.06);
  cursor: pointer;
  transition: all 0.2s;
  border: 2px solid transparent;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.paper-card:hover {
  border-color: #1976d2;
  box-shadow: 0 6px 12px rgba(0, 0, 0, 0.08);
}

.paper-title {
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

.paper-desc {
  font-size: 14px;
  color: #666;
}

.paper-meta {
  font-size: 12px;
  color: #999;
  display: flex;
  justify-content: space-between;
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
  display: flex;
  flex-direction: column;
  gap: 6px;
  font-size: 16px;
  color: #333;
}

.question-count {
  font-weight: 500;
}

.exam-title {
  font-size: 18px;
  font-weight: 600;
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
  flex-direction: column;
  gap: 12px;
  border: 2px dashed #ddd;
  border-radius: 4px;
  padding: 16px;
}

.question-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 14px;
  color: #666;
}

.question-type {
  background-color: #e3f2fd;
  color: #1976d2;
  padding: 4px 8px;
  border-radius: 6px;
  font-weight: 600;
}

.question-points {
  color: #999;
}

.question-body {
  font-size: 16px;
  color: #333;
}

.options-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.option-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border: 1px solid #ddd;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}

.option-item:hover {
  border-color: #1976d2;
  box-shadow: 0 2px 6px rgba(25, 118, 210, 0.12);
}

.option-item.active {
  border-color: #1976d2;
  background-color: #e3f2fd;
}

.option-label {
  font-weight: 700;
  color: #1976d2;
}

.option-text {
  color: #333;
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
  flex-direction: column;
  gap: 16px;
  border: 2px dashed #ddd;
  border-radius: 4px;
  padding: 16px;
}

.answer-box h3 {
  font-size: 18px;
  color: #666;
  font-weight: 500;
}

.answer-textarea {
  width: 100%;
  min-height: 200px;
  padding: 12px;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-size: 14px;
  font-family: inherit;
  resize: vertical;
}

.answer-actions {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
}

.nav-btn,
.submit-btn,
.back-btn {
  padding: 10px 16px;
  border-radius: 6px;
  border: 1px solid #ddd;
  background-color: #fff;
  cursor: pointer;
  transition: all 0.2s;
  font-size: 14px;
}

.nav-btn:hover,
.back-btn:hover {
  border-color: #1976d2;
  color: #1976d2;
}

.nav-btn:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

.submit-btn {
  background-color: #1976d2;
  color: #fff;
  border-color: #1976d2;
}

.submit-btn:hover {
  background-color: #1565c0;
  border-color: #1565c0;
}
</style>
