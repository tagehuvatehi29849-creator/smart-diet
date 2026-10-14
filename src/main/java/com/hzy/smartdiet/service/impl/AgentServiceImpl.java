package com.hzy.smartdiet.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hzy.smartdiet.constant.PromptConstants;
import com.hzy.smartdiet.mapper.DietRecordMapper;
import com.hzy.smartdiet.model.dto.ChatRequestDTO;
import com.hzy.smartdiet.model.entity.DietRecord;
import com.hzy.smartdiet.model.entity.UserProfile;
import com.hzy.smartdiet.rag.RagService;
import com.hzy.smartdiet.service.AgentService;
import com.hzy.smartdiet.service.ChatContextService;
import com.hzy.smartdiet.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.client.RestClient;
import reactor.core.publisher.Flux;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * AI 智能体服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentServiceImpl implements AgentService {

    private final ChatClient chatClient;
    private final ChatContextService chatContextService;
    private final UserProfileService userProfileService;
    private final RagService ragService;
    private final DietRecordMapper dietRecordMapper;

    /** 直接调用 DashScope 原生 API 所需的 API Key */
    @Value("${spring.ai.dashscope.api-key}")
    private String dashscopeApiKey;

    /** 多模态（视觉）模型名称，如 qwen-vl-plus / qwen-vl-max */
    @Value("${app.ai.dashscope.vision.model:qwen-vl-plus}")
    private String visionModel;

    /** DashScope 多模态生成接口地址 */
    @Value("${app.ai.dashscope.vision.base-url:https://dashscope.aliyuncs.com/api/v1/services/aigc/multimodal-generation/generation}")
    private String visionBaseUrl;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Flux<String> streamChat(ChatRequestDTO request) {
        String conversationId = resolveConversationId(request);

        // 构建系统追加内容（用户画像 + 今日饮食 + RAG）
        StringBuilder systemAppend = new StringBuilder();
        if (request.isEnableProfile()) {
            String profileCtx = buildUserProfileContext(request.getUserId());
            if (profileCtx != null) systemAppend.append(profileCtx);

            String todayDietCtx = buildTodayDietContext(request.getUserId());
            if (todayDietCtx != null) systemAppend.append("\n").append(todayDietCtx);
        }

        boolean hasImage = request.getImageUrl() != null && !request.getImageUrl().isBlank();
        String userText = (request.getMessage() != null && !request.getMessage().isBlank())
                ? request.getMessage() : (hasImage ? "请分析这张图片" : "");

        if (request.isEnableRag() && !userText.isBlank()) {
            String ragCtx = ragService.retrieveContext(userText);
            if (ragCtx != null && !ragCtx.isBlank()) systemAppend.append("\n").append(ragCtx);
        }

        final String finalConvId = conversationId;
        final String systemCtx   = systemAppend.toString();

        if (hasImage) {
            // ---- 视觉分析路径：直接调 DashScope VL API（同步），包装成 Flux 返回 ----
            return Flux.create(sink -> {
                try {
                    List<Message> historyMessages = chatContextService.getContextMessages(finalConvId);

                    // 把系统上下文、历史和当前文字合并成提示词
                    StringBuilder promptSb = new StringBuilder();
                    if (!systemCtx.isBlank()) {
                        promptSb.append(PromptConstants.SYSTEM_PROMPT).append("\n\n").append(systemCtx).append("\n\n");
                    }
                    // 最近几轮历史（纯文字摘要，不发给视觉模型全部 token）
                    if (!historyMessages.isEmpty()) {
                        promptSb.append("【历史对话摘要】\n");
                        historyMessages.forEach(m -> promptSb.append(m.getText()).append("\n"));
                        promptSb.append("\n");
                    }
                    promptSb.append("【用户当前消息】").append(userText);

                    String result = callDashScopeVisionApi(promptSb.toString(), request.getImageUrl());

                    // 按字符分批 emit，给用户"逐字"打印的体验（每批 ~10 字）
                    int batchSize = 10;
                    for (int i = 0; i < result.length(); i += batchSize) {
                        sink.next(result.substring(i, Math.min(i + batchSize, result.length())));
                    }
                    // 保存到 Redis
                    chatContextService.appendTurn(finalConvId,
                            hasImage ? "[图片] " + userText : userText, result);
                    sink.complete();
                } catch (Exception e) {
                    log.error("[Agent] 视觉对话异常, conversationId={}", finalConvId, e);
                    sink.error(e);
                }
            });
        }

        // ---- 纯文字路径（原有逻辑）----
        List<Message> historyMessages = chatContextService.getContextMessages(conversationId);
        StringBuilder fullResponse = new StringBuilder();

        return chatClient.prompt()
                .system(s -> {
                    if (!systemCtx.isBlank()) {
                        s.text(PromptConstants.SYSTEM_PROMPT + "\n\n" + systemCtx);
                    }
                })
                .messages(historyMessages)
                .user(userText)
                .stream()
                .content()
                .doOnNext(fullResponse::append)
                .doOnComplete(() -> {
                    chatContextService.appendTurn(finalConvId, userText, fullResponse.toString());
                    log.debug("[Agent] 会话 {} 本轮已保存到 Redis，回复长度={}", finalConvId, fullResponse.length());
                })
                .doOnError(e -> log.error("[Agent] 流式对话异常, conversationId={}", finalConvId, e));
    }

    @Override
    public String analyzeDietRecord(String userId, String mealDescription, int mealType, String imageUrl) {
        String mealTypeLabel = switch (mealType) {
            case 0 -> "早餐";
            case 1 -> "午餐";
            case 2 -> "晚餐";
            case 3 -> "加餐";
            case 4 -> "夜宵";
            default -> "餐食";
        };

        // DIET_ANALYSIS_TEMPLATE 含 JSON 示例花括号，不能用 PromptTemplate，改用 String.replace
        String desc = (mealDescription != null && !mealDescription.isBlank())
                ? mealDescription : "（请根据图片识别）";
        String prompt = PromptConstants.DIET_ANALYSIS_TEMPLATE
                .replace("{mealType}", mealTypeLabel)
                .replace("{mealDescription}", desc);

        boolean hasImage = imageUrl != null && !imageUrl.isBlank();

        if (!hasImage) {
            // 纯文字分析，使用默认文本模型
            return chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();
        }

        // 有图片 → 绕开 Spring AI media() 接口，直接调用 DashScope 原生多模态 API
        try {
            String imageValue;

            if (imageUrl.startsWith("data:")) {
                // 本地上传的 Base64 Data URL，直接透传
                imageValue = imageUrl;
            } else {
                // 普通 HTTP URL：后端下载后转为 Base64 Data URL，避免外部 URL 防盗链问题
                java.net.HttpURLConnection conn =
                        (java.net.HttpURLConnection) new java.net.URL(imageUrl).openConnection();
                conn.setRequestProperty("User-Agent",
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(15000);
                conn.connect();
                String ct = conn.getContentType();
                String mimeStr = (ct != null && ct.contains("/"))
                        ? ct.split(";")[0].trim()
                        : "image/jpeg";
                byte[] imageBytes;
                try (java.io.InputStream is = conn.getInputStream()) {
                    imageBytes = is.readAllBytes();
                }
                String b64 = java.util.Base64.getEncoder().encodeToString(imageBytes);
                imageValue = "data:" + mimeStr + ";base64," + b64;
            }

            return callDashScopeVisionApi(prompt, imageValue);

        } catch (Exception e) {
            log.warn("[Agent] 视觉分析失败，降级为纯文字分析, imageUrl前缀={}",
                    imageUrl.substring(0, Math.min(40, imageUrl.length())), e);
            return chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();
        }
    }

    /**
     * 直接调用 DashScope 原生多模态 API（绕过 Spring AI 的 media() URL 解析）。
     *
     * @param prompt     文字提示
     * @param imageValue data:image/xxx;base64,... 或外部 HTTP URL
     */
    private String callDashScopeVisionApi(String prompt, String imageValue) throws Exception {
        // 构造请求体：{"model":"<视觉模型>","input":{"messages":[{"role":"user","content":[{"image":"..."},{"text":"..."}]}]}}
        List<Map<String, Object>> contentList = new ArrayList<>();

        Map<String, Object> imagePart = new LinkedHashMap<>();
        imagePart.put("image", imageValue);
        contentList.add(imagePart);

        Map<String, Object> textPart = new LinkedHashMap<>();
        textPart.put("text", prompt);
        contentList.add(textPart);

        Map<String, Object> message = new LinkedHashMap<>();
        message.put("role", "user");
        message.put("content", contentList);

        Map<String, Object> input = new LinkedHashMap<>();
        input.put("messages", List.of(message));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", visionModel);
        body.put("input", input);

        String requestJson = objectMapper.writeValueAsString(body);
        log.debug("[Agent] DashScope VL 请求体（前200字符）: {}", requestJson.substring(0, Math.min(200, requestJson.length())));

        RestClient restClient = RestClient.builder()
                .baseUrl(visionBaseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + dashscopeApiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();

        String responseJson = restClient.post()
                .body(requestJson)
                .retrieve()
                .body(String.class);

        log.debug("[Agent] DashScope VL 原始响应: {}", responseJson);

        // 解析响应：{"output":{"choices":[{"message":{"content":[{"text":"..."}]}}]}}
        JsonNode root = objectMapper.readTree(responseJson);
        JsonNode choices = root.path("output").path("choices");
        if (choices.isArray() && !choices.isEmpty()) {
            JsonNode content = choices.get(0).path("message").path("content");
            if (content.isArray()) {
                for (JsonNode node : content) {
                    if (node.has("text")) {
                        return node.get("text").asText();
                    }
                }
            }
        }

        // 兜底：返回全部响应内容
        log.warn("[Agent] 无法从 DashScope VL 响应中提取 text，原始响应: {}", responseJson);
        return responseJson;
    }

    @Override
    public void clearConversation(String conversationId) {
        chatContextService.clearContext(conversationId);
    }

    // ---- 私有辅助方法 ----

    private String resolveConversationId(ChatRequestDTO request) {
        if (request.getConversationId() != null && !request.getConversationId().isBlank()) {
            return request.getConversationId();
        }
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 从 MySQL 读取用户画像，格式化为系统提示追加内容
     */
    private String buildUserProfileContext(String userId) {
        try {
            UserProfile profile = userProfileService.getByUserId(userId);
            if (profile == null) return null;

            BigDecimal bmi = null;
            if (profile.getHeightCm() != null && profile.getWeightKg() != null
                    && profile.getHeightCm().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal heightM = profile.getHeightCm().divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
                bmi = profile.getWeightKg().divide(heightM.multiply(heightM), 1, RoundingMode.HALF_UP);
            }

            return new PromptTemplate(PromptConstants.USER_PROFILE_CONTEXT_TEMPLATE)
                    .create(Map.ofEntries(
                            Map.entry("userId", userId),
                            Map.entry("nickname", orEmpty(profile.getNickname())),
                            Map.entry("age", orEmpty(profile.getAge())),
                            Map.entry("gender", profile.getGender() == null ? "未知" : (profile.getGender() == 1 ? "男" : "女")),
                            Map.entry("height", orEmpty(profile.getHeightCm())),
                            Map.entry("weight", orEmpty(profile.getWeightKg())),
                            Map.entry("bmi", bmi != null ? bmi.toPlainString() : "未知"),
                            Map.entry("activityLevel", profile.getActivityLevel() != null
                                    ? com.hzy.smartdiet.model.enums.ActivityLevelEnum.fromCode(profile.getActivityLevel()).getLabel()
                                    : "未知"),
                            Map.entry("healthGoals", listOrEmpty(profile.getHealthGoals())),
                            Map.entry("dietaryPreferences", orEmpty(profile.getDietaryPreferences())),
                            Map.entry("allergies", listOrEmpty(profile.getFoodAllergies())),
                            Map.entry("healthConditions", listOrEmpty(profile.getHealthConditions()))
                    ))
                    .getContents();
        } catch (Exception e) {
            log.warn("[Agent] 加载用户画像失败, userId={}", userId, e);
            return null;
        }
    }

    /**
     * 查询用户今日饮食记录，格式化为系统提示追加内容。
     * 直接使用 Mapper 查询，避免与 DietRecordService 产生循环依赖。
     */
    private String buildTodayDietContext(String userId) {
        try {
            LocalDate today = LocalDate.now();
            List<DietRecord> records = dietRecordMapper.selectByUserIdAndDate(userId, today);
            if (records == null || records.isEmpty()) return null;

            String[] mealLabels = {"早餐", "午餐", "晚餐", "加餐", "夜宵"};
            StringBuilder sb = new StringBuilder();
            BigDecimal totalCalories = BigDecimal.ZERO;

            for (DietRecord r : records) {
                String mealLabel = (r.getMealType() != null && r.getMealType() < mealLabels.length)
                        ? mealLabels[r.getMealType()] : "餐食";
                sb.append("- **").append(mealLabel).append("**：");

                // 优先展示 AI 解析出的食物列表
                if (r.getFoodItems() != null && !r.getFoodItems().isEmpty()) {
                    sb.append(String.join("、", r.getFoodItems()));
                } else if (r.getMealDescription() != null && !r.getMealDescription().isBlank()) {
                    sb.append(r.getMealDescription());
                } else {
                    sb.append("（无描述）");
                }

                // 追加热量
                if (r.getTotalCalories() != null) {
                    sb.append("（约 ").append(r.getTotalCalories().stripTrailingZeros().toPlainString()).append(" kcal）");
                    totalCalories = totalCalories.add(r.getTotalCalories());
                }

                // 追加 AI 建议（简短）
                if (r.getAiSuggestion() != null && !r.getAiSuggestion().isBlank()) {
                    sb.append("\n  > AI建议：").append(r.getAiSuggestion());
                }

                sb.append("\n");
            }

            if (totalCalories.compareTo(BigDecimal.ZERO) > 0) {
                sb.append("- **今日已记录总热量**：约 ")
                  .append(totalCalories.stripTrailingZeros().toPlainString())
                  .append(" kcal\n");
            }

            return PromptConstants.TODAY_DIET_CONTEXT_TEMPLATE
                    .replace("{date}", today.toString())
                    .replace("{records}", sb.toString().trim());
        } catch (Exception e) {
            log.warn("[Agent] 加载今日饮食记录失败, userId={}", userId, e);
            return null;
        }
    }

    private String orEmpty(Object val) {
        return val != null ? val.toString() : "未知";
    }

    private String listOrEmpty(List<String> list) {
        if (list == null || list.isEmpty()) return "无";
        return String.join("、", list);
    }
}
