<template>
  <div class="community-page">
    <h2 class="page-title">Community</h2>
    <p class="page-subtitle">Browse all public repositories shared by the community.</p>

    <div class="subjects-grid" v-if="!selectedSubject">
      <div
        class="subject-card"
        v-for="subject in publicSubjects"
        :key="subject.id"
        @click="selectSubject(subject)"
      >
        <div class="subject-header">
          <div class="subject-main">
            <div class="subject-title-row">
              <div class="subject-title">{{ subject?.name }}</div>
              <span class="visibility-badge public">Public</span>
            </div>
            <div class="subject-desc">{{ subject?.description }}</div>
          </div>
        </div>
        <div class="subject-bottom-row">
          <div class="subject-bottom-text">
            <span class="creator-label">Owner:</span>
            <span class="creator-name">{{ subject?.creatorUsername || 'Unknown' }}</span>
            <span class="subject-bottom-dot">·</span>
            <span class="subject-notes-inline">Notes: {{ subject?.notes?.length || 0 }}</span>
          </div>
          <button
            class="clone-btn clone-btn-inline"
            v-if="!isOwner(subject)"
            @click.stop="cloneSubjectToMyLibrary(subject)"
            :disabled="cloningSubjectId === subject.id"
          >
            {{ cloningSubjectId === subject.id ? 'Cloning...' : 'Clone' }}
          </button>
        </div>
      </div>
    </div>

    <div v-else class="notes-panel">
      <div class="notes-header">
        <div>
          <div class="breadcrumb">
            Community / {{ selectedSubject?.creatorUsername || 'Unknown' }} / {{ selectedSubject?.name || '' }}
          </div>
          <h3 class="notes-title">{{ selectedSubject?.name || '' }} - Notes</h3>
          <p class="notes-subtitle">
            Total {{ selectedSubject?.notes?.length || 0 }} notes. Click one to view details.
          </p>
        </div>
        <div class="notes-actions">
          <button
            class="clone-btn secondary"
            v-if="!isOwner(selectedSubject)"
            @click="cloneSubjectToMyLibrary(selectedSubject)"
            :disabled="cloningSubjectId === selectedSubject?.id"
          >
            {{ cloningSubjectId === selectedSubject?.id ? 'Cloning...' : 'Clone to my library' }}
          </button>
          <button class="back-btn" @click="backToSubjects">Back to community</button>
        </div>
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

    <div v-if="loading" class="loading-state">Loading...</div>
    <div v-if="!loading && publicSubjects.length === 0" class="empty-state">
      <p>No public repositories found.</p>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { api } from '../utils/api'

const router = useRouter()
const route = useRoute()

const publicSubjects = ref([])
const selectedSubject = ref(null)
const loading = ref(false)
const cloningSubjectId = ref(null)

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

const isOwner = (subject) => {
  const userId = getCurrentUserId()
  if (!userId || !subject || subject.userId == null) return false
  
  return String(subject.userId) === String(userId)
}

const loadPublicSubjects = async () => {
  loading.value = true
  try {
    publicSubjects.value = await api.getPublicSubjects()
    
    const subjectId = route.query.subjectId
    if (subjectId) {
      const subject = publicSubjects.value.find(s => s.id == subjectId)
      if (subject) {
        await selectSubject(subject)
      }
    }
  } catch (error) {
    console.error('Failed to load public subjects:', error)
    publicSubjects.value = []
  } finally {
    loading.value = false
  }
}

watch(() => route.query.subjectId, async (newSubjectId) => {
  if (newSubjectId && publicSubjects.value.length > 0) {
    const subject = publicSubjects.value.find(s => s.id == newSubjectId)
    if (subject) {
      await selectSubject(subject)
    }
  }
})

const selectSubject = async (subject) => {
  
  if (!subject.notes) {
    try {
      const fullSubject = await api.getSubjectById(subject.id)
      selectedSubject.value = fullSubject
    } catch (error) {
      console.error('Failed to load subject details:', error)
      selectedSubject.value = subject
    }
  } else {
    selectedSubject.value = subject
  }
}

