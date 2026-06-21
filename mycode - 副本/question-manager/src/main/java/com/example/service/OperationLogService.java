// src/main/java/com/example/service/OperationLogService.java
package com.example.service;

import com.example.entity.OperationLog;
import java.time.LocalDateTime;
import java.util.Map;

public interface OperationLogService {

    /**
     * 记录操作日志
     */
    void log(Integer userId, String operationType, String operationDesc, String ipAddress);

    /**
     * 分页查询日志
     */
    Map<String, Object> getLogs(Integer userId, String operationType,
                                LocalDateTime startTime, LocalDateTime endTime,
                                Integer pageNum, Integer pageSize);
}