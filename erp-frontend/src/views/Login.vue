<template>
  <div class="auth-wrapper">
    <!-- Ambient Background Lighting -->
    <div class="glow-orb orb-1"></div>
    <div class="glow-orb orb-2"></div>
    <div class="glow-orb orb-3"></div>

    <div class="auth-container">
      <!-- Brand Header -->
      <div class="brand-header">
        <div class="brand-logo">
          <svg viewBox="0 0 24 24" width="32" height="32" fill="none" stroke="currentColor" stroke-width="2">
            <path stroke-linecap="round" stroke-linejoin="round" d="M20 7l-8-4-8 4m16 0l-8 4m8-4v10l-8 4m0-10L4 7m8 4v10M4 7v10l8 4" />
          </svg>
        </div>
        <h1 class="brand-title">ERP Cloud System</h1>
        <p class="brand-subtitle">Hệ thống quản trị tài nguyên doanh nghiệp toàn diện</p>
      </div>

      <!-- Main Glass Card -->
      <div class="glass-card">
        <!-- Reusable Base Loading Overlay -->
        <BaseLoading 
          :active="loading" 
          :text="loadingText" 
          :sub-text="loadingSubText" 
        />

        <!-- MODE 1: LOGIN -->
        <transition name="fade-slide" mode="out-in">
          <div v-if="mode === 'login'" key="login" class="form-section">
            <div class="form-title-wrap">
              <h2 class="form-title">Đăng nhập</h2>
              <p class="form-desc">Chào mừng bạn trở lại! Vui lòng nhập thông tin.</p>
            </div>

            <!-- Quick Demo Credential Pill -->
            <div class="demo-credential-pill" @click="fillDemoAccount">
              <span class="pulse-dot"></span>
              <span>Tài khoản Demo: <strong>admin</strong> / <strong>password</strong> (Click để điền)</span>
            </div>

            <el-form :model="loginForm" @keyup.enter="handleLogin" class="custom-form">
              <div class="form-group">
                <label class="form-label">Tên đăng nhập</label>
                <el-input 
                  v-model="loginForm.username" 
                  placeholder="Nhập username của bạn" 
                  size="large"
                  class="modern-input"
                >
                  <template #prefix><el-icon><User /></el-icon></template>
                </el-input>
              </div>

              <div class="form-group">
                <div class="label-row">
                  <label class="form-label">Mật khẩu</label>
                  <a href="javascript:void(0)" class="forgot-link" @click="switchMode('forgot')">Quên mật khẩu?</a>
                </div>
                <el-input 
                  v-model="loginForm.password" 
                  type="password" 
                  placeholder="Nhập mật khẩu" 
                  show-password
                  size="large"
                  class="modern-input"
                >
                  <template #prefix><el-icon><Lock /></el-icon></template>
                </el-input>
              </div>

              <div class="remember-row">
                <el-checkbox v-model="rememberMe" label="Ghi nhớ đăng nhập" class="custom-checkbox" />
              </div>

              <!-- Reusable Math Captcha Challenge -->
              <div class="form-group" style="margin-top: 2px;">
                <BaseMathCaptcha 
                  ref="loginCaptchaRef" 
                  v-model="isLoginCaptchaPassed"
                  label="Xác thực giải toán bảo vệ"
                />
              </div>

              <button 
                type="button" 
                class="btn-gradient" 
                :disabled="loading" 
                @click="handleLogin"
              >
                <span v-if="!loading" class="btn-content">
                  Đăng nhập
                  <el-icon class="btn-icon"><ArrowRight /></el-icon>
                </span>
                <span v-else class="btn-content">Đang xác thực...</span>
              </button>
            </el-form>

            <div class="switch-mode-text">
              Chưa có tài khoản? 
              <a href="javascript:void(0)" class="highlight-link" @click="switchMode('register')">Đăng ký tài khoản mới</a>
            </div>
          </div>

          <!-- MODE 2: REGISTER -->
          <div v-else-if="mode === 'register'" key="register" class="form-section">
            <div class="form-title-wrap">
              <h2 class="form-title">Đăng ký tài khoản</h2>
              <p class="form-desc">Tạo tài khoản quản trị viên mới cho hệ thống ERP.</p>
            </div>

            <el-form :model="regForm" @keyup.enter="handleRegister" class="custom-form">
              <div class="form-group">
                <label class="form-label">Tên đăng nhập mong muốn</label>
                <el-input 
                  v-model="regForm.username" 
                  placeholder="Ví dụ: nva_manager" 
                  size="large"
                  class="modern-input"
                >
                  <template #prefix><el-icon><User /></el-icon></template>
                </el-input>
              </div>

              <div class="form-group">
                <label class="form-label">Mật khẩu</label>
                <el-input 
                  v-model="regForm.password" 
                  type="password" 
                  placeholder="Tối thiểu 6 ký tự" 
                  show-password
                  size="large"
                  class="modern-input"
                >
                  <template #prefix><el-icon><Lock /></el-icon></template>
                </el-input>
              </div>

              <div class="form-group">
                <label class="form-label">Xác nhận mật khẩu</label>
                <el-input 
                  v-model="regForm.confirmPassword" 
                  type="password" 
                  placeholder="Nhập lại mật khẩu" 
                  show-password
                  size="large"
                  class="modern-input"
                >
                  <template #prefix><el-icon><Key /></el-icon></template>
                </el-input>
              </div>

              <button 
                type="button" 
                class="btn-gradient btn-gradient-emerald" 
                :disabled="loading" 
                @click="handleRegister"
              >
                <span v-if="!loading" class="btn-content">
                  Tạo tài khoản ngay
                  <el-icon class="btn-icon"><ArrowRight /></el-icon>
                </span>
                <span v-else class="btn-content">Đang khởi tạo...</span>
              </button>
            </el-form>

            <div class="switch-mode-text">
              Đã có tài khoản? 
              <a href="javascript:void(0)" class="highlight-link" @click="switchMode('login')">Quay lại Đăng nhập</a>
            </div>
          </div>

          <!-- MODE 3: FORGOT PASSWORD (HOÀN CHỈNH 3 BƯỚC + OTP) -->
          <div v-else-if="mode === 'forgot'" key="forgot" class="form-section">
            <div class="form-title-wrap">
              <h2 class="form-title">Khôi phục mật khẩu</h2>
              <p class="form-desc">Quy trình đặt lại mật khẩu bảo mật qua mã xác thực OTP.</p>
            </div>

            <!-- Step Wizard Indicator -->
            <div class="wizard-steps">
              <div :class="['step-item', { active: forgotStep >= 1, current: forgotStep === 1 }]">
                <div class="step-circle">1</div>
                <span class="step-label">Tài khoản</span>
              </div>
              <div class="step-line" :class="{ filled: forgotStep >= 2 }"></div>
              <div :class="['step-item', { active: forgotStep >= 2, current: forgotStep === 2 }]">
                <div class="step-circle">2</div>
                <span class="step-label">Nhập OTP</span>
              </div>
              <div class="step-line" :class="{ filled: forgotStep >= 3 }"></div>
              <div :class="['step-item', { active: forgotStep >= 3, current: forgotStep === 3 }]">
                <div class="step-circle">3</div>
                <span class="step-label">Mật khẩu mới</span>
              </div>
            </div>

            <!-- BƯỚC 1: NHẬP TÀI KHOẢN & CHỌN KÊNH -->
            <div v-if="forgotStep === 1" class="step-content">
              <el-form :model="forgotForm" @keyup.enter="handleSendOtp" class="custom-form">
                <div class="form-group">
                  <label class="form-label">Tên đăng nhập hoặc Email tài khoản</label>
                  <el-input 
                    v-model="forgotForm.identifier" 
                    placeholder="Nhập username (ví dụ: admin)" 
                    size="large"
                    class="modern-input"
                  >
                    <template #prefix><el-icon><User /></el-icon></template>
                  </el-input>
                </div>

                <!-- KÊNH NHẬN MÃ OTP -->
                <div class="form-group">
                  <label class="form-label">Phương thức nhận mã xác thực OTP</label>
                  <div class="channel-grid">
                    <div 
                      class="channel-box" 
                      :class="{ active: forgotForm.channel === 'EMAIL' }"
                      @click="forgotForm.channel = 'EMAIL'"
                    >
                      <div class="channel-box-header">
                        <span class="channel-emoji">✉️</span>
                        <div class="channel-radio-dot"></div>
                      </div>
                      <div class="channel-box-title">Gmail / Email</div>
                      <div class="channel-box-sub">Gửi OTP tới hộp thư</div>
                    </div>

                    <div 
                      class="channel-box" 
                      :class="{ active: forgotForm.channel === 'TELEGRAM' }"
                      @click="forgotForm.channel = 'TELEGRAM'"
                    >
                      <div class="channel-box-header">
                        <span class="channel-emoji">✈️</span>
                        <div class="channel-radio-dot"></div>
                      </div>
                      <div class="channel-box-title">Telegram Bot</div>
                      <div class="channel-box-sub">Nhận tin nhắn tức thì</div>
                    </div>
                  </div>
                </div>

                <!-- Extra input when Email is chosen -->
                <div v-if="forgotForm.channel === 'EMAIL'" class="form-group sub-input-anim">
                  <label class="form-label">Email nhận OTP</label>
                  <el-input 
                    v-model="forgotForm.email" 
                    placeholder="Nhập email nhận mã OTP (ví dụ: your-email@gmail.com)" 
                    size="large"
                    class="modern-input"
                  >
                    <template #prefix><el-icon><Message /></el-icon></template>
                  </el-input>
                  <div class="channel-hint">
                    💡 Để trống hệ thống sẽ gửi về email đã đăng ký của tài khoản.
                  </div>
                </div>

                <!-- Extra input when Telegram is chosen -->
                <div v-else-if="forgotForm.channel === 'TELEGRAM'" class="form-group sub-input-anim">
                  <label class="form-label">Telegram Chat ID (Tùy chọn)</label>
                  <el-input 
                    v-model="forgotForm.telegramChatId" 
                    placeholder="Nhập Telegram Chat ID của bạn (ví dụ: 123456789)" 
                    size="large"
                    class="modern-input"
                  >
                    <template #prefix><el-icon><ChatDotRound /></el-icon></template>
                  </el-input>
                  <div class="channel-hint">
                    💡 Để trống hệ thống sẽ tự động gửi tới Telegram đã cấu hình.
                  </div>
                </div>

                <div class="security-info-box">
                  <p>⏰ Mã xác thực OTP gồm 6 chữ số và có thời hạn hiệu lực trong 15 phút.</p>
                </div>

                <!-- Reusable Math Captcha Anti-Spam -->
                <div class="form-group" style="margin-top: 4px;">
                  <BaseMathCaptcha 
                    ref="forgotCaptchaRef" 
                    v-model="isForgotCaptchaPassed"
                    label="Giải phép tính để nhận mã OTP (Chống spam)"
                  />
                </div>

                <button 
                  type="button" 
                  class="btn-gradient" 
                  :disabled="loading" 
                  @click="handleSendOtp"
                >
                  <span v-if="!loading" class="btn-content">
                    Gửi mã xác thực OTP
                    <el-icon class="btn-icon"><ArrowRight /></el-icon>
                  </span>
                  <span v-else class="btn-content">Đang gửi yêu cầu...</span>
                </button>
              </el-form>
            </div>

            <!-- BƯỚC 2: NHẬP MÃ OTP -->
            <div v-else-if="forgotStep === 2" class="step-content">
              <!-- Target Destination Pill -->
              <div v-if="serverTargetInfo" class="channel-destination-badge">
                <el-icon style="font-size: 16px; color: #38bdf8;"><Bell /></el-icon>
                <span>Mã đã được gửi tới: <strong>{{ serverTargetInfo }}</strong></span>
              </div>

              <!-- OTP Notification Banner (Dev & Test Preview) -->
              <div v-if="serverGeneratedOtp" class="otp-notice-pill">
                <el-icon style="font-size: 18px; color: #10b981;"><Key /></el-icon>
                <div class="otp-text-wrap">
                  <span class="otp-label-text">Mã xác thực OTP xem nhanh:</span>
                  <strong class="otp-highlight-number">{{ serverGeneratedOtp }}</strong>
                </div>
              </div>

              <el-form :model="forgotForm" @keyup.enter="handleVerifyOtp" class="custom-form">
                <div class="form-group">
                  <label class="form-label">Nhập mã OTP 6 chữ số</label>
                  <el-input 
                    v-model="forgotForm.otp" 
                    placeholder="Nhập 6 số (VD: 123456)" 
                    maxlength="6"
                    size="large"
                    class="modern-input otp-input-field"
                  >
                    <template #prefix><el-icon><Key /></el-icon></template>
                  </el-input>
                </div>

                <button 
                  type="button" 
                  class="btn-gradient" 
                  :disabled="loading" 
                  @click="handleVerifyOtp"
                >
                  <span v-if="!loading" class="btn-content">
                    Xác nhận mã OTP
                    <el-icon class="btn-icon"><ArrowRight /></el-icon>
                  </span>
                  <span v-else class="btn-content">Đang kiểm tra...</span>
                </button>

                <div class="resend-row">
                  <a href="javascript:void(0)" class="resend-link" @click="handleSendOtp">Chưa nhận được? Gửi lại mã OTP</a>
                </div>
              </el-form>
            </div>

            <!-- BƯỚC 3: THIẾT LẬP MẬT KHẨU MỚI -->
            <div v-else-if="forgotStep === 3" class="step-content">
              <el-form :model="forgotForm" @keyup.enter="handleResetPassword" class="custom-form">
                <div class="form-group">
                  <label class="form-label">Mật khẩu mới</label>
                  <el-input 
                    v-model="forgotForm.newPassword" 
                    type="password" 
                    placeholder="Tối thiểu 6 ký tự" 
                    show-password
                    size="large"
                    class="modern-input"
                  >
                    <template #prefix><el-icon><Lock /></el-icon></template>
                  </el-input>
                </div>

                <div class="form-group">
                  <label class="form-label">Xác nhận mật khẩu mới</label>
                  <el-input 
                    v-model="forgotForm.confirmPassword" 
                    type="password" 
                    placeholder="Nhập lại mật khẩu mới" 
                    show-password
                    size="large"
                    class="modern-input"
                  >
                    <template #prefix><el-icon><Key /></el-icon></template>
                  </el-input>
                </div>

                <button 
                  type="button" 
                  class="btn-gradient btn-gradient-emerald" 
                  :disabled="loading" 
                  @click="handleResetPassword"
                >
                  <span v-if="!loading" class="btn-content">
                    Cập nhật mật khẩu mới
                    <el-icon class="btn-icon"><ArrowRight /></el-icon>
                  </span>
                  <span v-else class="btn-content">Đang lưu mật khẩu...</span>
                </button>
              </el-form>
            </div>

            <!-- BƯỚC 4: THÀNH CÔNG -->
            <div v-else-if="forgotStep === 4" class="step-content text-center">
              <div class="success-icon-badge">
                <svg viewBox="0 0 24 24" width="40" height="40" fill="none" stroke="#10b981" stroke-width="2.5">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M5 13l4 4L19 7" />
                </svg>
              </div>
              <h3 class="success-title">Đổi mật khẩu thành công!</h3>
              <p class="success-desc">Mật khẩu tài khoản của bạn đã được cập nhật vào cơ sở dữ liệu.</p>
              
              <button 
                type="button" 
                class="btn-gradient" 
                @click="goToLoginAfterReset"
              >
                <span class="btn-content">
                  Đăng nhập ngay
                  <el-icon class="btn-icon"><ArrowRight /></el-icon>
                </span>
              </button>
            </div>

            <div class="switch-mode-text">
              <a href="javascript:void(0)" class="highlight-link back-link" @click="switchMode('login')">
                <el-icon><Back /></el-icon> Quay lại Đăng nhập
              </a>
            </div>
          </div>
        </transition>
      </div>

      <!-- Footer Info -->
      <div class="auth-footer">
        © 2026 ERP Cloud • Powered by Spring Boot 3 & Vue 3 • Hosted on Vercel
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../store/auth'
import { ElMessage } from 'element-plus'
import { User, Lock, Key, ArrowRight, Back, Message, ChatDotRound, Bell } from '@element-plus/icons-vue'
import api from '../api'

