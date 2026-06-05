# Hephaestus 项目说明

## 项目概览

Hephaestus 是一个基于 Spring Boot 3、Spring AI、MySQL 和 Redis 的多模块项目，当前主要围绕以下业务主线展开：

- 对话能力
  普通聊天、会话记忆、多轮会话管理。
- 多模态能力
  附件上传、文本附件参与问答、图片生成、图片流式返回。
- 持久化能力
  Chat Memory、媒体元数据、媒体访问路径、Liquibase 数据库变更管理。
- 登录与配置能力
  登录页、邮箱注册、邮箱验证码找回密码、Redis Session、登录拦截、RSA 密码加密传输、系统配置动态表单、公开配置读取、Redis 配置缓存、登录日志和操作日志。
- 组织管理能力
  部门 `unit` 树、人员管理、岗位管理、岗位权限、人员岗位、头像上传、权限范围、设置抽屉和个人信息弹窗。
- 前端体验能力
  ChatGPT 风格主界面、空会话居中输入、左下角用户菜单、网格/跑线/鼠标拖影视觉效果。

项目根采用 Maven 多模块结构，父工程在 [pom.xml](/E:/NEW_WORK/hephaestus/pom.xml) 中统一管理 Java 17、Spring Boot 3.5.14、Spring AI 1.1.6、Liquibase 5.0.2 和 MyBatis-Plus 3.5.7。

## 模块结构

### `modules/hephaestus-app`

应用主模块，负责：

- Spring Boot 启动入口
  [SpringAiDemoApplication.java](/E:/NEW_WORK/hephaestus/modules/hephaestus-app/src/main/java/olympus/hephaestus/SpringAiDemoApplication.java)
- ChatClient、ChatMemory、聊天配置
  [PersistentChatMemoryConfig.java](/E:/NEW_WORK/hephaestus/modules/hephaestus-app/src/main/java/olympus/hephaestus/config/PersistentChatMemoryConfig.java)
  [ChatService.java](/E:/NEW_WORK/hephaestus/modules/hephaestus-app/src/main/java/olympus/hephaestus/config/ChatService.java)
- 聊天与多模态服务
  [ManagedChatService.java](/E:/NEW_WORK/hephaestus/modules/hephaestus-app/src/main/java/olympus/hephaestus/service/ManagedChatService.java)
  [MultimodalChatService.java](/E:/NEW_WORK/hephaestus/modules/hephaestus-app/src/main/java/olympus/hephaestus/service/MultimodalChatService.java)
  [MultimodalStreamingService.java](/E:/NEW_WORK/hephaestus/modules/hephaestus-app/src/main/java/olympus/hephaestus/service/MultimodalStreamingService.java)
- 对外控制器
  `ChatController`、`AiController`、`ImageController`、`MultimodalChatController`、`MultimodalChatStreamController`
- 静态页面
  `chat.html`、`chat.js`、`chat.css`

当前主页面功能：

- 会话列表、新建会话和多轮聊天
- 附件选择、粘贴图片或文件
- 流式回复、发送中状态条和图片生成状态
- 空会话时输入框居中，开始聊天后输入框回到底部
- 生成图片按消息框大小展示
- 主界面网格、跑线和鼠标拖影视觉效果
- 左下角当前用户头像与姓名，点击展开“个人资料 / 设置 / 帮助 / 退出登录”菜单

### `modules/hephaestus-media`

媒体模块，负责：

- 媒体配置
  [MediaStorageProperties.java](/E:/NEW_WORK/hephaestus/modules/hephaestus-media/src/main/java/olympus/hephaestus/media/config/MediaStorageProperties.java)
- 媒体存储服务
  [MediaStorageService.java](/E:/NEW_WORK/hephaestus/modules/hephaestus-media/src/main/java/olympus/hephaestus/media/service/MediaStorageService.java)
  [MediaStorageServiceImpl.java](/E:/NEW_WORK/hephaestus/modules/hephaestus-media/src/main/java/olympus/hephaestus/media/service/impl/MediaStorageServiceImpl.java)
