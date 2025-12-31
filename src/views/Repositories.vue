<template>
  <div class="repositories-page" @click="activeMenuId = null">
    <h2 class="page-title">Subjects / Notes</h2>
    <p class="page-subtitle">Select a subject, then choose one of its notes to view details.</p>

    <div class="subjects-grid" v-if="!selectedSubject">
      <div
        class="subject-card"
        v-for="subject in subjects"
        :key="subject.id"
        @click="selectSubject(subject)"
      >
        <div class="subject-header">
          <div class="subject-main">
            <div class="subject-title">{{ subject?.name }}</div>
            <div class="subject-desc">{{ subject?.description }}</div>
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
  </div>

  <div
    v-if="showSubjectDialog"
    class="modal-mask"
    @click.self="closeSubjectDialog"
  >
    <div class="modal-panel">
      <h3 class="modal-title">
        {{ dialogMode === 'edit' ? 'Rename subject' : 'Create new subject' }}
      </h3>
      <input
        v-model="dialogName"
        type="text"
        class="modal-input"
        placeholder="Enter subject name"
      />
      <div class="modal-actions">
        <button class="modal-btn" @click="closeSubjectDialog">Cancel</button>
        <button class="modal-btn primary" @click="confirmSubjectDialog">Confirm</button>
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
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { subjects as subjectList } from '../data/notes'

const router = useRouter()

const STORAGE_KEY = 'revai-subjects'

const subjects = ref(subjectList.map((s) => ({ ...s })))
const selectedSubject = ref(null)
const activeMenuId = ref(null)

const showSubjectDialog = ref(false)
const dialogMode = ref('create') 
const dialogName = ref('')
const dialogSubject = ref(null)

const saveSubjects = () => {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(subjects.value))
  } catch (e) {
    console.warn('Failed to save subjects to localStorage', e)
  }
}

const loadSubjects = () => {
  try {
    const stored = localStorage.getItem(STORAGE_KEY)
    if (stored) {
      const parsed = JSON.parse(stored)
      if (Array.isArray(parsed) && parsed.length) {
        subjects.value = parsed
        return
      }
    }
  } catch (e) {
    console.warn('Failed to load subjects from localStorage', e)
  }
  
  subjects.value = subjectList.map((s) => ({ ...s }))
}

onMounted(() => {
  loadSubjects()
})

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
  showSubjectDialog.value = true
}

const startCreate = () => {
  dialogMode.value = 'create'
  dialogSubject.value = null
  dialogName.value = ''
  showSubjectDialog.value = true
}

const deleteSubject = (id) => {
  if (!confirm('Delete this subject? All its notes will also be removed (front-end demo only).')) {
    return
  }
  subjects.value = subjects.value.filter((s) => s.id !== id)

  if (selectedSubject.value?.id === id) {
    selectedSubject.value = null
  }
  saveSubjects()
}

const toggleMenu = (id) => {
  activeMenuId.value = activeMenuId.value === id ? null : id
}

const closeSubjectDialog = () => {
  showSubjectDialog.value = false
  dialogName.value = ''
  dialogSubject.value = null
}

const confirmSubjectDialog = () => {
  const name = dialogName.value.trim()
  if (!name) {
    alert('Please enter a subject name.')
    return
  }

  if (dialogMode.value === 'edit' && dialogSubject.value) {
    dialogSubject.value.name = name
  } else if (dialogMode.value === 'create') {
    subjects.value.push({
      id: Date.now().toString(),
      name,
      description: '',
      notes: []
    })
  }

  closeSubjectDialog()
  saveSubjects()
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
const generatedNote = ref(null)

const generatingStatus = ref('')

const submitToAI = async () => {
  if (!selectedFiles.value.length) {
    alert('Please upload at least one file.')
    return
  }

  isGenerating.value = true
  generatedNote.value = null
  generatingStatus.value = 'Uploading files...'

  try {
    
    const formData = new FormData()
    selectedFiles.value.forEach((file) => {
      formData.append('files', file)
    })
    formData.append('noteName', noteName.value || 'Study Notes')
    formData.append('subjectName', selectedSubject.value?.name || '')

    console.log('📤 Uploading files and generating notes...')

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

    const response = await fetch('/api/ai/generate-note', {
      method: 'POST',
      body: formData,
    })

    if (!response.ok) {
      const text = await response.text()
      let errMsg = text
      try {
        const errorData = JSON.parse(text)
        errMsg = errorData.error || JSON.stringify(errorData)
      } catch (e) {
        
      }
      throw new Error(errMsg || 'Generation failed')
    }

    const data = await response.json()
    console.log('✅ Note generated successfully:', data)

    if (data.note && selectedSubject.value) {
      const newNote = {
        id: Date.now().toString(),
        title: data.note.title,
        content: data.note.content,
        updatedAt: new Date().toLocaleString(),
      }
      selectedSubject.value.notes.push(newNote)
      generatedNote.value = newNote

      selectedFiles.value = []
      noteName.value = ''
      
      alert('Notes generated successfully')
      saveSubjects()
    }
  } catch (error) {
    console.error('❌ Failed to generate notes:', error)
    alert('Failed to generate notes: ' + error.message)
  } finally {
    isGenerating.value = false
    generatingStatus.value = ''
  }
}

const removeFile = (index) => {
  selectedFiles.value.splice(index, 1)
}

const deleteNote = (noteId) => {
  if (!confirm('Are you sure you want to delete this note? This action cannot be undone.')) {
    return
  }
  if (selectedSubject.value) {
    selectedSubject.value.notes = selectedSubject.value.notes.filter(n => n.id !== noteId)
    saveSubjects()
  }
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

.subject-title {
  font-size: 16px;
  font-weight: 600;
  color: #333;
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

.note-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 8px;
}

.note-delete-btn {
  border: none;
  background: transparent;
  color: #999;
  cursor: pointer;
  font-size: 20px;
  line-height: 1;
  padding: 2px 6px;
  border-radius: 4px;
  transition: all 0.2s;
  flex-shrink: 0;
}

.note-delete-btn:hover {
  background-color: #ffebee;
  color: #e53935;
}

.generating-status {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px 20px;
  background: linear-gradient(135deg, #e3f2fd 0%, #f3e5f5 100%);
  border-radius: 12px;
  margin: 12px 0;
  animation: pulse-bg 2s ease-in-out infinite;
}

@keyframes pulse-bg {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0.85;
  }
}

.generating-spinner {
  width: 32px;
  height: 32px;
  border: 3px solid #e0e0e0;
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

@media (max-width: 768px) {
  .subjects-grid {
    grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  }
}

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

.modal-input {
  width: 100%;
  padding: 9px 12px;
  border-radius: 6px;
  border: 1px solid #ddd;
  font-size: 14px;
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
