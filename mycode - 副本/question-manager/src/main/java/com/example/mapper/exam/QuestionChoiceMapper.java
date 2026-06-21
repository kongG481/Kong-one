package com.example.mapper.exam;

import com.example.entity.question.QuestionChoice;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface QuestionChoiceMapper {

    /**
     * 批量插入选项
     */
    @Insert("<script>" +
            "INSERT INTO question_choices (question_id, option_label, option_content, is_correct, sort_order) VALUES " +
            "<foreach collection='list' item='choice' separator=','>" +
            "(#{choice.questionId}, #{choice.optionLabel}, #{choice.optionContent}, #{choice.isCorrect}, #{choice.sortOrder})" +
            "</foreach>" +
            "</script>")
    int batchInsert(@Param("list") List<QuestionChoice> choices);

    /**
     * 插入单个选项
     */
    @Options(useGeneratedKeys = true, keyProperty = "id")
    @Insert("INSERT INTO question_choices (question_id, option_label, option_content, is_correct, sort_order) " +
            "VALUES (#{questionId}, #{optionLabel}, #{optionContent}, #{isCorrect}, #{sortOrder})")
    int insert(QuestionChoice choice);

    /**
     * 根据试题ID查询选项列表
     */
    @Select("SELECT * FROM question_choices WHERE question_id = #{questionId} ORDER BY sort_order")
    List<QuestionChoice> findByQuestionId(@Param("questionId") Integer questionId);

    /**
     * 根据选项ID查询
     */
    @Select("SELECT * FROM question_choices WHERE id = #{id}")
    QuestionChoice findById(@Param("id") Integer id);

    /**
     * 根据试题ID删除所有选项
     */
    @Delete("DELETE FROM question_choices WHERE question_id = #{questionId}")
    int deleteByQuestionId(@Param("questionId") Integer questionId);

    /**
     * 删除单个选项
     */
    @Delete("DELETE FROM question_choices WHERE id = #{id}")
    int deleteById(@Param("id") Integer id);

    /**
     * 更新选项
     */
    @Update("UPDATE question_choices SET " +
            "option_content = #{optionContent}, " +
            "is_correct = #{isCorrect}, " +
            "sort_order = #{sortOrder} " +
            "WHERE id = #{id}")
    int updateChoice(QuestionChoice choice);

    /**
     * 清除试题的所有正确标记（用于单选题）
     */
    @Update("UPDATE question_choices SET is_correct = 0 WHERE question_id = #{questionId}")
    int clearAllCorrect(@Param("questionId") Integer questionId);

    /**
     * 更新指定选项的正确标记
     */
    @Update("UPDATE question_choices SET is_correct = #{isCorrect} " +
            "WHERE question_id = #{questionId} AND option_label = #{optionLabel}")
    int updateCorrectFlag(@Param("questionId") Integer questionId,
                          @Param("optionLabel") String optionLabel,
                          @Param("isCorrect") Integer isCorrect);

    /**
     * 获取试题的正确答案标签列表
     */
    @Select("SELECT option_label FROM question_choices " +
            "WHERE question_id = #{questionId} AND is_correct = 1 " +
            "ORDER BY sort_order")
    List<String> findCorrectLabelsByQuestionId(@Param("questionId") Integer questionId);

    /**
     * 统计试题的选项数量
     */
    @Select("SELECT COUNT(*) FROM question_choices WHERE question_id = #{questionId}")
    int countByQuestionId(@Param("questionId") Integer questionId);

    /**
     * 更新选项的排序
     */
    @Update("UPDATE question_choices SET sort_order = #{sortOrder} WHERE id = #{id}")
    int updateSortOrder(@Param("id") Integer id, @Param("sortOrder") Integer sortOrder);
}