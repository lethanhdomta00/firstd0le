<template>
  <div class="heritage-auth-page">
    <!-- Subtle botanical archival background texture -->
    <div class="heritage-bg-pattern"></div>

    <div class="heritage-auth-shell">
      <!-- ================================================================= -->
      <!-- MODE 1: ĐĂNG NHẬP (HERITAGE SPLIT CARD - CHUẨN ẢNH 3)             -->
      <!-- ================================================================= -->
      <div v-if="mode === 'login'" class="heritage-login-card">
        <BaseLoading
          :active="loading"
          :text="loadingText"
          :sub-text="loadingSubText"
        />

        <!-- CỘT TRÁI: THƯƠNG HIỆU & CHỨNG CHỈ BẢO MẬT (DESKTOP) -->
        <aside class="heritage-brand-col">
          <div class="brand-logo-header">
            <div class="heritage-logo-badge">
              <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M3 21h18M5 21V7l7-4 7 4v14M9 21v-8h6v8M9 9h.01M15 9h.01M9 13h.01M15 13h.01" />
              </svg>
            </div>
            <div>
              <div class="brand-title-serif">ERP CLOUD</div>
              <div class="brand-est-sub">ENTERPRISE • EST. 2026</div>
            </div>
          </div>

          <div class="brand-hero-content">
            <span class="heritage-kicker">ĐẶC QUYỀN QUẢN TRỊ</span>
            <h1 class="heritage-hero-title">
              Đột phá hiệu suất, kết nối tương lai doanh nghiệp.
            </h1>
            <p class="heritage-hero-desc">
              Hệ thống Quản trị Doanh nghiệp Toàn diện &amp; Bền vững. Kiến tạo chuẩn mực điều hành thông suốt trên nền tảng kỷ cương và uy tín lịch sử.
            </p>

            <div class="heritage-ornament-divider">
              <span>❖</span><span>❖</span><span>❖</span>
            </div>

            <div class="cert-white-card">
              <span class="cert-check-circle">✓</span>
              <div>
                <div class="cert-card-title">CHỨNG CHỈ CHUẨN MỰC</div>
                <div class="cert-card-sub">ISO/IEC 27001 • SLA Cam kết 99.98%</div>
              </div>
            </div>
          </div>

          <div class="archive-vault-banner">
            <div class="vault-tag">MẬT MÃ LƯU TRỮ • TẦNG III</div>
            <p class="vault-quote">
              “Kỷ luật trong kiểm soát, chuẩn xác trong từng chỉ số tài chính.”
            </p>
          </div>
        </aside>

        <!-- CỘT PHẢI: BIỂU MẪU ĐĂNG NHẬP (DESKTOP + MOBILE) -->
        <main class="heritage-form-col">
          <!-- Mobile Top App Bar & Centered Brand (chỉ hiện ở <= 900px, chuẩn Ảnh 1) -->
          <div class="mobile-auth-topbar">
            <span class="m-top-back">←</span>
            <div class="m-top-center">
              <span class="m-top-kicker">ERP SOVEREIGN CLOUD</span>
              <span class="m-top-title">Credential Sign In</span>
            </div>
            <span class="m-top-avatar">👤</span>
          </div>

          <div class="mobile-center-emblem">
            <div class="heritage-logo-badge mini">
              <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M3 21h18M5 21V7l7-4 7 4v14M9 21v-8h6v8" />
              </svg>
            </div>
            <div class="m-emblem-line">— ERP CLOUD • EST. 2026 —</div>
            <div class="m-emblem-sub">ARCHIVAL SOVEREIGN ACCESS</div>
          </div>

          <!-- Top Bar (Desktop) -->
          <div class="form-top-status-bar">
            <div class="portal-secure-label">
              <span class="square-dot"></span>
              <span>CỔNG XÁC THỰC BẢO AN</span>
            </div>
            <div class="lang-selector-pill">🌐 VIE ⌄</div>
          </div>

          <div class="form-main-body">
            <div class="form-heading-wrap">
              <div class="mobile-safe-kicker">✦ ỦY NHIỆM AN TOÀN ✦</div>
              <div class="heading-inline">
                <h2 class="serif-page-title">Đăng nhập</h2>
                <span class="heading-slash-sub">/ Định danh Tổ chức</span>
              </div>
              <p class="form-desc-txt">
                Nhập thông tin ủy nhiệm để truy cập sổ cái điều hành ERP Cloud.
              </p>
            </div>

            <!-- Thẻ thử nghiệm Demo Ledger -->
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

            <!-- Form Login -->
            <form @submit.prevent="handleLogin" class="heritage-form-stack">
              <div class="h-input-group">
                <label class="h-field-label">TÊN ĐĂNG NHẬP HOẶC EMAIL <span class="req">*</span></label>
                <div class="h-input-box">
                  <span class="h-input-icon">
                    <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2">
                      <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
                      <circle cx="12" cy="7" r="4"></circle>
                    </svg>
                  </span>
                  <input
                    v-model="loginForm.username"
                    type="text"
                    placeholder="admin@enterprisegroup.vn"
                    required
                    autocomplete="username"
                    class="h-native-input"
                  />
                </div>
              </div>

              <div class="h-input-group">
                <div class="h-label-between">
                  <label class="h-field-label">MẬT KHẨU BẢO AN <span class="req">*</span></label>
                  <button type="button" class="h-link-btn" @click="switchMode('forgot')">
                    Quên mật khẩu?
                  </button>
                </div>
                <div class="h-input-box">
                  <span class="h-input-icon">
                    <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2">
                      <rect x="3" y="11" width="18" height="11" rx="2" ry="2"></rect>
                      <path d="M7 11V7a5 5 0 0 1 10 0v4"></path>
                    </svg>
                  </span>
                  <input
                    v-model="loginForm.password"
                    :type="showPassword ? 'text' : 'password'"
                    placeholder="••••••••••••"
                    required
                    autocomplete="current-password"
                    class="h-native-input"
                  />
                  <button type="button" class="h-eye-btn" @click="showPassword = !showPassword" title="Hiện/ẩn mật khẩu">
                    <svg v-if="!showPassword" viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2">
                      <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path>
                      <circle cx="12" cy="12" r="3"></circle>
                    </svg>
                    <svg v-else viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2">
                      <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"></path>
                      <line x1="1" y1="1" x2="23" y2="23"></line>
                    </svg>
                  </button>
                </div>
              </div>

              <!-- Phép tính bảo vệ hệ thống -->
              <div class="h-captcha-wrapper">
                <BaseMathCaptcha
                  ref="loginCaptchaRef"
                  v-model="isLoginCaptchaPassed"
                  label="PHÉP TÍNH BẢO VỆ HỆ THỐNG"
                  :compact="true"
                />
              </div>

              <div class="h-remember-row">
                <label class="h-checkbox-wrap">
                  <input type="checkbox" v-model="rememberMe" class="h-chk" />
                  <span>Lưu phiên đăng nhập trên thiết bị ủy thác</span>
                </label>
              </div>

              <button type="submit" class="h-btn-primary" :disabled="loading">
                <span>{{ loading ? 'ĐANG XÁC THỰC...' : 'ĐĂNG NHẬP VÀO HỆ THỐNG' }}</span>
                <span class="arrow-icon">➔</span>
              </button>
            </form>

            <div class="h-divider-row">
              <span>PHƯƠNG THỨC ỦY QUYỀN DOANH NGHIỆP</span>
            </div>

            <div class="h-sso-grid">
              <button type="button" class="h-sso-btn" @click="handleSocialLogin('Google Workspace')">
                <svg viewBox="0 0 24 24" width="15" height="15">
                  <path fill="#4285F4" d="M23.745 12.27c0-.7-.06-1.4-.19-2.07H12v4.51h6.6c-.29 1.52-1.14 2.82-2.4 3.68v3.05h3.88c2.27-2.09 3.665-5.17 3.665-9.17z"/>
                  <path fill="#34A853" d="M12 24c3.24 0 5.95-1.08 7.93-2.91l-3.88-3.05c-1.08.72-2.45 1.16-4.05 1.16-3.12 0-5.77-2.1-6.72-4.93H1.24v3.15C3.26 21.41 7.33 24 12 24z"/>
                  <path fill="#FBBC05" d="M5.28 14.27c-.25-.72-.38-1.49-.38-2.27s.13-1.55.38-2.27V6.58H1.24C.45 8.16 0 9.98 0 12s.45 3.84 1.24 5.42l4.04-3.15z"/>
                  <path fill="#EA4335" d="M12 4.75c1.77 0 3.35.61 4.6 1.8l3.42-3.42C17.95 1.19 15.24 0 12 0 7.33 0 3.26 2.59 1.24 6.58l4.04 3.15c.95-2.83 3.6-4.98 6.72-4.98z"/>
                </svg>
                <span>GOOGLE WORKSPACE ID</span>
              </button>

              <button type="button" class="h-sso-btn" @click="handleSocialLogin('Microsoft Entra')">
                <svg viewBox="0 0 24 24" width="15" height="15">
                  <path fill="#F25022" d="M1 1h10v10H1z"/>
                  <path fill="#7FBA00" d="M13 1h10v10H13z"/>
                  <path fill="#00A4EF" d="M1 13h10v10H1z"/>
                  <path fill="#FFB900" d="M13 13h10v10H13z"/>
                </svg>
                <span>MICROSOFT ENTRA</span>
              </button>
            </div>
          </div>

          <div class="h-form-footer">
            <span>🔒 GIAO THỨC MÃ HÓA CẤP CHỨNG THƯ TLS 1.3</span>
            <span>© 2026 ENTERPRISE GROUP • QUYỀN HẠN ĐÃ ĐĂNG KÝ</span>
          </div>

          <div class="mobile-cert-footer">
            <div class="m-cert-line">✓ ISO/IEC 27001 CERTIFIED • 🛡️ SLA 99.98%</div>
            <div class="m-cert-diamonds">❖ ❖ ❖</div>
            <div class="m-cert-sub">MÃ HÓA CHỨNG THƯ TLS 1.3 • © 2026 ENTERPRISE GROUP</div>
          </div>
        </main>
      </div>

      <!-- ================================================================= -->
      <!-- MODE 2: KHÔI PHỤC MẬT KHẨU (ARCHIVAL DOCUMENT FRAME - ẢNH 4 & 5)   -->
      <!-- ================================================================= -->
      <div v-else-if="mode === 'forgot'" class="archival-outer-frame">
        <BaseLoading
          :active="loading"
          :text="loadingText"
          :sub-text="loadingSubText"
        />

        <!-- Mobile Top App Bar cho Forgot Password (chỉ hiện <= 900px) -->
        <div class="mobile-auth-topbar forgot-topbar">
          <button
            type="button"
            class="m-top-back-btn"
            @click="forgotStep > 1 && forgotStep < 4 ? (forgotStep -= 1) : switchMode('login')"
          >
            ←
          </button>
          <div class="m-top-center">
            <span class="m-top-title single">{{ forgotStep === 2 ? 'Two Factor Verification' : 'Passcode Recovery' }}</span>
          </div>
          <span class="m-top-avatar">👤</span>
        </div>

        <!-- Corner diamond ornaments -->
        <span class="corner-diamond tl">✦</span>
        <span class="corner-diamond tr">✦</span>
        <span class="corner-diamond bl">✦</span>
        <span class="corner-diamond br">✦</span>

        <div class="archival-inner-frame">
          <!-- Header inside Archival Frame -->
          <div class="archival-header">
            <div class="brand-logo-header">
              <div class="heritage-logo-badge mini">
                <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M3 21h18M5 21V7l7-4 7 4v14M9 21v-8h6v8" />
                </svg>
              </div>
              <div>
                <div class="brand-title-serif sm">ERP CLOUD ENTERPRISE</div>
                <div class="brand-est-sub">EST. 2026 • REGISTRY VAULT</div>
              </div>
            </div>
            <div class="lang-selector-pill">🌐 VIE ⌄</div>
          </div>

          <!-- 3-Step Roman Wizard -->
          <div v-if="forgotStep <= 3" class="roman-stepper">
            <div class="roman-step" :class="{ active: forgotStep >= 1, current: forgotStep === 1 }">
              <div class="roman-circle">
                <span v-if="forgotStep > 1">✓</span>
                <span v-else>I</span>
              </div>
              <span class="roman-label">I. TÀI KHOẢN</span>
            </div>

            <div class="roman-line" :class="{ filled: forgotStep >= 2 }"></div>

            <div class="roman-step" :class="{ active: forgotStep >= 2, current: forgotStep === 2 }">
              <div class="roman-circle">
                <span v-if="forgotStep > 2">✓</span>
                <span v-else>II</span>
              </div>
              <span class="roman-label">II. MÃ OTP</span>
            </div>

            <div class="roman-line" :class="{ filled: forgotStep >= 3 }"></div>

            <div class="roman-step" :class="{ active: forgotStep >= 3, current: forgotStep === 3 }">
              <div class="roman-circle">III</div>
              <span class="roman-label">III. MẬT MÃ MỚI</span>
            </div>
          </div>

          <div class="archival-ornament-bar">
            <span></span><i>❖</i><span></span>
          </div>

          <!-- BƯỚC 1: TÀI KHOẢN & KÊNH NHẬN OTP -->
          <div v-if="forgotStep === 1" class="archival-step-body">
            <div class="archival-title-group">
              <h2 class="serif-center-title">Khôi phục tài khoản</h2>
              <p class="archival-sub-txt">Nhập tên đăng nhập hoặc email để hệ thống gửi mã niêm phong OTP</p>
            </div>

            <div class="h-input-group">
              <div class="h-label-between">
                <label class="h-field-label">EMAIL HOẶC TÊN ĐĂNG NHẬP <span class="req">*</span></label>
                <button type="button" class="btn-fill-sample sm" @click="forgotForm.identifier = 'admin'">
                  ⚡ Điền mẫu
                </button>
              </div>
              <div class="h-input-box">
                <span class="h-input-icon">
                  <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2">
                    <rect x="2" y="4" width="20" height="16" rx="2"></rect>
                    <path d="M22 7l-8.97 5.7a1.94 1.94 0 0 1-2.06 0L2 7"></path>
                  </svg>
                </span>
                <input
                  v-model="forgotForm.identifier"
                  type="text"
                  placeholder="admin@enterprisegroup.vn"
                  class="h-native-input"
                  @keyup.enter="handleSendOtp"
                />
              </div>
            </div>

            <div class="h-input-group">
              <label class="h-field-label">PHƯƠNG THỨC TIẾP NHẬN MÃ OTP</label>
              <div class="channel-grid-2col">
                <button
                  type="button"
                  class="channel-card-btn"
                  :class="{ active: forgotForm.channel === 'EMAIL' }"
                  @click="forgotForm.channel = 'EMAIL'"
                >
                  <span>✉️</span>
                  <span>Email công ty</span>
                </button>
                <button
                  type="button"
                  class="channel-card-btn"
                  :class="{ active: forgotForm.channel === 'TELEGRAM' }"
                  @click="forgotForm.channel = 'TELEGRAM'"
                >
                  <span>✈️</span>
                  <span>Telegram Bot</span>
                </button>
              </div>
            </div>

            <div v-if="forgotForm.channel === 'EMAIL'" class="h-input-group">
              <label class="h-field-label">HỘP THƯ NHẬN OTP (TÙY CHỌN)</label>
              <div class="h-input-box">
                <input
                  v-model="forgotForm.email"
                  type="email"
                  placeholder="Mặc định: Email đã đăng ký của tài khoản"
                  class="h-native-input pl-3"
                  @keyup.enter="handleSendOtp"
                />
              </div>
            </div>

            <div v-if="forgotForm.channel === 'TELEGRAM'" class="h-input-group">
              <label class="h-field-label">TELEGRAM CHAT ID (TÙY CHỌN)</label>
              <div class="h-input-box">
                <input
                  v-model="forgotForm.telegramChatId"
                  type="text"
                  placeholder="Mặc định: Telegram Bot đã liên kết"
                  class="h-native-input pl-3"
                  @keyup.enter="handleSendOtp"
                />
              </div>
            </div>

            <div class="h-captcha-wrapper">
              <BaseMathCaptcha
                ref="forgotCaptchaRef"
                v-model="isForgotCaptchaPassed"
                label="GIẢI PHÉP TÍNH ĐỂ NHẬN MÃ OTP"
                :compact="true"
              />
            </div>

            <button
              type="button"
              class="h-btn-primary"
              :disabled="loading"
              @click="handleSendOtp"
            >
              <span>{{ loading ? 'ĐANG GỬI ĐIỆN TÍN...' : 'GỬI MÃ XÁC THỰC' }}</span>
              <span class="arrow-icon">➔</span>
            </button>

            <button type="button" class="h-back-link-btn" @click="switchMode('login')">
              ← QUAY LẠI TRANG ĐĂNG NHẬP
            </button>
          </div>

          <!-- BƯỚC 2: XÁC THỰC MÃ OTP (6 Ô TÁCH RỜI - CHUẨN ẢNH 4) -->
          <div v-else-if="forgotStep === 2" class="archival-step-body">
            <div class="archival-title-group">
              <h2 class="serif-center-title">Xác thực mã OTP</h2>
            </div>

            <div class="otp-target-banner">
              <span class="otp-mail-icon">✉️</span>
              <div class="otp-target-text">
                Mã 6 chữ số niêm phong gửi đến:<br />
                <strong>{{ serverTargetInfo || forgotForm.identifier || 'admin@enterprisegroup.vn' }}</strong>
              </div>
            </div>

            <!-- Nút điền nhanh nếu server trả OTP demo -->
            <div
              v-if="serverGeneratedOtp"
              class="otp-preview-pill"
              title="Nhấn để điền nhanh vào 6 ô"
              @click="fillPreviewOtp(serverGeneratedOtp)"
            >
              <span>⚡ Mã niêm phong xem trước: <strong>{{ serverGeneratedOtp }}</strong> (Click để tự điền)</span>
            </div>

            <!-- 6 Ô NHẬP OTP TÁCH RỜI (3 + • + 3) -->
            <div class="otp-boxes-row">
              <template v-for="(digit, idx) in otpDigits" :key="idx">
                <span v-if="idx === 3" class="otp-mid-dot">•</span>
                <input
                  :ref="(el) => setOtpInputRef(el, idx)"
                  v-model="otpDigits[idx]"
                  type="text"
                  inputmode="numeric"
                  maxlength="1"
                  class="otp-single-box"
                  @input="handleOtpDigitInput(idx, $event)"
                  @keydown="handleOtpKeyDown(idx, $event)"
                  @paste="handleOtpPaste"
                />
              </template>
            </div>

            <!-- Đồng hồ đếm ngược -->
            <div class="otp-countdown-block">
              <div class="countdown-text-row">
                <span>⏳ Hiệu lực niêm phong:</span>
                <strong>{{ formattedCountdown }}</strong>
              </div>
              <div class="countdown-track">
                <div class="countdown-fill" :style="{ width: countdownPercent + '%' }"></div>
              </div>
              <div class="resend-line">
                Chưa nhận được mã điện tín?
                <button type="button" class="resend-underline-btn" @click="handleSendOtp">GỬI LẠI MÃ</button>
              </div>
            </div>

            <div class="security-directive-box">
              <span class="shield-ico">🛡️</span>
              <p>
                <strong>Chỉ lệnh bảo an:</strong> <em>Mật mã niêm phong chỉ sử dụng một lần. Tuyệt đối không cung cấp cho bất kỳ ai, bao gồm cả Thanh tra viên hệ thống ERP.</em>
              </p>
            </div>

            <button
              type="button"
              class="h-btn-primary"
              :disabled="loading"
              @click="handleVerifyOtp"
            >
              <span>{{ loading ? 'ĐANG KIỂM TRA...' : 'XÁC NHẬN MÃ BẢO AN' }}</span>
              <span class="arrow-icon">➔</span>
            </button>

            <button type="button" class="h-back-link-btn" @click="forgotStep = 1">
              ← THAY ĐỔI PHƯƠNG THỨC XÁC THỰC HOẶC TÀI KHOẢN
            </button>
          </div>

          <!-- BƯỚC 3: THIẾT LẬP MẬT MÃ MỚI (CHUẨN ẢNH 5) -->
          <div v-else-if="forgotStep === 3" class="archival-step-body">
            <div class="archival-title-group">
              <h2 class="serif-center-title">Thiết lập Mật mã mới</h2>
              <p class="archival-sub-txt italic">
                Khởi tạo khóa bảo an mới cho ủy nhiệm tài khoản<br />
                <strong>{{ serverTargetInfo || forgotForm.identifier }}</strong>
              </p>
            </div>

            <div class="h-input-group">
              <div class="h-label-between">
                <label class="h-field-label">MẬT MÃ BẢO AN MỚI <span class="req">*</span></label>
                <span class="nist-hint">TỐI ƯU HÓA NIST 800-63</span>
              </div>
              <div class="h-input-box">
                <span class="h-input-icon">🔓</span>
                <input
                  v-model="forgotForm.newPassword"
                  :type="showNewPwd ? 'text' : 'password'"
                  placeholder="Nhập mật khẩu mới..."
                  class="h-native-input"
                />
                <button type="button" class="h-eye-btn" @click="showNewPwd = !showNewPwd">
                  {{ showNewPwd ? '🙈' : '👁️' }}
                </button>
              </div>
            </div>

            <!-- Thanh đo độ vững mạnh mật khẩu + 4 tiêu chí -->
            <div class="pwd-strength-box">
              <div class="pwd-strength-header">
                <span>Độ vững mạnh:</span>
                <strong :class="passwordStrengthInfo.colorClass">{{ passwordStrengthInfo.label }}</strong>
              </div>
              <div class="pwd-bars-4">
                <div
                  v-for="bar in 4"
                  :key="bar"
                  class="pwd-bar-seg"
                  :class="{ filled: passwordStrengthInfo.score >= bar, [passwordStrengthInfo.colorClass]: passwordStrengthInfo.score >= bar }"
                ></div>
              </div>
              <div class="pwd-criteria-grid">
                <div class="pwd-crit-item" :class="{ passed: pwdCriteria.minLength }">
                  <span>{{ pwdCriteria.minLength ? '✓' : '○' }}</span>
                  <span>Tối thiểu 8 ký tự</span>
                </div>
                <div class="pwd-crit-item" :class="{ passed: pwdCriteria.mixedCase }">
                  <span>{{ pwdCriteria.mixedCase ? '✓' : '○' }}</span>
                  <span>Chữ hoa &amp; chữ thường</span>
                </div>
                <div class="pwd-crit-item" :class="{ passed: pwdCriteria.hasNumber }">
                  <span>{{ pwdCriteria.hasNumber ? '✓' : '○' }}</span>
                  <span>Ký tự số học (0-9)</span>
                </div>
                <div class="pwd-crit-item" :class="{ passed: pwdCriteria.hasSpecial }">
                  <span>{{ pwdCriteria.hasSpecial ? '✓' : '○' }}</span>
                  <span>Ký tự đặc biệt bảo an</span>
                </div>
              </div>
            </div>

            <div class="h-input-group">
              <label class="h-field-label">XÁC NHẬN MẬT MÃ MỚI <span class="req">*</span></label>
              <div class="h-input-box">
                <span class="h-input-icon">🛡️</span>
                <input
                  v-model="forgotForm.confirmPassword"
                  :type="showConfirmPwd ? 'text' : 'password'"
                  placeholder="Nhập lại mật khẩu mới..."
                  class="h-native-input"
                  @keyup.enter="handleResetPassword"
                />
                <span v-if="forgotForm.confirmPassword && forgotForm.confirmPassword === forgotForm.newPassword" class="match-check-ico">✓</span>
              </div>
            </div>

            <div class="security-directive-box">
              <span class="shield-ico">🛡️</span>
              <p>
                <strong>CHỈ LỆNH BẢO AN:</strong> Sau khi hoàn tất, toàn bộ phiên truy cập cũ trên mọi thiết bị ủy thác sẽ được tự động thu hồi ngay lập tức để bảo vệ toàn vẹn dữ liệu số cái doanh nghiệp.
              </p>
            </div>

            <button
              type="button"
              class="h-btn-primary"
              :disabled="loading"
              @click="handleResetPassword"
            >
              <span>{{ loading ? 'ĐANG CẬP NHẬT...' : 'CẬP NHẬT MẬT MÃ BẢO AN' }}</span>
              <span class="arrow-icon">➔</span>
            </button>

            <button type="button" class="h-back-link-btn" @click="switchMode('login')">
              ↩ HỦY BỎ VÀ QUAY LẠI ĐĂNG NHẬP
            </button>
          </div>

          <!-- BƯỚC 4: HOÀN TẤT THÀNH CÔNG -->
          <div v-else-if="forgotStep === 4" class="archival-step-body text-center-box">
            <div class="success-seal-circle">✓</div>
            <h2 class="serif-center-title">Khôi phục thành công!</h2>
            <p class="archival-sub-txt">
              Mật mã bảo an mới cho tài khoản đã được niêm phong và lưu trữ an toàn vào sổ cái hệ thống.
            </p>
            <button type="button" class="h-btn-primary" @click="goToLoginAfterReset">
              <span>ĐĂNG NHẬP NGAY</span>
              <span class="arrow-icon">➔</span>
            </button>
          </div>

          <!-- Archival Bottom Footer -->
          <div class="archival-footer">
            <div class="footer-diamonds">❖ ❖ ❖</div>
            <div class="archival-footer-txt">
              MÃ HÓA CHỨNG THƯ TLS 1.3 • © 2026 ENTERPRISE GROUP • ARCHIVAL SOVEREIGN REGISTRY
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../store/auth'
import { ElMessage } from 'element-plus'
import api from '../api'
import BaseMathCaptcha from '../components/base/BaseMathCaptcha.vue'
import BaseLoading from '../components/base/BaseLoading.vue'

