package io.github.mortuusars.envelope.world.mail.receiver;

import io.github.mortuusars.envelope.world.item.mail.Mail;
import io.github.mortuusars.envelope.world.item.component.mail.log.DeliveryRecord;
import io.github.mortuusars.envelope.world.mail.address.Address;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

public interface MailReceiver {
    /**
     * Handles incoming mail.
     * @return Item that will continue on delivery path.<br>
     * - Empty when consumed.
     * - Same item (with or without extra data) when returned due to various reasons.
     * - Another item when result is produced.
     */
    ItemStack receiveMail(ServerLevel level, Address sender, ItemStack mail);

    default ItemStack returned(ItemStack mail, Address by, Component message) {
        Mail.writeToLog(mail, DeliveryRecord.returned(by, message));
        Mail.setReturned(mail);
        return mail;
    }
}