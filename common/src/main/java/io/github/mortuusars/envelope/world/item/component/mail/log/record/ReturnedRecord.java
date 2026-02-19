package io.github.mortuusars.envelope.world.item.component.mail.log.record;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.mortuusars.envelope.world.item.component.mail.log.DeliveryRecord;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.codec.StreamCodec;

public record ReturnedRecord(Component message) implements DeliveryRecord {
    public static final MapCodec<ReturnedRecord> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
          ComponentSerialization.CODEC.fieldOf("message").forGetter(ReturnedRecord::message)
    ).apply(i, ReturnedRecord::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, ReturnedRecord> STREAM_CODEC = StreamCodec.composite(
          ComponentSerialization.STREAM_CODEC, ReturnedRecord::message,
          ReturnedRecord::new
    );

    @Override
    public Type getType() {
        return Type.RETURNED;
    }

    @Override
    public MutableComponent getDisplayComponent() {
        return Component.translatable("gui.envelope.delivery_log.record.returned", message);
    }
}
