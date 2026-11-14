<template>
  <div class="analytics-page-container">
    <!-- 1. HEADER SECTION (RESPONSIVE CHỐNG CO BẸP CHỮ) -->
    <div class="analytics-header-card">
      <div class="header-left">
        <div class="header-icon-box">
          <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M3 3v18h18" />
            <path d="m19 9-5 5-4-4-3 3" />
          </svg>
        </div>
        <div class="header-titles-wrap">
          <div class="header-title-row">
            <h1 class="header-title">Báo cáo &amp; Phân tích BI</h1>
            <span class="bi-badge">High-Scale Analytics</span>
          </div>
          <p class="header-subtitle">
            Hệ thống phân tích thông minh thời gian thực • Doanh thu, lưu lượng đơn hàng &amp; định giá tồn kho
          </p>
        </div>
      </div>

      <div class="header-right-actions">
        <el-button
          class="btn-action-outline"
          :loading="exporting"
          @click="handleExportExcel"
        >
          <el-icon><Download /></el-icon>
          <span>Xuất Báo cáo Excel (.xls)</span>
        </el-button>
        <el-button
          type="primary"
          class="btn-action-primary"
          :loading="loading"
          @click="fetchAnalytics"
        >
          <el-icon><Refresh /></el-icon>
          <span>Làm mới</span>
        </el-button>
      </div>
    </div>

    <!-- 2. TIME FILTER TOOLBAR -->
    <div class="time-filter-toolbar">
      <div class="filter-tabs-group">
        <button
          v-for="tab in filterTabs"
          :key="tab.id"
          type="button"
          class="filter-tab-btn"
          :class="{ active: currentTab === tab.id }"
          @click="selectTab(tab.id)"
        >
          {{ tab.label }}
        </button>
      </div>

      <div class="date-range-wrap">
        <span class="range-hint">Khoảng thời gian:</span>
        <el-date-picker
          v-model="customDateRange"
          type="daterange"
          range-separator="—"
          start-placeholder="Từ ngày"
          end-placeholder="Đến ngày"
          format="YYYY-MM-DD"
          value-format="YYYY-MM-DD"
          :clearable="false"
          size="default"
          class="analytics-date-picker"
          @change="onCustomDateChange"
        />
      </div>
    </div>

    <!-- 3. KPI METRIC CARDS GRID -->
    <div class="kpi-metrics-grid">
      <!-- Card 1: Doanh thu thực tế -->
      <div class="kpi-stat-card">
        <div class="kpi-top">
          <span class="kpi-category">TỔNG DOANH THU HỢP LỆ</span>
          <div class="kpi-icon-pill blue">💰</div>
        </div>
        <div class="kpi-main-number">{{ formatCurrency(data.totalRevenue) }}</div>
        <div class="kpi-footer-detail">
          <span>Hoàn tất: <strong>{{ formatCurrency(data.completedRevenue) }}</strong></span>
          <span class="dot-sep">•</span>
          <span class="text-green">Đã thu: {{ formatCurrency(data.paidRevenue) }}</span>
        </div>
      </div>

      <!-- Card 2: Tổng đơn hàng & Hôm nay -->
      <div class="kpi-stat-card">
        <div class="kpi-top">
          <span class="kpi-category">LƯỢNG ĐƠN HÀNG</span>
          <div class="kpi-icon-pill purple">📦</div>
        </div>
        <div class="kpi-main-number">
          {{ formatNumber(data.totalOrders) }}
          <span class="kpi-unit">đơn</span>
        </div>
        <div class="kpi-footer-detail">
          <span class="tag-today">⚡ {{ data.todayOrders }} đơn hôm nay</span>
          <span class="dot-sep">•</span>
          <span>Đã hủy: {{ data.cancelledOrders }}</span>
        </div>
      </div>

      <!-- Card 3: Giá trị trung bình đơn (AOV) -->
      <div class="kpi-stat-card">
        <div class="kpi-top">
          <span class="kpi-category">GIÁ TRỊ TB / ĐƠN (AOV)</span>
          <div class="kpi-icon-pill amber">🎯</div>
        </div>
        <div class="kpi-main-number">{{ formatCurrency(data.averageOrderValue) }}</div>
        <div class="kpi-footer-detail">
          <span>Tỷ lệ hoàn thành: <strong class="text-green">{{ data.completionRate }}%</strong></span>
          <span class="dot-sep">•</span>
          <span>{{ data.completedOrders }} đơn xong</span>
        </div>
      </div>

      <!-- Card 4: Định giá tài sản kho -->
      <div class="kpi-stat-card">
        <div class="kpi-top">
          <span class="kpi-category">ĐỊNH GIÁ TỔN KHO</span>
          <div class="kpi-icon-pill emerald">🏢</div>
        </div>
        <div class="kpi-main-number">{{ formatCurrency(data.inventoryValuation) }}</div>
        <div class="kpi-footer-detail">
          <span>Tổng: <strong>{{ formatNumber(data.totalStockUnits) }}</strong> cái</span>
          <span class="dot-sep">•</span>
          <span :class="data.lowStockCount > 0 ? 'text-danger' : 'text-muted'">
            ⚠️ {{ data.lowStockCount }} sắp hết
          </span>
        </div>
      </div>
    </div>

    <!-- 4. MAIN CHARTS SECTION -->
    <div class="charts-two-col-grid">
      <!-- Cột Trái: Biểu đồ xu hướng Doanh thu & Đơn hàng SVG tương tác -->
      <div class="analytics-box chart-main-box">
        <div class="box-header-row">
          <div>
            <h3 class="box-title">Xu hướng Doanh thu &amp; Đơn hàng</h3>
            <p class="box-desc">Dữ liệu tổng hợp theo từng ngày trong khoảng thời gian đã chọn</p>
          </div>
          <div class="chart-legend-row">
            <span class="legend-item"><span class="legend-dot blue"></span> Doanh thu (VNĐ)</span>
            <span class="legend-item"><span class="legend-dot purple"></span> Số lượng đơn</span>
          </div>
        </div>

        <div class="chart-stage-wrapper">
          <div v-if="!data.revenueTrends || data.revenueTrends.length === 0" class="empty-chart">
            Chưa có dữ liệu giao dịch trong khoảng thời gian này
          </div>

          <!-- SVG Chart Area -->
          <div v-else class="svg-chart-container">
            <!-- Tooltip nổi (Nền trắng to rõ ràng, dễ nhìn) -->
            <div
              v-if="hoveredPoint"
              class="chart-floating-tooltip"
              :style="{ left: hoveredPoint.x + 'px', top: (hoveredPoint.y - 88) + 'px' }"
            >
              <div class="tt-header">
                <span class="tt-date-txt">📅 {{ hoveredPoint.date }}</span>
                <span class="tt-badge-point">Ngày {{ hoveredPoint.label }}</span>
              </div>
              <div class="tt-body">
                <div class="tt-revenue-row">
                  <span class="tt-lbl">Doanh thu:</span>
                  <span class="tt-val-revenue">{{ formatCurrency(hoveredPoint.revenue) }}</span>
                </div>
                <div class="tt-order-row">
                  <span class="tt-lbl">Lượng đơn:</span>
                  <span class="tt-val-orders">{{ hoveredPoint.orderCount }} đơn hàng</span>
                </div>
              </div>
            </div>

            <svg viewBox="0 0 800 240" class="revenue-svg" preserveAspectRatio="none">
              <defs>
                <linearGradient id="revenueGrad" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="0%" stop-color="#3b82f6" stop-opacity="0.32" />
                  <stop offset="100%" stop-color="#3b82f6" stop-opacity="0.0" />
                </linearGradient>
              </defs>

              <!-- Grid Horizontal Lines -->
              <line x1="40" y1="30" x2="780" y2="30" stroke="#f1f5f9" stroke-dasharray="3,3" />
              <line x1="40" y1="80" x2="780" y2="80" stroke="#f1f5f9" stroke-dasharray="3,3" />
              <line x1="40" y1="130" x2="780" y2="130" stroke="#f1f5f9" stroke-dasharray="3,3" />
              <line x1="40" y1="180" x2="780" y2="180" stroke="#f1f5f9" />

              <!-- Bar: Cột số lượng đơn hàng nhạt phía sau -->
              <rect
                v-for="(pt, idx) in chartCoordinates"
                :key="'bar-' + idx"
                :x="pt.x - 6"
                :y="pt.barY"
                width="12"
                :height="180 - pt.barY"
                rx="3"
                class="chart-bar-rect"
              />

              <!-- Area Fill dưới đường cong Doanh thu -->
              <path :d="areaPathString" fill="url(#revenueGrad)" />

              <!-- Spline Line Doanh thu -->
              <path :d="linePathString" fill="none" stroke="#2563eb" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" />

              <!-- Tọa độ các điểm tròn -->
              <circle
                v-for="(pt, idx) in chartCoordinates"
                :key="'circle-' + idx"
                :cx="pt.x"
                :cy="pt.y"
                r="4.5"
                class="chart-point-circle"
                :class="{ active: hoveredPoint && hoveredPoint.index === idx }"
                @mouseenter="onPointHover(pt, idx)"
                @mouseleave="hoveredPoint = null"
              />

              <!-- Trục X nhãn ngày tháng -->
              <text
                v-for="(pt, idx) in xLabelCoordinates"
                :key="'txt-' + idx"
                :x="pt.x"
                y="205"
                text-anchor="middle"
                class="svg-x-axis-text"
              >
                {{ pt.label }}
              </text>
            </svg>
          </div>
        </div>
      </div>

      <!-- Cột Phải: Phân bổ Trạng thái & Phương thức thanh toán -->
      <div class="analytics-box side-breakdown-box">
        <div class="box-header-row">
          <div>
            <h3 class="box-title">Cơ cấu Trạng thái &amp; Thanh toán</h3>
            <p class="box-desc">Tỷ trọng các giai đoạn đơn &amp; kênh dòng tiền</p>
          </div>
        </div>

        <!-- Section 1: Order Status Breakdown -->
        <div class="breakdown-sub-section">
          <div class="sub-section-title">TRẠNG THÁI ĐƠN HÀNG</div>
          <div v-if="!data.orderStatusBreakdown || data.orderStatusBreakdown.length === 0" class="empty-sub">
            Chưa có phát sinh đơn
          </div>
          <div v-else class="status-bars-stack">
            <div
              v-for="st in data.orderStatusBreakdown"
              :key="st.status"
              class="status-bar-row"
            >
              <div class="bar-label-line">
                <span class="st-indicator" :style="{ backgroundColor: st.color }"></span>
                <span class="st-name">{{ st.label }}</span>
                <span class="st-count">{{ st.count }} đơn ({{ st.percentage }}%)</span>
              </div>
              <div class="bar-progress-track">
                <div
                  class="bar-progress-fill"
                  :style="{ width: st.percentage + '%', backgroundColor: st.color }"
                ></div>
              </div>
            </div>
          </div>
        </div>

        <div class="section-divider"></div>

        <!-- Section 2: Payment Methods Breakdown -->
        <div class="breakdown-sub-section">
          <div class="sub-section-title">KÊNH THANH TOÁN</div>
          <div v-if="!data.paymentMethodBreakdown || data.paymentMethodBreakdown.length === 0" class="empty-sub">
            Chưa có dữ liệu thanh toán
          </div>
          <div v-else class="payment-method-grid">
            <div
              v-for="pm in data.paymentMethodBreakdown"
              :key="pm.method"
              class="payment-stat-card"
            >
              <div class="pm-top">
                <span class="pm-icon" :style="{ color: pm.color }">●</span>
                <span class="pm-label">{{ pm.label }}</span>
              </div>
              <div class="pm-amount">{{ formatCurrency(pm.totalAmount) }}</div>
              <div class="pm-footer">
                <span>{{ pm.count }} giao dịch</span>
                <span class="pm-pct">{{ pm.percentage }}%</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 5. BOTTOM DETAILS: TOP PRODUCTS & LOW STOCK ALERTS -->
    <div class="bottom-tables-grid">
      <!-- Cột Trái: Top Sản phẩm Bán chạy -->
      <div class="analytics-box table-box">
        <div class="box-header-row">
          <div>
            <h3 class="box-title">Top Sản phẩm Bán chạy nhất</h3>
            <p class="box-desc">Xếp hạng theo tổng doanh thu mang lại</p>
          </div>
          <span class="box-badge-count">{{ data.topSellingProducts?.length || 0 }} sản phẩm</span>
        </div>

        <div v-if="!data.topSellingProducts || data.topSellingProducts.length === 0" class="empty-table-state">
          Chưa có sản phẩm nào phát sinh giao dịch trong kỳ
        </div>

        <div v-else class="clean-table-wrapper">
          <table class="clean-bi-table">
            <thead>
              <tr>
                <th style="width: 50px">Hạng</th>
                <th>Sản phẩm &amp; Mã SKU</th>
                <th style="width: 90px; text-align: center">Đã bán</th>
                <th style="width: 140px; text-align: right">Doanh thu</th>
                <th style="width: 90px; text-align: right">Tồn hiện tại</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(prod, idx) in data.topSellingProducts" :key="prod.productId">
                <td>
                  <span class="rank-badge" :class="'rank-' + (idx + 1)">{{ idx + 1 }}</span>
                </td>
                <td>
                  <div class="prod-cell">
                    <img
                      v-if="prod.productImageUrl"
                      :src="prod.productImageUrl"
                      class="prod-mini-thumb"
                      alt=""
                    />
                    <div v-else class="prod-thumb-fallback">📦</div>
                    <div class="prod-info">
                      <div class="prod-name" :title="prod.productName">{{ prod.productName }}</div>
                      <div class="prod-sku">{{ prod.productSku }}</div>
                    </div>
                  </div>
                </td>
                <td style="text-align: center">
                  <span class="qty-pill">{{ formatNumber(prod.totalQuantitySold) }}</span>
                </td>
                <td style="text-align: right" class="font-bold">
                  {{ formatCurrency(prod.totalRevenue) }}
                </td>
                <td style="text-align: right">
                  <span :class="prod.currentStock <= 10 ? 'stock-low' : 'stock-ok'">
                    {{ prod.currentStock }} cái
                  </span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- Cột Phải: Cảnh báo Tồn kho Sắp hết (Low Stock Alerts) -->
      <div class="analytics-box table-box">
        <div class="box-header-row">
          <div>
            <h3 class="box-title">Cảnh báo Tồn kho Sắp hết</h3>
            <p class="box-desc">Sản phẩm có lượng tồn kho an toàn &le; 10 cái</p>
          </div>
          <el-button
            type="primary"
            link
            class="link-quick-orders"
            @click="$router.push('/orders')"
          >
            Quản lý Kho ›
          </el-button>
        </div>

        <div v-if="!data.lowStockAlerts || data.lowStockAlerts.length === 0" class="all-stock-safe-state">
          <div class="safe-icon">✅</div>
          <div class="safe-text">Toàn bộ kho hàng đều đang ở ngưỡng an toàn!</div>
        </div>

        <div v-else class="clean-table-wrapper">
          <table class="clean-bi-table">
            <thead>
              <tr>
                <th>Sản phẩm</th>
                <th style="width: 100px">Danh mục</th>
                <th style="width: 90px; text-align: center">Tồn kho</th>
                <th style="width: 120px; text-align: right">Đơn giá</th>
                <th style="width: 80px; text-align: center">Thao tác</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in data.lowStockAlerts" :key="item.productId">
                <td>
                  <div class="prod-cell">
                    <img
                      v-if="item.productImageUrl"
                      :src="item.productImageUrl"
                      class="prod-mini-thumb"
                      alt=""
                    />
                    <div v-else class="prod-thumb-fallback">⚠️</div>
                    <div class="prod-info">
                      <div class="prod-name" :title="item.productName">{{ item.productName }}</div>
                      <div class="prod-sku">{{ item.productSku }}</div>
                    </div>
                  </div>
                </td>
                <td>
                  <span class="cat-pill">{{ item.categoryName }}</span>
                </td>
                <td style="text-align: center">
                  <span class="danger-stock-badge">
                    {{ item.currentStock }} cái
                  </span>
                </td>
                <td style="text-align: right" class="font-bold">
                  {{ formatCurrency(item.price) }}
                </td>
                <td style="text-align: center">
                  <el-button
                    size="small"
                    type="primary"
                    plain
                    class="btn-quick-adjust"
                    @click="$router.push('/orders')"
                  >
                    Nhập kho
                  </el-button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
