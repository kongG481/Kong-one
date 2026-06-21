package com.example.dto.exam;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateChoiceDTO {
    @NotNull(message = "选项ID不能为空")
    private Integer choiceId;

    private String optionContent;  // 选项内容
    private Integer isCorrect;      // 是否正确
    private Integer sortOrder;      // 排序
}