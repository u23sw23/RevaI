<template>
  <div class="group-page">
    <div class="groups-display">
      <div v-if="loadingGroups" class="loading-state">
        <p>Loading groups...</p>
      </div>
      <div v-else-if="groups.length === 0" class="empty-state">
        <p class="groups-desc">You haven't joined any groups yet. Create a new group or join an existing one.</p>
      </div>
      <div v-else class="groups-list">
        <h3 class="groups-title">My Groups ({{ groups.length }})</h3>
        <div class="groups-grid">
          <div
            v-for="group in groups"
            :key="group.id"
            class="group-card"
          >
            <div class="group-card-header">
              <h4 class="group-name">{{ group.name }}</h4>
              <span class="group-badge" v-if="group.creatorId === currentUserId">Creator</span>
            </div>
            <div class="group-card-info">
              <div class="group-info-item">
                <span class="info-label">Members:</span>
                <span class="info-value">{{ group.memberCount }}</span>
              </div>
              <div class="group-info-item">
                <span class="info-label">Created:</span>
                <span class="info-value">{{ group.createTime }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
    
    <div class="group-actions">
      <button class="action-btn add-group-btn" @click="openCreateGroup">
        create group...
      </button>
      <button class="action-btn join-group-btn" @click="openJoinGroup">
        join group...
      </button>
    </div>

    <!-- 创建群组弹窗 -->
    <div v-if="showCreateModal" class="modal-overlay">
      <div class="modal">
        <div class="modal-header">
          <h3>Create group</h3>
          <button class="close-btn" @click="closeCreateGroup">×</button>
        </div>

        <div class="modal-body">
          <!-- 群组名称 -->
          <div class="form-item">
            <label class="form-label">Group name</label>
            <input
              v-model="groupName"
              type="text"
              class="form-input"
              placeholder="Please enter the group name"
            />
          </div>

          <!-- 搜索并添加成员 -->
          <div class="form-item">
            <label class="form-label">Add member (search by username)</label>
            <div class="user-search-row">
              <input
                v-model="userSearchKeyword"
                type="text"
                class="form-input"
                placeholder="Please enter user name"
                @keyup.enter="searchUsers"
              />
              <button
                class="small-btn"
                @click="searchUsers"
                :disabled="isSearching"
              >
                {{ isSearching ? '搜索中...' : '搜索' }}
              </button>
            </div>

            <!-- 搜索结果列表 -->
            <div v-if="userSearchResults.length" class="user-list">
              <div
                v-for="user in userSearchResults"
                :key="user.id"
                class="user-item"
                :class="{ selected: isUserSelected(user.id) }"
                @click="toggleUser(user)"
              >
                <span>{{ user.username || user.name || 'Unknown' }}</span>
                <span class="user-tag">
                  {{ isUserSelected(user.id) ? 'Selected' : 'Add' }}
                </span>
              </div>
            </div>

            <!-- 已选择成员 -->
            <div v-if="selectedUsers.length" class="selected-users">
              <div
                v-for="user in selectedUsers"
                :key="user.id"
                class="selected-user-chip"
              >
                {{ user.username || user.name || 'Unknown' }}
                <span class="remove-chip" @click.stop="removeUser(user.id)">×</span>
              </div>
            </div>
          </div>

          <p v-if="errorMessage" class="error-text">{{ errorMessage }}</p>
        </div>

        <div class="modal-footer">
          <button class="secondary-btn" @click="closeCreateGroup">
            Cancel
          </button>
          <button
            class="primary-btn"
            @click="submitCreateGroup"
            :disabled="isSubmitting"
          >
            {{ isSubmitting ? 'Creating...' : 'Create group' }}
          </button>
        </div>
      </div>
    </div>

    <!-- 加入群组弹窗 -->
    <div v-if="showJoinModal" class="modal-overlay">
      <div class="modal">
        <div class="modal-header">
          <h3>Join group</h3>
          <button class="close-btn" @click="closeJoinGroup">×</button>
        </div>

        <div class="modal-body">
          <div class="form-item">
            <label class="form-label">Group name</label>
            <input
              v-model="groupNameToJoin"
              type="text"
              class="form-input"
              placeholder="Please enter group name"
              @keyup.enter="searchGroupByName"
            />
          </div>

          <button
            class="primary-btn"
            style="width: 120px; margin-bottom: 12px;"
            @click="searchGroupByName"
            :disabled="isSearchingGroup"
          >
            {{ isSearchingGroup ? 'Searching...' : 'Search group' }}
          </button>

          <p class="hint">Search by the group name and click "Join" after finding it.</p>

          <!-- 查询结果 -->
          <div v-if="searchedGroup" class="group-result-card">
            <div>
              <div class="group-name">{{ searchedGroup.name }}</div>
              <div class="group-info">ID: {{ searchedGroup.id }}</div>
            </div>
            <button
              class="primary-btn"
              @click="joinGroup"
              :disabled="isJoining"
            >
              {{ isJoining ? 'Joining...' : 'Join' }}
            </button>
          </div>

          <p v-if="joinError" class="error-text">{{ joinError }}</p>
        </div>

        <div class="modal-footer">
          <button class="secondary-btn" @click="closeJoinGroup">
            Cancel
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { api } from '../utils/api'

// 获取当前用户ID
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

const currentUserId = ref(null)
const groups = ref([])
const loadingGroups = ref(false)

// 加载用户群组列表
const loadGroups = async () => {
  const userId = getCurrentUserId()
  if (!userId) {
    groups.value = []
    return
  }

  currentUserId.value = userId
  loadingGroups.value = true
  try {
    groups.value = await api.getUserGroups(userId)
  } catch (error) {
    console.error('Failed to load groups:', error)
    groups.value = []
  } finally {
    loadingGroups.value = false
  }
}

onMounted(() => {
  loadGroups()
})

// 创建群组弹窗状态
const showCreateModal = ref(false)
const groupName = ref('')
const userSearchKeyword = ref('')
const userSearchResults = ref([])
const selectedUsers = ref([])
const isSearching = ref(false)
const isSubmitting = ref(false)
const errorMessage = ref('')
// 加入群组弹窗状态
const showJoinModal = ref(false)
const groupNameToJoin = ref('')
const searchedGroup = ref(null)
const isSearchingGroup = ref(false)
const isJoining = ref(false)
const joinError = ref('')

const openCreateGroup = () => {
  resetCreateForm()
  showCreateModal.value = true
}

const closeCreateGroup = () => {
  showCreateModal.value = false
}

const resetCreateForm = () => {
  groupName.value = ''
  userSearchKeyword.value = ''
  userSearchResults.value = []
  selectedUsers.value = []
  errorMessage.value = ''
  isSearching.value = false
  isSubmitting.value = false
}

// 搜索用户
const searchUsers = async () => {
  const keyword = userSearchKeyword.value.trim()
  if (!keyword) {
    errorMessage.value = 'Please enter the keyword of the username you want to search for.'
    return
  }

  errorMessage.value = ''
  isSearching.value = true

  try {
    const res = await fetch(`/api/users?q=${encodeURIComponent(keyword)}`, {
      method: 'GET'
    })

    const data = await res.json()
    if (!res.ok || !data.success) {
      throw new Error(data.message || 'Failed to search user')
    }

    userSearchResults.value = data.list || []
  } catch (error) {
    console.error('Error in searching user:', error)
    errorMessage.value = error.message || 'Failed to search user. Please try again later.'
    userSearchResults.value = []
  } finally {
    isSearching.value = false
  }
}

const isUserSelected = (userId) => {
  return selectedUsers.value.some((u) => u.id === userId)
}

const toggleUser = (user) => {
  if (isUserSelected(user.id)) {
    selectedUsers.value = selectedUsers.value.filter((u) => u.id !== user.id)
  } else {
    selectedUsers.value = [...selectedUsers.value, user]
  }
}

const removeUser = (userId) => {
  selectedUsers.value = selectedUsers.value.filter((u) => u.id !== userId)
}

// 提交创建群组
const submitCreateGroup = async () => {
  if (!groupName.value.trim()) {
    errorMessage.value = 'Please enter group name'
    return
  }
  if (!selectedUsers.value.length) {
    errorMessage.value = 'Please select at least one member.'
    return
  }

  const userId = getCurrentUserId()
  if (!userId) {
    errorMessage.value = 'Please login first'
    return
  }

  errorMessage.value = ''
  isSubmitting.value = true

  try {
    const payload = {
      name: groupName.value.trim(),
      memberIds: selectedUsers.value.map((u) => u.id)
    }

    // 创建群组向后端发送请求，传递 userId 作为 creatorId
    const res = await fetch(`/api/groups?userId=${userId}`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(payload)
    })

    const data = await res.json()
    if (!res.ok || !data.success) {
      throw new Error(data.message || 'Failed to create the group')
    }

    // 成功后关闭弹窗并刷新群组列表
    closeCreateGroup()
    alert('Group created successfully!')
    loadGroups()
  } catch (error) {
    console.error('Error occurred while creating the group:', error)
    errorMessage.value = error.message || 'Failed to create the group. Please try again later.'
  } finally {
    isSubmitting.value = false
  }
}