- 媒体元数据服务
  [MediaFileService.java](/E:/NEW_WORK/hephaestus/modules/hephaestus-media/src/main/java/olympus/hephaestus/media/service/MediaFileService.java)
  [MediaFileServiceImpl.java](/E:/NEW_WORK/hephaestus/modules/hephaestus-media/src/main/java/olympus/hephaestus/media/service/impl/MediaFileServiceImpl.java)
- 媒体领域对象与实体映射
  [MediaFile.java](/E:/NEW_WORK/hephaestus/modules/hephaestus-media/src/main/java/olympus/hephaestus/media/domain/MediaFile.java)
  [StoredMediaFile.java](/E:/NEW_WORK/hephaestus/modules/hephaestus-media/src/main/java/olympus/hephaestus/media/domain/StoredMediaFile.java)
  [MediaFileEntity.java](/E:/NEW_WORK/hephaestus/modules/hephaestus-media/src/main/java/olympus/hephaestus/media/entity/MediaFileEntity.java)
- 媒体仓储与接口
  [MediaFileRepository.java](/E:/NEW_WORK/hephaestus/modules/hephaestus-media/src/main/java/olympus/hephaestus/media/repository/MediaFileRepository.java)
  `MediaFileController`、`MediaAccessController`

### `modules/hephaestus-login`

登录与系统配置模块，负责：

- 登录认证
  [AuthController.java](/E:/NEW_WORK/hephaestus/modules/hephaestus-login/src/main/java/olympus/hephaestus/login/auth/AuthController.java)
  [AuthService.java](/E:/NEW_WORK/hephaestus/modules/hephaestus-login/src/main/java/olympus/hephaestus/login/auth/AuthService.java)
- 登录 Session 与拦截
  [LoginRequiredInterceptor.java](/E:/NEW_WORK/hephaestus/modules/hephaestus-login/src/main/java/olympus/hephaestus/login/auth/LoginRequiredInterceptor.java)
  [LoginWebMvcConfiguration.java](/E:/NEW_WORK/hephaestus/modules/hephaestus-login/src/main/java/olympus/hephaestus/login/auth/LoginWebMvcConfiguration.java)
- 密码加密传输
  [RsaPasswordCryptoService.java](/E:/NEW_WORK/hephaestus/modules/hephaestus-login/src/main/java/olympus/hephaestus/login/auth/RsaPasswordCryptoService.java)
  [LoginPasswordCryptoProperties.java](/E:/NEW_WORK/hephaestus/modules/hephaestus-login/src/main/java/olympus/hephaestus/login/auth/LoginPasswordCryptoProperties.java)
- 系统配置
  [SystemConfigController.java](/E:/NEW_WORK/hephaestus/modules/hephaestus-login/src/main/java/olympus/hephaestus/login/config/controller/SystemConfigController.java)
  [SystemConfigService.java](/E:/NEW_WORK/hephaestus/modules/hephaestus-login/src/main/java/olympus/hephaestus/login/config/service/SystemConfigService.java)
  [SystemConfigCacheService.java](/E:/NEW_WORK/hephaestus/modules/hephaestus-login/src/main/java/olympus/hephaestus/login/config/service/SystemConfigCacheService.java)
  [LoginConfigConst.java](/E:/NEW_WORK/hephaestus/modules/hephaestus-login/src/main/java/olympus/hephaestus/login/config/LoginConfigConst.java)
- 登录页静态资源
  `login.html`、`login.js`、`login.css`

当前登录模块能力：

