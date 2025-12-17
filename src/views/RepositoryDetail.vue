<template>
  <div class="repository-detail-page">
    <div class="detail-header">
      <div>
        <div class="breadcrumb" v-if="subject">Subject / {{ subject.name }}</div>
        <h2 class="page-title">{{ note?.title || 'Note not found' }}</h2>
        <p class="note-updated" v-if="note">Updated at: {{ note.updatedAt }}</p>
      </div>
      <button class="back-btn" @click="backToList">Back to notes</button>
    </div>

    <div class="note-content" v-if="note">
      <h3 class="section-title">Note content</h3>
      <pre class="note-body">{{ note.content }}</pre>
    </div>

    <div v-else class="empty-state">
      Note not found. Please go back and select again.
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { findNoteById } from '../data/notes'

const router = useRouter()

const props = defineProps({
  id: {
    type: String,
    required: true
  }
})

const { subject, note } = findNoteById(props.id)
const repositoryName = computed(() => note?.title || props.id)

const backToList = () => {
  router.push('/repositories')
}
</script>

<style scoped>
.repository-detail-page {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.breadcrumb {
  font-size: 12px;
  color: #999;
  margin-bottom: 4px;
}

.page-title {
  font-size: 20px;
  font-weight: 600;
  color: #333;
  margin-bottom: 8px;
}

.note-updated {
  color: #666;
  font-size: 13px;
}

.note-content {
  background-color: #fff;
  padding: 24px;
  border-radius: 12px;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.06);
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 12px;
}

.note-body {
  white-space: pre-wrap;
  line-height: 1.7;
  color: #333;
  font-size: 14px;
  background: #f8f8f8;
  padding: 16px;
  border-radius: 8px;
  border: 1px solid #eee;
}

.empty-state {
  background-color: #fff7e6;
  border: 1px solid #ffe0b3;
  color: #b36b00;
  padding: 16px;
  border-radius: 8px;
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
</style>
