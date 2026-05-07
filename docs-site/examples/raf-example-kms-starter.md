# raf-example-kms-starter

> 演示 `raf-framework-kms-starter` 的核心能力：多 Provider 密钥管理、字段级加解密、HMAC 索引生成、Jasypt 配置加密集成。

## 功能概述

`raf-framework-kms-starter` 提供：

- **多 Provider 支持**：LOCAL（本地密钥）、ALIYUN（阿里云 KMS）、HUAWEI（华为云 DEW）、GOOGLE（Google Cloud KMS）
- **CryptoManager**：统一加解密 API，屏蔽底层 Provider 差异
- **HMAC 索引**：对加密字段生成可搜索的 HMAC 摘要，支持密文字段的等值查询
- **Jasypt 集成**：复用 KMS 密钥对 `application.yml` 中的敏感配置进行加密（`ENC(...)`）
- **紧急降级模式**：云 KMS 不可用时自动切换本地密钥，保障业务连续性

## 快速接入

### 1. 引入依赖

```xml
<!-- KMS 密钥管理与字段加密 -->
<dependency>
    <groupId>io.github.jerryraf</groupId>
    <artifactId>raf-framework-kms-starter</artifactId>
</dependency>
```

### 2. 配置（`application.yml`）—— LOCAL 模式

LOCAL 模式使用本地配置的 AES 密钥，无需外部 KMS 服务，适合开发和测试环境。

```yaml
spring:
  application:
    name: raf-example-kms-starter

raf:
  kms:
    enabled: true
    provider: LOCAL          # 使用本地密钥，无需云服务
    local-keys:
      default:               # keyAlias，可定义多个
        plain-text: ${KMS_DEFAULT_SECRET:your-32-byte-aes-key-here!!!!!}
      user-pii:              # 用于用户隐私字段（手机号、身份证等）
        plain-text: ${KMS_PII_SECRET:another-32-byte-aes-key-here!!!!}
```

### 3. 启动类

```java
@SpringBootApplication
public class KmsExampleApplication {
    public static void main(String[] args) {
        SpringApplication.run(KmsExampleApplication.class, args);
    }
}
```

## CryptoManager API

注入 `CryptoManager` 即可使用所有加解密能力，无需关心底层 Provider。

```java
@Service
@RequiredArgsConstructor
public class UserService {

    private final CryptoManager cryptoManager;

    // ...
}
```

### encrypt — 加密

```java
// 使用 "user-pii" 密钥别名加密手机号
String cipherText = cryptoManager.encrypt("13800138000", "user-pii");
// 返回 Base64 编码的密文，如：AES:iv_base64:cipher_base64
```

### decrypt — 解密

```java
// 使用相同密钥别名解密
String plainText = cryptoManager.decrypt(cipherText, "user-pii");
// 返回原始明文：13800138000
```

### generateIndex — 生成 HMAC 索引

加密字段无法直接用于数据库等值查询，`generateIndex` 生成确定性 HMAC 摘要，存储为独立索引列，实现密文字段的精确搜索。

```java
// 对手机号生成 HMAC 索引（相同输入 + 相同密钥 → 相同摘要）
String index = cryptoManager.generateIndex("13800138000", "user-pii");
// 存入 phone_index 列，查询时对入参同样调用 generateIndex 后匹配

// 查询示例
public UserDO findByPhone(String phone) {
    String phoneIndex = cryptoManager.generateIndex(phone, "user-pii");
    return userMapper.selectByPhoneIndex(phoneIndex);
}
```

> HMAC 索引是单向的，无法从索引反推明文，安全性与加密字段相同。

### getRawKey — 获取原始密钥

在需要与第三方系统共享密钥材料的场景下使用，日常业务不建议调用。

```java
// 获取指定别名的原始密钥字节（谨慎使用）
byte[] rawKey = cryptoManager.getRawKey("default");
```

### 完整业务示例

