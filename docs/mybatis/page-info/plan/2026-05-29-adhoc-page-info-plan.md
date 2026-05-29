# MyBatis PageInfo 查询逻辑迁移 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在 `modules/mybatis` 增加最小通用分页能力，并把登录日志分页查询改造成复用该能力，保持现有接口字段结构不变。

**Architecture:** 采用“公共分页对象 + 业务仓储显式 count/list SQL”方案。`modules/mybatis` 只承载 `PageQuery`、`PageInfo`、`PageSupport` 三个最小分页类，`modules/hephaestus-login` 保持现有仓储 SQL 形式，只把服务层手写分页算法替换为公共支持。

**Tech Stack:** Java 17、Spring Boot 3、MyBatis-Plus 3.5.7、JUnit 5、Maven

---

## File Structure

### Create

- `modules/mybatis/src/main/java/com/example/springaidemo/mybatis/page/PageQuery.java`
  - 统一承载分页参数、默认值、页大小上限和 `offset` 计算。
- `modules/mybatis/src/main/java/com/example/springaidemo/mybatis/page/PageInfo.java`
  - 统一承载分页结果 `items/page/pageSize/total`。
- `modules/mybatis/src/main/java/com/example/springaidemo/mybatis/page/PageSupport.java`
  - 提供分页参数归一化与 `PageInfo` 组装静态方法。
- `modules/mybatis/src/test/java/com/example/springaidemo/mybatis/page/PageSupportTest.java`
  - 覆盖公共分页逻辑的最小单元测试。

### Modify

- `modules/hephaestus-login/src/main/java/com/example/springaidemo/login/log/service/impl/LoginLogServiceImpl.java`
  - 去掉手写分页算法，改为调用 `PageSupport`。
- `modules/hephaestus-login/src/main/java/com/example/springaidemo/login/log/dto/LoginLogPageResponse.java`
  - 增加从公共 `PageInfo<LoginLogResponse>` 转换的入口，保持业务 DTO 稳定。

### Optional Test

- `modules/hephaestus-login/src/test/java/com/example/springaidemo/login/log/service/LoginLogServiceImplTest.java`
  - 如果模块已有可复用测试基础，则补一条服务层兼容测试；若当前测试基建不足，可延后。

## Task 1: 公共分页对象测试先行

**Files:**

- Create: `modules/mybatis/src/test/java/com/example/springaidemo/mybatis/page/PageSupportTest.java`

- [ ] **Step 1: 写第一条 failing test，验证默认分页归一化**

```java
package com.example.springaidemo.mybatis.page;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PageSupportTest {

    @Test
    void should_use_default_page_and_page_size_when_input_is_null() {
        PageQuery query = PageSupport.normalize(null, null);

        assertEquals(1, query.page());
        assertEquals(20, query.pageSize());
        assertEquals(0, query.offset());
    }
}
```

- [ ] **Step 2: 运行测试，确认它先失败**

Run:

```bash
mvn -pl modules/mybatis -Dtest=PageSupportTest test
```

Expected:

- 编译失败或测试失败，原因是 `PageQuery` / `PageSupport` 还不存在。

- [ ] **Step 3: 实现最小分页对象让测试转绿**

`modules/mybatis/src/main/java/com/example/springaidemo/mybatis/page/PageQuery.java`

```java
package com.example.springaidemo.mybatis.page;

public record PageQuery(int page, int pageSize) {

    public int offset() {
        return (page - 1) * pageSize;
    }

    public int limit() {
        return pageSize;
    }
}
```

`modules/mybatis/src/main/java/com/example/springaidemo/mybatis/page/PageInfo.java`

```java
package com.example.springaidemo.mybatis.page;

import java.util.List;

public record PageInfo<T>(
        List<T> items,
        int page,
        int pageSize,
        long total
) {
}
```

`modules/mybatis/src/main/java/com/example/springaidemo/mybatis/page/PageSupport.java`

```java
package com.example.springaidemo.mybatis.page;

import java.util.List;

public final class PageSupport {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private PageSupport() {
    }

    public static PageQuery normalize(Integer page, Integer pageSize) {
        int normalizedPage = page == null || page < 1 ? DEFAULT_PAGE : page;
        int normalizedPageSize = pageSize == null || pageSize < 1
                ? DEFAULT_PAGE_SIZE
                : Math.min(pageSize, MAX_PAGE_SIZE);
        return new PageQuery(normalizedPage, normalizedPageSize);
    }

    public static <T> PageInfo<T> pageInfo(List<T> items, long total, PageQuery query) {
        return new PageInfo<>(items, query.page(), query.pageSize(), total);
    }
}
```

- [ ] **Step 4: 再次运行测试，确认转绿**

Run:

```bash
mvn -pl modules/mybatis -Dtest=PageSupportTest test
```

Expected:

- `PageSupportTest` PASS。

- [ ] **Step 5: 提交这一小步**

