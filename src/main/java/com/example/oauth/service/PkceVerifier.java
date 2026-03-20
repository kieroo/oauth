package com.example.oauth.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public final class PkceVerifier {
    private PkceVerifier() {
    }

    public static boolean matches(String challenge, String method, String verifier) {
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
