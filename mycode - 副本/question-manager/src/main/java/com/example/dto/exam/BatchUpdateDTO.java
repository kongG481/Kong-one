package com.example.dto.exam;

import lombok.Data;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * 批量更新试题DTO
 */
@Data
public class BatchUpdateDTO {
    @NotNull(message = "试题ID列表不能为空")
    private List<Integer> ids;

    private Integer difficulty;  // 要更新的难度
    private Integer subjectId;    // 要更新的学科
    private String changeNote;     // 修改说明
}