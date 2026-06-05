# Hephaestus App MyBatis 接入设计

## Goal

在 `E:\NEW_WORK\hephaestus` 中新增独立子模块 `modules\mybatis`，承载 MyBatis 持久化基础设施、通用 Repository 基座以及媒体文件相关 Repository，并将当前 `MediaFileRepository` 从 `JdbcTemplate` 实现迁移为接近 `urbanpro` `SysScheduleRepository` 风格的 Repository 接口实现，在不改变现有控制器和服务层注入方式的前提下，保持媒体文件元数据的保存、查询、更新行为不变。

## Context

当前 `hephaestus` 已经是 Maven 多模块 Spring Boot 项目，应用模块与数据库基础能力现状如下：

- 现有模块只有：
  - [`modules/hephaestus-app/pom.xml`](E:\NEW_WORK\hephaestus\modules\hephaestus-app\pom.xml)
  - [`modules/liquibase/pom.xml`](E:\NEW_WORK\hephaestus\modules\liquibase\pom.xml)
- 应用主模块为 [`modules/hephaestus-app/pom.xml`](E:\NEW_WORK\hephaestus\modules\hephaestus-app\pom.xml)。
- 根聚合 POM 为 [`pom.xml`](E:\NEW_WORK\hephaestus\pom.xml)。
- 数据源、Liquibase、Redis 和 Spring AI 已配置在 [`modules/hephaestus-app/src/main/resources/application.yml`](E:\NEW_WORK\hephaestus\modules\hephaestus-app\src\main\resources\application.yml)。
- 媒体文件元数据表 `spring_ai_media_file` 已由 Liquibase 管理，定义位于 [`modules/liquibase/src/main/resources/db/changelog/db.changelog.xml`](E:\NEW_WORK\hephaestus\modules\liquibase\src\main\resources\db\changelog\db.changelog.xml)。
- 当前仓储实现位于 [`modules/hephaestus-app/src/main/java/olympus/hephaestus/media/MediaFileRepository.java`](E:\NEW_WORK\hephaestus\modules\hephaestus-app\src\main\java\com\example\springaidemo\media\MediaFileRepository.java)，使用 `JdbcTemplate` 直接执行 SQL。
- 当前媒体元数据模型为 [`modules/hephaestus-app/src/main/java/olympus/hephaestus/media/MediaFile.java`](E:\NEW_WORK\hephaestus\modules\hephaestus-app\src\main\java\com\example\springaidemo\media\MediaFile.java)。

参考仓库 `E:\NEW_WORK\egova-urbanpro` 已接入 MyBatis，其核心使用方式是“Repository 继承一层通用 CRUD 能力，再在同一个 Repository 中追加自定义查询”。当前决策不是完全排斥 Egova 类，而是优先采用“最小迁移集”策略：如果 `BaseAbstractRepository`、`BaseInsertTemplate` 及其少量直接支撑类可以独立迁移到当前仓库，就优先迁移这些类到 `modules\mybatis`；只有当依赖面扩大到明显的平台级封装时，才退回本地等价实现。

结合当前新增约束，本次设计调整为：

- 可以引入非 Egova 的通用 MyBatis 增强框架；
- 最终在 `hephaestus` 内提供尽量接近 `urbanpro` 的 Repository 式增删改查体验；
- 代码落位必须在新增模块 `modules\mybatis`；
- 目标能力要至少覆盖 `urbanpro` 中 `SysScheduleRepository` 这一类 Repository 的关键能力；
- 仍然不直接引入 Egova 平台大封装依赖，但允许把少量 Egova 源码类作为“最小迁移集”并入 `modules\mybatis`。

## Constraints

