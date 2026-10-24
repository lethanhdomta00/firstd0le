<template>
  <div class="users-container">
    <div class="page-header">
      <div class="header-text">
        <h2 class="page-title">Quản lý Tài khoản (Users)</h2>
        <p class="page-subtitle">Danh sách người dùng và phân quyền truy cập hệ thống</p>
      </div>
      <el-button type="primary" size="large" @click="openDialog()" class="btn-add">
        <el-icon style="margin-right: 6px;"><Plus /></el-icon> Thêm tài khoản mới
      </el-button>
    </div>

    <el-card shadow="never" class="table-card">
      <div class="table-responsive-wrapper">
        <el-table :data="users" v-loading="loading" style="width: 100%; min-width: 550px;">
          <el-table-column prop="id" label="ID" width="70" align="center" />
          <el-table-column prop="username" label="Tên đăng nhập" min-width="160">
            <template #default="{ row }">
              <span class="user-name">{{ row.username }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="role" label="Phân quyền (Role)" width="150" align="center">
            <template #default="{ row }">
              <el-tag :type="row.role === 'ADMIN' ? 'danger' : 'success'" effect="light">
                {{ row.role }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="Hành động" width="130" align="center" fixed="right">
            <template #default="{ row }">
              <el-button size="small" type="danger" plain @click="deleteUser(row.id)">Xóa</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-card>

    <!-- Dialog Add User (Responsive Width) -->
    <el-dialog 
      title="Thêm tài khoản mới" 
      v-model="dialogVisible" 
      :width="isMobile ? '92%' : '440px'"
      destroy-on-close
    >
      <el-form :model="form" label-position="top">
        <el-form-item label="Tên đăng nhập *" required>
          <el-input v-model="form.username" placeholder="Nhập tên đăng nhập" />
        </el-form-item>
        <el-form-item label="Mật khẩu *" required>
          <el-input v-model="form.password" type="password" show-password placeholder="Nhập mật khẩu" />
        </el-form-item>
        <el-form-item label="Phân quyền *" required>
          <el-select v-model="form.role" style="width: 100%">
            <el-option label="ADMIN (Quản trị viên)" value="ADMIN" />
            <el-option label="USER (Nhân viên)" value="USER" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">Hủy</el-button>
          <el-button type="primary" @click="saveUser">Tạo tài khoản</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { useResponsive } from '../composables/useResponsive'
import api from '../api'

const { isMobile } = useResponsive()

const users = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const form = ref({ username: '', password: '', role: 'USER' })

const fetchUsers = async () => {
  loading.value = true
  try {
    const res = await api.get('/users')
    users.value = res.data
  } catch (e) {
    ElMessage.error('Lỗi khi lấy danh sách tài khoản!')
  } finally {
    loading.value = false
  }
}

const openDialog = () => {
  form.value = { username: '', password: '', role: 'USER' }
  dialogVisible.value = true
}

const saveUser = async () => {
  if (!form.value.username || !form.value.password) {
    ElMessage.warning('Vui lòng nhập đầy đủ Tên đăng nhập và Mật khẩu!')
    return
  }

  try {
    await api.post('/users', form.value)
    ElMessage.success('Tạo tài khoản thành công!')
    dialogVisible.value = false
    fetchUsers()
  } catch (e) {
    ElMessage.error(e.response?.data || 'Lỗi khi tạo tài khoản!')
  }
}

const deleteUser = async (id) => {
  try {
    await ElMessageBox.confirm('Bạn có chắc chắn muốn xóa tài khoản này không?', 'Xác nhận xóa', {
      confirmButtonText: 'Xóa ngay',
      cancelButtonText: 'Hủy',
      type: 'warning'
    })
    await api.delete(`/users/${id}`)
    ElMessage.success('Đã xóa tài khoản thành công!')
    fetchUsers()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('Lỗi khi xóa tài khoản!')
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

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 16px;
}

.header-text {
  flex: 1;
  min-width: 240px;
}

.page-title {
  margin: 0;
  font-size: 22px;
  font-weight: 700;
  color: #0f172a;
}

.page-subtitle {
  margin: 4px 0 0 0;
  font-size: 13px;
  color: #64748b;
}

.table-card {
  border-radius: 14px;
  border: 1px solid #e2e8f0;
}

.table-responsive-wrapper {
  width: 100%;
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;
}

.user-name {
  font-weight: 600;
  color: #0f172a;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

/* Mobile Responsive */
@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
    align-items: stretch;
    gap: 12px;
  }

  .btn-add {
    width: 100%;
  }

  .page-title {
    font-size: 18px;
  }

  .dialog-footer {
    flex-direction: column;
  }

  .dialog-footer .el-button {
    width: 100%;
    margin-left: 0 !important;
  }
}
</style>
