package io.github.mortuusars.envelope.world.item.component;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.item.component.mail.DeliveryLog;
import io.github.mortuusars.envelope.world.item.component.mail.DeliveryRecord;
import io.github.mortuusars.envelope.world.mail.address.Address;
import io.github.mortuusars.envelope.world.mail.entity.mail_service.payback_department.PaybackSubject;
import net.minecraft.util.Unit;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.UnaryOperator;

public final class Mail {
    private Mail() {
    }

    // -- Address

    public static Optional<Address> getSender(ItemStack stack) {
        return Optional.ofNullable(stack.get(Envelope.DataComponents.MAIL_SENDER));
    }

    public static @NotNull Address getSenderOrElse(ItemStack stack, Address orElse) {
        return stack.getOrDefault(Envelope.DataComponents.MAIL_SENDER, orElse);
    }

    public static @NotNull Address getSenderOrUnknown(ItemStack stack) {
        return getSenderOrElse(stack, Address.UNKNOWN);
    }

    public static void setSender(@NotNull ItemStack stack, @Nullable Address sender) {
        stack.set(Envelope.DataComponents.MAIL_SENDER, sender);
    }

    public static Optional<Address> getRecipient(ItemStack stack) {
        return Optional.ofNullable(stack.get(Envelope.DataComponents.MAIL_RECIPIENT));
    }

    public static @NotNull Address getRecipientOrElse(ItemStack stack, Address orElse) {
        return stack.getOrDefault(Envelope.DataComponents.MAIL_RECIPIENT, orElse);
    }

    public static @NotNull Address getRecipientOrUnknown(ItemStack stack) {
        return getRecipientOrElse(stack, Address.UNKNOWN);
    }

    public static void setRecipient(@NotNull ItemStack stack, @Nullable Address recipient) {
        stack.set(Envelope.DataComponents.MAIL_RECIPIENT, recipient);
    }

    // -- Id

    public static boolean hasId(ItemStack stack) {
        return stack.has(Envelope.DataComponents.MAIL_ID);
    }

    public static @Nullable Id getId(ItemStack stack) {
        return stack.get(Envelope.DataComponents.MAIL_ID);
    }

    public static Id getOrCreateId(ItemStack stack, Level level) {
        return stack.update(Envelope.DataComponents.MAIL_ID, Id.create(level), UnaryOperator.identity());
    }

    public static ItemStack setId(ItemStack stack, Id id) {
        stack.set(Envelope.DataComponents.MAIL_ID, id);
        return stack;
    }

    // -- Returned

    public static boolean isReturned(ItemStack stack) {
        return stack.has(Envelope.DataComponents.MAIL_RETURNED);
    }

    public static ItemStack setReturned(ItemStack stack, boolean returned) {
        stack.set(Envelope.DataComponents.MAIL_RETURNED, returned ? Unit.INSTANCE : null);
        return stack;
    }

    public static ItemStack setReturned(ItemStack stack) {
        stack.set(Envelope.DataComponents.MAIL_RETURNED, Unit.INSTANCE);
        return stack;
    }

    // -- Log

    public static DeliveryLog getLog(ItemStack stack) {
        return stack.getOrDefault(Envelope.DataComponents.MAIL_DELIVERY_LOG, DeliveryLog.EMPTY);
    }

    public static ItemStack setLog(ItemStack stack, DeliveryLog log) {
        stack.set(Envelope.DataComponents.MAIL_DELIVERY_LOG, log);
        return stack;
    }

    public static ItemStack writeToLog(ItemStack stack, DeliveryRecord record) {
        stack.update(Envelope.DataComponents.MAIL_DELIVERY_LOG, DeliveryLog.EMPTY, log -> log.append(record));
        return stack;
    }

    public static ItemStack writeToLog(ItemStack stack, DeliveryRecord.Builder record) {
        return writeToLog(stack, record.build());
    }

    public static ItemStack writeToLog(ItemStack stack, DeliveryRecord... records) {
        stack.update(Envelope.DataComponents.MAIL_DELIVERY_LOG, DeliveryLog.EMPTY, log -> log.append(records));
        return stack;
    }

    public static ItemStack writeToLog(ItemStack stack, DeliveryRecord.Builder... records) {
        stack.update(Envelope.DataComponents.MAIL_DELIVERY_LOG, DeliveryLog.EMPTY, log -> log.append(records));
        return stack;
    }

    // --

    public static ItemStack removePreviousDeliveryData(ItemStack mail) {
        if (mail.isEmpty()) return ItemStack.EMPTY;

        mail.remove(Envelope.DataComponents.MAIL_ID);
        mail.remove(Envelope.DataComponents.MAIL_SENDER);
        mail.remove(Envelope.DataComponents.MAIL_DELIVERY_LOG);
        mail.remove(Envelope.DataComponents.MAIL_RETURNED);

        return mail;
    }

    public static ItemStack asDelivered(ItemStack mail) {
        if (mail.isEmpty()) return ItemStack.EMPTY;

        if (!isReturned(mail)) {
            mail.remove(Envelope.DataComponents.MAIL_RECIPIENT);
            mail.remove(Envelope.DataComponents.MAIL_REQUESTED_PAYBACK);
        }

        return mail;
    }

    // --

    public static MailBuilder<?> of(ItemStack stack) {
        return new MailBuilder<>(stack);
    }

    public static MailBuilder<?> createPackage(PackageContents contents) {
        return new MailBuilder<>(Envelope.Items.PACKAGE.get())
              .set(Envelope.DataComponents.PACKAGE_CONTENTS, contents);
    }

    public static MailBuilder<?> createPaybackPackingBox(PaybackSubject subject) {
        return new MailBuilder<>(Envelope.Items.PACKAGE.get())
              .set(Envelope.DataComponents.PAYBACK_PACKAGE_SUBJECT, subject);
    }
}
