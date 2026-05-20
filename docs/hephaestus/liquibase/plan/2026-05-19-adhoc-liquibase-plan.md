# Hephaestus Liquibase Module Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Split the current app into a Maven multi-module build, add a reusable `modules/liquibase` module, and move schema management for `spring_ai_chat_memory` and `spring_ai_media_file` to Liquibase with lowercase table names.

**Architecture:** Convert the repository root into a Maven aggregator POM, move the current Spring Boot app into `modules/hephaestus-app`, and add `modules/liquibase` as a plain `jar` module that contributes the changelog and lowercase Spring AI JDBC chat memory dialect. The app module depends on `modules/liquibase`, disables Spring AI schema auto-init, and lets Liquibase become the only DDL entry point.

**Tech Stack:** Java 17, Spring Boot 3.5.14, Spring AI 1.1.6, Maven multi-module, Liquibase 5.0.2, MySQL, JUnit 5

---

## File Structure

### Root Aggregator

- `pom.xml`
  - Convert from executable Spring Boot build to `pom` packaging aggregator.
  - Define shared properties and dependency management.
  - Register submodules.

### Application Module

- `modules/hephaestus-app/pom.xml`
  - Spring Boot application module.
  - Depends on `modules/liquibase`.
- `modules/hephaestus-app/src/main/java/com/example/springaidemo/**`
  - Existing app code moved under the app module.
- `modules/hephaestus-app/src/main/resources/application.yml`
  - Disables Spring AI JDBC schema auto-init.
  - Enables Liquibase via classpath changelog.
- `modules/hephaestus-app/src/test/java/com/example/springaidemo/**`
  - Existing tests moved under the app module.

### Liquibase Module

- `modules/liquibase/pom.xml`
  - Reusable `jar` module for Liquibase and Spring AI JDBC dialect support.
- `modules/liquibase/src/main/resources/db/changelog/db.changelog.xml`
  - Single changelog file with baseline and compatibility patches.
- `modules/liquibase/src/main/java/com/example/springaidemo/liquibase/LowercaseMysqlChatMemoryRepositoryDialect.java`
  - Forces Spring AI JDBC SQL to use `spring_ai_chat_memory`.
- `modules/liquibase/src/test/java/com/example/springaidemo/liquibase/**`
  - Liquibase-focused tests for changelog and dialect wiring.

## Task 1: Convert the repository root into a Maven aggregator

**Files:**
- Modify: `pom.xml`
- Create: `modules/hephaestus-app/pom.xml`
- Create: `modules/liquibase/pom.xml`

- [ ] **Step 1: Replace the root executable POM with an aggregator POM**