const router = useRouter()
const authStore = useAuthStore()

// 'login' | 'register' | 'forgot'
const mode = ref('login')
const loading = ref(false)
const loadingText = ref('Đang xử lý...')
const loadingSubText = ref('')
const rememberMe = ref(true)

// Math Captcha Anti-Spam references & states
const loginCaptchaRef = ref(null)
const isLoginCaptchaPassed = ref(false)

const forgotCaptchaRef = ref(null)
const isForgotCaptchaPassed = ref(false)

const loginForm = ref({ username: 'admin', password: 'password' })
const regForm = ref({ username: '', password: '', confirmPassword: '' })

// Forgot password state
const forgotStep = ref(1) // 1: request, 2: otp, 3: new pass, 4: done
const serverGeneratedOtp = ref('')
const serverTargetInfo = ref('')
const forgotForm = ref({
  identifier: '',
  channel: 'EMAIL', // 'EMAIL' | 'TELEGRAM'
  email: '',
  telegramChatId: '',
  otp: '',
  newPassword: '',
  confirmPassword: ''
})

const switchMode = (newMode) => {
  mode.value = newMode
  if (newMode === 'forgot') {
    forgotStep.value = 1
    serverGeneratedOtp.value = ''
    serverTargetInfo.value = ''
    forgotForm.value = {
      identifier: '',
      channel: 'EMAIL',
      email: '',
      telegramChatId: '',
      otp: '',
      newPassword: '',
      confirmPassword: ''
    }
    if (forgotCaptchaRef.value) forgotCaptchaRef.value.reset()
  } else if (newMode === 'login') {
    if (loginCaptchaRef.value) loginCaptchaRef.value.reset()
  }
}

