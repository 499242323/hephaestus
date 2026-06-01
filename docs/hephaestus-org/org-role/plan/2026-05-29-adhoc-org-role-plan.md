# 岗位与组织权限功能实施计划

> **给编码代理：** 执行本计划时需要使用 `superpowers:subagent-driven-development`（推荐）或 `superpowers:executing-plans`，并按任务逐项推进。步骤使用复选框（`- [ ]`）跟踪。

**目标：** 在 `hephaestus` 组织设置中实现岗位、岗位类型、人员岗位、岗位权限、人员权限快照和前后端权限控制。

**架构：** 后端在 `com.example.springaidemo.org.role` 下按 controller、service、service.impl、dto、domain、repository 分层；人员主部门继续由 `heph_person.unit_id` 表达，多岗位通过关系表表达。前端新增 `org-role-settings.*` 作为岗位页面，`org-settings.*` 只负责设置抽屉接线、栏目权限控制和人员页岗位选择。

**技术栈：** Java 17、Spring Boot 3、MyBatis 注解 SQL、Liquibase、Redis Session、原生 HTML/CSS/JS。

---

## 1. 当前实现文件

### 1.1 后端新增文件

- `modules/hephaestus-org/src/main/java/com/example/springaidemo/org/role/constant/OrgPermissionCodes.java`
- `modules/hephaestus-org/src/main/java/com/example/springaidemo/org/role/controller/OrgPermissionController.java`
- `modules/hephaestus-org/src/main/java/com/example/springaidemo/org/role/controller/OrgRoleController.java`
- `modules/hephaestus-org/src/main/java/com/example/springaidemo/org/role/domain/OrgPermissionEntity.java`
- `modules/hephaestus-org/src/main/java/com/example/springaidemo/org/role/domain/OrgPersonPermissionEntity.java`
- `modules/hephaestus-org/src/main/java/com/example/springaidemo/org/role/domain/OrgPersonRoleEntity.java`
- `modules/hephaestus-org/src/main/java/com/example/springaidemo/org/role/domain/OrgRoleEntity.java`
- `modules/hephaestus-org/src/main/java/com/example/springaidemo/org/role/domain/OrgRolePermissionEntity.java`
- `modules/hephaestus-org/src/main/java/com/example/springaidemo/org/role/domain/OrgRoleTypeEntity.java`
- `modules/hephaestus-org/src/main/java/com/example/springaidemo/org/role/dto/OrgCurrentPermissionResponse.java`
- `modules/hephaestus-org/src/main/java/com/example/springaidemo/org/role/dto/OrgPermissionItem.java`
- `modules/hephaestus-org/src/main/java/com/example/springaidemo/org/role/dto/OrgPersonRoleItem.java`
- `modules/hephaestus-org/src/main/java/com/example/springaidemo/org/role/dto/OrgRolePersonItem.java`
- `modules/hephaestus-org/src/main/java/com/example/springaidemo/org/role/dto/OrgRoleRequest.java`
- `modules/hephaestus-org/src/main/java/com/example/springaidemo/org/role/dto/OrgRoleResponse.java`
- `modules/hephaestus-org/src/main/java/com/example/springaidemo/org/role/dto/OrgRoleTreeNode.java`
- `modules/hephaestus-org/src/main/java/com/example/springaidemo/org/role/dto/OrgRoleTypeItem.java`
- `modules/hephaestus-org/src/main/java/com/example/springaidemo/org/role/dto/UpdatePersonRolesRequest.java`
- `modules/hephaestus-org/src/main/java/com/example/springaidemo/org/role/dto/UpdateRolePeopleRequest.java`
- `modules/hephaestus-org/src/main/java/com/example/springaidemo/org/role/repository/OrgPermissionRepository.java`
- `modules/hephaestus-org/src/main/java/com/example/springaidemo/org/role/repository/OrgPersonPermissionRepository.java`
- `modules/hephaestus-org/src/main/java/com/example/springaidemo/org/role/repository/OrgPersonRoleRepository.java`
- `modules/hephaestus-org/src/main/java/com/example/springaidemo/org/role/repository/OrgRolePermissionRepository.java`
- `modules/hephaestus-org/src/main/java/com/example/springaidemo/org/role/repository/OrgRoleRepository.java`
- `modules/hephaestus-org/src/main/java/com/example/springaidemo/org/role/repository/OrgRoleTypeRepository.java`
- `modules/hephaestus-org/src/main/java/com/example/springaidemo/org/role/service/OrgPermissionGuard.java`
- `modules/hephaestus-org/src/main/java/com/example/springaidemo/org/role/service/OrgPersonPermissionRefreshService.java`
- `modules/hephaestus-org/src/main/java/com/example/springaidemo/org/role/service/OrgPersonRoleService.java`
- `modules/hephaestus-org/src/main/java/com/example/springaidemo/org/role/service/OrgRoleService.java`
- `modules/hephaestus-org/src/main/java/com/example/springaidemo/org/role/service/impl/OrgPermissionGuardImpl.java`
- `modules/hephaestus-org/src/main/java/com/example/springaidemo/org/role/service/impl/OrgPersonPermissionRefreshServiceImpl.java`
- `modules/hephaestus-org/src/main/java/com/example/springaidemo/org/role/service/impl/OrgPersonRoleServiceImpl.java`
- `modules/hephaestus-org/src/main/java/com/example/springaidemo/org/role/service/impl/OrgRoleServiceImpl.java`

