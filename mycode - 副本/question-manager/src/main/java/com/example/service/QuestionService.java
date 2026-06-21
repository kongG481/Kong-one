// QuestionService.java
package com.example.service;

import com.example.dto.exam.*;
import com.example.dto.question.BatchCheckResultVO;
import com.example.dto.question.RepeatCheckResultVO;
import com.example.dto.question.StatisticsVO;
import com.example.entity.question.*;
import com.example.vo.exam.PaperVO;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.Map;

public interface QuestionService {
    // 入库模块
    Integer insertQuestion(QuestionInsertDTO dto);
    Integer checkDuplicate(String content);
    List<Question> getDrafts();
    Question getDraftDetail(Integer id);
    void deleteDraft(Integer id, boolean physical);
    BatchImportResult batchImport(MultipartFile file) throws Exception;

    // 修改模块
    Integer updateQuestion(QuestionInsertDTO dto);
    Question getQuestionDetail(Integer id);
    List<Question> getQuestionList(Integer pageNum, Integer pageSize, Integer type, String keyword);
    Integer getQuestionCount(Integer type, String keyword);
    void batchUpdateDifficulty(BatchUpdateDTO dto);
    void batchUpdateSubject(BatchUpdateDTO dto);

    // 历史模块
    List<QuestionHistory> getQuestionHistory(Integer questionId);
    Map<String, Object> compareVersions(Integer historyId1, Integer historyId2);
    Integer rollbackToVersion(Integer questionId, Integer historyId);

    // 删除模块
    void deleteQuestion(Integer id, String deleteReason);
    List<Question> getRecycleBin();
    void restoreQuestion(Integer id);
    void permanentDelete(Integer id);

    // ========== 查询模块 ==========
    /**
     * 高级组合查询（支持多条件）
     */
    Map<String, Object> advancedQuery(QuestionQueryDTO query, Integer userId);

    /**
     * 全文检索
     */
    Map<String, Object> fullTextSearch(String keywords, QuestionQueryDTO query, Integer userId);

    /**
     * 保存查询条件
     */
    void saveQueryCondition(String queryName, QuestionQueryDTO query, Integer userId, Integer resultCount);

    /**
     * 获取查询历史
     */
    List<QueryHistory> getQueryHistory(Integer userId);

    /**
     * 删除查询历史
     */
    void deleteQueryHistory(Integer id, Integer userId);

// ========== 导出模块 ==========
    /**
     * 导出试题
     */
    String exportQuestions(QuestionExportDTO exportDTO, Integer userId) throws Exception;

    /**
     * 获取导出历史
     */
    List<ExportTask> getExportHistory(Integer userId);

    /**
     * 获取所有导出历史（管理员）
     */
    List<ExportTask> getAllExportHistory();

    /**
     * 删除导出记录
     */
    void deleteExportTask(Integer id, Integer userId, boolean isAdmin);

    /**
     * 下载导出文件
     */
    byte[] downloadExportFile(Integer taskId, Integer userId, boolean isAdmin) throws Exception;

    // QuestionService.java 中添加以下接口

    /**
     * 自动组卷
     */
    PaperVO autoGeneratePaper(AutoGeneratePaperDTO dto, Integer userId);

    /**
     * 导出试卷到Word
     */
    byte[] exportPaperToWord(PaperVO paper) throws Exception;

// ==================== 选项管理模块 ====================

    /**
     * 添加选项（为已有试题添加新选项）
     */
    Integer addChoice(Integer questionId, ChoiceDTO choiceDTO);

    /**
     * 更新选项内容
     */
    void updateChoice(UpdateChoiceDTO dto);

    /**
     * 删除选项
     */
    void deleteChoice(Integer choiceId);

    /**
     * 设置单选题的正确选项
     */
    void setSingleCorrectOption(Integer questionId, String correctLabel);

    /**
     * 设置多选题的正确选项
     */
    void setMultipleCorrectOptions(Integer questionId, List<String> correctLabels);

    /**
     * 获取试题的选项列表
     */
    List<QuestionChoice> getChoicesByQuestionId(Integer questionId);

    /**
     * 随机打乱选项顺序（用于考试场景）
     */
    List<QuestionChoice> getRandomOrderChoices(Integer questionId);

    // QuestionService.java 中添加

// ==================== 填空题/简答题验证模块 ====================

    /**
     * 验证填空题答案
     * @param questionId 试题ID
     * @param userAnswers 用户答案列表（按填空顺序）
     * @return 验证结果（每个填空是否正确）
     */
    Map<String, Object> validateBlankAnswers(Integer questionId, List<String> userAnswers);

    /**
     * 验证简答题答案
     * @param questionId 试题ID
     * @param userAnswer 用户答案文本
     * @return 评分结果（总分、得分、匹配的要点）
     */
    Map<String, Object> validateEssayAnswer(Integer questionId, String userAnswer);

    /**
     * 获取填空题的正确答案（用于展示）
     */
    List<QuestionAnswer> getBlankAnswers(Integer questionId);

    /**
     * 获取简答题的评分要点
     */
    List<QuestionAnswer> getEssayKeyPoints(Integer questionId);

    /**
     * 自动评分（通用方法）
     */
    AutoScoreResult autoScore(Integer questionId, Object userAnswer);

    // ========== 重复检测模块 ==========

    /**
     * 检测单个题目的重复
     */
    Map<String, Object> checkDuplicate(Integer questionId, Integer similarityThreshold);

    /**
     * 批量检测所有题目
     */
    Map<String, Object> batchCheckAllDuplicates();

    /**
     * 处理重复题目
     */
    boolean handleDuplicate(Integer duplicateId, String handleType);

    /**
     * 获取重复统计信息
     */
    Map<String, Object> getDuplicateStatistics();

    /**
     * 获取待处理的重复列表
     */
    List<Duplicate> getPendingDuplicates();

    // ========== 统计分析模块 ==========

    /**
     * 获取完整统计数据
     */
    Map<String, Object> getFullStatistics();

    /**
     * 导出统计数据到Excel
     */
    byte[] exportStatisticsToExcel() throws Exception;
}