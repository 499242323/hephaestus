# Hephaestus 登录鉴权与登录日志实施计划

> 面向编码代理的要求：实现本计划时必须按任务逐项执行，并遵循 TDD 流程；任务项使用 `- [ ]` 记录进度。

**目标**：实现基于 YML 的登录白名单、登录/登出审计日志、指定节点每日清理日志，以及设置界面的登录日志查询。

**架构**：认证能力收敛在 `modules/hephaestus-login`，复用 Spring MVC 拦截器、MyBatis 仓储和 Liquibase。白名单和清理任务配置来自 YML；登录日志写入 `sys_login_log`；设置抽屉通过登录后接口读取日志。

**技术栈**：Java 17、Spring Boot 3.5、Spring MVC、Spring Session Redis、MyBatis-Plus、Liquibase、原生 HTML/CSS/JS。

## 文件结构

- 新增 `modules/hephaestus-login/src/main/java/olympus/hephaestus/login/auth/LoginAuthProperties.java`：绑定 YML 白名单配置。
- 修改 `modules/hephaestus-login/src/main/java/olympus/hephaestus/login/auth/LoginWebMvcConfiguration.java`：将认证拦截器注册到 `/**`，移除硬编码排除列表。
- 修改 `modules/hephaestus-login/src/main/java/olympus/hephaestus/login/auth/LoginRequiredInterceptor.java`：匹配 YML 白名单，并保留 API 与页面未登录响应差异。
- 新增 `modules/hephaestus-login/src/main/java/olympus/hephaestus/login/log/*`：登录日志实体、仓储、服务、DTO、控制器、客户端信息解析器、清理定时任务、清理配置和节点匹配器。
- 修改 `modules/hephaestus-login/src/main/java/olympus/hephaestus/login/auth/AuthController.java`：传递请求上下文，并在销毁 Session 前记录登出日志。
- 修改 `modules/hephaestus-login/src/main/java/olympus/hephaestus/login/auth/AuthService.java`：记录登录成功和失败日志，不记录密码。
- 修改 `modules/hephaestus-app/src/main/java/olympus/hephaestus/SpringAiDemoApplication.java`：启用配置属性和定时任务。
- 修改 `modules/hephaestus-app/src/main/resources/application.yml`：增加白名单和清理任务默认配置。
- 修改 `modules/liquibase/src/main/resources/db/changelog/db.changelog.xml`：增加 `sys_login_log` 表。
- 修改 `modules/hephaestus-org/src/main/resources/static/org-settings-panel.html`：增加日志菜单和登录日志面板。
- 修改 `modules/hephaestus-org/src/main/resources/static/org-settings.js`：拉取并渲染登录日志。
- 修改 `modules/hephaestus-org/src/main/resources/static/org-settings.css`：补充日志筛选、表格和分页样式。
- 修改 `modules/hephaestus-app/src/main/resources/static/chat.html`：更新 `org-settings` 静态资源版本号。
- 在 `modules/hephaestus-login/src/test/java/olympus/hephaestus/login/...` 下补充测试。

## 任务 1：鉴权白名单配置与匹配器

**文件：**

- 新增：`modules/hephaestus-login/src/main/java/olympus/hephaestus/login/auth/LoginAuthProperties.java`
- 修改：`modules/hephaestus-login/src/main/java/olympus/hephaestus/login/auth/LoginRequiredInterceptor.java`
- 测试：`modules/hephaestus-login/src/test/java/olympus/hephaestus/login/auth/LoginRequiredInterceptorTest.java`

- [ ] **步骤 1：编写白名单行为的失败测试**

创建 `LoginRequiredInterceptorTest`，覆盖精确匹配、Ant 通配匹配、兜底白名单、非白名单 API 未登录拒绝。

