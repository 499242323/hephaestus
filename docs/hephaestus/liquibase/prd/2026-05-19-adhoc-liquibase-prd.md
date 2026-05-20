# Hephaestus Liquibase 接入设计

## Goal

在 `E:\NEW_WORK\demo` 内将 Liquibase 能力拆成一个独立 Maven 子模块 `modules/liquibase`，替换现有启动期手工建表和补字段逻辑，让数据库结构变更统一由 Liquibase 管理，并可作为单独依赖复用。

本次设计同时满足以下额外约束：

- 聊天记忆表与媒体表统一采用小写表名。
- Liquibase 使用截至 `2026-05-19` 的最新稳定社区版。
- 当前阶段仅维护一个 changelog 文件。

## Context

当前工程目前是单模块 Maven 项目，核心数据库初始化现状如下：

- [`pom.xml`](/E:/NEW_WORK/demo/pom.xml) 目前未引入 Liquibase 依赖。
- [`src/main/resources/application.yml`](/E:/NEW_WORK/demo/src/main/resources/application.yml) 中 `spring.ai.chat.memory.repository.jdbc.initialize-schema` 当前配置为 `always`。
- [`src/main/java/com/example/springaidemo/config/ChatMemorySchemaInitializer.java`](/E:/NEW_WORK/demo/src/main/java/com/example/springaidemo/config/ChatMemorySchemaInitializer.java) 在应用启动时对 `SPRING_AI_CHAT_MEMORY` 做增量 DDL。
- [`src/main/java/com/example/springaidemo/media/MediaFileSchemaInitializer.java`](/E:/NEW_WORK/demo/src/main/java/com/example/springaidemo/media/MediaFileSchemaInitializer.java) 在应用启动时创建 `spring_ai_media_file` 并兼容补列。
- 当前对应测试集中在 [`src/test/java/com/example/springaidemo/config/ChatMemorySchemaInitializerTest.java`](/E:/NEW_WORK/demo/src/test/java/com/example/springaidemo/config/ChatMemorySchemaInitializerTest.java) 和 [`src/test/java/com/example/springaidemo/media/MediaFileSchemaInitializerTest.java`](/E:/NEW_WORK/demo/src/test/java/com/example/springaidemo/media/MediaFileSchemaInitializerTest.java)。

参考项目 `E:\NEW_WORK\wizdom-urban-v14-framework` 已经接入 Liquibase，并通过 changelog 文件管理数据库变更；但当前项目体量明显更小，本次不采用其超大 changelog 组织方式，而是保留“独立 Liquibase 子模块 + 单 changelog 文件”的最小可维护方案。

## Constraints

- 必须在当前工程 `E:\NEW_WORK\demo` 内实现，并将 Liquibase 抽成独立 Maven 子模块 `modules/liquibase`。
- 必须遵循单个 changelog 文件方案，不拆 master/schema/patch 多文件。
- 必须关闭 Spring AI 的 JDBC 自动建表，让 Liquibase 成为唯一 DDL 入口。
- 必须统一使用小写表名：`spring_ai_chat_memory`、`spring_ai_media_file`。
- 必须兼容空库首次启动和旧库平滑升级两类场景。
- 由于 Maven 多模块聚合要求父工程使用 `pom` packaging，本次方案必须接受工程结构调整。
- 设计阶段只产出 PRD，不进入实现。

## Done When

满足以下条件后，本次设计视为完成：

- 已明确父聚合 POM、应用模块、`modules/liquibase` 模块、`application.yml`、单 changelog 文件和自定义 chat memory dialect 的目标改动。
- 已明确如何从旧的两个 schema initializer 平滑迁移到 Liquibase。
- 已明确小写表名方案的技术实现方式和风险。
- 已明确 TDD 测试切换方案、验证命令和实施边界。

## Repository Findings

### 1. 当前项目是单模块 Spring Boot 应用

