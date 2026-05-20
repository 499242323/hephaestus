# 聊天页统一多模态发送与多媒体代理存储设计

## 目标

把当前聊天页保持为一个统一发送窗口：用户可以输入文字、选择任意附件，然后点击同一个发送按钮。后端负责把附件传给 Spring AI 多模态接口，并让 LLM 判断是否需要生成图片。

本次新增重点是：上传附件和 AI 生成图片都先存入本地多媒体服务，再由 SpringAIDemo 提供一层代理访问接口。浏览器不直接访问 `http://localhost:18080`，也不会暴露多媒体服务的 Basic 认证信息或真实读写 URL。

## 范围

本次包含：

- 聊天页继续使用一个统一发送窗口。
- 上传附件先写入多媒体服务，并把媒体元数据保存到 MySQL。
- 上传附件仍然通过 Spring AI `.media(...)` 传给 LLM。
- LLM 通过系统提示词约束返回结构化字段，决定是否调用 `ImageModel` 生成图片。
- AI 生成图片后也上传到多媒体服务，并保存媒体元数据。
- 前端通过 SpringAIDemo 的代理 URL 展示图片和下载附件。
- 多媒体服务地址、账号、密码、存储前缀放到 `application.yml` 配置项中。
- 主界面采用接近 ChatGPT 的聊天体验，不向用户展示接口地址、请求路径、模型配置或其他技术细节。
- 多媒体目录带会话 ID，便于会话级归档和追溯。
- 聊天回复改为流式输出，过程状态和最终内容统一走事件流。
- 聊天消息区域支持 Markdown、代码块和安全白名单 HTML 渲染。

本次不包含：

- 用户登录鉴权。
- 文件搜索、标签管理、知识库索引。
- 手写 Office、PDF、压缩包等附件解析逻辑。
- 前端框架迁移。
- 直接暴露多媒体服务真实 URL 给浏览器。

## 多媒体服务集成

参考项目 `E:\NEW_WORK\wizdom-urban-v14-framework` 中的：

- `cn.com.egova.bizbase.tools.HttpFileUtils#getFileInputStream`
- `cn.com.egova.bizbase.tools.HttpFileUtils#uploadFile`

多媒体服务本地配置：

```yaml
app:
  media:
    base-url: http://localhost:18080
    username: egovahttp
    password: egovahttp
    storage-prefix: spring-ai-demo
```

SpringAIDemo 后端内部访问多媒体服务：

```text
POST http://localhost:18080/home/httpfile/writefile.htm?path={encodedPath}
POST http://localhost:18080/home/httpfile/readfile.htm?path={encodedPath}
```

请求头使用 Basic Auth：

```text
Authorization: Basic base64(username:password)
```

文件存储路径由后端生成，建议格式：

```text
spring-ai-demo/{sessionId}/{category}/{yyyyMMdd}/{uuid}-{storedFilename}
```

说明：

- `sessionId` 取自 `X-Session-Id`。
- `category` 建议为 `upload` 或 `generated`。
- `storedFilename` 为转换后的英文安全文件名。
- 浏览器展示和下载时仍优先使用原始文件名，不直接把英文存储名暴露给用户。

## 代理访问设计

前端只访问 SpringAIDemo，不直接访问多媒体服务：

```text
GET /api/media/files/{id}
GET /api/media/files/{id}/download
```

行为：

- `/api/media/files/{id}`：用于浏览器内预览，图片、PDF 等可 inline 展示的类型保持 inline。
- `/api/media/files/{id}/download`：强制下载，设置 `Content-Disposition: attachment`。
- 后端根据 `{id}` 查询媒体元数据，拿到 `storage_path` 后再访问多媒体服务读取文件流。
- 后端透传必要的 `Content-Type`、文件名和状态码，不把真实多媒体 URL 返回给浏览器。

这样可以隐藏：

- `http://localhost:18080`
- `/home/httpfile/writefile.htm`
- `/home/httpfile/readfile.htm`
- `egovahttp/egovahttp`
- 多媒体服务内部认证规则

## 数据表设计

新增媒体元数据表 `spring_ai_media_file`，使用当前项目已有的 `JdbcTemplate` 风格初始化和访问，不引入 JPA。

