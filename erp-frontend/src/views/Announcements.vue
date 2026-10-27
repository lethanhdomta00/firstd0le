<template>
  <div class="announcements-container">
    <!-- Page Header -->
    <BasePageHeader
      title="Quản lý Thông báo & Sự kiện"
      subtitle="Thiết lập nội dung dòng chữ chạy trên Header toàn hệ thống và các sự kiện nội bộ"
      :icon="Bell"
    >
      <BaseButton type="gradient" size="large" :icon="Plus" @click="openCreateDialog" class="btn-add">
        Tạo thông báo mới
      </BaseButton>
    </BasePageHeader>

    <!-- Base Table -->
    <BaseTable
      :data="announcements"
      :loading="loading"
      :skeleton-columns="6"
      :skeleton-rows="4"
      min-width="820px"
    >
      <el-table-column prop="id" label="ID" width="70" align="center" />

      <el-table-column prop="title" label="Tiêu đề" width="180">
        <template #default="{ row }">
          <div class="announce-title-cell">
            <span class="announce-title">{{ row.title }}</span>
            <el-tag v-if="row.active" size="small" type="success" effect="dark" class="active-banner-tag">
              ĐANG HIỂN THỊ
            </el-tag>
          </div>
        </template>
      </el-table-column>

      <el-table-column prop="content" label="Nội dung chạy chữ trên Header" min-width="260">
        <template #default="{ row }">
          <span class="announce-content">{{ row.content }}</span>
        </template>
      </el-table-column>

      <el-table-column prop="type" label="Loại" width="140" align="center">
        <template #default="{ row }">
          <BaseStatusBadge
            :type="getTypeBadge(row.type).badgeType"
            :text="getTypeBadge(row.type).label"
          />
        </template>
      </el-table-column>

      <el-table-column label="Hiển thị Header" width="160" align="center">
        <template #default="{ row }">
          <div class="toggle-wrap">
            <el-switch
              v-model="row.active"
              active-text="Bật"
              inactive-text="Tắt"
              inline-prompt
              :loading="togglingId === row.id"
              @change="handleToggleActive(row)"
            />
          </div>
        </template>
      </el-table-column>

      <el-table-column label="Ngày cập nhật" width="160" align="center">
        <template #default="{ row }">
          <span class="date-text">{{ formatDate(row.updatedAt || row.createdAt) }}</span>
        </template>
      </el-table-column>

      <el-table-column label="Thao tác" width="140" align="center" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="openEditDialog(row)">Sửa</el-button>
          <el-button size="small" type="danger" plain @click="deleteAnnouncement(row.id)">Xóa</el-button>
        </template>
      </el-table-column>
    </BaseTable>

    <!-- Create / Edit Dialog -->
    <BaseDialog
      v-model="dialogVisible"
      :title="isEditing ? 'Cập nhật thông báo' : 'Tạo thông báo mới'"
      :subtitle="isEditing ? 'Chỉnh sửa nội dung chạy chữ và thiết lập hiển thị' : 'Nhập thông điệp thông báo hoặc sự kiện để hiển thị trên thanh Header'"
      width="540px"
      :loading="saving"
      confirm-text="Lưu thông báo"
      @confirm="saveAnnouncement"
    >
      <el-form :model="form" label-position="top">
        <el-form-item label="Tiêu đề thông báo *" required>
          <el-input v-model="form.title" placeholder="Ví dụ: Chào mừng quý khách, Thông báo bảo trì..." />
        </el-form-item>

        <el-row :gutter="14">
          <el-col :span="12">
            <el-form-item label="Loại thông điệp *" required>
              <el-select v-model="form.type" style="width: 100%">
                <el-option label="📢 Thông báo chung" value="INFO" />
                <el-option label="🎉 Sự kiện đặc biệt" value="EVENT" />
                <el-option label="⚠️ Cảnh báo quan trọng" value="WARNING" />
                <el-option label="✨ Tin vui / Thành tích" value="SUCCESS" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="Kích hoạt lên Header ngay">
              <div style="padding-top: 6px;">
                <el-switch v-model="form.active" active-text="Bật hiển thị" inactive-text="Lưu nháp" />
              </div>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="Nội dung chạy chữ trên Header *" required>
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="4"
            maxlength="500"
            show-word-limit
            placeholder="Nhập nội dung thông điệp sẽ chạy từ phải sang trái trên Banner Header..."
          />
        </el-form-item>
      </el-form>
    </BaseDialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Bell } from '@element-plus/icons-vue'
