package com.example.oauth.controller;

import com.example.oauth.model.AccessToken;
import com.example.oauth.model.AuthorizationCode;
import com.example.oauth.service.AuthorizationCodeService;
import com.example.oauth.service.ClientRegistryService;
import com.example.oauth.service.PkceVerifier;
import com.example.oauth.service.TokenService;
import com.example.oauth.util.FormParser;
import com.example.oauth.util.HttpResponseWriter;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class TokenController implements HttpHandler {
    private final ClientRegistryService clientRegistryService;
    private final AuthorizationCodeService authorizationCodeService;
    private final TokenService tokenService;

    public TokenController(ClientRegistryService clientRegistryService,
                           AuthorizationCodeService authorizationCodeService,
                           TokenService tokenService) {
        this.clientRegistryService = clientRegistryService;
        this.authorizationCodeService = authorizationCodeService;
        this.tokenService = tokenService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            HttpResponseWriter.writeJson(exchange, 405, "{\"error\":\"method_not_allowed\"}");
            return;
        }

        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        Map<String, String> form = FormParser.parse(body);
        String grantType = form.get("grant_type");
        String code = form.get("code");
        String clientId = form.get("client_id");
        String redirectUri = form.get("redirect_uri");
        String codeVerifier = form.getOrDefault("code_verifier", "");

        if (!"authorization_code".equals(grantType)) {
            HttpResponseWriter.writeJson(exchange, 400, "{\"error\":\"unsupported_grant_type\"}");
            return;
        }
        if (!clientRegistryService.isValidRedirect(clientId, redirectUri)) {
            HttpResponseWriter.writeJson(exchange, 400, "{\"error\":\"invalid_client\"}");
            return;
        }

        Optional<AuthorizationCode> authorizationCode = authorizationCodeService.consumeCode(code);
        if (authorizationCode.isEmpty()) {
            HttpResponseWriter.writeJson(exchange, 400, "{\"error\":\"invalid_grant\"}");
            return;
        }

        AuthorizationCode storedCode = authorizationCode.get();
        if (!Objects.equals(storedCode.clientId(), clientId) || !Objects.equals(storedCode.redirectUri(), redirectUri)) {
            HttpResponseWriter.writeJson(exchange, 400, "{\"error\":\"invalid_grant\"}");
            return;
        }
        if (!PkceVerifier.matches(storedCode.codeChallenge(), storedCode.codeChallengeMethod(), codeVerifier)) {
            HttpResponseWriter.writeJson(exchange, 400, "{\"error\":\"invalid_code_verifier\"}");
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
        HttpResponseWriter.writeJson(exchange, 200, json);
    }
}
