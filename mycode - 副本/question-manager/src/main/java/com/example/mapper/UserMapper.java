package com.example.mapper;

import com.example.entity.User;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 用户Mapper接口
 */
@Mapper
public interface UserMapper {

    /**
     * 根据用户名查询用户
     */
    @Select("SELECT * FROM users WHERE username = #{username}")
    User findByUsername(@Param("username") String username);

    /**
     * 注册新用户
     */
    @Insert("INSERT INTO users(username, real_name, password, college, title, role, status, create_time) " +
            "VALUES(#{username}, #{realName}, #{password}, #{college}, #{title}, 0, 1, NOW())")
    void register(User user);

    /**
     * 更新用户登录信息
     */
    @Update("UPDATE users SET last_login_time = NOW(), last_login_ip = #{ip} WHERE id = #{userId}")
    void updateLoginInfo(@Param("userId") Integer userId, @Param("ip") String ip);

    /**
     * 根据ID查询用户
     */
    @Select("SELECT * FROM users WHERE id = #{id}")
    User findById(@Param("id") Integer id);


    /**
     * 更新用户基本信息
     */
    @Update("UPDATE users SET real_name = #{realName}, college = #{college}, title = #{title} WHERE id = #{id}")
    void updateProfile(User user);

    /**
     * 更新密码
     */
    @Update("UPDATE users SET password = #{password} WHERE id = #{id}")
    void updatePassword(@Param("id") Integer id, @Param("password") String password);

    /**
     * 分页查询用户列表
     */
    @Select("SELECT * FROM users WHERE username LIKE CONCAT('%', #{keyword}, '%') OR real_name LIKE CONCAT('%', #{keyword}, '%') ORDER BY id DESC LIMIT #{offset}, #{pageSize}")
    List<User> findUserList(@Param("keyword") String keyword, @Param("offset") Integer offset, @Param("pageSize") Integer pageSize);

    /**
     * 查询用户总数
     */
    @Select("SELECT COUNT(*) FROM users WHERE username LIKE CONCAT('%', #{keyword}, '%') OR real_name LIKE CONCAT('%', #{keyword}, '%')")
    Long countUsers(@Param("keyword") String keyword);

    /**
     * 更新用户状态
     */
    @Update("UPDATE users SET status = #{status} WHERE id = #{id}")
    void updateStatus(@Param("id") Integer id, @Param("status") Integer status);

    /**
     * 更新用户角色
     */
    @Update("UPDATE users SET role = #{role} WHERE id = #{id}")
    void updateRole(@Param("id") Integer id, @Param("role") Integer role);

    /**
     * 删除用户
     */
    @Delete("DELETE FROM users WHERE id = #{id}")
    void deleteById(@Param("id") Integer id);
}