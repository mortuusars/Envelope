package io.github.mortuusars.envelope.client.gui.tooltip;

import com.mojang.blaze3d.systems.RenderSystem;
import io.github.mortuusars.envelope.EnvelopeClient;
import io.github.mortuusars.envelope.world.item.component.seal.Seal;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import org.joml.Matrix4f;

public class ClientSealTooltip implements ClientTooltipComponent {
    protected final Seal seal;

    public ClientSealTooltip(Seal seal) {
        this.seal = seal;
    }

    @Override
    public int getWidth(Font font) {
        return 80;
    }

    @Override
    public int getHeight() {
        return 32;
    }

    @Override
    public void renderImage(Font font, int x, int y, GuiGraphics guiGraphics) {
        EnvelopeClient.getSealRenderer().render(seal, guiGraphics, x, y, 0, 0, 0);
    }

    @Override
    public void renderText(Font font, int x, int y, Matrix4f matrix, MultiBufferSource.BufferSource bufferSource) {
        // Signature:
        seal.material().getColors().side().setShaderColor();
        x += 34;
        y += 11;
        int color = seal.material().getColors().highlight().tint();
        int outlineColor = seal.material().getColors().shadow().tint();
        font.drawInBatch(seal.signature(), x - 1, y, outlineColor, false, matrix, bufferSource, Font.DisplayMode.NORMAL, 0x00000000, LightTexture.FULL_BRIGHT);
        font.drawInBatch(seal.signature(), x - 1, y - 1, outlineColor, false, matrix, bufferSource, Font.DisplayMode.NORMAL, 0x00000000, LightTexture.FULL_BRIGHT);
        font.drawInBatch(seal.signature(), x, y - 1, outlineColor, false, matrix, bufferSource, Font.DisplayMode.NORMAL, 0x00000000, LightTexture.FULL_BRIGHT);
        font.drawInBatch(seal.signature(), x + 1, y - 1, outlineColor, false, matrix, bufferSource, Font.DisplayMode.NORMAL, 0x00000000, LightTexture.FULL_BRIGHT);
        font.drawInBatch(seal.signature(), x + 1, y, outlineColor, false, matrix, bufferSource, Font.DisplayMode.NORMAL, 0x00000000, LightTexture.FULL_BRIGHT);
        font.drawInBatch(seal.signature(), x + 1, y + 1, outlineColor, false, matrix, bufferSource, Font.DisplayMode.NORMAL, 0x00000000, LightTexture.FULL_BRIGHT);
        font.drawInBatch(seal.signature(), x, y + 1, outlineColor, false, matrix, bufferSource, Font.DisplayMode.NORMAL, 0x00000000, LightTexture.FULL_BRIGHT);
        font.drawInBatch(seal.signature(), x - 1, y + 1, outlineColor, false, matrix, bufferSource, Font.DisplayMode.NORMAL, 0x00000000, LightTexture.FULL_BRIGHT);
        seal.material().getColors().highlight().setShaderColor();
        font.drawInBatch(seal.signature(), x, y, color, false, matrix, bufferSource, Font.DisplayMode.NORMAL, 0x00000000, LightTexture.FULL_BRIGHT);

        RenderSystem.setShaderColor(1, 1, 1, 1);
    }
}