```java
@Service
@RequiredArgsConstructor
public class UserService {

    private final CryptoManager cryptoManager;
    private final UserMapper userMapper;

    /** 保存用户，手机号加密存储 */
    public void saveUser(UserCreateReq req) {
        UserDO user = new UserDO();
        user.setName(req.getName());
        // 加密手机号
        user.setPhone(cryptoManager.encrypt(req.getPhone(), "user-pii"));
        // 生成手机号索引，用于后续查询
        user.setPhoneIndex(cryptoManager.generateIndex(req.getPhone(), "user-pii"));
        userMapper.insert(user);
    }

    /** 按手机号查询用户 */
    public UserDTO findByPhone(String phone) {
        String phoneIndex = cryptoManager.generateIndex(phone, "user-pii");
        UserDO user = userMapper.selectByPhoneIndex(phoneIndex);
        if (user == null) {
            return null;
        }
        UserDTO dto = new UserDTO();
        dto.setName(user.getName());
        // 解密手机号后返回
        dto.setPhone(cryptoManager.decrypt(user.getPhone(), "user-pii"));
        return dto;
    }
}
```

## 云 Provider 配置

### ALIYUN — 阿里云 KMS

```yaml
raf:
  kms:
    enabled: true
    provider: ALIYUN
    aliyun:
      region-id: cn-hangzhou
      access-key-id: ${ALIYUN_ACCESS_KEY_ID}
      access-key-secret: ${ALIYUN_ACCESS_KEY_SECRET}
    keys:
      default:
        key-id: ${ALIYUN_KMS_KEY_ID_DEFAULT}       # 阿里云 KMS CMK ID
        cipher-text: ${ALIYUN_KMS_CIPHER_DEFAULT}  # 用 CMK 加密后的数据密钥密文（Base64）
      user-pii:
        key-id: ${ALIYUN_KMS_KEY_ID_PII}
        cipher-text: ${ALIYUN_KMS_CIPHER_PII}
```

阿里云 KMS 使用信封加密：`key-id` 是 CMK ID，`cipher-text` 是用该 CMK 加密后的数据密钥密文，框架启动时调用 KMS 解密得到明文数据密钥。

### HUAWEI — 华为云 DEW

```yaml
raf:
  kms:
    enabled: true
    provider: HUAWEI
    huawei:
      region: cn-north-4
      project-id: ${HUAWEI_PROJECT_ID}
      access-key: ${HUAWEI_ACCESS_KEY}
      secret-key: ${HUAWEI_SECRET_KEY}
    keys:
      default:
        key-id: ${HUAWEI_KMS_KEY_ID_DEFAULT}
        cipher-text: ${HUAWEI_KMS_CIPHER_DEFAULT}
      user-pii:
        key-id: ${HUAWEI_KMS_KEY_ID_PII}
        cipher-text: ${HUAWEI_KMS_CIPHER_PII}
```

### GOOGLE — Google Cloud KMS

Google Cloud KMS 支持两种认证模式：

**模式一：应用默认凭证（ADC，推荐）**

在 GKE 或配置了 Workload Identity 的环境中，无需显式配置凭证文件。

```yaml
raf:
  kms:
    enabled: true
    provider: GOOGLE
    google:
      auth-mode: ADC   # 默认值，使用应用默认凭证
    keys:
      default:
        # key-id 为完整资源路径：projects/{project}/locations/{location}/keyRings/{ring}/cryptoKeys/{key}
        key-id: ${GCP_KMS_KEY_ID_DEFAULT}
        cipher-text: ${GCP_KMS_CIPHER_DEFAULT}
      user-pii:
        key-id: ${GCP_KMS_KEY_ID_PII}
        cipher-text: ${GCP_KMS_CIPHER_PII}
```

**模式二：JSON 凭证文件**

本地开发或非 GCP 环境下，指定服务账号 JSON 文件路径。

```yaml
raf:
  kms:
    enabled: true
    provider: GOOGLE
    google:
      auth-mode: JSON
      service-account-json-path: /etc/secrets/gcp-kms-sa.json
    keys:
      default:
        key-id: ${GCP_KMS_KEY_ID_DEFAULT}
        cipher-text: ${GCP_KMS_CIPHER_DEFAULT}
      user-pii:
        key-id: ${GCP_KMS_KEY_ID_PII}
        cipher-text: ${GCP_KMS_CIPHER_PII}
```

## Jasypt 集成

