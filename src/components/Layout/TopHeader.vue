<template>
  <header class="top-header">
    
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

    <div class="header-right">
      
      <div v-if="isLoggedIn" class="avatar" @click="goToProfile">
        {{ userInitial }}
      </div>

      <div v-else class="auth-buttons">
        <button class="auth-btn" @click="openLoginModal">Login</button>
        <button class="auth-btn primary" @click="openRegisterModal">Register</button>
      </div>
    </div>

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
import { ref, computed, watch, onMounted } from 'vue'
import { useRouter } from 'vue-router'

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

const emit = defineEmits(['search', 'update:currentRepoId', 'auth-success'])

const searchKeyword = ref('')

const selectedRepoId = ref(props.currentRepoId ?? '')

watch(
  () => props.currentRepoId,
  (newVal) => {
    selectedRepoId.value = newVal ?? ''
  }
)

const currentUserName = ref(props.userName)

const userInitial = computed(() => {
  if (!currentUserName.value) return 'U'
  return currentUserName.value.trim().charAt(0).toUpperCase()
})

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

onMounted(() => {
  const savedUser = localStorage.getItem('auth_user')
  if (savedUser) {
    isLoggedIn.value = true
    try {
      const user = JSON.parse(savedUser)
      currentUserName.value = user.name || 'User'
      
      emit('auth-success', user)
    } catch (e) {
      
    }
  }
})

const goToProfile = () => {
  router.push('/profile')
}

const handleSearch = () => {
  const keyword = searchKeyword.value.trim()
  emit('search', keyword)
}

const handleRepoChange = () => {
  const value = selectedRepoId.value || null
  emit('update:currentRepoId', value)
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

    if (!res.ok) {
      throw new Error('Login request failed')
    }

    const data = await res.json()
    
    const user = data.user || { name: loginForm.value.username }

    isLoggedIn.value = true
    
    currentUserName.value = user.name
    localStorage.setItem('auth_username', currentUserName.value)
    localStorage.setItem('auth_user', JSON.stringify(user))
    if (data.token) {
      localStorage.setItem('auth_token', data.token)
    }
    emit('auth-success', user)
    closeLoginModal()
  } catch (error) {
    authError.value = 'Login failed, please try again later'
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

    if (!res.ok) {
      throw new Error('Register request failed')
    }

    const data = await res.json()
    
    const user = data.user || { name: registerForm.value.username }

    isLoggedIn.value = true
    currentUserName.value = user.name
    localStorage.setItem('auth_username', currentUserName.value)
    localStorage.setItem('auth_user', JSON.stringify(user))
    if (data.token) {
      localStorage.setItem('auth_token', data.token)
    }
    emit('auth-success', user)
    closeRegisterModal()
  } catch (error) {
    authError.value = 'Register failed, please try again later'
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
}

.avatar:hover {
  background-color: #1976d2;
  color: white;
}

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
