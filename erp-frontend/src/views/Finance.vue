<template>
  <div class="finance-page-container">
    <!-- PAGE HEADER -->
    <div class="finance-header-row">
      <div class="header-left">
        <h1 class="page-title">
          <span class="title-icon">💼</span>
          Quản lý Tài chính & Sổ Quỹ Doanh nghiệp
        </h1>
        <p class="page-subtitle">
          Next-Gen Treasury, Hạch toán Thu - Chi, Đối soát Sao kê Ngân hàng & Báo cáo Lãi Lỗ P&L
        </p>
      </div>

      <div class="header-right-actions">
        <el-button 
          type="primary" 
          class="btn-action-primary"
          @click="openCreateVoucherModal('PAYMENT')"
        >
          <el-icon><Minus /></el-icon>
          Lập Phiếu Chi
        </el-button>

        <el-button 
          type="success" 
          class="btn-action-success"
          @click="openCreateVoucherModal('RECEIPT')"
        >
          <el-icon><Plus /></el-icon>
          Lập Phiếu Thu
        </el-button>

        <el-button 
          class="btn-action-neutral"
          @click="openTransferModal"
        >
          <el-icon><Switch /></el-icon>
          Chuyển Quỹ Nội Bộ
        </el-button>

        <el-button 
          class="btn-refresh"
          title="Tải lại dữ liệu"
          @click="refreshAllData"
          :loading="loading"
        >
          <el-icon><Refresh /></el-icon>
        </el-button>
      </div>
    </div>

    <!-- 4 THẺ KPI ĐIỀU HÀNH DÒNG TIỀN (TREASURY METRICS) -->
    <div class="kpi-metrics-grid">
      <!-- 1. Tổng tiền các quỹ -->
      <div class="kpi-card total-balance-card">
        <div class="kpi-card-header">
          <span class="kpi-label">Tổng Tài sản Quỹ (Tiền mặt + Ngân hàng)</span>
          <span class="kpi-icon-wrap bg-blue-subtle">💰</span>
        </div>
        <div class="kpi-value-row">
          <span class="kpi-value text-primary">{{ formatCurrency(totalBalance) }}</span>
        </div>
        <div class="kpi-footer-row">
          <span>{{ accounts.length }} tài khoản quỹ đang hoạt động</span>
        </div>
      </div>

      <!-- 2. Quỹ tiền mặt -->
      <div class="kpi-card">
        <div class="kpi-card-header">
          <span class="kpi-label">Tiền mặt tại quỹ (Cash on Hand)</span>
          <span class="kpi-icon-wrap bg-emerald-subtle">💵</span>
        </div>
        <div class="kpi-value-row">
          <span class="kpi-value text-emerald">{{ formatCurrency(totalCashBalance) }}</span>
        </div>
        <div class="kpi-footer-row">
          <span>Chi tiêu tác nghiệp trực tiếp tại kho & văn phòng</span>
        </div>
      </div>

      <!-- 3. Quỹ ngân hàng -->
      <div class="kpi-card">
        <div class="kpi-card-header">
          <span class="kpi-label">Tài khoản Ngân hàng (Bank Balance)</span>
          <span class="kpi-icon-wrap bg-indigo-subtle">🏦</span>
        </div>
        <div class="kpi-value-row">
          <span class="kpi-value text-indigo">{{ formatCurrency(totalBankBalance) }}</span>
        </div>
        <div class="kpi-footer-row">
          <span>Vietcombank, MBBank, Techcombank</span>
        </div>
      </div>

      <!-- 4. Cash Runway Dòng tiền -->
      <div class="kpi-card runway-card" :class="'is-' + (cashRunway?.riskLevel || 'safe').toLowerCase()">
        <div class="kpi-card-header">
          <span class="kpi-label">Dự báo Dòng tiền (Cash Runway)</span>
          <span class="kpi-risk-badge" :class="'badge-' + (cashRunway?.riskLevel || 'safe').toLowerCase()">
            {{ cashRunway?.runwayDays ?? 99 }} ngày an toàn
          </span>
        </div>
        <div class="kpi-value-row">
          <span class="kpi-value">{{ cashRunway?.runwayDays ?? 99 }} Ngày</span>
        </div>
        <div class="kpi-footer-row">
          <span>Burn rate: <strong>{{ formatCurrency(cashRunway?.dailyBurnRate) }}</strong>/ngày</span>
        </div>
      </div>
    </div>

    <!-- MAIN TABS CONTAINER -->
    <div class="finance-tabs-card">
      <el-tabs v-model="activeTab" class="finance-nav-tabs" @tab-click="handleTabChange">
        <!-- TAB 1: SỔ QUỸ & TÀI KHOẢN -->
        <el-tab-pane label="Sổ Quỹ & Tài Khoản" name="accounts">
          <div class="tab-pane-content">
            <div class="section-title-row">
              <h3 class="section-title">Danh sách Tài khoản Quỹ & Ngân hàng</h3>
              <el-button size="small" type="primary" plain @click="openCreateAccountModal">
                <el-icon><Plus /></el-icon>
                Thêm Tài Khoản Mới
              </el-button>
            </div>

            <!-- Grid Thẻ Tài khoản Quỹ -->
            <div class="accounts-cards-grid">
              <div 
                v-for="acc in accounts" 
                :key="acc.id" 
                class="account-box-card"
                :class="acc.accountType === 'CASH' ? 'is-cash' : 'is-bank'"
              >
                <div class="acc-card-top">
                  <div class="acc-icon-badge">
                    {{ acc.accountType === 'CASH' ? '💵' : '🏦' }}
                  </div>
                  <div class="acc-info-block">
                    <div class="acc-name-text">{{ acc.accountName }}</div>
                    <div class="acc-code-tag">{{ acc.accountCode }}</div>
                  </div>
                  <span class="acc-status-pill" :class="{ 'is-active': acc.active }">
                    {{ acc.active ? 'Hoạt động' : 'Tạm dừng' }}
                  </span>
                </div>

                <div class="acc-card-body">
                  <div v-if="acc.accountNumber" class="acc-meta-line">
                    <span class="meta-label">Số TK:</span>
                    <span class="meta-val font-mono">{{ acc.accountNumber }}</span>
                    <span v-if="acc.bankName" class="meta-sub">({{ acc.bankName }})</span>
                  </div>
                  <div v-if="acc.accountHolder" class="acc-meta-line">
                    <span class="meta-label">Chủ TK:</span>
                    <span class="meta-val">{{ acc.accountHolder }}</span>
                  </div>
                </div>

                <div class="acc-card-footer">
                  <span class="balance-label">Số dư hiện tại:</span>
                  <span class="balance-amount">{{ formatCurrency(acc.currentBalance) }}</span>
                </div>
              </div>
            </div>

            <!-- Nhật ký giao dịch chứng từ gần đây -->
            <div class="section-title-row mt-6">
              <h3 class="section-title">Nhật ký Giao dịch gần nhất</h3>
            </div>
            
            <el-table 
              :data="recentVouchers" 
              style="width: 100%" 
              class="scannable-table"
              v-loading="loadingVouchers"
            >
              <el-table-column label="Mã Chứng Từ" min-width="150">
                <template #default="{ row }">
                  <span class="font-mono font-bold text-primary">{{ row.voucherCode }}</span>
                </template>
              </el-table-column>

              <el-table-column label="Loại Phiếu" width="130">
                <template #default="{ row }">
                  <el-tag :type="getVoucherTagType(row.voucherType)" size="small" effect="plain">
                    {{ row.voucherTypeTitle }}
                  </el-tag>
                </template>
              </el-table-column>

              <el-table-column label="Số Tiền" min-width="140" align="right">
                <template #default="{ row }">
                  <span :class="row.voucherType === 'RECEIPT' ? 'text-emerald font-bold' : (row.voucherType === 'PAYMENT' ? 'text-rose font-bold' : 'text-blue font-bold')">
                    {{ row.voucherType === 'RECEIPT' ? '+' : (row.voucherType === 'PAYMENT' ? '-' : '⇄') }}
                    {{ formatCurrency(row.amount) }}
                  </span>
                </template>
              </el-table-column>

              <el-table-column label="Tài Khoản" min-width="160">
                <template #default="{ row }">
                  <span v-if="row.voucherType === 'PAYMENT'">{{ row.sourceAccountName }}</span>
                  <span v-else-if="row.voucherType === 'RECEIPT'">{{ row.targetAccountName }}</span>
                  <span v-else>{{ row.sourceAccountName }} ➔ {{ row.targetAccountName }}</span>
                </template>
              </el-table-column>

              <el-table-column label="Danh Mục Chi Phí" min-width="160">
                <template #default="{ row }">
                  <span>{{ row.costCenterTitle }}</span>
                </template>
              </el-table-column>

              <el-table-column label="Đối Tượng / Ghi Chú" min-width="200">
                <template #default="{ row }">
                  <div><strong>{{ row.payeePayorName || '--' }}</strong></div>
                  <div class="text-subtle text-xs">{{ row.note }}</div>
                </template>
              </el-table-column>

              <el-table-column label="Trạng Thái" width="120">
                <template #default="{ row }">
                  <el-tag :type="getStatusTagType(row.status)" size="small">
                    {{ row.statusTitle }}
                  </el-tag>
                </template>
              </el-table-column>

              <el-table-column label="Thời Gian" width="150">
                <template #default="{ row }">
                  <span class="text-subtle text-xs">{{ formatDateTime(row.createdAt) }}</span>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </el-tab-pane>

        <!-- TAB 2: QUẢN LÝ PHIẾU THU / CHI -->
        <el-tab-pane label="Phiếu Thu - Chi" name="vouchers">
          <div class="tab-pane-content">
            <!-- Filter Toolbar -->
            <div class="filters-toolbar-row">
              <div class="filter-item">
                <el-select v-model="voucherFilter.type" placeholder="Loại phiếu" clearable style="width: 140px" @change="fetchVouchers">
                  <el-option label="Tất cả loại" value="" />
                  <el-option label="Phiếu Thu" value="RECEIPT" />
                  <el-option label="Phiếu Chi" value="PAYMENT" />
                  <el-option label="Chuyển quỹ" value="INTERNAL_TRANSFER" />
                </el-select>
              </div>

              <div class="filter-item">
                <el-select v-model="voucherFilter.status" placeholder="Trạng thái" clearable style="width: 130px" @change="fetchVouchers">
                  <el-option label="Tất cả" value="" />
                  <el-option label="Đã duyệt" value="APPROVED" />
                  <el-option label="Dự thảo" value="DRAFT" />
                  <el-option label="Đã hủy" value="CANCELLED" />
                </el-select>
              </div>

              <div class="filter-item">
                <el-select v-model="voucherFilter.costCenter" placeholder="Trung tâm chi phí" clearable style="width: 180px" @change="fetchVouchers">
                  <el-option label="Tất cả chi phí" value="" />
                  <el-option label="Doanh thu bán hàng" value="SALES_REVENUE" />
                  <el-option label="Thu hồi nợ đại lý" value="DEBT_COLLECTION" />
                  <el-option label="Chi thuê mặt bằng" value="RENTAL" />
                  <el-option label="Chi lương nhân sự" value="SALARY" />
                  <el-option label="Chi marketing" value="MARKETING" />
                  <el-option label="Chi kho vận bưu cục" value="LOGISTICS" />
                  <el-option label="Chi điện nước tiện ích" value="UTILITIES" />
                  <el-option label="Chi tiếp khách" value="ENTERTAINMENT" />
                  <el-option label="Chi nhập hàng NCC" value="PURCHASE" />
                </el-select>
              </div>

              <div class="filter-item search-filter">
                <el-input 
                  v-model="voucherFilter.keyword" 
                  placeholder="Tìm theo mã phiếu, người nộp/nhận, đơn hàng..."
                  clearable
                  @keyup.enter="fetchVouchers"
                  style="width: 280px"
                >
                  <template #prefix><el-icon><Search /></el-icon></template>
                </el-input>
              </div>

              <el-button type="primary" plain @click="fetchVouchers">
                Lọc dữ liệu
              </el-button>
            </div>

            <!-- Bảng Phiếu Thu Chi chính thức -->
            <el-table 
              :data="vouchersList" 
              style="width: 100%" 
              class="scannable-table mt-4"
              v-loading="loadingVouchers"
            >
              <el-table-column label="Mã Chứng Từ" min-width="150">
                <template #default="{ row }">
                  <span class="font-mono font-bold text-primary">{{ row.voucherCode }}</span>
                  <div v-if="row.referenceOrderCode" class="text-xs text-subtle">
                    Đơn: <strong>{{ row.referenceOrderCode }}</strong>
                  </div>
                </template>
              </el-table-column>

              <el-table-column label="Loại Phiếu" width="130">
                <template #default="{ row }">
                  <el-tag :type="getVoucherTagType(row.voucherType)" size="small" effect="plain">
                    {{ row.voucherTypeTitle }}
                  </el-tag>
                </template>
              </el-table-column>

              <el-table-column label="Số Tiền" min-width="140" align="right">
                <template #default="{ row }">
                  <span :class="row.voucherType === 'RECEIPT' ? 'text-emerald font-bold' : (row.voucherType === 'PAYMENT' ? 'text-rose font-bold' : 'text-blue font-bold')">
                    {{ row.voucherType === 'RECEIPT' ? '+' : (row.voucherType === 'PAYMENT' ? '-' : '⇄') }}
                    {{ formatCurrency(row.amount) }}
                  </span>
                </template>
              </el-table-column>

              <el-table-column label="Tài Khoản Quỹ" min-width="160">
                <template #default="{ row }">
                  <span v-if="row.voucherType === 'PAYMENT'">{{ row.sourceAccountName }}</span>
                  <span v-else-if="row.voucherType === 'RECEIPT'">{{ row.targetAccountName }}</span>
                  <span v-else>{{ row.sourceAccountName }} ➔ {{ row.targetAccountName }}</span>
                </template>
              </el-table-column>

              <el-table-column label="Danh Mục Chi Phí" min-width="160">
                <template #default="{ row }">
                  <span>{{ row.costCenterTitle }}</span>
                </template>
              </el-table-column>

              <el-table-column label="Đối Tượng & Ghi Chú" min-width="220">
                <template #default="{ row }">
                  <div><strong>{{ row.payeePayorName || '--' }}</strong> <span v-if="row.payeePayorPhone" class="text-xs text-subtle">({{ row.payeePayorPhone }})</span></div>
                  <div class="text-subtle text-xs">{{ row.note }}</div>
                </template>
              </el-table-column>

              <el-table-column label="Trạng Thái" width="120">
                <template #default="{ row }">
                  <el-tag :type="getStatusTagType(row.status)" size="small">
                    {{ row.statusTitle }}
                  </el-tag>
                </template>
              </el-table-column>

              <el-table-column label="Thao Tác" width="160" fixed="right">
                <template #default="{ row }">
                  <div class="row-actions-wrap">
                    <el-button 
                      v-if="row.status === 'DRAFT'"
                      size="small" 
                      type="success" 
                      plain 
                      @click="handleApproveVoucher(row)"
                    >
                      Duyệt
                    </el-button>
                    <el-button 
                      v-if="row.status !== 'CANCELLED'"
                      size="small" 
                      type="danger" 
                      plain 
                      @click="handleCancelVoucher(row)"
                    >
                      Hủy
                    </el-button>
                  </div>
                </template>
              </el-table-column>
            </el-table>

            <!-- Pagination -->
            <div class="pagination-footer-row">
              <el-pagination
                v-model:current-page="voucherPage"
                v-model:page-size="voucherPageSize"
                :total="voucherTotal"
                :page-sizes="[10, 20, 50, 100]"
                layout="total, sizes, prev, pager, next"
                @current-change="fetchVouchers"
                @size-change="fetchVouchers"
              />
            </div>
          </div>
        </el-tab-pane>

        <!-- TAB 3: ĐỐI SOÁT SAO KÊ NGÂN HÀNG THÔNG MINH -->
        <el-tab-pane label="Đối Soát Sao Kê Ngân Hàng" name="reconciliation">
          <div class="tab-pane-content">
            <!-- Header đối soát -->
            <div class="reconcile-top-bar">
              <div class="rec-banner-info">
                <span class="rec-icon">⚡</span>
                <div class="rec-banner-text">
                  <strong>Smart Bank Reconciliation Engine (AI Fuzzy Matching):</strong>
                  Hệ thống tự động quét nội dung chuyển khoản, nhận diện mã đơn hàng (ORD-...), khớp số tiền và cho phép hạch toán thanh toán trong 1-click!
                </div>
              </div>

              <div class="rec-btns">
                <el-button type="primary" @click="openUploadStatementModal">
                  <el-icon><Upload /></el-icon>
                  Nạp Dữ Liệu Sao Kê
                </el-button>
                <el-button 
                  type="success" 
                  :disabled="matchedCount === 0" 
                  @click="handleReconcileAllMatched"
                >
                  <el-icon><Check /></el-icon>
                  Đối Soát Hàng Loạt ({{ matchedCount }} đơn khớp)
                </el-button>
              </div>
            </div>

            <!-- Bảng Dòng Sao Kê Ngân Hàng -->
            <el-table 
              :data="statementItems" 
              style="width: 100%" 
              class="scannable-table mt-4"
              v-loading="loadingStatements"
            >
              <el-table-column label="Mã Giao Dịch" min-width="150">
                <template #default="{ row }">
                  <span class="font-mono font-bold">{{ row.transactionCode }}</span>
                  <div class="text-xs text-subtle">{{ row.bankName }} • {{ row.accountNumber }}</div>
                </template>
              </el-table-column>

              <el-table-column label="Thời Gian GD" width="160">
                <template #default="{ row }">
                  <span>{{ formatDateTime(row.transactionDate) }}</span>
                </template>
              </el-table-column>

              <el-table-column label="Số Tiền" min-width="140" align="right">
                <template #default="{ row }">
                  <span class="text-emerald font-bold">
                    +{{ formatCurrency(row.amount) }}
                  </span>
                </template>
              </el-table-column>

              <el-table-column label="Nội Dung Chuyển Khoản" min-width="250">
                <template #default="{ row }">
                  <div class="statement-desc-box">{{ row.description }}</div>
                </template>
              </el-table-column>

              <el-table-column label="Kết Quả So Khớp Đơn" min-width="200">
                <template #default="{ row }">
                  <div v-if="row.matchedOrderCode" class="matched-order-badge">
                    <span class="matched-icon">🎯</span>
                    <div>
                      <strong>{{ row.matchedOrderCode }}</strong>
                      <div class="text-xs text-subtle">{{ row.matchedCustomerName }}</div>
                    </div>
                  </div>
                  <div v-else class="text-subtle text-xs">
                    ⚪ Chưa khớp mã đơn
                  </div>
                </template>
              </el-table-column>

              <el-table-column label="Trạng Thái" width="140">
                <template #default="{ row }">
                  <el-tag :type="getReconTagType(row.status)" size="small">
                    {{ row.statusTitle }}
                  </el-tag>
                </template>
              </el-table-column>

              <el-table-column label="Thao Tác" width="140" fixed="right">
                <template #default="{ row }">
                  <el-button 
                    v-if="row.status === 'MATCHED'"
                    size="small" 
                    type="success" 
                    plain 
                    @click="handleReconcileSingleItem(row)"
                  >
                    Khớp Đơn
                  </el-button>
                  <span v-else-if="row.status === 'RECONCILED'" class="text-emerald text-xs font-bold">
                    ✓ Đã hạch toán
                  </span>
                  <span v-else class="text-subtle text-xs">
                    Chờ xử lý
                  </span>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </el-tab-pane>

        <!-- TAB 4: BÁO CÁO LÃI LỖ P&L & DỰ BÁO DÒNG TIỀN -->
        <el-tab-pane label="Báo Cáo Lãi Lỗ P&L (BI)" name="pnl">
          <div class="tab-pane-content">
            <!-- Period Filter -->
            <div class="pnl-header-toolbar">
              <div class="pnl-title-block">
                <h3 class="section-title">Báo Cáo Hoạt Động Kinh Doanh P&L (Income Statement)</h3>
                <span class="text-subtle text-sm">Kỳ báo cáo: <strong>{{ pnlData?.periodTitle }}</strong></span>
              </div>

              <div class="pnl-period-pills">
                <button 
                  v-for="p in periodOptions" 
                  :key="p.value"
                  type="button" 
                  class="period-pill-btn"
                  :class="{ 'is-active': currentPeriod === p.value }"
                  @click="selectPeriod(p.value)"
                >
                  {{ p.label }}
                </button>
              </div>
            </div>

            <!-- Bảng Báo cáo P&L chuẩn mực -->
            <div class="pnl-statement-table">
              <!-- Mục 1: Doanh thu thuần -->
              <div class="pnl-row pnl-header-level-1">
                <span class="row-title">1. DOANH THU THUẦN (NET REVENUE)</span>
                <span class="row-amount text-primary font-bold">{{ formatCurrency(pnlData?.netRevenue) }}</span>
              </div>
              <div class="pnl-row pnl-sub-level">
                <span class="row-title">• Tổng doanh số bán hàng (Gross Revenue)</span>
                <span class="row-amount">{{ formatCurrency(pnlData?.grossRevenue) }}</span>
              </div>
              <div class="pnl-row pnl-sub-level">
                <span class="row-title">• Giảm trừ doanh thu & Chiết khấu (Discounts)</span>
                <span class="row-amount text-subtle">-{{ formatCurrency(pnlData?.discounts) }}</span>
              </div>

              <!-- Mục 2: Giá vốn hàng bán COGS -->
              <div class="pnl-row pnl-header-level-1 mt-2">
                <span class="row-title">2. GIÁ VỐN HÀNG BÁN (COGS - Cost of Goods Sold)</span>
                <span class="row-amount text-rose font-bold">-{{ formatCurrency(pnlData?.cogs) }}</span>
              </div>

              <!-- Mục 3: Lợi nhuận gộp -->
              <div class="pnl-row pnl-summary-level-1 mt-2">
                <span class="row-title">3. LỢI NHUẬN GỘP (GROSS PROFIT = Doanh thu thuần - Giá vốn)</span>
                <div class="row-right-group">
                  <span class="margin-pill">Biên lãi gộp: {{ pnlData?.grossMarginPercent }}%</span>
                  <span class="row-amount text-emerald font-bold">{{ formatCurrency(pnlData?.grossProfit) }}</span>
                </div>
              </div>

              <!-- Mục 4: Chi phí hoạt động OPEX -->
              <div class="pnl-row pnl-header-level-1 mt-4">
                <span class="row-title">4. CHI PHÍ HOẠT ĐỘNG & VẬN HÀNH (OPEX)</span>
                <span class="row-amount text-rose font-bold">-{{ formatCurrency(pnlData?.totalOpex) }}</span>
              </div>
              <!-- Danh sách chi phí phân bổ theo Cost Center -->
              <div 
                v-for="exp in (pnlData?.opexBreakdown || [])" 
                :key="exp.costCenterCode"
                class="pnl-row pnl-sub-level"
              >
                <span class="row-title">• {{ exp.costCenterTitle }} ({{ exp.voucherCount }} phiếu)</span>
                <div class="row-right-group">
                  <span class="text-xs text-subtle">{{ exp.percentageOfOpex }}% OPEX</span>
                  <span class="row-amount">-{{ formatCurrency(exp.amount) }}</span>
                </div>
              </div>

              <!-- Mục 5: LỢI NHUẬN RÒNG / THUẦN EBIT -->
              <div class="pnl-row pnl-final-ebit-row mt-4">
                <div class="ebit-title-col">
                  <span class="ebit-main-title">5. LỢI NHUẬN THUẦN DOANH NGHIỆP (EBIT / NET PROFIT)</span>
                  <span class="ebit-sub-formula">= Lợi nhuận gộp - Toàn bộ chi phí hoạt động OPEX</span>
                </div>
                <div class="ebit-val-col">
                  <span class="margin-pill ebit-margin-pill">Biên lãi ròng: {{ pnlData?.netMarginPercent }}%</span>
                  <span class="ebit-final-amount" :class="(pnlData?.netProfit || 0) >= 0 ? 'text-emerald' : 'text-rose'">
                    {{ formatCurrency(pnlData?.netProfit) }}
                  </span>
                </div>
              </div>
            </div>

            <!-- Khối Khuyến nghị CFO & Sức khỏe Dòng tiền -->
            <div class="cfo-advisory-card mt-6">
              <div class="cfo-card-header">
                <span class="cfo-icon">💡</span>
                <strong>Góc Nhìn Giám Đốc Tài Chính (CFO Strategic Copilot):</strong>
              </div>
              <p class="cfo-advice-text">
                {{ cashRunway?.recommendation || 'Dòng tiền và biên lợi nhuận của doanh nghiệp đang được kiểm soát ổn định.' }}
              </p>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>

    <!-- MODAL 1: LẬP PHIẾU THU / CHI -->
    <el-dialog 
      v-model="showVoucherModal" 
      :title="voucherForm.voucherType === 'RECEIPT' ? 'Lập Phiếu Thu Quỹ' : 'Lập Phiếu Chi Xuất Quỹ'"
      width="540px"
      destroy-on-close
    >
      <el-form label-position="top" class="custom-modal-form">
        <el-form-item label="Trung tâm chi phí / Nguồn thu *" required>
          <el-select v-model="voucherForm.costCenter" style="width: 100%">
            <template v-if="voucherForm.voucherType === 'RECEIPT'">
              <el-option label="Doanh thu bán hàng" value="SALES_REVENUE" />
              <el-option label="Thu hồi nợ đại lý" value="DEBT_COLLECTION" />
              <el-option label="Thu nhập khác" value="OTHER_INCOME" />
            </template>
            <template v-else>
              <el-option label="Chi thuê mặt bằng & kho" value="RENTAL" />
              <el-option label="Chi lương nhân sự" value="SALARY" />
              <el-option label="Chi phí Marketing" value="MARKETING" />
              <el-option label="Chi phí vận chuyển bưu cục" value="LOGISTICS" />
              <el-option label="Chi thanh toán nhập hàng NCC" value="PURCHASE" />
              <el-option label="Chi điện, nước, internet" value="UTILITIES" />
              <el-option label="Chi tiếp khách, đối ngoại" value="ENTERTAINMENT" />
              <el-option label="Chi phí khác" value="OTHER_EXPENSE" />
            </template>
          </el-select>
        </el-form-item>

        <el-form-item label="Số tiền (VNĐ) *" required>
          <el-input-number 
            v-model="voucherForm.amount" 
            :min="1000" 
            :step="500000"
            style="width: 100%" 
            :controls="false"
          />
          <div class="text-xs text-subtle mt-1">{{ formatCurrency(voucherForm.amount) }}</div>
        </el-form-item>

        <el-form-item 
          v-if="voucherForm.voucherType === 'PAYMENT'" 
          label="Tài khoản trích tiền (Nguồn) *" 
          required
        >
          <el-select v-model="voucherForm.sourceAccountId" style="width: 100%">
            <el-option 
              v-for="acc in accounts" 
              :key="acc.id" 
              :label="acc.accountName + ' (Số dư: ' + formatCurrency(acc.currentBalance) + ')'" 
              :value="acc.id" 
            />
          </el-select>
        </el-form-item>

        <el-form-item 
          v-else 
          label="Tài khoản nhận tiền (Đích) *" 
          required
        >
          <el-select v-model="voucherForm.targetAccountId" style="width: 100%">
            <el-option 
              v-for="acc in accounts" 
              :key="acc.id" 
              :label="acc.accountName + ' (Số dư: ' + formatCurrency(acc.currentBalance) + ')'" 
              :value="acc.id" 
            />
          </el-select>
        </el-form-item>

        <el-form-item :label="voucherForm.voucherType === 'RECEIPT' ? 'Người nộp tiền' : 'Người nhận tiền'">
          <el-input v-model="voucherForm.payeePayorName" placeholder="Tên đối tác / nhân viên..." />
        </el-form-item>

        <el-form-item label="Mã đơn hàng liên kết (nếu có)">
          <el-input v-model="voucherForm.referenceOrderCode" placeholder="ORD-20261114-1001" />
        </el-form-item>

        <el-form-item label="Lý do thu / chi chi tiết">
          <el-input v-model="voucherForm.note" type="textarea" :rows="2" placeholder="Ghi chú nội dung thu chi..." />
        </el-form-item>

        <el-form-item>
          <el-checkbox v-model="voucherForm.autoApprove">
            Phê duyệt & Hạch toán tăng/giảm số dư quỹ ngay lập tức
          </el-checkbox>
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="modal-footer-btns">
          <el-button @click="showVoucherModal = false">Đóng</el-button>
          <el-button type="primary" :loading="submittingVoucher" @click="submitVoucher">
            Lưu Chứng Từ
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- MODAL 2: CHUYỂN QUỸ NỘI BỘ -->
    <el-dialog 
      v-model="showTransferModal" 
      title="Chuyển Quỹ Nội Bộ Giữa Các Tài Khoản" 
      width="500px"
      destroy-on-close
    >
      <el-form label-position="top" class="custom-modal-form">
        <el-form-item label="Tài khoản nguồn (Trích tiền) *" required>
          <el-select v-model="transferForm.sourceAccountId" style="width: 100%">
            <el-option 
              v-for="acc in accounts" 
              :key="acc.id" 
              :label="acc.accountName + ' (Khả dụng: ' + formatCurrency(acc.currentBalance) + ')'" 
              :value="acc.id" 
            />
          </el-select>
        </el-form-item>

        <el-form-item label="Tài khoản đích (Nhận tiền) *" required>
          <el-select v-model="transferForm.targetAccountId" style="width: 100%">
            <el-option 
              v-for="acc in accounts" 
              :key="acc.id" 
              :label="acc.accountName" 
              :value="acc.id" 
            />
          </el-select>
        </el-form-item>

        <el-form-item label="Số tiền chuyển (VNĐ) *" required>
          <el-input-number 
            v-model="transferForm.amount" 
            :min="1000" 
            :step="1000000"
            style="width: 100%" 
            :controls="false"
          />
          <div class="text-xs text-subtle mt-1">{{ formatCurrency(transferForm.amount) }}</div>
        </el-form-item>

        <el-form-item label="Ghi chú chuyển quỹ">
          <el-input v-model="transferForm.note" placeholder="Ví dụ: Rút 20 triệu từ VCB về nhập quỹ tiền mặt văn phòng..." />
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="modal-footer-btns">
          <el-button @click="showTransferModal = false">Hủy</el-button>
          <el-button type="primary" :loading="submittingTransfer" @click="submitTransfer">
            Xác Nhận Chuyển Tiền
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- MODAL 3: NẠP DỮ LIỆU SAO KÊ NGÂN HÀNG -->
    <el-dialog 
      v-model="showUploadModal" 
      title="Nạp Dữ Liệu Sao Kê Ngân Hàng Đối Soát" 
      width="580px"
      destroy-on-close
    >
      <el-form label-position="top" class="custom-modal-form">
        <el-form-item label="Ngân hàng sao kê">
          <el-select v-model="uploadForm.bankName" style="width: 100%">
            <el-option label="Vietcombank" value="Vietcombank" />
            <el-option label="MBBank" value="MBBank" />
            <el-option label="Techcombank" value="Techcombank" />
          </el-select>
        </el-form-item>

        <el-form-item label="Tài khoản ngân hàng đích hạch toán">
          <el-select v-model="uploadForm.targetBankAccountId" style="width: 100%">
            <el-option 
              v-for="acc in bankAccounts" 
              :key="acc.id" 
              :label="acc.accountName" 
              :value="acc.id" 
            />
          </el-select>
        </el-form-item>

        <el-form-item label="Dữ liệu sao kê (CSV hoặc Dòng văn bản) *" required>
          <el-input 
            v-model="uploadForm.statementRawContent" 
            type="textarea" 
            :rows="6"
            placeholder="Dán các dòng sao kê hoặc CSV vào đây. Ví dụ:&#10;FT26324891004 | 2026-11-20 15:30:00 | +598000 | NGUYEN VAN A CK DON HANG ORD-20261114-1001"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="modal-footer-btns">
          <el-button @click="showUploadModal = false">Đóng</el-button>
          <el-button type="primary" :loading="submittingUpload" @click="submitUploadStatements">
            Bóc Tách & Đối Soát
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- MODAL 4: TẠO TÀI KHOẢN QUỸ MỚI -->
    <el-dialog 
      v-model="showCreateAccountModal" 
      title="Tạo Mới Tài Khoản Quỹ" 
      width="500px"
      destroy-on-close
    >
      <el-form label-position="top" class="custom-modal-form">
        <el-form-item label="Mã tài khoản (Viết hoa, duy nhất) *" required>
          <el-input v-model="newAccForm.accountCode" placeholder="VD: CASH_HA_NOI, BANK_ACB" />
        </el-form-item>

        <el-form-item label="Tên tài khoản quỹ *" required>
          <el-input v-model="newAccForm.accountName" placeholder="VD: Quỹ tiền mặt Chi nhánh Hà Nội" />
        </el-form-item>

        <el-form-item label="Loại tài khoản *" required>
          <el-select v-model="newAccForm.accountType" style="width: 100%">
            <el-option label="Tiền mặt tại quỹ (CASH)" value="CASH" />
            <el-option label="Tài khoản ngân hàng (BANK)" value="BANK" />
          </el-select>
        </el-form-item>

        <template v-if="newAccForm.accountType === 'BANK'">
          <el-form-item label="Tên ngân hàng">
            <el-input v-model="newAccForm.bankName" placeholder="VD: ACB, Vietcombank, VPBank..." />
          </el-form-item>
          <el-form-item label="Số tài khoản">
            <el-input v-model="newAccForm.accountNumber" placeholder="Số tài khoản ngân hàng..." />
          </el-form-item>
          <el-form-item label="Chủ tài khoản">
            <el-input v-model="newAccForm.accountHolder" placeholder="Tên chủ tài khoản in hoa..." />
          </el-form-item>
        </template>

        <el-form-item label="Số dư ban đầu (VNĐ)">
          <el-input-number v-model="newAccForm.initialBalance" :min="0" :step="1000000" style="width: 100%" :controls="false" />
        </el-form-item>

        <el-form-item label="Ghi chú">
          <el-input v-model="newAccForm.note" placeholder="Ghi chú mục đích sử dụng quỹ..." />
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="modal-footer-btns">
          <el-button @click="showCreateAccountModal = false">Hủy</el-button>
          <el-button type="primary" :loading="submittingAcc" @click="submitCreateAccount">
            Lưu Tài Khoản
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  Plus, Minus, Switch, Refresh, Search, Upload, Check, 
  Money, Wallet, Document, TrendCharts 
} from '@element-plus/icons-vue'
import api from '../api'

