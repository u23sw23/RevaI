<template>
  <div class="repositories-page" @click="activeMenuId = null">
    <h2 class="page-title">Subjects / Notes</h2>
    <p class="page-subtitle">Select a subject, then choose one of its notes to view details.</p>

    <!-- 科目列表 -->
    <div class="subjects-grid" v-if="!selectedSubject">
      <div
        class="subject-card"
        v-for="subject in subjects"
        :key="subject.id"
        @click="selectSubject(subject)"
      >
        <div class="subject-header">
          <div class="subject-main">
            <div class="subject-title-row">
              <div class="subject-title">{{ subject?.name }}</div>
              <span 
                class="visibility-badge" 
                :class="subject?.visibility === 'public' ? 'public' : 'private'"
              >
                {{ subject?.visibility === 'public' ? 'Public' : 'Private' }}
              </span>
            </div>
            <div v-if="subject?.description" class="subject-desc">{{ subject?.description }}</div>
          </div>
          <div class="menu-wrapper" @click.stop="toggleMenu(subject.id)">
            <div class="menu-dot"></div>
            <div class="menu-dot"></div>
            <div class="menu-dot"></div>
            <div
              v-if="activeMenuId === subject.id"
              class="subject-menu"
              @click.stop
            >
              <button class="menu-item" @click="startEditSubject(subject)">Rename</button>
              <button class="menu-item danger" @click="deleteSubject(subject.id)">Delete</button>
            </div>
          </div>
        </div>
        <div class="subject-meta">Notes: {{ subject?.notes?.length || 0 }}</div>
      </div>
    </div>

    <!-- 笔记列表（选中某科目后） -->
    <div v-else class="notes-panel">
      <div class="notes-header">
        <div>
          <div class="breadcrumb">Subject / {{ selectedSubject?.name || '' }}</div>
          <h3 class="notes-title">{{ selectedSubject?.name || '' }} - Notes</h3>
          <p class="notes-subtitle">Total {{ selectedSubject?.notes?.length || 0 }} notes. Click one to view details.</p>
        </div>
        <button class="back-btn" @click="backToSubjects">Back to subjects</button>
      </div>

      <div class="notes-grid">
        <div
          class="note-card"
          v-for="note in (selectedSubject?.notes || [])"
          :key="note.id"
          @click="goToNote(note.id)"
        >
          <div class="note-title">{{ note.title }}</div>
          <div class="note-desc">{{ trimContent(note.content) }}</div>
          <div class="note-meta">Updated at: {{ note.updatedAt }}</div>
        </div>
      </div>
    </div>

    <!-- Upload / create notes (visible only when a subject is selected) -->
    <div v-if="selectedSubject" class="create-section">
      <div
        class="drop-zone"
        :class="{ 'is-dragging': isDragging }"
        @dragover.prevent="onDragOver"
        @dragleave.prevent="onDragLeave"
        @drop.prevent="onDrop"
      >
        <div class="drop-zone-inner">
          <p class="drop-title">Drag & drop files here</p>
          <p class="drop-subtitle">or</p>
          <div class="create-actions">
            <label class="add-file-btn" for="note-file-input">add file ...</label>
            <input
              id="note-file-input"
              type="file"
              multiple
              class="file-input-hidden"
              @change="onFileChange"
            />
            <input 
              type="text" 
              placeholder="note / res name..." 
              class="repo-name-input"
              v-model="noteName"
            />
            <button class="look-btn" @click="submitToAI" :disabled="isGenerating">
              {{ isGenerating ? 'Generating...' : 'Generate Notes with AI' }}
            </button>
          </div>
          
          <!-- 生成中的动态提示 -->
          <div v-if="isGenerating" class="generating-status">
            <div class="generating-spinner"></div>
            <div class="generating-text">
              <span class="generating-title">✨ AI is creating your notes</span>
              <span class="generating-detail">{{ generatingStatus }}</span>
            </div>
          </div>
          
          <p v-else class="create-hint">
            Upload files and AI will generate well-structured, high-quality study notes for you.
          </p>
          <ul v-if="selectedFiles.length && !isGenerating" class="file-list">
            <li
              v-for="(file, index) in selectedFiles"
              :key="file.name + index"
              class="file-item"
            >
              <span class="file-name">{{ file.name }}</span>
              <button
                class="file-remove-btn"
                type="button"
                @click.stop="removeFile(index)"
                title="Remove file"
              >
                <span class="remove-icon">×</span>
              </button>
            </li>
          </ul>
        </div>
      </div>
    </div>

    <!-- 浮动创建按钮 -->
    <button
      v-if="!selectedSubject"
      class="fab"
      @click.stop="startCreate"
    >
      +
    </button>

    <!-- Subject create / rename dialog -->
    <div
      v-if="showSubjectDialog"
      class="modal-mask"
      @click.self="closeSubjectDialog"
    >
      <div class="modal-panel">
        <h3 class="modal-title">
          {{ dialogMode === 'edit' ? 'Edit subject' : 'Create new subject' }}
        </h3>
        <div class="modal-field">
          <label class="modal-label">Subject Name</label>
          <input
            v-model="dialogName"
            type="text"
            class="modal-input"
            placeholder="Enter subject name"
          />
        </div>
        <div class="modal-field">
          <label class="modal-label">Description</label>
          <textarea
            v-model="dialogDescription"
            class="modal-textarea"
            placeholder="Enter subject description (optional)"
            rows="3"
          ></textarea>
        </div>
        <div class="modal-field">
          <label class="modal-label">Visibility</label>
          <select v-model="dialogVisibility" class="modal-select">
            <option value="private">Private</option>
            <option value="public">Public</option>
          </select>
        </div>
        <div class="modal-actions">
          <button class="modal-btn" @click="closeSubjectDialog">Cancel</button>
          <button class="modal-btn primary" @click="confirmSubjectDialog">Confirm</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { api } from '../utils/api'

