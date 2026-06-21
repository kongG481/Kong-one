package com.example.dto.question;

import lombok.Data;
import jakarta.validation.constraints.NotNull;

/**
 * 重复检测请求DTO
 */
@Data
public class RepeatCheckDTO {
    @NotNull(message = "试题ID不能为空")
    private Integer questionId;

    private Integer similarity; // 相似度阈值，默认100

    private String handleType; // ignore:忽略, merge:合并
}