const router = useRouter()
const authStore = useAuthStore()

// =========================================================================
//                             STATE & REFS
// =========================================================================
const mode = ref('login') // 'login' | 'forgot'
const loading = ref(false)
const loadingText = ref('Đang xử lý...')
const loadingSubText = ref('Vui lòng đợi giây lát...')
const showPassword = ref(false)
const showNewPwd = ref(false)
const showConfirmPwd = ref(false)
const rememberMe = ref(true)

// Captcha refs
const isLoginCaptchaPassed = ref(false)
const isForgotCaptchaPassed = ref(false)
const loginCaptchaRef = ref(null)
const forgotCaptchaRef = ref(null)

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

// =========================================================================
//                         COMPUTED PROPERTIES
// =========================================================================
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
//                             METHODS
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
  const chars = String(code).trim().slice(0, 6).split('')
  for (let i = 0; i < 6; i++) {
    otpDigits.value[i] = chars[i] || ''
  }
  syncOtpToForm()
}

const handleOtpDigitInput = (idx, e) => {
  const val = (e.target.value || '').replace(/\D/g, '')
  otpDigits.value[idx] = val.slice(-1)
  syncOtpToForm()
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
  const focusIdx = Math.min(5, pasted.length)
  if (otpInputRefs.value[focusIdx]) {
    otpInputRefs.value[focusIdx].focus()
  }
}

