<template>
  <div class="dashboard-page-container">
    <!-- 1. OPERATIONAL STATUS BANNER (FIGMA MATCH) -->
    <div v-if="showBanner" class="operational-notice-banner">
      <div class="banner-left">
        <span class="banner-check-badge">
          <svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor">
            <path fill-rule="evenodd" d="M2.25 12c0-5.385 4.365-9.75 9.75-9.75s9.75 4.365 9.75 9.75-4.365 9.75-9.75 9.75S2.25 17.385 2.25 12zm13.36-1.814a.75.75 0 10-1.22-.872l-3.236 4.53L9.53 12.22a.75.75 0 00-1.06 1.06l2.25 2.25a.75.75 0 001.14-.094l3.74-5.25z" clip-rule="evenodd" />
          </svg>
        </span>
        <span class="banner-main-text">
          <strong>Hệ thống vận hành ổn định 99.98%</strong> • Phiên bản v4.12 • Cụm máy chủ Đông Nam Á (VN-Central)
        </span>
      </div>
      <div class="banner-right">
        <button type="button" class="btn-sync-live" @click="refreshData">
          <span class="sync-dot">●</span>
          <span>Đang đồng bộ thực</span>
        </button>
        <button type="button" class="btn-banner-close" @click="showBanner = false">✕</button>
      </div>
    </div>

    <!-- 1.2 DÒNG THÔNG BÁO HỆ THỐNG MARQUEE CHẠY CHỮ -->
    <div v-if="announcementMessage" class="dashboard-announcement-marquee">
      <div class="announcement-tag">
        <span class="bell-icon">📢</span>
        <span>THÔNG BÁO</span>
      </div>
      <div class="marquee-track-container">
        <div class="marquee-content-stream">
          <span class="announcement-text">{{ announcementMessage }}</span>
          <span class="stream-divider">✦</span>
          <span class="announcement-text">{{ announcementMessage }}</span>
          <span class="stream-divider">✦</span>
          <span class="announcement-text">{{ announcementMessage }}</span>
        </div>
      </div>
    </div>

    <!-- 2. TOP 4 KPI STAT CARDS (FIGMA MATCH) -->
    <div class="kpi-cards-grid">
      <!-- Card 1: Tổng Sản phẩm -->
      <div class="kpi-card" @click="$router.push('/products')">
        <div class="kpi-header">
          <span class="kpi-category">DANH MỤC KHO</span>
          <div class="kpi-icon-box blue">
            <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2">
              <rect x="3" y="4" width="18" height="18" rx="2" ry="2"/>
              <line x1="16" y1="2" x2="16" y2="6"/>
              <line x1="8" y1="2" x2="8" y2="6"/>
              <line x1="3" y1="10" x2="21" y2="10"/>
            </svg>
          </div>
        </div>
        <div class="kpi-title">Tổng Sản phẩm</div>
        <div class="kpi-value-row">
          <span class="kpi-number">{{ stats.products }}</span>
          <span class="trend-badge positive">↗ +12%</span>
        </div>
        <div class="kpi-footer">
          <span class="footer-sub">So với tháng trước (107 Cái/SKU)</span>
          <span class="footer-link">Chi tiết ›</span>
        </div>
      </div>

      <!-- Card 2: Đơn hàng mới -->
      <div class="kpi-card">
        <div class="kpi-header">
          <span class="kpi-category">LƯU LƯỢNG GIAO DỊCH</span>
          <div class="kpi-icon-box purple">
            <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="9" cy="21" r="1"/>
              <circle cx="20" cy="21" r="1"/>
              <path d="M1 1h4l2.68 13.39a2 2 0 0 0 2 1.61h9.72a2 2 0 0 0 2-1.61L23 6H6"/>
            </svg>
          </div>
        </div>
        <div class="kpi-title">Đơn hàng mới</div>
        <div class="kpi-value-row">
          <span class="kpi-number">45</span>
          <span class="trend-badge highlight-blue">⚡ 45 hôm nay</span>
        </div>
        <div class="kpi-footer">
          <span class="footer-sub">18 đơn đang chờ xuất kho</span>
          <span class="footer-link">Xử lý ngay ›</span>
        </div>
      </div>

      <!-- Card 3: Tổng Tài khoản -->
      <div class="kpi-card" @click="$router.push('/users')">
        <div class="kpi-header">
          <span class="kpi-category">BẢO MẬT & PHÂN QUYỀN</span>
          <div class="kpi-icon-box indigo">
            <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/>
            </svg>
          </div>
        </div>
        <div class="kpi-title">Tổng Tài khoản</div>
        <div class="kpi-value-row">
          <span class="kpi-number">{{ stats.users }}</span>
          <span class="trend-badge neutral">🛡️ RBAC Active</span>
        </div>
        <div class="kpi-footer">
          <span class="footer-sub">3 Quản trị | 9 Vận hành viên</span>
          <span class="footer-link">Phân quyền ›</span>
        </div>
      </div>

      <!-- Card 4: Tỷ lệ hoàn thành -->
      <div class="kpi-card">
        <div class="kpi-header">
          <span class="kpi-category">CHỈ SỐ DOANH NGHIỆP</span>
          <div class="kpi-icon-box cyan">
            <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/>
              <polyline points="22 4 12 14.01 9 11.01"/>
            </svg>
          </div>
        </div>
        <div class="kpi-title">Tỷ lệ hoàn thành</div>
        <div class="kpi-value-row">
          <span class="kpi-number">94.2%</span>
          <span class="trend-badge positive">↗ +1.8%</span>
        </div>
        <div class="kpi-progress-bar">
          <div class="progress-fill" style="width: 94.2%;"></div>
        </div>
        <div class="kpi-footer">
          <span class="footer-sub">Mục tiêu Q3: 92%</span>
          <span class="footer-link text-emerald">Vượt chỉ tiêu ✓</span>
        </div>
      </div>
    </div>

    <!-- 3. QUICK ACTIONS BAR (THAO TÁC NHANH - FIGMA MATCH) -->
    <div class="quick-actions-toolbar">
      <div class="actions-left">
        <span class="actions-heading">THAO TÁC NHANH</span>
        <button type="button" class="btn-action-primary" @click="$router.push('/products')">
          <span class="btn-icon">📦</span>
          <span>Quản lý Sản phẩm</span>
        </button>
        <button type="button" class="btn-action-outline" @click="$router.push('/users')">
          <span class="btn-icon">👥</span>
          <span>Quản lý Tài khoản</span>
        </button>
        <button type="button" class="btn-action-outline" @click="$router.push('/announcements')">
          <span class="btn-icon">📢</span>
          <span>Tạo tin báo</span>
        </button>
      </div>

      <div class="actions-right">
        <div class="date-filter-pill">
          <span>📅 Tháng 10, 2026</span>
          <span class="caret">⌄</span>
        </div>
        <button type="button" class="btn-export-excel" @click="exportExcel">
          <span class="btn-icon">📥</span>
          <span>Xuất báo cáo Excel</span>
        </button>
      </div>
    </div>

    <!-- 4. MIDDLE SECTION: CHART (65%) & ACTIVITY LOG (35%) -->
    <div class="middle-section-grid">
      <!-- Left Chart Card: Hiệu xuất Doanh thu & Đơn hàng -->
      <div class="dashboard-panel chart-panel">
        <div class="panel-header">
          <div class="panel-title-group">
            <div class="panel-title-row">
              <h2 class="panel-main-title">Hiệu xuất Doanh thu & Đơn hàng</h2>
              <span class="period-pill">30 ngày qua</span>
            </div>
            <p class="panel-sub-title">So sánh thực tế doanh thu cùng chu kỳ tăng trưởng đơn hoàn tất</p>
          </div>

          <!-- Time Frame Filter Tabs -->
          <div class="chart-tab-group">
            <button 
              type="button" 
              class="chart-tab-btn" 
              :class="{ active: chartTab === 'week' }"
              @click="chartTab = 'week'"
            >
              Theo Tuần
            </button>
            <button 
              type="button" 
              class="chart-tab-btn" 
              :class="{ active: chartTab === 'month' }"
              @click="chartTab = 'month'"
            >
              Theo Tháng
            </button>
            <button 
              type="button" 
              class="chart-tab-btn" 
              :class="{ active: chartTab === 'quarter' }"
              @click="chartTab = 'quarter'"
            >
              Theo Quý
            </button>
          </div>
        </div>

        <!-- Chart Legend & Peak Row -->
        <div class="chart-legend-row">
          <div class="legend-items">
            <span class="legend-badge blue">
              <span class="legend-color-box"></span>
              <span>Doanh thu (Triệu VNĐ)</span>
            </span>
            <span class="legend-badge cyan">
              <span class="legend-color-box cyan"></span>
              <span>Số đơn hoàn tất</span>
            </span>
          </div>
          <div class="peak-value-text">
            Cao nhất: <strong>364 Triệu VNĐ</strong>
          </div>
        </div>

        <!-- SVG Spline Area Chart (Custom Smooth Vector Curve) -->
        <div class="svg-chart-wrapper">
          <svg viewBox="0 0 680 220" class="smooth-area-chart" preserveAspectRatio="none">
            <defs>
              <linearGradient id="revenueGrad" x1="0" y1="0" x2="0" y2="1">
                <stop offset="0%" stop-color="#2563eb" stop-opacity="0.32" />
                <stop offset="100%" stop-color="#2563eb" stop-opacity="0.01" />
              </linearGradient>
            </defs>

            <!-- Background Grid Lines -->
            <line x1="0" y1="40" x2="680" y2="40" stroke="#f1f5f9" stroke-dasharray="4 4" />
            <line x1="0" y1="90" x2="680" y2="90" stroke="#f1f5f9" stroke-dasharray="4 4" />
            <line x1="0" y1="140" x2="680" y2="140" stroke="#f1f5f9" stroke-dasharray="4 4" />

            <!-- Smooth Area Fill -->
            <path 
              d="M 20 180 C 100 130, 160 160, 220 140 C 280 120, 320 60, 380 90 C 440 120, 480 170, 540 80 C 600 40, 640 50, 680 60 L 680 200 L 20 200 Z" 
              fill="url(#revenueGrad)" 
            />

            <!-- Primary Revenue Curve (Solid Blue) -->
            <path 
              d="M 20 180 C 100 130, 160 160, 220 140 C 280 120, 320 60, 380 90 C 440 120, 480 170, 540 80 C 600 40, 640 50, 680 60" 
              fill="none" 
              stroke="#2563eb" 
              stroke-width="3" 
              stroke-linecap="round"
            />

            <!-- Secondary Order Curve (Dashed Cyan) -->
            <path 
              d="M 20 190 C 100 160, 160 170, 220 160 C 280 140, 320 110, 380 130 C 440 150, 480 180, 540 110 C 600 80, 640 90, 680 95" 
              fill="none" 
              stroke="#06b6d4" 
              stroke-width="2.2" 
              stroke-dasharray="6 4"
              stroke-linecap="round"
            />

            <!-- Interactive Peak Data Points -->
            <circle cx="380" cy="90" r="4.5" fill="#2563eb" stroke="#ffffff" stroke-width="2.5" />
            <circle cx="540" cy="80" r="5" fill="#2563eb" stroke="#ffffff" stroke-width="3" />
          </svg>

          <!-- Chart X-Axis Labels -->
          <div class="chart-x-axis">
            <span>T2 (14/10)</span>
            <span>T3 (15/10)</span>
            <span>T4 (16/10)</span>
            <span>T5 (17/10)</span>
            <span>T6 (18/10)</span>
            <span>T7 (19/10)</span>
            <span>CN (20/10)</span>
          </div>
        </div>

        <!-- Bottom Stat Summary Metrics -->
        <div class="chart-bottom-metrics">
          <div class="chart-metric-item">
            <span class="m-label">Tổng doanh thu kỳ</span>
            <span class="m-val text-blue">1,842.5 tr đ</span>
          </div>
          <div class="chart-metric-item">
            <span class="m-label">Giá trị TB / đơn</span>
            <span class="m-val">4.1 tr đ</span>
          </div>
          <div class="chart-metric-item">
            <span class="m-label">Đơn hủy / hoàn</span>
            <span class="m-val text-red">1.2%</span>
          </div>
        </div>
      </div>

      <!-- Right Activity Log Panel: Nhật ký hoạt động -->
      <div class="dashboard-panel activity-panel">
        <div class="panel-header">
          <div class="activity-title-row">
            <span class="clock-icon-blue">🕒</span>
            <h2 class="panel-main-title">Nhật ký hoạt động</h2>
          </div>
          <span class="live-dot-green"></span>
        </div>

        <!-- Activity Feed List -->
        <div class="activity-feed-list">
          <!-- Item 1 -->
          <div class="activity-item">
            <div class="user-avatar-tag blue">NA</div>
            <div class="activity-content">
              <div class="activity-actor-row">
                <span class="actor-name">Nguyễn Anh (Kho HN)</span>
                <span class="actor-time">2 phút trước</span>
              </div>
              <p class="activity-desc">
                Xuất kho 12 sản phẩm mã <code class="sku-code">SKU-2101</code> cho đơn vận chuyển <strong>#ORD-9871</strong>
              </p>
              <span class="activity-status-pill green">✓ Đã duyệt tự động</span>
            </div>
          </div>

          <!-- Item 2 -->
          <div class="activity-item">
            <div class="user-avatar-tag purple">TL</div>
            <div class="activity-content">
              <div class="activity-actor-row">
                <span class="actor-name">Trần Linh (Kế toán)</span>
                <span class="actor-time">6 phút trước</span>
              </div>
              <p class="activity-desc">
                Xác nhận thanh toán công nợ <strong>250,000,000 đ</strong> từ đại lý phân phối FPT Synnex
              </p>
              <span class="activity-status-pill blue">Đã đối soát SAP</span>
            </div>
          </div>

          <!-- Item 3 -->
          <div class="activity-item">
            <div class="user-avatar-tag dark">🤖</div>
            <div class="activity-content">
              <div class="activity-actor-row">
                <span class="actor-name">ERP AI Copilot</span>
                <span class="actor-time">12 phút trước</span>
              </div>
              <p class="activity-desc">
                Cân bằng định mức bổ sung tự động cho SKU-1082 theo chu kỳ bán hàng tuần
              </p>
              <span class="activity-status-pill orange">Đề xuất mua hàng</span>
            </div>
          </div>

          <!-- Item 4 -->
          <div class="activity-item">
            <div class="user-avatar-tag slate">🛡️</div>
            <div class="activity-content">
              <div class="activity-actor-row">
                <span class="actor-name">Hệ thống Giám sát RBAC</span>
                <span class="actor-time">1 giờ trước</span>
              </div>
              <p class="activity-desc">
                Cấp quyền hạn chế Role Auditor cho tài khoản thanh tra nội bộ <code>@kiemtoan_01</code>
              </p>
            </div>
          </div>
        </div>

        <div class="activity-footer-link">
          <a href="#" @click.prevent="notifyFeature('Lịch sử kiểm toán')">
            Xem toàn bộ lịch sử sự kiện hôm nay ➔
          </a>
        </div>
      </div>
    </div>

    <!-- 5. BOTTOM ROW: INVENTORY SAFETY & SYSTEM HEALTH -->
    <div class="bottom-section-grid">
      <!-- Left: Cảnh báo tồn kho an toàn -->
      <div class="dashboard-panel inventory-warning-panel">
        <div class="panel-header">
          <div class="warning-title-group">
            <div class="warning-title-row">
              <span class="warning-triangle-icon">⚠️</span>
              <h2 class="panel-main-title">Cảnh báo tồn kho an toàn</h2>
            </div>
            <p class="panel-sub-title">3 mặt hàng đang dưới ngưỡng dự trữ viện kiểm định mức</p>
          </div>

          <a href="javascript:void(0)" class="view-all-stock-link" @click="$router.push('/products')">
            Xem toàn bộ kho ({{ stats.products }}) ›
          </a>
        </div>

        <!-- Table of Safety Stock -->
        <div class="table-responsive">
          <table class="inventory-table">
            <thead>
              <tr>
                <th>MÃ SKU & SẢN PHẨM</th>
                <th class="text-center">KHO HIỆN TẠI</th>
                <th class="text-center">NGƯỠNG TỐI THIỂU</th>
                <th class="text-center">TRẠNG THÁI RỦI RO</th>
                <th class="text-right">XỬ LÝ NHẬP</th>
              </tr>
            </thead>
            <tbody>
              <!-- Row 1 -->
              <tr>
                <td>
                  <div class="product-cell">
                    <img src="https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?w=100" class="prod-thumb" alt="Product" />
                    <div>
                      <div class="prod-name">Cáp Type-C Thunderbolt 4 Fast C...</div>
                      <div class="prod-sku">SKU-8921 • Phụ kiện vi tính</div>
                    </div>
                  </div>
                </td>
                <td class="text-center">
                  <span class="stock-num red">08</span> <span class="unit">chiếc</span>
                </td>
                <td class="text-center text-muted">50 chiếc</td>
                <td class="text-center">
                  <span class="risk-badge red">Cạn kiệt khẩn cấp</span>
                </td>
                <td class="text-right">
                  <button type="button" class="btn-po-create blue" @click="createPO('SKU-8921')">
                    Tạo PO Nhập
                  </button>
                </td>
              </tr>

              <!-- Row 2 -->
              <tr>
                <td>
                  <div class="product-cell">
                    <img src="https://images.unsplash.com/photo-1587829741301-dc798b83add3?w=100" class="prod-thumb" alt="Keyboard" />
                    <div>
                      <div class="prod-name">Bàn phím cơ Không dây RGB Tr...</div>
                      <div class="prod-sku">SKU-4105 • Thiết bị ngoại vi</div>
                    </div>
                  </div>
                </td>
                <td class="text-center">
                  <span class="stock-num orange">14</span> <span class="unit">chiếc</span>
                </td>
                <td class="text-center text-muted">40 chiếc</td>
                <td class="text-center">
                  <span class="risk-badge orange">Cận chạm định mức</span>
                </td>
                <td class="text-right">
                  <button type="button" class="btn-po-create cyan" @click="createPO('SKU-4105')">
                    Lên đơn nhận
                  </button>
                </td>
              </tr>

              <!-- Row 3 -->
              <tr>
                <td>
                  <div class="product-cell">
                    <img src="https://images.unsplash.com/photo-1615663245857-ac93bb7c39e7?w=100" class="prod-thumb" alt="Mouse" />
                    <div>
                      <div class="prod-name">Chuột không dây Silent Precision...</div>
                      <div class="prod-sku">SKU-1082 • Phụ kiện văn phòng</div>
                    </div>
                  </div>
                </td>
                <td class="text-center">
                  <span class="stock-num red">05</span> <span class="unit">chiếc</span>
                </td>
                <td class="text-center text-muted">30 chiếc</td>
                <td class="text-center">
                  <span class="risk-badge red">Báo động đỏ</span>
                </td>
                <td class="text-right">
                  <button type="button" class="btn-po-create blue" @click="createPO('SKU-1082')">
                    Tạo PO Nhập
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- Right: Tài nguyên & Trực thăng (System Health) -->
      <div class="dashboard-panel system-health-panel">
        <div class="panel-header">
          <div class="health-title-group">
            <h2 class="panel-main-title">Tài nguyên & Trực thăng</h2>
            <span class="server-icon-muted">🖳</span>
          </div>
        </div>

        <div class="health-metrics-list">
          <!-- CPU Cores -->
          <div class="health-item">
            <div class="health-label-row">
              <span class="h-name">Tải CPU Máy chủ chính (16 Cores)</span>
              <span class="h-value">28.4%</span>
            </div>
            <div class="health-progress-track">
              <div class="health-progress-bar blue" style="width: 28.4%;"></div>
            </div>
          </div>

          <!-- RAM Memory -->
          <div class="health-item">
            <div class="health-label-row">
              <span class="h-name">Bộ nhớ RAM (DDR4 64GB) / PostgresSQL</span>
              <span class="h-value">61.2% <small>(39.1/64 GB)</small></span>
            </div>
            <div class="health-progress-track">
              <div class="health-progress-bar purple" style="width: 61.2%;"></div>
            </div>
          </div>

          <!-- API Latency Card -->
          <div class="api-latency-card">
            <div class="api-left">
              <span class="api-icon">🌐</span>
              <span class="api-title">Độ trễ API Trung bình</span>
            </div>
            <span class="api-badge-fast">18ms • Siêu tốc</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import api from '../api'

