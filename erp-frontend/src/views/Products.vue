<template>
  <div class="products-container">
    <!-- TOP HEADER CARD (CHUẨN MẪU KHO v4.2) -->
    <header class="products-header-card">
      <div class="header-left-col">
        <div class="header-icon-box">
          <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2">
            <path stroke-linecap="round" stroke-linejoin="round" d="M6 2L3 6v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6l-3-4z"></path>
            <line x1="3" y1="6" x2="21" y2="6"></line>
            <path d="M16 10a4 4 0 0 1-8 0"></path>
          </svg>
        </div>
        <div class="header-title-block">
          <div class="title-with-pill">
            <h2 class="header-title">Quản lý Sản phẩm</h2>
            <span class="version-pill-badge">Kho v4.2</span>
          </div>
          <p class="header-subtitle">Danh sách hàng hóa, giá niêm yết, phân trang cơ sở dữ liệu và tồn kho</p>
        </div>
      </div>

      <div class="header-right-col">
        <button
          type="button"
          class="btn-header-outline"
          title="Xuất danh sách sản phẩm hiện tại ra file Excel (CSV)"
          @click="handleExportExcel"
        >
          <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path>
            <polyline points="7 10 12 15 17 10"></polyline>
            <line x1="12" y1="15" x2="12" y2="3"></line>
          </svg>
          <span>Xuất Excel</span>
        </button>

        <button
          v-if="authStore.isAdmin"
          type="button"
          class="btn-header-primary"
          @click="openDialog()"
        >
          <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2.5">
            <line x1="12" y1="5" x2="12" y2="19"></line>
            <line x1="5" y1="12" x2="19" y2="12"></line>
          </svg>
          <span>Thêm sản phẩm mới</span>
        </button>
      </div>
    </header>

    <!-- 4 THẺ METRICS KPI KHO HÀNG -->
    <section class="products-metrics-grid">
      <div class="kpi-stat-card">
        <div class="kpi-stat-content">
          <span class="kpi-stat-label">Tổng số mặt hàng</span>
          <span class="kpi-stat-value">{{ totalElements }}</span>
          <span class="kpi-stat-sub text-green">↗ Đồng bộ cơ sở dữ liệu</span>
        </div>
        <div class="kpi-stat-icon icon-blue">
          <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"></path>
            <polyline points="3.27 6.96 12 12.01 20.73 6.96"></polyline>
            <line x1="12" y1="22.08" x2="12" y2="12"></line>
          </svg>
        </div>
      </div>

      <div class="kpi-stat-card">
        <div class="kpi-stat-content">
          <span class="kpi-stat-label">Đang sẵn hàng</span>
          <span class="kpi-stat-value">{{ inStockCount }}</span>
          <span class="kpi-stat-sub text-muted">{{ inStockRatio }}% danh mục hiển thị</span>
        </div>
        <div class="kpi-stat-icon icon-teal">
          <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path>
            <polyline points="22 4 12 14.01 9 11.01"></polyline>
          </svg>
        </div>
      </div>

      <div class="kpi-stat-card">
        <div class="kpi-stat-content">
          <span class="kpi-stat-label">Cảnh báo sắp hết</span>
          <span class="kpi-stat-value">{{ lowOrOutStockCount }}</span>
          <span class="kpi-stat-sub" :class="lowOrOutStockCount > 0 ? 'text-red' : 'text-muted'">
            {{ lowOrOutStockCount > 0 ? '⚠️ Dưới ngưỡng an toàn' : '✓ Mức tồn kho ổn định' }}
          </span>
        </div>
        <div class="kpi-stat-icon icon-red">
          <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="9" cy="21" r="1"></circle>
            <circle cx="20" cy="21" r="1"></circle>
            <path d="M1 1h4l2.68 13.39a2 2 0 0 0 2 1.61h9.72a2 2 0 0 0 2-1.61L23 6H6"></path>
          </svg>
        </div>
      </div>

      <div class="kpi-stat-card">
        <div class="kpi-stat-content">
          <span class="kpi-stat-label">Giá trị kho (trang)</span>
          <span class="kpi-stat-value">{{ formatCompactCurrency(totalInventoryValue) }}</span>
          <span class="kpi-stat-sub text-muted">Ước tính trên trang hiện tại</span>
        </div>
        <div class="kpi-stat-icon icon-indigo">
          <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2">
            <rect x="2" y="5" width="20" height="14" rx="2"></rect>
            <line x1="2" y1="10" x2="22" y2="10"></line>
          </svg>
        </div>
      </div>
    </section>

    <!-- THANH TÌM KIẾM & BỘ LỌC -->
    <div class="products-filter-card">
      <div class="filter-left-group">
        <el-input
          v-model="filters.keyword"
          placeholder="Tìm theo tên hoặc mã SKU..."
          clearable
          class="filter-search-input"
          @clear="onSearch"
          @keyup.enter="onSearch"
        >
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>

        <el-select
          v-model="filters.stockStatus"
          placeholder="Tất cả trạng thái"
          class="filter-select-box"
          @change="onFilterChange"
        >
          <el-option label="Tất cả trạng thái" value="ALL" />
          <el-option label="Còn hàng (> 10)" value="IN_STOCK" />
          <el-option label="Sắp hết (1 - 10)" value="LOW_STOCK" />
          <el-option label="Đã hết hàng (0)" value="OUT_OF_STOCK" />
        </el-select>

        <el-select
          v-model="filters.sort"
          placeholder="Sắp xếp"
          class="filter-select-box"
          @change="onFilterChange"
        >
          <el-option label="Mới nhất (Mặc định)" value="id,desc" />
          <el-option label="Giá: Thấp đến Cao" value="price,asc" />
          <el-option label="Giá: Cao đến Thấp" value="price,desc" />
          <el-option label="Tồn kho: Nhiều nhất" value="stock,desc" />
          <el-option label="Tên sản phẩm: A - Z" value="name,asc" />
        </el-select>

        <button type="button" class="btn-filter-reset" @click="resetFilters">
          <el-icon><RefreshRight /></el-icon>
          <span>Đặt lại</span>
        </button>
      </div>

      <div class="filter-right-count">
        Tổng số: <strong>{{ totalElements }}</strong> sản phẩm
      </div>
    </div>

    <!-- THANH XÓA HÀNG LOẠT KHI CHỌN NHIỀU -->
    <transition name="el-zoom-in-top">
      <div v-if="selectedProductIds.length > 0 && authStore.isAdmin" class="batch-bar">
        <div class="batch-left">
          <el-icon class="batch-icon"><Delete /></el-icon>
          <span>Đã chọn <strong>{{ selectedProductIds.length }}</strong> sản phẩm</span>
        </div>
        <div class="batch-actions">
          <el-button size="small" @click="clearSelection">Bỏ chọn</el-button>
          <el-button
            type="danger"
            size="small"
            :loading="batchDeleting"
            @click="handleBatchDelete"
          >
            Xóa {{ selectedProductIds.length }} sản phẩm
          </el-button>
        </div>
      </div>
    </transition>

    <!-- CARD BẢNG DANH SÁCH SẢN PHẨM (DESKTOP & TABLET) + THẺ MOBILE -->
    <div v-loading="loading" class="products-table-card">
      <!-- BẢNG TRÊN DESKTOP / TABLET (> 768px) -->
      <div class="desktop-table-wrapper">
        <el-table
          ref="tableRef"
          :data="products"
          size="small"
          style="width: 100%"
          row-key="id"
          header-row-class-name="erp-table-header"
          @selection-change="handleSelectionChange"
        >
          <el-table-column
            v-if="authStore.isAdmin"
            type="selection"
            width="46"
            align="center"
          />

          <el-table-column prop="id" label="ID" width="68" align="center">
            <template #default="{ row }">
              <span class="id-text-cell">{{ row.id }}</span>
            </template>
          </el-table-column>

          <el-table-column label="Hình ảnh" width="84" align="center">
            <template #default="{ row }">
              <el-image
                v-if="row.imageUrl"
                :src="row.imageUrl"
                :preview-src-list="[row.imageUrl]"
                preview-teleported
                fit="cover"
                class="product-thumb"
              >
                <template #error>
                  <div class="image-placeholder"><el-icon><Picture /></el-icon></div>
                </template>
              </el-image>
              <div v-else class="image-placeholder">
                <el-icon><Picture /></el-icon>
              </div>
            </template>
          </el-table-column>

          <el-table-column prop="name" label="Tên sản phẩm & Thông tin" min-width="220">
            <template #default="{ row }">
              <div class="product-name-cell">
                <span class="product-name">{{ row.name }}</span>
                <div class="product-sub-meta">
                  <span class="meta-badge-chip">Kho ERP</span>
                  <span v-if="row.updatedAt" class="meta-date-txt">• Cập nhật {{ formatShortDate(row.updatedAt) }}</span>
                </div>
              </div>
            </template>
          </el-table-column>

          <el-table-column prop="sku" label="Mã SKU" width="135">
            <template #default="{ row }">
              <span class="sku-pill-badge">{{ row.sku }}</span>
            </template>
          </el-table-column>

          <el-table-column prop="price" label="Giá bán" width="145" align="right">
            <template #default="{ row }">
              <span class="price-tag">{{ formatCurrency(row.price) }}</span>
            </template>
          </el-table-column>

          <el-table-column prop="stock" label="Tồn kho" width="165" align="center">
            <template #default="{ row }">
              <span class="stock-status-pill" :class="getStockClass(row.stock)">
                <span class="stock-dot"></span>
                <span>{{ getStockLabel(row.stock) }}</span>
              </span>
            </template>
          </el-table-column>

          <el-table-column label="Thao tác" width="135" align="center" fixed="right">
            <template #default="{ row }">
              <div v-if="authStore.isAdmin" class="action-btn-group">
                <button type="button" class="btn-row-edit" @click="openDialog(row)">Sửa</button>
                <button type="button" class="btn-row-delete" @click="deleteProduct(row.id)">Xóa</button>
              </div>
              <span v-else class="view-only-pill">Chỉ xem</span>
            </template>
          </el-table-column>

          <template #empty>
            <div class="empty-state-box">
              <span class="empty-emoji">📦</span>
              <p>Không tìm thấy sản phẩm nào khớp với điều kiện tìm kiếm</p>
            </div>
          </template>
        </el-table>
      </div>

      <!-- DANH SÁCH THẺ MOBILE (<= 768px) -> KHÔNG BỊ TRÀN NGANG TRÊN MÀN HÌNH NHỎ -->
      <div class="mobile-cards-wrapper">
        <div v-if="products.length === 0 && !loading" class="empty-state-box">
          <span class="empty-emoji">📦</span>
          <p>Không tìm thấy sản phẩm nào</p>
        </div>

        <div
          v-for="row in products"
          :key="row.id"
          class="mobile-product-card"
        >
          <div class="mobile-card-top">
            <div class="mobile-card-thumb-wrap">
              <el-image
                v-if="row.imageUrl"
                :src="row.imageUrl"
                :preview-src-list="[row.imageUrl]"
                preview-teleported
                fit="cover"
                class="mobile-thumb-img"
              >
                <template #error>
                  <div class="mobile-thumb-placeholder"><el-icon><Picture /></el-icon></div>
                </template>
              </el-image>
              <div v-else class="mobile-thumb-placeholder">
                <el-icon><Picture /></el-icon>
              </div>
            </div>

            <div class="mobile-card-info">
              <div class="mobile-card-title-row">
                <h4 class="mobile-product-name">{{ row.name }}</h4>
                <span class="mobile-id-badge">#{{ row.id }}</span>
              </div>
              <div class="mobile-sku-row">
                <span class="sku-pill-badge">{{ row.sku }}</span>
                <span class="stock-status-pill" :class="getStockClass(row.stock)">
                  <span class="stock-dot"></span>
                  <span>{{ getStockLabel(row.stock) }}</span>
                </span>
              </div>
            </div>
          </div>

          <div class="mobile-card-bottom">
            <span class="price-tag">{{ formatCurrency(row.price) }}</span>
            <div v-if="authStore.isAdmin" class="action-btn-group">
              <button type="button" class="btn-row-edit" @click="openDialog(row)">Sửa</button>
              <button type="button" class="btn-row-delete" @click="deleteProduct(row.id)">Xóa</button>
            </div>
            <span v-else class="view-only-pill">Chỉ xem</span>
          </div>
        </div>
      </div>

      <!-- FOOTER PHÂN TRANG -->
      <div class="table-pagination-footer">
        <div class="pagination-summary">
          Hiển thị <strong>{{ products.length }}</strong> trên tổng số <strong>{{ totalElements }}</strong> sản phẩm
        </div>
        <el-pagination
          v-model:current-page="currentPageModel"
          v-model:page-size="size"
          :page-sizes="[10, 15, 25, 50]"
          :total="totalElements"
          :layout="isMobileView ? 'prev, pager, next' : 'sizes, prev, pager, next'"
          background
          small
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
    </div>

    <!-- MODAL THÊM / CẬP NHẬT SẢN PHẨM MỚI (CHUẨN SÁT ẢNH THIẾT KẾ) -->
    <Teleport to="body">
      <div v-if="dialogVisible" class="product-modal-overlay" @click.self="dialogVisible = false">
        <div class="product-modal-card">
          <!-- Modal Header -->
          <div class="pm-header">
            <div class="pm-header-left">
              <div class="pm-title-row">
                <h3 class="pm-title">{{ form.id ? 'Cập nhật Sản phẩm' : 'Thêm Sản phẩm Mới' }}</h3>
                <span class="pm-sku-tag">{{ form.id ? `#${form.id}` : 'SKU MỚI' }}</span>
              </div>
              <p class="pm-subtitle">
                {{ form.id ? 'Chỉnh sửa thông tin, giá niêm yết và tồn kho sản phẩm' : 'Nhập thông tin sản phẩm mới để lưu trữ vào cơ sở dữ liệu hệ thống' }}
              </p>
            </div>
            <button type="button" class="pm-close-btn" @click="dialogVisible = false">✕</button>
          </div>

          <!-- Modal Body -->
          <form @submit.prevent="saveProduct" class="pm-form">
            <div class="pm-body-scroll">
              <!-- Image Upload Box -->
              <div class="pm-field-block">
                <div class="pm-label-row">
                  <label class="pm-label">Hình ảnh sản phẩm</label>
                  <span class="pm-label-hint">Tối đa 1 ảnh chính • Tối đa 5MB</span>
                </div>

                <div class="pm-upload-zone">
                  <el-upload
                    class="pm-el-upload"
                    drag
                    action="#"
                    :http-request="handleCustomUpload"
                    :show-file-list="false"
                    accept=".jpg,.jpeg,.png,.webp,.gif"
                  >
                    <div v-if="!uploading" class="pm-upload-inner">
                      <div class="pm-cloud-circle">
                        <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="2">
                          <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path>
                          <polyline points="17 8 12 3 7 8"></polyline>
                          <line x1="12" y1="3" x2="12" y2="15"></line>
                        </svg>
                      </div>
                      <div class="pm-upload-text">
                        Kéo thả ảnh vào đây hoặc <span class="pm-link-blue">chọn file</span>
                      </div>
                      <div class="pm-upload-sub">JPG, PNG, WEBP tối đa 5MB (Lưu trữ Cloudinary an toàn)</div>
                    </div>
                    <div v-else class="pm-upload-loading">
                      <el-icon class="is-loading" style="font-size: 24px;"><Loading /></el-icon>
                      <span>Đang tải ảnh lên Cloud...</span>
                    </div>
                  </el-upload>

                  <!-- Preview file đã upload nằm ngay trong khung upload -->
                  <div v-if="form.imageUrl" class="pm-uploaded-file-card">
                    <img :src="form.imageUrl" alt="Preview" class="pm-preview-thumb" />
                    <div class="pm-file-meta">
                      <span class="pm-file-name">{{ getImageFileName(form.imageUrl) }}</span>
                      <span class="pm-file-status">✓ Đã tải lên • Sẵn sàng lưu</span>
                    </div>
                    <button
                      type="button"
                      class="pm-remove-img-btn"
                      title="Xóa ảnh này"
                      @click="form.imageUrl = ''"
                    >
                      <el-icon><Delete /></el-icon>
                    </button>
                  </div>
                </div>
              </div>

              <!-- Row 1: Tên sản phẩm & Mã SKU -->
              <div class="pm-grid-2col">
                <div class="pm-field-block">
                  <label class="pm-label">Tên sản phẩm <span class="req-star">*</span></label>
                  <input
                    v-model="form.name"
                    type="text"
                    required
                    placeholder="Ví dụ: Laptop Dell XPS 15 Core i7"
                    class="pm-text-input"
                  />
                </div>

                <div class="pm-field-block">
                  <div class="pm-label-row">
                    <label class="pm-label">Mã SKU <span class="req-star">*</span></label>
                    <button type="button" class="pm-gen-sku-btn" @click="regenerateSku">
                      🔄 Tự sinh mã
                    </button>
                  </div>
                  <div class="pm-sku-input-wrap">
                    <input
                      v-model="form.sku"
                      type="text"
                      required
                      placeholder="SKU-0001"
                      class="pm-text-input pm-sku-input"
                    />
                    <span class="pm-auto-tag">Tự động</span>
                  </div>
                </div>
              </div>

              <!-- Row 2: Đơn giá (VNĐ) & Số lượng tồn kho -->
              <div class="pm-grid-2col">
                <div class="pm-field-block">
                  <label class="pm-label">Đơn giá (VNĐ) <span class="req-star">*</span></label>
                  <div class="pm-stepper-box">
                    <button type="button" class="pm-step-btn" @click="adjustPrice(-10000)">−</button>
                    <input
                      v-model.number="form.price"
                      type="number"
                      min="0"
                      step="1000"
                      required
                      class="pm-step-input"
                    />
                    <button type="button" class="pm-step-btn" @click="adjustPrice(10000)">+</button>
                  </div>
                  <span class="pm-helper-text">
                    Bằng chữ: <strong>{{ numberToVietnameseWords(form.price) }}</strong>
                  </span>
                </div>

                <div class="pm-field-block">
                  <label class="pm-label">Số lượng tồn kho <span class="req-star">*</span></label>
                  <div class="pm-stepper-box">
                    <button type="button" class="pm-step-btn" @click="adjustStock(-1)">−</button>
                    <input
                      v-model.number="form.stock"
                      type="number"
                      min="0"
                      required
                      class="pm-step-input"
                    />
                    <button type="button" class="pm-step-btn" @click="adjustStock(1)">+</button>
                  </div>
                  <span class="pm-helper-text" :class="stockSafeHintClass">
                    {{ stockSafeHintText }}
                  </span>
                </div>
              </div>

              <!-- Box đồng bộ hệ thống -->
              <div class="pm-sync-banner">
                <div class="pm-sync-left">
                  <span class="pm-shield-badge">🛡️</span>
                  <div>
                    <div class="pm-sync-title">Tự động đồng bộ Realtime</div>
                    <div class="pm-sync-sub">Dữ liệu sẽ cập nhật ngay vào phân hệ Kho và Hệ thống ERP</div>
                  </div>
                </div>
                <span class="pm-sys-id">{{ form.id ? `ID: #${form.id}` : 'ID: #SYS-NEW' }}</span>
              </div>
            </div>

            <!-- Modal Footer -->
            <div class="pm-footer">
              <div class="pm-footer-status">
                <span class="status-dot-green"></span>
                <span>Sẵn sàng lưu kho</span>
              </div>
              <div class="pm-footer-actions">
                <button type="button" class="pm-btn-cancel" @click="dialogVisible = false">Hủy bỏ</button>
                <button type="submit" class="pm-btn-submit" :disabled="saving">
                  <span>{{ saving ? 'Đang lưu...' : '✓ Lưu sản phẩm' }}</span>
                </button>
              </div>
            </div>
          </form>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onBeforeUnmount } from 'vue'
