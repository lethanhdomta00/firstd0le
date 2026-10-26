<template>
  <div v-if="loading" class="base-skeleton-wrapper" :class="[`type-${type}`]">
    <!-- TYPE 1: TABLE SKELETON -->
    <div v-if="type === 'table'" class="skeleton-table">
      <!-- Skeleton Header -->
      <div v-if="showHeader" class="skeleton-table-header">
        <div 
          v-for="col in columns" 
          :key="'th-' + col" 
          class="skeleton-cell skeleton-th"
          :style="{ width: getHeaderColWidth(col) }"
        >
          <div class="skeleton-bar th-bar"></div>
        </div>
      </div>

      <!-- Skeleton Rows -->
      <div class="skeleton-table-body">
        <div 
          v-for="r in rows" 
          :key="'tr-' + r" 
          class="skeleton-table-row"
          :class="{ 'is-striped': striped && r % 2 === 0 }"
        >
          <div 
            v-for="col in columns" 
            :key="'td-' + r + '-' + col" 
            class="skeleton-cell"
            :style="{ width: getBodyColWidth(col) }"
          >
            <!-- First column: optional checkbox/ID circle or small bar -->
            <div v-if="col === 1" class="skeleton-badge-pill sm"></div>
            <!-- Second column: optional thumbnail or avatar placeholder -->
            <div v-else-if="col === 2 && hasThumbnail" class="skeleton-thumb-box"></div>
            <!-- Last column: action button placeholder -->
            <div v-else-if="col === columns" class="skeleton-action-buttons">
              <div class="skeleton-btn"></div>
              <div class="skeleton-btn"></div>
            </div>
            <!-- Normal cell: text bar of varied widths -->
            <div v-else class="skeleton-bar" :style="{ width: getRandomWidth(r, col) }"></div>
          </div>
        </div>
      </div>
    </div>

    <!-- TYPE 2: CARD / GRID SKELETON -->
    <div v-else-if="type === 'card' || type === 'grid'" class="skeleton-card-grid">
      <div v-for="c in rows" :key="'card-' + c" class="skeleton-card">
        <div v-if="hasThumbnail" class="skeleton-card-image"></div>
        <div class="skeleton-card-body">
          <div class="skeleton-bar" style="width: 75%; height: 16px;"></div>
          <div class="skeleton-bar" style="width: 50%; height: 12px; margin-top: 6px;"></div>
          <div class="skeleton-bar" style="width: 90%; height: 12px; margin-top: 10px;"></div>
          <div class="skeleton-card-footer">
            <div class="skeleton-badge-pill"></div>
            <div class="skeleton-btn"></div>
          </div>
        </div>
      </div>
    </div>

    <!-- TYPE 3: LIST ITEMS SKELETON -->
    <div v-else-if="type === 'list'" class="skeleton-list">
      <div v-for="l in rows" :key="'list-' + l" class="skeleton-list-item">
        <div class="skeleton-avatar"></div>
        <div class="skeleton-list-content">
          <div class="skeleton-bar" style="width: 45%; height: 15px;"></div>
          <div class="skeleton-bar" style="width: 70%; height: 12px; margin-top: 6px;"></div>
        </div>
      </div>
    </div>

    <!-- TYPE 4: STAT CARD SKELETON -->
    <div v-else-if="type === 'stat'" class="skeleton-stat-grid">
      <div v-for="s in rows" :key="'stat-' + s" class="skeleton-stat-card">
        <div class="skeleton-stat-header">
          <div class="skeleton-bar" style="width: 50%; height: 14px;"></div>
          <div class="skeleton-stat-icon"></div>
        </div>
        <div class="skeleton-bar" style="width: 40%; height: 26px; margin: 12px 0 6px 0;"></div>
        <div class="skeleton-bar" style="width: 70%; height: 12px;"></div>
      </div>
    </div>
  </div>

  <!-- CONTENT REVEAL (WHEN NOT LOADING) -->
  <slot v-else></slot>
</template>