// =========================================================================
// 1. HELPER FUNCTIONS (ĐẶT ĐẦU SCRIPT TRÁNH TEMPORAL DEAD ZONE TRÊN VUE 3)
// =========================================================================
function formatCurrency(val) {
  if (val == null || isNaN(val)) return '0 đ'
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(val)
}

function formatNumber(val) {
  if (val == null || isNaN(val)) return '0'
  return new Intl.NumberFormat('vi-VN').format(val)
}

function toLocalDateString(d) {
  const year = d.getFullYear()
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function formatToLocalDateTimeIso(dateStr, isEndOfDay = false) {
  if (!dateStr) return null
  return isEndOfDay ? `${dateStr}T23:59:59` : `${dateStr}T00:00:00`
}

function downloadBlob(dataBytes, filename) {
  const blob = new Blob([dataBytes], { type: 'application/vnd.ms-excel;charset=UTF-8' })
  const url = window.URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  window.URL.revokeObjectURL(url)
}

import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Download, Refresh } from '@element-plus/icons-vue'
import api from '../api'

// =========================================================================
// 2. STATE & REFS
// =========================================================================
const loading = ref(false)
const exporting = ref(false)
const currentTab = ref('month')
const customDateRange = ref([])
const hoveredPoint = ref(null)
let isFetching = false

