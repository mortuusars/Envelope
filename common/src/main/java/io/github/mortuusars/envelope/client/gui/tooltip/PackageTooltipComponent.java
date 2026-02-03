package io.github.mortuusars.envelope.client.gui.tooltip;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.item.component.PackageContents;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public class PackageTooltipComponent implements ClientTooltipComponent {
    public static final ResourceLocation BACKGROUND_SPRITE = Envelope.resource("tooltip/package/background");
    protected static final int BG_WIDTH = 58;
    protected static final int BG_HEIGHT = 40;

    protected final PackageContents packageContents;

    public PackageTooltipComponent(PackageContents packageContents) {
        this.packageContents = packageContents;
    }

    @Override
    public int getWidth(Font font) {
        return BG_WIDTH;
    }

    @Override
    public int getHeight() {
        return BG_HEIGHT + 3;
    }

    @Override
    public void renderImage(Font font, int x, int y, GuiGraphics guiGraphics) {
        guiGraphics.blitSprite(BACKGROUND_SPRITE, x, y, BG_WIDTH, BG_HEIGHT);

        for (int row = 0; row < 2; row++) {
            for (int column = 0; column < 3; column++) {
                int slotIndex = (column + row * 3);
                int slotX = x + column * 18 + 3;
                int slotY = y + row * 18 + 3;
                ItemStack stack = packageContents.getItemForReading(slotIndex);
                guiGraphics.renderItem(stack, slotX, slotY, slotIndex);
                guiGraphics.renderItemDecorations(font, stack, slotX, slotY);
            }
        }
    }
}
