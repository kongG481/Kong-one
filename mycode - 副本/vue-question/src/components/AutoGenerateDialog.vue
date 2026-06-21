<template>
  <el-dialog
      v-model="visible"
      title="自动组卷"
      width="600px"
      @close="handleClose"
  >
    <el-form :model="formData" label-width="100px">
      <el-form-item label="试卷标题" required>
        <el-input v-model="formData.paperTitle" placeholder="请输入试卷标题" />
      </el-form-item>

      <el-form-item label="所属学科">
        <el-cascader
            v-model="formData.subjectId"
            :options="subjectOptions"
            :props="{ value: 'id', label: 'name', checkStrictly: true, emitPath: false }"
            placeholder="请选择学科"
            clearable
            style="width: 100%"
        />
      </el-form-item>

      <el-divider content-position="left">题型配置</el-divider>

      <div v-for="(item, index) in formData.questionTypes" :key="index" class="type-config">
        <el-row :gutter="10">
          <el-col :span="8">
            <el-form-item label="题型" label-width="60px">
              <el-tag :type="getTypeTag(item.questionType)">
                {{ getTypeName(item.questionType) }}
              </el-tag>
            </el-form-item>
          </el-col>
          <el-col :span="7">
            <el-form-item label="数量" label-width="40px">
              <el-input-number
                  v-model="item.questionCount"
                  :min="0"
                  :max="50"
                  size="small"
              />
            </el-form-item>
          </el-col>
          <el-col :span="7">
            <el-form-item label="分值" label-width="40px">
              <el-input-number
                  v-model="item.scorePerQuestion"
                  :min="1"
                  :max="100"
                  size="small"
              />
            </el-form-item>
          </el-col>
          <el-col :span="2">
            <el-button
                type="danger"
                :icon="Delete"
                circle
                size="small"
                @click="removeType(index)"
                v-if="formData.questionTypes.length > 1"
            />
          </el-col>
        </el-row>
      </div>

      <el-button type="primary" link @click="addType">
        <el-icon><Plus /></el-icon>添加题型
      </el-button>

      <el-divider />

      <el-form-item label="总分预览">
        <el-tag type="warning" size="large">
          总分：{{ calculateTotalScore }} 分
        </el-tag>
        <el-tag type="success" size="large" style="margin-left: 10px">
          总题数：{{ calculateTotalQuestions }} 题
        </el-tag>
      </el-form-item>

      <el-alert
          title="组卷规则"
          type="info"
          :closable="false"
          show-icon
      >
        <div>• 按难度比例抽取：简单30% / 中等50% / 困难20%</div>
        <div>• 自动避免题目重复</div>
        <div>• 如果题库数量不足，会从其他难度补充</div>
      </el-alert>
    </el-form>

    <template #footer>
      <el-button @click="handleClose">取消</el-button>
      <el-button type="primary" :loading="generating" @click="handleGenerate">
        生成试卷
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
  }
})

const emit = defineEmits(['update:modelValue'])

const visible = ref(false)
const generating = ref(false)
const subjectOptions = ref([])

const formData = reactive({
  paperTitle: '',
  subjectId: null,
  questionTypes: [
    { questionType: 1, questionCount: 5, scorePerQuestion: 4 },
    { questionType: 2, questionCount: 5, scorePerQuestion: 4 },
    { questionType: 3, questionCount: 3, scorePerQuestion: 6 },
    { questionType: 4, questionCount: 2, scorePerQuestion: 10 }
  ]
})

// 计算总分
const calculateTotalScore = computed(() => {
  return formData.questionTypes.reduce((sum, item) => {
    return sum + (item.questionCount * item.scorePerQuestion)
  }, 0)
})

// 计算总题数
const calculateTotalQuestions = computed(() => {
  return formData.questionTypes.reduce((sum, item) => sum + item.questionCount, 0)
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

// 添加题型
const addType = () => {
  const nextType = getNextAvailableType()
  formData.questionTypes.push({
    questionType: nextType,
    questionCount: 2,
    scorePerQuestion: 5
  })
}

// 删除题型
const removeType = (index) => {
  formData.questionTypes.splice(index, 1)
}

// 获取下一个可用题型
const getNextAvailableType = () => {
  const existingTypes = formData.questionTypes.map(t => t.questionType)
  for (let i = 1; i <= 4; i++) {
    if (!existingTypes.includes(i)) return i
  }
  return 1
}

// 加载学科树
const loadSubjectTree = async () => {
  try {
    const res = await getSubjectTree()
    if (res.code === 0) {
      subjectOptions.value = res.data
    }
  } catch (error) {
    console.error('加载学科树失败', error)
  }
}

// 生成试卷
const handleGenerate = async () => {
  if (!formData.paperTitle) {
    ElMessage.warning('请输入试卷标题')
    return
  }

  if (calculateTotalQuestions.value === 0) {
    ElMessage.warning('请至少添加一道题目')
    return
  }

  generating.value = true

  try {
    // 生成试卷
    const res = await autoGeneratePaper(formData)
    if (res.code === 0) {
      ElMessage.success('试卷生成成功，正在导出...')

      // 导出试卷
      const exportRes = await exportPaper(res.data)
      const blob = new Blob([exportRes], { type: 'application/msword' })
      const url = window.URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = `${res.data.paperTitle}.doc`
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
      window.URL.revokeObjectURL(url)

      ElMessage.success('导出成功')
      handleClose()
    } else {
      ElMessage.error(res.message || '组卷失败')
    }
  } catch (error) {
    console.error('组卷失败', error)
    ElMessage.error('组卷失败：' + (error.message || '未知错误'))
  } finally {
    generating.value = false
  }
}

watch(() => props.modelValue, (val) => {
  visible.value = val
  if (val) {
    loadSubjectTree()
    // 重置表单
    formData.paperTitle = ''
    formData.subjectId = null
    formData.questionTypes = [
      { questionType: 1, questionCount: 5, scorePerQuestion: 4 },
      { questionType: 2, questionCount: 5, scorePerQuestion: 4 },
      { questionType: 3, questionCount: 3, scorePerQuestion: 6 },
      { questionType: 4, questionCount: 2, scorePerQuestion: 10 }
    ]
  }
})

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