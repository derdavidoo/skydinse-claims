package net.skydinse.claims.server;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class ClaimsHttpApi {
    private static final int MAX_BODY_BYTES = 2_000_000;
    private final ClaimsRepository repository;
    private final TokenAuthenticator authenticator;
    private final Set<String> allowedServers;
    private final Gson gson = new Gson();

    public ClaimsHttpApi(ClaimsRepository repository, TokenAuthenticator authenticator) {
        this(repository, authenticator, Set.of());
    }

    public ClaimsHttpApi(ClaimsRepository repository, TokenAuthenticator authenticator, Set<String> allowedServers) {
        this.repository = repository;
        this.authenticator = authenticator;
        this.allowedServers = Set.copyOf(allowedServers);
    }

    public void health(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equals("GET")) { ClaimsServer.sendError(exchange, 405, "method not allowed"); return; }
        ClaimsServer.send(exchange, 200, "{\"status\":\"ok\"}");
    }

    public void chunks(HttpExchange exchange) throws IOException {
        String player = authenticator.authenticate(exchange.getRequestHeaders().getFirst("Authorization"));
        if (player == null) { ClaimsServer.sendError(exchange, 401, "unauthorized"); return; }
        try {
            if (exchange.getRequestMethod().equals("GET")) get(exchange);
            else if (exchange.getRequestMethod().equals("POST")) post(exchange);
            else ClaimsServer.sendError(exchange, 405, "method not allowed");
        } catch (IllegalArgumentException exception) {
            ClaimsServer.sendError(exchange, 400, exception.getMessage());
        } catch (com.google.gson.JsonParseException | IllegalStateException exception) {
            ClaimsServer.sendError(exchange, 400, "invalid JSON");
        } catch (Exception exception) {
            ClaimsServer.sendError(exchange, 500, "storage error");
        }
    }

    private void get(HttpExchange exchange) throws Exception {
        String server = query(exchange, "server");
        requireAllowedServer(server);
        List<ClaimsRepository.Claim> claims = repository.find(server);
        JsonArray values = new JsonArray();
        for (ClaimsRepository.Claim claim : claims) values.add(toJson(claim));
        JsonObject result = new JsonObject();
        result.add("chunks", values);
        ClaimsServer.send(exchange, 200, gson.toJson(result));
    }

    private void post(HttpExchange exchange) throws Exception {
        byte[] bytes = exchange.getRequestBody().readNBytes(MAX_BODY_BYTES + 1);
        if (bytes.length > MAX_BODY_BYTES) throw new IllegalArgumentException("request too large");
        JsonObject root = JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8)).getAsJsonObject();
        String server = required(root, "server", 255);
        requireAllowedServer(server);
        JsonArray values = root.getAsJsonArray("chunks");
        if (values == null || values.size() > 10_000) throw new IllegalArgumentException("chunks must be an array of at most 10000 entries");
        List<ClaimsRepository.Claim> claims = new ArrayList<>();
        for (var element : values) {
            JsonObject value = element.getAsJsonObject();
            String dimension = required(value, "dimension", 255);
            int x = integer(value, "x");
            int z = integer(value, "z");
            String owner = required(value, "owner", 64);
            claims.add(new ClaimsRepository.Claim(server, dimension, x, z, owner));
        }
        repository.upsert(claims);
        ClaimsServer.send(exchange, 200, "{\"accepted\":" + claims.size() + "}");
    }

    private void requireAllowedServer(String server) {
        if (!allowedServers.isEmpty() && !allowedServers.contains(server)) {
            throw new ForbiddenServerException();
        }
    }

    private static JsonObject toJson(ClaimsRepository.Claim claim) {
        JsonObject value = new JsonObject();
        value.addProperty("dimension", claim.dimension());
        value.addProperty("x", claim.x());
        value.addProperty("z", claim.z());
        value.addProperty("owner", claim.owner());
        return value;
    }

    private static String query(HttpExchange exchange, String name) {
        String raw = exchange.getRequestURI().getRawQuery();
        if (raw != null) for (String item : raw.split("&")) {
            String[] pair = item.split("=", 2);
            if (pair.length == 2 && pair[0].equals(name)) return java.net.URLDecoder.decode(pair[1], StandardCharsets.UTF_8);
        }
        throw new IllegalArgumentException("missing " + name);
    }

    private static String required(JsonObject object, String key, int max) {
        if (!object.has(key) || !object.get(key).isJsonPrimitive()) throw new IllegalArgumentException("missing " + key);
        String value = object.get(key).getAsString().trim();
        if (value.isBlank() || value.length() > max) throw new IllegalArgumentException("invalid " + key);
        return value;
    }

    private static int integer(JsonObject object, String key) {
        if (!object.has(key) || !object.get(key).isJsonPrimitive()) throw new IllegalArgumentException("missing " + key);
        try { return object.get(key).getAsInt(); } catch (Exception exception) { throw new IllegalArgumentException("invalid " + key); }
    }

    private static final class ForbiddenServerException extends IllegalArgumentException {
        private ForbiddenServerException() { super("server dataset is not enabled"); }
    }
}
