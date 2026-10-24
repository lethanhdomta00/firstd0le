<template>
  <el-button
    :type="isGradient ? 'primary' : type"
    :size="size"
    :loading="loading"
    :disabled="disabled"
    :plain="plain"
    :class="[
      'base-button',
      { 'is-gradient': isGradient, 'is-block': block }
    ]"
    v-bind="$attrs"
  >
    <el-icon v-if="icon && !loading" class="btn-icon">
      <component :is="icon" />
    </el-icon>
    <slot></slot>
  </el-button>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  type: {
    type: String,
    default: 'default' // primary, success, warning, danger, info, default, gradient
  },
  size: {
    type: String,
    default: 'default' // small, default, large
  },
  loading: {
    type: Boolean,
    default: false
  },
  disabled: {
    type: Boolean,
    default: false
  },
  plain: {
    type: Boolean,
    default: false
  },
  icon: {
    type: [Object, Function],
    default: null
  },
  block: {
    type: Boolean,
    default: false
  }
})

const isGradient = computed(() => props.type === 'gradient')
</script>

<style scoped>
.base-button {
  font-weight: 500;
  border-radius: 10px;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}

.base-button:hover:not(:disabled) {
  transform: translateY(-1px);
}

.base-button:active:not(:disabled) {
  transform: translateY(0);
}

.base-button.is-block {
  width: 100%;
}

.base-button.is-gradient {
  background: linear-gradient(135deg, #0284c7 0%, #0369a1 100%);
  border: none;
  color: #ffffff;
  box-shadow: 0 4px 14px rgba(2, 132, 199, 0.35);
}

.base-button.is-gradient:hover:not(:disabled) {
  background: linear-gradient(135deg, #0369a1 0%, #075985 100%);
  box-shadow: 0 6px 18px rgba(2, 132, 199, 0.5);
}

.btn-icon {
  font-size: 16px;
}

@media (max-width: 768px) {
  .base-button {
    min-height: 38px;
  }
}
</style>

