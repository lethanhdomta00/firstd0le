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

    <!-- Con trỏ hào quang ánh sáng mềm mại theo chuột (JS Ambient Glow Spotlight với Lerp) -->
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
      <!-- BaseLoading Overlay chuẩn hệ thống -->
      <BaseLoading
        :active="loading"
        :text="loadingText"
        :sub-text="loadingSubText"
      />

      <!-- ================================================================= -->
      <!-- CỘT TRÁI: ENTERPRISE SHOWCASE (LIGHT TECH PORTAL NỀN SÁNG CAO CẤP)-->
      <!-- ================================================================= -->
      <aside class="hero-showcase-panel">
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
      </aside>

      <!-- ================================================================= -->
      <!-- CỘT PHẢI: FORM PANEL (LOGIN & FORGOT PASSWORD WIZARD CHUẨN GỐC)   -->
      <!-- ================================================================= -->
      <main class="auth-card-panel">
        <!-- MODE 1: FORM ĐĂNG NHẬP (GIỮ NGUYÊN 100% LOGIC GỐC) -->
        <div v-if="mode === 'login'" class="form-wrapper">
          <!-- Mascot Cyber Sentry theo dõi chuột & che mắt khi nhập mật khẩu -->
          <div class="mascot-sentry-wrap" title="Cyber Sentry: Trợ lý bảo mật ERP">
            <div class="sentry-head" :class="{ 'is-sleeping': isPasswordFocused && !showPassword, 'is-peeking': isPasswordFocused && showPassword }">
              <div class="sentry-antenna">
                <span class="antenna-dot"></span>
              </div>
              <div class="sentry-face">
                <div class="sentry-eye left-eye">
                  <div class="sentry-pupil" :style="leftPupilStyle"></div>
                  <div class="sentry-eyelid"></div>
                </div>
                <div class="sentry-eye right-eye">
                  <div class="sentry-pupil" :style="rightPupilStyle"></div>
                  <div class="sentry-eyelid"></div>
                </div>
              </div>
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
            <p class="form-sub">Nhập thông tin ủy nhiệm để truy cập sổ cái điều hành ERP Cloud</p>
          </div>

          <!-- Thẻ Thử Nghiệm (Demo Ledger) Chuẩn admin / password -->
          <div class="demo-ledger-box" @click="fillDemoAccount">
            <div class="demo-ledger-left">
              <span class="demo-ledger-icon">🔐</span>
              <div>
                <div class="demo-ledger-title">THẺ THỬ NGHIỆM (DEMO LEDGER)</div>
                <div class="demo-ledger-cred">Tài khoản: <strong>admin</strong> • Mật khẩu: <strong>password</strong></div>
              </div>
            </div>
            <button type="button" class="btn-fill-sample">
              ⚡ ĐIỀN MẪU
            </button>
          </div>

          <!-- Biểu Mẫu Đăng Nhập -->
          <form class="auth-form" @submit.prevent="handleLogin">
            <div class="form-field-group">
              <label class="field-label">Tên người dùng hoặc Email <span class="req">*</span></label>
              <div class="input-container" :class="{ 'has-value': loginForm.username }">
                <span class="field-icon">👤</span>
                <input
                  v-model="loginForm.username"
                  type="text"
                  class="field-input"
                  placeholder="admin@enterprisegroup.vn hoặc admin..."
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
                <label class="field-label">Mật khẩu bảo an <span class="req">*</span></label>
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

              <!-- Password Strength Gauge -->
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
                label="Phép tính bảo vệ hệ thống"
                :compact="true"
                @verified="onCaptchaVerified"
              />
            </div>

            <div class="form-options-row">
              <label class="checkbox-label">
                <input v-model="rememberMe" type="checkbox" class="custom-checkbox" @change="playTickSound" />
                <span>Lưu phiên đăng nhập trên thiết bị ủy thác (30 ngày)</span>
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
              <span>{{ loading ? 'ĐANG XÁC THỰC THÔNG TIN...' : 'ĐĂNG NHẬP VÀO HỆ THỐNG' }}</span>
              <span class="btn-arrow">➔</span>
            </button>
          </form>

          <!-- Phương thức Ủy quyền Doanh nghiệp (SSO) -->
          <div class="sso-divider-row">
            <span>PHƯƠNG THỨC ỦY QUYỀN DOANH NGHIỆP</span>
          </div>

          <div class="sso-btns-grid">
            <button type="button" class="btn-sso-item" @click="handleSocialLogin('Google Workspace')">
              <svg viewBox="0 0 24 24" width="14" height="14">
                <path fill="#4285F4" d="M23.745 12.27c0-.7-.06-1.4-.19-2.07H12v4.51h6.6c-.29 1.52-1.14 2.82-2.4 3.68v3.05h3.88c2.27-2.09 3.665-5.17 3.665-9.17z"/>
                <path fill="#34A853" d="M12 24c3.24 0 5.95-1.08 7.93-2.91l-3.88-3.05c-1.08.72-2.45 1.16-4.05 1.16-3.12 0-5.77-2.1-6.72-4.93H1.24v3.15C3.26 21.41 7.33 24 12 24z"/>
                <path fill="#FBBC05" d="M5.28 14.27c-.25-.72-.38-1.49-.38-2.27s.13-1.55.38-2.27V6.58H1.24C.45 8.16 0 9.98 0 12s.45 3.84 1.24 5.42l4.04-3.15z"/>
                <path fill="#EA4335" d="M12 4.75c1.77 0 3.35.61 4.6 1.8l3.42-3.42C17.95 1.19 15.24 0 12 0 7.33 0 3.26 2.59 1.24 6.58l4.04 3.15c.95-2.83 3.6-4.98 6.72-4.98z"/>
              </svg>
              <span>GOOGLE WORKSPACE</span>
            </button>
            <button type="button" class="btn-sso-item" @click="handleSocialLogin('Microsoft Entra')">
              <svg viewBox="0 0 24 24" width="14" height="14">
                <path fill="#F25022" d="M1 1h10v10H1z"/>
                <path fill="#7FBA00" d="M13 1h10v10H13z"/>
                <path fill="#00A4EF" d="M1 13h10v10H1z"/>
                <path fill="#FFB900" d="M13 13h10v10H13z"/>
              </svg>
              <span>MICROSOFT ENTRA</span>
            </button>
          </div>

          <div class="form-footer-line">
            <span>🔒 Giao thức mã hóa cấp chứng thư TLS 1.3</span>
            <span>Chưa có tài khoản? <strong class="admin-contact-hint">Liên hệ Quản trị viên cấp quyền</strong></span>
          </div>
        </div>

        <!-- MODE 2: QUÊN MẬT KHẨU WIZARD (GIỮ NGUYÊN 100% LOGIC GỐC) -->
        <div v-else class="form-wrapper forgot-wrapper">
          <div class="form-header">
            <div class="welcome-row">
              <h2 class="form-title">Khôi phục Mật khẩu</h2>
              <span class="auth-pill-secure">OTP Recovery</span>
            </div>
            <p class="form-sub">Nhận mã xác thực qua Email hoặc Telegram Bot để đặt lại mật mã</p>
          </div>

          <!-- Stepper 3 Bước Roman -->
          <div v-if="forgotStep <= 3" class="wizard-stepper">
            <div class="wizard-step-node" :class="{ active: forgotStep >= 1, current: forgotStep === 1 }">
              <div class="step-num-circle">
                <span v-if="forgotStep > 1">✓</span>
                <span v-else>1</span>
              </div>
              <span class="step-node-label">Tài khoản</span>
            </div>
            <div class="step-divider-line" :class="{ filled: forgotStep >= 2 }"></div>
            <div class="wizard-step-node" :class="{ active: forgotStep >= 2, current: forgotStep === 2 }">
              <div class="step-num-circle">
                <span v-if="forgotStep > 2">✓</span>
                <span v-else>2</span>
              </div>
              <span class="step-node-label">Mã OTP</span>
            </div>
            <div class="step-divider-line" :class="{ filled: forgotStep >= 3 }"></div>
            <div class="wizard-step-node" :class="{ active: forgotStep >= 3, current: forgotStep === 3 }">
              <div class="step-num-circle">3</div>
              <span class="step-node-label">Mật mã mới</span>
            </div>
          </div>

          <!-- BƯỚC 1: TÀI KHOẢN & KÊNH NHẬN OTP -->
          <div v-if="forgotStep === 1" class="wizard-step-body">
            <div class="form-field-group">
              <div class="pwd-label-row">
                <label class="field-label">Email hoặc Tên đăng nhập <span class="req">*</span></label>
                <button type="button" class="btn-sample-inline" @click="forgotForm.identifier = 'admin'">
                  ⚡ Điền mẫu (admin)
                </button>
              </div>
              <div class="input-container">
                <span class="field-icon">📧</span>
                <input
                  v-model="forgotForm.identifier"
                  type="text"
                  class="field-input"
                  placeholder="admin@enterprisegroup.vn hoặc admin..."
                  required
                  @keyup.enter="handleSendOtp"
                />
              </div>
            </div>

            <!-- Kênh nhận mã -->
            <div class="form-field-group">
              <label class="field-label">Phương thức tiếp nhận mã OTP</label>
              <div class="channel-grid">
                <button
                  type="button"
                  class="channel-pill-btn"
                  :class="{ active: forgotForm.channel === 'EMAIL' }"
                  @click="forgotForm.channel = 'EMAIL'"
                >
                  <span>✉️</span>
                  <span>Email doanh nghiệp</span>
                </button>
                <button
                  type="button"
                  class="channel-pill-btn"
                  :class="{ active: forgotForm.channel === 'TELEGRAM' }"
                  @click="forgotForm.channel = 'TELEGRAM'"
                >
                  <span>✈️</span>
                  <span>Telegram Bot</span>
                </button>
              </div>
            </div>

            <div v-if="forgotForm.channel === 'EMAIL'" class="form-field-group">
              <label class="field-label">Hộp thư nhận OTP (Tùy chọn)</label>
              <div class="input-container">
                <input
                  v-model="forgotForm.email"
                  type="email"
                  class="field-input"
                  placeholder="Mặc định: Email đã đăng ký trong hồ sơ"
                  @keyup.enter="handleSendOtp"
                />
              </div>
            </div>

            <div v-if="forgotForm.channel === 'TELEGRAM'" class="form-field-group">
              <label class="field-label">Telegram Chat ID (Tùy chọn)</label>
              <div class="input-container">
                <input
                  v-model="forgotForm.telegramChatId"
                  type="text"
                  class="field-input"
                  placeholder="Mặc định: Chat ID đã liên kết"
                  @keyup.enter="handleSendOtp"
                />
              </div>
            </div>

            <div class="captcha-form-slot">
              <BaseMathCaptcha
                ref="forgotCaptchaRef"
                v-model="isForgotCaptchaPassed"
                label="Giải phép tính để nhận mã OTP"
                :compact="true"
              />
            </div>

            <div class="wizard-actions">
              <button
                type="button"
                class="btn-submit-primary"
                :disabled="loading"
                @click="handleSendOtp"
              >
                <span>{{ loading ? 'ĐANG GỬI MÃ XÁC THỰC...' : 'GỬI MÃ XÁC THỰC OTP' }}</span>
                <span class="btn-arrow">➔</span>
              </button>
              <button type="button" class="btn-cancel-link" @click="switchMode('login')">
                ← Quay lại trang Đăng nhập
              </button>
            </div>
          </div>

          <!-- BƯỚC 2: XÁC THỰC MÃ OTP (6 Ô TÁCH RỜI CHUẨN GỐC) -->
          <div v-else-if="forgotStep === 2" class="wizard-step-body">
            <div class="otp-target-card">
              <span class="target-icon">✉️</span>
              <div class="target-text">
                Mã 6 chữ số niêm phong đã gửi đến:<br />
                <strong>{{ serverTargetInfo || forgotForm.identifier || 'admin@enterprisegroup.vn' }}</strong>
              </div>
            </div>

            <!-- Nút xem trước nếu server có trả về OTP demo -->
            <div
              v-if="serverGeneratedOtp"
              class="demo-otp-banner"
              @click="fillPreviewOtp(serverGeneratedOtp)"
            >
              <span>⚡ Mã xem trước: <strong>{{ serverGeneratedOtp }}</strong> (Nhấn để tự điền 6 ô)</span>
            </div>

            <!-- 6 Ô NHẬP OTP TÁCH RỜI (3 + • + 3) -->
            <div class="otp-inputs-row">
              <template v-for="(digit, idx) in otpDigits" :key="idx">
                <span v-if="idx === 3" class="otp-mid-divider">•</span>
                <input
                  :ref="(el) => setOtpInputRef(el, idx)"
                  v-model="otpDigits[idx]"
                  type="text"
                  inputmode="numeric"
                  maxlength="1"
                  class="otp-digit-box"
                  @input="handleOtpDigitInput(idx, $event)"
                  @keydown="handleOtpKeyDown(idx, $event)"
                  @paste="handleOtpPaste"
                />
              </template>
            </div>

            <!-- Đồng hồ đếm ngược 5 phút -->
            <div class="countdown-card">
              <div class="countdown-text-line">
                <span>⏳ Thời hạn niêm phong:</span>
                <strong>{{ formattedCountdown }}</strong>
              </div>
              <div class="countdown-progress-track">
                <div class="countdown-progress-bar" :style="{ width: countdownPercent + '%' }"></div>
              </div>
              <div class="resend-action-line">
                Chưa nhận được mã?
                <button type="button" class="btn-resend-link" @click="handleSendOtp">GỬI LẠI MÃ OTP</button>
              </div>
            </div>

            <div class="wizard-actions">
              <button
                type="button"
                class="btn-submit-primary"
                :disabled="loading"
                @click="handleVerifyOtp"
              >
                <span>{{ loading ? 'ĐANG KIỂM TRA MÃ...' : 'XÁC NHẬN MÃ BẢO AN' }}</span>
                <span class="btn-arrow">➔</span>
              </button>
              <button type="button" class="btn-cancel-link" @click="forgotStep = 1">
                ← Thay đổi tài khoản hoặc phương thức
              </button>
            </div>
          </div>

          <!-- BƯỚC 3: THIẾT LẬP MẬT KHẨU MỚI (CHUẨN NIST 800-63) -->
          <div v-else-if="forgotStep === 3" class="wizard-step-body">
            <div class="form-field-group">
              <div class="pwd-label-row">
                <label class="field-label">Mật mã bảo an mới <span class="req">*</span></label>
                <span class="nist-tag">CHUẨN NIST 800-63</span>
              </div>
              <div class="input-container">
                <span class="field-icon">🔓</span>
                <input
                  v-model="forgotForm.newPassword"
                  :type="showNewPwd ? 'text' : 'password'"
                  class="field-input"
                  placeholder="Nhập mật khẩu mới..."
                />
                <button type="button" class="btn-toggle-eye" @click="showNewPwd = !showNewPwd">
                  {{ showNewPwd ? '🙈' : '👁️' }}
                </button>
              </div>
            </div>

            <!-- 4 Tiêu chí độ vững mạnh -->
            <div class="pwd-criteria-panel">
              <div class="criteria-header">
                <span>Độ vững mạnh:</span>
                <strong :class="passwordStrengthInfo.colorClass">{{ passwordStrengthInfo.label }}</strong>
              </div>
              <div class="criteria-bars-grid">
                <div
                  v-for="bar in 4"
                  :key="bar"
                  class="criteria-bar-seg"
                  :class="{ filled: passwordStrengthInfo.score >= bar, [passwordStrengthInfo.colorClass]: passwordStrengthInfo.score >= bar }"
                ></div>
              </div>
              <div class="criteria-checklist">
                <div class="chk-item" :class="{ passed: pwdCriteria.minLength }">
                  <span>{{ pwdCriteria.minLength ? '✓' : '○' }}</span>
                  <span>Tối thiểu 8 ký tự</span>
                </div>
                <div class="chk-item" :class="{ passed: pwdCriteria.mixedCase }">
                  <span>{{ pwdCriteria.mixedCase ? '✓' : '○' }}</span>
                  <span>Chữ hoa &amp; chữ thường</span>
                </div>
                <div class="chk-item" :class="{ passed: pwdCriteria.hasNumber }">
                  <span>{{ pwdCriteria.hasNumber ? '✓' : '○' }}</span>
                  <span>Ký tự số (0-9)</span>
                </div>
                <div class="chk-item" :class="{ passed: pwdCriteria.hasSpecial }">
                  <span>{{ pwdCriteria.hasSpecial ? '✓' : '○' }}</span>
                  <span>Ký tự đặc biệt</span>
                </div>
              </div>
            </div>

            <div class="form-field-group">
              <label class="field-label">Xác nhận mật mã mới <span class="req">*</span></label>
              <div class="input-container">
                <span class="field-icon">🛡️</span>
                <input
                  v-model="forgotForm.confirmPassword"
                  :type="showConfirmPwd ? 'text' : 'password'"
                  class="field-input"
                  placeholder="Nhập lại mật khẩu mới..."
                  @keyup.enter="handleResetPassword"
                />
                <span v-if="forgotForm.confirmPassword && forgotForm.confirmPassword === forgotForm.newPassword" class="pwd-matched-ico">✓</span>
              </div>
            </div>

            <div class="wizard-actions">
              <button
                type="button"
                class="btn-submit-primary"
                :disabled="loading"
                @click="handleResetPassword"
              >
                <span>{{ loading ? 'ĐANG CẬP NHẬT...' : 'CẬP NHẬT MẬT MÃ BẢO AN' }}</span>
                <span class="btn-arrow">➔</span>
              </button>
              <button type="button" class="btn-cancel-link" @click="switchMode('login')">
                ↩ Hủy bỏ và quay lại Đăng nhập
              </button>
            </div>
          </div>

          <!-- BƯỚC 4: HOÀN TẤT THÀNH CÔNG -->
          <div v-else class="wizard-step-body text-center">
            <div class="success-seal-circle">✓</div>
            <h3 class="success-title">Khôi phục thành công!</h3>
            <p class="success-desc">
              Mật mã bảo an mới đã được cập nhật và lưu trữ an toàn vào sổ cái hệ thống.
            </p>
            <button
              type="button"
              class="btn-submit-primary"
              @click="goToLoginAfterReset"
            >
              <span>ĐĂNG NHẬP NGAY VỚI MẬT MÃ MỚI</span>
              <span class="btn-arrow">➔</span>
            </button>
          </div>
        </div>
      </main>
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
import api from '../api'
import BaseMathCaptcha from '../components/base/BaseMathCaptcha.vue'
import BaseLoading from '../components/base/BaseLoading.vue'

