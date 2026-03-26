package com.hzy.smartdiet.exception;

import com.hzy.smartdiet.common.ErrorCode;
import lombok.Getter;

/**
 * 业务异常
 * <p>
 * 用于主动抛出业务逻辑错误，由 GlobalExceptionHandler 统一捕获并返回标准响应。
 * </p>
 */
@Getter
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    public BusinessException(ErrorCode errorCode, String detail) {
        super(errorCode.getMessage() + "：" + detail);
        this.code = errorCode.getCode();
    }
}