- `pom.xml` 当前是可执行应用 POM，不是多模块聚合 POM。
- 现有依赖包含 Spring AI JDBC chat memory starter、MySQL 驱动、Redis 等，但不包含 Liquibase。

### 2. 要做真正的 `modules/liquibase` 子模块，必须调整 Maven 结构

- Maven 官方文档说明，多模块聚合项目要求父工程使用 `pom` packaging。
- 因此，如果要把 Liquibase 做成真正的 `modules/liquibase` Maven 子模块，而不是同仓库里的独立目录或普通源码包，则当前根工程不能继续同时承担可执行应用模块角色。
- 这意味着需要把当前应用源码迁移到独立应用模块，例如：
  - `modules/hephaestus-app`
  - `modules/liquibase`

### 3. 当前 DDL 入口分散在两个 ApplicationRunner

- `ChatMemorySchemaInitializer` 负责：
  - 检查表是否存在。
  - 补 `type` 列。
  - 补 `timestamp` 列。
  - 放宽 `message_type` 列。
  - 补 `(conversation_id, timestamp)` 索引。
- `MediaFileSchemaInitializer` 负责：
  - 创建 `spring_ai_media_file` 表。
  - 补 `stored_filename` 列。

### 4. Spring AI 默认表名与本次小写约束冲突

- Spring AI 官方文档说明，JDBC chat memory 自动建表默认目标是 `SPRING_AI_CHAT_MEMORY`。
- `JdbcChatMemoryRepository.Builder` 官方 API 只暴露 `jdbcTemplate`、`dataSource`、`dialect`、`transactionManager` 等入口，没有直接设置表名的方法。
- 因此，如果要把表名统一为小写，不能只改 changelog，还必须在应用侧提供一个自定义 JDBC dialect，让仓储 SQL 统一指向 `spring_ai_chat_memory`。

### 5. Spring AI 官方文档支持由 Liquibase 接管 schema

- 官方文档说明 `spring.ai.chat.memory.repository.jdbc.initialize-schema=never` 适用于使用 Flyway/Liquibase 的场景。
- 这意味着本项目关闭自动建表后，由 Liquibase 完整接管 chat memory 表结构是官方支持路径。

### 6. 参考项目已采用 Liquibase 依赖与 changelog 资源

- 参考项目根 POM 已引入 `egova-liquibase`。
- 参考项目模块资源目录下已有 `*-db-changelog.xml` 文件作为数据库变更入口。
- 但参考项目 changelog 体量较大，不适合直接照搬到当前 demo 规模工程。

## Design

### 1. 接入边界

将当前仓库调整为真正的多模块结构，而不是在单模块应用里放一个“伪插件目录”：

- 根目录 `pom.xml` 改为父聚合 POM，`packaging` 改为 `pom`。
- 新增应用模块，例如 `modules/hephaestus-app`，承载现有 `src/main`、`src/test`、`application.yml` 和 Spring Boot 启动类。
- 新增 `modules/liquibase` 模块，承载：
  - Liquibase 依赖
  - `db.changelog.xml`
  - 自定义 chat memory 小写表名 dialect
  - 可选的自动配置类
- 应用模块通过普通 Maven 依赖引入 `modules/liquibase`，不再自己持有 DDL 逻辑。

这样做的好处是：

- `modules/liquibase` 是真实可发布、可复用的 Maven artifact。
- Liquibase 逻辑与业务应用彻底解耦。
- 后续其他模块或服务如要复用数据库变更能力，可以直接依赖该模块。

### 2. 依赖与版本策略

在 `modules/liquibase/pom.xml` 中新增：

- `org.liquibase:liquibase-core`

版本策略：

- 采用截至 `2026-05-19` 核实到的最新稳定社区版 `5.0.2`。
- 不使用 nightly 版本。
- 不依赖 Spring Boot 默认托管版本，显式声明 Liquibase 版本，避免后续升级时被 Boot BOM 隐式覆盖。

