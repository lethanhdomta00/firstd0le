<template>
  <div>
    <div style="display: flex; justify-content: space-between; margin-bottom: 20px;">
      <h2>Quản lý tài khoản (Users)</h2>
      <el-button type="primary" @click="openDialog()">Thêm tài khoản</el-button>
    </div>

    <el-table :data="users" v-loading="loading" border style="width: 100%">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="username" label="Tên đăng nhập" />
      <el-table-column prop="role" label="Phân quyền (Role)" width="150">
        <template #default="{ row }">
          <el-tag :type="row.role === 'ADMIN' ? 'danger' : 'success'">{{ row.role }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="Hành động" width="150">
        <template #default="{ row }">
          <el-button size="small" type="danger" @click="deleteUser(row.id)">Xóa</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog title="Thêm tài khoản mới" v-model="dialogVisible" width="400px">
      <el-form :model="form" label-width="120px">
        <el-form-item label="Tên đăng nhập">
          <el-input v-model="form.username" />
        </el-form-item>
        <el-form-item label="Mật khẩu">
          <el-input v-model="form.password" type="password" show-password />
        </el-form-item>
        <el-form-item label="Phân quyền">
          <el-select v-model="form.role" style="width: 100%">
            <el-option label="ADMIN" value="ADMIN" />
            <el-option label="USER" value="USER" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">Hủy</el-button>
        <el-button type="primary" @click="saveUser">Lưu</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
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
    ElMessage.error('Lỗi khi lấy danh sách tài khoản')
  } finally {
    loading.value = false
  }
}

const openDialog = () => {
  form.value = { username: '', password: '', role: 'USER' }
  dialogVisible.value = true
}

const saveUser = async () => {
  try {
    await api.post('/users', form.value)
    ElMessage.success('Tạo tài khoản thành công')
    dialogVisible.value = false
    fetchUsers()
  } catch (e) {
    ElMessage.error(e.response?.data || 'Lỗi khi tạo tài khoản')
  }
}

const deleteUser = async (id) => {
  try {
    await ElMessageBox.confirm('Bạn có chắc chắn muốn xóa tài khoản này không?', 'Cảnh báo', { type: 'warning' })
    await api.delete(`/users/${id}`)
    ElMessage.success('Đã xóa tài khoản')
    fetchUsers()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('Lỗi khi xóa tài khoản')
  }
}

onMounted(() => {
  fetchUsers()
})
</script>

