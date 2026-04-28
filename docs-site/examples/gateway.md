# Gateway 网关示例

演示 RAF Framework 网关层的 ECIES 加密、ECDSA 签名验证和防重放攻击能力。

## 模块结构

```
raf-example-gateway/
├── src/main/java/
│   ├── GatewayExampleApplication.java
│   ├── config/GatewayRouteConfig.java   # 路由配置
│   └── filter/SecurityFilter.java       # 安全过滤器
├── src/test/java/
│   └── GatewaySecurityIT.java           # 集成测试
├── docker-compose.yml                   # Redis
└── application.yml
```

## 快速运行

**前置条件：** Docker（用于启动 Redis）

```bash
cd examples/raf-example-gateway
docker-compose up -d          # 启动 Redis
mvn spring-boot:run           # 启动网关（端口 8080）
```

## 核心功能

| 功能 | 说明 |
|------|------|
| ECIES 加密 | 请求体使用客户端 EC 公钥加密，服务端私钥解密 |
| ECDSA 签名 | 签名覆盖 `clientKey + ts + nonce + content` |
| 防重放 | 时间窗口 ±300s；Nonce 存 Redis TTL 10 分钟 |
| 路由配置 | 用户/订单/商品服务路由示例 |

## 请求头规范

| 请求头 | 示例值 | 说明 |
|--------|--------|------|
| `X-App-Key` | `MFkwEwYHKoZIzj0C...` | 客户端 EC 公钥 |
| `X-App-Ts` | `1765790000000` | 13 位 Unix 时间戳（毫秒） |
| `X-App-Nonce` | `a1b2c3d4e5f6...` | 32 位 UUID，一次性使用 |
| `X-App-Sign` | `MEQCIB...` | ECDSA 签名 |

## 测试验证

```bash
# 运行集成测试（需要 Docker）
mvn test -Dspring.profiles.active=test
```

测试覆盖：
- 正常请求 → `200 OK`
- 缺少签名头 → `401 Unauthorized`
- Nonce 重复 → `401 Unauthorized`
- 时间戳超窗口 → `401 Unauthorized`

## 关键配置

```yaml
raf:
  gateway:
    security:
      enabled: true
      replay-window-seconds: 300
      nonce-ttl-minutes: 10
```

详见 [示例源码](https://github.com/JerryRaf/raf-framework/tree/master/examples/raf-example-gateway) | [安全设计规范](/components/api-security)
