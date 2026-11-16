<template>
  <div class="settings-page-container">
    <!-- 1. HEADER SECTION -->
    <div class="settings-header-card">
      <div class="header-left">
        <div class="header-icon-box">
          <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="3" />
            <path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z" />
          </svg>
        </div>
        <div>
          <div class="header-title-row">
            <h1 class="header-title">Cài đặt Hệ thống &amp; Hồ sơ Quản trị</h1>
            <span class="settings-badge">ERP Enterprise v4.2</span>
          </div>
          <p class="header-subtitle">
            Trung tâm thiết lập cấu hình dịch vụ ngoài, quản lý bảo mật cá nhân và tham chiếu ma trận phân quyền vai trò (RBAC)
          </p>
        </div>
      </div>

      <div class="header-actions">
        <router-link to="/users" class="btn-goto-users">
          <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2" />
            <circle cx="9" cy="7" r="4" />
            <path d="M23 21v-2a4 4 0 0 0-3-3.87" />
            <path d="M16 3.13a4 4 0 0 1 0 7.75" />
          </svg>
          <span>Quản lý Tài khoản &amp; Ma trận RBAC</span>
        </router-link>
      </div>
    </div>

    <!-- 2. MAIN TABS -->
    <div class="settings-content-card">
      <el-tabs v-model="activeTab" class="settings-tabs">
        <!-- TAB 1: MA TRẬN PHÂN QUYỀN (RBAC MATRIX) -->
        <el-tab-pane label="Tổng quan Phân quyền (RBAC)" name="rbac">
          <div class="tab-pane-content">
            <div class="rbac-intro-box">
              <div class="intro-left">
                <h3>Kiến trúc Phân quyền Động 6 Vai trò (Dynamic RBAC Engine)</h3>
                <p>
                  Hệ thống áp dụng mô hình phân tách đặc quyền tối thiểu (Least Privilege). 
                  Mọi thao tác thêm/bớt quyền trực tiếp trên từng vai trò đều có hiệu lực thời gian thực ngay lập tức.
                </p>
              </div>
              <router-link to="/users" class="btn-action-primary">
                Tùy biến Ma trận Phân quyền →
              </router-link>
            </div>

            <!-- GRID 6 VAI TRÒ CHUẨN ENUM -->
            <div class="roles-cards-grid">
              <div class="role-card admin">
                <div class="rc-header">
                  <div>
                    <h4 class="rc-title">ADMIN</h4>
                    <span class="rc-sub">Quản trị viên tối cao</span>
                  </div>
                </div>
                <p class="rc-desc">Toàn quyền kiểm soát hệ thống, quản lý tài khoản, cấu hình cron jobs và cấp phát ma trận phân quyền.</p>
                <div class="rc-tags">
                  <span class="rc-tag">Toàn quyền</span>
                  <span class="rc-tag">Quản lý Tài khoản</span>
                  <span class="rc-tag">Cron Jobs</span>
                </div>
              </div>

              <div class="role-card manager">
                <div class="rc-header">
                  <div>
                    <h4 class="rc-title">MANAGER</h4>
                    <span class="rc-sub">Giám đốc / Quản lý</span>
                  </div>
                </div>
                <p class="rc-desc">Điều hành vận hành kinh doanh, phê duyệt đơn hàng, theo dõi báo cáo phân tích BI thời gian thực.</p>
                <div class="rc-tags">
                  <span class="rc-tag">Báo cáo BI</span>
                  <span class="rc-tag">Duyệt Đơn hàng</span>
                  <span class="rc-tag">Xem Nhân sự</span>
                </div>
              </div>

              <div class="role-card accountant">
                <div class="rc-header">
                  <div>
                    <h4 class="rc-title">ACCOUNTANT</h4>
                    <span class="rc-sub">Kế toán tài chính</span>
                  </div>
                </div>
                <p class="rc-desc">Đối soát dòng tiền, xác nhận thanh toán VietQR / chuyển khoản ngân hàng, xuất hóa đơn tài chính PDF/Excel.</p>
                <div class="rc-tags">
                  <span class="rc-tag">Xác nhận thanh toán</span>
                  <span class="rc-tag">Xuất hóa đơn</span>
                  <span class="rc-tag">Đối soát dòng tiền</span>
                </div>
              </div>

              <div class="role-card warehouse">
                <div class="rc-header">
                  <div>
                    <h4 class="rc-title">WAREHOUSE</h4>
                    <span class="rc-sub">Thủ kho / Kho vận</span>
                  </div>
                </div>
                <p class="rc-desc">Kiểm kê hàng hóa tồn kho, điều chỉnh số lượng xuất/nhập thực tế, tra cứu nhật ký luân chuyển kho.</p>
                <div class="rc-tags">
                  <span class="rc-tag">Điều chỉnh tồn kho</span>
                  <span class="rc-tag">Kiểm kê SKU</span>
                  <span class="rc-tag">Nhật ký kho</span>
                </div>
              </div>

              <div class="role-card sales">
                <div class="rc-header">
                  <div>
                    <h4 class="rc-title">SALES</h4>
                    <span class="rc-sub">Kinh doanh / Bán lẻ</span>
                  </div>
                </div>
                <p class="rc-desc">Tạo đơn hàng mới, tra cứu bảng giá sản phẩm, quản lý danh sách khách hàng và xuất báo cáo đơn hàng.</p>
                <div class="rc-tags">
                  <span class="rc-tag">Lên đơn hàng</span>
                  <span class="rc-tag">Tra cứu giá</span>
                  <span class="rc-tag">Xuất đơn</span>
                </div>
              </div>

              <div class="role-card user">
                <div class="rc-header">
                  <div>
                    <h4 class="rc-title">USER</h4>
                    <span class="rc-sub">Nhân viên nghiệp vụ</span>
                  </div>
                </div>
                <p class="rc-desc">Lên đơn hàng cơ bản, tra cứu tình trạng kho, trò chuyện nội bộ và tương tác với trợ lý ảo AI Copilot.</p>
                <div class="rc-tags">
                  <span class="rc-tag">Lên đơn hàng</span>
                  <span class="rc-tag">Tra cứu kho</span>
                  <span class="rc-tag">Chat &amp; AI</span>
                </div>
              </div>
            </div>

            <!-- BẢNG SO SÁNH MA TRẬN QUYỀN HẠN TỔNG QUÁT -->
            <div class="matrix-table-wrap">
              <h4 class="matrix-table-title">Bảng Năng lực Phân hệ Mặc định (Baseline Permission Capabilities)</h4>
              <div class="table-responsive">
                <table class="clean-matrix-table">
                  <thead>
                    <tr>
                      <th class="text-left">Phân hệ &amp; Tính năng</th>
                      <th class="text-center">ADMIN</th>
                      <th class="text-center">MANAGER</th>
                      <th class="text-center">ACCOUNTANT</th>
                      <th class="text-center">WAREHOUSE</th>
                      <th class="text-center">SALES</th>
                      <th class="text-center">USER</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr>
                      <td class="font-medium">Quản lý Tài khoản &amp; Phân quyền (/users)</td>
                      <td class="text-center text-green">✓ Toàn quyền</td>
                      <td class="text-center text-green">✓ Xem danh sách</td>
                      <td class="text-center text-muted">—</td>
                      <td class="text-center text-muted">—</td>
                      <td class="text-center text-muted">—</td>
                      <td class="text-center text-muted">—</td>
                    </tr>
                    <tr>
                      <td class="font-medium">Báo cáo &amp; Phân tích BI (/analytics)</td>
                      <td class="text-center text-green">✓ Toàn quyền</td>
                      <td class="text-center text-green">✓ Xem &amp; Xuất</td>
                      <td class="text-center text-green">✓ Xem thu chi</td>
                      <td class="text-center text-muted">—</td>
                      <td class="text-center text-muted">—</td>
                      <td class="text-center text-muted">—</td>
                    </tr>
                    <tr>
                      <td class="font-medium">Xác nhận Thanh toán &amp; VietQR (/orders)</td>
                      <td class="text-center text-green">✓ Có</td>
                      <td class="text-center text-green">✓ Có</td>
                      <td class="text-center text-green">✓ Chuyên trách</td>
                      <td class="text-center text-muted">—</td>
                      <td class="text-center text-muted">—</td>
                      <td class="text-center text-muted">—</td>
                    </tr>
                    <tr>
                      <td class="font-medium">Điều chỉnh &amp; Xuất Nhập Tồn kho (/products)</td>
                      <td class="text-center text-green">✓ Có</td>
                      <td class="text-center text-muted">—</td>
                      <td class="text-center text-muted">—</td>
                      <td class="text-center text-green">✓ Chuyên trách</td>
                      <td class="text-center text-muted">—</td>
                      <td class="text-center text-muted">—</td>
                    </tr>
                    <tr>
                      <td class="font-medium">Tác vụ Tự động Lập lịch (/jobs)</td>
                      <td class="text-center text-green">✓ Toàn quyền</td>
                      <td class="text-center text-green">✓ Xem &amp; Chạy</td>
                      <td class="text-center text-muted">—</td>
                      <td class="text-center text-muted">—</td>
                      <td class="text-center text-muted">—</td>
                      <td class="text-center text-muted">—</td>
                    </tr>
                    <tr>
                      <td class="font-medium">Tạo Đơn hàng &amp; Chat Real-time (/orders, /chat)</td>
                      <td class="text-center text-green">✓</td>
                      <td class="text-center text-green">✓</td>
                      <td class="text-center text-green">✓</td>
                      <td class="text-center text-green">✓</td>
                      <td class="text-center text-green">✓</td>
                      <td class="text-center text-green">✓</td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        </el-tab-pane>

        <!-- TAB 2: HỒ SƠ TÀI KHOẢN & ĐỔI MẬT KHẨU -->
        <el-tab-pane label="Hồ sơ &amp; Bảo mật" name="profile">
          <div class="tab-pane-content profile-pane">
            <div class="profile-layout-grid">
              <!-- KHỐI 1: THÔNG TIN HỒ SƠ -->
              <div class="profile-card">
                <div class="card-section-header">
                  <h3 class="section-title">Thông tin Hồ sơ Cá nhân</h3>
                  <span class="section-desc">Cập nhật thông tin hiển thị trên hệ thống và danh bạ nhân sự</span>
                </div>

                <div class="profile-avatar-row">
                  <img
                    :src="authStore.user?.avatarUrl || 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150'"
                    alt="Avatar"
                    class="profile-avatar-img"
                  />
                  <div class="profile-avatar-info">
                    <h3>{{ authStore.user?.fullName || authStore.user?.username }}</h3>
                    <div class="profile-meta-badges">
                      <span class="badge-role">{{ authStore.user?.role }}</span>
                      <span class="badge-status">Đang hoạt động</span>
                    </div>
                  </div>
                </div>

                <form class="profile-edit-form" @submit.prevent="saveProfile">
                  <div class="form-grid-2">
                    <div class="form-group">
                      <label>Tên đăng nhập</label>
                      <input type="text" :value="authStore.user?.username" disabled class="form-input disabled" />
                    </div>
                    <div class="form-group">
                      <label>Họ và tên</label>
                      <input v-model="profileForm.fullName" type="text" class="form-input" placeholder="Nhập họ và tên..." />
                    </div>
                    <div class="form-group">
                      <label>Email liên hệ</label>
                      <input v-model="profileForm.email" type="email" class="form-input" placeholder="admin@enterprise.vn" />
                    </div>
                    <div class="form-group">
                      <label>Số điện thoại</label>
                      <input v-model="profileForm.phone" type="text" class="form-input" placeholder="0987654321" />
                    </div>
                  </div>

                  <div class="form-group">
                    <label>Link ảnh đại diện (Avatar URL)</label>
                    <input v-model="profileForm.avatarUrl" type="text" class="form-input" placeholder="https://..." />
                  </div>

                  <div class="form-group">
                    <label>Tiểu sử / Ghi chú chức danh</label>
                    <textarea v-model="profileForm.bio" rows="3" class="form-textarea" placeholder="Mô tả chức danh, phòng ban làm việc..."></textarea>
                  </div>

                  <div class="form-actions-right">
                    <button type="submit" class="btn-action-primary" :disabled="savingProfile">
                      {{ savingProfile ? 'Đang lưu...' : 'Lưu thông tin hồ sơ' }}
                    </button>
                  </div>
                </form>
              </div>

              <!-- KHỐI 2: ĐỔI MẬT KHẨU TÀI KHOẢN -->
              <div class="profile-card password-card">
                <div class="card-section-header">
                  <h3 class="section-title">Đổi Mật khẩu Tài khoản</h3>
                  <span class="section-desc">Bảo vệ tài khoản bằng mật khẩu mạnh tối thiểu 6 ký tự</span>
                </div>

                <form class="password-edit-form" @submit.prevent="changePassword">
                  <div class="form-group">
                    <label>Mật khẩu hiện tại</label>
                    <input 
                      v-model="passwordForm.oldPassword" 
                      type="password" 
                      class="form-input" 
                      placeholder="Nhập mật khẩu hiện tại..." 
                      autocomplete="current-password"
                    />
                  </div>

                  <div class="form-group">
                    <label>Mật khẩu mới</label>
                    <input 
                      v-model="passwordForm.newPassword" 
                      type="password" 
                      class="form-input" 
                      placeholder="Tối thiểu 6 ký tự..." 
                      autocomplete="new-password"
                    />
                  </div>

                  <div class="form-group">
                    <label>Xác nhận mật khẩu mới</label>
                    <input 
                      v-model="passwordForm.confirmPassword" 
                      type="password" 
                      class="form-input" 
                      placeholder="Nhập lại mật khẩu mới..." 
                      autocomplete="new-password"
                    />
                  </div>

                  <div class="password-notice-box">
                    <span>Sau khi đổi mật khẩu thành công, phiên đăng nhập hiện tại vẫn được duy trì an toàn.</span>
                  </div>

                  <div class="form-actions-right">
                    <button type="submit" class="btn-action-primary" :disabled="changingPassword">
                      {{ changingPassword ? 'Đang xử lý...' : 'Cập nhật mật khẩu' }}
                    </button>
                  </div>
                </form>
              </div>
            </div>
          </div>
        </el-tab-pane>

        <!-- TAB 3: TÍCH HỢP & DỊCH VỤ NGOÀI -->
        <el-tab-pane label="Tích hợp &amp; Dịch vụ ngoài" name="integrations">
          <div class="tab-pane-content integrations-pane">
            <div class="integrations-grid">
              <!-- CARD 1: VIETQR NAPAS -->
              <div class="integration-card">
                <div class="int-top">
                  <div class="int-tag-pill">NAPAS 24/7</div>
                  <div>
                    <h4>Cổng VietQR Chuẩn NAPAS 24/7</h4>
                    <span class="int-status active">Đang kích hoạt</span>
                  </div>
                </div>
                <p>Tự động sinh mã QuickLink NAPAS kèm mã đơn hàng. Hỗ trợ webhook xác thực thanh toán tức thời.</p>
                <div class="int-specs">
                  <span>Ngân hàng thụ hưởng: <strong>MB Bank (Quân Đội)</strong></span>
                  <span>Số tài khoản: <strong>0987654321</strong></span>
                  <span>Chủ tài khoản: <strong>CONG TY ERP CLOUD ENTERPRISE</strong></span>
                  <span>Webhook URL: <code>/api/orders/payment-webhook</code></span>
                </div>
                <div class="card-footer-actions">
                  <button type="button" class="btn-int-action" @click="showSampleQrModal = true">
                    Xem mã QR mẫu
                  </button>
                </div>
              </div>

              <!-- CARD 2: TELEGRAM BOT -->
              <div class="integration-card">
                <div class="int-top">
                  <div class="int-tag-pill">TELEGRAM</div>
                  <div>
                    <h4>Telegram Bot Notification</h4>
                    <span class="int-status active">Đang kích hoạt</span>
                  </div>
                </div>
                <p>Tự động phát thông báo khi có đơn hàng mới, cảnh báo tồn kho an toàn và mã xác thực OTP.</p>
                <div class="int-specs">
                  <div class="telegram-input-group">
                    <label>Chat ID kiểm thử:</label>
                    <input 
                      v-model="telegramChatId" 
                      type="text" 
                      class="form-input-sm" 
                      placeholder="VD: 123456789" 
                    />
                  </div>
                  <span>Trạng thái kết nối: <strong>Sẵn sàng gửi tin</strong></span>
                </div>
                <div class="card-footer-actions">
                  <button 
                    type="button" 
                    class="btn-int-action primary" 
                    :disabled="testingTelegram" 
                    @click="handleTestTelegram"
                  >
                    {{ testingTelegram ? 'Đang gửi...' : 'Gửi tin nhắn test' }}
                  </button>
                </div>
              </div>

              <!-- CARD 3: SPRING TASK SCHEDULER -->
              <div class="integration-card">
                <div class="int-top">
                  <div class="int-tag-pill">CRON ENGINE</div>
                  <div>
                    <h4>Spring TaskScheduler Engine</h4>
                    <span class="int-status active">Đang kích hoạt</span>
                  </div>
                </div>
                <p>Bộ điều phối tác vụ ngầm tự động trong bộ nhớ RAM, hỗ trợ cấu hình biểu thức Cron linh hoạt.</p>
                <div class="int-specs">
                  <span>Tác vụ kích hoạt: <strong>3 Cron Jobs</strong></span>
                  <span>Nhật ký gần nhất: <strong>Thành công 100%</strong></span>
                  <span>Độ trễ hệ thống: <strong>&lt; 5ms</strong></span>
                </div>
                <div class="card-footer-actions">
                  <router-link to="/jobs" class="btn-int-action">
                    Mở Quản lý Tác vụ (/jobs) →
                  </router-link>
                </div>
              </div>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>

    <!-- MODAL PREVIEW MÃ VIETQR MẪU -->
    <el-dialog
      v-model="showSampleQrModal"
      title="Mã VietQR Demo (NAPAS 24/7)"
      width="420px"
      center
      append-to-body
    >
      <div class="qr-sample-dialog-content">
        <div class="qr-image-wrapper">
          <img 
            src="https://img.vietqr.io/image/MB-0987654321-compact2.png?amount=500000&addInfo=DEMO_TEST&accountName=CONG%20TY%20ERP%20CLOUD%20ENTERPRISE" 
            alt="VietQR Sample"
            class="sample-qr-img" 
          />
        </div>
        <div class="qr-info-note">
          <p><strong>MB Bank</strong> • 0987654321</p>
          <p class="account-name">CONG TY ERP CLOUD ENTERPRISE</p>
          <p class="qr-tip">Khách hàng quét mã qua ứng dụng ngân hàng bất kỳ để hoàn tất thanh toán.</p>
        </div>
      </div>
      <template #footer>
        <button type="button" class="btn-action-primary" @click="showSampleQrModal = false">
          Đóng cửa sổ
        </button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '../store/auth'
