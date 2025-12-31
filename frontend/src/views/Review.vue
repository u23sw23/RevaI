<template>
  <div class="review-page">
    
    <div v-if="!selectedExam" class="review-list">
      <h2 class="page-title">Review</h2>
      <p class="page-subtitle">Practice exams based on your learning progress.</p>
      
      <div v-if="loading" class="loading">Loading...</div>
      <div v-else-if="reviewExams.length === 0" class="empty-state">
        <p>No exams available for review. Complete some exams first!</p>
      </div>
      <div v-else class="papers-grid">
        <div
          v-for="item in reviewExams"
          :key="item.exam.id"
          class="paper-card review-card"
          @click="selectExam(item.exam)"
        >
          <div class="paper-header">
            <div class="paper-main">
              <div class="paper-title">{{ item.exam.title }}</div>
              <div class="paper-desc">Review this exam</div>
            </div>
          </div>
          <div class="paper-meta">
            <span v-if="item.lastPercentage !== null && item.lastPercentage !== undefined">
              Last: {{ typeof item.lastPercentage === 'number' ? item.lastPercentage.toFixed(1) : item.lastPercentage }}%
            </span>
            <span v-else>Never attempted</span>
            <span v-if="item.attemptCount > 0">
              Attempts: {{ item.attemptCount }}
            </span>
            <span v-if="item.lastSubmitTime">
              {{ formatDate(item.lastSubmitTime) }}
            </span>
          </div>
        </div>
      </div>
    </div>

    <div v-else class="exam-detail">
      <div class="exam-header">
        <div class="exam-info-left">
          <button class="back-btn" @click="backToList">← Back to review list</button>
          <div class="exam-title">{{ selectedExam.title }}</div>
          <div class="question-count">
            Total {{ totalQuestions }} questions / Question {{ currentIndex + 1 }}
          </div>
        </div>
        <div class="score-box">
          <div class="score-label">
            <span class="score-title">Current score</span>
            <span class="score-value">{{ score }}</span>
            <span class="score-unit">/ {{ maxObjectiveScore }} pts</span>
          </div>
        </div>
      </div>
      
      <div class="question-section">
        <div class="question-box">
          <div class="question-header">
            <div class="question-type">{{ formatQuestionType(currentQuestion.type) }}</div>
            <div class="question-points">{{ currentQuestion.points }} pts</div>
          </div>
          <div class="question-body">{{ currentQuestion.title }}</div>
          <ul v-if="currentQuestion.options" class="options-list">
            <li
              v-for="option in currentQuestion.options"
              :key="option.value"
              :class="[
                'option-item',
                { 
                  active: userAnswer === option.value,
                  correct: hasSubmitted && isCorrectOption(option.value),
                  wrong: hasSubmitted && userAnswer === option.value && !isCorrectOption(option.value)
                }
              ]"
              @click="selectAnswer(option.value)"
            >
              <span class="option-label">{{ option.label }}</span>
              <span class="option-text">{{ option.text }}</span>
            </li>
          </ul>

          <div v-if="hasSubmitted" class="explanation-box">
            <div class="explanation-answer">
              Correct answer: <strong>{{ currentQuestion.correctAnswer || 'N/A' }}</strong>
            </div>
            <div v-if="currentQuestion.explanation" class="explanation-text">
              {{ currentQuestion.explanation }}
            </div>
          </div>
          <div class="question-footer-actions">
            <button class="nav-btn" :disabled="currentIndex === 0" @click="prevQuestion">Previous</button>
            <button class="nav-btn" :disabled="currentIndex === totalQuestions - 1" @click="nextQuestion">Next</button>
            <button class="submit-btn" @click="submitExam">Submit exam</button>
          </div>
        </div>
      </div>
      
      <div
        v-if="currentQuestion.type === 'open question' || currentQuestion.type === 'open_question'"
        class="answer-section"
      >
        <div class="answer-box">
          <h3>Answer area</h3>
          <textarea
            v-model="subjectiveAnswer"
            class="answer-textarea"
            placeholder="Type your answer here..."
            @input="updateSubjectiveAnswer"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, onMounted } from 'vue'
