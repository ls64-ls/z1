<template>
  <div class="room-manage-page">
    <div class="page-header">
      <h2>会议室管理</h2>
      <el-button type="primary" @click="handleAdd">新增会议室</el-button>
    </div>

    <el-card class="filter-card">
      <el-form :inline="true" :model="filterForm">
        <el-form-item label="门店">
          <el-select v-model="filterForm.venueId" placeholder="全部门店" clearable style="width: 180px">
            <el-option v-for="v in venueList" :key="v.id" :label="v.name" :value="v.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="filterForm.status" placeholder="全部状态" clearable style="width: 140px">
            <el-option label="启用" value="AVAILABLE" />
            <el-option label="维护中" value="MAINTENANCE" />
            <el-option label="已下线" value="OFFLINE" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="filterForm.keyword" placeholder="搜索会议室名称" clearable style="width: 220px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="table-card">
      <el-table :data="tableData" stripe v-loading="loading" style="width: 100%">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="name" label="会议室名称" width="160" />
        <el-table-column prop="venueName" label="所属门店" width="120" />
        <el-table-column prop="floor" label="楼层" width="80" />
        <el-table-column prop="capacity" label="容纳人数" width="100" />
        <el-table-column prop="areaSqm" label="面积(m²)" width="100" />
        <el-table-column prop="pricePerHour" label="单价(元/时)" width="120" />
        <el-table-column label="设施" min-width="180">
          <template #default="{ row }">
            <el-tag v-for="a in (row.amenities || [])" :key="a.id" size="small" class="amenity-tag">{{ a.name }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button
              :type="row.status === 'AVAILABLE' ? 'warning' : 'success'"
              link size="small"
              @click="handleToggleStatus(row)"
            >
              {{ row.status === 'AVAILABLE' ? '停用' : '启用' }}
            </el-button>
            <el-button type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
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

    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑会议室' : '新增会议室'"
      width="640px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="100px">
        <el-form-item label="会议室名称" prop="name">
          <el-input v-model="formData.name" placeholder="请输入会议室名称" />
        </el-form-item>
        <el-form-item label="所属门店" prop="venueId">
          <el-select v-model="formData.venueId" placeholder="请选择门店" style="width: 100%">
            <el-option v-for="v in venueList" :key="v.id" :label="v.name" :value="v.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="楼层位置" prop="floor">
          <el-input-number v-model="formData.floor" :min="-5" :max="200" style="width: 100%" />
        </el-form-item>
        <el-form-item label="容纳人数" prop="capacity">
          <el-input-number v-model="formData.capacity" :min="1" :max="500" style="width: 100%" />
        </el-form-item>
        <el-form-item label="面积(m²)" prop="areaSqm">
          <el-input-number v-model="formData.areaSqm" :min="1" :max="9999" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="单价(元/时)" prop="pricePerHour">
          <el-input-number v-model="formData.pricePerHour" :min="0" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="formData.description" type="textarea" :rows="3" placeholder="会议室描述" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { roomApi, RoomItem } from '../../services/rooms'
import { venueApi, VenueItem } from '../../services/venues'

const loading = ref(false)
const tableData = ref<RoomItem[]>([])
const venueList = ref<VenueItem[]>([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
const formRef = ref()

const filterForm = reactive({
  venueId: null as number | null,
  status: '' as string,
  keyword: ''
})

const pagination = reactive({ page: 1, size: 10, total: 0 })

const formData = reactive({
  id: 0,
  name: '',
  venueId: 1,
  floor: 1,
  capacity: 10,
  areaSqm: 30,
  pricePerHour: 100,
  description: ''
})

const formRules = {
  name: [{ required: true, message: '请输入会议室名称', trigger: 'blur' }],
  venueId: [{ required: true, message: '请选择门店', trigger: 'change' }],
  capacity: [{ required: true, message: '请输入容纳人数', trigger: 'blur' }],
  pricePerHour: [{ required: true, message: '请输入单价', trigger: 'blur' }]
}

const fetchVenues = async () => {
  try {
    const res = await venueApi.list()
    if (res.data.code === 0) {
      venueList.value = res.data.data || []
    }
  } catch (err) {
    console.error('Failed to load venues', err)
  }
}

const fetchData = async () => {
  loading.value = true
  try {
    const params: any = {
      page: pagination.page,
      size: pagination.size
    }
    if (filterForm.venueId) params.venueId = filterForm.venueId
    if (filterForm.capacity) params.capacity = filterForm.capacity

    const res = await roomApi.list(params)
    if (res.data.code === 0) {
      const pageData = res.data.data
      // Apply client-side filters for status and keyword
      let records = pageData.records || []
      if (filterForm.status) {
        records = records.filter(r => r.status === filterForm.status)
      }
      if (filterForm.keyword) {
        const kw = filterForm.keyword.toLowerCase()
        records = records.filter(r => r.name.toLowerCase().includes(kw))
      }
      tableData.value = records
      pagination.total = pageData.total
    }
  } catch (err) {
    ElMessage.error('加载会议室列表失败')
  } finally {
    loading.value = false
  }
}

const statusLabel = (status: string) => {
  const map: Record<string, string> = {
    AVAILABLE: '启用',
    MAINTENANCE: '维护中',
    OFFLINE: '已下线'
  }
  return map[status] || status
}

const statusType = (status: string) => {
  const map: Record<string, string> = {
    AVAILABLE: 'success',
    MAINTENANCE: 'warning',
    OFFLINE: 'danger'
  }
  return map[status] || 'info'
}

const handleSearch = () => {
  pagination.page = 1
  fetchData()
}

const handleReset = () => {
  filterForm.venueId = null
  filterForm.status = ''
  filterForm.keyword = ''
  pagination.page = 1
  fetchData()
}

const resetForm = () => {
  formData.id = 0
  formData.name = ''
  formData.venueId = 1
  formData.floor = 1
  formData.capacity = 10
  formData.areaSqm = 30
  formData.pricePerHour = 100
  formData.description = ''
}

const handleAdd = () => {
  isEdit.value = false
  resetForm()
  dialogVisible.value = true
}

const handleEdit = (row: RoomItem) => {
  isEdit.value = true
  formData.id = row.id
  formData.name = row.name
  formData.venueId = row.venueId
  formData.floor = row.floor || 1
  formData.capacity = row.capacity
  formData.areaSqm = row.areaSqm || 30
  formData.pricePerHour = row.pricePerHour || 100
  formData.description = row.description || ''
  dialogVisible.value = true
}

const handleSubmit = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    const payload = {
      name: formData.name,
      venueId: formData.venueId,
      floor: formData.floor,
      capacity: formData.capacity,
      areaSqm: formData.areaSqm,
      pricePerHour: formData.pricePerHour,
      description: formData.description
    }
    if (isEdit.value) {
      await roomApi.update(formData.id, payload)
    } else {
      await roomApi.create(payload)
    }
    ElMessage.success(isEdit.value ? '编辑成功' : '新增成功')
    dialogVisible.value = false
    fetchData()
  } catch (err) {
    ElMessage.error('操作失败')
  } finally {
    submitting.value = false
  }
}

const handleToggleStatus = async (row: RoomItem) => {
  const newStatus = row.status === 'AVAILABLE' ? 'MAINTENANCE' : 'AVAILABLE'
  try {
    await roomApi.toggleStatus(row.id, newStatus)
    row.status = newStatus
    ElMessage.success(`会议室已${newStatus === 'AVAILABLE' ? '启用' : '停用'}`)
  } catch (err) {
    ElMessage.error('状态切换失败')
  }
}

const handleDelete = (row: RoomItem) => {
  ElMessageBox.confirm(`确定要删除会议室"${row.name}"吗？删除后不可恢复。`, '删除确认', {
    confirmButtonText: '确定删除',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await roomApi.delete(row.id)
      tableData.value = tableData.value.filter((item) => item.id !== row.id)
      ElMessage.success('删除成功')
    } catch (err) {
      ElMessage.error('删除失败')
    }
  }).catch(() => {})
}

onMounted(() => {
  fetchVenues()
  fetchData()
})
</script>

<style scoped lang="scss">
.room-manage-page {
  padding: 24px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;

  h2 {
    font-size: 24px;
    color: #262626;
    margin: 0;
  }
}

.filter-card {
  margin-bottom: 20px;
}

.table-card {
  .amenity-tag {
    margin-right: 6px;
    margin-bottom: 4px;
  }

  .pagination-box {
    display: flex;
    justify-content: flex-end;
    margin-top: 20px;
  }
}
</style>
