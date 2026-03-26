package com.hzy.smartdiet.common;

/**
 * 请求级别的用户上下文（ThreadLocal）
 * <p>
 * 由 {@link com.hzy.smartdiet.config.interceptor.AuthInterceptor} 在每次请求前写入，
 * 请求结束后清除，防止线程池污染。
 * 使用方式：{@code UserContext.getUserId()} 获取当前登录用户的业务 ID。
 * </p>
 */
public final class UserContext {

    private UserContext() {}

    private static final ThreadLocal<String> USER_ID_HOLDER = new ThreadLocal<>();

    public static void setUserId(String userId) {
        USER_ID_HOLDER.set(userId);
    }

    public static String getUserId() {
        return USER_ID_HOLDER.get();
    }

    public static void clear() {
        USER_ID_HOLDER.remove();
    }

    /** 与 {@link #clear()} 等价，保留此命名以兼容旧拦截器代码 */
    public static void remove() {
        USER_ID_HOLDER.remove();
    }
}
