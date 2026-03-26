package com.hzy.smartdiet.constant;

/**
 * Redis Key 常量
 * <p>
 * 命名规范：{业务前缀}:{模块}:{唯一标识}
 * 所有 Key 统一以 "smart-diet:" 开头，便于 Redis CLI 查询和 key-space 隔离
 * </p>
 */
public final class RedisKeyConstants {

    private RedisKeyConstants() {}

    /** 项目全局前缀 */
    private static final String PREFIX = "smart-diet:";

    // ==========================================
    // 对话上下文（短期记忆）
    // ==========================================

    /** 对话历史 List：smart-diet:chat:context:{conversationId} */
    public static final String CHAT_CONTEXT_PREFIX = PREFIX + "chat:context:";

    /** 对话用户映射：smart-diet:chat:user:{userId} → conversationId */
    public static final String CHAT_USER_CONV_PREFIX = PREFIX + "chat:user:";

    // ==========================================
    // 用户画像缓存
    // ==========================================

    /** 用户画像缓存：smart-diet:profile:{userId} */
    public static final String USER_PROFILE_PREFIX = PREFIX + "profile:";

    // ==========================================
    // 饮食记录缓存
    // ==========================================

    /** 当日饮食记录缓存：smart-diet:diet:daily:{userId}:{date} */
    public static final String DIET_DAILY_PREFIX = PREFIX + "diet:daily:";

    /** 周报缓存：smart-diet:diet:weekly:{userId}:{weekStartDate} */
    public static final String DIET_WEEKLY_PREFIX = PREFIX + "diet:weekly:";

    // ==========================================
    // 认证 Token
    // ==========================================

    /**
     * 登录 Token：smart-diet:auth:token:{token}
     * Value 为 JSON Map，包含 userId / username / nickname
     */
    public static final String AUTH_TOKEN_PREFIX = PREFIX + "auth:token:";

    public static String authTokenKey(String token) {
        return AUTH_TOKEN_PREFIX + token;
    }

    // ==========================================
    // 限流 / 防刷
    // ==========================================

    /** API 频率限制：smart-diet:rate:{userId} */
    public static final String RATE_LIMIT_PREFIX = PREFIX + "rate:";

    // ==========================================
    // Key 构建工具方法
    // ==========================================

    public static String chatContextKey(String conversationId) {
        return CHAT_CONTEXT_PREFIX + conversationId;
    }

    public static String userProfileKey(String userId) {
        return USER_PROFILE_PREFIX + userId;
    }

    public static String dietDailyKey(String userId, String date) {
        return DIET_DAILY_PREFIX + userId + ":" + date;
    }
}
