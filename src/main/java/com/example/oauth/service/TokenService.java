package com.example.oauth.service;

import com.example.oauth.model.AccessToken;
import com.example.oauth.util.TokenGenerator;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class TokenService {
    private final SecureRandom secureRandom = new SecureRandom();
    private final Map<String, AccessToken> tokens = new ConcurrentHashMap<>();

    public AccessToken issueToken(String subject, String scope, Duration ttl) {
        String tokenValue = TokenGenerator.randomToken(secureRandom, 48);
        AccessToken token = new AccessToken(tokenValue, subject, scope, Instant.now().plus(ttl));
        tokens.put(tokenValue, token);
        return token;
    }

    public Optional<AccessToken> findValidToken(String tokenValue) {
        AccessToken token = tokens.get(tokenValue);
        if (token == null || token.isExpired()) {
            tokens.remove(tokenValue);
            return Optional.empty();
        }
        return Optional.of(token);
    }
}
