# 依赖版本清单

> **来源**：`raf-framework-dependencies/pom.xml`
> **维护原则**：所有 README.md 和 CLAUDE.md 中不包含具体依赖版本号，版本信息统一在本文档维护。
> **更新规则**：每次升级依赖后同步更新本文档及底部升级历史。

---

## Spring 生态

| 属性名 | 组件 | 版本 |
|--------|------|------|
| `spring.boot.version` | Spring Boot | 3.5.14 |
| `spring.cloud.version` | Spring Cloud (2025) | 2025.0.2 |
| `aliababa.dependencies.version` | Spring Cloud Alibaba | 2022.0.0.2 |
| `nacos.client.version` | Nacos Client | 2.5.1 |
| `dubbo.starter.version` | Dubbo Spring Boot Starter | 3.3.4 |
| `dubbo.version` | Dubbo | 3.3.4 |
| `jakarta.servlet-api.version` | Jakarta Servlet API | 6.0.0 |
| `spring-retry.version` | Spring Retry | 2.0.11 |

---

## Maven 插件

| 属性名 | 插件 | 版本 |
|--------|------|------|
| `maven.deploy.plugin.version` | maven-deploy-plugin / maven-install-plugin | 3.1.4 |
| `maven.source.plugin.version` | maven-source-plugin | 3.3.1 |
| `maven.compiler.plugin.version` | maven-compiler-plugin | 3.14.0 |
| `maven.flatten.plugin.version` | flatten-maven-plugin | 1.7.1 |

---

## 数据库 & ORM

| 属性名 | 组件 | 版本 |
|--------|------|------|
| `mysql.version` | MySQL Connector/J | 9.3.0 |
| `mariadb.version` | MariaDB Connector/J | 3.5.3 |
| `mybatis.version` | MyBatis | 3.5.19 |
| `mybatis.spring.version` | MyBatis-Spring | 3.0.5 |
| `mybatis.plus.version` | MyBatis-Plus | 3.5.16 |
| `pagehelper.version` | PageHelper Spring Boot Starter | 6.1.0 |
| `druid.starter.version` | Druid Spring Boot Starter | 1.2.24 |

---

## 缓存

| 属性名 | 组件 | 版本 |
|--------|------|------|
| `redisson.starter.version` | Redisson Spring Boot Starter | 3.34.1 |
| `caffeine.version` | Caffeine | 3.1.8 |

---

## 消息队列

| 属性名 | 组件 | 版本 |
|--------|------|------|
| `rocketmq.version` | RocketMQ Spring Boot Starter | 5.3.2 |
| `kafka.version` | Kafka Clients | 3.9.0 |

---

## 搜索 & 文档数据库

| 属性名 | 组件 | 版本 |
|--------|------|------|
| `elasticsearch.version` | Elasticsearch Java Client | 8.19.14 |
| `mongodb.version` | MongoDB Driver | 5.2.1 |

---

## 分库分表

| 属性名 | 组件 | 版本 |
|--------|------|------|
| `shardingsphere.version` | ShardingSphere | 5.5.1 |

---

## HTTP 客户端

| 属性名 | 组件 | 版本 |
|--------|------|------|
| `okhttp.version` | OkHttp | 4.12.0 |
| `httpclient5.version` | Apache HttpClient 5 | 5.4.2 |
| `httpclient.version` | Apache HttpClient 4 | 4.5.14 |

---

## 安全 & 加密

| 属性名 | 组件 | 版本 |
|--------|------|------|
| `jasypt.boot.version` | Jasypt Spring Boot | 3.0.5 |
| `jjwt.version` | JJWT | 0.12.6 |
| `bouncycastle.version` | Bouncy Castle | 1.78.1 |
| `validation.api.version` | Jakarta Validation API | 3.1.0 |

---

## 云 KMS

