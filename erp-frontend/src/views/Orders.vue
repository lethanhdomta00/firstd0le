<template>
  <div class="orders-container">
    <!-- TOP HEADER CARD -->
    <header class="orders-header-card">
      <div class="header-left-col">
        <div class="header-icon-box">
          <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M9 5H7a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V7a2 2 0 0 0-2-2h-2" />
            <rect x="9" y="3" width="6" height="4" rx="1" />
            <path d="M9 12h6M9 16h4" />
          </svg>
        </div>
        <div class="header-title-block">
          <div class="title-with-pill">
            <h2 class="header-title">Quản lý Đơn hàng &amp; Xuất Kho</h2>
            <span class="version-pill-badge">Order v4.2</span>
          </div>
          <p class="header-subtitle">
            Quản lý đơn bán hàng, hóa đơn PDF A4 &amp; biến động tồn kho
          </p>
        </div>
      </div>

      <div class="header-right-col">
        <el-dropdown trigger="click" @command="handleExportCommand">
          <button
            type="button"
            class="btn-header-outline"
            :disabled="exportingExcel || exportingCsv"
          >
            <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" />
              <polyline points="7 10 12 15 17 10" />
              <line x1="12" y1="15" x2="12" y2="3" />
            </svg>
            <span>{{ exportingExcel || exportingCsv ? 'Đang xuất...' : 'Xuất dữ liệu ▾' }}</span>
          </button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="excel">📊 Xuất Excel (.xls đa sheet)</el-dropdown-item>
              <el-dropdown-item command="csv">📄 Xuất CSV (UTF-8)</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>

        <button
          v-if="authStore.isAdmin"
          type="button"
          class="btn-header-emerald"
          @click="openStockAdjustModal"
        >
          <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M12 3v12m0 0l-4-4m4 4l4-4" />
            <path d="M3 17v2a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-2" />
          </svg>
          <span>Nhập / Kiểm kho</span>
        </button>

        <button
          type="button"
          class="btn-header-primary"
          @click="openOrderModal()"
        >
          <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2.5">
            <line x1="12" y1="5" x2="12" y2="19" />
            <line x1="5" y1="12" x2="19" y2="12" />
          </svg>
          <span>Tạo đơn hàng</span>
        </button>
      </div>
    </header>

    <!-- 4 THẺ KPI ĐƠN HÀNG & DOANH THU -->
    <section class="orders-metrics-grid">
      <div class="kpi-stat-card">
        <div class="kpi-stat-content">
          <span class="kpi-stat-label">Tổng đơn hàng</span>
          <span class="kpi-stat-value">{{ stats.totalOrders }}</span>
          <span class="kpi-stat-sub text-muted">{{ stats.todayOrders }} đơn hôm nay</span>
        </div>
        <div class="kpi-stat-icon icon-blue">
          <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="9" cy="21" r="1" />
            <circle cx="20" cy="21" r="1" />
            <path d="M1 1h4l2.68 13.39a2 2 0 0 0 2 1.61h9.72a2 2 0 0 0 2-1.61L23 6H6" />
          </svg>
        </div>
      </div>

      <div class="kpi-stat-card">
        <div class="kpi-stat-content">
          <span class="kpi-stat-label">Chờ xử lý &amp; Đóng gói</span>
          <span class="kpi-stat-value">{{ stats.pendingFulfillmentCount }}</span>
          <span class="kpi-stat-sub text-muted">
            {{ stats.pendingOrders }} chờ duyệt • {{ stats.confirmedOrders }} đã xác nhận
          </span>
        </div>
        <div class="kpi-stat-icon icon-amber">
          <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="10" />
            <polyline points="12 6 12 12 16 14" />
          </svg>
        </div>
      </div>

      <div class="kpi-stat-card">
        <div class="kpi-stat-content">
          <span class="kpi-stat-label">Đơn hoàn thành</span>
          <span class="kpi-stat-value">{{ stats.completedOrders }}</span>
          <span class="kpi-stat-sub text-muted">
            Hoàn tất {{ stats.completionRate }}% • {{ stats.shippingOrders }} đang giao
          </span>
        </div>
        <div class="kpi-stat-icon icon-teal">
          <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14" />
            <polyline points="22 4 12 14.01 9 11.01" />
          </svg>
        </div>
      </div>

      <div class="kpi-stat-card">
        <div class="kpi-stat-content">
          <span class="kpi-stat-label">Doanh thu hiệu lực</span>
          <span class="kpi-stat-value text-indigo">{{ formatCompactRevenue(stats.activeRevenue) }}</span>
          <span class="kpi-stat-sub text-muted">
            Đã thu {{ formatCurrency(stats.paidRevenue) }}
          </span>
        </div>
        <div class="kpi-stat-icon icon-indigo">
          <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2">
            <rect x="2" y="5" width="20" height="14" rx="2" />
            <line x1="2" y1="10" x2="22" y2="10" />
          </svg>
        </div>
      </div>
    </section>

    <!-- CHUYỂN TAB: DANH SÁCH ĐƠN HÀNG / NHẬT KÝ KHO -->
    <div class="module-tabs-bar">
      <button
        type="button"
        class="module-tab-btn"
        :class="{ active: activeTab === 'orders' }"
        @click="switchTab('orders')"
      >
        <span>Danh sách Đơn hàng</span>
        <span class="tab-count-pill">{{ orderTotalElements }}</span>
      </button>

      <button
        type="button"
        class="module-tab-btn"
        :class="{ active: activeTab === 'inventory' }"
        @click="switchTab('inventory')"
      >
        <span>Lịch sử Xuất - Nhập Kho</span>
        <span class="tab-count-pill">{{ logTotalElements }}</span>
      </button>
    </div>

    <!-- ========================================================== -->
    <!-- TAB 1: DANH SÁCH ĐƠN HÀNG                                   -->
    <!-- ========================================================== -->
    <template v-if="activeTab === 'orders'">
      <!-- FILTER TOOLBAR -->
      <section class="filter-toolbar-card">
        <div class="filter-controls-row">
          <div class="search-input-wrap">
            <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="#94a3b8" stroke-width="2">
              <circle cx="11" cy="11" r="8" />
              <line x1="21" y1="21" x2="16.65" y2="16.65" />
            </svg>
            <input
              v-model="orderFilter.keyword"
              type="text"
              class="custom-search-input"
              placeholder="Tìm mã đơn ORD-..., tên khách hàng, SĐT..."
              @input="handleOrderSearchDebounce"
              @keyup.enter="fetchOrders"
            />
          </div>

          <el-select
            v-model="orderFilter.status"
            placeholder="Trạng thái đơn"
            clearable
            class="filter-select"
            @change="onOrderFilterChange"
          >
            <el-option label="Tất cả trạng thái" value="" />
            <el-option label="Chờ xác nhận" value="PENDING" />
            <el-option label="Đã xác nhận" value="CONFIRMED" />
            <el-option label="Đang giao hàng" value="SHIPPING" />
            <el-option label="Hoàn thành" value="COMPLETED" />
            <el-option label="Đã hủy" value="CANCELLED" />
          </el-select>

          <el-select
            v-model="orderFilter.paymentStatus"
            placeholder="Thanh toán"
            clearable
            class="filter-select"
            @change="onOrderFilterChange"
          >
            <el-option label="Mọi thanh toán" value="" />
            <el-option label="Chưa thanh toán" value="UNPAID" />
            <el-option label="Đã thanh toán" value="PAID" />
            <el-option label="Đã hoàn tiền" value="REFUNDED" />
          </el-select>

          <button type="button" class="btn-reset-filter" @click="resetOrderFilters">
            <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2">
              <polyline points="23 4 23 10 17 10" />
              <path d="M20.49 15a9 9 0 1 1-2.12-9.36L23 10" />
            </svg>
            <span>Đặt lại</span>
          </button>
        </div>

        <div class="filter-summary-right">
          Hiển thị: <strong>{{ orders.length }}</strong> / <strong>{{ orderTotalElements }}</strong> đơn
        </div>
      </section>

      <!-- DATA CARD: TABLE DESKTOP + CARDS MOBILE -->
      <section class="data-card">
        <div v-if="loadingOrders" class="loading-box">
          <el-skeleton :rows="5" animated />
        </div>

        <div v-else-if="orders.length === 0" class="empty-state-box">
          <div class="empty-icon-circle">🧾</div>
          <h4>Chưa có đơn hàng nào phù hợp</h4>
          <p>Nhấn nút <strong>"Tạo đơn hàng"</strong> để lập phiếu bán hàng &amp; xuất kho tự động đầu tiên.</p>
          <button type="button" class="btn-header-primary" @click="openOrderModal()">
            + Tạo đơn hàng mới
          </button>
        </div>

        <template v-else>
          <!-- DESKTOP TABLE -->
          <div class="desktop-table-wrap table-responsive-scroll">
            <table class="erp-table">
              <thead>
                <tr>
                  <th>Mã đơn</th>
                  <th>Khách hàng</th>
                  <th class="text-center">SL hàng</th>
                  <th class="text-right">Tổng tiền</th>
                  <th class="text-center">Thanh toán</th>
                  <th class="text-center">Trạng thái</th>
                  <th>Ngày tạo</th>
                  <th class="text-right">Thao tác</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="order in orders" :key="order.id">
                  <td>
                    <button type="button" class="order-code-link" @click="openDetailModal(order)">
                      {{ order.orderCode }}
                    </button>
                    <div class="sub-muted-line">
                      @{{ order.createdByUsername || 'system' }}
                    </div>
                  </td>

                  <td>
                    <div class="customer-name-cell" :title="order.customerName">{{ order.customerName }}</div>
                    <div class="sub-muted-line">{{ order.customerPhone || 'Chưa có SĐT' }}</div>
                  </td>

                  <td class="text-center">
                    <span class="qty-plain">{{ order.totalQuantity }} sp</span>
                  </td>

                  <td class="text-right">
                    <div class="price-cell">{{ formatCurrency(order.totalAmount) }}</div>
                    <div class="sub-muted-line">{{ formatPaymentShort(order.paymentMethod) }}</div>
                  </td>

                  <td class="text-center">
                    <span class="status-badge" :class="getPaymentBadgeClass(order.paymentStatus)">
                      {{ order.paymentStatusLabel }}
                    </span>
                  </td>

                  <td class="text-center">
                    <span class="status-badge" :class="getOrderBadgeClass(order.status)">
                      <span class="badge-dot"></span>
                      {{ order.statusLabel }}
                    </span>
                  </td>

                  <td>
                    <div class="date-cell">{{ formatDateTime(order.createdAt) }}</div>
                  </td>

                  <td class="text-right">
                    <div class="row-actions-group">
                      <button
                        type="button"
                        class="btn-row-action primary"
                        title="Xem chi tiết &amp; chuyển trạng thái"
                        @click="openDetailModal(order)"
                      >
                        Chi tiết
                      </button>

                      <button
                        type="button"
                        class="btn-row-action"
                        title="In / Xuất Hóa đơn PDF A4"
                        @click="printOrderInvoice(order)"
                      >
                        PDF
                      </button>

                      <el-dropdown
                        v-if="order.status === 'PENDING' || (authStore.isAdmin && order.status === 'CANCELLED')"
                        trigger="click"
                        @command="(cmd) => handleOrderRowCommand(cmd, order)"
                      >
                        <button type="button" class="btn-row-more" title="Thao tác khác">⋯</button>
                        <template #dropdown>
                          <el-dropdown-menu>
                            <el-dropdown-item v-if="order.status === 'PENDING'" command="edit">
                              ✏️ Sửa đơn hàng
                            </el-dropdown-item>
                            <el-dropdown-item
                              v-if="authStore.isAdmin && (order.status === 'PENDING' || order.status === 'CANCELLED')"
                              command="delete"
                              divided
                            >
                              <span style="color: #dc2626;">🗑️ Xóa đơn</span>
                            </el-dropdown-item>
                          </el-dropdown-menu>
                        </template>
                      </el-dropdown>
                    </div>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>

          <!-- MOBILE ORDER CARDS -->
          <div class="mobile-cards-list">
            <div v-for="order in orders" :key="'m-' + order.id" class="mobile-order-card">
              <div class="m-card-top">
                <div>
                  <button type="button" class="order-code-link" @click="openDetailModal(order)">
                    {{ order.orderCode }}
                  </button>
                  <div class="sub-muted-line">{{ formatDateTime(order.createdAt) }}</div>
                </div>
                <span class="status-badge" :class="getOrderBadgeClass(order.status)">
                  <span class="badge-dot"></span>
                  {{ order.statusLabel }}
                </span>
              </div>

              <div class="m-card-body">
                <div class="m-customer-row">
                  <strong>{{ order.customerName }}</strong>
                  <span class="sub-muted-line">{{ order.customerPhone || '—' }}</span>
                </div>
                <div class="m-price-row">
                  <span class="qty-chip">{{ order.totalQuantity }} sản phẩm</span>
                  <span class="price-cell">{{ formatCurrency(order.totalAmount) }}</span>
                </div>
                <div class="m-pay-row">
                  <span class="sub-muted-line">{{ order.paymentMethodLabel }}</span>
                  <span class="status-badge" :class="getPaymentBadgeClass(order.paymentStatus)">
                    {{ order.paymentStatusLabel }}
                  </span>
                </div>
              </div>

              <div class="m-card-actions">
                <button type="button" class="btn-row-action primary" @click="openDetailModal(order)">
                  Chi tiết / Xử lý
                </button>
                <button type="button" class="btn-row-action pdf" @click="printOrderInvoice(order)">
                  Xuất PDF
                </button>
                <button
                  v-if="order.status === 'PENDING'"
                  type="button"
                  class="btn-row-action"
                  @click="openOrderModal(order)"
                >
                  Sửa
                </button>
              </div>
            </div>
          </div>

          <!-- PAGINATION -->
          <div class="table-pagination-bar">
            <div class="pagination-info">
              Trang {{ orderPage + 1 }} / {{ Math.max(1, orderTotalPages) }} • Tổng {{ orderTotalElements }} đơn hàng
            </div>
            <el-pagination
              v-model:current-page="orderCurrentPageUi"
              v-model:page-size="orderPageSize"
              :page-sizes="[10, 15, 25, 50]"
              layout="sizes, prev, pager, next"
              small
              :total="orderTotalElements"
              @current-change="fetchOrders"
              @size-change="onOrderFilterChange"
            />
          </div>
        </template>
      </section>
    </template>

    <!-- ========================================================== -->
    <!-- TAB 2: LỊCH SỬ XUẤT - NHẬP KHO (INVENTORY LOGS)            -->
    <!-- ========================================================== -->
    <template v-else>
      <section class="filter-toolbar-card">
        <div class="filter-controls-row">
          <div class="search-input-wrap">
            <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="#94a3b8" stroke-width="2">
              <circle cx="11" cy="11" r="8" />
              <line x1="21" y1="21" x2="16.65" y2="16.65" />
            </svg>
            <input
              v-model="logFilter.keyword"
              type="text"
              class="custom-search-input"
              placeholder="Tìm mã SKU, tên sản phẩm, mã chứng từ..."
              @input="handleLogSearchDebounce"
              @keyup.enter="fetchInventoryLogs"
            />
          </div>

          <el-select
            v-model="logFilter.movementType"
            placeholder="Loại biến động"
            clearable
            class="filter-select"
            @change="onLogFilterChange"
          >
            <el-option label="Tất cả biến động" value="" />
            <el-option label="Xuất kho đơn hàng" value="EXPORT_ORDER" />
            <el-option label="Hoàn kho hủy/sửa đơn" value="IMPORT_CANCEL" />
            <el-option label="Điều chỉnh tồn kho" value="MANUAL_ADJUST" />
          </el-select>

          <el-date-picker
            v-model="logFilter.dateRange"
            type="daterange"
            value-format="YYYY-MM-DD"
            format="DD/MM/YYYY"
            range-separator="→"
            start-placeholder="Từ ngày"
            end-placeholder="Đến ngày"
            :shortcuts="logDateShortcuts"
            clearable
            class="log-date-picker"
            @change="onLogFilterChange"
          />

          <button type="button" class="btn-reset-filter" title="Khôi phục bộ lọc tháng hiện tại" @click="resetLogFilters">
            <span>Tháng này</span>
          </button>
        </div>

        <div class="filter-summary-right">
          Tổng nhật ký: <strong>{{ logTotalElements }}</strong> bản ghi
        </div>
      </section>

      <section class="data-card">
        <div v-if="loadingLogs" class="loading-box">
          <el-skeleton :rows="5" animated />
        </div>

        <div v-else-if="inventoryLogs.length === 0" class="empty-state-box">
          <div class="empty-icon-circle">📦</div>
          <h4>Chưa có biến động xuất/nhập kho</h4>
          <p>Mỗi khi bạn tạo đơn, hủy đơn hoặc kiểm kê kho, hệ thống tự động ghi vết audit tại đây.</p>
        </div>

        <template v-else>
          <div class="desktop-table-wrap table-responsive-scroll">
            <table class="erp-table">
              <thead>
                <tr>
                  <th>Thời gian</th>
                  <th>Sản phẩm &amp; SKU</th>
                  <th>Loại biến động</th>
                  <th class="text-center">Thay đổi</th>
                  <th class="text-center">Tồn trước → Sau</th>
                  <th>Chứng từ</th>
                  <th>Ghi chú &amp; Người thao tác</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="log in inventoryLogs" :key="log.id">
                  <td class="date-cell">{{ formatDateTime(log.createdAt) }}</td>
                  <td>
                    <div class="customer-name-cell">{{ log.productName }}</div>
                    <span class="sku-pill">{{ log.productSku }}</span>
                  </td>
                  <td>
                    <span class="status-badge" :class="getMovementBadgeClass(log.movementType)">
                      {{ log.movementTypeLabel }}
                    </span>
                  </td>
                  <td class="text-center">
                    <span
                      class="delta-pill"
                      :class="log.quantityDelta >= 0 ? 'delta-in' : 'delta-out'"
                    >
                      {{ log.quantityDelta >= 0 ? '+' + log.quantityDelta : log.quantityDelta }}
                    </span>
                  </td>
                  <td class="text-center">
                    <span class="stock-flow-text">
                      {{ log.stockBefore }} → <strong>{{ log.stockAfter }}</strong>
                    </span>
                  </td>
                  <td>
                    <span class="ref-code-chip">{{ log.referenceCode || '—' }}</span>
                  </td>
                  <td>
                    <div class="note-cell">{{ log.note || '—' }}</div>
                    <div class="sub-muted-line">bởi @{{ log.actorUsername || 'system' }}</div>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>

          <div class="mobile-cards-list">
            <div v-for="log in inventoryLogs" :key="'mlog-' + log.id" class="mobile-order-card">
              <div class="m-card-top">
                <div>
                  <strong>{{ log.productName }}</strong>
                  <div class="sub-muted-line">{{ log.productSku }} • {{ formatDateTime(log.createdAt) }}</div>
                </div>
                <span class="delta-pill" :class="log.quantityDelta >= 0 ? 'delta-in' : 'delta-out'">
                  {{ log.quantityDelta >= 0 ? '+' + log.quantityDelta : log.quantityDelta }}
                </span>
              </div>
              <div class="m-card-body">
                <div class="m-pay-row">
                  <span class="status-badge" :class="getMovementBadgeClass(log.movementType)">
                    {{ log.movementTypeLabel }}
                  </span>
                  <span class="stock-flow-text">
                    Tồn: {{ log.stockBefore }} → <strong>{{ log.stockAfter }}</strong>
                  </span>
                </div>
                <div class="sub-muted-line">Chứng từ: {{ log.referenceCode || '—' }} • {{ log.note }}</div>
              </div>
            </div>
          </div>

          <div class="table-pagination-bar">
            <div class="pagination-info">
              Trang {{ logPage + 1 }} / {{ Math.max(1, logTotalPages) }} • {{ logTotalElements }} bản ghi
            </div>
            <el-pagination
              v-model:current-page="logCurrentPageUi"
              v-model:page-size="logPageSize"
              :page-sizes="[10, 15, 25, 50]"
              layout="sizes, prev, pager, next"
              small
              :total="logTotalElements"
              @current-change="fetchInventoryLogs"
              @size-change="onLogFilterChange"
            />
          </div>
        </template>
      </section>
    </template>

    <!-- ========================================================== -->
    <!-- MODAL 1: TẠO / SỬA ĐƠN HÀNG                                 -->
    <!-- ========================================================== -->
    <div v-if="orderModalVisible" class="custom-modal-overlay" @click.self="closeOrderModal">
      <div class="custom-modal-card order-builder-modal">
        <div class="modal-head">
          <div>
            <div class="modal-title-row">
              <h3>{{ editingOrderId ? 'Cập nhật Đơn hàng' : 'Tạo Đơn hàng Mới' }}</h3>
              <span class="modal-pill">{{ editingOrderId ? editingOrderCode : 'TỰ ĐỘNG TRỪ KHO' }}</span>
            </div>
            <p class="modal-subtitle">
              Chọn sản phẩm từ kho thực tế, áp dụng chiết khấu/VAT và tự động giữ tồn kho an toàn
            </p>
          </div>
          <div class="modal-head-actions">
            <button
              v-if="!editingOrderId"
              type="button"
              class="btn-quick-fill"
              @click="fillSampleOrder"
            >
              ⚡ Điền mẫu nhanh
            </button>
            <button type="button" class="btn-modal-close" @click="closeOrderModal">✕</button>
          </div>
        </div>

        <div class="modal-body-scroll">
          <!-- KHÁCH HÀNG & THANH TOÁN -->
          <div class="form-section-box">
            <div class="section-box-title">1. Thông tin Khách hàng &amp; Thanh toán</div>
            <div class="form-grid-2">
              <div class="field-group">
                <label class="field-label">Tên khách hàng / Doanh nghiệp <span class="req">*</span></label>
                <input
                  v-model="orderForm.customerName"
                  type="text"
                  class="erp-input"
                  placeholder="VD: Công ty TNHH Công nghệ Sao Việt"
                />
              </div>
              <div class="field-group">
                <label class="field-label">Số điện thoại</label>
                <input
                  v-model="orderForm.customerPhone"
                  type="text"
                  class="erp-input"
                  placeholder="0912 345 678"
                />
              </div>
              <div class="field-group">
                <label class="field-label">Email nhận hóa đơn</label>
                <input
                  v-model="orderForm.customerEmail"
                  type="email"
                  class="erp-input"
                  placeholder="ketoan@company.vn"
                />
              </div>
              <div class="field-group">
                <label class="field-label">Địa chỉ giao hàng</label>
                <input
                  v-model="orderForm.shippingAddress"
                  type="text"
                  class="erp-input"
                  placeholder="Số nhà, đường, quận/huyện, tỉnh/thành..."
                />
              </div>
              <div class="field-group">
                <label class="field-label">Phương thức thanh toán</label>
                <el-select v-model="orderForm.paymentMethod" style="width: 100%">
                  <el-option label="Tiền mặt / COD" value="CASH" />
                  <el-option label="Chuyển khoản ngân hàng" value="BANK_TRANSFER" />
                  <el-option label="Công nợ doanh nghiệp" value="CREDIT" />
                </el-select>
              </div>
              <div class="field-group">
                <label class="field-label">Trạng thái thanh toán</label>
                <el-select v-model="orderForm.paymentStatus" style="width: 100%">
                  <el-option label="Chưa thanh toán" value="UNPAID" />
                  <el-option label="Đã thanh toán" value="PAID" />
                </el-select>
              </div>
            </div>
          </div>

          <!-- CHỌN SẢN PHẨM TỪ KHO -->
          <div class="form-section-box">
            <div class="section-box-title">2. Chọn Sản phẩm Xuất Kho</div>
            <div class="product-picker-row">
              <el-select
                v-model="selectedProductToAdd"
                filterable
                placeholder="Tìm theo tên hoặc mã SKU để thêm vào đơn..."
                class="product-picker-select"
                @change="addProductToOrder"
              >
                <el-option
                  v-for="prod in availableProducts"
                  :key="prod.id"
                  :label="`${prod.name} (${prod.sku}) — Tồn: ${prod.stock} — ${formatCurrency(prod.price)}`"
                  :value="prod.id"
                  :disabled="!editingOrderId && prod.stock <= 0"
                />
              </el-select>
            </div>

            <div v-if="orderForm.items.length === 0" class="empty-items-hint">
              Chưa chọn sản phẩm nào. Hãy chọn ít nhất 1 mặt hàng ở thanh tìm kiếm phía trên.
            </div>

            <div v-else class="order-items-table-wrap">
              <table class="mini-items-table">
                <thead>
                  <tr>
                    <th>Sản phẩm</th>
                    <th class="text-right">Đơn giá</th>
                    <th class="text-center">Số lượng</th>
                    <th class="text-right">Thành tiền</th>
                    <th class="text-center"></th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="(item, idx) in orderForm.items" :key="item.productId">
                    <td>
                      <div class="item-prod-name">{{ item.productName }}</div>
                      <div class="sub-muted-line">
                        {{ item.productSku }} • Tồn kho khả dụng: <strong>{{ item.maxStock }}</strong>
                      </div>
                    </td>
                    <td class="text-right">{{ formatCurrency(item.unitPrice) }}</td>
                    <td class="text-center">
                      <div class="qty-stepper-inline">
                        <button type="button" @click="stepItemQty(idx, -1)">−</button>
                        <input
                          v-model.number="item.quantity"
                          type="number"
                          min="1"
                          :max="item.maxStock"
                        />
                        <button type="button" @click="stepItemQty(idx, 1)">+</button>
                      </div>
                    </td>
                    <td class="text-right price-cell">
                      {{ formatCurrency((item.unitPrice ?? 0) * (item.quantity ?? 0)) }}
                    </td>
                    <td class="text-center">
                      <button type="button" class="btn-remove-item" @click="removeOrderItem(idx)">✕</button>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>

          <!-- CHIẾT KHẤU, VAT & TỔNG TIỀN -->
          <div class="form-section-box summary-section-box">
            <div class="summary-left-inputs">
              <div class="field-group">
                <label class="field-label">Giảm giá trực tiếp (VNĐ)</label>
                <input
                  v-model.number="orderForm.discountAmount"
                  type="number"
                  min="0"
                  step="1000"
                  class="erp-input"
                />
              </div>
              <div class="field-group">
                <label class="field-label">Thuế VAT (%)</label>
                <el-select v-model="orderForm.taxRate" style="width: 100%">
                  <el-option label="0% (Không chịu thuế)" :value="0" />
                  <el-option label="8% (Thuế GTGT giảm)" :value="8" />
                  <el-option label="10% (Thuế GTGT chuẩn)" :value="10" />
                </el-select>
              </div>
              <div class="field-group full-col">
                <label class="field-label">Ghi chú đơn hàng / Giao nhận</label>
                <textarea
                  v-model="orderForm.note"
                  rows="2"
                  class="erp-textarea"
                  placeholder="Yêu cầu xuất hóa đơn đỏ, giao giờ hành chính..."
                ></textarea>
              </div>
            </div>

            <div class="summary-right-totals">
              <div class="total-line">
                <span>Tạm tính:</span>
                <strong>{{ formatCurrency(formSubtotal) }}</strong>
              </div>
              <div class="total-line">
                <span>Giảm giá:</span>
                <span class="text-red">− {{ formatCurrency(effectiveDiscount) }}</span>
              </div>
              <div class="total-line">
                <span>Thuế VAT ({{ orderForm.taxRate ?? 0 }}%):</span>
                <span>+ {{ formatCurrency(formTaxAmount) }}</span>
              </div>
              <div class="total-grand-line">
                <span>Tổng thanh toán:</span>
                <strong>{{ formatCurrency(formGrandTotal) }}</strong>
              </div>
              <div class="amount-words-hint">
                Bằng chữ: <em>{{ numberToVietnameseWords(Math.round(formGrandTotal)) }}</em>
              </div>
            </div>
          </div>
        </div>

        <div class="modal-foot">
          <span class="foot-hint">
            {{ editingOrderId ? 'Sửa đơn sẽ tự động tính lại tồn kho' : 'Tạo đơn sẽ giữ & trừ tồn kho ngay lập tức' }}
          </span>
          <div class="foot-buttons">
            <button type="button" class="btn-header-outline" @click="closeOrderModal">Hủy bỏ</button>
            <button
              type="button"
              class="btn-header-primary"
              :disabled="savingOrder"
              @click="submitOrderForm"
            >
              {{ savingOrder ? 'Đang xử lý...' : (editingOrderId ? 'Lưu cập nhật đơn' : 'Xác nhận & Tạo đơn') }}
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- ========================================================== -->
    <!-- MODAL 2: CHI TIẾT ĐƠN HÀNG & TIMELINE & IN PDF A4          -->
    <!-- ========================================================== -->
    <div v-if="detailModalVisible && activeOrder" class="custom-modal-overlay" @click.self="detailModalVisible = false">
      <div class="custom-modal-card detail-modal">
        <div class="modal-head">
          <div>
            <div class="modal-title-row">
              <h3>Đơn hàng {{ activeOrder.orderCode }}</h3>
              <span class="status-badge" :class="getOrderBadgeClass(activeOrder.status)">
                <span class="badge-dot"></span>
                {{ activeOrder.statusLabel }}
              </span>
              <span class="status-badge" :class="getPaymentBadgeClass(activeOrder.paymentStatus)">
                {{ activeOrder.paymentStatusLabel }}
              </span>
            </div>
            <p class="modal-subtitle">
              Khởi tạo lúc {{ formatDateTime(activeOrder.createdAt) }} • Người tạo: @{{ activeOrder.createdByUsername || 'system' }}
            </p>
          </div>
          <div class="modal-head-actions">
            <button type="button" class="btn-header-outline" @click="printOrderInvoice(activeOrder)">
              🖨️ Xuất Hóa đơn PDF (A4)
            </button>
            <button type="button" class="btn-modal-close" @click="detailModalVisible = false">✕</button>
          </div>
        </div>

        <div class="modal-body-scroll detail-body-clean">
          <!-- 1. TIMELINE TIẾN TRÌNH (KHÔNG ĐÓNG KHUNG RỐI MẮT) -->
          <div class="clean-stepper" v-if="activeOrder.status !== 'CANCELLED'">
            <div
              v-for="(step, idx) in ORDER_STEPS"
              :key="step.key"
              class="clean-step"
              :class="{ done: isStepReached(activeOrder.status, step.key), current: activeOrder.status === step.key }"
            >
              <div class="clean-step-node">
                <span class="clean-step-dot">{{ isStepReached(activeOrder.status, step.key) ? '✓' : idx + 1 }}</span>
                <span class="clean-step-line" v-if="idx < ORDER_STEPS.length - 1"></span>
              </div>
              <span class="clean-step-label">{{ step.label }}</span>
            </div>
          </div>
          <div v-else class="cancelled-banner">
            Đơn hàng đã hủy lúc {{ formatDateTime(activeOrder.cancelledAt) }} — Tồn kho đã được hoàn lại tự động.
          </div>

          <!-- 2. THÔNG TIN KHÁCH & THANH TOÁN (PHẲNG, TINH GỌN, KHÔNG LẶP DỮ LIỆU) -->
          <div class="clean-meta-grid">
            <div class="clean-meta-col">
              <div class="clean-section-kicker">Khách hàng</div>
              <div class="clean-customer-title">{{ activeOrder.customerName }}</div>
              <div class="clean-meta-line" v-if="activeOrder.customerPhone || activeOrder.customerEmail">
                {{ [activeOrder.customerPhone, activeOrder.customerEmail].filter(Boolean).join(' • ') }}
              </div>
              <div class="clean-meta-line">{{ activeOrder.shippingAddress || 'Nhận tại kho' }}</div>
            </div>

            <div class="clean-meta-col">
              <div class="clean-section-kicker">Thanh toán &amp; Chứng từ</div>
              <div class="clean-meta-kv">
                <span>Phương thức</span>
                <strong>{{ activeOrder.paymentMethodLabel }}</strong>
              </div>
              <div class="clean-meta-kv" v-if="activeOrder.note">
                <span>Ghi chú</span>
                <span>{{ activeOrder.note }}</span>
              </div>
            </div>
          </div>

          <!-- 3. BẢNG HÀNG HÓA (GỘP SKU DƯỚI TÊN, ĐƯỜNG KẺ MẢNH) -->
          <div class="clean-items-wrap">
            <table class="clean-items-table">
              <thead>
                <tr>
                  <th>Sản phẩm</th>
                  <th class="text-right">Đơn giá</th>
                  <th class="text-center">SL</th>
                  <th class="text-right">Thành tiền</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="item in activeOrder.items" :key="item.id">
                  <td>
                    <div class="item-prod-name">{{ item.productName }}</div>
                    <div class="clean-item-sku">{{ item.productSku }}</div>
                  </td>
                  <td class="text-right muted-num">{{ formatCurrency(item.unitPrice) }}</td>
                  <td class="text-center"><strong>{{ item.quantity }}</strong></td>
                  <td class="text-right price-cell">{{ formatCurrency(item.lineTotal) }}</td>
                </tr>
              </tbody>
            </table>
          </div>

          <!-- 4. TỔNG THANH TOÁN GỌN BÊN PHẢI + BẰNG CHỮ -->
          <div class="clean-receipt-footer">
            <div class="clean-receipt-words">
              <span>Bằng chữ:</span>
              <em>{{ numberToVietnameseWords(Math.round(activeOrder.totalAmount ?? 0)) }}</em>
            </div>

            <div class="clean-receipt-totals">
              <div class="total-line">
                <span>Tạm tính</span>
                <span>{{ formatCurrency(activeOrder.subtotal) }}</span>
              </div>
              <div class="total-line" v-if="(activeOrder.discountAmount ?? 0) > 0">
                <span>Giảm giá</span>
                <span class="text-red">− {{ formatCurrency(activeOrder.discountAmount) }}</span>
              </div>
              <div class="total-line">
                <span>Thuế VAT ({{ activeOrder.taxRate ?? 0 }}%)</span>
                <span>+ {{ formatCurrency(activeOrder.taxAmount) }}</span>
              </div>
              <div class="total-grand-line">
                <span>Tổng cộng</span>
                <strong>{{ formatCurrency(activeOrder.totalAmount) }}</strong>
              </div>
            </div>
          </div>
        </div>

        <div class="modal-foot detail-foot-clean">
          <div class="detail-foot-left">
            <button
              v-if="activeOrder.status === 'PENDING' || activeOrder.status === 'CONFIRMED' || activeOrder.status === 'SHIPPING'"
              type="button"
              class="btn-link-danger"
              :disabled="updatingStatus"
              @click="confirmCancelOrder(activeOrder)"
            >
              Hủy đơn &amp; Hoàn kho
            </button>

            <button
              v-if="activeOrder.status !== 'CANCELLED' && activeOrder.paymentStatus === 'UNPAID'"
              type="button"
              class="btn-header-outline"
              :disabled="updatingStatus"
              @click="markOrderPaid(activeOrder)"
            >
              Đánh dấu Đã thanh toán
            </button>
          </div>

          <div class="detail-foot-right">
            <button type="button" class="btn-header-outline" @click="detailModalVisible = false">
              Đóng
            </button>

            <button
              v-if="activeOrder.status === 'PENDING'"
              type="button"
              class="btn-header-primary"
              :disabled="updatingStatus"
              @click="changeOrderStatus(activeOrder, 'CONFIRMED')"
            >
              ✓ Xác nhận &amp; Đóng gói
            </button>

            <button
              v-if="activeOrder.status === 'CONFIRMED'"
              type="button"
              class="btn-status-step ship"
              :disabled="updatingStatus"
              @click="changeOrderStatus(activeOrder, 'SHIPPING')"
            >
              Bàn giao vận chuyển
            </button>

            <button
              v-if="activeOrder.status === 'CONFIRMED' || activeOrder.status === 'SHIPPING'"
              type="button"
              class="btn-status-step complete"
              :disabled="updatingStatus"
              @click="changeOrderStatus(activeOrder, 'COMPLETED')"
            >
              ✓ Hoàn thành đơn
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- ========================================================== -->
    <!-- MODAL 3: NHẬP / ĐIỀU CHỈNH TỒN KHO (ADMIN)                 -->
    <!-- ========================================================== -->
    <div v-if="adjustModalVisible" class="custom-modal-overlay" @click.self="adjustModalVisible = false">
      <div class="custom-modal-card adjust-modal">
        <div class="modal-head">
          <div>
            <div class="modal-title-row">
              <h3>Nhập / Kiểm kê Điều chỉnh Kho</h3>
              <span class="modal-pill">AUDIT LOG</span>
            </div>
            <p class="modal-subtitle">Nhập thêm hàng mới về kho hoặc điều chỉnh chênh lệch kiểm kê thực tế</p>
          </div>
          <button type="button" class="btn-modal-close" @click="adjustModalVisible = false">✕</button>
        </div>

        <div class="modal-body-scroll">
          <div class="field-group">
            <label class="field-label">Chọn sản phẩm trong kho <span class="req">*</span></label>
            <el-select v-model="adjustForm.productId" filterable placeholder="Chọn sản phẩm..." style="width: 100%">
              <el-option
                v-for="prod in availableProducts"
                :key="prod.id"
                :label="`${prod.name} (${prod.sku}) — Tồn hiện tại: ${prod.stock}`"
                :value="prod.id"
              />
            </el-select>
          </div>

          <div class="field-group" style="margin-top: 14px;">
            <label class="field-label">
              Số lượng thay đổi <span class="req">*</span>
              <small>(Số dương: Nhập kho; Số âm: Xuất hao hụt/kiểm kê)</small>
            </label>
            <input
              v-model.number="adjustForm.quantityDelta"
              type="number"
              class="erp-input"
              placeholder="VD: 20 hoặc -5"
            />
          </div>

          <div class="field-group" style="margin-top: 14px;">
            <label class="field-label">Lý do / Số phiếu nhập</label>
            <textarea
              v-model="adjustForm.note"
              rows="3"
              class="erp-textarea"
              placeholder="VD: Nhập lô hàng nhà cung cấp tháng 11 / Kiểm kê kho định kỳ..."
            ></textarea>
          </div>
        </div>

        <div class="modal-foot">
          <span></span>
          <div class="foot-buttons">
            <button type="button" class="btn-header-outline" @click="adjustModalVisible = false">Hủy</button>
            <button
              type="button"
              class="btn-header-primary"
              :disabled="savingAdjust"
              @click="submitStockAdjust"
            >
              {{ savingAdjust ? 'Đang lưu...' : 'Xác nhận điều chỉnh kho' }}
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onBeforeUnmount } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../api'
import { useAuthStore } from '../store/auth'

