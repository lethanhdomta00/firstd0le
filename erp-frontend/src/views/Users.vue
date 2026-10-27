<template>
  <div class="users-container">
    <!-- Reusable Base Page Header -->
    <BasePageHeader
      title="Quản lý Tài khoản (Users)"
      subtitle="Danh sách người dùng, phân quyền truy cập và kiểm soát trạng thái tài khoản"
      :icon="UserIcon"
    >
      <BaseButton type="gradient" size="large" :icon="Plus" @click="openCreateDialog()" class="btn-add">
        Thêm tài khoản mới
      </BaseButton>
    </BasePageHeader>

    <!-- Reusable Base Table with Built-in Skeleton Loading & Server-Side Pagination -->
    <BaseTable 
      :data="users" 
      :loading="loading" 
      :skeleton-columns="6" 
      :skeleton-rows="4"
      min-width="840px"
      :show-pagination="true"
      :pagination="{
        currentPage: page + 1,
        pageSize: size,
        total: totalElements
      }"
      @page-change="handlePageChange"
      @size-change="handleSizeChange"
    >
      <!-- Search & Filter Toolbar -->
      <template #toolbar>
        <div class="toolbar-wrap">
          <div class="filter-group">
            <!-- Keyword Search -->
            <el-input 
              v-model="filters.keyword" 
              placeholder="Tìm theo Username hoặc Email..." 
              clearable
              class="filter-search"
              @clear="onSearch"
              @keyup.enter="onSearch"
            >
              <template #prefix><el-icon><Search /></el-icon></template>
            </el-input>

            <!-- Role Filter -->
            <el-select 
              v-model="filters.role" 
              placeholder="Vai trò" 
              class="filter-select"
              @change="onFilterChange"
            >
              <el-option label="Tất cả vai trò" value="ALL" />
              <el-option label="Quản trị viên (ADMIN)" value="ADMIN" />
              <el-option label="Nhân viên (USER)" value="USER" />
            </el-select>

            <!-- Status Filter -->
            <el-select 
              v-model="filters.status" 
              placeholder="Trạng thái" 
              class="filter-select"
              @change="onFilterChange"
            >
              <el-option label="Tất cả trạng thái" value="ALL" />
              <el-option label="Đang hoạt động" value="ACTIVE" />
              <el-option label="Đã bị khóa" value="LOCKED" />
            </el-select>

            <!-- Reset Button -->
            <el-button @click="resetFilters" plain class="btn-reset">
              <el-icon><RefreshRight /></el-icon>
              <span>Đặt lại</span>
            </el-button>
          </div>

          <div class="count-tag">
            <span>Tổng số: <strong>{{ totalElements }}</strong> tài khoản</span>
          </div>
        </div>
      </template>

      <!-- ID Column -->
      <el-table-column prop="id" label="ID" width="70" align="center" />

      <!-- User Profile (Avatar + Username + Email) -->
      <el-table-column label="Thông tin tài khoản" min-width="220">
        <template #default="{ row }">
          <div class="user-cell">
            <el-avatar :size="36" class="user-cell-avatar">
              {{ (row.username || 'U').charAt(0).toUpperCase() }}
            </el-avatar>
            <div class="user-cell-info">
              <div class="user-name-row">
                <span class="user-name">{{ row.username }}</span>
                <el-tag v-if="row.username === currentUsername" size="small" type="success" effect="dark" class="current-badge">
                  Bạn
                </el-tag>
              </div>
              <span class="user-email">{{ row.email || 'Chưa cập nhật email' }}</span>
            </div>
          </div>
        </template>
      </el-table-column>

      <!-- Role Badge -->
      <el-table-column prop="role" label="Phân quyền (Role)" width="180" align="center">
        <template #default="{ row }">
          <BaseStatusBadge
            :type="row.role === 'ADMIN' ? 'danger' : 'info'"
            :text="row.role === 'ADMIN' ? 'Quản trị viên (ADMIN)' : 'Nhân viên (USER)'"
          />
        </template>
      </el-table-column>

      <!-- Account Status with Quick Toggle -->
      <el-table-column label="Trạng thái" width="160" align="center">
        <template #default="{ row }">
          <div class="status-cell">
            <BaseStatusBadge
              :type="row.status === 'LOCKED' ? 'danger' : 'success'"
              :text="row.status === 'LOCKED' ? 'Đã khóa' : 'Hoạt động'"
            />
            <el-button 
              size="small" 
              link 
              :type="row.status === 'LOCKED' ? 'success' : 'warning'"
              :disabled="row.username === currentUsername"
              @click="toggleUserStatus(row)"
              class="status-toggle-btn"
            >
              {{ row.status === 'LOCKED' ? 'Mở khóa' : 'Khóa' }}
            </el-button>
          </div>
        </template>
      </el-table-column>

      <!-- Created Date -->
      <el-table-column label="Ngày tạo" width="160" align="center">
        <template #default="{ row }">
          <span class="created-date">{{ formatDate(row.createdAt) }}</span>
        </template>
      </el-table-column>

      <!-- Action Buttons -->
      <el-table-column label="Hành động" width="210" align="center" fixed="right">
        <template #default="{ row }">
          <div class="action-btn-group">
            <el-button size="small" @click="openEditDialog(row)">Sửa</el-button>
            <el-button size="small" type="warning" plain @click="openResetPasswordDialog(row)">
              Đổi mật khẩu
            </el-button>
            <el-button 
              size="small" 
              type="danger" 
              plain 
              :disabled="row.username === currentUsername"
              @click="deleteUser(row)"
            >
              Xóa
            </el-button>
          </div>
        </template>
      </el-table-column>
    </BaseTable>

    <!-- Create User Dialog -->
    <BaseDialog 
      v-model="createDialogVisible"
      title="Thêm tài khoản mới" 
      subtitle="Tạo tài khoản người dùng mới và phân quyền truy cập hệ thống"
      width="460px"
      confirm-text="Tạo tài khoản"
      :loading="saving"
      @confirm="saveCreateUser"
    >
      <el-form :model="createForm" label-position="top">
        <el-form-item label="Tên đăng nhập *" required>
          <el-input v-model="createForm.username" placeholder="Nhập tên đăng nhập (ví dụ: staff01)" />
        </el-form-item>
        <el-form-item label="Mật khẩu *" required>
          <el-input 
            v-model="createForm.password" 
            type="password" 
            show-password 
            placeholder="Tối thiểu 6 ký tự" 
          />
        </el-form-item>
        <el-form-item label="Email">
          <el-input v-model="createForm.email" placeholder="Nhập địa chỉ email người dùng" />
        </el-form-item>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="Phân quyền *" required>
              <el-select v-model="createForm.role" style="width: 100%">
                <el-option label="ADMIN (Quản trị viên)" value="ADMIN" />
                <el-option label="USER (Nhân viên)" value="USER" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="Trạng thái *" required>
              <el-select v-model="createForm.status" style="width: 100%">
                <el-option label="Hoạt động" value="ACTIVE" />
                <el-option label="Khóa tài khoản" value="LOCKED" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
    </BaseDialog>

    <!-- Edit User Dialog -->
    <BaseDialog 
      v-model="editDialogVisible"
      title="Cập nhật tài khoản" 
      :subtitle="'Chỉnh sửa thông tin cho người dùng @' + editForm.username"
      width="460px"
      confirm-text="Lưu thay đổi"
      :loading="saving"
      @confirm="saveEditUser"
    >
      <el-form :model="editForm" label-position="top">
        <el-form-item label="Tên đăng nhập">
          <el-input v-model="editForm.username" disabled />
        </el-form-item>
        <el-form-item label="Email">
          <el-input v-model="editForm.email" placeholder="Nhập địa chỉ email" />
        </el-form-item>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="Phân quyền *" required>
              <el-select v-model="editForm.role" style="width: 100%">
                <el-option label="ADMIN (Quản trị)" value="ADMIN" />
                <el-option label="USER (Nhân viên)" value="USER" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="Trạng thái *" required>
              <el-select 
                v-model="editForm.status" 
                style="width: 100%"
                :disabled="editForm.username === currentUsername"
              >
                <el-option label="Hoạt động" value="ACTIVE" />
                <el-option label="Khóa tài khoản" value="LOCKED" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
    </BaseDialog>

    <!-- Reset Password Dialog -->
    <BaseDialog 
      v-model="resetPasswordDialogVisible"
      title="Đặt lại mật khẩu" 
      :subtitle="'Thiết lập mật khẩu mới cho tài khoản @' + resetPasswordForm.username"
      width="420px"
      confirm-text="Cập nhật mật khẩu"
      :loading="saving"
      @confirm="saveResetPassword"
    >
      <el-form :model="resetPasswordForm" label-position="top">
        <el-form-item label="Mật khẩu mới *" required>
          <el-input 
            v-model="resetPasswordForm.newPassword" 
            type="password" 
            show-password 
            placeholder="Tối thiểu 6 ký tự" 
          />
        </el-form-item>
        <el-form-item label="Xác nhận mật khẩu *" required>
          <el-input 
            v-model="resetPasswordForm.confirmPassword" 
            type="password" 
            show-password 
            placeholder="Nhập lại mật khẩu mới" 
          />
        </el-form-item>
      </el-form>
    </BaseDialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useAuthStore } from '../store/auth'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, User as UserIcon, Search, RefreshRight } from '@element-plus/icons-vue'