const filterTabs = [
  { id: 'today', label: 'Hôm nay' },
  { id: '7days', label: '7 ngày qua' },
  { id: 'month', label: '30 ngày qua' },
  { id: 'thisMonth', label: 'Tháng này' },
  { id: 'year', label: 'Năm nay' }
]

const data = reactive({
  totalRevenue: 0,
  completedRevenue: 0,
  paidRevenue: 0,
  totalOrders: 0,
  completedOrders: 0,
  cancelledOrders: 0,
  todayOrders: 0,
  averageOrderValue: 0,
  completionRate: 0,
  inventoryValuation: 0,
  totalStockUnits: 0,
  lowStockCount: 0,
  revenueTrends: [],
  orderStatusBreakdown: [],
  paymentMethodBreakdown: [],
  topSellingProducts: [],
  lowStockAlerts: []
})

// =========================================================================
// 3. COMPUTED SVG CHART COORDINATES
// =========================================================================
const chartCoordinates = computed(() => {
  const trends = data.revenueTrends || []
  if (trends.length === 0) return []

  const maxRevenue = Math.max(...trends.map(t => Number(t.revenue) || 0), 100000)
  const maxOrders = Math.max(...trends.map(t => Number(t.orderCount) || 0), 1)

  const leftPad = 50
  const rightPad = 760
  const widthSpan = rightPad - leftPad
  const count = trends.length
  const step = count > 1 ? widthSpan / (count - 1) : 0

  return trends.map((t, idx) => {
    const x = count === 1 ? 400 : leftPad + idx * step
    const rev = Number(t.revenue) || 0
    // Trục y doanh thu từ y=180 (đáy) lên y=35 (đỉnh)
    const y = 180 - (rev / maxRevenue) * 145

    // Chiều cao cột đơn hàng (y từ 180 lên tối đa y=110)
    const ord = Number(t.orderCount) || 0
    const barHeight = Math.max((ord / maxOrders) * 70, 0)
    const barY = 180 - barHeight

    return {
      x,
      y,
      barY,
      date: t.date,
      label: t.label,
      revenue: rev,
      orderCount: ord
    }
  })
})