根聚合 POM 负责：

- 统一 `groupId`、`version`
- 统一 Java 版本和依赖管理
- 通过 `<modules>` 聚合：
  - `modules/hephaestus-app`
  - `modules/liquibase`

### 3. 配置调整

配置将分散在两个层次：

- 应用模块 `modules/hephaestus-app/src/main/resources/application.yml`
- Liquibase 模块的类路径资源与可选自动配置

在应用模块 `application.yml` 中做两类调整：

1. 关闭 Spring AI 自动建表

```yaml
spring:
  ai:
    chat:
      memory:
        repository:
          jdbc:
            initialize-schema: never
```

2. 启用 Liquibase

```yaml
spring:
  liquibase:
    enabled: true
    change-log: classpath:db/changelog/db.changelog.xml
```

### 4. 单 changelog 文件方案

只在 `modules/liquibase` 中维护一个文件：

- `modules/liquibase/src/main/resources/db/changelog/db.changelog.xml`

文件内部按注释分段组织，不再拆多文件：

- `chat-memory baseline`
- `chat-memory compatibility patches`
- `media-file baseline`
- `media-file compatibility patches`

虽然只有一个文件，但 changeSet 仍应细粒度拆分，避免一个 changeSet 同时承载过多表结构变更。

### 5. 小写表名策略

本次统一使用以下小写表名：

- `spring_ai_chat_memory`
- `spring_ai_media_file`

其中 `spring_ai_media_file` 已是小写，主要改动在 chat memory 表。

由于 Spring AI 默认 SQL 指向 `SPRING_AI_CHAT_MEMORY`，本次设计要求这些代码放在 `modules/liquibase` 中：

- 新增一个自定义 MySQL dialect，例如 `LowercaseMysqlChatMemoryRepositoryDialect`。
- 该 dialect 实现 `JdbcChatMemoryRepositoryDialect`，或继承现有 MySQL dialect 后覆写 SQL，确保：
  - 查询消息 SQL 使用 `spring_ai_chat_memory`
  - 插入消息 SQL 使用 `spring_ai_chat_memory`
  - 删除消息 SQL 使用 `spring_ai_chat_memory`
  - 查询会话 ID SQL 使用 `spring_ai_chat_memory`
- 在应用模块的 [`PersistentChatMemoryConfig.java`](/E:/NEW_WORK/demo/src/main/java/com/example/springaidemo/config/PersistentChatMemoryConfig.java) 中手动装配 `JdbcChatMemoryRepository.builder().jdbcTemplate(...).dialect(...)`，替代依赖默认表名行为。
- 如后续希望进一步插件化，可由 `modules/liquibase` 提供自动配置，应用模块只保留最小装配。

### 6. chat memory 表接管策略

`modules/liquibase` 将完整接管 `spring_ai_chat_memory`，不再依赖 Spring AI 自动 schema。

changelog 对该表采用“基线建表 + 兼容补丁”组合：

1. 基线建表 changeSet

- 仅在表不存在时创建 `spring_ai_chat_memory`
- 采用小写表名
- 字段以当前项目真实运行需要为准，至少覆盖：
  - `conversation_id`
  - `content`
  - `type`
  - `timestamp`
  - `message_type` 或等效兼容字段
- 建立 `(conversation_id, timestamp)` 索引

2. 兼容补丁 changeSet

- 当旧环境中存在历史表时，按需执行：
  - 补 `type`
  - 补 `timestamp`
  - 放宽 `message_type`
  - 补索引
- 每个 changeSet 都使用 `preConditions`
  - `tableExists`
  - `columnExists` / `not columnExists`
  - `indexExists` / `not indexExists`
- 失败策略优先采用 `MARK_RAN` 或按需跳过，避免旧环境重复执行 DDL 导致启动失败。

### 7. media file 表接管策略

`modules/liquibase` 将完整接管 `spring_ai_media_file`。

设计策略：

