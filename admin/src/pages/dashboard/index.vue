<template>
  <div class="dashboard-page">
    <div class="page-header">
      <h2>数据大盘</h2>
      <p class="page-desc">会议室使用情况概览</p>
    </div>

    <!-- 统计卡片 -->
    <el-row :gutter="20" class="stat-cards">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" style="background-color: rgba(22,119,255,0.1)">
              <el-icon :size="32"><Calendar /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-label">今日预订数</div>
              <div class="stat-value">{{ stats.todayBookings }}</div>
              <div class="stat-trend up">较昨日 +12%</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" style="background-color: rgba(82,196,26,0.1)">
              <el-icon :size="32"><Odometer /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-label">房间使用率</div>
              <div class="stat-value">{{ stats.usageRate }}%</div>
              <div class="stat-trend up">较昨日 +3%</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" style="background-color: rgba(250,173,20,0.1)">
              <el-icon :size="32"><Money /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-label">今日收入</div>
              <div class="stat-value">&yen;{{ stats.todayRevenue }}</div>
              <div class="stat-trend down">较昨日 -5%</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" style="background-color: rgba(255,77,79,0.1)">
              <el-icon :size="32"><Warning /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-label">今日取消</div>
              <div class="stat-value">{{ stats.todayCancelled }}</div>
              <div class="stat-trend down">较昨日 +1</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表区域 -->
    <el-row :gutter="20" class="chart-row">
      <el-col :span="16">
        <el-card class="chart-card">
          <template #header>
            <div class="chart-header">
              <span>近7天预订趋势</span>
              <el-radio-group v-model="chartType" size="small">
                <el-radio-button value="bookings">预订数</el-radio-button>
                <el-radio-button value="revenue">收入</el-radio-button>
              </el-radio-group>
            </div>
          </template>
          <div ref="trendChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card class="chart-card">
          <template #header>
            <span>门店使用率排行</span>
          </template>
          <div class="venue-ranking">
            <div v-for="(item, index) in venueRanking" :key="item.name" class="rank-item">
              <div class="rank-index" :class="'rank-' + (index + 1)">{{ index + 1 }}</div>
              <div class="rank-name">{{ item.name }}</div>
              <div class="rank-bar-box">
                <div class="rank-bar" :style="{ width: item.rate + '%', backgroundColor: item.color }"></div>
              </div>
              <div class="rank-rate">{{ item.rate }}%</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 实时预订 -->
    <el-card class="realtime-card">
      <template #header>
        <span>最新预订记录</span>
      </template>
      <el-table :data="recentBookings" stripe style="width: 100%">
        <el-table-column prop="roomName" label="会议室" width="140" />
        <el-table-column prop="userName" label="预订人" width="120" />
        <el-table-column prop="title" label="会议主题" min-width="180" show-overflow-tooltip />
        <el-table-column prop="date" label="日期" width="120" />
        <el-table-column prop="time" label="时间" width="160" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, nextTick } from 'vue'
import { Calendar, Odometer, Money, Warning } from '@element-plus/icons-vue'
import * as echarts from 'echarts'

const chartType = ref('bookings')
const trendChartRef = ref<HTMLDivElement>()

interface Stats {
  todayBookings: number
  usageRate: number
  todayRevenue: number
  todayCancelled: number
}

const stats = reactive<Stats>({
  todayBookings: 46,
  usageRate: 73,
  todayRevenue: 12480,
  todayCancelled: 3
})

const venueRanking = ref([
  { name: '总部大楼', rate: 85, color: '#1677FF' },
  { name: '分部A座', rate: 67, color: '#52C41A' },
  { name: '分部B座', rate: 52, color: '#FAAD14' }
])

interface RecentBooking {
  roomName: string
  userName: string
  title: string
  date: string
  time: string
  status: string
}

const recentBookings = ref<RecentBooking[]>([
  { roomName: 'A201 会议室', userName: '张三', title: 'Q1季度产品规划评审', date: '2026-05-24', time: '09:00 - 11:00', status: 'CONFIRMED' },
  { roomName: 'B101 报告厅', userName: '李四', title: '客户项目演示', date: '2026-05-24', time: '14:00 - 16:00', status: 'CHECKED_IN' },
  { roomName: 'C305 培训室', userName: '王五', title: '新员工入职培训', date: '2026-05-24', time: '10:00 - 12:00', status: 'CONFIRMED' },
  { roomName: 'A102 小会议室', userName: '赵六', title: '周例会', date: '2026-05-24', time: '15:00 - 16:00', status: 'COMPLETED' },
  { roomName: 'B201 接待室', userName: '孙七', title: '供应商洽谈', date: '2026-05-23', time: '09:00 - 10:30', status: 'CANCELLED' }
])