KMS Starter 可接管 Jasypt 的加解密，使 `application.yml` 中的敏感配置（数据库密码、第三方密钥等）由 KMS 统一管理。

### 启用方式

```yaml
raf:
  kms:
    enabled: true
    provider: LOCAL
    jasypt:
      enabled: true          # 启用 Jasypt 集成
      key-alias: default     # 使用哪个 keyAlias 加解密配置项
    local-keys:
      default:
        plain-text: ${KMS_DEFAULT_SECRET:your-32-byte-aes-key-here!!!!!}
```

### 在配置文件中使用 ENC(...)

启用后，所有 `ENC(...)` 包裹的配置值在启动时自动解密：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/mydb
    username: root
    password: ENC(Base64EncodedCipherTextHere==)   # 加密后的数据库密码

third-party:
  api-key: ENC(AnotherEncryptedValueHere==)        # 加密后的第三方 API Key
```

### 生成加密值

使用框架提供的工具类生成 `ENC(...)` 中的密文：

```java
// 在测试或工具类中生成加密配置值
@Autowired
CryptoManager cryptoManager;

public void generateEncryptedConfig() {
    String encrypted = cryptoManager.encrypt("my-db-password", "default");
    System.out.println("ENC(" + encrypted + ")");
    // 将输出结果填入 application.yml 的 ENC(...) 中
}
```

## 紧急模式

### FORCE_EMERGENCY_MODE 环境变量

当云 KMS 服务不可用（网络故障、服务降级、密钥轮换期间）时，可通过环境变量强制切换到本地密钥模式，保障业务不中断。

```bash
# 启动时设置环境变量，强制使用本地密钥
export FORCE_EMERGENCY_MODE=true
java -jar raf-example-kms-starter.jar
```

或在 Docker/Kubernetes 中注入：

```yaml
# Kubernetes Deployment 示例
env:
  - name: FORCE_EMERGENCY_MODE
    value: "true"
```

### 使用场景

| 场景 | 说明 |
|---|---|
| 云 KMS 网络不可达 | 跨区域网络抖动导致 KMS API 超时 |
| KMS 服务维护窗口 | 云厂商计划内维护，临时切换本地密钥 |
| 密钥轮换过渡期 | 新旧密钥切换期间的平滑降级 |
| 本地开发调试 | 无需配置云凭证，直接使用本地密钥 |

### 紧急模式配置

紧急模式下，框架自动使用 `raf.kms.local-keys` 中配置的本地密钥，因此**即使使用云 Provider，也建议同时配置本地密钥作为降级备份**：

```yaml
raf:
  kms:
    enabled: true
    provider: ALIYUN          # 正常使用阿里云 KMS
    aliyun:
      region-id: cn-hangzhou
      access-key-id: ${ALIYUN_ACCESS_KEY_ID}
      access-key-secret: ${ALIYUN_ACCESS_KEY_SECRET}
    keys:
      default:
        key-id: ${ALIYUN_KMS_KEY_ID_DEFAULT}
        cipher-text: ${ALIYUN_KMS_CIPHER_TEXT_DEFAULT}
    # 同时配置本地密钥，供紧急模式降级使用
    local-keys:
      default:
        plain-text: ${KMS_EMERGENCY_SECRET:fallback-32-byte-aes-key-here!}
