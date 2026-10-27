package net.skydinse.claims.client.map;

import net.minecraft.client.Minecraft;
import xaero.map.element.render.ElementReader;

public final class ClaimedChunkElementReader extends ElementReader<ClaimedChunkElement, ClaimedChunkRenderContext, ClaimedChunkElementRenderer> {
    @Override
    public boolean isHidden(ClaimedChunkElement element, ClaimedChunkRenderContext context) {
        return !element.chunk().dimension().equals(context.dimension);
    }

    @Override
    public double getRenderX(ClaimedChunkElement element, ClaimedChunkRenderContext context, float partialTicks) {
        return (element.chunk().chunkX() << 4) + 8.0;
    }

    @Override
    public double getRenderZ(ClaimedChunkElement element, ClaimedChunkRenderContext context, float partialTicks) {
        return (element.chunk().chunkZ() << 4) + 8.0;
    }

    @Override public int getInteractionBoxLeft(ClaimedChunkElement e, ClaimedChunkRenderContext c, float p) { return -8; }
    @Override public int getInteractionBoxRight(ClaimedChunkElement e, ClaimedChunkRenderContext c, float p) { return 8; }
    @Override public int getInteractionBoxTop(ClaimedChunkElement e, ClaimedChunkRenderContext c, float p) { return -8; }
    @Override public int getInteractionBoxBottom(ClaimedChunkElement e, ClaimedChunkRenderContext c, float p) { return 8; }
    @Override public int getRenderBoxLeft(ClaimedChunkElement e, ClaimedChunkRenderContext c, float p) { return -8; }
    @Override public int getRenderBoxRight(ClaimedChunkElement e, ClaimedChunkRenderContext c, float p) { return 8; }
    @Override public int getRenderBoxTop(ClaimedChunkElement e, ClaimedChunkRenderContext c, float p) { return -8; }
    @Override public int getRenderBoxBottom(ClaimedChunkElement e, ClaimedChunkRenderContext c, float p) { return 8; }
    @Override public int getLeftSideLength(ClaimedChunkElement e, Minecraft minecraft) { return 0; }
    @Override public String getMenuName(ClaimedChunkElement e) {
        return ClaimedChunk.isUnclaimedOwner(e.chunk().owner()) ? ClaimedChunk.UNCLAIMED_OWNER : "Claimed chunk";
    }
    @Override public String getFilterName(ClaimedChunkElement e) { return "Claimed chunk"; }
    @Override public int getMenuTextFillLeftPadding(ClaimedChunkElement e) { return 0; }
    @Override public int getRightClickTitleBackgroundColor(ClaimedChunkElement e) { return 0xFF2E7D32; }
    @Override public boolean shouldScaleBoxWithOptionalScale() { return false; }
}
