package io.github.mortuusars.envelope.client.gui.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import io.github.mortuusars.envelope.world.inventory.AbstractInHandContainerMenu;
import io.github.mortuusars.envelope.world.inventory.slot.DisabledSlot;
import io.github.mortuusars.envelope.world.inventory.slot.PickupOnlySlot;
import io.github.mortuusars.envelope.world.inventory.slot.PreviewSlot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

public class AbstractInHandContainerScreen<T extends AbstractInHandContainerMenu> extends AbstractContainerScreen<T> {
    private final ResourceLocation texture;

    public AbstractInHandContainerScreen(T menu, Inventory playerInventory, Component title, ResourceLocation texture) {
        super(menu, playerInventory, title);
        this.texture = texture;
    }

    @Override
    protected void init() {
        super.init();
        inventoryLabelY = imageHeight - 94;
    }

    public int getLeftPos() {
        return leftPos;
    }

    public int getTopPos() {
        return topPos;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        updateButtons();
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    protected void updateButtons() {

    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(texture, leftPos, topPos, 0, 0, imageWidth, imageHeight);
    }

    @Override
    protected void renderSlot(GuiGraphics guiGraphics, Slot slot) {
        super.renderSlot(guiGraphics, slot);
        renderSlotOverlays(guiGraphics, slot);
    }

    protected void renderSlotOverlays(GuiGraphics guiGraphics, Slot slot) {
        if (slot instanceof PreviewSlot) {
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(0, 0, 300);
            RenderSystem.enableBlend();
            guiGraphics.blitSprite(PreviewSlot.OVERLAY_SPRITE, slot.x - 1, slot.y - 1, 18, 18);
            RenderSystem.disableBlend();
            guiGraphics.pose().popPose();
        } else if (shouldRenderDisabledOverlayOver(slot)) {
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(0, 0, 300);
            RenderSystem.enableBlend();
            guiGraphics.blitSprite(DisabledSlot.OVERLAY_SPRITE, slot.x - 1, slot.y - 1, 18, 18);
            RenderSystem.disableBlend();
            guiGraphics.pose().popPose();
        }
    }

    protected boolean shouldRenderDisabledOverlayOver(Slot slot) {
        return slot instanceof DisabledSlot
              || slot instanceof PickupOnlySlot && !slot.hasItem()
              || (!slot.hasItem() && !slot.mayPickup(getMenu().getPlayer()));
    }
}
