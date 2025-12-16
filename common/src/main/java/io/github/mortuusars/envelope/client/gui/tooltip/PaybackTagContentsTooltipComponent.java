package io.github.mortuusars.envelope.client.gui.tooltip;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.item.component.PaybackTagContents;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public record PaybackTagContentsTooltipComponent(PaybackTagContents contents) implements ClientTooltipComponent {
    public static final ResourceLocation SLOT_SPRITE = Envelope.resource("tooltip/payback/slot");

    @Override
    public int getWidth(Font font) {
        return !contents.isEmpty() ? 18 * contents().items().size() + 4 : 0 ;
    }

    @Override
    public int getHeight() {
        return !contents.isEmpty() ? 24 : 0;
    }

    @Override
    public void renderImage(Font font, int x, int y, GuiGraphics guiGraphics) {
        if (contents.isEmpty()) {
            return;
        }

        List<ItemStack> items = contents().items().stream().filter(i -> !i.isEmpty()).toList();
        int slots = items.size();

        for (int i = 0; i < slots; i++) {
            ItemStack stack = items.get(i);

            int slotX = x + 2 + (i * 18);
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
        }
    }
}
