package com.hzy.smartdiet.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 每日饮食打卡记录实体（对应 diet_record 表）
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName(value = "diet_record", autoResultMap = true)
@Schema(description = "饮食打卡记录")
public class DietRecord implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "主键 ID")
    private Long id;

    @TableField("user_id")
    @Schema(description = "用户唯一标识")
    private String userId;

    @TableField("record_date")
    @Schema(description = "饮食记录日期")
    private LocalDate recordDate;

    /** 餐次：0=早餐 1=午餐 2=晚餐 3=加餐 4=夜宵 */
    @TableField("meal_type")
    @Schema(description = "餐次类型：0=早餐 1=午餐 2=晚餐 3=加餐 4=夜宵")
    private Integer mealType;

    @TableField("meal_description")
    @Schema(description = "用户输入的餐食原始描述")
    private String mealDescription;

    /** AI 解析后的食物列表，每条为一个可读字符串，如 "米饭（约150g）" */
    @TableField(value = "food_items", typeHandler = com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler.class)
    @Schema(description = "AI 解析后的食物列表")
    private List<String> foodItems;

    @TableField("image_url")
    @Schema(description = "餐食图片 URL（可选）")
    private String imageUrl;

    @TableField("total_calories")
    @Schema(description = "总热量（kcal）")
    private BigDecimal totalCalories;

    @TableField("protein_g")
    @Schema(description = "蛋白质（g）")
    private BigDecimal proteinG;

    @TableField("carbohydrates_g")
    @Schema(description = "碳水化合物（g）")
    private BigDecimal carbohydratesG;

    @TableField("fat_g")
    @Schema(description = "脂肪（g）")
    private BigDecimal fatG;

    @TableField("fiber_g")
    @Schema(description = "膳食纤维（g）")
    private BigDecimal fiberG;

    @TableField("sodium_mg")
    @Schema(description = "钠（mg）")
    private BigDecimal sodiumMg;

    /** AI 完整分析结果 JSON */
    @TableField("ai_analysis_result")
    @Schema(description = "AI 完整分析结果（JSON 字符串）")
    private String aiAnalysisResult;

    @TableField("ai_suggestion")
    @Schema(description = "AI 给出的健康建议")
    private String aiSuggestion;

    /** 本餐健康评分 0-100 */
    @TableField("health_score")
    @Schema(description = "健康评分（0-100）")
    private Integer healthScore;

    @TableField("is_abnormal")
    @Schema(description = "是否有饮食异常：0=正常 1=有异常")
    private Integer isAbnormal;

    @TableField("abnormal_reason")
    @Schema(description = "异常原因描述")
    private String abnormalReason;

    @TableLogic
    @TableField("deleted")
    private Integer deleted;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