const authStore = useAuthStore()

// =========================================================================
// HELPERS FIRST (Tránh Temporal Dead Zone trước mọi computed/watch)
// =========================================================================
const ORDER_STEPS = [
  { key: 'PENDING', label: 'Chờ xác nhận' },
  { key: 'CONFIRMED', label: 'Đã xác nhận' },
  { key: 'SHIPPING', label: 'Đang giao' },
  { key: 'COMPLETED', label: 'Hoàn thành' }
]

const STEP_INDEX = {
  PENDING: 0,
  CONFIRMED: 1,
  SHIPPING: 2,
  COMPLETED: 3
}

function isStepReached(currentStatus, targetKey) {
  const cur = STEP_INDEX[currentStatus] ?? -1
  const tgt = STEP_INDEX[targetKey] ?? 99
  return cur >= tgt
}

function formatCurrency(val) {
  const num = Number(val ?? 0)
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(num)
}

function formatCompactRevenue(val) {
  const num = Number(val ?? 0)
  if (num >= 1_000_000_000) {
    return (num / 1_000_000_000).toFixed(2).replace(/\.00$/, '') + ' Tỷ'
  }
  if (num >= 1_000_000) {
    return (num / 1_000_000).toFixed(1).replace(/\.0$/, '') + ' Tr đ'
  }
  return formatCurrency(num)
}

