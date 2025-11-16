package io.github.mortuusars.envelope.world.delivery.log;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.mortuusars.envelope.util.PrettyGameTime;
import io.github.mortuusars.envelope.world.mail.address.Address;
import io.github.mortuusars.envelope.world.mail.address.AddressFormatter;
import io.netty.buffer.ByteBuf;
import net.minecraft.ChatFormatting;
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

public record DeliveryRecord(Status status, Address address, Optional<Long> timestamp, Optional<Component> message) {
    public static final Codec<DeliveryRecord> CODEC = RecordCodecBuilder.create(instance -> instance.group(
          Status.CODEC.fieldOf("status").forGetter(DeliveryRecord::status),
          Address.CODEC.fieldOf("address").forGetter(DeliveryRecord::address),
          Codec.LONG.optionalFieldOf("timestamp").forGetter(DeliveryRecord::timestamp),
          ComponentSerialization.CODEC.optionalFieldOf("message").forGetter(DeliveryRecord::message)
    ).apply(instance, DeliveryRecord::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, DeliveryRecord> STREAM_CODEC = StreamCodec.composite(
          Status.STREAM_CODEC, DeliveryRecord::status,
          Address.STREAM_CODEC, DeliveryRecord::address,
          ByteBufCodecs.optional(ByteBufCodecs.VAR_LONG), DeliveryRecord::timestamp,
          ByteBufCodecs.optional(ComponentSerialization.STREAM_CODEC), DeliveryRecord::message,
          DeliveryRecord::new
    );

    public DeliveryRecord(Status status, Address address) {
        this(status, address, Optional.empty(), Optional.empty());
    }

    public DeliveryRecord atTime(long timestamp) {
        return new DeliveryRecord(status, address, Optional.of(timestamp), message);
    }

    public DeliveryRecord withMessage(Component message) {
        return new DeliveryRecord(status, address, timestamp, Optional.of(message));
    }

    public DeliveryRecord withMessage(Optional<Component> message) {
        return new DeliveryRecord(status, address, timestamp, message);
    }

    public DeliveryRecord withOperatorMessage(Optional<Component> operator) {
        return new DeliveryRecord(status, address, timestamp,
              operator.map(name -> Component.translatable("gui.envelope.delivery.log.record.message.by", name)));
    }

    // --

    public static DeliveryRecord sentFrom(Address address) {
        return new DeliveryRecord(Status.SENT, address);
    }

    public static DeliveryRecord arrivedTo(Address address) {
        return new DeliveryRecord(Status.ARRIVED, address);
    }

    public static DeliveryRecord returnedFrom(Address address) {
        return new DeliveryRecord(Status.RETURNED, address);
    }

    public static DeliveryRecord returned_recipientNotFound() {
        return new DeliveryRecord(Status.RETURNED, Address.MAIL_SERVICE, Optional.empty(),
              Optional.of(Component.translatable("gui.envelope.delivery.log.message.recipient_not_found")));
    }

    public static DeliveryRecord returned_unableToReach() {
        return new DeliveryRecord(Status.RETURNED, Address.MAIL_SERVICE, Optional.empty(),
              Optional.of(Component.translatable("gui.envelope.delivery.log.message.unable_to_reach")));
    }

    // --

    public MutableComponent translate(long gameTime) {
        MutableComponent component = Component.translatable("gui.envelope.delivery.log.record." + status().getSerializedName())
              .append(" ")
              .append(address.format()
                    .withIcon()
                    .withIconColor(AddressFormatter.NEUTRAL_COLOR)
                    .withColor(ChatFormatting.WHITE)
                    .toComponent());

        message.ifPresent(msg -> {
            component.append(" ");
            component.append(msg);
        });

        timestamp.ifPresent(time -> {
            component.append(" ");
            component.append(Component.translatable("gui.envelope.time.elapsed",
                  PrettyGameTime.durationLargest(gameTime - time)).withStyle(ChatFormatting.GRAY));
        });

        return component;
    }

    // --

    public enum Status implements StringRepresentable {
        SENT("sent", false),
        ARRIVED("arrived", false),
        RETURNED("returned", true),
        REJECTED("rejected", true),
        UNCLAIMED("unclaimed", true);

        public static final Codec<Status> CODEC = StringRepresentable.fromEnum(Status::values);
        public static final StreamCodec<ByteBuf, Status> STREAM_CODEC = ByteBufCodecs.idMapper(
              ByIdMap.continuous(Status::ordinal, Status.values(), ByIdMap.OutOfBoundsStrategy.ZERO), Status::ordinal);

        private final String name;
        private final boolean isException;

        Status(String name, boolean isException) {
            this.name = name;
            this.isException = isException;
        }

        @Override
        public @NotNull String getSerializedName() {
            return name;
        }

        public boolean isException() {
            return isException;
        }

        public MutableComponent translate() {
            return Component.translatable("gui.envelope.delivery.log.status." + getSerializedName());
        }
    }
}
