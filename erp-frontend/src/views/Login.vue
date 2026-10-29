<template>
  <div class="auth-page">
    <!-- Ambient Background Lighting -->
    <div class="bg-gradient-mesh"></div>

    <div class="auth-shell">
      <!-- MODE 1: LOGIN (SPLIT SCREEN 2 COLUMNS - FIGMA IMAGE 1) -->
      <div v-if="mode === 'login'" class="login-split-card">
        <!-- Reusable Base Loading Overlay -->
        <BaseLoading 
          :active="loading" 
          :text="loadingText" 
          :sub-text="loadingSubText" 
        />

        <!-- LEFT COLUMN: BRAND HERO & SLA METRICS -->
        <div class="hero-column">
          <!-- Logo & Enterprise Badge -->
          <div class="brand-top">
            <div class="brand-logo-icon">
              <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="2.2">
                <circle cx="12" cy="12" r="3" fill="currentColor" />
                <path stroke-linecap="round" d="M12 3v3m0 12v3M3 12h3m12 0h3M5.6 5.6l2.1 2.1m8.6 8.6l2.1 2.1M5.6 18.4l2.1-2.1m8.6-8.6l2.1-2.1" />
              </svg>
            </div>
            <div class="brand-info">
              <div class="brand-title-row">
                <span class="brand-name">ERP Cloud</span>
                <span class="enterprise-pill">ENTERPRISE</span>
              </div>
              <span class="brand-sub">Hệ thống Quản trị Doanh nghiệp Thông minh</span>
            </div>
          </div>

          <!-- Hero Headline -->
          <div class="hero-body">
            <div class="system-status-pill">
              <span class="green-dot"></span>
              <span>Hệ thống phân tán hoạt động 100%</span>
            </div>

            <h1 class="hero-heading">
              Đột phá hiệu suất,<br />
              <span class="highlight-blue">kết nối tương lai</span> doanh nghiệp.
            </h1>

            <p class="hero-desc">
              Tối ưu hóa nguồn lực, quản trị luồng dữ liệu thời gian thực và đồng bộ hóa mọi quy trình chỉ trong một nền tảng duy nhất.
            </p>

            <!-- SLA & Security Live Metric Card -->
            <div class="sla-metric-card">
              <div class="sla-card-header">
                <div class="sla-title-group">
                  <span class="sla-icon">
                    <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2">
                      <rect x="2" y="3" width="20" height="14" rx="2" />
                      <line x1="8" y1="21" x2="16" y2="21" />
                      <line x1="12" y1="17" x2="12" y2="21" />
                    </svg>
                  </span>
                  <div>
                    <div class="sla-title">Chỉ số vận hành hệ thống</div>
                    <div class="sla-sub">Cập nhật theo thời gian thực</div>
                  </div>
                </div>
                <span class="online-pill">Online</span>
              </div>

              <div class="sla-divider"></div>

              <div class="sla-stats-grid">
                <div class="sla-stat-item">
                  <div class="sla-label">Độ tin cậy SLA</div>
                  <div class="sla-value">99.98%</div>
                  <div class="sla-sub-badge tier-badge">⚡ Chuẩn Tier 4</div>
                </div>

                <div class="sla-stat-separator"></div>

                <div class="sla-stat-item">
                  <div class="sla-label">Bảo mật đa tầng</div>
                  <div class="sla-value">ISO 27001</div>
                  <div class="sla-sub-badge soc-badge">🛡️ SOC 2 Type II</div>
                </div>
              </div>
            </div>
          </div>

          <!-- Hero Footer -->
          <div class="hero-footer">
            <span class="footer-badge">🔒 Mã hóa TLS 1.3 | AES-256</span>
            <span class="footer-dot">•</span>
            <span class="footer-badge">🛡️ FIDO2 & SSO Ready</span>
            <span class="version-tag">v4.2-SG01/WAS1-S</span>
          </div>
        </div>

        <!-- RIGHT COLUMN: LOGIN FORM -->
        <div class="form-column">
          <!-- Top Row: Language switcher -->
          <div class="form-top-bar">
            <div class="lang-pill">
              <span>🌐 VIE</span>
              <span class="chevron">⌄</span>
            </div>
          </div>

          <div class="form-inner">
            <!-- Form Header -->
            <div class="form-heading-group">
              <div class="form-title-row">
                <h2 class="form-title">Đăng nhập</h2>
                <span class="portal-badge">Cổng Quản trị</span>
              </div>
              <p class="form-subtitle">Truy cập không gian làm việc số cho doanh nghiệp</p>
            </div>

            <!-- Demo Account Quick Card (Figma Match) -->
            <div class="demo-card-banner" @click="fillDemoAccount">
              <div class="demo-card-left">
                <div class="demo-icon-box">
                  <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2">
                    <path stroke-linecap="round" stroke-linejoin="round" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
                  </svg>
                </div>
                <div class="demo-text">
                  <div class="demo-title">Tài khoản demo</div>
                  <div class="demo-sub">admin@enterprisegroup.vn</div>
                </div>
              </div>
              <button type="button" class="btn-demo-autofill">
                Điền mẫu ⚡
              </button>
            </div>

            <!-- Main Login Form -->
            <form @submit.prevent="handleLogin" class="auth-form-fields">
              <!-- Username Field -->
              <div class="input-block">
                <label class="input-label">Tên đăng nhập hoặc Email</label>
                <div class="input-wrap">
                  <span class="input-icon">
                    <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2">
                      <rect x="2" y="4" width="20" height="16" rx="2" />
                      <path d="M22 7l-8.97 5.7a1.94 1.94 0 01-2.06 0L2 7" />
                    </svg>
                  </span>
                  <input 
                    v-model="loginForm.username" 
                    type="text" 
                    placeholder="name@enterprise.vn" 
                    required 
                    autocomplete="username"
                    class="native-input"
                  />
                </div>
              </div>

              <!-- Password Field -->
              <div class="input-block">
                <div class="label-with-link">
                  <label class="input-label">Mật khẩu</label>
                  <a href="javascript:void(0)" class="link-forgot" @click="switchMode('forgot')">
                    Quên mật khẩu?
                  </a>
                </div>
                <div class="input-wrap">
                  <span class="input-icon">
                    <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2">
                      <rect x="3" y="11" width="18" height="11" rx="2" ry="2" />
                      <path d="M7 11V7a5 5 0 0110 0v4" />
                    </svg>
                  </span>
                  <input 
                    v-model="loginForm.password" 
                    :type="showPassword ? 'text' : 'password'" 
                    placeholder="••••••••••••" 
                    required 
                    autocomplete="current-password"
                    class="native-input"
                  />
                  <button type="button" class="eye-toggle" @click="showPassword = !showPassword">
                    <svg v-if="!showPassword" viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2">
                      <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
                      <circle cx="12" cy="12" r="3" />
                    </svg>
                    <svg v-else viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2">
                      <path d="M17.94 17.94A10.07 10.07 0 0112 20c-7 0-11-8-11-8a18.45 18.45 0 015.06-5.94M9.9 4.24A9.12 9.12 0 0112 4c7 0 11 8 11 8a18.5 18.5 0 01-2.16 3.19m-6.72-1.07a3 3 0 11-4.24-4.24" />
                      <line x1="1" y1="1" x2="23" y2="23" />
                    </svg>
                  </button>
                </div>
              </div>

              <!-- Options Row: Remember Me & Passkey -->
              <div class="options-row">
                <label class="checkbox-label">
                  <input type="checkbox" v-model="rememberMe" class="custom-chk" />
                  <span>Ghi nhớ đăng nhập</span>
                </label>
                <div class="passkey-pill">
                  <span>🔑 Passkey</span>
                </div>
              </div>

              <!-- Math Captcha Challenge -->
              <div class="captcha-box">
                <BaseMathCaptcha 
                  ref="loginCaptchaRef" 
                  v-model="isLoginCaptchaPassed"
                  label="Xác thực bảo vệ hệ thống"
                />
              </div>

              <!-- Main Submit Button -->
              <button type="submit" class="btn-primary-submit" :disabled="loading">
                <span>{{ loading ? 'Đang xác thực...' : 'Đăng nhập' }}</span>
                <span class="btn-arrow">➔</span>
              </button>
            </form>

            <!-- Social Logins Divider -->
            <div class="divider-text">
              <span>HOẶC TIẾP TỤC VỚI</span>
            </div>

            <!-- Social Logins (Google & Microsoft Entra) -->
            <div class="social-btn-grid">
              <button type="button" class="btn-social" @click="handleSocialLogin('Google')">
                <svg viewBox="0 0 24 24" width="18" height="18" class="social-icon">
                  <path fill="#4285F4" d="M23.745 12.27c0-.7-.06-1.4-.19-2.07H12v4.51h6.6c-.29 1.52-1.14 2.82-2.4 3.68v3.05h3.88c2.27-2.09 3.665-5.17 3.665-9.17z"/>
                  <path fill="#34A853" d="M12 24c3.24 0 5.95-1.08 7.93-2.91l-3.88-3.05c-1.08.72-2.45 1.16-4.05 1.16-3.12 0-5.77-2.1-6.72-4.93H1.24v3.15C3.26 21.41 7.33 24 12 24z"/>
                  <path fill="#FBBC05" d="M5.28 14.27c-.25-.72-.38-1.49-.38-2.27s.13-1.55.38-2.27V6.58H1.24C.45 8.16 0 9.98 0 12s.45 3.84 1.24 5.42l4.04-3.15z"/>
                  <path fill="#EA4335" d="M12 4.75c1.77 0 3.35.61 4.6 1.8l3.42-3.42C17.95 1.19 15.24 0 12 0 7.33 0 3.26 2.59 1.24 6.58l4.04 3.15c.95-2.83 3.6-4.98 6.72-4.98z"/>
                </svg>
                <span>Google ID</span>
              </button>

              <button type="button" class="btn-social" @click="handleSocialLogin('Microsoft')">
                <svg viewBox="0 0 24 24" width="18" height="18" class="social-icon">
                  <path fill="#F25022" d="M1 1h10v10H1z"/>
                  <path fill="#7FBA00" d="M13 1h10v10H13z"/>
                  <path fill="#00A4EF" d="M1 13h10v10H1z"/>
                  <path fill="#FFB900" d="M13 13h10v10H13z"/>
                </svg>
                <span>Microsoft Entra</span>
              </button>
            </div>

            <!-- Footer Legal Links -->
            <div class="form-bottom-legal">
              <div class="legal-links">
                <a href="#">Điều khoản</a>
                <span class="dot">•</span>
                <a href="#">Bảo mật</a>
                <span class="dot">•</span>
                <a href="#">Hỗ trợ</a>
              </div>
              <div class="copyright-text">© 2026 Enterprise Group</div>
            </div>
          </div>
        </div>
      </div>

      <!-- MODE 2: FORGOT PASSWORD (STEP WIZARD - FIGMA IMAGE 2) -->
      <div v-else-if="mode === 'forgot'" class="forgot-center-card">
        <BaseLoading 
          :active="loading" 
          :text="loadingText" 
          :sub-text="loadingSubText" 
        />

        <!-- Brand Top -->
        <div class="forgot-header-bar">
          <div class="brand-logo-row">
            <div class="brand-logo-icon mini">
              <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2.2">
                <circle cx="12" cy="12" r="3" fill="currentColor" />
                <path stroke-linecap="round" d="M12 3v3m0 12v3M3 12h3m12 0h3M5.6 5.6l2.1 2.1m8.6 8.6l2.1 2.1M5.6 18.4l2.1-2.1m8.6-8.6l2.1-2.1" />
              </svg>
            </div>
            <span class="brand-name">ERP Cloud</span>
            <span class="enterprise-pill">ENTERPRISE</span>
          </div>

          <div class="lang-pill">
            <span>🌐 VIE</span>
            <span class="chevron">⌄</span>
          </div>
        </div>

        <!-- Inner Card -->
        <div class="forgot-modal-box">
          <!-- 3-Step Wizard Indicator -->
          <div class="wizard-steps-bar">
            <div :class="['wizard-step-node', { active: forgotStep >= 1, current: forgotStep === 1 }]">
              <span class="step-num">1</span>
              <span class="step-txt">Tài khoản</span>
            </div>
            <div class="wizard-step-line" :class="{ filled: forgotStep >= 2 }"></div>
            <div :class="['wizard-step-node', { active: forgotStep >= 2, current: forgotStep === 2 }]">
              <span class="step-num">2</span>
              <span class="step-txt">Mã OTP</span>
            </div>
            <div class="wizard-step-line" :class="{ filled: forgotStep >= 3 }"></div>
            <div :class="['wizard-step-node', { active: forgotStep >= 3, current: forgotStep === 3 }]">
              <span class="step-num">3</span>
              <span class="step-txt">Mật khẩu mới</span>
            </div>
          </div>

          <div class="forgot-title-area">
            <h2 class="forgot-main-title">Khôi phục mật khẩu</h2>
            <p class="forgot-sub-title">Nhập thông tin tài khoản để nhận mã xác thực một lần (OTP)</p>
          </div>

          <!-- STEP 1: Enter Username & Choose Channel -->
          <div v-if="forgotStep === 1" class="step-view">
            <div class="input-block">
              <div class="label-with-link">
                <label class="input-label">Email hoặc tên đăng nhập</label>
                <button type="button" class="btn-sample-fill" @click="forgotForm.identifier = 'admin'">
                  ⚡ Mẫu
                </button>
              </div>
              <div class="input-wrap">
                <span class="input-icon">
                  <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2">
                    <rect x="2" y="4" width="20" height="16" rx="2" />
                    <path d="M22 7l-8.97 5.7a1.94 1.94 0 01-2.06 0L2 7" />
                  </svg>
                </span>
                <input 
                  v-model="forgotForm.identifier" 
                  type="text" 
                  placeholder="admin@enterprisegroup.vn" 
                  class="native-input" 
                  @keyup.enter="handleSendOtp"
                />
              </div>
            </div>

            <!-- Choose Channel Tabs -->
            <div class="input-block">
              <label class="input-label">Nhận mã qua</label>
              <div class="channel-toggle-grid">
                <button 
                  type="button" 
                  class="channel-pill-btn" 
                  :class="{ active: forgotForm.channel === 'EMAIL' }"
                  @click="forgotForm.channel = 'EMAIL'"
                >
                  <span class="channel-icon">✉️</span>
                  <span>Email công ty</span>
                </button>

                <button 
                  type="button" 
                  class="channel-pill-btn" 
                  :class="{ active: forgotForm.channel === 'TELEGRAM' }"
                  @click="forgotForm.channel = 'TELEGRAM'"
                >
                  <span class="channel-icon">✈️</span>
                  <span>Telegram Bot</span>
                </button>
              </div>
            </div>

            <!-- Email Detail if chosen -->
            <div v-if="forgotForm.channel === 'EMAIL'" class="input-block">
              <label class="input-label">Hộp thư nhận OTP (Tùy chọn)</label>
              <input 
                v-model="forgotForm.email" 
                type="email" 
                placeholder="Mặc định: dolemta090899@gmail.com" 
                class="native-input" 
                @keyup.enter="handleSendOtp"
              />
              <span class="channel-hint-text">💡 Để trống hệ thống sẽ tự động gửi về email của tài khoản (dolemta090899@gmail.com).</span>
            </div>

            <!-- Telegram Detail if chosen -->
            <div v-if="forgotForm.channel === 'TELEGRAM'" class="input-block">
              <label class="input-label">Telegram Chat ID (Tùy chọn)</label>
              <input 
                v-model="forgotForm.telegramChatId" 
                type="text" 
                placeholder="Mặc định: 8813999524" 
                class="native-input" 
                @keyup.enter="handleSendOtp"
              />
              <span class="channel-hint-text">💡 Để trống hệ thống sẽ tự động gửi tới Telegram đã cấu hình (@mixi007_bot).</span>
            </div>

            <!-- Captcha for protection -->
            <div class="captcha-box">
              <BaseMathCaptcha 
                ref="forgotCaptchaRef" 
                v-model="isForgotCaptchaPassed"
                label="Giải phép tính để nhận mã OTP"
              />
            </div>

            <!-- Action Button -->
            <button 
              type="button" 
              class="btn-primary-submit" 
              :disabled="loading" 
              @click="handleSendOtp"
            >
              <span>{{ loading ? 'Đang gửi mã...' : 'Gửi mã xác thực' }}</span>
              <span class="btn-arrow">➔</span>
            </button>
          </div>

          <!-- STEP 2: Enter OTP -->
          <div v-else-if="forgotStep === 2" class="step-view">
            <div v-if="serverTargetInfo" class="channel-destination-badge">
              <span>Mã đã được gửi tới: <strong>{{ serverTargetInfo }}</strong></span>
            </div>

            <div 
              v-if="serverGeneratedOtp" 
              class="otp-notice-pill" 
              style="cursor: pointer;" 
              @click="forgotForm.otp = serverGeneratedOtp"
              title="Click để tự động điền mã"
            >
              <span>Mã xác thực xem nhanh: <strong>{{ serverGeneratedOtp }}</strong> <span style="font-size: 11px; opacity: 0.85;">(Click để điền)</span></span>
            </div>

            <div class="input-block">
              <label class="input-label">Nhập mã OTP 6 chữ số</label>
              <input 
                v-model="forgotForm.otp" 
                type="text" 
                maxlength="6" 
                placeholder="123456" 
                class="native-input text-center font-mono font-bold text-lg" 
                @keyup.enter="handleVerifyOtp"
              />
            </div>

            <button 
              type="button" 
              class="btn-primary-submit" 
              :disabled="loading" 
              @click="handleVerifyOtp"
            >
              <span>{{ loading ? 'Đang kiểm tra...' : 'Xác thực OTP' }}</span>
              <span class="btn-arrow">➔</span>
            </button>

            <div class="resend-text-center">
              <a href="javascript:void(0)" class="link-forgot" @click="handleSendOtp">Chưa nhận được mã? Gửi lại</a>
            </div>
          </div>

          <!-- STEP 3: Set New Password -->
          <div v-else-if="forgotStep === 3" class="step-view">
            <div class="input-block">
              <label class="input-label">Mật khẩu mới</label>
              <input 
                v-model="forgotForm.newPassword" 
                type="password" 
                placeholder="Tối thiểu 6 ký tự" 
                class="native-input" 
              />
            </div>

            <div class="input-block">
              <label class="input-label">Xác nhận mật khẩu mới</label>
              <input 
                v-model="forgotForm.confirmPassword" 
                type="password" 
                placeholder="Nhập lại mật khẩu mới" 
                class="native-input" 
                @keyup.enter="handleResetPassword"
              />
            </div>

            <button 
              type="button" 
              class="btn-primary-submit" 
              :disabled="loading" 
              @click="handleResetPassword"
            >
              <span>{{ loading ? 'Đang cập nhật...' : 'Cập nhật mật khẩu' }}</span>
              <span class="btn-arrow">➔</span>
            </button>
          </div>

          <!-- STEP 4: Success -->
          <div v-else-if="forgotStep === 4" class="step-view text-center">
            <div class="success-badge-circle">✓</div>
            <h3 class="success-title">Đổi mật khẩu thành công!</h3>
            <p class="success-desc">Mật khẩu mới của bạn đã được cập nhật an toàn vào hệ thống.</p>
            <button type="button" class="btn-primary-submit" @click="goToLoginAfterReset">
              <span>Đăng nhập ngay</span>
              <span class="btn-arrow">➔</span>
            </button>
          </div>

          <!-- Back to Login Link -->
          <div class="back-login-row">
            <a href="javascript:void(0)" class="link-back" @click="switchMode('login')">
              ← Quay lại đăng nhập
            </a>
          </div>
        </div>

        <div class="forgot-page-footer">
          <span>© 2026 Enterprise Group</span>
          <span class="footer-dot">•</span>
          <span>🔒 Mã hóa TLS 1.3</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../store/auth'
