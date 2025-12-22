const API_BASE = '/api'

// 格式化日期为前端需要的格式
const formatDate = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return date.toISOString().split('T')[0]
}

// 将后端数据转换为前端格式
export const transformSubject = (subject) => {
  return {
    id: subject.id,
    name: subject.name,
    description: subject.description || '',
    visibility: subject.visibility || 'private',
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
  // 获取所有科目
  getSubjects: async (userId) => {
    const res = await fetch(`${API_BASE}/subjects?userId=${userId}`)
    const data = await res.json()
    if (data.success && data.data) {
      return data.data.map(transformSubject)
    }
    return []
  },

  // 获取科目详情
  getSubjectById: async (id) => {
    const res = await fetch(`${API_BASE}/subjects/${id}`)
    const data = await res.json()
    if (data.success && data.data) {
      return transformSubject(data.data)
    }
    return null
  },

  // 创建科目
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

  // 更新科目
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

  // 删除科目
  deleteSubject: async (id) => {
    const res = await fetch(`${API_BASE}/subjects/${id}`, {
      method: 'DELETE'
    })
    const data = await res.json()
    if (!data.success) {
      throw new Error(data.message || '删除失败')
    }
  },

  // 获取笔记详情
  getNoteById: async (id) => {
    const res = await fetch(`${API_BASE}/notes/${id}`)
    const data = await res.json()
    if (data.success && data.data) {
      return transformNote(data.data)
    }
    return null
  },

  // 创建笔记
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

  // 更新笔记
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

  // 删除笔记
  deleteNote: async (id) => {
    const res = await fetch(`${API_BASE}/notes/${id}`, {
      method: 'DELETE'
    })
    const data = await res.json()
    if (!data.success) {
      throw new Error(data.message || '删除失败')
    }
  },

  // 上传文件并创建笔记
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

  // 获取用户个人信息（包含统计数据）
  getUserProfile: async (userId) => {
    const res = await fetch(`${API_BASE}/users/profile?userId=${userId}`)
    const data = await res.json()
    if (data.success && data.user) {
      return data.user
    }
    throw new Error(data.message || '获取用户信息失败')
  },

  // 更新用户个人信息
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

  // 获取所有公开的科目
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

  // 获取用户所属的群组列表
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

  // 上传头像
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

  // 获取考试（根据笔记ID）- 兼容旧版本
  getExamByNoteId: async (noteId, userId) => {
    const res = await fetch(`${API_BASE}/exams?noteId=${noteId}&userId=${userId}`)
    const data = await res.json()
    if (data.success && data.data) {
      return data.data
    }
    return null
  },

  // 获取笔记下的所有考试
  getExamsByNoteId: async (noteId, userId) => {
    const res = await fetch(`${API_BASE}/exams?noteId=${noteId}&userId=${userId}&all=true`)
    const data = await res.json()
    if (data.success && data.data) {
      return Array.isArray(data.data) ? data.data : [data.data]
    }
    return []
  },

  // 根据考试ID获取考试详情
  getExamById: async (examId) => {
    const res = await fetch(`${API_BASE}/exams/${examId}`)
    const data = await res.json()
    if (data.success && data.data) {
      return data.data
    }
    throw new Error(data.message || '获取考试失败')
  },

  // 生成考试（使用AI）- 支持配置
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

  // 删除考试
  deleteExam: async (examId, userId) => {
    const res = await fetch(`${API_BASE}/exams/${examId}?userId=${userId}`, {
      method: 'DELETE'
    })
    const data = await res.json()
    if (!data.success) {
      throw new Error(data.message || '删除考试失败')
    }
  },

  // 获取考试的所有作答记录
  getExamAttempts: async (examId, userId) => {
    const res = await fetch(`${API_BASE}/exams/${examId}/attempts?userId=${userId}`)
    const data = await res.json()
    if (data.success && data.data) {
      return Array.isArray(data.data) ? data.data : []
    }
    return []
  },

  // 获取作答记录的答案
  getExamAnswers: async (examId, userId, attemptId) => {
    const res = await fetch(`${API_BASE}/exams/${examId}/answers?userId=${userId}&attemptId=${attemptId || ''}`)
    const data = await res.json()
    if (data.success && data.data) {
      // 转换格式：{ questionId: answer }
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

  // 保存用户答案
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
    // 返回attemptId（如果有）
    return data.attemptId || null
  },

  // 提交考试
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
  }
}

// 根据笔记ID查找笔记和所属科目
export const findNoteById = async (noteId) => {
  try {
    const note = await api.getNoteById(noteId)
    if (!note) {
      return { subject: null, note: null }
    }

    // 需要先获取所有科目来找到笔记所属的科目
    // 这里简化处理，实际应该在后端提供根据noteId查找subject的接口
    // 暂时返回note，subject需要从父组件传入或通过其他方式获取
    return { subject: null, note }
  } catch (error) {
    console.error('Error finding note:', error)
    return { subject: null, note: null }
  }
}

