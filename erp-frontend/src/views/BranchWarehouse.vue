<template>
  <div class="branch-warehouse-container">
    <!-- PAGE HEADER -->
    <div class="page-header">
      <div class="header-left">
        <div class="title-row">
          <h1 class="page-title">Quản trị Đa Chi Nhánh & Đa Kho Hàng</h1>
          <span class="badge-enterprise">ENTERPRISE V3.0</span>
        </div>
        <p class="page-subtitle">
          Mô hình chuỗi đa điểm, đồng bộ tồn kho khả dụng thời gian thực & điều chuyển kho nội bộ (ITN)
        </p>
      </div>
      <div class="header-actions">
        <el-button 
          v-if="canManage"
          type="primary" 
          class="btn-action-primary" 
          @click="openCreateTransferDialog"
        >
          <el-icon><Van /></el-icon>
          <span>Tạo Phiếu Điều Chuyển (ITN)</span>
        </el-button>
        <el-dropdown v-if="canManage" trigger="click" @command="handleHeaderCommand">
          <el-button class="btn-action-secondary">
            <span>Thêm Mới</span>
            <el-icon class="el-icon--right"><ArrowDown /></el-icon>
          </el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="create_branch">
                <el-icon><OfficeBuilding /></el-icon>
                <span>Thêm Chi Nhánh Mới</span>
              </el-dropdown-item>
              <el-dropdown-item command="create_warehouse">
                <el-icon><House /></el-icon>
                <span>Thêm Kho Hàng Mới</span>
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </div>

    <!-- STATS OVERVIEW CARDS -->
    <div class="stats-grid">
      <div class="stat-card">
        <div class="stat-icon-wrapper stat-blue">
          <el-icon><OfficeBuilding /></el-icon>
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ branches.length }}</div>
          <div class="stat-label">Tổng Chi Nhánh Hoạt Động</div>
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-icon-wrapper stat-emerald">
          <el-icon><House /></el-icon>
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ warehouses.length }}</div>
          <div class="stat-label">Kho Hàng Toàn Chuỗi</div>
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-icon-wrapper stat-amber">
          <el-icon><Van /></el-icon>
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ inTransitCount }}</div>
          <div class="stat-label">Phiếu ITN Đang Đi Đường</div>
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-icon-wrapper stat-rose">
          <el-icon><Warning /></el-icon>
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ lowStockCount }}</div>
          <div class="stat-label">Sản Phẩm Cận Định Mức</div>
        </div>
      </div>
    </div>

    <!-- MAIN TABS CONTAINER -->
    <el-tabs v-model="activeTab" class="custom-enterprise-tabs">
      <!-- TAB 1: CHI NHÁNH & KHO HÀNG -->
      <el-tab-pane label="Mạng Lưới Chi Nhánh & Kho" name="network">
        <div class="network-split-layout">
          <!-- BRANCH LIST (LEFT) -->
          <div class="split-col branch-col">
            <div class="card-box">
              <div class="box-header">
                <div class="box-title-group">
                  <h3 class="box-title">Danh Sách Chi Nhánh</h3>
                  <span class="badge-count">{{ branches.length }}</span>
                </div>
                <el-button 
                  v-if="canManage"
                  size="small" 
                  type="primary" 
                  plain 
                  @click="openCreateBranchDialog"
                >
                  <el-icon><Plus /></el-icon>
                  <span>Thêm Chi Nhánh</span>
                </el-button>
              </div>

              <div class="branch-cards-list">
                <div 
                  v-for="b in branches" 
                  :key="b.id"
                  class="branch-item-card"
                  :class="{ 'is-selected': selectedBranchId === b.id }"
                  @click="selectBranch(b.id)"
                >
                  <div class="branch-card-top">
                    <div class="branch-name-wrap">
                      <span class="branch-code">{{ b.code }}</span>
                      <h4 class="branch-name">{{ b.name }}</h4>
                    </div>
                    <el-tag v-if="b.isHeadquarter" size="small" type="danger" effect="dark" class="tag-hq">
                      TRỤ SỞ CHÍNH
                    </el-tag>
                  </div>

                  <div class="branch-meta-row">
                    <div class="meta-item">
                      <el-icon><Location /></el-icon>
                      <span class="meta-text">{{ b.address || 'Chưa có địa chỉ' }}</span>
                    </div>
                    <div class="meta-item">
                      <el-icon><Phone /></el-icon>
                      <span class="meta-text">{{ b.phone || 'Chưa có SĐT' }}</span>
                    </div>
                  </div>

                  <div class="branch-card-bottom">
                    <div class="wh-pill">
                      <el-icon><House /></el-icon>
                      <span>{{ b.warehouseCount ?? 0 }} Kho trực thuộc</span>
                    </div>
                    <div class="branch-card-actions" @click.stop>
                      <el-switch 
                        v-if="canManage"
                        :model-value="b.status === 'ACTIVE'" 
                        active-text="Mở" 
                        inactive-text="Đóng"
                        inline-prompt
                        @change="toggleBranchStatus(b.id)"
                      />
                      <el-button 
                        v-if="canManage"
                        size="small" 
                        text 
                        type="primary" 
                        @click="openEditBranchDialog(b)"
                      >
                        Sửa
                      </el-button>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- WAREHOUSES OF BRANCH (RIGHT) -->
          <div class="split-col warehouse-col">
            <div class="card-box">
              <div class="box-header">
                <div class="box-title-group">
                  <h3 class="box-title">
                    {{ selectedBranch ? 'Kho Thuộc: ' + selectedBranch.name : 'Tất Cả Kho Hàng' }}
                  </h3>
                  <span class="badge-count">{{ filteredWarehouses.length }}</span>
                </div>
                <div class="header-right-tools">
                  <el-button 
                    v-if="selectedBranchId" 
                    size="small" 
                    text 
                    @click="selectedBranchId = null"
                  >
                    Xem tất cả kho
                  </el-button>
                  <el-button 
                    v-if="canManage"
                    size="small" 
                    type="primary" 
                    plain 
                    @click="openCreateWarehouseDialog"
                  >
                    <el-icon><Plus /></el-icon>
                    <span>Thêm Kho Hàng</span>
                  </el-button>
                </div>
              </div>

              <el-table 
                v-loading="loadingWarehouses" 
                :data="filteredWarehouses" 
                stripe 
                class="enterprise-table"
                empty-text="Chưa có kho hàng nào"
              >
                <el-table-column prop="code" label="Mã Kho" width="130">
                  <template #default="{ row }">
                    <span class="code-badge">{{ row.code }}</span>
                  </template>
                </el-table-column>

                <el-table-column prop="name" label="Tên Kho & Chi Nhánh" min-width="180">
                  <template #default="{ row }">
                    <div class="name-cell">
                      <span class="font-bold text-dark">{{ row.name }}</span>
                      <span class="sub-text">{{ row.branchName }}</span>
                    </div>
                  </template>
                </el-table-column>

                <el-table-column prop="warehouseType" label="Loại Kho" width="150">
                  <template #default="{ row }">
                    <el-tag :type="getWarehouseTypeTag(row.warehouseType)" size="small">
                      {{ getWarehouseTypeLabel(row.warehouseType) }}
                    </el-tag>
                  </template>
                </el-table-column>

                <el-table-column prop="totalSkus" label="Số Mặt Hàng" width="120" align="center">
                  <template #default="{ row }">
                    <span class="font-medium">{{ row.totalSkus ?? 0 }} SKU</span>
                  </template>
                </el-table-column>

                <el-table-column prop="totalAvailableStock" label="Tồn Khả Dụng" width="130" align="right">
                  <template #default="{ row }">
                    <span class="font-bold text-emerald">{{ (row.totalAvailableStock ?? 0).toLocaleString() }}</span>
                  </template>
                </el-table-column>

                <el-table-column prop="active" label="Trạng Thái" width="110" align="center">
                  <template #default="{ row }">
                    <el-tag :type="row.active ? 'success' : 'info'" size="small">
                      {{ row.active ? 'Hoạt động' : 'Tạm ngưng' }}
                    </el-tag>
                  </template>
                </el-table-column>

                <el-table-column v-if="canManage" label="Thao Tác" width="90" align="center">
                  <template #default="{ row }">
                    <el-button size="small" text type="primary" @click="openEditWarehouseDialog(row)">
                      Sửa
                    </el-button>
                  </template>
                </el-table-column>
              </el-table>
            </div>
          </div>
        </div>
      </el-tab-pane>

      <!-- TAB 2: TỒN KHO ĐA ĐIỂM -->
      <el-tab-pane label="Tồn Kho Đa Điểm & Ma Trận Kho" name="stocks">
        <div class="card-box">
          <div class="filter-toolbar">
            <el-select 
              v-model="stockFilterBranch" 
              placeholder="Lọc theo Chi nhánh" 
              clearable 
              style="width: 220px;"
              @change="fetchStocks"
            >
              <el-option 
                v-for="b in branches" 
                :key="b.id" 
                :label="b.name" 
                :value="b.id" 
              />
            </el-select>

            <el-select 
              v-model="stockFilterWarehouse" 
              placeholder="Lọc theo Kho" 
              clearable 
              style="width: 220px;"
              @change="fetchStocks"
            >
              <el-option 
                v-for="w in warehouses" 
                :key="w.id" 
                :label="w.name" 
                :value="w.id" 
              />
            </el-select>

            <el-input 
              v-model="stockSearchQuery" 
              placeholder="Tìm theo tên hoặc mã SKU sản phẩm..." 
              clearable 
              style="width: 280px;"
              @input="onStockSearchDebounce"
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>

            <el-button type="default" plain @click="fetchStocks">
              <el-icon><Refresh /></el-icon>
              <span>Làm Mới</span>
            </el-button>
          </div>

          <el-table 
            v-loading="loadingStocks" 
            :data="stocks" 
            stripe 
            class="enterprise-table"
            empty-text="Không có bản ghi tồn kho nào phù hợp"
          >
            <el-table-column prop="productSku" label="Mã SKU" width="130">
              <template #default="{ row }">
                <span class="code-badge">{{ row.productSku }}</span>
              </template>
            </el-table-column>

            <el-table-column prop="productName" label="Sản Phẩm" min-width="200">
              <template #default="{ row }">
                <span class="font-bold text-dark">{{ row.productName }}</span>
              </template>
            </el-table-column>

            <el-table-column prop="warehouseName" label="Vị Trí Kho Hàng" min-width="160">
              <template #default="{ row }">
                <div class="wh-location-tag">
                  <el-icon><House /></el-icon>
                  <span>{{ row.warehouseName }}</span>
                </div>
              </template>
            </el-table-column>

            <el-table-column prop="physicalStock" label="Tồn Thực Tế" width="120" align="right">
              <template #default="{ row }">
                <span class="font-bold">{{ (row.physicalStock ?? 0).toLocaleString() }}</span>
              </template>
            </el-table-column>

            <el-table-column prop="availableStock" label="Tồn Khả Dụng" width="130" align="right">
              <template #default="{ row }">
                <span 
                  class="font-bold" 
                  :class="row.isLowStock ? 'text-rose' : 'text-emerald'"
                >
                  {{ (row.availableStock ?? 0).toLocaleString() }}
                </span>
                <el-tag v-if="row.isLowStock" size="small" type="danger" effect="plain" class="ml-1">
                  Cận min
                </el-tag>
              </template>
            </el-table-column>

            <el-table-column prop="reservedStock" label="Giữ Chỗ" width="110" align="right">
              <template #default="{ row }">
                <span class="text-secondary">{{ (row.reservedStock ?? 0).toLocaleString() }}</span>
              </template>
            </el-table-column>

            <el-table-column prop="inTransitStock" label="Đang Đi Đường" width="130" align="right">
              <template #default="{ row }">
                <span v-if="(row.inTransitStock ?? 0) > 0" class="text-amber font-bold">
                  +{{ (row.inTransitStock ?? 0).toLocaleString() }}
                </span>
                <span v-else class="text-secondary">0</span>
              </template>
            </el-table-column>

            <el-table-column prop="minStockAlert" label="Định Mức Min" width="110" align="center">
              <template #default="{ row }">
                <span class="text-secondary">{{ row.minStockAlert ?? 10 }}</span>
              </template>
            </el-table-column>

            <el-table-column v-if="canManage" label="Thao Tác" width="130" align="center">
              <template #default="{ row }">
                <el-button size="small" text type="primary" @click="openAdjustStockDialog(row)">
                  Điều chỉnh
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>

      <!-- TAB 3: ĐIỀU CHUYỂN KHO NỘI BỘ (ITN) -->
      <el-tab-pane label="Phiếu Điều Chuyển Kho (ITN)" name="transfers">
        <div class="card-box">
          <div class="filter-toolbar">
            <el-radio-group v-model="transferStatusFilter" size="default" @change="fetchTransfers">
              <el-radio-button label="">Tất Cả</el-radio-button>
              <el-radio-button label="DRAFT">Bản Nháp ({{ draftTransfersCount }})</el-radio-button>
              <el-radio-button label="IN_TRANSIT">Đang Đi Đường ({{ inTransitCount }})</el-radio-button>
              <el-radio-button label="COMPLETED">Đã Hoàn Tất</el-radio-button>
              <el-radio-button label="CANCELLED">Đã Hủy</el-radio-button>
            </el-radio-group>

            <div class="flex-spacer"></div>

            <el-button 
              v-if="canManage"
              type="primary" 
              class="btn-action-primary" 
              @click="openCreateTransferDialog"
            >
              <el-icon><Plus /></el-icon>
              <span>Lập Phiếu Điều Chuyển Mới</span>
            </el-button>
          </div>

          <el-table 
            v-loading="loadingTransfers" 
            :data="transfers" 
            stripe 
            class="enterprise-table"
            empty-text="Chưa có phiếu điều chuyển kho nào"
          >
            <el-table-column prop="transferCode" label="Mã Phiếu ITN" width="170">
              <template #default="{ row }">
                <span class="code-badge-blue cursor-pointer" @click="viewTransferDetail(row)">
                  {{ row.transferCode }}
                </span>
              </template>
            </el-table-column>

            <el-table-column label="Lộ Trình Điều Chuyển" min-width="240">
              <template #default="{ row }">
                <div class="route-display">
                  <span class="route-point from">{{ row.fromWarehouseName }}</span>
                  <el-icon class="route-arrow"><Right /></el-icon>
                  <span class="route-point to">{{ row.toWarehouseName }}</span>
                </div>
              </template>
            </el-table-column>

            <el-table-column prop="totalQuantity" label="Tổng Số Lượng" width="130" align="center">
              <template #default="{ row }">
                <span class="font-bold text-dark">{{ (row.totalQuantity ?? 0).toLocaleString() }} cái</span>
              </template>
            </el-table-column>

            <el-table-column prop="status" label="Trạng Thái" width="150" align="center">
              <template #default="{ row }">
                <el-tag :type="getTransferStatusTag(row.status)" effect="light">
                  <span v-if="row.status === 'IN_TRANSIT'" class="pulse-dot"></span>
                  {{ getTransferStatusLabel(row.status) }}
                </el-tag>
              </template>
            </el-table-column>

            <el-table-column prop="createdBy" label="Người Lập & Ngày Tạo" width="170">
              <template #default="{ row }">
                <div class="meta-stacked">
                  <span class="font-medium text-dark">{{ row.createdBy }}</span>
                  <span class="sub-text">{{ formatDateTime(row.createdAt) }}</span>
                </div>
              </template>
            </el-table-column>

            <el-table-column label="Thao Tác Xử Lý" width="220" align="center" fixed="right">
              <template #default="{ row }">
                <div class="actions-group">
                  <el-button size="small" text type="primary" @click="viewTransferDetail(row)">
                    Chi tiết
                  </el-button>

                  <!-- Khi ở DRAFT: Có nút Xuất kho (Dispatch) -->
                  <el-button 
                    v-if="row.status === 'DRAFT' && canManage" 
                    size="small" 
                    type="warning" 
                    plain
                    @click="confirmDispatchTransfer(row)"
                  >
                    Xuất kho
                  </el-button>

                  <!-- Khi ở IN_TRANSIT: Có nút Nhận kho & Kiểm đếm (Receive & Audit) -->
                  <el-button 
                    v-if="row.status === 'IN_TRANSIT' && canManage" 
                    size="small" 
                    type="success"
                    @click="openReceiveDialog(row)"
                  >
                    Nhận hàng
                  </el-button>

                  <!-- Nút Hủy phiếu khi chưa Completed -->
                  <el-button 
                    v-if="(row.status === 'DRAFT' || row.status === 'IN_TRANSIT') && canManage" 
                    size="small" 
                    text 
                    type="danger"
                    @click="cancelTransfer(row)"
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
    <!-- DIALOG: THÊM / SỬA CHI NHÁNH -->
    <!-- ======================================================== -->
    <el-dialog 
      v-model="branchDialogVisible" 
      :title="editingBranchId ? 'Chỉnh Sửa Chi Nhánh' : 'Thêm Chi Nhánh Mới'"
      width="540px"
      destroy-on-close
    >
      <el-form :model="branchForm" label-position="top">
        <el-form-item label="Mã Chi Nhánh (Tùy chọn, để trống sẽ tự sinh)">
          <el-input v-model="branchForm.code" placeholder="Ví dụ: BR-HCM-02" :disabled="!!editingBranchId" />
        </el-form-item>

        <el-form-item label="Tên Chi Nhánh *" required>
          <el-input v-model="branchForm.name" placeholder="Ví dụ: Chi nhánh Quận 3 - TP.HCM" />
        </el-form-item>

        <el-form-item label="Địa Chỉ">
          <el-input v-model="branchForm.address" placeholder="Địa chỉ chi nhánh..." />
        </el-form-item>

        <el-form-item label="Số Điện Thoại">
          <el-input v-model="branchForm.phone" placeholder="028-xxxxxxxx" />
        </el-form-item>

        <el-form-item>
          <el-checkbox v-model="branchForm.isHeadquarter">
            Đánh dấu là Trụ Sở Chính (Headquarter)
          </el-checkbox>
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="branchDialogVisible = false">Hủy</el-button>
          <el-button type="primary" :loading="savingBranch" @click="saveBranch">
            Lưu Chi Nhánh
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- ======================================================== -->
    <!-- DIALOG: THÊM / SỬA KHO HÀNG -->
    <!-- ======================================================== -->
    <el-dialog 
      v-model="warehouseDialogVisible" 
      :title="editingWarehouseId ? 'Chỉnh Sửa Kho Hàng' : 'Thêm Kho Hàng Mới'"
      width="540px"
      destroy-on-close
    >
      <el-form :model="warehouseForm" label-position="top">
        <el-form-item label="Chi Nhánh Trực Thuộc *" required>
          <el-select v-model="warehouseForm.branchId" placeholder="Chọn chi nhánh sở hữu" style="width: 100%;">
            <el-option 
              v-for="b in branches" 
              :key="b.id" 
              :label="b.name" 
              :value="b.id" 
            />
          </el-select>
        </el-form-item>

        <el-form-item label="Mã Kho (Tùy chọn)">
          <el-input v-model="warehouseForm.code" placeholder="Ví dụ: WH-HCM-Q3" :disabled="!!editingWarehouseId" />
        </el-form-item>

        <el-form-item label="Tên Kho Hàng *" required>
          <el-input v-model="warehouseForm.name" placeholder="Ví dụ: Kho Bán Lẻ Quận 3" />
        </el-form-item>

        <el-form-item label="Loại Kho Hàng *" required>
          <el-select v-model="warehouseForm.warehouseType" style="width: 100%;">
            <el-option label="Kho Tổng (Central Hub)" value="CENTRAL_HUB" />
            <el-option label="Kho Cửa Hàng / Bán Lẻ (Retail Store)" value="RETAIL_STORE" />
            <el-option label="Kho Hàng Lỗi / Đổi Trả (Damaged Goods)" value="DAMAGED_GOODS" />
            <el-option label="Kho Trung Chuyển (Transit)" value="TRANSIT" />
          </el-select>
        </el-form-item>

        <el-form-item label="Địa Chỉ Kho">
          <el-input v-model="warehouseForm.address" placeholder="Địa chỉ chi tiết kho hàng..." />
        </el-form-item>

        <el-form-item>
          <el-switch v-model="warehouseForm.active" active-text="Hoạt động" inactive-text="Tạm ngưng" />
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="warehouseDialogVisible = false">Hủy</el-button>
          <el-button type="primary" :loading="savingWarehouse" @click="saveWarehouse">
            Lưu Kho Hàng
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- ======================================================== -->
    <!-- DIALOG: TẠO PHIẾU ĐIỀU CHUYỂN KHO (ITN) -->
    <!-- ======================================================== -->
    <el-dialog 
      v-model="createTransferDialogVisible" 
      title="Tạo Phiếu Điều Chuyển Kho Nội Bộ (ITN)"
      width="780px"
      destroy-on-close
    >
      <el-form :model="transferForm" label-position="top">
        <div class="grid-2-col">
          <el-form-item label="Kho Xuất Đi *" required>
            <el-select 
              v-model="transferForm.fromWarehouseId" 
              placeholder="Chọn kho xuất hàng" 
              style="width: 100%;"
              @change="onFromWarehouseChanged"
            >
              <el-option 
                v-for="w in warehouses" 
                :key="w.id" 
                :label="`${w.name} (${w.branchName})`" 
                :value="w.id" 
              />
            </el-select>
          </el-form-item>

          <el-form-item label="Kho Nhập Đến *" required>
            <el-select 
              v-model="transferForm.toWarehouseId" 
              placeholder="Chọn kho nhận hàng" 
              style="width: 100%;"
            >
              <el-option 
                v-for="w in targetWarehouses" 
                :key="w.id" 
                :label="`${w.name} (${w.branchName})`" 
                :value="w.id" 
              />
            </el-select>
          </el-form-item>
        </div>

        <el-form-item label="Ghi Chú Điều Chuyển">
          <el-input 
            v-model="transferForm.note" 
            placeholder="Lý do điều chuyển, biển số xe vận chuyển..." 
          />
        </el-form-item>

        <!-- ITEMS TABLE IN MODAL -->
        <div class="transfer-items-block">
          <div class="block-header">
            <span class="font-bold text-dark">Danh Sách Hàng Điều Chuyển</span>
            <el-button size="small" type="primary" plain @click="addTransferItemRow">
              <el-icon><Plus /></el-icon>
              <span>Thêm Hàng</span>
            </el-button>
          </div>

          <div v-for="(it, idx) in transferForm.items" :key="idx" class="transfer-item-row">
            <div class="row-product">
              <el-select 
                v-model="it.productId" 
                placeholder="Chọn sản phẩm..." 
                filterable 
                style="width: 100%;"
                @change="onProductSelect(it)"
              >
                <el-option 
                  v-for="p in allProducts" 
                  :key="p.id" 
                  :label="`[${p.sku}] ${p.name}`" 
                  :value="p.id" 
                />
              </el-select>
            </div>

            <div class="row-qty">
              <el-input-number 
                v-model="it.quantity" 
                :min="1" 
                :max="it.maxAvailable ?? 99999" 
                placeholder="Số lượng"
                style="width: 140px;" 
              />
              <span v-if="it.maxAvailable !== undefined" class="stock-hint">
                Tồn khả dụng: {{ it.maxAvailable }}
              </span>
            </div>

            <div class="row-action">
              <el-button 
                type="danger" 
                text 
                :disabled="transferForm.items.length === 1" 
                @click="removeTransferItemRow(idx)"
              >
                <el-icon><Delete /></el-icon>
              </el-button>
            </div>
          </div>
        </div>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="createTransferDialogVisible = false">Hủy</el-button>
          <el-button type="primary" :loading="savingTransfer" @click="saveTransfer">
            Tạo Phiếu ITN
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- ======================================================== -->
    <!-- DIALOG: NHẬN HÀNG & KIỂM ĐẾM THỰC TẾ (RECEIVE & AUDIT) -->
    <!-- ======================================================== -->
    <el-dialog 
      v-model="receiveDialogVisible" 
      title="Xác Nhận Nhập Kho & Kiểm Đếm Hàng Hóa"
      width="780px"
      destroy-on-close
    >
      <div v-if="currentTransfer" class="receive-dialog-body">
        <div class="receive-header-info">
          <div class="info-item">
            <span class="label">Mã Phiếu:</span>
            <span class="val font-bold text-blue">{{ currentTransfer.transferCode }}</span>
          </div>
          <div class="info-item">
            <span class="label">Từ Kho:</span>
            <span class="val">{{ currentTransfer.fromWarehouseName }}</span>
          </div>
          <div class="info-item">
            <span class="label">Đến Kho:</span>
            <span class="val font-bold text-emerald">{{ currentTransfer.toWarehouseName }}</span>
          </div>
        </div>

        <el-alert 
          type="info" 
          show-icon 
          :closable="false" 
          class="mb-3"
          title="Quy trình kiểm nhận Enterprise: Kiểm đếm số lượng thực tế nhận được. Nếu phát hiện rách, hỏng, lỗi, ghi nhận vào cột Hàng Lỗi để hệ thống tự động đưa vào Kho Hàng Lỗi."
        />

        <el-table :data="receiveItems" stripe class="enterprise-table">
          <el-table-column prop="productName" label="Sản Phẩm" min-width="180">
            <template #default="{ row }">
              <div class="name-cell">
                <span class="font-bold text-dark">{{ row.productName }}</span>
                <span class="sub-text">{{ row.productSku }}</span>
              </div>
            </template>
          </el-table-column>

          <el-table-column prop="transferredQuantity" label="Xuất Đi" width="90" align="center">
            <template #default="{ row }">
              <span class="font-bold">{{ row.transferredQuantity }}</span>
            </template>
          </el-table-column>

          <el-table-column label="Thực Nhận (Đạt Chuẩn)" width="160" align="center">
            <template #default="{ row }">
              <el-input-number 
                v-model="row.receivedQuantity" 
                :min="0" 
                :max="row.transferredQuantity" 
                size="small" 
                style="width: 120px;"
              />
            </template>
          </el-table-column>

          <el-table-column label="Hàng Lỗi / Rách" width="140" align="center">
            <template #default="{ row }">
              <el-input-number 
                v-model="row.damagedQuantity" 
                :min="0" 
                :max="row.transferredQuantity - (row.receivedQuantity ?? 0)" 
                size="small" 
                style="width: 110px;"
              />
            </template>
          </el-table-column>
        </el-table>

        <div class="mt-3">
          <el-input 
            v-model="receiveNote" 
            placeholder="Ghi chú biên bản kiểm nhận hàng thực tế..." 
          />
        </div>
      </div>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="receiveDialogVisible = false">Đóng</el-button>
          <el-button type="success" :loading="savingReceive" @click="submitReceive">
            Xác Nhận Nhập Kho Hoàn Tất
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- ======================================================== -->
    <!-- DIALOG: ĐIỀU CHỈNH TỒN KHO NHANH -->
    <!-- ======================================================== -->
    <el-dialog 
      v-model="adjustStockDialogVisible" 
      title="Điều Chỉnh Tồn Kho Thực Tế (Kiểm Kê Nhanh)"
      width="500px"
      destroy-on-close
    >
      <div v-if="adjustingStock" class="adjust-body">
        <div class="adjust-product-summary">
          <span class="code-badge">{{ adjustingStock.productSku }}</span>
          <h4 class="font-bold text-dark mt-1">{{ adjustingStock.productName }}</h4>
          <p class="text-secondary text-sm">Tại kho: {{ adjustingStock.warehouseName }}</p>
        </div>

        <el-form label-position="top" class="mt-3">
          <div class="grid-2-col">
            <el-form-item label="Tồn Hiện Tại">
              <el-input :model-value="adjustingStock.physicalStock" disabled />
            </el-form-item>
            <el-form-item label="Tồn Thực Tế Mới *" required>
              <el-input-number 
                v-model="adjustNewPhysical" 
                :min="0" 
                style="width: 100%;" 
              />
            </el-form-item>
          </div>

          <el-form-item label="Định Mức Cảnh Báo Tối Thiểu">
            <el-input-number 
              v-model="adjustMinAlert" 
              :min="0" 
              style="width: 100%;" 
            />
          </el-form-item>

          <el-form-item label="Lý Do Điều Chỉnh">
            <el-input 
              v-model="adjustReason" 
              placeholder="Kiểm kê đột xuất, phát hiện thừa/thiếu..." 
            />
          </el-form-item>
        </el-form>
      </div>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="adjustStockDialogVisible = false">Hủy</el-button>
          <el-button type="primary" :loading="savingAdjust" @click="submitAdjustStock">
            Cập Nhật Tồn Kho
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- ======================================================== -->
    <!-- DRAWER: CHI TIẾT PHIẾU ĐIỀU CHUYỂN ITN -->
    <!-- ======================================================== -->
    <el-drawer 
      v-model="detailDrawerVisible" 
      title="Chi Tiết Phiếu Điều Chuyển Kho"
      size="560px"
    >
      <div v-if="detailTransfer" class="drawer-detail-body">
        <div class="detail-header-card">
          <div class="code-row">
            <span class="code-large">{{ detailTransfer.transferCode }}</span>
            <el-tag :type="getTransferStatusTag(detailTransfer.status)" size="large">
              {{ getTransferStatusLabel(detailTransfer.status) }}
            </el-tag>
          </div>
          <div class="route-box mt-3">
            <div class="route-node">
              <span class="node-label">KHO XUẤT</span>
              <span class="node-name">{{ detailTransfer.fromWarehouseName }}</span>
            </div>
            <div class="route-divider">
              <el-icon><Right /></el-icon>
            </div>
            <div class="route-node">
              <span class="node-label">KHO NHẬN</span>
              <span class="node-name">{{ detailTransfer.toWarehouseName }}</span>
            </div>
          </div>
        </div>

        <!-- TIMELINE TRẠNG THÁI -->
        <div class="detail-section">
          <h4 class="section-title">Nhật Ký Điều Chuyển</h4>
          <el-timeline class="custom-timeline">
            <el-timeline-item 
              timestamp="Tạo phiếu (DRAFT)" 
              placement="top" 
              type="primary"
            >
              <p class="timeline-meta">Tạo bởi: {{ detailTransfer.createdBy }} lúc {{ formatDateTime(detailTransfer.createdAt) }}</p>
            </el-timeline-item>

            <el-timeline-item 
              v-if="detailTransfer.departureDate" 
              timestamp="Xuất kho & Đang đi đường (IN_TRANSIT)" 
              placement="top" 
              type="warning"
            >
              <p class="timeline-meta">Xuất bởi: {{ detailTransfer.transferredBy }} lúc {{ formatDateTime(detailTransfer.departureDate) }}</p>
            </el-timeline-item>

            <el-timeline-item 
              v-if="detailTransfer.arrivalDate" 
              timestamp="Đã nhận & Hoàn tất (COMPLETED)" 
              placement="top" 
              type="success"
            >
              <p class="timeline-meta">Nhận bởi: {{ detailTransfer.receivedBy }} lúc {{ formatDateTime(detailTransfer.arrivalDate) }}</p>
            </el-timeline-item>

            <el-timeline-item 
              v-if="detailTransfer.status === 'CANCELLED'" 
              timestamp="Đã hủy bỏ (CANCELLED)" 
              placement="top" 
              type="danger"
            >
              <p class="timeline-meta">Phiếu đã được hoàn lại kho xuất</p>
            </el-timeline-item>
          </el-timeline>
        </div>

        <!-- DANH SÁCH MẶT HÀNG -->
        <div class="detail-section">
          <h4 class="section-title">Danh Sách Mặt Hàng ({{ detailTransfer.items?.length ?? 0 }})</h4>
          <div class="detail-items-list">
            <div v-for="it in detailTransfer.items" :key="it.id" class="detail-item-card">
              <div class="it-top">
                <span class="font-bold text-dark">{{ it.productName }}</span>
                <span class="code-badge">{{ it.productSku }}</span>
              </div>
              <div class="it-qty-grid">
                <div class="qty-box">
                  <span class="qty-lbl">Yêu cầu</span>
                  <span class="qty-val">{{ it.requestedQuantity }}</span>
                </div>
                <div class="qty-box">
                  <span class="qty-lbl">Thực xuất</span>
                  <span class="qty-val font-bold text-blue">{{ it.transferredQuantity }}</span>
                </div>
                <div class="qty-box">
                  <span class="qty-lbl">Thực nhận</span>
                  <span class="qty-val font-bold text-emerald">{{ it.receivedQuantity }}</span>
                </div>
                <div v-if="(it.damagedQuantity ?? 0) > 0" class="qty-box">
                  <span class="qty-lbl">Hàng lỗi</span>
                  <span class="qty-val font-bold text-rose">{{ it.damagedQuantity }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div v-if="detailTransfer.note" class="detail-section">
          <h4 class="section-title">Ghi Chú</h4>
          <p class="note-box">{{ detailTransfer.note }}</p>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
// ==========================================
// 1. HELPER FUNCTIONS PLACED AT THE TOP (ZERO TDZ ERROR)
// ==========================================
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

const getWarehouseTypeLabel = (type) => {
  switch (type) {
    case 'CENTRAL_HUB': return 'Kho Tổng'
    case 'RETAIL_STORE': return 'Kho Cửa Hàng'
    case 'DAMAGED_GOODS': return 'Kho Hàng Lỗi'
    case 'TRANSIT': return 'Kho Trung Chuyển'
    default: return type || 'Kho'
  }
}

const getWarehouseTypeTag = (type) => {
  switch (type) {
    case 'CENTRAL_HUB': return 'danger'
    case 'RETAIL_STORE': return 'primary'
    case 'DAMAGED_GOODS': return 'warning'
    case 'TRANSIT': return 'info'
    default: return 'info'
  }
}

const getTransferStatusLabel = (status) => {
  switch (status) {
    case 'DRAFT': return 'Bản Nháp'
    case 'IN_TRANSIT': return 'Đang Vận Chuyển'
    case 'COMPLETED': return 'Đã Hoàn Tất'
    case 'CANCELLED': return 'Đã Hủy'
    default: return status || '—'
  }
}

const getTransferStatusTag = (status) => {
  switch (status) {
    case 'DRAFT': return 'info'
    case 'IN_TRANSIT': return 'warning'
    case 'COMPLETED': return 'success'
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
  OfficeBuilding,
  House,
  Van,
  Warning,
  Plus,
  Search,
  Refresh,
  Location,
  Phone,
  Right,
  Delete,
  ArrowDown
} from '@element-plus/icons-vue'

const authStore = useAuthStore()
const canManage = computed(() => {
  return authStore.isAdmin || authStore.hasPermission('BRANCH_MANAGE') || authStore.user?.role === 'MANAGER'
})

const activeTab = ref('network')

// Data lists
const branches = ref([])
const warehouses = ref([])
const stocks = ref([])
const transfers = ref([])
const allProducts = ref([])

// Loadings
const loadingWarehouses = ref(false)
const loadingStocks = ref(false)
const loadingTransfers = ref(false)

// Selections & filters
const selectedBranchId = ref(null)
const stockFilterBranch = ref(null)
const stockFilterWarehouse = ref(null)
const stockSearchQuery = ref('')
const transferStatusFilter = ref('')

// Computed stats
const selectedBranch = computed(() => branches.value.find(b => b.id === selectedBranchId.value))
const filteredWarehouses = computed(() => {
  if (!selectedBranchId.value) return warehouses.value
  return warehouses.value.filter(w => w.branchId === selectedBranchId.value)
})

const inTransitCount = computed(() => {
  return transfers.value.filter(t => t.status === 'IN_TRANSIT').length
})

const draftTransfersCount = computed(() => {
  return transfers.value.filter(t => t.status === 'DRAFT').length
})

const lowStockCount = computed(() => {
  return stocks.value.filter(s => s.isLowStock).length
})

const targetWarehouses = computed(() => {
  if (!transferForm.fromWarehouseId) return warehouses.value
  return warehouses.value.filter(w => w.id !== transferForm.fromWarehouseId && w.active)
})

// ==========================================
// 3. DIALOG STATES & FORMS
// ==========================================

// Branch Dialog
const branchDialogVisible = ref(false)
const savingBranch = ref(false)
const editingBranchId = ref(null)
const branchForm = reactive({
  code: '',
  name: '',
  address: '',
  phone: '',
  isHeadquarter: false
})

// Warehouse Dialog
const warehouseDialogVisible = ref(false)
const savingWarehouse = ref(false)
const editingWarehouseId = ref(null)
const warehouseForm = reactive({
  branchId: null,
  code: '',
  name: '',
  warehouseType: 'RETAIL_STORE',
  address: '',
  active: true
})

// Transfer Dialog
const createTransferDialogVisible = ref(false)
const savingTransfer = ref(false)
const transferForm = reactive({
  fromWarehouseId: null,
  toWarehouseId: null,
  note: '',
  items: []
})

// Receive Dialog
const receiveDialogVisible = ref(false)
const savingReceive = ref(false)
const currentTransfer = ref(null)
const receiveItems = ref([])
const receiveNote = ref('')

// Adjust Stock Dialog
const adjustStockDialogVisible = ref(false)
const savingAdjust = ref(false)
const adjustingStock = ref(null)
const adjustNewPhysical = ref(0)
const adjustMinAlert = ref(10)
const adjustReason = ref('')

// Detail Drawer
const detailDrawerVisible = ref(false)
const detailTransfer = ref(null)

// Debounce timer for search
let searchTimer = null
const onStockSearchDebounce = () => {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(() => {
    fetchStocks()
  }, 300)
}

// ==========================================
// 4. API METHODS
// ==========================================

const fetchBranches = async () => {
  try {
    const res = await api.get('/branches')
    branches.value = res.data || []
  } catch (err) {
    ElMessage.error(err.response?.data?.message || 'Không thể tải danh sách chi nhánh')
  }
}

const fetchWarehouses = async () => {
  loadingWarehouses.value = true
  try {
    const res = await api.get('/warehouses')
    warehouses.value = res.data || []
  } catch (err) {
    ElMessage.error(err.response?.data?.message || 'Không thể tải danh sách kho')
  } finally {
    loadingWarehouses.value = false
  }
}

const fetchStocks = async () => {
  loadingStocks.value = true
  try {
    const params = {}
    if (stockFilterBranch.value) params.branchId = stockFilterBranch.value
    if (stockFilterWarehouse.value) params.warehouseId = stockFilterWarehouse.value
    if (stockSearchQuery.value) params.query = stockSearchQuery.value

    const res = await api.get('/warehouses/stocks', { params })
    stocks.value = res.data || []
  } catch (err) {
    ElMessage.error(err.response?.data?.message || 'Không thể tải dữ liệu tồn kho')
  } finally {
    loadingStocks.value = false
  }
}

const fetchTransfers = async () => {
  loadingTransfers.value = true
  try {
    const params = {}
    if (transferStatusFilter.value) params.status = transferStatusFilter.value

    const res = await api.get('/transfers', { params })
    transfers.value = res.data || []
  } catch (err) {
    ElMessage.error(err.response?.data?.message || 'Không thể tải phiếu điều chuyển')
  } finally {
    loadingTransfers.value = false
  }
}

const fetchProducts = async () => {
  try {
    const res = await api.get('/products')
    allProducts.value = res.data?.content || res.data || []
  } catch {
    // fallback ignore
  }
}

const selectBranch = (branchId) => {
  if (selectedBranchId.value === branchId) {
    selectedBranchId.value = null
  } else {
    selectedBranchId.value = branchId
  }
}

const handleHeaderCommand = (cmd) => {
  if (cmd === 'create_branch') openCreateBranchDialog()
  else if (cmd === 'create_warehouse') openCreateWarehouseDialog()
}

// Branch Actions
const openCreateBranchDialog = () => {
  editingBranchId.value = null
  branchForm.code = ''
  branchForm.name = ''
  branchForm.address = ''
  branchForm.phone = ''
  branchForm.isHeadquarter = false
  branchDialogVisible.value = true
}

const openEditBranchDialog = (b) => {
  editingBranchId.value = b.id
  branchForm.code = b.code
  branchForm.name = b.name
  branchForm.address = b.address
  branchForm.phone = b.phone
  branchForm.isHeadquarter = Boolean(b.isHeadquarter)
  branchDialogVisible.value = true
}

const saveBranch = async () => {
  if (!branchForm.name.trim()) {
    return ElMessage.warning('Vui lòng nhập tên chi nhánh')
  }
  savingBranch.value = true
  try {
    if (editingBranchId.value) {
      await api.put(`/branches/${editingBranchId.value}`, branchForm)
      ElMessage.success('Cập nhật chi nhánh thành công')
    } else {
      await api.post('/branches', branchForm)
      ElMessage.success('Thêm chi nhánh mới thành công')
    }
    branchDialogVisible.value = false
    await fetchBranches()
    await fetchWarehouses()
  } catch (err) {
    ElMessage.error(err.response?.data?.message || 'Lưu chi nhánh thất bại')
  } finally {
    savingBranch.value = false
  }
}

const toggleBranchStatus = async (id) => {
  try {
    await api.patch(`/branches/${id}/toggle-status`)
    ElMessage.success('Đã cập nhật trạng thái chi nhánh')
    await fetchBranches()
  } catch (err) {
    ElMessage.error(err.response?.data?.message || 'Không thể đổi trạng thái chi nhánh')
  }
}

// Warehouse Actions
const openCreateWarehouseDialog = () => {
  editingWarehouseId.value = null
  warehouseForm.branchId = selectedBranchId.value || (branches.value[0]?.id ?? null)
  warehouseForm.code = ''
  warehouseForm.name = ''
  warehouseForm.warehouseType = 'RETAIL_STORE'
  warehouseForm.address = ''
  warehouseForm.active = true
  warehouseDialogVisible.value = true
}

const openEditWarehouseDialog = (w) => {
  editingWarehouseId.value = w.id
  warehouseForm.branchId = w.branchId
  warehouseForm.code = w.code
  warehouseForm.name = w.name
  warehouseForm.warehouseType = w.warehouseType
  warehouseForm.address = w.address
  warehouseForm.active = Boolean(w.active)
  warehouseDialogVisible.value = true
}

const saveWarehouse = async () => {
  if (!warehouseForm.branchId) {
    return ElMessage.warning('Vui lòng chọn chi nhánh trực thuộc')
  }
  if (!warehouseForm.name.trim()) {
    return ElMessage.warning('Vui lòng nhập tên kho')
  }
  savingWarehouse.value = true
  try {
    if (editingWarehouseId.value) {
      await api.put(`/warehouses/${editingWarehouseId.value}`, warehouseForm)
      ElMessage.success('Cập nhật kho hàng thành công')
    } else {
      await api.post('/warehouses', warehouseForm)
      ElMessage.success('Thêm kho hàng mới thành công')
    }
    warehouseDialogVisible.value = false
    await fetchWarehouses()
    await fetchBranches()
  } catch (err) {
    ElMessage.error(err.response?.data?.message || 'Lưu kho hàng thất bại')
  } finally {
    savingWarehouse.value = false
  }
}

// Transfer Actions
const openCreateTransferDialog = () => {
  transferForm.fromWarehouseId = warehouses.value[0]?.id ?? null
  transferForm.toWarehouseId = warehouses.value.find(w => w.id !== transferForm.fromWarehouseId)?.id ?? null
  transferForm.note = ''
  transferForm.items = [
    { productId: null, quantity: 1, maxAvailable: undefined }
  ]
  createTransferDialogVisible.value = true
}

const onFromWarehouseChanged = () => {
  if (transferForm.toWarehouseId === transferForm.fromWarehouseId) {
    transferForm.toWarehouseId = targetWarehouses.value[0]?.id ?? null
  }
  // Reset max available for existing items
  transferForm.items.forEach(it => onProductSelect(it))
}

const addTransferItemRow = () => {
  transferForm.items.push({ productId: null, quantity: 1, maxAvailable: undefined })
}

const removeTransferItemRow = (index) => {
  if (transferForm.items.length > 1) {
    transferForm.items.splice(index, 1)
  }
}

const onProductSelect = (it) => {
  if (!it.productId || !transferForm.fromWarehouseId) {
    it.maxAvailable = undefined
    return
  }
  const match = stocks.value.find(
    s => s.productId === it.productId && s.warehouseId === transferForm.fromWarehouseId
  )
  it.maxAvailable = match?.availableStock ?? 99999
}

const saveTransfer = async () => {
  if (!transferForm.fromWarehouseId || !transferForm.toWarehouseId) {
    return ElMessage.warning('Vui lòng chọn kho xuất và kho nhập')
  }
  if (transferForm.fromWarehouseId === transferForm.toWarehouseId) {
    return ElMessage.warning('Kho xuất và kho nhập không được trùng nhau')
  }
  const validItems = transferForm.items.filter(it => it.productId && it.quantity > 0)
  if (validItems.length === 0) {
    return ElMessage.warning('Vui lòng chọn ít nhất 1 sản phẩm với số lượng hợp lệ')
  }

  savingTransfer.value = true
  try {
    const payload = {
      fromWarehouseId: transferForm.fromWarehouseId,
      toWarehouseId: transferForm.toWarehouseId,
      note: transferForm.note,
      items: validItems.map(it => ({
        productId: it.productId,
        quantity: it.quantity,
        note: ''
      }))
    }
    await api.post('/transfers', payload)
    ElMessage.success('Đã tạo phiếu điều chuyển kho (DRAFT)')
    createTransferDialogVisible.value = false
    await fetchTransfers()
    activeTab.value = 'transfers'
  } catch (err) {
    ElMessage.error(err.response?.data?.message || 'Tạo phiếu điều chuyển thất bại')
  } finally {
    savingTransfer.value = false
  }
}

const confirmDispatchTransfer = async (row) => {
  try {
    await ElMessageBox.confirm(
      `Xác nhận xuất kho cho phiếu [${row.transferCode}]? Hàng sẽ được trừ khỏi ${row.fromWarehouseName} và chuyển sang trạng thái Đang Vận Chuyển.`,
      'Xác Nhận Xuất Kho',
      {
        confirmButtonText: 'Xuất Kho Ngay',
        cancelButtonText: 'Đóng',
        type: 'warning'
      }
    )

    await api.post(`/transfers/${row.id}/dispatch`)
    ElMessage.success(`Phiếu ${row.transferCode} đã xuất kho thành công (IN_TRANSIT)`)
    await fetchTransfers()
    await fetchStocks()
    await fetchWarehouses()
  } catch (err) {
    if (err !== 'cancel') {
      ElMessage.error(err.response?.data?.message || 'Xuất kho thất bại')
    }
  }
}

const openReceiveDialog = (row) => {
  currentTransfer.value = row
  receiveNote.value = ''
  receiveItems.value = (row.items || []).map(it => ({
    itemId: it.id,
    productId: it.productId,
    productName: it.productName,
    productSku: it.productSku,
    transferredQuantity: it.transferredQuantity,
    receivedQuantity: it.transferredQuantity, // Mặc định nhận đủ 100%
    damagedQuantity: 0,
    note: ''
  }))
  receiveDialogVisible.value = true
}

const submitReceive = async () => {
  if (!currentTransfer.value) return

  savingReceive.value = true
  try {
    const payload = {
      note: receiveNote.value,
      items: receiveItems.value.map(it => ({
        itemId: it.itemId,
        productId: it.productId,
        receivedQuantity: it.receivedQuantity,
        damagedQuantity: it.damagedQuantity,
        note: ''
      }))
    }
    await api.post(`/transfers/${currentTransfer.value.id}/receive`, payload)
    ElMessage.success(`Phiếu ${currentTransfer.value.transferCode} đã hoàn tất kiểm nhận & nhập kho!`)
    receiveDialogVisible.value = false
    await fetchTransfers()
    await fetchStocks()
    await fetchWarehouses()
  } catch (err) {
    ElMessage.error(err.response?.data?.message || 'Nhập kho thất bại')
  } finally {
    savingReceive.value = false
  }
}

const cancelTransfer = async (row) => {
  try {
    await ElMessageBox.confirm(
      `Hủy phiếu điều chuyển [${row.transferCode}]? Nếu đang đi đường, tồn kho sẽ được hoàn trả lại cho kho xuất.`,
      'Hủy Phiếu ITN',
      {
        confirmButtonText: 'Xác Nhận Hủy',
        cancelButtonText: 'Đóng',
        type: 'danger'
      }
    )

    await api.post(`/transfers/${row.id}/cancel`)
    ElMessage.success(`Đã hủy phiếu ${row.transferCode}`)
    await fetchTransfers()
    await fetchStocks()
    await fetchWarehouses()
  } catch (err) {
    if (err !== 'cancel') {
      ElMessage.error(err.response?.data?.message || 'Hủy phiếu thất bại')
    }
  }
}

const viewTransferDetail = (row) => {
  detailTransfer.value = row
  detailDrawerVisible.value = true
}

// Adjust Stock
const openAdjustStockDialog = (row) => {
  adjustingStock.value = row
  adjustNewPhysical.value = row.physicalStock ?? 0
  adjustMinAlert.value = row.minStockAlert ?? 10
  adjustReason.value = ''
  adjustStockDialogVisible.value = true
}

const submitAdjustStock = async () => {
  if (!adjustingStock.value) return
  savingAdjust.value = true
  try {
    const payload = {
      warehouseId: adjustingStock.value.warehouseId,
      productId: adjustingStock.value.productId,
      newPhysicalStock: adjustNewPhysical.value,
      minStockAlert: adjustMinAlert.value,
      reason: adjustReason.value
    }
    await api.post('/warehouses/stocks/adjust', payload)
    ElMessage.success('Điều chỉnh tồn kho thành công')
    adjustStockDialogVisible.value = false
    await fetchStocks()
    await fetchWarehouses()
  } catch (err) {
    ElMessage.error(err.response?.data?.message || 'Điều chỉnh tồn kho thất bại')
  } finally {
    savingAdjust.value = false
  }
}

// ==========================================
// 5. LIFECYCLE
// ==========================================
onMounted(async () => {
  await Promise.all([
    fetchBranches(),
    fetchWarehouses(),
    fetchStocks(),
    fetchTransfers(),
    fetchProducts()
  ])
})
</script>

<style scoped>
.branch-warehouse-container {
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
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

.stat-icon-wrapper {
  width: 48px;
  height: 48px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
}

.stat-blue { background: #eff6ff; color: #2563eb; }
.stat-emerald { background: #ecfdf5; color: #059669; }
.stat-amber { background: #fffbeb; color: #d97706; }
.stat-rose { background: #fff1f2; color: #e11d48; }

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

/* Tabs */
.custom-enterprise-tabs :deep(.el-tabs__header) {
  margin-bottom: 20px;
}

.custom-enterprise-tabs :deep(.el-tabs__item) {
  font-size: 15px;
  font-weight: 600;
  height: 44px;
}

/* Network Split Layout */
.network-split-layout {
  display: grid;
  grid-template-columns: 380px minmax(0, 1fr);
  gap: 20px;
}

@media (max-width: 1024px) {
  .network-split-layout {
    grid-template-columns: 1fr;
  }
}

.card-box {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.03);
}

.box-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f1f5f9;
}

.box-title-group {
  display: flex;
  align-items: center;
  gap: 8px;
}

.box-title {
  font-size: 16px;
  font-weight: 700;
  color: #0f172a;
  margin: 0;
}

.badge-count {
  font-size: 11px;
  font-weight: 700;
  padding: 2px 7px;
  border-radius: 999px;
  background: #f1f5f9;
  color: #475569;
}

/* Branch Cards List */
.branch-cards-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  max-height: 600px;
  overflow-y: auto;
  padding-right: 4px;
}

.branch-item-card {
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  padding: 14px;
  background: #ffffff;
  cursor: pointer;
  transition: all 0.2s ease;
}

.branch-item-card:hover {
  border-color: #93c5fd;
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.06);
}

.branch-item-card.is-selected {
  border-color: #2563eb;
  background: #f8fafc;
  box-shadow: 0 0 0 1px #2563eb;
}

.branch-card-top {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 8px;
}

.branch-code {
  font-size: 11px;
  font-weight: 700;
  color: #2563eb;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.branch-name {
  font-size: 14px;
  font-weight: 700;
  color: #0f172a;
  margin: 2px 0 0 0;
}

.tag-hq {
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.05em;
}

.branch-meta-row {
  margin-top: 8px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #64748b;
}

.meta-text {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.branch-card-bottom {
  margin-top: 12px;
  padding-top: 8px;
  border-top: 1px dashed #f1f5f9;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.wh-pill {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  font-weight: 600;
  color: #059669;
}

.branch-card-actions {
  display: flex;
  align-items: center;
  gap: 6px;
}

/* Filter Toolbar */
.filter-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.flex-spacer {
  flex: 1;
}

/* Enterprise Table Styles */
.enterprise-table {
  border-radius: 8px;
  overflow: hidden;
}

.code-badge {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 12px;
  font-weight: 600;
  padding: 2px 6px;
  border-radius: 4px;
  background: #f1f5f9;
  color: #334155;
}

.code-badge-blue {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 12px;
  font-weight: 700;
  padding: 3px 8px;
  border-radius: 4px;
  background: #eff6ff;
  color: #2563eb;
  transition: all 0.15s ease;
}

.code-badge-blue:hover {
  background: #dbeafe;
}

.name-cell {
  display: flex;
  flex-direction: column;
}

.sub-text {
  font-size: 12px;
  color: #64748b;
}

.wh-location-tag {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 500;
  color: #1e293b;
}

.route-display {
  display: flex;
  align-items: center;
  gap: 8px;
}

.route-point {
  font-size: 13px;
  font-weight: 600;
}

.route-point.from { color: #475569; }
.route-point.to { color: #059669; }

.route-arrow {
  color: #94a3b8;
  font-size: 14px;
}

.pulse-dot {
  display: inline-block;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #d97706;
  margin-right: 4px;
  box-shadow: 0 0 0 2px rgba(217, 119, 6, 0.2);
  animation: pulse 1.5s infinite;
}

@keyframes pulse {
  0% { transform: scale(0.95); opacity: 0.8; }
  50% { transform: scale(1.3); opacity: 1; }
  100% { transform: scale(0.95); opacity: 0.8; }
}

.meta-stacked {
  display: flex;
  flex-direction: column;
}

.actions-group {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
}

/* Modals & Transfer Items */
.grid-2-col {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.transfer-items-block {
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 14px;
  background: #f8fafc;
  margin-top: 12px;
}

.block-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.transfer-item-row {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 10px;
}

.row-product {
  flex: 1;
}

.row-qty {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.stock-hint {
  font-size: 11px;
  color: #64748b;
}

.row-action {
  padding-top: 2px;
}

/* Receive Body */
.receive-header-info {
  display: flex;
  gap: 20px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 12px 16px;
  margin-bottom: 16px;
}

.info-item {
  display: flex;
  gap: 6px;
  font-size: 13px;
}

.info-item .label {
  color: #64748b;
}

/* Adjust Body */
.adjust-product-summary {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 12px 16px;
}

/* Drawer Detail */
.detail-header-card {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  padding: 16px;
}

.code-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.code-large {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 18px;
  font-weight: 800;
  color: #2563eb;
}

.route-box {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 12px 16px;
}

.route-node {
  display: flex;
  flex-direction: column;
}

.node-label {
  font-size: 10px;
  font-weight: 700;
  color: #94a3b8;
  letter-spacing: 0.05em;
}

.node-name {
  font-size: 14px;
  font-weight: 700;
  color: #0f172a;
}

.route-divider {
  font-size: 18px;
  color: #64748b;
}

.detail-section {
  margin-top: 24px;
}

.section-title {
  font-size: 14px;
  font-weight: 700;
  color: #0f172a;
  margin-bottom: 12px;
  text-transform: uppercase;
  letter-spacing: 0.03em;
}

.custom-timeline {
  padding-left: 4px;
}

.timeline-meta {
  font-size: 12px;
  color: #64748b;
  margin: 2px 0 0 0;
}

.detail-items-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.detail-item-card {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 12px 14px;
}

.it-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.it-qty-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 8px;
  background: #f8fafc;
  border-radius: 6px;
  padding: 8px 12px;
}

.qty-box {
  display: flex;
  flex-direction: column;
}

.qty-lbl {
  font-size: 10px;
  color: #64748b;
}

.qty-val {
  font-size: 14px;
}

.note-box {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 12px;
  font-size: 13px;
  color: #334155;
  margin: 0;
}

/* Utilities */
.cursor-pointer { cursor: pointer; }
.font-bold { font-weight: 700; }
.font-medium { font-weight: 500; }
.text-dark { color: #0f172a; }
.text-secondary { color: #64748b; }
.text-emerald { color: #059669; }
.text-rose { color: #e11d48; }
.text-amber { color: #d97706; }
.text-blue { color: #2563eb; }
.ml-1 { margin-left: 4px; }
.mt-1 { margin-top: 4px; }
.mt-3 { margin-top: 12px; }
.mb-3 { margin-bottom: 12px; }
</style>
