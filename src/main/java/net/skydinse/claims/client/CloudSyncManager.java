package net.skydinse.claims.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.skydinse.claims.client.map.ClaimedChunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CloudSyncManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("skydinseclaims");
    private static final long POLL_INTERVAL_NANOS = TimeUnit.MINUTES.toNanos(5);
    private static final long PUSH_RETRY_NANOS = TimeUnit.SECONDS.toNanos(30);
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10)).build();
    private final Queue<List<ClaimedChunk>> pendingPulls = new ConcurrentLinkedQueue<>();
    private CloudSyncConfig config;
    private String serverAddress;
    private long nextPollNanos;
    private volatile boolean pullInFlight;
    private volatile boolean pushInFlight;
    private volatile List<ClaimedChunk> pendingPush;
    private volatile long nextPushRetryNanos;

    public CloudSyncManager(CloudSyncConfig config) {
        this.config = config;
    }

    public void start(String serverAddress, Iterable<ClaimedChunk> localClaims) {
        stop();
        this.serverAddress = serverAddress;
        if (!config.enabled()) return;
        nextPollNanos = System.nanoTime() + POLL_INTERVAL_NANOS;
        push(serverAddress, localClaims);
        pull(serverAddress);
    }

    public void stop() {
        serverAddress = null;
        pendingPulls.clear();
        pendingPush = null;
        nextPushRetryNanos = 0;
        pullInFlight = false;
        pushInFlight = false;
    }

    public void refreshConfig(CloudSyncConfig config) {
        this.config = config;
    }

    public void tick() {
        if (serverAddress == null || !config.enabled()) return;
        if (!pushInFlight && pendingPush != null && System.nanoTime() >= nextPushRetryNanos) {
            List<ClaimedChunk> retry = pendingPush;
            pendingPush = null;
            sendPush(serverAddress, retry);
        }
        if (pullInFlight) return;
        if (System.nanoTime() >= nextPollNanos) {
            nextPollNanos = System.nanoTime() + POLL_INTERVAL_NANOS;
            pull(serverAddress);
        }
    }

    public void push(String serverAddress, Iterable<ClaimedChunk> claims) {
        if (!config.enabled() || !serverAddress.equals(this.serverAddress)) return;
        List<ClaimedChunk> snapshot = new ArrayList<>();
        claims.forEach(snapshot::add);
        if (snapshot.isEmpty()) return;
        if (pushInFlight) {
            pendingPush = snapshot;
            return;
        }
        pendingPush = null;
        sendPush(serverAddress, snapshot);
    }

    private void sendPush(String serverAddress, List<ClaimedChunk> snapshot) {
        JsonObject body = new JsonObject();
        body.addProperty("server", serverAddress);
        JsonArray values = new JsonArray();
        for (ClaimedChunk claim : snapshot) {
            JsonObject value = new JsonObject();
            value.addProperty("dimension", dimensionName(claim.dimension()));
            value.addProperty("x", claim.chunkX());
            value.addProperty("z", claim.chunkZ());
            value.addProperty("owner", claim.owner());
            values.add(value);
        }
        body.add("chunks", values);
        pushInFlight = true;
        request("POST", endpoint("/v1/chunks"), body.toString())
                .whenComplete((response, error) -> {
                    pushInFlight = false;
                    boolean success = error == null && response.statusCode() / 100 == 2;
                    if (!success) {
                        if (error != null) LOGGER.warn("Cloud claims push failed: {}", error.toString());
                        else LOGGER.warn("Cloud claims push returned HTTP {}", response.statusCode());
                        if (pendingPush == null) pendingPush = snapshot;
                        nextPushRetryNanos = System.nanoTime() + PUSH_RETRY_NANOS;
                        return;
                    }
                    List<ClaimedChunk> retry = pendingPush;
                    pendingPush = null;
                    if (retry != null && serverAddress.equals(this.serverAddress)) sendPush(serverAddress, retry);
                });
    }

    public List<ClaimedChunk> drainPulled() {
        List<ClaimedChunk> result = new ArrayList<>();
        List<ClaimedChunk> batch;
        while ((batch = pendingPulls.poll()) != null) result.addAll(batch);
        return result;
    }

    private void pull(String serverAddress) {
        if (!config.enabled() || pullInFlight) return;
        pullInFlight = true;
        String query = URLEncoder.encode(serverAddress, StandardCharsets.UTF_8);
        request("GET", endpoint("/v1/chunks?server=" + query), null)
                .whenComplete((response, error) -> {
                    pullInFlight = false;
                    if (error != null) {
                        LOGGER.warn("Cloud claims pull failed: {}", error.toString());
                    } else if (response.statusCode() / 100 == 2) {
                        pendingPulls.add(parseChunks(response.body()));
                    } else {
                        LOGGER.warn("Cloud claims pull returned HTTP {}", response.statusCode());
                    }
                });
    }

    private CompletableFuture<HttpResponse<String>> request(String method, String uri, String body) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(uri))
                .timeout(Duration.ofSeconds(20))
                .header("Authorization", "Bearer " + config.token())
                .header("Accept", "application/json");
        if (body == null) builder.GET();
        else builder.method(method, HttpRequest.BodyPublishers.ofString(body))
                .header("Content-Type", "application/json");
        return http.sendAsync(builder.build(), HttpResponse.BodyHandlers.ofString())
                .orTimeout(25, TimeUnit.SECONDS);
    }

    private String endpoint(String path) {
        return config.endpoint() + path;
    }

    private static List<ClaimedChunk> parseChunks(String body) {
        List<ClaimedChunk> chunks = new ArrayList<>();
        try {
            JsonObject root = JsonParser.parseString(body).getAsJsonObject();
            JsonArray values = root.getAsJsonArray("chunks");
            if (values == null) return chunks;
            for (JsonElement element : values) {
                JsonObject value = element.getAsJsonObject();
                ResourceKey<Level> dimension = switch (value.get("dimension").getAsString()) {
                    case "minecraft:the_nether" -> Level.NETHER;
                    case "minecraft:the_end" -> Level.END;
                    default -> Level.OVERWORLD;
                };
                chunks.add(new ClaimedChunk(dimension, value.get("x").getAsInt(),
                        value.get("z").getAsInt(), value.get("owner").getAsString()));
            }
        } catch (Exception ignored) {
            // Malformed remote data is discarded without touching the local claims.
        }
        return chunks;
    }

    private static String dimensionName(ResourceKey<Level> dimension) {
        if (dimension.equals(Level.NETHER)) return "minecraft:the_nether";
        if (dimension.equals(Level.END)) return "minecraft:the_end";
        return "minecraft:overworld";
    }
}
