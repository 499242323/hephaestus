# Hephaestus Org Settings Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a new `hephaestus-org` module with tree-shaped `unit` management, person CRUD, avatar media binding, scope-based data permissions, and a ChatGPT-style settings drawer inside the existing chat page.

**Architecture:** Add a dedicated backend module for org domain logic and keep the existing `hephaestus-app` module as the boot app and static UI host. Reuse the existing media module for avatar storage, and enforce scope permissions on the backend using `X-Person-Id` plus `unit` ancestry.

**Tech Stack:** Java 17, Spring Boot 3, MyBatis repository pattern, Liquibase, static HTML/CSS/JavaScript, JUnit 5, Spring Boot Test

---

## File Structure

### New module

- Create: `modules/hephaestus-org/pom.xml`
- Create: `modules/hephaestus-org/src/main/java/com/example/springaidemo/org/controller/OrgUnitController.java`
- Create: `modules/hephaestus-org/src/main/java/com/example/springaidemo/org/controller/OrgPersonController.java`
- Create: `modules/hephaestus-org/src/main/java/com/example/springaidemo/org/service/OrgUnitService.java`
- Create: `modules/hephaestus-org/src/main/java/com/example/springaidemo/org/service/OrgPersonService.java`
- Create: `modules/hephaestus-org/src/main/java/com/example/springaidemo/org/service/OrgScopeService.java`
- Create: `modules/hephaestus-org/src/main/java/com/example/springaidemo/org/service/OrgAvatarService.java`
- Create: `modules/hephaestus-org/src/main/java/com/example/springaidemo/org/repository/OrgUnitRepository.java`
- Create: `modules/hephaestus-org/src/main/java/com/example/springaidemo/org/repository/OrgPersonRepository.java`
- Create: `modules/hephaestus-org/src/main/java/com/example/springaidemo/org/entity/OrgUnitEntity.java`
- Create: `modules/hephaestus-org/src/main/java/com/example/springaidemo/org/entity/OrgPersonEntity.java`
- Create: `modules/hephaestus-org/src/main/java/com/example/springaidemo/org/domain/OrgUnitTreeNode.java`
- Create: `modules/hephaestus-org/src/main/java/com/example/springaidemo/org/domain/OrgPersonSummary.java`
- Create: `modules/hephaestus-org/src/main/java/com/example/springaidemo/org/dto/CreateOrgUnitRequest.java`
- Create: `modules/hephaestus-org/src/main/java/com/example/springaidemo/org/dto/UpdateOrgUnitRequest.java`
- Create: `modules/hephaestus-org/src/main/java/com/example/springaidemo/org/dto/CreateOrgPersonRequest.java`
- Create: `modules/hephaestus-org/src/main/java/com/example/springaidemo/org/dto/UpdateOrgPersonRequest.java`
- Create: `modules/hephaestus-org/src/main/java/com/example/springaidemo/org/dto/OrgScopeResponse.java`
- Create: `modules/hephaestus-org/src/main/java/com/example/springaidemo/org/exception/OrgAccessDeniedException.java`
- Create: `modules/hephaestus-org/src/main/java/com/example/springaidemo/org/exception/OrgValidationException.java`

### Boot app and shared schema

- Modify: `pom.xml`
- Modify: `modules/hephaestus-app/pom.xml`
- Modify: `modules/liquibase/src/main/resources/db/changelog/db.changelog.xml`

### Frontend

- Modify: `modules/hephaestus-app/src/main/resources/static/chat.html`
- Modify: `modules/hephaestus-app/src/main/resources/static/chat.css`
- Modify: `modules/hephaestus-app/src/main/resources/static/chat.js`

### Tests

- Create: `modules/hephaestus-org/src/test/java/com/example/springaidemo/org/service/OrgScopeServiceTest.java`
- Create: `modules/hephaestus-org/src/test/java/com/example/springaidemo/org/service/OrgUnitServiceTest.java`
- Create: `modules/hephaestus-org/src/test/java/com/example/springaidemo/org/service/OrgPersonServiceTest.java`
- Create: `modules/hephaestus-org/src/test/java/com/example/springaidemo/org/controller/OrgUnitControllerTest.java`
- Create: `modules/hephaestus-org/src/test/java/com/example/springaidemo/org/controller/OrgPersonControllerTest.java`