- 允许引入非 Egova 的通用 MyBatis 增强框架，以承接底层运行时能力。
- 不直接引入 `flagwind-mybatis`、`egova-boot-*` 或 `urbanpro-*` 整包依赖。
- 优先迁移 `BaseAbstractRepository`、`BaseInsertTemplate` 及其最少直接依赖类到 `modules\mybatis`；如最小迁移集无法闭合，再退回本地等价实现。
- 保持现有数据库表结构、Liquibase 变更入口和数据源配置不变。
- 必须新增 `modules\mybatis` 模块，而不是把 MyBatis 代码直接散落在 `modules\hephaestus-app`。
- `MediaFileRepository`、相关持久化实体、Mapper/Repository 基类、批量插入支持等代码都放在 `modules\mybatis`。
- 首轮接入范围限定在 `spring_ai_media_file` 相关仓储，但 CRUD 组织方式要尽量向 `urbanpro` 靠拢。
- 如最小迁移集不可行，才引入本地轻量基类/基接口作为替代，但不能演变成新的平台大封装。
- 设计出的 Repository 形态至少要支持接近 `SysScheduleRepository` 的三类能力：
  - 通用 CRUD
  - 自定义查询方法
  - 批量插入且支持主键回填
- 设计阶段只产出 PRD，不进入实现。

## Done When

满足以下条件后，本次设计视为完成：

- 已明确 `modules\mybatis` 的模块职责、依赖、代码结构以及与 `hephaestus-app` 的依赖关系。
- 已明确 `MediaFileRepository` 迁移后的 Repository 形态、通用 CRUD 入口和自定义 SQL 组织方式。
- 已明确是否引入 MyBatis 增强框架，以及与 `urbanpro` 的对齐边界。
- 已明确最小 Egova 迁移集的优先级，以及退回本地等价实现的边界。
- 已明确如何对齐 `SysScheduleRepository` 风格的批量插入与主键回填能力。
- 已明确测试切入点、第一条 failing test 和验证命令。
- 已明确参考 `urbanpro` 的部分与不参考的部分。

## Repository Findings

### 1. 当前数据库基础已经齐备

- `hephaestus-app` 已配置 MySQL 数据源、HikariCP、Liquibase 和 Redis。
- `spring_ai_media_file` 表已经存在正式 changelog，字段包括：
  - `id`
  - `conversation_id`
  - `original_filename`
  - `stored_filename`
  - `content_type`
  - `file_size`
  - `storage_path`
  - `access_url`
  - `source_type`
  - `created_at`
- 当前并不存在“先补库再接 ORM”的前置阻塞，MyBatis 可以直接基于既有表结构接入。

### 1.1 当前项目还没有独立的持久化模块

- 当前 `modules` 下只有 `hephaestus-app` 与 `liquibase`。
- 如果要满足“代码放到 `modules\mybatis`”这一要求，就需要把 Repository 基础设施、实体与相关资源独立为新的 Maven 子模块。
- 这意味着根聚合 POM 需要新增 `modules/mybatis`，并让 `hephaestus-app` 依赖它。

### 2. 当前仓储边界非常集中

[`MediaFileRepository.java`](E:\NEW_WORK\hephaestus\modules\hephaestus-app\src\main\java\com\example\springaidemo\media\MediaFileRepository.java) 目前只承担 3 个数据库操作：

- `save(MediaFile mediaFile)`
- `findById(long id)`
- `updateAccessUrl(long id, String accessUrl)`

这意味着首轮 MyBatis 接入可以严格控制在单仓储、单表、少量 SQL 范围内，不需要同步引入复杂的分页、动态查询、批量更新或通用基类。

### 3. 媒体功能对仓储的行为契约已经存在

媒体相关测试已经围绕仓储门面建立了依赖：

- [`MediaFileControllerTest.java`](E:\NEW_WORK\hephaestus\modules\hephaestus-app\src\test\java\com\example\springaidemo\media\MediaFileControllerTest.java) 直接 mock `MediaFileRepository`。
- [`MediaStorageServiceTest.java`](E:\NEW_WORK\hephaestus\modules\hephaestus-app\src\test\java\com\example\springaidemo\media\MediaStorageServiceTest.java) 锁定了文件存储行为，但不关心具体持久化实现。

因此，保留 `MediaFileRepository` 作为门面，可以最大程度避免 Web 层和服务层测试改动。

### 4. `urbanpro` 的关键体验是“通用 CRUD Repository + 自定义查询”

从 `E:\NEW_WORK\egova-urbanpro` 的配置和代码可见：

