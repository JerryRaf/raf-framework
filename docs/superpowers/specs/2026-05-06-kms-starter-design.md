# raf-framework-kms-starter 设计文档

**日期**: 2026-05-06  
**作者**: Jerry  
**状态**: 待实现

---

## 1. 背景与目标

`co-demo` 项目中的 `co-common-kms` 模块实现了多云 KMS（Key Management Service）集成，支持 Google Cloud KMS、阿里云 KMS、华为云 KMS 和本地模式，并与 Jasypt 配置加密深度集成。

该能力具有通用性，适合封装为 raf-framework 的标准 starter，供所有业务项目复用。

**目标**：
1. 将 `co-common-kms` 的核心能力封装为 `raf-framework-kms-starter`
2. 设计为泛化（无业务枚举耦合），通过 `String alias` 驱动密钥操作
3. 同步更新示例项目和文档
4. 将 `co-demo` 的 `co-common-kms` 模块迁移为引用新 starter 的薄适配层

---

## 2. 架构设计

### 2.1 模块位置

```
raf-framework-starter/
└── raf-framework-kms-starter/          # 新增
```

与 `raf-framework-redis-starter`、`raf-framework-sentry-starter` 等平级。

### 2.2 包结构

```
src/main/java/com/raf/framework/kms/
├── KmsAutoConfiguration.java           # 主自动配置入口
├── properties/
│   └── KmsProperties.java              # 配置属性，前缀 raf.kms
├── core/
│   └── CryptoManager.java              # 核心管理器，Map<String,String> 密钥缓存
├── provider/
│   ├── KmsService.java                 # 统一 KMS 接口
│   ├── LocalKmsService.java            # 本地模式（明文，开发/测试用）
│   ├── AliyunKmsService.java           # 阿里云 KMS
│   ├── HuaweiKmsService.java           # 华为云 KMS
│   └── GoogleKmsService.java           # Google Cloud KMS
└── jasypt/
    ├── KmsJasyptProperties.java        # jasypt.encryptor 配置属性
    └── KmsJasyptConfig.java            # KMS 密钥作为 Jasypt 密码（条件启用）

src/main/resources/META-INF/spring/
└── org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

### 2.3 核心设计原则

- **泛化**：`CryptoManager` 不依赖任何业务枚举，全部通过 `String alias` 操作
- **热插拔**：`raf.kms.enabled=true` 启用，默认关闭
- **Provider 条件装配**：通过 `@ConditionalOnProperty(name="raf.kms.provider", havingValue="xxx")` 按需加载云 SDK
- **云 SDK 可选依赖**：所有云厂商 SDK 设为 `optional=true`，业务项目按需引入
- **Jasypt 集成可选**：`raf.kms.jasypt.enabled=true` 显式启用

---

## 3. 配置属性设计

配置前缀：`raf.kms`（替代原 `app.security`）

```yaml
raf:
  kms:
    enabled: true
    provider: LOCAL          # LOCAL | ALIYUN | HUAWEI | GOOGLE

    # 正式通道密钥（ALIYUN/HUAWEI/GOOGLE 使用）
    keys:
      aes-biz-key:
        key-id: "acs:kms:cn-hangzhou:xxx/key-id"
        cipher-text: "Base64EncodedCipherText=="
        description: "业务数据加密密钥"
      aes-config-key:
        key-id: "acs:kms:cn-hangzhou:xxx/key-id"
        cipher-text: "Base64EncodedCipherText=="
        description: "配置数据加密密钥"

    # 本地模式密钥（LOCAL 使用，明文存储）
    local-keys:
      aes-biz-key:
        plain-text: "your-32-byte-aes-key-here-123456"
      aes-config-key:
        plain-text: "your-32-byte-config-key-here-123"

    # 阿里云配置（provider=ALIYUN 时必填）
    aliyun:
      region-id: cn-hangzhou
      access-key-id: "your-ak"
      access-key-secret: "your-sk"

    # 华为云配置（provider=HUAWEI 时必填）
    huawei:
      region: cn-north-4
      access-key: "your-ak"
      secret-key: "your-sk"
      project-id: "your-project-id"

    # Google Cloud 配置（provider=GOOGLE 时必填）
    google:
      auth-mode: ADC           # ADC（云内）| JSON（云外，使用服务账号文件）
      service-account-json-path: "/path/to/sa.json"
      proxy:
        enabled: false
        host: 127.0.0.1
        port: 10809

    # Jasypt 集成（可选）
    jasypt:
      enabled: false
      key-alias: "aes-config-key"   # 用哪个 alias 的密钥作为 Jasypt 密码
```

---

## 4. 核心类设计

### 4.1 KmsProperties

```java
@ConfigurationProperties(prefix = "raf.kms")
public class KmsProperties {
    private boolean enabled = false;
    private String provider = "LOCAL";
    private Map<String, KeyConfig> keys = new HashMap<>();
    private Map<String, KeyConfig> localKeys = new HashMap<>();
    private AliyunConfig aliyun = new AliyunConfig();
    private HuaweiConfig huawei = new HuaweiConfig();
    private GoogleConfig google = new GoogleConfig();
    private JasyptConfig jasypt = new JasyptConfig();

