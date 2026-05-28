# Hephaestus 登录模块 PRD

**日期**：2026-05-27
**整理日期**：2026-05-28
**模块**：`modules/hephaestus-login`
**主题**：登录认证、Redis Session、系统配置、公开配置、登录页视觉配置
**状态**：已进入实现与联调阶段，本文档按当前流程重新整理需求边界。

## 1. 背景

Hephaestus 原有能力集中在聊天、多模态、媒体、组织设置等模块，但缺少统一登录入口、登录态保护和可由页面维护的系统配置能力。

本次登录模块建设需要满足以下诉求：

- 打开 Hephaestus 时，未登录用户进入登录页，已登录用户进入主聊天页。
- 登录态保存到 Redis，避免只依赖本地内存 Session。
- 登录超时时间可配置，默认半小时。
- 登录密码前后端加密传输，不依赖 HTTPS。
- 新增通用系统配置能力，参考 `tc_sys_config_item` 和 framework 的 `BaseController#getSysConfigHandler` 语义。
- 配置项放入“设置 -> 常规 -> 主系统配置”中，可查询、修改、保存。
- 前端可读取允许公开的配置项，但不能暴露私钥等敏感信息。
- 登录页支持配置化标题、副标题、背景图、网格背景和鼠标拖尾效果。

## 2. 目标

### 2.1 业务目标

- 提供可独立演进的登录与系统配置模块。
- 通过登录页承接系统入口，保护主页面和业务 API。
- 通过系统配置动态表单管理登录页、会话、安全和视觉相关配置。
- 让登录页和主界面能读取公开配置，形成可配置的前端体验。

### 2.2 技术目标

- 新增独立 Maven 模块 `modules/hephaestus-login`。
- 使用 Spring Session Redis 保存登录态。
- 复用 `heph_person.username/password/enabled` 作为第一版账号来源。
- 使用 RSA 公钥加密、私钥解密完成应用层密码传输保护。
- 系统配置采用“定义表 + 值表”模型，并将配置读取结果放入 Redis 缓存。
- 配置项常量集中在 `LoginConfigConst`，便于维护和避免散落字符串。

## 3. 范围

### 3.1 本期范围

- 登录页静态资源：`login.html`、`login.css`、`login.js`。
- 登录接口：登录、退出、当前用户。
- 登录拦截器：保护主页面和业务 API。
- Redis Session：登录成功写入 Session，超时时间从配置项读取。
- 系统配置：定义表、值表、动态表单接口、公开配置接口、Redis 缓存。
- 设置页集成：常规页展示“主系统配置”动态表单。
- 密码加密传输：前端 Web Crypto 加密，后端 RSA 解密。
- 配置兜底：优先使用配置项，配置项缺失时使用 YML；YML 也缺失时允许运行期生成临时密钥并打印公钥。
- 登录页视觉：背景图默认读取 `media_id = 1`，网格背景可开关，鼠标拖尾效果可配置。

### 3.2 非本期范围

- 不引入 Spring Security。
- 不新增独立账号表。
- 不实现角色、菜单、按钮权限。
- 不实现验证码、短信登录、扫码登录。
- 不做密码散列迁移，当前仍按现有 `heph_person.password` 比对。
- 不做配置历史版本、配置审批、配置审计。
- 不保证配置变更立即刷新所有历史 Session，超时配置优先对新登录会话生效。

## 4. 用户与场景

### 4.1 普通登录用户

- 访问系统入口时看到登录页。
- 输入账号密码后进入聊天主界面。
- 点击左下角用户菜单可退出登录。
- Session 超时后再次访问受保护页面会回到登录页。

### 4.2 系统配置维护人员

- 进入“设置 -> 常规 -> 主系统配置”。
- 查看登录页标题、副标题、背景图、鼠标拖尾、会话超时、密码加密等配置。
- 修改配置并保存。
- 保存后页面提示成功，并在后续读取中使用新配置。

### 4.3 前端页面

- 登录页读取公开配置，初始化标题、副标题、公钥、背景、网格和拖尾。
- 主界面可读取当前登录人，展示头像和姓名。
- 公开配置接口只返回允许前端读取的配置。

## 5. 功能需求

### 5.1 登录认证

- 提供 `POST /auth/login`。
- 请求包含 `username`、`password`、`encrypted`。
- 用户名按 `heph_person.username` 精确匹配。
- 仅允许 `enabled = true` 的人员登录。
- 密码解密后与 `heph_person.password` 比对。
- 登录成功后写入 `LoginSessionUser` 到 `HttpSession`。
- 登录失败返回明确错误，不返回敏感细节。