const router = useRouter()
const authStore = useAuthStore()

// =========================================================================
// 1. HELPER FUNCTIONS
// =========================================================================
function formatNumber(val) {
  if (val == null) return '0'
  return val.toString().replace(/\B(?=(\d{3})+(?!\d))/g, '.')
}

// =========================================================================
// 2. STATE KHAI BÁO & FORMS (BẢO TOÀN 100% LOGIC GỐC)
// =========================================================================
const mode = ref('login') // 'login' | 'forgot'
const loading = ref(false)
const loadingText = ref('Đang xử lý...')
const loadingSubText = ref('Vui lòng đợi giây lát...')
const showPassword = ref(false)
const showNewPwd = ref(false)
const showConfirmPwd = ref(false)
const rememberMe = ref(true)

// Focus tracking
const isPasswordFocused = ref(false)
const isUsernameFocused = ref(false)

// Captcha refs
const isLoginCaptchaPassed = ref(false)
const isForgotCaptchaPassed = ref(false)
const loginCaptchaRef = ref(null)
const forgotCaptchaRef = ref(null)
const captchaSlotRef = ref(null)

// Login form
const loginForm = reactive({
  username: '',
  password: ''
})

// Forgot Password Wizard
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

// 6 ô nhập OTP riêng biệt
const otpDigits = ref(['', '', '', '', '', ''])
const otpInputRefs = ref([])

