<!-- src/views/Register.vue -->
<template>
  <div class="register-container">
    <div class="register-box">
      <div class="register-header">
        <h2>教师注册</h2>
        <p>填写信息，注册账号</p>
      </div>

      <el-form
          ref="formRef"
          :model="registerForm"
          :rules="rules"
          label-position="top"
      >
        <el-form-item label="工号" prop="username">
          <el-input
              v-model="registerForm.username"
              placeholder="请输入5位数字工号"
              :prefix-icon="User"
              size="large"
          />
        </el-form-item>

        <el-form-item label="姓名" prop="realName">
          <el-input
              v-model="registerForm.realName"
              placeholder="请输入真实姓名"
              :prefix-icon="User"
              size="large"
          />
        </el-form-item>

        <el-form-item label="学院" prop="college">
          <el-select
              v-model="registerForm.college"
              placeholder="请选择学院"
              size="large"
              style="width: 100%"
          >
            <el-option label="计算机学院" value="计算机学院" />
            <el-option label="数学学院" value="数学学院" />
            <el-option label="外语学院" value="外语学院" />
            <el-option label="物理学院" value="物理学院" />
            <el-option label="化学学院" value="化学学院" />
          </el-select>
        </el-form-item>

        <el-form-item label="职称" prop="title">
          <el-select
              v-model="registerForm.title"
              placeholder="请选择职称"
              size="large"
              style="width: 100%"
          >
            <el-option label="教授" value="教授" />
            <el-option label="副教授" value="副教授" />
            <el-option label="讲师" value="讲师" />
            <el-option label="助教" value="助教" />
          </el-select>
        </el-form-item>

        <el-form-item label="密码" prop="password">
          <el-input
              v-model="registerForm.password"
              type="password"
              placeholder="请输入6-20位密码"
              :prefix-icon="Lock"
              size="large"
              show-password
          />
        </el-form-item>

        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input
              v-model="registerForm.confirmPassword"
              type="password"
              placeholder="请再次输入密码"
              :prefix-icon="Lock"
              size="large"
              show-password
          />
        </el-form-item>

        <el-form-item>
          <el-button
              type="primary"
              :loading="loading"
              class="register-btn"
              size="large"
              @click="handleRegister"
          >
            注册
          </el-button>
        </el-form-item>

        <div class="login-link">
          已有账号？
          <el-link type="primary" @click="goToLogin">立即登录</el-link>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { User, Lock } from '@element-plus/icons-vue';
import { useUserStore } from '@/store/user';

const router = useRouter();
const userStore = useUserStore();
const formRef = ref(null);
const loading = ref(false);

// 注册表单数据
const registerForm = reactive({
  username: '',
  realName: '',
  college: '',
  title: '',
  password: '',
  confirmPassword: ''
});

// 验证确认密码
const validateConfirmPassword = (rule, value, callback) => {
  if (value !== registerForm.password) {
    callback(new Error('两次输入的密码不一致'));
  } else {
    callback();
  }
};

// 表单验证规则
const rules = {
  username: [
    { required: true, message: '请输入工号', trigger: 'blur' },
    { pattern: /^[0-9]{5}$/, message: '工号必须为5位数字', trigger: 'blur' }
  ],
  realName: [
    { required: true, message: '请输入姓名', trigger: 'blur' },
    { min: 2, max: 20, message: '姓名长度必须在2-20字符之间', trigger: 'blur' }
  ],
  college: [
    { required: true, message: '请选择学院', trigger: 'change' }
  ],
  title: [
    { required: true, message: '请选择职称', trigger: 'change' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度必须在6-20字符之间', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ]
};

// 处理注册
const handleRegister = async () => {
  if (!formRef.value) return;

  await formRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true;
      try {
        const { confirmPassword, ...registerData } = registerForm;
        await userStore.handleRegister(registerData);
      } catch (error) {
        console.error('注册失败:', error);
      } finally {
        loading.value = false;
      }
    }
  });
};

// 跳转到登录页
const goToLogin = () => {
  router.push('/login');
};
</script>

<style scoped>
.register-container {
  min-height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;
}

.register-box {
  width: 500px;
  padding: 40px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.register-header {
  text-align: center;
  margin-bottom: 40px;
}

.register-header h2 {
  margin: 0 0 10px;
  color: #333;
  font-size: 28px;
}

.register-header p {
  margin: 0;
  color: #666;
  font-size: 16px;
}

.register-btn {
  width: 100%;
  margin-top: 20px;
}

.login-link {
  text-align: center;
  margin-top: 20px;
  color: #666;
}
</style>