import { useAuthStore } from '../store/auth'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Picture, Delete, Loading, Search, RefreshRight } from '@element-plus/icons-vue'
import api from '../api'

const authStore = useAuthStore()

// =========================================================================
//               HELPER FUNCTIONS (ĐẶT ĐẦU FILE TRÁNH TDZ)
// =========================================================================
const formatCurrency = (val) => {
  if (val == null) return '0 ₫'
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(val)
}

const formatCompactCurrency = (val) => {
  const num = Number(val) || 0
  if (num >= 1_000_000_000) {
    return (num / 1_000_000_000).toFixed(2).replace(/\.00$/, '') + ' tỷ'
  }
  if (num >= 1_000_000) {
    return (num / 1_000_000).toFixed(1).replace(/\.0$/, '') + ' tr'
  }
  return new Intl.NumberFormat('vi-VN').format(num) + ' ₫'
}

const formatShortDate = (dateStr) => {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  if (Number.isNaN(d.getTime())) return ''
  const pad = (n) => String(n).padStart(2, '0')
  return `${pad(d.getDate())}/${pad(d.getMonth() + 1)}`
}

const getImageFileName = (url) => {
  if (!url) return 'product-image'
  const parts = url.split('/')
  const last = parts[parts.length - 1] || 'product-image'
  return last.length > 34 ? last.substring(0, 31) + '...' : last
}

