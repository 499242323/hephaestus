# Hephaestus Org Settings PRD

## Goal

为 Hephaestus 新增独立的组织管理能力，支持树形 `unit` 与人员管理，人员可绑定头像媒体文件，并在聊天页面中提供类似 ChatGPT 风格的“设置”抽屉入口，完成组织数据的可视化管理。

## Context

- 当前项目采用 Maven 多模块结构，已包含 `liquibase`、`mybatis`、`hephaestus-media`、`hephaestus-app`。
- `hephaestus-app` 负责 Spring Boot 启动与静态聊天页面承载，当前前端主要集中在 `chat.html`、`chat.js`、`chat.css`。
- `hephaestus-media` 已具备媒体上传、落盘和访问能力，可复用来存储人员头像。
- 当前系统没有完整登录态，也没有现成的用户、人员、部门、权限模型。
- 用户要求新增独立 module 实现组织功能，不将人员和 `unit` 逻辑直接塞入现有聊天模块。
- 用户明确要求：
  - 部门统一命名为 `unit`
  - `unit` 采用树形结构
  - 人员头像走媒体上传存储
  - 前端以 ChatGPT 风格设置抽屉呈现
  - 权限为数据权限，只能管理自己所在 `unit` 及下级 `unit`

## Repository Findings

- 父工程 [pom.xml](E:/NEW_WORK/hephaestus/pom.xml) 当前模块结构清晰，适合新增 `modules/hephaestus-org`。
- [modules/hephaestus-app/pom.xml](E:/NEW_WORK/hephaestus/modules/hephaestus-app/pom.xml) 当前承担启动应用角色，适合作为新模块的依赖消费方。
- [modules/liquibase/src/main/resources/db/changelog/db.changelog.xml](E:/NEW_WORK/hephaestus/modules/liquibase/src/main/resources/db/changelog/db.changelog.xml) 目前已承载聊天与媒体表的变更，适合继续追加组织相关表结构。
- [modules/hephaestus-app/src/main/resources/static/chat.html](E:/NEW_WORK/hephaestus/modules/hephaestus-app/src/main/resources/static/chat.html) 为单页静态聊天入口，适合在侧栏或主视图内扩展设置入口与抽屉容器。
- `hephaestus-media` 已有 `MediaFileController`、`MediaAccessController`、`MediaFileService`、`MediaStorageService` 等能力，可直接服务头像上传与访问。
- 项目当前无登录态，因此数据权限必须先通过最小“当前人员上下文”方案完成闭环。

## Design

### 1. 模块拆分

新增 `modules/hephaestus-org` 模块，负责以下职责：

- `unit` 树形组织管理
- 人员管理
- 人员头像媒体绑定
- 基于“当前人员”的数据权限校验
- 对前端提供组织管理接口

`hephaestus-app` 继续保留：

- Spring Boot 启动入口
- 聊天相关接口
- 聊天页与设置抽屉前端资源

`hephaestus-org` 作为被 `hephaestus-app` 依赖的业务模块，不单独承担启动职责。

### 2. 数据模型

建议新增两张业务表。

`heph_unit`

- `id` bigint 主键
- `unit_code` varchar 唯一编码
- `unit_name` varchar 名称
- `parent_id` bigint 父节点 ID，根节点可为空或固定为 `0`
- `ancestor_path` varchar 祖先路径，例如 `1/4/7`
- `sort_order` int 排序值
- `enabled` tinyint 或 boolean 启用状态
- `created_at` timestamp
- `updated_at` timestamp

`heph_person`

- `id` bigint 主键
- `person_code` varchar 唯一编码
- `person_name` varchar 姓名
- `unit_id` bigint 所属 `unit`
- `avatar_media_id` bigint 头像媒体 ID
- `mobile` varchar 手机号
- `email` varchar 邮箱
- `remark` varchar 或 text 备注
- `enabled` tinyint 或 boolean 启用状态
- `created_at` timestamp
- `updated_at` timestamp

设计约束：

- 一个人员只能属于一个 `unit`
- 头像只保存 `avatar_media_id`，不在人员表中冗余媒体访问 URL
- 删除 `unit` 时，如果存在下级 `unit` 或人员，必须阻止删除
- `ancestor_path` 用于高效计算当前 `unit` 的下级数据范围

### 3. 权限模型

当前没有登录态，第一版采用“当前人员上下文”最小方案：

- 前端在设置抽屉中选择“当前人员”
- 后续组织管理请求统一依赖登录 Session
- 后端通过 Session 当前人员查出所属 `unit`
- 再根据当前 `unit` 的 `id` 与 `ancestor_path` 计算可管理范围

权限规则：

- 当前人员只能查看自己所属 `unit` 及其下级 `unit`
- 当前人员只能新增、编辑、删除自己权限范围内的 `unit`
- 当前人员只能查看、新增、编辑、删除自己权限范围内的人员
- 当前人员新增人员时，只能将人员挂到自己可管理的 `unit`
- 如果请求中的 `unitId` 或 `personId` 超出权限范围，后端直接拒绝

这是一种数据权限闭环方案，不依赖完整登录系统，但可以真实限制后端数据访问。

### 4. 接口设计

