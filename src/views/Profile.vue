<template>
  <div class="profile-page">
    <h2 class="page-title">个人信息</h2>
    
    <div class="profile-container">
      <div class="profile-card">
        <div class="avatar-section">
          <div class="avatar-large">
            <img v-if="userInfo.avatar" :src="userInfo.avatar" alt="头像" />
            <span v-else class="avatar-text">{{ userInfo.name?.charAt(0) || 'U' }}</span>
          </div>
          <button class="upload-avatar-btn">上传头像</button>
        </div>
        
        <div class="info-section">
          <div class="info-item">
            <label>用户名</label>
            <input 
              type="text" 
              v-model="userInfo.name"
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
  name: '',
  email: '',
  bio: '',
  avatar: '',
  createTime: '',
  repositoryCount: 0,
  cardCount: 0,
  examCount: 0,
  groupCount: 0
})

const originalInfo = JSON.parse(JSON.stringify(userInfo.value))

// 根据 props.user 或本地存储初始化 userInfo
const initUserInfo = () => {
  let source = props.user
  if (!source) {
    const savedUser = localStorage.getItem('auth_user')
    if (savedUser) {
      try {
        source = JSON.parse(savedUser)
      } catch (e) {
        source = null
      }
    }
  }

  if (source) {
    userInfo.value = {
      name: source.name || '',
      email: source.email || '',
      bio: source.bio || '',
      avatar: source.avatar || '',
      createTime: source.createTime || '',
      repositoryCount: source.repositoryCount || 0,
      cardCount: source.cardCount || 0,
      examCount: source.examCount || 0,
      groupCount: source.groupCount || 0
    }
    Object.assign(originalInfo, userInfo.value)
  }
}

onMounted(() => {
  initUserInfo()
})

// 当父组件传入的 user 变化时，同步更新
watch(
  () => props.user,
  () => {
    initUserInfo()
  },
  { deep: true }
)

const saveProfile = () => {
  // TODO: 保存个人信息到后端
  alert('保存成功！')
  Object.assign(originalInfo, userInfo.value)
  // 同步更新本地存储的用户信息，便于 TopHeader / Layout 读取
  localStorage.setItem('auth_user', JSON.stringify(userInfo.value))
}

const resetProfile = () => {
  userInfo.value = JSON.parse(JSON.stringify(originalInfo))
}

// 退出登录：清除本地登录状态并回到游客视图
const logout = () => {
  // 清除 TopHeader 使用的本地登录标记（如有 token 也可一并清除）
  localStorage.removeItem('auth_username')
  localStorage.removeItem('auth_user')
  // 根据需要可清空本地用户信息
  userInfo.value = {
    name: '',
    email: '',
    bio: '',
    avatar: '',
    createTime: '',
    repositoryCount: 0,
    cardCount: 0,
    examCount: 0,
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

.upload-avatar-btn:hover {
  background-color: #e0e0e0;
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

