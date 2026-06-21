<!-- src/views/teacher/ChangePassword.vue -->
<template>
  <div class="change-password-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <el-icon><Lock /></el-icon>
            <span>修改密码</span>
          </div>
        </div>
      </template>

      <el-steps :active="currentStep" finish-status="success" simple class="steps">
        <el-step title="验证身份" />
        <el-step title="设置新密码" />
        <el-step title="完成" />
      </el-steps>

      <!-- 步骤1：验证身份 -->
      <el-form
          v-if="currentStep === 0"
          ref="verifyFormRef"
          :model="verifyForm"
          :rules="verifyRules"
          label-width="100px"
          class="password-form"
      >
        <el-form-item label="原密码" prop="oldPassword">
          <el-input
              v-model="verifyForm.oldPassword"
              type="password"
              placeholder="请输入原密码"
              show-password
              clearable
              size="large"
              @keyup.enter="handleVerify"
          >
            <template #prepend>
              <el-icon><Key /></el-icon>
            </template>
          </el-input>
          <div class="form-tip">请输入当前使用的密码</div>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="handleVerify" :loading="verifying">
            下一步
          </el-button>
        </el-form-item>
      </el-form>

      <!-- 步骤2：设置新密码 -->
      <el-form
          v-if="currentStep === 1"
          ref="passwordFormRef"
          :model="passwordForm"
          :rules="passwordRules"
          label-width="100px"
          class="password-form"
      >
        <el-form-item label="新密码" prop="newPassword">
          <el-input
              v-model="passwordForm.newPassword"
              type="password"
              placeholder="请输入6-20位新密码"
              show-password
              clearable
              size="large"
              @input="checkPasswordStrength"
          >
            <template #prepend>
              <el-icon><Unlock /></el-icon>
            </template>
          </el-input>
        </el-form-item>

        <!-- 密码强度提示 -->
        <el-form-item label="密码强度" v-if="passwordForm.newPassword">
          <div class="password-strength">
            <el-progress
                :percentage="passwordStrength"
                :color="strengthColor"
                :format="strengthFormat"
                stroke-width="10"
            />
            <div class="strength-tips">
              <span>弱</span>
              <span>中</span>
              <span>强</span>
              <span>非常强</span>
            </div>
          </div>
        </el-form-item>

        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input
              v-model="passwordForm.confirmPassword"
              type="password"
              placeholder="请再次输入新密码"
              show-password
              clearable
              size="large"
          >
            <template #prepend>
              <el-icon><Check /></el-icon>
            </template>
          </el-input>
        </el-form-item>

        <!-- 密码规则提示 -->
        <el-form-item label="密码规则">
          <div class="password-rules">
            <div class="rule-item" :class="{ valid: rules.length }">
              <el-icon :color="rules.length ? '#67c23a' : '#909399'">
                <component :is="rules.length ? 'CircleCheck' : 'CircleClose'" />
              </el-icon>
              <span>长度6-20位</span>
            </div>
            <div class="rule-item" :class="{ valid: rules.number }">
              <el-icon :color="rules.number ? '#67c23a' : '#909399'">
                <component :is="rules.number ? 'CircleCheck' : 'CircleClose'" />
              </el-icon>
              <span>包含数字</span>
            </div>
            <div class="rule-item" :class="{ valid: rules.lowercase }">
              <el-icon :color="rules.lowercase ? '#67c23a' : '#909399'">
                <component :is="rules.lowercase ? 'CircleCheck' : 'CircleClose'" />
              </el-icon>
              <span>包含小写字母</span>
            </div>
            <div class="rule-item" :class="{ valid: rules.uppercase }">
              <el-icon :color="rules.uppercase ? '#67c23a' : '#909399'">
                <component :is="rules.uppercase ? 'CircleCheck' : 'CircleClose'" />
              </el-icon>
              <span>包含大写字母</span>
            </div>
            <div class="rule-item" :class="{ valid: rules.special }">
              <el-icon :color="rules.special ? '#67c23a' : '#909399'">
                <component :is="rules.special ? 'CircleCheck' : 'CircleClose'" />
              </el-icon>
              <span>包含特殊字符</span>
            </div>
          </div>
        </el-form-item>

        <el-form-item>
          <el-button @click="currentStep = 0">上一步</el-button>
          <el-button type="primary" @click="handleChangePassword" :loading="changing">
            确认修改
          </el-button>
        </el-form-item>
      </el-form>

      <!-- 步骤3：完成 -->
      <div v-if="currentStep === 2" class="complete-step">
        <el-result
            icon="success"
            title="密码修改成功"
            :sub-title="successMessage"
        >
          <template #extra>
            <el-button type="primary" @click="handleComplete">立即登录</el-button>
          </template>
        </el-result>
      </div>
    </el-card>

    <!-- 安全提示 -->
    <el-card class="mt-20">
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <el-icon><Warning /></el-icon>
            <span>密码安全提示</span>
          </div>
        </div>
      </template>

      <el-row :gutter="20">
        <el-col :span="8" v-for="(tip, index) in securityTips" :key="index">
          <div class="tip-item">
            <el-icon :color="tip.type === 'success' ? '#67c23a' : '#f56c6c'" :size="20">
              <component :is="tip.type === 'success' ? 'Check' : 'Close'" />
            </el-icon>
            <span>{{ tip.text }}</span>
          </div>
        </el-col>
      </el-row>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  Lock, Key, Unlock, Check, Warning,
  CircleCheck, CircleClose
} from '@element-plus/icons-vue';
import { changePassword, verifyOldPassword } from '@/api/user';

const router = useRouter();
const verifyFormRef = ref(null);
const passwordFormRef = ref(null);