import { ElMessage } from 'element-plus'
import api from '../api'
import BaseMathCaptcha from '../components/base/BaseMathCaptcha.vue'
import BaseLoading from '../components/base/BaseLoading.vue'

const router = useRouter()
const authStore = useAuthStore()

// State
const mode = ref('login') // 'login' | 'forgot'
const loading = ref(false)
const loadingText = ref('Đang xử lý...')
const loadingSubText = ref('Vui lòng đợi giây lát...')
const showPassword = ref(false)
const rememberMe = ref(true)

// Captcha
const isLoginCaptchaPassed = ref(false)
const isForgotCaptchaPassed = ref(false)
const loginCaptchaRef = ref(null)
const forgotCaptchaRef = ref(null)

// Forms
const loginForm = reactive({
  username: '',
  password: ''
})

const forgotStep = ref(1)
const forgotForm = reactive({
  identifier: '',
  channel: 'EMAIL',
  email: '',
  telegramChatId: '',
  otp: '',
  newPassword: '',
  confirmPassword: ''
})

const serverTargetInfo = ref('')
const serverGeneratedOtp = ref('')

function switchMode(newMode) {
  mode.value = newMode
  if (newMode === 'forgot') {
    forgotStep.value = 1
    serverTargetInfo.value = ''
    serverGeneratedOtp.value = ''
    forgotForm.identifier = ''
    forgotForm.channel = 'EMAIL'
    forgotForm.email = ''
    forgotForm.telegramChatId = ''
    forgotForm.otp = ''
    forgotForm.newPassword = ''
    forgotForm.confirmPassword = ''
    if (forgotCaptchaRef.value) forgotCaptchaRef.value.reset()
  } else if (newMode === 'login') {
    if (loginCaptchaRef.value) loginCaptchaRef.value.reset()
  }
}

