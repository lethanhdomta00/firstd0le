<template>
  <div class="activity-logs-page">
    <!-- Compact Header -->
    <div class="page-header-bar">
      <div class="header-title-block">
        <span class="title-icon-badge">
          <el-icon><Document /></el-icon>
        </span>
        <div class="title-texts">
          <h1 class="page-title">Nhật ký Thao tác &amp; Kiểm toán</h1>
          <span class="page-subtitle">Theo dõi đăng nhập, thay đổi dữ liệu &amp; hành vi rủi ro</span>
        </div>
      </div>

      <div class="header-actions">
        <el-button size="small" class="action-btn" :loading="loading" @click="handleRefresh">
          <el-icon><Refresh /></el-icon>
          <span>Làm mới</span>
        </el-button>
        <el-button size="small" type="primary" class="action-btn" :loading="exporting" @click="handleExportExcel">
          <el-icon><Download /></el-icon>
          <span>Xuất Excel</span>
        </el-button>
      </div>
    </div>

    <!-- Compact KPI Strip -->
    <div class="kpi-grid">
      <div class="kpi-card">
        <div class="kpi-row">
          <span class="kpi-label">Thao tác hôm nay</span>
          <span class="kpi-sub-pill">Tổng: {{ stats.totalAll ?? 0 }}</span>
        </div>
        <div class="kpi-value text-blue">{{ stats.totalToday ?? 0 }}</div>
      </div>
      <div class="kpi-card">
        <div class="kpi-row">
          <span class="kpi-label">Nhạy cảm / Rủi ro</span>
          <span class="kpi-sub-pill">Xóa, Hủy, Lỗi</span>
        </div>
        <div class="kpi-value text-red">{{ stats.riskyToday ?? 0 }}</div>
      </div>
      <div class="kpi-card">
        <div class="kpi-row">
          <span class="kpi-label">Tài khoản hoạt động</span>
          <span class="kpi-sub-pill">Hôm nay</span>
        </div>
        <div class="kpi-value text-emerald">{{ stats.activeUsersToday ?? 0 }}</div>
      </div>
      <div class="kpi-card">
        <div class="kpi-row">
          <span class="kpi-label">Phân hệ top (7 ngày)</span>
          <span class="kpi-sub-pill">{{ stats.topEntityCount ?? 0 }} lượt</span>
        </div>
        <div class="kpi-value text-slate">{{ formatEntityTypeLabel(stats.topEntity) }}</div>
      </div>
    </div>

    <!-- Compact Single-Row Filter Bar -->
    <div class="filter-panel">
      <div class="filter-controls">
        <el-input
          v-model="filters.keyword"
          size="small"
          placeholder="Tìm TraceID, tài khoản, chứng từ..."
          clearable
          class="filter-input-search"
          @keyup.enter="handleSearch"
          @clear="handleSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>

        <el-select
          v-model="filters.entityType"
          size="small"
          placeholder="Phân hệ"
          clearable
          class="filter-select"
          @change="handleSearch"
        >
          <el-option v-for="item in entityTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>

        <el-select
          v-model="filters.action"
          size="small"
          placeholder="Hành động"
          clearable
          class="filter-select"
          @change="handleSearch"
        >
          <el-option v-for="item in actionOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>

        <el-date-picker
          v-model="filters.dateRange"
          size="small"
          type="daterange"
          range-separator="→"
          start-placeholder="Từ ngày"
          end-placeholder="Đến ngày"
          format="DD/MM/YYYY"
          class="filter-date-range"
          @change="handleSearch"
        />

        <div class="filter-btns">
          <el-button size="small" type="primary" plain class="action-btn" @click="handleSearch">
            <el-icon><Filter /></el-icon>
            <span>Lọc</span>
          </el-button>
          <el-button size="small" class="action-btn" @click="handleResetFilters">
            <span>Hôm nay</span>
          </el-button>
        </div>
      </div>
    </div>

    <!-- Table with Sticky Header (height fixed to viewport for internal scroll) -->
    <div class="table-panel">
      <el-table
        v-loading="loading"
        :data="logs"
        size="small"
        stripe
        height="calc(100vh - 305px)"
        style="width: 100%"
        empty-text="Không có bản ghi nhật ký nào trong khoảng thời gian đã chọn."
      >
        <el-table-column label="Thời gian" width="152">
          <template #default="{ row }">
            <span class="nowrap-mono text-slate-600">{{ formatDateTime(row.timestamp) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="Mã Trace" width="132">
          <template #default="{ row }">
            <button
              type="button"
              class="trace-pill"
              title="Nhấn để sao chép TraceID"
              @click="copyText(row.traceId)"
            >
              {{ row.traceId || 'N/A' }}
            </button>
          </template>
        </el-table-column>

        <el-table-column label="Tài khoản" min-width="145">
          <template #default="{ row }">
            <div class="user-cell">
              <span class="user-avatar">{{ getInitial(row.username) }}</span>
              <div class="user-meta">
                <span class="user-name">@{{ row.username }}</span>
                <span class="user-role">{{ row.userRole || 'USER' }}</span>
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="Hành động" width="128">
          <template #default="{ row }">
            <el-tag :type="getActionTagType(row.action)" size="small" effect="light" class="nowrap-tag">
              {{ formatActionLabel(row.action) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="Phân hệ & Đối tượng" min-width="155">
          <template #default="{ row }">
            <div class="entity-cell">
              <span class="entity-badge">{{ formatEntityTypeLabel(row.entityType) }}</span>
              <span v-if="row.entityCode || row.entityId" class="entity-code">
                {{ row.entityCode || ('#' + row.entityId) }}
              </span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="Nội dung / Chi tiết thao tác" min-width="240">
          <template #default="{ row }">
            <span class="reason-text" :title="row.reason">{{ row.reason || '—' }}</span>
          </template>
        </el-table-column>

        <el-table-column label="Địa chỉ IP" width="118">
          <template #default="{ row }">
            <span class="nowrap-mono text-slate-500">{{ row.ipAddress || '127.0.0.1' }}</span>
          </template>
        </el-table-column>

        <el-table-column label="Chi tiết" width="84" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openDetailModal(row)">
              Xem Diff
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-bar">
        <span class="total-info">Tổng cộng: <strong>{{ totalElements }}</strong> bản ghi</span>
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          size="small"
          :page-sizes="[15, 20, 50, 100]"
          :total="totalElements"
          layout="sizes, prev, pager, next"
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </div>

    <!-- Detail & Diff Modal -->
    <el-dialog
      v-model="detailVisible"
      title="Chi tiết Bản ghi Kiểm toán & So sánh Dữ liệu"
      width="780px"
      destroy-on-close
      class="audit-detail-dialog"
    >
      <div v-if="selectedLog" class="detail-body">
        <div class="detail-meta-grid">
          <div class="meta-item">
            <span class="meta-label">Mã Trace</span>
            <span class="meta-value mono">{{ selectedLog.traceId }}</span>
          </div>
          <div class="meta-item">
            <span class="meta-label">Thời gian</span>
            <span class="meta-value">{{ formatDateTime(selectedLog.timestamp) }}</span>
          </div>
          <div class="meta-item">
            <span class="meta-label">Tài khoản</span>
            <span class="meta-value">@{{ selectedLog.username }} ({{ selectedLog.userRole }})</span>
          </div>
          <div class="meta-item">
            <span class="meta-label">Hành động / Phân hệ</span>
            <span class="meta-value">
              <el-tag :type="getActionTagType(selectedLog.action)" size="small">
                {{ formatActionLabel(selectedLog.action) }}
              </el-tag>
              <span class="ml-2">{{ formatEntityTypeLabel(selectedLog.entityType) }}</span>
            </span>
          </div>
          <div class="meta-item">
            <span class="meta-label">Địa chỉ IP</span>
            <span class="meta-value mono">{{ selectedLog.ipAddress || '127.0.0.1' }}</span>
          </div>
          <div class="meta-item">
            <span class="meta-label">Mã đối tượng</span>
            <span class="meta-value mono">{{ selectedLog.entityCode || (selectedLog.entityId ? '#' + selectedLog.entityId : '—') }}</span>
          </div>
        </div>

        <div class="detail-divider"></div>

        <div class="detail-section">
          <div class="section-heading">Mô tả / Lý do thao tác</div>
          <p class="detail-reason">{{ selectedLog.reason || 'Không có mô tả bổ sung.' }}</p>
        </div>

        <div class="detail-section">
          <div class="section-heading">Trình duyệt / Thiết bị (User-Agent)</div>
          <p class="detail-ua">{{ selectedLog.userAgent || 'Không xác định' }}</p>
        </div>

        <div class="detail-divider"></div>

        <div class="diff-grid">
          <div class="diff-col">
            <div class="diff-title old-title">Dữ liệu Trước thay đổi (Old Value)</div>
            <pre class="json-viewer">{{ formatJsonPretty(selectedLog.oldValueJson) }}</pre>
          </div>
          <div class="diff-col">
            <div class="diff-title new-title">Dữ liệu Sau thay đổi (New Value)</div>
            <pre class="json-viewer">{{ formatJsonPretty(selectedLog.newValueJson) }}</pre>
          </div>
        </div>
      </div>

      <template #footer>
        <div class="dialog-footer-clean">
          <el-button @click="detailVisible = false">Đóng</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Document, Refresh, Download, Search, Filter } from '@element-plus/icons-vue'
import api from '../api'

// Helpers khai báo đầu script setup (tránh TDZ ReferenceError)
function getTodayRange() {
  const now = new Date()
  const start = new Date(now.getFullYear(), now.getMonth(), now.getDate(), 0, 0, 0)
  const end = new Date(now.getFullYear(), now.getMonth(), now.getDate(), 23, 59, 59)
  return [start, end]
}

function formatToLocalIso(date, isEndOfDay = false) {
  if (!date) return null
  const d = new Date(date)
  if (Number.isNaN(d.getTime())) return null
  if (isEndOfDay) {
    d.setHours(23, 59, 59)
  } else {
    d.setHours(0, 0, 0)
  }
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

function formatDateTime(value) {
  if (!value) return '—'
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return String(value)
  const pad = (n) => String(n).padStart(2, '0')
  return `${pad(d.getDate())}/${pad(d.getMonth() + 1)}/${d.getFullYear()} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

function formatJsonPretty(raw) {
  if (!raw || !String(raw).trim()) return '(Không có dữ liệu snapshot)'
  try {
    return JSON.stringify(JSON.parse(raw), null, 2)
  } catch {
    return String(raw)
  }
}

function getInitial(username) {
  if (!username) return 'S'
  return String(username).charAt(0).toUpperCase()
}

const entityLabelMap = {
  AUTH: 'Xác thực (Auth)',
  PRODUCT: 'Sản phẩm',
  ORDER: 'Đơn hàng',
  USER: 'Tài khoản',
  FINANCE: 'Tài chính & Sổ quỹ',
  PROCUREMENT: 'Mua hàng & SRM',
  PROMOTION: 'Khuyến mại',
  BRANCH_WAREHOUSE: 'Chi nhánh & Kho',
  INVENTORY: 'Kiểm kê & Lô hàng',
  PURCHASE_ORDER: 'Đơn mua hàng (PO)',
  PAYMENT_VOUCHER: 'Phiếu chi / Thanh toán',
  SPECIAL_DISCOUNT: 'Chiết khấu đặc biệt',
  APPROVAL_REQUEST: 'Yêu cầu phê duyệt',
  ANNOUNCEMENT: 'Thông báo',
  CALENDAR: 'Lịch biểu',
  JOB_SCHEDULER: 'Tác vụ định kỳ',
  AI_COPILOT: 'AI Copilot',
  WORKFLOW: 'Quy trình duyệt',
  SYSTEM: 'Hệ thống'
}

const actionLabelMap = {
  CREATE: 'Thêm mới',
  UPDATE: 'Cập nhật',
  DELETE: 'Xóa',
  APPROVE: 'Phê duyệt',
  AUTO_APPROVE: 'Tự động duyệt',
  REJECT: 'Từ chối',
  CANCEL: 'Hủy bỏ',
  LOGIN_SUCCESS: 'Đăng nhập',
  LOGIN_FAILED: 'Đăng nhập lỗi',
  LOCK: 'Khóa tài khoản',
  UNLOCK: 'Mở khóa',
  CHANGE_PASSWORD: 'Đổi mật khẩu'
}

function formatEntityTypeLabel(type) {
  if (!type || type === 'N/A') return 'Chưa có'
  return entityLabelMap[type] ?? type
}

function formatActionLabel(action) {
  if (!action) return '—'
  return actionLabelMap[action] ?? action
}

function getActionTagType(action) {
  if (!action) return 'info'
  if (['CREATE', 'APPROVE', 'AUTO_APPROVE', 'LOGIN_SUCCESS', 'UNLOCK'].includes(action)) return 'success'
  if (['UPDATE', 'CHANGE_PASSWORD'].includes(action)) return 'primary'
  if (['DELETE', 'REJECT', 'CANCEL', 'LOGIN_FAILED', 'LOCK'].includes(action)) return 'danger'
  return 'info'
}

const entityTypeOptions = Object.entries(entityLabelMap).map(([value, label]) => ({ value, label }))
const actionOptions = Object.entries(actionLabelMap).map(([value, label]) => ({ value, label }))

// State
const loading = ref(false)
const exporting = ref(false)
const logs = ref([])
const totalElements = ref(0)
const currentPage = ref(1)
const pageSize = ref(20)
const detailVisible = ref(false)
const selectedLog = ref(null)

const stats = reactive({
  totalToday: 0,
  riskyToday: 0,
  activeUsersToday: 0,
  totalAll: 0,
  topEntity: 'N/A',
  topEntityCount: 0
})

const filters = reactive({
  keyword: '',
  entityType: '',
  action: '',
  dateRange: getTodayRange()
})

function buildQueryParams(includePagination = true) {
  const params = {}
  if (includePagination) {
    params.page = currentPage.value - 1
    params.size = pageSize.value
  }
  if (filters.keyword?.trim()) params.keyword = filters.keyword.trim()
  if (filters.entityType) params.entityType = filters.entityType
  if (filters.action) params.action = filters.action
  if (Array.isArray(filters.dateRange) && filters.dateRange.length === 2) {
    const fromIso = formatToLocalIso(filters.dateRange[0], false)
    const toIso = formatToLocalIso(filters.dateRange[1], true)
    if (fromIso) params.fromDate = fromIso
    if (toIso) params.toDate = toIso
  }
  return params
}

async function fetchStats() {
  try {
    const res = await api.get('/audit-logs/stats')
    if (res?.data) {
      Object.assign(stats, res.data)
    }
  } catch (err) {
    console.error('Lỗi tải thống kê audit logs:', err)
  }
}

async function fetchLogs() {
  loading.value = true
  try {
    const res = await api.get('/audit-logs', { params: buildQueryParams(true) })
    if (res?.data) {
      logs.value = res.data.content ?? []
      totalElements.value = res.data.totalElements ?? 0
    }
  } catch (err) {
    ElMessage.error(err?.response?.data?.message || 'Không thể tải nhật ký thao tác!')
  } finally {
    loading.value = false
  }
}

async function handleRefresh() {
  await Promise.all([fetchStats(), fetchLogs()])
}

function handleSearch() {
  currentPage.value = 1
  fetchLogs()
}

function handleResetFilters() {
  filters.keyword = ''
  filters.entityType = ''
  filters.action = ''
  filters.dateRange = getTodayRange()
  currentPage.value = 1
  fetchLogs()
}

function handlePageChange(page) {
  currentPage.value = page
  fetchLogs()
}

function handleSizeChange(size) {
  pageSize.value = size
  currentPage.value = 1
  fetchLogs()
}

async function handleExportExcel() {
  exporting.value = true
  try {
    const res = await api.get('/audit-logs/export', {
      params: buildQueryParams(false),
      responseType: 'blob'
    })
    const url = window.URL.createObjectURL(new Blob([res.data]))
    const link = document.createElement('a')
    link.href = url
    link.setAttribute('download', `nhat-ky-thao-tac-${Date.now()}.xlsx`)
    document.body.appendChild(link)
    link.click()
    link.remove()
    window.URL.revokeObjectURL(url)
    ElMessage.success('Đã xuất file Excel nhật ký thao tác thành công!')
  } catch (err) {
    ElMessage.error('Lỗi khi xuất báo cáo Excel nhật ký thao tác!')
  } finally {
    exporting.value = false
  }
}

function openDetailModal(row) {
  selectedLog.value = row
  detailVisible.value = true
}

async function copyText(text) {
  if (!text) return
  try {
    await navigator.clipboard.writeText(text)
    ElMessage.success(`Đã sao chép ${text}`)
  } catch {
    ElMessage.info(text)
  }
}

onMounted(() => {
  handleRefresh()
})
</script>

<style scoped>
.activity-logs-page {
  padding-bottom: 12px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.page-header-bar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  padding: 10px 16px;
}

.header-title-block {
  flex: 1 1 260px;
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 10px;
}

.title-icon-badge {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  background: #eff6ff;
  color: #2563eb;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  flex-shrink: 0;
}

.title-texts {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.page-title {
  margin: 0;
  font-size: 15.5px;
  font-weight: 700;
  color: #0f172a;
  line-height: 1.25;
}

.page-subtitle {
  font-size: 12px;
  color: #64748b;
  line-height: 1.3;
}

.header-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.action-btn {
  height: 32px;
  white-space: nowrap;
}

/* Compact KPI Strip */
.kpi-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}

@media (max-width: 1024px) {
  .kpi-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 600px) {
  .kpi-grid {
    grid-template-columns: 1fr;
  }
}

.kpi-card {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  padding: 9px 14px;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.kpi-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 6px;
}

.kpi-label {
  font-size: 12px;
  font-weight: 600;
  color: #64748b;
  white-space: nowrap;
}

.kpi-sub-pill {
  font-size: 11px;
  color: #94a3b8;
  white-space: nowrap;
}

.kpi-value {
  font-size: 18px;
  font-weight: 700;
  line-height: 1.25;
}

.text-blue { color: #2563eb; }
.text-red { color: #dc2626; }
.text-emerald { color: #059669; }
.text-slate { color: #0f172a; font-size: 15px; }

/* Single-Row Compact Filter Bar */
.filter-panel {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  padding: 9px 14px;
}

.filter-controls {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

.filter-input-search {
  flex: 1 1 190px;
  min-width: 170px;
}

.filter-select {
  width: 145px;
}

.filter-date-range {
  width: 245px !important;
  flex-shrink: 0;
}

.filter-btns {
  display: flex;
  gap: 6px;
  flex-shrink: 0;
}

.table-panel {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  padding: 8px 12px;
  min-width: 0;
}

:deep(.el-table th.el-table__cell) {
  background-color: #f8fafc !important;
  color: #334155;
  font-weight: 600;
  font-size: 12.5px;
  padding: 8px 6px;
}

:deep(.el-table td.el-table__cell) {
  padding: 6px;
}

.nowrap-mono {
  white-space: nowrap;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 12px;
}

.nowrap-tag {
  white-space: nowrap;
}

.trace-pill {
  border: 1px solid #cbd5e1;
  background: #f8fafc;
  color: #334155;
  border-radius: 5px;
  padding: 2px 6px;
  font-size: 11.5px;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  cursor: pointer;
  white-space: nowrap;
}

.trace-pill:hover {
  border-color: #2563eb;
  color: #2563eb;
  background: #eff6ff;
}

.user-cell {
  display: flex;
  align-items: center;
  gap: 7px;
}

.user-avatar {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: #e0e7ff;
  color: #3730a3;
  font-weight: 700;
  font-size: 11px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.user-meta {
  display: flex;
  flex-direction: column;
  min-width: 0;
  line-height: 1.2;
}

.user-name {
  font-weight: 600;
  font-size: 12.5px;
  color: #0f172a;
}

.user-role {
  font-size: 10.5px;
  color: #64748b;
}

.entity-cell {
  display: flex;
  flex-direction: column;
  gap: 1px;
  line-height: 1.25;
}

.entity-badge {
  font-size: 12.5px;
  font-weight: 600;
  color: #1e293b;
}

.entity-code {
  font-size: 11.5px;
  color: #64748b;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
}

.reason-text {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  font-size: 12.5px;
  color: #334155;
}

.pagination-bar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px solid #f1f5f9;
}

.total-info {
  font-size: 12.5px;
  color: #64748b;
}

/* Modal chi tiết */
.detail-body {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.detail-meta-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px 20px;
}

.meta-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.meta-label {
  font-size: 12px;
  color: #64748b;
}

.meta-value {
  font-size: 13.5px;
  font-weight: 600;
  color: #0f172a;
}

.meta-value.mono {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
}

.detail-divider {
  height: 1px;
  background: #f1f5f9;
}

.section-heading {
  font-size: 12.5px;
  font-weight: 600;
  color: #475569;
  margin-bottom: 4px;
}

.detail-reason {
  margin: 0;
  font-size: 13.5px;
  color: #0f172a;
}

.detail-ua {
  margin: 0;
  font-size: 12px;
  color: #64748b;
  word-break: break-word;
}

.diff-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

@media (max-width: 720px) {
  .detail-meta-grid,
  .diff-grid {
    grid-template-columns: 1fr;
  }
}

.diff-title {
  font-size: 12.5px;
  font-weight: 600;
  margin-bottom: 6px;
}

.old-title { color: #b91c1c; }
.new-title { color: #047857; }

.json-viewer {
  margin: 0;
  padding: 12px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  font-size: 12px;
  line-height: 1.5;
  color: #1e293b;
  max-height: 240px;
  overflow: auto;
  white-space: pre-wrap;
  word-break: break-word;
}

.ml-2 {
  margin-left: 8px;
}
</style>
