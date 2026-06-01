# Hephaestus 岗位与组织权限功能 PRD

## 1. 文档目标

本文档根据当前代码实现和已确认需求，整理 `hephaestus` 组织设置中的岗位功能、人员岗位关系、岗位权限、人员权限快照、前端权限控制和默认数据要求。

本功能参考 `E:\NEW_WORK\wizdom-urban-v14-framework` 的岗位管理思路，但按 `hephaestus` 当前轻量组织模块实现，不完整复刻老系统。

## 2. 业务目标

- 在设置抽屉中新增“岗位”栏目，与“常规、部门、人员、日志”并列。
- 每个岗位归属一个部门，岗位树按部门层级展示，部门下面展示岗位。
- 每个人员可以拥有多个岗位，人员主部门仍保留在 `heph_person.unit_id`。
- 岗位承载权限；人员权限由人员拥有的所有岗位权限合并生成。
- 岗位页显示岗位人员，人员页显示人员岗位。
- 保存岗位权限、保存岗位人员、保存人员岗位或保存人员信息后，重新生成人员权限快照。
- 管理员岗位不受岗位权限勾选约束，拥有组织设置全部权限。
- 没有对应栏目显示权限时，前端不显示该栏目；没有操作权限时按钮隐藏或输入框置灰。

## 3. 用户范围

- 系统管理员：拥有管理员岗位类型，不受岗位权限约束，可看到全部组织设置栏目和操作。
- 普通管理人员：通过岗位权限获得部分组织设置能力，例如查看人员、编辑人员、维护岗位、查看日志等。
- 无权限人员：不能看到无显示权限的栏目，也不能通过前端按钮触发对应操作；后端接口仍要做权限校验。

## 4. 功能范围

### 4.1 岗位基础能力

岗位字段：

- 岗位编码：必填，唯一。
- 岗位名称：必填。
- 岗位简称：可选。
- 岗位描述：可选。
- 所属部门：必填，只能选择当前人员可管理范围内的部门。
- 岗位分组：可选。
- 岗位类型：来自岗位类型表，默认包含普通岗位、管理员、业务岗、支撑岗。
- 岗位属性：当前前端隐藏，后端保留字段。
- 排序：用于岗位在部门下排序。
- 启用状态：停用岗位不能作为新分配岗位使用。

岗位操作：

- 右键部门新增岗位。
- 右键岗位删除岗位。
- 右侧只保留“保存”和“刷新权限”按钮。
- 未选中岗位且未进入新增状态时，右侧显示空白提示。
- 删除已绑定人员的岗位时拒绝删除。

### 4.2 岗位树

- 左侧岗位树样式、宽度、搜索框和部门密度与人员树保持一致。
- 部门节点使用部门树样式，岗位节点使用浅绿色条样式区分。
- 岗位不显示额外“岗位标志”文案。
- 部门排序按 `sort_order` 和 `id` 生效。
- 部门树修改后，岗位树需要重新加载并体现最新部门结构。

### 4.3 岗位人员

- 岗位基本属性页显示本岗位下所有人员摘要。
- 岗位“人员”页显示人员树，树大小和样式与人员页一致。
- 人员树展示头像，支持勾选。
- 勾选后点击保存岗位，保存本岗位人员关系。
- 再次打开本岗位时，已属于该岗位的人员保持勾选状态。
- 无“岗位-分配人员”权限时，人员页签隐藏或人员勾选不可用。

### 4.4 人员岗位

- 人员编辑页显示“人员岗位”区域。
- 人员岗位只显示系统中已存在且可分配的岗位。
- 有“人员-分配岗位”权限时可勾选岗位并随人员保存。
- 无“人员-分配岗位”权限时岗位勾选置灰，保存人员不能清空原有岗位。
- 保存人员基础属性时，如果请求带有岗位列表，同步保存人员岗位并刷新该人员权限。

### 4.5 岗位权限

权限展示按五大块组织：

- 常规：系统登录、登录页面。
- 部门：部门查看、新增、修改、删除。
- 人员：人员查看、新增、修改、删除、分配岗位。
- 岗位：岗位查看、新增、修改、删除、分配人员、刷新权限。
- 日志：登录日志、操作日志。

权限规则：

- 栏目权限控制栏目是否显示，例如 `general.person` 控制人员栏目。
- 明细权限控制具体行为，例如 `general.person.update` 控制人员修改。
- 常规与日志中的二级栏目需要单独控制，例如系统登录、登录页面、登录日志、操作日志。
- 勾选明细权限时，前端自动勾选其父级栏目权限。
- 取消父级栏目权限时，前端取消对应子权限。
- 后端校验时，普通人员需要同时拥有父级栏目权限和明细权限。
- 管理员岗位类型为 `admin` 时，跳过普通岗位权限约束。

### 4.6 默认数据

Liquibase 只能追加 changeSet，不修改已执行 changeSet。

默认数据包括：

- 岗位类型表 `heph_role_type`。
- 默认岗位类型：
  - `normal`：普通岗位。
  - `admin`：管理员，`admin_flag = 1`。
  - `operator`：业务岗。
  - `support`：支撑岗。
- 系统管理员岗位：
  - `role_code = system_admin`
  - `role_type = admin`
  - `role_property = system`
- 系统管理员岗位绑定全部组织权限。
- 默认管理员人员绑定系统管理员岗位。

## 5. 数据模型

### 5.1 岗位表

`heph_role`

- `id`
- `role_code`
- `role_name`
- `role_short_name`
- `role_desc`
- `unit_id`
- `role_type`
- `role_group`
- `role_property`
- `enabled`
- `sort_order`
- `created_at`
- `updated_at`

