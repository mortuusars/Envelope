package io.github.mortuusars.envelope.client.gui.tooltip;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.util.Colors;
import io.github.mortuusars.envelope.world.inventory.StackIngredient;
import io.github.mortuusars.envelope.world.item.component.PaybackDuration;
import io.github.mortuusars.envelope.world.item.component.PaybackRequest;
import net.minecraft.Util;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public record PaybackRequestTooltipComponent(PaybackRequest paybackRequest) implements ClientTooltipComponent {
    public static final ResourceLocation SLOT_SPRITE = Envelope.resource("tooltip/payback/slot");

    private static final ItemStack PAYBACK_TAG_SHORT = Util.make(() -> {
        ItemStack stack = new ItemStack(Envelope.Items.PAYBACK_TAG.get());
        stack.set(Envelope.DataComponents.PAYBACK_TAG_CONTENTS, PaybackRequest.createDefault(PaybackDuration.SHORT));
        return stack;
    });
    private static final ItemStack PAYBACK_TAG_MEDIUM = Util.make(() -> {
        ItemStack stack = new ItemStack(Envelope.Items.PAYBACK_TAG.get());
        stack.set(Envelope.DataComponents.PAYBACK_TAG_CONTENTS, PaybackRequest.createDefault(PaybackDuration.MEDIUM));
        return stack;
    });
    private static final ItemStack PAYBACK_TAG_LONG = Util.make(() -> {
        ItemStack stack = new ItemStack(Envelope.Items.PAYBACK_TAG.get());
        stack.set(Envelope.DataComponents.PAYBACK_TAG_CONTENTS, PaybackRequest.createDefault(PaybackDuration.LONG));
        return stack;
    });

    @Override
    public int getWidth(Font font) {
        return 18 + (18 * paybackRequest().items().size()) + 5;
    }

    @Override
    public int getHeight() {
        return 26;
    }

    @Override
    public void renderImage(Font font, int x, int y, GuiGraphics guiGraphics) {
        ItemStack tagPreview = switch (paybackRequest().duration()) {
            case SHORT -> PAYBACK_TAG_SHORT;
            case MEDIUM -> PAYBACK_TAG_MEDIUM;
            case LONG -> PAYBACK_TAG_LONG;
        };
        guiGraphics.renderFakeItem(tagPreview, x - 1, y + 4, 0);

        int slots = paybackRequest().items().size();

        for (int i = 0; i < slots; i++) {
            StackIngredient ingredient = paybackRequest().items().get(i);
            ItemStack stack = ingredient.getRollingDisplayedStack();

            int slotX = x + 17 + 3 + (i * 18);
            int slotY = y + 3;

            if (i == 0) {
                // Left border
                guiGraphics.blitSprite(SLOT_SPRITE, 24, 24, 0, 0, slotX - 3, y, 3, 24);
            }

            if (i == slots - 1) {
                // Right border
                guiGraphics.blitSprite(SLOT_SPRITE, 24, 24, 21, 0, slotX + 18, y, 3, 24);
            }

            guiGraphics.blitSprite(SLOT_SPRITE, 24, 24, 3, 0, slotX, y, 18, 24);
            guiGraphics.renderFakeItem(stack, slotX + 1, slotY + 1, 0);
            guiGraphics.renderItemDecorations(font, stack, slotX + 1, slotY + 1);

            if (ingredient.items().unwrapKey().isPresent()) {
                guiGraphics.pose().translate(0, 0, 200);
                guiGraphics.drawString(font, "#", slotX + 1 + 19 - 2 - font.width("#"), y + 1, Colors.WHITE, true);
            }
        }
    }
}
