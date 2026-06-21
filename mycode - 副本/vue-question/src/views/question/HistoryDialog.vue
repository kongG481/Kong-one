<!-- src/components/question/HistoryDialog.vue -->
<template>
  <el-dialog
      v-model="dialogVisible"
      title="历史版本"
      width="800px"
      @close="handleClose"
  >
    <el-timeline>
      <el-timeline-item
          v-for="(item, index) in historyList"
          :key="item.id"
          :timestamp="item.createTime"
          :type="getTimelineType(index)"
          placement="top"
          size="large"
      >
        <div class="history-item">
          <div class="history-header">
            <div class="history-info">
              <span class="history-operator">{{ item.operatorName }}</span>
              <el-tag size="small" :type="getOperationTypeTag(item.operationType)">
                {{ getOperationTypeName(item.operationType) }}
              </el-tag>
            </div>
            <div class="history-note">{{ item.changeNote || '无说明' }}</div>
          </div>
          <div class="history-content">
            <el-collapse>
              <el-collapse-item title="题目内容" name="content">
                <div class="content-preview">{{ item.content }}</div>
              </el-collapse-item>
              <el-collapse-item title="试题解析" name="analysis">
                <div class="content-preview">{{ item.analysis || '无' }}</div>
              </el-collapse-item>
              <el-collapse-item title="难度" name="difficulty">
                <el-rate :model-value="item.difficulty" :colors="difficultyColors" disabled />
              </el-collapse-item>
            </el-collapse>
          </div>
          <div class="history-actions">
            <el-button type="primary" size="small" @click="viewVersion(item)">查看详情</el-button>
            <el-button
                type="success"
                size="small"
                @click="compareWithCurrent(item)"
                v-if="index > 0"
            >
              对比当前
            </el-button>
            <el-button
                type="warning"
                size="small"
                @click="rollbackToVersion(item)"
            >
              回滚到此版本
            </el-button>
          </div>
        </div>
      </el-timeline-item>
    </el-timeline>

    <template #footer>
      <el-button @click="dialogVisible = false">关闭</el-button>
    </template>
  </el-dialog>

  <!-- 版本对比对话框 -->
  <el-dialog v-model="compareVisible" title="版本对比" width="800px" @close="compareVisible = false">
    <el-tabs v-model="activeTab">
      <el-tab-pane label="题目内容" name="content">
        <div class="compare-container">
          <div class="compare-version">
            <h4>历史版本 ({{ compareData.version1?.createTime }})</h4>
            <div class="compare-content" v-html="compareData.version1?.content"></div>
          </div>
          <div class="compare-version">
            <h4>当前版本</h4>
            <div class="compare-content" v-html="compareData.version2?.content"></div>
          </div>
        </div>
      </el-tab-pane>
      <el-tab-pane label="试题解析" name="analysis">
        <div class="compare-container">
          <div class="compare-version">
            <h4>历史版本</h4>
            <div class="compare-content">{{ compareData.version1?.analysis || '无' }}</div>
          </div>
          <div class="compare-version">
            <h4>当前版本</h4>
            <div class="compare-content">{{ compareData.version2?.analysis || '无' }}</div>
          </div>
        </div>
      </el-tab-pane>
      <el-tab-pane label="难度" name="difficulty">
        <div class="compare-container" v-if="compareData.version1 && compareData.version2">
          <div class="compare-version">
            <h4>历史版本</h4>
            <el-rate :model-value="compareData.version1.difficulty" :colors="difficultyColors" disabled />
          </div>
          <div class="compare-version">
            <h4>当前版本</h4>
            <el-rate :model-value="compareData.version2.difficulty" :colors="difficultyColors" disabled />
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>
    <template #footer>
      <el-button @click="compareVisible = false">关闭</el-button>
    </template>
  </el-dialog>

  <!-- 版本详情对话框 -->
  <el-dialog v-model="detailVisible" :title="'版本详情 - ' + detailData.createTime" width="600px">
    <div class="detail-container">
      <div class="detail-item">
        <span class="detail-label">操作人：</span>
        <span>{{ detailData.operatorName }}</span>
      </div>
      <div class="detail-item">
        <span class="detail-label">操作类型：</span>
        <el-tag :type="getOperationTypeTag(detailData.operationType)">
          {{ getOperationTypeName(detailData.operationType) }}
        </el-tag>
      </div>
      <div class="detail-item">
        <span class="detail-label">修改说明：</span>
        <span>{{ detailData.changeNote || '无' }}</span>
      </div>
      <div class="detail-item">
        <span class="detail-label">难度：</span>
        <el-rate :model-value="detailData.difficulty" :colors="difficultyColors" disabled />
      </div>
      <div class="detail-item">
        <span class="detail-label">题目内容：</span>
        <div class="detail-content">{{ detailData.content }}</div>
      </div>
      <div class="detail-item">
        <span class="detail-label">试题解析：</span>
        <div class="detail-content">{{ detailData.analysis || '无' }}</div>
      </div>
    </div>
    <template #footer>
      <el-button @click="detailVisible = false">关闭</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getQuestionDetail } from '@/api/question'

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  },
  questionId: {
    type: Number,
    required: true
  },
  historyList: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['update:modelValue', 'rollback'])