// Đếm ngược OTP 5 phút (300 giây)
const otpSecondsLeft = ref(300)
let otpCountdownTimer = null

// Password Strength State (Form Login)
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

// Password Strength State (Forgot Step 3 NIST)
const formattedCountdown = computed(() => {
  const m = Math.floor(otpSecondsLeft.value / 60)
  const s = otpSecondsLeft.value % 60
  return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
})

const countdownPercent = computed(() => {
  return Math.max(0, Math.min(100, Math.round((otpSecondsLeft.value / 300) * 100)))
})

const pwdCriteria = computed(() => {
  const p = forgotForm.newPassword || ''
  return {
    minLength: p.length >= 8,
    mixedCase: /[a-z]/.test(p) && /[A-Z]/.test(p),
    hasNumber: /[0-9]/.test(p),
    hasSpecial: /[^A-Za-z0-9]/.test(p)
  }
})

const passwordStrengthInfo = computed(() => {
  const c = pwdCriteria.value
  const p = forgotForm.newPassword || ''
  if (!p) {
    return { score: 0, label: 'CHƯA NHẬP', colorClass: 'str-none' }
  }
  let score = 0
  if (c.minLength || p.length >= 6) score++
  if (c.mixedCase) score++
  if (c.hasNumber) score++
  if (c.hasSpecial) score++

  if (score <= 1) return { score: 1, label: 'YẾU', colorClass: 'str-weak' }
  if (score === 2) return { score: 2, label: 'TRUNG BÌNH', colorClass: 'str-fair' }
  if (score === 3) return { score: 3, label: 'MẠNH', colorClass: 'str-good' }
  return { score: 4, label: 'TỐI ƯU (CẤP ĐỘ NGOẠI GIAO)', colorClass: 'str-strong' }
})

