package com.hzy.smartdiet.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * 饮食记录新增/更新请求 DTO
 */
@Data
@Schema(description = "饮食记录请求体")
public class DietRecordReqDTO {

    @NotBlank(message = "用户ID不能为空")
    @Schema(description = "用户唯一标识", requiredMode = Schema.RequiredMode.REQUIRED, example = "user_001")
    private String userId;

    @Schema(description = "记录日期（默认今天）", example = "2026-03-25")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate recordDate;

    @NotNull(message = "餐次类型不能为空")
    @Schema(description = "餐次：0=早餐 1=午餐 2=晚餐 3=加餐 4=夜宵", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    private Integer mealType;

    @NotBlank(message = "餐食描述不能为空")
    @Schema(description = "餐食描述（自然语言）", requiredMode = Schema.RequiredMode.REQUIRED,
            example = "早上吃了两个煎蛋、一碗小米粥、一根香蕉")
    private String mealDescription;

    @Schema(description = "餐食图片 URL（可选，用于图像识别）")
    private String imageUrl;

    /**
     * 是否立即触发 AI 分析（默认 true）。
     * 设为 false 时仅保存原始记录，稍后异步分析。
     */
    @Schema(description = "是否立即 AI 分析（默认 true）", defaultValue = "true")
    private boolean analyzeNow = true;
}
