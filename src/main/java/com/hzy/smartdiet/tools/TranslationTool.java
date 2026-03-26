package com.hzy.smartdiet.tools;

import cn.hutool.crypto.digest.DigestUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Random;

/**
 * 翻译工具（AI Tool）
 * <p>
 * 当用户使用非中文提问时，AI 会自动调用此工具将文本翻译为中文，
 * 再基于中文内容给出专业的饮食建议，确保全程以中文服务。
 * <br>
 * 底层使用百度翻译开放 API（免费标准版，每月 100 万字符免费额度）。
 * API 文档：https://api.fanyi.baidu.com/doc/21
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TranslationTool {

    private final ObjectMapper objectMapper;

    @Value("${app.baidu-translate.api-url:https://fanyi-api.baidu.com/api/trans/vip/translate}")
    private String apiUrl;

    @Value("${app.baidu-translate.app-id:}")
    private String appId;

    @Value("${app.baidu-translate.secret-key:}")
    private String secretKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final Random random = new Random();

    /**
     * 将任意语言文本翻译为中文
     *
     * @param text 需要翻译的文本（支持英文、日语、韩语等）
     * @return 翻译后的中文文本；若翻译失败则返回原文
     */
    @Tool(description = "将非中文文本翻译为中文。当用户用英文、日语、韩语等非中文语言提问时，先调用此工具翻译为中文，再用中文回答。")
    public String translateToChinese(String text) {
        if (text == null || text.isBlank()) return text;
        if (isChinese(text)) return text;
        return callBaiduTranslate(text, "auto", "zh");
    }

    /**
     * 将中文文本翻译为目标语言（供内部调试使用）
     *
     * @param text           中文文本
     * @param targetLanguage 目标语言代码（如 en/ja/ko/fr）
     * @return 翻译结果
     */
    @Tool(description = "将中文文本翻译为指定语言。支持的语言代码：en=英文, ja=日语, ko=韩语, fr=法语, de=德语")
    public String translateFromChinese(String text, String targetLanguage) {
        if (text == null || text.isBlank()) return text;
        return callBaiduTranslate(text, "zh", targetLanguage);
    }

    // ---- 私有方法 ----

    private String callBaiduTranslate(String text, String from, String to) {
        if (appId == null || appId.isBlank() || secretKey == null || secretKey.isBlank()) {
            log.warn("[Translation] 百度翻译 API Key 未配置，返回原文");
            return text;
        }
        try {
            String salt = String.valueOf(random.nextInt(100000));
            String sign = DigestUtil.md5Hex(appId + text + salt + secretKey);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("q", text);
            params.add("from", from);
            params.add("to", to);
            params.add("appid", appId);
            params.add("salt", salt);
            params.add("sign", sign);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    apiUrl, new HttpEntity<>(params, headers), String.class);

            JsonNode root = objectMapper.readTree(response.getBody());
            if (root.has("error_code")) {
                log.error("[Translation] 翻译 API 错误: {}", root.get("error_msg").asText());
                return text;
            }

            return root.path("trans_result").get(0).path("dst").asText(text);

        } catch (Exception e) {
            log.error("[Translation] 翻译请求异常，返回原文", e);
            return text;
        }
    }

    /**
     * 简单判断文本是否以中文为主（超过 30% 字符为中文）
     */
    private boolean isChinese(String text) {
        long chineseCount = text.chars()
                .filter(c -> c >= 0x4E00 && c <= 0x9FFF)
                .count();
        return (double) chineseCount / text.length() > 0.3;
    }
}
