<!-- src/components/question/RecycleBin.vue -->
<template>
  <el-dialog
      v-model="dialogVisible"
      title="回收站"
      width="900px"
      @close="handleClose"
  >
    <!-- 搜索栏 -->
    <div class="search-bar">
      <el-input
          v-model="searchKeyword"
          placeholder="搜索题目内容"
          clearable
          @keyup.enter="fetchRecycleBin"
          style="width: 250px"
      >
        <template #append>
          <el-button @click="fetchRecycleBin">
            <el-icon><Search /></el-icon>
          </el-button>
        </template>
      </el-input>
      <el-button type="danger" @click="handleEmptyRecycleBin" v-if="recycleBinList.length > 0">
        清空回收站
      </el-button>
    </div>

    <!-- 统计信息 -->
    <div class="stats-info" v-if="recycleBinList.length > 0">
      共 {{ recycleBinList.length }} 条已删除试题，占用空间约 {{ calculateSize }} KB
    </div>

    <el-table
        :data="filteredList"
        stripe
        v-loading="loading"
        style="width: 100%"
        height="400px"
    >
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column label="题型" width="100">
        <template #default="{ row }">
          <el-tag :type="getQuestionTypeTag(row.questionType)">
            {{ getQuestionTypeName(row.questionType) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="content" label="题目内容" show-overflow-tooltip min-width="200" />
      <el-table-column prop="creatorName" label="创建人" width="100" />
      <el-table-column prop="deleteTime" label="删除时间" width="160" />
      <el-table-column prop="deleteReason" label="删除原因" width="150" show-overflow-tooltip />
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link @click="handlePreview(row)">预览</el-button>
          <el-button type="success" link @click="handleRestore(row)">恢复</el-button>
          <el-button type="danger" link @click="handlePermanentDelete(row)">彻底删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <template #footer>
      <div class="dialog-footer">
        <span class="footer-info">已删除试题将在30天后自动彻底清除</span>
        <el-button @click="dialogVisible = false">关闭</el-button>
      </div>
    </template>
  </el-dialog>

  <!-- 预览对话框 -->
  <el-dialog v-model="previewVisible" title="试题预览" width="600px">
    <div class="preview-content">
      <div class="preview-header">
        <el-tag :type="getQuestionTypeTag(previewData.questionType)">
          {{ getQuestionTypeName(previewData.questionType) }}
        </el-tag>
        <el-rate v-model="previewData.difficulty" :colors="difficultyColors" disabled />
      </div>
      <div class="preview-item">
        <h4>题目：</h4>
        <div class="preview-text">{{ previewData.content }}</div>
      </div>
      <div class="preview-item" v-if="previewData.analysis">
        <h4>解析：</h4>
        <div class="preview-text">{{ previewData.analysis }}</div>
      </div>
      <div class="preview-item" v-if="previewData.deleteReason">
        <h4>删除原因：</h4>
        <el-tag type="danger">{{ previewData.deleteReason }}</el-tag>
      </div>
      <div class="preview-item">
        <h4>删除时间：</h4>
        <span>{{ previewData.deleteTime }}</span>
      </div>
    </div>
    <template #footer>
      <el-button @click="previewVisible = false">关闭</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { getRecycleBin, restoreQuestion, permanentDelete } from '@/api/question'

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update:modelValue', 'restore', 'delete'])

const dialogVisible = ref(props.modelValue)
const loading = ref(false)
const previewVisible = ref(false)
const searchKeyword = ref('')
const recycleBinList = ref([])
const previewData = ref({})

const difficultyColors = ['#99A9BF', '#F7BA2A', '#FF9900']

watch(() => props.modelValue, (val) => {
  dialogVisible.value = val
  if (val) {
    fetchRecycleBin()
  }
})

watch(dialogVisible, (val) => {
  emit('update:modelValue', val)
})

// 过滤后的列表
const filteredList = computed(() => {
  if (!searchKeyword.value) return recycleBinList.value
  return recycleBinList.value.filter(item =>
      (item.content && item.content.includes(searchKeyword.value)) ||
      (item.deleteReason && item.deleteReason.includes(searchKeyword.value))
  )
})

// 计算占用空间（估算）
const calculateSize = computed(() => {
  let total = 0
  recycleBinList.value.forEach(item => {
    total += (item.content ? item.content.length : 0) + (item.analysis ? item.analysis.length : 0)
  })
  return Math.round(total / 1024 * 100) / 100
})

// 获取回收站列表
const fetchRecycleBin = async () => {
  loading.value = true
  try {
    const res = await getRecycleBin()
    recycleBinList.value = res.data || []
  } catch (error) {
    ElMessage.error('获取回收站列表失败')
  } finally {
    loading.value = false
  }
}

const handleClose = () => {
  dialogVisible.value = false
}

// 预览
const handlePreview = (row) => {
  previewData.value = { ...row }
  previewVisible.value = true
}

// 恢复试题
const handleRestore = (row) => {
  ElMessageBox.confirm('确定要恢复该试题吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await restoreQuestion(row.id)
      ElMessage.success('恢复成功')
      emit('restore', row.id)
      fetchRecycleBin()
    } catch (error) {
      ElMessage.error('恢复失败')
    }
  }).catch(() => {})
}

// 彻底删除
const handlePermanentDelete = (row) => {
  ElMessageBox.confirm(
      '确定要彻底删除该试题吗？此操作不可恢复！',
      '警告',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
  ).then(async () => {
    try {
      await permanentDelete(row.id)
      ElMessage.success('彻底删除成功')
      emit('delete', row.id)
      fetchRecycleBin()
    } catch (error) {
      ElMessage.error('删除失败')
    }
  }).catch(() => {})
}

// 清空回收站
const handleEmptyRecycleBin = () => {
  ElMessageBox.confirm(
      '确定要清空回收站吗？所有试题将被永久删除！',
      '警告',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
  ).then(async () => {
    try {
      for (const item of recycleBinList.value) {
        await permanentDelete(item.id)
      }
      ElMessage.success('清空回收站成功')
      fetchRecycleBin()
    } catch (error) {
      ElMessage.error('清空回收站失败')
    }
  }).catch(() => {})
}

// 工具函数
const getQuestionTypeName = (type) => {
  const map = { 1: '单选题', 2: '多选题', 3: '填空题', 4: '简答题' }
  return map[type] || '未知'
}

const getQuestionTypeTag = (type) => {
  const map = { 1: 'primary', 2: 'success', 3: 'warning', 4: 'info' }
  return map[type] || ''
}
</script>

<style scoped>
.search-bar {
  margin-bottom: 15px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.stats-info {
  margin-bottom: 15px;
  padding: 8px 12px;
  background-color: #f0f9ff;
  border-radius: 4px;
  color: #606266;
  font-size: 13px;
}

.dialog-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.footer-info {
  color: #909399;
  font-size: 12px;
}

.preview-content {
  padding: 10px;
}

.preview-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 10px;
  border-bottom: 1px solid #eee;
}

.preview-item {
  margin-bottom: 20px;
}

.preview-item h4 {
  margin: 0 0 8px 0;
  font-size: 14px;
  color: #606266;
}

.preview-text {
  padding: 10px;
  background-color: #f8f9fa;
  border-radius: 4px;
  line-height: 1.6;
  white-space: pre-wrap;
}

:deep(.el-rate) {
  display: inline-block;
}
</style>