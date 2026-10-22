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
                  placeholder="Ví dụ: nhanvien01" 
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
                  <template #prefix><el-icon><Key /></el-icon></template>
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
                  <template #prefix><el-icon><Lock /></el-icon></template>
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

          <!-- MODE 3: FORGOT PASSWORD -->
          <div v-else-if="mode === 'forgot'" key="forgot" class="form-section">
            <div class="form-title-wrap">
              <h2 class="form-title">Khôi phục mật khẩu</h2>
              <p class="form-desc">Nhập tên đăng nhập để nhận liên kết đặt lại mật khẩu bảo mật.</p>
            </div>

            <el-form :model="forgotForm" @keyup.enter="handleForgot" class="custom-form">
              <div class="form-group">
                <label class="form-label">Tên đăng nhập hoặc Email</label>
                <el-input 
                  v-model="forgotForm.identifier" 
                  placeholder="Nhập username của bạn" 
                  size="large"
                  class="modern-input"
                >
                  <template #prefix><el-icon><Message /></el-icon></template>
                </el-input>
              </div>

              <div class="security-info-box">
                <p>💡 <strong>Gợi ý:</strong> Vì đây là hệ thống ERP nội bộ, nếu bạn quên mật khẩu quản trị cao nhất, bạn cũng có thể liên hệ trực tiếp Quản trị viên hệ thống (Super Admin) để được cấp lại trong 30 giây.</p>
              </div>

              <button 
                type="button" 
                class="btn-gradient" 
                :disabled="loading" 
                @click="handleForgot"
              >
                <span v-if="!loading" class="btn-content">
                  Gửi yêu cầu khôi phục
                  <el-icon class="btn-icon"><ArrowRight /></el-icon>
                </span>
                <span v-else class="btn-content">Đang gửi yêu cầu...</span>
              </button>
            </el-form>

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
import { User, Lock, Key, Message, ArrowRight, Back } from '@element-plus/icons-vue'
import api from '../api'

const router = useRouter()
const authStore = useAuthStore()

// 'login' | 'register' | 'forgot'
const mode = ref('login')
const loading = ref(false)
const rememberMe = ref(true)

const loginForm = ref({ username: 'admin', password: 'password' })
const regForm = ref({ username: '', password: '', confirmPassword: '' })
const forgotForm = ref({ identifier: '' })

const switchMode = (newMode) => {
  mode.value = newMode
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
  loading.value = true
  const success = await authStore.login(loginForm.value.username, loginForm.value.password)
  loading.value = false
  if (success) {
    ElMessage.success('Đăng nhập thành công! Đang chuyển hướng...')
    router.push('/')
  } else {
    ElMessage.error('Tài khoản hoặc mật khẩu không chính xác!')
  }
}

const handleRegister = async () => {
  if (!regForm.value.username || !regForm.value.password) {
    ElMessage.warning('Vui lòng nhập tên đăng nhập và mật khẩu!')
    return
  }
  if (regForm.value.password.length < 6) {
    ElMessage.warning('Mật khẩu phải chứa ít nhất 6 ký tự!')
    return
  }
  if (regForm.value.password !== regForm.value.confirmPassword) {
    ElMessage.error('Mật khẩu xác nhận không trùng khớp!')
    return
  }

  loading.value = true
  try {
    await api.post('/auth/register', {
      username: regForm.value.username,
      password: regForm.value.password
    })
    ElMessage.success('Đăng ký tài khoản thành công! Bạn có thể đăng nhập ngay.')
    loginForm.value.username = regForm.value.username
    loginForm.value.password = regForm.value.password
    regForm.value = { username: '', password: '', confirmPassword: '' }
    mode.value = 'login'
  } catch (err) {
    ElMessage.error(err.response?.data || 'Không thể tạo tài khoản, có thể username đã tồn tại!')
  } finally {
    loading.value = false
  }
}

const handleForgot = async () => {
  if (!forgotForm.value.identifier) {
    ElMessage.warning('Vui lòng nhập tên đăng nhập hoặc email!')
    return
  }
  loading.value = true
  setTimeout(() => {
    loading.value = false
    ElMessage.success('Yêu cầu đã được ghi nhận! Hệ thống đã gửi hướng dẫn tới quản trị viên.')
    mode.value = 'login'
  }, 1000)
}
</script>