import api from '../api'

const authStore = useAuthStore()
const activeTab = ref('rbac')

// Profile State
const savingProfile = ref(false)
const profileForm = reactive({
  fullName: '',
  email: '',
  phone: '',
  bio: '',
  avatarUrl: ''
})

// Password State
const changingPassword = ref(false)
const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

// Integrations State
const showSampleQrModal = ref(false)
const testingTelegram = ref(false)
const telegramChatId = ref(localStorage.getItem('erp_telegram_test_chat_id') || '')

onMounted(() => {
  if (authStore.user) {
    profileForm.fullName = authStore.user.fullName || ''
    profileForm.email = authStore.user.email || ''
    profileForm.phone = authStore.user.phone || ''
    profileForm.bio = authStore.user.bio || ''
    profileForm.avatarUrl = authStore.user.avatarUrl || ''
  }
})

// Lưu cập nhật hồ sơ
async function saveProfile() {
  savingProfile.value = true
  try {
    const res = await authStore.updateProfile(profileForm)
    if (res.success) {
      ElMessage.success('Cập nhật hồ sơ tài khoản thành công!')
    } else {
      ElMessage.error(res.message || 'Không thể cập nhật hồ sơ!')
    }
  } catch (err) {
    ElMessage.error(err.response?.data?.message || 'Không thể cập nhật hồ sơ!')
  } finally {
    savingProfile.value = false
  }
}

