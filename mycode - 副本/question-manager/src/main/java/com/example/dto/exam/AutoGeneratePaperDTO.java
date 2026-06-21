package com.example.dto.exam;

import lombok.Data;
import java.util.List;

/**
 * 自动组卷参数DTO
 */
@Data
public class AutoGeneratePaperDTO {
    private String paperTitle;           // 试卷标题
    private Integer subjectId;            // 学科ID
    private List<QuestionTypeConfig> questionTypes;  // 各题型配置

    @Data
    public static class QuestionTypeConfig {
        private Integer questionType;      // 题型：1-单选，2-多选，3-填空，4-简答
        private Integer questionCount;     // 题目数量
        private Integer scorePerQuestion;  // 每题分数
    }
}