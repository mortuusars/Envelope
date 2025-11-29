package io.github.mortuusars.envelope.util;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;

/**
 * Approximation of hard-light blend mode to calculate tint.<br>
 * Color brighter than 50% will produce brighter image, lower than 50% - darker.<br><br>
 * It's not 1:1, but I don't know of a closer solution.
 * setShaderColor seems to behave slightly differently than anything that I can do in photoshop.
 */
public record TintColor(float r, float g, float b, float a) {
    public static final Codec<TintColor> CODEC = EnvelopeCodecs.HEX_COLOR.xmap(TintColor::of, TintColor::tint);
    public static final StreamCodec<ByteBuf, TintColor> STREAM_CODEC = ByteBufCodecs.INT.map(TintColor::of, TintColor::tint);

    public static TintColor of(int argb) {
        float a = (float) (FastColor.ARGB32.alpha(argb) - 127) / 127 + 1;
        float r = (float) (FastColor.ARGB32.red(argb) - 127) / 127 + 1;
        float g = (float) (FastColor.ARGB32.green(argb) - 127) / 127 + 1;
        float b = (float) (FastColor.ARGB32.blue(argb) - 127) / 127 + 1;
        return new TintColor(r, g, b, a);
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