// Tab điều hướng
const activeTab = ref('accounts')
const loading = ref(false)

// State Tài khoản Quỹ
const accounts = ref([])
const totalBalance = computed(() => {
  return accounts.value.reduce((sum, a) => sum + (Number(a.currentBalance) || 0), 0)
})
const totalCashBalance = computed(() => {
  return accounts.value
    .filter(a => a.accountType === 'CASH')
    .reduce((sum, a) => sum + (Number(a.currentBalance) || 0), 0)
})
const totalBankBalance = computed(() => {
  return accounts.value
    .filter(a => a.accountType === 'BANK')
    .reduce((sum, a) => sum + (Number(a.currentBalance) || 0), 0)
})
const bankAccounts = computed(() => accounts.value.filter(a => a.accountType === 'BANK'))

// State Cash Runway
const cashRunway = ref(null)

// State Phiếu Thu Chi
const loadingVouchers = ref(false)
const vouchersList = ref([])
const recentVouchers = ref([])
const voucherTotal = ref(0)
const voucherPage = ref(1)
const voucherPageSize = ref(20)
const voucherFilter = reactive({
  type: '',
  status: '',
  costCenter: '',
  keyword: ''
})

// State Đối soát Sao kê
const loadingStatements = ref(false)
const statementItems = ref([])
const matchedCount = computed(() => {
  return statementItems.value.filter(s => s.status === 'MATCHED').length
})

