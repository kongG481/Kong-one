<template>
  <el-dialog
      v-model="visible"
      title="导出试题"
      width="550px"
      @close="handleClose"
  >
    <!-- 添加切换按钮 -->
    <el-radio-group v-model="exportMode" style="margin-bottom: 20px">
      <el-radio-button value="normal">常规导出</el-radio-button>
      <el-radio-button value="auto">自动组卷</el-radio-button>
    </el-radio-group>

    <!-- 常规导出表单 -->
    <template v-if="exportMode === 'normal'">
      <el-form label-width="100px">
        <el-form-item label="导出格式">
          <el-radio-group v-model="exportForm.format">
            <el-radio value="excel">Excel 格式</el-radio>
            <el-radio value="word">Word 格式</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="导出范围">
          <el-radio-group v-model="exportScope">
            <el-radio value="selected">选中的试题</el-radio>
            <el-radio value="all">当前查询结果</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="排序方式" v-if="exportScope === 'all'">
          <el-select v-model="exportForm.sortField" style="width: 120px">
            <el-option label="创建时间" value="create_time" />
            <el-option label="更新时间" value="update_time" />
            <el-option label="难度" value="difficulty" />
          </el-select>
          <el-select v-model="exportForm.sortOrder" style="width: 100px; margin-left: 10px">
            <el-option label="升序" value="ASC" />
            <el-option label="降序" value="DESC" />
          </el-select>
        </el-form-item>

        <el-form-item label="文件名称">
          <el-input
              v-model="exportForm.fileName"
              placeholder="可选，留空自动生成"
          />
        </el-form-item>

        <el-alert
            title="提示"
            type="info"
            :closable="false"
            show-icon
        >
          <template #default>
            <div>导出文件将自动下载到本地</div>
            <div>如需查看历史导出记录，请在"导出历史"中查看</div>
          </template>
        </el-alert>
      </el-form>
    </template>

    <!-- 自动组卷表单 -->
    <template v-else>
      <el-form label-width="100px">
        <el-form-item label="试卷标题" required>
          <el-input v-model="paperTitle" placeholder="请输入试卷标题" />
        </el-form-item>

        <el-form-item label="学科筛选">
          <el-cascader
              v-model="subjectId"
              :options="subjectOptions"
              :props="{ value: 'id', label: 'name', checkStrictly: true, emitPath: false }"
              placeholder="不限学科"
              clearable
              style="width: 100%"
          />
        </el-form-item>

        <el-divider content-position="left">题型配置</el-divider>

        <div v-for="(item, index) in typeConfigs" :key="index" class="type-config">
          <el-row :gutter="10" align="middle">
            <el-col :span="8">
              <el-tag :type="getTypeTag(item.questionType)">
                {{ getTypeName(item.questionType) }}
              </el-tag>
            </el-col>
            <el-col :span="7">
              <el-input-number
                  v-model="item.questionCount"
                  :min="0"
                  :max="30"
                  size="small"
                  placeholder="数量"
              />
            </el-col>
            <el-col :span="7">
              <el-input-number
                  v-model="item.scorePerQuestion"
                  :min="1"
                  :max="20"
                  size="small"
                  placeholder="分值"
              />
            </el-col>
            <el-col :span="2">
              <el-button
                  type="danger"
                  :icon="Delete"
                  circle
                  size="small"
                  @click="removeTypeConfig(index)"
                  v-if="typeConfigs.length > 1"
              />
            </el-col>
          </el-row>
        </div>

        <el-button type="primary" link @click="addTypeConfig">
          <el-icon><Plus /></el-icon>添加题型
        </el-button>

        <el-divider />

        <el-form-item label="总分预览">
          <el-tag type="warning" size="large">{{ totalScore }} 分</el-tag>
          <el-tag type="success" size="large" style="margin-left: 10px">{{ totalQuestions }} 题</el-tag>
        </el-form-item>

        <el-alert
            title="组卷说明"
            type="info"
            :closable="false"
            show-icon
        >
          <div>• 按难度比例（简单:中等:困难 = 3:5:2）自动抽取</div>
          <div>• 自动避免题目重复</div>
          <div>• 导出包含参考答案的Word试卷</div>
        </el-alert>
      </el-form>
    </template>

    <template #footer>
      <el-button @click="handleClose">取消</el-button>
      <el-button type="primary" :loading="exporting" @click="handleExport">
        {{ exportMode === 'normal' ? '开始导出' : '生成并导出试卷' }}
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, watch, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, Delete } from '@element-plus/icons-vue'
import { autoGeneratePaper, exportPaper, getSubjectTree } from '@/api/question'

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  },
  selectedIds: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['update:modelValue', 'success'])

