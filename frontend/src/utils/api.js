const API_BASE = '/api'

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return date.toISOString().split('T')[0]
}

export const transformSubject = (subject) => {
  return {
    id: subject.id,
    name: subject.name,
    description: subject.description || '',
    visibility: subject.visibility || 'private',
    userId: subject.userId || subject.user_id,
    creatorUsername: subject.creatorUsername || subject.creator_username,
    notes: (subject.notes || []).map(transformNote)
  }
}

export const transformNote = (note) => {
  return {
    id: note.id,
    title: note.title,
    content: note.content || '',
    subjectId: note.subjectId,
    updatedAt: formatDate(note.updateTime || note.updatedAt)
  }
}

export const api = {
  
  getSubjects: async (userId) => {
    const res = await fetch(`${API_BASE}/subjects?userId=${userId}`)
    const data = await res.json()
    if (data.success && data.data) {
      return data.data.map(transformSubject)
    }
    return []
  },

  getSubjectsSeparated: async (userId) => {
    const res = await fetch(`${API_BASE}/subjects?userId=${userId}&separated=true`)
    const data = await res.json()
    if (data.success && data.data) {
      return {
        personal: (data.data.personal || []).map(transformSubject),
        group: (data.data.group || []).map(transformSubject)
      }
    }
    return { personal: [], group: [] }
  },

  getSubjectById: async (id) => {
    const res = await fetch(`${API_BASE}/subjects/${id}`)
    const data = await res.json()
    if (data.success && data.data) {
      return transformSubject(data.data)
    }
    return null
  },

  createSubject: async (subject) => {
    const res = await fetch(`${API_BASE}/subjects`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(subject)
    })
    const data = await res.json()
    if (data.success && data.data) {
      return transformSubject(data.data)
    }
    throw new Error(data.message || '创建失败')
  },

  updateSubject: async (id, subject) => {
    const res = await fetch(`${API_BASE}/subjects/${id}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(subject)
    })
    const data = await res.json()
    if (data.success && data.data) {
      return transformSubject(data.data)
    }
    throw new Error(data.message || '更新失败')
  },

  deleteSubject: async (id) => {
    const res = await fetch(`${API_BASE}/subjects/${id}`, {
      method: 'DELETE'
    })
    const data = await res.json()
    if (!data.success) {
      throw new Error(data.message || '删除失败')
    }
  },

  cloneSubject: async (subjectId, userId) => {
    const res = await fetch(`${API_BASE}/subjects/${subjectId}/clone?userId=${userId}`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' }
    })
    const data = await res.json()
    if (data.success && data.data) {
      return transformSubject(data.data)
    }
    throw new Error(data.message || '克隆失败')
  },

  getNoteById: async (id) => {
    const res = await fetch(`${API_BASE}/notes/${id}`)
    const data = await res.json()
    if (data.success && data.data) {
      return transformNote(data.data)
    }
    return null
  },

  createNote: async (note) => {
    const res = await fetch(`${API_BASE}/notes`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(note)
    })
    const data = await res.json()
    if (data.success && data.data) {
      return transformNote(data.data)
    }
    throw new Error(data.message || '创建失败')
  },

  updateNote: async (id, note) => {
    const res = await fetch(`${API_BASE}/notes/${id}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(note)
    })
    const data = await res.json()
    if (data.success && data.data) {
      return transformNote(data.data)
    }
    throw new Error(data.message || '更新失败')
  },

  deleteNote: async (id) => {
    const res = await fetch(`${API_BASE}/notes/${id}`, {
      method: 'DELETE'
    })
    const data = await res.json()
    if (!data.success) {
      throw new Error(data.message || '删除失败')
    }
  },

  uploadAndCreateNote: async (subjectId, noteName, files, userId) => {
    const formData = new FormData()
    formData.append('subjectId', subjectId)
    formData.append('noteName', noteName)
    formData.append('userId', userId)
    if (files && files.length > 0) {
      Array.from(files).forEach(file => {
        formData.append('files', file)
      })
    }

    const res = await fetch(`${API_BASE}/notes/upload`, {
      method: 'POST',
      body: formData
    })
    const data = await res.json()
    if (data.success && data.data) {
      return transformNote(data.data)
    }
    throw new Error(data.message || '上传失败')
  },

  getUserProfile: async (userId) => {
    const res = await fetch(`${API_BASE}/users/profile?userId=${userId}`)
    const data = await res.json()
    if (data.success && data.user) {
      return data.user
    }
    throw new Error(data.message || '获取用户信息失败')
  },

  updateUserProfile: async (userId, userInfo) => {
    const res = await fetch(`${API_BASE}/users/profile?userId=${userId}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(userInfo)
    })
    const data = await res.json()
    if (data.success && data.user) {
      return data.user
    }
    throw new Error(data.message || '更新用户信息失败')
  },

  searchPublicSubjects: async (keyword) => {
    if (!keyword || !keyword.trim()) {
      return []
    }
    const res = await fetch(`${API_BASE}/subjects/search?keyword=${encodeURIComponent(keyword.trim())}`)
    const data = await res.json()
    if (data.success && data.data) {
      return data.data.map(transformSubject)
    }
    return []
  },

  getPublicSubjects: async () => {
    try {
      const res = await fetch(`${API_BASE}/subjects?visibility=public`)
      if (!res.ok) {
        const errorData = await res.json().catch(() => ({ message: `HTTP ${res.status}` }))
        throw new Error(errorData.message || `HTTP ${res.status}`)
      }
      const data = await res.json()
      if (data.success && data.data) {
        return data.data.map(subject => ({
          ...transformSubject(subject),
          creatorUsername: subject.creatorUsername || ''
        }))
      }
      return []
    } catch (error) {
      console.error('Failed to fetch public subjects:', error)
      throw error
    }
  },

  getUserGroups: async (userId) => {
    const res = await fetch(`${API_BASE}/groups?userId=${userId}`)
    const data = await res.json()
    if (data.success && data.data) {
      return data.data.map(group => ({
        id: group.id,
        name: group.name,
        creatorId: group.creatorId,
        memberCount: group.memberIds ? group.memberIds.length : 0,
        createTime: formatDate(group.createTime),
        updateTime: formatDate(group.updateTime)
      }))
    }
    return []
  },

  getGroupSharedSubject: async (groupId) => {
    try {
      const res = await fetch(`${API_BASE}/groups/${groupId}/subject`)
      const data = await res.json()
      if (data.success && data.data) {
        return transformSubject(data.data)
      }
      return null
    } catch (error) {
      console.error('Failed to get group shared subject:', error)
      return null
    }
  },

  createGroupSharedSubject: async (groupId, subject) => {
    try {
      const res = await fetch(`${API_BASE}/groups/${groupId}/subject`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(subject)
      })
      const data = await res.json()
      if (data.success && data.data) {
        return transformSubject(data.data)
      }
      throw new Error(data.message || '创建失败')
    } catch (error) {
      console.error('Failed to create group shared subject:', error)
      throw error
    }
  },

  uploadAvatar: async (userId, file) => {
    const formData = new FormData()
    formData.append('file', file)
    formData.append('userId', userId.toString())

    const res = await fetch(`${API_BASE}/users/avatar?userId=${userId}`, {
      method: 'POST',
      body: formData
    })

    const data = await res.json()
    if (data.success && data.avatarUrl) {
      return data.avatarUrl
    }
    throw new Error(data.message || '上传头像失败')
  },

  getExamByNoteId: async (noteId, userId) => {
    const res = await fetch(`${API_BASE}/exams?noteId=${noteId}&userId=${userId}`)
    const data = await res.json()
    if (data.success && data.data) {
      return data.data
    }
    return null
  },

  getExamsByNoteId: async (noteId, userId) => {
    const res = await fetch(`${API_BASE}/exams?noteId=${noteId}&userId=${userId}&all=true`)
    const data = await res.json()
    if (data.success && data.data) {
      return Array.isArray(data.data) ? data.data : [data.data]
    }
    return []
  },

  getExamById: async (examId) => {
    const res = await fetch(`${API_BASE}/exams/${examId}`)
    const data = await res.json()
    if (data.success && data.data) {
      return data.data
    }
    throw new Error(data.message || '获取考试失败')
  },

  generateExam: async (noteId, userId, noteContent, questionCount = 8, difficulty = 'medium', selectedTypes = ['single', 'true-false', 'open'], examName = null) => {
    const res = await fetch(`${API_BASE}/exams/generate`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        noteId,
        userId,
        noteContent,
        questionCount,
        difficulty,
        selectedTypes,
        examName
      })
    })
    const data = await res.json()
    if (data.success && data.questions) {
      return {
        exam: data.data,
        questions: data.questions
      }
    }
    throw new Error(data.message || '生成考试失败')
  },

  deleteExam: async (examId, userId) => {
    const res = await fetch(`${API_BASE}/exams/${examId}?userId=${userId}`, {
      method: 'DELETE'
    })
    const data = await res.json()
    if (!data.success) {
      throw new Error(data.message || '删除考试失败')
    }
  },

  getExamAttempts: async (examId, userId) => {
    const res = await fetch(`${API_BASE}/exams/${examId}/attempts?userId=${userId}`)
    const data = await res.json()
    if (data.success && data.data) {
      return Array.isArray(data.data) ? data.data : []
    }
    return []
  },

  getExamAnswers: async (examId, userId, attemptId) => {
    const res = await fetch(`${API_BASE}/exams/${examId}/answers?userId=${userId}&attemptId=${attemptId || ''}`)
    const data = await res.json()
    if (data.success && data.data) {
      
      const answers = {}
      if (Array.isArray(data.data)) {
        data.data.forEach(item => {
          answers[item.questionId] = item.answer
        })
      }
      return answers
    }
    return {}
  },

  saveExamAnswers: async (examId, userId, answers, attemptId = null) => {
    const res = await fetch(`${API_BASE}/exams/${examId}/answers?userId=${userId}${attemptId ? `&attemptId=${attemptId}` : ''}`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ answers })
    })
    const data = await res.json()
    if (!data.success) {
      throw new Error(data.message || '保存答案失败')
    }
    
    return data.attemptId || null
  },

  submitExam: async (examId, userId, answers, attemptId = null) => {
    const res = await fetch(`${API_BASE}/exams/${examId}/submit?userId=${userId}${attemptId ? `&attemptId=${attemptId}` : ''}`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ answers })
    })
    const data = await res.json()
    if (data.success) {
      return {
        totalScore: data.totalScore,
        maxScore: data.maxScore,
        percentage: data.percentage,
        attemptId: data.attemptId || attemptId
      }
    }
    throw new Error(data.message || '提交考试失败')
  },

  getReviewExams: async (userId, limit = 5) => {
    const res = await fetch(`${API_BASE}/exams/review?userId=${userId}&limit=${limit}`)
    const data = await res.json()
    if (!data.success) {
      console.error('获取复习试题失败:', data.message, data.error)
      throw new Error(data.message || '获取复习试题失败')
    }
    return data.data || []
  }
}

export const findNoteById = async (noteId) => {
  try {
    const note = await api.getNoteById(noteId)
    if (!note) {
      return { subject: null, note: null }
    }

    return { subject: null, note }
  } catch (error) {
    console.error('Error finding note:', error)
    return { subject: null, note: null }
  }
}

