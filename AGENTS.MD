# Hephaestus 协作说明

## 文档目的

本文档用于说明 `hephaestus` 仓库的协作边界、默认开发原则、模块职责和当前技术方向，面向开发者与编码代理。

## 仓库整体结构

- `modules/hephaestus-app`
  主应用模块，负责启动、聊天能力、多模态流程、Chat Memory 接线、主聊天页面和对外接口。
- `modules/hephaestus-login`
  登录与系统配置模块，负责登录页、认证接口、Redis Session、登录拦截、系统配置定义和值、配置 Redis 缓存与公开配置接口。
- `modules/hephaestus-org`
  组织设置模块，负责部门 `unit` 树、人员管理、人员头像绑定、当前人员权限范围、设置抽屉和个人信息弹窗前端资源。
- `modules/hephaestus-media`
  媒体模块，负责媒体存储、媒体元数据、媒体控制器和媒体仓储。
- `modules/mybatis`
  轻量 MyBatis 支撑模块，负责通用仓储基座与 SQL 模板。
- `modules/liquibase`
  数据库结构模块，负责 Liquibase 变更集与表结构约束。
  新增字段时要重新写新增的changelog

## 当前技术方向

当前项目采用分层清晰、职责收敛的方式组织数据访问能力：

- `modules/mybatis` 放通用仓储能力
- 业务实体与具体仓储放在业务模块中
- 业务模块按自身场景组织领域对象、服务与控制器
- 持久化实现优先保持简单、明确、可验证
- 登录、系统配置、组织管理、媒体、聊天记忆各自收敛在独立模块，避免把业务逻辑继续堆到启动模块

## 后端模块分层模式

后续后端功能新增或包结构整理时，默认参考 `modules/hephaestus-login` 中 `config`、`auth`、`log` 的分层方式组织代码。除非当前功能非常小且没有持久化或对外接口，否则不要继续把 Controller、Service、DTO、Entity、Repository 平铺在同一个业务包下。

推荐包结构如下：

- `controller`
  放 REST Controller，只负责请求参数接收、响应返回和极薄的流程编排，不承载业务规则。
- `service`
  放业务服务接口，Controller 和其他模块优先依赖接口。
- `service.impl`
  放业务服务实现类，命名为 `XxxServiceImpl`，承载主要业务流程、事务边界和跨仓储编排。
- `dto`
  放请求、响应、分页结果、前端表单模型等接口传输对象。
- `domain`
  放业务实体、数据库映射实体和领域对象。
- `repository`
  放 MyBatis Mapper、仓储接口和与数据库访问直接相关的 SQL。
- `config`
  放 `@ConfigurationProperties`、WebMvc 配置、模块内部 Spring 配置和启动配置。
- `support`
  放辅助组件、解析器、拦截器、调度器、加解密工具、客户端信息解析等非主业务服务。
- `exception`
  放模块内业务异常和异常处理器。
- `constant`
  放枚举、常量类和稳定业务类型定义。

实施约束：

- 新增业务服务时优先定义 `XxxService` 接口，再提供 `XxxServiceImpl` 实现。
- Controller 注入 `service` 接口，不直接依赖 `service.impl`。
- `support` 组件可以被 `service.impl` 编排，但不要反向依赖 Controller。
- DTO 不放业务逻辑；Entity 不直接承担请求响应职责。
- Repository 只处理持久化，不写登录、权限、日志等业务编排逻辑。
- 模块间引用优先引用对外稳定接口、DTO 或领域对象，避免跨模块引用 `impl` 包。
- 重构包结构时保持接口路径、数据库表、配置 key、业务行为不变，先移动结构再用编译验证收口。
- 涉及认证、日志、配置、组织等核心链路时，至少执行 Java 编译验证，确认 Spring Bean 注入、Mapper 扫描和配置属性绑定没有断裂。

## 当前主要功能边界

### 1. 登录与系统配置

- `modules/hephaestus-login` 提供 `/auth/login`、`/auth/me`、`/auth/logout`。
- 登录 Session 使用 Spring Session Redis 保存，超时时间优先读取系统配置项。
- 密码支持前端 Web Crypto RSA 加密传输，后端使用配置项中的私钥解密；配置项缺失时允许使用 YML 或运行期兜底。
- 系统配置采用定义表和值表，配置读取优先使用 Redis 缓存，保存后清理相关缓存。
- “设置 -> 常规 -> 主系统配置”通过动态表单维护配置，前端公开配置读取只返回允许公开且非敏感的配置。
- 登录页支持背景图、网格背景、鼠标拖尾效果，效果类型通过配置项控制。

