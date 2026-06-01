# Operation Log Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

> **当前实现说明：** 本历史计划早期示例曾让前端通过请求头传递操作人。当前代码已统一从登录 Session 获取当前人员，前端不再传递人员身份。

**Goal:** 新增结构化明细型操作日志，记录系统配置、部门、人员、岗位等管理写操作，并在现有日志页面中按权限查询显示。

**Architecture:** 操作日志与登录日志并列放在 `modules/hephaestus-login` 的 `login.log` 分层下，使用独立表、独立 Service、独立 Controller。业务模块通过 `OperationLogService` 记录中文摘要和中文明细，日志写入失败不阻断主业务。前端沿用现有“日志 -> 操作日志”页签，查询结构参考登录日志已有筛选区、时间范围、分页卡片和表格。

**Tech Stack:** Java 17、Spring Boot 3、MyBatis、Liquibase、原生 HTML/CSS/JS、现有 `Pagination<T>` 分页结构。

---

## File Structure

- Create: `modules/hephaestus-login/src/main/java/com/example/springaidemo/login/log/domain/OperationLogEntity.java`
  - 操作日志数据库实体。
- Create: `modules/hephaestus-login/src/main/java/com/example/springaidemo/login/log/dto/OperationLogResponse.java`
  - 操作日志列表响应 DTO。
- Create: `modules/hephaestus-login/src/main/java/com/example/springaidemo/login/log/dto/OperationLogRecordRequest.java`
  - 业务记录操作日志的请求 DTO。
- Create: `modules/hephaestus-login/src/main/java/com/example/springaidemo/login/log/repository/OperationLogRepository.java`
  - 操作日志 MyBatis Mapper，负责插入、分页查询和 count。
- Create: `modules/hephaestus-login/src/main/java/com/example/springaidemo/login/log/service/OperationLogService.java`
  - 操作日志服务接口。
- Create: `modules/hephaestus-login/src/main/java/com/example/springaidemo/login/log/service/impl/OperationLogServiceImpl.java`
  - 操作日志服务实现，负责旁路写入和分页查询。
- Create: `modules/hephaestus-login/src/main/java/com/example/springaidemo/login/log/controller/OperationLogController.java`
  - 操作日志查询接口，校验 `general.log.operation.view`。
- Modify: `modules/liquibase/src/main/resources/db/changelog/db.changelog.xml`
  - 追加新 changeset 创建 `heph_operation_log` 表和索引，不修改旧 changeset。
- Modify: `modules/hephaestus-login/src/main/java/com/example/springaidemo/login/config/controller/SystemConfigController.java`
  - 保存配置成功后记录操作日志。
- Modify: `modules/hephaestus-org/src/main/java/com/example/springaidemo/org/controller/OrgUnitController.java`
  - 部门新增、修改、删除、排序写操作接入操作日志。
- Modify: `modules/hephaestus-org/src/main/java/com/example/springaidemo/org/service/OrgPersonService.java`
  - 人员新增、修改、删除、头像、人员岗位写操作接入操作日志。
- Modify: `modules/hephaestus-org/src/main/java/com/example/springaidemo/org/role/service/impl/OrgRoleServiceImpl.java`
  - 岗位新增、修改、删除、岗位权限、刷新权限写操作接入操作日志。
- Modify: `modules/hephaestus-org/src/main/java/com/example/springaidemo/org/role/service/impl/OrgPersonRoleServiceImpl.java`
  - 岗位人员保存接入操作日志。
- Modify: `modules/hephaestus-org/src/main/resources/static/org-settings-panel.html`
  - 将操作日志占位区改成与登录日志一致的查询区、表格区、分页区。
- Modify: `modules/hephaestus-org/src/main/resources/static/org-settings.js`
  - 新增操作日志状态、查询参数、渲染表格、权限隐藏和事件绑定；结构参考已有登录日志实现。
- Modify: `modules/hephaestus-org/src/main/resources/static/org-settings.css`
  - 复用已有日志筛选、表格、分页样式；仅补充操作日志展开明细需要的少量样式。
- Modify: `modules/hephaestus-app/src/main/resources/static/chat.html`
  - 更新静态资源版本号，避免缓存。

## Task 1: 数据库表和基础实体

**Files:**
- Modify: `modules/liquibase/src/main/resources/db/changelog/db.changelog.xml`
- Create: `modules/hephaestus-login/src/main/java/com/example/springaidemo/login/log/domain/OperationLogEntity.java`

