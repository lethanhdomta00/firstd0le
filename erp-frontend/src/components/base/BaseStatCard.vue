<template>
  <el-card shadow="hover" :class="['base-stat-card', `${variant}-variant`]">
    <div class="stat-card-inner">
      <div class="stat-info">
        <span class="stat-title">{{ title }}</span>
        <div class="stat-value">{{ value }}</div>
        <span v-if="trend" :class="['stat-trend', trendType]">
          {{ trend }}
        </span>
      </div>

      <div v-if="icon" :class="['stat-icon-wrapper', `${variant}-icon`]">
        <component :is="icon" />
      </div>
    </div>
  </el-card>
</template>

<script setup>
defineProps({
  title: {
    type: String,
    required: true
  },
  value: {
    type: [String, Number],
    required: true
  },
  trend: {
    type: String,
    default: ''
  },
  trendType: {
    type: String,
    default: 'positive' // positive, negative, neutral
  },
  icon: {
    type: [Object, Function],
    default: null
  },
  variant: {
    type: String,
    default: 'blue' // blue, green, purple, amber, rose
  }
})
</script>

<style scoped>
.base-stat-card {
  border-radius: 14px;
  border: 1px solid #e2e8f0;
  transition: all 0.25s ease;
}

.base-stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 10px 24px -6px rgba(0, 0, 0, 0.08);
}

.stat-card-inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.stat-info {
  flex: 1;
}

.stat-title {
  font-size: 13px;
  font-weight: 500;
  color: #64748b;
  display: block;
}

.stat-value {
  font-size: 30px;
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

.stat-trend.negative {
  color: #ef4444;
}

.stat-trend.neutral {
  color: #64748b;
}

.stat-icon-wrapper {
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

.amber-icon {
  background-color: #fef3c7;
  color: #d97706;
}

.rose-icon {
  background-color: #ffe4e6;
  color: #e11d48;
}

@media (max-width: 768px) {
  .stat-value {
    font-size: 24px;
  }
}
</style>

