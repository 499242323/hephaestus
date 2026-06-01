# Hephaestus 登录鉴权与登录日志 PRD

**日期**：2026-05-28
**模块**：`modules/hephaestus-login`
**主题**：登录必拦、YML 白名单、登录/登出日志
**状态**：设计按反馈调整，待确认

## 目标

在当前 Hephaestus 登录模块基础上补齐第一版鉴权与登录日志能力：

- 所有业务接口默认必须登录后才能访问。
- 未登录访问页面跳转登录页，未登录访问 API 返回 401 JSON。
- 白名单路径配置在 YML 中，不放入系统配置表，不提供前端维护入口。
- 仅在登录、登出接口保存日志，日志包含客户端信息和操作类型。
- 前端不主动调用报错日志接口，第一版不做前端错误日志上报。
- 设置界面后续可查询登录日志；第一版日志数据来源只来自后端登录/登出流程。
- 新增登录日志清理定时任务，每天执行一次，清理 30 天之外的日志；多台部署时由启动脚本指定当前节点，只有指定节点执行。

## 背景

用户要求参考 `E:\NEW_WORK\wizdom-urban-v14-framework` 的 `/home` 接口设计，增强当前项目：

- “所有接口都需要登录才能访问，可以配置白名单”
- “增加登录和登出的日志表记录”
- “目前只在登录登出接口保存日志，包含客户端的信息和操作类型”
- “前端不主动调用报错日志”
- “白名单接口应当配置到 YML”
- “新增定时任务（多台环境只有一台启用，可指定），每天一次，清理30天之外的log日志”

当前项目相关上下文：

- `modules/hephaestus-login` 已提供 `/auth/login`、`/auth/me`、`/auth/logout`、Redis Session、系统配置和登录拦截器。
- `LoginWebMvcConfiguration` 当前只拦截 `/`、`/chat.html`、`/api/**`，并使用硬编码 `excludePathPatterns`。
- `LoginRequiredInterceptor` 当前只校验 Session 中的 `SessionUtils.SESSION_USER_KEY`，未从 YML 读取白名单。
- `AuthService` 当前负责登录校验、Session 写入、同账号单处登录处理，但没有记录登录成功或失败日志。
- `AuthController` 当前退出登录直接 `session.invalidate()`，没有记录登出日志。
- 当前仓库尚无登录日志表、登录日志查询接口和日志查询前端。
- 当前仓库尚无登录日志清理定时任务，新增任务需要避免多实例重复执行。

## 代码库发现

- [LoginWebMvcConfiguration.java](E:/NEW_WORK/hephaestus/modules/hephaestus-login/src/main/java/com/example/springaidemo/login/auth/LoginWebMvcConfiguration.java) 当前使用硬编码白名单，不能满足“白名单配置到 YML”。
- [LoginRequiredInterceptor.java](E:/NEW_WORK/hephaestus/modules/hephaestus-login/src/main/java/com/example/springaidemo/login/auth/LoginRequiredInterceptor.java) 当前只判断是否登录，白名单和未登录响应逻辑需要调整。
- [AuthService.java](E:/NEW_WORK/hephaestus/modules/hephaestus-login/src/main/java/com/example/springaidemo/login/auth/AuthService.java) 当前登录成功后写 Session，并支持同账号单处登录配置，但没有记录登录成功或失败日志。
- [AuthController.java](E:/NEW_WORK/hephaestus/modules/hephaestus-login/src/main/java/com/example/springaidemo/login/auth/AuthController.java) 当前退出登录直接销毁 Session，登出日志需要在销毁前记录。
- [application.yml](E:/NEW_WORK/hephaestus/modules/hephaestus-app/src/main/resources/application.yml) 或当前启动模块配置文件适合承载鉴权白名单默认值。
- [db.changelog.xml](E:/NEW_WORK/hephaestus/modules/liquibase/src/main/resources/db/changelog/db.changelog.xml) 已包含系统表和业务表 changeSet，本次应追加登录日志表 changeSet，不改旧 changeSet。
- [org-settings-panel.html](E:/NEW_WORK/hephaestus/modules/hephaestus-org/src/main/resources/static/org-settings-panel.html) 后续可增加日志查询入口；第一版前端只查询后端已有登录日志，不主动上报错误日志。

## 设计方案

### 1. 鉴权模型

采用 Spring MVC `HandlerInterceptor`，不引入 Spring Security。

拦截策略：