- [ ] **Step 1: 追加 Liquibase changeset**

在 `db.changelog.xml` 文件末尾、根节点结束前追加新 changeset。不要修改已经存在的 changeset。

```xml
    <changeSet id="operation-log-table-20260601" author="codex">
        <createTable tableName="heph_operation_log">
            <column name="id" type="BIGINT" autoIncrement="true">
                <constraints primaryKey="true" nullable="false"/>
            </column>
            <column name="operator_person_id" type="BIGINT"/>
            <column name="operator_name" type="VARCHAR(100)"/>
            <column name="operator_username" type="VARCHAR(100)"/>
            <column name="operator_unit_id" type="BIGINT"/>
            <column name="operator_unit_name" type="VARCHAR(200)"/>
            <column name="module_code" type="VARCHAR(64)">
                <constraints nullable="false"/>
            </column>
            <column name="module_name" type="VARCHAR(100)">
                <constraints nullable="false"/>
            </column>
            <column name="action_code" type="VARCHAR(64)">
                <constraints nullable="false"/>
            </column>
            <column name="action_name" type="VARCHAR(100)">
                <constraints nullable="false"/>
            </column>
            <column name="target_type" type="VARCHAR(64)"/>
            <column name="target_id" type="VARCHAR(64)"/>
            <column name="target_name" type="VARCHAR(200)"/>
            <column name="success" type="BOOLEAN" defaultValueBoolean="true">
                <constraints nullable="false"/>
            </column>
            <column name="summary" type="VARCHAR(500)">
                <constraints nullable="false"/>
            </column>
            <column name="detail" type="VARCHAR(2000)"/>
            <column name="client_ip" type="VARCHAR(64)"/>
            <column name="user_agent" type="VARCHAR(1000)"/>
            <column name="request_uri" type="VARCHAR(500)"/>
            <column name="request_method" type="VARCHAR(16)"/>
            <column name="created_at" type="DATETIME" defaultValueComputed="CURRENT_TIMESTAMP">
                <constraints nullable="false"/>
            </column>
        </createTable>
        <createIndex tableName="heph_operation_log" indexName="idx_heph_operation_log_created_at">
            <column name="created_at"/>
            <column name="id"/>
        </createIndex>
        <createIndex tableName="heph_operation_log" indexName="idx_heph_operation_log_operator">
            <column name="operator_person_id"/>
        </createIndex>
        <createIndex tableName="heph_operation_log" indexName="idx_heph_operation_log_module_action">
            <column name="module_code"/>
            <column name="action_code"/>
        </createIndex>
        <createIndex tableName="heph_operation_log" indexName="idx_heph_operation_log_target">
            <column name="target_type"/>
            <column name="target_id"/>
        </createIndex>
    </changeSet>
```

- [ ] **Step 2: 新增实体类**

```java
package com.example.springaidemo.login.log.domain;

import com.example.springaidemo.mybatis.annotation.Column;
import com.example.springaidemo.mybatis.annotation.Id;
import com.example.springaidemo.mybatis.annotation.Table;

import java.time.LocalDateTime;

@Table("heph_operation_log")
public class OperationLogEntity {
    @Id
    private Long id;
    @Column("operator_person_id")
    private Long operatorPersonId;
    @Column("operator_name")
    private String operatorName;
    @Column("operator_username")
    private String operatorUsername;
    @Column("operator_unit_id")
    private Long operatorUnitId;
    @Column("operator_unit_name")
    private String operatorUnitName;
    @Column("module_code")
    private String moduleCode;
    @Column("module_name")
    private String moduleName;
    @Column("action_code")
    private String actionCode;
    @Column("action_name")
    private String actionName;
    @Column("target_type")
    private String targetType;
    @Column("target_id")
    private String targetId;
    @Column("target_name")
    private String targetName;
    @Column("success")
    private Boolean success;
    @Column("summary")
    private String summary;
    @Column("detail")
    private String detail;
    @Column("client_ip")
    private String clientIp;
    @Column("user_agent")
    private String userAgent;
    @Column("request_uri")
    private String requestUri;
    @Column("request_method")
    private String requestMethod;
    @Column("created_at")
    private LocalDateTime createdAt;

    // Generate standard getters and setters for every field.
}
```

- [ ] **Step 3: 编译验证实体注解**

Run:

```powershell
rtk proxy powershell -NoProfile -Command "mvn -pl modules/hephaestus-login -am -DskipTests compile"
```

