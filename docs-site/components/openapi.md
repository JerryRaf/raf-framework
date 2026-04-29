# API 文档（openapi-starter）

## 功能概述

- **Springdoc OpenAPI**：自动生成 OpenAPI 3.0 规范文档
- **Knife4j UI**：增强版 Swagger UI，支持调试、导出
- **接口分组**：按模块分组展示接口
- **安全认证**：支持 Bearer Token 认证配置

## 配置项

| 配置键 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `springdoc.api-docs.enabled` | boolean | `true` | 是否启用 API 文档 |
| `springdoc.swagger-ui.enabled` | boolean | `true` | 是否启用 Swagger UI |
| `springdoc.swagger-ui.path` | string | `/swagger-ui.html` | Swagger UI 访问路径 |
| `knife4j.enable` | boolean | `false` | 是否启用 Knife4j 增强 |
| `knife4j.setting.language` | string | `zh_cn` | 界面语言 |

## 快速接入

```xml
<dependency>
    <groupId>io.github.jerryraf</groupId>
    <artifactId>raf-framework-openapi-starter</artifactId>
</dependency>
```

```yaml
springdoc:
  api-docs:
    enabled: true
    path: /v3/api-docs
  swagger-ui:
    enabled: true
    path: /swagger-ui.html

knife4j:
  enable: true
  setting:
    language: zh_cn
    enable-swagger-models: true
    enable-document-manage: true
```

## 核心用法

### 接口文档注解

```java
@Tag(name = "用户管理", description = "用户相关接口")
@RestController
@RequestMapping("/api/user")
public class UserController {

    @Operation(summary = "获取用户详情", description = "根据用户 ID 查询用户信息")
    @Parameter(name = "id", description = "用户 ID", required = true)
    @GetMapping("/{id}")
    @ResponseResult
    public User getUser(@PathVariable Long id) {
        return userService.getById(id);
    }

    @Operation(summary = "创建用户")
    @PostMapping
    @ResponseResult
    public User createUser(@RequestBody @Valid CreateUserRequest request) {
        return userService.create(request);
    }
}
```

### 请求/响应模型注解

```java
@Schema(description = "创建用户请求")
@Data
public class CreateUserRequest {

    @Schema(description = "用户名", example = "张三", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "用户名不能为空")
    private String name;

    @Schema(description = "邮箱", example = "zhangsan@example.com")
    @Email(message = "邮箱格式不正确")
    private String email;
}
```

### 配置 Bearer Token 认证

```java
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Your Service API")
                .version("1.0.0")
                .description("API 文档"))
            .addSecurityItem(new SecurityRequirement().addList("Bearer"))
            .components(new Components()
                .addSecuritySchemes("Bearer", new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")));
    }
}
```

## 常见问题

**Q: 生产环境如何关闭 API 文档？**

A: 设置 `springdoc.api-docs.enabled: false` 和 `springdoc.swagger-ui.enabled: false`，或通过 Spring Profile 控制。

**Q: Knife4j 访问地址是什么？**

A: 默认访问 `http://localhost:8080/doc.html`。

**Q: 接口在文档中不显示？**

A: 确认 Controller 类在 `springdoc.packages-to-scan` 配置的包路径下，或者没有被 `springdoc.paths-to-exclude` 排除。
