<template>
  <div class="promotions-container">
    <!-- Header Phân hệ -->
    <div class="promotions-header">
      <div class="header-left">
        <h1 class="page-title">
          <el-icon class="title-icon"><Present /></el-icon>
          Khuyến Mại, Voucher & Tích Điểm (Loyalty & Promotions)
        </h1>
        <p class="page-subtitle">
          Quản trị Voucher/Coupon, Bảng giá sỉ đa tầng theo số lượng & Động cơ tích điểm thăng hạng hội viên tự động.
        </p>
      </div>
      <div class="header-actions">
        <el-button v-if="activeTab === 'vouchers'" type="primary" @click="openCreateVoucherModal">
          <el-icon><Plus /></el-icon> Thêm Voucher Mới
        </el-button>
        <el-button v-if="activeTab === 'tiered'" type="primary" @click="openCreateTieredRuleModal">
          <el-icon><Plus /></el-icon> Thêm Quy Tắc Giá Sỉ
        </el-button>
        <el-button @click="refreshData">
          <el-icon><Refresh /></el-icon> Làm mới
        </el-button>
      </div>
    </div>

    <!-- Thẻ KPI Thống kê Tổng quan -->
    <el-row :gutter="16" class="stats-row">
      <el-col :xs="24" :sm="12" :md="8" :lg="4">
        <el-card shadow="never" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon-wrapper bg-blue-light text-blue">
              <el-icon><Ticket /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ overview.totalVouchers ?? 0 }}</div>
              <div class="stat-label">Tổng số Voucher</div>
              <div class="stat-sub text-blue">Đang chạy: {{ overview.activeVouchers ?? 0 }}</div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="12" :md="8" :lg="5">
        <el-card shadow="never" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon-wrapper bg-purple-light text-purple">
              <el-icon><Medal /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ overview.totalLoyaltyMembers ?? 0 }}</div>
              <div class="stat-label">Hội viên Khách hàng</div>
              <div class="stat-sub text-purple">4 Hạng thẻ tự động</div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="12" :md="8" :lg="5">
        <el-card shadow="never" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon-wrapper bg-amber-light text-amber">
              <el-icon><Coin /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ formatNumber(overview.totalPointsOutstanding ?? 0) }}</div>
              <div class="stat-label">Điểm Tích Lũy Chưa Dùng</div>
              <div class="stat-sub text-amber">&asymp; {{ formatCurrency((overview.totalPointsOutstanding ?? 0) * 1000) }}</div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="12" :md="8" :lg="5">
        <el-card shadow="never" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon-wrapper bg-emerald-light text-emerald">
              <el-icon><Money /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value font-bold text-emerald">{{ formatCurrency(overview.totalDiscountSponsored ?? 0) }}</div>
              <div class="stat-label">Khuyến Mại Đã Tài Trợ</div>
              <div class="stat-sub text-emerald">Trừ trực tiếp vào doanh thu</div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="12" :md="8" :lg="5">
        <el-card shadow="never" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon-wrapper bg-indigo-light text-indigo">
              <el-icon><Histogram /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ overview.totalTieredRules ?? 0 }}</div>
              <div class="stat-label">Quy Tắc Giá Sỉ Đa Tầng</div>
              <div class="stat-sub text-indigo">Tự động áp dụng theo SL</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- Các Tab Quản trị Nghiệp vụ -->
    <el-tabs v-model="activeTab" class="promotions-tabs" @tab-click="handleTabClick">
      
      <!-- ========================================================================= -->
      <!-- TAB 1: QUẢN LÝ MÃ VOUCHER / COUPON -->
      <!-- ========================================================================= -->
      <el-tab-pane label="🎟️ Mã Voucher & Coupon" name="vouchers">
        <div class="tab-content-wrapper">
          <!-- Thanh công cụ lọc Voucher -->
          <div class="toolbar-box">
            <div class="filter-group">
              <el-input
                v-model="voucherFilter.keyword"
                placeholder="Tìm mã voucher hoặc tên chương trình..."
                clearable
                style="width: 280px"
                @keyup.enter="loadVouchers"
                @clear="loadVouchers"
              >
                <template #prefix><el-icon><Search /></el-icon></template>
              </el-input>

              <el-select
                v-model="voucherFilter.active"
                placeholder="Trạng thái kích hoạt"
                clearable
                style="width: 180px"
                @change="loadVouchers"
              >
                <el-option label="Tất cả trạng thái" :value="null" />
                <el-option label="Đang kích hoạt" :value="true" />
                <el-option label="Đang tạm khóa" :value="false" />
              </el-select>

              <el-button type="primary" plain @click="loadVouchers">
                <el-icon><Filter /></el-icon> Lọc Voucher
              </el-button>
            </div>
          </div>

          <!-- Bảng Danh Sách Voucher -->
          <el-table
            v-loading="loadingVouchers"
            :data="vouchers"
            stripe
            border
            style="width: 100%"
            class="custom-table"
          >
            <el-table-column label="Mã Voucher" width="160">
              <template #default="{ row }">
                <div class="voucher-code-cell">
                  <span class="code-badge">{{ row.code }}</span>
                  <el-tooltip content="Sao chép mã" placement="top" effect="light">
                    <el-button link type="primary" size="small" @click="copyVoucherCode(row.code)">
                      <el-icon><CopyDocument /></el-icon>
                    </el-button>
                  </el-tooltip>
                </div>
              </template>
            </el-table-column>

            <el-table-column prop="name" label="Tên Chương Trình" min-width="200">
              <template #default="{ row }">
                <div class="font-medium text-slate-800">{{ row.name }}</div>
                <div v-if="row.description" class="text-xs text-slate-500 mt-0.5">{{ row.description }}</div>
              </template>
            </el-table-column>

            <el-table-column label="Mức Giảm Giá" width="180">
              <template #default="{ row }">
                <div v-if="row.discountType === 'PERCENTAGE'" class="font-bold text-blue">
                  Giảm {{ row.discountValue }}%
                  <div v-if="row.maxDiscountAmount" class="text-xs font-normal text-slate-500">
                    Tối đa {{ formatCurrency(row.maxDiscountAmount) }}
                  </div>
                </div>
                <div v-else class="font-bold text-emerald">
                  Giảm {{ formatCurrency(row.discountValue) }}
                </div>
              </template>
            </el-table-column>

            <el-table-column label="Đơn Tối Thiểu" width="150" align="right">
              <template #default="{ row }">
                <span>{{ formatCurrency(row.minOrderValue ?? 0) }}</span>
              </template>
            </el-table-column>

            <el-table-column label="Lượt Sử Dụng" width="150" align="center">
              <template #default="{ row }">
                <div>
                  <span class="font-bold text-slate-800">{{ row.usedCount ?? 0 }}</span>
                  <span class="text-slate-400"> / {{ row.maxUsage ? row.maxUsage : '∞' }}</span>
                </div>
                <div class="text-xs text-slate-400">Tối đa {{ row.maxUsagePerCustomer ?? 1 }} lần/khách</div>
              </template>
            </el-table-column>

            <el-table-column label="Thời Gian Hiệu Lực" width="190">
              <template #default="{ row }">
                <div class="text-xs text-slate-600">
                  <div><strong>Từ:</strong> {{ formatDate(row.startDate) || 'Ngay lập tức' }}</div>
                  <div><strong>Đến:</strong> {{ formatDate(row.endDate) || 'Không thời hạn' }}</div>
                </div>
              </template>
            </el-table-column>

            <el-table-column label="Trạng Thái" width="130" align="center">
              <template #default="{ row }">
                <el-tag v-if="!row.active" type="info" size="small">Tạm khóa</el-tag>
                <el-tag v-else-if="row.isExpired" type="danger" size="small">Hết hạn / Hết lượt</el-tag>
                <el-tag v-else type="success" size="small">Đang chạy</el-tag>
              </template>
            </el-table-column>

            <el-table-column label="Thao Tác" width="150" fixed="right" align="center">
              <template #default="{ row }">
                <el-tooltip :content="row.active ? 'Khóa voucher' : 'Mở kích hoạt'" placement="top" effect="light">
                  <el-button 
                    link 
                    :type="row.active ? 'warning' : 'success'" 
                    size="small" 
                    @click="toggleActiveVoucher(row)"
                  >
                    <el-icon><SwitchButton /></el-icon>
                  </el-button>
                </el-tooltip>
                <el-button link type="primary" size="small" @click="openEditVoucherModal(row)">
                  <el-icon><Edit /></el-icon> Sửa
                </el-button>
                <el-button link type="danger" size="small" @click="deleteVoucher(row)">
                  <el-icon><Delete /></el-icon>
                </el-button>
              </template>
            </el-table-column>
          </el-table>

          <!-- Phân trang Voucher -->
          <div class="pagination-wrapper">
            <el-pagination
              v-model:current-page="voucherPagination.page"
              v-model:page-size="voucherPagination.size"
              :total="voucherPagination.total"
              :page-sizes="[10, 15, 30, 50]"
              layout="total, sizes, prev, pager, next, jumper"
              @size-change="loadVouchers"
              @current-change="loadVouchers"
            />
          </div>
        </div>
      </el-tab-pane>

      <!-- ========================================================================= -->
      <!-- TAB 2: HẠNG THÀNH VIÊN & TÍCH ĐIỂM (LOYALTY) -->
      <!-- ========================================================================= -->
      <el-tab-pane label="👑 Hạng Hội Viên & Tích Điểm (Loyalty)" name="loyalty">
        <div class="tab-content-wrapper">
          <!-- Bảng 4 Hạng Thành Viên Chuẩn -->
          <div class="tiers-summary-grid">
            <div class="tier-card tier-bronze">
              <div class="tier-badge-icon">🥉</div>
              <div class="tier-info">
                <div class="tier-name">Hội Viên Đồng (BRONZE)</div>
                <div class="tier-spend">Chi tiêu: 0 &dash; 5.000.000đ</div>
                <div class="tier-benefit">⚡ Tỷ lệ tích điểm: <strong>1.0x</strong> (100k = 1 điểm)</div>
              </div>
            </div>

            <div class="tier-card tier-silver">
              <div class="tier-badge-icon">🥈</div>
              <div class="tier-info">
                <div class="tier-name">Hội Viên Bạc (SILVER)</div>
                <div class="tier-spend">Chi tiêu: Từ 5.000.000đ</div>
                <div class="tier-benefit">⚡ Tỷ lệ tích điểm: <strong>1.2x</strong> (Tặng thêm 20%)</div>
              </div>
            </div>

            <div class="tier-card tier-gold">
              <div class="tier-badge-icon">🥇</div>
              <div class="tier-info">
                <div class="tier-name">Hội Viên Vàng (GOLD)</div>
                <div class="tier-spend">Chi tiêu: Từ 20.000.000đ</div>
                <div class="tier-benefit">⚡ Tỷ lệ tích điểm: <strong>1.5x</strong> (Tặng thêm 50%)</div>
              </div>
            </div>

            <div class="tier-card tier-diamond">
              <div class="tier-badge-icon">💎</div>
              <div class="tier-info">
                <div class="tier-name">Hội Viên Kim Cương (DIAMOND)</div>
                <div class="tier-spend">Chi tiêu: Từ 50.000.000đ</div>
                <div class="tier-benefit">⚡ Tỷ lệ tích điểm: <strong>2.0x</strong> (Gấp đôi điểm thưởng)</div>
              </div>
            </div>
          </div>

          <!-- Thanh tìm kiếm Hội viên -->
          <div class="toolbar-box mt-3">
            <div class="filter-group">
              <el-input
                v-model="loyaltyFilter.keyword"
                placeholder="Tìm tên hoặc số điện thoại khách hàng..."
                clearable
                style="width: 320px"
                @keyup.enter="loadLoyaltyMembers"
                @clear="loadLoyaltyMembers"
              >
                <template #prefix><el-icon><Search /></el-icon></template>
              </el-input>

              <el-button type="primary" plain @click="loadLoyaltyMembers">
                <el-icon><Search /></el-icon> Tìm Hội Viên
              </el-button>
            </div>
          </div>

          <!-- Bảng Danh Sách Hội Viên & Điểm Tích Lũy -->
          <el-table
            v-loading="loadingLoyalty"
            :data="loyaltyMembers"
            stripe
            border
            style="width: 100%"
            class="custom-table"
          >
            <el-table-column label="Khách Hàng" min-width="200">
              <template #default="{ row }">
                <div class="font-semibold text-slate-800">{{ row.customerName }}</div>
                <div class="text-xs text-slate-500">📞 {{ row.customerPhone }}</div>
              </template>
            </el-table-column>

            <el-table-column label="Hạng Hội Viên" width="180" align="center">
              <template #default="{ row }">
                <el-tag :type="getTierTagType(row.memberTier)" effect="dark" class="font-bold">
                  {{ getTierIcon(row.memberTier) }} {{ row.tierDisplayName }}
                </el-tag>
                <div class="text-xs text-slate-400 mt-1">Hệ số: {{ row.pointMultiplier }}x</div>
              </template>
            </el-table-column>

            <el-table-column label="Điểm Tích Lũy Khả Dụng" width="200" align="right">
              <template #default="{ row }">
                <div class="font-bold text-amber-dark text-base">
                  {{ formatNumber(row.loyaltyPoints ?? 0) }} Điểm
                </div>
                <div class="text-xs text-slate-500">
                  &asymp; {{ formatCurrency(row.pointsValueVnd ?? 0) }} khấu trừ
                </div>
              </template>
            </el-table-column>

            <el-table-column label="Tổng Chi Tiêu Trọn Đời" width="190" align="right">
              <template #default="{ row }">
                <div class="font-medium text-slate-800">{{ formatCurrency(row.totalSpent ?? 0) }}</div>
                <div class="text-xs text-slate-400">Đơn hoàn tất</div>
              </template>
            </el-table-column>

            <el-table-column label="Thao Tác" width="220" fixed="right" align="center">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click="viewPointTransactions(row)">
                  <el-icon><Tickets /></el-icon> Xem Lịch Sử
                </el-button>
                <el-button link type="warning" size="small" @click="openAdjustPointsModal(row)">
                  <el-icon><EditPen /></el-icon> Chỉnh Điểm
                </el-button>
              </template>
            </el-table-column>
          </el-table>

          <!-- Phân trang Hội viên -->
          <div class="pagination-wrapper">
            <el-pagination
              v-model:current-page="loyaltyPagination.page"
              v-model:page-size="loyaltyPagination.size"
              :total="loyaltyPagination.total"
              :page-sizes="[10, 15, 30, 50]"
              layout="total, sizes, prev, pager, next, jumper"
              @size-change="loadLoyaltyMembers"
              @current-change="loadLoyaltyMembers"
            />
          </div>
        </div>
      </el-tab-pane>

      <!-- ========================================================================= -->
      <!-- TAB 3: BẢNG GIÁ SỈ ĐA TẦNG (TIERED PRICING) -->
      <!-- ========================================================================= -->
      <el-tab-pane label="📊 Bảng Giá Sỉ Đa Tầng (Tiered Pricing)" name="tiered">
        <div class="tab-content-wrapper">
          <div class="toolbar-box">
            <div class="text-sm text-slate-600">
              💡 <strong>Cơ chế Giá Sỉ Bậc Thang:</strong> Hệ thống tự động phát hiện số lượng mua của từng mặt hàng và giảm giá tương ứng mà nhân viên bán hàng không cần tính toán thủ công.
            </div>
          </div>

          <!-- Bảng Quy Tắc Giá Sỉ -->
          <el-table
            v-loading="loadingTiered"
            :data="tieredRules"
            stripe
            border
            style="width: 100%"
            class="custom-table"
          >
            <el-table-column prop="tierName" label="Tên Bậc Giá Sỉ" min-width="180">
              <template #default="{ row }">
                <div class="font-semibold text-slate-800">{{ row.tierName }}</div>
              </template>
            </el-table-column>

            <el-table-column label="Sản Phẩm Áp Dụng" min-width="220">
              <template #default="{ row }">
                <div v-if="row.productId" class="font-medium text-slate-700">
                  {{ row.productName }}
                  <div class="text-xs text-slate-400">SKU: {{ row.productSku }}</div>
                </div>
                <div v-else>
                  <el-tag type="info" size="small">Áp dụng chung mọi sản phẩm</el-tag>
                </div>
              </template>
            </el-table-column>

            <el-table-column label="Số Lượng Bậc Thang" width="180" align="center">
              <template #default="{ row }">
                <div class="font-bold text-slate-800">
                  {{ row.minQuantity }} &dash; {{ row.maxQuantity ? row.maxQuantity : 'Trở lên' }}
                </div>
                <div class="text-xs text-slate-400">đơn vị tính cơ sở</div>
              </template>
            </el-table-column>

            <el-table-column label="Mức Giá / Chiết Khấu" width="200">
              <template #default="{ row }">
                <div v-if="row.pricingType === 'PERCENT_DISCOUNT'" class="font-bold text-blue">
                  Giảm {{ row.discountPercent }}% so với giá lẻ
                </div>
                <div v-else class="font-bold text-emerald">
                  Giá sỉ cố định: {{ formatCurrency(row.fixedPrice) }}/cái
                </div>
              </template>
            </el-table-column>

            <el-table-column label="Trạng Thái" width="120" align="center">
              <template #default="{ row }">
                <el-tag :type="row.active ? 'success' : 'info'" size="small">
                  {{ row.active ? 'Đang áp dụng' : 'Tắt' }}
                </el-tag>
              </template>
            </el-table-column>

            <el-table-column label="Thao Tác" width="150" fixed="right" align="center">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click="openEditTieredModal(row)">
                  <el-icon><Edit /></el-icon> Sửa
                </el-button>
                <el-button link type="danger" size="small" @click="deleteTieredRule(row)">
                  <el-icon><Delete /></el-icon> Xóa
                </el-button>
              </template>
            </el-table-column>
          </el-table>

          <!-- Phân trang Giá Sỉ -->
          <div class="pagination-wrapper">
            <el-pagination
              v-model:current-page="tieredPagination.page"
              v-model:page-size="tieredPagination.size"
              :total="tieredPagination.total"
              :page-sizes="[10, 20, 50]"
              layout="total, sizes, prev, pager, next, jumper"
              @size-change="loadTieredRules"
              @current-change="loadTieredRules"
            />
          </div>
        </div>
      </el-tab-pane>

      <!-- ========================================================================= -->
      <!-- TAB 4: CÔNG CỤ MÔ PHỎNG GIỎ HÀNG (PROMOTION SIMULATOR) -->
      <!-- ========================================================================= -->
      <el-tab-pane label="🧮 Mô Phỏng Giỏ Hàng (Promotion Simulator)" name="simulator">
        <div class="tab-content-wrapper">
          <el-card shadow="never" class="simulator-card">
            <div class="simulator-header">
              <div class="font-bold text-base text-slate-800">
                ⚡ Thử Nghiệm Phối Hợp Động Cơ: Giá Sỉ + Voucher + Điểm Tích Lũy
              </div>
              <div class="text-xs text-slate-500 mt-1">
                Nhập tham số giỏ hàng để kiểm tra tính minh bạch và công thức khấu trừ của Promotion Engine.
              </div>
            </div>

            <div class="simulator-form mt-4">
              <el-row :gutter="16">
                <el-col :span="12">
                  <el-form-item label="Khách hàng mô phỏng:">
                    <el-select
                      v-model="simCustomer"
                      placeholder="Chọn khách hàng để nạp điểm và hạng thẻ..."
                      filterable
                      style="width: 100%"
                      @change="onSimCustomerChange"
                    >
                      <el-option
                        v-for="c in sampleCustomers"
                        :key="c.id"
                        :label="`${c.name} (${c.phone}) - Hạng: ${c.memberTier} - Có: ${c.loyaltyPoints} điểm`"
                        :value="c"
                      />
                    </el-select>
                  </el-form-item>
                </el-col>

                <el-col :span="6">
                  <el-form-item label="Mã Voucher thử nghiệm:">
                    <el-input v-model="simVoucherCode" placeholder="vd: VIP10, GIAM50K..." clearable />
                  </el-form-item>
                </el-col>

                <el-col :span="6">
                  <el-form-item label="Điểm muốn dùng (1đ = 1.000đ):">
                    <el-input-number 
                      v-model="simPointsToUse" 
                      :min="0" 
                      :max="simCustomer?.loyaltyPoints || 0" 
                      style="width: 100%" 
                    />
                  </el-form-item>
                </el-col>
              </el-row>

              <!-- Danh sách mặt hàng mua thử -->
              <div class="sim-items-header mt-2 mb-2 font-semibold text-slate-700">
                📦 Mặt Hàng Trong Giỏ Hàng:
                <el-button link type="primary" size="small" class="ml-2" @click="addSimItem">
                  <el-icon><Plus /></el-icon> Thêm mặt hàng
                </el-button>
              </div>

              <div v-for="(item, idx) in simItems" :key="idx" class="sim-item-row mb-2">
                <el-select
                  v-model="item.productId"
                  placeholder="Chọn sản phẩm..."
                  style="width: 380px"
                  @change="onSimItemProductChange(item)"
                >
                  <el-option
                    v-for="p in allProducts"
                    :key="p.id"
                    :label="`${p.name} (SKU: ${p.sku}) - ${formatCurrency(p.price)}`"
                    :value="p.id"
                  />
                </el-select>

                <el-input-number
                  v-model="item.quantity"
                  :min="1"
                  :max="1000"
                  class="ml-2"
                  placeholder="Số lượng"
                  style="width: 140px"
                />

                <span class="ml-3 text-sm text-slate-500">
                  Đơn giá cơ sở: <strong>{{ formatCurrency(item.baseUnitPrice || 0) }}</strong>
                </span>

                <el-button link type="danger" size="small" class="ml-auto" @click="removeSimItem(idx)">
                  <el-icon><Delete /></el-icon>
                </el-button>
              </div>

              <div class="mt-4">
                <el-button type="success" size="large" :loading="calculatingSim" @click="runSimulation">
                  <el-icon><CaretRight /></el-icon> Chạy Mô Phỏng Tính Toán Ngay
                </el-button>
              </div>

              <!-- Kết quả mô phỏng -->
              <div v-if="simResult" class="sim-result-box mt-4">
                <div class="font-bold text-emerald text-base mb-3">
                  🎉 Kết Quả Tính Toán Từ Promotion Engine:
                </div>

                <div class="receipt-breakdown">
                  <div class="receipt-row">
                    <span>Tổng tiền giá gốc (Chưa chiết khấu):</span>
                    <strong>{{ formatCurrency(simResult.originalSubtotal) }}</strong>
                  </div>
                  <div class="receipt-row text-blue">
                    <span>Chiết khấu Giá sỉ bậc thang (Tiered Discount):</span>
                    <strong>-{{ formatCurrency(simResult.tieredDiscountTotal) }}</strong>
                  </div>
                  <div class="receipt-row">
                    <span>Tạm tính sau giá sỉ:</span>
                    <strong>{{ formatCurrency(simResult.subtotalAfterTiered) }}</strong>
                  </div>
                  <div class="receipt-row text-purple">
                    <span>Khấu trừ Mã Voucher ({{ simResult.appliedVoucherCode || 'Không áp dụng' }}):</span>
                    <strong>-{{ formatCurrency(simResult.voucherDiscount) }}</strong>
                  </div>
                  <div v-if="simResult.voucherMessage" class="text-xs text-slate-500 text-right mb-1">
                    ({{ simResult.voucherMessage }})
                  </div>
                  <div class="receipt-row text-amber-dark">
                    <span>Khấu trừ Điểm thưởng ({{ simResult.loyaltyPointsRedeemed }} điểm):</span>
                    <strong>-{{ formatCurrency(simResult.loyaltyDiscount) }}</strong>
                  </div>
                  <div class="receipt-divider"></div>
                  <div class="receipt-row font-bold text-lg text-emerald">
                    <span>TỔNG TIỀN THANH TOÁN:</span>
                    <span>{{ formatCurrency(simResult.finalTotal) }}</span>
                  </div>
                  <div class="receipt-row text-xs text-slate-500 mt-2">
                    <span>Điểm tích lũy dự kiến nhận được khi hoàn tất đơn:</span>
                    <strong class="text-amber-dark">+{{ simResult.estimatedPointsEarned }} Điểm</strong>
                  </div>
                </div>
              </div>
            </div>
          </el-card>
        </div>
      </el-tab-pane>

    </el-tabs>

    <!-- ========================================================================= -->
    <!-- MODAL 1: THÊM / SỬA VOUCHER -->
    <!-- ========================================================================= -->
    <el-dialog
      v-model="showVoucherModal"
      :title="editingVoucherId ? 'Chỉnh Sửa Mã Voucher Khuyến Mại' : 'Tạo Mã Voucher Khuyến Mại Mới'"
      width="600px"
      destroy-on-close
    >
      <el-form :model="voucherForm" label-position="top">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="Mã Voucher (In hoa, không dấu) *" required>
              <el-input v-model="voucherForm.code" placeholder="vd: SALE2026, VIP10" style="text-transform: uppercase" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="Tên Chương Trình *" required>
              <el-input v-model="voucherForm.name" placeholder="vd: Khuyến mại Black Friday" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="Mô Tả Điều Kiện Áp Dụng">
          <el-input v-model="voucherForm.description" type="textarea" :rows="2" placeholder="Ghi chú chi tiết điều kiện..." />
        </el-form-item>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="Loại Chiết Khấu *" required>
              <el-select v-model="voucherForm.discountType" style="width: 100%">
                <el-option label="Giảm theo % (PERCENTAGE)" value="PERCENTAGE" />
                <el-option label="Giảm số tiền cố định (FIXED_AMOUNT)" value="FIXED_AMOUNT" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="voucherForm.discountType === 'PERCENTAGE' ? 'Giá Trị Giảm (%) *' : 'Số Tiền Giảm (VNĐ) *'" required>
              <el-input-number v-model="voucherForm.discountValue" :min="0" :max="voucherForm.discountType === 'PERCENTAGE' ? 100 : 100000000" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="Giảm Tối Đa (Nếu giảm %)">
              <el-input-number v-model="voucherForm.maxDiscountAmount" :min="0" placeholder="Không giới hạn" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="Đơn Hàng Tối Thiểu (VNĐ)">
              <el-input-number v-model="voucherForm.minOrderValue" :min="0" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="Tổng Lượt Dùng Toàn Hệ Thống">
              <el-input-number v-model="voucherForm.maxUsage" :min="1" placeholder="Không giới hạn" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="Lượt Dùng Tối Đa / Mỗi Khách">
              <el-input-number v-model="voucherForm.maxUsagePerCustomer" :min="1" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="Ngày Bắt Đầu">
              <el-date-picker v-model="voucherForm.startDate" type="datetime" placeholder="Chọn ngày bắt đầu" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="Ngày Kết Thúc (Hết Hạn)">
              <el-date-picker v-model="voucherForm.endDate" type="datetime" placeholder="Chọn ngày hết hạn" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="Trạng Thái">
          <el-switch v-model="voucherForm.active" active-text="Kích hoạt áp dụng ngay" inactive-text="Tạm khóa" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="showVoucherModal = false">Hủy</el-button>
        <el-button type="primary" :loading="submittingVoucher" @click="submitVoucherForm">
          {{ editingVoucherId ? 'Cập Nhật Voucher' : 'Tạo Voucher' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- ========================================================================= -->
    <!-- MODAL 2: ĐIỀU CHỈNH ĐIỂM HỘI VIÊN THỦ CÔNG (ADMIN) -->
    <!-- ========================================================================= -->
    <el-dialog
      v-model="showAdjustPointsModal"
      title="Điều Chỉnh Điểm Thưởng Hội Viên (Thủ Công)"
      width="500px"
      destroy-on-close
    >
      <div v-if="targetCustomerForPoints" class="mb-3">
        <p><strong>Khách hàng:</strong> {{ targetCustomerForPoints.customerName }} ({{ targetCustomerForPoints.customerPhone }})</p>
        <p><strong>Hạng hội viên:</strong> {{ targetCustomerForPoints.tierDisplayName }}</p>
        <p><strong>Điểm hiện tại:</strong> <span class="font-bold text-amber-dark">{{ targetCustomerForPoints.loyaltyPoints }}</span> điểm</p>
      </div>

      <el-form :model="adjustPointsForm" label-position="top">
        <el-form-item label="Số Điểm Biến Động (Dương: Cộng thêm, Âm: Trừ bớt) *" required>
          <el-input-number v-model="adjustPointsForm.points" :step="10" style="width: 100%" />
        </el-form-item>

        <el-form-item label="Lý Do Điều Chỉnh *" required>
          <el-input v-model="adjustPointsForm.reason" placeholder="vd: Thưởng sinh nhật khách hàng, Bù điểm khiếu nại..." />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="showAdjustPointsModal = false">Hủy</el-button>
        <el-button type="warning" :loading="submittingPoints" @click="submitAdjustPoints">
          Xác Nhận Điều Chỉnh Điểm
        </el-button>
      </template>
    </el-dialog>

    <!-- ========================================================================= -->
    <!-- DRAWER: LỊCH SỬ BIẾN ĐỘNG ĐIỂM -->
    <!-- ========================================================================= -->
    <el-drawer
      v-model="showTransactionsDrawer"
      title="Lịch Sử Biến Động Điểm Thưởng"
      size="480px"
    >
      <div v-if="loadingTransactions" class="p-4 text-center">
        <el-icon class="is-loading"><Loading /></el-icon> Đang tải lịch sử...
      </div>
      <div v-else-if="pointTransactions.length === 0" class="text-slate-400 text-center p-4">
        Khách hàng chưa có giao dịch tích/dùng điểm nào!
      </div>
      <div v-else class="transactions-list">
        <div v-for="tx in pointTransactions" :key="tx.id" class="tx-card mb-3">
          <div class="tx-header">
            <el-tag :type="getTxTagType(tx.type)" size="small">
              {{ tx.type }}
            </el-tag>
            <span class="font-bold text-sm" :class="tx.points > 0 ? 'text-emerald' : 'text-red'">
              {{ tx.points > 0 ? '+' : '' }}{{ tx.points }} Điểm
            </span>
          </div>
          <div class="tx-reason text-xs text-slate-600 mt-1">{{ tx.reason }}</div>
          <div class="tx-footer text-xs text-slate-400 mt-1">
            <span>Số dư sau: {{ tx.balanceAfter }} điểm</span>
            <span class="ml-auto">{{ formatDateTime(tx.createdAt) }}</span>
          </div>
        </div>
      </div>
    </el-drawer>

    <!-- ========================================================================= -->
    <!-- MODAL 3: THÊM / SỬA QUY TẮC GIÁ SỈ ĐA TẦNG -->
    <!-- ========================================================================= -->
    <el-dialog
      v-model="showTieredModal"
      :title="editingTieredId ? 'Chỉnh Sửa Quy Tắc Giá Sỉ' : 'Tạo Quy Tắc Giá Sỉ Đa Tầng Mới'"
      width="550px"
      destroy-on-close
    >
      <el-form :model="tieredForm" label-position="top">
        <el-form-item label="Tên Bậc Giá Sỉ *" required>
          <el-input v-model="tieredForm.tierName" placeholder="vd: Sỉ Bậc 1 (Mua từ 10 - 49 cái)" />
        </el-form-item>

        <el-form-item label="Sản Phẩm Áp Dụng (Để trống nếu áp dụng mọi sản phẩm)">
          <el-select
            v-model="tieredForm.productId"
            placeholder="Tất cả sản phẩm (Áp dụng chung)"
            clearable
            filterable
            style="width: 100%"
          >
            <el-option
              v-for="p in allProducts"
              :key="p.id"
              :label="`${p.name} (SKU: ${p.sku})`"
              :value="p.id"
            />
          </el-select>
        </el-form-item>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="Số Lượng Mua Tối Thiểu (Min Qty) *" required>
              <el-input-number v-model="tieredForm.minQuantity" :min="1" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="Số Lượng Mua Tối Đa (Max Qty)">
              <el-input-number v-model="tieredForm.maxQuantity" :min="1" placeholder="Không giới hạn" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="Hình Thức Định Giá *" required>
              <el-select v-model="tieredForm.pricingType" style="width: 100%">
                <el-option label="Giảm theo % giá lẻ" value="PERCENT_DISCOUNT" />
                <el-option label="Đơn giá sỉ cố định" value="FIXED_PRICE" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="tieredForm.pricingType === 'PERCENT_DISCOUNT' ? '% Giảm Giá *' : 'Đơn Giá Sỉ (VNĐ) *'" required>
              <el-input-number 
                v-model="tieredForm.discountPercent" 
                v-if="tieredForm.pricingType === 'PERCENT_DISCOUNT'"
                :min="0" 
                :max="100" 
                style="width: 100%" 
              />
              <el-input-number 
                v-model="tieredForm.fixedPrice" 
                v-else
                :min="0" 
                style="width: 100%" 
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="Trạng Thái">
          <el-switch v-model="tieredForm.active" active-text="Đang áp dụng" inactive-text="Tạm dừng" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="showTieredModal = false">Hủy</el-button>
        <el-button type="primary" :loading="submittingTiered" @click="submitTieredForm">
          {{ editingTieredId ? 'Cập Nhật Quy Tắc' : 'Tạo Quy Tắc' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
// =============================================================================
// 1. HELPER FUNCTIONS ĐẶT LÊN ĐẦU TRÁNH TEMPORAL DEAD ZONE
// =============================================================================
const formatCurrency = (val) => {
  const num = Number(val) ?? 0
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(num)
}

const formatNumber = (val) => {
  const num = Number(val) ?? 0
  return new Intl.NumberFormat('vi-VN').format(num)
}

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  return d.toLocaleDateString('vi-VN')
}

const formatDateTime = (dateStr) => {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  return d.toLocaleString('vi-VN')
}

const getTierTagType = (tier) => {
  switch (tier) {
    case 'DIAMOND': return 'danger'
    case 'GOLD': return 'warning'
    case 'SILVER': return 'info'
    default: return ''
  }
}

const getTierIcon = (tier) => {
  switch (tier) {
    case 'DIAMOND': return '💎'
    case 'GOLD': return '🥇'
    case 'SILVER': return '🥈'
    default: return '🥉'
  }
}

const getTxTagType = (type) => {
  switch (type) {
    case 'EARN': return 'success'
    case 'REDEEM': return 'warning'
    case 'REFUND': return 'info'
    case 'ADJUST': return 'primary'
    default: return ''
  }
}

// =============================================================================
// 2. IMPORTS & STATE
// =============================================================================
import { ref, reactive, onMounted } from 'vue'
import api from '../api'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Present, Plus, Refresh, Ticket, Medal, Coin, Money, Histogram,
  Search, Filter, CopyDocument, SwitchButton, Edit, Delete, Tickets, EditPen,
  CaretRight, Loading
} from '@element-plus/icons-vue'

const activeTab = ref('vouchers')

// KPI Overview
const overview = ref({
  totalVouchers: 0,
  activeVouchers: 0,
  totalDiscountSponsored: 0,
  totalLoyaltyMembers: 0,
  totalPointsOutstanding: 0,
  totalTieredRules: 0
})

const loadOverview = async () => {
  try {
    const res = await api.get('/promotions/overview')
    overview.value = res.data
  } catch (e) {
    console.error('Error loadOverview:', e)
  }
}

// =============================================================================
// 3. TAB 1: VOUCHERS STATE & METHODS
// =============================================================================
const loadingVouchers = ref(false)
const vouchers = ref([])
const voucherFilter = reactive({
  keyword: '',
  active: null
})
const voucherPagination = reactive({
  page: 1,
  size: 15,
  total: 0
})

const loadVouchers = async () => {
  loadingVouchers.value = true
  try {
    const params = {
      keyword: voucherFilter.keyword || undefined,
      active: voucherFilter.active !== null ? voucherFilter.active : undefined,
      page: voucherPagination.page - 1,
      size: voucherPagination.size
    }
    const res = await api.get('/promotions/vouchers', { params })
    if (res.data) {
      vouchers.value = res.data.content || []
      voucherPagination.total = res.data.totalElements || 0
    }
  } catch (err) {
    ElMessage.error('Lỗi khi tải danh sách voucher!')
    console.error(err)
  } finally {
    loadingVouchers.value = false
  }
}

const copyVoucherCode = async (code) => {
  try {
    await navigator.clipboard.writeText(code)
    ElMessage.success(`Đã sao chép mã '${code}' vào bộ nhớ tạm!`)
  } catch (e) {
    ElMessage.info(`Mã voucher: ${code}`)
  }
}

const toggleActiveVoucher = async (voucher) => {
  try {
    await api.patch(`/promotions/vouchers/${voucher.id}/toggle-active`)
    ElMessage.success(`Đã ${voucher.active ? 'tạm khóa' : 'kích hoạt'} voucher ${voucher.code}!`)
    loadVouchers()
    loadOverview()
  } catch (e) {
    ElMessage.error('Lỗi khi thay đổi trạng thái voucher!')
  }
}

const deleteVoucher = async (voucher) => {
  try {
    await ElMessageBox.confirm(`Bạn có chắc chắn muốn xóa vĩnh viễn voucher '${voucher.code}'?`, 'Xác nhận xóa', {
      type: 'warning',
      confirmButtonText: 'Xóa ngay',
      cancelButtonText: 'Hủy'
    })
    await api.delete(`/promotions/vouchers/${voucher.id}`)
    ElMessage.success('Đã xóa voucher thành công!')
    loadVouchers()
    loadOverview()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('Lỗi khi xóa voucher!')
  }
}

// Modal Thêm/Sửa Voucher
const showVoucherModal = ref(false)
const editingVoucherId = ref(null)
const submittingVoucher = ref(false)
const voucherForm = reactive({
  code: '',
  name: '',
  description: '',
  discountType: 'PERCENTAGE',
  discountValue: 10,
  maxDiscountAmount: null,
  minOrderValue: 0,
  maxUsage: null,
  maxUsagePerCustomer: 1,
  startDate: null,
  endDate: null,
  active: true
})

const openCreateVoucherModal = () => {
  editingVoucherId.value = null
  Object.assign(voucherForm, {
    code: '',
    name: '',
    description: '',
    discountType: 'PERCENTAGE',
    discountValue: 10,
    maxDiscountAmount: null,
    minOrderValue: 0,
    maxUsage: null,
    maxUsagePerCustomer: 1,
    startDate: null,
    endDate: null,
    active: true
  })
  showVoucherModal.value = true
}

const openEditVoucherModal = (v) => {
  editingVoucherId.value = v.id
  Object.assign(voucherForm, {
    code: v.code,
    name: v.name,
    description: v.description,
    discountType: v.discountType,
    discountValue: v.discountValue,
    maxDiscountAmount: v.maxDiscountAmount,
    minOrderValue: v.minOrderValue,
    maxUsage: v.maxUsage,
    maxUsagePerCustomer: v.maxUsagePerCustomer,
    startDate: v.startDate,
    endDate: v.endDate,
    active: v.active
  })
  showVoucherModal.value = true
}

const submitVoucherForm = async () => {
  if (!voucherForm.code || !voucherForm.name) {
    ElMessage.warning('Vui lòng điền mã voucher và tên chương trình!')
    return
  }
  submittingVoucher.value = true
  try {
    if (editingVoucherId.value) {
      await api.put(`/promotions/vouchers/${editingVoucherId.value}`, voucherForm)
      ElMessage.success('Cập nhật voucher thành công!')
    } else {
      await api.post('/promotions/vouchers', voucherForm)
      ElMessage.success('Tạo voucher mới thành công!')
    }
    showVoucherModal.value = false
    loadVouchers()
    loadOverview()
  } catch (err) {
    ElMessage.error(err.response?.data?.message || 'Lỗi khi lưu voucher!')
  } finally {
    submittingVoucher.value = false
  }
}

// =============================================================================
// 4. TAB 2: LOYALTY MEMBERS STATE & METHODS
// =============================================================================
const loadingLoyalty = ref(false)
const loyaltyMembers = ref([])
const loyaltyFilter = reactive({ keyword: '' })
const loyaltyPagination = reactive({
  page: 1,
  size: 15,
  total: 0
})

const loadLoyaltyMembers = async () => {
  loadingLoyalty.value = true
  try {
    const params = {
      keyword: loyaltyFilter.keyword || undefined,
      page: loyaltyPagination.page - 1,
      size: loyaltyPagination.size
    }
    const res = await api.get('/promotions/loyalty/members', { params })
    if (res.data) {
      loyaltyMembers.value = res.data.content || []
      loyaltyPagination.total = res.data.totalElements || 0
    }
  } catch (err) {
    ElMessage.error('Lỗi khi tải danh sách hội viên!')
  } finally {
    loadingLoyalty.value = false
  }
}

// Drawer Xem Lịch Sử Điểm
const showTransactionsDrawer = ref(false)
const loadingTransactions = ref(false)
const pointTransactions = ref([])

const viewPointTransactions = async (member) => {
  showTransactionsDrawer.value = true
  loadingTransactions.value = true
  try {
    const res = await api.get(`/promotions/loyalty/members/${member.customerId}/transactions`, {
      params: { size: 50 }
    })
    pointTransactions.value = res.data?.content || []
  } catch (e) {
    ElMessage.error('Lỗi khi tải lịch sử điểm!')
  } finally {
    loadingTransactions.value = false
  }
}

// Modal Điều Chỉnh Điểm Thủ Công
const showAdjustPointsModal = ref(false)
const targetCustomerForPoints = ref(null)
const submittingPoints = ref(false)
const adjustPointsForm = reactive({
  customerId: null,
  points: 10,
  reason: ''
})

const openAdjustPointsModal = (member) => {
  targetCustomerForPoints.value = member
  adjustPointsForm.customerId = member.customerId
  adjustPointsForm.points = 10
  adjustPointsForm.reason = 'Thưởng điểm hội viên thân thiết'
  showAdjustPointsModal.value = true
}

const submitAdjustPoints = async () => {
  if (!adjustPointsForm.reason) {
    ElMessage.warning('Vui lòng ghi rõ lý do điều chỉnh điểm!')
    return
  }
  submittingPoints.value = true
  try {
    await api.post('/promotions/loyalty/adjust-points', adjustPointsForm)
    ElMessage.success('Điều chỉnh điểm hội viên thành công!')
    showAdjustPointsModal.value = false
    loadLoyaltyMembers()
    loadOverview()
  } catch (err) {
    ElMessage.error(err.response?.data?.message || 'Lỗi khi điều chỉnh điểm!')
  } finally {
    submittingPoints.value = false
  }
}

// =============================================================================
// 5. TAB 3: TIERED PRICING STATE & METHODS
// =============================================================================
const loadingTiered = ref(false)
const tieredRules = ref([])
const tieredPagination = reactive({
  page: 1,
  size: 20,
  total: 0
})

const loadTieredRules = async () => {
  loadingTiered.value = true
  try {
    const params = {
      page: tieredPagination.page - 1,
      size: tieredPagination.size
    }
    const res = await api.get('/promotions/tiered-rules', { params })
    if (res.data) {
      tieredRules.value = res.data.content || []
      tieredPagination.total = res.data.totalElements || 0
    }
  } catch (err) {
    ElMessage.error('Lỗi khi tải bảng giá sỉ đa tầng!')
  } finally {
    loadingTiered.value = false
  }
}

const showTieredModal = ref(false)
const editingTieredId = ref(null)
const submittingTiered = ref(false)
const tieredForm = reactive({
  tierName: '',
  productId: null,
  minQuantity: 10,
  maxQuantity: null,
  pricingType: 'PERCENT_DISCOUNT',
  discountPercent: 10,
  fixedPrice: null,
  active: true
})

const allProducts = ref([])
const loadProducts = async () => {
  try {
    const res = await api.get('/products', { params: { size: 100 } })
    allProducts.value = res.data?.content || []
  } catch (e) {
    console.error('Error loadProducts:', e)
  }
}

const openCreateTieredRuleModal = async () => {
  if (allProducts.value.length === 0) await loadProducts()
  editingTieredId.value = null
  Object.assign(tieredForm, {
    tierName: '',
    productId: null,
    minQuantity: 10,
    maxQuantity: null,
    pricingType: 'PERCENT_DISCOUNT',
    discountPercent: 10,
    fixedPrice: null,
    active: true
  })
  showTieredModal.value = true
}

const openEditTieredModal = async (r) => {
  if (allProducts.value.length === 0) await loadProducts()
  editingTieredId.value = r.id
  Object.assign(tieredForm, {
    tierName: r.tierName,
    productId: r.productId,
    minQuantity: r.minQuantity,
    maxQuantity: r.maxQuantity,
    pricingType: r.pricingType,
    discountPercent: r.discountPercent,
    fixedPrice: r.fixedPrice,
    active: r.active
  })
  showTieredModal.value = true
}

const submitTieredForm = async () => {
  if (!tieredForm.tierName || !tieredForm.minQuantity) {
    ElMessage.warning('Vui lòng điền tên bậc giá và số lượng tối thiểu!')
    return
  }
  submittingTiered.value = true
  try {
    if (editingTieredId.value) {
      await api.put(`/promotions/tiered-rules/${editingTieredId.value}`, tieredForm)
      ElMessage.success('Cập nhật quy tắc giá sỉ thành công!')
    } else {
      await api.post('/promotions/tiered-rules', tieredForm)
      ElMessage.success('Tạo quy tắc giá sỉ mới thành công!')
    }
    showTieredModal.value = false
    loadTieredRules()
    loadOverview()
  } catch (err) {
    ElMessage.error(err.response?.data?.message || 'Lỗi khi lưu quy tắc giá sỉ!')
  } finally {
    submittingTiered.value = false
  }
}

const deleteTieredRule = async (r) => {
  try {
    await ElMessageBox.confirm(`Xóa quy tắc '${r.tierName}'?`, 'Xác nhận xóa', {
      type: 'warning',
      confirmButtonText: 'Xóa ngay',
      cancelButtonText: 'Hủy'
    })
    await api.delete(`/promotions/tiered-rules/${r.id}`)
    ElMessage.success('Đã xóa quy tắc giá sỉ!')
    loadTieredRules()
    loadOverview()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('Lỗi khi xóa quy tắc!')
  }
}

// =============================================================================
// 6. TAB 4: SIMULATOR STATE & METHODS
// =============================================================================
const sampleCustomers = ref([])
const simCustomer = ref(null)
const simVoucherCode = ref('')
const simPointsToUse = ref(0)
const simItems = ref([])
const calculatingSim = ref(false)
const simResult = ref(null)

const loadSampleCustomers = async () => {
  try {
    const res = await api.get('/promotions/loyalty/members', { params: { size: 20 } })
    sampleCustomers.value = (res.data?.content || []).map(m => ({
      id: m.customerId,
      name: m.customerName,
      phone: m.customerPhone,
      memberTier: m.memberTier,
      loyaltyPoints: m.loyaltyPoints
    }))
  } catch (e) {}
}

const onSimCustomerChange = (c) => {
  simPointsToUse.value = 0
}

const addSimItem = () => {
  simItems.value.push({
    productId: allProducts.value[0]?.id || null,
    quantity: 1,
    baseUnitPrice: allProducts.value[0]?.price || 0
  })
}

const onSimItemProductChange = (item) => {
  const p = allProducts.value.find(x => x.id === item.productId)
  if (p) item.baseUnitPrice = p.price
}

const removeSimItem = (idx) => {
  simItems.value.splice(idx, 1)
}

const runSimulation = async () => {
  if (simItems.value.length === 0) {
    ElMessage.warning('Vui lòng thêm ít nhất 1 mặt hàng vào giỏ để mô phỏng!')
    return
  }
  calculatingSim.value = true
  try {
    const payload = {
      customerId: simCustomer.value?.id || null,
      voucherCode: simVoucherCode.value || undefined,
      loyaltyPointsToRedeem: simPointsToUse.value || 0,
      items: simItems.value.map(i => ({
        productId: i.productId,
        quantity: i.quantity,
        baseUnitPrice: i.baseUnitPrice
      }))
    }
    const res = await api.post('/promotions/calculate-cart', payload)
    simResult.value = res.data
    ElMessage.success('Mô phỏng Promotion Engine hoàn tất!')
  } catch (e) {
    ElMessage.error('Lỗi khi mô phỏng giỏ hàng!')
  } finally {
    calculatingSim.value = false
  }
}

// =============================================================================
// 7. LAZY LOADING THEO TAB & LIFECYCLE
// =============================================================================
const handleTabClick = (tab) => {
  if (tab.paneName === 'vouchers') {
    if (vouchers.value.length === 0) loadVouchers()
  } else if (tab.paneName === 'loyalty') {
    if (loyaltyMembers.value.length === 0) loadLoyaltyMembers()
  } else if (tab.paneName === 'tiered') {
    if (tieredRules.value.length === 0) loadTieredRules()
  } else if (tab.paneName === 'simulator') {
    if (sampleCustomers.value.length === 0) loadSampleCustomers()
    if (allProducts.value.length === 0) loadProducts()
    if (simItems.value.length === 0) addSimItem()
  }
}

const refreshData = () => {
  loadOverview()
  if (activeTab.value === 'vouchers') loadVouchers()
  else if (activeTab.value === 'loyalty') loadLoyaltyMembers()
  else if (activeTab.value === 'tiered') loadTieredRules()
  ElMessage.success('Đã làm mới dữ liệu Khuyến Mại & Loyalty!')
}

onMounted(() => {
  loadOverview()
  loadVouchers()
})
</script>

<style scoped>
.promotions-container {
  padding: 20px;
  background-color: #f8fafc;
  min-height: calc(100vh - 60px);
  padding-bottom: 64px;
}

.theme-dark .promotions-container {
  background-color: #0f172a;
}

/* Header */
.promotions-header {
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
  color: #0f172a;
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0;
}

.theme-dark .page-title {
  color: #f1f5f9;
}

.title-icon {
  color: #ec4899;
}

.page-subtitle {
  font-size: 13px;
  color: #64748b;
  margin-top: 4px;
}

.theme-dark .page-subtitle {
  color: #94a3b8;
}

/* Stat Cards */
.stats-row {
  margin-bottom: 20px;
}

.stat-card {
  border-radius: 10px;
  border: 1px solid #e2e8f0;
  transition: all 0.2s;
  height: 100%;
}

.theme-dark .stat-card {
  border-color: #334155;
  background: #1e293b;
}

.stat-content {
  display: flex;
  align-items: center;
  gap: 12px;
}

.stat-icon-wrapper {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  flex-shrink: 0;
}

.bg-blue-light { background: #eff6ff; }
.text-blue { color: #2563eb; }

.bg-purple-light { background: #faf5ff; }
.text-purple { color: #9333ea; }

.bg-amber-light { background: #fffbeb; }
.text-amber { color: #d97706; }
.text-amber-dark { color: #b45309; }

.bg-emerald-light { background: #ecfdf5; }
.text-emerald { color: #059669; }

.bg-indigo-light { background: #e0e7ff; }
.text-indigo { color: #4f46e5; }

.stat-value {
  font-size: 18px;
  font-weight: 700;
  color: #0f172a;
  line-height: 1.2;
}

.theme-dark .stat-value {
  color: #f8fafc;
}

.stat-label {
  font-size: 12px;
  color: #64748b;
  margin-top: 2px;
}

.stat-sub {
  font-size: 11px;
  margin-top: 3px;
}

/* Tabs & Content */
.promotions-tabs {
  background: #ffffff;
  padding: 16px 20px;
  border-radius: 10px;
  border: 1px solid #e2e8f0;
}

.theme-dark .promotions-tabs {
  background: #1e293b;
  border-color: #334155;
}

.toolbar-box {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 16px;
}

.filter-group {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}

.custom-table {
  border-radius: 8px;
  overflow: hidden;
}

/* Voucher Code Badge */
.voucher-code-cell {
  display: flex;
  align-items: center;
  gap: 6px;
}

.code-badge {
  background: #f1f5f9;
  border: 1px dashed #cbd5e1;
  padding: 2px 8px;
  border-radius: 4px;
  font-family: monospace;
  font-weight: 700;
  color: #0f172a;
  font-size: 13px;
}

.theme-dark .code-badge {
  background: #334155;
  border-color: #475569;
  color: #f8fafc;
}

/* Tier Cards Grid */
.tiers-summary-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}

.tier-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border-radius: 10px;
  border: 1px solid #e2e8f0;
  background: #ffffff;
}

.theme-dark .tier-card {
  background: #0f172a;
  border-color: #334155;
}

.tier-badge-icon {
  font-size: 28px;
}

.tier-name {
  font-weight: 700;
  font-size: 13px;
  color: #0f172a;
}

.theme-dark .tier-name {
  color: #f8fafc;
}

.tier-spend {
  font-size: 11px;
  color: #64748b;
  margin-top: 2px;
}

.tier-benefit {
  font-size: 11px;
  color: #059669;
  margin-top: 3px;
}

.tier-bronze { border-left: 4px solid #b45309; }
.tier-silver { border-left: 4px solid #94a3b8; }
.tier-gold { border-left: 4px solid #eab308; }
.tier-diamond { border-left: 4px solid #06b6d4; }

/* Simulator */
.simulator-card {
  border-radius: 10px;
  border: 1px solid #cbd5e1;
}

.sim-item-row {
  display: flex;
  align-items: center;
}

.sim-result-box {
  background: #f0fdf4;
  border: 1px solid #bbf7d0;
  padding: 16px;
  border-radius: 8px;
}

.theme-dark .sim-result-box {
  background: #064e3b;
  border-color: #047857;
}

.receipt-breakdown {
  display: flex;
  flex-direction: column;
  gap: 6px;
  max-width: 500px;
}

.receipt-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 13px;
}

.receipt-divider {
  height: 1px;
  background: #cbd5e1;
  margin: 6px 0;
}

/* Drawer transactions */
.tx-card {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  padding: 10px 14px;
  border-radius: 8px;
}

.theme-dark .tx-card {
  background: #0f172a;
  border-color: #334155;
}

.tx-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.tx-footer {
  display: flex;
  justify-content: space-between;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
