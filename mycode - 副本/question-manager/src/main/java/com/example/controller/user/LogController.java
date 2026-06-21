package com.example.controller.user;
// src/main/java/com/example/controller/LogController.java

import com.example.entity.Result;
import com.example.service.OperationLogService;
import com.example.utils.PermissionUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/logs")
public class LogController {

    @Autowired
    private OperationLogService operationLogService;

    /**
     * 查询操作日志（管理员权限）
     */
    @GetMapping("/list")
    public Result<Map<String, Object>> getLogs(
            @RequestParam(required = false) Integer userId,
            @RequestParam(required = false) String operationType,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {

        // 检查管理员权限
        PermissionUtil.checkAdmin();

        Map<String, Object> result = operationLogService.getLogs(
                userId, operationType, startTime, endTime, pageNum, pageSize);
        return Result.success(result);
    }

    /**
     * 获取当前用户的操作日志
     */
    @GetMapping("/my-logs")
    public Result<Map<String, Object>> getMyLogs(
            @RequestParam(required = false) String operationType,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {

        Integer currentUserId = PermissionUtil.getCurrentUserId();

        Map<String, Object> result = operationLogService.getLogs(
                currentUserId, operationType, startTime, endTime, pageNum, pageSize);
        return Result.success(result);
    }
}