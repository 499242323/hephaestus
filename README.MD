# Hephaestus 项目说明

## 项目概览

Hephaestus 是一个基于 Spring Boot 3、Spring AI 和 MySQL 的多模块后端项目，当前主要围绕三条业务主线展开：

- 对话能力
  普通聊天、会话记忆、多轮会话管理。
- 多模态能力
  附件上传、文本附件参与问答、图片生成、图片流式返回。
- 持久化能力
  Chat Memory、媒体元数据、媒体访问路径、Liquibase 数据库变更管理。

项目根采用 Maven 多模块结构，父工程在 [pom.xml](/E:/NEW_WORK/hephaestus/pom.xml) 中统一管理 Java 17、Spring Boot 3.5.14、Spring AI 1.1.6、Liquibase 5.0.2 和 MyBatis-Plus 3.5.7。

## 模块结构

### `modules/hephaestus-app`

应用主模块，负责：

- Spring Boot 启动入口
  [SpringAiDemoApplication.java](/E:/NEW_WORK/hephaestus/modules/hephaestus-app/src/main/java/com/example/springaidemo/SpringAiDemoApplication.java)
- ChatClient、ChatMemory、聊天配置
  [PersistentChatMemoryConfig.java](/E:/NEW_WORK/hephaestus/modules/hephaestus-app/src/main/java/com/example/springaidemo/config/PersistentChatMemoryConfig.java)
  [ChatService.java](/E:/NEW_WORK/hephaestus/modules/hephaestus-app/src/main/java/com/example/springaidemo/config/ChatService.java)
- 聊天与多模态服务
  [ManagedChatService.java](/E:/NEW_WORK/hephaestus/modules/hephaestus-app/src/main/java/com/example/springaidemo/service/ManagedChatService.java)
  [MultimodalChatService.java](/E:/NEW_WORK/hephaestus/modules/hephaestus-app/src/main/java/com/example/springaidemo/service/MultimodalChatService.java)
  [MultimodalStreamingService.java](/E:/NEW_WORK/hephaestus/modules/hephaestus-app/src/main/java/com/example/springaidemo/service/MultimodalStreamingService.java)
- 对外控制器
  `ChatController`、`AiController`、`ImageController`、`MultimodalChatController`、`MultimodalChatStreamController`
- 静态页面
  `chat.html`、`chat.js`、`chat.css`

### `modules/hephaestus-media`

媒体模块，负责：

- 媒体配置
  [MediaStorageProperties.java](/E:/NEW_WORK/hephaestus/modules/hephaestus-media/src/main/java/com/example/springaidemo/media/config/MediaStorageProperties.java)
- 媒体存储服务
  [MediaStorageService.java](/E:/NEW_WORK/hephaestus/modules/hephaestus-media/src/main/java/com/example/springaidemo/media/service/MediaStorageService.java)
  [MediaStorageServiceImpl.java](/E:/NEW_WORK/hephaestus/modules/hephaestus-media/src/main/java/com/example/springaidemo/media/service/impl/MediaStorageServiceImpl.java)
- 媒体元数据服务
  [MediaFileService.java](/E:/NEW_WORK/hephaestus/modules/hephaestus-media/src/main/java/com/example/springaidemo/media/service/MediaFileService.java)
  [MediaFileServiceImpl.java](/E:/NEW_WORK/hephaestus/modules/hephaestus-media/src/main/java/com/example/springaidemo/media/service/impl/MediaFileServiceImpl.java)
- 媒体领域对象与实体映射
  [MediaFile.java](/E:/NEW_WORK/hephaestus/modules/hephaestus-media/src/main/java/com/example/springaidemo/media/domain/MediaFile.java)
  [StoredMediaFile.java](/E:/NEW_WORK/hephaestus/modules/hephaestus-media/src/main/java/com/example/springaidemo/media/domain/StoredMediaFile.java)
  [MediaFileEntity.java](/E:/NEW_WORK/hephaestus/modules/hephaestus-media/src/main/java/com/example/springaidemo/media/entity/MediaFileEntity.java)
- 媒体仓储与接口
  [MediaFileRepository.java](/E:/NEW_WORK/hephaestus/modules/hephaestus-media/src/main/java/com/example/springaidemo/media/repository/MediaFileRepository.java)
  `MediaFileController`、`MediaAccessController`

### `modules/mybatis`

轻量 MyBatis 基础设施模块，负责通用仓储能力与 SQL 模板。

当前包含：

- [HephaestusMybatisAutoConfiguration.java](/E:/NEW_WORK/hephaestus/modules/mybatis/src/main/java/com/example/springaidemo/mybatis/config/HephaestusMybatisAutoConfiguration.java)
- [BaseAbstractRepository.java](/E:/NEW_WORK/hephaestus/modules/mybatis/src/main/java/com/example/springaidemo/mybatis/repository/BaseAbstractRepository.java)
- [BaseInsertTemplate.java](/E:/NEW_WORK/hephaestus/modules/mybatis/src/main/java/com/example/springaidemo/mybatis/repository/BaseInsertTemplate.java)
- [BaseUpdateTemplate.java](/E:/NEW_WORK/hephaestus/modules/mybatis/src/main/java/com/example/springaidemo/mybatis/repository/BaseUpdateTemplate.java)
- [BaseTemplateSupport.java](/E:/NEW_WORK/hephaestus/modules/mybatis/src/main/java/com/example/springaidemo/mybatis/repository/BaseTemplateSupport.java)

### `modules/liquibase`

数据库结构模块，负责：

- Chat Memory 表定义
- Media File 表定义
- Schema Contract 相关测试

