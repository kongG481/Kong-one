// src/store/user.js
import { defineStore } from 'pinia';
import { ref } from 'vue';
import { login, register, logout } from '@/api/user';
import { ElMessage } from 'element-plus';
import router from '@/router';

export const useUserStore = defineStore('user', () => {
    // 状态
    const token = ref(localStorage.getItem('token') || '');
    const userInfo = ref(JSON.parse(localStorage.getItem('userInfo') || '{}'));
    const userRole = ref(localStorage.getItem('userRole') || '');

    // 登录
    const handleLogin = async (loginData) => {
        try {
            const res = await login(loginData);
            if (res.code === 0 && res.data) {
                const data = res.data;

                // 保存token
                token.value = data.token;
                userInfo.value = {
                    id: data.id,
                    username: data.username,
                    realName: data.realName,
                    college: data.college,
                    title: data.title
                };
                userRole.value = data.role;

                // 存储到localStorage
                localStorage.setItem('token', data.token);
                localStorage.setItem('userInfo', JSON.stringify(userInfo.value));
                localStorage.setItem('userRole', data.role);

                ElMessage.success('登录成功');

                // 根据角色跳转到不同页面
                if (data.role === 1) {
                    router.push('/admin/dashboard');
                } else {
                    router.push('/teacher/dashboard');
                }

                return true;
            }
        } catch (error) {
            console.error('登录失败:', error);
            return false;
        }
    };

    // 注册
    const handleRegister = async (registerData) => {
        try {
            const res = await register(registerData);
            if (res.code === 0) {
                ElMessage.success('注册成功，请登录');
                router.push('/login');
                return true;
            }
        } catch (error) {
            console.error('注册失败:', error);
            return false;
        }
    };

    // 登出
    const handleLogout = async () => {
        try {
            await logout();
        } finally {
            // 清除本地存储
            token.value = '';
            userInfo.value = {};
            userRole.value = '';
            localStorage.removeItem('token');
            localStorage.removeItem('userInfo');
            localStorage.removeItem('userRole');

            ElMessage.success('已退出登录');
            router.push('/login');
        }
    };

    return {
        token,
        userInfo,
        userRole,
        handleLogin,
        handleRegister,
        handleLogout
    };
});