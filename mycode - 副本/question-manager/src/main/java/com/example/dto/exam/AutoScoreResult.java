package com.example.dto.exam;

import lombok.Data;
import java.util.Map;

/**
 * 自动评分结果
 */
@Data
public class AutoScoreResult {
    private Integer questionId;      // 试题ID
    private Integer questionType;    // 题型
    private Integer score;           // 得分
    private Integer maxScore;        // 满分
    private String feedback;         // 评语
    private Map<String, Object> detail;  // 详细评分结果
}