import api from '../api'

const authStore = useAuthStore()
const currentUsername = computed(() => authStore.user?.username || '')

const users = ref([])
const totalElements = ref(0)
const totalPages = ref(0)
const page = ref(0)
const size = ref(10)
const loading = ref(false)
const saving = ref(false)

const filters = reactive({
  keyword: '',
  role: 'ALL',
  status: 'ALL'
})

// Dialog Visibility
const createDialogVisible = ref(false)
const editDialogVisible = ref(false)
const resetPasswordDialogVisible = ref(false)

// Forms
const createForm = ref({
  username: '',
  password: '',
  email: '',
  role: 'USER',
  status: 'ACTIVE'
})

const editForm = ref({
  id: null,
  username: '',
  email: '',
  role: 'USER',
  status: 'ACTIVE'
})

const resetPasswordForm = ref({
  id: null,
  username: '',
  newPassword: '',
  confirmPassword: ''
})

const formatDate = (dateStr) => {
  if (!dateStr) return '-'
  try {
    const d = new Date(dateStr)
    return d.toLocaleString('vi-VN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit'
    })
  } catch (e) {
    return dateStr
  }
}

const fetchUsers = async () => {
  loading.value = true
  try {
    const params = {
      page: page.value,
      size: size.value,
      keyword: filters.keyword ? filters.keyword.trim() : undefined,
      role: filters.role !== 'ALL' ? filters.role : undefined,
      status: filters.status !== 'ALL' ? filters.status : undefined,
      sortBy: 'id',
      sortDir: 'desc'
    }

    const res = await api.get('/users', { params })
    if (res.data && res.data.content !== undefined) {
      users.value = res.data.content
      totalElements.value = res.data.totalElements
      totalPages.value = res.data.totalPages
    } else if (Array.isArray(res.data)) {
      users.value = res.data
      totalElements.value = res.data.length
    }
  } catch (e) {
    ElMessage.error(e.response?.data?.message || 'Lỗi khi lấy danh sách tài khoản!')
  } finally {
    loading.value = false
  }
}

