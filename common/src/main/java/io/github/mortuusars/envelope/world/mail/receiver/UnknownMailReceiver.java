package io.github.mortuusars.envelope.world.mail.receiver;

import io.github.mortuusars.envelope.world.item.component.mail.log.DeliveryRecord;
import io.github.mortuusars.envelope.world.mail.address.Address;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

public class UnknownMailReceiver implements MailReceiver {
    @Override
    public ItemStack receiveMail(ServerLevel level, Address sender, ItemStack mail) {
        return returned(mail, DeliveryRecord.Message.RECIPIENT_CANNOT_BE_DETERMINED);
    }
}