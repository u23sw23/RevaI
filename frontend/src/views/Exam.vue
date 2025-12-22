<template>
  <div class="exam-page">
    <div v-if="!selectedNote" class="paper-list">
      <h2 class="page-title">Select exam / note</h2>
      <p class="page-subtitle">Choose a note to view or generate exams.</p>

      <div class="papers-grid" @click="activeMenuId = null">
        <div
          v-for="paper in papers"
          :key="paper.id"
          class="paper-card"
          @click="selectPaper(paper)"
        >
          <div class="paper-header">
            <div class="paper-main">
              <div class="paper-title">{{ paper.title }}</div>
              <div class="paper-desc">{{ paper.desc }}</div>
            </div>
            <div class="menu-wrapper" @click.stop="toggleMenu(paper.id)">
              <div class="menu-dot"></div>
              <div class="menu-dot"></div>
              <div class="menu-dot"></div>
              <div
                v-if="activeMenuId === paper.id"
                class="paper-menu"
                @click.stop
              >
                <button class="menu-item" @click="startEditNote(paper)">Rename</button>
                <button class="menu-item danger" @click="deleteNote(paper.id)">Delete</button>
              </div>
            </div>
          </div>
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
            <span v-if="selectedExam">- {{ selectedExam.title || selectedExam.name }}</span>
          </div>
          <div class="exam-sub-row">
            <div v-if="currentNoteExams && currentNoteExams.length > 0" class="exam-switcher">
              <label class="exam-switcher-label">Test set:</label>
              <select class="exam-switcher-select" v-model="selectedExamId" @change="onChangeExam">
                <option
                  v-for="exam in currentNoteExams"
                  :key="exam.id"
                  :value="exam.id"
                >
                  {{ exam.title || exam.name }} ({{ formatDate(exam.createTime || exam.createdAt) }})
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
            v-if="selectedExam && currentExamAttempts && currentExamAttempts.length > 1"
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
      
      <!-- 生成配置表单 -->
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

    <!-- Note edit dialog -->
    <div
      v-if="showNoteDialog"
      class="modal-mask"
      @click.self="closeNoteDialog"
    >
      <div class="modal-panel">
        <h3 class="modal-title">Rename note</h3>
        <div class="modal-field">
          <label class="modal-label">Note Name</label>
          <input
            v-model="dialogNoteName"
            type="text"
            class="modal-input"
            placeholder="Enter note name"
          />
        </div>
        <div class="modal-actions">
          <button class="modal-btn" @click="closeNoteDialog">Cancel</button>
          <button class="modal-btn primary" @click="confirmNoteDialog">Confirm</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, onMounted } from 'vue'
import { api } from '../utils/api'

const papers = ref([])
const loading = ref(false)
const activeMenuId = ref(null)

// 获取当前用户ID
const getCurrentUserId = () => {
  const savedUser = localStorage.getItem('auth_user')
  if (savedUser) {
    try {
      const user = JSON.parse(savedUser)
      if (user && user.id) {
        return user.id
      }
    } catch (e) {
      console.error('Failed to parse user from localStorage:', e)
    }
  }
  return null // 如果没有登录用户，返回 null
}

// 加载考试列表（从所有笔记生成）
const loadPapers = async () => {
  loading.value = true
  try {
    const userId = getCurrentUserId()
    if (!userId) {
      alert('请先登录')
      loading.value = false
      return
    }
    const subjects = await api.getSubjects(userId)
    
    // 将所有笔记转换为考试列表
    papers.value = subjects.flatMap(subject =>
      (subject.notes || []).map(note => ({
        id: note.id,
        title: note.title,
        desc: subject.name,
        total: 5, // default number of questions per exam
        updatedAt: note.updatedAt,
        content: note.content
      }))
    )
  } catch (error) {
    console.error('Failed to load papers:', error)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadPapers()
})

// 当前选中的笔记
const selectedNote = ref(null)
// 当前笔记下的所有考试
const currentNoteExams = ref([])
// 当前正在答的考试
const selectedExam = ref(null)
const selectedExamId = ref(null)
// 当前作答记录ID
const currentAttemptId = ref(null)
// 当前试卷的所有作答记录
const currentExamAttempts = ref([])

const currentIndex = ref(0)
const score = ref(0)
const userAnswer = ref(null)
const subjectiveAnswer = ref('')
const answers = ref({}) // 记录每题作答
const hasSubmitted = ref(false)

// Questions generated from selected note content
const questions = ref([])