// State Báo cáo P&L
const currentPeriod = ref('THIS_MONTH')
const periodOptions = [
  { label: 'Hôm nay', value: 'TODAY' },
  { label: 'Tuần này', value: 'THIS_WEEK' },
  { label: 'Tháng này', value: 'THIS_MONTH' },
  { label: 'Quý này', value: 'THIS_QUARTER' },
  { label: 'Năm này', value: 'THIS_YEAR' }
]
const pnlData = ref(null)

// Modal Forms
const showVoucherModal = ref(false)
const submittingVoucher = ref(false)
const voucherForm = reactive({
  voucherType: 'PAYMENT',
  costCenter: 'RENTAL',
  amount: 15000000,
  sourceAccountId: null,
  targetAccountId: null,
  payeePayorName: '',
  payeePayorPhone: '',
  referenceOrderCode: '',
  note: '',
  autoApprove: true
})

const showTransferModal = ref(false)
const submittingTransfer = ref(false)
const transferForm = reactive({
  sourceAccountId: null,
  targetAccountId: null,
  amount: 10000000,
  note: ''
})

const showUploadModal = ref(false)
const submittingUpload = ref(false)
const uploadForm = reactive({
  bankName: 'Vietcombank',
  accountNumber: '0071001234567',
  targetBankAccountId: null,
  statementRawContent: ''
})

