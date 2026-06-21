// src/utils/request.js
/**
 * Axios请求封装
 * 
 * 功能说明：
 * 1. 统一配置axios实例（baseURL、timeout、headers）
 * 2. 请求拦截器：自动携带Token
 * 3. 响应拦截器：统一处理成功/失败响应
 * 4. 错误处理：区分不同HTTP状态码，显示友好提示
 * 5. Token管理：失效时自动跳转登录页
 * 
 * @author System
 * @since 1.0
 */
import axios from 'axios';
import { ElMessage } from 'element-plus';
import router from '../router';

// 创建axios实例
const request = axios.create({
    baseURL: '/api', // 后端API基础地址，开发环境通过vite代理转发
    timeout: 60000, // 请求超时时间：60秒
    headers: {
        'Content-Type': 'application/json' // 默认请求头
    }
});

/**
 * 请求拦截器
 * 
 * 功能：
 * 1. 从localStorage获取Token
 * 2. 将Token添加到请求头Authorization字段
 * 3. 所有需要登录的接口都会自动携带Token
 */
request.interceptors.request.use(
    config => {
        // 从localStorage获取token
        const token = localStorage.getItem('token');
        if (token) {
            config.headers['Authorization'] = token;
        }
        return config;
    },
    error => {
        // 请求错误处理
        return Promise.reject(error);
    }
);

/**
 * 响应拦截器
 * 
 * 成功响应处理：
 * 1. Blob类型：检查是否为错误响应（后端可能返回JSON格式的错误）
 * 2. 普通JSON：根据code字段判断成功(code=0)或失败(code=1)
 * 
 * 失败响应处理：
 * 1. 401：Token失效，清除本地信息并跳转登录页
 * 2. 403：无权限访问
 * 3. 404：资源不存在
 * 4. 400：请求参数错误
 * 5. 500：服务器内部错误
 * 6. 其他：显示后端返回的具体错误信息
 */
request.interceptors.response.use(
    response => {
        // 如果是 blob 类型，需要检查是否为错误响应
        // 场景：下载文件时，如果服务器出错，会返回JSON格式的blob
        if (response.config.responseType === 'blob') {
            // 如果返回的是 JSON 类型的错误信息
            if (response.data.type === 'application/json') {
                return new Promise((resolve, reject) => {
                    const reader = new FileReader();
                    reader.onload = () => {
                        try {
                            const errorData = JSON.parse(reader.result);
                            ElMessage.error(errorData.message || '请求失败');
                            reject(new Error(errorData.message || '请求失败'));
                        } catch (e) {
                            ElMessage.error('下载失败');
                            reject(new Error('下载失败'));
                        }
                    };
                    reader.readAsText(response.data);
                });
            }
            // 正常的 blob 数据（文件流），直接返回
            return response.data;
        }
        
        // 普通JSON响应
        const res = response.data;

        // 根据后端返回的code判断请求是否成功
        // code=0表示成功，code=1表示失败
        if (res.code === 0) {
            return res;
        } else {
            // 业务错误，显示错误信息
            ElMessage.error(res.message || '请求失败');
            return Promise.reject(new Error(res.message || '请求失败'));
        }
    },
    error => {
        // HTTP错误处理（状态码非2xx）
        if (error.response) {
            switch (error.response.status) {
                case 401:
                    // Token失效或无效，清除本地信息并跳转到登录页
                    localStorage.removeItem('token');
                    localStorage.removeItem('userRole');
                    localStorage.removeItem('userInfo');
                    router.push('/login');
                    ElMessage.error('登录已过期，请重新登录');
                    break;
                case 403:
                    // 服务器理解请求但拒绝执行（权限不足）
                    ElMessage.error('没有权限访问');
                    break;
                case 404:
                    // 请求的资源不存在（接口地址错误或资源已删除）
                    ElMessage.error('请求的资源不存在');
                    break;
                case 400:
                    // 请求参数错误（参数格式不正确或缺少必要参数）
                    ElMessage.error(error.response.data?.message || '请求参数错误');
                    break;
                case 500:
                    // 服务器内部错误（后端代码异常）
                    ElMessage.error(error.response.data?.message || '服务器内部错误');
                    break;
                default:
                    // 其他HTTP错误，显示后端返回的具体信息
                    ElMessage.error(error.response.data?.message || '请求失败');
            }
        } else if (error.request) {
            // 请求已发出但没有收到响应（网络断开、服务器无响应）
            ElMessage.error('网络连接失败，请检查网络');
        } else {
            // 设置请求时发生错误（配置错误、请求被取消等）
            ElMessage.error('请求配置错误');
        }
        return Promise.reject(error);
    }
);

export default request;