package net.skydinse.claims.client.map;

import java.util.Iterator;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public final class ClaimedChunkRenderContext {
    ResourceKey<Level> dimension;
    Iterator<ClaimedChunkElement> iterator;
}