const showCreateAccountModal = ref(false)
const submittingAcc = ref(false)
const newAccForm = reactive({
  accountCode: '',
  accountName: '',
  accountType: 'BANK',
  bankName: '',
  accountNumber: '',
  accountHolder: '',
  initialBalance: 0,
  note: ''
})

// Format Helpers
const formatCurrency = (val) => {
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(val ?? 0)
}

const formatDateTime = (val) => {
  if (!val) return '--'
  try {
    const d = new Date(val)
    return d.toLocaleString('vi-VN', { hour: '2-digit', minute: '2-digit', day: '2-digit', month: '2-digit', year: 'numeric' })
  } catch (e) {
    return val
  }
}

const getVoucherTagType = (type) => {
  if (type === 'RECEIPT') return 'success'
  if (type === 'PAYMENT') return 'danger'
  return 'primary'
}

const getStatusTagType = (status) => {
  if (status === 'APPROVED') return 'success'
  if (status === 'CANCELLED') return 'info'
  return 'warning'
}

const getReconTagType = (status) => {
  if (status === 'RECONCILED') return 'success'
  if (status === 'MATCHED') return 'warning'
  if (status === 'UNMATCHED') return 'danger'
  return 'info'
}

// Fetch APIs
const fetchAccounts = async () => {
  try {
    const res = await api.get('/finance/accounts')
    accounts.value = res.data || []
    if (accounts.value.length && !voucherForm.sourceAccountId) {
      voucherForm.sourceAccountId = accounts.value[0].id
      voucherForm.targetAccountId = accounts.value[0].id
      transferForm.sourceAccountId = accounts.value[0].id
      if (accounts.value.length > 1) {
        transferForm.targetAccountId = accounts.value[1].id
      }
    }
  } catch (err) {
    console.error('Lỗi tải danh sách quỹ:', err)
  }
}