Use this as the target structure for `pom.xml`:

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.example</groupId>
    <artifactId>hephaestus-parent</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <packaging>pom</packaging>
    <name>hephaestus-parent</name>

    <properties>
        <java.version>17</java.version>
        <spring.boot.version>3.5.14</spring.boot.version>
        <spring.ai.version>1.1.6</spring.ai.version>
        <liquibase.version>5.0.2</liquibase.version>
    </properties>

    <modules>
        <module>modules/liquibase</module>
        <module>modules/hephaestus-app</module>
    </modules>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-dependencies</artifactId>
                <version>${spring.boot.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
            <dependency>
                <groupId>org.springframework.ai</groupId>
                <artifactId>spring-ai-bom</artifactId>
                <version>${spring.ai.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>
</project>
```

- [ ] **Step 2: Create the application module POM**

Create `modules/hephaestus-app/pom.xml` with this structure:

```xml
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

    <artifactId>hephaestus-app</artifactId>
    <name>hephaestus-app</name>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.ai</groupId>
            <artifactId>spring-ai-starter-model-openai</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.ai</groupId>
            <artifactId>spring-ai-starter-model-chat-memory-repository-jdbc</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis</artifactId>
        </dependency>
        <dependency>
            <groupId>org.apache.commons</groupId>
            <artifactId>commons-pool2</artifactId>
        </dependency>
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>com.example</groupId>
            <artifactId>liquibase</artifactId>
            <version>${project.version}</version>
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
        <dependency>
            <groupId>org.junit.platform</groupId>
            <artifactId>junit-platform-launcher</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 3: Create the Liquibase module POM**

Create `modules/liquibase/pom.xml` with this structure:

```xml
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

    <artifactId>liquibase</artifactId>
    <name>liquibase</name>

    <dependencies>
        <dependency>
            <groupId>org.liquibase</groupId>
            <artifactId>liquibase-core</artifactId>
            <version>${liquibase.version}</version>
        </dependency>
        <dependency>
            <groupId>org.springframework.ai</groupId>
            <artifactId>spring-ai-model-chat-memory-repository-jdbc</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework</groupId>
            <artifactId>spring-jdbc</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-autoconfigure</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
```

- [ ] **Step 4: Run Maven validation against the new module graph**

Run:

```bash
mvn -q validate
```

Expected:

```text
BUILD SUCCESS
```

- [ ] **Step 5: Commit the build-structure changes**

```bash
git add pom.xml modules/hephaestus-app/pom.xml modules/liquibase/pom.xml
git commit -m "build: convert project to Maven multi-module structure"
```

## Task 2: Move the current application into `modules/hephaestus-app`

**Files:**
- Create: `modules/hephaestus-app/src/main/java/com/example/springaidemo/**`
- Create: `modules/hephaestus-app/src/main/resources/application.yml`
- Create: `modules/hephaestus-app/src/main/resources/static/chat.html`
- Create: `modules/hephaestus-app/src/test/java/com/example/springaidemo/**`
- Delete: `src/main/java/com/example/springaidemo/**`
- Delete: `src/main/resources/application.yml`
- Delete: `src/main/resources/static/chat.html`
- Delete: `src/test/java/com/example/springaidemo/**`

- [ ] **Step 1: Move the existing application source tree into the app module**

Target layout after the move:

```text
modules/hephaestus-app/src/main/java/com/example/springaidemo/...
modules/hephaestus-app/src/main/resources/application.yml
modules/hephaestus-app/src/main/resources/static/chat.html
modules/hephaestus-app/src/test/java/com/example/springaidemo/...
```

Move these trees as-is before making functional edits:

```text
src/main/java -> modules/hephaestus-app/src/main/java
src/main/resources -> modules/hephaestus-app/src/main/resources
src/test/java -> modules/hephaestus-app/src/test/java
```

- [ ] **Step 2: Keep the Spring Boot entry point unchanged inside the new module**

Confirm `modules/hephaestus-app/src/main/java/com/example/springaidemo/SpringAiDemoApplication.java` still contains:

```java
package com.example.springaidemo;

import com.example.springaidemo.media.MediaStorageProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(MediaStorageProperties.class)
public class SpringAiDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringAiDemoApplication.class, args);
    }
}
```

- [ ] **Step 3: Run the app module tests to verify the move did not break package scanning**

Run:

```bash
mvn -q -pl modules/hephaestus-app test
```

Expected:

```text
BUILD SUCCESS
```

If failures are path-related, fix imports and resource locations before moving on.

- [ ] **Step 4: Commit the application-module move**

```bash
git add modules/hephaestus-app src
git commit -m "refactor: move Spring Boot app into hephaestus-app module"
```

## Task 3: Add the Liquibase module skeleton, changelog, and failing schema tests

**Files:**
- Create: `modules/liquibase/src/main/resources/db/changelog/db.changelog.xml`
- Create: `modules/liquibase/src/test/java/com/example/springaidemo/liquibase/ChangelogStructureTest.java`
- Create: `modules/liquibase/src/test/java/com/example/springaidemo/liquibase/LowercaseSchemaContractTest.java`

- [ ] **Step 1: Write the first failing test for the single changelog location**

Create `modules/liquibase/src/test/java/com/example/springaidemo/liquibase/ChangelogStructureTest.java`:

```java
package com.example.springaidemo.liquibase;

import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;

class ChangelogStructureTest {

    @Test
    void loadsSingleChangelogFromClasspath() {
        InputStream stream = getClass().getClassLoader()
                .getResourceAsStream("db/changelog/db.changelog.xml");

        assertThat(stream).isNotNull();
    }
}
```

- [ ] **Step 2: Run the single test to verify it fails before the changelog exists**

Run:

```bash
mvn -q -pl modules/liquibase -Dtest=ChangelogStructureTest test
```

Expected:

```text
FAILURE
```

With a failure similar to:

```text
expected: not null
but was: null
```

- [ ] **Step 3: Create the single changelog file with lowercase table sections**

Create `modules/liquibase/src/main/resources/db/changelog/db.changelog.xml` with this initial content:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog
        xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
        https://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-latest.xsd">

    <!-- chat-memory baseline -->
    <changeSet id="chat-memory-create-table" author="codex">
        <preConditions onFail="MARK_RAN">
            <not>
                <tableExists tableName="spring_ai_chat_memory"/>
            </not>
        </preConditions>
        <createTable tableName="spring_ai_chat_memory">
            <column name="conversation_id" type="varchar(255)">
                <constraints nullable="false"/>
            </column>
            <column name="content" type="text">
                <constraints nullable="false"/>
            </column>
            <column name="type" type="varchar(20)" defaultValue="USER">
                <constraints nullable="false"/>
            </column>
            <column name="timestamp" type="timestamp" defaultValueComputed="CURRENT_TIMESTAMP">
                <constraints nullable="false"/>
            </column>
            <column name="message_type" type="varchar(20)"/>
        </createTable>
        <createIndex tableName="spring_ai_chat_memory" indexName="idx_spring_ai_chat_memory_conversation_ts">
            <column name="conversation_id"/>
            <column name="timestamp"/>
        </createIndex>
    </changeSet>

    <!-- chat-memory compatibility patches -->
    <changeSet id="chat-memory-add-type" author="codex">
        <preConditions onFail="MARK_RAN">
            <tableExists tableName="spring_ai_chat_memory"/>
            <not>
                <columnExists tableName="spring_ai_chat_memory" columnName="type"/>
            </not>
        </preConditions>
        <addColumn tableName="spring_ai_chat_memory">
            <column name="type" type="varchar(20)" defaultValue="USER">
                <constraints nullable="false"/>
            </column>
        </addColumn>
    </changeSet>

    <changeSet id="chat-memory-add-timestamp" author="codex">
        <preConditions onFail="MARK_RAN">
            <tableExists tableName="spring_ai_chat_memory"/>
            <not>
                <columnExists tableName="spring_ai_chat_memory" columnName="timestamp"/>
            </not>
        </preConditions>
        <addColumn tableName="spring_ai_chat_memory">
            <column name="timestamp" type="timestamp" defaultValueComputed="CURRENT_TIMESTAMP">
                <constraints nullable="false"/>
            </column>
        </addColumn>
    </changeSet>

    <changeSet id="chat-memory-relax-message-type" author="codex">
        <preConditions onFail="MARK_RAN">
            <tableExists tableName="spring_ai_chat_memory"/>
            <columnExists tableName="spring_ai_chat_memory" columnName="message_type"/>
        </preConditions>
        <modifyDataType tableName="spring_ai_chat_memory" columnName="message_type" newDataType="varchar(20)"/>
    </changeSet>

    <changeSet id="chat-memory-add-index" author="codex">
        <preConditions onFail="MARK_RAN">
            <tableExists tableName="spring_ai_chat_memory"/>
            <not>
                <indexExists tableName="spring_ai_chat_memory" indexName="idx_spring_ai_chat_memory_conversation_ts"/>
            </not>
        </preConditions>
        <createIndex tableName="spring_ai_chat_memory" indexName="idx_spring_ai_chat_memory_conversation_ts">
            <column name="conversation_id"/>
            <column name="timestamp"/>
        </createIndex>
    </changeSet>

    <!-- media-file baseline -->
    <changeSet id="media-file-create-table" author="codex">
        <preConditions onFail="MARK_RAN">
            <not>
                <tableExists tableName="spring_ai_media_file"/>
            </not>
        </preConditions>
        <createTable tableName="spring_ai_media_file">
            <column name="id" type="bigint" autoIncrement="true">
                <constraints nullable="false" primaryKey="true" primaryKeyName="pk_spring_ai_media_file"/>
            </column>
            <column name="conversation_id" type="varchar(128)">
                <constraints nullable="false"/>
            </column>
            <column name="original_filename" type="varchar(255)">
                <constraints nullable="false"/>
            </column>
            <column name="stored_filename" type="varchar(255)">
                <constraints nullable="false"/>
            </column>
            <column name="content_type" type="varchar(128)">
                <constraints nullable="false"/>
            </column>
            <column name="file_size" type="bigint">
                <constraints nullable="false"/>
            </column>
            <column name="storage_path" type="varchar(512)">
                <constraints nullable="false"/>
            </column>
            <column name="access_url" type="varchar(512)">
                <constraints nullable="false"/>
            </column>
            <column name="source_type" type="varchar(32)">
                <constraints nullable="false"/>
            </column>
            <column name="created_at" type="timestamp" defaultValueComputed="CURRENT_TIMESTAMP">
                <constraints nullable="false"/>
            </column>
        </createTable>
        <createIndex tableName="spring_ai_media_file" indexName="idx_spring_ai_media_conversation">
            <column name="conversation_id"/>
            <column name="created_at"/>
        </createIndex>
        <addUniqueConstraint tableName="spring_ai_media_file"
                             columnNames="storage_path"
                             constraintName="uk_spring_ai_media_storage_path"/>
    </changeSet>

    <!-- media-file compatibility patches -->
    <changeSet id="media-file-add-stored-filename" author="codex">
        <preConditions onFail="MARK_RAN">
            <tableExists tableName="spring_ai_media_file"/>
            <not>
                <columnExists tableName="spring_ai_media_file" columnName="stored_filename"/>
            </not>
        </preConditions>
        <addColumn tableName="spring_ai_media_file">
            <column name="stored_filename" type="varchar(255)" defaultValue="">
                <constraints nullable="false"/>
            </column>
        </addColumn>
    </changeSet>
</databaseChangeLog>
```

- [ ] **Step 4: Add a second test that locks in lowercase table names**

Create `modules/liquibase/src/test/java/com/example/springaidemo/liquibase/LowercaseSchemaContractTest.java`:

```java
package com.example.springaidemo.liquibase;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class LowercaseSchemaContractTest {

    @Test
    void changelogUsesOnlyLowercaseBusinessTableNames() throws Exception {
        byte[] bytes = getClass().getClassLoader()
                .getResourceAsStream("db/changelog/db.changelog.xml")
                .readAllBytes();

        String xml = new String(bytes, StandardCharsets.UTF_8);

        assertThat(xml).contains("spring_ai_chat_memory");
        assertThat(xml).contains("spring_ai_media_file");
        assertThat(xml).doesNotContain("SPRING_AI_CHAT_MEMORY");
    }
}
```

- [ ] **Step 5: Run the Liquibase module tests and confirm they pass**

Run:

```bash
mvn -q -pl modules/liquibase -Dtest=ChangelogStructureTest,LowercaseSchemaContractTest test
```

Expected:

```text
BUILD SUCCESS
```

- [ ] **Step 6: Commit the Liquibase skeleton**

```bash
git add modules/liquibase/src/main/resources/db/changelog/db.changelog.xml modules/liquibase/src/test/java/com/example/springaidemo/liquibase
git commit -m "feat: add liquibase module changelog skeleton"
```

## Task 4: Add the lowercase Spring AI JDBC dialect and wire it into chat memory

**Files:**
- Create: `modules/liquibase/src/main/java/com/example/springaidemo/liquibase/LowercaseMysqlChatMemoryRepositoryDialect.java`
- Modify: `modules/hephaestus-app/src/main/java/com/example/springaidemo/config/PersistentChatMemoryConfig.java`
- Create: `modules/liquibase/src/test/java/com/example/springaidemo/liquibase/LowercaseMysqlChatMemoryRepositoryDialectTest.java`

- [ ] **Step 1: Write the failing test for lowercase table-name SQL generation**

Create `modules/liquibase/src/test/java/com/example/springaidemo/liquibase/LowercaseMysqlChatMemoryRepositoryDialectTest.java`:

```java
package com.example.springaidemo.liquibase;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LowercaseMysqlChatMemoryRepositoryDialectTest {

    @Test
    void usesLowercaseChatMemoryTableNameInSql() {
        LowercaseMysqlChatMemoryRepositoryDialect dialect = new LowercaseMysqlChatMemoryRepositoryDialect();

        assertThat(dialect.getSelectMessagesSql()).contains("spring_ai_chat_memory");
        assertThat(dialect.getInsertMessageSql()).contains("spring_ai_chat_memory");
        assertThat(dialect.getDeleteMessagesSql()).contains("spring_ai_chat_memory");
        assertThat(dialect.getFindConversationIdsSql()).contains("spring_ai_chat_memory");
    }
}
```

- [ ] **Step 2: Run the new dialect test to verify it fails before the class exists**

Run:

```bash
mvn -q -pl modules/liquibase -Dtest=LowercaseMysqlChatMemoryRepositoryDialectTest test
```

Expected:

```text
FAILURE
```

With a message similar to:

```text
cannot find symbol: class LowercaseMysqlChatMemoryRepositoryDialect
```

- [ ] **Step 3: Implement the lowercase dialect in the Liquibase module**

Create `modules/liquibase/src/main/java/com/example/springaidemo/liquibase/LowercaseMysqlChatMemoryRepositoryDialect.java`:

```java
package com.example.springaidemo.liquibase;

import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepositoryDialect;

public class LowercaseMysqlChatMemoryRepositoryDialect implements JdbcChatMemoryRepositoryDialect {

    public static final String TABLE_NAME = "spring_ai_chat_memory";

    @Override
    public String getSelectMessagesSql() {
        return "SELECT conversation_id, content, type, timestamp FROM " + TABLE_NAME
                + " WHERE conversation_id = ? ORDER BY timestamp";
    }

    @Override
    public String getInsertMessageSql() {
        return "INSERT INTO " + TABLE_NAME
                + " (conversation_id, content, type, timestamp) VALUES (?, ?, ?, ?)";
    }

    @Override
    public String getDeleteMessagesSql() {
        return "DELETE FROM " + TABLE_NAME + " WHERE conversation_id = ?";
    }

    @Override
    public String getFindConversationIdsSql() {
        return "SELECT DISTINCT conversation_id FROM " + TABLE_NAME + " ORDER BY conversation_id";
    }
}
```

- [ ] **Step 4: Wire the dialect into the application chat memory configuration**

Update `modules/hephaestus-app/src/main/java/com/example/springaidemo/config/PersistentChatMemoryConfig.java` to this shape:

```java
package com.example.springaidemo.config;

import com.example.springaidemo.liquibase.LowercaseMysqlChatMemoryRepositoryDialect;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class PersistentChatMemoryConfig {

    @Bean
    public ChatMemory chatMemory(JdbcTemplate jdbcTemplate) {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(
                        JdbcChatMemoryRepository.builder()
                                .jdbcTemplate(jdbcTemplate)
                                .dialect(new LowercaseMysqlChatMemoryRepositoryDialect())
                                .build()
                )
                .maxMessages(20)
                .build();
    }

    @Bean
    public ChatClient chatClient(ChatModel chatModel, ChatMemory chatMemory) {
        return ChatClient.builder(chatModel)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
    }
}
```

- [ ] **Step 5: Run the focused module tests and confirm the dialect integration passes**

Run:

```bash
mvn -q -pl modules/liquibase -Dtest=LowercaseMysqlChatMemoryRepositoryDialectTest test
mvn -q -pl modules/hephaestus-app -Dtest=ChatMemoryTest test
```

Expected:

```text
BUILD SUCCESS
```

- [ ] **Step 6: Commit the lowercase dialect support**

```bash
git add modules/liquibase/src/main/java/com/example/springaidemo/liquibase/LowercaseMysqlChatMemoryRepositoryDialect.java modules/liquibase/src/test/java/com/example/springaidemo/liquibase/LowercaseMysqlChatMemoryRepositoryDialectTest.java modules/hephaestus-app/src/main/java/com/example/springaidemo/config/PersistentChatMemoryConfig.java
git commit -m "feat: use lowercase chat memory table through custom dialect"
```

## Task 5: Switch the app module from startup DDL to Liquibase and remove old schema initializers

**Files:**
- Modify: `modules/hephaestus-app/src/main/resources/application.yml`
- Delete: `modules/hephaestus-app/src/main/java/com/example/springaidemo/config/ChatMemorySchemaInitializer.java`
- Delete: `modules/hephaestus-app/src/main/java/com/example/springaidemo/media/MediaFileSchemaInitializer.java`
- Delete: `modules/hephaestus-app/src/test/java/com/example/springaidemo/config/ChatMemorySchemaInitializerTest.java`
- Delete: `modules/hephaestus-app/src/test/java/com/example/springaidemo/media/MediaFileSchemaInitializerTest.java`
- Modify: `modules/hephaestus-app/src/test/java/com/example/springaidemo/ChatMemoryTest.java`

- [ ] **Step 1: Write the failing integration assertion that Liquibase tables exist**

Update `modules/hephaestus-app/src/test/java/com/example/springaidemo/ChatMemoryTest.java` to add this test before making config changes:

```java
@Test
void liquibaseCreatesLowercaseChatMemoryTable() {
    Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = ?",
            Integer.class,
            "spring_ai_chat_memory");

    assertThat(count).isNotNull();
    assertThat(count).isGreaterThan(0);
}
```

- [ ] **Step 2: Run the targeted test and verify it fails with the current startup-DDL wiring**

Run:

```bash
mvn -q -pl modules/hephaestus-app -Dtest=ChatMemoryTest#liquibaseCreatesLowercaseChatMemoryTable test
```

Expected:

```text
FAILURE
```

Because `spring_ai_chat_memory` is not yet managed by Liquibase.

- [ ] **Step 3: Switch application configuration from Spring AI schema init to Liquibase**

Update `modules/hephaestus-app/src/main/resources/application.yml` so the chat memory section contains:

```yaml
spring:
  ai:
    chat:
      memory:
        repository:
          jdbc:
            initialize-schema: never
  liquibase:
    enabled: true
    change-log: classpath:db/changelog/db.changelog.xml
```

Keep the existing datasource and Redis sections intact.

- [ ] **Step 4: Delete the old manual DDL classes and tests**

Remove:

```text
modules/hephaestus-app/src/main/java/com/example/springaidemo/config/ChatMemorySchemaInitializer.java
modules/hephaestus-app/src/main/java/com/example/springaidemo/media/MediaFileSchemaInitializer.java
modules/hephaestus-app/src/test/java/com/example/springaidemo/config/ChatMemorySchemaInitializerTest.java
modules/hephaestus-app/src/test/java/com/example/springaidemo/media/MediaFileSchemaInitializerTest.java
```

- [ ] **Step 5: Run the app module tests and confirm Liquibase now provides the schema**

Run:

```bash
mvn -q -pl modules/hephaestus-app -Dtest=ChatMemoryTest,MediaStorageServiceTest,MediaFileControllerTest test
```

Expected:

```text
BUILD SUCCESS
```

- [ ] **Step 6: Commit the Liquibase cutover**

```bash
git add modules/hephaestus-app/src/main/resources/application.yml modules/hephaestus-app/src/test/java/com/example/springaidemo/ChatMemoryTest.java modules/hephaestus-app/src/main/java/com/example/springaidemo/config modules/hephaestus-app/src/main/java/com/example/springaidemo/media modules/hephaestus-app/src/test/java/com/example/springaidemo/config modules/hephaestus-app/src/test/java/com/example/springaidemo/media
git commit -m "refactor: replace startup schema DDL with liquibase"
```

## Task 6: Verify the multi-module build end to end

**Files:**
- Modify: `HELP.md`

- [ ] **Step 1: Update the project help text for the new module layout**

Replace the build section in `HELP.md` with:

```md
### Build

Run all modules:

```bash
mvn test
```

Run the application module:

```bash
mvn -pl modules/hephaestus-app spring-boot:run
```

Run only the Liquibase module tests:

```bash
mvn -pl modules/liquibase test
```
```

- [ ] **Step 2: Run the full repository test suite**

Run:

```bash
mvn test
```

Expected:

```text
BUILD SUCCESS
```

- [ ] **Step 3: Run a compile-only verification across all modules**

Run:

```bash
mvn -q -DskipTests compile
```

Expected:

```text
BUILD SUCCESS
```

- [ ] **Step 4: Commit the final verification and docs update**

```bash
git add HELP.md
git commit -m "docs: update build instructions for liquibase module split"
```