// =========================================================================
// 3. OTP TIMER & OTP HELPERS
// =========================================================================
const stopOtpTimer = () => {
  if (otpCountdownTimer) {
    clearInterval(otpCountdownTimer)
    otpCountdownTimer = null
  }
}

const startOtpTimer = () => {
  stopOtpTimer()
  otpSecondsLeft.value = 300
  otpCountdownTimer = setInterval(() => {
    if (otpSecondsLeft.value > 0) {
      otpSecondsLeft.value -= 1
    } else {
      stopOtpTimer()
    }
  }, 1000)
}

const setOtpInputRef = (el, idx) => {
  if (el) {
    otpInputRefs.value[idx] = el
  }
}

const syncOtpToForm = () => {
  forgotForm.otp = otpDigits.value.join('')
}

const fillPreviewOtp = (code) => {
  if (!code) return
  playPopSound(500)
  const chars = String(code).trim().slice(0, 6).split('')
  for (let i = 0; i < 6; i++) {
    otpDigits.value[i] = chars[i] || ''
  }
  syncOtpToForm()
  ElMessage.success(`⚡ Đã tự động điền mã OTP: ${code}`)
}

const handleOtpDigitInput = (idx, e) => {
  const val = (e.target.value || '').replace(/\D/g, '')
  otpDigits.value[idx] = val.slice(-1)
  syncOtpToForm()
  playKeySound()
  if (val && idx < 5 && otpInputRefs.value[idx + 1]) {
    otpInputRefs.value[idx + 1].focus()
  }
}