const numberToVietnameseWords = (numVal) => {
  const n = Math.floor(Number(numVal) || 0)
  if (n <= 0) return 'Không đồng'
  const digits = ['không', 'một', 'hai', 'ba', 'bốn', 'năm', 'sáu', 'bảy', 'tám', 'chín']
  const units = ['', 'nghìn', 'triệu', 'tỷ', 'nghìn tỷ']

  const readBlock = (block, showZeroHundred) => {
    const tram = Math.floor(block / 100)
    const chuc = Math.floor((block % 100) / 10)
    const donvi = block % 10
    let res = ''
    if (tram > 0 || showZeroHundred) {
      res += digits[tram] + ' trăm '
    }
    if (chuc > 1) {
      res += digits[chuc] + ' mươi '
      if (donvi === 1) res += 'mốt '
      else if (donvi === 5) res += 'lăm '
      else if (donvi > 0) res += digits[donvi] + ' '
    } else if (chuc === 1) {
      res += 'mười '
      if (donvi === 5) res += 'lăm '
      else if (donvi > 0) res += digits[donvi] + ' '
    } else if (donvi > 0) {
      if (tram > 0 || showZeroHundred) res += 'lẻ '
      res += digits[donvi] + ' '
    }
    return res
  }

  let temp = n
  const blocks = []
  while (temp > 0) {
    blocks.push(temp % 1000)
    temp = Math.floor(temp / 1000)
  }

  let words = ''
  for (let i = blocks.length - 1; i >= 0; i--) {
    if (blocks[i] > 0) {
      const showHundred = i < blocks.length - 1
      words += readBlock(blocks[i], showHundred) + (units[i] ? units[i] + ' ' : '')
    }
  }
  const clean = words.trim()
  if (!clean) return 'Không đồng'
  return clean.charAt(0).toUpperCase() + clean.slice(1) + ' đồng'
}

