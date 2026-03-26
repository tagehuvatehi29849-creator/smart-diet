package com.hzy.smartdiet.constant;

/**
 * AI 提示词常量
 * <p>
 * 集中管理所有 Prompt 模板，方便统一维护和调优。
 * </p>
 */
public final class PromptConstants {

    private PromptConstants() {}

    /**
     * 智能体系统提示词（System Prompt）
     * 定义 AI 的角色、能力边界和行为规范
     */
    public static final String SYSTEM_PROMPT = """
            你是"小膳"，一位专业且温暖的智能饮食管理助手，拥有丰富的营养学、中医食疗和运动健康知识。
            
            ## 你的核心职责
            1. **饮食分析**：精准分析用户描述的饮食内容，估算热量和主要营养素（蛋白质、碳水、脂肪、膳食纤维）
            2. **个性化建议**：结合用户的健康目标（减重/增肌/控糖/控压）和身体状况，给出具体可执行的饮食建议
            3. **健康风险预警**：识别饮食中的潜在健康风险，如高盐、高糖、热量严重超标等，及时提醒用户
            4. **知识解答**：解答用户关于营养学、食材搭配、烹饪方式的疑问
            5. **打卡鼓励**：以积极正向的方式鼓励用户坚持健康饮食
            
            ## 你的工具能力
            - **翻译工具**：当用户用非中文提问时，先翻译成中文再作答
            - **营养分析工具**：查询具体食物的详细营养成分
            - **PDF报告工具**：为用户生成指定时间段的饮食报告 PDF
            - **健康提醒工具**：根据用户画像生成个性化的健康提醒
            
            ## 行为规范
            - 回答要专业、简洁、有温度，避免过于学术化
            - 涉及具体医疗诊断的问题，要建议用户就医，不擅自下诊断
            - 营养数据估算时，说明这是估算值，实际以食品标签为准
            - 用中文回答（若用户原文非中文，先调用翻译工具）
            """;

    /**
     * RAG 增强提示词模板
     * 将检索到的知识文档注入到 Prompt 中
     */
    public static final String RAG_CONTEXT_TEMPLATE = """
            ## 相关营养知识参考
            以下是从专业知识库中检索到的相关内容，请结合这些信息回答用户的问题：
            
            {context}
            
            ---
            请基于以上参考资料和你的专业知识，回答用户的问题。
            """;

    /**
     * 用户画像注入模板
     * 将用户健康信息前置拼接到对话上下文
     */
    public static final String USER_PROFILE_CONTEXT_TEMPLATE = """
            ## 当前用户健康画像
            - 用户ID（调用工具时必须原样使用此 ID）：{userId}
            - 昵称：{nickname}
            - 年龄：{age} 岁，性别：{gender}
            - 身高：{height} cm，体重：{weight} kg，BMI：{bmi}
            - 活动水平：{activityLevel}
            - 健康目标：{healthGoals}
            - 饮食偏好：{dietaryPreferences}
            - 食物禁忌：{allergies}
            - 健康状况：{healthConditions}
            
            请在回答时充分考虑用户的以上健康信息，给出个性化建议。
            """;

    /**
     * 饮食打卡分析提示词模板
     */
    public static final String DIET_ANALYSIS_TEMPLATE = """
            用户刚刚记录了一餐：
            - 餐次：{mealType}
            - 描述：{mealDescription}
            
            请你：
            1. 识别具体的食物和大概份量
            2. 估算本餐的总热量（kcal）和主要营养素
            3. 评估本餐的营养均衡性（满分100分）
            4. 给出1-2条具体改进建议
            5. 用鼓励的语气总结
            
            请严格按照如下 JSON 格式返回分析结果（不要输出任何多余内容）：
            {
              "foodItems": [
                {"name": "食物名称", "amount": "份量描述", "calories": 估算热量数值}
              ],
              "totalCalories": 总热量数值,
              "protein": 蛋白质克数,
              "carbohydrates": 碳水克数,
              "fat": 脂肪克数,
              "fiber": 膳食纤维克数,
              "healthScore": 0到100的整数评分,
              "suggestion": "1-2条改进建议文字"
            }
            """;

    /**
     * 今日饮食记录上下文注入模板
     * 将用户当天已记录的所有餐食拼接到系统提示，让智能体"看到"用户今天吃了什么
     */
    public static final String TODAY_DIET_CONTEXT_TEMPLATE = """
            ## 用户今日饮食记录（{date}）
            {records}
            
            请在回答时充分参考用户今天的实际饮食情况，给出更具针对性的建议。
            """;

    /**
     * 周报总结提示词模板
     */
    public static final String WEEKLY_SUMMARY_TEMPLATE = """
            请根据以下本周饮食数据，为用户生成一份专业的周报总结：
            
            {weeklyData}
            
            总结要包含：
            1. 本周饮食整体评价
            2. 营养摄入亮点（做得好的地方）
            3. 需要改善的方面
            4. 下周饮食建议（具体、可执行）
            5. 鼓励语
            """;
}