const dialogVisible = ref(props.modelValue)
const compareVisible = ref(false)
const detailVisible = ref(false)
const activeTab = ref('content')
const compareData = ref({
  version1: null,
  version2: null
})
const detailData = ref({})

const difficultyColors = ['#99A9BF', '#F7BA2A', '#FF9900']

watch(() => props.modelValue, (val) => {
  dialogVisible.value = val
})

watch(dialogVisible, (val) => {
  emit('update:modelValue', val)
})

const handleClose = () => {
  dialogVisible.value = false
}

const getTimelineType = (index) => {
  return index === 0 ? 'primary' : 'info'
}

const getOperationTypeTag = (type) => {
  const map = {
    'UPDATE': 'primary',
    'DELETE': 'danger',
    'BATCH_UPDATE': 'success',
    'ROLLBACK': 'warning'
  }
  return map[type] || 'info'
}

const getOperationTypeName = (type) => {
  const map = {
    'UPDATE': '修改',
    'DELETE': '删除',
    'BATCH_UPDATE': '批量修改',
    'ROLLBACK': '回滚'
  }
  return map[type] || type
}

const viewVersion = (item) => {
  detailData.value = { ...item }
  detailVisible.value = true
}

const compareWithCurrent = async (item) => {
  try {
    const res = await getQuestionDetail(props.questionId)
    compareData.value = {
      version1: { ...item },
      version2: { ...res.data }
    }
    compareVisible.value = true
  } catch (error) {
    ElMessage.error('获取当前版本失败')
  }
}

const rollbackToVersion = (item) => {
  ElMessageBox.confirm('确定要回滚到该版本吗？当前修改将被覆盖。', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    emit('rollback', item)
  }).catch(() => {})
}
</script>

<style scoped>
.history-item {
  padding: 15px;
  background-color: #f8f9fa;
  border-radius: 8px;
  margin-bottom: 10px;
}

.history-header {
  margin-bottom: 15px;
}

.history-info {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 5px;
}

.history-operator {
  font-weight: bold;
  font-size: 14px;
}

.history-note {
  color: #909399;
  font-size: 12px;
  padding: 4px 0;
}

.history-content {
  margin: 15px 0;
}

.content-preview {
  max-height: 100px;
  overflow-y: auto;
  padding: 8px;
  background-color: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  font-size: 13px;
  line-height: 1.6;
}

.history-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px dashed #e4e7ed;
}

.compare-container {
  display: flex;
  gap: 20px;
  min-height: 300px;
}

.compare-version {
  flex: 1;
  padding: 15px;
  background-color: #f8f9fa;
  border-radius: 4px;
}

.compare-version h4 {
  margin-top: 0;
  margin-bottom: 15px;
  padding-bottom: 8px;
  border-bottom: 1px solid #e4e7ed;
  font-size: 14px;
  color: #303133;
}

.compare-content {
  max-height: 300px;
  overflow-y: auto;
  padding: 10px;
  background-color: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  font-size: 13px;
  line-height: 1.6;
}

.detail-container {
  padding: 10px;
}

.detail-item {
  margin-bottom: 15px;
}

.detail-label {
  display: inline-block;
  width: 80px;
  font-weight: bold;
  color: #606266;
}

.detail-content {
  margin-top: 8px;
  padding: 10px;
  background-color: #f8f9fa;
  border-radius: 4px;
  white-space: pre-wrap;
  line-height: 1.6;
}

:deep(.el-timeline-item__node--large) {
  width: 16px;
  height: 16px;
}

:deep(.el-timeline-item__wrapper) {
  padding-left: 25px;
}

:deep(.el-rate) {
  display: inline-block;
}
</style>