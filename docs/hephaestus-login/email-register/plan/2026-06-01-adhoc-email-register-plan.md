# 邮箱注册与验证码重置密码 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现登录页内注册入口、邮箱验证码注册、邮箱验证码重置密码、每邮箱最多三个账号、注册人员归属配置部门和人员来源字段。

**Architecture:** 后端在 `hephaestus-login` 新增 `login.register` 领域，复用现有人员表和 `OrgPersonRepository` 完成账号落库；验证码使用数据库表保存哈希和状态，邮件通过 `JavaMailSender` 发送。前端在 `login.html/login.css/login.js` 内实现登录、注册和忘记密码切换，复用当前登录公共配置和 RSA 加密逻辑。

**Tech Stack:** Java 17、Spring Boot 3、MyBatis、Liquibase、Spring Mail、JUnit 5、Mockito、原生 HTML/CSS/JS。

---

## File Structure

### 新增文件

- `modules/hephaestus-login/src/main/java/olympus/hephaestus/login/register/controller/EmailRegisterController.java`
  - 提供发送验证码、注册、重置密码接口。
- `modules/hephaestus-login/src/main/java/olympus/hephaestus/login/register/service/EmailRegisterService.java`
  - 定义邮箱注册和重置密码服务接口。
- `modules/hephaestus-login/src/main/java/olympus/hephaestus/login/register/service/impl/EmailRegisterServiceImpl.java`
  - 实现注册、验证码发送、验证码校验和重置密码流程。
- `modules/hephaestus-login/src/main/java/olympus/hephaestus/login/register/domain/EmailVerificationCodeEntity.java`
  - 映射 `sys_email_verification_code` 表。
- `modules/hephaestus-login/src/main/java/olympus/hephaestus/login/register/domain/EmailVerificationScene.java`
  - 定义 `REGISTER`、`RESET_PASSWORD` 场景。
- `modules/hephaestus-login/src/main/java/olympus/hephaestus/login/register/dto/SendEmailCodeRequest.java`
  - 发送验证码请求。
- `modules/hephaestus-login/src/main/java/olympus/hephaestus/login/register/dto/RegisterRequest.java`
  - 注册请求。
- `modules/hephaestus-login/src/main/java/olympus/hephaestus/login/register/dto/ResetPasswordRequest.java`
  - 重置密码请求。
- `modules/hephaestus-login/src/main/java/olympus/hephaestus/login/register/dto/EmailRegisterResponse.java`
  - 通用响应。
- `modules/hephaestus-login/src/main/java/olympus/hephaestus/login/register/repository/EmailVerificationCodeRepository.java`
  - 验证码表 MyBatis 仓储。
- `modules/hephaestus-login/src/main/java/olympus/hephaestus/login/register/support/EmailCodeGenerator.java`
  - 生成 6 位数字验证码。
- `modules/hephaestus-login/src/main/java/olympus/hephaestus/login/register/support/EmailCodeHasher.java`
  - 使用 SHA-256 哈希验证码。
- `modules/hephaestus-login/src/main/java/olympus/hephaestus/login/register/support/RegisterMailSender.java`
  - 包装 `JavaMailSender`，发送中文验证码邮件。
- `modules/hephaestus-login/src/test/java/olympus/hephaestus/login/register/service/EmailRegisterServiceTest.java`
  - 服务层 TDD 测试。
- `modules/hephaestus-login/src/main/resources/static/login.html`
  - 登录页内注册入口面。
- `modules/hephaestus-login/src/main/resources/static/login.css`
  - 注册页样式。
- `modules/hephaestus-login/src/main/resources/static/login.js`
  - 注册页交互、验证码发送、注册和重置密码。

### 修改文件

- `modules/liquibase/src/main/resources/db/changelog/db.changelog.xml`
  - 追加 `source_type` 字段、验证码表、配置项和索引 changeset。
