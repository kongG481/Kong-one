<!-- src/layouts/AdminLayout.vue -->
<template>
  <el-container class="layout-container">
    <el-aside width="200px" class="aside">
      <div class="logo">
        <h3>试题管理系统</h3>
        <p class="admin-tag">管理员</p>
      </div>
      <el-menu
          :router="true"
          :default-active="$route.path"
          class="menu"
          background-color="#304156"
          text-color="#bfcbd9"
          active-text-color="#ffd04b"
      >
        <el-menu-item index="/admin/dashboard">
          <el-icon><Monitor /></el-icon>
          <span>仪表盘</span>
        </el-menu-item>
        <el-menu-item index="/admin/users">
          <el-icon><User /></el-icon>
          <span>用户管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/subjects">
          <el-icon><Collection /></el-icon>
          <span>学科管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/questions">
          <el-icon><Document /></el-icon>
          <span>试题管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/logs">
          <el-icon><List /></el-icon>
          <span>操作日志</span>
        </el-menu-item>
        <el-sub-menu index="5">
          <template #title>
            <el-icon><DataLine /></el-icon>
            <span>统计分析</span>
          </template>
          <el-menu-item index="/admin/stats/questions">试题统计</el-menu-item>
          <el-menu-item index="/admin/stats/users">用户统计</el-menu-item>
          <el-menu-item index="/admin/stats/duplicates">重复试题</el-menu-item>
        </el-sub-menu>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="header">
        <div class="header-left">
          <el-breadcrumb>
            <el-breadcrumb-item :to="{ path: '/admin/dashboard' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item>{{ $route.meta.title }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="header-right">
          <el-dropdown @command="handleCommand">
            <span class="user-info">
              <el-avatar :size="32" :icon="UserFilled" />
              <span class="username">{{ userStore.userInfo.realName }}</span>
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人信息</el-dropdown-item>
                <el-dropdown-item command="changePassword">修改密码</el-dropdown-item>
                <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { Monitor, User, Collection, Document, DataLine, List, UserFilled, ArrowDown } from '@element-plus/icons-vue';
import { useUserStore } from '@/store/user';
import { useRouter } from 'vue-router';

const userStore = useUserStore();
const router = useRouter();

const handleCommand = (command) => {
  switch (command) {
    case 'profile':
      router.push('/admin/profile');
      break;
    case 'changePassword':
      router.push('/admin/change-password');
      break;
    case 'logout':
      userStore.handleLogout();
      break;
  }
};
</script>

<style scoped>
.layout-container {
  height: 100vh;
}

.aside {
  background-color: #304156;
  overflow: hidden;
}

.logo {
  height: 80px;
  padding: 10px;
  text-align: center;
  color: white;
  background-color: #1f2d3a;
}

.logo h3 {
  margin: 5px 0;
}

.admin-tag {
  font-size: 12px;
  color: #ffd04b;
  margin: 0;
}

.menu {
  border-right: none;
}

.header {
  background-color: white;
  border-bottom: 1px solid #e6e9f0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}

.username {
  font-size: 14px;
  color: #333;
}

.main {
  background-color: #f0f2f5;
  padding: 20px;
}
</style>