function fillDemoAccount() {
  loginForm.username = 'admin'
  loginForm.password = 'password'
  if (loginCaptchaRef.value && loginCaptchaRef.value.autoSolveForDemo) {
    loginCaptchaRef.value.autoSolveForDemo()
  } else {
    isLoginCaptchaPassed.value = true
  }
  ElMessage.success({
    message: '⚡ Đã tự động điền tài khoản Quản trị viên (admin / password)!',
    duration: 2500
  })
}

function handleSocialLogin(provider) {
  ElMessage.info({
    message: `Đang kết nối xác thực SSO với ${provider}... (Tính năng Enterprise)`,
    duration: 3000
  })
}

async function handleLogin() {
  if (!loginForm.username || !loginForm.password) {
    ElMessage.warning('Vui lòng nhập đầy đủ tên đăng nhập và mật khẩu.')
    return
  }

  // Chống spam: Bắt buộc giải đúng phép tính bảo vệ
  if (loginCaptchaRef.value && !loginCaptchaRef.value.validate()) {
    ElMessage.warning('Vui lòng giải đúng phép tính để đăng nhập!')
    return
  }

  loading.value = true
  loadingText.value = 'Đang xác thực thông tin...'
  loadingSubText.value = 'Mã hóa token và thiết lập phiên làm việc...'

  try {
    const result = await authStore.login(loginForm.username.trim(), loginForm.password)
    if (result === true || result?.success) {
      ElMessage.success('Đăng nhập thành công! Đang chuyển hướng vào hệ thống...')
      router.push('/')
    } else {
      ElMessage.error(result?.message || 'Tên đăng nhập hoặc mật khẩu không chính xác!')
      if (loginCaptchaRef.value) loginCaptchaRef.value.reset()
    }
  } catch (err) {
    ElMessage.error(err.response?.data?.message || err.message || 'Đăng nhập thất bại. Vui lòng kiểm tra lại.')
    if (loginCaptchaRef.value) loginCaptchaRef.value.reset()
  } finally {
    loading.value = false
  }
}

