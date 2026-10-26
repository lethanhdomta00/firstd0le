<template>
  <div class="users-container">
    <!-- Reusable Base Page Header -->
    <BasePageHeader
      title="Quản lý Tài khoản (Users)"
      subtitle="Danh sách người dùng và phân quyền truy cập hệ thống"
      :icon="User"
    >
      <BaseButton type="gradient" size="large" :icon="Plus" @click="openDialog()" class="btn-add">
        Thêm tài khoản mới
      </BaseButton>
    </BasePageHeader>

    <!-- Reusable Base Table with Built-in Skeleton Loading -->
    <BaseTable 
      :data="users" 
      :loading="loading" 
      :skeleton-columns="4" 
      :skeleton-rows="4"
      min-width="560px"
    >
      <el-table-column prop="id" label="ID" width="70" align="center" />
      <el-table-column prop="username" label="Tên đăng nhập" min-width="160">
        <template #default="{ row }">
          <span class="user-name">{{ row.username }}</span>
        </template>
      </el-table-column>

      <!-- Reusable Base Status Badge for Role -->
      <el-table-column prop="role" label="Phân quyền (Role)" width="160" align="center">
        <template #default="{ row }">
          <BaseStatusBadge
            :type="row.role === 'ADMIN' ? 'danger' : 'info'"
            :text="row.role === 'ADMIN' ? 'Quản trị viên (ADMIN)' : 'Nhân viên (USER)'"
          />
        </template>
      </el-table-column>

      <el-table-column label="Hành động" width="130" align="center" fixed="right">
        <template #default="{ row }">
          <el-button size="small" type="danger" plain @click="deleteUser(row.id)">Xóa</el-button>
        </template>
      </el-table-column>
    </BaseTable>

    <!-- Reusable Base Dialog -->
    <BaseDialog 
      v-model="dialogVisible"
      title="Thêm tài khoản mới" 
      subtitle="Tạo tài khoản người dùng mới và chỉ định phân quyền truy cập"
      width="440px"
      confirm-text="Tạo tài khoản"
      @confirm="saveUser"
    >
      <el-form :model="form" label-position="top">
        <el-form-item label="Tên đăng nhập *" required>
          <el-input v-model="form.username" placeholder="Nhập tên đăng nhập" />
        </el-form-item>
        <el-form-item label="Mật khẩu *" required>
          <el-input v-model="form.password" type="password" show-password placeholder="Nhập mật khẩu (tối thiểu 6 ký tự)" />
        </el-form-item>
        <el-form-item label="Phân quyền *" required>
          <el-select v-model="form.role" style="width: 100%">
            <el-option label="ADMIN (Quản trị viên)" value="ADMIN" />
            <el-option label="USER (Nhân viên)" value="USER" />
          </el-select>
        </el-form-item>
      </el-form>
    </BaseDialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, User } from '@element-plus/icons-vue'
import api from '../api'

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
    ElMessage.error(e.response?.data?.message || e.response?.data || 'Lỗi khi tạo tài khoản!')
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
}

.user-name {
  font-weight: 600;
  color: #0f172a;
}

@media (max-width: 768px) {
  .btn-add {
    width: 100%;
  }
}
</style>
