package io.github.mortuusars.envelope.world.mail.receiver;

import io.github.mortuusars.envelope.world.delivery.log.DeliveryRecord;
import io.github.mortuusars.envelope.world.mail.Mail;
import io.github.mortuusars.envelope.world.mail.address.Address;
import io.github.mortuusars.envelope.world.service.MailService;
import net.minecraft.server.level.ServerLevel;

public class EntityMailReceiver implements MailReceiver {
    private final Address.Entity address;

    public EntityMailReceiver(Address.Entity address) {
        this.address = address;
    }

    @Override
    public Mail receiveMail(ServerLevel level, Mail mail) {
        return MailService.of(level).getMailEntities().byAddress(address)
              .map(entity -> entity.receiveMail(level, mail))
              .orElseGet(() -> mail.writeToLog(DeliveryRecord.returnedFrom(Address.MAIL_SERVICE)
                    .message(DeliveryRecord.Message.RECIPIENT_NOT_FOUND)));
    }
}