async function handleSendOtp() {
  if (!forgotForm.identifier || !forgotForm.identifier.trim()) {
    ElMessage.warning('Vui lòng nhập tên đăng nhập hoặc email!')
    return
  }

  // Chống spam: Bắt buộc giải đúng phép toán (chỉ khi ở Step 1)
  if (forgotStep.value === 1 && forgotCaptchaRef.value && !forgotCaptchaRef.value.validate()) {
    ElMessage.warning('Vui lòng giải đúng phép tính để nhận mã OTP!')
    return
  }

  loading.value = true
  loadingText.value = forgotForm.channel === 'EMAIL' 
    ? 'Đang gửi mã OTP qua Email...' 
    : 'Đang gửi mã OTP qua Telegram...'
  loadingSubText.value = 'Hệ thống đang bảo mật mã và chuyển phát tức thì...'

  try {
    const payload = {
      identifier: forgotForm.identifier.trim(),
      channel: forgotForm.channel,
      email: forgotForm.email ? forgotForm.email.trim() : undefined,
      telegramChatId: forgotForm.telegramChatId ? forgotForm.telegramChatId.trim() : undefined
    }

    const res = await api.post('/auth/forgot-password', payload)
    serverGeneratedOtp.value = res.data?.otp || ''
    serverTargetInfo.value = res.data?.target || (forgotForm.channel === 'EMAIL' ? 'Hộp thư Email' : 'Telegram Bot')
    forgotStep.value = 2
    ElMessage.success(res.data?.message || 'Mã xác thực OTP đã được gửi thành công!')
  } catch (err) {
    ElMessage.error(err.response?.data?.message || 'Không tìm thấy tài khoản trong hệ thống!')
    if (forgotStep.value === 1 && forgotCaptchaRef.value) forgotCaptchaRef.value.reset()
  } finally {
    loading.value = false
  }
}

