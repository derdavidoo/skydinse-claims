package net.skydinse.claims.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.nio.file.Files;
import java.util.List;
import org.junit.jupiter.api.Test;

final class ClaimsRepositoryTest {
    @Test
    void upsertChangesOwnerAndRetainsOmittedClaims() throws Exception {
        var file = Files.createTempFile("skydinse-claims", ".db");
        try {
            try (var repository = new AutoCloseRepository(file)) {
                repository.value.upsert(List.of(
                        new ClaimsRepository.Claim("play.example", "minecraft:overworld", 1, 2, "Alice"),
                        new ClaimsRepository.Claim("play.example", "minecraft:overworld", 3, 4, "Alice")));
                repository.value.upsert(List.of(
                        new ClaimsRepository.Claim("play.example", "minecraft:overworld", 1, 2, "Bob")));
                var claims = repository.value.find("play.example");
                assertEquals(2, claims.size());
                assertTrue(claims.stream().anyMatch(claim -> claim.x() == 1 && claim.owner().equals("Bob")));
                assertTrue(claims.stream().anyMatch(claim -> claim.x() == 3 && claim.owner().equals("Alice")));
            }
        } finally {
            Files.deleteIfExists(file);
        }
    }

    private static final class AutoCloseRepository implements AutoCloseable {
        private final ClaimsRepository value;

        private AutoCloseRepository(java.nio.file.Path path) throws Exception {
            value = new ClaimsRepository(path);
        }

        @Override
        public void close() { value.close(); }
    }
}
