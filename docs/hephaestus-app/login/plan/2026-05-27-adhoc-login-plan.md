# Hephaestus 登录模块实施计划

> **给 agentic worker 的要求：** 按任务逐项实施和复核；如果继续执行未完成项，使用 `superpowers:executing-plans`。步骤使用 checkbox (`- [ ]`) 跟踪。

**目标：** 建设独立 `modules/hephaestus-login`，完成登录页、认证接口、Redis Session、密码加密传输、系统配置动态表单、公开配置读取、Redis 配置缓存和登录页视觉配置。

**架构：** 登录模块承载认证与配置能力，`hephaestus-app` 作为启动聚合模块依赖登录模块，`hephaestus-org` 提供人员账号数据，`liquibase` 负责系统配置表结构与初始化数据。配置读取优先使用配置表和值表，缺失时回退 YML，必要时运行期生成临时 RSA 密钥对。

**技术栈：** Java 17、Spring Boot 3.5、Spring MVC、Spring Session Redis、StringRedisTemplate、MyBatis-Plus、Liquibase XML、原生 HTML/CSS/JavaScript、Web Crypto。

---

## 1. 当前状态

本计划已完成主体实现，当前文档用于统一记录模块建设流程、实现边界、验证项和后续补充项。

已完成：

- 新增 `modules/hephaestus-login` 模块。
- 接入 `spring-session-data-redis`。
- 新增登录页静态资源。
- 新增登录接口、退出接口、当前用户接口。
- 新增登录拦截器。
- 新增系统配置定义表和值表。
- 新增系统配置动态表单接口和公开配置接口。
- 新增 Redis 配置缓存与保存后清理逻辑。
- 新增登录页标题、副标题、背景图、网格、鼠标拖尾等配置项。
- 密码加密支持配置项、YML 和运行期临时密钥兜底。
- 配置项常量集中到 `LoginConfigConst`。
- 设置页“常规 -> 主系统配置”可维护配置项。

仍需按环境复核：

- Redis 中 Session 和配置缓存是否符合预期。
- 服务重启后 Redis 配置缓存是否重新获取。
- 浏览器中登录页背景图、网格、拖尾效果是否按配置切换。
- 生产环境是否准备固定 RSA 密钥对，避免长期依赖运行期临时密钥。

## 2. 文件结构

### 2.1 Maven 与模块

- 修改：`pom.xml`
- 修改：`modules/hephaestus-app/pom.xml`
- 新增：`modules/hephaestus-login/pom.xml`

### 2.2 登录认证

- 新增：`modules/hephaestus-login/src/main/java/com/example/springaidemo/login/auth/AuthController.java`
- 新增：`modules/hephaestus-login/src/main/java/com/example/springaidemo/login/auth/AuthService.java`
- 新增：`modules/hephaestus-login/src/main/java/com/example/springaidemo/login/auth/LoginRequest.java`
- 新增：`modules/hephaestus-login/src/main/java/com/example/springaidemo/login/auth/LoginResponse.java`
- 新增：`modules/hephaestus-login/src/main/java/com/example/springaidemo/login/auth/LoginSessionUser.java`
- 新增：`modules/hephaestus-login/src/main/java/com/example/springaidemo/login/auth/LoginRequiredInterceptor.java`
- 新增：`modules/hephaestus-login/src/main/java/com/example/springaidemo/login/auth/LoginWebMvcConfiguration.java`
- 新增：`modules/hephaestus-login/src/main/java/com/example/springaidemo/login/auth/LoginException.java`
- 新增：`modules/hephaestus-login/src/main/java/com/example/springaidemo/login/auth/LoginExceptionHandler.java`
- 新增：`modules/hephaestus-login/src/main/java/com/example/springaidemo/login/auth/RsaPasswordCryptoService.java`
- 新增：`modules/hephaestus-login/src/main/java/com/example/springaidemo/login/auth/LoginPasswordCryptoProperties.java`

### 2.3 系统配置

