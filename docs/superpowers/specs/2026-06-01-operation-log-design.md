# 操作日志结构化明细设计

## 背景

当前系统已经具备登录日志能力，设置抽屉中也已有“日志”栏目和“登录日志 / 操作日志”页签。登录日志已经能记录登录成功、登录失败和退出登录；操作日志目前只是前端占位文案，还没有独立的数据表、后端接口和采集逻辑。

本次目标是在不改变现有日志栏目结构的前提下，新增“结构化明细型”操作日志。操作日志用于记录系统配置、部门、人员、岗位等管理类写操作，方便后续按权限查询、审计和排查问题。

## 设计目标

1. 所有新增、修改、删除、保存配置等管理操作都应记录操作日志。
2. 日志内容要足够详细，但不保存完整修改前后 JSON。
3. 操作日志写入失败不能影响原业务操作。
4. 操作日志显示和查询受岗位权限控制。
5. 前端沿用现有“日志 -> 操作日志”页签，不调整常规、日志等五大块显示结构。
6. 文案、字段名、页面提示均使用中文。

## 不做范围

1. 不保存完整请求体、完整修改前 JSON 或完整修改后 JSON。
2. 不把登录日志迁移到操作日志表。
3. 不改变现有登录日志查询逻辑。
4. 不新增复杂审计统计、导出、告警能力。
5. 不改变现有岗位权限的大块结构。

## 日志内容

操作日志采用固定字段加中文摘要、中文明细的方式记录。

建议字段如下：

| 字段 | 含义 |
| --- | --- |
| id | 日志主键 |
| operatorPersonId | 操作人员 ID |
| operatorName | 操作人员姓名 |
| operatorUsername | 操作账号 |
| operatorUnitId | 操作人员部门 ID |
| operatorUnitName | 操作人员部门名称 |
| moduleCode | 模块编码，例如 system-config、org-unit、org-person、org-role |
| moduleName | 模块名称，例如系统配置、部门、人员、岗位 |
| actionCode | 动作编码，例如 create、update、delete、save-config、assign-person、save-permission |
| actionName | 动作名称，例如新增、修改、删除、保存配置、分配人员、设置权限 |
| targetType | 对象类型，例如配置、部门、人员、岗位 |
| targetId | 对象 ID |
| targetName | 对象名称 |
| success | 是否成功 |
| summary | 列表摘要 |
| detail | 中文明细文本 |
| clientIp | 客户端 IP |
| userAgent | 浏览器 User-Agent |
| requestUri | 请求路径 |
| requestMethod | 请求方法 |
| createdAt | 创建时间 |

## 明细规则

日志明细不保存完整 JSON，而是由各业务服务生成中文短句。

示例：

- 新增部门：`新增部门“综合管理部”，上级部门为“总部”。`
- 修改部门：`修改部门“综合管理部”：部门名称由“综合部”改为“综合管理部”；排序由“10”改为“20”。`
- 删除部门：`删除部门“综合管理部”。`
- 新增人员：`新增人员“Bob”，所属部门为“Branch Unit”，岗位为“常规人员”。`
- 修改人员：`修改人员“Bob”：手机号由“空”改为“138****8888”；邮箱由“空”改为“bob@example.com”。`
- 修改人员岗位：`调整人员“Bob”的岗位：新增“管理员”，移除“常规人员”。`
- 新增岗位：`新增岗位“部门管理员”，所属部门为“Branch Unit”，岗位类型为“管理员”。`
- 修改岗位：`修改岗位“部门管理员”：岗位名称由“部门管理”改为“部门管理员”；启用状态由“停用”改为“启用”。`
- 岗位人员保存：`保存岗位“部门管理员”的人员：新增“Bob、Alice”，移除“Tom”。`
- 岗位权限保存：`保存岗位“部门管理员”的权限：新增“常规-人员-查看、常规-部门-修改”，移除“常规-岗位-删除”。`
- 保存配置：`保存系统登录配置：密码加密模式由“RSA_OAEP”改为“RSA_PKCS1”。`

敏感字段只记录字段名和变更动作，不记录明文值。例如密码、私钥、密钥类配置只显示：`登录私钥已更新。`

## 采集范围

第一阶段覆盖以下写操作：

| 模块 | 操作 |
| --- | --- |
| 系统配置 | 保存配置 |
| 部门 | 新增、修改、删除、排序调整 |
| 人员 | 新增、修改、删除、头像上传、头像清空、人员岗位保存 |
| 岗位 | 新增、修改、删除、岗位人员保存、岗位权限保存、刷新人员权限 |

