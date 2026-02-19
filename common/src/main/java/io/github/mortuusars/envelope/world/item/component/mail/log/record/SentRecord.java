package io.github.mortuusars.envelope.world.item.component.mail.log.record;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.mortuusars.envelope.util.Colors;
import io.github.mortuusars.envelope.world.item.component.mail.log.DeliveryRecord;
import io.github.mortuusars.envelope.world.mail.address.Address;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record SentRecord(Address address, long timestamp) implements DeliveryRecord {
    public static final MapCodec<SentRecord> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
          Address.CODEC.fieldOf("address").forGetter(SentRecord::address),
          Codec.LONG.fieldOf("timestamp").forGetter(SentRecord::timestamp)
    ).apply(i, SentRecord::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, SentRecord> STREAM_CODEC = StreamCodec.composite(
          Address.STREAM_CODEC, SentRecord::address,
          ByteBufCodecs.VAR_LONG, SentRecord::timestamp,
          SentRecord::new
    );

    @Override
    public Type getType() {
        return Type.SENT;
    }

    @Override
    public MutableComponent getDisplayComponent() {
        Component address = this.address.format()
              .withIcon()
              .withIconColor(Colors.ADDRESS_SENDER)
              .withColor(Colors.ADDRESS_SENDER)
              .toComponent();

        return Component.translatable("gui.envelope.delivery_log.record.sent", address, getElapsedTime(timestamp).orElse(Component.empty()));
    }
}
