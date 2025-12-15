package io.github.mortuusars.envelope.client.gui.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.client.gui.Sprites;
import io.github.mortuusars.envelope.client.util.Minecrft;
import io.github.mortuusars.envelope.world.inventory.PackingMenu;
import io.github.mortuusars.envelope.world.inventory.slot.DisabledSlot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

public class PackingScreen extends AbstractContainerScreen<PackingMenu> {
    public static final ResourceLocation TEXTURE = Envelope.resource("textures/gui/packing.png");
    public static final WidgetSprites PACK_BUTTON_SPRITES = Sprites.threeStates(Envelope.resource("packing_box/pack_button"));

    protected ImageButton packButton;

    public PackingScreen(PackingMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected void init() {
        imageWidth = 176;
        imageHeight = 178;
        super.init();
        inventoryLabelY = imageHeight - 94;

        packButton = addRenderableWidget(new ImageButton(leftPos + 126, topPos + 40, 26, 20,
              PACK_BUTTON_SPRITES,
              button -> pack(),
              Component.translatable("gui.envelope.package.pack")));
        packButton.setTooltip(Tooltip.create(Component.translatable("gui.envelope.package.pack")
                .append(CommonComponents.NEW_LINE)
                .append(Component.translatable("gui.envelope.package.pack.tooltip.packs_remaining",
                        getPrettyPacksRemaining()))));
    }

    protected String getPrettyPacksRemaining() {
        int count = getMenu().getPackage().getRemainingPacks(getMenu().getBoxStack());
        if (count > 99) {
            return ">99";
        }
        return Integer.toString(count);
    }

    protected void pack() {
        Minecrft.gameMode().handleInventoryButtonClick(getMenu().containerId, PackingMenu.PACK_BUTTON_ID);
        onClose();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        packButton.visible = getMenu().needsPacking() && getMenu().canPack();
        guiGraphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);

        if (getMenu().isPackageDestroyedOnClose()) {
            guiGraphics.blit(TEXTURE, leftPos + 45, topPos + 17, 0, 178, 86, 64);
        }
    }

    @Override
    protected void renderSlot(GuiGraphics guiGraphics, Slot slot) {
        super.renderSlot(guiGraphics, slot);

        if (slot instanceof DisabledSlot) {
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(0, 0, 300);
            RenderSystem.enableBlend();
            guiGraphics.blit(TEXTURE, slot.x - 1, slot.y - 1, 176, 0, 18, 18);
            RenderSystem.disableBlend();
            guiGraphics.pose().popPose();
        }
    }
}
