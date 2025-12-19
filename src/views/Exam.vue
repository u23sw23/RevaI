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
        <div v-if="isGeneratingExam" class="exam-generating-banner">
          <div class="spinner"></div>
          <div class="exam-generating-text">
            <span class="exam-generating-title">AI is generating questions for this note</span>
            <span class="exam-generating-status">{{ generatingExamStatus }}</span>
          </div>
        </div>
        <div class="question-box">
          <div class="question-header">
            <div class="question-type">{{ formatQuestionType(currentQuestion.type) }}</div>
            <div class="question-points">{{ currentQuestion.points }} pts</div>
          </div>
          <div class="question-body">
            {{ currentQuestion.title }}
          </div>
          <ul v-if="currentQuestion.options" class="options-list">
            <li
              v-for="option in currentQuestion.options"
              :key="option.value"
              :class="[
                'option-item',
                { 
                  active: userAnswer === option.value,
                  correct: hasSubmitted && isCorrectOption(option.value),
                  wrong:
                    hasSubmitted &&
                    userAnswer === option.value &&
                    !isCorrectOption(option.value)
                }
              ]"
              @click="selectAnswer(option.value)"
            >
              <span class="option-label">{{ option.label }}</span>
              <span class="option-text">{{ option.text }}</span>
            </li>
          </ul>

          <!-- 提交后显示正确答案与解析 -->
          <div v-if="hasSubmitted" class="explanation-box">
            <div class="explanation-answer">
              Correct answer:
              <strong>{{ currentQuestion.correctAnswer || 'N/A' }}</strong>
            </div>
            <div
              v-if="currentQuestion.explanation"
              class="explanation-text"
            >
              {{ currentQuestion.explanation }}
            </div>
            <div v-else class="explanation-text muted">
              No explanation provided for this question.
            </div>
          </div>
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
            @input="updateSubjectiveAnswer"
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
import { computed, ref, onMounted } from 'vue'
import { subjects as seedSubjects } from '../data/notes'

// 从 localStorage 或默认数据构建科目和笔记
const SUBJECTS_STORAGE_KEY = 'revai-subjects'
const EXAMS_STORAGE_KEY = 'revai-exams' // 缓存 AI 生成试卷：{ [paperId]: { questions } }
const subjects = ref([])

const loadSubjects = () => {
  try {
    const stored = localStorage.getItem(SUBJECTS_STORAGE_KEY)
    if (stored) {
      const parsed = JSON.parse(stored)
      if (Array.isArray(parsed) && parsed.length) {
        subjects.value = parsed
        return
      }
    }
  } catch (e) {
    console.warn('Failed to load subjects for exams, using seed data', e)
  }
  subjects.value = seedSubjects.map(s => ({ ...s }))
}

// 试卷缓存：按 paperId 存储，包含 questions, answers, hasSubmitted, score
const loadExamFromStorage = (paperId) => {
  try {
    const stored = localStorage.getItem(EXAMS_STORAGE_KEY)
    if (!stored) return null
    const parsed = JSON.parse(stored)
    if (parsed && parsed[paperId] && Array.isArray(parsed[paperId].questions)) {
      return parsed[paperId]
    }
  } catch (e) {
    console.warn('Failed to load exams from localStorage', e)
  }
  return null
}

const saveExamToStorage = (paperId, examData) => {
  try {
    const stored = localStorage.getItem(EXAMS_STORAGE_KEY)
    const all = stored ? JSON.parse(stored) : {}
    all[paperId] = {
      questions: examData.questions || [],
      answers: examData.answers || {},
      hasSubmitted: examData.hasSubmitted || false,
      score: examData.score || 0,
      savedAt: new Date().toISOString(),
    }
    localStorage.setItem(EXAMS_STORAGE_KEY, JSON.stringify(all))
  } catch (e) {
    console.warn('Failed to save exams to localStorage', e)
  }
}

onMounted(() => {
  loadSubjects()
})

// Build exam list dynamically from note data
const papers = computed(() =>
  subjects.value.flatMap(subject =>
    (subject.notes || []).map(note => ({
      id: note.id,
      title: note.title,
      desc: subject.name,
      total: 8, // default number of questions per exam
      updatedAt: note.updatedAt,
      content: note.content,
    }))
  )
)