const router = useRouter()

const subjects = ref([])
const selectedSubject = ref(null)
const activeMenuId = ref(null)
const loading = ref(false)

// 获取当前用户ID（从localStorage或props）
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

// 加载科目列表
const loadSubjects = async () => {
  loading.value = true
  try {
    const userId = getCurrentUserId()
    if (!userId) {
      alert('请先登录')
      return
    }
    subjects.value = await api.getSubjects(userId)
  } catch (error) {
    console.error('Failed to load subjects:', error)
    alert('加载科目列表失败：' + error.message)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadSubjects()
})

// 弹窗状态
const showSubjectDialog = ref(false)
const dialogMode = ref('create') // 'create' | 'edit'
const dialogName = ref('')
const dialogDescription = ref('')
const dialogVisibility = ref('private')
const dialogSubject = ref(null)

const selectSubject = (subject) => {
  selectedSubject.value = subject
}

const backToSubjects = () => {
  selectedSubject.value = null
}

const startEditSubject = (subject) => {
  activeMenuId.value = null
  dialogMode.value = 'edit'
  dialogSubject.value = subject
  dialogName.value = subject.name || ''
  dialogDescription.value = subject.description || ''
  dialogVisibility.value = subject.visibility || 'private'
  showSubjectDialog.value = true
}

const startCreate = () => {
  dialogMode.value = 'create'
  dialogSubject.value = null
  dialogName.value = ''
  dialogDescription.value = ''
  dialogVisibility.value = 'private'
  showSubjectDialog.value = true
}

const deleteSubject = async (id) => {
  if (!confirm('Delete this subject? All its notes will also be removed.')) {
    return
  }
  
  try {
    await api.deleteSubject(id)
    subjects.value = subjects.value.filter((s) => s.id !== id)
    
    if (selectedSubject.value?.id === id) {
      selectedSubject.value = null
    }
    alert('删除成功')
  } catch (error) {
    console.error('Failed to delete subject:', error)
    alert('删除失败：' + error.message)
  }
}

const toggleMenu = (id) => {
  activeMenuId.value = activeMenuId.value === id ? null : id
}