const fetchCashRunway = async () => {
  try {
    const res = await api.get('/finance/cash-runway')
    cashRunway.value = res.data || null
  } catch (err) {
    console.error('Lỗi tải dự báo dòng tiền:', err)
  }
}

const fetchVouchers = async () => {
  loadingVouchers.value = true
  try {
    const params = {
      page: voucherPage.value - 1,
      size: voucherPageSize.value,
      voucherType: voucherFilter.type || undefined,
      status: voucherFilter.status || undefined,
      costCenter: voucherFilter.costCenter || undefined,
      keyword: voucherFilter.keyword || undefined
    }
    const res = await api.get('/finance/vouchers', { params })
    vouchersList.value = res.data?.content || []
    voucherTotal.value = res.data?.totalElements || 0
    if (voucherPage.value === 1) {
      recentVouchers.value = vouchersList.value.slice(0, 8)
    }
  } catch (err) {
    console.error('Lỗi tải danh sách phiếu:', err)
  } finally {
    loadingVouchers.value = false
  }
}

const fetchStatements = async () => {
  loadingStatements.value = true
  try {
    const res = await api.get('/finance/reconciliation/items', { params: { size: 50 } })
    statementItems.value = res.data?.content || []
  } catch (err) {
    console.error('Lỗi tải sao kê:', err)
  } finally {
    loadingStatements.value = false
  }
}

