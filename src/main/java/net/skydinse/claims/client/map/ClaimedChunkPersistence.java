package net.skydinse.claims.client.map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public final class ClaimedChunkPersistence {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "skydinseclaims-claimed-chunks.json";

    private ClaimedChunkPersistence() { }

    public static List<ClaimedChunk> load(String serverAddress) {
        Path file = file(serverAddress);
        if (!Files.isRegularFile(file)) return List.of();
        try {
            JsonElement root = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8));
            if (!root.isJsonArray()) return List.of();
            List<ClaimedChunk> result = new ArrayList<>();
            for (JsonElement element : root.getAsJsonArray()) {
                if (!element.isJsonObject()) continue;
                JsonObject object = element.getAsJsonObject();
                ResourceKey<Level> dimension = dimension(object.get("dimension").getAsString());
                if (dimension != null) {
                    result.add(new ClaimedChunk(dimension, object.get("x").getAsInt(),
                            object.get("z").getAsInt(), object.get("owner").getAsString()));
                }
            }
            return result;
        } catch (Exception ignored) {
            return List.of();
        }
    }

    public static void save(String serverAddress, Iterable<ClaimedChunk> chunks) {
        JsonArray array = new JsonArray();
        for (ClaimedChunk chunk : chunks) {
            JsonObject object = new JsonObject();
            object.addProperty("dimension", dimensionName(chunk.dimension()));
            object.addProperty("x", chunk.chunkX());
            object.addProperty("z", chunk.chunkZ());
            object.addProperty("owner", chunk.owner());
            array.add(object);
        }
        Path file = file(serverAddress);
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, GSON.toJson(array), StandardCharsets.UTF_8);
        } catch (IOException ignored) {
            // A failed cache write must not interrupt the client tick.
        }
    }

    private static Path file(String serverAddress) {
        String key = Integer.toUnsignedString(serverAddress.hashCode(), 16);
        return Minecraft.getInstance().gameDirectory.toPath().resolve("config").resolve(FILE_NAME + "-" + key);
    }

    private static ResourceKey<Level> dimension(String value) {
        return switch (value) {
            case "minecraft:overworld" -> Level.OVERWORLD;
            case "minecraft:the_nether" -> Level.NETHER;
            case "minecraft:the_end" -> Level.END;
            default -> null;
        };
    }

    private static String dimensionName(ResourceKey<Level> dimension) {
        if (dimension.equals(Level.NETHER)) return "minecraft:the_nether";
        if (dimension.equals(Level.END)) return "minecraft:the_end";
        return "minecraft:overworld";
    }
}
