# Chat Fix PRD

**Date**: 2026-05-19
**Topic**: 聊天附件 413 / Ctrl+V 粘贴 / 窗口布局修复

## Goal

修复 Hephaestus 聊天页面的四个问题：添加附件点击发送后出现 413 报错、Ctrl+V 无法粘贴附件、聊天窗口过大铺满屏幕、输入窗口过高。

## Context

- 前端：[chat.html](E:\NEW_WORK\hephaestus\modules\hephaestus-app\src\main\resources\static\chat.html)
- 后端接口：MultimodalChatStreamController.stream() → MultimodalStreamingService.stream()
- 附件上限由 spring.servlet.multipart.max-file-size 控制，当前未配置，使用 Spring Boot 默认值 1MB
- 前端仅通过 <input type=file> 选文件，无 paste 事件监听
- 布局为全屏 grid，消息区和输入区均无宽度限制
- textarea min-height: 70px，初始显示过高

## Repository Findings

| 文件 | 现状 |
|---|---|
| pplication.yml | 无 spring.servlet.multipart.* 配置，默认 1MB |
| chat.html | 无 paste 事件、.main 全屏宽、	extarea min-height: 70px |
| MultimodalChatStreamController.java | 接口正常，未做文件大小前置校验 |
| MultimodalStreamingService.java | 服务层正常，未做文件大小校验 |

## Design

### 1. 后端文件大小限制 → 2MB

在 pplication.yml 新增：

`yaml
spring:
  servlet:
    multipart:
      max-file-size: 2MB
      max-request-size: 2MB
`

### 2. 前端文件大小拦截

sendMessage() 开头新增校验：selectedFile.size > 2 * 1024 * 1024 时直接在前端报错，不发起请求。

### 3. Ctrl+V 粘贴附件

新增 document 级 paste 事件监听：
- 从 clipboardData.items 提取文件
- 赋值给 selectedFile，调用 showAttachmentChip(file)
- 纯文本粘贴不做处理

### 4. 聊天窗口居中窄栏

- .main：max-width: 768px; margin: 0 auto; width: 100%
- .messages：padding: 24px 16px
- .bubble：max-width: 100%
- .composer-wrap：max-width: 100%

### 5. 输入框高度降低

- textarea min-height 从 70px 改为 24px

## TDD Plan

| 场景 | 验证方式 |
|---|---|
| 413 → 2MB 上限 | 发送 1.5MB 附件期望 200 OK |
| 前端超 2MB 拦截 | 选择 >2MB 文件，期望前端报错提示 |
| Ctrl+V 粘贴图片 | 截图后 Ctrl+V，附件 chip 显示缩略图 |
| Ctrl+V 粘贴文件 | 资源管理器复制文件后 Ctrl+V，附件 chip 显示文件名 |
| 聊天窗口居中 | 窗口 >768px 时消息区居中，不铺满 |
| 输入框收敛 | 空输入框仅一行高度 |

## Risks

- max-width: 768px 可能在大屏上显得偏窄，后续可按需调大或使用 clamp()
- 粘贴事件在 document 级监听可能与未来其他页面组件冲突，需限定作用域
- 无后端单元测试覆盖 multipart 配置变更，以手动验证替代

## Verification

- 启动应用后发送 1.5MB 附件，确认不再 413
- 发送 >2MB 附件，确认前端拦截提示
- Ctrl+V 粘贴图片/文件，确认附件 chip 正确展示
- 调整浏览器窗口宽度，确认消息区在 >768px 时居中
- 空输入框高度为单行
