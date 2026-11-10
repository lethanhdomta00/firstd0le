<template>
  <div class="jobs-management-page">
    <!-- TOP HEADER BANNER -->
    <div class="page-header-card">
      <div class="header-content-left">
        <div class="header-icon-box">
          <el-icon class="cron-main-icon"><Timer /></el-icon>
        </div>
        <div>
          <div class="header-title-row">
            <h1 class="page-title">Quản lý Tác vụ tự động & Bộ lập lịch (Cron Jobs)</h1>
            <span class="engine-badge">
              <span class="engine-pulse-dot"></span>
              Spring TaskScheduler Engine
            </span>
          </div>
          <p class="page-subtitle">
            Điều phối và tự động hóa các tiến trình nền: tổng kết bán hàng, sao lưu, dọn dẹp cache và đối soát tồn kho định kỳ.
          </p>
        </div>
      </div>

      <div class="header-actions">
        <el-button 
          type="primary" 
          class="btn-create-job"
          @click="openCreateDialog"
        >
          <el-icon><Plus /></el-icon>
          <span>Tạo tác vụ mới</span>
        </el-button>

        <el-button 
          class="btn-view-logs"
          @click="openLogsDrawer(null)"
        >
          <el-icon><Document /></el-icon>
          <span>Nhật ký thực thi (Logs)</span>
        </el-button>

        <el-button 
          circle 
          title="Làm mới danh sách"
          :loading="loadingJobs"
          @click="fetchJobs"
        >
          <el-icon><Refresh /></el-icon>
        </el-button>
      </div>
    </div>

    <!-- METRICS SUMMARY CARDS -->
    <div class="metrics-grid">
      <div class="metric-card">
        <div class="metric-icon-wrap blue">
          <el-icon><Operation /></el-icon>
        </div>
        <div class="metric-info">
          <div class="metric-label">Tổng tác vụ cấu hình</div>
          <div class="metric-value">{{ jobs.length }}</div>
        </div>
      </div>

      <div class="metric-card">
        <div class="metric-icon-wrap green">
          <el-icon><VideoPlay /></el-icon>
        </div>
        <div class="metric-info">
          <div class="metric-label">Đang kích hoạt (Active)</div>
          <div class="metric-value text-green">{{ activeJobsCount }}</div>
        </div>
      </div>

      <div class="metric-card">
        <div class="metric-icon-wrap amber">
          <el-icon><VideoPause /></el-icon>
        </div>
        <div class="metric-info">
          <div class="metric-label">Đang tạm dừng (Paused)</div>
          <div class="metric-value text-amber">{{ pausedJobsCount }}</div>
        </div>
      </div>

      <div class="metric-card">
        <div class="metric-icon-wrap purple">
          <el-icon><CircleCheckFilled /></el-icon>
        </div>
        <div class="metric-info">
          <div class="metric-label">Lần chạy gần nhất</div>
          <div class="metric-sub-value truncate">{{ lastExecutedJobText }}</div>
        </div>
      </div>
    </div>

    <!-- FILTER & SEARCH BAR -->
    <div class="filter-bar-card">
      <div class="filter-left">
        <el-input 
          v-model="searchQuery" 
          placeholder="Tìm theo tên hoặc mã tác vụ (job code)..."
          clearable
          class="search-input"
          :prefix-icon="Search"
        />

        <el-select 
          v-model="statusFilter" 
          placeholder="Trạng thái" 
          clearable 
          class="filter-select"
        >
          <el-option label="Tất cả trạng thái" value="" />
          <el-option label="Đang chạy (ACTIVE)" value="ACTIVE" />
          <el-option label="Tạm dừng (PAUSED)" value="PAUSED" />
        </el-select>

        <el-select 
          v-model="typeFilter" 
          placeholder="Loại xử lý (Handler)" 
          clearable 
          class="filter-select"
        >
          <el-option label="Tất cả Handler" value="" />
          <el-option 
            v-for="type in availableJobTypes" 
            :key="type.jobType" 
            :label="type.displayName || type.jobType" 
            :value="type.jobType" 
          />
        </el-select>
      </div>

      <div class="filter-right">
        <span class="count-tag">Hiển thị {{ filteredJobs.length }} / {{ jobs.length }} tác vụ</span>
      </div>
    </div>

    <!-- MAIN JOBS TABLE -->
    <div class="table-card">
      <el-table 
        :data="filteredJobs" 
        v-loading="loadingJobs"
        style="width: 100%"
        empty-text="Chưa có tác vụ tự động nào được tạo."
      >
        <!-- Tên & Mã Tác vụ -->
        <el-table-column label="Tác vụ & Mã định danh" min-width="240">
          <template #default="{ row }">
            <div class="job-name-cell">
              <div class="job-name-row">
                <span class="job-display-name">{{ row.jobName }}</span>
                <el-tag size="small" type="info" class="job-code-tag">
                  {{ row.jobCode }}
                </el-tag>
              </div>
              <p v-if="row.description" class="job-desc-text">
                {{ row.description }}
              </p>
            </div>
          </template>
        </el-table-column>

        <!-- Loại Handler nghiệp vụ -->
        <el-table-column label="Nghiệp vụ thực thi" width="210">
          <template #default="{ row }">
            <div class="handler-type-badge">
              <el-icon class="handler-icon"><Cpu /></el-icon>
              <span>{{ getJobTypeLabel(row.jobType) }}</span>
            </div>
          </template>
        </el-table-column>

        <!-- Biểu thức Cron -->
        <el-table-column label="Lịch biểu (Cron Expression)" width="230">
          <template #default="{ row }">
            <div class="cron-cell">
              <div class="cron-expression-badge" :title="'Cú pháp Spring Cron: ' + row.cronExpression">
                <code>{{ row.cronExpression }}</code>
              </div>
              <span class="cron-human-desc">
                {{ explainCron(row.cronExpression) }}
              </span>
            </div>
          </template>
        </el-table-column>

        <!-- Lần chạy trước -->
        <el-table-column label="Lần chạy gần nhất" width="180">
          <template #default="{ row }">
            <div v-if="row.lastRunAt" class="run-time-cell">
              <div class="time-text">{{ formatDateTime(row.lastRunAt) }}</div>
              <div class="status-duration-row">
                <el-tag 
                  size="small" 
                  :type="row.lastRunStatus === 'SUCCESS' ? 'success' : (row.lastRunStatus === 'FAILED' ? 'danger' : 'info')"
                >
                  {{ row.lastRunStatus || 'N/A' }}
                </el-tag>
                <span v-if="row.lastRunDurationMs" class="duration-text">
                  {{ row.lastRunDurationMs }}ms
                </span>
              </div>
            </div>
            <span v-else class="text-muted">Chưa từng chạy</span>
          </template>
        </el-table-column>

        <!-- Lần chạy kế tiếp -->
        <el-table-column label="Lần chạy tới" width="180">
          <template #default="{ row }">
            <div v-if="row.status === 'ACTIVE' && row.nextRunAt" class="next-run-cell">
              <div class="time-text">{{ formatDateTime(row.nextRunAt) }}</div>
              <span class="time-relative">{{ getRelativeTime(row.nextRunAt) }}</span>
            </div>
            <span v-else-if="row.status === 'PAUSED'" class="paused-pill">
              ⏸️ Đang tạm dừng
            </span>
            <span v-else class="text-muted">Không xác định</span>
          </template>
        </el-table-column>

        <!-- Trạng thái -->
        <el-table-column label="Trạng thái" width="130" align="center">
          <template #default="{ row }">
            <el-switch
              v-model="row.status"
              active-value="ACTIVE"
              inactive-value="PAUSED"
              active-text="Bật"
              inactive-text="Tắt"
              inline-prompt
              :loading="row._statusLoading"
              @change="handleToggleStatus(row)"
            />
          </template>
        </el-table-column>

        <!-- Hành động -->
        <el-table-column label="Thao tác" width="200" align="right" fixed="right">
          <template #default="{ row }">
            <div class="action-buttons-cell">
              <!-- Trigger Now -->
              <el-button 
                size="small" 
                type="success" 
                plain
                class="btn-trigger"
                :loading="row._triggerLoading"
                title="Kích hoạt chạy ngay lập tức"
                @click="handleTriggerNow(row)"
              >
                <el-icon><Lightning /></el-icon>
                <span>Chạy ngay</span>
              </el-button>

              <el-dropdown trigger="click">
                <el-button size="small" circle class="btn-more-options">
                  <el-icon><MoreFilled /></el-icon>
                </el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item @click="openLogsDrawer(row.jobCode)">
                      <el-icon><Document /></el-icon> Xem nhật ký chạy
                    </el-dropdown-item>
                    <el-dropdown-item @click="openEditDialog(row)">
                      <el-icon><Edit /></el-icon> Chỉnh sửa lịch Cron
                    </el-dropdown-item>
                    <el-dropdown-item divided @click="handleDeleteJob(row)" class="text-danger">
                      <el-icon><Delete /></el-icon> Xóa tác vụ vĩnh viễn
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- ==================================================================== -->
    <!-- DIALOG: TẠO MỚI / CHỈNH SỬA TÁC VỤ & VISUAL CRON BUILDER            -->
    <!-- ==================================================================== -->
    <el-dialog 
      v-model="jobDialogVisible" 
      :title="isEditMode ? 'Chỉnh sửa Tác vụ tự động' : 'Tạo mới Tác vụ tự động (Cron Job)'"
      width="680px"
      destroy-on-close
      class="job-editor-dialog"
    >
      <el-form 
        ref="jobFormRef" 
        :model="jobForm" 
        :rules="jobRules" 
        label-position="top"
        class="job-dialog-form"
      >
        <el-row :gutter="16">
          <el-col :span="14">
            <el-form-item label="Tên tác vụ hiển thị" prop="jobName">
              <el-input 
                v-model="jobForm.jobName" 
                placeholder="Ví dụ: Tự động dọn tệp rác hệ thống"
              />
            </el-form-item>
          </el-col>
          <el-col :span="10">
            <el-form-item label="Mã định danh (Job Code)" prop="jobCode">
              <el-input 
                v-model="jobForm.jobCode" 
                placeholder="VD: CLEANUP_TEMP_FILES"
                :disabled="isEditMode"
                @input="val => jobForm.jobCode = val.toUpperCase().replace(/\s+/g, '_')"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="14">
            <el-form-item label="Nghiệp vụ thực thi (Task Handler)" prop="jobType">
              <el-select 
                v-model="jobForm.jobType" 
                placeholder="Chọn nghiệp vụ cần kích hoạt"
                style="width: 100%"
                :disabled="isEditMode"
              >
                <el-option 
                  v-for="type in availableJobTypes" 
                  :key="type.jobType" 
                  :label="type.displayName || type.jobType" 
                  :value="type.jobType" 
                >
                  <div class="select-option-row">
                    <span class="font-bold">{{ type.displayName }}</span>
                    <span class="text-xs text-muted">{{ type.jobType }}</span>
                  </div>
                </el-option>
              </el-select>
            </el-form-item>
          </el-col>

          <el-col :span="10">
            <el-form-item label="Nhóm tác vụ" prop="jobGroup">
              <el-input 
                v-model="jobForm.jobGroup" 
                placeholder="Mặc định: DEFAULT"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <!-- CRON EXPRESSION VISUAL BUILDER -->
        <div class="cron-builder-section">
          <div class="cron-builder-header">
            <span class="cron-builder-title">
              <el-icon><Clock /></el-icon> Cấu hình Lịch biểu chạy (Cron Schedule)
            </span>
            <span class="cron-standard-hint">Chuẩn Spring Boot 6 ký tự: <code>(sec min hour day mon weekday)</code></span>
          </div>

          <el-tabs v-model="cronTab" class="cron-preset-tabs">
            <!-- TAB 1: PRESETS THÔNG DỤNG -->
            <el-tab-pane label="Tần suất phổ biến (Presets)" name="presets">
              <div class="presets-chip-grid">
                <button
                  type="button"
                  v-for="p in cronPresets"
                  :key="p.cron"
                  class="preset-chip-btn"
                  :class="{ active: jobForm.cronExpression === p.cron }"
                  @click="applyCronPreset(p.cron)"
                >
                  <span class="preset-title">{{ p.label }}</span>
                  <code class="preset-code">{{ p.cron }}</code>
                </button>
              </div>
            </el-tab-pane>

            <!-- TAB 2: TÙY CHỌN THEO GIỜ/PHÚT -->
            <el-tab-pane label="Lập lịch Hàng ngày / Hàng tuần" name="builder">
              <div class="simple-builder-form">
                <div class="builder-row">
                  <span class="builder-label">Chạy vào lúc:</span>
                  <el-time-picker
                    v-model="builderTime"
                    format="HH:mm"
                    placeholder="Chọn giờ:phút"
                    @change="updateCronFromBuilder"
                  />
                  <span class="builder-hint">mỗi ngày (Lúc {{ builderTimeStr }})</span>
                </div>
              </div>
            </el-tab-pane>

            <!-- TAB 3: TÙY CHỈNH NÂNG CAO -->
            <el-tab-pane label="Cú pháp tùy chỉnh (Custom Cron)" name="custom">
              <p class="custom-cron-desc">
                Nhập trực tiếp biểu thức cron 6 trường của Spring. Ví dụ: <code>0 0/15 * * * ?</code> (mỗi 15 phút), <code>0 30 8 ? * MON-FRI</code> (8h30 sáng từ Thứ 2 đến Thứ 6).
              </p>
            </el-tab-pane>
          </el-tabs>

          <!-- Ô hiển thị và dịch nghĩa Cron -->
          <div class="cron-result-box">
            <div class="cron-input-group">
              <label class="cron-input-label">Biểu thức hiện tại:</label>
              <el-input 
                v-model="jobForm.cronExpression" 
                placeholder="VD: 0 0 2 * * ?"
                class="cron-target-input"
              />
            </div>
            <div class="cron-translated-banner">
              <el-icon class="magic-icon"><MagicStick /></el-icon>
              <span><strong>Diễn giải:</strong> {{ explainCron(jobForm.cronExpression) }}</span>
            </div>
          </div>
        </div>

        <el-form-item label="Mô tả mục đích tác vụ" prop="description" class="mt-3">
          <el-input 
            v-model="jobForm.description" 
            type="textarea" 
            :rows="2"
            placeholder="Giải thích ngắn gọn lý do và ảnh hưởng của tác vụ này đối với hệ thống ERP..."
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="jobDialogVisible = false">Hủy bỏ</el-button>
          <el-button 
            type="primary" 
            :loading="savingJob"
            @click="handleSubmitJob"
          >
            {{ isEditMode ? 'Lưu cập nhật' : 'Khởi tạo Tác vụ' }}
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- ==================================================================== -->
    <!-- DRAWER: NHẬT KÝ THỰC THI (EXECUTION LOGS)                           -->
    <!-- ==================================================================== -->
    <el-drawer 
      v-model="logsDrawerVisible" 
      title="Nhật ký thực thi tác vụ nền (Execution Logs)"
      size="750px"
      direction="rtl"
      class="logs-drawer-custom"
    >
      <div class="logs-drawer-body">
        <!-- Filter bar in drawer -->
        <div class="logs-filter-bar">
          <el-select 
            v-model="selectedLogJobCode" 
            placeholder="Lọc theo Tác vụ"
            clearable
            style="width: 260px"
            @change="() => { logsPage = 0; fetchLogs(); }"
          >
            <el-option label="Tất cả tác vụ" value="" />
            <el-option 
              v-for="j in jobs" 
              :key="j.jobCode" 
              :label="j.jobName + ' (' + j.jobCode + ')'" 
              :value="j.jobCode" 
            />
          </el-select>

          <el-button 
            :icon="Refresh" 
            circle 
            title="Làm mới logs"
            :loading="loadingLogs"
            @click="fetchLogs" 
          />
        </div>

        <!-- Logs Table -->
        <el-table 
          :data="executionLogs" 
          v-loading="loadingLogs"
          style="width: 100%"
          empty-text="Chưa có dữ liệu nhật ký thực thi nào."
          class="logs-table"
        >
          <el-table-column label="Thời gian chạy" width="160">
            <template #default="{ row }">
              <div class="log-time-cell">
                <span class="font-bold">{{ formatDateTime(row.startTime) }}</span>
                <span class="text-xs text-muted">{{ getRelativeTime(row.startTime) }}</span>
              </div>
            </template>
          </el-table-column>

          <el-table-column label="Tác vụ" min-width="150">
            <template #default="{ row }">
              <div class="log-job-info">
                <span class="font-semibold">{{ row.jobName || row.jobCode }}</span>
                <span class="code-sub">{{ row.jobCode }}</span>
              </div>
            </template>
          </el-table-column>

          <el-table-column label="Trạng thái" width="110" align="center">
            <template #default="{ row }">
              <el-tag 
                size="small" 
                :type="row.status === 'SUCCESS' ? 'success' : (row.status === 'FAILED' ? 'danger' : 'warning')"
              >
                {{ row.status }}
              </el-tag>
            </template>
          </el-table-column>

          <el-table-column label="Thời lượng" width="100" align="right">
            <template #default="{ row }">
              <span class="duration-mono">{{ row.durationMs != null ? row.durationMs + 'ms' : '-' }}</span>
            </template>
          </el-table-column>

          <el-table-column label="Kích hoạt bởi" width="120">
            <template #default="{ row }">
              <span class="trigger-by-tag">
                {{ row.triggeredBy === 'SYSTEM_CRON' ? '⏰ Hẹn giờ' : '👤 Thủ công' }}
              </span>
            </template>
          </el-table-column>

          <el-table-column label="Chi tiết kết quả" min-width="180">
            <template #default="{ row }">
              <div class="log-summary-cell">
                <span class="summary-text" :title="row.resultSummary || row.errorLog">
                  {{ row.resultSummary || (row.status === 'FAILED' ? row.errorLog : 'Hoàn tất') }}
                </span>
                <el-button 
                  v-if="row.errorLog" 
                  type="danger" 
                  link 
                  size="small"
                  @click="showErrorDetail(row)"
                >
                  Xem lỗi
                </el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>

        <!-- Pagination -->
        <div class="logs-pagination-row">
          <el-pagination 
            v-model:current-page="logsPagePlusOne"
            :page-size="logsPageSize"
            :total="logsTotalElements"
            layout="prev, pager, next, total"
            @current-change="handleLogsPageChange"
          />
        </div>
      </div>
    </el-drawer>

    <!-- ERROR DETAIL MODAL -->
    <el-dialog 
      v-model="errorModalVisible" 
      title="Chi tiết Ngoại lệ Thực thi (Stack Trace)" 
      width="600px"
    >
      <div class="error-modal-body">
        <pre class="error-pre">{{ selectedErrorLog }}</pre>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Timer, Plus, Document, Refresh, Operation, VideoPlay, VideoPause,
  CircleCheckFilled, Search, Cpu, Lightning, MoreFilled, Edit, Delete,
  Clock, MagicStick
} from '@element-plus/icons-vue'
import api from '../api'

