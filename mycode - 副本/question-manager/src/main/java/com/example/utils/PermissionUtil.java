// src/main/java/com/example/utils/PermissionUtil.java
package com.example.utils;

import com.example.entity.User;
import com.example.exception.BusinessException;

import java.util.Map;

/**
 * 权限控制工具类
 */
public class PermissionUtil {

    /**
     * 检查是否为管理员
     */
    public static void checkAdmin() {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer role = (Integer) claims.get("role");
        if (role != 1) {
            throw new BusinessException("无权限访问，需要管理员权限");
        }
    }

    /**
     * 检查是否为资源创建者或管理员
     * @param creatorId 资源创建者ID
     */
    public static void checkOwnerOrAdmin(Integer creatorId) {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer currentUserId = (Integer) claims.get("id");
        Integer role = (Integer) claims.get("role");

        if (!currentUserId.equals(creatorId) && role != 1) {
            throw new BusinessException("无权限操作此资源");
        }
    }

    /**
     * 获取当前用户ID
     */
    public static Integer getCurrentUserId() {
        Map<String, Object> claims = ThreadLocalUtil.get();
        return (Integer) claims.get("id");
    }

    /**
     * 获取当前用户角色
     */
    public static Integer getCurrentRole() {
        Map<String, Object> claims = ThreadLocalUtil.get();
        return (Integer) claims.get("role");
    }

    /**
     * 判断是否为管理员
     */
    public static boolean isAdmin() {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer role = (Integer) claims.get("role");
        return role == 1;
    }
}