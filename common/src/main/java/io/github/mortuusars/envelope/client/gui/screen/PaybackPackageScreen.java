package io.github.mortuusars.envelope.client.gui.screen;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.inventory.PaybackPackageMenu;
import io.github.mortuusars.envelope.world.inventory.slot.PreviewSlot;
import io.github.mortuusars.envelope.world.item.PaybackPackageItem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class PaybackPackageScreen extends AbstractInHandContainerScreen<PaybackPackageMenu> {
    public static final ResourceLocation TEXTURE = Envelope.resource("textures/gui/payback_package.png");

    public PaybackPackageScreen(PaybackPackageMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, TEXTURE);
    }

    @Override
    protected void init() {
        imageWidth = 176;
        imageHeight = 178;
        super.init();
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        super.renderBg(guiGraphics, partialTick, mouseX, mouseY);

        if (getMenu().isDestroyedOnClose()) {
            guiGraphics.blit(TEXTURE, leftPos + 45, topPos + 17, 0, 178, 86, 64);
        }
    }

    @Override
    protected @NotNull List<Component> getTooltipFromContainerItem(ItemStack stack) {
        List<Component> components = super.getTooltipFromContainerItem(stack);

        if (hoveredSlot instanceof PreviewSlot) {
            PaybackPackageItem.appendPaybackSubjectHoverText(components, getMenu().getPaybackSubject());
        }

        return components;
    }
}