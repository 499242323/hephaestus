# 邮箱注册与验证码重置密码 PRD

## Goal

在 Hephaestus 中新增邮箱注册和邮箱验证码重置密码能力。注册入口使用独立前端页面实现，视觉参考 `Silvana-kite/html-css-js-project` 登录表单风格和用户提供的视频截图，但认证、加密、人员落库、配置读取仍遵循当前项目结构。

功能完成后，未登录用户可以通过邮箱验证码注册账号；同一邮箱最多注册三个账号；注册生成的人员统一归属到系统配置指定的部门；忘记密码时必须通过邮箱验证码后才能重置密码。

## Context

当前登录能力位于 `modules/hephaestus-login`：

- `AuthController` 提供 `/auth/login`、`/auth/me`、`/auth/logout`。
- `AuthServiceImpl` 使用 `OrgPersonRepository` 按 `username` 查询 `heph_person`，并校验 `password`。
- 登录密码传输已存在前端 RSA 加密和后端解密逻辑，配置项包括 `login.password.encrypt.enabled`、`login.password.encrypt.algorithm`、`login.password.encrypt.public-key`、`login.password.encrypt.private-key`。
- 登录页静态资源位于 `modules/hephaestus-login/src/main/resources/static/login.html`、`login.css`、`login.js`。
- 登录拦截白名单由 `LoginRequiredInterceptor` 和 `hephaestus.auth.whitelist.paths` 控制。

当前人员能力位于 `modules/hephaestus-org`：

- 人员主表为 `heph_person`。
- `OrgPersonEntity` 已包含 `person_code`、`person_name`、`username`、`password`、`unit_id`、`email`、`enabled` 等字段。
- `OrgPersonService.createPerson` 用于管理员新增人员，当前没有人员来源字段。
- `OrgPersonRepository` 已提供按 `username` 查询人员的能力，尚缺按邮箱统计、按邮箱用户名查询、更新密码等注册场景所需方法。

当前数据库变更位于 `modules/liquibase/src/main/resources/db/changelog/db.changelog.xml`。项目约束要求不能修改历史 changeset，只能在原有基础上追加新的 changeset。

当前项目已有 `spring-boot-starter-mail` 和 `JavaMailSender` 使用示例，但没有专门的注册验证码模块。

## Repository Findings

1. 登录账号当前直接复用人员表，不存在独立账号表。邮箱注册应继续写入 `heph_person`，避免新增一套账号模型。
2. `hephaestus-login` 已依赖 `hephaestus-org`，因此登录模块可以调用组织模块仓储或稳定服务；反向依赖不应新增。
3. 系统配置表已经支持动态配置定义和值，适合新增 `login.register.unit-id`、验证码有效期、验证码发送间隔等配置项。
4. 静态资源是原生 HTML/CSS/JS，不应引入前端框架。
5. 登录页当前使用公共配置接口读取登录页文案和背景图，注册页应复用公共配置接口和密码加密逻辑，保持前后端密码加密一套逻辑。
6. 人员管理页面需要展示人员来源时，应由 `OrgPersonSummary` 和列表 SQL 补充字段；管理员新增人员默认来源为 `ADMIN`，注册生成来源为 `REGISTER`。

## Design

### 功能入口

新增独立前端文件：

- `login.html`
- `login.css`
- `login.js`

登录页保留原有登录能力，同时新增跳转注册页和忘记密码入口。注册页提供两个视图：

- 邮箱注册
- 邮箱验证码重置密码

注册页整体视觉参考用户给出的暗色森林、半透明表单、右上角登录/注册切换按钮效果，但实现应使用当前项目自有静态文件，不直接引入外部工程代码。

### 后端接口

在 `modules/hephaestus-login` 新增邮箱注册领域，推荐包结构：

- `olympus.hephaestus.login.register.controller`
- `olympus.hephaestus.login.register.service`
- `olympus.hephaestus.login.register.service.impl`
- `olympus.hephaestus.login.register.dto`
- `olympus.hephaestus.login.register.domain`
- `olympus.hephaestus.login.register.repository`
- `olympus.hephaestus.login.register.support`

接口建议：

