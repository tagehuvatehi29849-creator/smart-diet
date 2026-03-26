package com.hzy.smartdiet.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 营养成分分析工具（AI Tool）
 * <p>
 * 当 AI 需要查询某种食物的详细营养数据时，会自动调用此工具。
 * 数据来源：内置中国常见食物营养数据库（基于《中国食物成分表》）。
 * 生产环境可替换为第三方营养 API（如 Nutritionix、FatSecret 等）。
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NutritionAnalysisTool {

    private final ObjectMapper objectMapper;

    /** 内置常见食物营养数据（每100g）：{热量kcal, 蛋白质g, 碳水g, 脂肪g, 纤维g, 钠mg} */
    private static final Map<String, double[]> NUTRITION_DB = new HashMap<>();

    static {
        // 格式：食物名 → [热量, 蛋白质, 碳水, 脂肪, 纤维, 钠]
        NUTRITION_DB.put("米饭",           new double[]{116, 2.6, 25.6, 0.3, 0.3,  1.0});
        NUTRITION_DB.put("白米饭",         new double[]{116, 2.6, 25.6, 0.3, 0.3,  1.0});
        NUTRITION_DB.put("燕麦",           new double[]{379, 13.2, 67.7, 6.9, 10.6, 2.0});
        NUTRITION_DB.put("全麦面包",        new double[]{247, 8.5, 47.0, 3.4, 6.9, 480.0});
        NUTRITION_DB.put("馒头",           new double[]{221, 7.0, 46.1, 1.1, 1.3, 165.0});
        NUTRITION_DB.put("鸡蛋",           new double[]{155, 12.6, 0.6, 10.6, 0, 124.0});
        NUTRITION_DB.put("鸡胸肉",         new double[]{165, 31.0, 0, 3.6, 0, 74.0});
        NUTRITION_DB.put("猪里脊",         new double[]{143, 20.2, 0, 7.9, 0, 57.5});
        NUTRITION_DB.put("牛里脊",         new double[]{158, 26.1, 0, 5.3, 0, 57.0});
        NUTRITION_DB.put("三文鱼",         new double[]{208, 20.1, 0, 13.4, 0, 59.0});
        NUTRITION_DB.put("虾",             new double[]{99,  18.6, 0.9, 1.5, 0, 119.0});
        NUTRITION_DB.put("豆腐",           new double[]{76,  8.1, 3.8, 4.2, 0.4, 7.0});
        NUTRITION_DB.put("牛奶",           new double[]{61,  3.2, 4.8, 3.2, 0, 46.0});
        NUTRITION_DB.put("苹果",           new double[]{52,  0.3, 13.8, 0.2, 2.4, 1.0});
        NUTRITION_DB.put("香蕉",           new double[]{89,  1.1, 22.8, 0.3, 2.6, 1.0});
        NUTRITION_DB.put("橙子",           new double[]{47,  0.9, 11.8, 0.1, 2.4, 0.0});
        NUTRITION_DB.put("西蓝花",         new double[]{34,  2.8, 6.6, 0.4, 2.6, 33.0});
        NUTRITION_DB.put("菠菜",           new double[]{23,  2.9, 3.6, 0.4, 2.2, 79.0});
        NUTRITION_DB.put("胡萝卜",         new double[]{41,  0.9, 9.6, 0.2, 2.8, 69.0});
        NUTRITION_DB.put("土豆",           new double[]{77,  2.0, 17.5, 0.1, 2.2, 6.0});
        NUTRITION_DB.put("核桃",           new double[]{654, 15.2, 13.7, 65.2, 6.7, 2.0});
        NUTRITION_DB.put("花生",           new double[]{567, 25.8, 16.1, 49.2, 8.5, 18.0});
        NUTRITION_DB.put("可乐",           new double[]{41,  0,   10.6, 0,   0, 11.0});
        NUTRITION_DB.put("橙汁",           new double[]{45,  0.7, 10.4, 0.2, 0.2, 1.0});
    }

    /**
     * 查询指定食物的营养成分（每100g）
     *
     * @param foodName 食物名称（中文），如"鸡胸肉"、"燕麦"
     * @return JSON 字符串，包含热量和主要营养素数据；若未找到则给出提示
     */
    @Tool(description = "查询指定食物每100g的营养成分，包括热量(kcal)、蛋白质(g)、碳水化合物(g)、脂肪(g)、膳食纤维(g)、钠(mg)。当需要精确营养数据时调用此工具。")
    public String queryNutrition(String foodName) {
        if (foodName == null || foodName.isBlank()) {
            return "{\"error\": \"食物名称不能为空\"}";
        }

        // 模糊匹配：尝试找包含关键词的食物
        double[] data = findNutrition(foodName.trim());

        if (data == null) {
            return String.format("""
                    {"food": "%s", "found": false, "message": "未找到该食物的营养数据，请尝试更通用的名称（如'鸡肉'而非'清蒸鸡'）"}
                    """, foodName);
        }

        try {
            Map<String, Object> result = new HashMap<>();
            result.put("food", foodName);
            result.put("per100g", true);
            result.put("calories_kcal", data[0]);
            result.put("protein_g", data[1]);
            result.put("carbohydrates_g", data[2]);
            result.put("fat_g", data[3]);
            result.put("fiber_g", data[4]);
            result.put("sodium_mg", data[5]);
            result.put("found", true);
            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            log.error("[NutritionTool] JSON 序列化失败", e);
            return String.format("{\"food\":\"%s\",\"calories_kcal\":%.1f,\"protein_g\":%.1f,\"carbohydrates_g\":%.1f,\"fat_g\":%.1f}",
                    foodName, data[0], data[1], data[2], data[3]);
        }
    }

    /**
     * 根据食物重量估算营养摄入
     *
     * @param foodName    食物名称
     * @param amountGrams 重量（克）
     * @return 按实际重量计算的营养数据 JSON
     */
    @Tool(description = "根据食物名称和重量（克），精确计算实际营养摄入量。例如：计算吃了200g米饭的热量。")
    public String calculateNutritionByAmount(String foodName, double amountGrams) {
        if (amountGrams <= 0) return "{\"error\": \"重量必须大于0\"}";

        double[] per100g = findNutrition(foodName);
        if (per100g == null) {
            return String.format("{\"food\":\"%s\",\"found\":false,\"message\":\"未找到营养数据\"}", foodName);
        }

        double factor = amountGrams / 100.0;
        try {
            Map<String, Object> result = new HashMap<>();
            result.put("food", foodName);
            result.put("amount_g", amountGrams);
            result.put("calories_kcal", Math.round(per100g[0] * factor * 10.0) / 10.0);
            result.put("protein_g",     Math.round(per100g[1] * factor * 10.0) / 10.0);
            result.put("carbohydrates_g", Math.round(per100g[2] * factor * 10.0) / 10.0);
            result.put("fat_g",         Math.round(per100g[3] * factor * 10.0) / 10.0);
            result.put("fiber_g",       Math.round(per100g[4] * factor * 10.0) / 10.0);
            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            return "{\"error\":\"计算失败\"}";
        }
    }

    private double[] findNutrition(String name) {
        // 精确匹配
        if (NUTRITION_DB.containsKey(name)) return NUTRITION_DB.get(name);
        // 模糊匹配（食物名包含查询词，或查询词包含食物名）
        for (Map.Entry<String, double[]> entry : NUTRITION_DB.entrySet()) {
            if (name.contains(entry.getKey()) || entry.getKey().contains(name)) {
                return entry.getValue();
            }
        }
        return null;
    }
}