// State Jobs
const jobs = ref([])
const loadingJobs = ref(false)
const availableJobTypes = ref([])

// Filters
const searchQuery = ref('')
const statusFilter = ref('')
const typeFilter = ref('')

// Dialog Edit/Create
const jobDialogVisible = ref(false)
const isEditMode = ref(false)
const savingJob = ref(false)
const editingJobId = ref(null)
const jobFormRef = ref(null)

const jobForm = reactive({
  jobName: '',
  jobCode: '',
  jobGroup: 'DEFAULT',
  jobType: '',
  cronExpression: '0 0 2 * * ?',
  description: ''
})

const jobRules = {
  jobName: [{ required: true, message: 'Vui lòng nhập tên tác vụ', trigger: 'blur' }],
  jobCode: [{ required: true, message: 'Vui lòng nhập mã tác vụ', trigger: 'blur' }],
  jobType: [{ required: true, message: 'Vui lòng chọn loại tác vụ', trigger: 'change' }],
  cronExpression: [{ required: true, message: 'Vui lòng cấu hình biểu thức Cron', trigger: 'blur' }]
}

// Cron Builder Tabs & Presets
const cronTab = ref('presets')
const builderTime = ref(new Date(2026, 0, 1, 2, 0, 0))

const builderTimeStr = computed(() => {
  if (!builderTime.value) return '02:00'
  const d = new Date(builderTime.value)
  const h = String(d.getHours()).padStart(2, '0')
  const m = String(d.getMinutes()).padStart(2, '0')
  return `${h}:${m}`
})