建议提供以下接口：

`unit`

- `GET /api/org/units/tree`
  - 返回当前人员权限范围内的树形 `unit`
- `POST /api/org/units`
  - 新增 `unit`
- `PUT /api/org/units/{id}`
  - 修改 `unit`
- `DELETE /api/org/units/{id}`
  - 删除 `unit`

`person`

- `GET /api/org/persons`
  - 支持按 `personName`、`unitId`、`enabled` 查询当前权限范围内人员
- `GET /api/org/persons/current-scope`
  - 返回当前人员信息与可管理 `unit` 范围，供前端渲染上下文
- `POST /api/org/persons`
  - 新增人员
- `PUT /api/org/persons/{id}`
  - 修改人员
- `DELETE /api/org/persons/{id}`
  - 删除人员
- `POST /api/org/persons/{id}/avatar`
  - 上传头像并绑定到人员

接口约束：

- 所有组织管理接口默认要求存在有效登录 Session
- 后端统一做权限收口，前端不能单独决定权限边界
- 头像绑定接口内部复用现有媒体上传能力，成功后回写 `avatar_media_id`

### 5. 前端交互设计

前端继续基于现有聊天单页扩展，不新增独立前端工程。

入口设计：

- 在聊天页面新增类似 ChatGPT 的“设置”入口
- 点击后从右侧滑出抽屉式设置面板
- 抽屉支持关闭、恢复聊天视图，不跳转新页面

抽屉内容：

- 顶部显示 `Settings` 标题
- 提供“当前人员”切换器，作为最小权限上下文入口
- 分为两个主分区：
  - `Units`
  - `People`

`Units` 分区：

- 树形列表展示 `unit`
- 支持节点展开/折叠
- 支持新增子 `unit`
- 支持编辑当前 `unit`
- 支持删除叶子 `unit`
- 删除前展示约束提示

`People` 分区：

- 列表展示头像、姓名、所属 `unit`、联系方式、状态
- 支持按姓名和 `unit` 筛选
- 支持新增、编辑、删除人员
- 支持头像上传与即时预览

视觉风格：

- 参考 ChatGPT 设置面板
- 使用浅色背景、轻边框、圆角、克制色彩与弱分割线
- 保持与当前聊天页面风格一致，不做传统后台管理系统的重表格设计
- 避免二级弹窗堆叠，优先在抽屉内部切换编辑态

### 6. 异常与边界处理

- 未登录或登录过期时，返回明确错误
- Session 当前人员不存在时，返回明确错误
- 当前人员未绑定 `unit` 时，禁止进入管理态
- 越权访问任意 `unit` 或人员时，后端返回拒绝
- 删除 `unit` 前发现存在子节点或人员时，返回业务提示
- 上传头像失败时，不应破坏既有人员数据
- 媒体上传成功但人员绑定失败时，应记录日志并返回可定位错误

## TDD Plan

### 测试位置

- `modules/hephaestus-org/src/test/java/...`
- 前端行为验证可通过现有静态页面联调和必要的控制器测试补足

### First Failing Test

优先从数据权限开始写第一个失败测试：

- 当前人员仅能看到自己 `unit` 及下级 `unit` 的树和人员列表

这是本次需求最关键、最容易被忽略的核心约束。

### 关键测试场景

- 新增根 `unit`、子 `unit`、多级 `unit` 成功
- 修改 `unit` 基本信息成功
- 有子 `unit` 时删除失败
- `unit` 下存在人员时删除失败
- 新增人员时必须且只能绑定单个 `unit`
- 当前人员只能查询权限范围内的人员
- 当前人员越权编辑或删除他人 `unit` / 人员时失败
- 当前人员只能把新人员挂到可管理 `unit`
- 头像上传成功后，人员查询能返回 `avatarAccessUrl`
- 抽屉打开与关闭正常
- 树形 `unit` 与人员列表切换正常
- 上传头像后界面预览和列表刷新正常

### 最小实现范围

第一版实现以下闭环即可视为完成：

- 新增 `hephaestus-org` 模块
- Liquibase 增加 `heph_unit` 与 `heph_person`
- 后端完成树形 `unit`、人员、头像绑定、数据权限接口
- 聊天页面新增设置抽屉
- 抽屉可完成 `unit` 与人员的基本 CRUD
- 数据权限在后端真实生效

### 验证命令

- `rtk .\\tools\\mvn-java21.ps1 test`
- 必要时按模块执行测试以缩小范围

## Risks

- 历史第一版曾使用前端人员身份请求头联调；当前实现已统一改为后端从登录 Session 解析当前人员。
- 树形 `unit` 如果后续需要跨树移动节点，需要额外处理 `ancestor_path` 更新
- 头像媒体如果未来引入回收机制，需要明确人员解绑后的清理策略
- 抽屉式设置与现有聊天页耦合较高，前端状态管理要避免影响聊天主流程

## Verification

- 检查 PRD 是否覆盖模块拆分、树形 `unit`、人员头像、数据权限、前端抽屉、TDD 计划
- 检查是否存在未决的占位项或与用户确认内容不一致的设计
- 用户审阅 PRD 后，再进入 plan 阶段
