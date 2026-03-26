package com.hzy.smartdiet.service;

import com.hzy.smartdiet.model.dto.ChatRequestDTO;
import reactor.core.publisher.Flux;

/**
 * AI 智能体服务接口
 */
public interface AgentService {

    /**
     * 流式对话（SSE）
     * <p>
     * 内部流程：
     * <ol>
     *   <li>从 Redis 加载会话上下文（短期记忆）</li>
     *   <li>从 MySQL 加载用户画像（长期记忆）</li>
     *   <li>如启用 RAG，从 PgVector 检索相关知识片段</li>
     *   <li>构建完整 Prompt 并调用 DashScope 流式 API</li>
     *   <li>将本轮对话保存到 Redis</li>
     * </ol>
     *
     * @param request 对话请求
     * @return 流式文本 Flux（每个元素为一个文本增量片段）
     */
    Flux<String> streamChat(ChatRequestDTO request);

    /**
     * 为饮食记录触发 AI 分析（同步，返回完整分析结果 JSON 字符串）
     * <p>
     * 若提供 imageUrl（HTTP URL 或 data: Base64），将使用视觉模型识别图片中的食物。
     *
     * @param userId          用户ID
     * @param mealDescription 餐食描述（可为空，有图片时以图片为主）
     * @param mealType        餐次
     * @param imageUrl        图片 URL 或 Base64 Data URL（可为 null）
     * @return AI 分析结果 JSON 字符串
     */
    String analyzeDietRecord(String userId, String mealDescription, int mealType, String imageUrl);

    /**
     * 清除指定会话的上下文
     */
    void clearConversation(String conversationId);
}