Expected: 编译通过；如果 `@Column`、`@Id`、`@Table` 注解名称与现有实体不同，以 `LoginLogEntity` 的写法为准修正。

## Task 2: Repository、DTO 和 Service

**Files:**
- Create: `modules/hephaestus-login/src/main/java/com/example/springaidemo/login/log/dto/OperationLogResponse.java`
- Create: `modules/hephaestus-login/src/main/java/com/example/springaidemo/login/log/dto/OperationLogRecordRequest.java`
- Create: `modules/hephaestus-login/src/main/java/com/example/springaidemo/login/log/repository/OperationLogRepository.java`
- Create: `modules/hephaestus-login/src/main/java/com/example/springaidemo/login/log/service/OperationLogService.java`
- Create: `modules/hephaestus-login/src/main/java/com/example/springaidemo/login/log/service/impl/OperationLogServiceImpl.java`

- [ ] **Step 1: 新增响应 DTO**

```java
package com.example.springaidemo.login.log.dto;

import com.example.springaidemo.login.log.domain.OperationLogEntity;

import java.time.LocalDateTime;

public record OperationLogResponse(
        Long id,
        Long operatorPersonId,
        String operatorName,
        String operatorUsername,
        Long operatorUnitId,
        String operatorUnitName,
        String moduleCode,
        String moduleName,
        String actionCode,
        String actionName,
        String targetType,
        String targetId,
        String targetName,
        Boolean success,
        String summary,
        String detail,
        String clientIp,
        String userAgent,
        String requestUri,
        String requestMethod,
        LocalDateTime createdAt
) {
    public static OperationLogResponse from(OperationLogEntity entity) {
        return new OperationLogResponse(
                entity.getId(),
                entity.getOperatorPersonId(),
                entity.getOperatorName(),
                entity.getOperatorUsername(),
                entity.getOperatorUnitId(),
                entity.getOperatorUnitName(),
                entity.getModuleCode(),
                entity.getModuleName(),
                entity.getActionCode(),
                entity.getActionName(),
                entity.getTargetType(),
                entity.getTargetId(),
                entity.getTargetName(),
                entity.getSuccess(),
                entity.getSummary(),
                entity.getDetail(),
                entity.getClientIp(),
                entity.getUserAgent(),
                entity.getRequestUri(),
                entity.getRequestMethod(),
                entity.getCreatedAt()
        );
    }
}
```

- [ ] **Step 2: 新增记录请求 DTO**

```java
package com.example.springaidemo.login.log.dto;

public record OperationLogRecordRequest(
        Long operatorPersonId,
        String operatorName,
        String operatorUsername,
        Long operatorUnitId,
        String operatorUnitName,
        String moduleCode,
        String moduleName,
        String actionCode,
        String actionName,
        String targetType,
        String targetId,
        String targetName,
        String summary,
        String detail
) {
}
```

- [ ] **Step 3: 新增 Repository**

参考 `LoginLogRepository` 的 SQL 风格创建 `OperationLogRepository`。插入字段与实体字段一致，查询支持 `keyword`、`moduleCode`、`actionCode`、`success`、`startTime`、`endTime`、`offset`、`limit`。