- 基线 changeSet：表不存在时创建 `spring_ai_media_file`
- 建表时直接包含 `stored_filename`
- 补丁 changeSet：旧环境如缺 `stored_filename`，则补列
- 保留当前索引设计：
  - `idx_spring_ai_media_conversation`
  - `uk_spring_ai_media_storage_path`

### 8. 旧初始化器迁移策略

以下两个类会从应用模块中删除或停用：

- [`ChatMemorySchemaInitializer.java`](/E:/NEW_WORK/demo/src/main/java/com/example/springaidemo/config/ChatMemorySchemaInitializer.java)
- [`MediaFileSchemaInitializer.java`](/E:/NEW_WORK/demo/src/main/java/com/example/springaidemo/media/MediaFileSchemaInitializer.java)

迁移后的唯一 DDL 入口为 `modules/liquibase` 模块中的 Liquibase changelog。

迁移后启动路径变为：

1. 根聚合 POM 构建 `modules/liquibase` 与 `modules/hephaestus-app`
2. 应用模块启动后初始化数据源
3. Liquibase 从依赖模块类路径加载 `db/changelog/db.changelog.xml`
4. Liquibase 执行表结构变更
5. `JdbcChatMemoryRepository` 使用来自 `modules/liquibase` 的小写表名 dialect 正常读写 `spring_ai_chat_memory`
6. 业务服务开始对外提供能力

### 9. 测试策略切换

测试重点从“验证启动器有没有执行 SQL”切换为“验证 Liquibase 是否把结构准备正确”。

测试结构建议：

- Liquibase 模块测试目录：`modules/liquibase/src/test/java/...`
- 应用模块测试目录：`modules/hephaestus-app/src/test/java/...`
- 老的 initializer 单元测试将被移除或替换

建议覆盖的核心场景：

- 空数据库启动后，Liquibase 能创建：
  - `databasechangelog`
  - `databasechangeloglock`
  - `spring_ai_chat_memory`
  - `spring_ai_media_file`
- 旧 `spring_ai_media_file` 缺少 `stored_filename` 时能补齐
- 旧 chat memory 表缺少字段或索引时能补齐
- `PersistentChatMemoryConfig` 使用自定义小写 dialect 后，仓储 SQL 可正常工作

## TDD Plan

### 测试位置

- `modules/liquibase/src/test/java/...`
- `modules/hephaestus-app/src/test/java/...`

### First Failing Test

第一条 failing test：

- 启动应用模块测试上下文并加载 `modules/liquibase` 后，断言数据库中存在小写表 `spring_ai_chat_memory` 与 `spring_ai_media_file`。

### Key Test Scenarios

1. 空库初始化

- Liquibase 首次运行可创建两张业务表与 Liquibase 元数据表。

2. chat memory 旧表补丁

- 当旧表存在但缺少 `type` 时，可自动补齐。
- 当旧表存在但缺少 `timestamp` 时，可自动补齐。
- 当旧表存在但缺少 `(conversation_id, timestamp)` 索引时，可自动补齐。

3. media file 旧表补丁

- 当旧表存在但缺少 `stored_filename` 时，可自动补齐。

4. 小写表名读写

- `JdbcChatMemoryRepository` 通过自定义 dialect 对 `spring_ai_chat_memory` 成功保存和查询消息。

5. 旧入口清理

- 启动时不再加载手工 schema initializer。

### Minimal Implementation Scope

- 根 `pom.xml` 改为聚合父 POM
- 新增 `modules/hephaestus-app`
- 新增 `modules/liquibase`
- 应用模块 `application.yml` 切换为 Liquibase 管理
- 在 `modules/liquibase` 中新增 `db.changelog.xml` 与 chat memory 小写表名支持
- 移除应用模块内两个旧的 schema initializer 及相关旧测试

### Verification Commands

```bash
mvn test
```

如测试按类拆分，可优先执行：

```bash
mvn -Dtest=*Liquibase*,*ChatMemory* test
```