const linePathString = computed(() => {
  const coords = chartCoordinates.value
  if (coords.length === 0) return ''
  return coords.reduce((acc, curr, idx) => {
    if (idx === 0) return `M ${curr.x} ${curr.y}`
    return `${acc} L ${curr.x} ${curr.y}`
  }, '')
})

const areaPathString = computed(() => {
  const coords = chartCoordinates.value
  if (coords.length === 0) return ''
  const first = coords[0]
  const last = coords[coords.length - 1]
  const linePart = coords.reduce((acc, curr, idx) => {
    if (idx === 0) return `M ${curr.x} ${curr.y}`
    return `${acc} L ${curr.x} ${curr.y}`
  }, '')
  return `${linePart} L ${last.x} 180 L ${first.x} 180 Z`
})

const xLabelCoordinates = computed(() => {
  const coords = chartCoordinates.value
  if (coords.length === 0) return []
  // Lấy mẫu nhãn (tối đa khoảng 7-8 nhãn để không đè chữ)
  const total = coords.length
  if (total <= 8) return coords
  const step = Math.ceil(total / 7)
  return coords.filter((_, idx) => idx % step === 0 || idx === total - 1)
})

function onPointHover(pt, idx) {
  hoveredPoint.value = {
    ...pt,
    index: idx
  }
}

// =========================================================================
// 4. ZERO-404 DATA ENGINE: SỬ DỤNG 100% CÁC API BACKEND ĐANG HOẠT ĐỘNG
// =========================================================================
function computeRangeFromTab(tabId) {
  const now = new Date()
  const toDateStr = toLocalDateString(now)

  if (tabId === 'today') {
    return [toDateStr, toDateStr]
  }
  if (tabId === '7days') {
    const from = new Date()
    from.setDate(now.getDate() - 6)
    return [toLocalDateString(from), toDateStr]
  }
  if (tabId === 'month') {
    const from = new Date()
    from.setDate(now.getDate() - 29)
    return [toLocalDateString(from), toDateStr]
  }
  if (tabId === 'thisMonth') {
    const firstDay = new Date(now.getFullYear(), now.getMonth(), 1)
    return [toLocalDateString(firstDay), toDateStr]
  }
  if (tabId === 'year') {
    const firstDay = new Date(now.getFullYear(), 0, 1)
    return [toLocalDateString(firstDay), toDateStr]
  }
  return [toDateStr, toDateStr]
}

