# raf-framework GitHub 开源完善方案

**日期**: 2026-04-29
**目标受众**: 国内 Java 开发者
**目标**: 达到高质量 GitHub 开源项目标准

---

## 背景与现状

raf-framework 是企业级微服务开发框架，封装 18+ 中间件 Starter，支持 BOM/Parent 双接入模式。

**已具备的基础设施**（不需要改动）：
- LICENSE (MIT)
- .github/workflows — CI、发布、CodeQL、依赖安全检查、文档部署
- Issue/PR 模板
- Maven Central 发布配置（GPG 签名 + Sonatype）
- VitePress 文档站骨架（docs-site/）
- CHANGELOG 格式规范（Keep a Changelog + Semantic Versioning）

**核心差距**：文档站 18 个 Starter 中只有 2 个有文档页；5 个示例项目无 README；README 缺少定位说明和架构图；CI 有分支名 bug。

---

## 范围

本方案覆盖以下内容，CHANGELOG 历史版本**不在范围内**。

---

## P0：第一印象

### P0-1 README 重构

**改动点**：

1. **定位说明段落**（badge 下方，项目介绍之前）

   说明 raf-framework 与普通脚手架的区别：它是"中间件接入层"，解决每个微服务项目重复写 Redis 配置、MQ 消费者、多数据源路由、全局异常处理的问题。引入依赖即生效，不引入零侵入。

2. **架构图**（Mermaid，GitHub 原生渲染）

   分层图展示：应用层 → Starter 层 → Autoconfigure 核心层 → 中间件层，以及 BOM/Parent 双接入模式。

3. **与同类项目对比表**（3 行）

   | 对比维度 | raf-framework | 纯 Spring Boot 脚手架 | JHipster |
   |---|---|---|---|
   | 接入方式 | BOM/Parent 双模式 | 复制粘贴 | 代码生成 |
   | 中间件覆盖 | 18+ Starter | 按需手写 | 有限 |
   | 侵入性 | 零侵入 | 高 | 高 |

4. **Badge 区新增文档站链接**

   ```
   [![文档](https://img.shields.io/badge/文档-GitHub%20Pages-blue)](https://jerryraf.github.io/raf-framework/)
   ```

5. **Discussions 引导**（贡献指南小节前）

   ```
   遇到问题？
   - 使用问题 → GitHub Discussions
   - 发现 Bug → GitHub Issues（Bug Report 模板）
   - 功能建议 → GitHub Issues（Feature Request 模板）
   ```

### P0-2 CI 分支名修复

**文件**: `.github/workflows/ci.yml`

**问题**: push 触发监听 `master`，PR 触发监听 `main/master`，不一致。

**修复**: PR 触发分支统一改为只监听 `master`，与实际主分支一致。

---

## P1：上手体验

### P1-1 文档站 Starter 文档补全

**每个文档页的固定结构**（四节）：

```markdown
## 功能概述
## 配置项（表格：配置键 | 类型 | 默认值 | 说明）
## 快速接入（Maven 依赖 + 最小配置）
## 核心用法（代码示例）
## 常见问题
```

**第一批（高频，优先完成）**：

| Starter | 文档页路径 | 核心内容要点 |
|---|---|---|
| web-starter | `docs-site/components/web.md` | 全局异常处理、RafResult 响应封装、HTTP 请求日志（raf.log）、CORS 配置 |
| redis-starter | `docs-site/components/redis.md` | RedisUtil 常用方法、Redisson 分布式锁示例、自定义缓存 TTL（raf.customCache） |
| mybatis-starter | `docs-site/components/mybatis.md` | 多数据源配置、@DsSelector 注解用法、MyBatis-Plus 分页、慢 SQL 告警阈值 |
| datasource-starter | `docs-site/components/datasource.md` | Druid 连接池参数、监控页配置、多数据源路由原理 |
| rabbit-starter | `docs-site/components/rabbit.md` | Provider/Consumer 分离配置、延时队列（死信交换机模式）、消息确认机制 |
| rocketmq-starter | `docs-site/components/rocketmq.md` | 四种消息类型（NORMAL/FIFO/DELAY/TRANSACTION）、事务消息最佳实践 |

**第二批（中频）**：

| Starter | 文档页路径 |
|---|---|
| kafka-starter | `docs-site/components/kafka.md` |
| dubbo-starter | `docs-site/components/dubbo.md` |
| nacos-config-starter | `docs-site/components/nacos-config.md` |
| nacos-discovery-starter | `docs-site/components/nacos-discovery.md` |
| gateway-starter | `docs-site/components/gateway.md` |
| mongodb-starter | `docs-site/components/mongodb.md` |

