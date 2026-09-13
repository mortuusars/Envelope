package io.github.mortuusars.envelope.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.item.component.seal.*;
import io.github.mortuusars.mortaar.util.color.TintColor;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

public class SealRenderer {
    public static final ResourceLocation IRON_DIE_TEXTURE = Envelope.resource("textures/seal/die/iron.png");
    public static final ResourceLocation SEAL_GLINT_SPRITE = Envelope.resource("seal_glint.png");

    public void render(Seal seal, GuiGraphics guiGraphics, int x, int y) {
        SealMaterial material = seal.material().value();
        ShadingPalette colors = material.impressionPalette();

        ResourceLocation materialTexture = material.texture();
        ResourceLocation impressionTexture = seal.impression().value().texture();

        // Background
        guiGraphics.blit(materialTexture, x, y, 0, 0, 30, 30, 30, 30);

        // Side
        setShaderTintColor(colors.side());
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
        setShaderTintColor(colors.shadow());
        guiGraphics.blit(impressionTexture, x, y + 1, 0, 0, 30, 30, 30, 30);

        // Highlight
        setShaderTintColor(colors.highlight());
        guiGraphics.blit(impressionTexture, x, y - 1, 0, 0, 30, 30, 30, 30);

        // Base
        setShaderTintColor(colors.base());
        guiGraphics.blit(impressionTexture, x, y, 0, 0, 30, 30, 30, 30);

        RenderSystem.setShaderColor(1, 1, 1, 1);

        if (material.hasGlint()) {
            RenderSystem.enableBlend();
            guiGraphics.blitSprite(Envelope.resource("seal_glint"), 32, 32, 0, 0, x, y, 32, 32);
            RenderSystem.disableBlend();
        }
    }

    public void renderDie(SealSymbol impression, ShadingPalette colors, GuiGraphics guiGraphics, int x, int y) {
        ResourceLocation impressionTexture = impression.texture();

        // Background
        guiGraphics.blit(IRON_DIE_TEXTURE, x, y, 0, 0, 30, 30, 30, 30);

        // textureWidth parameter is negative to flip the impression texture on the X axis

        // Side
        setShaderTintColor(colors.side());
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
        setShaderTintColor(colors.highlight());
        guiGraphics.blit(impressionTexture, x, y + 1, 0, 0, 30, 30, -30, 30);

        // Shadow
        setShaderTintColor(colors.shadow());
        guiGraphics.blit(impressionTexture, x, y - 1, 0, 0, 30, 30, -30, 30);

        // Base
        setShaderTintColor(colors.base());
        guiGraphics.blit(impressionTexture, x, y, 0, 0, 30, 30, -30, 30);

        RenderSystem.setShaderColor(1, 1, 1, 1);
    }

    public static void setShaderTintColor(TintColor color) {
        RenderSystem.setShaderColor(color.r(), color.g(), color.b(), color.a());
    }

    // Have you seen someone rendering textures this way? Now you have.
    // Patent Pending ™
}
