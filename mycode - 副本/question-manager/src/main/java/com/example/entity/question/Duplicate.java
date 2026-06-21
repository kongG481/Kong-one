package com.example.entity.question;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Duplicate {
    private Integer id;
    private Integer questionIdA;
    private Integer questionIdB;
    private Integer similarity;
    private Integer status;  // 0-未处理，1-已忽略，2-已合并
    private LocalDateTime createTime;

    // 扩展字段
    private String duplicateContent;
    private String contentA;
    private String contentB;
}