function selectTab(tabId) {
  currentTab.value = tabId
  const range = computeRangeFromTab(tabId)
  customDateRange.value = range
  fetchAnalytics()
}

function onCustomDateChange(val) {
  if (val && val.length === 2) {
    currentTab.value = 'custom'
    fetchAnalytics()
  }
}

async function fetchAnalytics() {
  if (isFetching) return
  isFetching = true
  loading.value = true

  try {
    const [fromDate, toDate] = customDateRange.value && customDateRange.value.length === 2
      ? customDateRange.value
      : computeRangeFromTab('month')

    const fromIso = formatToLocalDateTimeIso(fromDate, false)
    const toIso = formatToLocalDateTimeIso(toDate, true)

    // Chỉ gọi các API đã xác nhận 100% tồn tại và hoạt động trong Backend:
    // 1. GET /api/orders/stats -> Thống kê KPI tổng quan
    // 2. GET /api/orders -> Danh sách đơn hàng trong dải ngày để dựng xu hướng & phân bổ
    // 3. GET /api/products -> Định giá tài sản kho & kiểm tra tồn kho
    const [orderStatsRes, ordersPageRes, productsRes] = await Promise.all([
      api.get('/orders/stats'),
      api.get('/orders', { params: { fromDate: fromIso, toDate: toIso, page: 0, size: 100 } }),
      api.get('/products', { params: { page: 0, size: 100 } })
    ])

    // A. Nạp KPI tổng hợp
    if (orderStatsRes?.data) {
      const s = orderStatsRes.data
      data.totalOrders = s.totalOrders ?? 0
      data.completedOrders = s.completedOrders ?? 0
      data.cancelledOrders = s.cancelledOrders ?? 0
      data.todayOrders = s.todayOrders ?? 0
      data.totalRevenue = s.activeRevenue ?? 0
      data.completedRevenue = s.completedRevenue ?? 0
      data.paidRevenue = s.paidRevenue ?? 0
      data.averageOrderValue = s.avgOrderValue ?? 0
      data.completionRate = s.completionRate ?? 0
    }

    // B. Xử lý dữ liệu đơn hàng theo thời gian
    const ordersList = ordersPageRes?.data?.content || ordersPageRes?.data || []
    
    // B1. Dựng biểu đồ xu hướng doanh thu & số đơn theo ngày trong dải ngày
    const dayMap = {}
    for (const ord of ordersList) {
      if (!ord.createdAt) continue
      const dateKey = String(ord.createdAt).slice(0, 10)
      if (!dayMap[dateKey]) {
        dayMap[dateKey] = { revenue: 0, count: 0 }
      }
      dayMap[dateKey].count += 1
      if (ord.status !== 'CANCELLED') {
        dayMap[dateKey].revenue += Number(ord.totalAmount) || 0
      }
    }

    const trends = []
    let cur = new Date(fromDate)
    const end = new Date(toDate)
    while (cur <= end) {
      const dKey = toLocalDateString(cur)
      const dayData = dayMap[dKey] || { revenue: 0, count: 0 }
      const dParts = dKey.split('-')
      const displayLabel = dParts.length === 3 ? `${dParts[2]}/${dParts[1]}` : dKey
      trends.push({
        date: dKey,
        label: displayLabel,
        revenue: dayData.revenue,
        orderCount: dayData.count
      })
      cur.setDate(cur.getDate() + 1)
    }
    data.revenueTrends = trends

    // B2. Cơ cấu trạng thái đơn hàng
    const statusCounts = {
      PENDING: 0,
      CONFIRMED: 0,
      SHIPPING: 0,
      COMPLETED: 0,
      CANCELLED: 0
    }
    const paymentCounts = {}

    for (const ord of ordersList) {
      if (ord.status && statusCounts[ord.status] !== undefined) {
        statusCounts[ord.status] += 1
      }
      const pm = ord.paymentMethod || 'CASH'
      if (!paymentCounts[pm]) {
        paymentCounts[pm] = { count: 0, amount: 0, label: ord.paymentMethodLabel || pm }
      }
      paymentCounts[pm].count += 1
      if (ord.status !== 'CANCELLED') {
        paymentCounts[pm].amount += Number(ord.totalAmount) || 0
      }
    }

    const totalStatusCount = Object.values(statusCounts).reduce((a, b) => a + b, 0) || 1
    data.orderStatusBreakdown = [
      { status: 'PENDING', label: 'Chờ xác nhận', count: statusCounts.PENDING, percentage: Math.round((statusCounts.PENDING * 1000) / totalStatusCount) / 10, color: '#f59e0b' },
      { status: 'CONFIRMED', label: 'Đã xác nhận', count: statusCounts.CONFIRMED, percentage: Math.round((statusCounts.CONFIRMED * 1000) / totalStatusCount) / 10, color: '#3b82f6' },
      { status: 'SHIPPING', label: 'Đang giao', count: statusCounts.SHIPPING, percentage: Math.round((statusCounts.SHIPPING * 1000) / totalStatusCount) / 10, color: '#8b5cf6' },
      { status: 'COMPLETED', label: 'Hoàn thành', count: statusCounts.COMPLETED, percentage: Math.round((statusCounts.COMPLETED * 1000) / totalStatusCount) / 10, color: '#10b981' },
      { status: 'CANCELLED', label: 'Đã hủy', count: statusCounts.CANCELLED, percentage: Math.round((statusCounts.CANCELLED * 1000) / totalStatusCount) / 10, color: '#ef4444' }
    ]

    // B3. Phân bổ phương thức thanh toán
    const totalPayCount = Object.values(paymentCounts).reduce((acc, c) => acc + c.count, 0) || 1
    const colorMap = {
      CASH: '#10b981',
      BANK_TRANSFER: '#2563eb',
      CREDIT_CARD: '#8b5cf6',
      E_WALLET: '#f59e0b'
    }
    data.paymentMethodBreakdown = Object.keys(paymentCounts).map(k => ({
      method: k,
      label: paymentCounts[k].label,
      count: paymentCounts[k].count,
      totalAmount: paymentCounts[k].amount,
      percentage: Math.round((paymentCounts[k].count * 1000) / totalPayCount) / 10,
      color: colorMap[k] || '#64748b'
    }))

    // B4. Tổng hợp Top sản phẩm bán chạy nhất từ Order Items
    const prodSaleMap = {}
    for (const ord of ordersList) {
      if (ord.status === 'CANCELLED' || !ord.items) continue
      for (const it of ord.items) {
        if (!it.productId) continue
        if (!prodSaleMap[it.productId]) {
          prodSaleMap[it.productId] = {
            productId: it.productId,
            productName: it.productName,
            productSku: it.productSku,
            productImageUrl: it.productImageUrl,
            totalQuantitySold: 0,
            totalRevenue: 0,
            currentStock: 0
          }
        }
        prodSaleMap[it.productId].totalQuantitySold += Number(it.quantity) || 0
        prodSaleMap[it.productId].totalRevenue += Number(it.lineTotal) || 0
      }
    }

    // C. Xử lý dữ liệu sản phẩm & kho
    const productList = productsRes?.data?.content || productsRes?.data || []
    let totalStockUnits = 0
    let inventoryValuation = 0
    const lowStockAlerts = []

    for (const p of productList) {
      const stock = Number(p.stock) || 0
      const price = Number(p.price) || 0
      totalStockUnits += stock
      inventoryValuation += stock * price

      // Cập nhật tồn kho vào top product map
      if (prodSaleMap[p.id]) {
        prodSaleMap[p.id].currentStock = stock
      }

      if (stock <= 10) {
        lowStockAlerts.push({
          productId: p.id,
          productName: p.name,
          productSku: p.sku,
          productImageUrl: p.imageUrl,
          currentStock: stock,
          price: price,
          categoryName: 'Sản phẩm ERP'
        })
      }
    }

    lowStockAlerts.sort((a, b) => a.currentStock - b.currentStock)
    data.inventoryValuation = inventoryValuation
    data.totalStockUnits = totalStockUnits
    data.lowStockCount = lowStockAlerts.length
    data.lowStockAlerts = lowStockAlerts.slice(0, 8)

    // Sắp xếp top sản phẩm theo doanh thu mang lại
    data.topSellingProducts = Object.values(prodSaleMap)
      .sort((a, b) => b.totalRevenue - a.totalRevenue)
      .slice(0, 8)

  } catch (err) {
    ElMessage.error(err?.response?.data?.message || 'Không thể tải dữ liệu phân tích BI!')
  } finally {
    loading.value = false
    isFetching = false
  }
}

