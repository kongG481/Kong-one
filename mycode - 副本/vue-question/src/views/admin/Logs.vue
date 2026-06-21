<!-- src/views/admin/Logs.vue -->
<template>
  <div class="logs-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>操作日志</span>
        </div>
      </template>

      <!-- 搜索条件 -->
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="操作用户">
          <el-input
              v-model="searchForm.username"
              placeholder="用户名/姓名"
              clearable
              style="width: 200px"
          />
        </el-form-item>

        <el-form-item label="操作类型">
          <el-select v-model="searchForm.operationType" placeholder="全部" clearable style="width: 150px">
            <el-option label="登录" value="LOGIN" />
            <el-option label="新增" value="INSERT" />
            <el-option label="修改" value="UPDATE" />
            <el-option label="删除" value="DELETE" />
          </el-select>
        </el-form-item>

        <el-form-item label="时间范围">
          <el-date-picker
              v-model="dateRange"
              type="datetimerange"
              range-separator="至"
              start-placeholder="开始时间"
              end-placeholder="结束时间"
              :shortcuts="dateShortcuts"
              style="width: 400px"
          />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="resetSearch">重置</el-button>
          <el-button @click="exportLogs">导出</el-button>
        </el-form-item>
      </el-form>

      <!-- 日志列表 -->
      <el-table :data="logList" border stripe v-loading="loading">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="username" label="工号" width="120" />
        <el-table-column prop="realName" label="姓名" width="120" />
        <el-table-column prop="operationType" label="操作类型" width="120">
          <template #default="{ row }">
            <el-tag :type="getOperationTypeTag(row.operationType)">
              {{ getOperationTypeText(row.operationType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="operationDesc" label="操作描述" min-width="200" show-overflow-tooltip />
        <el-table-column prop="ipAddress" label="IP地址" width="150" />
        <el-table-column prop="createTime" label="操作时间" width="180" />
      </el-table>

      <!-- 分页 -->
      <el-pagination
          v-model:current-page="pageNum"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
          class="pagination"
      />
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, watch } from 'vue';
import { ElMessage } from 'element-plus';
import { getLogs } from '@/api/admin';

const loading = ref(false);
const logList = ref([]);
const total = ref(0);
const pageNum = ref(1);
const pageSize = ref(10);

// 搜索表单
const searchForm = reactive({
  username: '',
  operationType: '',
  startTime: '',
  endTime: ''
});

// 日期范围
const dateRange = ref([]);

// 日期快捷选项
const dateShortcuts = [
  {
    text: '最近一小时',
    value: () => {
      const end = new Date();
      const start = new Date();
      start.setTime(start.getTime() - 3600 * 1000);
      return [start, end];
    }
  },
  {
    text: '今天',
    value: () => {
      const end = new Date();
      const start = new Date();
      start.setHours(0, 0, 0, 0);
      return [start, end];
    }
  },
  {
    text: '最近三天',
    value: () => {
      const end = new Date();
      const start = new Date();
      start.setTime(start.getTime() - 3600 * 1000 * 24 * 3);
      return [start, end];
    }
  },
  {
    text: '最近一周',
    value: () => {
      const end = new Date();
      const start = new Date();
      start.setTime(start.getTime() - 3600 * 1000 * 24 * 7);
      return [start, end];
    }
  }
];

// 获取操作类型标签
const getOperationTypeTag = (type) => {
  const map = {
    'LOGIN': 'info',
    'INSERT': 'success',
    'UPDATE': 'warning',
    'DELETE': 'danger'
  };
  return map[type] || 'info';
};

// 获取操作类型文本
const getOperationTypeText = (type) => {
  const map = {
    'LOGIN': '登录',
    'INSERT': '新增',
    'UPDATE': '修改',
    'DELETE': '删除'
  };
  return map[type] || type;
};

// 获取日志列表
const fetchLogs = async () => {
  loading.value = true;
  try {
    // 处理日期范围
    if (dateRange.value && dateRange.value.length === 2) {
      searchForm.startTime = dateRange.value[0].toISOString().slice(0, 19).replace('T', ' ');
      searchForm.endTime = dateRange.value[1].toISOString().slice(0, 19).replace('T', ' ');
    } else {
      searchForm.startTime = '';
      searchForm.endTime = '';
    }

    const res = await getLogs({
      ...searchForm,
      pageNum: pageNum.value,
      pageSize: pageSize.value
    });

    if (res.code === 0 && res.data) {
      logList.value = res.data.list || [];
      total.value = res.data.total || 0;
    }
  } catch (error) {
    console.error('获取日志失败:', error);
  } finally {
    loading.value = false;
  }
};

// 搜索
const handleSearch = () => {
  pageNum.value = 1;
  fetchLogs();
};

// 重置
const resetSearch = () => {
  searchForm.username = '';
  searchForm.operationType = '';
  dateRange.value = [];
  searchForm.startTime = '';
  searchForm.endTime = '';
  pageNum.value = 1;
  fetchLogs();
};

// 导出日志
const exportLogs = () => {
  ElMessage.info('导出功能开发中...');
};

// 分页
const handleSizeChange = (val) => {
  pageSize.value = val;
  fetchLogs();
};

const handleCurrentChange = (val) => {
  pageNum.value = val;
  fetchLogs();
};

// 监听用户名输入（防抖）
let timeoutId;
watch(() => searchForm.username, (newVal) => {
  clearTimeout(timeoutId);
  timeoutId = setTimeout(() => {
    if (newVal !== '') {
      fetchLogs();
    }
  }, 500);
});

onMounted(() => {
  fetchLogs();
});
</script>

<style scoped>
.logs-container {
  padding: 0;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: bold;
}

.search-form {
  margin-bottom: 20px;
  padding: 20px;
  background-color: #f5f7fa;
  border-radius: 4px;
}

.pagination {
  margin-top: 20px;
  justify-content: flex-end;
}
</style>