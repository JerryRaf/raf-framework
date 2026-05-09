
## 移动端（iOS / Android）字段类型规范

与 App 交互的 API，字段类型需要额外约束，避免平台解析差异导致的线上事故。

### 类型选用规则

| 字段类别 | 推荐 JSON 类型 | 示例值 | 原因 |
|----------|--------------|--------|------|
| 枚举 / 状态 | `string` | `"PENDING"` `"ACTIVE"` | 语义自描述；新增枚举值不影响旧版 App；switch-case 可读性高 |
| 布尔值 | `boolean` | `true` `false` | 类型安全；`"true"` 字符串在强类型语言（Swift / Kotlin）解析时易抛异常 |
| Long 型 ID | `string` | `"1234567890123456789"` | JS `Number` 最大安全整数为 2^53-1，超出后精度截断；H5 / WebView 场景必须字符串化 |
| 金额 / 价格 | `string` | `"99.99"` | 避免浮点精度丢失（如 `0.1 + 0.2 ≠ 0.3`）；前端展示和计算均以字符串传递 |
| 普通整数（计数、分页） | `number` | `20` `1024` | 无精度风险，保持原生类型，减少转换开销 |
| 时间戳 | `string`（ISO 8601） | `"2026-05-08T10:00:00Z"` | 时区明确，跨平台解析一致；避免毫秒 / 秒混淆 |

### 反例对比

```json
// 错误示范
{
  "status": 1,
  "isVip": "true",
  "orderId": 1234567890123456789,
  "amount": 99.99
}

// 正确示范
{
  "status": "PENDING",
  "isVip": true,
  "orderId": "1234567890123456789",
  "amount": "99.99"
}
```

### 枚举扩展兼容性

使用字符串枚举时，App 端必须实现**未知值兜底**逻辑：

```swift
// iOS Swift 示例
switch status {
case "PENDING": showPending()
case "ACTIVE": showActive()
default: showUnknown()  // 新版本新增的枚举值，旧 App 不崩溃
}
```

```kotlin
// Android Kotlin 示例
when (status) {
    "PENDING" -> showPending()
    "ACTIVE"  -> showActive()
    else      -> showUnknown()  // 兜底，防止新枚举值导致崩溃
}
```

> **版本兼容原则**：枚举字符串值一旦发布，只允许新增，禁止修改或删除，否则旧版 App 兜底逻辑失效。