async function handleExportExcel() {
  exporting.value = true
  try {
    const [fromDate, toDate] = customDateRange.value && customDateRange.value.length === 2
      ? customDateRange.value
      : computeRangeFromTab('month')

    // Gọi API export-excel có sẵn trong OrderController (200 OK)
    const res = await api.get('/orders/export-excel', {
      responseType: 'blob'
    })
    downloadBlob(res.data, `Bao_Cao_ERP_BI_${fromDate}_${toDate}.xls`)
    ElMessage.success('Đã xuất báo cáo BI đa sheet thành công!')
  } catch {
    ElMessage.error('Xuất báo cáo Excel thất bại!')
  } finally {
    exporting.value = false
  }
}

onMounted(() => {
  customDateRange.value = computeRangeFromTab('month')
  fetchAnalytics()
})
</script>

<style scoped>
.analytics-page-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding-bottom: 64px;
}

/* 1. Header Card - RESPONSIVE CHỐNG CO BẸP CHỮ */
.analytics-header-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
  background: #ffffff;
  border-radius: 12px;
  padding: 16px 20px;
  border: 1px solid #e2e8f0;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.03);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 14px;
  flex: 1 1 320px;
  min-width: 0;
}

.header-icon-box {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  background: #eff6ff;
  color: #2563eb;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.header-titles-wrap {
  min-width: 0;
  flex: 1 1 auto;
}

.header-title-row {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.header-title {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  color: #0f172a;
  white-space: nowrap;
}

.bi-badge {
  font-size: 11px;
  font-weight: 600;
  background: #e0f2fe;
  color: #0284c7;
  padding: 2px 8px;
  border-radius: 6px;
  text-transform: uppercase;
  white-space: nowrap;
}

.header-subtitle {
  margin: 3px 0 0;
  font-size: 13px;
  color: #64748b;
  line-height: 1.4;
}

.header-right-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  flex-shrink: 0;
}

.btn-action-outline {
  font-weight: 600;
  border-color: #cbd5e1;
  white-space: nowrap;
  height: 34px;
}

.btn-action-primary {
  font-weight: 600;
  background: #2563eb;
  border-color: #2563eb;
  white-space: nowrap;
  height: 34px;
}

@media (max-width: 900px) {
  .analytics-header-card {
    flex-direction: column;
    align-items: stretch;
    gap: 12px;
  }
  .header-left {
    flex: 1 1 100%;
  }
  .header-right-actions {
    width: 100%;
    justify-content: flex-start;
  }
  .btn-action-outline,
  .btn-action-primary {
    flex: 1 1 auto;
  }
}

