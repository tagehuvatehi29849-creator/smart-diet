package com.hzy.smartdiet.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 用户画像新增/更新请求 DTO
 */
@Data
@Schema(description = "用户画像请求体")
public class UserProfileDTO {
    private Integer status;

    @NotBlank(message = "用户ID不能为空")
    @Schema(description = "用户唯一标识", requiredMode = Schema.RequiredMode.REQUIRED)
    private String userId;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "头像 URL")
    private String avatarUrl;

    @Schema(description = "性别：0=未知 1=男 2=女", example = "1")
    private Integer gender;

    @Min(value = 1, message = "年龄必须大于0") @Max(value = 150, message = "年龄不能超过150")
    @Schema(description = "年龄（岁）", example = "28")
    private Integer age;

    @Schema(description = "身高（cm）", example = "175.0")
    private BigDecimal heightCm;

    @Schema(description = "体重（kg）", example = "70.0")
    private BigDecimal weightKg;

    @Schema(description = "目标体重（kg）", example = "65.0")
    private BigDecimal targetWeightKg;

    @Min(value = 1, message = "活动水平取值 1-5") @Max(value = 5, message = "活动水平取值 1-5")
    @Schema(description = "活动水平：1=久坐 2=轻度 3=中度 4=高度 5=极高", example = "2")
    private Integer activityLevel;

    @Schema(description = "健康目标列表，如[\"减重\",\"增肌\"]")
    private List<String> healthGoals;

    @Schema(description = "饮食偏好（JSON 对象，自定义结构）")
    private Object dietaryPreferences;

    @Schema(description = "食物过敏/忌口列表")
    private List<String> foodAllergies;

    @Schema(description = "健康状况列表，如[\"高血压\"]")
    private List<String> healthConditions;
}