- `service/egova-urbanpro-service-all/pom.xml` 依赖了 `flagwind-mybatis`。
- `service/egova-urbanpro-service-all/src/main/resources/application.yml` 使用 `flagwind.mybatis.*` 配置。
- Repository 普遍继承 `BaseAbstractRepository<Entity, IdType>`。
- Service 层通常持有 Repository，并直接调用通用方法，如：
  - `insert(entity)`
  - `getById(id)`
  - `delete(clause)`
  - `query(clause)`
- 自定义查询仍然可以继续写在同一个 Repository 接口中，使用注解 SQL 或 XML，例如 `SysRuleRepository` 与 `SysRuleRepository.xml`。

进一步看你点名的 `SysScheduleRepository`，它还体现了一个更具体的目标形态：

- Repository 接口本身继承通用 CRUD 基座。
- 可以直接声明领域方法，例如 `getByTimeSysId(...)`。
- 支持批量插入方法 `insertList(...)`。
- 批量插入方法要求支持数据库主键回填。

因此，本次设计不能只满足“单条 CRUD + 少量查询”，还需要预留或实现“批量插入 + 主键回填”这一层能力，至少达到 `SysScheduleRepository` 级别的使用方式。

### 4.1 当前更适合迁“最小 Egova 类集”，而不是整包依赖

目前已经确认两点：

- `BaseAbstractRepository` 在 `urbanpro` 多个模块中被广泛使用，说明它确实是目标使用体验的核心入口。
- `SysScheduleRepository` / `SysScheduleDetailRepository` 直接依赖 `BaseInsertTemplate` 来实现 `insertList(...)`。

因此，本次更合适的路线不是重新发明一套完全不同的 Repository 语义，而是：

- 优先在 `modules\mybatis` 中迁入 `BaseAbstractRepository` 风格所需的最少源码类；
- 优先在 `modules\mybatis` 中迁入 `BaseInsertTemplate` 风格所需的最少源码类；
- 如最小迁移集验证后发现还会牵出大范围条件 DSL、平台 service 基类或其他 Egova 平台能力，再停止扩张并切回本地等价实现。

也就是说，当前真正要避免的是“整包依赖接入”，不是“少量源码迁移”。

这说明 `urbanpro` 真正值得对齐的不是它的 Egova 依赖，而是它的使用体验：

- Repository 作为业务层的稳定注入点。
- Repository 先拥有一组通用 CRUD 方法，再叠加个性化 SQL。
- SQL 可按复杂度在注解与 XML 之间选择，而不是强制单一形式。

## Design

### 1. 选型结论

采用 `MyBatis-Plus + MyBatis Spring Boot Starter` 作为底层运行时，并新增独立子模块 `modules\mybatis`。该模块优先承载从 Egova 迁入的最小 Repository 类集，使使用方式尽量贴近 `urbanpro` 的 `BaseAbstractRepository` / `SysScheduleRepository` 体验；只有当最小迁移集无法收敛时，才补本地等价实现。

具体取舍如下：

- 采用：
  - `com.baomidou:mybatis-plus-spring-boot3-starter`
  - `@Mapper` / `@MapperScan`
  - 从 Egova 迁入的最小 `BaseAbstractRepository` 风格基类或接口
  - 从 Egova 迁入的最小 `BaseInsertTemplate` 风格批量插入支撑
  - `BaseMapper<T>` 或等价底层 mapper 能力作为运行时依托
  - MyBatis 原生注解或 XML Mapper 承载自定义 SQL
- 不采用：
  - `flagwind-mybatis`
  - `flagwind.mybatis.*` 专有配置
  - `egova-boot-*` 平台依赖
  - MyBatis-Plus 以外再叠加第二套 ORM/Repository 框架

### 2. Repository 形态

本次不再维持“`Repository` 类包住一个独立 `Mapper` 类”的最薄模式，而是改为更接近 `urbanpro` 的接口式 Repository。

建议结构：