import { api } from '../utils/api'

const loading = ref(false)
const reviewExams = ref([])
const selectedExam = ref(null)

const currentIndex = ref(0)
const score = ref(0)
const userAnswer = ref(null)
const subjectiveAnswer = ref('')
const answers = ref({})
const hasSubmitted = ref(false)
const questions = ref([])

const totalQuestions = computed(() => questions.value.length)
const currentQuestion = computed(() => questions.value[currentIndex.value] || {})

const maxObjectiveScore = computed(() => {
  if (!questions.value || !questions.value.length) return 0
  return questions.value.reduce((sum, q) => {
    if (!q || q.type === 'open question' || q.type === 'open_question') return sum
    const pts = parseInt(q.points, 10)
    return sum + (Number.isFinite(pts) ? pts : 0)
  }, 0)
})

const getCurrentUserId = () => {
  const savedUser = localStorage.getItem('auth_user')
  if (savedUser) {
    try {
      const user = JSON.parse(savedUser)
      return user?.id || null
    } catch (e) {
      console.error('Failed to parse user:', e)
    }
  }
  return null
}

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return date.toLocaleDateString()
}

const formatQuestionType = (type) => {
  const typeMap = {
    'single choice': 'Single Choice',
    'single_choice': 'Single Choice',
    'true-false': 'True / False',
    'true_false': 'True / False',
    'open question': 'Open Question',
    'open_question': 'Open Question',
  }
  return typeMap[type] || type
}

const isCorrectOption = (optionValue) => {
  const correct = String(currentQuestion.value.correctAnswer || '').trim().toLowerCase()
  const val = String(optionValue || '').trim().toLowerCase()
  return correct === val
}

const loadReviewExams = async () => {
  loading.value = true
  try {
    const userId = getCurrentUserId()
    if (!userId) {
      alert('请先登录')
      return
    }
    const exams = await api.getReviewExams(userId, 5)
    reviewExams.value = exams || []
  } catch (error) {
    console.error('Failed to load review exams:', error)
    alert('加载复习试题失败：' + error.message)
  } finally {
    loading.value = false
  }
}

const selectExam = async (exam) => {
  selectedExam.value = exam
  await loadExamSession(exam.id)
}

const loadExamSession = async (examId) => {
  const userId = getCurrentUserId()
  if (!userId) return

  try {
    const exam = await api.getExamById(examId)
    if (!exam) {
      alert('考试不存在')
      return
    }

    if (exam.questions && exam.questions.length > 0) {
      questions.value = formatQuestionsForDisplay(exam.questions)
    } else {
      questions.value = []
    }
    
    currentIndex.value = 0
    answers.value = {}
    score.value = 0
    hasSubmitted.value = false
    userAnswer.value = null
    subjectiveAnswer.value = ''
  } catch (error) {
    console.error('Failed to load exam session:', error)
    alert('加载考试失败：' + error.message)
  }
}

const formatQuestionsForDisplay = (questionsData) => {
  return questionsData.map((q, idx) => {
    let questionType = q.type || 'single choice'
    if (questionType === 'open' || questionType === 'open_question') {
      questionType = 'open question'
    } else if (questionType === 'true-false' || questionType === 'true_false') {
      questionType = 'true-false'
    } else if (questionType === 'single' || questionType === 'single_choice') {
      questionType = 'single choice'
    }

    let options = null
    if (questionType === 'true-false') {
      options = [
        { value: 'True', label: 'A', text: 'True' },
        { value: 'False', label: 'B', text: 'False' },
      ]
    } else if (Array.isArray(q.options) && q.options.length > 0) {
      options = q.options.map(o => ({
        value: o.value,
        label: o.label,
        text: o.text,
      }))
    }

    return {
      id: q.id || String(idx + 1),
      examId: q.examId,
      title: q.title || q.stem || `Question ${idx + 1}`,
      type: questionType,
      options,
      points: typeof q.points === 'number' ? q.points : 5,
      correctAnswer: q.correctAnswer || q.answer,
      explanation: q.explanation || '',
    }
  })
}

