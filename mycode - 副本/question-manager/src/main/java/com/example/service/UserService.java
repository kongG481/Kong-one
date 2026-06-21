package com.example.service;

import com.example.dto.user.*;
import com.example.entity.User;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;

public interface UserService {

    /**
     * 用户注册
     * @param registerDTO 注册信息
     */
    void register(UserRegisterDTO registerDTO);

    /**
     * 用户登录
     * @param loginDTO 登录信息
     * @param request HTTP请求（用于获取IP）
     * @return 登录响应信息（包含token和角色）
     */
    UserLoginVO login(UserLoginDTO loginDTO, HttpServletRequest request);


    /**
     * 获取个人信息
     */
    UserProfileVO getProfile(Integer userId);

    /**
     * 更新个人信息
     */
    void updateProfile(Integer userId, UserProfileUpdateDTO updateDTO);

    /**
     * 修改密码
     */
    void changePassword(Integer userId, PasswordChangeDTO passwordDTO);

    /**
     * 分页查询用户列表（管理员功能）
     */
    Map<String, Object> getUserList(String keyword, Integer pageNum, Integer pageSize);

    /**
     * 启用/禁用用户（管理员功能）
     */
    void updateUserStatus(Integer userId, Integer status);

    /**
     * 分配角色（管理员功能）
     */
    void updateUserRole(Integer userId, Integer role);

    /**
     * 删除用户（管理员功能）
     */
    void deleteUser(Integer userId);

    /**
     * 验证原密码是否正确
     */
    boolean verifyOldPassword(Integer userId, String oldPassword);
}