package com.example.dto.exam;
import lombok.Data;
import java.util.List;

/**
 * 批量导入结果DTO
 */
@Data
public class BatchImportResult {
    private Integer total;                 // 总处理数
    private Integer success;                // 成功数
    private Integer fail;                   // 失败数
    private List<String> errorMessages;     // 错误信息列表
}