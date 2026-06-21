<!-- src/views/teacher/profile.vue -->
<template>
  <div class="profile-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <el-icon><User /></el-icon>
            <span>个人信息</span>
          </div>
          <el-button type="primary" @click="handleEdit" v-if="!editing">
            <el-icon><Edit /></el-icon>编辑信息
          </el-button>
          <div class="header-actions" v-else>
            <el-button @click="cancelEdit">取消</el-button>
            <el-button type="primary" @click="saveProfile" :loading="saving">
              保存修改
            </el-button>
          </div>
        </div>
      </template>

      <el-form
          ref="profileFormRef"
          :model="profileForm"
          :rules="profileRules"
          label-width="100px"
          :disabled="!editing"
          class="profile-form"
      >
        <!-- 基本信息卡片 -->
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="工号">
              <el-input v-model="profileForm.username" disabled>
                <template #prepend>
                  <el-icon><UserFilled /></el-icon>
                </template>
              </el-input>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="姓名">
              <el-input v-model="profileForm.realName" disabled>
                <template #prepend>
                  <el-icon><User /></el-icon>
                </template>
              </el-input>
              <div class="form-tip" v-if="editing">姓名不可修改，如需修改请联系管理员</div>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="学院" prop="college">
              <el-select
                  v-model="profileForm.college"
                  :disabled="!editing"
                  placeholder="请选择学院"
                  style="width: 100%"
              >
                <el-option label="计算机学院" value="计算机学院" />
                <el-option label="数学学院" value="数学学院" />
                <el-option label="外语学院" value="外语学院" />
                <el-option label="物理学院" value="物理学院" />
                <el-option label="化学学院" value="化学学院" />
                <el-option label="生命科学学院" value="生命科学学院" />
                <el-option label="经济管理学院" value="经济管理学院" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="职称" prop="title">
              <el-select
                  v-model="profileForm.title"
                  :disabled="!editing"
                  placeholder="请选择职称"
                  style="width: 100%"
              >
                <el-option label="教授" value="教授" />
                <el-option label="副教授" value="副教授" />
                <el-option label="讲师" value="讲师" />
                <el-option label="助教" value="助教" />
                <el-option label="研究员" value="研究员" />
                <el-option label="工程师" value="工程师" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 状态信息卡片 -->
        <el-divider content-position="left">账号状态</el-divider>

        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="角色">
              <el-tag :type="profileForm.role === 1 ? 'danger' : 'success'" effect="dark" size="large">
                <el-icon :style="{ marginRight: '4px' }">
                  <component :is="profileForm.role === 1 ? 'Star' : 'User'" />
                </el-icon>
                {{ profileForm.role === 1 ? '管理员' : '普通教师' }}
              </el-tag>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="状态">
              <el-tag :type="profileForm.status === 1 ? 'success' : 'info'" effect="dark" size="large">
                <el-icon :style="{ marginRight: '4px' }">
                  <component :is="profileForm.status === 1 ? 'CircleCheck' : 'CircleClose'" />
                </el-icon>
                {{ profileForm.status === 1 ? '正常' : '禁用' }}
              </el-tag>
            </el-form-item>
          </el-col>
        </el-row>

        <el-divider content-position="left">登录信息</el-divider>

        <el-descriptions :column="2" border>
          <el-descriptions-item label="最后登录时间">
            <el-icon><Timer /></el-icon>
            {{ profileForm.lastLoginTime || '暂无' }}
          </el-descriptions-item>
          <el-descriptions-item label="最后登录IP">
            <el-icon><Location /></el-icon>
            {{ profileForm.lastLoginIp || '暂无' }}
          </el-descriptions-item>
          <el-descriptions-item label="注册时间">
            <el-icon><Calendar /></el-icon>
            {{ profileForm.createTime || '暂无' }}
          </el-descriptions-item>
        </el-descriptions>
      </el-form>
    </el-card>

    <!-- 密码修改提示卡片 -->
    <el-card class="mt-20">
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <el-icon><Lock /></el-icon>
            <span>修改密码</span>
          </div>
        </div>
      </template>

      <el-empty description="修改密码请到专门的密码修改页面">
        <template #image>
          <el-icon :size="60" color="#909399"><Lock /></el-icon>
        </template>
        <el-button type="primary" @click="goToChangePassword">
          <el-icon><EditPen /></el-icon>
          去修改密码
        </el-button>
      </el-empty>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import {
  User, Edit, UserFilled, Lock, Timer, Location, Calendar,
  CircleCheck, CircleClose, Star, EditPen
} from '@element-plus/icons-vue';
import { getUserProfile, updateUserProfile } from '@/api/user';

const router = useRouter();
const editing = ref(false);
const saving = ref(false);
const profileFormRef = ref(null);

// 个人信息表单
const profileForm = reactive({
  username: '',
  realName: '',
  college: '',
  title: '',
  role: 0,
  status: 1,
  lastLoginTime: '',
  lastLoginIp: '',
  createTime: ''
});

// 备份原始数据，用于取消编辑
const originalData = ref(null);

// 个人信息验证规则 - 移除了姓名的验证
const profileRules = {
  college: [
    { required: true, message: '请选择学院', trigger: 'change' }
  ],
  title: [
    { required: true, message: '请选择职称', trigger: 'change' }
  ]
};

// 获取个人信息
const fetchProfile = async () => {
  try {
    const res = await getUserProfile();
    if (res.code === 0 && res.data) {
      Object.assign(profileForm, res.data);
      // 备份原始数据
      originalData.value = { ...res.data };
    }
  } catch (error) {
    console.error('获取个人信息失败:', error);
    ElMessage.error('获取个人信息失败');
  }
};

// 编辑信息
const handleEdit = () => {
  editing.value = true;
};

// 取消编辑
const cancelEdit = () => {
  if (originalData.value) {
    Object.assign(profileForm, originalData.value);
  }
  editing.value = false;
  profileFormRef.value?.clearValidate();
};

// 保存修改
const saveProfile = async () => {
  if (!profileFormRef.value) return;

  await profileFormRef.value.validate(async (valid) => {
    if (valid) {
      saving.value = true;
      try {
        const res = await updateUserProfile({
          realName: profileForm.realName, // 姓名虽然不可编辑，但还是传回原值
          college: profileForm.college,
          title: profileForm.title
        });

        if (res.code === 0) {
          ElMessage.success('个人信息修改成功');
          editing.value = false;
          // 更新备份数据
          originalData.value = { ...profileForm };
          // 重新获取最新数据
          await fetchProfile();
        }
      } catch (error) {
        console.error('保存失败:', error);
      } finally {
        saving.value = false;
      }
    }
  });
};

// 跳转到修改密码页面
const goToChangePassword = () => {
  // 根据角色跳转到不同的密码修改页面
  if (profileForm.role === 1) {
    router.push('/admin/change-password');
  } else {
    router.push('/teacher/change-password');
  }
};

onMounted(() => {
  fetchProfile();
});
</script>

<style scoped>
.profile-container {
  max-width: 1000px;
  margin: 0 auto;
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: bold;
}

.header-actions {
  display: flex;
  gap: 12px;
}

.profile-form {
  padding: 20px 0;
}

.form-tip {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
}

.mt-20 {
  margin-top: 20px;
}

:deep(.el-descriptions) {
  margin-top: 10px;
}

:deep(.el-descriptions__label) {
  width: 120px;
}

:deep(.el-empty) {
  padding: 40px 0;
}
</style>