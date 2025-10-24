package io.github.mortuusars.envelope.world.item.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.mail.address.Address;
import io.github.mortuusars.envelope.world.mail.address.AddressDisplay;
import io.github.mortuusars.envelope.util.PrettyGameTime;
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
import java.util.function.Predicate;

public record MailDeliveryLog(List<Record> records) {
    public static final Codec<MailDeliveryLog> CODEC = Record.CODEC.listOf()
            .xmap(MailDeliveryLog::new, MailDeliveryLog::records);
    public static final StreamCodec<RegistryFriendlyByteBuf, MailDeliveryLog> STREAM_CODEC =
            Record.STREAM_CODEC.apply(ByteBufCodecs.list(32)).map(MailDeliveryLog::new, MailDeliveryLog::records);

    public static final MailDeliveryLog EMPTY = new MailDeliveryLog(Collections.emptyList());

    public boolean isEmpty() {
        return this.equals(EMPTY) || records.isEmpty();
    }

    public Optional<Record> getLastRecord(Predicate<Record> predicate) {
        for (int i = records.size() - 1; i >= 0; i--) {
            Record record = records.get(i);
            if (predicate.test(record)) {
                return Optional.ofNullable(record);
            }
        }
        return Optional.empty();
    }

    public Optional<Record> getLastRecord() {
        return !records.isEmpty() ? Optional.of(records.getLast()) : Optional.empty();
    }

    public Optional<Record> getLastRecordOfType(Record.Type type) {
        return getLastRecord(record -> record.type() == type);
    }

    // --

    public static MailDeliveryLog of(ItemStack mail) {
        return mail.getOrDefault(Envelope.DataComponents.MAIL_DELIVERY_LOG, EMPTY);
    }

    public static void addRecords(ItemStack mail, Record... records) {
        MailDeliveryLog mailDeliveryLog = mail.getOrDefault(Envelope.DataComponents.MAIL_DELIVERY_LOG, MailDeliveryLog.EMPTY);
        List<Record> travelingRecords = new ArrayList<>(mailDeliveryLog.records);
        travelingRecords.addAll(List.of(records));
        mail.set(Envelope.DataComponents.MAIL_DELIVERY_LOG, new MailDeliveryLog(travelingRecords));
    }

    public record Record(Type type, Address address, Optional<Long> timestamp, Optional<Component> message) {
        public static final Codec<Record> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Type.CODEC.fieldOf("status").forGetter(Record::type),
                Address.CODEC.fieldOf("address").forGetter(Record::address),
                Codec.LONG.optionalFieldOf("timestamp").forGetter(Record::timestamp),
                ComponentSerialization.CODEC.optionalFieldOf("message").forGetter(Record::message)
        ).apply(instance, Record::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, Record> STREAM_CODEC = StreamCodec.composite(
                Type.STREAM_CODEC, Record::type,
                Address.STREAM_CODEC, Record::address,
                ByteBufCodecs.optional(ByteBufCodecs.VAR_LONG), Record::timestamp,
                ByteBufCodecs.optional(ComponentSerialization.STREAM_CODEC), Record::message,
                Record::new
        );

        public Record(Type type, Address address) {
            this(type, address, Optional.empty(), Optional.empty());
        }

        public Record atTime(long timestamp) {
            return new Record(type, address, Optional.of(timestamp), message);
        }

        public Record withMessage(Component message) {
            return new Record(type, address, timestamp, Optional.of(message));
        }

        public Record withMessage(Optional<Component> message) {
            return new Record(type, address, timestamp, message);
        }

        public Record withOperatorMessage(Optional<Component> operator) {
            return new Record(type, address, timestamp,
                    operator.map(name -> Component.translatable("gui.envelope.mail.log.record.message.by", name)));
        }

        // --

        public static Record sentFrom(Address address) {
            return new Record(Type.SENT, address);
        }

        public static Record returnedFrom(Address address) {
            return new Record(Type.RETURNED, address);
        }

        public static Record arrivedTo(Address address) {
            return new Record(Type.ARRIVED, address);
        }

        // --

        public MutableComponent translate(long gameTime) {
            MutableComponent component = Component.translatable(
                    "gui.envelope.mail.log.record." + type.name, AddressDisplay.create(
                            address, AddressDisplay.GENERIC_ICON_STYLE, Style.EMPTY.applyFormat(ChatFormatting.WHITE)));
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

        public enum Type implements StringRepresentable {
            SENT("sent"),
            ARRIVED("arrived"),
            RETURNED("returned");

            public static final Codec<Type> CODEC = StringRepresentable.fromEnum(Type::values);
            public static final StreamCodec<ByteBuf, Type> STREAM_CODEC =
                    ByteBufCodecs.idMapper(ByIdMap.continuous(Type::ordinal, Type.values(), ByIdMap.OutOfBoundsStrategy.ZERO), Type::ordinal);

            private final String name;

            Type(String name) {
                this.name = name;
            }

            @Override
            public @NotNull String getSerializedName() {
                return name;
            }
        }
    }
}