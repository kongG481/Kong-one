package com.example.mapper.exam;

import com.example.entity.question.Subject;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface SubjectMapper {

    @Select("SELECT * FROM subjects WHERE parent_id = 0 AND status = 1 ORDER BY sort_order")
    List<Subject> getTopLevelSubjects();

    @Select("SELECT * FROM subjects WHERE parent_id = #{parentId} AND status = 1 ORDER BY sort_order")
    List<Subject> getSubjectsByParentId(@Param("parentId") Integer parentId);

    @Select("SELECT * FROM subjects WHERE id = #{id}")
    Subject findById(@Param("id") Integer id);

    // SubjectMapper.java 中添加

    /**
     * 插入学科
     */
    @Options(useGeneratedKeys = true, keyProperty = "id")
    @Insert("INSERT INTO subjects (name, parent_id, level, sort_order, status, create_time) " +
            "VALUES (#{name}, #{parentId}, #{level}, #{sortOrder}, #{status}, NOW())")
    int insert(Subject subject);

    /**
     * 更新学科
     */
    @Update("UPDATE subjects SET name = #{name}, parent_id = #{parentId}, " +
            "sort_order = #{sortOrder}, status = #{status} WHERE id = #{id}")
    int update(Subject subject);

    /**
     * 删除学科
     */
    @Delete("DELETE FROM subjects WHERE id = #{id}")
    int deleteById(@Param("id") Integer id);

    /**
     * 检查学科是否有子节点
     */
    @Select("SELECT COUNT(*) FROM subjects WHERE parent_id = #{parentId}")
    int countByParentId(@Param("parentId") Integer parentId);

    /**
     * 获取所有学科（用于统计）
     */
    @Select("SELECT * FROM subjects")
    List<Subject> getAllSubjects();

    /**
     * 根据名称检查是否存在
     */
    @Select("SELECT COUNT(*) FROM subjects WHERE name = #{name} AND id != #{id}")
    int countByName(@Param("name") String name, @Param("id") Integer id);
}