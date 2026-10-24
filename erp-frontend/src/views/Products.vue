<template>
  <div class="products-container">
    <div class="page-header">
      <div class="header-text">
        <h2 class="page-title">Quản lý Sản phẩm</h2>
        <p class="page-subtitle">Danh sách hàng hóa, giá niêm yết, tồn kho và hình ảnh sản phẩm</p>
      </div>
      <el-button type="primary" size="large" @click="openDialog()" class="btn-add">
        <el-icon style="margin-right: 6px;"><Plus /></el-icon> Thêm sản phẩm mới
      </el-button>
    </div>

    <!-- Product Table Card -->
    <el-card shadow="never" class="table-card">
      <div class="table-responsive-wrapper">
        <el-table :data="products" v-loading="loading" style="width: 100%; min-width: 700px;">
          <el-table-column prop="id" label="ID" width="70" align="center" />
          
          <!-- Product Image Column -->
          <el-table-column label="Hình ảnh" width="100" align="center">
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

          <el-table-column prop="stock" label="Tồn kho" width="110" align="center">
            <template #default="{ row }">
              <el-tag :type="row.stock > 10 ? 'success' : (row.stock > 0 ? 'warning' : 'danger')">
                {{ row.stock }} cái
              </el-tag>
            </template>
          </el-table-column>

          <el-table-column label="Thao tác" width="150" align="center" fixed="right">
            <template #default="{ row }">
              <el-button size="small" @click="openDialog(row)">Sửa</el-button>
              <el-button size="small" type="danger" @click="deleteProduct(row.id)">Xóa</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-card>

    <!-- Dialog Add / Edit Product (Responsive Width) -->
    <el-dialog 
      :title="form.id ? 'Cập nhật Sản phẩm' : 'Thêm Sản phẩm Mới'" 
      v-model="dialogVisible" 
      :width="isMobile ? '92%' : '560px'"
      destroy-on-close
      class="responsive-dialog"
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

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">Hủy</el-button>
          <el-button type="primary" :loading="saving" @click="saveProduct">Lưu sản phẩm</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Picture, Delete, UploadFilled, Loading } from '@element-plus/icons-vue'
import { useResponsive } from '../composables/useResponsive'
import api from '../api'

const { isMobile } = useResponsive()

const products = ref([])
const loading = ref(false)
const saving = ref(false)
const uploading = ref(false)
const dialogVisible = ref(false)

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
    const res = await api.get('/products')
    products.value = res.data
  } catch (e) {
    ElMessage.error('Không thể tải danh sách sản phẩm!')
  } finally {
    loading.value = false
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

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 16px;
}

.header-text {
  flex: 1;
  min-width: 240px;
}

.page-title {
  margin: 0;
  font-size: 22px;
  font-weight: 700;
  color: #0f172a;
}

.page-subtitle {
  margin: 4px 0 0 0;
  font-size: 13px;
  color: #64748b;
}

.table-card {
  border-radius: 14px;
  border: 1px solid #e2e8f0;
}

.table-responsive-wrapper {
  width: 100%;
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;
}

.product-thumb {
  width: 48px;
  height: 48px;
  border-radius: 8px;
  border: 1px solid #e2e8f0;
  display: inline-block;
  vertical-align: middle;
}

.image-placeholder {
  width: 48px;
  height: 48px;
  border-radius: 8px;
  background-color: #f1f5f9;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #94a3b8;
  font-size: 20px;
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

/* Upload Styling */
.upload-wrapper {
  width: 100%;
}

.image-preview-box {
  position: relative;
  width: 150px;
  height: 150px;
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
  padding: 18px;
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
  font-size: 32px;
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

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

/* Mobile Responsive */
@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
    align-items: stretch;
    gap: 12px;
  }

  .btn-add {
    width: 100%;
  }

  .page-title {
    font-size: 18px;
  }

  .dialog-footer {
    flex-direction: column;
  }

  .dialog-footer .el-button {
    width: 100%;
    margin-left: 0 !important;
  }
}
</style>