// ========== 通用变量 ==========
const visible = ref(false)
const exporting = ref(false)

// ========== 常规导出变量 ==========
const exportMode = ref('normal')  // normal: 常规导出, auto: 自动组卷
const exportScope = ref('selected')
const exportForm = reactive({
  format: 'excel',
  questionIds: [],
  sortField: 'create_time',
  sortOrder: 'DESC',
  fileName: ''
})

// ========== 自动组卷变量 ==========
const paperTitle = ref('')
const subjectId = ref(null)
const subjectOptions = ref([])
const typeConfigs = ref([
  { questionType: 1, questionCount: 5, scorePerQuestion: 4 },
  { questionType: 2, questionCount: 5, scorePerQuestion: 4 },
  { questionType: 3, questionCount: 3, scorePerQuestion: 6 },
  { questionType: 4, questionCount: 2, scorePerQuestion: 10 }
])

// 计算总分
const totalScore = computed(() => {
  return typeConfigs.value.reduce((sum, item) => {
    return sum + (item.questionCount * item.scorePerQuestion)
  }, 0)
})

// 计算总题数
const totalQuestions = computed(() => {
  return typeConfigs.value.reduce((sum, item) => sum + item.questionCount, 0)
})

// 题型名称
const getTypeName = (type) => {
  const map = { 1: '单选题', 2: '多选题', 3: '填空题', 4: '简答题' }
  return map[type] || '未知'
}

const getTypeTag = (type) => {
  const map = { 1: 'primary', 2: 'success', 3: 'warning', 4: 'info' }
  return map[type] || ''
}

// 添加题型配置
const addTypeConfig = () => {
  const existingTypes = typeConfigs.value.map(t => t.questionType)
  let nextType = 1
  for (let i = 1; i <= 4; i++) {
    if (!existingTypes.includes(i)) {
      nextType = i
      break
    }
  }
  typeConfigs.value.push({
    questionType: nextType,
    questionCount: 2,
    scorePerQuestion: 5
  })
}

// 删除题型配置
const removeTypeConfig = (index) => {
  typeConfigs.value.splice(index, 1)
}

// 加载学科树
const loadSubjectOptions = async () => {
  try {
    const res = await getSubjectTree()
    if (res.code === 0) {
      subjectOptions.value = res.data
    }
  } catch (error) {
    console.error('加载学科失败', error)
  }
}

// ========== 通用方法 ==========
watch(() => props.modelValue, (val) => {
  visible.value = val
  if (val) {
    exportForm.questionIds = [...props.selectedIds]
    loadSubjectOptions()
    // 重置自动组卷表单
    paperTitle.value = ''
    subjectId.value = null
    typeConfigs.value = [
      { questionType: 1, questionCount: 5, scorePerQuestion: 4 },
      { questionType: 2, questionCount: 5, scorePerQuestion: 4 },
      { questionType: 3, questionCount: 3, scorePerQuestion: 6 },
      { questionType: 4, questionCount: 2, scorePerQuestion: 10 }
    ]
  }
})

