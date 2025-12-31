<template>
  <div class="exam-page">
    
    <div v-if="!selectedSubject" class="paper-list">
      <h2 class="page-title">Select subject</h2>
      <p class="page-subtitle">Choose a subject to view its notes and exams.</p>

      <div class="subjects-container">
        
        <div v-if="personalSubjects.length > 0" class="subjects-section">
          <h3 class="section-title">Personal Subjects</h3>
          <div class="papers-grid">
            <div
              v-for="subject in personalSubjects"
              :key="subject.id"
              class="paper-card"
              @click="selectSubject(subject)"
            >
          <div class="paper-header">
            <div class="paper-main">
              <div class="paper-title">{{ subject.name }}</div>
              <div class="paper-desc">{{ subject.description || 'No description' }}</div>
            </div>
          </div>
          <div class="paper-meta">
            <span>Notes: {{ subject.notes?.length || 0 }}</span>
            <span>Updated: {{ formatDate(subject.updatedAt || subject.createTime) }}</span>
          </div>
        </div>
          </div>
        </div>

        <div v-if="groupSubjects.length > 0" class="subjects-section group-section">
          <h3 class="section-title">Group Subjects</h3>
          <div class="papers-grid">
            <div
              v-for="subject in groupSubjects"
              :key="subject.id"
              class="paper-card"
              @click="selectSubject(subject)"
            >
              <div class="paper-header">
                <div class="paper-main">
                  <div class="paper-title-row">
                    <div class="paper-title">{{ subject.name }}</div>
                    <span class="visibility-badge group-badge">Group</span>
                  </div>
                  <div class="paper-desc">{{ subject.description || 'No description' }}</div>
                </div>
              </div>
              <div class="paper-meta">
                <span>Notes: {{ subject.notes?.length || 0 }}</span>
                <span>Updated: {{ formatDate(subject.updatedAt || subject.createTime) }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div v-else-if="!selectedNote" class="paper-list">
      <div class="breadcrumb-bar">
        <button class="nav-btn" @click="backToSubjects">← Subjects</button>
        <span class="breadcrumb-text">{{ selectedSubject?.name }}</span>
      </div>
      <h2 class="page-title">Select note</h2>
      <p class="page-subtitle">Choose a note under this subject to view or generate exams.</p>

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

      <div v-if="showGenerateConfig" class="question-section">
        <div class="question-box">
          <div class="question-header">
            <div class="question-type">Generate exam</div>
          </div>
          <div class="question-body">
            <div class="generate-form">
              <label class="form-label-item">
                Test name:
                <input
                  v-model="generateConfig.name"
                  type="text"
                  class="form-input-styled"
                />
              </label>
              <label class="form-label-item">
                Question count:
                <input
                  v-model.number="generateConfig.questionCount"
                  type="number"
                  min="1"
                  max="50"
                  class="form-input-styled form-input-number"
                />
              </label>
              <label class="form-label-item">
                Difficulty:
                <select
                  v-model="generateConfig.difficulty"
                  class="form-select-styled"
                >
                  <option value="easy">Easy</option>
                  <option value="medium">Medium</option>
                  <option value="hard">Hard</option>
                </select>
              </label>
              <div class="checkbox-group">
                <div class="checkbox-group-label">Question types:</div>
                <div class="checkbox-items">
                  <label class="checkbox-item">
                    <input
                      type="checkbox"
                      value="single"
                      v-model="generateConfig.selectedTypes"
                      class="checkbox-input"
                    />
                    Single choice
                  </label>
                  <label class="checkbox-item">
                    <input
                      type="checkbox"
                      value="true-false"
                      v-model="generateConfig.selectedTypes"
                      class="checkbox-input"
                    />
                    True / False
                  </label>
                  <label class="checkbox-item">
                    <input
                      type="checkbox"
                      value="open"
                      v-model="generateConfig.selectedTypes"
                      class="checkbox-input"
                    />
                    Open question
                  </label>
                </div>
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

const personalSubjects = ref([])
const groupSubjects = ref([])
const papers = ref([]) 
const loading = ref(false)
const activeMenuId = ref(null)

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
  return null 
}