```java
package com.example.springaidemo.login.log.repository;

import com.example.springaidemo.login.log.domain.OperationLogEntity;
import com.example.springaidemo.mybatis.repository.BaseAbstractRepository;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface OperationLogRepository extends BaseAbstractRepository<OperationLogEntity, Long> {

    @Insert("""
            INSERT INTO heph_operation_log (
                operator_person_id, operator_name, operator_username, operator_unit_id, operator_unit_name,
                module_code, module_name, action_code, action_name, target_type, target_id, target_name,
                success, summary, detail, client_ip, user_agent, request_uri, request_method, created_at
            ) VALUES (
                #{operatorPersonId}, #{operatorName}, #{operatorUsername}, #{operatorUnitId}, #{operatorUnitName},
                #{moduleCode}, #{moduleName}, #{actionCode}, #{actionName}, #{targetType}, #{targetId}, #{targetName},
                #{success}, #{summary}, #{detail}, #{clientIp}, #{userAgent}, #{requestUri}, #{requestMethod}, #{createdAt}
            )
            """)
    void insertLog(OperationLogEntity entity);

    @Select("""
            <script>
            SELECT id,
                   operator_person_id AS operatorPersonId,
                   operator_name AS operatorName,
                   operator_username AS operatorUsername,
                   operator_unit_id AS operatorUnitId,
                   operator_unit_name AS operatorUnitName,
                   module_code AS moduleCode,
                   module_name AS moduleName,
                   action_code AS actionCode,
                   action_name AS actionName,
                   target_type AS targetType,
                   target_id AS targetId,
                   target_name AS targetName,
                   success,
                   summary,
                   detail,
                   client_ip AS clientIp,
                   user_agent AS userAgent,
                   request_uri AS requestUri,
                   request_method AS requestMethod,
                   created_at AS createdAt
              FROM heph_operation_log
             WHERE 1 = 1
               <if test="keyword != null and keyword != ''">
                 AND (
                    operator_name LIKE CONCAT('%', #{keyword}, '%')
                    OR operator_username LIKE CONCAT('%', #{keyword}, '%')
                    OR target_name LIKE CONCAT('%', #{keyword}, '%')
                    OR summary LIKE CONCAT('%', #{keyword}, '%')
                    OR detail LIKE CONCAT('%', #{keyword}, '%')
                    OR client_ip LIKE CONCAT('%', #{keyword}, '%')
                 )
               </if>
               <if test="moduleCode != null and moduleCode != ''">
                 AND module_code = #{moduleCode}
               </if>
               <if test="actionCode != null and actionCode != ''">
                 AND action_code = #{actionCode}
               </if>
               <if test="success != null">
                 AND success = #{success}
               </if>
               <if test="startTime != null">
                 AND created_at &gt;= #{startTime}
               </if>
               <if test="endTime != null">
                 AND created_at &lt;= #{endTime}
               </if>
             ORDER BY created_at DESC, id DESC
             LIMIT #{limit} OFFSET #{offset}
            </script>
            """)
    List<OperationLogEntity> query(@Param("keyword") String keyword,
                                   @Param("moduleCode") String moduleCode,
                                   @Param("actionCode") String actionCode,
                                   @Param("success") Boolean success,
                                   @Param("startTime") LocalDateTime startTime,
                                   @Param("endTime") LocalDateTime endTime,
                                   @Param("offset") long offset,
                                   @Param("limit") int limit);

    @Select("""
            <script>
            SELECT COUNT(1)
              FROM heph_operation_log
             WHERE 1 = 1
               <if test="keyword != null and keyword != ''">
                 AND (
                    operator_name LIKE CONCAT('%', #{keyword}, '%')
                    OR operator_username LIKE CONCAT('%', #{keyword}, '%')
                    OR target_name LIKE CONCAT('%', #{keyword}, '%')
                    OR summary LIKE CONCAT('%', #{keyword}, '%')
                    OR detail LIKE CONCAT('%', #{keyword}, '%')
                    OR client_ip LIKE CONCAT('%', #{keyword}, '%')
                 )
               </if>
               <if test="moduleCode != null and moduleCode != ''">
                 AND module_code = #{moduleCode}
               </if>
               <if test="actionCode != null and actionCode != ''">
                 AND action_code = #{actionCode}
               </if>
               <if test="success != null">
                 AND success = #{success}
               </if>
               <if test="startTime != null">
                 AND created_at &gt;= #{startTime}
               </if>
               <if test="endTime != null">
                 AND created_at &lt;= #{endTime}
               </if>
            </script>
            """)
    long count(@Param("keyword") String keyword,
               @Param("moduleCode") String moduleCode,
               @Param("actionCode") String actionCode,
               @Param("success") Boolean success,
               @Param("startTime") LocalDateTime startTime,
               @Param("endTime") LocalDateTime endTime);
}
```

- [ ] **Step 4: 新增 Service 接口和实现**

实现中参考 `LoginLogServiceImpl` 的分页和 `saveQuietly` 策略。`recordSuccess` 设置 `success=true`，补齐 `createdAt=LocalDateTime.now()`；请求信息可先由调用方后续扩展，当前 Service 接收 DTO 中业务字段。

```java
package com.example.springaidemo.login.log.service;

import com.example.springaidemo.login.log.dto.OperationLogRecordRequest;
import com.example.springaidemo.login.log.dto.OperationLogResponse;
import com.example.springaidemo.mybatis.pagination.Pagination;

import java.time.LocalDateTime;

public interface OperationLogService {
    void recordSuccess(OperationLogRecordRequest request);

    void recordFailure(OperationLogRecordRequest request, String message);

    Pagination<OperationLogResponse> query(String keyword,
                                           String moduleCode,
                                           String actionCode,
                                           Boolean success,
                                           LocalDateTime startTime,
                                           LocalDateTime endTime,
                                           int page,
                                           int pageSize);
}
```