## Risks

### 1. Spring AI 目标表结构风险

`spring_ai_chat_memory` 的基线结构不能只依赖当前补丁类反推，必须以当前 Spring AI JDBC repository 的真实读写 SQL 需求为准，否则会出现：

- 插入字段缺失
- 查询排序字段不匹配
- 历史消息反序列化失败

### 2. 小写表名改造风险

Spring AI 默认假设使用 `SPRING_AI_CHAT_MEMORY`。本次统一小写表名会引入一层自定义 dialect，风险点包括：

- SQL 未全部覆写完全
- 后续 Spring AI 升级后 SQL 模板变化，当前自定义 dialect 需要同步维护

### 3. 单文件 changelog 可维护性风险

当前只使用一个 changelog 文件满足需求，但后续变更持续增加时，文件会逐步膨胀。需要在文件内部保留明确分段注释与命名规则，避免后期不可维护。

### 4. 旧库平滑接管风险

如果生产或测试环境已存在大写表 `SPRING_AI_CHAT_MEMORY`，而本次目标是统一小写表名，需要明确实施策略：

- 若数据库对表名大小写不敏感，可能表面可运行，但命名治理目标不一致。
- 若数据库或部署环境对表名大小写敏感，则需要明确是否做 rename/migrate。

本次实现阶段应优先确认目标 MySQL 环境的 `lower_case_table_names` 行为，再决定是直接创建小写新表，还是对旧表做兼容迁移。

### 5. 多模块改造风险

把 Liquibase 改成真正的 `modules/liquibase` 子模块后，工程结构会发生明显变化：

- 根目录 `pom.xml` 改为 `pom` packaging
- 现有源码需要迁移到应用模块
- IDE 运行配置、脚本路径、构建命令都要同步调整

这比“单模块内聚区”方案改动面更大，需要在实施阶段严格分步迁移。

### 6. 当前仓库非 Git 仓库

`E:\NEW_WORK\demo` 当前不是 Git 仓库，因此本流程只能产出文档与代码变更，不能按常规要求生成 commit 作为审阅锚点。

## Verification

设计评审通过后，实施阶段需要完成以下验证：

- 在根聚合 POM 下执行 `mvn test` 通过
- 应用可正常启动
- Liquibase 元数据表成功生成
- `spring_ai_chat_memory` 与 `spring_ai_media_file` 均以小写表名存在
- chat memory 多轮对话可正常读写
- 历史媒体上传与查询功能不回归
- 启动日志中不再出现旧 schema initializer 执行痕迹

## External References

- Liquibase 最新稳定版核对：
  - Liquibase 官方 GitHub 仓库 README 与 Releases 显示最新稳定社区版为 `v5.0.2`，发布日期为 `2026-03-05`
  - 来源：<https://github.com/liquibase/liquibase>
  - 来源：<https://github.com/liquibase/liquibase/releases>
- Spring AI JDBC chat memory 文档：
  - 默认自动创建 `SPRING_AI_CHAT_MEMORY`
  - `initialize-schema=never` 适用于 Flyway/Liquibase
  - 可通过自定义 `JdbcChatMemoryRepositoryDialect` 扩展 SQL
  - 来源：<https://docs.spring.io/spring-ai/reference/api/chat-memory.html>
- `JdbcChatMemoryRepository.Builder` API：
  - 暴露 `jdbcTemplate`、`dialect`、`dataSource`、`transactionManager`
  - 未暴露表名 setter
  - 来源：<https://docs.spring.io/spring-ai/docs/current/api/org/springframework/ai/chat/memory/repository/jdbc/JdbcChatMemoryRepository.Builder.html>
- Maven 多模块聚合要求：
  - 聚合项目要求使用 `pom` packaging
  - 来源：<https://maven.apache.org/pom.html>
  - 来源：<https://maven.apache.org/guides/introduction/introduction-to-the-pom.html>
