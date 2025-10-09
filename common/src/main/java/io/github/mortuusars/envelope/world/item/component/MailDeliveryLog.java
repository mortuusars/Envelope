package io.github.mortuusars.envelope.world.item.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.core.address.Address;
import io.github.mortuusars.envelope.core.address.AddressDisplay;
import io.netty.buffer.ByteBuf;
import net.minecraft.ChatFormatting;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public record MailDeliveryLog(List<TravelingRecord> records) {
    public static final Codec<MailDeliveryLog> CODEC = TravelingRecord.CODEC.listOf()
            .xmap(MailDeliveryLog::new, MailDeliveryLog::records);
    public static final StreamCodec<RegistryFriendlyByteBuf, MailDeliveryLog> STREAM_CODEC =
            TravelingRecord.STREAM_CODEC.apply(ByteBufCodecs.list(32)).map(MailDeliveryLog::new, MailDeliveryLog::records);

    public static final MailDeliveryLog EMPTY = new MailDeliveryLog(Collections.emptyList());

    public boolean isEmpty() {
        return this.equals(EMPTY) || records.isEmpty();
    }

    public Optional<TravelingRecord> getLastRecord() {
        return !records.isEmpty() ? Optional.of(records.getLast()) : Optional.empty();
    }

    // --

    public static MailDeliveryLog of(ItemStack mail) {
        return mail.getOrDefault(Envelope.DataComponents.MAIL_DELIVERY_LOG, EMPTY);
    }

    public static void addRecords(ItemStack mail, TravelingRecord... records) {
        MailDeliveryLog mailDeliveryLog = mail.getOrDefault(Envelope.DataComponents.MAIL_DELIVERY_LOG, MailDeliveryLog.EMPTY);
        List<TravelingRecord> travelingRecords = new ArrayList<>(mailDeliveryLog.records);
        travelingRecords.addAll(List.of(records));
        mail.set(Envelope.DataComponents.MAIL_DELIVERY_LOG, new MailDeliveryLog(travelingRecords));
    }

    public record TravelingRecord(Status status, Address address, Optional<Long> timestamp, Optional<Component> message) {
        public static final Codec<TravelingRecord> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Status.CODEC.fieldOf("status").forGetter(TravelingRecord::status),
                Address.CODEC.fieldOf("address").forGetter(TravelingRecord::address),
                Codec.LONG.optionalFieldOf("timestamp").forGetter(TravelingRecord::timestamp),
                ComponentSerialization.CODEC.optionalFieldOf("message").forGetter(TravelingRecord::message)
        ).apply(instance, TravelingRecord::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, TravelingRecord> STREAM_CODEC = StreamCodec.composite(
                Status.STREAM_CODEC, TravelingRecord::status,
                Address.STREAM_CODEC, TravelingRecord::address,
                ByteBufCodecs.optional(ByteBufCodecs.VAR_LONG), TravelingRecord::timestamp,
                ByteBufCodecs.optional(ComponentSerialization.STREAM_CODEC), TravelingRecord::message,
                TravelingRecord::new
        );

        public TravelingRecord(Status status, Address address) {
            this(status, address, Optional.empty(), Optional.empty());
        }

        public TravelingRecord atTime(long timestamp) {
            return new TravelingRecord(status, address, Optional.of(timestamp), message);
        }

        public TravelingRecord withMessage(Component message) {
            return new TravelingRecord(status, address, timestamp, Optional.of(message));
        }

        public TravelingRecord withMessage(Optional<Component> message) {
            return new TravelingRecord(status, address, timestamp, message);
        }

        public TravelingRecord withOperatorMessage(Optional<Component> operator) {
            return new TravelingRecord(status, address, timestamp,
                    operator.map(name -> Component.translatable("gui.envelope.mail.log.record.message.by", name)));
        }

        // --

        public static TravelingRecord sentFrom(Address address) {
            return new TravelingRecord(TravelingRecord.Status.SENT, address);
        }

        public static TravelingRecord travelingTo(Address address) {
            return new TravelingRecord(TravelingRecord.Status.TRAVELING, address);
        }

        public static TravelingRecord returned(Address address) {
            return new TravelingRecord(TravelingRecord.Status.RETURNED, address);
        }

        public static TravelingRecord arrivedTo(Address address) {
            return new TravelingRecord(TravelingRecord.Status.ARRIVED, address);
        }

        public static TravelingRecord receivedAt(Address address) {
            return new TravelingRecord(TravelingRecord.Status.RECEIVED, address);
        }

        // --

        public MutableComponent translate() {
            MutableComponent component = Component.translatable(
                    "gui.envelope.mail.log.record." + status.name, AddressDisplay.create(
                            address, AddressDisplay.GENERIC_ICON_STYLE, Style.EMPTY.applyFormat(ChatFormatting.WHITE)));
            message.ifPresent(sibling -> {
                component.append(" ");
                component.append(sibling);
            });
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
                    ByteBufCodecs.idMapper(ByIdMap.continuous(Status::ordinal, Status.values(), ByIdMap.OutOfBoundsStrategy.ZERO), Status::ordinal);

            private final String name;

            Status(String name) {
                this.name = name;
            }

            @Override
            public @NotNull String getSerializedName() {
                return name;
            }
        }
    }
}