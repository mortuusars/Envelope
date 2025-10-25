package io.github.mortuusars.envelope.mixin.bugger;

import io.github.mortuusars.envelope.util.bugger.BuggerGui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DebugScreenOverlay.class)
public class ScreenOverlayMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void onRender(GuiGraphics guiGraphics, CallbackInfo ci) {
        if (BuggerGui.render(guiGraphics)) {
            ci.cancel();
        }
    }
}
