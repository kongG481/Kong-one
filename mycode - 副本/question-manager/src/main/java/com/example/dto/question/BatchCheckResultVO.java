package com.example.dto.question;

import lombok.Data;
import java.util.List;

/**
 * 批量检测结果VO
 */
@Data
public class BatchCheckResultVO {
    private Integer totalChecked;
    private Integer duplicateCount;
    private List<RepeatCheckResultVO> duplicateList;
    private String message;
}
