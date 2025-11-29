package io.github.mortuusars.envelope.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.item.component.seal.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

public class SealRenderer {
    public static final ResourceLocation IRON_DIE_TEXTURE = Envelope.resource("textures/gui/seal/die/iron.png");

    public void render(Seal seal, GuiGraphics guiGraphics, int x, int y) {
        SealMaterial material = seal.material().value();
        ShadingPalette colors = material.impressionPalette();

        ResourceLocation materialTexture = material.texture();
        ResourceLocation impressionTexture = seal.impression().texture();

        // Background
        guiGraphics.blit(materialTexture, x, y, 0, 0, 30, 30, 30, 30);

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

        RenderSystem.setShaderColor(1, 1, 1, 1);
    }

    public void renderDie(SealImpression impression, ShadingPalette colors, GuiGraphics guiGraphics, int x, int y) {
        ResourceLocation impressionTexture = impression.texture();

        // Background
        guiGraphics.blit(IRON_DIE_TEXTURE, x, y, 0, 0, 30, 30, 30, 30);

        // textureWidth parameter is negative to flip the impression texture on the X axis

        // Side
        colors.side().setShaderColor();
        guiGraphics.blit(impressionTexture, x + 1, y + 1, 0, 0, 30, 30, -30, 30);
        guiGraphics.blit(impressionTexture, x, y + 1, 0, 0, 30, 30, -30, 30);
        guiGraphics.blit(impressionTexture, x - 1, y + 1, 0, 0, 30, 30, -30, 30);
        guiGraphics.blit(impressionTexture, x + 1, y, 0, 0, 30, 30, -30, 30);
        guiGraphics.blit(impressionTexture, x, y, 0, 0, 30, 30, -30, 30);
        guiGraphics.blit(impressionTexture, x - 1, y, 0, 0, 30, 30, -30, 30);
        guiGraphics.blit(impressionTexture, x + 1, y - 1, 0, 0, 30, 30, -30, 30);
        guiGraphics.blit(impressionTexture, x, y - 1, 0, 0, 30, 30, -30, 30);
        guiGraphics.blit(impressionTexture, x - 1, y - 1, 0, 0, 30, 30, -30, 30);
        guiGraphics.blit(impressionTexture, x + 1, y - 2, 0, 0, 30, 30, -30, 30);
        guiGraphics.blit(impressionTexture, x, y - 2, 0, 0, 30, 30, -30, 30);
        guiGraphics.blit(impressionTexture, x - 1, y - 2, 0, 0, 30, 30, -30, 30);

        // Highlight
        colors.highlight().setShaderColor();
        guiGraphics.blit(impressionTexture, x, y + 1, 0, 0, 30, 30, -30, 30);

        // Shadow
        colors.shadow().setShaderColor();
        guiGraphics.blit(impressionTexture, x, y - 1, 0, 0, 30, 30, -30, 30);

        // Base
        colors.base().setShaderColor();
        guiGraphics.blit(impressionTexture, x, y, 0, 0, 30, 30, -30, 30);

        RenderSystem.setShaderColor(1, 1, 1, 1);
    }

    // Have you seen someone rendering textures this way? Now you have.
    // Patent Pending ™
}
