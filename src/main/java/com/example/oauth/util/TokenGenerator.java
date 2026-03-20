package com.example.oauth.util;

import java.security.SecureRandom;
import java.util.Base64;

public final class TokenGenerator {
    private TokenGenerator() {
    }

    public static String randomToken(SecureRandom secureRandom, int byteLength) {
        byte[] bytes = new byte[byteLength];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
