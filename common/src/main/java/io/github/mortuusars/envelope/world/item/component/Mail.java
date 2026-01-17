package io.github.mortuusars.envelope.world.item.component;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.delivery.Delivery;
import io.github.mortuusars.envelope.world.delivery.phase.DeliveryPhase;
import io.github.mortuusars.envelope.world.item.component.mail.DeliveryLog;
import io.github.mortuusars.envelope.world.item.component.mail.DeliveryRecord;
import io.github.mortuusars.envelope.world.item.component.mail.MailStatus;
import io.github.mortuusars.envelope.world.mail.address.Address;
import io.github.mortuusars.envelope.world.mail.entity.mail_service.payback_department.PaybackSubject;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.UnaryOperator;

public final class Mail {
    private Mail() {
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

    // -- Status

    public static MailStatus getStatus(ItemStack stack) {
        return stack.getOrDefault(Envelope.DataComponents.MAIL_STATUS, MailStatus.REGULAR);
    }

    public static ItemStack setStatus(ItemStack stack, MailStatus status) {
        stack.set(Envelope.DataComponents.MAIL_STATUS, status == MailStatus.REGULAR ? null : status);
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

    public static @NotNull Address getSenderOrElse(ItemStack stack, Address orElse) {
        return stack.getOrDefault(Envelope.DataComponents.MAIL_SENDER, orElse);
    }

    public static @NotNull Address getSender(ItemStack stack) {
        return getSenderOrElse(stack, Address.UNKNOWN);
    }

    public static void setSender(@NotNull ItemStack stack, Address sender) {
        stack.set(Envelope.DataComponents.MAIL_SENDER, sender);
    }

    public static @NotNull Address getRecipient(ItemStack stack) {
        return stack.getOrDefault(Envelope.DataComponents.MAIL_RECIPIENT, Address.UNKNOWN);
    }

    public static void setRecipient(@NotNull ItemStack stack, Address recipient) {
        stack.set(Envelope.DataComponents.MAIL_RECIPIENT, recipient);
    }

    // --

    /**
     *
     */
    public static ItemStack createDeliveryResult(@NotNull Delivery delivery, ServerLevel level) {
        if (delivery.getMail().isEmpty()) return ItemStack.EMPTY;

        ItemStack result = delivery.getMail().copyWithCount(1);

        if (delivery.getPhase().isOnRecipientSide() || Mail.getStatus(result) == MailStatus.REGULAR) {
            result.remove(Envelope.DataComponents.MAIL_RECIPIENT);
            result.remove(Envelope.DataComponents.MAIL_REQUESTED_PAYBACK);
            result.set(Envelope.DataComponents.MAIL_SENDER, delivery.getSender());
        }

        return result;
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
