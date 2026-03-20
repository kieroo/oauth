package com.example.oauth.view;

public final class HomePageView {
    private HomePageView() {
    }

    public static String render() {
        return """
                <html>
                <head><meta charset=\"UTF-8\"><title>Java OAuth Demo</title></head>
                <body>
                  <h1>Java OAuth 2.0 Demo</h1>
                  <p>这是一个纯 Java 的最小 OAuth 2.0 示例，内置了一个演示客户端：</p>
                  <ul>
                    <li>client_id: <code>demo-client</code></li>
                    <li>redirect_uri: <code>http://localhost:8080/callback</code></li>
                    <li>scope: <code>read</code></li>
                  </ul>
                  <p>可以直接点击下面的链接获取授权码：</p>
                  <p><a href=\"/authorize?response_type=code&client_id=demo-client&redirect_uri=http://localhost:8080/callback&scope=read&state=demo-state&code_challenge=demo-verifier&code_challenge_method=plain\">开始授权</a></p>
                  <p>然后使用页面上返回的 code 调用 <code>POST /token</code> 获取 access_token，再访问 <code>GET /resource</code>。</p>
                </body>
                </html>
                """;
    }
}