async function handleVerifyOtp() {
  if (!forgotForm.otp || forgotForm.otp.trim().length !== 6) {
    ElMessage.warning('Vui lòng nhập đầy đủ mã OTP 6 chữ số!')
    return
  }

  loading.value = true
  loadingText.value = 'Đang kiểm tra mã OTP...'
  loadingSubText.value = 'Xác minh tính hợp lệ và thời hạn sử dụng...'

  try {
    await api.post('/auth/verify-otp', {
      otp: forgotForm.otp.trim()
    })
    forgotStep.value = 3
    ElMessage.success('Xác thực OTP thành công! Vui lòng tạo mật khẩu mới.')
  } catch (err) {
    ElMessage.error(err.response?.data?.message || 'Mã OTP không chính xác hoặc đã hết hạn!')
  } finally {
    loading.value = false
  }
}

async function handleResetPassword() {
  if (!forgotForm.newPassword || forgotForm.newPassword.length < 6) {
    ElMessage.warning('Mật khẩu mới phải có ít nhất 6 ký tự!')
    return
  }

  if (forgotForm.newPassword !== forgotForm.confirmPassword) {
    ElMessage.warning('Mật khẩu xác nhận không trùng khớp!')
    return
  }

  loading.value = true
  loadingText.value = 'Đang đặt lại mật khẩu mới...'
  loadingSubText.value = 'Mật khẩu đang được mã hóa an toàn vào cơ sở dữ liệu...'

  try {
    await api.post('/auth/reset-password', {
      token: forgotForm.otp.trim(),
      newPassword: forgotForm.newPassword
    })
    forgotStep.value = 4
    ElMessage.success('Đặt lại mật khẩu thành công!')
  } catch (err) {
    ElMessage.error(err.response?.data?.message || 'Lỗi khi đặt lại mật khẩu!')
  } finally {
    loading.value = false
  }
}

