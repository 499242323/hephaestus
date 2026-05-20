# 聊天页图片工具实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**目标:** 为现有聊天 Demo 增加独立滚动、图片上传描述、agent 图片生成功能。

**架构:** 后端新增一个 `ImageController`，提供图片描述和图片生成两个接口；前端继续使用 `src/main/resources/static/chat.html`，在输入区上方加入两个工具卡片。图片描述第一版基于文件元信息和前端传入宽高生成中文描述，图片生成第一版返回确定性的 SVG data URL。

**技术栈:** Spring Boot 3.5、Spring MVC、JUnit 5、MockMvc、原生 HTML/CSS/JavaScript。

---

### 任务 1：后端图片接口

**文件:**
- 新建: `src/main/java/com/example/springaidemo/controller/ImageController.java`
- 新建: `src/test/java/com/example/springaidemo/controller/ImageControllerTest.java`

- [ ] **Step 1: 编写失败测试**

测试 `POST /api/images/describe` 可以接收图片并返回描述；非图片返回 `400`；`POST /api/images/generate` 能返回 SVG data URL；空 prompt 返回 `400`。

- [ ] **Step 2: 运行测试确认失败**

运行：

```bash
rtk proxy cmd /c "set JAVA_HOME=D:\Program Files\JetBrains\IntelliJ IDEA 2025.2.1\jbr&& set PATH=%JAVA_HOME%\bin;%PATH%&& mvn -q -Dtest=ImageControllerTest test"
```

预期：编译失败或测试失败，因为 `ImageController` 尚不存在。

- [ ] **Step 3: 实现 `ImageController`**

接口：

- `POST /api/images/describe`
  - multipart 参数：`file`
  - 可选参数：`width`、`height`
  - 返回：`fileName`、`imageType`、`fileSize`、`width`、`height`、`description`
- `POST /api/images/generate`
  - JSON 请求：`{ "prompt": "..." }`
  - 返回：`prompt`、`imageUrl`、`summary`

- [ ] **Step 4: 运行测试确认通过**

运行任务 1 的测试命令，预期测试通过。

### 任务 2：聊天页独立滚动与工具区

**文件:**
- 修改: `src/main/resources/static/chat.html`

- [ ] **Step 1: 调整 CSS 布局**

设置：

- `html, body { height: 100%; overflow: hidden; }`
- `.app { height: 100vh; overflow: hidden; }`
- `.main { min-height: 0; }`
- `.messages { min-height: 0; overflow-y: auto; }`
- 移动端保持可用布局。

- [ ] **Step 2: 增加图片工具区 HTML**

在 `<form id="chatForm" class="composer">` 内、`textarea` 上方增加：

- 图片描述卡片：文件选择、预览、描述按钮、加入对话按钮。
- 图片生成卡片：prompt 输入、生成按钮、图片预览、加入对话按钮。

- [ ] **Step 3: 增加前端 JavaScript**

实现：

- 选择图片后读取文件名、大小、类型，并用 `Image` 对象读取宽高和预览 URL。
- 点击“描述图片”调用 `POST /api/images/describe`。
- 点击“生成图片”调用 `POST /api/images/generate`。
- 工具结果可追加到聊天 textarea。
- 工具请求失败时只在对应工具卡片显示错误。

### 任务 3：集成验证

**文件:**
- 不新增文件。

- [ ] **Step 1: 编译**

运行：

```bash
rtk proxy cmd /c "set JAVA_HOME=D:\Program Files\JetBrains\IntelliJ IDEA 2025.2.1\jbr&& set PATH=%JAVA_HOME%\bin;%PATH%&& mvn -q -DskipTests compile"
```

预期：退出码 0。

- [ ] **Step 2: 启动服务**

运行：

```bash
rtk proxy cmd /c "set JAVA_HOME=D:\Program Files\JetBrains\IntelliJ IDEA 2025.2.1\jbr&& set PATH=%JAVA_HOME%\bin;%PATH%&& mvn spring-boot:run"
```

预期：服务启动在 `http://localhost:11018/demo-api`。

- [ ] **Step 3: 验证接口**

用脚本或 Node fetch 验证：

- `GET /demo-api/chat.html` 返回 200。
- `POST /demo-api/api/images/generate` 返回 `imageUrl`。
- `POST /demo-api/api/images/describe` 上传图片返回 `description`。

- [ ] **Step 4: 浏览器/页面验证**

验证：

- 页面主体不随聊天消息滚动。
- 图片上传描述结果可加入聊天输入框。
- 图片生成结果显示为图片卡片。
- 原有 `/api/chat/send` 仍可正常发送。

### 任务 4：收尾

**文件:**
- 检查所有已修改文件。

- [ ] **Step 1: 停止本地服务**

停止本次验证启动的 Spring Boot 进程。

- [ ] **Step 2: 汇总结果**

向用户说明：

- 修改了哪些文件。
- 哪些命令验证通过。
- 如存在外部 AI 服务不稳定，说明实际状态。

---

## 自审

- 设计文档中的独立滚动、图片上传描述、图片生成、错误处理、测试要求都已映射到任务。
- 计划不包含待定项。
- 当前目录不是 git 仓库，因此不安排 commit 步骤。
