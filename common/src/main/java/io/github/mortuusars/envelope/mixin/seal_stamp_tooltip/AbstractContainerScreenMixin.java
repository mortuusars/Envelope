package io.github.mortuusars.envelope.mixin.seal_stamp_tooltip;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.client.util.Minecrft;
import io.github.mortuusars.envelope.world.item.SealStampItem;
import io.github.mortuusars.envelope.world.item.Sealable;
import io.github.mortuusars.envelope.world.item.component.seal.Seal;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin<T extends AbstractContainerMenu> {
    @Shadow public abstract T getMenu();

    @Shadow @Nullable
    protected Slot hoveredSlot;

    /**
     * Rendering tooltip manually to see the item you're sealing and sealing results right away
     * - instead of having to place Seal Stamp back into inventory. (Because tooltip is not rendered when carrying an item)
     */
    @Inject(method = "renderTooltip", at = @At("HEAD"))
    private void onRenderTooltip(GuiGraphics guiGraphics, int x, int y, CallbackInfo ci) {
        if (hoveredSlot == null || !(getMenu().getCarried().getItem() instanceof SealStampItem)) return;
        ItemStack target = hoveredSlot.getItem();
        @Nullable Seal seal = target.get(Envelope.DataComponents.SEAL);
        if (seal != null || (target.getItem() instanceof Sealable sealable && sealable.canSeal(Minecrft.level(), target))) {
            guiGraphics.renderTooltip(Minecrft.get().font, target, x, y);
        }
    }
}
