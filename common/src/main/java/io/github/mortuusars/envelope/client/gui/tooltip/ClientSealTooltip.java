package io.github.mortuusars.envelope.client.gui.tooltip;

import com.mojang.blaze3d.systems.RenderSystem;
import io.github.mortuusars.envelope.EnvelopeClient;
import io.github.mortuusars.envelope.world.item.component.seal.Seal;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.FastColor;
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
        int color = seal.material().getItemTintColor();
        int outlineColor = FastColor.ARGB32.multiply(color, 0xFF7f7f7f);
        font.drawInBatch(seal.signature(), x + 33 - 1, y + 11, outlineColor, false, matrix, bufferSource, Font.DisplayMode.NORMAL, 0x00000000, LightTexture.FULL_BRIGHT);
        font.drawInBatch(seal.signature(), x + 33 - 1, y + 11 - 1, outlineColor, false, matrix, bufferSource, Font.DisplayMode.NORMAL, 0x00000000, LightTexture.FULL_BRIGHT);
        font.drawInBatch(seal.signature(), x + 33, y + 11 - 1, outlineColor, false, matrix, bufferSource, Font.DisplayMode.NORMAL, 0x00000000, LightTexture.FULL_BRIGHT);
        font.drawInBatch(seal.signature(), x + 33 + 1, y + 11 - 1, outlineColor, false, matrix, bufferSource, Font.DisplayMode.NORMAL, 0x00000000, LightTexture.FULL_BRIGHT);
        font.drawInBatch(seal.signature(), x + 33 + 1, y + 11, outlineColor, false, matrix, bufferSource, Font.DisplayMode.NORMAL, 0x00000000, LightTexture.FULL_BRIGHT);
        font.drawInBatch(seal.signature(), x + 33 + 1, y + 11 + 1, outlineColor, false, matrix, bufferSource, Font.DisplayMode.NORMAL, 0x00000000, LightTexture.FULL_BRIGHT);
        font.drawInBatch(seal.signature(), x + 33, y + 11 + 1, outlineColor, false, matrix, bufferSource, Font.DisplayMode.NORMAL, 0x00000000, LightTexture.FULL_BRIGHT);
        font.drawInBatch(seal.signature(), x + 33 - 1, y + 11 + 1, outlineColor, false, matrix, bufferSource, Font.DisplayMode.NORMAL, 0x00000000, LightTexture.FULL_BRIGHT);
        seal.material().getColors().highlight().setShaderColor();
        font.drawInBatch(seal.signature(), x + 33, y + 11, color, false, matrix, bufferSource, Font.DisplayMode.NORMAL, 0x00000000, LightTexture.FULL_BRIGHT);

        RenderSystem.setShaderColor(1, 1, 1, 1);
    }
}
