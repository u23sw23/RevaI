<template>
  <div class="community-page">
    <h2 class="page-title">Community</h2>
    <p class="page-subtitle">Browse all public repositories shared by the community.</p>

    <!-- 公开科目列表 -->
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
            <div class="subject-creator">
              <span class="creator-label">Owner:</span>
              <span class="creator-name">{{ subject?.creatorUsername || 'Unknown' }}</span>
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
          <div class="breadcrumb">
            Community / {{ selectedSubject?.creatorUsername || 'Unknown' }} / {{ selectedSubject?.name || '' }}
          </div>
          <h3 class="notes-title">{{ selectedSubject?.name || '' }} - Notes</h3>
          <p class="notes-subtitle">
            Total {{ selectedSubject?.notes?.length || 0 }} notes. Click one to view details.
          </p>
        </div>
        <button class="back-btn" @click="backToSubjects">Back to community</button>
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
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { api } from '../utils/api'

const router = useRouter()

const publicSubjects = ref([])
const selectedSubject = ref(null)
const loading = ref(false)

// 加载所有公开的科目
const loadPublicSubjects = async () => {
  loading.value = true
  try {
    publicSubjects.value = await api.getPublicSubjects()
  } catch (error) {
    console.error('Failed to load public subjects:', error)
    publicSubjects.value = []
  } finally {
    loading.value = false
  }
}

const selectSubject = async (subject) => {
  // 如果科目还没有加载笔记，先加载
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

.subject-desc {
  font-size: 14px;
  color: #666;
  margin-top: 4px;
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
  color: #1976d2;
  font-weight: 500;
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

.breadcrumb {
  font-size: 12px;
  color: #999;
  margin-bottom: 4px;
}

.notes-title {
  font-size: 20px;
  font-weight: 600;
  color: #333;
  margin-bottom: 8px;
}

.notes-subtitle {
  color: #666;
  font-size: 14px;
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
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
}

.note-card {
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

.loading-state,
.empty-state {
  text-align: center;
  padding: 48px;
  color: #999;
  font-size: 14px;
}
</style>