**第三批（低频）**：

| Starter | 文档页路径 |
|---|---|
| elasticsearch-starter | `docs-site/components/elasticsearch.md` |
| shardingsphere-starter | `docs-site/components/shardingsphere.md` |
| okhttp-starter | `docs-site/components/okhttp.md` |
| openapi-starter | `docs-site/components/openapi.md` |
| monitor-starter | `docs-site/components/monitor.md` |
| sentry-starter | `docs-site/components/sentry.md` |

**新增通用页面**：

- `docs-site/guide/faq.md` — 常见问题（启动报错、配置不生效、版本冲突）
- `docs-site/guide/upgrade.md` — 版本升级指南（2.x → 3.x Breaking Changes）

**同步更新 VitePress 侧边栏**：`docs-site/.vitepress/config.ts` 加入所有新页面的导航条目。

### P1-2 示例项目 README 补全

**5 个缺 README 的示例**，每个按统一模板：

```markdown
# 示例名称

## 功能演示
## 环境要求
## 快速启动
## 核心配置说明
## API 接口列表
```

| 示例 | 重点说明内容 |
|---|---|
| raf-example-basic | Web 基础、全局异常、RafResult 响应格式演示 |
| raf-example-redis | RedisUtil 用法、Redisson 分布式锁示例接口 |
| raf-example-dubbo | Provider/Consumer 双模块说明、Nacos 注册配置 |
| raf-example-distributed-tx | Seata AT 模式、三服务（order/payment/product）协调流程图 |
| raf-example-full-stack | 5 服务电商系统整体架构图、各服务职责、启动顺序 |

---

## P2：社区运营

### P2-1 CONTRIBUTING.md 完善

在现有内容基础上新增四个章节：

**① 本地开发环境搭建**

提供 `.github/dev/docker-compose.yml`，包含 Redis、MySQL、RabbitMQ 最小化配置，一条命令启动开发依赖：

```bash
docker-compose -f .github/dev/docker-compose.yml up -d
```

**② 代码风格**

- 使用 IntelliJ IDEA 默认格式化（不引入 Checkstyle，降低贡献门槛）
- 遵循 `.editorconfig` 配置（缩进、换行符、编码）
- 代码注释使用英文

**③ 如何新增一个 Starter（5 步）**

```
1. 在 raf-framework-starter/ 下创建新模块
2. 创建 *Properties 类（@ConfigurationProperties）
3. 创建 *Config 类（@Configuration + @ConditionalOnProperty）
4. 注册到 AutoConfiguration.imports
5. 编写单元测试（ApplicationContextRunner）+ 文档页
```

**④ 只跑单模块测试**

```bash
mvn test -pl raf-framework-starter/raf-framework-redis-starter -am \
  -s "D:\Program Files\apache-maven-3.9.10\conf\settings-raf.xml"
```

### P2-2 添加 `.editorconfig`

**文件**: `.editorconfig`（根目录）

```ini
root = true

[*]
charset = utf-8
end_of_line = lf
indent_style = space
indent_size = 4
trim_trailing_whitespace = true
insert_final_newline = true

[*.{yml,yaml,json}]
indent_size = 2

[*.md]
trim_trailing_whitespace = false
```

---

## P3：加分项

### P3-1 GitHub Pages 确认

确认 `docs.yml` workflow 已正确配置 GitHub Pages 部署，文档站 URL 在 README badge 中可访问。

---

## 工作量汇总

| 优先级 | 项目 | 涉及文件 |
|---|---|---|
| P0 | README 重构 | `README.md` |
| P0 | CI 分支名修复 | `.github/workflows/ci.yml` |
| P1 | 文档站第一批（6 个 Starter） | 6 个 `.md` 文件 |
| P1 | 文档站第二批（6 个 Starter） | 6 个 `.md` 文件 |
| P1 | 文档站第三批（6 个 Starter） | 6 个 `.md` 文件 |
| P1 | 文档站通用页面（FAQ + 升级指南） | 2 个 `.md` 文件 |
| P1 | VitePress 侧边栏更新 | `docs-site/.vitepress/config.ts` |
| P1 | 示例项目 README（5 个） | 5 个 `README.md` |
| P2 | CONTRIBUTING.md 完善 | `CONTRIBUTING.md` |
| P2 | Docker Compose 开发环境 | `.github/dev/docker-compose.yml` |
| P2 | EditorConfig | `.editorconfig` |
| P3 | GitHub Pages 确认 | — |

---

## 不在范围内

- CHANGELOG 历史版本补充
- 多语言（英文）文档
- Checkstyle / SpotBugs 配置
- Gitpod / GitHub Codespaces 配置
