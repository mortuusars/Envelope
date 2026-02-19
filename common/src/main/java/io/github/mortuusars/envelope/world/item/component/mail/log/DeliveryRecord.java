package io.github.mortuusars.envelope.world.item.component.mail.log;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import io.github.mortuusars.envelope.PlatformHelper;
import io.github.mortuusars.envelope.util.Colors;
import io.github.mortuusars.envelope.world.GameTime;
import io.github.mortuusars.envelope.world.item.component.mail.log.record.ArrivedRecord;
import io.github.mortuusars.envelope.world.item.component.mail.log.record.MessageRecord;
import io.github.mortuusars.envelope.world.item.component.mail.log.record.ReturnedRecord;
import io.github.mortuusars.envelope.world.item.component.mail.log.record.SentRecord;
import io.github.mortuusars.envelope.world.mail.address.Address;
import net.minecraft.ChatFormatting;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;
import java.util.function.IntFunction;

public interface DeliveryRecord {
    Codec<DeliveryRecord> CODEC = DeliveryRecord.Type.CODEC.dispatch(DeliveryRecord::getType, DeliveryRecord.Type::getCodec);
    StreamCodec<RegistryFriendlyByteBuf, DeliveryRecord> STREAM_CODEC = DeliveryRecord.Type.STREAM_CODEC.dispatch(DeliveryRecord::getType, DeliveryRecord.Type::getStreamCodec);

    DeliveryRecord.Type getType();

    MutableComponent getDisplayComponent();

    default Optional<Component> getElapsedTime(long timestamp) {
        if (timestamp <= 0) return Optional.empty();
        return getCurrentGameTime().map(time -> GameTime.formatLargest(time - timestamp, false)
              .withStyle(ChatFormatting.DARK_GRAY));
    }

    // --

    static DeliveryRecord sentFrom(Address address, long timestamp) {
        return new SentRecord(Objects.requireNonNull(address), timestamp);
    }

    /**
     * Uses current server time as timestamp.
     */
    static DeliveryRecord sentFrom(Address address) {
        return new SentRecord(Objects.requireNonNull(address), getCurrentGameTime().orElse(-1L));
    }

    static DeliveryRecord arrivedTo(Address address, long timestamp) {
        return new ArrivedRecord(Objects.requireNonNull(address), timestamp);
    }

    /**
     * Uses current server time as timestamp.
     */
    static DeliveryRecord arrivedTo(Address address) {
        return new ArrivedRecord(Objects.requireNonNull(address), getCurrentGameTime().orElse(-1L));
    }

    static DeliveryRecord returned(Component message) {
        return new ReturnedRecord(Objects.requireNonNull(message));
    }

    static DeliveryRecord message(Component message) {
        return new MessageRecord(Objects.requireNonNull(message));
    }

    // --

    static Optional<Long> getCurrentGameTime() {
        @Nullable MinecraftServer server = PlatformHelper.getCurrentServer();
        if (server == null) return Optional.empty();
        return Optional.of(server.overworld().getGameTime());
    }

    // --

    enum Type implements StringRepresentable {
        SENT("sent", SentRecord.CODEC, SentRecord.STREAM_CODEC),
        ARRIVED("arrived", ArrivedRecord.CODEC, ArrivedRecord.STREAM_CODEC),
        RETURNED("returned", ReturnedRecord.CODEC, ReturnedRecord.STREAM_CODEC),
        MESSAGE("message", MessageRecord.CODEC, MessageRecord.STREAM_CODEC);

        public static final Codec<DeliveryRecord.Type> CODEC = StringRepresentable.fromEnum(DeliveryRecord.Type::values);
        public static final IntFunction<DeliveryRecord.Type> BY_ID = ByIdMap.continuous(DeliveryRecord.Type::ordinal, values(), ByIdMap.OutOfBoundsStrategy.ZERO);
        public static final StreamCodec<RegistryFriendlyByteBuf, DeliveryRecord.Type> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, DeliveryRecord.Type::ordinal).cast();

        private final String name;
        private final MapCodec<? extends DeliveryRecord> codec;
        private final StreamCodec<RegistryFriendlyByteBuf, ? extends DeliveryRecord> streamCodec;

        Type(String name, MapCodec<? extends DeliveryRecord> codec, StreamCodec<RegistryFriendlyByteBuf, ? extends DeliveryRecord> streamCodec) {
            this.name = name;
            this.codec = codec;
            this.streamCodec = streamCodec;
        }

        @Override
        public @NotNull String getSerializedName() {
            return name;
        }

        public MapCodec<? extends DeliveryRecord> getCodec() {
            return codec;
        }

        public StreamCodec<RegistryFriendlyByteBuf, ? extends DeliveryRecord> getStreamCodec() {
            return streamCodec;
        }
    }

    interface Message {
        Component RECIPIENT_NOT_FOUND = Component.translatable("gui.envelope.delivery_log.message.recipient_not_found").withColor(Colors.TOOLTIP_RED);
        Component RECIPIENT_CANNOT_BE_DETERMINED = Component.translatable("gui.envelope.delivery_log.message.recipient_cannot_be_determined").withColor(Colors.TOOLTIP_RED);
        Component RECIPIENT_INBOX_IS_FULL = Component.translatable("gui.envelope.delivery_log.message.recipient_inbox_is_full").withColor(Colors.TOOLTIP_RED);
        Component UNABLE_TO_REACH = Component.translatable("gui.envelope.delivery_log.message.unable_to_reach").withColor(Colors.TOOLTIP_RED);
        Component REJECTED = Component.translatable("gui.envelope.delivery_log.message.rejected").withColor(Colors.TOOLTIP_RED);

        Component PAYBACK_FULFILLED = Component.translatable("gui.envelope.delivery_log.message.payback_fulfilled").withColor(Colors.TOOLTIP_GREEN);
        Component PAYBACK_SUBJECT_NOT_FOUND = Component.translatable("gui.envelope.delivery_log.message.payback.subject_not_found").withColor(Colors.TOOLTIP_RED);
        Component PAYBACK_IS_NOT_VALID = Component.translatable("gui.envelope.delivery_log.message.payback.is_not_valid").withColor(Colors.TOOLTIP_RED);
        Component PAYBACK_EXPIRED = Component.translatable("gui.envelope.delivery_log.message.payback.expired").withColor(Colors.TOOLTIP_RED);

        Component CRAFTING_UNPROCESSED_ITEMS = Component.translatable("gui.envelope.delivery_log.message.crafting.unprocessed_items").withColor(Colors.TOOLTIP_RED);
        Component CRAFTING_UNABLE_TO_PROCESS = Component.translatable("gui.envelope.delivery_log.message.crafting.unable_to_process").withColor(Colors.TOOLTIP_RED);
    }
}