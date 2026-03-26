# 智能饮食管理助手（Smart Diet）

基于 Spring Boot 与 Vue 3 的全栈应用：用户可维护健康档案、记录饮食（含图片）、与 AI 对话获取建议，并生成可下载的 PDF 饮食报告。对话支持文本与图片多模态输入，短期对话上下文使用 Redis；可选 RAG 使用 PostgreSQL + PgVector。

## 功能概览

| 模块 | 说明 |
|------|------|
| 认证 | 用户名 / 密码注册与登录，Token 存 Redis，与健康档案共用 `user_profile` |
| 健康档案 | 身高、体重、目标、活动水平、饮食偏好与禁忌等信息的维护 |
| 饮食记录 | 餐次、描述、图片（URL 或本地上传 Base64）、AI 营养解析与建议 |
| 智能对话 | SSE 流式回复；可结合用户画像、当日饮食摘要与 RAG；支持仅图片 / 图文多模态 |
| 报告 | 按日期区间生成 PDF（含餐图，支持 WebP 转嵌入格式），前端通过 `[DOWNLOAD:开始:结束]` 触发下载接口 |
| 数据看板 | 饮食与营养相关可视化（ECharts） |

## 技术栈

**后端**

- Java 21、Spring Boot 3.3.x、Maven  
- Spring AI Alibaba（通义千问 DashScope）、Spring AI PgVector  
- MyBatis-Plus、动态数据源（MySQL + PostgreSQL）  
- Redis（会话 Token、对话上下文）  
- Knife4j / OpenAPI 3  
- iText 9（PDF）、TwelveMonkeys（WebP 解码）等  

**前端**

- Vue 3、Vite 5、Vue Router、Pinia  
- Element Plus、Axios、Marked、ECharts  

## 目录结构

```
smart-diet/
├── pom.xml                          # 后端 Maven 工程
├── src/main/java/com/hzy/smartdiet/ # 后端源码（controller / service / config / tools …）
├── src/main/resources/
│   ├── application.yml              # 公共配置（端口、context-path、MyBatis 等）
│   ├── application-prod.yml       # 示例/环境配置（数据库、Redis、AI Key 等，勿提交真实密钥）
│   ├── db/schema.sql               # MySQL 建库建表脚本
│   └── static/                     # 可选：前端构建产物（一体化部署时）
└── frontend/                        # Vue 前端工程
    ├── package.json
    ├── vite.config.js              # 开发代理 /api → 后端
    └── src/
```

## 环境要求

- JDK 21+  
- Node.js 18+（前端）  
- MySQL 8.0+（业务库）  
- Redis  
- PostgreSQL（启用 `pgvector` 扩展，用于 RAG；若暂不使用 RAG，仍需按项目数据源配置处理）  

## 数据库初始化

1. 在 MySQL 中执行：

   `src/main/resources/db/schema.sql`

2. 按实际环境修改 Spring 数据源配置（见下一节）。

## 配置说明

1. **主配置**：`src/main/resources/application.yml`  
   - `server.port`：HTTP 端口（示例为 `8133`）  
   - `server.servlet.context-path`：统一为 **`/api`**（所有 Controller 映射在 `/api` 之下）  
   - `spring.profiles.active`：激活的环境名（如 `prod`、`local`）

2. **环境专属**：`application-<profile>.yml`（如 `application-prod.yml`）  
   建议在此配置（勿将含密码、API Key 的文件提交到公开仓库）：  
   - MySQL、`postgres` 向量库 JDBC  
   - Redis 地址与密码  
   - `spring.ai.dashscope.api-key`（通义千问）  
   - 可选：`app.baidu-translate`（百度翻译）  
   - `app.rag.dataloader.enabled`：是否在启动时向 PgVector 加载知识库文档  

3. **前端 API 基址**：`frontend/src/api/index.js` 中 `baseURL: '/api'`。开发时由 Vite 代理到后端。

## 本地运行

### 1. 启动后端

```bash
cd smart-diet
mvn spring-boot:run
# 或指定 profile：
# mvn spring-boot:run -Dspring-boot.run.profiles=local
```

启动成功后（默认端口以 `application.yml` 为准）：

- 业务 API 根路径：`http://localhost:<port>/api`  
- Knife4j 文档：`http://localhost:<port>/api/doc.html`  

### 2. 启动前端

```bash
cd frontend
npm install
npm run dev
```

默认开发地址：`http://localhost:5173`。

请将 `frontend/vite.config.js` 中 `server.proxy['/api'].target` 改为你的后端地址（含端口），例如：

```js
target: 'http://localhost:8133',
```

若指到远程机，请确保该环境 CORS 或同源策略满足联调需求。

### 3. 前端生产构建

```bash
cd frontend
npm run pure-build
```

产物在 `frontend/dist/`。可：

- 由 Nginx 等静态托管，并将 `/api` 反代到 Spring Boot；或  
- 将 `dist` 内文件复制到 `src/main/resources/static/`，再打 Jar 由 Spring Boot 一并提供静态资源（注意 SPA 路由与 `context-path` 是否一致）。

```bash
cd smart-diet
mvn package -DskipTests
java -jar target/smart-diet-api-0.0.1-SNAPSHOT.jar
```

## 主要 API 分组（REST）

上下文路径为 `/api` 时，浏览器或网关访问形如：`/api/v1/...`。

| 前缀 | 说明 |
|------|------|
| `/v1/auth/*` | 注册、登录、登出、用户信息、改密 |
| `/v1/profile/*` | 健康档案 CRUD |
| `/v1/diet/*` | 饮食记录增删改查、按日/区间查询、再分析 |
| `/v1/agent/*` | SSE 对话、清除会话上下文 |
| `/v1/report/download` | PDF 报告下载（Query：`startDate`、`endDate`，格式 `yyyy-MM-dd`） |

详细参数与试调见 **Knife4j**：`/api/doc.html`。

## AI 与 PDF 下载说明

- 对话流式接口使用 **SSE**；带图场景会调用 DashScope 多模态能力（需有效 API Key）。  
- 工具生成的报告下载占位符须为 **`[DOWNLOAD:yyyy-MM-dd:yyyy-MM-dd]`**（英文冒号）。前端会兼容部分模型误改格式，并避免错误 Markdown 链接导致整页跳转到非法路径。  

## 许可证与贡献

私有/学习项目请按需补充许可证说明。欢迎通过 Issue / PR 改进文档与示例配置。

---

**安全提示**：生产环境务必使用独立密钥文件或环境变量注入数据库密码、Redis 密码与 DashScope Key，并避免将 `application-*.yml` 中的真实密钥提交到版本库。