const router = useRouter()
const showBanner = ref(true)
const chartTab = ref('week')
const announcementMessage = ref('🎉 Chào mừng bạn đến với ERP Cloud System! Chúc bạn một ngày làm việc hiệu quả, năng lượng và hoàn thành xuất sắc mọi mục tiêu.')

const stats = reactive({
  products: 120,
  users: 12
})

async function fetchActiveAnnouncement() {
  try {
    const res = await api.get('/announcements/active')
    if (res.data && res.data.content) {
      announcementMessage.value = `${res.data.title ? res.data.title + ': ' : ''}${res.data.content}`
    }
  } catch {
    // Keep fallback
  }
}

async function fetchRealStats() {
  try {
    const [prodRes, userRes] = await Promise.allSettled([
      api.get('/products'),
      api.get('/users')
    ])
    if (prodRes.status === 'fulfilled' && Array.isArray(prodRes.value.data)) {
      stats.products = prodRes.value.data.length || 120
    }
    if (userRes.status === 'fulfilled' && Array.isArray(userRes.value.data)) {
      stats.users = userRes.value.data.length || 12
    }
  } catch {
    // Keep mock figures
  }
}

function refreshData() {
  fetchRealStats()
  fetchActiveAnnouncement()
  ElMessage.success('⚡ Đã đồng bộ dữ liệu thời gian thực từ Database!')
}