- `LoginWebMvcConfiguration` 拦截 `/**`，不再只拦截 `/api/**`。
- 白名单判断放到 `LoginRequiredInterceptor` 内部执行。
- 白名单来源为 YML 配置，不从系统配置表读取。
- 命中白名单的请求直接放行。
- 未命中白名单时，检查 Session 中是否存在 `SessionUtils.SESSION_USER_KEY`。
- 未登录 API 请求返回 401 JSON。
- 未登录页面请求跳转 `/login.html`。

白名单匹配规则：

- YML 支持列表形式。
- 支持 Ant 风格路径，例如 `/auth/login`、`/api/system-config/public/**`、`/**/*.js`。
- 路径匹配使用 Spring `AntPathMatcher`。
- 如果 YML 未配置或配置为空，运行时使用代码内最小兜底白名单，避免登录页被锁死。

YML 配置建议：

```yaml
hephaestus:
  auth:
    whitelist:
      paths:
        - /login.html
        - /login.css
        - /login.js
        - /auth/login
        - /api/system-config/public/**
        - /api/media/files/*
        - /favicon.ico
        - /error
        - /**/*.css
        - /**/*.js
        - /**/*.png
        - /**/*.jpg
        - /**/*.jpeg
        - /**/*.gif
        - /**/*.svg
        - /**/*.ico
        - /**/*.woff
        - /**/*.woff2
```

定时清理 YML 配置建议：

```yaml
hephaestus:
  login-log:
    cleanup:
      enabled: true
      current-node: ${HEPHAESTUS_NODE_ID:default}
      enabled-node: default
      retention-days: 30
      cron: "0 0 3 * * ?"
```

说明：

- `enabled` 控制是否启用清理任务。
- `current-node` 表示当前实例节点标识，由启动脚本或环境变量指定，例如 `HEPHAESTUS_NODE_ID=app-01`。
- `enabled-node` 表示允许执行清理任务的节点标识。
- 只有 `enabled = true` 且 `current-node = enabled-node` 时才执行清理。
- `retention-days` 默认 30，清理早于当前时间减 30 天的日志。
- `cron` 默认每天凌晨 3 点执行一次，满足“每天一次”。
- 多台环境中，每台机器使用不同 `current-node` 启动；只把其中一台配置为 `enabled-node`。
- 本地开发默认 `current-node=default` 且 `enabled-node=default`，可以正常执行一次清理逻辑。

### 2. 登录日志范围

第一版只记录登录、登出接口产生的日志，不做业务 API 操作日志自动采集，不做前端错误日志上报。

记录事件：

- `LOGIN_SUCCESS`：登录成功。
- `LOGIN_FAILURE`：登录失败，包括参数缺失、用户不存在、用户禁用、密码错误、密码解密失败等。
- `LOGOUT`：用户主动退出登录。

不做内容：

- 不新增通用 API 操作日志拦截器。
- 不新增 `sys_operation_log` 表。
- 不记录业务接口访问摘要。
- 不记录前端主动上报的错误日志。
- 不记录请求体、密码明文、密码密文或附件内容。

### 3. 登录日志表

新增 `sys_login_log` 表。

建议字段：

- `id`：主键。
- `operation_type`：操作类型，建议值 `LOGIN_SUCCESS`、`LOGIN_FAILURE`、`LOGOUT`。
- `username`：登录账号，失败时尽量记录用户输入的账号。
- `person_id`：登录成功或登出时记录人员 ID。
- `person_name`：登录成功或登出时记录人员姓名。
- `session_id`：登录成功或登出时记录 Session ID。
- `success_flag`：是否成功。
- `message`：失败原因或操作说明。
- `client_ip`：客户端 IP，优先识别 `X-Forwarded-For`、`X-Real-IP`，否则使用 `request.getRemoteAddr()`。
- `user_agent`：浏览器 UA。
- `request_uri`：触发日志的接口路径，例如 `/auth/login`、`/auth/logout`。
- `created_at`：发生时间。

字段约束：

- `operation_type`、`success_flag`、`client_ip`、`created_at` 非空。
- 文本字段限制长度，超长时截断，避免异常 UA 或错误消息撑爆字段。
- 密码字段不落库。

### 4. 记录位置

登录接口：

