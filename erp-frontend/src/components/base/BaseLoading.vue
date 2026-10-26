<template>
  <!-- WRAPPER MODE (WHEN DEFAULT SLOT IS PROVIDED) -->
  <div v-if="$slots.default" class="base-loading-container" :class="{ 'is-loading': active }">
    <slot></slot>
    <transition name="loading-fade">
      <div 
        v-if="active" 
        class="base-loading-overlay" 
        :class="[
          { 'is-fullscreen': fullscreen, 'has-blur': blur },
          `size-${size}`
        ]"
      >
        <div class="loading-content">
          <div class="spinner-ring-wrap">
            <div class="spinner-glow"></div>
            <div class="spinner-ring"></div>
            <div class="spinner-inner-dot"></div>
          </div>
          <p v-if="text" class="loading-text">{{ text }}</p>
          <span v-if="subText" class="loading-subtext">{{ subText }}</span>
        </div>
      </div>
    </transition>
  </div>

  <!-- STANDALONE / OVERLAY MODE (NO SLOT) -->
  <transition v-else name="loading-fade">
    <div 
      v-if="active" 
      class="base-loading-overlay" 
      :class="[
        { 'is-fullscreen': fullscreen, 'has-blur': blur },
        `size-${size}`
      ]"
    >
      <div class="loading-content">
        <div class="spinner-ring-wrap">
          <div class="spinner-glow"></div>
          <div class="spinner-ring"></div>
          <div class="spinner-inner-dot"></div>
        </div>
        <p v-if="text" class="loading-text">{{ text }}</p>
        <span v-if="subText" class="loading-subtext">{{ subText }}</span>
      </div>
    </div>
  </transition>
</template>

<script setup>
defineProps({
  active: {
    type: Boolean,
    default: false
  },
  text: {
    type: String,
    default: 'Đang xử lý...'
  },
  subText: {
    type: String,
    default: ''
  },
  fullscreen: {
    type: Boolean,
    default: false
  },
  blur: {
    type: Boolean,
    default: true
  },
  size: {
    type: String,
    default: 'md', // 'sm', 'md', 'lg'
    validator: (val) => ['sm', 'md', 'lg'].includes(val)
  }
})
</script>

<style scoped>
.base-loading-container {
  position: relative;
  width: 100%;
}

.base-loading-overlay {
  position: absolute;
  inset: 0;
  z-index: 999;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.88);
  border-radius: inherit;
  transition: all 0.25s ease;
}

.base-loading-overlay.has-blur {
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
}

.base-loading-overlay.is-fullscreen {
  position: fixed;
  z-index: 9999;
  border-radius: 0;
}

.loading-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 20px;
  text-align: center;
}

/* Spinner Visuals */
.spinner-ring-wrap {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
}

.size-sm .spinner-ring-wrap { width: 32px; height: 32px; }
.size-md .spinner-ring-wrap { width: 48px; height: 48px; }
.size-lg .spinner-ring-wrap { width: 64px; height: 64px; }

.spinner-glow {
  position: absolute;
  inset: -6px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(99, 102, 241, 0.3) 0%, transparent 70%);
  animation: pulse-glow 2s infinite ease-in-out;
}

.spinner-ring {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  border: 3px solid rgba(99, 102, 241, 0.18);
  border-top-color: #4f46e5;
  border-right-color: #0284c7;
  animation: spin 0.9s cubic-bezier(0.5, 0.1, 0.4, 0.9) infinite;
  box-shadow: 0 0 12px rgba(99, 102, 241, 0.25);
}

.size-sm .spinner-ring { border-width: 2.5px; }
.size-lg .spinner-ring { border-width: 4px; }

.spinner-inner-dot {
  position: absolute;
  width: 6px;
  height: 6px;
  background-color: #0284c7;
  border-radius: 50%;
  box-shadow: 0 0 8px #0284c7;
  animation: pulse-dot 1.4s infinite ease-in-out;
}

.size-lg .spinner-inner-dot {
  width: 8px;
  height: 8px;
}

/* Typography */
.loading-text {
  margin: 0;
  font-size: 14px;
  font-weight: 700;
  color: #0f172a;
  letter-spacing: 0.3px;
}

.size-sm .loading-text { font-size: 12px; }
.size-lg .loading-text { font-size: 16px; }

.loading-subtext {
  font-size: 12px;
  color: #64748b;
  max-width: 280px;
  line-height: 1.4;
}

/* Animations */
@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

@keyframes pulse-glow {
  0%, 100% { opacity: 0.3; transform: scale(0.95); }
  50% { opacity: 0.8; transform: scale(1.15); }
}

@keyframes pulse-dot {
  0%, 100% { opacity: 0.4; transform: scale(0.8); }
  50% { opacity: 1; transform: scale(1.2); }
}

/* Fade Transition */
.loading-fade-enter-active,
.loading-fade-leave-active {
  transition: opacity 0.25s ease;
}

.loading-fade-enter-from,
.loading-fade-leave-to {
  opacity: 0;
}
</style>