### 1.2 后端修改文件

- `modules/hephaestus-org/src/main/java/com/example/springaidemo/org/controller/OrgUnitController.java`
- `modules/hephaestus-org/src/main/java/com/example/springaidemo/org/domain/OrgPersonSummary.java`
- `modules/hephaestus-org/src/main/java/com/example/springaidemo/org/dto/CreateOrgPersonRequest.java`
- `modules/hephaestus-org/src/main/java/com/example/springaidemo/org/dto/UpdateOrgPersonRequest.java`
- `modules/hephaestus-org/src/main/java/com/example/springaidemo/org/service/OrgPersonService.java`
- `modules/hephaestus-org/src/main/java/com/example/springaidemo/org/service/OrgScopeService.java`
- `modules/liquibase/src/main/resources/db/changelog/db.changelog.xml`

### 1.3 前端新增和修改文件

- `modules/hephaestus-org/src/main/resources/static/org-role-settings.html`
- `modules/hephaestus-org/src/main/resources/static/org-role-settings.js`
- `modules/hephaestus-org/src/main/resources/static/org-role-settings.css`
- `modules/hephaestus-org/src/main/resources/static/org-settings-panel.html`
- `modules/hephaestus-org/src/main/resources/static/org-settings.js`
- `modules/hephaestus-org/src/main/resources/static/org-settings.css`
- `modules/hephaestus-app/src/main/resources/static/chat.html`

## 2. 已完成任务清单

### Task 1: 建表和默认数据

- [x] 追加 `heph_role` 表。
- [x] 追加 `heph_permission` 表。
- [x] 追加 `heph_role_permission` 表。
- [x] 追加 `heph_person_role` 表。
- [x] 追加 `heph_person_permission` 表。
- [x] 追加 `heph_role_type` 表。
- [x] 追加默认岗位类型：普通岗位、管理员、业务岗、支撑岗。
- [x] 追加系统管理员岗位 `system_admin`。
- [x] 追加系统管理员岗位权限。
- [x] 追加系统管理员人员岗位绑定。
- [x] 追加五大块组织权限：常规、部门、人员、岗位、日志。
- [x] 追加常规和日志拆分权限：系统登录、登录页面、登录日志、操作日志。
- [x] 通过追加 changeSet 修正权限数据，不修改已执行 changeSet。

### Task 2: 岗位领域和仓储

- [x] 新增岗位、权限、岗位权限、人员岗位、人员权限、岗位类型实体。
- [x] 新增对应 MyBatis 仓储接口。
- [x] 岗位查询支持按当前人员可管理部门范围过滤。
- [x] 岗位树按部门排序，并在部门下按岗位排序展示岗位。
- [x] 岗位详情返回岗位人员和岗位权限。

### Task 3: 人员权限生成