- `AuthController.login` 接收 `HttpServletRequest`，将客户端信息传给服务层，或构造日志上下文。
- `AuthService.login` 登录成功后记录 `LOGIN_SUCCESS`。
- `AuthService.login` 捕获登录失败路径并记录 `LOGIN_FAILURE` 后继续抛出原业务异常。
- 密码解密失败也记录 `LOGIN_FAILURE`，但不记录密码内容。

登出接口：

- `AuthController.logout` 在 `session.invalidate()` 前读取当前登录用户和 Session ID。
- 调用日志服务记录 `LOGOUT`。
- 日志写入完成或失败后再销毁 Session。

日志写入失败处理：

- 登录日志写入失败必须 `log.error` 打印上下文。
- 日志写入失败不能影响登录、登出接口的原始业务结果。

### 5. 登录日志查询

新增登录日志查询接口，放在 `modules/hephaestus-login`：

- `GET /api/logs/login`

查询能力第一版保持轻量：

- `page`：默认 1。
- `pageSize`：默认 20，最大 100。
- `keyword`：匹配账号、姓名、IP、消息。
- `operationType`：按操作类型筛选。
- `success`：按成功/失败筛选。
- `startTime` / `endTime`：可选时间范围。

返回结构建议：

```json
{
  "items": [],
  "page": 1,
  "pageSize": 20,
  "total": 0
}
```

接口鉴权：

- 日志查询接口本身必须登录后才能访问。
- 日志查询接口不加入默认白名单。

### 6. 设置界面日志查询

第一版设置界面只查询登录日志。

页面结构：

- 设置抽屉新增“日志”入口。
- 右侧展示“登录日志”列表。
- 顶部提供轻量筛选：
  - 关键词
  - 操作类型
  - 成功/失败
  - 刷新按钮
- 下方表格展示最近登录/登出日志。
- 分页先做“上一页 / 下一页”。

前端约束：

- 前端只调用 `GET /api/logs/login` 查询日志。
- 前端不主动调用错误日志、报错日志或操作日志上报接口。
- 修改 `org-settings-panel.html`、`org-settings.js`、`org-settings.css` 后，同步更新 [chat.html](E:/NEW_WORK/hephaestus/modules/hephaestus-app/src/main/resources/static/chat.html) 中相关 `?v=` 版本号。
- `node --check` 校验变更后的 JS。

### 7. 异常与安全

- 所有日志写入异常必须 `log.error` 打印上下文，但不能阻断登录或登出接口。
- 登录失败日志不记录明文密码、不记录密文密码。
- 不新增前端错误日志接口，避免前端错误信息泛化落库。
- 白名单不通过公开配置接口返回。
- 白名单不放系统配置表，避免页面误改导致安全边界变化。
- YML 白名单缺失时，代码内最小兜底白名单仍保证 `/login.html`、`/auth/login`、`/api/system-config/public/**` 可用。

### 8. 登录日志清理定时任务

新增登录日志清理任务，负责控制日志表增长。

执行规则：

- 默认每天执行一次。
- 默认清理 30 天之外的 `sys_login_log` 数据。
- 清理阈值按 `created_at < 当前时间 - retentionDays` 判断。
- 多台部署时，每台机器都可以触发定时任务检查，但只有 `current-node` 等于 `enabled-node` 的机器实际清理。
- 当前节点不匹配指定节点时直接跳过，并打印 debug 或 info 级别日志说明。
- `current-node` 需要由启动脚本或环境变量保证每台机器不同。
- 清理失败必须 `log.error` 打印异常，不影响应用运行。

实现建议：

- 使用 Spring `@Scheduled` 实现。
- 使用 `@EnableScheduling` 开启调度能力。
- 定时任务开关、当前节点、执行节点、保留天数和 cron 均来自 YML 或启动脚本传入的环境变量。
- 新增 `LoginLogCleanupNodeMatcher` 或等价小组件，负责判断 `current-node` 是否命中 `enabled-node`。
- Repository 提供 `deleteBefore(LocalDateTime cutoffTime)`。
- Service 返回清理条数，方便日志打印和后续排查。

## TDD 计划

### 测试位置

- `modules/hephaestus-login/src/test/java/...`
- 必要时补充 `modules/hephaestus-app` 集成层测试。
- 前端静态 JS 使用 `node --check` 做语法验证。

### 首个失败测试

第一条失败测试从“默认接口必须登录，YML 白名单可放行”开始：

- 未登录访问 `/api/chat/send` 应返回 401。
- 未登录访问 `/auth/login` 应放行。
- 未登录访问 `/api/system-config/public/main-system` 应放行。
- 将某路径加入 YML 白名单配置后，该路径应放行。