const fetchPnL = async () => {
  try {
    const res = await api.get('/finance/pnl', { params: { period: currentPeriod.value } })
    pnlData.value = res.data || null
  } catch (err) {
    console.error('Lỗi tải P&L:', err)
  }
}

const selectPeriod = (pVal) => {
  currentPeriod.value = pVal
  fetchPnL()
}

const handleTabChange = (tab) => {
  if (tab.paneName === 'vouchers') {
    fetchVouchers()
  } else if (tab.paneName === 'reconciliation') {
    fetchStatements()
  } else if (tab.paneName === 'pnl') {
    fetchPnL()
  }
}

const refreshAllData = async () => {
  loading.value = true
  await Promise.allSettled([
    fetchAccounts(),
    fetchCashRunway(),
    fetchVouchers(),
    fetchStatements(),
    fetchPnL()
  ])
  loading.value = false
  ElMessage.success('Đã làm mới dữ liệu tài chính!')
}

// Handlers Modal
const openCreateVoucherModal = (type) => {
  voucherForm.voucherType = type
  voucherForm.costCenter = type === 'RECEIPT' ? 'SALES_REVENUE' : 'RENTAL'
  voucherForm.amount = type === 'RECEIPT' ? 5000000 : 15000000
  showVoucherModal.value = true
}

