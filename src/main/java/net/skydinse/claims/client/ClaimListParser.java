package net.skydinse.claims.client;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ClaimListParser {
    private static final Pattern HEADER = Pattern.compile(".*Claim.*Gesch\\u00fctzte Chunks von\\s+(.+?):\\s*");
    private static final Pattern ENTRY = Pattern.compile(
            ".*Claim.*#\\d+: Koordinaten: \\[x: -?\\d+, z: -?\\d+\\] "
            + "Welt: \\[([^\\]]+)\\] Chunk: \\[(-?\\d+),\\s*(-?\\d+)\\] Datum:.*");
    private static final int RESPONSE_TIMEOUT = 600;
    private static final int QUIET_TICKS = 40;
    private final Set<Entry> pending = new LinkedHashSet<>();
    private boolean armed;
    private boolean collecting;
    private int elapsed;
    private int quiet;
    private String owner;

    public record Entry(String dimension, int chunkX, int chunkZ, String owner) { }

    public void commandSent(String command) {
        if (command.trim().matches("(?i)/?claim\\s+list\\s*")) {
            reset();
            armed = true;
        }
    }

    public void accept(String text) {
        if (!armed) {
            return;
        }
        for (String line : text.replaceAll("\\u00a7[0-9a-fk-orA-FK-OR]", "").split("\\R")) {
            Matcher header = HEADER.matcher(line);
            if (header.matches()) {
                pending.clear();
                owner = header.group(1).trim();
                collecting = true;
                quiet = 0;
                continue;
            }
            if (!collecting) {
                continue;
            }
            Matcher matcher = ENTRY.matcher(line);
            if (!matcher.matches()) {
                if (line.contains("Claim") && line.contains("Koordinaten:")) {
                    reset();
                    return;
                }
                continue;
            }
            String dimension = switch (matcher.group(1).toLowerCase(Locale.ROOT)) {
                case "oberwelt", "overworld" -> "minecraft:overworld";
                case "nether" -> "minecraft:the_nether";
                case "end", "ende", "das end", "the end" -> "minecraft:the_end";
                default -> null;
            };
            try {
                if (dimension == null || owner == null || owner.isBlank()) {
                    reset();
                    return;
                }
                pending.add(new Entry(dimension, Integer.parseInt(matcher.group(2)),
                        Integer.parseInt(matcher.group(3)), owner));
                quiet = 0;
            } catch (NumberFormatException ignored) {
                reset();
                return;
            }
        }
    }

    public Set<Entry> tick() {
        if (!armed) {
            return null;
        }
        if (++elapsed >= RESPONSE_TIMEOUT) {
            reset();
            return null;
        }
        if (!collecting || ++quiet < QUIET_TICKS) {
            return null;
        }
        Set<Entry> result = Set.copyOf(pending);
        reset();
        return result;
    }

    public void reset() {
        armed = false;
        collecting = false;
        elapsed = 0;
        quiet = 0;
        owner = null;
        pending.clear();
    }
}