// Đổi mật khẩu tài khoản
async function changePassword() {
  if (!passwordForm.oldPassword) {
    ElMessage.warning('Vui lòng nhập mật khẩu hiện tại!')
    return
  }
  if (!passwordForm.newPassword || passwordForm.newPassword.length < 6) {
    ElMessage.warning('Mật khẩu mới phải có tối thiểu 6 ký tự!')
    return
  }
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    ElMessage.warning('Mật khẩu xác nhận không khớp!')
    return
  }

  changingPassword.value = true
  try {
    const res = await api.put('/auth/change-password', {
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword
    })
    ElMessage.success(res.data?.message || 'Đổi mật khẩu tài khoản thành công!')
    passwordForm.oldPassword = ''
    passwordForm.newPassword = ''
    passwordForm.confirmPassword = ''
  } catch (err) {
    ElMessage.error(err.response?.data?.message || 'Đổi mật khẩu thất bại!')
  } finally {
    changingPassword.value = false
  }
}

// Gửi tin nhắn test Telegram
async function handleTestTelegram() {
  testingTelegram.value = true
  if (telegramChatId.value) {
    localStorage.setItem('erp_telegram_test_chat_id', telegramChatId.value.trim())
  }
  try {
    const res = await api.post('/auth/test-telegram', {
      chatId: telegramChatId.value.trim()
    })
    ElMessage.success(res.data?.message || 'Đã gửi tin nhắn test tới Telegram thành công!')
  } catch (err) {
    ElMessage.error(err.response?.data?.message || 'Lỗi khi gửi tin nhắn Telegram!')
  } finally {
    testingTelegram.value = false
  }
}
</script>

