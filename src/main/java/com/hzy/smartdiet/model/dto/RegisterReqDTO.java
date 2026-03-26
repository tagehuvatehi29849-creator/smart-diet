package com.hzy.smartdiet.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "用户注册请求参数")
public class RegisterReqDTO {

    @NotBlank(message = "账号不能为空")
    @Schema(description = "登录账号", example = "hzy_admin")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Schema(description = "登录密码", example = "123456")
    private String password;

    @Schema(description = "用户昵称", example = "HZY")
    private String nickname;
}