const handleOtpKeyDown = (idx, e) => {
  if (e.key === 'Backspace' && !otpDigits.value[idx] && idx > 0 && otpInputRefs.value[idx - 1]) {
    otpInputRefs.value[idx - 1].focus()
  } else if (e.key === 'Enter') {
    handleVerifyOtp()
  }
}

const handleOtpPaste = (e) => {
  e.preventDefault()
  const pasted = (e.clipboardData?.getData('text') || '').replace(/\D/g, '').slice(0, 6)
  if (!pasted) return
  for (let i = 0; i < 6; i++) {
    otpDigits.value[i] = pasted[i] || ''
  }
  syncOtpToForm()
  playPopSound(520)
  const focusIdx = Math.min(5, pasted.length)
  if (otpInputRefs.value[focusIdx]) {
    otpInputRefs.value[focusIdx].focus()
  }
}

// =========================================================================
// 4. THEME AURA SWITCHER
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
// 5. WEB AUDIO SYNTHESIZER
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
    // Ignore audio policy
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
// 6. MASCOT CYBER SENTRY EYE TRACKING
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
  if (loading.value) return 'Đang xử lý thông tin...'
  if (isPasswordFocused.value && !showPassword.value) return 'Mascot đã nhắm mắt bảo mật!'
  if (isPasswordFocused.value && showPassword.value) return 'Đang mở hé một mắt nhìn lén...'
  if (isUsernameFocused.value) return 'Nhập tên tài khoản của bạn nhé!'
  if (isLoginCaptchaPassed.value) return 'Đã vượt qua kiểm tra chống bot! ✓'
  return 'Sẵn sàng điều hành hệ thống!'
})