建议字段：

```sql
CREATE TABLE IF NOT EXISTS spring_ai_media_file (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  conversation_id VARCHAR(128) NOT NULL,
  original_filename VARCHAR(255) NOT NULL,
  stored_filename VARCHAR(255) NOT NULL,
  content_type VARCHAR(128) NOT NULL,
  file_size BIGINT NOT NULL,
  storage_path VARCHAR(512) NOT NULL,
  access_url VARCHAR(512) NOT NULL,
  source_type VARCHAR(32) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_spring_ai_media_conversation (conversation_id, created_at),
  UNIQUE KEY uk_spring_ai_media_storage_path (storage_path)
);
```

字段含义：

- `conversation_id`：来自 `X-Session-Id`，用于关联一次聊天会话。
- `original_filename`：用户上传文件名或 AI 生成图片原始展示名，可保留中文。
- `stored_filename`：转换后的英文安全文件名，用于真实存储和拼接多媒体路径。
- `content_type`：浏览器展示和下载时使用。
- `file_size`：文件大小。
- `storage_path`：多媒体服务内部存储路径。
- `access_url`：SpringAIDemo 代理访问 URL，如 `/demo-api/api/media/files/12`。
- `source_type`：`USER_UPLOAD` 或 `AI_GENERATED_IMAGE`。

## 后端组件设计

新增 `MediaStorageProperties`：

- 读取 `app.media.base-url`
- 读取 `app.media.username`
- 读取 `app.media.password`
- 读取 `app.media.storage-prefix`

新增 `MediaStorageService`：

- 生成带 `conversationId` 的安全存储路径。
- 生成英文安全文件名，并同时保留原始文件名。
- 上传输入流到多媒体服务。
- 从多媒体服务读取文件流。
- 生成 Basic Auth 请求头。
- 对 path 使用 UTF-8 URL encode。
- 上传返回值为 `"0"` 时视为成功。

新增 `MediaFileRepository`：

- 使用 `JdbcTemplate` 插入媒体元数据。
- 按 `id` 查询媒体元数据。
- 必要时可按 `conversation_id` 查询历史媒体。

新增 `MediaFileSchemaInitializer`：

- 应用启动时创建 `spring_ai_media_file` 表。
- 与当前 `ChatMemorySchemaInitializer` 保持类似风格。

新增 `MediaFileController`：

- 暴露 `/api/media/files/{id}` 和 `/api/media/files/{id}/download`。
- 负责查询元数据、调用 `MediaStorageService` 读取文件流、写回浏览器响应。

调整 `MultimodalChatService`：

- 用户上传附件时，先保存到多媒体服务并写入元数据表。
- 同一份附件字节继续传给 LLM 的 `.media(...)`。
- 使用系统提示词约束 LLM 返回 JSON，而不是把格式规则混入用户消息正文。
- LLM 判断 `generateImage=true` 时调用 `ImageModel`。
- 图片模型返回 URL 时，后端下载图片后上传多媒体服务。
- 图片模型返回 base64 时，后端解码后上传多媒体服务。
- 返回给前端的 `imageUrl` 使用 SpringAIDemo 代理 URL。

新增 `MultimodalChatStreamController` 或在现有控制器中新增流式接口：

- 暴露 `POST /api/chat/multimodal/stream`
- 返回 `text/event-stream`
- 用于向前端持续推送过程状态、文本增量、附件信息和生成图片事件

## 接口响应设计

`POST /api/chat/multimodal` 响应建议扩展为：

```json
{
  "type": "TEXT",
  "reply": "中文回复内容",
  "generateImage": false,
  "imagePrompt": "",
  "imageUrl": null,
  "attachments": [
    {
      "id": 12,
      "fileName": "参考图.jpg",
      "contentType": "image/jpeg",
      "fileSize": 12345,
      "url": "/demo-api/api/media/files/12",
      "downloadUrl": "/demo-api/api/media/files/12/download"
    }
  ],
  "generatedImage": null
}
```

生成图片时：