// AI 生成试卷状态
const isGeneratingExam = ref(false)
const generatingExamStatus = ref('')

// 生成配置
const showGenerateConfig = ref(false)
const generateConfig = ref({
  name: '测试1',
  questionCount: 8,
  difficulty: 'medium',
  selectedTypes: ['single', 'true-false', 'open'],
})

const totalQuestions = computed(() => questions.value.length)
const currentQuestion = computed(() => questions.value[currentIndex.value] || {})

// 当前试卷的客观题总分（只统计 single / true-false）
const maxObjectiveScore = computed(() => {
  if (!questions.value || !questions.value.length) return 0
  return questions.value.reduce((sum, q) => {
    if (!q || q.type === 'open question' || q.type === 'open_question') return sum
    const pts = parseInt(q.points, 10)
    return sum + (Number.isFinite(pts) ? pts : 0)
  }, 0)
})

// 格式化题目类型显示
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

// 判断选项是否为正确答案
const isCorrectOption = (optionValue) => {
  const correct = String(currentQuestion.value.correctAnswer || '').trim().toLowerCase()
  const val = String(optionValue || '').trim().toLowerCase()
  return correct === val
}

// 格式化日期
const formatDate = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return date.toLocaleDateString()
}

// 自动生成下一个测试名称
const getNextExamName = () => {
  if (!selectedNote.value) return '测试1'
  const exams = currentNoteExams.value || []
  const prefix = '测试'
  const existing = exams.map(e => e.title || e.name).filter(Boolean)
  let idx = 1
  while (existing.includes(`${prefix}${idx}`)) {
    idx += 1
  }
  return `${prefix}${idx}`
}

// 格式化作答记录标签
const formatAttemptLabel = (attempt, idx) => {
  const attemptNum = currentExamAttempts.value.length - idx
  const date = attempt.submitTime || attempt.createdAt ? formatDate(attempt.submitTime || attempt.createdAt) : ''
  const scoreText = typeof attempt.score === 'number' && attempt.hasSubmitted
    ? `Score: ${attempt.score}`
    : 'Not submitted'
  return `Attempt ${attemptNum} - ${scoreText}${date ? ' (' + date + ')' : ''}`
}

// 菜单相关函数
const toggleMenu = (id) => {
  activeMenuId.value = activeMenuId.value === id ? null : id
}

// 编辑笔记对话框状态
const showNoteDialog = ref(false)
const dialogNoteName = ref('')
const dialogNote = ref(null)

const startEditNote = (paper) => {
  activeMenuId.value = null
  dialogNote.value = paper
  dialogNoteName.value = paper.title || ''
  showNoteDialog.value = true
}

const closeNoteDialog = () => {
  showNoteDialog.value = false
  dialogNoteName.value = ''
  dialogNote.value = null
}

const confirmNoteDialog = async () => {
  const name = dialogNoteName.value.trim()
  if (!name) {
    return
  }

  if (!dialogNote.value) {
    return
  }

  try {
    const updated = await api.updateNote(dialogNote.value.id, {
      title: name
    })
    // 更新本地数据
    const index = papers.value.findIndex(p => p.id === dialogNote.value.id)
    if (index !== -1) {
      papers.value[index] = {
        ...papers.value[index],
        title: updated.title
      }
    }
    // 如果当前选中的是正在编辑的笔记，也要更新
    if (selectedNote.value?.id === dialogNote.value.id) {
      selectedNote.value = {
        ...selectedNote.value,
        title: updated.title
      }
    }
    closeNoteDialog()
  } catch (error) {
    console.error('Failed to update note:', error)
  }
}

const deleteNote = async (id) => {
  if (!confirm('Delete this note? All its exams will also be removed.')) {
    return
  }
  
  try {
    await api.deleteNote(id)
    papers.value = papers.value.filter((p) => p.id !== id)
    
    if (selectedNote.value?.id === id) {
      selectedNote.value = null
      selectedExam.value = null
      questions.value = []
    }
  } catch (error) {
    console.error('Failed to delete note:', error)
  }
}

