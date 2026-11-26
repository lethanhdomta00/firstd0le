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
        component: () => import('../views/Products.vue'),
        meta: { permission: 'PRODUCT_VIEW' }
      },
      {
        path: 'orders',
        name: 'Orders',
        component: () => import('../views/Orders.vue'),
        meta: { permission: 'ORDER_VIEW' }
      },
      {
        path: 'analytics',
        name: 'Analytics',
        component: () => import('../views/Analytics.vue'),
        meta: { permission: 'ANALYTICS_VIEW' }
      },
      {
        path: 'finance',
        name: 'Finance',
        component: () => import('../views/Finance.vue'),
        meta: { permission: 'ANALYTICS_VIEW' }
      },
      {
        path: 'procurement',
        name: 'Procurement',
        component: () => import('../views/Procurement.vue'),
        meta: { permission: 'PROCUREMENT_VIEW' }
      },
      {
        path: 'promotions',
        name: 'Promotions',
        component: () => import('../views/Promotions.vue'),
        meta: { permission: 'ORDER_VIEW' }
      },
      {
        path: 'branches',
        name: 'BranchWarehouse',
        component: () => import('../views/BranchWarehouse.vue'),
        meta: { permission: 'BRANCH_VIEW' }
      },
      {
        path: 'inventory-audit',
        name: 'InventoryAudit',
        component: () => import('../views/InventoryAudit.vue'),
        meta: { permission: 'PRODUCT_VIEW' }
      },
      {
        path: 'workflow-approvals',
        name: 'WorkflowApproval',
        component: () => import('../views/WorkflowApproval.vue'),
        meta: { title: 'Quy trình Phê duyệt & Kiểm toán' }
      },
      {
        path: 'activity-logs',
        name: 'ActivityLogs',
        component: () => import('../views/ActivityLogs.vue'),
        meta: { title: 'Nhật ký Thao tác Hệ thống' }
      },
      {
        path: 'users',
        name: 'Users',
        component: () => import('../views/Users.vue'),
        meta: { permission: 'USER_VIEW' }
      },
      {
        path: 'announcements',
        name: 'Announcements',
        component: () => import('../views/Announcements.vue'),
        meta: { role: 'ADMIN' } // Quản lý Thông báo & Sự kiện
      },
      {
        path: 'chat',
        name: 'Chat',
        component: () => import('../views/Chat.vue'),
        meta: { permission: 'CHAT_USE' }
      },
      {
        path: 'calendar',
        name: 'Calendar',
        component: () => import('../views/Calendar.vue')
      },
      {
        path: 'jobs',
        name: 'Jobs',
        component: () => import('../views/Jobs.vue'),
        meta: { permission: 'JOB_VIEW' }
      },
      {
        path: 'settings',
        name: 'Settings',
        component: () => import('../views/Settings.vue'),
        meta: { role: 'ADMIN' }
      },
      {
        path: ':pathMatch(.*)*',
        name: 'NotFound',
        component: () => import('../views/NotFound.vue')
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/'
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

  // Đảm bảo thông tin user và danh sách quyền hạn đã được nạp
  if (authStore.isAuthenticated && !authStore.user) {
    await authStore.fetchCurrentUser()
  }

  // Chặn người dùng theo vai trò tĩnh nếu route yêu cầu (ví dụ meta: { role: 'ADMIN' })
  if (to.meta.role && authStore.isAuthenticated) {
    if (authStore.user?.role !== to.meta.role) {
      ElMessage.warning('Bạn không có quyền truy cập vào phân hệ này!')
      return next({ name: 'Dashboard' })
    }
  }

  // Chặn người dùng theo quyền động (RBAC Permission Code)
  if (to.meta.permission && authStore.isAuthenticated) {
    if (!authStore.hasPermission(to.meta.permission)) {
      ElMessage.warning(`Bạn chưa được cấp quyền [${to.meta.permission}] để truy cập phân hệ này!`)
      return next({ name: 'Dashboard' })
    }
  }

  next()
})

export default router
