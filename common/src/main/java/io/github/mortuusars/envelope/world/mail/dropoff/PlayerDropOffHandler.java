package io.github.mortuusars.envelope.world.mail.dropoff;

import io.github.mortuusars.envelope.world.item.component.mail.log.DeliveryRecord;
import io.github.mortuusars.envelope.world.mail.address.type.PlayerAddress;

public class PlayerDropOffHandler implements MailDropOffHandler {
    private final BlockDropOffHandler blockHandler = new BlockDropOffHandler();

    @Override
    public MailDropOffResult handle(MailDropOffContext context) {
        if (!(context.getTarget() instanceof PlayerAddress address)) {
            return MailDropOffResult.PASS;
        }

        if (context.getMail().isEmpty()) {
            return MailDropOffResult.CONSUME;
        }

        return context.getService().getKnownPlayers().getDefaultAddressOf(address)
              .map(blockAddress -> blockHandler.handle(new MailDropOffContext(context.getService(), blockAddress, context.getDelivery())))
              .orElseGet(() -> MailDropOffResult.returned(context.getMail(), DeliveryRecord.Message.RECIPIENT_NOT_FOUND));
    }
}