import api from '../api'

const announcements = ref([])
const loading = ref(false)
const saving = ref(false)
const togglingId = ref(null)
const dialogVisible = ref(false)
const isEditing = ref(false)

const form = reactive({
  id: null,
  title: '',
  content: '',
  type: 'INFO',
  active: true
})

const getTypeBadge = (type) => {
  switch (type) {
    case 'EVENT':
      return { label: 'Sự kiện', badgeType: 'info' }
    case 'WARNING':
      return { label: 'Cảnh báo', badgeType: 'danger' }
    case 'SUCCESS':
      return { label: 'Tin vui', badgeType: 'success' }
    default:
      return { label: 'Thông báo', badgeType: 'primary' }
  }
}

const formatDate = (dateStr) => {
  if (!dateStr) return '-'
  try {
    const d = new Date(dateStr)
    return d.toLocaleString('vi-VN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit'
    })
  } catch (e) {
    return dateStr
  }
}

const fetchAnnouncements = async () => {
  loading.value = true
  try {
    const res = await api.get('/announcements')
    announcements.value = res.data
  } catch (e) {
    ElMessage.error(e.response?.data?.message || 'Lỗi khi lấy danh sách thông báo!')
  } finally {
    loading.value = false
  }
}

const openCreateDialog = () => {
  isEditing.value = false
  form.id = null
  form.title = ''
  form.content = ''
  form.type = 'INFO'
  form.active = true
  dialogVisible.value = true
}

const openEditDialog = (row) => {
  isEditing.value = true
  form.id = row.id
  form.title = row.title
  form.content = row.content
  form.type = row.type || 'INFO'
  form.active = row.active
  dialogVisible.value = true
}

const saveAnnouncement = async () => {
  if (!form.content || !form.content.trim()) {
    ElMessage.warning('Vui lòng nhập nội dung thông báo!')
    return
  }

  saving.value = true
  try {
    if (isEditing.value && form.id) {
      await api.put(`/announcements/${form.id}`, form)
      ElMessage.success('Cập nhật thông báo thành công!')
    } else {
      await api.post('/announcements', form)
      ElMessage.success('Tạo thông báo mới thành công!')
    }
    dialogVisible.value = false
    fetchAnnouncements()
    // Trigger custom event or reload header announcement
    window.dispatchEvent(new CustomEvent('announcement-updated'))
  } catch (e) {
    ElMessage.error(e.response?.data?.message || 'Lỗi khi lưu thông báo!')
  } finally {
    saving.value = false
  }
}

const handleToggleActive = async (row) => {
  togglingId.value = row.id
  try {
    const res = await api.put(`/announcements/${row.id}/toggle-active`)
    ElMessage.success(res.data.message || 'Cập nhật trạng thái thành công!')
    fetchAnnouncements()
    window.dispatchEvent(new CustomEvent('announcement-updated'))
  } catch (e) {
    row.active = !row.active // revert switch on error
    ElMessage.error(e.response?.data?.message || 'Lỗi khi cập nhật trạng thái!')
  } finally {
    togglingId.value = null
  }
}

const deleteAnnouncement = async (id) => {
  try {
    await ElMessageBox.confirm('Bạn có chắc chắn muốn xóa thông báo này?', 'Xác nhận xóa', {
      confirmButtonText: 'Xóa ngay',
      cancelButtonText: 'Hủy',
      type: 'warning'
    })
    await api.delete(`/announcements/${id}`)
    ElMessage.success('Đã xóa thông báo thành công!')
    fetchAnnouncements()
    window.dispatchEvent(new CustomEvent('announcement-updated'))
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error(e.response?.data?.message || 'Lỗi khi xóa thông báo!')
    }
  }
}

onMounted(() => {
  fetchAnnouncements()
})
</script>

<style scoped>
.announcements-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.announce-title-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.announce-title {
  font-weight: 600;
  color: #0f172a;
}

.active-banner-tag {
  width: fit-content;
  font-size: 10px;
  font-weight: 700;
}

.announce-content {
  color: #334155;
  font-size: 13px;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.date-text {
  font-size: 13px;
  color: #64748b;
}

.toggle-wrap {
  display: flex;
  align-items: center;
  justify-content: center;
}

@media (max-width: 768px) {
  .btn-add {
    width: 100%;
  }
}
</style>

