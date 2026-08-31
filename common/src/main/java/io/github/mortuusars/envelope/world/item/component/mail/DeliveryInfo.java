package io.github.mortuusars.envelope.world.item.component.mail;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.item.component.mail.log.DeliveryLog;
import io.github.mortuusars.envelope.world.mail.address.Address;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

public record DeliveryInfo(Optional<Address> sender, Optional<Address> recipient, DeliveryLog log, boolean isReturned) {
    public static final Codec<DeliveryInfo> CODEC = RecordCodecBuilder.create(i -> i.group(
          Address.CODEC.optionalFieldOf("sender").forGetter(DeliveryInfo::sender),
          Address.CODEC.optionalFieldOf("recipient").forGetter(DeliveryInfo::recipient),
          DeliveryLog.CODEC.optionalFieldOf("log", DeliveryLog.EMPTY).forGetter(DeliveryInfo::log),
          Codec.BOOL.optionalFieldOf("returned", false).forGetter(DeliveryInfo::isReturned)
    ).apply(i, DeliveryInfo::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, DeliveryInfo> STREAM_CODEC = StreamCodec.composite(
          ByteBufCodecs.optional(Address.STREAM_CODEC), DeliveryInfo::sender,
          ByteBufCodecs.optional(Address.STREAM_CODEC), DeliveryInfo::recipient,
          DeliveryLog.STREAM_CODEC, DeliveryInfo::log,
          ByteBufCodecs.BOOL, DeliveryInfo::isReturned,
          DeliveryInfo::new
    );

    public static final DeliveryInfo EMPTY = new DeliveryInfo(Optional.empty(), Optional.empty(), DeliveryLog.EMPTY, false);

    public static DeliveryInfo of(ItemStack stack) {
        return stack.getOrDefault(Envelope.DataComponents.MAIL_DELIVERY_INFO, EMPTY);
    }

    public static DeliveryInfo.Mutable create() {
        return new Mutable(EMPTY);
    }

    public boolean isEmpty() {
        return equals(EMPTY);
    }

    public Mutable mutable() {
        return new Mutable(this);
    }

    // --

    @Override
    public boolean equals(Object object) {
        if (object == null || getClass() != object.getClass()) return false;
        DeliveryInfo that = (DeliveryInfo) object;
        return isReturned == that.isReturned
              && Objects.equals(sender, that.sender)
              && Objects.equals(recipient, that.recipient)
              && Objects.equals(log, that.log);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sender, recipient, log, isReturned);
    }

    // --

    public static class Mutable {
        private @Nullable Address sender;
        private @Nullable Address recipient;
        private DeliveryLog log;
        private boolean returned;

        public Mutable(DeliveryInfo info) {
            sender = info.sender.orElse(null);
            recipient = info.recipient.orElse(null);
            log = info.log;
            returned = info.isReturned;
        }

        public Mutable sender(@Nullable Address sender) {
            this.sender = sender;
            return this;
        }

        public Mutable recipient(@Nullable Address recipient) {
            this.recipient = recipient;
            return this;
        }

        public Mutable setLog(DeliveryLog log) {
            this.log = log;
            return this;
        }

        public Mutable updateLog(Function<DeliveryLog, DeliveryLog> updater) {
            this.log = updater.apply(log);
            return this;
        }

        public Mutable returned(boolean returned) {
            this.returned = returned;
            return this;
        }

        public Mutable returned() {
            this.returned = true;
            return this;
        }

        public DeliveryInfo immutable() {
            return new DeliveryInfo(Optional.ofNullable(sender), Optional.ofNullable(recipient), log, returned);
        }

        public ItemStack immutableApplyTo(ItemStack stack) {
            stack.set(Envelope.DataComponents.MAIL_DELIVERY_INFO, immutable());
            return stack;
        }
    }
}
