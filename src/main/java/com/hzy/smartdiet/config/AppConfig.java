package com.hzy.smartdiet.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestTemplate;

/**
 * 应用通用 Bean 配置
 */
@Configuration
public class AppConfig {

    /**
     * 全局 ObjectMapper：支持 Java 8 时间类型，忽略未知属性，不写时间戳
     */
    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        return mapper;
    }

    /**
     * 全局 RestTemplate（供翻译工具等 HTTP 调用使用）
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
