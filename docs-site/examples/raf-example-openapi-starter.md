# raf-example-openapi-starter

> 演示 `raf-framework-web-starter` 集成 Springdoc OpenAPI 3 的核心能力：自动生成 API 文档、Knife4j 增强 UI、接口分组、Bearer Token 认证配置。

## 功能概述

- **Springdoc OpenAPI 3**：自动扫描 Controller 生成 OpenAPI 3.0 规范文档
- **Knife4j UI**：增强版 Swagger UI，支持调试、导出、离线文档
- **接口分组**：按模块分组展示接口
- **安全认证**：支持 Bearer Token 认证配置
- **请求/响应模型**：`@Schema` 注解自动生成字段说明

## 快速接入

### 1. 引入依赖

```xml
<dependency>
    <groupId>io.github.jerryraf</groupId>
    <artifactId>raf-framework-web-starter</artifactId>
</dependency>
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
</dependency>
<!-- 可选：Knife4j 增强 UI -->
<dependency>
    <groupId>com.github.xiaoymin</groupId>
    <artifactId>knife4j-openapi3-jakarta-spring-boot-starter</artifactId>
</dependency>
```

### 2. 配置（`application.yml`）

```yaml
spring:
  application:
    name: raf-example-openapi-starter

server:
  port: 8080

springdoc:
  api-docs:
    enabled: true
    path: /v3/api-docs
  swagger-ui:
    enabled: true
    path: /swagger-ui.html
    tags-sorter: alpha
    operations-sorter: alpha

knife4j:
  enable: true
  setting:
    language: zh_cn
    enable-swagger-models: true
    enable-document-manage: true
    swagger-model-name: 数据模型
```

访问地址：
- Swagger UI：`http://localhost:8080/swagger-ui.html`
- Knife4j UI：`http://localhost:8080/doc.html`
- OpenAPI JSON：`http://localhost:8080/v3/api-docs`

## 配置项详解

| 配置键 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `springdoc.api-docs.enabled` | boolean | `true` | 是否启用 API 文档 |
| `springdoc.api-docs.path` | string | `/v3/api-docs` | API 文档 JSON 路径 |
| `springdoc.swagger-ui.enabled` | boolean | `true` | 是否启用 Swagger UI |
| `springdoc.swagger-ui.path` | string | `/swagger-ui.html` | Swagger UI 访问路径 |
| `springdoc.packages-to-scan` | string | — | 扫描的包路径（不配置则扫描全部） |
| `springdoc.paths-to-exclude` | list | — | 排除的路径 |
| `knife4j.enable` | boolean | `false` | 是否启用 Knife4j 增强 |
| `knife4j.setting.language` | string | `zh_cn` | 界面语言 |

## 核心用法

### OpenAPI 全局配置

```java
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("RAF Framework 示例 API")
                .version("1.0.0")
                .description("演示 Springdoc OpenAPI 3 与 raf-framework-web-starter 集成")
                .contact(new Contact()
                    .name("RAF Framework Team")
                    .url("https://github.com/jerryraf/raf-framework"))
                .license(new License()
                    .name("Apache 2.0")
                    .url("https://www.apache.org/licenses/LICENSE-2.0")))
            // 配置 Bearer Token 认证
            .addSecurityItem(new SecurityRequirement().addList("Bearer"))
            .components(new Components()
                .addSecuritySchemes("Bearer", new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
                    .description("在此输入 JWT Token（不需要 Bearer 前缀）")));
    }
}
```

### Controller 注解

```java
@Tag(name = "用户管理", description = "用户 CRUD 接口")
@RestController
@RequestMapping("/api/users")
@ResponseResult
@RequiredArgsConstructor
public class UserApiController {

    private final Map<Long, UserDto> store = new ConcurrentHashMap<>();
    private final AtomicLong idGen = new AtomicLong(1);

    @Operation(summary = "获取用户详情", description = "根据用户 ID 查询用户信息")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @ApiResponse(responseCode = "404", description = "用户不存在")
    @GetMapping("/{id}")
    public UserDto getUser(
        @Parameter(description = "用户 ID", required = true, example = "1")
        @PathVariable Long id
    ) {
        UserDto user = store.get(id);
        if (user == null) {
            throw new BusinessException(UserErrorCode.USER_NOT_FOUND);
        }
        return user;
    }

    @Operation(summary = "创建用户")
    @PostMapping
    public UserDto createUser(
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "用户创建请求",
            required = true
        )
        @RequestBody @Valid UserCreateReq req
    ) {
        Long id = idGen.getAndIncrement();
        UserDto user = UserDto.builder()
            .id(id)
            .username(req.getUsername())
            .email(req.getEmail())
            .age(req.getAge())
            .build();
        store.put(id, user);
        return user;
    }

    @Operation(summary = "获取用户列表")
    @GetMapping
    public List<UserDto> listUsers() {
        return new ArrayList<>(store.values());
    }

    @Operation(summary = "删除用户")
    @DeleteMapping("/{id}")
    public void deleteUser(
        @Parameter(description = "用户 ID", required = true)
        @PathVariable Long id
    ) {
        if (store.remove(id) == null) {
            throw new BusinessException(UserErrorCode.USER_NOT_FOUND);
        }
    }
}
```

