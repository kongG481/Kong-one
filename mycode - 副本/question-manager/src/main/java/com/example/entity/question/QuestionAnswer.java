package com.example.entity.question;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 答案表实体类（填空题和简答题用）
 */
@Data
public class QuestionAnswer {
    private Integer id;                 // 答案ID
    private Integer questionId;          // 试题ID
    private Integer blankIndex;          // 填空序号(填空题用)
    private String answerText;           // 答案内容
    private Integer isKeyPoint;          // 是否为评分要点(简答题用) 0-否，1-是
    private Integer keyPointScore;       // 要点分值
    private Integer matchType;           // 匹配方式：1-完全匹配，2-模糊匹配
    private Integer caseSensitive;        // 是否区分大小写：0-不区分，1-区分
    private LocalDateTime createTime;     // 创建时间

    // 辅助方法，方便判断
    public boolean isKeyPoint() {
        return isKeyPoint != null && isKeyPoint == 1;
    }
}