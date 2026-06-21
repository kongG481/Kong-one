package com.example.dto.user;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 用户个人信息VO
 */
@Data
public class UserProfileVO {
    private Integer id;
    private String username;        // 工号
    private String realName;         // 真实姓名
    private String college;          // 学院
    private String title;            // 职称
    private Integer role;            // 角色
    private Integer status;          // 状态
    private LocalDateTime lastLoginTime; // 最后登录时间
    private String lastLoginIp;       // 最后登录IP
    private LocalDateTime createTime;   // 注册时间
}