| 属性名 | 组件 | 版本 |
|--------|------|------|
| `google-cloud-kms.version` | Google Cloud KMS | 2.50.0 |
| `google-auth-library.version` | Google Auth Library | 1.42.1 |
| `grpc.version` | gRPC | 1.79.0 |
| `protobuf.version` | Protobuf Java | 3.25.1 |
| `aliyun-java-core.version` | Aliyun Java SDK Core | 4.6.3 |
| `aliyun-java-kms.version` | Aliyun Java SDK KMS | 2.16.6 |
| `huaweicloud-sdk.version` | HuaweiCloud SDK | 3.1.60 |

---

## 扩展组件

| 属性名 | 组件 | 版本 |
|--------|------|------|
| `easyexcel.version` | EasyExcel | 4.0.3 |
| `mapstruct.version` | MapStruct | 1.6.3 |
| `seata.version` | Seata | 2.2.0 |
| `sentinel.version` | Sentinel | 1.8.9 |

---

## API 文档

| 属性名 | 组件 | 版本 |
|--------|------|------|
| `springdoc.version` | Springdoc OpenAPI | 2.5.0 |
| `knife4j.version` | Knife4j | 4.5.0 |

---

## 监控 & 可观测性

| 属性名 | 组件 | 版本 |
|--------|------|------|
| `micrometer.prometheus.version` | Micrometer Prometheus | 1.14.5 |
| `dynamic.tp.version` | Dynamic TP | 1.2.1-x |
| `sentry.version` | Sentry Spring Boot | 8.7.0 |

---

## AI

| 属性名 | 组件 | 版本 |
|--------|------|------|
| `spring-ai.version` | Spring AI | 1.0.0 |

---

## 邮件

| 属性名 | 组件 | 版本 |
|--------|------|------|
| `jakarta.mail.version` | Jakarta Mail API | 2.1.3 |
| `jakarta.mail.angus.version` | Angus Mail | 2.0.2 |

---

## 工具库

| 属性名 | 组件 | 版本 |
|--------|------|------|
| `lombok.version` | Lombok | 1.18.36 |
| `commons.lang3.version` | Apache Commons Lang3 | 3.20.0 |
| `commons-io.version` | Apache Commons IO | 2.18.0 |
| `guava.version` | Guava | 33.4.5-jre |
| `hutool.version` | Hutool | 5.8.36 |
| `gson.version` | Gson | 2.12.1 |
| `jackson.version` | Jackson Databind | 2.18.3 |
| `xstream.version` | XStream | 1.4.21 |
| `transmittable.version` | TransmittableThreadLocal | 2.14.5 |
| `poi.version` | Apache POI | 5.4.1 |
| `librepdf.version` | LibrePDF OpenPDF | 2.0.3 |
| `groovy.version` | Groovy | 4.0.26 |
| `zip4j.version` | Zip4j | 1.3.3 |
| `javassist.version` | Javassist | 3.31.0-GA |
| `dom4j.version` | Dom4j | 2.1.4 |

---

## 版本兼容说明

### Spring 生态版本对应关系

| Spring Boot | Spring Cloud | Spring Cloud Alibaba |
|-------------|--------------|----------------------|
| 3.5.x | 2025.0.x (Northfields) | 2022.0.0.2 |
| 3.4.x | 2024.0.x | 2022.0.0.x |
| 3.3.x | 2023.0.x | 2023.0.x.x |

> Spring Cloud 与 Spring Boot 版本必须严格对应，跨版本升级时三者需同步更新。

---

## 升级历史

| 日期 | 变更内容 |
|------|----------|
| 2026-05-08 | Spring Boot 3.4.7 → 3.5.14，Spring Cloud 2024.0.1 → 2025.0.2，gRPC 1.60.0 → 1.79.0，移除 spring.security.version 显式声明（由 Spring Boot BOM 统一管理） |
| 2026-05-07 | javassist 3.30.2-GA → 3.31.0-GA，aliyun-java-kms 2.16.0 → 2.16.6，sentinel 1.8.8 → 1.8.9 |
