<template>
  <div class="users-container">
    <!-- Reusable Base Page Header -->
    <BasePageHeader
      title="Quản lý Tài khoản (Users)"
      subtitle="Danh sách người dùng, ma trận phân quyền truy cập RBAC và kiểm soát trạng thái tài khoản"
      :icon="UserIcon"
    >
      <div class="header-action-btns">
        <el-button
          size="large"
          class="btn-matrix-action"
          @click="openRolesMatrixDrawer"
        >
          <el-icon><Grid /></el-icon>
          <span>Ma trận Quyền hạn RBAC</span>
        </el-button>
        <BaseButton type="gradient" size="large" :icon="Plus" @click="openCreateDialog()" class="btn-add">
          Thêm tài khoản mới
        </BaseButton>
      </div>
    </BasePageHeader>

    <!-- Reusable Base Table with Built-in Skeleton Loading & Server-Side Pagination -->
    <BaseTable 
      :data="users" 
      :loading="loading" 
      :skeleton-columns="6" 
      :skeleton-rows="6"
      min-width="840px"
      class="users-desktop-table"
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
              placeholder="Tìm theo Username, Tên hoặc Email..." 
              clearable
              class="filter-search"
              @clear="onSearch"
              @keyup.enter="onSearch"
            >
              <template #prefix><el-icon><Search /></el-icon></template>
            </el-input>

            <!-- Role Filter Đầy đủ 6 Roles -->
            <el-select 
              v-model="filters.role" 
              placeholder="Tất cả vai trò" 
              class="filter-select"
              @change="onFilterChange"
            >
              <el-option label="Tất cả vai trò" value="ALL" />
              <el-option label="Quản trị viên (ADMIN)" value="ADMIN" />
              <el-option label="Quản lý (MANAGER)" value="MANAGER" />
              <el-option label="Kế toán (ACCOUNTANT)" value="ACCOUNTANT" />
              <el-option label="Thủ kho (WAREHOUSE)" value="WAREHOUSE" />
              <el-option label="Kinh doanh (SALES)" value="SALES" />
              <el-option label="Nhân viên (USER)" value="USER" />
            </el-select>

            <!-- Status Filter -->
            <el-select 
              v-model="filters.status" 
              placeholder="Trạng thái" 
              class="filter-select sm"
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
      <el-table-column prop="id" label="ID" width="56" align="center" />

      <!-- User Profile (Avatar + Username + Email) -->
      <el-table-column label="Thông tin tài khoản" min-width="220">
        <template #default="{ row }">
          <div class="user-cell">
            <el-avatar :size="32" class="user-cell-avatar">
              {{ (row.username || 'U').charAt(0).toUpperCase() }}
            </el-avatar>
            <div class="user-cell-info">
              <div class="user-name-row">
                <span class="user-name">{{ row.username }}</span>
                <el-tag v-if="row.username === currentUsername" size="small" type="success" effect="dark" class="current-badge">
                  Bạn
                </el-tag>
              </div>
              <span v-if="row.fullName" class="user-fullname">{{ row.fullName }}</span>
              <span class="user-email">{{ row.email || 'Chưa cập nhật email' }}</span>
            </div>
          </div>
        </template>
      </el-table-column>

      <!-- Role Badge Đa Sắc Phân Cấp RBAC (Hỗ trợ 1-Click Dropdown đổi Role nhanh) -->
      <el-table-column prop="role" label="Phân quyền (RBAC)" width="200" align="center">
        <template #default="{ row }">
          <el-dropdown trigger="click" @command="(newRole) => handleQuickChangeRole(row, newRole)">
            <div 
              class="role-pill-badge clickable" 
              :class="'role-' + (row.role || 'USER').toLowerCase()"
              title="Click để đổi vai trò nhanh cho nhân sự này"
            >
              <span class="role-label-text">{{ getRoleDisplayName(row.role) }}</span>
              <span class="role-caret">▾</span>
            </div>
            <template #dropdown>
              <el-dropdown-menu class="quick-role-dropdown-menu">
                <div class="quick-role-menu-header">Chọn vai trò cho @{{ row.username }}:</div>
                <el-dropdown-item
                  v-for="r in ROLE_OPTIONS"
                  :key="r.value"
                  :command="r.value"
                  :disabled="row.role === r.value"
                  :divided="r.value === 'USER'"
                >
                  <div class="quick-role-item-row">
                    <div class="q-info">
                      <span class="q-name">{{ r.label }}</span>
                      <span class="q-desc">{{ r.desc }}</span>
                    </div>
                    <span v-if="row.role === r.value" class="q-active-tick">✓</span>
                  </div>
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </template>
      </el-table-column>

      <!-- Account Status with Quick Toggle -->
      <el-table-column label="Trạng thái" width="146" align="center">
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
      <el-table-column label="Ngày tạo" width="132" align="center">
        <template #default="{ row }">
          <span class="created-date">{{ formatDate(row.createdAt) }}</span>
        </template>
      </el-table-column>

      <!-- Action Buttons -->
      <el-table-column label="Hành động" width="215" align="center">
        <template #default="{ row }">
          <div class="action-btn-group">
            <el-button size="small" @click="openEditDialog(row)">Sửa</el-button>
            <el-button size="small" type="warning" plain @click="openResetPasswordDialog(row)">
              Đổi MK
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

    <!-- Mobile Card View (< 768px) -->
    <div class="users-mobile-list">
      <div v-for="u in users" :key="'mu-' + u.id" class="mobile-user-card">
        <div class="mu-top">
          <div class="user-cell">
            <el-avatar :size="32" class="user-cell-avatar">
              {{ (u.username || 'U').charAt(0).toUpperCase() }}
            </el-avatar>
            <div class="user-cell-info">
              <div class="user-name-row">
                <span class="user-name">{{ u.username }}</span>
                <el-tag v-if="u.username === currentUsername" size="small" type="success" effect="dark" class="current-badge">
                  Bạn
                </el-tag>
              </div>
              <span class="user-email">{{ u.email || 'Chưa cập nhật email' }}</span>
            </div>
          </div>
          <div class="role-pill-badge" :class="'role-' + (u.role || 'USER').toLowerCase()">
            <span>{{ getRoleDisplayName(u.role) }}</span>
          </div>
        </div>
        <div class="mu-meta">
          <div class="status-cell">
            <BaseStatusBadge
              :type="u.status === 'LOCKED' ? 'danger' : 'success'"
              :text="u.status === 'LOCKED' ? 'Đã khóa' : 'Hoạt động'"
            />
            <el-button
              size="small"
              link
              :type="u.status === 'LOCKED' ? 'success' : 'warning'"
              :disabled="u.username === currentUsername"
              @click="toggleUserStatus(u)"
            >
              {{ u.status === 'LOCKED' ? 'Mở khóa' : 'Khóa' }}
            </el-button>
          </div>
          <span class="created-date">{{ formatDate(u.createdAt) }}</span>
        </div>
        <div class="mu-actions">
          <el-button size="small" @click="openEditDialog(u)">Sửa</el-button>
          <el-button size="small" type="warning" plain @click="openResetPasswordDialog(u)">Đổi MK</el-button>
          <el-button size="small" type="danger" plain :disabled="u.username === currentUsername" @click="deleteUser(u)">Xóa</el-button>
        </div>
      </div>
    </div>

    <!-- Create User Dialog (Hỗ trợ chọn đầy đủ 6 Roles) -->
    <BaseDialog 
      v-model="createDialogVisible"
      title="Thêm tài khoản mới" 
      subtitle="Tạo tài khoản người dùng mới và phân quyền truy cập hệ thống ERP"
      width="480px"
      confirm-text="Tạo tài khoản"
      :loading="saving"
      @confirm="saveCreateUser"
    >
      <el-form :model="createForm" label-position="top">
        <el-form-item label="Tên đăng nhập *" required>
          <el-input v-model="createForm.username" placeholder="Nhập tên đăng nhập (ví dụ: staff01)" />
        </el-form-item>
        <el-form-item label="Họ và tên">
          <el-input v-model="createForm.fullName" placeholder="Nhập họ và tên đầy đủ..." />
        </el-form-item>
        <el-form-item label="Mật khẩu *" required>
          <el-input 
            v-model="createForm.password" 
            type="password" 
            show-password 
            placeholder="Tối thiểu 6 ký tự" 
          />
        </el-form-item>
        <el-form-item label="Email doanh nghiệp">
          <el-input v-model="createForm.email" placeholder="Nhập địa chỉ email người dùng..." />
        </el-form-item>
        <el-row :gutter="12">
          <el-col :span="14">
            <el-form-item label="Phân quyền (Role) *" required>
              <el-select v-model="createForm.role" style="width: 100%">
                <el-option label="ADMIN - Quản trị viên tối cao" value="ADMIN" />
                <el-option label="MANAGER - Giám đốc / Quản lý" value="MANAGER" />
                <el-option label="ACCOUNTANT - Kế toán tài chính" value="ACCOUNTANT" />
                <el-option label="WAREHOUSE - Thủ kho" value="WAREHOUSE" />
                <el-option label="SALES - Nhân viên kinh doanh" value="SALES" />
                <el-option label="USER - Nhân viên cơ bản" value="USER" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="10">
            <el-form-item label="Trạng thái">
              <el-select v-model="createForm.status" style="width: 100%">
                <el-option label="Hoạt động" value="ACTIVE" />
                <el-option label="Khóa tạm thời" value="LOCKED" />
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
      :subtitle="'Chỉnh sửa thông tin và phân quyền cho tài khoản @' + editForm.username"
      width="480px"
      confirm-text="Lưu thay đổi"
      :loading="saving"
      @confirm="saveEditUser"
    >
      <el-form :model="editForm" label-position="top">
        <el-form-item label="Tên đăng nhập">
          <el-input v-model="editForm.username" disabled />
        </el-form-item>
        <el-form-item label="Họ và tên">
          <el-input v-model="editForm.fullName" placeholder="Nhập họ và tên đầy đủ..." />
        </el-form-item>
        <el-form-item label="Email doanh nghiệp">
          <el-input v-model="editForm.email" placeholder="Nhập địa chỉ email..." />
        </el-form-item>
        <el-row :gutter="12">
          <el-col :span="14">
            <el-form-item label="Phân quyền (Role) *" required>
              <el-select v-model="editForm.role" style="width: 100%">
                <el-option label="ADMIN - Quản trị viên tối cao" value="ADMIN" />
                <el-option label="MANAGER - Giám đốc / Quản lý" value="MANAGER" />
                <el-option label="ACCOUNTANT - Kế toán tài chính" value="ACCOUNTANT" />
                <el-option label="WAREHOUSE - Thủ kho" value="WAREHOUSE" />
                <el-option label="SALES - Nhân viên kinh doanh" value="SALES" />
                <el-option label="USER - Nhân viên cơ bản" value="USER" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="10">
            <el-form-item label="Trạng thái">
              <el-select v-model="editForm.status" style="width: 100%">
                <el-option label="Hoạt động" value="ACTIVE" />
                <el-option label="Khóa tạm thời" value="LOCKED" />
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

    <!-- ========================================================== -->
    <!-- DIALOG MA TRẬN PHÂN QUYỀN ĐỘNG TƯƠNG TÁC (RBAC MATRIX)     -->
    <!-- ========================================================== -->
    <el-dialog
      v-model="matrixDrawerVisible"
      title="Ma trận Phân quyền Vai trò (RBAC Dynamic Matrix)"
      width="1060px"
      top="3vh"
      class="rbac-matrix-dialog"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <div class="matrix-dialog-container">
        <!-- 1. ACTION TOOLBAR -->
        <div class="matrix-toolbar-row">
          <div class="toolbar-search-box">
            <el-input
              v-model="permissionKeyword"
              placeholder="Tìm kiếm quyền hạn..."
              clearable
              size="default"
              class="search-perm-input"
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>

            <el-select v-model="selectedModuleFilter" placeholder="Tất cả phân hệ" size="default" class="module-filter-select">
              <el-option label="Tất cả phân hệ" value="ALL" />
              <el-option v-for="mod in SYSTEM_MODULES" :key="mod" :label="mod" :value="mod" />
            </el-select>

            <!-- Nút Thu gọn / Mở rộng tất cả phân hệ -->
            <el-button 
              size="default" 
              plain
              class="btn-toggle-all-modules"
              @click="toggleAllModules"
              :title="allModulesCollapsed ? 'Mở rộng toàn bộ phân hệ' : 'Thu gọn toàn bộ phân hệ'"
            >
              <svg viewBox="0 0 24 24" width="13" height="13" fill="none" stroke="currentColor" stroke-width="2.5" style="margin-right: 5px;">
                <path d="M7 15l5 5 5-5M7 9l5-5 5 5"/>
              </svg>
              <span>{{ allModulesCollapsed ? 'Mở rộng tất cả' : 'Thu gọn tất cả' }}</span>
            </el-button>
          </div>

          <div class="toolbar-btn-group">
            <el-button
              size="default"
              class="btn-reset-matrix"
              :disabled="savingMatrix"
              @click="confirmResetMatrix"
            >
              Khôi phục mặc định
            </el-button>
            <el-button
              type="primary"
              size="default"
              class="btn-save-matrix"
              :loading="savingMatrix"
              @click="handleSaveMatrix"
            >
              Lưu thay đổi
            </el-button>
          </div>
        </div>

        <!-- 2. BẢNG MA TRẬN PERMISSIONS GRID -->
        <div class="matrix-table-scroll-wrap">
          <table class="rbac-interactive-table">
            <thead>
              <tr>
                <th class="col-permission text-left">Phân hệ &amp; Quyền hạn</th>
                <th class="col-role admin text-center">
                  <div class="th-role-header">
                    <span class="role-title-tag tag-admin">ADMIN</span>
                    <span class="role-badge-sub">Quản trị</span>
                  </div>
                </th>
                <th class="col-role manager text-center">
                  <div class="th-role-header">
                    <span class="role-title-tag tag-manager">MANAGER</span>
                    <span class="role-badge-sub">Quản lý</span>
                  </div>
                </th>
                <th class="col-role accountant text-center">
                  <div class="th-role-header">
                    <span class="role-title-tag tag-accountant">ACCOUNTANT</span>
                    <span class="role-badge-sub">Kế toán</span>
                  </div>
                </th>
                <th class="col-role warehouse text-center">
                  <div class="th-role-header">
                    <span class="role-title-tag tag-warehouse">WAREHOUSE</span>
                    <span class="role-badge-sub">Thủ kho</span>
                  </div>
                </th>
                <th class="col-role sales text-center">
                  <div class="th-role-header">
                    <span class="role-title-tag tag-sales">SALES</span>
                    <span class="role-badge-sub">Kinh doanh</span>
                  </div>
                </th>
                <th class="col-role user text-center">
                  <div class="th-role-header">
                    <span class="role-title-tag tag-user">USER</span>
                    <span class="role-badge-sub">Nhân viên</span>
                  </div>
                </th>
              </tr>
            </thead>
            <tbody>
              <template v-for="group in filteredPermissionGroups" :key="group.module">
                <!-- Group Header Row (Hỗ trợ Click Thu gọn / Mở rộng phân hệ) -->
                <tr 
                  class="group-header-row" 
                  :class="{ 'is-collapsed': isModuleCollapsed(group.module) }"
                  @click="toggleModuleCollapse(group.module)"
                  title="Click để thu gọn hoặc mở rộng phân hệ này"
                >
                  <td colspan="7" class="group-header-cell">
                    <div class="group-header-flex">
                      <div class="gh-left">
                        <span class="group-name">{{ group.module }}</span>
                        <span class="group-count-tag">{{ group.permissions.length }} quyền</span>
                        <span class="group-granted-tag">{{ getModuleGrantedCount(group.module) }} lượt cấp</span>
                      </div>
                      <div class="gh-right">
                        <span class="gh-collapse-icon" :class="{ 'is-collapsed': isModuleCollapsed(group.module) }">
                          <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2.5">
                            <polyline points="6 9 12 15 18 9"></polyline>
                          </svg>
                        </span>
                      </div>
                    </div>
                  </td>
                </tr>

                <!-- Permission Rows (Ẩn khi phân hệ bị thu gọn) -->
                <template v-if="!isModuleCollapsed(group.module)">
                  <tr v-for="perm in group.permissions" :key="perm.code" class="perm-row">
                    <td class="perm-desc-cell">
                      <div class="perm-name-txt">{{ perm.name }}</div>
                      <code class="perm-code-tag">{{ perm.code }}</code>
                    </td>

                    <!-- ADMIN COLUMN (ALWAYS CHECKED & DISABLED) -->
                    <td class="perm-check-cell admin text-center">
                      <div class="fixed-check-pill" title="ADMIN luôn có toàn quyền để bảo vệ an ninh hệ thống">
                        <svg viewBox="0 0 24 24" width="10" height="10" fill="none" stroke="currentColor" stroke-width="3.5">
                          <polyline points="20 6 9 17 4 12"></polyline>
                        </svg>
                        <span>Cố định</span>
                      </div>
                    </td>

                    <!-- MANAGER COLUMN -->
                    <td class="perm-check-cell text-center" @click="togglePermission('MANAGER', perm.code)">
                      <div class="custom-chk-box" :class="{ checked: isRoleHasPermission('MANAGER', perm.code) }">
                        <svg v-if="isRoleHasPermission('MANAGER', perm.code)" viewBox="0 0 24 24" width="11" height="11" fill="none" stroke="currentColor" stroke-width="3.5" class="chk-svg">
                          <polyline points="20 6 9 17 4 12"></polyline>
                        </svg>
                      </div>
                    </td>

                    <!-- ACCOUNTANT COLUMN -->
                    <td class="perm-check-cell text-center" @click="togglePermission('ACCOUNTANT', perm.code)">
                      <div class="custom-chk-box" :class="{ checked: isRoleHasPermission('ACCOUNTANT', perm.code) }">
                        <svg v-if="isRoleHasPermission('ACCOUNTANT', perm.code)" viewBox="0 0 24 24" width="11" height="11" fill="none" stroke="currentColor" stroke-width="3.5" class="chk-svg">
                          <polyline points="20 6 9 17 4 12"></polyline>
                        </svg>
                      </div>
                    </td>

                    <!-- WAREHOUSE COLUMN -->
                    <td class="perm-check-cell text-center" @click="togglePermission('WAREHOUSE', perm.code)">
                      <div class="custom-chk-box" :class="{ checked: isRoleHasPermission('WAREHOUSE', perm.code) }">
                        <svg v-if="isRoleHasPermission('WAREHOUSE', perm.code)" viewBox="0 0 24 24" width="11" height="11" fill="none" stroke="currentColor" stroke-width="3.5" class="chk-svg">
                          <polyline points="20 6 9 17 4 12"></polyline>
                        </svg>
                      </div>
                    </td>

                    <!-- SALES COLUMN -->
                    <td class="perm-check-cell text-center" @click="togglePermission('SALES', perm.code)">
                      <div class="custom-chk-box" :class="{ checked: isRoleHasPermission('SALES', perm.code) }">
                        <svg v-if="isRoleHasPermission('SALES', perm.code)" viewBox="0 0 24 24" width="11" height="11" fill="none" stroke="currentColor" stroke-width="3.5" class="chk-svg">
                          <polyline points="20 6 9 17 4 12"></polyline>
                        </svg>
                      </div>
                    </td>

                    <!-- USER COLUMN -->
                    <td class="perm-check-cell text-center" @click="togglePermission('USER', perm.code)">
                      <div class="custom-chk-box" :class="{ checked: isRoleHasPermission('USER', perm.code) }">
                        <svg v-if="isRoleHasPermission('USER', perm.code)" viewBox="0 0 24 24" width="11" height="11" fill="none" stroke="currentColor" stroke-width="3.5" class="chk-svg">
                          <polyline points="20 6 9 17 4 12"></polyline>
                        </svg>
                      </div>
                    </td>
                  </tr>
                </template>
              </template>
            </tbody>
          </table>
        </div>
      </div>

      <template #footer>
        <div class="matrix-dialog-footer">
          <div class="footer-status-text">
            <span>Tổng cộng: <strong>{{ totalGrantedPermissionsCount }}</strong> quyền hạn được cấp phát.</span>
            <span v-if="matrixDirty" class="dirty-badge">Có thay đổi chưa lưu</span>
          </div>
          <div class="footer-btns">
            <el-button @click="matrixDrawerVisible = false">Đóng</el-button>
            <el-button type="primary" class="btn-save-matrix" :loading="savingMatrix" @click="handleSaveMatrix">
              Lưu thay đổi
            </el-button>
          </div>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
