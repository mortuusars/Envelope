package io.github.mortuusars.envelope.client.gui.screen;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.inventory.PackageMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class PackageScreen extends AbstractInHandContainerScreen<PackageMenu> {
    public static final ResourceLocation TEXTURE = Envelope.resource("textures/gui/package.png");

    public PackageScreen(PackageMenu menu, Inventory playerInventory, Component title) {
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
}