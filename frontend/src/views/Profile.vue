<template>
  <div class="profile-page">
    <h2 class="page-title">Personal Information</h2>
    
    <div class="profile-container">
      <div class="profile-card">
        <div class="avatar-section">
          <div class="avatar-large">
            <img 
              v-if="userInfo.avatar" 
              :src="getAvatarUrl(userInfo.avatar)" 
              alt="头像"
              @error="handleImageError"
            />
            <span v-else class="avatar-text">{{ userInfo.username?.charAt(0) || 'U' }}</span>
          </div>
          <label class="upload-avatar-btn" :class="{ uploading: uploadingAvatar }">
            {{ uploadingAvatar ? 'Uploading...' : 'Upload Avatar' }}
            <input
              type="file"
              ref="avatarInput"
              accept="image/*"
              style="display: none"
              @change="handleAvatarChange"
              :disabled="uploadingAvatar"
            />
          </label>
        </div>
        
        <div class="info-section">
          <div class="info-item">
            <label>username</label>
            <input 
              type="text" 
              v-model="userInfo.username"
              class="info-input"
              placeholder="请输入用户名"
            />
          </div>
          
          <div class="info-item">
            <label>email</label>
            <input 
              type="email" 
              v-model="userInfo.email"
              class="info-input"
              placeholder="Enter your email"
            />
          </div>
          
          <div class="info-item">
            <label>bio</label>
            <textarea 
              v-model="userInfo.bio"
              class="info-textarea"
              placeholder="Introduce yourself..."
              rows="4"
            ></textarea>
          </div>
          
          <div class="info-item">
            <label>createTime</label>
            <div class="info-display">{{ userInfo.createTime }}</div>
          </div>
          
          <div class="info-item">
            <label>repositoryCount</label>
            <div class="info-display">{{ userInfo.repositoryCount }}</div>
          </div>
        </div>
        
        <div class="action-section">
          <button class="save-btn" @click="saveProfile">Save Changes</button>
          <button class="cancel-btn" @click="resetProfile">Reset</button>
          <button class="logout-btn" @click="logout">Logout</button>
        </div>
      </div>
      
      <div class="stats-card">
        <h3>Statistics</h3>
        <div class="stats-grid">
          <div class="stat-item">
            <div class="stat-value">{{ userInfo.repositoryCount }}</div>
            <div class="stat-label">repositoryCount</div>
          </div>
          <div class="stat-item">
            <div class="stat-value">{{ userInfo.groupCount }}</div>
            <div class="stat-label">groupCount</div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { api } from '../utils/api'

const router = useRouter()

const props = defineProps({
  user: {
    type: Object,
    default: null
  }
})

const userInfo = ref({
  id: null,
  username: '',
  email: '',
  bio: '',
  avatar: '',
  createTime: '',
  repositoryCount: 0,
  groupCount: 0
})

const originalInfo = ref({})
const loading = ref(false)
const uploadingAvatar = ref(false)
const avatarInput = ref(null)