const onSearch = () => {
  page.value = 0
  fetchUsers()
}

const onFilterChange = () => {
  page.value = 0
  fetchUsers()
}

const resetFilters = () => {
  filters.keyword = ''
  filters.role = 'ALL'
  filters.status = 'ALL'
  page.value = 0
  fetchUsers()
}

const handlePageChange = (newPage) => {
  page.value = newPage - 1
  fetchUsers()
}

const handleSizeChange = (newSize) => {
  size.value = newSize
  page.value = 0
  fetchUsers()
}

// Create Dialog
const openCreateDialog = () => {
  createForm.value = {
    username: '',
    password: '',
    email: '',
    role: 'USER',
    status: 'ACTIVE'
  }
  createDialogVisible.value = true
}

const saveCreateUser = async () => {
  if (!createForm.value.username || !createForm.value.password) {
    ElMessage.warning('Vui lòng nhập đầy đủ Tên đăng nhập và Mật khẩu!')
    return
  }

  saving.value = true
  try {
    await api.post('/users', createForm.value)
    ElMessage.success('Tạo tài khoản thành công!')
    createDialogVisible.value = false
    fetchUsers()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || e.response?.data || 'Lỗi khi tạo tài khoản!')
  } finally {
    saving.value = false
  }
}

// Edit Dialog
const openEditDialog = (row) => {
  editForm.value = {
    id: row.id,
    username: row.username,
    email: row.email || '',
    role: row.role || 'USER',
    status: row.status || 'ACTIVE'
  }
  editDialogVisible.value = true
}

