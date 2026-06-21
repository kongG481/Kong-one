package com.example.dto.user;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 用户登录请求DTO
 */
@Data
public class UserLoginDTO {

    @NotBlank(message = "工号不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;
}
