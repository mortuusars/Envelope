package io.github.mortuusars.envelope.world.mail.receiver;

import io.github.mortuusars.envelope.world.item.component.Mail;
import io.github.mortuusars.envelope.world.item.component.mail.log.DeliveryRecord;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

public interface MailReceiver {
    ItemStack receiveMail(ServerLevel level, ItemStack mail);

    default ItemStack returned(ItemStack mail, Component message) {
        Mail.writeToLog(mail, DeliveryRecord.returned(message));
        Mail.setReturned(mail);
        return mail;
    }
}