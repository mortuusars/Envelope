package io.github.mortuusars.envelope.mixin.bugger;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;

@Mixin(DebugScreenOverlay.class)
public interface ScreenRenderLinesInvoker {
    @Invoker("renderLines")
    void drawLines(GuiGraphics guiGraphics, List<String> lines, boolean leftSide);
}