function switchMode(newMode) {
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
    forgotStep.value = 3
    ElMessage.success('Xác thực OTP thành công! Vui lòng thiết lập mật mã mới.')
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
  loadingText.value = 'Đang cập nhật mật mã mới...'
  loadingSubText.value = 'Đang mã hóa và niêm phong vào sổ cái bảo mật...'

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

onBeforeUnmount(() => {
  stopOtpTimer()
})
</script>

<style scoped>
/* =========================================================================
   HERITAGE CLASSIC / BOTANICAL SERENITY AUTH DESIGN (COMPACT PC & MOBILE)
   ========================================================================= */
.heritage-auth-page {
  min-height: 100vh;
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #ecf0ea;
  padding: 12px 16px;
  position: relative;
  overflow-x: hidden;
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
  color: #1c2e24;
}

.heritage-bg-pattern {
  position: fixed;
  inset: 0;
  pointer-events: none;
  background-image:
    radial-gradient(circle at 12% 18%, rgba(30, 58, 47, 0.06) 0%, transparent 45%),
    radial-gradient(circle at 88% 82%, rgba(74, 112, 92, 0.08) 0%, transparent 45%);
}

.heritage-auth-shell {
  width: 100%;
  max-width: 980px;
  position: relative;
  z-index: 2;
  display: flex;
  justify-content: center;
}

/* Mobile-only elements (ẩn trên Desktop) */
.mobile-auth-topbar,
.mobile-center-emblem,
.mobile-safe-kicker,
.mobile-cert-footer {
  display: none;
}

/* =========================================================================
   MODE 1: LOGIN SPLIT CARD (GỌN TRỌN 1 MÀN HÌNH PC, KHÔNG SCROLL)
   ========================================================================= */
.heritage-login-card {
  width: 100%;
  background: #ffffff;
  border: 1px solid #d5ddd3;
  border-radius: 6px;
  box-shadow: 0 18px 42px rgba(22, 42, 32, 0.11);
  display: grid;
  grid-template-columns: 355px minmax(0, 1fr);
  overflow: hidden;
  position: relative;
}

/* LEFT PANEL */
.heritage-brand-col {
  background: #f2f5ef;
  border-right: 1px solid #dce3da;
  padding: 20px 22px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  gap: 14px;
}

.brand-logo-header {
  display: flex;
  align-items: center;
  gap: 10px;
}

.heritage-logo-badge {
  width: 34px;
  height: 34px;
  border-radius: 4px;
  background: #1d3c2d;
  color: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.heritage-logo-badge.mini {
  width: 30px;
  height: 30px;
}

.brand-title-serif {
  font-family: 'Playfair Display', 'Times New Roman', serif;
  font-size: 15px;
  font-weight: 700;
  letter-spacing: 0.4px;
  color: #1b3629;
}

.brand-title-serif.sm {
  font-size: 13.5px;
}

.brand-est-sub {
  font-size: 9.5px;
  font-weight: 700;
  letter-spacing: 0.9px;
  color: #63756a;
  margin-top: 1px;
}

.heritage-kicker {
  display: inline-block;
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 1.3px;
  color: #4f6759;
  margin-bottom: 6px;
}

.heritage-hero-title {
  font-family: 'Playfair Display', 'Times New Roman', serif;
  font-size: 22px;
  line-height: 1.28;
  font-weight: 700;
  color: #162e22;
  margin: 0 0 10px;
}

.heritage-hero-desc {
  font-size: 12px;
  line-height: 1.5;
  color: #4e6156;
  margin: 0 0 12px;
}

.heritage-ornament-divider {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  color: #8b9d92;
  font-size: 10px;
  margin: 6px 0 12px;
}

.heritage-ornament-divider::before,
.heritage-ornament-divider::after {
  content: '';
  flex: 1;
  height: 1px;
  background: #d5ded4;
}

.cert-white-card {
  background: #ffffff;
  border: 1px solid #d9e1d7;
  border-radius: 4px;
  padding: 9px 12px;
  display: flex;
  align-items: center;
  gap: 10px;
}

.cert-check-circle {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: #e3ece5;
  color: #1d3c2d;
  font-size: 11px;
  font-weight: 800;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.cert-card-title {
  font-size: 10.5px;
  font-weight: 800;
  letter-spacing: 0.4px;
  color: #1b3629;
}

.cert-card-sub {
  font-size: 10.5px;
  color: #5b6f63;
  margin-top: 1px;
}

.archive-vault-banner {
  background: linear-gradient(145deg, #274a39, #193226);
  color: #f4f7f2;
  border-radius: 4px;
  padding: 13px 14px;
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.1);
}

.vault-tag {
  font-size: 9px;
  font-weight: 700;
  letter-spacing: 1.1px;
  color: #b7cbbd;
  margin-bottom: 4px;
}

.vault-quote {
  margin: 0;
  font-family: 'Playfair Display', 'Times New Roman', serif;
  font-style: italic;
  font-size: 12px;
  line-height: 1.45;
  color: #ffffff;
}

/* RIGHT FORM PANEL */
.heritage-form-col {
  padding: 18px 30px 16px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  gap: 10px;
}

.form-top-status-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.portal-secure-label {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 9.5px;
  font-weight: 700;
  letter-spacing: 1px;
  color: #5d7065;
}

.square-dot {
  width: 5px;
  height: 5px;
  background: #1d3c2d;
  display: inline-block;
}

.lang-selector-pill {
  font-size: 10.5px;
  font-weight: 700;
  color: #294637;
  background: #f2f5ef;
  border: 1px solid #d5ded4;
  padding: 3px 9px;
  border-radius: 4px;
  white-space: nowrap;
}

.form-main-body {
  display: flex;
  flex-direction: column;
  gap: 9px;
}

.heading-inline {
  display: flex;
  align-items: baseline;
  gap: 8px;
  flex-wrap: wrap;
}

.serif-page-title {
  margin: 0;
  font-family: 'Playfair Display', 'Times New Roman', serif;
  font-size: 24px;
  font-weight: 700;
  color: #162e22;
}

.heading-slash-sub {
  font-family: 'Playfair Display', 'Times New Roman', serif;
  font-style: italic;
  font-size: 12.5px;
  color: #63756a;
}

.form-desc-txt {
  margin: 2px 0 0;
  font-size: 12px;
  color: #5b6e63;
}

/* Demo Ledger Card */
.demo-ledger-box {
  background: #f4f6f2;
  border: 1px solid #d4ddd3;
  border-radius: 4px;
  padding: 7px 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  cursor: pointer;
  transition: border-color 0.2s ease, background 0.2s ease;
}

.demo-ledger-box:hover {
  border-color: #1d3c2d;
  background: #ecf0e9;
}

.demo-ledger-left {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.demo-ledger-icon {
  width: 26px;
  height: 26px;
  border-radius: 4px;
  background: #1d3c2d;
  color: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  flex-shrink: 0;
}

.demo-ledger-title {
  font-size: 10px;
  font-weight: 800;
  letter-spacing: 0.5px;
  color: #1b3629;
}

.demo-ledger-cred {
  font-size: 11px;
  color: #4f6257;
}

.btn-fill-sample {
  background: #ffffff;
  color: #1d3c2d;
  border: 1px solid #c3d0c3;
  border-radius: 4px;
  padding: 4px 9px;
  font-size: 10px;
  font-weight: 800;
  letter-spacing: 0.4px;
  white-space: nowrap;
  cursor: pointer;
  flex-shrink: 0;
}

.btn-fill-sample:hover {
  background: #1d3c2d;
  color: #ffffff;
}

/* Inputs & Form */
.heritage-form-stack {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.h-input-group {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.h-label-between {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
}

.h-field-label {
  font-size: 10px;
  font-weight: 800;
  letter-spacing: 0.6px;
  color: #2f463a;
}

.req { color: #b91c1c; }

.h-link-btn {
  background: none;
  border: none;
  padding: 0;
  font-size: 11px;
  font-weight: 600;
  font-style: italic;
  color: #2b4c3b;
  text-decoration: underline;
  cursor: pointer;
}

.h-input-box {
  display: flex;
  align-items: center;
  height: 36px;
  background: #ffffff;
  border: 1px solid #cdd7cd;
  border-radius: 4px;
  padding: 0 10px;
  gap: 8px;
  transition: border-color 0.15s ease, box-shadow 0.15s ease;
}

.h-input-box:focus-within {
  border-color: #1d3c2d;
  box-shadow: 0 0 0 2px rgba(29, 60, 45, 0.12);
}

.h-input-icon {
  color: #6b7f73;
  display: flex;
  align-items: center;
  flex-shrink: 0;
}

.h-native-input {
  flex: 1;
  min-width: 0;
  border: none;
  outline: none;
  background: transparent;
  font-size: 13px;
  color: #162e22;
}

.h-native-input.pl-3 {
  padding-left: 4px;
}

.h-eye-btn {
  background: none;
  border: none;
  color: #6b7f73;
  cursor: pointer;
  padding: 4px;
  display: flex;
  align-items: center;
}

.h-captcha-wrapper {
  background: #f5f7f3;
  border: 1px solid #d9e1d7;
  border-radius: 4px;
  padding: 5px 8px;
}

.h-remember-row {
  display: flex;
  align-items: center;
}

.h-checkbox-wrap {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  font-size: 11.5px;
  color: #4a5e53;
  cursor: pointer;
}

.h-chk {
  width: 14px;
  height: 14px;
  accent-color: #1d3c2d;
  cursor: pointer;
}

.h-btn-primary {
  width: 100%;
  height: 38px;
  background: #1d3c2d;
  color: #ffffff;
  border: none;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.8px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  cursor: pointer;
  transition: background 0.18s ease;
}

.h-btn-primary:hover:not(:disabled) {
  background: #152b20;
}

.h-btn-primary:disabled {
  opacity: 0.65;
  cursor: not-allowed;
}

.h-divider-row {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 9px;
  font-weight: 700;
  letter-spacing: 0.9px;
  color: #77897e;
}

.h-divider-row::before,
.h-divider-row::after {
  content: '';
  flex: 1;
  height: 1px;
  background: #dde4dc;
}

.h-sso-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}

.h-sso-btn {
  height: 34px;
  background: #ffffff;
  border: 1px solid #cfd9cf;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.3px;
  color: #243b2f;
  cursor: pointer;
  white-space: nowrap;
  transition: background 0.15s ease;
}

.h-sso-btn:hover {
  background: #f4f6f2;
  border-color: #1d3c2d;
}

.h-form-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
  padding-top: 8px;
  border-top: 1px solid #e6ece5;
  font-size: 9px;
  font-weight: 600;
  color: #728479;
}

/* =========================================================================
   MODE 2: ARCHIVAL FORGOT PASSWORD FRAME (GỌN 1 MÀN HÌNH PC)
   ========================================================================= */
.archival-outer-frame {
  width: 100%;
  max-width: 520px;
  background: #ffffff;
  border: 1px solid #c5d1c6;
  padding: 8px;
  box-shadow: 0 18px 42px rgba(22, 42, 32, 0.12);
  position: relative;
}

.corner-diamond {
  position: absolute;
  font-size: 9px;
  color: #556d5f;
  line-height: 1;
  z-index: 3;
}

.corner-diamond.tl { top: 3px; left: 4px; }
.corner-diamond.tr { top: 3px; right: 4px; }
.corner-diamond.bl { bottom: 3px; left: 4px; }
.corner-diamond.br { bottom: 3px; right: 4px; }

.archival-inner-frame {
  border: 1px solid #d9e2d9;
  padding: 16px 24px 14px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.archival-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding-bottom: 10px;
  border-bottom: 1px solid #e5ece4;
}

/* Roman 3-step indicator */
.roman-stepper {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 2px 10px;
}

.roman-step {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.roman-circle {
  width: 26px;
  height: 26px;
  border-radius: 50%;
  background: #eef2ec;
  border: 1px solid #cbd6cc;
  color: #6b7f73;
  font-family: 'Playfair Display', 'Times New Roman', serif;
  font-size: 11px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
}

.roman-step.active .roman-circle {
  background: #dce8de;
  color: #1d3c2d;
  border-color: #9cb3a1;
}

.roman-step.current .roman-circle {
  background: #1d3c2d;
  color: #ffffff;
  border-color: #1d3c2d;
}

.roman-label {
  font-size: 9.5px;
  font-weight: 700;
  letter-spacing: 0.5px;
  color: #7c8e83;
  white-space: nowrap;
}

.roman-step.active .roman-label,
.roman-step.current .roman-label {
  color: #1d3c2d;
}

.roman-line {
  flex: 1;
  height: 1.5px;
  background: #dfe7df;
  margin-bottom: 16px;
}

.roman-line.filled {
  background: #1d3c2d;
}

.archival-ornament-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  color: #6e8376;
  font-size: 10px;
  margin: -2px 0;
}

.archival-ornament-bar span {
  flex: 1;
  height: 1px;
  background: #e4ebe3;
}

.archival-step-body {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.archival-title-group {
  text-align: center;
}

.serif-center-title {
  margin: 0;
  font-family: 'Playfair Display', 'Times New Roman', serif;
  font-size: 21px;
  font-weight: 700;
  color: #162e22;
}

.archival-sub-txt {
  margin: 3px 0 0;
  font-size: 11.5px;
  line-height: 1.4;
  color: #55695d;
}

.archival-sub-txt.italic {
  font-style: italic;
}

.channel-grid-2col {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}

.channel-card-btn {
  height: 35px;
  border: 1px solid #cbd6cc;
  border-radius: 4px;
  background: #f8faf7;
  color: #364e41;
  font-size: 12px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  cursor: pointer;
}

.channel-card-btn.active {
  background: #1d3c2d;
  border-color: #1d3c2d;
  color: #ffffff;
}

/* STEP 2 OTP STYLES */
.otp-target-banner {
  background: #f4f6f2;
  border: 1px solid #d6dfd6;
  border-radius: 4px;
  padding: 8px 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  font-size: 12px;
  color: #3e5448;
  text-align: left;
}

.otp-preview-pill {
  background: #ecfdf5;
  border: 1px dashed #6ee7b7;
  color: #047857;
  padding: 5px 10px;
  border-radius: 4px;
  font-size: 11.5px;
  text-align: center;
  cursor: pointer;
}

.otp-boxes-row {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  margin: 2px 0;
}

.otp-single-box {
  width: 44px;
  height: 46px;
  border: 1.5px solid #cbd6cc;
  border-radius: 4px;
  background: #ffffff;
  text-align: center;
  font-family: 'Playfair Display', 'Times New Roman', serif;
  font-size: 20px;
  font-weight: 700;
  color: #162e22;
  outline: none;
  transition: border-color 0.15s ease, box-shadow 0.15s ease;
}

.otp-single-box:focus {
  border-color: #1d3c2d;
  box-shadow: 0 0 0 2px rgba(29, 60, 45, 0.14);
}

.otp-mid-dot {
  font-size: 14px;
  color: #6b7f73;
  font-weight: 700;
}

.otp-countdown-block {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.countdown-text-row {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 11.5px;
  color: #465b4f;
}

.countdown-track {
  width: 170px;
  height: 4px;
  background: #dfe7df;
  border-radius: 2px;
  overflow: hidden;
}

.countdown-fill {
  height: 100%;
  background: #1d3c2d;
  transition: width 0.5s linear;
}

.resend-line {
  font-size: 11.5px;
  color: #5b6e63;
}

.resend-underline-btn {
  background: none;
  border: none;
  padding: 0;
  font-size: 11px;
  font-weight: 800;
  color: #1d3c2d;
  text-decoration: underline;
  cursor: pointer;
  margin-left: 4px;
}

.security-directive-box {
  background: #f4f6f2;
  border: 1px solid #d5ded4;
  border-radius: 4px;
  padding: 8px 10px;
  display: flex;
  align-items: flex-start;
  gap: 8px;
  font-size: 11px;
  line-height: 1.45;
  color: #465b4f;
}

.security-directive-box p {
  margin: 0;
}

.h-back-link-btn {
  background: none;
  border: none;
  color: #4c6255;
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.6px;
  cursor: pointer;
  text-align: center;
  padding: 2px;
}

.h-back-link-btn:hover {
  color: #162e22;
  text-decoration: underline;
}

/* STEP 3 PASSWORD STRENGTH */
.nist-hint {
  font-size: 9px;
  font-weight: 700;
  letter-spacing: 0.5px;
  color: #6b7f73;
}

.pwd-strength-box {
  background: #f8faf7;
  border: 1px solid #dde5dc;
  border-radius: 4px;
  padding: 8px 10px;
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.pwd-strength-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 10.5px;
  color: #54685d;
}

.pwd-bars-4 {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 6px;
}

.pwd-bar-seg {
  height: 4px;
  background: #dfe7df;
  border-radius: 2px;
}

.pwd-bar-seg.filled.str-weak { background: #dc2626; }
.pwd-bar-seg.filled.str-fair { background: #d97706; }
.pwd-bar-seg.filled.str-good { background: #2563eb; }
.pwd-bar-seg.filled.str-strong { background: #1d3c2d; }

.str-weak { color: #dc2626; }
.str-fair { color: #d97706; }
.str-good { color: #2563eb; }
.str-strong { color: #1d3c2d; }

.pwd-criteria-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 3px 8px;
}

.pwd-crit-item {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 10.5px;
  color: #7c8e83;
}

.pwd-crit-item.passed {
  color: #1d3c2d;
  font-weight: 600;
}

.match-check-ico {
  color: #059669;
  font-weight: 800;
  font-size: 14px;
}

.text-center-box {
  align-items: center;
  text-align: center;
}

.success-seal-circle {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: #1d3c2d;
  color: #ffffff;
  font-size: 22px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.archival-footer {
  padding-top: 8px;
  border-top: 1px solid #e5ece4;
  text-align: center;
}

.footer-diamonds {
  font-size: 8.5px;
  color: #829489;
  letter-spacing: 5px;
  margin-bottom: 4px;
}

.archival-footer-txt {
  font-size: 8.5px;
  font-weight: 700;
  letter-spacing: 0.6px;
  color: #77897e;
}

/* =========================================================================
   RESPONSIVE DESIGN (MOBILE CHUẨN SÁT ẢNH 1 - <= 900px / <= 540px)
   ========================================================================= */
@media (max-width: 900px) {
  .heritage-auth-page {
    padding: 0;
    align-items: flex-start;
    background: #eef2ec;
  }

  .heritage-auth-shell {
    max-width: 100%;
  }

  .heritage-login-card {
    grid-template-columns: 1fr;
    max-width: 480px;
    margin: 0 auto;
    border: none;
    border-radius: 0;
    background: transparent;
    box-shadow: none;
  }

  .heritage-brand-col,
  .form-top-status-bar,
  .h-form-footer {
    display: none;
  }

  .heritage-form-col {
    padding: 0 14px 20px;
    gap: 12px;
  }

  /* Mobile Top App Bar (y chang Ảnh 1) */
  .mobile-auth-topbar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 10px 4px;
    border-bottom: 1px solid #dde4dc;
  }

  .m-top-back,
  .m-top-back-btn {
    width: 30px;
    height: 30px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 16px;
    color: #1d3c2d;
    background: none;
    border: none;
    cursor: pointer;
  }

  .m-top-center {
    display: flex;
    flex-direction: column;
    align-items: center;
  }

  .m-top-kicker {
    font-size: 9px;
    font-weight: 700;
    letter-spacing: 1px;
    color: #5b6e63;
  }

  .m-top-title {
    font-family: 'Playfair Display', 'Times New Roman', serif;
    font-size: 15px;
    font-weight: 700;
    color: #162e22;
  }

  .m-top-title.single {
    font-size: 15.5px;
  }

  .m-top-avatar {
    width: 28px;
    height: 28px;
    border-radius: 50%;
    background: #1d3c2d;
    color: #ffffff;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 13px;
  }

  /* Mobile Emblem Header */
  .mobile-center-emblem {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 4px;
    margin: 2px 0;
  }

  .m-emblem-line {
    font-size: 10px;
    font-weight: 700;
    letter-spacing: 1.2px;
    color: #2f463a;
    margin-top: 2px;
  }

  .m-emblem-sub {
    font-size: 8.5px;
    letter-spacing: 1.4px;
    color: #6b7f73;
  }

  /* White Card on Mobile */
  .form-main-body {
    background: #ffffff;
    border: 1px solid #d5ddd3;
    border-radius: 6px;
    padding: 16px 14px;
    box-shadow: 0 8px 24px rgba(22, 42, 32, 0.06);
    gap: 10px;
  }

  .mobile-safe-kicker {
    display: block;
    text-align: center;
    font-size: 9.5px;
    font-weight: 700;
    letter-spacing: 1.2px;
    color: #5b6e63;
    margin-bottom: 2px;
  }

  .heading-inline {
    justify-content: center;
  }

  .heading-slash-sub {
    display: none;
  }

  .form-desc-txt {
    text-align: center;
  }

  .mobile-cert-footer {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 4px;
    padding: 4px 0 8px;
    text-align: center;
  }

  .m-cert-line {
    font-size: 10px;
    font-weight: 700;
    color: #3e5448;
  }

  .m-cert-diamonds {
    font-size: 9px;
    color: #829489;
    letter-spacing: 5px;
  }

  .m-cert-sub {
    font-size: 8.5px;
    font-weight: 600;
    color: #77897e;
  }

  /* Mobile Forgot Password Archival */
  .archival-outer-frame {
    max-width: 480px;
    margin: 0 auto;
    background: transparent;
    border: none;
    box-shadow: none;
    padding: 0 14px 20px;
  }

  .corner-diamond {
    display: none;
  }

  .forgot-topbar {
    margin-bottom: 8px;
  }

  .archival-inner-frame {
    border: none;
    padding: 0;
    gap: 10px;
  }

  .archival-header {
    padding-bottom: 8px;
  }

  .archival-step-body {
    background: #ffffff;
    border: 1px solid #d5ddd3;
    border-radius: 6px;
    padding: 16px 14px;
    box-shadow: 0 8px 24px rgba(22, 42, 32, 0.06);
  }
}

@media (max-width: 480px) {
  .demo-ledger-box {
    padding: 7px 10px;
  }

  .pwd-criteria-grid {
    grid-template-columns: 1fr;
  }

  .otp-boxes-row {
    gap: 4px;
  }

  .otp-single-box {
    width: calc((100% - 42px) / 6);
    max-width: 40px;
    height: 44px;
    font-size: 18px;
  }

  .otp-mid-dot {
    font-size: 12px;
  }
}
</style>