### 2. 主聊天页面

- 主页面包含会话列表、附件粘贴/上传、流式回复、图片生成状态和生成图片展示。
- 空会话时输入框居中，开始聊天后输入框回到底部。
- 主界面、设置界面和个人信息弹窗共享网格、跑线、鼠标拖影等视觉效果。
- 左下角用户卡片显示当前登录人的头像和姓名，点击后出现个人资料、设置、帮助、退出登录菜单。

### 3. 组织设置与个人信息

- `modules/hephaestus-org` 提供部门 `unit` 树、人员 CRUD、头像上传和当前人员权限范围。
- 组织接口统一从 Redis Session 中的当前登录用户读取 `personId`，前端不再传递或兜底人员身份请求头。
- 设置抽屉包含常规、部门、人员三个入口。
- 部门树只显示部门名称和折叠箭头，节点样式与人员页部门树保持一致；新增/删除部门通过右键菜单触发。
- 人员页按部门树展示人员，右键部门可新增人员，右键人员可删除人员。
- 个人信息是独立小弹窗资源，不放在设置抽屉 tab 内；当前用户可修改姓名、手机号、邮箱、备注和头像。
- 无头像时统一显示蓝色底 + 右下绿色光团 + 首字母的头像；有头像时只显示图片，居中裁剪。

## 四个工作原则

