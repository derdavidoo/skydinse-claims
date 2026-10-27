package net.skydinse.claims.client.map;

import net.minecraft.client.Minecraft;
import net.skydinse.claims.client.SkydinseClaimsClient;
import xaero.lib.client.graphics.XaeroBufferProvider;
import xaero.map.element.MapElementGraphics;
import xaero.map.element.render.ElementRenderInfo;
import xaero.map.element.render.ElementRenderLocation;
import xaero.map.element.render.ElementRenderer;
import xaero.map.graphics.renderer.multitexture.MultiTextureRenderTypeRendererProvider;

public final class ClaimedChunkElementRenderer extends ElementRenderer<ClaimedChunkElement, ClaimedChunkRenderContext, ClaimedChunkElementRenderer> {
    private static final int FILL = 0x8822AA55;
    private static final int BORDER = 0xCC55FF77;
    private static final int UNCLAIMED_FILL = 0x55DD3333;
    private static final int UNCLAIMED_BORDER = 0xCCAA2222;
    private static final int OWN_BORDER = 0xCCAA6655;
    private static final int HALF_CHUNK = 8;

    public ClaimedChunkElementRenderer(ClaimedChunkRenderContext context,
                                       ClaimedChunkElementProvider provider,
                                       ClaimedChunkElementReader reader) {
        super(context, provider, reader);
    }

    @Override
    public void preRender(ElementRenderInfo info, XaeroBufferProvider buffers,
                          MultiTextureRenderTypeRendererProvider renderers, boolean hovered) {
        context.dimension = info.mapDimension;
    }

    @Override
    public void postRender(ElementRenderInfo info, XaeroBufferProvider buffers,
                           MultiTextureRenderTypeRendererProvider renderers, boolean hovered) {
    }

    @Override
    public void renderElementShadow(ClaimedChunkElement element, boolean hovered, float scale,
                                    double x, double z, ElementRenderInfo info, MapElementGraphics graphics,
                                    XaeroBufferProvider buffers,
                                    MultiTextureRenderTypeRendererProvider renderers) {
    }

    @Override
    public boolean renderElement(ClaimedChunkElement element, boolean hovered, double depth, float screenScale,
                                 double x, double z, ElementRenderInfo info, MapElementGraphics graphics,
                                 XaeroBufferProvider buffers,
                                 MultiTextureRenderTypeRendererProvider renderers) {
        if (info.location != ElementRenderLocation.WORLD_MAP || !element.chunk().dimension().equals(info.mapDimension)) {
            return false;
        }

        // Xaero has already translated the pose to the element position.
        ClaimedChunk chunk = element.chunk();
        boolean unclaimed = ClaimedChunk.isUnclaimedOwner(chunk.owner());
        boolean local = !unclaimed && SkydinseClaimsClient.isLocalOwner(chunk.owner(), Minecraft.getInstance());
        int fill = unclaimed ? UNCLAIMED_FILL : FILL;
        int border = unclaimed ? UNCLAIMED_BORDER : local ? OWN_BORDER : BORDER;
        graphics.fill(-HALF_CHUNK, -HALF_CHUNK, HALF_CHUNK, HALF_CHUNK, fill);
        graphics.fill(-HALF_CHUNK, -HALF_CHUNK, HALF_CHUNK, -HALF_CHUNK + 1, border);
        graphics.fill(-HALF_CHUNK, HALF_CHUNK - 1, HALF_CHUNK, HALF_CHUNK, border);
        graphics.fill(-HALF_CHUNK, -HALF_CHUNK, -HALF_CHUNK + 1, HALF_CHUNK, border);
        graphics.fill(HALF_CHUNK - 1, -HALF_CHUNK, HALF_CHUNK, HALF_CHUNK, border);
        return false;
    }

    @Override
    public boolean shouldRender(ElementRenderLocation location, boolean cave) {
        return location == ElementRenderLocation.WORLD_MAP;
    }

    @Override
    public int getOrder() {
        return 20;
    }

    @Override
    public boolean shouldBeDimScaled() {
        return true;
    }
}
