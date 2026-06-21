package com.example.vo.exam;

import lombok.Data;
import java.util.List;

/**
 * 试卷VO
 */
@Data
public class PaperVO {
    private String paperTitle;             // 试卷标题
    private String subjectName;            // 学科名称
    private Integer totalScore;            // 总分
    private Integer totalQuestions;        // 总题数
    private List<PaperQuestionVO> questions;  // 题目列表
    private List<PaperAnswerVO> answers;   // 参考答案

    @Data
    public static class PaperAnswerVO {
        private Integer questionNo;         // 题号
        private Integer questionType;       // 题型
        private String content;             // 题目内容
        private String answer;              // 答案
        private Integer score;              // 分数
    }
}