## Task 1: Wire The New Module

**Files:**
- Modify: `pom.xml`
- Modify: `modules/hephaestus-app/pom.xml`
- Create: `modules/hephaestus-org/pom.xml`

- [ ] **Step 1: Write the failing module dependency setup**

```xml
<modules>
    <module>modules/liquibase</module>
    <module>modules/mybatis</module>
    <module>modules/hephaestus-media</module>
    <module>modules/hephaestus-org</module>
    <module>modules/hephaestus-app</module>
</modules>
```

```xml
<dependency>
    <groupId>com.example</groupId>
    <artifactId>hephaestus-org</artifactId>
    <version>${project.version}</version>
</dependency>
```

- [ ] **Step 2: Run Maven validate to verify the missing module fails**

Run: `rtk mvn -q -pl modules/hephaestus-app -am validate`

Expected: FAIL with missing `modules/hephaestus-org/pom.xml` or unresolved module error.

- [ ] **Step 3: Create the minimal org module POM**

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

    <artifactId>hephaestus-org</artifactId>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>com.example</groupId>
            <artifactId>mybatis</artifactId>
            <version>${project.version}</version>
        </dependency>
        <dependency>
            <groupId>com.example</groupId>
            <artifactId>hephaestus-media</artifactId>
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
    </dependencies>
</project>
```

- [ ] **Step 4: Run Maven validate to verify module wiring passes**

Run: `rtk mvn -q -pl modules/hephaestus-app -am validate`

Expected: PASS with module graph resolved.

- [ ] **Step 5: Commit**

```bash
git add pom.xml modules/hephaestus-app/pom.xml modules/hephaestus-org/pom.xml
git commit -m "feat: add hephaestus org module"
```

## Task 2: Add Schema For Units And Persons

**Files:**
- Modify: `modules/liquibase/src/main/resources/db/changelog/db.changelog.xml`
- Test: `rtk mvn -q -pl modules/liquibase -am test`

- [ ] **Step 1: Write the failing changelog additions**

```xml
<changeSet id="heph-unit-create-table" author="codex">
    <createTable tableName="heph_unit">
        <column name="id" type="bigint" autoIncrement="true">
            <constraints primaryKey="true" nullable="false"/>
        </column>
        <column name="unit_code" type="varchar(64)">
            <constraints nullable="false"/>
        </column>
        <column name="unit_name" type="varchar(128)">
            <constraints nullable="false"/>
        </column>
        <column name="parent_id" type="bigint" defaultValueNumeric="0">
            <constraints nullable="false"/>
        </column>
        <column name="ancestor_path" type="varchar(512)">
            <constraints nullable="false"/>
        </column>
    </createTable>
</changeSet>
```

- [ ] **Step 2: Run Liquibase-related tests before full table definition**

Run: `rtk mvn -q -pl modules/liquibase -am test`

Expected: FAIL if changelog structure or checks need complete indexes and constraints.

- [ ] **Step 3: Complete the schema with indexes and person table**

```xml
<changeSet id="heph-unit-create-table" author="codex">
    <preConditions onFail="MARK_RAN">
        <not>
            <tableExists tableName="heph_unit"/>
        </not>
    </preConditions>
    <createTable tableName="heph_unit">
        <column name="id" type="bigint" autoIncrement="true">
            <constraints nullable="false" primaryKey="true" primaryKeyName="pk_heph_unit"/>
        </column>
        <column name="unit_code" type="varchar(64)">
            <constraints nullable="false"/>
        </column>
        <column name="unit_name" type="varchar(128)">
            <constraints nullable="false"/>
        </column>
        <column name="parent_id" type="bigint" defaultValueNumeric="0">
            <constraints nullable="false"/>
        </column>
        <column name="ancestor_path" type="varchar(512)">
            <constraints nullable="false"/>
        </column>
        <column name="sort_order" type="int" defaultValueNumeric="0">
            <constraints nullable="false"/>
        </column>
        <column name="enabled" type="tinyint" defaultValueNumeric="1">
            <constraints nullable="false"/>
        </column>
        <column name="created_at" type="timestamp" defaultValueComputed="CURRENT_TIMESTAMP"/>
        <column name="updated_at" type="timestamp" defaultValueComputed="CURRENT_TIMESTAMP"/>
    </createTable>
    <addUniqueConstraint tableName="heph_unit" columnNames="unit_code" constraintName="uk_heph_unit_code"/>
    <createIndex tableName="heph_unit" indexName="idx_heph_unit_parent_id">
        <column name="parent_id"/>
    </createIndex>
