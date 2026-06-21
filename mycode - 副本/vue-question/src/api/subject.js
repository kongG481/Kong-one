// src/api/subject.js
import request from '@/utils/request';

/**
 * 获取学科树
 */
export function getSubjectTree() {
    return request({
        url: '/question/subject/tree',
        method: 'get'
    });
}

/**
 * 添加学科
 */
export function addSubject(data) {
    return request({
        url: '/admin/subject',
        method: 'post',
        data
    });
}

/**
 * 更新学科
 */
export function updateSubject(data) {
    return request({
        url: `/admin/subject/${data.id}`,
        method: 'put',
        data
    });
}

/**
 * 删除学科
 */
export function deleteSubject(id) {
    return request({
        url: `/admin/subject/${id}`,
        method: 'delete'
    });
}

/**
 * 获取学科统计
 */
export function getSubjectStats() {
    return request({
        url: '/admin/subject/stats',
        method: 'get'
    });
}