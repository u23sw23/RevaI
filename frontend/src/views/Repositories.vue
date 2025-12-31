<template>
  <div class="repositories-page" @click="activeMenuId = null">
    <h2 class="page-title">Subjects / Notes</h2>
    <p class="page-subtitle">Select a subject, then choose one of its notes to view details.</p>

    <div v-if="!selectedSubject" class="subjects-container">
      
      <div v-if="personalSubjects.length > 0" class="subjects-section">
        <h3 class="section-title">Personal Subjects</h3>
        <div class="subjects-grid">
          <div
            class="subject-card"
            v-for="subject in personalSubjects"
            :key="subject.id"
            @click="selectSubject(subject)"
          >
        <div class="subject-header">
          <div class="subject-main">
              <div class="subject-title-row">
              <div class="subject-title">{{ subject?.name }}</div>
              <span 
                class="visibility-badge" 
                :class="subject?.visibility === 'public' ? 'public' : (subject?.visibility === 'clone' ? 'clone' : 'private')"
              >
                {{
                  subject?.visibility === 'public'
                    ? 'Public'
                    : subject?.visibility === 'clone'
                    ? 'Clone'
                    : 'Private'
                }}
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
      </div>

      <div v-if="groupSubjects.length > 0" class="subjects-section group-section">
        <h3 class="section-title">Group Subjects</h3>
        <div class="subjects-grid">
          <div
            class="subject-card"
            v-for="subject in groupSubjects"
            :key="subject.id"
            @click="selectSubject(subject)"
          >
            <div class="subject-header">
              <div class="subject-main">
                  <div class="subject-title-row">
                  <div class="subject-title">{{ subject?.name }}</div>
                  <span 
                    class="visibility-badge group-badge"
                  >
                    Group
                  </span>
                </div>
                <div v-if="subject?.description" class="subject-desc">{{ subject?.description }}</div>
              </div>
            </div>
            <div class="subject-meta">Notes: {{ subject?.notes?.length || 0 }}</div>
          </div>
        </div>
      </div>
    </div>

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

    <button
      v-if="!selectedSubject"
      class="fab"
      @click.stop="startCreate"
    >
      +
    </button>

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

const personalSubjects = ref([])
const groupSubjects = ref([])
const selectedSubject = ref(null)
const activeMenuId = ref(null)
const loading = ref(false)

const findSubjectInArrays = (subjectId) => {
  const personalIndex = personalSubjects.value.findIndex(s => s.id === subjectId)
  if (personalIndex !== -1) {
    return { array: personalSubjects, index: personalIndex, type: 'personal' }
  }
  const groupIndex = groupSubjects.value.findIndex(s => s.id === subjectId)
  if (groupIndex !== -1) {
    return { array: groupSubjects, index: groupIndex, type: 'group' }
  }
  return null
}

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
      return
    }
    const separated = await api.getSubjectsSeparated(userId)
    personalSubjects.value = separated.personal || []
    groupSubjects.value = separated.group || []
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

