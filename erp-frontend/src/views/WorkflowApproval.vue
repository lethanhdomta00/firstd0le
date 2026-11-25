<template>
  <div class="workflow-approval-container">
    <!-- Header Page -->
    <div class="page-header">
      <div class="header-left">
        <div class="header-badge">
          <el-icon><Stamp /></el-icon>
          <span>QUY TRÌNH & KIỂM TOÁN V3.0</span>
        </div>
        <h1 class="page-title">Động Cơ Phê Duyệt Nghiệp Vụ & Sổ Kiểm Toán Bất Biến</h1>
        <p class="page-subtitle">
          Ma trận phê duyệt đa cấp theo hạn mức, phê duyệt 1-click tức thời và nhật ký kiểm toán Append-Only minh bạch chuẩn Doanh nghiệp.
        </p>
      </div>
      <div class="header-right">
        <el-button class="action-btn" :icon="Refresh" @click="handleRefreshAll" :loading="loading">
          Làm mới
        </el-button>
        <el-button type="primary" class="action-btn-primary" :icon="Plus" @click="openSubmitDialog">
          Gửi Yêu Cầu Duyệt
        </el-button>
      </div>
    </div>

    <!-- 4 KPI Summary Cards -->
    <div class="kpi-grid">
      <div class="kpi-card kpi-pending">
        <div class="kpi-info">
          <div class="kpi-label">ĐANG CHỜ DUYỆT</div>
          <div class="kpi-value text-amber">{{ stats.pending ?? 0 }}</div>
          <div class="kpi-sub">Cần cấp có thẩm quyền xử lý</div>
        </div>
        <div class="kpi-icon-wrap bg-amber">
          <el-icon :size="24"><Timer /></el-icon>
        </div>
      </div>

      <div class="kpi-card kpi-approved">
        <div class="kpi-info">
          <div class="kpi-label">ĐÃ PHÊ DUYỆT</div>
          <div class="kpi-value text-emerald">{{ stats.approved ?? 0 }}</div>
          <div class="kpi-sub">Đã hoàn tất tất cả các cấp</div>
        </div>
        <div class="kpi-icon-wrap bg-emerald">
          <el-icon :size="24"><Check /></el-icon>
        </div>
      </div>

      <div class="kpi-card kpi-rejected">
        <div class="kpi-info">
          <div class="kpi-label">BỊ TỪ CHỐI</div>
          <div class="kpi-value text-rose">{{ stats.rejected ?? 0 }}</div>
          <div class="kpi-sub">Chứng từ hoàn trả kèm lý do</div>
        </div>
        <div class="kpi-icon-wrap bg-rose">
          <el-icon :size="24"><Close /></el-icon>
        </div>
      </div>

      <div class="kpi-card kpi-total">
        <div class="kpi-info">
          <div class="kpi-label">TỔNG YÊU CẦU</div>
          <div class="kpi-value text-blue">{{ stats.total ?? 0 }}</div>
          <div class="kpi-sub">Giao dịch trong quy trình</div>
        </div>
        <div class="kpi-icon-wrap bg-blue">
          <el-icon :size="24"><DocumentCopy /></el-icon>
        </div>
      </div>
    </div>

    <!-- Main Navigation Tabs -->
    <el-tabs v-model="activeTab" class="workflow-tabs" @tab-click="handleTabChange">
      <!-- TAB 1: HÀNG ĐỢI PHÊ DUYỆT (APPROVAL INBOX) -->
      <el-tab-pane name="inbox">
        <template #label>
          <div class="tab-label-custom">
            <el-icon><Tickets /></el-icon>
            <span>Hàng Đợi Phê Duyệt</span>
            <el-badge v-if="(stats.pending ?? 0) > 0" :value="stats.pending" class="tab-badge" />
          </div>
        </template>

        <!-- Filters Bar -->
        <div class="filter-card">
          <div class="filter-left">
            <el-input
              v-model="searchKeyword"
              placeholder="Tìm theo mã yêu cầu, mã chứng từ, tiêu đề..."
              prefix-icon="Search"
              clearable
              class="search-input"
              @clear="loadApprovals"
              @keyup.enter="loadApprovals"
            />
            <el-select
              v-model="filterStatus"
              placeholder="Trạng thái duyệt"
              clearable
              class="filter-select"
              @change="loadApprovals"
            >
              <el-option label="Tất cả trạng thái" value="" />
              <el-option label="Chờ phê duyệt (PENDING)" value="PENDING" />
              <el-option label="Đã phê duyệt (APPROVED)" value="APPROVED" />
              <el-option label="Đã từ chối (REJECTED)" value="REJECTED" />
              <el-option label="Đã hủy (CANCELLED)" value="CANCELLED" />
            </el-select>

            <el-select
              v-model="filterDocType"
              placeholder="Loại chứng từ"
              clearable
              class="filter-select"
              @change="loadApprovals"
            >
              <el-option label="Tất cả chứng từ" value="" />
              <el-option label="Đơn mua hàng (PO)" value="PURCHASE_ORDER" />
              <el-option label="Phiếu chi Quỹ (Payment)" value="PAYMENT_VOUCHER" />
              <el-option label="Chiết khấu POS đặc biệt" value="SPECIAL_DISCOUNT" />
            </el-select>
          </div>
          <div class="filter-right">
            <el-button type="primary" plain :icon="Search" @click="loadApprovals">Lọc</el-button>
          </div>
        </div>

        <!-- Approvals Table -->
        <div class="table-card">
          <el-table
            v-loading="tableLoading"
            :data="approvalList"
            stripe
            class="approval-table"
            empty-text="Chưa có yêu cầu phê duyệt nào phù hợp bộ lọc"
          >
            <el-table-column prop="requestCode" label="Mã Yêu Cầu" width="170">
              <template #default="{ row }">
                <div class="request-code-cell">
                  <span class="code-text">{{ row.requestCode }}</span>
                  <el-tooltip content="Sao chép mã" placement="top">
                    <el-icon class="copy-icon" @click="copyText(row.requestCode)"><CopyDocument /></el-icon>
                  </el-tooltip>
                </div>
              </template>
            </el-table-column>

            <el-table-column label="Chứng Từ Gốc" width="170">
              <template #default="{ row }">
                <div class="doc-cell">
                  <el-tag :type="getDocTypeTag(row.documentType)" size="small" effect="light">
                    {{ getDocTypeLabel(row.documentType) }}
                  </el-tag>
                  <div class="doc-code">{{ row.documentCode }}</div>
                </div>
              </template>
            </el-table-column>

            <el-table-column prop="title" label="Tiêu Đề / Nội Dung" min-width="240">
              <template #default="{ row }">
                <div class="title-cell">
                  <div class="main-title">{{ row.title }}</div>
                  <div v-if="row.decisionReason" class="decision-reason">
                    <span class="reason-label">Ghi chú:</span> {{ row.decisionReason }}
                  </div>
                </div>
              </template>
            </el-table-column>

            <el-table-column label="Số Tiền" width="160" align="right">
              <template #default="{ row }">
                <span class="amount-text">{{ formatCurrency(row.amount) }}</span>
              </template>
            </el-table-column>

            <el-table-column label="Tiến Trình Duyệt" width="180">
              <template #default="{ row }">
                <div class="level-cell">
                  <div class="level-badge" :class="getLevelBadgeClass(row)">
                    Cấp {{ row.currentLevel }} / {{ row.totalLevels }}
                  </div>
                  <div class="approver-role-text">
                    {{ formatRoleName(row.currentApproverRole) }}
                  </div>
                </div>
              </template>
            </el-table-column>

            <el-table-column label="Trạng Thái" width="140" align="center">
              <template #default="{ row }">
                <el-tag :type="getStatusTag(row.status)" effect="dark" class="status-tag">
                  {{ row.statusLabel || row.status }}
                </el-tag>
              </template>
            </el-table-column>

            <el-table-column label="Người Gửi & Ngày Tạo" width="180">
              <template #default="{ row }">
                <div class="requester-cell">
                  <div class="req-name">{{ row.requesterFullName || row.requesterUsername }}</div>
                  <div class="req-time">{{ formatDateTime(row.createdAt) }}</div>
                </div>
              </template>
            </el-table-column>

            <el-table-column label="Thao Tác" width="220" fixed="right" align="center">
              <template #default="{ row }">
                <div class="action-buttons">
                  <el-tooltip content="Xem tiến trình chi tiết" placement="top">
                    <el-button circle size="small" :icon="View" @click="viewDetail(row)" />
                  </el-tooltip>

                  <!-- Nút Phê duyệt / Từ chối nếu đang PENDING -->
                  <template v-if="row.status === 'PENDING'">
                    <el-button
                      type="success"
                      size="small"
                      :icon="Check"
                      @click="openDecisionDialog(row, 'APPROVE')"
                    >
                      Duyệt
                    </el-button>
                    <el-button
                      type="danger"
                      size="small"
                      plain
                      :icon="Close"
                      @click="openDecisionDialog(row, 'REJECT')"
                    >
                      Từ chối
                    </el-button>
                  </template>

                  <!-- One-time Token Action Link -->
                  <el-tooltip v-if="row.oneTimeToken && row.status === 'PENDING'" content="Copy link 1-Click Approve" placement="top">
                    <el-button circle size="small" type="warning" plain :icon="Link" @click="copyOneTimeLink(row)" />
                  </el-tooltip>
                </div>
              </template>
            </el-table-column>
          </el-table>

          <!-- Pagination -->
          <div class="pagination-wrap">
            <el-pagination
              v-model:current-page="currentPage"
              v-model:page-size="pageSize"
              :total="totalElements"
              :page-sizes="[10, 15, 30, 50]"
              layout="total, sizes, prev, pager, next, jumper"
              @size-change="loadApprovals"
              @current-change="loadApprovals"
            />
          </div>
        </div>
      </el-tab-pane>

      <!-- TAB 2: MA TRẬN QUY TẮC PHÊ DUYỆT (APPROVAL MATRIX RULES) -->
      <el-tab-pane name="matrix">
        <template #label>
          <div class="tab-label-custom">
            <el-icon><Operation /></el-icon>
            <span>Ma Trận Quy Tắc Động</span>
          </div>
        </template>

        <div class="matrix-card">
          <div class="matrix-header">
            <div>
              <h3 class="matrix-title">Cấu Hình Ma Trận Hạn Mức & Cấp Bậc Phê Duyệt</h3>
              <p class="matrix-desc">
                Hệ thống tự động so khớp giá trị chứng từ với các ngưỡng định mức để kích hoạt luồng phê duyệt 1 cấp, đa cấp hoặc tự động duyệt.
              </p>
            </div>
            <el-button type="primary" :icon="Plus" @click="openRuleDialog()">Thêm Quy Tắc Mới</el-button>
          </div>

          <el-table :data="ruleList" stripe v-loading="rulesLoading" class="rule-table">
            <el-table-column prop="priority" label="Ưu Tiên" width="90" align="center" />
            <el-table-column prop="ruleName" label="Tên Quy Tắc" min-width="220" />
            <el-table-column label="Loại Chứng Từ" width="180">
              <template #default="{ row }">
                <el-tag :type="getDocTypeTag(row.documentType)" size="small">
                  {{ getDocTypeLabel(row.documentType) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="Hạn Mức Định Mức" min-width="220">
              <template #default="{ row }">
                <span class="range-text">
                  {{ formatCurrency(row.minAmount) }}
                  <span class="arrow-sep">➔</span>
                  {{ row.maxAmount ? formatCurrency(row.maxAmount) : 'Không giới hạn trên' }}
                </span>
              </template>
            </el-table-column>
            <el-table-column label="Cấp Độ Duyệt" width="130" align="center">
              <template #default="{ row }">
                <el-tag effect="plain" type="info">Cấp {{ row.level }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="Vai Trò Phê Duyệt" width="180">
              <template #default="{ row }">
                <div class="role-tag">{{ formatRoleName(row.approverRole) }}</div>
              </template>
            </el-table-column>
            <el-table-column label="Tự Động Duyệt" width="140" align="center">
              <template #default="{ row }">
                <el-tag v-if="row.autoApprove" type="success" effect="dark" size="small">⚡ Auto-Approve</el-tag>
                <el-tag v-else type="info" size="small">Thủ công</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="Trạng Thái" width="110" align="center">
              <template #default="{ row }">
                <el-switch v-model="row.active" disabled size="small" />
              </template>
            </el-table-column>
            <el-table-column label="Thao Tác" width="120" align="center" fixed="right">
              <template #default="{ row }">
                <el-button type="primary" link size="small" @click="openRuleDialog(row)">Sửa</el-button>
                <el-button type="danger" link size="small" @click="deleteRule(row)">Xóa</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>

      <!-- TAB 3: SỔ NHẬT KÝ KIỂM TOÁN BẤT BIẾN (IMMUTABLE AUDIT TRAIL) -->
      <el-tab-pane name="audit">
        <template #label>
          <div class="tab-label-custom">
            <el-icon><ShieldCheck /></el-icon>
            <span>Nhật Ký Kiểm Toán (Audit Trail)</span>
          </div>
        </template>

        <!-- Audit Banner Alert -->
        <div class="audit-banner">
          <div class="audit-banner-icon">
            <el-icon :size="24"><Lock /></el-icon>
          </div>
          <div class="audit-banner-text">
            <h4>Sổ Nhật Ký Kiểm Toán Bất Biến (Immutable Append-Only Log)</h4>
            <p>
              Toàn bộ thao tác Tạo, Sửa, Duyệt, Từ chối và Tự động duyệt đều được ghi lại vĩnh viễn kèm IP, User-Agent và Snapshot dữ liệu. Không ai có quyền chỉnh sửa hoặc xóa dữ liệu này.
            </p>
          </div>
        </div>

        <!-- Audit Filters -->
        <div class="filter-card">
          <div class="filter-left">
            <el-input
              v-model="auditSearchUsername"
              placeholder="Tìm theo username..."
              prefix-icon="User"
              clearable
              class="search-input"
              @clear="loadAuditLogs"
              @keyup.enter="loadAuditLogs"
            />
            <el-select
              v-model="auditFilterEntity"
              placeholder="Đối tượng kiểm toán"
              clearable
              class="filter-select"
              @change="loadAuditLogs"
            >
              <el-option label="Tất cả đối tượng" value="" />
              <el-option label="Đơn mua hàng PO" value="PURCHASE_ORDER" />
              <el-option label="Phiếu chi Quỹ" value="PAYMENT_VOUCHER" />
              <el-option label="Chiết khấu đặc biệt" value="SPECIAL_DISCOUNT" />
              <el-option label="Yêu cầu phê duyệt" value="APPROVAL_REQUEST" />
            </el-select>
            <el-select
              v-model="auditFilterAction"
              placeholder="Hành động"
              clearable
              class="filter-select"
              @change="loadAuditLogs"
            >
              <el-option label="Tất cả hành động" value="" />
              <el-option label="Tạo / Gửi duyệt (SUBMIT)" value="SUBMIT" />
              <el-option label="Phê duyệt (APPROVE)" value="APPROVE" />
              <el-option label="Tự động duyệt (AUTO_APPROVE)" value="AUTO_APPROVE" />
              <el-option label="Từ chối (REJECT)" value="REJECT" />
            </el-select>
          </div>
          <div class="filter-right">
            <el-button type="primary" plain :icon="Search" @click="loadAuditLogs">Lọc Nhật Ký</el-button>
          </div>
        </div>

        <!-- Audit Table -->
        <div class="table-card">
          <el-table :data="auditLogs" stripe v-loading="auditLoading" class="audit-table">
            <el-table-column prop="traceId" label="Trace ID" width="140">
              <template #default="{ row }">
                <span class="trace-code">{{ row.traceId }}</span>
              </template>
            </el-table-column>

            <el-table-column label="Thời Điểm" width="170">
              <template #default="{ row }">
                <div class="time-text">{{ formatDateTime(row.timestamp) }}</div>
              </template>
            </el-table-column>

            <el-table-column label="Người Thực Hiện" width="180">
              <template #default="{ row }">
                <div class="user-block">
                  <div class="user-name">@{{ row.username }}</div>
                  <div class="user-role-badge">{{ row.userRole }}</div>
                </div>
              </template>
            </el-table-column>

            <el-table-column label="Hành Động" width="140" align="center">
              <template #default="{ row }">
                <el-tag :type="getAuditActionTag(row.action)" effect="dark" size="small">
                  {{ row.action }}
                </el-tag>
              </template>
            </el-table-column>

            <el-table-column label="Đối Tượng Chứng Từ" width="180">
              <template #default="{ row }">
                <div class="entity-block">
                  <span class="entity-type">{{ row.entityType }}</span>
                  <span class="entity-code">#{{ row.entityCode || row.entityId }}</span>
                </div>
              </template>
            </el-table-column>

            <el-table-column prop="reason" label="Lý Do / Ghi Chú" min-width="200" />

            <el-table-column label="IP & Thiết Bị" width="160">
              <template #default="{ row }">
                <div class="ip-info">
                  <div class="ip-address">{{ row.ipAddress || '127.0.0.1' }}</div>
                  <div class="user-agent-cut" :title="row.userAgent">{{ row.userAgent }}</div>
                </div>
              </template>
            </el-table-column>

            <el-table-column label="Snapshot Diff" width="110" align="center">
              <template #default="{ row }">
                <el-button
                  v-if="row.oldValueJson || row.newValueJson"
                  type="primary"
                  link
                  size="small"
                  @click="openSnapshotModal(row)"
                >
                  Xem Diff
                </el-button>
                <span v-else class="text-muted">—</span>
              </template>
            </el-table-column>
          </el-table>

          <!-- Audit Pagination -->
          <div class="pagination-wrap">
            <el-pagination
              v-model:current-page="auditCurrentPage"
              v-model:page-size="auditPageSize"
              :total="auditTotalElements"
              :page-sizes="[15, 30, 50, 100]"
              layout="total, sizes, prev, pager, next, jumper"
              @size-change="loadAuditLogs"
              @current-change="loadAuditLogs"
            />
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- DRAWER: CHI TIẾT YÊU CẦU & LỊCH SỬ TIẾN TRÌNH CÁC BƯỚC -->
    <el-drawer
      v-model="detailDrawerVisible"
      title="Chi Tiết Quy Trình Phê Duyệt"
      size="560px"
      direction="rtl"
      destroy-on-close
    >
      <div v-if="selectedApproval" class="detail-drawer-content">
        <!-- Top Status Card -->
        <div class="drawer-header-card">
          <div class="header-status-badge">
            <el-tag :type="getStatusTag(selectedApproval.status)" effect="dark" size="large">
              {{ selectedApproval.statusLabel }}
            </el-tag>
          </div>
          <h2 class="drawer-req-code">{{ selectedApproval.requestCode }}</h2>
          <div class="drawer-title-sub">{{ selectedApproval.title }}</div>
          <div class="drawer-amount">{{ formatCurrency(selectedApproval.amount) }}</div>
        </div>

        <!-- Info Grid -->
        <div class="drawer-info-grid">
          <div class="info-row">
            <span class="info-label">Loại chứng từ:</span>
            <span class="info-value font-semibold">{{ getDocTypeLabel(selectedApproval.documentType) }}</span>
          </div>
          <div class="info-row">
            <span class="info-label">Mã chứng từ:</span>
            <span class="info-value text-blue">{{ selectedApproval.documentCode }}</span>
          </div>
          <div class="info-row">
            <span class="info-label">Tiến trình hiện tại:</span>
            <span class="info-value">Cấp {{ selectedApproval.currentLevel }} / {{ selectedApproval.totalLevels }}</span>
          </div>
          <div class="info-row">
            <span class="info-label">Thẩm quyền duyệt:</span>
            <span class="info-value">{{ formatRoleName(selectedApproval.currentApproverRole) }}</span>
          </div>
          <div class="info-row">
            <span class="info-label">Người đề xuất:</span>
            <span class="info-value">{{ selectedApproval.requesterFullName || selectedApproval.requesterUsername }}</span>
          </div>
          <div class="info-row">
            <span class="info-label">Thời điểm gửi:</span>
            <span class="info-value">{{ formatDateTime(selectedApproval.createdAt) }}</span>
          </div>
          <div v-if="selectedApproval.decisionReason" class="info-row full-width">
            <span class="info-label">Ghi chú quyết định:</span>
            <span class="info-value text-highlight">{{ selectedApproval.decisionReason }}</span>
          </div>
        </div>

        <!-- Timeline of Steps -->
        <div class="timeline-section">
          <h3 class="section-title">Nhật Ký Các Bước Phê Duyệt</h3>
          <el-timeline class="approval-timeline">
            <el-timeline-item
              v-for="(step, idx) in stepHistories"
              :key="idx"
              :type="getStepTimelineType(step.action)"
              :timestamp="formatDateTime(step.actionTime)"
              placement="top"
            >
              <div class="timeline-box">
                <div class="timeline-step-header">
                  <span class="step-badge">Bước: {{ step.stepLevel === 0 ? 'Khởi tạo' : 'Cấp ' + step.stepLevel }}</span>
                  <el-tag size="small" :type="getStepActionTag(step.action)">{{ step.action }}</el-tag>
                </div>
                <div class="step-user">
                  <strong>{{ step.approverFullName || step.approverUsername }}</strong> ({{ formatRoleName(step.approverRole) }})
                </div>
                <div v-if="step.comment" class="step-comment">"{{ step.comment }}"</div>
              </div>
            </el-timeline-item>
          </el-timeline>
        </div>

        <!-- Drawer Bottom Action if Pending -->
        <div v-if="selectedApproval.status === 'PENDING'" class="drawer-actions">
          <el-button type="success" size="large" :icon="Check" @click="openDecisionDialog(selectedApproval, 'APPROVE')">
            Phê Duyệt Ngay
          </el-button>
          <el-button type="danger" size="large" plain :icon="Close" @click="openDecisionDialog(selectedApproval, 'REJECT')">
            Từ Chối
          </el-button>
        </div>
      </div>
    </el-drawer>

    <!-- DIALOG: QUYẾT ĐỊNH PHÊ DUYỆT HOẶC TỪ CHỐI -->
    <el-dialog
      v-model="decisionDialogVisible"
      :title="decisionAction === 'APPROVE' ? 'Xác Nhận Phê Duyệt Yêu Cầu' : 'Xác Nhận Từ Chối Yêu Cầu'"
      width="480px"
      destroy-on-close
    >
      <div class="decision-content">
        <p class="decision-desc">
          Bạn đang thực hiện thao tác
          <strong :class="decisionAction === 'APPROVE' ? 'text-emerald' : 'text-rose'">
            {{ decisionAction === 'APPROVE' ? 'PHÊ DUYỆT' : 'TỪ CHỐI' }}
          </strong>
          cho yêu cầu: <strong>{{ targetApproval?.requestCode }}</strong>.
        </p>
        <el-form :model="decisionForm" label-position="top">
          <el-form-item
            :label="decisionAction === 'APPROVE' ? 'Ghi chú / Nhận xét (Không bắt buộc)' : 'Lý do từ chối (Bắt buộc)'"
            :required="decisionAction === 'REJECT'"
          >
            <el-input
              v-model="decisionForm.comment"
              type="textarea"
              :rows="3"
              :placeholder="decisionAction === 'APPROVE' ? 'Nhập ghi chú hoặc lý do chấp thuận...' : 'Vui lòng nêu rõ lý do từ chối để bộ phận gửi biết và chỉnh sửa...'"
            />
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="decisionDialogVisible = false">Hủy bỏ</el-button>
          <el-button
            :type="decisionAction === 'APPROVE' ? 'success' : 'danger'"
            :loading="decisionSubmitting"
            @click="submitDecision"
          >
            {{ decisionAction === 'APPROVE' ? 'Xác Nhận Duyệt' : 'Xác Nhận Từ Chối' }}
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- DIALOG: GỬI YÊU CẦU PHÊ DUYỆT MỚI (TEST & THỰC THI) -->
    <el-dialog
      v-model="submitDialogVisible"
      title="Khởi Tạo Yêu Cầu Phê Duyệt Mới"
      width="540px"
      destroy-on-close
    >
      <el-form :model="submitForm" label-position="top" class="submit-form">
        <el-form-item label="Loại chứng từ cần duyệt" required>
          <el-select v-model="submitForm.documentType" class="w-full">
            <el-option label="Đơn mua hàng Nhà Cung Cấp (Purchase Order)" value="PURCHASE_ORDER" />
            <el-option label="Phiếu chi Quỹ Tài Chính (Payment Voucher)" value="PAYMENT_VOUCHER" />
            <el-option label="Chiết khấu bán hàng POS Đặc biệt (Special Discount)" value="SPECIAL_DISCOUNT" />
          </el-select>
        </el-form-item>

        <el-row :gutter="14">
          <el-col :span="12">
            <el-form-item label="ID Chứng Từ" required>
              <el-input-number v-model="submitForm.documentId" :min="1" class="w-full" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="Mã Chứng Từ Gốc" required>
              <el-input v-model="submitForm.documentCode" placeholder="PO-20261125-001 hoặc PC-001" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="Tiêu đề yêu cầu" required>
          <el-input v-model="submitForm.title" placeholder="Ví dụ: Duyệt mua 50 máy ảnh Canon EOS R6" />
        </el-form-item>

        <el-form-item label="Số tiền yêu cầu duyệt (VND)" required>
          <el-input-number
            v-model="submitForm.amount"
            :min="0"
            :step="5000000"
            class="w-full"
            :formatter="value => `${value}`.replace(/\B(?=(\d{3})+(?!\d))/g, ',')"
            :parser="value => value.replace(/\$\s?|(,*)/g, '')"
          />
          <div class="input-hint">
            Hệ thống sẽ dựa vào số tiền này để so khớp ma trận và quyết định: Tự động duyệt hoặc chuyển qua các cấp.
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="submitDialogVisible = false">Đóng</el-button>
          <el-button type="primary" :loading="submitLoading" @click="handleCreateApproval">
            Gửi Phê Duyệt Ngay
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- DIALOG: THÊM / SỬA QUY TẮC MA TRẬN PHÊ DUYỆT -->
    <el-dialog
      v-model="ruleDialogVisible"
      :title="ruleForm.id ? 'Cập Nhật Quy Tắc Ma Trận' : 'Thêm Mới Quy Tắc Ma Trận Phê Duyệt'"
      width="540px"
      destroy-on-close
    >
      <el-form :model="ruleForm" label-position="top">
        <el-form-item label="Tên quy tắc" required>
          <el-input v-model="ruleForm.ruleName" placeholder="Ví dụ: PO từ 20 đến 100 triệu [Cấp 1]" />
        </el-form-item>

        <el-row :gutter="14">
          <el-col :span="14">
            <el-form-item label="Loại chứng từ áp dụng" required>
              <el-select v-model="ruleForm.documentType" class="w-full">
                <el-option label="Đơn mua hàng PO" value="PURCHASE_ORDER" />
                <el-option label="Phiếu chi Quỹ" value="PAYMENT_VOUCHER" />
                <el-option label="Chiết khấu đặc biệt" value="SPECIAL_DISCOUNT" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="10">
            <el-form-item label="Độ ưu tiên" required>
              <el-input-number v-model="ruleForm.priority" :min="1" class="w-full" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="14">
          <el-col :span="12">
            <el-form-item label="Hạn mức tối thiểu (VND)" required>
              <el-input-number v-model="ruleForm.minAmount" :min="0" class="w-full" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="Hạn mức tối đa (Bỏ trống nếu không giới hạn)">
              <el-input-number v-model="ruleForm.maxAmount" :min="0" class="w-full" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="14">
          <el-col :span="10">
            <el-form-item label="Cấp độ duyệt" required>
              <el-input-number v-model="ruleForm.level" :min="1" :max="5" class="w-full" />
            </el-form-item>
          </el-col>
          <el-col :span="14">
            <el-form-item label="Vai trò người duyệt" required>
              <el-select v-model="ruleForm.approverRole" class="w-full">
                <el-option label="Trưởng phòng / Quản lý (ROLE_MANAGER)" value="ROLE_MANAGER" />
                <el-option label="Kế toán trưởng / CFO (ROLE_ACCOUNTANT)" value="ROLE_ACCOUNTANT" />
                <el-option label="Tổng Giám đốc / CEO (ROLE_ADMIN)" value="ROLE_ADMIN" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item>
          <el-checkbox v-model="ruleForm.autoApprove">
            <strong>Tự động phê duyệt (Auto-Approve)</strong> khi thỏa mãn ngưỡng này mà không cần chờ người duyệt
          </el-checkbox>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="ruleDialogVisible = false">Đóng</el-button>
          <el-button type="primary" :loading="ruleSaving" @click="saveRule">
            Lưu Quy Tắc
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- MODAL: XEM SNAPSHOT DIFF NHẬT KÝ KIỂM TOÁN -->
    <el-dialog v-model="snapshotDialogVisible" title="Dữ Liệu Snapshot Kiểm Toán (Diff)" width="640px" destroy-on-close>
      <div v-if="selectedAuditLog" class="snapshot-content">
        <div class="snapshot-header">
          <span class="trace-badge">Trace: {{ selectedAuditLog.traceId }}</span>
          <span class="snapshot-time">{{ formatDateTime(selectedAuditLog.timestamp) }}</span>
        </div>
        <div class="diff-container">
          <div class="diff-block">
            <h4 class="diff-title text-rose">Dữ Liệu Trước Khi Đổi (Old Value)</h4>
            <pre class="json-box">{{ formatJson(selectedAuditLog.oldValueJson) }}</pre>
          </div>
          <div class="diff-block">
            <h4 class="diff-title text-emerald">Dữ Liệu Sau Khi Đổi (New Value)</h4>
            <pre class="json-box">{{ formatJson(selectedAuditLog.newValueJson) }}</pre>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
// =========================================================================
// 1. HELPER FUNCTIONS HOISTED ĐẦU TIÊN ĐỂ TRÁNH LỖI TDZ (TEMPORAL DEAD ZONE)
// =========================================================================
const formatCurrency = (val) => {
  if (val === null || val === undefined) return '0 ₫'
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(val)
}

const formatDateTime = (val) => {
  if (!val) return '—'
  const d = new Date(val)
  return d.toLocaleString('vi-VN', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
}

const formatJson = (jsonStr) => {
  if (!jsonStr) return 'Không có dữ liệu (null)'
  try {
    const parsed = JSON.parse(jsonStr)
    return JSON.stringify(parsed, null, 2)
  } catch {
    return jsonStr
  }
}

const getDocTypeLabel = (type) => {
  const map = {
    PURCHASE_ORDER: 'Đơn Mua Hàng PO',
    PAYMENT_VOUCHER: 'Phiếu Chi Quỹ',
    SPECIAL_DISCOUNT: 'Chiết Khấu POS'
  }
  return map[type] || type || 'Chứng từ'
}

const getDocTypeTag = (type) => {
  const map = {
    PURCHASE_ORDER: 'primary',
    PAYMENT_VOUCHER: 'warning',
    SPECIAL_DISCOUNT: 'success'
  }
  return map[type] || 'info'
}

const getStatusTag = (status) => {
  const map = {
    PENDING: 'warning',
    APPROVED: 'success',
    REJECTED: 'danger',
    CANCELLED: 'info'
  }
  return map[status] || 'info'
}

const getLevelBadgeClass = (row) => {
  if (row.status === 'APPROVED') return 'badge-approved'
  if (row.status === 'REJECTED') return 'badge-rejected'
  return 'badge-pending'
}

const formatRoleName = (role) => {
  if (!role) return 'Hệ thống'
  if (role.includes('ADMIN')) return 'Tổng Giám đốc (CEO)'
  if (role.includes('ACCOUNTANT')) return 'Kế toán trưởng (CFO)'
  if (role.includes('MANAGER')) return 'Trưởng phòng / Quản lý'
  if (role.includes('SALES')) return 'Nhân viên Kinh doanh'
  return role
}

const getStepTimelineType = (action) => {
  if (action === 'APPROVE' || action === 'AUTO_APPROVE') return 'success'
  if (action === 'REJECT') return 'danger'
  if (action === 'SUBMIT') return 'primary'
  return 'info'
}

const getStepActionTag = (action) => {
  if (action === 'APPROVE' || action === 'AUTO_APPROVE') return 'success'
  if (action === 'REJECT') return 'danger'
  return 'info'
}

const getAuditActionTag = (action) => {
  if (action?.includes('APPROVE')) return 'success'
  if (action?.includes('REJECT')) return 'danger'
  if (action?.includes('SUBMIT')) return 'primary'
  return 'info'
}

// =========================================================================
// 2. IMPORTS & STATE DECLARATIONS
// =========================================================================
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../api'
import { useRealtimeChatStore } from '../store/realtimeChat'
import {
  Refresh,
  Plus,
  Timer,
  Check,
  Close,
  DocumentCopy,
  Tickets,
  Operation,
  View,
  Link,
  Search,
  CopyDocument,
  Lock
} from '@element-plus/icons-vue'

const chatStore = useRealtimeChatStore()

// Tabs
const activeTab = ref('inbox')
const loading = ref(false)
const tableLoading = ref(false)
const rulesLoading = ref(false)
const auditLoading = ref(false)

// KPI Stats
const stats = reactive({
  pending: 0,
  approved: 0,
  rejected: 0,
  cancelled: 0,
  total: 0
})

// Tab 1: Approval Inbox
const approvalList = ref([])
const searchKeyword = ref('')
const filterStatus = ref('')
const filterDocType = ref('')
const currentPage = ref(1)
const pageSize = ref(15)
const totalElements = ref(0)

// Tab 2: Rules
const ruleList = ref([])
const ruleDialogVisible = ref(false)
const ruleSaving = ref(false)
const ruleForm = reactive({
  id: null,
  ruleName: '',
  documentType: 'PURCHASE_ORDER',
  minAmount: 0,
  maxAmount: null,
  level: 1,
  approverRole: 'ROLE_MANAGER',
  autoApprove: false,
  priority: 1,
  active: true
})

// Tab 3: Audit Logs
const auditLogs = ref([])
const auditSearchUsername = ref('')
const auditFilterEntity = ref('')
const auditFilterAction = ref('')
const auditCurrentPage = ref(1)
const auditPageSize = ref(20)
const auditTotalElements = ref(0)
const snapshotDialogVisible = ref(false)
const selectedAuditLog = ref(null)

// Detail Drawer
const detailDrawerVisible = ref(false)
const selectedApproval = ref(null)
const stepHistories = ref([])

// Decision Dialog
const decisionDialogVisible = ref(false)
const decisionAction = ref('APPROVE')
const targetApproval = ref(null)
const decisionSubmitting = ref(false)
const decisionForm = reactive({
  comment: ''
})

// Submit Dialog
const submitDialogVisible = ref(false)
const submitLoading = ref(false)
const submitForm = reactive({
  documentType: 'PURCHASE_ORDER',
  documentId: 1,
  documentCode: 'PO-20261125-001',
  title: 'Duyệt đơn mua hàng phụ kiện máy ảnh',
  amount: 45000000,
  currency: 'VND'
})

// =========================================================================
// 3. API CALLS & DATA FETCHING
// =========================================================================
const loadStats = async () => {
  try {
    const res = await api.get('/workflow/approvals/stats')
    if (res?.data) {
      stats.pending = res.data.pending ?? 0
      stats.approved = res.data.approved ?? 0
      stats.rejected = res.data.rejected ?? 0
      stats.cancelled = res.data.cancelled ?? 0
      stats.total = res.data.total ?? 0
    }
  } catch (err) {
    console.error('Lỗi khi tải thống kê phê duyệt:', err)
  }
}

const loadApprovals = async () => {
  tableLoading.value = true
  try {
    const params = {
      page: currentPage.value - 1,
      size: pageSize.value
    }
    if (filterStatus.value) params.status = filterStatus.value
    if (filterDocType.value) params.docType = filterDocType.value
    if (searchKeyword.value?.trim()) params.search = searchKeyword.value.trim()

    const res = await api.get('/workflow/approvals', { params })
    if (res?.data) {
      approvalList.value = res.data.content ?? []
      totalElements.value = res.data.totalElements ?? 0
    }
  } catch (err) {
    console.error('Lỗi tải danh sách phê duyệt:', err)
  } finally {
    tableLoading.value = false
  }
}

const loadRules = async () => {
  rulesLoading.value = true
  try {
    const res = await api.get('/workflow/rules')
    ruleList.value = res?.data ?? []
  } catch (err) {
    console.error('Lỗi tải ma trận quy tắc:', err)
  } finally {
    rulesLoading.value = false
  }
}

const loadAuditLogs = async () => {
  auditLoading.value = true
  try {
    const params = {
      page: auditCurrentPage.value - 1,
      size: auditPageSize.value
    }
    if (auditFilterEntity.value) params.entityType = auditFilterEntity.value
    if (auditSearchUsername.value?.trim()) params.username = auditSearchUsername.value.trim()
    if (auditFilterAction.value) params.action = auditFilterAction.value

    const res = await api.get('/workflow/audit-logs', { params })
    if (res?.data) {
      auditLogs.value = res.data.content ?? []
      auditTotalElements.value = res.data.totalElements ?? 0
    }
  } catch (err) {
    console.error('Lỗi tải nhật ký kiểm toán:', err)
  } finally {
    auditLoading.value = false
  }
}

const handleRefreshAll = async () => {
  loading.value = true
  await Promise.all([loadStats(), loadApprovals()])
  if (activeTab.value === 'matrix') await loadRules()
  if (activeTab.value === 'audit') await loadAuditLogs()
  loading.value = false
  ElMessage.success('Đã làm mới dữ liệu hệ thống phê duyệt')
}

const handleTabChange = (tab) => {
  if (tab.paneName === 'inbox') loadApprovals()
  else if (tab.paneName === 'matrix') loadRules()
  else if (tab.paneName === 'audit') loadAuditLogs()
}

// =========================================================================
// 4. ACTION HANDLERS (APPROVE / REJECT / TOKEN / DETAIL)
// =========================================================================
const viewDetail = async (row) => {
  selectedApproval.value = row
  detailDrawerVisible.value = true
  try {
    const res = await api.get(`/workflow/approvals/${row.id}/steps`)
    stepHistories.value = res?.data ?? []
  } catch (err) {
    console.error('Lỗi nạp lịch sử bước duyệt:', err)
    stepHistories.value = row.stepHistories ?? []
  }
}

const openDecisionDialog = (row, action) => {
  targetApproval.value = row
  decisionAction.value = action
  decisionForm.comment = ''
  decisionDialogVisible.value = true
}

const submitDecision = async () => {
  if (decisionAction.value === 'REJECT' && !decisionForm.comment?.trim()) {
    ElMessage.warning('Vui lòng nhập lý do từ chối để hoàn trả chứng từ!')
    return
  }

  decisionSubmitting.value = true
  try {
    await api.post(`/workflow/approvals/${targetApproval.value.id}/decision`, {
      action: decisionAction.value,
      comment: decisionForm.comment?.trim() || ''
    })

    ElMessage.success(
      decisionAction.value === 'APPROVE'
        ? `Đã phê duyệt thành công yêu cầu ${targetApproval.value.requestCode}!`
        : `Đã từ chối yêu cầu ${targetApproval.value.requestCode}!`
    )

    decisionDialogVisible.value = false
    detailDrawerVisible.value = false
    await Promise.all([loadStats(), loadApprovals()])
  } catch (err) {
    const msg = err.response?.data?.message || err.message || 'Lỗi khi xử lý quyết định'
    ElMessage.error(msg)
  } finally {
    decisionSubmitting.value = false
  }
}

const copyOneTimeLink = (row) => {
  const origin = window.location.origin
  const link = `${origin}/api/workflow/approvals/quick-token-decision?token=${row.oneTimeToken}&action=APPROVE`
  navigator.clipboard.writeText(link)
  ElMessage.success('Đã sao chép liên kết 1-Click Approve (Hạn 24h)!')
}

const copyText = (txt) => {
  if (!txt) return
  navigator.clipboard.writeText(txt)
  ElMessage.success('Đã sao chép mã vào Clipboard')
}

// =========================================================================
// 5. SUBMIT NEW APPROVAL REQUEST
// =========================================================================
const openSubmitDialog = () => {
  submitForm.documentType = 'PURCHASE_ORDER'
  submitForm.documentId = Math.floor(Math.random() * 900) + 100
  submitForm.documentCode = `PO-20261125-${Math.floor(Math.random() * 900) + 100}`
  submitForm.title = 'Trình duyệt đơn mua hàng vật tư máy ảnh Canon'
  submitForm.amount = 45000000
  submitDialogVisible.value = true
}

const handleCreateApproval = async () => {
  if (!submitForm.title?.trim()) {
    ElMessage.warning('Vui lòng nhập tiêu đề yêu cầu!')
    return
  }
  submitLoading.value = true
  try {
    const res = await api.post('/workflow/approvals/submit', {
      ...submitForm,
      payloadSnapshot: JSON.stringify({
        title: submitForm.title,
        amount: submitForm.amount,
        createdAt: new Date().toISOString()
      })
    })

    if (res?.data?.status === 'APPROVED') {
      ElMessage.success('Tự động phê duyệt thành công theo định mức ma trận!')
    } else {
      ElMessage.success(`Khởi tạo yêu cầu ${res?.data?.requestCode} thành công! Đang chờ duyệt cấp 1.`)
    }

    submitDialogVisible.value = false
    await Promise.all([loadStats(), loadApprovals()])
  } catch (err) {
    ElMessage.error(err.response?.data?.message || 'Lỗi gửi yêu cầu phê duyệt')
  } finally {
    submitLoading.value = false
  }
}

// =========================================================================
// 6. RULES CRUD
// =========================================================================
const openRuleDialog = (rule = null) => {
  if (rule) {
    ruleForm.id = rule.id
    ruleForm.ruleName = rule.ruleName
    ruleForm.documentType = rule.documentType
    ruleForm.minAmount = rule.minAmount
    ruleForm.maxAmount = rule.maxAmount
    ruleForm.level = rule.level
    ruleForm.approverRole = rule.approverRole
    ruleForm.autoApprove = rule.autoApprove ?? false
    ruleForm.priority = rule.priority ?? 1
    ruleForm.active = rule.active ?? true
  } else {
    ruleForm.id = null
    ruleForm.ruleName = ''
    ruleForm.documentType = 'PURCHASE_ORDER'
    ruleForm.minAmount = 0
    ruleForm.maxAmount = null
    ruleForm.level = 1
    ruleForm.approverRole = 'ROLE_MANAGER'
    ruleForm.autoApprove = false
    ruleForm.priority = 1
    ruleForm.active = true
  }
  ruleDialogVisible.value = true
}

const saveRule = async () => {
  if (!ruleForm.ruleName?.trim()) {
    ElMessage.warning('Vui lòng nhập tên quy tắc!')
    return
  }
  ruleSaving.value = true
  try {
    await api.post('/workflow/rules', ruleForm)
    ElMessage.success(ruleForm.id ? 'Đã cập nhật quy tắc thành công!' : 'Đã thêm quy tắc mới vào ma trận!')
    ruleDialogVisible.value = false
    await loadRules()
  } catch (err) {
    ElMessage.error(err.response?.data?.message || 'Lỗi lưu quy tắc ma trận')
  } finally {
    ruleSaving.value = false
  }
}

const deleteRule = async (rule) => {
  try {
    await ElMessageBox.confirm(`Bạn có chắc muốn xóa quy tắc "${rule.ruleName}"?`, 'Xác nhận xóa', {
      confirmButtonText: 'Xóa',
      cancelButtonText: 'Hủy',
      type: 'warning'
    })
    await api.delete(`/workflow/rules/${rule.id}`)
    ElMessage.success('Đã xóa quy tắc khỏi hệ thống')
    await loadRules()
  } catch {
    // Cancelled
  }
}

// =========================================================================
// 7. AUDIT LOGS SNAPSHOT
// =========================================================================
const openSnapshotModal = (log) => {
  selectedAuditLog.value = log
  snapshotDialogVisible.value = true
}

// =========================================================================
// 8. LIFECYCLE & WEBSOCKET REALTIME
// =========================================================================
let stompSubscription = null

onMounted(async () => {
  await Promise.all([loadStats(), loadApprovals()])

  // Tích hợp STOMP Realtime nếu kết nối đang mở
  if (chatStore?.stompClient?.connected) {
    try {
      stompSubscription = chatStore.stompClient.subscribe('/topic/approvals', (msg) => {
        try {
          const event = JSON.parse(msg.body)
          console.log('[STOMP-APPROVAL] Nhận sự kiện realtime:', event)
          ElMessage.info({
            message: `Quy trình [${event.requestCode}]: ${event.eventType}`,
            grouping: true
          })
          loadStats()
          loadApprovals()
        } catch (e) {
          console.warn('Lỗi parse STOMP message:', e)
        }
      })
    } catch (e) {
      console.warn('Không thể subscribe /topic/approvals:', e)
    }
  }
})

onUnmounted(() => {
  if (stompSubscription) {
    try {
      stompSubscription.unsubscribe()
    } catch (e) {
      // Ignore
    }
  }
})
</script>

<style scoped>
.workflow-approval-container {
  padding: 24px;
  background-color: #f8fafc;
  min-height: calc(100vh - 64px);
  padding-bottom: 80px;
}

/* Header */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 24px;
  flex-wrap: wrap;
  gap: 16px;
}
.header-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 10px;
  background: #eff6ff;
  color: #2563eb;
  border-radius: 9999px;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.5px;
  margin-bottom: 8px;
}
.page-title {
  font-size: 22px;
  font-weight: 700;
  color: #0f172a;
  margin: 0 0 6px 0;
}
.page-subtitle {
  font-size: 13px;
  color: #64748b;
  margin: 0;
  max-width: 720px;
  line-height: 1.5;
}
.header-right {
  display: flex;
  gap: 12px;
  align-items: center;
}

/* KPI Cards */
.kpi-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(230px, 1fr));
  gap: 16px;
  margin-bottom: 24px;
}
.kpi-card {
  background: #ffffff;
  border-radius: 12px;
  padding: 18px 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border: 1px solid #e2e8f0;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}
.kpi-label {
  font-size: 11px;
  font-weight: 700;
  color: #64748b;
  letter-spacing: 0.5px;
  margin-bottom: 4px;
}
.kpi-value {
  font-size: 26px;
  font-weight: 800;
  line-height: 1.2;
}
.kpi-sub {
  font-size: 12px;
  color: #94a3b8;
  margin-top: 4px;
}
.kpi-icon-wrap {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.text-amber { color: #d97706; }
.text-emerald { color: #059669; }
.text-rose { color: #e11d48; }
.text-blue { color: #2563eb; }
.bg-amber { background: #fef3c7; color: #d97706; }
.bg-emerald { background: #d1fae5; color: #059669; }
.bg-rose { background: #ffe4e6; color: #e11d48; }
.bg-blue { background: #dbeafe; color: #2563eb; }

/* Tabs */
.workflow-tabs :deep(.el-tabs__header) {
  margin-bottom: 20px;
}
.tab-label-custom {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 600;
}
.tab-badge {
  margin-left: 4px;
}

/* Filters */
.filter-card {
  background: #ffffff;
  border-radius: 12px;
  padding: 14px 18px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
  border: 1px solid #e2e8f0;
  flex-wrap: wrap;
}
.filter-left {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  flex: 1;
}
.search-input {
  width: 320px;
}
.filter-select {
  width: 200px;
}

/* Tables */
.table-card, .matrix-card {
  background: #ffffff;
  border-radius: 12px;
  padding: 16px 20px;
  border: 1px solid #e2e8f0;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}
.request-code-cell {
  display: flex;
  align-items: center;
  gap: 6px;
}
.code-text {
  font-weight: 700;
  color: #0f172a;
}
.copy-icon {
  cursor: pointer;
  color: #94a3b8;
  transition: color 0.2s;
}
.copy-icon:hover {
  color: #2563eb;
}
.doc-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.doc-code {
  font-size: 12px;
  color: #64748b;
  font-family: monospace;
}
.main-title {
  font-weight: 600;
  color: #1e293b;
  margin-bottom: 2px;
}
.decision-reason {
  font-size: 12px;
  color: #e11d48;
}
.amount-text {
  font-weight: 800;
  font-size: 14px;
  color: #0f172a;
}
.level-cell {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.level-badge {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 6px;
  font-size: 11px;
  font-weight: 700;
  width: fit-content;
}
.badge-pending {
  background: #fef3c7;
  color: #b45309;
}
.badge-approved {
  background: #d1fae5;
  color: #047857;
}
.badge-rejected {
  background: #ffe4e6;
  color: #be123c;
}
.approver-role-text {
  font-size: 11px;
  color: #64748b;
}
.requester-cell {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.req-name {
  font-weight: 600;
  font-size: 12px;
  color: #1e293b;
}
.req-time {
  font-size: 11px;
  color: #94a3b8;
}
.action-buttons {
  display: flex;
  gap: 6px;
  justify-content: center;
  align-items: center;
}
.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

/* Matrix Section */
.matrix-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 20px;
  flex-wrap: wrap;
  gap: 12px;
}
.matrix-title {
  font-size: 16px;
  font-weight: 700;
  color: #0f172a;
  margin: 0 0 4px 0;
}
.matrix-desc {
  font-size: 13px;
  color: #64748b;
  margin: 0;
}
.range-text {
  font-weight: 600;
  color: #0f172a;
}
.arrow-sep {
  color: #94a3b8;
  margin: 0 4px;
}
.role-tag {
  font-size: 12px;
  font-weight: 600;
  color: #334155;
}

/* Audit Trail Section */
.audit-banner {
  background: #f0fdf4;
  border: 1px solid #bbf7d0;
  border-radius: 12px;
  padding: 16px 20px;
  display: flex;
  gap: 14px;
  align-items: flex-start;
  margin-bottom: 16px;
}
.audit-banner-icon {
  color: #16a34a;
  margin-top: 2px;
}
.audit-banner-text h4 {
  font-size: 15px;
  font-weight: 700;
  color: #166534;
  margin: 0 0 4px 0;
}
.audit-banner-text p {
  font-size: 13px;
  color: #15803d;
  margin: 0;
  line-height: 1.5;
}
.trace-code {
  font-family: monospace;
  font-weight: 700;
  color: #2563eb;
  background: #eff6ff;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 11px;
}
.user-block {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.user-name {
  font-weight: 600;
  font-size: 12px;
  color: #0f172a;
}
.user-role-badge {
  font-size: 10px;
  color: #64748b;
  font-family: monospace;
}
.entity-block {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.entity-type {
  font-weight: 600;
  font-size: 11px;
  color: #334155;
}
.entity-code {
  font-size: 11px;
  color: #2563eb;
  font-family: monospace;
}
.ip-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.ip-address {
  font-size: 11px;
  font-family: monospace;
  color: #334155;
}
.user-agent-cut {
  font-size: 10px;
  color: #94a3b8;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 140px;
}

/* Detail Drawer Content */
.detail-drawer-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
}
.drawer-header-card {
  background: #f8fafc;
  border-radius: 12px;
  padding: 20px;
  border: 1px solid #e2e8f0;
  text-align: center;
}
.drawer-req-code {
  font-size: 20px;
  font-weight: 800;
  color: #0f172a;
  margin: 8px 0 4px 0;
}
.drawer-title-sub {
  font-size: 13px;
  color: #64748b;
  margin-bottom: 8px;
}
.drawer-amount {
  font-size: 24px;
  font-weight: 800;
  color: #2563eb;
}
.drawer-info-grid {
  display: flex;
  flex-direction: column;
  gap: 10px;
  background: #ffffff;
  border: 1px solid #f1f5f9;
  padding: 16px;
  border-radius: 8px;
}
.info-row {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
}
.info-label {
  color: #64748b;
}
.info-value {
  color: #0f172a;
}
.info-row.full-width {
  flex-direction: column;
  gap: 4px;
  margin-top: 4px;
  border-top: 1px dashed #e2e8f0;
  padding-top: 8px;
}
.timeline-section {
  margin-top: 10px;
}
.section-title {
  font-size: 15px;
  font-weight: 700;
  color: #0f172a;
  margin-bottom: 16px;
}
.timeline-box {
  background: #f8fafc;
  border-radius: 8px;
  padding: 10px 14px;
  border: 1px solid #e2e8f0;
}
.timeline-step-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 4px;
}
.step-badge {
  font-size: 11px;
  font-weight: 700;
  color: #2563eb;
}
.step-user {
  font-size: 12px;
  color: #334155;
}
.step-comment {
  font-size: 12px;
  color: #64748b;
  font-style: italic;
  margin-top: 4px;
}
.drawer-actions {
  display: flex;
  gap: 12px;
  margin-top: 16px;
}
.drawer-actions .el-button {
  flex: 1;
}

/* Snapshot Modal */
.snapshot-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 16px;
}
.trace-badge {
  font-family: monospace;
  font-weight: 700;
  background: #eff6ff;
  color: #2563eb;
  padding: 4px 8px;
  border-radius: 6px;
}
.diff-container {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
}
.diff-title {
  font-size: 13px;
  font-weight: 700;
  margin-bottom: 6px;
}
.json-box {
  background: #0f172a;
  color: #38bdf8;
  padding: 12px;
  border-radius: 8px;
  font-size: 11px;
  font-family: monospace;
  max-height: 240px;
  overflow: auto;
}
.input-hint {
  font-size: 12px;
  color: #64748b;
  margin-top: 4px;
}
.w-full {
  width: 100%;
}
</style>
