package io.github.mortuusars.envelope.mixin.fix_edit_box;

import io.github.mortuusars.envelope.client.gui.screen.AddressTagScreen;
import io.github.mortuusars.mortaar.client.Minecrft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EditBox.class)
public abstract class EditBoxMixin extends AbstractWidget {
    public EditBoxMixin(int x, int y, int width, int height, Component message) {
        super(x, y, width, height, message);
    }

    // I hope no-one will try to do modify the same constant, because it will most likely crash.
    // But I don't see other easy ways to change the color.
    @ModifyConstant(method = "renderWidget", constant = @Constant(intValue = 0xFF808080))
    private int onRenderWidget(int constant) {
        if (Minecrft.get().screen instanceof AddressTagScreen addressTagScreen) {
            return addressTagScreen.getSuggestionTextColor();
        }
        return constant;
    }

    // Purpose of scissoring here is to limit suggestion text to the widget bounds, otherwise it overflows.
    // You can clearly tell that mojang made half of features of EditBox for chat screen and never bothered to make them work elsewhere.
    @Inject(method = "renderWidget", at = @At("HEAD"))
    private void onRenderWidgetHead(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (Minecrft.get().screen instanceof AddressTagScreen) {
            guiGraphics.enableScissor(getX() - 1, getY(), getX() + getWidth() + 1, getY() + getHeight());
        }
    }

    @Inject(method = "renderWidget", at = @At("RETURN"))
    private void onRenderWidgetReturn(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (Minecrft.get().screen instanceof AddressTagScreen) {
            guiGraphics.disableScissor();
        }
    }
}