```java
package com.example.springaidemo.login.log.service.impl;

import com.example.springaidemo.login.log.domain.OperationLogEntity;
import com.example.springaidemo.login.log.dto.OperationLogRecordRequest;
import com.example.springaidemo.login.log.dto.OperationLogResponse;
import com.example.springaidemo.login.log.repository.OperationLogRepository;
import com.example.springaidemo.login.log.service.OperationLogService;
import com.example.springaidemo.mybatis.pagination.Pagination;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class OperationLogServiceImpl implements OperationLogService {
    private static final Logger log = LoggerFactory.getLogger(OperationLogServiceImpl.class);

    private final OperationLogRepository repository;

    public OperationLogServiceImpl(OperationLogRepository repository) {
        this.repository = repository;
    }

    @Override
    public void recordSuccess(OperationLogRecordRequest request) {
        saveQuietly(toEntity(request, true, null));
    }

    @Override
    public void recordFailure(OperationLogRecordRequest request, String message) {
        saveQuietly(toEntity(request, false, message));
    }

    @Override
    public Pagination<OperationLogResponse> query(String keyword,
                                                  String moduleCode,
                                                  String actionCode,
                                                  Boolean success,
                                                  LocalDateTime startTime,
                                                  LocalDateTime endTime,
                                                  int page,
                                                  int pageSize) {
        int normalizedPage = Math.max(page, 1);
        int normalizedPageSize = Math.min(Math.max(pageSize, 1), 100);
        long offset = (long) (normalizedPage - 1) * normalizedPageSize;
        List<OperationLogResponse> items = repository.query(keyword, moduleCode, actionCode, success, startTime, endTime, offset, normalizedPageSize)
                .stream()
                .map(OperationLogResponse::from)
                .toList();
        long total = repository.count(keyword, moduleCode, actionCode, success, startTime, endTime);
        return repository.toPagination(items, normalizedPage, normalizedPageSize, total);
    }

    private OperationLogEntity toEntity(OperationLogRecordRequest request, boolean success, String failureMessage) {
        OperationLogEntity entity = new OperationLogEntity();
        entity.setOperatorPersonId(request.operatorPersonId());
        entity.setOperatorName(request.operatorName());
        entity.setOperatorUsername(request.operatorUsername());
        entity.setOperatorUnitId(request.operatorUnitId());
        entity.setOperatorUnitName(request.operatorUnitName());
        entity.setModuleCode(request.moduleCode());
        entity.setModuleName(request.moduleName());
        entity.setActionCode(request.actionCode());
        entity.setActionName(request.actionName());
        entity.setTargetType(request.targetType());
        entity.setTargetId(request.targetId());
        entity.setTargetName(request.targetName());
        entity.setSuccess(success);
        entity.setSummary(request.summary());
        entity.setDetail(success ? request.detail() : failureMessage);
        entity.setCreatedAt(LocalDateTime.now());
        return entity;
    }

    private void saveQuietly(OperationLogEntity entity) {
        try {
            repository.insertLog(entity);
        } catch (Exception exception) {
            log.warn("写入操作日志失败: {}", entity.getSummary(), exception);
        }
    }
}
```

- [ ] **Step 5: 编译验证**

Run:

```powershell
rtk proxy powershell -NoProfile -Command "mvn -pl modules/hephaestus-login -am -DskipTests compile"
```

Expected: 编译通过。

## Task 3: 查询接口和权限校验

**Files:**
- Create: `modules/hephaestus-login/src/main/java/com/example/springaidemo/login/log/controller/OperationLogController.java`

- [ ] **Step 1: 新增 Controller**