- `POST /auth/login` 登录
- `GET /auth/me` 当前登录人
- `POST /auth/logout` 退出登录
- `POST /auth/email-code/register` 发送注册邮箱验证码
- `POST /auth/register` 使用邮箱验证码注册账号
- `POST /auth/email-code/reset-password` 发送找回密码邮箱验证码
- `POST /auth/reset-password/verify` 校验找回密码验证码并返回该邮箱下可选账号
- `POST /auth/reset-password` 修改指定账号密码
- 未登录访问页面跳转登录页，未登录访问受保护 API 返回 401
- Session 存入 Redis，登录超时时间由配置项控制
- 配置项优先从配置表读取并写入 Redis 缓存，保存后清理缓存
- `GET /api/system-config/forms/{groupCode}` 返回动态表单
- `PUT /api/system-config/forms/{groupCode}` 保存配置
- `GET /api/system-config/public/{groupCode}` 返回公开且非敏感配置
- `GET /api/logs/login` 查询登录日志
- `GET /api/logs/operations` 查询操作日志
- 登录页支持背景图、网格背景和多种鼠标拖尾效果
- 登录、注册和忘记密码在同一登录页内切换；注册人员统一进入配置的注册部门，并通过人员来源字段标记为注册生成
- 邮箱注册限制为每个邮箱最多注册三个账号；忘记密码流程先验证邮箱和验证码，再选择账号并确认修改密码
- 验证码邮件使用 HTML 格式并说明验证码用途

### `modules/hephaestus-org`

组织设置模块，负责：

- 部门管理
  [OrgUnitController.java](/E:/NEW_WORK/hephaestus/modules/hephaestus-org/src/main/java/olympus/hephaestus/org/controller/OrgUnitController.java)
  [OrgUnitService.java](/E:/NEW_WORK/hephaestus/modules/hephaestus-org/src/main/java/olympus/hephaestus/org/service/OrgUnitService.java)
  [OrgUnitRepository.java](/E:/NEW_WORK/hephaestus/modules/hephaestus-org/src/main/java/olympus/hephaestus/org/repository/OrgUnitRepository.java)
- 人员管理
  [OrgPersonController.java](/E:/NEW_WORK/hephaestus/modules/hephaestus-org/src/main/java/olympus/hephaestus/org/controller/OrgPersonController.java)
  [OrgPersonService.java](/E:/NEW_WORK/hephaestus/modules/hephaestus-org/src/main/java/olympus/hephaestus/org/service/OrgPersonService.java)
  [OrgPersonRepository.java](/E:/NEW_WORK/hephaestus/modules/hephaestus-org/src/main/java/olympus/hephaestus/org/repository/OrgPersonRepository.java)
- 岗位、岗位人员和岗位权限
  [OrgRoleController.java](/E:/NEW_WORK/hephaestus/modules/hephaestus-org/src/main/java/olympus/hephaestus/org/role/controller/OrgRoleController.java)
  [OrgPermissionController.java](/E:/NEW_WORK/hephaestus/modules/hephaestus-org/src/main/java/olympus/hephaestus/org/role/controller/OrgPermissionController.java)
  [OrgRoleService.java](/E:/NEW_WORK/hephaestus/modules/hephaestus-org/src/main/java/olympus/hephaestus/org/role/service/OrgRoleService.java)
  [OrgPersonRoleService.java](/E:/NEW_WORK/hephaestus/modules/hephaestus-org/src/main/java/olympus/hephaestus/org/role/service/OrgPersonRoleService.java)
- 权限范围与头像
  [OrgScopeService.java](/E:/NEW_WORK/hephaestus/modules/hephaestus-org/src/main/java/olympus/hephaestus/org/service/OrgScopeService.java)
  [OrgAvatarService.java](/E:/NEW_WORK/hephaestus/modules/hephaestus-org/src/main/java/olympus/hephaestus/org/service/OrgAvatarService.java)
- 设置抽屉前端资源
  `org-settings-panel.html`、`org-settings.js`、`org-settings.css`
- 岗位设置前端资源
  `org-role-settings.js`