- [x] 实现 `OrgPersonPermissionRefreshService`。
- [x] 刷新单个人员权限时，先删除旧快照，再按人员岗位查询岗位权限。
- [x] 多岗位权限按 `permission_code` 去重。
- [x] 支持按岗位刷新本岗位下全部人员权限。
- [x] 支持批量刷新受影响人员权限。

### Task 4: 岗位服务

- [x] 实现岗位树、列表、详情、新增、修改、删除。
- [x] 新增岗位时校验岗位编码、岗位名称、所属部门。
- [x] 修改岗位时校验编码唯一和部门范围。
- [x] 保存岗位权限后刷新本岗位人员权限。
- [x] 删除已绑定人员的岗位时拒绝删除。
- [x] 保存岗位人员后刷新新增和移除人员权限。
- [x] 刷新权限按钮调用本岗位人员权限刷新逻辑。

### Task 5: 人员岗位服务

- [x] 实现查询人员岗位。
- [x] 实现保存人员岗位。
- [x] 保存人员岗位时校验人员和岗位都在当前管理范围内。
- [x] 停用岗位不允许新分配。
- [x] 保存人员岗位后刷新该人员权限。
- [x] 人员新增和修改请求支持 `roleIds`。
- [x] 人员摘要返回 `roles`，用于人员页展示人员岗位。

### Task 6: 权限守卫

- [x] 实现 `OrgPermissionGuard`。
- [x] 管理员岗位类型 `admin` 不受普通权限勾选约束。
- [x] 普通人员访问明细权限时，同时校验父级栏目权限。
- [x] 支持获取当前人员权限：`GET /api/org/permissions/current`。
- [x] 部门、人员、岗位、常规配置、日志接口接入权限校验。

### Task 7: 岗位前端

- [x] 新增 `org-role-settings.html`。
- [x] 新增 `org-role-settings.js`。
- [x] 新增 `org-role-settings.css`。
- [x] 设置抽屉新增“岗位”栏目。
- [x] 岗位页面左侧显示部门岗位树。
- [x] 岗位树右键部门新增岗位，右键岗位删除岗位。
- [x] 岗位右侧未选中或未新增时显示空白提示。
- [x] 岗位右侧只保留保存和刷新权限按钮。
- [x] 岗位基础页显示岗位人员摘要。
- [x] 岗位人员页显示人员树、头像和勾选状态。
- [x] 岗位权限页按五大块显示权限。
- [x] 岗位标识和岗位属性不在页面可见区显示。
- [x] 岗位类型从接口加载，前端保留兜底选项。

### Task 8: 人员和部门前端权限控制

- [x] 人员页显示人员岗位。
- [x] 人员岗位有分配权限时可勾选保存，无权限时置灰且不清空原岗位。
- [x] 部门、人员、岗位栏目无显示权限时不显示。
- [x] 无新增、修改、删除、分配、刷新权限时隐藏对应按钮或置灰输入框。
- [x] 部门、人员、岗位保存前做前端必填校验。
- [x] 部门修改后重新加载岗位页面，保证岗位树部门结构更新。

### Task 9: 前端样式和乱码收口

- [x] 岗位树左侧宽度、搜索框、部门密度与人员树保持一致。
- [x] 岗位节点使用浅绿色条样式。
- [x] 岗位树不显示岗位标志文案。
- [x] 权限页改为五大块排版。
- [x] 清理 `org-settings.js` 重复人员岗位函数。
- [x] 清理 `org-role-settings.js` 重复权限渲染函数。
- [x] 更新 `chat.html` 和动态 `fetch(...?v=)` 静态资源版本号。
- [x] 扫描改动前端文件，未发现乱码和异常孤立 `?`。

## 3. 接口与权限映射

### 3.1 岗位接口

- `GET /api/org/roles/tree`：需要 `general.role.view`。
- `GET /api/org/roles`：需要 `general.role.view`。
- `GET /api/org/roles/{id}`：需要 `general.role.view`。
- `POST /api/org/roles`：需要 `general.role.create`。
- `PUT /api/org/roles/{id}`：需要 `general.role.update`。
- `DELETE /api/org/roles/{id}`：需要 `general.role.delete`。
- `PUT /api/org/roles/{id}/people`：需要 `general.role.person.assign`。
- `POST /api/org/roles/{id}/refresh-permissions`：需要 `general.role.permission.refresh`。
- `GET /api/org/role-types`：需要 `general.role.view`。

