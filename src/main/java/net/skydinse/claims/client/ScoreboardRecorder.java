package net.skydinse.claims.client;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.Minecraft;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.Scoreboard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ScoreboardRecorder {
    private static final Logger LOGGER = LoggerFactory.getLogger("skydinseclaims");
    private static final int INTERVAL_TICKS = 200;
    private static final Pattern CLAIMS_LINE = Pattern.compile(".*\\bclaims?:\\s*(.+?)\\s*", Pattern.CASE_INSENSITIVE);
    private int ticks;

    public String findClaimsOwner(Minecraft client) {
        if (client.level == null) {
            return null;
        }
        Scoreboard scoreboard = client.level.getScoreboard();
        for (Objective objective : scoreboard.getObjectives()) {
            for (PlayerScoreEntry entry : scoreboard.listPlayerScores(objective)) {
                String display = entry.display() == null ? "" : stripFormatting(entry.display().getString());
                Matcher matcher = CLAIMS_LINE.matcher(display);
                if (matcher.matches()) {
                    String owner = matcher.group(1).trim();
                    return owner.isBlank() ? null : owner;
                }
            }
        }
        return null;
    }

    private static String stripFormatting(String text) {
        return text.replaceAll("\\u00a7[0-9a-fk-orA-FK-OR]", "").trim();
    }

    public void tick(Minecraft client) {
        if (!SkydinseServerAccess.active() || client.level == null || ++ticks < INTERVAL_TICKS) {
            return;
        }
        ticks = 0;
        record(client);
    }

    public void reset() {
        ticks = 0;
    }

    private void record(Minecraft client) {
        Scoreboard scoreboard = client.level.getScoreboard();
        StringBuilder output = new StringBuilder();
        output.append("\\n--- ").append(Instant.now()).append(" ---\\n");
        output.append("dimension=").append(client.level.dimension().identifier()).append('\n');
        if (client.player != null) {
            output.append("position=").append(client.player.blockPosition()).append('\n');
            output.append("chunk=").append(client.player.chunkPosition()).append('\n');
            output.append("claimsOwner=").append(findClaimsOwner(client)).append('\n');
        }
        output.append("sidebar=").append(displayName(scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR))).append('\n');
        for (Objective objective : scoreboard.getObjectives()) {
            output.append("objective=").append(objective.getName()).append(" display=")
                    .append(objective.getDisplayName().getString()).append('\n');
            for (PlayerScoreEntry entry : scoreboard.listPlayerScores(objective)) {
                output.append("  player=").append(entry.owner()).append(" value=").append(entry.value())
                        .append(" display=").append(entry.display() == null ? "" : entry.display().getString())
                        .append('\n');
            }
        }
        try {
            Path file = client.gameDirectory.toPath().resolve("skydinseclaims-scoreboard.log");
            Files.writeString(file, output, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            LOGGER.info("Recorded scoreboard snapshot to {}", file);
        } catch (IOException exception) {
            LOGGER.warn("Could not write scoreboard snapshot", exception);
        }
    }

    private static String displayName(Objective objective) {
        return objective == null ? "" : objective.getDisplayName().getString();
    }

    private static final class SkydinseServerAccess {
        private static boolean active() {
            return net.skydinse.claims.client.map.SkydinseServer.isActive();
        }
    }
}
