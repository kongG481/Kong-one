package com.example.utils;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;

import java.util.Date;
import java.util.Map;

/**
 * JWT Token工具类
 * <p>
 * 功能说明：
 * 1. 生成JWT Token（包含用户信息、过期时间）
 * 2. 解析和验证JWT Token
 * 3. 使用HMAC256算法加密
 * <p>
 * 安全说明：
 * - 密钥长度至少256位（32字节）
 * - Token默认有效期1小时
 * - 密钥应该配置在application.yml中，不要硬编码
 *
 * @author System
 * @since 1.0
 */
public class JwtUtil {

    /**
     * JWT签名密钥（生产环境应该使用至少32位的复杂密钥）
     * 建议配置：在application.yml中配置，并使用随机生成的密钥
     * 示例：openssl rand -base64 32
     */
    private static final String KEY = "question_bank_jwt_secret_key_2024_production_version_!@#$%";

    /**
     * Token默认有效期（毫秒）- 1小时
     */
    private static final long DEFAULT_EXPIRE_TIME = 1000 * 60 * 60;

    /**
     * 生成JWT Token
     * <p>
     * 将用户信息封装到Token中，并设置过期时间
     * <p>
     * 使用示例：
     * <pre>
     * Map<String, Object> claims = new HashMap<>();
     * claims.put("id", user.getId());
     * claims.put("username", user.getUsername());
     * claims.put("role", user.getRole());
     * String token = JwtUtil.genToken(claims);
     * </pre>
     *
     * @param claims 用户信息（如：id、username、role等）
     * @return 生成的JWT Token字符串
     */
    public static String genToken(Map<String, Object> claims) {
        return genToken(claims, DEFAULT_EXPIRE_TIME);
    }

    /**
     * 生成JWT Token（自定义有效期）
     * <p>
     * 适用于需要自定义Token有效期的场景
     *
     * @param claims 用户信息
     * @param expireMillis 有效期（毫秒）
     * @return 生成的JWT Token字符串
     */
    public static String genToken(Map<String, Object> claims, long expireMillis) {
        if (claims == null || claims.isEmpty()) {
            throw new IllegalArgumentException("用户信息不能为空");
        }

        return JWT.create()
                .withClaim("claims", claims)
                .withExpiresAt(new Date(System.currentTimeMillis() + expireMillis))
                .withIssuedAt(new Date()) // 签发时间
                .sign(Algorithm.HMAC256(KEY));
    }

    /**
     * 解析和验证JWT Token
     * <p>
     * 验证Token的签名和有效期，通过后返回用户信息
     * <p>
     * 可能的异常：
     * - TokenExpiredException: Token已过期
     * - SignatureVerificationException: 签名验证失败
     * - JWTVerificationException: 其他验证失败
     *
     * @param token JWT Token字符串
     * @return 用户信息Map（包含id、username、role等）
     * @throws JWTVerificationException Token无效或已过期
     */
    public static Map<String, Object> parseToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            throw new IllegalArgumentException("Token不能为空");
        }

        try {
            DecodedJWT jwt = JWT.require(Algorithm.HMAC256(KEY))
                    .build()
                    .verify(token);
            return jwt.getClaim("claims").asMap();
        } catch (JWTVerificationException e) {
            // 统一转换为运行时异常，由全局异常处理器处理
            throw new RuntimeException("Token验证失败: " + e.getMessage(), e);
        }
    }

    /**
     * 验证Token是否有效（不抛出异常）
     * <p>
     * 适用于需要检查Token有效性但不想处理异常的场景
     *
     * @param token JWT Token字符串
     * @return true-有效，false-无效或已过期
     */
    public static boolean isTokenValid(String token) {
        try {
            parseToken(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}