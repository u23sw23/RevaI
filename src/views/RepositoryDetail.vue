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
      <div class="note-body markdown-body" v-html="renderedContent"></div>
    </div>

    <div v-else class="empty-state">
      Note not found. Please go back and select again.
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { marked } from 'marked'
import { findNoteById } from '../data/notes'

marked.setOptions({
  breaks: true,
  gfm: true,
})

const router = useRouter()

const props = defineProps({
  id: {
    type: String,
    required: true
  }
})

const { subject, note } = findNoteById(props.id)
const repositoryName = computed(() => note?.title || props.id)

const renderedContent = computed(() => {
  if (!note?.content) return ''
  try {
    return marked.parse(note.content)
  } catch (e) {
    return note.content.replace(/\n/g, '<br />')
  }
})

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
  line-height: 1.8;
  color: #333;
  font-size: 15px;
  background: #fafafa;
  padding: 24px 28px;
  border-radius: 12px;
  border: 1px solid #e8e8e8;
}

.markdown-body :deep(h1) {
  font-size: 1.8em;
  font-weight: 700;
  color: #1a1a2e;
  margin: 24px 0 16px 0;
  padding-bottom: 8px;
  border-bottom: 2px solid #1976d2;
}

.markdown-body :deep(h2) {
  font-size: 1.5em;
  font-weight: 600;
  color: #1976d2;
  margin: 20px 0 12px 0;
  padding-bottom: 6px;
  border-bottom: 1px solid #e0e0e0;
}

.markdown-body :deep(h3) {
  font-size: 1.25em;
  font-weight: 600;
  color: #333;
  margin: 16px 0 10px 0;
}

.markdown-body :deep(h4),
.markdown-body :deep(h5),
.markdown-body :deep(h6) {
  font-size: 1.1em;
  font-weight: 600;
  color: #444;
  margin: 14px 0 8px 0;
}

.markdown-body :deep(p) {
  margin: 10px 0;
  line-height: 1.8;
}

.markdown-body :deep(ul),
.markdown-body :deep(ol) {
  margin: 12px 0;
  padding-left: 24px;
}

.markdown-body :deep(li) {
  margin: 6px 0;
  line-height: 1.7;
}

.markdown-body :deep(strong) {
  font-weight: 700;
  color: #c62828;
  background: linear-gradient(180deg, transparent 60%, #ffecb3 60%);
  padding: 0 2px;
}

.markdown-body :deep(em) {
  font-style: italic;
  color: #555;
}

.markdown-body :deep(code) {
  background: #e8f4fd;
  color: #1565c0;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 0.9em;
  font-family: 'SF Mono', 'Monaco', 'Consolas', monospace;
}

.markdown-body :deep(pre) {
  background: #263238;
  color: #eeffff;
  padding: 16px;
  border-radius: 8px;
  overflow-x: auto;
  margin: 16px 0;
}

.markdown-body :deep(pre code) {
  background: none;
  color: inherit;
  padding: 0;
}

.markdown-body :deep(blockquote) {
  margin: 16px 0;
  padding: 12px 20px;
  border-left: 4px solid #1976d2;
  background: #e3f2fd;
  color: #333;
  border-radius: 0 8px 8px 0;
}

.markdown-body :deep(table) {
  width: 100%;
  border-collapse: collapse;
  margin: 16px 0;
}

.markdown-body :deep(th),
.markdown-body :deep(td) {
  border: 1px solid #ddd;
  padding: 10px 12px;
  text-align: left;
}

.markdown-body :deep(th) {
  background: #f5f5f5;
  font-weight: 600;
}

.markdown-body :deep(tr:nth-child(even)) {
  background: #fafafa;
}

.markdown-body :deep(hr) {
  border: none;
  border-top: 2px dashed #e0e0e0;
  margin: 24px 0;
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