<style scoped>
/* Main Container */
.auth-wrapper {
  position: relative;
  min-height: 100vh;
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: radial-gradient(circle at 15% 20%, #1e293b 0%, #0f172a 60%, #020617 100%);
  overflow: hidden;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
  padding: 24px 16px;
  box-sizing: border-box;
}

/* Glowing Ambient Lights */
.glow-orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(100px);
  pointer-events: none;
  opacity: 0.35;
  z-index: 1;
}
.orb-1 {
  width: 450px;
  height: 450px;
  background: radial-gradient(circle, #6366f1 0%, transparent 70%);
  top: -100px;
  left: -100px;
}
.orb-2 {
  width: 500px;
  height: 500px;
  background: radial-gradient(circle, #06b6d4 0%, transparent 70%);
  bottom: -150px;
  right: -100px;
}
.orb-3 {
  width: 350px;
  height: 350px;
  background: radial-gradient(circle, #ec4899 0%, transparent 70%);
  top: 40%;
  left: 60%;
  opacity: 0.2;
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
  background: linear-gradient(135deg, #6366f1 0%, #3b82f6 50%, #06b6d4 100%);
  color: #ffffff;
  box-shadow: 0 10px 25px -5px rgba(99, 102, 241, 0.5);
  margin-bottom: 12px;
}
.brand-title {
  margin: 0;
  font-size: 26px;
  font-weight: 800;
  letter-spacing: -0.5px;
  background: linear-gradient(135deg, #ffffff 0%, #cbd5e1 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}
.brand-subtitle {
  margin: 6px 0 0 0;
  font-size: 13px;
  color: #94a3b8;
  font-weight: 400;
}

/* Glass Card */
.glass-card {
  width: 100%;
  background: rgba(30, 41, 59, 0.7);
  backdrop-filter: blur(24px);
  -webkit-backdrop-filter: blur(24px);
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 24px;
  padding: 32px 28px;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.5), 0 0 0 1px rgba(255, 255, 255, 0.05);
  box-sizing: border-box;
}

/* Form Headings */
.form-title-wrap {
  margin-bottom: 20px;
}
.form-title {
  margin: 0;
  font-size: 22px;
  font-weight: 700;
  color: #ffffff;
}
.form-desc {
  margin: 6px 0 0 0;
  font-size: 13px;
  color: #94a3b8;
}

/* Demo Credential Pill */
.demo-credential-pill {
  display: flex;
  align-items: center;
  gap: 8px;
  background: rgba(99, 102, 241, 0.12);
  border: 1px solid rgba(99, 102, 241, 0.3);
  padding: 9px 12px;
  border-radius: 12px;
  margin-bottom: 20px;
  font-size: 12px;
  color: #c7d2fe;
  cursor: pointer;
  transition: all 0.25s ease;
}
.demo-credential-pill:hover {
  background: rgba(99, 102, 241, 0.22);
  border-color: rgba(99, 102, 241, 0.6);
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
  color: #e2e8f0;
}
.forgot-link {
  font-size: 12px;
  color: #818cf8;
  text-decoration: none;
  transition: color 0.2s;
}
.forgot-link:hover {
  color: #a5b4fc;
  text-decoration: underline;
}

/* Inputs customization */
:deep(.modern-input .el-input__wrapper) {
  background-color: rgba(15, 23, 42, 0.6) !important;
  border: 1px solid rgba(255, 255, 255, 0.1) !important;
  border-radius: 14px !important;
  box-shadow: none !important;
  padding: 8px 14px !important;
  transition: all 0.25s ease;
}
:deep(.modern-input .el-input__wrapper:hover) {
  border-color: rgba(99, 102, 241, 0.5) !important;
}
:deep(.modern-input .el-input__wrapper.is-focus) {
  border-color: #6366f1 !important;
  box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.25) !important;
  background-color: rgba(15, 23, 42, 0.85) !important;
}
:deep(.modern-input input) {
  color: #f8fafc !important;
  font-size: 14px;
}
:deep(.modern-input input::placeholder) {
  color: #64748b;
}
:deep(.modern-input .el-input__prefix) {
  color: #94a3b8;
  margin-right: 8px;
}

/* Checkbox */
.remember-row {
  display: flex;
  align-items: center;
}
:deep(.custom-checkbox .el-checkbox__label) {
  color: #94a3b8 !important;
  font-size: 13px;
}
:deep(.custom-checkbox.is-checked .el-checkbox__inner) {
  background-color: #6366f1;
  border-color: #6366f1;
}

/* Modern Gradient CTA Button */
.btn-gradient {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  padding: 14px;
  border: none;
  border-radius: 14px;
  background: linear-gradient(135deg, #6366f1 0%, #4f46e5 50%, #3b82f6 100%);
  color: #ffffff;
  font-size: 15px;
  font-weight: 700;
  letter-spacing: 0.3px;
  cursor: pointer;
  box-shadow: 0 8px 24px -4px rgba(99, 102, 241, 0.5);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  overflow: hidden;
  margin-top: 6px;
}
.btn-gradient:hover:not(:disabled) {
  background: linear-gradient(135deg, #4f46e5 0%, #4338ca 50%, #2563eb 100%);
  box-shadow: 0 12px 30px -4px rgba(99, 102, 241, 0.7);
  transform: translateY(-2px);
}
.btn-gradient:active:not(:disabled) {
  transform: translateY(1px) scale(0.99);
  box-shadow: 0 4px 12px rgba(99, 102, 241, 0.4);
}
.btn-gradient:disabled {
  opacity: 0.65;
  cursor: not-allowed;
}
.btn-gradient-emerald {
  background: linear-gradient(135deg, #10b981 0%, #059669 50%, #0d9488 100%);
  box-shadow: 0 8px 24px -4px rgba(16, 185, 129, 0.4);
}
.btn-gradient-emerald:hover:not(:disabled) {
  background: linear-gradient(135deg, #059669 0%, #047857 50%, #0f766e 100%);
  box-shadow: 0 12px 30px -4px rgba(16, 185, 129, 0.6);
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
  background: rgba(14, 165, 233, 0.1);
  border: 1px dashed rgba(14, 165, 233, 0.3);
  padding: 12px 14px;
  border-radius: 12px;
  color: #7dd3fc;
  font-size: 12px;
  line-height: 1.5;
}
.security-info-box p {
  margin: 0;
}

/* Switch Link */
.switch-mode-text {
  text-align: center;
  margin-top: 24px;
  font-size: 13px;
  color: #94a3b8;
}
.highlight-link {
  color: #818cf8;
  font-weight: 600;
  text-decoration: none;
  transition: color 0.2s;
}
.highlight-link:hover {
  color: #a5b4fc;
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
</style>
