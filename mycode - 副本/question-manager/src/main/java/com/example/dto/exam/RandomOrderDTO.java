package com.example.dto.exam;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RandomOrderDTO {
    @NotNull(message = "试题ID不能为空")
    private Integer questionId;

    private Boolean randomOrder = false;  // 是否随机顺序
}