```bash
git add modules/mybatis/src/main/java/com/example/springaidemo/mybatis/page/PageQuery.java modules/mybatis/src/main/java/com/example/springaidemo/mybatis/page/PageInfo.java modules/mybatis/src/main/java/com/example/springaidemo/mybatis/page/PageSupport.java modules/mybatis/src/test/java/com/example/springaidemo/mybatis/page/PageSupportTest.java
git commit -m "feat: add mybatis page support"
```

## Task 2: 补齐分页边界测试

**Files:**

- Modify: `modules/mybatis/src/test/java/com/example/springaidemo/mybatis/page/PageSupportTest.java`

- [ ] **Step 1: 新增 failing tests，覆盖页码、页大小和结果封装**

```java
@Test
void should_reset_page_to_one_when_page_is_less_than_one() {
    PageQuery query = PageSupport.normalize(0, 10);

    assertEquals(1, query.page());
    assertEquals(10, query.pageSize());
    assertEquals(0, query.offset());
}

@Test
void should_reset_page_size_to_default_when_page_size_is_less_than_one() {
    PageQuery query = PageSupport.normalize(2, 0);

    assertEquals(2, query.page());
    assertEquals(20, query.pageSize());
    assertEquals(20, query.offset());
}

@Test
void should_cap_page_size_when_page_size_exceeds_max() {
    PageQuery query = PageSupport.normalize(3, 1000);

    assertEquals(3, query.page());
    assertEquals(100, query.pageSize());
    assertEquals(200, query.offset());
}

@Test
void should_build_page_info_with_same_query_and_total() {
    PageQuery query = PageSupport.normalize(2, 10);
    PageInfo<String> pageInfo = PageSupport.pageInfo(List.of("a", "b"), 35, query);

    assertEquals(List.of("a", "b"), pageInfo.items());
    assertEquals(2, pageInfo.page());
    assertEquals(10, pageInfo.pageSize());
    assertEquals(35, pageInfo.total());
}
```

- [ ] **Step 2: 运行测试，确认至少一条先失败**

Run:

```bash
mvn -pl modules/mybatis -Dtest=PageSupportTest test
```

Expected:

- 至少一条新增断言失败，证明边界行为尚未完整覆盖。

- [ ] **Step 3: 用最小实现修正公共分页支持**

`modules/mybatis/src/main/java/com/example/springaidemo/mybatis/page/PageSupport.java`

```java
package com.example.springaidemo.mybatis.page;

import java.util.List;

public final class PageSupport {

    static final int DEFAULT_PAGE = 1;
    static final int DEFAULT_PAGE_SIZE = 20;
    static final int MAX_PAGE_SIZE = 100;

    private PageSupport() {
    }

    public static PageQuery normalize(Integer page, Integer pageSize) {
        int normalizedPage = page == null || page < 1 ? DEFAULT_PAGE : page;
        int normalizedPageSize = pageSize == null || pageSize < 1
                ? DEFAULT_PAGE_SIZE
                : Math.min(pageSize, MAX_PAGE_SIZE);
        return new PageQuery(normalizedPage, normalizedPageSize);
    }

    public static <T> PageInfo<T> pageInfo(List<T> items, long total, PageQuery query) {
        List<T> normalizedItems = items == null ? List.of() : items;
        return new PageInfo<>(normalizedItems, query.page(), query.pageSize(), total);
    }
}
```

- [ ] **Step 4: 运行测试，确认全部通过**

Run:

```bash
mvn -pl modules/mybatis -Dtest=PageSupportTest test
```

Expected:

- `PageSupportTest` 全部 PASS。

- [ ] **Step 5: 提交这一小步**

```bash
git add modules/mybatis/src/main/java/com/example/springaidemo/mybatis/page/PageSupport.java modules/mybatis/src/test/java/com/example/springaidemo/mybatis/page/PageSupportTest.java
git commit -m "test: cover mybatis page support boundaries"
```

## Task 3: 登录日志服务接入公共分页支持

**Files:**

- Modify: `modules/hephaestus-login/src/main/java/com/example/springaidemo/login/log/service/impl/LoginLogServiceImpl.java`
- Modify: `modules/hephaestus-login/src/main/java/com/example/springaidemo/login/log/dto/LoginLogPageResponse.java`

- [ ] **Step 1: 先写 failing test 或最小兼容断言**

如果已有服务层测试基础，新增如下测试：

```java
@Test
void should_keep_login_log_page_response_fields_after_page_support_migration() {
    LoginLogPageResponse response = LoginLogPageResponse.from(
            new PageInfo<>(List.of(), 2, 10, 35)
    );

    assertEquals(2, response.page());
    assertEquals(10, response.pageSize());
    assertEquals(35, response.total());
    assertEquals(List.of(), response.items());
}
```

如果当前没有可复用测试基建，则把这一步降级为先改 DTO，再靠模块编译和现有接口字段结构验证。

- [ ] **Step 2: 运行受影响测试或模块编译，确认现状未满足**

Run:

```bash
mvn -pl modules/hephaestus-login -am test
```

Expected:

- 若先加了 `from(...)` 调用测试，则因 `from(...)` 方法不存在而失败；
- 若未加测试，则以当前服务层仍存在手写分页逻辑为待改状态。

