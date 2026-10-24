<template>
  <div class="dashboard-page">
    <div class="dashboard-header">
      <h2 class="section-title">Tổng quan Hệ thống</h2>
      <p class="section-subtitle">Chỉ số kinh doanh và hoạt động của doanh nghiệp</p>
    </div>

    <!-- Stat Cards with Responsive Columns -->
    <el-row :gutter="16" class="stats-row">
      <el-col :xs="24" :sm="12" :md="8" class="stat-col">
        <el-card shadow="hover" class="stat-card blue-card">
          <div class="stat-inner">
            <div>
              <span class="stat-label">Tổng Sản phẩm</span>
              <div class="stat-number">120</div>
              <span class="stat-trend positive">↑ 12% so với tháng trước</span>
            </div>
            <div class="stat-icon-box blue-icon">
              <el-icon><Goods /></el-icon>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="12" :md="8" class="stat-col">
        <el-card shadow="hover" class="stat-card green-card">
          <div class="stat-inner">
            <div>
              <span class="stat-label">Đơn hàng mới</span>
              <div class="stat-number">45</div>
              <span class="stat-trend positive">↑ 8 đơn hàng hôm nay</span>
            </div>
            <div class="stat-icon-box green-icon">
              <el-icon><Tickets /></el-icon>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="12" :md="8" class="stat-col">
        <el-card shadow="hover" class="stat-card purple-card">
          <div class="stat-inner">
            <div>
              <span class="stat-label">Tổng Tài khoản</span>
              <div class="stat-number">12</div>
              <span class="stat-trend neutral">Hệ thống phân quyền Role</span>
            </div>
            <div class="stat-icon-box purple-icon">
              <el-icon><User /></el-icon>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- Quick Action / Info Card -->
    <el-card shadow="never" class="quick-action-card">
      <template #header>
        <div class="card-header-row">
          <span class="card-header-title">Hành động nhanh</span>
        </div>
      </template>
      <div class="quick-btn-group">
        <el-button type="primary" size="default" @click="$router.push('/products')">
          <el-icon style="margin-right: 6px;"><Goods /></el-icon> Quản lý Sản phẩm
        </el-button>
        <el-button type="success" size="default" @click="$router.push('/users')">
          <el-icon style="margin-right: 6px;"><User /></el-icon> Quản lý Tài khoản
        </el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { Goods, Tickets, User } from '@element-plus/icons-vue'
</script>

<style scoped>
.dashboard-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.dashboard-header {
  margin-bottom: 4px;
}

.section-title {
  margin: 0;
  font-size: 22px;
  font-weight: 700;
  color: #0f172a;
}

.section-subtitle {
  margin: 4px 0 0 0;
  font-size: 13px;
  color: #64748b;
}

.stats-row {
  margin-bottom: 8px;
}

.stat-col {
  margin-bottom: 16px;
}

.stat-card {
  border-radius: 14px;
  border: 1px solid #e2e8f0;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 10px 20px -5px rgba(0, 0, 0, 0.08);
}

.stat-inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.stat-label {
  font-size: 13px;
  font-weight: 500;
  color: #64748b;
}

.stat-number {
  font-size: 32px;
  font-weight: 800;
  color: #0f172a;
  margin: 4px 0;
  line-height: 1.1;
}

.stat-trend {
  font-size: 12px;
  display: block;
}

.stat-trend.positive {
  color: #10b981;
}

.stat-trend.neutral {
  color: #64748b;
}

.stat-icon-box {
  width: 52px;
  height: 52px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  flex-shrink: 0;
}

.blue-icon {
  background-color: #e0f2fe;
  color: #0284c7;
}

.green-icon {
  background-color: #dcfce7;
  color: #16a34a;
}

.purple-icon {
  background-color: #f3e8ff;
  color: #9333ea;
}

/* Quick Action Card */
.quick-action-card {
  border-radius: 14px;
  border: 1px solid #e2e8f0;
}

.card-header-title {
  font-weight: 600;
  font-size: 15px;
  color: #1e293b;
}

.quick-btn-group {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

@media (max-width: 768px) {
  .section-title {
    font-size: 18px;
  }

  .stat-number {
    font-size: 26px;
  }

  .quick-btn-group {
    flex-direction: column;
  }

  .quick-btn-group .el-button {
    width: 100%;
    margin-left: 0 !important;
  }
}
</style>