const cronPresets = [
  { label: 'Mỗi 5 phút một lần', cron: '0 */5 * * * ?' },
  { label: 'Mỗi 15 phút một lần', cron: '0 */15 * * * ?' },
  { label: 'Mỗi 30 phút một lần', cron: '0 */30 * * * ?' },
  { label: 'Mỗi giờ (Phút thứ 0)', cron: '0 0 * * * ?' },
  { label: 'Hàng ngày lúc 00:00 (Nửa đêm)', cron: '0 0 0 * * ?' },
  { label: 'Hàng ngày lúc 02:00 sáng', cron: '0 0 2 * * ?' },
  { label: 'Hàng ngày lúc 08:30 sáng', cron: '0 30 8 * * ?' },
  { label: 'Hàng tuần vào Thứ 2 lúc 06:00', cron: '0 0 6 ? * MON' },
  { label: 'Đầu tháng lúc 01:00 sáng', cron: '0 0 1 1 * ?' }
]

function applyCronPreset(cron) {
  jobForm.cronExpression = cron
}

function updateCronFromBuilder() {
  if (!builderTime.value) return
  const d = new Date(builderTime.value)
  const h = d.getHours()
  const m = d.getMinutes()
  jobForm.cronExpression = `0 ${m} ${h} * * ?`
}

