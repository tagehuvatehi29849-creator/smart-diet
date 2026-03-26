package com.hzy.smartdiet.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Knife4j / SpringDoc OpenAPI 文档配置
 * <p>
 * 访问地址：http://localhost:8123/api/doc.html
 * </p>
 */
@Configuration
public class Knife4jConfig {

    @Bean
    public OpenAPI smartDietOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("智能饮食管理助手 API")
                        .description("Intelligent Diet Management Assistant — 基于 Spring AI Alibaba 的智能体后端接口文档")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("hzy")
                                .email("your_email@example.com"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")));
    }
}
