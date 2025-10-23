package io.github.mortuusars.envelope.world.mail;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.core.address.Address;
import io.github.mortuusars.envelope.world.item.component.MailDeliveryLog;
import io.github.mortuusars.envelope.world.item.component.MailStatus;
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
}
