package net.skydinse.claims.server;

public final class JsonUtil {
    private JsonUtil() { }

    public static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\r", "\\r").replace("\n", "\\n");
    }
}