后续如果新增其他管理模块，应复用同一套 `OperationLogService` 记录方式。

## 后端设计

在 `modules/hephaestus-login` 的 `log` 包内新增操作日志能力，与现有登录日志并列。

建议新增包和类：

- `login.log.domain.OperationLogEntity`
- `login.log.dto.OperationLogResponse`
- `login.log.dto.OperationLogRecordRequest`
- `login.log.repository.OperationLogRepository`
- `login.log.service.OperationLogService`
- `login.log.service.impl.OperationLogServiceImpl`
- `login.log.controller.OperationLogController`
- `login.log.constant.OperationLogActionType`
- `login.log.constant.OperationLogModuleType`

`OperationLogService` 提供面向业务的简洁入口：

```java
void recordSuccess(OperationLogRecordRequest request);

void recordFailure(OperationLogRecordRequest request, String message);
```

业务服务只负责传入模块、动作、对象、摘要、明细。操作人和请求信息优先由当前 Session、请求上下文或统一辅助组件补齐，避免每个业务服务重复解析 IP、User-Agent 和路径。

日志写入使用旁路策略：写入失败只记录应用日志，不回滚主业务事务。

## 数据库设计

通过新增 Liquibase changeset 创建 `heph_operation_log` 表。必须追加新 changeset，不能修改已经执行过的 changeset 内容。

建议索引：

- `idx_heph_operation_log_created_at`
- `idx_heph_operation_log_operator`
- `idx_heph_operation_log_module_action`
- `idx_heph_operation_log_target`

查询默认按 `created_at desc, id desc` 排序。

## 权限设计

沿用现有权限节点：

- `general.log`：显示“日志”大栏目。
- `general.log.operation`：显示“操作日志”页签。
- `general.log.operation.view`：允许查询操作日志列表和详情。

前端没有 `general.log.operation` 时不显示“操作日志”页签。没有 `general.log.operation.view` 时不调用查询接口。

后端 `OperationLogController` 必须校验 `general.log.operation.view`，避免只靠前端隐藏。

管理员岗位不受岗位权限勾选约束，仍拥有操作日志查询权限。

## 前端设计

前端沿用现有设置抽屉中的日志区域：

- 保持“日志”栏目不变。
- 保持“登录日志 / 操作日志”页签不变。
- 将操作日志占位文案替换为真实查询区域。

操作日志查询条件：

- 关键词：匹配操作人、对象名称、摘要、明细、IP。
- 模块：全部、系统配置、部门、人员、岗位。
- 动作：全部、新增、修改、删除、保存配置、分配人员、设置权限、刷新权限。
- 结果：全部、成功、失败。
- 时间范围。

操作日志列表字段：

- 时间
- 操作人
- 模块
- 动作
- 对象
- 结果
- 摘要

每条日志可以展开查看明细、IP、请求路径和 User-Agent。列表中不显示大段明细，避免页面变乱。

## 查询接口

建议新增接口：

```http
GET /api/logs/operations
```

查询参数：

- `keyword`
- `moduleCode`
- `actionCode`
- `success`
- `startTime`
- `endTime`
- `page`
- `pageSize`

响应保持当前分页结构 `Pagination<T>`，与登录日志分页方式一致。

## 错误处理

1. 业务操作成功、日志写入失败：业务仍返回成功，服务端记录错误日志。
2. 业务操作失败：默认不记录操作日志，除非该入口明确需要审计失败原因。
3. 查询无权限：后端返回无权限错误，前端按现有设置错误提示展示。
4. 查询失败：前端显示中文错误提示，不影响其他设置栏目。

## 验证计划

1. Java 编译验证，确认新增 Bean、Mapper、DTO 没有装配或扫描问题。
2. Liquibase 启动验证，确认只追加新 changeset，且表结构能正常创建。
3. 前端脚本执行 `node --check`。
4. 使用管理员账号执行一次部门、人员、岗位、配置保存操作，确认操作日志写入。
5. 使用有 `general.log.operation.view` 权限的账号确认能看到并查询操作日志。
6. 使用无 `general.log.operation` 或无 `general.log.operation.view` 权限的账号确认前端不显示或不查询，后端也拒绝。

## 待实施顺序

1. 新增数据库表和后端操作日志基础类。
2. 新增操作日志查询接口和权限校验。
3. 接入系统配置、部门、人员、岗位写操作的成功日志。
4. 接入前端操作日志查询和显示。
5. 补充权限校验和浏览器验证。