function formatPaymentShort(method) {
  switch (method) {
    case 'BANK_TRANSFER': return 'Chuyển khoản'
    case 'CREDIT': return 'Công nợ'
    default: return 'Tiền mặt'
  }
}

function formatDateYmd(d) {
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

function getCurrentMonthDateRange() {
  const now = new Date()
  const firstDay = new Date(now.getFullYear(), now.getMonth(), 1)
  const lastDay = new Date(now.getFullYear(), now.getMonth() + 1, 0)
  return [formatDateYmd(firstDay), formatDateYmd(lastDay)]
}

const logDateShortcuts = [
  {
    text: 'Tháng này',
    value: () => {
      const now = new Date()
      return [new Date(now.getFullYear(), now.getMonth(), 1), new Date(now.getFullYear(), now.getMonth() + 1, 0)]
    }
  },
  {
    text: 'Hôm nay',
    value: () => {
      const now = new Date()
      return [now, now]
    }
  },
  {
    text: '7 ngày qua',
    value: () => {
      const end = new Date()
      const start = new Date()
      start.setDate(start.getDate() - 6)
      return [start, end]
    }
  },
  {
    text: 'Tháng trước',
    value: () => {
      const now = new Date()
      return [new Date(now.getFullYear(), now.getMonth() - 1, 1), new Date(now.getFullYear(), now.getMonth(), 0)]
    }
  }
]

function formatDateTime(val) {
  if (!val) return '—'
  const date = new Date(val)
  if (Number.isNaN(date.getTime())) return String(val)
  return date.toLocaleString('vi-VN', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  })
}

