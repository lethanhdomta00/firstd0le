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
            <h1 class="header-title">Cài đặt Hệ thống &amp; Phân quyền</h1>
            <span class="settings-badge">ERP Enterprise Suite</span>
          </div>
          <p class="header-subtitle">
            Trung tâm quản trị cấu hình, thiết lập hồ sơ người dùng và ma trận phân quyền vai trò (RBAC)
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
          <span>Quản lý Tài khoản &amp; Gán vai trò</span>
        </router-link>
      </div>
    </div>

    <!-- 2. MAIN TABS -->
    <div class="settings-content-card">
      <el-tabs v-model="activeTab" class="settings-tabs">
        <!-- TAB 1: MA TRẬN PHÂN QUYỀN (RBAC MATRIX) -->
        <el-tab-pane label="🛡️ Ma trận Phân quyền (RBAC)" name="rbac">
          <div class="tab-pane-content">
            <div class="rbac-intro-box">
              <div class="intro-left">
                <h3>Hệ thống Phân quyền Động 6 Vai trò (Dynamic Role Matrix)</h3>
                <p>
                  Hệ thống phân quyền ERP Cloud phân định trách nhiệm rõ ràng giữa các phòng ban. 
                  Để đổi vai trò hoặc cấp quyền cho một nhân viên cụ thể, bạn chỉ cần vào mục 
                  <router-link to="/users" class="text-link-bold">Quản lý Tài khoản (/users)</router-link>.
                </p>
              </div>
              <router-link to="/users" class="btn-action-primary">
                Đến bảng Quản lý Tài khoản →
              </router-link>
            </div>

            <!-- GRID 6 VAI TRÒ CHÍNH -->
            <div class="roles-cards-grid">
              <div class="role-card admin">
                <div class="rc-header">
                  <span class="rc-icon">👑</span>
                  <div>
                    <h4 class="rc-title">ADMIN</h4>
                    <span class="rc-sub">Quản trị viên tối cao</span>
                  </div>
                </div>
                <p class="rc-desc">Toàn quyền kiểm soát hệ thống, phân quyền người dùng, cấu hình thông báo và lập lịch cron jobs.</p>
                <div class="rc-tags">
                  <span class="rc-tag">Toàn quyền</span>
                  <span class="rc-tag">Quản lý User</span>
                  <span class="rc-tag">Cấu hình Cron</span>
                </div>
              </div>

              <div class="role-card director">
                <div class="rc-header">
                  <span class="rc-icon">🎖️</span>
                  <div>
                    <h4 class="rc-title">DIRECTOR</h4>
                    <span class="rc-sub">Giám đốc điều hành</span>
                  </div>
                </div>
                <p class="rc-desc">Xem toàn bộ số liệu báo cáo BI thời gian thực, phê duyệt đơn hàng chiến lược, giám sát vận hành.</p>
                <div class="rc-tags">
                  <span class="rc-tag">Báo cáo BI</span>
                  <span class="rc-tag">Duyệt đơn</span>
                  <span class="rc-tag">Giám sát toàn diện</span>
                </div>
              </div>

              <div class="role-card manager">
                <div class="rc-header">
                  <span class="rc-icon">💼</span>
                  <div>
                    <h4 class="rc-title">MANAGER</h4>
                    <span class="rc-sub">Trưởng phòng kinh doanh</span>
                  </div>
                </div>
                <p class="rc-desc">Quản lý danh sách đơn hàng, phê duyệt chính sách chiết khấu, điều phối đội ngũ nhân viên kinh doanh.</p>
                <div class="rc-tags">
                  <span class="rc-tag">Quản lý Đơn</span>
                  <span class="rc-tag">Duyệt chiết khấu</span>
                  <span class="rc-tag">Điều phối sales</span>
                </div>
              </div>

              <div class="role-card accountant">
                <div class="rc-header">
                  <span class="rc-icon">💰</span>
                  <div>
                    <h4 class="rc-title">ACCOUNTANT</h4>
                    <span class="rc-sub">Kế toán trưởng</span>
                  </div>
                </div>
                <p class="rc-desc">Đối soát dòng tiền, xác nhận thanh toán VietQR / chuyển khoản ngân hàng, xuất hóa đơn tài chính PDF.</p>
                <div class="rc-tags">
                  <span class="rc-tag">Xác nhận thanh toán</span>
                  <span class="rc-tag">Xuất hóa đơn</span>
                  <span class="rc-tag">Đối soát dòng tiền</span>
                </div>
              </div>

              <div class="role-card warehouse">
                <div class="rc-header">
                  <span class="rc-icon">📦</span>
                  <div>
                    <h4 class="rc-title">WAREHOUSE</h4>
                    <span class="rc-sub">Quản lý kho vận</span>
                  </div>
                </div>
                <p class="rc-desc">Kiểm kê hàng hóa tồn kho, điều chỉnh số lượng xuất/nhập thực tế, theo dõi lịch sử luân chuyển kho.</p>
                <div class="rc-tags">
                  <span class="rc-tag">Điều chỉnh tồn kho</span>
                  <span class="rc-tag">Kiểm kê SKU</span>
                  <span class="rc-tag">Nhật ký kho</span>
                </div>
              </div>

              <div class="role-card user">
                <div class="rc-header">
                  <span class="rc-icon">👤</span>
                  <div>
                    <h4 class="rc-title">USER</h4>
                    <span class="rc-sub">Nhân viên nghiệp vụ</span>
                  </div>
                </div>
                <p class="rc-desc">Tạo đơn hàng mới, tra cứu tình trạng kho, trò chuyện nội bộ và làm việc cùng trợ lý ảo AI Copilot.</p>
                <div class="rc-tags">
                  <span class="rc-tag">Lên đơn hàng</span>
                  <span class="rc-tag">Tra cứu kho</span>
                  <span class="rc-tag">Chat nội bộ</span>
                </div>
              </div>
            </div>

            <!-- BẢNG SO SÁNH MA TRẬN QUYỀN HẠN -->
            <div class="matrix-table-wrap">
              <h4 class="matrix-table-title">📊 Bảng Ma trận Quyền hạn Phân hệ (Role Capability Matrix)</h4>
              <div class="table-responsive">
                <table class="clean-matrix-table">
                  <thead>
                    <tr>
                      <th class="text-left">Phân hệ &amp; Tính năng</th>
                      <th class="text-center">ADMIN</th>
                      <th class="text-center">DIRECTOR</th>
                      <th class="text-center">MANAGER</th>
                      <th class="text-center">ACCOUNTANT</th>
                      <th class="text-center">WAREHOUSE</th>
                      <th class="text-center">USER</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr>
                      <td class="font-medium">Quản lý Tài khoản &amp; Phân quyền (/users)</td>
                      <td class="text-center text-green">✓ Toàn quyền</td>
                      <td class="text-center text-muted">—</td>
                      <td class="text-center text-muted">—</td>
                      <td class="text-center text-muted">—</td>
                      <td class="text-center text-muted">—</td>
                      <td class="text-center text-muted">—</td>
                    </tr>
                    <tr>
                      <td class="font-medium">Báo cáo &amp; Phân tích BI (/analytics)</td>
                      <td class="text-center text-green">✓ Toàn quyền</td>
                      <td class="text-center text-green">✓ Xem &amp; Xuất</td>
                      <td class="text-center text-green">✓ Xem doanh số</td>
                      <td class="text-center text-green">✓ Xem thu chi</td>
                      <td class="text-center text-muted">—</td>
                      <td class="text-center text-muted">—</td>
                    </tr>
                    <tr>
                      <td class="font-medium">Xác nhận Thanh toán &amp; VietQR (/orders)</td>
                      <td class="text-center text-green">✓ Có</td>
                      <td class="text-center text-green">✓ Có</td>
                      <td class="text-center text-green">✓ Có</td>
                      <td class="text-center text-green">✓ Chuyên trách</td>
                      <td class="text-center text-muted">—</td>
                      <td class="text-center text-muted">—</td>
                    </tr>
                    <tr>
                      <td class="font-medium">Điều chỉnh &amp; Xuất Nhập Tồn kho (/products)</td>
                      <td class="text-center text-green">✓ Có</td>
                      <td class="text-center text-muted">—</td>
                      <td class="text-center text-muted">—</td>
                      <td class="text-center text-muted">—</td>
                      <td class="text-center text-green">✓ Chuyên trách</td>
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

        <!-- TAB 2: HỒ SƠ TÀI KHOẢN -->
        <el-tab-pane label="👤 Hồ sơ &amp; Bảo mật" name="profile">
          <div class="tab-pane-content profile-pane">
            <div class="profile-card">
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
                  <label>Tiểu sử / Ghi chú phòng ban</label>
                  <textarea v-model="profileForm.bio" rows="3" class="form-textarea" placeholder="Mô tả chức danh, phòng ban làm việc..."></textarea>
                </div>

                <div class="form-actions-right">
                  <button type="submit" class="btn-action-primary" :disabled="savingProfile">
                    {{ savingProfile ? 'Đang lưu...' : 'Lưu thông tin hồ sơ' }}
                  </button>
                </div>
              </form>
            </div>
          </div>
        </el-tab-pane>

        <!-- TAB 3: TÍCH HỢP HỆ THỐNG -->
        <el-tab-pane label="⚡ Tích hợp &amp; Dịch vụ ngoài" name="integrations">
          <div class="tab-pane-content integrations-pane">
            <div class="integrations-grid">
              <div class="integration-card">
                <div class="int-top">
                  <span class="int-icon">⚡</span>
                  <div>
                    <h4>Cổng VietQR Chuẩn NAPAS 24/7</h4>
                    <span class="int-status active">Đang kích hoạt</span>
                  </div>
                </div>
                <p>Tự động sinh mã QuickLink NAPAS kèm mã đơn hàng. Hỗ trợ webhook xác thực thanh toán tức thời.</p>
                <div class="int-specs">
                  <span>Ngân hàng thụ hưởng: <strong>MB Bank (Quân Đội)</strong></span>
                  <span>Số tài khoản: <strong>0987654321</strong></span>
                </div>
              </div>

              <div class="integration-card">
                <div class="int-top">
                  <span class="int-icon">🤖</span>
                  <div>
                    <h4>Telegram Bot Notification</h4>
                    <span class="int-status active">Đang kích hoạt</span>
                  </div>
                </div>
                <p>Tự động phát thông báo khi có đơn hàng mới, cảnh báo tồn kho an toàn và báo cáo tổng hợp cuối ngày.</p>
                <div class="int-specs">
                  <span>Kênh tiếp nhận: <strong>ERP Management Channel</strong></span>
                  <span>Tần suất: <strong>Thời gian thực</strong></span>
                </div>
              </div>

              <div class="integration-card">
                <div class="int-top">
                  <span class="int-icon">⏱️</span>
                  <div>
                    <h4>Spring TaskScheduler Engine</h4>
                    <span class="int-status active">Đang kích hoạt</span>
                  </div>
                </div>
                <p>Bộ điều phối tác vụ ngầm tự động trong bộ nhớ RAM, hỗ trợ cấu hình biểu thức Cron linh hoạt.</p>
                <div class="int-specs">
                  <router-link to="/jobs" class="text-link-bold">Truy cập Quản lý Tác vụ (/jobs) →</router-link>
                </div>
              </div>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '../store/auth'