const loadSubjects = async () => {
  loading.value = true
  try {
    const userId = getCurrentUserId()
    if (!userId) {
      alert('请先登录')
      loading.value = false
      return
    }
    const separated = await api.getSubjectsSeparated(userId)
    personalSubjects.value = separated.personal || []
    groupSubjects.value = separated.group || []
  } catch (error) {
    console.error('Failed to load subjects:', error)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadSubjects()
})

const selectedSubject = ref(null)
const selectedNote = ref(null)

const currentNoteExams = ref([])

const selectedExam = ref(null)
const selectedExamId = ref(null)

const currentAttemptId = ref(null)

const currentExamAttempts = ref([])

const currentIndex = ref(0)
const score = ref(0)
const userAnswer = ref(null)
const subjectiveAnswer = ref('')
const answers = ref({}) 
const hasSubmitted = ref(false)

const questions = ref([])

const isGeneratingExam = ref(false)
const generatingExamStatus = ref('')

const showGenerateConfig = ref(false)
const generateConfig = ref({
  name: '测试1',
  questionCount: 8,
  difficulty: 'medium',
  selectedTypes: ['single', 'true-false', 'open'],
})

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

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return date.toLocaleDateString()
}

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

const formatAttemptLabel = (attempt, idx) => {
  const attemptNum = currentExamAttempts.value.length - idx
  const date = attempt.submitTime || attempt.createdAt ? formatDate(attempt.submitTime || attempt.createdAt) : ''
  const scoreText = typeof attempt.score === 'number' && attempt.hasSubmitted
    ? `Score: ${attempt.score}`
    : 'Not submitted'
  return `Attempt ${attemptNum} - ${scoreText}${date ? ' (' + date + ')' : ''}`
}

const toggleMenu = (id) => {
  activeMenuId.value = activeMenuId.value === id ? null : id
}

const showNoteDialog = ref(false)
const dialogNoteName = ref('')
const dialogNote = ref(null)

const mapNotesFromSubject = (subject) => {
  if (!subject) return []
  return (subject.notes || []).map(note => ({
    id: note.id,
    title: note.title,
    desc: subject.name,
    total: note.total || 5,
    updatedAt: note.updatedAt,
    content: note.content
  }))
}