- [ ] **Step 3: 修改 DTO，增加公共分页转换入口**

`modules/hephaestus-login/src/main/java/com/example/springaidemo/login/log/dto/LoginLogPageResponse.java`

```java
package com.example.springaidemo.login.log.dto;

import com.example.springaidemo.mybatis.page.PageInfo;

import java.util.List;

public record LoginLogPageResponse(
        List<LoginLogResponse> items,
        int page,
        int pageSize,
        long total
) {

    public static LoginLogPageResponse from(PageInfo<LoginLogResponse> pageInfo) {
        return new LoginLogPageResponse(
                pageInfo.items(),
                pageInfo.page(),
                pageInfo.pageSize(),
                pageInfo.total()
        );
    }
}
```

- [ ] **Step 4: 修改登录日志服务，替换手写分页算法**

`modules/hephaestus-login/src/main/java/com/example/springaidemo/login/log/service/impl/LoginLogServiceImpl.java`

```java
import com.example.springaidemo.mybatis.page.PageInfo;
import com.example.springaidemo.mybatis.page.PageQuery;
import com.example.springaidemo.mybatis.page.PageSupport;
```

把 `query(...)` 方法中的分页部分改成：

```java
PageQuery pageQuery = PageSupport.normalize(page, pageSize);
List<LoginLogResponse> items = repository.query(
                normalize(keyword),
                normalize(operationType),
                success,
                startTime,
                endTime,
                pageQuery.limit(),
                pageQuery.offset()
        ).stream()
        .map(LoginLogResponse::from)
        .toList();
long total = repository.count(normalize(keyword), normalize(operationType), success, startTime, endTime);
PageInfo<LoginLogResponse> pageInfo = PageSupport.pageInfo(items, total, pageQuery);
return LoginLogPageResponse.from(pageInfo);
```

- [ ] **Step 5: 运行受影响测试，确认行为保持一致**

Run:

```bash
mvn -pl modules/hephaestus-login -am test
```

Expected:

- `modules/hephaestus-login` 编译通过；
- 如有新增测试则 PASS；
- 登录日志分页输出字段仍为 `items/page/pageSize/total`。

- [ ] **Step 6: 提交这一小步**

```bash
git add modules/hephaestus-login/src/main/java/com/example/springaidemo/login/log/service/impl/LoginLogServiceImpl.java modules/hephaestus-login/src/main/java/com/example/springaidemo/login/log/dto/LoginLogPageResponse.java
git commit -m "refactor: reuse mybatis page support in login logs"
```

## Task 4: 回归验证与收尾

**Files:**

- Modify: `docs/mybatis/page-info/prd/2026-05-29-adhoc-page-info-prd.md`（如需同步实现结论，可最后再补；不需要则不改）

- [ ] **Step 1: 运行分页公共层测试**

Run:

```bash
mvn -pl modules/mybatis -Dtest=PageSupportTest test
```

Expected:

- PASS

- [ ] **Step 2: 运行登录模块测试**

Run:

```bash
mvn -pl modules/hephaestus-login -am test
```

Expected:

- PASS，或至少确认失败与本次改动无关并记录。

- [ ] **Step 3: 运行前端脚本语法检查，防止关联模块静态资源被历史问题带偏**

Run:

```bash
node --check modules/hephaestus-org/src/main/resources/static/org-settings.js
```

Expected:

- PASS

- [ ] **Step 4: 做一次差异复核**

重点检查：

- `modules/mybatis` 只新增分页公共类，没有混入业务条件对象。
- `LoginLogRepository` SQL 没被不必要改动。
- `LoginLogPageResponse` 字段结构未变化。

- [ ] **Step 5: 最终提交**

```bash
git add modules/mybatis/src/main/java/com/example/springaidemo/mybatis/page modules/mybatis/src/test/java/com/example/springaidemo/mybatis/page modules/hephaestus-login/src/main/java/com/example/springaidemo/login/log/service/impl/LoginLogServiceImpl.java modules/hephaestus-login/src/main/java/com/example/springaidemo/login/log/dto/LoginLogPageResponse.java docs/mybatis/page-info/prd/2026-05-29-adhoc-page-info-prd.md docs/mybatis/page-info/plan/2026-05-29-adhoc-page-info-plan.md
git commit -m "feat: migrate minimal page info support"
```

## Self-Review

### Spec Coverage

- 公共分页对象：Task 1、Task 2 覆盖。
- 登录日志迁移首个落点：Task 3 覆盖。
- 只做最小分页支持、不扩查询体系：通过 File Structure 与 Task 4 差异复核约束。
- TDD 要求：每个实现前都先有 failing test 或明确的兼容断言。

### Placeholder Scan

- 本计划没有 `TODO`、`TBD`、`类似 Task N` 之类占位描述。
- 每个代码修改步骤都给出了明确代码或明确命令。

### Type Consistency

- 公共分页类型统一使用 `PageQuery`、`PageInfo<T>`、`PageSupport`。
- 登录日志业务侧统一通过 `LoginLogPageResponse.from(PageInfo<LoginLogResponse>)` 做兼容转换。
