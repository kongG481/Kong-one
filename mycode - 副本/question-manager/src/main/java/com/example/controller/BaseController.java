package com.example.controller;

import com.example.exception.BusinessException;
import com.example.utils.PermissionUtil;
import com.example.utils.ThreadLocalUtil;

import java.util.Map;

/**
 * Controller基类，提供公共方法
 * <p>
 * 功能说明：
 * 1. 提取所有Controller的公共方法，避免代码重复
 * 2. 提供统一的用户信息获取方式
 * 3. 提供统一的权限检查方法
 * <p>
 * 使用方式：
 * 所有Controller继承此类后，可直接调用以下方法：
 * - getCurrentUserId(): 获取当前登录用户ID
 * - getCurrentRole(): 获取当前用户角色
 * - checkAdminPermission(): 检查管理员权限（无权限自动抛异常）
 * - isAdmin(): 判断是否为管理员（返回布尔值）
 * - getUserClaims(): 获取完整的用户JWT信息
 *
 * @author System
 * @since 1.0
 */
public abstract class BaseController {

    /**
     * 获取当前登录用户的ID
     * <p>
     * 从JWT Token中解析用户信息，返回用户ID
     * 适用于需要记录操作人、数据归属等场景
     *
     * @return 当前用户ID
     */
    protected Integer getCurrentUserId() {
        return PermissionUtil.getCurrentUserId();
    }

    /**
     * 获取当前用户的角色
     * <p>
     * 从JWT Token中解析用户角色信息
     * 角色说明：0-普通教师，1-管理员
     *
     * @return 用户角色（0或1）
     */
    protected Integer getCurrentRole() {
        return PermissionUtil.getCurrentRole();
    }

    /**
     * 检查当前用户是否具有管理员权限
     * <p>
     * 如果当前用户不是管理员（role != 1），会自动抛出BusinessException
     * 适用于需要管理员权限的接口，无需手动判断和返回错误
     * <p>
     * 使用示例：
     * <pre>
     * public Result deleteUser(Integer userId) {
     *     checkAdminPermission(); // 不是管理员会自动抛异常
     *     userService.deleteUser(userId);
     *     return Result.success();
     * }
     * </pre>
     *
     * @throws BusinessException 当用户不是管理员时抛出
     */
    protected void checkAdminPermission() {
        PermissionUtil.checkAdmin();
    }

    /**
     * 判断当前用户是否为管理员
     * <p>
     * 与checkAdminPermission()不同，此方法不会抛异常，而是返回布尔值
     * 适用于需要根据角色返回不同数据的场景
     * <p>
     * 使用示例：
     * <pre>
     * public Result getData() {
     *     if (isAdmin()) {
     *         return Result.success(getAllData()); // 管理员看全部
     *     } else {
     *         return Result.success(getMyData()); // 普通用户只看自己
     *     }
     * }
     * </pre>
     *
     * @return true-是管理员，false-不是管理员
     */
    protected boolean isAdmin() {
        return PermissionUtil.isAdmin();
    }

    /**
     * 获取当前用户的完整JWT信息
     * <p>
     * 返回JWT Token中存储的所有用户信息，包括id、role、username等
     * 适用于需要获取多个用户字段的场景
     * <p>
     * 注意：一般情况下建议使用getCurrentUserId()或getCurrentRole()，
     * 只有在需要多个字段时才使用此方法
     *
     * @return 包含用户信息的Map，键包括：id、role、username等
     */
    protected Map<String, Object> getUserClaims() {
        return ThreadLocalUtil.get();
    }
}
