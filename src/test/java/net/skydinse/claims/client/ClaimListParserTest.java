package net.skydinse.claims.client;

import java.nio.charset.StandardCharsets;
import java.util.Set;

public final class ClaimListParserTest {
    private static final String HEADER = "[Claim] Gesch\u00fctzte Chunks von Player:";

    public static void main(String[] args) throws Exception {
        ClaimListParser parser = new ClaimListParser();
        String fixture;
        try (var input = ClaimListParserTest.class.getResourceAsStream("/claim-list.txt")) {
            fixture = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
        parser.accept(fixture);
        require(finish(parser) == null, "unsolicited responses must be ignored");
        parser.commandSent("claim list");
        for (String line : fixture.split("\\R")) {
            parser.accept(line);
        }
        Set<ClaimListParser.Entry> claims = finish(parser);
        require(claims != null && claims.size() == 150, "parse all 150 log claims");
        require(claims.contains(new ClaimListParser.Entry("minecraft:overworld", -410, -363, "EinDavidoo")), "negative coordinates");
        parser.commandSent("/CLAIM   list");
        parser.accept(HEADER + "\n" + row("Nether", -1, 2) + "\n" + row("Nether", -1, 2));
        claims = finish(parser);
        require(claims != null && claims.equals(Set.of(new ClaimListParser.Entry("minecraft:the_nether", -1, 2, "Player"))), "replace, deduplicate, and use record dimension");
        parser.commandSent("claim list");
        parser.accept(HEADER);
        require(finish(parser).isEmpty(), "header-only list clears old claims");
        parser.commandSent("claim list");
        parser.accept(HEADER + "\n" + row("Unknown", 1, 2));
        require(finish(parser) == null, "unknown dimensions preserve previous claims");
        parser.commandSent("claim list");
        parser.accept(HEADER + "\n[Claim] #1: Koordinaten: broken");
        require(finish(parser) == null, "malformed entries preserve previous claims");
        parser.commandSent("claim list");
        for (int i = 0; i < 600; i++) parser.tick();
        parser.accept(fixture);
        require(finish(parser) == null, "expired responses ignored");
        parser.commandSent("claim list");
        parser.accept(fixture);
        parser.reset();
        require(finish(parser) == null, "disconnect cancels collection");
        parser.commandSent("claim list");
        parser.accept(fixture);
        parser.commandSent("claim list");
        parser.accept(HEADER + "\n" + row("End", 999999, -999999));
        require(finish(parser).equals(Set.of(new ClaimListParser.Entry("minecraft:the_end", 999999, -999999, "Player"))), "repeated request replaces pending data");
        System.out.println("Claim list parser checks passed, including all 150 supplied log records.");
    }

    private static String row(String world, int x, int z) {
        return "[Claim] #1: Koordinaten: [x: 0, z: 0] Welt: [" + world + "] Chunk: [" + x + ", " + z + "] Datum: [date]";
    }

    private static Set<ClaimListParser.Entry> finish(ClaimListParser parser) {
        Set<ClaimListParser.Entry> result = null;
        for (int i = 0; i < 40; i++) {
            Set<ClaimListParser.Entry> update = parser.tick();
            if (update != null) result = update;
        }
        return result;
    }

    private static void require(boolean condition, String label) {
        if (!condition) throw new AssertionError(label);
    }
}