// 打开/关闭加入群组弹窗
const openJoinGroup = () => {
  resetJoinForm()
  showJoinModal.value = true
}

const closeJoinGroup = () => {
  showJoinModal.value = false
}

const resetJoinForm = () => {
  groupNameToJoin.value = ''
  searchedGroup.value = null
  joinError.value = ''
  isSearchingGroup.value = false
  isJoining.value = false
}

// 根据群组名称查询
const searchGroupByName = async () => {
  const name = groupNameToJoin.value.trim()
  if (!name) {
    joinError.value = 'Please enter group name'
    return
  }

  joinError.value = ''
  searchedGroup.value = null
  isSearchingGroup.value = true

  try {
    const res = await fetch(`/api/groups?name=${encodeURIComponent(name)}`, {
      method: 'GET'
    })

    const data = await res.json()
    if (!res.ok || !data.success) {
      throw new Error(data.message || 'Failed to find the group')
    }

    searchedGroup.value = data.group || null
    if (!searchedGroup.value) {
      joinError.value = 'Group not found. Please check the group name.'
    }
  } catch (error) {
    console.error('Error in finding the group:', error)
    joinError.value = error.message || 'Failed to find the group. Please try again later.'
  } finally {
    isSearchingGroup.value = false
  }
}

// 加入群组
const joinGroup = async () => {
  if (!searchedGroup.value?.id) {
    joinError.value = 'Please first search for the target group.'
    return
  }

  const userId = getCurrentUserId()
  if (!userId) {
    joinError.value = 'Please login first'
    return
  }

  joinError.value = ''
  isJoining.value = true

  try {
    const payload = {
      groupId: searchedGroup.value.id
    }

    const res = await fetch(`/api/groups/join?userId=${userId}`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(payload)
    })

    const data = await res.json()
    if (!res.ok || !data.success) {
      throw new Error(data.message || 'Failed to join the group')
    }

    // 成功后关闭弹窗并刷新群组列表
    closeJoinGroup()
    alert('Successfully joined the group!')
    loadGroups()
  } catch (error) {
    console.error('Error in joining the group:', error)
    joinError.value = error.message || 'Failed to join the group. Please try again later.'
  } finally {
    isJoining.value = false
  }
}
</script>

