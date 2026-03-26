package com.hzy.smartdiet.common;

import lombok.Getter;

/**
 * 业务错误码枚举
 * <p>
 * 编码规则：
 * <ul>
 *   <li>200xx  — 公共/通用错误</li>
 *   <li>201xx  — 用户/画像模块</li>
 *   <li>202xx  — 饮食记录模块</li>
 *   <li>203xx  — AI 智能体模块</li>
 *   <li>204xx  — RAG 知识库模块</li>
 * </ul>
 * </p>
 */
@Getter
public enum ErrorCode {

    // ---- 通用 ----
    SUCCESS(200, "操作成功"),
    PARAM_ERROR(20001, "请求参数错误"),
    UNAUTHORIZED(20002, "未授权，请先登录"),
    FORBIDDEN(20003, "无操作权限"),
    NOT_FOUND(20004, "资源不存在"),
    SYSTEM_ERROR(20005, "系统内部错误"),
    DATA_DUPLICATE(20006, "数据已存在，不可重复创建"),

    // ---- 用户画像模块 ----
    USER_NOT_FOUND(20101, "用户不存在"),
    USER_PROFILE_NOT_FOUND(20102, "用户画像不存在，请先完善个人信息"),

    // ---- 饮食记录模块 ----
    DIET_RECORD_NOT_FOUND(20201, "饮食记录不存在"),
    DIET_RECORD_DATE_INVALID(20202, "记录日期格式错误"),

    // ---- AI 智能体模块 ----
    AI_CHAT_ERROR(20301, "AI 对话服务异常，请稍后重试"),
    AI_CONTEXT_OVERFLOW(20302, "上下文长度超出限制"),
    CONVERSATION_NOT_FOUND(20303, "会话不存在或已过期"),

    // ---- 认证模块 ----
    USERNAME_EXISTS(20501, "用户名已被注册，请换一个"),
    USERNAME_NOT_FOUND(20502, "用户名不存在，请先注册"),
    PASSWORD_WRONG(20503, "密码错误，请重试"),
    TOKEN_INVALID(20504, "登录已过期或无效，请重新登录"),
    ACCOUNT_DISABLED(20505, "账号已被禁用，请联系管理员"),

    // ---- RAG 知识库 ----
    RAG_LOAD_ERROR(20401, "知识库加载失败"),
    RAG_SEARCH_ERROR(20402, "知识库检索失败");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
