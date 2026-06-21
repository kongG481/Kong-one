package com.example.dto.exam;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 选项DTO
 */
@Data
public class ChoiceDTO {
    @NotBlank(message = "选项标签不能为空")
    private String optionLabel;          // A,B,C,D

    @NotBlank(message = "选项内容不能为空")
    private String optionContent;        // 选项内容

    @NotNull(message = "是否正确不能为空")
    private Integer isCorrect;            // 0-错误，1-正确

    private Integer sortOrder;            // 排序
}

