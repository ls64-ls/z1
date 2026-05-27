<template>
  <div class="reports-page">
    <div class="page-header">
      <h2>数据报表</h2>
      <div class="header-actions">
        <el-button @click="handleExport">
          <el-icon><Download /></el-icon> 导出报表
        </el-button>
      </div>
    </div>

    <!-- 时间范围选择 -->
    <el-card class="filter-card">
      <el-form :inline="true" :model="filterForm">
        <el-form-item label="时间范围">
          <el-date-picker
            v-model="filterForm.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width: 280px"
          />
        </el-form-item>
        <el-form-item label="门店">
          <el-select v-model="filterForm.venueId" placeholder="全部门店" clearable style="width: 180px">
            <el-option label="总部大楼" :value="1" />
            <el-option label="分部A座" :value="2" />
            <el-option label="分部B座" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="维度">
          <el-select v-model="filterForm.dimension" style="width: 120px">
            <el-option label="按天" value="day" />
            <el-option label="按周" value="week" />
            <el-option label="按月" value="month" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 核心指标 -->
    <el-row :gutter="20" class="kpi-row">
      <el-col :span="6">
        <el-card class="kpi-card" shadow="hover">
          <div class="kpi-label">总预订次数</div>
          <div class="kpi-value">{{ kpi.totalBookings }}</div>
          <div class="kpi-compare">
            <span class="compare-label">同比</span>
            <span class="compare-up">+15.2%</span>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="kpi-card" shadow="hover">
          <div class="kpi-label">总使用时长(小时)</div>
          <div class="kpi-value">{{ kpi.totalHours }}</div>
          <div class="kpi-compare">
            <span class="compare-label">同比</span>
            <span class="compare-up">+12.8%</span>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="kpi-card" shadow="hover">
          <div class="kpi-label">总收入(元)</div>
          <div class="kpi-value">{{ formatMoney(kpi.totalRevenue) }}</div>
          <div class="kpi-compare">
            <span class="compare-label">同比</span>
            <span class="compare-up">+18.5%</span>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="kpi-card" shadow="hover">
          <div class="kpi-label">平均使用率</div>
          <div class="kpi-value">{{ kpi.avgUsageRate }}%</div>
          <div class="kpi-compare">
            <span class="compare-label">同比</span>
            <span class="compare-down">-2.1%</span>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表第一行 -->
    <el-row :gutter="20" class="chart-row">
      <el-col :span="12">
        <el-card class="chart-card">
          <template #header>
            <span>使用率趋势</span>
          </template>
          <div ref="usageChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card class="chart-card">
          <template #header>
            <span>收入趋势</span>
          </template>
          <div ref="revenueChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表第二行 -->
    <el-row :gutter="20" class="chart-row">
      <el-col :span="12">
        <el-card class="chart-card">
          <template #header>
            <span>高峰时段分布</span>
          </template>
          <div ref="peakHourChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card class="chart-card">
          <template #header>
            <span>会议室使用率排行</span>
          </template>
          <div ref="roomUsageChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 明细数据表 -->
    <el-card class="table-card">
      <template #header>
        <span>使用明细</span>
      </template>
      <el-table :data="detailData" stripe style="width: 100%" max-height="400">
        <el-table-column prop="date" label="日期" width="120" />
        <el-table-column prop="roomName" label="会议室" width="150" />
        <el-table-column prop="venueName" label="门店" width="100" />
        <el-table-column prop="bookings" label="预订数" width="80" />
        <el-table-column prop="hours" label="使用时长(h)" width="110" />
        <el-table-column prop="revenue" label="收入(元)" width="100" />
        <el-table-column prop="usageRate" label="使用率" width="80">
          <template #default="{ row }">
            <el-progress :percentage="row.usageRate" :stroke-width="6" :show-text="true" />
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { Download } from '@element-plus/icons-vue'
import * as echarts from 'echarts'

const filterForm = reactive({
  dateRange: [] as string[],
  venueId: null as number | null,
  dimension: 'day'
})

const kpi = reactive({
  totalBookings: 1285,
  totalHours: 3240,
  totalRevenue: 185600,
  avgUsageRate: 68
})

const usageChartRef = ref<HTMLDivElement>()
const revenueChartRef = ref<HTMLDivElement>()
const peakHourChartRef = ref<HTMLDivElement>()
const roomUsageChartRef = ref<HTMLDivElement>()

interface DetailRow {
  date: string
  roomName: string
  venueName: string
  bookings: number
  hours: number
  revenue: number
  usageRate: number
}

const detailData = ref<DetailRow[]>([
  { date: '2026-05-24', roomName: 'A201 大会议室', venueName: '总部大楼', bookings: 4, hours: 8, revenue: 1600, usageRate: 80 },
  { date: '2026-05-24', roomName: 'B101 报告厅', venueName: '分部A座', bookings: 2, hours: 6, revenue: 4800, usageRate: 60 },
  { date: '2026-05-24', roomName: 'C305 培训室', venueName: '分部B座', bookings: 3, hours: 7, revenue: 3500, usageRate: 70 },
  { date: '2026-05-23', roomName: 'A201 大会议室', venueName: '总部大楼', bookings: 5, hours: 10, revenue: 2000, usageRate: 100 },
  { date: '2026-05-23', roomName: 'B101 报告厅', venueName: '分部A座', bookings: 1, hours: 4, revenue: 3200, usageRate: 40 }
])

