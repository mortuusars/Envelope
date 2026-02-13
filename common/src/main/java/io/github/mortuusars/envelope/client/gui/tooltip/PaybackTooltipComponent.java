package io.github.mortuusars.envelope.client.gui.tooltip;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.inventory.StackIngredient;
import io.github.mortuusars.envelope.world.item.component.PaybackRequest;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public record PaybackTooltipComponent(PaybackRequest paybackRequest) implements ClientTooltipComponent {
    public static final ResourceLocation SLOT_SPRITE = Envelope.resource("tooltip/payback/slot");

    private static final ItemStack PAYBACK_TAG = new ItemStack(Envelope.Items.PAYBACK_TAG.get());

    @Override
    public int getWidth(Font font) {
        return 16 + (18 * paybackRequest().items().size()) + 5;
    }

    @Override
    public int getHeight() {
        return 23;
    }

    @Override
    public void renderImage(Font font, int x, int y, GuiGraphics guiGraphics) {
        guiGraphics.renderFakeItem(PAYBACK_TAG, x - 1, y + 3, 0);

        int slots = paybackRequest().items().size();

        for (int i = 0; i < slots; i++) {
            StackIngredient ingredient = paybackRequest().items().get(i);
            ItemStack stack = ingredient.getRollingDisplayedStack();

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

            if (ingredient.items().unwrapKey().isPresent()) {
                guiGraphics.pose().translate(0, 0, 200);
                guiGraphics.drawString(font, "#", slotX + 1 + 19 - 2 - font.width("#"), y + 1, 0xFFFFFFFF, true);
            }
        }
    }
}
