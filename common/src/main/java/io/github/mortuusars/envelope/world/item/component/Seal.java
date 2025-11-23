package io.github.mortuusars.envelope.world.item.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;

public record Seal(Component signature) {
    public static final Codec<Seal> CODEC = RecordCodecBuilder.create(i -> i.group(
          ComponentSerialization.CODEC.fieldOf("signature").forGetter(Seal::signature)
    ).apply(i, Seal::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, Seal> STREAM_CODEC = StreamCodec.composite(
          ComponentSerialization.STREAM_CODEC, Seal::signature,
          Seal::new
    );
}
