package io.github.mortuusars.envelope.client.gui.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.client.gui.Sprites;
import io.github.mortuusars.envelope.client.util.Minecrft;
import io.github.mortuusars.envelope.client.util.Pos2i;
import io.github.mortuusars.envelope.world.inventory.PackageMenu;
import io.github.mortuusars.envelope.world.item.PackageItem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class PackageScreen extends AbstractContainerScreen<PackageMenu> {
    public static final ResourceLocation TEXTURE = Envelope.resource("textures/gui/package.png");
    public static final WidgetSprites PACK_BUTTON_SPRITES = Sprites.threeStates(Envelope.resource("package/pack_button"));

    protected ImageButton packButton;

    public PackageScreen(PackageMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected void init() {
        imageWidth = 176;
        imageHeight = 178;
        super.init();
        inventoryLabelY = imageHeight - 94;

        packButton = new ImageButton(leftPos + 126, topPos + 40, 26, 20, PACK_BUTTON_SPRITES, button -> pack(), Component.translatable("gui.envelope.package.pack"));
        packButton.setTooltip(Tooltip.create(Component.translatable("gui.envelope.package.pack")
                .append(CommonComponents.NEW_LINE)
                .append(Component.translatable("gui.envelope.package.pack.tooltip.packs_remaining",
                        getPrettyPacksRemaining()))));
        addRenderableWidget(packButton);
    }

    protected String getPrettyPacksRemaining() {
        int count = getMenu().getPackage().map(PackageItem::getRemainingPacks);
        if (count > 99) {
            return ">99";
        }
        return Integer.toString(count);
    }

    protected void pack() {
        Minecrft.gameMode().handleInventoryButtonClick(getMenu().containerId, PackageMenu.PACK_BUTTON_ID);
        onClose();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
        Pos2i packageSlotPos = getMenu().getPackageSlotPos();
        guiGraphics.renderItem(getMenu().getPackage().getItemStack(), leftPos + packageSlotPos.x, topPos + packageSlotPos.y);
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0, 0, 300);
        RenderSystem.enableBlend();
        guiGraphics.blit(TEXTURE, leftPos + packageSlotPos.x - 1, topPos + packageSlotPos.y - 1, 176, 0, 18, 18);
        RenderSystem.disableBlend();
        guiGraphics.pose().popPose();
    }

    @Override
    protected void renderTooltip(GuiGraphics guiGraphics, int x, int y) {
        super.renderTooltip(guiGraphics, x, y);
        if (hoveredSlot == null && isHovering(45, 17, 86, 64, x, y) && getMenu().isPackageDestroyedOnClose()) {
            guiGraphics.renderTooltip(font, Component.translatable("gui.envelope.package.destroyed_on_close"), x, y);
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        packButton.visible = getMenu().canPack() && getMenu().needsPacking();
        guiGraphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        guiGraphics.blit(TEXTURE, leftPos + 45, topPos + 17, 0, 178, 86, 64);

        if (getMenu().isPackageDestroyedOnClose()) {
            guiGraphics.blit(TEXTURE, leftPos + 45, topPos + 17, 86, 178, 86, 64);
        }
    }
}