关键文件：

- [db.changelog.xml](/E:/NEW_WORK/hephaestus/modules/liquibase/src/main/resources/db/changelog/db.changelog.xml)

当前主要表：

- `spring_ai_chat_memory`
- `spring_ai_media_file`

## 当前核心链路

### 1. 普通聊天与会话记忆

主入口：

- [ChatService.java](/E:/NEW_WORK/hephaestus/modules/hephaestus-app/src/main/java/com/example/springaidemo/config/ChatService.java)
- [ManagedChatService.java](/E:/NEW_WORK/hephaestus/modules/hephaestus-app/src/main/java/com/example/springaidemo/service/ManagedChatService.java)

会话记忆使用 `chat_memory_conversation_id` 作为关键上下文参数，把多轮对话关联到数据库中的 `spring_ai_chat_memory`。

### 2. 多模态聊天

主入口：

- [MultimodalChatService.java](/E:/NEW_WORK/hephaestus/modules/hephaestus-app/src/main/java/com/example/springaidemo/service/MultimodalChatService.java)
- [MultimodalStreamingService.java](/E:/NEW_WORK/hephaestus/modules/hephaestus-app/src/main/java/com/example/springaidemo/service/MultimodalStreamingService.java)

主要流程：

- 用户上传附件或输入文本
- 系统按内容类型决定是否直接内联文本、是否走图像生成决策
- 媒体文件落远端存储
- 元数据写入 `spring_ai_media_file`
- 生成图片时回写访问路径并返回给前端

### 3. Media 元数据持久化

当前由 [MediaFileRepository.java](/E:/NEW_WORK/hephaestus/modules/hephaestus-media/src/main/java/com/example/springaidemo/media/repository/MediaFileRepository.java) 负责，具备：

- `saveMediaFile`
- `findById`
- `updateMediaFile`
- `deleteMediaFile`
- `insertList`
- `updateAccessUrl`

### 4. Chat Memory 持久化

当前由以下组件共同负责：

- [ChatMemoryMessageEntity.java](/E:/NEW_WORK/hephaestus/modules/hephaestus-app/src/main/java/com/example/springaidemo/chatmemory/entity/ChatMemoryMessageEntity.java)
- [ChatMemoryMessageRepository.java](/E:/NEW_WORK/hephaestus/modules/hephaestus-app/src/main/java/com/example/springaidemo/chatmemory/repository/ChatMemoryMessageRepository.java)
- [MybatisChatMemoryRepository.java](/E:/NEW_WORK/hephaestus/modules/hephaestus-app/src/main/java/com/example/springaidemo/chatmemory/service/MybatisChatMemoryRepository.java)

配置接线在：

- [PersistentChatMemoryConfig.java](/E:/NEW_WORK/hephaestus/modules/hephaestus-app/src/main/java/com/example/springaidemo/config/PersistentChatMemoryConfig.java)

当前支持：

- `findConversationIds`
- `findByConversationId`
- `saveAll`
- `deleteByConversationId`

兼容性说明：

- 新数据按 Spring AI `MessageType#getValue()` 持久化
- 历史大写枚举值如 `USER`、`ASSISTANT` 读取时仍兼容
- `TOOL` 类型当前先按文本回放为 `AssistantMessage`

## 当前通用仓储能力

当前项目已经具备以下通用仓储能力：

- 基础 CRUD 仓储入口
- 批量插入模板
- 按主键非空更新模板
- 实体与表元数据解析辅助能力

## 配置与运行

主配置文件：

- [application.yml](/E:/NEW_WORK/hephaestus/modules/hephaestus-app/src/main/resources/application.yml)

当前配置涵盖：

- 服务端口与 Context Path
- OpenAI / Image Model 配置
- MySQL 数据源
- Redis
- Liquibase
- 媒体服务外部地址、用户名、密码、访问前缀

## 测试分布

应用模块：

- [ChatMemoryTest.java](/E:/NEW_WORK/hephaestus/modules/hephaestus-app/src/test/java/com/example/springaidemo/ChatMemoryTest.java)
- [MybatisChatMemoryRepositoryTest.java](/E:/NEW_WORK/hephaestus/modules/hephaestus-app/src/test/java/com/example/springaidemo/chatmemory/service/MybatisChatMemoryRepositoryTest.java)
- 多模态 Controller / Service 测试

媒体模块：

- [MediaFileRepositoryIntegrationTest.java](/E:/NEW_WORK/hephaestus/modules/hephaestus-media/src/test/java/com/example/springaidemo/media/repository/MediaFileRepositoryIntegrationTest.java)
- Controller / Storage Service 测试

Liquibase 模块：

- Schema Contract 测试
- Changelog 结构测试

## 当前风险与注意点

- 本地构建环境如果仍是 Java 8，会与 Spring Boot 3、Spring AI 1.1.6、MyBatis Boot 3 不兼容
- [application.yml](/E:/NEW_WORK/hephaestus/modules/hephaestus-app/src/main/resources/application.yml) 当前包含敏感配置项，实际协作时建议尽快改为环境变量或外部配置
- Chat Memory 的 `TOOL` 消息目前是兼容性文本回放，不是完整语义恢复
- MyBatis 扫描范围必须保持收敛，避免把普通接口误注册成 Mapper Bean

## 建议的协作方向

后续如果继续扩展，建议顺序是：

1. 先完善真实业务仓储
2. 再把复用逻辑下沉到 `modules/mybatis`
3. 最后才考虑是否需要更重的抽象层

## 说明

本文档只描述当前项目现状、模块职责和使用方式，不记录历史演进来源。
