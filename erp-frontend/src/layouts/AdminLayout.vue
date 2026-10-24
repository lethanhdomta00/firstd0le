<template>
  <el-container class="admin-layout">
    <!-- DESKTOP SIDEBAR (Visible on >= 992px) -->
    <el-aside 
      v-if="!isMobile" 
      :width="isCollapse ? '64px' : '230px'" 
      class="desktop-sidebar"
    >
      <div class="sidebar-logo">
        <el-icon class="logo-icon"><Goods /></el-icon>
        <span v-if="!isCollapse" class="logo-text">ERP System</span>
      </div>

      <el-menu 
        :default-active="$route.path" 
        router 
        :collapse="isCollapse"
        background-color="#1e293b" 
        text-color="#94a3b8" 
        active-text-color="#38bdf8"
        class="custom-menu"
      >
        <el-menu-item index="/">
          <el-icon><Monitor /></el-icon>
          <template #title>Dashboard</template>
        </el-menu-item>
        <el-menu-item index="/products">
          <el-icon><Goods /></el-icon>
          <template #title>Quản lý Sản phẩm</template>
        </el-menu-item>
        <el-menu-item index="/users">
          <el-icon><User /></el-icon>
          <template #title>Quản lý Tài khoản</template>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <!-- MOBILE DRAWER SIDEBAR (Visible on < 992px) -->
    <el-drawer
      v-else
      v-model="mobileDrawerVisible"
      direction="ltr"
      size="260px"
      :with-header="false"
      class="mobile-drawer"
    >
      <div class="drawer-header">
        <div class="sidebar-logo">
          <el-icon class="logo-icon"><Goods /></el-icon>
          <span class="logo-text">ERP System</span>
        </div>
      </div>

      <el-menu 
        :default-active="$route.path" 
        router 
        background-color="#1e293b" 
        text-color="#94a3b8" 
        active-text-color="#38bdf8"
        class="custom-menu"
        @select="mobileDrawerVisible = false"
      >
        <el-menu-item index="/">
          <el-icon><Monitor /></el-icon>
          <span>Dashboard</span>
        </el-menu-item>
        <el-menu-item index="/products">
          <el-icon><Goods /></el-icon>
          <span>Quản lý Sản phẩm</span>
        </el-menu-item>
        <el-menu-item index="/users">
          <el-icon><User /></el-icon>
          <span>Quản lý Tài khoản</span>
        </el-menu-item>
      </el-menu>
    </el-drawer>

    <!-- MAIN APP CONTENT AREA -->
    <el-container class="main-wrapper">
      <!-- HEADER -->
      <el-header class="top-header">
        <div class="header-left">
          <!-- Toggle button for desktop collapse or mobile drawer -->
          <el-button 
            type="text" 
            class="toggle-btn"
            @click="toggleSidebar"
          >
            <el-icon :size="20">
              <Expand v-if="isMobile ? !mobileDrawerVisible : isCollapse" />
              <Fold v-else />
            </el-icon>
          </el-button>

          <!-- Current Route Title -->
          <span class="page-current-title">{{ currentRouteTitle }}</span>
        </div>

        <div class="header-right">
          <div class="user-info-pill">
            <el-avatar :size="30" class="user-avatar">A</el-avatar>
            <span class="username-text">Admin</span>
          </div>

          <el-button 
            @click="handleLogout" 
            type="danger" 
            size="small" 
            plain 
            class="logout-btn"
          >
            <el-icon><SwitchButton /></el-icon>
            <span class="logout-text">Đăng xuất</span>
          </el-button>
        </div>
      </el-header>

      <!-- CONTENT BODY -->
      <el-main class="content-body">
        <div class="content-container">
          <router-view></router-view>
        </div>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../store/auth'
import { useResponsive } from '../composables/useResponsive'
import { Monitor, Goods, User, Fold, Expand, SwitchButton } from '@element-plus/icons-vue'

const authStore = useAuthStore()
const router = useRouter()
const route = useRoute()
const { isMobile } = useResponsive()

const isCollapse = ref(false)
const mobileDrawerVisible = ref(false)

const toggleSidebar = () => {
  if (isMobile.value) {
    mobileDrawerVisible.value = !mobileDrawerVisible.value
  } else {
    isCollapse.value = !isCollapse.value
  }
}

const currentRouteTitle = computed(() => {
  if (route.path === '/products') return 'Quản lý Sản phẩm'
  if (route.path === '/users') return 'Quản lý Tài khoản'
  return 'Tổng quan Dashboard'
})

const handleLogout = () => {
  authStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.admin-layout {
  min-height: 100vh;
  width: 100%;
  background-color: #f8fafc;
}

/* Desktop Sidebar */
.desktop-sidebar {
  background-color: #1e293b;
  transition: width 0.28s cubic-bezier(0.4, 0, 0.2, 1);
  overflow-x: hidden;
  box-shadow: 2px 0 8px rgba(0, 0, 0, 0.05);
  display: flex;
  flex-direction: column;
}

.sidebar-logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  background-color: #0f172a;
  color: #38bdf8;
  font-weight: 700;
  font-size: 18px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
  white-space: nowrap;
}

.logo-icon {
  font-size: 22px;
}

.custom-menu {
  border-right: none;
  flex: 1;
}

.custom-menu :deep(.el-menu-item) {
  height: 50px;
  line-height: 50px;
  margin: 4px 8px;
  border-radius: 8px;
}

.custom-menu :deep(.el-menu-item.is-active) {
  background-color: #0284c7 !important;
  color: #ffffff !important;
  font-weight: 600;
}

/* Mobile Drawer */
:deep(.mobile-drawer .el-drawer__body) {
  padding: 0;
  background-color: #1e293b;
}

.drawer-header {
  background-color: #0f172a;
}

/* Main Area */
.main-wrapper {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background-color: #f8fafc;
  overflow-x: hidden;
}

/* Top Header */
.top-header {
  height: 60px;
  background-color: #ffffff;
  border-bottom: 1px solid #e2e8f0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
  position: sticky;
  top: 0;
  z-index: 99;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.toggle-btn {
  color: #475569 !important;
  padding: 8px !important;
  font-size: 18px;
  border-radius: 8px;
}

.toggle-btn:hover {
  background-color: #f1f5f9;
}

.page-current-title {
  font-size: 16px;
  font-weight: 600;
  color: #1e293b;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-info-pill {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 10px;
  background-color: #f1f5f9;
  border-radius: 20px;
}

.user-avatar {
  background-color: #0284c7;
  color: #ffffff;
  font-weight: 600;
  font-size: 13px;
}

.username-text {
  font-size: 13px;
  font-weight: 500;
  color: #334155;
}

.logout-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

/* Main Content */
.content-body {
  padding: 24px;
  flex: 1;
  width: 100%;
  box-sizing: border-box;
}

.content-container {
  width: 100%;
  max-width: 1400px;
  margin: 0 auto;
}

/* Responsive Breakpoints */
@media (max-width: 768px) {
  .top-header {
    padding: 0 12px;
  }

  .content-body {
    padding: 12px;
  }

  .page-current-title {
    font-size: 14px;
  }

  .username-text {
    display: none;
  }

  .logout-text {
    display: none;
  }

  .logout-btn {
    padding: 8px;
  }
}
</style>
