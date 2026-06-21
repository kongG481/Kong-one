package com.example.utils;

import org.springframework.util.StringUtils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Spring Boot 集成版 加盐MD5加密工具类
 * 相比纯MD5更安全，防止彩虹表破解
 */
public class Md5Util {
    // 加密算法
    private static final String ALGORITHM = "MD5";
    // 盐值长度（推荐16位）
    private static final int SALT_LENGTH = 16;
    // 加密次数（增加破解难度）
    private static final int HASH_ITERATIONS = 1024;

    /**
     * 生成带盐值的MD5加密字符串（加密+加盐）
     * @param password 明文密码
     * @return 加密后的字符串（格式：盐值:加密密码）
     */
    public static String encrypt(String password) {
        if (!StringUtils.hasText(password)) {
            throw new IllegalArgumentException("密码不能为空");
        }

        // 1. 生成随机盐值
        byte[] salt = generateSalt();
        // 2. 加盐加密
        byte[] encryptedBytes = md5WithSalt(password.getBytes(), salt);
        // 3. 盐值和加密密码拼接（Base64编码便于存储）
        String saltStr = Base64.getEncoder().encodeToString(salt);
        String encryptedStr = Base64.getEncoder().encodeToString(encryptedBytes);
        return saltStr + ":" + encryptedStr;
    }

    /**
     * 验证密码是否正确（对比明文密码+盐值 与 加密后的密码）
     * @param password 明文密码
     * @param encryptedPassword 加密后的密码（格式：盐值:加密密码）
     * @return 是否匹配
     */
    public static boolean verify(String password, String encryptedPassword) {
        if (!StringUtils.hasText(password) || !StringUtils.hasText(encryptedPassword)) {
            return false;
        }

        // 1. 拆分盐值和加密密码
        String[] parts = encryptedPassword.split(":");
        if (parts.length != 2) {
            return false;
        }

        try {
            // 2. 解码盐值
            byte[] salt = Base64.getDecoder().decode(parts[0]);
            // 3. 明文密码加盐加密
            byte[] encryptedBytes = md5WithSalt(password.getBytes(), salt);
            String encryptedStr = Base64.getEncoder().encodeToString(encryptedBytes);
            // 4. 对比
            return encryptedStr.equals(parts[1]);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 生成随机盐值（使用安全随机数生成器）
     */
    private static byte[] generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[SALT_LENGTH];
        random.nextBytes(salt);
        return salt;
    }

    /**
     * 加盐+多次MD5加密
     */
    private static byte[] md5WithSalt(byte[] passwordBytes, byte[] salt) {
        try {
            MessageDigest md = MessageDigest.getInstance(ALGORITHM);
            // 混入盐值
            md.update(salt);
            byte[] digest = md.digest(passwordBytes);

            // 多次加密，增加破解难度
            for (int i = 0; i < HASH_ITERATIONS; i++) {
                md.reset();
                digest = md.digest(digest);
            }
            return digest;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5算法不支持", e);
        }
    }

    // ====================== 兼容原有基础MD5方法（可选保留） ======================
    /**
     * 基础MD5加密（无盐值，仅用于兼容旧逻辑）
     * @param s 明文
     * @return 加密字符串
     */
    public static String getMD5String(String s) {
        if (!StringUtils.hasText(s)) {
            return "";
        }
        try {
            MessageDigest md = MessageDigest.getInstance(ALGORITHM);
            byte[] digest = md.digest(s.getBytes());
            return bufferToHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5算法不支持", e);
        }
    }

    // 字节数组转16进制字符串（基础MD5用）
    private static String bufferToHex(byte[] bytes) {
        char[] hexDigits = {'0','1','2','3','4','5','6','7','8','9','a','b','c','d','e','f'};
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(hexDigits[(b & 0xf0) >> 4]);
            sb.append(hexDigits[b & 0x0f]);
        }
        return sb.toString();
    }
}