const formatMoney = (val: number) => {
  return val.toLocaleString()
}

const initUsageChart = () => {
  if (!usageChartRef.value) return
  const chart = echarts.init(usageChartRef.value)
  chart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 50, right: 20, top: 20, bottom: 30 },
    xAxis: { type: 'category', data: ['05-18', '05-19', '05-20', '05-21', '05-22', '05-23', '05-24'] },
    yAxis: { type: 'value', max: 100, axisLabel: { formatter: '{value}%' } },
    series: [{
      type: 'bar',
      data: [65, 70, 72, 68, 75, 80, 73],
      itemStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: '#1677FF' },
          { offset: 1, color: '#69b1ff' }
        ]),
        borderRadius: [6, 6, 0, 0]
      }
    }]
  })
}

const initRevenueChart = () => {
  if (!revenueChartRef.value) return
  const chart = echarts.init(revenueChartRef.value)
  chart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 60, right: 20, top: 20, bottom: 30 },
    xAxis: { type: 'category', data: ['05-18', '05-19', '05-20', '05-21', '05-22', '05-23', '05-24'] },
    yAxis: { type: 'value', axisLabel: { formatter: '¥{value}' } },
    series: [{
      type: 'line',
      smooth: true,
      data: [8960, 10240, 12500, 11300, 13400, 14560, 12480],
      lineStyle: { color: '#52C41A', width: 3 },
      itemStyle: { color: '#52C41A' },
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(82,196,26,0.3)' },
          { offset: 1, color: 'rgba(82,196,26,0.02)' }
        ])
      }
    }]
  })
}

const initPeakHourChart = () => {
  if (!peakHourChartRef.value) return
  const chart = echarts.init(peakHourChartRef.value)
  const hours = ['08:00', '09:00', '10:00', '11:00', '12:00', '13:00', '14:00', '15:00', '16:00', '17:00', '18:00', '19:00']
  chart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 50, right: 20, top: 20, bottom: 30 },
    xAxis: { type: 'category', data: hours },
    yAxis: { type: 'value' },
    series: [{
      type: 'line',
      data: [5, 18, 32, 28, 12, 8, 25, 30, 22, 15, 6, 2],
      smooth: true,
      lineStyle: { color: '#FAAD14', width: 3 },
      itemStyle: { color: '#FAAD14' },
      markLine: {
        data: [{ type: 'average', name: '平均值' }],
        lineStyle: { color: '#FF4D4F', type: 'dashed' }
      }
    }]
  })
}

const initRoomUsageChart = () => {
  if (!roomUsageChartRef.value) return
  const chart = echarts.init(roomUsageChartRef.value)
  chart.setOption({
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: 100, right: 30, top: 10, bottom: 20 },
    xAxis: { type: 'value', max: 100, axisLabel: { formatter: '{value}%' } },
    yAxis: {
      type: 'category',
      data: ['A102 小会议室', 'B201 接待室', 'C305 培训室', 'B101 报告厅', 'A201 大会议室'],
      inverse: true
    },
    series: [{
      type: 'bar',
      data: [
        { value: 45, itemStyle: { color: '#FF4D4F' } },
        { value: 55, itemStyle: { color: '#FAAD14' } },
        { value: 68, itemStyle: { color: '#1677FF' } },
        { value: 75, itemStyle: { color: '#1677FF' } },
        { value: 82, itemStyle: { color: '#52C41A' } }
      ],
      barWidth: 20,
      borderRadius: [0, 4, 4, 0]
    }]
  })
}

const handleSearch = () => {
  ElMessage.success('查询成功')
}

const handleReset = () => {
  filterForm.dateRange = []
  filterForm.venueId = null
  filterForm.dimension = 'day'
}

const handleExport = () => {
  ElMessage.success('正在导出报表...')
}

onMounted(() => {
  nextTick(() => {
    initUsageChart()
    initRevenueChart()
    initPeakHourChart()
    initRoomUsageChart()
  })
})
</script>

<style scoped lang="scss">
.reports-page {
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

.kpi-row {
  margin-bottom: 20px;
}

.kpi-card {
  text-align: center;

  .kpi-label {
    font-size: 14px;
    color: #8c8c8c;
    margin-bottom: 12px;
  }

  .kpi-value {
    font-size: 32px;
    font-weight: 700;
    color: #262626;
    margin-bottom: 10px;
  }

  .kpi-compare {
    font-size: 13px;

    .compare-label {
      color: #8c8c8c;
      margin-right: 6px;
    }

    .compare-up { color: #52C41A; }
    .compare-down { color: #FF4D4F; }
  }
}

.chart-row {
  margin-bottom: 20px;
}

.chart-card {
  .chart-container {
    width: 100%;
    height: 300px;
  }
}

.table-card {
  .el-progress {
    width: 120px;
  }
}
</style>
