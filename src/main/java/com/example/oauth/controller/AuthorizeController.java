package com.example.oauth.controller;

import com.example.oauth.model.AuthorizationCode;
import com.example.oauth.service.AuthorizationCodeService;
import com.example.oauth.service.ClientRegistryService;
import com.example.oauth.util.FormParser;
import com.example.oauth.util.HttpResponseWriter;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.Map;

public class AuthorizeController implements HttpHandler {
    private final ClientRegistryService clientRegistryService;
    private final AuthorizationCodeService authorizationCodeService;

    public AuthorizeController(ClientRegistryService clientRegistryService,
                               AuthorizationCodeService authorizationCodeService) {
        this.clientRegistryService = clientRegistryService;
        this.authorizationCodeService = authorizationCodeService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            HttpResponseWriter.writeJson(exchange, 405, "{\"error\":\"method_not_allowed\"}");
            return;
        }

        Map<String, String> query = FormParser.parse(exchange.getRequestURI().getRawQuery());
        String responseType = query.get("response_type");
        String clientId = query.get("client_id");
        String redirectUri = query.get("redirect_uri");
        String state = query.getOrDefault("state", "");
        String scope = query.getOrDefault("scope", "read");
        String codeChallenge = query.getOrDefault("code_challenge", "");
        String challengeMethod = query.getOrDefault("code_challenge_method", "plain");

        if (!"code".equals(responseType)) {
            HttpResponseWriter.writeJson(exchange, 400, "{\"error\":\"unsupported_response_type\"}");
            return;
        }
        if (!clientRegistryService.isValidRedirect(clientId, redirectUri)) {
            HttpResponseWriter.writeJson(exchange, 400, "{\"error\":\"invalid_client_or_redirect_uri\"}");
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
                + "code=" + FormParser.urlEncode(authorizationCode.value())
                + "&state=" + FormParser.urlEncode(state);
        HttpResponseWriter.redirect(exchange, location);
    }
}
