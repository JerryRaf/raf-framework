# Examples 重构 Phase 1 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 删除 raf-example-full-stack 和 raf-example-distributed-tx，将现有 5 个 example 重命名并统一 pom 结构与包名。

**Architecture:** 每个 example 继承 `raf-framework-parent`，包名统一为 `io.github.jerryraf.examples.{shortname}`，artifactId 统一为 `raf-example-{name}-starter`。

**Tech Stack:** Java 17, Spring Boot 3.4.7, Maven, raf-framework-parent

**Spec:** `docs/superpowers/specs/2026-05-06-examples-refactor-design.md`

---

## File Map

### 删除
- `examples/raf-example-full-stack/`（整目录）
- `examples/raf-example-distributed-tx/`（整目录）

### 重命名（目录 + 内部修改）

| 原目录 | 新目录 | pom 变更 | 包名变更 |
|---|---|---|---|
| `raf-example-basic` | `raf-example-web-starter` | artifactId + name | `com.raf.example.basic` → `io.github.jerryraf.examples.web` |
| `raf-example-gateway` | `raf-example-gateway-starter` | artifactId + name | 包名已正确，无需改 |
| `raf-example-mybatis` | `raf-example-mybatis-starter` | 重写 pom（去掉独立 dependencyManagement，改继承 parent）| `com.github.raf.examples.mybatis` → `io.github.jerryraf.examples.mybatis`（部分文件已正确）|
| `raf-example-redis` | `raf-example-redis-starter` | artifactId + name | `com.raf.example.redis` → `io.github.jerryraf.examples.redis` |
| `raf-example-dubbo` | `raf-example-dubbo-starter` | artifactId + name（父+子模块）| 包名已正确，无需改 |

---

## Task 1: 删除 raf-example-full-stack 和 raf-example-distributed-tx

**Files:**
- Delete: `examples/raf-example-full-stack/`
- Delete: `examples/raf-example-distributed-tx/`

- [ ] **Step 1: 确认这两个目录不在根 pom.xml 的 modules 中**

```bash
grep -n "full-stack\|distributed-tx" D:/Framework/raf-framework/pom.xml
```

Expected: 无输出（这两个目录不是根聚合模块）

- [ ] **Step 2: 删除 raf-example-full-stack**

在文件管理器或 IDE 中删除整个目录 `examples/raf-example-full-stack/`，或执行：

```bash
rm -rf "D:/Framework/raf-framework/examples/raf-example-full-stack"
```

- [ ] **Step 3: 删除 raf-example-distributed-tx**

```bash
rm -rf "D:/Framework/raf-framework/examples/raf-example-distributed-tx"
```

- [ ] **Step 4: 验证删除结果**

```bash
ls "D:/Framework/raf-framework/examples/"
```

Expected 输出（只剩5个）:
```
raf-example-basic
raf-example-dubbo
raf-example-gateway
raf-example-mybatis
raf-example-redis
```

- [ ] **Step 5: Commit**

```bash
cd "D:/Framework/raf-framework"
git add -A
git commit -m "chore(examples): remove raf-example-full-stack and raf-example-distributed-tx"
```

---

## Task 2: 重命名 raf-example-basic → raf-example-web-starter

**Files:**
- Rename dir: `examples/raf-example-basic/` → `examples/raf-example-web-starter/`
- Modify: `examples/raf-example-web-starter/pom.xml`
- Modify: `examples/raf-example-web-starter/src/main/java/...` 所有 `.java` 文件（包名）
- Modify: `examples/raf-example-web-starter/src/main/resources/application.yml`

- [ ] **Step 1: 重命名目录**

```bash
mv "D:/Framework/raf-framework/examples/raf-example-basic" \
   "D:/Framework/raf-framework/examples/raf-example-web-starter"
```

- [ ] **Step 2: 修改 pom.xml**

文件路径：`examples/raf-example-web-starter/pom.xml`

将：
```xml
<artifactId>raf-example-basic</artifactId>
<version>1.0.0</version>
<name>RAF Example - Basic Web</name>
<description>Demonstrates unified response, exception hierarchy, and distributed tracing</description>
```

改为：
```xml
<artifactId>raf-example-web-starter</artifactId>
<version>1.0.0</version>
<name>RAF Example - Web Starter</name>
<description>Demonstrates RafResult unified response, exception hierarchy, traceId, request logging, and CORS</description>
```

- [ ] **Step 3: 修改所有 Java 文件的包名**

将所有 `package com.raf.example.basic` 改为 `package io.github.jerryraf.examples.web`，
将所有 `import com.raf.example.basic` 改为 `import io.github.jerryraf.examples.web`。

