package net.skydinse.claims.client.mixin;

import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.skydinse.claims.client.SkydinseClaimsClient;
import xaero.map.WorldMap;
import xaero.map.gui.GuiTexturedButton;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.map.gui.GuiMap;

@Mixin(GuiMap.class)
public abstract class GuiMapClaimsButtonMixin {
    @Unique
    private Button skydinseClaims$toggleButton;

    @Inject(method = "init", at = @At(value = "FIELD",
            target = "Lxaero/map/gui/GuiMap;caveModeButton:Lnet/minecraft/client/gui/components/Button;",
            opcode = Opcodes.PUTFIELD, shift = At.Shift.AFTER))
    private void skydinseClaims$addToggleButton(CallbackInfo callbackInfo) {
        GuiMap screen = (GuiMap) (Object) this;
        skydinseClaims$toggleButton = screen.addButton(new GuiTexturedButton(
                0, screen.height - 80, 20, 20,
                197, 64, 16, 16, WorldMap.guiTextures,
                button -> {
                    SkydinseClaimsClient.setHighlightsEnabled(!SkydinseClaimsClient.highlightsEnabled());
                }, null, 256, 256));
    }

    @Unique
    private static Component skydinseClaims$buttonText() {
        return Component.literal(SkydinseClaimsClient.highlightsEnabled() ? "C" : "-");
    }
}
