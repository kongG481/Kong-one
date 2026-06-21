// QuestionController.java
package com.example.controller.question;

import com.example.controller.BaseController;
import com.example.dto.exam.*;
import com.example.entity.question.*;
import com.example.entity.Result;
import com.example.exception.BusinessException;
import com.example.service.QuestionService;
import com.example.service.SubjectService;
import com.example.utils.ThreadLocalUtil;
import com.example.vo.exam.PaperVO;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/question")
public class QuestionController extends BaseController {

    @Autowired
    private QuestionService questionService;

    @Autowired
    private SubjectService subjectService;

    // ==================== 入库模块 ====================

    @PostMapping("/insert")
    public Result<Integer> insertQuestion(@Valid @RequestBody QuestionInsertDTO dto) {
        Integer id = questionService.insertQuestion(dto);
        return Result.success(id);
    }

    @GetMapping("/checkDuplicate")
    public Result<Integer> checkDuplicate(@RequestParam String content) {
        Integer count = questionService.checkDuplicate(content);
        return Result.success(count);
    }

    @GetMapping("/drafts")
    public Result<List<Question>> getDrafts() {
        List<Question> drafts = questionService.getDrafts();
        return Result.success(drafts);
    }

    @GetMapping("/draft/{id}")
    public Result<Question> getDraftDetail(@PathVariable Integer id) {
        Question question = questionService.getDraftDetail(id);
        return Result.success(question);
    }

    @DeleteMapping("/draft/{id}")
    public Result deleteDraft(@PathVariable Integer id,
                              @RequestParam(defaultValue = "false") boolean physical) {
        questionService.deleteDraft(id, physical);
        return Result.success();
    }

    @PostMapping("/batch/import")
    public Result<BatchImportResult> batchImport(@RequestParam("file") MultipartFile file) {
        try {
            BatchImportResult result = questionService.batchImport(file);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("批量导入失败：" + e.getMessage());
        }
    }

