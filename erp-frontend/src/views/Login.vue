<template>
  <div
    class="login-viewport"
    :class="currentTheme"
    @mousemove="handleViewportMouseMove"
    @mouseleave="handleViewportMouseLeave"
  >
    <!-- 1. FULLSCREEN INTERACTIVE LIGHT CANVAS & AURA SPOTLIGHT -->
    <canvas ref="canvasRef" class="interactive-particle-canvas"></canvas>

    <!-- Confetti Canvas cho hiệu ứng ăn mừng khi đăng nhập thành công -->
    <canvas ref="confettiCanvasRef" class="confetti-canvas"></canvas>

    <!-- Canvas Micro Sparkles khi giải đúng Captcha -->
    <canvas ref="sparkleCanvasRef" class="sparkle-canvas"></canvas>

    <!-- Con trỏ hào quang ánh sáng mềm mại theo chuột (JS Ambient Glow Spotlight) -->
    <div
      class="mouse-ambient-glow"
      :style="{
        left: mouseGlowPos.x + 'px',
        top: mouseGlowPos.y + 'px',
        opacity: isMouseInside ? 1 : 0,
        transform: `translate(-50%, -50%) scale(${isHoveringInteractive ? 1.6 : 1})`
      }"
    ></div>

    <!-- Background Aurora Light Blobs chuyển động hữu cơ theo chuột -->
    <div class="aurora-blob blob-1" :style="blob1Style"></div>
    <div class="aurora-blob blob-2" :style="blob2Style"></div>
    <div class="aurora-blob blob-3" :style="blob3Style"></div>

    <!-- Thanh tiện ích trên cao (Quick Controls: Âm thanh Web Audio & Đổi Gam màu sáng) -->
    <div class="top-floating-toolbar">
      <!-- Bộ đổi gam màu ánh sáng tương tác (Interactive Theme Aura Switcher) -->
      <div class="theme-aura-selector">
        <span class="toolbar-hint-txt">Hào quang:</span>
        <button
          type="button"
          class="theme-color-dot sapphire"
          :class="{ active: currentTheme === 'theme-sapphire' }"
          title="Sapphire Royal (Xanh Hoàng Gia)"
          @click="changeTheme('theme-sapphire')"
        ></button>
        <button
          type="button"
          class="theme-color-dot emerald"
          :class="{ active: currentTheme === 'theme-emerald' }"
          title="Emerald Mint (Xanh Ngọc Hiện Đại)"
          @click="changeTheme('theme-emerald')"
        ></button>
        <button
          type="button"
          class="theme-color-dot amethyst"
          :class="{ active: currentTheme === 'theme-amethyst' }"
          title="Amethyst Purple (Tím Thạch Anh Sang Trọng)"
          @click="changeTheme('theme-amethyst')"
        ></button>
      </div>

      <!-- Nút Bật / Tắt Âm thanh Web Audio Synthesizer -->
      <button
        type="button"
        class="sound-toggle-btn"
        :class="{ enabled: soundEnabled }"
        :title="soundEnabled ? 'Tắt âm thanh tương tác' : 'Bật âm thanh tương tác (Web Audio Synth)'"
        @click="toggleSound"
      >
        <span class="sound-icon">{{ soundEnabled ? '🔊' : '🔇' }}</span>
        <span class="sound-label">{{ soundEnabled ? 'Âm thanh: BẬT' : 'Âm thanh: TẮT' }}</span>
      </button>
    </div>

    <!-- 2. MAIN SPLIT DESKTOP STAGE (LIGHT THEME LUXURY VỪA VẶN MÀN HÌNH PC) -->
    <div
      ref="stageContainerRef"
      class="login-stage-container"
      :style="stageTiltStyle"
    >
      <!-- CỘT TRÁI: ENTERPRISE SHOWCASE (LIGHT TECH PORTAL NỀN SÁNG CAO CẤP) -->
      <div class="hero-showcase-panel">
        <!-- Logo & Branding -->
        <div class="brand-top-bar">
          <div class="brand-emblem-badge">
            <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="2.2">
              <path d="M12 2L2 7l10 5 10-5-10-5zM2 17l10 5 10-5M2 12l10 5 10-5" />
            </svg>
          </div>
          <div>
            <div class="brand-name">ERP CLOUD</div>
            <div class="brand-tagline">ENTERPRISE SUITE • CORE v4.12</div>
          </div>
        </div>

        <!-- Hero Content with JS Typewriter -->
        <div class="hero-content-body">
          <div class="pill-kicker">
            <span class="live-dot"></span>
            <span>NỀN TẢNG ĐIỀU HÀNH THỜI GIAN THỰC</span>
          </div>

          <h1 class="hero-title">
            Quản trị Vận hành &amp;<br />
            <span class="gradient-text">{{ dynamicSlogan }}</span>
            <span class="typewriter-cursor">|</span>
          </h1>

          <p class="hero-description">
            Kiến trúc đa luồng chịu tải hàng triệu bản ghi. Tích hợp quản lý kho ACID, báo cáo BI đa chiều &amp; kiểm soát dòng tiền tự động.
          </p>

          <!-- 3D Tilt Showcase Card với Specular Glare -->
          <div
            ref="tiltCardRef"
            class="interactive-tilt-card"
            :style="tiltStyle"
            @mouseenter="isHoveringTilt = true"
            @mouseleave="onTiltLeave"
          >
            <!-- Lớp phản quang ánh sáng chạy theo chuột -->
            <div class="tilt-specular-glare" :style="glareStyle"></div>

            <div class="tilt-card-header">
              <div class="tilt-status-chip">
                <span class="pulse-chip-dot"></span>
                <span>HỆ THỐNG TRỰC TUYẾN</span>
              </div>
              <span class="tilt-time">{{ currentTimeStr }}</span>
            </div>

            <!-- Chỉ số thời gian thực với bộ nhảy số giao dịch liên tục -->
            <div class="tilt-card-metrics">
              <div class="metric-mini-item">
                <div class="m-val">{{ counterUptime.toFixed(2) }}%</div>
                <div class="m-lbl">SLA Khả dụng</div>
              </div>
              <div class="m-sep"></div>
              <div class="metric-mini-item">
                <div class="m-val">&lt; {{ Math.round(counterLatency) }}ms</div>
                <div class="m-lbl">Độ trễ API</div>
              </div>
              <div class="m-sep"></div>
              <div class="metric-mini-item">
                <div class="m-val highlight-blue">{{ formatNumber(liveTpsCounter) }}+</div>
                <div class="m-lbl">Giao dịch / giây</div>
              </div>
            </div>

            <!-- Thanh Scanner Laser mô phỏng an ninh TLS 1.3 -->
            <div class="security-scanner-bar">
              <div class="scanner-laser-line"></div>
              <div class="scanner-text-row">
                <span class="cert-shield">🛡️</span>
                <span>Mã hóa AES-256 GCM • Phiên bảo mật cấp doanh nghiệp</span>
              </div>
            </div>
          </div>
        </div>

        <!-- Live Enterprise Security Audit Stream Ticker (JS Realtime Events) -->
        <div class="security-audit-ticker">
          <div class="ticker-label">
            <span class="ticker-radar-dot"></span>
            <span>AUDIT REALTIME</span>
          </div>
          <div class="ticker-content" :key="currentAuditIndex">
            <span class="ticker-text">{{ currentAuditMessage }}</span>
          </div>
        </div>

        <!-- Hero Footer -->
        <div class="hero-footer-bar">
          <div class="cluster-badge">
            <span class="region-flag">🇻🇳</span>
            <span>Cụm máy chủ Đông Nam Á (VN-Central)</span>
          </div>
          <span class="copyright-txt">© 2026 ERP Cloud Systems Inc.</span>
        </div>
      </div>

      <!-- CỘT PHẢI: AUTH FORM PANEL (LIGHT NỀN TRẮNG TINH KHIẾT) -->
      <div class="auth-card-panel">
        <!-- MODE 1: FORM ĐĂNG NHẬP -->
        <div v-if="mode === 'login'" class="form-wrapper">
          <!-- Mascot Cyber Sentry theo dõi chuột & che mắt khi nhập mật khẩu -->
          <div class="mascot-sentry-wrap" title="Cyber Sentry: Trợ lý bảo mật ERP">
            <div class="sentry-head" :class="{ 'is-sleeping': isPasswordFocused && !showPassword, 'is-peeking': isPasswordFocused && showPassword }">
              <div class="sentry-antenna">
                <span class="antenna-dot"></span>
              </div>
              <div class="sentry-face">
                <!-- Mắt trái -->
                <div class="sentry-eye left-eye">
                  <div
                    class="sentry-pupil"
                    :style="leftPupilStyle"
                  ></div>
                  <div class="sentry-eyelid"></div>
                </div>
                <!-- Mắt phải -->
                <div class="sentry-eye right-eye">
                  <div
                    class="sentry-pupil"
                    :style="rightPupilStyle"
                  ></div>
                  <div class="sentry-eyelid"></div>
                </div>
              </div>
              <!-- Tay che mắt (Hands Cover) khi đang nhập password -->
              <div class="sentry-hands" :class="{ visible: isPasswordFocused && !showPassword }">
                <div class="hand hand-left">🙈</div>
                <div class="hand hand-right">🙈</div>
              </div>
            </div>
            <div class="sentry-speech-bubble">
              <span>{{ mascotGreetingText }}</span>
            </div>
          </div>

          <div class="form-header">
            <div class="welcome-row">
              <h2 class="form-title">Đăng nhập</h2>
              <span class="auth-pill-secure">SSL 256-bit</span>
            </div>
            <p class="form-sub">Nhập thông tin ủy nhiệm để truy cập sổ cái điều hành</p>
          </div>

          <!-- Quick Fill Sample Accounts (Thao tác nhanh 1 chạm với hiệu ứng Magnetic) -->
          <div class="quick-accounts-toolbar">
            <span class="quick-title">⚡ Điền nhanh tài khoản mẫu:</span>
            <div class="quick-btns-row">
              <button
                type="button"
                class="btn-quick-fill admin magnetic-target"
                @click="fillCredentials('admin', '123456')"
                @mouseenter="onHoverInteractive"
                @mouseleave="onLeaveInteractive"
              >
                Quản trị viên (admin)
              </button>
              <button
                type="button"
                class="btn-quick-fill user magnetic-target"
                @click="fillCredentials('nhanvien', '123456')"
                @mouseenter="onHoverInteractive"
                @mouseleave="onLeaveInteractive"
              >
                Nhân viên (nhanvien)
              </button>
            </div>
          </div>

          <!-- Biểu mẫu đăng nhập -->
          <form class="auth-form" @submit.prevent="handleLogin">
            <div class="form-field-group">
              <label class="field-label">Tên người dùng / Tài khoản</label>
              <div class="input-container" :class="{ 'has-value': loginForm.username }">
                <span class="field-icon">👤</span>
                <input
                  v-model.trim="loginForm.username"
                  type="text"
                  class="field-input"
                  placeholder="Nhập username (vd: admin)..."
                  required
                  autocomplete="username"
                  @focus="onUsernameFocus"
                  @blur="onUsernameBlur"
                  @input="playKeySound"
                />
              </div>
            </div>

            <div class="form-field-group">
              <div class="pwd-label-row">
                <label class="field-label">Mật khẩu</label>
                <button
                  type="button"
                  class="link-forgot-btn"
                  @click="switchMode('forgot')"
                >
                  Quên mật khẩu?
                </button>
              </div>
              <div class="input-container" :class="{ 'has-value': loginForm.password }">
                <span class="field-icon">🔒</span>
                <input
                  v-model="loginForm.password"
                  :type="showPassword ? 'text' : 'password'"
                  class="field-input"
                  placeholder="Nhập mật khẩu..."
                  required
                  autocomplete="current-password"
                  @focus="onPasswordFocus"
                  @blur="onPasswordBlur"
                  @input="handlePasswordInput"
                />
                <button
                  type="button"
                  class="btn-toggle-eye"
                  :title="showPassword ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'"
                  @click="togglePasswordVisibility"
                >
                  {{ showPassword ? '🙈' : '👁️' }}
                </button>
              </div>

              <!-- Thanh đo độ mạnh mật khẩu động bằng JS -->
              <div v-if="loginForm.password" class="password-strength-container">
                <div class="strength-bar-track">
                  <div
                    class="strength-bar-fill"
                    :style="{ width: pwdStrengthPercent + '%', backgroundColor: pwdStrengthColor }"
                  ></div>
                </div>
                <span class="strength-label-text" :style="{ color: pwdStrengthColor }">
                  {{ pwdStrengthLabel }}
                </span>
              </div>
            </div>

            <!-- Math Captcha Component với Sparkle Trigger -->
            <div ref="captchaSlotRef" class="captcha-form-slot">
              <BaseMathCaptcha
                ref="loginCaptchaRef"
                v-model="isLoginCaptchaPassed"
                label="Bảo mật chống bot"
                :compact="true"
                @verified="onCaptchaVerified"
              />
            </div>

            <div class="form-options-row">
              <label class="checkbox-label">
                <input v-model="rememberMe" type="checkbox" class="custom-checkbox" @change="playTickSound" />
                <span>Ghi nhớ phiên đăng nhập (30 ngày)</span>
              </label>
            </div>

            <!-- Submit Button với hiệu ứng Magnetic, Ripple & Sound Synth -->
            <button
              ref="submitBtnRef"
              type="submit"
              class="btn-submit-primary"
              :style="submitBtnMagneticStyle"
              :disabled="loading"
              @mousemove="handleButtonMagneticMove"
              @mouseleave="handleButtonMagneticLeave"
              @mouseenter="onHoverInteractive"
              @click="onSubmitClick"
            >
              <span v-if="loading" class="spinner-dot"></span>
              <span>{{ loading ? 'ĐANG XÁC THỰC HỆ THỐNG...' : 'ĐĂNG NHẬP HỆ THỐNG' }}</span>
              <span class="btn-arrow">➔</span>
            </button>
          </form>

          <div class="form-footer-line">
            <span>Chưa có tài khoản doanh nghiệp?</span>
            <span class="admin-contact-hint">Liên hệ Quản trị viên cấp quyền</span>
          </div>
        </div>

        <!-- MODE 2: QUÊN MẬT KHẨU WIZARD -->
        <div v-else class="form-wrapper forgot-wrapper">
          <div class="form-header">
            <div class="welcome-row">
              <h2 class="form-title">Khôi phục Mật khẩu</h2>
              <span class="auth-pill-secure">OTP Recovery</span>
            </div>
            <p class="form-sub">Nhận mã xác thực qua Email đã đăng ký để đặt lại mật khẩu</p>
          </div>

          <!-- Bước 1: Nhập email/username -->
          <div v-if="forgotStep === 1" class="wizard-step">
            <div class="form-field-group">
              <label class="field-label">Tài khoản hoặc Email doanh nghiệp</label>
              <div class="input-container">
                <span class="field-icon">📧</span>
                <input
                  v-model.trim="forgotForm.emailOrUser"
                  type="text"
                  class="field-input"
                  placeholder="Nhập username hoặc email..."
                  required
                />
              </div>
            </div>

            <div class="captcha-form-slot">
              <BaseMathCaptcha
                ref="forgotCaptchaRef"
                v-model="isForgotCaptchaPassed"
                label="Bảo mật gửi OTP"
                :compact="true"
              />
            </div>

            <div class="wizard-btn-actions">
              <button
                type="button"
                class="btn-submit-primary"
                :disabled="forgotLoading || !forgotForm.emailOrUser || !isForgotCaptchaPassed"
                @click="handleSendOtp"
              >
                <span>{{ forgotLoading ? 'ĐANG GỬI MÃ...' : 'TIẾP TỤC: GỬI MÃ OTP' }}</span>
                <span class="btn-arrow">➔</span>
              </button>
              <button
                type="button"
                class="btn-cancel-link"
                @click="switchMode('login')"
              >
                ← Quay lại Đăng nhập
              </button>
            </div>
          </div>

          <!-- Bước 2: Nhập OTP -->
          <div v-else-if="forgotStep === 2" class="wizard-step">
            <div class="otp-notice-box">
              <span>Mã OTP giả lập hệ thống:</span>
              <span class="demo-otp-pill" @click="fillPreviewOtp('888666')">888666 (Click điền)</span>
            </div>

            <div class="form-field-group">
              <label class="field-label">Mã xác thực 6 số</label>
              <div class="input-container">
                <span class="field-icon">🔑</span>
                <input
                  v-model.trim="forgotForm.otp"
                  type="text"
                  maxlength="6"
                  class="field-input otp-input"
                  placeholder="Nhập 6 số..."
                />
              </div>
            </div>

            <div class="wizard-btn-actions">
              <button
                type="button"
                class="btn-submit-primary"
                :disabled="forgotForm.otp.length !== 6"
                @click="handleVerifyOtp"
              >
                <span>XÁC NHẬN MÃ OTP</span>
                <span class="btn-arrow">➔</span>
              </button>
              <button
                type="button"
                class="btn-cancel-link"
                @click="forgotStep = 1"
              >
                ← Nhập lại email
              </button>
            </div>
          </div>

          <!-- Bước 3: Đặt lại mật khẩu -->
          <div v-else-if="forgotStep === 3" class="wizard-step">
            <div class="form-field-group">
              <label class="field-label">Mật khẩu mới</label>
              <div class="input-container">
                <span class="field-icon">🔒</span>
                <input
                  v-model="forgotForm.newPassword"
                  type="password"
                  class="field-input"
                  placeholder="Nhập mật khẩu mới..."
                />
              </div>
            </div>

            <div class="form-field-group">
              <label class="field-label">Xác nhận mật khẩu</label>
              <div class="input-container">
                <span class="field-icon">🔒</span>
                <input
                  v-model="forgotForm.confirmPassword"
                  type="password"
                  class="field-input"
                  placeholder="Nhập lại mật khẩu..."
                />
              </div>
            </div>

            <div class="wizard-btn-actions">
              <button
                type="button"
                class="btn-submit-primary"
                :disabled="!forgotForm.newPassword || forgotForm.newPassword !== forgotForm.confirmPassword"
                @click="handleResetPassword"
              >
                <span>ĐẶT LẠI MẬT KHẨU</span>
                <span class="btn-arrow">➔</span>
              </button>
            </div>
          </div>

          <!-- Bước 4: Hoàn tất -->
          <div v-else class="wizard-step text-center">
            <div class="success-icon-seal">✓</div>
            <h3 class="success-step-title">Khôi phục thành công!</h3>
            <p class="success-step-desc">Mật khẩu mới đã được cập nhật an toàn vào hệ thống.</p>
            <button
              type="button"
              class="btn-submit-primary"
              @click="switchMode('login')"
            >
              <span>ĐĂNG NHẬP NGAY</span>
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
// =========================================================================
// 0. IMPORTS & HOOKS
// =========================================================================
import { ref, reactive, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../store/auth'
import { ElMessage } from 'element-plus'
import BaseMathCaptcha from '../components/base/BaseMathCaptcha.vue'

const router = useRouter()
const authStore = useAuthStore()

// =========================================================================
// 1. HELPER FUNCTIONS (ĐẶT LÊN ĐẦU TRÁNH TEMPORAL DEAD ZONE)
// =========================================================================
function formatNumber(val) {
  if (val == null) return '0'
  return val.toString().replace(/\B(?=(\d{3})+(?!\d))/g, '.')
}

// =========================================================================
// 2. STATE KHAI BÁO & FORMS
// =========================================================================
const mode = ref('login')
const loading = ref(false)
const showPassword = ref(false)
const rememberMe = ref(true)
const isPasswordFocused = ref(false)
const isUsernameFocused = ref(false)

const loginForm = reactive({
  username: '',
  password: ''
})

const isLoginCaptchaPassed = ref(false)
const loginCaptchaRef = ref(null)
const captchaSlotRef = ref(null)

const forgotStep = ref(1)
const forgotLoading = ref(false)
const isForgotCaptchaPassed = ref(false)
const forgotCaptchaRef = ref(null)
const forgotForm = reactive({
  emailOrUser: '',
  otp: '',
  newPassword: '',
  confirmPassword: ''
})

// Password Strength State
const pwdStrengthPercent = ref(0)
const pwdStrengthLabel = ref('')
const pwdStrengthColor = ref('#cbd5e1')

function calcPasswordStrength() {
  const pwd = loginForm.password
  if (!pwd) {
    pwdStrengthPercent.value = 0
    pwdStrengthLabel.value = ''
    pwdStrengthColor.value = '#cbd5e1'
    return
  }

  let score = 0
  if (pwd.length >= 6) score += 25
  if (pwd.length >= 10) score += 15
  if (/[A-Z]/.test(pwd)) score += 20
  if (/[0-9]/.test(pwd)) score += 20
  if (/[^A-Za-z0-9]/.test(pwd)) score += 20

  if (score < 40) {
    pwdStrengthPercent.value = 30
    pwdStrengthLabel.value = 'Mật khẩu yếu'
    pwdStrengthColor.value = '#ef4444'
  } else if (score < 75) {
    pwdStrengthPercent.value = 65
    pwdStrengthLabel.value = 'Mật khẩu trung bình'
    pwdStrengthColor.value = '#f59e0b'
  } else {
    pwdStrengthPercent.value = 100
    pwdStrengthLabel.value = 'Mật khẩu tối ưu (Chuẩn Doanh nghiệp)'
    pwdStrengthColor.value = '#10b981'
  }
}

// =========================================================================
// 3. THEME AURA SWITCHER (ĐỔI GAM MÀU ÁNH SÁNG TƯƠNG TÁC BẰNG JS)
// =========================================================================
const currentTheme = ref('theme-sapphire')

function changeTheme(themeName) {
  currentTheme.value = themeName
  playPopSound(540)
  ElMessage.info({
    message: `Đã kích hoạt không gian ánh sáng ${
      themeName === 'theme-sapphire' ? 'Sapphire Royal' : themeName === 'theme-emerald' ? 'Emerald Mint' : 'Amethyst Royal'
    }`,
    duration: 1500
  })
}

// =========================================================================
// 4. WEB AUDIO SYNTHESIZER (HIỆU ỨNG ÂM THANH JS KHÔNG CẦN FILE MP3)
// =========================================================================
const soundEnabled = ref(true)
let audioCtx = null

function getAudioContext() {
  if (!audioCtx && typeof window !== 'undefined') {
    const AudioContextClass = window.AudioContext || window.webkitAudioContext
    if (AudioContextClass) {
      audioCtx = new AudioContextClass()
    }
  }
  if (audioCtx && audioCtx.state === 'suspended') {
    audioCtx.resume()
  }
  return audioCtx
}

function toggleSound() {
  soundEnabled.value = !soundEnabled.value
  if (soundEnabled.value) {
    playChimeSound()
  }
}

function playTone(freq = 440, type = 'sine', duration = 0.08, vol = 0.05) {
  if (!soundEnabled.value) return
  try {
    const ctx = getAudioContext()
    if (!ctx) return
    const osc = ctx.createOscillator()
    const gain = ctx.createGain()

    osc.type = type
    osc.frequency.setValueAtTime(freq, ctx.currentTime)

    gain.gain.setValueAtTime(vol, ctx.currentTime)
    gain.gain.exponentialRampToValueAtTime(0.0001, ctx.currentTime + duration)

    osc.connect(gain)
    gain.connect(ctx.destination)

    osc.start()
    osc.stop(ctx.currentTime + duration)
  } catch {
    // Ignore audio autoplay restrictions
  }
}

function playTickSound() {
  playTone(850, 'triangle', 0.04, 0.03)
}

function playKeySound() {
  playTone(600 + Math.random() * 200, 'sine', 0.03, 0.015)
}

function playPopSound(baseFreq = 380) {
  playTone(baseFreq, 'sine', 0.09, 0.06)
}

function playChimeSound() {
  playTone(523.25, 'sine', 0.15, 0.05) // C5
  setTimeout(() => playTone(659.25, 'sine', 0.15, 0.05), 60) // E5
  setTimeout(() => playTone(783.99, 'sine', 0.25, 0.06), 120) // G5
}

function playSuccessFanfare() {
  playTone(523.25, 'sine', 0.12, 0.06)
  setTimeout(() => playTone(659.25, 'sine', 0.12, 0.06), 80)
  setTimeout(() => playTone(783.99, 'sine', 0.15, 0.07), 160)
  setTimeout(() => playTone(1046.50, 'triangle', 0.4, 0.08), 240) // C6
}

// =========================================================================
// 5. MASCOT CYBER SENTRY EYE TRACKING & GREETING
// =========================================================================
const mousePos = reactive({ x: 0, y: 0 })
const mouseGlowPos = reactive({ x: window.innerWidth / 2, y: window.innerHeight / 2 })
const isMouseInside = ref(true)
const isHoveringInteractive = ref(false)

const leftPupilPos = reactive({ x: 0, y: 0 })
const rightPupilPos = reactive({ x: 0, y: 0 })

const leftPupilStyle = computed(() => {
  if (isPasswordFocused.value && !showPassword.value) {
    return { transform: 'scaleY(0.1)' }
  }
  return { transform: `translate(${leftPupilPos.x}px, ${leftPupilPos.y}px)` }
})

const rightPupilStyle = computed(() => {
  if (isPasswordFocused.value && !showPassword.value) {
    return { transform: 'scaleY(0.1)' }
  }
  return { transform: `translate(${rightPupilPos.x}px, ${rightPupilPos.y}px)` }
})

const mascotGreetingText = computed(() => {
  if (loading.value) return 'Đang kết nối xác thực...'
  if (isPasswordFocused.value && !showPassword.value) return 'Mascot đã nhắm mắt bảo mật!'
  if (isPasswordFocused.value && showPassword.value) return 'Đang mở hé một mắt nhìn lén...'
  if (isUsernameFocused.value) return 'Nhập tên tài khoản của bạn nhé!'
  if (isLoginCaptchaPassed.value) return 'Đã vượt qua kiểm tra chống bot! ✓'
  return 'Sẵn sàng điều hành hệ thống!'
})

function updateMascotEyes(e) {
  const maxRadius = 4.5
  // Left eye center calculation
  const leftEyeElem = document.querySelector('.sentry-eye.left-eye')
  const rightEyeElem = document.querySelector('.sentry-eye.right-eye')

  if (leftEyeElem) {
    const rect = leftEyeElem.getBoundingClientRect()
    const eyeCenterX = rect.left + rect.width / 2
    const eyeCenterY = rect.top + rect.height / 2
    const angle = Math.atan2(e.clientY - eyeCenterY, e.clientX - eyeCenterX)
    const dist = Math.min(maxRadius, Math.hypot(e.clientX - eyeCenterX, e.clientY - eyeCenterY) * 0.08)
    leftPupilPos.x = Math.cos(angle) * dist
    leftPupilPos.y = Math.sin(angle) * dist
  }

  if (rightEyeElem) {
    const rect = rightEyeElem.getBoundingClientRect()
    const eyeCenterX = rect.left + rect.width / 2
    const eyeCenterY = rect.top + rect.height / 2
    const angle = Math.atan2(e.clientY - eyeCenterY, e.clientX - eyeCenterX)
    const dist = Math.min(maxRadius, Math.hypot(e.clientX - eyeCenterX, e.clientY - eyeCenterY) * 0.08)
    rightPupilPos.x = Math.cos(angle) * dist
    rightPupilPos.y = Math.sin(angle) * dist
  }
}

function onUsernameFocus() {
  isUsernameFocused.value = true
  playTickSound()
}

function onUsernameBlur() {
  isUsernameFocused.value = false
}

function onPasswordFocus() {
  isPasswordFocused.value = true
  playTickSound()
}

function onPasswordBlur() {
  isPasswordFocused.value = false
}

function togglePasswordVisibility() {
  showPassword.value = !showPassword.value
  playPopSound(420)
}

function handlePasswordInput() {
  playKeySound()
  calcPasswordStrength()
}

// =========================================================================
// 6. MAGNETIC BUTTON ATTRACTION EFFECT
// =========================================================================
const submitBtnRef = ref(null)
const submitBtnMagnetic = reactive({ x: 0, y: 0 })

const submitBtnMagneticStyle = computed(() => {
  return {
    transform: `translate(${submitBtnMagnetic.x}px, ${submitBtnMagnetic.y}px)`
  }
})

function handleButtonMagneticMove(e) {
  if (!submitBtnRef.value) return
  const rect = submitBtnRef.value.getBoundingClientRect()
  const btnCenterX = rect.left + rect.width / 2
  const btnCenterY = rect.top + rect.height / 2
  const deltaX = (e.clientX - btnCenterX) * 0.22
  const deltaY = (e.clientY - btnCenterY) * 0.22
  submitBtnMagnetic.x = deltaX
  submitBtnMagnetic.y = deltaY
}

function handleButtonMagneticLeave() {
  submitBtnMagnetic.x = 0
  submitBtnMagnetic.y = 0
}

function onHoverInteractive() {
  isHoveringInteractive.value = true
}

function onLeaveInteractive() {
  isHoveringInteractive.value = false
}

// =========================================================================
// 7. STAGE 3D PERSPECTIVE PARALLAX & AMBIENT GLOW LERP
// =========================================================================
const stageContainerRef = ref(null)
const stageTilt = reactive({ x: 0, y: 0 })
let glowRaf = null

const stageTiltStyle = computed(() => {
  return {
    transform: `perspective(1400px) rotateX(${stageTilt.x}deg) rotateY(${stageTilt.y}deg)`
  }
})

function handleViewportMouseMove(e) {
  mousePos.x = e.clientX
  mousePos.y = e.clientY
  isMouseInside.value = true

  // Cập nhật mắt mascot
  updateMascotEyes(e)

  // 3D Parallax cho stage container
  const centerX = window.innerWidth / 2
  const centerY = window.innerHeight / 2
  stageTilt.y = ((e.clientX - centerX) / centerX) * 2.2
  stageTilt.x = -((e.clientY - centerY) / centerY) * 2.2

  // 3D Tilt Card ở cột trái
  if (tiltCardRef.value && isHoveringTilt.value) {
    const rect = tiltCardRef.value.getBoundingClientRect()
    const cardX = e.clientX - rect.left
    const cardY = e.clientY - rect.top
    const cardCenterX = rect.width / 2
    const cardCenterY = rect.height / 2

    tiltAngle.y = ((cardX - cardCenterX) / cardCenterX) * 8
    tiltAngle.x = -((cardY - cardCenterY) / cardCenterY) * 8

    glarePos.x = (cardX / rect.width) * 100
    glarePos.y = (cardY / rect.height) * 100
    glarePos.opacity = 1
  }
}

function handleViewportMouseLeave() {
  isMouseInside.value = false
  stageTilt.x = 0
  stageTilt.y = 0
}

// Lerp mượt mà cho vầng hào quang chuột 60fps
function startGlowLerp() {
  function loop() {
    mouseGlowPos.x += (mousePos.x - mouseGlowPos.x) * 0.12
    mouseGlowPos.y += (mousePos.y - mouseGlowPos.y) * 0.12
    glowRaf = requestAnimationFrame(loop)
  }
  loop()
}

// Blobs phản ứng chuột
const blob1Style = computed(() => {
  const moveX = (mousePos.x - window.innerWidth / 2) * 0.025
  const moveY = (mousePos.y - window.innerHeight / 2) * 0.025
  return { transform: `translate(${moveX}px, ${moveY}px)` }
})

const blob2Style = computed(() => {
  const moveX = -(mousePos.x - window.innerWidth / 2) * 0.02
  const moveY = -(mousePos.y - window.innerHeight / 2) * 0.02
  return { transform: `translate(${moveX}px, ${moveY}px)` }
})

const blob3Style = computed(() => {
  const moveX = (mousePos.x - window.innerWidth / 2) * 0.012
  return { transform: `translate(${moveX}px, 0px)` }
})

// =========================================================================
// 8. DYNAMIC JS TYPEWRITER SLOGAN
// =========================================================================
const slogans = [
  'Đột phá Hiệu suất Doanh nghiệp',
  'Quản trị Chuỗi cung ứng Thông minh',
  'Kiến trúc High-Scale hàng triệu Giao dịch',
  'Bảo vệ Dòng tiền & Tồn kho Thời gian thực'
]
const dynamicSlogan = ref(slogans[0])
let sloganIndex = 0
let charIndex = slogans[0].length
let isDeleting = false
let typewriterTimeout = null

function tickTypewriter() {
  const currentFull = slogans[sloganIndex]
  if (isDeleting) {
    charIndex--
    dynamicSlogan.value = currentFull.substring(0, charIndex)
    if (charIndex === 0) {
      isDeleting = false
      sloganIndex = (sloganIndex + 1) % slogans.length
      typewriterTimeout = setTimeout(tickTypewriter, 300)
      return
    }
    typewriterTimeout = setTimeout(tickTypewriter, 35)
  } else {
    charIndex++
    dynamicSlogan.value = currentFull.substring(0, charIndex)
    if (charIndex === currentFull.length) {
      isDeleting = true
      typewriterTimeout = setTimeout(tickTypewriter, 2600)
      return
    }
    typewriterTimeout = setTimeout(tickTypewriter, 55)
  }
}

// =========================================================================
// 9. ANIMATED JS COUNTERS & LIVE CLOCK & SIMULATED REAL-TIME TPS
// =========================================================================
const counterUptime = ref(0)
const counterLatency = ref(100)
const liveTpsCounter = ref(1250)
const currentTimeStr = ref('')
let clockTimer = null
let tpsInterval = null

function animateCounters() {
  const duration = 1200
  const startTime = performance.now()

  function step(now) {
    const elapsed = now - startTime
    const progress = Math.min(elapsed / duration, 1)
    const easeOut = 1 - Math.pow(1 - progress, 3)

    counterUptime.value = 90 + easeOut * 9.98
    counterLatency.value = 80 - easeOut * 62
    liveTpsCounter.value = Math.round(easeOut * 1250)

    if (progress < 1) {
      requestAnimationFrame(step)
    } else {
      counterUptime.value = 99.98
      counterLatency.value = 18
      liveTpsCounter.value = 1250
    }
  }
  requestAnimationFrame(step)
}

function updateClock() {
  const d = new Date()
  const h = String(d.getHours()).padStart(2, '0')
  const m = String(d.getMinutes()).padStart(2, '0')
  const s = String(d.getSeconds()).padStart(2, '0')
  currentTimeStr.value = `${h}:${m}:${s} GMT+7`
}

function startLiveTpsSimulation() {
  tpsInterval = setInterval(() => {
    const delta = Math.floor(Math.random() * 5) + 1
    liveTpsCounter.value += delta
  }, 3500)
}

// =========================================================================
// 10. REAL-TIME AUDIT STREAM TICKER (JS WEBSOCKET SIMULATION)
// =========================================================================
const auditMessages = [
  '🟢 [Live] Cụm máy chủ VN-Central: Tải CPU 14% • RAM 36%',
  '🛡️ [TLS 1.3] Phiên bảo mật cấp Doanh nghiệp mã hóa AES-256 GCM',
  '⚡ [ACID Engine] Sẵn sàng xử lý 12.500+ TPS song song',
  '💼 [Cache L2] Hibernate Distributed Cache hit-rate 99.8%',
  '🚀 [Zero-Downtime] Cân bằng tải tự động qua Kubernetes Pods'
]
const currentAuditIndex = ref(0)
const currentAuditMessage = computed(() => auditMessages[currentAuditIndex.value])
let auditTickerInterval = null

function startAuditTicker() {
  auditTickerInterval = setInterval(() => {
    currentAuditIndex.value = (currentAuditIndex.value + 1) % auditMessages.length
  }, 4200)
}

// =========================================================================
// 11. 3D TILT EFFECT TRÊN TILT CARD
// =========================================================================
const tiltCardRef = ref(null)
const isHoveringTilt = ref(false)
const tiltAngle = reactive({ x: 0, y: 0 })
const glarePos = reactive({ x: 50, y: 50, opacity: 0 })

const tiltStyle = computed(() => {
  if (!isHoveringTilt.value) {
    return {
      transform: 'perspective(1000px) rotateX(0deg) rotateY(0deg) translateZ(0px)',
      transition: 'transform 0.5s ease-out'
    }
  }
  return {
    transform: `perspective(1000px) rotateX(${tiltAngle.x}deg) rotateY(${tiltAngle.y}deg) translateZ(8px)`,
    transition: 'transform 0.08s ease-out'
  }
})

const glareStyle = computed(() => {
  return {
    background: `radial-gradient(circle at ${glarePos.x}% ${glarePos.y}%, rgba(37, 99, 235, 0.12) 0%, rgba(255, 255, 255, 0) 70%)`,
    opacity: glarePos.opacity,
    transition: 'opacity 0.3s ease'
  }
})

function onTiltLeave() {
  isHoveringTilt.value = false
  tiltAngle.x = 0
  tiltAngle.y = 0
  glarePos.opacity = 0
}

// =========================================================================
// 12. CANVAS INTERACTIVE PARTICLES (NEURAL CONSTELLATION)
// =========================================================================
const canvasRef = ref(null)
let animFrameId = null

function initCanvasParticles() {
  const canvas = canvasRef.value
  if (!canvas) return
  const ctx = canvas.getContext('2d')
  if (!ctx) return

  function resize() {
    canvas.width = window.innerWidth
    canvas.height = window.innerHeight
  }
  resize()
  window.addEventListener('resize', resize)

  const count = 48
  const particles = []

  for (let i = 0; i < count; i++) {
    particles.push({
      x: Math.random() * canvas.width,
      y: Math.random() * canvas.height,
      vx: (Math.random() - 0.5) * 0.45,
      vy: (Math.random() - 0.5) * 0.45,
      radius: Math.random() * 2 + 1,
      baseAlpha: Math.random() * 0.4 + 0.15
    })
  }

  function render() {
    ctx.clearRect(0, 0, canvas.width, canvas.height)

    // Xác định màu sắc hạt theo Theme Aura hiện tại
    let particleColor = 'rgba(37, 99, 235, '
    let lineColor = 'rgba(147, 197, 253, '
    if (currentTheme.value === 'theme-emerald') {
      particleColor = 'rgba(16, 185, 129, '
      lineColor = 'rgba(167, 243, 208, '
    } else if (currentTheme.value === 'theme-amethyst') {
      particleColor = 'rgba(139, 92, 246, '
      lineColor = 'rgba(216, 180, 254, '
    }

    for (let i = 0; i < count; i++) {
      const p = particles[i]
      p.x += p.vx
      p.y += p.vy

      if (p.x < 0) p.x = canvas.width
      if (p.x > canvas.width) p.x = 0
      if (p.y < 0) p.y = canvas.height
      if (p.y > canvas.height) p.y = 0

      // Vẽ hạt tròn
      ctx.beginPath()
      ctx.arc(p.x, p.y, p.radius, 0, Math.PI * 2)
      ctx.fillStyle = particleColor + p.baseAlpha + ')'
      ctx.fill()

      // Tương tác chuột kéo hút nhẹ hạt xung quanh
      const dxMouse = mousePos.x - p.x
      const dyMouse = mousePos.y - p.y
      const distMouse = Math.sqrt(dxMouse * dxMouse + dyMouse * dyMouse)
      if (distMouse < 140) {
        ctx.beginPath()
        ctx.moveTo(p.x, p.y)
        ctx.lineTo(mousePos.x, mousePos.y)
        ctx.strokeStyle = lineColor + (1 - distMouse / 140) * 0.28 + ')'
        ctx.lineWidth = 0.8
        ctx.stroke()
      }

      // Nối dây giữa các hạt
      for (let j = i + 1; j < count; j++) {
        const p2 = particles[j]
        const dx = p.x - p2.x
        const dy = p.y - p2.y
        const dist = Math.sqrt(dx * dx + dy * dy)
        if (dist < 100) {
          ctx.beginPath()
          ctx.moveTo(p.x, p.y)
          ctx.lineTo(p2.x, p2.y)
          ctx.strokeStyle = lineColor + (1 - dist / 100) * 0.16 + ')'
          ctx.lineWidth = 0.6
          ctx.stroke()
        }
      }
    }

    animFrameId = requestAnimationFrame(render)
  }

  render()
}

// =========================================================================
// 13. CONFETTI CELEBRATION EFFECT KHI ĐĂNG NHẬP THÀNH CÔNG
// =========================================================================
const confettiCanvasRef = ref(null)
const confettiPieces = []

function triggerConfetti() {
  const canvas = confettiCanvasRef.value
  if (!canvas) return
  const ctx = canvas.getContext('2d')
  if (!ctx) return

  canvas.width = window.innerWidth
  canvas.height = window.innerHeight

  const colors = ['#2563eb', '#3b82f6', '#10b981', '#f59e0b', '#ec4899', '#8b5cf6']
  confettiPieces.length = 0

  for (let i = 0; i < 90; i++) {
    confettiPieces.push({
      x: window.innerWidth / 2,
      y: window.innerHeight / 2,
      w: Math.random() * 8 + 4,
      h: Math.random() * 8 + 4,
      vx: (Math.random() - 0.5) * 16,
      vy: (Math.random() - 0.7) * 18,
      gravity: 0.35,
      rotation: Math.random() * 360,
      vRotation: (Math.random() - 0.5) * 10,
      color: colors[Math.floor(Math.random() * colors.length)],
      opacity: 1
    })
  }

  let confettiFrame = null
  function runConfetti() {
    ctx.clearRect(0, 0, canvas.width, canvas.height)
    let hasLive = false

    for (const c of confettiPieces) {
      c.x += c.vx
      c.y += c.vy
      c.vy += c.gravity
      c.rotation += c.vRotation
      c.opacity -= 0.012

      if (c.opacity > 0) {
        hasLive = true
        ctx.save()
        ctx.translate(c.x, c.y)
        ctx.rotate((c.rotation * Math.PI) / 180)
        ctx.globalAlpha = Math.max(0, c.opacity)
        ctx.fillStyle = c.color
        ctx.fillRect(-c.w / 2, -c.h / 2, c.w, c.h)
        ctx.restore()
      }
    }

    if (hasLive) {
      confettiFrame = requestAnimationFrame(runConfetti)
    } else {
      ctx.clearRect(0, 0, canvas.width, canvas.height)
      if (confettiFrame) cancelAnimationFrame(confettiFrame)
    }
  }

  runConfetti()
}

// =========================================================================
// 14. MICRO SPARKLES CANVAS KHI GIẢI ĐÚNG CAPTCHA
// =========================================================================
const sparkleCanvasRef = ref(null)
const sparkles = []

function onCaptchaVerified() {
  playChimeSound()
  ElMessage.success('⚡ Xác thực phép tính thành công!')

  const canvas = sparkleCanvasRef.value
  if (!canvas || !captchaSlotRef.value) return
  const ctx = canvas.getContext('2d')
  if (!ctx) return

  canvas.width = window.innerWidth
  canvas.height = window.innerHeight

  const rect = captchaSlotRef.value.getBoundingClientRect()
  const originX = rect.left + rect.width / 2
  const originY = rect.top + rect.height / 2

  sparkles.length = 0
  for (let i = 0; i < 28; i++) {
    const angle = Math.random() * Math.PI * 2
    const speed = Math.random() * 4 + 2
    sparkles.push({
      x: originX,
      y: originY,
      vx: Math.cos(angle) * speed,
      vy: Math.sin(angle) * speed,
      radius: Math.random() * 3 + 1.5,
      color: ['#10b981', '#34d399', '#2563eb', '#60a5fa'][Math.floor(Math.random() * 4)],
      alpha: 1
    })
  }

  let sparkleFrame = null
  function runSparkles() {
    ctx.clearRect(0, 0, canvas.width, canvas.height)
    let hasLive = false

    for (const s of sparkles) {
      s.x += s.vx
      s.y += s.vy
      s.alpha -= 0.03
      if (s.alpha > 0) {
        hasLive = true
        ctx.beginPath()
        ctx.arc(s.x, s.y, s.radius, 0, Math.PI * 2)
        ctx.fillStyle = s.color
        ctx.globalAlpha = Math.max(0, s.alpha)
        ctx.fill()
      }
    }

    if (hasLive) {
      sparkleFrame = requestAnimationFrame(runSparkles)
    } else {
      ctx.clearRect(0, 0, canvas.width, canvas.height)
      if (sparkleFrame) cancelAnimationFrame(sparkleFrame)
    }
  }
  runSparkles()
}

// =========================================================================
// 15. FORM ACTIONS & RIPPLE
// =========================================================================
function fillCredentials(user, pass) {
  playPopSound(480)
  loginForm.username = user
  loginForm.password = pass
  calcPasswordStrength()
  isLoginCaptchaPassed.value = true
  ElMessage.success(`⚡ Đã điền thông tin tài khoản mẫu [${user}]!`)
}

function fillPreviewOtp(otpVal) {
  playPopSound(500)
  forgotForm.otp = otpVal
  ElMessage.success(`⚡ Đã điền mã OTP: ${otpVal}`)
}

function switchMode(newMode) {
  playPopSound(440)
  mode.value = newMode
  if (newMode === 'login') {
    isLoginCaptchaPassed.value = false
    loginCaptchaRef.value?.generateNewEquation?.()
  } else {
    forgotStep.value = 1
    isForgotCaptchaPassed.value = false
    forgotCaptchaRef.value?.generateNewEquation?.()
  }
}

function createRipple(e) {
  const btn = e.currentTarget
  const circle = document.createElement('span')
  const diameter = Math.max(btn.clientWidth, btn.clientHeight)
  const radius = diameter / 2
  circle.style.width = circle.style.height = `${diameter}px`
  circle.style.left = `${e.clientX - btn.getBoundingClientRect().left - radius}px`
  circle.style.top = `${e.clientY - btn.getBoundingClientRect().top - radius}px`
  circle.classList.add('btn-ripple-effect')
  const ripple = btn.getElementsByClassName('btn-ripple-effect')[0]
  if (ripple) ripple.remove()
  btn.appendChild(circle)
}

function onSubmitClick(e) {
  createRipple(e)
  playPopSound(560)
}

async function handleLogin() {
  if (!loginForm.username || !loginForm.password) {
    ElMessage.warning('Vui lòng nhập đầy đủ tài khoản và mật khẩu!')
    return
  }

  if (!isLoginCaptchaPassed.value) {
    ElMessage.warning('Vui lòng giải phép tính bảo mật trước khi đăng nhập!')
    return
  }

  loading.value = true
  try {
    const success = await authStore.login(loginForm.username, loginForm.password)
    if (success) {
      playSuccessFanfare()
      triggerConfetti()
      ElMessage.success(`🎉 Chào mừng ${loginForm.username} quay trở lại ERP Cloud!`)
      setTimeout(() => {
        router.push('/')
      }, 700)
    }
  } catch (err) {
    const msg = err?.response?.data?.message || err?.message || 'Tên đăng nhập hoặc mật khẩu không chính xác!'
    ElMessage.error(msg)
    isLoginCaptchaPassed.value = false
    loginCaptchaRef.value?.generateNewEquation?.()
  } finally {
    loading.value = false
  }
}

function handleSendOtp() {
  forgotLoading.value = true
  playPopSound(480)
  setTimeout(() => {
    forgotLoading.value = false
    forgotStep.value = 2
    ElMessage.success('Mã OTP đã được gửi đến hộp thư!')
  }, 600)
}

function handleVerifyOtp() {
  if (forgotForm.otp !== '888666' && forgotForm.otp.length !== 6) {
    ElMessage.error('Mã OTP không đúng! (Dùng mã giả lập 888666)')
    return
  }
  playChimeSound()
  forgotStep.value = 3
}

function handleResetPassword() {
  playSuccessFanfare()
  forgotStep.value = 4
}

// =========================================================================
// 16. LIFECYCLE HOOKS
// =========================================================================
onMounted(() => {
  initCanvasParticles()
  startGlowLerp()
  animateCounters()
  updateClock()
  startLiveTpsSimulation()
  startAuditTicker()
  clockTimer = setInterval(updateClock, 1000)
  typewriterTimeout = setTimeout(tickTypewriter, 1000)
})

onBeforeUnmount(() => {
  if (animFrameId) cancelAnimationFrame(animFrameId)
  if (glowRaf) cancelAnimationFrame(glowRaf)
  if (clockTimer) clearInterval(clockTimer)
  if (tpsInterval) clearInterval(tpsInterval)
  if (auditTickerInterval) clearInterval(auditTickerInterval)
  if (typewriterTimeout) clearTimeout(typewriterTimeout)
})
</script>

<style scoped>
/* =========================================================================
   1. VIEWPORT & CONTAINER: NỀN SÁNG CAO CẤP (LIGHT LUXURY SAAS)
   ========================================================================= */
.login-viewport {
  width: 100vw;
  height: 100vh;
  max-height: 100vh;
  overflow: hidden;
  position: relative;
  background: radial-gradient(circle at 10% 20%, #eff6ff 0%, #f8fafc 55%, #e2e8f0 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  user-select: none;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
  transition: background 0.5s ease;
}

/* Biến đổi Theme Aura sáng */
.login-viewport.theme-sapphire {
  --primary-color: #2563eb;
  --primary-hover: #1d4ed8;
  --primary-light: #eff6ff;
  --primary-border: #bfdbfe;
  --blob-c1: #bfdbfe;
  --blob-c2: #e0e7ff;
}

.login-viewport.theme-emerald {
  --primary-color: #059669;
  --primary-hover: #047857;
  --primary-light: #ecfdf5;
  --primary-border: #a7f3d0;
  --blob-c1: #a7f3d0;
  --blob-c2: #dcfce7;
  background: radial-gradient(circle at 10% 20%, #f0fdf4 0%, #f8fafc 55%, #e2e8f0 100%);
}

.login-viewport.theme-amethyst {
  --primary-color: #7c3aed;
  --primary-hover: #6d28d9;
  --primary-light: #f5f3ff;
  --primary-border: #ddd6fe;
  --blob-c1: #ddd6fe;
  --blob-c2: #ede9fe;
  background: radial-gradient(circle at 10% 20%, #faf5ff 0%, #f8fafc 55%, #e2e8f0 100%);
}

/* Canvases */
.interactive-particle-canvas,
.confetti-canvas,
.sparkle-canvas {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
}

.interactive-particle-canvas { z-index: 1; }
.sparkle-canvas { z-index: 98; }
.confetti-canvas { z-index: 99; }

/* Ambient Glow Follower */
.mouse-ambient-glow {
  position: absolute;
  width: 450px;
  height: 450px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(37, 99, 235, 0.08) 0%, rgba(255, 255, 255, 0) 70%);
  pointer-events: none;
  z-index: 1;
  transition: opacity 0.3s ease, transform 0.25s cubic-bezier(0.1, 0.8, 0.3, 1);
}

.login-viewport.theme-emerald .mouse-ambient-glow {
  background: radial-gradient(circle, rgba(5, 150, 105, 0.08) 0%, rgba(255, 255, 255, 0) 70%);
}

.login-viewport.theme-amethyst .mouse-ambient-glow {
  background: radial-gradient(circle, rgba(124, 58, 237, 0.08) 0%, rgba(255, 255, 255, 0) 70%);
}

/* Background Aurora Blobs */
.aurora-blob {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  pointer-events: none;
  z-index: 1;
  opacity: 0.55;
  transition: transform 0.2s cubic-bezier(0.1, 0.9, 0.2, 1);
}

.blob-1 {
  width: 480px;
  height: 480px;
  top: -120px;
  left: -80px;
  background: radial-gradient(circle, var(--blob-c1, #bfdbfe) 0%, transparent 70%);
}

.blob-2 {
  width: 520px;
  height: 520px;
  bottom: -150px;
  right: -100px;
  background: radial-gradient(circle, var(--blob-c2, #e0e7ff) 0%, transparent 70%);
}

.blob-3 {
  width: 380px;
  height: 380px;
  top: 40%;
  right: 40%;
  background: radial-gradient(circle, #f1f5f9 0%, transparent 70%);
  opacity: 0.4;
}

/* Top Floating Toolbar (Theme & Sound) */
.top-floating-toolbar {
  position: absolute;
  top: 16px;
  right: 28px;
  z-index: 10;
  display: flex;
  align-items: center;
  gap: 12px;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  padding: 6px 14px;
  border-radius: 24px;
  border: 1px solid rgba(226, 232, 240, 0.9);
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.04);
}

.theme-aura-selector {
  display: flex;
  align-items: center;
  gap: 6px;
}

.toolbar-hint-txt {
  font-size: 11px;
  font-weight: 600;
  color: #64748b;
  margin-right: 2px;
}

.theme-color-dot {
  width: 14px;
  height: 14px;
  border-radius: 50%;
  border: 2px solid transparent;
  cursor: pointer;
  transition: all 0.2s ease;
}

.theme-color-dot.sapphire { background: #2563eb; }
.theme-color-dot.emerald { background: #059669; }
.theme-color-dot.amethyst { background: #7c3aed; }

.theme-color-dot:hover {
  transform: scale(1.2);
}

.theme-color-dot.active {
  border-color: #0f172a;
  transform: scale(1.25);
  box-shadow: 0 0 8px rgba(0, 0, 0, 0.15);
}

.sound-toggle-btn {
  display: flex;
  align-items: center;
  gap: 5px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 14px;
  padding: 3px 8px;
  font-size: 11px;
  font-weight: 600;
  color: #475569;
  cursor: pointer;
  transition: all 0.2s ease;
}

.sound-toggle-btn:hover {
  background: #f1f5f9;
  color: #0f172a;
}

.sound-toggle-btn.enabled {
  background: #eff6ff;
  border-color: #bfdbfe;
  color: #1d4ed8;
}

/* Main Split Stage Vừa Vặn PC Desktop */
.login-stage-container {
  position: relative;
  z-index: 2;
  width: 90vw;
  max-width: 1180px;
  height: 86vh;
  max-height: 720px;
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-radius: 20px;
  border: 1px solid rgba(226, 232, 240, 0.9);
  box-shadow: 0 25px 60px -15px rgba(30, 58, 138, 0.09), 0 0 25px rgba(59, 130, 246, 0.05);
  display: grid;
  grid-template-columns: 1.15fr 1fr;
  overflow: hidden;
  transition: transform 0.25s cubic-bezier(0.1, 0.8, 0.3, 1);
}

@media (max-width: 900px) {
  .login-viewport {
    height: auto;
    min-height: 100vh;
    overflow-y: auto;
    padding: 20px 10px;
  }
  .login-stage-container {
    height: auto;
    max-height: none;
    grid-template-columns: 1fr;
  }
  .hero-showcase-panel {
    display: none;
  }
}

/* =========================================================================
   2. CỘT TRÁI: ENTERPRISE SHOWCASE PANEL (NỀN SÁNG SANG TRỌNG)
   ========================================================================= */
.hero-showcase-panel {
  padding: 32px 36px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  border-right: 1px solid #e2e8f0;
  background: linear-gradient(145deg, #ffffff 0%, #f8fafc 60%, #f1f5f9 100%);
}

.brand-top-bar {
  display: flex;
  align-items: center;
  gap: 12px;
}

.brand-emblem-badge {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  background: linear-gradient(135deg, var(--primary-hover, #1d4ed8) 0%, var(--primary-color, #2563eb) 100%);
  color: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 14px rgba(37, 99, 235, 0.28);
}

.brand-name {
  font-size: 18px;
  font-weight: 800;
  letter-spacing: 0.8px;
  color: #0f172a;
}

.brand-tagline {
  font-size: 10px;
  font-weight: 700;
  color: var(--primary-color, #2563eb);
  letter-spacing: 1px;
}

.hero-content-body {
  margin: auto 0;
  padding: 8px 0;
}

.pill-kicker {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  background: var(--primary-light, #eff6ff);
  border: 1px solid var(--primary-border, #bfdbfe);
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 11px;
  font-weight: 700;
  color: var(--primary-hover, #1d4ed8);
  margin-bottom: 14px;
}

.live-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--primary-color, #2563eb);
  box-shadow: 0 0 8px var(--primary-color, #3b82f6);
}

.hero-title {
  margin: 0 0 12px;
  font-size: 25px;
  font-weight: 800;
  line-height: 1.35;
  color: #0f172a;
}

.gradient-text {
  background: linear-gradient(90deg, var(--primary-color, #2563eb) 0%, #7c3aed 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.typewriter-cursor {
  color: var(--primary-color, #2563eb);
  font-weight: 400;
  animation: blink 0.8s infinite;
}

@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0; }
}

.hero-description {
  margin: 0 0 18px;
  font-size: 12.5px;
  line-height: 1.6;
  color: #475569;
  max-width: 480px;
}

/* 3D Tilt Showcase Card Nền Trắng Sang Trọng */
.interactive-tilt-card {
  position: relative;
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 14px;
  padding: 14px 18px;
  box-shadow: 0 12px 30px -5px rgba(30, 58, 138, 0.07);
  cursor: pointer;
  transform-style: preserve-3d;
  overflow: hidden;
}

.tilt-specular-glare {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  border-radius: 14px;
}

.tilt-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.tilt-status-chip {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 10px;
  font-weight: 700;
  color: #059669;
  letter-spacing: 0.5px;
}

.pulse-chip-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #10b981;
  box-shadow: 0 0 8px #34d399;
}

.tilt-time {
  font-size: 11px;
  font-family: monospace;
  color: #64748b;
  font-weight: 600;
}

.tilt-card-metrics {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #f8fafc;
  border-radius: 10px;
  padding: 8px 14px;
  border: 1px solid #f1f5f9;
}

.metric-mini-item {
  text-align: center;
}

.m-val {
  font-size: 15px;
  font-weight: 800;
  color: #0f172a;
}

.m-val.highlight-blue {
  color: var(--primary-color, #2563eb);
}

.m-lbl {
  font-size: 9.5px;
  color: #64748b;
  margin-top: 1px;
  font-weight: 500;
}

.m-sep {
  width: 1px;
  height: 22px;
  background: #e2e8f0;
}

/* Scanner Bar */
.security-scanner-bar {
  position: relative;
  margin-top: 12px;
  padding-top: 8px;
  border-top: 1px solid #f1f5f9;
  overflow: hidden;
}

.scanner-laser-line {
  position: absolute;
  top: 0;
  left: -40%;
  width: 40%;
  height: 2px;
  background: linear-gradient(90deg, transparent, var(--primary-color, #2563eb), transparent);
  animation: laserScan 2.8s infinite linear;
}

@keyframes laserScan {
  0% { left: -40%; }
  100% { left: 140%; }
}

.scanner-text-row {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 10.5px;
  color: #475569;
}

.cert-shield {
  font-size: 12px;
}

/* Security Audit Realtime Ticker */
.security-audit-ticker {
  margin: 10px 0 6px;
  padding: 6px 10px;
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  display: flex;
  align-items: center;
  gap: 10px;
  overflow: hidden;
}

.ticker-label {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 9px;
  font-weight: 800;
  color: #059669;
  background: #ecfdf5;
  padding: 2px 6px;
  border-radius: 4px;
  white-space: nowrap;
}

.ticker-radar-dot {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: #10b981;
  animation: radarBlink 1.2s infinite;
}

@keyframes radarBlink {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.4; transform: scale(1.4); }
}

.ticker-content {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ticker-text {
  font-size: 10.5px;
  font-family: monospace;
  color: #334155;
  animation: tickerSlideIn 0.4s ease-out;
}

@keyframes tickerSlideIn {
  from { opacity: 0; transform: translateY(6px); }
  to { opacity: 1; transform: translateY(0); }
}

.hero-footer-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 10.5px;
  color: #64748b;
  padding-top: 12px;
  border-top: 1px solid #e2e8f0;
}

.cluster-badge {
  display: flex;
  align-items: center;
  gap: 6px;
}

/* =========================================================================
   3. CỘT PHẢI: AUTH FORM PANEL & MASCOT CYBER SENTRY
   ========================================================================= */
.auth-card-panel {
  padding: 24px 34px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  background: #ffffff;
  color: #0f172a;
  overflow-y: auto;
}

.form-wrapper {
  max-width: 400px;
  width: 100%;
  margin: 0 auto;
}

/* Mascot Cyber Sentry SVG */
.mascot-sentry-wrap {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  margin-bottom: 8px;
}

.sentry-head {
  position: relative;
  width: 52px;
  height: 40px;
  background: #f1f5f9;
  border: 2px solid #cbd5e1;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.05);
  transition: all 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275);
}

.sentry-antenna {
  position: absolute;
  top: -8px;
  width: 2px;
  height: 8px;
  background: #94a3b8;
}

.antenna-dot {
  position: absolute;
  top: -4px;
  left: -3px;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--primary-color, #2563eb);
  box-shadow: 0 0 6px var(--primary-color, #3b82f6);
}

.sentry-face {
  display: flex;
  align-items: center;
  gap: 10px;
}

.sentry-eye {
  position: relative;
  width: 14px;
  height: 14px;
  border-radius: 50%;
  background: #ffffff;
  border: 1.5px solid #94a3b8;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.sentry-pupil {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--primary-hover, #1d4ed8);
  transition: transform 0.08s ease-out;
}

.sentry-hands {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: space-around;
  background: #e2e8f0;
  border-radius: 12px;
  opacity: 0;
  pointer-events: none;
  transform: translateY(10px) scale(0.8);
  transition: all 0.25s cubic-bezier(0.175, 0.885, 0.32, 1.275);
}

.sentry-hands.visible {
  opacity: 1;
  transform: translateY(0) scale(1);
}

.sentry-speech-bubble {
  background: var(--primary-light, #eff6ff);
  border: 1px solid var(--primary-border, #bfdbfe);
  border-radius: 8px;
  padding: 4px 10px;
  font-size: 10.5px;
  font-weight: 600;
  color: var(--primary-hover, #1d4ed8);
  max-width: 220px;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.03);
}

/* Form Headers */
.welcome-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.form-title {
  margin: 0;
  font-size: 22px;
  font-weight: 800;
  color: #0f172a;
}

.auth-pill-secure {
  font-size: 10px;
  font-weight: 700;
  background: #ecfdf5;
  color: #059669;
  padding: 3px 8px;
  border-radius: 6px;
  border: 1px solid #a7f3d0;
}

.form-sub {
  margin: 3px 0 12px;
  font-size: 12px;
  color: #64748b;
}

/* Quick Fill Buttons */
.quick-accounts-toolbar {
  background: #f8fafc;
  border: 1px dashed #cbd5e1;
  border-radius: 8px;
  padding: 7px 10px;
  margin-bottom: 12px;
}

.quick-title {
  font-size: 10.5px;
  font-weight: 600;
  color: #475569;
  display: block;
  margin-bottom: 5px;
}

.quick-btns-row {
  display: flex;
  gap: 8px;
}

.btn-quick-fill {
  flex: 1;
  border: none;
  padding: 5px 8px;
  border-radius: 6px;
  font-size: 11px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s cubic-bezier(0.175, 0.885, 0.32, 1.275);
}

.btn-quick-fill.admin {
  background: var(--primary-light, #eff6ff);
  color: var(--primary-hover, #1d4ed8);
  border: 1px solid var(--primary-border, #bfdbfe);
}

.btn-quick-fill.admin:hover {
  background: #dbeafe;
  transform: translateY(-1px);
}

.btn-quick-fill.user {
  background: #f0fdf4;
  color: #15803d;
  border: 1px solid #bbf7d0;
}

.btn-quick-fill.user:hover {
  background: #dcfce7;
  transform: translateY(-1px);
}

/* Form Fields */
.auth-form {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.form-field-group {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.field-label {
  font-size: 11.5px;
  font-weight: 600;
  color: #334155;
}

.pwd-label-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.link-forgot-btn {
  border: none;
  background: transparent;
  color: var(--primary-color, #2563eb);
  font-size: 11px;
  font-weight: 600;
  cursor: pointer;
}

.link-forgot-btn:hover {
  text-decoration: underline;
}

.input-container {
  display: flex;
  align-items: center;
  background: #f8fafc;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  padding: 0 10px;
  height: 38px;
  transition: all 0.15s ease;
}

.input-container:focus-within {
  border-color: var(--primary-color, #2563eb);
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.12);
  background: #ffffff;
}

.field-icon {
  font-size: 13px;
  margin-right: 8px;
  color: #64748b;
}

.field-input {
  flex: 1;
  border: none;
  background: transparent;
  outline: none;
  font-size: 12.5px;
  color: #0f172a;
}

.field-input::placeholder {
  color: #94a3b8;
}

.btn-toggle-eye {
  border: none;
  background: transparent;
  cursor: pointer;
  font-size: 14px;
  padding: 4px;
}

/* Password Strength Meter */
.password-strength-container {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 3px;
}

.strength-bar-track {
  flex: 1;
  height: 4px;
  background: #f1f5f9;
  border-radius: 2px;
  overflow: hidden;
}

.strength-bar-fill {
  height: 100%;
  border-radius: 2px;
  transition: width 0.3s ease, background-color 0.3s ease;
}

.strength-label-text {
  font-size: 10.5px;
  font-weight: 600;
}

.captcha-form-slot {
  margin: 2px 0;
}

.form-options-row {
  display: flex;
  align-items: center;
  font-size: 11.5px;
  color: #475569;
}

.checkbox-label {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
}

.custom-checkbox {
  cursor: pointer;
}

/* Submit Button với Magnetic & Ripple */
.btn-submit-primary {
  position: relative;
  overflow: hidden;
  height: 42px;
  background: linear-gradient(135deg, var(--primary-hover, #1d4ed8) 0%, var(--primary-color, #2563eb) 100%);
  color: #ffffff;
  border: none;
  border-radius: 8px;
  font-size: 12.5px;
  font-weight: 700;
  letter-spacing: 0.5px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  box-shadow: 0 4px 14px rgba(37, 99, 235, 0.25);
  transition: box-shadow 0.2s ease, background 0.3s ease;
  margin-top: 4px;
}

.btn-submit-primary:hover:not(:disabled) {
  box-shadow: 0 6px 18px rgba(37, 99, 235, 0.35);
}

.btn-submit-primary:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.btn-arrow {
  transition: transform 0.2s ease;
}

.btn-submit-primary:hover .btn-arrow {
  transform: translateX(3px);
}

:deep(.btn-ripple-effect) {
  position: absolute;
  border-radius: 50%;
  transform: scale(0);
  animation: rippleAnim 0.6s linear;
  background-color: rgba(255, 255, 255, 0.4);
  pointer-events: none;
}

@keyframes rippleAnim {
  to {
    transform: scale(4);
    opacity: 0;
  }
}

.spinner-dot {
  width: 14px;
  height: 14px;
  border: 2px solid #ffffff;
  border-top-color: transparent;
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.form-footer-line {
  margin-top: 14px;
  text-align: center;
  font-size: 11.5px;
  color: #64748b;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.admin-contact-hint {
  font-weight: 600;
  color: var(--primary-color, #2563eb);
}

/* Wizard Styles */
.wizard-step {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.wizard-btn-actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 4px;
}

.btn-cancel-link {
  border: none;
  background: transparent;
  color: #64748b;
  font-size: 11.5px;
  font-weight: 600;
  padding: 6px;
  cursor: pointer;
}

.btn-cancel-link:hover {
  color: #0f172a;
}

.otp-notice-box {
  background: #f0fdf4;
  border: 1px solid #bbf7d0;
  padding: 8px 12px;
  border-radius: 8px;
  font-size: 11.5px;
  color: #166534;
  display: flex;
  align-items: center;
  gap: 8px;
}

.demo-otp-pill {
  background: #fef3c7;
  border: 1px dashed #f59e0b;
  padding: 4px 8px;
  border-radius: 6px;
  font-size: 11px;
  color: #92400e;
  cursor: pointer;
}

.otp-input {
  letter-spacing: 4px;
  font-weight: 700;
  font-size: 15px;
}

.success-icon-seal {
  width: 50px;
  height: 50px;
  border-radius: 50%;
  background: #10b981;
  color: #ffffff;
  font-size: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 16px auto 8px;
  box-shadow: 0 8px 20px rgba(16, 185, 129, 0.35);
}

.success-step-title {
  font-size: 17px;
  font-weight: 700;
  color: #0f172a;
  margin: 0;
}

.success-step-desc {
  font-size: 12px;
  color: #64748b;
  margin: 4px 0 16px;
}

.text-center {
  text-align: center;
}
</style>
