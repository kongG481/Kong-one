package com.example.dto.exam;

import lombok.Data;
import java.util.List;

/**
 * 试题导出参数DTO
 */
@Data
public class QuestionExportDTO {
    private List<Integer> questionIds;   // 导出的试题ID列表
    private String format;               // 导出格式：word/excel
    private String sortField;            // 排序字段
    private String sortOrder;            // 排序方向
    private String fileName;             // 文件名（可选）
}