```

## 配置项速查

| 配置键 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `raf.kms.enabled` | boolean | `false` | 是否启用 KMS |
| `raf.kms.provider` | enum | `LOCAL` | Provider 类型：LOCAL / ALIYUN / HUAWEI / GOOGLE |
| `raf.kms.local-keys.{alias}.plain-text` | string | — | LOCAL 模式明文密钥（LOCAL 模式必填） |
| `raf.kms.keys.{alias}.key-id` | string | — | 云 KMS 密钥 ID（ALIYUN/HUAWEI/GOOGLE 模式必填） |
| `raf.kms.keys.{alias}.cipher-text` | string | — | 云 KMS 加密后的数据密钥密文（Base64） |
| `raf.kms.jasypt.enabled` | boolean | `false` | 是否启用 Jasypt 集成 |
| `raf.kms.jasypt.key-alias` | string | `default` | Jasypt 使用的密钥别名 |
| `raf.kms.aliyun.region-id` | string | — | 阿里云区域 ID |
| `raf.kms.aliyun.access-key-id` | string | — | 阿里云 AccessKey ID |
| `raf.kms.aliyun.access-key-secret` | string | — | 阿里云 AccessKey Secret |
| `raf.kms.huawei.region` | string | — | 华为云区域 |
| `raf.kms.huawei.project-id` | string | — | 华为云项目 ID |
| `raf.kms.huawei.access-key` | string | — | 华为云 AK |
| `raf.kms.huawei.secret-key` | string | — | 华为云 SK |
| `raf.kms.google.auth-mode` | string | `ADC` | Google 认证模式：ADC（云内）/ JSON（云外） |
| `raf.kms.google.service-account-json-path` | string | — | GCP 服务账号 JSON 文件路径（auth-mode=JSON 时使用） |
| `raf.kms.google.proxy.enabled` | boolean | `false` | 是否启用代理 |
| `raf.kms.google.proxy.host` | string | `127.0.0.1` | 代理主机 |
| `raf.kms.google.proxy.port` | int | `10809` | 代理端口 |

## 示例项目结构

```
raf-example-kms-starter/
├── src/main/java/io/github/jerryraf/examples/kms/
│   ├── KmsExampleApplication.java
│   ├── controller/
│   │   └── UserController.java         # 加密字段 CRUD 演示接口
│   ├── service/
│   │   └── UserService.java            # CryptoManager 使用示例
│   ├── mapper/
│   │   └── UserMapper.java             # 按 HMAC 索引查询
│   └── dto/
│       ├── UserCreateReq.java
│       └── UserDTO.java
└── src/main/resources/
    └── application.yml
```

## 最佳实践

1. **密钥别名语义化**：按数据分类命名，如 `user-pii`（用户隐私）、`payment`（支付信息），便于密钥轮换时精准替换
2. **HMAC 索引独立存储**：加密字段（如 `phone`）和索引字段（如 `phone_index`）分开存储，索引列加普通索引即可支持等值查询
3. **环境变量注入密钥**：本地密钥 `secret` 通过环境变量注入（`${KMS_SECRET}`），禁止硬编码在配置文件中
4. **云 Provider 配置降级**：生产环境使用云 KMS 时，同步配置 `local-keys`，确保 `FORCE_EMERGENCY_MODE` 可用
5. **Jasypt 密钥隔离**：Jasypt 集成建议使用独立的 `key-alias`，与业务加密密钥分开管理
6. **密钥轮换策略**：轮换密钥时先用新密钥重新加密存量数据，再切换配置，避免解密失败

## 常见问题

**Q: LOCAL 模式下 AES 密钥长度有要求吗？**

A: AES-128 需要 16 字节，AES-192 需要 24 字节，AES-256 需要 32 字节。建议统一使用 AES-256（32 字节），`secret` 不足时框架会自动填充，但建议显式提供足够长度的密钥。

**Q: 同一明文每次加密结果不同，如何做等值查询？**

A: 这是正常现象，AES-CBC/GCM 模式每次使用随机 IV，密文不同但解密结果相同。等值查询应使用 `generateIndex` 生成的 HMAC 索引，HMAC 对相同输入和密钥始终输出相同摘要。

**Q: 切换 Provider 后历史密文能否解密？**

A: 不能直接解密。切换 Provider 前需先用旧 Provider 解密所有存量数据，再用新 Provider 重新加密后存储。建议在低峰期执行数据迁移，迁移期间可同时保留两个 Provider 配置。

**Q: FORCE_EMERGENCY_MODE 下 Jasypt 配置能否正常解密？**

A: 可以，前提是 `raf.kms.local-keys` 中配置了与 `raf.kms.jasypt.key-alias` 对应的本地密钥，且该密钥与原始加密时使用的密钥相同。

**Q: Google Cloud KMS ADC 模式在本地开发如何配置？**

A: 执行 `gcloud auth application-default login` 后，ADC 会自动读取本地凭证。或者将服务账号 JSON 文件路径设置到环境变量 `GOOGLE_APPLICATION_CREDENTIALS` 中，无需在 `application.yml` 中指定 `credentials-file`。
