package com.example.dto.question;

import lombok.Data;
import java.util.List;

/**
 * 重复检测结果VO
 */
@Data
public class RepeatCheckResultVO {
    private Integer questionId;
    private String questionContent;
    private List<DuplicateInfo> duplicates;
    private Integer duplicateCount;
    private Boolean isDuplicate;

    @Data
    public static class DuplicateInfo {
        private Integer duplicateId;
        private Integer duplicateQuestionId;
        private String duplicateContent;
        private Integer similarity;
        private String status; // 0-未处理，1-已忽略，2-已合并
        private String statusDesc;
        private String createTime;
    }
}
