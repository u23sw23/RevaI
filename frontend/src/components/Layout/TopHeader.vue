<template>
  <header class="top-header">
    
    <div class="header-left">
      <div class="search-wrapper" ref="searchWrapperRef">
        <input
          v-model="searchKeyword"
          type="text"
          placeholder="Search public repositories..."
          class="search-input"
          @keyup.enter="handleSearch"
          @input="handleSearchInput"
          @focus="showSearchResults = true"
          @blur="handleSearchBlur"
        />
        <button class="search-button" @click="handleSearch">
        Search
        </button>
      </div>
    </div>

    <Teleport to="body">
      <div 
        v-if="showSearchResults && searchResults.length > 0" 
        class="search-results"
        :style="searchResultsStyle"
      >
        <div
          v-for="subject in searchResults"
          :key="subject.id"
          class="search-result-item"
          @mousedown.prevent="selectSearchResult(subject)"
        >
          <span class="result-title">{{ subject.name }}</span>
          <span class="result-creator">by {{ subject.creatorUsername || 'Unknown' }}</span>
        </div>
      </div>
      <div 
        v-else-if="showSearchResults && searchKeyword.trim() && !isSearching" 
        class="search-results"
        :style="searchResultsStyle"
      >
        <div class="search-result-empty">No results found</div>
      </div>
    </Teleport>

    <div class="header-center">
      <h1 class="app-title">RevAI</h1>
    </div>

    <div class="header-right">
      
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

      <div v-else class="auth-buttons">
        <button class="auth-btn" @click="openLoginModal">Login</button>
        <button class="auth-btn primary" @click="openRegisterModal">Register</button>
      </div>
    </div>

    <Teleport to="body">
      <div v-if="showLoginModal" class="modal-overlay" @click.self="closeLoginModal">
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
    </Teleport>

    <Teleport to="body">
      <div v-if="showRegisterModal" class="modal-overlay" @click.self="closeRegisterModal">
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
    </Teleport>
  </header>
</template>

<script setup>
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { api } from '../../utils/api'

const props = defineProps({
  userName: {
    type: String,
    default: 'User'
  }
})

const emit = defineEmits(['search', 'auth-success'])

const searchKeyword = ref('')
const searchResults = ref([])
const showSearchResults = ref(false)
const isSearching = ref(false)
const searchWrapperRef = ref(null)
let searchTimeout = null

const currentUserName = ref(props.userName)

const userAvatar = ref('')

const userInitial = computed(() => {
  if (!currentUserName.value) return 'U'
  return currentUserName.value.trim().charAt(0).toUpperCase()
})

const getAvatarUrl = (avatar) => {
  if (!avatar) return ''
  if (avatar.startsWith('http')) return avatar
  if (avatar.startsWith('/')) return `http://localhost:8080${avatar}`
  return avatar
}

const handleAvatarError = (event) => {
  event.target.style.display = 'none'
}

const router = useRouter()

const isLoggedIn = ref(false)

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
    
  }
}

let updateInterval = null
onMounted(() => {
  updateUserInfo(true)
  
  window.addEventListener('storage', updateUserInfo)
  
  updateInterval = setInterval(updateUserInfo, 2000)
})

onUnmounted(() => {
  if (updateInterval) clearInterval(updateInterval)
  window.removeEventListener('storage', updateUserInfo)
})

const goToProfile = () => {
  router.push('/profile')
}

const handleSearchInput = () => {
  const keyword = searchKeyword.value.trim()
  if (keyword.length < 2) {
    searchResults.value = []
    showSearchResults.value = false
    return
  }

  if (searchTimeout) clearTimeout(searchTimeout)
  searchTimeout = setTimeout(async () => {
    isSearching.value = true
    try {
      const results = await api.searchPublicSubjects(keyword)
      searchResults.value = results || []
      showSearchResults.value = true
    } catch (error) {
      console.error('Search failed:', error)
      searchResults.value = []
    } finally {
      isSearching.value = false
    }
  }, 300)
}

const searchResultsStyle = computed(() => {
  if (!searchWrapperRef.value) return {}
  const rect = searchWrapperRef.value.getBoundingClientRect()
  return {
    top: `${rect.bottom + 4}px`,
    left: `${rect.left}px`,
    width: `${rect.width}px`
  }
})

const handleSearchBlur = (e) => {
  
  setTimeout(() => {
    if (!searchWrapperRef.value?.contains(document.activeElement)) {
      showSearchResults.value = false
    }
  }, 200)
}

const selectSearchResult = (subject) => {
  searchKeyword.value = ''
  searchResults.value = []
  showSearchResults.value = false
  
  router.push({
    path: '/community',
    query: { subjectId: subject.id }
  })
}