function goToLoginAfterReset() {
  loginForm.username = forgotForm.identifier
  loginForm.password = forgotForm.newPassword
  switchMode('login')
}
</script>

<style scoped>
/* ==============================================================================
   FIGMA ENTERPRISE AUTHENTICATION STYLES (MATCHING IMAGES 1 & 2)
   ============================================================================== */
.auth-page {
  min-height: 100vh;
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #f8fafc;
  padding: 32px 16px;
  position: relative;
  overflow-x: hidden;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
  color: #0f172a;
}

.bg-gradient-mesh {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: 
    radial-gradient(at 0% 0%, rgba(219, 234, 254, 0.7) 0px, transparent 50%),
    radial-gradient(at 100% 100%, rgba(224, 242, 254, 0.6) 0px, transparent 50%),
    radial-gradient(at 50% 50%, rgba(241, 245, 249, 0.5) 0px, transparent 50%);
  pointer-events: none;
  z-index: 0;
}

.auth-shell {
  width: 100%;
  max-width: 1100px;
  position: relative;
  z-index: 1;
}

/* ==================== SPLIT CARD (IMAGE 1) ==================== */
.login-split-card {
  display: grid;
  grid-template-columns: 1.05fr 1fr;
  background: #ffffff;
  border-radius: 24px;
  box-shadow: 0 20px 40px -15px rgba(15, 23, 42, 0.08), 0 0 1px 1px rgba(226, 232, 240, 0.8);
  overflow: hidden;
  position: relative;
  min-height: 680px;
}