### 5.2 当前用户与退出

- 提供 `GET /auth/me` 返回当前登录人。
- 提供 `POST /auth/logout` 销毁当前 Session。
- 主页面左下角展示当前登录人头像和姓名。
- 未登录调用当前用户接口时按未登录处理。

### 5.3 登录拦截

- 采用 Spring MVC 拦截器实现，不引入 Spring Security。
- 放行登录页、登录静态资源、登录接口、退出接口、公开配置接口和登录页背景图访问。
- 页面请求未登录时跳转登录页。
- API 请求未登录时返回 401 JSON。
- 已登录访问根路径时进入聊天页。

### 5.4 Redis Session

- 引入 `spring-session-data-redis`。
- 登录成功后 Session 存入 Redis。
- 会话超时时间读取 `login.session.timeout.minutes`。
- 默认超时时间为 30 分钟。
- 配置变更后，新登录会话使用新超时时间。

### 5.5 密码加密传输

- 前端通过公开配置接口读取公钥。
- 前端使用 Web Crypto RSA 加密密码。
- 后端使用私钥解密密码。
- 密钥读取优先级为：配置项 -> YML -> 运行期临时生成。
- 运行期临时生成密钥时需要打印公钥，便于本地联调定位。
- 私钥不能通过公开配置接口返回。
- 加密开关由 `login.password.encrypt.enabled` 控制。

### 5.6 系统配置

系统配置采用两张表：

- `sys_config_definition`：配置定义、分组、分区、控件类型、默认值、公开标识、敏感标识、排序等。
- `sys_config_value`：配置当前值、更新人、更新时间等。

读取规则：

- 优先读取值表。
- 值表没有时使用定义表默认值。
- 需要运行时使用的登录配置可通过 `SystemConfigService#getValue/getBoolean/getInt` 获取。
- 配置读取结果写入 Redis 缓存。
- 保存配置后清理相关单项缓存和公开配置缓存。
- 服务重启后清理 Redis 配置缓存，避免旧缓存覆盖新定义或新默认值。

### 5.7 系统配置接口

- `GET /api/system-config/forms/{groupCode}`：返回动态表单 schema 和当前值。
- `PUT /api/system-config/forms/{groupCode}`：保存配置值，保存后返回最新表单。
- `GET /api/system-config/public/{groupCode}`：只返回公开且非敏感配置。

公开配置接口约束：

- 只返回 `public_flag = true` 的配置。
- 不返回 `sensitive_flag = true` 且不允许公开的配置。
- 不返回私钥。
- 公钥缺失时可由后端运行期密钥对补充返回。

### 5.8 设置页主系统配置

- 在“设置 -> 常规”中增加“主系统配置”标签页。
- 前端根据后端 schema 动态渲染控件。
- 支持 `text`、`number`、`textarea`、`switch`、`select` 等控件类型。
- 敏感字段使用更谨慎的展示方式。
- 保存成功后显示提示。
- 刷新可重新拉取配置。
- 去掉不需要的重置按钮时，应保持保存和刷新按钮在标题行同一行。

### 5.9 登录页视觉配置

登录页公开配置包括：

- `login.page.title`
- `login.page.subtitle`
- `login.page.background.media-id`
- `login.page.background.grid.enabled`
- `login.mouse.trail.effect`
- `login.password.encrypt.enabled`
- `login.password.encrypt.public-key`

视觉要求：

- 背景色保持白色体系。
- 背景图默认读取 `media_id = 1`。
- 背景图和网格背景不冲突，可叠加展示。
- 登录框颜色与背景相近，并保持一定透明度。
- 登录框四条边线具备动态流动效果。
- 鼠标拖尾效果由配置项控制，可切换多种效果。

## 6. 配置项清单

