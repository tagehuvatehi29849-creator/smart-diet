package com.hzy.smartdiet.controller;

import com.hzy.smartdiet.common.Result;
import com.hzy.smartdiet.model.dto.ChatRequestDTO;
import com.hzy.smartdiet.service.AgentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * AI 智能体对话控制器
 * <p>
 * 核心接口：SSE 流式对话，支持 Server-Sent Events 实时推流。
 * 前端连接方式示例（JavaScript）：
 * <pre>
 *   const es = new EventSource('/api/v1/agent/chat?...');
 *   // 但因为是 POST，推荐使用 fetch + ReadableStream 或 eventsource-polyfill
 * </pre>
 * </p>
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/v1/agent")
@RequiredArgsConstructor
@Tag(name = "AI 智能体接口", description = "大模型流式对话（SSE）")
public class AgentChatController {

    private final AgentService agentService;

    /**
     * 用于执行异步 SSE 写入的虚拟线程池（Java 21 Virtual Threads）
     * 虚拟线程轻量高并发，无需显式线程池大小配置
     */
    private final ExecutorService sseExecutor = Executors.newVirtualThreadPerTaskExecutor();

    // ==========================================
    // 核心 SSE 流式对话接口
    // ==========================================

    /**
     * 流式对话（SSE）
     * <p>
     * 客户端收到的 SSE 事件格式：
     * <pre>
     *   data: {"type":"delta","content":"你好"}
     *   data: {"type":"delta","content":"，我是小膳"}
     *   data: {"type":"done","conversationId":"xxx","content":""}
     *   data: {"type":"error","content":"错误信息"}
     * </pre>
     * </p>
     */
    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "SSE 流式对话", description = "发送用户消息，以 SSE 方式流式返回 AI 回复")
    public SseEmitter streamChat(@RequestBody ChatRequestDTO request) {
        // SSE 超时时间：3 分钟（大模型生成可能较慢）
        SseEmitter emitter = new SseEmitter(180_000L);

        sseExecutor.submit(() -> {
            try {
                agentService.streamChat(request)
                        .subscribe(
                                // onNext：每收到一个文本片段就推送给客户端
                                chunk -> {
                                    try {
                                        emitter.send(SseEmitter.event()
                                                .name("delta")
                                                .data("{\"type\":\"delta\",\"content\":" + escapeJson(chunk) + "}"));
                                    } catch (IOException e) {
                                        log.warn("[SSE] 写入 chunk 失败，可能客户端已断开", e);
                                        emitter.completeWithError(e);
                                    }
                                },
                                // onError：出错时推送错误事件
                                error -> {
                                    log.error("[SSE] 流式对话发生错误", error);
                                    try {
                                        emitter.send(SseEmitter.event()
                                                .name("error")
                                                .data("{\"type\":\"error\",\"content\":\"" + error.getMessage() + "\"}"));
                                    } catch (IOException ignored) {}
                                    emitter.completeWithError(error);
                                },
                                // onComplete：发送完成标记
                                () -> {
                                    try {
                                        String convId = request.getConversationId() != null
                                                ? request.getConversationId() : "";
                                        emitter.send(SseEmitter.event()
                                                .name("done")
                                                .data("{\"type\":\"done\",\"conversationId\":\"" + convId + "\",\"content\":\"\"}"));
                                    } catch (IOException e) {
                                        log.warn("[SSE] 发送 done 事件失败", e);
                                    } finally {
                                        emitter.complete();
                                    }
                                }
                        );
            } catch (Exception e) {
                log.error("[SSE] 启动流式对话失败", e);
                emitter.completeWithError(e);
            }
        });

        // 注册超时和错误回调
        emitter.onTimeout(() -> {
            log.warn("[SSE] 连接超时，userId={}", request.getUserId());
            emitter.complete();
        });
        emitter.onError(e -> log.error("[SSE] 连接错误，userId={}", request.getUserId(), e));

        return emitter;
    }

    // ==========================================
    // 辅助接口
    // ==========================================

    /**
     * 清除指定会话的上下文（用户主动重置对话）
     */
    @DeleteMapping("/conversation/{conversationId}")
    @Operation(summary = "清除会话上下文", description = "清除 Redis 中指定会话的历史消息")
    public Result<Void> clearConversation(
            @PathVariable @Parameter(description = "会话ID") String conversationId) {
        agentService.clearConversation(conversationId);
        return Result.success("会话已清除", null);
    }

    // ---- 工具方法 ----

    /**
     * 转义 JSON 字符串中的特殊字符，防止 SSE 数据格式错乱
     */
    private String escapeJson(String text) {
        if (text == null) return "\"\"";
        return "\"" + text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t")
                + "\"";
    }
}
