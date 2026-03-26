package com.hzy.smartdiet.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Vue SPA 回退路由：所有非 API、非静态资源的请求都转发到 index.html，
 * 由前端 Vue Router 处理具体路径（如 /login、/dashboard 等）。
 */
@Controller
public class SpaController {

    /**
     * 匹配所有不带扩展名、且不以 /v1 开头的路径，回退到 index.html。
     * 注意：context-path 为 /api，这里的路径已经是去掉 /api 后的部分。
     * 但前端静态文件在 context-path 之外，由 Spring Boot 的 ResourceHandler 直接服务，
     * 所以这个控制器实际上不会被触发——前端路径走的是根路径，不经过 /api context-path。
     *
     * 如果前端和后端都在同一端口（Spring Boot 直接 serve 静态文件），
     * 前端的路由路径（/login 等）不包含 /api 前缀，不会进入 context-path 范围，
     * 因此 Spring Boot 的静态资源处理器已经能处理 index.html 的回退——
     * 只需保证静态目录里有 index.html 即可。
     */
    @RequestMapping(value = { "/" })
    public String index() {
        return "forward:/index.html";
    }
}