- `modules/hephaestus-org/src/main/java/olympus/hephaestus/org/entity/OrgPersonEntity.java`
  - 增加 `sourceType` 属性。
- `modules/hephaestus-org/src/main/java/olympus/hephaestus/org/domain/OrgPersonSummary.java`
  - 增加人员来源字段。
- `modules/hephaestus-org/src/main/java/olympus/hephaestus/org/domain/OrgPersonListRow.java`
  - 增加人员来源字段。
- `modules/hephaestus-org/src/main/java/olympus/hephaestus/org/repository/OrgPersonRepository.java`
  - 查询补充 `source_type`，新增按邮箱统计、按邮箱用户名查询、更新密码方法。
- `modules/hephaestus-org/src/main/java/olympus/hephaestus/org/service/OrgPersonService.java`
  - 管理员新增人员默认 `ADMIN`，摘要返回来源字段。
- `modules/hephaestus-login/src/main/java/olympus/hephaestus/login/config/LoginConfigConst.java`
  - 增加注册部门和验证码配置 key。
- `modules/hephaestus-login/src/main/java/olympus/hephaestus/login/auth/support/LoginRequiredInterceptor.java`
  - fallback 白名单补充注册页与注册接口。
- `modules/hephaestus-app/src/main/resources/application.yml`
  - 配置白名单补充注册页与注册接口。
- `modules/hephaestus-login/src/main/resources/static/login.html`
  - 增加注册和忘记密码入口，更新 `login.css/login.js` 版本号。
- `modules/hephaestus-login/src/main/resources/static/login.css`
  - 登录页右上角入口样式。
- `modules/hephaestus-login/src/main/resources/static/login.js`
  - 只补充跳转交互，不改变登录加密主逻辑。

---

## Task 1: Liquibase 数据结构和配置项

**Files:**
- Modify: `modules/liquibase/src/main/resources/db/changelog/db.changelog.xml`

- [ ] **Step 1: 追加人员来源字段 changeset**

在文件末尾 `</databaseChangeLog>` 前追加：

```xml
    <changeSet id="email-register-person-source-type-20260601" author="codex">
        <preConditions onFail="MARK_RAN">
            <tableExists tableName="heph_person"/>
            <not>
                <columnExists tableName="heph_person" columnName="source_type"/>
            </not>
        </preConditions>
        <addColumn tableName="heph_person">
            <column name="source_type" type="varchar(32)" defaultValue="ADMIN">
                <constraints nullable="false"/>
            </column>
        </addColumn>
        <createIndex tableName="heph_person" indexName="idx_heph_person_email">
            <column name="email"/>
        </createIndex>
        <createIndex tableName="heph_person" indexName="idx_heph_person_source_type">
            <column name="source_type"/>
        </createIndex>
    </changeSet>
```

- [ ] **Step 2: 追加邮箱验证码表 changeset**

继续追加：

```xml
    <changeSet id="email-verification-code-create-table-20260601" author="codex">
        <preConditions onFail="MARK_RAN">
            <not>
                <tableExists tableName="sys_email_verification_code"/>
            </not>
        </preConditions>
        <createTable tableName="sys_email_verification_code">
            <column name="id" type="bigint" autoIncrement="true">
                <constraints nullable="false" primaryKey="true" primaryKeyName="pk_sys_email_verification_code"/>
            </column>
            <column name="email" type="varchar(128)">
                <constraints nullable="false"/>
            </column>
            <column name="scene" type="varchar(32)">
                <constraints nullable="false"/>
            </column>
            <column name="code_hash" type="varchar(128)">
                <constraints nullable="false"/>
            </column>
            <column name="expire_at" type="timestamp">
                <constraints nullable="false"/>
            </column>
            <column name="used_at" type="timestamp"/>
            <column name="send_ip" type="varchar(64)"/>
            <column name="created_at" type="timestamp" defaultValueComputed="CURRENT_TIMESTAMP"/>
        </createTable>
        <createIndex tableName="sys_email_verification_code" indexName="idx_email_code_scene_email_time">
            <column name="scene"/>
            <column name="email"/>
            <column name="created_at"/>
        </createIndex>
        <createIndex tableName="sys_email_verification_code" indexName="idx_email_code_expire_at">
            <column name="expire_at"/>
        </createIndex>
    </changeSet>
```

