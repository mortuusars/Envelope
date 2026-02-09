package io.github.mortuusars.envelope.world.mail.receiver;

import io.github.mortuusars.envelope.world.item.component.mail.log.DeliveryRecord;
import io.github.mortuusars.envelope.world.mail.address.Address;
import io.github.mortuusars.envelope.world.mail.address.type.CustomAddress;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

public class CustomMailReceiver implements MailReceiver {
    private final CustomAddress address;

    public CustomMailReceiver(CustomAddress address) {
        this.address = address;
    }

    @Override
    public ItemStack receiveMail(ServerLevel level, Address sender, ItemStack mail) {
        return returned(mail, DeliveryRecord.Message.RECIPIENT_NOT_FOUND);
    }
}