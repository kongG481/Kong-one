package com.example.mapper.exam;

import com.example.entity.question.QueryHistory;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface QueryHistoryMapper {

    @Insert("INSERT INTO query_history (user_id, query_condition, query_name, result_count, create_time) " +
            "VALUES (#{userId}, #{queryCondition}, #{queryName}, #{resultCount}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(QueryHistory history);

    @Select("SELECT id, user_id, query_condition, query_name, result_count, create_time FROM query_history " +
            "WHERE user_id = #{userId} " +
            "ORDER BY create_time DESC LIMIT 20")
    @Results({
            @Result(column = "id", property = "id"),
            @Result(column = "user_id", property = "userId"),
            @Result(column = "query_condition", property = "queryCondition"),
            @Result(column = "query_name", property = "queryName"),
            @Result(column = "result_count", property = "resultCount"),
            @Result(column = "create_time", property = "createTime"),
            @Result(column = "userName", property = "userName")
    })
    List<QueryHistory> findByUserId(@Param("userId") Integer userId);

    @Delete("DELETE FROM query_history WHERE user_id = #{userId} AND id = #{id}")
    int deleteByUserAndId(@Param("userId") Integer userId, @Param("id") Integer id);
}