const showSubjectDialog = ref(false)
const dialogMode = ref('create') 
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
    const found = findSubjectInArrays(id)
    if (found) {
      found.array.value = found.array.value.filter((s) => s.id !== id)
    }
    
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
      
      const updated = await api.updateSubject(dialogSubject.value.id, {
        name,
        description: dialogDescription.value.trim(),
        visibility: dialogVisibility.value
      })
      
      const found = findSubjectInArrays(dialogSubject.value.id)
      if (found) {
        found.array.value[found.index] = updated
      }
      
      if (selectedSubject.value?.id === dialogSubject.value.id) {
        selectedSubject.value = updated
      }
      alert('更新成功')
    } else if (dialogMode.value === 'create') {
      
      const newSubject = await api.createSubject({
        name,
        description: dialogDescription.value.trim(),
        visibility: dialogVisibility.value,
        userId
      })
      personalSubjects.value.push(newSubject)
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

    const formData = new FormData()
    selectedFiles.value.forEach((file) => {
      formData.append('files', file)
    })
    formData.append('noteName', noteName.value || 'Study Notes')
    formData.append('subjectName', selectedSubject.value?.name || '')

    const aiResponse = await fetch('/api/ai/generate-note', {
      method: 'POST',
      body: formData,
      
      signal: AbortSignal.timeout(300000)
    })

    if (!aiResponse.ok) {
      const text = await aiResponse.text()
      let errMsg = text
      try {
        const errorData = JSON.parse(text)
        errMsg = errorData.error || JSON.stringify(errorData)
      } catch (e) {
        
      }
      throw new Error(errMsg || 'Generation failed')
    }

    const aiData = await aiResponse.json()
    console.log('✅ Note generated successfully:', aiData)

    if (!aiData.note) {
      throw new Error('AI 返回格式错误')
    }

    const newNote = await api.createNote({
      title: aiData.note.title,
      content: aiData.note.content,
      subjectId: selectedSubject.value.id,
      userId: userId
    })

    const updatedSubject = await api.getSubjectById(selectedSubject.value.id)
    if (updatedSubject) {
      selectedSubject.value = updatedSubject
      
      const found = findSubjectInArrays(selectedSubject.value.id)
      if (found) {
        found.array.value[found.index] = updatedSubject
      }
    }

    selectedFiles.value = []
    noteName.value = ''
    alert('Notes generated successfully')
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
  font-size: 14px;
  margin-bottom: 24px;
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

.subjects-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 16px;
}

