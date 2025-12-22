<template>
  <header class="top-header">
    <!-- 左侧：搜索框 -->
    <div class="header-left">
      <div class="search-wrapper">
        <input
          v-model="searchKeyword"
          type="text"
        placeholder="Searching keywords..."
          class="search-input"
          @keyup.enter="handleSearch"
        />
        <button class="search-button" @click="handleSearch">
        Search
        </button>
      </div>
    </div>

    <!-- 中间：仓库下拉选择 -->
    <div class="header-center">
      <select
        class="repo-select"
        v-model="selectedRepoId"
        @change="handleRepoChange"
      >
        <option value="">All the repositories</option>
        <option
          v-for="repo in repositories"
          :key="repo.id"
          :value="repo.id"
        >
          {{ repo.name }}
        </option>
      </select>
    </div>

    <!-- 右侧：登录/注册 或 头像入口 -->
    <div class="header-right">
      <!-- 已登录：显示头像，点击进入 Profile -->
      <div v-if="isLoggedIn" class="avatar" @click="goToProfile">
        <img 
          v-if="userAvatar" 
          :src="getAvatarUrl(userAvatar)" 
          alt="Avatar"
          class="avatar-img"
          @error="handleAvatarError"
        />
        <span v-else class="avatar-text">{{ userInitial }}</span>
      </div>

      <!-- 未登录：显示 Login / Register 按钮 -->
      <div v-else class="auth-buttons">
        <button class="auth-btn" @click="openLoginModal">Login</button>
        <button class="auth-btn primary" @click="openRegisterModal">Register</button>
      </div>
    </div>

    <!-- 登录弹窗 -->
    <div v-if="showLoginModal" class="modal-overlay">
      <div class="modal">
        <div class="modal-header">
          <h3>Login</h3>
          <button class="close-btn" @click="closeLoginModal">×</button>
        </div>
        <div class="modal-body">
          <div class="form-item">
            <label class="form-label">Username</label>
            <input
              v-model="loginForm.username"
              type="text"
              class="form-input"
              placeholder="Please enter username"
            />
          </div>
          <div class="form-item">
            <label class="form-label">Password</label>
            <div class="password-row">
              <input
                :type="loginShowPassword ? 'text' : 'password'"
                v-model="loginForm.password"
                class="form-input"
                placeholder="Please enter password"
              />
              <button
                type="button"
                class="toggle-password-btn"
                @click="loginShowPassword = !loginShowPassword"
              >
                {{ loginShowPassword ? 'Hide' : 'Show' }}
              </button>
            </div>
          </div>
          <p v-if="authError" class="error-text">{{ authError }}</p>
        </div>
        <div class="modal-footer">
          <button class="secondary-btn" @click="closeLoginModal">Cancel</button>
          <button
            class="primary-btn"
            @click="submitLogin"
            :disabled="authLoading"
          >
            {{ authLoading ? 'Logging in...' : 'Login' }}
          </button>
        </div>
      </div>
    </div>

    <!-- 注册弹窗 -->
    <div v-if="showRegisterModal" class="modal-overlay">
      <div class="modal">
        <div class="modal-header">
          <h3>Register</h3>
          <button class="close-btn" @click="closeRegisterModal">×</button>
        </div>
        <div class="modal-body">
          <div class="form-item">
            <label class="form-label">Username</label>
            <input
              v-model="registerForm.username"
              type="text"
              class="form-input"
              placeholder="Please enter username"
            />
          </div>
          <div class="form-item">
            <label class="form-label">Password</label>
            <div class="password-row">
              <input
                :type="registerShowPassword ? 'text' : 'password'"
                v-model="registerForm.password"
                class="form-input"
                placeholder="Please enter password"
              />
              <button
                type="button"
                class="toggle-password-btn"
                @click="registerShowPassword = !registerShowPassword"
              >
                {{ registerShowPassword ? 'Hide' : 'Show' }}
              </button>
            </div>
          </div>
          <div class="form-item">
            <label class="form-label">Confirm password</label>
            <div class="password-row">
              <input
                :type="registerShowConfirm ? 'text' : 'password'"
                v-model="registerForm.confirmPassword"
                class="form-input"
                placeholder="Please confirm password"
              />
              <button
                type="button"
                class="toggle-password-btn"
                @click="registerShowConfirm = !registerShowConfirm"
              >
                {{ registerShowConfirm ? 'Hide' : 'Show' }}
              </button>
            </div>
          </div>
          <p v-if="authError" class="error-text">{{ authError }}</p>
        </div>
        <div class="modal-footer">
          <button class="secondary-btn" @click="closeRegisterModal">Cancel</button>
          <button
            class="primary-btn"
            @click="submitRegister"
            :disabled="authLoading"
          >
            {{ authLoading ? 'Registering...' : 'Register' }}
          </button>
        </div>
      </div>
    </div>
  </header>
