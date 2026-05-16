<template>
  <div class="product-list-page">
    <div class="page-header">
      <h2>商品管理</h2>
      <router-link to="/merchant/products/create">
        <el-button type="primary">发布新商品</el-button>
      </router-link>
    </div>

    <div class="search-bar">
      <el-input
        v-model="keyword"
        placeholder="商品名称搜索"
        clearable
        style="width: 260px"
        @keyup.enter="handleSearch"
      />
      <el-button type="primary" @click="handleSearch">搜索</el-button>
    </div>

    <div class="table-wrap">
      <el-table
        v-loading="loading"
        :data="productList"
        stripe
        border
        style="width: 100%"
      >
        <el-table-column label="商品图片" width="90" align="center">
          <template #default="{ row }">
            <el-image
              :src="row.mainImage"
              style="width: 50px; height: 50px"
              fit="cover"
              :preview-src-list="[row.mainImage]"
              preview-teleported
            />
          </template>
        </el-table-column>

        <el-table-column label="商品名称" prop="name" min-width="180" show-overflow-tooltip />

        <el-table-column label="价格" width="160">
          <template #default="{ row }">
            <PriceDisplay :price="row.minPrice ?? row.price" :originalPrice="row.marketPrice ?? row.maxPrice" />
          </template>
        </el-table-column>

        <el-table-column label="库存" prop="stock" width="80" align="center" />

        <el-table-column label="状态" width="110" align="center">
          <template #default="{ row }">
            <el-switch
              :model-value="row.status === 1"
              inline-prompt
              active-text="上架"
              inactive-text="下架"
              @change="handleStatusToggle(row)"
            />
          </template>
        </el-table-column>

        <el-table-column label="操作" width="160" align="center">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button
              :type="row.status === 1 ? 'danger' : 'success'"
              link
              size="small"
              @click="handleStatusToggle(row)"
            >
              {{ row.status === 1 ? '下架' : '上架' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="total > 0" class="pagination-wrap">
        <el-pagination
          background
          layout="total, prev, pager, next"
          :total="total"
          :page-size="pageSize"
          :current-page="currentPage"
          @current-change="onPageChange"
        />
      </div>

      <el-empty v-if="!loading && productList.length === 0" description="暂无商品" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getProductList, request } from '@supermarket/api'
import { PriceDisplay } from '@supermarket/ui'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()

const keyword = ref('')
const productList = ref<any[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const loading = ref(false)

async function fetchData() {
  loading.value = true
  try {
    const params: Record<string, any> = {
      page: currentPage.value,
      size: pageSize.value,
    }
    if (keyword.value) {
      params.name = keyword.value
    }
    const res: any = await getProductList(params)
    productList.value = res.records ?? []
    total.value = res.total ?? 0
  } catch {
    productList.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  currentPage.value = 1
  fetchData()
}

function onPageChange(page: number) {
  currentPage.value = page
  fetchData()
}

function handleEdit(row: any) {
  router.push({ name: 'productCreate', query: { spuId: row.spuId } })
}

async function handleStatusToggle(row: any) {
  const newStatus = row.status === 1 ? 0 : 1
  const actionText = newStatus === 1 ? '上架' : '下架'
  try {
    await ElMessageBox.confirm(`确认${actionText}该商品？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await request.put(`/product/spu/${row.spuId}/shelf`, null, { params: { shelfStatus: newStatus } })
    row.status = newStatus
    ElMessage.success(`${actionText}成功`)
  } catch {
    /* cancelled */
  }
}

onMounted(fetchData)
</script>

<style scoped>
.product-list-page {
  background: #fff;
  padding: 20px;
  border-radius: 4px;
}
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}
.page-header h2 {
  font-size: 18px;
  font-weight: 600;
  margin: 0;
}
.search-bar {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
}
.table-wrap {
  position: relative;
}
.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  padding: 16px 0 0;
}
</style>
