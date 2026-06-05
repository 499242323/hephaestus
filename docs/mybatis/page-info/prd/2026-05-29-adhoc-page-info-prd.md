# MyBatis PageInfo 查询逻辑迁移设计

## Goal

把当前项目中分散手写的分页查询逻辑收敛到 `modules/mybatis`，提供一套最小可复用的 `PageInfo` 查询能力，让业务仓储能按统一方式完成：

- 分页参数归一化
- `count + list` 查询结果封装
- 与现有业务 DTO/Controller 保持兼容

本次先以 `modules/hephaestus-login` 的登录日志分页链路为首个落点，后续其他仓储可按同样模式复用。

## Context

当前仓库中已经有轻量 MyBatis 基座：

- `modules/mybatis/src/main/java/olympus/hephaestus/mybatis/repository/BaseAbstractRepository.java`
- `modules/mybatis/src/main/java/olympus/hephaestus/mybatis/repository/BaseInsertTemplate.java`
- `modules/mybatis/src/main/java/olympus/hephaestus/mybatis/repository/BaseUpdateTemplate.java`
- `modules/mybatis/src/main/java/olympus/hephaestus/mybatis/repository/BaseTemplateSupport.java`

但还没有通用分页能力。现有分页主要表现为业务层手写：

- `modules/hephaestus-login/src/main/java/olympus/hephaestus/login/log/service/impl/LoginLogServiceImpl.java`
  - 手动归一化 `page/pageSize`
  - 手动计算 `offset`
  - 先查列表，再查 `count`
  - 最后组装 `LoginLogPageResponse`

仓储层当前也是显式分页 SQL：

- `modules/hephaestus-login/src/main/java/olympus/hephaestus/login/log/repository/LoginLogRepository.java`
  - `query(..., limit, offset)`
  - `count(...)`

这说明项目已经有“业务显式 SQL + 服务层组装分页结果”的基本模式，但缺少像 `urbanpro` 里 `PageInfo` 一样的统一承载对象和最小模板。

## Constraints

- 只迁移 `PageInfo` 相关最小能力，不顺手扩展条件 DSL、复杂分页插件或大封装。
- 不引入 Egova 平台级重框架，只在 `modules/mybatis` 内补最小公共类。
- 优先兼容当前显式 SQL 风格，不强推所有仓储改成拦截器式自动分页。
- 不破坏现有接口出参结构；首轮允许业务响应 DTO 继续存在。
- 首轮以登录日志分页为验证样例，不要求一次性替换全仓库所有列表查询。
- 遵循“简洁优先、精准修改、以结果为导向”，只补当前确实缺的分页骨架。

## Done When

满足以下条件后，本次设计视为完成：

- 明确 `modules/mybatis` 中 `PageInfo` 最小类集应该包含哪些对象。
- 明确首轮要不要引入分页插件，或继续使用显式 `limit/offset`。
- 明确登录日志分页如何从手写组装迁移到统一 `PageInfo` 组装。
- 明确首个 failing test、最小实现范围和验证命令。
- 明确本次不做什么，避免把任务扩成条件查询体系重构。

## Repository Findings

### 1. 当前分页能力完全分散在业务服务层

`LoginLogServiceImpl.query(...)` 里包含：

- 默认页码 `1`
- 默认页大小 `20`
- 页大小上限 `100`
- `offset = (page - 1) * pageSize`
- 仓储查列表
- 仓储查总数
- 组装 `LoginLogPageResponse`

这部分逻辑以后若在多个模块重复，会造成：

- 相同归一化规则多处复制
- 分页 DTO 命名不统一
- 业务服务承担过多基础分页职责

### 2. 当前仓储已经具备显式 SQL 分页的土壤

`LoginLogRepository` 已经拆成：

- 一个列表查询 SQL
- 一个总数查询 SQL

这说明首轮并不需要引入分页拦截器才能迁移 `PageInfo`。只要把“分页参数 + 分页结果封装”统一起来，就能先完成最小闭环。

### 3. `modules/mybatis` 适合承载分页公共对象，但不适合承载业务条件

