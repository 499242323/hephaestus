# Hephaestus Login Module And System Config PRD

**Date**: 2026-05-27
**Topic**: 独立登录模块 / 通用系统配置 / 动态表单 / Redis Session / 密码加密传输

## Goal

为 `hephaestus` 增加一套可独立演进的登录与系统配置能力：

- 新增独立模块 `modules/hephaestus-login`
- 提供登录页、登录接口、Redis Session、登录拦截
- 新增通用系统配置能力，支持定义表 + 值表
- 在“设置 -> 常规 -> 主系统配置”中动态维护系统参数
- 前端可按需读取一部分公开配置，语义参考 `cn.com.egova.bizbase.web.BaseController#getSysConfigHandler`
- 登录超时时间可配置
- 密码前后端加密传输，不依赖 HTTPS

## Context

- 主应用模块：`modules/hephaestus-app`
- 当前父工程模块定义位于 [pom.xml](E:/NEW_WORK/hephaestus/pom.xml)
- 当前前端主入口为 [chat.html](E:/NEW_WORK/hephaestus/modules/hephaestus-app/src/main/resources/static/chat.html)
- 当前设置抽屉来源于：
  - [org-settings-panel.html](E:/NEW_WORK/hephaestus/modules/hephaestus-org/src/main/resources/static/org-settings-panel.html)
  - [org-settings.js](E:/NEW_WORK/hephaestus/modules/hephaestus-org/src/main/resources/static/org-settings.js)
  - [org-settings.css](E:/NEW_WORK/hephaestus/modules/hephaestus-org/src/main/resources/static/org-settings.css)
- 当前“设置 -> 常规”面板仍为空壳
- 当前仓库已有 Redis 依赖和 Redis 连接配置，但没有 Spring Session Redis 登录态接入
- 当前仓库没有登录页、登录接口、登录拦截器或公开配置读取接口
- 当前 `heph_person` 已有 `username/password/enabled` 字段，可作为第一版登录账号源
- 你已确认：
  - 需要新增独立模块 `modules/hephaestus-login`
  - 系统配置采用“定义表 + 值表”
  - 配置范围是“全局唯一一套”
  - 配置页采用动态表单渲染
  - 前端可读取部分公开配置
  - 登录密码走前后端加密传输

## Repository Findings

| 文件 | 发现 |
|---|---|
| [pom.xml](E:/NEW_WORK/hephaestus/pom.xml) | 当前模块为 `liquibase / mybatis / hephaestus-media / hephaestus-org / hephaestus-app`，可顺势新增 `hephaestus-login` |
| [modules/hephaestus-app/pom.xml](E:/NEW_WORK/hephaestus/modules/hephaestus-app/pom.xml) | 已有 Redis 依赖，但没有 `spring-session-data-redis` |
| [modules/hephaestus-app/src/main/resources/application.yml](E:/NEW_WORK/hephaestus/modules/hephaestus-app/src/main/resources/application.yml) | 已配置 Redis 连接，未配置登录会话超时或业务配置加载 |
| [modules/hephaestus-app/src/main/java/com/example/springaidemo/config/MvcConfiguration.java](E:/NEW_WORK/hephaestus/modules/hephaestus-app/src/main/java/com/example/springaidemo/config/MvcConfiguration.java) | 当前只有 CORS 配置，没有登录拦截器、首页路由或静态页跳转控制 |
| [modules/hephaestus-org/src/main/resources/static/org-settings-panel.html](E:/NEW_WORK/hephaestus/modules/hephaestus-org/src/main/resources/static/org-settings-panel.html) | 常规页目前为空，可承接“主系统配置”标签页 |
| [modules/hephaestus-org/src/main/resources/static/org-settings.js](E:/NEW_WORK/hephaestus/modules/hephaestus-org/src/main/resources/static/org-settings.js) | 已有设置抽屉、tab、toast、错误提示，可继续扩展常规页动态表单逻辑 |
| [modules/liquibase/src/main/resources/db/changelog/db.changelog.xml](E:/NEW_WORK/hephaestus/modules/liquibase/src/main/resources/db/changelog/db.changelog.xml) | 当前尚无系统配置定义表和值表 |
| [modules/hephaestus-org/src/main/java/com/example/springaidemo/org/repository/OrgPersonRepository.java](E:/NEW_WORK/hephaestus/modules/hephaestus-org/src/main/java/com/example/springaidemo/org/repository/OrgPersonRepository.java) | 当前缺少按用户名查询登录人员的方法 |
| `wizdom-urban-v14-framework` 的 `BaseController#getSysConfigHandler` | 参考语义是“向前端返回一部分可公开系统配置项”，而不是全部配置原样下发 |