const selectAnswer = (val) => {
  const qType = currentQuestion.value.type
  if (qType === 'single choice' || qType === 'single_choice' || 
      qType === 'true-false' || qType === 'true_false') {
    userAnswer.value = val
    if (currentQuestion.value.id) {
      answers.value[currentQuestion.value.id] = val
    }
  }
}

const nextQuestion = () => {
  if (currentIndex.value < totalQuestions.value - 1) {
    currentIndex.value += 1
    const q = currentQuestion.value
    const saved = q && q.id ? answers.value[q.id] : null
    userAnswer.value = typeof saved === 'string' ? saved : null
    subjectiveAnswer.value = typeof saved === 'string' && 
      (q.type === 'open question' || q.type === 'open_question') ? saved : ''
  }
}

const prevQuestion = () => {
  if (currentIndex.value > 0) {
    currentIndex.value -= 1
    const q = currentQuestion.value
    const saved = q && q.id ? answers.value[q.id] : null
    userAnswer.value = typeof saved === 'string' ? saved : null
    subjectiveAnswer.value = typeof saved === 'string' && 
      (q.type === 'open question' || q.type === 'open_question') ? saved : ''
  }
}

const updateSubjectiveAnswer = () => {
  if ((currentQuestion.value.type === 'open question' || currentQuestion.value.type === 'open_question') 
      && currentQuestion.value.id) {
    answers.value[currentQuestion.value.id] = subjectiveAnswer.value
  }
}

const submitExam = async () => {
  if (!questions.value.length) {
    alert('No questions to submit.')
    return
  }

  const userId = getCurrentUserId()
  if (!userId) {
    alert('请先登录')
    return
  }

  const examId = selectedExam.value?.id
  if (!examId) {
    alert('考试ID不存在，无法提交')
    return
  }

  try {
    const answersToSubmit = {}
    for (const [questionId, answer] of Object.entries(answers.value)) {
      answersToSubmit[questionId] = answer
    }

    const result = await api.submitExam(examId, userId, answersToSubmit, null)
    
    score.value = result.totalScore
    hasSubmitted.value = true
    
    alert(`Exam submitted! Your score: ${result.totalScore} / ${result.maxScore} pts (${result.percentage.toFixed(1)}%)`)

    await loadReviewExams()
  } catch (error) {
    console.error('Failed to submit exam:', error)
    alert('提交失败：' + error.message)
  }
}

const backToList = () => {
  selectedExam.value = null
  currentIndex.value = 0
  userAnswer.value = null
  subjectiveAnswer.value = ''
  answers.value = {}
  score.value = 0
  hasSubmitted.value = false
  questions.value = []
}

onMounted(() => {
  loadReviewExams()
})
</script>