```java
package com.example.springaidemo.login.log.controller;

import com.example.springaidemo.login.log.dto.OperationLogResponse;
import com.example.springaidemo.login.log.service.OperationLogService;
import com.example.springaidemo.mybatis.pagination.Pagination;
import com.example.springaidemo.org.role.constant.OrgPermissionCodes;
import com.example.springaidemo.org.role.service.OrgPermissionGuard;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/logs/operations")
public class OperationLogController {
    private final OperationLogService operationLogService;
    private final OrgPermissionGuard permissionGuard;

    public OperationLogController(OperationLogService operationLogService,
                                  OrgPermissionGuard permissionGuard) {
        this.operationLogService = operationLogService;
        this.permissionGuard = permissionGuard;
    }

    @GetMapping
    public Pagination<OperationLogResponse> query(HttpSession session,
                                                  @RequestParam(value = "keyword", required = false) String keyword,
                                                  @RequestParam(value = "moduleCode", required = false) String moduleCode,
                                                  @RequestParam(value = "actionCode", required = false) String actionCode,
                                                  @RequestParam(value = "success", required = false) Boolean success,
                                                  @RequestParam(value = "startTime", required = false)
                                                  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
                                                  @RequestParam(value = "endTime", required = false)
                                                  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
                                                  @RequestParam(value = "page", defaultValue = "1") int page,
                                                  @RequestParam(value = "pageSize", defaultValue = "10") int pageSize) {
        Long personId = authService.currentUser(session).personId();
        permissionGuard.requirePermission(personId, OrgPermissionCodes.GENERAL_LOG_OPERATION_VIEW);
        return operationLogService.query(keyword, moduleCode, actionCode, success, startTime, endTime, page, pageSize);
    }
}
```

- [ ] **Step 2: 编译验证**

Run:

```powershell
rtk proxy powershell -NoProfile -Command "mvn -pl modules/hephaestus-login -am -DskipTests compile"
```

Expected: 编译通过；若 `OrgPermissionGuard` 包路径或方法名不同，以现有 `LoginLogController` 和 `SystemConfigController` 为准。

## Task 4: 系统配置操作日志

**Files:**
- Modify: `modules/hephaestus-login/src/main/java/com/example/springaidemo/login/config/controller/SystemConfigController.java`

- [ ] **Step 1: 注入 OperationLogService**

在构造函数注入 `OperationLogService`。保存配置成功后记录：

```java
operationLogService.recordSuccess(new OperationLogRecordRequest(
        personId,
        null,
        null,
        null,
        null,
        "system-config",
        "系统配置",
        "save-config",
        "保存配置",
        "配置",
        groupCode,
        "main-system".equals(groupCode) ? "主系统配置" : groupCode,
        "保存系统配置：" + ("main-system".equals(groupCode) ? "主系统配置" : groupCode),
        buildConfigSaveDetail(request)
));
```

- [ ] **Step 2: 新增配置明细生成方法**

在 Controller 内新增私有方法。敏感项只显示“已更新”，普通项显示配置 key 已保存。

```java
private String buildConfigSaveDetail(SystemConfigSaveRequest request) {
    if (request == null || request.values() == null || request.values().isEmpty()) {
        return "保存配置：未提交配置项。";
    }
    return request.values().keySet().stream()
            .sorted()
            .map(key -> isSensitiveConfigKey(key) ? key + " 已更新" : key + " 已保存")
            .collect(Collectors.joining("；"));
}

private boolean isSensitiveConfigKey(String key) {
    String lower = key == null ? "" : key.toLowerCase(Locale.ROOT);
    return lower.contains("password")
            || lower.contains("private-key")
            || lower.contains("secret")
            || lower.contains("token");
}
```

- [ ] **Step 3: 编译验证**

Run:

```powershell
rtk proxy powershell -NoProfile -Command "mvn -pl modules/hephaestus-login -am -DskipTests compile"
```

Expected: 编译通过。

## Task 5: 组织写操作接入操作日志

**Files:**
- Modify: `modules/hephaestus-org/src/main/java/com/example/springaidemo/org/controller/OrgUnitController.java`
- Modify: `modules/hephaestus-org/src/main/java/com/example/springaidemo/org/service/OrgPersonService.java`
- Modify: `modules/hephaestus-org/src/main/java/com/example/springaidemo/org/role/service/impl/OrgRoleServiceImpl.java`
- Modify: `modules/hephaestus-org/src/main/java/com/example/springaidemo/org/role/service/impl/OrgPersonRoleServiceImpl.java`

- [ ] **Step 1: 部门操作记录**

在部门新增、修改、删除、排序成功后调用 `OperationLogService.recordSuccess`。摘要使用：

```java
"新增部门：" + unitName
"修改部门：" + unitName
"删除部门：" + unitName
"调整部门排序：" + unitName
```

明细使用中文短句，至少包含部门名称、父部门 ID 或父部门名称、排序值。

- [ ] **Step 2: 人员操作记录**

