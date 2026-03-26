package com.hzy.smartdiet.model.enums;

import lombok.Getter;

/**
 * 餐次类型枚举
 */
@Getter
public enum MealTypeEnum {

    BREAKFAST(0, "早餐"),
    LUNCH(1, "午餐"),
    DINNER(2, "晚餐"),
    SNACK(3, "加餐"),
    LATE_NIGHT(4, "夜宵");

    private final int code;
    private final String label;

    MealTypeEnum(int code, String label) {
        this.code = code;
        this.label = label;
    }

    public static MealTypeEnum fromCode(int code) {
        for (MealTypeEnum e : values()) {
            if (e.code == code) {
                return e;
            }
        }
        throw new IllegalArgumentException("未知餐次类型：" + code);
    }

    public static String labelOf(int code) {
        for (MealTypeEnum e : values()) {
            if (e.code == code) {
                return e.label;
            }
        }
        return "未知";
    }
}
