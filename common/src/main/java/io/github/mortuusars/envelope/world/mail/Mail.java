package io.github.mortuusars.envelope.world.mail;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.delivery.log.DeliveryLog;
import io.github.mortuusars.envelope.world.delivery.log.DeliveryRecord;
import io.github.mortuusars.envelope.world.item.component.RequestedPayback;
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
          DeliveryLog.CODEC.optionalFieldOf("log", DeliveryLog.EMPTY).forGetter(Mail::getLog)
    ).apply(i, Mail::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, Mail> STREAM_CODEC = StreamCodec.composite(
          ItemStack.STREAM_CODEC, Mail::getItem,
          DeliveryLog.STREAM_CODEC, Mail::getLog,
          Mail::new
    );

    public static final Mail EMPTY = new Mail(ItemStack.EMPTY, DeliveryLog.EMPTY);

    private final ItemStack stack;
    private final DeliveryLog log;

    public Mail(ItemStack stack, DeliveryLog log) {
        this.stack = stack;
        this.log = log;
    }

    public Mail(ItemStack stack) {
        this.stack = stack;
        this.log = new DeliveryLog();
    }

    public ItemStack getItem() {
        return stack;
    }

    public DeliveryLog getLog() {
        return log;
    }

    public Address getRecipient() {
        return getOrDefault(Envelope.DataComponents.RECIPIENT_ADDRESS, Address.UNKNOWN);
    }

    public Optional<RequestedPayback> getPayback() {
        return Optional.ofNullable(get(Envelope.DataComponents.REQUESTED_PAYBACK));
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
        return has(Envelope.DataComponents.REQUESTED_PAYBACK);
    }

    public Mail writeToLog(DeliveryRecord record) {
        if (!isEmpty()) {
            getLog().append(record);
        }
        return this;
    }

    public Mail writeToLog(DeliveryRecord.Builder recordBuilder) {
        if (!isEmpty()) {
            getLog().append(recordBuilder);
        }
        return this;
    }

    /**
     * "Finalizes" mail by removing sending data and adding data that recipient should see.
     * @return Copied instance with changed components.
     */
    public Mail asDeliveryResult() {
        ItemStack stack = getItem().copy();
        stack.remove(Envelope.DataComponents.RECIPIENT_ADDRESS);
        stack.remove(Envelope.DataComponents.REQUESTED_PAYBACK);
        getLog().getFirstSender().ifPresent(sender -> stack.set(Envelope.DataComponents.SENDER_ADDRESS, sender));
        return new Mail(stack, getLog().copy());
    }

    // --

    @Override
    public String toString() {
        return getItem().getHoverName().getString();
    }
}