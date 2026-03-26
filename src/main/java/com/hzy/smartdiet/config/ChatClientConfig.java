package com.hzy.smartdiet.config;

import com.hzy.smartdiet.constant.PromptConstants;
import com.hzy.smartdiet.tools.HealthReminderTool;
import com.hzy.smartdiet.tools.NutritionAnalysisTool;
import com.hzy.smartdiet.tools.PdfReportTool;
import com.hzy.smartdiet.tools.TranslationTool;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring AI ChatClient 配置
 * <p>
 * 注册智能体的核心 ChatClient，并绑定：
 * <ul>
 *   <li>系统提示词（角色 & 能力描述）</li>
 *   <li>四个自定义 Tool（翻译、PDF报告、营养分析、健康提醒）</li>
 * </ul>
 * VectorStore 注入到 AgentService 中按需构建 RAG Advisor。
 * </p>
 */
@Configuration
public class ChatClientConfig {

    @Bean
    public ChatClient chatClient(
            ChatModel chatModel,
            TranslationTool translationTool,
            PdfReportTool pdfReportTool,
            NutritionAnalysisTool nutritionAnalysisTool,
            HealthReminderTool healthReminderTool) {

        return ChatClient.builder(chatModel)
                .defaultSystem(PromptConstants.SYSTEM_PROMPT)
                .defaultTools(
                        translationTool,
                        pdfReportTool,
                        nutritionAnalysisTool,
                        healthReminderTool
                )
                .build();
    }
}