const fillDemoAccount = () => {
  loginForm.value.username = 'admin'
  loginForm.value.password = 'password'
  ElMessage.info('Đã tự động điền tài khoản quản trị Demo!')
}

const handleLogin = async () => {
  if (!loginForm.value.username || !loginForm.value.password) {
    ElMessage.warning('Vui lòng điền đầy đủ tên đăng nhập và mật khẩu!')
    return
  }

  // Chống spam: Bắt buộc giải đúng phép tính bảo vệ
  if (loginCaptchaRef.value && !loginCaptchaRef.value.validate()) {
    ElMessage.warning('Vui lòng giải đúng phép tính để đăng nhập!')
    return
  }

  loadingText.value = 'Đang xác thực tài khoản...'
  loadingSubText.value = 'Hệ thống đang kiểm tra thông tin đăng nhập của bạn...'
  loading.value = true
  const success = await authStore.login(loginForm.value.username, loginForm.value.password)
  loading.value = false
  if (success) {
    ElMessage.success('Đăng nhập thành công!')
    router.push('/')
  } else {
    ElMessage.error('Tên đăng nhập hoặc mật khẩu không chính xác!')
    if (loginCaptchaRef.value) loginCaptchaRef.value.reset()
  }
}

const handleRegister = async () => {
  if (!regForm.value.username || !regForm.value.password) {
    ElMessage.warning('Vui lòng điền đầy đủ thông tin!')
    return
  }
  if (regForm.value.password !== regForm.value.confirmPassword) {
    ElMessage.error('Mật khẩu xác nhận không trùng khớp!')
    return
  }
  if (regForm.value.password.length < 6) {
    ElMessage.warning('Mật khẩu phải có ít nhất 6 ký tự!')
    return
  }

  loadingText.value = 'Đang tạo tài khoản mới...'
  loadingSubText.value = 'Đang lưu trữ dữ liệu người dùng...'
  loading.value = true
  try {
    await api.post('/auth/register', {
      username: regForm.value.username,
      password: regForm.value.password
    })
    ElMessage.success('Tạo tài khoản thành công! Bạn có thể đăng nhập ngay.')
    loginForm.value.username = regForm.value.username
    loginForm.value.password = regForm.value.password
    regForm.value = { username: '', password: '', confirmPassword: '' }
    mode.value = 'login'
  } catch (err) {
    ElMessage.error(err.response?.data?.message || err.response?.data || 'Không thể tạo tài khoản!')
  } finally {
    loading.value = false
  }
}