- 个人信息弹窗前端资源
  `org-profile-dialog.html`、`org-profile.js`、`org-profile.css`

当前组织模块能力：

- `GET /api/org/persons/current-scope` 返回当前人员与可管理部门范围
- `GET /api/org/units/tree` 返回权限范围内部门树
- `GET /api/org/roles/tree` 返回按部门组织的岗位树
- `GET /api/org/roles` 查询岗位列表
- `POST /api/org/roles` 新增岗位
- `PUT /api/org/roles/{id}` 修改岗位
- `DELETE /api/org/roles/{id}` 删除岗位
- `PUT /api/org/roles/{id}/people` 保存岗位人员
- `GET /api/org/permissions/current` 返回当前人员权限
- `PUT /api/org/persons/{id}/roles` 保存人员岗位
- 部门新增、编辑、删除
- 人员新增、编辑、删除
- 人员头像上传和访问 URL 回显
- 设置抽屉包含常规、部门、人员、岗位、日志五个入口；没有显示权限的入口不展示
- 部门树节点只显示部门名称和折叠箭头，右键可新增下级部门或删除部门
- 人员页按部门树组织人员，右键部门新增人员，右键人员删除人员
- 人员编辑页展示人员岗位和是否注册人员；已有密码的人员密码框显示 `*******`，保持该值保存不会修改原密码，没有密码的人员保存时必须填写密码
- 岗位页按部门树组织岗位，右键部门新增岗位，右键岗位删除岗位
- 岗位基础属性展示岗位人员列表，岗位人员树支持勾选保存，再次打开会回显已勾选人员
- 岗位权限按五大块组织，栏目显示权限控制入口是否可见，查看、新增、修改、删除、刷新等权限控制按钮和输入框状态
- 人员权限由岗位权限生成，使用 Spring Cache 缓存权限快照；岗位、岗位人员、人员岗位变更后刷新对应人员权限
- 个人信息弹窗可修改当前登录人姓名、手机号、邮箱、备注和头像
- 无头像时显示蓝色底、右下绿色光团和首字母；有头像时只显示居中裁剪后的图片

### `modules/mybatis`

轻量 MyBatis 基础设施模块，负责通用仓储能力与 SQL 模板。

当前包含：

- [HephaestusMybatisAutoConfiguration.java](/E:/NEW_WORK/hephaestus/modules/mybatis/src/main/java/olympus/hephaestus/mybatis/config/HephaestusMybatisAutoConfiguration.java)
- [BaseAbstractRepository.java](/E:/NEW_WORK/hephaestus/modules/mybatis/src/main/java/olympus/hephaestus/mybatis/repository/BaseAbstractRepository.java)
- [BaseInsertTemplate.java](/E:/NEW_WORK/hephaestus/modules/mybatis/src/main/java/olympus/hephaestus/mybatis/repository/BaseInsertTemplate.java)
- [BaseUpdateTemplate.java](/E:/NEW_WORK/hephaestus/modules/mybatis/src/main/java/olympus/hephaestus/mybatis/repository/BaseUpdateTemplate.java)
- [BaseTemplateSupport.java](/E:/NEW_WORK/hephaestus/modules/mybatis/src/main/java/olympus/hephaestus/mybatis/repository/BaseTemplateSupport.java)

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
- `heph_unit`
- `heph_person`
- `heph_role`
- `heph_role_type`
- `heph_permission`
- `heph_role_permission`
- `heph_person_role`
- `heph_person_permission`
- `heph_email_verification_code`
- `heph_login_log`
- `heph_operation_log`
- `sys_config_definition`
- `sys_config_value`

## 当前核心链路

### 1. 普通聊天与会话记忆

主入口：

- [ChatService.java](/E:/NEW_WORK/hephaestus/modules/hephaestus-app/src/main/java/olympus/hephaestus/config/ChatService.java)
- [ManagedChatService.java](/E:/NEW_WORK/hephaestus/modules/hephaestus-app/src/main/java/olympus/hephaestus/service/ManagedChatService.java)

