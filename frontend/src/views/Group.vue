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
            @click="goToGroupDetail(group.id)"
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

    <div v-if="showCreateModal" class="modal-overlay">
      <div class="modal">
        <div class="modal-header">
          <h3>Create group</h3>
          <button class="close-btn" @click="closeCreateGroup">×</button>
        </div>

        <div class="modal-body">
          
          <div class="form-item">
            <label class="form-label">Group name</label>
            <input
              v-model="groupName"
              type="text"
              class="form-input"
              placeholder="Please enter the group name"
            />
          </div>

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
import { useRouter } from 'vue-router'
import { api } from '../utils/api'

const router = useRouter()

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

const showCreateModal = ref(false)
const groupName = ref('')
const userSearchKeyword = ref('')
const userSearchResults = ref([])
const selectedUsers = ref([])
const isSearching = ref(false)
const isSubmitting = ref(false)
const errorMessage = ref('')

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

const goToGroupDetail = (groupId) => {
  router.push(`/group/${groupId}`)
}
</script>

<style scoped>
.group-page {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.groups-display {
  background: linear-gradient(to bottom, #FFFFFF 0%, #F8FBFF 100%);
  padding: 28px;
  border-radius: 16px;
  box-shadow: 0 4px 16px rgba(33, 150, 243, 0.1);
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
  font-size: 20px;
  font-weight: 700;
  background: linear-gradient(135deg, #1976D2 0%, #2196F3 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  margin-bottom: 20px;
  letter-spacing: -0.3px;
}

.groups-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
}

.group-card {
  background: linear-gradient(to bottom, #FFFFFF 0%, #F8FBFF 100%);
  border: 2px solid rgba(144, 202, 249, 0.8);
  border-radius: 12px;
  padding: 20px;
  transition: all 0.3s ease;
  cursor: pointer;
  box-shadow: 0 2px 8px rgba(33, 150, 243, 0.08);
  position: relative;
  overflow: hidden;
}

.group-card:hover {
  border-color: #2196F3;
  box-shadow: 0 6px 16px rgba(33, 150, 243, 0.25), 0 0 0 3px rgba(33, 150, 243, 0.15);
  transform: translateY(-2px);
}

.group-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.group-name {
  font-size: 18px;
  font-weight: 600;
  color: #1565C0;
  margin: 0;
}

.group-badge {
  background: linear-gradient(135deg, #2196F3 0%, #1976D2 100%);
  color: white;
  font-size: 11px;
  padding: 4px 10px;
  border-radius: 12px;
  font-weight: 600;
  box-shadow: 0 2px 6px rgba(33, 150, 243, 0.3);
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
  border: 2px solid rgba(187, 222, 251, 0.8);
  border-radius: 10px;
  background: linear-gradient(to bottom, #FFFFFF 0%, #F8FBFF 100%);
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.3s ease;
}

.action-btn:hover {
  background: linear-gradient(to bottom, #E3F2FD 0%, #BBDEFB 100%);
  border-color: #64B5F6;
  color: #1565C0;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(33, 150, 243, 0.15);
}

.add-group-btn {
  background: linear-gradient(135deg, #2196F3 0%, #1976D2 100%);
  color: white;
  border-color: #2196F3;
  box-shadow: 0 4px 12px rgba(33, 150, 243, 0.3);
}

.add-group-btn:hover {
  background: linear-gradient(135deg, #1976D2 0%, #1565C0 100%);
  color: white;
  box-shadow: 0 6px 16px rgba(33, 150, 243, 0.4);
  transform: translateY(-2px);
}

.modal-overlay {
  position: fixed;
  inset: 0;
  background-color: rgba(33, 150, 243, 0.2);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal {
  width: 520px;
  max-width: 90%;
  background: linear-gradient(to bottom, #FFFFFF 0%, #F8FBFF 100%);
  border-radius: 16px;
  box-shadow: 0 20px 60px rgba(33, 150, 243, 0.25);
  display: flex;
  flex-direction: column;
  border: 1px solid rgba(187, 222, 251, 0.6);
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
  padding: 10px 14px;
  border: 2px solid rgba(187, 222, 251, 0.6);
  border-radius: 10px;
  font-size: 14px;
  background: rgba(255, 255, 255, 0.9);
  transition: all 0.3s ease;
}

.form-input:focus {
  outline: none;
  border-color: #64B5F6;
  box-shadow: 0 0 0 4px rgba(33, 150, 243, 0.1);
  background: #FFFFFF;
}

.user-search-row {
  display: flex;
  gap: 8px;
}

.small-btn {
  padding: 8px 16px;
  border-radius: 8px;
  border: none;
  background: linear-gradient(135deg, #2196F3 0%, #1976D2 100%);
  color: #fff;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 2px 6px rgba(33, 150, 243, 0.3);
}

.small-btn:hover:not(:disabled) {
  background: linear-gradient(135deg, #1976D2 0%, #1565C0 100%);
  box-shadow: 0 4px 10px rgba(33, 150, 243, 0.4);
  transform: translateY(-1px);
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
  padding: 10px 14px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.3s ease;
  border-radius: 6px;
  margin: 4px;
}

.user-item:hover {
  background: linear-gradient(to right, rgba(227, 242, 253, 0.6) 0%, rgba(187, 222, 251, 0.3) 100%);
}

.user-item.selected {
  background: linear-gradient(to right, #E3F2FD 0%, rgba(187, 222, 251, 0.4) 100%);
  border: 1px solid rgba(144, 202, 249, 0.6);
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
  padding: 6px 12px;
  background: linear-gradient(135deg, #E3F2FD 0%, #BBDEFB 100%);
  color: #1565C0;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 500;
  box-shadow: 0 2px 4px rgba(33, 150, 243, 0.15);
  border: 1px solid rgba(144, 202, 249, 0.4);
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
  padding: 10px 20px;
  background: linear-gradient(135deg, #2196F3 0%, #1976D2 100%);
  color: #fff;
  border: none;
  border-radius: 10px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.3s ease;
  box-shadow: 0 4px 12px rgba(33, 150, 243, 0.3);
}

.primary-btn:hover:not(:disabled) {
  background: linear-gradient(135deg, #1976D2 0%, #1565C0 100%);
  box-shadow: 0 6px 16px rgba(33, 150, 243, 0.4);
  transform: translateY(-1px);
}

.primary-btn:disabled {
  background: linear-gradient(135deg, #90CAF9 0%, #BBDEFB 100%);
  cursor: not-allowed;
  opacity: 0.7;
}

.secondary-btn {
  padding: 10px 20px;
  background: linear-gradient(to bottom, #FFFFFF 0%, #F8FBFF 100%);
  color: #546E7A;
  border: 2px solid rgba(187, 222, 251, 0.8);
  border-radius: 10px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.3s ease;
}

.secondary-btn:hover {
  background: linear-gradient(to bottom, #E3F2FD 0%, #BBDEFB 100%);
  border-color: #64B5F6;
  color: #1565C0;
  transform: translateY(-1px);
}

.group-result-card {
  margin-top: 12px;
  padding: 16px 20px;
  border: 2px solid rgba(187, 222, 251, 0.6);
  border-radius: 12px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: linear-gradient(to bottom, rgba(255, 255, 255, 0.9) 0%, rgba(248, 251, 255, 0.6) 100%);
  box-shadow: 0 2px 8px rgba(33, 150, 243, 0.1);
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
