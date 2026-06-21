package com.example.entity.question;

import com.example.entity.question.QuestionAnswer;
import com.example.entity.question.QuestionChoice;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 试题主表实体类
 */
@Data
public class Question {
    private Integer id;                 // 试题ID
    private Integer questionType;        // 题型：1-单选，2-多选，3-填空，4-简答
    private String content;              // 题目内容
    private String analysis;             // 试题解析
    private Integer difficulty;           // 难度：1-简单，2-中等，3-困难
    private Integer subjectId;            // 所属学科ID
    private Integer suggestedTime;        // 建议用时(分钟)
    private String source;                // 来源
    private Integer creatorId;            // 创建者ID
    private Integer editorId;             // 最后编辑者ID
    private Integer status;                // 状态：0-暂存，1-正常，2-已删除
    private LocalDateTime createTime;      // 创建时间
    private LocalDateTime updateTime;      // 更新时间
    private LocalDateTime deleteTime;      // 删除时间
    private String deleteReason;           // 删除原因

    // 非数据库字段 - 用于接收前端传递的选项和答案数据
    private List<QuestionChoice> choices;  // 选择题选项（单选题、多选题用）
    private List<QuestionAnswer> answers;  // 答案（填空题、简答题用）
    private String subjectName;             // 学科名称（用于显示）
    private String creatorName;              // 创建者姓名（用于显示）
}