function updateMascotEyes(e) {
  const maxRadius = 4.5
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
// 7. MAGNETIC BUTTON EFFECT
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
// 8. STAGE 3D PERSPECTIVE PARALLAX & AMBIENT GLOW LERP
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

  updateMascotEyes(e)

  const centerX = window.innerWidth / 2
  const centerY = window.innerHeight / 2
  stageTilt.y = ((e.clientX - centerX) / centerX) * 2.2
  stageTilt.x = -((e.clientY - centerY) / centerY) * 2.2

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

function startGlowLerp() {
  function loop() {
    mouseGlowPos.x += (mousePos.x - mouseGlowPos.x) * 0.12
    mouseGlowPos.y += (mousePos.y - mouseGlowPos.y) * 0.12
    glowRaf = requestAnimationFrame(loop)
  }
  loop()
}

// Aurora Blobs
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
// 9. DYNAMIC JS TYPEWRITER SLOGAN
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
// 10. ANIMATED JS COUNTERS & LIVE CLOCK & SIMULATED REAL-TIME TPS
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
// 11. REAL-TIME AUDIT STREAM TICKER
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
// 12. 3D TILT EFFECT TRÊN TILT CARD
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
// 13. CANVAS PARTICLES & CONFETTI & SPARKLES
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

      ctx.beginPath()
      ctx.arc(p.x, p.y, p.radius, 0, Math.PI * 2)
      ctx.fillStyle = particleColor + p.baseAlpha + ')'
      ctx.fill()

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
// 14. LOGIC NGHIỆP VỤ (BẢO TOÀN 100% TỪ GỐC)
// =========================================================================
function switchMode(newMode) {
  playPopSound(440)
  mode.value = newMode
  stopOtpTimer()
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
    otpDigits.value = ['', '', '', '', '', '']
    if (forgotCaptchaRef.value) forgotCaptchaRef.value.reset()
  } else if (newMode === 'login') {
    if (loginCaptchaRef.value) loginCaptchaRef.value.reset()
  }
}

function fillDemoAccount() {
  playPopSound(480)
  loginForm.username = 'admin'
  loginForm.password = 'password'
  calcPasswordStrength()
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
  playPopSound(450)
  ElMessage.info({
    message: `Đang kết nối xác thực SSO với ${provider}... (Tính năng Enterprise)`,
    duration: 3000
  })
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

// Xử lý Đăng Nhập
async function handleLogin() {
  if (!loginForm.username || !loginForm.password) {
    ElMessage.warning('Vui lòng nhập đầy đủ tên đăng nhập và mật khẩu.')
    return
  }

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
      playSuccessFanfare()
      triggerConfetti()
      ElMessage.success('Đăng nhập thành công! Đang chuyển hướng vào hệ thống...')
      setTimeout(() => {
        router.push('/')
      }, 700)
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

// Xử lý Quên Mật Khẩu Bước 1: Gửi OTP
async function handleSendOtp() {
  if (!forgotForm.identifier || !forgotForm.identifier.trim()) {
    ElMessage.warning('Vui lòng nhập tên đăng nhập hoặc email!')
    return
  }

  if (forgotStep.value === 1 && forgotCaptchaRef.value && !forgotCaptchaRef.value.validate()) {
    ElMessage.warning('Vui lòng giải đúng phép tính để nhận mã OTP!')
    return
  }

  loading.value = true
  loadingText.value = forgotForm.channel === 'EMAIL'
    ? 'Đang gửi mã OTP qua Email...'
    : 'Đang gửi mã OTP qua Telegram...'
  loadingSubText.value = 'Hệ thống đang niêm phong mã bảo an và chuyển phát...'

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
    otpDigits.value = ['', '', '', '', '', '']
    forgotForm.otp = ''
    forgotStep.value = 2
    startOtpTimer()
    ElMessage.success(res.data?.message || 'Mã xác thực OTP đã được gửi thành công!')
  } catch (err) {
    ElMessage.error(err.response?.data?.message || 'Không tìm thấy tài khoản trong hệ thống!')
    if (forgotStep.value === 1 && forgotCaptchaRef.value) forgotCaptchaRef.value.reset()
  } finally {
    loading.value = false
  }
}

// Xử lý Quên Mật Khẩu Bước 2: Xác nhận OTP
async function handleVerifyOtp() {
  syncOtpToForm()
  if (!forgotForm.otp || forgotForm.otp.trim().length !== 6) {
    ElMessage.warning('Vui lòng nhập đầy đủ mã OTP 6 chữ số!')
    return
  }

  loading.value = true
  loadingText.value = 'Đang kiểm tra mã OTP...'
  loadingSubText.value = 'Xác minh tính hợp lệ và thời hạn niêm phong...'

  try {
    await api.post('/auth/verify-otp', {
      otp: forgotForm.otp.trim()
    })
    stopOtpTimer()
    playChimeSound()
    forgotStep.value = 3
    ElMessage.success('Xác thực OTP thành công! Vui lòng thiết lập mật mã mới.')
  } catch (err) {
    ElMessage.error(err.response?.data?.message || 'Mã OTP không chính xác hoặc đã hết hạn!')
  } finally {
    loading.value = false
  }
}

// Xử lý Quên Mật Khẩu Bước 3: Đặt lại mật khẩu mới
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
  loadingText.value = 'Đang cập nhật mật mã mới...'
  loadingSubText.value = 'Đang mã hóa và niêm phong vào sổ cái bảo mật...'

  try {
    await api.post('/auth/reset-password', {
      token: forgotForm.otp.trim(),
      newPassword: forgotForm.newPassword
    })
    playSuccessFanfare()
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
  calcPasswordStrength()
  switchMode('login')
}

// =========================================================================
// 15. LIFECYCLE HOOKS
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
  stopOtpTimer()
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
  background: rgba(255, 255, 255, 0.94);
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
  padding: 30px 36px;
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
  margin-bottom: 12px;
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
  margin: 0 0 16px;
  font-size: 12.5px;
  line-height: 1.6;
  color: #475569;
  max-width: 480px;
}

