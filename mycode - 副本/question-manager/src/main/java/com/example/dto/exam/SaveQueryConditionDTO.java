package com.example.dto.exam;

import lombok.Data;

/**
 * 保存查询条件DTO
 */
@Data
public class SaveQueryConditionDTO {
    private String queryName;            // 查询条件名称
    private QuestionQueryDTO query;      // 查询参数
}