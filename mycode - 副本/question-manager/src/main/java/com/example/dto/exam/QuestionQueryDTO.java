package com.example.dto.exam;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 试题查询参数DTO
 */
@Data
public class QuestionQueryDTO {
    // 基础查询
    private String keyword;              // 关键词
    private Integer questionType;        // 题型：1-单选，2-多选，3-填空，4-简答
    private Integer subjectId;           // 学科ID
    private Integer difficulty;          // 难度：1-简单，2-中等，3-困难

    // 我的试题
    private Boolean myQuestions;         // 是否只查自己创建的

    // 时间范围
    private LocalDateTime startTime;     // 开始时间
    private LocalDateTime endTime;       // 结束时间

    // 高级组合
    private List<Integer> difficultyList;    // 多难度筛选
    private List<Integer> subjectIdList;     // 多学科筛选
    private List<Integer> questionTypeList;  // 多题型筛选

    // 分页参数
    private Integer pageNum = 1;         // 页码
    private Integer pageSize = 20;       // 每页大小

    // 排序参数
    private String sortField = "create_time";   // 排序字段
    private String sortOrder = "DESC";          // 排序方向

    // 全文检索相关
    private Boolean highlight;           // 是否高亮
    private String highlightColor = "red"; // 高亮颜色
}