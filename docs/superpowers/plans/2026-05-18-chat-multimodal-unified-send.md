# 聊天页统一多模态发送实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**目标:** 把聊天页改成统一发送窗口，支持文字、任意附件、多模态 LLM 决策，以及 `ImageModel` 图片生成。

**架构:** 前端只调用一个统一接口 `POST /api/chat/multimodal`。后端新增多模态服务，先调用 Chat LLM 生成结构化决策 JSON；如果 `generateImage=true`，再调用 `ImageModel` 生成图片并统一返回。

**技术栈:** Spring Boot 3.5、Spring AI ChatClient、Spring AI ImageModel、JUnit 5、MockMvc、原生 HTML/CSS/JavaScript。

---

### 任务 1：后端多模态决策服务

**文件:**
- 新建: `src/main/java/olympus/hephaestus/service/MultimodalChatService.java`
- 新建: `src/test/java/olympus/hephaestus/service/MultimodalDecisionParserTest.java`

- [ ] **Step 1: 写 JSON 决策解析测试**

覆盖：

- 标准 JSON：`{"generateImage":true,"reply":"...","imagePrompt":"..."}`
- Markdown 代码块 JSON
- 非 JSON 文本兜底为普通回复

- [ ] **Step 2: 运行测试确认失败**

运行：

```bash
rtk proxy cmd /c "set JAVA_HOME=D:\Program Files\JetBrains\IntelliJ IDEA 2025.2.1\jbr&& set PATH=%JAVA_HOME%\bin;%PATH%&& mvn -q -Dtest=MultimodalDecisionParserTest test"
```

预期：测试编译失败，因为服务类尚不存在。

- [ ] **Step 3: 实现决策解析**

在 `MultimodalChatService` 中实现：

- `parseDecision(String raw)`
- `extractJson(String raw)`
- 返回内部记录类 `Decision(boolean generateImage, String reply, String imagePrompt)`

- [ ] **Step 4: 跑测试确认通过**

运行任务 1 测试命令，预期通过。

### 任务 2：统一多模态接口

**文件:**
- 修改: `src/main/java/olympus/hephaestus/service/MultimodalChatService.java`
- 新建: `src/main/java/olympus/hephaestus/controller/MultimodalChatController.java`
- 新建: `src/test/java/olympus/hephaestus/controller/MultimodalChatControllerTest.java`

- [ ] **Step 1: 写控制器测试**

覆盖：

- `message` 和 `file` 都为空时返回 `400`
- 普通文字返回 `type=TEXT`
- LLM 决策为 `generateImage=true` 时返回 `type=IMAGE` 和 `imageUrl`

- [ ] **Step 2: 实现控制器**

接口：

```text
POST /api/chat/multimodal
Content-Type: multipart/form-data
Header: X-Session-Id
```

字段：

- `message`
- `file`

- [ ] **Step 3: 实现多模态服务调用**

服务逻辑：

- 无附件：调用 ChatClient，要求返回 JSON 决策。
- 有附件：调用 ChatClient，并使用 `.media(MediaType.parseMediaType(contentType), new ByteArrayResource(file.getBytes()))`。
- 决策解析失败：按普通文本回复。
- `generateImage=false`：返回文字。
- `generateImage=true`：调用 `ImageModel` 生成图片。

### 任务 3：前端统一发送窗口

**文件:**
- 修改: `src/main/resources/static/chat.html`

- [ ] **Step 1: 移除旧工具卡片**

移除：

- 图片上传描述卡片
- Agent 生成图片卡片
- 对应 JS 状态和事件

- [ ] **Step 2: 增加附件状态条**

在输入框上方增加：

- 附件选择按钮
- 附件名、大小、类型展示
- 移除附件按钮

- [ ] **Step 3: 改造发送逻辑**

统一发送：

- `FormData`
- `message`
- 可选 `file`
- Header `X-Session-Id`
- 调用 `/api/chat/multimodal`

响应展示：

- `reply` 显示为助手消息
- `imageUrl` 存在时追加图片卡片
- 错误显示为错误消息

### 任务 4：验证

**文件:**
- 不新增文件。

- [ ] **Step 1: 编译**

```bash
rtk proxy cmd /c "set JAVA_HOME=D:\Program Files\JetBrains\IntelliJ IDEA 2025.2.1\jbr&& set PATH=%JAVA_HOME%\bin;%PATH%&& mvn -q -DskipTests compile"
```

- [ ] **Step 2: 跑聚焦测试**

```bash
rtk proxy cmd /c "set JAVA_HOME=D:\Program Files\JetBrains\IntelliJ IDEA 2025.2.1\jbr&& set PATH=%JAVA_HOME%\bin;%PATH%&& mvn -q -Dtest=MultimodalDecisionParserTest,MultimodalChatControllerTest test"
```

- [ ] **Step 3: 启动服务验证**

启动：

```bash
rtk proxy cmd /c "set JAVA_HOME=D:\Program Files\JetBrains\IntelliJ IDEA 2025.2.1\jbr&& set PATH=%JAVA_HOME%\bin;%PATH%&& mvn spring-boot:run"
```

验证：

- `/demo-api/chat.html` 返回 200
- 普通文字发送能返回内容
- 附件 + 文字能进入统一接口
- 要求生成图片时能返回 `imageUrl`

---

## 自审

- 已覆盖统一发送窗口、多模态附件、LLM 决策、ImageModel 图片生成、错误处理和验证要求。
- 没有待定项。
- 当前目录不是 git 仓库，因此不安排 commit 步骤。
