<template>
  <div class="products-container">
    <!-- Reusable Base Page Header -->
    <BasePageHeader
      title="Quản lý Sản phẩm"
      subtitle="Danh sách hàng hóa, giá niêm yết, phân trang cơ sở dữ liệu và tồn kho"
      :icon="Goods"
    >
      <BaseButton 
        v-if="authStore.isAdmin"
        type="gradient" 
        size="large" 
        :icon="Plus" 
        @click="openDialog()" 
        class="btn-add"
      >
        Thêm sản phẩm mới
      </BaseButton>
    </BasePageHeader>

    <!-- Floating Batch Actions Bar (when products are selected) -->
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
            Xóa {{ selectedProductIds.length }} sản phẩm đã chọn
          </el-button>
        </div>
      </div>
    </transition>

    <!-- Reusable Base Table with Built-in Skeleton Loading & Server-Side Pagination -->
    <BaseTable 
      ref="tableRef"
      :data="products" 
      :loading="loading" 
      :has-thumbnail="true" 
      :skeleton-columns="6" 
      :skeleton-rows="5"
      min-width="760px"
      :show-pagination="true"
      :pagination="{
        currentPage: page + 1,
        pageSize: size,
        total: totalElements
      }"
      @page-change="handlePageChange"
      @size-change="handleSizeChange"
      @selection-change="handleSelectionChange"
    >
      <!-- Search & Multi-Filter Toolbar -->
      <template #toolbar>
        <div class="toolbar-wrap">
          <div class="filter-group">
            <!-- Keyword Search -->
            <el-input 
              v-model="filters.keyword" 
              placeholder="Tìm theo tên hoặc mã SKU..." 
              clearable
              class="filter-search"
              @clear="onSearch"
              @keyup.enter="onSearch"
            >
              <template #prefix><el-icon><Search /></el-icon></template>
            </el-input>

            <!-- Stock Status Filter -->
            <el-select 
              v-model="filters.stockStatus" 
              placeholder="Tồn kho" 
              class="filter-select"
              @change="onFilterChange"
            >
              <el-option label="Tất cả kho hàng" value="ALL" />
              <el-option label="Còn hàng (> 10)" value="IN_STOCK" />
              <el-option label="Sắp hết (1 - 10)" value="LOW_STOCK" />
              <el-option label="Đã hết hàng (0)" value="OUT_OF_STOCK" />
            </el-select>

            <!-- Sorting Selector -->
            <el-select 
              v-model="filters.sort" 
              placeholder="Sắp xếp" 
              class="filter-select"
              @change="onFilterChange"
            >
              <el-option label="Mới nhất (Mặc định)" value="id,desc" />
              <el-option label="Giá: Thấp đến Cao" value="price,asc" />
              <el-option label="Giá: Cao đến Thấp" value="price,desc" />
              <el-option label="Tồn kho: Nhiều nhất" value="stock,desc" />
              <el-option label="Tên sản phẩm: A - Z" value="name,asc" />
            </el-select>

            <!-- Reset Filters -->
            <el-button @click="resetFilters" plain class="btn-reset">
              <el-icon><RefreshRight /></el-icon>
              <span>Đặt lại</span>
            </el-button>
          </div>

          <div class="count-tag">
            <span>Tổng số: <strong>{{ totalElements }}</strong> sản phẩm</span>
          </div>
        </div>
      </template>

      <!-- Selection Column (Admin Only) -->
      <el-table-column 
        v-if="authStore.isAdmin" 
        type="selection" 
        width="48" 
        align="center" 
      />

      <!-- Table Columns -->
      <el-table-column prop="id" label="ID" width="70" align="center" />
      
      <!-- Product Image Column -->
      <el-table-column label="Hình ảnh" width="90" align="center">
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

      <el-table-column prop="name" label="Tên sản phẩm" min-width="180">
        <template #default="{ row }">
          <span class="product-name">{{ row.name }}</span>
        </template>
      </el-table-column>

      <el-table-column prop="sku" label="Mã SKU" width="130">
        <template #default="{ row }">
          <el-tag size="small" type="info">{{ row.sku }}</el-tag>
        </template>
      </el-table-column>

      <el-table-column prop="price" label="Giá bán" width="140">
        <template #default="{ row }">
          <span class="price-tag">{{ formatCurrency(row.price) }}</span>
        </template>
      </el-table-column>

      <!-- Reusable Status Badge for Stock -->
      <el-table-column prop="stock" label="Tồn kho" width="130" align="center">
        <template #default="{ row }">
          <BaseStatusBadge
            :type="row.stock > 10 ? 'success' : (row.stock > 0 ? 'warning' : 'danger')"
            :text="row.stock > 0 ? row.stock + ' cái' : 'Hết hàng'"
          />
        </template>
      </el-table-column>

      <!-- Actions Column -->
      <el-table-column label="Thao tác" width="150" align="center" fixed="right">
        <template #default="{ row }">
          <div v-if="authStore.isAdmin" class="action-btn-group">
            <el-button size="small" @click="openDialog(row)">Sửa</el-button>
            <el-button size="small" type="danger" plain @click="deleteProduct(row.id)">Xóa</el-button>
          </div>
          <el-tag v-else size="small" type="info">Chỉ xem</el-tag>
        </template>
      </el-table-column>
    </BaseTable>

    <!-- Product Create/Edit Dialog -->
    <BaseDialog 
      v-model="dialogVisible" 
      :title="form.id ? 'Cập nhật Sản phẩm' : 'Thêm Sản phẩm Mới'" 
      :subtitle="form.id ? 'Cập nhật giá bán, số lượng tồn kho và hình ảnh sản phẩm' : 'Nhập thông tin sản phẩm mới để lưu trữ vào cơ sở dữ liệu'"
      width="560px"
      :loading="saving"
      confirm-text="Lưu sản phẩm"
      @confirm="saveProduct"
    >
      <el-form :model="form" label-position="top" class="product-form">
        <!-- Image Upload Section -->
        <el-form-item label="Hình ảnh sản phẩm">
          <div class="upload-wrapper">
            <div v-if="form.imageUrl" class="image-preview-box">
              <img :src="form.imageUrl" alt="Preview" class="preview-img" />
              <div class="preview-overlay">
                <el-button type="danger" circle size="small" @click="form.imageUrl = ''">
                  <el-icon><Delete /></el-icon>
                </el-button>
              </div>
            </div>

            <el-upload
              v-else
              class="image-uploader"
              drag
              action="#"
              :http-request="handleCustomUpload"
              :show-file-list="false"
              accept=".jpg,.jpeg,.png,.webp,.gif"
            >
              <div v-if="!uploading" class="upload-prompt">
                <el-icon class="upload-icon"><UploadFilled /></el-icon>
                <div class="el-upload__text">Kéo thả ảnh vào đây hoặc <em>chọn file</em></div>
                <div class="upload-tip">JPG, PNG, WEBP tối đa 5MB (Lưu trữ Cloudinary)</div>
              </div>
              <div v-else class="upload-loading">
                <el-icon class="is-loading" style="font-size: 24px;"><Loading /></el-icon>
                <p>Đang tải ảnh lên Cloud...</p>
              </div>
            </el-upload>
          </div>
        </el-form-item>

        <el-row :gutter="16">
          <el-col :xs="24" :sm="15">
            <el-form-item label="Tên sản phẩm *" required>
              <el-input v-model="form.name" placeholder="Ví dụ: Laptop Dell XPS 15" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="9">
            <el-form-item label="Mã SKU *" required>
              <el-input v-model="form.sku" placeholder="SKU-001" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :xs="24" :sm="12">
            <el-form-item label="Đơn giá (VNĐ) *" required>
              <el-input-number 
                v-model="form.price" 
                :min="0" 
                :step="10000"
                style="width: 100%" 
              />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="Số lượng tồn kho *" required>
              <el-input-number 
                v-model="form.stock" 
                :min="0" 
                style="width: 100%" 
              />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
    </BaseDialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useAuthStore } from '../store/auth'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Picture, Delete, UploadFilled, Loading, Search, Goods, RefreshRight } from '@element-plus/icons-vue'