</changeSet>

<changeSet id="heph-person-create-table" author="codex">
    <preConditions onFail="MARK_RAN">
        <not>
            <tableExists tableName="heph_person"/>
        </not>
    </preConditions>
    <createTable tableName="heph_person">
        <column name="id" type="bigint" autoIncrement="true">
            <constraints nullable="false" primaryKey="true" primaryKeyName="pk_heph_person"/>
        </column>
        <column name="person_code" type="varchar(64)">
            <constraints nullable="false"/>
        </column>
        <column name="person_name" type="varchar(128)">
            <constraints nullable="false"/>
        </column>
        <column name="unit_id" type="bigint">
            <constraints nullable="false"/>
        </column>
        <column name="avatar_media_id" type="bigint"/>
        <column name="mobile" type="varchar(64)"/>
        <column name="email" type="varchar(128)"/>
        <column name="remark" type="varchar(512)"/>
        <column name="enabled" type="tinyint" defaultValueNumeric="1">
            <constraints nullable="false"/>
        </column>
        <column name="created_at" type="timestamp" defaultValueComputed="CURRENT_TIMESTAMP"/>
        <column name="updated_at" type="timestamp" defaultValueComputed="CURRENT_TIMESTAMP"/>
    </createTable>
    <addUniqueConstraint tableName="heph_person" columnNames="person_code" constraintName="uk_heph_person_code"/>
    <createIndex tableName="heph_person" indexName="idx_heph_person_unit_id">
        <column name="unit_id"/>
    </createIndex>
</changeSet>
```

- [ ] **Step 4: Run Liquibase tests to verify schema changes pass**

Run: `rtk mvn -q -pl modules/liquibase -am test`

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add modules/liquibase/src/main/resources/db/changelog/db.changelog.xml
git commit -m "feat: add org unit and person schema"
```

## Task 3: Build Entities And Repositories

**Files:**
- Create: `modules/hephaestus-org/src/main/java/com/example/springaidemo/org/entity/OrgUnitEntity.java`
- Create: `modules/hephaestus-org/src/main/java/com/example/springaidemo/org/entity/OrgPersonEntity.java`
- Create: `modules/hephaestus-org/src/main/java/com/example/springaidemo/org/repository/OrgUnitRepository.java`
- Create: `modules/hephaestus-org/src/main/java/com/example/springaidemo/org/repository/OrgPersonRepository.java`

- [ ] **Step 1: Write the failing repository signatures**

```java
@Mapper
public interface OrgUnitRepository extends BaseAbstractRepository<OrgUnitEntity, Long> {
    List<OrgUnitEntity> findAllOrdered();
    List<OrgUnitEntity> findDescendants(@Param("unitId") Long unitId, @Param("ancestorPathPattern") String ancestorPathPattern);
    OrgUnitEntity getByUnitCode(@Param("unitCode") String unitCode);
}
```

```java
@Mapper
public interface OrgPersonRepository extends BaseAbstractRepository<OrgPersonEntity, Long> {
    List<OrgPersonEntity> findByScope(@Param("unitIds") List<Long> unitIds,
                                      @Param("personName") String personName,
                                      @Param("unitId") Long unitId,
                                      @Param("enabled") Boolean enabled);
    OrgPersonEntity getByPersonCode(@Param("personCode") String personCode);
}
```

- [ ] **Step 2: Run compilation to verify the missing entity classes fail**

Run: `rtk mvn -q -pl modules/hephaestus-org -am test -DskipTests`

Expected: FAIL with missing entity symbols or unmapped repository types.

- [ ] **Step 3: Add minimal entities matching the schema**

