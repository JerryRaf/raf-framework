# App API 安全设计

> **范围**：移动端 App ↔ 网关加密通信协议。
> **原则**：每个请求均需签名；每个响应体均需加密。协议握手失败时以明文返回 401，不会携带解密内容。

---

## 1. 安全通信协议

### 1.1 请求（App → 网关）

App 所有请求通过网关接入，请求体使用 AES 加密，外层信封使用 ECDSA 签名。

**HTTP 请求头：**

| 请求头 | 示例值 | 说明 |
|--------|--------|------|
| `X-App-Key` | `MFkwEwYHKoZIzj0C...` | 客户端 EC 公钥（注册时下发） |
| `X-App-Ts` | `1765790000000` | 13 位 Unix 时间戳（毫秒） |
| `X-App-Nonce` | `a1b2c3d4e5f6...` | 32 位 UUID，一次性使用 |
| `X-App-Sign` | `MEQCIB...` | ECDSA 签名，覆盖 `clientKey+ts+nonce+content` |

**加密前明文请求体：**

```json
{
  "token": "eyJhbGciOi...",
  "client": {
    "lang": "zh_CN",
    "version": "3.10.0",
    "osType": "ios",
    "osVersion": "18.0.1",
    "model": "iPhone16,1",
    "marketModel": "iPhone 15 Pro",
    "deviceId": "F7801...",
    "pushToken": "bd76..."
  },
  "data": {
    "email": "user@example.com"
  }
}
```

**加密后 HTTP 请求体：**

```json
{
  "content": "yJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

---

### 1.2 响应 — 业务结果（加密）

HTTP `200 OK` 的响应体均为 AES 加密。客户端须解密 `content` 才能获取实际结果。

**HTTP 状态码：** `200 OK`

**HTTP 响应体：**

```json
{
  "content": "U2FsdGVkX1+...",
  "sign": "MEQCIH..."
}
```

**客户端解密 `content` 后：**

**情况 A — 业务成功：**

```json
{
  "code": 0,
  "msg": "success",
  "data": { "orderId": "8888" },
  "timestamp": 1735790000500
}
```

**情况 B — 业务失败（如 Token 无效、参数校验失败）：**

```json
{
  "code": 40100,
  "msg": "Token 无效",
  "data": null,
  "timestamp": 1735790000500
}
```

---

### 1.3 响应 — 协议握手失败（明文）

安全握手失败（时间戳超出窗口、Nonce 重复、签名错误）时，网关返回**明文** 401，响应体**不加密**。

**HTTP 状态码：** `401 Unauthorized`

**HTTP 响应体：**

```json
{
  "code": 40101,
  "msg": "安全握手失败",
  "timestamp": 1735790000500
}
```

---

### 1.4 响应 — 网关 / 基础设施错误（明文）

来自网关或上游服务的非 200 错误以明文返回（不加密）。

**HTTP 状态码：** `4xx` / `5xx`

**HTTP 响应体：**

```json
{
  "timestamp": 1735870000794,
  "code": 405,
  "msg": "Method Not Allowed"
}
```

---

## 3. 防重放攻击

| 校验项 | 规则 |
|--------|------|
| 时间戳窗口 | `|服务器时间 - X-App-Ts| ≤ 300 秒` |
| Nonce 唯一性 | Nonce 存入 Redis，TTL 10 分钟；重复则拒绝 |
| 签名绑定 | 签名覆盖 `clientKey + ts + nonce + 加密内容` |

---

## 4. 密钥管理

| 密钥 | 算法 | 轮换策略 |
|------|------|----------|
| 客户端 EC 密钥对 | EC P-256 | App 安装时生成；重装时重新注册 |
| 会话 AES 密钥 | AES-256-GCM | 通过 ECDH 密钥交换逐会话派生 |
| 服务端签名密钥 | ECDSA P-256 | 每季度轮换；旧密钥在轮换后 7 天内继续有效 |

---

## 5. 安全检查清单

- [ ] 所有请求携带 `X-App-Sign`；网关拒绝未签名请求并返回 401
- [ ] Nonce 缓存使用 Redis（含 TTL），禁止使用内存 Map
- [ ] 任何响应体中均不出现堆栈信息
- [ ] 客户端公钥按设备存储，不按用户存储
- [ ] AES 密钥通过 ECDH 派生，禁止在网络中传输