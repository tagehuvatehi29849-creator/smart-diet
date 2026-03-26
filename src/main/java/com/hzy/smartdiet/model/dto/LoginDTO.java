package com.hzy.smartdiet.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 用户登录请求 DTO
 */
@Data
@Schema(description = "登录请求体")
public class LoginDTO {

    @NotBlank(message = "用户名不能为空")
    @Schema(description = "用户名", requiredMode = Schema.RequiredMode.REQUIRED, example = "hzy_user")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Schema(description = "登录密码（明文）", requiredMode = Schema.RequiredMode.REQUIRED, example = "Aa123456")
    private String password;

    @Schema(description = "记住我（true=Token 有效期 30 天，false=7 天）", defaultValue = "false")
    private boolean rememberMe;
}
