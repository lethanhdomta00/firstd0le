<template>
  <div class="dashboard-page">
    <!-- Reusable Base Page Header -->
    <BasePageHeader
      title="Tổng quan Hệ thống"
      subtitle="Chỉ số kinh doanh và hoạt động của doanh nghiệp"
      :icon="Monitor"
    />

    <!-- Reusable Base Stat Cards with Responsive Columns -->
    <el-row :gutter="16" class="stats-row">
      <el-col :xs="24" :sm="12" :md="8" class="stat-col">
        <BaseStatCard
          title="Tổng Sản phẩm"
          value="120"
          trend="↑ 12% so với tháng trước"
          trend-type="positive"
          :icon="Goods"
          variant="blue"
        />
      </el-col>

      <el-col :xs="24" :sm="12" :md="8" class="stat-col">
        <BaseStatCard
          title="Đơn hàng mới"
          value="45"
          trend="↑ 8 đơn hàng hôm nay"
          trend-type="positive"
          :icon="Tickets"
          variant="green"
        />
      </el-col>

      <el-col :xs="24" :sm="12" :md="8" class="stat-col">
        <BaseStatCard
          title="Tổng Tài khoản"
          value="12"
          trend="Hệ thống phân quyền Role"
          trend-type="neutral"
          :icon="User"
          variant="purple"
        />
      </el-col>
    </el-row>

    <!-- Quick Action Card -->
    <el-card shadow="never" class="quick-action-card">
      <template #header>
        <div class="card-header-row">
          <span class="card-header-title">Hành động nhanh</span>
        </div>
      </template>
      <div class="quick-btn-group">
        <BaseButton type="primary" :icon="Goods" @click="$router.push('/products')">
          Quản lý Sản phẩm
        </BaseButton>
        <BaseButton type="success" :icon="User" @click="$router.push('/users')">
          Quản lý Tài khoản
        </BaseButton>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { Monitor, Goods, Tickets, User } from '@element-plus/icons-vue'
</script>

<style scoped>
.dashboard-page {
  display: flex;
  flex-direction: column;
}

.stats-row {
  margin-bottom: 8px;
}

.stat-col {
  margin-bottom: 16px;
}

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
  .quick-btn-group {
    flex-direction: column;
  }

  .quick-btn-group .base-button {
    width: 100%;
    margin-left: 0 !important;
  }
}
</style>
