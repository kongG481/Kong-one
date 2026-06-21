<template>
  <el-dialog
      v-model="visible"
      title="高级查询"
      width="800px"
      :close-on-click-modal="false"
      @close="handleClose"
  >
    <el-form
        ref="formRef"
        :model="queryForm"
        label-width="100px"
        label-position="right"
    >
      <!-- 基础查询 -->
      <el-divider content-position="left">基础条件</el-divider>

      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="关键词">
            <el-input
                v-model="queryForm.keyword"
                placeholder="请输入题目关键词"
                clearable
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="题型">
            <el-select
                v-model="queryForm.questionType"
                placeholder="请选择题型"
                clearable
                style="width: 100%"
            >
              <el-option label="单选题" :value="1" />
              <el-option label="多选题" :value="2" />
              <el-option label="填空题" :value="3" />
              <el-option label="简答题" :value="4" />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="学科">
            <el-tree-select
                v-model="queryForm.subjectId"
                :data="subjectTree"
                :props="{ label: 'name', value: 'id', children: 'children' }"
                placeholder="请选择学科"
                clearable
                check-strictly
                style="width: 100%"
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="难度">
            <el-select
                v-model="queryForm.difficulty"
                placeholder="请选择难度"
                clearable
                style="width: 100%"
            >
              <el-option label="简单" :value="1" />
              <el-option label="中等" :value="2" />
              <el-option label="困难" :value="3" />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="创建时间">
            <el-date-picker
                v-model="dateRange"
                type="daterange"
                range-separator="至"
                start-placeholder="开始日期"
                end-placeholder="结束日期"
                value-format="YYYY-MM-DD HH:mm:ss"
                style="width: 100%"
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="我的试题">
            <el-switch v-model="queryForm.myQuestions" />
            <span style="margin-left: 10px; color: #909399; font-size: 12px">
              仅显示自己创建的试题
            </span>
          </el-form-item>
        </el-col>
      </el-row>

      <!-- 高级组合 -->
      <el-divider content-position="left">高级组合</el-divider>

      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="多题型">
            <el-select
                v-model="queryForm.questionTypeList"
                multiple
                placeholder="可多选"
                clearable
                style="width: 100%"
            >
              <el-option label="单选题" :value="1" />
              <el-option label="多选题" :value="2" />
              <el-option label="填空题" :value="3" />
              <el-option label="简答题" :value="4" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="多难度">
            <el-select
                v-model="queryForm.difficultyList"
                multiple
                placeholder="可多选"
                clearable
                style="width: 100%"
            >
              <el-option label="简单" :value="1" />
              <el-option label="中等" :value="2" />
              <el-option label="困难" :value="3" />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="多学科">
            <el-tree-select
                v-model="queryForm.subjectIdList"
                :data="subjectTree"
                :props="{ label: 'name', value: 'id', children: 'children' }"
                placeholder="可多选"
                multiple
                clearable
                check-strictly
                style="width: 100%"
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="排序">
            <el-select v-model="queryForm.sortField" style="width: 45%">
              <el-option label="创建时间" value="create_time" />
              <el-option label="更新时间" value="update_time" />
              <el-option label="难度" value="difficulty" />
            </el-select>
            <el-select v-model="queryForm.sortOrder" style="width: 45%; margin-left: 10%">
              <el-option label="升序" value="ASC" />
              <el-option label="降序" value="DESC" />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="高亮显示">
            <el-switch v-model="queryForm.highlight" />
            <span style="margin-left: 10px; color: #909399; font-size: 12px">
              关键词高亮显示
            </span>
          </el-form-item>
        </el-col>
      </el-row>

      <!-- 保存查询条件 -->
      <el-divider content-position="left">保存查询条件</el-divider>

      <el-row>
        <el-col :span="24">
          <el-form-item label="查询名称">
            <el-input
                v-model="saveQueryName"
                placeholder="输入查询名称后点击保存"
                style="width: 60%"
            />
            <el-button
                type="primary"
                style="margin-left: 10px"
                @click="saveCondition"
                :disabled="!saveQueryName"
            >
              保存当前条件
            </el-button>
          </el-form-item>
        </el-col>
      </el-row>

      <!-- 历史查询条件 -->
      <el-divider content-position="left">历史查询条件</el-divider>

      <el-table
          :data="queryHistory"
          style="width: 100%"
          size="small"
          max-height="200"
          v-loading="historyLoading"
      >
        <!-- 修改这里：确保 prop 名称与后端返回的字段名一致 -->
        <el-table-column prop="queryName" label="查询名称" min-width="150">
        </el-table-column>
        <el-table-column prop="resultCount" label="结果数量" width="100" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="row.resultCount > 0 ? 'success' : 'info'">
              {{ row.resultCount }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="160">
          <template #default="{ row }">
            {{ row.createTime || row.create_time }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" align="center">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="loadHistoryCondition(row)">
              加载
            </el-button>
            <el-button link type="danger" size="small" @click="deleteHistory(row.id)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-form>

    <template #footer>
      <el-button @click="handleClose">取消</el-button>
      <el-button type="primary" @click="handleSubmit">查询</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getSubjectTree } from '@/api/question'
import {
  saveQueryCondition,
  getQueryHistory,
  deleteQueryHistory
} from '@/api/question'

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update:modelValue', 'search'])

const visible = ref(false)
const formRef = ref()
const subjectTree = ref([])
const queryHistory = ref([])
const saveQueryName = ref('')

const queryForm = reactive({
  keyword: '',
  questionType: null,
  subjectId: null,
  difficulty: null,
  myQuestions: false,
  startTime: null,
  endTime: null,
  difficultyList: [],
  subjectIdList: [],
  questionTypeList: [],
  sortField: 'create_time',
  sortOrder: 'DESC',
  highlight: false,
  pageNum: 1,
  pageSize: 20
})

const dateRange = ref(null)

// 添加 historyLoading
const historyLoading = ref(false)

// 监听日期范围变化
watch(dateRange, (val) => {
  if (val) {
    queryForm.startTime = val[0]
    queryForm.endTime = val[1]
  } else {
    queryForm.startTime = null
    queryForm.endTime = null
  }
})

// 监听 visible
watch(() => props.modelValue, (val) => {
  visible.value = val
  if (val) {
    loadSubjectTree()
    loadQueryHistory()
  }
})


// 加载学科树
const loadSubjectTree = async () => {
  try {
    const res = await getSubjectTree()
    if (res.code === 0) {
      subjectTree.value = res.data
    }
  } catch (error) {
    console.error('加载学科树失败', error)
  }
}

// 加载查询历史 - 确保正确显示 queryName
const loadQueryHistory = async () => {
  historyLoading.value = true
  try {
    const res = await getQueryHistory()
    if (res.code === 0) {
      queryHistory.value = res.data || []
      // 调试：打印数据查看 queryName 是否存在
      console.log('查询历史数据:', queryHistory.value)
    }
  } catch (error) {
    console.error('加载查询历史失败', error)
  } finally {
    historyLoading.value = false
  }
}

// 保存查询条件 - 确保 queryName 正确传递
const saveCondition = async () => {
  if (!saveQueryName.value) {
    ElMessage.warning('请输入查询名称')
    return
  }

  try {
    // 深拷贝查询条件，排除分页参数
    const queryData = JSON.parse(JSON.stringify(queryForm))
    delete queryData.pageNum
    delete queryData.pageSize
    delete queryData.highlight

    console.log('保存查询条件:', {
      queryName: saveQueryName.value,
      query: queryData
    })

    const res = await saveQueryCondition({
      queryName: saveQueryName.value.trim(),
      query: queryData
    })

    if (res.code === 0) {
      ElMessage.success('保存成功')
      saveQueryName.value = ''
      loadQueryHistory()
    } else {
      ElMessage.error(res.message)
    }
  } catch (error) {
    console.error('保存查询条件失败', error)
    ElMessage.error('保存失败')
  }
}

// 加载历史查询条件 - 处理父级学科
const loadHistoryCondition = (history) => {
  try {

    // 确保 history 对象存在
    if (!history) {
      ElMessage.warning('查询条件不存在')
      return
    }
    let condition = {}
    if (typeof history.queryCondition === 'string') {
      condition = JSON.parse(history.queryCondition)
    } else {
      condition = history.queryCondition
    }

    // 清空当前表单
    Object.keys(queryForm).forEach(key => {
      if (key !== 'pageNum' && key !== 'pageSize') {
        queryForm[key] = null
      }
    })
    queryForm.difficultyList = []
    queryForm.subjectIdList = []
    queryForm.questionTypeList = []
    queryForm.sortField = 'create_time'
    queryForm.sortOrder = 'DESC'
    queryForm.highlight = false

    // 加载条件
    Object.assign(queryForm, condition)

    // 处理日期范围
    if (condition.startTime && condition.endTime) {
      dateRange.value = [condition.startTime, condition.endTime]
    } else {
      dateRange.value = null
    }

    ElMessage.success(`已加载查询条件: ${history.queryName || '未命名查询'}`)
  } catch (error) {
    console.error('解析查询条件失败', error)
    ElMessage.error('加载失败')
  }
}

// 删除历史查询条件
const deleteHistory = async (id) => {
  try {
    await ElMessageBox.confirm('确定删除该查询条件吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    const res = await deleteQueryHistory(id)
    if (res.code === 0) {
      ElMessage.success('删除成功')
      loadQueryHistory()
    } else {
      ElMessage.error(res.message)
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败', error)
    }
  }
}

// 提交查询
const handleSubmit = () => {
  const queryData = { ...queryForm }
  delete queryData.pageNum
  delete queryData.pageSize
  emit('search', queryData)
  handleClose()
}

// 关闭对话框
const handleClose = () => {
  visible.value = false
  emit('update:modelValue', false)
}
</script>

<style scoped>
:deep(.el-divider__text) {
  font-weight: bold;
  color: #409eff;
}
</style>