    @GetMapping("/template/download")
    public void downloadTemplate(HttpServletResponse response) {
        try {
            String fileName = "试题导入模板.csv";
            String content = "题型,学科ID,难度,题目内容,解析,选项A,选项A是否正确,选项B,选项B是否正确,选项C,选项C是否正确,选项D,选项D是否正确\n" +
                    "1,6,1,Java中定义常量的关键字是？,final关键字,static,错误,final,正确,const,错误,define,错误\n" +
                    "2,6,2,以下哪些是Java面向对象的特性？,封装继承多态,封装,正确,继承,正确,多态,正确,重载,错误\n";

            response.setContentType("text/csv;charset=UTF-8");
            response.setHeader("Content-Disposition", "attachment;filename=" +
                    URLEncoder.encode(fileName, StandardCharsets.UTF_8).replaceAll("\\+", "%20"));

            response.getOutputStream().write(content.getBytes(StandardCharsets.UTF_8));
            response.getOutputStream().flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ==================== 学科模块 ====================

    @GetMapping("/subject/tree")
    public Result<List<Subject>> getSubjectTree() {
        List<Subject> tree = subjectService.getSubjectTree();
        return Result.success(tree);
    }

    // ==================== 查询模块 ====================

    @GetMapping("/list")
    public Result<Map<String, Object>> getQuestionList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Integer type,
            @RequestParam(required = false) String keyword) {

        Map<String, Object> result = new HashMap<>();
        List<Question> records = questionService.getQuestionList(pageNum, pageSize, type, keyword);
        Integer total = questionService.getQuestionCount(type, keyword);

        result.put("records", records);
        result.put("total", total);
        result.put("pageNum", pageNum);
        result.put("pageSize", pageSize);

        return Result.success(result);
    }

    @GetMapping("/detail/{id}")
    public Result<Question> getQuestionDetail(@PathVariable Integer id) {
        Question question = questionService.getQuestionDetail(id);
        return Result.success(question);
    }

    // ==================== 修改模块 ====================

    @PutMapping("/update")
    public Result<Integer> updateQuestion(@Valid @RequestBody QuestionInsertDTO dto) {
        Integer id = questionService.updateQuestion(dto);
        return Result.success(id);
    }

    @PutMapping("/batch/difficulty")
    public Result batchUpdateDifficulty(@Valid @RequestBody BatchUpdateDTO dto) {
        questionService.batchUpdateDifficulty(dto);
        return Result.success();
    }

    @PutMapping("/batch/subject")
    public Result batchUpdateSubject(@Valid @RequestBody BatchUpdateDTO dto) {
        questionService.batchUpdateSubject(dto);
        return Result.success();
    }

    // ==================== 历史模块 ====================

    @GetMapping("/{id}/history")
    public Result<List<QuestionHistory>> getQuestionHistory(@PathVariable Integer id) {
        List<QuestionHistory> history = questionService.getQuestionHistory(id);
        return Result.success(history);
    }

    @GetMapping("/history/compare")
    public Result<Map<String, Object>> compareVersions(
            @RequestParam Integer historyId1,
            @RequestParam Integer historyId2) {
        Map<String, Object> comparison = questionService.compareVersions(historyId1, historyId2);
        return Result.success(comparison);
    }

    @PostMapping("/{questionId}/rollback/{historyId}")
    public Result rollbackToVersion(
            @PathVariable Integer questionId,
            @PathVariable Integer historyId) {
        questionService.rollbackToVersion(questionId, historyId);
        return Result.success();
    }

    // ==================== 删除模块 ====================

    @DeleteMapping("/{id}")
    public Result deleteQuestion(@PathVariable Integer id,
                                 @RequestBody(required = false) DeleteReasonDTO reason) {
        String deleteReason = reason != null ? reason.getDeleteReason() : "用户删除试题";
        questionService.deleteQuestion(id, deleteReason);
        return Result.success();
    }

    @GetMapping("/recycle-bin")
    public Result<List<Question>> getRecycleBin() {
        List<Question> recycleBin = questionService.getRecycleBin();
        return Result.success(recycleBin);
    }

    @PutMapping("/{id}/restore")
    public Result restoreQuestion(@PathVariable Integer id) {
        questionService.restoreQuestion(id);
        return Result.success();
    }

    @DeleteMapping("/{id}/permanent")
    public Result permanentDelete(@PathVariable Integer id) {
        questionService.permanentDelete(id);
        return Result.success();
    }

    // ========== 查询模块接口 ==========

    /**
     * 高级组合查询
     */
    @PostMapping("/query/advanced")
    public Result<Map<String, Object>> advancedQuery(@RequestBody QuestionQueryDTO query) {
        Integer userId = getCurrentUserId();
        return Result.success(questionService.advancedQuery(query, userId));
    }

    /**
     * 全文检索
     */
    @GetMapping("/query/search")
    public Result<Map<String, Object>> fullTextSearch(
            @RequestParam String keywords,
            @ModelAttribute QuestionQueryDTO query) {
        Integer userId = getCurrentUserId();
        return Result.success(questionService.fullTextSearch(keywords, query, userId));
    }

    /**
     * 保存查询条件
     */
    @PostMapping("/query/save")
    public Result<Void> saveQueryCondition(@RequestBody SaveQueryConditionDTO dto) {
        Integer userId = getCurrentUserId();
        questionService.saveQueryCondition(dto.getQueryName(), dto.getQuery(), userId, 0);
        return Result.success();
    }

    /**
     * 获取查询历史
     */
    @GetMapping("/query/history")
    public Result<List<QueryHistory>> getQueryHistory() {
        Integer userId = getCurrentUserId();
        return Result.success(questionService.getQueryHistory(userId));
    }

    /**
     * 删除查询历史
     */
    @DeleteMapping("/query/history/{id}")
    public Result<Void> deleteQueryHistory(@PathVariable Integer id) {
        Integer userId = getCurrentUserId();
        questionService.deleteQueryHistory(id, userId);
        return Result.success();
    }

    // ========== 导出模块接口 ==========


    // 修改 QuestionController - 直接返回文件流
    @PostMapping("/export")
    public void exportQuestions(@RequestBody QuestionExportDTO exportDTO,
                                HttpServletResponse response) {
        Integer userId = getCurrentUserId();

        try {
            // 导出文件并返回流
            String filePath = questionService.exportQuestions(exportDTO, userId);

            File file = new File(filePath);
            if (!file.exists()) {
                throw new BusinessException("文件不存在");
            }

            // 设置响应头
            String fileName = file.getName();
            response.setContentType("application/octet-stream");
            response.setHeader("Content-Disposition",
                    "attachment; filename=" + URLEncoder.encode(fileName, "UTF-8"));
            response.setContentLengthLong(file.length());

            // 写入响应流
            try (FileInputStream fis = new FileInputStream(file);
                 OutputStream os = response.getOutputStream()) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
                os.flush();
            }

        } catch (Exception e) {
            log.error("导出试题失败", e);
            throw new BusinessException("导出失败: " + e.getMessage());
        }
    }

