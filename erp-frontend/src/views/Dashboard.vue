<template>
  <div>
    <h2>Dashboard</h2>
    <el-row :gutter="20">
      <el-col :span="8">
        <el-card shadow="hover">
          <h3>Total Products</h3>
          <div class="stat-value">120</div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover">
          <h3>Total Orders</h3>
          <div class="stat-value">45</div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover">
          <h3>Total Users</h3>
          <div class="stat-value">12</div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<style scoped>
.stat-value {
  font-size: 36px;
  font-weight: bold;
  color: #409EFF;
}
</style>

