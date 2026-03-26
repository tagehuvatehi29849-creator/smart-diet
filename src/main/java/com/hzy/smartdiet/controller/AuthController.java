package com.hzy.smartdiet.controller;

import com.hzy.smartdiet.common.Result;
import com.hzy.smartdiet.common.UserContext;
import com.hzy.smartdiet.model.dto.LoginDTO;
import com.hzy.smartdiet.model.dto.RegisterDTO;
import com.hzy.smartdiet.model.vo.LoginVO;
import com.hzy.smartdiet.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 认证控制器（公开接口，不需要 Token）
 * <p>
 * 所有路径以 /v1/auth/ 开头，由 AuthInterceptor 放行。
 * </p>
 */
@Validated
@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
@Tag(name = "认证接口", description = "注册 / 登录 / 退出 / 修改密码")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "注册", description = "创建账号并自动登录，返回 Token 和用户信息")
    public Result<LoginVO> register(@RequestBody @Valid RegisterDTO dto) {
        return Result.success("注册成功", authService.register(dto));
    }

    @PostMapping("/login")
    @Operation(summary = "登录", description = "用户名 + 密码登录，返回访问 Token")
    public Result<LoginVO> login(@RequestBody @Valid LoginDTO dto) {
        return Result.success("登录成功", authService.login(dto));
    }

    @PostMapping("/logout")
    @Operation(summary = "退出登录", description = "销毁当前 Token，前端同步清除本地存储")
    public Result<Void> logout(HttpServletRequest request) {
        String token = extractToken(request);
        authService.logout(token);
        return Result.success("已退出登录", null);
    }

    @PostMapping("/change-password")
    @Operation(summary = "修改密码", description = "需要登录态（Token 有效）")
    public Result<Void> changePassword(
            @RequestBody Map<String, String> body) {
        String userId      = UserContext.getUserId();
        String oldPassword = body.get("oldPassword");
        String newPassword = body.get("newPassword");
        authService.changePassword(userId, oldPassword, newPassword);
        return Result.success("密码修改成功", null);
    }

    @GetMapping("/info")
    @Operation(summary = "获取当前登录用户信息", description = "根据 Token 返回 userId 等基础信息")
    public Result<Map<String, String>> info() {
        String userId = UserContext.getUserId();
        return Result.success(Map.of("userId", userId));
    }

    // ---- 工具方法 ----
    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}