// BƯỚC 1: GỬI YÊU CẦU MÃ OTP (HỖ TRỢ EMAIL HOẶC TELEGRAM)
const handleSendOtp = async () => {
  if (!forgotForm.value.identifier || !forgotForm.value.identifier.trim()) {
    ElMessage.warning('Vui lòng nhập tên đăng nhập hoặc email!')
    return
  }

  // Chống spam: Bắt buộc giải đúng phép toán
  if (forgotCaptchaRef.value && !forgotCaptchaRef.value.validate()) {
    ElMessage.warning('Vui lòng giải đúng phép tính để nhận mã OTP!')
    return
  }

  loadingText.value = forgotForm.value.channel === 'EMAIL' 
    ? 'Đang gửi mã OTP qua Email...' 
    : 'Đang gửi mã OTP qua Telegram...'
  loadingSubText.value = 'Hệ thống đang bảo mật mã và chuyển phát tức thì...'
  loading.value = true
  try {
    const payload = {
      identifier: forgotForm.value.identifier.trim(),
      channel: forgotForm.value.channel,
      email: forgotForm.value.email ? forgotForm.value.email.trim() : undefined,
      telegramChatId: forgotForm.value.telegramChatId ? forgotForm.value.telegramChatId.trim() : undefined
    }

    const res = await api.post('/auth/forgot-password', payload)
    serverGeneratedOtp.value = res.data.otp || ''
    serverTargetInfo.value = res.data.target || (forgotForm.value.channel === 'EMAIL' ? 'Hộp thư Email' : 'Telegram Bot')
    forgotStep.value = 2
    ElMessage.success(res.data.message || 'Mã xác thực OTP đã được gửi thành công!')
  } catch (err) {
    ElMessage.error(err.response?.data?.message || 'Không tìm thấy tài khoản trong hệ thống!')
    if (forgotCaptchaRef.value) forgotCaptchaRef.value.reset()
  } finally {
    loading.value = false
  }
}

