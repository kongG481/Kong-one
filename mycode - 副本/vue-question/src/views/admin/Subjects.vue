<!-- src/views/admin/Subjects.vue -->
<template>
  <div class="subjects-container">
    <!-- 顶部操作栏 -->
    <div class="operation-bar">
      <el-button type="primary" @click="handleAdd">
        <el-icon><Plus /></el-icon>新增学科
      </el-button>
      <el-button type="success" @click="handleRefresh">
        <el-icon><Refresh /></el-icon>刷新
      </el-button>
    </div>

    <!-- 学科树和表单区域 -->
    <el-row :gutter="20">
      <!-- 左侧学科树 -->
      <el-col :span="10">
        <el-card class="tree-card">
          <template #header>
            <div class="card-header">
              <span>学科结构</span>
              <el-tooltip content="展开/折叠全部" placement="top">
                <el-button :icon="Expand" circle size="small" @click="expandAll" />
              </el-tooltip>
            </div>
          </template>
          <el-tree
              ref="treeRef"
              :data="subjectTree"
              :props="defaultProps"
              node-key="id"
              highlight-current
              :expand-on-click-node="false"
              @node-click="handleNodeClick"
          >
            <template #default="{ node, data }">
              <span class="custom-tree-node">
                <span class="node-label">
                  <el-icon v-if="data.children && data.children.length"><Folder /></el-icon>
                  <el-icon v-else><Document /></el-icon>
                  {{ data.name }}
                </span>
                <span class="node-actions">
                  <el-button type="primary" link size="small" @click.stop="handleEdit(data)">编辑</el-button>
                  <el-button type="danger" link size="small" @click.stop="handleDelete(data)">删除</el-button>
                  <el-button type="success" link size="small" @click.stop="handleAddChild(data)">添加子级</el-button>
                </span>
              </span>
            </template>
          </el-tree>
        </el-card>
      </el-col>

      <!-- 右侧表单 -->
      <el-col :span="14">
        <el-card class="form-card">
          <template #header>
            <div class="card-header">
              <span>{{ formTitle }}</span>
            </div>
          </template>

          <el-form
              ref="formRef"
              :model="subjectForm"
              :rules="formRules"
              label-width="100px"
          >
            <el-form-item label="学科名称" prop="name">
              <el-input v-model="subjectForm.name" placeholder="请输入学科名称" />
            </el-form-item>

            <el-form-item label="上级学科" prop="parentId">
              <el-tree-select
                  v-model="subjectForm.parentId"
                  :data="subjectTree"
                  :props="{ value: 'id', label: 'name', children: 'children' }"
                  placeholder="请选择上级学科"
                  check-strictly
                  clearable
                  filterable
                  :disabled="!subjectForm.id"
              />
              <div v-if="!subjectForm.id" class="form-tip">
                <el-tag size="small" type="info">新增学科暂不选择上级，保存后可移动</el-tag>
              </div>
            </el-form-item>

            <el-form-item label="排序" prop="sortOrder">
              <el-input-number v-model="subjectForm.sortOrder" :min="0" :max="999" />
              <span class="unit">数字越小越靠前</span>
            </el-form-item>

            <el-form-item label="状态" prop="status">
              <el-radio-group v-model="subjectForm.status">
                <el-radio :value="1">启用</el-radio>
                <el-radio :value="0">禁用</el-radio>
              </el-radio-group>
            </el-form-item>

            <el-form-item>
              <el-button type="primary" @click="submitForm" :loading="submitting">
                {{ subjectForm.id ? '保存修改' : '添加学科' }}
              </el-button>
              <el-button @click="resetForm">重置</el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <!-- 学科统计卡片 -->
        <el-card class="stats-card" style="margin-top: 20px;">
          <template #header>
            <div class="card-header">
              <span>学科统计</span>
            </div>
          </template>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="总学科数">{{ stats.total }}</el-descriptions-item>
            <el-descriptions-item label="一级学科">{{ stats.level1 }}</el-descriptions-item>
            <el-descriptions-item label="二级学科">{{ stats.level2 }}</el-descriptions-item>
            <el-descriptions-item label="三级学科">{{ stats.level3 }}</el-descriptions-item>
            <el-descriptions-item label="启用学科">{{ stats.enabled }}</el-descriptions-item>
            <el-descriptions-item label="禁用学科">{{ stats.disabled }}</el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Folder, Document, Expand } from '@element-plus/icons-vue'
import {
  getSubjectTree,
  addSubject,
  updateSubject,
  deleteSubject,
  getSubjectStats
} from '@/api/subject'

// 数据定义
const loading = ref(false)
const submitting = ref(false)
const treeRef = ref(null)
const formRef = ref(null)
const subjectTree = ref([])
const stats = ref({
  total: 0,
  level1: 0,
  level2: 0,
  level3: 0,
  enabled: 0,
  disabled: 0
})

// 表单数据
const subjectForm = reactive({
  id: null,
  name: '',
  parentId: null,
  sortOrder: 0,
  status: 1
})

// 树形控件属性
const defaultProps = {
  children: 'children',
  label: 'name'
}

// 表单标题
const formTitle = computed(() => {
  return subjectForm.id ? '编辑学科' : '新增学科'
})

