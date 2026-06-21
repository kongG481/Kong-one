package com.example.mapper;

import com.example.entity.question.Duplicate;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface DuplicateMapper {

    @Insert("INSERT INTO duplicates (question_id_a, question_id_b, similarity, status, create_time) " +
            "VALUES (#{questionIdA}, #{questionIdB}, #{similarity}, #{status}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Duplicate duplicate);

    @Select("SELECT COUNT(*) FROM duplicates WHERE " +
            "(question_id_a = #{a} AND question_id_b = #{b}) OR " +
            "(question_id_a = #{b} AND question_id_b = #{a})")
    int existsDuplicate(@Param("a") Integer a, @Param("b") Integer b);

    @Select("SELECT d.*, q.content AS duplicateContent " +
            "FROM duplicates d LEFT JOIN questions q ON d.question_id_b = q.id " +
            "WHERE d.question_id_a = #{questionId} AND d.status = 0 " +
            "UNION " +
            "SELECT d.*, q.content AS duplicateContent " +
            "FROM duplicates d LEFT JOIN questions q ON d.question_id_a = q.id " +
            "WHERE d.question_id_b = #{questionId} AND d.status = 0")
    List<Duplicate> findByQuestionId(@Param("questionId") Integer questionId);

    @Update("UPDATE duplicates SET status = #{status} WHERE id = #{id}")
    int updateStatus(@Param("id") Integer id, @Param("status") Integer status);

    @Select("SELECT * FROM duplicates WHERE id = #{id}")
    Duplicate selectById(@Param("id") Integer id);

    @Select("SELECT " +
            "COUNT(*) AS total, " +
            "SUM(CASE WHEN status = 0 THEN 1 ELSE 0 END) AS pendingCount, " +
            "SUM(CASE WHEN status = 1 THEN 1 ELSE 0 END) AS ignoredCount, " +
            "SUM(CASE WHEN status = 2 THEN 1 ELSE 0 END) AS mergedCount " +
            "FROM duplicates")
    Map<String, Object> getDuplicateStatistics();

    @Select("SELECT d.*, q1.content AS contentA, q2.content AS contentB " +
            "FROM duplicates d " +
            "LEFT JOIN questions q1 ON d.question_id_a = q1.id " +
            "LEFT JOIN questions q2 ON d.question_id_b = q2.id " +
            "WHERE d.status = 0 ORDER BY d.create_time DESC")
    List<Duplicate> getPendingDuplicates();
}