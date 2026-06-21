package com.example.controller.user;

import com.example.controller.BaseController;
import com.example.dto.user.*;
import com.example.entity.Result;
import com.example.service.UserService;
import com.example.utils.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 用户管理控制器
 * <p>
 * 功能模块：
 * 1. 用户认证：注册、登录、登出、Token验证
 * 2. 个人信息：查看、修改个人信息、修改密码
 * 3. 用户管理（管理员）：用户列表、状态管理、角色分配、删除用户
 * <p>
 * 权限说明：
 * - 公开接口：注册、登录
 * - 登录用户：个人信息相关
 * - 管理员：用户管理相关
 *
 * @author System
 * @since 1.0
 */
@RestController
@RequestMapping("/user")
public class UserController extends BaseController {

    @Autowired
    private UserService userService;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;  // Redis模板，用于Token管理

    /**
     * 用户注册
     * <p>
     * 公开接口，无需登录即可访问
     * 自动校验参数（@Valid）：用户名、密码、邮箱等格式
     *
     * @param registerDTO 注册信息（包含用户名、密码、邮箱等）
     * @return 注册结果
     */
    @PostMapping("/register")
    public Result register(@Valid @RequestBody UserRegisterDTO registerDTO) {
        userService.register(registerDTO);
        return Result.success("注册成功");
    }

    /**
     * 用户登录
     * <p>
     * 公开接口，验证用户名密码后返回JWT Token
     * Token会同时存储在Redis中，用于后续请求验证和登出管理
     *
     * @param loginDTO 登录信息（用户名、密码）
     * @param request HTTP请求（用于获取客户端IP）
     * @return 登录结果（包含token、用户ID、角色等信息）
     */
    @PostMapping("/login")
    public Result<UserLoginVO> login(@Valid @RequestBody UserLoginDTO loginDTO,
                                     HttpServletRequest request) {
        UserLoginVO loginVO = userService.login(loginDTO, request);
        return Result.success(loginVO);
    }

