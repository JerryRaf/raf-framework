# raf-framework-kms-starter Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 co-common-kms 的多云 KMS 集成能力封装为 raf-framework 标准 starter，泛化设计（无业务枚举耦合），并将 co-demo 迁移为引用新 starter 的薄适配层。

**Architecture:** `raf-framework-kms-starter` 遵循 raf-framework starter 架构模式，通过 `raf.kms.enabled=true` 热插拔启用。`CryptoManager` 以 `Map<String,String>` 缓存密钥，全部通过 `String alias` 操作，不依赖任何业务枚举。四种 Provider（LOCAL/ALIYUN/HUAWEI/GOOGLE）通过 `@ConditionalOnProperty` 条件装配，云 SDK 均为 optional 依赖。Jasypt 集成通过 `raf.kms.jasypt.enabled=true` 显式启用。

**Tech Stack:** Java 17, Spring Boot 3.4.7, jasypt-spring-boot 3.0.5, spring-retry 2.0.11, Google Cloud KMS 2.50.0, Aliyun KMS SDK 2.16.6, Huawei Cloud KMS SDK 3.1.60

---

## File Map

### 新建文件（raf-framework）

| 文件 | 职责 |
|------|------|
| `raf-framework-starter/raf-framework-kms-starter/pom.xml` | starter 模块依赖声明 |
| `...kms/KmsAutoConfiguration.java` | 主自动配置，条件装配各 Provider |
| `...kms/properties/KmsProperties.java` | 配置属性，前缀 `raf.kms` |
| `...kms/provider/KmsService.java` | 统一 KMS 接口 |
| `...kms/provider/LocalKmsService.java` | 本地模式实现 |
| `...kms/provider/AliyunKmsService.java` | 阿里云 KMS 实现 |
| `...kms/provider/HuaweiKmsService.java` | 华为云 KMS 实现 |
| `...kms/provider/GoogleKmsService.java` | Google Cloud KMS 实现 |
| `...kms/core/CryptoManager.java` | 核心管理器，密钥缓存与业务 API |
| `...kms/jasypt/KmsJasyptProperties.java` | jasypt.encryptor 配置属性 |
| `...kms/jasypt/KmsJasyptConfig.java` | KMS 密钥作为 Jasypt 密码 |
| `META-INF/spring/AutoConfiguration.imports` | 自动配置注册 |
| `examples/raf-example-kms-starter/pom.xml` | 示例项目 |
| `examples/raf-example-kms-starter/.../KmsExampleApplication.java` | 示例启动类 |
| `examples/raf-example-kms-starter/.../CryptoController.java` | 示例 Controller |
| `examples/raf-example-kms-starter/src/main/resources/application.yml` | 示例配置 |
| `docs-site/examples/raf-example-kms-starter.md` | 示例文档 |

### 修改文件（raf-framework）

| 文件 | 变更 |
|------|------|
| `raf-framework-starter/pom.xml` | 新增 `raf-framework-kms-starter` module |
| `raf-framework-dependencies/pom.xml` | 新增 spring-retry 版本声明，补充 KMS SDK BOM 管理 |
| `examples/pom.xml` | 新增 `raf-example-kms-starter` module |

### 修改文件（co-demo）

| 文件 | 变更 |
|------|------|
| `co-common/co-common-kms/pom.xml` | 替换为引用 `raf-framework-kms-starter` |
| 删除 9 个 Java 文件 | provider/*, core/CryptoManager, config/SecretProperties, jasypt/* |
| 保留 `config/CryptoKey.java` | 业务枚举不动 |

---

## Task 1: 依赖版本管理

**Files:**
- Modify: `raf-framework-dependencies/pom.xml`

- [ ] **Step 1: 在 raf-framework-dependencies/pom.xml 的 `<properties>` 中补充 spring-retry 版本**

```xml
<!-- 在 <jasypt.boot.version>3.0.5</jasypt.boot.version> 之后添加 -->
<spring-retry.version>2.0.11</spring-retry.version>
```

- [ ] **Step 2: 在 `<dependencyManagement><dependencies>` 中补充 spring-retry 和 KMS SDK 的 BOM 条目**

在 jasypt-spring-boot 条目附近添加：

```xml
<!-- spring-retry -->
<dependency>
    <groupId>org.springframework.retry</groupId>
    <artifactId>spring-retry</artifactId>
    <version>${spring-retry.version}</version>
</dependency>

<!-- Google Cloud KMS -->
<dependency>
    <groupId>com.google.cloud</groupId>
    <artifactId>google-cloud-kms</artifactId>
    <version>${google-cloud-kms.version}</version>
</dependency>
<dependency>
    <groupId>com.google.auth</groupId>
    <artifactId>google-auth-library-oauth2-http</artifactId>
    <version>${google-auth-library.version}</version>
</dependency>
<dependency>
    <groupId>com.google.auth</groupId>
    <artifactId>google-auth-library-credentials</artifactId>
    <version>${google-auth-library.version}</version>
</dependency>
<dependency>
    <groupId>io.grpc</groupId>
    <artifactId>grpc-netty</artifactId>
    <version>${grpc.version}</version>
</dependency>

<!-- Aliyun KMS -->
<dependency>
    <groupId>com.aliyun</groupId>
    <artifactId>aliyun-java-sdk-core</artifactId>
    <version>${aliyun-java-core.version}</version>
</dependency>
<dependency>
    <groupId>com.aliyun</groupId>
    <artifactId>aliyun-java-sdk-kms</artifactId>
    <version>${aliyun-java-kms.version}</version>
</dependency>

<!-- Huawei Cloud KMS -->
<dependency>
    <groupId>com.huaweicloud.sdk</groupId>
    <artifactId>huaweicloud-sdk-core</artifactId>
    <version>${huaweicloud-sdk.version}</version>
</dependency>
<dependency>
    <groupId>com.huaweicloud.sdk</groupId>
    <artifactId>huaweicloud-sdk-kms</artifactId>
    <version>${huaweicloud-sdk.version}</version>
</dependency>
```

- [ ] **Step 3: 验证 pom.xml 语法**

```bash
cd D:/Framework/raf-framework
mvn help:effective-pom -pl raf-framework-dependencies -q
```

Expected: 无报错输出

- [ ] **Step 4: Commit**

```bash
git add raf-framework-dependencies/pom.xml
git commit -m "feat(deps): add spring-retry and KMS SDK version management"
```

---

## Task 2: 创建 starter 模块骨架

**Files:**
- Create: `raf-framework-starter/raf-framework-kms-starter/pom.xml`
- Modify: `raf-framework-starter/pom.xml`

- [ ] **Step 1: 创建目录结构**

```bash
mkdir -p "D:/Framework/raf-framework/raf-framework-starter/raf-framework-kms-starter/src/main/java/com/raf/framework/kms/properties"
mkdir -p "D:/Framework/raf-framework/raf-framework-starter/raf-framework-kms-starter/src/main/java/com/raf/framework/kms/provider"
mkdir -p "D:/Framework/raf-framework/raf-framework-starter/raf-framework-kms-starter/src/main/java/com/raf/framework/kms/core"
mkdir -p "D:/Framework/raf-framework/raf-framework-starter/raf-framework-kms-starter/src/main/java/com/raf/framework/kms/jasypt"
mkdir -p "D:/Framework/raf-framework/raf-framework-starter/raf-framework-kms-starter/src/main/resources/META-INF/spring"
mkdir -p "D:/Framework/raf-framework/raf-framework-starter/raf-framework-kms-starter/src/test/java/com/raf/framework/kms"
```

- [ ] **Step 2: 创建 pom.xml**

创建文件 `raf-framework-starter/raf-framework-kms-starter/pom.xml`：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>io.github.jerryraf</groupId>
        <artifactId>raf-framework-starter</artifactId>
        <version>${revision}</version>
    </parent>

    <artifactId>raf-framework-kms-starter</artifactId>
    <name>raf-framework-kms-starter</name>
    <description>Multi-cloud KMS integration starter with Jasypt support</description>

    <dependencies>
        <!-- Core -->
        <dependency>
            <groupId>io.github.jerryraf</groupId>
            <artifactId>raf-framework-core</artifactId>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <scope>provided</scope>
        </dependency>

        <!-- Jasypt (required) -->
        <dependency>
            <groupId>com.github.ulisesbocchio</groupId>
            <artifactId>jasypt-spring-boot</artifactId>
        </dependency>

        <!-- spring-retry (required) -->
        <dependency>
            <groupId>org.springframework.retry</groupId>
            <artifactId>spring-retry</artifactId>
        </dependency>

        <!-- Google Cloud KMS (optional) -->
        <dependency>
            <groupId>com.google.cloud</groupId>
            <artifactId>google-cloud-kms</artifactId>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>com.google.auth</groupId>
            <artifactId>google-auth-library-oauth2-http</artifactId>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>com.google.auth</groupId>
            <artifactId>google-auth-library-credentials</artifactId>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>io.grpc</groupId>
            <artifactId>grpc-netty</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- Aliyun KMS (optional) -->
        <dependency>
            <groupId>com.aliyun</groupId>
            <artifactId>aliyun-java-sdk-core</artifactId>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>com.aliyun</groupId>
            <artifactId>aliyun-java-sdk-kms</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- Huawei Cloud KMS (optional) -->
        <dependency>
            <groupId>com.huaweicloud.sdk</groupId>
            <artifactId>huaweicloud-sdk-core</artifactId>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>com.huaweicloud.sdk</groupId>
            <artifactId>huaweicloud-sdk-kms</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- Test -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
```

- [ ] **Step 3: 在 raf-framework-starter/pom.xml 的 `<modules>` 中添加新模块**

在最后一个 `<module>` 之后添加：

```xml
<module>raf-framework-kms-starter</module>
```

- [ ] **Step 4: Commit**

```bash
git add raf-framework-starter/raf-framework-kms-starter/pom.xml raf-framework-starter/pom.xml
git commit -m "feat(kms): scaffold raf-framework-kms-starter module"
```

---

## Task 3: KmsProperties 配置属性类

**Files:**
- Create: `raf-framework-starter/raf-framework-kms-starter/src/main/java/com/raf/framework/kms/properties/KmsProperties.java`
- Create: `raf-framework-starter/raf-framework-kms-starter/src/test/java/com/raf/framework/kms/KmsPropertiesTest.java`

- [ ] **Step 1: 编写测试**

创建文件 `src/test/java/com/raf/framework/kms/KmsPropertiesTest.java`：

```java
package com.raf.framework.kms;

import com.raf.framework.kms.properties.KmsProperties;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class KmsPropertiesTest {

    @Test
    void defaultProviderIsLocal() {
        KmsProperties props = new KmsProperties();
        assertThat(props.getProvider()).isEqualTo("LOCAL");
        assertThat(props.isEnabled()).isFalse();
    }

    @Test
    void keyConfigHoldsAllFields() {
        KmsProperties.KeyConfig kc = new KmsProperties.KeyConfig();
        kc.setKeyId("key-001");
        kc.setCipherText("abc==");
        kc.setPlainText("secret");
        kc.setDescription("test key");
        assertThat(kc.getKeyId()).isEqualTo("key-001");
        assertThat(kc.getCipherText()).isEqualTo("abc==");
        assertThat(kc.getPlainText()).isEqualTo("secret");
    }

    @Test
    void jasyptConfigDefaultKeyAliasIsEmpty() {
        KmsProperties.JasyptConfig jc = new KmsProperties.JasyptConfig();
        assertThat(jc.isEnabled()).isFalse();
        assertThat(jc.getKeyAlias()).isNull();
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

```bash
cd D:/Framework/raf-framework
mvn test -pl raf-framework-starter/raf-framework-kms-starter -Dtest=KmsPropertiesTest -q 2>&1 | tail -5
```

Expected: FAIL（类不存在）

- [ ] **Step 3: 创建 KmsProperties.java**

创建文件 `src/main/java/com/raf/framework/kms/properties/KmsProperties.java`：

```java
package com.raf.framework.kms.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * KMS 配置属性
 * <p>
 * 所有配置不允许有加密数据，避免循环依赖问题。
 *
 * @author Jerry
 * @since 2026-05-06
 */