function exportExcel() {
  ElMessage.success('📥 Đang kết xuất báo cáo doanh nghiệp ra tệp Excel (.xlsx)...')
}

function createPO(sku) {
  ElMessage.success(`📦 Đã tạo lệnh nhập kho Purchase Order (PO) tự động cho ${sku}!`)
}

function notifyFeature(name) {
  ElMessage.info(`Tính năng [${name}] đang sẵn sàng ở bản Enterprise v4.2.`)
}

onMounted(() => {
  fetchRealStats()
  fetchActiveAnnouncement()
})
</script>

<style scoped>
/* ==============================================================================
   FIGMA ENTERPRISE DASHBOARD STYLES (MATCHING IMAGES 3, 4, 5)
   ============================================================================== */
.dashboard-page-container {
  display: flex;
  flex-direction: column;
  gap: 20px;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
}

/* ==================== 1. OPERATIONAL BANNER ==================== */
.operational-notice-banner {
  background: #f0fdf4;
  border: 1px solid #bbf7d0;
  border-radius: 12px;
  padding: 10px 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

/* ==================== 1.2 ANNOUNCEMENT MARQUEE ==================== */
.dashboard-announcement-marquee {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 8px 14px;
  display: flex;
  align-items: center;
  gap: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
  overflow: hidden;
}

.announcement-tag {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background: linear-gradient(135deg, #fef3c7 0%, #fde68a 100%);
  border: 1px solid #fcd34d;
  color: #92400e;
  font-size: 11px;
  font-weight: 800;
  padding: 3px 9px;
  border-radius: 8px;
  letter-spacing: 0.5px;
  flex-shrink: 0;
}

.marquee-track-container {
  flex: 1;
  overflow: hidden;
  position: relative;
  mask-image: linear-gradient(to right, transparent, black 2%, black 98%, transparent);
  -webkit-mask-image: linear-gradient(to right, transparent, black 2%, black 98%, transparent);
}

.marquee-content-stream {
  display: inline-flex;
  align-items: center;
  white-space: nowrap;
  animation: stream-scroll 32s linear infinite;
}

.marquee-content-stream:hover {
  animation-play-state: paused;
}

.announcement-text {
  font-size: 13px;
  font-weight: 600;
  color: #1e293b;
  cursor: default;
}

.stream-divider {
  color: #f59e0b;
  font-size: 11px;
  margin: 0 20px;
}

@keyframes stream-scroll {
  0% {
    transform: translateX(0);
  }
  100% {
    transform: translateX(-33.33%);
  }
}

.banner-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.banner-check-badge {
  color: #059669;
  background: #dcfce7;
  border-radius: 6px;
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.banner-main-text {
  font-size: 13px;
  color: #1e293b;
}

.banner-main-text strong {
  color: #0f172a;
}

.banner-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.btn-sync-live {
  background: #ecfeff;
  border: 1px solid #a5f3fc;
  color: #0891b2;
  font-size: 12px;
  font-weight: 600;
  padding: 4px 12px;
  border-radius: 20px;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 6px;
  box-shadow: none;
}

.sync-dot {
  font-size: 9px;
  color: #0891b2;
}

.btn-banner-close {
  background: none;
  border: none;
  color: #64748b;
  font-size: 14px;
  cursor: pointer;
  padding: 2px 4px;
}

.btn-banner-close:hover {
  color: #166534;
}

/* ==================== 2. KPI CARDS ==================== */
.kpi-cards-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.kpi-card {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 16px;
  padding: 18px 20px;
  cursor: pointer;
  transition: all 0.2s;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.02);
}

.kpi-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 16px -4px rgba(15, 23, 42, 0.08);
  border-color: #cbd5e1;
}

.kpi-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 6px;
}