    /**
     * 用户登出
     * <p>
     * 从Redis中删除Token，使当前Token失效
     * 前端需要同时清除本地存储的Token
     *
     * @param request HTTP请求（用于获取Token）
     * @return 登出结果
     */
    @PostMapping("/logout")
    public Result logout(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token != null) {
            // 从Redis中删除token，使其失效
            redisTemplate.delete(token);
        }
        return Result.success("登出成功");
    }

    // src/main/java/com/example/controller/UserController.java (添加以下方法)

    /**
     * 获取个人信息
     * <p>
     * 需要登录，返回当前用户的详细信息
     * 包括：用户名、邮箱、手机号、角色等
     *
     * @return 用户个人信息
     */
    @GetMapping("/profile")
    public Result<UserProfileVO> getProfile() {
        Integer userId = getCurrentUserId();
        UserProfileVO profile = userService.getProfile(userId);
        return Result.success(profile);
    }

    /**
     * 修改个人信息
     * <p>
     * 需要登录，只能修改自己的信息
     * 可修改字段：昵称、手机号、邮箱等
     *
     * @param updateDTO 更新的用户信息
     * @return 修改结果
     */
    @PutMapping("/profile")
    public Result updateProfile(@Valid @RequestBody UserProfileUpdateDTO updateDTO) {
        Integer userId = getCurrentUserId();
        userService.updateProfile(userId, updateDTO);
        return Result.success("修改成功");
    }

    /**
     * 修改密码
     * <p>
     * 需要登录，验证旧密码后设置新密码
     * 安全性：
     * 1. 必须提供正确的旧密码
     * 2. 新密码需要符合复杂度要求
     * 3. 密码使用MD5加密存储
     *
     * @param passwordDTO 密码信息（旧密码、新密码）
     * @return 修改结果
     */
    @PutMapping("/password")
    public Result changePassword(@Valid @RequestBody PasswordChangeDTO passwordDTO) {
        Integer userId = getCurrentUserId();
        userService.changePassword(userId, passwordDTO);
        return Result.success("密码修改成功");
    }

    /**
     * 分页查询用户列表
     * <p>
     * 【管理员权限】查看系统中所有用户
     * 支持关键词搜索（用户名、邮箱、手机号）
     * 返回分页数据，包含：用户列表、总数、当前页码
     *
     * @param keyword 搜索关键词（可选）
     * @param pageNum 页码，默认1
     * @param pageSize 每页数量，默认10
     * @return 分页用户列表
     */
    @GetMapping("/admin/list")
    public Result<Map<String, Object>> getUserList(
            @RequestParam(required = false, defaultValue = "") String keyword,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {

        // 检查管理员权限，无权限自动抛异常
        checkAdminPermission();

        Map<String, Object> result = userService.getUserList(keyword, pageNum, pageSize);
        return Result.success(result);
    }

    /**
     * 启用/禁用用户
     * <p>
     * 【管理员权限】控制用户账号状态
     * 状态说明：0-禁用（无法登录），1-启用（正常）
     * 使用场景：违规用户封号、离职员工禁用等
     *
     * @param userId 要操作的用户ID
     * @param status 目标状态（0-禁用，1-启用）
     * @return 操作结果
     */
    @PutMapping("/admin/{userId}/status")
    public Result updateUserStatus(
            @PathVariable Integer userId,
            @RequestParam Integer status) {

        // 检查管理员权限
        checkAdminPermission();

        if (status != 0 && status != 1) {
            return Result.error("状态值不正确");
        }

        userService.updateUserStatus(userId, status);
        return Result.success(status == 1 ? "用户已启用" : "用户已禁用");
    }

    /**
     * 分配用户角色
     * <p>
     * 【管理员权限】修改用户的角色
     * 角色说明：0-普通教师，1-管理员
     * 注意：角色变更会影响用户的权限范围
     *
     * @param userId 要操作的用户ID
     * @param role 目标角色（0-教师，1-管理员）
     * @return 操作结果
     */
    @PutMapping("/admin/{userId}/role")
    public Result updateUserRole(
            @PathVariable Integer userId,
            @RequestParam Integer role) {

        // 检查管理员权限
        checkAdminPermission();

        if (role != 0 && role != 1) {
            return Result.error("角色值不正确");
        }

        userService.updateUserRole(userId, role);
        return Result.success(role == 1 ? "已设置为管理员" : "已设置为普通教师");
    }

    /**
     * 删除用户
     * <p>
     * 【管理员权限】物理删除用户账号
     * 注意：此操作不可恢复，删除后用户所有数据可能被影响
     * 建议：优先使用禁用功能（updateUserStatus）
     *
     * @param userId 要删除的用户ID
     * @return 删除结果
     */
    @DeleteMapping("/admin/{userId}")
    public Result deleteUser(@PathVariable Integer userId) {
        // 检查管理员权限
        checkAdminPermission();

        userService.deleteUser(userId);
        return Result.success("用户已删除");
    }

    /**
     * 验证原密码是否正确
     * <p>
     * 用于修改密码前的二次验证
     * 前端可以先调用此接口验证旧密码，再调用修改密码接口
     *
     * @param params 包含oldPassword字段的Map
     * @return true-密码正确，false-密码错误
     */
    @PostMapping("/verify-password")
    public Result<Boolean> verifyPassword(@RequestBody Map<String, String> params) {
        Integer userId = getCurrentUserId();
        String oldPassword = params.get("oldPassword");

        if (oldPassword == null || oldPassword.trim().isEmpty()) {
            return Result.error("原密码不能为空");
        }

        boolean isValid = userService.verifyOldPassword(userId, oldPassword);
        return Result.success(isValid);
    }
    /**
     * 验证Token是否有效
     * <p>
     * 检查Token是否在Redis中存在且未过期
     * 用于前端判断当前登录状态
     * 注意：即使Token格式正确，如果Redis中不存在也视为无效
     *
     * @param request HTTP请求（用于获取Token）
     * @return true-Token有效，false-Token无效
     */
    @GetMapping("/validate-token")
    public Result<Boolean> validateToken(HttpServletRequest request) {
        String token = request.getHeader("Authorization");

        if (token == null || token.isEmpty()) {
            return Result.success(false);
        }

        try {
            // 从Redis获取相同的token
            ValueOperations<String, String> operations = redisTemplate.opsForValue();
            String redisToken = operations.get(token);

            // 如果 Redis 中存在该 token，说明有效
            if (redisToken != null) {
                // 解析token验证签名
                JwtUtil.parseToken(token);
                return Result.success(true);
            }

            return Result.success(false);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.success(false);
        }
    }
}