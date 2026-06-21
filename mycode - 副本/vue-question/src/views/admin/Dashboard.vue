<template>
  <div class="dashboard">
    <h2>管理仪表盘</h2>

    <!-- 统计卡片 -->
    <div class="stats-cards">
      <el-card class="stat-card" @click="showStatistics">
        <div class="stat-content">
          <div class="stat-value">{{ totalQuestions }}</div>
          <div class="stat-label">试题总数</div>
        </div>
        <el-icon class="stat-icon"><Document /></el-icon>
      </el-card>
      <el-card class="stat-card" @click="showDuplicateCheck">
        <div class="stat-content">
          <div class="stat-value" :class="{ 'warning': duplicateRate > 5 }">
            {{ duplicateRate }}%
          </div>
          <div class="stat-label">重复率</div>
        </div>
        <el-icon class="stat-icon"><WarningFilled /></el-icon>
      </el-card>
      <el-card class="stat-card">
        <div class="stat-content">
          <div class="stat-value">{{ userCount }}</div>
          <div class="stat-label">用户总数</div>
        </div>
        <el-icon class="stat-icon"><User /></el-icon>
      </el-card>
      <el-card class="stat-card">
        <div class="stat-content">
          <div class="stat-value">{{ todayNewQuestions }}</div>
          <div class="stat-label">今日新增</div>
        </div>
        <el-icon class="stat-icon"><TrendCharts /></el-icon>
      </el-card>
    </div>

    <!-- 图表区域 -->
    <div class="charts-row">
      <el-card class="chart-card">
        <template #header>
          <span>题型分布</span>
        </template>
        <div ref="typeChartRef" class="chart-container"></div>
      </el-card>
      <el-card class="chart-card">
        <template #header>
          <span>近7日新增趋势</span>
        </template>
        <div ref="trendChartRef" class="chart-container"></div>
      </el-card>
    </div>

    <!-- 重复检测和统计弹窗 -->
    <DuplicateCheckDialog v-model="duplicateDialogVisible" @success="loadDashboardData" />
    <StatisticsDialog v-model="statisticsDialogVisible" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Document, WarningFilled, User, TrendCharts } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import { getFullStatistics } from '@/api/question'
import { getUserList } from '@/api/admin'
import DuplicateCheckDialog from '@/components/DuplicateCheckDialog.vue'
import StatisticsDialog from '@/components/StatisticsDialog.vue'

const totalQuestions = ref(0)
const duplicateRate = ref(0)
const userCount = ref(0)
const todayNewQuestions = ref(0)

const duplicateDialogVisible = ref(false)
const statisticsDialogVisible = ref(false)

const typeChartRef = ref(null)
let typeChart = null
const trendChartRef = ref(null)
let trendChart = null

const loadDashboardData = async () => {
  try {
    const statsRes = await getFullStatistics()
    totalQuestions.value = statsRes.data.totalCount?.total || 0
    duplicateRate.value = statsRes.data.qualityAnalysis?.duplicateRate?.toFixed(2) || 0

    // 渲染图表
    renderTypeChart(statsRes.data.typeStats)
    renderTrendChart(statsRes.data.trendStats?.slice(-7))

    const userRes = await getUserList({ pageNum: 1, pageSize: 1 })
    userCount.value = userRes.data?.total || 0
  } catch (error) {
    console.error('加载数据失败', error)
  }
}

const renderTypeChart = (data) => {
  if (!typeChartRef.value) return
  if (typeChart) typeChart.dispose()
  typeChart = echarts.init(typeChartRef.value)

  typeChart.setOption({
    tooltip: { trigger: 'item' },
    legend: { orient: 'vertical', left: 'left' },
    series: [{
      type: 'pie',
      radius: '50%',
      data: (data || []).map(item => ({
        name: item.typeName,
        value: item.count
      }))
    }]
  })
}

const renderTrendChart = (data) => {
  if (!trendChartRef.value) return
  if (trendChart) trendChart.dispose()
  trendChart = echarts.init(trendChartRef.value)

  trendChart.setOption({
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: (data || []).map(item => item.date) },
    yAxis: { type: 'value' },
    series: [{
      type: 'line',
      data: (data || []).map(item => item.count),
      smooth: true,
      areaStyle: { opacity: 0.3 }
    }]
  })
}

const showDuplicateCheck = () => {
  duplicateDialogVisible.value = true
}

const showStatistics = () => {
  statisticsDialogVisible.value = true
}

onMounted(() => {
  loadDashboardData()
})
</script>

<style scoped>
.dashboard {
  padding: 20px;
}

.stats-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin-bottom: 24px;
}

.stat-card {
  cursor: pointer;
  transition: all 0.3s;
  position: relative;
  overflow: hidden;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.stat-content {
  text-align: center;
  padding: 10px 0;
}

.stat-value {
  font-size: 32px;
  font-weight: bold;
  color: #409eff;
}

.stat-value.warning {
  color: #f56c6c;
}

.stat-label {
  font-size: 14px;
  color: #909399;
  margin-top: 8px;
}

.stat-icon {
  position: absolute;
  right: 16px;
  bottom: 16px;
  font-size: 48px;
  opacity: 0.2;
}

.charts-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}

.chart-card {
  height: 400px;
}

.chart-container {
  width: 100%;
  height: 320px;
}
</style>