## Design

### 1. 模块拆分

新增独立模块：

- `modules/hephaestus-login`

该模块负责：

- 登录控制器
- 登录服务
- Redis Session 配置与会话模型
- 系统配置查询与保存接口
- 公开配置读取接口
- 登录页静态资源

主应用模块 `hephaestus-app` 的职责收敛为：

- 聚合业务模块
- 提供聊天主页面和现有应用入口
- 依赖 `hephaestus-login` 后对外暴露登录与配置能力

### 2. 系统配置数据模型

本次采用“双表”方案：

- `sys_config_definition`
- `sys_config_value`

并约束为“全局唯一一套配置”。

#### 2.1 定义表职责

定义表描述配置项本身和前端渲染规则，至少包含：

- `config_code`
- `config_name`
- `config_group`
- `config_tab`
- `component_type`
- `default_value`
- `options_json`
- `placeholder_text`
- `help_text`
- `required_flag`
- `public_flag`
- `sensitive_flag`
- `sort_order`
- `enabled_flag`

其中：

- `public_flag` 控制该配置是否允许前端公开读取
- `sensitive_flag` 控制该配置是否应遮罩显示或限制接口输出

#### 2.2 值表职责

值表只负责保存当前生效值，至少包含：

- `config_code`
- `config_value`
- `updated_by`
- `updated_at`

当值表不存在某项配置时，读取时回退到定义表的 `default_value`。

### 3. 首批主系统配置项

“主系统配置”作为动态分组，第一批先承载登录相关参数：

- `login.session.timeout.minutes`
- `login.password.encrypt.enabled`
- `login.password.encrypt.algorithm`
- `login.password.encrypt.public-key`
- `login.password.encrypt.private-key`
- `login.password.encrypt.cipher-transformation`
- `login.page.title`
- `login.page.subtitle`

控件建议：

- 超时时间：`number`
- 是否启用加密：`switch`
- 算法：`text`
- 公钥：`textarea`
- 私钥：`textarea`
- 标题 / 副标题：`text`

约束建议：

- 公钥：`public_flag=true`
- 私钥：`public_flag=false` 且 `sensitive_flag=true`
- 登录页文案：`public_flag=true`

### 4. 配置中心后端接口

接口放在 `hephaestus-login` 模块中：

- `GET /api/system-config/forms/{groupCode}`
- `PUT /api/system-config/forms/{groupCode}`
- `GET /api/system-config/public/{groupCode}`

#### 4.1 表单接口

`GET /api/system-config/forms/{groupCode}` 返回：

- `groupCode`
- `groupName`
- `sections`
- 每个 section 下的 `fields`

每个 field 至少包含：

- `code`
- `label`
- `componentType`
- `value`
- `defaultValue`
- `required`
- `public`
- `sensitive`
- `placeholder`
- `helpText`
- `options`

`PUT /api/system-config/forms/{groupCode}` 接收：

- `values: { configCode: value }`

后端流程：

1. 读取分组下所有定义项
2. 校验非法编码
3. 按组件类型做基础校验
4. 批量 upsert 到值表
5. 返回最新表单结果

#### 4.2 公开配置接口

`GET /api/system-config/public/{groupCode}` 参考 `getSysConfigHandler` 的“公开读取部分系统配置”语义，只返回：

- 标记为 `public_flag=true`
- 且当前分组允许前端读取的配置项