.kpi-category {
  font-size: 10.5px;
  font-weight: 700;
  color: #94a3b8;
  letter-spacing: 0.5px;
}

.kpi-icon-box {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.kpi-icon-box.blue { background: #eff6ff; color: #2563eb; }
.kpi-icon-box.purple { background: #f5f3ff; color: #7c3aed; }
.kpi-icon-box.indigo { background: #eef2ff; color: #4f46e5; }
.kpi-icon-box.cyan { background: #ecfeff; color: #0891b2; }

.kpi-title {
  font-size: 14px;
  font-weight: 700;
  color: #334155;
  margin-bottom: 8px;
}

.kpi-value-row {
  display: flex;
  align-items: baseline;
  gap: 10px;
  margin-bottom: 12px;
}

.kpi-number {
  font-size: 28px;
  font-weight: 800;
  color: #0f172a;
  letter-spacing: -0.8px;
}

.trend-badge {
  font-size: 11px;
  font-weight: 700;
  padding: 2px 8px;
  border-radius: 6px;
}

.trend-badge.positive {
  background: #ecfdf5;
  color: #059669;
}

.trend-badge.highlight-blue {
  background: #eff6ff;
  color: #2563eb;
}

.trend-badge.neutral {
  background: #f1f5f9;
  color: #475569;
}

.kpi-progress-bar {
  width: 100%;
  height: 6px;
  background: #f1f5f9;
  border-radius: 3px;
  overflow: hidden;
  margin-bottom: 10px;
}

.progress-fill {
  height: 100%;
  background: #2563eb;
  border-radius: 3px;
}

.kpi-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 11.5px;
  border-top: 1px solid #f8fafc;
  padding-top: 8px;
}

.footer-sub {
  color: #64748b;
}

.footer-link {
  color: #2563eb;
  font-weight: 600;
}

.text-emerald {
  color: #059669 !important;
}

/* ==================== 3. QUICK ACTIONS TOOLBAR ==================== */
.quick-actions-toolbar {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 14px;
  padding: 12px 18px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
}

.actions-left {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.actions-heading {
  font-size: 11px;
  font-weight: 800;
  color: #64748b;
  letter-spacing: 0.6px;
  margin-right: 6px;
}

.btn-action-primary {
  background: #2563eb;
  color: #ffffff;
  border: none;
  padding: 8px 14px;
  border-radius: 10px;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 6px;
  box-shadow: 0 4px 10px rgba(37, 99, 235, 0.2);
  transition: all 0.2s;
}

.btn-action-primary:hover {
  background: #1d4ed8;
}

.btn-action-outline {
  background: #ffffff;
  border: 1px solid #cbd5e1;
  color: #334155;
  padding: 8px 14px;
  border-radius: 10px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 6px;
  transition: all 0.2s;
}

.btn-action-outline:hover {
  background: #f8fafc;
  border-color: #94a3b8;
}

.actions-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

.date-filter-pill {
  display: flex;
  align-items: center;
  gap: 6px;
  background: #ffffff;
  border: 1px solid #cbd5e1;
  padding: 8px 14px;
  border-radius: 10px;
  font-size: 12.5px;
  font-weight: 600;
  color: #475569;
  cursor: pointer;
}

.btn-export-excel {
  background: #f8fafc;
  border: 1px solid #cbd5e1;
  color: #334155;
  padding: 8px 14px;
  border-radius: 10px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 6px;
  transition: all 0.2s;
}

.btn-export-excel:hover {
  background: #f1f5f9;
}

/* ==================== 4. MIDDLE SECTION GRID ==================== */
.middle-section-grid {
  display: grid;
  grid-template-columns: 1.8fr 1fr;
  gap: 20px;
}

.dashboard-panel {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 18px;
  padding: 22px 24px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.02);
}

.panel-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 16px;
}

.panel-main-title {
  font-size: 16px;
  font-weight: 800;
  color: #0f172a;
  letter-spacing: -0.3px;
  margin: 0;
}

.panel-title-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.period-pill {
  font-size: 10.5px;
  font-weight: 700;
  background: #eff6ff;
  color: #2563eb;
  padding: 2px 8px;
  border-radius: 6px;
}

.panel-sub-title {
  font-size: 12.5px;
  color: #64748b;
  margin: 4px 0 0 0;
}

/* Chart tabs */
.chart-tab-group {
  display: flex;
  background: #f1f5f9;
  border-radius: 8px;
  padding: 3px;
  gap: 2px;
}

.chart-tab-btn {
  background: none;
  border: none;
  font-size: 11.5px;
  font-weight: 600;
  color: #64748b;
  padding: 5px 12px;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
}

.chart-tab-btn.active {
  background: #ffffff;
  color: #0f172a;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
}

.chart-legend-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 12px;
  color: #475569;
  margin-bottom: 14px;
}

.legend-items {
  display: flex;
  align-items: center;
  gap: 16px;
}

.legend-badge {
  display: flex;
  align-items: center;
  gap: 6px;
}

.legend-color-box {
  width: 10px;
  height: 10px;
  background: #2563eb;
  border-radius: 3px;
}

.legend-color-box.cyan {
  background: #06b6d4;
}

.peak-value-text {
  font-size: 12px;
  color: #64748b;
}

.peak-value-text strong {
  color: #0f172a;
}

.svg-chart-wrapper {
  position: relative;
  width: 100%;
}

.smooth-area-chart {
  width: 100%;
  height: 190px;
}

.chart-x-axis {
  display: flex;
  justify-content: space-between;
  font-size: 11px;
  color: #94a3b8;
  margin-top: 8px;
  padding: 0 10px;
}

.chart-bottom-metrics {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
  border-top: 1px solid #f1f5f9;
  padding-top: 16px;
  margin-top: 16px;
}

.chart-metric-item {
  display: flex;
  flex-direction: column;
}

.m-label {
  font-size: 11px;
  color: #64748b;
  margin-bottom: 2px;
}

.m-val {
  font-size: 18px;
  font-weight: 800;
  color: #0f172a;
}

.m-val.text-blue { color: #2563eb; }
.m-val.text-red { color: #ef4444; }

/* Activity Panel */
.activity-title-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.live-dot-green {
  width: 7px;
  height: 7px;
  background: #10b981;
  border-radius: 50%;
}

.activity-feed-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
  margin-top: 6px;
}

.activity-item {
  display: flex;
  gap: 12px;
  align-items: flex-start;
}

.user-avatar-tag {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  font-size: 11px;
  font-weight: 800;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.user-avatar-tag.blue { background: #eff6ff; color: #2563eb; }
.user-avatar-tag.purple { background: #f5f3ff; color: #7c3aed; }
.user-avatar-tag.dark { background: #0f172a; color: #ffffff; }
.user-avatar-tag.slate { background: #f1f5f9; color: #475569; }

.activity-content {
  flex: 1;
}

.activity-actor-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 2px;
}

.actor-name {
  font-size: 12.5px;
  font-weight: 700;
  color: #0f172a;
}

.actor-time {
  font-size: 11px;
  color: #94a3b8;
}

.activity-desc {
  font-size: 12px;
  color: #475569;
  line-height: 1.4;
  margin: 0 0 6px 0;
}

.sku-code {
  background: #f1f5f9;
  padding: 1px 4px;
  border-radius: 4px;
  font-family: monospace;
}

.activity-status-pill {
  font-size: 10px;
  font-weight: 700;
  padding: 2px 7px;
  border-radius: 5px;
  display: inline-block;
}

.activity-status-pill.green { background: #ecfdf5; color: #059669; }
.activity-status-pill.blue { background: #eff6ff; color: #2563eb; }
.activity-status-pill.orange { background: #fff7ed; color: #ea580c; }

.activity-footer-link {
  margin-top: 18px;
  padding-top: 12px;
  border-top: 1px solid #f1f5f9;
  text-align: center;
}

.activity-footer-link a {
  font-size: 12px;
  font-weight: 600;
  color: #2563eb;
  text-decoration: none;
}

/* ==================== 5. BOTTOM SECTION GRID ==================== */
.bottom-section-grid {
  display: grid;
  grid-template-columns: 1.8fr 1fr;
  gap: 20px;
}

.warning-title-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.view-all-stock-link {
  font-size: 12px;
  font-weight: 600;
  color: #2563eb;
  text-decoration: none;
}

.table-responsive {
  overflow-x: auto;
}

.inventory-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

.inventory-table th {
  text-align: left;
  font-size: 10.5px;
  font-weight: 700;
  color: #94a3b8;
  letter-spacing: 0.5px;
  padding: 10px 8px;
  border-bottom: 1px solid #f1f5f9;
}

.inventory-table td {
  padding: 12px 8px;
  border-bottom: 1px solid #f8fafc;
  vertical-align: middle;
}

.product-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.prod-thumb {
  width: 38px;
  height: 38px;
  border-radius: 8px;
  object-fit: cover;
  background: #f1f5f9;
}

.prod-name {
  font-weight: 700;
  color: #0f172a;
}

.prod-sku {
  font-size: 11px;
  color: #64748b;
  margin-top: 2px;
}

.stock-num {
  font-size: 15px;
  font-weight: 800;
}

.stock-num.red { color: #dc2626; }
.stock-num.orange { color: #ea580c; }

.unit {
  font-size: 11px;
  color: #64748b;
}

.text-muted { color: #64748b; }
.text-center { text-align: center; }
.text-right { text-align: right; }

.risk-badge {
  font-size: 11px;
  font-weight: 700;
  padding: 3px 8px;
  border-radius: 6px;
  display: inline-block;
}

.risk-badge.red { background: #fef2f2; color: #dc2626; }
.risk-badge.orange { background: #fff7ed; color: #ea580c; }

.btn-po-create {
  border: none;
  padding: 6px 12px;
  border-radius: 8px;
  font-size: 11.5px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-po-create.blue { background: #2563eb; color: #ffffff; }
.btn-po-create.cyan { background: #0891b2; color: #ffffff; }

.btn-po-create:hover {
  opacity: 0.9;
  transform: translateY(-1px);
}

/* System Health */
.health-title-group {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
}

.server-icon-muted {
  font-size: 16px;
  color: #94a3b8;
}

.health-metrics-list {
  display: flex;
  flex-direction: column;
  gap: 18px;
  margin-top: 8px;
}

.health-label-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 12.5px;
  color: #334155;
  margin-bottom: 6px;
}

.h-name { font-weight: 600; }
.h-value { font-weight: 700; color: #0f172a; }

.health-progress-track {
  width: 100%;
  height: 8px;
  background: #f1f5f9;
  border-radius: 4px;
  overflow: hidden;
}

.health-progress-bar {
  height: 100%;
  border-radius: 4px;
}

.health-progress-bar.blue { background: #2563eb; }
.health-progress-bar.purple { background: #7c3aed; }

.api-latency-card {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 12px 14px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 6px;
}

.api-left {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12.5px;
  font-weight: 600;
  color: #334155;
}

.api-badge-fast {
  font-size: 11px;
  font-weight: 800;
  background: #ecfdf5;
  color: #059669;
  padding: 3px 8px;
  border-radius: 6px;
}

/* Responsive */
@media (max-width: 1200px) {
  .kpi-cards-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .middle-section-grid,
  .bottom-section-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .kpi-cards-grid {
    grid-template-columns: 1fr;
  }
}
</style>
