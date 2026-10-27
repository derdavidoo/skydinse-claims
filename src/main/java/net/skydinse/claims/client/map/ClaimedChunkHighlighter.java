package net.skydinse.claims.client.map;

import java.awt.Color;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.skydinse.claims.client.SkydinseClaimsClient;
import xaero.map.highlight.ChunkHighlighter;

public final class ClaimedChunkHighlighter extends ChunkHighlighter {
    private static final int OWN_FILL = 0x33CC66CC;
    private static final int OWN_BORDER = 0x7A6045AA;
    private static final int UNCLAIMED_FILL = 0xDD333355;
    private static final int UNCLAIMED_BORDER = 0xCC2222AA;
    private static final int OTHER_ALPHA = 0x88;
    private static final Map<String, int[]> OWNER_COLORS = new HashMap<>();

    public ClaimedChunkHighlighter() { super(true); }

    @Override
    public boolean regionHasHighlights(ResourceKey<Level> dimension, int regionX, int regionZ) {
        if (!SkydinseServer.isActive() || !SkydinseClaimsClient.highlightsEnabled()) return false;
        return !SkydinseClaimsClientAccess.store().snapshotRegion(dimension, regionX, regionZ).isEmpty();
    }

    @Override
    protected int[] getColors(ResourceKey<Level> dimension, int chunkX, int chunkZ) {
        ClaimedChunk chunk = SkydinseClaimsClientAccess.store().find(dimension, chunkX, chunkZ);
        if (!SkydinseServer.isActive() || !SkydinseClaimsClient.highlightsEnabled() || chunk == null) return null;
        int fill = colorFor(chunk.owner());
        int border = borderFor(chunk.owner());
        return new int[]{fill,
                sameOwner(dimension, chunkX, chunkZ - 1, chunk.owner()) ? fill : border,
                sameOwner(dimension, chunkX + 1, chunkZ, chunk.owner()) ? fill : border,
                sameOwner(dimension, chunkX, chunkZ + 1, chunk.owner()) ? fill : border,
                sameOwner(dimension, chunkX - 1, chunkZ, chunk.owner()) ? fill : border};
    }

    @Override
    public boolean chunkIsHighlit(ResourceKey<Level> dimension, int chunkX, int chunkZ) {
        return SkydinseServer.isActive() && SkydinseClaimsClient.highlightsEnabled()
                && SkydinseClaimsClientAccess.store().contains(dimension, chunkX, chunkZ);
    }

    @Override
    public int calculateRegionHash(ResourceKey<Level> dimension, int regionX, int regionZ) {
        if (!SkydinseServer.isActive() || !SkydinseClaimsClient.highlightsEnabled()) return 0;
        int hash = 1;
        for (ClaimedChunk chunk : SkydinseClaimsClientAccess.store().snapshotRegion(dimension, regionX, regionZ)) {
            hash = 31 * hash + chunk.hashCode();
        }
        return hash;
    }

    @Override
    public Component getChunkHighlightSubtleTooltip(ResourceKey<Level> dimension, int chunkX, int chunkZ) {
        ClaimedChunk chunk = SkydinseClaimsClientAccess.store().find(dimension, chunkX, chunkZ);
        return SkydinseServer.isActive() && SkydinseClaimsClient.highlightsEnabled() && chunk != null
                ? Component.literal(ClaimedChunk.isUnclaimedOwner(chunk.owner())
                        ? ClaimedChunk.UNCLAIMED_OWNER : chunk.owner()) : null;
    }

    @Override
    public Component getChunkHighlightBluntTooltip(ResourceKey<Level> dimension, int chunkX, int chunkZ) {
        return getChunkHighlightSubtleTooltip(dimension, chunkX, chunkZ);
    }

    @Override
    public void addMinimapBlockHighlightTooltips(List<Component> tooltips, ResourceKey<Level> dimension,
                                                   int blockX, int blockY, int blockZ) { }

    private static boolean sameOwner(ResourceKey<Level> dimension, int chunkX, int chunkZ, String owner) {
        ClaimedChunk neighbor = SkydinseClaimsClientAccess.store().find(dimension, chunkX, chunkZ);
        return neighbor != null && neighbor.owner().equalsIgnoreCase(owner);
    }

    private static int colorFor(String owner) {
        if (ClaimedChunk.isUnclaimedOwner(owner)) return UNCLAIMED_FILL;
        if (SkydinseClaimsClient.isLocalOwner(owner, Minecraft.getInstance())) return OWN_FILL;
        return ownerColors(owner)[0];
    }

    private static int borderFor(String owner) {
        if (ClaimedChunk.isUnclaimedOwner(owner)) return UNCLAIMED_BORDER;
        if (SkydinseClaimsClient.isLocalOwner(owner, Minecraft.getInstance())) return OWN_BORDER;
        return ownerColors(owner)[1];
    }

    private static synchronized int[] ownerColors(String owner) {
        String key = owner.toLowerCase(java.util.Locale.ROOT);
        return OWNER_COLORS.computeIfAbsent(key, ignored -> {
            int hash = key.hashCode();
            float hue = Math.floorMod(hash, 360) / 360.0f;
            Color fill = Color.getHSBColor(hue, 0.72f, 0.95f);
            Color border = Color.getHSBColor(hue, 0.82f, 0.55f);
            return new int[]{
                    (fill.getRed() << 24) | (fill.getGreen() << 16) | (fill.getBlue() << 8) | OTHER_ALPHA,
                    (border.getRed() << 24) | (border.getGreen() << 16) | (border.getBlue() << 8) | OTHER_ALPHA
            };
        });
    }
}
