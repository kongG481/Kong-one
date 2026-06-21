package com.example.entity.question;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 查询历史记录实体类
 */
@Data
public class QueryHistory {
    private Integer id;                 // 历史ID
    private Integer userId;             // 用户ID
    private String queryCondition;      // 查询条件(JSON格式)
    private String queryName;           // 保存的查询名称
    private Integer resultCount;        // 查询结果数量
    private LocalDateTime createTime;   // 创建时间

    // 非数据库字段
    private String userName;            // 用户姓名
}