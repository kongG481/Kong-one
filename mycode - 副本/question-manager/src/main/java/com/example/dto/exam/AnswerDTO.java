package com.example.dto.exam;

import lombok.Data;

/**
 * 答案DTO
 */
@Data
public class AnswerDTO {
    private Integer blankIndex;          // 填空序号
    private String answerText;            // 答案内容
    private Integer isKeyPoint;           // 是否为评分要点
    private Integer keyPointScore;        // 要点分值
    private Integer matchType;             // 匹配方式：1-完全匹配，2-模糊匹配
    private Integer caseSensitive;         // 是否区分大小写：0-不区分，1-区分
    private Integer isCorrect;  // 填空题用
}
