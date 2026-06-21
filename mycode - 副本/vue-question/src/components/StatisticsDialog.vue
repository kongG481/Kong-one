<template>
  <el-dialog
      v-model="visible"
      title="统计分析"
      width="1200px"
      destroy-on-close
      fullscreen
  >
    <div class="statistics-container">
      <!-- 操作栏 -->
      <div class="action-bar">
        <el-button type="primary" @click="refreshData" :loading="loading">
          <el-icon><Refresh /></el-icon>
          刷新数据
        </el-button>
        <el-button type="success" @click="exportExcel" :loading="exporting">
          <el-icon><Download /></el-icon>
          导出Excel报表
        </el-button>
      </div>

      <!-- 统计卡片 -->
      <div class="stats-cards">
        <el-card class="stat-card">
          <template #header>
            <div class="card-header">
              <span>试题总数</span>
              <el-icon><Document /></el-icon>
            </div>
          </template>
          <div class="card-value">{{ totalStats?.total || 0 }}</div>
        </el-card>
        <el-card class="stat-card">
          <template #header>
            <div class="card-header">
              <span>单选题</span>
              <el-icon><List /></el-icon>
            </div>
          </template>
          <div class="card-value">{{ totalStats?.singleCount || 0 }}</div>
        </el-card>
        <el-card class="stat-card">
          <template #header>
            <div class="card-header">
              <span>多选题</span>
              <el-icon><Grid /></el-icon>
            </div>
          </template>
          <div class="card-value">{{ totalStats?.multiCount || 0 }}</div>
        </el-card>
        <el-card class="stat-card">
          <template #header>
            <div class="card-header">
              <span>填空题</span>
              <el-icon><Edit /></el-icon>
            </div>
          </template>
          <div class="card-value">{{ totalStats?.blankCount || 0 }}</div>
        </el-card>
        <el-card class="stat-card">
          <template #header>
            <div class="card-header">
              <span>简答题</span>
              <el-icon><ChatLineSquare /></el-icon>
            </div>
          </template>
          <div class="card-value">{{ totalStats?.essayCount || 0 }}</div>
        </el-card>
        <el-card class="stat-card">
          <template #header>
            <div class="card-header">
              <span>重复率</span>
              <el-icon><WarningFilled /></el-icon>
            </div>
          </template>
          <div class="card-value" :class="{ 'danger-text': (qualityAnalysis?.duplicateRate || 0) > 5 }">
            {{ formatDuplicateRate(qualityAnalysis?.duplicateRate) }}%
          </div>
        </el-card>
      </div>

      <!-- 图表区域 -->
      <div class="charts-row">
        <el-card class="chart-card">
          <template #header>
            <span>题型分布</span>
          </template>
          <div ref="typeChartRef" class="chart-container" style="width: 100%; height: 320px;"></div>
        </el-card>
        <el-card class="chart-card">
          <template #header>
            <span>难度分布</span>
          </template>
          <div ref="difficultyChartRef" class="chart-container" style="width: 100%; height: 320px;"></div>
        </el-card>
      </div>

      <div class="charts-row">
        <el-card class="chart-card">
          <template #header>
            <span>学科分布（Top 10）</span>
          </template>
          <div ref="subjectChartRef" class="chart-container" style="width: 100%; height: 320px;"></div>
        </el-card>
        <el-card class="chart-card">
          <template #header>
            <span>每日新增趋势（近30天）</span>
          </template>
          <div ref="trendChartRef" class="chart-container" style="width: 100%; height: 320px;"></div>
        </el-card>
      </div>

      <!-- 教师排行榜 -->
      <el-card class="rank-card">
        <template #header>
          <span>教师出题排行榜</span>
        </template>
        <el-table :data="teacherRank" stripe border v-if="teacherRank.length > 0">
          <el-table-column type="index" label="排名" width="80" />
          <el-table-column prop="teacherName" label="教师姓名" />
          <el-table-column prop="college" label="学院" />
          <el-table-column prop="questionCount" label="出题数量" sortable>
            <template #default="{ row }">
              <el-tag type="primary">{{ row.questionCount }}</el-tag>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-else description="暂无教师数据" />
      </el-card>

      <!-- 热门试题 -->
      <el-card class="hot-card">
        <template #header>
          <span>热门试题（按查看次数）</span>
        </template>
        <el-table :data="hotQuestions" stripe border v-if="hotQuestions.length > 0">
          <el-table-column type="index" label="排名" width="80" />
          <el-table-column prop="content" label="题目内容" min-width="300" show-overflow-tooltip />
          <el-table-column prop="typeName" label="题型" width="100" />
          <el-table-column prop="subjectName" label="学科" width="150" />
          <el-table-column prop="viewCount" label="查看次数" width="120" sortable>
            <template #default="{ row }">
              <el-tag type="warning">{{ row.viewCount }}</el-tag>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-else description="暂无热门试题" />
      </el-card>
    </div>

    <template #footer>
      <el-button @click="visible = false">关闭</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, watch, onMounted, onUnmounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh, Download, Document, List, Grid, Edit, ChatLineSquare, WarningFilled } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import { getFullStatistics, exportStatistics } from '@/api/question'

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update:modelValue'])