涉及文件（逐一修改）：
- `src/main/java/com/raf/example/basic/BasicExampleApplication.java`
- `src/main/java/com/raf/example/basic/controller/UserController.java`
- `src/main/java/com/raf/example/basic/service/UserService.java`
- `src/main/java/com/raf/example/basic/dto/UserCreateReq.java`
- `src/main/java/com/raf/example/basic/dto/UserRes.java`
- `src/main/java/com/raf/example/basic/common/UserErrorCode.java`

每个文件第一行改为对应新包名，例如：
```java
package io.github.jerryraf.examples.web;
```
```java
package io.github.jerryraf.examples.web.controller;
```

- [ ] **Step 4: 重命名 Java 目录结构**

将 `src/main/java/com/raf/example/basic/` 目录重命名为 `src/main/java/io/github/jerryraf/examples/web/`（需要先创建新目录，移动文件，删除旧目录）。

- [ ] **Step 5: 修改 application.yml 中的 spring.application.name**

```yaml
spring:
  application:
    name: raf-example-web-starter
```

- [ ] **Step 6: 验证编译**

```bash
cd "D:/Framework/raf-framework/examples/raf-example-web-starter"
mvn compile -s "D:/Program Files/apache-maven-3.9.10/conf/settings-raf.xml" -Dmaven.repo.local="D:/data/repository/local"
```

Expected: `BUILD SUCCESS`

- [ ] **Step 7: Commit**

```bash
cd "D:/Framework/raf-framework"
git add -A
git commit -m "refactor(examples): rename raf-example-basic to raf-example-web-starter, unify package name"
```

---

## Task 3: 重命名 raf-example-gateway → raf-example-gateway-starter

**Files:**
- Rename dir: `examples/raf-example-gateway/` → `examples/raf-example-gateway-starter/`
- Modify: `examples/raf-example-gateway-starter/pom.xml`

- [ ] **Step 1: 重命名目录**

```bash
mv "D:/Framework/raf-framework/examples/raf-example-gateway" \
   "D:/Framework/raf-framework/examples/raf-example-gateway-starter"
```

- [ ] **Step 2: 修改 pom.xml**

文件路径：`examples/raf-example-gateway-starter/pom.xml`

将：
```xml
<artifactId>raf-example-gateway</artifactId>
<version>1.0.0</version>
<name>RAF Example - Gateway Security</name>
```

改为：
```xml
<artifactId>raf-example-gateway-starter</artifactId>
<version>1.0.0</version>
<name>RAF Example - Gateway Starter</name>
```

- [ ] **Step 3: 修改 application.yml 中的 spring.application.name**

文件路径：`examples/raf-example-gateway-starter/src/main/resources/application.yml`

```yaml
spring:
  application:
    name: raf-example-gateway-starter
```

- [ ] **Step 4: 验证编译**

```bash
cd "D:/Framework/raf-framework/examples/raf-example-gateway-starter"
mvn compile -s "D:/Program Files/apache-maven-3.9.10/conf/settings-raf.xml" -Dmaven.repo.local="D:/data/repository/local"
```

Expected: `BUILD SUCCESS`

- [ ] **Step 5: Commit**

```bash
cd "D:/Framework/raf-framework"
git add -A
git commit -m "refactor(examples): rename raf-example-gateway to raf-example-gateway-starter"
```

---

## Task 4: 重命名 raf-example-redis → raf-example-redis-starter（含包名修正）

**Files:**
- Rename dir: `examples/raf-example-redis/` → `examples/raf-example-redis-starter/`
- Modify: `examples/raf-example-redis-starter/pom.xml`
- Modify: 所有 `.java` 文件（包名 `com.raf.example.redis` → `io.github.jerryraf.examples.redis`）
- Modify: `application.yml`

- [ ] **Step 1: 重命名目录**

```bash
mv "D:/Framework/raf-framework/examples/raf-example-redis" \
   "D:/Framework/raf-framework/examples/raf-example-redis-starter"
```

- [ ] **Step 2: 修改 pom.xml**

将：
```xml
<artifactId>raf-example-redis</artifactId>
<version>1.0.0</version>
<name>RAF Example - Redis &amp; Distributed Lock</name>
```

改为：
```xml
<artifactId>raf-example-redis-starter</artifactId>
<version>1.0.0</version>
<name>RAF Example - Redis Starter</name>
<description>Demonstrates RedisService common operations and Redisson distributed lock</description>
```

- [ ] **Step 3: 修改所有 Java 文件包名**

