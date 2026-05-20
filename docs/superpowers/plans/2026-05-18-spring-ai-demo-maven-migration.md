# SpringAIDemo Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Convert the current Spring Boot Gradle project to Maven, rename the project to `SpringAIDemo`, and align the Java package/class names with the new project identity.

**Architecture:** Replace Gradle metadata with a Maven `pom.xml` that preserves the existing Spring Boot, Java, and Spring AI versions. Move the application and test classes into a renamed package and class structure so the build name, artifact coordinates, and Java entry point stay consistent.

**Tech Stack:** Java 17, Spring Boot 3.5.14, Spring AI BOM 1.1.6, Maven, JUnit 5

---

### Task 1: Replace Gradle build metadata with Maven

**Files:**
- Create: `pom.xml`
- Delete: `build.gradle`
- Delete: `settings.gradle`
- Delete: `gradlew`
- Delete: `gradlew.bat`
- Delete: `gradle/wrapper/gradle-wrapper.jar`
- Delete: `gradle/wrapper/gradle-wrapper.properties`

- [ ] **Step 1: Write the Maven build file**
- [ ] **Step 2: Remove obsolete Gradle build files**

### Task 2: Rename application package and entry point

**Files:**
- Create: `src/main/java/com/example/springaidemo/SpringAiDemoApplication.java`
- Delete: `src/main/java/com/example/demo/DemoApplication.java`
- Create: `src/test/java/com/example/springaidemo/SpringAiDemoApplicationTests.java`
- Delete: `src/test/java/com/example/demo/DemoApplicationTests.java`

- [ ] **Step 1: Move the main application class into the new package and rename the class**
- [ ] **Step 2: Move the test class into the new package and update its target**

### Task 3: Refresh project docs and verify the build

**Files:**
- Modify: `HELP.md`

- [ ] **Step 1: Replace Gradle-oriented help links with Maven-oriented links**
- [ ] **Step 2: Run `mvn test` and confirm the Spring context test passes**
