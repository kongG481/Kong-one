package com.example.vo.exam;

import lombok.Data;
import java.util.List;

/**
 * 试卷题目VO
 */
@Data
public class PaperQuestionVO {
    private Integer id;
    private Integer questionType;
    private String typeName;
    private String content;
    private Integer difficulty;
    private String difficultyName;
    private Integer score;                 // 本题分数
    private List<ChoiceVO> choices;        // 选项（选择题）
    private List<AnswerVO> answers;        // 答案

    @Data
    public static class ChoiceVO {
        private String optionLabel;
        private String optionContent;
        private Boolean isCorrect;
    }

    @Data
    public static class AnswerVO {
        private String answerText;
        private Integer keyPointScore;
    }
}