const closeSubjectDialog = () => {
  showSubjectDialog.value = false
  dialogName.value = ''
  dialogDescription.value = ''
  dialogVisibility.value = 'private'
  dialogSubject.value = null
}

const confirmSubjectDialog = async () => {
  const name = dialogName.value.trim()
  if (!name) {
    alert('Please enter a subject name.')
    return
  }

  const userId = getCurrentUserId()
  if (!userId) {
    alert('请先登录')
    return
  }

  try {
    if (dialogMode.value === 'edit' && dialogSubject.value) {
      // 更新科目
      const updated = await api.updateSubject(dialogSubject.value.id, {
        name,
        description: dialogDescription.value.trim(),
        visibility: dialogVisibility.value
      })
      // 更新本地数据
      const index = subjects.value.findIndex(s => s.id === dialogSubject.value.id)
      if (index !== -1) {
        subjects.value[index] = updated
      }
      // 如果当前选中的是正在编辑的科目，也要更新
      if (selectedSubject.value?.id === dialogSubject.value.id) {
        selectedSubject.value = updated
      }
      alert('更新成功')
    } else if (dialogMode.value === 'create') {
      // 创建科目
      const newSubject = await api.createSubject({
        name,
        description: dialogDescription.value.trim(),
        visibility: dialogVisibility.value,
        userId
      })
      subjects.value.push(newSubject)
      alert('创建成功')
    }
    
    closeSubjectDialog()
  } catch (error) {
    console.error('Failed to save subject:', error)
    alert('操作失败：' + error.message)
  }
}

const trimContent = (content) => {
  if (!content) return ''
  return content.length > 120 ? content.slice(0, 118) + '...' : content
}

const goToNote = (noteId) => {
  router.push(`/repositories/${noteId}`)
}

// drag & drop upload state and handlers
const isDragging = ref(false)
const selectedFiles = ref([])
const noteName = ref('')

const onDragOver = () => {
  isDragging.value = true
}

const onDragLeave = () => {
  isDragging.value = false
}

const onDrop = (event) => {
  isDragging.value = false
  const files = Array.from(event.dataTransfer.files || [])
  if (files.length) {
    selectedFiles.value = files
  }
}

const onFileChange = (event) => {
  const files = Array.from(event.target.files || [])
  if (files.length) {
    selectedFiles.value = files
  }
}

const isGenerating = ref(false)
const generatingStatus = ref('')