// 表单验证规则
const formRules = {
  name: [
    { required: true, message: '请输入学科名称', trigger: 'blur' },
    { min: 2, max: 50, message: '长度在 2 到 50 个字符', trigger: 'blur' }
  ],
  sortOrder: [
    { required: true, message: '请输入排序号', trigger: 'blur' }
  ]
}

onMounted(() => {
  loadSubjectTree()
  loadStats()
})

// 加载学科树
const loadSubjectTree = async () => {
  loading.value = true
  try {
    const res = await getSubjectTree()
    subjectTree.value = res.data || []
    // 更新统计信息
    updateStatsFromTree()
  } catch (error) {
    ElMessage.error('获取学科树失败')
    subjectTree.value = []
  } finally {
    loading.value = false
  }
}

// 加载统计信息
const loadStats = async () => {
  try {
    const res = await getSubjectStats()
    stats.value = res.data || {
      total: 0,
      level1: 0,
      level2: 0,
      level3: 0,
      enabled: 0,
      disabled: 0
    }
  } catch (error) {
    console.error('获取统计信息失败', error)
    // 从树数据更新统计
    updateStatsFromTree()
  }
}

// 从树数据更新统计
const updateStatsFromTree = () => {
  const allSubjects = []

  // 递归获取所有节点
  const collectSubjects = (nodes) => {
    nodes.forEach(node => {
      allSubjects.push(node)
      if (node.children && node.children.length) {
        collectSubjects(node.children)
      }
    })
  }

  collectSubjects(subjectTree.value)

  stats.value = {
    total: allSubjects.length,
    level1: subjectTree.value.length,
    level2: allSubjects.filter(s => s.level === 2).length,
    level3: allSubjects.filter(s => s.level === 3).length,
    enabled: allSubjects.filter(s => s.status === 1).length,
    disabled: allSubjects.filter(s => s.status === 0).length
  }
}

// 刷新
const handleRefresh = () => {
  loadSubjectTree()
  loadStats()
}

// 展开/折叠全部
const expandAll = () => {
  const nodes = treeRef.value?.store?.nodesMap
  const allExpanded = Array.from(nodes.values()).every(node => node.expanded)

  for (let key in nodes) {
    nodes[key].expanded = !allExpanded
  }
}

// 点击节点
const handleNodeClick = (data) => {
  // 清空表单，显示当前节点的信息
  resetForm()
  subjectForm.id = data.id
  subjectForm.name = data.name
  subjectForm.parentId = data.parentId
  subjectForm.sortOrder = data.sortOrder || 0
  subjectForm.status = data.status || 1
}

// 新增
const handleAdd = () => {
  resetForm()
  subjectForm.parentId = null
}

// 新增子级
const handleAddChild = (data) => {
  resetForm()
  subjectForm.parentId = data.id
  ElMessage.info(`将在 "${data.name}" 下添加子学科`)
}

// 编辑
const handleEdit = (data) => {
  subjectForm.id = data.id
  subjectForm.name = data.name
  subjectForm.parentId = data.parentId
  subjectForm.sortOrder = data.sortOrder || 0
  subjectForm.status = data.status || 1
}

// 删除
const handleDelete = (data) => {
  // 检查是否有子节点
  if (data.children && data.children.length > 0) {
    ElMessage.warning('该学科下有子学科，不能删除')
    return
  }

  ElMessageBox.confirm(
      `确定要删除学科 "${data.name}" 吗？`,
      '警告',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
  ).then(async () => {
    try {
      await deleteSubject(data.id)
      ElMessage.success('删除成功')
      loadSubjectTree()
      loadStats()
      resetForm()
    } catch (error) {
      ElMessage.error('删除失败')
    }
  }).catch(() => {})
}

// 提交表单
const submitForm = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (valid) {
      submitting.value = true
      try {
        if (subjectForm.id) {
          await updateSubject(subjectForm)
          ElMessage.success('更新成功')
        } else {
          await addSubject(subjectForm)
          ElMessage.success('添加成功')
        }
        loadSubjectTree()
        loadStats()
        resetForm()
      } catch (error) {
        ElMessage.error(error.response?.data?.message || '操作失败')
      } finally {
        submitting.value = false
      }
    }
  })
}

// 重置表单
const resetForm = () => {
  subjectForm.id = null
  subjectForm.name = ''
  subjectForm.parentId = null
  subjectForm.sortOrder = 0
  subjectForm.status = 1
  formRef.value?.clearValidate()
}
</script>

<style scoped>
.subjects-container {
  padding: 20px;
}

.operation-bar {
  margin-bottom: 20px;
  display: flex;
  gap: 10px;
}

.tree-card,
.form-card {
  height: fit-content;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.custom-tree-node {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 14px;
  padding-right: 8px;
}

.node-label {
  display: flex;
  align-items: center;
  gap: 5px;
}

.node-actions {
  display: none;
}

.custom-tree-node:hover .node-actions {
  display: inline-flex;
  gap: 5px;
}

.unit {
  margin-left: 10px;
  color: #999;
  font-size: 12px;
}

.form-tip {
  margin-top: 5px;
}

.stats-card {
  margin-top: 20px;
}

:deep(.el-descriptions) {
  margin-top: 10px;
}

:deep(.el-tree-node__content) {
  height: 36px;
}
</style>