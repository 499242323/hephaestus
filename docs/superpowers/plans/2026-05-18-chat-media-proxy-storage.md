# Chat Media Proxy Storage Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为 SpringAIDemo 增加多媒体代理存储能力，让上传附件和 AI 生成图片先保存到本地多媒体服务，再由主应用代理展示和下载。

**Architecture:** 新增 `media` 包封装配置、存储客户端、元数据仓库、表初始化和代理控制器。`MultimodalChatService` 在调用 LLM 前保存上传附件，在生成图片后保存图片，并把代理 URL 返回给前端。`chat.html` 改成类 ChatGPT 的主界面，不展示接口和独立附件框。

**Tech Stack:** Spring Boot 3.5、Spring MVC、JdbcTemplate、RestClient、Spring AI、MySQL、原生 HTML/CSS/JavaScript。

---

### Task 1: 多媒体配置与元数据模型

**Files:**
- Create: `src/main/java/com/example/springaidemo/media/MediaStorageProperties.java`
- Create: `src/main/java/com/example/springaidemo/media/MediaFile.java`
- Modify: `src/main/java/com/example/springaidemo/SpringAiDemoApplication.java`
- Modify: `src/main/resources/application.yml`

- [ ] 创建 `MediaStorageProperties`，绑定 `app.media.base-url`、`username`、`password`、`storage-prefix`。
- [ ] 创建 `MediaFile` record，字段包含 id、conversationId、originalFilename、contentType、fileSize、storagePath、accessUrl、sourceType、createdAt。
- [ ] 在启动类启用 `MediaStorageProperties`。
- [ ] 在 `application.yml` 增加本地多媒体服务默认配置。
- [ ] 运行 `mvn -DskipTests compile`，预期编译通过。

### Task 2: 媒体元数据表与仓库

**Files:**
- Create: `src/main/java/com/example/springaidemo/media/MediaFileSchemaInitializer.java`
- Create: `src/main/java/com/example/springaidemo/media/MediaFileRepository.java`
- Test: `src/test/java/com/example/springaidemo/media/MediaFileRepositoryTest.java`

- [ ] 写 repository 测试，覆盖保存和按 id 查询。
- [ ] 实现 `MediaFileSchemaInitializer`，启动时创建 `spring_ai_media_file` 表。
- [ ] 实现 `MediaFileRepository#save` 和 `findById`。
- [ ] 运行 `mvn -Dtest=MediaFileRepositoryTest test`，预期通过。

### Task 3: 多媒体服务客户端

**Files:**
- Create: `src/main/java/com/example/springaidemo/media/MediaStorageException.java`
- Create: `src/main/java/com/example/springaidemo/media/StoredMediaFile.java`
- Create: `src/main/java/com/example/springaidemo/media/MediaStorageService.java`
- Test: `src/test/java/com/example/springaidemo/media/MediaStorageServiceTest.java`

- [ ] 写 `MediaStorageService` 测试，覆盖 path 生成、Basic Auth、上传返回 `"0"` 成功、非 `"0"` 失败。
- [ ] 实现上传到 `/home/httpfile/writefile.htm?path=...`。
- [ ] 实现从 `/home/httpfile/readfile.htm?path=...` 读取字节。
- [ ] 实现从图片 URL 下载后上传、从 base64 解码后上传。
- [ ] 运行 `mvn -Dtest=MediaStorageServiceTest test`，预期通过。

### Task 4: 代理展示和下载接口

**Files:**
- Create: `src/main/java/com/example/springaidemo/media/MediaFileController.java`
- Test: `src/test/java/com/example/springaidemo/media/MediaFileControllerTest.java`

- [ ] 写 controller 测试，覆盖预览、下载、id 不存在。
- [ ] 实现 `GET /api/media/files/{id}`，inline 返回文件内容。
- [ ] 实现 `GET /api/media/files/{id}/download`，attachment 返回文件内容。
- [ ] 确保响应不包含真实多媒体服务地址或认证信息。
- [ ] 运行 `mvn -Dtest=MediaFileControllerTest test`，预期通过。

### Task 5: 多模态聊天接入媒体存储

**Files:**
- Modify: `src/main/java/com/example/springaidemo/service/MultimodalChatService.java`
- Modify: `src/test/java/com/example/springaidemo/controller/MultimodalChatControllerTest.java`
- Create: `src/test/java/com/example/springaidemo/service/MultimodalChatServiceMediaTest.java`

- [ ] 扩展响应结构，增加 `attachments` 和 `generatedImage`。
- [ ] 上传附件时先保存多媒体服务和元数据，再继续 `.media(...)` 调 LLM。
- [ ] 图片生成返回 URL 时下载并上传多媒体服务。
- [ ] 图片生成返回 base64 时解码并上传多媒体服务。
- [ ] 图片存储失败时返回文字兜底，不让接口 500。
- [ ] 运行 `mvn -Dtest=MultimodalChatControllerTest,MultimodalChatServiceMediaTest,MultimodalDecisionParserTest test`，预期通过。

### Task 6: 前端改成类 ChatGPT 主界面

**Files:**
- Modify: `src/main/resources/static/chat.html`

- [ ] 移除可见接口地址、调试路径、技术字段文本。
- [ ] 移除独立附件框，改为输入框中的附件图标入口。
- [ ] 选择附件后在输入框内部显示胶囊或缩略片。
- [ ] 渲染后端返回的附件卡片和生成图片卡片。
- [ ] 图片使用代理 URL 预览，非图片显示下载按钮。

### Task 7: 集成验证

**Files:**
- No code changes expected.

- [ ] 运行 `mvn -DskipTests compile`，预期通过。
- [ ] 运行核心测试：`mvn -Dtest=MediaFileRepositoryTest,MediaStorageServiceTest,MediaFileControllerTest,MultimodalChatControllerTest,MultimodalChatServiceMediaTest,MultimodalDecisionParserTest test`，预期通过。
- [ ] 启动应用并打开 `/demo-api/chat.html`。
- [ ] 验证主界面不展示接口路径和独立附件框。
- [ ] 验证普通文本发送、附件发送、图片生成路径。
