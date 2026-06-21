// src/router/index.js
import { createRouter, createWebHistory } from 'vue-router';
import { ElMessage } from 'element-plus';

const routes = [
  {
    path: '/',
    redirect: '/login'
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/Register.vue'),
    meta: { title: '注册' }
  },
  {
    path: '/teacher',
    name: 'TeacherLayout',
    component: () => import('@/layouts/TeacherLayout.vue'),
    meta: { requiresAuth: true, role: 0 },
    children: [
      {
        path: 'dashboard',
        name: 'TeacherDashboard',
        component: () => import('@/views/teacher/Dashboard.vue'),
        meta: { title: '教师仪表盘' }
      },
      {
        path: 'questions',
        name: 'TeacherQuestions',
        component: () => import('@/views/teacher/Questions.vue'),
        meta: { title: '试题管理' }
      },
      {
        path: 'profile',
        name: 'TeacherProfile',
        component: () => import('@/views/teacher/Profile.vue'),
        meta: { title: '个人信息' }
      },
      {
        path: 'change-password',
        name: 'TeacherChangePassword',
        component: () => import('@/views/teacher/ChangePassword.vue'),
        meta: { title: '修改密码' }
      }

    ]
  },
  {
    path: '/admin',
    name: 'AdminLayout',
    component: () => import('@/layouts/AdminLayout.vue'),
    meta: { requiresAuth: true, role: 1 },
    children: [
      {
        path: 'dashboard',
        name: 'AdminDashboard',
        component: () => import('@/views/admin/Dashboard.vue'),
        meta: { title: '管理仪表盘' }
      },
      {
        path: 'users',
        name: 'AdminUsers',
        component: () => import('@/views/admin/Users.vue'),
        meta: { title: '用户管理' }
      },
      {
        path: 'logs',
        name: 'AdminLogs',
        component: () => import('@/views/admin/Logs.vue'),
        meta: { title: '操作日志' }
      },
      {
        path: 'subjects',
        name: 'AdminSubjects',
        component: () => import('@/views/admin/Subjects.vue'),
        meta: { title: '学科管理' }
      },
      {
        path: 'questions',
        name: 'AdminQuestions',
        component: () => import('@/views/teacher/Questions.vue'),
        meta: { title: '试题管理' }
      },
      {
        path: 'users',
        name: 'AdminUsers',
        component: () => import('@/views/admin/Users.vue'),
        meta: { title: '用户管理'  }
      },
      {
        path: 'profile',
        name: 'AdminProfile',
        component: () => import('@/views/teacher/Profile.vue'), // 复用个人信息页面
        meta: { title: '个人信息'}
      },
      {
        path: 'change-password',
        name: 'AdminChangePassword',
        component: () => import('@/views/admin/ChangePassword.vue'),
        meta: { title: '修改密码' }
      }
    ]
  }
];

const router = createRouter({
  history: createWebHistory(),
  routes
});

// 验证token是否有效
async function validateToken(token) {
  try {
    const response = await fetch('/api/user/validate-token', { // 使用 /api 代理
      method: 'GET',
      headers: {
        'Authorization': token,
        'Content-Type': 'application/json'
      }
    });

    if (!response.ok) {
      return false;
    }

    const data = await response.json();
    return data.code === 0 && data.data === true;
  } catch (error) {
    console.error('Token validation failed:', error);
    return false;
  }
}

// 清除用户数据
function clearUserData() {
  localStorage.removeItem('token');
  localStorage.removeItem('userRole');
  localStorage.removeItem('userInfo');
  localStorage.removeItem('userId');
}

// 路由守卫
router.beforeEach(async (to, from, next) => {
  // 设置页面标题
  document.title = to.meta.title ? `${to.meta.title} - 试题管理系统` : '试题管理系统';

  const token = localStorage.getItem('token');
  const userRole = localStorage.getItem('userRole');

  // 如果是根路径，重定向到登录页
  if (to.path === '/') {
    next('/login');
    return;
  }

  if (to.meta.requiresAuth) {
    if (!token) {
      // 未登录，跳转到登录页
      ElMessage.warning('请先登录');
      next('/login');
    } else {
      try {
        // 验证 token 是否有效
        const isValid = await validateToken(token);

        if (!isValid) {
          // token 无效，清除本地存储并跳转到登录页
          clearUserData();
          ElMessage.error('登录已过期，请重新登录');
          next('/login');
          return;
        }

        // 根据角色判断是否有权限访问
        if (to.meta.role !== undefined) {
          if (to.meta.role.toString() !== userRole) {
            // 角色不匹配，跳转到对应角色的首页
            ElMessage.warning('无权限访问该页面');
            if (userRole === '1') {
              next('/admin/dashboard');
            } else if (userRole === '0') {
              next('/teacher/dashboard');
            } else {
              next('/login');
            }
          } else {
            next();
          }
        } else {
          next();
        }
      } catch (error) {
        // 验证失败，清除本地存储并跳转到登录页
        clearUserData();
        ElMessage.error('登录验证失败，请重新登录');
        next('/login');
      }
    }
  } else {
    // 如果已登录，访问登录页或注册页，跳转到对应首页
    if (token && (to.path === '/login' || to.path === '/register')) {
      try {
        const isValid = await validateToken(token);
        if (!isValid) {
          clearUserData();
          next();
          return;
        }

        if (userRole === '1') {
          next('/admin/dashboard');
        } else if (userRole === '0') {
          next('/teacher/dashboard');
        } else {
          next();
        }
      } catch (error) {
        clearUserData();
        next();
      }
    } else {
      next();
    }
  }
});


export default router;