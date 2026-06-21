// src/main/java/com/example/entity/OperationLog.java
package com.example.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 操作日志实体类
 */
@Data
public class OperationLog {
    private Integer id;              // 日志ID
    private Integer userId;           // 操作用户ID
    private String username;          // 用户名（冗余字段，方便查询）
    private String realName;          // 真实姓名（冗余字段）
    private String operationType;     // 操作类型：LOGIN/INSERT/UPDATE/DELETE
    private String operationDesc;     // 操作描述
    private String ipAddress;         // IP地址
    private LocalDateTime createTime; // 操作时间
}