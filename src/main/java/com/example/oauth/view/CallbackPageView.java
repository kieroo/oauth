package com.example.oauth.view;

import com.example.oauth.util.HtmlEscaper;

public final class CallbackPageView {
    private CallbackPageView() {
    }

    public static String render(String code, String state) {
        String safeCode = HtmlEscaper.escape(code);
        String safeState = HtmlEscaper.escape(state);
        return """
                <html>
                <head><meta charset=\"UTF-8\"><title>OAuth Callback</title></head>
                <body>
                  <h2>授权成功</h2>
                  <p>code: <code>%s</code></p>
                  <p>state: <code>%s</code></p>
                  <p>使用如下命令换取 token：</p>
                  <pre>curl -X POST http://localhost:8080/token \\
                    -H 'Content-Type: application/x-www-form-urlencoded' \\
                    -d 'grant_type=authorization_code&client_id=demo-client&redirect_uri=http://localhost:8080/callback&code=%s&code_verifier=demo-verifier'</pre>
                </body>
                </html>
                """.formatted(safeCode, safeState, safeCode);
    }
}