const visible = ref(false)
const loading = ref(false)
const exporting = ref(false)
const totalStats = ref(null)
const qualityAnalysis = ref(null)
const teacherRank = ref([])
const hotQuestions = ref([])

// 图表实例
let typeChart = null
let difficultyChart = null
let subjectChart = null
let trendChart = null

const typeChartRef = ref(null)
const difficultyChartRef = ref(null)
const subjectChartRef = ref(null)
const trendChartRef = ref(null)

// 监听对话框打开
watch(() => props.modelValue, async (val) => {
  visible.value = val
  if (val) {
    // 等待 DOM 渲染完成
    await nextTick()
    setTimeout(() => {
      loadStatistics()
    }, 100)
  }
})

watch(visible, (val) => {
  emit('update:modelValue', val)
})

// 格式化重复率
const formatDuplicateRate = (rate) => {
  if (rate === undefined || rate === null) return '0.00'
  return rate.toFixed(2)
}

// 加载统计数据
const loadStatistics = async () => {
  loading.value = true
  try {
    const res = await getFullStatistics()
    console.log('统计数据:', res.data)

    const data = res.data

    totalStats.value = data.totalCount || {}
    qualityAnalysis.value = data.qualityAnalysis || {}
    teacherRank.value = data.teacherRank || []
    hotQuestions.value = data.hotQuestions || []

    // 延迟渲染图表，确保 DOM 已准备好
    await nextTick()
    setTimeout(() => {
      renderAllCharts(data)
    }, 100)
  } catch (error) {
    console.error('加载统计数据失败:', error)
    ElMessage.error('加载统计数据失败')
  } finally {
    loading.value = false
  }
}

// 渲染所有图表
const renderAllCharts = (data) => {
  // 题型分布
  if (typeChartRef.value) {
    renderTypeChart(data.typeStats, data.totalCount?.total)
  } else {
    console.warn('typeChartRef 未找到')
  }

  // 难度分布
  if (difficultyChartRef.value) {
    renderDifficultyChart(data.difficultyStats)
  } else {
    console.warn('difficultyChartRef 未找到')
  }

  // 学科分布
  if (subjectChartRef.value) {
    renderSubjectChart(data.subjectStats)
  } else {
    console.warn('subjectChartRef 未找到')
  }

  // 每日趋势
  if (trendChartRef.value) {
    renderTrendChart(data.trendStats)
  } else {
    console.warn('trendChartRef 未找到')
  }
}

// 渲染题型分布饼图
const renderTypeChart = (data, total) => {
  if (!typeChartRef.value) {
    console.error('typeChartRef 元素不存在')
    return
  }

  try {
    if (typeChart) {
      typeChart.dispose()
    }
    typeChart = echarts.init(typeChartRef.value)

    const chartData = (data || []).map(item => ({
      name: item.typeName || getTypeName(item.type),
      value: item.count || 0
    }))

    console.log('题型数据:', chartData)

    typeChart.setOption({
      tooltip: { trigger: 'item', formatter: '{b}: {d}% ({c}题)' },
      legend: { orient: 'vertical', left: 'left', top: 'center' },
      series: [{
        type: 'pie',
        radius: '55%',
        center: ['50%', '50%'],
        data: chartData,
        label: {
          show: true,
          formatter: '{b}: {d}%'
        },
        emphasis: {
          scale: true
        }
      }]
    })
  } catch (error) {
    console.error('渲染题型图表失败:', error)
  }
}

// 渲染难度分布柱状图
const renderDifficultyChart = (data) => {
  if (!difficultyChartRef.value) {
    console.error('difficultyChartRef 元素不存在')
    return
  }

  try {
    if (difficultyChart) {
      difficultyChart.dispose()
    }
    difficultyChart = echarts.init(difficultyChartRef.value)

    const difficultyMap = { 1: '简单', 2: '中等', 3: '困难' }
    const chartData = (data || []).map(item => ({
      name: difficultyMap[item.difficulty] || '未知',
      value: item.count || 0,
      difficulty: item.difficulty
    }))

    console.log('难度数据:', chartData)

    difficultyChart.setOption({
      tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
      grid: { left: '10%', right: '5%', top: '10%', bottom: '5%', containLabel: true },
      xAxis: {
        type: 'category',
        data: chartData.map(item => item.name),
        axisLabel: { fontSize: 12 }
      },
      yAxis: { type: 'value', name: '题目数量' },
      series: [{
        type: 'bar',
        data: chartData.map(item => item.value),
        itemStyle: {
          borderRadius: [4, 4, 0, 0],
          color: (params) => {
            const colors = ['#67c23a', '#e6a23c', '#f56c6c']
            return colors[params.dataIndex] || '#409eff'
          }
        },
        label: {
          show: true,
          position: 'top',
          formatter: '{c}'
        }
      }]
    })
  } catch (error) {
    console.error('渲染难度图表失败:', error)
  }
}

