package com.example.entity.question;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 学科实体类
 */
@Data
public class Subject {
    private Integer id;
    private String name;
    private Integer parentId;
    private Integer level;
    private Integer sortOrder;
    private Integer status;
    private LocalDateTime createTime;

    // 非数据库字段，用于树形结构
    private transient List<Subject> children;
}