从现有边界看，`modules/mybatis` 目前只放：

- 通用 Repository 基座
- 通用 SQL 模板
- 元数据辅助类

因此分页公共层也应只放：

- `PageQuery`
- `PageInfo`
- `PageSupport`

而不应放：

- 登录日志查询条件对象
- 人员查询条件对象
- 与某个业务强绑定的 where 拼装

### 4. 当前项目里还没有测试覆盖分页公共层

检索结果显示，现阶段没有已命中的 `PageInfo` / `PageHelper` / 公共分页测试代码。首轮实现时需要补最小单元测试，先把公共归一化和结果封装钉住。

## Approaches

### 方案 1：只补公共分页对象，继续使用显式 `limit/offset` 查询

做法：

- 在 `modules/mybatis` 新增 `PageQuery`
- 在 `modules/mybatis` 新增通用 `PageInfo<T>`
- 在 `modules/mybatis` 新增 `PageSupport`，负责页码归一化、`offset` 计算、`PageInfo` 组装
- 业务仓储继续保留 `query(..., limit, offset)` 和 `count(...)`

优点：

- 最小改动
- 不影响现有 SQL 写法
- 不引入额外分页插件和自动拦截行为
- 与你一直强调的“不要搞复杂”最一致

缺点：

- 还不是完全自动化分页
- 业务仓储仍需保留 `count + list` 两个查询方法

### 方案 2：引入 MyBatis-Plus 或分页插件统一拦截分页

做法：

- 为查询接入分页插件
- 仓储方法改成只写查询主体，由插件自动补 `limit/offset`
- 返回统一分页对象

优点：

- 后续批量列表查询写法可能更统一

缺点：

- 首轮接入成本更高
- 容易牵连 SQL、配置和行为差异
- 与当前仓库“显式 SQL 为主”的风格不完全一致

### 方案 3：一步到位迁移完整条件查询 / 分页模板体系

做法：

- 在 `modules/mybatis` 引入完整的条件对象、模板、统一查询抽象

优点：

- 看起来更接近大型框架

缺点：

- 范围明显过大
- 与“最小迁移集”冲突
- 风险和理解成本都高

### Recommendation

推荐方案 1。

原因很直接：你当前要的是把 `PageInfo` 查询逻辑迁进来，不是重做整个查询框架。方案 1 能先把“统一分页参数和结果模型”补齐，同时最大限度复用现有仓储 SQL。

## Design

### 1. 新增最小分页对象

建议在 `modules/mybatis` 新增：

- `olympus.hephaestus.mybatis.page.PageQuery`
- `olympus.hephaestus.mybatis.page.PageInfo`
- `olympus.hephaestus.mybatis.page.PageSupport`

职责如下：

`PageQuery`

- 持有 `page`、`pageSize`
- 提供默认值与上限控制
- 提供 `offset()`、`limit()` 等派生值

`PageInfo<T>`

- 持有 `List<T> items`
- 持有 `page`
- 持有 `pageSize`
- 持有 `total`
- 可选提供 `totalPages()` 之类纯派生方法，但首轮可以不加

`PageSupport`

- 接收原始 `page/pageSize`
- 归一化为 `PageQuery`
- 根据 `items + total + pageQuery` 组装 `PageInfo<T>`

### 2. 业务仓储仍保留显式分页 SQL

首轮不改仓储查询风格：

- 仓储继续保留 `query(..., limit, offset)`
- 仓储继续保留 `count(...)`

原因：

- 这部分已经可用
- 改动最小
- 不引入分页插件副作用

### 3. 业务服务从“手写分页细节”改为“调用公共分页支持”

以登录日志为例，服务层调整为：

1. 原始参数传入 `PageSupport.normalize(page, pageSize)`
2. 用归一化后的 `limit/offset` 调仓储查询列表
3. 调仓储 `count(...)`
4. 用 `PageSupport.pageInfo(items, total, pageQuery)` 组装统一分页结果

这样服务层保留业务查询编排职责，但不再重复写分页基础算法。

