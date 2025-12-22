<template>
  <div class="exam-page">
    <!-- Exam list (similar to repositories cards) -->
    <div v-if="!selectedNote" class="paper-list">
      <h2 class="page-title">Select exam / note</h2>
      <p class="page-subtitle">Choose a note to view or generate exams.</p>

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
        <div class="exam-info-left">
          <button class="back-btn" @click="backToList">Back to exam list</button>
          <div class="exam-title">
            {{ selectedNote.title }}
            <span v-if="selectedExam">- {{ selectedExam.name }}</span>
          </div>
          <div class="exam-sub-row">
            <div v-if="Object.keys(currentNoteExams || {}).length" class="exam-switcher">
              <label class="exam-switcher-label">Test set:</label>
              <select class="exam-switcher-select" v-model="selectedExamId" @change="onChangeExam">
                <option
                  v-for="exam in Object.values(currentNoteExams).sort((a, b) => new Date(b.createdAt || 0) - new Date(a.createdAt || 0))"
                  :key="exam.id"
                  :value="exam.id"
                >
                  {{ exam.name }} ({{ new Date(exam.createdAt).toLocaleDateString() }})
                </option>
              </select>
            </div>
            <div v-else class="exam-switcher">
              <button class="exam-secondary-btn" @click="showNewExamConfig">
                + Generate first test
              </button>
            </div>
            <div class="exam-actions" v-if="selectedNote">
              <button class="exam-secondary-btn" @click="showNewExamConfig">
                + New test
              </button>
              <button
                v-if="selectedExam"
                class="exam-secondary-btn"
                @click="redoCurrentExam"
              >
                Redo this test
              </button>
              <button
                v-if="selectedExam"
                class="exam-danger-btn"
                @click="deleteExam(selectedExam.id)"
              >
                Delete test
              </button>
            </div>
          </div>
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
          <div
            v-if="selectedExam && currentExamAttempts.length > 1"
            class="attempt-switcher"
          >
            <label class="attempt-label">Attempts:</label>
            <select
              v-model="currentAttemptId"
              @change="onChangeAttempt"
              class="attempt-select"
            >
              <option
                v-for="(att, idx) in currentExamAttempts"
                :key="att.id"
                :value="att.id"
              >
                {{ formatAttemptLabel(att, idx) }}
              </option>
            </select>
          </div>
        </div>
      </div>
      
      <!-- 生成配置（简单表单，尽量不影响原有布局） -->
      <div v-if="showGenerateConfig" class="question-section">
        <div class="question-box">
          <div class="question-header">
            <div class="question-type">Generate exam</div>
          </div>
          <div class="question-body">
            <div style="display:flex; flex-direction:column; gap:12px; max-width:480px;">
              <label>
                Test name:
                <input
                  v-model="generateConfig.name"
                  type="text"
                  style="width:100%; padding:6px 8px; border:1px solid #ddd; border-radius:4px;"
                />
              </label>
              <label>
                Question count:
                <input
                  v-model.number="generateConfig.questionCount"
                  type="number"
                  min="1"
                  max="50"
                  style="width:120px; padding:6px 8px; border:1px solid #ddd; border-radius:4px;"
                />
              </label>
              <label>
                Difficulty:
                <select
                  v-model="generateConfig.difficulty"
                  style="width:160px; padding:6px 8px; border:1px solid #ddd; border-radius:4px;"
                >
                  <option value="easy">Easy</option>
                  <option value="medium">Medium</option>
                  <option value="hard">Hard</option>
                </select>
              </label>
              <div>
                <div style="margin-bottom:4px;">Question types:</div>
                <label style="margin-right:12px;">
                  <input
                    type="checkbox"
                    value="single"
                    v-model="generateConfig.selectedTypes"
                  />
                  Single choice
                </label>
                <label style="margin-right:12px;">
                  <input
                    type="checkbox"
                    value="true-false"
                    v-model="generateConfig.selectedTypes"
                  />
                  True / False
                </label>
                <label>
                  <input
                    type="checkbox"
                    value="open"
                    v-model="generateConfig.selectedTypes"
                  />
                  Open question
                </label>
              </div>
              <div class="answer-actions">
                <button class="nav-btn" @click="showGenerateConfig = false">
                  Cancel
                </button>
                <button class="submit-btn" @click="createNewExamWithAI">
                  Generate exam
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="question-section" v-if="!showGenerateConfig">
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
          <div class="question-footer-actions" v-if="totalQuestions > 0">
            <button class="nav-btn" :disabled="currentIndex === 0" @click="prevQuestion">Previous</button>
            <button class="nav-btn" :disabled="currentIndex === totalQuestions - 1" @click="nextQuestion">Next</button>
            <button class="submit-btn" @click="submitExam">Submit exam</button>
          </div>
        </div>
      </div>
      
      <div
        v-if="currentQuestion.type === 'open question'"
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
import { subjects as seedSubjects } from '../data/notes'