function truncateText(str, maxLen) {
  if (!str) return ''
  return str.length > maxLen ? str.slice(0, maxLen) + '...' : str
}

function getOrderBadgeClass(status) {
  switch (status) {
    case 'PENDING': return 'badge-amber'
    case 'CONFIRMED': return 'badge-blue'
    case 'SHIPPING': return 'badge-purple'
    case 'COMPLETED': return 'badge-green'
    case 'CANCELLED': return 'badge-red'
    default: return 'badge-gray'
  }
}

function getPaymentBadgeClass(paymentStatus) {
  switch (paymentStatus) {
    case 'PAID': return 'badge-green'
    case 'REFUNDED': return 'badge-purple'
    default: return 'badge-amber'
  }
}

function getMovementBadgeClass(type) {
  switch (type) {
    case 'EXPORT_ORDER': return 'badge-amber'
    case 'IMPORT_CANCEL': return 'badge-blue'
    case 'MANUAL_ADJUST': return 'badge-green'
    default: return 'badge-gray'
  }
}

function numberToVietnameseWords(num) {
  const n = Math.floor(Number(num ?? 0))
  if (!Number.isFinite(n) || n <= 0) return 'Không đồng'
  const digits = ['không', 'một', 'hai', 'ba', 'bốn', 'năm', 'sáu', 'bảy', 'tám', 'chín']
  const units = ['', 'nghìn', 'triệu', 'tỷ', 'nghìn tỷ']

  const readThree = (block, showZeroHundred) => {
    const tram = Math.floor(block / 100)
    const chuc = Math.floor((block % 100) / 10)
    const dv = block % 10
    let res = ''
    if (tram > 0 || showZeroHundred) {
      res += digits[tram] + ' trăm '
    }
    if (chuc > 1) {
      res += digits[chuc] + ' mươi '
      if (dv === 1) res += 'mốt '
      else if (dv === 5) res += 'lăm '
      else if (dv > 0) res += digits[dv] + ' '
    } else if (chuc === 1) {
      res += 'mười '
      if (dv === 5) res += 'lăm '
      else if (dv > 0) res += digits[dv] + ' '
    } else if (dv > 0) {
      if (tram > 0 || showZeroHundred) res += 'lẻ '
      res += digits[dv] + ' '
    }
    return res
  }

  let temp = n
  const groups = []
  while (temp > 0) {
    groups.push(temp % 1000)
    temp = Math.floor(temp / 1000)
  }

  let out = ''
  for (let i = groups.length - 1; i >= 0; i--) {
    const g = groups[i]
    if (g > 0) {
      out += readThree(g, i < groups.length - 1) + (units[i] ? units[i] + ' ' : '')
    }
  }
  const clean = out.trim().replace(/\s+/g, ' ')
  return clean.charAt(0).toUpperCase() + clean.slice(1) + ' đồng'
}

// =========================================================================
// STATE
// =========================================================================
const activeTab = ref('orders')

const stats = reactive({
  totalOrders: 0,
  pendingOrders: 0,
  confirmedOrders: 0,
  shippingOrders: 0,
  completedOrders: 0,
  cancelledOrders: 0,
  todayOrders: 0,
  pendingFulfillmentCount: 0,
  activeRevenue: 0,
  completedRevenue: 0,
  paidRevenue: 0,
  avgOrderValue: 0,
  completionRate: 0
})

// Orders list state
const loadingOrders = ref(false)
const orders = ref([])
const orderPage = ref(0)
const orderPageSize = ref(15)
const orderTotalElements = ref(0)
const orderTotalPages = ref(1)
const orderCurrentPageUi = computed({
  get: () => orderPage.value + 1,
  set: (v) => { orderPage.value = Math.max(0, v - 1) }
})

const orderFilter = reactive({
  keyword: '',
  status: '',
  paymentStatus: ''
})

// Inventory logs state
const loadingLogs = ref(false)
const inventoryLogs = ref([])
const logPage = ref(0)
const logPageSize = ref(15)
const logTotalElements = ref(0)
const logTotalPages = ref(1)
const logCurrentPageUi = computed({
  get: () => logPage.value + 1,
  set: (v) => { logPage.value = Math.max(0, v - 1) }
})

const logFilter = reactive({
  keyword: '',
  movementType: '',
  dateRange: getCurrentMonthDateRange()
})

// Products catalog for order builder & stock adjust
const availableProducts = ref([])
const selectedProductToAdd = ref(null)

