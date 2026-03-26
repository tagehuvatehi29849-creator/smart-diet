package com.hzy.smartdiet.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户健康画像实体（对应 user_profile 表）
 * <p>
 * JSON 类型字段（healthGoals、dietaryPreferences 等）由 MyBatis-Plus
 * 配合 Hutool 的 JSONObject 自动序列化/反序列化（存为 VARCHAR/TEXT）
 * </p>
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName(value = "user_profile", autoResultMap = true)
@Schema(description = "用户健康画像")
public class UserProfile implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "主键 ID")
    private Long id;

    @TableField("user_id")
    @Schema(description = "用户唯一标识")
    private String userId;

    // ==================== 新增：登录与账号权限字段 ====================

    @TableField("username")
    @Schema(description = "登录账号(手机号或英文名)")
    private String username;

    /**
     * 登录密码 (BCrypt加密)
     * @JsonIgnore: 防止 SpringMVC 在返回 JSON 时把密文发给前端
     * hidden = true: 防止 Knife4j 接口文档中出现该字段
     */
    @TableField("password")
    @Schema(description = "登录密码(已加密)", hidden = true)
    @JsonIgnore
    private String password;

    @TableField("status")
    @Schema(description = "账号状态：1=正常 0=禁用")
    private Integer status;

    @TableField("last_login_at")
    @Schema(description = "上次登录时间")
    private LocalDateTime lastLoginAt;

    // ==================== 原有：健康与基础信息字段 ====================

    @TableField("nickname")
    @Schema(description = "昵称")
    private String nickname;

    @TableField("avatar_url")
    @Schema(description = "头像 URL")
    private String avatarUrl;

    /** 性别：0=未知 1=男 2=女 */
    @TableField("gender")
    @Schema(description = "性别：0=未知 1=男 2=女")
    private Integer gender;

    @TableField("age")
    @Schema(description = "年龄（岁）")
    private Integer age;

    @TableField("height_cm")
    @Schema(description = "身高（cm）")
    private BigDecimal heightCm;

    @TableField("weight_kg")
    @Schema(description = "体重（kg）")
    private BigDecimal weightKg;

    @TableField("target_weight_kg")
    @Schema(description = "目标体重（kg）")
    private BigDecimal targetWeightKg;

    /** 基础代谢率（由系统自动计算） */
    @TableField("bmr")
    @Schema(description = "基础代谢率 BMR（kcal/天）")
    private BigDecimal bmr;

    /** 每日总能量消耗（BMR × 活动系数） */
    @TableField("tdee")
    @Schema(description = "每日总能量消耗 TDEE（kcal/天）")
    private BigDecimal tdee;

    /** 活动水平：1=久坐 2=轻度 3=中度 4=高度 5=极高 */
    @TableField("activity_level")
    @Schema(description = "活动水平：1~5")
    private Integer activityLevel;

    /** 健康目标 JSON，存储为 TEXT，使用 @TableField 的 typeHandler 处理 */
    @TableField(value = "health_goals", typeHandler = com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler.class)
    @Schema(description = "健康目标列表，如[\"减重\",\"增肌\"]")
    private List<String> healthGoals;

    /** 饮食偏好 JSON */
    @TableField(value = "dietary_preferences", typeHandler = com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler.class)
    @Schema(description = "饮食偏好 JSON")
    private Object dietaryPreferences;

    /** 食物过敏/忌口 */
    @TableField(value = "food_allergies", typeHandler = com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler.class)
    @Schema(description = "食物过敏/忌口列表")
    private List<String> foodAllergies;

    /** 健康状况 */
    @TableField(value = "health_conditions", typeHandler = com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler.class)
    @Schema(description = "健康状况列表，如[\"高血压\",\"糖尿病\"]")
    private List<String> healthConditions;

    /** AI 归纳的长期记忆摘要 */
    @TableField("ai_profile_summary")
    @Schema(description = "AI 归纳的用户健康画像摘要（长期记忆）")
    private String aiProfileSummary;

    @TableLogic
    @TableField("deleted")
    @Schema(description = "逻辑删除标志", hidden = true)
    private Integer deleted;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}