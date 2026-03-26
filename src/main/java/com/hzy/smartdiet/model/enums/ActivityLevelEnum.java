package com.hzy.smartdiet.model.enums;

import lombok.Getter;

/**
 * 活动水平枚举（用于 TDEE 计算）
 * <p>
 * TDEE = BMR × 活动系数
 * BMR 使用 Mifflin-St Jeor 公式计算
 * </p>
 */
@Getter
public enum ActivityLevelEnum {

    /** 久坐，几乎不运动 */
    SEDENTARY(1, "久坐", 1.2),
    /** 轻度活动，每周运动 1-3 天 */
    LIGHT(2, "轻度活动", 1.375),
    /** 中度活动，每周运动 3-5 天 */
    MODERATE(3, "中度活动", 1.55),
    /** 高度活动，每周运动 6-7 天 */
    ACTIVE(4, "高度活动", 1.725),
    /** 极高强度，每天高强度运动或体力劳动 */
    VERY_ACTIVE(5, "极高活动", 1.9);

    private final int code;
    private final String label;
    /** PAL 活动系数 */
    private final double factor;

    ActivityLevelEnum(int code, String label, double factor) {
        this.code = code;
        this.label = label;
        this.factor = factor;
    }

    public static ActivityLevelEnum fromCode(int code) {
        for (ActivityLevelEnum e : values()) {
            if (e.code == code) {
                return e;
            }
        }
        return SEDENTARY;
    }
}