const selectPaper = async (paper) => {
  selectedNote.value = paper
  selectedExam.value = null
  currentNoteExams.value = []
  userAnswer.value = null
  subjectiveAnswer.value = ''
  answers.value = {}
  score.value = 0
  hasSubmitted.value = false
  questions.value = []
  
  const userId = getCurrentUserId()
  if (!userId) {
    alert('请先登录')
    return
  }

  try {
    // 加载该笔记下的所有考试
    const exams = await api.getExamsByNoteId(paper.id, userId)
    currentNoteExams.value = exams || []
    
    if (exams && exams.length > 0) {
      // 默认选中最新的考试
      const latestExam = exams.sort((a, b) => 
        new Date(b.createTime || b.createdAt || 0) - new Date(a.createTime || a.createdAt || 0)
      )[0]
      await loadExamSession(latestExam.id, { useLatestAttempt: true })
    } else {
      // 没有考试，显示生成配置
      generateConfig.value = {
        name: getNextExamName(),
        questionCount: paper.total || 8,
        difficulty: 'medium',
        selectedTypes: ['single', 'true-false', 'open'],
      }
      showGenerateConfig.value = true
    }
  } catch (error) {
    console.error('Failed to load exams:', error)
    // 如果API不存在，降级到旧逻辑
    try {
      const existingExam = await api.getExamByNoteId(paper.id, userId)
      if (existingExam && existingExam.questions && existingExam.questions.length > 0) {
        currentNoteExams.value = [existingExam]
        await loadExamSession(existingExam.id, { useLatestAttempt: true })
      } else {
        generateConfig.value = {
          name: getNextExamName(),
          questionCount: paper.total || 8,
          difficulty: 'medium',
          selectedTypes: ['single', 'true-false', 'open'],
        }
        showGenerateConfig.value = true
      }
    } catch (e) {
      alert('加载考试失败：' + error.message)
    }
  }
}

const backToList = () => {
  selectedNote.value = null
  selectedExam.value = null
  currentNoteExams.value = []
  currentIndex.value = 0
  userAnswer.value = null
  subjectiveAnswer.value = ''
  answers.value = {}
  score.value = 0
  hasSubmitted.value = false
  questions.value = []
  showGenerateConfig.value = false
}