这是本需求最核心的安全边界，优先用测试锁住。

### 关键测试场景

- 未登录访问 API 返回 401 JSON。
- 未登录访问页面跳转登录页。
- 已登录访问 API 正常放行。
- YML 白名单支持列表配置。
- YML 白名单支持 Ant 风格通配符。
- YML 白名单缺失时使用最小兜底白名单，登录页不被锁死。
- 登录成功写入 `LOGIN_SUCCESS` 日志。
- 登录失败写入 `LOGIN_FAILURE` 日志，且不记录密码。
- 密码解密失败写入 `LOGIN_FAILURE` 日志。
- 登出写入 `LOGOUT` 日志。
- 登录日志包含客户端 IP、User-Agent、请求路径、操作类型。
- 日志写入失败时登录/登出接口不失败，且服务端打印错误日志。
- 登录日志查询支持分页、关键词、操作类型和成功/失败筛选。
- 登录日志清理任务在多实例环境下只有 `current-node = enabled-node` 的机器执行。
- 登录日志清理任务当前节点不匹配时跳过。
- 登录日志清理任务启动脚本可指定不同 `HEPHAESTUS_NODE_ID`。
- 登录日志清理任务清理 30 天之外的数据，不删除 30 天内的数据。
- 登录日志清理任务关闭时不执行清理。
- 设置界面日志列表能查询并渲染空数据、正常数据和失败提示。
- 前端不存在主动上报错误日志调用。

### 最小实现范围

- 新增 YML 白名单配置属性类。
- 改造登录拦截器为“拦截全部 + YML 白名单 + 兜底白名单”。
- Liquibase 新增 `sys_login_log` 表。
- 新增登录日志实体、Repository、Service、Controller。
- 登录成功、失败、登出写登录日志。
- 新增登录日志清理 YML 配置属性和定时任务。
- 新增当前节点与执行节点匹配逻辑。
- 登录日志 Repository 增加按时间清理方法。
- 设置抽屉新增登录日志查询界面。

### 验证命令

- `rtk proxy node --check modules/hephaestus-org/src/main/resources/static/org-settings.js`
- `rtk proxy git diff --check`
- 如果本机 Java 17 可用，执行 `rtk .\tools\mvn-java21.ps1 test`
- 如果本机仍是 Java 8，需要明确记录“无法本地完成 Maven 验证，项目要求 Java 17”

## 风险

- 全局拦截从部分路径扩大到 `/**`，最容易误伤登录页、公开配置、静态资源和媒体背景图，因此必须保留代码兜底白名单。
- 白名单放在 YML 后，运行期不能通过设置界面动态调整；这是当前安全边界选择，变更需要改配置并重启。
- 登录日志写库如果同步失败，不能影响登录登出结果，因此需要完整的异常捕获和 `log.error`。
- 当前没有权限体系，日志查询接口第一版只要求登录，不做角色权限判断；后续如果增加管理员权限，应再收敛日志查询访问范围。
- 多实例部署时如果多台机器使用相同 `current-node` 且命中 `enabled-node`，会导致多台执行清理；启动脚本需要保证节点标识唯一。
- 如果启动脚本未指定节点，所有机器都会使用默认值 `default`；生产环境必须显式指定 `HEPHAESTUS_NODE_ID` 并设置唯一执行节点。
- 登录日志清理任务会删除历史数据，保留天数应通过 YML 明确配置，默认 30 天。

## 验证方式

设计确认后进入 plan 阶段，实施完成后至少验证：

- 未登录无法访问聊天、组织、媒体上传、系统配置保存等业务接口。
- 登录页、登录接口、公开配置接口和必要静态资源在未登录状态可用。
- 白名单来自 YML，不来自系统配置表。
- 登录成功、登录失败、登出均有数据库日志。
- 登录日志包含客户端 IP、User-Agent、请求路径、操作类型。
- 登录日志清理任务每天一次，多实例环境只有启动脚本指定并匹配的节点执行。
- 生产环境每台机器应配置不同 `HEPHAESTUS_NODE_ID`。
- 30 天之外的登录日志会被清理，30 天内日志保留。
- 前端没有主动调用报错日志接口。
- 日志查询接口必须登录后才能访问。
- 设置界面新增登录日志查询入口。
- `node --check` 通过。
- `git diff --check` 通过。
