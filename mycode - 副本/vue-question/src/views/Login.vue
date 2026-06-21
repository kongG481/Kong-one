<!-- src/views/Login.vue -->
<template>
  <div class="login-container">
    <div class="login-box">
      <div class="login-header">
        <h2>试题管理系统</h2>
        <p>登录</p>
      </div>

      <el-form
          ref="formRef"
          :model="loginForm"
          :rules="rules"
          label-position="top"
          @keyup.enter="handleLogin"
      >
        <el-form-item label="工号" prop="username">
          <el-input
              v-model="loginForm.username"
              placeholder="请输入工号"
              :prefix-icon="User"
              size="large"
          />
        </el-form-item>

        <el-form-item label="密码" prop="password">
          <el-input
              v-model="loginForm.password"
              type="password"
              placeholder="请输入密码"
              :prefix-icon="Lock"
              size="large"
              show-password
          />
        </el-form-item>

        <el-form-item>
          <el-button
              type="primary"
              :loading="loading"
              class="login-btn"
              size="large"
              @click="handleLogin"
          >
            登录
          </el-button>
        </el-form-item>

        <div class="register-link">
          还没有账号？
          <el-link type="primary" @click="goToRegister">立即注册</el-link>
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

// 登录表单数据
const loginForm = reactive({
  username: '',
  password: ''
});

// 自定义验证规则：工号验证（5位数字或admin）
const validateUsername = (rule, value, callback) => {
  if (!value) {
    callback(new Error('请输入工号'));
  } else {
    // 允许admin账号或5位数字工号
    const isAdmin = value === 'admin';
    const isTeacherId = /^[0-9]{5}$/.test(value);

    if (isAdmin || isTeacherId) {
      callback();
    } else {
      callback(new Error('工号必须为5位数字，管理员请输入admin'));
    }
  }
};

// 表单验证规则
const rules = {
  username: [
    { required: true, message: '请输入工号', trigger: 'blur' },
    { validator: validateUsername, trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度必须在6-20字符之间', trigger: 'blur' }
  ]
};

// 处理登录
const handleLogin = async () => {
  if (!formRef.value) return;

  await formRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true;
      try {
        await userStore.handleLogin(loginForm);
      } catch (error) {
        console.error('登录失败:', error);
      } finally {
        loading.value = false;
      }
    }
  });
};

// 跳转到注册页
const goToRegister = () => {
  router.push('/register');
};
</script>

<style scoped>
.login-container {
  height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.login-box {
  width: 400px;
  padding: 40px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.login-header {
  text-align: center;
  margin-bottom: 40px;
}

.login-header h2 {
  margin: 0 0 10px;
  color: #333;
  font-size: 28px;
}

.login-header p {
  margin: 0;
  color: #666;
  font-size: 16px;
}

.login-btn {
  width: 100%;
  margin-top: 20px;
}

.register-link {
  text-align: center;
  margin-top: 20px;
  color: #666;
}
</style>