// 渲染学科分布柱状图
const renderSubjectChart = (data) => {
  if (!subjectChartRef.value) {
    console.error('subjectChartRef 元素不存在')
    return
  }

  try {
    if (subjectChart) {
      subjectChart.dispose()
    }
    subjectChart = echarts.init(subjectChartRef.value)

    const topData = (data || []).slice(0, 10)
    console.log('学科数据:', topData)

    if (topData.length === 0) {
      subjectChart.setOption({
        title: { show: true, text: '暂无数据', left: 'center', top: 'center' }
      })
      return
    }

    subjectChart.setOption({
      tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
      grid: { left: '15%', right: '5%', top: '10%', bottom: '10%', containLabel: true },
      xAxis: {
        type: 'category',
        data: topData.map(item => item.subjectName || '未知学科'),
        axisLabel: { rotate: 35, interval: 0, fontSize: 11 }
      },
      yAxis: { type: 'value', name: '题目数量' },
      series: [{
        type: 'bar',
        data: topData.map(item => item.count || 0),
        itemStyle: {
          borderRadius: [4, 4, 0, 0],
          color: '#409eff'
        },
        label: {
          show: true,
          position: 'top',
          formatter: '{c}'
        }
      }]
    })
  } catch (error) {
    console.error('渲染学科图表失败:', error)
  }
}

// 渲染每日趋势折线图
const renderTrendChart = (data) => {
  if (!trendChartRef.value) {
    console.error('trendChartRef 元素不存在')
    return
  }

  try {
    if (trendChart) {
      trendChart.dispose()
    }
    trendChart = echarts.init(trendChartRef.value)

    const chartData = (data || []).map(item => ({
      date: item.date,
      count: item.count || 0
    }))

    console.log('趋势数据:', chartData)

    if (chartData.length === 0) {
      trendChart.setOption({
        title: { show: true, text: '暂无数据', left: 'center', top: 'center' }
      })
      return
    }

    trendChart.setOption({
      tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
      grid: { left: '10%', right: '5%', top: '10%', bottom: '5%', containLabel: true },
      xAxis: {
        type: 'category',
        data: chartData.map(item => item.date),
        axisLabel: { rotate: 35, interval: 4, fontSize: 11 }
      },
      yAxis: { type: 'value', name: '新增题目数量' },
      series: [{
        type: 'line',
        data: chartData.map(item => item.count),
        smooth: true,
        symbol: 'circle',
        symbolSize: 8,
        lineStyle: { width: 3, color: '#409eff' },
        areaStyle: { opacity: 0.3, color: '#409eff' },
        itemStyle: { color: '#409eff' },
        label: {
          show: true,
          position: 'top',
          formatter: '{c}'
        }
      }]
    })
  } catch (error) {
    console.error('渲染趋势图表失败:', error)
  }
}

// 辅助方法：获取题型名称
const getTypeName = (type) => {
  const map = { 1: '单选题', 2: '多选题', 3: '填空题', 4: '简答题' }
  return map[type] || '未知'
}

// 刷新数据
const refreshData = () => {
  loadStatistics()
}

// 导出Excel
const exportExcel = async () => {
  exporting.value = true
  try {
    const blob = await exportStatistics()
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `统计报表_${new Date().toLocaleDateString().replace(/\//g, '-')}.xlsx`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  } catch (error) {
    console.error('导出失败:', error)
    ElMessage.error('导出失败')
  } finally {
    exporting.value = false
  }
}

const getDuplicateRateClass = (rate) => {
  if (rate >= 10) return 'danger-text'
  if (rate >= 5) return 'warning-text'
  return ''
}

// 窗口大小变化时重新渲染图表
const handleResize = () => {
  setTimeout(() => {
    if (typeChart) typeChart.resize()
    if (difficultyChart) difficultyChart.resize()
    if (subjectChart) subjectChart.resize()
    if (trendChart) trendChart.resize()
  }, 100)
}

onMounted(() => {
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  if (typeChart) typeChart.dispose()
  if (difficultyChart) difficultyChart.dispose()
  if (subjectChart) subjectChart.dispose()
  if (trendChart) trendChart.dispose()
})
</script>

<style scoped>
.statistics-container {
  padding: 10px;
}

.action-bar {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-bottom: 20px;
}

.stats-cards {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}

.stat-card {
  text-align: center;
  cursor: pointer;
  transition: all 0.3s;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-value {
  font-size: 28px;
  font-weight: bold;
  color: #409eff;
}

.card-value.danger-text {
  color: #f56c6c;
}

.card-value.warning-text {
  color: #e6a23c;
}

.charts-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  margin-bottom: 20px;
}

.chart-card {
  height: 420px;
}

.chart-container {
  width: 100%;
  height: 340px;
}

.rank-card, .hot-card {
  margin-bottom: 20px;
}
</style>