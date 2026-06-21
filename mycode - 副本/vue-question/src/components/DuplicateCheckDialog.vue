<template>
  <el-dialog
      v-model="visible"
      title="重复检测"
      width="1000px"
      destroy-on-close
      @close="handleClose"
  >
    <div class="duplicate-check-container">
      <!-- 操作栏 -->
      <div class="action-bar">
        <el-button type="primary" @click="batchCheckAll" :loading="batchChecking">
          <el-icon><RefreshRight /></el-icon>
          批量检测全部题目
        </el-button>
        <el-button @click="loadPendingList" :loading="pendingLoading">
          <el-icon><Refresh /></el-icon>
          刷新待处理列表
        </el-button>
        <div class="stats-info" v-if="stats">
          <el-tag type="danger">待处理: {{ stats.pendingCount || 0 }}</el-tag>
          <el-tag type="info">已忽略: {{ stats.ignoredCount || 0 }}</el-tag>
          <el-tag type="success">已合并: {{ stats.mergedCount || 0 }}</el-tag>
        </div>
      </div>

      <!-- 选项卡 -->
      <el-tabs v-model="activeTab">
        <el-tab-pane label="待处理重复" name="pending">
          <el-table
              :data="pendingList"
              stripe
              border
              v-loading="pendingLoading"
              max-height="400"
          >
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column label="题目A" min-width="250" show-overflow-tooltip>
              <template #default="{ row }">
                <div class="question-preview">
                  <span class="question-id">[ID:{{ row.questionIdA }}]</span>
                  {{ truncateText(row.contentA, 80) }}
                </div>
              </template>
            </el-table-column>
            <el-table-column label="题目B" min-width="250" show-overflow-tooltip>
              <template #default="{ row }">
                <div class="question-preview">
                  <span class="question-id">[ID:{{ row.questionIdB }}]</span>
                  {{ truncateText(row.contentB, 80) }}
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="similarity" label="相似度" width="150">
              <template #default="{ row }">
                <el-progress
                    :percentage="row.similarity"
                    :color="getProgressColor(row.similarity)"
                    :format="(p) => `${p}%`"
                />
              </template>
            </el-table-column>
            <el-table-column label="等级" width="100">
              <template #default="{ row }">
                <el-tag :type="getLevelTagType(row.similarity)">
                  {{ getDuplicateLevel(row.similarity) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="180" fixed="right">
              <template #default="{ row }">
                <el-button
                    type="success"
                    size="small"
                    @click="handleDuplicateRecord(row.id, 'ignore')"
                >
                  忽略
                </el-button>
                <el-button
                    type="danger"
                    size="small"
                    @click="handleDuplicateRecord(row.id, 'merge')"
                >
                  合并
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="单题检测" name="single">
          <div class="single-check-section">
            <div class="check-input">
              <el-input
                  v-model="singleQuestionId"
                  placeholder="请输入试题ID"
                  type="number"
                  style="width: 200px"
              />
              <el-button type="primary" @click="checkSingleDuplicate" :loading="singleChecking">
                开始检测
              </el-button>
            </div>

            <div v-if="singleResult" class="check-result">
              <el-alert
                  :title="singleResult.isDuplicate ? '⚠️ 检测到重复题目' : '✅ 未检测到重复题目'"
                  :type="singleResult.isDuplicate ? 'warning' : 'success'"
                  :closable="false"
              />

              <div v-if="singleResult.duplicates?.length" class="duplicate-list">
                <h4>重复题目列表（共 {{ singleResult.duplicateCount }} 条）：</h4>
                <div
                    v-for="(dup, idx) in singleResult.duplicates"
                    :key="idx"
                    class="duplicate-item"
                >
                  <div class="dup-header">
                    <span class="dup-id">ID: {{ dup.duplicateQuestionId }}</span>
                    <el-tag :type="getLevelTagType(dup.similarity)" size="small">
                      {{ dup.level }}
                    </el-tag>
                    <el-tag type="info" size="small">{{ dup.suggestion }}</el-tag>
                  </div>
                  <div class="dup-content">{{ dup.duplicateContent }}</div>
                  <div class="dup-similarity">
                    相似度: {{ dup.similarity }}%
                    <el-progress
                        :percentage="dup.similarity"
                        :show-text="false"
                        :stroke-width="6"
                    />
                  </div>
                </div>
              </div>
            </div>
          </div>
        </el-tab-pane>

        <el-tab-pane label="检测报告" name="report">
          <div v-if="batchResult" class="batch-result">
            <el-descriptions :column="2" border>
              <el-descriptions-item label="检测时间">
                {{ batchResult.checkTime || new Date().toLocaleString() }}
              </el-descriptions-item>
              <el-descriptions-item label="检测题目总数">
                {{ batchResult.totalChecked || 0 }}
              </el-descriptions-item>
              <el-descriptions-item label="发现重复对数">
                <el-tag type="danger">{{ batchResult.duplicatePairs || 0 }}</el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="重复率">
                <el-tag :type="getDuplicateRateType(batchResult.duplicateRate)">
                  {{ batchResult.duplicateRate || 0 }}%
                </el-tag>
              </el-descriptions-item>
            </el-descriptions>
          </div>
          <el-empty v-else description="暂无检测报告，请先执行批量检测" />
        </el-tab-pane>
      </el-tabs>
    </div>

    <template #footer>
      <el-button @click="visible = false">关闭</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, watch, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { RefreshRight, Refresh } from '@element-plus/icons-vue'
import {
  batchCheckDuplicates,
  handleDuplicate,
  getDuplicateStatistics,
  getPendingDuplicates,
  checkQuestionDuplicate
} from '@/api/question'

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  },
  defaultQuestionId: {
    type: Number,
    default: null
  }
})

