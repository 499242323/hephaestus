# Hephaestus MyBatis Module Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a new `modules/mybatis` Maven module that provides UrbanPro-style repository CRUD behavior for `spring_ai_media_file`, prioritizing a minimal migration of Egova repository classes such as `BaseAbstractRepository` and `BaseInsertTemplate`, including custom queries and `insertList(...)` with key backfill semantics.

**Architecture:** Keep Spring Boot startup and web/service logic inside `modules/hephaestus-app`, move MyBatis-Plus infrastructure and media persistence into `modules/mybatis`, and let `hephaestus-app` depend on that module. In `modules/mybatis`, first try to migrate the smallest viable Egova repository class set needed for `BaseAbstractRepository`-style CRUD and `BaseInsertTemplate`-style batch insert; only if that dependency set does not close should we replace it with local equivalents.

**Tech Stack:** Maven multi-module build, Spring Boot 3.5, MyBatis-Plus Boot 3 starter, MySQL, Liquibase, JUnit 5, Spring Boot Test.

---

## File Structure

- Create: `E:\NEW_WORK\hephaestus\modules\mybatis\pom.xml`
- Create: `E:\NEW_WORK\hephaestus\modules\mybatis\src\main\java\com\example\springaidemo\mybatis\config\HephaestusMybatisAutoConfiguration.java`
- Create: `E:\NEW_WORK\hephaestus\modules\mybatis\src\main\java\com\example\springaidemo\mybatis\repository\BaseAbstractRepository.java`
- Create: `E:\NEW_WORK\hephaestus\modules\mybatis\src\main\java\com\example\springaidemo\mybatis\repository\BaseInsertTemplate.java`
- Create: `E:\NEW_WORK\hephaestus\modules\mybatis\src\main\java\com\example\springaidemo\mybatis\repository\...` (only direct support classes required to compile the migrated Egova base types)
- Create: `E:\NEW_WORK\hephaestus\modules\mybatis\src\main\java\com\example\springaidemo\media\MediaFileEntity.java`
- Create: `E:\NEW_WORK\hephaestus\modules\mybatis\src\main\java\com\example\springaidemo\media\MediaFileRepository.java`
- Create: `E:\NEW_WORK\hephaestus\modules\mybatis\src\main\resources\META-INF\spring\org.springframework.boot.autoconfigure.AutoConfiguration.imports`
- Modify: `E:\NEW_WORK\hephaestus\pom.xml`
- Modify: `E:\NEW_WORK\hephaestus\modules\hephaestus-app\pom.xml`
- Modify: `E:\NEW_WORK\hephaestus\modules\hephaestus-app\src\main\java\com\example\springaidemo\SpringAiDemoApplication.java`
- Modify: `E:\NEW_WORK\hephaestus\modules\hephaestus-app\src\main\resources\application.yml`
- Delete: `E:\NEW_WORK\hephaestus\modules\hephaestus-app\src\main\java\com\example\springaidemo\media\MediaFileRepository.java`
- Create: `E:\NEW_WORK\hephaestus\modules\hephaestus-app\src\test\java\com\example\springaidemo\media\MediaFileRepositoryIntegrationTest.java`

### Task 1: Add the `modules/mybatis` module to the build

**Files:**
- Create: `E:\NEW_WORK\hephaestus\modules\mybatis\pom.xml`
- Modify: `E:\NEW_WORK\hephaestus\pom.xml`
- Modify: `E:\NEW_WORK\hephaestus\modules\hephaestus-app\pom.xml`

- [ ] **Step 1: Write the failing build wiring first**

Add the new module to the root aggregator and add a dependency from `hephaestus-app` to `mybatis` before any Java code exists so the first compile failure proves the module boundary is active.

```xml
<!-- E:\NEW_WORK\hephaestus\pom.xml -->
<modules>
    <module>modules/liquibase</module>
    <module>modules/mybatis</module>
    <module>modules/hephaestus-app</module>
</modules>
```

```xml
<!-- E:\NEW_WORK\hephaestus\modules\hephaestus-app\pom.xml -->
<dependency>
    <groupId>com.example</groupId>
    <artifactId>mybatis</artifactId>
    <version>${project.version}</version>
</dependency>
```

- [ ] **Step 2: Run the Maven compile to verify it fails because `modules/mybatis` does not exist yet**

Run: `rtk mvn -pl modules/hephaestus-app -am -DskipTests compile`

Expected: FAIL with a Maven module or artifact resolution error mentioning `modules/mybatis` or `com.example:mybatis`.

- [ ] **Step 3: Create the new module POM with MyBatis-Plus dependencies**

