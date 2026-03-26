package com.hzy.smartdiet.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "用户登录请求参数")
public class LoginReqDTO {

    @NotBlank(message = "账号不能为空")
    @Schema(description = "登录账号")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Schema(description = "登录密码")
    private String password;
}