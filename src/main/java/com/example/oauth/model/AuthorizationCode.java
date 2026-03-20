package com.example.oauth.model;

import java.time.Instant;

public record AuthorizationCode(
        String value,
        String clientId,
        String redirectUri,
        String subject,
        String scope,
        String codeChallenge,
        String codeChallengeMethod,
        Instant expiresAt
) {
    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }
}