const handleSearch = () => {
  const keyword = searchKeyword.value.trim()
  if (keyword && searchResults.value.length > 0) {
    
    selectSearchResult(searchResults.value[0])
  } else {
  emit('search', keyword)
  }
}

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

    window.location.reload()
  } catch (error) {
    authError.value = error.message || 'Login failed, please try again later'
    console.error('Login error:', error)
  } finally {
    authLoading.value = false
  }
}

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
  background: linear-gradient(to right, #FFFFFF 0%, #F8FBFF 100%);
  border-bottom: 1px solid rgba(187, 222, 251, 0.6);
  box-shadow: 0 2px 12px rgba(33, 150, 243, 0.1);
  backdrop-filter: blur(10px);
  position: relative;
  z-index: 100;
}

.header-left {
  flex: 1;
  max-width: 400px;
}

.search-wrapper {
  display: flex;
  align-items: center;
  gap: 8px;
  position: relative;
}

.search-input {
  flex: 1;
  padding: 8px 12px;
  border: 1px solid rgba(187, 222, 251, 0.8);
  border-radius: 8px;
  font-size: 14px;
  background-color: #FFFFFF;
  transition: all 0.3s ease;
}

.search-input:focus {
  outline: none;
  border-color: #64B5F6;
  box-shadow: 0 0 0 3px rgba(33, 150, 243, 0.1);
}

.search-button {
  padding: 8px 16px;
  border: none;
  border-radius: 8px;
  background: linear-gradient(135deg, #2196F3 0%, #1976D2 100%);
  color: #fff;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 2px 6px rgba(33, 150, 243, 0.3);
}

.search-button:hover {
  background: linear-gradient(135deg, #1976D2 0%, #1565C0 100%);
  box-shadow: 0 4px 12px rgba(33, 150, 243, 0.4);
  transform: translateY(-1px);
}

.search-results {
  position: fixed;
  background: #FFFFFF;
  border: 1px solid rgba(187, 222, 251, 0.8);
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(33, 150, 243, 0.15);
  max-height: 300px;
  overflow-y: auto;
  z-index: 10002;
}

.search-result-item {
  padding: 8px 16px;
  cursor: pointer;
  transition: background-color 0.2s;
  border-bottom: 1px solid rgba(187, 222, 251, 0.3);
  display: flex;
  align-items: center;
  gap: 8px;
}

.search-result-item:last-child {
  border-bottom: none;
}

.search-result-item:hover {
  background: linear-gradient(to right, rgba(227, 242, 253, 0.6) 0%, rgba(187, 222, 251, 0.3) 100%);
}

.result-title {
  font-weight: 600;
  color: #1565C0;
  font-size: 14px;
}

.result-creator {
  color: #546E7A;
  font-size: 12px;
}

.search-result-empty {
  padding: 12px 16px;
  text-align: center;
  color: #999;
  font-size: 14px;
}

.header-center {
  position: absolute;
  left: 50%;
  top: 50%;
  transform: translate(-50%, -50%);
  display: flex;
  align-items: center;
  justify-content: center;
  pointer-events: none;
}

.app-title {
  font-size: 36px;
  font-weight: 700;
  background: linear-gradient(135deg, #1976D2 0%, #2196F3 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  margin: 0;
  letter-spacing: -0.5px;
  pointer-events: none;
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
  border-radius: 8px;
  font-size: 13px;
  border: 1px solid rgba(187, 222, 251, 0.8);
  background-color: #FFFFFF;
  cursor: pointer;
  transition: all 0.3s ease;
}

.auth-btn.primary {
  background: linear-gradient(135deg, #2196F3 0%, #1976D2 100%);
  color: #fff;
  border-color: #2196F3;
  box-shadow: 0 2px 6px rgba(33, 150, 243, 0.3);
}

.auth-btn:hover {
  background-color: #F0F7FF;
  border-color: #90CAF9;
  transform: translateY(-1px);
}

.auth-btn.primary:hover {
  background: linear-gradient(135deg, #1976D2 0%, #1565C0 100%);
  color: #fff;
  box-shadow: 0 4px 12px rgba(33, 150, 243, 0.4);
  transform: translateY(-1px);
}

.avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: linear-gradient(135deg, #BBDEFB 0%, #90CAF9 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.3s ease;
  overflow: hidden;
  position: relative;
  box-shadow: 0 2px 8px rgba(33, 150, 243, 0.2);
  border: 2px solid rgba(255, 255, 255, 0.8);
}

.avatar:hover {
  background: linear-gradient(135deg, #2196F3 0%, #1976D2 100%);
  color: white;
  box-shadow: 0 4px 12px rgba(33, 150, 243, 0.4);
  transform: scale(1.05);
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

.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 10001;
  overflow-y: auto;
  padding: 20px;
}

.modal {
  width: 420px;
  max-width: calc(100% - 40px);
  background-color: #fff;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  display: flex;
  flex-direction: column;
  margin: auto;
  max-height: calc(100vh - 40px);
  overflow-y: auto;
  position: relative;
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