const emit = defineEmits(['update:modelValue', 'success'])

const visible = ref(false)
const activeTab = ref('pending')
const pendingList = ref([])
const stats = ref(null)
const pendingLoading = ref(false)
const batchChecking = ref(false)
const batchResult = ref(null)
const singleQuestionId = ref('')
const singleResult = ref(null)
const singleChecking = ref(false)

watch(() => props.modelValue, (val) => {
  visible.value = val
  if (val) {
    loadData()
  }
})

watch(visible, (val) => {
  emit('update:modelValue', val)
})

// 监听 defaultQuestionId，自动切换到单题检测
watch(() => props.defaultQuestionId, (val) => {
  if (val && visible.value) {
    activeTab.value = 'single'
    singleQuestionId.value = val
    nextTick(() => {
      checkSingleDuplicate()
    })
  }
})

const loadData = () => {
  loadPendingList()
  loadStats()
}

const loadPendingList = async () => {
  pendingLoading.value = true
  try {
    const res = await getPendingDuplicates()
    pendingList.value = res.data || []
  } catch (error) {
    console.error('加载待处理列表失败', error)
  } finally {
    pendingLoading.value = false
  }
}

const loadStats = async () => {
  try {
    const res = await getDuplicateStatistics()
    stats.value = res.data
  } catch (error) {
    console.error('加载统计失败', error)
  }
}

const batchCheckAll = async () => {
  batchChecking.value = true
  try {
    const res = await batchCheckDuplicates()
    batchResult.value = res.data
    ElMessage.success(`批量检测完成，共检测 ${res.data.totalChecked} 道题，发现 ${res.data.duplicatePairs} 对重复`)
    await loadPendingList()
    await loadStats()
    activeTab.value = 'report'
  } catch (error) {
    ElMessage.error('批量检测失败')
  } finally {
    batchChecking.value = false
  }
}

const checkSingleDuplicate = async () => {
  if (!singleQuestionId.value) {
    ElMessage.warning('请输入试题ID')
    return
  }
  singleChecking.value = true
  try {
    const res = await checkQuestionDuplicate(singleQuestionId.value)
    singleResult.value = res.data
    if (res.data.isDuplicate) {
      ElMessage.warning(`检测到 ${res.data.duplicateCount} 个重复题目`)
    } else {
      ElMessage.success('未检测到重复题目')
    }
  } catch (error) {
    ElMessage.error('检测失败')
  } finally {
    singleChecking.value = false
  }
}

// 重命名为 handleDuplicateRecord 避免与导入的函数冲突
const handleDuplicateRecord = async (id, type) => {
  const actionText = type === 'ignore' ? '忽略' : '合并'
  try {
    await ElMessageBox.confirm(
        `确定要${actionText}该重复记录吗？${type === 'merge' ? '合并后将删除重复题目。' : ''}`,
        '提示',
        {type: 'warning'}
    )
    await handleDuplicate(id, type)
    ElMessage.success(`${actionText}成功`)
    await loadPendingList()
    await loadStats()
    emit('success')
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(`${actionText}失败`)
    }
  }
}

const getProgressColor = (percentage) => {
  if (percentage >= 95) return '#f56c6c'
  if (percentage >= 85) return '#e6a23c'
  if (percentage >= 70) return '#409eff'
  return '#67c23a'
}

const getLevelTagType = (similarity) => {
  if (similarity >= 95) return 'danger'
  if (similarity >= 85) return 'warning'
  if (similarity >= 70) return 'primary'
  return 'info'
}

const getDuplicateLevel = (similarity) => {
  if (similarity >= 95) return '完全相同'
  if (similarity >= 85) return '高度相似'
  if (similarity >= 70) return '中度相似'
  return '低度相似'
}

const getDuplicateRateType = (rate) => {
  if (rate >= 10) return 'danger'
  if (rate >= 5) return 'warning'
  return 'success'
}

const truncateText = (text, maxLen) => {
  if (!text) return ''
  return text.length > maxLen ? text.substring(0, maxLen) + '...' : text
}

const handleClose = () => {
  visible.value = false
  singleQuestionId.value = ''
  singleResult.value = null
}
</script>

<style scoped>
.duplicate-check-container {
  padding: 10px 0;
}

.action-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

.stats-info {
  display: flex;
  gap: 10px;
  margin-left: auto;
}

.question-preview {
  font-size: 13px;
  line-height: 1.5;
}

.question-id {
  color: #909399;
  font-size: 12px;
  margin-right: 5px;
}

.single-check-section {
  padding: 20px 0;
}

.check-input {
  display: flex;
  gap: 10px;
  margin-bottom: 20px;
}

.check-result {
  margin-top: 20px;
}

.duplicate-list {
  margin-top: 20px;
}

.duplicate-item {
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 12px;
  margin-bottom: 12px;
  background-color: #fafafa;
}

.dup-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
}

.dup-id {
  font-weight: bold;
  color: #409eff;
}

.dup-content {
  font-size: 14px;
  margin-bottom: 8px;
  color: #606266;
}

.dup-similarity {
  font-size: 12px;
  color: #909399;
}

.batch-result {
  padding: 10px 0;
}
</style>