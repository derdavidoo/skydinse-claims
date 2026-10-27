package net.skydinse.claims.server;

import java.util.HashMap;
import java.util.Map;

public final class TokenAuthenticator {
    private final Map<String, String> tokens;

    private TokenAuthenticator(Map<String, String> tokens) {
        this.tokens = Map.copyOf(tokens);
    }

    public static TokenAuthenticator fromEnvironment() {
        Map<String, String> tokens = new HashMap<>();
        String value = System.getenv().getOrDefault("SKYDINSE_CLAIMS_TOKENS", "");
        for (String entry : value.split(",")) {
            int separator = entry.indexOf(':');
            if (separator > 0 && separator < entry.length() - 1) {
                tokens.put(entry.substring(0, separator).trim(), entry.substring(separator + 1).trim());
            }
        }
        return new TokenAuthenticator(tokens);
    }

    public String authenticate(String header) {
        if (header == null || !header.startsWith("Bearer ")) return null;
        String token = header.substring("Bearer ".length()).trim();
        for (var entry : tokens.entrySet()) {
            if (java.security.MessageDigest.isEqual(entry.getValue().getBytes(java.nio.charset.StandardCharsets.UTF_8),
                    token.getBytes(java.nio.charset.StandardCharsets.UTF_8))) return entry.getKey();
        }
        return null;
    }

    public boolean isEmpty() { return tokens.isEmpty(); }
}
