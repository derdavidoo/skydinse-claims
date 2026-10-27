package net.skydinse.claims.client.map;

import java.util.Iterator;
import java.util.Set;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import xaero.map.element.render.ElementRenderLocation;
import xaero.map.element.render.ElementRenderProvider;

public final class ClaimedChunkElementProvider extends ElementRenderProvider<ClaimedChunkElement, ClaimedChunkRenderContext> {
    @Override
    public void begin(ElementRenderLocation location, ClaimedChunkRenderContext context) {
        ResourceKey<Level> dimension = context.dimension;
        Set<ClaimedChunk> snapshot = SkydinseClaimsClientAccess.store().snapshot(dimension);
        context.iterator = snapshot.stream()
                .map(ClaimedChunkElement::new)
                .iterator();
    }

    @Override
    public boolean hasNext(ElementRenderLocation location, ClaimedChunkRenderContext context) {
        return context.iterator != null && context.iterator.hasNext();
    }

    @Override
    public ClaimedChunkElement getNext(ElementRenderLocation location, ClaimedChunkRenderContext context) {
        return context.iterator.next();
    }

    @Override
    public void end(ElementRenderLocation location, ClaimedChunkRenderContext context) {
        context.iterator = null;
    }
}
