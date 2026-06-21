// src/api/user.js
import request from '@/utils/request';

/**
 * 用户注册
 * @param {Object} data 注册信息
 * @returns {Promise}
 */
export function register(data) {
    return request({
        url: '/user/register',
        method: 'post',
        data
    });
}

/**
 * 用户登录
 * @param {Object} data 登录信息
 * @returns {Promise}
 */
export function login(data) {
    return request({
        url: '/user/login',
        method: 'post',
        data
    });
}

/**
 * 用户登出
 * @returns {Promise}
 */
export function logout() {
    return request({
        url: '/user/logout',
        method: 'post'
    });
}
/**
 * 获取个人信息
 */
export function getUserProfile() {
    return request({
        url: '/user/profile',
        method: 'get'
    });
}

/**
 * 更新个人信息
 */
export function updateUserProfile(data) {
    return request({
        url: '/user/profile',
        method: 'put',
        data
    });
}

/**
 * 验证原密码是否正确
 */
export function verifyOldPassword(data) {
    return request({
        url: '/user/verify-password',
        method: 'post',
        data
    });
}

/**
 * 修改密码
 */
export function changePassword(data) {
    return request({
        url: '/user/password',
        method: 'put',
        data
    });
}