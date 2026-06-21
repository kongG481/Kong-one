// src/api/question.js
import request from '@/utils/request';

// ==================== 入库模块 ====================

export function insertQuestion(data) {
    return request({
        url: '/question/insert',
        method: 'post',
        data
    });
}

export function checkDuplicate(content) {
    return request({
        url: '/question/checkDuplicate',
        method: 'get',
        params: { content }
    });
}

export function getDrafts() {
    return request({
        url: '/question/drafts',
        method: 'get'
    });
}

export function getDraftDetail(id) {
    return request({
        url: `/question/draft/${id}`,
        method: 'get'
    });
}

export function deleteDraft(id, physical = false) {
    return request({
        url: `/question/draft/${id}`,
        method: 'delete',
        params: { physical }
    });
}

export function batchImport(formData) {
    return request({
        url: '/question/batch/import',
        method: 'post',
        data: formData,
        headers: {
            'Content-Type': 'multipart/form-data'
        }
    });
}

export function downloadTemplate() {
    return request({
        url: '/question/template/download',
        method: 'get',
        responseType: 'blob'
    });
}

// ==================== 学科模块 ====================

export function getSubjectTree() {
    return request({
        url: '/question/subject/tree',
        method: 'get'
    });
}

// ==================== 查询模块 ====================

export function getQuestionList(params) {
    return request({
        url: '/question/list',
        method: 'get',
        params
    });
}

export function getQuestionDetail(id) {
    return request({
        url: `/question/detail/${id}`,
        method: 'get'
    });
}

// ==================== 修改模块 ====================

export function updateQuestion(data) {
    return request({
        url: '/question/update',
        method: 'put',
        data
    });
}

export function batchUpdateDifficulty(data) {
    return request({
        url: '/question/batch/difficulty',
        method: 'put',
        data
    });
}

export function batchUpdateSubject(data) {
    return request({
        url: '/question/batch/subject',
        method: 'put',
        data
    });
}

// ==================== 历史模块 ====================

export function getQuestionHistory(questionId) {
    return request({
        url: `/question/${questionId}/history`,
        method: 'get'
    });
}

export function compareHistoryVersions(historyId1, historyId2) {
    return request({
        url: '/question/history/compare',
        method: 'get',
        params: { historyId1, historyId2 }
    });
}

export function rollbackToVersion(questionId, historyId) {
    return request({
        url: `/question/${questionId}/rollback/${historyId}`,
        method: 'post'
    });
}

// ==================== 删除模块 ====================

export function deleteQuestion(id, reason) {
    return request({
        url: `/question/${id}`,
        method: 'delete',
        data: { deleteReason: reason }
    });
}

export function getRecycleBin() {
    return request({
        url: '/question/recycle-bin',
        method: 'get'
    });
}

export function restoreQuestion(id) {
    return request({
        url: `/question/${id}/restore`,
        method: 'put'
    });
}

export function permanentDelete(id) {
    return request({
        url: `/question/${id}/permanent`,
        method: 'delete'
    });
}

// ==================== 查询模块（新增） ====================

/**
 * 高级组合查询
 */
export function advancedQuery(data) {
    return request({
        url: '/question/query/advanced',
        method: 'post',
        data
    });
}

/**
 * 全文检索
 */
export function fullTextSearch(keywords, params) {
    return request({
        url: '/question/query/search',
        method: 'get',
        params: {
            keywords,
            ...params
        }
    });
}

/**
 * 保存查询条件
 */
export function saveQueryCondition(data) {
    return request({
        url: '/question/query/save',
        method: 'post',
        data
    });
}

/**
 * 获取查询历史
 */
export function getQueryHistory() {
    return request({
        url: '/question/query/history',
        method: 'get'
    });
}

/**
 * 删除查询历史
 */
export function deleteQueryHistory(id) {
    return request({
        url: `/question/query/history/${id}`,
        method: 'delete'
    });
}

/// ==================== 导出模块 ====================

/**
 * 导出试题
 */
export function exportQuestions(data) {
    return request({
        url: '/question/export',
        method: 'post',
        data,
        responseType: 'blob'
    })
}

/**
 * 获取导出历史
 */
export function getExportHistory() {
    return request({
        url: '/question/export/history',
        method: 'get'
    })
}

/**
 * 获取所有导出历史（管理员）
 */
export function getAllExportHistory() {
    return request({
        url: '/question/export/all',
        method: 'get'
    })
}

/**
 * 删除导出记录
 */
export function deleteExportTask(id) {
    return request({
        url: `/question/export/${id}`,
        method: 'delete'
    })
}

/**
 * 下载导出文件
 */
export function downloadExportFile(taskId) {
    return request({
        url: `/question/export/download/${taskId}`,
        method: 'get',
        responseType: 'blob'
    })
}

// 自动组卷
export function autoGeneratePaper(data) {
    return request({
        url: '/question/paper/generate',
        method: 'post',
        data
    });
}

// 导出试卷
export function exportPaper(data) {
    return request({
        url: '/question/paper/export',
        method: 'post',
        data,
        responseType: 'blob'
    });
}

// ==================== 重复检测模块 ====================

/**
 * 检测单个题目的重复
 */
export function checkQuestionDuplicate(questionId, threshold) {
    return request({
        url: `/question/duplicate/check/${questionId}`,
        method: 'get',
        params: { threshold }
    });
}

/**
 * 批量检测所有题目
 */
export function batchCheckDuplicates() {
    return request({
        url: '/question/duplicate/batch-check',
        method: 'post'
    });
}

/**
 * 处理重复题目
 */
export function handleDuplicate(duplicateId, handleType) {
    return request({
        url: `/question/duplicate/handle/${duplicateId}`,
        method: 'put',
        params: { handleType }
    });
}

/**
 * 获取重复统计信息
 */
export function getDuplicateStatistics() {
    return request({
        url: '/question/duplicate/statistics',
        method: 'get'
    });
}

/**
 * 获取待处理的重复列表
 */
export function getPendingDuplicates() {
    return request({
        url: '/question/duplicate/pending',
        method: 'get'
    });
}

// ==================== 统计分析模块 ====================

/**
 * 获取完整统计数据
 */
export function getFullStatistics() {
    return request({
        url: '/question/statistics/full',
        method: 'get'
    });
}

/**
 * 导出统计报表
 */
export function exportStatistics() {
    return request({
        url: '/question/statistics/export/excel',
        method: 'get',
        responseType: 'blob'
    });
}