/* 2. Filter Toolbar */
.time-filter-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
  background: #ffffff;
  border-radius: 10px;
  padding: 10px 16px;
  border: 1px solid #e2e8f0;
}

.filter-tabs-group {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 4px;
  background: #f1f5f9;
  padding: 3px;
  border-radius: 8px;
}

.filter-tab-btn {
  border: none;
  background: transparent;
  padding: 6px 14px;
  font-size: 13px;
  font-weight: 500;
  color: #475569;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.15s ease;
  white-space: nowrap;
}

.filter-tab-btn:hover {
  color: #0f172a;
}

.filter-tab-btn.active {
  background: #ffffff;
  color: #2563eb;
  font-weight: 600;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
}

.date-range-wrap {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.range-hint {
  font-size: 13px;
  color: #64748b;
  white-space: nowrap;
}

.analytics-date-picker {
  width: 250px;
}

@media (max-width: 820px) {
  .time-filter-toolbar {
    flex-direction: column;
    align-items: stretch;
  }
  .filter-tabs-group {
    width: 100%;
    justify-content: space-between;
  }
  .filter-tab-btn {
    flex: 1 1 auto;
    text-align: center;
    padding: 6px 8px;
  }
  .date-range-wrap {
    width: 100%;
    justify-content: space-between;
  }
  .analytics-date-picker {
    flex: 1 1 auto;
    width: 100% !important;
  }
}

/* 3. KPI Metrics Grid */
.kpi-metrics-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

@media (max-width: 1024px) {
  .kpi-metrics-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .kpi-metrics-grid {
    grid-template-columns: 1fr;
  }
}

.kpi-stat-card {
  background: #ffffff;
  border-radius: 10px;
  border: 1px solid #e2e8f0;
  padding: 14px 16px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.02);
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.kpi-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.kpi-category {
  font-size: 11px;
  font-weight: 700;
  color: #64748b;
  letter-spacing: 0.5px;
}

.kpi-icon-pill {
  width: 28px;
  height: 28px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
}

.kpi-icon-pill.blue { background: #eff6ff; }
.kpi-icon-pill.purple { background: #faf5ff; }
.kpi-icon-pill.amber { background: #fffbeb; }
.kpi-icon-pill.emerald { background: #ecfdf5; }

.kpi-main-number {
  font-size: 20px;
  font-weight: 700;
  color: #0f172a;
  margin: 10px 0 6px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.kpi-unit {
  font-size: 13px;
  font-weight: 500;
  color: #64748b;
}

.kpi-footer-detail {
  font-size: 12px;
  color: #64748b;
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

.dot-sep {
  color: #cbd5e1;
}

.text-green { color: #10b981; }
.text-danger { color: #ef4444; font-weight: 600; }
.text-muted { color: #64748b; }

.tag-today {
  color: #2563eb;
  font-weight: 600;
}

/* 4. Charts Section */
.charts-two-col-grid {
  display: grid;
  grid-template-columns: minmax(0, 2fr) minmax(0, 1.2fr);
  gap: 16px;
}

@media (max-width: 1100px) {
  .charts-two-col-grid {
    grid-template-columns: 1fr;
  }
}

.analytics-box {
  background: #ffffff;
  border-radius: 12px;
  border: 1px solid #e2e8f0;
  padding: 16px 20px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.02);
  min-width: 0;
}

.box-header-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
  flex-wrap: wrap;
  gap: 8px;
}

.box-title {
  margin: 0;
  font-size: 15px;
  font-weight: 700;
  color: #0f172a;
  white-space: nowrap;
}

.box-desc {
  margin: 3px 0 0;
  font-size: 12px;
  color: #64748b;
}

.chart-legend-row {
  display: flex;
  align-items: center;
  gap: 14px;
  font-size: 12px;
  color: #475569;
  flex-wrap: wrap;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 6px;
  white-space: nowrap;
}

.legend-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.legend-dot.blue { background: #2563eb; }
.legend-dot.purple { background: #8b5cf6; }

.chart-stage-wrapper {
  position: relative;
  min-height: 220px;
}

.empty-chart {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 200px;
  font-size: 13px;
  color: #94a3b8;
  text-align: center;
  padding: 16px;
}

.svg-chart-container {
  position: relative;
  width: 100%;
}

.revenue-svg {
  width: 100%;
  height: 220px;
  overflow: visible;
}

.chart-bar-rect {
  fill: #e0e7ff;
  transition: all 0.2s ease;
}

.chart-bar-rect:hover {
  fill: #c7d2fe;
}

.chart-point-circle {
  fill: #ffffff;
  stroke: #2563eb;
  stroke-width: 2.5;
  cursor: pointer;
  transition: all 0.15s ease;
}

.chart-point-circle:hover,
.chart-point-circle.active {
  r: 6.5;
  fill: #2563eb;
  stroke: #ffffff;
}

.svg-x-axis-text {
  font-size: 11px;
  fill: #94a3b8;
}

.chart-floating-tooltip {
  position: absolute;
  pointer-events: none;
  background: #ffffff;
  color: #0f172a;
  padding: 10px 14px;
  border-radius: 10px;
  border: 1px solid #e2e8f0;
  transform: translateX(-50%);
  box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.12), 0 8px 10px -6px rgba(0, 0, 0, 0.08);
  z-index: 20;
  white-space: nowrap;
  min-width: 175px;
}

.chart-floating-tooltip::after {
  content: '';
  position: absolute;
  bottom: -6px;
  left: 50%;
  transform: translateX(-50%) rotate(45deg);
  width: 10px;
  height: 10px;
  background: #ffffff;
  border-right: 1px solid #e2e8f0;
  border-bottom: 1px solid #e2e8f0;
}

.tt-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding-bottom: 6px;
  margin-bottom: 6px;
  border-bottom: 1px solid #f1f5f9;
}

.tt-date-txt {
  font-size: 12px;
  font-weight: 700;
  color: #334155;
}

.tt-badge-point {
  font-size: 11px;
  font-weight: 600;
  background: #eff6ff;
  color: #2563eb;
  padding: 1px 6px;
  border-radius: 4px;
}

.tt-body {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.tt-revenue-row,
.tt-order-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  font-size: 13px;
}

.tt-lbl {
  color: #64748b;
  font-weight: 500;
}

.tt-val-revenue {
  font-size: 15px;
  font-weight: 800;
  color: #2563eb;
}

.tt-val-orders {
  font-size: 13px;
  font-weight: 700;
  color: #0f172a;
}

/* Side Breakdown */
.breakdown-sub-section {
  margin-top: 8px;
}

.sub-section-title {
  font-size: 11px;
  font-weight: 700;
  color: #64748b;
  letter-spacing: 0.5px;
  margin-bottom: 10px;
}

.empty-sub {
  font-size: 12px;
  color: #94a3b8;
  padding: 8px 0;
}

.status-bars-stack {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.bar-label-line {
  display: flex;
  align-items: center;
  font-size: 12px;
  gap: 8px;
  margin-bottom: 4px;
}

.st-indicator {
  width: 8px;
  height: 8px;
  border-radius: 2px;
}

.st-name {
  font-weight: 500;
  color: #334155;
  flex: 1;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.st-count {
  font-weight: 600;
  color: #0f172a;
  white-space: nowrap;
}

.bar-progress-track {
  height: 6px;
  background: #f1f5f9;
  border-radius: 3px;
  overflow: hidden;
}

.bar-progress-fill {
  height: 100%;
  border-radius: 3px;
  transition: width 0.3s ease;
}

.section-divider {
  height: 1px;
  background: #f1f5f9;
  margin: 16px 0;
}

.payment-method-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.payment-stat-card {
  background: #f8fafc;
  border-radius: 8px;
  padding: 10px 12px;
  border: 1px solid #f1f5f9;
}

.pm-top {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #475569;
}

.pm-label {
  font-weight: 600;
  white-space: nowrap;
}

.pm-amount {
  font-size: 14px;
  font-weight: 700;
  color: #0f172a;
  margin: 4px 0 2px;
  white-space: nowrap;
}

.pm-footer {
  display: flex;
  justify-content: space-between;
  font-size: 11px;
  color: #64748b;
}

.pm-pct {
  font-weight: 600;
  color: #2563eb;
}

/* 5. Bottom Tables */
.bottom-tables-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

@media (max-width: 1024px) {
  .bottom-tables-grid {
    grid-template-columns: 1fr;
  }
}

.box-badge-count {
  font-size: 12px;
  font-weight: 600;
  background: #f1f5f9;
  color: #475569;
  padding: 2px 8px;
  border-radius: 6px;
  white-space: nowrap;
}

.empty-table-state,
.all-stock-safe-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 30px;
  color: #64748b;
  font-size: 13px;
}

.safe-icon {
  font-size: 24px;
  margin-bottom: 6px;
}

.clean-table-wrapper {
  overflow-x: auto;
  margin-top: 6px;
}

.clean-bi-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

.clean-bi-table th {
  text-align: left;
  padding: 8px 10px;
  font-size: 11px;
  font-weight: 700;
  color: #64748b;
  text-transform: uppercase;
  border-bottom: 1px solid #e2e8f0;
  background: #fafafa;
  white-space: nowrap;
}

.clean-bi-table td {
  padding: 10px;
  border-bottom: 1px solid #f1f5f9;
  vertical-align: middle;
}

.rank-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  font-size: 11px;
  font-weight: 700;
  background: #f1f5f9;
  color: #475569;
}

.rank-badge.rank-1 { background: #fef3c7; color: #b45309; }
.rank-badge.rank-2 { background: #f1f5f9; color: #334155; }
.rank-badge.rank-3 { background: #ffedd5; color: #c2410c; }

.prod-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.prod-mini-thumb {
  width: 34px;
  height: 34px;
  border-radius: 6px;
  object-fit: cover;
  border: 1px solid #e2e8f0;
  flex-shrink: 0;
}

.prod-thumb-fallback {
  width: 34px;
  height: 34px;
  border-radius: 6px;
  background: #f1f5f9;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  flex-shrink: 0;
}

.prod-info {
  min-width: 0;
}

.prod-name {
  font-weight: 600;
  color: #0f172a;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 170px;
}

.prod-sku {
  font-size: 11px;
  color: #64748b;
  font-family: monospace;
}

.qty-pill {
  font-weight: 700;
  background: #eff6ff;
  color: #2563eb;
  padding: 2px 8px;
  border-radius: 6px;
  font-size: 12px;
}

.stock-ok {
  color: #0f172a;
  font-weight: 500;
}

.stock-low {
  color: #ef4444;
  font-weight: 700;
}

.cat-pill {
  font-size: 11px;
  background: #f1f5f9;
  color: #475569;
  padding: 2px 6px;
  border-radius: 4px;
}

.danger-stock-badge {
  font-weight: 700;
  color: #dc2626;
  background: #fee2e2;
  padding: 2px 8px;
  border-radius: 6px;
  font-size: 12px;
}

.btn-quick-adjust {
  font-size: 11px;
  padding: 4px 8px;
}

.font-bold {
  font-weight: 600;
}
</style>
