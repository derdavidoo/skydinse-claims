package net.skydinse.claims.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.InputStreamReader;
import net.minecraft.client.Minecraft;

public final class CloudSyncConfig {
    public static final String DEFAULT_ENDPOINT = "https://130.61.216.244";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final CloudSyncConfig DEFAULTS = loadDefaults();
    private final String endpoint;
    private final String token;

    private CloudSyncConfig(String endpoint, String token) {
        this.endpoint = endpoint == null ? "" : endpoint.trim().replaceAll("/+$", "");
        this.token = token == null ? "" : token.trim();
    }

    public static CloudSyncConfig load(Minecraft client) {
        Path file = client.gameDirectory.toPath().resolve("config").resolve("skydinseclaims-cloud.json");
        if (!Files.isRegularFile(file)) {
            DEFAULTS.write(file);
            return DEFAULTS;
        }
        try {
            JsonObject object = GSON.fromJson(Files.readString(file, StandardCharsets.UTF_8), JsonObject.class);
            String token = object != null && object.has("token") ? object.get("token").getAsString() : DEFAULTS.token;
            if (token == null || token.isBlank()) token = DEFAULTS.token;
            return new CloudSyncConfig(
                    object != null && object.has("endpoint") ? object.get("endpoint").getAsString() : DEFAULTS.endpoint,
                    token);
        } catch (Exception ignored) {
            return DEFAULTS;
        }
    }

    private static CloudSyncConfig loadDefaults() {
        try (var input = CloudSyncConfig.class.getResourceAsStream("/skydinseclaims-cloud-defaults.json")) {
            if (input != null) {
                JsonObject object = GSON.fromJson(new InputStreamReader(input, StandardCharsets.UTF_8), JsonObject.class);
                if (object != null) {
                    return new CloudSyncConfig(
                            object.has("endpoint") ? object.get("endpoint").getAsString() : DEFAULT_ENDPOINT,
                            object.has("token") ? object.get("token").getAsString() : "");
                }
            }
        } catch (Exception ignored) {
            // Missing build-time defaults leave cloud sync configurable by the user.
        }
        return new CloudSyncConfig(DEFAULT_ENDPOINT, "");
    }

    public boolean enabled() {
        return !endpoint.isBlank() && !token.isBlank();
    }

    public String endpoint() {
        return endpoint;
    }

    public String token() {
        return token;
    }

    private void write(Path file) {
        JsonObject object = new JsonObject();
        object.addProperty("endpoint", endpoint);
        object.addProperty("token", token);
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, GSON.toJson(object), StandardCharsets.UTF_8);
        } catch (IOException ignored) {
            // Configuration is optional; a failed default write must not stop the client.
        }
    }
}