import api from '../api'

const authStore = useAuthStore()

const tableRef = ref(null)
const products = ref([])
const totalElements = ref(0)
const totalPages = ref(0)
const page = ref(0)
const size = ref(10)
const loading = ref(false)
const saving = ref(false)
const uploading = ref(false)
const batchDeleting = ref(false)
const dialogVisible = ref(false)

const selectedProductIds = ref([])

const filters = reactive({
  keyword: '',
  stockStatus: 'ALL',
  sort: 'id,desc'
})

const form = ref({
  id: null,
  name: '',
  sku: '',
  price: 0,
  stock: 0,
  imageUrl: ''
})

const formatCurrency = (val) => {
  if (val == null) return '0 ₫'
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(val)
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
      // Spring Data Page<Product> response
      products.value = res.data.content
      totalElements.value = res.data.totalElements
      totalPages.value = res.data.totalPages
    } else if (Array.isArray(res.data)) {
      // Fallback if array
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
  page.value = newPage - 1 // Element Plus is 1-indexed, Spring Data is 0-indexed
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
  if (tableRef.value && tableRef.value.$el) {
    const elTable = tableRef.value.$el.querySelector('.el-table')
    if (elTable && elTable.__vueParentComponent?.ctx?.clearSelection) {
      elTable.__vueParentComponent.ctx.clearSelection()
    }
  }
}

