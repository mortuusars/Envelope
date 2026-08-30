package io.github.mortuusars.envelope.neoforge;

import io.github.mortuusars.envelope.neoforge.api.event.HandleMailDropOffEvent;
import io.github.mortuusars.envelope.neoforge.integration.kubejs.event.EnvelopeJSEvents;
import io.github.mortuusars.envelope.world.mail.dropoff.MailDropOffContext;
import io.github.mortuusars.envelope.world.mail.dropoff.MailDropOffResult;
import io.github.mortuusars.mortaar.Platform;
import net.neoforged.neoforge.common.NeoForge;

public class EnvelopeImpl {
    public static void registerServiceDropOffHandlers() {
        if (Platform.isModLoaded("kubejs")) {
            EnvelopeJSEvents.registerServiceDropOffHandlers();
        }
    }

    public static MailDropOffResult postHandleMailDropOffEvent(MailDropOffContext context) {
        return NeoForge.EVENT_BUS.post(new HandleMailDropOffEvent(context)).getResult();
    }
}