结合你提供的 [README.zh.md](https://github.com/multica-ai/andrej-karpathy-skills/blob/main/README.zh.md) 思路，当前仓库协作默认补充以下四个原则：

### 1. 编码前先思考

先澄清问题，再开始修改，不带着错误假设直接编码。

- 不把不确定内容当成确定事实
- 有歧义时先明确假设或给出分支判断
- 如果存在更简单路径，要主动指出
- 上下文不足时，先补足上下文再动手

### 2. 简洁优先

优先用最少、最直接的代码解决问题，不为了“未来也许会用到”而过度设计。

- 不额外添加需求之外的功能
- 不为单次场景提前抽象过多层次
- 不无端引入新的可配置项、扩展点或框架包装
- 能清楚解决问题的小改动，优先于大而全设计

### 3. 精准修改

只改和当前任务直接相关的代码，不顺手改无关内容。

- 不顺手“优化”邻近代码、注释或格式
- 不重构当前任务未触及的稳定逻辑
- 保持并尊重项目现有风格与结构
- 发现历史问题可以指出，但不默认顺手改掉

### 4. 以结果为导向

把任务转成可验证的结果，而不是停留在模糊动作描述上。

- 先定义什么算完成
- 多步骤任务要有清晰推进顺序
- 每次改动尽量都能对应验证方式
- 优先让结果可检查、可回归、可复现

## 当前已经具备的通用仓储能力

- `BaseAbstractRepository`
  通用仓储基类，提供基础 CRUD 风格入口。
- `BaseInsertTemplate`
  通用批量插入 SQL 模板。
- `BaseUpdateTemplate`
  通用按主键非空更新 SQL 模板。
- `BaseTemplateSupport`
  实体类型、表名、字段元数据解析辅助类。

## 当前主要数据访问链路

### 1. 媒体元数据仓储

- `MediaFileRepository`

当前负责媒体元数据的保存、查询、更新、删除和批量写入。

### 2. Chat Memory 持久化链路

- `MybatisChatMemoryRepository`
- `ChatMemoryMessageRepository`

当前负责会话消息的查询、写入、会话列表读取和会话数据清理。

### 3. 登录与配置链路

- `AuthController`
- `AuthService`
- `LoginRequiredInterceptor`
- `SystemConfigController`
- `SystemConfigService`
- `SystemConfigCacheService`

当前负责登录、退出、当前用户查询、未登录拦截、系统配置动态表单、公开配置读取和 Redis 配置缓存。

### 4. 组织与头像链路

- `OrgUnitController`
- `OrgPersonController`
- `OrgUnitService`
- `OrgPersonService`
- `OrgScopeService`
- `OrgAvatarService`

当前负责部门树、人员列表、当前权限范围、人员头像上传/清空和设置页面组织管理交互。

## 协作边界

### 可以放进 `modules/mybatis` 的内容

- 通用仓储基类
- 通用 SQL Provider
- 与具体业务无关的仓储模板辅助类

### 不应放进 `modules/mybatis` 的内容

- 业务实体
- 业务领域对象
- 业务 Service
- 与具体业务强绑定的处理逻辑

## 开发约定

- 优先先实现真实业务场景，再抽象公共能力
- 只有在至少两个场景可复用时，才考虑继续下沉到 `modules/mybatis`
- MyBatis 扫描范围必须保持收敛，避免把普通接口误注册成 `Mapper Bean`
- MyBatis 扫描默认使用 `@MapperScan(basePackages = "com.example.springaidemo", annotationClass = Mapper.class)` 这一类“全包扫描 + `@Mapper` 过滤”方式，不使用仅靠包路径硬编码逐个维护的方式
- 新增 MyBatis 仓储接口时，统一放在业务模块的 `repository` 包下，命名保持 `*Repository`，并显式标注 `@Mapper`
- 只有真正的 MyBatis Mapper 接口才允许标注 `@Mapper`；普通服务类、适配器类即使名字里带 `Repository` 也不要标注 `@Mapper`
- 对于只有单一业务含义的更新语句，优先保留显式 SQL，不强行套通用模板
- 默认分页查询采用 `modules/mybatis` 中的 `Paging`、`PageInfo`、`Pagination`、`PageSupport`
- 业务侧分页返回优先使用 `Pagination<T>` 或兼容其字段结构，不再为每个模块重复定义一套新的分页结果模型
- 分页查询优先保持“显式 list SQL + 显式 count SQL + `toPagination(...)` 组装”的方式；没有明确收益时，不引入更重的条件 DSL 或分页插件抽象
- 业务仓储做分页组装时，优先复用 `BaseAbstractRepository` 中的 `toPageInfo(...)`、`toPagination(...)` 默认方法，避免重复手写页码归一化和结果封装
- 纯数据库写操作默认在 `service` / `service.impl` 层声明事务边界，统一使用 `@Transactional(rollbackFor = Exception.class)`
- 对创建、更新、删除、批量保存、批量删除、跨多个仓储的数据库写操作，默认要有事务；除非能明确说明该链路故意不需要事务
- 登录日志这类旁路型记录允许不阻断主流程，可保留“写入失败只记录日志、不回滚主业务”的策略，不强制提升为主事务失败条件
- 涉及“外部文件服务 / HTTP 服务 / Redis / Session + 数据库”的混合写链路时，不要把数据库事务误当成完整一致性方案；如需增强一致性，优先设计补偿、幂等或清理策略
- 默认遵循“编码前先思考、简洁优先、精准修改、以结果为导向”四个原则
- 前端静态资源修改后要同步更新 `chat.html` 中相关 `?v=` 版本号，避免浏览器缓存导致视觉验证不一致
- 组织设置相关前端资源保持拆分：设置抽屉用 `org-settings-*`，个人信息弹窗用 `org-profile-*`
- 不要把登录配置敏感项通过公开配置接口暴露给前端

## 验证要求

涉及仓储或持久化修改时，至少要确认：

- Spring Bean 注入没有出现重复装配
- Liquibase 表结构与仓储字段映射一致
- 主键自增回填正常
- 历史数据格式仍可读取
- Chat Memory 多轮对话仍可回放
- 登录拦截不会误拦截登录页、静态资源和公开配置接口
- Redis Session、配置缓存保存和清理逻辑符合预期
- 组织接口的 Session 当前人员数据权限范围正确
- 前端脚本至少执行 `node --check` 验证语法

## 已知约束

- 项目依赖 Java 17、Spring Boot 3、Spring AI 1.1.6 和 MyBatis-Plus 3.5.7
- 如果本地仍使用 Java 8，会导致构建与运行不兼容
- Chat Memory 的 `TOOL` 消息目前是兼容性文本回放，不是完整语义恢复
- 登录私钥属于敏感配置，不能通过公开配置接口返回
- 当前前端仍是原生 HTML/CSS/JS，状态较多，修改 UI 行为时要优先复用现有事件和状态变量
- 组织设置的删除入口当前通过右键菜单触发，避免再恢复左侧常驻删除按钮

## 后续建议顺序

1. 继续完善真实业务仓储
2. 再抽取最小可复用模板
3. 最后才考虑是否需要更重的抽象层

## 说明

本文档只描述当前项目现状、协作边界和开发原则，不记录历史演进来源。
