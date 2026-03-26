package com.hzy.smartdiet.exception;

import com.hzy.smartdiet.common.ErrorCode;
import com.hzy.smartdiet.common.Result;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.stream.Collectors;

/**
 * 全局异常处理器
 * <p>
 * 统一捕获并处理以下异常类型：
 * <ul>
 *   <li>{@link BusinessException}  — 业务逻辑异常</li>
 *   <li>{@link MethodArgumentNotValidException} — @Valid 校验失败</li>
 *   <li>{@link ConstraintViolationException}    — @Validated 方法参数校验失败</li>
 *   <li>{@link BindException}                   — 表单绑定失败</li>
 *   <li>{@link IllegalArgumentException}        — 非法参数</li>
 *   <li>{@link Exception}                       — 其他未捕获异常</li>
 * </ul>
 * </p>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 业务异常 */
    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.OK)
    public Result<Void> handleBusinessException(BusinessException ex) {
        log.warn("[业务异常] code={}, message={}", ex.getCode(), ex.getMessage());
        return Result.fail(ex.getCode(), ex.getMessage());
    }

    /** @Valid / @RequestBody 校验失败 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.OK)
    public Result<Void> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        log.warn("[参数校验失败] {}", message);
        return Result.fail(ErrorCode.PARAM_ERROR.getCode(), message);
    }

    /** @Validated 方法参数校验失败 */
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.OK)
    public Result<Void> handleConstraintViolation(ConstraintViolationException ex) {
        String message = ex.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining("; "));
        log.warn("[约束校验失败] {}", message);
        return Result.fail(ErrorCode.PARAM_ERROR.getCode(), message);
    }

    /** 表单绑定异常 */
    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.OK)
    public Result<Void> handleBindException(BindException ex) {
        String message = ex.getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        log.warn("[绑定异常] {}", message);
        return Result.fail(ErrorCode.PARAM_ERROR.getCode(), message);
    }

    /** 缺少必要请求参数 */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.OK)
    public Result<Void> handleMissingParam(MissingServletRequestParameterException ex) {
        String message = "缺少必要参数: " + ex.getParameterName();
        log.warn("[缺少参数] {}", message);
        return Result.fail(ErrorCode.PARAM_ERROR.getCode(), message);
    }

    /** 参数类型不匹配 */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.OK)
    public Result<Void> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String message = String.format("参数 [%s] 类型错误，期望类型: %s",
                ex.getName(), ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown");
        log.warn("[类型不匹配] {}", message);
        return Result.fail(ErrorCode.PARAM_ERROR.getCode(), message);
    }

    /** 非法参数 */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.OK)
    public Result<Void> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("[非法参数] {}", ex.getMessage());
        return Result.fail(ErrorCode.PARAM_ERROR.getCode(), ex.getMessage());
    }

    /** 兜底：其他所有未捕获异常 */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleException(Exception ex) {
        log.error("[系统异常] {}", ex.getMessage(), ex);
        return Result.fail(ErrorCode.SYSTEM_ERROR);
    }
}