const selectedExam = ref(null)
const currentIndex = ref(0)
const score = ref(0)
const userAnswer = ref(null)
const subjectiveAnswer = ref('')
const answers = ref({}) // 记录每题作答
const hasSubmitted = ref(false)

// Questions generated from selected note content
const questions = ref([])

const totalQuestions = computed(() => questions.value.length)
const currentQuestion = computed(() => questions.value[currentIndex.value] || {})

// 判断选项是否为正确答案（支持大小写和 True/False 匹配）
const isCorrectOption = (optionValue) => {
  const correct = String(currentQuestion.value.correctAnswer || '').trim().toLowerCase()
  const val = String(optionValue || '').trim().toLowerCase()
  return correct === val
}

// 格式化题目类型显示
const formatQuestionType = (type) => {
  const typeMap = {
    'single choice': 'Single Choice',
    'true-false': 'True / False',
    'open question': 'Open Question',
  }
  return typeMap[type] || type
}

// AI 生成试卷状态
const isGeneratingExam = ref(false)
const generatingExamStatus = ref('')

const selectPaper = async (paper) => {
  selectedExam.value = paper
  currentIndex.value = 0
  
  // 先尝试从本地缓存读取试卷（包含题目、答案、提交状态）
  const cached = loadExamFromStorage(paper.id)
  if (cached && Array.isArray(cached.questions) && cached.questions.length) {
    questions.value = cached.questions
    answers.value = cached.answers || {}
    hasSubmitted.value = cached.hasSubmitted || false
    score.value = cached.score || 0
    // 恢复第一题的作答状态
    const firstQ = questions.value[0]
    if (firstQ && firstQ.id && answers.value[firstQ.id]) {
      if (firstQ.type === 'open question') {
        subjectiveAnswer.value = answers.value[firstQ.id]
        userAnswer.value = null
      } else {
        userAnswer.value = answers.value[firstQ.id]
        subjectiveAnswer.value = ''
      }
    } else {
      userAnswer.value = null
      subjectiveAnswer.value = ''
    }
    return
  }

  // 没有缓存，重置所有状态
  userAnswer.value = null
  subjectiveAnswer.value = ''
  answers.value = {}
  score.value = 0
  hasSubmitted.value = false

  // 默认直接使用 AI 生成试卷；如失败再回退到本地生成
  const success = await generateQuestionsWithAI(paper, { questionCount: paper.total || 8, difficulty: 'medium' })
  if (!success) {
    generateQuestionsFromContent(paper.content)
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
}

const selectAnswer = (val) => {
  const qType = currentQuestion.value.type
  if (qType === 'single choice' || qType === 'true-false') {
    userAnswer.value = val
    if (currentQuestion.value.id) {
      answers.value[currentQuestion.value.id] = val
      // 自动保存答案到 localStorage
      if (selectedExam.value?.id) {
        saveExamToStorage(selectedExam.value.id, {
          questions: questions.value,
          answers: answers.value,
          hasSubmitted: hasSubmitted.value,
          score: score.value,
        })
      }
    }
  }
}

const nextQuestion = () => {
  if (currentIndex.value < totalQuestions.value - 1) {
    currentIndex.value += 1
    const q = currentQuestion.value
    const saved = q && q.id ? answers.value[q.id] : null
    userAnswer.value = typeof saved === 'string' ? saved : null
    subjectiveAnswer.value = typeof saved === 'string' && currentQuestion.value.type === 'open question' ? saved : ''
  }
}

const prevQuestion = () => {
  if (currentIndex.value > 0) {
    currentIndex.value -= 1
    const q = currentQuestion.value
    const saved = q && q.id ? answers.value[q.id] : null
    userAnswer.value = typeof saved === 'string' ? saved : null
    subjectiveAnswer.value = typeof saved === 'string' && currentQuestion.value.type === 'open question' ? saved : ''
  }
}

// 同步主观题答案
const updateSubjectiveAnswer = () => {
  if (currentQuestion.value.type === 'open question' && currentQuestion.value.id) {
    answers.value[currentQuestion.value.id] = subjectiveAnswer.value
    // 自动保存答案到 localStorage
    if (selectedExam.value?.id) {
      saveExamToStorage(selectedExam.value.id, {
        questions: questions.value,
        answers: answers.value,
        hasSubmitted: hasSubmitted.value,
        score: score.value,
      })
    }
  }
}

const submitExam = () => {
  if (!questions.value.length) {
    alert('No questions to submit.')
    return
  }

  // 计算客观题得分：单选 / 判断
  let total = 0
  questions.value.forEach((q) => {
    const ans = answers.value[q.id]
    if (!ans || q.type === 'open question') return
    const userAns = String(ans).trim().toUpperCase()
    const correct = String(q.correctAnswer || '').trim().toUpperCase()
    if (userAns && correct && userAns === correct) {
      total += q.points || 0
    }
  })

  score.value = total
  hasSubmitted.value = true

  // 保存提交状态到 localStorage
  if (selectedExam.value?.id) {
    saveExamToStorage(selectedExam.value.id, {
      questions: questions.value,
      answers: answers.value,
      hasSubmitted: true,
      score: total,
    })
  }

  alert(`Exam submitted! Your objective score: ${score.value} pts`)
}

// Very simple local generator that creates questions based on note content.
// Used as a fallback when AI generation fails.
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

// 使用后端 AI 生成试卷
const generateQuestionsWithAI = async (paper, { questionCount = 8, difficulty = 'medium' } = {}) => {
  if (!paper?.content) {
    return false
  }

  isGeneratingExam.value = true
  generatingExamStatus.value = 'AI is generating questions based on this note...'
  questions.value = []

  try {
    const res = await fetch('/api/ai/generate-exam', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        noteContent: paper.content,
        questionCount,
        difficulty,
      }),
    })

    const text = await res.text()
    if (!res.ok) {
      let msg = text
      try {
        const err = JSON.parse(text)
        msg = err.error || JSON.stringify(err)
      } catch {
        // ignore
      }
      alert('Failed to generate exam: ' + msg)
      return false
    }

    let data
    try {
      data = JSON.parse(text)
    } catch (e) {
      console.error('Failed to parse exam JSON:', e, text.slice(0, 200))
      alert('Failed to parse AI exam response.')
      return false
    }

    const apiQuestions = Array.isArray(data.questions) ? data.questions : []
    if (!apiQuestions.length) {
      alert('AI did not return any questions. Please try again with fewer questions or shorter notes.')
      return false
    }

    const mapped = apiQuestions.map((q, idx) => {
      // 确定题目类型
      let questionType = 'single choice'
      if (q.type === 'open') {
        questionType = 'open question'
      } else if (q.type === 'true-false') {
        questionType = 'true-false'
      }

      // 处理选项
      let options = null
      if (questionType === 'true-false') {
        // 判断题强制生成 True/False 选项
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
        title: q.stem || `Question ${idx + 1}`,
        type: questionType,
        options,
        points: typeof q.points === 'number' ? q.points : 5,
        correctAnswer: q.answer,
        explanation: q.explanation || '',
      }
    })

    questions.value = mapped
    // 缓存到本地，避免下次重复生成
    if (paper?.id) {
      saveExamToStorage(paper.id, { questions: mapped })
    }

    generatingExamStatus.value = `AI generated ${questions.value.length} questions.`
    return true
  } catch (e) {
    console.error('Error generating exam with AI:', e)
    alert('Error generating exam with AI: ' + e.message)
    return false
  } finally {
    isGeneratingExam.value = false
  }
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
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.exam-generating-banner {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  border-radius: 8px;
  background: linear-gradient(135deg, #e3f2fd 0%, #f3e5f5 100%);
  border: 1px solid #d0e2ff;
  font-size: 13px;
}

.spinner {
  width: 18px;
  height: 18px;
  border-radius: 50%;
  border: 2px solid #ffffff;
  border-top-color: #1976d2;
  animation: exam-spin 0.8s linear infinite;
}

@keyframes exam-spin {
  to {
    transform: rotate(360deg);
  }
}

.exam-generating-text {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.exam-generating-title {
  font-weight: 600;
  color: #1a237e;
}

.exam-generating-status {
  color: #424242;
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

.option-item.correct {
  border-color: #2e7d32;
  background-color: #e8f5e9;
}

.option-item.wrong {
  border-color: #c62828;
  background-color: #ffebee;
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

.explanation-box {
  margin-top: 12px;
  padding: 10px 12px;
  border-radius: 8px;
  background-color: #fafafa;
  border: 1px solid #eee;
  font-size: 13px;
  color: #333;
}

.explanation-answer {
  margin-bottom: 6px;
}

.explanation-text.muted {
  color: #999;
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