// 步骤控制
const currentStep = ref(0);
const verifying = ref(false);
const changing = ref(false);

// 验证表单
const verifyForm = reactive({
  oldPassword: ''
});

// 密码表单
const passwordForm = reactive({
  newPassword: '',
  confirmPassword: ''
});

// 验证规则
const verifyRules = {
  oldPassword: [
    { required: true, message: '请输入原密码', trigger: 'blur' }
  ]
};

// 密码规则检查
const rules = computed(() => {
  const pwd = passwordForm.newPassword;
  return {
    length: pwd && pwd.length >= 6 && pwd.length <= 20,
    number: /\d/.test(pwd),
    lowercase: /[a-z]/.test(pwd),
    uppercase: /[A-Z]/.test(pwd),
    special: /[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?]/.test(pwd)
  };
});

// 验证确认密码
const validateConfirmPassword = (rule, value, callback) => {
  if (value !== passwordForm.newPassword) {
    callback(new Error('两次输入的密码不一致'));
  } else {
    callback();
  }
};

// 密码验证规则
const passwordRules = {
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度必须在6-20位之间', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ]
};

// 计算密码强度
const passwordStrength = computed(() => {
  const pwd = passwordForm.newPassword;
  if (!pwd) return 0;

  let strength = 0;
  if (pwd.length >= 6) strength += 20;
  if (pwd.length >= 10) strength += 10;
  if (/\d/.test(pwd)) strength += 20;
  if (/[a-z]/.test(pwd)) strength += 20;
  if (/[A-Z]/.test(pwd)) strength += 15;
  if (/[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?]/.test(pwd)) strength += 15;

  return Math.min(strength, 100);
});

// 强度颜色
const strengthColor = computed(() => {
  const strength = passwordStrength.value;
  if (strength < 30) return '#f56c6c';
  if (strength < 60) return '#e6a23c';
  if (strength < 80) return '#67c23a';
  return '#409eff';
});

// 强度文本
const strengthFormat = (percentage) => {
  if (percentage < 30) return '弱';
  if (percentage < 60) return '中';
  if (percentage < 80) return '强';
  return '非常强';
};

// 检查密码强度（实时更新）
const checkPasswordStrength = () => {
  // 触发性计算属性更新
};

// 安全提示
const securityTips = [
  { type: 'success', text: '密码长度应在6-20位之间' },
  { type: 'success', text: '建议包含大小写字母、数字和特殊字符' },
  { type: 'success', text: '不要使用与账号相同或相似的密码' },
  { type: 'success', text: '定期更换密码可以提高账号安全性' },
  { type: 'error', text: '不要将密码告诉他人' },
  { type: 'error', text: '不要在多个网站使用相同密码' }
];

// 验证身份
const handleVerify = async () => {
  if (!verifyFormRef.value) return;

  await verifyFormRef.value.validate(async (valid) => {
    if (valid) {
      verifying.value = true;
      try {
        // 调用后端验证原密码
        const res = await verifyOldPassword({
          oldPassword: verifyForm.oldPassword
        });

        if (res.code === 0 && res.data === true) {
          // 验证通过，进入下一步
          currentStep.value = 1;
        } else {
          ElMessage.error('原密码错误，请重新输入');
          verifyForm.oldPassword = '';
          verifyFormRef.value?.clearValidate();
        }
      } catch (error) {
        console.error('验证失败:', error);
        ElMessage.error('验证失败，请稍后重试');
      } finally {
        verifying.value = false;
      }
    }
  });
};

// 修改密码
const handleChangePassword = async () => {
  if (!passwordFormRef.value) return;

  await passwordFormRef.value.validate(async (valid) => {
    if (valid) {
      // 确认修改
      try {
        await ElMessageBox.confirm(
            '修改密码后需要重新登录，确定要继续吗？',
            '确认修改',
            {
              confirmButtonText: '确定',
              cancelButtonText: '取消',
              type: 'warning'
            }
        );

        changing.value = true;
        const res = await changePassword({
          oldPassword: verifyForm.oldPassword,
          newPassword: passwordForm.newPassword,
          confirmPassword: passwordForm.confirmPassword
        });

        if (res.code === 0) {
          currentStep.value = 2;
          successMessage.value = '您的密码已成功修改，请使用新密码重新登录。';

          // 3秒后自动跳转到登录页
          setTimeout(() => {
            handleComplete();
          }, 3000);
        }
      } catch (error) {
        if (error !== 'cancel') {
          console.error('修改密码失败:', error);
        }
      } finally {
        changing.value = false;
      }
    }
  });
};

const successMessage = ref('');

// 完成，跳转到登录页
const handleComplete = () => {
  // 清除所有登录信息
  localStorage.clear();
  sessionStorage.clear();
  router.push('/login');
};
</script>

<style scoped>
.change-password-container {
  max-width: 800px;
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

.steps {
  margin-bottom: 30px;
}

.password-form {
  max-width: 500px;
  margin: 0 auto;
  padding: 20px 0;
}

.form-tip {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
}

.password-strength {
  width: 100%;
}

.strength-tips {
  display: flex;
  justify-content: space-between;
  margin-top: 5px;
  font-size: 12px;
  color: #999;
}

.password-rules {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px;
  padding: 10px;
  background-color: #f5f7fa;
  border-radius: 4px;
}

.rule-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #606266;
}

.rule-item.valid {
  color: #67c23a;
}

.complete-step {
  padding: 20px 0;
}

.mt-20 {
  margin-top: 20px;
}

.tip-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 0;
  font-size: 14px;
}

.tip-item span {
  flex: 1;
}
</style>