```java
package olympus.hephaestus.login.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LoginRequiredInterceptorTest {

    @Test
    void preHandleAllowsConfiguredExactWhitelistPath() throws Exception {
        LoginAuthProperties properties = new LoginAuthProperties();
        properties.getWhitelist().setPaths(List.of("/auth/login"));
        LoginRequiredInterceptor interceptor = new LoginRequiredInterceptor(properties);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/auth/login");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean result = interceptor.preHandle(request, response, new Object());

        assertThat(result).isTrue();
    }

    @Test
    void preHandleAllowsConfiguredAntWhitelistPath() throws Exception {
        LoginAuthProperties properties = new LoginAuthProperties();
        properties.getWhitelist().setPaths(List.of("/api/system-config/public/**"));
        LoginRequiredInterceptor interceptor = new LoginRequiredInterceptor(properties);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/system-config/public/main-system");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean result = interceptor.preHandle(request, response, new Object());

        assertThat(result).isTrue();
    }

    @Test
    void preHandleUsesFallbackWhitelistWhenYmlPathsAreEmpty() throws Exception {
        LoginRequiredInterceptor interceptor = new LoginRequiredInterceptor(new LoginAuthProperties());
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/login.html");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean result = interceptor.preHandle(request, response, new Object());

        assertThat(result).isTrue();
    }

    @Test
    void preHandleRejectsAnonymousApiOutsideWhitelist() throws Exception {
        LoginAuthProperties properties = new LoginAuthProperties();
        properties.getWhitelist().setPaths(List.of("/auth/login"));
        LoginRequiredInterceptor interceptor = new LoginRequiredInterceptor(properties);
        HttpServletRequest request = new MockHttpServletRequest("POST", "/api/chat/send");
        HttpServletResponse response = new MockHttpServletResponse();

        boolean result = interceptor.preHandle(request, response, new Object());

        assertThat(result).isFalse();
        assertThat(((MockHttpServletResponse) response).getStatus()).isEqualTo(401);
        assertThat(((MockHttpServletResponse) response).getContentAsString()).contains("未登录");
    }
}
```

- [ ] **步骤 2：运行测试并确认失败**

执行：`rtk .\tools\mvn-java21.ps1 -pl modules/hephaestus-login -Dtest=LoginRequiredInterceptorTest test`

预期：测试失败，因为 `LoginAuthProperties` 尚不存在，`LoginRequiredInterceptor` 也没有对应构造方法。

- [ ] **步骤 3：实现配置属性和匹配逻辑**

创建 `LoginAuthProperties`：

```java
package olympus.hephaestus.login.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "hephaestus.auth")
public class LoginAuthProperties {

    private final Whitelist whitelist = new Whitelist();

    public Whitelist getWhitelist() {
        return whitelist;
    }

    public static class Whitelist {
        private List<String> paths = new ArrayList<>();

        public List<String> getPaths() {
            return paths;
        }

        public void setPaths(List<String> paths) {
            this.paths = paths == null ? new ArrayList<>() : paths;
        }
    }
}
```

更新 `LoginRequiredInterceptor`：接收 `LoginAuthProperties`，使用 `AntPathMatcher`，并保留兜底路径：

```java
private static final List<String> FALLBACK_WHITELIST = List.of(
        "/login.html", "/login.css", "/login.js", "/auth/login",
        "/api/system-config/public/**", "/api/media/files/*", "/favicon.ico", "/error",
        "/**/*.css", "/**/*.js", "/**/*.png", "/**/*.jpg", "/**/*.jpeg",
        "/**/*.gif", "/**/*.svg", "/**/*.ico", "/**/*.woff", "/**/*.woff2"
);
```

- [ ] **步骤 4：运行测试并确认通过**

执行：`rtk .\tools\mvn-java21.ps1 -pl modules/hephaestus-login -Dtest=LoginRequiredInterceptorTest test`

预期：测试通过。

## 任务 2：注册鉴权配置

**文件：**

- 修改：`modules/hephaestus-login/src/main/java/olympus/hephaestus/login/auth/LoginWebMvcConfiguration.java`
- 修改：`modules/hephaestus-app/src/main/java/olympus/hephaestus/SpringAiDemoApplication.java`
- 修改：`modules/hephaestus-app/src/main/resources/application.yml`

- [ ] **步骤 1：更新 MVC 拦截注册**

将 `addInterceptors` 改为：

```java
registry.addInterceptor(loginRequiredInterceptor)
        .addPathPatterns("/**");
```

- [ ] **步骤 2：启用配置属性和定时任务**

在启动类中加入 `LoginAuthProperties`、日志清理配置属性，并增加 `@EnableScheduling`。

- [ ] **步骤 3：增加 YML 默认配置**

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
  login-log:
    cleanup:
      enabled: true
      current-node: ${HEPHAESTUS_NODE_ID:default}
      enabled-node: default
      retention-days: 30
      cron: "0 0 3 * * ?"