const statusLabel = (status: string) => {
  const map: Record<string, string> = {
    CONFIRMED: '已确认',
    CHECKED_IN: '已签到',
    COMPLETED: '已完成',
    CANCELLED: '已取消'
  }
  return map[status] || status
}

const statusType = (status: string) => {
  const map: Record<string, string> = {
    CONFIRMED: 'primary',
    CHECKED_IN: 'success',
    COMPLETED: 'info',
    CANCELLED: 'danger'
  }
  return map[status] || 'info'
}

const initTrendChart = () => {
  if (!trendChartRef.value) return
  const chart = echarts.init(trendChartRef.value)
  const dates = ['05-18', '05-19', '05-20', '05-21', '05-22', '05-23', '05-24']
  const bookingsData = [32, 38, 45, 41, 48, 52, 46]
  const revenueData = [8960, 10240, 12500, 11300, 13400, 14560, 12480]

  chart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 50, right: 20, top: 30, bottom: 30 },
    xAxis: { type: 'category', data: dates },
    yAxis: { type: 'value' },
    series: [{
      name: chartType.value === 'bookings' ? '预订数' : '收入',
      type: 'line',
      smooth: true,
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(22,119,255,0.3)' },
          { offset: 1, color: 'rgba(22,119,255,0.02)' }
        ])
      },
      lineStyle: { color: '#1677FF', width: 3 },
      itemStyle: { color: '#1677FF' },
      data: chartType.value === 'bookings' ? bookingsData : revenueData
    }]
  })

  // Re-render when chartType changes
  return chart
}

onMounted(() => {
  nextTick(() => initTrendChart())
})
</script>

<style scoped lang="scss">
.dashboard-page {
  padding: 24px;
}

.page-header {
  margin-bottom: 24px;

  h2 {
    font-size: 24px;
    color: #262626;
    margin: 0 0 8px;
  }

  .page-desc {
    color: #8c8c8c;
    font-size: 14px;
    margin: 0;
  }
}

.stat-cards {
  margin-bottom: 20px;
}

.stat-card {
  .stat-content {
    display: flex;
    align-items: center;
    gap: 16px;
  }

  .stat-icon {
    width: 64px;
    height: 64px;
    border-radius: 12px;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;

    .el-icon {
      color: #1677FF;
    }
  }

  .stat-info {
    flex: 1;
    min-width: 0;
  }

  .stat-label {
    font-size: 14px;
    color: #8c8c8c;
    margin-bottom: 6px;
  }

  .stat-value {
    font-size: 28px;
    font-weight: 700;
    color: #262626;
    margin-bottom: 4px;
  }

  .stat-trend {
    font-size: 12px;

    &.up { color: #52C41A; }
    &.down { color: #FF4D4F; }
  }
}

.chart-row {
  margin-bottom: 20px;
}

.chart-card {
  .chart-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .chart-container {
    width: 100%;
    height: 320px;
  }
}

.venue-ranking {
  .rank-item {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-bottom: 16px;

    &:last-child { margin-bottom: 0; }
  }

  .rank-index {
    width: 24px;
    height: 24px;
    border-radius: 6px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 12px;
    font-weight: 600;
    color: #fff;
    background-color: #ccc;
    flex-shrink: 0;

    &.rank-1 { background-color: #FF4D4F; }
    &.rank-2 { background-color: #FAAD14; }
    &.rank-3 { background-color: #1677FF; }
  }

  .rank-name {
    font-size: 14px;
    color: #262626;
    width: 72px;
    flex-shrink: 0;
  }

  .rank-bar-box {
    flex: 1;
    height: 8px;
    background-color: #f0f0f0;
    border-radius: 4px;
    overflow: hidden;
  }

  .rank-bar {
    height: 100%;
    border-radius: 4px;
    transition: width 0.3s;
  }

  .rank-rate {
    font-size: 14px;
    font-weight: 600;
    color: #262626;
    width: 40px;
    text-align: right;
  }
}

.realtime-card {
  margin-bottom: 20px;
}
</style>
