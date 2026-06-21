package com.example.dto.exam;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * 试题入库请求DTO
 */
@Data
public class QuestionInsertDTO {
    private Integer id;  // 更新时使用

    @NotNull(message = "题型不能为空")
    private Integer questionType;

    @NotBlank(message = "题目内容不能为空")
    private String content;

    private String analysis;

    @NotNull(message = "难度等级不能为空")
    private Integer difficulty;

    @NotNull(message = "所属学科不能为空")
    private Integer subjectId;

    private Integer suggestedTime;
    private String source;
    private List<ChoiceDTO> choices;
    private List<AnswerDTO> answers;
    private Integer status;
    private String changeNote;
}