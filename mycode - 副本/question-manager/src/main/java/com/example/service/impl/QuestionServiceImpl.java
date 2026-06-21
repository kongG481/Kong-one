// QuestionServiceImpl.java
package com.example.service.impl;

import com.example.dto.exam.*;
import com.example.dto.question.BatchCheckResultVO;
import com.example.dto.question.RepeatCheckResultVO;
import com.example.entity.question.*;
import com.example.exception.BusinessException;
import com.example.mapper.DuplicateMapper;
import com.example.mapper.exam.*;
import com.example.service.BaseService;
import com.example.service.QuestionService;
import com.example.utils.SimilarityUtil;
import com.example.utils.ThreadLocalUtil;
import com.example.vo.exam.PaperQuestionVO;
import com.example.vo.exam.PaperVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.config.ExportConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 试题服务实现类
 * <p>
 * 功能模块：
 * 1. 试题入库：单题新增、批量导入、暂存
 * 2. 试题查询：列表、详情、高级查询、全文检索
 * 3. 试题修改：更新、批量修改难度/学科
 * 4. 试题删除：软删除、回收站、永久删除
 * 5. 历史版本：版本记录、版本对比、版本回滚
 * 6. 选项管理：增删改查、随机顺序
 * 7. 答案验证：填空验证、简答验证、自动评分
 * 8. 重复检测：单题检测、批量检测、重复处理
 * 9. 统计分析：题型统计、难度统计、学科统计
 * 10. 导出功能：Word/Excel导出、导出历史
 *
 * @author System
 * @since 1.0
 */
@Slf4j
@Service
public class QuestionServiceImpl extends BaseService implements QuestionService {

    @Autowired
    private QuestionMapper questionMapper;

    @Autowired
    private QuestionChoiceMapper choiceMapper;

    @Autowired
    private QuestionAnswerMapper answerMapper;

    @Autowired
    private QuestionHistoryMapper historyMapper;
    @Autowired
    private QueryHistoryMapper queryHistoryMapper;

    @Autowired
    private DuplicateMapper duplicateMapper;

    @Autowired
    private SimilarityUtil similarityUtil;

    @Autowired
    private ExportTaskMapper exportTaskMapper;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ExportConfig exportConfig;

    @Autowired
    private SubjectMapper subjectMapper;

    private static final int DEFAULT_SIMILARITY_THRESHOLD = 70;
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer insertQuestion(QuestionInsertDTO dto) {
        Integer userId = getCurrentUserId();

        if (checkDuplicate(dto.getContent()) > 0) {
            throw new BusinessException("试题内容已存在，请勿重复添加");
        }

        Question question = new Question();
        question.setQuestionType(dto.getQuestionType());
        question.setContent(dto.getContent());
        question.setAnalysis(dto.getAnalysis());
        question.setDifficulty(dto.getDifficulty());
        question.setSubjectId(dto.getSubjectId());
        question.setSuggestedTime(dto.getSuggestedTime() == null ? 0 : dto.getSuggestedTime());
        question.setSource(dto.getSource() == null ? "自编" : dto.getSource());
        question.setCreatorId(userId);
        question.setEditorId(userId);
        question.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());

        questionMapper.insert(question);

        saveQuestionDetails(question.getId(), dto);

        return question.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer updateQuestion(QuestionInsertDTO dto) {
        Integer userId = getCurrentUserId();

        if (dto.getId() == null) {
            throw new BusinessException("更新试题时必须提供ID");
        }

        Question existing = questionMapper.findById(dto.getId());
        if (existing == null) {
            throw new BusinessException("试题不存在");
        }

        if (!existing.getCreatorId().equals(userId)) {
            throw new BusinessException("无权修改他人的试题");
        }

        // 题型不可修改
        if (!existing.getQuestionType().equals(dto.getQuestionType())) {
            throw new BusinessException("题型不可修改");
        }

        // 保存历史版本
        QuestionHistory history = new QuestionHistory();
        history.setQuestionId(existing.getId());
        history.setContent(existing.getContent());
        history.setAnalysis(existing.getAnalysis());
        history.setDifficulty(existing.getDifficulty());
        history.setOperatorId(userId);
        history.setOperationType("UPDATE");
        history.setChangeNote(dto.getChangeNote() != null ? dto.getChangeNote() : "修改试题");
        historyMapper.insert(history);

        Question question = new Question();
        question.setId(dto.getId());
        question.setQuestionType(dto.getQuestionType());
        question.setContent(dto.getContent());
        question.setAnalysis(dto.getAnalysis());
        question.setDifficulty(dto.getDifficulty());
        question.setSubjectId(dto.getSubjectId());
        question.setSuggestedTime(dto.getSuggestedTime() == null ? 0 : dto.getSuggestedTime());
        question.setSource(dto.getSource() == null ? "自编" : dto.getSource());
        question.setEditorId(userId);
        question.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());

        questionMapper.update(question);

        // 删除旧选项/答案
        choiceMapper.deleteByQuestionId(dto.getId());
        answerMapper.deleteByQuestionId(dto.getId());

        // 插入新选项/答案
        saveQuestionDetails(dto.getId(), dto);

