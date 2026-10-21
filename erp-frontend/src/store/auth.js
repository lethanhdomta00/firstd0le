import { defineStore } from 'pinia'
import api from '../api'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem('token') || null,
    user: null
  }),
  getters: {
    isAuthenticated: (state) => !!state.token
  },
  actions: {
    async login(username, password) {
      try {
        const response = await api.post('/auth/login', { username, password })
        this.token = response.data.accessToken
        localStorage.setItem('token', this.token)
        return true
      } catch (error) {
        console.error('Login failed:', error)
        return false
      }
    },
    logout() {
      this.token = null
      this.user = null
      localStorage.removeItem('token')
    }
  }
})

