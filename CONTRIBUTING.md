# Contributing to RAF Framework

## Prerequisites

- JDK 17+
- Maven 3.8.8+

## 本地开发环境搭建

### 1. 启动依赖中间件

项目提供了开箱即用的 Docker Compose 配置，一键启动本地开发所需的全部中间件：

```bash
docker compose -f .github/dev/docker-compose.yml up -d
```

包含服务：Redis（6379）、MySQL（3306）、RabbitMQ（5672 / 管理页 15672）。

### 2. 克隆并构建

```bash
git clone https://github.com/jerryraf/raf-framework.git
cd raf-framework
mvn clean install -DskipTests
```

### 3. 运行示例项目

```bash
# 使用 H2 内存数据库，无需外部依赖
cd examples/raf-example-mybatis
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

## 代码风格

- **格式化**：使用 IntelliJ IDEA 默认代码格式化（`Ctrl+Alt+L`），提交前请格式化修改的文件。
- **编码**：所有文件统一 UTF-8，行尾 LF（`.editorconfig` 已配置，IDE 会自动应用）。
- **缩进**：Java 文件 4 空格，YAML/JSON/XML 文件 2 空格。
- **命名**：遵循阿里巴巴 Java 开发手册规范。
- **注释**：仅在逻辑不自明处添加注释，禁止无意义的 Javadoc 模板。

## 如何新增一个 Starter

新增 Starter 遵循以下 5 步标准流程：

**第 1 步：在 `raf-framework-starter/` 下创建子模块**

```
raf-framework-starter/
└── raf-framework-{name}-starter/
    └── pom.xml   # 仅声明对 raf-framework-core 的依赖
```

`pom.xml` 示例：

```xml
<dependency>
    <groupId>io.github.jerryraf</groupId>
    <artifactId>raf-framework-core</artifactId>
</dependency>
```

**第 2 步：在 `raf-framework-core` 中创建 Properties 类**

```java
@ConfigurationProperties(prefix = "raf.{name}")
public class {Name}Properties {
    /** 是否启用，默认 false（热插拔原则） */
    private boolean enabled = false;
    // 其他配置字段...
}
```

**第 3 步：创建 Config 类（条件注解 + Bean 注册）**

```java
@Configuration
@EnableConfigurationProperties({Name}Properties.class)
@ConditionalOnProperty(prefix = "raf.{name}", name = "enabled", havingValue = "true")
public class {Name}Config {
    @Bean
    public {Name}Service {name}Service({Name}Properties props) {
        return new {Name}Service(props);
    }
}
```

**第 4 步：注册到自动配置**

在 `raf-framework-core/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 末尾追加：

```
com.raf.framework.autoconfigure.{name}.{Name}Config
```

**第 5 步：编写 `ApplicationContextRunner` 单元测试**

```java
@Test
void enabledWhenPropertySet() {
    new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of({Name}Config.class))
        .withPropertyValues("raf.{name}.enabled=true")
        .run(ctx -> assertThat(ctx).hasSingleBean({Name}Service.class));
}

@Test
void disabledByDefault() {
    new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of({Name}Config.class))
        .run(ctx -> assertThat(ctx).doesNotHaveBean({Name}Service.class));
}
```

## Build and Test

```bash
# 完整构建（含测试）
mvn clean test --no-transfer-progress

# 只跑单模块测试（以 redis-starter 为例）
mvn test -pl raf-framework-core --no-transfer-progress \
    -Dtest="*Redis*,*Redisson*"

# 跳过测试快速安装
mvn clean install -DskipTests --no-transfer-progress
```

## Branch Naming

- `feature/<name>`
- `fix/<name>`
- `docs/<name>`
- `test/<name>`

## Commit Convention

Use Conventional Commits:

- `feat:` 新功能
- `fix:` Bug 修复
- `docs:` 文档变更
- `test:` 测试相关
- `refactor:` 重构（不影响功能）
- `chore:` 构建/工具链变更

示例：`feat(redis): add multi-level cache support`

## Testing Requirements

- New starter/configuration behavior must include `ApplicationContextRunner` tests.
- Behavior changes must include at least one failing-first test.

## Pull Request Process

1. Fork repository.
2. Create branch.
3. Add tests and docs.
4. Open PR with template completed.