在人员新增、修改、删除、头像上传、头像清空、人员岗位保存成功后记录日志。摘要使用：

```java
"新增人员：" + personName
"修改人员：" + personName
"删除人员：" + personName
"上传人员头像：" + personName
"清空人员头像：" + personName
"保存人员岗位：" + personName
```

明细中包含人员名称、部门、手机号、邮箱、岗位变化摘要。敏感字段不记录明文。

- [ ] **Step 3: 岗位操作记录**

在岗位新增、修改、删除、岗位权限保存、刷新人员权限成功后记录日志。摘要使用：

```java
"新增岗位：" + roleName
"修改岗位：" + roleName
"删除岗位：" + roleName
"保存岗位权限：" + roleName
"刷新岗位人员权限：" + roleName
```

岗位权限明细记录新增和移除的权限显示名；如果当前实现难以拿到显示名，先记录权限编码列表，并在前端显示时保持中文字段名。

- [ ] **Step 4: 岗位人员保存记录**

在岗位人员保存成功后记录：

```java
"保存岗位人员：" + roleName
```

明细记录新增人员和移除人员名称；如果只能拿到 ID，先记录人员 ID 列表。

- [ ] **Step 5: 编译验证**

Run:

```powershell
rtk proxy powershell -NoProfile -Command "mvn -pl modules/hephaestus-org -am -DskipTests compile"
```

Expected: 编译通过。

## Task 6: 前端操作日志查询界面

**Files:**
- Modify: `modules/hephaestus-org/src/main/resources/static/org-settings-panel.html`
- Modify: `modules/hephaestus-org/src/main/resources/static/org-settings.js`
- Modify: `modules/hephaestus-org/src/main/resources/static/org-settings.css`
- Modify: `modules/hephaestus-app/src/main/resources/static/chat.html`

- [ ] **Step 1: HTML 复用登录日志查询结构**

将 `operationLogPane` 中的占位文案替换为与 `loginLogPane` 同类结构：

```html
<div id="operationLogPane" class="settings-log-pane" hidden>
    <div class="settings-log-filters">
        <input id="operationLogKeywordInput" class="settings-input" type="search" placeholder="操作人 / 对象 / 摘要 / IP">
        <input id="operationLogTimeRangeInput" class="settings-input" type="text" placeholder="请选择时间范围" readonly>
        <select id="operationLogModuleSelect" class="settings-input">
            <option value="">全部模块</option>
            <option value="system-config">系统配置</option>
            <option value="org-unit">部门</option>
            <option value="org-person">人员</option>
            <option value="org-role">岗位</option>
        </select>
        <select id="operationLogActionSelect" class="settings-input">
            <option value="">全部动作</option>
            <option value="create">新增</option>
            <option value="update">修改</option>
            <option value="delete">删除</option>
            <option value="save-config">保存配置</option>
            <option value="assign-person">分配人员</option>
            <option value="save-permission">设置权限</option>
            <option value="refresh-permission">刷新权限</option>
        </select>
        <select id="operationLogSuccessSelect" class="settings-input">
            <option value="">全部结果</option>
            <option value="true">成功</option>
            <option value="false">失败</option>
        </select>
        <button id="refreshOperationLogsButton" class="settings-action settings-action--compact" type="button">刷新</button>
    </div>
    <div id="operationLogTimeRangePicker" class="settings-time-range-picker" hidden></div>
    <div id="operationLogTable" class="settings-log-table">
        <div class="settings-empty">正在加载操作日志。</div>
    </div>
    <div id="operationLogPageCards" class="settings-log-page-cards" aria-label="操作日志分页"></div>
</div>
```

如果登录日志筛选区实际 class 名不同，以现有登录日志 HTML 为准，保持视觉一致。

- [ ] **Step 2: JS 新增状态和查询参数**

在 `org-settings.js` 中参考 `loginLogState` 新增：

```javascript
let operationLogState = {
    page: 1,
    pageSize: 10,
    total: 0,
    startTime: "",
    endTime: "",
    rangePickerLeftMonth: new Date(),
    rangePickerRightMonth: new Date(new Date().getFullYear(), new Date().getMonth() + 1, 1),
    rangePickerMode: "date",
    rangePickerPanelIndex: 0,
    showRangeTime: false
};
```

新增 `collectOperationLogFilters()`，构造 `/api/logs/operations` 查询参数，字段为 `keyword`、`moduleCode`、`actionCode`、`success`、`startTime`、`endTime`、`page`、`pageSize`。

