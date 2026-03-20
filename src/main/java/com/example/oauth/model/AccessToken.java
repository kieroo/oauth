package com.example.oauth.model;

import java.time.Duration;
import java.time.Instant;

public record AccessToken(String value, String subject, String scope, Instant expiresAt) {
    public long expiresInSeconds() {
        return Math.max(0, Duration.between(Instant.now(), expiresAt).getSeconds());
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }
}
