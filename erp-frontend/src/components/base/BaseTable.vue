<template>
  <el-card shadow="never" class="base-table-card">
    <!-- Optional Toolbar Slot (Search, Filter, Actions) -->
    <div v-if="$slots.toolbar" class="table-toolbar">
      <slot name="toolbar"></slot>
    </div>

    <!-- Responsive Table Container -->
    <div class="table-responsive-wrapper">
      <el-table
        :data="data"
        v-loading="loading"
        :style="{ width: '100%', minWidth: minWidth }"
        stripe
        class="custom-el-table"
        v-bind="$attrs"
      >
        <slot></slot>

        <!-- Empty State Slot -->
        <template #empty>
          <slot name="empty">
            <el-empty :description="emptyText" :image-size="80" />
          </slot>
        </template>
      </el-table>
    </div>

    <!-- Optional Pagination -->
    <div v-if="showPagination && pagination" class="table-pagination">
      <el-pagination
        :current-page="pagination.currentPage"
        :page-size="pagination.pageSize"
        :total="pagination.total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next"
        @size-change="(size) => $emit('sizeChange', size)"
        @current-change="(page) => $emit('pageChange', page)"
      />
    </div>
  </el-card>
</template>

<script setup>
defineProps({
  data: {
    type: Array,
    default: () => []
  },
  loading: {
    type: Boolean,
    default: false
  },
  minWidth: {
    type: String,
    default: '680px'
  },
  emptyText: {
    type: String,
    default: 'Không có dữ liệu hiển thị'
  },
  showPagination: {
    type: Boolean,
    default: false
  },
  pagination: {
    type: Object,
    default: null
  }
})

defineEmits(['sizeChange', 'pageChange'])
</script>

<style scoped>
.base-table-card {
  border-radius: 14px;
  border: 1px solid #e2e8f0;
  overflow: hidden;
}

.table-toolbar {
  padding-bottom: 16px;
  margin-bottom: 16px;
  border-bottom: 1px solid #f1f5f9;
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
}

.table-responsive-wrapper {
  width: 100%;
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;
}

.table-pagination {
  margin-top: 18px;
  display: flex;
  justify-content: flex-end;
}

@media (max-width: 768px) {
  .table-pagination {
    justify-content: center;
  }
  
  :deep(.el-pagination) {
    flex-wrap: wrap;
    justify-content: center;
    gap: 8px;
  }
}
</style>