- 新增：`modules/hephaestus-login/src/main/java/com/example/springaidemo/login/config/LoginConfigConst.java`
- 新增：`modules/hephaestus-login/src/main/java/com/example/springaidemo/login/config/controller/SystemConfigController.java`
- 新增：`modules/hephaestus-login/src/main/java/com/example/springaidemo/login/config/service/SystemConfigService.java`
- 新增：`modules/hephaestus-login/src/main/java/com/example/springaidemo/login/config/service/SystemConfigCacheService.java`
- 新增：`modules/hephaestus-login/src/main/java/com/example/springaidemo/login/config/repository/SystemConfigDefinitionRepository.java`
- 新增：`modules/hephaestus-login/src/main/java/com/example/springaidemo/login/config/repository/SystemConfigValueRepository.java`
- 新增：`modules/hephaestus-login/src/main/java/com/example/springaidemo/login/config/domain/SystemConfigDefinitionEntity.java`
- 新增：`modules/hephaestus-login/src/main/java/com/example/springaidemo/login/config/domain/SystemConfigValueEntity.java`
- 新增：`modules/hephaestus-login/src/main/java/com/example/springaidemo/login/config/dto/*`

### 2.4 数据库变更

- 修改：`modules/liquibase/src/main/resources/db/changelog/db.changelog.xml`

涉及表：

- `sys_config_definition`
- `sys_config_value`

### 2.5 前端资源

- 新增：`modules/hephaestus-login/src/main/resources/static/login.html`
- 新增：`modules/hephaestus-login/src/main/resources/static/login.css`
- 新增：`modules/hephaestus-login/src/main/resources/static/login.js`
- 修改：`modules/hephaestus-app/src/main/resources/static/chat.html`
- 修改：`modules/hephaestus-app/src/main/resources/static/chat.js`
- 修改：`modules/hephaestus-org/src/main/resources/static/org-settings-panel.html`
- 修改：`modules/hephaestus-org/src/main/resources/static/org-settings.js`
- 修改：`modules/hephaestus-org/src/main/resources/static/org-settings.css`

## 3. 实施任务

### 任务 1：新增登录模块

**目标：** 让父工程识别 `modules/hephaestus-login`，并让主应用能聚合登录模块能力。

- [x] 在父工程 `pom.xml` 中加入 `modules/hephaestus-login`。
- [x] 新建 `modules/hephaestus-login/pom.xml`。
- [x] 登录模块引入 Web、Spring Session Redis、MyBatis、Org、Test 等依赖。
- [x] 在 `modules/hephaestus-app/pom.xml` 中依赖 `hephaestus-login`。
- [x] 编译确认 Maven 模块图可解析。

验证命令：

```powershell
rtk proxy powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\mvn-java21.ps1 -pl modules/hephaestus-login -am -DskipTests compile
```

### 任务 2：新增系统配置表结构与初始配置

**目标：** 通过 Liquibase 新增配置定义和值表，并初始化主系统配置。

- [x] 新增 `sys_config_definition`。
- [x] 新增 `sys_config_value`。
- [x] 初始化 `main-system` 分组。
- [x] 初始化登录超时配置。
- [x] 初始化密码加密配置。
- [x] 初始化登录页标题、副标题。
- [x] 初始化鼠标拖尾配置。
- [x] 初始化登录页背景图 `media_id` 配置，默认值为 `1`。
- [x] 初始化登录页网格背景开关。

配置项：

- `login.session.timeout.minutes`
- `login.password.encrypt.enabled`
- `login.password.encrypt.algorithm`
- `login.password.encrypt.cipher-transformation`
- `login.password.encrypt.public-key`
- `login.password.encrypt.private-key`
- `login.page.title`
- `login.page.subtitle`
- `login.mouse.trail.effect`
- `login.page.background.media-id`
- `login.page.background.grid.enabled`

验证要点：

- Liquibase 能创建表。
- 初始化数据不会重复插入。
- 新增字段或配置项时，继续追加新的 changelog，不直接改已执行变更。

### 任务 3：实现系统配置后端能力

**目标：** 支持配置动态表单、保存、公开读取和 Redis 缓存。

- [x] 实现配置定义实体和值实体。
- [x] 实现配置定义和值仓储。
- [x] 实现 `SystemConfigService`。
- [x] 实现 `SystemConfigCacheService`。
- [x] 实现 `GET /api/system-config/forms/{groupCode}`。
- [x] 实现 `PUT /api/system-config/forms/{groupCode}`。
- [x] 实现 `GET /api/system-config/public/{groupCode}`。
- [x] 保存配置后清理单项配置缓存和公开配置缓存。
- [x] 应用启动后清理配置缓存，避免 Redis 中旧配置影响新版本。
- [x] 公开配置接口只返回公开且非敏感配置。
- [x] 私钥不通过公开配置接口返回。

