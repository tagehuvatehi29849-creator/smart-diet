package com.hzy.smartdiet;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * 智能饮食管理助手 — 启动类
 * <p>
 * 项目信息：
 * <ul>
 *   <li>应用名：smart-diet</li>
 *   <li>端口：8123（context-path: /api）</li>
 *   <li>API 文档：http://localhost:8123/api/doc.html</li>
 * </ul>
 * </p>
 */
@SpringBootApplication
@EnableTransactionManagement
@EnableCaching
public class SmartDietApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartDietApplication.class, args);
    }
}