会话记忆使用 `chat_memory_conversation_id` 作为关键上下文参数，把多轮对话关联到数据库中的 `spring_ai_chat_memory`。

### 2. 多模态聊天

主入口：

- [MultimodalChatService.java](/E:/NEW_WORK/hephaestus/modules/hephaestus-app/src/main/java/olympus/hephaestus/service/MultimodalChatService.java)
- [MultimodalStreamingService.java](/E:/NEW_WORK/hephaestus/modules/hephaestus-app/src/main/java/olympus/hephaestus/service/MultimodalStreamingService.java)

主要流程：

- 用户上传附件或输入文本
- 系统按内容类型决定是否直接内联文本、是否走图像生成决策
- 媒体文件落远端存储
- 元数据写入 `spring_ai_media_file`
- 生成图片时回写访问路径并返回给前端

### 3. Media 元数据持久化

当前由 [MediaFileRepository.java](/E:/NEW_WORK/hephaestus/modules/hephaestus-media/src/main/java/olympus/hephaestus/media/repository/MediaFileRepository.java) 负责，具备：

- `saveMediaFile`
- `findById`
- `updateMediaFile`
- `deleteMediaFile`
- `insertList`
- `updateAccessUrl`

### 4. Chat Memory 持久化

当前由以下组件共同负责：

- [ChatMemoryMessageEntity.java](/E:/NEW_WORK/hephaestus/modules/hephaestus-app/src/main/java/olympus/hephaestus/chatmemory/entity/ChatMemoryMessageEntity.java)
- [ChatMemoryMessageRepository.java](/E:/NEW_WORK/hephaestus/modules/hephaestus-app/src/main/java/olympus/hephaestus/chatmemory/repository/ChatMemoryMessageRepository.java)
- [MybatisChatMemoryRepository.java](/E:/NEW_WORK/hephaestus/modules/hephaestus-app/src/main/java/olympus/hephaestus/chatmemory/service/MybatisChatMemoryRepository.java)

配置接线在：

- [PersistentChatMemoryConfig.java](/E:/NEW_WORK/hephaestus/modules/hephaestus-app/src/main/java/olympus/hephaestus/config/PersistentChatMemoryConfig.java)

当前支持：

- `findConversationIds`
- `findByConversationId`
- `saveAll`
- `deleteByConversationId`

兼容性说明：

- 新数据按 Spring AI `MessageType#getValue()` 持久化
- 历史大写枚举值如 `USER`、`ASSISTANT` 读取时仍兼容
- `TOOL` 类型当前先按文本回放为 `AssistantMessage`

### 5. 登录与系统配置

主入口：

- [AuthController.java](/E:/NEW_WORK/hephaestus/modules/hephaestus-login/src/main/java/olympus/hephaestus/login/auth/AuthController.java)
- [AuthService.java](/E:/NEW_WORK/hephaestus/modules/hephaestus-login/src/main/java/olympus/hephaestus/login/auth/AuthService.java)
- [EmailRegisterController.java](/E:/NEW_WORK/hephaestus/modules/hephaestus-login/src/main/java/olympus/hephaestus/login/register/controller/EmailRegisterController.java)
- [EmailRegisterService.java](/E:/NEW_WORK/hephaestus/modules/hephaestus-login/src/main/java/olympus/hephaestus/login/register/service/EmailRegisterService.java)
- [SystemConfigController.java](/E:/NEW_WORK/hephaestus/modules/hephaestus-login/src/main/java/olympus/hephaestus/login/config/controller/SystemConfigController.java)
- [LoginLogController.java](/E:/NEW_WORK/hephaestus/modules/hephaestus-login/src/main/java/olympus/hephaestus/login/log/controller/LoginLogController.java)
- [OperationLogController.java](/E:/NEW_WORK/hephaestus/modules/hephaestus-login/src/main/java/olympus/hephaestus/login/log/controller/OperationLogController.java)

