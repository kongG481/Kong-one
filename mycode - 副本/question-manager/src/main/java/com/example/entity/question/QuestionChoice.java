package com.example.entity.question;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 选择题选项表实体类
 */
@Data
public class QuestionChoice {
    private Integer id;                 // 选项ID
    private Integer questionId;          // 试题ID
    private String optionLabel;          // 选项标签：A,B,C,D
    private String optionContent;        // 选项内容
    private Integer isCorrect;           // 是否正确：0-错误，1-正确
    private Integer sortOrder;            // 排序
    private LocalDateTime createTime;     // 创建时间

    // 辅助方法，方便判断
    public boolean isCorrect() {
        return isCorrect != null && isCorrect == 1;
    }
}