</template>

<script setup>
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'

/**
 * 父组件可以给 TopHeader 传入：
 * - repositories：仓库列表 [{ id, name }, ...]
 * - currentRepoId：当前选中的仓库 id
 * - userName：当前用户名称（用于头像上的首字母）
 */
const props = defineProps({
  repositories: {
    type: Array,
    default: () => []
  },
  currentRepoId: {
    type: [String, Number, null],
    default: null
  },
  userName: {
    type: String,
    default: 'User'
  }
})

/**
 * 向父组件发送事件：
 * - search：用户按下回车进行搜索时
 * - update:currentRepoId：用户切换仓库时
 * - auth-success：登录或注册成功时，将用户信息通知父组件
 */
const emit = defineEmits(['search', 'update:currentRepoId', 'auth-success'])

// 搜索关键字（双向绑定到输入框）
const searchKeyword = ref('')

// 当前选择的仓库 id（和父组件的 currentRepoId 对齐）
const selectedRepoId = ref(props.currentRepoId ?? '')

watch(
  () => props.currentRepoId,
  (newVal) => {
    selectedRepoId.value = newVal ?? ''
  }
)

// 当前登录用户名（本地维护一份，避免直接修改 props）
const currentUserName = ref(props.userName)

// 用户头像
const userAvatar = ref('')

// 头像上显示的首字母
const userInitial = computed(() => {
  if (!currentUserName.value) return 'U'
  return currentUserName.value.trim().charAt(0).toUpperCase()
})

// 获取头像完整URL
const getAvatarUrl = (avatar) => {
  if (!avatar) return ''
  if (avatar.startsWith('http')) return avatar
  if (avatar.startsWith('/')) return `http://localhost:8080${avatar}`
  return avatar
}

// 处理头像加载错误
const handleAvatarError = (event) => {
  event.target.style.display = 'none'
}

const router = useRouter()

// 登录状态（简单本地状态；实际项目中可改为从全局 store 或 token 推导）
const isLoggedIn = ref(false)

// 登录/注册弹窗相关状态
const showLoginModal = ref(false)
const showRegisterModal = ref(false)
const authLoading = ref(false)
const authError = ref('')

const loginForm = ref({
  username: '',
  password: ''
})
const registerForm = ref({
  username: '',
  password: '',
  confirmPassword: ''
})

const loginShowPassword = ref(false)
const registerShowPassword = ref(false)
const registerShowConfirm = ref(false)

// 更新用户信息（从 localStorage 读取）
const updateUserInfo = (emitEvent = false) => {
  const savedUser = localStorage.getItem('auth_user')
  if (!savedUser) return
  
  try {
    const user = JSON.parse(savedUser)
    isLoggedIn.value = true
    currentUserName.value = user.username || user.name || 'User'
    userAvatar.value = user.avatar || ''
    if (emitEvent) emit('auth-success', user)
  } catch (e) {
    // ignore parse error
  }
}

// 初始化：根据本地存储还原登录状态
let updateInterval = null
onMounted(() => {
  updateUserInfo(true)
  // 监听 storage 事件（跨标签页同步）
  window.addEventListener('storage', updateUserInfo)
  // 定期检查 localStorage 变化（同标签页内，用于 Profile 页面更新头像后同步）
  updateInterval = setInterval(updateUserInfo, 2000)
})

// 组件卸载时清理
onUnmounted(() => {
  if (updateInterval) clearInterval(updateInterval)
  window.removeEventListener('storage', updateUserInfo)
})

// 点击头像，跳转到个人信息页
const goToProfile = () => {
  router.push('/profile')
}

// 回车触发搜索，把关键词发给父组件
const handleSearch = () => {
  const keyword = searchKeyword.value.trim()
  emit('search', keyword)
}

// 切换仓库，把新的仓库 id 通知给父组件
const handleRepoChange = () => {
  const value = selectedRepoId.value || null
  emit('update:currentRepoId', value)
}

// 打开/关闭登录弹窗
const openLoginModal = () => {
  authError.value = ''
  authLoading.value = false
  loginShowPassword.value = false
  loginForm.value = {
    username: '',
    password: ''
  }
  showLoginModal.value = true
}

const closeLoginModal = () => {
  showLoginModal.value = false
}

// 打开/关闭注册弹窗
const openRegisterModal = () => {
  authError.value = ''
  authLoading.value = false
  registerShowPassword.value = false
  registerShowConfirm.value = false
  registerForm.value = {
    username: '',
    password: '',
    confirmPassword: ''
  }
  showRegisterModal.value = true
}

const closeRegisterModal = () => {
  showRegisterModal.value = false
}

