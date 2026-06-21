package com.example.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 导入记录实体类
 */
@Data
public class ImportRecord {
    private Long id;
    private Long operatorId;           // 操作人ID
    private String fileName;            // 文件名
    private String filePath;            // 文件存储路径
    private Integer totalCount;         // 总记录数
    private Integer successCount;       // 成功数
    private Integer failCount;          // 失败数
    private Integer importStatus;       // 状态：0-处理中，1-成功，2-部分成功，3-失败
    private String errorLog;            // 错误日志
    private LocalDateTime importTime;   // 导入时间
    private LocalDateTime finishTime;   // 完成时间

    // 非数据库字段
    private String operatorName;        // 操作人姓名
}