import api from '../api'

const authStore = useAuthStore()
const activeTab = ref('rbac')
const savingProfile = ref(false)

const profileForm = reactive({
  fullName: '',
  email: '',
  phone: '',
  bio: '',
  avatarUrl: ''
})

onMounted(() => {
  if (authStore.user) {
    profileForm.fullName = authStore.user.fullName || ''
    profileForm.email = authStore.user.email || ''
    profileForm.phone = authStore.user.phone || ''
    profileForm.bio = authStore.user.bio || ''
    profileForm.avatarUrl = authStore.user.avatarUrl || ''
  }
})

async function saveProfile() {
  savingProfile.value = true
  try {
    const res = await api.put('/users/profile', profileForm)
    authStore.user = { ...authStore.user, ...res.data }
    localStorage.setItem('user', JSON.stringify(authStore.user))
    ElMessage.success('Cập nhật hồ sơ tài khoản thành công!')
  } catch (err) {
    ElMessage.error(err.response?.data?.message || 'Không thể cập nhật hồ sơ!')
  } finally {
    savingProfile.value = false
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
  background: linear-gradient(135deg, #f0fdf4 0%, #ecfdf5 100%);
  border: 1px solid #bbf7d0;
  padding: 16px 20px;
  border-radius: 10px;
}

.intro-left h3 {
  font-size: 15px;
  font-weight: 700;
  color: #166534;
  margin: 0 0 6px 0;
}
.intro-left p {
  font-size: 13px;
  color: #15803d;
  margin: 0;
  line-height: 1.5;
}

.text-link-bold {
  color: #2563eb;
  font-weight: 600;
  text-decoration: underline;
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

.role-card.admin { border-top: 3px solid #ef4444; }
.role-card.director { border-top: 3px solid #3b82f6; }
.role-card.manager { border-top: 3px solid #8b5cf6; }
.role-card.accountant { border-top: 3px solid #0d9488; }
.role-card.warehouse { border-top: 3px solid #f59e0b; }
.role-card.user { border-top: 3px solid #64748b; }

.rc-header {
  display: flex;
  align-items: center;
  gap: 10px;
}
.rc-icon {
  font-size: 22px;
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

/* PROFILE PANE */
.profile-card {
  max-width: 680px;
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 24px;
}

.profile-avatar-row {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 24px;
}
.profile-avatar-img {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid #e2e8f0;
}
.profile-avatar-info h3 {
  font-size: 17px;
  font-weight: 700;
  color: #0f172a;
  margin: 0 0 6px 0;
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
  gap: 16px;
  margin-bottom: 16px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-bottom: 14px;
}
.form-group label {
  font-size: 12px;
  font-weight: 600;
  color: #475569;
}
.form-input, .form-textarea {
  padding: 9px 12px;
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

.form-actions-right {
  display: flex;
  justify-content: flex-end;
  margin-top: 10px;
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
.int-icon { font-size: 24px; }
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
  gap: 4px;
  font-size: 11px;
  color: #64748b;
  border-top: 1px dashed #e2e8f0;
  padding-top: 10px;
}

@media (max-width: 992px) {
  .roles-cards-grid,
  .integrations-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .roles-cards-grid,
  .integrations-grid,
  .form-grid-2 {
    grid-template-columns: 1fr;
  }
}
</style>
