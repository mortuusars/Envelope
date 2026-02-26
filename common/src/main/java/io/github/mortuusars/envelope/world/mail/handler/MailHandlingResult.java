package io.github.mortuusars.envelope.world.mail.handler;

import io.github.mortuusars.envelope.world.item.component.mail.log.DeliveryRecord;
import io.github.mortuusars.envelope.world.item.mail.Mail;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public interface MailHandlingResult {
    MailHandlingResult CONSUME = () -> ItemStack.EMPTY;
    MailHandlingResult PASS = () -> ItemStack.EMPTY;

    ItemStack getMail();

    default boolean isHandled() {
        return this != PASS;
    }

    // --

    static MailHandlingResult reply(ItemStack mail) {
        return new Reply(mail);
    }

    static MailHandlingResult returned(ItemStack mail, Component message) {
        Mail.writeToLog(mail, DeliveryRecord.returned(message));
        return new Returned(mail);
    }

    // --

    record Reply(ItemStack mail) implements MailHandlingResult {
        @Override
        public ItemStack getMail() {
            return mail;
        }
    }

    record Returned(ItemStack mail) implements MailHandlingResult {
        @Override
        public ItemStack getMail() {
            return mail;
        }
    }
}