// 登录提交：调用后端 /api/auth/login
const submitLogin = async () => {
  if (!loginForm.value.username || !loginForm.value.password) {
    authError.value = 'Username and password are required'
    return
  }

  authError.value = ''
  authLoading.value = true

  try {
    const res = await fetch('/api/auth/login', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        username: loginForm.value.username,
        password: loginForm.value.password
      })
    })

    const data = await res.json()
    
    if (!res.ok || !data.success) {
      throw new Error(data.message || 'Login request failed')
    }

    const user = data.user || { username: loginForm.value.username }
    localStorage.setItem('auth_username', user.username || user.name || loginForm.value.username)
    localStorage.setItem('auth_user', JSON.stringify(user))
    if (data.token) localStorage.setItem('auth_token', data.token)
    
    updateUserInfo(true)
    closeLoginModal()
  } catch (error) {
    authError.value = error.message || 'Login failed, please try again later'
    console.error('Login error:', error)
  } finally {
    authLoading.value = false
  }
}

// 注册提交：调用后端 /api/auth/register
const submitRegister = async () => {
  if (!registerForm.value.username || !registerForm.value.password || !registerForm.value.confirmPassword) {
    authError.value = 'Please fill in all fields'
    return
  }
  if (registerForm.value.password !== registerForm.value.confirmPassword) {
    authError.value = 'Two passwords are inconsistent'
    return
  }

  authError.value = ''
  authLoading.value = true

  try {
    const res = await fetch('/api/auth/register', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        username: registerForm.value.username,
        password: registerForm.value.password
      })
    })

    // 先检查响应状态，再尝试解析JSON
    let data
    try {
      const text = await res.text()
      data = text ? JSON.parse(text) : {}
    } catch (parseError) {
      console.error('JSON parse error:', parseError)
      throw new Error(`服务器返回了无效的响应 (${res.status})`)
    }
    
    if (!res.ok || !data.success) {
      throw new Error(data.message || `注册失败 (${res.status})`)
    }
    const user = data.user || { username: registerForm.value.username }
    localStorage.setItem('auth_username', user.username || user.name || registerForm.value.username)
    localStorage.setItem('auth_user', JSON.stringify(user))
    if (data.token) localStorage.setItem('auth_token', data.token)
    
    updateUserInfo(true)
    closeRegisterModal()
  } catch (error) {
    authError.value = error.message || 'Register failed, please try again later'
    console.error('Register error:', error)
  } finally {
    authLoading.value = false
  }
}
</script>

<style scoped>
.top-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 24px;
  background-color: #fff;
  border-bottom: 1px solid #e0e0e0;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
}

.header-left {
  flex: 1;
  max-width: 400px;
}

.search-wrapper {
  display: flex;
  align-items: center;
  gap: 8px;
}

.search-input {
  flex: 1;
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
}

.search-button {
  padding: 8px 16px;
  border: none;
  border-radius: 4px;
  background-color: #1976d2;
  color: #fff;
  font-size: 14px;
  cursor: pointer;
  transition: background-color 0.2s;
}

.search-button:hover {
  background-color: #1565c0;
}

.header-center {
  flex: 1;
  display: flex;
  justify-content: center;
}

.repo-select {
  padding: 8px 16px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
  background-color: #fff;
  cursor: pointer;
}

.header-right {
  flex: 1;
  display: flex;
  justify-content: flex-end;
}

.auth-buttons {
  display: flex;
  gap: 8px;
}

.auth-btn {
  padding: 6px 14px;
  border-radius: 4px;
  font-size: 13px;
  border: 1px solid #ddd;
  background-color: #fff;
  cursor: pointer;
  transition: all 0.2s;
}

.auth-btn.primary {
  background-color: #1976d2;
  color: #fff;
  border-color: #1976d2;
}

.auth-btn:hover {
  background-color: #f5f5f5;
}

.auth-btn.primary:hover {
  background-color: #1565c0;
  color: #fff;
}

.avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background-color: #e0e0e0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s;
  overflow: hidden;
  position: relative;
}

.avatar:hover {
  background-color: #1976d2;
  color: white;
}

.avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 50%;
}

.avatar-text {
  font-weight: 500;
}

/* 弹窗通用样式，和 Group.vue 中的风格保持一致 */
.modal-overlay {
  position: fixed;
  inset: 0;
  background-color: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1100;
}

.modal {
  width: 420px;
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
  padding: 14px 18px;
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
  padding: 14px 18px;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  padding: 10px 18px 14px;
  border-top: 1px solid #eee;
}

.form-item {
  margin-bottom: 14px;
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

.password-row {
  display: flex;
  gap: 8px;
}

.toggle-password-btn {
  padding: 0 8px;
  border-radius: 4px;
  border: 1px solid #ddd;
  background-color: #f5f5f5;
  font-size: 12px;
  cursor: pointer;
  white-space: nowrap;
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
</style>
