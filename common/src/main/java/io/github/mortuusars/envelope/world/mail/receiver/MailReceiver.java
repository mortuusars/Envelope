package io.github.mortuusars.envelope.world.mail.receiver;

import io.github.mortuusars.envelope.world.item.component.Mail;
import io.github.mortuusars.envelope.world.item.component.mail.DeliveryRecord;
import io.github.mortuusars.envelope.world.mail.address.Address;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

public interface MailReceiver {
    ItemStack receiveMail(ServerLevel level, ItemStack mail);

    default ItemStack returned(ItemStack mail, Component message) {
        Mail.writeToLog(mail, DeliveryRecord.returned(Address.MAIL_SERVICE).message(message));
        Mail.setReturned(mail);
        return mail;
    }
}