// =========================================================================
//                               STATE & REFS
// =========================================================================
const tableRef = ref(null)
const products = ref([])
const totalElements = ref(0)
const totalPages = ref(0)
const page = ref(0)
const size = ref(15)
const loading = ref(false)
const saving = ref(false)
const uploading = ref(false)
const batchDeleting = ref(false)
const dialogVisible = ref(false)
const selectedProductIds = ref([])
const windowWidth = ref(typeof window !== 'undefined' ? window.innerWidth : 1280)

const filters = reactive({
  keyword: '',
  stockStatus: 'ALL',
  sort: 'id,desc'
})

const form = ref({
  id: null,
  name: '',
  sku: '',
  price: 100000,
  stock: 10,
  imageUrl: ''
})

// =========================================================================
//                           COMPUTED PROPERTIES
// =========================================================================
const isMobileView = computed(() => windowWidth.value <= 768)

const currentPageModel = computed({
  get: () => page.value + 1,
  set: (val) => {
    page.value = Math.max(0, val - 1)
  }
})

const inStockCount = computed(() => {
  return products.value.filter(p => (p.stock ?? 0) > 10).length
})

const inStockRatio = computed(() => {
  if (products.value.length === 0) return 0
  return Math.round((inStockCount.value / products.value.length) * 1000) / 10
})

