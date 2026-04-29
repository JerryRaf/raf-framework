# 版本升级指南

## 2.x → 3.x 升级指南

### 环境要求变更

| 项目 | 2.x | 3.x |
|---|---|---|
| JDK | 11+ | **17+** |
| Spring Boot | 2.x | **3.x** |
| Jakarta EE | javax.* | **jakarta.*** |

### Breaking Changes

#### 1. Jakarta EE 命名空间迁移

Spring Boot 3.x 将 `javax.*` 包迁移到 `jakarta.*`，影响所有使用 Servlet API 的代码：

```java
// 2.x
import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotBlank;

// 3.x
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotBlank;
```

**迁移方式**：使用 IDE 的全局替换，或 OpenRewrite 自动迁移工具。

#### 2. 配置前缀变更

| 功能 | 2.x 配置前缀 | 3.x 配置前缀 |
|---|---|---|
| 异步线程池 | `raf.executor` | `raf.async` |
| 请求日志 | `raf.log.enabled` | `raf.log.level`（枚举值） |

**异步线程池迁移示例**：

```yaml
# 2.x
raf:
  executor:
    enabled: true
    corePoolSize: 10
    maxPoolSize: 50

# 3.x
raf:
  async:
    enabled: true
    pools:
      default:
        coreSize: 10
        maxSize: 50
        queueCapacity: 100
```

#### 3. 自动配置注册文件变更

Spring Boot 3.x 使用新的自动配置注册机制：

```
# 2.x
META-INF/spring.factories

# 3.x
META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

框架已处理此变更，应用层无需关注。

#### 4. Elasticsearch 客户端变更

2.x 使用 `RestHighLevelClient`（已废弃），3.x 迁移到 `ElasticsearchClient`：

```java
// 2.x
@Autowired
private RestHighLevelClient restHighLevelClient;

// 3.x
@Autowired
private ElasticsearchClient esClient;
```

### 升级步骤

1. **升级 JDK** 到 17+
2. **升级 Spring Boot** 到 3.4.x
3. **替换 javax.* 为 jakarta.*** （全局替换）
4. **更新框架版本** 到 3.0.0
5. **迁移配置前缀**（参考上方 Breaking Changes）
6. **运行测试**，修复编译错误和运行时问题

### 常见升级问题

**Q: 升级后 `@Validated` 不生效？**

A: Spring Boot 3.x 中 `@Validated` 需要配合 `spring-boot-starter-validation` 依赖，确认已引入。

**Q: 升级后 Druid 监控页面 404？**

A: Spring Boot 3.x 对 Servlet 注册方式有变化，需要更新 `StatViewServlet` 的注册方式，参考 Druid 官方 Spring Boot 3 适配文档。