- [ ] **Step 3: 追加系统配置 changeset**

继续追加 `login.register.unit-id`、`login.email-code.expire-minutes`、`login.email-code.resend-seconds`。配置放在 `main-system` 分组、`login` tab、`login` section，`register.unit-id` 不公开，验证码时长可不公开。

- [ ] **Step 4: 检查 XML 结构**

Run:

```bash
rtk proxy powershell -NoProfile -Command "Select-String -Path 'modules/liquibase/src/main/resources/db/changelog/db.changelog.xml' -Pattern 'email-register-person-source-type-20260601|email-verification-code-create-table-20260601|login.register.unit-id'"
```

Expected: 三类新增内容均能查到，且文件只有一个 `</databaseChangeLog>`。

---

## Task 2: 人员来源字段接入组织模型

**Files:**
- Modify: `modules/hephaestus-org/src/main/java/olympus/hephaestus/org/entity/OrgPersonEntity.java`
- Modify: `modules/hephaestus-org/src/main/java/olympus/hephaestus/org/domain/OrgPersonSummary.java`
- Modify: `modules/hephaestus-org/src/main/java/olympus/hephaestus/org/domain/OrgPersonListRow.java`
- Modify: `modules/hephaestus-org/src/main/java/olympus/hephaestus/org/repository/OrgPersonRepository.java`
- Modify: `modules/hephaestus-org/src/main/java/olympus/hephaestus/org/service/OrgPersonService.java`

- [ ] **Step 1: 增加实体字段**

在 `OrgPersonEntity` 的 `email` 后增加：

```java
    @TableField("source_type")
    private String sourceType;
```

并增加 getter/setter：

```java
    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }
```

- [ ] **Step 2: 扩展摘要 record**

在 `OrgPersonSummary` 和 `OrgPersonListRow` 中，在 `email` 后增加：

```java
        String sourceType,
```

- [ ] **Step 3: 补充 SQL 查询字段**

在 `OrgPersonRepository` 所有查询人员字段列表中补充：

```sql
                   source_type AS sourceType,
```

涉及 `findByScope`、`findListRowsByCurrentScope`、`getByPersonCode`、`getByUsername`。

- [ ] **Step 4: 新增注册所需仓储方法**

在 `OrgPersonRepository` 中增加：

```java
    @Select("SELECT COUNT(1) FROM heph_person WHERE email = #{email}")
    long countByEmail(@Param("email") String email);

    @Select("""
            SELECT id,
                   person_code AS personCode,
                   person_name AS personName,
                   username,
                   password,
                   unit_id AS unitId,
                   avatar_media_id AS avatarMediaId,
                   mobile,
                   email,
                   source_type AS sourceType,
                   remark,
                   enabled,
                   created_at AS createdAt,
                   updated_at AS updatedAt
            FROM heph_person
            WHERE email = #{email}
              AND username = #{username}
            """)
    OrgPersonEntity getByEmailAndUsername(@Param("email") String email, @Param("username") String username);

    @Update("UPDATE heph_person SET password = #{password}, updated_at = CURRENT_TIMESTAMP WHERE id = #{id}")
    int updatePassword(@Param("id") Long id, @Param("password") String password);
```

- [ ] **Step 5: 管理员新增默认来源**

在 `OrgPersonService.createPerson` 设置邮箱后增加：

```java
        entity.setSourceType("ADMIN");
```

- [ ] **Step 6: 摘要返回来源**

在 `toSummary(OrgPersonEntity...)` 和 `toSummary(OrgPersonListRow...)` 构造 `OrgPersonSummary` 时，把 `sourceType` 放到 `email` 之后。

