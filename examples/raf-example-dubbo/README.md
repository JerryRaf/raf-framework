# raf-example-dubbo

Dubbo RPC + Nacos 服务发现示例，演示 raf-framework-dubbo-starter 的核心功能。

## 功能演示

- Dubbo Provider/Consumer 双模块
- Nacos 服务注册与发现
- traceId 在 RPC 调用链中自动传播
- 异常统一处理（CustExceptionFilter）
- JSR-303 参数校验（ValidFilter）

## 模块结构

```
raf-example-dubbo/
├── dubbo-provider/    # 服务提供者（端口 20880）
├── dubbo-consumer/    # 服务消费者（端口 8081）
└── dubbo-facade/      # 接口定义（共享）
```

## 环境要求

- JDK 17+
- Maven 3.8.8+
- Nacos 2.x（本地或 Docker）

## 快速启动

**1. 启动 Nacos**

```bash
docker run -d --name nacos \
  -e MODE=standalone \
  -p 8848:8848 -p 9848:9848 \
  nacos/nacos-server:v2.3.0
```

**2. 启动 Provider**

```bash
cd examples/raf-example-dubbo/dubbo-provider
mvn spring-boot:run
```

**3. 启动 Consumer**

```bash
cd examples/raf-example-dubbo/dubbo-consumer
mvn spring-boot:run
```

## 核心配置说明

```yaml
# dubbo-provider/application.yml
dubbo:
  application:
    name: dubbo-provider
  registry:
    address: nacos://127.0.0.1:8848
  protocol:
    name: dubbo
    port: 20880
  provider:
    filter: providerTraceFilter,custExceptionFilter,validFilter
```

## API 接口列表

通过 Consumer 的 HTTP 接口触发 Dubbo 调用：

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/user/{id}` | Consumer 调用 Provider 查询用户 |
| POST | `/api/user` | Consumer 调用 Provider 创建用户 |
| GET | `/api/trace` | 验证 traceId 在 RPC 链路中传播 |
