package io.github.mortuusars.envelope.world.mail;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.delivery.log.DeliveryLog;
import io.github.mortuusars.envelope.world.delivery.log.DeliveryRecord;
import io.github.mortuusars.envelope.world.item.PaybackPackageItem;
import io.github.mortuusars.envelope.world.mail.address.Address;
import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.function.Consumer;

public class Mail implements DataComponentHolder {
    public static final Codec<Mail> CODEC = RecordCodecBuilder.create(i -> i.group(
          ItemStack.OPTIONAL_CODEC.optionalFieldOf("item", ItemStack.EMPTY).forGetter(Mail::getItemForReading),
          DeliveryLog.CODEC.optionalFieldOf("log", DeliveryLog.EMPTY).forGetter(Mail::getLog)
    ).apply(i, Mail::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, Mail> STREAM_CODEC = StreamCodec.composite(
          ItemStack.STREAM_CODEC, Mail::getItemForReading,
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
        this(stack, DeliveryLog.EMPTY);
    }

    public ItemStack getItemForReading() {
        return stack;
    }

    public ItemStack getItemCopy() {
        return stack.copy();
    }

    public DeliveryLog getLog() {
        return log;
    }

    public Address getSender() {
        return getOrDefault(Envelope.DataComponents.SENDER, Address.UNKNOWN);
    }

    public Address getRecipient() {
        return getOrDefault(Envelope.DataComponents.RECIPIENT, Address.UNKNOWN);
    }

    @Override
    public @NotNull DataComponentMap getComponents() {
        return getItemForReading().getComponents();
    }

    // --

    public boolean isEmpty() {
        return this.equals(EMPTY) || getItemForReading().isEmpty();
    }

    public boolean hasPayback() {
        return has(Envelope.DataComponents.PAYBACK);
    }

    public boolean shouldBeHandledByMailService() {
        return hasPayback() || getItemForReading().getItem() instanceof PaybackPackageItem;
    }

    // --

    public Mail copy() {
        return new Mail(stack.copy(), log);
    }

    public Mail withItem(ItemStack stack) {
        return new Mail(stack, log);
    }

    public Mail withLog(DeliveryLog log) {
        return new Mail(stack, log);
    }

    public Mail withItem(Consumer<ItemStack> consumer) {
        if (isEmpty()) return this;
        ItemStack stackCopy = getItemForReading().copy();
        consumer.accept(stackCopy);
        return new Mail(stackCopy, log);
    }

    public Mail withSender(Address sender) {
        return withItem(item -> item.set(Envelope.DataComponents.SENDER, sender));
    }

    public Mail withRecipient(Address recipient) {
        return withItem(item -> item.set(Envelope.DataComponents.RECIPIENT, recipient));
    }

    public Mail writeToLog(DeliveryRecord record) {
        if (isEmpty()) return this;
        return new Mail(getItemForReading(), log.append(record));
    }

    public Mail writeToLog(DeliveryRecord.Builder recordBuilder) {
        if (isEmpty()) return this;
        return new Mail(getItemForReading(), log.append(recordBuilder));
    }

    // --

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Mail mail = (Mail) o;
        return Objects.equals(stack, mail.stack) && Objects.equals(log, mail.log);
    }

    @Override
    public int hashCode() {
        return stack.hashCode() + log.hashCode();
    }

    @Override
    public String toString() {
        return getItemForReading().getHoverName().getString();
    }

    // --

//    public static class Builder<T extends Builder<T>> {
//        protected final @NotNull ItemStack stack;
//        protected @Nullable Address sender;
//        protected @Nullable Address recipient;
//        protected DeliveryLog log = DeliveryLog.EMPTY;
//
//        public Builder(ItemStack stack) {
//            this.stack = stack;
//            this.sender = stack.get(Envelope.DataComponents.SENDER);
//            this.recipient = stack.get(Envelope.DataComponents.RECIPIENT);
//        }
//
//        public T withSender(Address sender) {
//            this.sender = sender;
//            return self();
//        }
//
//        public T withRecipient(Address recipient) {
//            this.recipient = recipient;
//            return self();
//        }
//
//        public Mail create() {
//            stack.set(Envelope.DataComponents.SENDER, sender);
//            stack.set(Envelope.DataComponents.RECIPIENT, recipient);
//            return new Mail(stack, log);
//        }
//
//        @SuppressWarnings("unchecked")
//        protected T self() {
//            return (T) this;
//        }
//    }
}