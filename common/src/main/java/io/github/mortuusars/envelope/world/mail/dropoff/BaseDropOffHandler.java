package io.github.mortuusars.envelope.world.mail.dropoff;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.item.component.mail.log.DeliveryRecord;
import io.github.mortuusars.envelope.world.mail.address.type.CustomAddress;
import io.github.mortuusars.envelope.world.mail.address.type.UnknownAddress;

public class BaseDropOffHandler implements MailDropOffHandler {
    @Override
    public MailDropOffResult handle(MailDropOffContext context) {
        if (context.getMail().isEmpty()) {
            return MailDropOffResult.CONSUME;
        }

        if (context.getTarget() instanceof UnknownAddress) {
            return MailDropOffResult.returned(context.getMail(), DeliveryRecord.Message.RECIPIENT_IS_UNKNOWN);
        }

        MailDropOffResult eventResult = Envelope.postHandleMailDropOffEvent(context);
        if (eventResult.isHandled()) {
            return eventResult;
        }

        if (context.getTarget() instanceof CustomAddress) {
            return MailDropOffResult.returned(context.getMail(), DeliveryRecord.Message.RECIPIENT_NOT_FOUND);
        }

        return MailDropOffResult.PASS;
    }
}