const getCurrentUserId = () => {
  
  if (props.user && props.user.id) {
    return props.user.id
  }
  
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

const loadUserProfile = async () => {
  const userId = getCurrentUserId()
  if (!userId) {
    console.warn('No user ID found, cannot load profile')
    return
  }

  loading.value = true
  try {
    const profile = await api.getUserProfile(userId)
    
    userInfo.value = {
      id: profile.id,
      username: profile.username || '',
      email: profile.email || '',
      bio: profile.bio || '',
      avatar: profile.avatar || '',
      createTime: profile.createTime || '',
      repositoryCount: profile.repositoryCount || 0,
      groupCount: profile.groupCount || 0
    }
    originalInfo.value = JSON.parse(JSON.stringify(userInfo.value))
    
    const savedData = {
      ...userInfo.value,
      name: userInfo.value.username 
    }
    localStorage.setItem('auth_user', JSON.stringify(savedData))
  } catch (error) {
    console.error('Failed to load user profile:', error)
    
    initUserInfoFromLocal()
  } finally {
    loading.value = false
  }
}

const initUserInfoFromLocal = () => {
  const savedUser = localStorage.getItem('auth_user')
  if (savedUser) {
    try {
      const source = JSON.parse(savedUser)
      userInfo.value = {
        id: source.id || null,
        username: source.username || source.name || '',
        email: source.email || '',
        bio: source.bio || '',
        avatar: source.avatar || '',
        createTime: source.createTime || '',
        repositoryCount: source.repositoryCount || 0,
        groupCount: source.groupCount || 0
      }
      originalInfo.value = JSON.parse(JSON.stringify(userInfo.value))
    } catch (e) {
      console.error('Failed to parse saved user:', e)
    }
  }
}

onMounted(() => {
  loadUserProfile()
})

watch(
  () => props.user,
  () => {
    loadUserProfile()
  },
  { deep: true }
)

const getAvatarUrl = (avatar) => {
  if (!avatar) return ''
  
  if (avatar.startsWith('http://') || avatar.startsWith('https://')) {
    return avatar
  }
  
  if (avatar.startsWith('/')) {
    return `http://localhost:8080${avatar}`
  }
  return avatar
}

const handleImageError = (event) => {
  
  event.target.style.display = 'none'
}

const handleAvatarChange = async (event) => {
  const file = event.target.files?.[0]
  if (!file) {
    return
  }

  if (!file.type.startsWith('image/')) {
    alert('只能上传图片文件')
    return
  }

  if (file.size > 5 * 1024 * 1024) {
    alert('图片大小不能超过 5MB')
    return
  }

  const userId = getCurrentUserId()
  if (!userId) {
    alert('请先登录')
    return
  }

  uploadingAvatar.value = true
  try {
    const formData = new FormData()
    formData.append('file', file)
    formData.append('userId', userId.toString())

    const avatarUrl = await api.uploadAvatar(userId, file)

    userInfo.value.avatar = avatarUrl
    originalInfo.value.avatar = avatarUrl

    const savedData = {
      ...userInfo.value,
      name: userInfo.value.username
    }
    localStorage.setItem('auth_user', JSON.stringify(savedData))
    
    alert('头像上传成功！')
  } catch (error) {
    console.error('Failed to upload avatar:', error)
    alert('头像上传失败：' + (error.message || '未知错误'))
  } finally {
    uploadingAvatar.value = false
    
    if (avatarInput.value) {
      avatarInput.value.value = ''
    }
  }
}

const saveProfile = async () => {
  const userId = getCurrentUserId()
  if (!userId) {
    alert('无法保存：未找到用户ID')
    return
  }

  loading.value = true
  try {
    
    const updateData = {
      username: userInfo.value.username,
      email: userInfo.value.email,
      bio: userInfo.value.bio,
      avatar: userInfo.value.avatar
    }

    const updated = await api.updateUserProfile(userId, updateData)
    
    userInfo.value = {
      ...userInfo.value,
      username: updated.username || userInfo.value.username,
      email: updated.email || userInfo.value.email,
      bio: updated.bio || userInfo.value.bio,
      avatar: updated.avatar || userInfo.value.avatar
    }
    originalInfo.value = JSON.parse(JSON.stringify(userInfo.value))
    
    const savedData = {
      ...userInfo.value,
      name: userInfo.value.username 
    }
    localStorage.setItem('auth_user', JSON.stringify(savedData))
    alert('保存成功！')
  } catch (error) {
    console.error('Failed to save profile:', error)
    alert('保存失败：' + (error.message || '未知错误'))
  } finally {
    loading.value = false
  }
}

const resetProfile = () => {
  userInfo.value = JSON.parse(JSON.stringify(originalInfo.value))
}

const logout = () => {
  
  localStorage.removeItem('auth_username')
  localStorage.removeItem('auth_user')
  
  userInfo.value = {
    id: null,
    username: '',
    email: '',
    bio: '',
    avatar: '',
    createTime: '',
    repositoryCount: 0,
    groupCount: 0
  }
  
  router.push('/repositories').then(() => {
    window.location.reload()
  })
}
</script>

<style scoped>
.profile-page {
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

.profile-container {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 24px;
}

.profile-card {
  background: linear-gradient(to bottom, #FFFFFF 0%, #F8FBFF 100%);
  padding: 32px;
  border-radius: 16px;
  box-shadow: 0 4px 16px rgba(33, 150, 243, 0.1);
  border: 2px solid rgba(71, 169, 249, 0.8);
}

.avatar-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 32px;
  padding-bottom: 32px;
  border-bottom: 1px solid #e0e0e0;
}

.avatar-large {
  width: 120px;
  height: 120px;
  border-radius: 50%;
  background: linear-gradient(135deg, #BBDEFB 0%, #90CAF9 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 16px;
  overflow: hidden;
  box-shadow: 0 4px 12px rgba(33, 150, 243, 0.2);
  border: 3px solid rgba(255, 255, 255, 0.8);
}

.avatar-large img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-text {
  font-size: 48px;
  color: #666;
  font-weight: 500;
}

.upload-avatar-btn {
  padding: 10px 20px;
  background: linear-gradient(to bottom, #FFFFFF 0%, #F8FBFF 100%);
  border: 2px solid rgba(187, 222, 251, 0.8);
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.3s ease;
}

.upload-avatar-btn:hover:not(.uploading) {
  background: linear-gradient(to bottom, #E3F2FD 0%, #BBDEFB 100%);
  border-color: #64B5F6;
  color: #1565C0;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(33, 150, 243, 0.15);
}

.upload-avatar-btn.uploading {
  opacity: 0.6;
  cursor: not-allowed;
}

.info-section {
  display: flex;
  flex-direction: column;
  gap: 20px;
  margin-bottom: 32px;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.info-item label {
  font-size: 14px;
  font-weight: 500;
  color: #666;
}

.info-input,
.info-textarea {
  padding: 10px 14px;
  border: 2px solid rgba(187, 222, 251, 0.6);
  border-radius: 10px;
  font-size: 14px;
  font-family: inherit;
  transition: all 0.3s ease;
  background: rgba(255, 255, 255, 0.9);
}

.info-input:focus,
.info-textarea:focus {
  outline: none;
  border-color: #64B5F6;
  box-shadow: 0 0 0 4px rgba(33, 150, 243, 0.1);
  background: #FFFFFF;
}

.info-textarea {
  resize: vertical;
}

.info-display {
  padding: 10px 14px;
  background: linear-gradient(to bottom, #F8FBFF 0%, rgba(227, 242, 253, 0.4) 100%);
  border-radius: 10px;
  font-size: 14px;
  color: #546E7A;
  border: 1px solid rgba(187, 222, 251, 0.6);
}

.action-section {
  display: flex;
  gap: 12px;
  padding-top: 24px;
  border-top: 1px solid #e0e0e0;
}

.save-btn,
.cancel-btn {
  padding: 12px 24px;
  border-radius: 10px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.3s ease;
  border: none;
  font-weight: 500;
}

.save-btn {
  background: linear-gradient(135deg, #2196F3 0%, #1976D2 100%);
  color: white;
  box-shadow: 0 4px 12px rgba(33, 150, 243, 0.3);
}

.save-btn:hover {
  background: linear-gradient(135deg, #1976D2 0%, #1565C0 100%);
  box-shadow: 0 6px 16px rgba(33, 150, 243, 0.4);
  transform: translateY(-1px);
}

.cancel-btn {
  background: linear-gradient(to bottom, #FFFFFF 0%, #F8FBFF 100%);
  color: #546E7A;
  border: 2px solid rgba(187, 222, 251, 0.8);
}

.cancel-btn:hover {
  background: linear-gradient(to bottom, #E3F2FD 0%, #BBDEFB 100%);
  border-color: #64B5F6;
  color: #1565C0;
  transform: translateY(-1px);
}

.logout-btn {
  padding: 12px 24px;
  border-radius: 10px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.3s ease;
  border: 2px solid rgba(244, 67, 54, 0.6);
  background: linear-gradient(to bottom, #FFFFFF 0%, rgba(255, 235, 238, 0.4) 100%);
  color: #F44336;
  font-weight: 500;
}

.logout-btn:hover {
  background: linear-gradient(to bottom, #FFEBEE 0%, #FFCDD2 100%);
  border-color: #E53935;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(244, 67, 54, 0.2);
}

.stats-card {
  background: linear-gradient(to bottom, #FFFFFF 0%, #F8FBFF 100%);
  padding: 28px;
  border-radius: 16px;
  box-shadow: 0 4px 16px rgba(33, 150, 243, 0.1);
  height: fit-content;
}

.stats-card h3 {
  font-size: 18px;
  font-weight: 700;
  color: #1565C0;
  margin-bottom: 24px;
  letter-spacing: -0.3px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.stat-item {
  text-align: center;
  padding: 20px;
  background: linear-gradient(to bottom, #F8FBFF 0%, rgba(227, 242, 253, 0.4) 100%);
  border-radius: 12px;
  border: 1px solid rgba(187, 222, 251, 0.6);
  box-shadow: 0 2px 8px rgba(33, 150, 243, 0.08);
}

.stat-value {
  font-size: 28px;
  font-weight: 800;
  background: linear-gradient(135deg, #1976D2 0%, #2196F3 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  margin-bottom: 6px;
}

.stat-label {
  font-size: 12px;
  color: #666;
}
</style>

