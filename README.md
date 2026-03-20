# Java OAuth Demo

这是一个使用 **Java 17** 编写的最小 OAuth 2.0 示例程序，不依赖 Spring 等大型框架，直接使用 JDK 自带的 `HttpServer` 实现了一个可运行的 Demo。

## MVC 结构

项目现在按照更标准的 MVC 方式拆分：

- `controller/`：处理 HTTP 请求和路由分发。
- `service/`：封装授权码、令牌、客户端注册和 PKCE 等业务逻辑。
- `model/`：定义 `Client`、`AuthorizationCode`、`AccessToken` 等领域模型。
- `view/`：负责首页和回调页的 HTML 渲染。
- `util/`：放置表单解析、响应写回、HTML 转义、随机令牌生成等通用工具。

入口仍然是 `com.example.oauth.OAuthDemoServer`，负责组装各层并启动服务器。

## 功能

- `GET /authorize`：模拟授权端点，签发授权码。
- `POST /token`：使用授权码换取 Bearer Token。
- `GET /resource`：访问受保护资源。
- `GET /callback`：展示授权回调结果页面。
- 支持简化版 PKCE 校验（`plain` 和 `S256`）。

## 运行方式

推荐直接使用 JDK 自带命令运行，无需下载额外依赖：

```bash
mkdir -p out
javac -d out $(find src/main/java -name "*.java") $(find src/test/java -name "*.java")
java -cp out com.example.oauth.OAuthDemoServer
```

运行自检：

```bash
java -cp out com.example.oauth.OAuthDemoServerSelfTest
```

启动后访问：

- 首页：<http://localhost:8080/>
- 授权端点：<http://localhost:8080/authorize>
- Token 端点：<http://localhost:8080/token>
- 资源端点：<http://localhost:8080/resource>
- 回调页面：<http://localhost:8080/callback>

## 内置客户端

- `client_id`: `demo-client`
- `redirect_uri`: `http://localhost:8080/callback`
- `scope`: `read`
- 示例 `code_verifier`: `demo-verifier`

## 示例流程

### 1. 获取授权码

直接访问：

```text
http://localhost:8080/authorize?response_type=code&client_id=demo-client&redirect_uri=http://localhost:8080/callback&scope=read&state=demo-state&code_challenge=demo-verifier&code_challenge_method=plain
```

浏览器会被重定向到 `/callback`，并显示授权码 `code`。

### 2. 换取 Access Token

```bash
curl -X POST http://localhost:8080/token \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -d 'grant_type=authorization_code&client_id=demo-client&redirect_uri=http://localhost:8080/callback&code=替换成上一步的code&code_verifier=demo-verifier'
```

### 3. 访问受保护资源

```bash
curl http://localhost:8080/resource \
  -H 'Authorization: Bearer 替换成access_token'
```

## 说明

这是一个教学性质的 Demo，方便理解 OAuth 授权码模式的核心流程，不适合直接用于生产环境。生产环境还需要补充：

- 用户登录和授权确认页面
- 持久化存储
- 客户端认证
- 刷新令牌
- 更严格的错误处理和安全策略
