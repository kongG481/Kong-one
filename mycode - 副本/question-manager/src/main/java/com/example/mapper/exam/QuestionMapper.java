// QuestionMapper.java
package com.example.mapper.exam;

import com.example.entity.question.Question;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface QuestionMapper {

    @Options(useGeneratedKeys = true, keyProperty = "id")
    @Insert("INSERT INTO questions (question_type, content, analysis, difficulty, subject_id, " +
            "suggested_time, source, creator_id, editor_id, status, create_time, update_time) " +
            "VALUES (#{questionType}, #{content}, #{analysis}, #{difficulty}, #{subjectId}, " +
            "#{suggestedTime}, #{source}, #{creatorId}, #{editorId}, #{status}, NOW(), NOW())")
    int insert(Question question);

    @Update("UPDATE questions SET " +
            "question_type = #{questionType}, " +
            "content = #{content}, " +
            "analysis = #{analysis}, " +
            "difficulty = #{difficulty}, " +
            "subject_id = #{subjectId}, " +
            "suggested_time = #{suggestedTime}, " +
            "source = #{source}, " +
            "editor_id = #{editorId}, " +
            "update_time = NOW() " +
            "WHERE id = #{id}")
    int update(Question question);

    @Select("SELECT * FROM questions WHERE id = #{id} AND status != 2")
    @Results(id = "questionMap", value = {
            @Result(column = "id", property = "id"),
            @Result(column = "question_type", property = "questionType"),
            @Result(column = "subject_id", property = "subjectId"),
            @Result(column = "suggested_time", property = "suggestedTime"),
            @Result(column = "creator_id", property = "creatorId"),
            @Result(column = "editor_id", property = "editorId"),
            @Result(column = "create_time", property = "createTime"),
            @Result(column = "update_time", property = "updateTime"),
            @Result(column = "delete_time", property = "deleteTime"),
            @Result(column = "delete_reason", property = "deleteReason")
    })
    Question findById(@Param("id") Integer id);


    @Select("SELECT * FROM questions WHERE creator_id = #{userId} AND status = 0 ORDER BY update_time DESC")
    List<Question> findDraftsByUser(@Param("userId") Integer userId);

    @Update("UPDATE questions SET status = 2, delete_time = NOW(), delete_reason = #{reason} " +
            "WHERE id = #{id}")
    int softDelete(@Param("id") Integer id, @Param("reason") String reason);

    @Delete("DELETE FROM questions WHERE id = #{id}")
    int physicalDelete(@Param("id") Integer id);

    @Select("<script>" +
            "SELECT q.*, s.name as subjectName, u.real_name as creatorName FROM questions q " +
            "LEFT JOIN subjects s ON q.subject_id = s.id " +
            "LEFT JOIN users u ON q.creator_id = u.id " +
            "WHERE q.status = 1 " +
            "<if test='type != null and type != 0'> AND q.question_type = #{type} </if>" +
            "<if test='keyword != null and keyword != \"\"'> AND q.content LIKE CONCAT('%', #{keyword}, '%') </if>" +
            "ORDER BY q.create_time DESC " +
            "LIMIT #{offset}, #{pageSize}" +
            "</script>")
    @Results({
            @Result(column = "id", property = "id"),
            @Result(column = "question_type", property = "questionType"),
            @Result(column = "subject_id", property = "subjectId"),
            @Result(column = "suggested_time", property = "suggestedTime"),
            @Result(column = "creator_id", property = "creatorId"),
            @Result(column = "editor_id", property = "editorId"),
            @Result(column = "create_time", property = "createTime"),
            @Result(column = "update_time", property = "updateTime"),
            @Result(column = "subjectName", property = "subjectName"),
            @Result(column = "creatorName", property = "creatorName")
    })
    List<Question> getQuestionList(@Param("offset") int offset,
                                   @Param("pageSize") int pageSize,
                                   @Param("type") Integer type,
                                   @Param("keyword") String keyword);

    @Select("<script>" +
            "SELECT COUNT(*) FROM questions WHERE status = 1 " +
            "<if test='type != null and type != 0'> AND question_type = #{type} </if>" +
            "<if test='keyword != null and keyword != \"\"'> AND content LIKE CONCAT('%', #{keyword}, '%') </if>" +
            "</script>")
    int getQuestionCount(@Param("type") Integer type, @Param("keyword") String keyword);

    @Update("<script>" +
            "UPDATE questions SET difficulty = #{difficulty}, update_time = NOW() " +
            "WHERE id IN " +
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            "</script>")
    int batchUpdateDifficulty(@Param("ids") List<Integer> ids, @Param("difficulty") Integer difficulty);

    @Update("<script>" +
            "UPDATE questions SET subject_id = #{subjectId}, update_time = NOW() " +
            "WHERE id IN " +
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            "</script>")
    int batchUpdateSubject(@Param("ids") List<Integer> ids, @Param("subjectId") Integer subjectId);

    @Select("SELECT q.*, u.real_name as creatorName, s.name as subjectName FROM questions q " +
            "LEFT JOIN users u ON q.creator_id = u.id " +
            "LEFT JOIN subjects s ON q.subject_id = s.id " +
            "WHERE q.status = 2 " +
            "ORDER BY q.delete_time DESC")
    List<Question> getRecycleBin();

    @Update("UPDATE questions SET status = 1, delete_time = NULL, delete_reason = NULL " +
            "WHERE id = #{id}")
    int restoreQuestion(@Param("id") Integer id);

    @Delete("DELETE FROM questions WHERE id = #{id}")
    int permanentDelete(@Param("id") Integer id);

    /**
     * 统计指定学科下的试题数量
     */
    @Select("SELECT COUNT(*) FROM questions WHERE subject_id = #{subjectId} AND status != 2")
    int countBySubjectId(@Param("subjectId") Integer subjectId);


    /**
     * 高级组合查询试题（支持多条件组合）
     */
    @Select("<script>" +
            "SELECT q.*, s.name as subjectName, u.real_name as creatorName " +
            "FROM questions q " +
            "LEFT JOIN subjects s ON q.subject_id = s.id " +
            "LEFT JOIN users u ON q.creator_id = u.id " +
            "WHERE q.status = 1 " +
            "<if test='query.questionType != null'> AND q.question_type = #{query.questionType} </if>" +
            "<if test='query.subjectId != null'> AND (q.subject_id = #{query.subjectId} OR q.subject_id IN (SELECT id FROM subjects WHERE parent_id = #{query.subjectId})) </if>" +
            "<if test='query.difficulty != null'> AND q.difficulty = #{query.difficulty} </if>" +
            "<if test='query.keyword != null and query.keyword != \"\"'> AND q.content LIKE CONCAT('%', #{query.keyword}, '%') </if>" +
            "<if test='query.myQuestions != null and query.myQuestions'> AND q.creator_id = #{userId} </if>" +
            "<if test='query.startTime != null'> AND q.create_time &gt;= #{query.startTime} </if>" +
            "<if test='query.endTime != null'> AND q.create_time &lt;= #{query.endTime} </if>" +
            "<if test='query.difficultyList != null and query.difficultyList.size() > 0'> " +
            "AND q.difficulty IN " +
            "<foreach collection='query.difficultyList' item='dif' open='(' separator=',' close=')'>#{dif}</foreach> " +
            "</if>" +
            "<if test='query.subjectIdList != null and query.subjectIdList.size() > 0'> " +
            "AND ( " +
            "<foreach collection='query.subjectIdList' item='sid' separator=' OR '>" +
            "(q.subject_id = #{sid} OR q.subject_id IN (SELECT id FROM subjects WHERE parent_id = #{sid}))" +
            "</foreach>" +
            ") " +
            "</if>" +
            "<if test='query.questionTypeList != null and query.questionTypeList.size() > 0'> " +
            "AND q.question_type IN " +
            "<foreach collection='query.questionTypeList' item='qt' open='(' separator=',' close=')'>#{qt}</foreach> " +
            "</if>" +
            "ORDER BY ${query.sortField} ${query.sortOrder} " +
            "LIMIT #{offset}, #{query.pageSize}" +
            "</script>")
    List<Question> advancedQuery(@Param("query") com.example.dto.exam.QuestionQueryDTO query,
                                 @Param("userId") Integer userId,
                                 @Param("offset") int offset);

    /**
     * 统计高级查询结果数量
     */
    @Select("<script>" +
            "SELECT COUNT(*) FROM questions q " +
            "WHERE q.status = 1 " +
            "<if test='query.questionType != null'> AND q.question_type = #{query.questionType} </if>" +
            "<if test='query.subjectId != null'> AND (q.subject_id = #{query.subjectId} OR q.subject_id IN (SELECT id FROM subjects WHERE parent_id = #{query.subjectId})) </if>" +
            "<if test='query.difficulty != null'> AND q.difficulty = #{query.difficulty} </if>" +
            "<if test='query.keyword != null and query.keyword != \"\"'> AND q.content LIKE CONCAT('%', #{query.keyword}, '%') </if>" +
            "<if test='query.myQuestions != null and query.myQuestions'> AND q.creator_id = #{userId} </if>" +
            "<if test='query.startTime != null'> AND q.create_time &gt;= #{query.startTime} </if>" +
            "<if test='query.endTime != null'> AND q.create_time &lt;= #{query.endTime} </if>" +
            "<if test='query.difficultyList != null and query.difficultyList.size() > 0'> " +
            "AND q.difficulty IN " +
            "<foreach collection='query.difficultyList' item='dif' open='(' separator=',' close=')'>#{dif}</foreach> " +
            "</if>" +
            "<if test='query.subjectIdList != null and query.subjectIdList.size() > 0'> " +
            "AND ( " +
            "<foreach collection='query.subjectIdList' item='sid' separator=' OR '>" +
            "(q.subject_id = #{sid} OR q.subject_id IN (SELECT id FROM subjects WHERE parent_id = #{sid}))" +
            "</foreach>" +
            ") " +
            "</if>" +
            "<if test='query.questionTypeList != null and query.questionTypeList.size() > 0'> " +
            "AND q.question_type IN " +
            "<foreach collection='query.questionTypeList' item='qt' open='(' separator=',' close=')'>#{qt}</foreach> " +
            "</if>" +
            "</script>")
    int countAdvancedQuery(@Param("query") com.example.dto.exam.QuestionQueryDTO query,
                           @Param("userId") Integer userId);

    /**
     * 根据ID列表查询试题（用于导出）
     */
    @Select("<script>" +
            "SELECT q.*, s.name as subjectName, u.real_name as creatorName " +
            "FROM questions q " +
            "LEFT JOIN subjects s ON q.subject_id = s.id " +
            "LEFT JOIN users u ON q.creator_id = u.id " +
            "WHERE q.status = 1 AND q.id IN " +
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach>" +
            "</script>")
    List<Question> findByIds(@Param("ids") List<Integer> ids);


    // ========== 重复检测相关 ==========

    /**
     * 根据内容精确查找完全相同题目
     */
    @Select("SELECT * FROM questions WHERE content = #{content} AND status != 2 AND id != #{excludeId}")
    List<Question> findExactDuplicate(@Param("content") String content, @Param("excludeId") Integer excludeId);

    /**
     * 查找相似题目（使用全文索引）
     */
    @Select("SELECT *, MATCH(content) AGAINST(#{content} IN NATURAL LANGUAGE MODE) AS relevance " +
            "FROM questions WHERE MATCH(content) AGAINST(#{content} IN NATURAL LANGUAGE MODE) " +
            "AND status != 2 AND id != #{excludeId} ORDER BY relevance DESC LIMIT #{limit}")
    List<Question> findSimilarQuestions(@Param("content") String content, @Param("excludeId") Integer excludeId, @Param("limit") int limit);


    /**
     * 获取所有正常状态的试题（用于重复检测）
     */
    @Select("SELECT * FROM questions WHERE status = 1 ORDER BY id")
    List<Question> getAllNormalQuestions();

    /**
     * 根据内容查找相似题目（使用全文索引）
     */
    @Select("SELECT *, MATCH(content) AGAINST(#{content} IN NATURAL LANGUAGE MODE) AS relevance " +
            "FROM questions WHERE MATCH(content) AGAINST(#{content} IN NATURAL LANGUAGE MODE) " +
            "AND status = 1 AND id != #{excludeId} ORDER BY relevance DESC LIMIT #{limit}")
    List<Question> findSimilarByContent(@Param("content") String content,
                                        @Param("excludeId") Integer excludeId,
                                        @Param("limit") int limit);
    /**
     * 检查内容是否存在（用于入库前检测）
     */
    @Select("SELECT COUNT(*) FROM questions WHERE content = #{content} AND status != 2")
    int countByContent(String content);

    // ========== 统计分析相关方法 ==========

    /**
     * 获取总数统计
     */
    @Select("SELECT " +
            "COUNT(*) AS total, " +
            "SUM(CASE WHEN question_type = 1 THEN 1 ELSE 0 END) AS singleCount, " +
            "SUM(CASE WHEN question_type = 2 THEN 1 ELSE 0 END) AS multiCount, " +
            "SUM(CASE WHEN question_type = 3 THEN 1 ELSE 0 END) AS blankCount, " +
            "SUM(CASE WHEN question_type = 4 THEN 1 ELSE 0 END) AS essayCount, " +
            "SUM(CASE WHEN difficulty = 1 THEN 1 ELSE 0 END) AS easyCount, " +
            "SUM(CASE WHEN difficulty = 2 THEN 1 ELSE 0 END) AS mediumCount, " +
            "SUM(CASE WHEN difficulty = 3 THEN 1 ELSE 0 END) AS hardCount " +
            "FROM questions WHERE status = 1")
    Map<String, Object> getTotalStatistics();

    /**
     * 按题型统计
     */
    @Select("SELECT question_type AS type, COUNT(*) AS count FROM questions WHERE status = 1 GROUP BY question_type")
    List<Map<String, Object>> getTypeStatistics();

    /**
     * 按学科统计
     */
    @Select("SELECT q.subject_id AS subjectId, s.name AS subjectName, COUNT(*) AS count " +
            "FROM questions q LEFT JOIN subjects s ON q.subject_id = s.id " +
            "WHERE q.status = 1 GROUP BY q.subject_id ORDER BY count DESC")
    List<Map<String, Object>> getSubjectStatistics();

    /**
     * 按难度统计
     */
    @Select("SELECT difficulty, COUNT(*) AS count FROM questions WHERE status = 1 GROUP BY difficulty")
    List<Map<String, Object>> getDifficultyStatistics();

    /**
     * 按教师统计（排行榜）
     */
    @Select("SELECT u.id AS teacherId, u.real_name AS teacherName, u.college, COUNT(q.id) AS questionCount " +
            "FROM users u LEFT JOIN questions q ON u.id = q.creator_id AND q.status = 1 " +
            "WHERE u.role = 0 GROUP BY u.id ORDER BY questionCount DESC LIMIT #{limit}")
    List<Map<String, Object>> getTeacherStatistics(@Param("limit") int limit);

    /**
     * 获取每日新增趋势
     */
    @Select("SELECT DATE(create_time) AS date, COUNT(*) AS count " +
            "FROM questions WHERE status = 1 AND create_time >= #{startDate} " +
            "GROUP BY DATE(create_time) ORDER BY date ASC")
    List<Map<String, Object>> getDailyTrend(@Param("startDate") LocalDateTime startDate);

    /**
     * 获取热门试题
     */
    @Select("SELECT q.id, q.content, q.question_type AS type, s.name AS subjectName, " +
            "COALESCE(q.view_count, 0) AS viewCount " +
            "FROM questions q LEFT JOIN subjects s ON q.subject_id = s.id " +
            "WHERE q.status = 1 ORDER BY viewCount DESC LIMIT #{limit}")
    List<Map<String, Object>> getHotQuestions(@Param("limit") int limit);

    /**
     * 增加试题查看次数
     */
    @Update("UPDATE questions SET view_count = COALESCE(view_count, 0) + 1 WHERE id = #{questionId}")
    int incrementViewCount(@Param("questionId") Integer questionId);

    // 在 QuestionMapper.java 中添加以下方法

    /**
     * 按题型和难度随机抽取试题（排除指定ID列表）
     */
    @Select("<script>" +
            "SELECT q.*, s.name as subjectName, u.real_name as creatorName " +
            "FROM questions q " +
            "LEFT JOIN subjects s ON q.subject_id = s.id " +
            "LEFT JOIN users u ON q.creator_id = u.id " +
            "WHERE q.status = 1 " +
            "AND q.question_type = #{questionType} " +
            "AND q.difficulty = #{difficulty} " +
            "<if test='subjectId != null'> AND q.subject_id = #{subjectId} </if>" +
            "<if test='excludeIds != null and excludeIds.size() > 0'> " +
            "AND q.id NOT IN " +
            "<foreach collection='excludeIds' item='id' open='(' separator=',' close=')'>#{id}</foreach> " +
            "</if>" +
            "ORDER BY RAND() " +
            "LIMIT #{limit}" +
            "</script>")
    List<Question> randomSelectByTypeAndDifficulty(@Param("questionType") Integer questionType,
                                                   @Param("difficulty") Integer difficulty,
                                                   @Param("subjectId") Integer subjectId,
                                                   @Param("excludeIds") List<Integer> excludeIds,
                                                   @Param("limit") int limit);

    /**
     * 统计各题型各难度的题目数量
     */
    @Select("<script>" +
            "SELECT q.question_type, q.difficulty, COUNT(*) as count " +
            "FROM questions q " +
            "WHERE q.status = 1 " +
            "<if test='subjectId != null'> AND q.subject_id = #{subjectId} </if>" +
            "GROUP BY q.question_type, q.difficulty" +
            "</script>")
    List<Map<String, Object>> countByTypeAndDifficulty(@Param("subjectId") Integer subjectId);
}