package net.skydinse.claims.client.mixin;

import net.skydinse.claims.client.map.ClaimedChunkHighlighter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.map.highlight.HighlighterRegistry;

@Mixin(HighlighterRegistry.class)
public abstract class HighlighterRegistryMixin {
    @Inject(method = "end", at = @At("HEAD"))
    private void skydinseClaims$registerHighlighter(CallbackInfo callbackInfo) {
        ((HighlighterRegistry) (Object) this).register(new ClaimedChunkHighlighter());
    }
}