const lowOrOutStockCount = computed(() => {
  return products.value.filter(p => (p.stock ?? 0) <= 10).length
})

const totalInventoryValue = computed(() => {
  return products.value.reduce((sum, p) => sum + (Number(p.price) || 0) * (Number(p.stock) || 0), 0)
})

const stockSafeHintClass = computed(() => {
  const s = Number(form.value.stock) ?? 0
  if (s === 0) return 'hint-danger'
  if (s <= 5) return 'hint-warn'
  return 'hint-safe'
})

const stockSafeHintText = computed(() => {
  const s = Number(form.value.stock) ?? 0
  if (s === 0) return '⚠️ Cảnh báo: Sản phẩm đang hết hàng (0)'
  if (s <= 5) return '⚠️ Sắp chạm ngưỡng an toàn (<= 5 sản phẩm)'
  return 'Định mức an toàn: > 5 sản phẩm'
})

// =========================================================================
//                             METHODS & API
// =========================================================================
const getStockClass = (stock) => {
  const s = stock ?? 0
  if (s > 10) return 'stock-green'
  if (s > 0) return 'stock-amber'
  return 'stock-red'
}

const getStockLabel = (stock) => {
  const s = stock ?? 0
  if (s > 10) return `${s} cái (Còn hàng)`
  if (s > 0) return `${s} cái (Sắp hết)`
  return '0 cái (Hết hàng)'
}

const fetchProducts = async () => {
  loading.value = true
  try {
    const [sortBy, sortDir] = filters.sort.split(',')
    const params = {
      page: page.value,
      size: size.value,
      keyword: filters.keyword ? filters.keyword.trim() : undefined,
      stockStatus: filters.stockStatus !== 'ALL' ? filters.stockStatus : undefined,
      sortBy: sortBy || 'id',
      sortDir: sortDir || 'desc'
    }

    const res = await api.get('/products', { params })
    if (res.data && res.data.content !== undefined) {
      products.value = res.data.content
      totalElements.value = res.data.totalElements ?? 0
      totalPages.value = res.data.totalPages ?? 0
    } else if (Array.isArray(res.data)) {
      products.value = res.data
      totalElements.value = res.data.length
    }
  } catch (e) {
    ElMessage.error('Không thể tải danh sách sản phẩm!')
  } finally {
    loading.value = false
  }
}

const onSearch = () => {
  page.value = 0
  fetchProducts()
}

const onFilterChange = () => {
  page.value = 0
  fetchProducts()
}

const resetFilters = () => {
  filters.keyword = ''
  filters.stockStatus = 'ALL'
  filters.sort = 'id,desc'
  page.value = 0
  fetchProducts()
}

const handlePageChange = (newPage) => {
  page.value = Math.max(0, newPage - 1)
  fetchProducts()
}

const handleSizeChange = (newSize) => {
  size.value = newSize
  page.value = 0
  fetchProducts()
}

const handleSelectionChange = (selection) => {
  selectedProductIds.value = selection.map(item => item.id)
}

const clearSelection = () => {
  selectedProductIds.value = []
  if (tableRef.value && tableRef.value.clearSelection) {
    tableRef.value.clearSelection()
  }
}

const handleBatchDelete = async () => {
  if (selectedProductIds.value.length === 0) return
  try {
    await ElMessageBox.confirm(
      `Bạn có chắc chắn muốn xóa vĩnh viễn ${selectedProductIds.value.length} sản phẩm đã chọn?`,
      'Xác nhận xóa hàng loạt',
      { confirmButtonText: 'Xóa ngay', cancelButtonText: 'Hủy', type: 'warning' }
    )
    batchDeleting.value = true
    await api.post('/products/batch-delete', { ids: selectedProductIds.value })
    ElMessage.success(`Đã xóa ${selectedProductIds.value.length} sản phẩm thành công!`)
    selectedProductIds.value = []
    fetchProducts()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error(e.response?.data?.message || 'Lỗi khi xóa hàng loạt sản phẩm!')
    }
  } finally {
    batchDeleting.value = false
  }
}

const regenerateSku = () => {
  form.value.sku = 'SKU-' + Math.floor(1000 + Math.random() * 9000)
}

const adjustPrice = (delta) => {
  const next = (Number(form.value.price) || 0) + delta
  form.value.price = Math.max(0, next)
}

const adjustStock = (delta) => {
  const next = (Number(form.value.stock) || 0) + delta
  form.value.stock = Math.max(0, next)
}

const openDialog = (row = null) => {
  if (row) {
    form.value = {
      id: row.id,
      name: row.name || '',
      sku: row.sku || '',
      price: row.price ?? 0,
      stock: row.stock ?? 0,
      imageUrl: row.imageUrl || ''
    }
  } else {
    form.value = {
      id: null,
      name: '',
      sku: 'SKU-' + Math.floor(1000 + Math.random() * 9000),
      price: 100000,
      stock: 10,
      imageUrl: ''
    }
  }
  dialogVisible.value = true
}

const handleCustomUpload = async ({ file, onSuccess, onError }) => {
  uploading.value = true
  const formData = new FormData()
  formData.append('file', file)
  try {
    const res = await api.post('/files/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    form.value.imageUrl = res.data.url
    ElMessage.success('Tải ảnh lên Cloudinary thành công!')
    onSuccess(res.data)
  } catch (err) {
    ElMessage.error(err.response?.data?.message || 'Lỗi khi tải ảnh lên Cloud!')
    onError(err)
  } finally {
    uploading.value = false
  }
}

const saveProduct = async () => {
  if (!form.value.name.trim() || !form.value.sku.trim()) {
    ElMessage.warning('Vui lòng nhập Tên sản phẩm và Mã SKU!')
    return
  }
  saving.value = true
  try {
    if (form.value.id) {
      await api.put(`/products/${form.value.id}`, form.value)
      ElMessage.success('Cập nhật sản phẩm thành công!')
    } else {
      await api.post('/products', form.value)
      ElMessage.success('Tạo mới sản phẩm thành công!')
    }
    dialogVisible.value = false
    fetchProducts()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || 'Lỗi khi lưu sản phẩm!')
  } finally {
    saving.value = false
  }
}