    // 内部类：KeyConfig, AliyunConfig, HuaweiConfig, GoogleConfig, ProxyConfig, JasyptConfig
}
```

### 4.2 KmsService 接口

```java
public interface KmsService {
    Map<String, String> resolveKeys(KmsProperties properties);
    String decrypt(String keyId, String cipherText);
    String encrypt(String keyId, String plainText);
}
```

### 4.3 CryptoManager（泛化核心）

```java
@Component
public class CryptoManager {
    private final KmsService kmsService;
    private final KmsProperties properties;
    // 密钥缓存：alias -> plainText
    private final Map<String, String> keyCache = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        // 支持 FORCE_EMERGENCY_MODE 环境变量（紧急模式直接读 local-keys）
        // 正常模式：带重试从 KMS 加载
    }

    // 业务 API
    public String encrypt(String plainText, String keyAlias);
    public String decrypt(String cipherText, String keyAlias);
    public String generateIndex(String plainText, String keyAlias);
    public String getRawKey(String keyAlias);
}
```

**紧急模式（Break Glass）**：`FORCE_EMERGENCY_MODE=true` 时，直接读取 `local-keys` 的 `plain-text`，无需额外环境变量，与业务无关。

### 4.4 KmsJasyptConfig（条件启用）

```java
@Configuration
@ConditionalOnProperty(name = "raf.kms.jasypt.enabled", havingValue = "true")
@EnableEncryptableProperties
public class KmsJasyptConfig {
    // 从 CryptoManager 获取 raf.kms.jasypt.key-alias 对应的密钥
    // 作为 Jasypt PooledPBEStringEncryptor 的密码
}
```

### 4.5 KmsAutoConfiguration

```java
@AutoConfiguration
@ConditionalOnProperty(name = "raf.kms.enabled", havingValue = "true")
@EnableConfigurationProperties(KmsProperties.class)
public class KmsAutoConfiguration {
    // 根据 provider 条件装配对应的 KmsService Bean
    // 装配 CryptoManager Bean
}
```

---

## 5. 依赖管理

### 5.1 raf-framework-dependencies 变更

补充 `spring-retry` 版本声明（其他 KMS SDK 版本已存在）：

```xml
<spring-retry.version>2.0.11</spring-retry.version>
```

### 5.2 raf-framework-kms-starter/pom.xml 依赖策略

| 依赖 | scope | optional | 说明 |
|------|-------|----------|------|
| raf-framework-core | compile | false | 必选，提供 CryptoUtil |
| jasypt-spring-boot | compile | false | 必选，Jasypt 集成 |
| spring-retry | compile | false | 必选，重试机制 |
| google-cloud-kms | compile | true | 按需引入 |
| google-auth-library-oauth2-http | compile | true | Google 认证 |
| google-auth-library-credentials | compile | true | Google 凭证类型 |
| grpc-netty | compile | true | Google gRPC 传输 |
| aliyun-java-sdk-core | compile | true | 阿里云 |
| aliyun-java-sdk-kms | compile | true | 阿里云 KMS |
| huaweicloud-sdk-core | compile | true | 华为云 |
| huaweicloud-sdk-kms | compile | true | 华为云 KMS |

---

## 6. 示例项目

新增 `examples/raf-example-kms-starter`，演示：
- LOCAL 模式快速启动（无需云账号）
- `CryptoManager` 加解密 API 调用
- Jasypt 集成（配置文件中使用 `ENC(...)` 加密敏感配置）

文档位置：`docs-site/examples/raf-example-kms-starter.md`

---

## 7. co-demo 迁移方案

### 7.1 迁移步骤

1. `co-common-kms/pom.xml`：删除所有 KMS SDK 依赖，引入 `raf-framework-kms-starter`，按实际 provider 补充对应云 SDK
2. 删除以下文件（由 starter 提供）：
   - `provider/KmsService.java`
   - `provider/LocalKmsService.java`
   - `provider/AliyunKmsService.java`
   - `provider/HuaweiKmsService.java`
   - `provider/GoogleKmsService.java`
   - `core/CryptoManager.java`
   - `config/SecretProperties.java`
   - `jasypt/JasyptProperties.java`
   - `jasypt/KmsJasyptConfig.java`
3. **保留** `config/CryptoKey.java`（业务枚举，不动）
4. 配置文件前缀：`app.security` → `raf.kms`
5. 调用方式适配：`cryptoManager.encrypt(text, CryptoKey.BIZ.getAlias())`（原来传枚举，现在传 alias 字符串）

### 7.2 迁移后 co-common-kms 结构

```
co-common-kms/
├── pom.xml                             # 只依赖 raf-framework-kms-starter
└── src/main/java/com/company/common/kms/
    └── config/
        └── CryptoKey.java              # 业务密钥枚举（保留）
```

---

## 8. 文档更新

- 新增 `docs-site/examples/raf-example-kms-starter.md`
- 更新 `CLAUDE.md` 模块架构表（新增 `raf-framework-kms-starter` 行）
- 更新 `examples/README.md`（新增示例说明）

---

## 9. 不在本次范围内

- KMS 密钥轮换（Key Rotation）
- 运行时动态刷新密钥
- 字段级加密注解（`@Encrypted`）