// 从 localStorage 或默认数据构建科目和笔记
const SUBJECTS_STORAGE_KEY = 'revai-subjects'
// 试卷与作答缓存（v2 结构，兼容旧数据）
// 结构示例：
// {
//   [noteId]: {
//     exams: {
//       [examId]: {
//         id,
//         name,
//         createdAt,
//         config: { questionCount, difficulty, selectedTypes },
//         questions: Question[],
//         attempts: [
//           {
//             id,
//             createdAt,
//             score,
//             answers,
//             hasSubmitted
//           }
//         ]
//       }
//     }
//   }
// }
const EXAMS_STORAGE_KEY = 'revai-exams-v2'
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

// 从本地读取某个笔记下的所有试卷（兼容旧结构）
const loadNoteExamsFromStorage = (noteId) => {
  try {
    const stored = localStorage.getItem(EXAMS_STORAGE_KEY)
    if (!stored) return { exams: {} }
    const parsed = JSON.parse(stored)
    if (!parsed || typeof parsed !== 'object') return { exams: {} }
    const noteData = parsed[noteId]
    if (!noteData) return { exams: {} }
    // 兼容旧数据：如果直接存的是 { questions, answers, ... }，则包装成默认考试 default
    if (!noteData.exams) {
      if (Array.isArray(noteData.questions) && noteData.questions.length) {
        return {
          exams: {
            default: {
              id: 'default',
              name: '默认测试',
              createdAt: noteData.savedAt || new Date().toISOString(),
              config: {
                questionCount: noteData.questions.length,
                difficulty: 'medium',
                selectedTypes: ['single', 'true-false', 'open'],
              },
              questions: noteData.questions,
              attempts: [
                {
                  id: 'legacy',
                  createdAt: noteData.savedAt || new Date().toISOString(),
                  score: noteData.score || 0,
                  answers: noteData.answers || {},
                  hasSubmitted: !!noteData.hasSubmitted,
                },
              ],
            },
          },
        }
      }
      return { exams: {} }
    }
    return { exams: noteData.exams || {} }
  } catch (e) {
    console.warn('Failed to load exams from localStorage', e)
  }
  return { exams: {} }
}

