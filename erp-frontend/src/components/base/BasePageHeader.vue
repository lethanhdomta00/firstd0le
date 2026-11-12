<template>
  <div class="base-page-header">
    <div class="header-info">
      <div v-if="icon" class="header-icon-box">
        <component :is="icon" />
      </div>
      <div>
        <h2 class="header-title">{{ title }}</h2>
        <p v-if="subtitle" class="header-subtitle">{{ subtitle }}</p>
      </div>
    </div>

    <div v-if="$slots.default || $slots.actions" class="header-actions">
      <slot name="actions">
        <slot></slot>
      </slot>
    </div>
  </div>
</template>

<script setup>
defineProps({
  title: {
    type: String,
    required: true
  },
  subtitle: {
    type: String,
    default: ''
  },
  icon: {
    type: [Object, Function],
    default: null
  }
})
</script>

<style scoped>
.base-page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 12px;
}

.header-info {
  display: flex;
  align-items: center;
  gap: 10px;
  flex: 1;
  min-width: 240px;
}

.header-icon-box {
  width: 38px;
  height: 38px;
  border-radius: 10px;
  background: #e0f2fe;
  color: #0284c7;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 19px;
  flex-shrink: 0;
}

.header-title {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  color: #0f172a;
  letter-spacing: -0.3px;
}

.header-subtitle {
  margin: 2px 0 0 0;
  font-size: 12.5px;
  color: #64748b;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

@media (max-width: 768px) {
  .base-page-header {
    flex-direction: column;
    align-items: stretch;
    gap: 14px;
    margin-bottom: 16px;
  }

  .header-title {
    font-size: 19px;
  }

  .header-actions {
    width: 100%;
  }

  .header-actions > * {
    flex: 1;
  }
}
</style>

