package com.example.mapper.exam;

import com.example.entity.question.QuestionAnswer;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface QuestionAnswerMapper {

    @Insert("<script>" +
            "INSERT INTO question_answers (question_id, blank_index, answer_text, is_key_point, " +
            "key_point_score, match_type, case_sensitive, create_time) VALUES " +
            "<foreach collection='list' item='item' separator=','>" +
            "(#{item.questionId}, #{item.blankIndex}, #{item.answerText}, #{item.isKeyPoint}, " +
            "#{item.keyPointScore}, #{item.matchType}, #{item.caseSensitive}, NOW())" +
            "</foreach>" +
            "</script>")
    int batchInsert(List<QuestionAnswer> list);

    @Delete("DELETE FROM question_answers WHERE question_id = #{questionId}")
    int deleteByQuestionId(@Param("questionId") Integer questionId);

    @Select("SELECT * FROM question_answers WHERE question_id = #{questionId} ORDER BY blank_index")
    List<QuestionAnswer> findByQuestionId(@Param("questionId") Integer questionId);
}