<template>
  <div class="login-container">
    <el-card class="login-card">
      <template #header>
        <h2 style="text-align: center; margin: 0">ERP Login</h2>
      </template>
      <el-form :model="form" @keyup.enter="handleLogin">
        <el-form-item>
          <el-input v-model="form.username" placeholder="Username">
            <template #prefix><el-icon><User /></el-icon></template>
          </el-input>
        </el-form-item>
        <el-form-item>
          <el-input v-model="form.password" type="password" placeholder="Password" show-password>
            <template #prefix><el-icon><Lock /></el-icon></template>
          </el-input>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleLogin" style="width: 100%" :loading="loading">
            Login
          </el-button>
        </el-form-item>
      </el-form>
      <div style="text-align: center; margin-top: 10px;">
        <el-button link type="primary" @click="seedUsers">Seed Admin User (admin/admin)</el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../store/auth'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import api from '../api'

const form = ref({ username: 'admin', password: 'password' })
const loading = ref(false)
const router = useRouter()
const authStore = useAuthStore()

const handleLogin = async () => {
  loading.value = true
  const success = await authStore.login(form.value.username, form.value.password)
  loading.value = false
  if (success) {
    ElMessage.success('Login successful')
    router.push('/')
  } else {
    ElMessage.error('Invalid credentials')
  }
}

const seedUsers = async () => {
  try {
    await api.post('/auth/register', { username: 'admin', password: 'password' })
    ElMessage.success('Seed successful, try logging in with admin / password')
  } catch (e) {
    ElMessage.error('Error seeding data')
  }
}
</script>

<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  background-color: #2d3a4b;
}
.login-card {
  width: 400px;
}
</style>