验证要点：

- `GET /api/system-config/forms/main-system` 返回分区、字段、控件类型和值。
- `PUT /api/system-config/forms/main-system` 保存后返回最新表单。
- `GET /api/system-config/public/main-system` 不包含私钥。
- 保存配置后再次读取能拿到新值。

### 任务 4：实现密码加密和密钥兜底

**目标：** 不依赖 HTTPS 的前提下，完成应用层密码加密传输。

- [x] 实现 `RsaPasswordCryptoService`。
- [x] 前端使用公开公钥加密密码。
- [x] 后端使用私钥解密密码。
- [x] 密钥优先读取配置项。
- [x] 配置项缺失时读取 YML。
- [x] 配置项和 YML 都缺失时运行期生成临时密钥对。
- [x] 运行期临时生成密钥时打印公钥。
- [x] 配置项密钥对不匹配时记录日志并回退。
- [x] 解密失败时记录异常日志并返回登录失败。

验证要点：

- 前端加密后的密码后端可解密。
- 配置项密钥对正确时优先使用配置项。
- 配置项缺失时使用 YML。
- 私钥不会进入公开配置响应。

### 任务 5：实现登录认证和 Session

**目标：** 使用组织人员作为账号来源，登录成功后写入 Redis Session。

- [x] 在 `OrgPersonRepository` 中补充按用户名查询能力。
- [x] 使用 `heph_person.username/password/enabled` 校验登录。
- [x] 登录成功后创建 `LoginSessionUser`。
- [x] 将 `LoginSessionUser` 写入 `HttpSession`。
- [x] 使用 `login.session.timeout.minutes` 设置 Session 超时。
- [x] 实现 `POST /auth/login`。
- [x] 实现 `GET /auth/me`。
- [x] 实现 `POST /auth/logout`。
- [x] 使用 `LoginExceptionHandler` 统一处理登录异常。

验证要点：

- 正确账号密码登录成功。
- 用户不存在登录失败。
- 密码错误登录失败。
- 禁用人员登录失败。
- 登录后 Redis 中存在 Session。
- 退出后 Session 失效。

### 任务 6：实现登录拦截和页面跳转

**目标：** 未登录时保护主页面和业务 API。

- [x] 实现 `LoginRequiredInterceptor`。
- [x] 实现 `LoginWebMvcConfiguration`。
- [x] 放行登录页和登录静态资源。
- [x] 放行 `/auth/login`、`/auth/logout`。
- [x] 放行 `/api/system-config/public/**`。
- [x] 放行登录页背景图访问 `/api/media/files/*`。
- [x] 未登录访问页面时跳转 `/login.html`。
- [x] 未登录访问 API 时返回 401。
- [x] 根路径转发到聊天页或登录页。

验证要点：

- 未登录访问 `/hephaestus/` 进入登录页。
- 未登录访问 `/hephaestus/chat.html` 跳到登录页。
- 未登录访问受保护 API 返回 401。
- 登录后可访问聊天页和设置页接口。

### 任务 7：实现登录页

**目标：** 提供配置化登录页面。

- [x] 新增 `login.html`。
- [x] 新增 `login.css`。
- [x] 新增 `login.js`。
- [x] 登录页启动时读取 `/api/system-config/public/main-system`。
- [x] 显示配置化标题和副标题。
- [x] 使用 Web Crypto 加密密码。
- [x] 登录成功后跳转主聊天页。
- [x] 登录失败时显示错误提示。
- [x] 背景图默认读取 `media_id = 1`。
- [x] 背景图和网格可叠加。
- [x] 登录框使用接近背景的半透明颜色。
- [x] 登录框四条边线具有流动效果。
- [x] 鼠标拖尾效果由 `login.mouse.trail.effect` 控制。

验证命令：

```powershell
rtk proxy node --check modules/hephaestus-login/src/main/resources/static/login.js
```

### 任务 8：设置页集成主系统配置

**目标：** 在设置界面维护登录和主系统配置。