<style scoped>
.group-page {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.groups-display {
  background-color: #fff;
  padding: 24px;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
  min-height: 400px;
}

.loading-state,
.empty-state {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 400px;
  text-align: center;
}

.groups-desc {
  font-size: 14px;
  color: #666;
  line-height: 1.6;
}

.groups-title {
  font-size: 18px;
  font-weight: 600;
  color: #333;
  margin-bottom: 16px;
}

.groups-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
}

.group-card {
  background-color: #f8f9fa;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  padding: 16px;
  transition: all 0.2s;
  cursor: pointer;
}

.group-card:hover {
  border-color: #1976d2;
  box-shadow: 0 2px 8px rgba(25, 118, 210, 0.15);
  transform: translateY(-2px);
}

.group-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.group-name {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin: 0;
}

.group-badge {
  background-color: #1976d2;
  color: white;
  font-size: 11px;
  padding: 4px 8px;
  border-radius: 12px;
  font-weight: 500;
}

.group-card-info {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.group-info-item {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
}

.info-label {
  color: #666;
}

.info-value {
  color: #333;
  font-weight: 500;
}

.group-actions {
  display: flex;
  gap: 16px;
}

.action-btn {
  flex: 1;
  padding: 12px 24px;
  border: 1px solid #ddd;
  border-radius: 4px;
  background-color: #fff;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.2s;
}

.action-btn:hover {
  background-color: #f5f5f5;
  border-color: #1976d2;
  color: #1976d2;
}

.add-group-btn {
  background-color: #1976d2;
  color: white;
  border-color: #1976d2;
}

.add-group-btn:hover {
  background-color: #1565c0;
  color: white;
}

/* 弹窗样式 */
.modal-overlay {
  position: fixed;
  inset: 0;
  background-color: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal {
  width: 520px;
  max-width: 90%;
  background-color: #fff;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  display: flex;
  flex-direction: column;
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid #eee;
}

.modal-header h3 {
  margin: 0;
  font-size: 16px;
}

.close-btn {
  border: none;
  background: transparent;
  font-size: 20px;
  cursor: pointer;
}

.modal-body {
  padding: 16px 20px;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  padding: 12px 20px 16px;
  border-top: 1px solid #eee;
}

.form-item {
  margin-bottom: 16px;
}

.form-label {
  display: block;
  margin-bottom: 6px;
  font-size: 13px;
  color: #555;
}

.form-input {
  width: 100%;
  padding: 8px 10px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
}

.user-search-row {
  display: flex;
  gap: 8px;
}

.small-btn {
  padding: 8px 14px;
  border-radius: 4px;
  border: none;
  background-color: #1976d2;
  color: #fff;
  font-size: 13px;
  cursor: pointer;
}

.small-btn:disabled {
  background-color: #90caf9;
  cursor: not-allowed;
}

.hint {
  margin-top: 4px;
  font-size: 12px;
  color: #999;
}

.user-list {
  margin-top: 8px;
  border: 1px solid #eee;
  border-radius: 4px;
  max-height: 180px;
  overflow-y: auto;
}

.user-item {
  display: flex;
  justify-content: space-between;
  padding: 8px 10px;
  font-size: 14px;
  cursor: pointer;
}

.user-item:hover {
  background-color: #f5f5f5;
}

.user-item.selected {
  background-color: #e3f2fd;
}

.user-tag {
  font-size: 12px;
  color: #666;
}

.selected-users {
  margin-top: 8px;
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.selected-user-chip {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 8px;
  background-color: #e3f2fd;
  color: #1976d2;
  border-radius: 999px;
  font-size: 12px;
}

.remove-chip {
  cursor: pointer;
}

.error-text {
  margin-top: 4px;
  font-size: 12px;
  color: #d32f2f;
}

.primary-btn {
  padding: 8px 16px;
  background-color: #1976d2;
  color: #fff;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
}

.primary-btn:disabled {
  background-color: #90caf9;
  cursor: not-allowed;
}

.secondary-btn {
  padding: 8px 16px;
  background-color: #f5f5f5;
  color: #333;
  border: 1px solid #ddd;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
}

.group-result-card {
  margin-top: 12px;
  padding: 12px 14px;
  border: 1px solid #ddd;
  border-radius: 6px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.group-name {
  font-weight: 600;
  color: #333;
}

.group-info {
  font-size: 12px;
  color: #777;
}
</style>