const backToSubjects = () => {
  selectedSubject.value = null
}

const cloneSubjectToMyLibrary = async (subject) => {
  const userId = getCurrentUserId()
  if (!userId) {
    alert('请先登录')
    return
  }
  if (!subject?.id) return

  cloningSubjectId.value = subject.id
  try {
    await api.cloneSubject(subject.id, userId)
    alert('克隆成功，已加入到你的 Repositories 中')
  } catch (error) {
    console.error('Failed to clone subject:', error)
    alert('克隆失败：' + (error.message || '未知错误'))
  } finally {
    cloningSubjectId.value = null
  }
}

const goToNote = (noteId) => {
  router.push(`/repositories/${noteId}`)
}

const trimContent = (content) => {
  if (!content) return ''
  return content.length > 120 ? content.slice(0, 118) + '...' : content
}

onMounted(() => {
  loadPublicSubjects()
})
</script>

<style scoped>
.community-page {
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

.subject-desc {
  font-size: 14px;
  color: #546E7A;
  margin-top: 4px;
  font-weight: 400;
}

.subject-creator {
  font-size: 12px;
  color: #999;
  margin-top: 8px;
  display: flex;
  align-items: center;
  gap: 4px;
}

.creator-label {
  color: #666;
}

.creator-name {
  color: #1565C0;
  font-weight: 600;
}

.subject-meta {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
}

.subject-bottom-row {
  margin-top: 6px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.subject-bottom-text {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 4px;
  font-size: 12px;
  color: #777;
}

.subject-bottom-dot {
  color: #b0bec5;
}

.subject-notes-inline {
  color: #607D8B;
}

.subject-actions {
  margin-top: 8px;
  display: flex;
  justify-content: flex-end;
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

.notes-panel {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.notes-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.notes-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.breadcrumb {
  font-size: 12px;
  color: #999;
  margin-bottom: 4px;
}

.notes-title {
  font-size: 22px;
  font-weight: 700;
  color: #1565C0;
  margin-bottom: 8px;
  letter-spacing: -0.3px;
}

.notes-subtitle {
  color: #666;
  font-size: 14px;
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

.clone-btn {
  padding: 6px 12px;
  border-radius: 8px;
  border: 2px solid rgba(144, 202, 249, 0.8);
  background: linear-gradient(to bottom, #FFFFFF 0%, #F8FBFF 100%);
  cursor: pointer;
  font-size: 13px;
  font-weight: 500;
  color: #1565C0;
  transition: all 0.3s ease;
}

.clone-btn-inline {
  white-space: nowrap;
}

.clone-btn.secondary {
  padding: 8px 14px;
}

.clone-btn:hover:not(:disabled) {
  background: linear-gradient(to bottom, #E3F2FD 0%, #BBDEFB 100%);
  border-color: #2196F3;
  color: #0D47A1;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(33, 150, 243, 0.2);
}

.clone-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.notes-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
}

.note-card {
  background: linear-gradient(to bottom, #FFFFFF 0%, #F8FBFF 100%);
  padding: 24px;
  border-radius: 16px;
  box-shadow: 0 4px 12px rgba(33, 150, 243, 0.08);
  cursor: pointer;
  transition: all 0.3s ease;
  border: 2px solid rgba(144, 202, 249, 0.8);
  display: flex;
  flex-direction: column;
  gap: 10px;
  position: relative;
  overflow: hidden;
}

.note-card:hover {
  border-color: #2196F3;
  box-shadow: 0 8px 24px rgba(33, 150, 243, 0.25), 0 0 0 3px rgba(33, 150, 243, 0.15);
  transform: translateY(-2px);
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

.loading-state,
.empty-state {
  text-align: center;
  padding: 48px;
  color: #999;
  font-size: 14px;
}
</style>

