// 创建 src/api/admin.js
import request from '@/utils/request';

/**
 * 获取用户列表
 */
export function getUserList(params) {
    return request({
        url: '/user/admin/list',
        method: 'get',
        params
    });
}
/**
 * 更新用户状态
 */
export function updateUserStatus(userId, status) {
    return request({
        url: `/user/admin/${userId}/status`,
        method: 'put',
        params: { status }
    });
}

/**
 * 更新用户角色
 */
export function updateUserRole(userId, role) {
    return request({
        url: `/user/admin/${userId}/role`,
        method: 'put',
        params: { role }
    });
}

/**
 * 删除用户
 */
export function deleteUser(userId) {
    return request({
        url: `/user/admin/${userId}`,
        method: 'delete'
    });
}

// src/api/admin.js

/**
 * 获取操作日志
 */
export function getLogs(params) {
    return request({
        url: '/logs/list',
        method: 'get',
        params
    });
}

/**
 * 获取当前用户的操作日志
 */
export function getMyLogs(params) {
    return request({
        url: '/logs/my-logs',
        method: 'get',
        params
    });
}