@Data
@ConfigurationProperties(prefix = "raf.kms")
public class KmsProperties {

    /** 是否启用 KMS，默认关闭 */
    private boolean enabled = false;

    /** Cloud Provider: LOCAL | ALIYUN | HUAWEI | GOOGLE */
    private String provider = "LOCAL";

    /** 正式通道密钥（ALIYUN/HUAWEI/GOOGLE 使用）alias -> KeyConfig */
    private Map<String, KeyConfig> keys = new HashMap<>();

    /** 本地模式密钥（LOCAL 专用，明文存储）alias -> KeyConfig */
    private Map<String, KeyConfig> localKeys = new HashMap<>();

    private AliyunConfig aliyun = new AliyunConfig();
    private HuaweiConfig huawei = new HuaweiConfig();
    private GoogleConfig google = new GoogleConfig();
    private JasyptConfig jasypt = new JasyptConfig();

    @Data
    public static class KeyConfig {
        /** KMS 密钥 ID（KEK ID） */
        private String keyId;
        /** 使用 KMS 加密后的数据密钥密文（Base64） */
        private String cipherText;
        /** LOCAL 模式下直接明文密钥 */
        private String plainText;
        /** 密钥描述（可选） */
        private String description;
    }

    @Data
    public static class AliyunConfig {
        private String regionId;
        private String accessKeyId;
        private String accessKeySecret;
    }

    @Data
    public static class HuaweiConfig {
        private String region;
        private String accessKey;
        private String secretKey;
        private String projectId;
    }

    @Data
    public static class GoogleConfig {
        /** ADC（云内）| JSON（云外，使用服务账号文件） */
        private String authMode = "ADC";
        /** 服务账号 JSON 文件路径（authMode=JSON 时使用） */
        private String serviceAccountJsonPath;
        private ProxyConfig proxy = new ProxyConfig();
    }

    @Data
    public static class ProxyConfig {
        private boolean enabled = false;
        private String host = "127.0.0.1";
        private int port = 10809;
        private String username;
        private String password;
    }

    @Data
    public static class JasyptConfig {
        /** 是否启用 KMS-Jasypt 集成 */
        private boolean enabled = false;
        /** 用哪个 alias 的密钥作为 Jasypt 密码 */
        private String keyAlias;
    }
}
```

- [ ] **Step 4: 运行测试确认通过**

```bash
cd D:/Framework/raf-framework
mvn test -pl raf-framework-starter/raf-framework-kms-starter -Dtest=KmsPropertiesTest -q
```

Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add raf-framework-starter/raf-framework-kms-starter/src/
git commit -m "feat(kms): add KmsProperties configuration class"
```

---

## Task 4: KmsService 接口 + LocalKmsService

**Files:**
- Create: `...kms/provider/KmsService.java`
- Create: `...kms/provider/LocalKmsService.java`
- Create: `src/test/java/com/raf/framework/kms/LocalKmsServiceTest.java`

- [ ] **Step 1: 编写 LocalKmsService 测试**

创建文件 `src/test/java/com/raf/framework/kms/LocalKmsServiceTest.java`：

