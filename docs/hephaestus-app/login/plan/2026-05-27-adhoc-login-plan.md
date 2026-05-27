# Hephaestus 登录模块实施计划

> **给 agentic worker 的要求：** 实施本计划时使用 `superpowers:executing-plans`。步骤使用 checkbox (`- [ ]`) 跟踪。

**目标：** 新增 `modules/hephaestus-login`，实现动态系统配置、公开配置读取、配置项 Redis 缓存、Redis 登录会话、密码加密传输和设置页配置维护。

**架构：** 新增独立登录模块承载认证、系统配置、公开配置读取、登录页静态资源。主应用 `hephaestus-app` 依赖登录模块，组织模块继续提供人员数据，Liquibase 模块负责表结构和初始化数据。

**技术栈：** Java 17、Spring Boot 3.5、Spring MVC、Spring Session Redis、Spring Data Redis、MyBatis-Plus Mapper、Liquibase XML、原生 HTML/CSS/JS。

---

### 任务 1：新增模块与数据库结构

**文件：**
- 修改：`pom.xml`
- 新建：`modules/hephaestus-login/pom.xml`
- 修改：`modules/hephaestus-app/pom.xml`
- 修改：`modules/liquibase/src/main/resources/db/changelog/db.changelog.xml`

- [x] 在父工程中加入 `modules/hephaestus-login`。
- [x] 创建登录模块 POM，并加入 web、session redis、mybatis、org、test 依赖。
- [x] 在 `hephaestus-app` 中依赖 `hephaestus-login`。
- [x] 新增 `sys_config_definition` 和 `sys_config_value` 两张表。
- [x] 初始化 `main-system` 分组下的登录配置定义和默认值。

### 任务 2：配置中心领域模型、接口与 Redis 缓存

**文件：**
- 新建：`modules/hephaestus-login/src/main/java/com/example/springaidemo/login/config/domain/SystemConfigDefinitionEntity.java`
- 新建：`modules/hephaestus-login/src/main/java/com/example/springaidemo/login/config/domain/SystemConfigValueEntity.java`
- 新建：`modules/hephaestus-login/src/main/java/com/example/springaidemo/login/config/repository/SystemConfigDefinitionRepository.java`
- 新建：`modules/hephaestus-login/src/main/java/com/example/springaidemo/login/config/repository/SystemConfigValueRepository.java`
- 新建：`modules/hephaestus-login/src/main/java/com/example/springaidemo/login/config/service/SystemConfigCacheService.java`
- 新建：`modules/hephaestus-login/src/main/java/com/example/springaidemo/login/config/service/SystemConfigService.java`
- 新建：`modules/hephaestus-login/src/main/java/com/example/springaidemo/login/config/controller/SystemConfigController.java`

- [x] 实现配置定义和值实体。
- [x] 实现配置定义查询、配置值查询和配置值 upsert。
- [x] 实现 `GET /api/system-config/forms/{groupCode}`，返回动态表单 schema 和当前值。
- [x] 实现 `PUT /api/system-config/forms/{groupCode}`，校验配置编码并保存。
- [x] 实现 `GET /api/system-config/public/{groupCode}`，只返回公开且非敏感配置。
- [x] 参考 framework 配置缓存思路，将单项配置值和公开配置结果放入 Redis 缓存。
- [x] 保存配置后主动清理该分组相关 Redis 缓存。

### 任务 3：登录服务、加密与拦截器

**文件：**
- 修改：`modules/hephaestus-org/src/main/java/com/example/springaidemo/org/repository/OrgPersonRepository.java`
- 新建：`modules/hephaestus-login/src/main/java/com/example/springaidemo/login/auth/LoginSessionUser.java`
- 新建：`modules/hephaestus-login/src/main/java/com/example/springaidemo/login/auth/LoginRequest.java`
- 新建：`modules/hephaestus-login/src/main/java/com/example/springaidemo/login/auth/LoginResponse.java`
- 新建：`modules/hephaestus-login/src/main/java/com/example/springaidemo/login/auth/RsaPasswordCryptoService.java`
- 新建：`modules/hephaestus-login/src/main/java/com/example/springaidemo/login/auth/AuthService.java`
- 新建：`modules/hephaestus-login/src/main/java/com/example/springaidemo/login/auth/AuthController.java`
- 新建：`modules/hephaestus-login/src/main/java/com/example/springaidemo/login/auth/LoginRequiredInterceptor.java`
- 新建：`modules/hephaestus-login/src/main/java/com/example/springaidemo/login/auth/LoginWebMvcConfiguration.java`

- [x] 在 `OrgPersonRepository` 中补充按用户名查询方法。
- [x] 实现 RSA 私钥解密，配置未填写密钥时使用运行期兜底密钥。
- [x] 使用 `heph_person.username/password/enabled` 校验登录。
- [x] 登录成功后将 `LoginSessionUser` 写入 `HttpSession`。
- [x] 使用 `login.session.timeout.minutes` 设置当前 Session 超时时间。
- [x] 实现 `POST /auth/login`、`GET /auth/me`、`POST /auth/logout`。
- [x] 实现未登录页面跳转和 API 401 返回。

### 任务 4：登录页与设置页前端

**文件：**
- 新建：`modules/hephaestus-login/src/main/resources/static/login.html`
- 新建：`modules/hephaestus-login/src/main/resources/static/login.css`
- 新建：`modules/hephaestus-login/src/main/resources/static/login.js`
- 修改：`modules/hephaestus-app/src/main/resources/static/chat.html`
- 修改：`modules/hephaestus-app/src/main/resources/static/chat.js`
- 修改：`modules/hephaestus-org/src/main/resources/static/org-settings-panel.html`
- 修改：`modules/hephaestus-org/src/main/resources/static/org-settings.js`
- 修改：`modules/hephaestus-org/src/main/resources/static/org-settings.css`

- [x] 登录页读取公开配置并展示标题、副标题、公钥等公开参数。
- [x] 登录页使用浏览器原生 Web Crypto 对密码做公钥加密。
- [x] 登录页支持乐符拖尾、飘动线条、绚丽流星、大波浪、鼠标涂鸦、连线点阵、萤火之光、多彩烟花、放大镜，并通过 `login.mouse.trail.effect` 配置项控制当前效果。
- [x] 聊天页侧边栏增加退出登录入口。
- [x] 设置页“常规”中新增“主系统配置”动态表单。
- [x] 动态渲染 `text`、`number`、`textarea`、`switch`、`select` 和敏感字段。
- [x] 实现配置保存、重置、刷新。
- [x] 更新 `chat.html` 中静态资源版本号。

### 任务 5：验证

**文件：**
- 使用上述变更文件。

- [x] 执行 `./tools/mvn-java21.ps1 -pl modules/hephaestus-login -am -DskipTests compile`。
- [x] 执行 `./tools/mvn-java21.ps1 -pl modules/hephaestus-app -am -DskipTests compile`。
- [x] 执行 `node --check modules/hephaestus-login/src/main/resources/static/login.js`。
- [x] 执行 `node --check modules/hephaestus-org/src/main/resources/static/org-settings.js`。
- [x] 执行 `node --check modules/hephaestus-app/src/main/resources/static/chat.js`。
- [ ] 如果服务正在运行，使用浏览器验证登录、公开配置、配置保存、退出和未登录跳转。
