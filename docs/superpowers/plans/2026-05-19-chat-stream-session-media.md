# Chat Stream Session Media Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为 SpringAIDemo 增加会话级媒体目录、原始名与英文存储名双保存、基于系统提示词的决策约束、SSE 流式多模态聊天，以及支持 Markdown/代码块/安全 HTML 的 GPT 风格消息渲染。

**Architecture:** 保留现有同步多模态接口用于兼容，同时新增 `POST /api/chat/multimodal/stream` 作为主聊天路径。后端把附件先保存到带 `conversationId` 的多媒体目录，再通过系统提示词驱动 LLM 决策；文本回复和过程状态走 SSE，图片生成完成后再发送媒体事件。前端改成事件驱动消息渲染，支持逐步追加文本与富文本展示。

**Tech Stack:** Spring Boot 3.5、Spring MVC SSE、JdbcTemplate、RestClient、Spring AI、MySQL、原生 HTML/CSS/JavaScript。

---

### Task 1: 扩展媒体元数据模型与存储路径

**Files:**
- Modify: `src/main/java/com/example/springaidemo/media/MediaFile.java`
- Modify: `src/main/java/com/example/springaidemo/media/StoredMediaFile.java`
- Modify: `src/main/java/com/example/springaidemo/media/MediaFileSchemaInitializer.java`
- Modify: `src/main/java/com/example/springaidemo/media/MediaFileRepository.java`
- Modify: `src/main/java/com/example/springaidemo/media/MediaStorageService.java`
- Test: `src/test/java/com/example/springaidemo/media/MediaStorageServiceTest.java`
- Test: `src/test/java/com/example/springaidemo/media/MediaFileSchemaInitializerTest.java`

- [ ] 给媒体模型补 `storedFilename` 字段。
- [ ] 调整建表 SQL，新增 `stored_filename` 列。
- [ ] 调整 repository 的保存、查询与更新逻辑。
- [ ] 把存储路径改成 `storage-prefix/conversationId/category/yyyyMMdd/uuid-storedFilename`。
- [ ] 为原始文件名生成英文安全存储名，同时保留原始文件名。
- [ ] 运行 `mvn -Dtest=MediaStorageServiceTest,MediaFileSchemaInitializerTest test`，预期通过。

### Task 2: 把 JSON 约束迁移到系统提示词

**Files:**
- Modify: `src/main/java/com/example/springaidemo/service/MultimodalChatService.java`
- Test: `src/test/java/com/example/springaidemo/service/MultimodalDecisionParserTest.java`

- [ ] 拆分系统提示词与用户消息正文。
- [ ] 普通决策调用和带附件决策调用都使用 system prompt 约束 JSON。
- [ ] 保持现有 JSON 解析容错逻辑。
- [ ] 运行 `mvn -Dtest=MultimodalDecisionParserTest test`，预期通过。

### Task 3: 新增流式多模态聊天服务与控制器

**Files:**
- Create: `src/main/java/com/example/springaidemo/service/MultimodalStreamEvent.java`
- Create: `src/main/java/com/example/springaidemo/service/MultimodalStreamingService.java`
- Create: `src/main/java/com/example/springaidemo/controller/MultimodalChatStreamController.java`
- Test: `src/test/java/com/example/springaidemo/controller/MultimodalChatStreamControllerTest.java`

- [ ] 设计固定事件类型：`status`、`attachments`、`delta`、`image`、`done`、`error`。
- [ ] 流式接口先推送附件保存结果和“正在分析附件”状态。
- [ ] 文本回复按增量推送 `delta`。
- [ ] 如需生成图片，先推送“正在生成图片”，图片完成后推送 `image`。
- [ ] 完成时推送 `done`，异常时推送 `error`。
- [ ] 运行 `mvn -Dtest=MultimodalChatStreamControllerTest test`，预期通过。

### Task 4: 同步接口适配新媒体字段

**Files:**
- Modify: `src/main/java/com/example/springaidemo/service/MultimodalChatService.java`
- Modify: `src/test/java/com/example/springaidemo/controller/MultimodalChatControllerTest.java`

- [ ] 同步接口的附件保存逻辑切换到新媒体字段和新路径规则。
- [ ] 生成图片保存逻辑切换到新媒体字段和新路径规则。
- [ ] 下载仍优先返回原始文件名。
- [ ] 运行 `mvn -Dtest=MultimodalChatControllerTest test`，预期通过。

### Task 5: 前端改成流式富文本渲染

**Files:**
- Modify: `src/main/resources/static/chat.html`

- [ ] 改为优先调用 `/api/chat/multimodal/stream`。
- [ ] 使用 `fetch` + `ReadableStream` 读取 SSE 事件。
- [ ] 对 `status` 事件显示过程提示。
- [ ] 对 `delta` 事件累积文本，并渲染 Markdown / 代码块 / 安全 HTML。
- [ ] 对 `attachments` 和 `image` 事件插入媒体卡片。
- [ ] 保持输入框附件胶囊、独立滚动和不暴露接口文案。

### Task 6: 集成验证

**Files:**
- No code changes expected.

- [ ] 运行 `mvn test`，预期通过。
- [ ] 运行 `mvn -DskipTests compile`，预期通过。
- [ ] 启动应用并访问 `/demo-api/chat.html`。
- [ ] 验证附件目录包含 `conversationId`。
- [ ] 验证回复流式出现，包含“正在分析附件”“正在生成图片”等过程提示。
- [ ] 验证 Markdown、代码块和媒体卡片渲染正常。