// =========================================================================
// 0. IMPORTS & HOOKS
// =========================================================================
import { ref, reactive, computed, watch, onMounted } from 'vue'
import { useAuthStore } from '../store/auth'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, User as UserIcon, Search, RefreshRight, Grid } from '@element-plus/icons-vue'
import api from '../api'

const authStore = useAuthStore()
const currentUsername = computed(() => authStore.user?.username || '')

// =========================================================================
// 1. HELPER FUNCTIONS
// =========================================================================
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
  } catch {
    return dateStr
  }
}

const getRoleDisplayName = (role) => {
  switch (role) {
    case 'ADMIN': return 'Quản trị (ADMIN)'
    case 'MANAGER': return 'Quản lý (MANAGER)'
    case 'ACCOUNTANT': return 'Kế toán (ACCOUNTANT)'
    case 'WAREHOUSE': return 'Thủ kho (WAREHOUSE)'
    case 'SALES': return 'Kinh doanh (SALES)'
    default: return 'Nhân viên (USER)'
  }
}

const getRoleTooltip = (role) => {
  switch (role) {
    case 'ADMIN': return 'Toàn quyền tối cao quản trị tài khoản, phân quyền và cấu hình hệ thống'
    case 'MANAGER': return 'Điều hành chung, duyệt đơn hàng, quản lý sản phẩm và xem báo cáo BI'
    case 'ACCOUNTANT': return 'Kiểm soát dòng tiền, xuất hóa đơn tài chính và phân tích doanh thu'
    case 'WAREHOUSE': return 'Quản lý tồn kho, nhập/xuất kho và điều chỉnh thông tin sản phẩm'
    case 'SALES': return 'Tạo đơn hàng, quản lý đơn hàng và theo dõi vận chuyển'
    default: return 'Nhân viên cơ bản sử dụng các tiện ích nội bộ'
  }
}

