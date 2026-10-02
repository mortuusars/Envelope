package io.github.mortuusars.envelope.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.item.component.seal.*;
import io.github.mortuusars.mortaar.client.Minecrft;
import io.github.mortuusars.mortaar.util.color.TintColor;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class SealRenderer {
    public static final ResourceLocation IRON_DIE_SPRITE = Envelope.resource("seal/die/iron");
    public static final ResourceLocation SEAL_GLINT_SPRITE = Envelope.resource("seal/glint");
    public static final ResourceLocation LOCK_SPRITE = Envelope.resource("seal/lock");
    public static final ResourceLocation LOCKED_OVERLAY_SPRITE = Envelope.resource("seal/locked_overlay");

    public void render(Seal seal, GuiGraphics guiGraphics, int x, int y) {
        SealMaterial material = seal.material().value();
        ShadingPalette colors = material.impressionPalette();

        ResourceLocation materialTexture = material.sprite();
        ResourceLocation impressionTexture = seal.impression().value().sprite();

        // Background
        guiGraphics.blitSprite(materialTexture, x, y, 30, 30);

        // Side
        setShaderTintColor(colors.side());
        guiGraphics.blitSprite(impressionTexture, x + 1, y + 2, 30, 30);
        guiGraphics.blitSprite(impressionTexture, x, y + 2, 30, 30);
        guiGraphics.blitSprite(impressionTexture, x - 1, y + 2, 30, 30);
        guiGraphics.blitSprite(impressionTexture, x + 1, y + 1, 30, 30);
        guiGraphics.blitSprite(impressionTexture, x, y + 1, 30, 30);
        guiGraphics.blitSprite(impressionTexture, x - 1, y + 1, 30, 30);
        guiGraphics.blitSprite(impressionTexture, x + 1, y, 30, 30);
        guiGraphics.blitSprite(impressionTexture, x, y, 30, 30);
        guiGraphics.blitSprite(impressionTexture, x - 1, y, 30, 30);
        guiGraphics.blitSprite(impressionTexture, x + 1, y - 1, 30, 30);
        guiGraphics.blitSprite(impressionTexture, x, y - 1, 30, 30);
        guiGraphics.blitSprite(impressionTexture, x - 1, y - 1, 30, 30);
        guiGraphics.blitSprite(impressionTexture, x, y - 2, 30, 30);

        // Shadow
        setShaderTintColor(colors.shadow());
        guiGraphics.blitSprite(impressionTexture, x, y + 1, 30, 30);

        // Highlight
        setShaderTintColor(colors.highlight());
        guiGraphics.blitSprite(impressionTexture, x, y - 1, 30, 30);

        // Base
        setShaderTintColor(colors.base());
        guiGraphics.blitSprite(impressionTexture, x, y, 30, 30);

        RenderSystem.setShaderColor(1, 1, 1, 1);

        if (material.hasGlint()) {
            RenderSystem.enableBlend();
            guiGraphics.blitSprite(SEAL_GLINT_SPRITE, 32, 32, 0, 0, x, y, 32, 32);
            RenderSystem.disableBlend();
        }

        seal.lock().ifPresent(lock -> {
            if (lock.isLocked(Minecrft.level())) {
                // Heartbeat pulsing effect:
                double time = (Minecrft.level().getGameTime() + Minecrft.get().getTimer().getGameTimeDeltaPartialTick(true)) / 20.0;
                double beat = Math.pow((Math.sin(time * Math.PI * 2 * 1) + 1.0) * 0.5, 10);
                float brightness = (float) Mth.lerp(beat, 1.0, 1.4);
                RenderSystem.setShaderColor(brightness, brightness, brightness, 1.0f);

                RenderSystem.enableBlend();
                guiGraphics.blitSprite(LOCKED_OVERLAY_SPRITE, 34, 34,
                      0, 0, x - 2, y - 2, 34, 34);
                RenderSystem.disableBlend();

                guiGraphics.blitSprite(LOCK_SPRITE, 34, 34,
                      0, 0, x - 2, y - 2, 34, 34);
                RenderSystem.setShaderColor(1, 1, 1, 1);
            }
        });
    }

    public void renderDie(SealSymbol impression, ShadingPalette colors, GuiGraphics guiGraphics, int x, int y) {
        // Background
        guiGraphics.blitSprite(IRON_DIE_SPRITE, x, y, 30, 30);

        ResourceLocation impressionSprite = impression.sprite();

        // flipping the sprite on the X axis is a bit tricky, hopefully it'll not break

        // Side
        setShaderTintColor(colors.side());
        guiGraphics.blitSprite(impressionSprite, -30, 30,  -30, 0, x + 1, y + 1,  30, 30);
        guiGraphics.blitSprite(impressionSprite, -30, 30,  -30, 0, x, y + 1, 30, 30);
        guiGraphics.blitSprite(impressionSprite, -30, 30,  -30, 0, x - 1, y + 1, 30, 30);
        guiGraphics.blitSprite(impressionSprite, -30, 30,  -30, 0, x + 1, y, 30, 30);
        guiGraphics.blitSprite(impressionSprite, -30, 30,  -30, 0, x, y, 30, 30);
        guiGraphics.blitSprite(impressionSprite, -30, 30,  -30, 0, x - 1, y, 30, 30);
        guiGraphics.blitSprite(impressionSprite, -30, 30,  -30, 0, x + 1, y - 1, 30, 30);
        guiGraphics.blitSprite(impressionSprite, -30, 30,  -30, 0, x, y - 1, 30, 30);
        guiGraphics.blitSprite(impressionSprite, -30, 30,  -30, 0, x - 1, y - 1, 30, 30);
        guiGraphics.blitSprite(impressionSprite, -30, 30,  -30, 0, x + 1, y - 2, 30, 30);
        guiGraphics.blitSprite(impressionSprite, -30, 30,  -30, 0, x, y - 2, 30, 30);
        guiGraphics.blitSprite(impressionSprite, -30, 30,  -30, 0, x - 1, y - 2, 30, 30);

        // Highlight
        setShaderTintColor(colors.highlight());
        guiGraphics.blitSprite(impressionSprite, -30, 30,  -30, 0,  x, y + 1, 30, 30);

        // Shadow
        setShaderTintColor(colors.shadow());
        guiGraphics.blitSprite(impressionSprite, -30, 30,  -30, 0,  x, y - 1, 30, 30);

        // Base
        setShaderTintColor(colors.base());
        guiGraphics.blitSprite(impressionSprite, -30, 30,  -30, 0,  x, y, 30, 30);

        RenderSystem.setShaderColor(1, 1, 1, 1);
    }

    public static void setShaderTintColor(TintColor color) {
        RenderSystem.setShaderColor(color.r(), color.g(), color.b(), color.a());
    }

    // Have you seen someone rendering textures this way? Now you have.
    // Patent Pending ™
}
