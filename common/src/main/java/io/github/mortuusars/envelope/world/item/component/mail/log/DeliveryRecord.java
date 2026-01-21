package io.github.mortuusars.envelope.world.item.component.mail.log;

import com.google.common.base.Preconditions;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.mortuusars.envelope.world.GameTime;
import io.github.mortuusars.envelope.world.mail.address.Address;
import io.github.mortuusars.envelope.world.mail.address.AddressFormatter;
import io.netty.buffer.ByteBuf;
import net.minecraft.ChatFormatting;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.*;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public record DeliveryRecord(Type type, Optional<Address> address, Optional<Long> timestamp,
                             Optional<Component> message, MessageType messageType) {
    public static final Codec<DeliveryRecord> CODEC = RecordCodecBuilder.create(i -> i.group(
          Type.CODEC.fieldOf("type").forGetter(DeliveryRecord::type),
          Address.CODEC.optionalFieldOf("address").forGetter(DeliveryRecord::address),
          Codec.LONG.optionalFieldOf("timestamp").forGetter(DeliveryRecord::timestamp),
          ComponentSerialization.CODEC.optionalFieldOf("message").forGetter(DeliveryRecord::message),
          MessageType.CODEC.optionalFieldOf("message_type", MessageType.NEUTRAL).forGetter(DeliveryRecord::messageType)
    ).apply(i, DeliveryRecord::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, DeliveryRecord> STREAM_CODEC = StreamCodec.composite(
          Type.STREAM_CODEC, DeliveryRecord::type,
          ByteBufCodecs.optional(Address.STREAM_CODEC), DeliveryRecord::address,
          ByteBufCodecs.optional(ByteBufCodecs.VAR_LONG), DeliveryRecord::timestamp,
          ByteBufCodecs.optional(ComponentSerialization.STREAM_CODEC), DeliveryRecord::message,
          MessageType.STREAM_CODEC, DeliveryRecord::messageType,
          DeliveryRecord::new
    );

    // --

    public static DeliveryRecord sentFrom(@NotNull Address address, long timestamp) {
        Preconditions.checkNotNull(address);
        return new DeliveryRecord(Type.SENT, Optional.of(address), Optional.of(timestamp),
              Optional.empty(), MessageType.NEUTRAL);
    }

    public static DeliveryRecord arrivedTo(@NotNull Address address, long timestamp) {
        Preconditions.checkNotNull(address);
        return new DeliveryRecord(Type.ARRIVED, Optional.of(address), Optional.of(timestamp),
              Optional.empty(), MessageType.NEUTRAL);
    }

    public static DeliveryRecord returned(Component reason) {
        Preconditions.checkNotNull(reason);
        return new DeliveryRecord(Type.RETURNED, Optional.of(Address.MAIL_SERVICE), Optional.empty(),
              Optional.of(reason), MessageType.NEGATIVE);
    }

    public static DeliveryRecord payback(@NotNull Component message, MessageType type) {
        return new DeliveryRecord(Type.PAYBACK, Optional.empty(), Optional.empty(),
              Optional.of(message), type);
    }

    // --

    public MutableComponent toComponent(long gameTime) {
        int addressColor = switch (type()) {
            case SENT -> AddressFormatter.SENDER_COLOR;
            case ARRIVED -> AddressFormatter.RECIPIENT_COLOR;
            case RETURNED, PAYBACK, CUSTOM -> AddressFormatter.NEUTRAL_COLOR;
        };

        Component addressComponent = this.address.map(a -> a.format()
                    .withIcon()
                    .withIconColor(addressColor)
                    .withColor(addressColor)
                    .withMaxLength(25)
                    .toComponent())
              .orElse(Component.empty());

        MutableComponent messageComponent = Component.empty().append(message.orElse(CommonComponents.EMPTY))
              .withStyle(messageType().getStyle());

        Component elapsedTimeComponent = timestamp.map(time -> GameTime.formatLargest(gameTime - time, false)
              .withStyle(ChatFormatting.DARK_GRAY)).orElse(Component.empty());

        return Component.translatable("gui.envelope.delivery_log.record." + this.type().getSerializedName(),
              addressComponent, messageComponent, elapsedTimeComponent).withStyle(ChatFormatting.GRAY);
    }

    // --

    public static class Builder {
        private final Type type;
        private Optional<Address> address = Optional.empty();
        private Optional<Long> timestamp = Optional.empty();
        private Optional<Component> message = Optional.empty();
        private MessageType messageType = MessageType.NEUTRAL;

        public Builder(Type type) {
            this.type = type;
        }

        public Builder address(Address address) {
            this.address = Optional.of(address);
            return this;
        }

        public Builder at(long timestamp) {
            this.timestamp = Optional.of(timestamp);
            return this;
        }

        public Builder message(Component message) {
            this.message = Optional.ofNullable(message);
            return this;
        }

        public Builder messageType(MessageType messageType) {
            this.messageType = messageType;
            return this;
        }

        public DeliveryRecord build() {
            return new DeliveryRecord(type, address, timestamp, message, messageType);
        }
    }

    // --

    public enum Type implements StringRepresentable {
        SENT("sent"),
        ARRIVED("arrived"),
        RETURNED("returned"),
        PAYBACK("payback"),
        CUSTOM("custom");

        public static final Codec<Type> CODEC = StringRepresentable.fromEnum(Type::values);
        public static final StreamCodec<ByteBuf, Type> STREAM_CODEC = ByteBufCodecs.idMapper(
              ByIdMap.continuous(Type::ordinal, Type.values(), ByIdMap.OutOfBoundsStrategy.ZERO), Type::ordinal);

        private final String name;

        Type(String name) {
            this.name = name;
        }

        @Override
        public @NotNull String getSerializedName() {
            return name;
        }

        public MutableComponent translate() {
            return Component.translatable("gui.envelope.delivery_log.record." + getSerializedName());
        }
    }

    public interface Message {
        Component RECIPIENT_NOT_FOUND = Component.translatable("gui.envelope.delivery_log.message.returned_recipient_not_found");
        Component RECIPIENT_INBOX_IS_FULL = Component.translatable("gui.envelope.delivery_log.message.returned_recipient_inbox_is_full");
        Component UNABLE_TO_REACH = Component.translatable("gui.envelope.delivery_log.message.returned_unable_to_reach");
        Component REJECTED = Component.translatable("gui.envelope.delivery_log.message.returned_rejected");

        Component PAYBACK_FULFILLED = Component.translatable("gui.envelope.delivery_log.message.payback_fulfilled");
        Component RETURNED_PAYBACK_SUBJECT_NOT_FOUND = Component.translatable("gui.envelope.delivery_log.message.returned_payback_subject_not_found");
        Component RETURNED_PAYBACK_IS_NOT_VALID = Component.translatable("gui.envelope.delivery_log.message.returned_payback_is_not_valid");
        Component RETURNED_PAYBACK_EXPIRED = Component.translatable("gui.envelope.delivery_log.message.returned_payback_expired");
    }

    public enum MessageType implements StringRepresentable {
        NEUTRAL("neutral", Style.EMPTY.withColor(ChatFormatting.GRAY)),
        POSITIVE("positive", Style.EMPTY.withColor(0xFF75DC7C)),
        NEGATIVE("negative", Style.EMPTY.withColor(0xFFE48174));

        public static final Codec<MessageType> CODEC = StringRepresentable.fromEnum(MessageType::values);
        public static final StreamCodec<ByteBuf, MessageType> STREAM_CODEC = ByteBufCodecs.idMapper(
              ByIdMap.continuous(MessageType::ordinal, MessageType.values(), ByIdMap.OutOfBoundsStrategy.ZERO), MessageType::ordinal);

        private final String name;
        private final Style style;

        MessageType(String name, Style style) {
            this.name = name;
            this.style = style;
        }

        @Override
        public @NotNull String getSerializedName() {
            return name;
        }

        public Style getStyle() {
            return style;
        }
    }
}
