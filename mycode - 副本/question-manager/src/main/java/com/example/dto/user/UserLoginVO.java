package com.example.dto.user;

import lombok.Data;

/**
 * 用户登录响应DTO
 */
@Data
public class UserLoginVO {
    private Integer id;
    private String username;
    private String realName;
    private String college;
    private String title;
    private Integer role;           // 角色：0-普通教师，1-管理员
    private String token;            // JWT令牌
}