// BƯỚC 2: XÁC NHẬN MÃ OTP
const handleVerifyOtp = async () => {
  if (!forgotForm.value.otp || forgotForm.value.otp.trim().length !== 6) {
    ElMessage.warning('Vui lòng nhập đầy đủ mã OTP 6 chữ số!')
    return
  }

  loadingText.value = 'Đang kiểm tra mã OTP...'
  loadingSubText.value = 'Xác minh tính hợp lệ và thời hạn sử dụng...'
  loading.value = true
  try {
    await api.post('/auth/verify-otp', {
      otp: forgotForm.value.otp.trim()
    })
    forgotStep.value = 3
    ElMessage.success('Xác thực OTP thành công! Vui lòng tạo mật khẩu mới.')
  } catch (err) {
    ElMessage.error(err.response?.data?.message || 'Mã OTP không chính xác hoặc đã hết hạn!')
  } finally {
    loading.value = false
  }
}

// BƯỚC 3: ĐẶT MẬT KHẨU MỚI
const handleResetPassword = async () => {
  if (!forgotForm.value.newPassword || forgotForm.value.newPassword.length < 6) {
    ElMessage.warning('Mật khẩu mới phải có ít nhất 6 ký tự!')
    return
  }
  if (forgotForm.value.newPassword !== forgotForm.value.confirmPassword) {
    ElMessage.error('Mật khẩu xác nhận không trùng khớp!')
    return
  }

  loadingText.value = 'Đang đặt lại mật khẩu mới...'
  loadingSubText.value = 'Mật khẩu đang được mã hóa an toàn vào cơ sở dữ liệu...'
  loading.value = true
  try {
    await api.post('/auth/reset-password', {
      token: forgotForm.value.otp.trim(),
      newPassword: forgotForm.value.newPassword
    })
    forgotStep.value = 4
    ElMessage.success('Đặt lại mật khẩu thành công!')
  } catch (err) {
    ElMessage.error(err.response?.data?.message || 'Lỗi khi đặt lại mật khẩu!')
  } finally {
    loading.value = false
  }
}