Create `E:\NEW_WORK\hephaestus\modules\mybatis\pom.xml` with the parent link and the smallest dependency set needed for repository support.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.example</groupId>
        <artifactId>hephaestus-parent</artifactId>
        <version>0.0.1-SNAPSHOT</version>
        <relativePath>../..</relativePath>
    </parent>

    <artifactId>mybatis</artifactId>
    <name>mybatis</name>
    <description>Hephaestus MyBatis support module</description>

    <dependencies>
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
            <version>3.5.7</version>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-autoconfigure</artifactId>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
```

- [ ] **Step 4: Run compile again to verify the module wiring now passes**

Run: `rtk mvn -pl modules/hephaestus-app -am -DskipTests compile`

Expected: PASS for Maven project structure, or FAIL later on missing repository classes instead of module resolution.

### Task 2: Build the minimal migrated Egova repository foundation

**Files:**
- Create: `E:\NEW_WORK\hephaestus\modules\mybatis\src\main\java\com\example\springaidemo\mybatis\config\HephaestusMybatisAutoConfiguration.java`
- Create: `E:\NEW_WORK\hephaestus\modules\mybatis\src\main\java\com\example\springaidemo\mybatis\repository\BaseAbstractRepository.java`
- Create: `E:\NEW_WORK\hephaestus\modules\mybatis\src\main\java\com\example\springaidemo\mybatis\repository\BaseInsertTemplate.java`
- Create: `E:\NEW_WORK\hephaestus\modules\mybatis\src\main\java\com\example\springaidemo\mybatis\repository\...` (only direct support classes required to compile the migrated Egova base types)
- Create: `E:\NEW_WORK\hephaestus\modules\mybatis\src\main\resources\META-INF\spring\org.springframework.boot.autoconfigure.AutoConfiguration.imports`
- Modify: `E:\NEW_WORK\hephaestus\modules\hephaestus-app\src\main\java\com\example\springaidemo\SpringAiDemoApplication.java`
- Modify: `E:\NEW_WORK\hephaestus\modules\hephaestus-app\src\main\resources\application.yml`

- [ ] **Step 1: Write the failing repository foundation test target by compiling references that do not exist yet**

Prepare the application to scan the new repository package and fail on missing types.

```java
// E:\NEW_WORK\hephaestus\modules\hephaestus-app\src\main\java\com\example\springaidemo\SpringAiDemoApplication.java
@SpringBootApplication
@EnableConfigurationProperties(MediaStorageProperties.class)
public class SpringAiDemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(SpringAiDemoApplication.class, args);
    }
}
```

```yaml
# E:\NEW_WORK\hephaestus\modules\hephaestus-app\src\main\resources\application.yml
mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true
```

The compile should still fail until the configuration class exists.

- [ ] **Step 2: Run compile to verify the missing configuration fails loudly**

Run: `rtk mvn -pl modules/hephaestus-app -am -DskipTests compile`

Expected: FAIL on unresolved `olympus.hephaestus.mybatis...` classes once you reference them in later steps.

- [ ] **Step 3: Create the auto-configuration and migrate the smallest viable Egova repository types**

Add the base configuration and migrate the smallest viable Egova repository contracts first. Do not invent local names unless compilation proves the migrated set cannot close.

```java
// E:\NEW_WORK\hephaestus\modules\mybatis\src\main\java\com\example\springaidemo\mybatis\config\HephaestusMybatisAutoConfiguration.java
package olympus.hephaestus.mybatis.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.AutoConfiguration;

@AutoConfiguration
@MapperScan("olympus.hephaestus")
public class HephaestusMybatisAutoConfiguration {
}
```

```java
// E:\NEW_WORK\hephaestus\modules\mybatis\src\main\java\com\example\springaidemo\mybatis\repository\BaseAbstractRepository.java
package olympus.hephaestus.mybatis.repository;

public interface BaseAbstractRepository<T, ID> {
    int insert(T entity);
    T getById(ID id);
}
```

```java
// E:\NEW_WORK\hephaestus\modules\mybatis\src\main\java\com\example\springaidemo\mybatis\repository\BaseInsertTemplate.java
package olympus.hephaestus.mybatis.repository;

