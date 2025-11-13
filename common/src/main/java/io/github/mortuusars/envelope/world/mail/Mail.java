package io.github.mortuusars.envelope.world.mail;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.delivery.log.DeliveryLog;
import io.github.mortuusars.envelope.world.mail.address.Address;
import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.UnaryOperator;

public class Mail implements DataComponentHolder {
    public static final Codec<Mail> CODEC = RecordCodecBuilder.create(i -> i.group(
          ItemStack.OPTIONAL_CODEC.optionalFieldOf("item", ItemStack.EMPTY).forGetter(Mail::getItemForReading),
          DeliveryLog.CODEC.optionalFieldOf("log", DeliveryLog.EMPTY).forGetter(Mail::getDeliveryLog)
    ).apply(i, Mail::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, Mail> STREAM_CODEC = StreamCodec.composite(
          ItemStack.STREAM_CODEC, Mail::getItemForReading,
          DeliveryLog.STREAM_CODEC, Mail::getDeliveryLog,
          Mail::new
    );

    public static final Mail EMPTY = new Mail(ItemStack.EMPTY, DeliveryLog.EMPTY);

    private final ItemStack stack;
    private final DeliveryLog deliveryLog;

    public Mail(ItemStack stack, DeliveryLog deliveryLog) {
        this.stack = stack;
        this.deliveryLog = deliveryLog;
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

    public DeliveryLog getDeliveryLog() {
        return deliveryLog;
    }

    public Optional<Address> getSender() {
        return Optional.ofNullable(get(Envelope.DataComponents.MAIL_SENDER));
    }

    public Address getSenderOrElse(Address orElse) {
        return getOrDefault(Envelope.DataComponents.MAIL_SENDER, orElse);
    }

    public Address getSenderOrThrow() {
        return getSender().orElseThrow(() ->
              new IllegalStateException("Mail '" + getItemForReading() + "' does not have 'envelope:mail_sender' defined."));
    }

    public Optional<Address> getRecipient() {
        return Optional.ofNullable(getItemForReading().get(Envelope.DataComponents.MAIL_RECIPIENT));
    }

    public Address getRecipientOrElse(Address orElse) {
        return getOrDefault(Envelope.DataComponents.MAIL_RECIPIENT, orElse);
    }

    public Address getRecipientOrThrow() {
        return getRecipient().orElseThrow(() ->
              new IllegalStateException("Mail '" + getItemForReading() + "' does not have 'envelope:mail_recipient' defined."));
    }

    // -- Data Component Holder

    @Override
    public @NotNull DataComponentMap getComponents() {
        return getItemForReading().getComponents();
    }

    @Nullable
    @Override
    public <T> T get(DataComponentType<? extends T> component) {
        return getItemForReading().get(component);
    }

    @Override
    public <T> @NotNull T getOrDefault(DataComponentType<? extends T> component, T defaultValue) {
        return getItemForReading().getOrDefault(component, defaultValue);
    }

    @Override
    public boolean has(DataComponentType<?> component) {
        return getItemForReading().has(component);
    }

    // --

    public boolean isEmpty() {
        return this == EMPTY || getItemForReading().isEmpty();
    }

    public Mail writeToLog(UnaryOperator<DeliveryLog> writer) {
        if (isEmpty()) return this;
        return new Mail(getItemForReading(), writer.apply(getDeliveryLog()));
    }
}