package io.github.mortuusars.envelope.client.gui.tooltip;

import io.github.mortuusars.envelope.EnvelopeClient;
import io.github.mortuusars.envelope.world.item.component.seal.Seal;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import org.joml.Matrix4f;

public class SealTooltip implements ClientTooltipComponent {
    protected final Seal seal;

    public SealTooltip(Seal seal) {
        this.seal = seal;
    }

    @Override
    public int getWidth(Font font) {
        return 35 + font.width(seal.signature());
    }

    @Override
    public int getHeight() {
        return 32;
    }

    @Override
    public void renderImage(Font font, int x, int y, GuiGraphics guiGraphics) {
        EnvelopeClient.getSealRenderer().render(seal, guiGraphics, x, y);
    }

    @Override
    public void renderText(Font font, int x, int y, Matrix4f matrix, MultiBufferSource.BufferSource buffer) {
        // Signature:
        x += 34;
        y += 11;
        int color = seal.material().getImpressionTheme().highlight().tint();
        int outlineColor = seal.material().getImpressionTheme().shadow().tint();
        text(seal.signature(), font, x - 1, y, outlineColor, matrix, buffer);
        text(seal.signature(), font, x - 1, y - 1, outlineColor, matrix, buffer);
        text(seal.signature(), font, x, y - 1, outlineColor, matrix, buffer);
        text(seal.signature(), font, x + 1, y - 1, outlineColor, matrix, buffer);
        text(seal.signature(), font, x + 1, y, outlineColor, matrix, buffer);
        text(seal.signature(), font, x + 1, y + 1, outlineColor, matrix, buffer);
        text(seal.signature(), font, x, y + 1, outlineColor, matrix, buffer);
        text(seal.signature(), font, x - 1, y + 1, outlineColor, matrix, buffer);
        text(seal.signature(), font, x, y, color, matrix, buffer);
    }

    private void text(Component text, Font font, int x, int y, int color, Matrix4f matrix, MultiBufferSource.BufferSource buffer) {
        font.drawInBatch(text, x, y, color, false, matrix, buffer,
              Font.DisplayMode.NORMAL, 0x00000000, LightTexture.FULL_BRIGHT);
    }
}
