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

public record ArrivedRecord(Address address, long timestamp) implements DeliveryRecord {
    public static final MapCodec<ArrivedRecord> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
          Address.CODEC.fieldOf("address").forGetter(ArrivedRecord::address),
          Codec.LONG.fieldOf("timestamp").forGetter(ArrivedRecord::timestamp)
    ).apply(i, ArrivedRecord::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, ArrivedRecord> STREAM_CODEC = StreamCodec.composite(
          Address.STREAM_CODEC, ArrivedRecord::address,
          ByteBufCodecs.VAR_LONG, ArrivedRecord::timestamp,
          ArrivedRecord::new
    );

    @Override
    public Type getType() {
        return Type.ARRIVED;
    }

    @Override
    public MutableComponent getDisplayComponent() {
        Component address = this.address.format()
              .withIcon()
              .withIconColor(Colors.ADDRESS_RECIPIENT)
              .withColor(Colors.ADDRESS_RECIPIENT)
              .toComponent();

        return Component.translatable("gui.envelope.delivery_log.record.arrived", address, getElapsedTime(timestamp).orElse(Component.empty()));
    }
}
