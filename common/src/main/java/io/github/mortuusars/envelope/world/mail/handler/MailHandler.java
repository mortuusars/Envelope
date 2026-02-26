package io.github.mortuusars.envelope.world.mail.handler;

import io.github.mortuusars.envelope.world.item.component.mail.log.DeliveryRecord;
import io.github.mortuusars.envelope.world.mail.MailService;
import io.github.mortuusars.envelope.world.mail.delivery.Delivery;

public interface MailHandler {
    MailHandler RETURN_NOT_FOUND = (level, delivery) ->
          MailHandlingResult.returned(delivery.getMail(), DeliveryRecord.Message.RECIPIENT_NOT_FOUND);

    MailHandler RETURN_RECIPIENT_CANNOT_BE_DETERMINED = (level, delivery) ->
          MailHandlingResult.returned(delivery.getMail(), DeliveryRecord.Message.RECIPIENT_CANNOT_BE_DETERMINED);

    /**
     * Processes incoming or returning mail.
     */
    MailHandlingResult handle(MailService service, Delivery delivery);
}