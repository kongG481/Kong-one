package com.example.dto.exam;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SetCorrectOptionDTO {
    @NotNull(message = "试题ID不能为空")
    private Integer questionId;

    @NotNull(message = "正确选项标签不能为空")
    private String correctLabel;  // 单选题：如"A"

    // 多选题：多个正确选项标签
    private java.util.List<String> correctLabels;
}