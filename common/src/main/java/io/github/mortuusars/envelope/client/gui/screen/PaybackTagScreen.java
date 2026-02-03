package io.github.mortuusars.envelope.client.gui.screen;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.client.gui.Sprites;
import io.github.mortuusars.envelope.client.util.Minecrft;
import io.github.mortuusars.envelope.world.inventory.PaybackTagMenu;
import io.github.mortuusars.envelope.world.inventory.slot.DisabledSlot;
import io.github.mortuusars.envelope.world.item.component.PaybackRequest;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class PaybackTagScreen extends AbstractContainerScreen<PaybackTagMenu> {
    public static final ResourceLocation TEXTURE = Envelope.resource("textures/gui/payback_tag.png");

    protected ImageButton confirmButton;

    public PaybackTagScreen(PaybackTagMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected void init() {
        imageWidth = 186;
        imageHeight = 154;
        super.init();
        inventoryLabelY = imageHeight - 94;
        inventoryLabelX = 12;

        titleLabelX = (imageWidth / 2) - (font.width(getTitle()) / 2);

        confirmButton = new ImageButton(leftPos + 128, topPos + 28, 19, 19,
              Sprites.CONFIRM_BUTTON_SPRITES, button -> confirm(), Component.translatable("gui.envelope.confirm"));
        confirmButton.setTooltip(Tooltip.create(Component.translatable("gui.envelope.confirm")));
        addRenderableWidget(confirmButton);
    }

    protected void confirm() {
        getMenu().clickMenuButton(getMenu().getPlayer(), PaybackTagMenu.CONFIRM_BUTTON_ID);
        Minecrft.gameMode().handleInventoryButtonClick(getMenu().containerId, PaybackTagMenu.CONFIRM_BUTTON_ID);
        onClose();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTargetPreview(guiGraphics, mouseX, mouseY);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
    }

    @Override
    protected void renderSlot(GuiGraphics guiGraphics, Slot slot) {
        super.renderSlot(guiGraphics, slot);

        if (slot instanceof DisabledSlot) {
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(0, 0, 300);
            RenderSystem.enableBlend();
            guiGraphics.blit(TEXTURE, slot.x - 1, slot.y - 1, 186, 0, 18, 18);
            RenderSystem.disableBlend();
            guiGraphics.pose().popPose();
        }

//        if (slot.index < Payback.SLOTS) {
//            guiGraphics.pose().pushPose();
//            guiGraphics.pose().translate(0, 0, 300);
//            RenderSystem.enableBlend();
//            guiGraphics.blit(TEXTURE, slot.x - 1, slot.y - 1, 186, 18, 18, 18);
//            RenderSystem.disableBlend();
//            guiGraphics.pose().popPose();
//        }
    }

    protected void renderTargetPreview(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
        ItemStack target = getMenu().getTag().getItemStack();
        if (target.isEmpty()) {
            return;
        }

        float scale = 2f;
        int size = (int) (16 * scale);

        int x = leftPos - size - 4;
        int y = topPos + 36 - size / 2;

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(x + (float) size / 2, y + (float) size / 2, 0);
        guiGraphics.pose().scale(scale, scale, scale);
        guiGraphics.pose().translate(-8, -8, 0);
        guiGraphics.renderItem(target, 0, 0);
        guiGraphics.pose().popPose();

        if (isHovering(x - leftPos, y - topPos, size, size, mouseX, mouseY)) {
            guiGraphics.renderTooltip(font, target, mouseX, mouseY);
        }
    }

    // -- Input

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (super.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) {
            return true;
        }

        if (hoveredSlot != null && hoveredSlot.index < PaybackRequest.SLOTS) {
            boolean decreasing = scrollY < 0;
            boolean fast = Screen.hasShiftDown();
            changeRequestedItemCount(decreasing, fast);
            return true;
        }

        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (super.keyPressed(keyCode, scanCode, modifiers)){
            return true;
        }

        if (hoveredSlot != null && hoveredSlot.index < PaybackRequest.SLOTS) {
            if (keyCode == InputConstants.KEY_ADD || keyCode == InputConstants.KEY_EQUALS) {
                changeRequestedItemCount(false, Screen.hasShiftDown());
                return true;
            }

            if (keyCode == 333 /*KEY_SUBTRACT*/ || keyCode == InputConstants.KEY_MINUS) {
                changeRequestedItemCount(true, Screen.hasShiftDown());
                return true;
            }
        }

        return false;
    }

    protected void changeRequestedItemCount(boolean decreasing, boolean fast) {
        if (hoveredSlot == null) return;
        int startId = decreasing ? PaybackTagMenu.DECREASE_COUNT_START_BUTTON_ID : PaybackTagMenu.INCREASE_COUNT_START_BUTTON_ID;
        int id = startId + hoveredSlot.index + (fast ? PaybackRequest.SLOTS : 0);
        getMenu().clickMenuButton(getMenu().getPlayer(), id);
        Minecrft.gameMode().handleInventoryButtonClick(getMenu().containerId, id);
    }
}