返回结构可简化为：

- `groupCode`
- `items: { configCode: value }`

严禁通过该接口返回：

- 私钥
- 服务端敏感内部参数
- 标记为 `sensitive_flag=true` 且不允许公开的项

### 5. 配置中心前端设计

不新增独立设置系统，直接扩展现有“设置 -> 常规”。

#### 5.1 页面结构

“常规”页新增：

- 二级标签：`主系统配置`
- 动态表单内容区
- `保存 / 重置 / 刷新` 操作区

#### 5.2 动态渲染规则

前端根据 `componentType` 动态生成控件：

- `text`
- `number`
- `textarea`
- `switch`
- `select`
- `password`

同时渲染：

- 标签
- placeholder
- 帮助说明
- 默认值
- 当前值

对 `sensitive=true` 的字段：

- 优先按密码型展示
- 不在公开配置场景下展示真实值

#### 5.3 用户交互

- 打开设置抽屉
- 选择“常规”
- 默认进入“主系统配置”
- 修改后保存
- 保存成功显示 toast
- 刷新重新拉取 schema 和值
- 重置恢复最近一次后端回显值

### 6. 登录数据源

第一版登录继续复用 `heph_person.username/password`：

- 用户名精确匹配
- 密码解密后与库中 `password` 做字符串比对
- 仅允许 `enabled = true` 的人员登录

本次不实现：

- 独立账号表
- 权限角色模型
- 验证码
- 密码散列迁移

### 7. 密码前后端加密传输

因为你明确要求“不依赖 HTTPS”，本次采用应用层加密。

推荐方案：

- 前端使用公钥加密密码
- 后端使用私钥解密后校验

加密相关配置来自系统配置：

- `login.password.encrypt.enabled`
- `login.password.encrypt.algorithm`
- `login.password.encrypt.public-key`
- `login.password.encrypt.private-key`
- `login.password.encrypt.cipher-transformation`

约束：

- 私钥只在服务端使用
- 公钥可通过公开配置接口下发
- 若关闭加密开关，可退回明文提交流程，但默认应开启

### 8. Redis Session 与超时配置

登录态采用 Spring Session Redis。

实现方式：

- `hephaestus-login` 引入 `spring-session-data-redis`
- 登录成功后写入 `HttpSession`
- 会话实际存入 Redis

Session 中至少保存：

- `personId`
- `username`
- `personName`
- `unitId`
- `loginAt`

会话超时时间读取配置项：

- `login.session.timeout.minutes`

默认值可由定义表提供，建议默认 `30`。

如果运行时动态更新所有已有 Session 成本过高，本版允许按“配置变更后对新登录会话生效”处理，并在配置帮助说明中明确。

### 9. 登录接口与公开配置读取

登录模块至少提供：

- `POST /auth/login`
- `POST /auth/logout`
- `GET /auth/me`
- `GET /api/system-config/public/main-system`

#### 9.1 接口职责

- `GET /api/system-config/public/main-system`
  - 登录页和部分前端功能读取公开配置
  - 返回标题、公钥、加密开关等
- `POST /auth/login`
  - 接收用户名和密码密文
  - 解密并校验
  - 建立 Redis Session
- `GET /auth/me`
  - 返回当前登录用户摘要
- `POST /auth/logout`
  - 销毁当前 Session

### 10. 访问控制

继续采用 MVC 拦截器，不引入完整 Spring Security。

放行：

- `/login.html`
- `/login.css`
- `/login.js`
- `/auth/login`
- `/auth/logout`
- `/api/system-config/public/main-system`
- 登录页需要的静态资源

拦截：

- `/`
- `/chat.html`
- `/api/**`

未登录处理：

- 页面访问：302 跳转 `/login.html`
- API 访问：401 JSON

### 11. 登录页与聊天页跳转

登录静态资源放在 `hephaestus-login` 模块中，并通过主应用聚合后对外提供。

行为要求：

