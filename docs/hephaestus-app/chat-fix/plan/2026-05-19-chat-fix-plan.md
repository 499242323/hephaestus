# Chat Fix Implementation Plan

**Date**: 2026-05-19
**Based on PRD**: docs/hephaestus-app/chat-fix/prd/2026-05-19-chat-fix-prd.md

## Steps

### Step 1: 后端 multipart 上限 2MB
- 文件: modules/hephaestus-app/src/main/resources/application.yml
- 操作: 在 spring: 下新增 servlet.multipart.max-file-size: 2MB 和 max-request-size: 2MB
- 验证: 重启应用后发送 1.5MB 附件不 413

### Step 2: chat.html 布局修复（聊天窗口居中 + 输入框降低）
- 文件: modules/hephaestus-app/src/main/resources/static/chat.html
- CSS 变更:
  - .main: 新增 max-width: 768px; margin: 0 auto; width: 100%
  - .messages: padding 从 28px 32px 12px 改为 24px 16px 10px
  - .bubble: max-width 从 min(780px, 86%) 改为 100%
  - .composer-wrap: max-width: 820px 改为 100%
  - 	extarea min-height: 从 70px 改为 24px
- 验证: 浏览器检查布局居中、输入框单行

### Step 3: chat.html Ctrl+V 粘贴附件
- 文件: modules/hephaestus-app/src/main/resources/static/chat.html
- JS 变更: 
  - 新增 document.addEventListener(paste, ...) 处理函数
  - 从 clipboardData 提取文件，赋值给 selectedFile 并更新附件 chip
  - 纯文本粘贴不做拦截
- 验证: 截图后 Ctrl+V 出现附件 chip

### Step 4: chat.html 前端超 2MB 拦截
- 文件: modules/hephaestus-app/src/main/resources/static/chat.html
- JS 变更:
  - sendMessage() 开头新增: if (selectedFile && selectedFile.size > 2*1024*1024) → 显示 error 气泡并 return
- 验证: 选择 >2MB 文件点发送，前端报错提示

## Verification Checklist

- [ ] 1.5MB 附件发送成功，不 413
- [ ] >2MB 附件前端拦截提示
- [ ] Ctrl+V 图片粘贴附件 chip 正确
- [ ] Ctrl+V 文件粘贴附件 chip 正确
- [ ] 聊天窗口居中（>768px 时）
- [ ] 空输入框单行高度
- [ ] 移动端布局不退化