- `POST /auth/email-code/register`：发送注册验证码。
- `POST /auth/email-code/reset-password`：发送重置密码验证码。
- `POST /auth/register`：邮箱验证码注册。
- `POST /auth/reset-password`：邮箱验证码重置密码。

接口均允许未登录访问，但要有参数校验、频率控制和清晰错误提示。

### 验证码规则

新增验证码表 `sys_email_verification_code`，字段建议：

- `id`
- `email`
- `scene`：`REGISTER`、`RESET_PASSWORD`
- `code_hash`
- `expire_at`
- `used_at`
- `send_ip`
- `created_at`

验证码只保存哈希，不保存明文。默认 6 位数字，默认 10 分钟有效，同一邮箱同一场景 60 秒内不可重复发送。发送成功后，用户必须提交邮箱、验证码和业务表单才能完成注册或重置密码。验证码使用后写入 `used_at`，避免重复使用。

### 注册规则

注册请求字段：

- 邮箱
- 用户名
- 姓名
- 密码
- 确认密码
- 验证码

后端校验：

- 邮箱格式合法。
- 用户名不能为空，且不能与已有 `heph_person.username` 重复。
- 密码不能为空，前端传输加密逻辑与登录保持一致。
- 同一邮箱在 `heph_person` 中最多允许三个启用或历史账号，达到三个时拒绝注册。
- 验证码场景必须为 `REGISTER`，未过期、未使用、哈希匹配。
- `login.register.unit-id` 必须配置且指向存在的部门。

注册成功后：

- 新增 `heph_person` 记录。
- `email` 写注册邮箱。
- `unit_id` 写 `login.register.unit-id`。
- `enabled` 默认为 true。
- `source_type` 写 `REGISTER`。
- `person_code` 可按规则生成，例如 `REG` + 时间戳或序列，要求唯一。
- 注册不自动登录，返回成功提示，引导用户去登录页登录。

### 重置密码规则

重置密码请求字段：

- 邮箱
- 用户名
- 新密码
- 确认密码
- 验证码

后端校验：

- 邮箱、用户名、新密码、验证码必填。
- 按邮箱和用户名定位唯一人员。
- 验证码场景必须为 `RESET_PASSWORD`，未过期、未使用、哈希匹配。
- 新密码按当前登录密码加密配置解密后保存。

重置成功后：

- 更新对应 `heph_person.password`。
- 标记验证码已使用。
- 不改变人员来源、部门、岗位和权限。

### 人员来源字段

在 `heph_person` 新增 `source_type varchar(32)`：

- 默认值：`ADMIN`
- 管理员新增人员：`ADMIN`
- 邮箱注册生成：`REGISTER`

后端实体、人员摘要、列表查询、创建逻辑同步补充该字段。前端人员管理可显示为“管理员新增”或“邮箱注册”，但本次实现不强制新增筛选项。

### 系统配置

新增系统配置项：

- `login.register.unit-id`：注册人员默认部门 ID，必填，非公开。
- `login.email-code.expire-minutes`：验证码有效分钟数，默认 10。
- `login.email-code.resend-seconds`：验证码重发间隔秒数，默认 60。

注册页需要读取公开登录页配置以复用背景和加密公钥，但注册归属部门 ID 不对前端公开。

### 安全与错误处理

- 注册、发送验证码、重置密码接口全部走统一 JSON 错误响应。
- 验证码对比使用哈希值，避免数据库明文保存验证码。
- 邮箱发送失败时不创建验证码可用状态。
- 接口错误提示要中文、明确，但不要泄露过多账号枚举信息；重置密码场景可提示“邮箱、用户名或验证码不正确”。
- 密码加密沿用现有 RSA 配置，不新增第二套加密逻辑。

### 白名单与静态资源

登录白名单新增：

- `/login.html`
- `/login.css`
- `/login.js`
- `/auth/email-code/register`
- `/auth/email-code/reset-password`
- `/auth/register`
- `/auth/reset-password`

如 `application.yml` 中也维护白名单，应同步补充，避免运行环境使用配置覆盖 fallback 白名单。

## TDD Plan

### 测试位置

后端测试建议新增：

