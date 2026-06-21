<!-- src/views/admin/Users.vue -->
<template>
  <div class="users-container">
    <!-- 搜索栏 -->
    <el-card class="search-card">
      <el-form :inline="true" :model="searchForm" @submit.prevent>
        <el-form-item label="搜索">
          <el-input
              v-model="searchForm.keyword"
              placeholder="工号/姓名"
              clearable
              @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 用户列表 -->
    <el-card class="table-card">
      <el-table :data="userList" border stripe v-loading="loading">
        <el-table-column prop="username" label="工号" width="120" />
        <el-table-column prop="realName" label="姓名" width="120" />
        <el-table-column prop="college" label="学院" width="150" />
        <el-table-column prop="title" label="职称" width="100" />
        <el-table-column prop="role" label="角色" width="100">
          <template #default="{ row }">
            <el-tag :type="row.role === 1 ? 'danger' : 'success'" size="small">
              {{ row.role === 1 ? '管理员' : '教师' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-switch
                v-model="row.status"
                :active-value="1"
                :inactive-value="0"
                active-color="#13ce66"
                inactive-color="#ff4949"
                :disabled="row.id === currentUserId"
                @change="(val) => handleStatusChange(row, val)"
            />
          </template>
        </el-table-column>
        <el-table-column prop="lastLoginTime" label="最后登录" width="180" />
        <el-table-column prop="createTime" label="注册时间" width="180" />
        <el-table-column label="操作" fixed="right" width="250">
          <template #default="{ row }">
            <el-button
                type="primary"
                link
                @click="handleEditRole(row)"
                :disabled="row.id === currentUserId"
            >
              分配角色
            </el-button>
            <el-button
                type="danger"
                link
                @click="handleDelete(row)"
                :disabled="row.id === currentUserId"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
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

    <!-- 分配角色对话框 -->
    <el-dialog v-model="roleDialogVisible" title="分配角色" width="400px">
      <el-form>
        <el-form-item label="用户">
          <span>{{ currentUser?.realName }} ({{ currentUser?.username }})</span>
        </el-form-item>
        <el-form-item label="角色">
          <el-radio-group v-model="selectedRole">
            <el-radio :label="0">普通教师</el-radio>
            <el-radio :label="1">管理员</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="roleDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="saveRole" :loading="savingRole">
            确定
          </el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { getUserList, updateUserStatus, updateUserRole, deleteUser } from '@/api/admin';

const loading = ref(false);
const userList = ref([]);
const total = ref(0);
const pageNum = ref(1);
const pageSize = ref(10);
const currentUserId = ref(parseInt(JSON.parse(localStorage.getItem('userInfo') || '{}').id || '0'));

// 搜索表单
const searchForm = reactive({
  keyword: ''
});

// 角色对话框
const roleDialogVisible = ref(false);
const currentUser = ref(null);
const selectedRole = ref(0);
const savingRole = ref(false);

// 获取用户列表
const fetchUserList = async () => {
  loading.value = true;
  try {
    const res = await getUserList({
      keyword: searchForm.keyword,
      pageNum: pageNum.value,
      pageSize: pageSize.value
    });
    if (res.code === 0 && res.data) {
      userList.value = res.data.list || [];
      total.value = res.data.total || 0;
    }
  } catch (error) {
    console.error('获取用户列表失败:', error);
  } finally {
    loading.value = false;
  }
};

// 搜索
const handleSearch = () => {
  pageNum.value = 1;
  fetchUserList();
};

// 重置搜索
const resetSearch = () => {
  searchForm.keyword = '';
  handleSearch();
};

// 分页
const handleSizeChange = (val) => {
  pageSize.value = val;
  fetchUserList();
};

const handleCurrentChange = (val) => {
  pageNum.value = val;
  fetchUserList();
};

// 状态修改
const handleStatusChange = async (row, status) => {
  const action = status === 1 ? '启用' : '禁用';
  try {
    await ElMessageBox.confirm(`确定要${action}用户 ${row.realName} 吗？`, '提示', {
      type: 'warning'
    });

    const res = await updateUserStatus(row.id, status);
    if (res.code === 0) {
      ElMessage.success(`${action}成功`);
      fetchUserList();
    }
  } catch (error) {
    // 取消操作，恢复状态
    row.status = status === 1 ? 0 : 1;
    if (error !== 'cancel') {
      console.error('操作失败:', error);
    }
  }
};

// 编辑角色
const handleEditRole = (row) => {
  currentUser.value = row;
  selectedRole.value = row.role;
  roleDialogVisible.value = true;
};

// 保存角色
const saveRole = async () => {
  if (!currentUser.value) return;

  savingRole.value = true;
  try {
    const res = await updateUserRole(currentUser.value.id, selectedRole.value);
    if (res.code === 0) {
      ElMessage.success('角色分配成功');
      roleDialogVisible.value = false;
      fetchUserList();
    }
  } catch (error) {
    console.error('分配角色失败:', error);
  } finally {
    savingRole.value = false;
  }
};

// 删除用户
const handleDelete = (row) => {
  ElMessageBox.confirm(
      `确定要删除用户 ${row.realName} 吗？删除后该用户的所有数据将被清空！`,
      '警告',
      {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'error'
      }
  ).then(async () => {
    try {
      const res = await deleteUser(row.id);
      if (res.code === 0) {
        ElMessage.success('删除成功');
        fetchUserList();
      }
    } catch (error) {
      console.error('删除失败:', error);
    }
  }).catch(() => {});
};

onMounted(() => {
  fetchUserList();
});
</script>

<style scoped>
.users-container {
  padding: 0;
}

.search-card {
  margin-bottom: 20px;
}

.table-card {
  margin-bottom: 20px;
}

.pagination {
  margin-top: 20px;
  justify-content: flex-end;
}
</style>