- [ ] **Step 7: 编译组织模块**

Run:

```bash
rtk proxy cmd /c "set JAVA_HOME=D:\Program Files\JetBrains\IntelliJ IDEA 2025.2.1\jbr&& set PATH=D:\Program Files\JetBrains\IntelliJ IDEA 2025.2.1\jbr\bin;%PATH%&& mvn -pl modules/hephaestus-org -am -DskipTests compile"
```

Expected: `BUILD SUCCESS`。

---

## Task 3: 邮箱验证码领域和工具

**Files:**
- Create: `modules/hephaestus-login/src/main/java/olympus/hephaestus/login/register/domain/EmailVerificationScene.java`
- Create: `modules/hephaestus-login/src/main/java/olympus/hephaestus/login/register/domain/EmailVerificationCodeEntity.java`
- Create: `modules/hephaestus-login/src/main/java/olympus/hephaestus/login/register/support/EmailCodeGenerator.java`
- Create: `modules/hephaestus-login/src/main/java/olympus/hephaestus/login/register/support/EmailCodeHasher.java`
- Create: `modules/hephaestus-login/src/main/java/olympus/hephaestus/login/register/support/RegisterMailSender.java`

- [ ] **Step 1: 创建验证码场景枚举**

```java
package olympus.hephaestus.login.register.domain;

public enum EmailVerificationScene {
    REGISTER,
    RESET_PASSWORD
}
```

- [ ] **Step 2: 创建验证码实体**

```java
package olympus.hephaestus.login.register.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("sys_email_verification_code")
public class EmailVerificationCodeEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("email")
    private String email;

    @TableField("scene")
    private String scene;

    @TableField("code_hash")
    private String codeHash;

    @TableField("expire_at")
    private LocalDateTime expireAt;

    @TableField("used_at")
    private LocalDateTime usedAt;

    @TableField("send_ip")
    private String sendIp;

    @TableField("created_at")
    private LocalDateTime createdAt;

    // 生成标准 getter/setter。
}
```

- [ ] **Step 3: 创建 6 位验证码生成器**

```java
package olympus.hephaestus.login.register.support;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class EmailCodeGenerator {

    private final SecureRandom random = new SecureRandom();

    public String generate() {
        return String.format("%06d", random.nextInt(1_000_000));
    }
}
```

- [ ] **Step 4: 创建 SHA-256 哈希工具**

```java
package olympus.hephaestus.login.register.support;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Component
public class EmailCodeHasher {

    public String hash(String email, String scene, String code) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest((normalize(email) + ":" + scene + ":" + code).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("当前运行环境不支持 SHA-256", exception);
        }
    }

    private String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }
}
```

- [ ] **Step 5: 创建邮件发送组件**

```java
package olympus.hephaestus.login.register.support;

import olympus.hephaestus.weather.tools.EmailProperties;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class RegisterMailSender {

    private final JavaMailSender mailSender;
    private final EmailProperties emailProperties;

    public RegisterMailSender(JavaMailSender mailSender, EmailProperties emailProperties) {
        this.mailSender = mailSender;
        this.emailProperties = emailProperties;
    }

    public void sendCode(String email, String title, String code, int expireMinutes) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(resolveFromAddress());
        message.setTo(email);
        message.setSubject(title);
        message.setText("您的验证码是：" + code + "，" + expireMinutes + " 分钟内有效。若非本人操作，请忽略本邮件。");
        mailSender.send(message);
    }

    private String resolveFromAddress() {
        if (!StringUtils.hasText(emailProperties.getFromAddress())) {
            throw new IllegalStateException("未配置发件人邮箱地址 app.mail.from-address");
        }
        return emailProperties.getFromAddress().trim();
    }
}
```

- [ ] **Step 6: 编译登录模块**

Run:

