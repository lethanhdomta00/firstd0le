import { defineStore } from 'pinia'
import api from '../api'

export const useAuthStore = defineStore('auth', {
  state: () => {
    let savedUser = null
    try {
      savedUser = JSON.parse(localStorage.getItem('user') || 'null')
    } catch (e) {
      savedUser = null
    }

    return {
      token: localStorage.getItem('token') || null,
      user: savedUser
    }
  },

  getters: {
    isAuthenticated: (state) => !!state.token,
    isAdmin: (state) => state.user?.role === 'ADMIN',
    currentUser: (state) => state.user
  },

  actions: {
    async login(username, password) {
      try {
        const response = await api.post('/auth/login', { username, password })
        this.token = response.data.accessToken
        this.user = response.data.user || null
        
        localStorage.setItem('token', this.token)
        if (this.user) {
          localStorage.setItem('user', JSON.stringify(this.user))
        }
        return { success: true }
      } catch (error) {
        console.error('Login failed:', error)
        const message = error.response?.data?.message || 'Đăng nhập không thành công!'
        return { success: false, message }
      }
    },

    async fetchCurrentUser() {
      if (!this.token) return null
      try {
        const response = await api.get('/auth/me')
        this.user = response.data
        localStorage.setItem('user', JSON.stringify(this.user))
        return this.user
      } catch (error) {
        console.error('Failed to fetch user profile:', error)
        if (error.response?.status === 401 || error.response?.status === 403) {
          this.logout()
        }
        return null
      }
    },

    async updateProfile(profileData) {
      try {
        const response = await api.put('/auth/profile', profileData)
        this.user = response.data
        localStorage.setItem('user', JSON.stringify(this.user))
        return { success: true, data: this.user }
      } catch (error) {
        console.error('Failed to update profile:', error)
        const message = error.response?.data?.message || 'Lỗi khi cập nhật thông tin tài khoản!'
        return { success: false, message }
      }
    },

    logout() {
      this.token = null
      this.user = null
      localStorage.removeItem('token')
      localStorage.removeItem('user')
    }
  }
})