// Order create/edit modal state
const orderModalVisible = ref(false)
const editingOrderId = ref(null)
const editingOrderCode = ref('')
const savingOrder = ref(false)

const orderForm = reactive({
  customerName: '',
  customerPhone: '',
  customerEmail: '',
  shippingAddress: '',
  note: '',
  paymentMethod: 'CASH',
  paymentStatus: 'UNPAID',
  discountAmount: 0,
  taxRate: 8,
  items: []
})

// Detail modal state
const detailModalVisible = ref(false)
const activeOrder = ref(null)
const updatingStatus = ref(false)

// Adjust stock modal state
const adjustModalVisible = ref(false)
const savingAdjust = ref(false)
const adjustForm = reactive({
  productId: null,
  quantityDelta: 10,
  note: ''
})

// Export loading flags
const exportingExcel = ref(false)
const exportingCsv = ref(false)

let orderSearchTimer = null
let logSearchTimer = null

// =========================================================================
// COMPUTED FOR ORDER FORM TOTALS
// =========================================================================
const formSubtotal = computed(() => {
  return orderForm.items.reduce((sum, item) => {
    const qty = Number(item.quantity ?? 0)
    const price = Number(item.unitPrice ?? 0)
    return sum + qty * price
  }, 0)
})

const effectiveDiscount = computed(() => {
  const raw = Math.max(0, Number(orderForm.discountAmount ?? 0))
  return Math.min(raw, formSubtotal.value)
})

const formTaxAmount = computed(() => {
  const taxable = Math.max(0, formSubtotal.value - effectiveDiscount.value)
  const rate = Math.max(0, Number(orderForm.taxRate ?? 0))
  return Math.round((taxable * rate) / 100)
})

const formGrandTotal = computed(() => {
  return Math.max(0, formSubtotal.value - effectiveDiscount.value + formTaxAmount.value)
})

// =========================================================================
// API LOADERS
// =========================================================================
async function fetchOrderStats() {
  try {
    const res = await api.get('/orders/stats')
    Object.assign(stats, res.data || {})
  } catch (err) {
    console.error('Lỗi tải thống kê đơn hàng:', err)
  }
}

async function fetchOrders() {
  loadingOrders.value = true
  try {
    const params = {
      page: orderPage.value,
      size: orderPageSize.value,
      sortBy: 'createdAt',
      sortDir: 'desc'
    }
    if (orderFilter.keyword?.trim()) params.keyword = orderFilter.keyword.trim()
    if (orderFilter.status) params.status = orderFilter.status
    if (orderFilter.paymentStatus) params.paymentStatus = orderFilter.paymentStatus

    const res = await api.get('/orders', { params })
    orders.value = res.data?.content || []
    orderTotalElements.value = res.data?.totalElements ?? 0
    orderTotalPages.value = res.data?.totalPages ?? 1
  } catch (err) {
    ElMessage.error(err.response?.data?.message || 'Không thể tải danh sách đơn hàng!')
  } finally {
    loadingOrders.value = false
  }
}

async function fetchInventoryLogs() {
  loadingLogs.value = true
  try {
    const params = {
      page: logPage.value,
      size: logPageSize.value
    }
    if (logFilter.keyword?.trim()) params.keyword = logFilter.keyword.trim()
    if (logFilter.movementType) params.movementType = logFilter.movementType
    if (Array.isArray(logFilter.dateRange) && logFilter.dateRange.length === 2) {
      if (logFilter.dateRange[0]) params.fromDate = `${logFilter.dateRange[0]}T00:00:00`
      if (logFilter.dateRange[1]) params.toDate = `${logFilter.dateRange[1]}T23:59:59`
    }

    const res = await api.get('/orders/inventory-logs', { params })
    inventoryLogs.value = res.data?.content || []
    logTotalElements.value = res.data?.totalElements ?? 0
    logTotalPages.value = res.data?.totalPages ?? 1
  } catch (err) {
    ElMessage.error(err.response?.data?.message || 'Không thể tải lịch sử xuất nhập kho!')
  } finally {
    loadingLogs.value = false
  }
}

async function fetchProductsCatalog() {
  try {
    const res = await api.get('/products', { params: { page: 0, size: 100, sortBy: 'name', sortDir: 'asc' } })
    availableProducts.value = res.data?.content || []
  } catch (err) {
    console.error('Không thể tải danh mục sản phẩm:', err)
  }
}

function handleOrderSearchDebounce() {
  clearTimeout(orderSearchTimer)
  orderSearchTimer = setTimeout(() => {
    orderPage.value = 0
    fetchOrders()
  }, 350)
}

function onOrderFilterChange() {
  orderPage.value = 0
  fetchOrders()
}

function resetOrderFilters() {
  orderFilter.keyword = ''
  orderFilter.status = ''
  orderFilter.paymentStatus = ''
  orderPage.value = 0
  fetchOrders()
}

function handleLogSearchDebounce() {
  clearTimeout(logSearchTimer)
  logSearchTimer = setTimeout(() => {
    logPage.value = 0
    fetchInventoryLogs()
  }, 350)
}

function onLogFilterChange() {
  logPage.value = 0
  fetchInventoryLogs()
}

function resetLogFilters() {
  logFilter.keyword = ''
  logFilter.movementType = ''
  logFilter.dateRange = getCurrentMonthDateRange()
  logPage.value = 0
  fetchInventoryLogs()
}

function switchTab(tab) {
  activeTab.value = tab
  if (tab === 'inventory') {
    fetchInventoryLogs()
  } else {
    fetchOrders()
  }
}

// =========================================================================
// ORDER CREATE / EDIT BUILDER
// =========================================================================
async function openOrderModal(existingOrder = null) {
  await fetchProductsCatalog()

  if (existingOrder) {
    editingOrderId.value = existingOrder.id
    editingOrderCode.value = existingOrder.orderCode
    orderForm.customerName = existingOrder.customerName || ''
    orderForm.customerPhone = existingOrder.customerPhone || ''
    orderForm.customerEmail = existingOrder.customerEmail || ''
    orderForm.shippingAddress = existingOrder.shippingAddress || ''
    orderForm.note = existingOrder.note || ''
    orderForm.paymentMethod = existingOrder.paymentMethod || 'CASH'
    orderForm.paymentStatus = existingOrder.paymentStatus || 'UNPAID'
    orderForm.discountAmount = Number(existingOrder.discountAmount ?? 0)
    orderForm.taxRate = Number(existingOrder.taxRate ?? 0)
    orderForm.items = (existingOrder.items || []).map((item) => {
      const catalogProd = availableProducts.value.find((p) => p.id === item.productId)
      const currentCatalogStock = catalogProd ? Number(catalogProd.stock ?? 0) : 0
      return {
        productId: item.productId,
        productName: item.productName,
        productSku: item.productSku,
        unitPrice: Number(item.unitPrice ?? 0),
        quantity: Number(item.quantity ?? 1),
        maxStock: currentCatalogStock + Number(item.quantity ?? 0)
      }
    })
  } else {
    editingOrderId.value = null
    editingOrderCode.value = ''
    orderForm.customerName = ''
    orderForm.customerPhone = ''
    orderForm.customerEmail = ''
    orderForm.shippingAddress = ''
    orderForm.note = ''
    orderForm.paymentMethod = 'BANK_TRANSFER'
    orderForm.paymentStatus = 'UNPAID'
    orderForm.discountAmount = 0
    orderForm.taxRate = 8
    orderForm.items = []
  }

  selectedProductToAdd.value = null
  orderModalVisible.value = true
}

function closeOrderModal() {
  orderModalVisible.value = false
}

function addProductToOrder(productId) {
  if (!productId) return
  const prod = availableProducts.value.find((p) => p.id === productId)
  if (!prod) return

  const existing = orderForm.items.find((i) => i.productId === prod.id)
  if (existing) {
    if (existing.quantity < existing.maxStock) {
      existing.quantity += 1
    } else {
      ElMessage.warning(`Sản phẩm "${prod.name}" đã đạt tối đa tồn kho (${existing.maxStock})!`)
    }
  } else {
    if ((prod.stock ?? 0) <= 0 && !editingOrderId.value) {
      ElMessage.warning(`Sản phẩm "${prod.name}" đã hết hàng trong kho!`)
    } else {
      orderForm.items.push({
        productId: prod.id,
        productName: prod.name,
        productSku: prod.sku,
        unitPrice: Number(prod.price ?? 0),
        quantity: 1,
        maxStock: Number(prod.stock ?? 0)
      })
    }
  }

  selectedProductToAdd.value = null
}

function stepItemQty(index, delta) {
  const item = orderForm.items[index]
  if (!item) return
  const next = Number(item.quantity ?? 1) + delta
  if (next < 1) return
  if (next > item.maxStock) {
    ElMessage.warning(`Số lượng vượt quá tồn kho (${item.maxStock})!`)
    return
  }
  item.quantity = next
}

function removeOrderItem(index) {
  orderForm.items.splice(index, 1)
}

function fillSampleOrder() {
  orderForm.customerName = 'Công ty CP Giải pháp Doanh nghiệp An Phát'
  orderForm.customerPhone = '0909 888 777'
  orderForm.customerEmail = 'muahang@anphat.vn'
  orderForm.shippingAddress = 'Tầng 8, Tòa nhà Lotte, Ba Đình, Hà Nội'
  orderForm.paymentMethod = 'BANK_TRANSFER'
  orderForm.paymentStatus = 'PAID'
  orderForm.taxRate = 8
  orderForm.note = 'Giao giờ hành chính, xuất kèm hóa đơn GTGT bản PDF.'

  if (orderForm.items.length === 0) {
    const firstInStock = availableProducts.value.find((p) => (p.stock ?? 0) > 0)
    if (firstInStock) {
      addProductToOrder(firstInStock.id)
    }
  }
}

async function submitOrderForm() {
  if (!orderForm.customerName?.trim()) {
    ElMessage.warning('Vui lòng nhập tên khách hàng!')
    return
  }
  if (orderForm.items.length === 0) {
    ElMessage.warning('Vui lòng chọn ít nhất 1 sản phẩm vào đơn hàng!')
    return
  }
  for (const item of orderForm.items) {
    if (!item.quantity || item.quantity < 1) {
      ElMessage.warning(`Số lượng cho "${item.productName}" phải >= 1!`)
      return
    }
  }

  savingOrder.value = true
  try {
    const payload = {
      customerName: orderForm.customerName.trim(),
      customerPhone: orderForm.customerPhone?.trim() || null,
      customerEmail: orderForm.customerEmail?.trim() || null,
      shippingAddress: orderForm.shippingAddress?.trim() || null,
      note: orderForm.note?.trim() || null,
      paymentMethod: orderForm.paymentMethod,
      paymentStatus: orderForm.paymentStatus,
      discountAmount: Number(orderForm.discountAmount ?? 0),
      taxRate: Number(orderForm.taxRate ?? 0),
      items: orderForm.items.map((i) => ({
        productId: i.productId,
        quantity: Number(i.quantity)
      }))
    }

    if (editingOrderId.value) {
      await api.put(`/orders/${editingOrderId.value}`, payload)
      ElMessage.success('Đã cập nhật đơn hàng và đồng bộ lại tồn kho!')
    } else {
      await api.post('/orders', payload)
      ElMessage.success('Tạo đơn hàng thành công & đã trừ tồn kho tự động!')
    }

    orderModalVisible.value = false
    await Promise.all([fetchOrders(), fetchOrderStats(), fetchInventoryLogs()])
  } catch (err) {
    ElMessage.error(err.response?.data?.message || 'Không thể lưu đơn hàng!')
  } finally {
    savingOrder.value = false
  }
}