```bash
rtk proxy cmd /c "set JAVA_HOME=D:\Program Files\JetBrains\IntelliJ IDEA 2025.2.1\jbr&& set PATH=D:\Program Files\JetBrains\IntelliJ IDEA 2025.2.1\jbr\bin;%PATH%&& mvn -pl modules/hephaestus-login -am -DskipTests compile"
```

Expected: `BUILD SUCCESS`。

---

## Task 4: 验证码仓储和服务 TDD

**Files:**
- Create: `modules/hephaestus-login/src/main/java/olympus/hephaestus/login/register/repository/EmailVerificationCodeRepository.java`
- Create: `modules/hephaestus-login/src/test/java/olympus/hephaestus/login/register/service/EmailRegisterServiceTest.java`

- [ ] **Step 1: 创建验证码仓储**

```java
package olympus.hephaestus.login.register.repository;

import olympus.hephaestus.login.register.domain.EmailVerificationCodeEntity;
import olympus.hephaestus.mybatis.repository.BaseAbstractRepository;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface EmailVerificationCodeRepository extends BaseAbstractRepository<EmailVerificationCodeEntity, Long> {

    @Select("""
            SELECT id,
                   email,
                   scene,
                   code_hash AS codeHash,
                   expire_at AS expireAt,
                   used_at AS usedAt,
                   send_ip AS sendIp,
                   created_at AS createdAt
            FROM sys_email_verification_code
            WHERE email = #{email}
              AND scene = #{scene}
            ORDER BY created_at DESC, id DESC
            LIMIT 1
            """)
    EmailVerificationCodeEntity findLatest(@Param("email") String email, @Param("scene") String scene);

    @Update("UPDATE sys_email_verification_code SET used_at = CURRENT_TIMESTAMP WHERE id = #{id} AND used_at IS NULL")
    int markUsed(@Param("id") Long id);
}
```

- [ ] **Step 2: 编写第一个失败测试：同邮箱最多三个账号**

在 `EmailRegisterServiceTest` 写 Mockito 测试，构造 `countByEmail` 返回 3，调用注册应抛出异常。

核心断言：

```java
assertThatThrownBy(() -> service.register(request))
        .isInstanceOf(LoginException.class)
        .hasMessageContaining("同一邮箱最多注册三个账号");
verify(orgPersonRepository, never()).save(any());
```

- [ ] **Step 3: 运行测试确认失败**

Run:

```bash
rtk proxy cmd /c "set JAVA_HOME=D:\Program Files\JetBrains\IntelliJ IDEA 2025.2.1\jbr&& set PATH=D:\Program Files\JetBrains\IntelliJ IDEA 2025.2.1\jbr\bin;%PATH%&& mvn -pl modules/hephaestus-login -Dtest=EmailRegisterServiceTest test"
```

Expected: FAIL，原因是 `EmailRegisterServiceImpl` 尚不存在。

---

## Task 5: 注册服务最小实现

**Files:**
- Create: `modules/hephaestus-login/src/main/java/olympus/hephaestus/login/register/dto/SendEmailCodeRequest.java`
- Create: `modules/hephaestus-login/src/main/java/olympus/hephaestus/login/register/dto/RegisterRequest.java`
- Create: `modules/hephaestus-login/src/main/java/olympus/hephaestus/login/register/dto/ResetPasswordRequest.java`
- Create: `modules/hephaestus-login/src/main/java/olympus/hephaestus/login/register/dto/EmailRegisterResponse.java`
- Create: `modules/hephaestus-login/src/main/java/olympus/hephaestus/login/register/service/EmailRegisterService.java`
- Create: `modules/hephaestus-login/src/main/java/olympus/hephaestus/login/register/service/impl/EmailRegisterServiceImpl.java`
- Modify: `modules/hephaestus-login/src/main/java/olympus/hephaestus/login/config/LoginConfigConst.java`

- [ ] **Step 1: 增加配置常量**

在 `LoginConfigConst` 增加：

