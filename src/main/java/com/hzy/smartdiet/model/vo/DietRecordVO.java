package com.hzy.smartdiet.model.vo;

import com.hzy.smartdiet.model.enums.MealTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 饮食记录视图 VO（返回给前端）
 */
@Data
@Schema(description = "饮食记录响应体")
public class DietRecordVO {

    @Schema(description = "记录 ID")
    private Long id;

    @Schema(description = "用户ID")
    private String userId;

    @Schema(description = "记录日期")
    private LocalDate recordDate;

    @Schema(description = "餐次代码")
    private Integer mealType;

    @Schema(description = "餐次名称，如：早餐")
    private String mealTypeLabel;

    @Schema(description = "原始餐食描述")
    private String mealDescription;

    @Schema(description = "AI 解析的食物列表")
    private List<String> foodItems;

    @Schema(description = "图片 URL")
    private String imageUrl;

    @Schema(description = "总热量（kcal）")
    private BigDecimal totalCalories;

    @Schema(description = "蛋白质（g）")
    private BigDecimal proteinG;

    @Schema(description = "碳水化合物（g）")
    private BigDecimal carbohydratesG;

    @Schema(description = "脂肪（g）")
    private BigDecimal fatG;

    @Schema(description = "膳食纤维（g）")
    private BigDecimal fiberG;

    @Schema(description = "AI 健康建议")
    private String aiSuggestion;

    @Schema(description = "健康评分（0-100）")
    private Integer healthScore;

    @Schema(description = "是否有异常警告")
    private Boolean isAbnormal;

    @Schema(description = "异常原因")
    private String abnormalReason;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    /** 从实体转换为 VO */
    public static DietRecordVO from(com.hzy.smartdiet.model.entity.DietRecord entity) {
        if (entity == null) return null;
        DietRecordVO vo = new DietRecordVO();
        vo.setId(entity.getId());
        vo.setUserId(entity.getUserId());
        vo.setRecordDate(entity.getRecordDate());
        vo.setMealType(entity.getMealType());
        vo.setMealTypeLabel(entity.getMealType() != null ? MealTypeEnum.labelOf(entity.getMealType()) : null);
        vo.setMealDescription(entity.getMealDescription());
        vo.setFoodItems(entity.getFoodItems());
        vo.setImageUrl(entity.getImageUrl());
        vo.setTotalCalories(entity.getTotalCalories());
        vo.setProteinG(entity.getProteinG());
        vo.setCarbohydratesG(entity.getCarbohydratesG());
        vo.setFatG(entity.getFatG());
        vo.setFiberG(entity.getFiberG());
        vo.setAiSuggestion(entity.getAiSuggestion());
        vo.setHealthScore(entity.getHealthScore());
        vo.setIsAbnormal(entity.getIsAbnormal() != null && entity.getIsAbnormal() == 1);
        vo.setAbnormalReason(entity.getAbnormalReason());
        vo.setCreatedAt(entity.getCreatedAt());
        return vo;
    }
}
