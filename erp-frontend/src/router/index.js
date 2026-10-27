import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../store/auth'
import { ElMessage } from 'element-plus'
import AdminLayout from '../layouts/AdminLayout.vue'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue')
  },
  {
    path: '/',
    component: AdminLayout,
    meta: { requiresAuth: true },
    children: [
      {
        path: '',
        name: 'Dashboard',
        component: () => import('../views/Dashboard.vue')
      },
      {
        path: 'products',
        name: 'Products',
        component: () => import('../views/Products.vue')
      },
      {
        path: 'users',
        name: 'Users',
        component: () => import('../views/Users.vue'),
        meta: { role: 'ADMIN' } // Chỉ ADMIN mới được vào màn Quản lý Tài khoản
      },
      {
        path: 'announcements',
        name: 'Announcements',
        component: () => import('../views/Announcements.vue'),
        meta: { role: 'ADMIN' } // Quản lý Thông báo & Sự kiện
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach(async (to, from, next) => {
  const authStore = useAuthStore()

  if (to.meta.requiresAuth && !authStore.isAuthenticated) {
    return next({ name: 'Login' })
  }
  
  if (to.name === 'Login' && authStore.isAuthenticated) {
    return next({ name: 'Dashboard' })
  }

  // Chặn người dùng không phải ADMIN truy cập vào các route yêu cầu quyền ADMIN
  if (to.meta.role && authStore.isAuthenticated) {
    if (!authStore.user) {
      await authStore.fetchCurrentUser()
    }
    if (authStore.user?.role !== to.meta.role) {
      ElMessage.warning('Bạn không có quyền truy cập vào trang này!')
      return next({ name: 'Dashboard' })
    }
  }

  next()
})

export default router
