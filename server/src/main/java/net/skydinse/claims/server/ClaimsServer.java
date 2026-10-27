package net.skydinse.claims.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.Executors;

public final class ClaimsServer {
    private ClaimsServer() { }

    public static void main(String[] args) throws Exception {
        int port = Integer.parseInt(System.getenv().getOrDefault("SKYDINSE_CLAIMS_PORT", "8080"));
        String bindAddress = System.getenv().getOrDefault("SKYDINSE_CLAIMS_BIND", "127.0.0.1");
        Path database = Path.of(System.getenv().getOrDefault("SKYDINSE_CLAIMS_DB", "./data/claims.db"));
        Files.createDirectories(database.toAbsolutePath().getParent());
        TokenAuthenticator authenticator = TokenAuthenticator.fromEnvironment();
        if (authenticator.isEmpty()) throw new IllegalStateException("SKYDINSE_CLAIMS_TOKENS is required");

        ClaimsRepository repository = new ClaimsRepository(database);
        ClaimsHttpApi api = new ClaimsHttpApi(repository, authenticator);
        HttpServer server = HttpServer.create(new InetSocketAddress(bindAddress, port), 0);
        server.createContext("/health", api::health);
        server.createContext("/v1/chunks", api::chunks);
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            server.stop(0);
            repository.close();
        }));
        server.start();
        System.out.println("Skydinse Claims cloud server listening on " + port);
    }

    static void send(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.sendResponseHeaders(status, bytes.length);
        try (var output = exchange.getResponseBody()) { output.write(bytes); }
    }

    static void sendError(HttpExchange exchange, int status, String message) throws IOException {
        send(exchange, status, "{\"error\":\"" + JsonUtil.escape(message) + "\"}");
    }
}