```java
package com.raf.framework.kms;

import com.raf.framework.kms.properties.KmsProperties;
import com.raf.framework.kms.provider.LocalKmsService;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalKmsServiceTest {

    private final LocalKmsService service = new LocalKmsService();

    @Test
    void resolveKeysReturnsPlainTextByAlias() {
        KmsProperties props = new KmsProperties();
        KmsProperties.KeyConfig kc = new KmsProperties.KeyConfig();
        kc.setPlainText("my-secret-key-32bytes-padding123");
        props.getLocalKeys().put("aes-biz-key", kc);

        Map<String, String> result = service.resolveKeys(props);

        assertThat(result).containsEntry("aes-biz-key", "my-secret-key-32bytes-padding123");
    }

    @Test
    void resolveKeysThrowsWhenLocalKeysEmpty() {
        KmsProperties props = new KmsProperties();
        assertThatThrownBy(() -> service.resolveKeys(props))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("localKeys");
    }

    @Test
    void decryptReturnsInputAsIs() {
        assertThat(service.decrypt("key-id", "cipher")).isEqualTo("cipher");
    }

    @Test
    void encryptReturnsInputAsIs() {
        assertThat(service.encrypt("key-id", "plain")).isEqualTo("plain");
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

```bash
cd D:/Framework/raf-framework
mvn test -pl raf-framework-starter/raf-framework-kms-starter -Dtest=LocalKmsServiceTest -q 2>&1 | tail -5
```

Expected: FAIL（类不存在）

- [ ] **Step 3: 创建 KmsService.java**

创建文件 `src/main/java/com/raf/framework/kms/provider/KmsService.java`：

```java
package com.raf.framework.kms.provider;

import com.raf.framework.kms.properties.KmsProperties;
import java.util.Map;

/**
 * 统一 KMS 接口，支持多云提供商。
 *
 * @author Jerry
 * @since 2026-05-06
 */
public interface KmsService {

    /**
     * 根据配置校验并加载/解密所有密钥。
     *
     * @param properties KMS 配置
     * @return alias -> plainText 映射
     */
    Map<String, String> resolveKeys(KmsProperties properties);

    /**
     * 解密密文。
     *
     * @param keyId      KMS 密钥 ID
     * @param cipherText Base64 编码密文
     * @return 明文
     */
    String decrypt(String keyId, String cipherText);

    /**
     * 加密明文。
     *
     * @param keyId     KMS 密钥 ID
     * @param plainText 明文
     * @return Base64 编码密文
     */
    String encrypt(String keyId, String plainText);
}
```

- [ ] **Step 4: 创建 LocalKmsService.java**

创建文件 `src/main/java/com/raf/framework/kms/provider/LocalKmsService.java`：

```java
package com.raf.framework.kms.provider;

import com.raf.framework.kms.properties.KmsProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 本地 KMS 服务实现。
 * <p>
 * 适用于开发、测试或无法访问云 KMS 的环境。
 * 本地模式使用独立的 localKeys 配置，直接返回明文，不进行实际加解密。
 *
 * @author Jerry
 * @since 2026-05-06
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "raf.kms.provider", havingValue = "LOCAL", matchIfMissing = true)
public class LocalKmsService implements KmsService {

    @Override
    public Map<String, String> resolveKeys(KmsProperties properties) {
        if (properties.getLocalKeys().isEmpty()) {
            throw new IllegalArgumentException("[Local KMS] No localKeys found in configuration.");
        }
        log.info("Using LOCAL KMS Provider.");
        Map<String, String> resolvedKeys = new HashMap<>();
        for (Map.Entry<String, KmsProperties.KeyConfig> entry : properties.getLocalKeys().entrySet()) {
            String alias = entry.getKey();
            KmsProperties.KeyConfig keyConfig = entry.getValue();
            try {
                resolvedKeys.put(alias, keyConfig.getPlainText());
                log.info("Successfully loaded key for alias: {}", alias);
            } catch (Exception e) {
                log.error("Failed to load key for alias: {}", alias, e);
                throw new RuntimeException("Local KMS key loading failed for alias: " + alias, e);
            }
        }
        return resolvedKeys;
    }

    @Override
    public String decrypt(String keyId, String cipherText) {
        return cipherText;
    }

    @Override
    public String encrypt(String keyId, String plainText) {
        return plainText;
    }
}
```

- [ ] **Step 5: 运行测试确认通过**

```bash
cd D:/Framework/raf-framework
mvn test -pl raf-framework-starter/raf-framework-kms-starter -Dtest=LocalKmsServiceTest -q
```

Expected: BUILD SUCCESS

- [ ] **Step 6: Commit**

```bash
git add raf-framework-starter/raf-framework-kms-starter/src/
git commit -m "feat(kms): add KmsService interface and LocalKmsService"
```

---

## Task 5: AliyunKmsService

**Files:**
- Create: `...kms/provider/AliyunKmsService.java`

注意：阿里云 SDK 为 optional 依赖，此 Service 仅在引入 aliyun-java-sdk-kms 时才会被编译使用。

- [ ] **Step 1: 创建 AliyunKmsService.java**

创建文件 `src/main/java/com/raf/framework/kms/provider/AliyunKmsService.java`：

```java
package com.raf.framework.kms.provider;

import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.kms.model.v20160120.DecryptRequest;
import com.aliyuncs.kms.model.v20160120.DecryptResponse;
import com.aliyuncs.kms.model.v20160120.EncryptRequest;
import com.aliyuncs.kms.model.v20160120.EncryptResponse;
import com.aliyuncs.profile.DefaultProfile;
import com.raf.framework.kms.properties.KmsProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 阿里云 KMS 服务实现。
 *
 * @author Jerry
 * @since 2026-05-06
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "raf.kms.provider", havingValue = "ALIYUN")
public class AliyunKmsService implements KmsService {

    private final IAcsClient acsClient;

    public AliyunKmsService(KmsProperties properties) {
        KmsProperties.AliyunConfig config = properties.getAliyun();
        if (config.getRegionId() == null || config.getAccessKeyId() == null || config.getAccessKeySecret() == null) {
            throw new IllegalArgumentException("Aliyun KMS configuration (regionId, accessKeyId, accessKeySecret) is missing");
        }
        log.info("Initializing Aliyun KMS Client for region: {}", config.getRegionId());
        DefaultProfile profile = DefaultProfile.getProfile(
                config.getRegionId(), config.getAccessKeyId(), config.getAccessKeySecret());
        this.acsClient = new DefaultAcsClient(profile);
    }

    @Override
    public Map<String, String> resolveKeys(KmsProperties properties) {
        if (properties.getKeys().isEmpty()) {
            throw new IllegalArgumentException("[Aliyun KMS] keys map cannot be empty");
        }
        log.info("Using Aliyun KMS Provider.");
        Map<String, String> resolvedKeys = new HashMap<>();
        for (Map.Entry<String, KmsProperties.KeyConfig> entry : properties.getKeys().entrySet()) {
            String alias = entry.getKey();
            KmsProperties.KeyConfig keyConfig = entry.getValue();
            try {
                resolvedKeys.put(alias, decrypt(keyConfig.getKeyId(), keyConfig.getCipherText()));
                log.info("Successfully decrypted key for alias: {}", alias);
            } catch (Exception e) {
                log.error("Failed to decrypt key for alias: {}", alias, e);
                throw new RuntimeException("Aliyun KMS decryption failed for alias: " + alias, e);
            }
        }
        return resolvedKeys;
    }

    @Override
    public String decrypt(String keyId, String cipherTextBase64) {
        if (cipherTextBase64 == null) {
            throw new IllegalArgumentException("Cipher text is null");
        }
        DecryptRequest request = new DecryptRequest();
        request.setCiphertextBlob(cipherTextBase64);
        try {
            DecryptResponse response = acsClient.getAcsResponse(request);
            return response.getPlaintext();
        } catch (ClientException e) {
            log.error("Aliyun KMS Decryption failed. Code: {}, Msg: {}", e.getErrCode(), e.getErrMsg());
            throw new RuntimeException("Aliyun KMS Decrypt Error: " + e.getErrMsg(), e);
        }
    }