<style scoped>
.review-page {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.page-title {
  font-size: 24px;
  font-weight: 700;
  background: linear-gradient(135deg, #1976D2 0%, #2196F3 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  margin-bottom: 8px;
}

.page-subtitle {
  color: #546E7A;
  margin-bottom: 24px;
  font-size: 14px;
}

.loading, .empty-state {
  text-align: center;
  padding: 40px;
  color: #999;
}

.papers-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 16px;
}

.paper-card {
  background: linear-gradient(to bottom, #FFFFFF 0%, #F8FBFF 100%);
  padding: 24px;
  border-radius: 16px;
  box-shadow: 0 4px 12px rgba(33, 150, 243, 0.08);
  cursor: pointer;
  transition: all 0.3s ease;
  border: 2px solid rgba(144, 202, 249, 0.8);
}

.paper-card:hover {
  border-color: #2196F3;
  box-shadow: 0 8px 24px rgba(33, 150, 243, 0.25);
  transform: translateY(-2px);
}

.paper-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 12px;
}

.paper-main {
  flex: 1;
}

.paper-title {
  font-size: 18px;
  font-weight: 600;
  color: #1565C0;
  margin-bottom: 4px;
}

.paper-desc {
  font-size: 14px;
  color: #546E7A;
}

.paper-meta {
  font-size: 12px;
  color: #999;
  display: flex;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 8px;
}

.exam-detail {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.exam-header {
  display: flex;
  justify-content: space-between;
  align-items: stretch;
  background: linear-gradient(to bottom, #FFFFFF 0%, #F8FBFF 100%);
  padding: 20px 28px;
  border-radius: 16px;
  box-shadow: 0 4px 16px rgba(33, 150, 243, 0.12);
  gap: 20px;
  border: 2px solid rgba(144, 202, 249, 0.8);
}

.exam-info-left {
  display: flex;
  flex-direction: column;
  gap: 8px;
  font-size: 16px;
  color: #333;
}

.exam-title {
  font-size: 20px;
  font-weight: 700;
  color: #1565C0;
  letter-spacing: -0.3px;
}

.question-count {
  font-weight: 500;
}

.score-box {
  min-width: 200px;
  background: linear-gradient(135deg, #E3F2FD 0%, #BBDEFB 50%, #E3F2FD 100%);
  padding: 16px 20px;
  border-radius: 12px;
  font-size: 14px;
  font-weight: 500;
  display: flex;
  flex-direction: column;
  gap: 8px;
  align-items: flex-start;
  justify-content: center;
  box-shadow: 0 4px 12px rgba(33, 150, 243, 0.15);
  border: 1px solid rgba(144, 202, 249, 0.5);
}

.score-label {
  display: flex;
  align-items: baseline;
  gap: 6px;
}

.score-title {
  font-size: 11px;
  text-transform: uppercase;
  color: #1565C0;
  letter-spacing: 0.08em;
  font-weight: 600;
}

.score-value {
  font-size: 28px;
  font-weight: 800;
  background: linear-gradient(135deg, #1976D2 0%, #2196F3 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.score-unit {
  font-size: 12px;
  color: #546E7A;
  font-weight: 500;
}

.question-section {
  background: linear-gradient(to bottom, #FFFFFF 0%, #F8FBFF 100%);
  padding: 28px;
  border-radius: 16px;
  box-shadow: 0 4px 16px rgba(33, 150, 243, 0.1);
  display: flex;
  flex-direction: column;
  gap: 16px;
  border: 2px solid rgba(144, 202, 249, 0.8);
}

.question-box {
  min-height: 200px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  border: 2px dashed rgba(144, 202, 249, 0.5);
  border-radius: 12px;
  padding: 24px;
  background: rgba(255, 255, 255, 0.6);
  transition: all 0.3s ease;
}

.question-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 14px;
  color: #666;
}

.question-type {
  background: linear-gradient(135deg, #E3F2FD 0%, #BBDEFB 100%);
  color: #1565C0;
  padding: 6px 12px;
  border-radius: 8px;
  font-weight: 600;
  font-size: 13px;
  box-shadow: 0 2px 4px rgba(33, 150, 243, 0.15);
  border: 1px solid rgba(144, 202, 249, 0.3);
}

.question-points {
  color: #999;
}

.question-body {
  font-size: 17px;
  color: #263238;
  line-height: 1.6;
  font-weight: 500;
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
  gap: 12px;
  padding: 14px 16px;
  border: 2px solid rgba(187, 222, 251, 0.6);
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s ease;
  background: rgba(255, 255, 255, 0.8);
}

.option-item:hover {
  border-color: #64B5F6;
  box-shadow: 0 4px 12px rgba(33, 150, 243, 0.15);
  transform: translateX(4px);
  background: rgba(227, 242, 253, 0.5);
}

.option-item.active {
  border-color: #2196F3;
  background: linear-gradient(to right, #E3F2FD 0%, rgba(187, 222, 251, 0.4) 100%);
  box-shadow: 0 4px 12px rgba(33, 150, 243, 0.2);
}

.option-item.correct {
  border-color: #4CAF50;
  background: linear-gradient(to right, #E8F5E9 0%, rgba(200, 230, 201, 0.4) 100%);
  box-shadow: 0 4px 12px rgba(76, 175, 80, 0.2);
}

.option-item.wrong {
  border-color: #F44336;
  background: linear-gradient(to right, #FFEBEE 0%, rgba(255, 205, 210, 0.4) 100%);
  box-shadow: 0 4px 12px rgba(244, 67, 54, 0.2);
}

.option-label {
  font-weight: 700;
  color: #2196F3;
  font-size: 15px;
  min-width: 24px;
  text-align: center;
  background: linear-gradient(135deg, #E3F2FD 0%, #BBDEFB 100%);
  padding: 4px 8px;
  border-radius: 6px;
  box-shadow: 0 2px 4px rgba(33, 150, 243, 0.15);
}

.option-text {
  color: #333;
}

.explanation-box {
  margin-top: 16px;
  padding: 16px 20px;
  border-radius: 12px;
  background: linear-gradient(to bottom, #F8FBFF 0%, rgba(227, 242, 253, 0.4) 100%);
  border: 1px solid rgba(187, 222, 251, 0.6);
  font-size: 14px;
  color: #263238;
  box-shadow: 0 2px 8px rgba(33, 150, 243, 0.08);
}

.explanation-answer {
  margin-bottom: 6px;
  font-weight: 500;
}

.explanation-text {
  line-height: 1.6;
  color: #555;
}

.question-footer-actions {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
  margin-top: 12px;
}

.answer-section {
  background: linear-gradient(to bottom, #FFFFFF 0%, #F8FBFF 100%);
  padding: 28px;
  border-radius: 16px;
  box-shadow: 0 4px 16px rgba(33, 150, 243, 0.1);
  border: 2px solid rgba(144, 202, 249, 0.8);
}

.answer-box {
  min-height: 300px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  border: 2px dashed rgba(144, 202, 249, 0.5);
  border-radius: 12px;
  padding: 20px;
  background: rgba(255, 255, 255, 0.6);
}

.answer-box h3 {
  font-size: 18px;
  color: #1565C0;
  font-weight: 600;
}

.answer-textarea {
  width: 100%;
  min-height: 200px;
  padding: 14px;
  border: 2px solid rgba(187, 222, 251, 0.6);
  border-radius: 10px;
  font-size: 14px;
  font-family: inherit;
  resize: vertical;
  background: rgba(255, 255, 255, 0.9);
  transition: all 0.3s ease;
}

.answer-textarea:focus {
  outline: none;
  border-color: #64B5F6;
  box-shadow: 0 0 0 4px rgba(33, 150, 243, 0.1);
  background: #FFFFFF;
}

.nav-btn,
.submit-btn,
.back-btn {
  padding: 12px 20px;
  border-radius: 10px;
  border: 2px solid rgba(187, 222, 251, 0.8);
  background-color: #FFFFFF;
  cursor: pointer;
  transition: all 0.3s ease;
  font-size: 14px;
  font-weight: 500;
}

.nav-btn:hover,
.back-btn:hover {
  border-color: #64B5F6;
  color: #1976D2;
  background: linear-gradient(to right, #F0F7FF 0%, rgba(227, 242, 253, 0.5) 100%);
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(33, 150, 243, 0.15);
}

.nav-btn:disabled {
  cursor: not-allowed;
  opacity: 0.5;
  transform: none;
}

.submit-btn {
  background: linear-gradient(135deg, #2196F3 0%, #1976D2 100%);
  color: #fff;
  border-color: #2196F3;
  box-shadow: 0 4px 12px rgba(33, 150, 243, 0.3);
}

.submit-btn:hover {
  background: linear-gradient(135deg, #1976D2 0%, #1565C0 100%);
  border-color: #1976D2;
  box-shadow: 0 6px 16px rgba(33, 150, 243, 0.4);
  transform: translateY(-2px);
}
</style>

