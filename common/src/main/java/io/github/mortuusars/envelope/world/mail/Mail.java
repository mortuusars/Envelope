package io.github.mortuusars.envelope.world.mail;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.delivery.log.DeliveryLog;
import io.github.mortuusars.envelope.world.delivery.log.DeliveryRecord;
import io.github.mortuusars.envelope.world.item.component.Payback;
import io.github.mortuusars.envelope.world.mail.address.Address;
import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class Mail implements DataComponentHolder {
    public static final Codec<Mail> CODEC = RecordCodecBuilder.create(i -> i.group(
          ItemStack.OPTIONAL_CODEC.optionalFieldOf("item", ItemStack.EMPTY).forGetter(Mail::getItem),
          DeliveryLog.CODEC.optionalFieldOf("log", DeliveryLog.empty()).forGetter(Mail::getLog)
    ).apply(i, Mail::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, Mail> STREAM_CODEC = StreamCodec.composite(
          ItemStack.STREAM_CODEC, Mail::getItem,
          DeliveryLog.STREAM_CODEC, Mail::getLog,
          Mail::new
    );

    private final ItemStack stack;
    private final DeliveryLog log;

    public Mail(ItemStack stack, DeliveryLog log) {
        this.stack = stack;
        this.log = log;
    }

    public Mail(ItemStack stack) {
        this.stack = stack;
        this.log = DeliveryLog.empty();
    }

    public static Mail empty() {
        return new Mail(ItemStack.EMPTY, DeliveryLog.empty());
    }

    public ItemStack getItem() {
        return stack;
    }

    public DeliveryLog getLog() {
        return log;
    }

    public Address getRecipient() {
        return getOrDefault(Envelope.DataComponents.ADDRESS_TAG, Address.UNKNOWN);
    }

    public Optional<Payback> getPayback() {
        return Optional.ofNullable(get(Envelope.DataComponents.PAYBACK_TAG));
    }

    public Address getSenderAddress() {
        return getOrDefault(Envelope.DataComponents.SENDER_ADDRESS, Address.UNKNOWN);
    }

    @Override
    public @NotNull DataComponentMap getComponents() {
        return getItem().getComponents();
    }

    // --

    public boolean isEmpty() {
        return getItem().isEmpty();
    }

    public boolean hasPayback() {
        return has(Envelope.DataComponents.PAYBACK_TAG);
    }

    public Mail writeToLog(DeliveryRecord record) {
        getLog().append(record);
        return this;
    }

    public Mail writeToLog(DeliveryRecord.Builder recordBuilder) {
        getLog().append(recordBuilder);
        return this;
    }

    // --

    @Override
    public String toString() {
        return getItem().getHoverName().getString();
    }
}