    @Override
    public String encrypt(String keyId, String plainText) {
        if (keyId == null || plainText == null) {
            throw new IllegalArgumentException("KeyId and PlainText are required");
        }
        EncryptRequest request = new EncryptRequest();
        request.setKeyId(keyId);
        request.setPlaintext(plainText);
        try {
            EncryptResponse response = acsClient.getAcsResponse(request);
            return response.getCiphertextBlob();
        } catch (ClientException e) {
            log.error("Aliyun KMS Encryption failed. Code: {}, Msg: {}", e.getErrCode(), e.getErrMsg());
            throw new RuntimeException("Aliyun KMS Encrypt Error: " + e.getErrMsg(), e);
        }
    }
}
```

- [ ] **Step 2: 编译验证**

```bash
cd D:/Framework/raf-framework
mvn compile -pl raf-framework-starter/raf-framework-kms-starter -q
```

Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add raf-framework-starter/raf-framework-kms-starter/src/main/java/com/raf/framework/kms/provider/AliyunKmsService.java
git commit -m "feat(kms): add AliyunKmsService"
```

---

## Task 6: HuaweiKmsService

**Files:**
- Create: `...kms/provider/HuaweiKmsService.java`

- [ ] **Step 1: 创建 HuaweiKmsService.java**

创建文件 `src/main/java/com/raf/framework/kms/provider/HuaweiKmsService.java`：

```java
package com.raf.framework.kms.provider;

import com.huaweicloud.sdk.core.auth.BasicCredentials;
import com.huaweicloud.sdk.core.auth.ICredential;
import com.huaweicloud.sdk.core.exception.ConnectionException;
import com.huaweicloud.sdk.core.exception.RequestTimeoutException;
import com.huaweicloud.sdk.core.exception.ServiceResponseException;
import com.huaweicloud.sdk.kms.v2.KmsClient;
import com.huaweicloud.sdk.kms.v2.model.*;
import com.huaweicloud.sdk.kms.v2.region.KmsRegion;
import com.raf.framework.kms.properties.KmsProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 华为云 KMS 服务实现。
 *
 * @author Jerry
 * @since 2026-05-06
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "raf.kms.provider", havingValue = "HUAWEI")
public class HuaweiKmsService implements KmsService {

    private final KmsClient kmsClient;

    public HuaweiKmsService(KmsProperties properties) {
        KmsProperties.HuaweiConfig config = properties.getHuawei();
        if (config.getRegion() == null || config.getAccessKey() == null || config.getSecretKey() == null) {
            throw new IllegalArgumentException("Huawei KMS configuration (region, accessKey, secretKey) is missing");
        }
        log.info("Initializing Huawei KMS Client for region: {}", config.getRegion());
        ICredential auth = new BasicCredentials()
                .withAk(config.getAccessKey())
                .withSk(config.getSecretKey())
                .withProjectId(config.getProjectId());
        this.kmsClient = KmsClient.newBuilder()
                .withCredential(auth)
                .withRegion(KmsRegion.valueOf(config.getRegion()))
                .build();
    }

    @Override
    public Map<String, String> resolveKeys(KmsProperties properties) {
        if (properties.getKeys().isEmpty()) {
            throw new IllegalArgumentException("[Huawei KMS] keys map cannot be empty");
        }
        log.info("Using Huawei KMS Provider.");
        Map<String, String> resolvedKeys = new HashMap<>();
        for (Map.Entry<String, KmsProperties.KeyConfig> entry : properties.getKeys().entrySet()) {
            String alias = entry.getKey();
            KmsProperties.KeyConfig keyConfig = entry.getValue();
            try {
                resolvedKeys.put(alias, decrypt(keyConfig.getKeyId(), keyConfig.getCipherText()));
                log.info("Successfully decrypted key for alias: {}", alias);
            } catch (Exception e) {
                log.error("Failed to decrypt key for alias: {}", alias, e);
                throw new RuntimeException("Huawei KMS decryption failed for alias: " + alias, e);
            }
        }
        return resolvedKeys;
    }

    @Override
    public String decrypt(String keyId, String cipherTextBase64) {
        if (cipherTextBase64 == null || keyId == null) {
            throw new IllegalArgumentException("KeyId and CipherText are required");
        }
        try {
            DecryptDataRequestBody body = new DecryptDataRequestBody();
            body.setKeyId(keyId);
            body.setCipherText(cipherTextBase64);
            DecryptDataRequest request = new DecryptDataRequest();
            request.setBody(body);
            DecryptDataResponse response = kmsClient.decryptData(request);
            if (response == null) {
                throw new RuntimeException("Huawei KMS returned null response");
            }
            return response.getPlainText();
        } catch (ConnectionException e) {
            log.error("Huawei KMS Connection error: {}", e.getMessage());
            throw new RuntimeException("Huawei KMS Connection Error: " + e.getMessage(), e);
        } catch (RequestTimeoutException e) {
            log.error("Huawei KMS Request timeout: {}", e.getMessage());
            throw new RuntimeException("Huawei KMS Request Timeout: " + e.getMessage(), e);
        } catch (ServiceResponseException e) {
            log.error("Huawei KMS Service error. HttpStatusCode: {}, ErrorCode: {}, ErrorMsg: {}",
                    e.getHttpStatusCode(), e.getErrorCode(), e.getErrorMsg());
            throw new RuntimeException("Huawei KMS Service Error: " + e.getErrorMsg(), e);
        }
    }

    @Override
    public String encrypt(String keyId, String plainText) {
        if (keyId == null || plainText == null) {
            throw new IllegalArgumentException("KeyId and PlainText are required");
        }
        try {
            EncryptDataRequestBody body = new EncryptDataRequestBody();
            body.setKeyId(keyId);
            body.setPlainText(plainText);
            EncryptDataRequest request = new EncryptDataRequest();
            request.setBody(body);
            EncryptDataResponse response = kmsClient.encryptData(request);
            if (response == null) {
                throw new RuntimeException("Huawei KMS returned null response");
            }
            return response.getCipherText();
        } catch (ConnectionException e) {
            log.error("Huawei KMS Connection error: {}", e.getMessage());
            throw new RuntimeException("Huawei KMS Connection Error: " + e.getMessage(), e);
        } catch (RequestTimeoutException e) {
            log.error("Huawei KMS Request timeout: {}", e.getMessage());
            throw new RuntimeException("Huawei KMS Request Timeout: " + e.getMessage(), e);
        } catch (ServiceResponseException e) {
            log.error("Huawei KMS Service error. HttpStatusCode: {}, ErrorCode: {}, ErrorMsg: {}",
                    e.getHttpStatusCode(), e.getErrorCode(), e.getErrorMsg());
            throw new RuntimeException("Huawei KMS Service Error: " + e.getErrorMsg(), e);
        }
    }
}
```

- [ ] **Step 2: 编译验证**

```bash
cd D:/Framework/raf-framework
mvn compile -pl raf-framework-starter/raf-framework-kms-starter -q
```

Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add raf-framework-starter/raf-framework-kms-starter/src/main/java/com/raf/framework/kms/provider/HuaweiKmsService.java
git commit -m "feat(kms): add HuaweiKmsService"
```

---

## Task 7: GoogleKmsService

**Files:**
- Create: `...kms/provider/GoogleKmsService.java`

- [ ] **Step 1: 创建 GoogleKmsService.java**

创建文件 `src/main/java/com/raf/framework/kms/provider/GoogleKmsService.java`，内容与 co-common-kms 中的 GoogleKmsService 完全一致，但做以下修改：
1. 包名改为 `com.raf.framework.kms.provider`
2. 所有 `SecretProperties` 引用改为 `KmsProperties`（包括 import 和方法参数）
3. 删除对 `CryptoManager.getEnvironmentVariable` 的引用（紧急模式检测移至 CryptoManager）
4. `@ConditionalOnProperty` 改为 `name = "raf.kms.provider", havingValue = "GOOGLE"`
5. `@Service` 改为 `@Component`

完整代码结构（关键变更部分）：

```java
package com.raf.framework.kms.provider;