### 5.2 权限表

`heph_permission`

- `id`
- `permission_code`
- `permission_name`
- `permission_group`
- `permission_section`
- `sort_order`
- `enabled`
- `created_at`
- `updated_at`

### 5.3 岗位权限关系

`heph_role_permission`

- `role_id`
- `permission_id`
- `permission_code`

### 5.4 人员岗位关系

`heph_person_role`

- `person_id`
- `role_id`

### 5.5 人员权限快照

`heph_person_permission`

- `person_id`
- `permission_id`
- `permission_code`

### 5.6 岗位类型

`heph_role_type`

- `id`
- `type_code`
- `type_name`
- `admin_flag`
- `sort_order`
- `enabled`
- `created_at`
- `updated_at`

## 6. 后端接口

接口统一挂载在 `/api/org` 下，当前人员通过 `X-Person-Id` 获取。

岗位接口：

- `GET /api/org/roles/tree`：查询岗位树。
- `GET /api/org/roles`：按部门、关键词、启用状态查询岗位。
- `GET /api/org/roles/{id}`：查询岗位详情、岗位权限和岗位人员。
- `POST /api/org/roles`：新增岗位。
- `PUT /api/org/roles/{id}`：保存岗位基础信息和岗位权限。
- `DELETE /api/org/roles/{id}`：删除岗位。
- `PUT /api/org/roles/{id}/people`：保存岗位人员。
- `POST /api/org/roles/{id}/refresh-permissions`：刷新本岗位人员权限。
- `GET /api/org/role-types`：查询启用岗位类型。

人员岗位接口：

- `GET /api/org/persons/{id}/roles`：查询人员岗位。
- `PUT /api/org/persons/{id}/roles`：保存人员岗位。

权限接口：

- `GET /api/org/permissions`：查询启用权限定义。
- `GET /api/org/persons/{id}/permissions`：查询指定人员权限快照。
- `GET /api/org/permissions/current`：查询当前人员是否管理员以及权限编码列表。

## 7. 权限编码

当前组织设置权限编码：

- `general.config`
- `general.config.login`
- `general.config.login.view`
- `general.config.login.update`
- `general.config.login-page`
- `general.config.login-page.view`
- `general.config.login-page.update`
- `general.unit`
- `general.unit.view`
- `general.unit.create`
- `general.unit.update`
- `general.unit.delete`
- `general.person`
- `general.person.view`
- `general.person.create`
- `general.person.update`
- `general.person.delete`
- `general.person.role.assign`
- `general.role`
- `general.role.view`
- `general.role.create`
- `general.role.update`
- `general.role.delete`
- `general.role.person.assign`
- `general.role.permission.refresh`
- `general.log`
- `general.log.login`
- `general.log.login.view`
- `general.log.operation`
- `general.log.operation.view`

## 8. 前端页面

新增文件：

- `modules/hephaestus-org/src/main/resources/static/org-role-settings.html`
- `modules/hephaestus-org/src/main/resources/static/org-role-settings.js`
- `modules/hephaestus-org/src/main/resources/static/org-role-settings.css`

修改文件：

- `modules/hephaestus-org/src/main/resources/static/org-settings-panel.html`
- `modules/hephaestus-org/src/main/resources/static/org-settings.js`
- `modules/hephaestus-org/src/main/resources/static/org-settings.css`
- `modules/hephaestus-app/src/main/resources/static/chat.html`

前端行为：

- 设置抽屉根据当前权限动态显示“常规、部门、人员、岗位、日志”栏目。
- 无栏目权限时不显示栏目。
- 无操作权限时按钮隐藏，表单输入框置灰。
- 部门、人员、岗位前端都做必填校验。
- 岗位页面 HTML 独立加载，避免继续膨胀人员页面。
- 修改前端静态资源后必须同步更新 `chat.html` 和动态 `fetch(...?v=)` 版本号。

## 9. 验收标准

- 设置抽屉存在岗位栏目，且栏目显示受 `general.role` 控制。
- 岗位树部门样式、密度、宽度与人员树一致，岗位以浅绿色条显示。
- 右键部门可以新增岗位，右键岗位可以删除岗位。
- 岗位基础信息可输入、可保存，必填项缺失时前端阻止保存。
- 岗位基本属性页显示岗位人员摘要。
- 岗位人员页显示带头像的人员树，勾选保存后再次打开仍保持勾选。
- 人员页显示人员岗位，有权限时可勾选保存。
- 无人员、部门、岗位对应权限时，栏目不显示；无操作权限时按钮不显示或表单置灰。
- 保存岗位权限后，岗位下人员权限快照刷新。
- 保存岗位人员后，新增和移除的人员权限快照刷新。
- 保存人员岗位后，该人员权限快照刷新。
- 管理员岗位不受岗位权限约束。
- 所有前端中文无乱码。
- `org-settings.js`、`org-role-settings.js`、`login.js` 通过 `node --check`。
- 涉及后端修改时，至少执行 `mvn -pl modules/hephaestus-org -am test` 或等价编译验证。

## 10. 当前风险和注意事项

- 人员管理范围仍基于人员主部门，不因多岗位自动扩展管理范围。
- Liquibase 已执行 changeSet 不允许修改，只能追加修正 changeSet。
- 前端仍是原生 HTML/CSS/JS，状态集中在 `org-settings.js` 和 `org-role-settings.js`，修改时要避免重复函数覆盖。
- 权限控制必须前后端一致：前端负责隐藏和置灰，后端负责最终拦截。
- 管理员判断基于启用岗位的 `role_type = admin`，需要保证岗位类型和管理员岗位默认数据存在。
