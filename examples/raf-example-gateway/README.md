# RAF Example - Gateway Security

演示 RAF Framework 网关层的安全能力：ECIES 加密、ECDSA 签名验证、防重放攻击。

## 快速运行

### 方式 A：Docker Compose（推荐）

```bash
docker-compose up -d
```

### 方式 B：本地运行

前置条件：Redis 运行在 localhost:6379

```bash
mvn spring-boot:run
```

## 核心功能

| 功能 | 说明 |
|------|------|
| ECIES 加密 | 请求体使用 EC P-256 密钥对加密 |
| ECDSA 签名 | 防止请求被篡改 |
| 防重放攻击 | 时间窗口 ±300s + Nonce 唯一性校验 |
| 路由配置 | /api/users/**, /api/orders/**, /api/products/** |

## 请求头规范

| 请求头 | 说明 |
|--------|------|
| `X-App-Key` | 客户端 EC 公钥（注册时下发） |
| `X-App-Ts` | 13 位 Unix 时间戳（毫秒） |
| `X-App-Nonce` | 32 位 UUID，一次性使用 |
| `X-App-Sign` | ECDSA 签名，覆盖 clientKey+ts+nonce+content |

## 运行测试

```bash
mvn test
```