- [x] 在“设置 -> 常规”中增加“主系统配置”。
- [x] 调用 `GET /api/system-config/forms/main-system` 获取 schema。
- [x] 根据 `componentType` 动态渲染字段。
- [x] 支持文本、数字、多行文本、开关、选择等控件。
- [x] 保存时调用 `PUT /api/system-config/forms/main-system`。
- [x] 保存成功后显示提示。
- [x] 刷新按钮重新拉取表单。
- [x] 保存和刷新按钮放在“系统配置”标题右侧同一行。
- [x] 删除不需要的重置按钮。
- [x] 优化开关和表单布局，避免一行显示错乱。

验证命令：

```powershell
rtk proxy node --check modules/hephaestus-org/src/main/resources/static/org-settings.js
```

### 任务 9：主界面用户态集成

**目标：** 主聊天界面识别当前登录用户，并提供用户菜单。

- [x] 主界面加载时调用 `/auth/me`。
- [x] 左下角展示登录人头像和姓名。
- [x] 点击用户卡片后显示个人信息、设置、帮助、退出登录菜单。
- [x] 退出登录调用 `/auth/logout`。
- [x] 未登录时跳转登录页。
- [x] 主界面共享网格、跑线、拖尾视觉效果，但透明度独立调整。

验证命令：

```powershell
rtk proxy node --check modules/hephaestus-app/src/main/resources/static/chat.js
```

### 任务 10：静态资源版本与缓存处理

**目标：** 避免浏览器缓存导致页面仍加载旧资源。

- [x] 登录页资源带 `?v=` 版本号。
- [x] 主页面引用设置、个人信息等静态资源时同步更新 `?v=`。
- [x] 修改静态资源后检查 `chat.html` 版本号。
- [x] 配置保存后清理 Redis 配置缓存。
- [x] 应用启动后清理 Redis 配置缓存。

验证要点：

- 浏览器刷新后能加载最新 JS/CSS。
- 修改配置后公开配置接口返回新值。
- 重启服务后不会继续读取 Redis 旧配置。

## 4. 验证清单

### 4.1 静态脚本语法

```powershell
rtk proxy node --check modules/hephaestus-login/src/main/resources/static/login.js
rtk proxy node --check modules/hephaestus-org/src/main/resources/static/org-settings.js
rtk proxy node --check modules/hephaestus-app/src/main/resources/static/chat.js
```

### 4.2 编译验证

```powershell
rtk proxy powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\mvn-java21.ps1 -pl modules/hephaestus-login -am -DskipTests compile
rtk proxy powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\mvn-java21.ps1 -pl modules/hephaestus-app -am -DskipTests compile
```

### 4.3 接口手测

- [ ] 未登录访问 `/hephaestus/`，应进入登录页。
- [ ] `GET /hephaestus/api/system-config/public/main-system` 返回公开配置。
- [ ] 公开配置不包含 `login.password.encrypt.private-key`。
- [ ] 登录页能展示背景图、网格和鼠标拖尾。
- [ ] 正确账号密码登录成功。
- [ ] 登录后进入聊天页。
- [ ] Redis 中可查看到 Session。
- [ ] 左下角显示当前登录人头像和姓名。
- [ ] 退出登录后再次访问主界面回到登录页。
- [ ] “设置 -> 常规 -> 主系统配置”能加载并保存配置。
- [ ] 修改登录页背景或拖尾配置后，刷新登录页生效。
- [ ] 重启服务后配置重新从表/YML 获取，不继续依赖旧 Redis 缓存。

## 5. 回归范围

- 登录页：配置读取、密码加密、登录失败提示、背景图、网格、拖尾。
- 主界面：未登录跳转、当前用户展示、退出登录。
- 设置界面：主系统配置动态表单、保存、刷新、提示。
- 组织模块：按用户名查询人员不能破坏原有人员列表和人员设置功能。
- 媒体模块：登录页背景图 `/api/media/files/{id}` 未登录可访问。
- Redis：Session 和配置缓存 key 不互相影响。

## 6. 风险与后续建议

- 当前 `heph_person.password` 仍为明文比对，后续建议迁移为密码哈希。
- 运行期临时 RSA 密钥只能作为兜底，生产应写入配置项或 YML。
- 登录配置私钥必须保持非公开、敏感，不允许进入公开配置接口。
- 如果后续引入角色权限，再评估是否接入 Spring Security。
- 如果配置项继续增多，建议按分组拆分更多配置页，避免“主系统配置”过重。