### 3.2 人员岗位接口

- `GET /api/org/persons/{id}/roles`：要求目标人员在当前范围内。
- `PUT /api/org/persons/{id}/roles`：需要岗位分配权限，当前代码使用 `general.role.person.assign`。

### 3.3 权限接口

- `GET /api/org/permissions`：查询权限定义。
- `GET /api/org/persons/{id}/permissions`：要求目标人员在当前范围内。
- `GET /api/org/permissions/current`：查询当前人员权限列表和管理员状态。

## 4. 剩余检查任务

### Task 10: 权限编码语义复核

**文件：**

- 按需修改：`modules/hephaestus-org/src/main/java/com/example/springaidemo/org/role/service/impl/OrgPersonRoleServiceImpl.java`
- 按需修改：`modules/hephaestus-org/src/main/resources/static/org-settings.js`
- 测试：`modules/hephaestus-org/src/test/java/com/example/springaidemo/org/role/service/OrgPersonRoleServiceImplTest.java`

- [ ] **Step 1: 确认“人员页分配岗位”使用的权限编码**

当前前端人员页使用 `general.person.role.assign` 控制人员岗位勾选；当前 `OrgPersonRoleServiceImpl.replacePersonRoles` 使用 `general.role.person.assign`。需要确认最终语义：

```java
permissionGuard.requirePermission(currentPersonId, OrgPermissionCodes.GENERAL_ROLE_PERSON_ASSIGN);
```

若需求定义为“人员页给人员分配岗位”，后端应改为：

```java
permissionGuard.requirePermission(currentPersonId, OrgPermissionCodes.GENERAL_PERSON_ROLE_ASSIGN);
```

- [ ] **Step 2: 写权限守卫测试**

测试目标：拥有 `general.person` + `general.person.role.assign` 的人员可以通过人员页保存人员岗位。

运行：

```powershell
mvn -pl modules/hephaestus-org -am -Dtest=OrgPersonRoleServiceImplTest test
```

预期：修正前如果后端仍要求 `general.role.person.assign`，测试失败；修正后通过。

- [ ] **Step 3: 保持岗位页分配人员权限不变**

岗位页维护岗位人员仍使用：

```java
OrgPermissionCodes.GENERAL_ROLE_PERSON_ASSIGN
```

人员页维护人员岗位使用：

```java
OrgPermissionCodes.GENERAL_PERSON_ROLE_ASSIGN
```

### Task 11: 后端自动化验证补齐

**文件：**

- 测试：`modules/hephaestus-org/src/test/java/com/example/springaidemo/org/role/service/OrgPersonPermissionRefreshServiceImplTest.java`
- 测试：`modules/hephaestus-org/src/test/java/com/example/springaidemo/org/role/service/OrgRoleServiceImplTest.java`
- 测试：`modules/hephaestus-org/src/test/java/com/example/springaidemo/org/role/service/OrgPersonRoleServiceImplTest.java`

- [ ] **Step 1: 验证权限刷新去重**

覆盖场景：

```text
人员 100 拥有岗位 10 和 11。
岗位 10 有 general.person.view、general.person.update。
岗位 11 有 general.person.view、general.person.delete。
刷新人员 100 后，快照只有 3 条：view、update、delete。
```

- [ ] **Step 2: 验证岗位保存刷新人员权限**

覆盖场景：

```text
岗位 10 已绑定人员 100。
修改岗位 10 的权限。
调用 updateRole。
断言 refreshByRoleId(10) 被调用。
```

- [ ] **Step 3: 验证岗位人员保存刷新受影响人员**

覆盖场景：

```text
岗位 10 原人员为 100、101。
新人员为 101、102。
保存后刷新人员集合应为 100、101、102。
```

- [ ] **Step 4: 验证删除绑定人员岗位被拒绝**

覆盖场景：

