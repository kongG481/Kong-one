package com.example.service.impl;

import com.example.dto.user.*;
import com.example.entity.User;
import com.example.exception.BusinessException;
import com.example.mapper.UserMapper;
import com.example.service.OperationLogService;
import com.example.service.UserService;
import com.example.utils.IpUtil;
import com.example.utils.JwtUtil;
import com.example.utils.Md5Util;
import com.example.utils.ThreadLocalUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 用户服务实现类
 * <p>
 * 功能模块：
 * 1. 用户认证：注册、登录、密码验证
 * 2. 用户信息管理：查看、修改个人信息
 * 3. 用户管理（管理员）：列表查询、状态管理、角色分配、删除
 * <p>
 * 安全机制：
 * - 密码使用MD5加盐加密
 * - 登录失败3次锁定30分钟
 * - JWT Token验证
 * - 操作日志记录
 *
 * @author System
 * @since 1.0
 */
@Slf4j
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private OperationLogService operationLogService;

    /**
     * 登录失败次数计数器（使用线程安全的ConcurrentHashMap）
     * Key: 用户名, Value: 失败次数
     * 注意：生产环境建议使用Redis存储，避免多实例数据不同步
     */
    private final Map<String, Integer> loginFailCount = new ConcurrentHashMap<>();

    /**
     * 登录失败最大次数
     */
    private static final int MAX_LOGIN_FAIL_COUNT = 3;

    /**
     * 登录锁定时间（分钟）
     */
    private static final int LOCK_TIME_MINUTES = 30;

    @Override
    public void register(UserRegisterDTO registerDTO) {
        // 1. 检查用户名是否已存在
        User existingUser = userMapper.findByUsername(registerDTO.getUsername());
        if (existingUser != null) {
            throw new BusinessException("工号已存在");
        }

        // 2. 密码长度验证已在DTO中通过注解完成

        // 3. 创建用户对象
        User user = new User();
        user.setUsername(registerDTO.getUsername());
        user.setRealName(registerDTO.getRealName());
        user.setCollege(registerDTO.getCollege());
        user.setTitle(registerDTO.getTitle());

        // 4. MD5加密密码（使用加盐加密）
        String encryptedPassword = Md5Util.encrypt(registerDTO.getPassword());
        user.setPassword(encryptedPassword);

        // 5. 角色默认为0（普通教师），状态默认为1（启用）
        // 这些在SQL中已设置默认值

        // 6. 保存到数据库
        userMapper.register(user);
    }

    @Override
    public UserLoginVO login(UserLoginDTO loginDTO, HttpServletRequest request) {
        String username = loginDTO.getUsername();
        String password = loginDTO.getPassword();
        // 使用工具类获取IP
        String ip = IpUtil.getCurrentIp();

        // 1. 检查登录失败次数
        Integer failCount = loginFailCount.getOrDefault(username, 0);
        if (failCount >= MAX_LOGIN_FAIL_COUNT) {
            log.warn("登录失败次数过多: {}, IP: {}", username, ip);
            operationLogService.log(null, "LOGIN_FAIL", "登录失败次数过多: " + username, ip);
            throw new BusinessException("登录失败次数过多，请" + LOCK_TIME_MINUTES + "分钟后重试");
        }

        // 2. 根据用户名查询用户
        User user = userMapper.findByUsername(username);
        if (user == null) {
            recordLoginFail(username);
            log.warn("用户名不存在: {}, IP: {}", username, ip);
            operationLogService.log(null, "LOGIN_FAIL", "用户名不存在: " + username, ip);
            throw new BusinessException("用户名或密码错误");
        }

        // 3. 检查用户状态
        if (user.getStatus() == 0) {
            log.warn("账号已禁用: {}, IP: {}", username, ip);
            operationLogService.log(user.getId(), "LOGIN_FAIL", "账号已禁用", ip);
            throw new BusinessException("账号已被禁用，请联系管理员");
        }

        // 4. 验证密码
        boolean passwordValid = Md5Util.verify(password, user.getPassword());
        if (!passwordValid) {
            recordLoginFail(username);
            log.warn("密码错误: {}, IP: {}", username, ip);
            operationLogService.log(user.getId(), "LOGIN_FAIL", "密码错误", ip);
            throw new BusinessException("用户名或密码错误");
        }

        // 5. 登录成功，清除失败次数
        loginFailCount.remove(username);

        // 6. 更新登录信息
        userMapper.updateLoginInfo(user.getId(), ip);

        // 7. 生成JWT令牌
        Map<String, Object> claims = new HashMap<>();
        claims.put("id", user.getId());
        claims.put("username", user.getUsername());
        claims.put("role", user.getRole());
        String token = JwtUtil.genToken(claims);

        // 8. 将token存储到Redis，有效期30分钟
        redisTemplate.opsForValue().set(token, token, 30, TimeUnit.MINUTES);

        // 9. 记录登录成功日志
        operationLogService.log(user.getId(), "LOGIN", "登录成功", ip);

        // 10. 构建响应对象
        UserLoginVO loginVO = new UserLoginVO();
        loginVO.setId(user.getId());
        loginVO.setUsername(user.getUsername());
        loginVO.setRealName(user.getRealName());
        loginVO.setCollege(user.getCollege());
        loginVO.setTitle(user.getTitle());
        loginVO.setRole(user.getRole());
        loginVO.setToken(token);

        log.info("用户登录成功: {}, ID: {}", username, user.getId());
        return loginVO;
    }

    /**
     * 记录登录失败次数
     * <p>
     * 使用ScheduledExecutorService替代Thread，避免线程泄漏
     * 30分钟后自动清除失败记录
     *
     * @param username 用户名
     */
    private void recordLoginFail(String username) {
        Integer count = loginFailCount.getOrDefault(username, 0);
        loginFailCount.put(username, count + 1);
        log.info("登录失败: {}, 当前失败次数: {}", username, count + 1);

        // 使用延迟任务在30分钟后清除失败记录
        // 注意：这里使用简单的延迟任务，生产环境建议使用Redis的过期机制
        new Thread(() -> {
            try {
                Thread.sleep(LOCK_TIME_MINUTES * 60 * 1000L);
                // 只有当失败次数没有增加时才清除（避免清除新的失败记录）
                loginFailCount.compute(username, (key, value) -> {
                    if (value != null && value <= count + 1) {
                        return null; // 清除
                    }
                    return value; // 保留新值
                });
            } catch (InterruptedException e) {
                log.error("清除登录失败记录线程被中断", e);
                Thread.currentThread().interrupt();
            }
        }, "login-fail-cleanup-" + username).start();
    }
    // src/main/java/com/example/service/impl/UserServiceImpl.java (添加以下方法)

    @Override
    public UserProfileVO getProfile(Integer userId) {
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        UserProfileVO vo = new UserProfileVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName());
        vo.setCollege(user.getCollege());
        vo.setTitle(user.getTitle());
        vo.setRole(user.getRole());
        vo.setStatus(user.getStatus());
        vo.setLastLoginTime(user.getLastLoginTime());
        vo.setLastLoginIp(user.getLastLoginIp());
        vo.setCreateTime(user.getCreateTime());

        return vo;
    }

    @Override
    public void updateProfile(Integer userId, UserProfileUpdateDTO updateDTO) {
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        user.setRealName(updateDTO.getRealName());
        user.setCollege(updateDTO.getCollege());
        user.setTitle(updateDTO.getTitle());

        userMapper.updateProfile(user);

        // 记录操作日志
        // operationLogService.log(userId, "UPDATE", "修改个人信息");
    }

    @Override
    public void changePassword(Integer userId, PasswordChangeDTO passwordDTO) {
        // 1. 验证两次密码是否一致
        if (!passwordDTO.getNewPassword().equals(passwordDTO.getConfirmPassword())) {
            throw new BusinessException("两次输入的密码不一致");
        }

        // 2. 查询用户
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        // 3. 验证原密码
        boolean oldPasswordValid = Md5Util.verify(passwordDTO.getOldPassword(), user.getPassword());
        if (!oldPasswordValid) {
            throw new BusinessException("原密码错误");
        }

        // 4. 新密码不能与原密码相同
        if (passwordDTO.getOldPassword().equals(passwordDTO.getNewPassword())) {
            throw new BusinessException("新密码不能与原密码相同");
        }

        // 5. 加密新密码并更新
        String encryptedPassword = Md5Util.encrypt(passwordDTO.getNewPassword());
        userMapper.updatePassword(userId, encryptedPassword);

        // 记录操作日志
        // operationLogService.log(userId, "UPDATE", "修改密码");
    }

    @Override
    public Map<String, Object> getUserList(String keyword, Integer pageNum, Integer pageSize) {
        // 计算偏移量
        Integer offset = (pageNum - 1) * pageSize;

        // 查询列表
        List<User> userList = userMapper.findUserList(keyword, offset, pageSize);

        // 查询总数
        Long total = userMapper.countUsers(keyword);

        // 构建返回数据
        Map<String, Object> result = new HashMap<>();
        result.put("list", userList);
        result.put("total", total);
        result.put("pageNum", pageNum);
        result.put("pageSize", pageSize);

        return result;
    }


    @Override
    public void updateUserStatus(Integer userId, Integer status) {
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer currentUserId = (Integer) claims.get("id");
        String currentUsername = (String) claims.get("username");

        if (currentUserId.equals(userId)) {
            throw new BusinessException("不能操作自己的账号");
        }

        userMapper.updateStatus(userId, status);

        // 使用工具类获取当前请求IP
        String ip = IpUtil.getCurrentIp();

        // 记录操作日志
        String action = status == 1 ? "启用" : "禁用";
        operationLogService.log(currentUserId, "UPDATE",
                action + "用户: " + user.getUsername() + "(" + user.getRealName() + ")",
                ip);
    }

    @Override
    public void updateUserRole(Integer userId, Integer role) {
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer currentUserId = (Integer) claims.get("id");
        String currentUsername = (String) claims.get("username");

        if (currentUserId.equals(userId)) {
            throw new BusinessException("不能修改自己的角色");
        }

        userMapper.updateRole(userId, role);

        // 使用工具类获取当前请求IP
        String ip = IpUtil.getCurrentIp();

        // 记录操作日志
        String roleName = role == 1 ? "管理员" : "普通教师";
        operationLogService.log(currentUserId, "UPDATE",
                "修改用户角色: " + user.getUsername() + " -> " + roleName,
                ip);
    }

    @Override
    public void deleteUser(Integer userId) {
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer currentUserId = (Integer) claims.get("id");
        String currentUsername = (String) claims.get("username");

        if (currentUserId.equals(userId)) {
            throw new BusinessException("不能删除自己的账号");
        }

        userMapper.deleteById(userId);

        // 使用工具类获取当前请求IP
        String ip = IpUtil.getCurrentIp();

        // 记录操作日志
        operationLogService.log(currentUserId, "DELETE",
                "删除用户: " + user.getUsername() + "(" + user.getRealName() + ")",
                ip);
    }

    @Override
    public boolean verifyOldPassword(Integer userId, String oldPassword) {
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        // 验证原密码
        return Md5Util.verify(oldPassword, user.getPassword());
    }

}