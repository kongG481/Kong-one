package com.example.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户注册请求DTO
 */
@Data
public class UserRegisterDTO {

    @NotBlank(message = "工号不能为空")
    @Pattern(regexp = "^[0-9]{5}$", message = "工号必须为5位数字")
    private String username;

    @NotBlank(message = "姓名不能为空")
    @Size(min = 2, max = 20, message = "姓名长度必须在2-20字符之间")
    private String realName;

    @NotBlank(message = "学院不能为空")
    private String college;

    @NotBlank(message = "职称不能为空")
    private String title;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度必须在6-20字符之间")
    private String password;
}