/* 3D Tilt Showcase Card */
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
   3. CỘT PHẢI: AUTH FORM PANEL (NỀN TRẮNG TINH KHIẾT)
   ========================================================================= */
.auth-card-panel {
  padding: 22px 32px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  background: #ffffff;
  color: #0f172a;
  overflow-y: auto;
}

.form-wrapper {
  max-width: 410px;
  width: 100%;
  margin: 0 auto;
}

/* Mascot Cyber Sentry SVG */
.mascot-sentry-wrap {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  margin-bottom: 6px;
}

.sentry-head {
  position: relative;
  width: 50px;
  height: 38px;
  background: #f1f5f9;
  border: 2px solid #cbd5e1;
  border-radius: 12px;
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
  gap: 9px;
}

.sentry-eye {
  position: relative;
  width: 13px;
  height: 13px;
  border-radius: 50%;
  background: #ffffff;
  border: 1.5px solid #94a3b8;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.sentry-pupil {
  width: 5.5px;
  height: 5.5px;
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
  transform: translateY(8px) scale(0.8);
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
  padding: 3px 8px;
  font-size: 10px;
  font-weight: 600;
  color: var(--primary-hover, #1d4ed8);
  max-width: 220px;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.03);
}

/* Header */
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
  padding: 2px 7px;
  border-radius: 6px;
  border: 1px solid #a7f3d0;
}

.form-sub {
  margin: 2px 0 10px;
  font-size: 12px;
  color: #64748b;
}

/* Thẻ Thử Nghiệm Demo Ledger */
.demo-ledger-box {
  background: #f8fafc;
  border: 1px dashed #cbd5e1;
  border-radius: 8px;
  padding: 8px 12px;
  margin-bottom: 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  cursor: pointer;
  transition: all 0.2s ease;
}

