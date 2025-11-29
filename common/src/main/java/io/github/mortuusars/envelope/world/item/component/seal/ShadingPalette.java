package io.github.mortuusars.envelope.world.item.component.seal;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.mortuusars.envelope.util.TintColor;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;

public record ShadingPalette(TintColor base, TintColor highlight, TintColor shadow, TintColor side) {
    public ShadingPalette(int base, int highlight, int shadow, int side) {
        this(TintColor.of(base), TintColor.of(highlight), TintColor.of(shadow), TintColor.of(side));
    }

    public static final Codec<ShadingPalette> CODEC = RecordCodecBuilder.create(i -> i.group(
          TintColor.CODEC.fieldOf("base").forGetter(ShadingPalette::base),
          TintColor.CODEC.fieldOf("highlight").forGetter(ShadingPalette::highlight),
          TintColor.CODEC.fieldOf("shadow").forGetter(ShadingPalette::shadow),
          TintColor.CODEC.fieldOf("side").forGetter(ShadingPalette::side)
    ).apply(i, ShadingPalette::new));

    public static final StreamCodec<ByteBuf, ShadingPalette> STREAM_CODEC = StreamCodec.composite(
          TintColor.STREAM_CODEC, ShadingPalette::base,
          TintColor.STREAM_CODEC, ShadingPalette::highlight,
          TintColor.STREAM_CODEC, ShadingPalette::shadow,
          TintColor.STREAM_CODEC, ShadingPalette::side,
          ShadingPalette::new
    );

    public static final ShadingPalette IRON_DIE = new ShadingPalette(0xFF9A9CA1, 0xFFDFE0E3, 0xFF5D6068, 0xFFC4C5C8);
}