const goToLoginAfterReset = () => {
  loginForm.value.username = forgotForm.value.identifier
  loginForm.value.password = forgotForm.value.newPassword
  mode.value = 'login'
}
</script>

<style scoped>
/* Main Container - Crisp Modern White & Light Theme */
.auth-wrapper {
  position: relative;
  min-height: 100vh;
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #f8fafc 0%, #e2e8f0 50%, #f1f5f9 100%);
  overflow: hidden;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
  padding: 24px 16px;
  box-sizing: border-box;
}

/* Soft Pastel Ambient Lighting */
.glow-orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(90px);
  pointer-events: none;
  z-index: 1;
}
.orb-1 {
  width: 450px;
  height: 450px;
  background: radial-gradient(circle, #c7d2fe 0%, transparent 70%);
  top: -100px;
  left: -100px;
  opacity: 0.5;
}
.orb-2 {
  width: 500px;
  height: 500px;
  background: radial-gradient(circle, #bae6fd 0%, transparent 70%);
  bottom: -150px;
  right: -100px;
  opacity: 0.5;
}
.orb-3 {
  width: 350px;
  height: 350px;
  background: radial-gradient(circle, #ddd6fe 0%, transparent 70%);
  top: 40%;
  left: 60%;
  opacity: 0.4;
}

/* Auth Center Column */
.auth-container {
  position: relative;
  z-index: 2;
  width: 100%;
  max-width: 440px;
  display: flex;
  flex-direction: column;
  align-items: center;
}

/* Brand Header */
.brand-header {
  text-align: center;
  margin-bottom: 24px;
}
.brand-logo {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 58px;
  height: 58px;
  border-radius: 18px;
  background: linear-gradient(135deg, #4f46e5 0%, #3b82f6 50%, #0284c7 100%);
  color: #ffffff;
  box-shadow: 0 10px 25px -5px rgba(79, 70, 229, 0.35);
  margin-bottom: 12px;
}
.brand-title {
  margin: 0;
  font-size: 26px;
  font-weight: 800;
  letter-spacing: -0.5px;
  color: #0f172a;
}
.brand-subtitle {
  margin: 6px 0 0 0;
  font-size: 13.5px;
  color: #64748b;
  font-weight: 400;
}

/* Main White Card (Crisp High-Contrast Glassmorphism) */
.glass-card {
  position: relative;
  width: 100%;
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 24px;
  padding: 32px 28px;
  box-shadow: 0 20px 45px -10px rgba(15, 23, 42, 0.08), 0 1px 3px rgba(15, 23, 42, 0.04);
  box-sizing: border-box;
  overflow: hidden;
}

/* Form Headings */
.form-title-wrap {
  margin-bottom: 20px;
}
.form-title {
  margin: 0;
  font-size: 22px;
  font-weight: 800;
  color: #0f172a;
}
.form-desc {
  margin: 6px 0 0 0;
  font-size: 13.5px;
  color: #64748b;
}

/* Wizard Steps */
.wizard-steps {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
  padding: 0 8px;
}

.step-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  opacity: 0.5;
  transition: all 0.3s ease;
}

.step-item.active {
  opacity: 1;
}

.step-circle {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: #f1f5f9;
  border: 1px solid #cbd5e1;
  color: #64748b;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 700;
}

.step-item.current .step-circle {
  background: #4f46e5;
  border-color: #4f46e5;
  color: #ffffff;
  box-shadow: 0 4px 12px rgba(79, 70, 229, 0.35);
}

.step-label {
  font-size: 11px;
  color: #64748b;
  font-weight: 500;
}
.step-item.active .step-label {
  color: #0f172a;
  font-weight: 600;
}

.step-line {
  flex: 1;
  height: 2px;
  background: #e2e8f0;
  margin: 0 6px 16px 6px;
  transition: all 0.3s ease;
}

.step-line.filled {
  background: #4f46e5;
}

/* Demo Credential Pill */
.demo-credential-pill {
  display: flex;
  align-items: center;
  gap: 8px;
  background: #eef2ff;
  border: 1px solid #c7d2fe;
  padding: 9px 12px;
  border-radius: 12px;
  margin-bottom: 20px;
  font-size: 12px;
  color: #4338ca;
  cursor: pointer;
  transition: all 0.25s ease;
}
.demo-credential-pill:hover {
  background: #e0e7ff;
  border-color: #a5b4fc;
  transform: translateY(-1px);
}
.pulse-dot {
  width: 8px;
  height: 8px;
  background-color: #10b981;
  border-radius: 50%;
  box-shadow: 0 0 8px #10b981;
  flex-shrink: 0;
}

/* Channel Selection Styles */
.channel-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}
.channel-box {
  background: #f8fafc;
  border: 1.5px solid #e2e8f0;
  border-radius: 14px;
  padding: 12px 14px;
  cursor: pointer;
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.channel-box:hover {
  border-color: #6366f1;
  background: #ffffff;
  transform: translateY(-2px);
  box-shadow: 0 6px 16px -4px rgba(15, 23, 42, 0.08);
}
.channel-box.active {
  border-color: #4f46e5;
  background: #eef2ff;
  box-shadow: 0 0 0 1px #4f46e5, 0 4px 14px -2px rgba(79, 70, 229, 0.2);
}
.channel-box-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.channel-emoji {
  font-size: 20px;
}
.channel-radio-dot {
  width: 14px;
  height: 14px;
  border-radius: 50%;
  border: 2px solid #cbd5e1;
  transition: all 0.2s ease;
  position: relative;
}
.channel-box.active .channel-radio-dot {
  border-color: #4f46e5;
  background-color: #4f46e5;
  box-shadow: 0 0 6px rgba(79, 70, 229, 0.6);
}
.channel-box-title {
  font-size: 13px;
  font-weight: 700;
  color: #0f172a;
  margin-top: 4px;
}
.channel-box-sub {
  font-size: 11px;
  color: #64748b;
}
.channel-hint {
  font-size: 11.5px;
  color: #64748b;
  margin-top: 2px;
  line-height: 1.4;
}
.channel-hint code {
  color: #0284c7;
  background: #e0f2fe;
  padding: 2px 5px;
  border-radius: 4px;
  font-weight: 600;
}
.sub-input-anim {
  animation: fadeIn 0.3s ease;
}
.channel-destination-badge {
  background: #f0f9ff;
  border: 1px solid #bae6fd;
  border-radius: 12px;
  padding: 10px 14px;
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 16px;
  font-size: 13px;
  color: #0369a1;
}
.channel-destination-badge strong {
  color: #0284c7;
}

/* OTP Notification Banner */
.otp-notice-pill {
  background: #f0fdf4;
  border: 1px solid #bbf7d0;
  border-radius: 14px;
  padding: 12px 14px;
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
}
.otp-text-wrap {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.otp-label-text {
  font-size: 12px;
  color: #166534;
}
.otp-highlight-number {
  font-size: 20px;
  letter-spacing: 4px;
  color: #15803d;
  font-weight: 800;
}

.otp-input-field :deep(input) {
  letter-spacing: 4px;
  font-size: 18px;
  font-weight: 700;
  text-align: center;
}

.resend-row {
  text-align: center;
  margin-top: 10px;
}
.resend-link {
  font-size: 12px;
  color: #4f46e5;
  text-decoration: none;
}
.resend-link:hover {
  color: #3730a3;
  text-decoration: underline;
}

/* Form Elements */
.custom-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.form-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.label-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.form-label {
  font-size: 13px;
  font-weight: 600;
  color: #1e293b;
}
.forgot-link {
  font-size: 12px;
  color: #4f46e5;
  font-weight: 500;
  text-decoration: none;
  transition: color 0.2s;
}
.forgot-link:hover {
  color: #3730a3;
  text-decoration: underline;
}

/* Modern Inputs in Light Theme */
:deep(.modern-input .el-input__wrapper) {
  background-color: #f8fafc !important;
  border: 1.5px solid #cbd5e1 !important;
  border-radius: 14px !important;
  box-shadow: none !important;
  padding: 8px 14px !important;
  transition: all 0.25s ease;
}
:deep(.modern-input .el-input__wrapper:hover) {
  border-color: #6366f1 !important;
  background-color: #ffffff !important;
}
:deep(.modern-input .el-input__wrapper.is-focus) {
  border-color: #4f46e5 !important;
  background-color: #ffffff !important;
  box-shadow: 0 0 0 3px rgba(79, 70, 229, 0.15) !important;
}
:deep(.modern-input input) {
  color: #0f172a !important;
  font-size: 14px;
  font-weight: 500;
}
:deep(.modern-input input::placeholder) {
  color: #94a3b8 !important;
}
:deep(.modern-input .el-input__prefix-inner) {
  color: #64748b;
  font-size: 16px;
}

/* Checkbox */
.remember-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
:deep(.custom-checkbox .el-checkbox__label) {
  color: #475569 !important;
  font-size: 13px;
  font-weight: 500;
}

/* Action Button */
.btn-gradient {
  width: 100%;
  padding: 13px 20px;
  border-radius: 14px;
  border: none;
  background: linear-gradient(135deg, #4f46e5 0%, #4338ca 50%, #2563eb 100%);
  color: #ffffff;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  box-shadow: 0 8px 24px -4px rgba(79, 70, 229, 0.4);
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  margin-top: 6px;
}
.btn-gradient:hover:not(:disabled) {
  background: linear-gradient(135deg, #4338ca 0%, #3730a3 50%, #1d4ed8 100%);
  box-shadow: 0 12px 28px -4px rgba(79, 70, 229, 0.55);
  transform: translateY(-2px);
}
.btn-gradient:active:not(:disabled) {
  transform: translateY(1px) scale(0.99);
  box-shadow: 0 4px 12px rgba(79, 70, 229, 0.3);
}
.btn-gradient:disabled {
  opacity: 0.65;
  cursor: not-allowed;
}
.btn-gradient-emerald {
  background: linear-gradient(135deg, #10b981 0%, #059669 50%, #0d9488 100%);
  box-shadow: 0 8px 24px -4px rgba(16, 185, 129, 0.35);
}
.btn-gradient-emerald:hover:not(:disabled) {
  background: linear-gradient(135deg, #059669 0%, #047857 50%, #0f766e 100%);
  box-shadow: 0 12px 28px -4px rgba(16, 185, 129, 0.5);
}
.btn-content {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}
.btn-icon {
  font-size: 16px;
  transition: transform 0.25s ease;
}
.btn-gradient:hover .btn-icon {
  transform: translateX(4px);
}

/* Security Box for Forgot view */
.security-info-box {
  background: #f0f9ff;
  border: 1px dashed #7dd3fc;
  padding: 12px 14px;
  border-radius: 12px;
  color: #0369a1;
  font-size: 12px;
  line-height: 1.5;
}
.security-info-box p {
  margin: 0;
}

/* Success Card */
.text-center {
  text-align: center;
}
.success-icon-badge {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  background: #f0fdf4;
  border: 2px solid #bbf7d0;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 16px auto;
}
.success-title {
  font-size: 20px;
  font-weight: 800;
  color: #0f172a;
  margin: 0 0 8px 0;
}
.success-desc {
  font-size: 13.5px;
  color: #64748b;
  margin: 0 0 24px 0;
}

/* Switch Link */
.switch-mode-text {
  text-align: center;
  margin-top: 24px;
  font-size: 13px;
  color: #64748b;
}
.highlight-link {
  color: #4f46e5;
  font-weight: 600;
  text-decoration: none;
  transition: color 0.2s;
}
.highlight-link:hover {
  color: #3730a3;
  text-decoration: underline;
}
.back-link {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

/* Footer */
.auth-footer {
  margin-top: 24px;
  font-size: 12px;
  color: #64748b;
  text-align: center;
}

/* Animations */
.fade-slide-enter-active,
.fade-slide-leave-active {
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
}
.fade-slide-enter-from {
  opacity: 0;
  transform: translateY(8px);
}
.fade-slide-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}

/* Responsive Media Queries */
@media (max-width: 480px) {
  .auth-wrapper {
    padding: 16px 12px;
  }
  .glass-card {
    padding: 22px 16px;
    border-radius: 18px;
  }
  .brand-title {
    font-size: 22px;
  }
  .brand-subtitle {
    font-size: 12px;
  }
  .demo-credential-pill {
    padding: 8px 10px;
    font-size: 11px;
    line-height: 1.4;
  }
  .form-title {
    font-size: 20px;
  }
}
</style>
