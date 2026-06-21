package com.example.service;

import com.example.exception.BusinessException;
import com.example.utils.PermissionUtil;
import com.example.utils.ThreadLocalUtil;

import java.util.Map;

/**
 * Service基类，提供公共方法
 * <p>
 * 功能说明：
 * 1. 提取所有Service的公共方法，避免代码重复
 * 2. 提供统一的用户信息获取方式
 * 3. 提供统一的权限校验方法
 * <p>
 * 使用场景：
 * 在Service层需要获取当前用户信息时，继承此类后可以直接调用：
 * - getCurrentUserId(): 获取当前用户ID
 * - getCurrentUserName(): 获取当前用户名
 * - checkOwnership(creatorId): 检查资源所有权
 * 
 * 与BaseController的区别：
 * - BaseController用于Controller层，侧重HTTP请求相关
 * - BaseService用于Service层，侧重业务逻辑相关
 *
 * @author System
 * @since 1.0
 */
public abstract class BaseService {

    /**
     * 获取当前登录用户的ID
     * <p>
     * 从JWT Token中解析用户信息，返回用户ID
     * 适用于Service层需要记录操作人、数据归属等场景
     * <p>
     * 使用示例：
     * <pre>
     * public void createQuestion(QuestionDTO dto) {
     *     Integer userId = getCurrentUserId();
     *     question.setCreatorId(userId);
     *     questionMapper.insert(question);
     * }
     * </pre>
     *
     * @return 当前用户ID
     */
    protected Integer getCurrentUserId() {
        return PermissionUtil.getCurrentUserId();
    }

    /**
     * 获取当前登录用户的用户名
     * <p>
     * 从JWT Token中解析用户名信息
     * 适用于需要记录操作人名称的场景
     *
     * @return 当前用户名
     */
    protected String getCurrentUserName() {
        Map<String, Object> claims = ThreadLocalUtil.get();
        return (String) claims.get("username");
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
     * 适用于Service层需要管理员权限的业务逻辑
     * <p>
     * 使用示例：
     * <pre>
     * public void deleteUser(Integer userId) {
     *     checkAdminPermission(); // 不是管理员会自动抛异常
     *     userMapper.deleteById(userId);
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
     * 适用于需要根据角色执行不同业务逻辑的场景
     *
     * @return true-是管理员，false-不是管理员
     */
    protected boolean isAdmin() {
        return PermissionUtil.isAdmin();
    }

    /**
     * 检查当前用户是否为资源的创建者（所有者）
     * <p>
     * 用于数据权限控制，确保用户只能修改/删除自己创建的资源
     * 如果既不是创建者也不是管理员，会抛出BusinessException
     * <p>
     * 使用示例：
     * <pre>
     * public void updateQuestion(Integer questionId, QuestionDTO dto) {
     *     Question question = questionMapper.findById(questionId);
     *     checkOwnership(question.getCreatorId()); // 检查是否有权限修改
     *     questionMapper.update(question);
     * }
     * </pre>
     *
     * @param creatorId 资源的创建者ID
     * @throws BusinessException 当用户既不是创建者也不是管理员时抛出
     */
    protected void checkOwnership(Integer creatorId) {
        PermissionUtil.checkOwnerOrAdmin(creatorId);
    }

    /**
     * 获取当前用户的完整JWT信息
     * <p>
     * 返回JWT Token中存储的所有用户信息，包括id、role、username等
     * 适用于需要获取多个用户字段的场景
     * <p>
     * 注意：一般情况下建议使用getCurrentUserId()或getCurrentUserName()，
     * 只有在需要多个字段时才使用此方法
     *
     * @return 包含用户信息的Map，键包括：id、role、username等
     */
    protected Map<String, Object> getUserClaims() {
        return ThreadLocalUtil.get();
    }
}