- `modules/mybatis/pom.xml`
- `modules/mybatis/src/main/java/olympus/hephaestus/mybatis/config/...`
- `modules/mybatis/src/main/java/olympus/hephaestus/mybatis/repository/BaseAbstractRepository.java`
- `modules/mybatis/src/main/java/olympus/hephaestus/mybatis/repository/BaseInsertTemplate.java`
- `modules/mybatis/src/main/java/olympus/hephaestus/mybatis/repository/...`（最小直接依赖类）
- `modules/mybatis/src/main/java/olympus/hephaestus/media/MediaFileEntity.java`
- `modules/mybatis/src/main/java/olympus/hephaestus/media/MediaFileRepository.java`
- `modules/mybatis/src/main/resources/mybatis/mapper/...`（如需要 XML）

职责划分如下：

- 迁移后的 `BaseAbstractRepository<T, ID>`
  - 作为 `urbanpro` 风格的核心 CRUD 入口。
  - 只保留 `MediaFileRepository` 当前阶段真正需要的最小能力，例如 `insert`、`getById`。
  - 明确不在首轮迁入 `SingleClause` / `CombineClause` 全量条件 DSL，除非验证为直接必需依赖。
- 迁移后的 `BaseInsertTemplate`
  - 用于支撑 `SysScheduleRepository.insertList(...)` 风格的批量插入入口。
  - 首轮目标是满足批量插入与主键回填语义，而不是还原全部 Egova 模板能力。
- 其他最小直接依赖类
  - 只迁被 `BaseAbstractRepository` / `BaseInsertTemplate` 直接引用且无法替换的类。
  - 迁入范围由实际编译依赖闭包决定，不主动扩大到 service、条件 DSL、平台配置层。
- `MediaFileEntity`
  - 作为 MyBatis-Plus 持久化实体，映射 `spring_ai_media_file`。
  - 包含主键、字段注解和必要的时间字段映射。
- `MediaFileRepository`
  - 放在 `modules\mybatis` 中，作为 Spring 注入的仓储接口。
  - 优先继承迁移后的 `BaseAbstractRepository<MediaFileEntity, Long>` 获得通用 CRUD。
  - 按需实现或声明批量插入能力。
  - 在同一接口中追加 `updateAccessUrl`、按需补充其他查询方法。
- 可选 XML / 注解 SQL
  - 只承载 `BaseMapper` 之外的个性化 SQL。

这样做的好处是：

- 使用体验更接近 `urbanpro`：Repository 本身就是通用 CRUD 入口。
- 后续如果还有别的表要迁移，可以继续复用迁入的最小 Egova Repository 基座。
- 仍然把封装控制在仓库内部，不引入 Egova 平台依赖。

### 3. CRUD 逻辑对齐方式

与 `urbanpro` 对齐的重点不是完全复制其方法实现，而是在当前项目中实现同类使用模式：

- 通用增删改查优先来自迁入的 `BaseAbstractRepository`；
- 个性化查询和更新继续写在 Repository 中；
- Service / Controller 注入 Repository，而不是显式区分“JdbcRepository + Mapper”两层；
- 对于 `SysScheduleRepository.insertList(...)` 这类模式，要能在本地 Repository 中提供批量插入入口。

建议本次首轮就把 `spring_ai_media_file` 的常用逻辑整理为：

- 通用 CRUD：
  - `insert(entity)`
  - `getById(id)` 或等价读取方法
  - `updateById(entity)`
  - `deleteById(id)`
- 个性化逻辑：
  - `updateAccessUrl(id, accessUrl)`
  - `getByConversationId(...)` 这类后续可扩展的查询方法
- 批量逻辑：
  - `insertList(List<MediaFileEntity> list)` 或等价批量插入入口
  - 如后续需要，可继续扩展“按会话查询列表”等方法

这意味着 `hephaestus` 内部最终将具备和 `urbanpro` 相似的增删改查组织方式，尤其是接近 `SysScheduleRepository` 的接口式 Repository 体验，但底层来源是 MyBatis-Plus 与本地轻量扩展，而不是 Egova 私有基座。

### 4. SQL 组织策略

首轮推荐采用“通用 CRUD 优先复用迁入的 `BaseAbstractRepository`，简单定制 SQL 用注解，`insertList` 优先复用迁入的 `BaseInsertTemplate`，复杂 SQL 再转 XML”的策略。