// =========================================================================
// DETAIL, STATUS WORKFLOW & DELETE
// =========================================================================
async function openDetailModal(order) {
  try {
    const res = await api.get(`/orders/${order.id}`)
    activeOrder.value = res.data
    detailModalVisible.value = true
  } catch (err) {
    ElMessage.error(err.response?.data?.message || 'Không thể tải chi tiết đơn hàng!')
  }
}

async function changeOrderStatus(order, newStatus, note = null) {
  updatingStatus.value = true
  try {
    const res = await api.patch(`/orders/${order.id}/status`, {
      status: newStatus,
      note
    })
    activeOrder.value = res.data
    ElMessage.success(`Đã chuyển trạng thái đơn ${order.orderCode} → ${res.data.statusLabel}`)
    await Promise.all([fetchOrders(), fetchOrderStats(), fetchInventoryLogs()])
  } catch (err) {
    ElMessage.error(err.response?.data?.message || 'Không thể cập nhật trạng thái đơn!')
  } finally {
    updatingStatus.value = false
  }
}

async function markOrderPaid(order) {
  updatingStatus.value = true
  try {
    const res = await api.patch(`/orders/${order.id}/payment`, { paymentStatus: 'PAID' })
    activeOrder.value = res.data
    ElMessage.success(`Đã xác nhận thanh toán cho đơn ${order.orderCode}!`)
    await Promise.all([fetchOrders(), fetchOrderStats()])
  } catch (err) {
    ElMessage.error(err.response?.data?.message || 'Không thể cập nhật thanh toán!')
  } finally {
    updatingStatus.value = false
  }
}

function confirmCancelOrder(order) {
  ElMessageBox.prompt(
    `Hủy đơn ${order.orderCode} sẽ tự động hoàn lại toàn bộ số lượng sản phẩm vào kho. Nhập lý do hủy:`,
    'Xác nhận hủy đơn hàng',
    {
      confirmButtonText: 'Hủy đơn & Hoàn kho',
      cancelButtonText: 'Quay lại',
      confirmButtonClass: 'el-button--danger',
      inputPlaceholder: 'VD: Khách đổi ý / Sai thông tin giao hàng'
    }
  ).then(({ value }) => {
    changeOrderStatus(order, 'CANCELLED', value || 'Hủy theo yêu cầu')
  }).catch(() => {})
}

function confirmDeleteOrder(order) {
  ElMessageBox.confirm(
    `Bạn có chắc chắn muốn xóa vĩnh viễn đơn ${order.orderCode}?`,
    'Xóa đơn hàng',
    {
      confirmButtonText: 'Xóa đơn',
      cancelButtonText: 'Hủy',
      type: 'warning'
    }
  ).then(async () => {
    try {
      await api.delete(`/orders/${order.id}`)
      ElMessage.success(`Đã xóa đơn hàng ${order.orderCode}`)
      await Promise.all([fetchOrders(), fetchOrderStats(), fetchInventoryLogs()])
    } catch (err) {
      ElMessage.error(err.response?.data?.message || 'Không thể xóa đơn hàng!')
    }
  }).catch(() => {})
}

// =========================================================================
// MANUAL STOCK ADJUSTMENT
// =========================================================================
async function openStockAdjustModal() {
  await fetchProductsCatalog()
  adjustForm.productId = availableProducts.value[0]?.id || null
  adjustForm.quantityDelta = 10
  adjustForm.note = ''
  adjustModalVisible.value = true
}

async function submitStockAdjust() {
  if (!adjustForm.productId) {
    ElMessage.warning('Vui lòng chọn sản phẩm!')
    return
  }
  if (!adjustForm.quantityDelta || adjustForm.quantityDelta === 0) {
    ElMessage.warning('Số lượng thay đổi phải khác 0!')
    return
  }
  savingAdjust.value = true
  try {
    await api.post('/orders/inventory-adjust', {
      productId: adjustForm.productId,
      quantityDelta: Number(adjustForm.quantityDelta),
      note: adjustForm.note?.trim() || null
    })
    ElMessage.success('Đã cập nhật tồn kho & ghi nhận nhật ký kho!')
    adjustModalVisible.value = false
    await Promise.all([fetchInventoryLogs(), fetchProductsCatalog()])
  } catch (err) {
    ElMessage.error(err.response?.data?.message || 'Không thể điều chỉnh tồn kho!')
  } finally {
    savingAdjust.value = false
  }
}

// =========================================================================
// EXPORTS: EXCEL (.xls), CSV & PDF A4 INVOICE
// =========================================================================
function triggerBlobDownload(blobData, mimeType, filename) {
  const url = window.URL.createObjectURL(new Blob([blobData], { type: mimeType }))
  const link = document.createElement('a')
  link.href = url
  link.setAttribute('download', filename)
  document.body.appendChild(link)
  link.click()
  link.remove()
  window.URL.revokeObjectURL(url)
}

function handleExportCommand(cmd) {
  if (cmd === 'excel') handleExportExcel()
  else if (cmd === 'csv') handleExportCsv()
}

function handleOrderRowCommand(cmd, order) {
  if (cmd === 'edit') openOrderModal(order)
  else if (cmd === 'delete') confirmDeleteOrder(order)
}

async function handleExportExcel() {
  exportingExcel.value = true
  try {
    const params = {}
    if (orderFilter.keyword?.trim()) params.keyword = orderFilter.keyword.trim()
    if (orderFilter.status) params.status = orderFilter.status
    if (orderFilter.paymentStatus) params.paymentStatus = orderFilter.paymentStatus

    const res = await api.get('/orders/export-excel', {
      params,
      responseType: 'blob'
    })
    triggerBlobDownload(
      res.data,
      'application/vnd.ms-excel;charset=utf-8',
      `erp-orders-report-${new Date().toISOString().slice(0, 10)}.xls`
    )
    ElMessage.success('Đã xuất file Excel (.xls) gồm sheet Đơn hàng & Lịch sử Kho!')
  } catch (err) {
    ElMessage.error('Không thể xuất file Excel!')
  } finally {
    exportingExcel.value = false
  }
}

async function handleExportCsv() {
  exportingCsv.value = true
  try {
    const params = {}
    if (orderFilter.keyword?.trim()) params.keyword = orderFilter.keyword.trim()
    if (orderFilter.status) params.status = orderFilter.status
    if (orderFilter.paymentStatus) params.paymentStatus = orderFilter.paymentStatus

    const res = await api.get('/orders/export-csv', {
      params,
      responseType: 'blob'
    })
    triggerBlobDownload(
      res.data,
      'text/csv;charset=utf-8',
      `erp-orders-${new Date().toISOString().slice(0, 10)}.csv`
    )
    ElMessage.success('Đã xuất file CSV!')
  } catch (err) {
    ElMessage.error('Không thể xuất file CSV!')
  } finally {
    exportingCsv.value = false
  }
}

function escapeHtml(str) {
  return String(str ?? '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
}

function printOrderInvoice(order) {
  const win = window.open('', '_blank', 'width=920,height=780')
  if (!win) {
    ElMessage.warning('Trình duyệt đang chặn cửa sổ bật lên (Pop-up). Hãy cho phép Pop-up để xuất PDF A4!')
    return
  }

  const rowsHtml = (order.items || []).map((item, idx) => `
    <tr>
      <td class="tc">${idx + 1}</td>
      <td>
        <strong>${escapeHtml(item.productName)}</strong>
        <div class="sku">SKU: ${escapeHtml(item.productSku)}</div>
      </td>
      <td class="tc">${item.quantity}</td>
      <td class="tr">${formatCurrency(item.unitPrice)}</td>
      <td class="tr"><strong>${formatCurrency(item.lineTotal)}</strong></td>
    </tr>
  `).join('')

  const html = `<!DOCTYPE html>
  <html lang="vi">
  <head>
    <meta charset="UTF-8" />
    <title>Hoa-don-${escapeHtml(order.orderCode)}</title>
    <style>
      @page { size: A4; margin: 14mm; }
      * { box-sizing: border-box; }
      body {
        font-family: 'Inter', 'Segoe UI', Roboto, Arial, sans-serif;
        color: #0f172a;
        margin: 0;
        padding: 20px;
        background: #fff;
        font-size: 13px;
        line-height: 1.5;
      }
      .invoice-sheet { max-width: 790px; margin: 0 auto; }
      .header {
        display: flex;
        justify-content: space-between;
        align-items: flex-start;
        border-bottom: 2px solid #1d4ed8;
        padding-bottom: 14px;
        margin-bottom: 18px;
      }
      .company-name { font-size: 20px; font-weight: 800; color: #1e3a8a; letter-spacing: -0.02em; }
      .company-meta { font-size: 12px; color: #475569; margin-top: 4px; }
      .invoice-badge { text-align: right; }
      .invoice-title { font-size: 22px; font-weight: 800; color: #0f172a; margin: 0; }
      .order-code { font-size: 14px; font-weight: 700; color: #1d4ed8; margin-top: 4px; }
      .meta-grid {
        display: grid;
        grid-template-columns: 1fr 1fr;
        gap: 14px;
        margin-bottom: 18px;
      }
      .meta-box {
        border: 1px solid #e2e8f0;
        border-radius: 8px;
        padding: 12px 14px;
        background: #f8fafc;
      }
      .meta-box h4 {
        margin: 0 0 6px;
        font-size: 11px;
        text-transform: uppercase;
        letter-spacing: 0.06em;
        color: #64748b;
      }
      .meta-box p { margin: 3px 0; font-size: 12.5px; }
      table { width: 100%; border-collapse: collapse; margin-bottom: 16px; }
      th {
        background: #1e3a8a;
        color: #fff;
        font-size: 12px;
        padding: 9px 10px;
        text-align: left;
      }
      td {
        padding: 9px 10px;
        border-bottom: 1px solid #e2e8f0;
        vertical-align: top;
      }
      .tc { text-align: center; }
      .tr { text-align: right; }
      .sku { font-size: 11px; color: #64748b; font-family: monospace; }
      .totals-wrap {
        width: 330px;
        margin-left: auto;
        border: 1px solid #e2e8f0;
        border-radius: 8px;
        padding: 12px 14px;
        background: #f8fafc;
      }
      .t-row { display: flex; justify-content: space-between; margin-bottom: 6px; font-size: 13px; }
      .t-grand {
        display: flex;
        justify-content: space-between;
        border-top: 1.5px solid #cbd5e1;
        padding-top: 8px;
        margin-top: 6px;
        font-size: 15px;
        font-weight: 800;
        color: #1d4ed8;
      }
      .words { margin-top: 12px; font-size: 12.5px; color: #334155; }
      .signs {
        display: grid;
        grid-template-columns: repeat(3, 1fr);
        gap: 20px;
        margin-top: 38px;
        text-align: center;
      }
      .sign-col strong { display: block; font-size: 13px; }
      .sign-col span { font-size: 11px; color: #64748b; }
      .sign-space { height: 64px; }
      .print-toolbar {
        display: flex;
        justify-content: flex-end;
        gap: 10px;
        margin-bottom: 16px;
      }
      .btn-print {
        background: #1d4ed8;
        color: #fff;
        border: none;
        border-radius: 6px;
        padding: 8px 16px;
        font-weight: 600;
        cursor: pointer;
      }
      @media print {
        body { padding: 0; }
        .print-toolbar { display: none; }
      }
    </style>
  </head>
  <body>
    <div class="invoice-sheet">
      <div class="print-toolbar">
        <button class="btn-print" onclick="window.print()">🖨️ In / Lưu thành PDF (A4)</button>
      </div>
      <div class="header">
        <div>
          <div class="company-name">ERP CLOUD ENTERPRISE SUITE</div>
          <div class="company-meta">Hệ thống Quản trị Doanh nghiệp &amp; Kho vận Hợp nhất</div>
          <div class="company-meta">MST: 0109998888 • Hotline: 1900 6868 • Hà Nội, Việt Nam</div>
        </div>
        <div class="invoice-badge">
          <h1 class="invoice-title">HÓA ĐƠN BÁN HÀNG</h1>
          <div class="order-code">Số phiếu: ${escapeHtml(order.orderCode)}</div>
          <div class="company-meta">Ngày lập: ${escapeHtml(formatDateTime(order.createdAt))}</div>
        </div>
      </div>

      <div class="meta-grid">
        <div class="meta-box">
          <h4>Thông tin Khách hàng</h4>
          <p><strong>${escapeHtml(order.customerName)}</strong></p>
          <p>SĐT: ${escapeHtml(order.customerPhone || '—')}</p>
          <p>Email: ${escapeHtml(order.customerEmail || '—')}</p>
          <p>Địa chỉ: ${escapeHtml(order.shippingAddress || '—')}</p>
        </div>
        <div class="meta-box">
          <h4>Thông tin Chứng từ &amp; Thanh toán</h4>
          <p>Trạng thái đơn: <strong>${escapeHtml(order.statusLabel)}</strong></p>
          <p>Thanh toán: <strong>${escapeHtml(order.paymentStatusLabel)}</strong></p>
          <p>Hình thức: ${escapeHtml(order.paymentMethodLabel)}</p>
          <p>Nhân viên lập: @${escapeHtml(order.createdByUsername || 'system')}</p>
        </div>
      </div>

      <table>
        <thead>
          <tr>
            <th class="tc" style="width: 44px;">STT</th>
            <th>Tên hàng hóa / Mã SKU</th>
            <th class="tc" style="width: 64px;">SL</th>
            <th class="tr" style="width: 130px;">Đơn giá</th>
            <th class="tr" style="width: 140px;">Thành tiền</th>
          </tr>
        </thead>
        <tbody>${rowsHtml}</tbody>
      </table>

      <div class="totals-wrap">
        <div class="t-row"><span>Tạm tính:</span><span>${formatCurrency(order.subtotal)}</span></div>
        <div class="t-row"><span>Giảm giá:</span><span>− ${formatCurrency(order.discountAmount)}</span></div>
        <div class="t-row"><span>Thuế VAT (${order.taxRate ?? 0}%):</span><span>+ ${formatCurrency(order.taxAmount)}</span></div>
        <div class="t-grand"><span>TỔNG CỘNG:</span><span>${formatCurrency(order.totalAmount)}</span></div>
      </div>

      <div class="words">
        <strong>Số tiền viết bằng chữ:</strong> <em>${escapeHtml(numberToVietnameseWords(Math.round(order.totalAmount ?? 0)))}</em>
        ${order.note ? `<br/><strong>Ghi chú:</strong> ${escapeHtml(order.note)}` : ''}
      </div>

      <div class="signs">
        <div class="sign-col">
          <strong>NGƯỜI MUA HÀNG</strong>
          <span>(Ký, ghi rõ họ tên)</span>
          <div class="sign-space"></div>
        </div>
        <div class="sign-col">
          <strong>THỦ KHO / GIAO NHẬN</strong>
          <span>(Ký, ghi rõ họ tên)</span>
          <div class="sign-space"></div>
        </div>
        <div class="sign-col">
          <strong>NGƯỜI LẬP PHIẾU</strong>
          <span>(Ký, đóng dấu doanh nghiệp)</span>
          <div class="sign-space"></div>
        </div>
      </div>
    </div>
    <script>
      window.onload = function() { setTimeout(function() { window.print(); }, 250); };
    <\/script>
  </body>
  </html>`

  win.document.open()
  win.document.write(html)
  win.document.close()
}

// =========================================================================
// LIFECYCLE
// =========================================================================
onMounted(() => {
  fetchOrders()
  fetchOrderStats()
  fetchInventoryLogs()
})

onBeforeUnmount(() => {
  clearTimeout(orderSearchTimer)
  clearTimeout(logSearchTimer)
})
</script>

<style scoped>
.orders-container {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding-bottom: 64px;
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}

/* HEADER CARD */
.orders-header-card {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 11px;
  padding: 10px 14px;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 10px 14px;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.04);
}

