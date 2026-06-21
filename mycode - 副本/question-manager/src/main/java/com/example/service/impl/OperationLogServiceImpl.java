// src/main/java/com/example/service/impl/OperationLogServiceImpl.java
package com.example.service.impl;

import com.example.entity.OperationLog;
import com.example.entity.User;
import com.example.mapper.OperationLogMapper;
import com.example.mapper.UserMapper;
import com.example.service.OperationLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class OperationLogServiceImpl implements OperationLogService {

    @Autowired
    private OperationLogMapper operationLogMapper;

    @Autowired
    private UserMapper userMapper;

    @Override
    public void log(Integer userId, String operationType, String operationDesc, String ipAddress) {
        OperationLog log = new OperationLog();
        log.setUserId(userId);
        log.setOperationType(operationType);
        log.setOperationDesc(operationDesc);
        log.setIpAddress(ipAddress);

        operationLogMapper.insert(log);
    }

    @Override
    public Map<String, Object> getLogs(Integer userId, String operationType,
                                       LocalDateTime startTime, LocalDateTime endTime,
                                       Integer pageNum, Integer pageSize) {
        Integer offset = (pageNum - 1) * pageSize;

        List<OperationLog> list = operationLogMapper.findLogs(
                userId, operationType, startTime, endTime, offset, pageSize);

        Long total = operationLogMapper.countLogs(
                userId, operationType, startTime, endTime);

        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", total);
        result.put("pageNum", pageNum);
        result.put("pageSize", pageSize);

        return result;
    }
}