import com.raf.framework.kms.properties.KmsProperties;
// ... 其他 Google SDK imports 保持不变 ...

@Slf4j
@Component
@ConditionalOnProperty(name = "raf.kms.provider", havingValue = "GOOGLE")
public class GoogleKmsService implements KmsService {

    private volatile KeyManagementServiceClient client;
    private final KmsProperties properties;

    public GoogleKmsService(KmsProperties properties) {
        this.properties = properties;
    }

    @Override
    public Map<String, String> resolveKeys(KmsProperties properties) {
        // 与原实现相同，但参数类型为 KmsProperties
        // properties.getKeys() 遍历逻辑不变
    }

    @Override
    public String decrypt(String keyId, String cipherText) {
        // 与原实现完全相同
    }

    @Override
    public String encrypt(String keyId, String plainText) {
        // 与原实现完全相同
    }

    private void ensureInitialized() {
        // 双重检查锁定，与原实现相同
        // 删除 FORCE_EMERGENCY_MODE 检查（由 CryptoManager 统一处理）
    }

    private KeyManagementServiceClient createClientWithJsonFile(String jsonFilePath) throws Exception {
        // 与原实现完全相同，包括代理配置逻辑
        // ProxyConfig 类型改为 KmsProperties.ProxyConfig
    }
}
```

- [ ] **Step 2: 编译验证**

```bash
cd D:/Framework/raf-framework
mvn compile -pl raf-framework-starter/raf-framework-kms-starter -q
```

Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add raf-framework-starter/raf-framework-kms-starter/src/main/java/com/raf/framework/kms/provider/GoogleKmsService.java
git commit -m "feat(kms): add GoogleKmsService"
```

---

## Task 8: CryptoManager 核心管理器

**Files:**
- Create: `...kms/core/CryptoManager.java`
- Create: `src/test/java/com/raf/framework/kms/CryptoManagerTest.java`

- [ ] **Step 1: 编写 CryptoManager 测试**

创建文件 `src/test/java/com/raf/framework/kms/CryptoManagerTest.java`：

```java
package com.raf.framework.kms;

import com.raf.framework.kms.core.CryptoManager;
import com.raf.framework.kms.properties.KmsProperties;
import com.raf.framework.kms.provider.LocalKmsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CryptoManagerTest {

    private CryptoManager cryptoManager;
    private static final String TEST_ALIAS = "aes-biz-key";
    // AES-256 需要 32 字节密钥，Base64 编码
    private static final String TEST_KEY_BASE64 = "dGVzdC1rZXktMzItYnl0ZXMtcGFkZGluZzEyMzQ1Njc=";

    @BeforeEach
    void setUp() {
        KmsProperties props = new KmsProperties();
        props.setEnabled(true);
        KmsProperties.KeyConfig kc = new KmsProperties.KeyConfig();
        kc.setPlainText(TEST_KEY_BASE64);
        props.getLocalKeys().put(TEST_ALIAS, kc);
        LocalKmsService localKms = new LocalKmsService();
        cryptoManager = new CryptoManager(localKms, props);
        cryptoManager.init();
    }

    @Test
    void encryptAndDecryptRoundTrip() {
        String plainText = "hello-world";
        String cipher = cryptoManager.encrypt(plainText, TEST_ALIAS);
        assertThat(cipher).isNotEqualTo(plainText);
        String decrypted = cryptoManager.decrypt(cipher, TEST_ALIAS);
        assertThat(decrypted).isEqualTo(plainText);
    }

    @Test
    void generateIndexIsDeterministic() {
        String index1 = cryptoManager.generateIndex("13800138000", TEST_ALIAS);
        String index2 = cryptoManager.generateIndex("13800138000", TEST_ALIAS);
        assertThat(index1).isEqualTo(index2);
    }

    @Test
    void encryptNullOrEmptyReturnsInput() {
        assertThat(cryptoManager.encrypt(null, TEST_ALIAS)).isNull();
        assertThat(cryptoManager.encrypt("", TEST_ALIAS)).isEmpty();
    }

    @Test
    void decryptNullOrEmptyReturnsInput() {
        assertThat(cryptoManager.decrypt(null, TEST_ALIAS)).isNull();
        assertThat(cryptoManager.decrypt("", TEST_ALIAS)).isEmpty();
    }

    @Test
    void getKeyThrowsForUnknownAlias() {
        assertThatThrownBy(() -> cryptoManager.encrypt("text", "unknown-alias"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("unknown-alias");
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

```bash
cd D:/Framework/raf-framework
mvn test -pl raf-framework-starter/raf-framework-kms-starter -Dtest=CryptoManagerTest -q 2>&1 | tail -5
```

Expected: FAIL（类不存在）

- [ ] **Step 3: 创建 CryptoManager.java**

创建文件 `src/main/java/com/raf/framework/kms/core/CryptoManager.java`：

```java
package com.raf.framework.kms.core;

import com.raf.framework.core.util.CryptoUtil;
import com.raf.framework.kms.properties.KmsProperties;
import com.raf.framework.kms.provider.KmsService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 核心加密管理器。
 * 密钥缓存：alias -> plainText，通过 String alias 操作，无业务枚举耦合。
 *
 * @author Jerry
 * @since 2026-05-06
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CryptoManager {

    private final KmsService kmsService;
    private final KmsProperties properties;

    /** 密钥缓存：alias -> plainText */
    private final Map<String, String> keyCache = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        log.info("Starting initialization of security keys...");
        String forceEmergency = getEnvironmentVariable("FORCE_EMERGENCY_MODE");
        if ("true".equalsIgnoreCase(forceEmergency)) {
            log.warn("!!! FORCE_EMERGENCY_MODE is enabled, loading keys from local-keys config !!!");
            loadFromLocalKeys();
            return;
        }
        loadFromKms();
    }

    private void loadFromLocalKeys() {
        if (properties.getLocalKeys().isEmpty()) {
            throw new RuntimeException("FORCE_EMERGENCY_MODE is enabled but local-keys is empty");
        }
        for (Map.Entry<String, KmsProperties.KeyConfig> entry : properties.getLocalKeys().entrySet()) {
            String alias = entry.getKey();
            String plainText = entry.getValue().getPlainText();
            if (StringUtils.isEmpty(plainText)) {
                throw new RuntimeException("Emergency mode: plainText is empty for alias: " + alias);
            }
            keyCache.put(alias, plainText);
        }
        log.info("Emergency mode: loaded {} keys from local-keys config", keyCache.size());
    }

    private void loadFromKms() {
        RetryTemplate retryTemplate = RetryTemplate.builder()
                .maxAttempts(2)
                .fixedBackoff(500)
                .retryOn(RuntimeException.class)
                .build();
        try {
            retryTemplate.execute(context -> {
                log.info("Attempting to resolve keys using provider: {} (Attempt: {})",
                        kmsService.getClass().getSimpleName(), context.getRetryCount() + 1);
                Map<String, String> resolvedKeys = kmsService.resolveKeys(properties);
                resolvedKeys.forEach((alias, plainText) -> {
                    if (StringUtils.isEmpty(plainText)) {
                        throw new IllegalStateException("Missing plainText for alias: " + alias);
                    }
                    keyCache.put(alias, plainText);
                });
                return null;
            });
            log.info("Successfully loaded and cached {} security keys.", keyCache.size());
        } catch (Exception e) {
            log.error("Critical failure: Unable to retrieve keys after retries.", e);
            throw new RuntimeException("KMS Unavailable", e);
        }
    }

    // ===== Business API =====

    public String encrypt(String plainText, String keyAlias) {
        if (StringUtils.isEmpty(plainText)) {
            return plainText;
        }
        return CryptoUtil.aesEncrypt(plainText, getKey(keyAlias));
    }

    public String decrypt(String cipherText, String keyAlias) {
        if (StringUtils.isEmpty(cipherText)) {
            return cipherText;
        }
        return CryptoUtil.aesDecrypt(cipherText, getKey(keyAlias));
    }

    public String generateIndex(String plainText, String keyAlias) {
        if (StringUtils.isEmpty(plainText)) {
            return null;
        }
        return CryptoUtil.hmacSha256(plainText, getKey(keyAlias));
    }

    public String getRawKey(String keyAlias) {
        return getKey(keyAlias);
    }

    private String getKey(String keyAlias) {
        String key = keyCache.get(keyAlias);
        if (key == null) {
            throw new IllegalStateException("Security Error: Key not initialized for alias: " + keyAlias);
        }
        return key;
    }

    public static String getEnvironmentVariable(String key) {
        String value = System.getProperty(key);
        if (StringUtils.isEmpty(value)) {
            value = System.getenv(key);
        }
        return value;
    }
}
```

- [ ] **Step 4: 运行测试确认通过**

```bash
cd D:/Framework/raf-framework
mvn test -pl raf-framework-starter/raf-framework-kms-starter -Dtest=CryptoManagerTest -q
```

Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add raf-framework-starter/raf-framework-kms-starter/src/
git commit -m "feat(kms): add CryptoManager with alias-based key cache"
```

