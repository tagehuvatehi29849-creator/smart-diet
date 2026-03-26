package com.hzy.smartdiet.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * AI 对话请求 DTO
 */
@Data
@Schema(description = "AI 对话请求体")
public class ChatRequestDTO {

    @Schema(description = "用户唯一标识", requiredMode = Schema.RequiredMode.REQUIRED, example = "user_001")
    private String userId;

    /**
     * 会话 ID。
     * 若为空，后端自动创建新会话；传入已有 ID 则继续上下文对话。
     */
    @Schema(description = "会话 ID（为空时自动创建新会话）", example = "conv_abc123")
    private String conversationId;

    /** 文字消息（有图片时可为空） */
    @Size(max = 2000, message = "消息内容不能超过 2000 字")
    @Schema(description = "用户发送的消息内容（有图片时可为空）",
            example = "我今天早饭吃了两个煎蛋、一碗小米粥和一根香蕉，大概摄入了多少热量？")
    private String message;

    /**
     * 图片（Base64 Data URL 或 HTTP URL），可选。
     * 有图片时，消息将通过视觉模型（qwen-vl-plus）处理。
     */
    @Schema(description = "图片 Data URL（data:image/xxx;base64,...）或 HTTP URL，可选")
    private String imageUrl;

    /** 是否启用 RAG 知识库增强（默认 true） */
    @Schema(description = "是否启用 RAG 知识库检索增强", defaultValue = "true")
    private boolean enableRag = true;

    /** 是否注入用户画像上下文（默认 true） */
    @Schema(description = "是否注入用户画像上下文", defaultValue = "true")
    private boolean enableProfile = true;
}
