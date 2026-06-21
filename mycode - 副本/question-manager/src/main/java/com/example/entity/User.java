package com.example.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 用户实体类，对应数据库users表
 */
@Data
public class User {
    private Integer id;                 // 用户ID
    private String username;             // 工号
    private String realName;             // 真实姓名
    private String password;              // 密码(MD5加密)
    private String college;               // 学院
    private String title;                 // 职称
    private Integer role;                  // 角色：0-普通教师，1-管理员
    private Integer status;                // 状态：0-禁用，1-启用
    private LocalDateTime lastLoginTime;   // 最后登录时间
    private String lastLoginIp;             // 最后登录IP
    private LocalDateTime createTime;       // 注册时间
}