const deleteProduct = async (id) => {
  try {
    await ElMessageBox.confirm('Bạn có chắc chắn muốn xóa sản phẩm này?', 'Xác nhận xóa', {
      confirmButtonText: 'Xóa ngay',
      cancelButtonText: 'Hủy',
      type: 'warning'
    })
    await api.delete(`/products/${id}`)
    ElMessage.success('Đã xóa sản phẩm thành công!')
    fetchProducts()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('Lỗi khi xóa sản phẩm!')
  }
}

const handleExportExcel = () => {
  if (products.value.length === 0) {
    ElMessage.warning('Không có dữ liệu sản phẩm để xuất!')
    return
  }
  const headers = ['ID', 'Tên sản phẩm', 'Mã SKU', 'Đơn giá (VNĐ)', 'Tồn kho']
  const rows = products.value.map(p => [
    p.id,
    `"${(p.name || '').replace(/"/g, '""')}"`,
    `"${(p.sku || '').replace(/"/g, '""')}"`,
    p.price ?? 0,
    p.stock ?? 0
  ])
  const csvContent = '\uFEFF' + [headers.join(','), ...rows.map(r => r.join(','))].join('\n')
  const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.setAttribute('download', `danh-sach-san-pham-erp.csv`)
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
  ElMessage.success('📥 Đã xuất danh sách sản phẩm ra file Excel (.csv)!')
}

const handleWindowResize = () => {
  windowWidth.value = window.innerWidth
}

onMounted(() => {
  window.addEventListener('resize', handleWindowResize)
  fetchProducts()
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleWindowResize)
})
</script>

<style scoped>
.products-container {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 0 0 64px;
  background: #f8fafc;
}

/* HEADER CARD */
.products-header-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 10px 14px;
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 11px;
  padding: 10px 14px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.03);
}

.header-left-col {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1 1 300px;
  min-width: 0;
}

