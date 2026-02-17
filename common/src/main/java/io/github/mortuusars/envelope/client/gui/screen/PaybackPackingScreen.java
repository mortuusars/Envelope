package io.github.mortuusars.envelope.client.gui.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.client.gui.Sprites;
import io.github.mortuusars.envelope.client.util.Minecrft;
import io.github.mortuusars.envelope.world.inventory.PaybackPackingMenu;
import io.github.mortuusars.envelope.world.inventory.slot.PreviewSlot;
import io.github.mortuusars.envelope.world.inventory.slot.RequestedItemSlot;
import io.github.mortuusars.envelope.world.item.PaybackPackageItem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class PaybackPackingScreen extends AbstractInHandContainerScreen<PaybackPackingMenu> {
    public static final ResourceLocation TEXTURE = Envelope.resource("textures/gui/payback_packing.png");
    public static final WidgetSprites PACK_BUTTON_SPRITES = Sprites.threeStates(Envelope.resource("packing/payback_pack_button"));

    protected ImageButton packButton;

    public PaybackPackingScreen(PaybackPackingMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, TEXTURE);
    }

    @Override
    protected void init() {
        imageWidth = 176;
        imageHeight = 178;
        super.init();

        packButton = new ImageButton(leftPos + 126, topPos + 40, 26, 20,
              PACK_BUTTON_SPRITES,
              button -> pack(),
              Component.translatable("gui.envelope.package.pack"));
        packButton.setTooltip(Tooltip.create(Component.translatable("gui.envelope.package.pack")));
        addRenderableWidget(packButton);
    }

    protected void pack() {
        getMenu().clickMenuButton(getMenu().getPlayer(), PaybackPackingMenu.PACK_BUTTON_ID);
        Minecrft.gameMode().handleInventoryButtonClick(getMenu().containerId, PaybackPackingMenu.PACK_BUTTON_ID);
        onClose();
    }

    @Override
    protected void updateButtons() {
        packButton.active = !getMenu().isPacked();
        packButton.visible = getMenu().canPack();
    }

    @Override
    protected void renderSlot(GuiGraphics guiGraphics, Slot slot) {
        if (slot instanceof RequestedItemSlot requestedItemSlot) {
            if (!slot.hasItem()) {
                ItemStack preview = requestedItemSlot.getIngredient().getRollingDisplayedStack();
                guiGraphics.pose().pushPose();
                guiGraphics.pose().translate(0, 0, -100);
                guiGraphics.renderItem(preview, slot.x, slot.y);
                guiGraphics.renderItemDecorations(Minecrft.get().font, preview, slot.x, slot.y);
                if (requestedItemSlot.getIngredient().items().unwrapKey().isPresent()) {
                    guiGraphics.pose().translate(0, 0, 200);
                    guiGraphics.drawString(font, "#", slot.x + 1 + 19 - 2 - font.width("#"), slot.y - 1, 0xFFFFFFFF, true);
                }
                guiGraphics.pose().popPose();
            }

            boolean isFulfilled = requestedItemSlot.getIngredient().test(slot.getItem());

            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(0, 0, 150);
            RenderSystem.enableBlend();
            guiGraphics.blit(TEXTURE, slot.x - 1, slot.y - 1, 176, isFulfilled ? 18 : 0, 18, 18);
            RenderSystem.disableBlend();
            guiGraphics.pose().popPose();
        }

        super.renderSlot(guiGraphics, slot);
    }

    @Override
    protected void renderTooltip(GuiGraphics guiGraphics, int x, int y) {
        if (menu.getCarried().isEmpty() && hoveredSlot instanceof RequestedItemSlot requestedItemSlot && !requestedItemSlot.hasItem()) {
            ItemStack stack = requestedItemSlot.getRequestedItemPreview();

            List<Component> lines = requestedItemSlot.getIngredient().items().unwrapKey()
                  .map(tag -> {
                      List<Component> list = new ArrayList<>(getTooltipFromContainerItem(stack));
                      list.addLast(Component.literal("Accepts Tag:").withStyle(ChatFormatting.GRAY));
                      list.addLast(Component.literal("#" + tag.location()).withStyle(ChatFormatting.GRAY));
                      return list;
                  })
                  .orElseGet(() -> getTooltipFromContainerItem(stack));

            guiGraphics.renderTooltip(this.font, lines, stack.getTooltipImage(), x, y);
        } else {
            super.renderTooltip(guiGraphics, x, y);
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