const submitVoucher = async () => {
  submittingVoucher.value = true
  try {
    const payload = { ...voucherForm }
    const res = await api.post('/finance/vouchers', payload)
    ElMessage.success(`Đã tạo thành công chứng từ ${res.data?.voucherCode}!`)
    showVoucherModal.value = false
    await Promise.all([fetchAccounts(), fetchVouchers(), fetchPnL()])
  } catch (err) {
    ElMessage.error(err.response?.data?.message || 'Lỗi khi tạo chứng từ thu chi!')
  } finally {
    submittingVoucher.value = false
  }
}

const handleApproveVoucher = async (row) => {
  try {
    await ElMessageBox.confirm(`Phê duyệt chứng từ [${row.voucherCode}] và hạch toán số dư quỹ?`, 'Xác nhận duyệt', {
      type: 'warning'
    })
    const res = await api.post(`/finance/vouchers/${row.id}/approve`)
    ElMessage.success(res.data?.message || 'Đã phê duyệt thành công!')
    await Promise.all([fetchAccounts(), fetchVouchers(), fetchPnL()])
  } catch (err) {
    if (err !== 'cancel') {
      ElMessage.error(err.response?.data?.message || 'Lỗi phê duyệt!')
    }
  }
}

const handleCancelVoucher = async (row) => {
  try {
    await ElMessageBox.confirm(`Bạn có chắc muốn hủy chứng từ [${row.voucherCode}]? (Nếu đã duyệt sẽ được hoàn tiền lại quỹ)`, 'Xác nhận hủy', {
      type: 'warning'
    })
    const res = await api.post(`/finance/vouchers/${row.id}/cancel`)
    ElMessage.success('Đã hủy chứng từ thành công!')
    await Promise.all([fetchAccounts(), fetchVouchers(), fetchPnL()])
  } catch (err) {
    if (err !== 'cancel') {
      ElMessage.error(err.response?.data?.message || 'Lỗi hủy chứng từ!')
    }
  }
}

const openTransferModal = () => {
  showTransferModal.value = true
}

const submitTransfer = async () => {
  if (transferForm.sourceAccountId === transferForm.targetAccountId) {
    ElMessage.warning('Tài khoản nguồn và tài khoản đích không được trùng nhau!')
    return
  }
  submittingTransfer.value = true
  try {
    const res = await api.post('/finance/transfer', transferForm)
    ElMessage.success(`Chuyển tiền nội bộ thành công (Mã: ${res.data?.voucherCode})!`)
    showTransferModal.value = false
    await Promise.all([fetchAccounts(), fetchVouchers()])
  } catch (err) {
    ElMessage.error(err.response?.data?.message || 'Lỗi khi chuyển quỹ!')
  } finally {
    submittingTransfer.value = false
  }
}

const openUploadStatementModal = () => {
  const bankAcc = bankAccounts.value[0]
  if (bankAcc) {
    uploadForm.targetBankAccountId = bankAcc.id
    uploadForm.bankName = bankAcc.bankName || 'Vietcombank'
    uploadForm.accountNumber = bankAcc.accountNumber || '0071001234567'
  }
  showUploadModal.value = true
}

const submitUploadStatements = async () => {
  submittingUpload.value = true
  try {
    const res = await api.post('/finance/reconciliation/upload', uploadForm)
    ElMessage.success(`Đã nạp và đối soát thành công ${res.data?.length || 0} dòng sao kê!`)
    showUploadModal.value = false
    uploadForm.statementRawContent = ''
    fetchStatements()
  } catch (err) {
    ElMessage.error(err.response?.data?.message || 'Lỗi nạp sao kê!')
  } finally {
    submittingUpload.value = false
  }
}

const handleReconcileSingleItem = async (item) => {
  const targetAcc = bankAccounts.value[0]
  if (!targetAcc) {
    ElMessage.warning('Chưa có tài khoản ngân hàng nào để hạch toán!')
    return
  }
  try {
    const res = await api.post(`/finance/reconciliation/${item.id}/reconcile?targetBankAccountId=${targetAcc.id}`)
    ElMessage.success('Đã đối soát và hạch toán thành công!')
    await Promise.all([fetchStatements(), fetchAccounts(), fetchVouchers()])
  } catch (err) {
    ElMessage.error(err.response?.data?.message || 'Lỗi đối soát!')
  }
}

const handleReconcileAllMatched = async () => {
  const targetAcc = bankAccounts.value[0]
  if (!targetAcc) {
    ElMessage.warning('Chưa có tài khoản ngân hàng nào để hạch toán!')
    return
  }
  try {
    await ElMessageBox.confirm(`Tiến hành hạch toán đối soát tự động cho ${matchedCount.value} đơn hàng đã khớp?`, 'Xác nhận đối soát', {
      type: 'success'
    })
    const res = await api.post(`/finance/reconciliation/reconcile-all?targetBankAccountId=${targetAcc.id}`)
    ElMessage.success(res.data?.message || 'Đối soát hàng loạt thành công!')
    await Promise.all([fetchStatements(), fetchAccounts(), fetchVouchers()])
  } catch (err) {
    if (err !== 'cancel') {
      ElMessage.error(err.response?.data?.message || 'Lỗi đối soát!')
    }
  }
}

const openCreateAccountModal = () => {
  newAccForm.accountCode = ''
  newAccForm.accountName = ''
  newAccForm.accountType = 'BANK'
  newAccForm.bankName = ''
  newAccForm.accountNumber = ''
  newAccForm.accountHolder = ''
  newAccForm.initialBalance = 0
  newAccForm.note = ''
  showCreateAccountModal.value = true
}

const submitCreateAccount = async () => {
  submittingAcc.value = true
  try {
    await api.post('/finance/accounts', newAccForm)
    ElMessage.success('Đã tạo mới tài khoản quỹ thành công!')
    showCreateAccountModal.value = false
    fetchAccounts()
  } catch (err) {
    ElMessage.error(err.response?.data?.message || 'Lỗi tạo tài khoản quỹ!')
  } finally {
    submittingAcc.value = false
  }
}

onMounted(() => {
  refreshAllData()
})
</script>

<style scoped>
.finance-page-container {
  padding: 20px;
  padding-bottom: 72px; /* Chống che bởi nút AI Copilot theo AGENTS.md */
}

/* Header */
.finance-header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 20px;
}
.page-title {
  font-size: 20px;
  font-weight: 700;
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0 0 4px 0;
}
.page-subtitle {
  font-size: 13px;
  color: var(--el-text-color-secondary);
  margin: 0;
}
.header-right-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

/* KPI Cards Grid */
.kpi-metrics-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 16px;
  margin-bottom: 20px;
}
.kpi-card {
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 10px;
  padding: 16px;
  display: flex;
  flex-direction: column;
  transition: all 0.2s;
}
.kpi-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
  transform: translateY(-2px);
}
.kpi-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}
.kpi-label {
  font-size: 12.5px;
  color: var(--el-text-color-secondary);
  font-weight: 500;
}
.kpi-icon-wrap {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
}
.bg-blue-subtle { background: rgba(59, 130, 246, 0.1); }
.bg-emerald-subtle { background: rgba(16, 185, 129, 0.1); }
.bg-indigo-subtle { background: rgba(99, 102, 241, 0.1); }

