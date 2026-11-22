<template>
  <div class="procurement-page-container">
    <!-- PAGE HEADER -->
    <div class="procurement-header-row">
      <div class="header-left">
        <h1 class="page-title">
          <span class="title-icon">📦</span>
          Mua hàng & Quản lý Nhà cung cấp (Procurement & SRM)
        </h1>
        <p class="page-subtitle">
          Quản trị Đơn đặt mua PO, Nhập kho kiểm nhận GRN & Tính Giá vốn Bình quân Gia quyền theo thời gian thực
        </p>
      </div>

      <div class="header-right-actions">
        <el-button 
          type="primary" 
          class="btn-action-primary"
          @click="openCreatePoModal"
        >
          <el-icon><Plus /></el-icon>
          Tạo Đơn Mua Hàng PO
        </el-button>

        <el-button 
          class="btn-action-neutral"
          @click="openSupplierModal(null)"
        >
          <el-icon><OfficeBuilding /></el-icon>
          Thêm Nhà Cung Cấp
        </el-button>

        <el-button 
          class="btn-refresh"
          title="Tải lại dữ liệu"
          @click="refreshAllData"
        >
          <el-icon><Refresh /></el-icon>
        </el-button>
      </div>
    </div>

    <!-- KPI SUMMARY CARDS -->
    <div class="kpi-grid">
      <div class="kpi-card">
        <div class="kpi-icon-box bg-blue">
          <el-icon><OfficeBuilding /></el-icon>
        </div>
        <div class="kpi-info">
          <div class="kpi-label">Nhà cung cấp / Xưởng</div>
          <div class="kpi-value text-blue">{{ overview?.totalSuppliers ?? 0 }}</div>
          <div class="kpi-subtext">Đang hoạt động: <strong>{{ overview?.activeSuppliers ?? 0 }}</strong></div>
        </div>
      </div>

      <div class="kpi-card">
        <div class="kpi-icon-box bg-amber">
          <el-icon><Tickets /></el-icon>
        </div>
        <div class="kpi-info">
          <div class="kpi-label">Đơn PO Chờ Duyệt</div>
          <div class="kpi-value text-amber">{{ overview?.pendingPurchaseOrders ?? 0 }}</div>
          <div class="kpi-subtext">Đã duyệt sẵn sàng nhập: <strong>{{ overview?.approvedPurchaseOrders ?? 0 }}</strong></div>
        </div>
      </div>

      <div class="kpi-card">
        <div class="kpi-icon-box bg-rose">
          <el-icon><Money /></el-icon>
        </div>
        <div class="kpi-info">
          <div class="kpi-label">Công nợ Phải trả NCC (AP)</div>
          <div class="kpi-value text-rose">{{ formatCurrency(overview?.totalAccountsPayable) }}</div>
          <div class="kpi-subtext">Nợ theo đơn PO: {{ formatCurrency(overview?.totalRemainingPoDebt) }}</div>
        </div>
      </div>

      <div class="kpi-card">
        <div class="kpi-icon-box bg-emerald">
          <el-icon><TrendCharts /></el-icon>
        </div>
        <div class="kpi-info">
          <div class="kpi-label">Mua Hàng Trong Tháng</div>
          <div class="kpi-value text-emerald">{{ formatCurrency(overview?.totalPurchasedThisMonth) }}</div>
          <div class="kpi-subtext">Đã hạch toán vào nguồn vốn</div>
        </div>
      </div>
    </div>

    <!-- MAIN TABS CONTAINER -->
    <el-tabs v-model="activeTab" class="procurement-main-tabs" @tab-click="handleTabClick">
      <!-- TAB 1: ĐƠN ĐẶT HÀNG MUA PO -->
      <el-tab-pane label="📋 Đơn Đặt Mua (PO)" name="orders">
        <div class="tab-content-wrapper">
          <!-- Toolbar & Bộ lọc PO -->
          <div class="table-toolbar">
            <div class="toolbar-left">
              <el-input 
                v-model="poFilter.keyword" 
                placeholder="Tìm mã PO, tên nhà cung cấp..." 
                clearable 
                class="search-input"
                @keyup.enter="loadPurchaseOrders"
                @clear="loadPurchaseOrders"
              >
                <template #prefix><el-icon><Search /></el-icon></template>
              </el-input>

              <el-select 
                v-model="poFilter.status" 
                placeholder="Trạng thái" 
                clearable 
                class="filter-select"
                @change="loadPurchaseOrders"
              >
                <el-option label="Tất cả trạng thái" value="" />
                <el-option label="Dự thảo (DRAFT)" value="DRAFT" />
                <el-option label="Chờ duyệt (PENDING_APPROVAL)" value="PENDING_APPROVAL" />
                <el-option label="Đã duyệt (APPROVED)" value="APPROVED" />
                <el-option label="Nhập một phần (PARTIALLY_RECEIVED)" value="PARTIALLY_RECEIVED" />
                <el-option label="Hoàn tất (COMPLETED)" value="COMPLETED" />
                <el-option label="Đã hủy (CANCELLED)" value="CANCELLED" />
              </el-select>
            </div>

            <div class="toolbar-right">
              <el-button type="primary" @click="openCreatePoModal">
                <el-icon><Plus /></el-icon> Soạn Đơn PO
              </el-button>
            </div>
          </div>

          <!-- Bảng danh sách PO -->
          <el-table 
            v-loading="loadingPo" 
            :data="purchaseOrders" 
            stripe 
            class="custom-data-table"
            empty-text="Không có đơn đặt mua hàng nào"
          >
            <el-table-column label="Mã PO" prop="poCode" min-width="150">
              <template #default="{ row }">
                <span class="code-link" @click="viewPoDetail(row)">{{ row.poCode }}</span>
              </template>
            </el-table-column>

            <el-table-column label="Nhà Cung Cấp" min-width="200">
              <template #default="{ row }">
                <div class="supplier-cell">
                  <span class="supplier-name font-medium">{{ row.supplierName }}</span>
                  <span v-if="row.supplierPhone" class="supplier-phone text-subtle">{{ row.supplierPhone }}</span>
                </div>
              </template>
            </el-table-column>

            <el-table-column label="Ngày Đặt" prop="orderDate" width="120" />

            <el-table-column label="Dự Kiến Giao" prop="expectedDeliveryDate" width="130">
              <template #default="{ row }">
                <span>{{ row.expectedDeliveryDate || '—' }}</span>
              </template>
            </el-table-column>

            <el-table-column label="Tổng Thanh Toán" width="160" align="right">
              <template #default="{ row }">
                <span class="font-bold text-primary">{{ formatCurrency(row.finalAmount) }}</span>
              </template>
            </el-table-column>

            <el-table-column label="Còn Nợ" width="150" align="right">
              <template #default="{ row }">
                <span v-if="Number(row.remainingAmount) > 0" class="text-rose font-medium">
                  {{ formatCurrency(row.remainingAmount) }}
                </span>
                <span v-else class="text-emerald font-medium">Đã thanh toán đủ</span>
              </template>
            </el-table-column>

            <el-table-column label="Trạng Thái" width="160" align="center">
              <template #default="{ row }">
                <el-tag :type="getPoStatusTagType(row.status)" effect="light">
                  {{ row.statusLabel || row.status }}
                </el-tag>
              </template>
            </el-table-column>

            <el-table-column label="Thao Tác" width="220" fixed="right" align="center">
              <template #default="{ row }">
                <div class="action-btn-group">
                  <el-button 
                    size="small" 
                    link 
                    type="primary" 
                    @click="viewPoDetail(row)"
                  >
                    Chi tiết
                  </el-button>

                  <el-button 
                    v-if="row.status === 'DRAFT'" 
                    size="small" 
                    link 
                    type="warning" 
                    @click="submitPo(row)"
                  >
                    Gửi duyệt
                  </el-button>

                  <el-button 
                    v-if="row.status === 'PENDING_APPROVAL' && canApprovePo" 
                    size="small" 
                    link 
                    type="success" 
                    @click="approvePo(row)"
                  >
                    Duyệt
                  </el-button>

                  <el-button 
                    v-if="row.status === 'APPROVED' || row.status === 'PARTIALLY_RECEIVED'" 
                    size="small" 
                    type="success" 
                    plain
                    class="btn-compact"
                    @click="openGoodsReceiptForPo(row)"
                  >
                    📦 Nhập kho
                  </el-button>

                  <el-button 
                    v-if="row.status === 'DRAFT' || row.status === 'PENDING_APPROVAL'" 
                    size="small" 
                    link 
                    type="danger" 
                    @click="cancelPo(row)"
                  >
                    Hủy
                  </el-button>
                </div>
              </template>
            </el-table-column>
          </el-table>

          <!-- Phân trang PO -->
          <div class="pagination-row">
            <el-pagination
              v-model:current-page="poPagination.page"
              v-model:page-size="poPagination.size"
              :total="poPagination.total"
              :page-sizes="[10, 15, 25, 50]"
              layout="total, sizes, prev, pager, next, jumper"
              @size-change="loadPurchaseOrders"
              @current-change="loadPurchaseOrders"
            />
          </div>
        </div>
      </el-tab-pane>

      <!-- TAB 2: NHẬP KHO KIỂM NHẬN (GRN) & GIÁ VỐN -->
      <el-tab-pane label="📦 Nhập Kho & Giá Vốn (GRN)" name="receipts">
        <div class="tab-content-wrapper">
          <!-- Banner Giải thích Thuật toán Giá vốn -->
          <div class="algorithm-banner">
            <div class="banner-icon">💡</div>
            <div class="banner-body">
              <div class="banner-title">Thuật toán Giá vốn Bình quân Gia quyền (Moving Weighted Average Cost)</div>
              <div class="banner-formula">
                $$\text{Giá vốn mới} = \frac{(\text{Tồn kho cũ} \times \text{Giá vốn cũ}) + (\text{Số lượng nhập mới} \times \text{Đơn giá nhập mới})}{\text{Tồn kho cũ} + \text{Số lượng nhập mới}}$$
              </div>
              <div class="banner-desc">
                Hệ thống tự động cập nhật lại trường <code>moving_avg_cost</code> và <code>physical_stock</code> trên bảng sản phẩm ngay khi thủ kho bấm xác nhận kiểm nhận.
              </div>
            </div>
            <div class="banner-action">
              <el-button type="success" @click="openQuickReceiptModal">
                <el-icon><Plus /></el-icon> Lập Phiếu Nhập Kho
              </el-button>
            </div>
          </div>

          <!-- Bảng danh sách phiếu nhập kho GRN -->
          <el-table 
            v-loading="loadingGrn" 
            :data="receiptNotes" 
            stripe 
            class="custom-data-table"
            empty-text="Chưa có phiếu nhập kho nào"
          >
            <el-table-column label="Mã Phiếu GRN" prop="grnCode" min-width="160">
              <template #default="{ row }">
                <span class="code-link" @click="viewGrnDetail(row)">{{ row.grnCode }}</span>
              </template>
            </el-table-column>

            <el-table-column label="Đơn PO Gốc" min-width="150">
              <template #default="{ row }">
                <el-tag type="info">{{ row.poCode }}</el-tag>
              </template>
            </el-table-column>

            <el-table-column label="Nhà Cung Cấp" prop="supplierName" min-width="200" />

            <el-table-column label="Kho Tiếp Nhận" prop="warehouseLocation" min-width="160" />

            <el-table-column label="Thời Điểm Nhập" min-width="160">
              <template #default="{ row }">
                <span>{{ formatDateTime(row.receiptDate) }}</span>
              </template>
            </el-table-column>

            <el-table-column label="Tổng Giá Trị Nhập" width="180" align="right">
              <template #default="{ row }">
                <span class="font-bold text-emerald">{{ formatCurrency(row.totalReceiptAmount) }}</span>
              </template>
            </el-table-column>

            <el-table-column label="Người Nhận" prop="receivedByUsername" width="130" />

            <el-table-column label="Thao Tác" width="120" fixed="right" align="center">
              <template #default="{ row }">
                <el-button size="small" link type="primary" @click="viewGrnDetail(row)">
                  Xem chi tiết
                </el-button>
              </template>
            </el-table-column>
          </el-table>

          <!-- Phân trang GRN -->
          <div class="pagination-row">
            <el-pagination
              v-model:current-page="grnPagination.page"
              v-model:page-size="grnPagination.size"
              :total="grnPagination.total"
              :page-sizes="[10, 15, 25, 50]"
              layout="total, sizes, prev, pager, next, jumper"
              @size-change="loadReceiptNotes"
              @current-change="loadReceiptNotes"
            />
          </div>
        </div>
      </el-tab-pane>

      <!-- TAB 3: DANH BẠ NHÀ CUNG CẤP (SUPPLIERS SRM) -->
      <el-tab-pane label="🏢 Nhà Cung Cấp (SRM)" name="suppliers">
        <div class="tab-content-wrapper">
          <div class="table-toolbar">
            <div class="toolbar-left">
              <el-input 
                v-model="supFilter.keyword" 
                placeholder="Tìm mã, tên NCC, số điện thoại..." 
                clearable 
                class="search-input"
                @keyup.enter="loadSuppliers"
                @clear="loadSuppliers"
              >
                <template #prefix><el-icon><Search /></el-icon></template>
              </el-input>

              <el-select 
                v-model="supFilter.status" 
                placeholder="Trạng thái" 
                clearable 
                class="filter-select"
                @change="loadSuppliers"
              >
                <el-option label="Tất cả trạng thái" value="" />
                <el-option label="Đang hoạt động" value="ACTIVE" />
                <el-option label="Tạm ngưng" value="INACTIVE" />
              </el-select>
            </div>

            <div class="toolbar-right">
              <el-button type="primary" @click="openSupplierModal(null)">
                <el-icon><Plus /></el-icon> Thêm Nhà Cung Cấp
              </el-button>
            </div>
          </div>

          <el-table 
            v-loading="loadingSup" 
            :data="suppliers" 
            stripe 
            class="custom-data-table"
            empty-text="Không có nhà cung cấp nào"
          >
            <el-table-column label="Mã NCC" prop="code" width="130">
              <template #default="{ row }">
                <span class="font-mono font-medium">{{ row.code }}</span>
              </template>
            </el-table-column>

            <el-table-column label="Tên Nhà Cung Cấp / Xưởng" min-width="220">
              <template #default="{ row }">
                <div class="sup-info-cell">
                  <div class="sup-name font-bold">{{ row.name }}</div>
                  <div class="sup-address text-subtle text-xs">{{ row.address || 'Chưa cập nhật địa chỉ' }}</div>
                </div>
              </template>
            </el-table-column>

            <el-table-column label="Người Liên Hệ" prop="contactPerson" min-width="150" />

            <el-table-column label="Điện Thoại" prop="phone" width="130" />

            <el-table-column label="Hạn Mức Nợ" width="150" align="right">
              <template #default="{ row }">
                <span>{{ formatCurrency(row.creditLimit) }}</span>
              </template>
            </el-table-column>

            <el-table-column label="Dư Nợ Hiện Tại (AP)" width="170" align="right">
              <template #default="{ row }">
                <span 
                  class="font-bold"
                  :class="Number(row.currentDebt) > 0 ? 'text-rose' : 'text-subtle'"
                >
                  {{ formatCurrency(row.currentDebt) }}
                </span>
              </template>
            </el-table-column>

            <el-table-column label="Hạn TT" width="100" align="center">
              <template #default="{ row }">
                <span>{{ row.paymentTermsDays }} ngày</span>
              </template>
            </el-table-column>

            <el-table-column label="Trạng Thái" width="120" align="center">
              <template #default="{ row }">
                <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'danger'" effect="light">
                  {{ row.status === 'ACTIVE' ? 'Hoạt động' : 'Tạm ngưng' }}
                </el-tag>
              </template>
            </el-table-column>

            <el-table-column label="Thao Tác" width="180" fixed="right" align="center">
              <template #default="{ row }">
                <el-button size="small" link type="primary" @click="openSupplierModal(row)">
                  Sửa
                </el-button>
                <el-button 
                  v-if="Number(row.currentDebt) > 0" 
                  size="small" 
                  link 
                  type="success" 
                  @click="openPayDebtModal(row)"
                >
                  Trả nợ
                </el-button>
                <el-button 
                  v-if="Number(row.currentDebt) <= 0" 
                  size="small" 
                  link 
                  type="danger" 
                  @click="deleteSupplier(row)"
                >
                  Xóa
                </el-button>
              </template>
            </el-table-column>
          </el-table>

          <div class="pagination-row">
            <el-pagination
              v-model:current-page="supPagination.page"
              v-model:page-size="supPagination.size"
              :total="supPagination.total"
              :page-sizes="[10, 15, 25, 50]"
              layout="total, sizes, prev, pager, next, jumper"
              @size-change="loadSuppliers"
              @current-change="loadSuppliers"
            />
          </div>
        </div>
      </el-tab-pane>

      <!-- TAB 4: CÔNG NỢ PHẢI TRẢ & THANH TOÁN (AP) -->
      <el-tab-pane label="💳 Công Nợ Phải Trả & Thanh Toán" name="debt">
        <div class="tab-content-wrapper">
          <div class="debt-header-alert">
            <div class="alert-icon">💼</div>
            <div class="alert-info">
              <div class="alert-title">Quản lý Công nợ Phải trả Nhà Cung Cấp (Accounts Payable - AP)</div>
              <div class="alert-desc">
                Khi thanh toán nợ cho nhà cung cấp, hệ thống sẽ tự động lập <strong>Phiếu Chi Xuất Quỹ (Payment Voucher)</strong> từ tài khoản tiền mặt hoặc ngân hàng được chỉ định, giảm số dư quỹ và cập nhật giảm công nợ đồng thời.
              </div>
            </div>
          </div>

          <div class="debt-table-wrapper">
            <h3 class="section-title">Danh sách Nhà Cung Cấp Đang Có Dư Nợ</h3>
            <el-table :data="debtorSuppliers" stripe class="custom-data-table">
              <el-table-column label="Mã NCC" prop="code" width="130" />
              <el-table-column label="Tên Nhà Cung Cấp" prop="name" min-width="220" />
              <el-table-column label="Số Điện Thoại" prop="phone" width="140" />
              <el-table-column label="Hạn Mức Tín Dụng" width="180" align="right">
                <template #default="{ row }">
                  <span>{{ formatCurrency(row.creditLimit) }}</span>
                </template>
              </el-table-column>
              <el-table-column label="Công Nợ Hiện Tại (AP)" width="200" align="right">
                <template #default="{ row }">
                  <span class="font-bold text-rose text-base">{{ formatCurrency(row.currentDebt) }}</span>
                </template>
              </el-table-column>
              <el-table-column label="Tỷ Lệ Chạm Hạn Mức" width="180">
                <template #default="{ row }">
                  <el-progress 
                    :percentage="calculateDebtRatio(row.currentDebt, row.creditLimit)" 
                    :status="calculateDebtRatio(row.currentDebt, row.creditLimit) > 80 ? 'exception' : 'warning'"
                  />
                </template>
              </el-table-column>
              <el-table-column label="Hành Động" width="160" align="center" fixed="right">
                <template #default="{ row }">
                  <el-button type="primary" size="small" @click="openPayDebtModal(row)">
                    Thanh Toán Nợ
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- ========================================================================= -->
    <!-- MODAL 1: TẠO ĐƠN ĐẶT MUA HÀNG (PURCHASE ORDER) -->
    <!-- ========================================================================= -->
    <el-dialog 
      v-model="showCreatePoModal" 
      title="Soạn Thảo Đơn Đặt Hàng Mua (Purchase Order - PO)" 
      width="900px"
      destroy-on-close
    >
      <el-form label-position="top" class="custom-modal-form">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="Nhà Cung Cấp / Xưởng May *" required>
              <el-select 
                v-model="poForm.supplierId" 
                placeholder="Chọn nhà cung cấp" 
                filterable 
                style="width: 100%"
              >
                <el-option 
                  v-for="sup in activeSuppliers" 
                  :key="sup.id" 
                  :label="sup.name + ' (' + sup.code + ')'" 
                  :value="sup.id" 
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="Ngày Lập Đơn">
              <el-date-picker 
                v-model="poForm.orderDate" 
                type="date" 
                placeholder="Ngày đặt" 
                style="width: 100%" 
                format="YYYY-MM-DD"
                value-format="YYYY-MM-DD"
              />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="Dự Kiến Giao Hàng">
              <el-date-picker 
                v-model="poForm.expectedDeliveryDate" 
                type="date" 
                placeholder="Ngày dự kiến" 
                style="width: 100%" 
                format="YYYY-MM-DD"
                value-format="YYYY-MM-DD"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <!-- Danh sách mặt hàng đặt mua -->
        <div class="po-items-section">
          <div class="section-header-row">
            <span class="font-bold">Danh sách mặt hàng đặt mua:</span>
            <el-button size="small" type="primary" plain @click="addPoItemRow">
              <el-icon><Plus /></el-icon> Thêm Sản Phẩm
            </el-button>
          </div>

          <table class="po-items-table">
            <thead>
              <tr>
                <th style="width: 35%">Sản phẩm</th>
                <th style="width: 15%">Số lượng đặt</th>
                <th style="width: 25%">Đơn giá nhập (VNĐ)</th>
                <th style="width: 20%">Thành tiền</th>
                <th style="width: 5%"></th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(item, idx) in poForm.items" :key="idx">
                <td>
                  <el-select 
                    v-model="item.productId" 
                    placeholder="Chọn sản phẩm" 
                    filterable 
                    style="width: 100%"
                    @change="onProductSelected(item)"
                  >
                    <el-option 
                      v-for="p in allProducts" 
                      :key="p.id" 
                      :label="p.name + ' (' + p.sku + ')'" 
                      :value="p.id" 
                    />
                  </el-select>
                </td>
                <td>
                  <el-input-number 
                    v-model="item.orderedQuantity" 
                    :min="1" 
                    style="width: 100%" 
                    @change="calculatePoTotals"
                  />
                </td>
                <td>
                  <el-input-number 
                    v-model="item.unitPrice" 
                    :min="0" 
                    :step="1000" 
                    style="width: 100%" 
                    @change="calculatePoTotals"
                  />
                </td>
                <td class="font-bold text-right">
                  {{ formatCurrency((item.orderedQuantity || 0) * (item.unitPrice || 0)) }}
                </td>
                <td class="text-center">
                  <el-button 
                    type="danger" 
                    link 
                    :disabled="poForm.items.length <= 1" 
                    @click="removePoItemRow(idx)"
                  >
                    <el-icon><Delete /></el-icon>
                  </el-button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <el-row :gutter="16" class="mt-4">
          <el-col :span="12">
            <el-form-item label="Ghi chú điều khoản mua hàng">
              <el-input 
                v-model="poForm.notes" 
                type="textarea" 
                :rows="3" 
                placeholder="Ghi chú thời gian giao hàng, kiểm định vải, chất lượng..." 
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <div class="po-summary-box">
              <div class="summary-line">
                <span>Tạm tính tiền hàng:</span>
                <span class="font-bold">{{ formatCurrency(poSubtotal) }}</span>
              </div>
              <div class="summary-line">
                <span>Chiết khấu NCC (-):</span>
                <el-input-number 
                  v-model="poForm.discountAmount" 
                  :min="0" 
                  size="small" 
                  @change="calculatePoTotals" 
                />
              </div>
              <div class="summary-line">
                <span>Thuế VAT (+):</span>
                <el-input-number 
                  v-model="poForm.taxAmount" 
                  :min="0" 
                  size="small" 
                  @change="calculatePoTotals" 
                />
              </div>
              <div class="summary-line total-line">
                <span class="font-bold text-base">Tổng thanh toán:</span>
                <span class="font-bold text-primary text-lg">{{ formatCurrency(poFinalTotal) }}</span>
              </div>
            </div>
          </el-col>
        </el-row>
      </el-form>

      <template #footer>
        <div class="modal-footer-btns">
          <el-button @click="showCreatePoModal = false">Hủy</el-button>
          <el-button type="primary" :loading="submittingPo" @click="submitCreatePo">
            Tạo Đơn Hàng PO
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- ========================================================================= -->
    <!-- MODAL 2: NHẬP KHO KIỂM NHẬN (GRN) & CẬP NHẬT GIÁ VỐN BÌNH QUÂN GIA QUYỀN -->
    <!-- ========================================================================= -->
    <el-dialog 
      v-model="showReceiptModal" 
      title="Lập Phiếu Nhập Kho Kiểm Nhận (GRN) & Cập Nhật Giá Vốn" 
      width="960px"
      destroy-on-close
    >
      <div v-if="selectedPoForReceipt" class="receipt-modal-content">
        <!-- Thông tin đơn PO -->
        <div class="receipt-po-info-card">
          <div class="info-item">
            <span class="info-label">Mã đơn PO:</span>
            <span class="info-val font-mono font-bold">{{ selectedPoForReceipt.poCode }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">Nhà cung cấp:</span>
            <span class="info-val font-bold">{{ selectedPoForReceipt.supplierName }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">Kho nhập:</span>
            <el-input v-model="receiptForm.warehouseLocation" size="small" style="width: 220px" />
          </div>
        </div>

        <!-- Bảng danh sách hàng kiểm nhận -->
        <div class="receipt-items-table-box mt-3">
          <table class="receipt-table">
            <thead>
              <tr>
                <th>Sản phẩm</th>
                <th style="width: 100px; text-align: center">Đã đặt</th>
                <th style="width: 100px; text-align: center">Đã nhận</th>
                <th style="width: 130px; text-align: center">Thực nhận đợt này</th>
                <th style="width: 150px; text-align: right">Đơn giá nhập</th>
                <th style="width: 180px; text-align: center">Mô phỏng Giá vốn mới</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in receiptForm.items" :key="item.purchaseOrderItemId">
                <td>
                  <div class="prod-cell">
                    <span class="font-bold">{{ item.productName }}</span>
                    <span class="text-xs text-subtle">SKU: {{ item.productSku }}</span>
                  </div>
                </td>
                <td class="text-center font-medium">{{ item.orderedQuantity }}</td>
                <td class="text-center">{{ item.alreadyReceived }}</td>
                <td>
                  <el-input-number 
                    v-model="item.quantityReceived" 
                    :min="0" 
                    :max="item.orderedQuantity - item.alreadyReceived"
                    size="small" 
                    style="width: 100%"
                    @change="updateCostSimulation(item)"
                  />
                </td>
                <td class="text-right">
                  <el-input-number 
                    v-model="item.importUnitPrice" 
                    :min="0" 
                    size="small" 
                    style="width: 100%"
                    @change="updateCostSimulation(item)"
                  />
                </td>
                <td class="text-center">
                  <div v-if="item.simulation" class="sim-pill">
                    <span class="old-cost">{{ formatCurrency(item.simulation.oldCostPrice) }}</span>
                    <span class="arrow-icon">➔</span>
                    <span class="new-cost font-bold text-emerald">{{ formatCurrency(item.simulation.newCostPrice) }}</span>
                  </div>
                  <span v-else class="text-xs text-subtle">Chưa tính</span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <!-- Ghi chú kiểm nhận -->
        <div class="mt-3">
          <el-input 
            v-model="receiptForm.notes" 
            placeholder="Ghi chú tình trạng hàng hóa khi mở thùng kiểm nhận..." 
          />
        </div>

        <!-- Khung giải thích công thức thời gian thực -->
        <div class="formula-realtime-card mt-3">
          <div class="formula-title">⚡ Minh bạch Công thức Moving Weighted Average Cost:</div>
          <div class="formula-text">
            Mỗi mặt hàng nhận vào sẽ tăng tồn kho vật lý và tự động hạch toán lại giá vốn chuẩn xác theo tỷ trọng hàng cũ và hàng mới.
          </div>
        </div>
      </div>

      <template #footer>
        <div class="modal-footer-btns">
          <el-button @click="showReceiptModal = false">Đóng</el-button>
          <el-button 
            type="success" 
            :loading="submittingReceipt" 
            @click="submitGoodsReceipt"
          >
            Xác Nhận Nhập Kho & Cập Nhật Giá Vốn
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- ========================================================================= -->
    <!-- MODAL 3: XEM CHI TIẾT ĐƠN PO / CHI TIẾT GRN -->
    <!-- ========================================================================= -->
    <el-dialog 
      v-model="showPoDetailModal" 
      :title="'Chi Tiết Đơn Đặt Mua: ' + (activePoDetail?.poCode || '')" 
      width="800px"
    >
      <div v-if="activePoDetail" class="po-detail-content">
        <div class="detail-header-grid">
          <div><strong>Nhà cung cấp:</strong> {{ activePoDetail.supplierName }}</div>
          <div><strong>Ngày đặt:</strong> {{ activePoDetail.orderDate }}</div>
          <div><strong>Trạng thái:</strong> <el-tag :type="getPoStatusTagType(activePoDetail.status)">{{ activePoDetail.statusLabel }}</el-tag></div>
          <div><strong>Người tạo:</strong> {{ activePoDetail.creatorUsername }}</div>
          <div><strong>Người duyệt:</strong> {{ activePoDetail.approverUsername || 'Chưa duyệt' }}</div>
          <div><strong>Tổng thanh toán:</strong> <span class="font-bold text-primary">{{ formatCurrency(activePoDetail.finalAmount) }}</span></div>
        </div>

        <h4 class="mt-4 mb-2 font-bold">Danh sách mặt hàng đặt:</h4>
        <el-table :data="activePoDetail.items || []" stripe size="small" border>
          <el-table-column label="Mã SKU" prop="productSku" width="130" />
          <el-table-column label="Tên sản phẩm" prop="productName" />
          <el-table-column label="Đơn vị" prop="unitName" width="80" align="center" />
          <el-table-column label="Số lượng đặt" prop="orderedQuantity" width="110" align="center" />
          <el-table-column label="Đã nhập" prop="receivedQuantity" width="100" align="center" />
          <el-table-column label="Đơn giá nhập" width="140" align="right">
            <template #default="{ row }">
              <span>{{ formatCurrency(row.unitPrice) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="Thành tiền" width="140" align="right">
            <template #default="{ row }">
              <span class="font-bold">{{ formatCurrency(row.subtotal) }}</span>
            </template>
          </el-table-column>
        </el-table>
      </div>
      <template #footer>
        <el-button @click="showPoDetailModal = false">Đóng</el-button>
      </template>
    </el-dialog>

    <!-- ========================================================================= -->
    <!-- MODAL 4: THÊM / SỬA NHÀ CUNG CẤP -->
    <!-- ========================================================================= -->
    <el-dialog 
      v-model="showSupplierModal" 
      :title="editingSupId ? 'Cập Nhật Nhà Cung Cấp' : 'Thêm Mới Nhà Cung Cấp / Xưởng May'" 
      width="600px"
      destroy-on-close
    >
      <el-form label-position="top" class="custom-modal-form">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="Mã NCC (bỏ trống tự sinh)">
              <el-input v-model="supForm.code" placeholder="VD: NCC-TANBINH" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="Tên Nhà Cung Cấp / Xưởng *" required>
              <el-input v-model="supForm.name" placeholder="Tên công ty hoặc chủ xưởng..." />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="Người liên hệ">
              <el-input v-model="supForm.contactPerson" placeholder="Chuyên viên phụ trách / Giám đốc xưởng..." />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="Số điện thoại *" required>
              <el-input v-model="supForm.phone" placeholder="09xxxxxxxx..." />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="Email">
              <el-input v-model="supForm.email" placeholder="email@ncc.com" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="Mã số thuế">
              <el-input v-model="supForm.taxCode" placeholder="Mã số thuế doanh nghiệp..." />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="Địa chỉ xưởng / văn phòng">
          <el-input v-model="supForm.address" placeholder="Địa chỉ chi tiết..." />
        </el-form-item>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="Hạn mức nợ tối đa (Credit Limit)">
              <el-input-number v-model="supForm.creditLimit" :min="0" :step="10000000" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="Thời hạn thanh toán (ngày)">
              <el-input-number v-model="supForm.paymentTermsDays" :min="1" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="Ghi chú năng lực cung ứng">
          <el-input v-model="supForm.notes" type="textarea" :rows="2" placeholder="Sản phẩm thế mạnh, công suất may..." />
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="modal-footer-btns">
          <el-button @click="showSupplierModal = false">Hủy</el-button>
          <el-button type="primary" :loading="submittingSup" @click="submitSupplierForm">
            {{ editingSupId ? 'Cập Nhật' : 'Lưu Nhà Cung Cấp' }}
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- ========================================================================= -->
    <!-- MODAL 5: THANH TOÁN CÔNG NỢ NHÀ CUNG CẤP (XUẤT QUỸ SỔ QUỸ TÀI CHÍNH) -->
    <!-- ========================================================================= -->
    <el-dialog 
      v-model="showPayDebtModal" 
      title="Lập Phiếu Chi Thanh Toán Công Nợ Nhà Cung Cấp" 
      width="540px"
      destroy-on-close
    >
      <div v-if="targetSupplierForPay" class="pay-debt-dialog-body">
        <div class="supplier-debt-badge">
          <div>Nhà cung cấp: <strong>{{ targetSupplierForPay.name }}</strong></div>
          <div>Dư nợ hiện tại: <strong class="text-rose">{{ formatCurrency(targetSupplierForPay.currentDebt) }}</strong></div>
        </div>

        <el-form label-position="top" class="custom-modal-form mt-3">
          <el-form-item label="Số tiền thanh toán (VNĐ) *" required>
            <el-input-number 
              v-model="payDebtForm.amount" 
              :min="1000" 
              :max="Number(targetSupplierForPay.currentDebt)" 
              :step="500000" 
              style="width: 100%" 
            />
          </el-form-item>

          <el-form-item label="Tài khoản quỹ trích tiền *" required>
            <el-select v-model="payDebtForm.sourceAccountCode" style="width: 100%">
              <el-option 
                v-for="acc in treasuryAccounts" 
                :key="acc.accountCode" 
                :label="acc.accountName + ' (Số dư: ' + formatCurrency(acc.currentBalance) + ')'" 
                :value="acc.accountCode" 
              />
            </el-select>
          </el-form-item>

          <el-form-item label="Lý do & Ghi chú thanh toán">
            <el-input 
              v-model="payDebtForm.notes" 
              type="textarea" 
              :rows="2" 
              placeholder="VD: Ủy nhiệm chi thanh toán tiền vải đợt 1..." 
            />
          </el-form-item>
        </el-form>
      </div>

      <template #footer>
        <div class="modal-footer-btns">
          <el-button @click="showPayDebtModal = false">Hủy</el-button>
          <el-button type="danger" :loading="submittingPay" @click="submitPaySupplierDebt">
            Xác Nhận Xuất Quỹ Trả Nợ
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
// =============================================================================
// 1. HELPER FUNCTIONS (Hoisting safe: đặt trước toàn bộ computed / watch)
// =============================================================================
const formatCurrency = (val) => {
  if (val === null || val === undefined || isNaN(val)) return '0 ₫'
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(val)
}

const formatDateTime = (val) => {
  if (!val) return '—'
  const d = new Date(val)
  return isNaN(d.getTime()) ? val : d.toLocaleString('vi-VN')
}

const getPoStatusTagType = (status) => {
  switch (status) {
    case 'DRAFT': return 'info'
    case 'PENDING_APPROVAL': return 'warning'
    case 'APPROVED': return 'primary'
    case 'PARTIALLY_RECEIVED': return ''
    case 'COMPLETED': return 'success'
    case 'CANCELLED': return 'danger'
    default: return 'info'
  }
}

const calculateDebtRatio = (debt, limit) => {
  const d = Number(debt) || 0
  const l = Number(limit) || 1
  return Math.min(100, Math.round((d / l) * 100))
}

// =============================================================================
// 2. IMPORTS & COMPONENT SETUP
// =============================================================================
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  Plus, Minus, Refresh, Search, Delete,
  OfficeBuilding, Tickets, Money, TrendCharts 
} from '@element-plus/icons-vue'
import { useAuthStore } from '../store/auth'
import api from '../api'

const authStore = useAuthStore()
const activeTab = ref('orders')
const overview = ref(null)

// Phân quyền
const canApprovePo = computed(() => {
  return authStore.isAdmin || authStore.user?.role === 'MANAGER'
})

// =============================================================================
// 3. TAB 1: PURCHASE ORDERS (PO)
// =============================================================================
const loadingPo = ref(false)
const purchaseOrders = ref([])
const poFilter = reactive({
  keyword: '',
  status: ''
})
const poPagination = reactive({
  page: 1,
  size: 15,
  total: 0
})

const loadPurchaseOrders = async () => {
  loadingPo.value = true
  try {
    const params = {
      page: poPagination.page - 1,
      size: poPagination.size,
      keyword: poFilter.keyword || undefined,
      status: poFilter.status || undefined
    }
    const res = await api.get('/api/procurement/purchase-orders', { params })
    if (res.data) {
      purchaseOrders.value = res.data.content || []
      poPagination.total = res.data.totalElements || 0
    }
  } catch (err) {
    ElMessage.error(err.response?.data?.message || 'Lỗi tải danh sách đơn đặt mua!')
  } finally {
    loadingPo.value = false
  }
}

// =============================================================================
// 4. TAB 2: GOODS RECEIPT NOTES (GRN)
// =============================================================================
const loadingGrn = ref(false)
const receiptNotes = ref([])
const grnPagination = reactive({
  page: 1,
  size: 15,
  total: 0
})

const loadReceiptNotes = async () => {
  loadingGrn.value = true
  try {
    const params = {
      page: grnPagination.page - 1,
      size: grnPagination.size
    }
    const res = await api.get('/api/procurement/goods-receipts', { params })
    if (res.data) {
      receiptNotes.value = res.data.content || []
      grnPagination.total = res.data.totalElements || 0
    }
  } catch (err) {
    ElMessage.error(err.response?.data?.message || 'Lỗi tải danh sách phiếu nhập kho!')
  } finally {
    loadingGrn.value = false
  }
}

// =============================================================================
// 5. TAB 3 & 4: SUPPLIERS SRM & DEBT
// =============================================================================
const loadingSup = ref(false)
const suppliers = ref([])
const activeSuppliers = ref([])
const supFilter = reactive({
  keyword: '',
  status: ''
})
const supPagination = reactive({
  page: 1,
  size: 15,
  total: 0
})

const debtorSuppliers = computed(() => {
  return suppliers.value.filter(s => Number(s.currentDebt) > 0)
})

const loadSuppliers = async () => {
  loadingSup.value = true
  try {
    const params = {
      page: supPagination.page - 1,
      size: supPagination.size,
      keyword: supFilter.keyword || undefined,
      status: supFilter.status || undefined
    }
    const res = await api.get('/api/procurement/suppliers', { params })
    if (res.data) {
      suppliers.value = res.data.content || []
      supPagination.total = res.data.totalElements || 0
    }
  } catch (err) {
    ElMessage.error(err.response?.data?.message || 'Lỗi tải danh sách nhà cung cấp!')
  } finally {
    loadingSup.value = false
  }
}

const loadActiveSuppliers = async () => {
  try {
    const res = await api.get('/api/procurement/suppliers/active')
    activeSuppliers.value = res.data || []
  } catch (e) {
    console.error('Error loadActiveSuppliers:', e)
  }
}

const loadOverview = async () => {
  try {
    const res = await api.get('/api/procurement/overview')
    overview.value = res.data
  } catch (e) {
    console.error('Error loadOverview:', e)
  }
}

// Quỹ tài chính phục vụ trả nợ
const treasuryAccounts = ref([])
const loadTreasuryAccounts = async () => {
  try {
    const res = await api.get('/api/finance/treasury-accounts')
    treasuryAccounts.value = res.data || []
  } catch (e) {
    console.error('Error loadTreasuryAccounts:', e)
  }
}

// Danh mục sản phẩm để chọn trong PO
const allProducts = ref([])
const loadProducts = async () => {
  try {
    const res = await api.get('/api/products', { params: { size: 100 } })
    allProducts.value = res.data?.content || []
  } catch (e) {
    console.error('Error loadProducts:', e)
  }
}

// =============================================================================
// 6. MODAL ACTIONS: TẠO ĐƠN PO
// =============================================================================
const showCreatePoModal = ref(false)
const submittingPo = ref(false)
const poForm = reactive({
  supplierId: null,
  orderDate: new Date().toISOString().split('T')[0],
  expectedDeliveryDate: null,
  discountAmount: 0,
  taxAmount: 0,
  notes: '',
  items: []
})

const poSubtotal = computed(() => {
  return poForm.items.reduce((sum, item) => {
    return sum + ((Number(item.orderedQuantity) || 0) * (Number(item.unitPrice) || 0))
  }, 0)
})

const poFinalTotal = computed(() => {
  const sub = poSubtotal.value
  const disc = Number(poForm.discountAmount) || 0
  const tax = Number(poForm.taxAmount) || 0
  return Math.max(0, sub - disc + tax)
})

const openCreatePoModal = () => {
  poForm.supplierId = activeSuppliers.value[0]?.id || null
  poForm.orderDate = new Date().toISOString().split('T')[0]
  poForm.expectedDeliveryDate = null
  poForm.discountAmount = 0
  poForm.taxAmount = 0
  poForm.notes = ''
  poForm.items = [
    { productId: null, orderedQuantity: 50, unitPrice: 0 }
  ]
  showCreatePoModal.value = true
}

const addPoItemRow = () => {
  poForm.items.push({ productId: null, orderedQuantity: 10, unitPrice: 0 })
}

const removePoItemRow = (index) => {
  if (poForm.items.length > 1) {
    poForm.items.splice(index, 1)
  }
}

const onProductSelected = (item) => {
  const prod = allProducts.value.find(p => p.id === item.productId)
  if (prod) {
    item.unitPrice = Number(prod.movingAvgCost) || Number(prod.price) || 0
  }
}

const calculatePoTotals = () => {
  // Handled via computed
}

const submitCreatePo = async () => {
  if (!poForm.supplierId) {
    ElMessage.warning('Vui lòng chọn nhà cung cấp!')
    return
  }
  const invalidItem = poForm.items.find(i => !i.productId || !i.orderedQuantity || i.orderedQuantity <= 0)
  if (invalidItem) {
    ElMessage.warning('Vui lòng chọn đầy đủ sản phẩm và số lượng đặt lớn hơn 0!')
    return
  }

  submittingPo.value = true
  try {
    const payload = {
      supplierId: poForm.supplierId,
      orderDate: poForm.orderDate,
      expectedDeliveryDate: poForm.expectedDeliveryDate,
      discountAmount: poForm.discountAmount,
      taxAmount: poForm.taxAmount,
      notes: poForm.notes,
      items: poForm.items.map(i => ({
        productId: i.productId,
        orderedQuantity: i.orderedQuantity,
        unitPrice: i.unitPrice
      }))
    }
    await api.post('/api/procurement/purchase-orders', payload)
    ElMessage.success('Tạo đơn đặt mua hàng PO thành công!')
    showCreatePoModal.value = false
    loadPurchaseOrders()
    loadOverview()
  } catch (err) {
    ElMessage.error(err.response?.data?.message || 'Lỗi khi tạo đơn PO!')
  } finally {
    submittingPo.value = false
  }
}

const submitPo = async (po) => {
  try {
    await ElMessageBox.confirm(`Gửi phê duyệt đơn đặt hàng ${po.poCode}?`, 'Xác nhận', {
      confirmButtonText: 'Gửi duyệt',
      cancelButtonText: 'Hủy'
    })
    await api.put(`/api/procurement/purchase-orders/${po.id}/submit`)
    ElMessage.success('Đã gửi phê duyệt đơn PO thành công!')
    loadPurchaseOrders()
    loadOverview()
  } catch (e) {
    // cancelled
  }
}

const approvePo = async (po) => {
  try {
    await ElMessageBox.confirm(`Phê duyệt đơn đặt hàng ${po.poCode}? Đơn sẽ sẵn sàng nhập kho.`, 'Xác nhận duyệt', {
      confirmButtonText: 'Phê duyệt ngay',
      confirmButtonClass: 'el-button--success',
      cancelButtonText: 'Đóng'
    })
    await api.put(`/api/procurement/purchase-orders/${po.id}/approve`)
    ElMessage.success('Đã phê duyệt đơn đặt hàng PO thành công!')
    loadPurchaseOrders()
    loadOverview()
  } catch (e) {
    // cancelled
  }
}

const cancelPo = async (po) => {
  try {
    const { value: reason } = await ElMessageBox.prompt('Nhập lý do hủy đơn đặt hàng PO:', 'Hủy đơn PO', {
      confirmButtonText: 'Xác nhận hủy',
      cancelButtonText: 'Đóng',
      inputPlaceholder: 'Lý do hủy đơn...'
    })
    await api.put(`/api/procurement/purchase-orders/${po.id}/cancel`, { reason })
    ElMessage.success('Đã hủy đơn đặt hàng PO thành công!')
    loadPurchaseOrders()
    loadOverview()
  } catch (e) {
    // cancelled
  }
}

// =============================================================================
// 7. MODAL ACTIONS: NHẬP KHO KIỂM NHẬN (GRN) & GIÁ VỐN
// =============================================================================
const showReceiptModal = ref(false)
const submittingReceipt = ref(false)
const selectedPoForReceipt = ref(null)
const receiptForm = reactive({
  purchaseOrderId: null,
  warehouseLocation: 'Kho Chính Trung Tâm',
  notes: '',
  items: []
})

const openGoodsReceiptForPo = async (po) => {
  try {
    const res = await api.get(`/api/procurement/purchase-orders/${po.id}`)
    selectedPoForReceipt.value = res.data
    receiptForm.purchaseOrderId = po.id
    receiptForm.warehouseLocation = 'Kho Chính Trung Tâm'
    receiptForm.notes = ''
    receiptForm.items = (res.data.items || []).map(item => {
      const remaining = Math.max(0, item.orderedQuantity - (item.receivedQuantity || 0))
      return {
        purchaseOrderItemId: item.id,
        productId: item.productId,
        productSku: item.productSku,
        productName: item.productName,
        orderedQuantity: item.orderedQuantity,
        alreadyReceived: item.receivedQuantity || 0,
        quantityReceived: remaining,
        importUnitPrice: Number(item.unitPrice) || 0,
        simulation: null
      }
    })

    // Kích hoạt mô phỏng giá vốn cho từng dòng
    for (const item of receiptForm.items) {
      updateCostSimulation(item)
    }

    showReceiptModal.value = true
  } catch (err) {
    ElMessage.error(err.response?.data?.message || 'Lỗi tải chi tiết đơn PO!')
  }
}

const openQuickReceiptModal = () => {
  // Lấy đơn PO approved đầu tiên
  const approved = purchaseOrders.value.find(p => p.status === 'APPROVED' || p.status === 'PARTIALLY_RECEIVED')
  if (approved) {
    openGoodsReceiptForPo(approved)
  } else {
    ElMessage.info('Không có đơn PO nào ở trạng thái ĐÃ DUYỆT để nhập kho. Hãy duyệt một đơn trước!')
    activeTab.value = 'orders'
  }
}

const updateCostSimulation = async (item) => {
  if (!item.productId || !item.quantityReceived || item.quantityReceived <= 0) {
    item.simulation = null
    return
  }
  try {
    const res = await api.post('/api/procurement/goods-receipts/simulate-cost', null, {
      params: {
        productId: item.productId,
        quantityReceived: item.quantityReceived,
        importUnitPrice: item.importUnitPrice
      }
    })
    item.simulation = res.data
  } catch (e) {
    console.error('Simulation error:', e)
  }
}

const submitGoodsReceipt = async () => {
  const validItems = receiptForm.items.filter(i => i.quantityReceived && i.quantityReceived > 0)
  if (validItems.length === 0) {
    ElMessage.warning('Vui lòng nhập số lượng kiểm nhận lớn hơn 0 ít nhất một mặt hàng!')
    return
  }

  submittingReceipt.value = true
  try {
    const payload = {
      purchaseOrderId: receiptForm.purchaseOrderId,
      warehouseLocation: receiptForm.warehouseLocation,
      notes: receiptForm.notes,
      items: validItems.map(i => ({
        purchaseOrderItemId: i.purchaseOrderItemId,
        quantityReceived: i.quantityReceived,
        importUnitPrice: i.importUnitPrice
      }))
    }
    const res = await api.post('/api/procurement/goods-receipts', payload)
    ElMessage.success(`Lập phiếu nhập kho ${res.data.grnCode} & cập nhật giá vốn thành công!`)
    showReceiptModal.value = false
    loadPurchaseOrders()
    loadReceiptNotes()
    loadSuppliers()
    loadOverview()
  } catch (err) {
    ElMessage.error(err.response?.data?.message || 'Lỗi khi lập phiếu nhập kho!')
  } finally {
    submittingReceipt.value = false
  }
}

// =============================================================================
// 8. CHI TIẾT ĐƠN PO / GRN
// =============================================================================
const showPoDetailModal = ref(false)
const activePoDetail = ref(null)

const viewPoDetail = async (po) => {
  try {
    const res = await api.get(`/api/procurement/purchase-orders/${po.id}`)
    activePoDetail.value = res.data
    showPoDetailModal.value = true
  } catch (err) {
    ElMessage.error(err.response?.data?.message || 'Lỗi tải chi tiết đơn PO!')
  }
}

const viewGrnDetail = async (grn) => {
  try {
    const res = await api.get(`/api/procurement/goods-receipts/${grn.id}`)
    ElMessageBox.alert(
      `<div style="text-align: left; font-size: 13px;">
        <p><strong>Mã Phiếu:</strong> ${res.data.grnCode}</p>
        <p><strong>Đơn PO:</strong> ${res.data.poCode}</p>
        <p><strong>Nhà cung cấp:</strong> ${res.data.supplierName}</p>
        <p><strong>Kho tiếp nhận:</strong> ${res.data.warehouseLocation}</p>
        <p><strong>Tổng tiền nhập:</strong> <strong style="color: #059669">${formatCurrency(res.data.totalReceiptAmount)}</strong></p>
        <p><strong>Người nhận:</strong> ${res.data.receivedByUsername}</p>
      </div>`,
      'Thông Tin Phiếu Nhập Kho GRN',
      { dangerouslyUseHTMLString: true, confirmButtonText: 'Đóng' }
    )
  } catch (err) {
    ElMessage.error('Lỗi tải chi tiết phiếu nhập kho!')
  }
}

// =============================================================================
// 9. THÊM / SỬA NHÀ CUNG CẤP
// =============================================================================
const showSupplierModal = ref(false)
const submittingSup = ref(false)
const editingSupId = ref(null)
const supForm = reactive({
  code: '',
  name: '',
  contactPerson: '',
  phone: '',
  email: '',
  taxCode: '',
  address: '',
  creditLimit: 200000000,
  paymentTermsDays: 30,
  notes: ''
})

const openSupplierModal = (sup) => {
  if (sup) {
    editingSupId.value = sup.id
    supForm.code = sup.code
    supForm.name = sup.name
    supForm.contactPerson = sup.contactPerson || ''
    supForm.phone = sup.phone
    supForm.email = sup.email || ''
    supForm.taxCode = sup.taxCode || ''
    supForm.address = sup.address || ''
    supForm.creditLimit = Number(sup.creditLimit) || 200000000
    supForm.paymentTermsDays = sup.paymentTermsDays || 30
    supForm.notes = sup.notes || ''
  } else {
    editingSupId.value = null
    supForm.code = ''
    supForm.name = ''
    supForm.contactPerson = ''
    supForm.phone = ''
    supForm.email = ''
    supForm.taxCode = ''
    supForm.address = ''
    supForm.creditLimit = 200000000
    supForm.paymentTermsDays = 30
    supForm.notes = ''
  }
  showSupplierModal.value = true
}

const submitSupplierForm = async () => {
  if (!supForm.name || !supForm.phone) {
    ElMessage.warning('Vui lòng nhập tên và số điện thoại nhà cung cấp!')
    return
  }

  submittingSup.value = true
  try {
    if (editingSupId.value) {
      await api.put(`/api/procurement/suppliers/${editingSupId.value}`, supForm)
      ElMessage.success('Cập nhật nhà cung cấp thành công!')
    } else {
      await api.post('/api/procurement/suppliers', supForm)
      ElMessage.success('Thêm mới nhà cung cấp thành công!')
    }
    showSupplierModal.value = false
    loadSuppliers()
    loadActiveSuppliers()
    loadOverview()
  } catch (err) {
    ElMessage.error(err.response?.data?.message || 'Lỗi lưu nhà cung cấp!')
  } finally {
    submittingSup.value = false
  }
}

const deleteSupplier = async (sup) => {
  try {
    await ElMessageBox.confirm(`Xác nhận xóa nhà cung cấp ${sup.name}?`, 'Cảnh báo', {
      type: 'warning',
      confirmButtonText: 'Xóa ngay',
      cancelButtonText: 'Hủy'
    })
    await api.delete(`/api/procurement/suppliers/${sup.id}`)
    ElMessage.success('Đã xóa nhà cung cấp thành công!')
    loadSuppliers()
    loadActiveSuppliers()
    loadOverview()
  } catch (e) {
    // cancelled
  }
}

// =============================================================================
// 10. THANH TOÁN CÔNG NỢ NCC
// =============================================================================
const showPayDebtModal = ref(false)
const submittingPay = ref(false)
const targetSupplierForPay = ref(null)
const payDebtForm = reactive({
  supplierId: null,
  amount: 0,
  sourceAccountCode: '',
  notes: ''
})

const openPayDebtModal = (sup) => {
  targetSupplierForPay.value = sup
  payDebtForm.supplierId = sup.id
  payDebtForm.amount = Number(sup.currentDebt) || 0
  payDebtForm.sourceAccountCode = treasuryAccounts.value[0]?.accountCode || 'BANK_VCB'
  payDebtForm.notes = `Ủy nhiệm chi trả tiền hàng cho ${sup.name}`
  showPayDebtModal.value = true
}

const submitPaySupplierDebt = async () => {
  if (!payDebtForm.amount || payDebtForm.amount <= 0) {
    ElMessage.warning('Vui lòng nhập số tiền thanh toán lớn hơn 0!')
    return
  }
  if (!payDebtForm.sourceAccountCode) {
    ElMessage.warning('Vui lòng chọn tài khoản quỹ trích tiền!')
    return
  }

  submittingPay.value = true
  try {
    const res = await api.post('/api/procurement/payments/pay-debt', payDebtForm)
    ElMessage.success(`Thanh toán thành công! Đã tạo Phiếu Chi ${res.data.voucherCode} và trừ tiền quỹ.`)
    showPayDebtModal.value = false
    loadSuppliers()
    loadTreasuryAccounts()
    loadPurchaseOrders()
    loadOverview()
  } catch (err) {
    ElMessage.error(err.response?.data?.message || 'Lỗi thanh toán công nợ!')
  } finally {
    submittingPay.value = false
  }
}

const handleTabClick = (tab) => {
  if (tab.paneName === 'orders') loadPurchaseOrders()
  else if (tab.paneName === 'receipts') loadReceiptNotes()
  else if (tab.paneName === 'suppliers') loadSuppliers()
  else if (tab.paneName === 'debt') loadSuppliers()
}

const refreshAllData = () => {
  loadOverview()
  loadPurchaseOrders()
  loadReceiptNotes()
  loadSuppliers()
  loadActiveSuppliers()
  loadTreasuryAccounts()
  loadProducts()
  ElMessage.success('Đã làm mới dữ liệu Mua hàng & SRM!')
}

// =============================================================================
// 11. LIFECYCLE
// =============================================================================
onMounted(() => {
  loadOverview()
  loadPurchaseOrders()
  loadReceiptNotes()
  loadSuppliers()
  loadActiveSuppliers()
  loadTreasuryAccounts()
  loadProducts()
})
</script>

<style scoped>
.procurement-page-container {
  padding: 20px 24px;
  background-color: var(--erp-bg-page, #f8fafc);
  min-height: calc(100vh - 64px);
  padding-bottom: 80px;
}

.procurement-header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 16px;
  margin-bottom: 24px;
}

.page-title {
  font-size: 24px;
  font-weight: 700;
  color: #0f172a;
  margin: 0 0 6px 0;
  display: flex;
  align-items: center;
  gap: 10px;
}

.theme-dark .page-title {
  color: #f1f5f9;
}

.page-subtitle {
  font-size: 14px;
  color: #64748b;
  margin: 0;
}

.header-right-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

/* KPI Grid */
.kpi-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(230px, 1fr));
  gap: 16px;
  margin-bottom: 24px;
}

.kpi-card {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 16px 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

.theme-dark .kpi-card {
  background: #1e293b;
  border-color: #334155;
}

.kpi-icon-box {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  flex-shrink: 0;
}

.kpi-icon-box.bg-blue { background: #e0f2fe; color: #0284c7; }
.kpi-icon-box.bg-amber { background: #fef3c7; color: #d97706; }
.kpi-icon-box.bg-rose { background: #ffe4e6; color: #e11d48; }
.kpi-icon-box.bg-emerald { background: #d1fae5; color: #059669; }

.kpi-info {
  flex: 1;
}

.kpi-label {
  font-size: 13px;
  color: #64748b;
  margin-bottom: 4px;
}

.kpi-value {
  font-size: 22px;
  font-weight: 700;
  line-height: 1.2;
}

.kpi-subtext {
  font-size: 11px;
  color: #94a3b8;
  margin-top: 4px;
}

.text-blue { color: #0284c7; }
.text-amber { color: #d97706; }
.text-rose { color: #e11d48; }
.text-emerald { color: #059669; }

/* Tabs & Content */
.procurement-main-tabs {
  background: #ffffff;
  border-radius: 12px;
  border: 1px solid #e2e8f0;
  padding: 16px 20px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

.theme-dark .procurement-main-tabs {
  background: #1e293b;
  border-color: #334155;
}

.tab-content-wrapper {
  padding-top: 8px;
}

.table-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 16px;
}

.toolbar-left {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.search-input {
  width: 280px;
}

.filter-select {
  width: 220px;
}

.custom-data-table {
  border-radius: 8px;
  overflow: hidden;
}

.code-link {
  color: #2563eb;
  font-family: monospace;
  font-weight: 600;
  cursor: pointer;
}

.code-link:hover {
  text-decoration: underline;
}

.supplier-cell {
  display: flex;
  flex-direction: column;
}

.supplier-name {
  color: #0f172a;
}
.theme-dark .supplier-name {
  color: #f1f5f9;
}

.action-btn-group {
  display: flex;
  align-items: center;
  gap: 6px;
  justify-content: center;
}

.btn-compact {
  padding: 4px 10px;
  font-size: 12px;
}

.pagination-row {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

/* Algorithm Banner */
.algorithm-banner {
  background: linear-gradient(135deg, #f0fdf4 0%, #ecfdf5 100%);
  border: 1px solid #a7f3d0;
  border-radius: 12px;
  padding: 16px 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 20px;
}

.theme-dark .algorithm-banner {
  background: #064e3b;
  border-color: #047857;
}

.banner-icon {
  font-size: 32px;
}

.banner-body {
  flex: 1;
}

.banner-title {
  font-weight: 700;
  color: #065f46;
  font-size: 15px;
  margin-bottom: 4px;
}

.theme-dark .banner-title {
  color: #6ee7b7;
}

.banner-formula {
  font-family: monospace;
  background: rgba(255, 255, 255, 0.7);
  padding: 6px 12px;
  border-radius: 6px;
  display: inline-block;
  margin: 4px 0;
  font-size: 13px;
  color: #047857;
}

.theme-dark .banner-formula {
  background: rgba(0, 0, 0, 0.3);
  color: #a7f3d0;
}

.banner-desc {
  font-size: 12px;
  color: #047857;
}

.theme-dark .banner-desc {
  color: #d1fae5;
}

/* Debt Header Alert */
.debt-header-alert {
  background: #f8fafc;
  border: 1px solid #cbd5e1;
  border-radius: 10px;
  padding: 16px;
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 20px;
}

.theme-dark .debt-header-alert {
  background: #1e293b;
  border-color: #475569;
}

.alert-icon {
  font-size: 28px;
}

.alert-title {
  font-weight: 700;
  color: #1e293b;
  margin-bottom: 4px;
}

.theme-dark .alert-title {
  color: #f1f5f9;
}

.alert-desc {
  font-size: 13px;
  color: #64748b;
  line-height: 1.5;
}

.section-title {
  font-size: 16px;
  font-weight: 700;
  color: #0f172a;
  margin: 16px 0 12px 0;
}

.theme-dark .section-title {
  color: #f1f5f9;
}

/* PO Items Table inside Dialog */
.po-items-section {
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 12px;
  margin-top: 12px;
  background: #f8fafc;
}

.theme-dark .po-items-section {
  background: #0f172a;
  border-color: #334155;
}

.section-header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.po-items-table {
  width: 100%;
  border-collapse: collapse;
}

.po-items-table th {
  text-align: left;
  font-size: 12px;
  color: #64748b;
  padding: 6px 8px;
  border-bottom: 1px solid #cbd5e1;
}

.po-items-table td {
  padding: 8px;
  border-bottom: 1px solid #e2e8f0;
}

.po-summary-box {
  background: #f1f5f9;
  border-radius: 8px;
  padding: 12px 16px;
}

.theme-dark .po-summary-box {
  background: #1e293b;
}

.summary-line {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
  font-size: 13px;
}

.total-line {
  border-top: 1px dashed #cbd5e1;
  padding-top: 8px;
  margin-top: 8px;
}

/* Receipt Modal Styles */
.receipt-po-info-card {
  display: flex;
  gap: 24px;
  background: #f8fafc;
  padding: 12px 16px;
  border-radius: 8px;
  border: 1px solid #e2e8f0;
  align-items: center;
}

.theme-dark .receipt-po-info-card {
  background: #1e293b;
  border-color: #334155;
}

.info-item {
  display: flex;
  gap: 8px;
  align-items: center;
  font-size: 13px;
}

.info-label {
  color: #64748b;
}

.receipt-table {
  width: 100%;
  border-collapse: collapse;
}

.receipt-table th {
  font-size: 12px;
  color: #64748b;
  padding: 8px;
  border-bottom: 1px solid #e2e8f0;
  background: #f8fafc;
}

.theme-dark .receipt-table th {
  background: #0f172a;
}

.receipt-table td {
  padding: 10px 8px;
  border-bottom: 1px solid #e2e8f0;
}

.prod-cell {
  display: flex;
  flex-direction: column;
}

.sim-pill {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background: #f0fdf4;
  border: 1px solid #bbf7d0;
  padding: 4px 8px;
  border-radius: 6px;
  font-size: 12px;
}

.theme-dark .sim-pill {
  background: #064e3b;
  border-color: #047857;
}

.old-cost {
  color: #64748b;
  text-decoration: line-through;
}

.arrow-icon {
  color: #10b981;
}

.formula-realtime-card {
  background: #eff6ff;
  border: 1px dashed #93c5fd;
  border-radius: 8px;
  padding: 10px 14px;
}

.theme-dark .formula-realtime-card {
  background: #1e3a8a;
  border-color: #3b82f6;
}

.formula-title {
  font-weight: 700;
  color: #1d4ed8;
  font-size: 13px;
  margin-bottom: 2px;
}

.theme-dark .formula-title {
  color: #93c5fd;
}

.formula-text {
  font-size: 12px;
  color: #1e40af;
}

.theme-dark .formula-text {
  color: #dbeafe;
}

/* Pay Debt Body */
.supplier-debt-badge {
  background: #fef2f2;
  border: 1px solid #fecaca;
  border-radius: 8px;
  padding: 12px 16px;
  font-size: 14px;
  display: flex;
  justify-content: space-between;
}

.theme-dark .supplier-debt-badge {
  background: #450a0a;
  border-color: #7f1d1d;
}

.detail-header-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
  background: #f8fafc;
  padding: 14px;
  border-radius: 8px;
  font-size: 13px;
}

.theme-dark .detail-header-grid {
  background: #1e293b;
}

.modal-footer-btns {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>
