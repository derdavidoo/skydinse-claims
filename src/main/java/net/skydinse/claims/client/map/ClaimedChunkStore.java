package net.skydinse.claims.client.map;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public final class ClaimedChunkStore {
    private final Map<ChunkKey, ClaimedChunk> chunks = new HashMap<>();
    private Set<ClaimedChunk> allSnapshot = Set.of();
    private final Map<ResourceKey<Level>, Set<ClaimedChunk>> dimensionSnapshots = new HashMap<>();
    private final Map<RegionKey, Set<ClaimedChunk>> regionSnapshots = new HashMap<>();

    public synchronized boolean replace(Collection<ClaimedChunk> newChunks) {
        Map<ChunkKey, ClaimedChunk> replacement = new HashMap<>(Math.max(16, newChunks.size() * 2));
        for (ClaimedChunk chunk : newChunks) replacement.put(keyOf(chunk), chunk);
        if (chunks.equals(replacement)) return false;
        chunks.clear();
        chunks.putAll(replacement);
        invalidateSnapshots();
        return true;
    }

    public synchronized boolean append(Collection<ClaimedChunk> newChunks) {
        boolean changed = false;
        for (ClaimedChunk chunk : newChunks) {
            if (putInternal(chunk)) changed = true;
        }
        if (changed) invalidateSnapshots();
        return changed;
    }

    public synchronized boolean put(ClaimedChunk newChunk) {
        if (!putInternal(newChunk)) return false;
        invalidateSnapshots();
        return true;
    }

    public synchronized boolean clear() {
        if (chunks.isEmpty()) return false;
        chunks.clear();
        invalidateSnapshots();
        return true;
    }

    public synchronized Set<ClaimedChunk> snapshot() {
        if (allSnapshot.isEmpty() && !chunks.isEmpty()) rebuildSnapshots();
        return allSnapshot;
    }

    public synchronized Set<ClaimedChunk> snapshot(ResourceKey<Level> dimension) {
        if (!dimensionSnapshots.containsKey(dimension)) rebuildDimensionSnapshot(dimension);
        return dimensionSnapshots.getOrDefault(dimension, Set.of());
    }

    public synchronized Set<ClaimedChunk> snapshotRegion(ResourceKey<Level> dimension, int regionX, int regionZ) {
        RegionKey region = new RegionKey(dimension, regionX, regionZ);
        if (!regionSnapshots.containsKey(region)) {
            Set<ClaimedChunk> result = new HashSet<>();
            int firstChunkX = regionX << 5;
            int firstChunkZ = regionZ << 5;
            for (ClaimedChunk chunk : snapshot(dimension)) {
                if (chunk.chunkX() >= firstChunkX && chunk.chunkX() < firstChunkX + 32
                        && chunk.chunkZ() >= firstChunkZ && chunk.chunkZ() < firstChunkZ + 32) result.add(chunk);
            }
            regionSnapshots.put(region, result.isEmpty() ? Set.of() : Collections.unmodifiableSet(result));
        }
        return regionSnapshots.get(region);
    }

    public synchronized boolean contains(ResourceKey<Level> dimension, int chunkX, int chunkZ) {
        return find(dimension, chunkX, chunkZ) != null;
    }

    public synchronized ClaimedChunk find(ResourceKey<Level> dimension, int chunkX, int chunkZ) {
        return chunks.get(new ChunkKey(dimension, chunkX, chunkZ));
    }

    private boolean putInternal(ClaimedChunk chunk) {
        ChunkKey key = keyOf(chunk);
        if (chunk.equals(chunks.get(key))) return false;
        chunks.put(key, chunk);
        return true;
    }

    private static ChunkKey keyOf(ClaimedChunk chunk) {
        return new ChunkKey(chunk.dimension(), chunk.chunkX(), chunk.chunkZ());
    }

    private void invalidateSnapshots() {
        allSnapshot = Set.of();
        dimensionSnapshots.clear();
        regionSnapshots.clear();
    }

    private void rebuildSnapshots() {
        allSnapshot = Collections.unmodifiableSet(new HashSet<>(chunks.values()));
    }

    private void rebuildDimensionSnapshot(ResourceKey<Level> dimension) {
        Set<ClaimedChunk> result = new HashSet<>();
        for (ClaimedChunk chunk : chunks.values()) {
            if (chunk.dimension().equals(dimension)) result.add(chunk);
        }
        dimensionSnapshots.put(dimension, result.isEmpty() ? Set.of() : Collections.unmodifiableSet(result));
    }

    private record ChunkKey(ResourceKey<Level> dimension, int chunkX, int chunkZ) { }
    private record RegionKey(ResourceKey<Level> dimension, int regionX, int regionZ) { }
}