涉及文件：
- `RedisExampleApplication.java` → `package io.github.jerryraf.examples.redis;`
- `controller/StockController.java` → `package io.github.jerryraf.examples.redis.controller;`
- `service/StockService.java` → `package io.github.jerryraf.examples.redis.service;`
- `dto/StockDeductReq.java` → `package io.github.jerryraf.examples.redis.dto;`

同时修改各文件中的 import 语句，将 `com.raf.example.redis` 替换为 `io.github.jerryraf.examples.redis`。

- [ ] **Step 4: 重命名 Java 目录结构**

将 `src/main/java/com/raf/example/redis/` 移动到 `src/main/java/io/github/jerryraf/examples/redis/`。

- [ ] **Step 5: 修改 application.yml**

```yaml
spring:
  application:
    name: raf-example-redis-starter
```

- [ ] **Step 6: 验证编译**

```bash
cd "D:/Framework/raf-framework/examples/raf-example-redis-starter"
mvn compile -s "D:/Program Files/apache-maven-3.9.10/conf/settings-raf.xml" -Dmaven.repo.local="D:/data/repository/local"
```

Expected: `BUILD SUCCESS`

- [ ] **Step 7: Commit**

```bash
cd "D:/Framework/raf-framework"
git add -A
git commit -m "refactor(examples): rename raf-example-redis to raf-example-redis-starter, unify package name"
```

---

## Task 5: 重命名 raf-example-dubbo → raf-example-dubbo-starter

**Files:**
- Rename dir: `examples/raf-example-dubbo/` → `examples/raf-example-dubbo-starter/`
- Modify: `examples/raf-example-dubbo-starter/pom.xml`（父 pom）
- Modify: `examples/raf-example-dubbo-starter/dubbo-api/pom.xml`
- Modify: `examples/raf-example-dubbo-starter/dubbo-provider/pom.xml`
- Modify: `examples/raf-example-dubbo-starter/dubbo-consumer/pom.xml`

- [ ] **Step 1: 重命名目录**

```bash
mv "D:/Framework/raf-framework/examples/raf-example-dubbo" \
   "D:/Framework/raf-framework/examples/raf-example-dubbo-starter"
```

- [ ] **Step 2: 修改父 pom.xml**

文件路径：`examples/raf-example-dubbo-starter/pom.xml`

将：
```xml
<artifactId>raf-example-dubbo</artifactId>
<name>RAF Example - Dubbo RPC</name>
```

改为：
```xml
<artifactId>raf-example-dubbo-starter</artifactId>
<name>RAF Example - Dubbo Starter</name>
```

- [ ] **Step 3: 修改子模块 pom 中的 parent artifactId**

`dubbo-api/pom.xml`、`dubbo-provider/pom.xml`、`dubbo-consumer/pom.xml` 中的 `<parent>` 块：

将：
```xml
<parent>
    <artifactId>raf-example-dubbo</artifactId>
    ...
</parent>
```

改为：
```xml
<parent>
    <artifactId>raf-example-dubbo-starter</artifactId>
    ...
</parent>
```

- [ ] **Step 4: 修改各服务 application.yml 中的 spring.application.name**

`dubbo-provider/src/main/resources/application.yml`:
```yaml
spring:
  application:
    name: raf-example-dubbo-provider
```

`dubbo-consumer/src/main/resources/application.yml`:
```yaml
spring:
  application:
    name: raf-example-dubbo-consumer
```

- [ ] **Step 5: 验证编译**

```bash
cd "D:/Framework/raf-framework/examples/raf-example-dubbo-starter"
mvn compile -s "D:/Program Files/apache-maven-3.9.10/conf/settings-raf.xml" -Dmaven.repo.local="D:/data/repository/local"
```

Expected: `BUILD SUCCESS`

- [ ] **Step 6: Commit**

```bash
cd "D:/Framework/raf-framework"
git add -A
git commit -m "refactor(examples): rename raf-example-dubbo to raf-example-dubbo-starter"
```

---

## Task 6: 重构 raf-example-mybatis → raf-example-mybatis-starter（pom 统一 + 包名修正）

**Files:**
- Rename dir: `examples/raf-example-mybatis/` → `examples/raf-example-mybatis-starter/`
- Rewrite: `examples/raf-example-mybatis-starter/pom.xml`（去掉独立 dependencyManagement，改继承 raf-framework-parent）
- Modify: `bootstrap.yml`（type-aliases-package 包名）
- Modify: 所有 Java 文件（包名 `com.github.raf.examples.mybatis` → `io.github.jerryraf.examples.mybatis`，部分文件已正确）

- [ ] **Step 1: 重命名目录**

