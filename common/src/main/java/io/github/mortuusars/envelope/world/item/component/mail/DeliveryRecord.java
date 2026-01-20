package io.github.mortuusars.envelope.world.item.component.mail;

import com.google.common.base.Preconditions;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.mortuusars.envelope.world.GameTime;
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

    public DeliveryRecord atTime(long timestamp) {
        return new DeliveryRecord(status, address, Optional.of(timestamp), message);
    }

    public DeliveryRecord withMessage(Component message) {
        return new DeliveryRecord(status, address, timestamp, Optional.of(message));
    }

    // --

    public static Builder sentFrom(@NotNull Address address) {
        Preconditions.checkNotNull(address);
        return new Builder(Status.SENT, address);
    }

    public static Builder arrivedTo(@NotNull Address address) {
        Preconditions.checkNotNull(address);
        return new Builder(Status.ARRIVED, address);
    }

    public static Builder returned(@NotNull Address address) {
        Preconditions.checkNotNull(address);
        return new Builder(Status.RETURNED, address);
    }

    // --

    public MutableComponent translate(long gameTime) {
        int addressColor = switch (status()) {
            case SENT -> AddressFormatter.SENDER_COLOR;
            case ARRIVED -> AddressFormatter.RECIPIENT_COLOR;
            case RETURNED, REJECTED, UNCLAIMED -> AddressFormatter.NEUTRAL_COLOR;
        };
        MutableComponent component = Component.translatable("gui.envelope.delivery.log.record." + status().getSerializedName())
              .append(" ")
              .append(address.format()
                    .withIcon()
                    .withIconColor(addressColor)
                    .withColor(addressColor)
                    .withMaxLength(25)
                    .toComponent());

        message.ifPresent(msg -> {
            component.append(" ");
            component.append(msg);
        });

        timestamp.ifPresent(time -> {
            component.append(" ");
            component.append(GameTime.formatLargest(gameTime - time, false)).withStyle(ChatFormatting.DARK_GRAY);
        });

        return component;
    }

    // --

    public static class Builder {
        private final Status status;
        private final Address address;
        private Optional<Long> timestamp = Optional.empty();
        private Optional<Component> message = Optional.empty();

        public Builder(Status status, Address address) {
            this.status = status;
            this.address = address;
        }

        public Builder at(long timestamp) {
            this.timestamp = Optional.of(timestamp);
            return this;
        }

        public Builder message(Component message) {
            this.message = Optional.ofNullable(message);
            return this;
        }

        public DeliveryRecord build() {
            return new DeliveryRecord(status, address, timestamp, message);
        }
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

    public interface Message {
        Component RECIPIENT_NOT_FOUND = Component.translatable("gui.envelope.delivery.log.message.recipient_not_found");
        Component RECIPIENT_INBOX_IS_FULL = Component.translatable("gui.envelope.delivery.log.message.recipient_inbox_is_full");
        Component UNABLE_TO_REACH = Component.translatable("gui.envelope.delivery.log.message.unable_to_reach");

        Component WAITING_FOR_PAYMENT = Component.translatable("gui.envelope.delivery.log.message.waiting_for_payment");
        Component PAYBACK_SUBJECT_NOT_FOUND = Component.translatable("gui.envelope.delivery.log.message.payback_subject_not_found");
        Component PAYBACK_FULFILLED = Component.translatable("gui.envelope.delivery.log.message.payback_fulfilled");
        Component PAYBACK_IS_NOT_VALID = Component.translatable("gui.envelope.delivery.log.message.payback_is_not_valid");
        Component PAYBACK_IS_TIMED_OUT = Component.translatable("gui.envelope.delivery.log.message.payback_is_timed_out");
    }
}