// Logs Drawer State
const logsDrawerVisible = ref(false)
const loadingLogs = ref(false)
const executionLogs = ref([])
const selectedLogJobCode = ref('')
const logsPage = ref(0)
const logsPageSize = ref(15)
const logsTotalElements = ref(0)

const logsPagePlusOne = computed({
  get: () => logsPage.value + 1,
  set: (val) => { logsPage.value = val - 1 }
})

const errorModalVisible = ref(false)
const selectedErrorLog = ref('')

// Computed Metrics
const activeJobsCount = computed(() => jobs.value.filter(j => j.status === 'ACTIVE').length)
const pausedJobsCount = computed(() => jobs.value.filter(j => j.status === 'PAUSED').length)

const lastExecutedJobText = computed(() => {
  const executed = jobs.value.filter(j => j.lastRunAt).sort((a, b) => new Date(b.lastRunAt) - new Date(a.lastRunAt))
  if (executed.length === 0) return 'Chưa có tác vụ nào chạy'
  const top = executed[0]
  return `${top.jobName} (${getRelativeTime(top.lastRunAt)})`
})

const filteredJobs = computed(() => {
  return jobs.value.filter(j => {
    const matchSearch = !searchQuery.value || 
      (j.jobName && j.jobName.toLowerCase().includes(searchQuery.value.toLowerCase())) ||
      (j.jobCode && j.jobCode.toLowerCase().includes(searchQuery.value.toLowerCase()))
    const matchStatus = !statusFilter.value || j.status === statusFilter.value
    const matchType = !typeFilter.value || j.jobType === typeFilter.value
    return matchSearch && matchStatus && matchType
  })
})

