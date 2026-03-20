package com.example.oauth;

import com.example.oauth.model.AccessToken;
import com.example.oauth.model.AuthorizationCode;
import com.example.oauth.service.AuthorizationCodeService;
import com.example.oauth.service.PkceVerifier;
import com.example.oauth.service.TokenService;
import com.example.oauth.util.FormParser;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;

public class OAuthDemoServerSelfTest {
    public static void main(String[] args) throws Exception {
        parseFormShouldDecodeUrlEncodedPairs();
        authorizationCodeCanOnlyBeConsumedOnce();
        tokenServiceShouldRejectExpiredToken();
        pkceVerifierSupportsPlainAndS256();
        System.out.println("All OAuth demo self-tests passed.");
    }

    private static void parseFormShouldDecodeUrlEncodedPairs() {
        Map<String, String> form = FormParser.parse("client_id=demo-client&scope=read%20write");
        assertEquals("demo-client", form.get("client_id"), "client_id should be decoded");
        assertEquals("read write", form.get("scope"), "scope should be URL-decoded");
    }

    private static void authorizationCodeCanOnlyBeConsumedOnce() {
        AuthorizationCodeService service = new AuthorizationCodeService();
        AuthorizationCode code = service.createCode(
                "demo-client",
                "http://localhost:8080/callback",
                "demo-user",
                "read",
                "demo-verifier",
                "plain"
        );

        Optional<AuthorizationCode> firstUse = service.consumeCode(code.value());
        Optional<AuthorizationCode> secondUse = service.consumeCode(code.value());

        assertTrue(firstUse.isPresent(), "authorization code should be consumable the first time");
        assertTrue(secondUse.isEmpty(), "authorization code should not be reusable");
    }

    private static void tokenServiceShouldRejectExpiredToken() throws InterruptedException {
        TokenService tokenService = new TokenService();
        AccessToken token = tokenService.issueToken("demo-user", "read", Duration.ofMillis(5));
        Thread.sleep(20);
        assertTrue(tokenService.findValidToken(token.value()).isEmpty(), "expired token should be rejected");
    }

    private static void pkceVerifierSupportsPlainAndS256() throws NoSuchAlgorithmException {
        assertTrue(PkceVerifier.matches("demo-verifier", "plain", "demo-verifier"), "plain PKCE should match");
        assertFalse(PkceVerifier.matches("demo-verifier", "plain", "wrong"), "plain PKCE should reject mismatched verifier");

        String verifier = "another-verifier";
        String s256 = Base64.getUrlEncoder().withoutPadding().encodeToString(
                MessageDigest.getInstance("SHA-256").digest(verifier.getBytes(StandardCharsets.UTF_8))
        );
        assertTrue(PkceVerifier.matches(s256, "S256", verifier), "S256 PKCE should match");
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void assertFalse(boolean condition, String message) {
        assertTrue(!condition, message);
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if ((expected == null && actual != null) || (expected != null && !expected.equals(actual))) {
            throw new AssertionError(message + ", expected=" + expected + ", actual=" + actual);
        }
    }
}