主要流程：

- 登录页读取公开配置，初始化标题、副标题、公钥、背景和拖尾效果
- 前端按登录密码加密模式和公开公钥加密密码
- 后端读取同一加密模式和私钥进行 RSA 解密
- 登录成功后将 `LoginSessionUser` 写入 Redis Session
- 登录拦截器保护主页面和业务 API
- 设置页常规面板读取系统配置动态表单，保存后更新配置值并清理 Redis 缓存
- 注册流程发送邮箱验证码，校验邮箱账号数量限制后创建人员并标记来源为注册生成
- 找回密码流程先发送并校验邮箱验证码，再选择邮箱下账号并确认修改密码
- 登录日志记录登录成功、登录失败和退出登录
- 操作日志记录配置、部门、人员、头像、岗位、岗位权限和岗位人员等管理操作，支持关键词和时间范围查询

### 6. 岗位、权限与组织设置

主入口：

- [OrgUnitController.java](/E:/NEW_WORK/hephaestus/modules/hephaestus-org/src/main/java/olympus/hephaestus/org/controller/OrgUnitController.java)
- [OrgPersonController.java](/E:/NEW_WORK/hephaestus/modules/hephaestus-org/src/main/java/olympus/hephaestus/org/controller/OrgPersonController.java)
- [OrgRoleController.java](/E:/NEW_WORK/hephaestus/modules/hephaestus-org/src/main/java/olympus/hephaestus/org/role/controller/OrgRoleController.java)
- [OrgPermissionController.java](/E:/NEW_WORK/hephaestus/modules/hephaestus-org/src/main/java/olympus/hephaestus/org/role/controller/OrgPermissionController.java)
- [org-settings.js](/E:/NEW_WORK/hephaestus/modules/hephaestus-org/src/main/resources/static/org-settings.js)
- [org-role-settings.js](/E:/NEW_WORK/hephaestus/modules/hephaestus-org/src/main/resources/static/org-role-settings.js)
- [org-profile.js](/E:/NEW_WORK/hephaestus/modules/hephaestus-org/src/main/resources/static/org-profile.js)

主要流程：

- 前端通过 `/auth/me` 获取当前登录人展示信息，组织接口统一由后端从 Redis Session 读取当前登录人的 `personId`
- 设置抽屉打开时按首个可见入口加载对应数据，切换常规、部门、人员、岗位、日志时再请求最新接口
- 部门树右键提供新增下级部门和删除部门
- 人员树按部门展开，搜索时自动过滤并展开匹配节点
- 岗位树按部门展开，右键部门新增岗位，右键岗位删除岗位
- 人员栏只显示已有岗位，人员编辑页可维护人员岗位
- 岗位基础属性页维护岗位信息和权限，岗位人员页通过人员树勾选保存人员
- 保存岗位、保存人员岗位或保存岗位人员后刷新人员权限缓存
- 没有对应权限时栏目不展示，按钮隐藏或输入框置灰；管理员岗位不受岗位权限约束
- 人员头像点击上传，保存后列表刷新
- 个人信息弹窗独立加载当前人员资料，保存后派发 `hephaestus:user-profile-updated` 事件刷新左下角用户卡片

## 当前通用仓储能力

当前项目已经具备以下通用仓储能力：

- 基础 CRUD 仓储入口
- 批量插入模板
- 按主键非空更新模板
- 实体与表元数据解析辅助能力

复合主键关系表如 `heph_person_role`、`heph_role_permission`、`heph_person_permission` 使用显式 SQL Mapper，不继承 `BaseAbstractRepository`，避免 MyBatis-Plus 期望单列 `@TableId` 并在启动时输出主键警告。

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
- 登录密码加密 YML 兜底配置
- 邮箱注册默认部门配置
- 系统配置表中的登录页标题、副标题、背景图、网格开关、鼠标拖尾、会话超时、公私钥等配置

