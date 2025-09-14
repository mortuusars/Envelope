package io.github.mortuusars.envelope.mixin.bugger;

import io.github.mortuusars.envelope.PlatformHelper;
import io.github.mortuusars.envelope.util.bugger.Bugger;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DebugScreenOverlay.class)
public class ScreenOverlayMixin {
    @SuppressWarnings("deprecation")
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void onRender(GuiGraphics guiGraphics, CallbackInfo ci) {
        if (!PlatformHelper.isInDevEnv()) return;

        if (Bugger.page == 0) {
            Minecraft.getInstance().getProfiler().push("bugger_main");
            guiGraphics.drawManaged(() -> Bugger.renderMainPage(guiGraphics));
            Minecraft.getInstance().getProfiler().pop();
            ci.cancel();
        }

        if (Bugger.page == 1) {
            Minecraft.getInstance().getProfiler().push("bugger_tag");
            guiGraphics.drawManaged(() -> Bugger.renderTagPage(guiGraphics));
            Minecraft.getInstance().getProfiler().pop();
            ci.cancel();
        }

        String str = "[<-] and [->] to switch pages";
        int strWidth = Minecraft.getInstance().font.width(str);
        int x = guiGraphics.guiWidth() / 2 - strWidth / 2;
        guiGraphics.fill(x - 1, 1, x + strWidth + 1, 10, -1873784752);
        guiGraphics.drawString(Minecraft.getInstance().font, str, x, 2, 0xFFFFFFFF, false);
    }
}
