<template>
  <div class="schedule-page">
    <div class="page-header">
      <h2>排期管理</h2>
      <div class="header-actions">
        <el-button type="primary" @click="handleAddBlock">
          <el-icon><Plus /></el-icon> 添加封锁时段
        </el-button>
      </div>
    </div>

    <!-- 筛选区域 -->
    <el-card class="filter-card">
      <el-form :inline="true" :model="filterForm">
        <el-form-item label="门店">
          <el-select v-model="filterForm.venueId" placeholder="全部门店" clearable style="width: 180px">
            <el-option label="总部大楼" :value="1" />
            <el-option label="分部A座" :value="2" />
            <el-option label="分部B座" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="会议室">
          <el-select v-model="filterForm.roomId" placeholder="全部会议室" clearable style="width: 200px">
            <el-option label="A201 大会议室" :value="1" />
            <el-option label="B101 报告厅" :value="2" />
            <el-option label="C305 培训室" :value="3" />
            <el-option label="A102 小会议室" :value="4" />
          </el-select>
        </el-form-item>
        <el-form-item label="日期范围">
          <el-date-picker
            v-model="filterForm.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width: 260px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 可用性规则表格 -->
    <el-card class="rules-card">
      <template #header>
        <span>可用性规则</span>
      </template>
      <el-table :data="rules" stripe style="width: 100%">
        <el-table-column prop="name" label="规则名称" width="180" />
        <el-table-column label="适用会议室" min-width="200">
          <template #default="{ row }">
            <el-tag v-for="room in row.rooms" :key="room" size="small" class="room-tag">{{ room }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="适用周期" width="200">
          <template #default="{ row }">
            <span>{{ row.weekDaysLabel }}</span>
          </template>
        </el-table-column>
        <el-table-column label="开放时间" width="140">
          <template #default="{ row }">
            <span>{{ row.openTime }} - {{ row.closeTime }}</span>
          </template>
        </el-table-column>
        <el-table-column label="单次最大时长" width="120">
          <template #default="{ row }">
            <span>{{ row.maxDuration }}小时</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-switch
              :model-value="row.enabled"
              @change="(val: boolean) => { row.enabled = val }"
            />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleEditRule(row)">编辑</el-button>
            <el-button type="danger" link size="small" @click="handleDeleteRule(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 封锁时段列表 -->
    <el-card class="blocks-card">
      <template #header>
        <span>封锁时段</span>
      </template>
      <el-table :data="blockedSlots" stripe style="width: 100%">
        <el-table-column prop="roomName" label="会议室" width="160" />
        <el-table-column prop="date" label="日期" width="120" />
        <el-table-column label="时段" width="160">
          <template #default="{ row }">
            <span>{{ row.startTime }} - {{ row.endTime }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="reason" label="封锁原因" min-width="180" />
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button type="danger" link size="small" @click="handleDeleteBlock(row)">解除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 添加封锁时段弹窗 -->
    <el-dialog v-model="blockDialogVisible" title="添加封锁时段" width="520px" :close-on-click-modal="false">
      <el-form :model="blockForm" label-width="100px">
        <el-form-item label="会议室" required>
          <el-select v-model="blockForm.roomId" placeholder="请选择会议室" style="width: 100%">
            <el-option label="A201 大会议室" :value="1" />
            <el-option label="B101 报告厅" :value="2" />
            <el-option label="C305 培训室" :value="3" />
            <el-option label="A102 小会议室" :value="4" />
          </el-select>
        </el-form-item>
        <el-form-item label="封锁日期" required>
          <el-date-picker
            v-model="blockForm.date"
            type="date"
            placeholder="选择日期"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="封锁时段" required>
          <el-time-picker
            v-model="blockForm.timeRange"
            is-range
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            value-format="HH:mm"
            format="HH:mm"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="封锁原因">
          <el-input v-model="blockForm.reason" type="textarea" :rows="2" placeholder="请输入封锁原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="blockDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleBlockSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'

const filterForm = reactive({
  venueId: null as number | null,
  roomId: null as number | null,
  dateRange: [] as string[]
})

interface Rule {
  id: number
  name: string
  rooms: string[]
  weekDays: number[]
  weekDaysLabel: string
  openTime: string
  closeTime: string
  maxDuration: number
  enabled: boolean
}

interface BlockedSlot {
  id: number
  roomName: string
  date: string
  startTime: string
  endTime: string
  reason: string
  createTime: string
}

const rules = ref<Rule[]>([
  { id: 1, name: '标准工作日规则', rooms: ['A201 大会议室', 'A102 小会议室'], weekDays: [1, 2, 3, 4, 5], weekDaysLabel: '周一至周五', openTime: '08:00', closeTime: '20:00', maxDuration: 4, enabled: true },
  { id: 2, name: '报告厅规则', rooms: ['B101 报告厅'], weekDays: [1, 2, 3, 4, 5, 6], weekDaysLabel: '周一至周六', openTime: '09:00', closeTime: '18:00', maxDuration: 3, enabled: true },
  { id: 3, name: '培训室规则', rooms: ['C305 培训室'], weekDays: [1, 2, 3, 4, 5], weekDaysLabel: '周一至周五', openTime: '08:00', closeTime: '21:00', maxDuration: 8, enabled: false }
])

const blockedSlots = ref<BlockedSlot[]>([
  { id: 1, roomName: 'A201 大会议室', date: '2026-05-26', startTime: '14:00', endTime: '18:00', reason: '设备维护', createTime: '2026-05-20 10:00' },
  { id: 2, roomName: 'B101 报告厅', date: '2026-05-28', startTime: '09:00', endTime: '12:00', reason: '公司全员大会', createTime: '2026-05-19 15:30' }
])

const blockDialogVisible = ref(false)

const blockForm = reactive({
  roomId: null as number | null,
  date: '',
  timeRange: [] as string[],
  reason: ''
})

const handleSearch = () => {
  ElMessage.success('查询成功')
}

const handleReset = () => {
  filterForm.venueId = null
  filterForm.roomId = null
  filterForm.dateRange = []
}

const handleEditRule = (row: Rule) => {
  ElMessage.info(`编辑规则: ${row.name}`)
}

const handleDeleteRule = (row: Rule) => {
  ElMessageBox.confirm(`确定删除规则"${row.name}"吗？`, '删除确认', {
    type: 'warning'
  }).then(() => {
    rules.value = rules.value.filter((r) => r.id !== row.id)
    ElMessage.success('删除成功')
  }).catch(() => {})
}

const handleAddBlock = () => {
  blockForm.roomId = null
  blockForm.date = ''
  blockForm.timeRange = []
  blockForm.reason = ''
  blockDialogVisible.value = true
}

const handleBlockSubmit = () => {
  if (!blockForm.roomId || !blockForm.date || blockForm.timeRange.length < 2) {
    ElMessage.warning('请完善封锁信息')
    return
  }
  blockedSlots.value.push({
    id: Date.now(),
    roomName: `会议室 ${blockForm.roomId}`,
    date: blockForm.date,
    startTime: blockForm.timeRange[0],
    endTime: blockForm.timeRange[1],
    reason: blockForm.reason || '无',
    createTime: new Date().toLocaleString()
  })
  ElMessage.success('封锁时段添加成功')
  blockDialogVisible.value = false
}

const handleDeleteBlock = (row: BlockedSlot) => {
  ElMessageBox.confirm(`确定解除该时段的封锁吗？`, '解除确认', {
    type: 'warning'
  }).then(() => {
    blockedSlots.value = blockedSlots.value.filter((b) => b.id !== row.id)
    ElMessage.success('封锁已解除')
  }).catch(() => {})
}

onMounted(() => {
  // Initial data fetch
})
</script>

<style scoped lang="scss">
.schedule-page {
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

.rules-card {
  margin-bottom: 20px;

  .room-tag {
    margin-right: 6px;
  }
}

.blocks-card {
  .room-tag {
    margin-right: 6px;
  }
}
</style>
