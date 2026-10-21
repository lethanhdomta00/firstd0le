<template>
  <div>
    <div style="display: flex; justify-content: space-between; margin-bottom: 20px;">
      <h2>Product Management</h2>
      <el-button type="primary" @click="openDialog()">Add Product</el-button>
    </div>

    <el-table :data="products" v-loading="loading" border style="width: 100%">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="name" label="Name" />
      <el-table-column prop="sku" label="SKU" />
      <el-table-column prop="price" label="Price ($)" width="120" />
      <el-table-column prop="stock" label="Stock" width="120" />
      <el-table-column label="Actions" width="150">
        <template #default="{ row }">
          <el-button size="small" @click="openDialog(row)">Edit</el-button>
          <el-button size="small" type="danger" @click="deleteProduct(row.id)">Delete</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog :title="form.id ? 'Edit Product' : 'Add Product'" v-model="dialogVisible" width="500px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="Name">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="SKU">
          <el-input v-model="form.sku" />
        </el-form-item>
        <el-form-item label="Price">
          <el-input-number v-model="form.price" :min="0" :precision="2" />
        </el-form-item>
        <el-form-item label="Stock">
          <el-input-number v-model="form.stock" :min="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">Cancel</el-button>
        <el-button type="primary" @click="saveProduct">Save</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../api'

const products = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const form = ref({ id: null, name: '', sku: '', price: 0, stock: 0 })

const fetchProducts = async () => {
  loading.value = true
  try {
    const res = await api.get('/products')
    products.value = res.data
  } catch (e) {
    ElMessage.error('Failed to fetch products')
  } finally {
    loading.value = false
  }
}

const openDialog = (row = null) => {
  if (row) {
    form.value = { ...row }
  } else {
    form.value = { id: null, name: '', sku: '', price: 0, stock: 0 }
  }
  dialogVisible.value = true
}

const saveProduct = async () => {
  try {
    if (form.value.id) {
      await api.put(`/products/${form.value.id}`, form.value)
      ElMessage.success('Product updated')
    } else {
      await api.post('/products', form.value)
      ElMessage.success('Product created')
    }
    dialogVisible.value = false
    fetchProducts()
  } catch (e) {
    ElMessage.error('Failed to save product')
  }
}

const deleteProduct = async (id) => {
  try {
    await ElMessageBox.confirm('Are you sure you want to delete this product?', 'Warning', { type: 'warning' })
    await api.delete(`/products/${id}`)
    ElMessage.success('Product deleted')
    fetchProducts()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('Failed to delete product')
  }
}

onMounted(() => {
  fetchProducts()
})
</script>

