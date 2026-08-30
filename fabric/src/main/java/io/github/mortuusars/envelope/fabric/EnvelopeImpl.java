package io.github.mortuusars.envelope.fabric;

import io.github.mortuusars.envelope.fabric.api.event.EnvelopeFabricEvents;
import io.github.mortuusars.envelope.world.mail.dropoff.MailDropOffContext;
import io.github.mortuusars.envelope.world.mail.dropoff.MailDropOffResult;

public class EnvelopeImpl {
    public static void registerServiceDropOffHandlers() {

    }

    public static MailDropOffResult postHandleMailDropOffEvent(MailDropOffContext context) {
        return EnvelopeFabricEvents.HANDLE_MAIL_DROP_OFF.invoker().handle(context);
    }
}
