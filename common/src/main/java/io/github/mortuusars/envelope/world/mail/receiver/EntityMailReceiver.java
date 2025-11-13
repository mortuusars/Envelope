package io.github.mortuusars.envelope.world.mail.receiver;

import io.github.mortuusars.envelope.world.delivery.log.DeliveryRecord;
import io.github.mortuusars.envelope.world.mail.Mail;
import io.github.mortuusars.envelope.world.mail.address.Address;
import net.minecraft.server.level.ServerLevel;

public class EntityMailReceiver implements MailReceiver {
    private final Address.Entity address;

    public EntityMailReceiver(Address.Entity address) {
        this.address = address;
    }

    @Override
    public Mail receiveMail(ServerLevel level, Mail mail) {
        return level.getEnvelopeContext().getMailEntities().byAddress(address)
              .map(entity -> entity.receiveMail(level, mail))
              .orElseGet(() -> mail.writeToLog(log -> log.append(DeliveryRecord.returned_recipientNotFound())));
    }
}