```java
    public static final String REGISTER_UNIT_ID = "login.register.unit-id";
    public static final String EMAIL_CODE_EXPIRE_MINUTES = "login.email-code.expire-minutes";
    public static final String EMAIL_CODE_RESEND_SECONDS = "login.email-code.resend-seconds";
```

- [ ] **Step 2: 创建 DTO records**

```java
public record SendEmailCodeRequest(String email) {}
public record RegisterRequest(String email, String username, String personName, String password, String confirmPassword, String code, boolean encrypted) {}
public record ResetPasswordRequest(String email, String username, String password, String confirmPassword, String code, boolean encrypted) {}
public record EmailRegisterResponse(boolean success, String message) {}
```

- [ ] **Step 3: 创建服务接口**

```java
public interface EmailRegisterService {
    EmailRegisterResponse sendRegisterCode(SendEmailCodeRequest request, String clientIp);
    EmailRegisterResponse sendResetPasswordCode(SendEmailCodeRequest request, String clientIp);
    EmailRegisterResponse register(RegisterRequest request);
    EmailRegisterResponse resetPassword(ResetPasswordRequest request);
}
```

- [ ] **Step 4: 实现注册主流程**

`EmailRegisterServiceImpl.register` 必须按顺序：

1. 校验邮箱、用户名、姓名、密码、确认密码、验证码。
2. 解密密码，解密逻辑复用 `RsaPasswordCryptoService` 和 `LoginConfigConst.PASSWORD_ENCRYPT_ENABLED`。
3. `orgPersonRepository.countByEmail(email)` 大于等于 3 时抛出 `LoginException("同一邮箱最多注册三个账号")`。
4. `orgPersonRepository.getByUsername(username)` 非空时抛出 `LoginException("用户名已存在")`。
5. 校验 `REGISTER` 验证码。
6. 读取 `login.register.unit-id`，部门不存在时抛出 `LoginException("注册部门不存在，请联系管理员")`。
7. 保存 `OrgPersonEntity`，字段包括 `sourceType = "REGISTER"`。
8. 标记验证码已使用。

- [ ] **Step 5: 运行第一个测试确认通过**

Run:

```bash
rtk proxy cmd /c "set JAVA_HOME=D:\Program Files\JetBrains\IntelliJ IDEA 2025.2.1\jbr&& set PATH=D:\Program Files\JetBrains\IntelliJ IDEA 2025.2.1\jbr\bin;%PATH%&& mvn -pl modules/hephaestus-login -Dtest=EmailRegisterServiceTest test"
```

Expected: `BUILD SUCCESS`。

---

## Task 6: 补齐验证码发送和重置密码测试

**Files:**
- Modify: `modules/hephaestus-login/src/test/java/olympus/hephaestus/login/register/service/EmailRegisterServiceTest.java`
- Modify: `modules/hephaestus-login/src/main/java/olympus/hephaestus/login/register/service/impl/EmailRegisterServiceImpl.java`

- [ ] **Step 1: 增加发送频率测试**

测试 `findLatest` 返回 30 秒前创建且未过期的验证码，`sendRegisterCode` 应抛出 `LoginException("验证码发送太频繁，请稍后再试")`。

- [ ] **Step 2: 增加注册成功测试**

测试验证码正确、邮箱账号数为 2、用户名不存在、部门存在时，保存人员，断言：

```java
assertThat(savedPerson.getSourceType()).isEqualTo("REGISTER");
assertThat(savedPerson.getUnitId()).isEqualTo(2L);
assertThat(savedPerson.getEmail()).isEqualTo("new@example.com");
verify(emailVerificationCodeRepository).markUsed(codeEntity.getId());
```

- [ ] **Step 3: 增加重置密码成功测试**

测试 `getByEmailAndUsername` 返回人员，验证码正确后调用 `updatePassword(personId, newPassword)`，并标记验证码已使用。

- [ ] **Step 4: 实现缺失逻辑**

