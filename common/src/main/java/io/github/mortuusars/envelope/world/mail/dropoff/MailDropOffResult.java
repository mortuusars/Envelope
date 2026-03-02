package io.github.mortuusars.envelope.world.mail.dropoff;

import io.github.mortuusars.envelope.world.item.component.mail.log.DeliveryRecord;
import io.github.mortuusars.envelope.world.item.mail.Mail;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public interface MailDropOffResult {
    MailDropOffResult CONSUME = () -> ItemStack.EMPTY;
    MailDropOffResult PASS = () -> ItemStack.EMPTY;

    ItemStack getMail();

    default boolean isHandled() {
        return this != PASS;
    }

    default boolean isReply() {
        return this instanceof Reply;
    }

    // --

    static MailDropOffResult reply(ItemStack mail) {
        return new Reply(mail);
    }

    static MailDropOffResult returned(ItemStack mail) {
        Mail.returned(mail, DeliveryRecord.Message.REJECTED);
        return new Returned(mail);
    }

    static MailDropOffResult returned(ItemStack mail, Component message) {
        Mail.returned(mail, message);
        return new Returned(mail);
    }

    // --

    record Reply(ItemStack mail) implements MailDropOffResult {
        @Override
        public ItemStack getMail() {
            return mail;
        }
    }

    record Returned(ItemStack mail) implements MailDropOffResult {
        @Override
        public ItemStack getMail() {
            return mail;
        }
    }
}