const selectSubject = (subject) => {
  selectedSubject.value = subject
  selectedNote.value = null
  papers.value = mapNotesFromSubject(subject)
  activeMenuId.value = null
}

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
    
    const index = papers.value.findIndex(p => p.id === dialogNote.value.id)
    if (index !== -1) {
      papers.value[index] = {
        ...papers.value[index],
        title: updated.title
      }
    }
    if (selectedSubject.value) {
      selectedSubject.value = {
        ...selectedSubject.value,
        notes: (selectedSubject.value.notes || []).map(n => n.id === dialogNote.value.id ? { ...n, title: updated.title } : n)
      }
      
      const personalIndex = personalSubjects.value.findIndex(s => s.id === selectedSubject.value.id)
      if (personalIndex !== -1) {
        personalSubjects.value[personalIndex] = selectedSubject.value
      } else {
        const groupIndex = groupSubjects.value.findIndex(s => s.id === selectedSubject.value.id)
        if (groupIndex !== -1) {
          groupSubjects.value[groupIndex] = selectedSubject.value
        }
      }
    }
    
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
    if (selectedSubject.value) {
      selectedSubject.value = {
        ...selectedSubject.value,
        notes: (selectedSubject.value.notes || []).filter(n => n.id !== id)
      }
      
      const personalIndex = personalSubjects.value.findIndex(s => s.id === selectedSubject.value.id)
      if (personalIndex !== -1) {
        personalSubjects.value[personalIndex] = selectedSubject.value
      } else {
        const groupIndex = groupSubjects.value.findIndex(s => s.id === selectedSubject.value.id)
        if (groupIndex !== -1) {
          groupSubjects.value[groupIndex] = selectedSubject.value
        }
      }
    }
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
    
    const exams = await api.getExamsByNoteId(paper.id, userId)
    currentNoteExams.value = exams || []
    
    if (exams && exams.length > 0) {
      
      const latestExam = exams.sort((a, b) => 
        new Date(b.createTime || b.createdAt || 0) - new Date(a.createTime || a.createdAt || 0)
      )[0]
      await loadExamSession(latestExam.id, { useLatestAttempt: true })
    } else {
      
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

const backToSubjects = () => {
  backToList()
  selectedSubject.value = null
  papers.value = []
}

const selectAnswer = (val) => {
  const qType = currentQuestion.value.type
  if (qType === 'single choice' || qType === 'single_choice' || 
      qType === 'true-false' || qType === 'true_false') {
    userAnswer.value = val
    if (currentQuestion.value.id) {
      answers.value[currentQuestion.value.id] = val
      
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

const updateSubjectiveAnswer = () => {
  if ((currentQuestion.value.type === 'open question' || currentQuestion.value.type === 'open_question') 
      && currentQuestion.value.id) {
    answers.value[currentQuestion.value.id] = subjectiveAnswer.value
    
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
    
    const answersToSubmit = {}
    for (const [questionId, answer] of Object.entries(answers.value)) {
      answersToSubmit[questionId] = answer
    }

    const result = await api.submitExam(examId, userId, answersToSubmit, currentAttemptId.value)
    
    score.value = result.totalScore
    hasSubmitted.value = true

    try {
      const attempts = await api.getExamAttempts(examId, userId)
      currentExamAttempts.value = attempts || []
      
      if (result.attemptId) {
        currentAttemptId.value = result.attemptId
      }
    } catch (e) {
      
    }
    
    alert(`Exam submitted! Your score: ${result.totalScore} / ${result.maxScore} pts (${result.percentage.toFixed(1)}%)`)
  } catch (error) {
    console.error('Failed to submit exam:', error)
    alert('提交失败：' + error.message)
  }
}

const loadExamSession = async (examId, { attemptId = null, useLatestAttempt = false } = {}) => {
  const userId = getCurrentUserId()
  if (!userId) return

  try {
    
    const exam = await api.getExamById(examId)
    if (!exam) {
      alert('考试不存在')
      return
    }

    selectedExam.value = exam
    selectedExamId.value = exam.id

    if (exam.questions && exam.questions.length > 0) {
      questions.value = formatQuestionsForDisplay(exam.questions)
    } else {
      questions.value = []
    }
    
    currentIndex.value = 0

    try {
      const attempts = await api.getExamAttempts(examId, userId)
      currentExamAttempts.value = attempts || []
    } catch (e) {
      
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
      
      currentAttemptId.value = null
      answers.value = {}
      score.value = 0
      hasSubmitted.value = false
      userAnswer.value = null
      subjectiveAnswer.value = ''
    } else {
      currentAttemptId.value = targetAttempt.id
      
      try {
        const savedAnswers = await api.getExamAnswers(examId, userId, targetAttempt.id)
        answers.value = savedAnswers || {}
      } catch (e) {
        answers.value = targetAttempt.answers || {}
      }
      score.value = targetAttempt.score || 0
      hasSubmitted.value = !!targetAttempt.hasSubmitted

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

const onChangeExam = async () => {
  if (!selectedExamId.value) return
  await loadExamSession(selectedExamId.value, { useLatestAttempt: true })
}

const onChangeAttempt = async () => {
  if (!selectedExam.value || !currentAttemptId.value) return
  await loadExamSession(selectedExam.value.id, { attemptId: currentAttemptId.value })
}

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

const createNewExamWithAI = async () => {
  if (!selectedNote.value) return
  const paper = selectedNote.value
  const cfg = generateConfig.value

  showGenerateConfig.value = false

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

      const exams = await api.getExamsByNoteId(paper.id, userId)
      currentNoteExams.value = exams || []

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

    const exams = await api.getExamsByNoteId(selectedNote.value.id, userId)
    currentNoteExams.value = exams || []

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

const redoCurrentExam = async () => {
  if (!selectedExam.value) return
  await loadExamSession(selectedExam.value.id, { attemptId: null, useLatestAttempt: false })
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
    
  }
}

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
  font-size: 24px;
  font-weight: 700;
  background: linear-gradient(135deg, #1976D2 0%, #2196F3 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  margin-bottom: 8px;
  letter-spacing: -0.5px;
}

.page-subtitle {
  color: #546E7A;
  margin-bottom: 24px;
  font-size: 14px;
  font-weight: 400;
}

.subjects-container {
  display: flex;
  flex-direction: column;
  gap: 32px;
}

.subjects-section {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.group-section {
  margin-top: 8px;
  padding-top: 32px;
  border-top: 2px solid rgba(144, 202, 249, 0.3);
}

.section-title {
  font-size: 18px;
  font-weight: 600;
  color: #1565C0;
  margin-bottom: 4px;
  letter-spacing: -0.3px;
}

.papers-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 16px;
}

.breadcrumb-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.breadcrumb-text {
  font-weight: 700;
  color: #1565C0;
  font-size: 14px;
}

.paper-card {
  background: linear-gradient(to bottom, #FFFFFF 0%, #F8FBFF 100%);
  padding: 24px;
  border-radius: 16px;
  box-shadow: 0 4px 12px rgba(33, 150, 243, 0.08);
  cursor: pointer;
  transition: all 0.3s ease;
  border: 2px solid rgba(144, 202, 249, 0.8);
  display: flex;
  flex-direction: column;
  gap: 12px;
  position: relative;
  overflow: hidden;
}

.paper-card:hover {
  border-color: #2196F3;
  box-shadow: 0 8px 24px rgba(33, 150, 243, 0.25), 0 0 0 3px rgba(33, 150, 243, 0.15);
  transform: translateY(-2px);
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
  font-size: 18px;
  font-weight: 600;
  color: #1565C0;
  margin-bottom: 4px;
}

.paper-title-row {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 4px;
}

.visibility-badge {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 12px;
  font-weight: 500;
}

.visibility-badge.group-badge {
  background: linear-gradient(135deg, #2196F3 0%, #1976D2 100%);
  color: white;
}

.paper-desc {
  font-size: 14px;
  color: #546E7A;
  font-weight: 400;
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
  padding: 8px 12px;
  border-radius: 8px;
  border: 2px solid rgba(187, 222, 251, 0.6);
  font-size: 13px;
  background: linear-gradient(to bottom, #FFFFFF 0%, #F8FBFF 100%);
  transition: all 0.3s ease;
  cursor: pointer;
}

.exam-switcher-select:focus {
  outline: none;
  border-color: #64B5F6;
  box-shadow: 0 0 0 3px rgba(33, 150, 243, 0.1);
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
  font-size: 20px;
  font-weight: 700;
  color: #1565C0;
  letter-spacing: -0.3px;
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
  padding: 6px 10px;
  border-radius: 8px;
  border: 2px solid rgba(187, 222, 251, 0.6);
  background: linear-gradient(to bottom, #FFFFFF 0%, #F8FBFF 100%);
  transition: all 0.3s ease;
  cursor: pointer;
}

.attempt-select:focus {
  outline: none;
  border-color: #64B5F6;
  box-shadow: 0 0 0 3px rgba(33, 150, 243, 0.1);
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

.exam-generating-banner {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 20px;
  border-radius: 12px;
  background: linear-gradient(135deg, #E3F2FD 0%, #BBDEFB 100%);
  border: 1px solid rgba(144, 202, 249, 0.6);
  font-size: 14px;
  box-shadow: 0 2px 8px rgba(33, 150, 243, 0.1);
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
  gap: 16px;
  border: 2px dashed rgba(144, 202, 249, 0.5);
  border-radius: 12px;
  padding: 24px;
  background: rgba(255, 255, 255, 0.6);
  transition: all 0.3s ease;
}

.question-box:hover {
  border-color: rgba(144, 202, 249, 0.8);
  background: rgba(255, 255, 255, 0.8);
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

.exam-secondary-btn {
  padding: 8px 16px;
  border-radius: 8px;
  border: 2px solid rgba(187, 222, 251, 0.8);
  background: linear-gradient(to bottom, #FFFFFF 0%, #F8FBFF 100%);
  cursor: pointer;
  font-size: 13px;
  color: #546E7A;
  transition: all 0.3s ease;
  font-weight: 500;
}

.exam-secondary-btn:hover {
  background: linear-gradient(to bottom, #E3F2FD 0%, #BBDEFB 100%);
  border-color: #64B5F6;
  color: #1565C0;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(33, 150, 243, 0.15);
}

.exam-danger-btn {
  padding: 8px 16px;
  border-radius: 8px;
  border: 2px solid rgba(255, 205, 210, 0.8);
  background: linear-gradient(to bottom, #FFEBEE 0%, rgba(255, 205, 210, 0.4) 100%);
  cursor: pointer;
  font-size: 13px;
  color: #C62828;
  transition: all 0.3s ease;
  font-weight: 500;
}

.exam-danger-btn:hover {
  background: linear-gradient(to bottom, #FFCDD2 0%, #FFEBEE 100%);
  border-color: #E53935;
  color: #B71C1C;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(244, 67, 54, 0.2);
}

.modal-mask {
  position: fixed;
  inset: 0;
  background-color: rgba(33, 150, 243, 0.2);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 40;
}

.modal-panel {
  background: linear-gradient(to bottom, #FFFFFF 0%, #F8FBFF 100%);
  border-radius: 16px;
  padding: 28px 32px;
  box-shadow: 0 20px 60px rgba(33, 150, 243, 0.25);
  width: 420px;
  max-width: 90%;
  border: 1px solid rgba(187, 222, 251, 0.6);
}

.modal-title {
  font-size: 18px;
  font-weight: 700;
  margin-bottom: 16px;
  color: #1565C0;
  letter-spacing: -0.3px;
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
  padding: 10px 14px;
  border-radius: 10px;
  border: 2px solid rgba(187, 222, 251, 0.6);
  font-size: 14px;
  font-family: inherit;
  background: rgba(255, 255, 255, 0.9);
  transition: all 0.3s ease;
}

.modal-input:focus {
  outline: none;
  border-color: #64B5F6;
  box-shadow: 0 0 0 4px rgba(33, 150, 243, 0.1);
  background: #FFFFFF;
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 16px;
}

.modal-btn {
  padding: 10px 20px;
  border-radius: 10px;
  border: 2px solid rgba(187, 222, 251, 0.8);
  background-color: #FFFFFF;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.3s ease;
}

.modal-btn.primary {
  background: linear-gradient(135deg, #2196F3 0%, #1976D2 100%);
  border-color: #2196F3;
  color: #fff;
  box-shadow: 0 4px 12px rgba(33, 150, 243, 0.3);
}

.modal-btn.primary:hover {
  background: linear-gradient(135deg, #1976D2 0%, #1565C0 100%);
  border-color: #1976D2;
  box-shadow: 0 6px 16px rgba(33, 150, 243, 0.4);
  transform: translateY(-1px);
}

.modal-btn:not(.primary):hover {
  border-color: #64B5F6;
  background: linear-gradient(to right, #F0F7FF 0%, rgba(227, 242, 253, 0.5) 100%);
  color: #1976D2;
  transform: translateY(-1px);
}

.generate-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
  max-width: 480px;
}

.form-label-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
  font-size: 14px;
  color: #546E7A;
  font-weight: 500;
}

.form-input-styled {
  width: 100%;
  padding: 10px 14px;
  border: 2px solid rgba(187, 222, 251, 0.6);
  border-radius: 10px;
  font-size: 14px;
  background: rgba(255, 255, 255, 0.9);
  transition: all 0.3s ease;
  font-family: inherit;
}

.form-input-styled:focus {
  outline: none;
  border-color: #64B5F6;
  box-shadow: 0 0 0 4px rgba(33, 150, 243, 0.1);
  background: #FFFFFF;
}

.form-input-number {
  width: 120px;
}

.form-select-styled {
  width: 160px;
  padding: 10px 14px;
  border: 2px solid rgba(187, 222, 251, 0.6);
  border-radius: 10px;
  font-size: 14px;
  background: linear-gradient(to bottom, #FFFFFF 0%, #F8FBFF 100%);
  transition: all 0.3s ease;
  cursor: pointer;
  font-family: inherit;
}

.form-select-styled:focus {
  outline: none;
  border-color: #64B5F6;
  box-shadow: 0 0 0 4px rgba(33, 150, 243, 0.1);
}

.checkbox-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.checkbox-group-label {
  font-size: 14px;
  color: #546E7A;
  font-weight: 500;
  margin-bottom: 4px;
}

.checkbox-items {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
}

.checkbox-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: #546E7A;
  cursor: pointer;
  transition: color 0.3s ease;
}

.checkbox-item:hover {
  color: #1976D2;
}

.checkbox-input {
  width: 18px;
  height: 18px;
  cursor: pointer;
  accent-color: #2196F3;
}
</style>