- [ ] **Step 3: JS 渲染操作日志表格**

新增 `renderOperationLogs(pageData)`，表格列为：

```text
时间 / 操作人 / 模块 / 动作 / 对象 / 结果 / 摘要
```

每行加一个“展开”按钮，点击后在下一行显示：

```text
明细、IP、请求路径、User-Agent
```

所有动态文本用现有 `escapeHtml`。

- [ ] **Step 4: JS 权限控制**

修正或确认权限判断：

```javascript
function canViewOperationLog() {
    return hasOrgPermission("general.log.operation") && hasOrgPermission("general.log.operation.view");
}
```

没有权限时隐藏“操作日志”页签，不调用 `/api/logs/operations`。

- [ ] **Step 5: JS 事件绑定**

参考登录日志绑定：

- 点击“刷新”调用 `queryOperationLogsFromFilters()`。
- 关键词回车查询。
- 模块、动作、结果 select change 查询。
- 时间范围选择器复用登录日志现有逻辑；如果复用成本过高，先抽一个通用方法，避免复制出两套行为不一致的时间选择器。
- 页码和 pageSize 行为与登录日志一致。

- [ ] **Step 6: 更新静态版本并同步 target/classes**

更新 `chat.html` 中 `org-settings.js`、`org-settings.css` 或相关资源版本号，例如 `20260601-operation-log1`。

同步静态资源：

```powershell
rtk proxy powershell -NoProfile -Command "Copy-Item -LiteralPath 'modules/hephaestus-org/src/main/resources/static/org-settings-panel.html' -Destination 'modules/hephaestus-org/target/classes/static/org-settings-panel.html' -Force; Copy-Item -LiteralPath 'modules/hephaestus-org/src/main/resources/static/org-settings.js' -Destination 'modules/hephaestus-org/target/classes/static/org-settings.js' -Force; Copy-Item -LiteralPath 'modules/hephaestus-org/src/main/resources/static/org-settings.css' -Destination 'modules/hephaestus-org/target/classes/static/org-settings.css' -Force; Copy-Item -LiteralPath 'modules/hephaestus-app/src/main/resources/static/chat.html' -Destination 'modules/hephaestus-app/target/classes/static/chat.html' -Force"
```

- [ ] **Step 7: 前端语法验证**

Run:

```powershell
rtk proxy powershell -NoProfile -Command "node --check 'modules/hephaestus-org/src/main/resources/static/org-settings.js'"
```

Expected: 无语法错误。

## Task 7: 集成验证

**Files:**
- No new files.

- [ ] **Step 1: 后端编译**

Run:

```powershell
rtk proxy powershell -NoProfile -Command "mvn -pl modules/hephaestus-app -am -DskipTests compile"
```

Expected: 编译通过。

- [ ] **Step 2: 浏览器验证管理员可查询**

使用 `alice / 1234567` 登录 `http://127.0.0.1:11018/hephaestus/chat.html`。

执行一个配置保存或岗位保存操作，然后打开：

```text
设置 -> 日志 -> 操作日志
```

Expected:

- 操作日志页签可见。
- 筛选结构与登录日志风格一致。
- 列表出现刚才的操作记录。
- 展开后能看到中文明细。

- [ ] **Step 3: 浏览器验证权限隐藏**

使用没有 `general.log.operation` 或没有 `general.log.operation.view` 的账号登录。

Expected:

- 没有 `general.log.operation` 时不显示“操作日志”页签。
- 没有 `general.log.operation.view` 时不调用 `/api/logs/operations`。
- 直接请求接口返回无权限。

- [ ] **Step 4: 检查乱码**

在浏览器中检查：

- 日志栏目标题。
- 登录日志筛选项。
- 操作日志筛选项。
- 表格列名。
- 空状态、加载状态、错误提示。

Expected: 中文显示正常，没有乱码。

## Self-Review

- Spec coverage: 覆盖了独立操作日志表、结构化字段、中文摘要和明细、权限控制、前端查询显示、查询结构参考登录日志、写入失败不阻断主业务、只追加 Liquibase changeset。
- Placeholder scan: 本计划没有 TBD/TODO；每个任务包含文件、步骤和验证命令。
- Type consistency: DTO、Service、Controller、Repository 使用 `OperationLog*` 命名；权限统一使用 `OrgPermissionCodes.GENERAL_LOG_OPERATION_VIEW`；前端接口统一为 `/api/logs/operations`。