// =========================================================================
// 2. STATE & DATA
// =========================================================================
const users = ref([])
const totalElements = ref(0)
const totalPages = ref(0)
const page = ref(0)
const size = ref(15)
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
const matrixDrawerVisible = ref(false)
const rolesMatrix = ref([])

// Forms
const createForm = ref({
  username: '',
  fullName: '',
  password: '',
  email: '',
  role: 'USER',
  status: 'ACTIVE'
})

const editForm = ref({
  id: null,
  username: '',
  fullName: '',
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

// =========================================================================
// 3. API METHODS (BẢO TOÀN 100% LOGIC CŨ)
// =========================================================================
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

// Roles Matrix Drawer & Dynamic Matrix Management
const permissionKeyword = ref('')
const selectedModuleFilter = ref('ALL')
const savingMatrix = ref(false)
const matrixDirty = ref(false)

const ROLE_OPTIONS = [
  { value: 'ADMIN', label: 'Quản trị viên (ADMIN)', desc: 'Toàn quyền tối cao' },
  { value: 'MANAGER', label: 'Quản lý (MANAGER)', desc: 'Điều hành & duyệt đơn' },
  { value: 'ACCOUNTANT', label: 'Kế toán (ACCOUNTANT)', desc: 'Xác nhận tiền & hóa đơn' },
  { value: 'WAREHOUSE', label: 'Thủ kho (WAREHOUSE)', desc: 'Kiểm kê & điều chỉnh kho' },
  { value: 'SALES', label: 'Kinh doanh (SALES)', desc: 'Tạo đơn & xử lý khách' },
  { value: 'USER', label: 'Nhân viên (USER)', desc: 'Tiện ích cơ bản & chat' }
]

const SYSTEM_MODULES = [
  'Quản lý Tài khoản',
  'Kho & Sản phẩm',
  'Đơn hàng & Thanh toán',
  'Báo cáo & Phân tích BI',
  'Tác vụ tự động',
  'Giao tiếp & AI Copilot'
]

const SYSTEM_PERMISSIONS = [
  // Module 1: Quản lý Người dùng
  { code: 'USER_VIEW', name: 'Xem danh sách nhân sự', module: 'Quản lý Tài khoản' },
  { code: 'USER_CREATE', name: 'Tạo tài khoản mới', module: 'Quản lý Tài khoản' },
  { code: 'USER_EDIT', name: 'Chỉnh sửa thông tin tài khoản', module: 'Quản lý Tài khoản' },
  { code: 'USER_STATUS', name: 'Khóa / Mở khóa tài khoản', module: 'Quản lý Tài khoản' },
  { code: 'USER_ROLE', name: 'Phân quyền & Gán vai trò', module: 'Quản lý Tài khoản' },

  // Module 2: Sản phẩm & Kho hàng
  { code: 'PRODUCT_VIEW', name: 'Xem danh mục sản phẩm', module: 'Kho & Sản phẩm' },
  { code: 'PRODUCT_CREATE', name: 'Thêm sản phẩm mới', module: 'Kho & Sản phẩm' },
  { code: 'PRODUCT_EDIT', name: 'Sửa giá & thông tin sản phẩm', module: 'Kho & Sản phẩm' },
  { code: 'PRODUCT_STOCK', name: 'Điều chỉnh xuất/nhập/tồn kho', module: 'Kho & Sản phẩm' },
  { code: 'PRODUCT_DELETE', name: 'Xóa sản phẩm khỏi kho', module: 'Kho & Sản phẩm' },

  // Module 3: Đơn hàng & Cổng VietQR
  { code: 'ORDER_VIEW', name: 'Xem danh sách đơn hàng', module: 'Đơn hàng & Thanh toán' },
  { code: 'ORDER_CREATE', name: 'Tạo mới đơn hàng', module: 'Đơn hàng & Thanh toán' },
  { code: 'ORDER_STATUS', name: 'Phê duyệt trạng thái đơn hàng', module: 'Đơn hàng & Thanh toán' },
  { code: 'ORDER_PAY', name: 'Xác nhận thanh toán & VietQR', module: 'Đơn hàng & Thanh toán' },
  { code: 'ORDER_EXPORT', name: 'Xuất hóa đơn & Báo cáo PDF/Excel', module: 'Đơn hàng & Thanh toán' },
  { code: 'ORDER_CANCEL', name: 'Hủy đơn hàng & hoàn trả kho', module: 'Đơn hàng & Thanh toán' },

  // Module 4: Báo cáo & Phân tích BI
  { code: 'ANALYTICS_VIEW', name: 'Xem báo cáo BI & Doanh thu', module: 'Báo cáo & Phân tích BI' },
  { code: 'ANALYTICS_EXPORT', name: 'Xuất file phân tích kinh doanh', module: 'Báo cáo & Phân tích BI' },

  // Module 5: Tác vụ tự động & Cron Jobs
  { code: 'JOB_VIEW', name: 'Xem danh sách Cron Jobs', module: 'Tác vụ tự động' },
  { code: 'JOB_TRIGGER', name: 'Kích hoạt chạy ngay (Trigger Now)', module: 'Tác vụ tự động' },
  { code: 'JOB_MANAGE', name: 'Tạo / Sửa / Xóa cấu hình Cron', module: 'Tác vụ tự động' },

  // Module 6: Trò chuyện & AI Copilot
  { code: 'CHAT_USE', name: 'Trò chuyện nội bộ công ty', module: 'Giao tiếp & AI Copilot' },
  { code: 'COPILOT_USE', name: 'Tương tác với trợ lý AI Copilot', module: 'Giao tiếp & AI Copilot' }
]

const rolePermissionsMap = ref({
  ADMIN: new Set(SYSTEM_PERMISSIONS.map(p => p.code)),
  MANAGER: new Set(),
  ACCOUNTANT: new Set(),
  WAREHOUSE: new Set(),
  SALES: new Set(),
  USER: new Set()
})

// Collapsible / Expandable Modules Management
const collapsedModules = ref(new Set())

const isModuleCollapsed = (moduleName) => {
  return collapsedModules.value.has(moduleName)
}

const toggleModuleCollapse = (moduleName) => {
  const next = new Set(collapsedModules.value)
  if (next.has(moduleName)) {
    next.delete(moduleName)
  } else {
    next.add(moduleName)
  }
  collapsedModules.value = next
}

const collapseAllModules = () => {
  collapsedModules.value = new Set(SYSTEM_MODULES)
}

const expandAllModules = () => {
  collapsedModules.value = new Set()
}

const allModulesCollapsed = computed(() => {
  return SYSTEM_MODULES.length > 0 && SYSTEM_MODULES.every(m => collapsedModules.value.has(m))
})

const toggleAllModules = () => {
  if (allModulesCollapsed.value) {
    expandAllModules()
  } else {
    collapseAllModules()
  }
}

const getModuleGrantedCount = (moduleName) => {
  const perms = SYSTEM_PERMISSIONS.filter(p => p.module === moduleName)
  let count = perms.length // ADMIN luôn có toàn bộ
  for (const role of ['MANAGER', 'ACCOUNTANT', 'WAREHOUSE', 'SALES', 'USER']) {
    const set = rolePermissionsMap.value[role]
    if (set) {
      for (const p of perms) {
        if (set.has(p.code)) count++
      }
    }
  }
  return count
}

// Tự động mở rộng tất cả phân hệ khi gõ từ khóa tìm kiếm
watch(permissionKeyword, (newKw) => {
  if (newKw && newKw.trim()) {
    expandAllModules()
  }
})

const filteredPermissionGroups = computed(() => {
  const kw = (permissionKeyword.value || '').trim().toLowerCase()
  const modFilter = selectedModuleFilter.value

  const groups = []
  for (const mod of SYSTEM_MODULES) {
    if (modFilter !== 'ALL' && modFilter !== mod) continue

    const matchedPerms = SYSTEM_PERMISSIONS.filter(p => {
      if (p.module !== mod) return false
      if (!kw) return true
      return p.name.toLowerCase().includes(kw) || p.code.toLowerCase().includes(kw)
    })

    if (matchedPerms.length > 0) {
      groups.push({
        module: mod,
        permissions: matchedPerms
      })
    }
  }
  return groups
})

const isRoleHasPermission = (role, code) => {
  if (role === 'ADMIN') return true
  const set = rolePermissionsMap.value[role]
  return set ? set.has(code) : false
}

const togglePermission = (role, code) => {
  if (role === 'ADMIN') return
  const currentSet = rolePermissionsMap.value[role] || new Set()
  const newSet = new Set(currentSet)
  if (newSet.has(code)) {
    newSet.delete(code)
  } else {
    newSet.add(code)
  }
  rolePermissionsMap.value[role] = newSet
  matrixDirty.value = true
}

const totalGrantedPermissionsCount = computed(() => {
  let count = SYSTEM_PERMISSIONS.length // Admin full
  for (const role of ['MANAGER', 'ACCOUNTANT', 'WAREHOUSE', 'SALES', 'USER']) {
    count += rolePermissionsMap.value[role]?.size || 0
  }
  return count
})

const handleQuickChangeRole = async (user, newRole) => {
  if (!user || user.role === newRole) return
  try {
    const res = await api.patch(`/users/${user.id}/role`, { role: newRole })
    user.role = newRole
    ElMessage.success(res.data?.message || `Đã chuyển vai trò @${user.username} thành ${getRoleDisplayName(newRole)}!`)
  } catch (err) {
    ElMessage.error(err.response?.data?.message || 'Không thể cập nhật vai trò!')
  }
}

const openRolesMatrixDrawer = async () => {
  matrixDrawerVisible.value = true
  matrixDirty.value = false
  try {
    const res = await api.get('/users/roles-matrix')
    if (Array.isArray(res.data)) {
      rolesMatrix.value = res.data
      for (const item of res.data) {
        if (item.role && Array.isArray(item.permissions)) {
          rolePermissionsMap.value[item.role] = new Set(item.permissions)
        }
      }
      // ADMIN luôn full
      rolePermissionsMap.value.ADMIN = new Set(SYSTEM_PERMISSIONS.map(p => p.code))
    }
  } catch (err) {
    console.warn('Fallback ma trận quyền cục bộ', err)
  }
}

const handleSaveMatrix = async () => {
  savingMatrix.value = true
  try {
    const payload = {}
    for (const [r, set] of Object.entries(rolePermissionsMap.value)) {
      payload[r] = Array.from(set)
    }
    const res = await api.put('/users/roles-matrix', payload)
    if (Array.isArray(res.data)) {
      rolesMatrix.value = res.data
      for (const item of res.data) {
        if (item.role && Array.isArray(item.permissions)) {
          rolePermissionsMap.value[item.role] = new Set(item.permissions)
        }
      }
    }
    matrixDirty.value = false
    ElMessage.success('Đã lưu cấu hình ma trận phân quyền RBAC thành công!')
  } catch (err) {
    ElMessage.error(err.response?.data?.message || 'Không thể lưu ma trận phân quyền!')
  } finally {
    savingMatrix.value = false
  }
}

const confirmResetMatrix = () => {
  ElMessageBox.confirm(
    'Bạn có chắc chắn muốn khôi phục toàn bộ Ma trận phân quyền về chuẩn mặc định của hệ thống?',
    'Khôi phục mặc định RBAC',
    {
      confirmButtonText: 'Khôi phục ngay',
      cancelButtonText: 'Hủy',
      type: 'warning'
    }
  ).then(async () => {
    try {
      const res = await api.post('/users/roles-matrix/reset')
      if (Array.isArray(res.data)) {
        rolesMatrix.value = res.data
        for (const item of res.data) {
          if (item.role && Array.isArray(item.permissions)) {
            rolePermissionsMap.value[item.role] = new Set(item.permissions)
          }
        }
      }
      matrixDirty.value = false
      ElMessage.success('Đã khôi phục ma trận phân quyền về chuẩn mặc định!')
    } catch (err) {
      ElMessage.error(err.response?.data?.message || 'Không thể khôi phục mặc định!')
    }
  }).catch(() => {})
}

// Create Dialog
const openCreateDialog = () => {
  createForm.value = {
    username: '',
    fullName: '',
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
    fullName: row.fullName || '',
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
      fullName: editForm.value.fullName,
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
  gap: 8px;
  padding-bottom: 64px;
}

.header-action-btns {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.btn-matrix-action {
  height: 40px;
  border-radius: 8px;
  font-weight: 600;
  font-size: 13px;
  color: #2563eb;
  background: #eff6ff;
  border: 1px solid #bfdbfe;
  transition: all 0.2s ease;
}

.btn-matrix-action:hover {
  background: #dbeafe;
  color: #1d4ed8;
  transform: translateY(-1px);
}

/* Filter Toolbar */
.toolbar-wrap {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 10px;
}

.filter-group {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  flex: 1 1 540px;
  min-width: 0;
}

.filter-search {
  flex: 1 1 220px;
  max-width: 280px;
  min-width: 180px;
}

.filter-select {
  width: 180px;
}

.filter-select.sm {
  width: 140px;
}

.btn-reset {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.count-tag {
  font-size: 12.5px;
  color: #64748b;
  white-space: nowrap;
}

/* User Profile Cell */
.user-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.user-cell-avatar {
  background: linear-gradient(135deg, #2563eb 0%, #7c3aed 100%);
  color: #ffffff;
  font-weight: 700;
  font-size: 13px;
  flex-shrink: 0;
  box-shadow: 0 2px 6px rgba(37, 99, 235, 0.2);
}

.user-cell-info {
  display: flex;
  flex-direction: column;
  line-height: 1.25;
  min-width: 0;
}

.user-name-row {
  display: flex;
  align-items: center;
  gap: 6px;
}

.user-name {
  font-weight: 700;
  font-size: 13px;
  color: #0f172a;
}

.user-fullname {
  font-size: 11.5px;
  color: #334155;
  font-weight: 500;
}

.current-badge {
  font-size: 10px;
  padding: 0 4px;
  height: 17px;
  line-height: 17px;
}

.user-email {
  font-size: 11px;
  color: #64748b;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* Role Badge Đa Sắc RBAC */
.role-pill-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 10px;
  border-radius: 20px;
  font-size: 11px;
  font-weight: 700;
  white-space: nowrap;
  cursor: pointer;
  transition: transform 0.15s ease;
}

.role-pill-badge:hover {
  transform: scale(1.04);
}

.role-admin {
  background: #fef2f2;
  color: #dc2626;
  border: 1px solid #fecaca;
}

.role-manager {
  background: #faf5ff;
  color: #7c3aed;
  border: 1px solid #e9d5ff;
}

.role-accountant {
  background: #f0fdfa;
  color: #0d9488;
  border: 1px solid #99f6e4;
}

.role-warehouse {
  background: #fffbeb;
  color: #d97706;
  border: 1px solid #fde68a;
}

.role-sales {
  background: #eff6ff;
  color: #2563eb;
  border: 1px solid #bfdbfe;
}

.role-user {
  background: #f8fafc;
  color: #475569;
  border: 1px solid #e2e8f0;
}

/* Status Cell */
.status-cell {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}

.status-toggle-btn {
  font-size: 12px;
  margin-left: 0 !important;
}

.created-date {
  font-size: 12px;
  color: #475569;
  white-space: nowrap;
}

.action-btn-group {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 5px;
  white-space: nowrap;
}

.action-btn-group :deep(.el-button + .el-button) {
  margin-left: 0;
}

.action-btn-group :deep(.el-button) {
  padding: 4px 8px;
  font-size: 12px;
}

/* Mobile List */
.users-mobile-list {
  display: none;
  flex-direction: column;
  gap: 10px;
}

.mobile-user-card {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 11px;
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.mu-top,
.mu-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.mu-meta {
  padding: 6px 0;
  border-top: 1px solid #f1f5f9;
  border-bottom: 1px solid #f1f5f9;
}

.mu-actions {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.mu-actions :deep(.el-button + .el-button) {
  margin-left: 0;
}

/* ========================================================================= */
/* INTERACTIVE RBAC MATRIX DIALOG & QUICK ROLE DROPDOWN                      */
/* ========================================================================= */
.role-pill-badge.clickable {
  cursor: pointer;
  transition: all 0.2s ease;
  user-select: none;
}
.role-pill-badge.clickable:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.08);
  filter: brightness(0.96);
}
.role-caret {
  font-size: 10px;
  margin-left: 4px;
  opacity: 0.6;
}

.quick-role-dropdown-menu {
  padding: 8px;
  min-width: 250px;
  border-radius: 12px;
  box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.12);
}
.quick-role-menu-header {
  font-size: 11px;
  font-weight: 700;
  color: #64748b;
  padding: 6px 10px;
  border-bottom: 1px dashed #f1f5f9;
  margin-bottom: 4px;
}
.quick-role-item-row {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 4px 0;
}
.q-icon { font-size: 16px; }
.q-info {
  display: flex;
  flex-direction: column;
  flex: 1;
}
.q-name {
  font-size: 13px;
  font-weight: 600;
  color: #0f172a;
}
.q-desc {
  font-size: 11px;
  color: #64748b;
}
.q-active-tick {
  color: #10b981;
  font-weight: 800;
  font-size: 13px;
}

/* Dialog Ma Trận Phân Quyền */
:deep(.rbac-matrix-dialog) {
  border-radius: 16px;
  overflow: hidden;
}
/* Dialog Layout & Viewport Constraint (Fit 100% in Screen, Zero Window Overflow) */
:deep(.rbac-matrix-dialog) {
  max-width: 1060px;
  width: 95vw;
  margin-bottom: 0 !important;
}
:deep(.rbac-matrix-dialog .el-dialog) {
  max-height: 94vh;
  display: flex;
  flex-direction: column;
  margin-top: 0 !important;
  top: 3vh;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 20px 40px -10px rgba(15, 23, 42, 0.25);
}
:deep(.rbac-matrix-dialog .el-dialog__header) {
  margin: 0;
  padding: 12px 18px;
  border-bottom: 1px solid #e2e8f0;
  flex-shrink: 0;
  background: #ffffff;
}
:deep(.rbac-matrix-dialog .el-dialog__title) {
  font-size: 15px;
  font-weight: 700;
  color: #0f172a;
}
:deep(.rbac-matrix-dialog .el-dialog__body) {
  padding: 10px 18px;
  background: #f8fafc;
  flex: 1 1 auto;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  min-height: 0;
}
:deep(.rbac-matrix-dialog .el-dialog__footer) {
  padding: 8px 18px;
  border-top: 1px solid #e2e8f0;
  background: #ffffff;
  flex-shrink: 0;
}

.matrix-dialog-container {
  display: flex;
  flex-direction: column;
  gap: 8px;
  height: 100%;
  flex: 1;
  min-height: 0;
}

.matrix-toolbar-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  flex-wrap: wrap;
  background: #ffffff;
  padding: 6px 10px;
  border-radius: 8px;
  border: 1px solid #e2e8f0;
  flex-shrink: 0;
}
.toolbar-search-box {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
  min-width: 260px;
}
.search-perm-input {
  max-width: 230px;
}
.module-filter-select {
  width: 170px;
}
.btn-toggle-all-modules {
  font-size: 11.5px;
  font-weight: 600;
  color: #334155;
  background: #f1f5f9;
  border: 1px solid #cbd5e1;
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 0 10px;
  height: 32px;
  border-radius: 6px;
  transition: all 0.15s ease;
}
.btn-toggle-all-modules:hover {
  background: #e2e8f0;
  color: #0f172a;
}
.btn-toggle-icon {
  font-size: 11px;
  color: #64748b;
}
.toolbar-btn-group {
  display: flex;
  align-items: center;
  gap: 8px;
}
.btn-save-matrix {
  background: linear-gradient(135deg, #2563eb 0%, #1d4ed8 100%);
  font-weight: 600;
  border: none;
}

.matrix-table-scroll-wrap {
  flex: 1 1 auto;
  min-height: 200px;
  max-height: calc(88vh - 165px);
  overflow-y: auto;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #ffffff;
}

.rbac-interactive-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 12px;
  text-align: left;
}
.rbac-interactive-table thead th {
  position: sticky;
  top: 0;
  z-index: 5;
  background: #f8fafc;
  color: #334155;
  font-weight: 700;
  padding: 8px 6px;
  border-bottom: 2px solid #e2e8f0;
}
.col-permission {
  width: 280px;
  min-width: 220px;
}
.col-role {
  width: 105px;
  border-left: 1px solid #f1f5f9;
}
.th-role-header {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
}

.btn-reset-matrix {
  background: transparent !important;
  border: 1px solid #cbd5e1;
  color: #475569;
  font-weight: 500;
}
.btn-reset-matrix:hover {
  background: #f8fafc !important;
  color: #dc2626;
  border-color: #fca5a5;
}

/* Role Title Badges (Phẳng hoàn toàn, Không màu nền) */
.role-title-tag {
  font-size: 11.5px;
  font-weight: 700;
  letter-spacing: 0.3px;
  display: inline-block;
  line-height: 1.3;
  background: transparent !important;
  border: none !important;
  padding: 0 !important;
  color: #0f172a;
}
.role-title-tag.tag-admin {
  color: #15803d;
}
.role-title-tag.tag-manager {
  color: #1d4ed8;
}
.role-title-tag.tag-accountant {
  color: #0e7490;
}
.role-title-tag.tag-warehouse {
  color: #b45309;
}
.role-title-tag.tag-sales {
  color: #be123c;
}
.role-title-tag.tag-user {
  color: #475569;
}
.role-badge-sub {
  font-size: 10px;
  font-weight: 500;
  color: #64748b;
}

/* Accordion Group Header Row */
.group-header-row {
  cursor: pointer;
  user-select: none;
  background: #f8fafc;
  transition: all 0.15s ease;
}
.group-header-row:hover {
  background: #f1f5f9;
}
.group-header-row.is-collapsed {
  background: #fbfcfe;
}
.group-header-cell {
  padding: 6px 14px;
  border-top: 1px solid #e2e8f0;
  border-bottom: 1px solid #e2e8f0;
}
.group-header-flex {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
}
.gh-left {
  display: flex;
  align-items: center;
  gap: 8px;
}
.group-name {
  font-size: 11.5px;
  font-weight: 700;
  color: #0f172a;
  letter-spacing: 0.3px;
  text-transform: uppercase;
}
.group-count-tag {
  font-size: 10px;
  font-weight: 600;
  color: #475569;
  background: #e2e8f0;
  padding: 1px 6px;
  border-radius: 10px;
}
.group-granted-tag {
  font-size: 10px;
  font-weight: 600;
  color: #0284c7;
  background: #e0f2fe;
  padding: 1px 6px;
  border-radius: 10px;
}
.gh-right {
  display: flex;
  align-items: center;
}
.gh-collapse-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 6px;
  color: #475569;
  background: #ffffff;
  border: 1px solid #cbd5e1;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
}
.gh-collapse-icon.is-collapsed {
  transform: rotate(-90deg);
}
.group-header-row:hover .gh-collapse-icon {
  color: #2563eb;
  border-color: #3b82f6;
  background: #eff6ff;
  box-shadow: 0 1px 4px rgba(37, 99, 235, 0.15);
}

/* Perm Row */
.perm-row {
  border-bottom: 1px solid #f1f5f9;
  transition: background 0.12s ease;
}
.perm-row:hover {
  background: #f8fafc;
}
.perm-desc-cell {
  padding: 5px 10px;
  line-height: 1.25;
}
.perm-name-txt {
  font-size: 12px;
  font-weight: 600;
  color: #0f172a;
  margin-bottom: 1px;
}
.perm-code-tag {
  font-size: 9.5px;
  font-family: 'Consolas', monospace;
  color: #64748b;
  background: #f1f5f9;
  padding: 1px 4px;
  border-radius: 3px;
  display: inline-block;
}

.perm-check-cell {
  padding: 3px 6px;
  border-left: 1px solid #f1f5f9;
  cursor: pointer;
  user-select: none;
  vertical-align: middle;
  transition: background 0.12s ease;
}
.perm-check-cell:not(.admin):hover {
  background: #f0f7ff;
}
.perm-check-cell.admin {
  cursor: default;
  background: #fafafa;
}

.fixed-check-pill {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  background: #dcfce7;
  color: #15803d;
  font-size: 10px;
  font-weight: 700;
  padding: 2px 6px;
  border-radius: 4px;
  border: 1px solid #bbf7d0;
}

/* Custom Checkbox click toggle */
.custom-chk-box {
  width: 19px;
  height: 19px;
  border: 1.5px solid #cbd5e1;
  border-radius: 4px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: #ffffff;
  transition: all 0.12s ease;
  margin: 0 auto;
}
.perm-check-cell:hover .custom-chk-box:not(.checked) {
  border-color: #3b82f6;
  background: #eff6ff;
  transform: scale(1.05);
}
.custom-chk-box.checked {
  background: #2563eb;
  border-color: #2563eb;
  box-shadow: 0 1px 4px rgba(37, 99, 235, 0.3);
}
.chk-svg {
  color: #ffffff;
}
.perm-check-cell:hover .custom-chk-box:not(.checked) {
  border-color: #3b82f6;
  background: #eff6ff;
}
.custom-chk-box.checked {
  background: #2563eb;
  border-color: #2563eb;
  box-shadow: 0 1px 3px rgba(37, 99, 235, 0.25);
}
.chk-tick {
  color: #ffffff;
  font-size: 11px;
  font-weight: 900;
  line-height: 1;
}

.matrix-dialog-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
}
.footer-status-text {
  font-size: 11.5px;
  color: #475569;
  display: flex;
  align-items: center;
  gap: 8px;
}
.dirty-badge {
  background: #fef3c7;
  color: #b45309;
  font-size: 10.5px;
  font-weight: 700;
  padding: 1px 5px;
  border-radius: 3px;
  border: 1px solid #fde68a;
}
.footer-btns {
  display: flex;
  gap: 8px;
}

@media (max-width: 768px) {
  .users-desktop-table :deep(.table-responsive-wrapper) {
    display: none;
  }

  .users-mobile-list {
    display: flex;
  }

  .filter-search,
  .filter-select,
  .btn-add,
  .btn-matrix-action {
    width: 100%;
    max-width: 100%;
  }

  .filter-group {
    width: 100%;
  }
}
</style>
