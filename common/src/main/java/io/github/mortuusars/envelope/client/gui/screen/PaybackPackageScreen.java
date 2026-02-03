package io.github.mortuusars.envelope.client.gui.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.client.util.Minecrft;
import io.github.mortuusars.envelope.world.GameTime;
import io.github.mortuusars.envelope.world.inventory.PaybackPackageMenu;
import io.github.mortuusars.envelope.world.inventory.slot.DisabledSlot;
import io.github.mortuusars.envelope.world.inventory.slot.PreviewSlot;
import io.github.mortuusars.envelope.world.item.component.mail.log.DeliveryRecord;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class PaybackPackageScreen extends AbstractContainerScreen<PaybackPackageMenu> {
    public static final ResourceLocation TEXTURE = Envelope.resource("textures/gui/payback_packing.png");

    public PaybackPackageScreen(PaybackPackageMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected void init() {
        imageWidth = 176;
        imageHeight = 178;
        super.init();
        inventoryLabelY = imageHeight - 94;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);

        if (getMenu().isDestroyedOnClose()) {
            guiGraphics.blit(TEXTURE, leftPos + 45, topPos + 17, 0, 178, 86, 64);
        }
    }

    @Override
    protected void renderSlot(GuiGraphics guiGraphics, Slot slot) {
        super.renderSlot(guiGraphics, slot);

        if (slot instanceof DisabledSlot || (!slot.allowModification(Minecrft.player()) && !slot.hasItem())) {
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(0, 0, 300);
            RenderSystem.enableBlend();
            guiGraphics.blit(TEXTURE, slot.x - 1, slot.y - 1, 176, 0, 18, 18);
            RenderSystem.disableBlend();
            guiGraphics.pose().popPose();
        }

        if (slot instanceof PreviewSlot) {
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(0, 0, 300);
            RenderSystem.enableBlend();
            guiGraphics.blit(TEXTURE, slot.x - 1, slot.y - 1, 176, 18, 18, 18);
            RenderSystem.disableBlend();
            guiGraphics.pose().popPose();
        }
    }

    @Override
    protected @NotNull List<Component> getTooltipFromContainerItem(ItemStack stack) {
        List<Component> components = super.getTooltipFromContainerItem(stack);

        if (hoveredSlot instanceof PreviewSlot) {
            components.add(Component.literal("⌛ ")
                  .append(GameTime.formatLargest(
                        getMenu().getPaybackSubject().timeoutTick() - Minecrft.level().getGameTime(), false))
                  .withStyle(DeliveryRecord.MessageType.NEGATIVE.getStyle()));
        }

        return components;
    }
}
