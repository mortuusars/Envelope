package io.github.mortuusars.envelope.client.renderer;

import io.github.mortuusars.envelope.world.item.component.seal.Seal;
import io.github.mortuusars.envelope.world.item.component.seal.SealMaterial;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

public class SealRenderer {
    public void render(Seal seal, GuiGraphics guiGraphics, int x, int y, int mouseX, int mouseY, float partialTick) {
        SealMaterial material = seal.material();
        SealMaterial.Colors colors = material.getColors();

        ResourceLocation sealTexture = material.getTexture();
        ResourceLocation impressionTexture = seal.impression().getTexture();

        // Background
        guiGraphics.blit(sealTexture, x, y, 0, 0, 30, 30, 30, 30);

        // Side
        colors.side().setShaderColor();
        guiGraphics.blit(impressionTexture, x + 1, y + 2, 0, 0, 30, 30, 30, 30);
        guiGraphics.blit(impressionTexture, x, y + 2, 0, 0, 30, 30, 30, 30);
        guiGraphics.blit(impressionTexture, x - 1, y + 2, 0, 0, 30, 30, 30, 30);
        guiGraphics.blit(impressionTexture, x + 1, y + 1, 0, 0, 30, 30, 30, 30);
        guiGraphics.blit(impressionTexture, x, y + 1, 0, 0, 30, 30, 30, 30);
        guiGraphics.blit(impressionTexture, x - 1, y + 1, 0, 0, 30, 30, 30, 30);
        guiGraphics.blit(impressionTexture, x + 1, y, 0, 0, 30, 30, 30, 30);
        guiGraphics.blit(impressionTexture, x, y, 0, 0, 30, 30, 30, 30);
        guiGraphics.blit(impressionTexture, x - 1, y, 0, 0, 30, 30, 30, 30);
        guiGraphics.blit(impressionTexture, x + 1, y - 1, 0, 0, 30, 30, 30, 30);
        guiGraphics.blit(impressionTexture, x, y - 1, 0, 0, 30, 30, 30, 30);
        guiGraphics.blit(impressionTexture, x - 1, y - 1, 0, 0, 30, 30, 30, 30);
        guiGraphics.blit(impressionTexture, x, y - 2, 0, 0, 30, 30, 30, 30);

        // Shadow
        colors.shadow().setShaderColor();
        guiGraphics.blit(impressionTexture, x, y + 1, 0, 0, 30, 30, 30, 30);

        // Highlight
        colors.highlight().setShaderColor();
        guiGraphics.blit(impressionTexture, x, y - 1, 0, 0, 30, 30, 30, 30);

        // Base
        colors.base().setShaderColor();
        guiGraphics.blit(impressionTexture, x, y, 0, 0, 30, 30, 30, 30);
    }
}
