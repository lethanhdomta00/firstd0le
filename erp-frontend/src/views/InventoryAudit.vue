<template>
  <div class="inventory-audit-container">
    <!-- PAGE HEADER -->
    <div class="page-header">
      <div class="header-left">
        <div class="title-row">
          <h1 class="page-title">Quản Lý Lô Sản Xuất, Hạn Dùng & Kiểm Kê Kho</h1>
          <span class="badge-enterprise">ENTERPRISE V3.0</span>
        </div>
        <p class="page-subtitle">
          Kiểm soát Lô hàng (Batch/Lot), Radar Cảnh Báo Cận Date 3 Cấp Độ, Serial/IMEI & Kiểm Kê Cân Đối Kho Tự Động
        </p>
      </div>
      <div class="header-actions">
        <el-button 
          v-if="canManage"
          type="primary" 
          class="btn-action-primary" 
          @click="openCreateAuditDialog"
        >
          <el-icon><List /></el-icon>
          <span>Tạo Phiếu Kiểm Kê Kho</span>
        </el-button>
        <el-dropdown v-if="canManage" trigger="click" @command="handleHeaderCommand">
          <el-button class="btn-action-secondary">
            <span>Thêm Mới</span>
            <el-icon class="el-icon--right"><ArrowDown /></el-icon>
          </el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="create_batch">
                <el-icon><Box /></el-icon>
                <span>Nhập Lô Hàng Mới (Batch)</span>
              </el-dropdown-item>
              <el-dropdown-item command="create_serial">
                <el-icon><Tickets /></el-icon>
                <span>Đăng Ký Mã Serial / IMEI</span>
              </el-dropdown-item>
              <el-dropdown-item command="test_fefo">
                <el-icon><Operation /></el-icon>
                <span>Mô Phỏng Xuất Kho FEFO</span>
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </div>

    <!-- STATS OVERVIEW CARDS (RADAR OVERVIEW) -->
    <div class="stats-grid">
      <div class="stat-card" @click="activeTab = 'batches'">
        <div class="stat-icon-wrapper stat-emerald">
          <el-icon><CircleCheckFilled /></el-icon>
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ radarSummary.greenCount }}</div>
          <div class="stat-label">Lô An Toàn (> 90 ngày)</div>
        </div>
      </div>

      <div class="stat-card" @click="activeTab = 'batches'">
        <div class="stat-icon-wrapper stat-amber">
          <el-icon><WarningFilled /></el-icon>
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ radarSummary.yellowCount }}</div>
          <div class="stat-label">Lô Cần Lưu Ý (30 - 90 ngày)</div>
        </div>
      </div>

      <div class="stat-card" @click="activeTab = 'batches'">
        <div class="stat-icon-wrapper stat-rose">
          <el-icon><CircleCloseFilled /></el-icon>
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ radarSummary.redCount }}</div>
          <div class="stat-label">Lô Nguy Cấp & Quá Hạn (≤ 30 ngày)</div>
        </div>
      </div>

      <div class="stat-card" @click="activeTab = 'stocktakes'">
        <div class="stat-icon-wrapper stat-blue">
          <el-icon><DocumentChecked /></el-icon>
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ activeAuditsCount }}</div>
          <div class="stat-label">Phiếu Kiểm Kê Đang Xử Lý</div>
        </div>
      </div>
    </div>

    <!-- MAIN TABS CONTAINER -->
    <el-tabs v-model="activeTab" class="custom-enterprise-tabs">
      <!-- TAB 1: LÔ SẢN XUẤT & RADAR CẬN DATE -->
      <el-tab-pane label="Lô Hàng & Radar Cận Date" name="batches">
        <div class="card-box">
          <!-- RADAR METER BANNER -->
          <div class="radar-meter-banner">
            <div class="radar-info">
              <span class="radar-title">HỆ THỐNG RADAR CẢNH BÁO RỦI RO HẠN SỬ DỤNG (EXPIRY RADAR)</span>
              <p class="radar-sub">
                Tự động quét toàn bộ kho, phân loại 3 cấp độ rủi ro và gợi ý thuật toán FEFO (First Expired, First Out)
              </p>
            </div>
            <div class="radar-actions">
              <el-button size="small" type="primary" plain @click="openFefoDialog">
                <el-icon><Operation /></el-icon>
                <span>Gợi Ý Xuất FEFO</span>
              </el-button>
              <el-button size="small" type="default" plain @click="fetchBatchesAndRadar">
                <el-icon><Refresh /></el-icon>
                <span>Quét Radar Lại</span>
              </el-button>
            </div>
          </div>

          <!-- FILTER TOOLBAR -->
          <div class="filter-toolbar mt-3">
            <el-select 
              v-model="batchFilterWarehouse" 
              placeholder="Lọc theo Kho hàng" 
              clearable 
              style="width: 220px;"
              @change="fetchBatchesAndRadar"
            >
              <el-option 
                v-for="w in warehouses" 
                :key="w.id" 
                :label="w.name" 
                :value="w.id" 
              />
            </el-select>

            <el-radio-group v-model="batchRiskFilter" size="default">
              <el-radio-button label="ALL">Tất Cả ({{ batches.length }})</el-radio-button>
              <el-radio-button label="RED">🔴 Nguy Cấp ({{ radarSummary.redCount }})</el-radio-button>
              <el-radio-button label="YELLOW">🟡 Cần Lưu Ý ({{ radarSummary.yellowCount }})</el-radio-button>
              <el-radio-button label="GREEN">🟢 An Toàn ({{ radarSummary.greenCount }})</el-radio-button>
            </el-radio-group>

            <div class="flex-spacer"></div>

            <el-button 
              v-if="canManage"
              type="primary" 
              plain 
              @click="openCreateBatchDialog"
            >
              <el-icon><Plus /></el-icon>
              <span>Thêm Lô Hàng</span>
            </el-button>
          </div>

          <!-- BATCHES TABLE -->
          <el-table 
            v-loading="loadingBatches" 
            :data="filteredBatches" 
            stripe 
            class="enterprise-table"
            empty-text="Chưa có lô hàng nào phù hợp"
          >
            <el-table-column prop="batchCode" label="Mã Lô (Lot)" width="160">
              <template #default="{ row }">
                <span class="code-badge-blue">{{ row.batchCode }}</span>
              </template>
            </el-table-column>

            <el-table-column prop="productName" label="Sản Phẩm" min-width="190">
              <template #default="{ row }">
                <div class="name-cell">
                  <span class="font-bold text-dark">{{ row.productName }}</span>
                  <span class="sub-text">{{ row.productSku }}</span>
                </div>
              </template>
            </el-table-column>

            <el-table-column prop="warehouseName" label="Kho Chứa" width="160">
              <template #default="{ row }">
                <span class="text-secondary">{{ row.warehouseName }}</span>
              </template>
            </el-table-column>

            <el-table-column prop="expiryDate" label="Hạn Dùng & Cận Date" width="180">
              <template #default="{ row }">
                <div class="expiry-cell">
                  <span class="font-bold text-dark">{{ formatDate(row.expiryDate) }}</span>
                  <span 
                    class="days-badge" 
                    :class="getRiskBadgeClass(row.riskZone)"
                  >
                    {{ row.daysUntilExpiry <= 0 ? 'Đã hết date' : `Còn ${row.daysUntilExpiry} ngày` }}
                  </span>
                </div>
              </template>
            </el-table-column>

            <el-table-column prop="initialQuantity" label="Nhập Ban Đầu" width="120" align="right">
              <template #default="{ row }">
                <span class="text-secondary">{{ (row.initialQuantity ?? 0).toLocaleString() }}</span>
              </template>
            </el-table-column>

            <el-table-column prop="remainingQuantity" label="Tồn Hiện Tại" width="130" align="right">
              <template #default="{ row }">
                <span class="font-bold text-emerald">{{ (row.remainingQuantity ?? 0).toLocaleString() }} cái</span>
              </template>
            </el-table-column>

            <el-table-column prop="status" label="Trạng Thái" width="130" align="center">
              <template #default="{ row }">
                <el-tag :type="getBatchStatusTag(row.status)" size="small">
                  {{ getBatchStatusLabel(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>

      <!-- TAB 2: ĐỊNH DANH SERIAL / IMEI & BẢO HÀNH ĐIỆN TỬ -->
      <el-tab-pane label="Định Danh Serial/IMEI & Bảo Hành" name="serials">
        <div class="card-box">
          <div class="filter-toolbar">
            <el-input 
              v-model="serialSearchQuery" 
              placeholder="Tìm mã Serial, IMEI, tên SP, số điện thoại khách..." 
              clearable 
              style="width: 340px;"
              @input="onSerialSearchDebounce"
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>

            <el-select 
              v-model="serialFilterStatus" 
              placeholder="Trạng thái Serial" 
              clearable 
              style="width: 200px;"
              @change="fetchSerials"
            >
              <el-option label="Đang tồn kho (IN_STOCK)" value="IN_STOCK" />
              <el-option label="Đang bảo hành (ACTIVE_WARRANTY)" value="ACTIVE_WARRANTY" />
              <el-option label="Tiếp nhận sửa chữa (UNDER_REPAIR)" value="UNDER_REPAIR" />
              <el-option label="Đổi trả hàng lỗi (RETURNED_DEFECTIVE)" value="RETURNED_DEFECTIVE" />
            </el-select>

            <div class="flex-spacer"></div>

            <el-button 
              v-if="canManage"
              type="primary" 
              plain 
              @click="openCreateSerialDialog"
            >
              <el-icon><Plus /></el-icon>
              <span>Đăng Ký Serial Mới</span>
            </el-button>
          </div>

          <el-table 
            v-loading="loadingSerials" 
            :data="serials" 
            stripe 
            class="enterprise-table"
            empty-text="Chưa có mã Serial/IMEI nào"
          >
            <el-table-column prop="serialNumber" label="Mã Serial / IMEI" width="190">
              <template #default="{ row }">
                <span class="code-badge-dark">{{ row.serialNumber }}</span>
              </template>
            </el-table-column>

            <el-table-column prop="productName" label="Sản Phẩm" min-width="190">
              <template #default="{ row }">
                <div class="name-cell">
                  <span class="font-bold text-dark">{{ row.productName }}</span>
                  <span class="sub-text">{{ row.productSku }}</span>
                </div>
              </template>
            </el-table-column>

            <el-table-column prop="warehouseName" label="Vị Trí Kho" width="160">
              <template #default="{ row }">
                <span class="text-secondary">{{ row.warehouseName || 'Đã giao khách' }}</span>
              </template>
            </el-table-column>

            <el-table-column prop="status" label="Trạng Thái Vòng Đời" width="170" align="center">
              <template #default="{ row }">
                <el-tag :type="getSerialStatusTag(row.status)">
                  {{ getSerialStatusLabel(row.status) }}
                </el-tag>
              </template>
            </el-table-column>

            <el-table-column label="Thông Tin Sở Hữu & Bảo Hành" min-width="210">
              <template #default="{ row }">
                <div v-if="row.customerName" class="meta-stacked">
                  <span class="font-bold text-dark">{{ row.customerName }} ({{ row.customerPhone }})</span>
                  <span class="sub-text">Hạn BH: {{ formatDate(row.warrantyEndDate) }}</span>
                  <el-tag v-if="row.isUnderWarranty" size="small" type="success" effect="plain" class="mt-1">
                    Còn bảo hành
                  </el-tag>
                  <el-tag v-else-if="row.warrantyEndDate" size="small" type="info" effect="plain" class="mt-1">
                    Hết hạn bảo hành
                  </el-tag>
                </div>
                <span v-else class="text-muted text-sm">Chưa bán ra</span>
              </template>
            </el-table-column>

            <el-table-column v-if="canManage" label="Thao Tác" width="120" align="center" fixed="right">
              <template #default="{ row }">
                <el-button size="small" text type="primary" @click="openUpdateSerialDialog(row)">
                  Cập nhật
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>

      <!-- TAB 3: KIỂM KÊ KHO & CÂN ĐỐI THỪA/THIẾU -->
      <el-tab-pane label="Kiểm Kê Kho (Stocktake Audit)" name="stocktakes">
        <div class="card-box">
          <div class="filter-toolbar">
            <el-radio-group v-model="stocktakeStatusFilter" size="default" @change="fetchStocktakes">
              <el-radio-button label="">Tất Cả</el-radio-button>
              <el-radio-button label="DRAFT">Chờ Đếm</el-radio-button>
              <el-radio-button label="IN_PROGRESS">Đang Đếm ({{ activeAuditsCount }})</el-radio-button>
              <el-radio-button label="BALANCED">Đã Cân Đối</el-radio-button>
              <el-radio-button label="CANCELLED">Đã Hủy</el-radio-button>
            </el-radio-group>

            <div class="flex-spacer"></div>

            <el-button 
              v-if="canManage"
              type="primary" 
              class="btn-action-primary" 
              @click="openCreateAuditDialog"
            >
              <el-icon><Plus /></el-icon>
              <span>Lập Phiếu Kiểm Kê Mới</span>
            </el-button>
          </div>

          <el-table 
            v-loading="loadingAudits" 
            :data="stocktakes" 
            stripe 
            class="enterprise-table"
            empty-text="Chưa có phiếu kiểm kê kho nào"
          >
            <el-table-column prop="auditCode" label="Mã Phiếu" width="170">
              <template #default="{ row }">
                <span class="code-badge-blue cursor-pointer" @click="viewAuditDetail(row)">
                  {{ row.auditCode }}
                </span>
              </template>
            </el-table-column>

            <el-table-column prop="title" label="Tiêu Đề & Kho Kiểm Kê" min-width="220">
              <template #default="{ row }">
                <div class="name-cell">
                  <span class="font-bold text-dark">{{ row.title }}</span>
                  <span class="sub-text">Tại kho: {{ row.warehouseName }} • {{ row.auditType === 'PERIODIC' ? 'Định kỳ' : 'Đột xuất' }}</span>
                </div>
              </template>
            </el-table-column>

            <el-table-column prop="totalItems" label="Mặt Hàng" width="110" align="center">
              <template #default="{ row }">
                <span class="font-medium">{{ row.totalItems ?? 0 }} SKU</span>
              </template>
            </el-table-column>

            <el-table-column prop="totalDiscrepancyQty" label="Độ Chênh Lệch" width="130" align="center">
              <template #default="{ row }">
                <span 
                  v-if="(row.totalDiscrepancyQty ?? 0) === 0" 
                  class="text-emerald font-bold"
                >
                  Khớp 100%
                </span>
                <span 
                  v-else 
                  class="text-rose font-bold"
                >
                  Lệch {{ row.totalDiscrepancyQty }} cái
                </span>
              </template>
            </el-table-column>

            <el-table-column prop="status" label="Trạng Thái" width="140" align="center">
              <template #default="{ row }">
                <el-tag :type="getStocktakeStatusTag(row.status)">
                  {{ getStocktakeStatusLabel(row.status) }}
                </el-tag>
              </template>
            </el-table-column>

            <el-table-column prop="createdBy" label="Người Lập & Ngày Tạo" width="160">
              <template #default="{ row }">
                <div class="meta-stacked">
                  <span class="font-medium text-dark">{{ row.createdBy }}</span>
                  <span class="sub-text">{{ formatDateTime(row.createdAt) }}</span>
                </div>
              </template>
            </el-table-column>

            <el-table-column label="Thao Tác" width="220" align="center" fixed="right">
              <template #default="{ row }">
                <div class="actions-group">
                  <el-button size="small" text type="primary" @click="viewAuditDetail(row)">
                    {{ row.status === 'BALANCED' ? 'Xem kết quả' : 'Đếm hàng' }}
                  </el-button>

                  <!-- Nút Cân Đối Tồn Kho khi chưa Balanced -->
                  <el-button 
                    v-if="(row.status === 'DRAFT' || row.status === 'IN_PROGRESS') && canManage" 
                    size="small" 
                    type="success" 
                    plain
                    @click="confirmBalanceAudit(row)"
                  >
                    Cân đối
                  </el-button>

                  <!-- Nút Hủy phiếu -->
                  <el-button 
                    v-if="(row.status === 'DRAFT' || row.status === 'IN_PROGRESS') && canManage" 
                    size="small" 
                    text 
                    type="danger"
                    @click="cancelAudit(row)"
                  >
                    Hủy
                  </el-button>
                </div>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- ======================================================== -->
    <!-- DIALOG: THÊM LÔ HÀNG MỚI (BATCH/LOT) -->
    <!-- ======================================================== -->
    <el-dialog 
      v-model="createBatchDialogVisible" 
      title="Nhập Lô Hàng Mới (Batch / Lot Number)"
      width="600px"
      destroy-on-close
    >
      <el-form :model="batchForm" label-position="top">
        <el-form-item label="Sản Phẩm *" required>
          <el-select v-model="batchForm.productId" placeholder="Chọn sản phẩm..." filterable style="width: 100%;">
            <el-option 
              v-for="p in allProducts" 
              :key="p.id" 
              :label="`[${p.sku}] ${p.name}`" 
              :value="p.id" 
            />
          </el-select>
        </el-form-item>

        <div class="grid-2-col">
          <el-form-item label="Kho Hàng Tiếp Nhận *" required>
            <el-select v-model="batchForm.warehouseId" placeholder="Chọn kho..." style="width: 100%;">
              <el-option 
                v-for="w in warehouses" 
                :key="w.id" 
                :label="`${w.name} (${w.branchName})`" 
                :value="w.id" 
              />
            </el-select>
          </el-form-item>

          <el-form-item label="Mã Lô (Tùy chọn, để trống tự sinh)">
            <el-input v-model="batchForm.batchCode" placeholder="Ví dụ: LOT-202611-01" />
          </el-form-item>
        </div>

        <div class="grid-2-col">
          <el-form-item label="Ngày Sản Xuất">
            <el-date-picker 
              v-model="batchForm.manufacturingDate" 
              type="date" 
              placeholder="Chọn ngày" 
              style="width: 100%;" 
            />
          </el-form-item>

          <el-form-item label="Hạn Sử Dụng (Expiry Date) *" required>
            <el-date-picker 
              v-model="batchForm.expiryDate" 
              type="date" 
              placeholder="Chọn hạn sử dụng" 
              style="width: 100%;" 
            />
          </el-form-item>
        </div>

        <div class="grid-2-col">
          <el-form-item label="Số Lượng Nhập Lô *" required>
            <el-input-number v-model="batchForm.initialQuantity" :min="1" style="width: 100%;" />
          </el-form-item>

          <el-form-item label="Giá Vốn Nhập Lô (VNĐ)">
            <el-input-number v-model="batchForm.costPrice" :min="0" :step="1000" style="width: 100%;" />
          </el-form-item>
        </div>

        <el-form-item label="Nhà Cung Cấp">
          <el-input v-model="batchForm.supplierName" placeholder="Tên đối tác / xưởng sản xuất..." />
        </el-form-item>

        <el-form-item label="Ghi Chú">
          <el-input v-model="batchForm.notes" placeholder="Ghi chú về lô hàng..." />
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="createBatchDialogVisible = false">Hủy</el-button>
          <el-button type="primary" :loading="savingBatch" @click="saveBatch">
            Lưu Lô Hàng
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- ======================================================== -->
    <!-- DIALOG: MÔ PHỎNG THUẬT TOÁN XUẤT KHO FEFO / FIFO -->
    <!-- ======================================================== -->
    <el-dialog 
      v-model="fefoDialogVisible" 
      title="Mô Phỏng Thuật Toán Phân Bổ Xuất Kho FEFO / FIFO"
      width="680px"
      destroy-on-close
    >
      <div class="fefo-body">
        <el-alert 
          type="success" 
          show-icon 
          :closable="false" 
          class="mb-3"
          title="Thuật toán FEFO (First Expired, First Out): Tự động tính toán và gợi ý bốc hàng từ các lô có hạn sử dụng gần nhất trước, giúp triệt tiêu rủi ro hàng ứ đọng hết date."
        />

        <div class="grid-3-col">
          <el-form-item label="Sản Phẩm">
            <el-select v-model="fefoProductId" placeholder="Chọn sản phẩm..." filterable style="width: 100%;">
              <el-option 
                v-for="p in allProducts" 
                :key="p.id" 
                :label="`[${p.sku}] ${p.name}`" 
                :value="p.id" 
              />
            </el-select>
          </el-form-item>

          <el-form-item label="Số Lượng Cần Xuất">
            <el-input-number v-model="fefoQuantity" :min="1" style="width: 100%;" />
          </el-form-item>

          <el-form-item label="Chiến Lược">
            <el-select v-model="fefoStrategy" style="width: 100%;">
              <el-option label="FEFO (Hạn gần trước)" value="FEFO" />
              <el-option label="FIFO (Nhập trước trước)" value="FIFO" />
            </el-select>
          </el-form-item>
        </div>

        <el-button type="primary" :loading="allocatingFefo" @click="runFefoSimulation">
          Chạy Phân Bổ Thuật Toán
        </el-button>

        <!-- KẾT QUẢ PHÂN BỔ -->
        <div v-if="fefoResult" class="fefo-results mt-3">
          <div class="fefo-summary-bar">
            <span>Yêu cầu: <strong>{{ fefoResult.requestedQuantity }}</strong></span>
            <span>Đã phân bổ: <strong class="text-emerald">{{ fefoResult.allocatedQuantity }}</strong></span>
            <span>Chiến lược: <el-tag size="small">{{ fefoResult.strategy }}</el-tag></span>
          </div>

          <el-table :data="fefoResult.allocations" stripe class="enterprise-table mt-2">
            <el-table-column prop="batchCode" label="Mã Lô Được Chọn" width="160">
              <template #default="{ row }">
                <span class="code-badge-blue">{{ row.batchCode }}</span>
              </template>
            </el-table-column>

            <el-table-column prop="warehouseName" label="Kho Chứa" min-width="150" />

            <el-table-column prop="expiryDate" label="Hạn Sử Dụng" width="130">
              <template #default="{ row }">
                <span class="font-bold text-dark">{{ formatDate(row.expiryDate) }}</span>
              </template>
            </el-table-column>

            <el-table-column prop="remainingQuantity" label="Tồn Lô Còn" width="110" align="right" />

            <el-table-column prop="quantityToPick" label="Số Lấy Từ Lô" width="130" align="right">
              <template #default="{ row }">
                <span class="font-bold text-rose">+{{ row.quantityToPick }} cái</span>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>
      <template #footer>
        <el-button @click="fefoDialogVisible = false">Đóng</el-button>
      </template>
    </el-dialog>

    <!-- ======================================================== -->
    <!-- DIALOG: ĐĂNG KÝ SERIAL / IMEI MỚI -->
    <!-- ======================================================== -->
    <el-dialog 
      v-model="createSerialDialogVisible" 
      title="Đăng Ký Mã Định Danh Serial / IMEI"
      width="540px"
      destroy-on-close
    >
      <el-form :model="serialForm" label-position="top">
        <el-form-item label="Sản Phẩm *" required>
          <el-select v-model="serialForm.productId" placeholder="Chọn sản phẩm..." filterable style="width: 100%;">
            <el-option 
              v-for="p in allProducts" 
              :key="p.id" 
              :label="`[${p.sku}] ${p.name}`" 
              :value="p.id" 
            />
          </el-select>
        </el-form-item>

        <el-form-item label="Kho Hàng Lưu Trữ">
          <el-select v-model="serialForm.warehouseId" placeholder="Chọn kho..." style="width: 100%;">
            <el-option 
              v-for="w in warehouses" 
              :key="w.id" 
              :label="`${w.name} (${w.branchName})`" 
              :value="w.id" 
            />
          </el-select>
        </el-form-item>

        <el-form-item label="Mã Serial / IMEI *" required>
          <el-input v-model="serialForm.serialNumber" placeholder="Ví dụ: SN-IP15P-VN009" />
        </el-form-item>

        <el-form-item label="Thời Hạn Bảo Hành Mặc Định (Tháng)">
          <el-input-number v-model="serialForm.warrantyMonths" :min="1" :max="60" style="width: 100%;" />
        </el-form-item>

        <el-form-item label="Ghi Chú">
          <el-input v-model="serialForm.notes" placeholder="Ghi chú về thiết bị..." />
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="createSerialDialogVisible = false">Hủy</el-button>
          <el-button type="primary" :loading="savingSerial" @click="saveSerial">
            Đăng Ký Serial
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- ======================================================== -->
    <!-- DIALOG: CẬP NHẬT TRẠNG THÁI SERIAL & BẢO HÀNH -->
    <!-- ======================================================== -->
    <el-dialog 
      v-model="updateSerialDialogVisible" 
      title="Cập Nhật Trạng Thái & Bảo Hành Serial"
      width="560px"
      destroy-on-close
    >
      <div v-if="editingSerial" class="edit-serial-body">
        <div class="serial-top-badge">
          <span class="code-badge-dark">{{ editingSerial.serialNumber }}</span>
          <h4 class="font-bold text-dark mt-1">{{ editingSerial.productName }}</h4>
        </div>

        <el-form :model="serialUpdateForm" label-position="top" class="mt-3">
          <el-form-item label="Trạng Thái Vòng Đời *" required>
            <el-select v-model="serialUpdateForm.newStatus" style="width: 100%;">
              <el-option label="Đang tồn kho (IN_STOCK)" value="IN_STOCK" />
              <el-option label="Đang giữ chỗ đơn hàng (RESERVED)" value="RESERVED" />
              <el-option label="Đã xuất bán & Kích hoạt bảo hành (DELIVERED)" value="DELIVERED" />
              <el-option label="Đang trong thời hạn bảo hành (ACTIVE_WARRANTY)" value="ACTIVE_WARRANTY" />
              <el-option label="Tiếp nhận bảo hành sửa chữa (UNDER_REPAIR)" value="UNDER_REPAIR" />
              <el-option label="Hàng lỗi đổi trả (RETURNED_DEFECTIVE)" value="RETURNED_DEFECTIVE" />
            </el-select>
          </el-form-item>

          <div class="grid-2-col">
            <el-form-item label="Tên Khách Hàng Sở Hữu">
              <el-input v-model="serialUpdateForm.customerName" placeholder="Tên khách hàng..." />
            </el-form-item>

            <el-form-item label="Số Điện Thoại">
              <el-input v-model="serialUpdateForm.customerPhone" placeholder="SĐT khách hàng..." />
            </el-form-item>
          </div>

          <div class="grid-2-col">
            <el-form-item label="Mã Đơn Hàng Bán">
              <el-input v-model="serialUpdateForm.orderCode" placeholder="Ví dụ: ORD-202611-001" />
            </el-form-item>

            <el-form-item label="Thời Hạn BH (Tháng)">
              <el-input-number v-model="serialUpdateForm.warrantyMonths" :min="1" :max="60" style="width: 100%;" />
            </el-form-item>
          </div>

          <el-form-item label="Ghi Chú Vòng Đời">
            <el-input v-model="serialUpdateForm.notes" placeholder="Lịch sử bảo hành / sửa chữa..." />
          </el-form-item>
        </el-form>
      </div>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="updateSerialDialogVisible = false">Hủy</el-button>
          <el-button type="primary" :loading="savingSerialUpdate" @click="submitUpdateSerial">
            Cập Nhật Trạng Thái
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- ======================================================== -->
    <!-- DIALOG: TẠO PHIẾU KIỂM KÊ KHO MỚI -->
    <!-- ======================================================== -->
    <el-dialog 
      v-model="createAuditDialogVisible" 
      title="Tạo Phiếu Kiểm Kê Kho (Stocktake Audit)"
      width="600px"
      destroy-on-close
    >
      <el-form :model="auditForm" label-position="top">
        <el-form-item label="Kho Hàng Cần Kiểm Kê *" required>
          <el-select v-model="auditForm.warehouseId" placeholder="Chọn kho kiểm kê..." style="width: 100%;">
            <el-option 
              v-for="w in warehouses" 
              :key="w.id" 
              :label="`${w.name} (${w.branchName})`" 
              :value="w.id" 
            />
          </el-select>
        </el-form-item>

        <el-form-item label="Tiêu Đề Đợt Kiểm Kê *" required>
          <el-input v-model="auditForm.title" placeholder="Ví dụ: Kiểm kê định kỳ cuối tháng 11" />
        </el-form-item>

        <el-form-item label="Loại Hình Kiểm Kê">
          <el-select v-model="auditForm.auditType" style="width: 100%;">
            <el-option label="Kiểm Kê Định Kỳ Toàn Bộ (PERIODIC)" value="PERIODIC" />
            <el-option label="Kiểm Kê Đột Xuất Theo Mặt Hàng (SPOT_CHECK)" value="SPOT_CHECK" />
          </el-select>
        </el-form-item>

        <el-form-item label="Ghi Chú Đợt Kiểm Kê">
          <el-input v-model="auditForm.notes" placeholder="Mục đích đợt kiểm kê, thành phần tổ kiểm kê..." />
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="createAuditDialogVisible = false">Hủy</el-button>
          <el-button type="primary" :loading="savingAudit" @click="saveAudit">
            Tạo Phiếu Kiểm Kê
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- ======================================================== -->
    <!-- DIALOG: NHẬP SỐ ĐẾM & CHI TIẾT KIỂM KÊ -->
    <!-- ======================================================== -->
    <el-dialog 
      v-model="auditDetailDialogVisible" 
      title="Chi Tiết & Ghi Nhận Kiểm Đếm Thực Tế"
      width="820px"
      destroy-on-close
    >
      <div v-if="currentAudit" class="audit-detail-body">
        <div class="audit-top-bar">
          <div>
            <span class="code-badge-blue">{{ currentAudit.auditCode }}</span>
            <h3 class="font-bold text-dark mt-1">{{ currentAudit.title }}</h3>
            <p class="text-secondary text-sm">Tại kho: {{ currentAudit.warehouseName }}</p>
          </div>
          <el-tag :type="getStocktakeStatusTag(currentAudit.status)" size="large">
            {{ getStocktakeStatusLabel(currentAudit.status) }}
          </el-tag>
        </div>

        <el-table :data="auditCountItems" stripe class="enterprise-table mt-3">
          <el-table-column prop="productName" label="Sản Phẩm" min-width="190">
            <template #default="{ row }">
              <div class="name-cell">
                <span class="font-bold text-dark">{{ row.productName }}</span>
                <span class="sub-text">{{ row.productSku }}</span>
              </div>
            </template>
          </el-table-column>

          <el-table-column prop="systemStock" label="Tồn Hệ Thống" width="120" align="center">
            <template #default="{ row }">
              <span class="font-bold">{{ row.systemStock }}</span>
            </template>
          </el-table-column>

          <el-table-column label="Thực Tế Đếm Được" width="160" align="center">
            <template #default="{ row }">
              <el-input-number 
                v-model="row.countedStock" 
                :min="0" 
                :disabled="currentAudit.status === 'BALANCED' || currentAudit.status === 'CANCELLED'"
                size="small"
                style="width: 120px;"
                @change="onCountChange(row)"
              />
            </template>
          </el-table-column>

          <el-table-column label="Chênh Lệch" width="120" align="center">
            <template #default="{ row }">
              <span 
                class="font-bold"
                :class="(row.differenceQty ?? 0) === 0 ? 'text-secondary' : ((row.differenceQty ?? 0) > 0 ? 'text-emerald' : 'text-rose')"
              >
                {{ (row.differenceQty ?? 0) > 0 ? `+${row.differenceQty}` : row.differenceQty }}
              </span>
            </template>
          </el-table-column>

          <el-table-column label="Lý Do Chênh Lệch" min-width="160">
            <template #default="{ row }">
              <el-input 
                v-model="row.reason" 
                placeholder="Hao hụt / Thừa..." 
                size="small"
                :disabled="currentAudit.status === 'BALANCED' || currentAudit.status === 'CANCELLED'"
              />
            </template>
          </el-table-column>
        </el-table>
      </div>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="auditDetailDialogVisible = false">Đóng</el-button>
          <el-button 
            v-if="currentAudit && currentAudit.status !== 'BALANCED' && currentAudit.status !== 'CANCELLED' && canManage"
            type="primary" 
            :loading="savingCounts" 
            @click="submitCounts"
          >
            Lưu Kết Quả Đếm
          </el-button>
          <el-button 
            v-if="currentAudit && currentAudit.status !== 'BALANCED' && currentAudit.status !== 'CANCELLED' && canManage"
            type="success" 
            @click="confirmBalanceAudit(currentAudit)"
          >
            Cân Đối Tồn Kho
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
// ==========================================
// 1. HELPER FUNCTIONS PLACED AT THE TOP (ZERO TDZ ERROR)
// ==========================================
const formatDate = (val) => {
  if (!val) return '—'
  const d = new Date(val)
  if (isNaN(d.getTime())) return String(val)
  return d.toLocaleDateString('vi-VN', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric'
  })
}

const formatDateTime = (val) => {
  if (!val) return '—'
  const d = new Date(val)
  if (isNaN(d.getTime())) return String(val)
  return d.toLocaleString('vi-VN', {
    hour: '2-digit',
    minute: '2-digit',
    day: '2-digit',
    month: '2-digit',
    year: 'numeric'
  })
}

const getBatchStatusLabel = (status) => {
  switch (status) {
    case 'ACTIVE': return 'Đang bán'
    case 'NEAR_EXPIRY': return 'Cận Date'
    case 'EXPIRED': return 'Đã hết hạn'
    case 'DEPLETED': return 'Đã hết hàng'
    default: return status || '—'
  }
}

const getBatchStatusTag = (status) => {
  switch (status) {
    case 'ACTIVE': return 'success'
    case 'NEAR_EXPIRY': return 'warning'
    case 'EXPIRED': return 'danger'
    case 'DEPLETED': return 'info'
    default: return 'info'
  }
}

const getRiskBadgeClass = (zone) => {
  switch (zone) {
    case 'RED': return 'risk-badge-red'
    case 'YELLOW': return 'risk-badge-yellow'
    case 'GREEN': return 'risk-badge-green'
    default: return 'risk-badge-green'
  }
}

const getSerialStatusLabel = (status) => {
  switch (status) {
    case 'IN_STOCK': return 'Tồn trong kho'
    case 'RESERVED': return 'Giữ chỗ đơn'
    case 'DELIVERED': return 'Đã giao khách'
    case 'ACTIVE_WARRANTY': return 'Đang bảo hành'
    case 'UNDER_REPAIR': return 'Tiếp nhận sửa'
    case 'RETURNED_DEFECTIVE': return 'Lỗi đổi trả'
    default: return status || '—'
  }
}

const getSerialStatusTag = (status) => {
  switch (status) {
    case 'IN_STOCK': return 'info'
    case 'RESERVED': return 'warning'
    case 'DELIVERED': return 'primary'
    case 'ACTIVE_WARRANTY': return 'success'
    case 'UNDER_REPAIR': return 'danger'
    case 'RETURNED_DEFECTIVE': return 'danger'
    default: return 'info'
  }
}

const getStocktakeStatusLabel = (status) => {
  switch (status) {
    case 'DRAFT': return 'Chờ đếm'
    case 'IN_PROGRESS': return 'Đang đếm'
    case 'BALANCED': return 'Đã cân đối'
    case 'CANCELLED': return 'Đã hủy'
    default: return status || '—'
  }
}

const getStocktakeStatusTag = (status) => {
  switch (status) {
    case 'DRAFT': return 'info'
    case 'IN_PROGRESS': return 'warning'
    case 'BALANCED': return 'success'
    case 'CANCELLED': return 'danger'
    default: return 'info'
  }
}

// ==========================================
// 2. IMPORTS & STATE DEFINITIONS
// ==========================================
import { ref, computed, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../api'
import { useAuthStore } from '../store/auth'
import {
  List,
  Box,
  Tickets,
  Operation,
  CircleCheckFilled,
  WarningFilled,
  CircleCloseFilled,
  DocumentChecked,
  Search,
  Plus,
  Refresh,
  ArrowDown
} from '@element-plus/icons-vue'

const authStore = useAuthStore()
const canManage = computed(() => {
  return authStore.isAdmin || authStore.hasPermission('BRANCH_MANAGE') || authStore.user?.role === 'WAREHOUSE'
})

const activeTab = ref('batches')

// Core Lists
const batches = ref([])
const serials = ref([])
const stocktakes = ref([])
const warehouses = ref([])
const allProducts = ref([])

// Loadings
const loadingBatches = ref(false)
const loadingSerials = ref(false)
const loadingAudits = ref(false)

// Radar Summary
const radarSummary = reactive({
  totalActiveBatches: 0,
  redCount: 0,
  yellowCount: 0,
  greenCount: 0,
  expiredCount: 0
})

// Filters
const batchFilterWarehouse = ref(null)
const batchRiskFilter = ref('ALL')
const serialSearchQuery = ref('')
const serialFilterStatus = ref('')
const stocktakeStatusFilter = ref('')

const activeAuditsCount = computed(() => {
  return stocktakes.value.filter(s => s.status === 'IN_PROGRESS' || s.status === 'DRAFT').length
})

const filteredBatches = computed(() => {
  if (batchRiskFilter.value === 'ALL') return batches.value
  return batches.value.filter(b => b.riskZone === batchRiskFilter.value)
})

// ==========================================
// 3. DIALOGS & FORMS
// ==========================================

// Batch Form
const createBatchDialogVisible = ref(false)
const savingBatch = ref(false)
const batchForm = reactive({
  batchCode: '',
  productId: null,
  warehouseId: null,
  manufacturingDate: null,
  expiryDate: null,
  initialQuantity: 10,
  costPrice: 0,
  supplierName: '',
  notes: ''
})

// FEFO Simulation Dialog
const fefoDialogVisible = ref(false)
const fefoProductId = ref(null)
const fefoQuantity = ref(10)
const fefoStrategy = ref('FEFO')
const allocatingFefo = ref(false)
const fefoResult = ref(null)

// Serial Form
const createSerialDialogVisible = ref(false)
const savingSerial = ref(false)
const serialForm = reactive({
  serialNumber: '',
  productId: null,
  warehouseId: null,
  warrantyMonths: 12,
  notes: ''
})

// Serial Update Form
const updateSerialDialogVisible = ref(false)
const savingSerialUpdate = ref(false)
const editingSerial = ref(null)
const serialUpdateForm = reactive({
  newStatus: 'IN_STOCK',
  customerName: '',
  customerPhone: '',
  orderCode: '',
  warrantyMonths: 12,
  notes: ''
})

// Stocktake Form
const createAuditDialogVisible = ref(false)
const savingAudit = ref(false)
const auditForm = reactive({
  warehouseId: null,
  title: '',
  auditType: 'PERIODIC',
  notes: ''
})

// Audit Detail / Count Dialog
const auditDetailDialogVisible = ref(false)
const currentAudit = ref(null)
const auditCountItems = ref([])
const savingCounts = ref(false)

// Debounce timer for search
let serialSearchTimer = null
const onSerialSearchDebounce = () => {
  clearTimeout(serialSearchTimer)
  serialSearchTimer = setTimeout(() => {
    fetchSerials()
  }, 300)
}

// ==========================================
// 4. API METHODS
// ==========================================

const fetchBatchesAndRadar = async () => {
  loadingBatches.value = true
  try {
    const params = {}
    if (batchFilterWarehouse.value) params.warehouseId = batchFilterWarehouse.value

    const [batchRes, radarRes] = await Promise.all([
      api.get('/inventory/batches', { params }),
      api.get('/inventory/batches/radar')
    ])

    batches.value = batchRes.data || []
    if (radarRes.data?.summary) {
      Object.assign(radarSummary, radarRes.data.summary)
    }
  } catch (err) {
    ElMessage.error(err.response?.data?.message || 'Không thể tải dữ liệu Lô hàng & Radar')
  } finally {
    loadingBatches.value = false
  }
}

const fetchSerials = async () => {
  loadingSerials.value = true
  try {
    const params = {}
    if (serialSearchQuery.value) params.query = serialSearchQuery.value
    if (serialFilterStatus.value) params.status = serialFilterStatus.value

    const res = await api.get('/inventory/serials', { params })
    serials.value = res.data || []
  } catch (err) {
    ElMessage.error(err.response?.data?.message || 'Không thể tải danh sách Serial/IMEI')
  } finally {
    loadingSerials.value = false
  }
}

const fetchStocktakes = async () => {
  loadingAudits.value = true
  try {
    const params = {}
    if (stocktakeStatusFilter.value) params.status = stocktakeStatusFilter.value

    const res = await api.get('/inventory/stocktakes', { params })
    stocktakes.value = res.data || []
  } catch (err) {
    ElMessage.error(err.response?.data?.message || 'Không thể tải phiếu kiểm kê')
  } finally {
    loadingAudits.value = false
  }
}

const fetchWarehousesAndProducts = async () => {
  try {
    const [whRes, pRes] = await Promise.all([
      api.get('/warehouses'),
      api.get('/products')
    ])
    warehouses.value = whRes.data || []
    allProducts.value = pRes.data?.content || pRes.data || []
  } catch {
    // ignore
  }
}

const handleHeaderCommand = (cmd) => {
  if (cmd === 'create_batch') openCreateBatchDialog()
  else if (cmd === 'create_serial') openCreateSerialDialog()
  else if (cmd === 'test_fefo') openFefoDialog()
}

// Batch Actions
const openCreateBatchDialog = () => {
  batchForm.batchCode = ''
  batchForm.productId = allProducts.value[0]?.id ?? null
  batchForm.warehouseId = warehouses.value[0]?.id ?? null
  batchForm.manufacturingDate = new Date()
  const exp = new Date()
  exp.setDate(exp.getDate() + 180)
  batchForm.expiryDate = exp
  batchForm.initialQuantity = 20
  batchForm.costPrice = 0
  batchForm.supplierName = ''
  batchForm.notes = ''
  createBatchDialogVisible.value = true
}

const saveBatch = async () => {
  if (!batchForm.productId || !batchForm.warehouseId || !batchForm.expiryDate) {
    return ElMessage.warning('Vui lòng điền đầy đủ sản phẩm, kho và hạn sử dụng')
  }
  savingBatch.value = true
  try {
    await api.post('/inventory/batches', batchForm)
    ElMessage.success('Đã nhập lô hàng mới thành công')
    createBatchDialogVisible.value = false
    await fetchBatchesAndRadar()
  } catch (err) {
    ElMessage.error(err.response?.data?.message || 'Lưu lô hàng thất bại')
  } finally {
    savingBatch.value = false
  }
}

// FEFO Simulation
const openFefoDialog = () => {
  fefoProductId.value = allProducts.value[0]?.id ?? null
  fefoQuantity.value = 10
  fefoStrategy.value = 'FEFO'
  fefoResult.value = null
  fefoDialogVisible.value = true
}

const runFefoSimulation = async () => {
  if (!fefoProductId.value || fefoQuantity.value <= 0) {
    return ElMessage.warning('Vui lòng chọn sản phẩm và số lượng cần xuất')
  }
  allocatingFefo.value = true
  try {
    const res = await api.get('/inventory/batches/allocate', {
      params: {
        productId: fefoProductId.value,
        quantity: fefoQuantity.value,
        strategy: fefoStrategy.value
      }
    })
    fefoResult.value = res.data
    ElMessage.success(`Thuật toán ${fefoStrategy.value} đã phân bổ ${res.data.allocatedQuantity}/${fefoQuantity.value} sản phẩm`)
  } catch (err) {
    ElMessage.error(err.response?.data?.message || 'Mô phỏng phân bổ thất bại')
  } finally {
    allocatingFefo.value = false
  }
}

// Serial Actions
const openCreateSerialDialog = () => {
  serialForm.serialNumber = 'SN-' + Date.now().toString().slice(-8)
  serialForm.productId = allProducts.value[0]?.id ?? null
  serialForm.warehouseId = warehouses.value[0]?.id ?? null
  serialForm.warrantyMonths = 12
  serialForm.notes = ''
  createSerialDialogVisible.value = true
}

const saveSerial = async () => {
  if (!serialForm.serialNumber.trim() || !serialForm.productId) {
    return ElMessage.warning('Vui lòng nhập mã Serial và chọn sản phẩm')
  }
  savingSerial.value = true
  try {
    await api.post('/inventory/serials', serialForm)
    ElMessage.success('Đăng ký mã Serial/IMEI thành công')
    createSerialDialogVisible.value = false
    await fetchSerials()
  } catch (err) {
    ElMessage.error(err.response?.data?.message || 'Đăng ký Serial thất bại')
  } finally {
    savingSerial.value = false
  }
}

const openUpdateSerialDialog = (row) => {
  editingSerial.value = row
  serialUpdateForm.newStatus = row.status
  serialUpdateForm.customerName = row.customerName || ''
  serialUpdateForm.customerPhone = row.customerPhone || ''
  serialUpdateForm.orderCode = row.orderCode || ''
  serialUpdateForm.warrantyMonths = row.warrantyMonths || 12
  serialUpdateForm.notes = ''
  updateSerialDialogVisible.value = true
}

const submitUpdateSerial = async () => {
  if (!editingSerial.value) return
  savingSerialUpdate.value = true
  try {
    await api.patch(`/inventory/serials/${editingSerial.value.id}/status`, serialUpdateForm)
    ElMessage.success('Cập nhật trạng thái Serial thành công')
    updateSerialDialogVisible.value = false
    await fetchSerials()
  } catch (err) {
    ElMessage.error(err.response?.data?.message || 'Cập nhật Serial thất bại')
  } finally {
    savingSerialUpdate.value = false
  }
}

// Stocktake Actions
const openCreateAuditDialog = () => {
  auditForm.warehouseId = warehouses.value[0]?.id ?? null
  auditForm.title = 'Kiểm kê định kỳ Kho ' + (warehouses.value[0]?.name || '')
  auditForm.auditType = 'PERIODIC'
  auditForm.notes = ''
  createAuditDialogVisible.value = true
}

const saveAudit = async () => {
  if (!auditForm.warehouseId || !auditForm.title.trim()) {
    return ElMessage.warning('Vui lòng chọn kho và nhập tiêu đề kiểm kê')
  }
  savingAudit.value = true
  try {
    await api.post('/inventory/stocktakes', auditForm)
    ElMessage.success('Đã tạo phiếu kiểm kê kho mới')
    createAuditDialogVisible.value = false
    await fetchStocktakes()
    activeTab.value = 'stocktakes'
  } catch (err) {
    ElMessage.error(err.response?.data?.message || 'Tạo phiếu kiểm kê thất bại')
  } finally {
    savingAudit.value = false
  }
}

const viewAuditDetail = (row) => {
  currentAudit.value = row
  auditCountItems.value = (row.items || []).map(it => ({
    itemId: it.id,
    productId: it.productId,
    productName: it.productName,
    productSku: it.productSku,
    systemStock: it.systemStock,
    countedStock: it.countedStock,
    differenceQty: it.differenceQty,
    reason: it.reason || ''
  }))
  auditDetailDialogVisible.value = true
}

const onCountChange = (row) => {
  row.differenceQty = (row.countedStock ?? 0) - (row.systemStock ?? 0)
}

const submitCounts = async () => {
  if (!currentAudit.value) return
  savingCounts.value = true
  try {
    const payload = {
      notes: 'Ghi nhận số đếm lúc ' + new Date().toLocaleTimeString(),
      counts: auditCountItems.value.map(it => ({
        itemId: it.itemId,
        productId: it.productId,
        countedStock: it.countedStock,
        reason: it.reason
      }))
    }
    await api.post(`/inventory/stocktakes/${currentAudit.value.id}/counts`, payload)
    ElMessage.success('Đã lưu kết quả kiểm đếm thực tế')
    await fetchStocktakes()
    // Refresh currentAudit
    const updated = stocktakes.value.find(s => s.id === currentAudit.value.id)
    if (updated) currentAudit.value = updated
  } catch (err) {
    ElMessage.error(err.response?.data?.message || 'Lưu số đếm thất bại')
  } finally {
    savingCounts.value = false
  }
}

const confirmBalanceAudit = async (row) => {
  try {
    await ElMessageBox.confirm(
      `Cân đối và hoàn tất đợt kiểm kê [${row.auditCode}]? Tồn kho thực tế của các sản phẩm chênh lệch sẽ được cập nhật trực tiếp vào hệ thống.`,
      'Xác Nhận Cân Đối Tồn Kho',
      {
        confirmButtonText: 'Cân Đối Ngay',
        cancelButtonText: 'Đóng',
        type: 'warning'
      }
    )

    await api.post(`/inventory/stocktakes/${row.id}/balance`)
    ElMessage.success(`Phiếu kiểm kê ${row.auditCode} đã được cân đối kho hoàn tất!`)
    auditDetailDialogVisible.value = false
    await fetchStocktakes()
  } catch (err) {
    if (err !== 'cancel') {
      ElMessage.error(err.response?.data?.message || 'Cân đối kiểm kê thất bại')
    }
  }
}

const cancelAudit = async (row) => {
  try {
    await ElMessageBox.confirm(
      `Hủy phiếu kiểm kê [${row.auditCode}]?`,
      'Hủy Phiếu Kiểm Kê',
      {
        confirmButtonText: 'Xác Nhận Hủy',
        cancelButtonText: 'Đóng',
        type: 'danger'
      }
    )

    await api.post(`/inventory/stocktakes/${row.id}/cancel`)
    ElMessage.success(`Đã hủy phiếu ${row.auditCode}`)
    await fetchStocktakes()
  } catch (err) {
    if (err !== 'cancel') {
      ElMessage.error(err.response?.data?.message || 'Hủy phiếu thất bại')
    }
  }
}

// ==========================================
// 5. LIFECYCLE
// ==========================================
onMounted(async () => {
  await Promise.all([
    fetchBatchesAndRadar(),
    fetchSerials(),
    fetchStocktakes(),
    fetchWarehousesAndProducts()
  ])
})
</script>

<style scoped>
.inventory-audit-container {
  padding: 24px;
  max-width: 1560px;
  margin: 0 auto;
  padding-bottom: 80px;
}

/* Page Header */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 24px;
  gap: 16px;
  flex-wrap: wrap;
}

.title-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.page-title {
  font-size: 24px;
  font-weight: 800;
  color: #0f172a;
  letter-spacing: -0.02em;
  margin: 0;
}

.badge-enterprise {
  font-size: 11px;
  font-weight: 700;
  padding: 3px 8px;
  border-radius: 999px;
  background: linear-gradient(135deg, #2563eb, #1d4ed8);
  color: #ffffff;
  letter-spacing: 0.05em;
}

.page-subtitle {
  font-size: 14px;
  color: #64748b;
  margin-top: 4px;
  margin-bottom: 0;
}

.header-actions {
  display: flex;
  gap: 10px;
  align-items: center;
}

.btn-action-primary {
  height: 38px;
  font-weight: 600;
  border-radius: 8px;
}

.btn-action-secondary {
  height: 38px;
  font-weight: 600;
  border-radius: 8px;
}

/* Stats Overview */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 16px;
  margin-bottom: 24px;
}

.stat-card {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 16px 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  cursor: pointer;
  transition: all 0.2s ease;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.06);
}

.stat-icon-wrapper {
  width: 48px;
  height: 48px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
}

.stat-emerald { background: #ecfdf5; color: #059669; }
.stat-amber { background: #fffbeb; color: #d97706; }
.stat-rose { background: #fff1f2; color: #e11d48; }
.stat-blue { background: #eff6ff; color: #2563eb; }

.stat-value {
  font-size: 24px;
  font-weight: 800;
  color: #0f172a;
  line-height: 1.2;
}

.stat-label {
  font-size: 13px;
  color: #64748b;
  font-weight: 500;
  margin-top: 2px;
}

/* Radar Meter Banner */
.radar-meter-banner {
  background: linear-gradient(135deg, #f8fafc, #eff6ff);
  border: 1px solid #bfdbfe;
  border-radius: 10px;
  padding: 16px 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}

.radar-title {
  font-size: 13px;
  font-weight: 800;
  color: #1e40af;
  letter-spacing: 0.05em;
}

.radar-sub {
  font-size: 13px;
  color: #475569;
  margin: 4px 0 0 0;
}

.radar-actions {
  display: flex;
  gap: 10px;
}

/* Card Box */
.card-box {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.03);
}

.filter-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.flex-spacer {
  flex: 1;
}

/* Tables */
.enterprise-table {
  border-radius: 8px;
  overflow: hidden;
  margin-top: 14px;
}

.code-badge-blue {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 12px;
  font-weight: 700;
  padding: 3px 8px;
  border-radius: 4px;
  background: #eff6ff;
  color: #2563eb;
}

.code-badge-dark {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 12px;
  font-weight: 700;
  padding: 3px 8px;
  border-radius: 4px;
  background: #0f172a;
  color: #f8fafc;
}

.name-cell {
  display: flex;
  flex-direction: column;
}

.sub-text {
  font-size: 12px;
  color: #64748b;
}

.meta-stacked {
  display: flex;
  flex-direction: column;
}

.expiry-cell {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.days-badge {
  font-size: 11px;
  font-weight: 600;
  padding: 1px 6px;
  border-radius: 4px;
  width: fit-content;
}

.risk-badge-red {
  background: #fff1f2;
  color: #e11d48;
}

.risk-badge-yellow {
  background: #fffbeb;
  color: #d97706;
}

.risk-badge-green {
  background: #ecfdf5;
  color: #059669;
}

.actions-group {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
}

/* FEFO Results */
.fefo-summary-bar {
  display: flex;
  gap: 24px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 10px 16px;
  font-size: 14px;
}

/* Grids */
.grid-2-col {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.grid-3-col {
  display: grid;
  grid-template-columns: 2fr 1fr 1fr;
  gap: 16px;
}

/* Audit Detail */
.audit-top-bar {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 14px 18px;
}

/* Utilities */
.cursor-pointer { cursor: pointer; }
.font-bold { font-weight: 700; }
.font-medium { font-weight: 500; }
.text-dark { color: #0f172a; }
.text-secondary { color: #64748b; }
.text-muted { color: #94a3b8; }
.text-emerald { color: #059669; }
.text-rose { color: #e11d48; }
.text-amber { color: #d97706; }
.mt-1 { margin-top: 4px; }
.mt-2 { margin-top: 8px; }
.mt-3 { margin-top: 12px; }
.mb-3 { margin-bottom: 12px; }
</style>