- 访问 `/${context-path}/` 时，未登录进入登录页，已登录进入 `chat.html`
- 直接访问 `chat.html` 时，无登录态则跳回登录页
- 登录成功后进入聊天页
- 聊天页增加“退出登录”入口

登录页展示内容优先来自公开配置接口：

- 登录页标题
- 登录页副标题
- 加密开关
- 公钥

### 12. 模块职责划分

- `hephaestus-login`
  - 登录控制器
  - 登录服务
  - 系统配置服务
  - Redis Session 接入
  - 公开配置接口
  - 登录页静态资源
- `hephaestus-app`
  - 主应用聚合入口
  - 引入登录模块
  - 聊天主页面与现有业务入口
- `hephaestus-org`
  - 组织人员查询
  - 补充按用户名查询方法
- `liquibase`
  - 配置定义表和值表变更集

## TDD Plan

### 测试位置

优先在 `modules/hephaestus-login/src/test/java` 下新增：

- 系统配置接口测试
- 公开配置接口测试
- 登录接口测试
- 登录拦截器测试

### First Failing Test

先写两个 failing test：

1. `GET /api/system-config/forms/main-system` 期望返回 `schema + values`
2. `GET /api/system-config/public/main-system` 期望只返回 `public_flag=true` 的项

随后再写登录成功测试：

- 使用 `alice / 123456`
- 模拟前端按公钥加密密码
- 调用 `POST /auth/login`
- 期望返回 200 且建立 Session

### Key Test Scenarios

1. 配置表单查询能返回字段定义和当前值
2. 配置保存后能正确写入值表
3. 未配置值时会回退默认值
4. 公开配置接口不会暴露私钥
5. 公开配置接口只返回允许公开项
6. 启用加密时，前端密文可被后端正确解密
7. 用户名不存在时登录失败
8. 密码错误时登录失败
9. `enabled=false` 人员不可登录
10. 未登录访问 `/api/**` 返回 401
11. 未登录访问 `/` 或 `/chat.html` 跳到 `/login.html`
12. 登录成功后 Session 存入 Redis
13. 登录超时时间按配置项生效
14. 退出登录后 Session 被销毁

### Minimal Implementation Scope

第一轮只实现：

- `hephaestus-login` 独立模块
- 双表配置中心
- 常规页“主系统配置”动态表单
- 公开配置读取接口
- 登录页
- Redis Session
- RSA 密码加密传输
- 超时时间配置接入

不实现：

- 多租户配置
- 配置历史版本
- 配置变更审计
- 密码散列存储迁移
- 动态热刷新所有历史 Session

### Verification Commands

- 新增模块后，至少验证父 `pom` 能正确识别模块
- 涉及 Liquibase 时，新增 changelog 并检查语法
- 若全仓编译被历史问题阻塞，至少保证：
  - `hephaestus-login` 相关类编译通过
  - Liquibase 变更语法无误
  - 前端脚本语法通过
  - 本地登录和配置保存流程可手测

## Risks

- 当前人员密码仍为明文存储，本次只解决传输加密，不解决落库存储安全
- 私钥保存在业务配置体系中，需要严格限制接口暴露与前端展示
- 动态表单会让“常规”页脚本复杂度提升，需要控制 schema 范围
- 会话超时配置若要求即时影响所有历史会话，实现复杂度会明显上升
- 新增独立模块后，静态资源聚合路径和依赖装配需要谨慎验证

## Verification

- 父工程成功识别 `modules/hephaestus-login`
- 打开 `http://127.0.0.1:11018/hephaestus/`，未登录时进入登录页
- 登录页能通过公开配置接口读取标题、公钥等公开参数
- 用 `alice / 123456` 登录成功并进入聊天页
- Redis 中可看到对应 Session
- 打开“设置 -> 常规 -> 主系统配置”，可看到动态生成的配置表单
- 修改并保存配置后，重新加载仍能回显新值
- 私钥等敏感项不会出现在公开配置接口中
- 超时时间配置修改后，新登录会话按新值生效
- 未登录访问受保护接口时返回 401 或跳回登录页