const selectAnswer = (val) => {
  const qType = currentQuestion.value.type
  if (qType === 'single choice' || qType === 'single_choice' || 
      qType === 'true-false' || qType === 'true_false') {
    userAnswer.value = val
    if (currentQuestion.value.id) {
      answers.value[currentQuestion.value.id] = val
      // 自动保存答案到数据库
      saveAnswerToDatabase(currentQuestion.value.id, val)
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

// 同步主观题答案
const updateSubjectiveAnswer = () => {
  if ((currentQuestion.value.type === 'open question' || currentQuestion.value.type === 'open_question') 
      && currentQuestion.value.id) {
    answers.value[currentQuestion.value.id] = subjectiveAnswer.value
    // 自动保存答案到数据库
    saveAnswerToDatabase(currentQuestion.value.id, subjectiveAnswer.value)
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
    // 转换answers格式为后端需要的格式（questionId -> answer）
    const answersToSubmit = {}
    for (const [questionId, answer] of Object.entries(answers.value)) {
      answersToSubmit[questionId] = answer
    }

    const result = await api.submitExam(examId, userId, answersToSubmit, currentAttemptId.value)
    
    score.value = result.totalScore
    hasSubmitted.value = true
    
    // 重新加载作答记录
    try {
      const attempts = await api.getExamAttempts(examId, userId)
      currentExamAttempts.value = attempts || []
      // 如果有新的attemptId，更新
      if (result.attemptId) {
        currentAttemptId.value = result.attemptId
      }
    } catch (e) {
      // 忽略错误
    }
    
    alert(`Exam submitted! Your score: ${result.totalScore} / ${result.maxScore} pts (${result.percentage.toFixed(1)}%)`)
  } catch (error) {
    console.error('Failed to submit exam:', error)
    alert('提交失败：' + error.message)
  }
}

// 加载考试会话（包括题目和作答记录）
const loadExamSession = async (examId, { attemptId = null, useLatestAttempt = false } = {}) => {
  const userId = getCurrentUserId()
  if (!userId) return

  try {
    // 获取考试详情
    const exam = await api.getExamById(examId)
    if (!exam) {
      alert('考试不存在')
      return
    }

    selectedExam.value = exam
    selectedExamId.value = exam.id
    
    // 加载题目
    if (exam.questions && exam.questions.length > 0) {
      questions.value = formatQuestionsForDisplay(exam.questions)
    } else {
      questions.value = []
    }
    
    currentIndex.value = 0

    // 加载作答记录
    try {
      const attempts = await api.getExamAttempts(examId, userId)
      currentExamAttempts.value = attempts || []
    } catch (e) {
      // 如果API不存在，使用空数组
      currentExamAttempts.value = []
    }

    let targetAttempt = null
    if (attemptId) {
      targetAttempt = currentExamAttempts.value.find(a => a.id === attemptId) || null
    } else if (useLatestAttempt && currentExamAttempts.value.length > 0) {
      targetAttempt = [...currentExamAttempts.value].sort((a, b) => {
        return new Date(b.submitTime || b.createdAt || 0) - new Date(a.submitTime || a.createdAt || 0)
      })[0]
    }

    if (!targetAttempt) {
      // 新作答
      currentAttemptId.value = null
      answers.value = {}
      score.value = 0
      hasSubmitted.value = false
      userAnswer.value = null
      subjectiveAnswer.value = ''
    } else {
      currentAttemptId.value = targetAttempt.id
      // 加载答案
      try {
        const savedAnswers = await api.getExamAnswers(examId, userId, targetAttempt.id)
        answers.value = savedAnswers || {}
      } catch (e) {
        answers.value = targetAttempt.answers || {}
      }
      score.value = targetAttempt.score || 0
      hasSubmitted.value = !!targetAttempt.hasSubmitted

      // 恢复第一题的作答状态
      const firstQ = questions.value[0]
      if (firstQ && firstQ.id && answers.value[firstQ.id]) {
        if (firstQ.type === 'open question' || firstQ.type === 'open_question') {
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
    }
  } catch (error) {
    console.error('Failed to load exam session:', error)
    alert('加载考试失败：' + error.message)
  }
}

// 切换试卷
const onChangeExam = async () => {
  if (!selectedExamId.value) return
  await loadExamSession(selectedExamId.value, { useLatestAttempt: true })
}

// 切换作答记录
const onChangeAttempt = async () => {
  if (!selectedExam.value || !currentAttemptId.value) return
  await loadExamSession(selectedExam.value.id, { attemptId: currentAttemptId.value })
}

// 显示生成配置
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

// 创建新考试
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

  const success = await generateExamWithAI(paper, cfg)
  if (!success) {
    generateQuestionsFromContent(paper.content)
  }
}

// 使用AI生成考试（支持配置）
const generateExamWithAI = async (paper, config) => {
  if (!paper?.content) {
    alert('笔记内容为空，无法生成考试')
    return false
  }

  const userId = getCurrentUserId()
  if (!userId) {
    alert('请先登录')
    return false
  }

  isGeneratingExam.value = true
  generatingExamStatus.value = 'AI is generating questions based on this note...'
  questions.value = []

  try {
    const questionCount = config?.questionCount || paper.total || 8
    const difficulty = config?.difficulty || 'medium'
    const selectedTypes = config?.selectedTypes || ['single', 'true-false', 'open']
    const examName = config?.name || getNextExamName()

    const result = await api.generateExam(
      paper.id,
      userId,
      paper.content,
      questionCount,
      difficulty,
      selectedTypes,
      examName
    )

    if (result && result.questions && result.questions.length > 0) {
      questions.value = formatQuestionsForDisplay(result.questions)
      
      // 重新加载该笔记下的所有考试
      const exams = await api.getExamsByNoteId(paper.id, userId)
      currentNoteExams.value = exams || []
      
      // 选中新创建的考试
      if (result.exam && result.exam.id) {
        selectedExamId.value = result.exam.id
        await loadExamSession(result.exam.id, { useLatestAttempt: false })
      }
      
      generatingExamStatus.value = `AI generated ${questions.value.length} questions.`
      return true
    } else {
      throw new Error('AI未返回任何题目')
    }
  } catch (error) {
    console.error('Error generating exam with AI:', error)
    alert('AI生成考试失败：' + error.message)
    return false
  } finally {
    isGeneratingExam.value = false
  }
}

// 删除考试
const deleteExam = async (examId) => {
  if (!selectedNote.value) return
  if (!confirm('确定要删除这套测试吗？此操作无法撤销，但不会删除笔记本身。')) return

  const userId = getCurrentUserId()
  if (!userId) {
    alert('请先登录')
    return
  }

  try {
    await api.deleteExam(examId, userId)
    
    // 重新加载该笔记下的所有考试
    const exams = await api.getExamsByNoteId(selectedNote.value.id, userId)
    currentNoteExams.value = exams || []

    // 如果删掉的是当前正在看的试卷，则切换到最新一份或清空
    if (selectedExam.value?.id === examId) {
      if (currentNoteExams.value.length > 0) {
        const latestExam = currentNoteExams.value.sort((a, b) => 
          new Date(b.createTime || b.createdAt || 0) - new Date(a.createTime || a.createdAt || 0)
        )[0]
        await loadExamSession(latestExam.id, { useLatestAttempt: true })
      } else {
        selectedExam.value = null
        questions.value = []
        currentIndex.value = 0
        answers.value = {}
        score.value = 0
        hasSubmitted.value = false
        showGenerateConfig.value = true
      }
    }
  } catch (error) {
    console.error('Failed to delete exam:', error)
    alert('删除失败：' + error.message)
  }
}

// 重新答当前试卷
const redoCurrentExam = async () => {
  if (!selectedExam.value) return
  await loadExamSession(selectedExam.value.id, { attemptId: null, useLatestAttempt: false })
}

// 格式化题目为前端显示格式
const formatQuestionsForDisplay = (questionsData) => {
  return questionsData.map((q, idx) => {
    // 确定题目类型
    let questionType = q.type || 'single choice'
    if (questionType === 'open' || questionType === 'open_question') {
      questionType = 'open question'
    } else if (questionType === 'true-false' || questionType === 'true_false') {
      questionType = 'true-false'
    } else if (questionType === 'single' || questionType === 'single_choice') {
      questionType = 'single choice'
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

// 保存答案到数据库
const saveAnswerToDatabase = async (questionId, answer) => {
  const userId = getCurrentUserId()
  if (!userId || !selectedExam.value?.id) {
    return
  }

  try {
    const answersToSave = { [questionId]: answer }
    await api.saveExamAnswers(selectedExam.value.id, userId, answersToSave, currentAttemptId.value)
  } catch (error) {
    console.error('Failed to save answer:', error)
    // 静默失败，不打扰用户
  }
}

// 简单的本地生成器（作为后备）
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

.paper-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
}

.paper-main {
  flex: 1;
  min-width: 0;
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

.menu-wrapper {
  position: relative;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: background-color 0.2s;
  flex-shrink: 0;
}

.menu-wrapper:hover {
  background-color: #f2f4f7;
}

.menu-dot {
  width: 4px;
  height: 4px;
  background: #666;
  border-radius: 50%;
  margin: 1px 0;
}

.paper-menu {
  position: absolute;
  top: 32px;
  right: 0;
  background: #fff;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.08);
  padding: 6px 0;
  min-width: 120px;
  z-index: 10;
}

.menu-item {
  width: 100%;
  padding: 8px 12px;
  text-align: left;
  background: none;
  border: none;
  font-size: 13px;
  color: #333;
  cursor: pointer;
}

.menu-item:hover {
  background-color: #f5f7fa;
}

.menu-item.danger {
  color: #e53935;
}

.menu-item.danger:hover {
  background-color: #fff2f1;
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
  font-weight: 500;
}

.explanation-text {
  line-height: 1.6;
  color: #555;
}

.explanation-text.muted {
  color: #999;
}

.answer-actions {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
}

.question-footer-actions {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
  margin-top: 12px;
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

/* 弹窗样式 */
.modal-mask {
  position: fixed;
  inset: 0;
  background-color: rgba(0, 0, 0, 0.25);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 40;
}

.modal-panel {
  background-color: #fff;
  border-radius: 12px;
  padding: 20px 24px;
  box-shadow: 0 16px 40px rgba(0, 0, 0, 0.18);
  width: 420px;
  max-width: 90%;
}

.modal-title {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 12px;
  color: #333;
}

.modal-field {
  margin-bottom: 16px;
}

.modal-label {
  display: block;
  margin-bottom: 6px;
  font-size: 13px;
  font-weight: 500;
  color: #555;
}

.modal-input {
  width: 100%;
  padding: 9px 12px;
  border-radius: 6px;
  border: 1px solid #ddd;
  font-size: 14px;
  font-family: inherit;
}

.modal-input:focus {
  outline: none;
  border-color: #1976d2;
  box-shadow: 0 0 0 2px rgba(25, 118, 210, 0.15);
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 16px;
}

.modal-btn {
  padding: 8px 16px;
  border-radius: 6px;
  border: 1px solid #ddd;
  background-color: #fff;
  cursor: pointer;
  font-size: 14px;
}

.modal-btn.primary {
  background-color: #1976d2;
  border-color: #1976d2;
  color: #fff;
}

.modal-btn.primary:hover {
  background-color: #1565c0;
  border-color: #1565c0;
}

.modal-btn:not(.primary):hover {
  border-color: #bbb;
}
</style>