---

## Task 9: KmsJasyptProperties + KmsJasyptConfig

**Files:**
- Create: `...kms/jasypt/KmsJasyptProperties.java`
- Create: `...kms/jasypt/KmsJasyptConfig.java`

- [ ] **Step 1: 创建 KmsJasyptProperties.java**

创建文件 `src/main/java/com/raf/framework/kms/jasypt/KmsJasyptProperties.java`：

```java
package com.raf.framework.kms.jasypt;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Jasypt 加密器配置属性。
 * 从配置中心读取，支持动态刷新。
 *
 * @author Jerry
 * @since 2026-05-06
 */
@Data
@ConfigurationProperties(prefix = "jasypt.encryptor")
public class KmsJasyptProperties {
    private String algorithm = "PBEWithHMACSHA512AndAES_256";
    private String ivGeneratorClassName = "org.jasypt.iv.RandomIvGenerator";
    private Integer keyObtentionIterations = 1000;
    private Integer poolSize = 1;
    private String providerName = "SunJCE";
    private String saltGeneratorClassName = "org.jasypt.salt.RandomSaltGenerator";
    private String stringOutputType = "base64";
}
```

- [ ] **Step 2: 创建 KmsJasyptConfig.java**

创建文件 `src/main/java/com/raf/framework/kms/jasypt/KmsJasyptConfig.java`：

```java
package com.raf.framework.kms.jasypt;

import com.raf.framework.kms.core.CryptoManager;
import com.raf.framework.kms.properties.KmsProperties;
import com.ulisesbocchio.jasyptspringboot.annotation.EnableEncryptableProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jasypt.encryption.StringEncryptor;
import org.jasypt.encryption.pbe.PooledPBEStringEncryptor;
import org.jasypt.encryption.pbe.config.SimpleStringPBEConfig;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * KMS 与 Jasypt 集成配置。
 * 将 KMS 解密后的指定 alias 密钥作为 Jasypt 的加密密码。
 * 通过 raf.kms.jasypt.enabled=true 显式启用。
 *
 * @author Jerry
 * @since 2026-05-06
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "raf.kms.jasypt.enabled", havingValue = "true")
@EnableEncryptableProperties
@EnableConfigurationProperties(KmsJasyptProperties.class)
@RequiredArgsConstructor
public class KmsJasyptConfig {

    private final CryptoManager cryptoManager;
    private final KmsProperties kmsProperties;
    private final KmsJasyptProperties jasyptProperties;

    @Bean("jasyptStringEncryptor")
    public StringEncryptor kmsStringEncryptor() {
        String keyAlias = kmsProperties.getJasypt().getKeyAlias();
        if (keyAlias == null || keyAlias.isBlank()) {
            throw new IllegalStateException("raf.kms.jasypt.key-alias must be configured when jasypt is enabled");
        }
        log.info("Configuring Jasypt encryptor using KMS key alias: {}", keyAlias);

        PooledPBEStringEncryptor encryptor = new PooledPBEStringEncryptor();
        SimpleStringPBEConfig config = new SimpleStringPBEConfig();
        config.setPassword(cryptoManager.getRawKey(keyAlias));
        config.setAlgorithm(jasyptProperties.getAlgorithm());
        config.setIvGeneratorClassName(jasyptProperties.getIvGeneratorClassName());
        config.setKeyObtentionIterations(String.valueOf(jasyptProperties.getKeyObtentionIterations()));
        config.setPoolSize(String.valueOf(jasyptProperties.getPoolSize()));
        config.setProviderName(jasyptProperties.getProviderName());
        config.setSaltGeneratorClassName(jasyptProperties.getSaltGeneratorClassName());
        config.setStringOutputType(jasyptProperties.getStringOutputType());
        encryptor.setConfig(config);
        return encryptor;
    }
}
```

- [ ] **Step 3: 编译验证**

```bash
cd D:/Framework/raf-framework
mvn compile -pl raf-framework-starter/raf-framework-kms-starter -q
```

Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add raf-framework-starter/raf-framework-kms-starter/src/main/java/com/raf/framework/kms/jasypt/
git commit -m "feat(kms): add KmsJasyptConfig for Jasypt integration"
```

---

## Task 10: KmsAutoConfiguration + AutoConfiguration.imports

**Files:**
- Create: `...kms/KmsAutoConfiguration.java`
- Create: `src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`

- [ ] **Step 1: 创建 KmsAutoConfiguration.java**

创建文件 `src/main/java/com/raf/framework/kms/KmsAutoConfiguration.java`：

```java
package com.raf.framework.kms;

import com.raf.framework.kms.core.CryptoManager;
import com.raf.framework.kms.properties.KmsProperties;
import com.raf.framework.kms.provider.AliyunKmsService;
import com.raf.framework.kms.provider.GoogleKmsService;
import com.raf.framework.kms.provider.HuaweiKmsService;
import com.raf.framework.kms.provider.LocalKmsService;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;

/**
 * KMS Starter 主自动配置入口。
 * 通过 raf.kms.enabled=true 启用，默认关闭。
 * Provider 由各自的 @ConditionalOnProperty 条件装配。
 *
 * @author Jerry
 * @since 2026-05-06
 */
@AutoConfiguration
@ConditionalOnProperty(name = "raf.kms.enabled", havingValue = "true")
@EnableConfigurationProperties(KmsProperties.class)
@Import({
        LocalKmsService.class,
        AliyunKmsService.class,
        HuaweiKmsService.class,
        GoogleKmsService.class,
        CryptoManager.class
})
public class KmsAutoConfiguration {
}
```

- [ ] **Step 2: 创建 AutoConfiguration.imports**

创建文件 `src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`：

```
com.raf.framework.kms.KmsAutoConfiguration
com.raf.framework.kms.jasypt.KmsJasyptConfig
```

- [ ] **Step 3: 编译验证**

```bash
cd D:/Framework/raf-framework
mvn compile -pl raf-framework-starter/raf-framework-kms-starter -q
```

Expected: BUILD SUCCESS

- [ ] **Step 4: 运行所有测试**

```bash
cd D:/Framework/raf-framework
mvn test -pl raf-framework-starter/raf-framework-kms-starter -q
```

Expected: BUILD SUCCESS，所有测试通过

- [ ] **Step 5: Commit**

```bash
git add raf-framework-starter/raf-framework-kms-starter/src/
git commit -m "feat(kms): add KmsAutoConfiguration and AutoConfiguration.imports"
```

---

## Task 11: 示例项目 raf-example-kms-starter

**Files:**
- Create: `examples/raf-example-kms-starter/pom.xml`
- Create: `examples/raf-example-kms-starter/src/main/java/io/github/jerryraf/examples/kms/KmsExampleApplication.java`
- Create: `examples/raf-example-kms-starter/src/main/java/io/github/jerryraf/examples/kms/controller/CryptoController.java`
- Create: `examples/raf-example-kms-starter/src/main/resources/application.yml`
- Modify: `examples/pom.xml`

- [ ] **Step 1: 创建目录结构**

```bash
mkdir -p "D:/Framework/raf-framework/examples/raf-example-kms-starter/src/main/java/io/github/jerryraf/examples/kms/controller"
mkdir -p "D:/Framework/raf-framework/examples/raf-example-kms-starter/src/main/resources"
```

- [ ] **Step 2: 创建 pom.xml**

创建文件 `examples/raf-example-kms-starter/pom.xml`，内容参考 raf-example-redis-starter/pom.xml，关键差异：
- artifactId: raf-example-kms-starter
- description: Demonstrates multi-cloud KMS integration with LOCAL mode and Jasypt config encryption
- 依赖：raf-framework-web-starter + raf-framework-kms-starter

- [ ] **Step 3: 创建 KmsExampleApplication.java**

创建文件 `src/main/java/io/github/jerryraf/examples/kms/KmsExampleApplication.java`：

```java
package io.github.jerryraf.examples.kms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class KmsExampleApplication {
    public static void main(String[] args) {
        SpringApplication.run(KmsExampleApplication.class, args);
    }
}
```

- [ ] **Step 4: 创建 CryptoController.java**

创建文件 `src/main/java/io/github/jerryraf/examples/kms/controller/CryptoController.java`：

```java
package io.github.jerryraf.examples.kms.controller;

