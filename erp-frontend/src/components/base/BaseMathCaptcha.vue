<template>
  <div class="base-captcha-container">
    <div v-if="label" class="captcha-label-row">
      <span class="captcha-label">
        <el-icon class="shield-icon"><ShieldCheck /></el-icon>
        {{ label }}
      </span>
      <span v-if="isVerified" class="verified-tag">
        <el-icon><Check /></el-icon> Đã xác thực
      </span>
    </div>

    <div class="captcha-box" :class="{ 'is-invalid': hasError, 'is-verified': isVerified, 'is-cooldown': cooldownSeconds > 0 }">
      <!-- Challenge Display (Stylized Math Badge) -->
      <div class="equation-display" :title="'Giải phép tính: ' + equationText">
        <span class="noise-overlay"></span>
        <span class="equation-text">{{ equationText }}</span>
        <button 
          type="button" 
          class="btn-refresh" 
          :disabled="cooldownSeconds > 0"
          title="Đổi phép tính khác" 
          @click="generateNewEquation"
        >
          <el-icon class="refresh-icon"><RefreshRight /></el-icon>
        </button>
      </div>

      <!-- User Input -->
      <div class="captcha-input-wrap">
        <input
          v-model="userAnswer"
          type="text"
          inputmode="numeric"
          :placeholder="cooldownSeconds > 0 ? `Đợi ${cooldownSeconds}s...` : placeholder"
          :disabled="cooldownSeconds > 0 || isVerified"
          class="captcha-input"
          @input="handleInput"
          @keyup.enter="validate"
        />
      </div>
    </div>

    <!-- Feedback Message -->
    <div v-if="cooldownSeconds > 0" class="captcha-hint warning">
      ⏳ Thử sai quá nhiều lần. Vui lòng đợi <strong>{{ cooldownSeconds }}s</strong> trước khi thử lại.
    </div>
    <div v-else-if="errorMessage" class="captcha-hint error">
      ⚠️ {{ errorMessage }}
    </div>
    <div v-else-if="!isVerified" class="captcha-hint info">
      💡 Vui lòng tính kết quả phép toán trên để tiếp tục.
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { RefreshRight, Check } from '@element-plus/icons-vue'

// Custom ShieldCheck Icon SVG component
const ShieldCheck = {
  template: `
    <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2">
      <path stroke-linecap="round" stroke-linejoin="round" d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
    </svg>
  `
}

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  },
  label: {
    type: String,
    default: 'Xác thực chống spam'
  },
  placeholder: {
    type: String,
    default: 'Đáp án?'
  },
  maxFailedAttempts: {
    type: Number,
    default: 3
  },
  cooldownDuration: {
    type: Number,
    default: 10 // seconds
  }
})

const emit = defineEmits(['update:modelValue', 'verified', 'fail', 'change'])

const equationText = ref('')
const correctAnswer = ref(0)
const userAnswer = ref('')
const isVerified = ref(false)
const hasError = ref(false)
const errorMessage = ref('')
const failedCount = ref(0)
const cooldownSeconds = ref(0)
let cooldownTimer = null

const generateNewEquation = () => {
  hasError.value = false
  errorMessage.value = ''
  userAnswer.value = ''
  isVerified.value = false
  emit('update:modelValue', false)

  // Random operation: +, -, *
  const ops = ['+', '-', '×']
  const op = ops[Math.floor(Math.random() * ops.length)]
  let num1, num2, ans

  if (op === '+') {
    num1 = Math.floor(Math.random() * 15) + 3 // 3 to 17
    num2 = Math.floor(Math.random() * 15) + 2 // 2 to 16
    ans = num1 + num2
  } else if (op === '-') {
    num1 = Math.floor(Math.random() * 20) + 10 // 10 to 29
    num2 = Math.floor(Math.random() * (num1 - 2)) + 1 // 1 to num1 - 2
    ans = num1 - num2
  } else {
    // Multiplication (small numbers)
    num1 = Math.floor(Math.random() * 7) + 2 // 2 to 8
    num2 = Math.floor(Math.random() * 5) + 2 // 2 to 6
    ans = num1 * num2
  }

  equationText.value = `${num1} ${op} ${num2} = ?`
  correctAnswer.value = ans
  emit('change', { equation: equationText.value })
}

const handleInput = () => {
  hasError.value = false
  errorMessage.value = ''
  
  if (userAnswer.value.trim() === String(correctAnswer.value)) {
    isVerified.value = true
    emit('update:modelValue', true)
    emit('verified')
  } else {
    if (isVerified.value) {
      isVerified.value = false
      emit('update:modelValue', false)
    }
  }
}

const startCooldown = () => {
  cooldownSeconds.value = props.cooldownDuration
  if (cooldownTimer) clearInterval(cooldownTimer)
  cooldownTimer = setInterval(() => {
    cooldownSeconds.value--
    if (cooldownSeconds.value <= 0) {
      clearInterval(cooldownTimer)
      failedCount.value = 0
      generateNewEquation()
    }
  }, 1000)
}

