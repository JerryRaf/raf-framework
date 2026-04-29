# raf-example-basic

基础 Web 应用示例，演示 raf-framework-web-starter 的核心功能。

## 功能演示

- 统一响应封装（`@ResponseResult`）
- 全局异常处理（四层异常体系）
- HTTP 请求日志（可配置级别）
- CORS 跨域配置
- 参数校验（JSR-303）

## 环境要求

- JDK 17+
- Maven 3.8.8+

## 快速启动

```bash
cd examples/raf-example-basic
mvn spring-boot:run
```

服务启动后访问：`http://localhost:8080`

## 核心配置说明

```yaml
# application.yml
raf:
  log:
    level: REQ_BODY    # 开发环境记录请求体
  cors:
    enabled: true
    path: /**
```

## API 接口列表

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/demo/hello` | 返回标准响应示例 |
| GET | `/api/demo/error/business` | 触发 BusinessException 示例 |
| GET | `/api/demo/error/system` | 触发 SystemException 示例 |
| POST | `/api/demo/validate` | 参数校验示例 |

### 响应示例

**成功响应：**
```json
{
  "code": 200,
  "msg": "成功！",
  "data": "Hello, RAF Framework!"
}
```

**业务异常响应：**
```json
{
  "code": 10001,
  "msg": "用户不存在",
  "data": null
}
```
