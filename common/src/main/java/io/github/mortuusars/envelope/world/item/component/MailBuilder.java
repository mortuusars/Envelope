package io.github.mortuusars.envelope.world.item.component;

import com.google.common.base.Preconditions;
import io.github.mortuusars.envelope.world.item.component.mail.log.DeliveryLog;
import io.github.mortuusars.envelope.world.item.component.mail.log.DeliveryRecord;
import io.github.mortuusars.envelope.world.mail.address.Address;
import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.UnaryOperator;

public class MailBuilder<T extends MailBuilder<T>> implements DataComponentHolder {
    private final @NotNull ItemStack item;

    public MailBuilder(@NotNull ItemStack item) {
        Preconditions.checkNotNull(item);
        this.item = item;
    }

    public MailBuilder(@NotNull Item item) {
        Preconditions.checkNotNull(item);
        this.item = new ItemStack(item);
    }

    @SuppressWarnings("unchecked")
    T self() {
        return (T) this;
    }

    public T id(Id id) {
        Mail.setId(item, id);
        return self();
    }

    public T sender(Address sender) {
        Mail.setSender(item, sender);
        return self();
    }

    public T recipient(Address recipient) {
        Mail.setRecipient(item, recipient);
        return self();
    }

    public T setLog(DeliveryLog log) {
        Mail.setLog(item, log);
        return self();
    }

    public T writeToLog(DeliveryRecord record) {
        Mail.writeToLog(item, record);
        return self();
    }

    public T writeToLog(DeliveryRecord.Builder record) {
        Mail.writeToLog(item, record);
        return self();
    }

    // --

    @Override
    public @NotNull DataComponentMap getComponents() {
        return item.getComponents();
    }

    public <V> T set(DataComponentType<V> type, @Nullable V value) {
        item.set(type, value);
        return self();
    }

    public <V> T update(DataComponentType<V> type, V defaultValue, @NotNull UnaryOperator<V> updater) {
        item.update(type, defaultValue, updater);
        return self();
    }

    // --

    public @NotNull ItemStack get() {
        return item;
    }
}