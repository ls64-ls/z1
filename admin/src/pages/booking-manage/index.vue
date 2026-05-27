<template>
  <div class="booking-manage-page">
    <div class="page-header">
      <h2>预订管理</h2>
    </div>

    <el-card class="filter-card">
      <el-form :inline="true" :model="filterForm">
        <el-form-item label="日期">
          <el-date-picker
            v-model="filterForm.date"
            type="date"
            placeholder="选择日期"
            value-format="YYYY-MM-DD"
            style="width: 200px"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="filterForm.status" placeholder="全部状态" clearable style="width: 140px">
            <el-option label="待支付" value="PENDING" />
            <el-option label="已确认" value="CONFIRMED" />
            <el-option label="已签到" value="CHECKED_IN" />
            <el-option label="已完成" value="COMPLETED" />
            <el-option label="已取消" value="CANCELLED" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="table-card">
      <el-table :data="tableData" stripe v-loading="loading" style="width: 100%">
        <el-table-column prop="bookingNo" label="预订编号" width="180" />
        <el-table-column prop="roomName" label="会议室" width="140" />
        <el-table-column prop="venueName" label="门店" width="100" />
        <el-table-column prop="title" label="会议主题" min-width="180" show-overflow-tooltip />
        <el-table-column prop="bookingDate" label="日期" width="120" />
        <el-table-column label="时间" width="140">
          <template #default="{ row }">{{ row.startTime }} - {{ row.endTime }}</template>
        </el-table-column>
        <el-table-column label="金额" width="100">
          <template #default="{ row }">¥{{ row.totalAmount || 0 }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleView(row)">详情</el-button>
            <el-button
              v-if="row.status === 'CONFIRMED'"
              type="danger" link size="small"
              @click="handleCancel(row)"
            >取消</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-box">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          :total="pagination.total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @change="fetchData"
        />
      </div>
    </el-card>

    <el-dialog v-model="detailVisible" title="预订详情" width="560px">
      <el-descriptions v-if="currentDetail" :column="2" border>
        <el-descriptions-item label="预订编号">{{ currentDetail.bookingNo }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="statusType(currentDetail.status)" size="small">{{ statusLabel(currentDetail.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="会议室">{{ currentDetail.roomName }}</el-descriptions-item>
        <el-descriptions-item label="门店">{{ currentDetail.venueName }}</el-descriptions-item>
        <el-descriptions-item label="会议主题" :span="2">{{ currentDetail.title }}</el-descriptions-item>
        <el-descriptions-item label="日期">{{ currentDetail.bookingDate }}</el-descriptions-item>
        <el-descriptions-item label="时间">{{ currentDetail.startTime }} - {{ currentDetail.endTime }}</el-descriptions-item>
        <el-descriptions-item label="金额">¥{{ currentDetail.totalAmount || 0 }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ currentDetail.createdAt }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { bookingApi, BookingItem } from '../../services/bookings'

const loading = ref(false)
const tableData = ref<BookingItem[]>([])
const detailVisible = ref(false)
const currentDetail = ref<BookingItem | null>(null)

const filterForm = reactive({
  date: '',
  status: ''
})

const pagination = reactive({ page: 1, size: 10, total: 0 })

const statusLabel = (status: string) => {
  const map: Record<string, string> = {
    PENDING: '待支付', CONFIRMED: '已确认', CHECKED_IN: '已签到',
    COMPLETED: '已完成', CANCELLED: '已取消', EXPIRED: '已过期'
  }
  return map[status] || status
}

const statusType = (status: string) => {
  const map: Record<string, string> = {
    PENDING: 'warning', CONFIRMED: 'primary', CHECKED_IN: 'success',
    COMPLETED: 'info', CANCELLED: 'danger', EXPIRED: 'info'
  }
  return map[status] || 'info'
}

const fetchData = async () => {
  loading.value = true
  try {
    const params: any = { page: pagination.page, size: pagination.size }
    if (filterForm.status) params.status = filterForm.status
    if (filterForm.date) params.date = filterForm.date
    const res = await bookingApi.list(params)
    if (res.data.code === 0) {
      tableData.value = res.data.data.records || []
      pagination.total = res.data.data.total
    }
  } catch (err) {
    ElMessage.error('加载预订列表失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => { pagination.page = 1; fetchData() }
const handleReset = () => { filterForm.date = ''; filterForm.status = ''; pagination.page = 1; fetchData() }

const handleView = (row: BookingItem) => { currentDetail.value = row; detailVisible.value = true }

const handleCancel = (row: BookingItem) => {
  ElMessageBox.confirm(`确定取消预订"${row.title}"吗？`, '取消确认', {
    confirmButtonText: '确定取消', cancelButtonText: '返回', type: 'warning'
  }).then(async () => {
    try {
      await bookingApi.cancel(row.id, '管理员取消')
      row.status = 'CANCELLED'
      ElMessage.success('预订已取消')
    } catch (err) {
      ElMessage.error('取消失败')
    }
  }).catch(() => {})
}

onMounted(() => { fetchData() })
</script>

<style scoped lang="scss">
.booking-manage-page { padding: 24px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px;
  h2 { font-size: 24px; color: #262626; margin: 0; }
}
.filter-card { margin-bottom: 20px; }
.table-card {
  .pagination-box { display: flex; justify-content: flex-end; margin-top: 20px; }
}
</style>
