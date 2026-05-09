# raf-framework-ai-starter

AI 大模型统一接入 starter，封装 OpenAI、Claude（Anthropic）、DeepSeek 三个主流 Provider，提供同步/流式调用、多模型路由和会话管理，业务层注入 `AiService` 即用。

## 快速开始

### 1. 添加依赖

```xml
<dependency>
    <groupId>io.github.jerryraf</groupId>
    <artifactId>raf-framework-ai-starter</artifactId>
</dependency>
```

### 2. 配置

```yaml
raf:
  ai:
    enabled: true
    default-provider: deepseek          # 默认 Provider

    # 场景路由（可选）
    routes:
      code: claude
      summary: openai

    # 会话管理
    session:
      max-history: 20                   # 最大历史消息条数
      ttl: 3600                         # 会话 TTL（秒），0 不过期

    providers:
      openai:
        enabled: true
        api-key: ${OPENAI_API_KEY}
        base-url: https://api.openai.com/v1
        model: gpt-4o
        temperature: 0.7
        max-tokens: 4096
        timeout: 60

      claude:
        enabled: true
        api-key: ${ANTHROPIC_API_KEY}
        base-url: https://api.anthropic.com
        model: claude-opus-4-5
        temperature: 0.7
        max-tokens: 4096
        timeout: 60

      deepseek:
        enabled: true
        api-key: ${DEEPSEEK_API_KEY}
        base-url: https://api.deepseek.com/v1
        model: deepseek-chat
        temperature: 0.7
        max-tokens: 4096
        timeout: 60
```

### 3. 注入使用

```java
@RestController
@RequiredArgsConstructor
public class MyController {

    private final AiService aiService;

    // 同步调用（走默认 Provider）
    @GetMapping("/chat")
    public String chat(@RequestParam String prompt) {
        return aiService.chat(prompt);
    }

    // 流式调用（SSE）
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> stream(@RequestParam String prompt) {
        return aiService.streamChat(prompt);
    }
}
```

---

## AiService 接口

```java
public interface AiService {
    // 简单同步（默认 Provider）
    String chat(String prompt);

    // 指定 Provider 同步
    String chat(String provider, String prompt);

    // 完整参数同步
    String chat(AiRequest request);

    // 简单流式（默认 Provider）
    Flux<String> streamChat(String prompt);

    // 完整参数流式
    Flux<String> streamChat(AiRequest request);
}
```

---

## AiRequest 参数说明

```java
AiRequest request = AiRequest.builder()
    .provider("claude")          // 可选：直接指定 Provider，优先级最高
    .scene("code")               // 可选：触发场景路由
    .sessionId("user-123")       // 可选：启用会话管理，自动维护历史
    .systemPrompt("你是一个...")  // 可选：系统提示词
    .prompt("帮我写一个...")      // 必填：用户输入
    .temperature(0.7)            // 可选：覆盖默认温度
    .maxTokens(2048)             // 可选：覆盖默认 maxTokens
    .build();
```

---

## 多模型路由

三层优先级，从高到低：

| 优先级 | 条件 | 说明 |
|--------|------|------|
| 1 | `request.provider` 不为空 | 直接使用指定 Provider |
| 2 | `request.scene` 不为空 | 查找 `raf.ai.routes.{scene}` 配置 |
| 3 | 兜底 | 使用 `raf.ai.default-provider` |

**示例：**

```java
// 走默认 Provider（deepseek）
aiService.chat("你好");

// 直接指定 claude
aiService.chat("claude", "帮我写代码");

// 触发场景路由 code→claude
aiService.chat(AiRequest.builder().scene("code").prompt("写一个排序算法").build());
```

---

## 会话管理

传入 `sessionId` 即可启用多轮对话，框架自动维护历史消息：

```java
// 第一轮
aiService.chat(AiRequest.builder()
    .sessionId("user-123")
    .prompt("我叫 Jerry")
    .build());

// 第二轮（AI 能记住上文）
aiService.chat(AiRequest.builder()
    .sessionId("user-123")
    .prompt("我叫什么名字？")
    .build());
```

**存储后端自动选择：**
- 检测到 `RedissonClient` Bean → 使用 Redis（集群友好，支持 TTL）
- 否则 → 使用内存（重启丢失，适合开发/单机）

**滑动窗口：** 超过 `max-history` 条时，自动丢弃最早的消息。

---

## DeepSeek 接入说明

DeepSeek 兼容 OpenAI 协议，无需额外依赖，只需配置 `base-url` 指向 DeepSeek 端点：

```yaml
raf:
  ai:
    providers:
      deepseek:
        enabled: true
        api-key: ${DEEPSEEK_API_KEY}
        base-url: https://api.deepseek.com/v1
        model: deepseek-chat
```

---

## 异常处理

| 场景 | 异常类型 | 错误码 |
|------|----------|--------|
| prompt 为空 | `BusinessException` | 10700 (PARAM_ERROR) |
| Provider 名称不存在 | `BusinessException` | 10700 (PARAM_ERROR) |
| API 调用失败（超时、Key 无效等） | `InfrastructureException` | 10500 (SERVER_ERROR) |

---

## 示例项目

参考 `examples/raf-example-ai-starter`，演示以下接口：

| 接口 | 说明 |
|------|------|
| `GET /ai/chat?prompt=xxx` | 同步调用（默认 Provider） |
| `GET /ai/chat/{provider}?prompt=xxx` | 指定 Provider 同步调用 |
| `GET /ai/stream?prompt=xxx` | 流式调用（SSE） |
| `GET /ai/session?sessionId=xxx&prompt=xxx` | 多轮会话流式对话 |
| `POST /ai/full` | 完整参数调用 |