.header-left-col {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  flex: 1 1 280px;
  min-width: 0;
}

.header-icon-box {
  width: 42px;
  height: 42px;
  border-radius: 11px;
  background: #eff6ff;
  color: #2563eb;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.header-title-block {
  min-width: 0;
}

.title-with-pill {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.header-title {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  color: #0f172a;
}

.version-pill-badge {
  background: #e0f2fe;
  color: #0369a1;
  font-size: 11px;
  font-weight: 700;
  padding: 2px 8px;
  border-radius: 999px;
}

.header-subtitle {
  margin: 4px 0 0;
  font-size: 13px;
  color: #64748b;
}

.header-right-col {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}

.btn-header-outline,
.btn-header-emerald,
.btn-header-primary {
  height: 36px;
  padding: 0 13px;
  border-radius: 9px;
  font-size: 13px;
  font-weight: 600;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  white-space: nowrap;
  cursor: pointer;
  transition: all 0.16s ease;
}

.btn-header-outline {
  background: #ffffff;
  color: #334155;
  border: 1px solid #cbd5e1;
}

.btn-header-outline:hover:not(:disabled) {
  background: #f8fafc;
  border-color: #94a3b8;
}

.btn-header-emerald {
  background: #ecfdf5;
  color: #047857;
  border: 1px solid #a7f3d0;
}

.btn-header-emerald:hover {
  background: #d1fae5;
}

.btn-header-primary {
  background: #2563eb;
  color: #ffffff;
  border: 1px solid #2563eb;
}

.btn-header-primary:hover:not(:disabled) {
  background: #1d4ed8;
}

/* KPI GRID */
.orders-metrics-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.kpi-stat-card {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 12px 15px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
}

.kpi-stat-content {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.kpi-stat-label {
  font-size: 11.5px;
  font-weight: 600;
  color: #64748b;
}

.kpi-stat-value {
  font-size: 19px;
  font-weight: 800;
  color: #0f172a;
  line-height: 1.15;
}

.kpi-stat-sub {
  font-size: 11px;
  font-weight: 500;
}

.text-blue { color: #2563eb; }
.text-amber { color: #d97706; }
.text-green { color: #059669; }
.text-indigo { color: #4338ca; }
.text-red { color: #dc2626; }
.text-muted { color: #64748b; }

.kpi-stat-icon {
  width: 34px;
  height: 34px;
  border-radius: 9px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.icon-blue { background: #eff6ff; color: #2563eb; }
.icon-amber { background: #fffbeb; color: #d97706; }
.icon-teal { background: #ecfdf5; color: #059669; }
.icon-indigo { background: #eef2ff; color: #4338ca; }

/* MODULE TABS */
.module-tabs-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  border-bottom: 1px solid #e2e8f0;
  padding-bottom: 0;
}

.module-tab-btn {
  height: 36px;
  padding: 0 14px;
  border-radius: 8px 8px 0 0;
  border: 1px solid transparent;
  border-bottom: 2px solid transparent;
  background: transparent;
  color: #64748b;
  font-size: 13px;
  font-weight: 600;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  margin-bottom: -1px;
  transition: all 0.15s ease;
}

.module-tab-btn:hover {
  color: #0f172a;
}

.module-tab-btn.active {
  background: #ffffff;
  color: #1d4ed8;
  border-color: #e2e8f0 #e2e8f0 #2563eb #e2e8f0;
  border-bottom: 2px solid #2563eb;
}

.tab-count-pill {
  background: #f1f5f9;
  color: #475569;
  padding: 1px 7px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 700;
}

.module-tab-btn.active .tab-count-pill {
  background: #dbeafe;
  color: #1d4ed8;
}

/* FILTER BAR */
.filter-toolbar-card {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  padding: 8px 12px;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.filter-controls-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
  flex: 1 1 420px;
  min-width: 0;
}

.search-input-wrap {
  display: flex;
  align-items: center;
  gap: 8px;
  background: #f8fafc;
  border: 1px solid #cbd5e1;
  border-radius: 9px;
  padding: 0 11px;
  height: 36px;
  flex: 1 1 240px;
  min-width: 180px;
}

.custom-search-input {
  border: none;
  background: transparent;
  outline: none;
  width: 100%;
  font-size: 13px;
  color: #0f172a;
}

.filter-select {
  width: 175px;
}

.log-date-picker {
  width: 248px !important;
  flex-shrink: 0;
}

.btn-reset-filter {
  height: 36px;
  padding: 0 12px;
  border: 1px solid #cbd5e1;
  border-radius: 9px;
  background: #ffffff;
  color: #475569;
  font-size: 13px;
  font-weight: 600;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
}

.filter-summary-right {
  font-size: 13px;
  color: #475569;
  white-space: nowrap;
}

/* DATA CARD & TABLE */
.data-card {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 14px;
  overflow: hidden;
}

.loading-box,
.empty-state-box {
  padding: 36px 24px;
  text-align: center;
}

.empty-icon-circle {
  width: 56px;
  height: 56px;
  margin: 0 auto 12px;
  border-radius: 50%;
  background: #eff6ff;
  font-size: 26px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.empty-state-box h4 {
  margin: 0 0 6px;
  font-size: 16px;
  color: #0f172a;
}

.empty-state-box p {
  margin: 0 0 16px;
  font-size: 13px;
  color: #64748b;
}

.table-responsive-scroll {
  width: 100%;
  overflow-x: auto;
  min-width: 0;
}

.erp-table {
  width: 100%;
  border-collapse: collapse;
  min-width: 820px;
}

.erp-table th {
  background: #f8fafc;
  color: #475569;
  font-size: 11.5px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.03em;
  padding: 10px 14px;
  border-bottom: 1px solid #e2e8f0;
  text-align: left;
  white-space: nowrap;
}

.erp-table td {
  padding: 11px 14px;
  border-bottom: 1px solid #f1f5f9;
  vertical-align: middle;
  font-size: 13px;
  color: #0f172a;
}

.erp-table tr:hover td {
  background: #f8fafc;
}

.text-center { text-align: center !important; }
.text-right { text-align: right !important; }

.order-code-link {
  border: none;
  background: transparent;
  padding: 0;
  font-weight: 700;
  font-size: 13px;
  color: #2563eb;
  cursor: pointer;
  white-space: nowrap;
}

.order-code-link:hover {
  text-decoration: underline;
}

.customer-name-cell {
  font-weight: 600;
  color: #0f172a;
  max-width: 240px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.qty-plain {
  font-weight: 600;
  color: #334155;
  white-space: nowrap;
}

.sub-muted-line {
  font-size: 11.5px;
  color: #64748b;
  margin-top: 2px;
  white-space: nowrap;
}

.qty-chip,
.sku-pill,
.ref-code-chip {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 6px;
  font-size: 11.5px;
  font-weight: 600;
  background: #f1f5f9;
  color: #334155;
  white-space: nowrap;
}

.price-cell {
  font-weight: 700;
  color: #1d4ed8;
  white-space: nowrap;
}

.date-cell {
  font-size: 12.5px;
  color: #475569;
  white-space: nowrap;
}

.status-badge {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 3px 9px;
  border-radius: 999px;
  font-size: 11.5px;
  font-weight: 600;
  white-space: nowrap;
}

.badge-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: currentColor;
}

.badge-amber { background: #fef3c7; color: #b45309; }
.badge-blue { background: #dbeafe; color: #1d4ed8; }
.badge-purple { background: #ede9fe; color: #6d28d9; }
.badge-green { background: #d1fae5; color: #047857; }
.badge-red { background: #fee2e2; color: #b91c1c; }
.badge-gray { background: #f1f5f9; color: #475569; }

.delta-pill {
  display: inline-block;
  padding: 2px 9px;
  border-radius: 6px;
  font-weight: 700;
  font-size: 12.5px;
}
.delta-in { background: #d1fae5; color: #047857; }
.delta-out { background: #fee2e2; color: #b91c1c; }

.row-actions-group {
  display: inline-flex;
  align-items: center;
  justify-content: flex-end;
  gap: 6px;
}

.btn-row-action {
  height: 28px;
  padding: 0 9px;
  border-radius: 7px;
  border: 1px solid #cbd5e1;
  background: #ffffff;
  color: #334155;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  white-space: nowrap;
}

.btn-row-action.primary {
  background: #eff6ff;
  border-color: #bfdbfe;
  color: #1d4ed8;
}

.btn-row-action.pdf {
  background: #f5f3ff;
  border-color: #ddd6fe;
  color: #6d28d9;
}

.btn-row-action.danger {
  background: #fef2f2;
  border-color: #fecaca;
  color: #dc2626;
}

.btn-row-more {
  width: 28px;
  height: 28px;
  border-radius: 7px;
  border: 1px solid #cbd5e1;
  background: #ffffff;
  color: #475569;
  font-weight: 700;
  font-size: 14px;
  line-height: 1;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.btn-row-more:hover {
  background: #f8fafc;
  color: #0f172a;
}

.table-pagination-bar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 12px 16px;
  border-top: 1px solid #f1f5f9;
}

.pagination-info {
  font-size: 12.5px;
  color: #64748b;
}

/* MOBILE CARDS */
.mobile-cards-list {
  display: none;
  flex-direction: column;
  gap: 10px;
  padding: 12px;
}

.mobile-order-card {
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 12px;
  background: #ffffff;
}

.m-card-top {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 8px;
  margin-bottom: 10px;
}

.m-card-body {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 8px 0;
  border-top: 1px solid #f1f5f9;
  border-bottom: 1px solid #f1f5f9;
}

.m-customer-row,
.m-price-row,
.m-pay-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
}

.m-card-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 10px;
}

/* MODALS */
.custom-modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.55);
  backdrop-filter: blur(2px);
  z-index: 1050;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px;
}

.custom-modal-card {
  background: #ffffff;
  border-radius: 16px;
  width: 100%;
  max-height: 92vh;
  display: flex;
  flex-direction: column;
  box-shadow: 0 24px 48px rgba(15, 23, 42, 0.22);
  overflow: hidden;
}

.order-builder-modal { max-width: 860px; }
.detail-modal { max-width: 820px; }
.adjust-modal { max-width: 500px; }

.modal-head {
  padding: 16px 20px;
  border-bottom: 1px solid #e2e8f0;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.modal-title-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.modal-title-row h3 {
  margin: 0;
  font-size: 17px;
  font-weight: 700;
  color: #0f172a;
}

.modal-pill {
  background: #eff6ff;
  color: #2563eb;
  font-size: 10.5px;
  font-weight: 700;
  padding: 2px 8px;
  border-radius: 6px;
}

.modal-subtitle {
  margin: 4px 0 0;
  font-size: 12.5px;
  color: #64748b;
}

.modal-head-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.btn-quick-fill {
  height: 32px;
  padding: 0 11px;
  border-radius: 8px;
  border: 1px solid #fcd34d;
  background: #fffbeb;
  color: #b45309;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
}

.btn-modal-close {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  border: 1px solid #e2e8f0;
  background: #f8fafc;
  color: #475569;
  cursor: pointer;
}

.modal-body-scroll {
  padding: 16px 20px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.form-section-box {
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 14px;
  background: #fafbfc;
}

.section-box-title {
  font-size: 13px;
  font-weight: 700;
  color: #0f172a;
  margin-bottom: 10px;
}

.form-grid-2 {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px 14px;
}

.field-group {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.field-group.full-col {
  grid-column: 1 / -1;
}

.field-label {
  font-size: 12px;
  font-weight: 600;
  color: #334155;
}

.req { color: #dc2626; }

.erp-input,
.erp-textarea {
  width: 100%;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  padding: 8px 10px;
  font-size: 13px;
  color: #0f172a;
  background: #ffffff;
  outline: none;
}

.erp-input:focus,
.erp-textarea:focus {
  border-color: #2563eb;
}

.product-picker-select {
  width: 100%;
}

.empty-items-hint {
  margin-top: 10px;
  padding: 16px;
  border: 1px dashed #cbd5e1;
  border-radius: 9px;
  text-align: center;
  font-size: 12.5px;
  color: #64748b;
  background: #ffffff;
}

.order-items-table-wrap {
  margin-top: 10px;
  overflow-x: auto;
  border: 1px solid #e2e8f0;
  border-radius: 9px;
  background: #ffffff;
}

.mini-items-table {
  width: 100%;
  border-collapse: collapse;
  min-width: 520px;
}

.mini-items-table th {
  background: #f8fafc;
  padding: 8px 10px;
  font-size: 11.5px;
  color: #475569;
  text-align: left;
  border-bottom: 1px solid #e2e8f0;
}

.mini-items-table td {
  padding: 9px 10px;
  border-bottom: 1px solid #f1f5f9;
  font-size: 12.5px;
}

.item-prod-name {
  font-weight: 600;
  color: #0f172a;
}

.qty-stepper-inline {
  display: inline-flex;
  align-items: center;
  border: 1px solid #cbd5e1;
  border-radius: 7px;
  overflow: hidden;
}

.qty-stepper-inline button {
  width: 26px;
  height: 28px;
  border: none;
  background: #f8fafc;
  cursor: pointer;
  font-weight: 700;
}

.qty-stepper-inline input {
  width: 44px;
  height: 28px;
  border: none;
  text-align: center;
  font-size: 12.5px;
  font-weight: 600;
  outline: none;
}

.btn-remove-item {
  border: none;
  background: #fee2e2;
  color: #dc2626;
  width: 24px;
  height: 24px;
  border-radius: 6px;
  cursor: pointer;
}

.summary-section-box {
  display: grid;
  grid-template-columns: 1.2fr 1fr;
  gap: 16px;
}

.summary-left-inputs {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.summary-right-totals {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  padding: 12px 14px;
  display: flex;
  flex-direction: column;
  gap: 7px;
}

.total-line {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
  color: #334155;
}

.total-grand-line {
  display: flex;
  justify-content: space-between;
  border-top: 1px dashed #cbd5e1;
  padding-top: 8px;
  margin-top: 4px;
  font-size: 15px;
  font-weight: 800;
  color: #1d4ed8;
}

.amount-words-hint {
  font-size: 11.5px;
  color: #64748b;
  margin-top: 2px;
}

.modal-foot {
  padding: 13px 20px;
  border-top: 1px solid #e2e8f0;
  background: #f8fafc;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.foot-hint {
  font-size: 12px;
  color: #64748b;
}

.foot-buttons {
  display: flex;
  gap: 8px;
}

/* DETAIL MODAL EXTRAS - ULTRA CLEAN RECEIPT UX */
.detail-modal {
  max-width: 720px;
}

.detail-body-clean {
  padding: 20px 24px;
  gap: 20px;
}

.clean-stepper {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 8px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f1f5f9;
}

.clean-step {
  display: flex;
  flex-direction: column;
  gap: 6px;
  opacity: 0.45;
}

.clean-step.done {
  opacity: 1;
}

.clean-step-node {
  display: flex;
  align-items: center;
  gap: 8px;
}

.clean-step-dot {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: #e2e8f0;
  color: #475569;
  font-size: 11px;
  font-weight: 700;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.clean-step.done .clean-step-dot {
  background: #2563eb;
  color: #ffffff;
}

.clean-step-line {
  flex: 1;
  height: 2px;
  background: #e2e8f0;
  border-radius: 2px;
}

.clean-step.done .clean-step-line {
  background: #bfdbfe;
}

.clean-step-label {
  font-size: 12px;
  font-weight: 600;
  color: #334155;
}

.clean-step.current .clean-step-label {
  color: #1d4ed8;
}

.cancelled-banner {
  background: #fef2f2;
  color: #b91c1c;
  border-radius: 8px;
  padding: 10px 14px;
  font-size: 13px;
  font-weight: 500;
}

.clean-meta-grid {
  display: grid;
  grid-template-columns: 1.2fr 1fr;
  gap: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f1f5f9;
}

.clean-meta-col {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.clean-section-kicker {
  font-size: 11px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: #94a3b8;
  margin-bottom: 4px;
}

.clean-customer-title {
  font-size: 14px;
  font-weight: 700;
  color: #0f172a;
}

.clean-meta-line {
  font-size: 12.5px;
  color: #475569;
  line-height: 1.45;
}

.clean-meta-kv {
  display: flex;
  flex-direction: column;
  gap: 2px;
  font-size: 12.5px;
  color: #334155;
  margin-bottom: 6px;
}

.clean-meta-kv > span:first-child {
  font-size: 11.5px;
  color: #64748b;
}

.clean-items-wrap {
  width: 100%;
  overflow-x: auto;
}

.clean-items-table {
  width: 100%;
  border-collapse: collapse;
  min-width: 460px;
}

.clean-items-table th {
  padding: 0 0 10px;
  font-size: 11px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.04em;
  color: #64748b;
  border-bottom: 1px solid #e2e8f0;
  text-align: left;
}

.clean-items-table td {
  padding: 11px 0;
  border-bottom: 1px solid #f1f5f9;
  font-size: 13px;
  color: #0f172a;
  vertical-align: middle;
}

.clean-item-sku {
  font-family: monospace;
  font-size: 11px;
  color: #64748b;
  margin-top: 2px;
}

.muted-num {
  color: #475569;
}

.clean-receipt-footer {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 24px;
  padding-top: 4px;
}

.clean-receipt-words {
  font-size: 12px;
  color: #64748b;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.clean-receipt-totals {
  width: 260px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.detail-foot-clean {
  justify-content: space-between;
  background: #ffffff;
}

.detail-foot-left,
.detail-foot-right {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}

.btn-link-danger {
  border: none;
  background: transparent;
  color: #dc2626;
  font-size: 13px;
  font-weight: 600;
  padding: 6px 8px;
  border-radius: 7px;
  cursor: pointer;
}

.btn-link-danger:hover:not(:disabled) {
  background: #fef2f2;
}

.btn-status-step {
  height: 36px;
  padding: 0 14px;
  border-radius: 9px;
  border: none;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
}

.btn-status-step.ship { background: #7c3aed; color: #fff; }
.btn-status-step.complete { background: #059669; color: #fff; }

/* RESPONSIVE */
@media (max-width: 1120px) {
  .orders-metrics-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .desktop-table-wrap {
    display: none;
  }
  .mobile-cards-list {
    display: flex;
  }
  .form-grid-2,
  .summary-section-box,
  .clean-meta-grid,
  .clean-stepper {
    grid-template-columns: 1fr;
  }
  .clean-receipt-footer {
    flex-direction: column-reverse;
    align-items: stretch;
  }
  .clean-receipt-totals {
    width: 100%;
  }
  .filter-select,
  .log-date-picker {
    width: 100% !important;
  }
}

@media (max-width: 560px) {
  .orders-metrics-grid {
    grid-template-columns: 1fr;
  }
}
</style>
