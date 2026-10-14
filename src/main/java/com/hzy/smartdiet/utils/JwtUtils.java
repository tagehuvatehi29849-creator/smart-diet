package com.hzy.smartdiet.utils;

import cn.hutool.jwt.JWT;
import cn.hutool.jwt.JWTPayload;
import cn.hutool.jwt.JWTUtil;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT 认证凭证工具类 (基于 Hutool)
 */
public class JwtUtils {
    
    // 签名密钥：从环境变量 JWT_SECRET 读取，避免硬编码泄露到代码仓库
    private static final byte[] KEY = resolveKey();

    /**
     * 解析 JWT 签名密钥。优先读取环境变量 JWT_SECRET，其次读取系统属性 JWT_SECRET。
     * 两者都未配置时抛出异常，强制部署前显式设置密钥，避免落到弱默认值。
     */
    private static byte[] resolveKey() {
        String secret = System.getenv("JWT_SECRET");
        if (secret == null || secret.isBlank()) {
            secret = System.getProperty("JWT_SECRET");
        }
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "JWT 密钥未配置：请设置环境变量 JWT_SECRET（或系统属性 -DJWT_SECRET=...）");
        }
        return secret.getBytes(StandardCharsets.UTF_8);
    }
    
    // Token 过期时间：7天 (换算成毫秒)
    private static final long EXPIRE_MILLIS = 7L * 24 * 60 * 60 * 1000;

    /**
     * 为指定用户生成 JWT Token
     * @param userId 用户的全局唯一业务 ID
     * @return 生成的 Token 字符串
     */
    public static String createToken(String userId) {
        long nowMillis = System.currentTimeMillis();
        Date now = new Date(nowMillis);
        Date expireDate = new Date(nowMillis + EXPIRE_MILLIS);

        Map<String, Object> payload = new HashMap<>();
        payload.put(JWTPayload.ISSUED_AT, now); // 签发时间
        payload.put(JWTPayload.EXPIRES_AT, expireDate); // 过期时间
        
        // 存放我们的自定义载荷：用户唯一标识
        payload.put("userId", userId); 

        return JWTUtil.createToken(payload, KEY);
    }

    /**
     * 校验并解析 Token，提取其中的 userId
     * @param token 前端传来的 JWT 字符串
     * @return 如果校验失败、被篡改或已过期，则返回 null；否则返回 userId
     */
    public static String parseToken(String token) {
        try {
            // 1. 校验签名是否正确 (防篡改)
            boolean verify = JWTUtil.verify(token, KEY);
            if (!verify) {
                return null;
            }
            
            JWT jwt = JWTUtil.parseToken(token);
            
            // 2. 校验时间是否已过期 (允许 0 秒误差)
            if (!jwt.validate(0)) {
                return null;
            }
            
            // 3. 安全提取并返回载荷中的 userId
            return (String) jwt.getPayload("userId");
        } catch (Exception e) {
            // 解析过程中出现任何异常，都视为无效 token
            return null;
        }
    }
}