// Cron Humanizer
function explainCron(cron) {
  if (!cron) return 'Chưa cấu hình lịch chạy'
  const c = cron.trim()
  if (c === '0 */5 * * * ?') return 'Chạy lặp lại mỗi 5 phút một lần'
  if (c === '0 */15 * * * ?') return 'Chạy lặp lại mỗi 15 phút một lần'
  if (c === '0 */30 * * * ?') return 'Chạy lặp lại mỗi 30 phút một lần'
  if (c === '0 0 * * * ?') return 'Chạy vào phút thứ 0 của mỗi giờ'
  if (c === '0 0 0 * * ?') return 'Chạy hàng ngày vào đúng 00:00 (Nửa đêm)'
  if (c === '0 0 2 * * ?') return 'Chạy hàng ngày vào lúc 02:00 sáng'
  if (c === '0 30 8 * * ?') return 'Chạy hàng ngày vào lúc 08:30 sáng'
  if (c === '0 0 6 ? * MON') return 'Chạy hàng tuần vào lúc 06:00 sáng Thứ Hai'
  if (c === '0 0 1 1 * ?') return 'Chạy vào ngày mùng 1 hàng tháng lúc 01:00 sáng'

  // Generic patterns
  const parts = c.split(/\s+/)
  if (parts.length >= 6) {
    const [sec, min, hour, day, mon, week] = parts
    if (day === '*' && mon === '*' && (week === '?' || week === '*')) {
      return `Chạy hàng ngày vào lúc ${hour.padStart(2, '0')}:${min.padStart(2, '0')}:${sec.padStart(2, '0')}`
    }
  }
  return `Lập lịch chạy theo biểu thức: ${cron}`
}

function getJobTypeLabel(jobType) {
  const found = availableJobTypes.value.find(t => t.jobType === jobType)
  return found ? found.displayName : jobType
}