```text
personRoleRepository.findPersonIdsByRoleId(roleId) 返回非空。
调用 deleteRole。
期望抛出 OrgValidationException，消息包含“岗位已绑定人员”。
```

### Task 12: 前端回归验证

**文件：**

- 验证：`modules/hephaestus-app/src/main/resources/static/chat.html`
- 验证：`modules/hephaestus-login/src/main/resources/static/login.js`
- 验证：`modules/hephaestus-org/src/main/resources/static/org-settings.js`
- 验证：`modules/hephaestus-org/src/main/resources/static/org-role-settings.js`

- [ ] **Step 1: JS 语法检查**

运行：

```powershell
node --check modules\hephaestus-login\src\main\resources\static\login.js
node --check modules\hephaestus-org\src\main\resources\static\org-settings.js
node --check modules\hephaestus-org\src\main\resources\static\org-role-settings.js
```

预期：三条命令退出码为 0。

- [ ] **Step 2: 乱码扫描**

扫描范围：

```text
modules/hephaestus-app/src/main/resources/static/chat.html
modules/hephaestus-login/src/main/resources/static/login.html
modules/hephaestus-login/src/main/resources/static/login.js
modules/hephaestus-org/src/main/resources/static/org-role-settings.css
modules/hephaestus-org/src/main/resources/static/org-role-settings.html
modules/hephaestus-org/src/main/resources/static/org-role-settings.js
modules/hephaestus-org/src/main/resources/static/org-settings-panel.html
modules/hephaestus-org/src/main/resources/static/org-settings.css
modules/hephaestus-org/src/main/resources/static/org-settings.js
```

预期：无 `U+FFFD`、无常见 mojibake、无 UI 孤立 `?`。

- [ ] **Step 3: 手工页面核对**

验收点：

```text
没有权限的用户不显示常规、部门、人员、岗位、日志中无权限的栏目。
有查看无修改权限时，表单置灰，保存按钮不可见。
右键部门菜单显示在鼠标点击下方。
岗位树部门密度与人员树一致，岗位为浅绿色条。
岗位人员树显示头像，可勾选，保存后再次打开仍勾选。
人员页显示人员岗位，有权限时可勾选保存。
岗位权限页为五大块，不显示“5 项操作”这类统计文案。
```

### Task 13: 整体验证和收口

**文件：**

- 验证所有改动文件。

- [ ] **Step 1: Java 编译或测试**

运行：

```powershell
mvn -pl modules/hephaestus-org -am test
```

预期：构建成功，无 Bean 注入、Mapper 扫描、编译错误。

- [ ] **Step 2: Liquibase 风险核对**

确认：

```text
没有修改已执行 changeSet 的内容。
新增表字段与实体字段一致。
默认权限和管理员岗位通过追加 changeSet 修正。
```

- [ ] **Step 3: Git 差异检查**

运行：

```powershell
git diff --check
git diff --name-only
```

预期：

```text
diff --check 无空白错误。
差异文件均与岗位、权限、登录加密、组织设置相关；没有无关格式化。
```

## 5. 当前验证记录

最近一次前端检查结果：

- `node --check modules\hephaestus-login\src\main\resources\static\login.js`：通过。
- `node --check modules\hephaestus-org\src\main\resources\static\org-settings.js`：通过。
- `node --check modules\hephaestus-org\src\main\resources\static\org-role-settings.js`：通过。
- 改动前端页面乱码扫描：通过。
- HTMLParser 检查 `chat.html`、`login.html`、`org-role-settings.html`、`org-settings-panel.html`：通过。
- 重复函数扫描：通过。
- `git diff --check`：无空白错误，仅 Windows LF/CRLF 提示。

## 6. 注意事项

- Liquibase 只追加 changeSet，不改历史 changeSet。
- 文档和前端文案统一中文。
- 前端静态资源变更后同步更新 `chat.html` 和动态 `fetch(...?v=)` 版本号。
- 不要用会破坏 UTF-8 中文文件的写入方式批量改中文文件。
- 前端权限控制是体验层，后端 `OrgPermissionGuard` 是最终权限边界。
- 管理员岗位判断依赖启用岗位类型 `admin`，不要只看岗位名称。
