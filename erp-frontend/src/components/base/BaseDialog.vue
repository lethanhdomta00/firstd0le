<template>
  <el-dialog
    :model-value="modelValue"
    :title="title"
    :width="dialogWidth"
    :before-close="handleClose"
    destroy-on-close
    class="base-dialog"
    v-bind="$attrs"
  >
    <template #header>
      <div class="dialog-header-wrap">
        <h3 class="dialog-title">{{ title }}</h3>
        <p v-if="subtitle" class="dialog-subtitle">{{ subtitle }}</p>
      </div>
    </template>

    <div class="dialog-body-wrap">
      <slot></slot>
    </div>

    <template #footer v-if="showFooter">
      <slot name="footer">
        <div class="dialog-default-footer">
          <el-button @click="handleCancel" size="default">
            {{ cancelText }}
          </el-button>
          <el-button 
            :type="confirmType" 
            :loading="loading" 
            @click="$emit('confirm')" 
            size="default"
          >
            {{ confirmText }}
          </el-button>
        </div>
      </slot>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed } from 'vue'
import { useResponsive } from '../../composables/useResponsive'

const props = defineProps({
  modelValue: {
    type: Boolean,
    required: true
  },
  title: {
    type: String,
    required: true
  },
  subtitle: {
    type: String,
    default: ''
  },
  width: {
    type: String,
    default: '560px'
  },
  loading: {
    type: Boolean,
    default: false
  },
  confirmText: {
    type: String,
    default: 'Lưu thay đổi'
  },
  cancelText: {
    type: String,
    default: 'Hủy'
  },
  confirmType: {
    type: String,
    default: 'primary'
  },
  showFooter: {
    type: Boolean,
    default: true
  }
})

const emit = defineEmits(['update:modelValue', 'confirm', 'cancel'])

const { isMobile } = useResponsive()

const dialogWidth = computed(() => {
  return isMobile.value ? '92%' : props.width
})

const handleClose = () => {
  emit('update:modelValue', false)
}

const handleCancel = () => {
  emit('cancel')
  emit('update:modelValue', false)
}
</script>

<style scoped>
.dialog-header-wrap {
  padding-bottom: 4px;
}

.dialog-title {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  color: #0f172a;
}

.dialog-subtitle {
  margin: 4px 0 0 0;
  font-size: 13px;
  color: #64748b;
}

.dialog-body-wrap {
  max-height: 72vh;
  overflow-y: auto;
  padding: 4px 0;
}

.dialog-default-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

@media (max-width: 768px) {
  .dialog-default-footer {
    flex-direction: column;
  }

  .dialog-default-footer .el-button {
    width: 100%;
    margin-left: 0 !important;
  }
}
</style>

