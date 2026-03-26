package com.hzy.smartdiet.tools;

import com.hzy.smartdiet.model.entity.UserProfile;
import com.hzy.smartdiet.model.enums.ActivityLevelEnum;
import com.hzy.smartdiet.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 健康提醒工具（AI Tool）
 * <p>
 * 根据用户的健康画像（BMI、TDEE、目标等）和当前时间，
 * 动态生成个性化的健康提醒，AI 可在对话中适时调用。
 * 包含以下功能：
 * <ul>
 *   <li>BMI 评估与体重管理建议</li>
 *   <li>每日热量配额提醒</li>
 *   <li>基于时间的饮食/运动/喝水提醒</li>
 *   <li>健康风险预警（如高血压、糖尿病相关）</li>
 * </ul>
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HealthReminderTool {

    private final UserProfileService userProfileService;

    /**
     * 获取用户个性化健康提醒
     *
     * @param userId 用户唯一标识
     * @return 格式化的健康提醒文本（多条提醒以换行分隔）
     */
    @Tool(description = "根据用户健康画像生成个性化健康提醒，包含 BMI 评估、每日热量目标、饮食建议和生活方式提醒。在用户询问健康建议、今日怎么吃、需要注意什么时调用。")
    public String getHealthReminders(String userId) {
        UserProfile profile = userProfileService.getByUserId(userId);
        if (profile == null) {
            return "⚠️ 未找到您的健康画像，建议先完善个人健康信息（身高、体重、健康目标等），以获得更精准的个性化建议。";
        }

        List<String> reminders = new ArrayList<>();
        LocalTime now = LocalTime.now();

        // ---- BMI 评估 ----
        if (profile.getHeightCm() != null && profile.getWeightKg() != null) {
            BigDecimal heightM = profile.getHeightCm().divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
            BigDecimal bmi = profile.getWeightKg().divide(heightM.multiply(heightM), 1, RoundingMode.HALF_UP);
            double bmiVal = bmi.doubleValue();

            String bmiStatus;
            String bmiAdvice;
            if (bmiVal < 18.5) {
                bmiStatus = "偏瘦（BMI " + bmi + "）";
                bmiAdvice = "建议适当增加优质蛋白和碳水摄入，结合力量训练，健康增重。";
            } else if (bmiVal < 24.0) {
                bmiStatus = "正常（BMI " + bmi + "）";
                bmiAdvice = "保持均衡饮食和规律运动，继续维持健康体重！";
            } else if (bmiVal < 28.0) {
                bmiStatus = "超重（BMI " + bmi + "）";
                bmiAdvice = "建议减少精制碳水和高脂食物摄入，每周保持 150 分钟中等强度运动。";
            } else {
                bmiStatus = "肥胖（BMI " + bmi + "）";
                bmiAdvice = "建议在专业指导下制定减重计划，控制热量缺口 500 kcal/天，循序渐进。";
            }
            reminders.add("📊 体重状态：" + bmiStatus + " — " + bmiAdvice);
        }

        // ---- 每日热量目标 ----
        if (profile.getTdee() != null) {
            double tdee = profile.getTdee().doubleValue();
            double target = tdee;

            // 根据健康目标调整热量目标
            if (profile.getHealthGoals() != null) {
                if (profile.getHealthGoals().stream().anyMatch(g -> g.contains("减重") || g.contains("减脂"))) {
                    target = tdee - 400;
                    reminders.add("🎯 今日热量目标：约 " + Math.round(target) + " kcal（TDEE " + Math.round(tdee) + " - 400 kcal 缺口）");
                } else if (profile.getHealthGoals().stream().anyMatch(g -> g.contains("增肌") || g.contains("增重"))) {
                    target = tdee + 300;
                    reminders.add("🎯 今日热量目标：约 " + Math.round(target) + " kcal（TDEE " + Math.round(tdee) + " + 300 kcal 盈余）");
                } else {
                    reminders.add("🎯 今日热量目标：约 " + Math.round(target) + " kcal（维持体重）");
                }
            } else {
                reminders.add("🎯 今日热量目标：约 " + Math.round(target) + " kcal");
            }

            // 三餐热量建议分配
            reminders.add("📅 热量分配参考：早餐 " + Math.round(target * 0.30) + " kcal | 午餐 " + Math.round(target * 0.40) + " kcal | 晚餐 " + Math.round(target * 0.30) + " kcal");
        }

        // ---- 基于时间的提醒 ----
        if (now.isBefore(LocalTime.of(9, 0))) {
            reminders.add("🌅 早安！记得吃早餐，早餐建议包含蛋白质（鸡蛋/牛奶）+ 复合碳水（燕麦/全麦面包），为一天提供稳定能量。");
        } else if (now.isBefore(LocalTime.of(12, 0))) {
            reminders.add("☀️ 上午好！记得喝水，建议上午喝 500-600ml 水，保持身体水分充足。");
        } else if (now.isBefore(LocalTime.of(14, 0))) {
            reminders.add("🍱 午饭时间！午餐是一天热量摄入的核心，建议荤素搭配，多选蔬菜，主食选复合碳水。");
        } else if (now.isBefore(LocalTime.of(18, 0))) {
            reminders.add("🌤️ 下午好！如感到饥饿可以适量加餐，推荐：一小把坚果（约 15g）或一个水果。");
        } else if (now.isBefore(LocalTime.of(20, 0))) {
            reminders.add("🌙 晚餐建议清淡为主，减少高脂高热食物，可多选蔬菜和优质蛋白。");
        } else {
            reminders.add("🌙 已到晚间，避免夜宵或高热量零食，若有饥饿感可喝一杯温牛奶或吃少量坚果。");
        }

        // ---- 饮水提醒 ----
        reminders.add("💧 每日饮水目标：约 " + calcWaterIntake(profile) + " ml，请均匀分配全天饮水量。");

        // ---- 运动提醒 ----
        if (profile.getActivityLevel() != null && profile.getActivityLevel() <= 2) {
            reminders.add("🏃 检测到您的活动水平较低，建议每天增加 20-30 分钟有氧运动（散步、快走、骑行），有助于改善代谢和情绪。");
        }

        // ---- 健康风险提醒 ----
        if (profile.getHealthConditions() != null) {
            if (profile.getHealthConditions().contains("高血压")) {
                reminders.add("⚠️ 高血压注意：今日钠摄入控制在 1500mg 以内，避免腌制食品、加工食品和外卖重口味菜肴。");
            }
            if (profile.getHealthConditions().contains("糖尿病") || profile.getHealthConditions().contains("血糖偏高")) {
                reminders.add("⚠️ 血糖管理：避免空腹和餐后血糖大幅波动，每餐碳水控制在 45-60g，优先选低GI食物。");
            }
            if (profile.getHealthConditions().contains("高血脂") || profile.getHealthConditions().contains("高胆固醇")) {
                reminders.add("⚠️ 血脂管理：减少饱和脂肪和反式脂肪摄入，增加膳食纤维，可适量食用深海鱼（富含Omega-3）。");
            }
        }

        return String.join("\n", reminders);
    }

    /**
     * 计算推荐每日饮水量（ml）
     * 基于体重：约 30-35 ml/kg，高活动量增加
     */
    private int calcWaterIntake(UserProfile profile) {
        double baseWeight = profile.getWeightKg() != null ? profile.getWeightKg().doubleValue() : 60.0;
        double baseWater = baseWeight * 33; // 33ml/kg
        int level = profile.getActivityLevel() != null ? profile.getActivityLevel() : 1;
        if (level >= 4) baseWater += 500; // 高活动量多补
        return (int) Math.round(baseWater / 100) * 100;
    }
}
