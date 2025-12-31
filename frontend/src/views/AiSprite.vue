<template>
    <div
      class="ai-sprite-wrapper"
      :style="wrapperStyle"
    >
      
      <transition name="ai-sprite-notification">
        <div v-if="autoNotification.show" class="ai-sprite-notification">
          <div class="ai-sprite-notification-content">
            <div class="ai-sprite-notification-icon">💡</div>
            <div class="ai-sprite-notification-text">{{ autoNotification.text }}</div>
          </div>
          <button class="ai-sprite-notification-close" @click="closeAutoNotification">✕</button>
        </div>
      </transition>

      <button
        class="ai-sprite-button"
        @mousedown="startDrag"
        @touchstart.prevent="startDrag"
        @mousemove="handleMouseMove"
        @mouseleave="handleMouseLeave"
        aria-label="AI 学习小精灵"
      >
        <svg 
          class="ai-sprite-icon-svg" 
          :class="{ 'thinking': isLoading, 'active': hasUnreadMessages, 'hover': isHovering }"
          :style="{ 
            transform: `
              translate(${eyeOffsetX}px, ${eyeOffsetY}px) 
              scale(${hoverScale}) 
              rotateY(${rotateY}deg) 
              rotateX(${rotateX}deg)
              perspective(1000px)
            ` 
          }"
          viewBox="0 0 200 200" 
          xmlns="http://www.w3.org/2000/svg"
        >
          <defs>
            
            <linearGradient id="bookCoverGradient" x1="0%" y1="0%" x2="0%" y2="100%">
              <stop offset="0%" stop-color="#e0f2fe"/>
              <stop offset="50%" stop-color="#93c5fd"/>
              <stop offset="100%" stop-color="#3b82f6"/>
            </linearGradient>
            
            <linearGradient id="bookSideGradient" x1="0%" y1="0%" x2="100%" y2="0%">
              <stop offset="0%" stop-color="#e5e7eb"/>
              <stop offset="100%" stop-color="#d4d4d8"/>
            </linearGradient>
            <radialGradient id="bookLight" cx="50%" cy="30%">
              <stop offset="0%" stop-color="#ffffff" stop-opacity="0.6"/>
              <stop offset="50%" stop-color="#ffffff" stop-opacity="0.25"/>
              <stop offset="100%" stop-color="#ffffff" stop-opacity="0"/>
            </radialGradient>
            <radialGradient id="bookShadow" cx="50%" cy="100%">
              <stop offset="0%" stop-color="#000000" stop-opacity="0"/>
              <stop offset="100%" stop-color="#000000" stop-opacity="0.25"/>
            </radialGradient>
          </defs>

          <ellipse
            cx="98"
            cy="162"
            rx="40"
            ry="10"
            class="ai-sprite-glow"
          />

          <polygon points="132,68 142,78 142,158 132,148" fill="url(#bookSideGradient)" class="ai-sprite-book-side">
            <animate attributeName="points" values="132,68 142,78 142,158 132,148;132,66 142,76 142,156 132,146;132,68 142,78 142,158 132,148" dur="2.5s" repeatCount="indefinite"/>
          </polygon>

          <rect x="62" y="62" width="72" height="92" rx="4" fill="url(#bookCoverGradient)" class="ai-sprite-book-cover" stroke="#2563eb" stroke-width="1.8">
            <animate attributeName="y" values="62;60;62" dur="2.5s" repeatCount="indefinite"/>
          </rect>

          <path
            d="M 122 150 L 134 150 Q 138 150 138 154 L 138 162 L 130 158 Q 126 156 122 152 Z"
            fill="#e5f0ff"
            class="ai-sprite-corner"
          />

          <rect x="62" y="62" width="72" height="92" rx="4" fill="url(#bookLight)" opacity="0.7">
            <animate attributeName="y" values="62;60;62" dur="2.5s" repeatCount="indefinite"/>
          </rect>

          <rect x="62" y="62" width="72" height="4" rx="4 4 0 0" fill="#ffffff" opacity="0.8">
            <animate attributeName="y" values="62;60;62" dur="2.5s" repeatCount="indefinite"/>
          </rect>
          
          <g opacity="0.6">
            <line x1="64" y1="66" x2="95" y2="66" stroke="#e5e7eb" stroke-width="1"/>
            <line x1="97" y1="66" x2="132" y2="66" stroke="#e5e7eb" stroke-width="1"/>
          </g>

          <rect x="62" y="150" width="72" height="6" rx="0 0 4 4" fill="url(#bookShadow)" opacity="0.5">
            <animate attributeName="y" values="150;148;150" dur="2.5s" repeatCount="indefinite"/>
          </rect>

          <g class="ai-sprite-left-eye">
            <circle cx="80" cy="104" r="15" fill="#ffffff" class="ai-sprite-eye-white">
              <animate attributeName="cy" values="104;102;104" dur="2.5s" repeatCount="indefinite"/>
            </circle>
            <circle 
              :cx="80 + leftEyeX" 
              :cy="104 + leftEyeY" 
              r="8" 
              fill="#2563eb" 
              class="ai-sprite-eye-pupil"
            >
              <animate attributeName="cy" values="104;102;104" dur="2.5s" repeatCount="indefinite"/>
            </circle>
            <circle 
              :cx="81.5 + leftEyeX" 
              :cy="102.5 + leftEyeY" 
              r="3" 
              fill="#ffffff" 
              class="ai-sprite-eye-highlight"
            >
              <animate attributeName="cy" values="102.5;100.5;102.5" dur="2.5s" repeatCount="indefinite"/>
            </circle>
          </g>

          <g class="ai-sprite-right-eye">
            <circle cx="116" cy="104" r="15" fill="#ffffff" class="ai-sprite-eye-white">
              <animate attributeName="cy" values="104;102;104" dur="2.5s" repeatCount="indefinite"/>
            </circle>
            <circle 
              :cx="116 + rightEyeX" 
              :cy="104 + rightEyeY" 
              r="8" 
              fill="#2563eb" 
              class="ai-sprite-eye-pupil"
            >
              <animate attributeName="cy" values="104;102;104" dur="2.5s" repeatCount="indefinite"/>
            </circle>
            <circle 
              :cx="117.5 + rightEyeX" 
              :cy="102.5 + rightEyeY" 
              r="3" 
              fill="#ffffff" 
              class="ai-sprite-eye-highlight"
            >
              <animate attributeName="cy" values="102.5;100.5;102.5" dur="2.5s" repeatCount="indefinite"/>
            </circle>
          </g>

          <path 
            d="M 82 128 Q 98 136 114 128" 
            stroke="#2563eb" 
            stroke-width="2.6" 
            fill="none" 
            stroke-linecap="round"
            class="ai-sprite-mouth"
          >
            <animate attributeName="d" values="M 82 128 Q 98 136 114 128;M 82 129 Q 98 137 114 129;M 82 128 Q 98 136 114 128" dur="3s" repeatCount="indefinite"/>
          </path>

          <ellipse cx="78" cy="118" rx="8" ry="4" fill="#fecaca" opacity="0.5" />
          <ellipse cx="118" cy="118" rx="8" ry="4" fill="#fecaca" opacity="0.5" />

          <g class="ai-sprite-book-lines" opacity="0.25">
            <line x1="68" y1="102" x2="92" y2="102" stroke="#64748b" stroke-width="0.8">
              <animate attributeName="y1" values="102;100;102" dur="2.5s" repeatCount="indefinite"/>
              <animate attributeName="y2" values="102;100;102" dur="2.5s" repeatCount="indefinite"/>
            </line>
            <line x1="68" y1="112" x2="92" y2="112" stroke="#64748b" stroke-width="0.8">
              <animate attributeName="y1" values="112;110;112" dur="2.5s" repeatCount="indefinite"/>
              <animate attributeName="y2" values="112;110;112" dur="2.5s" repeatCount="indefinite"/>
            </line>
            <line x1="104" y1="102" x2="132" y2="102" stroke="#64748b" stroke-width="0.8">
              <animate attributeName="y1" values="102;100;102" dur="2.5s" repeatCount="indefinite"/>
              <animate attributeName="y2" values="102;100;102" dur="2.5s" repeatCount="indefinite"/>
            </line>
            <line x1="104" y1="112" x2="132" y2="112" stroke="#64748b" stroke-width="0.8">
              <animate attributeName="y1" values="112;110;112" dur="2.5s" repeatCount="indefinite"/>
              <animate attributeName="y2" values="112;110;112" dur="2.5s" repeatCount="indefinite"/>
            </line>
            <line x1="68" y1="122" x2="92" y2="122" stroke="#64748b" stroke-width="0.8">
              <animate attributeName="y1" values="122;120;122" dur="2.5s" repeatCount="indefinite"/>
              <animate attributeName="y2" values="122;120;122" dur="2.5s" repeatCount="indefinite"/>
            </line>
            <line x1="104" y1="122" x2="132" y2="122" stroke="#64748b" stroke-width="0.8">
              <animate attributeName="y1" values="122;120;122" dur="2.5s" repeatCount="indefinite"/>
              <animate attributeName="y2" values="122;120;122" dur="2.5s" repeatCount="indefinite"/>
            </line>
          </g>

          <g class="ai-sprite-star" opacity="0.2">
            <polygon points="148,62 152,68 160,70 154,75 156,83 148,79 140,83 142,75 136,70 144,68" fill="#facc15" />
          </g>

          <g class="ai-sprite-thought">
            <circle cx="160" cy="40" r="12" fill="rgba(255,255,255,0.95)" opacity="0">
              <animate attributeName="opacity" values="0;1;0" dur="1.5s" repeatCount="indefinite"/>
            </circle>
            <circle cx="175" cy="25" r="8" fill="rgba(255,255,255,0.95)" opacity="0">
              <animate attributeName="opacity" values="0;1;0" dur="1.5s" repeatCount="indefinite" begin="0.3s"/>
            </circle>

            <g class="ai-sprite-thought-question" opacity="0.7">
              <circle cx="150" cy="30" r="6" fill="rgba(255,255,255,0.95)"/>
              <text
                x="150"
                y="32"
                text-anchor="middle"
                font-size="9"
                font-family="system-ui"
                fill="#1d4ed8"
              >?</text>
            </g>
          </g>
        </svg>
        
        <span v-if="hasUnreadMessages" class="ai-sprite-badge"></span>
      </button>

      <transition name="ai-sprite-fade">
        <div
          v-if="isOpen"
          class="ai-sprite-panel"
          :style="{ width: panelWidth + 'px', height: panelHeight + 'px' }"
          @mousedown.stop
          @touchstart.stop
        >
          <header class="ai-sprite-header">
            <div class="ai-sprite-title">
              <div class="ai-sprite-avatar">
                <svg 
                  class="ai-sprite-avatar-svg" 
                  viewBox="0 0 200 200" 
                  xmlns="http://www.w3.org/2000/svg"
                >
                  <defs>
                    <linearGradient id="avatarBookGradient" x1="0%" y1="0%" x2="0%" y2="100%">
                      <stop offset="0%" stop-color="#e0f2fe"/>
                      <stop offset="100%" stop-color="#3b82f6"/>
                    </linearGradient>
                  </defs>

                  <polygon points="140,60 150,70 150,130 140,120" fill="#e5e7eb"/>

                  <rect x="60" y="60" width="80" height="70" rx="3" fill="url(#avatarBookGradient)" stroke="#1d4ed8" stroke-width="2"/>

                  <line x1="100" y1="60" x2="100" y2="130" stroke="#1e3a8a" stroke-width="2"/>

                  <circle cx="80" cy="95" r="12" fill="#ffffff"/>
                  <circle cx="80" cy="95" r="6" fill="#2563eb"/>
                  <circle cx="82" cy="93" r="2.5" fill="#ffffff"/>

                  <circle cx="120" cy="95" r="12" fill="#ffffff"/>
                  <circle cx="120" cy="95" r="6" fill="#2563eb"/>
                  <circle cx="122" cy="93" r="2.5" fill="#ffffff"/>

                  <path d="M 85 110 Q 100 115 115 110" stroke="#2563eb" stroke-width="2.5" fill="none" stroke-linecap="round"/>
                </svg>
              </div>
              <div>
                <div class="ai-sprite-name">
                  {{ locales[currentLang].assistantName }}
                </div>
                <div class="ai-sprite-subtitle">
                  {{ locales[currentLang].subtitle }}
                </div>
              </div>
            </div>
            <div class="ai-sprite-header-actions">
              <button class="ai-sprite-lang-toggle" @click="toggleLang">
                {{ currentLang === 'en' ? '中文' : 'EN' }}
              </button>
              <button
                class="ai-sprite-clear"
                @click="clearMessages"
                :title="currentLang === 'en' ? 'Clear conversation' : '清除对话'"
              >
                🗑️
              </button>
              <button class="ai-sprite-close" @click="toggleOpen">✕</button>
            </div>
          </header>

          <main class="ai-sprite-messages" ref="messageList">
            
            <div
              v-if="messages.filter(m => m.role === 'user').length === 0"
              class="ai-sprite-quick-questions"
            >
              <div class="ai-sprite-quick-title">
                {{ locales[currentLang].quickTitle }}
              </div>
              <div class="ai-sprite-quick-buttons">
                <button
                  v-for="q in quickQuestions"
                  :key="q"
                  class="ai-sprite-quick-btn"
                  @click="sendQuickQuestion(q)"
                >
                  {{ q }}
                </button>
              </div>
            </div>
  
            <div
              v-for="msg in messages"
              :key="msg.id"
              :class="['ai-sprite-message', `ai-sprite-message--${msg.role}`]"
            >
              <div class="ai-sprite-message-bubble-wrapper">
                <div
                  class="ai-sprite-message-bubble"
                  v-html="renderMarkdown(msg.content)"
                ></div>
                
                <button
                  v-if="msg.role === 'assistant'"
                  class="ai-sprite-copy-btn"
                  @click="copyMessage(msg)"
                >
                  {{ copySuccess === msg.id ? '已复制 ✓' : '复制' }}
                </button>
              </div>
              
              <div class="ai-sprite-message-time">{{ formatTime(msg.timestamp) }}</div>
            </div>
  
            <div v-if="isLoading" class="ai-sprite-typing">
              {{ locales[currentLang].typing }}
              <span class="dot"></span><span class="dot"></span><span class="dot"></span>
            </div>
          </main>

          <footer class="ai-sprite-input">
            <textarea
              ref="textareaRef"
              v-model="userInput"
              class="ai-sprite-textarea"
              rows="3"
              :placeholder="locales[currentLang].inputPlaceholder"
              @keydown.enter.prevent="handleEnter"
              @input="autoResizeTextarea"
            ></textarea>
            <button
              class="ai-sprite-send"
              :disabled="!userInput.trim() || isLoading"
              @click="sendMessage"
            >
              发送
            </button>
          </footer>
          
          <div
            class="ai-sprite-resize-handle"
            @mousedown="startResize"
            title="拖拽调整大小"
          >
            <svg width="12" height="12" viewBox="0 0 12 12">
              <path d="M1 1 L11 11 M1 5 L7 11 M1 9 L3 11" stroke="currentColor" stroke-width="1.5" fill="none"/>
            </svg>
          </div>
        </div>
      </transition>
    </div>
  </template>
  
  <script setup>
  import { ref, watch, onMounted, nextTick, computed } from 'vue'
  import { marked } from 'marked'
  
  const props = defineProps({
    userName: {
      type: String,
      default: '',
    },
    lastLoginAt: {
      type: String,
      default: '',
    },
    todayStudyMinutes: {
      type: Number,
      default: 0,
    },
    todayWrongCount: {
      type: Number,
      default: 0,
    },
    daysSinceLastLogin: {
      type: Number,
      default: null,
    },
    
    currentSubject: {
      type: String,
      default: '',
    },
    currentNoteTitle: {
      type: String,
      default: '',
    },
    
    isViewingWrongQuestion: {
      type: Boolean,
      default: false,
    },
  })
  
  const isOpen = ref(false)
  const userInput = ref('')
  const isLoading = ref(false)
  const messages = ref([])
  const messageList = ref(null)
  const copySuccess = ref(null) 
  const hasUnreadMessages = ref(false) 
  const textareaRef = ref(null)

  const isHovering = ref(false)
  const hoverScale = ref(1)
  const rotateX = ref(0) 
  const rotateY = ref(0) 
  const eyeOffsetX = ref(0)
  const eyeOffsetY = ref(0)
  const leftEyeX = ref(0)
  const leftEyeY = ref(0)
  const rightEyeX = ref(0)
  const rightEyeY = ref(0)

  const spritePosition = ref({ x: 0, y: 0 })
  const isDraggingSprite = ref(false)
  const dragStart = ref({ x: 0, y: 0, mouseX: 0, mouseY: 0 })
  const dragMoved = ref(false)
  const suppressClick = ref(false)

  const autoNotification = ref({
    show: false,
    text: '',
  })

  const STORAGE_KEY = 'ai-sprite-messages'
  const STORAGE_LAST_STUDY_REMINDER = 'ai-sprite-last-study-reminder'
  const STORAGE_LAST_WRONG_QUESTION_OFFER = 'ai-sprite-last-wrong-offer'
  const STORAGE_LAST_WRONG_ENCOURAGE = 'ai-sprite-last-wrong-encourage'

  const currentLang = ref('en') 
  
  const locales = {
    en: {
      assistantName: 'Study Sprite',
      subtitle: 'Your personal study assistant',
      typing: 'The sprite is thinking...',
      inputPlaceholder:
        'Any study questions or things you want to talk about? Press Enter to send, Shift+Enter for a new line.',
      quickTitle: 'Quick questions',
      quickQuestions: [
        'Explain this question for me',
        'Help me organize my notes',
        'What should I study today?',
        'Give me some study advice',
      ],
    },
    zh: {
      assistantName: '学习小精灵',
      subtitle: '你的专属学习助手',
      typing: '小精灵正在思考中…',
      inputPlaceholder:
        '有什么学习问题，或者想和我聊聊学习情况吗？按 Enter 发送，Shift+Enter 换行',
      quickTitle: '快捷提问',
      quickQuestions: [
        '帮我解释这道题',
        '帮我整理笔记',
        '今天学什么好？',
        '给我一些学习建议',
      ],
    },
  }
  
  const quickQuestions = computed(() => locales[currentLang.value].quickQuestions)
  
  const toggleLang = () => {
    currentLang.value = currentLang.value === 'en' ? 'zh' : 'en'
  }

  const panelWidth = ref(360)
  const panelHeight = ref(520)
  const isResizing = ref(false)
  const resizeStartX = ref(0)
  const resizeStartY = ref(0)
  const resizeStartWidth = ref(0)
  const resizeStartHeight = ref(0)

  const AI_API_URL = import.meta.env.VITE_AI_API_URL || '/api/ai/chat'
  const DEEPSEEK_MODEL = 'deepseek-chat'
  
  const initGreetingSent = ref(false)

  marked.setOptions({
    breaks: true,
  })
  
  const toggleOpen = () => {
    if (suppressClick.value) {
      suppressClick.value = false
      return
    }
    isOpen.value = !isOpen.value
    
    if (isOpen.value) {
      
      if (messages.value.length === 0) {
        const hasHistory = loadMessagesFromStorage()
        if (!hasHistory) {
          
          pushSystemGreeting()
        }
      }
      hasUnreadMessages.value = false
      nextTick(scrollToBottom)
    }
  }

  const wrapperStyle = computed(() => ({
    top: `${spritePosition.value.y}px`,
    left: `${spritePosition.value.x}px`,
  }))
  
  function buildGreetingText() {
    const namePart = props.userName ? `${props.userName}，` : ''
  
    let daysGap = props.daysSinceLastLogin
    if (daysGap == null && props.lastLoginAt) {
      const last = new Date(props.lastLoginAt)
      const now = new Date()
      daysGap = Math.floor((now - last) / (1000 * 60 * 60 * 24))
    }
  
    const studyMins = props.todayStudyMinutes
    const wrong = props.todayWrongCount
  
    const hour = new Date().getHours()
    let timeGreeting = '你好'
    if (hour >= 5 && hour < 11) timeGreeting = '早上好'
    else if (hour >= 11 && hour < 14) timeGreeting = '中午好'
    else if (hour >= 14 && hour < 18) timeGreeting = '下午好'
    else timeGreeting = '晚上好'
  
    let loginPart = ''
    if (daysGap != null) {
      if (daysGap >= 7) {
        loginPart = `我们已经 ${daysGap} 天没见了，欢迎回来继续学习！重新开始永远不晚，我会陪你一点点赶上来。`
      } else if (daysGap >= 3) {
        loginPart = `有 ${daysGap} 天没来学习了，能回来继续坚持很棒！一起把节奏捡回来。`
      } else if (daysGap >= 1) {
        loginPart = `昨天没来学习，今天能重新打开页面已经很值得表扬了！`
      } else {
        loginPart = '欢迎回来，继续向自己的目标迈进吧！'
      }
    } else {
      loginPart = '欢迎来到学习空间，我会帮你把知识学得更轻松。'
    }
  
    let studyPart = ''
    if (studyMins >= 240) {
      studyPart =
        `你今天已经学习了大约 ${studyMins} 分钟，专注力超级棒！不过也记得站起来活动一下，喝口水，劳逸结合效果会更好～`
    } else if (studyMins >= 120) {
      studyPart =
        `今天累计学习差不多 ${studyMins} 分钟了，坚持真的不容易，再完成一点点就可以适当休息一下啦。`
    } else if (studyMins >= 30) {
      studyPart =
        `今天已经学习了约 ${studyMins} 分钟，节奏很好，保持下去你会看到很大的变化。`
    } else if (studyMins > 0) {
      studyPart =
        `刚刚开始学习的这 ${studyMins} 分钟也很重要，每一次打开页面都是在为未来的自己投资。`
    }
  
    let wrongPart = ''
    if (wrong >= 10) {
      wrongPart =
        `今天已经记录了 ${wrong} 道错题啦。错题并不可怕，它们正在告诉你哪里可以再加强一点，我们可以一起把它们变成“得分题”。`
    } else if (wrong >= 1) {
      wrongPart =
        `今天有几道题没做对很正常，错题就是最精准的老师，只要愿意回头看看，就已经比很多人走得更远。`
    }
  
    return `${timeGreeting}，${namePart}${loginPart}\n${studyPart}\n${wrongPart}\n有不懂的知识点、搞不明白的题目，或者只是想让我帮你规划学习，我都可以试试帮你。`
  }

  function saveMessagesToStorage() {
    try {
      const data = {
        messages: messages.value,
        lastUpdate: new Date().toISOString(),
      }
      localStorage.setItem(STORAGE_KEY, JSON.stringify(data))
    } catch (err) {
      console.warn('保存对话失败:', err)
    }
  }

  function loadMessagesFromStorage() {
    try {
      const stored = localStorage.getItem(STORAGE_KEY)
      if (stored) {
        const data = JSON.parse(stored)
        if (data.messages && Array.isArray(data.messages)) {
          
          data.messages.forEach(msg => {
            if (msg.timestamp) {
              msg.timestamp = new Date(msg.timestamp)
            }
          })
          messages.value = data.messages
          return true
        }
      }
    } catch (err) {
      console.warn('加载对话失败:', err)
    }
    return false
  }

  function clearMessages() {
    messages.value = []
    localStorage.removeItem(STORAGE_KEY)
    pushSystemGreeting()
  }

  function showAutoNotification(text, duration = 5000) {
    autoNotification.value = {
      show: true,
      text: text,
    }
    hasUnreadMessages.value = true
    
    setTimeout(() => {
      if (autoNotification.value.show) {
        closeAutoNotification()
      }
    }, duration)
  }

  function closeAutoNotification() {
    autoNotification.value.show = false
  }

  function checkAndShowAutoEncouragement() {
    const studyMins = props.todayStudyMinutes
    const wrong = props.todayWrongCount
    const now = Date.now()

    if (studyMins >= 120) {
      const lastReminder = localStorage.getItem(STORAGE_LAST_STUDY_REMINDER)
      const reminderKey = `study-${Math.floor(studyMins / 30)}` 
      
      if (!lastReminder || lastReminder !== reminderKey) {
        const hours = Math.floor(studyMins / 60)
        const minutes = studyMins % 60
        showAutoNotification(
          `🎉 太棒了！你已经连续学习了 ${hours} 小时${minutes > 0 ? minutes + '分钟' : ''}，专注力真的很强！记得起来活动一下，喝口水，劳逸结合效果会更好～`,
          8000
        )
        localStorage.setItem(STORAGE_LAST_STUDY_REMINDER, reminderKey)
      }
    }
    
    else if (studyMins >= 30 && studyMins < 60) {
      const lastReminder = localStorage.getItem(STORAGE_LAST_STUDY_REMINDER)
      if (lastReminder !== 'study-30') {
        showAutoNotification(
          `💪 很好！你已经学习了 ${studyMins} 分钟，继续保持这个节奏，你会看到很大的进步！`,
          6000
        )
        localStorage.setItem(STORAGE_LAST_STUDY_REMINDER, 'study-30')
      }
    }

    if (wrong >= 5) {
      const lastWrongReminder = localStorage.getItem(STORAGE_LAST_WRONG_ENCOURAGE)
      const wrongKey = `wrong-${Math.floor(wrong / 5)}` 
      
      if (!lastWrongReminder || lastWrongReminder !== wrongKey) {
        showAutoNotification(
          `📚 今天已经记录了 ${wrong} 道错题，这说明你在认真思考！错题是进步的阶梯，我们一起把它们变成得分题～`,
          7000
        )
        localStorage.setItem(STORAGE_LAST_WRONG_ENCOURAGE, wrongKey)
      }
    }
  }

  function checkWrongQuestionOffer() {
    if (!props.isViewingWrongQuestion) return
    
    const lastOffer = localStorage.getItem(STORAGE_LAST_WRONG_QUESTION_OFFER)
    const now = Date.now()

    if (lastOffer && now - parseInt(lastOffer) < 5 * 60 * 1000) {
      return
    }
    
    showAutoNotification(
      '📖 看到你在看错题了，需要我帮你讲解这道题吗？点击右下角的小精灵，我可以为你详细分析～',
      8000
    )
    localStorage.setItem(STORAGE_LAST_WRONG_QUESTION_OFFER, now.toString())
  }
  
  function pushSystemGreeting() {
    
    if (messages.value.length > 0) {
      return
    }
    
    messages.value.push({
      id: Date.now(),
      role: 'assistant',
      content: buildGreetingText(),
      timestamp: new Date(),
    })
  }

  function formatTime(date) {
    if (!date) return ''
    const d = new Date(date)
    const hours = d.getHours()
    const minutes = d.getMinutes().toString().padStart(2, '0')
    const period = hours < 12 ? '上午' : '下午'
    const hour12 = hours % 12 || 12
    return `${period} ${hour12}:${minutes}`
  }

  async function copyMessage(msg) {
    try {
      await navigator.clipboard.writeText(msg.content)
      copySuccess.value = msg.id
      setTimeout(() => {
        copySuccess.value = null
      }, 2000)
    } catch (err) {
      console.error('复制失败:', err)
    }
  }

  function sendQuickQuestion(question) {
    userInput.value = question
    sendMessage()
  }

  function handleMouseMove(e) {
    isHovering.value = true
    
    const button = e.currentTarget
    const rect = button.getBoundingClientRect()
    const centerX = rect.left + rect.width / 2
    const centerY = rect.top + rect.height / 2

    const mouseX = e.clientX - centerX
    const mouseY = e.clientY - centerY

    const distance = Math.sqrt(mouseX * mouseX + mouseY * mouseY)
    const maxDistance = 150 

    if (distance < maxDistance) {
      hoverScale.value = 1 + (maxDistance - distance) / maxDistance * 0.2 
    } else {
      hoverScale.value = 1
    }

    const maxRotate = 15 
    rotateY.value = (mouseX / rect.width) * maxRotate 
    rotateX.value = -(mouseY / rect.height) * maxRotate 

    eyeOffsetX.value = mouseX * 0.05 
    eyeOffsetY.value = mouseY * 0.05

    const svgScale = 200 / 80 
    const svgMouseX = mouseX * svgScale
    const svgMouseY = mouseY * svgScale
    
    const eyeMaxOffset = 5 

    const leftEyeCenterX = 70
    const leftEyeCenterY = 110
    const leftEyeRelativeX = svgMouseX - (leftEyeCenterX - 100) 
    const leftEyeRelativeY = svgMouseY - (leftEyeCenterY - 100)
    
    const leftEyeDistance = Math.min(
      Math.sqrt(leftEyeRelativeX * leftEyeRelativeX + leftEyeRelativeY * leftEyeRelativeY),
      eyeMaxOffset
    )
    const leftEyeAngle = Math.atan2(leftEyeRelativeY, leftEyeRelativeX)
    leftEyeX.value = Math.cos(leftEyeAngle) * leftEyeDistance
    leftEyeY.value = Math.sin(leftEyeAngle) * leftEyeDistance

    const rightEyeCenterX = 115
    const rightEyeCenterY = 110
    const rightEyeRelativeX = svgMouseX - (rightEyeCenterX - 100)
    const rightEyeRelativeY = svgMouseY - (rightEyeCenterY - 100)
    
    const rightEyeDistance = Math.min(
      Math.sqrt(rightEyeRelativeX * rightEyeRelativeX + rightEyeRelativeY * rightEyeRelativeY),
      eyeMaxOffset
    )
    const rightEyeAngle = Math.atan2(rightEyeRelativeY, rightEyeRelativeX)
    rightEyeX.value = Math.cos(rightEyeAngle) * rightEyeDistance
    rightEyeY.value = Math.sin(rightEyeAngle) * rightEyeDistance
  }
  
  function handleMouseLeave() {
    isHovering.value = false
    hoverScale.value = 1
    rotateX.value = 0
    rotateY.value = 0
    eyeOffsetX.value = 0
    eyeOffsetY.value = 0

    leftEyeX.value = 0
    leftEyeY.value = 0
    rightEyeX.value = 0
    rightEyeY.value = 0
  }
  
  function renderMarkdown(text) {
    
    try {
      return marked.parse(text || '')
    } catch (e) {
      
      return (text || '').replace(/\n/g, '<br />')
    }
  }
  
  function scrollToBottom() {
    if (!messageList.value) return
    const el = messageList.value
    el.scrollTop = el.scrollHeight
  }
  
  function buildDeepSeekPayload(userMessage) {
    const history = messages.value.map((m) => ({
      role: m.role === 'user' ? 'user' : 'assistant',
      content: m.content,
    }))

    let contextInfo = '你是一个温柔、积极、会鼓励学生的学习小助手。学生正在一个在线学习网站中使用你，你会根据对方的学习情况给予鼓励、安慰和具体学习建议，语气轻松但不敷衍，尽量用简体中文回答。'
    
    if (props.currentSubject) {
      contextInfo += `\n\n当前学生正在学习「${props.currentSubject}」这门课程。`
    }
    
    if (props.currentNoteTitle) {
      contextInfo += `\n\n学生当前正在查看笔记「${props.currentNoteTitle}」。`
    }
    
    if (props.currentSubject && props.currentNoteTitle) {
      contextInfo += `\n\n回答问题时，可以结合当前学习的「${props.currentSubject}」和正在查看的「${props.currentNoteTitle}」相关内容，让回答更精准、更有针对性。`
    }
  
    return {
      model: DEEPSEEK_MODEL,
      messages: [
        {
          role: 'system',
          content: contextInfo,
        },
        ...history,
        {
          role: 'user',
          content: userMessage,
        },
      ],
      temperature: 0.7,
      max_tokens: 4096,
      stream: false,
    }
  }
  
  async function callDeepSeek(userMessage) {
    const payload = buildDeepSeekPayload(userMessage)

    const res = await fetch(AI_API_URL, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        messages: payload.messages,
      }),
    })
  
    if (!res.ok) {
      const text = await res.text()
      throw new Error(`AI 接口请求失败：${res.status} ${text}`)
    }
  
    const data = await res.json()

    const content = data.content || data.reply || data.message
    if (!content) {
      throw new Error('后端返回格式错误，请检查接口')
    }
    return content.trim()
  }
  
  async function sendMessage() {
    const text = userInput.value.trim()
    if (!text || isLoading.value) return
  
    const now = Date.now()

    messages.value.push({
      id: now,
      role: 'user',
      content: text,
      timestamp: new Date(),
    })
    userInput.value = ''
    
    if (textareaRef.value) {
      const lineHeight = 20
      const minHeight = lineHeight * 3 + 16
      textareaRef.value.style.height = minHeight + 'px'
    }
    saveMessagesToStorage() 
    nextTick(scrollToBottom)
  
    isLoading.value = true
    try {
      const reply = await callDeepSeek(text)

      messages.value.push({
        id: now + 1,
        role: 'assistant',
        content: reply,
        timestamp: new Date(),
      })
      
      saveMessagesToStorage() 
      
    } catch (err) {
      console.error(err)
      messages.value.push({
        id: now + 2,
        role: 'assistant',
        content:
          '啊哦，和 AI 的连接好像出了点问题：' +
          (err.message || '请稍后再试，或者检查网络与密钥配置。'),
        timestamp: new Date(),
      })
      saveMessagesToStorage() 
    } finally {
      isLoading.value = false
      nextTick(scrollToBottom)
    }
  }
  
  function handleEnter(e) {
    if (e.shiftKey) {
      userInput.value += '\n'
    } else {
      sendMessage()
    }
  }

  function autoResizeTextarea() {
    if (!textareaRef.value) return

    textareaRef.value.style.height = 'auto'

    const lineHeight = 20 
    const minHeight = lineHeight * 3 + 16 
    const maxHeight = lineHeight * 10 + 16 
    const scrollHeight = textareaRef.value.scrollHeight

    const newHeight = Math.min(Math.max(scrollHeight, minHeight), maxHeight)
    textareaRef.value.style.height = newHeight + 'px'
  }
  
  onMounted(() => {
    
    if (textareaRef.value) {
      const lineHeight = 20
      const minHeight = lineHeight * 3 + 16
      textareaRef.value.style.height = minHeight + 'px'
    }
    
    checkAndShowAutoEncouragement()

    checkWrongQuestionOffer()

    loadMessagesFromStorage()

    const saved = localStorage.getItem('ai-sprite-position')
    if (saved) {
      try {
        const parsed = JSON.parse(saved)
        spritePosition.value = clampPosition(parsed.x, parsed.y)
      } catch (e) {
        setDefaultPosition()
      }
    } else {
      setDefaultPosition()
    }
  })

  watch(
    () => props.todayStudyMinutes,
    (newVal, oldVal) => {
      
      if (newVal > oldVal) {
        checkAndShowAutoEncouragement()
      }
    },
    { immediate: false }
  )

  watch(
    () => props.isViewingWrongQuestion,
    (newVal) => {
      if (newVal) {
        checkWrongQuestionOffer()
      }
    }
  )

  watch(
    () => props.todayWrongCount,
    (newVal, oldVal) => {
      if (newVal > oldVal && newVal >= 3) {
        
        setTimeout(() => {
          checkAndShowAutoEncouragement()
        }, 2000) 
      }
    }
  )

  function startResize(e) {
    e.preventDefault()
    isResizing.value = true
    resizeStartX.value = e.clientX
    resizeStartY.value = e.clientY
    resizeStartWidth.value = panelWidth.value
    resizeStartHeight.value = panelHeight.value
  
    document.addEventListener('mousemove', handleResize)
    document.addEventListener('mouseup', stopResize)
  }
  
  function handleResize(e) {
    if (!isResizing.value) return

    const deltaX = resizeStartX.value - e.clientX
    const deltaY = resizeStartY.value - e.clientY

    const newWidth = Math.max(320, Math.min(600, resizeStartWidth.value + deltaX))
    const newHeight = Math.max(350, Math.min(700, resizeStartHeight.value + deltaY))
  
    panelWidth.value = newWidth
    panelHeight.value = newHeight
  }
  
  function stopResize() {
    isResizing.value = false
    document.removeEventListener('mousemove', handleResize)
    document.removeEventListener('mouseup', stopResize)
  }

  function setDefaultPosition() {
    const padding = 20
    const buttonSize = 150
    const x = Math.max(padding, window.innerWidth - buttonSize - padding)
    const y = Math.max(padding, window.innerHeight - buttonSize - padding - 50) 
    spritePosition.value = { x, y }
  }
  
  function clampPosition(x, y) {
    const padding = 12
    const buttonSize = 150
    const maxX = window.innerWidth - buttonSize - padding
    const maxY = window.innerHeight - buttonSize - padding
    return {
      x: Math.min(Math.max(padding, x), maxX),
      y: Math.min(Math.max(padding, y), maxY),
    }
  }
  
  function savePosition() {
    try {
      localStorage.setItem('ai-sprite-position', JSON.stringify(spritePosition.value))
    } catch (e) {
      console.warn('保存位置失败', e)
    }
  }
  
  function startDrag(e) {
    
    if (isOpen.value) return
    
    const isTouch = e.type === 'touchstart'
    const clientX = isTouch ? e.touches[0].clientX : e.clientX
    const clientY = isTouch ? e.touches[0].clientY : e.clientY
  
    isDraggingSprite.value = true
    dragStart.value = {
      x: spritePosition.value.x,
      y: spritePosition.value.y,
      mouseX: clientX,
      mouseY: clientY,
    }
    dragMoved.value = false
    document.addEventListener('mousemove', handleDragMove)
    document.addEventListener('mouseup', stopDrag)
    document.addEventListener('touchmove', handleDragMove, { passive: false })
    document.addEventListener('touchend', stopDrag)
  }
  
  function handleDragMove(e) {
    if (!isDraggingSprite.value) return
    const isTouch = e.type === 'touchmove'
    const clientX = isTouch ? e.touches[0].clientX : e.clientX
    const clientY = isTouch ? e.touches[0].clientY : e.clientY
  
    const deltaX = clientX - dragStart.value.mouseX
    const deltaY = clientY - dragStart.value.mouseY
    const next = clampPosition(dragStart.value.x + deltaX, dragStart.value.y + deltaY)
    spritePosition.value = next
    if (Math.abs(deltaX) > 4 || Math.abs(deltaY) > 4) {
      dragMoved.value = true
    }
  
    if (isTouch) e.preventDefault()
  }
  
  function stopDrag() {
    if (!isDraggingSprite.value) return
    isDraggingSprite.value = false
    savePosition()

    if (!dragMoved.value) {
      toggleOpen()
    }
    
    document.removeEventListener('mousemove', handleDragMove)
    document.removeEventListener('mouseup', stopDrag)
    document.removeEventListener('touchmove', handleDragMove)
    document.removeEventListener('touchend', stopDrag)
  }
  
  function resetPosition() {
    setDefaultPosition()
    savePosition()
  }
  </script>
  
  <style scoped>

  .ai-sprite-wrapper {
    position: fixed;
    top: 20px;
    left: 20px;
    z-index: 9999;
    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC',
      'Microsoft YaHei', sans-serif;
    touch-action: none; 
  }

  .ai-sprite-button {
    width: 150px; 
    height: 150px;
    border: none;
    background: transparent;
    cursor: pointer;
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 0;
    position: relative;
    transition: transform 0.2s ease;
  }
  
  .ai-sprite-button:hover {
    transform: translateY(-3px);
  }

  .ai-sprite-icon-svg {
    width: 80px;
    height: 80px;
    display: block;
    transform-style: preserve-3d;
    filter: 
      drop-shadow(0 8px 16px rgba(0, 0, 0, 0.2))
      drop-shadow(0 4px 8px rgba(0, 0, 0, 0.15))
      drop-shadow(0 2px 4px rgba(0, 0, 0, 0.1));
    animation: ai-float-3d 3.4s ease-in-out infinite;
    transition: 
      transform 0.4s cubic-bezier(0.34, 1.56, 0.64, 1),
      filter 0.3s ease;
    transform-origin: center center;
    will-change: transform;
  }
  
  @keyframes ai-float-3d {
    0%, 100% {
      transform: translateY(0) scale(1) rotateY(0deg) rotateX(0deg);
    }
    25% {
      transform: translateY(-5px) scale(1.02) rotateY(2deg) rotateX(-1deg);
    }
    50% {
      transform: translateY(-8px) scale(1.03) rotateY(0deg) rotateX(-2deg);
    }
    75% {
      transform: translateY(-5px) scale(1.02) rotateY(-2deg) rotateX(-1deg);
    }
  }

  .ai-sprite-icon-svg.hover {
    filter: 
      drop-shadow(0 12px 24px rgba(0, 0, 0, 0.3))
      drop-shadow(0 6px 12px rgba(0, 0, 0, 0.2))
      drop-shadow(0 3px 6px rgba(0, 0, 0, 0.15));
  }

  .ai-sprite-icon-svg.thinking {
    animation: ai-float-3d 1.5s ease-in-out infinite, ai-think-pulse-3d 1s ease-in-out infinite;
  }
  
  .ai-sprite-icon-svg.thinking .ai-sprite-thought {
    display: block;
  }
  
  .ai-sprite-icon-svg:not(.thinking) .ai-sprite-thought {
    display: none;
  }
  
  @keyframes ai-think-pulse-3d {
    0%, 100% {
      filter: 
        drop-shadow(0 8px 16px rgba(0, 0, 0, 0.2))
        drop-shadow(0 4px 8px rgba(0, 0, 0, 0.15));
    }
    50% {
      filter: 
        drop-shadow(0 16px 32px rgba(0, 0, 0, 0.35))
        drop-shadow(0 8px 16px rgba(0, 0, 0, 0.25));
    }
  }

  .ai-sprite-icon-svg.active {
    animation: ai-float-3d 1.8s ease-in-out infinite, ai-active-pulse-3d 1.6s ease-in-out infinite;
  }
  
  @keyframes ai-active-pulse-3d {
    0%, 100% {
      filter: 
        drop-shadow(0 8px 16px rgba(0, 0, 0, 0.18))
        drop-shadow(0 0 18px rgba(59, 130, 246, 0.45));
    }
    50% {
      filter: 
        drop-shadow(0 14px 28px rgba(0, 0, 0, 0.22))
        drop-shadow(0 0 26px rgba(59, 130, 246, 0.7));
    }
  }

  .ai-sprite-eye-pupil,
  .ai-sprite-eye-highlight {
    transition: cx 0.2s cubic-bezier(0.4, 0, 0.2, 1), cy 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  }

  .ai-sprite-book-cover {
    filter: 
      drop-shadow(0 4px 8px rgba(0, 0, 0, 0.2))
      drop-shadow(0 2px 4px rgba(0, 0, 0, 0.15));
  }

  .ai-sprite-book-side {
    filter: drop-shadow(0 3px 6px rgba(0, 0, 0, 0.3));
  }

  .ai-sprite-book-spine {
    filter: drop-shadow(0 1px 2px rgba(0, 0, 0, 0.2));
  }

  .ai-sprite-eye-white {
    filter: drop-shadow(0 2px 4px rgba(0, 0, 0, 0.15));
  }

  .ai-sprite-glow {
    fill: radial-gradient(circle at center, rgba(191, 219, 254, 0.8), rgba(191, 219, 254, 0));
  }
  
  .ai-sprite-icon-svg .ai-sprite-glow {
    opacity: 0.35;
    transition: opacity 0.25s ease, transform 0.25s ease;
  }
  
  .ai-sprite-icon-svg.hover .ai-sprite-glow,
  .ai-sprite-icon-svg.active .ai-sprite-glow {
    opacity: 0.7;
    transform: scaleX(1.08) scaleY(1.05);
  }

  .ai-sprite-corner {
    filter: drop-shadow(0 1px 2px rgba(15, 23, 42, 0.2));
    transition: transform 0.25s ease;
    transform-origin: 138px 162px;
  }
  
  .ai-sprite-icon-svg.hover .ai-sprite-corner {
    transform: translateY(-1px) rotateZ(-4deg);
  }

  .ai-sprite-star {
    transform-origin: center;
    transition: opacity 0.25s ease;
  }
  
  .ai-sprite-icon-svg.hover .ai-sprite-star {
    opacity: 0.9;
    animation: ai-star-bounce 1.4s ease-in-out infinite;
  }
  
  @keyframes ai-star-bounce {
    0%, 100% {
      transform: translateY(0) scale(1);
    }
    50% {
      transform: translateY(-2px) scale(1.08);
    }
  }

  .ai-sprite-icon-svg.hover .ai-sprite-book-cover {
    animation: ai-book-sway-3d 2s ease-in-out infinite;
  }
  
  @keyframes ai-book-sway-3d {
    0%, 100% {
      transform: translateX(0) translateY(0);
    }
    50% {
      transform: translateX(1px) translateY(-1px);
    }
  }

  .ai-sprite-badge {
    position: absolute;
    top: -2px;
    right: -2px;
    width: 10px;
    height: 10px;
    background: #ef4444;
    border-radius: 50%;
    border: 2px solid #ffffff;
    animation: ai-pulse 2s infinite;
  }
  
  @keyframes ai-pulse {
    0%, 100% {
      opacity: 1;
      transform: scale(1);
    }
    50% {
      opacity: 0.8;
      transform: scale(1.1);
    }
  }

  .ai-sprite-notification {
    position: fixed;
    bottom: 90px;
    right: 24px;
    max-width: 360px;
    background: #ffffff;
    border: 1px solid #e5e7eb;
    border-radius: 12px;
    box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
    padding: 12px 14px;
    display: flex;
    align-items: flex-start;
    gap: 10px;
    z-index: 10000;
    animation: ai-slide-in 0.3s ease-out;
  }
  
  @keyframes ai-slide-in {
    from {
      transform: translateX(100%);
      opacity: 0;
    }
    to {
      transform: translateX(0);
      opacity: 1;
    }
  }
  
  .ai-sprite-notification-content {
    flex: 1;
    display: flex;
    align-items: flex-start;
    gap: 10px;
  }
  
  .ai-sprite-notification-icon {
    font-size: 20px;
    flex-shrink: 0;
  }
  
  .ai-sprite-notification-text {
    font-size: 13px;
    line-height: 1.5;
    color: #374151;
  }
  
  .ai-sprite-notification-close {
    border: none;
    background: transparent;
    color: #9ca3af;
    cursor: pointer;
    font-size: 16px;
    padding: 0;
    width: 20px;
    height: 20px;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 4px;
    transition: all 0.15s ease;
    flex-shrink: 0;
  }
  
  .ai-sprite-notification-close:hover {
    background: #f3f4f6;
    color: #6b7280;
  }

  .ai-sprite-notification-enter-active,
  .ai-sprite-notification-leave-active {
    transition: all 0.3s ease;
  }
  
  .ai-sprite-notification-enter-from,
  .ai-sprite-notification-leave-to {
    transform: translateX(100%);
    opacity: 0;
  }

  .ai-sprite-panel {
    position: absolute;
    bottom: 70px;
    right: 0;
    min-width: 320px;
    max-width: 700px;
    min-height: 350px;
    max-height: 800px;
    background: #ffffff;
    border: 1px solid #e5e7eb;
    border-radius: 16px;
    box-shadow: 0 8px 30px rgba(0, 0, 0, 0.12);
    display: flex;
    flex-direction: column;
    overflow: hidden;
    color: #1f2937;
  }

  .ai-sprite-fade-enter-active,
  .ai-sprite-fade-leave-active {
    transition: opacity 0.2s ease, transform 0.2s ease;
  }
  
  .ai-sprite-fade-enter-from,
  .ai-sprite-fade-leave-to {
    opacity: 0;
    transform: translateY(10px) scale(0.98);
  }

  .ai-sprite-header {
    padding: 12px 16px;
    background: #eff6ff;
    border-bottom: 1px solid #dbeafe;
    display: flex;
    align-items: center;
    justify-content: space-between;
  }
  
  .ai-sprite-title {
    display: flex;
    align-items: center;
    gap: 10px;
  }
  
  .ai-sprite-avatar {
    width: 34px;
    height: 34px;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    background: transparent;
    overflow: visible;
  }
  
  .ai-sprite-avatar-svg {
    width: 34px;
    height: 34px;
    display: block;
  }
  
  .ai-sprite-name {
    font-size: 14px;
    font-weight: 600;
    color: #1e40af;
  }
  
  .ai-sprite-subtitle {
    font-size: 11px;
    color: #6b7280;
  }
  
  .ai-sprite-close {
    border: none;
    width: 28px;
    height: 28px;
    border-radius: 6px;
    background: transparent;
    color: #6b7280;
    cursor: pointer;
    font-size: 14px;
    display: flex;
    align-items: center;
    justify-content: center;
    transition: all 0.15s ease;
  }
  
  .ai-sprite-close:hover {
    background: #dbeafe;
    color: #1e40af;
  }
  
  .ai-sprite-header-actions {
    display: flex;
    align-items: center;
    gap: 4px;
  }
  
  .ai-sprite-clear {
    border: none;
    width: 28px;
    height: 28px;
    border-radius: 6px;
    background: transparent;
    color: #6b7280;
    cursor: pointer;
    font-size: 14px;
    display: flex;
    align-items: center;
    justify-content: center;
    transition: all 0.15s ease;
  }
  
  .ai-sprite-clear:hover {
    background: #fee2e2;
    color: #dc2626;
  }

  .ai-sprite-messages {
    padding: 12px;
    overflow-y: auto;
    flex: 1;
    display: flex;
    flex-direction: column;
    gap: 10px;
    background: #fafbfc;
  }
  
  .ai-sprite-messages::-webkit-scrollbar {
    width: 5px;
  }
  
  .ai-sprite-messages::-webkit-scrollbar-track {
    background: transparent;
  }
  
  .ai-sprite-messages::-webkit-scrollbar-thumb {
    background: #cbd5e1;
    border-radius: 10px;
  }

  .ai-sprite-quick-questions {
    background: #ffffff;
    border: 1px solid #e5e7eb;
    border-radius: 12px;
    padding: 12px;
    margin-bottom: 8px;
  }
  
  .ai-sprite-quick-title {
    font-size: 12px;
    color: #6b7280;
    margin-bottom: 8px;
  }
  
  .ai-sprite-quick-buttons {
    display: flex;
    flex-wrap: wrap;
    gap: 6px;
  }
  
  .ai-sprite-quick-btn {
    background: #eff6ff;
    border: 1px solid #dbeafe;
    border-radius: 16px;
    padding: 6px 12px;
    font-size: 12px;
    color: #1e40af;
    cursor: pointer;
    transition: all 0.15s ease;
  }
  
  .ai-sprite-quick-btn:hover {
    background: #dbeafe;
    border-color: #93c5fd;
  }
  
  .ai-sprite-message {
    display: flex;
    flex-direction: column;
    max-width: 88%;
  }
  
  .ai-sprite-message--user {
    align-self: flex-end;
  }
  
  .ai-sprite-message--assistant {
    align-self: flex-start;
  }

  .ai-sprite-message-bubble-wrapper {
    position: relative;
  }

  .ai-sprite-copy-btn {
    display: block;
    margin-top: 6px;
    margin-left: auto;
    background: #eff6ff;
    border: 1px solid #dbeafe;
    padding: 3px 10px;
    cursor: pointer;
    font-size: 11px;
    color: #2563eb;
    border-radius: 4px;
    transition: all 0.15s ease;
  }
  
  .ai-sprite-copy-btn:hover {
    background: #dbeafe;
    border-color: #93c5fd;
  }

  .ai-sprite-message-time {
    font-size: 10px;
    color: #9ca3af;
    margin-top: 2px;
    padding: 0 4px;
  }
  
  .ai-sprite-message--user .ai-sprite-message-time {
    text-align: right;
  }

  .ai-sprite-message-bubble {
    border-radius: 12px;
    padding: 8px 12px;
    font-size: 13px;
    line-height: 1.6;
    word-break: break-word;
  }

  .ai-sprite-message--user .ai-sprite-message-bubble {
    background: #2563eb;
    color: #ffffff;
  }

  .ai-sprite-message--assistant .ai-sprite-message-bubble {
    background: #ffffff;
    border: 1px solid #e5e7eb;
    color: #374151;
  }

  .ai-sprite-message--assistant .ai-sprite-message-bubble-wrapper {
    background: #ffffff;
    border: 1px solid #e5e7eb;
    border-radius: 12px;
    padding: 8px 12px 6px;
  }
  
  .ai-sprite-message--assistant .ai-sprite-message-bubble-wrapper .ai-sprite-message-bubble {
    border: none;
    padding: 0;
    background: transparent;
  }

  .ai-sprite-message--user .ai-sprite-message-bubble-wrapper {
    background: #2563eb;
    border-radius: 12px;
    padding: 8px 12px;
  }
  
  .ai-sprite-message--user .ai-sprite-message-bubble-wrapper .ai-sprite-message-bubble {
    border: none;
    padding: 0;
    background: transparent;
  }

  .ai-sprite-message-bubble :deep(h1),
  .ai-sprite-message-bubble :deep(h2),
  .ai-sprite-message-bubble :deep(h3),
  .ai-sprite-message-bubble :deep(h4),
  .ai-sprite-message-bubble :deep(h5),
  .ai-sprite-message-bubble :deep(h6) {
    margin: 6px 0 3px 0;
    font-size: 13px;
    font-weight: 600;
    line-height: 1.4;
    color: #1e40af;
  }
  
  .ai-sprite-message-bubble :deep(p) {
    margin: 3px 0;
    line-height: 1.6;
  }
  
  .ai-sprite-message-bubble :deep(ul),
  .ai-sprite-message-bubble :deep(ol) {
    margin: 3px 0;
    padding-left: 18px;
    line-height: 1.5;
  }
  
  .ai-sprite-message-bubble :deep(li) {
    margin: 1px 0;
    line-height: 1.5;
  }
  
  .ai-sprite-message-bubble :deep(strong) {
    font-weight: 600;
    color: #1e40af;
  }
  
  .ai-sprite-message-bubble :deep(code) {
    background: #f1f5f9;
    padding: 1px 4px;
    border-radius: 4px;
    font-size: 12px;
    font-family: 'SF Mono', 'Monaco', 'Consolas', monospace;
    color: #dc2626;
  }
  
  .ai-sprite-message-bubble :deep(blockquote) {
    margin: 4px 0;
    padding-left: 10px;
    border-left: 3px solid #2563eb;
    color: #6b7280;
  }

  .ai-sprite-message--user .ai-sprite-message-bubble :deep(h1),
  .ai-sprite-message--user .ai-sprite-message-bubble :deep(h2),
  .ai-sprite-message--user .ai-sprite-message-bubble :deep(h3),
  .ai-sprite-message--user .ai-sprite-message-bubble :deep(h4),
  .ai-sprite-message--user .ai-sprite-message-bubble :deep(h5),
  .ai-sprite-message--user .ai-sprite-message-bubble :deep(h6),
  .ai-sprite-message--user .ai-sprite-message-bubble :deep(strong) {
    color: #ffffff;
  }
  
  .ai-sprite-message--user .ai-sprite-message-bubble :deep(code) {
    background: rgba(255, 255, 255, 0.2);
    color: #ffffff;
  }

  .ai-sprite-typing {
    font-size: 12px;
    color: #6b7280;
    display: flex;
    align-items: center;
    gap: 4px;
    padding: 4px 0;
  }
  
  .ai-sprite-typing .dot {
    width: 5px;
    height: 5px;
    border-radius: 50%;
    background: #2563eb;
    animation: ai-bounce 1.2s infinite ease-in-out both;
  }
  
  .ai-sprite-typing .dot:nth-child(2) {
    animation-delay: 0.2s;
  }
  .ai-sprite-typing .dot:nth-child(3) {
    animation-delay: 0.4s;
  }
  
  @keyframes ai-bounce {
    0%, 80%, 100% {
      transform: scale(0.6);
      opacity: 0.5;
    }
    40% {
      transform: scale(1);
      opacity: 1;
    }
  }

  .ai-sprite-input {
    border-top: 1px solid #e5e7eb;
    padding: 10px 12px;
    display: flex;
    gap: 8px;
    align-items: flex-end;
    background: #ffffff;
  }
  
  .ai-sprite-textarea {
    flex: 1;
    resize: none;
    border-radius: 10px;
    border: 1px solid #d1d5db;
    background: #f9fafb;
    color: #1f2937;
    padding: 8px 10px;
    font-size: 13px;
    line-height: 1.5;
    transition: height 0.15s ease;
    overflow-y: auto;
    min-height: 76px;
    max-height: 200px;
  }
  
  .ai-sprite-textarea::placeholder {
    color: #9ca3af;
  }
  
  .ai-sprite-textarea:focus {
    outline: none;
    border-color: #2563eb;
    background: #ffffff;
    box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.1);
  }
  
  .ai-sprite-send {
    border: none;
    border-radius: 8px;
    padding: 8px 16px;
    font-size: 13px;
    font-weight: 500;
    background: #2563eb;
    color: #ffffff;
    cursor: pointer;
    white-space: nowrap;
    transition: all 0.15s ease;
  }
  
  .ai-sprite-send:hover:not(:disabled) {
    background: #1d4ed8;
  }
  
  .ai-sprite-send:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }

  .ai-sprite-resize-handle {
    position: absolute;
    bottom: 4px;
    left: 4px;
    width: 18px;
    height: 18px;
    cursor: nesw-resize;
    display: flex;
    align-items: center;
    justify-content: center;
    color: #94a3b8;
    opacity: 0.5;
    transition: all 0.15s ease;
    border-radius: 4px;
  }
  
  .ai-sprite-resize-handle:hover {
    opacity: 1;
    color: #2563eb;
    background: rgba(37, 99, 235, 0.1);
  }

  @media (max-width: 600px) {
    .ai-sprite-panel {
      width: 94vw !important;
      right: -10px;
      bottom: 65px;
      max-height: 70vh !important;
    }
  }
  </style>

  