.header-icon-box {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  background: linear-gradient(135deg, #2563eb, #1d4ed8);
  color: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  box-shadow: 0 4px 10px rgba(37, 99, 235, 0.25);
}

.header-title-block {
  min-width: 0;
}

.title-with-pill {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.header-title {
  margin: 0;
  font-size: 17px;
  font-weight: 700;
  color: #0f172a;
  white-space: nowrap;
}

.version-pill-badge {
  font-size: 11px;
  font-weight: 700;
  color: #2563eb;
  background: #eff6ff;
  border: 1px solid #bfdbfe;
  padding: 1px 8px;
  border-radius: 12px;
  white-space: nowrap;
}

.header-subtitle {
  margin: 2px 0 0;
  font-size: 12.5px;
  color: #64748b;
}

.header-right-col {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.btn-header-outline {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 36px;
  padding: 0 14px;
  background: #ffffff;
  color: #334155;
  border: 1px solid #cbd5e1;
  border-radius: 9px;
  font-size: 13px;
  font-weight: 600;
  white-space: nowrap;
  cursor: pointer;
  transition: all 0.18s ease;
}

.btn-header-outline:hover {
  background: #f8fafc;
  border-color: #94a3b8;
  color: #0f172a;
}

.btn-header-primary {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 36px;
  padding: 0 16px;
  background: linear-gradient(135deg, #2563eb, #1d4ed8);
  color: #ffffff;
  border: none;
  border-radius: 9px;
  font-size: 13px;
  font-weight: 600;
  white-space: nowrap;
  cursor: pointer;
  box-shadow: 0 3px 8px rgba(37, 99, 235, 0.25);
  transition: all 0.18s ease;
}

.btn-header-primary:hover {
  background: linear-gradient(135deg, #1d4ed8, #1e40af);
  transform: translateY(-1px);
}

/* 4 KPI CARDS */
.products-metrics-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}

.kpi-stat-card {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  padding: 9px 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.02);
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
  font-weight: 600;
}

.text-green { color: #059669; }
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
.icon-teal { background: #ecfdf5; color: #059669; }
.icon-red { background: #fef2f2; color: #dc2626; }
.icon-indigo { background: #eef2ff; color: #4f46e5; }

/* FILTER BAR */
.products-filter-card {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  padding: 8px 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 8px;
}

.filter-left-group {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
  flex: 1 1 560px;
}

.filter-search-input {
  width: 240px;
}

.filter-select-box {
  width: 175px;
}

.btn-filter-reset {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  height: 32px;
  padding: 0 12px;
  background: #f8fafc;
  color: #475569;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  font-size: 12.5px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s ease;
}

.btn-filter-reset:hover {
  background: #f1f5f9;
  color: #0f172a;
}

.filter-right-count {
  font-size: 13px;
  color: #475569;
  white-space: nowrap;
}

/* BATCH BAR */
.batch-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 16px;
  background: #fef2f2;
  border: 1px solid #fecaca;
  border-radius: 10px;
  color: #991b1b;
  font-size: 13.5px;
}

.batch-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.batch-icon {
  color: #ef4444;
  font-size: 18px;
}

.batch-actions {
  display: flex;
  gap: 8px;
}

/* TABLE CARD */
.products-table-card {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.03);
}

:deep(.erp-table-header th) {
  background: #f8fafc !important;
  color: #475569 !important;
  font-size: 11.5px !important;
  font-weight: 700 !important;
  text-transform: uppercase;
  padding: 8px 0 !important;
}

.products-table-card :deep(.el-table__cell) {
  padding: 6px 0 !important;
}

.id-text-cell {
  font-weight: 600;
  color: #64748b;
}

.product-thumb {
  width: 34px;
  height: 34px;
  border-radius: 7px;
  border: 1px solid #e2e8f0;
  display: inline-block;
  vertical-align: middle;
}

.image-placeholder {
  width: 34px;
  height: 34px;
  border-radius: 7px;
  background-color: #f1f5f9;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #94a3b8;
  font-size: 15px;
  border: 1px dashed #cbd5e1;
}

.product-name-cell {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.product-name {
  font-weight: 600;
  font-size: 13.5px;
  color: #0f172a;
}

.product-sub-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 11.5px;
  color: #64748b;
}

.meta-badge-chip {
  background: #eff6ff;
  color: #2563eb;
  padding: 1px 6px;
  border-radius: 4px;
  font-weight: 600;
  font-size: 10.5px;
}

.sku-pill-badge {
  display: inline-block;
  font-family: monospace;
  font-size: 11.5px;
  font-weight: 700;
  color: #334155;
  background: #f1f5f9;
  border: 1px solid #e2e8f0;
  padding: 2px 8px;
  border-radius: 6px;
}

.price-tag {
  font-weight: 700;
  font-size: 13.5px;
  color: #1d4ed8;
}

.stock-status-pill {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 3px 10px;
  border-radius: 12px;
  font-size: 11.5px;
  font-weight: 600;
}

.stock-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
}

.stock-green { background: #ecfdf5; color: #047857; }
.stock-green .stock-dot { background: #10b981; }
.stock-amber { background: #fffbeb; color: #b45309; }
.stock-amber .stock-dot { background: #f59e0b; }
.stock-red { background: #fef2f2; color: #b91c1c; }
.stock-red .stock-dot { background: #ef4444; }

.action-btn-group {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.btn-row-edit {
  padding: 4px 10px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 600;
  border: 1px solid #cbd5e1;
  background: #ffffff;
  color: #334155;
  cursor: pointer;
}

.btn-row-edit:hover {
  background: #f1f5f9;
  color: #0f172a;
}

.btn-row-delete {
  padding: 4px 10px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 600;
  border: 1px solid #fecaca;
  background: #fef2f2;
  color: #dc2626;
  cursor: pointer;
}

.btn-row-delete:hover {
  background: #fee2e2;
}

.view-only-pill {
  font-size: 11.5px;
  color: #64748b;
  background: #f1f5f9;
  padding: 2px 8px;
  border-radius: 6px;
}

.empty-state-box {
  padding: 40px 16px;
  text-align: center;
  color: #94a3b8;
  font-size: 13px;
}

.empty-emoji {
  font-size: 32px;
  display: block;
  margin-bottom: 6px;
}

.table-pagination-footer {
  padding: 12px 16px;
  border-top: 1px solid #f1f5f9;
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 10px;
}

.pagination-summary {
  font-size: 12.5px;
  color: #64748b;
}

/* MOBILE PRODUCT CARDS */
.mobile-cards-wrapper {
  display: none;
  flex-direction: column;
  gap: 10px;
  padding: 12px;
}

.mobile-product-card {
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  background: #ffffff;
}

.mobile-card-top {
  display: flex;
  gap: 12px;
  align-items: flex-start;
}

.mobile-thumb-img {
  width: 56px;
  height: 56px;
  border-radius: 8px;
  border: 1px solid #e2e8f0;
  flex-shrink: 0;
}

.mobile-thumb-placeholder {
  width: 56px;
  height: 56px;
  border-radius: 8px;
  background: #f1f5f9;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #94a3b8;
  border: 1px dashed #cbd5e1;
  flex-shrink: 0;
}

.mobile-card-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.mobile-card-title-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 8px;
}

.mobile-product-name {
  margin: 0;
  font-size: 14px;
  font-weight: 700;
  color: #0f172a;
  word-break: break-word;
}

.mobile-id-badge {
  font-size: 11px;
  font-weight: 700;
  color: #64748b;
  background: #f1f5f9;
  padding: 1px 6px;
  border-radius: 4px;
  flex-shrink: 0;
}

.mobile-sku-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
}

.mobile-card-bottom {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: 8px;
  border-top: 1px solid #f1f5f9;
}

/* MODAL THÊM / SỬA SẢN PHẨM (CHUẨN ẢNH 1) */
.product-modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.55);
  backdrop-filter: blur(4px);
  z-index: 1050;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px;
}

.product-modal-card {
  background: #ffffff;
  width: 100%;
  max-width: 600px;
  max-height: 92vh;
  border-radius: 16px;
  box-shadow: 0 24px 48px rgba(0, 0, 0, 0.2);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.pm-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  padding: 18px 22px 14px;
  border-bottom: 1px solid #f1f5f9;
}

.pm-title-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.pm-title {
  margin: 0;
  font-size: 18px;
  font-weight: 800;
  color: #0f172a;
}

.pm-sku-tag {
  font-size: 10.5px;
  font-weight: 700;
  color: #2563eb;
  background: #eff6ff;
  border: 1px solid #bfdbfe;
  padding: 2px 8px;
  border-radius: 6px;
}

.pm-subtitle {
  margin: 4px 0 0;
  font-size: 12.5px;
  color: #64748b;
}

.pm-close-btn {
  width: 30px;
  height: 30px;
  border-radius: 8px;
  border: none;
  background: #f8fafc;
  color: #64748b;
  font-size: 14px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.pm-close-btn:hover {
  background: #f1f5f9;
  color: #0f172a;
}

.pm-form {
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.pm-body-scroll {
  padding: 16px 22px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.pm-field-block {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.pm-label-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.pm-label {
  font-size: 12.5px;
  font-weight: 700;
  color: #1e293b;
}

.req-star {
  color: #ef4444;
}

.pm-label-hint {
  font-size: 11.5px;
  color: #64748b;
}

.pm-upload-zone {
  border: 1.5px dashed #93c5fd;
  background: #f8fbff;
  border-radius: 12px;
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

:deep(.pm-el-upload .el-upload) {
  width: 100%;
}

:deep(.pm-el-upload .el-upload-dragger) {
  width: 100%;
  border: none !important;
  background: transparent !important;
  padding: 10px !important;
}

.pm-upload-inner {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
}

.pm-cloud-circle {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: #dbeafe;
  color: #2563eb;
  display: flex;
  align-items: center;
  justify-content: center;
}

.pm-upload-text {
  font-size: 13px;
  font-weight: 600;
  color: #1e293b;
}

.pm-link-blue {
  color: #2563eb;
  text-decoration: underline;
}

.pm-upload-sub {
  font-size: 11.5px;
  color: #64748b;
}

.pm-upload-loading {
  padding: 16px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  color: #2563eb;
  font-size: 13px;
}

.pm-uploaded-file-card {
  display: flex;
  align-items: center;
  gap: 10px;
  background: #ffffff;
  border: 1px solid #dbeafe;
  border-radius: 10px;
  padding: 8px 12px;
}

.pm-preview-thumb {
  width: 40px;
  height: 40px;
  border-radius: 6px;
  object-fit: cover;
  border: 1px solid #e2e8f0;
  flex-shrink: 0;
}

.pm-file-meta {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.pm-file-name {
  font-size: 12.5px;
  font-weight: 700;
  color: #0f172a;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.pm-file-status {
  font-size: 11px;
  color: #059669;
  font-weight: 600;
}

.pm-remove-img-btn {
  width: 28px;
  height: 28px;
  border-radius: 6px;
  border: none;
  background: #fef2f2;
  color: #ef4444;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
}

.pm-remove-img-btn:hover {
  background: #fee2e2;
}

.pm-grid-2col {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
}

.pm-text-input {
  height: 38px;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  padding: 0 12px;
  font-size: 13.5px;
  color: #0f172a;
  background: #f8fafc;
  outline: none;
  width: 100%;
  transition: all 0.15s ease;
}

.pm-text-input:focus {
  border-color: #2563eb;
  background: #ffffff;
  box-shadow: 0 0 0 2px rgba(37, 99, 235, 0.12);
}

.pm-gen-sku-btn {
  border: none;
  background: none;
  color: #2563eb;
  font-size: 11.5px;
  font-weight: 700;
  cursor: pointer;
  padding: 0;
}

.pm-gen-sku-btn:hover {
  text-decoration: underline;
}

.pm-sku-input-wrap {
  position: relative;
  display: flex;
  align-items: center;
}

.pm-sku-input {
  padding-right: 72px;
  font-family: monospace;
  font-weight: 700;
}

.pm-auto-tag {
  position: absolute;
  right: 8px;
  font-size: 10.5px;
  font-weight: 600;
  color: #2563eb;
  background: #eff6ff;
  border: 1px solid #bfdbfe;
  padding: 2px 6px;
  border-radius: 4px;
  pointer-events: none;
}

.pm-stepper-box {
  display: flex;
  align-items: center;
  height: 38px;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  background: #f8fafc;
  overflow: hidden;
}

.pm-step-btn {
  width: 36px;
  height: 100%;
  border: none;
  background: #f1f5f9;
  color: #334155;
  font-size: 16px;
  font-weight: 700;
  cursor: pointer;
  flex-shrink: 0;
}

.pm-step-btn:hover {
  background: #e2e8f0;
}

.pm-step-input {
  flex: 1;
  min-width: 0;
  border: none;
  background: transparent;
  text-align: center;
  font-size: 13.5px;
  font-weight: 700;
  color: #0f172a;
  outline: none;
}

.pm-helper-text {
  font-size: 11.5px;
  color: #64748b;
}

.hint-safe { color: #059669; font-weight: 600; }
.hint-warn { color: #d97706; font-weight: 600; }
.hint-danger { color: #dc2626; font-weight: 600; }

.pm-sync-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  background: #f0f9ff;
  border: 1px solid #bae6fd;
  border-radius: 10px;
  padding: 10px 14px;
}

.pm-sync-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.pm-shield-badge {
  font-size: 18px;
}

.pm-sync-title {
  font-size: 12.5px;
  font-weight: 700;
  color: #0369a1;
}

.pm-sync-sub {
  font-size: 11.5px;
  color: #0284c7;
}

.pm-sys-id {
  font-family: monospace;
  font-size: 11.5px;
  font-weight: 700;
  color: #64748b;
  white-space: nowrap;
}

.pm-footer {
  padding: 14px 22px;
  border-top: 1px solid #f1f5f9;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  background: #ffffff;
}

.pm-footer-status {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  font-weight: 600;
  color: #475569;
}

.status-dot-green {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #10b981;
}

.pm-footer-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.pm-btn-cancel {
  height: 36px;
  padding: 0 16px;
  border-radius: 8px;
  border: 1px solid #cbd5e1;
  background: #ffffff;
  color: #475569;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
}

.pm-btn-cancel:hover {
  background: #f8fafc;
}

.pm-btn-submit {
  height: 36px;
  padding: 0 18px;
  border-radius: 8px;
  border: none;
  background: linear-gradient(135deg, #2563eb, #1d4ed8);
  color: #ffffff;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  box-shadow: 0 3px 8px rgba(37, 99, 235, 0.25);
}

.pm-btn-submit:disabled {
  opacity: 0.65;
  cursor: not-allowed;
}

/* =========================================================================
   RESPONSIVE BREAKPOINTS (TABLET & MÀN HÌNH NHỎ MOBILE)
   ========================================================================= */
@media (max-width: 1100px) {
  .products-metrics-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .products-container {
    padding: 8px 10px 76px;
    gap: 10px;
  }

  .products-header-card {
    padding: 12px 14px;
  }

  .header-right-col {
    width: 100%;
  }

  .btn-header-outline,
  .btn-header-primary {
    flex: 1;
    justify-content: center;
  }

  .products-metrics-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 8px;
  }

  .kpi-stat-card {
    padding: 10px 12px;
  }

  .kpi-stat-value {
    font-size: 18px;
  }

  .kpi-stat-icon {
    width: 34px;
    height: 34px;
  }

  .filter-search-input,
  .filter-select-box {
    width: 100% !important;
  }

  .desktop-table-wrapper {
    display: none;
  }

  .mobile-cards-wrapper {
    display: flex;
  }

  .pm-grid-2col {
    grid-template-columns: 1fr;
  }

  .pm-sync-banner {
    flex-direction: column;
    align-items: flex-start;
  }

  .pm-footer {
    flex-direction: column;
    align-items: stretch;
  }

  .pm-footer-actions {
    width: 100%;
  }

  .pm-btn-cancel,
  .pm-btn-submit {
    flex: 1;
  }
}

@media (max-width: 480px) {
  .products-metrics-grid {
    grid-template-columns: 1fr;
  }
}
</style>
