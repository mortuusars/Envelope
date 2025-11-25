package io.github.mortuusars.envelope.util;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;

/**
 * Approximate simulation of hard-light blend mode to calculate tint for seals.<br><br>
 * It's not 1:1, but I don't know of a closer solution.
 * setShaderColor seems to behave slightly differently than anything that I can do in photoshop.
 */
public record HardLightColor(float r, float g, float b, float a) {
    public static HardLightColor of(int argb) {
        float a = (float) (FastColor.ARGB32.alpha(argb) - 127) / 127 + 1;
        float r = (float) (FastColor.ARGB32.red(argb) - 127) / 127 + 1;
        float g = (float) (FastColor.ARGB32.green(argb) - 127) / 127 + 1;
        float b = (float) (FastColor.ARGB32.blue(argb) - 127) / 127 + 1;
        return new HardLightColor(r, g, b, a);
    }

    public void setShaderColor() {
        RenderSystem.setShaderColor(r, g ,b, a);
    }

    public int tint(int argb) {
        int a = (int)Mth.clamp(FastColor.ARGB32.alpha(argb) * this.a, 0, 255);
        int r = (int)Mth.clamp(FastColor.ARGB32.red(argb) * this.r, 0, 255);
        int g = (int)Mth.clamp(FastColor.ARGB32.green(argb) * this.g, 0, 255);
        int b = (int)Mth.clamp(FastColor.ARGB32.blue(argb) * this.b, 0, 255);
        return FastColor.ARGB32.color(a, r, g, b);
    }

    public int tint() {
        return tint(0xFF7F7F7F);
    }
}
