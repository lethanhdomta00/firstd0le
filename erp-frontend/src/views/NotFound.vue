<template>
  <div class="not-found-page">
    <div class="not-found-card">
      <!-- 404 Status Badge -->
      <div class="badge-row">
        <span class="status-code-badge">404 NOT FOUND</span>
      </div>

      <!-- Icon Illustration -->
      <div class="illustration-wrap">
        <div class="icon-circle">
          <svg viewBox="0 0 24 24" width="56" height="56" fill="none" stroke="currentColor" stroke-width="1.8">
            <circle cx="12" cy="12" r="10" stroke="#cbd5e1" stroke-width="2"/>
            <path d="M16 16s-1.5-2-4-2-4 2-4 2" stroke-linecap="round"/>
            <line x1="9" y1="9" x2="9.01" y2="9" stroke-width="3" stroke-linecap="round"/>
            <line x1="15" y1="9" x2="15.01" y2="9" stroke-width="3" stroke-linecap="round"/>
          </svg>
        </div>
      </div>

      <!-- Heading & Description -->
      <h1 class="not-found-title">Không tìm thấy phân hệ này</h1>
      <p class="not-found-desc">
        Đường dẫn bạn vừa truy cập không tồn tại hoặc tài nguyên đã được di chuyển sang danh mục khác trên hệ thống ERP Cloud.
      </p>

      <!-- Action Buttons -->
      <div class="actions-group">
        <button type="button" class="btn-action btn-outline" @click="$router.back()">
          <span class="btn-arrow">←</span>
          <span>Quay lại</span>
        </button>

        <button type="button" class="btn-action btn-primary" @click="$router.push('/')">
          <span>Về Tổng quan Dashboard</span>
          <span class="btn-arrow">➔</span>
        </button>
      </div>

      <!-- Quick Helpful Links -->
      <div class="quick-links-box">
        <span class="quick-title">Phân hệ phổ biến:</span>
        <div class="links-row">
          <a href="javascript:void(0)" @click="$router.push('/products')">📦 Quản lý Sản phẩm</a>
          <span class="dot">•</span>
          <a href="javascript:void(0)" @click="$router.push('/users')">👥 Quản lý Tài khoản</a>
          <span class="dot">•</span>
          <a href="javascript:void(0)" @click="$router.push('/announcements')">📢 Quản lý Thông báo</a>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
</script>

<style scoped>
.not-found-page {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: calc(100vh - 120px);
  padding: 32px 16px;
}

.not-found-card {
  max-width: 560px;
  width: 100%;
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 20px;
  padding: 40px 36px;
  text-align: center;
  box-shadow: 0 10px 25px -5px rgba(15, 23, 42, 0.05), 0 0 1px 1px rgba(226, 232, 240, 0.8);
}

.badge-row {
  margin-bottom: 20px;
}

.status-code-badge {
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 1px;
  color: #2563eb;
  background: #eff6ff;
  border: 1px solid #bfdbfe;
  padding: 4px 12px;
  border-radius: 20px;
  display: inline-block;
}

.illustration-wrap {
  display: flex;
  justify-content: center;
  margin-bottom: 20px;
}

.icon-circle {
  width: 96px;
  height: 96px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #64748b;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.03);
}

.not-found-title {
  font-size: 24px;
  font-weight: 800;
  color: #0f172a;
  margin: 0 0 8px 0;
  letter-spacing: -0.3px;
}

.not-found-desc {
  font-size: 14px;
  line-height: 1.6;
  color: #64748b;
  margin: 0 0 28px 0;
}

.actions-group {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  margin-bottom: 28px;
}

.btn-action {
  padding: 11px 20px;
  border-radius: 12px;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
}

.btn-outline {
  background: #ffffff;
  border: 1px solid #cbd5e1;
  color: #334155;
}

.btn-outline:hover {
  background: #f8fafc;
  border-color: #94a3b8;
  transform: translateY(-1px);
}

.btn-primary {
  background: #2563eb;
  border: 1px solid #2563eb;
  color: #ffffff;
  box-shadow: 0 4px 14px rgba(37, 99, 235, 0.25);
}

.btn-primary:hover {
  background: #1d4ed8;
  transform: translateY(-1px);
  box-shadow: 0 6px 18px rgba(37, 99, 235, 0.35);
}

.quick-links-box {
  border-top: 1px solid #f1f5f9;
  padding-top: 20px;
  font-size: 12px;
  color: #64748b;
}

.quick-title {
  display: block;
  margin-bottom: 8px;
  font-weight: 600;
}

.links-row {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  flex-wrap: wrap;
}

.links-row a {
  color: #2563eb;
  text-decoration: none;
  font-weight: 600;
  transition: color 0.2s;
}

.links-row a:hover {
  text-decoration: underline;
  color: #1d4ed8;
}

.dot {
  color: #cbd5e1;
}

@media (max-width: 640px) {
  .actions-group {
    flex-direction: column;
    width: 100%;
  }
  .btn-action {
    width: 100%;
    justify-content: center;
  }
}
</style>