const validate = () => {
  if (cooldownSeconds.value > 0) {
    return false
  }

  if (userAnswer.value.trim() === String(correctAnswer.value)) {
    isVerified.value = true
    hasError.value = false
    errorMessage.value = ''
    failedCount.value = 0
    emit('update:modelValue', true)
    emit('verified')
    return true
  }

  // Failed
  isVerified.value = false
  hasError.value = true
  failedCount.value++
  emit('update:modelValue', false)
  emit('fail')

  if (failedCount.value >= props.maxFailedAttempts) {
    startCooldown()
  } else {
    errorMessage.value = `Kết quả chưa đúng! (${failedCount.value}/${props.maxFailedAttempts})`
    // Auto refresh on error to prevent bruteforcing same math question
    setTimeout(() => {
      generateNewEquation()
    }, 700)
  }
  return false
}

const reset = () => {
  failedCount.value = 0
  if (cooldownTimer) clearInterval(cooldownTimer)
  cooldownSeconds.value = 0
  generateNewEquation()
}

const autoSolveForDemo = () => {
  userAnswer.value = String(correctAnswer.value)
  isVerified.value = true
  hasError.value = false
  errorMessage.value = ''
  emit('update:modelValue', true)
  emit('verified')
}

watch(() => props.modelValue, (val) => {
  if (!val && isVerified.value) {
    reset()
  }
})

onMounted(() => {
  generateNewEquation()
})

defineExpose({
  validate,
  reset,
  autoSolveForDemo,
  isVerified
})
</script>

<style scoped>
.base-captcha-container {
  display: flex;
  flex-direction: column;
  gap: 6px;
  width: 100%;
}

.captcha-label-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.captcha-label {
  font-size: 12.5px;
  font-weight: 600;
  color: #334155;
  display: flex;
  align-items: center;
  gap: 5px;
}

.shield-icon {
  color: #4f46e5;
}

.verified-tag {
  font-size: 11px;
  font-weight: 600;
  color: #059669;
  background: rgba(16, 185, 129, 0.12);
  border: 1px solid rgba(16, 185, 129, 0.35);
  padding: 2px 8px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  gap: 4px;
}

.captcha-box {
  display: flex;
  align-items: center;
  gap: 10px;
  background: #f8fafc;
  border: 1.5px solid #cbd5e1;
  border-radius: 12px;
  padding: 6px 10px;
  transition: all 0.25s ease;
}

.captcha-box:hover {
  border-color: #6366f1;
  background: #ffffff;
}

.captcha-box.is-verified {
  border-color: #10b981;
  background: rgba(16, 185, 129, 0.06);
}

.captcha-box.is-invalid {
  border-color: #ef4444;
  background: rgba(239, 68, 68, 0.06);
  animation: shake 0.4s ease-in-out;
}

.captcha-box.is-cooldown {
  opacity: 0.6;
  pointer-events: none;
}

/* Equation Badge */
.equation-display {
  position: relative;
  display: flex;
  align-items: center;
  gap: 8px;
  background: linear-gradient(135deg, #1e293b 0%, #0f172a 100%);
  border: 1px solid #334155;
  border-radius: 8px;
  padding: 6px 10px;
  overflow: hidden;
  user-select: none;
}

.noise-overlay {
  position: absolute;
  inset: 0;
  background: repeating-linear-gradient(
    45deg,
    transparent,
    transparent 4px,
    rgba(255, 255, 255, 0.05) 4px,
    rgba(255, 255, 255, 0.05) 8px
  );
  pointer-events: none;
}

.equation-text {
  font-family: 'Courier New', Courier, monospace;
  font-size: 15px;
  font-weight: 800;
  color: #38bdf8;
  letter-spacing: 1.5px;
  text-shadow: 0 0 8px rgba(56, 189, 248, 0.4);
}

.btn-refresh {
  background: none;
  border: none;
  color: #94a3b8;
  cursor: pointer;
  padding: 2px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 4px;
  transition: all 0.2s ease;
}

.btn-refresh:hover:not(:disabled) {
  color: #f1f5f9;
  transform: rotate(45deg);
}

.refresh-icon {
  font-size: 14px;
}

/* User Input */
.captcha-input-wrap {
  flex: 1;
}

.captcha-input {
  width: 100%;
  background: transparent;
  border: none;
  outline: none;
  color: #0f172a;
  font-size: 14px;
  font-weight: 700;
  text-align: center;
  padding: 6px 8px;
}

.captcha-input::placeholder {
  color: #94a3b8;
  font-size: 13px;
  font-weight: 400;
}

/* Feedback Hints */
.captcha-hint {
  font-size: 11.5px;
  line-height: 1.4;
  margin-top: 2px;
}

.captcha-hint.info { color: #64748b; }
.captcha-hint.warning { color: #d97706; font-weight: 600; }
.captcha-hint.error { color: #dc2626; font-weight: 600; }

@keyframes shake {
  0%, 100% { transform: translateX(0); }
  20%, 60% { transform: translateX(-4px); }
  40%, 80% { transform: translateX(4px); }
}
</style>

