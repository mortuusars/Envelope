package io.github.mortuusars.envelope.world.item.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record LetterAndQuillContent(String text) {
    public static final int MAX_LENGTH = 4096;

    public static final Codec<LetterAndQuillContent> CODEC = RecordCodecBuilder.create(i -> i.group(
          Codec.string(0, MAX_LENGTH).optionalFieldOf("text", "").forGetter(LetterAndQuillContent::text)
    ).apply(i, LetterAndQuillContent::new));

    public static final StreamCodec<ByteBuf, LetterAndQuillContent> STREAM_CODEC = StreamCodec.composite(
          ByteBufCodecs.stringUtf8(MAX_LENGTH), LetterAndQuillContent::text,
          LetterAndQuillContent::new
    );

    public static final LetterAndQuillContent EMPTY = new LetterAndQuillContent("");

    public boolean isEmpty() {
        return this.equals(EMPTY);
    }

    public LetterContent toWritten() {
        return new LetterContent(Component.literal(text));
    }
}
