// QuestionHistoryMapper.java
package com.example.mapper.exam;

import com.example.entity.question.QuestionHistory;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface QuestionHistoryMapper {

    @Options(useGeneratedKeys = true, keyProperty = "id")
    @Insert("INSERT INTO question_history (question_id, content, analysis, difficulty, operator_id, " +
            "operation_type, change_note, create_time) " +
            "VALUES (#{questionId}, #{content}, #{analysis}, #{difficulty}, #{operatorId}, " +
            "#{operationType}, #{changeNote}, NOW())")
    int insert(QuestionHistory history);

    @Select("SELECT h.*, u.real_name as operatorName FROM question_history h " +
            "LEFT JOIN users u ON h.operator_id = u.id " +
            "WHERE h.question_id = #{questionId} " +
            "ORDER BY h.create_time DESC")
    @Results({
            @Result(column = "id", property = "id"),
            @Result(column = "question_id", property = "questionId"),
            @Result(column = "operator_id", property = "operatorId"),
            @Result(column = "operation_type", property = "operationType"),
            @Result(column = "change_note", property = "changeNote"),
            @Result(column = "create_time", property = "createTime"),
            @Result(column = "operatorName", property = "operatorName")
    })
    List<QuestionHistory> findByQuestionId(@Param("questionId") Integer questionId);

    @Select("SELECT * FROM question_history WHERE id = #{id}")
    QuestionHistory findById(@Param("id") Integer id);
}