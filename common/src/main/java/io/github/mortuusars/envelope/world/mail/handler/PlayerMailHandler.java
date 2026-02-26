package io.github.mortuusars.envelope.world.mail.handler;

import io.github.mortuusars.envelope.world.item.component.mail.log.DeliveryRecord;
import io.github.mortuusars.envelope.world.mail.MailService;
import io.github.mortuusars.envelope.world.mail.address.type.PlayerAddress;
import io.github.mortuusars.envelope.world.mail.delivery.Delivery;

public class PlayerMailHandler implements MailHandler {
    private final PlayerAddress address;

    public PlayerMailHandler(PlayerAddress address) {
        this.address = address;
    }

    @Override
    public MailHandlingResult handle(MailService service, Delivery delivery) {
        if (!(address instanceof PlayerAddress playerAddress)) {
            return MailHandlingResult.PASS;
        }

        if (delivery.getMail().isEmpty()) {
            return MailHandlingResult.CONSUME;
        }

        return service.getKnownPlayers().getDefaultAddressOf(playerAddress)
              .map(blockAddress -> service.getDeliveryManager().getMailHandler(blockAddress).handle(service, delivery))
              .orElseGet(() -> MailHandlingResult.returned(delivery.getMail(), DeliveryRecord.Message.RECIPIENT_NOT_FOUND));
    }
}