```java
public class OrgUnitEntity {
    private Long id;
    private String unitCode;
    private String unitName;
    private Long parentId;
    private String ancestorPath;
    private Integer sortOrder;
    private Boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

```java
public class OrgPersonEntity {
    private Long id;
    private String personCode;
    private String personName;
    private Long unitId;
    private Long avatarMediaId;
    private String mobile;
    private String email;
    private String remark;
    private Boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

- [ ] **Step 4: Run compilation to verify repositories and entities build**

Run: `rtk mvn -q -pl modules/hephaestus-org -am test -DskipTests`

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add modules/hephaestus-org/src/main/java/com/example/springaidemo/org/entity modules/hephaestus-org/src/main/java/com/example/springaidemo/org/repository
git commit -m "feat: add org entities and repositories"
```

## Task 4: Enforce Scope Permissions First

**Files:**
- Create: `modules/hephaestus-org/src/test/java/com/example/springaidemo/org/service/OrgScopeServiceTest.java`
- Create: `modules/hephaestus-org/src/main/java/com/example/springaidemo/org/service/OrgScopeService.java`
- Create: `modules/hephaestus-org/src/main/java/com/example/springaidemo/org/exception/OrgAccessDeniedException.java`

- [ ] **Step 1: Write the first failing scope test**

```java
@Test
void shouldReturnCurrentUnitAndDescendantUnitIds() {
    OrgUnitEntity root = unit(1L, "1");
    OrgUnitEntity child = unit(2L, "1/2");
    OrgUnitEntity outside = unit(9L, "9");
    OrgPersonEntity current = person(100L, 1L);

    List<Long> scope = service.resolveManageableUnitIds(current, List.of(root, child, outside));

    assertThat(scope).containsExactly(1L, 2L);
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `rtk mvn -q -pl modules/hephaestus-org -Dtest=OrgScopeServiceTest test`

Expected: FAIL with missing `OrgScopeService`.

- [ ] **Step 3: Implement the minimal scope service**

```java
public class OrgScopeService {

    public List<Long> resolveManageableUnitIds(OrgPersonEntity currentPerson, List<OrgUnitEntity> units) {
        OrgUnitEntity currentUnit = units.stream()
                .filter(unit -> Objects.equals(unit.getId(), currentPerson.getUnitId()))
                .findFirst()
                .orElseThrow(() -> new OrgAccessDeniedException("Current person has no manageable unit"));

        String selfPath = currentUnit.getAncestorPath();
        return units.stream()
                .filter(unit -> unit.getAncestorPath().equals(selfPath)
                        || unit.getAncestorPath().startsWith(selfPath + "/"))
                .map(OrgUnitEntity::getId)
                .sorted()
                .toList();
    }
}
```

- [ ] **Step 4: Run the scope test to verify it passes**

Run: `rtk mvn -q -pl modules/hephaestus-org -Dtest=OrgScopeServiceTest test`

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add modules/hephaestus-org/src/test/java/com/example/springaidemo/org/service/OrgScopeServiceTest.java modules/hephaestus-org/src/main/java/com/example/springaidemo/org/service/OrgScopeService.java modules/hephaestus-org/src/main/java/com/example/springaidemo/org/exception/OrgAccessDeniedException.java
git commit -m "feat: add org scope resolution"
```

## Task 5: Implement Unit Service With Tree Rules

**Files:**
- Create: `modules/hephaestus-org/src/test/java/com/example/springaidemo/org/service/OrgUnitServiceTest.java`
- Create: `modules/hephaestus-org/src/main/java/com/example/springaidemo/org/service/OrgUnitService.java`
- Create: `modules/hephaestus-org/src/main/java/com/example/springaidemo/org/domain/OrgUnitTreeNode.java`
- Create: `modules/hephaestus-org/src/main/java/com/example/springaidemo/org/dto/CreateOrgUnitRequest.java`
- Create: `modules/hephaestus-org/src/main/java/com/example/springaidemo/org/dto/UpdateOrgUnitRequest.java`
- Create: `modules/hephaestus-org/src/main/java/com/example/springaidemo/org/exception/OrgValidationException.java`

- [ ] **Step 1: Write the failing unit deletion rule test**

```java
@Test
void shouldRejectDeletingUnitWhenItHasChildren() {
    when(unitRepository.findDescendants(1L, "1/%")).thenReturn(List.of(childUnit()));

    assertThatThrownBy(() -> service.deleteUnit(100L, 1L))
            .isInstanceOf(OrgValidationException.class)
            .hasMessageContaining("child unit");
}
```

- [ ] **Step 2: Run the unit service test to verify it fails**

Run: `rtk mvn -q -pl modules/hephaestus-org -Dtest=OrgUnitServiceTest test`

Expected: FAIL with missing `OrgUnitService` logic.

- [ ] **Step 3: Implement minimal unit service behavior**

```java
public void deleteUnit(Long currentPersonId, Long unitId) {
    assertUnitInScope(currentPersonId, unitId);
    List<OrgUnitEntity> descendants = unitRepository.findDescendants(unitId, buildDescendantPattern(unitId));
    if (!descendants.isEmpty()) {
        throw new OrgValidationException("Cannot delete unit with child unit");
    }
    if (personRepository.countByUnitId(unitId) > 0) {
        throw new OrgValidationException("Cannot delete unit with persons");
    }
    unitRepository.deleteById(unitId);
}
```

- [ ] **Step 4: Run the unit service test to verify it passes**

Run: `rtk mvn -q -pl modules/hephaestus-org -Dtest=OrgUnitServiceTest test`

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add modules/hephaestus-org/src/test/java/com/example/springaidemo/org/service/OrgUnitServiceTest.java modules/hephaestus-org/src/main/java/com/example/springaidemo/org/service/OrgUnitService.java modules/hephaestus-org/src/main/java/com/example/springaidemo/org/domain/OrgUnitTreeNode.java modules/hephaestus-org/src/main/java/com/example/springaidemo/org/dto/CreateOrgUnitRequest.java modules/hephaestus-org/src/main/java/com/example/springaidemo/org/dto/UpdateOrgUnitRequest.java modules/hephaestus-org/src/main/java/com/example/springaidemo/org/exception/OrgValidationException.java
git commit -m "feat: add org unit service rules"
```

## Task 6: Implement Person Service And Avatar Binding

**Files:**
- Create: `modules/hephaestus-org/src/test/java/com/example/springaidemo/org/service/OrgPersonServiceTest.java`
- Create: `modules/hephaestus-org/src/main/java/com/example/springaidemo/org/service/OrgPersonService.java`
- Create: `modules/hephaestus-org/src/main/java/com/example/springaidemo/org/service/OrgAvatarService.java`
- Create: `modules/hephaestus-org/src/main/java/com/example/springaidemo/org/domain/OrgPersonSummary.java`
- Create: `modules/hephaestus-org/src/main/java/com/example/springaidemo/org/dto/CreateOrgPersonRequest.java`
- Create: `modules/hephaestus-org/src/main/java/com/example/springaidemo/org/dto/UpdateOrgPersonRequest.java`

- [ ] **Step 1: Write the failing person scope test**

```java
@Test
void shouldRejectCreatingPersonOutsideManageableUnit() {
    CreateOrgPersonRequest request = new CreateOrgPersonRequest("P-1", "Alice", 9L, null, null, null, null, true);

    assertThatThrownBy(() -> service.createPerson(100L, request))
            .isInstanceOf(OrgAccessDeniedException.class)
            .hasMessageContaining("scope");
}
```

- [ ] **Step 2: Run the person service test to verify it fails**

Run: `rtk mvn -q -pl modules/hephaestus-org -Dtest=OrgPersonServiceTest test`

Expected: FAIL with missing `OrgPersonService`.

- [ ] **Step 3: Implement minimal person and avatar services**

```java
public OrgPersonSummary createPerson(Long currentPersonId, CreateOrgPersonRequest request) {
    assertUnitInScope(currentPersonId, request.unitId());
    OrgPersonEntity entity = new OrgPersonEntity();
    entity.setPersonCode(request.personCode());
    entity.setPersonName(request.personName());
    entity.setUnitId(request.unitId());
    entity.setEnabled(request.enabled());
    personRepository.insert(entity);
    return toSummary(entity, null);
}
```

```java
public OrgPersonSummary bindAvatar(Long currentPersonId, Long personId, MultipartFile file) {
    OrgPersonEntity person = requirePersonInScope(currentPersonId, personId);
    MediaFile media = mediaFileService.storeUploadedFile("org-person-avatar-" + personId, file);
    personRepository.updateAvatarMediaId(personId, media.id());
    return toSummary(personRepository.getById(personId), media.accessUrl());
}
```

- [ ] **Step 4: Run the person service test to verify it passes**

Run: `rtk mvn -q -pl modules/hephaestus-org -Dtest=OrgPersonServiceTest test`

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add modules/hephaestus-org/src/test/java/com/example/springaidemo/org/service/OrgPersonServiceTest.java modules/hephaestus-org/src/main/java/com/example/springaidemo/org/service/OrgPersonService.java modules/hephaestus-org/src/main/java/com/example/springaidemo/org/service/OrgAvatarService.java modules/hephaestus-org/src/main/java/com/example/springaidemo/org/domain/OrgPersonSummary.java modules/hephaestus-org/src/main/java/com/example/springaidemo/org/dto/CreateOrgPersonRequest.java modules/hephaestus-org/src/main/java/com/example/springaidemo/org/dto/UpdateOrgPersonRequest.java
git commit -m "feat: add org person service and avatar binding"
```

## Task 7: Expose Unit And Person Controllers

**Files:**
- Create: `modules/hephaestus-org/src/test/java/com/example/springaidemo/org/controller/OrgUnitControllerTest.java`
- Create: `modules/hephaestus-org/src/test/java/com/example/springaidemo/org/controller/OrgPersonControllerTest.java`
- Create: `modules/hephaestus-org/src/main/java/com/example/springaidemo/org/controller/OrgUnitController.java`
- Create: `modules/hephaestus-org/src/main/java/com/example/springaidemo/org/controller/OrgPersonController.java`
- Create: `modules/hephaestus-org/src/main/java/com/example/springaidemo/org/dto/OrgScopeResponse.java`

- [ ] **Step 1: Write the failing controller permission-header test**

```java
@Test
void shouldRejectUnitTreeRequestWithoutPersonHeader() throws Exception {
    mockMvc.perform(get("/api/org/units/tree"))
            .andExpect(status().isBadRequest());
}
```

- [ ] **Step 2: Run the controller test to verify it fails**

Run: `rtk mvn -q -pl modules/hephaestus-org -Dtest=OrgUnitControllerTest,OrgPersonControllerTest test`

Expected: FAIL with missing controllers or missing request mappings.

- [ ] **Step 3: Implement minimal controllers**

```java
@RestController
@RequestMapping("/api/org/units")
public class OrgUnitController {

    @GetMapping("/tree")
    public List<OrgUnitTreeNode> getTree(@RequestHeader("X-Person-Id") Long personId) {
        return orgUnitService.getUnitTree(personId);
    }
}
```

```java
@RestController
@RequestMapping("/api/org/persons")
public class OrgPersonController {

    @GetMapping("/current-scope")
    public OrgScopeResponse getCurrentScope(@RequestHeader("X-Person-Id") Long personId) {
        return orgPersonService.getCurrentScope(personId);
    }
}
```

- [ ] **Step 4: Run the controller tests to verify they pass**

Run: `rtk mvn -q -pl modules/hephaestus-org -Dtest=OrgUnitControllerTest,OrgPersonControllerTest test`

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add modules/hephaestus-org/src/test/java/com/example/springaidemo/org/controller modules/hephaestus-org/src/main/java/com/example/springaidemo/org/controller modules/hephaestus-org/src/main/java/com/example/springaidemo/org/dto/OrgScopeResponse.java
git commit -m "feat: add org management controllers"
```

## Task 8: Add The Settings Drawer Shell

**Files:**
- Modify: `modules/hephaestus-app/src/main/resources/static/chat.html`
- Modify: `modules/hephaestus-app/src/main/resources/static/chat.css`
- Modify: `modules/hephaestus-app/src/main/resources/static/chat.js`

- [ ] **Step 1: Write the failing UI shell markup**

```html
<button id="settingsButton" class="sidebar-link" type="button">Settings</button>

<aside id="settingsDrawer" class="settings-drawer" aria-hidden="true">
    <div class="settings-drawer__header">
        <h2>Settings</h2>
        <button id="closeSettingsButton" type="button">×</button>
    </div>
    <div id="settingsDrawerContent" class="settings-drawer__content"></div>
</aside>
```

- [ ] **Step 2: Run the app and verify the drawer cannot open yet**

Run: `rtk .\\tools\\mvn-java21.ps1 -pl modules/hephaestus-app -am spring-boot:run`

Expected: The page loads, but clicking `Settings` does nothing because drawer behavior is not implemented.

- [ ] **Step 3: Implement the minimal drawer styles and toggle behavior**

```css
.settings-drawer {
    position: fixed;
    top: 0;
    right: 0;
    width: min(520px, 100vw);
    height: 100vh;
    transform: translateX(100%);
    transition: transform 180ms ease;
}

.settings-drawer.is-open {
    transform: translateX(0);
}
```

```js
const settingsButton = document.getElementById("settingsButton");
const closeSettingsButton = document.getElementById("closeSettingsButton");
const settingsDrawer = document.getElementById("settingsDrawer");

function setSettingsDrawerOpen(open) {
    settingsDrawer.classList.toggle("is-open", open);
    settingsDrawer.setAttribute("aria-hidden", String(!open));
}

settingsButton.addEventListener("click", () => setSettingsDrawerOpen(true));
closeSettingsButton.addEventListener("click", () => setSettingsDrawerOpen(false));
```

- [ ] **Step 4: Run the app and verify the drawer opens and closes**

Run: `rtk .\\tools\\mvn-java21.ps1 -pl modules/hephaestus-app -am spring-boot:run`

Expected: The `Settings` drawer slides in from the right and closes cleanly.

- [ ] **Step 5: Commit**

```bash
git add modules/hephaestus-app/src/main/resources/static/chat.html modules/hephaestus-app/src/main/resources/static/chat.css modules/hephaestus-app/src/main/resources/static/chat.js
git commit -m "feat: add settings drawer shell"
```

## Task 9: Render Unit Tree And People List In Drawer

**Files:**
- Modify: `modules/hephaestus-app/src/main/resources/static/chat.html`
- Modify: `modules/hephaestus-app/src/main/resources/static/chat.css`
- Modify: `modules/hephaestus-app/src/main/resources/static/chat.js`

- [ ] **Step 1: Write the failing drawer content placeholders**

```html
<div class="settings-tabs">
    <button id="unitsTabButton" type="button">Units</button>
    <button id="peopleTabButton" type="button">People</button>
</div>
<section id="unitsPanel"></section>
<section id="peoplePanel" hidden></section>
```

- [ ] **Step 2: Run the app and verify the drawer still lacks data rendering**

Run: `rtk .\\tools\\mvn-java21.ps1 -pl modules/hephaestus-app -am spring-boot:run`

Expected: Drawer shell exists, but unit tree and person list are empty.

- [ ] **Step 3: Implement minimal fetch and render logic**

```js
async function loadOrgScope() {
    const response = await fetch("/api/org/persons/current-scope", {
        headers: { "X-Person-Id": String(currentSettingsPersonId) }
    });
    return response.json();
}

async function loadUnitTree() {
    const response = await fetch("/api/org/units/tree", {
        headers: { "X-Person-Id": String(currentSettingsPersonId) }
    });
    return response.json();
}
```

```js
function renderUnitNode(node) {
    const children = (node.children || []).map(renderUnitNode).join("");
    return `<li data-unit-id="${node.id}"><div class="unit-row">${node.unitName}</div><ul>${children}</ul></li>`;
}
```

- [ ] **Step 4: Run the app and verify both panels render data**

Run: `rtk .\\tools\\mvn-java21.ps1 -pl modules/hephaestus-app -am spring-boot:run`

Expected: The `Units` tab shows a tree and the `People` tab shows scoped persons.

- [ ] **Step 5: Commit**

```bash
git add modules/hephaestus-app/src/main/resources/static/chat.html modules/hephaestus-app/src/main/resources/static/chat.css modules/hephaestus-app/src/main/resources/static/chat.js
git commit -m "feat: render org settings content"
```

## Task 10: Add CRUD Interactions And Avatar Upload

**Files:**
- Modify: `modules/hephaestus-app/src/main/resources/static/chat.html`
- Modify: `modules/hephaestus-app/src/main/resources/static/chat.css`
- Modify: `modules/hephaestus-app/src/main/resources/static/chat.js`

- [ ] **Step 1: Write the failing create-person form markup**

```html
<form id="personEditorForm" class="settings-form">
    <input id="personNameInput" name="personName" />
    <select id="personUnitIdInput" name="unitId"></select>
    <input id="personAvatarInput" name="avatar" type="file" accept="image/*" />
    <button type="submit">Save person</button>
</form>
```

- [ ] **Step 2: Run the app and verify submitting the form does nothing yet**

Run: `rtk .\\tools\\mvn-java21.ps1 -pl modules/hephaestus-app -am spring-boot:run`

Expected: UI renders the form, but no network write occurs.

- [ ] **Step 3: Implement minimal create, update, delete, and avatar upload calls**

```js
async function savePerson(payload) {
    const response = await fetch("/api/org/persons", {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
            "X-Person-Id": String(currentSettingsPersonId)
        },
        body: JSON.stringify(payload)
    });
    return response.json();
}
```

```js
async function uploadPersonAvatar(personId, file) {
    const formData = new FormData();
    formData.append("file", file);
    const response = await fetch(`/api/org/persons/${personId}/avatar`, {
        method: "POST",
        headers: { "X-Person-Id": String(currentSettingsPersonId) },
        body: formData
    });
    return response.json();
}
```

- [ ] **Step 4: Run the app and verify CRUD and avatar upload work end to end**

Run: `rtk .\\tools\\mvn-java21.ps1 -pl modules/hephaestus-app -am spring-boot:run`

Expected: A user can create a unit, create a person in-scope, edit values, delete allowed records, and upload an avatar with preview refresh.

- [ ] **Step 5: Commit**

```bash
git add modules/hephaestus-app/src/main/resources/static/chat.html modules/hephaestus-app/src/main/resources/static/chat.css modules/hephaestus-app/src/main/resources/static/chat.js
git commit -m "feat: add org settings crud interactions"
```

## Task 11: Full Verification Pass

**Files:**
- Test: `modules/hephaestus-org/src/test/java/com/example/springaidemo/org/service/OrgScopeServiceTest.java`
- Test: `modules/hephaestus-org/src/test/java/com/example/springaidemo/org/service/OrgUnitServiceTest.java`
- Test: `modules/hephaestus-org/src/test/java/com/example/springaidemo/org/service/OrgPersonServiceTest.java`
- Test: `modules/hephaestus-org/src/test/java/com/example/springaidemo/org/controller/OrgUnitControllerTest.java`
- Test: `modules/hephaestus-org/src/test/java/com/example/springaidemo/org/controller/OrgPersonControllerTest.java`
- Test: `modules/hephaestus-app/src/main/resources/static/chat.html`
- Test: `modules/hephaestus-app/src/main/resources/static/chat.js`
- Test: `modules/hephaestus-app/src/main/resources/static/chat.css`

- [ ] **Step 1: Run targeted org module tests**

Run: `rtk mvn -q -pl modules/hephaestus-org -Dtest=OrgScopeServiceTest,OrgUnitServiceTest,OrgPersonServiceTest,OrgUnitControllerTest,OrgPersonControllerTest test`

Expected: PASS.

- [ ] **Step 2: Run the full app test suite**

Run: `rtk .\\tools\\mvn-java21.ps1 test`

Expected: PASS.

- [ ] **Step 3: Run manual verification of the drawer flows**

Run: `rtk .\\tools\\mvn-java21.ps1 -pl modules/hephaestus-app -am spring-boot:run`

Expected:
- `Settings` drawer opens and closes
- current person switch changes visible unit tree and people list
- cannot delete a unit with children or persons
- cannot create or edit out-of-scope records
- avatar upload refreshes the visible row

- [ ] **Step 4: Review git diff for only intended files**

Run: `rtk git diff --stat`

Expected: Only org module, Liquibase, parent/app POMs, and chat static files changed for this feature.

- [ ] **Step 5: Commit**

```bash
git add modules/hephaestus-org modules/liquibase/src/main/resources/db/changelog/db.changelog.xml modules/hephaestus-app/src/main/resources/static/chat.html modules/hephaestus-app/src/main/resources/static/chat.css modules/hephaestus-app/src/main/resources/static/chat.js pom.xml modules/hephaestus-app/pom.xml
git commit -m "feat: add org settings management"
```

## Self-Review Checklist

- Spec coverage: module split, schema, tree `unit`, person CRUD, avatar binding, scope permissions, settings drawer, and testing all map to tasks above.
- Placeholder scan: no `TODO`, `TBD`, or “implement later” placeholders remain.
- Type consistency: `unit`, `person`, `avatar_media_id`, `X-Person-Id`, and scope terminology are consistent across tasks.