// 保存某个笔记下的所有试卷
const saveNoteExamsToStorage = (noteId, examsObject) => {
  try {
    const stored = localStorage.getItem(EXAMS_STORAGE_KEY)
    const all = stored ? JSON.parse(stored) : {}
    all[noteId] = {
      exams: examsObject,
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

// 当前选中的笔记（paper 就是一个 note）
const selectedNote = ref(null)
// 当前笔记下的所有试卷
const currentNoteExams = ref({}) // { [examId]: examMeta }

// 当前正在答的“试卷 + 作答”
const selectedExam = ref(null) // { id, name, questions, config, ... }
const selectedExamId = ref(null) // 供下拉框使用
const currentAttemptId = ref(null)
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

// 当前试卷下的所有作答记录
const currentExamAttempts = computed(() => {
  if (!selectedExam.value || !Array.isArray(selectedExam.value.attempts)) {
    return []
  }
  // 最近的在前
  return [...selectedExam.value.attempts].sort((a, b) => {
    return new Date(b.createdAt || 0) - new Date(a.createdAt || 0)
  })
})

const formatAttemptLabel = (attempt, index) => {
  const idx = currentExamAttempts.value.length - index
  const date = attempt.createdAt ? new Date(attempt.createdAt).toLocaleString() : ''
  const scoreText =
    typeof attempt.score === 'number' && attempt.hasSubmitted
      ? `Score: ${attempt.score}`
      : 'Not submitted'
  return `Attempt ${idx} - ${scoreText}${date ? ' (' + date + ')' : ''}`
}

// 当前试卷的客观题总分（只统计 single / true-false）
const maxObjectiveScore = computed(() => {
  if (!questions.value || !questions.value.length) return 0
  return questions.value.reduce((sum, q) => {
    if (!q || q.type === 'open question') return sum
    const pts = parseInt(q.points, 10)
    return sum + (Number.isFinite(pts) ? pts : 0)
  }, 0)
})

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

// AI 生成试卷状态 & 配置
const isGeneratingExam = ref(false)
const generatingExamStatus = ref('')

// 生成配置（题型、题数、难度）
const showGenerateConfig = ref(false)
const generateConfig = ref({
  name: '测试1',
  questionCount: 8,
  difficulty: 'medium',
  // 前端希望生成的题型，传给后端做提示
  selectedTypes: ['single', 'true-false', 'open'],
})

// 方便根据当前 exams 自动生成下一个测试名称
const getNextExamName = () => {
  if (!selectedNote.value) return '测试1'
  const exams = currentNoteExams.value || {}
  const prefix = '测试'
  const existing = Object.values(exams).map(e => e.name).filter(Boolean)
  let idx = 1
  while (existing.includes(`${prefix}${idx}`)) {
    idx += 1
  }
  return `${prefix}${idx}`
}

const selectPaper = async (paper) => {
  // 选择笔记
  selectedNote.value = paper
  selectedExam.value = null
  currentNoteExams.value = loadNoteExamsFromStorage(paper.id).exams || {}
  userAnswer.value = null
  subjectiveAnswer.value = ''
  answers.value = {}
  score.value = 0
  hasSubmitted.value = false

  // 如果已有试卷，默认选中最新的一份及其最近一次作答
  const exams = currentNoteExams.value
  const examList = Object.values(exams).sort((a, b) => {
    return new Date(b.createdAt || 0) - new Date(a.createdAt || 0)
  })

  if (examList.length) {
    loadExamSession(examList[0].id, { useLatestAttempt: true })
  } else {
    // 没有试卷时，弹出生成配置
    generateConfig.value = {
      name: getNextExamName(),
      questionCount: paper.total || 8,
      difficulty: 'medium',
      selectedTypes: ['single', 'true-false', 'open'],
    }
    showGenerateConfig.value = true
  }
}

const backToList = () => {
  selectedNote.value = null
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
      persistCurrentAttempt()
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
    persistCurrentAttempt()
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
      const pts = parseInt(q.points, 10)
      total += Number.isFinite(pts) ? pts : 0
    }
  })

  score.value = total
  hasSubmitted.value = true

  persistCurrentAttempt(true, total)

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

// 根据当前 selectedExam + currentAttemptId，将答案等写回 localStorage
const persistCurrentAttempt = (submitted = false, finalScore = null) => {
  if (!selectedNote.value || !selectedExam.value || !currentAttemptId.value) return
  const noteId = selectedNote.value.id
  const exams = { ...(currentNoteExams.value || {}) }
  const examId = selectedExam.value.id
  const exam = exams[examId]
  if (!exam) return

  const attempts = Array.isArray(exam.attempts) ? [...exam.attempts] : []
  const idx = attempts.findIndex(a => a.id === currentAttemptId.value)
  const now = new Date().toISOString()
  const payload = {
    id: currentAttemptId.value,
    createdAt: attempts[idx]?.createdAt || now,
    score: typeof finalScore === 'number' ? finalScore : (attempts[idx]?.score || 0),
    answers: { ...answers.value },
    hasSubmitted: submitted ? true : (attempts[idx]?.hasSubmitted || false),
  }
  if (idx >= 0) {
    attempts[idx] = payload
  } else {
    attempts.push(payload)
  }

  exams[examId] = {
    ...exam,
    questions: [...questions.value],
    attempts,
  }
  currentNoteExams.value = exams
  saveNoteExamsToStorage(noteId, exams)
}

// 选择某一套试卷 + 某次作答（或新作答）
const loadExamSession = (examId, { attemptId = null, useLatestAttempt = false } = {}) => {
  const exams = currentNoteExams.value || {}
  const exam = exams[examId]
  if (!exam) return

  selectedExam.value = exam
  selectedExamId.value = exam.id
  questions.value = Array.isArray(exam.questions) ? [...exam.questions] : []
  currentIndex.value = 0

  const attempts = Array.isArray(exam.attempts) ? exam.attempts : []
  let targetAttempt = null

  if (attemptId) {
    targetAttempt = attempts.find(a => a.id === attemptId) || null
  } else if (useLatestAttempt && attempts.length) {
    targetAttempt = [...attempts].sort((a, b) => {
      return new Date(b.createdAt || 0) - new Date(a.createdAt || 0)
    })[0]
  }

  if (!targetAttempt) {
    // 新作答
    const newAttemptId = `attempt-${Date.now()}`
    currentAttemptId.value = newAttemptId
    answers.value = {}
    score.value = 0
    hasSubmitted.value = false
    userAnswer.value = null
    subjectiveAnswer.value = ''
    persistCurrentAttempt(false, 0)
  } else {
    currentAttemptId.value = targetAttempt.id
    answers.value = { ...(targetAttempt.answers || {}) }
    score.value = targetAttempt.score || 0
    hasSubmitted.value = !!targetAttempt.hasSubmitted

    // 恢复第一题的作答状态
    const firstQ = questions.value[0]
    if (firstQ && firstQ.id && targetAttempt.answers[firstQ.id]) {
      if (firstQ.type === 'open question') {
        subjectiveAnswer.value = targetAttempt.answers[firstQ.id]
        userAnswer.value = null
      } else {
        userAnswer.value = targetAttempt.answers[firstQ.id]
        subjectiveAnswer.value = ''
      }
    } else {
      userAnswer.value = null
      subjectiveAnswer.value = ''
    }
  }
}

// 创建一套新试卷（根据当前配置）
const createNewExamWithAI = async () => {
  if (!selectedNote.value) return
  const paper = selectedNote.value
  const cfg = generateConfig.value

  showGenerateConfig.value = false

  // 初始化状态
  userAnswer.value = null
  subjectiveAnswer.value = ''
  answers.value = {}
  score.value = 0
  hasSubmitted.value = false

  const success = await generateQuestionsWithAI(paper, {
    questionCount: cfg.questionCount || 8,
    difficulty: cfg.difficulty || 'medium',
    selectedTypes: cfg.selectedTypes || ['single', 'true-false', 'open'],
  })
  if (!success) {
    generateQuestionsFromContent(paper.content)
  }
}

// 供下拉框切换试卷时使用
const onChangeExam = () => {
  if (!selectedExamId.value) return
  loadExamSession(selectedExamId.value, { useLatestAttempt: true })
}

// 在同一套试卷内切换不同作答记录
const onChangeAttempt = () => {
  if (!selectedExam.value || !currentAttemptId.value) return
  loadExamSession(selectedExam.value.id, { attemptId: currentAttemptId.value })
}

// 手动打开生成配置（比如点击“New test”按钮）
const showNewExamConfig = () => {
  if (!selectedNote.value) return
  generateConfig.value = {
    name: getNextExamName(),
    questionCount: selectedNote.value.total || 8,
    difficulty: 'medium',
    selectedTypes: ['single', 'true-false', 'open'],
  }
  showGenerateConfig.value = true
}

// 删除一整套试卷
const deleteExam = (examId) => {
  if (!selectedNote.value) return
  const exams = { ...(currentNoteExams.value || {}) }
  if (!exams[examId]) return
  if (!confirm('确定要删除这套测试吗？此操作无法撤销，但不会删除笔记本身。')) return

  delete exams[examId]
  currentNoteExams.value = exams
  saveNoteExamsToStorage(selectedNote.value.id, exams)

  // 如果删掉的是当前正在看的试卷，则切换到最新一份或清空
  if (selectedExam.value?.id === examId) {
    const examList = Object.values(exams).sort((a, b) => {
      return new Date(b.createdAt || 0) - new Date(a.createdAt || 0)
    })
    if (examList.length) {
      loadExamSession(examList[0].id, { useLatestAttempt: true })
    } else {
      selectedExam.value = null
      questions.value = []
      currentIndex.value = 0
      answers.value = {}
      score.value = 0
      hasSubmitted.value = false
    }
  }
}

// 为当前试卷再做一次（保留历史作答）
const redoCurrentExam = () => {
  if (!selectedExam.value) return
  loadExamSession(selectedExam.value.id, { attemptId: null, useLatestAttempt: false })
}

// 使用后端 AI 生成试卷
const generateQuestionsWithAI = async (paper, { questionCount = 8, difficulty = 'medium', selectedTypes = ['single', 'true-false', 'open'] } = {}) => {
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
        selectedTypes,
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

    // 创建一套新的试卷元数据
    if (paper?.id) {
      const exams = { ...(currentNoteExams.value || {}) }
      const examId = `exam-${Date.now()}`
      const cfg = generateConfig.value
      const examMeta = {
        id: examId,
        name: cfg.name || getNextExamName(),
        createdAt: new Date().toISOString(),
        config: {
          questionCount: questionCount,
          difficulty,
          selectedTypes,
        },
        questions: mapped,
        attempts: [],
      }
      exams[examId] = examMeta
      currentNoteExams.value = exams
      saveNoteExamsToStorage(paper.id, exams)

      // 立刻进入这套试卷的首次作答
      loadExamSession(examId, { attemptId: null })
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
  align-items: stretch;
  background-color: #fff;
  padding: 16px 24px;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
  gap: 16px;
}

.exam-info-left {
  display: flex;
  flex-direction: column;
  gap: 8px;
  font-size: 16px;
  color: #333;
}

.exam-sub-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 16px;
  align-items: center;
}

.exam-switcher {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.exam-switcher-label {
  font-size: 13px;
  color: #555;
}

.exam-switcher-select {
  padding: 6px 10px;
  border-radius: 6px;
  border: 1px solid #ddd;
  font-size: 13px;
  background-color: #fafafa;
}

.exam-actions {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.question-count {
  font-weight: 500;
}

.exam-title {
  font-size: 18px;
  font-weight: 600;
}

.score-box {
  min-width: 180px;
  background: linear-gradient(135deg, #e3f2fd 0%, #e8f5e9 100%);
  padding: 10px 16px;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 500;
  display: flex;
  flex-direction: column;
  gap: 6px;
  align-items: flex-start;
  justify-content: center;
}

.score-label {
  display: flex;
  align-items: baseline;
  gap: 6px;
}

.score-title {
  font-size: 12px;
  text-transform: uppercase;
  color: #1a237e;
  letter-spacing: 0.04em;
}

.score-value {
  font-size: 22px;
  font-weight: 700;
  color: #2e7d32;
}

.score-unit {
  font-size: 12px;
  color: #388e3c;
}

.attempt-switcher {
  display: flex;
  align-items: center;
  gap: 6px;
}

.attempt-label {
  font-size: 12px;
  color: #424242;
}

.attempt-select {
  font-size: 12px;
  padding: 4px 6px;
  border-radius: 6px;
  border: 1px solid #c5cae9;
  background-color: #ffffff;
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

.exam-secondary-btn {
  padding: 6px 12px;
  border-radius: 6px;
  border: 1px solid #e0e0e0;
  background-color: #fafafa;
  cursor: pointer;
  font-size: 13px;
  color: #424242;
  transition: all 0.2s;
}

.exam-secondary-btn:hover {
  background-color: #e3f2fd;
  border-color: #90caf9;
  color: #1565c0;
}

.exam-danger-btn {
  padding: 6px 12px;
  border-radius: 6px;
  border: 1px solid #ffcdd2;
  background-color: #ffebee;
  cursor: pointer;
  font-size: 13px;
  color: #c62828;
  transition: all 0.2s;
}

.exam-danger-btn:hover {
  background-color: #ffcdd2;
  border-color: #e53935;
  color: #b71c1c;
}
</style>
