<template>
  <div class="group-detail-page">
    <div v-if="loadingGroup" class="loading-state">
      <p>Loading group...</p>
    </div>
    <div v-else-if="!group" class="error-state">
      <p>Group not found</p>
      <button class="back-btn" @click="goBack">Back to Groups</button>
    </div>
    <div v-else>
      
      <div class="group-header">
        <div>
          <div class="breadcrumb">Group / {{ group.name }}</div>
          <h2 class="group-title">{{ group.name }}</h2>
          <p class="group-subtitle">Shared workspace for all group members</p>
          <div class="group-info">
            <span class="info-item">Members: {{ group.memberCount }}</span>
            <span class="info-item">Created: {{ group.createTime }}</span>
          </div>
        </div>
        <button class="back-btn" @click="goBack">Back to Groups</button>
      </div>

      <div v-if="loadingSubject" class="loading-state">
        <p>Loading shared notes...</p>
      </div>
      <div v-else-if="sharedSubject" class="notes-panel">
        <div class="notes-header">
          <div>
            <h3 class="notes-title">Shared Notes</h3>
            <p class="notes-subtitle">Total {{ sharedSubject?.notes?.length || 0 }} notes. All group members can view and edit.</p>
          </div>
        </div>

        <div class="notes-grid">
          <div
            class="note-card"
            v-for="note in (sharedSubject?.notes || [])"
            :key="note.id"
            @click="goToNote(note.id)"
          >
            <div class="note-header">
              <div class="note-title">{{ note.title }}</div>
              <button
                class="note-delete-btn"
                @click.stop="deleteNote(note.id)"
                title="Delete note"
              >
                ×
              </button>
            </div>
            <div class="note-desc">{{ trimContent(note.content) }}</div>
            <div class="note-meta">Updated at: {{ note.updatedAt }}</div>
          </div>
          <div v-if="!sharedSubject?.notes || sharedSubject.notes.length === 0" class="empty-notes">
            <p>No notes yet. Upload files to generate notes!</p>
          </div>
        </div>
      </div>
      <div v-else class="empty-state">
        <p>Shared workspace is being initialized...</p>
      </div>

      <div v-if="sharedSubject" class="create-section">
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
              <label class="add-file-btn" for="group-note-file-input">add file ...</label>
              <input
                id="group-note-file-input"
                type="file"
                multiple
                class="file-input-hidden"
                @change="onFileChange"
                accept=".txt,.md,.pdf,.doc,.docx,.ppt,.pptx,.png,.jpg,.jpeg"
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
              Upload files and AI will generate well-structured, high-quality study notes for you. All group members can access these notes.
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
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { api } from '../utils/api'

const router = useRouter()
const route = useRoute()

const group = ref(null)
const sharedSubject = ref(null)
const loadingGroup = ref(false)
const loadingSubject = ref(false)

const getCurrentUserId = () => {
  const savedUser = localStorage.getItem('auth_user')
  if (savedUser) {
    try {
      const user = JSON.parse(savedUser)
      return user.id || null
    } catch (e) {
      return null
    }
  }
  return null
}

const loadGroup = async () => {
  const groupId = route.params.id
  if (!groupId) {
    return
  }

  loadingGroup.value = true
  try {
    const res = await fetch(`/api/groups/${groupId}`)
    const data = await res.json()
    if (data.success && data.data) {
      group.value = {
        id: data.data.id,
        name: data.data.name,
        creatorId: data.data.creatorId,
        memberCount: data.data.memberIds ? data.data.memberIds.length : 0,
        createTime: formatDate(data.data.createTime)
      }
      
      await loadSharedSubject(groupId)
    }
  } catch (error) {
    console.error('Failed to load group:', error)
  } finally {
    loadingGroup.value = false
  }
}

const loadSharedSubject = async (groupId) => {
  loadingSubject.value = true
  try {
    
    const subject = await api.getGroupSharedSubject(groupId)
    if (subject) {
      sharedSubject.value = subject
    } else {
      
      const newSubject = await api.createGroupSharedSubject(groupId, {
        name: `Group: ${group.value.name}`,
        description: `Shared workspace for group "${group.value.name}"`
      })
      sharedSubject.value = newSubject
    }
  } catch (error) {
    console.error('Failed to load shared subject:', error)
    alert('Failed to load shared workspace: ' + (error.message || 'Unknown error'))
  } finally {
    loadingSubject.value = false
  }
}

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return date.toISOString().split('T')[0]
}

const goBack = () => {
  router.push('/group')
}

const goToNote = (noteId) => {
  router.push(`/repositories/${noteId}`)
}

const trimContent = (content) => {
  if (!content) return ''
  return content.length > 120 ? content.slice(0, 118) + '...' : content
}

const deleteNote = async (noteId) => {
  if (!confirm('Delete this note? All group members will lose access to it.')) {
    return
  }
  
  try {
    await api.deleteNote(noteId)
    
    if (sharedSubject.value) {
      const updated = await api.getSubjectById(sharedSubject.value.id)
      if (updated) {
        sharedSubject.value = updated
      }
    }
    alert('Note deleted successfully')
  } catch (error) {
    console.error('Failed to delete note:', error)
    alert('Failed to delete note: ' + error.message)
  }
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
  if (!sharedSubject.value) {
    alert('Shared workspace is not ready.')
    return
  }

  const userId = getCurrentUserId()
  if (!userId) {
    alert('Please login first')
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
    formData.append('subjectName', sharedSubject.value?.name || '')

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
      subjectId: sharedSubject.value.id,
      userId: userId
    })

    const updatedSubject = await api.getSubjectById(sharedSubject.value.id)
    if (updatedSubject) {
      sharedSubject.value = updatedSubject
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

onMounted(() => {
  loadGroup()
})
</script>

<style scoped>
.group-detail-page {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.loading-state,
.error-state,
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 400px;
  text-align: center;
  gap: 16px;
}

.group-header {
  background: linear-gradient(to bottom, #FFFFFF 0%, #F8FBFF 100%);
  padding: 28px;
  border-radius: 16px;
  box-shadow: 0 4px 16px rgba(33, 150, 243, 0.1);
  border: 2px solid rgba(144, 202, 249, 0.8);
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.breadcrumb {
  font-size: 12px;
  color: #999;
  margin-bottom: 4px;
}

.group-title {
  font-size: 24px;
  font-weight: 700;
  background: linear-gradient(135deg, #1976D2 0%, #2196F3 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  margin-bottom: 8px;
  letter-spacing: -0.5px;
}

.group-subtitle {
  color: #546E7A;
  font-size: 14px;
  margin-bottom: 12px;
  font-weight: 400;
}

.group-info {
  display: flex;
  gap: 16px;
  font-size: 13px;
}

.info-item {
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

.note-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 8px;
}

.note-title {
  font-size: 16px;
  font-weight: 600;
  color: #1565C0;
  flex: 1;
}

.note-delete-btn {
  border: none;
  background: transparent;
  color: #999;
  cursor: pointer;
  font-size: 24px;
  line-height: 1;
  padding: 0;
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  transition: all 0.2s;
  flex-shrink: 0;
}

.note-delete-btn:hover {
  background-color: #ffebee;
  color: #e53935;
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

.empty-notes {
  grid-column: 1 / -1;
  text-align: center;
  padding: 40px;
  color: #999;
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

.look-btn:hover:not(:disabled) {
  background: linear-gradient(135deg, #1976D2 0%, #1565C0 100%);
  box-shadow: 0 6px 16px rgba(33, 150, 243, 0.4);
  transform: translateY(-1px);
}

.look-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
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
</style>