/* LEFT COLUMN: HERO AREA */
.hero-column {
  background: linear-gradient(145deg, #f0f7ff 0%, #e0effe 100%);
  padding: 44px 40px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  border-right: 1px solid #e2e8f0;
  position: relative;
}

.brand-top {
  display: flex;
  align-items: center;
  gap: 14px;
}

.brand-logo-icon {
  width: 44px;
  height: 44px;
  background: #2563eb;
  color: #ffffff;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.3);
}

.brand-logo-icon.mini {
  width: 34px;
  height: 34px;
  border-radius: 9px;
}

.brand-title-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.brand-name {
  font-size: 19px;
  font-weight: 800;
  color: #0f172a;
  letter-spacing: -0.3px;
}

.enterprise-pill {
  font-size: 10px;
  font-weight: 700;
  background: #dbeafe;
  color: #1d4ed8;
  padding: 2px 7px;
  border-radius: 6px;
  letter-spacing: 0.5px;
}

.brand-sub {
  font-size: 12px;
  color: #64748b;
  display: block;
  margin-top: 2px;
}

.hero-body {
  margin: 36px 0;
}

.system-status-pill {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  background: rgba(255, 255, 255, 0.85);
  border: 1px solid #e2e8f0;
  padding: 5px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
  color: #334155;
  margin-bottom: 22px;
  backdrop-filter: blur(4px);
}

.green-dot {
  width: 8px;
  height: 8px;
  background: #10b981;
  border-radius: 50%;
  box-shadow: 0 0 0 2px rgba(16, 185, 129, 0.2);
}

.hero-heading {
  font-size: 32px;
  line-height: 1.25;
  font-weight: 800;
  color: #0f172a;
  letter-spacing: -0.8px;
  margin: 0 0 16px 0;
}

.highlight-blue {
  color: #2563eb;
}

.hero-desc {
  font-size: 14px;
  line-height: 1.6;
  color: #475569;
  margin: 0 0 28px 0;
  max-width: 440px;
}

/* Floating SLA Card */
.sla-metric-card {
  background: #ffffff;
  border-radius: 16px;
  padding: 18px 20px;
  box-shadow: 0 10px 25px -5px rgba(15, 23, 42, 0.05), 0 0 0 1px rgba(226, 232, 240, 0.8);
}

.sla-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.sla-title-group {
  display: flex;
  align-items: center;
  gap: 12px;
}

.sla-icon {
  width: 32px;
  height: 32px;
  background: #eff6ff;
  color: #2563eb;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.sla-title {
  font-size: 13px;
  font-weight: 700;
  color: #0f172a;
}

.sla-sub {
  font-size: 11px;
  color: #64748b;
}

.online-pill {
  font-size: 11px;
  font-weight: 600;
  color: #059669;
  background: #ecfdf5;
  padding: 3px 10px;
  border-radius: 12px;
}

.sla-divider {
  height: 1px;
  background: #f1f5f9;
  margin: 14px 0;
}

.sla-stats-grid {
  display: flex;
  align-items: center;
  justify-content: space-around;
}

.sla-stat-item {
  text-align: left;
}

.sla-label {
  font-size: 11px;
  color: #64748b;
  margin-bottom: 2px;
}

.sla-value {
  font-size: 20px;
  font-weight: 800;
  color: #0f172a;
  letter-spacing: -0.5px;
}

.sla-sub-badge {
  font-size: 10px;
  font-weight: 600;
  margin-top: 3px;
}

.tier-badge {
  color: #0284c7;
}

.soc-badge {
  color: #4f46e5;
}

.sla-stat-separator {
  width: 1px;
  height: 36px;
  background: #e2e8f0;
}

.hero-footer {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 11px;
  color: #64748b;
  flex-wrap: wrap;
}

.footer-dot {
  color: #cbd5e1;
}

.version-tag {
  margin-left: auto;
  font-family: monospace;
  font-size: 10px;
  color: #94a3b8;
}

/* RIGHT COLUMN: LOGIN FORM */
.form-column {
  padding: 40px 48px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.form-top-bar {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 8px;
}

.lang-pill {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  border: 1px solid #e2e8f0;
  padding: 4px 10px;
  border-radius: 8px;
  font-size: 12px;
  color: #475569;
  cursor: pointer;
  background: #ffffff;
}

.chevron {
  font-size: 10px;
  color: #94a3b8;
}

.form-inner {
  max-width: 420px;
  width: 100%;
  margin: 0 auto;
}

.form-title-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 4px;
}

.form-title {
  font-size: 26px;
  font-weight: 800;
  color: #0f172a;
  letter-spacing: -0.5px;
  margin: 0;
}

.portal-badge {
  font-size: 11px;
  font-weight: 600;
  background: #eff6ff;
  color: #2563eb;
  padding: 2px 8px;
  border-radius: 6px;
}

.form-subtitle {
  font-size: 13px;
  color: #64748b;
  margin: 0 0 20px 0;
}

/* Demo Credential Card */
.demo-card-banner {
  background: #eff6ff;
  border: 1px solid #bfdbfe;
  border-radius: 12px;
  padding: 10px 14px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 22px;
  cursor: pointer;
  transition: all 0.2s;
}

.demo-card-banner:hover {
  background: #e0f2fe;
  border-color: #93c5fd;
  transform: translateY(-1px);
}

.demo-card-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.demo-icon-box {
  width: 32px;
  height: 32px;
  background: #2563eb;
  color: #ffffff;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.demo-title {
  font-size: 12px;
  font-weight: 700;
  color: #1e3a8a;
}

.demo-sub {
  font-size: 11px;
  color: #3b82f6;
  font-family: monospace;
}

.btn-demo-autofill {
  background: #ffffff;
  border: 1px solid #bfdbfe;
  color: #1d4ed8;
  font-size: 11px;
  font-weight: 600;
  padding: 5px 10px;
  border-radius: 8px;
  cursor: pointer;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
}

/* Form Fields */
.auth-form-fields {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.input-block {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.label-with-link {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.input-label {
  font-size: 13px;
  font-weight: 600;
  color: #334155;
}

.link-forgot {
  font-size: 12px;
  color: #2563eb;
  text-decoration: none;
  font-weight: 600;
}

.link-forgot:hover {
  text-decoration: underline;
}

.input-wrap {
  position: relative;
  display: flex;
  align-items: center;
}

.input-icon {
  position: absolute;
  left: 14px;
  color: #94a3b8;
  display: flex;
  align-items: center;
  pointer-events: none;
}

.native-input {
  width: 100%;
  padding: 12px 14px 12px 42px;
  border-radius: 12px;
  border: 1px solid #cbd5e1;
  background: #ffffff;
  font-size: 14px;
  color: #0f172a;
  outline: none;
  transition: all 0.2s;
  box-sizing: border-box;
}

.native-input:focus {
  border-color: #2563eb;
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.12);
}

.eye-toggle {
  position: absolute;
  right: 12px;
  background: none;
  border: none;
  color: #94a3b8;
  cursor: pointer;
  padding: 4px;
  display: flex;
  align-items: center;
}

.eye-toggle:hover {
  color: #475569;
}

.options-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: -2px;
}

.checkbox-label {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #475569;
  cursor: pointer;
}

.custom-chk {
  width: 16px;
  height: 16px;
  accent-color: #2563eb;
  cursor: pointer;
}

.passkey-pill {
  font-size: 11px;
  font-weight: 600;
  color: #64748b;
  background: #f1f5f9;
  padding: 3px 8px;
  border-radius: 6px;
}

.captcha-box {
  margin-top: 2px;
}

.btn-primary-submit {
  width: 100%;
  background: #2563eb;
  color: #ffffff;
  border: none;
  padding: 13px 20px;
  border-radius: 12px;
  font-size: 15px;
  font-weight: 700;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  box-shadow: 0 8px 18px rgba(37, 99, 235, 0.25);
  transition: all 0.2s;
}

.btn-primary-submit:hover:not(:disabled) {
  background: #1d4ed8;
  transform: translateY(-1px);
  box-shadow: 0 10px 22px rgba(37, 99, 235, 0.3);
}

.btn-primary-submit:disabled {
  opacity: 0.65;
  cursor: not-allowed;
}

.btn-arrow {
  font-size: 16px;
  transition: transform 0.2s;
}

.btn-primary-submit:hover .btn-arrow {
  transform: translateX(3px);
}

.divider-text {
  display: flex;
  align-items: center;
  text-align: center;
  margin: 18px 0 14px 0;
  color: #94a3b8;
  font-size: 11px;
  font-weight: 600;
}

.divider-text::before,
.divider-text::after {
  content: '';
  flex: 1;
  border-bottom: 1px solid #e2e8f0;
}

.divider-text span {
  padding: 0 12px;
}

.social-btn-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.btn-social {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 10px 14px;
  background: #ffffff;
  border: 1px solid #cbd5e1;
  border-radius: 10px;
  font-size: 13px;
  font-weight: 600;
  color: #334155;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-social:hover {
  background: #f8fafc;
  border-color: #94a3b8;
}

.form-bottom-legal {
  margin-top: 28px;
  text-align: center;
}

.legal-links {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  font-size: 12px;
}

.legal-links a {
  color: #64748b;
  text-decoration: none;
}

.legal-links a:hover {
  color: #2563eb;
}

.copyright-text {
  font-size: 11px;
  color: #94a3b8;
  margin-top: 6px;
}

/* ==================== FORGOT PASSWORD (IMAGE 2) ==================== */
.forgot-center-card {
  max-width: 540px;
  margin: 0 auto;
}

.forgot-header-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 18px;
}

.brand-logo-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.forgot-modal-box {
  background: #ffffff;
  border-radius: 24px;
  padding: 36px 36px 30px 36px;
  box-shadow: 0 20px 40px -15px rgba(15, 23, 42, 0.08), 0 0 1px 1px rgba(226, 232, 240, 0.8);
}

.wizard-steps-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 28px;
}

