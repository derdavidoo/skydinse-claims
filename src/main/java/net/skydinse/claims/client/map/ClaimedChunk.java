package net.skydinse.claims.client.map;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public record ClaimedChunk(ResourceKey<Level> dimension, int chunkX, int chunkZ, String owner) {
    public static final String UNCLAIMED_OWNER = "Unclaimed";

    public ClaimedChunk {
        if (isUnclaimedOwner(owner)) owner = UNCLAIMED_OWNER;
    }

    public ClaimedChunk(ResourceKey<Level> dimension, int chunkX, int chunkZ) {
        this(dimension, chunkX, chunkZ, "EinDavidoo");
    }

    public static boolean isUnclaimedOwner(String owner) {
        return owner != null && (owner.trim().equalsIgnoreCase("nobody")
                || owner.trim().equalsIgnoreCase("niemand")
                || owner.trim().equalsIgnoreCase(UNCLAIMED_OWNER));
    }
}