### 4. 业务响应兼容策略

首轮有两个可选兼容方式：

- 方式 A：`LoginLogPageResponse` 删除，控制器直接返回 `PageInfo<LoginLogResponse>`
- 方式 B：保留 `LoginLogPageResponse`，但内部增加一个 `from(PageInfo<LoginLogResponse>)`

建议先用方式 A，如果外部前端只依赖字段结构而不依赖类名，则 JSON 结构不变：

- `items`
- `page`
- `pageSize`
- `total`

如果你更希望业务模块出参名保持稳定，也可以先用方式 B，作为过渡。

我当前更推荐方式 B 作为首轮落地，因为它改动更窄，也更稳。

### 5. 本次明确不纳入的范围

本次不做：

- 通用条件 DSL
- 通用动态 where 模板
- 自动分页拦截器
- 通用排序字段白名单体系
- 所有业务分页一次性替换

这样可以把任务严格收在“PageInfo 逻辑迁移”本身。

## TDD Plan

### 测试位置

优先新增：

- `modules/mybatis/src/test/java/olympus/hephaestus/mybatis/page/PageSupportTest.java`

如登录日志改造一起落地，再补：

- `modules/hephaestus-login/src/test/java/olympus/hephaestus/login/log/service/LoginLogServiceImplTest.java`

### First Failing Test

第一条 failing test 建议写在 `PageSupportTest`：

- 输入 `page = null, pageSize = null`
- 期望归一化为 `page = 1, pageSize = 20, offset = 0`

这能先把最基础、最稳定、最可复用的行为钉住。

### Key Test Scenarios

1. `page/pageSize` 为空时使用默认值。
2. `page < 1` 时回退到 `1`。
3. `pageSize < 1` 时回退到默认值。
4. `pageSize > max` 时截断到最大值。
5. `offset()` 按 `(page - 1) * pageSize` 正确计算。
6. `PageInfo` 组装后，`items/page/pageSize/total` 正确保留。
7. 登录日志服务改造后，仍能输出与原来一致的分页 JSON 字段结构。

### Minimal Implementation Scope

- 新增 `PageQuery`
- 新增 `PageInfo<T>`
- 新增 `PageSupport`
- 登录日志服务改用公共分页对象
- 视兼容策略决定是否保留 `LoginLogPageResponse`
- 新增最小测试

### Verification Commands

```bash
mvn -pl modules/mybatis -Dtest=PageSupportTest test
```

如果同时改登录日志：

```bash
mvn -pl modules/hephaestus-login -Dtest=LoginLogServiceImplTest test
```

保守起见还应补一次模块编译：

```bash
mvn -pl modules/mybatis,modules/hephaestus-login -am test
```

## Risks

### 1. 范围膨胀风险

一旦把 `PageInfo` 迁移误做成“完整查询框架迁移”，很容易失控。本次必须坚持只做分页对象和分页支持。

### 2. 兼容风险

如果直接把 `LoginLogPageResponse` 改成公共 `PageInfo<LoginLogResponse>`，虽然 JSON 字段大概率不变，但仍要确认没有前端或调用方依赖特殊序列化行为。

### 3. 测试落点风险

如果登录日志服务当前没有现成单测，首轮可能先只能稳住公共分页层，再补业务层测试。

### 4. 未来扩展风险

当前方案故意不覆盖通用条件查询模板，所以后续若要继续迁移复杂列表能力，需要再单独设计，不应在这次任务里顺手塞进去。

## Verification

设计评审通过后，实施阶段至少验证：

- `PageSupport` 归一化行为符合默认规则。
- `PageInfo` 结果封装字段与当前前端使用字段一致。
- 登录日志查询链路分页结果不变。
- 新增分页公共类不会破坏现有 `modules/mybatis` 仓储基座。

## Assumptions

- 当前案卷号未提供，文档编号使用 `adhoc`。
- 当前 `PageInfo` 迁移首轮只落一个真实业务样例。
- 当前目标是“最小可用分页公共层”，不是“完整 urbanpro 查询体系”。