<script setup>
const props = defineProps({
  loading: {
    type: Boolean,
    default: true
  },
  type: {
    type: String,
    default: 'table', // 'table', 'card', 'grid', 'list', 'stat'
    validator: (val) => ['table', 'card', 'grid', 'list', 'stat'].includes(val)
  },
  rows: {
    type: Number,
    default: 5
  },
  columns: {
    type: Number,
    default: 5
  },
  showHeader: {
    type: Boolean,
    default: true
  },
  hasThumbnail: {
    type: Boolean,
    default: true
  },
  striped: {
    type: Boolean,
    default: true
  }
})

// Width distribution helpers
const getHeaderColWidth = (col) => {
  if (col === 1) return '60px'
  if (col === 2 && props.hasThumbnail) return '90px'
  if (col === props.columns) return '120px'
  return 'auto'
}

const getBodyColWidth = (col) => {
  if (col === 1) return '60px'
  if (col === 2 && props.hasThumbnail) return '90px'
  if (col === props.columns) return '120px'
  return 'auto'
}

const getRandomWidth = (r, col) => {
  const seeds = ['55%', '70%', '85%', '60%', '75%', '90%', '65%']
  return seeds[(r * 3 + col) % seeds.length]
}
</script>

<style scoped>
.base-skeleton-wrapper {
  width: 100%;
  animation: fadeIn 0.25s ease;
}

/* Base Shimmer Animation */
.skeleton-bar,
.skeleton-badge-pill,
.skeleton-thumb-box,
.skeleton-avatar,
.skeleton-btn,
.skeleton-card-image,
.skeleton-stat-icon {
  background: linear-gradient(
    90deg,
    #f1f5f9 0%,
    #e2e8f0 40%,
    #f8fafc 60%,
    #f1f5f9 100%
  );
  background-size: 200% 100%;
  animation: shimmer 1.6s infinite ease-in-out;
  border-radius: 6px;
}

/* TABLE SKELETON */
.skeleton-table {
  width: 100%;
  border-radius: 10px;
  overflow: hidden;
  background: #ffffff;
}

.skeleton-table-header {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  background: #f8fafc;
  border-bottom: 1.5px solid #e2e8f0;
}

.skeleton-th .th-bar {
  height: 14px;
  width: 60%;
  background-color: #cbd5e1;
}

.skeleton-table-body {
  display: flex;
  flex-direction: column;
}

.skeleton-table-row {
  display: flex;
  align-items: center;
  padding: 14px 16px;
  border-bottom: 1px solid #f1f5f9;
  transition: background 0.2s ease;
}

.skeleton-table-row.is-striped {
  background: #fafafa;
}

.skeleton-cell {
  flex: 1;
  display: flex;
  align-items: center;
  padding: 0 8px;
}

.skeleton-cell:first-child {
  flex: 0 0 60px;
  justify-content: center;
}

.skeleton-cell:last-child {
  flex: 0 0 120px;
  justify-content: flex-end;
}

.skeleton-bar {
  height: 14px;
  max-width: 100%;
}

.skeleton-badge-pill {
  width: 38px;
  height: 18px;
  border-radius: 10px;
}

.skeleton-thumb-box {
  width: 44px;
  height: 44px;
  border-radius: 8px;
  flex-shrink: 0;
}

.skeleton-action-buttons {
  display: flex;
  gap: 6px;
}

.skeleton-btn {
  width: 46px;
  height: 26px;
  border-radius: 6px;
}

/* CARD / GRID SKELETON */
.skeleton-card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 16px;
  padding: 4px;
}

.skeleton-card {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

.skeleton-card-image {
  width: 100%;
  height: 140px;
  border-radius: 0;
}

.skeleton-card-body {
  padding: 14px;
}

.skeleton-card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 14px;
  padding-top: 10px;
  border-top: 1px solid #f1f5f9;
}

/* LIST SKELETON */
.skeleton-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.skeleton-list-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
}

.skeleton-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  flex-shrink: 0;
}

.skeleton-list-content {
  flex: 1;
}

/* STAT SKELETON */
.skeleton-stat-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 16px;
}

.skeleton-stat-card {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 14px;
  padding: 18px;
}

.skeleton-stat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.skeleton-stat-icon {
  width: 36px;
  height: 36px;
  border-radius: 10px;
}

/* Shimmer Keyframes */
@keyframes shimmer {
  0% {
    background-position: -200% 0;
  }
  100% {
    background-position: 200% 0;
  }
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}
</style>