理由：

- 与 `urbanpro` 实际风格一致：基础 CRUD 不手写，特殊查询按需补。
- 当前媒体表个性化 SQL 只有少量语句，先用注解更轻。
- 批量插入是 `SysScheduleRepository` 级别要求，必须在设计中单独考虑，而不是留作后续补丁。
- 如果后续出现批量更新、动态条件或复杂结果映射，再转 XML 成本较低。

本次初步设计：

- `insert`、`getById` 优先由迁入的 `BaseAbstractRepository` 提供。
- `updateAccessUrl` 使用 `@Update`。
- `insertList` 优先由迁入的 `BaseInsertTemplate` 承载，并在测试中验证主键写回策略。
- 如迁移过程中发现少量方法最终仍需委托到底层 `BaseMapper`，允许在 `modules\mybatis` 内做适配，但对外语义保持 `urbanpro` 风格。
- 如 `created_at -> LocalDateTime` 的映射在当前版本下表现不稳定，再补充 `resultMap` 或转为 XML。

### 5. 模型与映射方式

为了让 Repository 直接继承通用 CRUD，建议引入专用持久化实体，而不是直接把当前 `record MediaFile` 作为 MyBatis-Plus 实体。

推荐方式：

- 新增 `MediaFileEntity` 作为数据库实体；
- 保留现有 `MediaFile` 作为服务层使用的领域模型，或在实现阶段评估是否直接合并为普通实体类；
- Repository 层负责必要的实体转换。

这样做的原因是：

- `record` 作为通用 CRUD 实体的适配弹性较差；
- MyBatis-Plus 对常规 JavaBean / 实体类的支持更稳定；
- 能把 ORM 注解和外部业务模型解耦。

### 6. 模块组织方式

本次接入不只是加依赖，而是调整 Maven 模块结构：

- 根 `pom.xml` 新增子模块：
  - `modules/liquibase`
  - `modules/mybatis`
  - `modules/hephaestus-app`
- `modules/mybatis` 负责：
  - MyBatis-Plus 依赖
  - 迁入的最小 Egova Repository 类集
  - 迁入的批量插入模板类集
  - 持久化实体
  - 领域 Repository 接口
  - MyBatis 相关 XML 资源（如需要）
- `modules/hephaestus-app` 负责：
  - Spring Boot 启动类
  - Controller / Service / 业务流程
  - 依赖 `modules/mybatis` 与 `modules/liquibase`

这样才能满足“代码放在 `modules\mybatis`”且不污染应用模块。

### 7. 配置接入方式

在 `modules/hephaestus-app` 中新增 MyBatis-Plus 基础配置，但不改动现有数据源和 Liquibase 配置。

设计要点：

- 在 `pom.xml` 中新增 MyBatis-Plus starter 依赖。
- 在根聚合 POM 中注册 `modules/mybatis`。
- 在 `modules/mybatis/pom.xml` 中声明 MyBatis-Plus 与必要的 Spring/MyBatis 依赖。
- 在 `modules/hephaestus-app/pom.xml` 中依赖 `modules/mybatis`。
- 在启动类 [`SpringAiDemoApplication.java`](E:\NEW_WORK\hephaestus\modules\hephaestus-app\src\main\java\com\example\springaidemo\SpringAiDemoApplication.java) 或专用配置类中开启 `@MapperScan`。
- 在 `application.yml` 中只添加必要的 MyBatis / MyBatis-Plus 配置：
  - `map-underscore-to-camel-case`
  - 可选 `mapper-locations`
  - 可选日志实现配置

本次不计划新增：

- 多数据源
- 通用分页插件
- 二级缓存
- 自定义方言

### 8. 兼容迁移策略

迁移后，`MediaFileRepository` 的“类型名称”和“注入角色”保持不变，但其内部形态会从 `class` 调整为 `interface Repository`。

兼容策略分两层：

1. 注入兼容

- 业务层和控制器仍然注入 `MediaFileRepository`。
- 测试中的 `mock(MediaFileRepository.class)` 与 `@MockBean MediaFileRepository` 仍然成立。