import com.raf.framework.core.common.result.RafResult;
import com.raf.framework.kms.core.CryptoManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/crypto")
@RequiredArgsConstructor
public class CryptoController {

    private final CryptoManager cryptoManager;

    @GetMapping("/encrypt")
    public RafResult<String> encrypt(@RequestParam String text,
                                     @RequestParam(defaultValue = "aes-biz-key") String alias) {
        return RafResult.success(cryptoManager.encrypt(text, alias));
    }

    @GetMapping("/decrypt")
    public RafResult<String> decrypt(@RequestParam String cipher,
                                     @RequestParam(defaultValue = "aes-biz-key") String alias) {
        return RafResult.success(cryptoManager.decrypt(cipher, alias));
    }

    @GetMapping("/index")
    public RafResult<String> index(@RequestParam String text,
                                   @RequestParam(defaultValue = "aes-biz-key") String alias) {
        return RafResult.success(cryptoManager.generateIndex(text, alias));
    }
}
```

- [ ] **Step 5: 创建 application.yml**

创建文件 `src/main/resources/application.yml`：

```yaml
spring:
  application:
    name: raf-example-kms-starter

server:
  port: 8080

raf:
  kms:
    enabled: true
    provider: LOCAL
    local-keys:
      aes-biz-key:
        plain-text: ${KMS_BIZ_KEY:dGVzdC1rZXktMzItYnl0ZXMtcGFkZGluZzEyMzQ1Njc=}
        description: "业务数据加密密钥（示例，生产请替换）"
      aes-config-key:
        plain-text: ${KMS_CONFIG_KEY:Y29uZmlnLWtleS0zMi1ieXRlcy1wYWRkaW5nMTIzNDU=}
        description: "配置数据加密密钥（示例，生产请替换）"
  log:
    enabled: true
    level: REQ_BODY
```

- [ ] **Step 6: 在 examples/pom.xml 的 modules 中添加**



- [ ] **Step 7: 编译验证**



Expected: BUILD SUCCESS

- [ ] **Step 8: Commit**



---


## Task 11: 示例项目 raf-example-kms-starter

**Files:**
- Create: `examples/raf-example-kms-starter/pom.xml`
- Create: `examples/raf-example-kms-starter/src/main/java/io/github/jerryraf/examples/kms/KmsExampleApplication.java`
- Create: `examples/raf-example-kms-starter/src/main/java/io/github/jerryraf/examples/kms/controller/CryptoController.java`
- Create: `examples/raf-example-kms-starter/src/main/resources/application.yml`
- Modify: `examples/pom.xml`

- [ ] **Step 1: 创建目录结构**

```bash
mkdir -p "D:/Framework/raf-framework/examples/raf-example-kms-starter/src/main/java/io/github/jerryraf/examples/kms/controller"
mkdir -p "D:/Framework/raf-framework/examples/raf-example-kms-starter/src/main/resources"
```

- [ ] **Step 2: 创建 pom.xml**

参考 `examples/raf-example-redis-starter/pom.xml`，关键差异：
- artifactId: `raf-example-kms-starter`
- description: Demonstrates multi-cloud KMS integration with LOCAL mode and Jasypt config encryption
- 依赖：`raf-framework-web-starter` + `raf-framework-kms-starter`

- [ ] **Step 3: 创建 KmsExampleApplication.java**

```java
package io.github.jerryraf.examples.kms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class KmsExampleApplication {
    public static void main(String[] args) {
        SpringApplication.run(KmsExampleApplication.class, args);
    }
}
```

- [ ] **Step 4: 创建 CryptoController.java**

```java
package io.github.jerryraf.examples.kms.controller;

import com.raf.framework.core.common.result.RafResult;
import com.raf.framework.kms.core.CryptoManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/crypto")
@RequiredArgsConstructor
public class CryptoController {

    private final CryptoManager cryptoManager;

    @GetMapping("/encrypt")
    public RafResult<String> encrypt(@RequestParam String text,
                                     @RequestParam(defaultValue = "aes-biz-key") String alias) {
        return RafResult.success(cryptoManager.encrypt(text, alias));
    }

    @GetMapping("/decrypt")
    public RafResult<String> decrypt(@RequestParam String cipher,
                                     @RequestParam(defaultValue = "aes-biz-key") String alias) {
        return RafResult.success(cryptoManager.decrypt(cipher, alias));
    }

    @GetMapping("/index")
    public RafResult<String> index(@RequestParam String text,
                                   @RequestParam(defaultValue = "aes-biz-key") String alias) {
        return RafResult.success(cryptoManager.generateIndex(text, alias));
    }
}
```

- [ ] **Step 5: 创建 application.yml**

```yaml
spring:
  application:
    name: raf-example-kms-starter

server:
  port: 8080

raf:
  kms:
    enabled: true
    provider: LOCAL
    local-keys:
      aes-biz-key:
        plain-text: ${KMS_BIZ_KEY:dGVzdC1rZXktMzItYnl0ZXMtcGFkZGluZzEyMzQ1Njc=}
        description: "业务数据加密密钥（示例，生产请替换）"
      aes-config-key:
        plain-text: ${KMS_CONFIG_KEY:Y29uZmlnLWtleS0zMi1ieXRlcy1wYWRkaW5nMTIzNDU=}
        description: "配置数据加密密钥（示例，生产请替换）"
  log:
    enabled: true
    level: REQ_BODY
