<template>
  <el-dialog
      v-model="visible"
      title="导出历史"
      width="900px"
      @close="handleClose"
  >
    <el-table :data="historyList" stripe v-loading="loading" border>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="userName" label="导出人" width="100" v-if="isAdmin" />
      <el-table-column label="导出格式" width="90">
        <template #default="{ row }">
          <el-tag :type="row.exportFormat === 'excel' ? 'success' : 'primary'" size="small">
            {{ row.exportFormat === 'excel' ? 'Excel' : 'Word' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="questionCount" label="导出数量" width="80" align="center" />
      <el-table-column prop="createTime" label="导出时间" width="160" />
      <el-table-column label="文件状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.available ? 'success' : 'danger'" size="small">
            {{ row.available ? '可用' : '已过期' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="文件大小" width="100">
        <template #default="{ row }">
          {{ formatFileSize(row.fileSize) }}
        </template>
      </el-table-column>
      <el-table-column prop="fileName" label="文件名" min-width="200" show-overflow-tooltip />
      <el-table-column label="操作" width="130" fixed="right">
        <template #default="{ row }">
          <el-button
              link
              type="primary"
              size="small"
              @click="downloadFile(row)"
              :disabled="!row.available"
          >
            下载
          </el-button>
          <el-button
              link
              type="danger"
              size="small"
              @click="deleteHistory(row.id)"
          >
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-empty v-if="!loading && historyList.length === 0" description="暂无导出记录" />
  </el-dialog>
</template>

<script setup>
import { ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getExportHistory,
  getAllExportHistory,
  deleteExportTask,
  downloadExportFile
} from '@/api/question'

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  },
  isAdmin: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update:modelValue'])

const visible = ref(false)
const loading = ref(false)
const historyList = ref([])

watch(() => props.modelValue, async (val) => {
  visible.value = val
  if (val) {
    await loadHistory()
  }
})

// 加载导出历史
const loadHistory = async () => {
  loading.value = true
  try {
    const api = props.isAdmin ? getAllExportHistory : getExportHistory
    const res = await api()
    if (res.code === 0) {
      historyList.value = (res.data || []).map(item => ({
        ...item,
        available: item.available !== undefined ? item.available : true,
        fileSize: item.fileSize || 0,
        fileName: item.fileUrl ? item.fileUrl.split(/[\\/]/).pop() : '未知'
      }))
    } else {
      ElMessage.error(res.message)
    }
  } catch (error) {
    console.error('加载导出历史失败', error)
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

// 修改 downloadFile 方法
const downloadFile = async (row) => {
  try {
    // 直接使用 a 标签的 href 属性，不通过 createObjectURL
    // 因为后端返回的是文件路径，直接访问即可
    const token = localStorage.getItem('token')
    const downloadUrl = `/api/question/export/download/${row.id}`

    // 创建隐藏的 a 标签
    const link = document.createElement('a')
    link.href = downloadUrl
    link.download = ''  // 让浏览器自动处理文件名
    link.style.display = 'none'

    // 添加 token 到请求头（通过 fetch 方式）
    // 如果是简单的 a 标签，无法添加自定义请求头，需要改用 fetch

    // 方案1：使用 fetch 获取 blob
    const response = await fetch(downloadUrl, {
      headers: {
        'Authorization': token
      }
    })

    if (!response.ok) {
      throw new Error('下载失败')
    }

    const blob = await response.blob()

    // 验证 blob 是否有效
    if (blob.size === 0) {
      throw new Error('文件为空')
    }

    // 获取文件名
    let fileName = `export_${row.id}`
    const contentDisposition = response.headers.get('Content-Disposition')
    if (contentDisposition) {
      const match = contentDisposition.match(/filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/)
      if (match && match[1]) {
        fileName = decodeURIComponent(match[1].replace(/['"]/g, ''))
      }
    }

    // 创建 blob URL
    const blobUrl = window.URL.createObjectURL(blob)
    const link2 = document.createElement('a')
    link2.href = blobUrl
    link2.download = fileName
    link2.style.display = 'none'

    document.body.appendChild(link2)
    link2.click()

    // 清理
    setTimeout(() => {
      document.body.removeChild(link2)
      window.URL.revokeObjectURL(blobUrl)
    }, 100)

    ElMessage.success('下载成功')
  } catch (error) {
    console.error('下载失败', error)
    ElMessage.error('下载失败：' + (error.message || '未知错误'))
  }
}

// 删除导出记录
const deleteHistory = async (id) => {
  try {
    await ElMessageBox.confirm('确定删除该导出记录吗？删除后文件也将被清除。', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    const res = await deleteExportTask(id)
    if (res.code === 0) {
      ElMessage.success('删除成功')
      await loadHistory()
    } else {
      ElMessage.error(res.message)
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败', error)
    }
  }
}

// 格式化文件大小
const formatFileSize = (bytes) => {
  if (!bytes || bytes === 0) return '-'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(2) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(2) + ' MB'
}

const handleClose = () => {
  visible.value = false
  emit('update:modelValue', false)
}
</script>