```

- [ ] **步骤 4：运行鉴权拦截器测试**

执行：`rtk .\tools\mvn-java21.ps1 -pl modules/hephaestus-login -Dtest=LoginRequiredInterceptorTest test`

预期：测试通过。

## 任务 3：登录日志表和仓储

**文件：**

- 修改：`modules/liquibase/src/main/resources/db/changelog/db.changelog.xml`
- 新增：`modules/hephaestus-login/src/main/java/olympus/hephaestus/login/log/LoginLogEntity.java`
- 新增：`modules/hephaestus-login/src/main/java/olympus/hephaestus/login/log/LoginLogRepository.java`

- [ ] **步骤 1：增加 Liquibase changeSet**

在 `</databaseChangeLog>` 前追加新的 changeSet：

```xml
<changeSet id="sys-login-log-create-table" author="codex">
    <preConditions onFail="MARK_RAN">
        <not>
            <tableExists tableName="sys_login_log"/>
        </not>
    </preConditions>
    <createTable tableName="sys_login_log">
        <column name="id" type="bigint" autoIncrement="true">
            <constraints nullable="false" primaryKey="true" primaryKeyName="pk_sys_login_log"/>
        </column>
        <column name="operation_type" type="varchar(32)">
            <constraints nullable="false"/>
        </column>
        <column name="username" type="varchar(128)"/>
        <column name="person_id" type="bigint"/>
        <column name="person_name" type="varchar(128)"/>
        <column name="session_id" type="varchar(128)"/>
        <column name="success_flag" type="tinyint">
            <constraints nullable="false"/>
        </column>
        <column name="message" type="varchar(512)"/>
        <column name="client_ip" type="varchar(64)">
            <constraints nullable="false"/>
        </column>
        <column name="user_agent" type="varchar(512)"/>
        <column name="request_uri" type="varchar(255)"/>
        <column name="created_at" type="timestamp" defaultValueComputed="CURRENT_TIMESTAMP">
            <constraints nullable="false"/>
        </column>
    </createTable>
    <createIndex tableName="sys_login_log" indexName="idx_sys_login_log_created_at">
        <column name="created_at"/>
    </createIndex>
    <createIndex tableName="sys_login_log" indexName="idx_sys_login_log_operation">
        <column name="operation_type"/>
        <column name="success_flag"/>
    </createIndex>
</changeSet>
```

- [ ] **步骤 2：创建实体和仓储**

`LoginLogEntity` 使用 MyBatis-Plus 注解，`LoginLogRepository` 继承 `BaseAbstractRepository<LoginLogEntity, Long>`，并提供：

```java
@Insert("INSERT INTO sys_login_log (...) VALUES (...)")
void insertLog(LoginLogEntity entity);

@Select(""" ... """)
List<LoginLogEntity> query(...);

@Select(""" SELECT COUNT(1) ... """)
long count(...);