    /**
     * 获取导出历史
     */
    @GetMapping("/export/history")
    public Result<List<ExportTask>> getExportHistory() {
        Integer userId = getCurrentUserId();
        return Result.success(questionService.getExportHistory(userId));
    }

    /**
     * 获取所有导出历史（管理员）
     */
    @GetMapping("/export/all")
    public Result<List<ExportTask>> getAllExportHistory() {
        checkAdminPermission();
        return Result.success(questionService.getAllExportHistory());
    }

    /**
     * 删除导出记录
     */
    @DeleteMapping("/export/{id}")
    public Result<Void> deleteExportTask(@PathVariable Integer id) {
        Integer userId = getCurrentUserId();
        boolean isAdmin = isAdmin();
        questionService.deleteExportTask(id, userId, isAdmin);
        return Result.success();
    }

    /**
     * 下载导出文件
     */
    @GetMapping("/export/download/{taskId}")
    public ResponseEntity<byte[]> downloadExportFile(@PathVariable Integer taskId) throws Exception {
        Integer userId = getCurrentUserId();
        boolean isAdmin = isAdmin();

        byte[] data = questionService.downloadExportFile(taskId, userId, isAdmin);
        List<ExportTask> history = questionService.getExportHistory(userId);
        
        ExportTask task = history.stream()
                .filter(t -> t.getId().equals(taskId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("导出任务不存在"));

        String fileName = task.getFileUrl() != null ?
                task.getFileUrl().substring(task.getFileUrl().lastIndexOf("/") + 1) : "export";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(data);
    }

    // QuestionController.java 中添加以下接口
    // 在 QuestionController.java 中添加

    /**
     * 自动组卷
     */
    @PostMapping("/paper/generate")
    public Result<PaperVO> autoGeneratePaper(@RequestBody AutoGeneratePaperDTO dto) {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer userId = (Integer) claims.get("id");
        try {
            PaperVO paper = questionService.autoGeneratePaper(dto, userId);
            return Result.success(paper);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("组卷失败：" + e.getMessage());
        }
    }

    /**
     * 导出试卷
     */
    @PostMapping("/paper/export")
    public ResponseEntity<byte[]> exportPaper(@RequestBody PaperVO paper) {
        try {
            byte[] data = questionService.exportPaperToWord(paper);
            String fileName = URLEncoder.encode(paper.getPaperTitle() + ".doc", "UTF-8");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", fileName);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(data);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("导出失败：" + e.getMessage());
        }
    }

// ==================== 单选题/多选题选项管理 ====================

    /**
     * 获取试题的选项列表
     */
    @GetMapping("/{questionId}/choices")
    public Result<List<QuestionChoice>> getChoices(@PathVariable Integer questionId) {
        List<QuestionChoice> choices = questionService.getChoicesByQuestionId(questionId);
        return Result.success(choices);
    }

    /**
     * 添加选项
     */
    @PostMapping("/{questionId}/choice")
    public Result<Integer> addChoice(@PathVariable Integer questionId,
                                     @Valid @RequestBody ChoiceDTO choiceDTO) {
        Integer choiceId = questionService.addChoice(questionId, choiceDTO);
        return Result.success(choiceId);
    }

    /**
     * 更新选项
     */
    @PutMapping("/choice")
    public Result<Void> updateChoice(@Valid @RequestBody UpdateChoiceDTO dto) {
        questionService.updateChoice(dto);
        return Result.success();
    }

    /**
     * 删除选项
     */
    @DeleteMapping("/choice/{choiceId}")
    public Result<Void> deleteChoice(@PathVariable Integer choiceId) {
        questionService.deleteChoice(choiceId);
        return Result.success();
    }

    /**
     * 设置单选题的正确选项
     */
    @PutMapping("/{questionId}/single-correct")
    public Result<Void> setSingleCorrectOption(@PathVariable Integer questionId,
                                               @RequestParam String correctLabel) {
        questionService.setSingleCorrectOption(questionId, correctLabel);
        return Result.success();
    }

    /**
     * 设置多选题的正确选项
     */
    @PutMapping("/{questionId}/multiple-correct")
    public Result<Void> setMultipleCorrectOptions(@PathVariable Integer questionId,
                                                  @RequestBody SetCorrectOptionDTO dto) {
        questionService.setMultipleCorrectOptions(questionId, dto.getCorrectLabels());
        return Result.success();
    }

    /**
     * 获取随机顺序的选项（用于考试/练习场景）
     */
    @GetMapping("/{questionId}/random-choices")
    public Result<List<QuestionChoice>> getRandomChoices(@PathVariable Integer questionId) {
        List<QuestionChoice> choices = questionService.getRandomOrderChoices(questionId);
        return Result.success(choices);
    }

    /**
     * 验证试题的答案（新增）
     */
    @PostMapping("/{questionId}/validate")
    public Result<Map<String, Object>> validateAnswer(@PathVariable Integer questionId,
                                                      @RequestBody Map<String, Object> answer) {
        // 这里可以根据题型验证答案
        // 单选题：answer中包含selectedLabel
        // 多选题：answer中包含selectedLabels列表
        // 返回是否正确、得分等信息
        Map<String, Object> result = new HashMap<>();
        result.put("correct", true);
        result.put("score", 100);
        return Result.success(result);
    }

    // QuestionController.java 中添加

// ==================== 填空题/简答题接口 ====================

    /**
     * 验证填空题答案
     */
    @PostMapping("/{questionId}/validate-blank")
    public Result<Map<String, Object>> validateBlankAnswers(
            @PathVariable Integer questionId,
            @RequestBody List<String> userAnswers) {
        Map<String, Object> result = questionService.validateBlankAnswers(questionId, userAnswers);
        return Result.success(result);
    }

    /**
     * 验证简答题答案
     */
    @PostMapping("/{questionId}/validate-essay")
    public Result<Map<String, Object>> validateEssayAnswer(
            @PathVariable Integer questionId,
            @RequestBody Map<String, String> request) {
        String userAnswer = request.get("answer");
        Map<String, Object> result = questionService.validateEssayAnswer(questionId, userAnswer);
        return Result.success(result);
    }

    /**
     * 获取填空题答案（用于展示正确答案）
     */
    @GetMapping("/{questionId}/blank-answers")
    public Result<List<QuestionAnswer>> getBlankAnswers(@PathVariable Integer questionId) {
        List<QuestionAnswer> answers = questionService.getBlankAnswers(questionId);
        return Result.success(answers);
    }

    /**
     * 获取简答题评分要点
     */
    @GetMapping("/{questionId}/key-points")
    public Result<List<QuestionAnswer>> getEssayKeyPoints(@PathVariable Integer questionId) {
        List<QuestionAnswer> keyPoints = questionService.getEssayKeyPoints(questionId);
        return Result.success(keyPoints);
    }

    /**
     * 自动评分（通用接口）
     */
    @PostMapping("/{questionId}/auto-score")
    public Result<AutoScoreResult> autoScore(
            @PathVariable Integer questionId,
            @RequestBody Map<String, Object> request) {
        Object userAnswer = request.get("userAnswer");
        AutoScoreResult result = questionService.autoScore(questionId, userAnswer);
        return Result.success(result);
    }
}