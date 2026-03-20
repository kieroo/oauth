package com.example.oauth.service;

import com.example.oauth.model.AuthorizationCode;
import com.example.oauth.util.TokenGenerator;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class AuthorizationCodeService {
    private final SecureRandom secureRandom = new SecureRandom();
    private final Map<String, AuthorizationCode> codes = new ConcurrentHashMap<>();

    public AuthorizationCode createCode(String clientId,
                                        String redirectUri,
                                        String subject,
                                        String scope,
                                        String codeChallenge,
                                        String codeChallengeMethod) {
        String value = TokenGenerator.randomToken(secureRandom, 32);
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

    public Optional<AuthorizationCode> consumeCode(String codeValue) {
        AuthorizationCode code = codes.remove(codeValue);
        if (code == null || code.isExpired()) {
            return Optional.empty();
        }
        return Optional.of(code);
    }
}