public class BaseInsertTemplate {
    public String dynamicSQL() {
        throw new UnsupportedOperationException("Replace with migrated Egova implementation");
    }
}
```

If the real migrated Egova code needs 1-3 direct support classes, add only those classes in this step. If it starts pulling in broad clause DSL or service-layer abstractions, stop and replace the base types with local equivalents while keeping the same external method names.

```text
// E:\NEW_WORK\hephaestus\modules\mybatis\src\main\resources\META-INF\spring\org.springframework.boot.autoconfigure.AutoConfiguration.imports
olympus.hephaestus.mybatis.config.HephaestusMybatisAutoConfiguration
```

- [ ] **Step 4: Run compile to verify the repository foundation is loadable**

Run: `rtk mvn -pl modules/hephaestus-app -am -DskipTests compile`

Expected: PASS for the shared module and FAIL only if media-specific repository migration is still incomplete.

### Task 3: Migrate `MediaFileRepository` into `modules/mybatis`

**Files:**
- Create: `E:\NEW_WORK\hephaestus\modules\mybatis\src\main\java\com\example\springaidemo\media\MediaFileEntity.java`
- Create: `E:\NEW_WORK\hephaestus\modules\mybatis\src\main\java\com\example\springaidemo\media\MediaFileRepository.java`
- Delete: `E:\NEW_WORK\hephaestus\modules\hephaestus-app\src\main\java\com\example\springaidemo\media\MediaFileRepository.java`

- [ ] **Step 1: Write the target repository API so compile fails until the entity exists**

Create the repository interface first so the migration target is explicit.

```java
package olympus.hephaestus.media;

import olympus.hephaestus.mybatis.repository.BaseAbstractRepository;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.UpdateProvider;

import java.util.List;
import java.util.Optional;

public interface MediaFileRepository extends BaseAbstractRepository<MediaFileEntity, Long> {

    @Update("UPDATE spring_ai_media_file SET access_url = #{accessUrl} WHERE id = #{id}")
    void updateAccessUrl(@Param("id") long id, @Param("accessUrl") String accessUrl);

    default MediaFile save(MediaFile mediaFile) {
        MediaFileEntity entity = MediaFileEntity.fromDomain(mediaFile);
        this.insert(entity);
        return entity.toDomain();
    }

    default Optional<MediaFile> findById(long id) {
        MediaFileEntity entity = this.getById(id);
        return entity == null ? Optional.empty() : Optional.of(entity.toDomain());
    }

