package com.hzy.smartdiet.model.vo;

import com.hzy.smartdiet.model.entity.UserProfile;
import com.hzy.smartdiet.model.enums.ActivityLevelEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户画像视图 VO
 */
@Data
@Schema(description = "用户画像响应体")
public class UserProfileVO {

    @Schema(description = "画像 ID")
    private Long id;

    @Schema(description = "用户ID")
    private String userId;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "头像 URL")
    private String avatarUrl;

    @Schema(description = "性别描述")
    private String genderLabel;

    @Schema(description = "年龄")
    private Integer age;

    @Schema(description = "身高（cm）")
    private BigDecimal heightCm;

    @Schema(description = "体重（kg）")
    private BigDecimal weightKg;

    @Schema(description = "目标体重（kg）")
    private BigDecimal targetWeightKg;

    @Schema(description = "BMI 值")
    private BigDecimal bmi;

    @Schema(description = "基础代谢率 BMR（kcal/天）")
    private BigDecimal bmr;

    @Schema(description = "每日总能量消耗 TDEE（kcal/天）")
    private BigDecimal tdee;

    @Schema(description = "活动水平描述")
    private String activityLevelLabel;

    @Schema(description = "健康目标列表")
    private List<String> healthGoals;

    @Schema(description = "饮食偏好")
    private Object dietaryPreferences;

    @Schema(description = "食物过敏/忌口")
    private List<String> foodAllergies;

    @Schema(description = "健康状况")
    private List<String> healthConditions;

    @Schema(description = "AI 健康画像摘要")
    private String aiProfileSummary;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;

    /** 从实体转换为 VO（自动计算 BMI 和活动水平标签） */
    public static UserProfileVO from(UserProfile entity) {
        if (entity == null) return null;
        UserProfileVO vo = new UserProfileVO();
        vo.setId(entity.getId());
        vo.setUserId(entity.getUserId());
        vo.setNickname(entity.getNickname());
        vo.setAvatarUrl(entity.getAvatarUrl());
        vo.setGenderLabel(genderLabel(entity.getGender()));
        vo.setAge(entity.getAge());
        vo.setHeightCm(entity.getHeightCm());
        vo.setWeightKg(entity.getWeightKg());
        vo.setTargetWeightKg(entity.getTargetWeightKg());
        vo.setBmi(calcBmi(entity.getHeightCm(), entity.getWeightKg()));
        vo.setBmr(entity.getBmr());
        vo.setTdee(entity.getTdee());
        vo.setActivityLevelLabel(entity.getActivityLevel() != null
                ? ActivityLevelEnum.fromCode(entity.getActivityLevel()).getLabel() : null);
        vo.setHealthGoals(entity.getHealthGoals());
        vo.setDietaryPreferences(entity.getDietaryPreferences());
        vo.setFoodAllergies(entity.getFoodAllergies());
        vo.setHealthConditions(entity.getHealthConditions());
        vo.setAiProfileSummary(entity.getAiProfileSummary());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }

    private static String genderLabel(Integer gender) {
        if (gender == null) return "未知";
        return switch (gender) {
            case 1 -> "男";
            case 2 -> "女";
            default -> "未知";
        };
    }

    private static BigDecimal calcBmi(BigDecimal heightCm, BigDecimal weightKg) {
        if (heightCm == null || weightKg == null || heightCm.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        // BMI = 体重(kg) / 身高(m)²
        BigDecimal heightM = heightCm.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
        return weightKg.divide(heightM.multiply(heightM), 1, RoundingMode.HALF_UP);
    }
}
