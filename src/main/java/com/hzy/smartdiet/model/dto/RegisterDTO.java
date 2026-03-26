package com.hzy.smartdiet.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 用户注册请求 DTO
 */
@Data
@Schema(description = "注册请求体")
public class RegisterDTO {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 30, message = "用户名长度 3~30 位")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "用户名只允许字母、数字和下划线")
    @Schema(description = "用户名（登录凭证，唯一）", requiredMode = Schema.RequiredMode.REQUIRED, example = "hzy_user")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 50, message = "密码长度 6~50 位")
    @Schema(description = "登录密码（明文，后端 BCrypt 加密）", requiredMode = Schema.RequiredMode.REQUIRED, example = "Aa123456")
    private String password;

    @Schema(description = "昵称（默认与用户名相同）", example = "小明")
    private String nickname;

    @Schema(description = "性别：1=男 2=女", example = "1")
    private Integer gender;

    @Min(value = 10, message = "年龄不合法") @Max(value = 120, message = "年龄不合法")
    @Schema(description = "年龄（岁）", example = "25")
    private Integer age;

    @Schema(description = "身高（cm）", example = "175.0")
    private BigDecimal heightCm;

    @Schema(description = "体重（kg）", example = "68.5")
    private BigDecimal weightKg;

    @Schema(description = "活动水平：1=久坐 2=轻度 3=中度 4=高度 5=极高", defaultValue = "2")
    private Integer activityLevel;

    @Schema(description = "健康目标列表", example = "[\"减重\",\"增强体质\"]")
    private List<String> healthGoals;
}