2. 调用兼容

- 若实现阶段发现改成 `urbanpro` 风格 CRUD 方法名会造成上层改动面过大，可保留少量兼容默认方法或门面方法。
- 若改动面可控，则可逐步把调用迁到更接近 `urbanpro` 的通用 CRUD 语义。

当前推荐的优先级是：

- 优先保证 Repository 注入点不变；
- 再逐步把内部 CRUD 组织方式切换到 `BaseMapper` 风格；
- 允许在过渡期保留 `save()` / `findById()` 这类兼容方法，避免一次性改动过大。

兼容要求：

- 插入后仍需能拿到自增主键。
- 读取逻辑仍需能按主键拿回完整媒体元数据。
- `updateAccessUrl` 的现有业务语义保持不变。

### 9. 参考 `urbanpro` 的边界

本次会参考 `urbanpro` 的内容：

- Repository 作为业务层稳定注入入口。
- Repository 先继承通用 CRUD，再在同一接口中增加自定义 SQL。
- Repository 支持 `SysScheduleRepository` 风格的批量插入能力。
- Service / Controller 面向 Repository，而不是显式操作底层 Mapper。
- SQL 允许注解和 XML 混合组织。
- 按领域包组织持久层代码。

本次不会参考 `urbanpro` 的内容：

- `BaseAbstractRepository` 泛型基类体系。
- `flagwind-mybatis` 自定义模板与扩展语法。
- 平台级统一仓储父类、分页方言、规则 SQL 宏等高级能力。
- `egova-boot`、Nacos、OAuth、Flowable 等平台能力。

## TDD Plan

### 测试位置

- 优先在 `modules/hephaestus-app/src/test/java/olympus/hephaestus/media/` 下补充仓储集成测试。
- 如 `modules/mybatis` 需要单独验证基接口或批量能力，可在 `modules/mybatis/src/test/java/...` 下补充模块级测试。
- 保留现有 Controller / Service 测试不动，利用它们验证门面兼容性。

### First Failing Test

第一条 failing test：

- 新增一个面向 `MediaFileRepository` 的集成测试，先断言 `modules\mybatis` 中的 Repository 通用插入能力能把一条媒体记录正确写入 `spring_ai_media_file`，并回填非空主键。

这是最关键的第一条测试，因为它同时覆盖：

- MyBatis-Plus 自动配置是否生效
- Repository 是否被扫描
- 通用 CRUD 是否真正可用
- 主键回填是否正常

### Key Test Scenarios

1. 通用插入

- 插入一条媒体实体后返回或回填数据库生成的 `id`。
- 插入后数据库中对应字段值与输入一致。

2. 通用按主键查询

- Repository 的主键查询能力能查回完整媒体实体。
- `created_at` 可稳定映射为 Java 时间类型。
- 业务兼容层仍能处理“查不到”的场景。

3. 通用更新与自定义更新

- `updateById()` 能正确更新标准字段。
- `updateAccessUrl()` 执行后，再次查询应能看到新的 `access_url`。

4. 批量插入能力

- `insertList(...)` 能批量写入多条记录。
- 如设计目标要求对齐 `SysScheduleRepository`，则需要验证批量插入后的主键回填行为或明确记录其兼容策略。

5. 门面兼容

- 现有依赖 `MediaFileRepository` 的控制器测试无需因接入 MyBatis 改方法签名或注入类型。

### Minimal Implementation Scope

- 在根聚合 POM 中增加 `modules/mybatis`。
- 新增 `modules/mybatis/pom.xml`。
- 在 `modules/hephaestus-app/pom.xml` 中增加对 `modules/mybatis` 的依赖。
- 新增 MyBatis / Repository 扫描配置。
- 在 `modules/mybatis` 中迁入最小 `BaseAbstractRepository` / `BaseInsertTemplate` 相关类集。
- 在 `modules/mybatis` 中新增批量插入支持。
- 在 `modules/mybatis` 中新增 `MediaFileEntity`。
- 将 `MediaFileRepository` 迁移为 `modules/mybatis` 下的接口式 Repository，并继承通用 CRUD 基接口。
- 新增或更新仓储测试。

