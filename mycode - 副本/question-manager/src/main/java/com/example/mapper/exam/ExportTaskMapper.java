package com.example.mapper.exam;

import com.example.entity.question.ExportTask;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface ExportTaskMapper {

    @Insert("INSERT INTO export_tasks (user_id, export_format, question_count, file_url, create_time) " +
            "VALUES (#{userId}, #{exportFormat}, #{questionCount}, #{fileUrl}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ExportTask task);

    @Select("SELECT et.*, u.real_name as userName FROM export_tasks et " +
            "LEFT JOIN users u ON et.user_id = u.id " +
            "WHERE et.id = #{id}")
    ExportTask findById(@Param("id") Integer id);

    @Select("SELECT et.*, u.real_name as userName FROM export_tasks et " +
            "LEFT JOIN users u ON et.user_id = u.id " +
            "WHERE et.user_id = #{userId} " +
            "ORDER BY et.create_time DESC")
    List<ExportTask> findByUserId(@Param("userId") Integer userId);

    @Select("SELECT et.*, u.real_name as userName FROM export_tasks et " +
            "LEFT JOIN users u ON et.user_id = u.id " +
            "ORDER BY et.create_time DESC LIMIT 100")
    List<ExportTask> findAll();

    @Delete("DELETE FROM export_tasks WHERE id = #{id}")
    int deleteById(@Param("id") Integer id);

    // 在 ExportTaskMapper.java 中添加

    /**
     * 查询创建时间之前的导出任务（用于清理过期文件）
     */
    @Select("SELECT et.*, u.real_name as userName FROM export_tasks et " +
            "LEFT JOIN users u ON et.user_id = u.id " +
            "WHERE et.create_time < #{expireTime}")
    List<ExportTask> findByCreateTimeBefore(@Param("expireTime") LocalDateTime expireTime);

    /**
     * 根据用户ID和ID查询
     */
    @Select("SELECT et.*, u.real_name as userName FROM export_tasks et " +
            "LEFT JOIN users u ON et.user_id = u.id " +
            "WHERE et.id = #{id} AND et.user_id = #{userId}")
    ExportTask findByIdAndUserId(@Param("id") Integer id, @Param("userId") Integer userId);
}