        return dto.getId();
    }

    // QuestionServiceImpl.java 中的 saveQuestionDetails 方法增强

    private void saveQuestionDetails(Integer questionId, QuestionInsertDTO dto) {
        if (dto.getQuestionType() == 1) {  // 单选题
            if (dto.getChoices() == null || dto.getChoices().isEmpty()) {
                throw new BusinessException("单选题必须提供选项");
            }

            // 验证选项数量（标准是4个）
            if (dto.getChoices().size() != 4) {
                throw new BusinessException("单选题应有4个选项");
            }

            // 验证只有一个正确选项
            long correctCount = dto.getChoices().stream()
                    .filter(c -> c.getIsCorrect() != null && c.getIsCorrect() == 1)
                    .count();
            if (correctCount != 1) {
                throw new BusinessException("单选题必须有且仅有一个正确答案");
            }

            // 保存选项
            List<QuestionChoice> choices = dto.getChoices().stream().map(c -> {
                QuestionChoice choice = new QuestionChoice();
                choice.setQuestionId(questionId);
                choice.setOptionLabel(c.getOptionLabel());
                choice.setOptionContent(c.getOptionContent());
                choice.setIsCorrect(c.getIsCorrect());
                choice.setSortOrder(c.getSortOrder() == null ? 0 : c.getSortOrder());
                return choice;
            }).collect(Collectors.toList());
            choiceMapper.batchInsert(choices);

        } else if (dto.getQuestionType() == 2) {  // 多选题
            if (dto.getChoices() == null || dto.getChoices().isEmpty()) {
                throw new BusinessException("多选题必须提供选项");
            }

            // 验证选项数量（至少2个，最多6个）
            if (dto.getChoices().size() < 2 || dto.getChoices().size() > 6) {
                throw new BusinessException("多选题应有2-6个选项");
            }

            // 验证至少有一个正确选项
            long correctCount = dto.getChoices().stream()
                    .filter(c -> c.getIsCorrect() != null && c.getIsCorrect() == 1)
                    .count();
            if (correctCount < 1) {
                throw new BusinessException("多选题至少有一个正确答案");
            }
            if (correctCount > 5) {
                throw new BusinessException("多选题正确选项不能超过5个");
            }

            // 保存选项
            List<QuestionChoice> choices = dto.getChoices().stream().map(c -> {
                QuestionChoice choice = new QuestionChoice();
                choice.setQuestionId(questionId);
                choice.setOptionLabel(c.getOptionLabel());
                choice.setOptionContent(c.getOptionContent());
                choice.setIsCorrect(c.getIsCorrect());
                choice.setSortOrder(c.getSortOrder() == null ? 0 : c.getSortOrder());
                return choice;
            }).collect(Collectors.toList());
            choiceMapper.batchInsert(choices);

        } else if (dto.getQuestionType() == 3) {
            if (dto.getAnswers() != null && !dto.getAnswers().isEmpty()) {
                List<QuestionAnswer> answers = dto.getAnswers().stream().map(a -> {
                    QuestionAnswer answer = new QuestionAnswer();
                    answer.setQuestionId(questionId);
                    answer.setBlankIndex(a.getBlankIndex() == null ? 1 : a.getBlankIndex());
                    answer.setAnswerText(a.getAnswerText());
                    answer.setMatchType(a.getMatchType() == null ? 2 : a.getMatchType());
                    answer.setCaseSensitive(a.getCaseSensitive() == null ? 0 : a.getCaseSensitive());
                    return answer;
                }).collect(Collectors.toList());
                answerMapper.batchInsert(answers);
            } else {
                throw new BusinessException("填空题必须提供答案");
            }
        } else if (dto.getQuestionType() == 4) {
            if (dto.getAnswers() != null && !dto.getAnswers().isEmpty()) {
                List<QuestionAnswer> answers = dto.getAnswers().stream().map(a -> {
                    QuestionAnswer answer = new QuestionAnswer();
                    answer.setQuestionId(questionId);
                    answer.setAnswerText(a.getAnswerText());
                    answer.setIsKeyPoint(1);
                    answer.setKeyPointScore(a.getKeyPointScore() == null ? 5 : a.getKeyPointScore());
                    answer.setMatchType(2);
                    return answer;
                }).collect(Collectors.toList());
                answerMapper.batchInsert(answers);
            }
        }
    }

    @Override
    public Integer checkDuplicate(String content) {
        return questionMapper.countByContent(content);
    }

    @Override
    public List<Question> getDrafts() {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer userId = (Integer) claims.get("id");
        return questionMapper.findDraftsByUser(userId);
    }

    @Override
    public Question getDraftDetail(Integer id) {
        Question question = questionMapper.findById(id);
        if (question == null || question.getStatus() != 0) {
            throw new BusinessException("试题不存在或不是暂存状态");
        }
        loadQuestionDetails(question);
        return question;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDraft(Integer id, boolean physical) {
        Integer userId = getCurrentUserId();

        Question question = questionMapper.findById(id);
        if (question == null || !question.getCreatorId().equals(userId)) {
            throw new BusinessException("无权操作此试题");
        }

        if (physical) {
            choiceMapper.deleteByQuestionId(id);
            answerMapper.deleteByQuestionId(id);
            questionMapper.physicalDelete(id);
        } else {
            questionMapper.softDelete(id, "用户删除暂存");
        }
    }

    @Override
    public Question getQuestionDetail(Integer id) {
        Question question = questionMapper.findById(id);
        if (question == null) {
            throw new BusinessException("试题不存在");
        }
        loadQuestionDetails(question);
        return question;
    }

    private void loadQuestionDetails(Question question) {
        if (question.getQuestionType() == 1 || question.getQuestionType() == 2) {
            question.setChoices(choiceMapper.findByQuestionId(question.getId()));
        } else if (question.getQuestionType() == 3 || question.getQuestionType() == 4) {
            question.setAnswers(answerMapper.findByQuestionId(question.getId()));
        }
    }

    @Override
    public List<Question> getQuestionList(Integer pageNum, Integer pageSize, Integer type, String keyword) {
        int offset = (pageNum - 1) * pageSize;
        return questionMapper.getQuestionList(offset, pageSize, type, keyword);
    }

    @Override
    public Integer getQuestionCount(Integer type, String keyword) {
        return questionMapper.getQuestionCount(type, keyword);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchUpdateDifficulty(BatchUpdateDTO dto) {
        Integer userId = getCurrentUserId();

        if (dto.getIds() == null || dto.getIds().isEmpty()) {
            throw new BusinessException("请选择要更新的试题");
        }

        for (Integer id : dto.getIds()) {
            Question question = questionMapper.findById(id);
            if (question == null) {
                throw new BusinessException("试题ID " + id + " 不存在");
            }
            if (!question.getCreatorId().equals(userId)) {
                throw new BusinessException("无权修改他人的试题 ID: " + id);
            }

            QuestionHistory history = new QuestionHistory();
            history.setQuestionId(question.getId());
            history.setContent(question.getContent());
            history.setAnalysis(question.getAnalysis());
            history.setDifficulty(question.getDifficulty());
            history.setOperatorId(userId);
            history.setOperationType("BATCH_UPDATE");
            history.setChangeNote(dto.getChangeNote() != null ? dto.getChangeNote() : "批量修改难度为" + dto.getDifficulty());
            historyMapper.insert(history);
        }

        questionMapper.batchUpdateDifficulty(dto.getIds(), dto.getDifficulty());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchUpdateSubject(BatchUpdateDTO dto) {
        Integer userId = getCurrentUserId();

        if (dto.getIds() == null || dto.getIds().isEmpty()) {
            throw new BusinessException("请选择要更新的试题");
        }

        for (Integer id : dto.getIds()) {
            Question question = questionMapper.findById(id);
            if (question == null) {
                throw new BusinessException("试题ID " + id + " 不存在");
            }
            if (!question.getCreatorId().equals(userId)) {
                throw new BusinessException("无权修改他人的试题 ID: " + id);
            }

            QuestionHistory history = new QuestionHistory();
            history.setQuestionId(question.getId());
            history.setContent(question.getContent());
            history.setAnalysis(question.getAnalysis());
            history.setDifficulty(question.getDifficulty());
            history.setOperatorId(userId);
            history.setOperationType("BATCH_UPDATE");
            history.setChangeNote(dto.getChangeNote() != null ? dto.getChangeNote() : "批量修改学科");
            historyMapper.insert(history);
        }

        questionMapper.batchUpdateSubject(dto.getIds(), dto.getSubjectId());
    }

    @Override
    public List<QuestionHistory> getQuestionHistory(Integer questionId) {
        return historyMapper.findByQuestionId(questionId);
    }

    @Override
    public Map<String, Object> compareVersions(Integer historyId1, Integer historyId2) {
        QuestionHistory version1 = historyMapper.findById(historyId1);
        QuestionHistory version2 = historyMapper.findById(historyId2);

        if (version1 == null || version2 == null) {
            throw new BusinessException("历史版本不存在");
        }

        Map<String, Object> comparison = new HashMap<>();
        comparison.put("contentChanged", !equals(version1.getContent(), version2.getContent()));
        comparison.put("content1", version1.getContent());
        comparison.put("content2", version2.getContent());
        comparison.put("analysisChanged", !equals(version1.getAnalysis(), version2.getAnalysis()));
        comparison.put("analysis1", version1.getAnalysis());
        comparison.put("analysis2", version2.getAnalysis());
        comparison.put("difficultyChanged", !version1.getDifficulty().equals(version2.getDifficulty()));
        comparison.put("difficulty1", version1.getDifficulty());
        comparison.put("difficulty2", version2.getDifficulty());

        return comparison;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer rollbackToVersion(Integer questionId, Integer historyId) {
        Integer userId = getCurrentUserId();

        QuestionHistory history = historyMapper.findById(historyId);
        if (history == null || !history.getQuestionId().equals(questionId)) {
            throw new BusinessException("历史版本不存在");
        }

        Question current = questionMapper.findById(questionId);
        if (current == null) {
            throw new BusinessException("试题不存在");
        }

        if (!current.getCreatorId().equals(userId)) {
            throw new BusinessException("无权修改他人的试题");
        }

        QuestionHistory currentHistory = new QuestionHistory();
        currentHistory.setQuestionId(current.getId());
        currentHistory.setContent(current.getContent());
        currentHistory.setAnalysis(current.getAnalysis());
        currentHistory.setDifficulty(current.getDifficulty());
        currentHistory.setOperatorId(userId);
        currentHistory.setOperationType("ROLLBACK");
        currentHistory.setChangeNote("回滚到历史版本 " + historyId);
        historyMapper.insert(currentHistory);

        Question rollback = new Question();
        rollback.setId(questionId);
        rollback.setContent(history.getContent());
        rollback.setAnalysis(history.getAnalysis());
        rollback.setDifficulty(history.getDifficulty());
        rollback.setEditorId(userId);
        rollback.setQuestionType(current.getQuestionType());
        rollback.setSubjectId(current.getSubjectId());
        rollback.setSuggestedTime(current.getSuggestedTime());
        rollback.setSource(current.getSource());

        questionMapper.update(rollback);

        return questionId;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteQuestion(Integer id, String deleteReason) {
        Integer userId = getCurrentUserId();

        Question question = questionMapper.findById(id);
        if (question == null) {
            throw new BusinessException("试题不存在");
        }

        if (!question.getCreatorId().equals(userId)) {
            throw new BusinessException("无权删除他人的试题");
        }

        QuestionHistory history = new QuestionHistory();
        history.setQuestionId(question.getId());
        history.setContent(question.getContent());
        history.setAnalysis(question.getAnalysis());
        history.setDifficulty(question.getDifficulty());
        history.setOperatorId(userId);
        history.setOperationType("DELETE");
        history.setChangeNote(deleteReason != null ? deleteReason : "删除试题");
        historyMapper.insert(history);

        questionMapper.softDelete(id, deleteReason);
    }

    @Override
    public List<Question> getRecycleBin() {
        checkAdminPermission();
        return questionMapper.getRecycleBin();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void restoreQuestion(Integer id) {
        Integer userId = getCurrentUserId();
        Integer userRole = getCurrentRole();

        Question question = questionMapper.findById(id);
        if (question == null) {
            throw new BusinessException("试题不存在");
        }

        if (userRole != 1 && !question.getCreatorId().equals(userId)) {
            throw new BusinessException("无权恢复他人的试题");
        }

        questionMapper.restoreQuestion(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void permanentDelete(Integer id) {
        checkAdminPermission();

        choiceMapper.deleteByQuestionId(id);
        answerMapper.deleteByQuestionId(id);
        questionMapper.permanentDelete(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BatchImportResult batchImport(MultipartFile file) throws Exception {
        BatchImportResult result = new BatchImportResult();
        List<String> errors = new ArrayList<>();
        int success = 0;
        int total = 0;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            String line = reader.readLine();
            if (line == null) {
                throw new BusinessException("CSV文件为空");
            }

            int lineNum = 1;
            while ((line = reader.readLine()) != null) {
                lineNum++;
                total++;

                try {
                    if (line.trim().isEmpty()) {
                        continue;
                    }

                    String[] fields = line.split(",");
                    if (fields.length < 5) {
                        errors.add("第" + lineNum + "行：字段数不足");
                        continue;
                    }

                    QuestionInsertDTO dto = parseCsvLine(fields);
                    insertQuestion(dto);
                    success++;

                } catch (Exception e) {
                    errors.add("第" + lineNum + "行：" + e.getMessage());
                }
            }
        }

        result.setTotal(total);
        result.setSuccess(success);
        result.setFail(total - success);
        result.setErrorMessages(errors);
        return result;
    }

    private QuestionInsertDTO parseCsvLine(String[] fields) {
        QuestionInsertDTO dto = new QuestionInsertDTO();
        dto.setQuestionType(Integer.parseInt(fields[0].trim()));
        dto.setSubjectId(Integer.parseInt(fields[1].trim()));
        dto.setDifficulty(Integer.parseInt(fields[2].trim()));
        dto.setContent(fields[3].trim());
        dto.setAnalysis(fields.length > 4 ? fields[4].trim() : "");

        if (dto.getQuestionType() == 1 || dto.getQuestionType() == 2) {
            List<ChoiceDTO> choices = new ArrayList<>();
            for (int i = 5; i < fields.length && i + 1 < fields.length; i += 2) {
                if (fields[i].trim().isEmpty()) {
                    break;
                }

                ChoiceDTO choice = new ChoiceDTO();
                choice.setOptionLabel(String.valueOf((char) ('A' + (i - 5) / 2)));
                choice.setOptionContent(fields[i].trim());
                choice.setIsCorrect("正确".equals(fields[i + 1].trim()) ? 1 : 0);
                choice.setSortOrder((i - 5) / 2);
                choices.add(choice);
            }
            dto.setChoices(choices);
        }

        return dto;
    }

    private boolean equals(Object obj1, Object obj2) {
        if (obj1 == null && obj2 == null) {
            return true;
        }
        if (obj1 == null || obj2 == null) {
            return false;
        }
        return obj1.equals(obj2);
    }

    // ========== 查询模块实现 ==========

    @Override
    public Map<String, Object> advancedQuery(QuestionQueryDTO query, Integer userId) {
        int offset = (query.getPageNum() - 1) * query.getPageSize();

        List<Question> questions = questionMapper.advancedQuery(query, userId, offset);
        int total = questionMapper.countAdvancedQuery(query, userId);

        // 关键词高亮处理
        if (query.getHighlight() != null && query.getHighlight() && query.getKeyword() != null) {
            for (Question q : questions) {
                q.setContent(highlightKeywords(q.getContent(), query.getKeyword()));
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("list", questions);
        result.put("total", total);
        result.put("pageNum", query.getPageNum());
        result.put("pageSize", query.getPageSize());
        result.put("pages", (int) Math.ceil((double) total / query.getPageSize()));

        // 保存查询历史
        if (hasQueryCondition(query)) {
            saveQueryHistory(query, userId, total);
        }

        return result;
    }

    @Override
    public Map<String, Object> fullTextSearch(String keywords, QuestionQueryDTO query, Integer userId) {
        // 使用LIKE进行全文搜索
        query.setKeyword(keywords);
        return advancedQuery(query, userId);
    }

    @Override
    public void saveQueryCondition(String queryName, QuestionQueryDTO query, Integer userId, Integer resultCount) {
        try {
            // 确保 queryName 不为空
            if (queryName == null || queryName.trim().isEmpty()) {
                queryName = "未命名查询_" + System.currentTimeMillis();
            }

            String queryJson = objectMapper.writeValueAsString(query);
            QueryHistory history = new QueryHistory();
            history.setUserId(userId);
            history.setQueryCondition(queryJson);
            history.setQueryName(queryName);  // 确保这里设置了值
            history.setResultCount(resultCount);

            int result = queryHistoryMapper.insert(history);
            System.out.println("保存查询历史结果: " + result + ", queryName: " + queryName);

        } catch (Exception e) {
            log.error("保存查询条件失败", e);
            throw new BusinessException("保存查询条件失败");
        }
    }

    @Override
    public List<QueryHistory> getQueryHistory(Integer userId) {
        return queryHistoryMapper.findByUserId(userId);
    }

    @Override
    public void deleteQueryHistory(Integer id, Integer userId) {
        queryHistoryMapper.deleteByUserAndId(userId, id);
    }

    // ========== 导出模块实现 ==========


    /**
     * 获取导出文件路径
     */
    private String getExportFilePath() {
        String path = exportConfig.getPath();
        // 确保路径以分隔符结尾
        if (!path.endsWith(File.separator)) {
            path += File.separator;
        }
        return path;
    }

    /**
     * 获取文件保留天数
     */
    private Integer getRetentionDays() {
        return exportConfig.getRetention().getDays();
    }
    @Override
    public String exportQuestions(QuestionExportDTO exportDTO, Integer userId) throws Exception {
        List<Question> questions = getQuestionsForExport(exportDTO);
        if (questions.isEmpty()) {
            throw new BusinessException("没有找到要导出的试题");
        }

        // 确保导出目录存在
        String exportFilePath = getExportFilePath();
        File dir = new File(exportFilePath);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        String fileName = "试题导出_" + LocalDateTime.now().format(DATE_FORMATTER);
        String filePath;

        if ("excel".equalsIgnoreCase(exportDTO.getFormat())) {
            filePath = exportToExcel(questions, fileName, exportFilePath);
        } else {
            filePath = exportToWord(questions, fileName, exportFilePath);
        }

        // 保存导出记录 - 使用绝对路径
        ExportTask task = new ExportTask();
        task.setUserId(userId);
        task.setExportFormat(exportDTO.getFormat());
        task.setQuestionCount(questions.size());
        task.setFileUrl(filePath);  // 保存完整路径
        exportTaskMapper.insert(task);

        // 返回文件路径（相对路径或文件名，供前端下载）
        return filePath;
    }

    @Override
    public List<ExportTask> getExportHistory(Integer userId) {
        return exportTaskMapper.findByUserId(userId);
    }

    @Override
    public List<ExportTask> getAllExportHistory() {
        return exportTaskMapper.findAll();
    }

    @Override
    public void deleteExportTask(Integer id, Integer userId, boolean isAdmin) {
        ExportTask task = exportTaskMapper.findById(id);
        if (task == null) {
            throw new BusinessException("导出记录不存在");
        }

        if (!isAdmin && !task.getUserId().equals(userId)) {
            throw new BusinessException("无权删除此记录");
        }

        // 删除文件
        if (task.getFileUrl() != null) {
            File file = new File(task.getFileUrl());
            if (file.exists()) {
                file.delete();
            }
        }

        exportTaskMapper.deleteById(id);
    }

    @Override
    public byte[] downloadExportFile(Integer taskId, Integer userId, boolean isAdmin) throws Exception {
        ExportTask task = exportTaskMapper.findById(taskId);
        if (task == null) {
            throw new BusinessException("导出记录不存在");
        }

        if (!isAdmin && !task.getUserId().equals(userId)) {
            throw new BusinessException("无权下载此文件");
        }

        File file = new File(task.getFileUrl());
        if (!file.exists()) {
            throw new BusinessException("文件不存在或已过期");
        }

        try (FileInputStream fis = new FileInputStream(file)) {
            return fis.readAllBytes();
        }
    }

    // ========== 私有辅助方法 ==========

    private List<Question> getQuestionsForExport(QuestionExportDTO exportDTO) {
        if (exportDTO.getQuestionIds() != null && !exportDTO.getQuestionIds().isEmpty()) {
            return questionMapper.findByIds(exportDTO.getQuestionIds());
        }
        // 导出所有正常试题
        return questionMapper.getQuestionList(0, 10000, null, null);
    }

    /**
     * 导出为Excel（使用POI）
     */
    private String exportToExcel(List<Question> questions, String fileName, String exportFilePath) throws IOException {
        String filePath = exportFilePath + fileName + ".xlsx";

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("试题库");

        // 创建样式
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setBorderBottom(BorderStyle.THIN);
        headerStyle.setBorderTop(BorderStyle.THIN);
        headerStyle.setBorderLeft(BorderStyle.THIN);
        headerStyle.setBorderRight(BorderStyle.THIN);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);

        CellStyle contentStyle = workbook.createCellStyle();
        contentStyle.setBorderBottom(BorderStyle.THIN);
        contentStyle.setBorderTop(BorderStyle.THIN);
        contentStyle.setBorderLeft(BorderStyle.THIN);
        contentStyle.setBorderRight(BorderStyle.THIN);
        contentStyle.setVerticalAlignment(VerticalAlignment.TOP);
        contentStyle.setWrapText(true);

        // 创建表头
        Row headerRow = sheet.createRow(0);
        String[] headers = {"序号", "题型", "题目内容", "难度", "学科", "解析", "选项/答案", "创建时间"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
            sheet.setColumnWidth(i, 5000);
        }

        // 填充数据
        int rowNum = 1;
        for (int i = 0; i < questions.size(); i++) {
            Question q = questions.get(i);
            Row row = sheet.createRow(rowNum++);

            row.createCell(0).setCellValue(i + 1);
            row.createCell(1).setCellValue(getTypeName(q.getQuestionType()));
            row.createCell(2).setCellValue(q.getContent());
            row.createCell(3).setCellValue(getDifficultyName(q.getDifficulty()));
            row.createCell(4).setCellValue(getSubjectName(q.getSubjectId()));
            row.createCell(5).setCellValue(q.getAnalysis() == null ? "" : q.getAnalysis());
            row.createCell(6).setCellValue(getQuestionDetailText(q));
            row.createCell(7).setCellValue(q.getCreateTime() == null ? "" :
                    q.getCreateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

            // 应用样式
            for (int j = 0; j < headers.length; j++) {
                row.getCell(j).setCellStyle(contentStyle);
            }
        }

        // 写入文件
        try (FileOutputStream fos = new FileOutputStream(filePath)) {
            workbook.write(fos);
        }
        workbook.close();

        return filePath;
    }

    /**
     * 导出为Word（HTML格式）
     */
    private String exportToWord(List<Question> questions, String fileName, String exportFilePath) throws IOException {
        String filePath = exportFilePath + fileName + ".doc";

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>");
        html.append("<html><head><meta charset='UTF-8'>");
        html.append("<title>试题导出</title>");
        html.append("<style>");
        html.append("body{font-family:SimSun;margin:20px;}");
        html.append(".question{margin-bottom:30px;page-break-after:avoid;}");
        html.append(".question-title{font-weight:bold;font-size:14pt;margin-bottom:10px;}");
        html.append(".question-content{margin-left:20px;margin-bottom:10px;}");
        html.append(".choice{margin-left:30px;}");
        html.append(".answer{margin-left:30px;color:#0066cc;}");
        html.append(".analysis{margin-left:20px;color:#666;margin-top:10px;}");
        html.append("</style></head><body>");

        for (int i = 0; i < questions.size(); i++) {
            Question q = questions.get(i);
            html.append("<div class='question'>");
            html.append("<div class='question-title'>");
            html.append(i + 1).append(". ").append(getTypeName(q.getQuestionType()));
            html.append(" (").append(getDifficultyName(q.getDifficulty())).append(")");
            html.append("</div>");
            html.append("<div class='question-content'>");
            html.append(escapeHtml(q.getContent()));
            html.append("</div>");

            // 添加选项
            if (q.getQuestionType() == 1 || q.getQuestionType() == 2) {
                List<QuestionChoice> choices = choiceMapper.findByQuestionId(q.getId());
                for (QuestionChoice choice : choices) {
                    html.append("<div class='choice'>");
                    html.append(choice.getOptionLabel()).append(". ");
                    html.append(escapeHtml(choice.getOptionContent()));
                    // 修改后（正确）
                    if (choice.getIsCorrect() != null && choice.getIsCorrect() == 1) {
                        html.append(" <span style='color:red'>(正确答案)</span>");
                    }
                    html.append("</div>");
                }
            }

            // 添加答案
            if (q.getQuestionType() == 3 || q.getQuestionType() == 4) {
                List<QuestionAnswer> answers = answerMapper.findByQuestionId(q.getId());
                html.append("<div class='answer'><strong>参考答案：</strong><br>");
                for (QuestionAnswer answer : answers) {
                    if (q.getQuestionType() == 3) {
                        html.append("填空").append(answer.getBlankIndex()).append("：");
                    }
                    html.append(escapeHtml(answer.getAnswerText()));
                    // 修改后（正确）
                    if (answer.getIsKeyPoint() != null && answer.getIsKeyPoint() == 1 && answer.getKeyPointScore() != null) {
                        html.append(" (").append(answer.getKeyPointScore()).append("分)");
                    }
                    html.append("<br>");
                }
                html.append("</div>");
            }

            // 添加解析
            if (q.getAnalysis() != null && !q.getAnalysis().isEmpty()) {
                html.append("<div class='analysis'><strong>试题解析：</strong><br>");
                html.append(escapeHtml(q.getAnalysis()));
                html.append("</div>");
            }

            html.append("</div><hr>");
        }

        html.append("</body></html>");

        try (FileOutputStream fos = new FileOutputStream(filePath)) {
            fos.write(html.toString().getBytes("UTF-8"));
        }

        return filePath;
    }

    /**
     * 清理过期文件（可以定时执行）
     */
    @Scheduled(cron = "0 0 2 * * ?")  // 每天凌晨2点执行
    public void cleanExpiredFiles() {
        Integer retentionDays = getRetentionDays();
        LocalDateTime expireTime = LocalDateTime.now().minusDays(retentionDays);

        List<ExportTask> expiredTasks = exportTaskMapper.findByCreateTimeBefore(expireTime);

        for (ExportTask task : expiredTasks) {
            if (task.getFileUrl() != null) {
                File file = new File(task.getFileUrl());
                if (file.exists()) {
                    boolean deleted = file.delete();
                    if (deleted) {
                        System.out.println("已删除过期文件: " + task.getFileUrl());
                    }
                }
            }
            exportTaskMapper.deleteById(task.getId());
        }
    }

    private String getTypeName(Integer type) {
        if (type == null) {
            return "";
        }
        switch (type) {
            case 1: return "单选题";
            case 2: return "多选题";
            case 3: return "填空题";
            case 4: return "简答题";
            default: return "未知";
        }
    }

    private String getDifficultyName(Integer difficulty) {
        if (difficulty == null) {
            return "";
        }
        switch (difficulty) {
            case 1: return "简单";
            case 2: return "中等";
            case 3: return "困难";
            default: return "未知";
        }
    }

    private String getSubjectName(Integer subjectId) {
        // 简化处理，实际可以从缓存获取
        return subjectId == null ? "" : "学科" + subjectId;
    }

    private String getQuestionDetailText(Question question) {
        if (question.getQuestionType() == 1 || question.getQuestionType() == 2) {
            List<QuestionChoice> choices = choiceMapper.findByQuestionId(question.getId());
            StringBuilder sb = new StringBuilder();
            for (QuestionChoice choice : choices) {
                sb.append(choice.getOptionLabel()).append(". ");
                sb.append(choice.getOptionContent());
                // 修复这里
                if (choice.getIsCorrect() != null && choice.getIsCorrect() == 1) {
                    sb.append("[正确]");
                }
                sb.append("; ");
            }
            return sb.toString();
        } else {
            List<QuestionAnswer> answers = answerMapper.findByQuestionId(question.getId());
            return answers.stream()
                    .map(QuestionAnswer::getAnswerText)
                    .collect(Collectors.joining("；"));
        }
    }

    private boolean hasQueryCondition(QuestionQueryDTO query) {
        return query.getKeyword() != null ||
                query.getQuestionType() != null ||
                query.getSubjectId() != null ||
                query.getDifficulty() != null ||
                query.getStartTime() != null ||
                query.getEndTime() != null ||
                (query.getDifficultyList() != null && !query.getDifficultyList().isEmpty()) ||
                (query.getSubjectIdList() != null && !query.getSubjectIdList().isEmpty()) ||
                (query.getQuestionTypeList() != null && !query.getQuestionTypeList().isEmpty());
    }

    private void saveQueryHistory(QuestionQueryDTO query, Integer userId, Integer resultCount) {
        try {
            String queryJson = objectMapper.writeValueAsString(query);
            QueryHistory history = new QueryHistory();
            history.setUserId(userId);
            history.setQueryCondition(queryJson);
            history.setResultCount(resultCount);
            queryHistoryMapper.insert(history);
        } catch (Exception e) {
            // 记录日志但不影响主流程
            e.printStackTrace();
        }
    }

    private String highlightKeywords(String content, String keyword) {
        if (content == null || keyword == null) {
            return content;
        }
        return content.replaceAll(
                "(?i)(" + keyword + ")",
                "<span style='color:red;font-weight:bold'>$1</span>"
        );
    }

    private String escapeHtml(String str) {
        if (str == null) {
            return "";
        }
        return str.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    // QuestionServiceImpl.java 中添加以下实现

// ==================== 选项管理模块实现 ===================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer addChoice(Integer questionId, ChoiceDTO choiceDTO) {
        Integer userId = getCurrentUserId();

        // 验证试题是否存在且属于当前用户
        Question question = questionMapper.findById(questionId);
        if (question == null) {
            throw new BusinessException("试题不存在");
        }

        // 验证题型必须是选择题（单选或多选）
        if (question.getQuestionType() != 1 && question.getQuestionType() != 2) {
            throw new BusinessException("只有选择题可以添加选项");
        }

        // 检查是否是本人创建
        if (!question.getCreatorId().equals(userId)) {
            throw new BusinessException("无权修改他人的试题");
        }

        // 获取当前选项数量
        List<QuestionChoice> existingChoices = choiceMapper.findByQuestionId(questionId);
        int currentCount = existingChoices.size();

        // 单选题最多4个选项，多选题最多6个选项
        int maxChoices = question.getQuestionType() == 1 ? 4 : 6;
        if (currentCount >= maxChoices) {
            throw new BusinessException("选项数量已达上限（最多" + maxChoices + "个）");
        }

        // 检查选项标签是否重复
        boolean labelExists = existingChoices.stream()
                .anyMatch(c -> c.getOptionLabel().equals(choiceDTO.getOptionLabel()));
        if (labelExists) {
            throw new BusinessException("选项标签 " + choiceDTO.getOptionLabel() + " 已存在");
        }

        // 如果是单选题且新选项标记为正确，需要清除原有的正确标记
        if (question.getQuestionType() == 1 && choiceDTO.getIsCorrect() == 1) {
            choiceMapper.clearAllCorrect(questionId);
        }

        // 如果是多选题，验证正确选项数量
        if (question.getQuestionType() == 2 && choiceDTO.getIsCorrect() == 1) {
            long correctCount = existingChoices.stream()
                    .filter(QuestionChoice::isCorrect)
                    .count();
            // 多选题最多5个正确答案
            if (correctCount >= 5) {
                throw new BusinessException("正确选项数量已达上限（最多5个）");
            }
        }

        // 创建新选项
        QuestionChoice choice = new QuestionChoice();
        choice.setQuestionId(questionId);
        choice.setOptionLabel(choiceDTO.getOptionLabel());
        choice.setOptionContent(choiceDTO.getOptionContent());
        choice.setIsCorrect(choiceDTO.getIsCorrect());
        choice.setSortOrder(choiceDTO.getSortOrder() != null ? choiceDTO.getSortOrder() : currentCount);

        choiceMapper.insert(choice);

        return choice.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateChoice(UpdateChoiceDTO dto) {
        Integer userId = getCurrentUserId();

        // 获取选项信息
        QuestionChoice choice = choiceMapper.findById(dto.getChoiceId());
        if (choice == null) {
            throw new BusinessException("选项不存在");
        }

        // 获取试题信息
        Question question = questionMapper.findById(choice.getQuestionId());
        if (question == null) {
            throw new BusinessException("试题不存在");
        }

        // 权限验证
        if (!question.getCreatorId().equals(userId)) {
            throw new BusinessException("无权修改他人的试题");
        }

        // 如果是单选题且要设置当前选项为正确
        if (question.getQuestionType() == 1 && dto.getIsCorrect() == 1) {
            // 清除该题所有选项的正确标记
            choiceMapper.clearAllCorrect(question.getId());
        }

        // 如果是多选题且要设置为正确，验证正确选项数量
        if (question.getQuestionType() == 2 && dto.getIsCorrect() == 1) {
            // 检查是否已经是正确选项
            if (choice.getIsCorrect() != 1) {
                List<QuestionChoice> allChoices = choiceMapper.findByQuestionId(question.getId());
                long correctCount = allChoices.stream()
                        .filter(c -> c.getIsCorrect() == 1 && !c.getId().equals(dto.getChoiceId()))
                        .count();
                if (correctCount >= 5) {
                    throw new BusinessException("正确选项数量已达上限（最多5个）");
                }
            }
        }

        // 更新选项
        choice.setOptionContent(dto.getOptionContent());
        choice.setIsCorrect(dto.getIsCorrect());
        if (dto.getSortOrder() != null) {
            choice.setSortOrder(dto.getSortOrder());
        }

        choiceMapper.updateChoice(choice);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteChoice(Integer choiceId) {
        Integer userId = getCurrentUserId();

        QuestionChoice choice = choiceMapper.findById(choiceId);
        if (choice == null) {
            throw new BusinessException("选项不存在");
        }

        Question question = questionMapper.findById(choice.getQuestionId());
        if (question == null) {
            throw new BusinessException("试题不存在");
        }

        // 权限验证
        if (!question.getCreatorId().equals(userId)) {
            throw new BusinessException("无权修改他人的试题");
        }

        // 获取当前选项列表
        List<QuestionChoice> choices = choiceMapper.findByQuestionId(question.getId());
        if (choices.size() <= 2) {
            throw new BusinessException("至少保留2个选项");
        }

        // 如果删除的是正确选项，且是单选题，需要提示用户重新设置正确选项
        if (question.getQuestionType() == 1 && choice.getIsCorrect() == 1) {
            // 删除正确选项后，如果没有其他正确选项，需要用户重新设置
            choiceMapper.deleteById(choiceId);
            // 清除所有正确标记，因为原来的正确选项被删除了
            choiceMapper.clearAllCorrect(question.getId());
        } else {
            choiceMapper.deleteById(choiceId);
        }
    }

    @Override
    public void setSingleCorrectOption(Integer questionId, String correctLabel) {
        Integer userId = getCurrentUserId();

        Question question = questionMapper.findById(questionId);
        if (question == null) {
            throw new BusinessException("试题不存在");
        }

        // 验证题型必须是单选题
        if (question.getQuestionType() != 1) {
            throw new BusinessException("只有单选题可以设置唯一正确答案");
        }

        // 权限验证
        if (!question.getCreatorId().equals(userId)) {
            throw new BusinessException("无权修改他人的试题");
        }

        // 验证选项标签是否存在
        List<QuestionChoice> choices = choiceMapper.findByQuestionId(questionId);
        boolean labelExists = choices.stream()
                .anyMatch(c -> c.getOptionLabel().equals(correctLabel));
        if (!labelExists) {
            throw new BusinessException("选项 " + correctLabel + " 不存在");
        }

        // 清除所有正确标记，然后设置当前选项为正确
        choiceMapper.clearAllCorrect(questionId);
        choiceMapper.updateCorrectFlag(questionId, correctLabel, 1);
    }

    @Override
    public void setMultipleCorrectOptions(Integer questionId, List<String> correctLabels) {
        Integer userId = getCurrentUserId();

        Question question = questionMapper.findById(questionId);
        if (question == null) {
            throw new BusinessException("试题不存在");
        }

        // 验证题型必须是多选题
        if (question.getQuestionType() != 2) {
            throw new BusinessException("只有多选题可以设置多个正确答案");
        }

        // 权限验证
        if (!question.getCreatorId().equals(userId)) {
            throw new BusinessException("无权修改他人的试题");
        }

        // 验证正确选项数量
        if (correctLabels == null || correctLabels.isEmpty()) {
            throw new BusinessException("至少选择一个正确答案");
        }
        if (correctLabels.size() > 5) {
            throw new BusinessException("正确选项不能超过5个");
        }

        List<QuestionChoice> choices = choiceMapper.findByQuestionId(questionId);

        // 验证所有选项标签都存在
        for (String label : correctLabels) {
            boolean exists = choices.stream()
                    .anyMatch(c -> c.getOptionLabel().equals(label));
            if (!exists) {
                throw new BusinessException("选项 " + label + " 不存在");
            }
        }

        // 清除所有正确标记，然后设置指定的选项为正确
        choiceMapper.clearAllCorrect(questionId);
        for (String label : correctLabels) {
            choiceMapper.updateCorrectFlag(questionId, label, 1);
        }
    }

    @Override
    public List<QuestionChoice> getChoicesByQuestionId(Integer questionId) {
        return choiceMapper.findByQuestionId(questionId);
    }

    @Override
    public List<QuestionChoice> getRandomOrderChoices(Integer questionId) {
        List<QuestionChoice> choices = choiceMapper.findByQuestionId(questionId);
        // 随机打乱顺序，但保持正确选项的标记不变
        Collections.shuffle(choices);
        return choices;
    }

    // QuestionServiceImpl.java 中添加

// ==================== 填空题/简答题验证模块实现 ====================

    @Override
    public Map<String, Object> validateBlankAnswers(Integer questionId, List<String> userAnswers) {
        // 获取试题
        Question question = questionMapper.findById(questionId);
        if (question == null) {
            throw new BusinessException("试题不存在");
        }

        // 验证题型必须是填空题
        if (question.getQuestionType() != 3) {
            throw new BusinessException("该试题不是填空题");
        }

        // 获取所有标准答案
        List<QuestionAnswer> standardAnswers = answerMapper.findByQuestionId(questionId);
        if (standardAnswers.isEmpty()) {
            throw new BusinessException("该试题没有设置答案");
        }

        // 按填空序号分组
        Map<Integer, List<QuestionAnswer>> answersByBlank = standardAnswers.stream()
                .collect(Collectors.groupingBy(QuestionAnswer::getBlankIndex));

        // 验证结果
        List<Map<String, Object>> blankResults = new ArrayList<>();
        int correctCount = 0;

        for (int i = 0; i < userAnswers.size(); i++) {
            int blankIndex = i + 1;
            String userAnswer = userAnswers.get(i);
            List<QuestionAnswer> correctAnswers = answersByBlank.getOrDefault(blankIndex, new ArrayList<>());

            boolean isCorrect = checkBlankAnswer(userAnswer, correctAnswers);

            Map<String, Object> blankResult = new HashMap<>();
            blankResult.put("blankIndex", blankIndex);
            blankResult.put("userAnswer", userAnswer);
            blankResult.put("isCorrect", isCorrect);
            blankResult.put("correctAnswers", correctAnswers.stream()
                    .map(QuestionAnswer::getAnswerText)
                    .collect(Collectors.toList()));

            blankResults.add(blankResult);
            if (isCorrect) {
                correctCount++;
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("totalBlanks", userAnswers.size());
        result.put("correctCount", correctCount);
        result.put("score", calculateBlankScore(correctCount, userAnswers.size()));
        result.put("results", blankResults);

        return result;
    }

    /**
     * 检查单个填空答案是否正确
     */
    private boolean checkBlankAnswer(String userAnswer, List<QuestionAnswer> correctAnswers) {
        if (userAnswer == null || userAnswer.trim().isEmpty()) {
            return false;
        }

        for (QuestionAnswer answer : correctAnswers) {
            if (isAnswerMatch(userAnswer, answer)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断答案是否匹配
     */
    private boolean isAnswerMatch(String userAnswer, QuestionAnswer answer) {
        String standardAnswer = answer.getAnswerText();
        String userInput = userAnswer;

        // 区分大小写处理
        if (answer.getCaseSensitive() == null || answer.getCaseSensitive() == 0) {
            standardAnswer = standardAnswer.toLowerCase();
            userInput = userInput.toLowerCase();
        }

        // 匹配方式
        if (answer.getMatchType() != null && answer.getMatchType() == 1) {
            // 完全匹配
            return userInput.equals(standardAnswer);
        } else {
            // 模糊匹配（包含）
            return userInput.contains(standardAnswer) || standardAnswer.contains(userInput);
        }
    }

    /**
     * 计算填空题得分（简单按比例，可自定义）
     */
    private int calculateBlankScore(int correctCount, int totalCount) {
        if (totalCount == 0) return 0;
        // 每题满分10分，按正确比例给分
        return (int) (10.0 * correctCount / totalCount);
    }

    @Override
    public Map<String, Object> validateEssayAnswer(Integer questionId, String userAnswer) {
        // 获取试题
        Question question = questionMapper.findById(questionId);
        if (question == null) {
            throw new BusinessException("试题不存在");
        }

        // 验证题型必须是简答题
        if (question.getQuestionType() != 4) {
            throw new BusinessException("该试题不是简答题");
        }

        // 获取所有评分要点
        List<QuestionAnswer> keyPoints = answerMapper.findByQuestionId(questionId).stream()
                .filter(QuestionAnswer::isKeyPoint)
                .collect(Collectors.toList());

        if (keyPoints.isEmpty()) {
            // 没有设置要点时，使用完整答案匹配
            return validateEssayWithFullAnswer(questionId, userAnswer);
        }

        // 按要点评分
        int totalScore = 0;
        int maxScore = 0;
        List<Map<String, Object>> matchedPoints = new ArrayList<>();

        for (QuestionAnswer point : keyPoints) {
            int pointScore = point.getKeyPointScore() != null ? point.getKeyPointScore() : 5;
            maxScore += pointScore;

            boolean matched = checkKeyPointMatch(userAnswer, point);

            Map<String, Object> pointResult = new HashMap<>();
            pointResult.put("keyPoint", point.getAnswerText());
            pointResult.put("maxScore", pointScore);
            pointResult.put("matched", matched);

            if (matched) {
                pointResult.put("score", pointScore);
                totalScore += pointScore;
            } else {
                pointResult.put("score", 0);
            }

            matchedPoints.add(pointResult);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("totalScore", totalScore);
        result.put("maxScore", maxScore);
        result.put("score", totalScore);
        result.put("matchedPoints", matchedPoints);
        result.put("userAnswer", userAnswer);

        // 检查答案长度
        if (userAnswer != null) {
            result.put("answerLength", userAnswer.length());
            if (userAnswer.length() < 20) {
                result.put("warning", "答案过于简短，建议详细回答");
            }
        }

        return result;
    }

    /**
     * 使用完整答案匹配（当没有设置要点时）
     */
    private Map<String, Object> validateEssayWithFullAnswer(Integer questionId, String userAnswer) {
        List<QuestionAnswer> fullAnswers = answerMapper.findByQuestionId(questionId).stream()
                .filter(a -> a.getIsKeyPoint() == null || a.getIsKeyPoint() == 0)
                .collect(Collectors.toList());

        boolean matched = false;
        if (!fullAnswers.isEmpty()) {
            for (QuestionAnswer answer : fullAnswers) {
                if (isAnswerMatch(userAnswer, answer)) {
                    matched = true;
                    break;
                }
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("totalScore", matched ? 10 : 0);
        result.put("maxScore", 10);
        result.put("score", matched ? 10 : 0);
        result.put("matched", matched);
        result.put("userAnswer", userAnswer);

        return result;
    }

    /**
     * 检查是否匹配评分要点（关键词匹配）
     */
    private boolean checkKeyPointMatch(String userAnswer, QuestionAnswer keyPoint) {
        if (userAnswer == null || keyPoint.getAnswerText() == null) {
            return false;
        }

        String userLower = userAnswer.toLowerCase();
        String keywordLower = keyPoint.getAnswerText().toLowerCase();

        // 支持多个关键词（用逗号分隔）
        String[] keywords = keywordLower.split("[，,]");
        for (String keyword : keywords) {
            String trimmed = keyword.trim();
            if (trimmed.isEmpty()) continue;

            if (userLower.contains(trimmed)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public List<QuestionAnswer> getBlankAnswers(Integer questionId) {
        Question question = questionMapper.findById(questionId);
        if (question == null || question.getQuestionType() != 3) {
            throw new BusinessException("该试题不是填空题");
        }
        return answerMapper.findByQuestionId(questionId);
    }

    @Override
    public List<QuestionAnswer> getEssayKeyPoints(Integer questionId) {
        Question question = questionMapper.findById(questionId);
        if (question == null || question.getQuestionType() != 4) {
            throw new BusinessException("该试题不是简答题");
        }

        return answerMapper.findByQuestionId(questionId).stream()
                .filter(QuestionAnswer::isKeyPoint)
                .collect(Collectors.toList());
    }

    @Override
    public AutoScoreResult autoScore(Integer questionId, Object userAnswer) {
        Question question = questionMapper.findById(questionId);
        if (question == null) {
            throw new BusinessException("试题不存在");
        }

        AutoScoreResult result = new AutoScoreResult();
        result.setQuestionId(questionId);
        result.setQuestionType(question.getQuestionType());

        switch (question.getQuestionType()) {
            case 1: // 单选题
            case 2: // 多选题
                // 选择题验证（已有逻辑）
                break;
            case 3: // 填空题
                if (userAnswer instanceof List) {
                    Map<String, Object> validateResult = validateBlankAnswers(questionId, (List<String>) userAnswer);
                    result.setScore((Integer) validateResult.get("score"));
                    result.setDetail(validateResult);
                }
                break;
            case 4: // 简答题
                if (userAnswer instanceof String) {
                    Map<String, Object> validateResult = validateEssayAnswer(questionId, (String) userAnswer);
                    result.setScore((Integer) validateResult.get("score"));
                    result.setDetail(validateResult);
                }
                break;
            default:
                throw new BusinessException("不支持的题型");
        }

        return result;
    }

   // ========== 重复检测模块实现 ==========

    @Override
    public Map<String, Object> checkDuplicate(Integer questionId, Integer similarityThreshold) {
        Question question = questionMapper.findById(questionId);
        if (question == null) {
            throw new BusinessException("试题不存在");
        }

        double threshold = similarityThreshold != null ? similarityThreshold : DEFAULT_SIMILARITY_THRESHOLD;

        // 获取所有其他正常题目
        List<Question> allQuestions = questionMapper.getAllNormalQuestions();
        List<Question> otherQuestions = allQuestions.stream()
                .filter(q -> !q.getId().equals(questionId))
                .collect(Collectors.toList());

        List<Map<String, Object>> duplicates = new ArrayList<>();

        for (Question other : otherQuestions) {
            double similarity = SimilarityUtil.calculateSimilarity(question.getContent(), other.getContent());
            if (similarity >= threshold) {
                Map<String, Object> dupInfo = new HashMap<>();
                dupInfo.put("duplicateQuestionId", other.getId());
                dupInfo.put("duplicateContent", other.getContent());
                dupInfo.put("similarity", similarity);
                dupInfo.put("level", SimilarityUtil.getDuplicateLevel(similarity));
                dupInfo.put("suggestion", SimilarityUtil.getSuggestion(similarity));
                duplicates.add(dupInfo);

                // 保存到duplicates表
                saveDuplicateRecord(questionId, other.getId(), (int) similarity);
            }
        }

        // 按相似度降序排序
        duplicates.sort((a, b) -> Double.compare(
                (double) b.get("similarity"), (double) a.get("similarity")));

        Map<String, Object> result = new HashMap<>();
        result.put("questionId", questionId);
        result.put("questionContent", question.getContent());
        result.put("isDuplicate", !duplicates.isEmpty());
        result.put("duplicateCount", duplicates.size());
        result.put("duplicates", duplicates);

        return result;
    }

    @Override
    public Map<String, Object> batchCheckAllDuplicates() {
        List<Question> allQuestions = questionMapper.getAllNormalQuestions();
        List<Map<String, Object>> allDuplicates = new ArrayList<>();
        int duplicatePairs = 0;

        for (int i = 0; i < allQuestions.size(); i++) {
            for (int j = i + 1; j < allQuestions.size(); j++) {
                Question q1 = allQuestions.get(i);
                Question q2 = allQuestions.get(j);
                double similarity = SimilarityUtil.calculateSimilarity(q1.getContent(), q2.getContent());

                if (similarity >= DEFAULT_SIMILARITY_THRESHOLD) {
                    duplicatePairs++;
                    Map<String, Object> pair = new HashMap<>();
                    pair.put("questionId1", q1.getId());
                    pair.put("questionContent1", q1.getContent());
                    pair.put("questionId2", q2.getId());
                    pair.put("questionContent2", q2.getContent());
                    pair.put("similarity", similarity);
                    pair.put("level", SimilarityUtil.getDuplicateLevel(similarity));
                    allDuplicates.add(pair);

                    saveDuplicateRecord(q1.getId(), q2.getId(), (int) similarity);
                }
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("totalChecked", allQuestions.size());
        result.put("duplicatePairs", duplicatePairs);
        result.put("duplicates", allDuplicates);
        result.put("message", String.format("批量检测完成，共检测%d道题，发现%d对重复题",
                allQuestions.size(), duplicatePairs));

        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean handleDuplicate(Integer duplicateId, String handleType) {
        checkAdminPermission();

        Duplicate duplicate = duplicateMapper.selectById(duplicateId);
        if (duplicate == null) {
            throw new BusinessException("重复记录不存在");
        }

        int status;
        if ("ignore".equals(handleType)) {
            status = 1; // 已忽略
        } else if ("merge".equals(handleType)) {
            status = 2; // 已合并
            // 合并逻辑：将question_id_b的题目标记为删除
            questionMapper.softDelete(duplicate.getQuestionIdB(), "与题目" + duplicate.getQuestionIdA() + "重复合并");
        } else {
            throw new BusinessException("无效的处理类型");
        }

        return duplicateMapper.updateStatus(duplicateId, status) > 0;
    }

    @Override
    public Map<String, Object> getDuplicateStatistics() {
        return duplicateMapper.getDuplicateStatistics();
    }

    @Override
    public List<Duplicate> getPendingDuplicates() {
        return duplicateMapper.getPendingDuplicates();
    }

    private void saveDuplicateRecord(Integer questionIdA, Integer questionIdB, int similarity) {
        if (duplicateMapper.existsDuplicate(questionIdA, questionIdB) == 0) {
            Duplicate duplicate = new Duplicate();
            duplicate.setQuestionIdA(questionIdA);
            duplicate.setQuestionIdB(questionIdB);
            duplicate.setSimilarity(similarity);
            duplicate.setStatus(0);
            duplicateMapper.insert(duplicate);
        }
    }

// ========== 统计分析模块实现 ==========

    @Override
    public Map<String, Object> getFullStatistics() {
        Map<String, Object> result = new HashMap<>();

        // 1. 总数统计
        Map<String, Object> totalStats = questionMapper.getTotalStatistics();
        result.put("totalCount", totalStats);

        // 2. 按题型统计
        List<Map<String, Object>> typeStats = questionMapper.getTypeStatistics();
        for (Map<String, Object> stat : typeStats) {
            Integer type = ((Number) stat.get("type")).intValue();
            stat.put("typeName", getTypeName(type));
        }
        result.put("typeStats", typeStats);

        // 3. 按学科统计
        List<Map<String, Object>> subjectStats = questionMapper.getSubjectStatistics();
        result.put("subjectStats", subjectStats);

        // 4. 按难度统计
        List<Map<String, Object>> difficultyStats = questionMapper.getDifficultyStatistics();
        for (Map<String, Object> stat : difficultyStats) {
            Integer difficulty = ((Number) stat.get("difficulty")).intValue();
            stat.put("difficultyName", getDifficultyName(difficulty));
        }
        result.put("difficultyStats", difficultyStats);

        // 5. 教师排行榜
        List<Map<String, Object>> teacherStats = questionMapper.getTeacherStatistics(10);
        int rank = 1;
        for (Map<String, Object> stat : teacherStats) {
            stat.put("rank", rank++);
        }
        result.put("teacherRank", teacherStats);

        // 6. 每日新增趋势（最近30天）
        LocalDateTime startDate = LocalDateTime.now().minusDays(30);
        List<Map<String, Object>> trendStats = questionMapper.getDailyTrend(startDate);
        result.put("trendStats", trendStats);

        // 7. 热门试题
        List<Map<String, Object>> hotQuestions = questionMapper.getHotQuestions(10);
        for (Map<String, Object> hot : hotQuestions) {
            Integer type = ((Number) hot.get("type")).intValue();
            hot.put("typeName", getTypeName(type));
        }
        result.put("hotQuestions", hotQuestions);

        // 8. 质量分析（重复率）
        Map<String, Object> dupStats = duplicateMapper.getDuplicateStatistics();
        if (dupStats != null && totalStats != null) {
            Integer total = ((Number) totalStats.get("total")).intValue();
            Integer duplicateTotal = ((Number) dupStats.getOrDefault("total", 0)).intValue();
            dupStats.put("duplicateRate", total > 0 ? duplicateTotal * 100.0 / total : 0);
        }
        result.put("qualityAnalysis", dupStats);

        return result;
    }

    @Override
    public byte[] exportStatisticsToExcel() throws Exception {
        Map<String, Object> stats = getFullStatistics();

        try (Workbook workbook = new XSSFWorkbook()) {
            // Sheet1: 总览
            Sheet overviewSheet = workbook.createSheet("总览统计");
            int rowNum = 0;
            Row headerRow = overviewSheet.createRow(rowNum++);
            headerRow.createCell(0).setCellValue("统计项");
            headerRow.createCell(1).setCellValue("数量");

            Map<String, Object> total = (Map<String, Object>) stats.get("totalCount");
            addStatRow(overviewSheet, rowNum++, "总试题数", total.get("total"));
            addStatRow(overviewSheet, rowNum++, "单选题", total.get("singleCount"));
            addStatRow(overviewSheet, rowNum++, "多选题", total.get("multiCount"));
            addStatRow(overviewSheet, rowNum++, "填空题", total.get("blankCount"));
            addStatRow(overviewSheet, rowNum++, "简答题", total.get("essayCount"));
            addStatRow(overviewSheet, rowNum++, "简单题", total.get("easyCount"));
            addStatRow(overviewSheet, rowNum++, "中等题", total.get("mediumCount"));
            addStatRow(overviewSheet, rowNum++, "困难题", total.get("hardCount"));

            // Sheet2: 题型统计
            Sheet typeSheet = workbook.createSheet("题型统计");
            rowNum = 0;
            headerRow = typeSheet.createRow(rowNum++);
            headerRow.createCell(0).setCellValue("题型");
            headerRow.createCell(1).setCellValue("数量");
            headerRow.createCell(2).setCellValue("占比(%)");

            List<Map<String, Object>> typeStats = (List<Map<String, Object>>) stats.get("typeStats");
            if (typeStats != null) {
                int totalCount = ((Number) total.get("total")).intValue();
                for (Map<String, Object> ts : typeStats) {
                    Row row = typeSheet.createRow(rowNum++);
                    row.createCell(0).setCellValue((String) ts.get("typeName"));
                    Integer count = ((Number) ts.get("count")).intValue();
                    row.createCell(1).setCellValue(count);
                    row.createCell(2).setCellValue(totalCount > 0 ? count * 100.0 / totalCount : 0);
                }
            }

            // Sheet3: 教师排行榜
            Sheet teacherSheet = workbook.createSheet("教师排行榜");
            rowNum = 0;
            headerRow = teacherSheet.createRow(rowNum++);
            headerRow.createCell(0).setCellValue("排名");
            headerRow.createCell(1).setCellValue("教师");
            headerRow.createCell(2).setCellValue("学院");
            headerRow.createCell(3).setCellValue("出题数量");

            List<Map<String, Object>> teacherRank = (List<Map<String, Object>>) stats.get("teacherRank");
            if (teacherRank != null) {
                for (Map<String, Object> tr : teacherRank) {
                    Row row = teacherSheet.createRow(rowNum++);
                    row.createCell(0).setCellValue(((Number) tr.get("rank")).intValue());
                    row.createCell(1).setCellValue((String) tr.get("teacherName"));
                    row.createCell(2).setCellValue((String) tr.get("college"));
                    row.createCell(3).setCellValue(((Number) tr.get("questionCount")).intValue());
                }
            }

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            workbook.write(bos);
            return bos.toByteArray();
        }
    }

    private void addStatRow(Sheet sheet, int rowNum, String label, Object value) {
        Row row = sheet.createRow(rowNum);
        row.createCell(0).setCellValue(label);
        row.createCell(1).setCellValue(value != null ? value.toString() : "0");
    }

    // 在 QuestionServiceImpl.java 中添加

    private static final int[] DIFFICULTY_RATIO = {3, 5, 2};  // 简单:中等:困难 = 3:5:2

    @Override
    public PaperVO autoGeneratePaper(AutoGeneratePaperDTO dto, Integer userId) {
        PaperVO paper = new PaperVO();
        paper.setPaperTitle(dto.getPaperTitle());
        paper.setTotalScore(0);
        paper.setTotalQuestions(0);

        List<PaperQuestionVO> questionList = new ArrayList<>();
        List<PaperVO.PaperAnswerVO> answerList = new ArrayList<>();

        Set<Integer> usedQuestionIds = new HashSet<>();  // 避免重复

        int questionNo = 1;
        int totalScore = 0;
        int totalQuestions = 0;

        for (AutoGeneratePaperDTO.QuestionTypeConfig config : dto.getQuestionTypes()) {
            int questionType = config.getQuestionType();
            int targetCount = config.getQuestionCount();
            int scorePerQuestion = config.getScorePerQuestion();

            // 按难度比例计算各难度需要抽取的数量
            int easyCount = (int) Math.round(targetCount * DIFFICULTY_RATIO[0] / 10.0);
            int mediumCount = (int) Math.round(targetCount * DIFFICULTY_RATIO[1] / 10.0);
            int hardCount = targetCount - easyCount - mediumCount;

            // 抽取简单题
            List<Question> easyQuestions = randomSelectByTypeAndDifficulty(
                    questionType, 1, dto.getSubjectId(), new ArrayList<>(usedQuestionIds), easyCount);
            for (Question q : easyQuestions) {
                usedQuestionIds.add(q.getId());
                PaperQuestionVO pq = convertToPaperQuestion(q, questionNo++, scorePerQuestion);
                questionList.add(pq);
                answerList.add(convertToPaperAnswer(pq, questionNo - 1));
                totalScore += scorePerQuestion;
                totalQuestions++;
            }

            // 抽取中等题
            List<Question> mediumQuestions = randomSelectByTypeAndDifficulty(
                    questionType, 2, dto.getSubjectId(), new ArrayList<>(usedQuestionIds), mediumCount);
            for (Question q : mediumQuestions) {
                usedQuestionIds.add(q.getId());
                PaperQuestionVO pq = convertToPaperQuestion(q, questionNo++, scorePerQuestion);
                questionList.add(pq);
                answerList.add(convertToPaperAnswer(pq, questionNo - 1));
                totalScore += scorePerQuestion;
                totalQuestions++;
            }

            // 抽取困难题
            List<Question> hardQuestions = randomSelectByTypeAndDifficulty(
                    questionType, 3, dto.getSubjectId(), new ArrayList<>(usedQuestionIds), hardCount);
            for (Question q : hardQuestions) {
                usedQuestionIds.add(q.getId());
                PaperQuestionVO pq = convertToPaperQuestion(q, questionNo++, scorePerQuestion);
                questionList.add(pq);
                answerList.add(convertToPaperAnswer(pq, questionNo - 1));
                totalScore += scorePerQuestion;
                totalQuestions++;
            }

            // 如果数量不够，从其他难度补充
            if (questionList.size() < targetCount) {
                int remaining = targetCount - (easyQuestions.size() + mediumQuestions.size() + hardQuestions.size());
                List<Question> additional = randomSelectByTypeAndDifficulty(
                        questionType, null, dto.getSubjectId(), new ArrayList<>(usedQuestionIds), remaining);
                for (Question q : additional) {
                    usedQuestionIds.add(q.getId());
                    PaperQuestionVO pq = convertToPaperQuestion(q, questionNo++, scorePerQuestion);
                    questionList.add(pq);
                    answerList.add(convertToPaperAnswer(pq, questionNo - 1));
                    totalScore += scorePerQuestion;
                    totalQuestions++;
                }
            }
        }

        paper.setQuestions(questionList);
        paper.setAnswers(answerList);
        paper.setTotalScore(totalScore);
        paper.setTotalQuestions(totalQuestions);

        // 获取学科名称
        if (dto.getSubjectId() != null) {
            Subject subject = subjectMapper.findById(dto.getSubjectId());
            paper.setSubjectName(subject != null ? subject.getName() : "");
        }

        return paper;
    }

    private List<Question> randomSelectByTypeAndDifficulty(Integer questionType, Integer difficulty,
                                                           Integer subjectId, List<Integer> excludeIds, int limit) {
        if (limit <= 0) return new ArrayList<>();
        return questionMapper.randomSelectByTypeAndDifficulty(questionType, difficulty, subjectId, excludeIds, limit);
    }

    private PaperQuestionVO convertToPaperQuestion(Question q, int questionNo, int score) {
        PaperQuestionVO pq = new PaperQuestionVO();
        pq.setId(q.getId());
        pq.setQuestionType(q.getQuestionType());
        pq.setTypeName(getTypeName(q.getQuestionType()));
        pq.setContent(q.getContent());
        pq.setDifficulty(q.getDifficulty());
        pq.setDifficultyName(getDifficultyName(q.getDifficulty()));
        pq.setScore(score);

        // 获取选项
        if (q.getQuestionType() == 1 || q.getQuestionType() == 2) {
            List<QuestionChoice> choices = choiceMapper.findByQuestionId(q.getId());
            List<PaperQuestionVO.ChoiceVO> choiceVOs = new ArrayList<>();
            for (QuestionChoice c : choices) {
                PaperQuestionVO.ChoiceVO cv = new PaperQuestionVO.ChoiceVO();
                cv.setOptionLabel(c.getOptionLabel());
                cv.setOptionContent(c.getOptionContent());
                cv.setIsCorrect(c.getIsCorrect() != null && c.getIsCorrect() == 1);
                choiceVOs.add(cv);
            }
            pq.setChoices(choiceVOs);
        }

        // 获取答案
        if (q.getQuestionType() == 3 || q.getQuestionType() == 4) {
            List<QuestionAnswer> answers = answerMapper.findByQuestionId(q.getId());
            List<PaperQuestionVO.AnswerVO> answerVOs = new ArrayList<>();
            for (QuestionAnswer a : answers) {
                PaperQuestionVO.AnswerVO av = new PaperQuestionVO.AnswerVO();
                av.setAnswerText(a.getAnswerText());
                av.setKeyPointScore(a.getKeyPointScore());
                answerVOs.add(av);
            }
            pq.setAnswers(answerVOs);
        }

        return pq;
    }

    private PaperVO.PaperAnswerVO convertToPaperAnswer(PaperQuestionVO pq, int questionNo) {
        PaperVO.PaperAnswerVO answer = new PaperVO.PaperAnswerVO();
        answer.setQuestionNo(questionNo);
        answer.setQuestionType(pq.getQuestionType());
        answer.setContent(pq.getContent());
        answer.setScore(pq.getScore());

        StringBuilder answerText = new StringBuilder();
        if (pq.getChoices() != null && !pq.getChoices().isEmpty()) {
            for (PaperQuestionVO.ChoiceVO choice : pq.getChoices()) {
                if (choice.getIsCorrect()) {
                    answerText.append(choice.getOptionLabel()).append(" ");
                }
            }
        } else if (pq.getAnswers() != null && !pq.getAnswers().isEmpty()) {
            for (PaperQuestionVO.AnswerVO ans : pq.getAnswers()) {
                answerText.append(ans.getAnswerText()).append("；");
            }
        }
        answer.setAnswer(answerText.toString());

        return answer;
    }

    @Override
    public byte[] exportPaperToWord(PaperVO paper) throws Exception {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>");
        html.append("<html><head><meta charset='UTF-8'>");
        html.append("<title>").append(paper.getPaperTitle()).append("</title>");
        html.append("<style>");
        html.append("body{font-family:SimSun;margin:20px;}");
        html.append(".paper-title{text-align:center;font-size:22pt;font-weight:bold;margin-bottom:10px;}");
        html.append(".paper-info{text-align:center;font-size:12pt;margin-bottom:20px;color:#666;}");
        html.append(".question{margin-bottom:25px;page-break-after:avoid;}");
        html.append(".question-header{font-weight:bold;margin-bottom:8px;}");
        html.append(".question-content{margin-left:20px;margin-bottom:10px;}");
        html.append(".choice{margin-left:35px;}");
        html.append(".answer-section{margin-top:30px;page-break-before:avoid;}");
        html.append(".answer-title{font-size:16pt;font-weight:bold;margin-bottom:15px;}");
        html.append(".answer-item{margin-bottom:10px;margin-left:20px;}");
        html.append("</style>");
        html.append("</head><body>");

        // 试卷标题
        html.append("<div class='paper-title'>").append(paper.getPaperTitle()).append("</div>");
        html.append("<div class='paper-info'>");
        html.append("学科：").append(paper.getSubjectName()).append("&nbsp;&nbsp;&nbsp;");
        html.append("总分：").append(paper.getTotalScore()).append("分&nbsp;&nbsp;&nbsp;");
        html.append("题数：").append(paper.getTotalQuestions()).append("题");
        html.append("</div>");

        // 试题部分
        html.append("<div class='questions-section'>");
        int questionNo = 1;
        for (PaperQuestionVO q : paper.getQuestions()) {
            html.append("<div class='question'>");
            html.append("<div class='question-header'>");
            html.append(questionNo).append(". ").append(q.getTypeName());
            html.append("（").append(q.getScore()).append("分）");
            html.append("</div>");
            html.append("<div class='question-content'>");
            html.append(escapeHtml(q.getContent()));
            html.append("</div>");

            if (q.getChoices() != null && !q.getChoices().isEmpty()) {
                for (PaperQuestionVO.ChoiceVO choice : q.getChoices()) {
                    html.append("<div class='choice'>");
                    html.append(choice.getOptionLabel()).append(". ");
                    html.append(escapeHtml(choice.getOptionContent()));
                    html.append("</div>");
                }
            }

            html.append("</div>");
            questionNo++;
        }
        html.append("</div>");

        // 参考答案
        html.append("<div class='answer-section'>");
        html.append("<div class='answer-title'>参考答案</div>");
        for (PaperVO.PaperAnswerVO ans : paper.getAnswers()) {
            html.append("<div class='answer-item'>");
            html.append(ans.getQuestionNo()).append(". ");
            html.append(getTypeName(ans.getQuestionType())).append("：");
            html.append(ans.getAnswer());
            html.append("（").append(ans.getScore()).append("分）");
            html.append("</div>");
        }
        html.append("</div>");

        html.append("</body></html>");

        return html.toString().getBytes("UTF-8");
    }
}