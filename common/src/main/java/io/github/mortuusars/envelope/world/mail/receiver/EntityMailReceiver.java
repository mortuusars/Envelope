package io.github.mortuusars.envelope.world.mail.receiver;

import io.github.mortuusars.envelope.world.item.component.mail.DeliveryRecord;
import io.github.mortuusars.envelope.world.mail.address.Address;
import io.github.mortuusars.envelope.world.service.MailService;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

public class EntityMailReceiver implements MailReceiver {
    private final Address.Entity address;

    public EntityMailReceiver(Address.Entity address) {
        this.address = address;
    }

    @Override
    public ItemStack receiveMail(ServerLevel level, ItemStack mail) {
        return MailService.of(level).getMailEntities().byAddress(address)
              .map(entity -> entity.receiveMail(level, mail))
              .orElseGet(() -> returned(mail, DeliveryRecord.Message.RECIPIENT_NOT_FOUND));
    }
}
