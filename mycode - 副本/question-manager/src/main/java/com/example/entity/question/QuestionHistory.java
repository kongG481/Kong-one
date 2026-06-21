package com.example.entity.question;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 试题历史实体类
 */
@Data
public class QuestionHistory {
    private Integer id;                 // 历史ID
    private Integer questionId;          // 试题ID
    private String content;              // 历史题目内容
    private String analysis;             // 历史解析
    private Integer difficulty;           // 历史难度
    private Integer operatorId;           // 操作人ID
    private String operationType;         // 操作类型：UPDATE/DELETE
    private String changeNote;            // 修改说明
    private LocalDateTime createTime;     // 操作时间

    // 非数据库字段
    private String operatorName;          // 操作人姓名
}