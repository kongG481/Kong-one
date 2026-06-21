// src/main/java/com/example/mapper/OperationLogMapper.java
package com.example.mapper;

import com.example.entity.OperationLog;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface OperationLogMapper {

    /**
     * 插入操作日志
     */
    @Insert("INSERT INTO operation_logs(user_id, operation_type, operation_desc, ip_address, create_time) " +
            "VALUES(#{userId}, #{operationType}, #{operationDesc}, #{ipAddress}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(OperationLog log);

    /**
     * 分页查询日志
     */
    @Select("<script>" +
            "SELECT l.*, u.username, u.real_name " +
            "FROM operation_logs l " +
            "LEFT JOIN users u ON l.user_id = u.id " +
            "WHERE 1=1 " +
            "<if test='userId != null'> AND l.user_id = #{userId} </if>" +
            "<if test='operationType != null and operationType != \"\"'> AND l.operation_type = #{operationType} </if>" +
            "<if test='startTime != null'> AND l.create_time >= #{startTime} </if>" +
            "<if test='endTime != null'> AND l.create_time &lt;= #{endTime} </if>" +
            "ORDER BY l.create_time DESC " +
            "LIMIT #{offset}, #{pageSize}" +
            "</script>")
    List<OperationLog> findLogs(@Param("userId") Integer userId,
                                @Param("operationType") String operationType,
                                @Param("startTime") LocalDateTime startTime,
                                @Param("endTime") LocalDateTime endTime,
                                @Param("offset") Integer offset,
                                @Param("pageSize") Integer pageSize);

    /**
     * 统计日志总数
     */
    @Select("<script>" +
            "SELECT COUNT(*) FROM operation_logs l " +
            "WHERE 1=1 " +
            "<if test='userId != null'> AND l.user_id = #{userId} </if>" +
            "<if test='operationType != null and operationType != \"\"'> AND l.operation_type = #{operationType} </if>" +
            "<if test='startTime != null'> AND l.create_time >= #{startTime} </if>" +
            "<if test='endTime != null'> AND l.create_time &lt;= #{endTime} </if>" +
            "</script>")
    Long countLogs(@Param("userId") Integer userId,
                   @Param("operationType") String operationType,
                   @Param("startTime") LocalDateTime startTime,
                   @Param("endTime") LocalDateTime endTime);
}