### Verification Commands

```bash
mvn -pl modules/hephaestus-app test
```

如需先聚焦媒体相关测试，可优先执行：

```bash
mvn -pl modules/hephaestus-app -Dtest=*Media* test
```

## Risks

### 1. 类型映射风险

`created_at` 映射到 `LocalDateTime` 需要确认当前 MyBatis-Plus + JDBC 驱动组合下表现稳定。如果默认映射不满足预期，需要显式配置结果映射。

### 2. 主键回填风险

当前 `JdbcTemplate` 通过 `GeneratedKeyHolder` 回填主键；切换到 MyBatis-Plus 后，需要确认主键策略、`@TableId` 和回填配置正确，否则插入后的实体会缺失 `id`。

### 3. 配置冲突风险

项目当前已使用 Spring AI JDBC chat memory repository 和 Liquibase。虽然它们与 MyBatis-Plus 理论上可以共存，但仍需验证不会引入自动配置顺序或数据源装配上的冲突。

### 4. 范围膨胀风险

`urbanpro` 中 MyBatis 方案覆盖大量平台能力，容易让实现阶段不自觉扩大范围。本次必须坚持“只引入非 Egova 通用能力 + 只迁移 `MediaFileRepository`”，不把接入任务演变成平台改造。

### 5. 最小迁移集闭包风险

`SysScheduleRepository.insertList(...)` 使用的是 Egova 私有 `BaseInsertTemplate`，而 `BaseAbstractRepository` 也可能继续依赖少量关联类。本次如果采用最小迁移集路线，需要明确：

- 最小迁移集是否真的能在 `modules\mybatis` 内独立闭合；
- 是否只牵出少量模板/元数据类；
- 还是会继续牵出 `SingleClause`、`CombineClause`、更大的条件 DSL 或其他平台类。

如果依赖面开始明显扩大，就应停止继续迁移并退回本地等价实现。

### 6. 批量插入兼容风险

即使最小迁移集可行，也仍需明确：

- 是真正做到批量插入且逐条回填主键；
- 还是通过逐条执行插入来保证主键回填语义；
- 或者对批量方法的返回/写回策略做出可测试的兼容约定。

这部分必须在实现阶段通过测试钉住，不能只停留在接口命名层面。

### 7. 模型切换风险

如果为适配通用 CRUD 而新增 `MediaFileEntity`，则实现阶段需要处理实体与现有 `MediaFile` 模型之间的转换；若直接把 `MediaFile` 从 `record` 改为实体类，则会带来更广的调用侧修改。两种路径都需要在实现前做一次最小化改动评估。

### 8. 模块拆分风险

新增 `modules/mybatis` 会引入新的 Maven 依赖方向、包位置和资源扫描路径。若模块边界定义不清，容易出现：

- 扫描不到 Mapper/Repository
- `hephaestus-app` 和 `mybatis` 循环依赖
- 业务模型与持久化模型混放导致职责再次模糊

因此模块边界需要在实施前就固定清楚。

### 9. 当前仓库不在 Git 根目录

当前工作目录未检测到 `.git`，因此本流程可以写文档和代码，但不能依赖本目录 Git 提交作为审阅锚点。若后续需要提交文档或代码，需要先确认实际 Git 根目录。

## Verification

设计评审通过后，实施阶段需要完成以下验证：

- `hephaestus-app` 模块测试通过。
- `modules/mybatis` 如有独立测试，也需通过。
- 新增仓储测试能证明 Repository 的通用 CRUD、批量插入与 `updateAccessUrl` 行为都可正常工作。
- 现有媒体相关控制器测试不因 Repository 内部实现切换而失败。
- 应用启动时 Repository 能被正确扫描并注入。
- 现有 Liquibase 建表与数据源配置不需要额外变更即可支撑 MyBatis-Plus 读写。

## Assumptions

- 文档编号使用 `adhoc`，因为当前未提供明确案卷号。
- 本次目标涉及新增 `modules/mybatis` 子模块，但不改造 `modules/liquibase` 的职责边界。
- `urbanpro` 仅作为使用方式参考，不作为直接依赖来源。
