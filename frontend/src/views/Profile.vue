<template>
  <div class="profile-page">
    <h2 class="page-title">个人信息</h2>
    
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
            {{ uploadingAvatar ? '上传中...' : '上传头像' }}
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
            <label>用户名</label>
            <input 
              type="text" 
              v-model="userInfo.username"
              class="info-input"
              placeholder="请输入用户名"
            />
          </div>
          
          <div class="info-item">
            <label>邮箱</label>
            <input 
              type="email" 
              v-model="userInfo.email"
              class="info-input"
              placeholder="请输入邮箱"
            />
          </div>
          
          <div class="info-item">
            <label>个人简介</label>
            <textarea 
              v-model="userInfo.bio"
              class="info-textarea"
              placeholder="介绍一下自己..."
              rows="4"
            ></textarea>
          </div>
          
          <div class="info-item">
            <label>创建时间</label>
            <div class="info-display">{{ userInfo.createTime }}</div>
          </div>
          
          <div class="info-item">
            <label>已创建仓库数</label>
            <div class="info-display">{{ userInfo.repositoryCount }}</div>
          </div>
        </div>
        
        <div class="action-section">
          <button class="save-btn" @click="saveProfile">保存修改</button>
          <button class="cancel-btn" @click="resetProfile">重置</button>
          <button class="logout-btn" @click="logout">退出登录</button>
        </div>
      </div>
      
      <div class="stats-card">
        <h3>统计信息</h3>
        <div class="stats-grid">
          <div class="stat-item">
            <div class="stat-value">{{ userInfo.repositoryCount }}</div>
            <div class="stat-label">仓库数</div>
          </div>
          <div class="stat-item">
            <div class="stat-value">{{ userInfo.groupCount }}</div>
            <div class="stat-label">加入群组</div>
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

// 从 Layout 传入的当前用户信息（来源于登录/注册成功或本地存储）
const props = defineProps({
  user: {
    type: Object,
    default: null
  }
})

// 当前页面编辑态下的用户信息
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

