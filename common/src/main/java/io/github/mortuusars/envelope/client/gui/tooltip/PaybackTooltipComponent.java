package io.github.mortuusars.envelope.client.gui.tooltip;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.client.gui.RequestedItemDisplay;
import io.github.mortuusars.envelope.world.inventory.RequestedItem;
import io.github.mortuusars.envelope.world.item.component.RequestedPayback;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public record PaybackTooltipComponent(RequestedPayback requestedPayback) implements ClientTooltipComponent {
    public static final ResourceLocation SLOT_SPRITE = Envelope.resource("tooltip/payback/slot");

    private static final ItemStack PAYBACK_TAG = new ItemStack(Envelope.Items.PAYBACK_TAG.get());

    @Override
    public int getWidth(Font font) {
        return 16 + (18 * requestedPayback().items().size()) + 5;
    }

    @Override
    public int getHeight() {
        return 23;
    }

    @Override
    public void renderImage(Font font, int x, int y, GuiGraphics guiGraphics) {
        guiGraphics.renderFakeItem(PAYBACK_TAG, x - 1, y + 3, 0);

        int slots = requestedPayback().items().size();

        for (int i = 0; i < slots; i++) {
            RequestedItem requestedItem = requestedPayback().items().get(i);
            RequestedItemDisplay display = new RequestedItemDisplay(requestedItem);
            ItemStack stack = display.getDisplayedItem();

            int slotX = x + 17 + 2 + (i * 18);
            int slotY = y + 2;

            if (i == 0) {
                // Left border
                guiGraphics.blitSprite(SLOT_SPRITE, 22, 22, 0, 0, slotX - 2, y, 2, 22);
            }

            if (i == slots - 1) {
                // Right border
                guiGraphics.blitSprite(SLOT_SPRITE, 22, 22, 20, 0, slotX + 18, y, 2, 22);
            }

            guiGraphics.blitSprite(SLOT_SPRITE, 22, 22, 2, 0, slotX, y, 18, 22);
            guiGraphics.renderFakeItem(stack, slotX + 1, slotY + 1, 0);
            guiGraphics.renderItemDecorations(font, stack, slotX + 1, slotY + 1);

            if (requestedItem.item().left().isPresent()) {
                guiGraphics.pose().translate(0, 0, 200);
                guiGraphics.drawString(font, "#", slotX + 1 + 19 - 2 - font.width("#"), y + 1, 0xFFFFFFFF, true);
            }
        }
    }
}
