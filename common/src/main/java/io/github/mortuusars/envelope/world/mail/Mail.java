package io.github.mortuusars.envelope.world.mail;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.mail.address.Address;
import io.github.mortuusars.envelope.world.item.component.MailDeliveryLog;
import io.github.mortuusars.envelope.world.item.component.MailStatus;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class Mail {
    public static ItemStack sent(ItemStack mail, Level level) {
        mail.remove(Envelope.DataComponents.MAIL_DELIVERY_LOG); // Remove previous log before new send
        mail.remove(Envelope.DataComponents.MAIL_STATUS); // Remove previous status before new send

        MailDeliveryLog.addRecords(mail,
              MailDeliveryLog.Record.sentFrom(mail.getOrDefault(Envelope.DataComponents.MAIL_SENDER, Address.UNKNOWN))
                    .atTime(level.getGameTime()));
        return mail;
    }

    public static ItemStack returned(ItemStack mail, Address from) {
        mail.set(Envelope.DataComponents.MAIL_STATUS, MailStatus.RETURNED);
        MailDeliveryLog.addRecords(mail, MailDeliveryLog.Record.returnedFrom(from));
        return mail;
    }

    public static ItemStack returned(ItemStack mail, Address from, Component message) {
        mail.set(Envelope.DataComponents.MAIL_STATUS, MailStatus.RETURNED);
        MailDeliveryLog.addRecords(mail, MailDeliveryLog.Record.returnedFrom(from).withMessage(message));
        return mail;
    }

    public static ItemStack returnedRecipientNotFound(ItemStack mail) {
        mail.set(Envelope.DataComponents.MAIL_STATUS, MailStatus.RETURNED);
        MailDeliveryLog.addRecords(mail, MailDeliveryLog.Record.returnedFrom(Address.MAIL_SERVICE)
              .withMessage(Component.translatable("gui.envelope.mail.log.returned.recipient_not_found")));
        return mail;
    }
}