.kpi-value-row {
  margin-bottom: 6px;
}
.kpi-value {
  font-size: 22px;
  font-weight: 700;
  white-space: nowrap;
}
.kpi-footer-row {
  font-size: 11.5px;
  color: var(--el-text-color-secondary);
}
.kpi-risk-badge {
  font-size: 11px;
  font-weight: 700;
  padding: 2px 8px;
  border-radius: 4px;
}
.badge-safe { background: rgba(16, 185, 129, 0.15); color: #10b981; }
.badge-warning { background: rgba(245, 158, 11, 0.15); color: #f59e0b; }
.badge-critical { background: rgba(239, 68, 68, 0.15); color: #ef4444; }

/* Tabs Card */
.finance-tabs-card {
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 10px;
  padding: 16px 20px;
}
.tab-pane-content {
  padding-top: 12px;
}
.section-title-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}
.section-title {
  font-size: 15px;
  font-weight: 700;
  margin: 0;
}

/* Accounts Cards Grid */
.accounts-cards-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 14px;
}
.account-box-card {
  background: var(--el-fill-color-blank);
  border: 1px solid var(--el-border-color-light);
  border-radius: 8px;
  padding: 14px;
  display: flex;
  flex-direction: column;
}
.account-box-card.is-cash {
  border-left: 4px solid #10b981;
}
.account-box-card.is-bank {
  border-left: 4px solid #3b82f6;
}
.acc-card-top {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}
.acc-icon-badge {
  font-size: 22px;
}
.acc-info-block {
  flex: 1;
  min-width: 0;
}
.acc-name-text {
  font-weight: 700;
  font-size: 13.5px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.acc-code-tag {
  font-size: 10.5px;
  color: var(--el-text-color-secondary);
  font-family: monospace;
}
.acc-status-pill {
  font-size: 10px;
  padding: 2px 6px;
  border-radius: 4px;
  background: var(--el-fill-color);
  color: var(--el-text-color-secondary);
}
.acc-status-pill.is-active {
  background: rgba(16, 185, 129, 0.12);
  color: #10b981;
}
.acc-card-body {
  font-size: 12px;
  color: var(--el-text-color-regular);
  margin-bottom: 12px;
  display: flex;
  flex-direction: column;
  gap: 3px;
}
.acc-meta-line {
  display: flex;
  align-items: center;
  gap: 6px;
}
.meta-label {
  color: var(--el-text-color-secondary);
}
.meta-val {
  font-weight: 600;
}
.meta-sub {
  color: var(--el-text-color-secondary);
  font-size: 11px;
}
.acc-card-footer {
  margin-top: auto;
  border-top: 1px dashed var(--el-border-color-lighter);
  padding-top: 8px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.balance-label {
  font-size: 11.5px;
  color: var(--el-text-color-secondary);
}
.balance-amount {
  font-size: 15px;
  font-weight: 700;
  color: #10b981;
}

/* Filters Toolbar */
.filters-toolbar-row {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  align-items: center;
  margin-bottom: 12px;
}

/* Scannable Table */
.scannable-table :deep(th.el-table__cell) {
  background-color: var(--el-fill-color-light);
  font-size: 12.5px;
  font-weight: 600;
}
.scannable-table :deep(td.el-table__cell) {
  font-size: 12.5px;
}
.text-primary { color: #3b82f6; }
.text-emerald { color: #10b981; }
.text-rose { color: #ef4444; }
.text-blue { color: #2563eb; }
.text-subtle { color: var(--el-text-color-secondary); }
.font-mono { font-family: monospace; }
.font-bold { font-weight: 700; }
.text-xs { font-size: 11px; }
.text-sm { font-size: 12.5px; }
.mt-1 { margin-top: 4px; }
.mt-2 { margin-top: 8px; }
.mt-4 { margin-top: 16px; }
.mt-6 { margin-top: 24px; }

.pagination-footer-row {
  display: flex;
  justify-content: flex-end;
  margin-top: 14px;
}

/* Reconciliation Top Bar */
.reconcile-top-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  background: var(--el-fill-color-light);
  border-radius: 8px;
  padding: 12px 16px;
}
.rec-banner-info {
  display: flex;
  align-items: center;
  gap: 10px;
  flex: 1;
}
.rec-icon {
  font-size: 24px;
}
.rec-banner-text {
  font-size: 12.5px;
  line-height: 1.4;
}
.rec-btns {
  display: flex;
  gap: 8px;
}
.statement-desc-box {
  font-family: monospace;
  font-size: 11.5px;
  background: var(--el-fill-color-blank);
  padding: 4px 8px;
  border-radius: 4px;
  border: 1px solid var(--el-border-color-lighter);
}
.matched-order-badge {
  display: flex;
  align-items: center;
  gap: 6px;
}

/* P&L BI Statement */
.pnl-header-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 16px;
}
.pnl-period-pills {
  display: flex;
  gap: 6px;
}
.period-pill-btn {
  padding: 5px 12px;
  border-radius: 20px;
  border: 1px solid var(--el-border-color);
  background: var(--el-bg-color);
  color: var(--el-text-color-regular);
  font-size: 12px;
  cursor: pointer;
  transition: all 0.15s;
}
.period-pill-btn:hover {
  border-color: #3b82f6;
  color: #3b82f6;
}
.period-pill-btn.is-active {
  background: #3b82f6;
  color: #ffffff;
  border-color: #3b82f6;
  font-weight: 600;
}

.pnl-statement-table {
  border: 1px solid var(--el-border-color-light);
  border-radius: 8px;
  overflow: hidden;
}
.pnl-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 16px;
  border-bottom: 1px solid var(--el-border-color-lighter);
  font-size: 13px;
}
.pnl-row:last-child {
  border-bottom: none;
}
.pnl-header-level-1 {
  background: var(--el-fill-color-light);
  font-weight: 700;
}
.pnl-sub-level {
  padding-left: 32px;
  font-size: 12.5px;
}
.pnl-summary-level-1 {
  background: rgba(16, 185, 129, 0.08);
  font-weight: 700;
  border-top: 1px solid var(--el-border-color);
}
.pnl-final-ebit-row {
  background: rgba(59, 130, 246, 0.08);
  padding: 14px 16px;
  border-top: 2px solid #3b82f6;
}
.ebit-main-title {
  font-weight: 800;
  font-size: 14px;
}
.ebit-sub-formula {
  font-size: 11px;
  color: var(--el-text-color-secondary);
}
.ebit-title-col {
  display: flex;
  flex-direction: column;
}
.ebit-val-col {
  display: flex;
  align-items: center;
  gap: 12px;
}
.ebit-final-amount {
  font-size: 20px;
  font-weight: 800;
}
.row-right-group {
  display: flex;
  align-items: center;
  gap: 10px;
}
.margin-pill {
  font-size: 11px;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 4px;
  background: rgba(0, 0, 0, 0.06);
}

.cfo-advisory-card {
  background: rgba(59, 130, 246, 0.05);
  border: 1px solid rgba(59, 130, 246, 0.2);
  border-radius: 8px;
  padding: 14px 18px;
}
.cfo-card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13.5px;
  color: #2563eb;
  margin-bottom: 6px;
}
.cfo-advice-text {
  font-size: 12.5px;
  line-height: 1.5;
  margin: 0;
}

.modal-footer-btns {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>
