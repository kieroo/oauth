package com.example.oauth;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

/**
 * 一个最小可运行的 OAuth 2.0 Demo：
 * 1. /authorize 颁发授权码
 * 2. /token 颁发 Bearer Token
 * 3. /resource 验证 Token 并返回受保护资源
 */
public class OAuthDemoServer {
    private static final int PORT = 8080;

    public static void main(String[] args) throws IOException {
        ClientRegistry clientRegistry = new ClientRegistry();
        AuthorizationCodeService authorizationCodeService = new AuthorizationCodeService();
        TokenService tokenService = new TokenService();

        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext("/", new HomeHandler());
        server.createContext("/authorize", new AuthorizeHandler(clientRegistry, authorizationCodeService));
        server.createContext("/token", new TokenHandler(clientRegistry, authorizationCodeService, tokenService));
        server.createContext("/resource", new ResourceHandler(tokenService));
        server.createContext("/callback", new CallbackHandler());
        server.setExecutor(Executors.newFixedThreadPool(8));
        server.start();

        System.out.println("OAuth Demo server started at http://localhost:" + PORT);
        System.out.println("Open http://localhost:" + PORT + " to view the demo instructions.");
    }

    static final class HomeHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String html = """
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
            writeResponse(exchange, 200, "text/html; charset=UTF-8", html);
        }
    }

    static final class AuthorizeHandler implements HttpHandler {
        private final ClientRegistry clientRegistry;
        private final AuthorizationCodeService authorizationCodeService;

        AuthorizeHandler(ClientRegistry clientRegistry, AuthorizationCodeService authorizationCodeService) {
            this.clientRegistry = clientRegistry;
            this.authorizationCodeService = authorizationCodeService;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                writeJson(exchange, 405, "{\"error\":\"method_not_allowed\"}");
                return;
            }

            Map<String, String> query = parseForm(exchange.getRequestURI().getRawQuery());
            String responseType = query.get("response_type");
            String clientId = query.get("client_id");
            String redirectUri = query.get("redirect_uri");
            String state = query.getOrDefault("state", "");
            String scope = query.getOrDefault("scope", "read");
            String codeChallenge = query.getOrDefault("code_challenge", "");
            String challengeMethod = query.getOrDefault("code_challenge_method", "plain");

            if (!"code".equals(responseType)) {
                writeJson(exchange, 400, "{\"error\":\"unsupported_response_type\"}");
                return;
            }
            if (!clientRegistry.isValidRedirect(clientId, redirectUri)) {
                writeJson(exchange, 400, "{\"error\":\"invalid_client_or_redirect_uri\"}");
                return;
            }

            AuthorizationCode authorizationCode = authorizationCodeService.createCode(
                    clientId,
                    redirectUri,
                    "demo-user",
                    scope,
                    codeChallenge,
                    challengeMethod
            );

            String location = redirectUri
                    + (redirectUri.contains("?") ? "&" : "?")
                    + "code=" + urlEncode(authorizationCode.value())
                    + "&state=" + urlEncode(state);
            redirect(exchange, location);
        }
    }

    static final class TokenHandler implements HttpHandler {
        private final ClientRegistry clientRegistry;
        private final AuthorizationCodeService authorizationCodeService;
        private final TokenService tokenService;

        TokenHandler(ClientRegistry clientRegistry,
                     AuthorizationCodeService authorizationCodeService,
                     TokenService tokenService) {
            this.clientRegistry = clientRegistry;
            this.authorizationCodeService = authorizationCodeService;
            this.tokenService = tokenService;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                writeJson(exchange, 405, "{\"error\":\"method_not_allowed\"}");
                return;
            }

            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            Map<String, String> form = parseForm(body);
            String grantType = form.get("grant_type");
            String code = form.get("code");
            String clientId = form.get("client_id");
            String redirectUri = form.get("redirect_uri");
            String codeVerifier = form.getOrDefault("code_verifier", "");

            if (!"authorization_code".equals(grantType)) {
                writeJson(exchange, 400, "{\"error\":\"unsupported_grant_type\"}");
                return;
            }
            if (!clientRegistry.isValidRedirect(clientId, redirectUri)) {
                writeJson(exchange, 400, "{\"error\":\"invalid_client\"}");
                return;
            }

            Optional<AuthorizationCode> authorizationCode = authorizationCodeService.consumeCode(code);
            if (authorizationCode.isEmpty()) {
                writeJson(exchange, 400, "{\"error\":\"invalid_grant\"}");
                return;
            }

            AuthorizationCode storedCode = authorizationCode.get();
            if (!Objects.equals(storedCode.clientId(), clientId) || !Objects.equals(storedCode.redirectUri(), redirectUri)) {
                writeJson(exchange, 400, "{\"error\":\"invalid_grant\"}");
                return;
            }
            if (!PkceVerifier.matches(storedCode.codeChallenge(), storedCode.codeChallengeMethod(), codeVerifier)) {
                writeJson(exchange, 400, "{\"error\":\"invalid_code_verifier\"}");
                return;
            }

            AccessToken token = tokenService.issueToken(storedCode.subject(), storedCode.scope(), Duration.ofMinutes(30));
            String json = """
                    {
                      "access_token":"%s",
                      "token_type":"Bearer",
                      "expires_in":%d,
                      "scope":"%s"
                    }
                    """.formatted(token.value(), token.expiresInSeconds(), token.scope());
            writeJson(exchange, 200, json);
        }
    }

    static final class ResourceHandler implements HttpHandler {
        private final TokenService tokenService;

        ResourceHandler(TokenService tokenService) {
            this.tokenService = tokenService;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                writeJson(exchange, 405, "{\"error\":\"method_not_allowed\"}");
                return;
            }

            String authorization = exchange.getRequestHeaders().getFirst("Authorization");
            if (authorization == null || !authorization.startsWith("Bearer ")) {
                writeJson(exchange, 401, "{\"error\":\"missing_bearer_token\"}");
                return;
            }

            String tokenValue = authorization.substring("Bearer ".length());
            Optional<AccessToken> token = tokenService.findValidToken(tokenValue);
            if (token.isEmpty()) {
                writeJson(exchange, 401, "{\"error\":\"invalid_or_expired_token\"}");
                return;
            }

            AccessToken accessToken = token.get();
            String json = """
                    {
                      "message":"This is a protected resource.",
                      "subject":"%s",
                      "scope":"%s",
                      "expires_at":"%s"
                    }
                    """.formatted(accessToken.subject(), accessToken.scope(), accessToken.expiresAt());
            writeJson(exchange, 200, json);
        }
    }

    static final class CallbackHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            URI uri = exchange.getRequestURI();
            Map<String, String> query = parseForm(uri.getRawQuery());
            String html = """
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
                    """.formatted(
                    escapeHtml(query.getOrDefault("code", "")),
                    escapeHtml(query.getOrDefault("state", "")),
                    escapeHtml(query.getOrDefault("code", ""))
            );
            writeResponse(exchange, 200, "text/html; charset=UTF-8", html);
        }
    }

    record Client(String clientId, String redirectUri) {
    }

    static final class ClientRegistry {
        private final Map<String, Client> clients = Map.of(
                "demo-client", new Client("demo-client", "http://localhost:8080/callback")
        );

        boolean isValidRedirect(String clientId, String redirectUri) {
            Client client = clients.get(clientId);
            return client != null && client.redirectUri().equals(redirectUri);
        }
    }

    record AuthorizationCode(
            String value,
            String clientId,
            String redirectUri,
            String subject,
            String scope,
            String codeChallenge,
            String codeChallengeMethod,
            Instant expiresAt
    ) {
        boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }

    static final class AuthorizationCodeService {
        private final SecureRandom secureRandom = new SecureRandom();
        private final Map<String, AuthorizationCode> codes = new ConcurrentHashMap<>();

        AuthorizationCode createCode(String clientId,
                                     String redirectUri,
                                     String subject,
                                     String scope,
                                     String codeChallenge,
                                     String codeChallengeMethod) {
            String value = randomToken(secureRandom, 32);
            AuthorizationCode authorizationCode = new AuthorizationCode(
                    value,
                    clientId,
                    redirectUri,
                    subject,
                    scope,
                    codeChallenge,
                    codeChallengeMethod,
                    Instant.now().plus(Duration.ofMinutes(5))
            );
            codes.put(value, authorizationCode);
            return authorizationCode;
        }

        Optional<AuthorizationCode> consumeCode(String codeValue) {
            AuthorizationCode code = codes.remove(codeValue);
            if (code == null || code.isExpired()) {
                return Optional.empty();
            }
            return Optional.of(code);
        }
    }

    record AccessToken(String value, String subject, String scope, Instant expiresAt) {
        long expiresInSeconds() {
            return Math.max(0, Duration.between(Instant.now(), expiresAt).getSeconds());
        }

        boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }

    static final class TokenService {
        private final SecureRandom secureRandom = new SecureRandom();
        private final Map<String, AccessToken> tokens = new ConcurrentHashMap<>();

        AccessToken issueToken(String subject, String scope, Duration ttl) {
            String tokenValue = randomToken(secureRandom, 48);
            AccessToken token = new AccessToken(tokenValue, subject, scope, Instant.now().plus(ttl));
            tokens.put(tokenValue, token);
            return token;
        }

        Optional<AccessToken> findValidToken(String tokenValue) {
            AccessToken token = tokens.get(tokenValue);
            if (token == null || token.isExpired()) {
                tokens.remove(tokenValue);
                return Optional.empty();
            }
            return Optional.of(token);
        }
    }

    static final class PkceVerifier {
        private PkceVerifier() {
        }

        static boolean matches(String challenge, String method, String verifier) {
            if (challenge == null || challenge.isBlank()) {
                return true;
            }
            if (verifier == null || verifier.isBlank()) {
                return false;
            }
            if ("S256".equalsIgnoreCase(method)) {
                return challenge.equals(base64UrlSha256(verifier));
            }
            return challenge.equals(verifier);
        }

        private static String base64UrlSha256(String value) {
            try {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
                return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
            } catch (NoSuchAlgorithmException e) {
                throw new IllegalStateException("SHA-256 algorithm unavailable", e);
            }
        }
    }

    static Map<String, String> parseForm(String form) {
        Map<String, String> result = new LinkedHashMap<>();
        if (form == null || form.isBlank()) {
            return result;
        }
        String[] pairs = form.split("&");
        for (String pair : pairs) {
            if (pair.isBlank()) {
                continue;
            }
            String[] parts = pair.split("=", 2);
            String key = urlDecode(parts[0]);
            String value = parts.length > 1 ? urlDecode(parts[1]) : "";
            result.put(key, value);
        }
        return result;
    }

    static void redirect(HttpExchange exchange, String location) throws IOException {
        Headers headers = exchange.getResponseHeaders();
        headers.set("Location", location);
        exchange.sendResponseHeaders(302, -1);
        exchange.close();
    }

    static void writeJson(HttpExchange exchange, int statusCode, String json) throws IOException {
        writeResponse(exchange, statusCode, "application/json; charset=UTF-8", json);
    }

    static void writeResponse(HttpExchange exchange, int statusCode, String contentType, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(bytes);
        }
    }

    static String randomToken(SecureRandom secureRandom, int byteLength) {
        byte[] bytes = new byte[byteLength];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    static String urlDecode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    static String escapeHtml(String input) {
        Map<String, String> escapes = new HashMap<>();
        escapes.put("&", "&amp;");
        escapes.put("<", "&lt;");
        escapes.put(">", "&gt;");
        escapes.put("\"", "&quot;");
        escapes.put("'", "&#39;");
        String escaped = input;
        for (Map.Entry<String, String> entry : escapes.entrySet()) {
            escaped = escaped.replace(entry.getKey(), entry.getValue());
        }
        return escaped;
    }
}
