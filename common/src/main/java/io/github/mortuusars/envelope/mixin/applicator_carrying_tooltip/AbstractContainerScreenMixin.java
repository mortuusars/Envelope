package io.github.mortuusars.envelope.mixin.applicator_carrying_tooltip;

import io.github.mortuusars.envelope.client.util.Minecrft;
import io.github.mortuusars.envelope.world.item.ApplicatorItem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin<T extends AbstractContainerMenu> extends Screen {
    protected AbstractContainerScreenMixin(Component title) {
        super(title);
    }

    @Shadow public abstract T getMenu();

    @Shadow @Nullable
    protected Slot hoveredSlot;

    @Shadow protected abstract List<Component> getTooltipFromContainerItem(ItemStack stack);

    /**
     * Because tooltip is not rendered when carrying an item, we render it manually when ApplicatorItem needs it.<br>
     * This helps to choose/see the item you're changing.
     */
    @Inject(method = "renderTooltip", at = @At("HEAD"), cancellable = true)
    private void onRenderTooltip(GuiGraphics guiGraphics, int x, int y, CallbackInfo ci) {
        if (hoveredSlot == null || !hoveredSlot.hasItem()
              || !(getMenu().getCarried().getItem() instanceof ApplicatorItem applicator)) {
            return;
        }

        ItemStack hovered = hoveredSlot.getItem();

        if (applicator.shouldRenderTooltipWhileCarrying(Minecrft.level(), hoveredSlot.getItem(), hovered)) {
            guiGraphics.renderTooltip(font, getTooltipFromContainerItem(hovered), hovered.getTooltipImage(), x, y);
            ci.cancel();
        }
    }
}