.subject-card {
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

.subject-card:hover {
  border-color: #2196F3;
  box-shadow: 0 8px 24px rgba(33, 150, 243, 0.25), 0 0 0 3px rgba(33, 150, 243, 0.15);
  transform: translateY(-2px);
}

.subject-title-row {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.subject-title {
  font-size: 18px;
  font-weight: 600;
  color: #1565C0;
  margin-bottom: 4px;
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

.visibility-badge.clone {
  background-color: #0D47A1;
  color: #E3F2FD;
}

.visibility-badge.group-badge {
  background: linear-gradient(135deg, #2196F3 0%, #1976D2 100%);
  color: white;
}

.subject-desc {
  font-size: 14px;
  color: #546E7A;
  font-weight: 400;
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
  background: linear-gradient(to bottom, #FFFFFF 0%, #F8FBFF 100%);
  padding: 28px;
  border-radius: 16px;
  box-shadow: 0 4px 16px rgba(33, 150, 243, 0.1);
  display: flex;
  flex-direction: column;
  gap: 20px;
  border: 2px solid rgba(144, 202, 249, 0.8);
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
  font-size: 20px;
  font-weight: 700;
  color: #1565C0;
  letter-spacing: -0.3px;
}

.notes-subtitle {
  font-size: 14px;
  color: #666;
}

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

.back-btn:hover {
  border-color: #64B5F6;
  color: #1976D2;
  background: linear-gradient(to right, #F0F7FF 0%, rgba(227, 242, 253, 0.5) 100%);
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(33, 150, 243, 0.15);
}

.notes-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 14px;
}

.note-card {
  background: linear-gradient(to bottom, rgba(255, 255, 255, 0.9) 0%, rgba(248, 251, 255, 0.6) 100%);
  padding: 20px;
  border-radius: 12px;
  border: 2px solid rgba(144, 202, 249, 0.8);
  cursor: pointer;
  transition: all 0.3s ease;
  display: flex;
  flex-direction: column;
  gap: 10px;
  position: relative;
}

.note-card:hover {
  border-color: #2196F3;
  box-shadow: 0 6px 16px rgba(33, 150, 243, 0.25), 0 0 0 3px rgba(33, 150, 243, 0.15);
  transform: translateY(-2px);
  background: linear-gradient(to bottom, #FFFFFF 0%, #F8FBFF 100%);
}

.note-title {
  font-size: 16px;
  font-weight: 600;
  color: #1565C0;
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

@media (max-width: 768px) {
  .subjects-grid {
    grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  }
}

.create-section {
  background: linear-gradient(to bottom, #FFFFFF 0%, #F8FBFF 100%);
  padding: 28px;
  border-radius: 16px;
  box-shadow: 0 4px 16px rgba(33, 150, 243, 0.1);
  border: 2px solid rgba(144, 202, 249, 0.8);
}

.drop-zone {
  border: 2px dashed rgba(144, 202, 249, 0.5);
  border-radius: 12px;
  padding: 20px;
  transition: all 0.3s ease;
  background: rgba(255, 255, 255, 0.6);
}

.drop-zone.is-dragging {
  border-color: #64B5F6;
  background: linear-gradient(to bottom, #E3F2FD 0%, rgba(187, 222, 251, 0.4) 100%);
  box-shadow: 0 4px 12px rgba(33, 150, 243, 0.15);
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
  background: linear-gradient(135deg, #2196F3 0%, #1976D2 100%);
  color: white;
  border: none;
  border-radius: 10px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.3s ease;
  box-shadow: 0 4px 12px rgba(33, 150, 243, 0.3);
}

.look-btn:hover {
  background: linear-gradient(135deg, #1976D2 0%, #1565C0 100%);
  box-shadow: 0 6px 16px rgba(33, 150, 243, 0.4);
  transform: translateY(-1px);
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
  padding: 10px 18px;
  background: linear-gradient(to bottom, #FFFFFF 0%, #F8FBFF 100%);
  border: 2px solid rgba(187, 222, 251, 0.8);
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.3s ease;
}

.add-file-btn:hover {
  background: linear-gradient(to bottom, #E3F2FD 0%, #BBDEFB 100%);
  border-color: #64B5F6;
  color: #1565C0;
  transform: translateY(-1px);
}

.repo-name-input {
  padding: 10px 14px;
  border: 2px solid rgba(187, 222, 251, 0.6);
  border-radius: 8px;
  font-size: 14px;
  min-width: 220px;
  background: rgba(255, 255, 255, 0.9);
  transition: all 0.3s ease;
}

.repo-name-input:focus {
  outline: none;
  border-color: #64B5F6;
  box-shadow: 0 0 0 4px rgba(33, 150, 243, 0.1);
  background: #FFFFFF;
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
  padding: 16px 20px;
  background: linear-gradient(135deg, #E3F2FD 0%, #BBDEFB 100%);
  border: 1px solid rgba(144, 202, 249, 0.6);
  border-radius: 12px;
  margin-top: 12px;
  box-shadow: 0 2px 8px rgba(33, 150, 243, 0.1);
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

.modal-input,
.modal-textarea,
.modal-select {
  width: 100%;
  padding: 10px 14px;
  border-radius: 10px;
  border: 2px solid rgba(187, 222, 251, 0.6);
  font-size: 14px;
  font-family: inherit;
  resize: vertical;
  background: rgba(255, 255, 255, 0.9);
  transition: all 0.3s ease;
}

.modal-textarea {
  min-height: 60px;
  line-height: 1.5;
}

.modal-input:focus,
.modal-textarea:focus,
.modal-select:focus {
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

.fab {
  position: fixed;
  right: 32px;
  bottom: 32px;
  width: 56px;
  height: 56px;
  border-radius: 50%;
  border: none;
  background: linear-gradient(135deg, #2196F3 0%, #1976D2 100%);
  color: #fff;
  font-size: 32px;
  line-height: 56px;
  text-align: center;
  cursor: pointer;
  box-shadow: 0 8px 24px rgba(33, 150, 243, 0.4);
  transition: all 0.3s ease;
  z-index: 15;
}

.fab:hover {
  transform: translateY(-3px) scale(1.05);
  box-shadow: 0 12px 32px rgba(33, 150, 243, 0.5);
  background: linear-gradient(135deg, #1976D2 0%, #1565C0 100%);
}
</style>
