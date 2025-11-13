package io.github.mortuusars.envelope.world.mail.receiver;

import io.github.mortuusars.envelope.world.delivery.log.DeliveryRecord;
import io.github.mortuusars.envelope.world.mail.Mail;
import io.github.mortuusars.envelope.world.mail.address.Address;
import net.minecraft.server.level.ServerLevel;

public class PlayerMailReceiver implements MailReceiver {
    private final Address.Player address;

    public PlayerMailReceiver(Address.Player address) {
        this.address = address;
    }

    @Override
    public Mail receiveMail(ServerLevel level, Mail mail) {
        return level.getEnvelopeContext().getKnownPlayers().getUuid(address)
              .flatMap(uuid -> level.getEnvelopeContext().getDefaultAddresses().of(uuid))
              .map(PigeonholeMailReceiver::new)
              .map(receiver -> receiver.receiveMail(level, mail))
              .orElseGet(() -> mail.writeToLog(log -> log.append(DeliveryRecord.returned_recipientNotFound())));
    }
}