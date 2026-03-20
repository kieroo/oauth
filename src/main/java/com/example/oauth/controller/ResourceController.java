package com.example.oauth.controller;

import com.example.oauth.model.AccessToken;
import com.example.oauth.service.TokenService;
import com.example.oauth.util.HttpResponseWriter;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.Optional;

public class ResourceController implements HttpHandler {
    private final TokenService tokenService;

    public ResourceController(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            HttpResponseWriter.writeJson(exchange, 405, "{\"error\":\"method_not_allowed\"}");
            return;
        }

        String authorization = exchange.getRequestHeaders().getFirst("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            HttpResponseWriter.writeJson(exchange, 401, "{\"error\":\"missing_bearer_token\"}");
            return;
        }

        String tokenValue = authorization.substring("Bearer ".length());
        Optional<AccessToken> token = tokenService.findValidToken(tokenValue);
        if (token.isEmpty()) {
            HttpResponseWriter.writeJson(exchange, 401, "{\"error\":\"invalid_or_expired_token\"}");
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
        HttpResponseWriter.writeJson(exchange, 200, json);
    }
}
