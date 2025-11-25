package io.github.mortuusars.envelope.util;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.util.FastColor;

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
}