function formatDateTime(isoStr) {
  if (!isoStr) return '-'
  try {
    const d = new Date(isoStr)
    if (isNaN(d.getTime())) return isoStr
    const pad = n => String(n).padStart(2, '0')
    return `${pad(d.getDate())}/${pad(d.getMonth() + 1)}/${d.getFullYear()} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
  } catch {
    return isoStr
  }
}

function getRelativeTime(isoStr) {
  if (!isoStr) return ''
  try {
    const d = new Date(isoStr)
    const now = new Date()
    const diffSec = Math.floor((d.getTime() - now.getTime()) / 1000)
    if (Math.abs(diffSec) < 60) return diffSec >= 0 ? 'Trong vòng vài giây' : 'Vừa xong'
    if (diffSec > 0) {
      const min = Math.floor(diffSec / 60)
      if (min < 60) return `Sau ${min} phút nữa`
      const h = Math.floor(min / 60)
      if (h < 24) return `Sau ${h} giờ nữa`
      return `Sau ${Math.floor(h / 24)} ngày nữa`
    } else {
      const pastMin = Math.floor(Math.abs(diffSec) / 60)
      if (pastMin < 60) return `${pastMin} phút trước`
      const pastH = Math.floor(pastMin / 60)
      if (pastH < 24) return `${pastH} giờ trước`
      return `${Math.floor(pastH / 24)} ngày trước`
    }
  } catch {
    return ''
  }
}

// Fetch APIs
async function fetchJobs() {
  loadingJobs.value = true
  try {
    const res = await api.get('/scheduler/jobs')
    jobs.value = (res.data || []).map(j => ({
      ...j,
      _statusLoading: false,
      _triggerLoading: false
    }))
  } catch (err) {
    ElMessage.error(err.response?.data?.message || 'Không thể tải danh sách tác vụ tự động!')
  } finally {
    loadingJobs.value = false
  }
}

async function fetchJobTypes() {
  try {
    const res = await api.get('/scheduler/job-types')
    availableJobTypes.value = res.data || []
  } catch (err) {
    console.error('Lỗi tải danh sách job types:', err)
  }
}

async function handleToggleStatus(job) {
  job._statusLoading = true
  const isPause = job.status === 'PAUSED'
  try {
    if (isPause) {
      await api.post(`/scheduler/jobs/${job.id}/pause`)
      ElMessage.warning(`Đã tạm dừng tác vụ [${job.jobName}]`)
    } else {
      await api.post(`/scheduler/jobs/${job.id}/resume`)
      ElMessage.success(`Đã kích hoạt lại tác vụ [${job.jobName}]`)
    }
    await fetchJobs()
  } catch (err) {
    job.status = isPause ? 'ACTIVE' : 'PAUSED'
    ElMessage.error(err.response?.data?.message || 'Lỗi khi thay đổi trạng thái tác vụ!')
  } finally {
    job._statusLoading = false
  }
}

async function handleTriggerNow(job) {
  try {
    await ElMessageBox.confirm(
      `Bạn có chắc chắn muốn kích hoạt chạy ngay tác vụ "${job.jobName}" (${job.jobCode}) mà không cần chờ đến lịch Cron?`,
      'Xác nhận chạy tác vụ ngay',
      {
        confirmButtonText: 'Kích hoạt ngay ⚡',
        cancelButtonText: 'Hủy bỏ',
        type: 'warning'
      }
    )

    job._triggerLoading = true
    const res = await api.post(`/scheduler/jobs/${job.id}/trigger`)
    ElMessage.success(res.data?.message || `Đã kích hoạt tác vụ [${job.jobName}] thành công!`)
    setTimeout(fetchJobs, 1500)
  } catch (err) {
    if (err !== 'cancel') {
      ElMessage.error(err.response?.data?.message || 'Lỗi khi kích hoạt chạy tác vụ!')
    }
  } finally {
    job._triggerLoading = false
  }
}

function openCreateDialog() {
  isEditMode.value = false
  editingJobId.value = null
  jobForm.jobName = ''
  jobForm.jobCode = ''
  jobForm.jobGroup = 'DEFAULT'
  jobForm.jobType = availableJobTypes.value[0]?.jobType || ''
  jobForm.cronExpression = '0 0 2 * * ?'
  jobForm.description = ''
  jobDialogVisible.value = true
}

function openEditDialog(job) {
  isEditMode.value = true
  editingJobId.value = job.id
  jobForm.jobName = job.jobName
  jobForm.jobCode = job.jobCode
  jobForm.jobGroup = job.jobGroup || 'DEFAULT'
  jobForm.jobType = job.jobType
  jobForm.cronExpression = job.cronExpression
  jobForm.description = job.description || ''
  jobDialogVisible.value = true
}

async function handleSubmitJob() {
  if (!jobFormRef.value) return
  await jobFormRef.value.validate(async valid => {
    if (!valid) return
    savingJob.value = true
    try {
      if (isEditMode.value) {
        await api.put(`/scheduler/jobs/${editingJobId.value}`, {
          jobName: jobForm.jobName,
          jobGroup: jobForm.jobGroup,
          cronExpression: jobForm.cronExpression,
          description: jobForm.description
        })
        ElMessage.success('Đã cập nhật cấu hình tác vụ thành công!')
      } else {
        await api.post('/scheduler/jobs', {
          jobCode: jobForm.jobCode,
          jobName: jobForm.jobName,
          jobGroup: jobForm.jobGroup,
          jobType: jobForm.jobType,
          cronExpression: jobForm.cronExpression,
          description: jobForm.description
        })
        ElMessage.success('Đã khởi tạo tác vụ tự động mới thành công!')
      }
      jobDialogVisible.value = false
      await fetchJobs()
    } catch (err) {
      ElMessage.error(err.response?.data?.message || 'Lỗi khi lưu tác vụ!')
    } finally {
      savingJob.value = false
    }
  })
}

async function handleDeleteJob(job) {
  try {
    await ElMessageBox.confirm(
      `Hành động này sẽ xóa vĩnh viễn tác vụ "${job.jobName}" (${job.jobCode}) và hủy bỏ hoàn toàn lịch chạy nền. Bạn có chắc chắn không?`,
      'Cảnh báo xóa tác vụ',
      {
        confirmButtonText: 'Xác nhận xóa',
        cancelButtonText: 'Hủy',
        type: 'error'
      }
    )
    await api.delete(`/scheduler/jobs/${job.id}`)
    ElMessage.success(`Đã xóa tác vụ [${job.jobName}] thành công!`)
    await fetchJobs()
  } catch (err) {
    if (err !== 'cancel') {
      ElMessage.error(err.response?.data?.message || 'Lỗi khi xóa tác vụ!')
    }
  }
}

// Logs Drawer
function openLogsDrawer(jobCode) {
  selectedLogJobCode.value = jobCode || ''
  logsPage.value = 0
  logsDrawerVisible.value = true
  fetchLogs()
}

async function fetchLogs() {
  loadingLogs.value = true
  try {
    const res = await api.get('/scheduler/logs', {
      params: {
        jobCode: selectedLogJobCode.value || undefined,
        page: logsPage.value,
        size: logsPageSize.value
      }
    })
    executionLogs.value = res.data?.content || []
    logsTotalElements.value = res.data?.totalElements || 0
  } catch (err) {
    ElMessage.error(err.response?.data?.message || 'Không thể tải nhật ký thực thi!')
  } finally {
    loadingLogs.value = false
  }
}

function handleLogsPageChange(newPage) {
  logsPage.value = newPage - 1
  fetchLogs()
}

function showErrorDetail(row) {
  selectedErrorLog.value = row.errorLog || 'Không có stack trace chi tiết.'
  errorModalVisible.value = true
}

onMounted(() => {
  fetchJobs()
  fetchJobTypes()
})
</script>

<style scoped>
.jobs-management-page {
  padding: 20px;
  max-width: 1400px;
  margin: 0 auto;
}

/* TOP HEADER CARD */
.page-header-card {
  background: linear-gradient(135deg, #ffffff 0%, #f8fafc 100%);
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 20px 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

.header-content-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.header-icon-box {
  width: 52px;
  height: 52px;
  background: #eff6ff;
  border: 1px solid #bfdbfe;
  color: #2563eb;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 26px;
  box-shadow: 0 2px 4px rgba(37, 99, 235, 0.1);
}

.header-title-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.page-title {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
  color: #0f172a;
  letter-spacing: -0.01em;
}

.engine-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 3px 10px;
  background: #f0fdf4;
  border: 1px solid #bbf7d0;
  border-radius: 20px;
  color: #15803d;
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.02em;
}

.engine-pulse-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #22c55e;
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0% { transform: scale(0.95); box-shadow: 0 0 0 0 rgba(34, 197, 94, 0.7); }
  70% { transform: scale(1); box-shadow: 0 0 0 6px rgba(34, 197, 94, 0); }
  100% { transform: scale(0.95); box-shadow: 0 0 0 0 rgba(34, 197, 94, 0); }
}

.page-subtitle {
  margin: 4px 0 0 0;
  color: #64748b;
  font-size: 13px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.btn-create-job {
  background: #2563eb;
  font-weight: 600;
  border-radius: 8px;
}

/* METRICS GRID */
.metrics-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}

@media (max-width: 992px) {
  .metrics-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 600px) {
  .metrics-grid {
    grid-template-columns: 1fr;
  }
}

.metric-card {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 16px 18px;
  display: flex;
  align-items: center;
  gap: 14px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.03);
}

.metric-icon-wrap {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
}

.metric-icon-wrap.blue { background: #eff6ff; color: #2563eb; }
.metric-icon-wrap.green { background: #f0fdf4; color: #16a34a; }
.metric-icon-wrap.amber { background: #fefce8; color: #d97706; }
.metric-icon-wrap.purple { background: #faf5ff; color: #9333ea; }

.metric-label {
  font-size: 12px;
  color: #64748b;
  font-weight: 500;
}

.metric-value {
  font-size: 24px;
  font-weight: 700;
  color: #0f172a;
  line-height: 1.2;
}

.metric-value.text-green { color: #16a34a; }
.metric-value.text-amber { color: #d97706; }
.metric-sub-value {
  font-size: 13px;
  font-weight: 600;
  color: #334155;
  max-width: 170px;
}

/* FILTER BAR */
.filter-bar-card {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  padding: 12px 18px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  gap: 12px;
  flex-wrap: wrap;
}

.filter-left {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.search-input {
  width: 280px;
}

.filter-select {
  width: 180px;
}

.count-tag {
  font-size: 13px;
  color: #64748b;
  font-weight: 500;
}

/* TABLE CARD */
.table-card {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
}

.job-name-cell {
  display: flex;
  flex-col: column;
  gap: 2px;
}

.job-name-row {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.job-display-name {
  font-weight: 600;
  color: #0f172a;
  font-size: 14px;
}

.job-code-tag {
  font-family: monospace;
  font-weight: 600;
}

.job-desc-text {
  margin: 3px 0 0 0;
  font-size: 12px;
  color: #64748b;
  line-height: 1.4;
}

.handler-type-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 10px;
  background: #f1f5f9;
  border: 1px solid #cbd5e1;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 600;
  color: #334155;
}

.handler-icon {
  color: #64748b;
}

.cron-cell {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.cron-expression-badge {
  display: inline-block;
  background: #0f172a;
  color: #38bdf8;
  padding: 3px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-family: monospace;
  letter-spacing: 0.05em;
  width: fit-content;
}

.cron-human-desc {
  font-size: 11px;
  color: #64748b;
  font-style: italic;
}

.run-time-cell, .next-run-cell {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.time-text {
  font-size: 13px;
  font-weight: 500;
  color: #1e293b;
}

.status-duration-row {
  display: flex;
  align-items: center;
  gap: 6px;
}

.duration-text {
  font-size: 11px;
  font-family: monospace;
  color: #64748b;
}

.time-relative {
  font-size: 11px;
  color: #2563eb;
  font-weight: 500;
}

.paused-pill {
  font-size: 12px;
  color: #d97706;
  font-weight: 600;
}

.text-muted {
  color: #94a3b8;
  font-size: 12px;
}

.action-buttons-cell {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 6px;
}

.btn-trigger {
  font-weight: 600;
}

.btn-more-options {
  color: #64748b;
}

/* DIALOG CRON BUILDER STYLES */
.cron-builder-section {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 14px;
  margin-top: 10px;
}

.cron-builder-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.cron-builder-title {
  font-size: 13px;
  font-weight: 700;
  color: #1e293b;
  display: flex;
  align-items: center;
  gap: 6px;
}

.cron-standard-hint {
  font-size: 11px;
  color: #64748b;
}

.presets-chip-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
  margin-top: 8px;
}

.preset-chip-btn {
  background: #ffffff;
  border: 1px solid #cbd5e1;
  border-radius: 6px;
  padding: 8px;
  text-align: left;
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.preset-chip-btn:hover {
  border-color: #2563eb;
  background: #eff6ff;
}

.preset-chip-btn.active {
  background: #eff6ff;
  border-color: #2563eb;
  box-shadow: 0 0 0 2px rgba(37, 99, 235, 0.2);
}

.preset-title {
  font-size: 12px;
  font-weight: 600;
  color: #1e293b;
}

.preset-code {
  font-size: 11px;
  color: #64748b;
  font-family: monospace;
}

.simple-builder-form {
  padding: 10px 0;
}

.builder-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.builder-label {
  font-size: 13px;
  font-weight: 600;
  color: #334155;
}

.builder-hint {
  font-size: 13px;
  color: #2563eb;
  font-weight: 500;
}

.custom-cron-desc {
  font-size: 12px;
  color: #64748b;
  line-height: 1.5;
  margin: 6px 0;
}

.cron-result-box {
  margin-top: 14px;
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  padding: 10px 14px;
}

.cron-input-group {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}

.cron-input-label {
  font-size: 12px;
  font-weight: 600;
  color: #334155;
  white-space: nowrap;
}

.cron-target-input {
  max-width: 260px;
  font-family: monospace;
  font-weight: bold;
}

.cron-translated-banner {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #15803d;
  font-size: 12px;
  padding-top: 4px;
  border-top: 1px dashed #e2e8f0;
}

.magic-icon {
  color: #16a34a;
}

/* LOGS DRAWER STYLES */
.logs-drawer-body {
  padding: 0 10px;
}

.logs-filter-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.log-time-cell {
  display: flex;
  flex-direction: column;
}

.log-job-info {
  display: flex;
  flex-direction: column;
}

.code-sub {
  font-size: 11px;
  font-family: monospace;
  color: #64748b;
}

.duration-mono {
  font-family: monospace;
  font-weight: 600;
  color: #334155;
}

.trigger-by-tag {
  font-size: 11px;
  color: #64748b;
}

.log-summary-cell {
  display: flex;
  align-items: center;
  gap: 6px;
}

.summary-text {
  font-size: 12px;
  color: #334155;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 220px;
}

.logs-pagination-row {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

.error-modal-body {
  background: #0f172a;
  color: #f87171;
  padding: 14px;
  border-radius: 6px;
  max-height: 400px;
  overflow-y: auto;
}

.error-pre {
  margin: 0;
  font-family: monospace;
  font-size: 12px;
  white-space: pre-wrap;
  word-break: break-all;
}

.text-danger {
  color: #ef4444 !important;
}
</style>