补齐 `sendRegisterCode`、`sendResetPasswordCode`、`resetPassword`、验证码校验、邮箱格式校验、密码确认校验、验证码哈希比较。

- [ ] **Step 5: 运行服务测试**

Run:

```bash
rtk proxy cmd /c "set JAVA_HOME=D:\Program Files\JetBrains\IntelliJ IDEA 2025.2.1\jbr&& set PATH=D:\Program Files\JetBrains\IntelliJ IDEA 2025.2.1\jbr\bin;%PATH%&& mvn -pl modules/hephaestus-login -Dtest=EmailRegisterServiceTest test"
```

Expected: `BUILD SUCCESS`。

---

## Task 7: Controller 和白名单

**Files:**
- Create: `modules/hephaestus-login/src/main/java/olympus/hephaestus/login/register/controller/EmailRegisterController.java`
- Modify: `modules/hephaestus-login/src/main/java/olympus/hephaestus/login/auth/support/LoginRequiredInterceptor.java`
- Modify: `modules/hephaestus-app/src/main/resources/application.yml`

- [ ] **Step 1: 创建 Controller**

Controller 映射：

```java
@RestController
@RequestMapping("/auth")
public class EmailRegisterController {
    @PostMapping("/email-code/register")
    public EmailRegisterResponse sendRegisterCode(@RequestBody SendEmailCodeRequest request, HttpServletRequest servletRequest) { ... }

    @PostMapping("/email-code/reset-password")
    public EmailRegisterResponse sendResetPasswordCode(@RequestBody SendEmailCodeRequest request, HttpServletRequest servletRequest) { ... }

    @PostMapping("/register")
    public EmailRegisterResponse register(@RequestBody RegisterRequest request) { ... }

    @PostMapping("/reset-password")
    public EmailRegisterResponse resetPassword(@RequestBody ResetPasswordRequest request) { ... }
}
```

- [ ] **Step 2: fallback 白名单增加路径**

在 `LoginRequiredInterceptor.FALLBACK_WHITELIST` 增加：

```java
            "/login.html",
            "/login.css",
            "/login.js",
            "/auth/email-code/register",
            "/auth/email-code/reset-password",
            "/auth/register",
            "/auth/reset-password",
```

- [ ] **Step 3: application.yml 白名单增加路径**

在 `hephaestus.auth.whitelist.paths` 增加同样 7 个路径。

- [ ] **Step 4: 编译登录和 app 模块**

Run:

```bash
rtk proxy cmd /c "set JAVA_HOME=D:\Program Files\JetBrains\IntelliJ IDEA 2025.2.1\jbr&& set PATH=D:\Program Files\JetBrains\IntelliJ IDEA 2025.2.1\jbr\bin;%PATH%&& mvn -pl modules/hephaestus-login,modules/hephaestus-app -am -DskipTests compile"
```

Expected: `BUILD SUCCESS`。

---

## Task 8: 登录页内注册与重置入口

**Files:**
- Create: `modules/hephaestus-login/src/main/resources/static/login.html`
- Create: `modules/hephaestus-login/src/main/resources/static/login.css`
- Create: `modules/hephaestus-login/src/main/resources/static/login.js`
- Modify: `modules/hephaestus-login/src/main/resources/static/login.html`
- Modify: `modules/hephaestus-login/src/main/resources/static/login.css`
- Modify: `modules/hephaestus-login/src/main/resources/static/login.js`

- [ ] **Step 1: 改造 login.html**

页面包含：

- 右上角“登录”“注册”切换按钮。
- 注册表单：姓名、用户名、邮箱、验证码、密码、确认密码。
- 重置密码表单：用户名、邮箱、验证码、新密码、确认密码。
- 验证码按钮。
- 错误提示区和成功提示区。
- 引用 `webjars/jsencrypt` 和 `login.js`。

- [ ] **Step 2: 改造 login.css**

实现：

