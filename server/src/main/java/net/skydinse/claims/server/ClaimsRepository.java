package net.skydinse.claims.server;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class ClaimsRepository {
    public record Claim(String server, String dimension, int x, int z, String owner) { }
    private final Connection connection;

    public ClaimsRepository(Path database) throws SQLException {
        connection = DriverManager.getConnection("jdbc:sqlite:" + database.toAbsolutePath());
        try (var statement = connection.createStatement()) {
            statement.executeUpdate("PRAGMA journal_mode=WAL");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS claims (server TEXT NOT NULL, dimension TEXT NOT NULL, x INTEGER NOT NULL, z INTEGER NOT NULL, owner TEXT NOT NULL, updated_at INTEGER NOT NULL, PRIMARY KEY(server, dimension, x, z))");
            statement.executeUpdate("CREATE INDEX IF NOT EXISTS claims_server ON claims(server)");
        }
    }

    public synchronized void upsert(List<Claim> claims) throws SQLException {
        try (var statement = connection.prepareStatement("INSERT INTO claims(server, dimension, x, z, owner, updated_at) VALUES(?,?,?,?,?,unixepoch()) ON CONFLICT(server, dimension, x, z) DO UPDATE SET owner=excluded.owner, updated_at=excluded.updated_at")) {
            for (Claim claim : claims) {
                statement.setString(1, claim.server());
                statement.setString(2, claim.dimension());
                statement.setInt(3, claim.x());
                statement.setInt(4, claim.z());
                statement.setString(5, claim.owner());
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    public synchronized List<Claim> find(String server) throws SQLException {
        List<Claim> result = new ArrayList<>();
        try (var statement = connection.prepareStatement("SELECT server, dimension, x, z, owner FROM claims WHERE server=? ORDER BY dimension, x, z")) {
            statement.setString(1, server);
            try (var rows = statement.executeQuery()) {
                while (rows.next()) result.add(new Claim(rows.getString(1), rows.getString(2), rows.getInt(3), rows.getInt(4), rows.getString(5)));
            }
        }
        return result;
    }

    public void close() {
        try { connection.close(); } catch (SQLException ignored) { }
    }
}