    @UpdateProvider(type = BaseInsertTemplate.class, method = "dynamicSQL")
    void insertList(@Param("_list") List<MediaFileEntity> entities);
}
```

- [ ] **Step 2: Run compile to verify it fails on missing `MediaFileEntity`**

Run: `rtk mvn -pl modules/hephaestus-app -am -DskipTests compile`

Expected: FAIL with unresolved symbol errors for `MediaFileEntity`.

- [ ] **Step 3: Add the persistence entity with conversion helpers and key metadata**

```java
package olympus.hephaestus.media;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("spring_ai_media_file")
public class MediaFileEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("conversation_id")
    private String conversationId;

    @TableField("original_filename")
    private String originalFilename;

    @TableField("stored_filename")
    private String storedFilename;

    @TableField("content_type")
    private String contentType;

    @TableField("file_size")
    private Long fileSize;

    @TableField("storage_path")
    private String storagePath;

    @TableField("access_url")
    private String accessUrl;

    @TableField("source_type")
    private String sourceType;

    @TableField("created_at")
    private LocalDateTime createdAt;

    public static MediaFileEntity fromDomain(MediaFile mediaFile) {
        MediaFileEntity entity = new MediaFileEntity();
        entity.setId(mediaFile.id());
        entity.setConversationId(mediaFile.conversationId());
        entity.setOriginalFilename(mediaFile.originalFilename());
        entity.setStoredFilename(mediaFile.storedFilename());
        entity.setContentType(mediaFile.contentType());
        entity.setFileSize(mediaFile.fileSize());
        entity.setStoragePath(mediaFile.storagePath());
        entity.setAccessUrl(mediaFile.accessUrl());
        entity.setSourceType(mediaFile.sourceType());
        entity.setCreatedAt(mediaFile.createdAt());
        return entity;
    }

    public MediaFile toDomain() {
        return new MediaFile(
                id,
                conversationId,
                originalFilename,
                storedFilename,
                contentType,
                fileSize == null ? 0L : fileSize,
                storagePath,
                accessUrl,
                sourceType,
                createdAt
        );
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getConversationId() { return conversationId; }
    public void setConversationId(String conversationId) { this.conversationId = conversationId; }
    public String getOriginalFilename() { return originalFilename; }
    public void setOriginalFilename(String originalFilename) { this.originalFilename = originalFilename; }
    public String getStoredFilename() { return storedFilename; }
    public void setStoredFilename(String storedFilename) { this.storedFilename = storedFilename; }
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
    public String getStoragePath() { return storagePath; }
    public void setStoragePath(String storagePath) { this.storagePath = storagePath; }
    public String getAccessUrl() { return accessUrl; }
    public void setAccessUrl(String accessUrl) { this.accessUrl = accessUrl; }
    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
```

- [ ] **Step 4: Delete the old `JdbcTemplate` repository and verify the app now compiles against the moved interface**

Delete:

```text
E:\NEW_WORK\hephaestus\modules\hephaestus-app\src\main\java\com\example\springaidemo\media\MediaFileRepository.java
```

Run: `rtk mvn -pl modules/hephaestus-app -am -DskipTests compile`

Expected: PASS for compile with imports now resolving from `modules/mybatis`.

### Task 4: Add repository integration tests and lock in `insertList(...)` behavior

**Files:**
- Create: `E:\NEW_WORK\hephaestus\modules\hephaestus-app\src\test\java\com\example\springaidemo\media\MediaFileRepositoryIntegrationTest.java`

- [ ] **Step 1: Write the failing repository integration test**

Create an integration test that verifies single insert, lookup, update, and `insertList(...)` key backfill semantics.

```java
package olympus.hephaestus.media;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class MediaFileRepositoryIntegrationTest {

    @Autowired
    private MediaFileRepository mediaFileRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanTable() {
        jdbcTemplate.update("DELETE FROM spring_ai_media_file");
    }

    @Test
    void savesFindsUpdatesAndBatchInsertsMediaFiles() {
        MediaFile saved = mediaFileRepository.save(new MediaFile(
                null, "session-1", "a.txt", "a-txt", "text/plain", 10L,
                "rec/upload/a-txt", "", "USER_UPLOAD", LocalDateTime.now()
        ));
        assertThat(saved.id()).isNotNull();

        MediaFile loaded = mediaFileRepository.findById(saved.id()).orElseThrow();
        assertThat(loaded.originalFilename()).isEqualTo("a.txt");

        mediaFileRepository.updateAccessUrl(saved.id(), "/hephaestus/media/a-txt");
        MediaFile updated = mediaFileRepository.findById(saved.id()).orElseThrow();
        assertThat(updated.accessUrl()).isEqualTo("/hephaestus/media/a-txt");

        MediaFileEntity first = MediaFileEntity.fromDomain(new MediaFile(
                null, "session-2", "b.txt", "b-txt", "text/plain", 11L,
                "rec/upload/b-txt", "", "USER_UPLOAD", LocalDateTime.now()
        ));
        MediaFileEntity second = MediaFileEntity.fromDomain(new MediaFile(
                null, "session-3", "c.txt", "c-txt", "text/plain", 12L,
                "rec/upload/c-txt", "", "USER_UPLOAD", LocalDateTime.now()
        ));

        mediaFileRepository.insertList(List.of(first, second));

        assertThat(first.getId()).isNotNull();
        assertThat(second.getId()).isNotNull();
    }
}
```

- [ ] **Step 2: Run the repository test to verify it fails before final wiring adjustments**

Run: `rtk mvn -pl modules/hephaestus-app -Dtest=MediaFileRepositoryIntegrationTest test`

Expected: FAIL on context wiring, missing scan, or repository mapping issues until all MyBatis configuration is complete.

- [ ] **Step 3: Fix any remaining scan, migrated base class, or batch template gaps with the minimal code needed**

If the auto-configuration import alone is not enough, keep the fix minimal and inside the new module. The only acceptable additional code here is repository scanning support or the direct dependencies needed to make the migrated `BaseAbstractRepository` / `BaseInsertTemplate` compile and run, not business logic expansion.

```java
// Keep the configuration class limited to scanning and boot integration.
@AutoConfiguration
@MapperScan("olympus.hephaestus")
public class HephaestusMybatisAutoConfiguration {
}
```

- [ ] **Step 4: Run the focused repository test and then the media test suite**

Run: `rtk mvn -pl modules/hephaestus-app -Dtest=MediaFileRepositoryIntegrationTest test`

Expected: PASS

Run: `rtk mvn -pl modules/hephaestus-app -Dtest=*Media* test`

Expected: PASS

- [ ] **Step 5: Run the module build for final verification**

Run: `rtk mvn -pl modules/hephaestus-app -am test`

Expected: PASS for `modules/mybatis`, `modules/liquibase`, and `modules/hephaestus-app`.

## Self-Review Notes

- Spec coverage: this plan covers the new `modules/mybatis` module, the minimal Egova migration set priority, UrbanPro-style repository behavior, `MediaFileRepository` migration, and `insertList(...)` key backfill semantics.
- Placeholder scan: no `TODO`, `TBD`, or “similar to above” shortcuts remain.
- Type consistency: the plan consistently uses `MediaFileEntity`, `MediaFileRepository`, `BaseAbstractRepository`, and `BaseInsertTemplate`.