- `modules/hephaestus-login/src/test/java/olympus/hephaestus/login/register/service/EmailRegisterServiceTest.java`
- `modules/hephaestus-login/src/test/java/olympus/hephaestus/login/register/controller/EmailRegisterControllerTest.java`

如当前测试环境缺少数据库依赖，优先使用 Mockito 单元测试覆盖服务规则；仓储 SQL 通过编译和必要集成验证收口。

前端验证：

- `node --check modules/hephaestus-login/src/main/resources/static/login.js`
- `node --check modules/hephaestus-login/src/main/resources/static/login.js`
- 使用 browser-qa 打开 `http://localhost:11018/hephaestus/login.html` 验证页面、注册表单、验证码按钮和返回登录入口。

### 第一个 failing test

先写服务层测试：

当同一邮箱已有 3 个 `heph_person` 账号时，调用注册方法应抛出业务异常，且不保存新人员、不消费验证码。

### 关键测试场景

1. 发送注册验证码成功，生成验证码哈希、过期时间和场景。
2. 60 秒内重复发送同邮箱同场景验证码失败。
3. 注册验证码错误失败。
4. 注册验证码过期失败。
5. 注册验证码已使用失败。
6. 同一邮箱第 4 个账号注册失败。
7. 用户名重复注册失败。
8. 未配置注册部门 ID 时注册失败。
9. 注册成功后新增人员来源为 `REGISTER`，部门为配置部门。
10. 管理员新增人员默认来源为 `ADMIN`。
11. 重置密码验证码正确时更新密码并标记验证码已使用。
12. 重置密码成功后不改变人员部门、岗位和来源。
13. 注册页必填、邮箱格式、密码确认、验证码倒计时前端逻辑通过语法检查和浏览器验证。

### 最小实现范围

第一阶段只实现邮箱注册和邮箱验证码重置密码，不实现短信验证码、第三方登录、邮箱激活状态、复杂密码强度策略、注册后自动分配岗位。

## Risks

1. 当前密码以明文形式存储在 `heph_person.password`，本次只沿用现状，不引入密码哈希，否则会扩大登录兼容改造范围。
2. 邮件发送依赖外部 SMTP 配置，开发环境可能发送失败。实现需要清晰返回邮件发送失败原因，并允许通过测试替身验证业务逻辑。
3. 注册页视觉参考外部示例，但不能直接复制大量外部代码；应保持项目自有实现和中文文案。
4. `login.register.unit-id` 配置错误会导致注册失败，设置页面需要能维护该配置。
5. 静态资源缓存可能导致页面验证看不到新效果，修改登录页资源时需要更新版本号。

## Verification

实现完成后至少执行：

1. Java 编译：

```bash
rtk proxy cmd /c "set JAVA_HOME=D:\Program Files\JetBrains\IntelliJ IDEA 2025.2.1\jbr&& set PATH=D:\Program Files\JetBrains\IntelliJ IDEA 2025.2.1\jbr\bin;%PATH%&& mvn -pl modules/hephaestus-login,modules/hephaestus-org,modules/hephaestus-app -am -DskipTests compile"
```

2. 后端测试：

```bash
rtk proxy cmd /c "set JAVA_HOME=D:\Program Files\JetBrains\IntelliJ IDEA 2025.2.1\jbr&& set PATH=D:\Program Files\JetBrains\IntelliJ IDEA 2025.2.1\jbr\bin;%PATH%&& mvn -pl modules/hephaestus-login -am test"
```

3. 前端语法检查：

```bash
rtk proxy node --check modules/hephaestus-login/src/main/resources/static/login.js
rtk proxy node --check modules/hephaestus-login/src/main/resources/static/login.js
```

4. Liquibase 检查：

- 确认只追加 changeset。
- 确认 `heph_person.source_type` 默认值和历史数据回填正确。
- 确认 `sys_email_verification_code` 表、索引和配置项 changeset 可重复执行。

5. 浏览器验证：

- 打开注册页，检查视觉和表单布局。
- 发送验证码。
- 使用验证码注册账号。
- 尝试同邮箱第 4 个账号注册，确认失败提示。
- 使用邮箱验证码重置密码，确认新密码可登录。



