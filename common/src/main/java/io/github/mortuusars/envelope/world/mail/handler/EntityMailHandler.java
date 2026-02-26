package io.github.mortuusars.envelope.world.mail.handler;

import io.github.mortuusars.envelope.world.mail.MailService;
import io.github.mortuusars.envelope.world.mail.address.type.EntityAddress;
import io.github.mortuusars.envelope.world.mail.delivery.Delivery;

public interface EntityMailHandler extends MailHandler {
    EntityAddress getAddress();
    MailHandlingResult handle(MailService service, Delivery delivery);
}