```bash
mv "D:/Framework/raf-framework/examples/raf-example-mybatis" \
   "D:/Framework/raf-framework/examples/raf-example-mybatis-starter"
```

- [ ] **Step 2: 重写 pom.xml**

文件路径：`examples/raf-example-mybatis-starter/pom.xml`

完整内容替换为：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>io.github.jerryraf</groupId>
        <artifactId>raf-framework-parent</artifactId>
        <version>${revision}</version>
        <relativePath>../../raf-framework-parent</relativePath>
    </parent>

    <groupId>io.github.jerryraf.examples</groupId>
    <artifactId>raf-example-mybatis-starter</artifactId>
    <version>1.0.0</version>
    <packaging>jar</packaging>

    <name>RAF Example - MyBatis Starter</name>
    <description>Demonstrates multi-datasource, read-write splitting, CRUD, pagination, and transaction management</description>

    <dependencies>
        <!-- RAF Starters -->
        <dependency>
            <groupId>io.github.jerryraf</groupId>
            <artifactId>raf-framework-web-starter</artifactId>
        </dependency>
        <dependency>
            <groupId>io.github.jerryraf</groupId>
            <artifactId>raf-framework-datasource-starter</artifactId>
        </dependency>
        <dependency>
            <groupId>io.github.jerryraf</groupId>
            <artifactId>raf-framework-mybatis-starter</artifactId>
        </dependency>

        <!-- Database Driver (not managed by framework) -->
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
        </dependency>

        <!-- H2 for dev profile -->
        <dependency>
            <groupId>com.h2database</groupId>
            <artifactId>h2</artifactId>
            <scope>runtime</scope>
        </dependency>

        <!-- Validation -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>

        <!-- Test -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
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

- [ ] **Step 3: 检查并修正 Java 文件包名**

检查所有 Java 文件的包名，将 `com.github.raf.examples.mybatis` 改为 `io.github.jerryraf.examples.mybatis`（`DataSourceConfig.java` 已是正确包名，跳过）：

涉及文件（检查后按需修改）：
- `MybatisExampleApplication.java`
- `controller/UserController.java`
- `service/UserService.java`
- `dao/UserMapper.java`
- `entity/User.java`
- `config/MyBatisConfig.java`

- [ ] **Step 4: 修正 bootstrap.yml 中的包名引用**

文件路径：`examples/raf-example-mybatis-starter/src/main/resources/bootstrap.yml`

将：
```yaml
mybatis:
  type-aliases-package: io.github.jerryraf.examples.mybatis.entity
```

确认已是正确包名（如果是旧包名则修正）。同时修改：
```yaml
spring:
  application:
    name: raf-example-mybatis-starter
```

- [ ] **Step 5: 验证编译**

```bash
cd "D:/Framework/raf-framework/examples/raf-example-mybatis-starter"
mvn compile -s "D:/Program Files/apache-maven-3.9.10/conf/settings-raf.xml" -Dmaven.repo.local="D:/data/repository/local"
```

Expected: `BUILD SUCCESS`

- [ ] **Step 6: 验证 dev profile 启动**

```bash
cd "D:/Framework/raf-framework/examples/raf-example-mybatis-starter"
mvn spring-boot:run -Dspring-boot.run.profiles=dev \
  -s "D:/Program Files/apache-maven-3.9.10/conf/settings-raf.xml" \
  -Dmaven.repo.local="D:/data/repository/local"
```

Expected: 应用正常启动，无报错。

- [ ] **Step 7: Commit**

```bash
cd "D:/Framework/raf-framework"
git add -A
git commit -m "refactor(examples): rename raf-example-mybatis to raf-example-mybatis-starter, unify pom and package name"
```

---

## Phase 1 完成验证

- [ ] **验证 examples 目录结构**

```bash
ls "D:/Framework/raf-framework/examples/"
```

Expected:
```
raf-example-dubbo-starter
raf-example-gateway-starter
raf-example-mybatis-starter
raf-example-redis-starter
raf-example-web-starter
```

- [ ] **全量编译验证**

```bash
cd "D:/Framework/raf-framework/examples/raf-example-web-starter"
mvn compile -s "D:/Program Files/apache-maven-3.9.10/conf/settings-raf.xml" -Dmaven.repo.local="D:/data/repository/local"

cd "D:/Framework/raf-framework/examples/raf-example-redis-starter"
mvn compile -s "D:/Program Files/apache-maven-3.9.10/conf/settings-raf.xml" -Dmaven.repo.local="D:/data/repository/local"
```

Expected: 均 `BUILD SUCCESS`