const submitToAI = async () => {
  if (!selectedFiles.value.length) {
    alert('Please upload at least one file.')
    return
  }
  if (!selectedSubject.value) {
    alert('Please select a subject first.')
    return
  }

  const userId = getCurrentUserId()
  if (!userId) {
    alert('请先登录')
    return
  }

  isGenerating.value = true
  generatingStatus.value = 'Uploading files...'

  try {
    // 模拟进度提示
    setTimeout(() => {
      if (isGenerating.value) generatingStatus.value = 'Processing your files...'
    }, 1500)
    setTimeout(() => {
      if (isGenerating.value) generatingStatus.value = 'AI is analyzing the content...'
    }, 3000)
    setTimeout(() => {
      if (isGenerating.value) generatingStatus.value = 'Organizing key concepts...'
    }, 6000)
    setTimeout(() => {
      if (isGenerating.value) generatingStatus.value = 'Structuring your notes...'
    }, 10000)
    setTimeout(() => {
      if (isGenerating.value) generatingStatus.value = 'Almost done, please wait...'
    }, 15000)

    // 1. 先调用AI接口生成笔记
    const formData = new FormData()
    selectedFiles.value.forEach((file) => {
      formData.append('files', file)
    })
    formData.append('noteName', noteName.value || 'Study Notes')
    formData.append('subjectName', selectedSubject.value?.name || '')

    const aiResponse = await fetch('/api/ai/generate-note', {
      method: 'POST',
      body: formData,
      // 增加超时时间（5分钟）
      signal: AbortSignal.timeout(300000)
    })

    if (!aiResponse.ok) {
      const text = await aiResponse.text()
      let errMsg = text
      try {
        const errorData = JSON.parse(text)
        errMsg = errorData.error || JSON.stringify(errorData)
      } catch (e) {
        // 不是 JSON，就直接用文本
      }
      throw new Error(errMsg || 'Generation failed')
    }

    const aiData = await aiResponse.json()
    console.log('✅ Note generated successfully:', aiData)

    if (!aiData.note) {
      throw new Error('AI 返回格式错误')
    }

    // 2. 将生成的笔记保存到后端
    const newNote = await api.createNote({
      title: aiData.note.title,
      content: aiData.note.content,
      subjectId: selectedSubject.value.id,
      userId: userId
    })
    
    // 刷新当前科目的笔记列表
    const updatedSubject = await api.getSubjectById(selectedSubject.value.id)
    if (updatedSubject) {
      selectedSubject.value = updatedSubject
      // 同时更新subjects列表中的对应科目
      const index = subjects.value.findIndex(s => s.id === selectedSubject.value.id)
      if (index !== -1) {
        subjects.value[index] = updatedSubject
      }
    }
    
    // 清空表单
    selectedFiles.value = []
    noteName.value = ''
    alert('🎉 Notes generated successfully! Added to current subject.')
  } catch (error) {
    console.error('❌ Failed to generate notes:', error)
    let errorMsg = error.message || 'Unknown error'
    if (error.name === 'AbortError' || error.name === 'TimeoutError') {
      errorMsg = '请求超时，请尝试上传较小的文件或减少文件数量'
    } else if (error.message && error.message.includes('Failed to fetch')) {
      errorMsg = '无法连接到服务器，请检查后端服务是否正在运行（端口 8080）'
    } else if (error.message && error.message.includes('ERR_CONNECTION_RESET')) {
      errorMsg = '连接被重置，可能是文件太大或后端处理超时，请尝试上传较小的文件'
    }
    alert('Failed to generate notes: ' + errorMsg)
  } finally {
    isGenerating.value = false
    generatingStatus.value = ''
  }
}

const removeFile = (index) => {
  selectedFiles.value.splice(index, 1)
}
</script>

<style scoped>
.repositories-page {
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
  font-size: 14px;
  margin-bottom: 8px;
}

.subjects-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 16px;
}

.subject-card {
  background-color: #fff;
  padding: 20px;
  border-radius: 12px;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.06);
  cursor: pointer;
  transition: all 0.2s;
  border: 2px solid transparent;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.subject-card:hover {
  border-color: #1976d2;
  box-shadow: 0 6px 12px rgba(0, 0, 0, 0.08);
}