```json
{
  "type": "IMAGE",
  "reply": "已根据你的描述生成图片。",
  "generateImage": true,
  "imagePrompt": "最终图片生成 prompt",
  "imageUrl": "/demo-api/api/media/files/13",
  "attachments": [],
  "generatedImage": {
    "id": 13,
    "fileName": "generated-13.png",
    "contentType": "image/png",
    "fileSize": 45678,
    "url": "/demo-api/api/media/files/13",
    "downloadUrl": "/demo-api/api/media/files/13/download"
  }
}
```

新增流式接口：

```text
POST /api/chat/multimodal/stream
Content-Type: multipart/form-data
Accept: text/event-stream
Header: X-Session-Id
```

事件类型建议固定为：

```text
status
attachments
delta
image
done
error
```

事件载荷示例：

```json
event: status
data: {"phase":"analyzing","message":"正在分析附件"}
```

```json
event: attachments
data: {"attachments":[{"id":12,"fileName":"方案说明.docx","contentType":"application/vnd.openxmlformats-officedocument.wordprocessingml.document","fileSize":123456,"url":"/demo-api/api/media/files/12","downloadUrl":"/demo-api/api/media/files/12/download"}]}
```

```json
event: delta
data: {"content":"我先帮你总结这个附件的核心内容："}
```

```json
event: status
data: {"phase":"image_generating","message":"正在生成图片"}
```

```json
event: image
data: {"generatedImage":{"id":13,"fileName":"generated-cover.png","contentType":"image/png","fileSize":45678,"url":"/demo-api/api/media/files/13","downloadUrl":"/demo-api/api/media/files/13/download"}}
```

```json
event: done
data: {"type":"IMAGE","generateImage":true}
```

## 前端展示设计

聊天页保持独立滚动布局和统一发送窗口，整体体验参考 ChatGPT：

- 页面主体以聊天消息为中心，不展示接口地址、请求路径、JSON 字段名、调试信息或后端配置。
- 左侧区域只保留会话、新建聊天、预设提示等用户能理解的功能，不出现 `/api/...`、`X-Session-Id`、`multipart/form-data` 等技术文案。
- 顶部标题和提示语使用产品化中文表达，例如“Spring AI 助手”“可以上传附件或描述你想生成的图片”。
- 底部输入区承担文字输入、附件选择、发送三件事，避免拆成多个工具卡片，也不单独展示“附件框”。
- 附件选择入口放在输入框左侧或底部工具栏，用图标按钮触发文件选择，视觉上和 ChatGPT 的上传入口一致。
- 选择附件后，在输入框内部或紧贴输入框上沿显示附件胶囊/缩略片，不额外占用独立面板。
- 接口调用错误转化为用户能理解的提示，例如“发送失败，请稍后重试”，不把后端 URL、堆栈、状态码细节直接展示在主界面。
- 聊天回复区域支持 Markdown、代码块、行内代码、列表、表格和安全白名单 HTML 渲染。
- 流式回复期间，消息气泡内容持续增长，过程状态单独以弱提示样式展示，体验接近 GPT。

发送前：

- 用户选择附件后，输入框内显示一个紧凑附件胶囊，包含文件名、简短类型/大小和移除按钮。
- 图片附件可显示小缩略图，非图片附件显示文件图标和文件名。
- 用户仍可在同一个输入框输入文字说明，比如“根据这个附件生成一张海报”。
- 不出现单独的附件上传框、附件工具卡片或接口调试区域。

发送后：

- 用户消息气泡展示文字。
- 如果选择了附件，等待后端返回 `attachments` 事件后，在聊天记录中展示附件卡片。
- 图片类型附件使用 `<img>` 预览。
- 非图片附件显示文件名、类型、大小和下载按钮。
- 代码块显示语言标签、等宽字体和复制按钮。
- 允许安全白名单 HTML，例如 `p`、`pre`、`code`、`ul`、`ol`、`li`、`table`、`thead`、`tbody`、`tr`、`th`、`td`、`strong`、`em`、`blockquote`、`a`。
- 生成图片结果在收到 `image` 事件后使用代理 URL 展示图片卡片。

接口地址只允许存在于前端 JavaScript 调用代码中，不作为可见文本渲染到页面。

## 数据流