- 全屏暗色背景。
- 半透明居中表单。
- 右上角小号胶囊按钮。
- 移动端表单宽度自适应。
- 不使用过大的营销式 hero 卡片。

- [ ] **Step 3: 改造 login.js**

实现：

- 加载 `/api/system-config/public/main-system`。
- 复用登录页 RSA 加密策略：`RSA_PKCS1` 使用 JSEncrypt，`RSA_OAEP_SHA256` 使用 WebCrypto。
- 注册表单前端必填、邮箱格式、密码确认校验。
- 发送验证码后按钮 60 秒倒计时。
- 调用 `/auth/email-code/register`、`/auth/register`、`/auth/email-code/reset-password`、`/auth/reset-password`。

- [ ] **Step 4: 登录页增加入口**

在 `login.html` 增加右上角链接：

```html
<nav class="login-actions" aria-label="登录操作">
    <a href="./login.html">登录</a>
    <a href="./login.html">注册</a>
</nav>
```

在登录表单下增加：

```html
<a class="login-forgot-link" href="./login.html?mode=reset">忘记密码？</a>
```

- [ ] **Step 5: 前端语法检查**

Run:

```bash
rtk proxy node --check modules/hephaestus-login/src/main/resources/static/login.js
rtk proxy node --check modules/hephaestus-login/src/main/resources/static/login.js
```

Expected: 两条命令均无语法错误。

---

## Task 9: 全量验证和浏览器 QA

**Files:**
- Verify only.

- [ ] **Step 1: Java 编译**

Run:

```bash
rtk proxy cmd /c "set JAVA_HOME=D:\Program Files\JetBrains\IntelliJ IDEA 2025.2.1\jbr&& set PATH=D:\Program Files\JetBrains\IntelliJ IDEA 2025.2.1\jbr\bin;%PATH%&& mvn -pl modules/hephaestus-login,modules/hephaestus-org,modules/hephaestus-app -am -DskipTests compile"
```

Expected: `BUILD SUCCESS`。

- [ ] **Step 2: 注册服务测试**

Run:

```bash
rtk proxy cmd /c "set JAVA_HOME=D:\Program Files\JetBrains\IntelliJ IDEA 2025.2.1\jbr&& set PATH=D:\Program Files\JetBrains\IntelliJ IDEA 2025.2.1\jbr\bin;%PATH%&& mvn -pl modules/hephaestus-login -Dtest=EmailRegisterServiceTest test"
```

Expected: `BUILD SUCCESS`。

- [ ] **Step 3: 静态资源同步**

如果 11018 服务已运行且未重启，将新增或修改的静态文件同步到：

```text
modules/hephaestus-app/target/classes/static
```

- [ ] **Step 4: browser-qa 验证注册页**

打开：

```text
http://localhost:11018/hephaestus/login.html
```

检查：

- 页面无明显错位。
- 注册/重置密码切换正常。
- 邮箱格式错误会阻止提交。
- 密码确认不一致会阻止提交。
- 登录按钮返回 `login.html`。

- [ ] **Step 5: 手工接口验证**

如 SMTP 可用：

1. 发送注册验证码。
2. 用验证码注册账号。
3. 第 4 次同邮箱注册失败。
4. 发送重置密码验证码。
5. 重置密码。
6. 使用新密码登录。

如 SMTP 不可用：

1. 通过服务测试覆盖验证码业务规则。
2. 浏览器验证邮件发送失败提示为中文且不崩溃。

---

## Self-Review

- PRD 中的邮箱注册、验证码重置、每邮箱最多三个账号、注册部门配置、人员来源字段、白名单、独立前端页都已映射到任务。
- 计划没有使用 `TODO`、`TBD` 作为待填内容。
- 数据库字段名统一为 `source_type`，Java 属性统一为 `sourceType`。
- 验证码场景统一为 `REGISTER` 和 `RESET_PASSWORD`。
- 执行阶段必须遵循 TDD：Task 4 先写失败测试，再实现 Task 5 和 Task 6。