```

- [ ] **Step 6: 在 examples/pom.xml 的 modules 中添加 raf-example-kms-starter**

- [ ] **Step 7: 编译验证**

```bash
cd D:/Framework/raf-framework
mvn compile -pl examples/raf-example-kms-starter -am -q
```

Expected: BUILD SUCCESS

- [ ] **Step 8: Commit**

```bash
git add examples/raf-example-kms-starter/ examples/pom.xml
git commit -m "feat(examples): add raf-example-kms-starter"
```

---


## Task 12: 文档 docs-site/examples/raf-example-kms-starter.md

**Files:**
- Create: `docs-site/examples/raf-example-kms-starter.md`

- [ ] **Step 1: 创建文档**

创建文件 `docs-site/examples/raf-example-kms-starter.md`，内容包含：
1. 概述（支持的 Provider、核心功能）
2. 快速开始（Maven 依赖、LOCAL 模式配置）
3. CryptoManager API 使用示例
4. 各云 Provider 配置示例（ALIYUN/HUAWEI/GOOGLE）
5. Jasypt 集成说明
6. 紧急模式（FORCE_EMERGENCY_MODE）说明

参考 `docs-site/examples/raf-example-redis-starter.md` 的文档风格和结构。

- [ ] **Step 2: Commit**

```bash
git add docs-site/examples/raf-example-kms-starter.md
git commit -m "docs(examples): add raf-example-kms-starter documentation"
```

---

## Task 13: co-demo 迁移 - 替换 co-common-kms

**Files:**
- Modify: `D:/Framework/co-demo/co-common/co-common-kms/pom.xml`
- Delete: 9 个 Java 文件（provider/*, core/CryptoManager, config/SecretProperties, jasypt/*）
- Keep: `config/CryptoKey.java`

- [ ] **Step 1: 修改 co-common-kms/pom.xml**

将 pom.xml 中所有 KMS SDK 依赖替换为 raf-framework-kms-starter：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <artifactId>co-common-kms</artifactId>
    <packaging>jar</packaging>

    <parent>
        <groupId>com.company</groupId>
        <artifactId>co-common</artifactId>
        <version>${revision}</version>
    </parent>

    <dependencies>
        <!-- KMS Starter -->
        <dependency>
            <groupId>io.github.jerryraf</groupId>
            <artifactId>raf-framework-kms-starter</artifactId>
        </dependency>
    </dependencies>
</project>
```

注意：如果 co-demo 使用 GOOGLE provider，还需要额外引入 google-cloud-kms 等 optional 依赖。

- [ ] **Step 2: 删除由 starter 提供的文件**

删除以下文件（这些类已由 raf-framework-kms-starter 提供）：

```bash
rm "D:/Framework/co-demo/co-common/co-common-kms/src/main/java/com/company/common/kms/provider/KmsService.java"
rm "D:/Framework/co-demo/co-common/co-common-kms/src/main/java/com/company/common/kms/provider/LocalKmsService.java"
rm "D:/Framework/co-demo/co-common/co-common-kms/src/main/java/com/company/common/kms/provider/AliyunKmsService.java"
rm "D:/Framework/co-demo/co-common/co-common-kms/src/main/java/com/company/common/kms/provider/HuaweiKmsService.java"
rm "D:/Framework/co-demo/co-common/co-common-kms/src/main/java/com/company/common/kms/provider/GoogleKmsService.java"
rm "D:/Framework/co-demo/co-common/co-common-kms/src/main/java/com/company/common/kms/core/CryptoManager.java"
rm "D:/Framework/co-demo/co-common/co-common-kms/src/main/java/com/company/common/kms/config/SecretProperties.java"
rm "D:/Framework/co-demo/co-common/co-common-kms/src/main/java/com/company/common/kms/jasypt/JasyptProperties.java"
rm "D:/Framework/co-demo/co-common/co-common-kms/src/main/java/com/company/common/kms/jasypt/KmsJasyptConfig.java"
```

保留 `config/CryptoKey.java`（业务枚举，不动）。

- [ ] **Step 3: 更新 co-demo 中的配置文件**

将所有使用 `app.security` 前缀的配置改为 `raf.kms`。

在 Nacos 或本地配置文件中，将：
```yaml
app:
  security:
    provider: LOCAL
    local-keys:
      aes-biz-key:
        plain-text: xxx
```
改为：
```yaml
raf:
  kms:
    enabled: true
    provider: LOCAL
    local-keys:
      aes-biz-key:
        plain-text: xxx
```

- [ ] **Step 4: 检查调用方代码**

检查 `co-cms-service/src/main/java/com/company/cms/controller/CryptoController.java`：

当前代码：
```java
cryptoManager.encrypt(plaintext)   // 使用默认 BIZ 密钥（原来的重载方法）
cryptoManager.decrypt(ciphertext)  // 使用默认 BIZ 密钥
```

新 CryptoManager 没有无参重载，需要改为：
```java
cryptoManager.encrypt(plaintext, "aes-biz-key")
cryptoManager.decrypt(ciphertext, "aes-biz-key")
```

或者在 co-common-kms 中保留一个薄包装类（推荐，避免改动业务代码）：

在 `co-common-kms` 中创建 `CryptoFacade.java`：
```java
package com.company.common.kms;

import com.company.common.kms.config.CryptoKey;
import com.raf.framework.kms.core.CryptoManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 业务层加密门面，封装 CryptoKey 枚举与 CryptoManager 的适配。
 */
@Component
@RequiredArgsConstructor
public class CryptoFacade {

    private final CryptoManager cryptoManager;

    public String encrypt(String plainText) {
        return cryptoManager.encrypt(plainText, CryptoKey.BIZ.getAlias());
    }

    public String decrypt(String cipherText) {
        return cryptoManager.decrypt(cipherText, CryptoKey.BIZ.getAlias());
    }

    public String encrypt(String plainText, CryptoKey keyType) {
        return cryptoManager.encrypt(plainText, keyType.getAlias());
    }

    public String decrypt(String cipherText, CryptoKey keyType) {
        return cryptoManager.decrypt(cipherText, keyType.getAlias());
    }

    public String generateIndex(String plainText, CryptoKey keyType) {
        return cryptoManager.generateIndex(plainText, keyType.getAlias());
    }
}
```

这样 `CryptoController` 改为注入 `CryptoFacade` 即可，其他业务代码零改动。

- [ ] **Step 5: 编译验证 co-demo**

```bash
cd D:/Framework/co-demo
mvn compile -pl co-common/co-common-kms -am -q
```

Expected: BUILD SUCCESS

- [ ] **Step 6: Commit co-demo 变更**

```bash
cd D:/Framework/co-demo
git add co-common/co-common-kms/
git commit -m "refactor(kms): migrate co-common-kms to raf-framework-kms-starter"
```

---

## Task 14: 全量构建验证

- [ ] **Step 1: 构建 raf-framework**

```bash
cd D:/Framework/raf-framework
mvn clean install -q
```

Expected: BUILD SUCCESS

- [ ] **Step 2: 运行 raf-framework 所有测试**

```bash
cd D:/Framework/raf-framework
mvn test -pl raf-framework-starter/raf-framework-kms-starter -q
```

Expected: BUILD SUCCESS，KmsPropertiesTest、LocalKmsServiceTest、CryptoManagerTest 全部通过

- [ ] **Step 3: 构建 co-demo**

```bash
cd D:/Framework/co-demo
mvn clean compile -q
```

Expected: BUILD SUCCESS

- [ ] **Step 4: 最终 Commit**

```bash
cd D:/Framework/raf-framework
git add -A
git commit -m "feat(kms): complete raf-framework-kms-starter implementation"
```

---

## 自检结果

**Spec 覆盖检查：**
- [x] Task 1: raf-framework-dependencies 版本管理
- [x] Task 2: starter 模块骨架
- [x] Task 3: KmsProperties（前缀 raf.kms）
- [x] Task 4: KmsService 接口 + LocalKmsService
- [x] Task 5: AliyunKmsService
- [x] Task 6: HuaweiKmsService
- [x] Task 7: GoogleKmsService
- [x] Task 8: CryptoManager（alias-based，无枚举耦合）
- [x] Task 9: KmsJasyptConfig（条件启用）
- [x] Task 10: KmsAutoConfiguration + AutoConfiguration.imports
- [x] Task 11: 示例项目
- [x] Task 12: 文档
- [x] Task 13: co-demo 迁移（含 CryptoFacade 适配层）
- [x] Task 14: 全量构建验证

**类型一致性：**
- KmsService.resolveKeys 参数：KmsProperties（Task 4 定义，Task 5/6/7 使用）✓
- CryptoManager 构造参数：KmsService + KmsProperties（Task 8 定义，Task 10 Import 装配）✓
- KmsJasyptConfig 依赖：CryptoManager + KmsProperties + KmsJasyptProperties（Task 9 定义）✓
