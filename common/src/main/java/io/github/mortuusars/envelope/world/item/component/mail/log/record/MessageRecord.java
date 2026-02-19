package io.github.mortuusars.envelope.world.item.component.mail.log.record;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.mortuusars.envelope.world.item.component.mail.log.DeliveryRecord;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.codec.StreamCodec;

public record MessageRecord(Component message) implements DeliveryRecord {
    public static final MapCodec<MessageRecord> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
          ComponentSerialization.CODEC.fieldOf("message").forGetter(MessageRecord::message)
    ).apply(i, MessageRecord::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, MessageRecord> STREAM_CODEC = StreamCodec.composite(
          ComponentSerialization.STREAM_CODEC, MessageRecord::message,
          MessageRecord::new
    );

    @Override
    public Type getType() {
        return Type.MESSAGE;
    }

    @Override
    public MutableComponent getDisplayComponent() {
        return Component.translatable("gui.envelope.delivery_log.record.message", message);
    }
}
