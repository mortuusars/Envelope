package io.github.mortuusars.envelope.world.item.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record LetterContent(Component text, boolean unfolded) {
    public static final Codec<LetterContent> CODEC = RecordCodecBuilder.create(i -> i.group(
          ComponentSerialization.CODEC.optionalFieldOf("text", CommonComponents.EMPTY).forGetter(LetterContent::text),
          Codec.BOOL.optionalFieldOf("unfolded", false).forGetter(LetterContent::unfolded)
    ).apply(i, LetterContent::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, LetterContent> STREAM_CODEC = StreamCodec.composite(
          ComponentSerialization.STREAM_CODEC, LetterContent::text,
          ByteBufCodecs.BOOL, LetterContent::unfolded,
          LetterContent::new
    );

    public static final LetterContent EMPTY = new LetterContent(CommonComponents.EMPTY);

    public LetterContent(Component text) {
        this(text, false);
    }

    public boolean isEmpty() {
        return this.equals(EMPTY) || text().getString().isEmpty();
    }

    public LetterContent withUnfolded(boolean unfolded) {
        return new LetterContent(text, unfolded);
    }
}