### 请求/响应模型注解

```java
@Schema(description = "用户信息")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {

    @Schema(description = "用户 ID", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @Schema(description = "用户名", example = "alice", requiredMode = Schema.RequiredMode.REQUIRED)
    private String username;

    @Schema(description = "邮箱", example = "alice@example.com")
    private String email;

    @Schema(description = "年龄", example = "25", minimum = "0", maximum = "150")
    private Integer age;
}

@Schema(description = "创建用户请求")
@Data
public class UserCreateReq {

    @Schema(description = "用户名", example = "alice", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "用户名不能为空")
    @Size(min = 2, max = 32, message = "用户名长度 2-32 位")
    private String username;

    @Schema(description = "邮箱", example = "alice@example.com")
    @Email(message = "邮箱格式不正确")
    private String email;

    @Schema(description = "年龄", example = "25", minimum = "0", maximum = "150")
    @Min(value = 0, message = "年龄不能为负数")
    @Max(value = 150, message = "年龄不能超过 150")
    private Integer age;
}
```

### 接口分组

```java
@Configuration
public class OpenApiGroupConfig {

    @Bean
    public GroupedOpenApi userApi() {
        return GroupedOpenApi.builder()
            .group("用户管理")
            .pathsToMatch("/api/users/**")
            .build();
    }

    @Bean
    public GroupedOpenApi orderApi() {
        return GroupedOpenApi.builder()
            .group("订单管理")
            .pathsToMatch("/api/orders/**")
            .build();
    }
}
```

## 示例项目结构

```
raf-example-openapi-starter/
├── src/main/java/io/github/jerryraf/examples/openapi/
│   ├── OpenApiExampleApplication.java
│   ├── config/
│   │   └── OpenApiConfig.java          # OpenAPI 全局配置（Bearer Token）
│   ├── controller/
│   │   └── UserApiController.java      # 带完整注解的 Controller
│   └── dto/
│       ├── UserDto.java                # @Schema 注解响应模型
│       └── UserCreateReq.java          # @Schema 注解请求模型
└── src/main/resources/
    └── application.yml
```

## 最佳实践

1. **生产环境关闭文档**：通过 Profile 控制，生产环境设置 `springdoc.api-docs.enabled: false`
2. **接口分组**：按业务模块分组，避免所有接口堆在一起
3. **必填字段标注**：使用 `requiredMode = Schema.RequiredMode.REQUIRED` 明确标注必填字段
4. **示例值**：为每个字段提供 `example`，方便测试人员快速填写
5. **安全配置**：配置 Bearer Token 认证，方便在文档页面直接测试需要鉴权的接口
6. **排除内部接口**：使用 `springdoc.paths-to-exclude` 排除 `/actuator/**` 等内部接口

## 常见问题

**Q: 生产环境如何关闭 API 文档？**

A: 在 `application-prod.yml` 中设置：

```yaml
springdoc:
  api-docs:
    enabled: false
  swagger-ui:
    enabled: false
```

**Q: Knife4j 访问地址是什么？**

A: 默认访问 `http://localhost:8080/doc.html`。

**Q: 接口在文档中不显示？**

A: 确认 Controller 类在 `springdoc.packages-to-scan` 配置的包路径下，或者没有被 `springdoc.paths-to-exclude` 排除。

**Q: 如何在文档中显示枚举值说明？**

A: 在枚举类上加 `@Schema(description = "状态：1-正常 0-禁用")`，或在字段上使用 `allowableValues`：

```java
@Schema(description = "状态", allowableValues = {"0", "1"}, example = "1")
private Integer status;
```

**Q: 如何隐藏某个接口不在文档中显示？**

A: 在方法或类上加 `@Hidden` 注解：

```java
@Hidden
@GetMapping("/internal/health")
public String internalHealth() { ... }
```
