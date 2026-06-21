// src/main/java/com/example/dto/UserProfileUpdateDTO.java
package com.example.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 个人信息修改DTO
 */
@Data
public class UserProfileUpdateDTO {

    @NotBlank(message = "姓名不能为空")
    @Size(min = 2, max = 20, message = "姓名长度必须在2-20字符之间")
    private String realName;

    @NotBlank(message = "学院不能为空")
    private String college;

    @NotBlank(message = "职称不能为空")
    private String title;
}