.subject-title-row {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.subject-title {
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

.visibility-badge {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 12px;
  font-weight: 500;
}

.visibility-badge.public {
  background-color: #4caf50;
  color: white;
}

.visibility-badge.private {
  background-color: #757575;
  color: white;
}

.subject-desc {
  font-size: 14px;
  color: #666;
}

.subject-meta {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
}

.subject-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
}

.subject-main {
  flex: 1;
  min-width: 0;
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

.subject-menu {
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

.notes-panel {
  background-color: #fff;
  padding: 20px;
  border-radius: 12px;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.06);
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.notes-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.breadcrumb {
  font-size: 12px;
  color: #999;
  margin-bottom: 4px;
}

.notes-title {
  font-size: 18px;
  font-weight: 600;
  color: #333;
}

.notes-subtitle {
  font-size: 14px;
  color: #666;
}

.back-btn {
  padding: 10px 14px;
  border-radius: 8px;
  border: 1px solid #ddd;
  background-color: #fff;
  cursor: pointer;
  transition: all 0.2s;
  font-size: 14px;
}

.back-btn:hover {
  border-color: #1976d2;
  color: #1976d2;
}

.notes-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 14px;
}

.note-card {
  background-color: #f9f9f9;
  padding: 16px;
  border-radius: 10px;
  border: 1px solid #eee;
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.note-card:hover {
  border-color: #1976d2;
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.08);
}

.note-title {
  font-size: 15px;
  font-weight: 600;
  color: #333;
}

.note-desc {
  font-size: 13px;
  color: #666;
  line-height: 1.5;
}

.note-meta {
  font-size: 12px;
  color: #999;
}

/* 让科目卡片在小屏也保持舒适间距 */
@media (max-width: 768px) {
  .subjects-grid {
    grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  }
}

/* 上传笔记区域样式 */
.create-section {
  background-color: #fff;
  padding: 24px;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
}

.drop-zone {
  border: 2px dashed #d0d7de;
  border-radius: 8px;
  padding: 16px;
  transition: border-color 0.2s, background-color 0.2s;
}

.drop-zone.is-dragging {
  border-color: #1976d2;
  background-color: #e3f2fd;
}

.drop-zone-inner {
  display: flex;
  flex-direction: column;
  gap: 8px;
  align-items: flex-start;
}

.drop-title {
  font-size: 14px;
  font-weight: 600;
  color: #333;
}

.drop-subtitle {
  font-size: 12px;
  color: #999;
}

.look-btn {
  padding: 12px 24px;
  background-color: #ff4081;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
}

.look-btn:hover {
  background-color: #e91e63;
}

.create-hint {
  font-size: 12px;
  color: #666;
  margin-bottom: 12px;
}

.create-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.add-file-btn {
  padding: 8px 16px;
  background-color: #f5f5f5;
  border: 1px solid #ddd;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
}

.add-file-btn:hover {
  background-color: #e0e0e0;
}

.repo-name-input {
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
  min-width: 220px;
}

.file-input-hidden {
  display: none;
}

.file-list {
  margin-top: 8px;
  list-style: none;
  padding: 0;
  font-size: 12px;
  color: #555;
}

.file-item {
  margin-bottom: 4px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.file-name {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-remove-btn {
  border: none;
  background: transparent;
  color: #999;
  cursor: pointer;
  padding: 4px;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
  flex-shrink: 0;
}

.file-remove-btn:hover {
  background-color: #ffebee;
  color: #e53935;
}

.remove-icon {
  font-size: 20px;
  line-height: 1;
  font-weight: 300;
}

.generating-status {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  background-color: #e3f2fd;
  border: 1px solid #90caf9;
  border-radius: 8px;
  margin-top: 12px;
}

.generating-spinner {
  width: 24px;
  height: 24px;
  border: 3px solid #e3f2fd;
  border-top-color: #1976d2;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.generating-text {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.generating-title {
  font-size: 15px;
  font-weight: 600;
  color: #1976d2;
}

.generating-detail {
  font-size: 13px;
  color: #666;
  animation: fade-in-out 1.5s ease-in-out infinite;
}

@keyframes fade-in-out {
  0%, 100% {
    opacity: 0.7;
  }
  50% {
    opacity: 1;
  }
}

.look-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
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

.modal-input,
.modal-textarea,
.modal-select {
  width: 100%;
  padding: 9px 12px;
  border-radius: 6px;
  border: 1px solid #ddd;
  font-size: 14px;
  font-family: inherit;
  resize: vertical;
}

.modal-textarea {
  min-height: 60px;
  line-height: 1.5;
}

.modal-input:focus,
.modal-textarea:focus,
.modal-select:focus {
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

/* 浮动创建按钮 */
.fab {
  position: fixed;
  right: 32px;
  bottom: 32px;
  width: 56px;
  height: 56px;
  border-radius: 50%;
  border: none;
  background-color: #1976d2;
  color: #fff;
  font-size: 32px;
  line-height: 56px;
  text-align: center;
  cursor: pointer;
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.2);
  transition: transform 0.2s, box-shadow 0.2s, background-color 0.2s;
  z-index: 15;
}

.fab:hover {
  transform: translateY(-2px);
  box-shadow: 0 10px 24px rgba(0, 0, 0, 0.25);
  background-color: #1565c0;
}
</style>