1. 用户在聊天页输入文字并可选附件。
2. 前端优先调用 `POST /api/chat/multimodal/stream`。
3. 后端读取附件字节。
4. 如有附件，先生成英文安全文件名，并按 `conversationId` 路径上传多媒体服务。
5. 后端保存 `original_filename`、`stored_filename`、`storage_path` 等元数据。
6. 后端向前端推送 `attachments` 或 `status(analyzing)` 事件。
7. 后端使用系统提示词 + 用户消息 + 可选附件 `.media(...)` 调用 Chat LLM。
8. 后端边接收模型文本边向前端推送 `delta` 事件，过程状态也通过 `status` 事件推送。
9. 如 `generateImage=false`，推送 `done` 事件结束。
10. 如 `generateImage=true`，先推送 `status(image_generating)`。
11. 后端调用 `ImageModel`，把生成图片上传到多媒体服务并保存元数据。
12. 后端推送 `image` 事件，再推送 `done` 事件。
13. 前端流式渲染文本、代码块、HTML 内容，并在收到媒体事件后插入附件或图片卡片。
14. 浏览器访问代理 URL 时，SpringAIDemo 再转发读取多媒体服务文件流。

## 错误处理

前端：

- 发送中禁用按钮。
- 接口失败时在聊天区显示错误消息。
- 附件上传失败时显示清晰提示。

后端：

- `message` 和 `file` 都为空时返回 `400`。
- 多媒体上传失败时返回明确错误，不继续伪造附件 URL。
- 原始文件名可保留中文，但真实存储名必须转换为英文安全文件名，避免路径兼容性问题。
- LLM 无法解析附件时返回友好的中文说明，不让接口直接崩溃。
- 图片生成失败时返回文字说明，保留已上传附件信息。
- SSE 流式输出中，如中途异常，推送 `error` 事件并关闭流。
- 代理读取文件时，如元数据不存在返回 `404`。
- 多媒体服务读取失败时返回 `502` 或对应错误状态。
- 不在日志中输出 Basic 密码、OpenAI Key 或完整文件内容。

## 测试设计

后端测试：

- 媒体配置能正确绑定。
- `MediaStorageService` 上传时使用正确 URL、Basic Auth、path encode 和 `conversationId` 路径。
- 文件名转换逻辑能同时保留原始文件名并生成英文安全文件名。
- 上传返回 `"0"` 视为成功，其他返回视为失败。
- `MediaFileRepository` 能保存并查询元数据。
- `MediaFileController` 能通过 id 代理返回文件内容。
- `POST /api/chat/multimodal` 有附件时会保存附件元数据并返回代理 URL。
- `POST /api/chat/multimodal/stream` 能按顺序返回 `status`、`delta`、`image`、`done` 等事件。
- 系统提示词中包含 JSON 结构约束，用户消息正文不再混入格式要求。
- `ImageModel` 返回 base64 时能上传生成图片并返回代理 URL。
- 多媒体服务异常时返回可理解错误。

前端验证：

- 页面仍然只有一个统一发送窗口。
- 消息区独立滚动，浏览器窗口不跟随消息区滚动。
- 主界面不展示接口地址、请求路径、JSON 字段或调试信息。
- 附件选择、胶囊展示、移除、发送状态正常。
- 不出现独立附件框，附件状态融入输入框。
- 流式回复能逐步显示文本，而不是等待整段完成后一次性出现。
- Markdown、代码块和安全 HTML 能正确渲染。
- 图片附件可预览。
- 非图片附件可下载。
- AI 生成图片使用代理 URL 正常展示。

## 验收标准

- 浏览器不会看到多媒体服务真实地址和认证信息。
- 主界面对普通用户不暴露接口路径和技术调试信息。
- `application.yml` 中可配置多媒体服务地址、账号、密码和存储前缀。
- 多媒体目录包含会话 ID。
- 元数据表同时保存原始文件名和英文存储文件名。
- 用户上传附件会保存到多媒体服务并写入 MySQL 元数据表。
- AI 生成图片会保存到多媒体服务并写入 MySQL 元数据表。
- 流式聊天接口能输出过程状态、文本增量和图片完成事件。
- 聊天接口返回的是 SpringAIDemo 代理 URL。
- 前端能展示图片附件、下载普通附件、展示生成图片。
- Maven 编译通过。
- 相关后端测试通过。
