package com.hzy.smartdiet.config.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hzy.smartdiet.common.ErrorCode;
import com.hzy.smartdiet.common.Result;
import com.hzy.smartdiet.common.UserContext;
import com.hzy.smartdiet.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Token 认证拦截器
 * <p>
 * 委托 {@link AuthService#getUserIdByToken} 做 Token 校验，
 * 与 Token 格式（UUID / JWT）解耦，后续切换只需改 AuthService。
 * 路径匹配规则在 {@link com.hzy.smartdiet.config.WebMvcConfig} 中配置。
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final AuthService    authService;
    private final ObjectMapper   objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {

        String token = extractToken(request);

        if (!StringUtils.hasText(token)) {
            return reject(response, ErrorCode.TOKEN_INVALID.getCode(), "请求未携带 Token，请先登录");
        }

        String userId = authService.getUserIdByToken(token);
        if (userId == null) {
            return reject(response, ErrorCode.TOKEN_INVALID.getCode(), "登录已过期或无效，请重新登录");
        }

        // 将 userId 注入线程上下文，供 Controller / Service 使用
        UserContext.setUserId(userId);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        // 请求结束后必须清除，防止线程池线程复用导致上下文污染
        UserContext.remove();
    }

    // ---- 私有工具方法 ----

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            return header.substring(7).trim();
        }
        return null;
    }

    private boolean reject(HttpServletResponse response, int code, String msg) throws Exception {
        response.setStatus(HttpServletResponse.SC_OK);  // 统一 200，业务码区分
        response.setContentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8");
        Result<Void> result = Result.fail(code, msg);
        response.getWriter().write(objectMapper.writeValueAsString(result));
        return false;
    }
}
