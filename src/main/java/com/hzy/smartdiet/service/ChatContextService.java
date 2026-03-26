package com.hzy.smartdiet.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hzy.smartdiet.constant.RedisKeyConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 对话上下文服务（短期记忆）
 * <p>
 * 使用 Redis List 存储每个会话的历史消息。
 * 数据结构：
 * <pre>
 *   Key:  smart-diet:chat:context:{conversationId}
 *   Type: List
 *   Item: JSON 字符串 {"role":"user"|"assistant", "content":"..."}
 * </pre>
 * 每次对话后通过 LTRIM 保留最新的 maxTurns*2 条消息（1轮=1用户+1助手）。
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatContextService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    /** 保留的最大轮数（1轮 = user + assistant 各1条） */
    @Value("${app.chat.context.max-turns:5}")
    private int maxTurns;

    /** Redis Key TTL（秒），默认 24 小时 */
    @Value("${app.chat.context.ttl-seconds:86400}")
    private long ttlSeconds;

    // ---- 写操作 ----

    /**
     * 追加一条用户消息到 Redis
     */
    public void appendUserMessage(String conversationId, String content) {
        append(conversationId, "user", content);
    }

    /**
     * 追加一条 AI 回复到 Redis
     */
    public void appendAssistantMessage(String conversationId, String content) {
        append(conversationId, "assistant", content);
    }

    /**
     * 同时追加用户消息和 AI 回复（完整一轮），并裁剪旧记录
     */
    public void appendTurn(String conversationId, String userContent, String assistantContent) {
        appendUserMessage(conversationId, userContent);
        appendAssistantMessage(conversationId, assistantContent);
        trim(conversationId);
    }

    // ---- 读操作 ----

    /**
     * 读取最近 N 轮对话，转换为 Spring AI {@link Message} 列表
     *
     * @return 按时间正序排列的消息列表（旧 → 新）
     */
    public List<Message> getContextMessages(String conversationId) {
        String key = RedisKeyConstants.chatContextKey(conversationId);
        List<Object> rawList = redisTemplate.opsForList().range(key, 0, -1);
        if (rawList == null || rawList.isEmpty()) {
            return new ArrayList<>();
        }

        List<Message> messages = new ArrayList<>();
        for (Object raw : rawList) {
            try {
                Map<String, String> map = parseMessageMap(raw);
                String role = map.get("role");
                String content = map.get("content");
                if ("user".equals(role)) {
                    messages.add(new UserMessage(content));
                } else if ("assistant".equals(role)) {
                    messages.add(new AssistantMessage(content));
                }
            } catch (Exception e) {
                log.warn("[ChatContext] 消息反序列化失败，跳过: {}", raw, e);
            }
        }
        return messages;
    }

    /**
     * 获取上下文的原始 Map 列表（供拼接 Prompt 字符串时使用）
     */
    public List<Map<String, String>> getContextRaw(String conversationId) {
        String key = RedisKeyConstants.chatContextKey(conversationId);
        List<Object> rawList = redisTemplate.opsForList().range(key, 0, -1);
        if (rawList == null) return new ArrayList<>();

        List<Map<String, String>> result = new ArrayList<>();
        for (Object raw : rawList) {
            try {
                result.add(parseMessageMap(raw));
            } catch (Exception ignored) {}
        }
        return result;
    }

    // ---- 删除 ----

    /**
     * 清除指定会话的所有上下文（用户主动重置对话时调用）
     */
    public void clearContext(String conversationId) {
        String key = RedisKeyConstants.chatContextKey(conversationId);
        redisTemplate.delete(key);
        log.info("[ChatContext] 已清除会话上下文: {}", conversationId);
    }

    /**
     * 查询当前会话共有多少条消息
     */
    public long contextSize(String conversationId) {
        String key = RedisKeyConstants.chatContextKey(conversationId);
        Long size = redisTemplate.opsForList().size(key);
        return size != null ? size : 0;
    }

    // ---- 私有方法 ----

    private void append(String conversationId, String role, String content) {
        String key = RedisKeyConstants.chatContextKey(conversationId);
        Map<String, String> msgMap = new HashMap<>();
        msgMap.put("role", role);
        msgMap.put("content", content);
        try {
            String json = objectMapper.writeValueAsString(msgMap);
            redisTemplate.opsForList().rightPush(key, json);
            redisTemplate.expire(key, ttlSeconds, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("[ChatContext] 写入消息失败, conversationId={}, role={}", conversationId, role, e);
        }
    }

    /** 裁剪，保留最新的 maxTurns*2 条消息 */
    private void trim(String conversationId) {
        String key = RedisKeyConstants.chatContextKey(conversationId);
        long keepCount = (long) maxTurns * 2;
        redisTemplate.opsForList().trim(key, -keepCount, -1);
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> parseMessageMap(Object raw) throws Exception {
        if (raw instanceof Map<?, ?> map) {
            return (Map<String, String>) map;
        }
        // 存储的是 JSON 字符串
        return objectMapper.readValue(raw.toString(), new TypeReference<>() {});
    }
}