const saveEditUser = async () => {
  saving.value = true
  try {
    await api.put(`/users/${editForm.value.id}`, {
      email: editForm.value.email,
      role: editForm.value.role,
      status: editForm.value.status
    })
    ElMessage.success('Cập nhật tài khoản thành công!')
    editDialogVisible.value = false
    fetchUsers()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || 'Lỗi khi cập nhật tài khoản!')
  } finally {
    saving.value = false
  }
}

// Reset Password Dialog
const openResetPasswordDialog = (row) => {
  resetPasswordForm.value = {
    id: row.id,
    username: row.username,
    newPassword: '',
    confirmPassword: ''
  }
  resetPasswordDialogVisible.value = true
}

const saveResetPassword = async () => {
  if (!resetPasswordForm.value.newPassword || resetPasswordForm.value.newPassword.length < 6) {
    ElMessage.warning('Mật khẩu mới phải có tối thiểu 6 ký tự!')
    return
  }
  if (resetPasswordForm.value.newPassword !== resetPasswordForm.value.confirmPassword) {
    ElMessage.warning('Xác nhận mật khẩu không khớp!')
    return
  }

  saving.value = true
  try {
    await api.put(`/users/${resetPasswordForm.value.id}/reset-password`, {
      newPassword: resetPasswordForm.value.newPassword
    })
    ElMessage.success(`Đã đổi mật khẩu cho tài khoản @${resetPasswordForm.value.username} thành công!`)
    resetPasswordDialogVisible.value = false
  } catch (e) {
    ElMessage.error(e.response?.data?.message || 'Lỗi khi đặt lại mật khẩu!')
  } finally {
    saving.value = false
  }
}

// Toggle Status (Active / Locked)
const toggleUserStatus = async (row) => {
  const isLocking = row.status !== 'LOCKED'
  const actionText = isLocking ? 'khóa' : 'mở khóa'

  try {
    await ElMessageBox.confirm(
      `Bạn có chắc chắn muốn ${actionText} tài khoản @${row.username}?`,
      `Xác nhận ${actionText}`,
      {
        confirmButtonText: 'Đồng ý',
        cancelButtonText: 'Hủy',
        type: isLocking ? 'warning' : 'info'
      }
    )

    await api.put(`/users/${row.id}/toggle-status`)
    ElMessage.success(`Đã ${actionText} tài khoản @${row.username}!`)
    fetchUsers()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error(e.response?.data?.message || `Lỗi khi ${actionText} tài khoản!`)
    }
  }
}

// Delete User
const deleteUser = async (row) => {
  try {
    await ElMessageBox.confirm(
      `Bạn có chắc chắn muốn xóa vĩnh viễn tài khoản @${row.username}? Hành động này không thể hoàn tác!`,
      'Xác nhận xóa tài khoản',
      {
        confirmButtonText: 'Xóa ngay',
        cancelButtonText: 'Hủy',
        type: 'danger'
      }
    )
    await api.delete(`/users/${row.id}`)
    ElMessage.success('Đã xóa tài khoản thành công!')
    fetchUsers()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error(e.response?.data?.message || 'Lỗi khi xóa tài khoản!')
    }
  }
}

onMounted(() => {
  fetchUsers()
})
</script>

<style scoped>
.users-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* Filter Toolbar */
.toolbar-wrap {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
}

.filter-group {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}

.filter-search {
  width: 260px;
}

.filter-select {
  width: 180px;
}

.btn-reset {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.count-tag {
  font-size: 13px;
  color: #64748b;
}

/* User Profile Cell */
.user-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-cell-avatar {
  background: linear-gradient(135deg, #6366f1 0%, #06b6d4 100%);
  color: #ffffff;
  font-weight: 700;
  font-size: 14px;
  flex-shrink: 0;
}

.user-cell-info {
  display: flex;
  flex-direction: column;
  line-height: 1.3;
}

.user-name-row {
  display: flex;
  align-items: center;
  gap: 6px;
}

.user-name {
  font-weight: 600;
  color: #0f172a;
}

.current-badge {
  font-size: 10px;
  padding: 0 4px;
  height: 18px;
  line-height: 18px;
}

.user-email {
  font-size: 12px;
  color: #64748b;
}

/* Status Cell */
.status-cell {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.status-toggle-btn {
  font-size: 12px;
}

.created-date {
  font-size: 13px;
  color: #475569;
}

.action-btn-group {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

@media (max-width: 768px) {
  .filter-search,
  .filter-select,
  .btn-add {
    width: 100%;
    max-width: 100%;
  }

  .filter-group {
    width: 100%;
  }
}
</style>