// 获取当前用户ID
const getCurrentUserId = () => {
  // 优先从 props.user 获取
  if (props.user && props.user.id) {
    return props.user.id
  }
  // 从 localStorage 获取
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

// 从后端 API 加载用户信息和统计数据
const loadUserProfile = async () => {
  const userId = getCurrentUserId()
  if (!userId) {
    console.warn('No user ID found, cannot load profile')
    return
  }

  loading.value = true
  try {
    const profile = await api.getUserProfile(userId)
    // 映射后端字段到前端字段
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
    // 同步更新本地存储（兼容旧格式，添加 name 字段用于其他组件）
    const savedData = {
      ...userInfo.value,
      name: userInfo.value.username // 兼容其他组件使用的 name 字段
    }
    localStorage.setItem('auth_user', JSON.stringify(savedData))
  } catch (error) {
    console.error('Failed to load user profile:', error)
    // 如果API失败，回退到从localStorage加载
    initUserInfoFromLocal()
  } finally {
    loading.value = false
  }
}

// 从本地存储初始化（作为回退方案）
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

// 当父组件传入的 user 变化时，重新加载
watch(
  () => props.user,
  () => {
    loadUserProfile()
  },
  { deep: true }
)

// 获取头像完整URL
const getAvatarUrl = (avatar) => {
  if (!avatar) return ''
  // 如果已经是完整URL，直接返回
  if (avatar.startsWith('http://') || avatar.startsWith('https://')) {
    return avatar
  }
  // 如果是相对路径，添加后端地址
  if (avatar.startsWith('/')) {
    return `http://localhost:8080${avatar}`
  }
  return avatar
}

// 处理图片加载错误
const handleImageError = (event) => {
  // 如果图片加载失败，隐藏图片，显示默认头像
  event.target.style.display = 'none'
}

// 处理头像文件选择
const handleAvatarChange = async (event) => {
  const file = event.target.files?.[0]
  if (!file) {
    return
  }

  // 验证文件类型
  if (!file.type.startsWith('image/')) {
    alert('只能上传图片文件')
    return
  }

  // 验证文件大小（限制为 5MB）
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
    
    // 更新用户信息中的头像
    userInfo.value.avatar = avatarUrl
    originalInfo.value.avatar = avatarUrl
    
    // 同步更新本地存储
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
    // 清空文件输入，允许重复选择同一文件
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
    // 准备要保存的数据（只包含后端可更新的字段）
    const updateData = {
      username: userInfo.value.username,
      email: userInfo.value.email,
      bio: userInfo.value.bio,
      avatar: userInfo.value.avatar
    }

    const updated = await api.updateUserProfile(userId, updateData)
    // 更新用户信息（保留统计数据）
    userInfo.value = {
      ...userInfo.value,
      username: updated.username || userInfo.value.username,
      email: updated.email || userInfo.value.email,
      bio: updated.bio || userInfo.value.bio,
      avatar: updated.avatar || userInfo.value.avatar
    }
    originalInfo.value = JSON.parse(JSON.stringify(userInfo.value))
    // 同步更新本地存储（兼容旧格式）
    const savedData = {
      ...userInfo.value,
      name: userInfo.value.username // 兼容其他组件使用的 name 字段
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

// 退出登录：清除本地登录状态并回到游客视图
const logout = () => {
  // 清除 TopHeader 使用的本地登录标记（如有 token 也可一并清除）
  localStorage.removeItem('auth_username')
  localStorage.removeItem('auth_user')
  // 根据需要可清空本地用户信息
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
  // 跳转回首页（或任意页面），并刷新一次让 TopHeader 重新以游客状态渲染
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
  font-size: 20px;
  font-weight: 600;
  color: #333;
  margin-bottom: 8px;
}

.profile-container {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 24px;
}

.profile-card {
  background-color: #fff;
  padding: 32px;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
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
  background-color: #e0e0e0;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 16px;
  overflow: hidden;
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
  padding: 8px 16px;
  background-color: #f5f5f5;
  border: 1px solid #ddd;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.2s;
}

.upload-avatar-btn:hover:not(.uploading) {
  background-color: #e0e0e0;
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
  padding: 10px 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
  font-family: inherit;
  transition: all 0.2s;
}

.info-input:focus,
.info-textarea:focus {
  outline: none;
  border-color: #1976d2;
  box-shadow: 0 0 0 2px rgba(25, 118, 210, 0.1);
}

.info-textarea {
  resize: vertical;
}

.info-display {
  padding: 10px 12px;
  background-color: #f5f5f5;
  border-radius: 4px;
  font-size: 14px;
  color: #666;
}

.action-section {
  display: flex;
  gap: 12px;
  padding-top: 24px;
  border-top: 1px solid #e0e0e0;
}

.save-btn,
.cancel-btn {
  padding: 10px 24px;
  border-radius: 4px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
  border: none;
}

.save-btn {
  background-color: #1976d2;
  color: white;
}

.save-btn:hover {
  background-color: #1565c0;
}

.cancel-btn {
  background-color: #f5f5f5;
  color: #666;
  border: 1px solid #ddd;
}

.cancel-btn:hover {
  background-color: #e0e0e0;
}

.logout-btn {
  padding: 10px 24px;
  border-radius: 4px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
  border: 1px solid #f44336;
  background-color: #fff;
  color: #f44336;
}

.logout-btn:hover {
  background-color: #ffebee;
}

.stats-card {
  background-color: #fff;
  padding: 24px;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
  height: fit-content;
}

.stats-card h3 {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin-bottom: 20px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.stat-item {
  text-align: center;
  padding: 16px;
  background-color: #f5f5f5;
  border-radius: 8px;
}

.stat-value {
  font-size: 24px;
  font-weight: 600;
  color: #1976d2;
  margin-bottom: 4px;
}

.stat-label {
  font-size: 12px;
  color: #666;
}
</style>

