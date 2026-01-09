package io.github.mortuusars.envelope.world.item.component;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.item.component.mail.MailDeliveryLog;
import io.github.mortuusars.envelope.world.item.component.mail.MailDeliveryRecord;
import io.github.mortuusars.envelope.world.item.component.mail.MailStatus;
import io.github.mortuusars.envelope.world.mail.address.Address;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.function.UnaryOperator;

public final class NewMail {
    private NewMail() {}

    // -- Id

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

    public static MailDeliveryLog getLog(ItemStack stack) {
        return stack.getOrDefault(Envelope.DataComponents.MAIL_DELIVERY_LOG, MailDeliveryLog.EMPTY);
    }

    public static ItemStack setLog(ItemStack stack, MailDeliveryLog log) {
        stack.set(Envelope.DataComponents.MAIL_DELIVERY_LOG, log);
        return stack;
    }

    public static ItemStack writeToLog(ItemStack stack, MailDeliveryRecord record) {
        stack.update(Envelope.DataComponents.MAIL_DELIVERY_LOG, MailDeliveryLog.EMPTY, log -> log.append(record));
        return stack;
    }

    public static ItemStack writeToLog(ItemStack stack, MailDeliveryRecord.Builder record) {
        return writeToLog(stack, record.build());
    }

    public static ItemStack writeToLog(ItemStack stack, MailDeliveryRecord... records) {
        stack.update(Envelope.DataComponents.MAIL_DELIVERY_LOG, MailDeliveryLog.EMPTY, log -> log.append(records));
        return stack;
    }

    public static ItemStack writeToLog(ItemStack stack, MailDeliveryRecord.Builder... records) {
        stack.update(Envelope.DataComponents.MAIL_DELIVERY_LOG, MailDeliveryLog.EMPTY, log -> log.append(records));
        return stack;
    }

    // --

    public static Address getSender(ItemStack stack) {
        return stack.getOrDefault(Envelope.DataComponents.MAIL_SENDER, Address.UNKNOWN);
    }
}