<style scoped>
.settings-page-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding-bottom: 64px;
}

/* 1. Header Card */
.settings-header-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
  background: #ffffff;
  border-radius: 12px;
  padding: 18px 24px;
  border: 1px solid #e2e8f0;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 14px;
}

.header-icon-box {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  background: #eff6ff;
  color: #2563eb;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.header-title-row {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.header-title {
  font-size: 18px;
  font-weight: 700;
  color: #0f172a;
  margin: 0;
}

.settings-badge {
  background: #f1f5f9;
  color: #475569;
  font-size: 11px;
  font-weight: 600;
  padding: 3px 8px;
  border-radius: 6px;
}

.header-subtitle {
  font-size: 13px;
  color: #64748b;
  margin: 4px 0 0 0;
}

.btn-goto-users {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  background: #eff6ff;
  color: #2563eb;
  border: 1px solid #bfdbfe;
  padding: 8px 16px;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 600;
  text-decoration: none;
  transition: all 0.2s ease;
}
.btn-goto-users:hover {
  background: #2563eb;
  color: #ffffff;
}

/* 2. Content Card & Tabs */
.settings-content-card {
  background: #ffffff;
  border-radius: 12px;
  padding: 16px 20px;
  border: 1px solid #e2e8f0;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

.tab-pane-content {
  padding: 16px 4px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* RBAC INTRO */
.rbac-intro-box {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  padding: 16px 20px;
  border-radius: 10px;
}

.intro-left h3 {
  font-size: 15px;
  font-weight: 700;
  color: #0f172a;
  margin: 0 0 6px 0;
}
.intro-left p {
  font-size: 13px;
  color: #475569;
  margin: 0;
  line-height: 1.5;
}

.btn-action-primary {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background: #2563eb;
  color: #ffffff;
  border: none;
  padding: 9px 18px;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 600;
  text-decoration: none;
  cursor: pointer;
  transition: background 0.2s ease;
}
.btn-action-primary:hover:not(:disabled) {
  background: #1d4ed8;
}

/* ROLES CARDS GRID */
.roles-cards-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.role-card {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}
.role-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.06);
}