.demo-ledger-box:hover {
  background: #f1f5f9;
  border-color: var(--primary-color, #2563eb);
  transform: translateY(-1px);
}

.demo-ledger-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.demo-ledger-icon {
  font-size: 18px;
}

.demo-ledger-title {
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.5px;
  color: #475569;
}

.demo-ledger-cred {
  font-size: 11px;
  color: #1e293b;
}

.btn-fill-sample {
  border: 1px solid var(--primary-border, #bfdbfe);
  background: var(--primary-light, #eff6ff);
  color: var(--primary-hover, #1d4ed8);
  font-size: 10px;
  font-weight: 700;
  padding: 4px 8px;
  border-radius: 4px;
  cursor: pointer;
}

.btn-fill-sample:hover {
  background: #dbeafe;
}

/* Form Fields */
.auth-form {
  display: flex;
  flex-direction: column;
  gap: 9px;
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

.req {
  color: #ef4444;
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
  margin-top: 2px;
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
  margin: 1px 0;
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
  margin-top: 2px;
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

/* SSO Buttons */
.sso-divider-row {
  margin: 10px 0 8px;
  display: flex;
  align-items: center;
  text-align: center;
  color: #94a3b8;
  font-size: 9.5px;
  font-weight: 700;
  letter-spacing: 0.8px;
}

.sso-divider-row::before,
.sso-divider-row::after {
  content: '';
  flex: 1;
  border-bottom: 1px solid #e2e8f0;
}

.sso-divider-row span {
  padding: 0 8px;
}

.sso-btns-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}

.btn-sso-item {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  padding: 6px 10px;
  border-radius: 6px;
  font-size: 10.5px;
  font-weight: 700;
  color: #334155;
  cursor: pointer;
  transition: all 0.15s ease;
}

.btn-sso-item:hover {
  background: #f1f5f9;
  border-color: #cbd5e1;
  transform: translateY(-1px);
}

.form-footer-line {
  margin-top: 10px;
  text-align: center;
  font-size: 11px;
  color: #64748b;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.admin-contact-hint {
  font-weight: 600;
  color: var(--primary-color, #2563eb);
}

/* =========================================================================
   4. FORGOT PASSWORD WIZARD (CHUẨN GỐC GIAO DIỆN SÁNG)
   ========================================================================= */
.wizard-stepper {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
}

.wizard-step-node {
  display: flex;
  align-items: center;
  gap: 6px;
  opacity: 0.5;
  transition: opacity 0.2s ease;
}

.wizard-step-node.active {
  opacity: 1;
}

.step-num-circle {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: #e2e8f0;
  color: #475569;
  font-size: 10.5px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
}

.wizard-step-node.active .step-num-circle {
  background: var(--primary-color, #2563eb);
  color: #ffffff;
}

.step-node-label {
  font-size: 11px;
  font-weight: 700;
  color: #1e293b;
}

.step-divider-line {
  flex: 1;
  height: 2px;
  background: #e2e8f0;
  margin: 0 8px;
}

.step-divider-line.filled {
  background: var(--primary-color, #2563eb);
}

.wizard-step-body {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.btn-sample-inline {
  border: none;
  background: transparent;
  color: var(--primary-color, #2563eb);
  font-size: 11px;
  font-weight: 600;
  cursor: pointer;
}

.channel-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}

.channel-pill-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 8px;
  border: 1px solid #cbd5e1;
  border-radius: 6px;
  background: #f8fafc;
  font-size: 11px;
  font-weight: 600;
  color: #475569;
  cursor: pointer;
  transition: all 0.2s ease;
}

.channel-pill-btn.active {
  border-color: var(--primary-color, #2563eb);
  background: var(--primary-light, #eff6ff);
  color: var(--primary-hover, #1d4ed8);
  font-weight: 700;
}

.wizard-actions {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-top: 4px;
}

.btn-cancel-link {
  border: none;
  background: transparent;
  color: #64748b;
  font-size: 11.5px;
  font-weight: 600;
  padding: 4px;
  cursor: pointer;
}

.btn-cancel-link:hover {
  color: #0f172a;
}

/* OTP Step 2 */
.otp-target-card {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 8px 12px;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 11.5px;
  color: #334155;
}

.demo-otp-banner {
  background: #fef3c7;
  border: 1px dashed #f59e0b;
  padding: 6px 10px;
  border-radius: 6px;
  font-size: 11px;
  color: #92400e;
  cursor: pointer;
  text-align: center;
}

.otp-inputs-row {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  margin: 6px 0;
}

.otp-digit-box {
  width: 44px;
  height: 48px;
  text-align: center;
  font-size: 20px;
  font-weight: 800;
  color: #0f172a;
  border: 1.5px solid #cbd5e1;
  border-radius: 8px;
  background: #ffffff;
  outline: none;
  transition: all 0.15s ease;
}

.otp-digit-box:focus {
  border-color: var(--primary-color, #2563eb);
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.15);
}

.otp-mid-divider {
  font-size: 16px;
  font-weight: 700;
  color: #94a3b8;
}

.countdown-card {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 8px 12px;
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.countdown-text-line {
  display: flex;
  justify-content: space-between;
  font-size: 11.5px;
  color: #475569;
}

.countdown-progress-track {
  height: 4px;
  background: #e2e8f0;
  border-radius: 2px;
  overflow: hidden;
}

.countdown-progress-bar {
  height: 100%;
  background: var(--primary-color, #2563eb);
  transition: width 1s linear;
}

.resend-action-line {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 11px;
  color: #64748b;
}

.btn-resend-link {
  border: none;
  background: transparent;
  color: var(--primary-color, #2563eb);
  font-weight: 700;
  font-size: 10.5px;
  cursor: pointer;
}

.btn-resend-link:hover {
  text-decoration: underline;
}

/* Step 3 NIST Password Criteria */
.nist-tag {
  font-size: 9px;
  font-weight: 700;
  background: #ecfdf5;
  color: #059669;
  padding: 2px 6px;
  border-radius: 4px;
}

.pwd-criteria-panel {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 8px 12px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.criteria-header {
  display: flex;
  justify-content: space-between;
  font-size: 11px;
  color: #475569;
}

.criteria-bars-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 4px;
}

.criteria-bar-seg {
  height: 4px;
  background: #e2e8f0;
  border-radius: 2px;
}

.criteria-bar-seg.filled.str-weak { background: #ef4444; }
.criteria-bar-seg.filled.str-fair { background: #f59e0b; }
.criteria-bar-seg.filled.str-good { background: #3b82f6; }
.criteria-bar-seg.filled.str-strong { background: #10b981; }

.criteria-checklist {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 4px;
  font-size: 10.5px;
  color: #64748b;
}

.chk-item.passed {
  color: #059669;
  font-weight: 600;
}

.pwd-matched-ico {
  color: #10b981;
  font-weight: 800;
  font-size: 14px;
}

/* Step 4 Success */
.success-seal-circle {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: #10b981;
  color: #ffffff;
  font-size: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 14px auto 6px;
  box-shadow: 0 6px 18px rgba(16, 185, 129, 0.35);
}

.success-title {
  font-size: 18px;
  font-weight: 800;
  color: #0f172a;
  margin: 0;
}

.success-desc {
  font-size: 12px;
  color: #64748b;
  margin: 4px 0 16px;
}

.text-center {
  text-align: center;
}
</style>