// 下载 blob
const downloadBlob = (blob, fileName) => {
  if (!(blob instanceof Blob)) {
    console.error('Invalid blob:', blob)
    throw new Error('文件数据无效')
  }
  const url = window.URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = fileName
  link.style.display = 'none'
  document.body.appendChild(link)
  link.click()
  setTimeout(() => {
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
  }, 100)
}

// ========== 常规导出逻辑 ==========
const handleNormalExport = async () => {
  if (exportScope.value === 'selected' && exportForm.questionIds.length === 0) {
    ElMessage.warning('请先选择要导出的试题')
    return
  }

  const data = {
    format: exportForm.format,
    questionIds: exportScope.value === 'selected' ? exportForm.questionIds : null,
    sortField: exportForm.sortField,
    sortOrder: exportForm.sortOrder,
    fileName: exportForm.fileName || null
  }

  const token = localStorage.getItem('token')
  const response = await fetch('/api/question/export', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': token
    },
    body: JSON.stringify(data)
  })

  if (!response.ok) {
    let errorMessage = '导出失败'
    try {
      const errorData = await response.json()
      errorMessage = errorData.message || errorMessage
    } catch {
      errorMessage = response.statusText || errorMessage
    }
    throw new Error(errorMessage)
  }

  const contentType = response.headers.get('Content-Type')

  if (contentType && contentType.includes('application/json')) {
    const result = await response.json()
    if (result.code === 0 && result.data) {
      const fileResponse = await fetch(result.data, {
        headers: { 'Authorization': token }
      })
      if (!fileResponse.ok) throw new Error('下载文件失败')
      const blob = await fileResponse.blob()
      const fileName = result.data.split('/').pop()
      downloadBlob(blob, fileName)
    } else {
      throw new Error(result.message || '导出失败')
    }
  } else {
    const blob = await response.blob()
    if (blob.size === 0) throw new Error('导出的文件为空')

    const contentDisposition = response.headers.get('Content-Disposition')
    let fileName = exportForm.fileName || `试题导出_${new Date().getTime()}`

    if (contentDisposition) {
      const match = contentDisposition.match(/filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/)
      if (match && match[1]) {
        fileName = decodeURIComponent(match[1].replace(/['"]/g, ''))
      }
    } else {
      const ext = exportForm.format === 'excel' ? '.xlsx' : '.doc'
      fileName = fileName + ext
    }
    downloadBlob(blob, fileName)
  }
}

// ========== 自动组卷逻辑 ==========
const handleAutoGenerate = async () => {
  if (!paperTitle.value) {
    ElMessage.warning('请输入试卷标题')
    return
  }

  if (totalQuestions.value === 0) {
    ElMessage.warning('请至少添加一道题目')
    return
  }

  const data = {
    paperTitle: paperTitle.value,
    subjectId: subjectId.value || null,
    questionTypes: typeConfigs.value.filter(item => item.questionCount > 0)
  }

  // 生成试卷
  const res = await autoGeneratePaper(data)
  if (res.code !== 0) {
    throw new Error(res.message || '组卷失败')
  }

  // 导出试卷
  const exportRes = await exportPaper(res.data)
  const blob = new Blob([exportRes], { type: 'application/msword' })
  downloadBlob(blob, `${res.data.paperTitle}.doc`)
}

// ========== 主导出方法 ==========
const handleExport = async () => {
  exporting.value = true
  try {
    if (exportMode.value === 'normal') {
      await handleNormalExport()
    } else {
      await handleAutoGenerate()
    }
    ElMessage.success(exportMode.value === 'normal' ? '导出成功' : '试卷生成并导出成功')
    emit('success')
    handleClose()
  } catch (error) {
    console.error('导出失败:', error)
    ElMessage.error('导出失败：' + (error.message || '未知错误'))
  } finally {
    exporting.value = false
  }
}

const handleClose = () => {
  visible.value = false
  emit('update:modelValue', false)
}
</script>

<style scoped>
.type-config {
  margin-bottom: 10px;
  padding: 10px;
  background-color: #f5f7fa;
  border-radius: 4px;
}
</style>