.rc-header {
  display: flex;
  align-items: center;
  gap: 10px;
}
.rc-title {
  font-size: 14px;
  font-weight: 700;
  color: #0f172a;
  margin: 0;
}
.rc-sub {
  font-size: 11px;
  color: #64748b;
}

.rc-desc {
  font-size: 12px;
  color: #475569;
  line-height: 1.45;
  margin: 0;
  flex: 1;
}

.rc-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
.rc-tag {
  background: #f1f5f9;
  color: #334155;
  font-size: 11px;
  padding: 2px 7px;
  border-radius: 4px;
}

/* MATRIX TABLE */
.matrix-table-wrap {
  margin-top: 8px;
}
.matrix-table-title {
  font-size: 14px;
  font-weight: 700;
  color: #0f172a;
  margin: 0 0 12px 0;
}

.clean-matrix-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}
.clean-matrix-table th {
  background: #f8fafc;
  color: #475569;
  font-weight: 600;
  padding: 10px 12px;
  border: 1px solid #e2e8f0;
}
.clean-matrix-table td {
  padding: 10px 12px;
  border: 1px solid #f1f5f9;
}
.clean-matrix-table tbody tr:hover {
  background: #f8fafc;
}
.text-green { color: #16a34a; font-weight: 600; }
.text-muted { color: #94a3b8; }

/* PROFILE LAYOUT GRID */
.profile-layout-grid {
  display: grid;
  grid-template-columns: 1.2fr 0.8fr;
  gap: 20px;
}

.profile-card {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 20px;
}

.card-section-header {
  margin-bottom: 18px;
  border-bottom: 1px solid #f1f5f9;
  padding-bottom: 10px;
}
.section-title {
  font-size: 15px;
  font-weight: 700;
  color: #0f172a;
  margin: 0 0 4px 0;
}
.section-desc {
  font-size: 12px;
  color: #64748b;
}

.profile-avatar-row {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 20px;
}
.profile-avatar-img {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid #e2e8f0;
}
.profile-avatar-info h3 {
  font-size: 16px;
  font-weight: 700;
  color: #0f172a;
  margin: 0 0 4px 0;
}
.profile-meta-badges {
  display: flex;
  gap: 8px;
}
.badge-role {
  background: #eff6ff;
  color: #2563eb;
  font-size: 11px;
  font-weight: 700;
  padding: 2px 8px;
  border-radius: 4px;
}
.badge-status {
  background: #ecfdf5;
  color: #059669;
  font-size: 11px;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 4px;
}

.form-grid-2 {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 5px;
  margin-bottom: 14px;
}
.form-group label {
  font-size: 12px;
  font-weight: 600;
  color: #475569;
}
.form-input, .form-textarea {
  padding: 8px 12px;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  font-size: 13px;
  color: #0f172a;
  outline: none;
  transition: border-color 0.2s;
}
.form-input:focus, .form-textarea:focus {
  border-color: #2563eb;
}
.form-input.disabled {
  background: #f1f5f9;
  color: #64748b;
  cursor: not-allowed;
}

.form-input-sm {
  padding: 6px 10px;
  border: 1px solid #cbd5e1;
  border-radius: 6px;
  font-size: 12px;
  color: #0f172a;
  outline: none;
  width: 100%;
}
.form-input-sm:focus {
  border-color: #2563eb;
}

.password-notice-box {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 10px 12px;
  font-size: 12px;
  color: #475569;
  line-height: 1.4;
  margin-bottom: 14px;
}

.form-actions-right {
  display: flex;
  justify-content: flex-end;
  margin-top: 6px;
}

/* INTEGRATIONS */
.integrations-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}
.integration-card {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 18px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.int-top {
  display: flex;
  align-items: center;
  gap: 12px;
}
.int-tag-pill {
  padding: 4px 8px;
  background: #f1f5f9;
  color: #475569;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.03em;
}

.int-top h4 {
  font-size: 14px;
  font-weight: 700;
  color: #0f172a;
  margin: 0;
}
.int-status.active {
  font-size: 11px;
  color: #16a34a;
  font-weight: 600;
}
.integration-card p {
  font-size: 12px;
  color: #475569;
  line-height: 1.45;
  margin: 0;
  flex: 1;
}
.int-specs {
  display: flex;
  flex-direction: column;
  gap: 6px;
  font-size: 11px;
  color: #64748b;
  border-top: 1px dashed #e2e8f0;
  padding-top: 10px;
}
.int-specs code {
  background: #f1f5f9;
  padding: 2px 5px;
  border-radius: 4px;
  color: #0f172a;
  font-size: 10px;
}
.telegram-input-group {
  display: flex;
  flex-direction: column;
  gap: 4px;
  margin-bottom: 4px;
}
.telegram-input-group label {
  font-size: 11px;
  font-weight: 600;
  color: #475569;
}

.card-footer-actions {
  margin-top: 6px;
  border-top: 1px solid #f1f5f9;
  padding-top: 10px;
}
.btn-int-action {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 8px 12px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 600;
  border: 1px solid #cbd5e1;
  background: #ffffff;
  color: #334155;
  cursor: pointer;
  text-decoration: none;
  transition: all 0.2s;
}
.btn-int-action:hover:not(:disabled) {
  background: #f8fafc;
  border-color: #94a3b8;
}
.btn-int-action.primary {
  background: #eff6ff;
  color: #2563eb;
  border-color: #bfdbfe;
}
.btn-int-action.primary:hover:not(:disabled) {
  background: #2563eb;
  color: #ffffff;
}

/* SAMPLE QR DIALOG */
.qr-sample-dialog-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 14px;
  text-align: center;
}
.qr-image-wrapper {
  background: #f8fafc;
  padding: 12px;
  border-radius: 12px;
  border: 1px solid #e2e8f0;
}
.sample-qr-img {
  width: 220px;
  height: auto;
  border-radius: 8px;
}
.qr-info-note p {
  margin: 3px 0;
  font-size: 13px;
  color: #0f172a;
}
.qr-info-note .account-name {
  font-weight: 700;
  color: #2563eb;
  font-size: 12px;
}
.qr-tip {
  font-size: 11px;
  color: #64748b;
  margin-top: 6px !important;
}

@media (max-width: 992px) {
  .roles-cards-grid,
  .integrations-grid,
  .profile-layout-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .form-grid-2 {
    grid-template-columns: 1fr;
  }
}
</style>