.wizard-step-node {
  display: flex;
  align-items: center;
  gap: 8px;
}

.step-num {
  width: 26px;
  height: 26px;
  border-radius: 50%;
  background: #f1f5f9;
  color: #64748b;
  font-size: 12px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
}

.step-txt {
  font-size: 13px;
  font-weight: 600;
  color: #64748b;
}

.wizard-step-node.active .step-num {
  background: #2563eb;
  color: #ffffff;
}

.wizard-step-node.active .step-txt {
  color: #0f172a;
}

.wizard-step-line {
  flex: 1;
  height: 2px;
  background: #e2e8f0;
  margin: 0 10px;
}

.wizard-step-line.filled {
  background: #2563eb;
}

.forgot-title-area {
  margin-bottom: 22px;
}

.forgot-main-title {
  font-size: 22px;
  font-weight: 800;
  color: #0f172a;
  margin: 0 0 6px 0;
}

.forgot-sub-title {
  font-size: 13px;
  color: #64748b;
  margin: 0;
}

.step-view {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.btn-sample-fill {
  background: none;
  border: none;
  color: #2563eb;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
}

.channel-toggle-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.channel-pill-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 12px 14px;
  background: #ffffff;
  border: 1px solid #cbd5e1;
  border-radius: 12px;
  font-size: 13px;
  font-weight: 600;
  color: #334155;
  cursor: pointer;
  transition: all 0.2s;
}

.channel-pill-btn.active {
  background: #eff6ff;
  border-color: #2563eb;
  color: #1d4ed8;
}

.channel-hint-text {
  font-size: 11.5px;
  color: #64748b;
  margin-top: 4px;
  line-height: 1.4;
}

.channel-destination-badge {
  background: #f0fdf4;
  border: 1px solid #bbf7d0;
  color: #15803d;
  padding: 10px 14px;
  border-radius: 10px;
  font-size: 12px;
}

.otp-notice-pill {
  background: #eff6ff;
  border: 1px solid #bfdbfe;
  color: #1d4ed8;
  padding: 10px 14px;
  border-radius: 10px;
  font-size: 13px;
}

.resend-text-center {
  text-align: center;
  margin-top: 4px;
}

.back-login-row {
  margin-top: 24px;
  text-align: center;
}

.link-back {
  font-size: 13px;
  font-weight: 600;
  color: #475569;
  text-decoration: none;
}

.link-back:hover {
  color: #2563eb;
}

.forgot-page-footer {
  margin-top: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  font-size: 11px;
  color: #94a3b8;
}

.success-badge-circle {
  width: 52px;
  height: 52px;
  background: #10b981;
  color: #ffffff;
  border-radius: 50%;
  font-size: 26px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 12px auto;
}

.success-title {
  font-size: 20px;
  font-weight: 800;
  color: #0f172a;
  margin: 0 0 6px 0;
}

.success-desc {
  font-size: 13px;
  color: #64748b;
  margin: 0 0 20px 0;
}

/* Responsive adjustments */
@media (max-width: 900px) {
  .login-split-card {
    grid-template-columns: 1fr;
  }
  .hero-column {
    padding: 32px 24px;
  }
  .form-column {
    padding: 32px 24px;
  }
}
</style>