| 配置项 | 用途 | 是否公开 | 是否敏感 |
|---|---|---:|---:|
| `login.session.timeout.minutes` | 登录 Session 超时时间，单位分钟 | 否 | 否 |
| `login.password.encrypt.enabled` | 是否启用密码加密传输 | 是 | 否 |
| `login.password.encrypt.algorithm` | 密钥算法，默认 RSA | 否 | 否 |
| `login.password.encrypt.cipher-transformation` | Cipher transformation | 否 | 否 |
| `login.password.encrypt.public-key` | 前端加密使用的公钥 | 是 | 否 |
| `login.password.encrypt.private-key` | 后端解密使用的私钥 | 否 | 是 |
| `login.page.title` | 登录页标题 | 是 | 否 |
| `login.page.subtitle` | 登录页副标题 | 是 | 否 |
| `login.mouse.trail.effect` | 鼠标拖尾效果类型 | 是 | 否 |
| `login.page.background.media-id` | 登录页背景图媒体 ID | 是 | 否 |
| `login.page.background.grid.enabled` | 登录页网格背景开关 | 是 | 否 |

## 7. 接口清单

| 接口 | 方法 | 说明 | 登录要求 |
|---|---|---|---|
| `/auth/login` | POST | 登录并建立 Session | 否 |
| `/auth/me` | GET | 查询当前登录人 | 是 |
| `/auth/logout` | POST | 退出登录并销毁 Session | 否 |
| `/api/system-config/forms/{groupCode}` | GET | 查询配置动态表单 | 是 |
| `/api/system-config/forms/{groupCode}` | PUT | 保存配置动态表单 | 是 |
| `/api/system-config/public/{groupCode}` | GET | 查询公开配置 | 否 |
| `/api/media/files/{id}` | GET | 登录页背景图访问 | 否 |

## 8. 数据与模块边界

### 8.1 模块职责

- `hephaestus-login`：登录认证、登录拦截、系统配置、配置缓存、公开配置、登录页资源。
- `hephaestus-org`：人员数据来源、按用户名查询登录人员、头像信息。
- `hephaestus-app`：启动聚合、主聊天页、用户菜单入口。
- `liquibase`：系统配置表结构和初始化配置项。

### 8.2 数据来源

- 登录账号来源：`heph_person.username`。
- 登录密码来源：`heph_person.password`。
- 登录人展示名称来源：`heph_person.person_name`。
- 登录人头像来源：`heph_person.avatar_media_id` 关联媒体访问能力。
- 系统配置来源：`sys_config_definition` + `sys_config_value`。

## 9. 验收标准

- 未登录访问 `/hephaestus/` 或 `/hephaestus/chat.html` 时进入登录页。
- 登录页可正常读取 `/api/system-config/public/main-system`。
- 登录页展示配置化标题、副标题、背景图、网格和鼠标拖尾。
- 前端使用公钥加密密码，后端可正常解密并登录。
- 登录成功后进入聊天页，并在 Redis 中生成 Session。
- 左下角显示当前登录人头像和姓名。
- 点击退出后 Session 失效，再访问主界面回到登录页。
- “设置 -> 常规 -> 主系统配置”可加载动态表单。
- 修改配置并保存后有成功提示，刷新后能回显。
- 公开配置接口不返回私钥。
- 配置项缺失时按“配置项 -> YML -> 运行期临时生成”顺序兜底。
- 服务重启后配置缓存会重新获取，不继续使用旧 Redis 配置缓存。

## 10. 测试场景

- `GET /api/system-config/forms/main-system` 返回表单分区、字段、当前值和默认值。
- `PUT /api/system-config/forms/main-system` 保存合法配置成功。
- 保存未知配置编码时返回错误。
- `GET /api/system-config/public/main-system` 只返回公开配置。
- 公开配置接口不包含 `login.password.encrypt.private-key`。
- 配置值不存在时回退定义表默认值。
- Redis 缓存命中后仍能正确返回配置。
- 保存配置后 Redis 缓存被清理。
- 用户不存在时登录失败。
- 密码错误时登录失败。
- 禁用人员登录失败。
- 加密密码可被后端解密。
- 密钥配置不匹配时输出明确日志并返回登录失败。
- 未登录访问 API 返回 401。
- 未登录访问页面跳转登录页。
- 登录超时时间按配置项生效。
- 退出登录后再次访问受保护接口失败。

## 11. 风险与约束

- 当前密码仍是明文落库，本期只解决传输加密，不解决存储安全。
- 不依赖 HTTPS 的应用层加密只能降低传输明文暴露风险，不能替代完整 HTTPS 安全边界。
- 私钥进入配置体系后必须严格禁止公开接口返回。
- 运行期临时密钥对适合本地兜底，不适合作为生产固定密钥来源。
- Redis 配置缓存需要在保存和重启时清理，否则可能出现配置已改但前端仍读取旧值。
- 登录页视觉效果较多，后续继续叠加时要避免影响登录表单性能和可读性。