const handleBatchDelete = async () => {
  if (selectedProductIds.value.length === 0) return

  try {
    await ElMessageBox.confirm(
      `Bạn có chắc chắn muốn xóa vĩnh viễn ${selectedProductIds.value.length} sản phẩm đã chọn?`,
      'Xác nhận xóa hàng loạt',
      {
        confirmButtonText: 'Xóa ngay',
        cancelButtonText: 'Hủy',
        type: 'warning'
      }
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

const openDialog = (row = null) => {
  if (row) {
    form.value = { ...row }
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
  if (!form.value.name || !form.value.sku) {
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

onMounted(() => {
  fetchProducts()
})
</script>

<style scoped>
.products-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* Floating Batch Bar */
.batch-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 18px;
  background: #fef2f2;
  border: 1px solid #fecaca;
  border-radius: 12px;
  color: #991b1b;
  font-size: 14px;
}

.batch-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.batch-icon {
  font-size: 18px;
  color: #ef4444;
}

.batch-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

/* Filter Toolbar */
.toolbar-wrap {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
}

.filter-group {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}

.filter-search {
  width: 250px;
}

.filter-select {
  width: 170px;
}

.btn-reset {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.count-tag {
  font-size: 13px;
  color: #64748b;
}

.product-thumb {
  width: 44px;
  height: 44px;
  border-radius: 8px;
  border: 1px solid #e2e8f0;
  display: inline-block;
  vertical-align: middle;
}

.image-placeholder {
  width: 44px;
  height: 44px;
  border-radius: 8px;
  background-color: #f1f5f9;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #94a3b8;
  font-size: 18px;
  border: 1px dashed #cbd5e1;
}

.product-name {
  font-weight: 600;
  color: #0f172a;
}

.price-tag {
  font-weight: 700;
  color: #0284c7;
}

.action-btn-group {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

/* Upload Styling */
.upload-wrapper {
  width: 100%;
}

.image-preview-box {
  position: relative;
  width: 140px;
  height: 140px;
  border-radius: 12px;
  overflow: hidden;
  border: 2px solid #e2e8f0;
}

.preview-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.preview-overlay {
  position: absolute;
  top: 8px;
  right: 8px;
}

:deep(.image-uploader .el-upload-dragger) {
  padding: 16px;
  border-radius: 12px;
  border: 2px dashed #cbd5e1;
  background-color: #f8fafc;
  transition: all 0.25s ease;
}

:deep(.image-uploader .el-upload-dragger:hover) {
  border-color: #0284c7;
  background-color: #f0f9ff;
}

.upload-icon {
  font-size: 30px;
  color: #0284c7;
  margin-bottom: 6px;
}

.upload-tip {
  font-size: 11px;
  color: #94a3b8;
  margin-top: 4px;
}

.upload-loading {
  padding: 20px;
  text-align: center;
  color: #0284c7;
}

@media (max-width: 768px) {
  .filter-search,
  .filter-select,
  .btn-add {
    width: 100%;
    max-width: 100%;
  }

  .filter-group {
    width: 100%;
  }

  .batch-bar {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }
}
</style>