@Delete("DELETE FROM sys_login_log WHERE created_at < #{cutoffTime}")
int deleteBefore(@Param("cutoffTime") LocalDateTime cutoffTime);
```

- [ ] **步骤 3：执行 diff 检查**

执行：`rtk proxy git diff --check`

预期：没有空白字符错误。

## 任务 4：登录日志服务和客户端信息

**文件：**

- 新增：`modules/hephaestus-login/src/main/java/olympus/hephaestus/login/log/LoginLogOperationType.java`
- 新增：`modules/hephaestus-login/src/main/java/olympus/hephaestus/login/log/LoginLogClientInfo.java`
- 新增：`modules/hephaestus-login/src/main/java/olympus/hephaestus/login/log/LoginLogClientInfoResolver.java`
- 新增：`modules/hephaestus-login/src/main/java/olympus/hephaestus/login/log/LoginLogService.java`
- 测试：`modules/hephaestus-login/src/test/java/olympus/hephaestus/login/log/LoginLogClientInfoResolverTest.java`

- [ ] **步骤 1：编写客户端信息的失败测试**

覆盖 `X-Forwarded-For`、`X-Real-IP` 和 `remoteAddr` 兜底。

- [ ] **步骤 2：运行测试并确认失败**

执行：`rtk .\tools\mvn-java21.ps1 -pl modules/hephaestus-login -Dtest=LoginLogClientInfoResolverTest test`

预期：测试失败，因为客户端信息解析器尚不存在。

- [ ] **步骤 3：实现解析器和服务**

解析器规则：

```java
String forwardedFor = request.getHeader("X-Forwarded-For");
if (StringUtils.hasText(forwardedFor)) {
    return firstNonBlankPart(forwardedFor);
}
String realIp = request.getHeader("X-Real-IP");
if (StringUtils.hasText(realIp)) {
    return realIp.trim();
}
return request.getRemoteAddr();
```

服务规则：

- `recordSuccess(LoginSessionUser user, String sessionId, LoginLogClientInfo clientInfo)`
- `recordFailure(String username, String message, LoginLogClientInfo clientInfo)`
- `recordLogout(LoginSessionUser user, String sessionId, LoginLogClientInfo clientInfo)`
- 捕获仓储异常并使用 `log.error` 打印。
- 按字段长度截断字符串。

- [ ] **步骤 4：运行测试并确认通过**

执行：`rtk .\tools\mvn-java21.ps1 -pl modules/hephaestus-login -Dtest=LoginLogClientInfoResolverTest test`

预期：测试通过。

## 任务 5：接入登录和登出日志

**文件：**

- 修改：`modules/hephaestus-login/src/main/java/olympus/hephaestus/login/auth/AuthController.java`
- 修改：`modules/hephaestus-login/src/main/java/olympus/hephaestus/login/auth/AuthService.java`

- [ ] **步骤 1：控制器增加请求上下文**

登录方法接收 `HttpServletRequest request`，解析 `LoginLogClientInfo`，并传入 `AuthService.login`。

- [ ] **步骤 2：销毁 Session 前记录登出日志**

登出时先读取当前用户和 Session ID，调用 `loginLogService.recordLogout(...)`，再执行 `session.invalidate()`。

- [ ] **步骤 3：记录登录成功和失败**

包装登录逻辑：校验、解密、认证失败时调用 `recordFailure` 后继续抛出 `LoginException`；成功时调用 `recordSuccess`。

- [ ] **步骤 4：运行定向测试**

执行：`rtk .\tools\mvn-java21.ps1 -pl modules/hephaestus-login test`

预期：Java 17 可用时测试通过。

## 任务 6：登录日志查询接口

**文件：**

- 新增：`modules/hephaestus-login/src/main/java/olympus/hephaestus/login/log/LoginLogQueryRequest.java`
- 新增：`modules/hephaestus-login/src/main/java/olympus/hephaestus/login/log/LoginLogResponse.java`
- 新增：`modules/hephaestus-login/src/main/java/olympus/hephaestus/login/log/LoginLogPageResponse.java`
- 新增：`modules/hephaestus-login/src/main/java/olympus/hephaestus/login/log/LoginLogController.java`

- [ ] **步骤 1：实现 DTO**

响应对象使用 record；查询过滤条件可以使用简单请求对象，也可以使用方法参数。

- [ ] **步骤 2：实现控制器**

新增：

```java
@RestController
@RequestMapping("/api/logs/login")
public class LoginLogController {
    @GetMapping
    public LoginLogPageResponse query(...) {
        return loginLogService.query(...);
    }
}
```

将 `pageSize` 限制到最大 100。

- [ ] **步骤 3：确认接口受保护**

不要把 `/api/logs/login` 加入白名单。

## 任务 7：清理定时任务

**文件：**

- 新增：`modules/hephaestus-login/src/main/java/olympus/hephaestus/login/log/LoginLogCleanupProperties.java`
- 新增：`modules/hephaestus-login/src/main/java/olympus/hephaestus/login/log/LoginLogCleanupNodeMatcher.java`
- 新增：`modules/hephaestus-login/src/main/java/olympus/hephaestus/login/log/LoginLogCleanupScheduler.java`
- 测试：`modules/hephaestus-login/src/test/java/olympus/hephaestus/login/log/LoginLogCleanupNodeMatcherTest.java`

- [ ] **步骤 1：编写节点匹配器失败测试**

覆盖以下场景：

- `matchesWhenEnabledAndCurrentEqualsEnabledNode`
- `doesNotMatchWhenDisabled`
- `doesNotMatchWhenCurrentNodeDiffers`
- 匹配前会裁剪首尾空格。

- [ ] **步骤 2：运行测试并确认失败**

执行：`rtk .\tools\mvn-java21.ps1 -pl modules/hephaestus-login -Dtest=LoginLogCleanupNodeMatcherTest test`

预期：测试失败，因为匹配器和配置类尚不存在。

- [ ] **步骤 3：实现清理配置和匹配器**

绑定前缀 `hephaestus.login-log.cleanup`，包含字段：

- `enabled`
- `currentNode`
- `enabledNode`
- `retentionDays`
- `cron`

匹配器仅在启用且裁剪后的当前节点等于执行节点时返回 true。

- [ ] **步骤 4：实现定时任务**

```java
@Scheduled(cron = "${hephaestus.login-log.cleanup.cron:0 0 3 * * ?}")
public void cleanup() {
    if (!nodeMatcher.shouldRun(properties)) {
        log.info("Skip login log cleanup, currentNode={}, enabledNode={}", ...);
        return;
    }
    int retentionDays = Math.max(properties.getRetentionDays(), 1);
    LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays);
    int deleted = loginLogService.cleanupBefore(cutoff);
    log.info("Login log cleanup finished, cutoffTime={}, deleted={}", cutoff, deleted);
}
```

- [ ] **步骤 5：运行匹配器测试**

执行：`rtk .\tools\mvn-java21.ps1 -pl modules/hephaestus-login -Dtest=LoginLogCleanupNodeMatcherTest test`

预期：测试通过。

## 任务 8：设置界面登录日志面板

**文件：**

- 修改：`modules/hephaestus-org/src/main/resources/static/org-settings-panel.html`
- 修改：`modules/hephaestus-org/src/main/resources/static/org-settings.js`
- 修改：`modules/hephaestus-org/src/main/resources/static/org-settings.css`
- 修改：`modules/hephaestus-app/src/main/resources/static/chat.html`

- [ ] **步骤 1：增加日志菜单和面板**

在左侧菜单增加 `logsTabButton`，在抽屉主体增加 `logsPanel`，包含筛选条件和表格容器。

- [ ] **步骤 2：增加 JS 状态和拉取逻辑**

增加：

- 支持 `currentSettingsTab = "logs"`。
- `loginLogState = { page: 1, pageSize: 20, keyword: "", operationType: "", success: "" }`。
- `loadLoginLogs()` 调用 `/api/logs/login`。
- `renderLoginLogs(response)` 渲染表格行。

- [ ] **步骤 3：增加 CSS**

沿用设置界面现有视觉语言，补充紧凑筛选、表格、状态徽标和分页按钮样式。

- [ ] **步骤 4：更新静态资源版本**

更新 `chat.html` 中 `org-settings.css` 和 `org-settings.js` 的查询参数。

- [ ] **步骤 5：运行 JS 语法检查**

执行：`rtk proxy node --check modules/hephaestus-org/src/main/resources/static/org-settings.js`

预期：无语法错误。

## 任务 9：最终验证

**文件：**

- 所有变更文件。

- [ ] **步骤 1：Java 17 可用时运行 Java 测试**

执行：`rtk .\tools\mvn-java21.ps1 test`

预期：测试通过。

- [ ] **步骤 2：运行 JS 语法检查**

执行：`rtk proxy node --check modules/hephaestus-org/src/main/resources/static/org-settings.js`

预期：检查通过。

- [ ] **步骤 3：运行 diff 空白检查**

执行：`rtk proxy git diff --check`

预期：检查通过。

- [ ] **步骤 4：检查敏感日志**

搜索：

`rtk proxy rg -n "password|request.getParameter|request.getInputStream|request.getReader" modules/hephaestus-login/src/main/java/olympus/hephaestus/login -S`

预期：没有密码值写入登录日志，也没有记录请求体。

## 自检

- 规格覆盖：已覆盖 YML 白名单、登录/登出日志、客户端信息、禁止前端错误上报、按启动节点执行清理任务、设置界面登录日志查询。
- 占位符扫描：计划中没有保留 TODO/TBD 占位符。
- 类型一致性：配置前缀为 `hephaestus.auth` 和 `hephaestus.login-log.cleanup`；操作类型值为 `LOGIN_SUCCESS`、`LOGIN_FAILURE`、`LOGOUT`。
