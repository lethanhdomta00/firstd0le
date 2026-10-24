<template>
  <span :class="['base-status-badge', type]">
    <span v-if="dot" class="status-dot"></span>
    <span class="status-text"><slot>{{ text }}</slot></span>
  </span>
</template>

<script setup>
defineProps({
  type: {
    type: String,
    default: 'info' // success, warning, danger, info
  },
  text: {
    type: String,
    default: ''
  },
  dot: {
    type: Boolean,
    default: true
  }
})
</script>

<style scoped>
.base-status-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 3px 10px;
  border-radius: 9999px;
  font-size: 12px;
  font-weight: 600;
  white-space: nowrap;
}

.status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  flex-shrink: 0;
}

/* Success */
.base-status-badge.success {
  background-color: #ecfdf5;
  color: #065f46;
  border: 1px solid #a7f3d0;
}
.base-status-badge.success .status-dot {
  background-color: #10b981;
}

/* Warning */
.base-status-badge.warning {
  background-color: #fffbeb;
  color: #92400e;
  border: 1px solid #fde68a;
}
.base-status-badge.warning .status-dot {
  background-color: #f59e0b;
}

/* Danger */
.base-status-badge.danger {
  background-color: #fef2f2;
  color: #991b1b;
  border: 1px solid #fecaca;
}
.base-status-badge.danger .status-dot {
  background-color: #ef4444;
}

/* Info */
.base-status-badge.info {
  background-color: #f0f9ff;
  color: #075985;
  border: 1px solid #bae6fd;
}
.base-status-badge.info .status-dot {
  background-color: #0284c7;
}
</style>

