package io.github.mortuusars.envelope.api.mail.log;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.mortuusars.envelope.api.mail.Address;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public record TravelingRecord(Status status, Address address, long timestamp, int duration,
                              Optional<Component> operator, Optional<Component> message) {
    public static final Codec<TravelingRecord> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Status.CODEC.optionalFieldOf("status", Status.SENT).forGetter(TravelingRecord::status),
            Address.CODEC.fieldOf("address").forGetter(TravelingRecord::address),
            Codec.LONG.fieldOf("timestamp").forGetter(TravelingRecord::timestamp),
            Codec.INT.optionalFieldOf("duration", 0).forGetter(TravelingRecord::duration),
            ComponentSerialization.CODEC.optionalFieldOf("operator").forGetter(TravelingRecord::operator),
            ComponentSerialization.CODEC.optionalFieldOf("message").forGetter(TravelingRecord::message)
    ).apply(instance, TravelingRecord::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, TravelingRecord> STREAM_CODEC = StreamCodec.composite(
            Status.STREAM_CODEC, TravelingRecord::status,
            Address.STREAM_CODEC, TravelingRecord::address,
            ByteBufCodecs.VAR_LONG, TravelingRecord::timestamp,
            ByteBufCodecs.VAR_INT, TravelingRecord::duration,
            ByteBufCodecs.optional(ComponentSerialization.STREAM_CODEC), TravelingRecord::operator,
            ByteBufCodecs.optional(ComponentSerialization.STREAM_CODEC), TravelingRecord::message,
            TravelingRecord::new
    );

    // --

    public static TravelingRecord sentFrom(Address address, long timestamp, Optional<Component> operator) {
        return new TravelingRecord(Status.SENT, address, timestamp, 0, operator, Optional.empty());
    }

    public static TravelingRecord travelingTo(Address address, long timestamp, int duration) {
        return new TravelingRecord(Status.TRAVELING, address, timestamp, duration, Optional.empty(), Optional.empty());
    }

    public static TravelingRecord returned(Address recipientAddress, long timestamp, Optional<Component> operator, Optional<Component> message) {
        return new TravelingRecord(Status.RETURNED, recipientAddress, timestamp, 0, operator, message);
    }

    public static TravelingRecord arrivedTo(Address address, long timestamp) {
        return new TravelingRecord(Status.ARRIVED, address, timestamp, 0, Optional.empty(), Optional.empty());
    }

    public static TravelingRecord receivedAt(Address address, long timestamp, Optional<Component> operator) {
        return new TravelingRecord(Status.RECEIVED, address, timestamp, 0, operator, Optional.empty());
    }

    // --

    public MutableComponent translate() {
        MutableComponent component = Component.translatable("gui.envelope.mail.log.record." + status.name, address.getDisplayName());
        operator.ifPresent(o -> component.append(Component.translatable("gui.envelope.mail.log.record.by", o)));
        return component;
    }

    // --

    public enum Status implements StringRepresentable {
        SENT("sent"),
        TRAVELING("traveling"),
        ARRIVED("arrived"),
        RETURNED("returned"),
        RECEIVED("received");

        public static final Codec<Status> CODEC = StringRepresentable.fromEnum(Status::values);
        public static final StreamCodec<ByteBuf, Status> STREAM_CODEC =
                ByteBufCodecs.idMapper(ByIdMap.continuous(Status::ordinal, values(), ByIdMap.OutOfBoundsStrategy.ZERO), Status::ordinal);

        private final String name;

        Status(String name) {
            this.name = name;
        }

        @Override
        public @NotNull String getSerializedName() {
            return name;
        }

        public MutableComponent translate() {
            return Component.translatable("gui.envelope.mail.status." + name);
        }
    }
}