## 前端静态资源说明

当前前端仍为原生 HTML/CSS/JavaScript：

- 主聊天页：`modules/hephaestus-app/src/main/resources/static/chat.*`
- 登录页：`modules/hephaestus-login/src/main/resources/static/login.*`
- 设置抽屉：`modules/hephaestus-org/src/main/resources/static/org-settings-*`
- 岗位设置：`modules/hephaestus-org/src/main/resources/static/org-role-settings.js`
- 个人信息弹窗：`modules/hephaestus-org/src/main/resources/static/org-profile-*`

修改静态资源后需要同步更新 [chat.html](/E:/NEW_WORK/hephaestus/modules/hephaestus-app/src/main/resources/static/chat.html) 中对应 `?v=` 版本号，避免浏览器缓存导致页面仍展示旧样式。

## 测试分布

应用模块：

- [ChatMemoryTest.java](/E:/NEW_WORK/hephaestus/modules/hephaestus-app/src/test/java/olympus/hephaestus/ChatMemoryTest.java)
- [MybatisChatMemoryRepositoryTest.java](/E:/NEW_WORK/hephaestus/modules/hephaestus-app/src/test/java/olympus/hephaestus/chatmemory/service/MybatisChatMemoryRepositoryTest.java)
- 多模态 Controller / Service 测试

媒体模块：

- [MediaFileRepositoryIntegrationTest.java](/E:/NEW_WORK/hephaestus/modules/hephaestus-media/src/test/java/olympus/hephaestus/media/repository/MediaFileRepositoryIntegrationTest.java)
- Controller / Storage Service 测试

Liquibase 模块：

- Schema Contract 测试
- Changelog 结构测试

常用本地验证命令：

```powershell
rtk proxy node --check modules/hephaestus-app/src/main/resources/static/chat.js
rtk proxy node --check modules/hephaestus-login/src/main/resources/static/login.js
rtk proxy node --check modules/hephaestus-org/src/main/resources/static/org-settings.js
rtk proxy node --check modules/hephaestus-org/src/main/resources/static/org-profile.js
rtk proxy powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\mvn-java21.ps1 -pl modules/hephaestus-app -am -DskipTests compile
```

## 当前风险与注意点

- 本地构建环境如果仍是 Java 8，会与 Spring Boot 3、Spring AI 1.1.6、MyBatis Boot 3 不兼容
- [application.yml](/E:/NEW_WORK/hephaestus/modules/hephaestus-app/src/main/resources/application.yml) 当前包含敏感配置项，实际协作时建议尽快改为环境变量或外部配置
- Chat Memory 的 `TOOL` 消息目前是兼容性文本回放，不是完整语义恢复
- MyBatis 扫描范围必须保持收敛，避免把普通接口误注册成 Mapper Bean
- `AutoConfiguration.imports` 必须保存为 UTF-8 无 BOM，否则 Spring Boot 会把 BOM 当成自动配置类名的一部分导致启动失败
- 私钥属于敏感配置，不能通过公开配置接口返回
- 组织设置依赖 Redis Session 中当前登录人的 `personId` 计算权限范围；前端不再传递人员 ID，系统自动任务统一使用系统机器人身份
- 岗位权限缓存使用 Spring Cache，修改岗位、人员岗位或岗位人员后需要刷新对应人员权限，避免前端权限入口和后端校验不一致
- 设置抽屉、个人信息弹窗和主界面共用网格/跑线/拖影效果，调整层级时要避免遮挡弹窗或主聊天操作

## 建议的协作方向

后续如果继续扩展，建议顺序是：

1. 先完善真实业务仓储
2. 再把复用逻辑下沉到 `modules/mybatis`
3. 最后才考虑是否需要更重的抽象层

## 说明

本文档只描述当前项目现状、模块职责和使用方式，不记录历史演进来源。
